#!/usr/bin/env python3
"""Checks flag constants, objective classes and rune constants resolve across the codebase."""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "java", "com", "aetherius")
CHRON = os.path.join(ROOT, "api", "chronicle")


def read(path):
    return open(path, encoding="utf-8").read()


counters = read(os.path.join(CHRON, "ChronicleCounters.java"))
flags = set(re.findall(r'public\s+static\s+final\s+String\s+(FLAG_[A-Z0-9_]+)', counters))
print(f"ChronicleCounters flags: {len(flags)}")

objectives = {f[:-5] for f in os.listdir(os.path.join(CHRON, "objective")) if f.endswith(".java")}
print(f"objective classes: {len(objectives)}")

rune_src = read(os.path.join(ROOT, "api", "magic", "Rune.java"))
runes = set(re.findall(r'^\s{4}([A-Z][A-Z0-9_]*)\(', rune_src, re.M))
print(f"rune constants: {len(runes)}")

problems = []
referenced_objectives = set()
for dirpath, _, files in os.walk(ROOT):
    for name in files:
        if not name.endswith(".java"):
            continue
        path = os.path.join(dirpath, name)
        src = read(path)
        rel = os.path.relpath(path, ROOT)
        for flag in set(re.findall(r'ChronicleCounters\.(FLAG_[A-Z0-9_]+)', src)):
            if flag not in flags:
                problems.append(f"{rel}: unknown flag {flag}")
        for obj in set(re.findall(r'\bnew\s+([A-Z][A-Za-z0-9_]*(?:Objective|Kill|Craft|Find|Reach|Defeat|Witness|Visit|Obtain)[A-Za-z0-9_]*)\s*\(', src)):
            if obj.endswith("Objective") or obj in objectives or obj.startswith(("Kill", "Craft", "Find", "Obtain")):
                referenced_objectives.add(obj)
                if obj not in objectives and not obj.startswith("Objective"):
                    problems.append(f"{rel}: objective class {obj} has no file")
        for rune in set(re.findall(r'\bRune\.([A-Z][A-Z0-9_]+)\b', src)):
            if rune not in runes:
                problems.append(f"{rel}: unknown rune {rune}")

print(f"objective types instantiated: {len(referenced_objectives)}")
if problems:
    print("PROBLEMS:")
    for p in sorted(set(problems)):
        print(" -", p)
    sys.exit(1)
print("chronicle/rune references resolve")
