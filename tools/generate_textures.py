#!/usr/bin/env python3
"""
Aetherius texture generator.

Writes every PNG the mod needs, procedurally, with zero third-party dependencies
(no PIL): raw RGBA buffers are zlib-compressed into valid PNG chunks.

Run from the repo root:  python3 tools/generate_textures.py
Outputs into src/main/resources/assets/aetherius/textures/**.

<h2>Art direction</h2>
Everything is built from three layers, in this order:

1. <b>Tileable value noise</b> ({@code fbm}) - the "material" of a texture: stone grain,
   grass clumps, scales, ash drifts. It wraps at the tile edge, so blocks do not seam.
2. <b>Structure</b> - the readable part: ore clusters with a rim and a specular, a grass lip
   overhang on the side texture, pillar flutes, forge rivets, a lantern frame, item silhouettes.
3. <b>Lighting</b> - a cheap top-left light: bevels on block edges, a highlight where an item
   silhouette meets empty space, a dark outline around it. This is what stops a texture from
   reading as a flat block of colour.

Colours are picked from a small Aetherius palette (violet, starlight blue, ember orange, bone)
so the whole mod looks like one set rather than fifteen unrelated placeholders.

Entity atlases keep the layouts the models expect: 64x64 biped skins, and the 256x256
Nox'Thal atlas whose uv regions (scales 0,0 / membrane 0,96 / bone 100,0) are the ones named
in assets/aetherius/geo/entity/nox_thal.geo.json.
"""
import os
import struct
import zlib
import math
import random

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "aetherius", "textures")


# ----------------------------------------------------------------------------- PNG
def write_png(path, width, height, pixels):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        row = pixels[y]
        for x in range(width):
            r, g, b, a = row[x]
            raw += bytes((r & 255, g & 255, b & 255, a & 255))
    compressed = zlib.compress(bytes(raw), 9)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", compressed)
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(png)


def blank(w, h, color=(0, 0, 0, 0)):
    return [[color for _ in range(w)] for _ in range(h)]


def hexc(value, alpha=255):
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255, alpha)


def shade(color, factor):
    alpha = color[3] if len(color) > 3 else 255
    return (max(0, min(255, int(color[0] * factor))),
            max(0, min(255, int(color[1] * factor))),
            max(0, min(255, int(color[2] * factor))),
            alpha)


def mix(a, b, t):
    """Linear blend between two RGBA tuples, t in 0..1."""
    t = max(0.0, min(1.0, t))
    return (int(a[0] + (b[0] - a[0]) * t),
            int(a[1] + (b[1] - a[1]) * t),
            int(a[2] + (b[2] - a[2]) * t),
            int((a[3] if len(a) > 3 else 255) + ((b[3] if len(b) > 3 else 255)
                                                 - (a[3] if len(a) > 3 else 255)) * t))


def ramp(stops, t):
    """stops = [(0.0, color), (0.5, color), ...] sorted by position."""
    t = max(0.0, min(1.0, t))
    for i in range(len(stops) - 1):
        p0, c0 = stops[i]
        p1, c1 = stops[i + 1]
        if p0 <= t <= p1:
            span = p1 - p0
            return mix(c0, c1, 0.0 if span <= 0 else (t - p0) / span)
    return stops[-1][1] if t > stops[-1][0] else stops[0][1]


# --------------------------------------------------------------------- value noise
def _hash(ix, iy, seed):
    h = (ix * 374761393 + iy * 668265263 + seed * 1442695040888963407) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    h = h ^ (h >> 16)
    return (h & 0xFFFFFF) / float(0xFFFFFF)


def _smoothstep(t):
    return t * t * (3.0 - 2.0 * t)


def tile_noise(x, y, size, cells, seed):
    """Value noise on a `cells` x `cells` lattice wrapped to `size` - tiles seamlessly."""
    u = (x / float(size)) * cells
    v = (y / float(size)) * cells
    x0 = int(math.floor(u))
    y0 = int(math.floor(v))
    tx = _smoothstep(u - x0)
    ty = _smoothstep(v - y0)

    def h(ix, iy):
        return _hash(ix % cells, iy % cells, seed)

    top = h(x0, y0) + (h(x0 + 1, y0) - h(x0, y0)) * tx
    bottom = h(x0, y0 + 1) + (h(x0 + 1, y0 + 1) - h(x0, y0 + 1)) * tx
    return top + (bottom - top) * ty


def fbm(x, y, size, seed, cells=2, octaves=3, gain=0.5):
    """Fractal sum of tileable value noise in 0..1."""
    total = 0.0
    norm = 0.0
    amp = 1.0
    c = cells
    for o in range(octaves):
        total += tile_noise(x, y, size, c, seed + o * 7919) * amp
        norm += amp
        amp *= gain
        c *= 2
    return total / norm


def worley_near(x, y, size, cells, seed):
    """Distance to the nearest wrapped feature point, normalised 0..1 (crystal/speckle cells)."""
    fx = (x / float(size)) * cells
    fy = (y / float(size)) * cells
    cx = int(math.floor(fx))
    cy = int(math.floor(fy))
    best = 9.9
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            gx = cx + dx
            gy = cy + dy
            px = gx + _hash(gx % cells, gy % cells, seed)
            py = gy + _hash(gx % cells, gy % cells, seed + 31)
            best = min(best, math.hypot(fx - px, fy - py))
    return min(1.0, best)


# --------------------------------------------------------------------- lighting
def bevel(img, up=1.16, down=0.82, sides=1.06):
    """Top row lighter, bottom row darker, columns slightly graded - reads as a raised block."""
    h = len(img)
    w = len(img[0])
    out = [row[:] for row in img]
    for x in range(w):
        out[0][x] = shade(out[0][x], up)
        out[h - 1][x] = shade(out[h - 1][x], down)
    for y in range(h):
        out[y][0] = shade(out[y][0], sides)
        out[y][w - 1] = shade(out[y][w - 1], 2.0 - sides)
    return out


def vignette(img, strength=0.16):
    """Darken towards the tile border; stops 16x16 tiles from looking flat at distance."""
    h = len(img)
    w = len(img[0])
    for y in range(h):
        for x in range(w):
            dx = min(x, w - 1 - x) / (w / 2.0)
            dy = min(y, h - 1 - y) / (h / 2.0)
            edge = min(dx, dy)
            img[y][x] = shade(img[y][x], 1.0 - strength * (1.0 - edge))
    return img


def outline_shape(img, color, diagonal=False):
    """Wrap every opaque pixel cluster in a 1px outline (items read against any background)."""
    h = len(img)
    w = len(img[0])
    solid = [[img[y][x][3] > 8 for x in range(w)] for y in range(h)]
    offsets = [(-1, 0), (1, 0), (0, -1), (0, 1)]
    if diagonal:
        offsets += [(-1, -1), (1, 1), (-1, 1), (1, -1)]
    out = [row[:] for row in img]
    for y in range(h):
        for x in range(w):
            if solid[y][x]:
                continue
            for dx, dy in offsets:
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and solid[ny][nx]:
                    out[y][x] = color
                    break
    return out


def lit_item(img, up=1.28, down=0.78):
    """Light an item sprite: top-left silhouette edge catches the light, bottom-right falls away."""
    h = len(img)
    w = len(img[0])
    solid = [[img[y][x][3] > 8 for x in range(w)] for y in range(h)]
    out = [row[:] for row in img]
    for y in range(h):
        for x in range(w):
            if not solid[y][x]:
                continue
            top = y == 0 or not solid[y - 1][x]
            left = x == 0 or not solid[y][x - 1]
            bottom = y == h - 1 or not solid[y + 1][x]
            right = x == w - 1 or not solid[y][x + 1]
            f = 1.0
            if top or left:
                f *= up
            if bottom or right:
                f *= down
            if f != 1.0:
                out[y][x] = shade(out[y][x], f)
    return out


def fill_rect(img, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(len(img), y1)):
        for x in range(max(0, x0), min(len(img[0]), x1)):
            img[y][x] = color
    return img


def put(img, x, y, color):
    if 0 <= y < len(img) and 0 <= x < len(img[0]):
        img[y][x] = color
    return img


# ------------------------------------------------------------------ block textures
def material_tile(size, seed, stops, cells=3, octaves=3, grain=0.35, grain_cells=8):
    """A block face: fbm ramp for the body plus a fine grain layer, beveled at the edges."""
    img = blank(size, size)
    for y in range(size):
        for x in range(size):
            v = fbm(x, y, size, seed, cells, octaves)
            g = fbm(x, y, size, seed + 4001, grain_cells, 2)
            img[y][x] = ramp(stops, v * (1.0 - grain) + g * grain)
    return img


def ore_tile(size, seed, stone_stops, crystal_stops, clusters=5, glow=None):
    """Stone with embedded crystal clusters: dark rim, body, bright facet, optional specular."""
    img = material_tile(size, seed, stone_stops, 3, 3, 0.4, 9)
    rnd = random.Random(seed)
    for _ in range(clusters):
        cx = rnd.randrange(2, size - 2)
        cy = rnd.randrange(2, size - 2)
        rx = rnd.uniform(1.4, 2.6)
        ry = rx * rnd.uniform(0.7, 1.15)
        for y in range(size):
            for x in range(size):
                d = math.hypot((x - cx) / rx, (y - cy) / ry)
                if d > 1.0:
                    continue
                if d > 0.82:
                    img[y][x] = ramp(crystal_stops, 0.12)
                elif d > 0.45:
                    img[y][x] = ramp(crystal_stops, 0.55)
                else:
                    img[y][x] = ramp(crystal_stops, 0.95)
        if glow:
            put(img, int(cx), int(cy), glow)
    return bevel(img)


def grass_top(size, seed):
    """Starlit grass: deep teal clumps, lighter moss, a scatter of bright blades."""
    img = blank(size, size)
    for y in range(size):
        for x in range(size):
            clump = fbm(x, y, size, seed, 2, 3)
            fine = fbm(x, y, size, seed + 91, 8, 2)
            t = clump * 0.75 + fine * 0.25
            img[y][x] = ramp([(0.0, (26, 62, 104, 255)), (0.35, (46, 104, 158, 255)),
                              (0.7, (72, 146, 202, 255)), (1.0, (110, 190, 234, 255))], t)
    rnd = random.Random(seed + 11)
    for _ in range(12):
        x = rnd.randrange(size)
        y = rnd.randrange(size)
        put(img, x, y, (150, 214, 248, 255))
        if rnd.random() < 0.5:
            put(img, x, y - 1, (198, 238, 255, 255))
    for _ in range(5):
        x, y = rnd.randrange(size), rnd.randrange(size)
        put(img, x, y, (22, 52, 92, 255))
    return vignette(bevel(img, 1.10, 0.86), 0.14)

def grass_side(size, seed):
    """Dirt face with a ragged starlit lip on top - the silhouette players read as 'grass'."""
    dirt = [(0.0, (58, 42, 32, 255)), (0.5, (86, 64, 46, 255)), (1.0, (112, 86, 62, 255))]
    img = material_tile(size, seed, dirt, 3, 3, 0.45, 8)
    rnd = random.Random(seed + 5)
    lip = [(0.0, (38, 84, 132, 255)), (0.6, (74, 146, 200, 255)), (1.0, (128, 200, 238, 255))]
    for x in range(size):
        depth = 3 + (1 if rnd.random() < 0.35 else 0) + (1 if rnd.random() < 0.15 else 0)
        for y in range(depth):
            img[y][x] = ramp(lip, 0.25 + 0.55 * fbm(x, y, size, seed + 90, 8, 2))
        if rnd.random() < 0.4:
            put(img, x, depth, shade(ramp(lip, 0.9), 0.9))
    return bevel(img, 1.12, 0.84)


def ash_tile(size, seed):
    """Star-Ash: pale dunes with wind ripples, dark grit and a cold violet sheen."""
    img = blank(size, size)
    for y in range(size):
        for x in range(size):
            v = fbm(x, y, size, seed, 2, 3)
            ripple = 0.5 + 0.5 * math.sin(y * 0.9 + fbm(x, y, size, seed + 17, 3, 2) * 5.0)
            t = v * 0.7 + ripple * 0.3
            img[y][x] = ramp([(0.0, (104, 100, 112, 255)), (0.4, (150, 146, 158, 255)),
                              (0.75, (188, 184, 194, 255)), (1.0, (222, 218, 226, 255))], t)
    rnd = random.Random(seed + 3)
    for _ in range(8):
        x, y = rnd.randrange(size), rnd.randrange(size)
        img[y][x] = shade(img[y][x], 0.74)
        if rnd.random() < 0.6:
            put(img, x + 1, y, shade(img[y][x], 0.9))
    for _ in range(3):
        x, y = rnd.randrange(size), rnd.randrange(size)
        put(img, x, y, (176, 140, 232, 255))
    return vignette(bevel(img, 1.10, 0.88), 0.12)

def pillar_side(size, seed):
    """Quartz column: fluted vertical grooves with light and shadow bands."""
    stops = [(0.0, (176, 170, 186, 255)), (0.55, (214, 208, 220, 255)),
             (1.0, (238, 234, 244, 255))]
    img = material_tile(size, seed, stops, 2, 2, 0.3, 5)
    flutes = [2, 3, 6, 7, 10, 11, 14]
    for x in flutes:
        for y in range(size):
            v = fbm(x, y, size, seed + 61, 4, 2)
            img[y][x] = shade(img[y][x], 0.74 + 0.16 * v)
    for x in (x for x in range(size) if x not in flutes):
        for y in range(size):
            if x % 4 == 1:
                img[y][x] = shade(img[y][x], 1.06)
    # Bands top and bottom catch the light like carved stone.
    for x in range(size):
        img[0][x] = shade(img[0][x], 1.2)
        img[1][x] = mix(img[1][x], (198, 160, 240, 255), 0.25)
        img[size - 1][x] = shade(img[size - 1][x], 0.72)
    return img


def pillar_top(size, seed):
    """Concentric carved rings with a violet inlay at the centre."""
    img = blank(size, size)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - (size - 1) / 2.0, y - (size - 1) / 2.0) / (size / 2.0)
            if d > 1.02:
                img[y][x] = (186, 180, 196, 255)
            elif d > 0.86:
                img[y][x] = (206, 200, 214, 255)
            elif d > 0.78:
                img[y][x] = (150, 118, 210, 255)
            elif d > 0.55:
                img[y][x] = (222, 216, 230, 255)
            elif d > 0.46:
                img[y][x] = (162, 128, 220, 255)
            elif d > 0.2:
                img[y][x] = (232, 226, 240, 255)
            else:
                img[y][x] = (196, 160, 250, 255)
    for y in range(size):
        for x in range(size):
            img[y][x] = shade(img[y][x], 0.94 + 0.12 * fbm(x, y, size, seed, 4, 2))
    return img


def forge_side(size, seed):
    """Dark plated metal with rivets and a cool glow leaking between the plates."""
    stops = [(0.0, (18, 28, 44, 255)), (0.5, (30, 48, 72, 255)), (1.0, (44, 70, 100, 255))]
    img = material_tile(size, seed, stops, 2, 2, 0.3, 6)
    for y in (4, 11):
        for x in range(size):
            img[y][x] = shade(img[y][x], 0.66)
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13), (7, 7), (8, 8)):
        put(img, x, y, (104, 132, 160, 255))
        put(img, x, y + 1, (22, 30, 42, 255))
    for x in range(size):
        img[7][x] = (86, 196, 240, 255) if x % 3 else (140, 226, 255, 255)
    return bevel(img, 1.14, 0.8)


def forge_top(size, seed):
    """Grille over the forge's vent, cooling channels glowing faint blue."""
    img = material_tile(size, seed, [(0.0, (22, 34, 52, 255)), (1.0, (46, 70, 98, 255))],
                        2, 2, 0.25, 5)
    for y in range(2, size - 2):
        for x in range(2, size - 2):
            channel = (y % 3 == 0) or (x % 3 == 0)
            if channel:
                img[y][x] = (70, 150, 200, 255) if (x + y) % 4 else (150, 226, 255, 255)
    for i in range(size):
        img[1][i] = shade(img[1][i], 1.2)
        img[size - 2][i] = shade(img[size - 2][i], 0.7)
    return bevel(img, 1.14, 0.8)


def forge_front(size, seed):
    """Mouth of the forge: iron frame, a grate over the fire, sparks on the floor."""
    img = material_tile(size, seed, [(0.0, (24, 20, 26, 255)), (1.0, (56, 48, 56, 255))],
                        2, 2, 0.3, 6)
    for y in range(3, 14):
        for x in range(3, 13):
            d = math.hypot((x - 7.5) / 4.6, (y - 8.5) / 5.0)
            if d > 1.0:
                continue
            img[y][x] = ramp([(0.0, (92, 26, 10, 255)), (0.4, (214, 84, 22, 255)),
                              (0.75, (255, 170, 60, 255)), (1.0, (255, 238, 196, 255))],
                             1.0 - d)
    # Grate: three bars and a cross brace over the fire.
    for x in range(4, 12, 3):
        for y in range(4, 13):
            img[y][x] = (34, 28, 30, 255)
    for x in range(4, 12):
        img[8][x] = (40, 32, 34, 255)
    # Iron frame + corner rivets.
    for x in range(2, 14):
        img[2][x] = (78, 68, 64, 255)
        img[14][x] = (22, 18, 18, 255)
    for y in range(2, 15):
        img[y][2] = (78, 68, 64, 255)
        img[y][14] = (22, 18, 18, 255)
    for (x, y) in ((3, 3), (12, 3), (3, 13), (12, 13)):
        put(img, x, y, (184, 172, 148, 255))
        put(img, x, y + 1, (30, 24, 22, 255))
    put(img, 6, 5, (255, 246, 210, 255))
    put(img, 11, 10, (255, 210, 140, 255))
    return vignette(bevel(img, 1.12, 0.78), 0.10)

def altar_core(size, seed):
    """Pale stone set with a violet rune ring - the altar's controller face."""
    img = material_tile(size, seed, [(0.0, (196, 192, 204, 255)), (0.6, (228, 224, 236, 255)),
                                     (1.0, (246, 244, 250, 255))], 2, 2, 0.25, 5)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - 7.5, y - 7.5)
            if 4.4 < d < 5.6:
                img[y][x] = (146, 92, 224, 255)
            elif d < 2.0:
                img[y][x] = (198, 150, 250, 255)
            elif 2.4 < d < 3.2:
                img[y][x] = (168, 116, 236, 255)
    return bevel(img, 1.12, 0.86)


def altar_top(size, seed):
    """Carved top face: inlaid border, four rune studs and an eight-point star."""
    img = blank(size, size)
    for y in range(size):
        for x in range(size):
            v = fbm(x, y, size, seed, 3, 3)
            img[y][x] = ramp([(0.0, (196, 192, 206, 255)), (0.6, (224, 220, 232, 255)),
                              (1.0, (244, 242, 250, 255))], v)
    for i in range(size):
        for (a, b) in ((1, 1), (size - 2, 1), (1, size - 2), (size - 2, size - 2)):
            put(img, i, a, (150, 104, 216, 255))
            put(img, a, i, (150, 104, 216, 255))
    for (cx, cy) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        put(img, cx, cy, (120, 74, 196, 255))
        put(img, cx + 1, cy, (168, 124, 232, 255))
    cx = cy = (size - 1) / 2.0
    for y in range(size):
        for x in range(size):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            angle = math.atan2(dy, dx)
            star = 3.6 + 2.0 * math.cos(angle * 4.0)
            if d < star and d > 1.4:
                img[y][x] = (158, 108, 232, 255)
            elif d <= 1.4:
                img[y][x] = (216, 182, 255, 255)
    return vignette(bevel(img, 1.10, 0.88), 0.10)

def fountain_face(size, seed):
    """Basin wall: cut stone courses, a water lip on top, a row of glowing runes."""
    img = blank(size, size)
    for y in range(size):
        for x in range(size):
            v = fbm(x, y, size, seed, 3, 3)
            img[y][x] = ramp([(0.0, (34, 44, 62, 255)), (0.55, (56, 72, 96, 255)),
                              (1.0, (80, 102, 130, 255))], v)
    # Stone courses with staggered joints.
    for row, y in enumerate(range(0, size, 5)):
        for x in range(size):
            img[y][x] = shade(img[y][x], 0.66)
        for x in range((row * 3) % 8, size, 8):
            for yy in range(y, min(size, y + 5)):
                put(img, x, yy, shade(img[yy][x], 0.72))
    # Water lip.
    for x in range(size):
        for y in range(1, 4):
            img[y][x] = ramp([(0.0, (28, 92, 140, 255)), (0.6, (86, 186, 232, 255)),
                              (1.0, (168, 232, 255, 255))], (y - 1) / 2.0)
    for x in range(size):
        img[0][x] = (206, 244, 255, 255) if x % 3 else (140, 214, 250, 255)
    # Runes.
    for x in range(3, size - 2, 5):
        for y in range(7, 12):
            img[y][x] = (150, 236, 255, 255)
        put(img, x + 1, 9, (86, 170, 220, 255))
    return vignette(bevel(img, 1.10, 0.82), 0.12)

def lantern_sprite(size, seed):
    """Iron lantern frame with three warm panes and a handle."""
    img = blank(size, size)
    rnd = random.Random(seed)
    frame = [(0.0, (54, 42, 30, 255)), (1.0, (96, 74, 46, 255))]
    for y in range(2, size - 1):
        for x in range(2, size - 2):
            img[y][x] = ramp(frame, fbm(x, y, size, seed, 3, 2))
    # Glowing panes.
    for (px, py) in ((4, 4), (8, 4), (6, 8), (4, 9), (9, 9)):
        for dy in range(2):
            for dx in range(2):
                img[py + dy][px + dx] = (255, 214, 128, 255) if (dx + dy) % 2 else (
                    255, 240, 190, 255)
    img[3][7] = (255, 250, 220, 255)
    # Corner posts, cap and handle.
    for x in (2, size - 3):
        for y in range(2, size - 1):
            img[y][x] = (44, 34, 24, 255)
    for x in range(2, size - 2):
        img[1][x] = (72, 56, 34, 255)
        img[size - 2][x] = (34, 26, 18, 255)
    put(img, size // 2, 0, (110, 90, 56, 255))
    return img


def anchor_face(size, seed):
    """Obsidian ring with four heart sockets; the middle glows once filled."""
    img = material_tile(size, seed, [(0.0, (16, 10, 24, 255)), (0.55, (32, 20, 44, 255)),
                                     (1.0, (52, 32, 70, 255))], 3, 3, 0.35, 7)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - 7.5, y - 7.5)
            if 5.6 < d < 6.6:
                img[y][x] = (26, 16, 36, 255)
            elif 2.6 < d < 3.6:
                img[y][x] = (128, 74, 220, 255)
            elif d < 1.6:
                img[y][x] = (196, 160, 255, 255)
    for (x, y) in ((7, 1), (7, 14), (1, 7), (14, 7)):
        put(img, x, y, (230, 208, 160, 255))
    return bevel(img, 1.14, 0.8)


def portal_face(size, seed):
    """A hole in the world: violet vortex, brightest at the core, translucent at the rim."""
    img = blank(size, size)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - 7.5, y - 7.5) / 10.0
            swirl = 0.5 + 0.5 * math.sin(
                math.atan2(y - 7.5, x - 7.5) * 3.0 + d * 9.0 + fbm(x, y, size, seed, 3, 2) * 3.0)
            color = ramp([(0.0, (20, 8, 36, 255)), (0.5, (86, 40, 158, 255)),
                          (0.85, (168, 112, 246, 255)), (1.0, (232, 216, 255, 255))],
                         swirl * (1.0 - d * 0.75) + 0.15)
            alpha = int(max(0, min(255, 245 * (1.0 - d * 0.85))))
            img[y][x] = (color[0], color[1], color[2], alpha)
    return img


def lily_sprite(size, seed):
    """Mana lily: cross-model flower with glowing petals and a tall starlit stamen."""
    img = blank(size, size)
    for y in range(9, size):
        img[y][7] = (46, 96, 66, 255)
        img[y][8] = (62, 122, 84, 255)
    for (x, y) in ((5, 12), (4, 13), (10, 12), (11, 13), (6, 14), (9, 14)):
        put(img, x, y, (78, 148, 96, 255))
    cx, cy = 7.5, 6.0
    for y in range(size):
        for x in range(size):
            d = math.hypot((x - cx), (y - cy))
            if d < 3.6:
                if d > 2.6:
                    img[y][x] = (108, 190, 250, 255)
                elif d > 1.2:
                    img[y][x] = (168, 224, 255, 255)
                else:
                    img[y][x] = (255, 246, 190, 255)
    for (x, y) in ((7, 2), (8, 2), (7, 1)):
        put(img, x, y, (255, 255, 225, 255))
    return img


def telescope_face(size, seed):
    """Brass barrel with riveted bands and a glint along the top."""
    img = material_tile(size, seed, [(0.0, (58, 46, 34, 255)), (0.5, (96, 76, 44, 255)),
                                     (1.0, (146, 118, 66, 255))], 2, 2, 0.3, 6)
    for y in (3, 8, 12):
        for x in range(size):
            img[y][x] = shade(img[y][x], 1.22)
            img[y + 1][x] = shade(img[y + 1][x], 0.7)
    for x in range(size):
        img[1][x] = mix(img[1][x], (255, 236, 180, 255), 0.45)
    for (x, y) in ((4, 5), (11, 5), (4, 10), (11, 10)):
        put(img, x, y, (214, 190, 128, 255))
    return bevel(img, 1.12, 0.84)


# ------------------------------------------------------------------- item textures
def item_base(size=16):
    return blank(size, size)


def crystal_shard(size=16):
    """Faceted shard: three planes, one specular."""
    img = item_base(size)
    for y in range(2, 14):
        for x in range(3, 13):
            left = (y - 2) * 0.55 + 3
            right = 12 - (y - 2) * 0.35
            if left <= x <= right:
                t = (x - left) / max(1.0, right - left)
                img[y][x] = mix((64, 156, 232, 255), (196, 240, 255, 255), t)
    for y in range(3, 12):
        img[y][5] = (244, 254, 255, 255)
    for y in range(4, 11):
        img[y][10] = (74, 140, 208, 255)
    for x in range(6, 10):
        img[2][x] = (226, 248, 255, 255)
    return outline_shape(lit_item(img), (16, 26, 44, 255), True)


def droplet_item(base_color, size=16):
    """A teardrop: pointed at the top, round and heavy at the bottom, with a wet highlight."""
    img = item_base(size)
    for y in range(1, 15):
        t = (y - 1) / 13.0
        # Narrow neck at the top, heavy round belly low down - a falling drop.
        r = 0.45 + 3.6 * (t ** 1.5) * (1.0 - 0.35 * max(0.0, t - 0.86) / 0.14)
        for x in range(size):
            d = abs(x - 7.5) / max(0.6, r)
            if d <= 1.0:
                img[y][x] = mix(shade(base_color, 1.35), shade(base_color, 0.7), d)
    for x in range(6, 10):
        img[1][x] = shade(base_color, 1.1)
    # Wet highlight and a darker underside.
    put(img, 6, 7, (255, 232, 242, 255))
    put(img, 5, 8, (255, 210, 228, 220))
    for x in range(5, 11):
        img[13][x] = shade(base_color, 0.62)
    return outline_shape(lit_item(img), (30, 8, 16, 255), True)

def dust_pile(primary, sparkle, size=16):
    img = item_base(size)
    rnd = random.Random(len(primary) * 31 + primary[0])
    for y in range(6, 14):
        span = int((y - 5) * 1.1)
        for x in range(8 - span, 8 + span):
            if 0 <= x < size and rnd.random() < 0.92:
                img[y][x] = shade(primary, 0.85 + rnd.random() * 0.4)
    for _ in range(3):
        put(img, rnd.randrange(5, 11), rnd.randrange(7, 12), sparkle)
    return outline_shape(lit_item(img), (12, 8, 20, 255), False)


def heart_crystal(primary, accent, size=16):
    """A gem cut in the shape of a heart: two lobes, a point, and a bright facet on one side."""
    img = item_base(size)
    for y in range(size):
        for x in range(size):
            nx = (x - 7.5) / 5.6
            ny = (y - 6.5) / 4.6
            inside = nx * nx + ny * ny < 1.0 or (y > 8 and abs(x - 7.5) < (14.5 - y) * 0.95)
            if not inside:
                continue
            facet = (x - 7.5) + (y - 7.0) * 0.8
            img[y][x] = shade(primary, 1.20 if facet < -1.5 else (0.78 if facet > 1.5 else 1.0))
    # Inner highlight plane and the tip.
    for y in range(4, 8):
        for x in range(4, 7):
            if img[y][x][3]:
                img[y][x] = accent
    put(img, 10, 4, (255, 255, 255, 255))
    for x in range(7, 9):
        img[13][x] = shade(primary, 0.62)
    return outline_shape(lit_item(img), (18, 10, 26, 255), True)

def wand_item(wood, head, corona, eclipse=False, size=16):
    img = item_base(size)
    for i in range(11):
        x = 3 + int(i * 0.85)
        y = 13 - i
        put(img, x, y, mix(shade(wood, 0.85), shade(wood, 1.15), (i % 3) / 2.0))
        put(img, x + 1, y, shade(wood, 0.72))
    cx, cy = 12, 3
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - cx, y - cy)
            if eclipse:
                if d < 2.2:
                    img[y][x] = (26, 16, 40, 255)
                elif d < 3.4:
                    img[y][x] = mix(head, corona, (d - 2.2) / 1.2)
            else:
                if d < 2.6:
                    t = 1.0 - d / 2.6
                    img[y][x] = mix(head, (255, 255, 255, 255), t * 0.85)
    if eclipse:
        for angle in range(0, 360, 12):
            rad = math.radians(angle)
            put(img, int(cx + math.cos(rad) * 3.9), int(cy + math.sin(rad) * 3.9), corona)
    else:
        put(img, cx - 1, cy - 1, (255, 255, 255, 255))
    return outline_shape(lit_item(img), (14, 10, 20, 255), True)


def astrolabe_item(brass, gem, size=16):
    img = item_base(size)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 6.4:
                img[y][x] = shade(brass, 0.8 + 0.35 * (1.0 - d / 6.4))
            if 4.4 < d < 5.2:
                img[y][x] = (60, 44, 24, 255)
            if d < 1.8:
                img[y][x] = gem
    for i in range(-5, 6):
        put(img, 7 + i, 7 - int(i * 0.25), shade(brass, 1.25))
    put(img, 7, 1, (230, 206, 140, 255))
    return outline_shape(lit_item(img), (26, 18, 10, 255), True)


def monocle_item(brass, lens, size=16):
    img = item_base(size)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - 8.5, y - 8.0)
            if d < 5.2:
                img[y][x] = (24, 40, 62, 255) if d > 4.2 else (
                    lens if d > 3.4 else mix(lens, (255, 255, 255, 255), 0.55))
            if 4.4 < d < 5.4:
                img[y][x] = shade(brass, 1.05)
    for i in range(4):
        put(img, 3 - i // 2, 11 + i, shade(brass, 0.9))
    put(img, 6, 5, (255, 255, 255, 255))
    return outline_shape(lit_item(img), (20, 14, 8, 255), True)


def tome_item(cover, glyph, size=16):
    img = item_base(size)
    for y in range(3, 14):
        for x in range(2, 14):
            img[y][x] = shade(cover, 0.9 + 0.2 * fbm(x, y, size, 77, 3, 2))
    for x in range(2, 14):
        img[3][x] = shade(cover, 1.25)
        img[13][x] = shade(cover, 0.7)
    for y in range(4, 13):
        img[y][2] = shade(cover, 0.6)
        img[y][13] = shade(cover, 1.2)
    for x in range(3, 13):
        img[6][x] = (226, 214, 190, 255)
        img[8][x] = (206, 192, 168, 255)
    # Star glyph and clasp.
    put(img, 7, 10, glyph)
    put(img, 8, 10, glyph)
    put(img, 7, 11, shade(glyph, 0.85))
    put(img, 8, 11, shade(glyph, 0.85))
    put(img, 12, 4, (232, 206, 132, 255))
    return outline_shape(lit_item(img), (10, 8, 18, 255), True)


def flask_item(liquid, cork, glow=False, size=16):
    img = item_base(size)
    for y in range(6, 14):
        r = 2.4 + (y - 6) * 0.45
        for x in range(size):
            d = math.hypot(x - 7.5, (y - 10.5) * 0.9)
            if d < r:
                t = 1.0 - d / r
                color = mix(shade(liquid, 0.7), shade(liquid, 1.25), t)
                if glow and d < r * 0.5:
                    color = mix(color, (255, 255, 255, 255), 0.35)
                img[y][x] = color
    for y in range(3, 7):
        for x in range(6, 10):
            img[y][x] = (198, 214, 226, 160)
    for x in range(6, 10):
        img[2][x] = cork
    put(img, 5, 9, (255, 255, 255, 200))
    return outline_shape(lit_item(img), (16, 18, 26, 255), True)


def clump_item(primary, sparkle, size=16):
    """A heaped pile: solid mound silhouette, coarse grain, a few bright specks."""
    img = item_base(size)
    rnd = random.Random(primary[0] + 7)
    profile = {6: 3, 7: 4, 8: 5, 9: 6, 10: 6, 11: 6, 12: 6, 13: 5}
    for y, half in profile.items():
        for x in range(8 - half, 8 + half):
            if 0 <= x < size:
                img[y][x] = shade(primary, 0.82 + rnd.random() * 0.42)
    for y in range(7, 14):
        for x in range(size):
            if img[y][x][3] and rnd.random() < 0.25:
                img[y][x] = shade(img[y][x], 1.25)
    for _ in range(3):
        put(img, rnd.randrange(5, 11), rnd.randrange(7, 12), sparkle)
    return outline_shape(lit_item(img), (12, 8, 18, 255), False)

def skin_texture(primary, secondary, accent, seed, size=64, glow_eyes=True):
    """
    64x64 biped skin layout (the same regions the vanilla player model samples).

    Every band gets fbm shading plus plate seams, so limbs read as armour rather than flat
    colour, and the head gets a brighter face plate with glowing eyes.
    """
    img = blank(size, size)
    regions = [
        (0, 0, 32, 16, primary),                    # head top/sides
        (16, 16, 40, 32, secondary),                # body
        (40, 16, 56, 32, shade(secondary, 0.86)),   # right arm
        (0, 16, 16, 32, shade(primary, 0.9)),       # left arm
        (32, 48, 16, 64, shade(secondary, 0.92)),   # right leg
        (16, 48, 32, 64, shade(primary, 0.82)),     # left leg
    ]
    for (x0, y0, x1, y1, color) in regions:
        for y in range(y0, y1):
            for x in range(x0, x1):
                v = fbm(x, y, size, seed, 4, 3)
                img[y][x] = shade(color, 0.84 + 0.32 * v)
    # Plate seams across the body bands.
    for y0, y1 in ((16, 32),):
        for y in range(y0, y1, 5):
            for x in range(16, 56):
                img[y][x] = shade(img[y][x], 0.72)
            for x in range(16, 56):
                img[y - 1][x] = shade(img[y - 1][x], 1.12) if y - 1 >= y0 else img[y - 1][x]
    # Face plate and eyes.
    for y in range(8, 16):
        for x in range(8, 16):
            img[y][x] = shade(img[y][x], 1.15)
    for (ex, ey) in ((10, 10), (13, 10)):
        img[ey][ex] = accent
        img[ey][ex + 1] = shade(accent, 0.8)
        if glow_eyes:
            img[ey - 1][ex] = mix(accent, (255, 255, 255, 255), 0.4)
    for x in range(11, 14):
        img[13][x] = (16, 10, 22, 255)
    # Chest sigil.
    for y in range(22, 26):
        for x in range(24, 28):
            img[y][x] = accent if (abs(x - 25.5) + abs(y - 23.5)) < 2.2 else shade(accent, 0.7)
    return img


def dragon_texture(size_w=256, size_h=256):
    """Atlas for the GeckoLib Nox'Thal model: three big material patches, matching the uv
    regions used by assets/aetherius/geo/entity/nox_thal.geo.json.

    scales (0,0 96x96) · membrane (0,96 96x96) · bone (100,0 32x32). Every cuboid samples one of
    those, and each patch is a little larger than the largest box footprint that uses it
    (2*(w+d) x (h+d)), so no part can bleed into a neighbouring material.
    """
    img = blank(size_w, size_h)

    # --- scales: violet plates, lit from the top, dark seams in a brick pattern
    scale_stops = [(0.0, (26, 16, 42, 255)), (0.45, (48, 30, 74, 255)),
                   (0.8, (72, 44, 108, 255)), (1.0, (98, 62, 142, 255))]
    for y in range(0, 96):
        for x in range(0, 96):
            v = fbm(x, y, 96, 21, 5, 3)
            img[y][x] = ramp(scale_stops, v)
    for row in range(0, 96, 6):
        for x in range(0, 96):
            img[row][x] = (20, 12, 32, 255)
        for x in range(0, 96, 9):
            px = x + (4 if (row // 6) % 2 else 0)
            for y in range(row, min(96, row + 6)):
                put(img, px % 96, y, (24, 14, 38, 255))
    # Highlight ridge along the top of each scale row.
    for row in range(1, 96, 6):
        for x in range(0, 96):
            img[row][x] = mix(img[row][x], (120, 82, 170, 255), 0.45)

    # --- membrane: translucent violet skin with darker veins
    membrane_stops = [(0.0, (52, 30, 82, 255)), (0.6, (86, 54, 128, 255)),
                      (1.0, (124, 84, 172, 255))]
    for y in range(96, 192):
        for x in range(0, 96):
            v = fbm(x, y, 96, 33, 3, 3)
            img[y][x] = ramp(membrane_stops, v)
    for i in range(0, 96, 11):
        for y in range(96, 192):
            xx = (i + int(math.sin(y * 0.16) * 3)) % 96
            img[y][xx] = (36, 20, 58, 255)
            img[y][(xx + 1) % 96] = (44, 26, 68, 255)

    # --- bone: pale horn with a faint marbled grain
    bone_stops = [(0.0, (176, 168, 196, 255)), (0.6, (212, 206, 228, 255)),
                  (1.0, (240, 236, 250, 255))]
    for y in range(0, 32):
        for x in range(100, 132):
            v = fbm(x, y, 32, 47, 3, 3)
            img[y][x] = ramp(bone_stops, v)

    # --- the skull's face lives inside the scale patch: brow ridge, maw, glowing eyes
    for x in range(58, 94):
        img[3][x] = (18, 10, 28, 255)
        img[4][x] = (36, 22, 54, 255)
        img[12][x] = (18, 10, 28, 255)
    for y in range(4, 13):
        for x in range(58, 94):
            if 5 <= y <= 11:
                img[y][x] = mix(img[y][x], (74, 46, 112, 255), 0.25)
    for (x0, x1) in ((61, 73), (79, 91)):
        for x in range(x0, x1):
            img[7][x] = (186, 96, 244, 255)
            img[8][x] = (236, 208, 255, 255)
            img[6][x] = (120, 54, 190, 255)
    for x in range(66, 86):
        img[11][x] = (250, 244, 255, 255)
    return img


def manta_texture(size=64):
    """Soft blue ray skin: vertical gradient, fin highlights, scattered spots."""
    img = blank(size, size)
    top = (168, 226, 252, 255)
    bottom = (86, 156, 208, 255)
    for y in range(size):
        for x in range(size):
            base = mix(top, bottom, y / float(size - 1))
            v = fbm(x, y, size, 55, 4, 3)
            img[y][x] = shade(base, 0.86 + 0.3 * v)
    rnd = random.Random(41)
    for _ in range(28):
        x, y = rnd.randrange(size), rnd.randrange(size)
        r = 1 if rnd.random() < 0.7 else 2
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if math.hypot(dx, dy) <= r:
                    put(img, x + dx, y + dy, (206, 242, 255, 255))
    for x in range(8, 24):
        for y in range(8, 16):
            img[y][x] = mix(img[y][x], (255, 255, 255, 255), 0.25)
    return img


# ------------------------------------------------------------------------- GUI
def bar_texture(fill_a, fill_b, background, seed, width=128, height=16):
    """Top half = empty frame, bottom half = filled; the HUD shifts the UV for the fill amount."""
    img = blank(width, height)
    for y in range(height):
        for x in range(width):
            v = fbm(x, y, width, seed, 8, 2)
            img[y][x] = shade(background, 0.85 + 0.3 * v)
    for x in range(width):
        img[0][x] = shade(background, 0.55)
        img[height - 1][x] = shade(background, 0.55)
    for y in range(height):
        img[y][0] = shade(background, 1.4)
        img[y][width - 1] = shade(background, 0.7)
    for x in range(2, width - 2):
        t = x / float(width)
        for y in range(2, height // 2 - 1):
            base = mix(fill_a, fill_b, t)
            gloss = 0.85 + 0.3 * (1.0 - (y - 2) / max(1.0, height / 2.0 - 3))
            img[y][x] = shade(base, gloss)
        img[height // 2 - 1][x] = shade(fill_b, 1.15)
    for x in range(2, width - 2, 8):
        for y in range(height // 2 + 1, height - 1):
            img[y][x] = shade(fill_b, 1.2)
    return img


def rune_pip(color, seed):
    """10x20 pip: an empty gem on top, a lit one below (swapped by UV when inscribed)."""
    img = blank(10, 20)
    for y in range(10):
        for x in range(10):
            d = math.hypot(x - 4.5, y - 4.5)
            if d < 4.2:
                img[y][x] = (40, 26, 62, 235) if d > 3.0 else (26, 16, 40, 230)
    for y in range(10, 20):
        for x in range(10):
            d = math.hypot(x - 4.5, (y - 10) - 4.5)
            if d < 4.2:
                img[y][x] = shade(color, 0.7) if d > 3.0 else (
                    mix(color, (255, 255, 255, 255), 0.5) if d < 1.6 else color)
    return img


def particle_sprite(color, seed, size=8):
    img = blank(size, size)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - (size - 1) / 2.0, y - (size - 1) / 2.0) / (size / 2.0)
            if d < 1.0:
                alpha = int(255 * (1.0 - d) ** 1.4)
                img[y][x] = (color[0], color[1], color[2], max(0, alpha))
    img[size // 2 - 1][size // 2 - 1] = (255, 255, 255, 255)
    return img


def icon_texture(size=128):
    """Mod icon: violet eclipse over a starlit horizon, with the dragon's eye."""
    img = blank(size, size, (10, 6, 20, 255))
    half = size / 2.0
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - half, y - half) / half
            if d < 1.0:
                img[y][x] = mix((26, 14, 48, 255), (8, 4, 16, 255), d)
    # Starfield.
    rnd = random.Random(7)
    for _ in range(90):
        x, y = rnd.randrange(size), rnd.randrange(size)
        if math.hypot(x - half, y - half) < half * 0.95:
            img[y][x] = (220, 230, 255, 255) if rnd.random() < 0.7 else (150, 170, 255, 255)
    # Eclipse disc with corona.
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - half, y - half * 0.92)
            if d < half * 0.42:
                img[y][x] = (16, 8, 26, 255) if d < half * 0.36 else (36, 18, 56, 255)
            elif d < half * 0.5:
                t = (d - half * 0.42) / (half * 0.08)
                img[y][x] = mix((186, 110, 255, 255), (255, 170, 90, 255), t)
    # Dragon eye slit.
    for x in range(int(half * 0.72), int(half * 1.28)):
        y = int(half * 0.92 + math.sin((x - half) * 0.35) * 3)
        for dy in range(-3, 4):
            put(img, x, y + dy, (255, 214, 120, 255) if abs(dy) < 2 else (214, 120, 40, 255))
    # Horizon glow.
    for x in range(size):
        for y in range(int(size * 0.78), size):
            fade = 1.0 - (y - size * 0.78) / (size * 0.22)
            img[y][x] = mix(img[y][x], (120, 70, 210, 255), max(0.0, fade) * 0.35)
    return img


# ------------------------------------------------------------------------- main
def main():
    blocks = os.path.join(ROOT, "block")
    items = os.path.join(ROOT, "item")
    entity = os.path.join(ROOT, "entity")
    gui = os.path.join(ROOT, "gui")
    particle = os.path.join(ROOT, "particle")

    def save_block(name, img):
        write_png(os.path.join(blocks, name + ".png"), 16, 16, img)

    def save_item(name, img):
        write_png(os.path.join(items, name + ".png"), 16, 16, img)

    # ---------------------------------------------------------------- blocks
    stone_stops = [(0.0, (34, 30, 44, 255)), (0.5, (52, 46, 64, 255)), (1.0, (72, 64, 86, 255))]
    save_block("star_vein_ore", ore_tile(
        16, 11, stone_stops,
        [(0.0, (58, 30, 110, 255)), (0.6, (132, 78, 232, 255)), (1.0, (216, 176, 255, 255))],
        clusters=5, glow=(255, 248, 255, 255)))

    blood_stone = [(0.0, (36, 26, 30, 255)), (0.5, (56, 40, 44, 255)), (1.0, (78, 56, 60, 255))]
    save_block("aether_blood_ore", ore_tile(
        16, 12, blood_stone,
        [(0.0, (72, 14, 34, 255)), (0.6, (176, 40, 78, 255)), (1.0, (255, 150, 178, 255))],
        clusters=5, glow=(255, 226, 236, 255)))

    save_block("star_ash", ash_tile(16, 13))
    save_block("starlit_grass_block_top", grass_top(16, 14))
    save_block("starlit_grass_block_side", grass_side(16, 15))
    save_block("mana_lily", lily_sprite(16, 16))
    save_block("aether_fountain", fountain_face(16, 17))
    save_block("celestial_pillar", pillar_side(16, 18))
    save_block("celestial_pillar_top", pillar_top(16, 19))
    save_block("aether_forge_top", forge_top(16, 20))
    save_block("aether_forge_side", forge_side(16, 21))
    save_block("aether_forge_front", forge_front(16, 22))
    save_block("aether_altar_core", altar_core(16, 23))
    save_block("aether_altar_core_top", altar_top(16, 24))
    save_block("telescope", telescope_face(16, 25))
    save_block("rune_lantern", lantern_sprite(16, 26))
    save_block("sky_rift_anchor", anchor_face(16, 27))
    save_block("sky_rift_portal", portal_face(16, 28))

    # ----------------------------------------------------------------- items
    save_item("aether_shard", crystal_shard())
    save_item("aether_blood", droplet_item((186, 28, 68, 255)))
    save_item("void_dust", dust_pile((74, 52, 118, 255), (196, 170, 255, 255)))
    save_item("star_ash_clump", clump_item((120, 116, 132, 255), (214, 186, 255, 255)))
    save_item("heart_of_sol", heart_crystal((236, 118, 40, 255), (255, 238, 186, 255)))
    save_item("heart_of_luna", heart_crystal((104, 190, 250, 255), (238, 250, 255, 255)))
    save_item("heart_of_terra", heart_crystal((108, 168, 84, 255), (232, 218, 150, 255)))
    save_item("heart_of_caelum", heart_crystal((196, 208, 250, 255), (255, 255, 255, 255)))
    save_item("astral_wand", wand_item((104, 74, 44, 255), (128, 208, 255, 255),
                                       (236, 250, 255, 255)))
    save_item("eclipse_wand", wand_item((58, 40, 34, 255), (86, 40, 140, 255),
                                        (255, 148, 60, 255), eclipse=True))
    save_item("astrolabe", astrolabe_item((196, 172, 108, 255), (150, 96, 232, 255)))
    save_item("aethersight_monocle", monocle_item((206, 176, 96, 255), (150, 216, 250, 255)))
    save_item("chronicle_tome", tome_item((44, 30, 76, 255), (176, 148, 255, 255)))
    save_item("starlight_elixir", flask_item((104, 186, 246, 255), (150, 116, 76, 255)))
    save_item("purified_starlight_elixir", flask_item((232, 244, 255, 255), (196, 158, 96, 255),
                                                      glow=True))

    # ------------------------------------------------------------------- GUI
    write_png(os.path.join(gui, "mana_bar.png"), 128, 16,
              bar_texture((32, 104, 190, 255), (128, 216, 255, 255), (14, 10, 26, 255), 51))
    write_png(os.path.join(gui, "tension_bar.png"), 128, 16,
              bar_texture((92, 36, 168, 255), (206, 132, 255, 255), (14, 10, 26, 255), 52))
    write_png(os.path.join(gui, "rune_pip.png"), 10, 20, rune_pip((150, 220, 255, 255), 53))

    # --------------------------------------------------------------- entity
    write_png(os.path.join(entity, "void_sentinel.png"), 64, 64,
              skin_texture((26, 18, 44, 255), (44, 28, 74, 255), (170, 96, 255, 255), 61))
    write_png(os.path.join(entity, "void_specter.png"), 64, 64,
              skin_texture((16, 12, 26, 255), (30, 20, 48, 255), (226, 236, 255, 255), 62))
    write_png(os.path.join(entity, "void_warped_abomination.png"), 64, 64,
              skin_texture((52, 40, 28, 255), (76, 58, 42, 255), (176, 96, 255, 255), 63))
    write_png(os.path.join(entity, "elemental_warden.png"), 64, 64,
              skin_texture((58, 56, 68, 255), (88, 76, 112, 255), (255, 198, 106, 255), 64))
    write_png(os.path.join(entity, "nox_thal.png"), 256, 256, dragon_texture())
    write_png(os.path.join(entity, "aether_manta.png"), 64, 64, manta_texture())

    # ------------------------------------------------------------- particles
    write_png(os.path.join(particle, "astral_spark.png"), 8, 8,
              particle_sprite((184, 238, 255), 71))
    write_png(os.path.join(particle, "void_mote.png"), 8, 8,
              particle_sprite((158, 96, 255), 72))
    write_png(os.path.join(particle, "sol_ember.png"), 8, 8,
              particle_sprite((255, 152, 72), 73))
    write_png(os.path.join(particle, "star_dust.png"), 8, 8,
              particle_sprite((255, 250, 214), 74))
    write_png(os.path.join(particle, "blight_spore.png"), 8, 8,
              particle_sprite((124, 82, 184), 75))

    # --------------------------------------------------------------- mod icon
    # fabric.mod.json points at assets/aetherius/icon.png, one level above textures/.
    write_png(os.path.join(ROOT, "..", "icon.png"), 128, 128, icon_texture())

    print("textures written to", ROOT)


if __name__ == "__main__":
    main()
