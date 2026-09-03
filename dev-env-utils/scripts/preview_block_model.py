#!/usr/bin/env python3
"""
Offline previewer for CSM block models -- renders a Forge JSON element model or a Wavefront OBJ
against its real texture, without launching Minecraft.

The point is to catch the mistakes that only show up once texture meets geometry: UV rects that
sample the transparent margin of a texture (so you see straight through the side of a device), a
square lens element wearing a round lens photo, or a 1-deep side face carrying a 5-unit-wide UV
smeared across it.

Face winding, UV origin and the `flip-v` OBJ convention all match how Minecraft/Forge bake models,
so what this draws is what the game draws. It is verified against known-good models -- a traffic
sign's north face reads its legend the right way round here.

Usage:
    python preview_block_model.py <model> <texture> [-o out.png] [--yaw D] [--pitch D]

    <model>    path under assets/csm/models/block/ (e.g. lifesafety/shared_models/foo.json),
               or an absolute path. Both .json and .obj are accepted.
    <texture>  texture ref, either "csm:blocks/lifesafety/foo" or "blocks/lifesafety/foo".
               For OBJ models the MTL supplies the textures, so pass "-" to use them; pass a
               real ref to override every material (the red/white variant case).

    --yaw 0 --pitch 0 looks straight at the block's north face, which is the front of every
    wall-mounted device in this mod. --yaw -38 --pitch 16 is the useful three-quarter view.

Requires Pillow and numpy.
"""

import argparse
import json
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

REPO_ROOT = layout.REPO_ROOT


# --------------------------------------------------------------------------- textures
def resolve_texture(ref):
    """csm:blocks/lifesafety/foo -> absolute path of the PNG, searched across every tree."""
    rel = "textures/" + ref.split(":", 1)[-1] + ".png"
    return layout.resolve_asset(rel) or rel


def load_texture(ref):
    return np.asarray(Image.open(resolve_texture(ref)).convert("RGBA"), dtype=np.float32) / 255.0


# --------------------------------------------------------------------------- JSON models
# Corner order per face, each corner picking min (0) or max (1) of the element on each axis, and
# the face's outward normal. The order pairs with UV_CORNERS below so uv[0],uv[1] is the corner of
# the texture rect that Minecraft puts at that first vertex.
FACE_DEF = {
    "north": ([(1, 1, 0), (0, 1, 0), (0, 0, 0), (1, 0, 0)], (0, 0, -1)),
    "south": ([(0, 1, 1), (1, 1, 1), (1, 0, 1), (0, 0, 1)], (0, 0, 1)),
    "west": ([(0, 1, 0), (0, 1, 1), (0, 0, 1), (0, 0, 0)], (-1, 0, 0)),
    "east": ([(1, 1, 1), (1, 1, 0), (1, 0, 0), (1, 0, 1)], (1, 0, 0)),
    "up": ([(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)], (0, 1, 0)),
    "down": ([(0, 0, 1), (1, 0, 1), (1, 0, 0), (0, 0, 0)], (0, -1, 0)),
}
FACE_UV_AXES = {"north": (0, 1), "south": (0, 1), "east": (2, 1),
                "west": (2, 1), "up": (0, 2), "down": (0, 2)}


def rotation_matrix(axis, radians):
    c, s = math.cos(radians), math.sin(radians)
    if axis == "x":
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == "y":
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def quads_from_json(model):
    """Flatten a JSON element model into (points, uvs, normal, texture_key) quads."""
    quads = []
    for element in model.get("elements", []):
        lo = np.array(element["from"], dtype=float)
        hi = np.array(element["to"], dtype=float)
        rotation = element.get("rotation")
        for face_name, face in element.get("faces", {}).items():
            picks, normal_def = FACE_DEF[face_name]
            points = np.array([[hi[i] if pick[i] else lo[i] for i in range(3)] for pick in picks],
                              dtype=float)
            normal = np.array(normal_def, dtype=float)
            if rotation and rotation.get("angle"):
                matrix = rotation_matrix(rotation["axis"], math.radians(rotation["angle"]))
                origin = np.array(rotation["origin"], dtype=float)
                points = (points - origin) @ matrix.T + origin
                normal = normal @ matrix.T
            u0, v0, u1, v1 = face.get("uv", [0, 0, 16, 16])
            uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
            steps = int(face.get("rotation", 0)) // 90 % 4
            if steps:
                uvs = uvs[-steps:] + uvs[:-steps]
            uvs = np.array([(u / 16.0, v / 16.0) for u, v in uvs])
            quads.append((points, uvs, normal, face.get("texture", "#0")))
    return quads


# --------------------------------------------------------------------------- OBJ models
def quads_from_obj(path, flip_v=True):
    """Read an OBJ (and its MTL) into the same quad form. Triangles get a repeated 4th corner.

    `flip_v` mirrors Forge's `custom: {"flip-v": true}` blockstate option: OBJ v runs bottom-up,
    Minecraft UV rows run top-down.
    """
    positions, texcoords, quads = [], [], []
    material_textures = {}
    current = "#none"
    base = os.path.dirname(path)
    for line in open(path):
        parts = line.split()
        if not parts:
            continue
        keyword = parts[0]
        if keyword == "mtllib":
            mtl_path = os.path.join(base, parts[1])
            if os.path.exists(mtl_path):
                name = None
                for mtl_line in open(mtl_path):
                    bits = mtl_line.split()
                    if not bits:
                        continue
                    if bits[0] == "newmtl":
                        name = bits[1]
                    elif bits[0] == "map_Kd" and name:
                        material_textures["#" + name] = bits[1]
        elif keyword == "v":
            positions.append([float(x) * 16.0 for x in parts[1:4]])
        elif keyword == "vt":
            texcoords.append([float(x) for x in parts[1:3]])
        elif keyword == "usemtl":
            current = "#" + parts[1]
        elif keyword == "f":
            corners = []
            for token in parts[1:]:
                bits = token.split("/")
                uv_index = int(bits[1]) if len(bits) > 1 and bits[1] else None
                corners.append((int(bits[0]), uv_index))
            for k in range(1, len(corners) - 1):
                triangle = [corners[0], corners[k], corners[k + 1]]
                points = np.array([positions[i - 1] for i, _ in triangle], dtype=float)
                uvs = []
                for _, uv_index in triangle:
                    if uv_index is None:
                        uvs.append((0.0, 0.0))
                        continue
                    u, v = texcoords[uv_index - 1]
                    uvs.append((u, 1.0 - v if flip_v else v))
                normal = np.cross(points[1] - points[0], points[2] - points[0])
                length = np.linalg.norm(normal)
                normal = normal / length if length else np.array([0.0, 0.0, -1.0])
                quads.append((np.vstack([points, points[2]]),
                              np.vstack([np.array(uvs), np.array(uvs)[2]]),
                              normal, current))
    return quads, material_textures


# --------------------------------------------------------------------------- raster
def render(quads, textures, size=512, yaw=-38.0, pitch=16.0, scale=1.35,
           background=(0.13, 0.13, 0.16)):
    """Orthographic, back-face culled, z-buffered. Affine UV interpolation is exact here because
    the projection is orthographic."""
    matrix = rotation_matrix("x", math.radians(pitch)) @ rotation_matrix("y", math.radians(yaw))
    image = np.zeros((size, size, 3), dtype=np.float32)
    image[:] = background
    depth = np.full((size, size), 1e9, dtype=np.float32)
    span = 20.0 / scale
    centre = np.array([8.0, 8.0, 8.0])
    light = np.array([0.35, 0.75, -0.55])
    light /= np.linalg.norm(light)

    def project(points):
        view = (points - centre) @ matrix.T
        # The camera sits at -Z looking north, so world +X is screen-left.
        return (0.5 - view[:, 0] / span) * size, (0.5 - view[:, 1] / span) * size, view[:, 2]

    for points, uvs, normal, texture_key in quads:
        if (normal @ matrix.T)[2] > -0.001:
            continue
        texture = textures.get(texture_key, textures.get("*"))
        if texture is None:
            continue
        height, width = texture.shape[0], texture.shape[1]
        unit = normal / max(1e-9, np.linalg.norm(normal))
        shade = 0.42 + 0.58 * max(0.0, float(unit @ light))
        sx, sy, sz = project(points)
        for a, b, c in ((0, 1, 2), (0, 2, 3)):
            x = np.array([sx[a], sx[b], sx[c]])
            y = np.array([sy[a], sy[b], sy[c]])
            z = np.array([sz[a], sz[b], sz[c]])
            u = np.array([uvs[a][0], uvs[b][0], uvs[c][0]])
            v = np.array([uvs[a][1], uvs[b][1], uvs[c][1]])
            x0, x1 = int(max(0, math.floor(x.min()))), int(min(size - 1, math.ceil(x.max())))
            y0, y1 = int(max(0, math.floor(y.min()))), int(min(size - 1, math.ceil(y.max())))
            if x1 < x0 or y1 < y0:
                continue
            det = (y[1] - y[2]) * (x[0] - x[2]) + (x[2] - x[1]) * (y[0] - y[2])
            if abs(det) < 1e-9:
                continue
            rows, cols = np.mgrid[y0:y1 + 1, x0:x1 + 1]
            px, py = cols + 0.5, rows + 0.5
            l0 = ((y[1] - y[2]) * (px - x[2]) + (x[2] - x[1]) * (py - y[2])) / det
            l1 = ((y[2] - y[0]) * (px - x[2]) + (x[0] - x[2]) * (py - y[2])) / det
            l2 = 1.0 - l0 - l1
            inside = (l0 >= -1e-6) & (l1 >= -1e-6) & (l2 >= -1e-6)
            if not inside.any():
                continue
            zz = l0 * z[0] + l1 * z[1] + l2 * z[2]
            uu = l0 * u[0] + l1 * u[1] + l2 * u[2]
            vv = l0 * v[0] + l1 * v[1] + l2 * v[2]
            texel = texture[np.clip((vv * height).astype(int), 0, height - 1),
                            np.clip((uu * width).astype(int), 0, width - 1)]
            inside &= texel[..., 3] > 0.35
            inside &= zz < depth[y0:y1 + 1, x0:x1 + 1]
            if not inside.any():
                continue
            image[y0:y1 + 1, x0:x1 + 1][inside] = texel[..., :3][inside] * shade
            depth[y0:y1 + 1, x0:x1 + 1][inside] = zz[inside]
    return Image.fromarray((np.clip(image, 0, 1) * 255).astype(np.uint8))


def preview(model_path, texture_ref=None, **kwargs):
    """Render a model by path (repo-relative under models/block/, or absolute)."""
    if not os.path.isabs(model_path):
        rel = "models/block/" + model_path
        model_path = layout.resolve_asset(rel) or rel
    if model_path.endswith(".obj"):
        quads, material_textures = quads_from_obj(model_path)
        if texture_ref:
            textures = {"*": load_texture(texture_ref)}
        else:
            textures = {key: load_texture(ref) for key, ref in material_textures.items()}
    else:
        quads = quads_from_json(json.load(open(model_path)))
        texture = load_texture(texture_ref)
        textures = {"*": texture, "#0": texture, "#all": texture, "#particle": texture}
    return render(quads, textures, **kwargs)


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("model")
    parser.add_argument("texture", nargs="?", default="-")
    parser.add_argument("-o", "--out", default="_preview.png")
    parser.add_argument("--yaw", type=float, default=-38.0)
    parser.add_argument("--pitch", type=float, default=16.0)
    parser.add_argument("--size", type=int, default=512)
    parser.add_argument("--scale", type=float, default=1.35)
    args = parser.parse_args()

    texture_ref = None if args.texture == "-" else args.texture
    image = preview(args.model, texture_ref, size=args.size, yaw=args.yaw,
                    pitch=args.pitch, scale=args.scale)
    image.save(args.out)
    print("wrote", args.out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
