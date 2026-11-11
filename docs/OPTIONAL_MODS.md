# Optional companion mods

Aetherius runs on **Fabric API alone**. Everything in the table below is optional: it changes
how the game *feels*, never whether the mod works. If a helper mod disappears from your pack,
Aetherius does not notice.

Target environment (what the alpha was actually tested on):

| Component | Version |
|---|---|
| Minecraft | 1.20.1 |
| Fabric Loader | 0.20.1 (0.15.11 is the build target) |
| Fabric API | 0.92.12+1.20.1 |
| Java | 17 |

## Recommended (safe, no interaction with Aetherius internals)

| Mod | Version tested | Why |
|---|---|---|
| **Mod Menu** | 7.2.2 | lists Aetherius with its icon, description and entrypoints; pure UI |
| **Iris Shaders** | 1.6.x for 1.20.1 | the Shattered Sky relies on sky colour + fog; shaders make the floating islands read properly |
| **Sodium** | 0.5.x for 1.20.1 | frame time in a dimension full of transparent islands |
| **Indium** | 1.0.x | keeps Aetherius' translucent `rendertype_entity_translucent_emissive` entities (Nox'Thal, Aether Manta) rendering correctly on Sodium |
| **Jade** | 11.x | block/entity tooltips while testing `/aetherius blight`, ore spawns and boss phases |
| **Xaero's Minimap** | 23.x | the dimension is 48-block-radius-per-arena large; a map saves a lot of walking |

> If you install **Sodium without Indium**, translucent mobs can render as opaque blocks. That is a
> Sodium limitation, not an Aetherius bug — add Indium.

## Optional, only if you extend the mod

| Mod | Why |
|---|---|
| **Fabric Language Kotlin** | required only if you add Kotlin helper mods to your pack |
| **Cloth Config API** | the client options screen is a stub; this is what it will be built on |
| **Architectury API** | only if you load other Architectury-based mods alongside |

## What Aetherius does *not* need

- No TerraBlender. Biome injection into vanilla worldgen used to be listed as a dependency;
  it is not. The mod ships its own dimension and a runtime blight system instead.
- No resource pack, no datapack, no shader pack of its own.
- No `sounds.json` — all audio is aliased from vanilla `SoundEvents`, so no missing `.ogg` files.

## If something looks wrong

1. Remove every optional mod except Fabric API and retest. If the problem disappears, it was an
   interaction; note which mod.
2. Keep the launcher log — the lines starting with `[Aetherius]` are enough to tell initialisation
   failures apart from rendering failures.
3. `docs/VERIFICATION.md` lists what has actually been verified versus what is untested.
