#!/usr/bin/env python3
"""
gen_concrete_poles.py -- the concrete traffic poles and their accessories.

Concrete poles come in two profiles, a spun ROUND pole and a precast OCTAGON pole, each in the
two widths the rest of the pole family has: the 12-across thick pole and the 8-across thin pole.
All four are one block type each (``BlockTrafficPoleConcrete``), stacked to any height, which
decide for themselves what the two ends of their shaft show -- the same design as the pedestal
pole (see ``assets/docs/PEDESTAL_POLE_SYSTEM.md``):

  * ``none``  -- another concrete pole continues it, or it runs into some other solid block
  * ``cap``   -- the shaft ends in the open: a cap band and a chamfered top
  * ``tenon`` -- a post-top light sits on it: a collar and the tenon the fixture slips over
  * ``base``  -- a vertical pole standing on the ground: a stepped plinth

This script writes:

  * ``shared_models/concretepole_<profile>_<width>_<end>_<n|s>.json`` and ``..._inv.json``
    for the four poles, and their four blockstates
  * ``shared_models/<model>_octagon.json`` -- the straight vertical accessories with their round
    pole body swapped for the octagon one, every fitting kept
  * the round-concrete and octagon-concrete accessory blockstates, cloned from the silver ones
  * ``textures/blocks/trafficsignals/shared_textures/concrete_light_pole.png`` -- the concrete
    itself, which every one of the above (and the retiring legacy pieces) wears

WHY EACH END CARRIES ITS OWN HALF OF THE SHAFT
----------------------------------------------
The pedestal pole draws its tube in every block and hangs a cap or base on the end. The concrete
pole's cap and tenon are NARROWER than the shaft at the top of the block (a chamfer, a tapered
octagon spigot), which only works if the shaft stops short there. So the shaft is split at the
middle of the block and each end value supplies its own half: ``none`` a full half, ``cap`` and
``tenon`` a shortened one. That keeps the pole exactly one block tall per block, as the legacy
top pieces (``rcpt``, ``ocpt``) were.

Coordinates are in 1/16 block units. The shaft runs along model Z; model NORTH (z = 0) is the
end that points in the ``facing`` direction, so a pole placed on the ground (facing UP) has its
top at model north and its base at model south. Parts are authored at one end and mirrored.

Every face gets an explicit UV, proportional to the face and wrapped into 0..16 by whole
blocks: an automatic UV past the block edge samples the neighbouring sprite on the atlas.
The narrow rectangles a 16-gon or octagon is built from overlap, so their end faces would be
coplanar and flicker; each rectangle's end is set back by a hair more than the last so exactly
one wins. The half-shafts draw no end faces at all, since a shaft end is always a joint or
pressed against another block.

Usage:
    python gen_concrete_poles.py            # writes models, blockstates, lang/tab fragments
    python gen_concrete_poles.py --check    # regenerate into a temp dir and diff against the tree

Requires Pillow.
"""
import argparse
import copy
import json
import math
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

OWNER = layout.owner_of_folder("trafficaccessories")
MODEL_DIR = layout.asset_dir_for_write(OWNER, "models/block/trafficaccessories/shared_models")
BLOCKSTATE_DIR = layout.asset_dir_for_write(OWNER, "blockstates")
TEXTURE_REL = "textures/blocks/trafficsignals/shared_textures/concrete_light_pole.png"
TEXTURE_PATH = layout.resolve_asset(TEXTURE_REL)
SCRATCH_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_concrete_poles_out")

CONCRETE = "csm:blocks/trafficsignals/shared_textures/concrete_light_pole"
SILVER = "csm:blocks/trafficsignals/shared_textures/metal_silver"
MODEL_PREFIX = "csm:trafficaccessories/shared_models/"

# Setback between the end faces of the overlapping rectangles of one polygon (1/16 units).
STAGGER = 0.002

# ------------------------------------------------------------------------------------------
# The four poles
# ------------------------------------------------------------------------------------------
# Each end part is a list of (diameter, z_from, z_to) tiers authored at model NORTH (the top of
# a pole standing on the ground), except the base, which is authored at model SOUTH. Diameters
# are across the flats. "shaft" in a tier list is the pole's own diameter.
#
# Large round: base and tenon follow the legacy rcpb2 / rcpt pieces; large octagon: ocpb / ocpt.
# The plinth's upper step is D+3, not the legacy D+2, so a one-block pole wearing both a plinth
# and a tenon collar (D+2) never has two coplanar walls.
POLES = {
    "round_large": {
        "shape": "round", "d": 12.0,
        "cap": [("shaft", 1.0, 8.0), (13.0, 0.5, 1.5), (10.0, 0.0, 0.5)],
        "tenon": [("shaft", 5.0, 8.0), (14.0, 4.0, 5.0), (5.0, -4.0, 4.0)],
        "base": [(15.0, 4.0, 8.0), (16.0, 8.0, 16.0)],
    },
    "octagon_large": {
        "shape": "octagon", "d": 12.0,
        "cap": [("shaft", 1.0, 8.0), (13.0, 0.5, 1.5), (10.0, 0.0, 0.5)],
        "tenon": [("shaft", 4.5, 8.0), (14.0, 3.5, 4.5), (10.0, 0.5, 3.5), (8.0, -0.5, 0.5),
                  (4.0, -1.5, -0.5), (2.0, -2.5, -1.5)],
        "base": [(15.0, 4.0, 8.0), (16.0, 8.0, 16.0)],
    },
    "round_thin": {
        "shape": "round", "d": 8.0,
        "cap": [("shaft", 1.0, 8.0), (9.0, 0.5, 1.5), (6.5, 0.0, 0.5)],
        "tenon": [("shaft", 5.0, 8.0), (10.0, 4.0, 5.0), (4.0, -4.0, 4.0)],
        "base": [(11.0, 4.0, 8.0), (12.0, 8.0, 16.0)],
    },
    "octagon_thin": {
        "shape": "octagon", "d": 8.0,
        "cap": [("shaft", 1.0, 8.0), (9.0, 0.5, 1.5), (6.5, 0.0, 0.5)],
        "tenon": [("shaft", 4.5, 8.0), (10.0, 3.5, 4.5), (7.0, 0.5, 3.5), (5.5, -0.5, 0.5),
                  (3.0, -1.5, -0.5), (1.5, -2.5, -1.5)],
        "base": [(11.0, 4.0, 8.0), (12.0, 8.0, 16.0)],
    },
}

# (registry name, pole key, mount model, display name)
POLE_BLOCKS = [
    ("trafficpoleverticalconcrete", "round_large", "csm:lighting/shared_models/large_mount",
     "Concrete Thick Traffic Pole"),
    ("trafficpoleverticalconcreteoctagon", "octagon_large",
     "csm:lighting/shared_models/large_mount", "Octagon Concrete Thick Traffic Pole"),
    ("trafficpolehorizontalconcrete", "round_thin",
     "csm:trafficaccessories/shared_models/small_mount", "Concrete Thin Traffic Pole"),
    ("trafficpolehorizontalconcreteoctagon", "octagon_thin",
     "csm:trafficaccessories/shared_models/small_mount", "Octagon Concrete Thin Traffic Pole"),
]

END_STYLES = ["none", "cap", "tenon", "base"]

# ------------------------------------------------------------------------------------------
# The accessories
# ------------------------------------------------------------------------------------------
# (silver blockstate to clone, round-concrete id, octagon-concrete id or None, display stem)
# The display stem gets " (Concrete)" / " (Octagon Concrete)" appended, the style the five
# existing concrete accessories already use. Those five predate this script; it now writes their
# blockstates too (same ids, a body with UVs that suit the concrete), but not their lang lines.
# Families that are already retiring (horizontal single/double mounts, angle mounts 1-3) and the
# pole base / decorative metal top are deliberately absent; see CONCRETE_POLE_SYSTEM.md.
ACCESSORIES = [
    ("trafficpolehorizontalanglesilver", "trafficpolehorizontalangleconcrete", None,
     "Angled Thin Traffic Pole"),
    ("trafficpolehorizsignmountsilver", "trafficpolehorizsignmountconcrete", None,
     "Horizontal Traffic Pole Sign Mount"),
    ("trafficpoleverticalconnectorangledsilver", "trafficpoleverticalconnectorangledconcrete",
     "trafficpoleverticalconnectorangledconcreteoctagon",
     "Vertical Traffic Pole with Angled Connector"),
    ("trafficpoleverticalconnectordoublesilver", "trafficpoleverticalconnectordoubleconcrete",
     "trafficpoleverticalconnectordoubleconcreteoctagon",
     "Vertical Traffic Pole with Double Connector"),
    ("trafficpoleverticalcurveconnector", "trafficpoleverticalcurveconnectorconcrete", None,
     "Vertical Traffic Pole with Curve Connector"),
    ("trafficpoleverticalcurveconnectordoubleguysilver",
     "trafficpoleverticalcurveconnectordoubleguyconcrete", None,
     "Traffic Pole Curved Double Guy Connector"),
    ("trafficpoleverticaldoubleguymountsilver", "trafficpoleverticaldoubleguymountconcrete",
     "trafficpoleverticaldoubleguymountconcreteoctagon",
     "Vertical Traffic Pole with Double Guy Mount"),
    ("trafficpoleverticallightmount", "trafficpoleverticallightmountconcrete", None,
     "Vertical Traffic Pole with Light Mount"),
    # The five that predate this script (see EXISTING_ROUND).
    ("trafficpolehorzdblsilver", "trafficpolehorzdblconcrete", None,
     "Double Horizontal Traffic Pole"),
    ("trafficpoleverticalconnector", "trafficpoleverticalconnectorconcrete",
     "trafficpoleverticalconnectorconcreteoctagon",
     "Vertical Traffic Pole with Connector"),
    ("trafficpoleverticalquadmount", "trafficpoleverticalquadmountconcrete",
     "trafficpoleverticalquadmountconcreteoctagon",
     "Vertical Traffic Pole with Quad Mount"),
    ("trafficpoleverticalsignalmount", "trafficpoleverticalsignalmountconcrete",
     "trafficpoleverticalsignalmountconcreteoctagon",
     "Vertical Traffic Pole with Signal Mount"),
    ("trafficpolevertdblsilver", "trafficpolevertdblconcrete", "trafficpolevertdblconcreteoctagon",
     "Double Vertical Traffic Pole"),
]

# Round concrete accessories whose ids and lang lines were in the tree before this script.
EXISTING_ROUND = {
    "trafficpolehorzdblconcrete", "trafficpoleverticalconnectorconcrete",
    "trafficpoleverticalquadmountconcrete", "trafficpoleverticalsignalmountconcrete",
    "trafficpolevertdblconcrete",
}

# The straight vertical sections that get an octagon body. Their first eight elements are the
# 12-across 16-gon pole body (asserted below); everything after is fittings, kept as drawn.
# Every accessory model whose body is a plain 16-gon (asserted) gets a round-concrete copy with
# that body redrawn; the curved sweeps are hundreds of segments and keep their own.
ROUND_SOURCE_MODELS = [
    "trafficpolecurveconnector",
    "trafficpoleverticalconnectordouble",
    "trafficpoleverticaldoubleguymount",
    "trafficpoleverticalconnector",
    "trafficpoleverticalquadmount",
    "trafficpoleverticalsignalmount",
    "trafficpolevertical_double",
    "trafficpolehorizontal_double",
    "trafficpolehorizontalsignmount",
]

OCTAGON_SOURCE_MODELS = [
    "trafficpolecurveconnector",
    "trafficpoleverticalconnectordouble",
    "trafficpoleverticaldoubleguymount",
    "trafficpoleverticalconnector",
    "trafficpoleverticalquadmount",
    "trafficpoleverticalsignalmount",
    "trafficpolevertical_double",
]


# ------------------------------------------------------------------------------------------
# Geometry
# ------------------------------------------------------------------------------------------
def r5(v):
    return round(v + 0.0, 5) + 0.0


def wrap_span(a, b):
    """Shifts a UV span by whole blocks into 0..16 (never clamps: a clamp stretches)."""
    lo, hi = min(a, b), max(a, b)
    shift = 0.0
    while lo + shift < -1e-6:
        shift += 16.0
    while hi + shift > 16.0 + 1e-6:
        shift -= 16.0
    return r5(a + shift), r5(b + shift)


def face_uv(face, f, t):
    """Minecraft's automatic UV for a face of the unrotated box, wrapped into the sprite."""
    x0, y0, z0 = f
    x1, y1, z1 = t
    if face == "north":
        u = (16 - x1, 16 - x0)
        v = (16 - y1, 16 - y0)
    elif face == "south":
        u = (x0, x1)
        v = (16 - y1, 16 - y0)
    elif face == "east":
        u = (16 - z1, 16 - z0)
        v = (16 - y1, 16 - y0)
    elif face == "west":
        u = (z0, z1)
        v = (16 - y1, 16 - y0)
    elif face == "up":
        u = (x0, x1)
        v = (z0, z1)
    else:  # down
        u = (x0, x1)
        v = (16 - z1, 16 - z0)
    u0, u1 = wrap_span(*u)
    v0, v1 = wrap_span(*v)
    return [u0, v0, u1, v1]


def element(f, t, angle, axis, texture="#0", faces=("north", "east", "south", "west", "up",
                                                    "down"), name=None):
    el = {}
    if name:
        el["name"] = name
    el["from"] = [r5(c) for c in f]
    el["to"] = [r5(c) for c in t]
    if angle:
        el["rotation"] = {"angle": angle, "axis": axis, "origin": [8, 8, 8]}
    el["faces"] = {fc: {"uv": face_uv(fc, f, t), "texture": texture} for fc in faces}
    return el


def polygon_rects(shape, d):
    """The rectangles a 16-gon ("round") or octagon of diameter d across the flats is built
    from: (half-width across x, half-width across y, rotation angle). Each rectangle is d long
    and d*tan(pi/n) wide; turned to the listed angles they cover every pair of sides."""
    if shape == "round":
        w = d * math.tan(math.pi / 16) / 2
        return ([(w, d / 2, a) for a in (-45, -22.5, 0, 22.5, 45)]
                + [(d / 2, w, a) for a in (-22.5, 0, 22.5)])
    w = d * math.tan(math.pi / 8) / 2
    return [(w, d / 2, 0), (w, d / 2, 45), (d / 2, w, 0), (d / 2, w, 45)]


def prism_z(shape, d, z0, z1, ends=True, name=None, end_south=None):
    """A prism along model Z from z0 to z1. With ``ends`` the end faces are drawn, each
    rectangle's set back by STAGGER more than the last so the overlapping ends never tie.
    ``end_south``, when given, decides the z1 end separately and ``ends`` only the z0 end."""
    if end_south is None:
        end_south = ends
    out = []
    for i, (hx, hy, a) in enumerate(polygon_rects(shape, d)):
        e0 = STAGGER * i if ends else 0.0
        e1 = STAGGER * i if end_south else 0.0
        faces = (["east", "west", "up", "down"] + (["north"] if ends else [])
                 + (["south"] if end_south else []))
        out.append(element((8 - hx, 8 - hy, z0 + e0), (8 + hx, 8 + hy, z1 - e1), a, "z",
                           faces=faces, name=name or shape))
    return out


def block_spans(a, b):
    """[a, b] cut at every block boundary, so no face is longer than one sprite: a UV span
    past 16 cannot be wrapped and samples the neighbouring sprite."""
    cuts = [a] + [16.0 * k for k in range(int(math.floor(a / 16)) + 1,
                                          int(math.ceil(b / 16)))] + [b]
    return list(zip(cuts, cuts[1:]))


def prism_y(shape, d, y0, y1, name):
    """A prism along model Y, for the accessories whose pole body runs that way. No end faces:
    an accessory's vertical body ends are joints with the pole above and below."""
    out = []
    for s0, s1 in block_spans(y0, y1):
        for hx, hz, a in polygon_rects(shape, d):
            out.append(element((8 - hx, s0, 8 - hz), (8 + hx, s1, 8 + hz), a, "y",
                               faces=["north", "east", "south", "west"], name=name))
    return out


def mirror_z(elements):
    """The same part at the other end of the block: z -> 16 - z. A rotation about Z is
    unchanged by the mirror; the north and south faces trade places."""
    out = []
    for el in elements:
        f, t = el["from"], el["to"]
        nf = [f[0], f[1], 16 - t[2]]
        nt = [t[0], t[1], 16 - f[2]]
        faces = []
        for fc in el["faces"]:
            faces.append({"north": "south", "south": "north"}.get(fc, fc))
        rot = el.get("rotation")
        new = element(nf, nt, rot["angle"] if rot else 0, rot["axis"] if rot else "z",
                      texture=next(iter(el["faces"].values()))["texture"],
                      faces=[fc for fc in ("north", "east", "south", "west", "up", "down")
                             if fc in faces], name=el.get("name"))
        out.append(new)
    return out


def build_part(pole, end):
    """The elements of one end of a pole, at model north (base: model south)."""
    spec = POLES[pole]
    shape, d = spec["shape"], spec["d"]
    if end == "none":
        return prism_z(shape, d, 0.0, 8.0, ends=False, name="shaft")
    els = []
    for dia, z0, z1 in spec[end]:
        if dia == "shaft":
            els += prism_z(shape, d, z0, z1, ends=False, name="shaft")
        else:
            els += prism_z(shape, dia, z0, z1, name=end)
    return els


def model_json(elements, texture):
    return {
        "credit": "Generated by dev-env-utils/scripts/gen_concrete_poles.py -- do not edit",
        "textures": {"0": texture, "particle": texture},
        "elements": elements,
    }


def pole_models(pole):
    """{suffix: model} for every end model of one pole, plus the inventory model."""
    models = {}
    for end in END_STYLES:
        part = build_part(pole, end)
        if end == "base":  # authored at model south
            models["base_s"] = part
            models["base_n"] = mirror_z(part)
        else:
            models[end + "_n"] = part
            models[end + "_s"] = mirror_z(part)
    models["inv"] = models["cap_n"] + models["base_s"]
    for key, els in models.items():
        for el in els:
            for c in el["from"] + el["to"]:
                assert -16.0 <= c <= 32.0, (pole, key, c)
    return {k: model_json(v, CONCRETE) for k, v in models.items()}


def model_name(pole, suffix):
    return "concretepole_%s_%s" % (pole, suffix)


# ------------------------------------------------------------------------------------------
# Blockstates
# ------------------------------------------------------------------------------------------
TEXTURES = {"all": CONCRETE, "particle": CONCRETE, "0": CONCRETE}


def mount_variant(key, mount_model, rotation):
    return {"false": {}, "true": {"submodel": {key: {
        "uvlock": True,
        "model": mount_model,
        "textures": dict(TEXTURES),
        "transform": {"rotation": rotation},
    }}}}


def pole_blockstate(pole, mount_model):
    def m(suffix):
        return MODEL_PREFIX + model_name(pole, suffix)

    return {
        "forge_marker": 1,
        "defaults": {"model": m("inv"), "textures": dict(TEXTURES)},
        "variants": {
            # Stands the pole up in the inventory, the way a pole placed on the ground stands.
            "inventory": [{"x": 270}],
            "facing": {
                "north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
                "up": {"x": 270}, "down": {"x": 90},
            },
            # One property may set the model and another add submodels over it (the
            # arrangement controllablerrfb.json uses): the north end is the model, the south
            # end a submodel. Neither end draws the other half of the shaft.
            "endn": {end: {"model": m(end + "_n")} for end in END_STYLES},
            "ends": {end: {"submodel": {"ends": {"model": m(end + "_s"),
                                                  "textures": dict(TEXTURES)}}}
                     for end in END_STYLES},
            "mounteast": mount_variant("mounte", mount_model, [{"x": 0}, {"y": 90}, {"z": 0}]),
            "mountwest": mount_variant("mountw", mount_model, [{"x": 0}, {"y": 270}, {"z": 0}]),
            "mountup": mount_variant("mountu", mount_model, [{"x": 270}, {"y": 0}, {"z": 0}]),
            "mountdown": mount_variant("mountd", mount_model, [{"x": 90}, {"y": 0}, {"z": 0}]),
        },
    }


def retexture(node):
    """Every silver texture reference in a blockstate, made concrete. Anything else (a sign
    plate's art) is left as it is."""
    if isinstance(node, dict):
        return {k: retexture(v) for k, v in node.items()}
    if isinstance(node, list):
        return [retexture(v) for v in node]
    if node == SILVER:
        return CONCRETE
    return node


def remodel(node, mapping):
    """Every model reference in a blockstate that has an octagon twin, pointed at the twin."""
    if isinstance(node, dict):
        out = {}
        for k, v in node.items():
            if k == "model" and isinstance(v, str) and v in mapping:
                out[k] = mapping[v]
            else:
                out[k] = remodel(v, mapping)
        return out
    if isinstance(node, list):
        return [remodel(v, mapping) for v in node]
    return node


def body_swapped_model(source, shape, source_name):
    """The accessory model with its round pole body redrawn as ``shape``, every fitting kept.

    The source models draw their body as eight rectangles of one 16-gon with a whole 0..16 UV
    stretched across each narrow strip. On a flat metal texture that is invisible; on the
    concrete it streaks. So the round concrete accessories get the body redrawn too, at the same
    width, with the proportional UVs the poles use, and the octagon ones get the octagon.

    A body running along Y is a vertical pole section: both ends are joints with the pole above
    and below, so they are left open. A body along Z is an arm whose ends can be in the open, so
    its outer ends are closed; the cuts at block boundaries in between are not."""
    src = copy.deepcopy(source)
    body, rest = src["elements"][:8], src["elements"][8:]
    tex = next(iter(body[0]["faces"].values()))["texture"]
    axes, widths = set(), set()
    for el in body:
        f, t = el["from"], el["to"]
        dims = sorted(round(t[k] - f[k], 3) for k in range(3))
        d = dims[1]
        assert abs(dims[0] - d * math.tan(math.pi / 16)) < 0.01, (source_name, el)
        widths.add(round(d, 3))
        axes.add(max(range(3), key=lambda k: t[k] - f[k]))
    assert len(axes) == 1 and len(widths) == 1, (source_name, axes, widths)
    axis, d = axes.pop(), widths.pop()
    lo, hi = body[0]["from"][axis], body[0]["to"][axis]
    if axis == 1:
        new_body = prism_y(shape, d, lo, hi, shape)
    elif axis == 2:
        new_body = []
        for s0, s1 in block_spans(lo, hi):
            new_body += prism_z(shape, d, s0, s1, ends=(s0 == lo), end_south=(s1 == hi),
                                name=shape)
    else:
        raise AssertionError("no accessory runs its pole along X: " + source_name)
    for el in new_body:
        for fc in el["faces"].values():
            fc["texture"] = tex
    src["elements"] = new_body + rest
    src["credit"] = ("Generated by dev-env-utils/scripts/gen_concrete_poles.py from %s.json "
                     "-- do not edit" % source_name)
    src.pop("groups", None)
    return src


# ------------------------------------------------------------------------------------------
# The concrete
# ------------------------------------------------------------------------------------------
# The texture it replaced was a photo of exposed aggregate: pebbles a quarter of a block across
# at full contrast, which on a 12-across pole read as white blotches and on the thin strips of a
# 16-gon as stripes. This keeps its colour (the mean of the old texture) and draws what a cast or
# spun concrete pole actually shows at this distance: an uneven cure and sparse small pits.
TEX_SIZE = 64
TEX_BASE = (162, 156, 149)
TEX_SEED = 20260922


def _mottle(rng, cells):
    """Value noise that tiles: random heights on a ``cells`` x ``cells`` lattice, eased between
    with a cosine and read with wrap-around. The same construction as gen_stucco.py's; lattices
    that do not divide the tile keep the cell edges from lining up into a grid."""
    lattice = [[rng.uniform(-1, 1) for _ in range(cells)] for _ in range(cells)]
    step = TEX_SIZE / cells
    out = [[0.0] * TEX_SIZE for _ in range(TEX_SIZE)]
    for y in range(TEX_SIZE):
        gy, fy = divmod(y / step, 1)
        wy = (1 - math.cos(fy * math.pi)) / 2
        y0, y1 = int(gy) % cells, (int(gy) + 1) % cells
        for x in range(TEX_SIZE):
            gx, fx = divmod(x / step, 1)
            wx = (1 - math.cos(fx * math.pi)) / 2
            x0, x1 = int(gx) % cells, (int(gx) + 1) % cells
            top = lattice[y0][x0] * (1 - wx) + lattice[y0][x1] * wx
            bottom = lattice[y1][x0] * (1 - wx) + lattice[y1][x1] * wx
            out[y][x] = top * (1 - wy) + bottom * wy
    return out


# Pit colours, as offsets from the base: the voids and fine aggregate a cast face shows are
# darker than the paste and not grey -- browns, a rust, a cool grey -- which is what makes them
# read as stone rather than as noise.
PIT_TINTS = [(-34, -38, -40), (-30, -36, -46), (-24, -34, -42), (-38, -38, -34),
             (-20, -26, -30)]


def concrete_texture():
    """A smooth cast face: a pale, gently mottled paste with sparse dark pits.

    Modelled on a photo of a cast concrete face: almost all of the surface is plain paste
    with a soft, cloudy variation; the aggregate shows as scattered small dark pits, most a
    single point, a few larger, in warm browns and greys; bright grains are rare. The base colour
    is the old texture's mean, a shade darker than the photo, so the poles keep their tone."""
    rng = random.Random(TEX_SEED)
    broad, mid, fine = _mottle(rng, 3), _mottle(rng, 5), _mottle(rng, 11)
    field = [[[c + broad[y][x] * 8 + mid[y][x] * 5 + fine[y][x] * 2 + rng.uniform(-2.5, 2.5)
               for c in TEX_BASE] for x in range(TEX_SIZE)] for y in range(TEX_SIZE)]

    def pit(x, y, tint, weight):
        cell = field[y % TEX_SIZE][x % TEX_SIZE]
        for i in range(3):
            cell[i] += tint[i] * weight

    # Single-point pits, at varying depth.
    for _ in range(int(TEX_SIZE * TEX_SIZE * 0.035)):
        pit(rng.randrange(TEX_SIZE), rng.randrange(TEX_SIZE), rng.choice(PIT_TINTS),
            rng.uniform(0.45, 1.0))
    # Larger pits: a dark core with a lighter rim pixel or two, never a square.
    for _ in range(14):
        x, y, tint = rng.randrange(TEX_SIZE), rng.randrange(TEX_SIZE), rng.choice(PIT_TINTS)
        pit(x, y, tint, 1.1)
        for dx, dy in rng.sample([(1, 0), (0, 1), (-1, 0), (0, -1), (1, 1)], rng.randint(1, 2)):
            pit(x + dx, y + dy, tint, 0.55)
    # The rare bright grain.
    for _ in range(int(TEX_SIZE * TEX_SIZE * 0.004)):
        pit(rng.randrange(TEX_SIZE), rng.randrange(TEX_SIZE), (14, 14, 14), 1.0)

    img = Image.new("RGBA", (TEX_SIZE, TEX_SIZE))
    px = img.load()
    for y in range(TEX_SIZE):
        for x in range(TEX_SIZE):
            px[x, y] = tuple(max(0, min(255, int(round(v)))) for v in field[y][x]) + (255,)
    return img


def same_image(a, b):
    if not os.path.exists(b):
        return False
    return (Image.open(a).convert("RGBA").tobytes()
            == Image.open(b).convert("RGBA").tobytes())


# ------------------------------------------------------------------------------------------
# Output
# ------------------------------------------------------------------------------------------
def dump(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


def read_json(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def generate(model_dir, blockstate_dir, scratch_dir, texture_path):
    """Writes everything; returns (model file names, blockstate file names)."""
    models, blockstates = [], []
    os.makedirs(os.path.dirname(texture_path), exist_ok=True)
    concrete_texture().save(texture_path)
    lang, tab = [], []

    for name, pole, mount, label in POLE_BLOCKS:
        for suffix, data in pole_models(pole).items():
            fn = model_name(pole, suffix) + ".json"
            dump(os.path.join(model_dir, fn), data)
            models.append(fn)
        dump(os.path.join(blockstate_dir, name + ".json"), pole_blockstate(pole, mount))
        blockstates.append(name + ".json")
        lang.append("tile.%s.name=%s" % (name, label))

    round_map, octagon_map = {}, {}
    for shape, sources, suffix, mapping in (("round", ROUND_SOURCE_MODELS, "_concrete", round_map),
                                            ("octagon", OCTAGON_SOURCE_MODELS, "_octagon",
                                             octagon_map)):
        for src_name in sources:
            src_path = layout.resolve_asset(
                "models/block/trafficaccessories/shared_models/%s.json" % src_name)
            fn = src_name + suffix + ".json"
            dump(os.path.join(model_dir, fn), body_swapped_model(read_json(src_path), shape,
                                                                 src_name))
            models.append(fn)
            mapping[MODEL_PREFIX + src_name] = MODEL_PREFIX + src_name + suffix

    for silver, round_id, oct_id, label in ACCESSORIES:
        source = retexture(read_json(layout.blockstate_file(silver)))
        round_state = remodel(source, round_map)
        if round_id:
            dump(os.path.join(blockstate_dir, round_id + ".json"), round_state)
            blockstates.append(round_id + ".json")
            if round_id not in EXISTING_ROUND:
                lang.append("tile.%s.name=%s (Concrete)" % (round_id, label))
        if oct_id:
            oct_state = remodel(source, octagon_map)
            assert oct_state != source, "no octagon model for " + silver
            dump(os.path.join(blockstate_dir, oct_id + ".json"), oct_state)
            blockstates.append(oct_id + ".json")
            lang.append("tile.%s.name=%s (Octagon Concrete)" % (oct_id, label))

    os.makedirs(scratch_dir, exist_ok=True)
    with open(os.path.join(scratch_dir, "lang_fragment.txt"), "w", newline="\n") as fh:
        fh.write("\n".join(lang) + "\n")
    return models, blockstates


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true",
                    help="regenerate into a temp dir and fail if the tree differs")
    args = ap.parse_args()

    if not args.check:
        models, states = generate(MODEL_DIR, BLOCKSTATE_DIR, SCRATCH_DIR, TEXTURE_PATH)
        print("wrote %d models to %s" % (len(models), MODEL_DIR))
        print("wrote %d blockstates to %s" % (len(states), BLOCKSTATE_DIR))
        print("lang fragment in %s" % SCRATCH_DIR)
        return 0

    tmp = tempfile.mkdtemp(prefix="concrete_poles_")
    try:
        models, states = generate(os.path.join(tmp, "m"), os.path.join(tmp, "b"),
                                  os.path.join(tmp, "s"), os.path.join(tmp, "t.png"))
        stale = []
        if not same_image(os.path.join(tmp, "t.png"), TEXTURE_PATH):
            stale.append(TEXTURE_REL)
        for fn in models:
            if not layout.same_generated_text(os.path.join(tmp, "m", fn),
                                              os.path.join(MODEL_DIR, fn)):
                stale.append(fn)
        for fn in states:
            if not layout.same_generated_text(os.path.join(tmp, "b", fn),
                                              os.path.join(BLOCKSTATE_DIR, fn)):
                stale.append(fn)
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("concrete pole assets are up to date")
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
