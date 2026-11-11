# Architecture

How the mod is put together, how data flows, and where to add things.

---

## 1. Entry points & init order

`com.aetherius.Aetherius` (`ModInitializer`) runs a strictly ordered bootstrap — ordering is
a contract, not a style choice:

| Step | Call | Why it must be here |
|---|---|---|
| 1 | `ModSounds`, `ModParticles`, `ModStatusEffects` | no dependencies; effects are referenced by the capability |
| 2 | `ModBlocks` | item registry needs block instances |
| 3 | `ModEntities` | spawn eggs need entity types |
| 4 | `ModItems` | block items + spawn eggs need steps 2–3 |
| 5 | `ModBlockEntities`, `ModItemGroups` | block entities reference blocks; the group lists items |
| 6 | `ModEntities.registerAttributes()` | attribute containers are per-entity-type |
| 7 | `ModWorldGen.register()` | registers the `ShatteredSkyChunkGenerator` codec + placed features |
| 8 | `SpellRegistry.bootstrap()`, `ChronicleRegistry.bootstrap()` | content arrays are static and cheap; must exist before packets |
| 9 | `AetherNetworking.registerServerReceivers()` | receivers reference spell/quest registries |
| 10 | `registerServerEvents()` | tick/join/death hooks |

`AetheriusClient` mirrors this on the client: renderer + model-layer registration, HUD
callbacks, particle factories, keybinds, and a client tick hook that drains
`ClientEffects`.

Startup logs one line: `Initialised in N ms - X spells, Y quests, Z lore entries`. That is
the cheapest smoke test in the project.

---

## 2. Package map

```
api/magic        PlayerMagicCapability, MagicUser, MagicComponents, MagicHolder,
                 SpellSchool, Rune, CastResult, MagicCosts
api/spell        SpellRegistry, AbstractSpell, SchoolPattern, RuneCombination,
                 SpellContext, impl/{BaseSpells, DuoSpells, TrioSpells, ResonantEchoSpell}
api/chronicle    Chapter, Quest, QuestObjective, QuestReward, PlayerChronicle,
                 ChronicleCounters, ChronicleRegistry, ChronicleManager,
                 objective/{Item,Counter,Flag,Biome,Dimension,AnyOf}Objective
network          AetherNetworking + 9 packet types
registry         ModBlocks, ModItems, ModEntities, ModSounds, ModParticles,
                 ModStatusEffects, ModBlockEntities, ModItemGroups
item             WandItem, AstrolabeItem, AethersightMonocleItem, ChronicleTomeItem,
                 StarlightElixirItem, AetherArmorMaterials
block            12 blocks; block/entity: forge, altar, telescope, anchor + core BE
multiblock       MultiblockPattern, ModMultiblocks (COSMIC_FORGE, ALTAR, ...)
entity           6 mobs + boss/phase/{StarlitHerald,GravityCollapse,Supernova}
entity/boss/bt   behaviour tree: BTStatus, BehaviorNode, Behaviors, 7 node types
dimension        AetherDimensions, ShatteredSkyChunkGenerator, SkyRiftTeleporter,
                 TemporalStormManager
world            AetheriusWorldState (SavedState), StarfallManager,
                 blight/CelestialBlightManager
worldgen         ModWorldGen, feature/{StarlightObservatory,ManaLilyPatch}
client           AetheriusClient, ClientEffects, ClientNetworkHandlers,
                 hud/{MagicHudOverlay,NoxThalBossBarOverlay}, screen/ChronicleScreen,
                 render/*, particle/*
mixin            4 mixins (player, server player, entity gravity, living entity)
```

**Rule:** `api/` never references `client/`. Everything else may reference `api/`, and only
`item`/`block`/`entity`/`network` touch Minecraft internals.

---

## 3. Player state: capability + NBT + sync

```
ServerPlayerEntity
   └─ MagicComponents.get(player)  →  PlayerMagicCapability  (MagicUser)
            ├─ mana, maxMana, astralTension
            ├─ unlockedRunes (List<Rune>), mastery[school] (int[])
            ├─ castLockoutTicks, backfireTicks, celestialBlindnessTicks
            └─ chronicle (PlayerChronicle: quest progress, counters, flags, lore)
```

- Persistence: `PlayerMagicCapability` writes an NBT compound with `NBT_VERSION = 2` into the
  player's persistent state via the mixin; unknown versions are upgraded field-by-field.
- Dirty flag: any mutation calls `markDirty()`; the server tick flushes dirty capabilities
  once per tick through `AetherNetworking.syncMagic`.
- Client mirror: `MagicSyncS2CPacket` carries mana, maxMana, tension, mastery[5],
  unlocked runes (list of ordinals), lockout/backfire/blindness timers. The client stores a
  read-only `MagicHolder` — it never simulates costs.
- `ChronicleSyncS2CPacket` ships the whole `PlayerChronicle` as NBT (progress maps, counters,
  flags) because quest state is small and authority matters more than bytes here.

**Why a capability and not an attached data component?** 1.20.1 has no data components, and
the mixin approach gives every player a single object that both the tick loop and the packet
layer can reach without lookups.

---

## 4. Casting pipeline

```
Right-click wand
  └─ WandItem.use()
       └─ InscribeRuneC2SPacket / local sequence read
            └─ SpellRegistry.resolve(List<Rune>)
                 ├─ canonical key ("caelum+sol+nox")
                 ├─ exact match → AbstractSpell
                 └─ fallback → ResonantEchoSpell
                      └─ PlayerMagicCapability.cast(spell)
                           ├─ canCast?  (dimension/boss/duel rules)
                           ├─ costs = MagicCosts.of(combination)
                           │     × 2 if isManaVolatile()
                           ├─ mana check → CastResult.Reason.NOT_ENOUGH_MANA
                           ├─ lockout check
                           ├─ spell.cast(SpellContext.of(player, world, look, eyePos))
                           ├─ tension += costs.tension (× 2 if volatile)
                           ├─ wand.onSuccessfulCast()
                           ├─ CastFeedbackS2CPacket → action bar
                           └─ tick(): tension >= 100 → triggerAstralBackfire()
```

The client never decides a cast succeeded. `CastFeedbackS2CPacket` is the only source of
truth for the action-bar line and the wand animation.

---

## 5. Packets

| Channel | Direction | Payload |
|---|---|---|
| `aetherius:cast_spell` | C2S | inscribed rune ordinals |
| `aetherius:inscribe_rune` | C2S | append rune / clear |
| `aetherius:chronicle_action` | C2S | open / track quest id |
| `aetherius:magic_sync` | S2C | full magic state |
| `aetherius:cast_feedback` | S2C | success + failure reason |
| `aetherius:chronicle_sync` | S2C | chronicle NBT + toast |
| `aetherius:screen_shake` | S2C | origin, radius, duration |
| `aetherius:boss_event` | S2C | phase change, darkness, reset |
| `aetherius:open_chronicle` | S2C | force-open the Chronicle screen |

All packets implement `Packet`-style `write/read` over `PacketByteBuf` and are registered in
one place (`AetherNetworking`), with static `send(...)` helpers so call sites never touch
`ServerPlayNetworking` directly.

---

## 6. Multiblocks

`MultiblockPattern` is a data description:

```java
MultiblockPattern.builder("cosmic_forge", 3, 3, 3)
    .layer("QLQ", "LQL", "QLQ")   // y = 0: inscribed base
    .layer("PPP", "PFP", "PPP")   // y = 1: eight pillars + forge core
    .layer("###", "###", "###")   // y = 2: open above the forge
    .legend('Q', QUARTZ)
    .legend('L', LAPIS)
    .legend('P', PILLAR)
    .legend('F', ModBlocks.AETHER_FORGE)
    .build();
```

Legend semantics: `'#'` matches anything, `'.'` matches air or any non-solid block (so a
player can stand inside the altar chamber), every other character is a `Predicate<BlockState>`.
The matcher tries all four horizontal rotations, so a structure can be built facing any
direction. `ModMultiblocks` holds `COSMIC_FORGE` (3×3×3, core at local `1,1,1` via
`FORGE_CORE_OFFSET`) and `AETHER_ALTAR` (5×5×5, quartz/lapis shell, hollow pillar chamber,
core `A` in the centre).

Validation flow (forge example):

1. `AetherForgeBlock.onUse` → `ModMultiblocks.COSMIC_FORGE.match(world, pos, facing)`.
2. Match walks offsets, resolves the anchor position to the core, and returns it.
3. On success the core block entity sets `Formed = true`, stores `Infusions`, and the block
   state swaps to the formed model.
4. Any break/`onStateReplaced` un-forms it and drops stored infusions — no ghost structures.
5. Block entities validate lazily on load, so a chunk loaded in a broken state self-heals.

The altar is the same machinery plus a rite (`Purifications`, `LastRite`) and a 300-second
cooldown; the telescope is not a multiblock (it is repaired with items and phase-locked).

---

## 7. World state

`AetheriusWorldState` is a `PersistentState` (key `aetherius`), not per-chunk NBT, because it
must be globally consistent:

| Field | Meaning |
|---|---|
| `Blight` | map of `ChunkPos → intensity` (0–100) |
| `Observatories` | claimed observatory positions (one claim per structure) |
| `NoxDefeated` | final-boss flag for the world |
| `LastDecayTick` | throttle for passive decay |

`CelestialBlightManager` owns the spread rules: spread only at intensity ≥ 60, 12 intensity
per spread event, 12% chance per orthogonal neighbour, 100-tick cadence, ≤24 block edits per
tick, abomination spawns ≥ 40. `StarfallManager` owns the meteor shower; both are singletons
ticked from the server tick event.

---

## 8. Worldgen

- Dimension `aetherius:shattered_sky`: type with `min_y 0`, `height 256`, `fixed_time 15000`,
  ambient light 0.14, `effects: the_end`, no beds/anchors/respawn.
- `ShatteredSkyChunkGenerator` generates the archipelago: a noise-free island field built
  from hashed cell positions, with a fixed seed bound at `SERVER_STARTED`. It extends
  `ChunkGenerator` and registers its own codec.
- Biomes: `starlit_meadows` (sky-blue, observatories + mana lilies), `shattered_sky`
  (night, mantas + specters), `blighted_barrens` (dark, abominations).
- Features: `starlight_observatory` (surface structure, once per 3×3 region via a placement
  modifier), `mana_lily_patch` (vegetal decoration).
- Overworld ores are injected with `BiomeModifications.addFeature` over
  `BiomeSelectors.foundInOverworld()`.
- Overworld *biome* injection is out of scope for vanilla Fabric — see
  `compat/TerraBlenderCompat` for the three supported strategies.

---

## 9. Mixins

| Mixin | Target | Purpose |
|---|---|---|
| `PlayerEntityMixin` | `PlayerEntity` | shrink the hitbox/eye height while inverted, hook use-item for wands |
| `ServerPlayerEntityMixin` | `ServerPlayerEntity` | attach the capability, save/load NBT, resync on copy |
| `EntityGravityMixin` | `Entity` | gravity inversion (`ClientEffects` + server flag) |
| `LivingEntityMixin` | `LivingEntity` | damage interception (Sentinel absorption, boss phase rules), death hook |

`aetherius.mixins.json` sets `compatibilityLevel: JAVA_17`, `defaultRequire: 1` and declares
no client mixins (the client overlay uses the HUD callback instead).

---

## 10. Client rendering

- **HUD** — `HudRenderCallback` draws mana/tension/rune pips from the mirrored state; shake
  comes from `ClientEffects` counters seeded by `ScreenShakeS2CPacket`.
- **Boss bar** — `NoxThalBossBarOverlay` finds the nearest Nox'Thal each frame, reads phase
  from `ClientEffects.bossPhase(entityId)` (fed by `BossEventS2CPacket`), draws an RGB gradient with
  notch marks at 67%/34%, and applies the Supernova blackout tint while the darkness timer
  runs.
- **Entities** — `AetherMobRenderer` (generic scaled humanoid), `AetherMantaModel` +
  `AetherMantaRenderer` (wings, rider offset), `NoxThalModel` + `NoxThalRenderer` (dragon
  body with per-phase glow), `GravityAnchorRenderer` (crystal block + rotating ring).
- **Chronicle** — plain `Screen` with a tab strip (4 chapters), a quest list column, a detail
  pane (objectives with progress bars, rewards, lore), and a footer showing act completion.
  It is read-mostly by design; mutations go through `ChronicleActionC2SPacket`.
- **Particles** — `AetherParticles.register()` binds each type to `AetherParticle.Factory`
  with a colour profile; a missing factory is the classic "invisible particle" bug.

---

## 11. Adding content — checklists

**New spell:** subclass `AbstractSpell` in `api/spell/impl`, register in `SpellRegistry`, add
`aetherius.spell.<key>` and `.desc` lang keys, add a texture if it gets an icon.

**New quest:** add a `quest(...)` call in `ChronicleRegistry`, pick objectives, add
`.title`/`.lore` keys, and (if it unlocks a rune) `QuestReward.ofRune`.

**New block:** register in `ModBlocks`, add blockstate + model + texture (or extend
`tools/generate_models.py`), loot table in `data/aetherius/loot_tables/blocks`, tool tag, and
a block item in `ModItems`.

**New boss phase:** implement `BossPhase` (`index/id/titleKey/barColor/tree/onEnter/tick/onExit`),
append to the entity's phase list, and make sure thresholds map to your new index.

**New packet:** add a class in `network/` with `write/read` and static `send`, register both
sides in `AetherNetworking`, and never import a client class from the server handler.
