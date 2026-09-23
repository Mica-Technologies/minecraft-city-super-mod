#!/usr/bin/env python3
"""
Generates the traffic enforcement cameras: red light cameras, speed cameras and the external
flash unit that fires for either, every one of them hardware that mounts on the mod's own traffic
poles rather than carrying a pole of its own.

WHAT IS MADE
------------
Five devices, each in a white and a black finish (ten blocks):

  * red light camera, pole top   -- the large housing with the angled sun-hood front that sits on
                                    top of the pole on a slip-fitter collar;
  * red light camera, side arm   -- the smaller chamfered camera on a tilt head and a short arm
                                    clamped to the side of the pole;
  * flash unit, side arm         -- the strobe box with a frosted Fresnel front;
  * speed camera, pole top       -- the tall cabinet with a camera window, radar panel and IR
                                    illuminator;
  * speed camera, side arm       -- the compact radar camera on the same arm as the others.

HOW EACH ONE MEETS THE POLE
---------------------------
A pole-top device sits in the block above the pole. Its collar sleeves down a fifth of a block
into the pole's own cell and is wider than the widest pole (12 across), so it reads as a slip
fitter on any of the three pole styles and needs no per-pole model. The Java block is
``ICsmTrafficPoleIgnored`` so the pole does not also grow a mount stub up into it.

A side-arm device hangs beside the pole, not in front of it: its arm runs sideways along the
kerb into the pole on its left or right, and the device itself turns to keep facing the road, the
way the side cameras on a real red light pole are hung. The arm has to end at the pole's skin,
which is at a different depth for each pole width, so every side-arm model is written for both
hands and all three widths -- ``_right`` / ``_left`` times large, ``_thin`` and ``_pedestal`` --
and the blockstate names every combination, since the ``arm`` and ``polefit`` actual-state
properties both choose the model. The Java block is ``ICsmPoleFitted`` and resolves both from
which neighbour is a pole. Only the arm, the clamp plate and the band straps differ; the camera
itself is identical, and the left hand is the right one mirrored.

Coordinates follow gen_miovision_obj.py: 1 unit = 1 block, X and Z centred on the block, Y up
from the block floor, the device looks toward -Z (the unrotated "north" variant), and every face
is wound outward. One MTL serves every model; the finish is chosen per block by the blockstate's
``#housing`` / ``#trim`` / ``#bracket`` overrides.

Run:  python dev-env-utils/scripts/gen_enforcement_cameras.py [--check] [--fragments]
"""

import io
import json
import math
import os
import random
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402
from gen_miovision_obj import Mesh, _nrm  # noqa: E402
from PIL import Image  # noqa: E402

OWNER = layout.owner_of_folder("trafficaccessories")
MODEL_REL = "models/block/trafficaccessories/shared_models/enforcement"
TEX_REL = "textures/blocks/trafficaccessories/enforcement"
TEX_ID = "csm:blocks/trafficaccessories/enforcement"
MODEL_ID = "csm:trafficaccessories/shared_models/enforcement"
MTL_NAME = "enforcement_camera.mtl"

SILVER = "csm:blocks/trafficsignals/shared_textures/metal_silver"
BLACK_METAL = "csm:blocks/trafficsignals/shared_textures/metal_black"

# Finish -> the three retextured materials. Glass, the flash lens and the radar panel look the
# same whatever colour the housing is painted.
FINISHES = {
    "white": {"housing": TEX_ID + "/housing_white", "trim": TEX_ID + "/trim_black",
              "bracket": SILVER},
    "black": {"housing": TEX_ID + "/housing_black", "trim": TEX_ID + "/trim_charcoal",
              "bracket": BLACK_METAL},
}

# Pole fits: suffix and the pole's tube radius in sixteenths (see CsmPoleFit).
POLE_FITS = [("large", "", 6.0), ("thin", "_thin", 4.0), ("pedestal", "_pedestal", 3.0)]
POLE_CENTRE = 1.0       # the pole beside stands in the next cell, centred one block over
COLLAR_R = 0.42         # wider than the 12-across pole (0.375)
RING = 16


# ---- primitives ------------------------------------------------------------------------------
QUAD_UV = [(0, 0), (1, 0), (1, 1), (0, 1)]


def box(mesh, x0, x1, y0, y1, z0, z1, mat, front_mat=None):
    """An axis-aligned box. ``front_mat`` (if given) paints the -Z face."""
    c = ((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2)
    faces = [
        ([(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)], front_mat or mat),  # -Z
        ([(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], mat),  # +Z
        ([(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)], mat),  # -X
        ([(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)], mat),  # +X
        ([(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)], mat),  # -Y
        ([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], mat),  # +Y
    ]
    for verts, m in faces:
        mesh.quad_out(m, verts, QUAD_UV, c)


def _fan(mesh, pts, outward, mat):
    """Close a convex planar polygon with a fan wound so its normal points along ``outward``."""
    n = len(pts)
    cen = tuple(sum(p[k] for p in pts) / n for k in range(3))
    # Planar UVs over the polygon's bounds, so a textured cap (the octagonal glass) shows its
    # texture once and undistorted. Project onto the two axes the polygon's plane spans.
    axes = [k for k in range(3) if abs(outward[k]) < 0.5]
    lo = [min(p[k] for p in pts) for k in axes]
    span = [max(p[k] for p in pts) - lo[i] or 1.0 for i, k in enumerate(axes)]

    def uv(p):
        return tuple((p[k] - lo[i]) / span[i] for i, k in enumerate(axes))
    for j in range(n):
        a, b = pts[j], pts[(j + 1) % n]
        nrm = _nrm(cen, a, b)
        if sum(nrm[k] * outward[k] for k in range(3)) >= 0:
            mesh.tri(mat, cen, a, b, uv(cen), uv(a), uv(b))
        else:
            mesh.tri(mat, cen, b, a, uv(cen), uv(b), uv(a))


def prism_x(mesh, profile_zy, x0, x1, mat):
    """Extrude a convex (z, y) side profile across x0..x1: a housing with a shaped side view."""
    n = len(profile_zy)
    cz = sum(p[0] for p in profile_zy) / n
    cy = sum(p[1] for p in profile_zy) / n
    centre = ((x0 + x1) / 2, cy, cz)
    for j in range(n):
        (za, ya), (zb, yb) = profile_zy[j], profile_zy[(j + 1) % n]
        mesh.quad_out(mat, [(x0, ya, za), (x0, yb, zb), (x1, yb, zb), (x1, ya, za)], QUAD_UV,
                      centre)
    _fan(mesh, [(x0, y, z) for z, y in profile_zy], (-1, 0, 0), mat)
    _fan(mesh, [(x1, y, z) for z, y in profile_zy], (1, 0, 0), mat)


def prism_z(mesh, profile_xy, z0, z1, mat, front_mat=None, back_cap=True):
    """Extrude a convex (x, y) cross-section from z1 (back) to z0 (front, -Z)."""
    n = len(profile_xy)
    cx = sum(p[0] for p in profile_xy) / n
    cy = sum(p[1] for p in profile_xy) / n
    centre = (cx, cy, (z0 + z1) / 2)
    for j in range(n):
        (xa, ya), (xb, yb) = profile_xy[j], profile_xy[(j + 1) % n]
        mesh.quad_out(mat, [(xa, ya, z0), (xb, yb, z0), (xb, yb, z1), (xa, ya, z1)], QUAD_UV,
                      centre)
    _fan(mesh, [(x, y, z0) for x, y in profile_xy], (0, 0, -1), front_mat or mat)
    if back_cap:
        _fan(mesh, [(x, y, z1) for x, y in profile_xy], (0, 0, 1), mat)


def chamfered_rect(cx, cy, hw, hh, ch):
    """An octagon: a rectangle of half-size hw x hh with its corners cut back by ch."""
    return [(cx - hw + ch, cy - hh), (cx + hw - ch, cy - hh), (cx + hw, cy - hh + ch),
            (cx + hw, cy + hh - ch), (cx + hw - ch, cy + hh), (cx - hw + ch, cy + hh),
            (cx - hw, cy + hh - ch), (cx - hw, cy - hh + ch)]


def cylinder(mesh, cx, cz, r, y0, y1, mat, caps=True, ring=RING):
    """A vertical cylinder about (cx, cz)."""
    pts = lambda y: [(cx + r * math.cos(2 * math.pi * j / ring), y,
                      cz + r * math.sin(2 * math.pi * j / ring)) for j in range(ring)]
    lo, hi = pts(y0), pts(y1)
    centre = (cx, (y0 + y1) / 2, cz)
    for j in range(ring):
        j2 = (j + 1) % ring
        u0, u1 = j / ring, (j + 1) / ring
        mesh.quad_out(mat, [lo[j], lo[j2], hi[j2], hi[j]], [(u0, 0), (u1, 0), (u1, 1), (u0, 1)],
                      centre)
    if caps:
        _fan(mesh, lo, (0, -1, 0), mat)
        _fan(mesh, hi, (0, 1, 0), mat)


# ---- shared mounting hardware ------------------------------------------------------------------
def pole_top_mount(mesh, head_top):
    """Slip-fitter collar over the pole top, and the pan/tilt head the housing bolts to."""
    cylinder(mesh, 0.0, 0.0, COLLAR_R, -0.22, 0.12, "bracket")
    cylinder(mesh, 0.0, 0.0, 0.16, 0.12, head_top - 0.06, "bracket")
    box(mesh, -0.13, 0.13, head_top - 0.06, head_top, -0.13, 0.13, "bracket")


def side_arm_mount(mesh, pole_r16, strap_pad, arm_y0, arm_y1, head_x, head_top):
    """Arm from under the device sideways to the pole on its +X side, a clamp plate on the
    pole's skin and two band straps round the pole. The device keeps facing -Z, the way the
    road is, which is how these are hung: the arm runs along the kerb and the camera looks
    down the approach. ``pole_r16`` is the pole radius in sixteenths. The -X hand is this
    mirrored (see mirror_x). ``strap_pad`` is how far the band straps stand off the pole; the
    two hands use different pads so a device on each side of one pole never draws its straps
    in the same place as the other's, which would z-fight between two finishes."""
    r = pole_r16 / 16.0
    skin = POLE_CENTRE - r
    box(mesh, -0.06, skin + 0.01, arm_y0, arm_y1, -0.055, 0.055, "bracket")
    box(mesh, -head_x, head_x, arm_y1 - 0.02, head_top, -head_x, head_x, "bracket")
    plate_hw = min(0.13, r * 0.8)
    box(mesh, skin - 0.03, skin + 0.02, arm_y0 - 0.12, arm_y1 + 0.12, -plate_hw, plate_hw,
        "bracket")
    for yb in (arm_y0 - 0.09, arm_y1 + 0.05):
        cylinder(mesh, POLE_CENTRE, 0.0, r + strap_pad, yb, yb + 0.04, "bracket", caps=False)


def mirror_x(mesh):
    """Mirror a mesh through x = 0, reversing each face so it still winds outward."""
    mesh.v = [(-x, y, z) for (x, y, z) in mesh.v]
    mesh.vn = [(-x, y, z) for (x, y, z) in mesh.vn]
    mesh.faces = [(mat, list(reversed(idx))) for mat, idx in mesh.faces]


# ---- the devices -------------------------------------------------------------------------------
def red_light_top(mesh):
    pole_top_mount(mesh, 0.26)
    # Side profile (z, y): flat back, flat roof that overhangs the front as a sun hood, and a
    # front that slopes back toward the floor so the window looks down at the stop line.
    lip = (-0.58, 1.03)
    foot = (-0.40, 0.26)
    prism_x(mesh, [(0.42, 0.26), (0.42, 1.10), (-0.58, 1.10), lip, foot], -0.40, 0.40,
            "housing")
    # Black edge along the floor and the hood lip.
    box(mesh, -0.405, 0.405, 0.255, 0.31, -0.405, 0.425, "trim")
    box(mesh, -0.405, 0.405, 1.025, 1.105, -0.585, -0.51, "trim")
    # Window set in the sloped face, standing a hair proud of it.
    dz, dy = foot[0] - lip[0], foot[1] - lip[1]
    ln = math.hypot(dz, dy)
    nz, ny = -dy / ln, dz / ln          # outward normal in the z-y plane: forward and down
    if nz > 0:
        nz, ny = -nz, -ny
    off = 0.004

    def on_face(t, x):
        return (x, lip[1] + dy * t + ny * off, lip[0] + dz * t + nz * off)
    t0, t1, hw = 0.16, 0.62, 0.27
    win = [on_face(t0, -hw), on_face(t0, hw), on_face(t1, hw), on_face(t1, -hw)]
    mesh.quad_out("glass", win, QUAD_UV, (0.0, 0.68, 0.0))


def red_light_side(mesh, pole_r16, strap_pad):
    side_arm_mount(mesh, pole_r16, strap_pad, 0.29, 0.40, 0.07, 0.435)
    body = chamfered_rect(0.0, 0.62, 0.23, 0.19, 0.07)
    prism_z(mesh, body, -0.42, 0.22, "housing")
    ring = chamfered_rect(0.0, 0.62, 0.24, 0.20, 0.075)
    prism_z(mesh, ring, -0.47, -0.40, "trim", back_cap=False)
    glass = chamfered_rect(0.0, 0.62, 0.18, 0.14, 0.05)
    _fan(mesh, [(x, y, -0.474) for x, y in glass], (0, 0, -1), "glass")
    box(mesh, -0.25, 0.25, 0.822, 0.845, -0.58, 0.12, "housing")    # sun hood


def flash_side(mesh, pole_r16, strap_pad):
    side_arm_mount(mesh, pole_r16, strap_pad, 0.29, 0.40, 0.07, 0.405)
    box(mesh, -0.22, 0.22, 0.40, 0.86, -0.30, 0.18, "housing")
    box(mesh, -0.20, 0.20, 0.44, 0.82, -0.315, -0.29, "trim")
    box(mesh, -0.165, 0.165, 0.47, 0.79, -0.322, -0.31, "trim", front_mat="lens")
    box(mesh, -0.24, 0.24, 0.862, 0.885, -0.44, 0.18, "housing")     # sun hood


def speed_side(mesh, pole_r16, strap_pad):
    side_arm_mount(mesh, pole_r16, strap_pad, 0.29, 0.40, 0.07, 0.425)
    box(mesh, -0.25, 0.25, 0.42, 0.88, -0.36, 0.22, "housing")
    box(mesh, -0.23, 0.23, 0.45, 0.86, -0.375, -0.35, "trim")
    box(mesh, -0.18, 0.18, 0.67, 0.83, -0.382, -0.37, "trim", front_mat="glass")
    box(mesh, -0.18, 0.18, 0.48, 0.64, -0.382, -0.37, "trim", front_mat="radar")
    box(mesh, -0.27, 0.27, 0.882, 0.905, -0.52, 0.22, "housing")     # sun hood


def speed_top(mesh):
    pole_top_mount(mesh, 0.26)
    box(mesh, -0.34, 0.34, 0.26, 1.55, -0.34, 0.34, "housing")
    box(mesh, -0.30, 0.30, 0.36, 1.46, -0.355, -0.33, "trim")
    box(mesh, -0.24, 0.24, 1.06, 1.40, -0.362, -0.35, "trim", front_mat="glass")
    box(mesh, -0.24, 0.24, 0.64, 0.98, -0.362, -0.35, "trim", front_mat="radar")
    box(mesh, -0.24, 0.24, 0.42, 0.56, -0.362, -0.35, "trim", front_mat="lens")
    box(mesh, -0.37, 0.37, 1.552, 1.60, -0.50, 0.37, "housing")      # roof overhang


# (base model name, builder, is it pole-fitted)
MODELS = [
    ("red_light_camera_top", red_light_top, False),
    ("red_light_camera_side", red_light_side, True),
    ("flash_unit_side", flash_side, True),
    ("speed_camera_top", speed_top, False),
    ("speed_camera_side", speed_side, True),
]

# (registry stem, model, display name stem). The finish is appended to both.
BLOCKS = [
    ("enforcement_red_light_camera_top", "red_light_camera_top", "Red Light Camera (Pole Top"),
    ("enforcement_red_light_camera_side", "red_light_camera_side",
     "Red Light Camera (Side Arm"),
    ("enforcement_flash_unit_side", "flash_unit_side", "Enforcement Camera Flash Unit (Side Arm"),
    ("enforcement_speed_camera_top", "speed_camera_top", "Speed Camera (Pole Top"),
    ("enforcement_speed_camera_side", "speed_camera_side", "Speed Camera (Side Arm"),
]


# ---- textures ----------------------------------------------------------------------------------
def _noise_tex(seed, base, spread, size=32):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            d = rng.randint(-spread, spread)
            img.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (255,))
    return img


def _glass_tex():
    size = 32
    img = Image.new("RGBA", (size, size))
    cx = cy = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            r, g, b = 34, 42, 52
            if abs((x + y) - 18) <= 2:            # diagonal reflection streak
                r, g, b = 70, 82, 96
            d = math.hypot(x - cx, y - cy)
            if d < 7.5:                           # the lens behind the glass
                r, g, b = (14, 16, 20) if d < 6 else (58, 60, 64)
                if 2.0 < d < 3.2 and x < cx and y < cy:
                    r, g, b = 90, 104, 130
            if x in (0, size - 1) or y in (0, size - 1):
                r, g, b = 20, 22, 26
            img.putpixel((x, y), (r, g, b, 255))
    return img


def _lens_tex():
    size = 32
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = 222 if y % 4 else 196               # horizontal Fresnel ridges
            v -= int(abs(x - 15.5) * 0.8)
            if x in (0, size - 1) or y in (0, size - 1):
                v = 150
            img.putpixel((x, y), (v, v, min(255, v + 10), 255))
    return img


def _radar_tex():
    size = 32
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = 78 if (x % 4 == 0 or y % 4 == 0) else 62
            if x in (0, size - 1) or y in (0, size - 1):
                v = 40
            img.putpixel((x, y), (v, v + 2, v + 4, 255))
    return img


TEXTURES = {
    "housing_white": lambda: _noise_tex(1, (228, 226, 216), 5),
    "housing_black": lambda: _noise_tex(2, (38, 40, 42), 4),
    "trim_black": lambda: _noise_tex(3, (22, 22, 23), 3),
    "trim_charcoal": lambda: _noise_tex(4, (66, 68, 71), 3),
    "glass": _glass_tex,
    "flash_lens": _lens_tex,
    "radar_panel": _radar_tex,
}


# ---- writing -----------------------------------------------------------------------------------
def _obj_text(mesh, name):
    out = io.StringIO()
    out.write("# Procedurally generated by dev-env-utils/scripts/gen_enforcement_cameras.py\n")
    out.write("mtllib %s\no %s\n" % (MTL_NAME, name))
    for x, y, z in mesh.v:
        out.write("v %.6f %.6f %.6f\n" % (x + 0.5, y, z + 0.5))
    for u, w in mesh.vt:
        out.write("vt %.6f %.6f\n" % (u, w))
    for x, y, z in mesh.vn:
        out.write("vn %.6f %.6f %.6f\n" % (x, y, z))
    cur = None
    for mat, idx in mesh.faces:
        if mat != cur:
            out.write("usemtl %s\n" % mat)
            cur = mat
        out.write("f %s\n" % " ".join("%d/%d/%d" % t for t in idx))
    return out.getvalue()


def _mtl_text():
    mats = [("housing", FINISHES["white"]["housing"]), ("trim", FINISHES["white"]["trim"]),
            ("bracket", FINISHES["white"]["bracket"]), ("glass", TEX_ID + "/glass"),
            ("lens", TEX_ID + "/flash_lens"), ("radar", TEX_ID + "/radar_panel")]
    lines = ["# Procedurally generated by gen_enforcement_cameras.py", ""]
    for name, tex in mats:
        lines += ["newmtl %s" % name, "map_Kd %s" % tex, ""]
    return "\n".join(lines)


# The arm's hand, named as seen from behind the camera (looking the way it looks): "right" puts
# the pole on the model's +X side. Resolved in Java from which neighbour is a pole.
# (hand, mirrored, band strap stand-off from the pole)
ARMS = [("right", False, 0.012), ("left", True, 0.02)]
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}

# The inventory icon: forge:default-block with the gui entry fitted per model so it sits inside
# its 16 px slot -- every one of these is taller or longer than a block. The fit is the model's
# vertices projected through the gui rotation, scaled so the longer side spans 14.5 px and
# shifted to centre; audit_inventory_renders.py measured the pole-top speed camera at 0.4451
# against 0.4458 from this, so the projection and the in-game render agree. The side-arm
# entries are fitted on the right-hand model, which is the one the item draws.
INVENTORY_GUI = {
    "red_light_camera_top": (0.5516, [-0.0313, 0.0066, 0.0]),
    "speed_camera_top": (0.4425, [-0.0203, -0.0842, 0.0]),
    "red_light_camera_side": (0.5391, [0.1367, 0.1017, 0.0]),
    "flash_unit_side": (0.5754, [0.1764, 0.0935, 0.0]),
    "speed_camera_side": (0.5483, [0.1468, 0.0775, 0.0]),
}


def _inventory(model, fitted):
    scale, translation = INVENTORY_GUI[model]
    inv = {"transform": {
        "gui": {"rotation": [30, 225, 0], "scale": scale, "translation": translation},
        "ground": {"translation": [0, 0.1875, 0], "scale": 0.25},
        "fixed": {"scale": 0.5},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 0.15625, 0],
                                  "scale": 0.375},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 0.15625, 0],
                                 "scale": 0.375},
        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": 0.4},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": 0.4},
    }}
    if fitted:
        inv["model"] = "%s/%s_right.obj" % (MODEL_ID, model)
    return [inv]


def _blockstate(model, fitted, finish):
    tex = {"#" + k: v for k, v in FINISHES[finish].items()}
    if not fitted:
        variants = {
            "facing": {f: ({"y": y} if y else {}) for f, y in FACING_Y.items()},
            "inventory": _inventory(model, False),
            "normal": [{}],
        }
        defaults = {"model": "%s/%s.obj" % (MODEL_ID, model), "custom": {"flip-v": True},
                    "textures": tex}
    else:
        # Arm and pole fit both choose the model, which the per-property form cannot express,
        # so every state is written out whole (keys in the alphabetical order Forge names them).
        variants = {}
        for arm, _mirror, _pad in ARMS:
            for facing, y in FACING_Y.items():
                for fit, suffix, _r in POLE_FITS:
                    v = {"model": "%s/%s_%s%s.obj" % (MODEL_ID, model, arm, suffix)}
                    if y:
                        v["y"] = y
                    variants["arm=%s,facing=%s,polefit=%s" % (arm, facing, fit)] = [v]
        variants["inventory"] = _inventory(model, True)
        defaults = {"custom": {"flip-v": True}, "textures": tex}
    state = {"forge_marker": 1, "defaults": defaults, "variants": variants}
    return json.dumps(state, indent=2) + "\n"


def outputs():
    """Every generated file: {absolute path: bytes}."""
    files = {}
    model_dir = MODEL_REL + "/"
    for name, build, fitted in MODELS:
        if not fitted:
            variants = [("", None, False, None)]
        else:
            variants = [("_%s%s" % (arm, suffix), r16, mirror, pad)
                        for arm, mirror, pad in ARMS for _fit, suffix, r16 in POLE_FITS]
        for suffix, r16, mirror, pad in variants:
            mesh = Mesh()
            if fitted:
                build(mesh, r16, pad)
            else:
                build(mesh)
            if mirror:
                mirror_x(mesh)
            for (u, v) in mesh.vt:
                assert 0.0 <= u <= 1.0 and 0.0 <= v <= 1.0, name
            path = layout.asset_for_write(OWNER, model_dir + name + suffix + ".obj")
            files[path] = _obj_text(mesh, name + suffix).encode("utf-8")
    files[layout.asset_for_write(OWNER, model_dir + MTL_NAME)] = _mtl_text().encode("utf-8")
    for tex, make in TEXTURES.items():
        buf = io.BytesIO()
        make().save(buf, format="PNG", optimize=True)
        files[layout.asset_for_write(OWNER, TEX_REL + "/" + tex + ".png")] = buf.getvalue()
    fitted_models = {n for n, _b, f in MODELS if f}
    for stem, model, _disp in BLOCKS:
        for finish in FINISHES:
            path = layout.asset_for_write(OWNER, "blockstates/%s_%s.json" % (stem, finish))
            files[path] = _blockstate(model, model in fitted_models, finish).encode("utf-8")
    return files


def fragments():
    print("# lang")
    for stem, _model, disp in BLOCKS:
        for finish in FINISHES:
            print("tile.%s_%s.name=%s, %s)" % (stem, finish, disp, finish.capitalize()))
    print("\n# tab lines")
    for stem, model, _disp in BLOCKS:
        cls = "SideArm" if model.endswith("_side") else "PoleTop"
        for finish in FINISHES:
            print('    initTabBlock(new BlockEnforcementCamera.%s("%s_%s",\n'
                  '        BlockEnforcementCamera.BB_%s));' % (cls, stem, finish, model.upper()))


def main():
    if "--fragments" in sys.argv:
        fragments()
        return 0
    check = "--check" in sys.argv
    drift = []
    for path, data in sorted(outputs().items()):
        if path.endswith(".png"):
            same = os.path.exists(path) and open(path, "rb").read() == data
        else:
            with tempfile.NamedTemporaryFile("wb", delete=False, suffix=".tmp") as tmp:
                tmp.write(data)
            same = layout.same_generated_text(tmp.name, path)
            os.unlink(tmp.name)
        if same:
            continue
        drift.append(path)
        if not check:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "wb") as handle:
                handle.write(data)
    verb = "drifted" if check else "written"
    for path in drift:
        print("%s: %s" % (verb, os.path.relpath(path, layout.REPO_ROOT)))
    print("%d file(s) %s" % (len(drift), verb))
    return 1 if (check and drift) else 0


if __name__ == "__main__":
    sys.exit(main())
