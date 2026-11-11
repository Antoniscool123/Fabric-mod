#!/usr/bin/env python3
"""Validates resource/datapack JSON shapes that only the game would otherwise catch.

Encodes the three real 1.20.1 bugs found at first launch:
  1. biome "carvers" must be an object ("Not a JSON object: []").
  2. resource locations must contain exactly one colon
     ("aetherius:block/minecraft:block/dirt is not valid resource location").
  3. sounds.json "name" is a path under assets/<ns>/sounds/ without dots and
     without a "sounds/" prefix ("minecraft:sounds/item.firecharge.use.ogg does not exist").
Plus structural checks for dimension types, dimensions, biomes and worldgen entries.
"""
import glob
import json
import os
import re
import sys

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
            if not isinstance(entries, list):
                problems.append(f"{rel}: spawners.{category} is not a list")
            for entry in entries:
                if not isinstance(entry, dict) or "type" not in entry:
                    problems.append(f"{rel}: spawners.{category} entry lacks 'type'")
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


def check_sounds(rel, doc):
    for event, entry in doc.items():
        sounds = entry.get("sounds") if isinstance(entry, dict) else None
        if not isinstance(sounds, list) or not sounds:
            problems.append(f"{rel}: {event} has no sounds list")
            continue
        for sound in sounds:
            name = sound.get("name") if isinstance(sound, dict) else sound
            if not isinstance(name, str):
                problems.append(f"{rel}: {event} has a non-string sound entry")
                continue
            path = name.split(":", 1)[1] if ":" in name else name
            if path.startswith("sounds/"):
                problems.append(f"{rel}: {event} name {name!r} must not start with 'sounds/'")
            if "." in path:
                problems.append(f"{rel}: {event} name {name!r} must use '/' not '.' and no "
                                "'.ogg' suffix")
            if name.count(":") > 1:
                problems.append(f"{rel}: {event} name {name!r} has too many colons")


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
        if rel.endswith("sounds.json"):
            check_sounds(rel.replace(os.sep, "/"), doc)
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
