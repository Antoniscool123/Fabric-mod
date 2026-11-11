#!/usr/bin/env python3
"""Render the GeckoLib model (assets/aetherius/geo/entity/nox_thal.geo.json) to a PNG.

The model is data, so it can be rasterised offline: this is the only way to look at the dragon
without launching Minecraft. Three views (side, three-quarter, top) in one image.

Bedrock geometry is Y-up with the ground at y = 0, so the rasteriser converts to Minecraft's
Y-down screen space before projecting. Bones are transformed hierarchically, and an animation's
first keyframe can be applied with `--animation <name>` to preview a pose.

Usage:
    python3 tools/preview_model.py [out.png] [--animation idle]
"""
import json
import math
import os
import struct
import sys
import zlib

BASE = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
GEO = os.path.join(BASE, "src", "main", "resources", "assets", "aetherius", "geo", "entity",
                   "nox_thal.geo.json")
ANIM = os.path.join(BASE, "src", "main", "resources", "assets", "aetherius", "animations",
                    "entity", "nox_thal.animation.json")

PANEL_W, PANEL_H = 560, 620
WIDTH, HEIGHT = PANEL_W * 3, PANEL_H
SCALE = 4.6
VIEWS = [(96.0, 6.0), (140.0, 18.0), (96.0, 86.0)]

MATERIAL_COLORS = {
    # uv regions: scales (0,0) · membrane (0,96) · bone (100,0)
    "scale": (78, 50, 122),
    "membrane": (120, 84, 170),
    "bone": (214, 206, 232),
}


def material_for_uv(u, v):
    if v >= 96:
        return "membrane"
    if u >= 100:
        return "bone"
    return "scale"


def load_geo():
    data = json.load(open(GEO, encoding="utf-8"))
    geometry = data["minecraft:geometry"][0]
    bones = []
    for bone in geometry["bones"]:
        cubes = []
        for cube in bone.get("cubes", []):
            ox, oy, oz = cube["origin"]
            w, h, d = cube["size"]
            u, v = cube.get("uv", [0, 0])[:2]
            cubes.append({
                "origin": (ox, oy, oz),
                "size": (w, h, d),
                "material": material_for_uv(u, v),
            })
        bones.append({
            "name": bone["name"],
            "parent": bone.get("parent"),
            "pivot": tuple(bone.get("pivot", (0, 0, 0))),
            "rotation": tuple(bone.get("rotation", (0, 0, 0))),
            "cubes": cubes,
        })
    return geometry["description"], bones


def load_animation_pose(name, at=None):
    """Return {bone: (rx, ry, rz)} sampled from an animation (linearly interpolated)."""
    if not name:
        return {}
    try:
        data = json.load(open(ANIM, encoding="utf-8"))
    except OSError:
        return {}
    animation = data.get("animations", {}).get(name)
    if not animation:
        return {}
    pose = {}
    for bone, channels in animation.get("bones", {}).items():
        rotation = channels.get("rotation")
        if not isinstance(rotation, dict) or not rotation:
            continue
        frames = sorted(((float(k), v) for k, v in rotation.items() if len(v) == 3))
        if at is None:
            pose[bone] = tuple(float(v) for v in frames[0][1])
            continue
        if at <= frames[0][0]:
            pose[bone] = tuple(float(v) for v in frames[0][1])
            continue
        for i in range(len(frames) - 1):
            t0, v0 = frames[i]
            t1, v1 = frames[i + 1]
            if t0 <= at <= t1:
                f = 0.0 if t1 == t0 else (at - t0) / (t1 - t0)
                pose[bone] = tuple(v0[k] + (v1[k] - v0[k]) * f for k in range(3))
                break
        else:
            pose[bone] = tuple(float(v) for v in frames[-1][1])
    return pose


def rot_matrix(rx, ry, rz):
    rx, ry, rz = (math.radians(v) for v in (rx, ry, rz))
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    # Z * Y * X applied to a column vector
    return (
        (cy * cz, cz * sx * sy - cx * sz, cx * cz * sy + sx * sz),
        (cy * sz, cx * cz + sx * sy * sz, -cz * sx + cx * sy * sz),
        (-sy, cy * sx, cx * cy),
    )


def matmul(a, b):
    return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3))
                 for i in range(3))


def apply(matrix, vec):
    return tuple(sum(matrix[i][k] * vec[k] for k in range(3)) for i in range(3))


def bone_transforms(bones, pose):
    """Absolute (matrix, translation) per bone, honouring parent pivots."""
    by_name = {b["name"]: b for b in bones}
    cache = {}

    def resolve(bone):
        if bone["name"] in cache:
            return cache[bone["name"]]
        rx, ry, rz = pose.get(bone["name"], bone["rotation"])
        rotation = rot_matrix(rx, ry, rz)
        pivot = bone["pivot"]
        if bone["parent"] and bone["parent"] in by_name:
            parent_rot, parent_offset = resolve(by_name[bone["parent"]])
            rotation = matmul(parent_rot, rotation)
            offset = apply(parent_rot, tuple(pivot[i] - by_name[bone["parent"]]["pivot"][i]
                                             for i in range(3)))
            offset = tuple(parent_offset[i] + offset[i] for i in range(3))
            pivot = by_name[bone["parent"]]["pivot"]
        else:
            offset = pivot
        cache[bone["name"]] = (rotation, offset)
        return cache[bone["name"]]

    return {bone["name"]: resolve(bone) for bone in bones}, by_name


def cube_corners(bone, cube, transforms):
    rotation, offset = transforms[bone["name"]]
    (ox, oy, oz), (w, h, d) = cube["origin"], cube["size"]
    corners = []
    for dx in (0, w):
        for dy in (0, h):
            for dz in (0, d):
                point = (ox + dx, oy + dy, oz + dz)
                rel = tuple(point[i] - bone["pivot"][i] for i in range(3))
                rotated = apply(rotation, rel)
                world = tuple(rotated[i] + bone["pivot"][i] + offset[i] - bone["pivot"][i]
                              for i in range(3))
                corners.append(world)
    # order: 0(0,0,0) 1(0,0,d) 2(0,h,0) 3(0,h,d) 4(w,0,0) 5(w,0,d) 6(w,h,0) 7(w,h,d)
    return [corners[0], corners[1], corners[2], corners[3],
            corners[4], corners[5], corners[6], corners[7]]


def to_screen(point, yaw, pitch, center, zoom):
    """Bedrock space (Y up) -> camera -> screen (Y down)."""
    x, y, z = point
    y = -y                                    # flip to Minecraft's Y-down model space
    cy, sy = math.cos(yaw), math.sin(yaw)
    x, z = x * cy - z * sy, x * sy + z * cy
    cp, sp = math.cos(pitch), math.sin(pitch)
    y, z = y * cp - z * sp, y * sp + z * cp
    return (center[0] + x * zoom, center[1] + y * zoom, z)


def fill_polygon(buf, points, color):
    ys = [p[1] for p in points]
    y0, y1 = max(0, int(min(ys))), min(HEIGHT - 1, int(max(ys)) + 1)
    for y in range(y0, y1 + 1):
        xs = []
        for i in range(len(points)):
            ax, ay = points[i][0], points[i][1]
            bx, by = points[(i + 1) % len(points)][0], points[(i + 1) % len(points)][1]
            if (ay <= y < by) or (by <= y < ay):
                xs.append(ax + (bx - ax) * (y - ay) / (by - ay))
        xs.sort()
        for i in range(0, len(xs) - 1, 2):
            for x in range(max(0, int(math.ceil(xs[i]))),
                           min(WIDTH - 1, int(math.floor(xs[i + 1]))) + 1):
                o = (y * WIDTH + x) * 3
                buf[o:o + 3] = bytes(color)


def shade(color, factor):
    return tuple(min(255, max(0, int(c * factor))) for c in color)


FACE_DEFS = [
    ((0, 2, 3, 1), (0, -1, 0), 1.18),
    ((4, 5, 7, 6), (0, 1, 0), 0.72),
    ((0, 1, 5, 4), (0, 0, -1), 1.05),
    ((2, 6, 7, 3), (0, 0, 1), 0.82),
    ((0, 4, 6, 2), (-1, 0, 0), 0.94),
    ((1, 3, 7, 5), (1, 0, 0), 0.94),
]


def render_view(buf, bones, pose, offset, yaw_deg, pitch_deg):
    yaw, pitch = math.radians(yaw_deg), math.radians(pitch_deg)
    center = (offset[0] + PANEL_W * 0.5, offset[1] + PANEL_H * 0.56)
    transforms, _ = bone_transforms(bones, pose)
    faces = []
    for bone in bones:
        for cube in bone["cubes"]:
            corners = cube_corners(bone, cube, transforms)
            for indices, normal, factor in FACE_DEFS:
                pts = [corners[i] for i in indices]
                projected = [to_screen(p, yaw, pitch, center, SCALE) for p in pts]
                # back-face culling in screen space (counter-clockwise = facing the camera)
                area = 0.0
                for i in range(len(projected)):
                    x0, y0 = projected[i][0], projected[i][1]
                    x1, y1 = projected[(i + 1) % len(projected)][0], projected[(i + 1) % len(projected)][1]
                    area += x0 * y1 - x1 * y0
                if area >= 0:
                    continue
                depth = sum(p[2] for p in pts) / 4.0
                faces.append((depth, projected, MATERIAL_COLORS[cube["material"]], factor))
    faces.sort(key=lambda f: -f[0])
    for _, projected, color, factor in faces:
        fill_polygon(buf, [(p[0], p[1]) for p in projected], shade(color, factor))


def main():
    args = [a for a in sys.argv[1:]]
    animation = None
    at = None
    if "--animation" in args:
        index = args.index("--animation")
        animation = args[index + 1]
        del args[index:index + 2]
    if "--at" in args:
        index = args.index("--at")
        at = float(args[index + 1])
        del args[index:index + 2]
    out = args[0] if args else os.path.join(BASE, "docs", "img", "nox_thal_model.png")

    description, bones = load_geo()
    pose = load_animation_pose(animation, at)
    cubes = sum(len(b["cubes"]) for b in bones)
    ground = min(c["origin"][1] for b in bones for c in b["cubes"] or [{"origin": (0, 0, 0)}])
    top = max(c["origin"][1] + c["size"][1] for b in bones for c in b["cubes"] or [{"origin": (0, 0, 0), "size": (0, 0, 0)}])
    print(f"{description['identifier']}: {len(bones)} bones, {cubes} cubes, "
          f"height {top / 16:.2f} blocks, lowest y {ground:g}")
    if animation:
        print(f"pose: {animation}" + (f" @ {at}s" if at is not None else " @ 0s")
              + f" ({len(pose)} animated bones)")

    buf = bytearray(b"\x0e\x0b\x1a" * (WIDTH * HEIGHT))
    for i, (yaw, pitch) in enumerate(VIEWS):
        off = (i * PANEL_W, 0)
        render_view(buf, bones, pose, off, yaw, pitch)
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
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "wb") as fh:
        fh.write(png)
    print("wrote", out)


if __name__ == "__main__":
    main()
