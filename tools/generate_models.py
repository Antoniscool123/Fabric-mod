#!/usr/bin/env python3
"""Generates blockstates, block/item models and particle definitions for Aetherius."""
import json
import os

RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "aetherius")


def dump(rel_path, data):
    path = os.path.join(ASSETS, rel_path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


def blockstate_cube_all(name, texture=None, render_type=None):
    texture = texture or name
    model = f"aetherius:block/{name}"
    dump(f"blockstates/{name}.json", {"variants": {"": {"model": model}}})
    dump(f"models/block/{name}.json", {"parent": "minecraft:block/cube_all",
                                       "textures": {"all": f"aetherius:block/{texture}"}})


def blockstate_cube_column(name, side=None, top=None, bottom=None):
    side = side or name
    top = top or f"{name}_top"
    bottom = bottom or top
    dump(f"blockstates/{name}.json", {"variants": {"": {"model": f"aetherius:block/{name}"}}})
    dump(f"models/block/{name}.json", {
        "parent": "minecraft:block/cube_column",
        "textures": {"side": f"aetherius:block/{side}",
                     "end": f"aetherius:block/{top}"}})


def tex(name):
    """Prefixes with the mod namespace unless the caller already gave one (e.g. minecraft:block/dirt)."""
    return name if ":" in name else f"aetherius:block/{name}"


def blockstate_cube_bottom_top(name, top, side, bottom):
    dump(f"blockstates/{name}.json", {"variants": {"": {"model": f"aetherius:block/{name}"}}})
    dump(f"models/block/{name}.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": tex(top), "side": tex(side), "bottom": tex(bottom)}})


def item_generated(name):
    dump(f"models/item/{name}.json", {"parent": "minecraft:item/generated",
                                      "textures": {"layer0": f"aetherius:item/{name}"}})


def item_handheld(name):
    dump(f"models/item/{name}.json", {"parent": "minecraft:item/handheld",
                                      "textures": {"layer0": f"aetherius:item/{name}"}})


def item_block(name, block_model=None):
    dump(f"models/item/{name}.json", {"parent": f"aetherius:block/{block_model or name}"})


def main():
    # ---- blocks ----
    blockstate_cube_all("star_vein_ore")
    blockstate_cube_all("aether_blood_ore")
    blockstate_cube_all("star_ash")
    blockstate_cube_bottom_top("starlit_grass_block", "starlit_grass_block_top",
                               "starlit_grass_block_side", "minecraft:block/dirt")
    blockstate_cube_column("celestial_pillar")
    blockstate_cube_column("aether_altar_core", side="aether_altar_core",
                           top="aether_altar_core_top")
    blockstate_cube_column("telescope", side="telescope", top="celestial_pillar_top")
    blockstate_cube_all("aether_fountain")
    blockstate_cube_all("rune_lantern")
    blockstate_cube_all("sky_rift_anchor")

    # Mana Lily: cross model, cutout render layer.
    dump("blockstates/mana_lily.json", {"variants": {"": {"model": "aetherius:block/mana_lily"}}})
    dump("models/block/mana_lily.json", {"parent": "minecraft:block/cross",
                                         "textures": {"cross": "aetherius:block/mana_lily"},
                                         "render_type": "minecraft:cutout"})

    # Cosmic Forge: lit/unlit variants share a model, with a distinct front face.
    dump("blockstates/aether_forge.json", {
        "variants": {
            "lit=false": {"model": "aetherius:block/aether_forge"},
            "lit=true": {"model": "aetherius:block/aether_forge_lit"}
        }
    })
    for suffix, front in (("", "aether_forge_front"), ("_lit", "aether_forge_front")):
        dump(f"models/block/aether_forge{suffix}.json", {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {
                "top": "aetherius:block/aether_forge_top",
                "side": "aetherius:block/aether_forge_side",
                "bottom": "aetherius:block/aether_forge_side"
            }
        })

    # Sky-Rift portal surface is translucent.
    dump("blockstates/sky_rift_portal.json",
         {"variants": {"": {"model": "aetherius:block/sky_rift_portal"}}})
    dump("models/block/sky_rift_portal.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "aetherius:block/sky_rift_portal"},
        "render_type": "minecraft:translucent"
    })

    # ---- block items ----
    for name in ["star_vein_ore", "aether_blood_ore", "star_ash", "starlit_grass_block",
                 "mana_lily", "aether_fountain", "celestial_pillar", "aether_forge",
                 "aether_altar_core", "telescope", "rune_lantern", "sky_rift_anchor"]:
        item_block(name)

    # ---- plain items ----
    for name in ["aether_shard", "aether_blood", "void_dust", "star_ash_clump",
                 "heart_of_sol", "heart_of_luna", "heart_of_terra", "heart_of_caelum",
                 "astrolabe", "aethersight_monocle", "chronicle_tome",
                 "starlight_elixir", "purified_starlight_elixir"]:
        item_generated(name)
    item_handheld("astral_wand")
    item_handheld("eclipse_wand")

    # ---- spawn eggs (vanilla template) ----
    for egg in ["aether_manta_spawn_egg", "void_sentinel_spawn_egg", "void_specter_spawn_egg",
                "void_warped_abomination_spawn_egg", "nox_thal_spawn_egg"]:
        dump(f"models/item/{egg}.json", {"parent": "minecraft:item/template_spawn_egg"})

    # ---- particles ----
    for particle in ["astral_spark", "void_mote", "sol_ember", "star_dust", "blight_spore"]:
        dump(f"particles/{particle}.json", {"textures": [f"aetherius:{particle}"]})

    print("models + particles written")


if __name__ == "__main__":
    main()
