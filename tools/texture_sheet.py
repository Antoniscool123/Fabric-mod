#!/usr/bin/env python3
"""
Builds docs/img/texture_sheet.png: every generated texture at 5x, in one contact sheet.

This is the review tool for tools/generate_textures.py - a texture that reads as mud at 16x16
is obvious here at a glance, and the whole set can be compared for colour consistency.

Run from the repo root:  python3 tools/texture_sheet.py
"""
import os
import struct
import zlib
import importlib.util

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "aetherius", "textures")
OUT = os.path.join(ROOT, "docs", "img", "texture_sheet.png")

SCALE = 5
PAD = 6
COLS = 6


def _load_png_writer():
    spec = importlib.util.spec_from_file_location("gt", os.path.join(HERE, "generate_textures.py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.write_png


def read_png(path):
    """Minimal RGBA PNG reader (filters 0-4), so the sheet needs no third-party library."""
    data = open(path, "rb").read()
    pos = 8
    width = height = None
    idat = b""
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        tag = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if tag == b"IHDR":
            width, height = struct.unpack(">II", body[:8])
        elif tag == b"IDAT":
            idat += body
    raw = zlib.decompress(idat)
    stride = width * 4
    prev = bytearray(stride)
    rows = []
    i = 0
    for _ in range(height):
        filt = raw[i]
        i += 1
        line = bytearray(raw[i:i + stride])
        i += stride
        if filt == 1:
            for x in range(4, stride):
                line[x] = (line[x] + line[x - 4]) & 255
        elif filt == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 255
        elif filt == 3:
            for x in range(stride):
                left = line[x - 4] if x >= 4 else 0
                line[x] = (line[x] + (left + prev[x]) // 2) & 255
        elif filt == 4:
            for x in range(stride):
                a = line[x - 4] if x >= 4 else 0
                b = prev[x]
                c = prev[x - 4] if x >= 4 else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 255
        prev = line
        rows.append([tuple(line[x * 4:x * 4 + 4]) for x in range(width)])
    return width, height, rows


def main():
    paths = []
    for sub in ("block", "item", "entity", "particle"):
        directory = os.path.join(TEX, sub)
        if not os.path.isdir(directory):
            continue
        for name in sorted(os.listdir(directory)):
            if name.endswith(".png"):
                paths.append(os.path.join(directory, name))

    cell = 16 * SCALE + PAD
    rows_count = (len(paths) + COLS - 1) // COLS
    width = COLS * cell
    height = rows_count * cell
    sheet = [[(30, 26, 40, 255) for _ in range(width)] for _ in range(height)]

    for i, path in enumerate(paths):
        w, h, pixels = read_png(path)
        cx = (i % COLS) * cell + PAD // 2
        cy = (i // COLS) * cell + PAD // 2
        for y in range(h):
            for x in range(w):
                r, g, b, a = pixels[y][x]
                if a == 0:
                    continue
                for dy in range(SCALE):
                    for dx in range(SCALE):
                        sy = cy + y * SCALE + dy
                        sx = cx + x * SCALE + dx
                        if 0 <= sy < height and 0 <= sx < width:
                            under = sheet[sy][sx]
                            t = a / 255.0
                            sheet[sy][sx] = (int(r * t + under[0] * (1 - t)),
                                             int(g * t + under[1] * (1 - t)),
                                             int(b * t + under[2] * (1 - t)), 255)

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    _load_png_writer()(OUT, width, height, sheet)
    print(f"{len(paths)} textures -> {os.path.relpath(OUT, ROOT)} ({width}x{height})")


if __name__ == "__main__":
    main()
