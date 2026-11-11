# Verification Status

The project is now **built and packaged by CI** — `compileJava` and `build` both pass, and a
jar is attached to the `v1.0.0-alpha.2` release. It has **never been launched**, so runtime
behaviour is still unknown. This file records exactly what is checked, how, and what remains
unverified.

## 0. CI build (authoritative)

| Item | Value |
|---|---|
| Workflow | `.github/workflows/build.yml` |
| JDK | Temurin 17 |
| Gradle | 8.6 (Loom 1.5 requires ≥ 8.3) |
| Loom / Yarn | 1.5.8 / 1.20.1+build.10 |
| Tasks | `compileJava` → `build` (remapJar, sourcesJar) |
| Result | green; `aetherius-1.0.0-alpha.2.jar` + `-sources.jar` + `SHA256SUMS.txt` on the release |
| Errors left | 0 (`-Xmaxerrs 500`, so nothing is hidden behind the 100-error default) |

The compile run also regenerates every asset and datapack file and re-runs the static
checkers below, so a release cannot be published from a state that fails them.

---

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
/aetherius spells            # prints the 19 registered spell keys
/aetherius quests            # prints the 14 quest ids + dependencies
/give @s aetherius:astral_wand
/aetherius magic tension 100 # should trigger a backfire on the next tick
/aetherius dimension         # teleports to aetherius:shattered_sky
/summon aetherius:nox_thal   # walks through the three phase bars
```

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
