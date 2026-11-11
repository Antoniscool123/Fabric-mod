# Changelog

All notable changes to Aetherius: Echoes of the Shattered Sky.
The mod is in alpha: versions are `1.0.0-alpha.N`, each one a playable checkpoint rather than a
finished product. Jars are attached to the matching GitHub release.

## [1.0.0-alpha.10] — 2026-11-11

### Changed — ores and grass are vanilla-shaped now

The first procedural pass made blocks from smooth value noise, which reads as "soft" next to
vanilla art. The blocks a player sees next to vanilla content were rewritten as **vanilla-style
pixel art**, keeping the mod's palette:

| Texture | Structure now |
|---|---|
| `star_vein_ore`, `aether_blood_ore` | vanilla stone base (three-tone speckle, 2x2 pebbles) with 6 clusters of 2x2-4x3 crystal pixels: dark rim, solid body, one bright glint. The stone base deliberately uses vanilla stone tones, because these ores generate inside vanilla stone |
| `starlit_grass_block_top` | vanilla grass layout — an even carpet with scattered tufts and a few star-bright pixels |
| `starlit_grass_block_side` | vanilla dirt tones with the ragged 3-5 pixel grass lip, in starlit blue |
| `star_ash` | vanilla gravel/sand structure: pale three-tone speckle with darker pebbles |

Everything else keeps the noise-and-lighting treatment, which suits the mod's own blocks and items.
`tools/texture_sheet.py` builds `docs/img/texture_sheet.png` (every texture at 5x) so the set can be
reviewed in one image.

## [1.0.0-alpha.9] — 2026-11-11

### Fixed — the vanilla boss bar no longer sits under the custom one

`NoxThalEntity` kept a `ServerBossBar` and added every tracking player to it, so the client drew
the vanilla purple bar *and* the custom RGB overlay on top of each other. The `ServerBossBar` is
gone; the custom HUD is fed by `BossEventS2CPacket` (phase colour, Supernova darkness) plus the
entity's synced health, which is what it was already using for the pixels.

### Added — a Guide tab in the Chronicle

`K` now has a fifth tab, **Guide**, with five pages: getting started, runes and spells, the
Shattered Sky, the Nox'Thal, and forges/altars/lanterns. The text lives in `GuidePages` and
`en_us.json`, so `tools/check_lang.py` proves every key exists, and every statement was checked
against the code it documents (wand recipe, `R`/`Shift+R`, the 50/100 tension thresholds, the
four-anchor portal, the gravity-anchor grounding, the invulnerable phase transition).

### Changed — every texture regenerated

`tools/generate_textures.py` was rewritten around three layers instead of flat colour fills:

1. **tileable value noise** (`fbm`) for the material, so blocks do not seam;
2. **structure** — ore clusters with rim + specular, a ragged grass lip, pillar flutes, forge
   grille and rivets, lantern frame, item silhouettes;
3. **lighting** — bevels on block faces, outlines and top-left highlights on items, vignettes.

All 47 PNGs were regenerated, including the 64x64 entity skins, the 256x256 Nox'Thal atlas (same
uv regions the model names) and the GUI bars.

### Changed — the Shattered Sky generates normal terrain

The floating-island field is gone. `ShatteredSkyChunkGenerator` now builds continental terrain
from three value-noise octaves (`worldgen/SkyNoise`): hills, lowlands that flood at sea level 76,
sand beaches, stone/deepslate layers, ore veins, and a flat plateau around the origin so arrivals
and the boss arena always land on solid ground. Oaks grow in the meadows, celestial pillars stand
in the highlands, mana lilies near grass.

The dimension was using `minecraft:fixed` (a single biome). It now uses
`aetherius:shattered_sky`, a custom `BiomeSource` whose regions come from the same noise the
terrain uses - so all three biomes actually occur, and the biome always matches the ground:
`starlit_meadows` low and green, `shattered_sky` above y 104, `blighted_barrens` wherever the
blight field wins.

### Still unverified

Compile and packaging are CI-verified; no world has been created with `alpha.9` yet, so the new
terrain, the biome distribution, the guide pages and the boss-bar fix are all pending a live run.

## [1.0.0-alpha.8] — 2026-11-11

### Changed — the Nox'Thal is a GeckoLib model now

The boss is no longer ~600 lines of hand-written cuboids. It is a Blockbench-style data model plus
an animation file, which is what the user asked for after three passes of the Java model still not
looking right in game.

| Piece | Path |
|---|---|
| Model — 10 bones, 50 cubes, feet at y = 0, 3.19 blocks tall, 7-block wingspan | `assets/aetherius/geo/entity/nox_thal.geo.json` |
| Animations — `flight`, `heavy_flight`, `grounded`, `supernova`, `attack` | `assets/aetherius/animations/entity/nox_thal.animation.json` |
| `DefaultedEntityGeoModel` (head bone `head`) + `GeoEntityRenderer` (shadow 2.2, phase-3 roll in `preRender`) | `client/render/NoxThalGeoModel`, `NoxThalRenderer` |
| `GeoEntity` implementation: `RawAnimation`s, instance cache, controller `"main"`, `triggerableAnim("attack", …)` fired by the cosmic breath | `entity/NoxThalEntity` |

`NoxThalModel` (the `EntityModel` version) and its `EntityModelLayerRegistry` entry are gone.

### Breaking — GeckoLib 4.8.4+ is now a hard dependency

`fabric.mod.json` moved `geckolib` from `suggests` to `depends` (`">=4.8.0"`). A profile without it
now fails with the normal Fabric "missing dependency" screen instead of crashing inside the
renderer. **Install `geckolib-fabric-1.20.1-4.8.4.jar` (or newer 4.8.x) for 1.20.1.**

### Fixed — the toolchain

GeckoLib 4.8.4 is built with Fabric Loom 1.10.5, and Loom refuses to remap a mod built with a newer
Loom than the project uses. The build failed with a misleading `ChunkGenerator` error until the log
was read to the end (`Mod was built with a newer version of Loom (1.10.5), you are using Loom
(1.5.8)`). Now: **Loom 1.10.5 + Gradle 8.14**, and the GeckoLib repository is restricted to its own
group with `exclusiveContent`, so it can no longer shadow a Minecraft or mapping artifact.

### Tooling

- `tools/preview_model.py` was rewritten for the data model: it rasterises any `.geo.json` (with any
  animation at any timestamp) to a PNG — `docs/img/nox_thal_model.png`, `docs/img/nox_thal_flight.png`.
- `tools/check_data.py` gained `check_geo` and `check_animation`: every animated bone must exist in
  the model, every keyframe must be a valid channel shape, and the geometry must carry a real uv
  layout. 112 JSON files now validate before every build.
- CI's jar gate also asserts that both GeckoLib assets are inside the release jar.

### Still unverified

GeckoLib compiles and packs, but no world has been loaded with `alpha.8` yet — see
`docs/VERIFICATION.md` §0e.

## [1.0.0-alpha.7] — 2026-11-11

### Fixed — world creation crash (`Failed to load registries`)

alpha.6 shipped two invalid codec shapes, both of which abort registry loading before a world can
be created:

| Symptom in the log | Cause | Fix |
|---|---|---|
| `Failed to parse aetherius:dimension_type/shattered_sky.json` … `Not a JSON object: null` | `monster_spawn_light_level` was a flat `{"type":"uniform","min_inclusive":0,"max_inclusive":7}`; 1.20.1's `IntProvider` codec wants the range nested under `value` | `{"type":"minecraft:uniform","value":{"min_inclusive":0,"max_inclusive":7}}` |
| `Failed to parse …/biome/*.json` … `Not a JSON object: 1.0` / `1.5` / `0.6` | `spawn_costs` was written as `{"mob": 1.0}`; vanilla's `SpawnCost` is a record, so it must be an object | `{"mob": {"energy_budget": 0.7, "charge": 0.15}}` |

Both are now guarded by `tools/check_data.py`, which validates every `IntProvider` shape, every
`spawn_costs` entry, spawner categories/weights and `creature_spawn_probability` before compile —
so this class of bug cannot reach a release again. The alpha.6 shapes were re-injected into the
datapack to prove the checker catches them.

## [1.0.0-alpha.6] — 2026-11-11

### Fixed

- **Nothing spawns in the void any more.** No Aetherius entity had a `SpawnRestriction`, which
  vanilla reads as "spawns anywhere the position is free" — in a sky dimension that is the entire
  256-block column. All ten types now register one: walkers require a solid block under the feet,
  fliers require island ground within 24 blocks plus headroom, the boss and gravity anchor require
  open sky at least 6 blocks above an island. Vanilla's spawn machinery does the rest.
- **Specters no longer sink for ever.** They had `setNoGravity(true)` plus a constant downward
  drift, so an idle specter fell into the void. They now hold an altitude band over their island
  and are removed if they end up in the void.
- **Mantas no longer climb out of the world.** They only ever gained height; they now hold a band
  around 12 blocks above the island ground.
- **Monsters can actually spawn on the islands.** `monster_spawn_light_level` was `0` while the
  dimension has skylight, so the light check could never pass. It is now a 0-7 uniform band.
- Biome spawn budgets: `spawn_costs` per mob plus `creature_spawn_probability: 0.08`.
- **Nox'Thal model, third pass.** The head was a sibling of the neck (so neck rotation tore it
  off) and torso bobbing moved the torso alone. The head is now parented to the neck, whole-body
  motion is applied to the model root, and the geometry is rebuilt as: tapered chest+hips torso
  with spine plates and a shoulder hump, a two-step forward-leaning neck, a 14x13x12 skull with
  brow, jaw, snout, crest and swept horns, four-bone wings with four membrane panels and claws, a
  three-segment tail with a vertical fin, and four two-bone legs landing exactly on the ground
  plane. Renderer scale is now 1.0 — the model is authored at true size (2.8 blocks tall,
  5.9 block wingspan) with shadow radius 2.2.

### Added

- `tools/preview_model.py` — rasterises `NoxThalModel`'s cuboids offline into
  `docs/img/nox_thal_model.png` (side, three-quarter, top views, airborne pose applied). Both model
  bugs above were found with it, before a release.
- `fabric.mod.json` now *suggests* (never requires) Mod Menu, Cloth Config, Sodium, Indium, Iris,
  Jade and Xaero's Minimap, and dropped the stale TerraBlender suggestion.
- `docs/OPTIONAL_MODS.md` rewritten: what to install, what to skip, and why a hard dependency on an
  animation library (GeckoLib) is a choice rather than a default.

## [1.0.0-alpha.5] — 2026-11-11

Playability patch. The world already loaded in alpha.4; this release makes the things you can
actually click on work.

### Fixed

- **The Chronicle (K) is interactive again.** `ChronicleScreen.init()` used to ask the server to
  open the book, the server replied "open", and the client built a brand new screen on top — over
  and over. Every click, the Close button and even ESC were undone within a frame. `ACTION_OPEN`
  is now a pure snapshot request, and `OPEN_CHRONICLE` is ignored while a screen is already open.
  Tabs, quest rows, tracking and Close all respond; ESC closes the book.
- **Nox'Thal no longer floats apart.** The model mixed body-space and part-space offsets: the head
  sat about 46 model pixels above the neck, and two head boxes were anchored to a different origin
  than the skull. The model is rebuilt in absolute model space with pivots on the real joints
  (shoulders, neck base, neck top, tail root), the skull/snout/horns are one part so head rotation
  cannot split them, the atlas is a matching 256×256 sheet, and the hitbox is 3.0 × 3.2 so the
  nameplate, shadow and hit tests line up with the silhouette. Wings, tail, neck and a gentle
  hover bob are still animated per boss phase.
- **`/aetherius dimension` exists.** It was documented but never registered. Now there is
  `dimension` (teleport onto the Shattered Sky surface) and `dimension return` (back to the
  Overworld spawn). The root command no longer requires op, so travelling and reading your own
  state work in singleplayer without cheats; every mutating subcommand re-checks permission
  level 2. `/aetherius spells` and `/aetherius quests` are new read-only shortcuts.

### Changed

- **No more missing sound files.** `sounds.json` guessed vanilla `.ogg` paths (`item/firecharge_use`,
  `entity/warden/sonic_boom`, …), which produced 12 warnings per session, and 14 mod-owned sound
  events had no audio at all (`Unable to play empty soundEvent: aetherius:boss_nox_roar`). Both are
  gone: `ModSounds` now aliases real vanilla `SoundEvents`, so the compiler guarantees every sound
  exists and the mod ships no `sounds.json`.
- Chronicle Tome use no longer sends a redundant client→server open request; the server answers
  with open + snapshot in one round trip.
- CI jar gate now checks `textures/entity/nox_thal.png` instead of the removed `sounds.json`.

### Added

- `CHANGELOG.md` (this file).
- `docs/OPTIONAL_MODS.md` — exactly which companion mods are safe, with tested versions, and what
  Aetherius explicitly does *not* need (no TerraBlender, no resource pack).
- README command table with the real permission level of every subcommand.

### Verified

- All six offline checkers green: `check_java`, `check_api`, `check_refs`, `check_lang`,
  `check_chronicle`, `check_data` (110 JSON files).
- `docs/VERIFICATION.md` §0c records the alpha.4 live log and the root cause of each bug above.

### Known gaps

- No one has played the story end-to-end: quest rewards, boss phases in real combat and the Cosmic
  Forge are still only statically verified.
- Textures and models remain generated placeholders.
- The biome *injection* into vanilla worldgen still needs TerraBlender or a datapack; the mod's own
  answer is the runtime blight system plus its own dimension.

## [1.0.0-alpha.4] — 2026-11-11

- Fixed the fatal world-load crash: biome JSON emitted `"carvers": []` where 1.20.1 needs an
  object; block models double-prefixed the namespace (`aetherius:block/minecraft:block/dirt`).
- Added `tools/check_data.py` (biome/dimension/model/blockstate/recipe/feature shapes) as a sixth
  CI checker; removed two orphaned loot tables and an unused recipe key.
- Mood sounds unified on `minecraft:ambient.cave` so every biome resolves.

## [1.0.0-alpha.3] — 2026-11-10

- Worldgen moved to datapack JSON (dimension, dimension type, three biomes, placed/configured
  features) instead of code registration.
- Fixed the gravity mixin so it cannot fail to apply (`require = 0`).

## [1.0.0-alpha.2] — 2026-11-10

- First CI-packaged jar: 131 Java files, 18 spells, 14 quests, 12 lore entries, 3 biomes, 1
  dimension, 48 textures, 65 models.

## [1.0.0-alpha.1] — 2026-11-09

- Initial scaffold: registries, magic system, runes, chronicle, blight, boss AI, four mixins,
  client HUD and screens.
