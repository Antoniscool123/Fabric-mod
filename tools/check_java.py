#!/usr/bin/env python3
"""Crude static checks: brace/paren balance, duplicate class names, missing package dirs."""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "java")
problems = []
classes = {}

def strip_strings_and_comments(src):
    out = []
    i = 0
    n = len(src)
    state = None
    while i < n:
        ch = src[i]
        nxt = src[i + 1] if i + 1 < n else ''
        if state is None:
            if ch == '/' and nxt == '/':
                state = 'line'
                i += 2
                continue
            if ch == '/' and nxt == '*':
                state = 'block'
                i += 2
                continue
            if ch == '"':
                state = 'str'
                i += 1
                continue
            if ch == "'":
                state = 'char'
                i += 1
                continue
            out.append(ch)
            i += 1
        elif state == 'line':
            if ch == '\n':
                state = None
                out.append(ch)
            i += 1
        elif state == 'block':
            if ch == '*' and nxt == '/':
                state = None
                i += 2
                continue
            i += 1
        elif state == 'str':
            if ch == '\\':
                i += 2
                continue
            if ch == '"':
                state = None
            i += 1
        elif state == 'char':
            if ch == '\\':
                i += 2
                continue
            if ch == "'":
                state = None
            i += 1
    return ''.join(out)

for dirpath, _, files in os.walk(ROOT):
    for name in sorted(files):
        if not name.endswith('.java'):
            continue
        path = os.path.join(dirpath, name)
        src = open(path, encoding='utf-8').read()
        clean = strip_strings_and_comments(src)
        for open_ch, close_ch, label in [('{', '}', 'braces'), ('(', ')', 'parens'), ('[', ']', 'brackets')]:
            if clean.count(open_ch) != clean.count(close_ch):
                problems.append(f"{path}: unbalanced {label} "
                                f"({clean.count(open_ch)} vs {clean.count(close_ch)})")
        m = re.search(r'\b(?:class|interface|enum|record)\s+([A-Za-z0-9_]+)', clean)
        if m:
            cls = m.group(1)
            classes.setdefault(cls, []).append(path)
        # package must match directory
        pm = re.search(r'package\s+([\w.]+);', clean)
        rel = os.path.relpath(dirpath, ROOT).replace(os.sep, '.')
        if pm and pm.group(1) != rel:
            problems.append(f"{path}: package {pm.group(1)} != dir {rel}")
        # non-ascii identifiers guard (allow unicode in comments/strings only)
        if re.search(r'[^\x00-\x7F]', clean):
            problems.append(f"{path}: non-ASCII outside strings/comments")

dupes = {k: v for k, v in classes.items() if len(v) > 1}
for k, v in dupes.items():
    # Nested/private inner classes with the same simple name are legal; only flag top-level.
    problems.append(f"duplicate top-level class name {k}: {v}")

print(f"scanned {sum(len(v) for v in classes.values())} class declarations in "
      f"{len(classes)} names")
if problems:
    print("PROBLEMS:")
    for p in problems:
        print(" -", p)
    sys.exit(1)
print("no structural problems found")
