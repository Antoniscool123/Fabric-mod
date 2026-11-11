#!/usr/bin/env python3
"""Validates resource/datapack JSON shapes that only the game would otherwise catch.

Encodes the three real 1.20.1 bugs found at first launch:
  1. biome "carvers" must be an object ("Not a JSON object: []").
  2. resource locations must contain exactly one colon
     ("aetherius:block/minecraft:block/dirt is not valid resource location").
  3. (retired) we no longer ship sounds.json: every sound is a vanilla SoundEvents alias,
     so no guessed .ogg path can go missing at runtime.
Plus structural checks for dimension types, dimensions, biomes and worldgen entries.
"""
import glob
import json
import os
import re
import sys

SPAWN_CATEGORIES = {"monster", "creature", "ambient", "axolotls", "underground_water_creature",
                    "water_creature", "water_ambient", "misc"}

BASE = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
RES = os.path.join(BASE, "src", "main", "resources")
DATA = os.path.join(RES, "data", "aetherius")
ASSETS = os.path.join(RES, "assets", "aetherius")

problems = []
checked = 0


def load(path):
    try:
        with open(path, encoding="utf-8") as fh:
            return json.load(fh)
    except Exception as exc:  # noqa: BLE001 - report, don't crash
        problems.append(f"{os.path.relpath(path, RES)}: invalid JSON ({exc})")
        return None


def walk_strings(node, path=""):
    if isinstance(node, dict):
        for key, value in node.items():
            yield from walk_strings(value, f"{path}.{key}" if path else key)
    elif isinstance(node, list):
        for index, value in enumerate(node):
            yield from walk_strings(value, f"{path}[{index}]")
    elif isinstance(node, str):
        yield path, node


def check_resource_locations(rel, doc):
    """A resource location has exactly one colon; two means a broken namespace prefix."""
    for where, value in walk_strings(doc):
        if value.count(":") < 2 or any(ch.isspace() for ch in value):
            continue  # prose, descriptions and single-namespace ids are fine
        if re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_/.-]+:[a-z0-9_/.-]+", value):
            problems.append(f"{rel}: {where} = {value!r} is not a valid resource location")


def check_biome(rel, doc):
    carvers = doc.get("carvers")
    if not isinstance(carvers, dict):
        problems.append(f"{rel}: carvers must be an object, got {type(carvers).__name__} "
                        "(1.20.1: {\"air\": []})")
    features = doc.get("features")
    if not isinstance(features, list) or len(features) != 11:
        problems.append(f"{rel}: features must be 11 generation-step lists, got "
                        f"{len(features) if isinstance(features, list) else type(features).__name__}")
    if isinstance(features, list):
        for index, step in enumerate(features):
            if not isinstance(step, list):
                problems.append(f"{rel}: features[{index}] is not a list")
    spawners = doc.get("spawners")
    if not isinstance(spawners, dict):
        problems.append(f"{rel}: spawners must be an object")
    else:
        for category, entries in spawners.items():
            if category not in SPAWN_CATEGORIES:
                problems.append(f"{rel}: unknown spawner category '{category}'")
            if not isinstance(entries, list):
                problems.append(f"{rel}: spawners.{category} is not a list")
                continue
            for entry in entries:
                if not isinstance(entry, dict) or "type" not in entry:
                    problems.append(f"{rel}: spawners.{category} entry lacks 'type'")
                    continue
                for field in ("weight", "minCount", "maxCount"):
                    if not isinstance(entry.get(field), int):
                        problems.append(f"{rel}: spawners.{category} entry {entry.get('type')} "
                                        f"needs an int '{field}'")

    # SpawnCost is a record -> JSON object. A bare number here aborts registry loading with
    # "Not a JSON object: 1.0" (alpha.6 crash).
    costs = doc.get("spawn_costs")
    if not isinstance(costs, dict):
        problems.append(f"{rel}: spawn_costs must be an object")
    else:
        for mob, cost in costs.items():
            if not isinstance(cost, dict):
                problems.append(f"{rel}: spawn_costs.{mob} must be an object "
                                "{energy_budget, charge}, not a bare number")
                continue
            for field in ("energy_budget", "charge"):
                if not isinstance(cost.get(field), (int, float)):
                    problems.append(f"{rel}: spawn_costs.{mob} needs a numeric '{field}'")

    probability = doc.get("creature_spawn_probability")
    if probability is not None and not (isinstance(probability, (int, float))
                                        and 0.0 <= probability <= 1.0):
        problems.append(f"{rel}: creature_spawn_probability must be a number in [0, 1]")
    effects = doc.get("effects")
    if not isinstance(effects, dict):
        problems.append(f"{rel}: effects must be an object")
    else:
        for key in ("sky_color", "fog_color", "water_color"):
            if not isinstance(effects.get(key), int):
                problems.append(f"{rel}: effects.{key} must be an int")
        mood = effects.get("mood_sound")
        if mood is not None:
            for key in ("sound", "tick_delay", "block_search_extent", "offset"):
                if key not in mood:
                    problems.append(f"{rel}: effects.mood_sound missing '{key}'")
    if not isinstance(doc.get("temperature"), (int, float)):
        problems.append(f"{rel}: temperature must be numeric")
    if "has_precipitation" not in doc:
        problems.append(f"{rel}: has_precipitation is required in 1.20.1")


def check_dimension_type(rel, doc):
    required = ["ultrawarm", "natural", "coordinate_scale", "has_skylight", "has_ceiling",
                "ambient_light", "monster_spawn_light_level", "monster_spawn_block_light_limit",
                "piglin_safe", "bed_works", "respawn_anchor_works", "has_raids",
                "logical_height", "min_y", "height", "infiniburn", "effects"]
    for key in required:
        if key not in doc:
            problems.append(f"{rel}: missing required field '{key}'")
    if isinstance(doc.get("min_y"), int) and doc.get("min_y") % 16:
        problems.append(f"{rel}: min_y must be a multiple of 16")
    if isinstance(doc.get("height"), int) and doc.get("height") % 16:
        problems.append(f"{rel}: height must be a multiple of 16")
    logical = doc.get("logical_height")
    height = doc.get("height")
    if isinstance(logical, int) and isinstance(height, int) and logical > height:
        problems.append(f"{rel}: logical_height must be <= height")
    infiniburn = doc.get("infiniburn")
    if isinstance(infiniburn, str) and not infiniburn.startswith("#"):
        problems.append(f"{rel}: infiniburn must be a tag id ('#minecraft:...')")

    # IntProvider codec: int, or {"type": ..., "value": ...}. A flat {"type":..., "min_inclusive":...}
    # parses as "Not a JSON object: null" and aborts registry loading (alpha.6 crash).
    light = doc.get("monster_spawn_light_level")
    if isinstance(light, dict):
        if "type" not in light or "value" not in light:
            problems.append(f"{rel}: monster_spawn_light_level as an IntProvider needs "
                            '{"type": ..., "value": ...}')
        elif not isinstance(light["value"], (int, dict)):
            problems.append(f"{rel}: monster_spawn_light_level.value must be an int or an object")
    elif not isinstance(light, int):
        problems.append(f"{rel}: monster_spawn_light_level must be an int or an IntProvider")
    if not isinstance(doc.get("monster_spawn_block_light_limit"), int):
        problems.append(f"{rel}: monster_spawn_block_light_limit must be an int")
    ambient = doc.get("ambient_light")
    if isinstance(ambient, (int, float)) and not 0.0 <= ambient <= 1.0:
        problems.append(f"{rel}: ambient_light must be in [0, 1]")


def check_dimension(rel, doc):
    for key in ("type", "generator"):
        if key not in doc:
            problems.append(f"{rel}: missing '{key}'")
    generator = doc.get("generator", {})
    for key in ("type", "biome_source"):
        if key not in generator:
            problems.append(f"{rel}: generator missing '{key}'")


def check_placed(rel, doc):
    if "feature" not in doc or not isinstance(doc.get("placement"), list):
        problems.append(f"{rel}: placed feature needs 'feature' and a 'placement' list")
        return
    for index, modifier in enumerate(doc["placement"]):
        if not isinstance(modifier, dict) or "type" not in modifier:
            problems.append(f"{rel}: placement[{index}] lacks 'type'")


def check_configured(rel, doc):
    if "type" not in doc or "config" not in doc:
        problems.append(f"{rel}: configured feature needs 'type' and 'config'")


def check_model(rel, doc):
    parent = doc.get("parent")
    if parent is not None and not re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_/.-]+", parent):
        problems.append(f"{rel}: invalid parent {parent!r}")
    for key, value in (doc.get("textures") or {}).items():
        if not isinstance(value, str):
            continue
        if value.startswith("#"):
            continue
        if not re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_/.-]+", value):
            problems.append(f"{rel}: invalid texture reference {key}={value!r}")


def check_geo(rel, doc):
    """GeckoLib geometry: unique bone names, resolvable parents, sane cubes."""
    geometries = doc.get("minecraft:geometry")
    if not isinstance(geometries, list) or not geometries:
        problems.append(f"{rel}: needs a non-empty 'minecraft:geometry' list")
        return
    for geometry in geometries:
        description = geometry.get("description") or {}
        identifier = description.get("identifier", "")
        if not isinstance(identifier, str) or not identifier.startswith("geometry."):
            problems.append(f"{rel}: description.identifier must start with 'geometry.'")
        for key in ("texture_width", "texture_height"):
            if not isinstance(description.get(key), int):
                problems.append(f"{rel}: description.{key} must be an int")
        bones = geometry.get("bones")
        if not isinstance(bones, list) or not bones:
            problems.append(f"{rel}: geometry has no bones")
            return
        names = {}
        for bone in bones:
            name = bone.get("name")
            if not isinstance(name, str):
                problems.append(f"{rel}: a bone has no name")
                continue
            if name in names:
                problems.append(f"{rel}: duplicate bone '{name}'")
            names[name] = bone
        for name, bone in names.items():
            parent = bone.get("parent")
            if parent is not None and parent not in names:
                problems.append(f"{rel}: bone '{name}' has unknown parent '{parent}'")
            pivot = bone.get("pivot")
            if not isinstance(pivot, list) or len(pivot) != 3:
                problems.append(f"{rel}: bone '{name}' needs a 3-number pivot")
            for cube in bone.get("cubes") or []:
                origin, size, uv = cube.get("origin"), cube.get("size"), cube.get("uv")
                if not isinstance(origin, list) or len(origin) != 3:
                    problems.append(f"{rel}: bone '{name}' has a cube without a 3-number origin")
                if not isinstance(size, list) or len(size) != 3:
                    problems.append(f"{rel}: bone '{name}' has a cube without a 3-number size")
                if not isinstance(uv, list) or len(uv) < 2:
                    problems.append(f"{rel}: bone '{name}' has a cube without a uv origin")


def check_animation(rel, doc, geo_bones):
    """Every animated bone must exist in the matching model — otherwise the animation silently
    does nothing (or crashes, if crashIfBoneMissing is left at its default)."""
    animations = doc.get("animations")
    if not isinstance(animations, dict) or not animations:
        problems.append(f"{rel}: needs an 'animations' object")
        return
    for name, animation in animations.items():
        if not isinstance(animation, dict):
            problems.append(f"{rel}: {name} is not an object")
            continue
        if isinstance(animation.get("animation_length"), bool) or \
                not isinstance(animation.get("animation_length"), (int, float)):
            problems.append(f"{rel}: {name} needs a numeric animation_length")
        bones = animation.get("bones")
        if not isinstance(bones, dict):
            problems.append(f"{rel}: {name} has no bones block")
            continue
        for bone, channels in bones.items():
            if geo_bones and bone not in geo_bones:
                problems.append(f"{rel}: {name} animates '{bone}', which is not a bone in the model")
            for channel, keyframes in channels.items():
                if channel not in ("rotation", "position", "scale"):
                    problems.append(f"{rel}: {name}.{bone} has unknown channel '{channel}'")
                    continue
                if not isinstance(keyframes, dict):
                    problems.append(f"{rel}: {name}.{bone}.{channel} must be keyframe objects")
                    continue
                for time, value in keyframes.items():
                    try:
                        float(time)
                    except ValueError:
                        problems.append(f"{rel}: {name}.{bone}.{channel} key '{time}' is not a time")
                    if channel == "scale":
                        continue
                    if not isinstance(value, list) or len(value) != 3:
                        problems.append(f"{rel}: {name}.{bone}.{channel}['{time}'] must be [x, y, z]")


def check_recipe(rel, doc):
    kind = doc.get("type", "")
    if kind == "minecraft:crafting_shaped":
        pattern = doc.get("pattern") or []
        keys = set((doc.get("key") or {}).keys())
        used = {ch for row in pattern for ch in row if ch != " "}
        if len(pattern) not in (1, 2, 3):
            problems.append(f"{rel}: shaped pattern must have 1-3 rows")
        for row in pattern:
            if not 1 <= len(row) <= 3:
                problems.append(f"{rel}: pattern row {row!r} must be 1-3 wide")
        for missing in sorted(used - keys):
            problems.append(f"{rel}: pattern uses '{missing}' but no key is defined")
        for unused in sorted(keys - used):
            problems.append(f"{rel}: key '{unused}' is defined but never used")
    if "result" not in doc:
        problems.append(f"{rel}: recipe has no result")


def check_blockstate(rel, doc):
    variants = doc.get("variants")
    if variants is not None:
        if not isinstance(variants, dict):
            problems.append(f"{rel}: variants must be an object")
            return
        for key, value in variants.items():
            entries = value if isinstance(value, list) else [value]
            for entry in entries:
                if not isinstance(entry, dict) or "model" not in entry:
                    problems.append(f"{rel}: variant {key!r} lacks 'model'")


def main():
    global checked
    geo_bones = set()
    for path in glob.glob(os.path.join(RES, "assets", "**", "geo", "**", "*.geo.json"),
                          recursive=True):
        doc = load(path)
        if not doc:
            continue
        for geometry in doc.get("minecraft:geometry") or []:
            for bone in geometry.get("bones") or []:
                if isinstance(bone.get("name"), str):
                    geo_bones.add(bone["name"])
    placed_ids = set()
    configured_ids = set()
    for path in glob.glob(os.path.join(DATA, "worldgen", "placed_feature", "*.json")):
        placed_ids.add("aetherius:" + os.path.basename(path)[:-5])
    for path in glob.glob(os.path.join(DATA, "worldgen", "configured_feature", "*.json")):
        configured_ids.add("aetherius:" + os.path.basename(path)[:-5])
    for path in glob.glob(os.path.join(RES, "**", "*.json"), recursive=True):
        rel = os.path.relpath(path, RES)
        doc = load(path)
        if doc is None:
            continue
        checked += 1
        check_resource_locations(rel.replace(os.sep, "/"), doc)
        if "/models/" in rel.replace(os.sep, "/") or rel.startswith("assets"):
            if "models/" in rel.replace(os.sep, "/"):
                check_model(rel.replace(os.sep, "/"), doc)
            if "blockstates/" in rel.replace(os.sep, "/"):
                check_blockstate(rel.replace(os.sep, "/"), doc)
        if "/worldgen/biome/" in rel.replace(os.sep, "/"):
            check_biome(rel.replace(os.sep, "/"), doc)
        if "/dimension_type/" in rel.replace(os.sep, "/"):
            check_dimension_type(rel.replace(os.sep, "/"), doc)
        if rel.replace(os.sep, "/").startswith("data/") and "/dimension/" in rel.replace(os.sep, "/"):
            check_dimension(rel.replace(os.sep, "/"), doc)
        if "/placed_feature/" in rel.replace(os.sep, "/"):
            check_placed(rel.replace(os.sep, "/"), doc)
        if "/configured_feature/" in rel.replace(os.sep, "/"):
            check_configured(rel.replace(os.sep, "/"), doc)
            feature = doc.get("feature")
            if isinstance(feature, str) and feature.startswith("aetherius:") \
                    and feature not in configured_ids:
                problems.append(f"{rel}: unknown configured feature {feature}")
        if "/recipes/" in rel.replace(os.sep, "/"):
            check_recipe(rel.replace(os.sep, "/"), doc)
        normalised = rel.replace(os.sep, "/")
        if normalised.endswith(".geo.json"):
            check_geo(normalised, doc)
        if normalised.endswith(".animation.json"):
            check_animation(normalised, doc, geo_bones)

    # biome feature references must resolve to a placed feature that exists
    for path in glob.glob(os.path.join(DATA, "worldgen", "biome", "*.json")):
        doc = load(path)
        if not doc:
            continue
        rel = os.path.relpath(path, RES).replace(os.sep, "/")
        for step in doc.get("features") or []:
            for entry in step if isinstance(step, list) else []:
                if isinstance(entry, str) and entry.startswith("aetherius:") \
                        and entry not in placed_ids:
                    problems.append(f"{rel}: unknown placed feature {entry}")

    print(f"validated {checked} JSON files")
    if problems:
        print("PROBLEMS:")
        for problem in sorted(set(problems)):
            print(" -", problem)
        sys.exit(1)
    print("data pack and resource JSON shapes look valid")


if __name__ == "__main__":
    main()
