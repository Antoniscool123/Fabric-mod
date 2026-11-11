#!/usr/bin/env python3
"""Cross-checks Mod*.<FIELD> references against the fields actually declared in registries."""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "java", "com", "aetherius")
REGISTRIES = ["ModBlocks", "ModItems", "ModEntities", "ModSounds", "ModParticles",
              "ModStatusEffects", "ModBlockEntities", "ModMultiblocks", "ModItemGroups",
              "ModWorldGen"]

fields = {}
for reg in REGISTRIES:
    path = None
    for dirpath, _, files in os.walk(ROOT):
        if reg + ".java" in files:
            path = os.path.join(dirpath, reg + ".java")
            break
    if not path:
        print(f"!! registry {reg} not found")
        continue
    src = open(path, encoding="utf-8").read()
    names = set(re.findall(r'public\s+static\s+final\s+[\w<>\[\],.? ]+?\s+([A-Z][A-Z0-9_]*)\s*=', src))
    fields[reg] = names
    print(f"{reg}: {len(names)} fields")

problems = []
for dirpath, _, files in os.walk(ROOT):
    for name in files:
        if not name.endswith(".java"):
            continue
        path = os.path.join(dirpath, name)
        src = open(path, encoding="utf-8").read()
        for reg, declared in fields.items():
            for used in set(re.findall(re.escape(reg) + r'\.([A-Z][A-Z0-9_]*)', src)):
                if used not in declared:
                    problems.append(f"{os.path.relpath(path, ROOT)}: {reg}.{used} is not declared")

if problems:
    print("UNRESOLVED REFERENCES:")
    for p in sorted(set(problems)):
        print(" -", p)
    sys.exit(1)
print("all registry references resolve")
