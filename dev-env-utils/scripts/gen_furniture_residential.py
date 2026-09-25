#!/usr/bin/env python3
"""
gen_furniture_residential.py -- the Residential tab's living and dining furniture in the
Furniture & Novelties module: dining table, coffee table, side table, cafe table, dining chair,
bar stool, bookcase, TV stand and sideboard in three wood finishes (light oak, walnut, white),
and the armchair, sofa and sofa corner in four fabrics (charcoal, navy, oatmeal, red) on walnut feet.

One catalogue writes, under modules/furnishings/src/main/resources/assets/csm:

  * textures/blocks/furniture/residential/*.png   wood grain, fabric weave, book spines, metal
  * models/block/furniture/residential/base/*.json  the geometry, textured light oak / charcoal
  * models/block/furniture/residential/<registry>_<part>.json  a finish's copy of each part a
    multipart blockstate picks (a multipart blockstate cannot retexture, so each finish needs
    its own part models; they only name the parent and the textures)
  * blockstates/<registry>.json, and models/item/<registry>.json for the multipart blocks
  * the tile and tab names in all four languages, by key, leaving every other line alone

Every model faces north with its back (the wall a bookcase stands against, a sofa's backrest)
at +Z, as the rotatable blocks in the mod are drawn. Real-world scale, 1 block = 1 m.

What joins, and how (the Java classes in furniture/residential compute these as actual state;
nothing here is stored in metadata):

  * the dining table joins on all four sides, in world directions: its top is always drawn,
    an apron only on an open side, and a leg only at a corner whose two sides are both open,
    so a 2 x 3 rectangle of blocks is one table with four legs;
  * the sofa, bookcase, TV stand and sideboard join left and right (the sitter's, or the
    viewer's facing the front): arms and end panels only where the run stops;
  * the bookcase also stacks: one plinth at the bottom of a stack and one top at its head;
  * the sofa corner joins sofas on its two open sides; where an L of sofas stops at the
    corner, an arm closes it (on its own, the corner is a corner chair with no arms).

Usage:
    python gen_furniture_residential.py              # write everything
    python gen_furniture_residential.py --check      # fail if the tree has drifted
    python gen_furniture_residential.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import argparse
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
import model_depth  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "modules", "furnishings", "src", "main", "resources", "assets", "csm")
LOCALES = ["en_us", "de_de", "es_es", "sv_se"]
SUB = "furniture/residential"
TEX = "csm:blocks/%s/" % SUB
MODEL = "csm:%s/" % SUB
BASE = MODEL + "base/"

ALL = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")


def T(name):
    return TEX + name


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def shade(c, k):
    return clamp(tuple(v * k for v in c[:3]))


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def wood(base, seed, vertical=False, size=32, streak=0.08, line=0.84, figure=0.05):
    """Wood grain along u (or along v if vertical): streaky rows, a darker grain line every few
    rows that wanders along its length, and a slow figure across the board."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    rows = [1.0 + rng.uniform(-streak, streak) for _ in range(size)]
    lines = set()
    b = rng.randrange(3)
    while b < size:
        lines.add(b)
        b += rng.choice((3, 4, 5, 6))
    phase = [rng.uniform(0, 6.28) for _ in range(size)]
    for y in range(size):
        for x in range(size):
            a, r = (x, y) if not vertical else (y, x)
            wob = int(round(math.sin(a * 2 * math.pi / size * 2 + phase[r]) * 0.7))
            rr = (r + wob) % size
            k = rows[rr] * (1.0 + figure * math.sin(a * 2 * math.pi / size + r * 0.3))
            if rr in lines:
                k *= line
            k *= 1.0 + rng.uniform(-0.02, 0.02)
            px[x, y] = shade(base, k) + (255,)
    return img


def laminate(base, seed, size=32):
    """White painted wood / laminate: flat, the faintest grain showing through."""
    return wood(base, seed, size=size, streak=0.006, line=0.985, figure=0.0)


def flat(colour, seed, size=16, grain=4):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            d = rng.uniform(-grain, grain)
            px[x, y] = clamp(tuple(v + d for v in colour)) + (255,)
    return img


def fabric(base, seed, size=16, heather=0.0):
    """A woven upholstery fabric: a basket weave of lighter and darker texels, a little
    slub, and for a heathered fabric (oatmeal) flecks of lighter and darker yarn."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            weave = 1.035 if (x + y) % 2 == 0 else 0.965
            if (x // 2 + y // 2) % 2:
                weave *= 0.985
            k = weave * (1.0 + rng.uniform(-0.03, 0.03))
            c = shade(base, k)
            if heather and rng.random() < heather:
                c = shade(base, rng.choice((0.8, 1.14)))
            px[x, y] = c + (255,)
    return img


def metal(base, seed, size=16):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    lines = [rng.uniform(-0.05, 0.05) for _ in range(size)]
    for y in range(size):
        for x in range(size):
            px[x, y] = shade(base, 1.0 + lines[y] + rng.uniform(-0.02, 0.02)) + (255,)
    return img


BOOK_COLOURS = [(128, 30, 34), (34, 66, 112), (40, 88, 56), (196, 170, 118), (36, 34, 38),
                (150, 90, 40), (96, 48, 96), (210, 204, 190), (176, 60, 40), (60, 110, 120),
                (190, 150, 50), (80, 80, 84)]


def books(seed, size=32):
    """Book spines, one to two texels (3 to 6 cm) each, running up the texture: a band near
    the head of some, a title block, a darker foot. u runs along the shelf, v down from the
    top of the tallest book."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    x = 0
    while x < size:
        w = rng.choice((1, 1, 2, 2, 2, 3))
        c = rng.choice(BOOK_COLOURS)
        band = rng.random() < 0.6
        gilt = (214, 190, 110) if rng.random() < 0.5 else shade(c, 1.45)
        title = rng.randrange(6, 14)
        for xx in range(x, min(size, x + w)):
            for y in range(size):
                k = 1.0 + rng.uniform(-0.03, 0.03)
                col = shade(c, k)
                if band and y in (2, 3):
                    col = gilt
                elif title <= y < title + 5 and w > 1 and xx == x + w // 2:
                    col = shade(c, 1.35)
                elif y >= size - 2:
                    col = shade(c, 0.7)
                if xx == x and w > 1:
                    col = shade(col, 0.8)
                px[xx, y] = col + (255,)
        x += w
    return img


def book_tops(seed, size=16):
    """Book heads from above: cream page edges in cover-coloured rims."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    x = 0
    while x < size:
        w = rng.choice((1, 2, 2))
        c = rng.choice(BOOK_COLOURS)
        for xx in range(x, min(size, x + w)):
            for y in range(size):
                if y < 2 or xx == x:
                    col = c
                else:
                    col = shade((226, 218, 196), 1.0 + rng.uniform(-0.05, 0.03))
                px[xx, y] = clamp(col) + (255,)
        x += w
    return img


OAK = (214, 178, 128)
OAK_EDGE = (186, 148, 100)
WALNUT = (98, 62, 40)
WALNUT_EDGE = (74, 46, 30)
WHITE = (236, 236, 232)
WHITE_EDGE = (158, 160, 162)

TEXTURES = {
    "oak": lambda: wood(OAK, 301),
    "oak_v": lambda: wood(OAK, 301, vertical=True),
    "oak_edge": lambda: wood(OAK_EDGE, 302, streak=0.05),
    "walnut": lambda: wood(WALNUT, 303, line=0.78, figure=0.08),
    "walnut_v": lambda: wood(WALNUT, 303, vertical=True, line=0.78, figure=0.08),
    "walnut_edge": lambda: wood(WALNUT_EDGE, 304, streak=0.05),
    "white": lambda: laminate(WHITE, 305),
    "white_v": lambda: laminate(WHITE, 305),
    "white_edge": lambda: flat(WHITE_EDGE, 306, grain=3),
    "charcoal": lambda: fabric((74, 74, 78), 311),
    "charcoal_dark": lambda: fabric((56, 56, 60), 312),
    "navy": lambda: fabric((40, 54, 94), 313),
    "navy_dark": lambda: fabric((30, 41, 72), 314),
    "oatmeal": lambda: fabric((204, 190, 164), 315, heather=0.08),
    "oatmeal_dark": lambda: fabric((178, 164, 140), 316, heather=0.08),
    "red": lambda: fabric((150, 36, 38), 317),
    "red_dark": lambda: fabric((114, 26, 28), 318),
    "books": lambda: books(321),
    "books_b": lambda: books(322),
    "book_tops": lambda: book_tops(323),
    "metal_black": lambda: metal((44, 44, 46), 331),
    "metal_steel": lambda: metal((172, 176, 180), 332),
}

WOODS = [
    # (id, textures, en, de, es, sv)
    ("oak", {"wood": T("oak"), "wood_v": T("oak_v"), "edge": T("oak_edge")},
     "Light Oak", "Helle Eiche", "roble claro", "ljus ek"),
    ("walnut", {"wood": T("walnut"), "wood_v": T("walnut_v"), "edge": T("walnut_edge")},
     "Walnut", "Nussbaum", "nogal", "valnöt"),
    ("white", {"wood": T("white"), "wood_v": T("white_v"), "edge": T("white_edge")},
     "White", "Weiß", "blanco", "vit"),
]
FABRICS = [
    ("charcoal", {"fabric": T("charcoal"), "fabric_dark": T("charcoal_dark")},
     "Charcoal", "Anthrazit", "antracita", "antracit"),
    ("navy", {"fabric": T("navy"), "fabric_dark": T("navy_dark")},
     "Navy", "Marineblau", "azul marino", "marinblå"),
    ("oatmeal", {"fabric": T("oatmeal"), "fabric_dark": T("oatmeal_dark")},
     "Oatmeal", "Haferbeige", "avena", "havrebeige"),
    ("red", {"fabric": T("red"), "fabric_dark": T("red_dark")},
     "Red", "Rot", "rojo", "röd"),
]
# The textures every base model starts with; a finish overrides its own keys.
DEFAULT_TEX = {"wood": T("oak"), "wood_v": T("oak_v"), "edge": T("oak_edge"),
               "fabric": T("charcoal"), "fabric_dark": T("charcoal_dark"),
               "leg": T("walnut_v"), "books": T("books"), "books_b": T("books_b"),
               "book_tops": T("book_tops"), "metal": T("metal_steel"),
               "metal_black": T("metal_black")}


# ------------------------------------------------------------------------------------------
# Elements: specs (box, texture key, faces) turned into JSON with fitted UVs at the end, so
# mirroring, turning and swapping axes work on boxes and the UVs always fit what they cover
# ------------------------------------------------------------------------------------------
def el(frm, to, tex, faces=ALL, per=None, uv=None, rot=None):
    return {"from": list(frm), "to": list(to), "tex": tex, "faces": tuple(faces),
            "per": dict(per or {}), "uv": dict(uv or {}), "rot": rot}


FACE_AXIS = {"east": 0, "west": 0, "up": 1, "down": 1, "north": 2, "south": 2}


def board(frm, to, grain="h", faces=ALL):
    """A board: its two broad faces (square to its thinnest side) in the finish's grain, its
    four narrow edges in the edge texture (a white laminate's grey edge banding)."""
    dims = [to[i] - frm[i] for i in range(3)]
    thin = dims.index(min(dims))
    surf = "wood" if grain == "h" else "wood_v"
    per = {f: (surf if FACE_AXIS[f] == thin else "edge") for f in faces}
    return el(frm, to, surf, faces, per)


def leg(frm, to, faces=("north", "south", "east", "west", "down"), tex="wood_v"):
    return el(frm, to, tex, faces)


def _face_uv(f, frm, to):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    std = {
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        "south": [x0, 16 - y1, x1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
        "west": [z0, 16 - y1, z1, 16 - y0],
        "up": [x0, z0, x1, z1],
        "down": [x0, 16 - z1, x1, 16 - z0],
    }[f]
    if all(0 <= v <= 16 for v in std):
        uv = std
    else:
        # Past the cell: keep the face's size, slid back inside the texture.
        w = abs(std[2] - std[0])
        h = abs(std[3] - std[1])
        k = min(1.0, 16.0 / max(w, h, 1e-6))
        u0 = min(max(std[0], 0), 16 - w * k)
        v0 = min(max(std[1], 0), 16 - h * k)
        uv = [u0, v0, u0 + w * k, v0 + h * k]
    if abs(uv[2] - uv[0]) < 0.01:
        uv[2] = uv[0] + 0.01
    if abs(uv[3] - uv[1]) < 0.01:
        uv[3] = uv[1] + 0.01
    return uv


def build(spec):
    faces = {}
    for f in spec["faces"]:
        u = spec["uv"].get(f) or _face_uv(f, spec["from"], spec["to"])
        faces[f] = {"texture": "#" + spec["per"].get(f, spec["tex"]),
                    "uv": [round(v, 3) for v in u]}
    e = {"from": [round(v, 3) for v in spec["from"]], "to": [round(v, 3) for v in spec["to"]],
         "faces": faces}
    if spec["rot"]:
        axis, angle, origin = spec["rot"]
        e["rotation"] = {"origin": [round(v, 3) for v in origin], "axis": axis, "angle": angle}
    return e


def _remap(spec, point, face_map):
    """A copy of spec moved by point(x, y, z) -> (x, y, z), faces renamed by face_map."""
    a = point(*spec["from"])
    b = point(*spec["to"])
    out = dict(spec)
    out["from"] = [min(a[i], b[i]) for i in range(3)]
    out["to"] = [max(a[i], b[i]) for i in range(3)]
    out["faces"] = tuple(face_map.get(f, f) for f in spec["faces"])
    out["per"] = {face_map.get(f, f): t for f, t in spec["per"].items()}
    out["uv"] = {}
    if spec["rot"]:
        raise ValueError("cannot remap a turned element")
    return out


def mirror_x(specs):
    return [_remap(s, lambda x, y, z: (16 - x, y, z), {"east": "west", "west": "east"})
            for s in specs]


def swap_xz(specs):
    """Reflected through the x = z plane: what ran along x now runs along z."""
    return [_remap(s, lambda x, y, z: (z, y, x),
                   {"north": "west", "west": "north", "south": "east", "east": "south"})
            for s in specs]


def turn(specs, deg):
    """Turned clockwise seen from above, as a blockstate's y does: 90 takes north to east."""
    out = list(specs)
    fm = {"north": "east", "east": "south", "south": "west", "west": "north"}
    for _ in range((deg // 90) % 4):
        out = [_remap(s, lambda x, y, z: (16 - z, y, x), fm) for s in out]
    return out


TAN_22_5 = math.tan(math.pi / 8)


def octagon(cx, cz, r, y0, y1, tex, side=None, caps=("up", "down")):
    """An upright regular octagon of inradius r about (cx, cz): four rectangles, two of them
    turned 45 degrees, whose corners are exactly the octagon's. Each draws only its two long
    sides; the caps sit a hair apart so the four coplanar ends do not fight."""
    a = r * TAN_22_5
    out = []
    for k, (h1, h2, turned) in enumerate(((r, a, False), (a, r, False), (r, a, True),
                                          (a, r, True))):
        eps = 0.004 * k
        frm = [cx - h1, y0 - eps, cz - h2]
        to = [cx + h1, y1 + eps, cz + h2]
        sides = ["east", "west"] if h1 == r else ["north", "south"]
        faces = sides + list(caps)
        per = {f: side for f in sides} if side else {}
        rot = ("y", 45, [cx, y0, cz]) if turned else None
        out.append(el(frm, to, tex, faces, per, rot=rot))
    return out


# ------------------------------------------------------------------------------------------
# Models
# ------------------------------------------------------------------------------------------
def used_keys(specs):
    keys = set()
    for s in specs:
        for f in s["faces"]:
            keys.add(s["per"].get(f, s["tex"]))
    return keys


def geometry(specs, particle):
    tex = {k: DEFAULT_TEX[k] for k in sorted(used_keys(specs) | {particle})}
    tex["particle"] = "#" + particle
    return {"parent": "block/block", "textures": tex, "elements": [build(s) for s in specs]}


# ------------------------------------------------------------------------------------------
# The pieces, each drawn facing north (front at -Z, back at +Z), in pixels
# ------------------------------------------------------------------------------------------
# --- dining table: 0.75 m high, a leg 9 cm square, the apron 12 cm deep -------------------
TABLE_TOP = [board([0, 11, 0], [16, 12, 16])]
TABLE_APRON = [board([2.5, 9, 1.25], [13.5, 11, 2], faces=("north", "south", "down"))]
TABLE_LEG = [leg([1, 0, 1], [2.5, 11, 2.5])]
# A corner with one side joined: the open side's apron runs on to the block's edge to meet
# the next block's. _a is the north apron reaching west, _b the west apron reaching north.
TABLE_APRON_A = [board([0, 9, 1.25], [2.5, 11, 2], faces=("north", "south", "down"))]
TABLE_APRON_B = [board([1.25, 9, 0], [2, 11, 2.5], faces=("east", "west", "down"))]

# --- coffee table: 1.0 x 0.44 x 0.62 m with a shelf underneath -----------------------------
COFFEE = ([board([0.5, 6, 3], [15.5, 7, 13])]
          + [leg([x, 0, z], [x + 1.25, 6, z + 1.25]) for x in (1, 13.75) for z in (3.5, 11.25)]
          + [board([2.25, 1.5, 3.5], [13.75, 2.25, 12.5])])

# --- side table: 0.5 x 0.56 x 0.5 m, a drawer and a shelf ---------------------------------
SIDE = ([board([4, 8, 4], [12, 9, 12])]
        + [leg([x, 0, z], [x + 1, 8, z + 1]) for x in (4.5, 10.5) for z in (4.5, 10.5)]
        + [board([5.5, 6, 5], [10.5, 8, 11], faces=("east", "west", "south", "down")),
           board([5.5, 6.1, 4.75], [10.5, 7.9, 5]),
           el([7.6, 6.75, 4.35], [8.4, 7.25, 4.75], "metal"),
           board([5.5, 1.5, 5.5], [10.5, 2, 10.5])])

# --- cafe table: a round top 0.7 m across on a cast iron pedestal --------------------------
CAFE = (octagon(8, 8, 5.6, 11, 12, "wood", side="edge")
        + octagon(8, 8, 1.5, 10, 11, "metal_black", caps=("down",))
        + octagon(8, 8, 0.7, 1, 10, "metal_black", caps=())
        + octagon(8, 8, 3.5, 0, 1, "metal_black"))

# --- dining chair: seat 0.47 m high, back 0.94 m --------------------------------------------
CHAIR = ([board([4, 6.5, 4], [12, 7.5, 11.5])]
         + [leg([x, 0, 4.25], [x + 1, 6.5, 5.25]) for x in (4.25, 10.75)]
         + [leg([x, 0, 10.5], [x + 1, 15, 11.5], faces=ALL) for x in (4.25, 10.75)]
         + [board([5.25, 12, 10.75], [10.75, 14.5, 11.25]),
            board([5.25, 9, 10.75], [10.75, 10, 11.25])]
         + [leg([x, 10, 10.85], [x + 0.6, 12, 11.15], faces=("north", "south", "east", "west"))
            for x in (6.5, 7.7, 8.9)]
         + [leg([x, 2.5, 5.25], [x + 0.5, 3, 10.5], faces=("east", "west", "up", "down"))
            for x in (4.5, 11)])

# --- bar stool: a round seat 0.75 m up, a steel foot rail ----------------------------------
STOOL = (octagon(8, 8, 3.6, 11, 12, "wood", side="edge")
         + [leg([x, 0, z], [x + 1, 11, z + 1]) for x in (5, 10) for z in (5, 10)]
         + [el([6, 4, 5.25], [10, 4.5, 5.75], "metal", ("north", "south", "up", "down")),
            el([6, 4, 10.25], [10, 4.5, 10.75], "metal", ("north", "south", "up", "down")),
            el([5.25, 4, 6], [5.75, 4.5, 10], "metal", ("east", "west", "up", "down")),
            el([10.25, 4, 6], [10.75, 4.5, 10], "metal", ("east", "west", "up", "down"))])


# --- bookcase: 0.34 m deep against the wall, two shelves of books a block ----------------
def book_row(y0, y_top, seed, tex):
    """Books standing on a shelf at y0, no taller than y_top: groups of two or three, each its
    own element so the heights differ, with a gap or two, between x 1 and 15."""
    rng = random.Random(seed)
    out = []
    x = 1.0
    u = rng.uniform(0, 8)
    while x < 14.6:
        w = min(rng.choice((1.0, 1.5, 1.5, 2.0, 2.5)), 15 - x)
        if w < 0.75:
            break
        if rng.random() < 0.12 and x > 2:
            x += rng.choice((0.75, 1.25))
            continue
        h = y_top - y0 - rng.uniform(0, 1.6)
        z0 = 11 + rng.choice((0, 0.25, 0.5))
        u0 = u % (16 - w)
        uv = {"north": [u0, 0, u0 + w, h], "south": [u0, 0, u0 + w, h],
              "east": [u0, 0, u0 + 0.5, h], "west": [u0, 0, u0 + 0.5, h]}
        spec = el([x, y0, z0], [x + w, y0 + h, 14.75], tex,
                  ("north", "east", "west", "up"), {"up": "book_tops"}, uv=uv)
        out.append(spec)
        u += w + rng.uniform(1, 3)
        x += w
    return out


BOOK_TOP = 14.75
SHELF_FACES = ("north", "up", "down")
BOOKCASE_BODY = ([board([0, 0, 15], [16, 16, 15.75], grain="v", faces=("north", "south")),
                  board([0, 7.5, 10.5], [16, 8.25, 15], faces=SHELF_FACES)]
                 + book_row(8.25, BOOK_TOP, 41, "books"))
BOOKCASE_PLINTH = ([board([0, 0, 11], [16, 1.5, 11.75], faces=("north",)),
                    board([0, 1.5, 10.5], [16, 2.25, 15], faces=("north", "up"))]
                   + book_row(2.25, 7.25, 42, "books_b"))
BOOKCASE_STACKED = ([board([0, 0, 10.5], [16, 0.75, 15], faces=SHELF_FACES)]
                    + book_row(0.75, 7.25, 43, "books_b"))
BOOKCASE_TOP = [board([0, 15, 10], [16, 16, 16])]
BOOKCASE_SIDE_SHORT = [board([0, 0, 10.5], [1, 15, 16], grain="v")]
BOOKCASE_SIDE_FULL = [board([0, 0, 10.5], [1, 16, 16], grain="v")]

# --- TV stand: 0.5 m high, 0.44 m deep, two doors a block ---------------------------------
TV_BODY = [board([0, 7.25, 9], [16, 8, 16]),
           board([0, 1, 9.5], [16, 1.75, 15.25], faces=("up", "down")),
           board([0, 0, 10], [16, 1, 10.5], faces=("north",)),
           board([0, 1, 15.25], [16, 7.25, 15.9], grain="v", faces=("north", "south")),
           board([1, 1.25, 9], [7.9, 7, 9.5], grain="v"),
           board([8.1, 1.25, 9], [15, 7, 9.5], grain="v"),
           el([7, 3, 8.5], [7.4, 5.5, 9], "metal"),
           el([8.6, 3, 8.5], [9, 5.5, 9], "metal")]
TV_END = [board([0, 0, 9], [1, 7.25, 16], grain="v")]
TV_STILE = [board([0, 1.25, 9.05], [1, 7, 9.5], grain="v", faces=("north", "up", "down"))]

# --- sideboard: 0.85 m high on 0.19 m legs, drawers over doors ----------------------------
SIDEBOARD_BODY = [board([0, 13, 8.75], [16, 13.75, 16]),
                  board([0, 3, 9], [16, 3.75, 16], faces=("north", "down")),
                  board([0, 3.75, 15.25], [16, 13, 15.9], grain="v", faces=("north", "south")),
                  board([1, 10.25, 9], [7.9, 12.75, 9.5]),
                  board([8.1, 10.25, 9], [15, 12.75, 9.5]),
                  board([1, 4, 9], [7.9, 10, 9.5], grain="v"),
                  board([8.1, 4, 9], [15, 10, 9.5], grain="v"),
                  el([3.5, 11.25, 8.6], [5.4, 11.75, 9], "metal"),
                  el([10.6, 11.25, 8.6], [12.5, 11.75, 9], "metal"),
                  el([7, 6, 8.6], [7.4, 8.5, 9], "metal"),
                  el([8.6, 6, 8.6], [9, 8.5, 9], "metal")]
SIDEBOARD_END = [board([0, 3, 9], [1, 13, 16], grain="v"),
                 leg([0.25, 0, 9.25], [1.5, 3, 10.5]),
                 leg([0.25, 0, 14.5], [1.5, 3, 15.75])]
SIDEBOARD_STILE = [board([0, 3.75, 9.05], [1, 13, 9.5], grain="v", faces=("north",))]

# --- armchair: 0.84 m square, seat 0.45 m, arms 0.64 m -----------------------------------
ARM_LEFT = [el([1.25, 5, 1.75], [3.5, 9.5, 12], "fabric_dark"),
            el([1.5, 9.5, 2], [3.25, 10.25, 12], "fabric"),
            el([1.5, 5.5, 1.5], [3.25, 9.25, 1.75], "fabric")]
ARMCHAIR = ([leg([x, 0, z], [x + 1.25, 1.5, z + 1.25], tex="leg")
             for x in (1.75, 13) for z in (2, 12.75)]
            + [el([1.25, 1.5, 1.5], [14.75, 5, 14.5], "fabric_dark"),
               el([3.5, 5, 1.75], [12.5, 7.25, 12], "fabric"),
               el([1.25, 5, 12], [14.75, 13, 14.5], "fabric_dark"),
               el([1.5, 13, 12.25], [14.5, 13.75, 14.25], "fabric_dark"),
               el([3.5, 7.25, 10.5], [12.5, 12.5, 12], "fabric")]
            + ARM_LEFT + mirror_x(ARM_LEFT))

# --- sofa: 0.9 m deep, 0.84 m high, a seat cushion and a back cushion a block -------------
SOFA_BODY = [el([0, 1.5, 2], [16, 5, 15.5], "fabric_dark", ("north", "south", "down")),
             el([0, 5, 12], [16, 13, 15.5], "fabric_dark", ("north", "south", "up")),
             el([0, 13, 12.25], [16, 13.5, 15.25], "fabric_dark"),
             el([0.1, 5, 2], [15.9, 7.25, 12], "fabric"),
             el([0.1, 7.25, 10.5], [15.9, 12.5, 12], "fabric"),
             leg([7.4, 0, 2.5], [8.6, 1.5, 3.75], tex="leg"),
             leg([7.4, 0, 13.75], [8.6, 1.5, 15], tex="leg")]
# Where a run stops: the arm, rounded by a narrower roll on top and a narrower pad in front,
# a panel closing the end of the back above the arm, and two walnut feet.
SOFA_ARM = [el([0, 1.5, 1.75], [2.5, 9.5, 15.5], "fabric_dark"),
            el([0.25, 9.5, 2], [2.25, 10.25, 15.25], "fabric"),
            el([0.25, 2, 1.5], [2.25, 9.25, 1.75], "fabric"),
            el([0, 9.5, 12], [0.75, 13, 15.5], "fabric_dark"),
            leg([0.5, 0, 2.5], [1.75, 1.5, 3.75], tex="leg"),
            leg([0.5, 0, 13.75], [1.75, 1.5, 15], tex="leg")]

# --- sofa corner: the backs along +Z and +X, seats open to -X and -Z ----------------------
CORNER_BODY = [el([0, 1.5, 2], [15.5, 5, 15.5], "fabric_dark", ("north", "south", "east", "down")),
               el([2, 1.5, 0], [15.5, 5, 2], "fabric_dark", ("west", "east", "down")),
               el([0, 5, 12], [15.5, 13, 15.5], "fabric_dark", ("north", "south", "east", "up")),
               el([12, 5, 0], [15.5, 13, 12], "fabric_dark", ("west", "east", "up")),
               el([0, 13, 12.25], [15.25, 13.5, 15.25], "fabric_dark"),
               el([12.25, 13, 0], [15.25, 13.5, 12.25], "fabric_dark",
                  ("north", "east", "west", "up", "down")),
               el([0.1, 5, 2], [12, 7.25, 12], "fabric"),
               el([2, 5, 0.1], [12, 7.25, 2], "fabric", ("north", "east", "west", "up", "down")),
               el([0.1, 7.25, 10.5], [12, 12.5, 12], "fabric"),
               el([10.5, 7.25, 0.1], [12, 12.5, 10.5], "fabric")]
CORNER_BODY += [leg([x, 0, z], [x + 1.25, 1.5, z + 1.25], tex="leg")
                for x, z in ((2.5, 2.5), (2.5, 13.75), (13.75, 2.5), (13.75, 13.75))]
CORNER_ARM_FRONT = swap_xz(SOFA_ARM)
# A corner with nothing on either open side: the ends of its base and backs closed.
CORNER_CAP_LEFT = [el([0, 1.5, 2], [0.01, 5, 15.5], "fabric_dark", ("west",)),
                   el([0, 5, 12], [0.01, 13, 15.5], "fabric_dark", ("west",))]
CORNER_CAPS = CORNER_CAP_LEFT + swap_xz(CORNER_CAP_LEFT)


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
PIECES = [
    # (piece, kind, en, de, es, sv)
    ("dining_table", "wood", "Dining Table", "Esstisch", "Mesa de comedor", "Matbord"),
    ("coffee_table", "wood", "Coffee Table", "Couchtisch", "Mesa de centro", "Soffbord"),
    ("side_table", "wood", "Side Table", "Beistelltisch", "Mesa auxiliar", "Sidobord"),
    ("cafe_table", "wood", "Café Table", "Bistrotisch", "Mesa de café", "Cafébord"),
    ("dining_chair", "wood", "Dining Chair", "Esszimmerstuhl", "Silla de comedor", "Matstol"),
    ("bar_stool", "wood", "Bar Stool", "Barhocker", "Taburete de bar", "Barstol"),
    ("bookcase", "wood", "Bookcase", "Bücherregal", "Estantería", "Bokhylla"),
    ("tv_stand", "wood", "TV Stand", "TV-Möbel", "Mueble de TV", "TV-bänk"),
    ("sideboard", "wood", "Sideboard", "Sideboard", "Aparador", "Skänk"),
    ("armchair", "fabric", "Armchair", "Sessel", "Sillón", "Fåtölj"),
    ("sofa", "fabric", "Sofa", "Sofa", "Sofá", "Soffa"),
    ("sofa_corner", "fabric", "Sofa Corner", "Sofa-Eckelement", "Rinconera de sofá", "Soffhörn"),
]

# Single pieces: one model, turned by facing. (geometry, particle, java box, java)
SINGLE = {
    "coffee_table": (COFFEE, "wood", [0, 0, 3, 16, 7, 13], None),
    "side_table": (SIDE, "wood", [4, 0, 4, 12, 9, 12], None),
    "cafe_table": (CAFE, "wood", [2, 0, 2, 14, 12, 14], None),
    # seat: (seat top, how far forward of the middle, how far left of it), in pixels
    "dining_chair": (CHAIR, "wood", [4, 0, 4, 12, 15, 12], (7.5, 0.25, 0.0)),
    "bar_stool": (STOOL, "wood", [4, 0, 4, 12, 12, 12], (12.0, 0.0, 0.0)),
    "armchair": (ARMCHAIR, "fabric", [1, 0, 1, 15, 14, 15], (7.25, 1.9, 0.0)),
}

# Multipart pieces: parts (name -> geometry), the item's geometry, the multipart rules.
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def faced(parts_when, facing_prop=True):
    """Multipart rules for a faced piece: each (part, when) once per facing, turned."""
    rules = []
    for facing, r in ROT.items():
        for part, when in parts_when:
            w = dict(when)
            w["facing"] = facing
            rules.append((part, w, r))
    return rules


def table_rules():
    rules = [("top", {}, 0)]
    for side, r in ROT.items():
        rules.append(("apron", {side: "false"}, r))
    for (d1, d2), r in ((("north", "west"), 0), (("east", "north"), 90),
                        (("south", "east"), 180), (("west", "south"), 270)):
        rules.append(("leg", {d1: "false", d2: "false"}, r))
        rules.append(("apron_a", {d1: "false", d2: "true"}, r))
        rules.append(("apron_b", {d1: "true", d2: "false"}, r))
    return rules


MULTI = {
    "dining_table": {
        "parts": {"top": TABLE_TOP, "apron": TABLE_APRON, "leg": TABLE_LEG,
                  "apron_a": TABLE_APRON_A, "apron_b": TABLE_APRON_B},
        "item": TABLE_TOP + [s for r in (0, 90, 180, 270)
                             for s in turn(TABLE_APRON + TABLE_LEG, r)],
        "rules": table_rules(), "particle": "wood",
        "java": 'new BlockDiningTable("%s")',
    },
    "bookcase": {
        "parts": {"body": BOOKCASE_BODY, "plinth": BOOKCASE_PLINTH,
                  "stacked": BOOKCASE_STACKED, "top": BOOKCASE_TOP,
                  "left": BOOKCASE_SIDE_SHORT, "left_full": BOOKCASE_SIDE_FULL,
                  "right": mirror_x(BOOKCASE_SIDE_SHORT),
                  "right_full": mirror_x(BOOKCASE_SIDE_FULL)},
        "item": (BOOKCASE_BODY + BOOKCASE_PLINTH + BOOKCASE_TOP + BOOKCASE_SIDE_SHORT
                 + mirror_x(BOOKCASE_SIDE_SHORT)),
        "rules": faced([("body", {}), ("plinth", {"down": "false"}),
                        ("stacked", {"down": "true"}), ("top", {"up": "false"}),
                        ("left", {"left": "false", "up": "false"}),
                        ("left_full", {"left": "false", "up": "true"}),
                        ("right", {"right": "false", "up": "false"}),
                        ("right_full", {"right": "false", "up": "true"})]),
        "particle": "wood", "java": 'new BlockBookcase("%s", new int[]{0, 0, 10, 16, 16, 16})',
    },
    "tv_stand": {
        "parts": {"body": TV_BODY, "left": TV_END, "right": mirror_x(TV_END),
                  "stile_left": TV_STILE, "stile_right": mirror_x(TV_STILE)},
        "item": TV_BODY + TV_END + mirror_x(TV_END),
        "rules": faced([("body", {}), ("left", {"left": "false"}), ("right", {"right": "false"}),
                        ("stile_left", {"left": "true"}), ("stile_right", {"right": "true"})]),
        "particle": "wood",
        "java": 'new BlockResidentialStorage("%s", new int[]{0, 0, 9, 16, 8, 16}, 9)',
    },
    "sideboard": {
        "parts": {"body": SIDEBOARD_BODY, "left": SIDEBOARD_END,
                  "right": mirror_x(SIDEBOARD_END), "stile_left": SIDEBOARD_STILE,
                  "stile_right": mirror_x(SIDEBOARD_STILE)},
        "item": SIDEBOARD_BODY + SIDEBOARD_END + mirror_x(SIDEBOARD_END),
        "rules": faced([("body", {}), ("left", {"left": "false"}), ("right", {"right": "false"}),
                        ("stile_left", {"left": "true"}), ("stile_right", {"right": "true"})]),
        "particle": "wood",
        "java": 'new BlockResidentialStorage("%s", new int[]{0, 0, 8, 16, 14, 16}, 18)',
    },
    "sofa": {
        "parts": {"body": SOFA_BODY, "left": SOFA_ARM, "right": mirror_x(SOFA_ARM)},
        "item": SOFA_BODY + SOFA_ARM + mirror_x(SOFA_ARM),
        "rules": faced([("body", {}), ("left", {"left": "false"}),
                        ("right", {"right": "false"})]),
        "particle": "fabric", "java": 'new BlockSofa("%s", new int[]{0, 0, 1, 16, 14, 16})',
    },
    "sofa_corner": {
        "parts": {"body": CORNER_BODY, "left": SOFA_ARM, "front": CORNER_ARM_FRONT,
                  "caps": CORNER_CAPS},
        # An arm closes the L where it stops: on an open side only while the other side has a
        # sofa. A corner on its own is a corner chair, open on both sides, its ends capped.
        "item": CORNER_BODY + CORNER_CAPS,
        "rules": faced([("body", {}), ("left", {"left": "false", "front": "true"}),
                        ("front", {"front": "false", "left": "true"}),
                        ("caps", {"left": "false", "front": "false"})]),
        "particle": "fabric", "java": 'new BlockSofaCorner("%s")',
    },
}


def finishes(kind):
    return WOODS if kind == "wood" else FABRICS


def entries():
    """Every block: (registry, piece, kind, finish id, finish textures, names)."""
    out = []
    for piece, kind, *names in PIECES:
        for fid, ftex, *fnames in finishes(kind):
            reg = "%s_%s" % (piece, fid)
            full = ["%s (%s)" % (names[0], fnames[0])] + [
                "%s (%s)" % (n, f) for n, f in zip(names[1:], fnames[1:])]
            out.append((reg, piece, kind, fid, ftex, full))
    return out


def java_line(reg, piece, kind):
    if piece in SINGLE:
        _, _, jbox, seat = SINGLE[piece]
        box = ", ".join(_num(v) for v in jbox)
        up = "true" if kind == "fabric" else "false"
        if seat:
            return ('new BlockResidentialFurniture("%s", new int[]{%s}, %s, %s, %s, %s)'
                    % (reg, box, up, _num(seat[0]), _num(seat[1]), _num(seat[2])))
        return 'new BlockResidentialFurniture("%s", new int[]{%s}, %s)' % (reg, box, up)
    return MULTI[piece]["java"] % reg


def _num(v):
    return str(int(v)) if float(v).is_integer() else repr(float(v))


# ------------------------------------------------------------------------------------------
# Output
# ------------------------------------------------------------------------------------------
def dump(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2, ensure_ascii=False)
        fh.write("\n")


def separate_faces(assets, written):
    """Moves the coplanar faces this generator wrote apart, so they do not z-fight
    ({@code model_depth}); run by every furniture generator at the end of generate."""
    model_depth.separate(assets, written, dump)


def single_state(piece, ftex):
    return {"forge_marker": 1,
            "defaults": {"model": BASE + piece, "textures": dict(ftex)},
            "variants": {"facing": {f: ({"y": r} if r else {}) for f, r in ROT.items()},
                         "inventory": [{}]}}


def multipart_state(reg, piece):
    parts = []
    for part, when, r in MULTI[piece]["rules"]:
        apply = {"model": MODEL + "%s_%s" % (reg, part)}
        if r:
            apply["y"] = r
        rule = {"apply": apply}
        if when:
            rule = {"when": when, "apply": apply}
        parts.append(rule)
    return {"multipart": parts}


EXTRA_LANG = {
    "itemGroup.tabresidential": ("CSM: Residential", "CSM: Wohnen", "CSM: Residencial",
                                 "CSM: Bostad"),
    "csm.furnishings.seat.taken": ("Someone is already sitting there", "Da sitzt schon jemand",
                                   "Ya hay alguien sentado ahí", "Någon sitter redan där"),
}


def lang_entries():
    out = {loc: {} for loc in LOCALES}
    for key, names in EXTRA_LANG.items():
        for i, loc in enumerate(LOCALES):
            out[loc][key] = names[i]
    for reg, _piece, _kind, _fid, _ftex, names in entries():
        for i, loc in enumerate(LOCALES):
            out[loc]["tile.%s.name" % reg] = names[i]
    return out


def write_lang(lang_dir, values_by_locale):
    """Keeps each owned key's line in place (or appends it), leaving every other line alone,
    in the file's own line endings."""
    for loc, values in values_by_locale.items():
        path = os.path.join(lang_dir, loc + ".lang")
        text = ""
        if os.path.exists(path):
            with open(path, encoding="utf-8", newline="") as fh:
                text = fh.read()
        eol = "\r\n" if "\r\n" in text else "\n"
        lines = text.replace("\r\n", "\n").split("\n")
        if lines and lines[-1] == "":
            lines.pop()
        seen = set()
        for i, line in enumerate(lines):
            key = line.split("=", 1)[0]
            if key in values:
                lines[i] = "%s=%s" % (key, values[key])
                seen.add(key)
        for key, value in values.items():
            if key not in seen:
                lines.append("%s=%s" % (key, value))
        with open(path, "w", encoding="utf-8", newline="") as fh:
            fh.write(eol.join(lines) + eol)


def generate(assets):
    written = []
    for name, draw in TEXTURES.items():
        rel = "textures/blocks/%s/%s.png" % (SUB, name)
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
    # Base geometry.
    for piece, (geo, particle, _box, _seat) in SINGLE.items():
        rel = "models/block/%s/base/%s.json" % (SUB, piece)
        dump(os.path.join(assets, rel), geometry(geo, particle))
        written.append(rel)
    for piece, spec in MULTI.items():
        for part, geo in spec["parts"].items():
            rel = "models/block/%s/base/%s_%s.json" % (SUB, piece, part)
            dump(os.path.join(assets, rel), geometry(geo, spec["particle"]))
            written.append(rel)
        rel = "models/block/%s/base/%s_item.json" % (SUB, piece)
        dump(os.path.join(assets, rel), geometry(spec["item"], spec["particle"]))
        written.append(rel)
    # Blocks.
    for reg, piece, _kind, _fid, ftex, _names in entries():
        if piece in SINGLE:
            state = single_state(piece, ftex)
        else:
            for part in MULTI[piece]["parts"]:
                rel = "models/block/%s/%s_%s.json" % (SUB, reg, part)
                dump(os.path.join(assets, rel),
                     {"parent": "csm:block/%s/base/%s_%s" % (SUB, piece, part), "textures": dict(ftex)})
                written.append(rel)
            rel = "models/item/%s.json" % reg
            dump(os.path.join(assets, rel),
                 {"parent": "csm:block/%s/base/%s_item" % (SUB, piece), "textures": dict(ftex)})
            written.append(rel)
            state = multipart_state(reg, piece)
        rel = "blockstates/%s.json" % reg
        dump(os.path.join(assets, rel), state)
        written.append(rel)
    write_lang(os.path.join(assets, "lang"), lang_entries())
    written += ["lang/%s.lang" % loc for loc in LOCALES]
    separate_faces(assets, written)
    return written


def fragments():
    lines = []
    last = None
    for reg, piece, kind, _fid, _ftex, names in entries():
        if piece != last:
            if last is not None:
                lines.append("")
            lines.append("    // %s" % names[0].split(" (")[0].replace("é", "e"))
            last = piece
        lines.append("    initTabBlock(%s);" % java_line(reg, piece, kind))
    return "\n".join(lines)


def same_file(a, b):
    if not os.path.exists(b):
        return False
    if a.endswith(".png"):
        return (Image.open(a).convert("RGBA").tobytes()
                == Image.open(b).convert("RGBA").tobytes())
    return layout.same_generated_text(a, b)


def check_bounds():
    """Every element inside -16..32 and every UV inside 0..16, or the game clamps them."""
    bad = []
    geos = [g for g, *_ in SINGLE.values()]
    for spec in MULTI.values():
        geos += list(spec["parts"].values()) + [spec["item"]]
    for geo in geos:
        for s in geo:
            e = build(s)
            if any(v < -16 or v > 32 for v in e["from"] + e["to"]):
                bad.append("element out of range: %s" % e)
            for f in e["faces"].values():
                if any(v < 0 or v > 16 for v in f["uv"]):
                    bad.append("uv out of range: %s" % e)
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
    tmp = tempfile.mkdtemp(prefix="residential_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("residential furniture is up to date (%d files)" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
