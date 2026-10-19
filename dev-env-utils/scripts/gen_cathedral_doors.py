#!/usr/bin/env python3
"""The cathedral doors, their arch heads, the tympana and the carved panelling.

Not run on its own: gen_doors.py imports it and writes and checks these files with the rest of the
doors (`gen_doors.py`, `gen_doors.py --check`, `gen_doors.py --fragments`).

A cathedral door leaf is two blocks wide and as tall as the opening it is placed in (three or four
blocks from the item, any height by command), with iron strap hinges, a ring pull and studded
panels, in dark oak, weathered oak or bronze. An optional ARCH HEAD, three blocks of leaf above it,
gives it a pointed (Gothic) top: over a pair the two heads make one arch four blocks across, over a
single leaf a lancet two across. Above a square-headed door the TYMPANUM fills the same arch with a
carved panel that does not move.

ONE BLOCK PER CELL, as the garage doors are (BlockCathedralDoor). Every cell of a leaf stores the
same four bits: facing, open, hinge. Which column it is (hinge or latch), which row, and, in a head,
which course and whether it is half of a pair's arch are actual state, read off its neighbours. So
each cell draws its own piece from a baked model -- culled and lit as its own block, with no tile
entity at rest -- and a leaf is placed by command by filling its cells with one metadata value.

Drawn with the INSIDE to the north and the outside to the south, hinged on the left (x = 0, seen from
outside), as the other doors are. The leaf (T px thick) lies on the outside face of its cells and
swings in; open, its hinge column lies along the jamb in its own cell and its latch column in the
cell inside that, which a JSON element can still reach (-16..32). The open models are the shut
ones turned a quarter about the hinge pivot, written out, and the swing renderer turns the shut
models about the same pivot. SHARED with BlockCathedralDoor: T, the pivot, RISE and the spans.

The arch is stepped a pixel at a time: an element per run of pixel rows with the same width. The
stone beside the arch in a head cell (the spandrel) is fixed to the wall, so it is marked
TINT_FIXED and the swing renderer leaves it where it is. It is drawn in the Building module's own
stone veneers, `stone_ashlar` or `stone_cast` (gen_stone.py), whichever the jambs beside the arch
are built of (`stone`, actual state): the head's spandrel is its own multipart part, a model
shared by the three door variants, so the choice costs a part and no stored bit. The tympanum
picks its stone the same way, from the same spandrel parts (its columns are a pair's head cells:
0 and 1 a left leaf's hinge and latch, 2 and 3 a right leaf's latch and hinge).
"""

import copy
import math
import random

from PIL import Image

import gen_logistics
import gen_scaffold as sc

_shift = gen_logistics._shift
box = sc._box

TEX_REF = "csm:blocks/doors/%s"
MODEL_REF = "csm:doors/%s"
PARENT_REF = "csm:block/doors/%s"
# The spandrel's stone: Building's own veneers, picked by the jambs beside the arch (SHARED with
# BlockCathedralDoorArch.Stone).
STONES = {"ashlar": "csm:blocks/buildingmaterials/stone/stone_ashlar",
          "cast": "csm:blocks/buildingmaterials/stone/stone_cast"}

# SHARED with BlockCathedralDoor: the leaf's thickness, and the pivot a left-hinged leaf turns about
# in its hinge column's cell, in px, inside to the north.
T = 2.5
Z0 = 16 - T
PIVOT_X, PIVOT_Z = T / 2, 16 - T / 2
# The pointed arch: its rise in px, and its span over a pair of leaves and over a single leaf. Each
# side is an arc centred on the springing line, so the arch's radius follows from span and rise.
RISE = 48
SPANS = {"pair": 64, "single": 32}

TINT_FIXED = 1   # SHARED with TileEntityDoorSwingRenderer: drawn where it is, not swung

FACE_TURN = {"south": "east", "north": "west", "east": "north", "west": "south",
             "up": "up", "down": "down"}

# variant -> (wood colour, stud colour, hardware texture, metal?, name in each language)
VARIANTS = {
    "dark_oak": ((70, 46, 27), (34, 32, 32), "cathedral_iron", False,
                 ("Dark Oak", "Roble Oscuro", "Dunkle Eiche", "Mörk Ek")),
    "weathered_oak": ((122, 112, 98), (88, 66, 50), "cathedral_iron", False,
                      ("Weathered Oak", "Roble Envejecido", "Verwitterte Eiche", "Väderbiten Ek")),
    "bronze": ((132, 94, 48), (176, 134, 70), "cathedral_bronze_hw", True,
               ("Bronze", "Bronce", "Bronze", "Brons")),
}

DOOR_NAMES = ("Cathedral Door", "Puerta de Catedral", "Kathedralentür", "Katedraldörr")
ARCH_NAMES = ("Cathedral Door Arch Head", "Cabezal Ojival de Puerta de Catedral",
              "Spitzbogen für Kathedralentür", "Spetsbåge till Katedraldörr")
TYMP_NAMES = ("Tympanum", "Tímpano", "Tympanon", "Tympanon")
PANELLING = {"panelling_carved_dark_oak": ("Carved Panelling (Dark Oak)",
                                           "Panelado Tallado (Roble Oscuro)",
                                           "Geschnitzte Vertäfelung (Dunkle Eiche)",
                                           "Snidad Panel (Mörk Ek)")}
MESSAGES = {
    "gui.csm.door.cathedral.no_room": (
        "A cathedral door needs a clear opening two blocks wide and three high",
        "Una puerta de catedral necesita un hueco libre de dos bloques de ancho y tres de alto",
        "Eine Kathedralentür braucht eine freie Öffnung von zwei Blöcken Breite und drei Höhe",
        "En katedraldörr behöver en fri öppning två block bred och tre hög"),
    "gui.csm.door.cathedral.no_leaf": (
        "Place the arch head on top of a cathedral door, with three clear blocks above it",
        "Coloca el cabezal sobre una puerta de catedral, con tres bloques libres encima",
        "Den Spitzbogen auf eine Kathedralentür setzen, mit drei freien Blöcken darüber",
        "Sätt spetsbågen ovanpå en katedraldörr, med tre fria block ovanför"),
    "gui.csm.door.cathedral.no_tympanum_room": (
        "A tympanum needs a clear space two or four blocks wide and three high",
        "Un tímpano necesita un espacio libre de dos o cuatro bloques de ancho y tres de alto",
        "Ein Tympanon braucht einen freien Raum von zwei oder vier Blöcken Breite und drei Höhe",
        "Ett tympanon behöver ett fritt utrymme två eller fyra block brett och tre högt"),
}


def door_name(v):
    return "cathedral_door_" + v


def arch_name(v):
    return "cathedral_door_%s_arch" % v


def tymp_name(v):
    return "cathedral_tympanum_" + v


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------


def _fill(size, rgb, rng, amount):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = _shift(rgb, rng.uniform(-amount, amount))
    return img, px


def _boards(px, size, rgb, rng, width, metal):
    """Vertical boards with grain running up them, a dark seam between boards; or, for bronze, a
    hammered casting."""
    for x in range(size):
        tone = rng.uniform(-8, 8) if not metal else 0
        for y in range(size):
            if metal:
                px[x, y] = _shift(rgb, rng.uniform(-7, 7))
            else:
                px[x, y] = _shift(rgb, tone + rng.uniform(-4, 4))
        if not metal and x % width == 0:
            for y in range(size):
                px[x, y] = _shift(rgb, -30 + rng.uniform(-3, 3))


def _stud(px, x, y, stud, size=32):
    """A nail head two pixels across, lit from the top left; wraps at the tile edge."""
    x1, y1 = (x + 1) % size, (y + 1) % size
    px[x, y] = _shift(stud, 46)
    px[x1, y] = _shift(stud, 8)
    px[x, y1] = _shift(stud, 4)
    px[x1, y1] = _shift(stud, -22)


def face_texture(v):
    """The leaf's outside face, one cell of it: a studded panel in a raised frame, 32 px."""
    rgb, stud, _, metal, _ = VARIANTS[v]
    rng = random.Random("cathedral_face_" + v)
    img, px = _fill(32, rgb, rng, 3)
    _boards(px, 32, rgb, rng, 8, metal)
    for y in range(32):
        for x in range(32):
            ring = min(x, y, 31 - x, 31 - y)
            if ring < 2:
                lit = (x < 2 and x <= 31 - y) or (y < 2 and y <= 31 - x)
                px[x, y] = _shift(rgb, (16 if lit else -26) + rng.uniform(-3, 3))
            elif ring == 2:
                px[x, y] = _shift(rgb, -32 if (x == 2 or y == 2) else 10)
    for sy in (7, 15, 23):
        for sx in (7, 15, 23):
            _stud(px, sx, sy, stud)
    return img


def field_texture(v):
    """The arch head's outside face: the same boards and studs with no frame, since the arch cuts
    across where a frame would be."""
    rgb, stud, _, metal, _ = VARIANTS[v]
    rng = random.Random("cathedral_field_" + v)
    img, px = _fill(32, rgb, rng, 3)
    _boards(px, 32, rgb, rng, 8, metal)
    for j in range(4):
        for i in range(4):
            _stud(px, 3 + 8 * i + 4 * (j % 2), 3 + 8 * j, stud)
    return img


def inside_texture(v):
    """The inside face: boards with a ledge across each cell (a plain casting for bronze)."""
    rgb, _, _, metal, _ = VARIANTS[v]
    rng = random.Random("cathedral_inside_" + v)
    img, px = _fill(16, rgb, rng, 3)
    _boards(px, 16, _shift(rgb, -6)[:3], rng, 4, metal)
    for x in range(16):
        for y in range(6, 10):
            px[x, y] = _shift(rgb, (18 if y == 6 else -28 if y == 9 else 6) + rng.uniform(-3, 3))
    return img


def edge_texture(v):
    rgb, _, _, _, _ = VARIANTS[v]
    return _fill(16, _shift(rgb, -12)[:3], random.Random("cathedral_edge_" + v), 4)[0]


def hardware_texture(rgb, seed):
    return _fill(16, rgb, random.Random(seed), 6)[0]


def _arch_lo(y, span):
    """Where the arch's left side is at height y (px above the springing), across a span."""
    s = span / 2.0
    radius = (s * s + RISE * RISE) / (2 * s)
    if y >= RISE:
        return s
    return radius - math.sqrt(radius * radius - y * y)


def _near_arc(x, y, x0, x1, rise, width=0.9):
    """Whether (x, y) is on a pointed arch from x0 to x1 rising `rise`, within `width` px."""
    s = (x1 - x0) / 2.0
    r = (s * s + rise * rise) / (2 * s)
    if y < 0 or y > rise + 0.5:
        return False
    if x <= x0 + s:
        return abs(math.hypot(x - (x0 + r), y) - r) < width
    return abs(math.hypot(x - (x1 - r), y) - r) < width


def tympanum_texture(v, span):
    """A carved (or cast) tympanum, 16 px a block: blind tracery inside the arch -- two lancets
    and a ring over a pair's span, a lancet and a ring over a single leaf's. Plain Gothic forms,
    no figures or symbols. The design sits at the top left of a 64 px sheet, Y up from row 48."""
    rgb, _, _, metal, _ = VARIANTS[v]
    rng = random.Random("cathedral_tympanum_%s_%s" % (v, span))
    ground = _shift(rgb, -16)[:3]
    img, px = _fill(64, ground, rng, 3)
    w = SPANS[span]
    line = [[False] * 64 for _ in range(64)]
    for py in range(48):
        for pxi in range(w):
            x, y = pxi + 0.5, 48 - (py + 0.5)
            lo = _arch_lo(y, w)
            on = False
            # The moulding just inside the arch.
            if lo + 1.5 <= x <= w - lo - 1.5:
                inner = _near_arc(x, y, 3, w - 3, RISE - 4)
                on = on or inner
            if span == "pair":
                on = on or (abs(x - 32) < 1 and y < 22)
                on = on or _near_arc(x, y, 4, 31, 20) or _near_arc(x, y, 33, 60, 20)
                on = on or (y < 1.2 and 4 <= x <= 60)
                on = on or abs(math.hypot(x - 32, y - 33) - 8) < 0.9
                on = on or abs(math.hypot(x - 32, y - 33) - 3.5) < 0.8
            else:
                on = on or _near_arc(x, y, 5, 27, 22)
                on = on or (y < 1.2 and 5 <= x <= 27)
                on = on or abs(math.hypot(x - 16, y - 30) - 5.5) < 0.9
            line[py][pxi] = on
    for py in range(48):
        for pxi in range(w):
            if line[py][pxi]:
                px[pxi, py] = _shift(rgb, 22 + rng.uniform(-3, 3))
            elif py > 0 and pxi > 0 and line[py - 1][pxi - 1]:
                px[pxi, py] = _shift(ground, -24)
    if not metal:
        # A little grain in the ground, so carved oak does not read as plaster.
        for pxi in range(w):
            tone = rng.uniform(-5, 5)
            for py in range(48):
                if not line[py][pxi]:
                    r, g, b, a = px[pxi, py]
                    px[pxi, py] = _shift((r, g, b), tone)
    return img


def linenfold_texture():
    """Carved linenfold panelling, dark oak: a raised frame round vertical folds whose ends curl."""
    rgb = VARIANTS["dark_oak"][0]
    rng = random.Random("panelling_linenfold")
    img, px = _fill(32, rgb, rng, 3)
    for y in range(32):
        for x in range(32):
            ring = min(x, y, 31 - x, 31 - y)
            if ring < 3:
                lit = (x < 3 and x <= 31 - y) or (y < 3 and y <= 31 - x)
                px[x, y] = _shift(rgb, (14 if lit else -24) + rng.uniform(-3, 3))
                continue
            phase = 2 * math.pi * (x - 3) / 6.5
            if y in (4, 5, 26, 27):
                phase += math.pi
            tone = 20 * math.sin(phase)
            if y in (3, 28):
                tone = -26
            px[x, y] = _shift(rgb, tone + rng.uniform(-3, 3))
    return img


def door_icon(v):
    rgb, stud, hw, _, _ = VARIANTS[v]
    hw_rgb = (52, 50, 50) if hw == "cathedral_iron" else (96, 70, 38)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (0, 15)
            px[x, y] = _shift(rgb, -24 if edge else (6 if x % 3 else -12))
    for y in (3, 12):
        for x in range(3, 12):
            px[x, y] = _shift(hw_rgb, 10)
    px[10, 8] = px[9, 9] = px[11, 9] = px[10, 10] = _shift(hw_rgb, 30)
    return img


def arch_icon(v):
    rgb, _, _, _, _ = VARIANTS[v]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for py in range(16):
        y = (15.5 - py) * 3
        lo = _arch_lo(y, 48) / 3.0
        for x in range(16):
            if 1 + lo - 0.25 <= x + 0.5 <= 15 - lo + 0.25:
                px[x, py] = _shift(rgb, -20 if py == 15 else (4 if x % 3 else -10))
    return img


def tympanum_icon(v):
    sheet = tympanum_texture(v, "pair").crop((0, 0, 64, 48)).resize((16, 12), Image.BOX)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    mask = arch_icon(v).crop((0, 4, 16, 16))
    for y in range(12):
        for x in range(16):
            if mask.getpixel((x, y))[3]:
                img.putpixel((x, y + 2), sheet.getpixel((x, y)))
    return img


def textures():
    out = {"cathedral_iron": hardware_texture((52, 50, 50), "cathedral_iron"),
           "cathedral_bronze_hw": hardware_texture((98, 72, 40), "cathedral_bronze_hw"),
           "panelling_linenfold_dark_oak": linenfold_texture()}
    for v in VARIANTS:
        out["cathedral_%s_face" % v] = face_texture(v)
        out["cathedral_%s_field" % v] = field_texture(v)
        out["cathedral_%s_inside" % v] = inside_texture(v)
        out["cathedral_%s_edge" % v] = edge_texture(v)
        out["cathedral_%s_tympanum_pair" % v] = tympanum_texture(v, "pair")
        out["cathedral_%s_tympanum_single" % v] = tympanum_texture(v, "single")
        out["cathedral_%s_icon" % v] = door_icon(v)
        out["cathedral_%s_arch_icon" % v] = arch_icon(v)
        out["cathedral_%s_tympanum_icon" % v] = tympanum_icon(v)
    return out

# --------------------------------------------------------------------------------------------
# Geometry -- left hinge, shut, inside to the north
# --------------------------------------------------------------------------------------------


def _but(*leave):
    return tuple(f for f in ("north", "south", "east", "west", "up", "down") if f not in leave)


def _tint(elements, index):
    for e in elements:
        for spec in e["faces"].values():
            spec["tintindex"] = index
    return elements


STRAP_Y = {"bottom": (2.5, 4.5), "top": (11.5, 13.5)}


def straps(col, row):
    """A strap hinge across the leaf at the foot of its bottom row and the head of its top row: on
    the hinge column the strap and the knuckle round its pintle, on the latch column the strap
    running on and tapering to a spear point."""
    if row not in STRAP_Y:
        return []
    y0, y1 = STRAP_Y[row]
    faces = _but("north")
    if col == "hinge":
        return [box(0, y0, 16, 16, y1, 16.5, "#hw", faces=faces),
                box(0, y0 - 0.75, 16, 1.5, y1 + 0.75, 17, "#hw", faces=faces)]
    return [box(0, y0, 16, 9, y1, 16.5, "#hw", faces=faces),
            box(9, y0 + 0.5, 16, 11, y1 - 0.5, 16.5, "#hw", faces=faces),
            box(11, y0 + 0.75, 16, 12.5, y1 - 0.75, 16.5, "#hw", faces=faces)]


def ring_pull():
    """A ring pull on the latch column's bottom row, outside: a rose, its boss, and the ring hung
    from it, an octagon of short bars."""
    f = _but("north")
    z0, z1 = 16.9, 17.5
    out = [box(10.5, 12.5, 16, 13.5, 15.5, 16.4, "#hw", faces=f),
           box(11.5, 13.25, 16.4, 12.5, 14.25, 17.25, "#hw", faces=f)]
    for x0, y0, x1, y1 in ((11, 12.5, 13, 13.25), (11, 8.5, 13, 9.25), (10, 9.5, 10.75, 12.25),
                           (13.25, 9.5, 14, 12.25), (10.5, 8.75, 11.25, 9.5),
                           (12.75, 8.75, 13.5, 9.5), (10.5, 12.25, 11.25, 13),
                           (12.75, 12.25, 13.5, 13)):
        out.append(box(x0, y0, z0, x1, y1, z1, "#hw"))
    return out


def body_cell(col, row):
    """One cell of a leaf below its head: the slab, its edges where they are the leaf's, and the
    ironwork on it."""
    faces = ["south", "north"]
    if col == "hinge":
        faces.append("west")
    else:
        faces.append("east")
    if row == "top":
        faces.append("up")
    if row == "bottom":
        faces.append("down")
    el = box(0, 0, Z0, 16, 16, 16, "#edge", faces=tuple(faces))
    el["faces"]["south"]["texture"] = "#face"
    el["faces"]["north"]["texture"] = "#inside"
    out = [el] + straps(col, row)
    if col == "latch" and row == "bottom":
        out += ring_pull()
    return out


def arch_rows(c, r, span):
    """The pixel rows of cell column c (0 at the arch's left springing) and course r of an arch
    over `span` px, merged where the leaf is the same width: (y0, y1, lo, hi) in the cell's px,
    lo and hi None where the row has no leaf."""
    rows = []
    for j in range(16):
        y = 16 * r + j + 0.5
        lo = round(_arch_lo(y, span))
        hi = span - lo
        a, b = max(lo - 16 * c, 0), min(hi - 16 * c, 16)
        key = (a, b) if a < b else (None, None)
        if rows and rows[-1][2:] == key:
            rows[-1] = (rows[-1][0], j + 1) + key
        else:
            rows.append((j, j + 1) + key)
    return rows


def arch_cell(c, r, span, outside, keep_edges, tymp_uv=False):
    """(leaf, spandrel) of one cell of an arch: the leaf inside the arch, a stepped element a run of
    rows, and the stone outside it in the leaf's plane (TINT_FIXED). Side faces are kept where they
    are the arch's curve, or at the cell edges in `keep_edges` (span px)."""
    leaf, stone = [], []
    for y0, y1, a, b in arch_rows(c, r, span):
        if a is None:
            stone.append(box(0, y0, Z0, 16, y1, 16, "#stone", faces=("north", "south")))
            continue
        faces = ["south", "north", "up"]
        for name, x in (("west", a), ("east", b)):
            span_x = 16 * c + x
            if x % 16 or span_x in keep_edges:
                faces.append(name)
        el = box(a, y0, Z0, b, y1, 16, "#edge", faces=tuple(faces))
        el["faces"]["south"]["texture"] = outside
        el["faces"]["north"]["texture"] = "#inside"
        if tymp_uv:
            el["faces"]["south"]["uv"] = [round((16 * c + a) / 4.0, 4),
                                          round((48 - 16 * r - y1) / 4.0, 4),
                                          round((16 * c + b) / 4.0, 4),
                                          round((48 - 16 * r - y0) / 4.0, 4)]
        leaf.append(el)
        if a > 0:
            stone.append(box(0, y0, Z0, a, y1, 16, "#stone", faces=("north", "south", "east")))
        if b < 16:
            stone.append(box(b, y0, Z0, 16, y1, 16, "#stone", faces=("north", "south", "west")))
    return leaf, _tint(stone, TINT_FIXED)


def _turn(elements, px, pz):
    """A quarter turn anticlockwise (seen from above) about (px, pz): (x, z) -> (z - pz + px,
    pz + px - x), the faces renamed for the way they then face and their UVs taken from where they
    now are (a whole block away is shifted back into the texture, so the latch column's piece in
    the next cell keeps its window)."""
    out = []
    for e in elements:
        m = copy.deepcopy(e)
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        m["from"] = [round(z0 - pz + px, 4), y0, round(pz + px - x1, 4)]
        m["to"] = [round(z1 - pz + px, 4), y1, round(pz + px - x0, 4)]
        m["faces"] = {FACE_TURN[face]: dict(spec) for face, spec in e["faces"].items()}
        out.append(sc._reuv(m))
    return out


def _pivot(col):
    """The hinge pivot in a column's own cell: the latch column's is a block to its west."""
    return (PIVOT_X if col == "hinge" else PIVOT_X - 16), PIVOT_Z


def _rounded(elements):
    for e in elements:
        e["from"] = [round(v, 4) for v in e["from"]]
        e["to"] = [round(v, 4) for v in e["to"]]
    return elements


def geometry():
    """Every geometry parent: name -> elements."""
    out = {}
    for col in ("hinge", "latch"):
        for row in ("bottom", "middle", "top"):
            left = body_cell(col, row)
            opened = _turn(left, *_pivot(col))
            base = "cathedral_geo_body_%s_%s" % (col, row)
            out[base + "_left"] = left
            out[base + "_right"] = sc._mirror_x(left)
            out[base + "_left_open"] = opened
            out[base + "_right_open"] = sc._mirror_x(opened)
        for span in SPANS:
            for course in range(3):
                c = 0 if col == "hinge" else 1
                leaf, stone = arch_cell(c, course, SPANS[span], "#field", (0, 32))
                base = "cathedral_geo_arch_%s_%s_%d" % (span, col, course)
                opened = _turn(leaf, *_pivot(col))
                out[base + "_left"] = leaf
                out[base + "_right"] = sc._mirror_x(leaf)
                out[base + "_left_open"] = opened
                out[base + "_right_open"] = sc._mirror_x(opened)
                if stone:
                    # The spandrel: its own part, the same open or shut, for every variant.
                    sp = "cathedral_geo_spandrel_%s_%s_%d" % (span, col, course)
                    out[sp + "_left"] = stone
                    out[sp + "_right"] = sc._mirror_x(stone)
    for span, w in SPANS.items():
        for c in range(w // 16):
            for course in range(3):
                leaf, _ = arch_cell(c, course, w, "#tymp", (), tymp_uv=True)
                out["cathedral_geo_tymp_%s_%d_%d" % (span, c, course)] = leaf
    return {k: _rounded(v) for k, v in out.items()}


def _door_textures(v):
    hw = VARIANTS[v][2]
    return {"particle": TEX_REF % ("cathedral_%s_face" % v),
            "face": TEX_REF % ("cathedral_%s_face" % v),
            "field": TEX_REF % ("cathedral_%s_field" % v),
            "inside": TEX_REF % ("cathedral_%s_inside" % v),
            "edge": TEX_REF % ("cathedral_%s_edge" % v),
            "hw": TEX_REF % hw}


def _tymp_textures(v, span):
    return {"particle": TEX_REF % ("cathedral_%s_edge" % v),
            "tymp": TEX_REF % ("cathedral_%s_tympanum_%s" % (v, span)),
            "inside": TEX_REF % ("cathedral_%s_inside" % v),
            "edge": TEX_REF % ("cathedral_%s_edge" % v)}


def models():
    out = {}
    geo = geometry()
    for name, elements in geo.items():
        out[name] = {"elements": elements}
    for v in VARIANTS:
        for name in geo:
            rest = name[len("cathedral_geo_"):]
            if rest.startswith("spandrel_"):
                continue
            if rest.startswith("tymp_"):
                span = rest.split("_")[1]
                tex = _tymp_textures(v, span)
            else:
                tex = _door_textures(v)
            out["cathedral_%s_%s" % (v, rest)] = {"parent": PARENT_REF % name, "textures": tex}
        for kind in ("", "_arch", "_tympanum"):
            out["cathedral_%s%s_inventory" % (v, kind)] = {
                "parent": "item/generated",
                "textures": {"layer0": TEX_REF % ("cathedral_%s%s_icon" % (v, kind))}}
    for name in geo:
        if name.startswith("cathedral_geo_spandrel_"):
            rest = name[len("cathedral_geo_spandrel_"):]
            for stone, tex in STONES.items():
                out["cathedral_spandrel_%s_%s" % (stone, rest)] = {
                    "parent": PARENT_REF % name, "textures": {"particle": tex, "stone": tex}}
    out["panelling_carved_dark_oak"] = {
        "parent": "block/cube_column",
        "textures": {"end": TEX_REF % "cathedral_dark_oak_edge",
                     "side": TEX_REF % "panelling_linenfold_dark_oak"}}
    return out

# --------------------------------------------------------------------------------------------
# Blockstates
# --------------------------------------------------------------------------------------------

SIDES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def _part(when, model, rot):
    apply = {"model": MODEL_REF % model}
    if rot:
        apply["y"] = rot
    return {"when": when, "apply": apply}


def door_state(v):
    parts = []
    for side, rot in SIDES:
        for hinge in ("left", "right"):
            for open_ in (False, True):
                suffix = hinge + ("_open" if open_ else "")
                for col in ("hinge", "latch"):
                    for row in ("bottom", "middle", "top"):
                        when = {"facing": side, "hinge": hinge, "open": str(open_).lower(),
                                "column": col, "row": row, "swing": "false"}
                        parts.append(_part(when, "cathedral_%s_body_%s_%s_%s"
                                           % (v, col, row, suffix), rot))
    return {"variants": {"inventory": {"model": MODEL_REF % ("cathedral_%s_inventory" % v)}},
            "multipart": parts}


def arch_state(v, spandrels):
    parts = []
    for side, rot in SIDES:
        for hinge in ("left", "right"):
            for open_ in (False, True):
                suffix = hinge + ("_open" if open_ else "")
                for col in ("hinge", "latch"):
                    for course in range(3):
                        for paired in (True, False):
                            when = {"facing": side, "hinge": hinge, "open": str(open_).lower(),
                                    "column": col, "course": str(course),
                                    "paired": str(paired).lower(), "swing": "false"}
                            span = "pair" if paired else "single"
                            parts.append(_part(when, "cathedral_%s_arch_%s_%s_%d_%s"
                                               % (v, span, col, course, suffix), rot))
                            sp = "%s_%s_%d_%s" % (span, col, course, hinge)
                            if open_ or sp not in spandrels:
                                continue
                            # The spandrel: one part whether the leaf is open or shut.
                            for stone in STONES:
                                when = {"facing": side, "hinge": hinge, "column": col,
                                        "course": str(course), "paired": str(paired).lower(),
                                        "stone": stone, "swing": "false"}
                                parts.append(_part(when, "cathedral_spandrel_%s_%s"
                                                   % (stone, sp), rot))
    return {"variants": {"inventory": {"model": MODEL_REF % ("cathedral_%s_arch_inventory" % v)}},
            "multipart": parts}


# A tympanum column's spandrel: the head cell it stands in for (column, hand).
TYMP_SPANDREL = {0: ("hinge", "left"), 1: ("latch", "left"), 2: ("latch", "right"),
                 3: ("hinge", "right")}


def tymp_state(v, spandrels):
    parts = []
    for col in range(4):
        for course in range(3):
            for side, rot in SIDES:
                for paired in (False, True):
                    span = "pair" if paired or col >= 2 else "single"
                    when = {"col": str(col), "course": str(course), "facing": side,
                            "paired": str(paired).lower()}
                    parts.append(_part(when, "cathedral_%s_tymp_%s_%d_%d"
                                       % (v, span, col, course), rot))
                    sp = "%s_%s_%d_%s" % ((span, TYMP_SPANDREL[col][0], course,
                                           TYMP_SPANDREL[col][1]))
                    if sp not in spandrels:
                        continue
                    for stone in STONES:
                        parts.append(_part(dict(when, stone=stone),
                                           "cathedral_spandrel_%s_%s" % (stone, sp), rot))
    return {"variants": {"inventory": {"model": MODEL_REF % ("cathedral_%s_tympanum_inventory"
                                                             % v)}},
            "multipart": parts}


def blockstates():
    out = {}
    spandrels = {n[len("cathedral_geo_spandrel_"):] for n in geometry()
                 if n.startswith("cathedral_geo_spandrel_")}
    for v in VARIANTS:
        out[door_name(v)] = door_state(v)
        out[arch_name(v)] = arch_state(v, spandrels)
        out[tymp_name(v)] = tymp_state(v, spandrels)
    out["panelling_carved_dark_oak"] = {"variants": {
        "normal": {"model": MODEL_REF % "panelling_carved_dark_oak"},
        "inventory": {"model": MODEL_REF % "panelling_carved_dark_oak"}}}
    return out

# --------------------------------------------------------------------------------------------
# Lang and tab lines
# --------------------------------------------------------------------------------------------


def _named(base, v):
    return tuple("%s (%s)" % (b, n) for b, n in zip(base, VARIANTS[v][4]))


def lang_entries(langs):
    out = []
    for v in VARIANTS:
        out.append(("tile.%s.name" % door_name(v), dict(zip(langs, _named(DOOR_NAMES, v)))))
        out.append(("tile.%s.name" % arch_name(v), dict(zip(langs, _named(ARCH_NAMES, v)))))
        out.append(("tile.%s.name" % tymp_name(v), dict(zip(langs, _named(TYMP_NAMES, v)))))
    for name, names in PANELLING.items():
        out.append(("tile.%s.name" % name, dict(zip(langs, names))))
    for key, names in MESSAGES.items():
        out.append((key, dict(zip(langs, names))))
    return out


def tab_lines():
    lines = []
    for v in VARIANTS:
        lines.append('    initTabBlock(new BlockCathedralDoor("%s")); // %s'
                     % (door_name(v), _named(DOOR_NAMES, v)[0]))
        lines.append('    initTabBlock(new BlockCathedralDoorArch("%s")); // %s'
                     % (arch_name(v), _named(ARCH_NAMES, v)[0]))
    for v in VARIANTS:
        lines.append('    initTabBlock(new BlockCathedralTympanum("%s")); // %s'
                     % (tymp_name(v), _named(TYMP_NAMES, v)[0]))
    for name, names in PANELLING.items():
        lines.append('    initTabBlock(new BlockCarvedPanelling("%s")); // %s' % (name, names[0]))
    return lines
