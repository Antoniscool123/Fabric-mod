#!/usr/bin/env python3
"""
Aetherius texture generator.

Writes every PNG the mod needs, procedurally, with zero third-party dependencies
(no PIL): raw RGBA buffers are zlib-compressed into valid PNG chunks.

Run from the repo root:  python3 tools/generate_textures.py
Outputs into src/main/resources/assets/aetherius/textures/**.

The art is intentionally simple, deterministic placeholder art that still reads at
16x16: each generator takes a palette and a structural rule (ore veins, pillars,
lantern frame, skin regions) so blocks are distinguishable in-game while the real
art pass is still pending.
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


# ----------------------------------------------------------------------- helpers
def noise_tile(size, base, variance, seed, speckle=None, speckle_chance=0.0):
    rnd = random.Random(seed)
    img = blank(size, size)
    for y in range(size):
        for x in range(size):
            f = 1.0 + rnd.uniform(-variance, variance)
            color = shade(base, f)
            if speckle and rnd.random() < speckle_chance:
                color = speckle
            img[y][x] = color
    return img


def draw_border(img, color):
    h = len(img)
    w = len(img[0])
    for x in range(w):
        img[0][x] = color
        img[h - 1][x] = color
    for y in range(h):
        img[y][0] = color
        img[y][w - 1] = color
    return img


def draw_rect(img, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(len(img), y1)):
        for x in range(max(0, x0), min(len(img[0]), x1)):
            img[y][x] = color
    return img


def ore_texture(stone, vein, glow, seed, ore_dots=7):
    img = noise_tile(16, stone, 0.09, seed)
    rnd = random.Random(seed + 7)
    for _ in range(ore_dots):
        cx, cy = rnd.randrange(1, 15), rnd.randrange(1, 15)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if abs(dx) + abs(dy) > 1 and rnd.random() < 0.5:
                    continue
                x, y = cx + dx, cy + dy
                if 0 <= x < 16 and 0 <= y < 16:
                    img[y][x] = glow if (dx == 0 and dy == 0) else vein
    return img


def pillar_texture(base, accent, seed):
    img = noise_tile(16, base, 0.06, seed)
    for x in (3, 4, 11, 12):
        for y in range(16):
            img[y][x] = accent
    for y in (0, 7, 15):
        for x in range(16):
            img[y][x] = shade(accent, 1.15)
    return img


def crystal_block(base, glow, seed):
    img = noise_tile(16, base, 0.08, seed)
    rnd = random.Random(seed + 3)
    for _ in range(5):
        x0, y0 = rnd.randrange(1, 12), rnd.randrange(1, 12)
        for i in range(rnd.randrange(3, 7)):
            x, y = x0 + i, y0 + i // 2
            if x < 15 and y < 15:
                img[y][x] = glow
    return img


def cross_sprite(petal, center, seed):
    img = blank(16, 16)
    rnd = random.Random(seed)
    draw_rect(img, 7, 8, 9, 16, (58, 120, 74, 255))      # stem
    for dy in range(4, 9):
        for dx in range(4, 12):
            dist = math.hypot(dx - 7.5, dy - 6.5)
            if dist < 3.4:
                img[dy][dx] = petal if dist > 1.4 else center
    for _ in range(6):
        x, y = rnd.randrange(4, 12), rnd.randrange(4, 9)
        if img[y][x][3] == 0:
            img[y][x] = shade(petal, 1.2)
    return img


def lantern_sprite(frame, glass, glow, seed):
    img = noise_tile(16, frame, 0.05, seed)
    draw_rect(img, 4, 3, 12, 13, shade(frame, 0.75))
    draw_rect(img, 5, 4, 11, 12, glass)
    draw_rect(img, 7, 6, 9, 10, glow)
    draw_rect(img, 6, 0, 10, 2, shade(frame, 1.1))
    return img


def item_sprite(shape, primary, accent, seed):
    """Tiny procedural icons: shard / droplet / heart / wand / orb / book / flask / disc."""
    img = blank(16, 16)
    rnd = random.Random(seed)
    if shape == "shard":
        for y in range(2, 14):
            span = min(5, (y - 2) if y < 8 else (14 - y))
            for x in range(8 - span, 8 + span):
                img[y][x] = primary if (x + y) % 5 else accent
        draw_rect(img, 7, 1, 9, 3, accent)
    elif shape == "droplet":
        for y in range(3, 14):
            r = 1 + (y - 3) * 0.45
            for x in range(8 - int(r), 8 + int(r)):
                if 0 <= x < 16:
                    img[y][x] = primary if y < 10 else shade(primary, 0.8)
        draw_rect(img, 7, 1, 9, 4, accent)
        img[7][6] = accent
    elif shape == "heart":
        for y in range(3, 14):
            for x in range(2, 14):
                dx, dy = (x - 7.5) / 5.0, (y - 6.0) / 4.0
                if dx * dx + dy * dy < 1.0 or (y > 8 and abs(x - 7.5) < (14 - y) * 0.9):
                    img[y][x] = primary if (x < 8) else shade(primary, 0.85)
        draw_rect(img, 6, 5, 8, 7, accent)
    elif shape == "wand":
        for i in range(10):
            img[13 - i][2 + i] = (92, 62, 32, 255) if i % 2 else (120, 84, 46, 255)
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if abs(dx) + abs(dy) <= 2:
                    img[4 + dy][12 + dx] = primary if (dx + dy) % 2 else accent
    elif shape == "orb":
        for y in range(2, 14):
            for x in range(2, 14):
                dist = math.hypot(x - 7.5, y - 7.5)
                if dist < 5.6:
                    img[y][x] = accent if dist > 4.0 else primary
        for angle in range(0, 360, 45):
            rad = math.radians(angle)
            x = int(7.5 + math.cos(rad) * 6)
            y = int(7.5 + math.sin(rad) * 6)
            if 0 <= x < 16 and 0 <= y < 16:
                img[y][x] = accent
    elif shape == "book":
        draw_rect(img, 3, 2, 13, 14, primary)
        draw_rect(img, 4, 3, 12, 13, shade(primary, 1.18))
        draw_rect(img, 7, 2, 9, 14, accent)
        draw_rect(img, 5, 5, 11, 6, accent)
        draw_rect(img, 5, 9, 11, 10, accent)
    elif shape == "flask":
        draw_rect(img, 6, 1, 10, 4, (210, 220, 235, 255))
        for y in range(4, 14):
            r = 2 + (y - 4) * 0.55
            for x in range(int(8 - r), int(9 + r)):
                if 0 <= x < 16:
                    img[y][x] = primary if y > 7 else shade(primary, 0.7)
        draw_rect(img, 7, 9, 9, 11, accent)
    elif shape == "disc":
        for y in range(2, 14):
            for x in range(2, 14):
                dist = math.hypot(x - 7.5, y - 7.5)
                if 3.0 < dist < 5.6:
                    img[y][x] = primary
                elif dist <= 3.0:
                    img[y][x] = accent
        draw_rect(img, 7, 2, 9, 4, accent)
    elif shape == "monocle":
        for y in range(3, 13):
            for x in range(3, 13):
                dist = math.hypot(x - 7.5, y - 7.5)
                if 3.4 < dist < 5.2:
                    img[y][x] = primary
                elif dist <= 3.4:
                    img[y][x] = (140, 210, 255, 170)
        for i in range(4):
            img[12 + i][12] = accent
            img[12 + i][13] = shade(accent, 0.8)
    elif shape == "clump":
        for _ in range(40):
            x, y = rnd.randrange(2, 14), rnd.randrange(2, 14)
            img[y][x] = primary if rnd.random() < 0.7 else accent
    else:
        draw_rect(img, 2, 2, 14, 14, primary)
    return img


def bar_texture(fill_colors, background, seed):
    """128x16: top half = empty frame, bottom half = filled (revealed by UV offset)."""
    img = noise_tile(128, background, 0.015, seed)
    for x in range(128):
        img[0][x] = shade(background, 0.7)
        img[15][x] = shade(background, 0.7)
    for y in range(16):
        img[y][0] = shade(background, 0.7)
        img[y][127] = shade(background, 0.7)
    fill = noise_tile(128, fill_colors[0], 0.03, seed + 1, speckle=fill_colors[1],
                      speckle_chance=0.05)
    for y in range(8):
        for x in range(1, 127):
            factor = 0.82 + 0.18 * abs(math.cos(x * 0.11))
            img[y][x] = shade(fill[y][x], factor)
    draw_rect(img, 1, 9, 127, 15, fill_colors[1])
    for x in range(1, 127, 8):
        for y in range(9, 15):
            img[y][x] = shade(fill_colors[1], 1.25)
    for y in range(16):
        img[y][1] = shade(background, 1.25)
        img[y][126] = shade(background, 1.25)
    return img


def rune_pip(color, seed):
    img = blank(10, 20)
    rnd = random.Random(seed)
    for y in range(10):
        for x in range(10):
            edge = x in (0, 9) or y in (0, 9)
            img[y][x] = (24, 16, 40, 235) if edge else (46, 30, 74, 220)
    for _ in range(8):
        x, y = rnd.randrange(2, 8), rnd.randrange(2, 8)
        img[y][x] = color
    img[5][5] = shade(color, 1.4)
    return img


def skin_texture(primary, secondary, accent, seed, size=64):
    """64x64 biped skin layout: head/body/arms/legs bands with per-region shading."""
    rnd = random.Random(seed)
    img = blank(size, size)
    regions = [
        (0, 0, 32, 16, primary),        # head wrap
        (16, 16, 40, 32, secondary),    # body
        (40, 16, 56, 32, shade(secondary, 0.85)),  # right arm
        (32, 48, 48, 64, shade(secondary, 0.9)),   # right leg
        (0, 16, 16, 32, shade(primary, 0.9)),      # left arm
        (16, 48, 32, 64, shade(primary, 0.8)),     # left leg
    ]
    for (x0, y0, x1, y1, color) in regions:
        for y in range(y0, y1):
            for x in range(x0, x1):
                img[y][x] = shade(color, 1.0 + rnd.uniform(-0.08, 0.08))
    # Face plate (front of head is at 8..16, 8..16 in the classic layout).
    draw_rect(img, 8, 8, 16, 16, shade(primary, 1.1))
    img[10][10] = accent
    img[10][13] = accent
    draw_rect(img, 12, 12, 14, 13, accent)
    # Chest sigil.
    draw_rect(img, 24, 22, 28, 26, accent)
    return img


def dragon_texture(size_w=128, size_h=160):
    img = blank(size_w, size_h)
    rnd = random.Random(99)
    scale_dark = (26, 16, 46, 255)
    scale_light = (58, 36, 92, 255)
    for y in range(size_h):
        for x in range(size_w):
            if rnd.random() < 0.06:
                continue
            img[y][x] = scale_light if (x + y) % 7 == 0 else scale_dark
    draw_rect(img, 64, 0, 76, 30, (150, 60, 200, 255))     # eyes / glow band
    draw_rect(img, 0, 100, 60, 112, (90, 50, 130, 255))    # wing membrane
    return img


def manta_texture(size=64):
    img = blank(size, size)
    rnd = random.Random(41)
    for y in range(size):
        for x in range(size):
            v = 0.9 + rnd.uniform(-0.1, 0.1)
            img[y][x] = shade((120, 205, 245, 255), v)
    draw_rect(img, 8, 8, 24, 16, (196, 240, 255, 255))
    return img


def particle_sprite(color, seed):
    img = blank(8, 8)
    rnd = random.Random(seed)
    for y in range(8):
        for x in range(8):
            dist = math.hypot(x - 3.5, y - 3.5)
            if dist < 3.2:
                alpha = int(255 * (1.0 - dist / 3.2))
                img[y][x] = (color[0], color[1], color[2], max(30, alpha))
    img[3][3] = shade(color, 1.5)
    return img


def icon_texture(size=128):
    img = blank(size, size, (10, 6, 20, 255))
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - size / 2, y - size / 2) / (size / 2)
            if d < 1.0:
                img[y][x] = (14 + int(30 * d), 8 + int(12 * d), 30 + int(50 * d), 255)
    # A broken ring with a dragon silhouette hint.
    for angle in range(0, 360, 2):
        rad = math.radians(angle)
        for r in (44, 45):
            x = int(size / 2 + math.cos(rad) * r)
            y = int(size / 2 + math.sin(rad) * r)
            if 0 <= x < size and 0 <= y < size and not (110 < angle < 170):
                img[y][x] = (155, 77, 255, 255)
    for i in range(30):
        x = size // 2 + int(math.sin(i * 0.4) * 18) - 3
        y = 30 + i * 2
        for dx in range(6):
            for dy in range(3):
                if 0 <= x + dx < size and 0 <= y + dy < size:
                    img[y + dy][x + dx] = (255, 138, 61, 255) if i % 4 == 0 else (60, 30, 90, 255)
    return img


def main():
    blocks = os.path.join(ROOT, "block")
    items = os.path.join(ROOT, "item")
    entity = os.path.join(ROOT, "entity")
    gui = os.path.join(ROOT, "gui")
    particle = os.path.join(ROOT, "particle")

    # ---- Blocks ----
    def save_block(name, img):
        write_png(os.path.join(blocks, name + ".png"), 16, 16, img)

    save_block("star_vein_ore", ore_texture((44, 40, 60, 255), (120, 70, 220, 255),
                                           (215, 170, 255, 255), 11))
    save_block("aether_blood_ore", ore_texture((58, 44, 52, 255), (168, 40, 78, 255),
                                              (255, 140, 170, 255), 12))
    save_block("star_ash", noise_tile(16, (38, 34, 42, 255), 0.14, 13,
                                      speckle=(96, 70, 130, 255), speckle_chance=0.05))
    save_block("starlit_grass_block_top", noise_tile(16, (74, 150, 220, 255), 0.1, 14,
                                                    speckle=(180, 230, 255, 255),
                                                    speckle_chance=0.04))
    save_block("starlit_grass_block_side", noise_tile(16, (96, 72, 56, 255), 0.1, 15))
    grass_side = noise_tile(16, (96, 72, 56, 255), 0.1, 15)
    for x in range(16):
        for y in range(0, 4):
            grass_side[y][x] = shade((74, 150, 220, 255), 1.0 + (y * 0.03))
    save_block("starlit_grass_block_side", grass_side)
    save_block("mana_lily", cross_sprite((150, 220, 255, 255), (255, 245, 160, 255), 16))
    save_block("aether_fountain", crystal_block((30, 60, 96, 255), (150, 225, 255, 255), 17))
    save_block("celestial_pillar", pillar_texture((214, 208, 196, 255), (150, 120, 220, 255), 18))
    save_block("celestial_pillar_top", noise_tile(16, (226, 220, 206, 255), 0.05, 19,
                                                  speckle=(160, 130, 230, 255),
                                                  speckle_chance=0.08))
    save_block("aether_forge_top", crystal_block((22, 46, 74, 255), (120, 220, 255, 255), 20))
    save_block("aether_forge_side", noise_tile(16, (34, 58, 86, 255), 0.08, 21,
                                              speckle=(90, 200, 255, 255),
                                              speckle_chance=0.06))
    save_block("aether_forge_front", noise_tile(16, (40, 66, 96, 255), 0.07, 22))
    forge_front = noise_tile(16, (40, 66, 96, 255), 0.07, 22)
    draw_rect(forge_front, 5, 5, 11, 12, (14, 20, 32, 255))
    draw_rect(forge_front, 6, 7, 10, 11, (255, 170, 80, 255))
    save_block("aether_forge_front", forge_front)
    save_block("aether_altar_core", crystal_block((238, 236, 246, 255), (170, 120, 255, 255), 23))
    save_block("aether_altar_core_top", crystal_block((246, 244, 252, 255), (140, 90, 240, 255), 24))
    save_block("telescope", pillar_texture((70, 62, 58, 255), (200, 170, 90, 255), 25))
    save_block("rune_lantern", lantern_sprite((92, 70, 34, 255), (255, 216, 120, 200),
                                              (255, 250, 190, 255), 26))
    save_block("sky_rift_anchor", noise_tile(16, (18, 12, 26, 255), 0.12, 27,
                                            speckle=(120, 70, 220, 255), speckle_chance=0.1))
    portal = blank(16, 16, (18, 8, 32, 235))
    for y in range(16):
        for x in range(16):
            if (x * 3 + y * 5) % 11 == 0:
                portal[y][x] = (150, 90, 255, 220)
            elif (x + y) % 13 == 0:
                portal[y][x] = (230, 220, 255, 200)
    save_block("sky_rift_portal", portal)

    # ---- Items ----
    def save_item(name, img):
        write_png(os.path.join(items, name + ".png"), 16, 16, img)

    save_item("aether_shard", item_sprite("shard", (150, 225, 255, 255), (255, 255, 255, 255), 31))
    save_item("aether_blood", item_sprite("droplet", (190, 32, 72, 255), (255, 150, 180, 255), 32))
    save_item("void_dust", item_sprite("clump", (94, 62, 150, 255), (180, 150, 255, 255), 33))
    save_item("star_ash_clump", item_sprite("clump", (48, 44, 56, 255), (120, 100, 170, 255), 34))
    save_item("heart_of_sol", item_sprite("heart", (255, 128, 48, 255), (255, 240, 180, 255), 35))
    save_item("heart_of_luna", item_sprite("heart", (110, 200, 255, 255), (240, 250, 255, 255), 36))
    save_item("heart_of_terra", item_sprite("heart", (120, 170, 90, 255), (230, 210, 140, 255), 37))
    save_item("heart_of_caelum", item_sprite("heart", (220, 230, 255, 255), (150, 200, 255, 255), 38))
    save_item("astral_wand", item_sprite("wand", (130, 210, 255, 255), (255, 255, 255, 255), 39))
    save_item("eclipse_wand", item_sprite("wand", (150, 60, 220, 255), (255, 140, 60, 255), 40))
    save_item("astrolabe", item_sprite("disc", (200, 180, 120, 255), (130, 90, 220, 255), 41))
    save_item("aethersight_monocle", item_sprite("monocle", (206, 176, 96, 255), (255, 230, 160, 255), 42))
    save_item("chronicle_tome", item_sprite("book", (46, 32, 78, 255), (160, 130, 255, 255), 43))
    save_item("starlight_elixir", item_sprite("flask", (120, 200, 255, 255), (255, 255, 255, 255), 44))
    save_item("purified_starlight_elixir", item_sprite("flask", (230, 240, 255, 255),
                                                      (255, 200, 120, 255), 45))
    # ---- GUI ----
    write_png(os.path.join(gui, "mana_bar.png"), 128, 16,
              bar_texture([(40, 120, 200, 255), (110, 205, 255, 255)], (16, 12, 30, 255), 51))
    write_png(os.path.join(gui, "tension_bar.png"), 128, 16,
              bar_texture([(96, 40, 170, 255), (190, 110, 255, 255)], (16, 12, 30, 255), 52))
    write_png(os.path.join(gui, "rune_pip.png"), 10, 20, rune_pip((150, 220, 255, 255), 53))

    # ---- Entities ----
    write_png(os.path.join(entity, "void_sentinel.png"), 64, 64,
              skin_texture((26, 18, 44, 255), (44, 28, 74, 255), (160, 90, 255, 255), 61))
    write_png(os.path.join(entity, "void_specter.png"), 64, 64,
              skin_texture((18, 12, 28, 255), (30, 20, 48, 255), (220, 230, 255, 255), 62))
    write_png(os.path.join(entity, "void_warped_abomination.png"), 64, 64,
              skin_texture((52, 40, 28, 255), (74, 56, 40, 255), (170, 90, 255, 255), 63))
    write_png(os.path.join(entity, "elemental_warden.png"), 64, 64,
              skin_texture((60, 58, 70, 255), (86, 74, 110, 255), (255, 200, 110, 255), 64))
    write_png(os.path.join(entity, "nox_thal.png"), 128, 160, dragon_texture())
    write_png(os.path.join(entity, "aether_manta.png"), 64, 64, manta_texture())

    # ---- Particles ----
    write_png(os.path.join(particle, "astral_spark.png"), 8, 8,
              particle_sprite((180, 235, 255), 71))
    write_png(os.path.join(particle, "void_mote.png"), 8, 8,
              particle_sprite((150, 90, 255), 72))
    write_png(os.path.join(particle, "sol_ember.png"), 8, 8,
              particle_sprite((255, 150, 70), 73))
    write_png(os.path.join(particle, "star_dust.png"), 8, 8,
              particle_sprite((255, 250, 210), 74))
    write_png(os.path.join(particle, "blight_spore.png"), 8, 8,
              particle_sprite((120, 80, 180), 75))

    # ---- Mod icon ----
    # fabric.mod.json points at assets/aetherius/icon.png, one level above textures/.
    write_png(os.path.join(ROOT, "..", "icon.png"), 128, 128, icon_texture())

    print("textures written to", ROOT)


if __name__ == "__main__":
    main()
