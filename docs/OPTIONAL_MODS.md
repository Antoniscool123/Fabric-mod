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

`GeckoLib` / `PlayerAnimator` would let the boss be authored in Blockbench with real per-bone
rotation and smooth animation curves. That would look better than hand-placed boxes, but it makes
GeckoLib a **hard dependency**: the jar would refuse to load without it, which breaks the "works
with Fabric API only" rule this mod follows.

If you want that trade, say so and it becomes a deliberate decision for a later release — with
GeckoLib added to `depends` and a Blockbench model replacing `NoxThalModel`. What exists today is a
vanilla `EntityModel` (no extra dependency) that is verified offline with
`tools/preview_model.py` → `docs/img/nox_thal_model.png`.

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
