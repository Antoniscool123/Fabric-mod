# Optional companion mods

Aetherius runs on **Fabric API alone**. Everything below is optional: it changes how the game feels,
never whether the mod works. If a helper mod disappears from your pack, Aetherius does not notice.

Tested environment: Minecraft **1.20.1**, Fabric Loader **0.20.1** (build target 0.15.11),
Fabric API **0.92.x**, Java **17**.

## Recommended for your pack — safe, no Aetherius internals touched

| Mod | Version for 1.20.1 | Why |
|---|---|---|
| **Mod Menu** | 7.2.2 | lists Aetherius with icon and description; pure UI |
| **Iris Shaders** | 1.6.x | the Shattered Sky is built around sky colour and fog; shaders make the floating islands read |
| **Sodium** | 0.5.x | frame time in a dimension full of transparent geometry |
| **Indium** | 1.0.x | **required if you use Sodium** — otherwise translucent Aetherius mobs (Nox'Thal, Aether Manta) render as opaque blobs |
| **Jade** | 11.x | block/entity tooltips while testing `/aetherius blight`, ore spawns and boss phases |
| **Xaero's Minimap** | 23.x | the dimension is large and the islands are sparse |

Aetherius declares these in `fabric.mod.json` under `suggests`, so they are never required and never
block loading.

## Optional config/UI libraries

| Mod | When it matters |
|---|---|
| **Cloth Config API** | there is no server config screen yet; Cloth is what a future client options screen would be built on |
| **YACL (YetAnotherConfigLib)** | alternative to Cloth, same purpose |
| **owo-lib** | only if you load other owo-based mods |

## The animation-library question (GeckoLib)

**Decided in `alpha.8`: GeckoLib is now required.**

The trade was made deliberately — the user asked for it after the hand-built `EntityModel` kept
looking wrong in game. `fabric.mod.json` now lists `"geckolib": ">=4.8.0"` under `depends`, so a
profile without GeckoLib fails at load time with a clear missing-dependency message instead of a
crash somewhere in the renderer.

What that bought:

| Before (`alpha.7`) | Now (`alpha.8`) |
|---|---|
| `NoxThalModel` — 50 cuboids created in Java, per-part pivots in *part space* | `assets/aetherius/geo/entity/nox_thal.geo.json` — 10 bones, 50 cubes, absolute model-space pivots, editable in Blockbench |
| Movement faked by rotating parts in a `setAngles` branch | `assets/aetherius/animations/entity/nox_thal.animation.json` — `flight`, `heavy_flight`, `grounded`, `supernova`, `attack`, driven by an `AnimationController` |
| Phase 3 roll needed a manual `PoseStack.rotate` | still a `PoseStack` roll in `NoxThalRenderer#preRender`, on top of GeckoLib's pose |

`tools/preview_model.py` was rewritten for the data model: it rasterises the `.geo.json` (and any
animation, at any time) into `docs/img/nox_thal_model.png` /
`docs/img/nox_thal_flight.png`, so pivot mistakes are caught before a release.

**Toolchain consequence:** GeckoLib 4.8.4 is built with Fabric Loom 1.10.5, and Loom refuses to
remap a mod built with a *newer* Loom than itself. The project therefore moved from Loom 1.5.8 /
Gradle 8.6 to **Loom 1.10.5 / Gradle 8.14**. Keep these two numbers in step if GeckoLib is ever
updated.

## What Aetherius explicitly does *not* need

- **TerraBlender** — no longer suggested. Aetherius ships its own dimension and uses a runtime blight
  system instead of injecting biomes into vanilla worldgen.
- No resource pack, datapack or shader pack of its own.
- No `sounds.json`: every sound is an alias of a vanilla `SoundEvents` constant, so no missing `.ogg`
  files and no empty sound events.

## If something looks wrong

1. Remove every optional mod except Fabric API and retest. If the problem goes away, it was an
   interaction; note which mod.
2. Keep the launcher log — the `[Aetherius]` lines separate initialisation failures from rendering
   failures.
3. `docs/VERIFICATION.md` records what has been verified (§0b-0d) versus what is still untested.
