#!/usr/bin/env python3
"""
gen_pole_bases.py -- the bolted bases of the painted steel traffic poles, and those poles'
blockstates.

THE BASES (GitHub #348)
-----------------------
A thin (8 across) or thick (12 across) painted traffic pole stood on the ground as a bare tube.
Each now draws a base at its foot, in a style its owner picks with the Street Light
Configuration Tool:

  * ``square``    a square plate with a domed nut cover at each corner (the default)
  * ``squircle``  the same plate with its corners rounded off, a little thicker
  * ``slim``      a thin square plate on exposed anchor bolts: stud, washer and hex nut
  * ``pedestal``  a tall tapered square base, the shape the pedestal pole stands on
  * ``none``      nothing

A base is authored standing (Y up, the ground at y = 0, the pole's axis through the block
centre) and written in the model frame of a pole facing UP, whose foot is at model south. The
poles are always drawn facing up (``AbstractBlockTrafficPoleFooted``), so there is one model per
style and width, not two. They are OBJ so a nut cover can be a dome and a base can taper; an OBJ
may also leave its block, which the wider bases do by a pixel or two.

THE BLOCKSTATES
---------------
The ten painted pole blockstates were hand kept. They are written here now, because the foot is
one property, ``foot``, that carries the base *and* the reach-down extension through a guardrail
that ``extenddown`` used to: the two are never drawn together, so one property of six values
replaces what would have been two of five and two. Every value multiplies the variants a Forge
blockstate bakes (6 facings x 16 mounts x 6 feet = 576 a pole, up from 192).

Each pole keeps the model, mount stub and texture it had; ``POLES`` restates them.

OUTPUT
------
  models/block/trafficaccessories/shared_models/polebase_<width>_<style>.obj
  models/block/trafficaccessories/shared_models/polebase.mtl
  blockstates/<pole>.json

Run:  python dev-env-utils/scripts/gen_pole_bases.py [--check]
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402
import gen_pedestal_pole as ped  # noqa: E402  (the mesh, the lathe and the standing frame)

OWNER = layout.owner_of_folder("trafficaccessories")
MODEL_REL = "models/block/trafficaccessories/shared_models"
MODEL_PREFIX = "csm:trafficaccessories/shared_models/"
MTL_NAME = "polebase.mtl"
MATERIAL = "body"
TEX = "csm:blocks/trafficsignals/shared_textures/"
# The finishes that do not live with the rest. Naming the wrong folder is a missing texture, the
# whole pole in magenta and black, and nothing offline says so.
TEXTURE_FOLDER = {"metal_mattetan": "csm:blocks/trafficaccessories/shared_textures/"}

AXIS = 8.0
INSET = 0.01          # keeps the underside off the block boundary plane

# width id -> (pole radius, scale of the fittings)
WIDTHS = {
    "thin": (4.0, 0.8),
    "large": (6.0, 1.0),
}

STYLES = ["square", "squircle", "slim", "pedestal"]
FEET = ["none", "extend"] + STYLES

# (registry name, width id, pole model, mount stub model or None, texture)
POLES = [
    ("trafficpolevertical", "large", "large", "csm:lighting/shared_models/large_mount",
     "metal_silver"),
    ("trafficpoleverticalblack", "large", "large", "csm:lighting/shared_models/large_mount",
     "metal_black"),
    ("trafficpoleverticaltan", "large", "large", "csm:lighting/shared_models/large_mount",
     "metal_mattetan"),
    ("trafficpoleverticalwhite", "large", "large", "csm:lighting/shared_models/large_mount",
     "metal_mattewhite"),
    ("trafficpoleverticalunpainted", "large", "large", "csm:lighting/shared_models/large_mount",
     "metal_white"),
    ("trafficpolehorizontal", "thin", "small", MODEL_PREFIX + "small_mount", "metal_silver"),
    ("trafficpolehorizontalblack", "thin", "small", MODEL_PREFIX + "small_mount", "metal_black"),
    ("trafficpolehorizontaltan", "thin", "small", MODEL_PREFIX + "small_mount", "metal_mattetan"),
    ("trafficpolehorizontalwhite", "thin", "small", MODEL_PREFIX + "small_mount",
     "metal_mattewhite"),
    ("trafficpolehorzunpainted", "thin", "small", MODEL_PREFIX + "small_mount", "metal_white"),
]


# ------------------------------------------------------------------------------------------
# Geometry (standing frame: Y up, ground at y = 0, axis at x = z = 8)
# ------------------------------------------------------------------------------------------
def rounded_square(hw, cr, seg=5):
    """The outline of a square of half-width ``hw`` with corners of radius ``cr``, as (x, z)."""
    if cr < 1e-6:
        return [(AXIS + hw, AXIS + hw), (AXIS - hw, AXIS + hw), (AXIS - hw, AXIS - hw),
                (AXIS + hw, AXIS - hw)]
    pts = []
    c = hw - cr
    for sx, sz, a0 in ((1, 1, 0.0), (-1, 1, 90.0), (-1, -1, 180.0), (1, -1, 270.0)):
        for k in range(seg + 1):
            a = math.radians(a0 + 90.0 * k / seg)
            pts.append((AXIS + sx * c + cr * math.cos(a), AXIS + sz * c + cr * math.sin(a)))
    return pts


def slab(mesh, outline, y0, y1):
    """A plate: the outline's walls from y0 to y1 and its top. No underside; it lies on the
    ground."""
    n = len(outline)
    for i in range(n):
        (ax, az), (bx, bz) = outline[i], outline[(i + 1) % n]
        if math.hypot(bx - ax, bz - az) < 1e-6:
            continue
        normal = (bz - az, 0.0, -(bx - ax))
        mid = ((ax + bx) / 2.0 - AXIS, (az + bz) / 2.0 - AXIS)
        if normal[0] * mid[0] + normal[2] * mid[1] < 0:
            normal = (-normal[0], 0.0, -normal[2])
        u0, u1 = i / n, (i + 1) / n
        mesh.quad_out([(ax, y0, az), (bx, y0, bz), (bx, y1, bz), (ax, y1, az)], normal,
                      [ped.uv(u0, y0 / 16.0), ped.uv(u1, y0 / 16.0), ped.uv(u1, y1 / 16.0),
                       ped.uv(u0, y1 / 16.0)])
    up = (0.0, 1.0, 0.0)
    centre = ((AXIS, y1, AXIS), ped.uv(0.5, 0.5), up)

    def top_uv(p):
        return ped.uv(p[0] / 16.0, p[1] / 16.0)

    for i in range(n):
        a, b = outline[i], outline[(i + 1) % n]
        if math.hypot(b[0] - a[0], b[1] - a[1]) < 1e-6:
            continue
        mesh.tri([centre, ((a[0], y1, a[1]), top_uv(a), up), ((b[0], y1, b[1]), top_uv(b), up)])


def weld_collar(mesh, r, y, k):
    """The fillet where the tube meets the plate."""
    ped.lathe(mesh, [(r + 0.7 * k, y), (r + 0.7 * k, y + 0.35 * k), (r + 0.05, y + 1.0 * k)])


def nut_cover(mesh, x, z, y, k):
    """A domed cover over an anchor bolt."""
    r = 1.45 * k
    ped.lathe(mesh, [(r, y), (r, y + 0.55 * k), (0.86 * r, y + 1.15 * k), (0.5 * r, y + 1.6 * k),
                     (0.0, y + 1.75 * k)], sides=8, cx=x, cz=z)


def anchor_bolt(mesh, x, z, y, k):
    """An exposed anchor: washer, hex nut and the stud standing through it."""
    ped.lathe(mesh, [(1.0 * k, y), (1.0 * k, y + 0.14), (0.78 * k, y + 0.14),
                     (0.78 * k, y + 0.75 * k), (0.34 * k, y + 0.75 * k), (0.34 * k, y + 1.55 * k),
                     (0.0, y + 1.55 * k)], sides=6, cx=x, cz=z)


def corners(d):
    return [(AXIS + sx * d, AXIS + sz * d) for sx in (1, -1) for sz in (1, -1)]


def build(style, r, k):
    """One base, as a mesh in the model frame of a pole facing up."""
    mesh = ped.Mesh(ped.to_model_south, ped.to_model_south_n)
    if style == "square":
        hw, t = r + 2.7 * k, 1.0 * k
        slab(mesh, rounded_square(hw, 0.0), INSET, t)
        weld_collar(mesh, r, t, k)
        for x, z in corners(hw - 1.65 * k):
            nut_cover(mesh, x, z, t, k)
    elif style == "squircle":
        hw, t = r + 2.9 * k, 1.5 * k
        slab(mesh, rounded_square(hw, 0.55 * hw), INSET, t)
        weld_collar(mesh, r, t, k)
        # on the diagonal, as far out as the rounded corner leaves room for the cover
        for x, z in corners(hw - 0.55 * hw * (1.0 - math.sqrt(0.5)) - 1.9 * k):
            nut_cover(mesh, x, z, t, k)
    elif style == "slim":
        hw, t = r + 2.2 * k, 0.45
        slab(mesh, rounded_square(hw, 0.7 * k), INSET, t)
        weld_collar(mesh, r, t, 0.6 * k)
        for x, z in corners(hw - 1.25 * k):
            anchor_bolt(mesh, x, z, t, k)
    elif style == "pedestal":
        hb, ht, h = r + 3.0 * k, r + 1.0 * k, 10.0 + 2.0 * k
        ped.square_frustum(mesh, [(hb, INSET), (hb, 0.9 * k), (hb - 0.35 * k, 0.9 * k), (ht, h)],
                           bottom_face=False)
        ped.lathe(mesh, [(r + 0.75 * k, h), (r + 0.75 * k, h + 0.8 * k), (r + 0.05, h + 1.5 * k)])
    else:
        raise AssertionError(style)
    return mesh


def obj_text(mesh, name, header):
    lines = ["# Generated by dev-env-utils/scripts/gen_pole_bases.py -- do not edit",
             "# " + header, "mtllib %s" % MTL_NAME, "o %s" % name]
    for p in mesh.v:
        lines.append("v %.6f %.6f %.6f" % (p[0] / 16.0, p[1] / 16.0, p[2] / 16.0))
    for t in mesh.vt:
        assert 0.0 <= t[0] <= 1.0 and 0.0 <= t[1] <= 1.0, (name, t)
        lines.append("vt %.6f %.6f" % t)
    for n in mesh.vn:
        lines.append("vn %.6f %.6f %.6f" % n)
    lines.append("usemtl %s" % MATERIAL)
    for tri in mesh.f:
        lines.append("f " + " ".join("%d/%d/%d" % i for i in tri))
    return "\n".join(lines) + "\n"


def mtl_text():
    return ("# Generated by dev-env-utils/scripts/gen_pole_bases.py -- do not edit\n"
            "# One material; every pole's blockstate retextures #body with its own finish.\n"
            "newmtl %s\nmap_Kd %smetal_silver\n" % (MATERIAL, TEX))


# ------------------------------------------------------------------------------------------
# Blockstates
# ------------------------------------------------------------------------------------------
def base_name(width, style):
    return "polebase_%s_%s" % (width, style)


def blockstate(width, model, mount, texture):
    tex = TEXTURE_FOLDER.get(texture, TEX) + texture
    textures = {"all": tex, "particle": tex, "0": tex}
    model_ref = MODEL_PREFIX + model

    def mount_variant(key, rotation):
        if mount is None:
            return {"false": {}, "true": {}}
        return {"false": {}, "true": {"submodel": {key: {
            "uvlock": True,
            "model": mount,
            "textures": dict(textures),
            "transform": {"rotation": rotation},
        }}}}

    foot = {
        "none": {},
        # One more block of tube below, through the guardrail the pole stands on. In the model
        # frame of a pole facing up, down is +z.
        "extend": {"submodel": {"extension": {
            "model": model_ref,
            "textures": dict(textures),
            "transform": {"translation": [0.0, 0.0, 1.0]},
        }}},
    }
    for style in STYLES:
        foot[style] = {"submodel": {"foot": {
            "model": MODEL_PREFIX + base_name(width, style) + ".obj",
            "custom": {"flip-v": True},
            "textures": {"#" + MATERIAL: tex},
        }}}
    return {
        "forge_marker": 1,
        "defaults": {"model": model_ref, "textures": dict(textures)},
        "variants": {
            "inventory": [{}],
            "facing": {
                "north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
                "up": {"x": 270}, "down": {"x": 90},
            },
            "foot": foot,
            "mounteast": mount_variant("mounte", [{"x": 0}, {"y": 90}, {"z": 0}]),
            "mountwest": mount_variant("mountw", [{"x": 0}, {"y": 270}, {"z": 0}]),
            "mountup": mount_variant("mountu", [{"x": 270}, {"y": 0}, {"z": 0}]),
            "mountdown": mount_variant("mountd", [{"x": 90}, {"y": 0}, {"z": 0}]),
            "normal": [{}],
        },
    }


# ------------------------------------------------------------------------------------------
# Output
# ------------------------------------------------------------------------------------------
def outputs():
    """Every file the script owns: tree path -> text."""
    files = {layout.asset_for_write(OWNER, MODEL_REL + "/" + MTL_NAME): mtl_text()}
    for width, (r, k) in WIDTHS.items():
        for style in STYLES:
            name = base_name(width, style)
            files[layout.asset_for_write(OWNER, MODEL_REL + "/" + name + ".obj")] = obj_text(
                build(style, r, k), name,
                "the %s base of the %s pole (radius %.0f), in the frame of a pole facing up"
                % (style, width, r))
    for registry, width, model, mount, texture in POLES:
        files[layout.asset_for_write(OWNER, "blockstates/%s.json" % registry)] = json.dumps(
            blockstate(width, model, mount, texture), indent=2) + "\n"
    return files


def main(argv):
    files = outputs()
    check = "--check" in argv
    drift = []
    for path, text in files.items():
        current = None
        if os.path.exists(path):
            with open(path, encoding="utf-8", newline="") as fh:
                current = fh.read()
        if check:
            if current is None or current.replace("\r\n", "\n") != text:
                drift.append(os.path.basename(path))
            continue
        # keep the line endings the tree has the file in
        if current is not None and "\r\n" in current:
            text = text.replace("\n", "\r\n")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="") as fh:
            fh.write(text)
    if check:
        if drift:
            print("gen_pole_bases: %d file(s) drifted:\n  %s" % (len(drift), "\n  ".join(drift)))
            return 1
        print("gen_pole_bases: up to date")
        return 0
    print("wrote %d files" % len(files))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
