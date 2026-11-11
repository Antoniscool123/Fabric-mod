#!/usr/bin/env python3
"""Finds translation keys used in Java that are missing from en_us.json."""
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "java", "com", "aetherius")
BASE = os.path.abspath(os.path.join(ROOT, *[".."] * 5))  # repo root
LANG = os.path.join(BASE, "src", "main", "resources", "assets", "aetherius", "lang", "en_us.json")

keys = set(json.load(open(LANG, encoding="utf-8")))
print(f"lang keys: {len(keys)}")

pattern = re.compile(r'(?:Aeth\.lang|Text\.translatable)\(\s*"([^"]+)"')
used = {}
for dirpath, _, files in os.walk(ROOT):
    for name in files:
        if name.endswith(".java"):
            path = os.path.join(dirpath, name)
            src = open(path, encoding="utf-8").read()
            for key in pattern.findall(src):
                used.setdefault(key, set()).add(os.path.relpath(path, ROOT))

DYNAMIC_PREFIXES = ("aetherius.boss.nox_thal.phase",
                   "item.aetherius.heart_of_",
                   "item.aetherius.wand.tier.")
missing = {k: v for k, v in used.items()
           if k not in keys and not any(k.startswith(pref) for pref in DYNAMIC_PREFIXES)}
dynamic = {k: v for k, v in missing.items() if not k.endswith(".")}
print(f"static keys referenced: {len(used)}")
if missing:
    print("MISSING KEYS:")
    for k in sorted(missing):
        print(f" - {k}  ({sorted(missing[k])[0]})")
# registry ids must have translation keys
registry_checks = 0
problems = []
import glob
for path in glob.glob(os.path.join(ROOT, "registry", "Mod*.java")):
    src = open(path, encoding="utf-8").read()
    name = os.path.basename(path)
    checks = []
    if name == "ModBlocks.java":
        checks += [("block.aetherius.", ident) for ident in
                   re.findall(r'register\(\s*"([a-z0-9_]+)"', src)]
    elif name == "ModItems.java":
        # plain items own item.* keys; block items resolve through the block's own key
        checks += [("item.aetherius.", ident) for ident in
                   re.findall(r'^\s*public static final Item [A-Z_]+ = register\("([a-z0-9_]+)"', src, re.M)]
        checks += [("block.aetherius.", ident) for ident in
                   re.findall(r'blockItem\("([a-z0-9_]+)"', src)]
    elif name == "ModEntities.java":
        checks += [("entity.aetherius.", ident) for ident in
                   re.findall(r'register\(\s*"([a-z0-9_]+)"', src)]
    for prefix, ident in checks:
        registry_checks += 1
        if prefix + ident not in keys:
            problems.append(f"{name}: missing lang key {prefix + ident}")

for path in glob.glob(os.path.join(ROOT, "api", "spell", "impl", "*.java")):
    src = open(path, encoding="utf-8").read()
    for spell in re.findall(r'super\(\s*"([a-z0-9_]+)"', src):
        for suffix in ("", ".desc"):
            key = f"aetherius.spell.{spell}{suffix}"
            registry_checks += 1
            if key not in keys:
                problems.append(f"{os.path.basename(path)}: missing lang key {key}")

if problems:
    print("OTHER PROBLEMS:")
    for prob in sorted(set(problems)):
        print(" -", prob)
print(f"registry/spell lang checks: {registry_checks}")

# report unused lang keys too, for documentation value
unused = sorted(k for k in keys if k not in used)
print(f"lang keys not referenced statically: {len(unused)} (built dynamically or future-facing)")
sys.exit(1 if missing else 0)
