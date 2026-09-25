#!/usr/bin/env python3
"""
gen_furniture_bedroom.py -- the Residential tab's bedroom, study and nursery in the Furniture &
Novelties module: beds you sleep in (single, double and king in four fabrics; a day bed and a
bunk bed in the three wood finishes), the nightstand, dresser, dresser with mirror, wardrobe,
the white built-in closet, the blanket chest, the desk and desk chair, the crib, the cradle
with drawers, the changing table, the rocking chair, the standing mirror, the vanity and its
stool, and area rugs in four colours.

Borrows gen_furniture_residential.py's element helpers, finishes, textures and output, and
gen_furniture_kitchen.py's Shaker fronts, pulls and the cut of a two-block piece into halves
(imported, not copied), and writes, under modules/furnishings/src/main/resources/assets/csm:

  * textures/blocks/furniture/residential/*.png  bedding (quilted duvets, tufted headboards,
    sheets, pillows, ticking), mirror glass, brass, louvres, the nursery's gingham and changing
    pad, and the rugs' fields and bound borders
  * models/block/furniture/residential/base/*.json  the geometry
  * models/block/furniture/residential/<registry>_<part>.json  a finish's copy of each part or
    cell a blockstate picks
  * blockstates/<registry>.json, and models/item/<registry>.json where the state has no
    inventory variant
  * the tile names in all four languages, and the bed's status message, by key

Every model faces north with its back at +Z, as in the living room set; a bed's foot is its
front (towards whoever placed it) and its head against the wall. Real-world scale, 1 block =
1 m: a mattress top at 0.56 m as a vanilla bed's, a desk at 0.75 m, a wardrobe 1.9 m.

A bed is drawn whole, in the frame BedLayout.java names (x to the right of someone at the foot,
z towards the head, the placed block at the origin), and cut at the block lines into one model
per block it fills, so each is lit and culled in its own block; the mattress heights must be
the ones BedLayout.java holds (9 for single, double, king; 7 for the day bed; 6 and 5 (in its
own block) for the lower and upper bunk). The two-block pieces are cut into halves the same way
as the kitchen's refrigerator.

What joins (the Java classes compute it as actual state; nothing is stored):

  * the desk joins left and right into one long desk, a side panel only where it stops;
  * the closet joins left and right into a wall of closets, end panels only at its ends;
  * the rug joins on all four sides into a rug of any size: its bound border runs only along
    an open side, so a 3 x 4 patch is one rug with one border.

Usage:
    python gen_furniture_bedroom.py              # write everything
    python gen_furniture_bedroom.py --check      # fail if the tree has drifted
    python gen_furniture_bedroom.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import argparse
import copy
import math
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_furniture_residential as R  # noqa: E402
import gen_furniture_kitchen as K  # noqa: E402

ASSETS = R.ASSETS
SUB = R.SUB
T = R.T
MODEL = R.MODEL
BASE = R.BASE
ROT = R.ROT
el, board, leg = R.el, R.board, R.leg
mirror_x, turn = R.mirror_x, R.turn
ALL = R.ALL
shaker, bar_h, bar_v = K.shaker, K.bar_h, K.bar_v
NO_BACK = ("north", "east", "west", "up", "down")


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def duvet(base, seed, size=16):
    """A quilted duvet: the fabric's weave, sewn through every half metre, each square puffed
    up towards its middle."""
    img = R.fabric(base, seed, size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            dx = min(x % 8, 7 - (x % 8))
            dy = min(y % 8, 7 - (y % 8))
            d = min(dx, dy)
            k = 0.8 if d == 0 and (x % 8 == 0 or y % 8 == 0) else 0.9 + 0.035 * min(d, 3)
            c = px[x, y][:3]
            px[x, y] = R.shade(c, k) + (255,)
    return img


def tufted(base, seed, size=16):
    """A button-tufted headboard: buttons on a diamond lattice, the fabric drawn in to each
    and a crease running between neighbouring buttons."""
    img = R.fabric(base, seed, size)
    px = img.load()
    buttons = [(bx, by) for by in range(0, size + 1, 4) for bx in range(0, size + 1, 4)
               if (bx // 4 + by // 4) % 2 == 0]
    for y in range(size):
        for x in range(size):
            d = min(math.hypot(x + 0.5 - bx, y + 0.5 - by) for bx, by in buttons)
            crease = abs(((x + y) % 8) - 4) < 0.5 or abs(((x - y) % 8) - 4) < 0.5
            k = 0.62 if d < 0.8 else (0.82 + 0.06 * min(d, 2.5))
            if crease and d > 0.8:
                k *= 0.93
            px[x, y] = R.shade(px[x, y][:3], k) + (255,)
    return img


def ticking(seed, size=16):
    """Mattress ticking: white cotton with narrow blue-grey stripes along it."""
    img = R.fabric((236, 236, 232), seed, size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if x % 4 == 1:
                px[x, y] = R.shade((160, 172, 188), 1.0 + (0.02 if y % 2 else -0.02)) + (255,)
    return img


def mirror_glass(size=16):
    """Silvered glass: a cool grey-blue brighter towards the top, with two soft diagonal
    highlights."""
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = 1.08 - 0.16 * y / (size - 1)
            s = (x + y) % 16
            if s in (4, 5):
                k += 0.18
            elif s in (3, 6, 10):
                k += 0.08
            px[x, y] = R.shade((170, 188, 198), k) + (255,)
    return img


def gingham(seed, size=16):
    """The nursery's pale blue gingham: two-texel checks, deeper where the stripes cross."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            a = (x // 2) % 2
            b = (y // 2) % 2
            c = (168, 198, 226) if a and b else ((212, 228, 242) if a or b else (248, 250, 252))
            px[x, y] = R.shade(c, 1.0 + rng.uniform(-0.015, 0.015)) + (255,)
    return img


def louvre(base, seed, size=16):
    """Louvred door slats seen from the front: each slat lit on its upper face and falling into
    shadow at its lower edge, two texels to a slat."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        k = 1.02 if y % 2 == 0 else 0.84
        for x in range(size):
            px[x, y] = R.shade(base, k + rng.uniform(-0.01, 0.01)) + (255,)
    return img


def rug_field(field, motif, seed, size=16):
    """A rug's field: soft wool with a diamond trellis every half block, so the pattern tiles
    across a rug of any size without showing where one block ends."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            u = abs(((x + 0.5) % 8) - 4)
            v = abs(((y + 0.5) % 8) - 4)
            d = u + v
            c = motif if abs(d - 3.0) < 0.6 or d < 0.8 else field
            k = 1.0 + rng.uniform(-0.035, 0.035)
            if rng.random() < 0.04:
                k *= 0.9
            px[x, y] = R.shade(c, k) + (255,)
    return img


def rug_border(binding, accent, field, seed, size=16):
    """A rug's bound border, three texels deep: the binding at the edge, a band of the accent,
    then the field's own colour darkened. The top left 3 x 3 is the corner, turned round both
    edges; the bottom row is the binding again, for the border's outer edge."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    rings = [binding, accent, R.shade(field, 0.8)]
    for y in range(size):
        for x in range(size):
            if y >= size - 1:
                c = binding
            elif x < 3 and y < 3:
                c = rings[min(x, y)]
            elif y < 3:
                c = rings[y]
            else:
                c = R.shade(field, 0.8)
            px[x, y] = R.shade(c, 1.0 + rng.uniform(-0.03, 0.03)) + (255,)
    return img


FABRIC_BASE = {"charcoal": (74, 74, 78), "navy": (40, 54, 94), "oatmeal": (204, 190, 164),
               "red": (150, 36, 38)}
# Rugs: (field, motif, binding, accent)
RUG_COLOURS = {
    "charcoal": ((74, 74, 78), (112, 112, 116), (40, 40, 44), (200, 190, 168)),
    "navy": ((40, 54, 94), (176, 168, 140), (26, 34, 60), (200, 186, 150)),
    "oatmeal": ((204, 190, 164), (168, 146, 116), (116, 86, 60), (150, 64, 48)),
    "red": ((140, 36, 40), (198, 162, 92), (40, 44, 70), (206, 176, 110)),
}

TEXTURES = {
    "bed_sheet": lambda: R.fabric((238, 238, 234), 501),
    "bed_pillow": lambda: R.fabric((246, 245, 240), 502),
    "bed_ticking": lambda: ticking(503),
    "mirror_glass": mirror_glass,
    "brass": lambda: K.brushed((196, 160, 84), 504),
    "louvre_white": lambda: louvre((236, 236, 232), 505),
    "nursery_gingham": lambda: gingham(506),
    "changing_pad": lambda: R.flat((196, 228, 212), 507, grain=2),
    "blanket_knit": lambda: R.fabric((226, 206, 214), 508, heather=0.06),
}
for _i, (_fid, _base) in enumerate(sorted(FABRIC_BASE.items())):
    TEXTURES["duvet_" + _fid] = (lambda b=_base, s=510 + _i: duvet(b, s))
    TEXTURES["tufted_" + _fid] = (lambda b=_base, s=520 + _i: tufted(R.shade(b, 0.9), s))
for _i, (_cid, (_f, _m, _b, _a)) in enumerate(sorted(RUG_COLOURS.items())):
    TEXTURES["rug_" + _cid] = (lambda f=_f, m=_m, s=530 + _i: rug_field(f, m, s))
    TEXTURES["rug_border_" + _cid] = (lambda f=_f, b=_b, a=_a, s=540 + _i: rug_border(b, a, f, s))

DEFAULT_TEX = dict(K.DEFAULT_TEX)
DEFAULT_TEX.update({
    "handle": T("brass"), "brass": T("brass"), "sheet": T("bed_sheet"),
    "pillow": T("bed_pillow"), "mattress": T("bed_ticking"), "duvet": T("duvet_navy"),
    "tufted": T("tufted_navy"), "mirror": T("mirror_glass"), "louvre": T("louvre_white"),
    "nursery": T("nursery_gingham"), "pad": T("changing_pad"), "blanket": T("blanket_knit"),
    "rug": T("rug_navy"), "rug_border": T("rug_border_navy"),
})


def geometry(specs, particle, display=None):
    tex = {k: DEFAULT_TEX[k] for k in sorted(R.used_keys(specs) | {particle})}
    tex["particle"] = "#" + particle
    out = {"parent": "block/block", "textures": tex, "elements": [R.build(s) for s in specs]}
    if display:
        out["display"] = display
    return out


# ------------------------------------------------------------------------------------------
# Moving, mirroring and cutting whole pieces
# ------------------------------------------------------------------------------------------
def shift(specs, dx=0.0, dy=0.0, dz=0.0):
    out = []
    for s in specs:
        n = copy.deepcopy(s)
        n["from"] = [n["from"][0] + dx, n["from"][1] + dy, n["from"][2] + dz]
        n["to"] = [n["to"][0] + dx, n["to"][1] + dy, n["to"][2] + dz]
        n["uv"] = {}
        if n["rot"]:
            axis, angle, origin = n["rot"]
            n["rot"] = (axis, angle, [origin[0] + dx, origin[1] + dy, origin[2] + dz])
        out.append(n)
    return out


def mirror_about(specs, cx):
    """Reflected through the plane x = cx."""
    return [R._remap(s, lambda x, y, z: (2 * cx - x, y, z), {"east": "west", "west": "east"})
            for s in specs]


LO_FACE = {0: "west", 1: "down", 2: "north"}
HI_FACE = {0: "east", 1: "up", 2: "south"}


def clip(specs, axis, lo, hi):
    """The part of each element between lo and hi along axis; the faces on a cut are dropped,
    being inside the piece. A turned element is not cut: it goes whole to the cell its middle
    is in."""
    out = []
    for s in specs:
        a0, a1 = s["from"][axis], s["to"][axis]
        if s["rot"]:
            if lo <= (a0 + a1) / 2 < hi:
                out.append(s)
            continue
        if a1 <= lo or a0 >= hi:
            continue
        n = copy.deepcopy(s)
        faces = list(s["faces"])
        if a0 < lo:
            n["from"][axis] = lo
            faces = [f for f in faces if f != LO_FACE[axis]]
        if a1 > hi:
            n["to"][axis] = hi
            faces = [f for f in faces if f != HI_FACE[axis]]
        if (a0, a1) != (n["from"][axis], n["to"][axis]):
            n["uv"] = {}
        n["faces"] = tuple(faces)
        if faces:
            out.append(n)
    return out


def cut_cells(specs, cells):
    """Each cell's share of a whole piece, moved into the cell's own 0..16."""
    out = []
    for cx, cy, cz in cells:
        part = specs
        for axis, c in ((0, cx), (1, cy), (2, cz)):
            part = clip(part, axis, 16 * c, 16 * c + 16)
        out.append(shift(part, -16 * cx, -16 * cy, -16 * cz))
    return out


def extent(specs):
    lo = [min(s["from"][i] for s in specs) for i in range(3)]
    hi = [max(s["to"][i] for s in specs) for i in range(3)]
    return lo, hi


def big_display(specs):
    """Item transforms for a piece bigger than a block, drawn centred on the block: scaled so
    its width seen from the inventory's corner view (or its height, if taller) fits a slot as a
    block does."""
    lo, hi = extent(specs)
    w = (hi[0] - lo[0] + hi[2] - lo[2]) * 0.7071
    h = (hi[1] - lo[1]) * 1.25
    s = round(min(0.625, 0.625 * 22.6 / max(w, h, 1e-6)), 3)
    cy = (lo[1] + hi[1]) / 2 - 8
    ty = round(-cy * s, 2)
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, ty, 0], "scale": [s, s, s]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0],
                   "scale": [s * 0.5, s * 0.5, s * 0.5]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, ty, 0], "scale": [s, s, s]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [s * 0.6, s * 0.6, s * 0.6]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                  "scale": [s * 0.7, s * 0.7, s * 0.7]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                 "scale": [s * 0.7, s * 0.7, s * 0.7]},
    }


def centred(specs):
    """The piece moved so its footprint is centred on the block, for its item."""
    lo, hi = extent(specs)
    return shift(specs, 8 - (lo[0] + hi[0]) / 2, 0, 8 - (lo[2] + hi[2]) / 2)


# ------------------------------------------------------------------------------------------
# Beds, each drawn whole: foot at z 0, head (against the wall) at z 32
# ------------------------------------------------------------------------------------------
def bedding(x0, x1, top, pillows, fold_z=18.0, duvet_to=21.0, foot=0.5):
    """A mattress in ticking under a sheet, a quilted duvet over its foot two thirds, turned
    down in white, and pillows at the head. The mattress's top is at `top`; x0..x1 is its
    width."""
    base = top - 3.5
    return ([el([x0, base, 1.5], [x1, top, 30.5], "mattress", ("north", "east", "west", "up"),
                {"up": "sheet"}),
             el([x0 - 0.5, top - 2.5, foot], [x1 + 0.5, top + 0.6, duvet_to], "duvet"),
             el([x0 - 0.5, top + 0.6, fold_z], [x1 + 0.5, top + 1.0, duvet_to + 0.5], "sheet",
                ("north", "south", "east", "west", "up"))]
            + [el([a, top, 24.5], [b, top + 2.25, 29.75], "pillow") for a, b in pillows])


def upholstered_bed(x0, x1, head_top, pillows, tufted_head=False, accents=()):
    """A bed on an upholstered base with walnut feet and a padded headboard."""
    out = [el([x0 + 0.25, 2.5, 0.75], [x1 - 0.25, 5.5, 30.5], "fabric_dark",
              ("north", "east", "west", "up", "down"))]
    for lx in (x0 + 0.75, x1 - 2.25):
        for lz in (1.25, 28.75):
            out.append(leg([lx, 0, lz], [lx + 1.5, 2.5, lz + 1.5], tex="leg"))
    out += bedding(x0 + 0.5, x1 - 0.5, 9, pillows)
    out += [el([a, 9, 22.5], [b, 11.75, 24.25], "fabric") for a, b in accents]
    face = "tufted" if tufted_head else "fabric"
    out += [el([x0, 2.5, 30.5], [x1, head_top, 32], "fabric_dark", ALL, {"north": face}),
            el([x0 - 0.01, head_top - 0.75, 30.25], [x1 + 0.01, head_top, 30.5], "fabric_dark",
               ("north", "east", "west", "up"))]
    return out


BED_SINGLE = upholstered_bed(0.5, 15.5, 18, [(2.5, 13.5)])
BED_DOUBLE = upholstered_bed(-12, 12, 20, [(-10.5, -1), (1, 10.5)])
BED_KING = upholstered_bed(-15.75, 15.75, 24, [(-14, -1), (1, 14)], tufted_head=True,
                           accents=[(-9, -2.5), (2.5, 9)])

# --- the bunk bed: a single bed below another, on four corner posts, with a ladder at the
# foot, a guard rail round the top bunk, and a panel at each head ---------------------------
POSTS = [leg([x, 0, z], [x + 1.5, 28, z + 1.5], faces=ALL)
         for x in (0.5, 14) for z in (0.5, 30)]


def bunk_tier(y, top, head_to, foot_panel):
    """One tier: the deck's rails at y, a mattress whose top is at `top`, bedding, and the
    panels at its head (to head_to) and, if foot_panel, its foot."""
    out = [board([2, y, 0.75], [14, y + 2, 1.5], faces=NO_BACK + ("south",)),
           board([2, y, 30.5], [14, y + 2, 31.25], faces=NO_BACK + ("south",)),
           board([0.75, y, 2], [1.75, y + 2, 30], grain="h"),
           board([14.25, y, 2], [15.25, y + 2, 30], grain="h"),
           board([1.75, y + 0.5, 1.5], [14.25, y + 1, 30.5], faces=("up", "down"))]
    out += bedding(2, 14, top, [(3.5, 12.5)], fold_z=17.5, duvet_to=20.5, foot=1.6)
    out.append(board([2, y + 2, 30.75], [14, head_to, 31.25], grain="v"))
    if foot_panel:
        out.append(board([2, y + 2, 0.75], [14, y + 5, 1.25], grain="v"))
    return out


BUNK = (POSTS + bunk_tier(2.5, 6, 13, True) + bunk_tier(17.5, 21, 27, False)
        # the guard rail round the top bunk, along both sides, with two balusters each
        + [board([0.75, 25, 2], [1.75, 26, 30], grain="h"),
           board([14.25, 25, 2], [15.25, 26, 30], grain="h")]
        + [leg([x, 19.5, z], [x + 0.75, 25, z + 0.75], faces=("north", "south", "east", "west"))
           for x in (0.875, 14.375) for z in (10.5, 20.5)]
        # the ladder: rungs across the foot between the posts
        + [leg([2, y, 0.8], [14, y + 0.75, 1.45], faces=("north", "south", "up", "down"))
           for y in (10.5, 14.5, 23)])

# --- the day bed: two blocks wide, one deep, its back to the wall and an arm at each end ------
DAY_ARM = [board([14.25, 2.5, 1], [15.75, 11.5, 15], grain="v"),
           board([14, 11.5, 0.75], [16, 12.25, 15.25]),
           leg([14.25, 0, 1], [15.75, 2.5, 2.5]),
           leg([14.25, 0, 13.5], [15.75, 2.5, 15])]
DAY_BED = (DAY_ARM + mirror_about(DAY_ARM, 0)
           + [board([-14.25, 2.5, 0.75], [14.25, 4.5, 1.5], grain="h"),
              board([-14.25, 3, 1.5], [14.25, 3.5, 14.25], faces=("up", "down")),
              # the back: a bottom rail, spindles and a top rail
              board([-14.25, 2.5, 14.25], [14.25, 7, 15], grain="h"),
              board([-16, 14.25, 13.75], [16, 15.25, 15.5]),
              el([-14, 4, 1.5], [14, 7, 14.25], "fabric", ("north", "up")),
              el([-13, 7, 11.25], [-0.5, 11.75, 14], "fabric_dark"),
              el([0.5, 7, 11.25], [13, 11.75, 14], "fabric_dark"),
              el([9, 7, 4], [13.5, 9.25, 9.5], "pillow")]
           + [leg([x, 7, 14.4], [x + 1, 14.25, 14.9], faces=("north", "south", "east", "west"))
              for x in (-12.5 + 3 * k for k in range(9))])

# ------------------------------------------------------------------------------------------
# Case pieces
# ------------------------------------------------------------------------------------------
# --- nightstand: 0.69 m wide, its top at 0.56 m, a drawer over an open shelf --------------
NIGHTSTAND = ([leg([x, 0, z], [x + 1.25, 1.5, z + 1.25]) for x in (2.75, 12) for z in (4.25, 14)]
              + [board([2.5, 1.5, 4], [3.25, 8.25, 15.5], grain="v"),
                 board([12.75, 1.5, 4], [13.5, 8.25, 15.5], grain="v"),
                 board([3.25, 1.5, 15], [12.75, 8.25, 15.5], grain="v", faces=("north", "south")),
                 board([3.25, 1.5, 4], [12.75, 2.25, 15], faces=("north", "up", "down")),
                 board([3.25, 5, 4.25], [12.75, 5.5, 15], faces=("north", "up", "down")),
                 board([2.25, 8.25, 3.5], [13.75, 9, 15.75])]
              + shaker(3.25, 12.75, 5.5, 8.25, 3.5, grain="h", frame=0.75)
              + [el([7.5, 6.5, 3.0], [8.5, 7.25, 3.5], "brass")]
              # a book lying on the shelf
              + [el([5, 2.25, 6.5], [10, 3, 12.5], "books", ("north", "east", "west"),
                    {"north": "books_b"}),
                 el([5, 3, 6.5], [10, 3.01, 12.5], "wood", ("up",), {"up": "books"})])

# --- dresser: 0.94 m wide, its top at 0.88 m, six drawers ----------------------------------
DRESSER = ([leg([x, 0, z], [x + 1.25, 2, z + 1.25]) for x in (0.75, 14) for z in (4.75, 14.25)]
           + [board([0.5, 2, 4.5], [1.25, 13.25, 15.75], grain="v"),
              board([14.75, 2, 4.5], [15.5, 13.25, 15.75], grain="v"),
              board([1.25, 2, 15], [14.75, 13.25, 15.75], grain="v", faces=("north", "south")),
              board([1.25, 2, 4.5], [14.75, 2.5, 15], faces=("north", "down")),
              board([0.25, 13.25, 4], [15.75, 14, 16])])
for _y0, _y1 in ((2.75, 6.25), (6.5, 9.75), (10, 13)):
    for _x0, _x1 in ((1.25, 7.9), (8.1, 14.75)):
        DRESSER += shaker(_x0, _x1, _y0, _y1, 4.0, grain="h", frame=0.75)
        DRESSER += bar_h((_x0 + _x1) / 2, (_y0 + _y1) / 2 - 0.2, 4.0, w=2.5)


def framed_mirror(x0, x1, y0, y1, z0, z1, frame=1.0):
    """A mirror in a wooden frame standing at z0..z1, its glass facing north."""
    f = frame
    zg = z0 + 0.5
    return [board([x0, y0, z0], [x0 + f, y1, z1], grain="v"),
            board([x1 - f, y0, z0], [x1, y1, z1], grain="v"),
            board([x0 + f, y1 - f, z0], [x1 - f, y1, z1]),
            board([x0 + f, y0, z0], [x1 - f, y0 + f, z1]),
            el([x0 + f, y0 + f, zg], [x1 - f, y1 - f, zg + 0.25], "mirror", ("north",)),
            board([x0 + f, y0 + f, zg + 0.25], [x1 - f, y1 - f, z1], faces=("south",))]


DRESSER_MIRROR = (DRESSER + framed_mirror(2.5, 13.5, 14, 29, 14.25, 15.5)
                  + [board([3.5, 14, 13.5], [4.5, 14.75, 15.5]),
                     board([11.5, 14, 13.5], [12.5, 14.75, 15.5])])

# --- wardrobe: 0.94 x 0.88 m and 1.88 m tall, two full-height doors ----------------------
WARDROBE = ([el([1, 0, 3], [15, 1.5, 15.5], "edge", ("north", "east", "west")),
             board([0.5, 1.5, 2.5], [1.25, 29, 16], grain="v"),
             board([14.75, 1.5, 2.5], [15.5, 29, 16], grain="v"),
             board([1.25, 1.5, 15.25], [14.75, 29, 16], grain="v", faces=("north", "south")),
             board([1.25, 1.5, 2.5], [14.75, 2, 15.25], faces=("north", "down")),
             board([0.25, 29, 2], [15.75, 30, 16]),
             board([0.5, 28.5, 2.25], [15.5, 29, 2.5], faces=("north", "east", "west", "down"))]
            + shaker(1.25, 7.95, 1.75, 15, 2.0) + shaker(1.25, 7.95, 15, 28.5, 2.0)
            + shaker(8.05, 14.75, 1.75, 15, 2.0) + shaker(8.05, 14.75, 15, 28.5, 2.0)
            + bar_v(6.8, 13, 18, 2.0) + bar_v(8.8, 13, 18, 2.0))

# --- the built-in closet: floor to 2 m, two louvred doors a block --------------------------


def louvred_leaf(x0, x1, y0, y1, zf, f=0.75):
    """A louvred door leaf: a frame of stiles and rails, the slats set a quarter pixel back."""
    fr = ("north", "east", "west", "up", "down")
    return [el([x0 + f, y0 + f, zf + 0.25], [x1 - f, y1 - f, zf + 0.5], "louvre", ("north",)),
            board([x0, y0, zf], [x0 + f, y1, zf + 0.5], grain="v", faces=fr),
            board([x1 - f, y0, zf], [x1, y1, zf + 0.5], grain="v", faces=fr),
            board([x0 + f, y0, zf], [x1 - f, y0 + f, zf + 0.5], faces=fr),
            board([x0 + f, y1 - f, zf], [x1 - f, y1, zf + 0.5], faces=fr),
            board([x0 + f, (y0 + y1) / 2 - 0.5, zf], [x1 - f, (y0 + y1) / 2 + 0.5, zf + 0.5],
                  faces=fr)]


CLOSET_BODY = ([board([0, 0, 15.5], [16, 32, 16], grain="v", faces=("north", "south")),
                board([0, 29, 3], [16, 32, 4.25], faces=("north", "up", "down")),
                board([0, 29, 4.25], [16, 32, 15.5], faces=("up",)),
                el([0, 0, 4], [16, 1.5, 4.5], "edge", ("north", "up"))]
               + louvred_leaf(0.25, 7.95, 1.5, 29, 3.75) + louvred_leaf(8.05, 15.75, 1.5, 29, 3.75)
               + [el([7.1, 14.5, 3.25], [7.6, 15.5, 3.75], "brass"),
                  el([8.4, 14.5, 3.25], [8.9, 15.5, 3.75], "brass")])
CLOSET_END = [board([0, 0, 3], [0.5, 32, 16], grain="v", faces=("west", "north", "up", "down"))]

# --- blanket chest: 0.88 x 0.56 m, its lid at 0.44 m -------------------------------------
CHEST = ([leg([x, 0, z], [x + 1.5, 1.25, z + 1.5]) for x in (1.25, 13.25) for z in (4.25, 11.25)]
         + [el([1.25, 1.25, 4.5], [14.75, 5.75, 12.75], "wood", ("east", "west", "south",
                                                                  "down")),
            board([1.25, 1.25, 4.25], [14.75, 5.75, 4.5], faces=("north",))]
         + shaker(1.75, 14.25, 1.75, 5.25, 3.75, grain="h", frame=0.75)
         + [board([1, 5.75, 4], [15, 7, 13]),
            board([1.25, 5.5, 3.75], [14.75, 5.75, 4], faces=("north", "down")),
            el([0.75, 3.75, 7.5], [1.25, 4.25, 9.75], "handle"),
            el([14.75, 3.75, 7.5], [15.25, 4.25, 9.75], "handle"),
            el([7.5, 4.5, 3.4], [8.5, 5.5, 3.75], "brass")])

# --- desk: its top at 0.75 m, a drawer under it, side panels only at the run's ends -------
DESK_BODY = ([board([0, 11.25, 3.5], [16, 12, 16]),
              board([0, 4.5, 14.75], [16, 11.25, 15.25], grain="v", faces=("north", "south",
                                                                             "down")),
              el([3, 9.25, 4.5], [13, 11.25, 14.75], "wood", ("east", "west", "down")),
              board([0, 9.5, 4], [3, 11.25, 4.5], faces=("north", "down")),
              board([13, 9.5, 4], [16, 11.25, 4.5], faces=("north", "down"))]
             + shaker(3, 13, 9.4, 11.15, 4.0, grain="h", frame=0.6)
             + bar_h(8, 10.1, 4.0, w=3.0))
DESK_END = [board([0, 0, 3.75], [1, 11.25, 15.75], grain="v")]

# --- desk chair: a swivel chair on four casters, upholstered seat and back ------------------
DESK_CHAIR = ([el([2.5, 1, 7.25], [13.5, 2, 8.75], "metal_black"),
               el([7.25, 1, 2.5], [8.75, 2, 13.5], "metal_black")]
              + [el([x, 0, z], [x + 1.5, 1, z + 1.5], "rubber")
                 for x, z in ((2.25, 7.25), (12.25, 7.25), (7.25, 2.25), (7.25, 12.25))]
              + [el([7.25, 2, 7.25], [8.75, 5.5, 8.75], "metal", ("north", "south", "east",
                                                                   "west")),
                 el([3.5, 5.5, 4], [12.5, 6, 12], "metal_black"),
                 el([3, 6, 3.5], [13, 8, 12.5], "fabric"),
                 el([7.25, 6, 12.5], [8.75, 9.5, 13.75], "metal_black"),
                 el([3.5, 9, 12.75], [12.5, 16, 14.25], "fabric", ALL, {"south": "fabric_dark"}),
                 el([2.5, 9.5, 5], [3.5, 10.25, 11], "metal_black"),
                 el([2.75, 8, 8], [3.25, 9.5, 9], "metal_black",
                    ("north", "south", "east", "west")),
                 el([12.5, 9.5, 5], [13.5, 10.25, 11], "metal_black"),
                 el([12.75, 8, 8], [13.25, 9.5, 9], "metal_black",
                    ("north", "south", "east", "west"))])


# ------------------------------------------------------------------------------------------
# Nursery
# ------------------------------------------------------------------------------------------
def slats(x0, x1, y0, y1, z0, z1, step=1.4, w=0.6):
    """Upright slats from x0 to x1, one every `step`."""
    out = []
    n = int((x1 - x0 - w) // step)
    pad = (x1 - x0 - w - n * step) / 2
    for i in range(n + 1):
        x = x0 + pad + i * step
        out.append(leg([x, y0, z0], [x + w, y1, z1], faces=("north", "south", "east", "west")))
    return out


CRIB = ([leg([x, 0, z], [x + 1.25, 14.5, z + 1.25], faces=ALL)
         for x in (0.5, 14.25) for z in (3, 12.75)]
        + [board([0.75, 3, 4.25], [1.5, 13.25, 12.75], grain="v"),
           board([14.5, 3, 4.25], [15.25, 13.25, 12.75], grain="v")]
        + [board([1.75, y, z], [14.25, y + 1, z + 0.75], grain="h")
           for y in (3, 12.5) for z in (3.25, 12.75)]
        + slats(1.75, 14.25, 4, 12.5, 3.4, 3.85) + slats(1.75, 14.25, 4, 12.5, 12.9, 13.35)
        + [board([1.75, 4.5, 4.25], [14.25, 5, 12.75], faces=("up", "down")),
           el([1.75, 5, 4.25], [14.25, 7, 12.75], "nursery", ("north", "south", "east", "west",
                                                              "up")),
           el([8.5, 7, 5], [13.5, 7.6, 12], "blanket")])

CRADLE_BASE = ([leg([x, 0, z], [x + 1.25, 1, z + 1.25]) for x in (1.25, 13.5) for z in (3.75, 12.25)]
               + [el([1.25, 1, 4], [14.75, 7, 13.25], "wood", ("east", "west", "south")),
                  board([1, 7, 3.5], [15, 7.5, 13.75])]
               + shaker(1.25, 7.9, 1.25, 6.75, 3.5, grain="h", frame=0.75)
               + shaker(8.1, 14.75, 1.25, 6.75, 3.5, grain="h", frame=0.75)
               + [el([4.1, 3.6, 3.0], [5.1, 4.4, 3.5], "brass"),
                  el([10.9, 3.6, 3.0], [11.9, 4.4, 3.5], "brass")])
CRADLE_BASKET = ([board([2, 7.5, 4.5], [2.75, 13.5, 12.5], grain="v"),
                  board([13.25, 7.5, 4.5], [14, 13.5, 12.5], grain="v")]
                 + [board([2.75, y, z], [13.25, y + 0.75, z + 0.75], grain="h")
                    for y in (7.5, 12.5) for z in (4.5, 11.75)]
                 + slats(2.75, 13.25, 8.25, 12.5, 4.6, 5.1, step=1.3)
                 + slats(2.75, 13.25, 8.25, 12.5, 11.9, 12.4, step=1.3)
                 + [el([2.75, 8, 5.25], [13.25, 9.5, 11.75], "nursery",
                       ("north", "south", "east", "west", "up"))])
CRADLE = CRADLE_BASE + CRADLE_BASKET

CHANGING = ([leg([x, 0, z], [x + 1.25, 13, z + 1.25]) for x in (0.5, 14.25) for z in (3, 13.75)]
            + [board([0.75, 1.5, 4.25], [1.5, 13, 13.75], grain="v"),
               board([14.5, 1.5, 4.25], [15.25, 13, 13.75], grain="v"),
               board([1.75, 1.5, 14.25], [14.25, 13, 14.75], grain="v", faces=("north", "south")),
               board([1.75, 1.5, 3.5], [14.25, 2, 14.25], faces=("north", "up", "down")),
               board([1.75, 7.25, 3.5], [14.25, 12.75, 14.25], faces=("down",)),
               board([0.25, 13, 2.75], [15.75, 13.75, 15.25])]
            + shaker(1.75, 14.25, 10.1, 12.75, 3.0, grain="h", frame=0.75)
            + shaker(1.75, 14.25, 7.25, 9.9, 3.0, grain="h", frame=0.75)
            + bar_h(8, 11.2, 3.0, w=3) + bar_h(8, 8.35, 3.0, w=3)
            # guard rails round the top, and the pad with its raised sides
            + [board([0.5, 13.75, 14.25], [15.5, 16, 15]),
               board([0.5, 13.75, 3.25], [1.25, 16, 14.25], grain="h"),
               board([14.75, 13.75, 3.25], [15.5, 16, 14.25], grain="h"),
               el([1.5, 13.75, 3.75], [14.5, 14.75, 14], "pad"),
               el([1.5, 14.75, 3.75], [14.5, 15.5, 5], "pad"),
               el([1.5, 14.75, 12.75], [14.5, 15.5, 14], "pad"),
               # folded towels on the shelf
               el([9, 2, 6], [13.5, 3.25, 12], "sheet"),
               el([9.25, 3.25, 6.25], [13.25, 4.25, 11.75], "blanket")])

# --- rocking chair: spindle back leaning 22.5 degrees, arms, on two rockers ---------------
_ROCKER = [el([3, 0, 4.5], [4, 1.25, 11.5], "wood"),
           el([3, 0.25, 1], [4, 1.5, 4.75], "wood", rot=("x", -22.5, [3.5, 0.75, 4.75])),
           el([3, 0.25, 11.25], [4, 1.5, 15], "wood", rot=("x", 22.5, [3.5, 0.75, 11.25]))]
_BACK_ORIGIN = [8, 7, 10.75]
_BACK = ([leg([x, 7, 10.25], [x + 1, 16, 11.25], faces=ALL, tex="wood_v") for x in (3.25, 11.75)]
         + [board([4.25, 14, 10.4], [11.75, 15.5, 11.1])]
         + [leg([x, 7.5, 10.55], [x + 0.6, 14, 10.95], faces=("north", "south", "east", "west"))
            for x in (5.3, 6.9, 8.5, 10.1)])
for _s in _BACK:
    _s["rot"] = ("x", 22.5, _BACK_ORIGIN)
ROCKING_CHAIR = (_ROCKER + mirror_x(_ROCKER[:1])
                 + [el([12, 0.25, 1], [13, 1.5, 4.75], "wood", rot=("x", -22.5, [12.5, 0.75, 4.75])),
                    el([12, 0.25, 11.25], [13, 1.5, 15], "wood",
                       rot=("x", 22.5, [12.5, 0.75, 11.25]))]
                 + [leg([x, 1.25, z], [x + 1, 6.5, z + 1]) for x in (3.25, 11.75) for z in (4.75, 10)]
                 + [board([3, 6.5, 3.75], [13, 7.5, 11.75]),
                    board([2.75, 10, 4], [4.25, 10.75, 11]),
                    board([11.75, 10, 4], [13.25, 10.75, 11]),
                    leg([3.25, 7.5, 4.75], [4, 10, 5.5]),
                    leg([12, 7.5, 4.75], [12.75, 10, 5.5]),
                    board([4.25, 3.5, 4.75], [11.75, 4, 5.25], faces=("north", "south", "up",
                                                                      "down"))]
                 + _BACK)

# --- standing mirror: a cheval glass 1.6 m tall on two feet --------------------------------
STANDING_MIRROR = ([board([3, 0, 5], [4.5, 1, 11]), board([11.5, 0, 5], [13, 1, 11]),
                    leg([3.25, 1, 7.5], [4.25, 26, 8.5], faces=ALL),
                    leg([11.75, 1, 7.5], [12.75, 26, 8.5], faces=ALL)]
                   + framed_mirror(4.25, 11.75, 3, 28, 7.25, 8.75, frame=0.75))

# --- vanity: a dressing table with a drawer, its mirror standing at the back ---------------
VANITY = ([leg([x, 0, z], [x + 1, 11.25, z + 1]) for x in (1, 14) for z in (5.5, 14.25)]
          + [board([0.5, 11.25, 5], [15.5, 12, 15.75]),
             board([2, 9.5, 5.75], [4.5, 11.25, 6.25], faces=("north", "down")),
             board([11.5, 9.5, 5.75], [14, 11.25, 6.25], faces=("north", "down")),
             board([1.25, 9.5, 6.5], [1.75, 11.25, 14.25], grain="h", faces=("east", "west",
                                                                             "down")),
             board([14.25, 9.5, 6.5], [14.75, 11.25, 14.25], grain="h", faces=("east", "west",
                                                                               "down")),
             board([2, 9.5, 14.25], [14, 11.25, 14.75], faces=("north", "south", "down")),
             el([4.5, 9.4, 6.25], [11.5, 11.25, 14.25], "wood", ("east", "west", "down"))]
          + shaker(4.5, 11.5, 9.4, 11.15, 5.25, grain="h", frame=0.5)
          + [el([7.5, 9.9, 4.75], [8.5, 10.6, 5.25], "brass")]
          + framed_mirror(3, 13, 12, 26, 13.75, 15)
          + [board([4, 12, 13], [5, 12.75, 15]), board([11, 12, 13], [12, 12.75, 15]),
             # a scent bottle and a jar on the top
             el([10.5, 12, 8], [11.5, 13.75, 9], "mirror"),
             el([10.75, 13.75, 8.25], [11.25, 14.25, 8.75], "brass"),
             el([4, 12, 7.5], [5.5, 13, 9], "pillow")])

# --- vanity stool: an octagonal cushion on four legs --------------------------------------
VANITY_STOOL = ([leg([x, 0, z], [x + 1, 6.5, z + 1]) for x in (5, 10) for z in (5, 10)]
                + R.octagon(8, 8, 3.75, 6.5, 8.25, "fabric", side="fabric_dark"))

# --- rug: a sixteenth thick, its bound border three texels deep on an open side -----------
RUG_FIELD = [el([0, 0, 0], [16, 0.75, 16], "rug", ("up",))]
RUG_SIDE = [el([3, 0, 0], [13, 1, 3], "rug_border", ("up", "north"))]
RUG_CORNER = [el([0, 0, 0], [3, 1, 3], "rug_border", ("up", "north", "west"))]


def rug_rules():
    rules = [("field", {}, 0)]
    for side, r in ROT.items():
        rules.append(("side", {side: "false"}, r))
    for (d1, d2), r in ((("north", "west"), 0), (("east", "north"), 90),
                        (("south", "east"), 180), (("west", "south"), 270)):
        rules.append(("corner", {"OR": [{d1: "false"}, {d2: "false"}]}, r))
    return rules


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
WOODS = R.WOODS
FABRICS = [(fid, dict(ftex, duvet=T("duvet_" + fid), tufted=T("tufted_" + fid)), *names)
           for fid, ftex, *names in R.FABRICS]
OATMEAL_CUSHION = {"fabric": T("oatmeal"), "fabric_dark": T("oatmeal_dark")}
RUGS = [(cid, {"rug": T("rug_" + cid), "rug_border": T("rug_border_" + cid)}, *names)
        for cid, _t, *names in R.FABRICS]

DRAWERS = "FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE"
DOORS = "FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE"


def jbox(box):
    return ", ".join(R._num(v) for v in box)


# Beds: (piece, kind, layout, cells, geometry, per-cell boxes, extra textures, names)
BED_CELLS = {
    "SINGLE": [(0, 0, 0), (0, 0, 1)],
    "WIDE": [(0, 0, 0), (0, 0, 1), (-1, 0, 0), (-1, 0, 1)],
    "BUNK": [(0, 0, 0), (0, 0, 1), (0, 1, 0), (0, 1, 1)],
    "DAY": [(0, 0, 0), (-1, 0, 0)],
}
BEDS = [
    ("bed_single", "fabric", "SINGLE", BED_SINGLE, [[0, 0, 0, 16, 9, 16]] * 2, {},
     ("Single Bed", "Einzelbett", "Cama individual", "Enkelsäng")),
    ("bed_double", "fabric", "WIDE", BED_DOUBLE,
     [[0, 0, 0, 12, 9, 16]] * 2 + [[4, 0, 0, 16, 9, 16]] * 2, {},
     ("Double Bed", "Doppelbett", "Cama doble", "Dubbelsäng")),
    ("bed_king", "fabric", "WIDE", BED_KING, [[0, 0, 0, 16, 9, 16]] * 4, {},
     ("King Bed", "Kingsize-Bett", "Cama king size", "Kingsize-säng")),
    ("day_bed", "wood", "DAY", DAY_BED, [[0, 0, 1, 16, 7, 15]] * 2, OATMEAL_CUSHION,
     ("Day Bed", "Tagesbett", "Diván cama", "Dagbädd")),
    ("bunk_bed", "wood", "BUNK", BUNK, [[0, 0, 0, 16, 6, 16]] * 2 + [[0, 1, 0, 16, 5, 16]] * 2, {},
     ("Bunk Bed", "Etagenbett", "Litera", "Våningssäng")),
]

# Two-block pieces: (piece, kind, geometry, box to 32, slots, sounds, names)
TALL = [
    ("dresser_mirror", "wood", DRESSER_MIRROR, [0, 0, 4, 16, 29, 16], 27, DRAWERS,
     ("Dresser with Mirror", "Kommode mit Spiegel", "Cómoda con espejo", "Byrå med spegel")),
    ("wardrobe", "wood", WARDROBE, [0, 0, 2, 16, 30, 16], 27, DOORS,
     ("Wardrobe", "Kleiderschrank", "Armario ropero", "Garderob")),
    ("standing_mirror", "wood", STANDING_MIRROR, [3, 0, 5, 13, 28, 11], 0, None,
     ("Standing Mirror", "Standspiegel", "Espejo de pie", "Golvspegel")),
    ("vanity", "wood", VANITY, [0, 0, 5, 16, 26, 16], 9, DRAWERS,
     ("Vanity Table", "Schminktisch", "Tocador", "Sminkbord")),
]

# One-block pieces that store things, standing alone: (piece, geometry, box, slots, sounds, names)
STORAGE = [
    ("nightstand", NIGHTSTAND, [2, 0, 3, 14, 9, 16], 9, DRAWERS,
     ("Nightstand", "Nachttisch", "Mesita de noche", "Nattduksbord")),
    ("dresser", DRESSER, [0, 0, 4, 16, 14, 16], 27, DRAWERS,
     ("Dresser", "Kommode", "Cómoda", "Byrå")),
    ("blanket_chest", CHEST, [1, 0, 4, 15, 7, 13], 18, DOORS,
     ("Blanket Chest", "Wäschetruhe", "Baúl", "Förvaringskista")),
    ("cradle_with_drawers", CRADLE, [1, 0, 3, 15, 13, 14], 9, DRAWERS,
     ("Cradle with Drawers", "Wiege mit Schubladen", "Moisés con cajones", "Vagga med lådor")),
    ("changing_table", CHANGING, [0, 0, 3, 16, 15, 15], 9, DRAWERS,
     ("Changing Table", "Wickeltisch", "Cambiador", "Skötbord")),
]

# Plain pieces: (piece, kind, geometry, box, seat (top, forward, left) or None, extra, names)
SINGLES = [
    ("desk_chair", "fabric", DESK_CHAIR, [3, 0, 3, 13, 16, 14], (8, 0.5, 0), {},
     ("Desk Chair", "Schreibtischstuhl", "Silla de escritorio", "Skrivbordsstol")),
    ("crib", "wood", CRIB, [0, 0, 3, 16, 14, 14], None, {},
     ("Crib", "Gitterbett", "Cuna", "Spjälsäng")),
    ("rocking_chair", "wood", ROCKING_CHAIR, [2, 0, 1, 14, 15, 15], (7.5, 0.5, 0), {},
     ("Rocking Chair", "Schaukelstuhl", "Mecedora", "Gungstol")),
    ("vanity_stool", "wood", VANITY_STOOL, [4, 0, 4, 12, 8, 12], (8.25, 0, 0), OATMEAL_CUSHION,
     ("Vanity Stool", "Schminkhocker", "Taburete de tocador", "Sminkpall")),
]

DESK_NAMES = ("Desk", "Schreibtisch", "Escritorio", "Skrivbord")
CLOSET_NAMES = ("Closet", "Einbauschrank", "Armario empotrado", "Inbyggd garderob")
RUG_NAMES = ("Rug", "Teppich", "Alfombra", "Matta")

# The order the pieces appear in the tab.
ORDER = ["bed_single", "bed_double", "bed_king", "day_bed", "bunk_bed", "nightstand", "dresser",
         "dresser_mirror", "wardrobe", "closet", "blanket_chest", "desk", "desk_chair",
         "standing_mirror", "vanity", "vanity_stool", "crib", "cradle_with_drawers",
         "changing_table", "rocking_chair", "rug"]


def names_with(names, fnames):
    return ["%s (%s)" % (n, f) for n, f in zip(names, fnames)]


def finishes(kind):
    return WOODS if kind == "wood" else FABRICS


def entries():
    """Every block: (registry, kind, piece, finish textures, names, java)."""
    by_piece = {}
    for piece, kind, layout, _geo, boxes, extra, names in BEDS:
        rows = []
        for fid, ftex, *fnames in finishes(kind):
            reg = "%s_%s" % (piece, fid)
            java = ('new BlockResidentialBed("%s", BedLayout.%s, new int[][]{%s}, %s)'
                    % (reg, layout, ", ".join("{%s}" % jbox(b) for b in boxes),
                       "true" if kind == "fabric" else "false"))
            rows.append((reg, "bed", piece, dict(ftex, **extra), names_with(names, fnames), java))
        by_piece[piece] = rows
    for piece, kind, _geo, box, slots, sounds, names in TALL:
        rows = []
        for fid, ftex, *fnames in finishes(kind):
            reg = "%s_%s" % (piece, fid)
            java = ('new BlockResidentialTall("%s", new int[]{%s}, false, %d, %s)'
                    % (reg, jbox(box), slots, sounds or "null, null"))
            rows.append((reg, "tall", piece, dict(ftex), names_with(names, fnames), java))
        by_piece[piece] = rows
    for piece, _geo, box, slots, sounds, names in STORAGE:
        rows = []
        for fid, ftex, *fnames in WOODS:
            reg = "%s_%s" % (piece, fid)
            java = ('new BlockResidentialStorage("%s", new int[]{%s}, %d, %s)'
                    % (reg, jbox(box), slots, sounds))
            rows.append((reg, "storage", piece, dict(ftex), names_with(names, fnames), java))
        by_piece[piece] = rows
    for piece, kind, _geo, box, seat, extra, names in SINGLES:
        rows = []
        for fid, ftex, *fnames in finishes(kind):
            reg = "%s_%s" % (piece, fid)
            up = "true" if kind == "fabric" else "false"
            if seat:
                java = ('new BlockResidentialFurniture("%s", new int[]{%s}, %s, %s, %s, %s)'
                        % (reg, jbox(box), up, R._num(seat[0]), R._num(seat[1]),
                           R._num(seat[2])))
            else:
                java = 'new BlockResidentialFurniture("%s", new int[]{%s}, %s)' % (reg, jbox(box),
                                                                                   up)
            rows.append((reg, "single", piece, dict(ftex, **extra), names_with(names, fnames),
                         java))
        by_piece[piece] = rows
    by_piece["desk"] = [
        ("desk_%s" % fid, "desk", "desk", dict(ftex), names_with(DESK_NAMES, fnames),
         'new BlockResidentialStorage("desk_%s", new int[]{0, 0, 3, 16, 12, 16}, 9, %s)'
         % (fid, DRAWERS))
        for fid, ftex, *fnames in WOODS]
    fid, ftex, *fnames = [w for w in WOODS if w[0] == "white"][0]
    by_piece["closet"] = [
        ("closet_white", "closet", "closet", dict(ftex, louvre=T("louvre_white")),
         names_with(CLOSET_NAMES, fnames),
         'new BlockCloset("closet_white", new int[]{0, 0, 3, 16, 32, 16}, 27, %s)' % DOORS)]
    by_piece["rug"] = [
        ("rug_%s" % cid, "rug", "rug", dict(rtex), names_with(RUG_NAMES, rnames),
         'new BlockRug("rug_%s")' % cid)
        for cid, rtex, *rnames in RUGS]
    out = []
    for piece in ORDER:
        out += by_piece[piece]
    return out


# ------------------------------------------------------------------------------------------
# Output
# ------------------------------------------------------------------------------------------
def cell_state(reg, n):
    variants = {}
    for f, r in ROT.items():
        for p in range(n):
            v = {"model": MODEL + "%s_cell%d" % (reg, p)}
            if r:
                v["y"] = r
            variants["facing=%s,part=%d" % (f, p)] = v
    return {"variants": variants}


def multipart_state(reg, rules):
    parts = []
    for part, when, r in rules:
        apply = {"model": MODEL + "%s_%s" % (reg, part)}
        if r:
            apply["y"] = r
        parts.append({"when": when, "apply": apply} if when else {"apply": apply})
    return {"multipart": parts}


def closet_rules():
    rules = []
    for half in ("lower", "upper"):
        up = "true" if half == "upper" else "false"
        rules += R.faced([("body_" + half, {"upper": up}),
                          ("left_" + half, {"upper": up, "left": "false"}),
                          ("right_" + half, {"upper": up, "right": "false"})])
    return rules


def split_halves(geo):
    return K.split_y(copy.deepcopy(geo))


def base_models():
    """Every base geometry model: (name, geometry json)."""
    out = []
    for piece, kind, layout, geo, _b, _e, _n in BEDS:
        particle = "fabric_dark" if kind == "fabric" else "wood"
        for i, cell in enumerate(cut_cells(geo, BED_CELLS[layout])):
            out.append(("%s_cell%d" % (piece, i), geometry(cell, particle)))
        item = centred(geo)
        out.append(("%s_item" % piece, geometry(item, particle, big_display(item))))
    for piece, _k, geo, *_ in TALL:
        lower, upper = split_halves(geo)
        out.append(("%s_lower" % piece, geometry(lower, "wood")))
        out.append(("%s_upper" % piece, geometry(upper, "wood")))
        out.append(("%s_item" % piece, geometry(geo, "wood", big_display(geo))))
    for piece, geo, *_ in STORAGE:
        out.append(("%s_body" % piece, geometry(geo, "wood")))
        out.append(("%s_item" % piece, geometry(geo, "wood")))
    for piece, kind, geo, *_ in SINGLES:
        out.append((piece, geometry(geo, "wood" if kind == "wood" else "fabric")))
    out.append(("desk_body", geometry(DESK_BODY, "wood")))
    out.append(("desk_left", geometry(DESK_END, "wood")))
    out.append(("desk_right", geometry(mirror_x(DESK_END), "wood")))
    out.append(("desk_item", geometry(DESK_BODY + DESK_END + mirror_x(DESK_END), "wood")))
    body_l, body_u = split_halves(CLOSET_BODY)
    left_l, left_u = split_halves(CLOSET_END)
    right_l, right_u = split_halves(mirror_x(CLOSET_END))
    for name, geo in (("body_lower", body_l), ("body_upper", body_u), ("left_lower", left_l),
                      ("left_upper", left_u), ("right_lower", right_l),
                      ("right_upper", right_u)):
        out.append(("closet_" + name, geometry(geo, "wood")))
    closet_item = CLOSET_BODY + CLOSET_END + mirror_x(CLOSET_END)
    out.append(("closet_item", geometry(closet_item, "wood", big_display(closet_item))))
    out.append(("rug_field", geometry(RUG_FIELD, "rug")))
    out.append(("rug_side", geometry(RUG_SIDE, "rug_border")))
    out.append(("rug_corner", geometry(RUG_CORNER, "rug_border")))
    rug_item = RUG_FIELD + [s for r in (0, 90, 180, 270) for s in turn(RUG_SIDE + RUG_CORNER, r)]
    out.append(("rug_item", geometry(rug_item, "rug")))
    return out


EXTRA_LANG = {
    "csm.furnishings.bed.nowhere": ("You can't sleep here", "Hier kannst du nicht schlafen",
                                    "No puedes dormir aquí", "Du kan inte sova här"),
}


def lang_entries():
    out = {loc: {} for loc in R.LOCALES}
    for key, names in EXTRA_LANG.items():
        for i, loc in enumerate(R.LOCALES):
            out[loc][key] = names[i]
    for reg, _k, _p, _t, names, _j in entries():
        for i, loc in enumerate(R.LOCALES):
            out[loc]["tile.%s.name" % reg] = names[i]
    return out


def generate(assets):
    written = []

    def dump(rel, data):
        R.dump(os.path.join(assets, rel), data)
        written.append(rel)

    def copy_model(rel, parent, ftex):
        dump(rel, {"parent": "csm:block/%s/base/%s" % (SUB, parent), "textures": dict(ftex)})

    for name, draw in TEXTURES.items():
        rel = "textures/blocks/%s/%s.png" % (SUB, name)
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
    for name, data in base_models():
        dump("models/block/%s/base/%s.json" % (SUB, name), data)
    beds = {b[0]: b for b in BEDS}
    for reg, kind, piece, ftex, _names, _java in entries():
        blk = "models/block/%s/%s_%%s.json" % (SUB, reg)
        item = "models/item/%s.json" % reg
        if kind == "bed":
            n = len(BED_CELLS[beds[piece][2]])
            for i in range(n):
                copy_model(blk % ("cell%d" % i), "%s_cell%d" % (piece, i), ftex)
            copy_model(item, "%s_item" % piece, ftex)
            state = cell_state(reg, n)
        elif kind == "tall":
            for half in ("lower", "upper"):
                copy_model(blk % half, "%s_%s" % (piece, half), ftex)
            copy_model(item, "%s_item" % piece, ftex)
            state = K.fridge_state(reg)
        elif kind == "storage":
            copy_model(blk % "body", "%s_body" % piece, ftex)
            copy_model(item, "%s_item" % piece, ftex)
            state = multipart_state(reg, R.faced([("body", {})]))
        elif kind == "desk":
            for part in ("body", "left", "right"):
                copy_model(blk % part, "desk_%s" % part, ftex)
            copy_model(item, "desk_item", ftex)
            state = multipart_state(reg, R.faced([("body", {}), ("left", {"left": "false"}),
                                                  ("right", {"right": "false"})]))
        elif kind == "closet":
            for side in ("body", "left", "right"):
                for half in ("lower", "upper"):
                    copy_model(blk % ("%s_%s" % (side, half)), "closet_%s_%s" % (side, half), ftex)
            copy_model(item, "closet_item", ftex)
            state = multipart_state(reg, closet_rules())
        elif kind == "rug":
            for part in ("field", "side", "corner"):
                copy_model(blk % part, "rug_%s" % part, ftex)
            copy_model(item, "rug_item", ftex)
            state = multipart_state(reg, rug_rules())
        else:
            state = R.single_state(piece, ftex)
        dump("blockstates/%s.json" % reg, state)
    R.write_lang(os.path.join(assets, "lang"), lang_entries())
    written += ["lang/%s.lang" % loc for loc in R.LOCALES]
    R.separate_faces(assets, written)
    return written


def fragments():
    lines = []
    last = None
    for reg, _kind, piece, _t, names, java in entries():
        if piece != last:
            if last is not None:
                lines.append("")
            lines.append("    // %s" % names[0].split(" (")[0])
            last = piece
        lines.append("    initTabBlock(%s);" % java)
    return "\n".join(lines)


def check_bounds():
    bad = []
    for name, data in base_models():
        for e in data["elements"]:
            if any(v < -16 or v > 32 for v in e["from"] + e["to"]):
                bad.append("%s: element out of range: %s" % (name, e))
            for f in e["faces"].values():
                if any(v < 0 or v > 16 for v in f["uv"]):
                    bad.append("%s: uv out of range: %s" % (name, e))
    return bad


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    args = ap.parse_args()
    bad = check_bounds()
    if bad:
        print("\n".join(bad))
        return 1
    if args.fragments:
        print(fragments())
        return 0
    if not args.check:
        written = generate(ASSETS)
        print("wrote %d files under %s" % (len(written), ASSETS))
        return 0
    tmp = tempfile.mkdtemp(prefix="bedroom_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not R.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("bedroom furniture is up to date (%d files)" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
