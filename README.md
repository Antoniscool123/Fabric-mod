# Aetherius: Echoes of the Shattered Sky

[![Build](https://github.com/Antoniscool123/Fabric-mod/actions/workflows/build.yml/badge.svg)](https://github.com/Antoniscool123/Fabric-mod/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/Antoniscool123/Fabric-mod?include_prereleases)](https://github.com/Antoniscool123/Fabric-mod/releases)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

A large, story-driven magic mod for **Minecraft Java Edition 1.20.1 · Fabric**.
Three acts, five schools of magic, a rune-combinatoric spell system, a chunk-rotting
Celestial Blight, multi-block ritual structures, a low-gravity island dimension and a
three-phase dragon that eats the sky.

The whole mod is written as a **framework**: systems are data-driven, extended by
registration rather than by patching, and every piece of content is reachable from Java
without touching a GUI editor.

```
Content:  131 Java files · ~12.6k lines · 13 blocks · 32 items · 10 entities
          19 spells · 15 runes · 14 quests · 12 lore entries
          3 biomes · 1 dimension · 5 particle types · 280 translation keys
Assets:   48 generated textures · 65 block/item models · 45 data files
```

---

## Download & play

1. Download **[aetherius-1.0.0-alpha.3.jar](https://github.com/Antoniscool123/Fabric-mod/releases/download/v1.0.0-alpha.3/aetherius-1.0.0-alpha.3.jar)**
   (plus [`SHA256SUMS.txt`](https://github.com/Antoniscool123/Fabric-mod/releases/download/v1.0.0-alpha.3/SHA256SUMS.txt)).
2. Install **Fabric Loader 0.15.11+** for Minecraft **1.20.1**.
3. Drop the jar **and** [Fabric API 0.92.2+1.20.1](https://modrinth.com/mod/fabric-api) into `.minecraft/mods`.
4. Launch, create a world, and press **K** to open the Chronicle.

The jar is produced by GitHub Actions (`.github/workflows/build.yml`) on every tag:
JDK 17 → Gradle 8.6 → Loom 1.5.8 → `compileJava` + `build` → attached to the release.

> **Alpha caveat:** the mod *compiles and packages* cleanly in CI, but no one has launched
> it in a running client yet. Treat the first launch as a test: if it crashes, the stack
> trace names the exact class. `docs/VERIFICATION.md` lists the known runtime risks.

---

## Table of contents

1. [Status](#status)
2. [Install & build](#install--build)
3. [The story in one paragraph](#the-story-in-one-paragraph)
4. [Playing it: progression walkthrough](#playing-it-progression-walkthrough)
5. [Systems](#systems)
6. [Content index](#content-index)
7. [Repository layout](#repository-layout)
8. [Extending the mod](#extending-the-mod)
9. [Tooling](#tooling)
10. [Known limitations](#known-limitations)

---

## Status

| Area | State |
|---|---|
| Server systems (magic, chronicle, worldstate, blight, boss AI) | implemented |
| Client (HUD, boss bar, Chronicle screen, renderers, particles, keybinds) | implemented |
| Registries, mixins, networking, commands | implemented |
| Datapack content (dimension, biomes, loot, recipes, tags) | implemented |
| Assets (textures, models, lang, sounds) | generated / placeholder-quality |
| Compilation & packaging | **verified in CI** — JDK 17 / Gradle 8.6 / Loom 1.5.8, `compileJava` + `build` green, jar released |
| Runtime (launch, gameplay balance) | **not yet verified** — no client has been run |

Packaging is real: the release jar is built by CI from this exact commit, with the mod's
`fabric.mod.json` version expanded and every asset and datapack file inside. What is *not*
proven is behaviour — spawning Nox'Thal, the blight spread cadence and the HUD layout have
never been observed in a running game. See `docs/VERIFICATION.md` for what CI does and does
not cover.

## Install & build

Requirements: **JDK 17**, Gradle 8.6+ (Loom 1.5 rejects older Gradle), and the Fabric
toolchain that Loom downloads.

```bash
# one-time: the wrapper JAR is a binary and is not committed
gradle wrapper --gradle-version 8.6

./gradlew build          # jar lands in build/libs/aetherius-1.0.0-alpha.jar
./gradlew runClient      # dev client
./gradlew runServer      # dev server
```

Then drop the jar in `.minecraft/mods` alongside **Fabric Loader 0.15.11+** and
**Fabric API 0.92.2+1.20.1**.

Gradle properties (`gradle.properties`) pin MC 1.20.1, Yarn `1.20.1+build.10`,
Loader `0.15.11`, Fabric API `0.92.2+1.20.1`, Java 17, group `com.aetherius`.

## The story in one paragraph

The sky-gods fell at the **First Convergence**, and they fell arguing. Their blood cooled
into ore, their thoughts cooled into runes, and the argument is still happening in the
veins under your feet. **Nox'Thal** — the one that learned to eat what it killed instead of
mourning it — has been quietly devouring light ever since, and the **Celestial Blight** is
what that digestion looks like from below. You are not a chosen one. You are a person with
a wand, a monocle and a book, and the ending is not "the world is fixed" but "the world is
finally answerable."

Full narrative bible: [`docs/LORE.md`](docs/LORE.md).

## Playing it: progression walkthrough

### Act I — The Fallen Starlight

| # | Quest | Gate / objective | Reward |
|---|---|---|---|
| 1 | *A Light Beneath the Stone* | break 1 Star-Vein Ore, hold 2 Aether Shards | Chronicle Tome, +10 mana, *First Convergence* lore |
| 2 | *The Blood of Gods* | collect 4 Aether-Blood | Rune **Flaris**, Sol mastery |
| 3 | *An Instrument for the Blind* | craft the Astrolabe of the Void | Rune **Gelu**, *Sky-Temples* lore |
| 4 | *The Broken Eye* | find a Starlight Observatory, repair its Telescope | Rune **Lunaris**, Caelum mastery |
| 5 | *The Sentinel at the Door* | kill a Void Sentinel | Rune **Murus**, 3 XP, Act I flag |

Star-Vein Ore generates in the overworld between Y −32 and Y 24 and drops **Star-fall**:
a brief meteor shower that seeds Aether Shards and, in the open, exposes Aether-Blood.
The Astrolabe is right-clicked to sweep for the nearest **Sky-Temple** (a Starlight
Observatory); each Observatory can only be claimed once per save.

Repairing the Telescope of the Ancients is phase-locked: it accepts an offering only on
the moon phase matching its rune, so a player may need several in-game nights
(`TelescopeBlockEntity.LastOfferingDay`).

### Act II — The Weave & The Rot

| # | Quest | Gate / objective | Reward |
|---|---|---|---|
| 1 | *What the Monocle Shows* | wear the Aethersight Monocle in the Deep Dark **or** the Nether | Rune **Hiatus**, *Aethersight* lore |
| 2 | *The Rot Has a Name* | witness a blighted chunk, cast 10 Void spells | Rune **Terranox**, *Celestial Blight* lore |
| 3 | *A Machine for Forgiving Yourself* | build the 5×5×5 Aetherial Altar of Purification | Terra mastery, amethyst, *On Purification* |
| 4 | *The First Rite* | perform 1 purification rite | Rune **Solaris**, +15 mana, 25% tension resistance |

Wearing the Monocle underground attunes you to the Weave — it is the only way to *see*
Celestial Blight for what it is. Blight starts as scattered patches of Star-Ash and
spreads once its intensity in a chunk passes 60; the manager runs on a 100-tick cadence
and is capped at 24 block edits per tick so infected regions decay instead of melting
the server.

The Altar rite consumes a bucket of Aether-Blood, a Heart-adjacent catalyst and a
Starlight Elixir, then purifies a 48-block radius: Star-Ash → Starlit Grass, blight
intensity → 0, and the surrounding Void mobs are weakened for a minute. Cooldown: 300 s.

### Act III — The Shattered Horizon

| # | Quest | Gate / objective | Reward |
|---|---|---|---|
| 1 | *Four Keys, Four Wardens* | hold the Hearts of Sol, Luna, Terra and Caelum | Rune **Eclipse**, *Elemental Hearts* lore |
| 2 | *Opening the Horizon* | open the Sky-Rift | +20 mana, *Shattered Sky* lore |
| 3 | *The Shattered Sky* | enter the dimension | Rune **Caelus**, *Aether-Mantas* lore |
| 4 | *Grounding the Unreasonable* | destroy 8 Gravity Anchors | Caelum mastery, *Nox'Thal* lore |
| 5 | *The Eclipse Dragon* (hidden) | kill Nox'Thal | 30 XP, +30 mana, *Epilogue* |

Hearts drop from the four **Elemental Wardens**, which spawn one per school in the
Shattered Sky and cycle elemental abilities every 200 ticks. Placing the four Hearts into
a **Sky-Rift Anchor** opens the portal. Anchors are also Nox'Thal's phase-2 mechanic:
while any anchor lives, the boss takes no damage.

### Cosmic Forge (optional Tier-4 path)

A player-built **3×3×3**: one Aether Forge Block with eight Celestial Pillars on a
lapis/quartz base, validated by `ModMultiblocks.COSMIC_FORGE`. Formed forges accept two
infusions and produce Eclipse-tier artifacts; breaking any component un-forms the
structure and drops the stored infusions.

## Systems

| System | Class(es) | Notes |
|---|---|---|
| Mana + Astral Tension | `PlayerMagicCapability`, `MagicCosts` | dual meter; 50 = Volatility (×2 damage, ×2 cost), 100 = Backfire |
| Backfire | `PlayerMagicCapability.backfire()` | pool explosion, Celestial Blindness 220t, Void Specters, tension → 25, 8t lockout |
| Spell registry | `SpellRegistry`, `RuneCombination`, `AbstractSpell` | keys are sorted rune lists: `"caelum+sol+nox"` |
| Chronicle | `ChronicleRegistry`, `ChronicleManager`, `PlayerChronicle` | 14 quests, 4 chapters, objectives, rewards, lore unlocks |
| Blight | `CelestialBlightManager` | 0–100 intensity per chunk, spread ≥60, abominations ≥40 |
| Temporal storms | `TemporalStormManager` | dimension-wide lightning/particle waves on a random interval |
| Boss AI | `entity/boss/bt/*`, `entity/boss/phase/*` | behaviour tree + phase state machine |
| Rare structures | `StarlightObservatoryFeature` | surface structure, NBT-free, repairable telescope |
| Networking | `AetherNetworking` | 9 packet channels, C2S and S2C |
| HUD | `MagicHudOverlay`, `NoxThalBossBarOverlay` | bars + screen shake, custom RGB boss bar |

Detailed math, spell tables and tuning: [`docs/MAGIC.md`](docs/MAGIC.md).
Boss breakdowns: [`docs/BOSSES.md`](docs/BOSSES.md).

## Content index

**Blocks (13):** star_vein_ore, aether_blood_ore, star_ash, starlit_grass_block,
mana_lily, aether_fountain, celestial_pillar, aether_forge, aether_altar_core, telescope,
rune_lantern, sky_rift_anchor, sky_rift_portal.

**Items (32):** aether_shard, aether_blood, void_dust, star_ash_clump, heart_of_sol/luna/
terra/caelum, astral_wand, eclipse_wand, astrolabe, aethersight_monocle, chronicle_tome,
starlight_elixir, purified_starlight_elixir, 12 block items, 5 spawn eggs.

**Entities (10):** aether_manta, void_sentinel, void_specter, void_warped_abomination,
embers_warden, tides_warden, stones_warden, storms_warden, gravity_anchor, nox_thal.

**Runes (15):** Ignis, Flaris, Solaris · Unda, Gelu, Lunaris · Lapis, Murus, Terranox ·
Ventus, Fulgur, Caelus · Umbra, Hiatus, Eclipse.

**Biomes (3):** `aetherius:shattered_sky`, `aetherius:starlit_meadows`,
`aetherius:blighted_barrens`. **Dimension:** `aetherius:shattered_sky` (0–256, fixed
dusk, 0.14 ambient light, low gravity, End sky effects).

## Repository layout

```
src/main/java/com/aetherius/
├── Aetherius.java              server entrypoint + init ordering
├── AetheriusClient.java        client entrypoint (renderers, HUD, keys, particles)
├── api/magic/                  capability, runes, schools, cost math, sync payload
├── api/spell/                  spell framework + 18 implementations
├── api/chronicle/              quests, chapters, objectives, rewards, manager
├── block/ block/entity/        blocks and five block entities
├── client/                     HUD, boss bar, Chronicle screen, renderers, particles
├── command/                    /aetherius admin surface
├── compat/                     TerraBlender detection
├── dimension/                  dimension keys, teleporter, chunk generator, storms
├── entity/ entity/ai/          base mobs + shared AI goals
├── entity/boss/bt/ phase/      behaviour tree and phase state machine
├── item/                       wand, monocle, astrolabe, tome, elixir, materials
├── mixin/                      4 mixins
├── multiblock/                 pattern matching + structure definitions
├── network/                    packets + channel registration
├── registry/                   sounds, particles, effects, blocks, items, entities, BEs
├── util/                       Aeth (id/lang helpers)
├── world/ world/blight/        world state, starfall, blight manager
└── worldgen/                   worldgen bootstrap, two features

src/main/resources/
├── assets/aetherius/           textures, blockstates, models, particles, lang, sounds
├── data/aetherius/             dimension, biomes, loot, recipes, tags
├── data/minecraft/             tool tags
├── fabric.mod.json
└── aetherius.mixins.json
```

Architecture and data flow: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Extending the mod

**Add a spell** — one class plus one registration:

```java
new AbstractSpell("aetherius:chrono_fold", SpellSchool.CAELUM,
        new SchoolPattern(List.of(Rune.CAELUS, Rune.CAELUS, Rune.HIATUS)),
        /* cooldown */ 200) {
    @Override public void cast(SpellContext ctx) { ... }
    @Override public boolean canCast(SpellContext ctx) { ... }
};
```

Keys are canonical (`caelum+caelum+hiatus`), so rune order never matters at cast time.

**Add a quest** — `ChronicleRegistry.quest(id, chapter, order, objectives, rewards,
requires, hidden, milestoneFlag)`, then add `.title`/`.lore` keys to
`assets/aetherius/lang/en_us.json`. Objectives are composable records implementing
`QuestObjective` (`ItemObjective`, `CounterObjective`, `FlagObjective`, `BiomeObjective`,
`DimensionObjective`, `AnyOfObjective`).

**Add a boss phase** — implement `BossPhase` (enter/tick/exit), register it in the
entity's phase list and, if needed, add behaviour-tree branches built from
`BTStatus`/`BehaviorNode`/`CooldownNode` over a `BossBlackboard`.

## Tooling

The asset pipeline is pure Python (no Pillow, no external downloads):

| Script | Output |
|---|---|
| `tools/generate_textures.py` | 48 PNGs via a hand-written zlib PNG encoder |
| `tools/generate_models.py` | 65 blockstates/models/particle definitions |
| `tools/generate_lang.py` | `lang/en_us.json` (280 keys) |
| `tools/generate_data.py` | dimension, biomes, loot tables, recipes, tags |
| `tools/check_java.py` | brace/paren balance, package-vs-directory, duplicate classes, non-ASCII identifiers |
| `tools/check_refs.py` | every `ModBlocks.X`/`ModItems.X`/… reference resolves to a declared field |
| `tools/check_chronicle.py` | quest flags, objective classes, rune constants resolve |
| `tools/check_lang.py` | translation keys used in Java exist in `en_us.json` |

Run them all:

```bash
python3 tools/generate_textures.py && python3 tools/generate_models.py \
  && python3 tools/generate_lang.py && python3 tools/generate_data.py
python3 tools/check_java.py && python3 tools/check_refs.py \
  && python3 tools/check_chronicle.py && python3 tools/check_lang.py
```

## Known limitations

- **Not compiled yet.** See `docs/VERIFICATION.md` for exactly what was and wasn't checked.
- Textures and models are generated placeholders — readable silhouettes, not final art.
- No custom sounds: `sounds.json` maps 14 events onto vanilla sound files.
- Biome *injection* into vanilla worldgen needs TerraBlender or a datapack override; the
  mod's own answer to "where are the Blighted Barrens" is the runtime blight system.
- The Chronicle screen is a custom `Screen` (no `ScreenHandler`), so it is read-mostly by
  design: quest progress is server-authoritative and pushed via packets.
- Nox'Thal's phase-3 star strikes use vanilla lightning cosmetics plus custom AoE damage;
  there is no per-strike custom model.

## Licence & credits

MIT (see `LICENSE`). Designed and generated for the *Aetherius* concept brief.
Minecraft is a trademark of Mojang; this project is not affiliated with Mojang or Fabric.
