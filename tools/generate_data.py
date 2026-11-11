#!/usr/bin/env python3
"""Generates data-pack content: dimension, biomes, loot tables, recipes and tags."""
import json
import os

RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "src", "main", "resources")
DATA = os.path.join(RES, "data")


def dump(rel, data, namespace="aetherius"):
    path = os.path.join(DATA, namespace, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


EMPTY_SPAWNERS = {k: [] for k in ["monster", "creature", "ambient", "axolotls",
                                  "underground_water_creature", "water_creature",
                                  "water_ambient", "misc"]}


def biome(temperature, downfall, sky, fog, water, features, spawners=None, music=None,
          spawn_costs=None):
    body = {
        "temperature": temperature,
        "downfall": downfall,
        "has_precipitation": temperature < 0.9,
        "effects": {
            "sky_color": sky,
            "fog_color": fog,
            "water_color": water,
            "water_fog_color": 0x2A1B44,
        },
        "carvers": {"air": []},
        "features": features,
        "spawners": spawners or EMPTY_SPAWNERS,
        # Vanilla's spawn budget: a mob of this type already nearby makes the next one of the
        # same type less likely. Without entries the floating-island biomes filled up fast.
        "spawn_costs": spawn_costs or {},
        "creature_spawn_probability": 0.08,
    }
    if music:
        body["effects"]["mood_sound"] = {
            "sound": music,
            "tick_delay": 6000,
            "block_search_extent": 8,
            "offset": 2.0,
        }
    return body


def eleven(*layers):
    """Pads a partial feature-step list to the 11 vanilla generation steps."""
    out = [[] for _ in range(11)]
    for index, entries in layers:
        out[index] = entries
    return out


def block_loot(drops, silk_touch_self=True, name=None):
    """drops = list of (item, min, max)."""
    entries = []
    for item, low, high in drops:
        entry = {"type": "minecraft:item", "name": item}
        if (low, high) != (1, 1):
            entry["functions"] = [{"function": "minecraft:set_count",
                                   "count": {"type": "minecraft:uniform",
                                             "min": low, "max": high}}]
        entries.append(entry)
    pool = {
        "rolls": 1,
        "entries": entries if len(entries) > 1 else entries,
        "conditions": [{"condition": "minecraft:survives_explosion"}],
    }
    return {"type": "minecraft:block", "pools": [pool]}


def simple_self(name, extra=None):
    drops = [(f"aetherius:{name}", 1, 1)]
    if extra:
        drops += extra
    return block_loot(drops)


def main():
    # ---------------------------------------------------------------- dimension
    dump("dimension_type/shattered_sky.json", {
        "ultrawarm": False,
        "natural": False,
        "coordinate_scale": 1.0,
        "has_skylight": True,
        "has_ceiling": False,
        "ambient_light": 0.14,
        "fixed_time": 15000,
        # Sky light in a dimension with has_skylight = true is always 15, so a light level of 0
        # would mean "monsters can never spawn on the islands". A 0-7 uniform band gives the
        # eternal twilight the intended feel: monsters appear in shadowed spots, not in daylight.
        "monster_spawn_light_level": {"type": "minecraft:uniform",
                                      "min_inclusive": 0, "max_inclusive": 7},
        "monster_spawn_block_light_limit": 0,
        "piglin_safe": False,
        "bed_works": False,
        "respawn_anchor_works": False,
        "has_raids": False,
        "logical_height": 256,
        "min_y": 0,
        "height": 256,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "effects": "minecraft:the_end"
    })

    dump("dimension/shattered_sky.json", {
        "type": "aetherius:shattered_sky",
        "generator": {
            "type": "aetherius:shattered_sky",
            "biome_source": {"type": "minecraft:fixed", "biome": "aetherius:shattered_sky"}
        }
    })

    # ---------------------------------------------------------------- biomes
    dump("worldgen/biome/starlit_meadows.json", biome(
        0.7, 0.8, 0x6FB7FF, 0xBBD9FF, 0x3C7FD1,
        eleven((4, ["aetherius:starlight_observatory"]),
               (6, ["aetherius:mana_lily_patch"])),
        spawners={**EMPTY_SPAWNERS,
                  "creature": [{"type": "aetherius:aether_manta", "weight": 8,
                                "minCount": 1, "maxCount": 3}],
                  "ambient": [{"type": "minecraft:bat", "weight": 4,
                               "minCount": 1, "maxCount": 2}]},
        music="minecraft:ambient.cave",
        spawn_costs={"minecraft:bat": 1.0}))

    dump("worldgen/biome/shattered_sky.json", biome(
        0.3, 0.2, 0x1A1030, 0x2B1A4A, 0x4A2F82,
        eleven((6, ["aetherius:mana_lily_patch"])),
        spawners={**EMPTY_SPAWNERS,
                  "creature": [{"type": "aetherius:aether_manta", "weight": 12,
                                "minCount": 1, "maxCount": 2}],
                  "monster": [{"type": "aetherius:void_specter", "weight": 10,
                               "minCount": 1, "maxCount": 3},
                              {"type": "aetherius:void_sentinel", "weight": 3,
                               "minCount": 1, "maxCount": 1}]},
        music="minecraft:ambient.cave",
        spawn_costs={"aetherius:void_specter": 1.0, "aetherius:void_sentinel": 2.0,
                     "aetherius:aether_manta": 0.6}))

    dump("worldgen/biome/blighted_barrens.json", biome(
        1.1, 0.1, 0x2A1030, 0x3A1030, 0x5A1F3A,
        eleven((9, ["aetherius:mana_lily_patch"])),
        spawners={**EMPTY_SPAWNERS,
                  "monster": [{"type": "aetherius:void_warped_abomination", "weight": 20,
                               "minCount": 2, "maxCount": 5},
                              {"type": "aetherius:void_specter", "weight": 12,
                               "minCount": 1, "maxCount": 3}]},
        music="minecraft:ambient.cave",
        spawn_costs={"aetherius:void_warped_abomination": 1.5, "aetherius:void_specter": 1.0}))

    # ---------------------------------------------------------------- loot
    dump("loot_tables/blocks/star_vein_ore.json", block_loot(
        [("aetherius:aether_shard", 2, 5), ("aetherius:aether_blood", 1, 2)]))
    dump("loot_tables/blocks/aether_blood_ore.json", block_loot(
        [("aetherius:aether_blood", 1, 3)]))
    for name in ["star_ash", "starlit_grass_block", "mana_lily", "aether_fountain",
                 "celestial_pillar", "aether_forge", "aether_altar_core", "telescope",
                 "rune_lantern"]:
        dump(f"loot_tables/blocks/{name}.json", simple_self(name))
    dump("loot_tables/blocks/sky_rift_anchor.json", simple_self(
        "sky_rift_anchor", [("minecraft:crying_obsidian", 1, 2)]))


    # ---------------------------------------------------------------- recipes
    def shaped(pattern, key, result, count=1):
        return {"type": "minecraft:crafting_shaped", "pattern": pattern,
                "key": {k: {"item": v} for k, v in key.items()},
                "result": {"item": result, "count": count}}

    def shapeless(ingredients, result, count=1):
        return {"type": "minecraft:crafting_shapeless",
                "ingredients": [{"item": i} for i in ingredients],
                "result": {"item": result, "count": count}}

    recipes = {
        "astral_wand": shaped(["  S", " A ", "A  "],
                              {"S": "minecraft:stick", "A": "aetherius:aether_shard"},
                              "aetherius:astral_wand"),
        "astrolabe": shaped([" G ", "GAG", " M "],
                            {"G": "minecraft:gold_ingot", "A": "aetherius:aether_shard",
                             "M": "minecraft:amethyst_shard"}, "aetherius:astrolabe"),
        "chronicle_tome": shapeless(["minecraft:book", "aetherius:aether_shard",
                                     "minecraft:ink_sac"], "aetherius:chronicle_tome"),
        "starlight_elixir": shapeless(["minecraft:glass_bottle", "aetherius:aether_shard",
                                       "minecraft:glowstone_dust"], "aetherius:starlight_elixir"),
        "purified_starlight_elixir": shapeless(["aetherius:starlight_elixir",
                                                "aetherius:aether_blood"],
                                               "aetherius:purified_starlight_elixir"),
        "aethersight_monocle": shaped([" A ", "GAG", " A "],
                                      {"A": "aetherius:aether_shard",
                                       "G": "minecraft:gold_ingot"},
                                      "aetherius:aethersight_monocle"),
        "celestial_pillar": shaped(["QAQ", "QLQ", "QAQ"],
                                   {"Q": "minecraft:quartz_block", "A": "aetherius:aether_shard",
                                    "L": "minecraft:lapis_block"}, "aetherius:celestial_pillar", 4),
        "aether_forge": shaped(["ALA", "LDL", "ALA"],
                               {"A": "aetherius:aether_shard", "L": "minecraft:lapis_block",
                                "D": "minecraft:deepslate"}, "aetherius:aether_forge"),
        "aether_altar_core": shaped(["AAA", "AQA", "AAA"],
                                     {"A": "aetherius:aether_shard",
                                      "Q": "minecraft:quartz_block"},
                                     "aetherius:aether_altar_core"),
        "aether_fountain": shaped(["LAL", "AQA", "LAL"],
                                  {"L": "minecraft:lapis_block", "A": "aetherius:aether_shard",
                                   "Q": "minecraft:quartz_block"}, "aetherius:aether_fountain"),
        "rune_lantern": shaped([" G ", "GLG", " A "],
                               {"G": "minecraft:gold_nugget", "L": "minecraft:glowstone",
                                "A": "aetherius:aether_shard"}, "aetherius:rune_lantern", 2),
        "telescope": shaped([" AC", "ACC", "CCC"],
                            {"A": "aetherius:aether_shard", "C": "minecraft:copper_ingot"},
                            "aetherius:telescope"),
        "sky_rift_anchor": shaped(["CBC", "BAB", "CBC"],
                                  {"C": "minecraft:crying_obsidian",
                                   "B": "aetherius:aether_blood",
                                   "A": "aetherius:aether_shard"},
                                  "aetherius:sky_rift_anchor"),
        "aether_shard_from_block": shapeless(["aetherius:aether_blood"], "aetherius:aether_shard", 2),
    }
    for name, recipe in recipes.items():
        dump(f"recipes/{name}.json", recipe)

    # ------------------------------------------------- worldgen: features
    def ore_configured(size, targets):
        return {
            "type": "minecraft:ore",
            "config": {
                "size": size,
                "discard_chance_on_air_exposure": 0.0,
                "targets": [
                    {"target": {"predicate_type": "minecraft:block_match", "block": block},
                     "state": {"Name": state}}
                    for block, state in targets
                ],
            },
        }

    def plain_configured(feature_type):
        return {"type": feature_type, "config": {}}

    def placed(feature, placement):
        return {"feature": feature, "placement": placement}

    star_vein_configured = ore_configured(6, [
        ("minecraft:stone", "aetherius:star_vein_ore"),
        ("minecraft:deepslate", "aetherius:star_vein_ore"),
    ])
    blood_configured = ore_configured(3, [
        ("minecraft:deepslate", "aetherius:aether_blood_ore"),
    ])

    dump("worldgen/configured_feature/star_vein_ore.json", star_vein_configured)
    dump("worldgen/configured_feature/aether_blood_ore.json", blood_configured)
    dump("worldgen/configured_feature/starlight_observatory.json",
         plain_configured("aetherius:starlight_observatory"))
    dump("worldgen/configured_feature/mana_lily_patch.json",
         plain_configured("aetherius:mana_lily_patch"))

    def height_range(low, high):
        return {
            "type": "minecraft:height_range",
            "height": {
                "type": "minecraft:uniform",
                "min_inclusive": {"absolute": low},
                "max_inclusive": {"absolute": high},
            },
        }

    dump("worldgen/placed_feature/star_vein_ore.json", placed("aetherius:star_vein_ore", [
        {"type": "minecraft:count", "count": 6},
        {"type": "minecraft:in_square"},
        height_range(-48, 24),
        {"type": "minecraft:biome"},
    ]))
    dump("worldgen/placed_feature/aether_blood_ore.json", placed("aetherius:aether_blood_ore", [
        {"type": "minecraft:count", "count": 3},
        {"type": "minecraft:in_square"},
        height_range(-60, -12),
        {"type": "minecraft:biome"},
    ]))
    dump("worldgen/placed_feature/starlight_observatory.json", placed("aetherius:starlight_observatory", [
        {"type": "minecraft:rarity_filter", "chance": 140},
        {"type": "minecraft:in_square"},
        height_range(60, 140),
        {"type": "minecraft:biome"},
    ]))
    dump("worldgen/placed_feature/mana_lily_patch.json", placed("aetherius:mana_lily_patch", [
        {"type": "minecraft:count", "count": 8},
        {"type": "minecraft:in_square"},
        height_range(60, 120),
        {"type": "minecraft:biome"},
    ]))

# ---------------------------------------------------------------- tags
    pickaxe = [f"aetherius:{b}" for b in
               ["star_vein_ore", "aether_blood_ore", "celestial_pillar", "aether_forge",
                "aether_altar_core", "telescope", "aether_fountain", "sky_rift_anchor"]]
    dump("tags/blocks/mineable/pickaxe.json", {"values": pickaxe}, namespace="minecraft")
    dump("tags/blocks/needs_iron_tool.json",
         {"values": ["aetherius:star_vein_ore", "aetherius:aether_forge",
                     "aetherius:aether_altar_core", "aetherius:sky_rift_anchor"]},
         namespace="minecraft")
    dump("tags/blocks/needs_stone_tool.json",
         {"values": ["aetherius:aether_blood_ore", "aetherius:celestial_pillar"]},
         namespace="minecraft")
    dump("tags/blocks/star_vein_ore.json", {"replace": False, "values": []})

    print("data pack written")


if __name__ == "__main__":
    main()
