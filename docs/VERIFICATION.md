# Verification Status

The project is **built and packaged by CI** — `compileJava` and `build` both pass — and it has
been **launched by a real player** twice (alpha.3: world load crashed; alpha.4: world load clean).
This file records exactly what is checked, how, and what remains unverified.

## 0. CI build (authoritative)

| Item | Value |
|---|---|
| Workflow | `.github/workflows/build.yml` |
| JDK | Temurin 17 |
| Gradle | 8.14 (Loom 1.10 requires ≥ 8.11) |
| Loom / Yarn | 1.5.8 / 1.20.1+build.10 |
| Tasks | `compileJava` → `build` (remapJar, sourcesJar) |
| Result | green; `aetherius-1.0.0-alpha.8.jar` + `-sources.jar` + `SHA256SUMS.txt` on the release |
| Errors left | 0 (`-Xmaxerrs 500`, so nothing is hidden behind the 100-error default) |

The compile run also regenerates every asset and datapack file and re-runs the static
checkers below, so a release cannot be published from a state that fails them.

---

## 0b. First live run — 2026-11-11 (Windows 11, Prism 11.1.1, MC 1.20.1, Loader 0.20.1)

A user launched `1.0.0-alpha.3` and clicked **Singleplayer**. What the log proved and what it
broke:

**Worked**

- Client reached the main menu with Fabric API 0.92.12 loaded alongside the mod.
- All four mixins applied — no `MixinApplyError`, so the `@Inject` targets are valid in 1.20.1.
- `[Aetherius] Initialised in 205 ms - 18 spells, 14 quests, 12 lore entries`.
- Registry sync, block/item/entity/particle registries and the model loader all started.

**Broke (three bugs, all in the generated resources, none in Java)**

| Symptom in the log | Cause | Fix |
|---|---|---|
| `Failed to parse aetherius:worldgen/biome/*.json` → `RuntimeException: Not a JSON object: []` | biomes emitted `"carvers": []`; 1.20.1 expects an object | `"carvers": {"air": []}` |
| `aetherius:block/minecraft:block/dirt is not valid resource location` | the model generator prefixed `aetherius:block/` onto the vanilla `minecraft:block/dirt` texture | namespace-aware `tex()` helper |
| 14 × `File minecraft:sounds/....ogg does not exist` | `sounds.json` used dotted event names instead of `assets/<ns>/sounds/` paths | generated sounds table with `minecraft:item/firecharge_use` style paths |

The third bug was cosmetic (missing sound, no crash); the first was fatal — it aborted registry
loading, which is why the crash happens when a world screen tries to build its
`DynamicRegistryManager`. Note the second and third were *warnings* in an otherwise healthy
reload, which is exactly why they are now gated in CI instead of being left to the game.

**New guard:** `tools/check_data.py` validates biome `carvers`/`features`/`spawners` shapes,
resource-location syntax (exactly one colon), model parent/texture references, blockstate
variants, placed/configured feature structure, dimension and dimension-type required fields,
and sounds.json path format. It runs before compile in CI, so a release can no longer ship any
of the three bugs above.

---

## 0c. Second live run — `1.0.0-alpha.4`, same machine

The user's log after alpha.4 shows the world loads with **no crash**:

```
[Aetherius] World seed 4470475199119711005 bound to the Shattered Sky generator
[Aetherius] Initialised in 93 ms - 18 spells, 14 quests, 12 lore entries
```

That confirmed the worldgen/resource class of bugs was closed. Four *playability* bugs remained,
all reported from the same session and all fixed in `1.0.0-alpha.5`:

| Symptom | Root cause | Fix |
|---|---|---|
| Chronicle (K) opened but nothing reacted — no tab, no Close, not even ESC | `ChronicleScreen.init()` sent `ACTION_OPEN`; the server answered with `OPEN_CHRONICLE`; the client replaced the already-open screen with a fresh one. `init()` ran again, so every click/ESC was undone within a frame (infinite screen churn) | `ACTION_OPEN` is now a *sync request only* (`CHRONICLE_SYNC`); `OPEN_CHRONICLE` is ignored while any screen is open, and `ChronicleTomeItem` no longer double-requests |
| Nox'Thal's head floated above the neck; body looked like loose boxes | head cuboids used body-space `y = -24..-26` offsets *and* a `pivot(0, -22, -6)` on a neck that already spanned `-24` → head displaced ~46 px; two of the four head boxes were anchored to a different origin than the skull | model rebuilt in absolute model space, pivots on the real joints, head+snout+horns in one part so rotation cannot split them; 256×256 atlas matching the actual cuboid faces; hitbox raised to 3.0×3.2 so the model is inside it |
| `/aetherius dimension` "does not exist" | the subcommand was documented but never registered | `dimension` + `dimension return` added; the root literal now allows permission level 0, with op level re-applied per mutating subcommand; added `spells`/`quests` shortcuts |
| 12 × `File minecraft:sounds/....ogg does not exist` + `Unable to play empty soundEvent: aetherius:boss_nox_roar` | `sounds.json` guessed vanilla file paths, and 14 mod-owned events had no audio at all | `sounds.json` deleted; `ModSounds` is now a table of real vanilla `SoundEvents` aliases, so the compiler guarantees the file exists and there are no empty events |

The lesson recorded here: **do not hand-write asset paths the game owns.** Either point at a
`SoundEvents` constant in Java, or ship the asset. `check_data.py` now no longer validates
`sounds.json` because nothing ships one.

---

## 0d. Third run — `1.0.0-alpha.5`, and what alpha.6 changes

The user confirmed **the Chronicle is interactive** (tabs, Close, ESC) and that the Nox'Thal model
no longer falls apart — but reported it still looks wrong, and that **things spawn in the Shattered
Sky where there is no block**.

### Spawning in mid-air — the actual cause

Nothing registered a `SpawnRestriction` for any Aetherius entity. Vanilla treats a type without a
restriction as *"may spawn wherever the position is free"*, and "free" in a sky dimension is
"anywhere in the 256-block column". So specters, sentinels, mantas and abominations appeared in
open air. On top of that:

- `VoidSpecterEntity` applied a constant `-0.01` downward drift with `setNoGravity(true)`, so any
  specter not currently chasing a player sank into the void for ever.
- `AetherMantaEntity` only ever gained height (`+0.02` every 40 ticks) and slowly climbed out of
  the world.
- `monster_spawn_light_level: 0` with `has_skylight: true` meant monster light level is always 15,
  i.e. monsters could **never** spawn on the islands — the dimension was both "empty" and "noisy
  with flying things".

Fixed in alpha.6:

| Change | File |
|---|---|
| `SpawnRestriction.register(...)` for all 10 entity types: walkers need a solid block under the feet, fliers need island ground within 24 blocks plus 3 blocks of headroom, the boss/anchor need open sky at least 6 blocks above an island | `registry/ModEntities`, `world/SkySpawnRules` |
| Hover correction + void cleanup for the specter and manta | `entity/VoidSpecterEntity`, `entity/AetherMantaEntity` |
| Monsters spawn in a light band of 0-7 instead of only light 0 | `dimension_type/shattered_sky.json` |
| `spawn_costs` per biome + `creature_spawn_probability: 0.08` so spawn budgets are finite | `worldgen/biome/*.json` |

### Nox'Thal model, third pass

`tools/preview_model.py` now renders the model offline from the Java source (three views, with the
airborne animation pose applied) — see `docs/img/nox_thal_model.png`. That caught two bugs a live
test would have shown only after a release:

- the head was a *sibling* of the neck, so `neck.pitch` tore the head off the body;
- `body.pivotY` bobbing moved the torso alone, sliding it out of the neck and wings.

The head is now parented to the neck, all whole-body motion is applied to the model root, and the
geometry is a tapered chest+hips torso, a two-step forward-leaning neck, a 14x13x12 skull with
brow, jaw, snout, crest and swept horns, four-bone wings with four membrane panels plus claws,
a three-segment tail with a vertical fin, and four two-bone legs that land exactly on the y = 24
ground plane. Renderer scale is now 1.0 (the model is authored at true size), shadow radius 2.2.

## 0e. `1.0.0-alpha.8` — GeckoLib migration and the Loom trap

`alpha.7` fixed the codec crash and **compiled green in CI**, so the first attempt at the GeckoLib
migration (`5a58c33`) was expected to be a compile-only change. It failed with a misleading error:

```
ShatteredSkyChunkGenerator.java:41: error: ShatteredSkyChunkGenerator is not abstract and does not
override abstract method getColumnSample(int,int,HeightLimitView,NoiseConfig) in ChunkGenerator
```

The method was in fact implemented with exactly the signature `javap` reports for the mapped
Minecraft jar, so the message was collateral. Reading the whole Gradle log (not just `error:` lines)
gave the real cause:

```
Caused by: java.lang.IllegalStateException:
  Mod was built with a newer version of Loom (1.10.5), you are using Loom (1.5.8)
```

GeckoLib 4.8.4 is published with Loom 1.10.5, and Loom refuses to remap a mod built with a **newer**
Loom than the project's own. With the GeckoLib jar unreachable, Loom left the whole
`modImplementation` classpath half-resolved and javac reported a bogus abstract-method error.

Fix: `fabric-loom` `1.5-SNAPSHOT` → **`1.10.5`**, Gradle **8.14** (wrapper, CI and README), and the
Cloudsmith repository restricted with `exclusiveContent { filter { includeGroup
'software.bernie.geckolib' } }` so it can never serve a Minecraft/mapping artifact.

CI after the bump (`97cd52f`): `compileJava` exit 0, jar assembled, jar gate verified, checksums
written — **the first green build that contains GeckoLib code**.

> **Lesson recorded:** a failure whose message points at the *last* good file is usually a
> dependency-resolution failure. `grep "Caused by"` on the full log beats trusting the first
> `error:` line.

### What is still unverified in alpha.8

GeckoLib compiles, but nothing has *run* yet:

- `getTick(Object)`, the `AnimationController` transition length and the `RawAnimation` names are
  compile-checked only; a wrong animation id would silently play nothing.
- The `.geo.json` is verified by `tools/preview_model.py` (bones, pivots, silhouette, wing
  symmetry) and by `tools/check_data.py` (every animated bone exists in the model), not by the game.
- The `attack` triggerable animation is wired to the cosmic-breath attack; whether it fires and
  blends back into flight was not observed.

## 0g. `1.0.0-alpha.11` — islands restored, overworld injected (optional)

Two changes, one of them a deliberate revert of alpha.9.

| Change | Risk | What CI proves | What only a live run can prove |
|---|---|---|---|
| `ShatteredSkyChunkGenerator` rewritten as a floating-island generator | **high** — it is the dimension's whole terrain | compiles; `islandAt`/`surfaceHeight`/`nearestIslandCentre` still exist, which is what `SkyRiftTeleporter` calls; `getDebugHudText` retained; brace/package/reference checks pass | that islands generate at sane altitudes, that the origin island is landable, that you can walk off an edge and fall (intended) without the client dying |
| TerraBlender overworld injection (optional) | medium | compiles against `TerraBlender-fabric 1.20.1-3.0.1.11`; every class touched is inside `compat/`; the jar's `fabric.mod.json` lists the `terrablender` entrypoint and `suggests` TerraBlender rather than depending on it | that the biomes actually appear in a **new** overworld, and that the game still loads with TerraBlender **absent** |

Design points that were checked before compiling rather than after:

- **Signature drift.** `Region#addBiomes` takes a `Consumer<Pair<MultiNoiseUtil.NoiseHypercube, RegistryKey<Biome>>>`
  in Yarn 1.20.1 (mojmap: `Climate.ParameterPoint`). That type name was taken from the Yarn 1.20.1
  javadoc, not guessed, because a wrong nested-class name is a hard compile error.
- **Soft dependency.** No class outside `compat/` mentions `terrablender.*`; the dependency is
  `modCompileOnly`, so the produced jar contains no TerraBlender classes and no code path that
  requires them unless the `terrablender` entrypoint is invoked.
- **Biome source untouched.** The Shattered Sky keeps its own three-biome source; the overworld
  injection is additive and does not alter `aetherius:dimension/shattered_sky.json`.

## 0f. `1.0.0-alpha.9` — four changes, one of them risky

| Change | Risk | What CI proves | What only a live run can prove |
|---|---|---|---|
| Vanilla boss bar removed | low | compiles, no dangling `ServerBossBar` | that only one bar is on screen |
| Guide tab in the Chronicle | low | all 33 lang keys exist, tab/mouse handling compiles | that the text fits the panel |
| All textures regenerated | low | 47 valid PNGs, sheet inspected by eye | that they read well at 16x16 in game |
| **Terrain rewritten + custom biome source** | **high** | compiles, dimension JSON parses, `biome_source` shape checked | that the dimension loads at all, and that the three biomes appear |

The risky one is the worldgen. Two specific failure modes were designed out:

- **Registry order.** `Registries.BIOME_SOURCE` is registered in `ModWorldGen.register()`, which the
  initialiser calls *before* the datapack is read. If it were missing, the dimension fails to parse
  and no world can be created — the alpha.6 failure mode.
- **Sampler dependence.** A custom generator has no climate sampler, so the biome source ignores
  `MultiNoiseUtil.MultiNoiseSampler` completely and picks regions from the same value noise the
  terrain uses. `minecraft:multi_noise` was rejected for exactly this reason.

The biome list in the dimension JSON is positional (`meadows, highlands, barrens`), and
`ShatteredSkyBiomeSource` clamps instead of throwing if the list is short, so a partial list
degrades to one biome instead of aborting the load.

## 1. What is verified (mechanically)

| Check | Tool | Result |
|---|---|---|
| Brace/paren/bracket balance in all 131 Java files | `tools/check_java.py` | pass |
| `package` matches directory for every file | `tools/check_java.py` | pass |
| No duplicate top-level class names | `tools/check_java.py` | pass |
| No non-ASCII outside comments/strings (identifier guard) | `tools/check_java.py` | pass |
| Every `ModBlocks.X` / `ModItems.X` / `ModEntities.X` / `ModSounds.X` / `ModParticles.X` / `ModStatusEffects.X` / `ModBlockEntities.X` / `ModMultiblocks.X` reference resolves | `tools/check_refs.py` | pass |
| Every `ChronicleCounters.FLAG_*` reference is declared | `tools/check_chronicle.py` | pass |
| Every objective class instantiated exists in `api/chronicle/objective` | `tools/check_chronicle.py` | pass |
| Every `Rune.X` reference is a declared constant | `tools/check_chronicle.py` | pass |
| Every statically referenced translation key exists in `en_us.json` | `tools/check_lang.py` | pass (3 dynamic prefixes verified by hand) |
| `en_us.json` parses as valid JSON, 280 keys | `generate_lang.py` re-read | pass |
| 37 datapack files parse as valid JSON | `generate_data.py` | pass |
| 48 textures written as valid PNGs | `generate_textures.py` | pass |
| 65 blockstate/model JSON files | `generate_models.py` | pass |

Hand-verified consistencies:

- Dynamic lang keys: `aetherius.boss.nox_thal.phase1..3`, `item.aetherius.heart_of_*`,
  `item.aetherius.wand.tier.{astral,eclipse}` — all present.
- `Identifier.ofVanilla` is **not** used anywhere (1.20.1 only has the `new Identifier(ns, path)`
  constructor); the one occurrence was removed.
- Quest dependency graph is acyclic and every `requires` id exists in the registry.
- Boss phase thresholds (1.0/0.67/0.34) match `phaseIndex = ratio <= 0.67 ? 1 : 0 · ratio <= 0.34 ? 2`.
- `Mana/tension` numbers in `docs/MAGIC.md` were recomputed by hand against `MagicCosts.of`.

## 2. What is NOT verified

| Risk | Notes |
|---|---|
| Runtime behaviour | no server or client ever started; tick order, packet handling and rendering are unproven. |
| Mixin targets | they compile, but the `@Inject` targets are only proven at launch. `EntityGravityMixin` is marked `require = 0` so a missing `Entity#getGravity` degrades instead of crashing. |
| Client rendering | `HudRenderCallback`, `Screen` layout, model UVs and renderer transforms are untested. |
| Datapack schema | biomes/dimension JSONs follow 1.20.1 shapes but were not loaded by the game. |
| Balance | tuning numbers are design values, not playtested values. |
| Assets | generated PNGs are placeholder art. |

## 3. First-build playbook

When you do get a toolchain, run in this order and fix in this order — it front-loads the
failures that cascade:

```bash
gradle wrapper --gradle-version 8.14   # binary wrapper JAR is not committed
./gradlew compileJava 2>&1 | tee build.log
```

1. **Missing/renamed vanilla symbols.** Expect a handful of `cannot find symbol` in
   `client/`, `entity/boss/`, and `block/entity/`. Fix by checking the Yarn javadoc for
   1.20.1+build.10; do not guess.
2. **Mixin validation.** `InvalidMixinException` at launch lists the exact target signature.
   Common culprits: `EntityGravityMixin` (gravity field/`travel`) and
   `ServerPlayerEntityMixin` (read/write NBT hook names).
3. **Registry ordering.** If the game dies with "Item requires a block" or a null entity type,
   verify `Aetherius.onInitialize()` still calls blocks → entities → items in that order.
4. **Datapack load.** `Failed to parse ... dimension_type/shattered_sky.json` means a field
   name drifted; compare against `data/minecraft/dimension_type/the_end.json` in the
   extracted 1.20.1 jar.
5. **Client crash on world join.** Almost always an unregistered particle factory or an
   entity renderer registered for a type the client doesn't know; check the `AetheriusClient`
   log line and the `aetherius:magic_sync` handler.

Quick smoke tests once it boots:

```
/aetherius spells            # prints the 19 registered spell keys        (no op needed)
/aetherius quests            # prints the 14 quest ids + dependencies     (no op needed)
/aetherius dimension         # teleports to aetherius:shattered_sky       (no op needed)
/aetherius dimension return  # back to the Overworld spawn
/aetherius chronicle         # opens the Chronicle book
/give @s aetherius:astral_wand
/aetherius tension 100       # op only: should trigger a backfire on the next tick
/summon aetherius:nox_thal   # walks through the three phase bars
```

Anything that mutates world or player state (`mana`, `tension`, `rune`, `quest`, `blight`,
`boss`) requires permission level 2; the read/teleport commands above do not.

Expected: backfire explodes the mana pool, blinds the player for ~11 s, spawns 2–4 Void
Specters and injects +3 blight into the chunk. Nox'Thal should announce phase 1, become
immune to arrows, summon four anchors at 67%, and black out the screen at 34%.

## 4. Repository hygiene

- Generated artefacts (`build/`, `.gradle/`, `run/`) are gitignored.
- `tools/*.py` are deterministic and dependency-free; re-running them reproduces every
  asset and data file byte-for-byte (JSON is written with `indent=2`).
- The Gradle wrapper **JAR** is intentionally absent (binary, unavailable offline). The
  `gradle/wrapper/gradle-wrapper.properties` pinning Gradle 8.1.1 *is* committed so the
  first `gradle wrapper` call reproduces the expected version.

## 4b. Known runtime risks (ranked)

1. **Mixin application** — `PlayerEntityMixin` (`writeCustomDataToNbt`/`readCustomDataFromNbt`/`checkFallFlying`), `ServerPlayerEntityMixin` (`copyFrom`), `LivingEntityMixin` (`jump`/`handleFallDamage`). If a name drifted, launch fails loudly; the log names the mixin.
2. **Registry bootstrap order** — `Aetherius.onInitialize` must keep blocks → entities → items.
3. **Datapack schema** — one malformed field in `dimension_type/shattered_sky.json` blocks the world from loading (not the client from starting).
4. **Particle factories** — an unregistered type logs a warning per particle, it does not crash.
5. **Chunk generator callbacks** — `getColumnSample`/`getHeight` feed vanilla heightmaps; wrong values produce floating/sunken terrain rather than a crash.

## 5. Definition of "working"

The mod is considered MVP-complete when all of the following hold in a dev client:

1. `./gradlew build` produces a jar with no errors.
2. A new world starts, Star-Vein Ore generates, breaking it triggers a star-fall.
3. The Chronicle opens with K, shows Act I, and completes quest 1 on shard pickup.
4. Casting `sol` (Ignis) spawns Ember Lance and drains 8 mana / 1 tension.
5. Pushing tension to 100 backfires with shake, blindness and Specters.
6. A crafted Observatory is found with the Astrolabe and its Telescope repairs on the
   correct moon phase.
7. Building the Cosmic Forge forms the multiblock; breaking a pillar un-forms it.
8. `/aetherius dimension` lands the player in the Shattered Sky with visible islands, mantas
   and a storm within a few minutes.
9. Nox'Thal completes all three phases and killing it awards the hidden epilogue quest.

None of the above has been executed yet. Treat this file as the acceptance test.
