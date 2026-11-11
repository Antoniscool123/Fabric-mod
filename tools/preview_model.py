#!/usr/bin/env python3
"""Render NoxThalModel's cuboids to a PNG without Minecraft.

The model is pure data (pivots + cuboid offsets), so it can be rasterised offline. This exists
because the model has twice shipped with boxes anchored in the wrong space, and a screenshot from
the game costs a release cycle. The script reads the numbers straight out of the Java file, so the
preview cannot drift from the model.

Usage: python3 tools/preview_model.py [out.png]
"""
import math
import os
import re
import struct
import sys
import zlib

BASE = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
MODEL = os.path.join(BASE, "src", "main", "java", "com", "aetherius", "client",
                     "render", "NoxThalModel.java")

PANEL_W, PANEL_H = 560, 620
WIDTH, HEIGHT = PANEL_W * 3, PANEL_H
SCALE = 4.4
VIEWS = [(90.0, 4.0, "side"), (140.0, 16.0, "three-quarter"), (92.0, 88.0, "top")]

MATERIAL_COLORS = {
    "scale": (78, 50, 122),
    "membrane": (120, 84, 170),
    "bone": (214, 206, 232),
}


CUBOID_RE = re.compile(r"\.cuboid\(([-\d.f]+),\s*([-\d.f]+),\s*([-\d.f]+),\s*([-\d.f]+),"
                       r"\s*([-\d.f]+),\s*([-\d.f]+)\)")
MARKER_RE = re.compile(r"(bone\(\)|membrane\(\)|scale\(\)|\.uv\(UV_(\w+)_X)")


def material_of(marker):
    if marker.startswith("bone"):
        return "bone"
    if marker.startswith("membrane"):
        return "membrane"
    if marker.startswith("scale"):
        return "scale"
    name = marker.split("UV_")[1].split("_X")[0]
    return {"BONE": "bone", "MEMBRANE": "membrane"}.get(name, "scale")


def boxes_in(body, default="scale"):
    """Walk a model part body in order, attributing every cuboid to the material marker before it."""
    marks = [(m.start(), material_of(m.group(1))) for m in MARKER_RE.finditer(body)]
    boxes = []
    for m in CUBOID_RE.finditer(body):
        material = default
        for pos, name in marks:
            if pos < m.start():
                material = name
            else:
                break
        x, y, z, w, h, d = (float(v.rstrip("f")) for v in m.groups())
        boxes.append({"material": material, "min": (x, y, z), "max": (x + w, y + h, z + d)})
    return boxes


def parse_model():
    src = open(MODEL, encoding="utf-8").read()
    # the shared leg() helper: its cuboids are instantiated by four addChild calls
    leg_body = src.split("private static ModelPartBuilder leg(boolean mirrored)")[1].split("\n    }")[0]
    leg_boxes = boxes_in(leg_body, default="bone")
    parts = []
    offsets = {"root": (0.0, 0.0, 0.0)}
    for m in re.finditer(r'(\w+)\.addChild\("([a-z_]+)"', src):
        parent, name = m.group(1), m.group(2)
        body = src[m.end():].split(");", 1)[0]
        pivot = re.search(r'ModelTransform\.pivot\(([-\d.f]+),\s*([-\d.f]+),\s*([-\d.f]+)\)', body)
        if not pivot:
            continue
        local = tuple(float(v.rstrip("f")) for v in pivot.groups())
        base = offsets.get(parent, (0.0, 0.0, 0.0))
        abs_pivot = tuple(base[i] + local[i] for i in range(3))
        offsets[name] = abs_pivot
        boxes = leg_boxes if "leg(" in body else boxes_in(body)
        parts.append({"name": name, "pivot": abs_pivot, "boxes": boxes,
                      "parent": parent if parent != "root" else None,
                      "local": local})
    return parts


def rotate_xyz(point, pitch, yaw, roll):
    """Model-space part rotation, matching ModelPart's pitch/yaw/roll order closely enough."""
    x, y, z = point
    cp, sp = math.cos(pitch), math.sin(pitch)
    y, z = y * cp - z * sp, y * sp + z * cp
    cy, sy = math.cos(yaw), math.sin(yaw)
    x, z = x * cy + z * sy, -x * sy + z * cy
    cr, sr = math.cos(roll), math.sin(roll)
    x, y = x * cr - y * sr, x * sr + y * cr
    return x, y, z


# Pose used by setAngles() for the airborne state, so the preview shows what players will see.
POSE = {
    "left_wing": (0.04, 0.30, -0.30),
    "right_wing": (0.04, -0.30, 0.30),
    "front_left_leg": (0.55, 0.0, 0.0),
    "front_right_leg": (0.45, 0.0, 0.0),
    "back_left_leg": (-0.35, 0.0, 0.0),
    "back_right_leg": (-0.25, 0.0, 0.0),
}


def rotate(point, yaw, pitch):
    x, y, z = point
    # yaw around the vertical axis (y is down in model space)
    cy, sy = math.cos(yaw), math.sin(yaw)
    x, z = x * cy - z * sy, x * sy + z * cy
    # pitch: tilt the camera down a little
    cp, sp = math.cos(pitch), math.sin(pitch)
    y, z = y * cp - z * sp, y * sp + z * cp
    return x, y, z


def project(point, yaw, pitch, center, zoom):
    x, y, z = rotate(point, yaw, pitch)
    return (center[0] + x * zoom, center[1] + y * zoom, z)


def fill_polygon(buf, points, color):
    ys = [p[1] for p in points]
    y0, y1 = max(0, int(min(ys))), min(HEIGHT - 1, int(max(ys)) + 1)
    for y in range(y0, y1):
        row = y * WIDTH
        xs = []
        for i in range(len(points)):
            ax, ay = points[i][0], points[i][1]
            bx, by = points[(i + 1) % len(points)][0], points[(i + 1) % len(points)][1]
            if (ay <= y < by) or (by <= y < ay):
                xs.append(ax + (bx - ax) * (y - ay) / (by - ay))
        xs.sort()
        for i in range(0, len(xs) - 1, 2):
            x0 = max(0, int(math.ceil(xs[i])))
            x1 = min(WIDTH - 1, int(math.floor(xs[i + 1])))
            for x in range(x0, x1 + 1):
                o = (row + x) * 3
                buf[o:o + 3] = bytes(color)


def shade(color, factor):
    return tuple(min(255, max(0, int(c * factor))) for c in color)


FACE_DEFS = [
    # (corner indices, normal axis/sign, shade factor)
    ((0, 1, 3, 2), "y-", 1.18),
    ((4, 6, 7, 5), "y+", 0.72),
    ((0, 4, 5, 1), "z-", 1.05),
    ((2, 3, 7, 6), "z+", 0.82),
    ((0, 2, 6, 4), "x-", 0.94),
    ((1, 5, 7, 3), "x+", 0.94),
]


def render_view(buf, parts, offset, yaw_deg, pitch_deg):
    yaw, pitch = math.radians(yaw_deg), math.radians(pitch_deg)
    center = (offset[0] + PANEL_W * 0.5, offset[1] + PANEL_H * 0.52)
    faces = []
    for part in parts:
        px, py, pz = part["pivot"]
        pose = POSE.get(part["name"], (0.0, 0.0, 0.0))
        for box in part["boxes"]:
            (x0, y0, z0), (x1, y1, z1) = box["min"], box["max"]
            corners = [
                (px + x0, py + y0, pz + z0), (px + x1, py + y0, pz + z0),
                (px + x0, py + y1, pz + z0), (px + x1, py + y1, pz + z0),
                (px + x0, py + y0, pz + z1), (px + x1, py + y0, pz + z1),
                (px + x0, py + y1, pz + z1), (px + x1, py + y1, pz + z1),
            ]
            if any(pose):
                corners = [(lambda r: (px + r[0], py + r[1], pz + r[2]))(
                    rotate_xyz((c[0] - px, c[1] - py, c[2] - pz), *pose)) for c in corners]
            bcenter = (px + (x0 + x1) / 2, py + (y0 + y1) / 2, pz + (z0 + z1) / 2)
            for idx, normal, factor in FACE_DEFS:
                pts3 = [corners[i] for i in idx]
                nvec = {"x-": (-1.0, 0.0, 0.0), "x+": (1.0, 0.0, 0.0),
                        "y-": (0.0, -1.0, 0.0), "y+": (0.0, 1.0, 0.0),
                        "z-": (0.0, 0.0, -1.0), "z+": (0.0, 0.0, 1.0)}[normal]
                # cull faces pointing away from the camera (camera looks down +z at the model)
                _, _, nz = rotate(nvec, yaw, pitch)
                if nz > -0.02:
                    continue
                projected = [project(p, yaw, pitch, center, SCALE) for p in pts3]
                depth = rotate(bcenter, yaw, pitch)[2] + sum(
                    rotate(p, yaw, pitch)[2] for p in pts3) / 400.0
                faces.append((depth, projected, MATERIAL_COLORS[box["material"]], factor))
    faces.sort(key=lambda f: -f[0])
    for _, projected, color, factor in faces:
        fill_polygon(buf, [(p[0], p[1]) for p in projected], shade(color, factor))


def render(parts, out_path):
    buf = bytearray(b"\x0e\x0b\x1a" * (WIDTH * HEIGHT))
    for i, (yaw, pitch, _label) in enumerate(VIEWS):
        off = (i * PANEL_W, 0)
        render_view(buf, parts, off, yaw, pitch)
        # panel divider
        for y in range(HEIGHT):
            o = (y * WIDTH + (off[0] - 1)) * 3
            if o >= 0:
                buf[o:o + 3] = b"\x3a\x2c\x59"
    raw = b"".join(b"\x00" + bytes(buf[y * WIDTH * 3:(y + 1) * WIDTH * 3])
                   for y in range(HEIGHT))

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", WIDTH, HEIGHT, 8, 2, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "wb") as fh:
        fh.write(png)
    return out_path


def main():
    parts = parse_model()
    total = sum(len(p["boxes"]) for p in parts)
    print(f"parts: {len(parts)}, cuboids: {total}")
    for p in parts:
        print(f"  {p['name']:<18} pivot={p['pivot']} boxes={len(p['boxes'])}")
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(BASE, "docs", "img",
                                                             "nox_thal_model.png")
    print("wrote", render(parts, out))


if __name__ == "__main__":
    main()
