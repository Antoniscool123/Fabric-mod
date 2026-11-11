#!/usr/bin/env python3
"""Checks calls on our own classes against members they actually declare (incl. inheritance).

Catches the most common "cannot find symbol" class of compile error without a JDK:
  - Method X.foo(...) where X (our class) has no foo, even inherited.
  - Field  X.FOO     where X (our class) has no FOO, even inherited.
  - Interface methods implemented by name-mismatch are reported separately.
"""
import os
import re
import sys
from collections import defaultdict

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "java", "com", "aetherius")

TYPE_DECL = re.compile(r'\b(class|interface|enum|record)\s+([A-Za-z0-9_]+)'
                       r'(?:\s*<[^>{]*>)?(?:\s*\([^)]*\))?(?:\s+extends\s+([A-Za-z0-9_.<>, ]+?))?'
                       r'(?:\s+implements\s+([A-Za-z0-9_.<>, ]+?))?\s*\{')
METHOD_DECL = re.compile(r'(?:^|\n)\s*(?:@\w+(?:\([^)]*\))?\s*)*'
                         r'(?:public|protected|private|static|final|abstract|default|synchronized|native|\s)*'
                         r'[\w<>\[\],.?\s]+?\s+([a-z_][A-Za-z0-9_]*)\s*\([^;{]*\)\s*(?:throws [\w, ]+)?\s*[{;]')
FIELD_DECL = re.compile(r'(?:^|\n)\s*(?:public|protected|private|static|final|transient|volatile|\s)+'
                        r'[\w<>\[\],.?]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=|;)')

files = []
for dirpath, _, names in os.walk(ROOT):
    for n in names:
        if n.endswith(".java"):
            files.append(os.path.join(dirpath, n))

types = {}          # fully-qualified-ish name -> dict(...)
simple_index = defaultdict(list)   # simple name -> [qualified keys]
for path in files:
    src = open(path, encoding="utf-8").read()
    # Blank out block comments (keeping newlines) so a ';' or '{' inside a javadoc can never
    # truncate a body: that bug hid a new enum constant from this checker once already.
    src = re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), src, flags=re.S)
    for m in TYPE_DECL.finditer(src):
        kind, name, ext, impl = m.group(1), m.group(2), m.group(3), m.group(4)
        body = src[m.end():]
        # crude body extraction: balanced braces
        depth, i = 1, 0
        while i < len(body) and depth:
            if body[i] == '{':
                depth += 1
            elif body[i] == '}':
                depth -= 1
            i += 1
        body = body[:i]
        methods = set(METHOD_DECL.findall(body))
        fields = set(FIELD_DECL.findall(body))
        fields |= set(re.findall(r'\b([A-Z][A-Z0-9_]+)\s*[=;,)]', body))
        # enum constants
        if kind == "enum":
            first = body.split(";")[0]
            fields |= set(re.findall(r'^\s*([A-Z][A-Z0-9_]+)\s*[(,;]', first, re.M))
        key = name
        q = name
        types[q] = {
            "methods": methods,
            "fields": fields,
            "extends": (ext or "").strip(),
            "implements": (impl or "").strip(),
            "path": path,
            "kind": kind,
            "top_level": (name == os.path.splitext(os.path.basename(path))[0]),
        }
        simple_index[name].append(q)

def simple(name):
    name = name.strip()
    name = re.sub(r'<.*', '', name)
    return name.split('.')[-1].strip()

def lookup(name):
    """Returns the type dict for an unambiguous *top-level* simple name, else None."""
    keys = [k for k in simple_index.get(name, []) if types[k]["top_level"]]
    return types[keys[0]] if len(keys) == 1 else None

def all_methods(name, seen=None):
    seen = seen or set()
    t = lookup(name)
    if t is None:
        return None
    if name in seen:
        return set()
    seen.add(name)
    out = set(t["methods"])
    # enum constants get values()/valueOf() from java.lang.Enum for free
    if t["kind"] == "enum":
        out |= {"values", "valueOf", "ordinal", "name", "toString", "compareTo", "equals",
                "hashCode", "getDeclaringClass", "describeConstable", "clone"}
    if t["extends"]:
        parent = all_methods(simple(t["extends"]), seen)
        if parent:
            out |= parent
    for part in re.split(r',', t["implements"]):
        if part.strip():
            iface = all_methods(simple(part), seen)
            if iface:
                out |= iface
    return out

def all_fields(name, seen=None):
    seen = seen or set()
    t = lookup(name)
    if t is None:
        return None
    if name in seen:
        return set()
    seen.add(name)
    out = set(t["fields"])
    if t["extends"]:
        parent = all_fields(simple(t["extends"]), seen)
        if parent:
            out |= parent
    return out

METHOD_CALL = re.compile(r'\b([A-Z][A-Za-z0-9_]*)\.([a-z_][A-Za-z0-9_]*)\s*\(')
FIELD_USE = re.compile(r'\b([A-Z][A-Za-z0-9_]*)\.([A-Za-z_][A-Za-z0-9_]*)\b(?!\s*\()')

problems = []
checked = 0
for path in files:
    src = open(path, encoding="utf-8").read()
    # Blank out block comments (keeping newlines) so a ';' or '{' inside a javadoc can never
    # truncate a body: that bug hid a new enum constant from this checker once already.
    src = re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), src, flags=re.S)
    rel = os.path.relpath(path, ROOT)
    for owner, member in METHOD_CALL.findall(src):
        methods = all_methods(owner)
        if methods is None:
            continue
        checked += 1
        if member not in methods:
            problems.append(f"{rel}: {owner}.{member}(...) - no such method")
    for owner, member in FIELD_USE.findall(src):
        if member in ("class", "this", "super"):
            continue
        # PascalCase members are nested type references (Outer.Inner), not fields
        if re.match(r'^[A-Z][a-z]', member):
            continue
        fields = all_fields(owner)
        if fields is None:
            continue
        checked += 1
        if member not in fields:
            problems.append(f"{rel}: {owner}.{member} - no such field")

# required interface methods
INTERFACES = ["QuestObjective", "BossPhase", "BehaviorNode", "MagicUser", "SpellSchool"]
for iface in INTERFACES:
    if iface not in types:
        continue
    required = types[iface]["methods"]
    for name, t in types.items():
        if name == iface:
            continue
        impls = [simple(p) for p in re.split(r',', t["implements"]) if p.strip()]
        extends = simple(t["extends"]) if t["extends"] else ""
        if iface in impls or extends == iface:
            have = all_methods(name)
            missing = {m for m in required if m not in have}
            if missing:
                problems.append(f"{os.path.relpath(t['path'], ROOT)}: {name} implements "
                                f"{iface} but lacks {sorted(missing)}")

print(f"checked {checked} cross-class member references across {len(types)} types")
if problems:
    print("PROBLEMS:")
    for p in sorted(set(problems)):
        print(" -", p)
    sys.exit(1)
print("all cross-class member references resolve")
