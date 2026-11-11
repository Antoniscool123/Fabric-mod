# Verification Status

The project is **built and packaged by CI** — `compileJava` and `build` both pass — and it has
been **launched by a real player** twice (alpha.3: world load crashed; alpha.4: world load clean).
This file records exactly what is checked, how, and what remains unverified.

## 0. CI build (authoritative)

| Item | Value |
|---|---|
| Workflow | `.github/workflows/build.yml` |
| JDK | Temurin 17 |
| Gradle | 8.6 (Loom 1.5 requires ≥ 8.3) |
| Loom / Yarn | 1.5.8 / 1.20.1+build.10 |
| Tasks | `compileJava` → `build` (remapJar, sourcesJar) |
| Result | green; `aetherius-1.0.0-alpha.5.jar` + `-sources.jar` + `SHA256SUMS.txt` on the release |
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
gradle wrapper --gradle-version 8.6   # binary wrapper JAR is not committed
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
