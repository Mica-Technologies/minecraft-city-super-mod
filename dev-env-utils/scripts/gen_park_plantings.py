#!/usr/bin/env python3
"""
gen_park_plantings.py -- the Parks & Greenery street-tree accessories and plantings.

Everything here is a JSON-element or cross model; nothing is drawn in Java. One catalogue
(BLOCKS) says for each block which class constructs it, how it is drawn and what it is called in
the four languages, and this script writes, under modules/parks/.../assets/csm:

  * textures/blocks/parks/landscape/*.png   every texture, drawn here from noise and palettes
  * models/block/parks/landscape/*.json     the models
  * blockstates/<registry>.json              forge variants, or multipart for the joining blocks
  * models/item/<registry>.json              for the multipart blocks, which have no inventory
                                              variant
  * lang lines in all four languages, kept in place by key

The classes (BlockParkProp, BlockParkJoining, BlockParkFacing) take their size from the
constructor; --fragments prints the tab registration lines, which carry those sizes, so the
Java and the models are written from the same numbers.

Street trees stand ON a tree grate or pit (full ground blocks), so nothing here shares a cell
with a trunk: the hoop fence goes round the pit, and a stake stands in the next cell with its
tie reaching back to the trunk.

The hanging baskets are side-mounted accessories (ICsmPoleFitted): each has _thin and _pedestal
model copies with the bracket plate moved back to the thinner pole's skin, as
gen_pole_fit_models.py does for the light mounts (models face north, the pole behind at +Z, its
skin at z = 24 - r).

Usage:
    python gen_park_plantings.py              # write everything
    python gen_park_plantings.py --check      # fail if the tree has drifted
    python gen_park_plantings.py --fragments  # print the tab registration lines

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
import gen_trees  # noqa: E402
import model_depth  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "modules", "parks", "src", "main", "resources", "assets", "csm")
LOCALES = ["en_us", "de_de", "es_es", "sv_se"]
TEX = "csm:blocks/parks/landscape/"
MODEL = "csm:parks/landscape/"


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def noise_tex(palette, seed, size=16, cells=4, grain=0.6, speckle=0.0, speck_colour=None):
    """Tileable value noise mapped onto a palette (light to dark), with optional speckles."""
    rng = random.Random(seed)
    field = gen_trees._noise(rng, cells, size)
    fine = gen_trees._noise(rng, max(2, size // 2), size)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    n = len(palette)
    for y in range(size):
        for x in range(size):
            v = field[y][x] * (1 - grain) + fine[y][x] * grain + rng.uniform(-0.25, 0.25)
            i = int((v + 1) / 2 * n)
            c = palette[max(0, min(n - 1, i))]
            if speckle and rng.random() < speckle:
                c = speck_colour[rng.randrange(len(speck_colour))]
            px[x, y] = clamp(c) + (255,)
    return img


CONCRETE = [(188, 186, 180), (176, 174, 168), (164, 162, 156), (150, 148, 142)]
IRON = [(70, 70, 72), (58, 58, 60), (46, 46, 48), (36, 36, 38)]
MULCH = [(122, 84, 52), (104, 70, 42), (86, 56, 34), (66, 42, 26)]
SOIL = [(96, 72, 52), (82, 60, 42), (68, 50, 34), (54, 40, 28)]
GRAVEL = [(176, 168, 152), (150, 142, 128), (124, 118, 106), (98, 94, 86)]
DG = [(200, 170, 128), (186, 154, 112), (170, 140, 98), (150, 122, 84)]
TURF = [(92, 158, 62), (80, 142, 54), (70, 128, 46), (60, 112, 40)]
BOXWOOD = [(74, 112, 50), (62, 98, 42), (50, 84, 34), (40, 70, 28)]
PRIVET = [(104, 146, 64), (90, 130, 56), (76, 114, 46), (62, 98, 38)]
JUNIPER = [(96, 132, 114), (80, 116, 98), (66, 100, 84), (52, 84, 70)]
CEDAR = [(170, 116, 76), (152, 102, 66), (132, 88, 56), (112, 74, 46)]
CORTEN = [(150, 82, 44), (134, 70, 36), (116, 60, 30), (96, 50, 26)]
STAKE = [(200, 176, 132), (184, 160, 118), (166, 142, 104), (146, 124, 90)]
FLOWERS = {
    "red": [(214, 40, 48), (182, 26, 36)],
    "yellow": [(246, 208, 52), (224, 176, 32)],
    "purple": [(142, 72, 176), (116, 52, 150)],
    "pink": [(236, 120, 170), (210, 90, 146)],
    "white": [(244, 242, 236), (216, 214, 206)],
}
LEAF = [(84, 132, 58), (68, 114, 48), (54, 96, 40)]


def grate(style, seed):
    rng = random.Random(seed)
    img = noise_tex(CONCRETE, seed, grain=0.8)
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            if r < 2.6:
                c = MULCH[rng.randrange(4)]  # the opening round the trunk
            elif style == "square":
                if x in (0, 15) or y in (0, 15):
                    c = IRON[0]  # frame
                elif r < 3.6:
                    c = IRON[1]  # the collar round the opening
                else:
                    # Radiating slots: dark soil between iron bars.
                    ang = math.atan2(dy, dx)
                    slot = (ang * 12 / math.pi) % 2 < 0.8 and 1 < x < 14 and 1 < y < 14
                    c = MULCH[3] if slot and r > 4.2 else IRON[rng.randrange(1, 3)]
            else:
                if r > 7.6:
                    continue  # concrete pad outside the round grate
                if r > 6.8 or r < 3.6:
                    c = IRON[1]
                else:
                    ring = int((r - 3.6) * 1.6) % 2 == 0
                    ang = math.atan2(dy, dx)
                    bar = (ang * 8 / math.pi) % 2 < 0.35
                    c = IRON[2] if (bar or not ring) else MULCH[3]
            px[x, y] = clamp(c) + (255,)
    return img


def tree_pit(seed):
    img = noise_tex(MULCH, seed, grain=0.8)
    px = img.load()
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px[x, y] = clamp(CONCRETE[1]) + (255,)
    return img


def leafy(palette, seed, flowers=None, density=0.0, size=16):
    """A dense clipped-leaf texture, optionally flecked with flower clusters."""
    rng = random.Random(seed)
    img = noise_tex(palette, seed, size=size, cells=4, grain=0.9)
    px = img.load()
    for _ in range(size * size // 6):
        x, y = rng.randrange(size), rng.randrange(size)
        px[x, y] = clamp(palette[0 if rng.random() < 0.5 else -1]) + (255,)
    if flowers:
        for _ in range(int(size * size * density / 5)):
            cx, cy = rng.randrange(size), rng.randrange(size)
            for ox, oy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
                if rng.random() < 0.8:
                    px[(cx + ox) % size, (cy + oy) % size] = clamp(
                        flowers[rng.randrange(len(flowers))]) + (255,)
    return img


def grass_tex(kind, seed):
    """A cross texture: blades from the bottom, a plume on top where the species has one."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    if kind == "fountain":
        blades, green, plume = 24, [(104, 150, 70), (86, 128, 58), (120, 162, 82)], (206, 186, 150)
    elif kind == "feather_reed":
        blades, green, plume = 16, [(128, 150, 84), (110, 132, 70)], (196, 168, 110)
    else:
        blades, green, plume = 26, [(128, 162, 176), (108, 144, 160), (146, 178, 188)], None
    top = 15 if kind != "blue_fescue" else 9
    for b in range(blades):
        x = 7.5 + rng.uniform(-4.5, 4.5)
        # Blades lean away from the middle of the clump, most of all a fountain grass's.
        lean = (x - 7.5) / 4.5 * rng.uniform(0.2, 0.6) * (1.6 if kind == "fountain" else 0.7)
        h = rng.randint(top - 5, top) if kind != "blue_fescue" else rng.randint(5, 9)
        for k in range(h):
            y = 15 - k
            xx = int(round(x + lean * k + (lean * k * k * 0.04 if kind == "fountain" else 0)))
            if 0 <= xx < 16:
                px[xx, y] = green[rng.randrange(len(green))] + (255,)
        if plume and b % 2 == 0:
            for k in range(h - 4, h):
                y = 15 - k
                xx = int(round(x + lean * k + (lean * k * k * 0.04 if kind == "fountain" else 0)))
                for w in (0, 1) if kind == "fountain" else (0,):
                    if 0 <= xx + w < 16 and 0 <= y < 16:
                        px[xx + w, y] = clamp(tuple(c + rng.randint(-10, 10) for c in plume)) + (255,)
    return img


def flower_tex(colours, seed, trailing=False):
    """Low bedding flowers: foliage in the lower half, blooms on it; transparent above."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    lo = 0 if trailing else 8
    for _ in range(90):
        x, y = rng.randrange(16), rng.randrange(lo, 16)
        px[x, y] = LEAF[rng.randrange(3)] + (255,)
    for _ in range(16 if not trailing else 22):
        cx, cy = rng.randrange(16), rng.randrange(lo, 15)
        c = colours[rng.randrange(len(colours))]
        for ox, oy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if rng.random() < 0.85 and 0 <= cx + ox < 16:
                px[cx + ox, cy + oy] = clamp(c[0] if (ox + oy) % 2 == 0 else c[1]) + (255,)
        px[cx, cy] = (250, 232, 120, 255) if rng.random() < 0.3 else px[cx, cy]
    return img


def boards(palette, seed):
    """Horizontal boards with dark joints every four pixels."""
    img = noise_tex(palette, seed, grain=0.4)
    px = img.load()
    for y in range(0, 16, 4):
        for x in range(16):
            px[x, y] = clamp(palette[-1]) + (255,)
    return img


def planter_top(side_palette, seed, wood=False):
    img = noise_tex(SOIL, seed + 1, grain=0.8)
    rim = boards(side_palette, seed) if wood else noise_tex(side_palette, seed, grain=0.5)
    ip, rp = img.load(), rim.load()
    for y in range(16):
        for x in range(16):
            if x < 2 or x > 13 or y < 2 or y > 13:
                ip[x, y] = rp[x, y]
    return img


def hoop_tex():
    """A hoop fence arch, 16 across and 7 high, peaking at x = 8 (the post), feet at the edges."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    black = (34, 36, 38, 255)
    prev = None
    for x in range(16):
        y = int(round(15 - 7 * math.sin(math.pi * (x + 0.5) / 16)))
        px[x, y] = black
        # Fill the step to the last column so the steep ends read as a continuous bar.
        if prev is not None:
            for yy in range(min(prev, y), max(prev, y) + 1):
                px[x, yy] = black
        prev = y
    return img


def wire_tex(seed):
    """A hanging basket's wire frame over a coir liner."""
    img = noise_tex([(120, 94, 62), (104, 80, 52), (88, 66, 42)], seed, grain=0.9)
    px = img.load()
    for i in range(16):
        for x in (0, 5, 10, 15):
            px[x, i] = (30, 42, 32, 255)
        if i % 5 == 0:
            for x in range(16):
                px[x, i] = (30, 42, 32, 255)
    return img


def solid(colour):
    img = Image.new("RGBA", (16, 16), clamp(colour) + (255,))
    return img


TEXTURES = {
    "grate_square": lambda: grate("square", 1),
    "grate_round": lambda: grate("round", 2),
    "concrete": lambda: noise_tex(CONCRETE, 3, grain=0.8),
    "tree_pit": lambda: tree_pit(4),
    "steel": lambda: noise_tex([(48, 50, 52), (38, 40, 42), (30, 32, 34)], 5, grain=0.9),
    "hoop": hoop_tex,
    "stake": lambda: boards(STAKE, 6),
    "tie": lambda: noise_tex([(62, 112, 60), (52, 98, 50)], 7),
    "wire": lambda: wire_tex(8),
    "petunia": lambda: flower_tex([FLOWERS["red"], FLOWERS["pink"]], 9, trailing=True),
    "mixed_basket": lambda: flower_tex([FLOWERS["purple"], FLOWERS["white"], FLOWERS["yellow"]],
                                       10, trailing=True),
    "boxwood": lambda: leafy(BOXWOOD, 11),
    "privet": lambda: leafy(PRIVET, 12),
    "hydrangea": lambda: leafy(BOXWOOD, 13, [(118, 140, 214), (150, 170, 232), (196, 150, 210)],
                               density=0.55),
    "juniper": lambda: leafy(JUNIPER, 14),
    "grass_fountain": lambda: grass_tex("fountain", 15),
    "grass_feather_reed": lambda: grass_tex("feather_reed", 16),
    "grass_blue_fescue": lambda: grass_tex("blue_fescue", 17),
    "bed_red": lambda: flower_tex([FLOWERS["red"]], 18),
    "bed_yellow": lambda: flower_tex([FLOWERS["yellow"]], 19),
    "bed_purple": lambda: flower_tex([FLOWERS["purple"]], 20),
    "bed_mixed": lambda: flower_tex([FLOWERS["red"], FLOWERS["yellow"], FLOWERS["purple"],
                                     FLOWERS["white"]], 21),
    "mulch": lambda: noise_tex(MULCH, 22, grain=0.9),
    "pea_gravel": lambda: noise_tex(GRAVEL, 23, cells=8, grain=0.95,
                                    speckle=0.15, speck_colour=[(210, 200, 184), (90, 84, 76)]),
    "decomposed_granite": lambda: noise_tex(DG, 24, grain=0.9, speckle=0.1,
                                            speck_colour=[(226, 204, 170), (130, 104, 72)]),
    "turf": lambda: noise_tex(TURF, 25, cells=8, grain=0.95),
    "soil": lambda: noise_tex(SOIL, 26, grain=0.8),
    "planter_concrete": lambda: noise_tex(CONCRETE, 27, grain=0.5),
    "planter_wood": lambda: boards(CEDAR, 28),
    "planter_corten": lambda: noise_tex(CORTEN, 29, grain=0.7),
    "planter_concrete_top": lambda: planter_top(CONCRETE, 30),
    "planter_wood_top": lambda: planter_top(CEDAR, 31, wood=True),
    "planter_corten_top": lambda: planter_top(CORTEN, 32),
}


# ------------------------------------------------------------------------------------------
# Model helpers
# ------------------------------------------------------------------------------------------
def face(tex, uv=None, cull=None):
    f = {"texture": "#" + tex}
    if uv is not None:
        f["uv"] = [round(v, 3) for v in uv]
    if cull:
        f["cullface"] = cull
    return f


def box(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), per=None):
    """An element with UVs fitted to its own size, so nothing stretches."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    uv = {
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        "south": [x0, 16 - y1, x1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
        "west": [z0, 16 - y1, z1, 16 - y0],
        "up": [x0, z0, x1, z1],
        "down": [x0, 16 - z1, x1, 16 - z0],
    }
    out = {}
    for f in faces:
        u = uv[f]
        # Keep UVs inside 0..16 for elements that reach past the cell.
        u = [min(16, max(0, v)) for v in u]
        if u[0] == u[2]:
            u[2] = u[0] + 0.01
        if u[1] == u[3]:
            u[3] = u[1] + 0.01
        out[f] = face((per or {}).get(f, tex), u)
    return {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": out}


def model(textures, elements, parent="block/block", ao=True):
    m = {"parent": parent, "textures": dict(textures)}
    if not ao:
        m["ambientocclusion"] = False
    if elements:
        m["elements"] = elements
    return m


def wbox(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), per=None):
    """box(), for an element reaching past the cell: each face's UV window is slid into 0..16
    whole rather than clamped, so it keeps its size instead of stretching."""
    el = box([min(15.99, max(0, v)) for v in frm], [min(16, max(0.01, v)) for v in to], tex,
             faces, per)
    el["from"], el["to"] = [round(v, 3) for v in frm], [round(v, 3) for v in to]
    x0, y0, z0 = frm
    x1, y1, z1 = to
    raw = {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
           "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
           "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}
    for f in el["faces"]:
        u = raw[f]
        for a, b in ((0, 2), (1, 3)):
            if u[a] < 0:
                u[b] -= u[a]
                u[a] = 0
            if u[b] > 16:
                u[a] -= u[b] - 16
                u[b] = 16
        el["faces"][f]["uv"] = [round(v, 3) for v in u]
    return el


def plane(frm, to, tex, uv=(0, 0, 16, 16), angle=0, origin=None):
    """A zero-thickness card, drawn from both sides: a crop plant, a seedling row, a trellis."""
    axis_z = frm[2] == to[2]
    faces = ("north", "south") if axis_z else ("east", "west")
    el = {"from": list(frm), "to": list(to), "shade": False,
          "faces": {f: face(tex, list(uv)) for f in faces}}
    if angle:
        el["rotation"] = {"origin": origin or [8, 8, 8], "axis": "y", "angle": angle,
                          "rescale": False}
    return el


def cross_planes(tex, height, uv_top=0, four=False):
    """Crossed planes, as a plant is drawn, cut to a height: two at 45 degrees, or four at
    22.5 either side of each axis for a fuller clump (an element turns at most 45 degrees, in
    steps of 22.5)."""
    els = []
    planes = [("x", 22.5), ("x", -22.5), ("z", 22.5), ("z", -22.5)] if four else         [("x", 45), ("x", -45)]
    for along, angle in planes:
        if along == "x":
            frm, to, faces = [0.8, 0, 8], [15.2, height, 8], ("north", "south")
        else:
            frm, to, faces = [8, 0, 0.8], [8, height, 15.2], ("east", "west")
        els.append({
            "from": frm, "to": to,
            "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": angle, "rescale": four is False},
            "shade": False,
            "faces": {f: face(tex, [0, uv_top, 16, 16]) for f in faces},
        })
    return els


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
# Each entry: registry -> dict(java=..., names=(en, de, es, sv), build=callable returning
# {"models": {name: json}, "blockstate": json, "item": json or None}).
BLOCKS = []


def T(name):
    return TEX + name


def simple_state(model_name, item=None):
    variants = {"normal": [{}], "inventory": [{"model": item}] if item else [{}]}
    return {"forge_marker": 1, "defaults": {"model": MODEL + model_name}, "variants": variants}


def add(registry, java, names, models, blockstate, item=None):
    BLOCKS.append({"registry": registry, "java": java, "names": names, "models": models,
                   "blockstate": blockstate, "item": item})


def prop(registry, kind, height, inset, names, models, blockstate, item=None):
    add(registry, 'new BlockParkProp("%s", BlockParkProp.Kind.%s, %d, %d)'
        % (registry, kind, height, inset), names, models, blockstate, item)


# --- tree grates and pits: full ground blocks a tree stands on ---
for gid, names in (
        ("square", ("Square Tree Grate", "Quadratischer Baumrost", "Alcorque cuadrado",
                    "Fyrkantigt trädgaller")),
        ("round", ("Round Tree Grate", "Runder Baumrost", "Alcorque redondo",
                   "Runt trädgaller"))):
    reg = "tree_grate_" + gid
    prop(reg, "GROUND", 16, 0, names,
         {reg: {"parent": "block/cube_bottom_top",
                "textures": {"top": T("grate_" + gid), "side": T("concrete"),
                             "bottom": T("concrete"), "particle": T("concrete")}}},
         simple_state(reg))
prop("tree_pit_mulch", "GROUND", 16, 0,
     ("Mulched Tree Pit", "Gemulchte Baumscheibe", "Alcorque con mantillo",
      "Trädgrop med täckbark"),
     {"tree_pit_mulch": {"parent": "block/cube_bottom_top",
                         "textures": {"top": T("tree_pit"), "side": T("concrete"),
                                      "bottom": T("concrete"), "particle": T("tree_pit")}}},
     simple_state("tree_pit_mulch"))


# --- joining blocks: multipart on four actual-state sides ---
SIDES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def joining(registry, kind, height, width, names, post, side, item_extra=(), side_when="true",
            corner=None):
    """post: elements always drawn; side: elements drawn toward north, turned for the others.

    A hedge or fence draws a side where it joins (side_when "true"); a bed draws its wall where
    it does not ("false"), so a run of beds is one bed walled only round the outside.

    corner: the north-east corner post of a walled block, turned for the other three and drawn
    wherever either wall beside it is. A bed's wall then stops short of the corners, so two
    walls never overlap there: turned, their tops carry turned pixels on one plane, which
    z-fight."""
    models = {registry + "_post": post, registry + "_side": side}
    parts = [{"apply": {"model": MODEL + registry + "_post"}}]
    for direction, rot in SIDES:
        apply = {"model": MODEL + registry + "_side"}
        if rot:
            apply["y"] = rot
            apply["uvlock"] = False
        parts.append({"when": {direction: side_when}, "apply": apply})
    if corner is not None:
        models[registry + "_corner"] = corner
        for i, (direction, rot) in enumerate(SIDES):
            apply = {"model": MODEL + registry + "_corner"}
            if rot:
                apply["y"] = rot
                apply["uvlock"] = False
            after = SIDES[(i + 1) % 4][0]
            parts.append({"when": {"OR": [{direction: side_when}, {after: side_when}]},
                          "apply": apply})
    # The item: a post and a side either way, so the icon reads as a run.
    item_model = {"parent": "csm:block/parks/landscape/" + registry + "_item"}
    models[registry + "_item"] = {
        "parent": "block/block",
        "textures": post["textures"],
        "elements": post.get("elements", []) + (side.get("elements", []) if side_when == "true"
                                                else []) + list(item_extra),
        "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0],
                            "scale": [0.625, 0.625, 0.625]}},
    }
    add(registry, 'new BlockParkJoining("%s", BlockParkJoining.Kind.%s, %d, %d)'
        % (registry, kind, height, width), names, models, {"multipart": parts}, item_model)


for species, names_sp in (("boxwood", ("Boxwood", "Buchsbaum", "boj", "buxbom")),
                          ("privet", ("Privet", "Liguster", "aligustre", "liguster"))):
    for height, hname in ((10, "low"), (16, "tall")):
        reg = "hedge_%s_%s" % (species, hname)
        tex = {"leaves": T(species), "particle": T(species)}
        lo, hi = 1, 15
        post = model(tex, [box([lo, 0, lo], [hi, height, hi], "leaves")])
        side = model(tex, [box([lo, 0, 0], [hi, height, lo], "leaves",
                               faces=("east", "west", "up", "down", "north"))])
        en = "%s %s Hedge" % ("Low" if hname == "low" else "Tall", names_sp[0])
        de = "%s (%s)" % ("Niedrige Hecke" if hname == "low" else "Hohe Hecke", names_sp[1])
        es = "Seto %s de %s" % ("bajo" if hname == "low" else "alto", names_sp[2])
        sv = "%s häck av %s" % ("Låg" if hname == "low" else "Hög", names_sp[3])
        joining(reg, "HEDGE", height, 14, (en, de, es, sv), post, side)

fence_tex = {"steel": T("steel"), "hoop": T("hoop"), "particle": T("steel")}
# Each arm carries half an arch whose peak is at the post, so a run reads as one row of hoops.
fence_post = model(fence_tex, [box([7.5, 0, 7.5], [8.5, 8, 8.5], "steel")])
fence_side = model(fence_tex, [
    {"from": [8, 0, 0], "to": [8, 8, 8], "shade": False,
     "faces": {"east": face("hoop", [8, 8, 16, 16]), "west": face("hoop", [0, 8, 8, 16])}},
])
joining("tree_pit_fence", "FENCE", 9, 2,
        ("Tree Pit Hoop Fence", "Baumscheiben-Bügelzaun", "Valla de arcos para alcorque",
         "Bågstaket kring trädgrop"), fence_post, fence_side)

for mat, names_m in (("concrete", ("Concrete", "Beton", "hormigón", "betong")),
                     ("wood", ("Cedar", "Zedernholz", "cedro", "ceder")),
                     ("corten", ("Corten Steel", "Cortenstahl", "acero corten", "cortenstål"))):
    reg = "raised_bed_" + mat
    tex = {"wall": T("planter_" + mat), "soil": T("soil"), "particle": T("planter_" + mat)}
    # The soil is only its top (and its underside): its sides would lie on the walls' outer
    # faces, or face a joined bed's soil across the block line.
    post = model(tex, [box([0, 0, 0], [16, 10, 16], "soil", faces=("up", "down"))])
    # A wall stops at the corners, which have posts of their own (see joining), and has no end
    # faces, since a corner post always stands at each end of it.
    side = model(tex, [box([2, 0, 0], [14, 12, 2], "wall", faces=("north", "south", "up"))])
    # No wall draws its underside: the soil's covers the whole block, on the same plane.
    corner = model(tex, [box([14, 0, 0], [16, 12, 2], "wall",
                             faces=("north", "south", "east", "west", "up"))])
    # The item is a single bed: walls on all four sides.
    sides4 = ("north", "south", "east", "west", "up")
    walls = [box([0, 0, 0], [16, 12, 2], "wall", faces=sides4),
             box([0, 0, 14], [16, 12, 16], "wall", faces=sides4),
             box([0, 0, 2], [2, 12, 14], "wall", faces=sides4),
             box([14, 0, 2], [16, 12, 14], "wall", faces=sides4)]
    joining(reg, "BED", 12, 16,
            ("Raised %s Planting Bed" % names_m[0], "Hochbeet (%s)" % names_m[1],
             "Bancal elevado de %s" % names_m[2], "Upphöjd odlingsbädd av %s" % names_m[3]),
            post, side, item_extra=walls, side_when="false", corner=corner)
    reg = "planter_" + mat
    tex = {"side": T("planter_" + mat), "top": T("planter_%s_top" % mat),
           "particle": T("planter_" + mat)}
    prop(reg, "PLANTER", 14, 1,
         ("%s Planter" % names_m[0], "Pflanzkübel (%s)" % names_m[1],
          "Jardinera de %s" % names_m[2], "Planteringskärl av %s" % names_m[3]),
         {reg: model(tex, [box([1, 0, 1], [15, 14, 15], "side", per={"up": "top"})])},
         simple_state(reg))


# --- shrubs: stacked boxes ---
SHRUBS = [
    ("boxwood", "boxwood", 12, 2, [([3, 0, 3], [13, 3, 13]), ([1, 2, 1], [15, 10, 15]),
                                    ([3, 9, 3], [13, 12, 13])],
     ("Boxwood Ball", "Buchsbaumkugel", "Bola de boj", "Buxbomsklot")),
    ("hydrangea", "hydrangea", 14, 1, [([2, 0, 2], [14, 3, 14]), ([0.5, 2, 0.5], [15.5, 11, 15.5]),
                                       ([2.5, 10, 2.5], [13.5, 14, 13.5])],
     ("Hydrangea", "Hortensie", "Hortensia", "Hortensia")),
    ("juniper", "juniper", 16, 2, [([2, 0, 2], [14, 6, 14]), ([3.5, 5, 3.5], [12.5, 11, 12.5]),
                                   ([5, 10, 5], [11, 16, 11])],
     ("Upright Juniper", "Säulenwacholder", "Enebro columnar", "Pelarén")),
]
for sid, tex_name, h, inset, parts, names in SHRUBS:
    reg = "shrub_" + sid
    tex = {"leaves": T(tex_name), "particle": T(tex_name)}
    prop(reg, "SHRUB", h, inset, names,
         {reg: model(tex, [box(a, b, "leaves") for a, b in parts])}, simple_state(reg))

# --- ornamental grasses: cross models ---
for gid, names in (
        ("fountain", ("Fountain Grass", "Lampenputzergras", "Hierba de fuente", "Fjädertörel")),
        ("feather_reed", ("Feather Reed Grass", "Reitgras", "Cálamo plumoso", "Fodertrav")),
        ("blue_fescue", ("Blue Fescue", "Blauschwingel", "Festuca azul", "Blåsvingel"))):
    reg = "grass_" + gid
    tex = {"cross": T("grass_" + gid), "particle": T("grass_" + gid)}
    prop(reg, "PLANT", 12, 2, names,
         {reg: model(tex, cross_planes("cross", 16, four=True), parent="block/block", ao=False),
          reg + "_item": {"parent": "item/generated", "textures": {"layer0": T("grass_" + gid)}}},
         simple_state(reg, item=MODEL + reg + "_item"))

# --- flower beds: a mulch layer and low bedding flowers ---
for colour, names in (
        ("red", ("Red Flower Bed", "Rotes Blumenbeet", "Parterre de flores rojas",
                 "Röd blomrabatt")),
        ("yellow", ("Yellow Flower Bed", "Gelbes Blumenbeet", "Parterre de flores amarillas",
                    "Gul blomrabatt")),
        ("purple", ("Purple Flower Bed", "Violettes Blumenbeet", "Parterre de flores moradas",
                    "Lila blomrabatt")),
        ("mixed", ("Mixed Flower Bed", "Gemischtes Blumenbeet", "Parterre de flores variadas",
                   "Blandad blomrabatt"))):
    reg = "flower_bed_" + colour
    tex = {"flowers": T("bed_" + colour), "mulch": T("mulch"), "particle": T("bed_" + colour)}
    els = [box([0, 0, 0], [16, 1, 16], "mulch")] + cross_planes("flowers", 8, uv_top=8)
    prop(reg, "PLANT", 6, 0, names,
         {reg: model(tex, els, ao=False),
          reg + "_item": {"parent": "item/generated", "textures": {"layer0": T("bed_" + colour)}}},
         simple_state(reg, item=MODEL + reg + "_item"))

# --- ground covers: a one-pixel layer ---
for gid, names in (
        ("mulch", ("Mulch", "Rindenmulch", "Mantillo", "Täckbark")),
        ("pea_gravel", ("Pea Gravel", "Rundkies", "Grava de río", "Ärtsingel")),
        ("decomposed_granite", ("Decomposed Granite", "Granitgrus", "Granito descompuesto",
                                "Granitgrus")),
        ("turf", ("Artificial Turf", "Kunstrasen", "Césped artificial", "Konstgräs"))):
    reg = "ground_" + gid
    tex = {"all": T(gid), "particle": T(gid)}
    prop(reg, "COVER", 1, 0, names,
         {reg: model(tex, [box([0, 0, 0], [16, 1, 16], "all")])}, simple_state(reg))


# --- facing: the stake and the hanging baskets ---
def facing_state(model_name, fitted=False):
    variants = {
        "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}},
        "inventory": [{}],
    }
    if fitted:
        variants["polefit"] = {"large": {}, "thin": {"model": MODEL + model_name + "_thin"},
                               "pedestal": {"model": MODEL + model_name + "_pedestal"}}
    return {"forge_marker": 1, "defaults": {"model": MODEL + model_name}, "variants": variants}


stake_tex = {"wood": T("stake"), "tie": T("tie"), "particle": T("stake")}
add("tree_stake",
    'new BlockParkFacing.TreeStake("tree_stake", new int[]{7, 0, 11, 9, 24, 13}, true)',
    ("Tree Stake", "Baumpfahl", "Tutor para árbol", "Trädstöd"),
    {"tree_stake": model(stake_tex, [
        box([7, 0, 11], [9, 24, 13], "wood"),
        # The tie reaches back to a thin trunk in the next cell (centre z = 24, skin z = 22).
        box([7.5, 17, 13], [8.5, 18.5, 22], "tie", faces=("east", "west", "up", "down", "south")),
    ])},
    facing_state("tree_stake"))

# (value, suffix, how much further back the pole's skin is than the large pole's)
FITS = [("thin", "_thin", 2.0), ("pedestal", "_pedestal", 3.0)]


def basket_model(flowers, delta=0.0):
    tex = {"steel": T("steel"), "wire": T("wire"), "flowers": T(flowers), "particle": T(flowers)}
    return model(tex, [
        # Bracket: the plate on the pole's skin, the arm out to the hanger, the hanger rod.
        box([6.5, 11, 17.5 + delta], [9.5, 16, 18.5 + delta], "steel"),
        box([7.5, 14, 6], [8.5, 15, 17.5 + delta], "steel"),
        box([7.75, 9, 5.75], [8.25, 14, 6.25], "steel"),
        # Basket and the flowers heaped in it and trailing over its sides.
        box([3, 1, 1], [13, 6, 11], "wire"),
        box([2, 6, 0], [14, 10, 12], "flowers"),
        box([1.5, 0, -0.5], [14.5, 7, 12.5], "flowers", faces=("north", "south", "east", "west")),
    ])


for bid, flowers, names in (
        ("petunia", "petunia", ("Petunia Hanging Basket", "Petunien-Hängeampel",
                                "Cesta colgante de petunias", "Hängampel med petunior")),
        ("mixed", "mixed_basket", ("Mixed Hanging Basket", "Gemischte Hängeampel",
                                   "Cesta colgante variada", "Blandad hängampel"))):
    reg = "hanging_basket_" + bid
    models = {reg: basket_model(flowers)}
    for _, suffix, delta in FITS:
        models[reg + suffix] = basket_model(flowers, delta)
    add(reg, 'new BlockParkFacing.PoleFitted("%s", new int[]{2, 0, 0, 14, 16, 12}, false)' % reg,
        names, models, facing_state(reg, fitted=True))


# ------------------------------------------------------------------------------------------
# Regional plantings: native plants of California, New Hampshire, Colorado, Florida, Japan
# and Scandinavia, each also in a nursery pot, so a garden centre can be stocked in rows.
# ------------------------------------------------------------------------------------------
def _canvas():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return img, img.load()


def _dot(px, x, y, c):
    x, y = int(round(x)), int(round(y))
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = clamp(c) + (255,)


def _head(px, x, y, pattern, colours):
    """Stamps a flower head: pattern rows top to bottom, its bottom row centred on (x, y)."""
    x, y = int(round(x)), int(round(y))
    for r, row in enumerate(pattern):
        yy = y - (len(pattern) - 1 - r)
        for c, ch in enumerate(row):
            if ch != ".":
                _dot(px, x - len(row) // 2 + c, yy, colours[ch])


def _foliage(px, rng, palette, top, count):
    """Basal leaves: scattered pixels from row `top` down to the ground."""
    for _ in range(count):
        _dot(px, rng.randrange(16), rng.randrange(top, 16), palette[rng.randrange(len(palette))])


def herb_tex(seed, foliage, foliage_top, foliage_count, stems, stem_colour, heads, pattern,
             lean=0.12):
    """Stemmed flowers over basal foliage. stems: (count, shortest, tallest); heads: a list of
    colour maps, one picked per stem (a lupine stand is mixed, a poppy stand is not)."""
    rng = random.Random(seed)
    img, px = _canvas()
    _foliage(px, rng, foliage, foliage_top, foliage_count)
    count, lo, hi = stems
    for i in range(count):
        # Spread the stems across the card, a little jittered, so no two heads sit together.
        x = 1.5 + (i + rng.uniform(0.15, 0.85)) * 13.0 / count
        h = rng.randint(lo, hi)
        tilt = rng.uniform(-lean, lean)
        for k in range(h):
            _dot(px, x + tilt * k, 15 - k, stem_colour)
        _head(px, x + tilt * h, 15 - h, pattern, heads[rng.randrange(len(heads))])
    return img


def harebell_tex(seed):
    """Harebell: hair-thin wiry stems, each nodding a blue bell off to one side."""
    rng = random.Random(seed)
    img, px = _canvas()
    _foliage(px, rng, [(92, 132, 70), (76, 114, 58)], 13, 26)
    for i in range(6):
        x = 1.5 + (i + rng.uniform(0.2, 0.8)) * 13.0 / 6
        h = rng.randint(7, 11)
        for k in range(h):
            _dot(px, x + math.sin(k * 0.5 + i) * 0.6, 15 - k, (96, 126, 70))
        tx, ty = x + math.sin(h * 0.5 + i) * 0.6, 15 - h
        side = 1 if i % 2 else -1
        _dot(px, tx + side, ty, (96, 126, 70))
        for dx, dy, c in ((1, 1, (132, 140, 226)), (2, 1, (112, 118, 212)),
                          (1, 2, (104, 110, 204)), (2, 2, (88, 94, 186))):
            _dot(px, tx + side * dx, ty + dy, c)
    return img


def ladys_slipper_tex(seed):
    """Pink lady's slipper: two broad pleated leaves at the base, a bare stalk, a pink pouch."""
    rng = random.Random(seed)
    img, px = _canvas()
    leaf = [(76, 124, 62), (60, 104, 50), (88, 138, 72)]
    for x0 in (4, 11):
        for s in (-1, 1):
            cx, cy = x0 + s * 1.6, 13
            for y in range(10, 16):
                for x in range(16):
                    u, v = (x - cx) / 1.9, (y - cy) / 3.0
                    if u * u + v * v <= 1:
                        _dot(px, x, y, leaf[0 if x == round(cx) else 1 + (x + y) % 2])
        h = rng.randint(9, 11)
        for k in range(h):
            _dot(px, x0, 15 - k, (86, 110, 58))
        ty = 15 - h
        _dot(px, x0, ty, (104, 132, 64))  # the bract over the flower
        _dot(px, x0 - 2, ty + 1, (122, 64, 60))  # the twisted maroon petals
        _dot(px, x0 + 2, ty + 1, (122, 64, 60))
        _head(px, x0, ty + 4, [".a.", "aca", "aca", ".a."],
              {"a": (234, 142, 182), "c": (204, 100, 148)})
    return img


def iris_tex(seed):
    """Japanese iris: upright sword leaves, and big flat purple flowers with a yellow signal."""
    rng = random.Random(seed)
    img, px = _canvas()
    green = [(84, 130, 70), (68, 112, 58), (100, 146, 84)]
    for i in range(11):
        x = 1.5 + i * 1.3 + rng.uniform(-0.4, 0.4)
        h = rng.randint(7, 12)
        tilt = rng.uniform(-0.08, 0.08)
        for k in range(h):
            _dot(px, x + tilt * k, 15 - k, green[rng.randrange(3)])
    for x in (3.5, 8.2, 12.6):
        h = rng.randint(11, 13)
        for k in range(h):
            _dot(px, x, 15 - k, green[1])
        _head(px, x, 15 - h + 3, [".a.", "aba", "aya", "b.b"],
              {"a": (132, 88, 206), "b": (100, 60, 170), "y": (238, 204, 70)})
    return img


def yucca_tex(seed):
    """Soapweed yucca: a rosette of stiff blue-green swords, and a stalk of cream bells."""
    rng = random.Random(seed)
    img, px = _canvas()
    blade = [(128, 152, 124), (108, 132, 106), (148, 170, 140)]
    for i in range(15):
        ang = math.radians(-78 + i * 156 / 14 + rng.uniform(-4, 4))
        length = rng.uniform(5.0, 8.5)
        x0 = 8 + rng.uniform(-1, 1)
        for t in range(int(length * 2)):
            d = t / 2.0
            _dot(px, x0 + math.sin(ang) * d, 15 - math.cos(ang) * d,
                 blade[2] if d > length - 1.5 else blade[rng.randrange(2)])
    for y in range(1, 9):
        _dot(px, 8, y, (120, 132, 84))
    for y in range(1, 7):
        side = -1 if y % 2 else 1
        _dot(px, 8 + side, y, (240, 236, 206))
        _dot(px, 8 + side, y + 1, (218, 212, 176))
    return img


def fan_plant_tex(seed):
    """Saw palmetto: stiff silvery fans on stalks, splayed out of a low clump."""
    rng = random.Random(seed)
    img, px = _canvas()
    palette = [(146, 172, 146), (122, 150, 124), (100, 128, 104), (80, 106, 86)]
    fans = [(2.5, 7.5, -0.9), (6.0, 4.0, -0.3), (10.0, 4.0, 0.3), (13.5, 7.5, 0.9),
            (8.0, 9.5, 0.0)]
    for cx, cy, facing in fans:
        # The stalk down to the clump.
        for t in range(12):
            f = t / 11.0
            _dot(px, 8 + (cx - 8) * f, 15 + (cy + 3 - 15) * f, (118, 118, 74))
        for y in range(16):
            for x in range(16):
                dx, dy = x + 0.5 - cx, (cy + 3) - (y + 0.5)
                r = math.hypot(dx, dy)
                if r > 5.2 or r < 0.8:
                    continue
                ang = math.atan2(dx, dy) - facing
                if abs(ang) > math.radians(78):
                    continue
                seg = (ang + math.radians(78)) / math.radians(156) * 9
                frac = seg - int(seg)
                if r > 3.6 and abs(frac - 0.5) < 0.18:
                    continue  # split tips
                shade = 0 if frac < 0.5 else 1
                if r < 2:
                    shade = 2
                if frac < 0.1 or frac > 0.9:
                    shade = 3
                if rng.random() < 0.12:
                    shade = min(3, shade + 1)
                _dot(px, x, y, palette[shade])
    return img


def cycad_tex(seed):
    """Coontie: short arching fronds of stiff dark leaflets from a buried trunk."""
    rng = random.Random(seed)
    img, px = _canvas()
    leaf = [(62, 108, 54), (50, 92, 44), (78, 124, 64)]
    for i in range(7):
        ang = math.radians(-66 + i * 22 + rng.uniform(-5, 5))
        length = rng.uniform(7.0, 9.0)
        for t in range(int(length * 2)):
            d = t / 2.0
            # Up and out, then over: the tip droops.
            x = 8 + math.sin(ang) * d
            y = 15 - math.cos(ang) * d + 0.04 * d * d * (1 + abs(math.sin(ang)))
            _dot(px, x, y, (96, 104, 60))
            if t % 2 == 0 and d > 1:
                for s in (-1, 1):
                    _dot(px, x + math.cos(ang) * s * 1.2, y + math.sin(ang) * s * 1.2 - 0.6,
                         leaf[rng.randrange(3)])
    return img


def bamboo_tex(seed):
    """Bamboo canes with nodes and leaf sprays every eight pixels, so the texture tiles up a
    stack of blocks into one tall grove."""
    rng = random.Random(seed)
    img, px = _canvas()
    cane = [(132, 170, 78), (110, 150, 62)]
    node = (80, 110, 48)
    leaf = [(90, 146, 62), (112, 164, 74), (74, 126, 52)]
    for x in (1, 4, 7, 10, 13):
        x += rng.choice((0, 1))
        for y in range(16):
            _dot(px, x, y, node if y % 8 == 3 else cane[(y // 3) % 2])
        for ny in (3, 11):
            for s in (-1, 1):
                if rng.random() < 0.75:
                    length = rng.randint(2, 4)
                    for k in range(1, length + 1):
                        _dot(px, (x + s * k) % 16, (ny + k // 2) % 16, leaf[rng.randrange(3)])
    return img


def grass_tex2(kind, seed):
    """The regional grasses, drawn as grass_tex draws the ornamental ones."""
    rng = random.Random(seed)
    img, px = _canvas()
    if kind == "deergrass":
        green = [(146, 160, 114), (126, 142, 98), (162, 172, 128)]
        for b in range(30):
            x = 7.5 + rng.uniform(-5, 5)
            lean = (x - 7.5) / 5 * rng.uniform(0.1, 0.3)
            h = rng.randint(9, 14)
            for k in range(h):
                _dot(px, x + lean * k, 15 - k, green[rng.randrange(3)])
            if b % 3 == 0:
                # A narrow upright plume, straight up past the blade.
                for k in range(h, min(16, h + 4)):
                    _dot(px, x + lean * h, 15 - k, (176, 156, 128) if k % 2 else (156, 136, 112))
    elif kind == "blue_grama":
        green = [(124, 152, 132), (104, 134, 116), (140, 164, 146)]
        for _ in range(26):
            x = 7.5 + rng.uniform(-6, 6)
            lean = (x - 7.5) / 6 * rng.uniform(0.2, 0.5)
            h = rng.randint(3, 6)
            for k in range(h):
                _dot(px, x + lean * k, 15 - k, green[rng.randrange(3)])
        for i in range(6):
            x = 2 + i * 2.4 + rng.uniform(-0.4, 0.4)
            h = rng.randint(8, 11)
            for k in range(h):
                _dot(px, x + 0.08 * k, 15 - k, (138, 146, 110))
            # The eyelash: a seed flag held out sideways at the top.
            tx, ty = x + 0.08 * h, 15 - h
            for d in range(1, 4):
                _dot(px, tx + d, ty + d * 0.3, (132, 92, 112) if d % 2 else (156, 118, 128))
    elif kind == "pink_muhly":
        green = [(100, 134, 74), (86, 118, 62)]
        for _ in range(22):
            x = 7.5 + rng.uniform(-5, 5)
            h = rng.randint(4, 7)
            for k in range(h):
                _dot(px, x + (x - 7.5) * 0.05 * k, 15 - k, green[rng.randrange(2)])
        pink = [(232, 134, 182), (214, 112, 166), (244, 176, 208), (196, 98, 150)]
        for y in range(1, 11):
            for x in range(16):
                u, v = (x - 7.5) / 8.0, (y - 6.0) / 5.5
                if u * u + v * v <= 1 and rng.random() < 0.42:
                    _dot(px, x, y, pink[rng.randrange(4)])
    elif kind == "hakone":
        gold = [(220, 202, 76), (194, 180, 58), (168, 158, 48), (150, 168, 70)]
        for b in range(24):
            d = 1 if rng.random() < 0.8 else -1
            x0 = 8 + rng.uniform(-4, 3) * d
            length = rng.randint(8, 13)
            c = gold[rng.randrange(4)]
            for t in range(length):
                _dot(px, x0 + d * 0.55 * t, 15 - 1.2 * t + 0.06 * t * t, c)
    else:
        raise ValueError(kind)
    return img


def shrub_tex(palette, seed, flowers=None, density=0.0, berries=None, berry_count=0):
    """A shrub's leafy texture: leafy(), with berries as single pixels over it."""
    img = leafy(palette, seed, flowers, density)
    if berries:
        rng = random.Random(seed + 1000)
        px = img.load()
        for _ in range(berry_count):
            x, y = rng.randrange(16), rng.randrange(16)
            px[x, y] = clamp(berries[rng.randrange(len(berries))]) + (255,)
    return img


def twigs_tex(colours, seed, top):
    """Bare stems for under a shrub's canopy: forking from the ground up to row `top`."""
    rng = random.Random(seed)
    img, px = _canvas()
    for i in range(5):
        x = 3 + i * 2.5 + rng.uniform(-0.5, 0.5)
        drift = rng.uniform(-0.35, 0.35) + (x - 8) * 0.06
        for y in range(15, top - 1, -1):
            _dot(px, x, y, colours[rng.randrange(len(colours))])
            x += drift
            if rng.random() < 0.18:
                drift = -drift
    return img


def spikes_tex(seed):
    """White sage's flower spikes: pale stems standing well clear of the mound, pale whorls."""
    rng = random.Random(seed)
    img, px = _canvas()
    for i in range(5):
        x = 2.5 + i * 2.6 + rng.uniform(-0.4, 0.4)
        h = rng.randint(11, 15)
        for k in range(5, h):
            y = 15 - k
            _dot(px, x, y, (184, 190, 176))
            if k > 7 and k % 2 == 0:
                _dot(px, x - 1, y, (232, 226, 240))
                _dot(px, x + 1, y, (214, 204, 230))
    return img


def pot_tex(seed):
    """A black plastic nursery container: faint moulded ribs, and a lip at row 10, the top
    of the window its 6 px sides sample."""
    rng = random.Random(seed)
    img, px = _canvas()
    for y in range(16):
        for x in range(16):
            v = 40 + (6 if x % 4 == 0 else 0) + rng.randint(-3, 3)
            if y == 10:
                v += 14
            px[x, y] = (v, v, v + 2, 255)
    return img


def pot_top_tex(seed):
    """The pot seen from above: soil inside a lip, the lip at pixels 4 and 11 (the up face is
    8 px across, from 4 to 12)."""
    img = noise_tex(SOIL, seed, grain=0.8)
    px = img.load()
    for i in range(4, 12):
        for x, y in ((i, 4), (i, 11), (4, i), (11, i)):
            px[x, y] = (52, 52, 54, 255)
    return img


TEXTURES.update({
    # California
    "california_poppy": lambda: herb_tex(
        40, [(126, 162, 142), (104, 140, 122), (86, 120, 104)], 10, 70, (5, 4, 8),
        (104, 140, 122), [{"a": (248, 150, 34), "b": (226, 110, 22), "c": (196, 88, 24)}],
        ["a.a", "aba", ".c."]),
    "manzanita": lambda: shrub_tex([(132, 150, 118), (114, 134, 102), (96, 116, 88),
                                    (80, 100, 74)], 41, [(242, 222, 226), (230, 190, 204)], 0.2),
    "manzanita_stems": lambda: twigs_tex([(142, 50, 36), (118, 40, 30), (160, 64, 44)], 42, 9),
    "ceanothus": lambda: shrub_tex([(60, 96, 52), (48, 82, 44), (38, 68, 36), (30, 56, 30)], 43,
                                   [(76, 102, 204), (104, 130, 226), (90, 116, 214)], 0.75),
    "white_sage": lambda: shrub_tex([(206, 212, 204), (186, 194, 186), (166, 176, 168),
                                     (146, 158, 150)], 44),
    "white_sage_spikes": lambda: spikes_tex(45),
    "grass_deergrass": lambda: grass_tex2("deergrass", 46),
    # New Hampshire
    "mountain_laurel": lambda: shrub_tex([(62, 104, 60), (50, 90, 50), (40, 76, 42),
                                          (32, 62, 34)], 47,
                                         [(246, 226, 234), (234, 170, 198), (224, 140, 178)],
                                         0.5),
    "blueberry": lambda: shrub_tex([(104, 146, 74), (88, 130, 62), (72, 112, 52), (58, 94, 42)],
                                   48, berries=[(64, 84, 150), (92, 112, 170), (52, 66, 124)],
                                   berry_count=26),
    "blueberry_stems": lambda: twigs_tex([(110, 92, 78), (130, 108, 90)], 49, 7),
    "winterberry": lambda: shrub_tex([(78, 104, 58), (64, 90, 48), (52, 76, 40), (42, 62, 34)],
                                     50, berries=[(214, 30, 36), (186, 20, 30), (236, 60, 56)],
                                     berry_count=46),
    "winterberry_stems": lambda: twigs_tex([(96, 88, 80), (116, 106, 96)], 51, 6),
    "lupine": lambda: herb_tex(
        52, [(96, 142, 72), (80, 124, 60), (112, 156, 84)], 10, 60, (5, 11, 15),
        (86, 126, 64),
        [{"a": (150, 126, 228), "b": (108, 84, 196)}, {"a": (236, 150, 196), "b": (206, 104, 164)},
         {"a": (126, 108, 220), "b": (84, 70, 176)}, {"a": (246, 242, 246), "b": (214, 204, 226)}],
        ["a", "ab", "ba", "ab", "ba", "ab", "ba"]),
    "ladys_slipper": lambda: ladys_slipper_tex(53),
    # Colorado
    "columbine": lambda: herb_tex(
        54, [(116, 150, 128), (98, 132, 112), (132, 164, 140)], 9, 64, (5, 9, 13),
        (104, 132, 102), [{"a": (130, 118, 224), "b": (244, 244, 240), "y": (242, 212, 84)}],
        ["a.a", ".b.", "byb", ".a."]),
    "penstemon": lambda: herb_tex(
        55, [(92, 132, 96), (76, 114, 82)], 12, 34, (6, 10, 14), (82, 116, 84),
        [{"a": (96, 100, 218), "b": (132, 110, 222)}, {"a": (84, 92, 204), "b": (160, 108, 208)}],
        ["a", ".a", "b.", ".a", "a.", ".b", "a."]),
    "rabbitbrush": lambda: shrub_tex([(150, 164, 126), (132, 148, 110), (114, 130, 96),
                                      (98, 114, 84)], 56, [(236, 198, 48), (220, 176, 36)], 0.35),
    "rabbitbrush_top": lambda: shrub_tex([(150, 164, 126), (132, 148, 110), (114, 130, 96),
                                          (98, 114, 84)], 57,
                                         [(244, 206, 54), (226, 182, 38), (250, 222, 90)], 1.5),
    "sagebrush": lambda: shrub_tex([(166, 176, 168), (146, 158, 150), (126, 140, 132),
                                    (108, 122, 114)], 58),
    "sagebrush_stems": lambda: twigs_tex([(110, 102, 92), (132, 124, 112)], 59, 7),
    "grass_blue_grama": lambda: grass_tex2("blue_grama", 60),
    "yucca": lambda: yucca_tex(61),
    # Florida
    "saw_palmetto": lambda: fan_plant_tex(62),
    "coontie": lambda: cycad_tex(63),
    "beautyberry": lambda: shrub_tex([(112, 154, 68), (96, 138, 58), (80, 120, 48), (64, 102, 40)],
                                     64, [(176, 64, 176), (148, 44, 158), (196, 90, 196)], 0.6),
    "beautyberry_stems": lambda: twigs_tex([(126, 104, 80), (104, 86, 66)], 65, 5),
    "firebush": lambda: shrub_tex([(104, 128, 60), (90, 112, 50), (120, 96, 52), (72, 92, 42)], 66,
                                  [(230, 80, 36), (204, 54, 30), (244, 120, 44)], 0.5),
    "coreopsis": lambda: herb_tex(
        67, [(100, 146, 70), (84, 128, 58)], 12, 36, (7, 6, 11), (92, 136, 64),
        [{"a": (248, 204, 42), "d": (126, 74, 32)}], [".a.", "ada", ".a."]),
    "grass_pink_muhly": lambda: grass_tex2("pink_muhly", 68),
    # Japan
    "satsuki_azalea": lambda: shrub_tex([(62, 96, 50), (50, 82, 42), (40, 68, 34), (32, 56, 28)],
                                        69, [(230, 84, 152), (244, 126, 182), (212, 60, 132)],
                                        1.1),
    "camellia": lambda: shrub_tex([(44, 84, 50), (36, 72, 42), (28, 60, 36), (22, 48, 30)], 70,
                                  [(206, 32, 52), (232, 74, 94), (176, 22, 40)], 0.4),
    "japanese_iris": lambda: iris_tex(71),
    "bamboo": lambda: bamboo_tex(72),
    "grass_hakone": lambda: grass_tex2("hakone", 73),
    # Sweden and Denmark
    "heather": lambda: shrub_tex([(96, 110, 84), (80, 94, 70), (66, 80, 58), (54, 66, 48)], 74,
                                 [(176, 96, 170), (200, 126, 190), (152, 78, 150)], 1.2),
    "lingonberry": lambda: shrub_tex([(58, 100, 50), (46, 86, 42), (36, 72, 34), (28, 58, 28)],
                                     75, berries=[(206, 28, 40), (180, 18, 32), (230, 64, 64)],
                                     berry_count=30),
    "wood_anemone": lambda: herb_tex(
        76, [(72, 124, 60), (58, 106, 50), (88, 140, 72)], 10, 64, (8, 3, 6), (96, 120, 70),
        [{"a": (248, 248, 246), "b": (234, 216, 228)}], ["ab", "ba"]),
    "harebell": lambda: harebell_tex(77),
    "marguerite": lambda: herb_tex(
        78, [(70, 116, 56), (58, 100, 48)], 11, 40, (6, 8, 13), (80, 120, 60),
        [{"a": (248, 248, 244), "y": (246, 204, 48)}], [".a.", "aya", ".a."]),
    # The nursery pot
    "nursery_pot": lambda: pot_tex(79),
    "nursery_pot_top": lambda: pot_top_tex(80),
})


def shrub_els(leaves, parts, top=None, stems=None, spikes=None):
    """A shrub: stacked leafy boxes, with bare stems under a raised canopy and spikes over it
    where the species has them. Returns (textures, elements)."""
    tex = {"leaves": T(leaves), "particle": T(leaves)}
    per = None
    if top:
        tex["top"] = T(top)
        per = {"up": "top"}
    els = [box(a, b, "leaves", per=per) for a, b in parts]
    if stems:
        tex["stems"] = T(stems[0])
        els += cross_planes("stems", stems[1], uv_top=16 - stems[1])
    if spikes:
        tex["spikes"] = T(spikes)
        els += cross_planes("spikes", 16, four=True)
    return tex, els


def plant_els(texture, height):
    """A plant drawn as four crossed planes, cut to its height."""
    return ({"cross": T(texture), "particle": T(texture)},
            cross_planes("cross", height, uv_top=16 - height, four=True))


POT_LIFT = 6
POT_SCALE = 0.6


def potted(tex, els):
    """The same plant at 60% in a nursery pot, standing on its soil."""
    def sc(v, axis):
        return round(POT_LIFT + v[1] * POT_SCALE, 3) if axis == 1 else \
            round(8 + (v[axis] - 8) * POT_SCALE, 3)

    out = []
    for e in els:
        e = json.loads(json.dumps(e))
        e["from"] = [sc(e["from"], a) for a in range(3)]
        e["to"] = [sc(e["to"], a) for a in range(3)]
        if "rotation" in e:
            e["rotation"]["origin"] = [sc(e["rotation"]["origin"], a) for a in range(3)]
        out.append(e)
    pot = box([4, 0, 4], [12, POT_LIFT, 12], "pot", per={"up": "pot_top"})
    tex = dict(tex)
    tex.update({"pot": T("nursery_pot"), "pot_top": T("nursery_pot_top")})
    return tex, [pot] + out


# ------------------------------------------------------------------------------------------
# Herbs and garden plants, cacti and succulents, garden flowers (GitHub #250). Drawn as the
# regional plants are: crossed planes for an open plant, stacked boxes for a dense one, a solid
# ribbed body for a cactus. Each one is drawn from the real plant's habit at 1 px = 6.25 cm.
# ------------------------------------------------------------------------------------------
def arching_spikes_tex(seed, count, length, spread, arch, stem, spike, spike_len, base_row,
                       dots=None):
    """Flower spikes standing out of a mound: stems from the mound's top (row `base_row`),
    leaning out by up to `spread` radians and curving further out along their length (`arch`),
    the last `spike_len` pixels in the spike's colours (with an odd pale dot where the species
    shows its corollas)."""
    rng = random.Random(seed)
    img, px = _canvas()
    for i in range(count):
        f = (i + 0.5) / count * 2 - 1
        ang = f * spread + rng.uniform(-0.08, 0.08)
        L = rng.uniform(*length)
        x, y = 8 + f * 3.5 + rng.uniform(-0.5, 0.5), base_row
        d = 0.0
        while d < L:
            a = ang * (1 + arch * d / L)
            x += math.sin(a) * 0.5
            y -= math.cos(a) * 0.5
            d += 0.5
            if d < L - spike_len:
                _dot(px, x, y, stem)
            else:
                c = spike[int(d * 2) % len(spike)]
                if dots and rng.random() < 0.18:
                    c = dots
                _dot(px, x, y, c)
    return img


def russian_sage_tex(seed):
    """Russian sage: a see-through haze of silvery stems and side shoots, misted lavender-blue
    with tiny flowers over the top two-thirds."""
    rng = random.Random(seed)
    img, px = _canvas()
    _foliage(px, rng, [(140, 160, 140), (120, 142, 122)], 11, 28)
    silver = [(204, 210, 210), (184, 192, 194), (218, 222, 220)]
    blue = [(126, 128, 210), (150, 150, 226), (108, 112, 196), (170, 166, 232)]
    for i in range(8):
        x = 1.5 + (i + rng.uniform(0.2, 0.8)) * 13.0 / 8
        h = rng.randint(11, 16)
        lean = (x - 8) * 0.025 + rng.uniform(-0.05, 0.05)
        for k in range(h):
            xx, yy = x + lean * k, 15 - k
            _dot(px, xx, yy, silver[k % 3])
            if k > 5 and k % 4 == i % 4:
                side = 1 if (k + i) % 2 else -1
                _dot(px, xx + side, yy - 1, silver[1])
                _dot(px, xx + side * 2, yy - 2, blue[rng.randrange(4)])
            if k > 4 and rng.random() < 0.8:
                _dot(px, xx + rng.choice((-1, 1)), yy, blue[rng.randrange(4)])
    return img


def bird_of_paradise_tex(seed):
    """Strelitzia: a fan of paddle leaves on long stalks, blue-green with a pale midrib and the
    odd tear, and two flowers held at leaf height: a grey-green beak, an orange crest, a blue
    tongue."""
    rng = random.Random(seed)
    img, px = _canvas()
    blade = [(70, 116, 92), (60, 104, 82), (82, 128, 102)]
    for ang, L in ((-0.62, 9), (0.66, 9.5), (-0.34, 12), (0.38, 12), (-0.1, 13.5), (0.14, 12.5)):
        for t in range(int((L - 3) * 2)):
            d = t / 2.0
            _dot(px, 8 + math.sin(ang) * d, 15 - math.cos(ang) * d, (104, 132, 100))
        cx, cy = 8 + math.sin(ang) * L, 15 - math.cos(ang) * L
        for y in range(16):
            for x in range(16):
                dx, dy = x - cx, y - cy
                u = dx * math.sin(ang) - dy * math.cos(ang)  # along the leaf
                v = dx * math.cos(ang) + dy * math.sin(ang)  # across it
                if (u / 3.5) ** 2 + (v / 1.9) ** 2 > 1:
                    continue
                if abs(v) < 0.45:
                    c = (150, 172, 140)
                elif abs(v) > 0.9 and rng.random() < 0.08:
                    continue  # a tear in the blade
                else:
                    c = blade[rng.randrange(3)]
                _dot(px, x, y, c)
    for x0, h, side in ((6.5, 11, 1), (10.0, 9, -1)):
        for k in range(h):
            _dot(px, x0, 15 - k, (110, 132, 104))
        tx, ty = x0, 15 - h
        for d in range(4):  # the beak, held out sideways
            _dot(px, tx + side * d, ty + (1 if d < 2 else 0), (112, 96, 104) if d % 2 else
                 (96, 110, 98))
        for dx, dy, c in ((1, -1, (248, 140, 30)), (2, -2, (252, 172, 44)),
                          (1, -2, (240, 118, 22)), (2, -1, (248, 150, 34)),
                          (3, -3, (250, 160, 40)), (2, 0, (56, 78, 200))):
            _dot(px, tx + side * dx, ty + dy, c)
    return img


def foxtail_tex(seed):
    """Foxtail fern: dense, bright green, cylindrical plumes arching out of the crown, each
    tapering to a point."""
    rng = random.Random(seed)
    img, px = _canvas()
    green = [(110, 168, 62), (92, 150, 50), (130, 184, 78), (76, 128, 44)]
    for i in range(8):
        f = (i + 0.5) / 8 * 2 - 1
        ang = f * 0.95 + rng.uniform(-0.06, 0.06)
        L = 9.0 - abs(f) * 2.5 + rng.uniform(-0.5, 0.5)
        x, y, d = 8 + f * 1.5, 15.0, 0.0
        while d < L:
            a = ang * (1 + 0.35 * d / L)
            x += math.sin(a) * 0.5
            y -= math.cos(a) * 0.5
            d += 0.5
            if d < 1.5:
                _dot(px, x, y, (100, 140, 60))
                continue
            w = 1.2 if d < L - 2 else 0.5
            for s in (-w, -w / 2, 0, w / 2, w):
                if rng.random() < 0.8:
                    _dot(px, x + math.cos(a) * s, y + math.sin(a) * s, green[rng.randrange(4)])
    return img


def mustard_tex(seed):
    """Wild mustard: tall, branching, rather bare stems over lobed basal leaves, each tip a
    loose cluster of small bright yellow flowers with thin seed pods held out below it."""
    rng = random.Random(seed)
    img, px = _canvas()
    _foliage(px, rng, [(70, 112, 52), (58, 98, 44), (84, 124, 60)], 11, 44)
    head = {"a": (246, 222, 48), "b": (226, 196, 30)}
    for i in range(5):
        x = 2 + (i + rng.uniform(0.2, 0.8)) * 12.0 / 5
        h = rng.randint(11, 15)
        tilt = rng.uniform(-0.1, 0.1)
        for k in range(h):
            _dot(px, x + tilt * k, 15 - k, (110, 140, 70))
            if 3 < k < h - 2 and k % 2 == 0:
                _dot(px, x + tilt * k + (1 if k % 4 else -1), 15 - k, (150, 170, 92))  # pods
        _head(px, x + tilt * h, 15 - h, [".a.", "aba", "a.a"], head)
        # A side branch with a smaller cluster of its own.
        side = 1 if i % 2 else -1
        k0 = h // 2 + 1
        bx, by = x + tilt * k0, 15 - k0
        for d in range(1, 4):
            _dot(px, bx + side * d * 0.7, by - d, (110, 140, 70))
        _head(px, bx + side * 2.1, by - 3, ["a.", "ba"], head)
    return img


def saguaro_tex(seed):
    """A saguaro's pleated skin: ribs every 2 px, a pale spine cluster (areole) every third row
    along each rib's crest, staggered from rib to rib."""
    rng = random.Random(seed)
    img, px = _canvas()
    for y in range(16):
        for x in range(16):
            n = rng.randint(-5, 5)
            if x % 2 == 0:
                c = (72 + n, 104 + n, 62 + n)
            else:
                c = (100 + n, 136 + n, 82 + n)
                if y % 3 == (x // 2) % 3:
                    c = (196, 186, 152)
            px[x, y] = clamp(c) + (255,)
    return img


def saguaro_top_tex(seed):
    """The crown from above: the ribs meeting at a tuft of tan felt."""
    rng = random.Random(seed)
    img, px = _canvas()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            n = rng.randint(-5, 5)
            if r < 1.6:
                c = (194 + n, 178 + n, 132 + n)
            else:
                rib = int((math.atan2(dy, dx) + math.pi) / (2 * math.pi) * 16) % 2
                c = (100 + n, 136 + n, 82 + n) if rib else (72 + n, 104 + n, 62 + n)
                if rib and r < 3.2 and rng.random() < 0.4:
                    c = (196, 186, 152)
            px[x, y] = clamp(c) + (255,)
    return img


def barrel_side_tex(seed):
    """A golden barrel cactus's side: green ribs every 3 px, their crests thick with golden
    spines that splay over the grooves."""
    rng = random.Random(seed)
    img, px = _canvas()
    gold = [(238, 206, 88), (214, 180, 64), (248, 222, 120)]
    for y in range(16):
        for x in range(16):
            n = rng.randint(-5, 5)
            rib = x % 3
            c = [(58 + n, 100 + n, 46 + n), (86 + n, 132 + n, 60 + n),
                 (104 + n, 148 + n, 70 + n)][rib]
            if rib == 2 and y % 2 == 0:
                c = gold[rng.randrange(3)]
            elif rib != 0 and rng.random() < 0.28:
                c = gold[rng.randrange(3)]
            px[x, y] = clamp(c) + (255,)
    return img


def barrel_top_tex(seed):
    """The barrel from above: radial ribs crested with spines round a woolly yellow crown."""
    rng = random.Random(seed)
    img, px = _canvas()
    gold = [(238, 206, 88), (214, 180, 64), (248, 222, 120)]
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            n = rng.randint(-6, 6)
            if r < 2.0:
                c = (228 + n, 206 + n, 128 + n)
            else:
                rib = int((math.atan2(dy, dx) + math.pi) / (2 * math.pi) * 20) % 2
                c = (100 + n, 146 + n, 68 + n) if rib else (60 + n, 102 + n, 48 + n)
                if rib and rng.random() < 0.6:
                    c = gold[rng.randrange(3)]
            px[x, y] = clamp(c) + (255,)
    return img


def pad_tex(seed, fruit=False):
    """A prickly pear pad: an obovate cladode, narrowing to where it joins the pad below, with
    areoles in a diagonal lattice; the fruiting pad carries magenta-red fruit along its top."""
    rng = random.Random(seed)
    img, px = _canvas()
    body = [(112, 156, 78), (100, 144, 70), (124, 168, 88)]
    for y in range(16):
        for x in range(16):
            v = (y + 0.5 - 8.0) / 7.8
            if abs(v) > 1:
                continue
            hw = 7.2 * math.sqrt(1 - v * v) * (1 - 0.38 * max(0.0, v))
            u = abs(x + 0.5 - 8.0)
            if u > hw:
                continue
            c = body[rng.randrange(3)]
            if hw - u < 1.0 or abs(v) > 0.93:
                c = (84, 124, 58)
            elif (x + 2 * y) % 4 == 0 and y % 2 == 0:
                c = (176, 158, 112)
            px[x, y] = clamp(c) + (255,)
    if fruit:
        for fx in (4, 8, 12):
            fy = 1 if fx == 8 else 2
            for dx, dy, c in ((0, 0, (206, 56, 110)), (1, 0, (178, 30, 86)),
                              (0, 1, (178, 30, 86)), (1, 1, (150, 22, 66)),
                              (0, 2, (150, 22, 66)), (1, 2, (130, 20, 58))):
                _dot(px, fx - 1 + dx, fy - 1 + dy, c)
    return img


def rosette_tex(seed, count, spread, length, half_width, palette, edge, tip, lift,
                speckle=None, teeth=None):
    """A rosette of thick tapering leaves from one crown (agave, aloe): the outer leaves drawn
    first, so the inner ones lie over them; each curves up toward the vertical by `lift`."""
    rng = random.Random(seed)
    img, px = _canvas()
    angles = [math.radians(-spread + i * 2 * spread / (count - 1) + rng.uniform(-4, 4))
              for i in range(count)]
    angles.sort(key=lambda a: -abs(a))
    for ang in angles:
        L = rng.uniform(*length) * (1.0 + 0.25 * (1 - abs(ang) / math.radians(spread)))
        x, y, d = 8.0 + rng.uniform(-0.6, 0.6), 15.5, 0.0
        while d < L:
            a = ang * (1 - lift * d / L)
            w = half_width * (1 - d / L) ** 0.8
            s = -w
            while s <= w + 0.01:
                xx, yy = x + math.cos(a) * s, y + math.sin(a) * s
                if d > L - 0.6:
                    c = tip
                elif abs(s) > w - 0.5:
                    c = teeth if (teeth and int(d * 2) % 5 == 0) else edge
                else:
                    c = palette[0 if abs(s) < 0.4 else 1 + rng.randrange(len(palette) - 1)]
                    if speckle and rng.random() < 0.12:
                        c = speckle
                _dot(px, xx, yy, c)
                s += 0.5
            x += math.sin(a) * 0.5
            y -= math.cos(a) * 0.5
            d += 0.5
    return img


def aloe_tex(seed):
    """Aloe vera: upright, fleshy grey-green leaves with pale flecks and soft pale teeth, and a
    flower stalk of hanging yellow tubes out of the middle."""
    img = rosette_tex(seed, 9, 42, (6.0, 7.5), 1.6, [(140, 170, 124), (120, 156, 108),
                                                      (104, 140, 96)],
                      (96, 130, 88), (128, 132, 92), 0.35, speckle=(196, 210, 176),
                      teeth=(206, 212, 170))
    px = img.load()
    for y in range(5, 12):
        _dot(px, 8, y, (122, 132, 92))
    for y in range(5, 9):
        for side in (-1, 1):
            if (y + side) % 2:
                _dot(px, 8 + side, y, (246, 200, 56) if y < 7 else (236, 160, 40))
    return img


def bulb_tex(seed, heads, pattern, stems, leaf_len, leaf_count, broad):
    """Bulbs in flower (tulips, daffodils): strap leaves up from the ground, broad and grey-green
    for a tulip, and one flower to a bare stem."""
    rng = random.Random(seed)
    img, px = _canvas()
    leaf = [(104, 150, 98), (88, 134, 86), (120, 162, 110)]
    for i in range(leaf_count):
        x = 1.5 + (i + rng.uniform(0.1, 0.9)) * 13.0 / leaf_count
        n = rng.randint(*leaf_len)
        lean = rng.uniform(-0.3, 0.3)
        for k in range(n):
            c = leaf[rng.randrange(3)]
            _dot(px, x + lean * k, 15 - k, c)
            if broad and k < n - 2:
                _dot(px, x + lean * k + 1, 15 - k, leaf[1])
    count, lo, hi = stems
    for i in range(count):
        x = 1.5 + (i + rng.uniform(0.2, 0.8)) * 13.0 / count
        h = rng.randint(lo, hi)
        for k in range(h):
            _dot(px, x, 15 - k, (96, 140, 80))
        _head(px, x, 15 - h, pattern, heads[rng.randrange(len(heads))])
    return img


def sunflower_stem_tex(seed):
    """A sunflower's lower stem: a thick hairy stalk with big heart-shaped leaves on stalks,
    alternating up it (the cross planes' lower block)."""
    rng = random.Random(seed)
    img, px = _canvas()
    leaf = [(78, 128, 48), (66, 112, 40), (92, 142, 58)]
    for y in range(16):
        _dot(px, 7, y, (96, 140, 58))
        _dot(px, 8, y, (82, 124, 48) if y % 3 else (130, 160, 96))
    for row, side in ((3, 1), (7, -1), (11, 1), (14, -1)):
        cx = 8 + side * 4.5 if side > 0 else 7 + side * 4.5
        for d in (1, 2):
            _dot(px, (8 if side > 0 else 7) + side * d, row - d * 0.5, (96, 140, 58))
        for y in range(16):
            for x in range(16):
                u, v = (x - cx) / 3.0, (y - row) / 2.1
                if u * u + v * v <= 1:
                    _dot(px, x, y, (110, 156, 76) if abs(y - row) < 0.5 else
                         leaf[rng.randrange(3)])
    return img


def sunflower_head_tex(seed):
    """The sunflower's head on the top of its stalk: a broad brown disc in a ring of yellow
    rays, a leaf below it."""
    rng = random.Random(seed)
    img, px = _canvas()
    for y in range(9, 16):
        _dot(px, 7, y, (96, 140, 58))
        _dot(px, 8, y, (82, 124, 48))
    leaf = [(78, 128, 48), (66, 112, 40), (92, 142, 58)]
    for y in range(16):
        for x in range(16):
            u, v = (x - 3.5) / 3.0, (y - 12.5) / 2.0
            if u * u + v * v <= 1:
                _dot(px, x, y, leaf[rng.randrange(3)])
    cx, cy = 7.5, 5.0
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            if r < 2.7:
                c = [(92, 56, 26), (70, 40, 20), (112, 72, 34)][(x * 3 + y * 5) % 3]
            elif r < 5.2:
                ray = (math.atan2(dy, dx) + math.pi) / (2 * math.pi) * 18
                if r > 4.2 and (ray - int(ray)) > 0.6:
                    continue  # the gaps between the rays' tips
                c = [(250, 200, 30), (240, 176, 20), (252, 216, 60)][rng.randrange(3)]
            else:
                continue
            _dot(px, x, y, c)
    return img


def sunflower_item_tex(seed):
    """The sunflower's inventory sprite: the whole plant, head and all, in one square."""
    rng = random.Random(seed)
    img, px = _canvas()
    for y in range(6, 16):
        _dot(px, 8, y, (90, 134, 54))
    leaf = [(78, 128, 48), (66, 112, 40), (92, 142, 58)]
    for cx, cy in ((5.0, 12.0), (11.0, 9.5)):
        for y in range(16):
            for x in range(16):
                u, v = (x - cx) / 2.3, (y - cy) / 1.5
                if u * u + v * v <= 1:
                    _dot(px, x, y, leaf[rng.randrange(3)])
    for y in range(9):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 4.0)
            if r < 1.8:
                _dot(px, x, y, (92, 56, 26) if (x + y) % 2 else (70, 40, 20))
            elif r < 3.9:
                _dot(px, x, y, (250, 200, 30) if (x + y) % 2 else (240, 176, 20))
    return img


def vine_tex(seed, leaf, leaf_size, flowers, top):
    """A sprawling cucurbit vine seen from the side: leaves held up on stalks from runners along
    the ground, from row `top` down, and the odd yellow flower."""
    rng = random.Random(seed)
    img, px = _canvas()
    for x in range(16):
        _dot(px, x, 15 - (1 if (x // 3) % 2 else 0), (96, 132, 62))
    for i in range(6):
        x = 1.5 + (i + rng.uniform(0.1, 0.9)) * 13.0 / 6
        h = rng.randint(2, 15 - top - 2)
        for k in range(1, h):
            _dot(px, x + 0.15 * k * (1 if i % 2 else -1), 15 - k, (110, 146, 74))
        lx, ly = x + 0.15 * h * (1 if i % 2 else -1), 15 - h
        rx, ry = leaf_size
        for y in range(16):
            for xx in range(16):
                u, v = (xx - lx) / rx, (y - ly) / ry
                if u * u + v * v <= 1:
                    c = leaf[rng.randrange(len(leaf))]
                    if abs(xx - lx) < 0.5 or rng.random() < 0.06:
                        c = (150, 182, 120)  # midrib and the mottling
                    _dot(px, xx, y, c)
    for _ in range(flowers):
        _head(px, rng.uniform(2, 14), rng.randint(top + 3, 13), ["a.a", ".a."],
              {"a": (246, 196, 40)})
    return img


def bean_tex(seed):
    """A pole lima bean: a twining stem up the middle, trifoliate leaves off it, the odd white
    flower, and flat broad pods hanging in pairs."""
    rng = random.Random(seed)
    img, px = _canvas()
    leaf = [(84, 140, 60), (70, 124, 50), (98, 154, 70)]
    for y in range(16):
        _dot(px, 8 + round(math.sin(y * 0.8)), y, (92, 128, 58))
    for cy in range(1, 15, 3):
        side = 1 if cy % 2 else -1
        for dx, dy in ((2, 0), (3, -1), (3, 1), (4, 0), (2, -1), (5, 0), (4, 1)):
            _dot(px, 8 + side * dx, cy + dy, leaf[rng.randrange(3)])
    for cx, cy in ((5, 4), (11, 7), (6, 10), (10, 12), (12, 2)):
        for k in range(3):
            _dot(px, cx, cy + k, (152, 192, 92) if k else (132, 176, 80))
            _dot(px, cx + 1, cy + k + 1, (140, 184, 86))
    for cx, cy in ((4, 1), (12, 10)):
        _dot(px, cx, cy, (244, 242, 232))
        _dot(px, cx + 1, cy, (226, 222, 236))
    return img


def ribbed_tex(seed, colours, groove, period=3):
    """A ribbed fruit's skin (pumpkin): vertical lobes with darker grooves every `period` px."""
    rng = random.Random(seed)
    img, px = _canvas()
    for y in range(16):
        for x in range(16):
            c = groove if x % period == 0 else colours[rng.randrange(len(colours))]
            px[x, y] = clamp(tuple(v + rng.randint(-4, 4) for v in c)) + (255,)
    return img


def melon_tex(seed):
    """A watermelon's rind: pale green with dark wavy stripes along its length."""
    rng = random.Random(seed)
    img, px = _canvas()
    for y in range(16):
        for x in range(16):
            dark = (y + int(round(math.sin(x * 0.9 + y) * 0.8))) % 4 < 2
            c = (44, 92, 38) if dark else (140, 186, 96)
            px[x, y] = clamp(tuple(v + rng.randint(-5, 5) for v in c)) + (255,)
    return img


def boysenberry_tex(seed):
    """Boysenberry canes trained along two wires: canes up from the crown to the wires and
    tied along them, three-leaflet leaves, and clusters of berries ripening from red to the
    near-black purple they are picked at."""
    rng = random.Random(seed)
    img, px = _canvas()
    leaf = [(76, 128, 56), (62, 112, 46), (90, 142, 66)]
    cane = [(124, 72, 62), (106, 60, 52)]
    for wire in (3, 8):
        for x in range(16):
            _dot(px, x, wire, (156, 158, 160))
            _dot(px, x, wire + (1 if math.sin(x * 0.8 + wire) > 0 else -1), cane[x % 2])
    for x0 in (2, 8, 13):
        for y in range(4, 16):
            _dot(px, x0 + (1 if y % 4 == 0 else 0), y, cane[y % 2])
    for _ in range(70):
        _dot(px, rng.randrange(16), rng.choice((0, 1, 2, 4, 5, 6, 7, 9, 10, 11)),
             leaf[rng.randrange(3)])
    for cx, cy, ripe in ((2, 5, True), (6, 10, True), (10, 5, True), (13, 9, False),
                         (5, 0, True), (14, 1, True), (9, 10, False), (11, 0, True)):
        c = [(52, 18, 40), (84, 30, 56), (40, 14, 30)] if ripe else [(176, 40, 52),
                                                                   (150, 30, 44)]
        for ox, oy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            _dot(px, cx + ox, cy + oy, c[(ox + oy) % len(c)])
    return img


def lavender_tex(seed):
    """English lavender as a card: a loose mound of narrow, upright silvery-green leaves, and
    many slender stems standing well clear of it, each topped with a purple spike in whorls."""
    rng = random.Random(seed)
    img, px = _canvas()
    leaf = [(132, 164, 120), (116, 150, 106), (150, 178, 136), (100, 134, 94)]
    for _ in range(34):
        x = rng.uniform(1.0, 15.0)
        dist = abs(x - 8) / 7.0
        h = int((1 - dist * dist) * 5) + rng.randint(1, 2)
        lean = (x - 8) * 0.06
        for k in range(h):
            _dot(px, x + lean * k, 15 - k, leaf[rng.randrange(4)])
    purple = [(124, 96, 178), (98, 74, 156), (150, 122, 204)]
    for i in range(13):
        f = (i + 0.5) / 13 * 2 - 1
        x = 8 + f * 6 + rng.uniform(-0.4, 0.4)
        top = int(4 + abs(f) * 3 + rng.randint(0, 1))
        lean = f * 0.12
        for y in range(15, top - 1, -1):
            k = 15 - y
            xx = x + lean * k
            if y <= top + 3:
                c = purple[(y + i) % 3] if (y - top) % 2 == 0 or y == top else purple[1]
            else:
                c = (126, 150, 112)
            if y < 11 or y <= top + 3:
                _dot(px, xx, y, c)
    return img


def rosemary_tex(seed):
    """Rosemary as a card: an upright, dense shrub taller than it is wide, of stiff stems clothed
    in dark needle leaves, woody at the base, small pale-blue flowers scattered through it."""
    rng = random.Random(seed)
    img, px = _canvas()
    needle = [(62, 94, 62), (50, 80, 54), (78, 108, 74), (44, 70, 48)]
    for i in range(11):
        f = (i + 0.5) / 11 * 2 - 1
        x = 8 + f * 4.6 + rng.uniform(-0.3, 0.3)
        h = int(14 - abs(f) * 5 + rng.randint(-1, 0))
        lean = f * 0.08
        for k in range(h):
            xx, y = x + lean * k, 15 - k
            if k < 2:
                if i % 2 == 0:
                    _dot(px, xx, y, (110, 96, 76))
                continue
            _dot(px, xx, y, needle[rng.randrange(4)])
            for side in (-1, 1):
                if rng.random() < 0.75:
                    _dot(px, xx + side, y, needle[rng.randrange(4)])
            if rng.random() < 0.22 and k > 4:
                _dot(px, xx + rng.choice((-1, 1)), y, (170, 186, 232) if rng.random() < 0.6
                     else (146, 164, 222))
    return img


def mexican_bush_sage_tex(seed):
    """Mexican bush sage as a card: arching grey-green leafy stems in a loose low clump, and long
    velvety purple spikes curving outward over it, the odd white corolla showing."""
    img = arching_spikes_tex(seed, 13, (9.0, 11.0), 1.15, 1.0, (120, 138, 108),
                             [(132, 58, 150), (150, 78, 170), (112, 46, 132)], 5.5, 15,
                             dots=(236, 230, 242))
    rng = random.Random(seed + 1)
    px = img.load()
    leaf = [(118, 146, 104), (102, 130, 92), (136, 160, 118)]
    for i in range(16):
        f = (i + 0.5) / 16 * 2 - 1
        x, y, d = 8 + f * 2.0, 15.0, 0.0
        ang, L = f * 1.2, rng.uniform(5.0, 7.0)
        while d < L:
            a = ang * (1 + 0.6 * d / L)
            x += math.sin(a) * 0.5
            y -= math.cos(a) * 0.5
            d += 0.5
            _dot(px, x, y, leaf[rng.randrange(3)])
            if rng.random() < 0.5:
                _dot(px, x + math.cos(a), y + math.sin(a), leaf[rng.randrange(3)])
    return img


def fringe_tex(seed, palette, top, bottom, blooms=(), bloom_count=0, stems=None, droop=0,
               sprays=None):
    """A shrub's outline card: leaves filling a rounded, ragged-edged mound from row `top` to
    row `bottom`, bare stems below it to the ground, and flowers over it; drawn on four crossed
    planes round a smaller solid core, so the shrub reads as rounded and leafy, not a cube.
    blooms: (pattern, colours) pairs; droop: cascading sprays off each side ending in `sprays`."""
    rng = random.Random(seed)
    img, px = _canvas()
    cy, ry, rx = (top + bottom) / 2.0, (bottom - top) / 2.0, 7.6
    if stems:
        for x0 in (5.5, 7.5, 9.5, 11):
            for y in range(int(bottom) - 1, 16):
                _dot(px, x0 + (y % 3 == 0) * 0.6, y, stems[y % len(stems)])
    for y in range(16):
        for x in range(16):
            u, v = (x + 0.5 - 8) / rx, (y + 0.5 - cy) / ry
            r = u * u + v * v
            if r > 1.0:
                continue
            if rng.random() > (0.95 if r < 0.55 else 0.95 - (r - 0.55) * 1.7):
                continue
            px[x, y] = clamp(palette[rng.randrange(len(palette))]) + (255,)
    for k in range(droop):
        for side in (-1, 1):
            x, y = 8 + side * rng.uniform(3.5, 5.5), top + 1.5 + k * 2.2
            for d in range(5):
                x += side * 0.8
                y += 0.25 + d * 0.15
                _dot(px, x, y, palette[rng.randrange(len(palette))])
                _dot(px, x, y + 1, palette[rng.randrange(len(palette))])
            if sprays:
                _head(px, x, y + 1, ["ab", "ba", "a."], sprays)
    for pattern, colours in blooms:
        for _ in range(bloom_count):
            for _try in range(20):
                x, y = rng.uniform(2, 14), rng.uniform(top + 1, bottom - 1)
                u, v = (x - 8) / rx, (y - cy) / ry
                if u * u + v * v < 0.8:
                    break
            _head(px, x, y, pattern, colours)
    return img


TULIPS = [{"a": (220, 36, 44), "b": (176, 24, 36)}, {"a": (250, 214, 52), "b": (222, 180, 30)},
          {"a": (242, 128, 170), "b": (210, 92, 140)}, {"a": (246, 244, 238), "b": (214, 210, 200)},
          {"a": (128, 58, 140), "b": (98, 40, 112)}, {"a": (244, 126, 40), "b": (212, 96, 26)}]

TEXTURES.update({
    # Herbs and garden
    "lavender": lambda: lavender_tex(101),
    "rosemary": lambda: rosemary_tex(103),
    "mexican_bush_sage": lambda: mexican_bush_sage_tex(106),
    "russian_sage": lambda: russian_sage_tex(107),
    "star_jasmine": lambda: shrub_tex([(46, 96, 46), (36, 80, 38), (30, 66, 32), (62, 116, 58)], 108,
                                      berries=[(248, 248, 242), (234, 234, 224)],
                                      berry_count=7),
    "star_jasmine_fringe": lambda: fringe_tex(
        138, [(46, 96, 46), (36, 80, 38), (30, 66, 32), (62, 116, 58)], 6, 16,
        blooms=[(["a"], {"a": (250, 250, 244)}), ([".a.", "aya", ".a."],
                                                  {"a": (246, 246, 238), "y": (240, 230, 170)})],
        bloom_count=4),
    "bird_of_paradise": lambda: bird_of_paradise_tex(109),
    "foxtail_fern": lambda: foxtail_tex(110),
    "wild_mustard": lambda: mustard_tex(111),
    # Desert
    "saguaro": lambda: saguaro_tex(112),
    "saguaro_top": lambda: saguaro_top_tex(113),
    "golden_barrel": lambda: barrel_side_tex(114),
    "golden_barrel_top": lambda: barrel_top_tex(115),
    "prickly_pear_pad": lambda: pad_tex(116),
    "prickly_pear_fruit": lambda: pad_tex(116, fruit=True),
    "agave": lambda: rosette_tex(117, 11, 66, (7.0, 9.0), 2.3,
                                 [(162, 190, 194), (140, 170, 178), (120, 152, 162)],
                                 (112, 142, 150), (84, 70, 58), 0.2, teeth=(132, 112, 92)),
    "aloe_vera": lambda: aloe_tex(118),
    "echeveria": lambda: noise_tex([(170, 200, 190), (150, 186, 178), (134, 172, 166),
                                    (118, 158, 154)], 119, grain=0.7, speckle=0.1,
                                   speck_colour=[(212, 150, 172), (196, 128, 156)]),
    "jade": lambda: shrub_tex([(98, 164, 96), (84, 150, 84), (70, 134, 72), (58, 118, 62)], 120,
                              berries=[(176, 72, 62), (150, 62, 54)], berry_count=14),
    "jade_stems": lambda: twigs_tex([(112, 108, 72), (126, 120, 80), (98, 96, 62)], 121, 12),
    # Garden flowers
    "tulips": lambda: bulb_tex(122, TULIPS, ["a.a", "aba", "aba"], (6, 4, 6), (3, 5), 7, True),
    "daffodils": lambda: bulb_tex(
        123, [{"a": (250, 236, 120), "b": (246, 206, 40), "y": (236, 170, 24)},
              {"a": (246, 244, 232), "b": (246, 206, 40), "y": (236, 170, 24)}],
        ["a..", "aby", "a.."], (6, 4, 6), (4, 6), 8, False),
    "shrub_rose": lambda: shrub_tex([(58, 96, 48), (48, 82, 40), (38, 68, 32), (70, 108, 56)], 124,
                                    [(196, 20, 40), (170, 12, 32), (224, 52, 66)], 0.3),
    "shrub_rose_fringe": lambda: fringe_tex(
        139, [(58, 96, 48), (48, 82, 40), (38, 68, 32), (70, 108, 56)], 1, 13,
        blooms=[([".a.", "aba", ".a."], {"a": (206, 24, 44), "b": (150, 10, 28)})],
        bloom_count=7, stems=[(92, 84, 52), (110, 98, 60)]),
    "sunflower_stem": lambda: sunflower_stem_tex(125),
    "sunflower_head": lambda: sunflower_head_tex(126),
    "sunflower_item": lambda: sunflower_item_tex(127),
    "marigolds": lambda: herb_tex(
        128, [(58, 100, 44), (46, 86, 36), (70, 114, 52)], 10, 90, (9, 3, 5), (60, 100, 44),
        [{"a": (244, 140, 20), "b": (220, 100, 14)}, {"a": (250, 200, 30), "b": (232, 160, 20)},
         {"a": (200, 70, 20), "b": (244, 160, 30)}], [".a.", "aba", ".b."]),
    "zinnias": lambda: herb_tex(
        129, [(86, 140, 60), (72, 124, 50)], 10, 60, (7, 7, 11), (90, 136, 62),
        [{"a": (220, 40, 140), "b": (180, 24, 110), "y": (250, 210, 60)},
         {"a": (248, 120, 30), "b": (214, 90, 20), "y": (250, 210, 60)},
         {"a": (214, 30, 40), "b": (170, 20, 30), "y": (250, 210, 60)},
         {"a": (248, 210, 50), "b": (220, 176, 30), "y": (196, 120, 30)},
         {"a": (246, 140, 180), "b": (220, 104, 150), "y": (250, 210, 60)}],
        ["aya", "bab"]),
    "hibiscus": lambda: shrub_tex([(44, 96, 46), (36, 82, 38), (54, 110, 54), (28, 68, 32)], 130,
                                  [(220, 30, 40), (240, 70, 60), (190, 20, 36)], 0.08),
    "hibiscus_fringe": lambda: fringe_tex(
        140, [(44, 96, 46), (36, 82, 38), (54, 110, 54), (28, 68, 32)], 0, 12,
        blooms=[(["aa.", "aya", ".aa"], {"a": (224, 32, 44), "y": (248, 214, 64)})],
        bloom_count=4, stems=[(96, 86, 62), (114, 102, 72)]),
    "bougainvillea": lambda: shrub_tex([(70, 118, 52), (58, 102, 44), (84, 130, 60), (48, 88, 38)], 131,
                                       [(214, 40, 150), (232, 70, 170), (190, 28, 130),
                                        (240, 110, 190)], 0.45),
    "bougainvillea_fringe": lambda: fringe_tex(
        141, [(70, 118, 52), (58, 102, 44), (84, 130, 60), (48, 88, 38)], 2, 14, blooms=[(["ab", "ba"], {"a": (220, 46, 156), "b": (240, 112, 192)})], bloom_count=9, droop=3,
        sprays={"a": (220, 46, 156), "b": (240, 112, 192)}, stems=[(110, 92, 64), (126, 106, 74)]),
    # Crops
    "lima_bean_plant": lambda: bean_tex(132),
    "pumpkin_vine": lambda: vine_tex(133, [(54, 108, 44), (46, 94, 38), (64, 120, 52)],
                                     (2.6, 1.8), 3, 8),
    "pumpkin": lambda: ribbed_tex(134, [(236, 130, 30), (244, 146, 44), (226, 120, 26)],
                                  (196, 92, 20)),
    "watermelon_vine": lambda: vine_tex(135, [(96, 138, 84), (82, 124, 72), (110, 150, 96)],
                                        (1.8, 1.3), 1, 10),
    "watermelon": lambda: melon_tex(136),
    "boysenberry_canes": lambda: boysenberry_tex(137),
})


def cross_band(tex, y0, y1, uv_top):
    """Four crossed planes from y0 to y1, as cross_planes draws them, for a plant taller than a
    block (a sunflower's head plane above its stem's)."""
    els = cross_planes(tex, y1 - y0, uv_top=uv_top, four=True)
    for e in els:
        e["from"][1], e["to"][1] = y0, y1
    return els


def rosette_boxes(cx, cz, w, h, tex):
    """A succulent rosette (echeveria) of square layers, each turned 45 degrees from the one
    under it and narrower, so its outline is a star of pointed leaves."""
    els = []
    for scale, y0, y1, turn in ((1.0, 0, 0.45, 0), (0.8, 0.3, 0.72, 45), (0.56, 0.58, 0.9, 0),
                                (0.32, 0.8, 1.0, 45)):
        s = w * scale / 2
        el = box([cx - s, round(h * y0, 2), cz - s], [cx + s, round(h * y1, 2), cz + s], tex,
                 faces=("north", "south", "east", "west", "up"))
        if turn:
            el["rotation"] = {"origin": [cx, 0, cz], "axis": "y", "angle": turn, "rescale": False}
        els.append(el)
    return els


def prickly_pear_els():
    tex = {"pad": T("prickly_pear_pad"), "fruit": T("prickly_pear_fruit"),
           "particle": T("prickly_pear_pad")}
    els = [plane([2.5, 0, 8], [10.5, 9, 8], "pad", angle=22.5, origin=[6.5, 0, 8]),
           plane([6.5, 0, 7], [13.5, 8, 7], "pad", angle=-45, origin=[10, 0, 7]),
           plane([1, 7.5, 8.5], [7.5, 15, 8.5], "fruit", angle=-22.5, origin=[4.25, 0, 8.5]),
           plane([8.5, 6.5, 7.5], [14.5, 13.5, 7.5], "fruit", angle=45, origin=[11.5, 0, 7.5]),
           plane([5, 11, 8], [10, 16, 8], "pad", angle=0)]
    return tex, els


SAGUARO_TEX = {"ribs": T("saguaro"), "top": T("saguaro_top"), "particle": T("saguaro")}
SIDES4 = ("north", "south", "east", "west")


def saguaro_young():
    """A young saguaro, a column with no arms yet (they come at about 5 m): what is potted."""
    return SAGUARO_TEX, [box([5, 0, 5], [11, 13, 11], "ribs", faces=SIDES4),
                         box([5.5, 13, 5.5], [10.5, 14.5, 10.5], "ribs", per={"up": "top"},
                             faces=SIDES4 + ("up",))]


def saguaro_block(reg, names):
    """The saguaro is stacked from blocks (BlockParkSaguaro): the trunk in every block, the
    rounded crown on the top one, and a pair of arms on the second block of a saguaro three or
    more blocks tall, rising past the block into the one above, as a real saguaro branches only
    once it is several metres tall."""
    shaft = box([4, 0, 4], [12, 16, 12], "ribs", faces=SIDES4)
    crown = [box([4, 0, 4], [12, 14, 12], "ribs", faces=SIDES4),
             box([4.5, 14, 4.5], [11.5, 15.25, 11.5], "ribs", per={"up": "top"},
                 faces=SIDES4 + ("up",)),
             box([5.5, 15.25, 5.5], [10.5, 16, 10.5], "ribs", per={"up": "top"},
                 faces=SIDES4 + ("up",))]
    arms = []
    for x0, x1, ex0, ex1, y0, top in ((15, 21, 12, 15, 2, 25), (-5, 1, 1, 4, 5, 22)):
        arms += [wbox([ex0, y0, 5], [ex1, y0 + 5.5, 11], "ribs",
                      faces=("north", "south", "up", "down")),
                 wbox([x0, y0, 5], [x1, top, 11], "ribs",
                      faces=("north", "south", "east", "west", "down")),
                 wbox([x0 + 0.75, top, 5.75], [x1 - 0.75, top + 1.5, 10.25], "ribs",
                      per={"up": "top"}, faces=SIDES4 + ("up",))]
    item = [box([5, 0, 5], [11, 14, 11], "ribs", faces=SIDES4 + ("down",)),
            box([5.5, 14, 5.5], [10.5, 15, 10.5], "ribs", per={"up": "top"},
                faces=SIDES4 + ("up",)),
            box([11, 4, 6], [13, 7.5, 10], "ribs", faces=("north", "south", "up", "down")),
            box([13, 4, 6], [16, 11, 10], "ribs", faces=SIDES4 + ("down",)),
            box([13.5, 11, 6.5], [15.5, 12, 9.5], "ribs", per={"up": "top"},
                faces=SIDES4 + ("up",)),
            box([3, 6, 6], [5, 9, 10], "ribs", faces=("north", "south", "up", "down")),
            box([0, 6, 6], [3, 10.5, 10], "ribs", faces=SIDES4 + ("down",)),
            box([0.5, 10.5, 6.5], [2.5, 11.5, 9.5], "ribs", per={"up": "top"},
                faces=SIDES4 + ("up",))]
    add(reg, 'new BlockParkSaguaro("%s", 4)' % reg, names,
        {reg + "_shaft": model(SAGUARO_TEX, [shaft]),
         reg + "_crown": model(SAGUARO_TEX, crown),
         reg + "_arms": model(SAGUARO_TEX, arms),
         reg + "_item": model(SAGUARO_TEX, item)},
        {"multipart": [
            {"when": {"cap": "false"}, "apply": {"model": MODEL + reg + "_shaft"}},
            {"when": {"cap": "true"}, "apply": {"model": MODEL + reg + "_crown"}},
            {"when": {"arms": "true"}, "apply": {"model": MODEL + reg + "_arms"}}]},
        {"parent": "csm:block/parks/landscape/" + reg + "_item"})


def barrel_els():
    tex = {"body": T("golden_barrel"), "top": T("golden_barrel_top"),
           "particle": T("golden_barrel")}
    up = {"up": "top"}
    return tex, [box([4.5, 0, 4.5], [11.5, 1, 11.5], "body", faces=SIDES4),
                 box([3, 1, 3], [13, 6.5, 13], "body", per=up, faces=SIDES4 + ("up",)),
                 box([3.75, 6.5, 3.75], [12.25, 8.25, 12.25], "body", per=up,
                     faces=SIDES4 + ("up",)),
                 box([5.25, 8.25, 5.25], [10.75, 9.25, 10.75], "body", per=up,
                     faces=SIDES4 + ("up",))]


def echeveria_els():
    tex = {"leaves": T("echeveria"), "particle": T("echeveria")}
    els = rosette_boxes(8.5, 8.5, 8, 5, "leaves")
    for cx, cz, w, h in ((3, 3, 3.5, 3), (13, 4, 3, 2.5), (3.5, 13, 3.5, 3),
                         (13.5, 13, 3.5, 2.75)):
        els += rosette_boxes(cx, cz, w, h, "leaves")
    return tex, els


def sunflower_els():
    tex = {"stem": T("sunflower_stem"), "head": T("sunflower_head"),
           "particle": T("sunflower_head")}
    return tex, cross_band("stem", 0, 16, 0) + cross_band("head", 15, 30, 1)


# (registry, kind, height, inset, names en/de/es/sv, (textures, elements), item texture for a
# crossed-plane plant or None). Grouped by region (REGIONS), in tab order.
REGIONAL = [
    # --- California ---
    ("flower_california_poppy", "PLANT", 8, 2,
     ("California Poppy", "Kalifornischer Mohn", "Amapola de California", "Sömntuta"),
     plant_els("california_poppy", 10), "california_poppy"),
    ("shrub_manzanita", "SHRUB", 14, 1,
     ("Manzanita", "Manzanita", "Manzanita", "Manzanita"),
     shrub_els("manzanita", [([1, 5, 2], [10, 11, 11]), ([6, 7, 5], [15, 13, 14]),
                             ([3, 10.5, 4], [11, 14, 12])], stems=("manzanita_stems", 9)), None),
    ("shrub_ceanothus", "SHRUB", 12, 0,
     ("California Lilac", "Säckelblume", "Lila de California", "Kaliforniskt syren"),
     shrub_els("ceanothus", [([0.5, 0, 0.5], [15.5, 7, 15.5]), ([2, 6.5, 2], [14, 11, 14]),
                             ([4.5, 10.5, 4.5], [11.5, 12, 11.5])]), None),
    ("shrub_white_sage", "SHRUB", 8, 1,
     ("White Sage", "Weißer Salbei", "Salvia blanca", "Vit salvia"),
     shrub_els("white_sage", [([1, 0, 1], [15, 5, 15]), ([3, 4.5, 3], [13, 8, 13])],
               spikes="white_sage_spikes"), None),
    ("grass_deergrass", "PLANT", 14, 1,
     ("Deergrass", "Hirschgras", "Zacate de venado", "Hjortgräs"),
     plant_els("grass_deergrass", 16), "grass_deergrass"),
    # --- New Hampshire ---
    ("shrub_mountain_laurel", "SHRUB", 14, 1,
     ("Mountain Laurel", "Berglorbeer", "Laurel de montaña", "Bredbladig kalmia"),
     shrub_els("mountain_laurel", [([2, 0, 2], [14, 3, 14]), ([0.5, 2.5, 0.5], [15.5, 10, 15.5]),
                                   ([1.5, 9.5, 2.5], [13.5, 13, 14.5]),
                                   ([4, 12.5, 4], [11, 14, 11])]), None),
    ("shrub_highbush_blueberry", "SHRUB", 16, 1,
     ("Highbush Blueberry", "Amerikanische Heidelbeere", "Arándano alto", "Amerikanskt blåbär"),
     shrub_els("blueberry", [([2.5, 6, 2.5], [13.5, 12, 13.5]), ([1, 10, 1], [15, 15, 15]),
                             ([4, 14.5, 4], [12, 16, 12])], stems=("blueberry_stems", 7)), None),
    ("shrub_winterberry", "SHRUB", 15, 1,
     ("Winterberry", "Amerikanische Winterbeere", "Acebo de invierno", "Vinterbär"),
     shrub_els("winterberry", [([2, 5, 2], [14, 13, 14]), ([3.5, 12.5, 3.5], [12.5, 15, 12.5])],
               stems=("winterberry_stems", 6)), None),
    ("flower_lupine", "PLANT", 14, 2,
     ("Wild Lupine", "Wilde Lupine", "Lupino silvestre", "Vildlupin"),
     plant_els("lupine", 16), "lupine"),
    ("flower_ladys_slipper", "PLANT", 11, 3,
     ("Pink Lady's Slipper", "Rosa Frauenschuh", "Zapatilla de dama rosa", "Rosa guckusko"),
     plant_els("ladys_slipper", 12), "ladys_slipper"),
    # --- Colorado ---
    ("flower_columbine", "PLANT", 12, 2,
     ("Colorado Blue Columbine", "Blaue Akelei", "Aguileña azul de Colorado", "Blå akleja"),
     plant_els("columbine", 14), "columbine"),
    ("flower_penstemon", "PLANT", 14, 2,
     ("Rocky Mountain Penstemon", "Rocky-Mountain-Bartfaden", "Penstemon de las Rocosas",
      "Blå penstemon"),
     plant_els("penstemon", 16), "penstemon"),
    ("shrub_rabbitbrush", "SHRUB", 12, 0,
     ("Rubber Rabbitbrush", "Gummi-Kaninchenstrauch", "Arbusto de conejo", "Kaninbuske"),
     shrub_els("rabbitbrush", [([2, 0, 2], [14, 4, 14]), ([0.5, 3.5, 0.5], [15.5, 12, 15.5])],
               top="rabbitbrush_top"), None),
    ("shrub_sagebrush", "SHRUB", 13, 1,
     ("Big Sagebrush", "Dreizähniger Beifuß", "Artemisa tridentada", "Malörtsbuske"),
     shrub_els("sagebrush", [([1, 4, 3], [9, 10, 12]), ([7, 5.5, 1], [15, 12, 10]),
                             ([3, 9.5, 5], [12, 13, 14])], stems=("sagebrush_stems", 7)), None),
    ("grass_blue_grama", "PLANT", 10, 1,
     ("Blue Grama", "Moskitogras", "Navajita azul", "Moskitgräs"),
     plant_els("grass_blue_grama", 12), "grass_blue_grama"),
    ("plant_yucca", "PLANT", 15, 1,
     ("Soapweed Yucca", "Palmlilie", "Yuca", "Palmlilja"),
     plant_els("yucca", 16), "yucca"),
    # --- Florida ---
    ("plant_saw_palmetto", "PLANT", 14, 0,
     ("Saw Palmetto", "Sägepalme", "Palmito de sierra", "Sågpalmetto"),
     plant_els("saw_palmetto", 16), "saw_palmetto"),
    ("plant_coontie", "PLANT", 9, 1,
     ("Coontie", "Florida-Palmfarn", "Coontie", "Floridakottepalm"),
     plant_els("coontie", 10), "coontie"),
    ("shrub_beautyberry", "SHRUB", 13, 0,
     ("American Beautyberry", "Amerikanische Schönfrucht", "Calicarpa americana",
      "Amerikanskt praktbär"),
     shrub_els("beautyberry", [([3, 4, 3], [13, 8, 13]), ([0.5, 7.5, 0.5], [15.5, 12, 15.5]),
                               ([3, 11.5, 3], [13, 13, 13])], stems=("beautyberry_stems", 5)),
     None),
    ("shrub_firebush", "SHRUB", 16, 1,
     ("Firebush", "Feuerbusch", "Arbusto de fuego", "Eldbuske"),
     shrub_els("firebush", [([3, 0, 3], [13, 4, 13]), ([1.5, 3.5, 1.5], [14.5, 13, 14.5]),
                            ([3.5, 12.5, 3.5], [12.5, 16, 12.5])]), None),
    ("flower_coreopsis", "PLANT", 10, 2,
     ("Coreopsis", "Mädchenauge", "Coreopsis", "Flicköga"),
     plant_els("coreopsis", 12), "coreopsis"),
    ("grass_pink_muhly", "PLANT", 14, 0,
     ("Pink Muhly Grass", "Rosa Haargras", "Pasto muhly rosa", "Rosa muhlygräs"),
     plant_els("grass_pink_muhly", 16), "grass_pink_muhly"),
    # --- Japan ---
    ("shrub_satsuki_azalea", "SHRUB", 10, 0,
     ("Satsuki Azalea", "Satsuki-Azalee", "Azalea satsuki", "Satsukiazalea"),
     shrub_els("satsuki_azalea", [([1, 0, 1], [15, 4, 15]), ([0.5, 3.5, 0.5], [15.5, 7, 15.5]),
                                  ([2, 6.5, 2], [14, 9, 14]), ([4.5, 8.5, 4.5], [11.5, 10, 11.5])]),
     None),
    ("shrub_camellia", "SHRUB", 16, 1,
     ("Camellia", "Kamelie", "Camelia", "Kamelia"),
     shrub_els("camellia", [([3, 0, 3], [13, 3, 13]), ([1.5, 2.5, 1.5], [14.5, 12, 14.5]),
                            ([3, 11.5, 3], [13, 15, 13]), ([5, 14.5, 5], [11, 16, 11])]), None),
    ("flower_japanese_iris", "PLANT", 13, 1,
     ("Japanese Iris", "Japanische Sumpf-Schwertlilie", "Lirio japonés", "Japansk iris"),
     plant_els("japanese_iris", 14), "japanese_iris"),
    ("plant_bamboo", "PLANT", 16, 0,
     ("Bamboo", "Bambus", "Bambú", "Bambu"),
     plant_els("bamboo", 16), "bamboo"),
    ("grass_hakone", "PLANT", 7, 0,
     ("Japanese Forest Grass", "Japanisches Waldgras", "Hierba japonesa del bosque",
      "Japanskt skogsgräs"),
     plant_els("grass_hakone", 10), "grass_hakone"),
    # --- Sweden and Denmark ---
    ("shrub_heather", "SHRUB", 8, 0,
     ("Heather", "Besenheide", "Brezo", "Ljung"),
     shrub_els("heather", [([0.5, 0, 0.5], [15.5, 5, 15.5]), ([2, 4.5, 2], [14, 8, 14])]), None),
    ("shrub_lingonberry", "PLANT", 4, 0,
     ("Lingonberry", "Preiselbeere", "Arándano rojo", "Lingon"),
     shrub_els("lingonberry", [([0, 0, 0], [16, 2, 16]), ([1, 1.5, 1], [8, 4, 8]),
                               ([7, 1.5, 7.5], [15, 3.5, 15]), ([1.5, 1.5, 9], [7, 3, 15])]),
     None),
    ("flower_wood_anemone", "PLANT", 6, 1,
     ("Wood Anemone", "Buschwindröschen", "Anémona de bosque", "Vitsippa"),
     plant_els("wood_anemone", 8), "wood_anemone"),
    ("flower_harebell", "PLANT", 10, 2,
     ("Harebell", "Rundblättrige Glockenblume", "Campanilla", "Blåklocka"),
     plant_els("harebell", 12), "harebell"),
    ("flower_marguerite", "PLANT", 12, 2,
     ("Oxeye Daisy", "Margerite", "Margarita", "Prästkrage"),
     plant_els("marguerite", 14), "marguerite"),
    # --- Herbs and garden (GitHub #250) ---
    ("shrub_lavender", "SHRUB", 11, 2,
     ("English Lavender", "Echter Lavendel", "Lavanda inglesa", "Lavendel"),
     plant_els("lavender", 12), "lavender", {"ao": False}),
    ("shrub_rosemary", "SHRUB", 15, 2,
     ("Rosemary", "Rosmarin", "Romero", "Rosmarin"),
     plant_els("rosemary", 15), "rosemary", {"ao": False}),
    ("shrub_mexican_bush_sage", "SHRUB", 16, 0,
     ("Mexican Bush Sage", "Mexikanischer Buschsalbei", "Salvia mexicana",
      "Mexikansk busksalvia"),
     plant_els("mexican_bush_sage", 16), "mexican_bush_sage", {"ao": False}),
    ("shrub_russian_sage", "PLANT", 16, 1,
     ("Russian Sage", "Blauraute", "Salvia rusa", "Perovskia"),
     plant_els("russian_sage", 16), "russian_sage"),
    ("shrub_star_jasmine", "SHRUB", 9, 0,
     ("Star Jasmine", "Sternjasmin", "Jazmín estrella", "Stjärnjasmin"),
     shrub_els("star_jasmine", [([3.5, 0, 3.5], [12.5, 4, 12.5]), ([5, 3.5, 5.5], [11, 6, 11])],
               spikes="star_jasmine_fringe"), None),
    ("plant_bird_of_paradise", "PLANT", 16, 1,
     ("Bird of Paradise", "Paradiesvogelblume", "Ave del paraíso", "Papegojblomma"),
     plant_els("bird_of_paradise", 16), "bird_of_paradise"),
    ("plant_foxtail_fern", "PLANT", 10, 2,
     ("Foxtail Fern", "Fuchsschwanz-Spargel", "Helecho cola de zorro", "Rävsvansspargel"),
     plant_els("foxtail_fern", 10), "foxtail_fern"),
    ("flower_wild_mustard", "PLANT", 16, 2,
     ("Wild Mustard", "Acker-Senf", "Mostaza silvestre", "Åkersenap"),
     plant_els("wild_mustard", 16), "wild_mustard"),
    # --- Desert: cacti and succulents ---
    ("cactus_saguaro", "CACTUS", 15, 4,
     ("Saguaro", "Saguaro-Kaktus", "Saguaro", "Saguarokaktus"),
     saguaro_young(), None, {"custom": saguaro_block}),
    ("cactus_golden_barrel", "CACTUS", 10, 3,
     ("Golden Barrel Cactus", "Goldkugelkaktus", "Biznaga dorada", "Gyllene tunnkaktus"),
     barrel_els(), None),
    ("cactus_prickly_pear", "CACTUS", 16, 1,
     ("Prickly Pear", "Feigenkaktus", "Nopal", "Fikonkaktus"),
     prickly_pear_els(), None, {"ao": False}),
    ("plant_agave", "SHRUB", 12, 1,
     ("Century Plant", "Hundertjährige Agave", "Agave americano", "Amerikansk agave"),
     plant_els("agave", 12), "agave", {"ao": False}),
    ("plant_aloe_vera", "PLANT", 11, 3,
     ("Aloe Vera", "Aloe vera", "Aloe vera", "Aloe vera"),
     plant_els("aloe_vera", 11), "aloe_vera"),
    ("plant_echeveria", "PLANT", 5, 0,
     ("Echeveria", "Echeverie", "Echeveria", "Echeveria"),
     echeveria_els(), None),
    ("plant_jade", "SHRUB", 12, 2,
     ("Jade Plant", "Geldbaum", "Árbol de jade", "Paradisträd"),
     shrub_els("jade", [([3, 3.5, 3], [9, 8.5, 9]), ([7, 4, 6], [14, 10, 13]),
                        ([2, 6.5, 7], [9, 11, 14]), ([5, 9.5, 4], [11, 12, 10])],
               stems=("jade_stems", 4)), None),
    # --- Garden flowers ---
    ("flower_tulips", "PLANT", 9, 2,
     ("Tulips", "Tulpen", "Tulipanes", "Tulpaner"),
     plant_els("tulips", 10), "tulips"),
    ("flower_daffodils", "PLANT", 8, 2,
     ("Daffodils", "Osterglocken", "Narcisos", "Påskliljor"),
     plant_els("daffodils", 9), "daffodils"),
    ("shrub_rose", "SHRUB", 15, 1,
     ("Shrub Rose", "Strauchrose", "Rosal arbustivo", "Buskros"),
     shrub_els("shrub_rose", [([4, 4, 4], [12, 11.5, 12]), ([5.5, 11, 5.5], [10.5, 13, 10.5])],
               spikes="shrub_rose_fringe"), None),
    ("flower_sunflower", "PLANT", 28, 3,
     ("Sunflower", "Sonnenblume", "Girasol", "Solros"),
     sunflower_els(), "sunflower_item"),
    ("flower_marigolds", "PLANT", 7, 1,
     ("Marigolds", "Studentenblumen", "Tagetes", "Tagetes"),
     plant_els("marigolds", 8), "marigolds"),
    ("flower_zinnias", "PLANT", 12, 2,
     ("Zinnias", "Zinnien", "Zinnias", "Zinnior"),
     plant_els("zinnias", 13), "zinnias"),
    ("shrub_hibiscus", "SHRUB", 16, 1,
     ("Hibiscus", "Chinesischer Roseneibisch", "Hibisco", "Hibiskus"),
     shrub_els("hibiscus", [([4, 5, 4], [12, 13.5, 12]), ([5.5, 13, 5.5], [10.5, 15, 10.5])],
               spikes="hibiscus_fringe"), None),
    ("shrub_bougainvillea", "SHRUB", 14, 0,
     ("Bougainvillea", "Bougainvillea", "Buganvilla", "Bougainvillea"),
     shrub_els("bougainvillea", [([4, 3, 4], [12, 10, 12]), ([5.5, 9.5, 5.5], [10.5, 12, 10.5])],
               spikes="bougainvillea_fringe"), None),
]
# where each group starts in REGIONAL: the six regions, then herbs and garden plants,
# cacti and succulents, and garden flowers
REGIONS = [0, 5, 10, 16, 22, 27, 32, 40, 47, 55]


def potted_names(names):
    return ("Potted " + names[0], names[1] + " im Topf", names[2] + " en maceta",
            names[3] + " i kruka")


for start, end in zip(REGIONS, REGIONS[1:]):
    for row in REGIONAL[start:end]:
        reg, kind, height, inset, names, (tex, els), item = row[:7]
        # An optional eighth entry: "custom", a function that adds the block itself (the
        # stacking saguaro), or "ao" for a box-kind plant drawn as planes.
        extra = row[7] if len(row) > 7 else {}
        if "custom" in extra:
            extra["custom"](reg, names)
            continue
        ao = extra.get("ao", kind in ("SHRUB", "CACTUS"))
        models = {reg: model(tex, els, ao=ao)}
        state = simple_state(reg)
        if item:
            models[reg + "_item"] = {"parent": "item/generated", "textures": {"layer0": T(item)}}
            state = simple_state(reg, item=MODEL + reg + "_item")
        prop(reg, kind, height, inset, names, models, state)
    # The same plants in nursery pots, after the region's plants.
    for row in REGIONAL[start:end]:
        reg, kind, height, inset, names, (tex, els), item = row[:7]
        preg = "potted_" + reg
        ptex, pels = potted(tex, els)
        top = int(math.ceil(POT_LIFT + height * POT_SCALE))
        prop(preg, "SHRUB", min(16, top), 3, potted_names(names),
             {preg: model(ptex, pels, ao=False)}, simple_state(preg))


# ------------------------------------------------------------------------------------------
# The nursery, garden centre and farm: more planters, the benches and stock a plant nursery is
# laid out with, and a small farm's rows, compost and barrow. Plants for them are the potted_
# blocks above; nothing here is a potted plant again.
# ------------------------------------------------------------------------------------------
TERRACOTTA = [(204, 112, 72), (190, 100, 64), (174, 90, 58), (156, 80, 52)]
GLAZE = [(52, 92, 168), (42, 78, 150), (34, 66, 132), (26, 54, 112)]
OAK = [(150, 110, 70), (136, 98, 62), (120, 86, 54), (104, 74, 46)]
GALVANIZED = [(182, 186, 186), (166, 170, 172), (150, 154, 156), (134, 138, 140)]
PLASTIC = [(46, 46, 48), (40, 40, 42), (34, 34, 36)]


def rim_top(palette, lo, hi, seed, width=1, rim=None):
    """Soil in a pot seen from above, with the pot's rim as a ring at pixels lo .. hi - 1: the up
    face of a rim box from lo to hi samples exactly that window, so the ring lies on its edge."""
    img = noise_tex(SOIL, seed, grain=0.8)
    ring = rim if rim is not None else noise_tex(palette, seed + 1, grain=0.5)
    px, rp = img.load(), ring.load()
    for y in range(lo, hi):
        for x in range(lo, hi):
            if min(x - lo, y - lo, hi - 1 - x, hi - 1 - y) < width:
                px[x, y] = rp[x, y]
    return img


def banded(palette, seed, rows=(), shade=0.8):
    """A noise texture darkened along the given rows (a pot's moulded bands)."""
    img = noise_tex(palette, seed, grain=0.6)
    px = img.load()
    for y in rows:
        for x in range(16):
            px[x, y] = clamp(tuple(c * shade for c in px[x, y][:3])) + (255,)
    return img


def glaze_tex(seed):
    """A reactive glaze: deep blue, lighter where it thinned over the shoulder, with speckles."""
    rng = random.Random(seed)
    img = noise_tex(GLAZE, seed, grain=0.5)
    px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, _ = px[x, y]
            if y < 5:  # the lighter band where the glaze ran thin
                r, g, b = r + 30, g + 36, b + 40
            if rng.random() < 0.06:
                r, g, b = r + 60, g + 60, b + 50
            px[x, y] = clamp((r, g, b)) + (255,)
    return img


def staves_tex(seed):
    """Barrel staves: upright boards with dark joints every 3 pixels."""
    img = noise_tex(OAK, seed, grain=0.5)
    px = img.load()
    for x in range(0, 16, 3):
        for y in range(16):
            px[x, y] = clamp(OAK[-1]) + (255,)
    return img


def mesh_tex():
    """A nursery bench's expanded steel mesh: diamonds of galvanized strand, open between."""
    img, px = _canvas()
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                px[x, y] = clamp(GALVANIZED[(x // 4 + y) % 3]) + (255,)
    return img


def pot_stack_tex(seed):
    """Nested black pots seen from the side: a lip every 2 pixels, each a little lighter."""
    rng = random.Random(seed)
    img, px = _canvas()
    for y in range(16):
        for x in range(16):
            v = 38 + rng.randint(-3, 3) + (16 if y % 2 == 0 else 0)
            px[x, y] = (v, v, v + 2, 255)
    return img


def pot_mouth_tex():
    """The top pot of a stack, seen from above: its rim, and the dark inside nested pots."""
    img, px = _canvas()
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            v = 56 if edge < 2 else (26 if edge < 5 else 18 + (edge % 2) * 6)
            px[x, y] = (v, v, v + 2, 255)
    return img


def tray_tex(seed):
    """A plug tray from above: 2 px cells in black plastic, compost in each, a seedling's green
    leaves in most."""
    rng = random.Random(seed)
    img, px = _canvas()
    for y in range(16):
        for x in range(16):
            if x % 2 == 0 or y % 2 == 0:
                px[x, y] = (30, 30, 32, 255)
            else:
                px[x, y] = clamp(SOIL[rng.randrange(4)]) + (255,)
                if rng.random() < 0.75:
                    px[x, y] = clamp((96 + rng.randint(-12, 12), 164, 70)) + (255,)
    return img


def seedlings_tex(seed):
    """A row of seedlings from the side: pairs of seed leaves on short stems."""
    rng = random.Random(seed)
    img, px = _canvas()
    for i in range(8):
        x = 1 + i * 2
        h = rng.randint(1, 2)
        for k in range(h):
            _dot(px, x, 15 - k, (120, 160, 84))
        for dx in (-1, 1):
            _dot(px, x + dx, 15 - h, (104, 172, 72) if dx < 0 else (126, 186, 86))
        _dot(px, x, 15 - h, (112, 176, 78))
    return img


def compost_tex(seed):
    """Compost: dark crumbly humus flecked with scraps (leaves, peel, eggshell)."""
    return noise_tex([(84, 62, 42), (70, 52, 34), (58, 42, 28), (44, 32, 22)], seed, cells=8,
                     grain=0.9, speckle=0.12,
                     speck_colour=[(104, 132, 56), (196, 132, 44), (222, 214, 190), (140, 96, 52)])


def tilled_tex(seed):
    """Tilled soil from above: furrows along the row."""
    img = noise_tex(SOIL, seed, grain=0.8)
    px = img.load()
    for y in range(0, 16, 4):
        for x in range(16):
            px[x, y] = clamp(tuple(c * 0.72 for c in px[x, y][:3])) + (255,)
    return img


def tomato_tex(seed):
    """A staked tomato plant from the side: a stem up the middle, leaves off it, trusses of red
    and green fruit."""
    rng = random.Random(seed)
    img, px = _canvas()
    leaf = [(70, 122, 52), (58, 104, 44), (84, 138, 60)]
    for y in range(1, 16):
        _dot(px, 8 + (0.6 if y % 5 < 2 else 0), y, (82, 120, 54))
    for _ in range(80):
        y = rng.randrange(1, 15)
        spread = 2 + (15 - abs(y - 8)) * 0.35
        _dot(px, 8 + rng.uniform(-spread, spread), y, leaf[rng.randrange(3)])
    for cx, cy, ripe in ((5, 11, True), (11, 9, True), (6, 6, False), (10, 4, True),
                         (4, 8, False), (12, 13, False)):
        c = [(214, 44, 36), (190, 30, 28)] if ripe else [(150, 180, 70), (126, 160, 58)]
        for ox, oy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            _dot(px, cx + ox, cy + oy, c[(ox + oy) % 2])
    return img


def trellis_tex(seed):
    """A timber lattice, square on the diagonal, with a clematis climbing it: stems, leaves, and
    big purple flowers."""
    rng = random.Random(seed)
    img, px = _canvas()
    for y in range(16):
        for x in range(16):
            if (x + y) % 8 in (0, 1) or (x - y) % 8 in (0, 1):
                px[x, y] = clamp(STAKE[(x + 2 * y) % 4]) + (255,)
    leaf = [(64, 116, 50), (52, 100, 42), (80, 132, 58)]
    x = 7.0
    for y in range(15, -1, -1):
        x += rng.uniform(-0.8, 0.8)
        x = max(3, min(12, x))
        _dot(px, x, y, (78, 104, 50))
        for _ in range(3):
            _dot(px, x + rng.uniform(-3.5, 3.5), y + rng.uniform(-1, 1), leaf[rng.randrange(3)])
    for cx, cy in ((4, 3), (11, 6), (6, 10), (12, 13), (9, 1)):
        _head(px, cx, cy + 1, [".a.", "aya", ".a."],
              {"a": (132, 72, 184), "y": (236, 222, 170)})
    return img


def lettuce_tex(seed):
    return leafy([(150, 204, 92), (128, 186, 76), (106, 166, 62), (86, 144, 50)], seed)


TEXTURES.update({
    "terracotta": lambda: banded(TERRACOTTA, 81, rows=(3,)),
    "terracotta_top_small": lambda: rim_top(TERRACOTTA, 4, 12, 82),
    "terracotta_top_large": lambda: rim_top(TERRACOTTA, 2, 14, 83),
    "glazed_urn": lambda: glaze_tex(84),
    "glazed_urn_top": lambda: rim_top(GLAZE, 4, 12, 85, rim=glaze_tex(84)),
    "barrel_staves": lambda: staves_tex(86),
    "barrel_top": lambda: rim_top(OAK, 1, 15, 87, width=2, rim=staves_tex(86)),
    "galvanized": lambda: noise_tex(GALVANIZED, 88, grain=0.7),
    "bench_mesh": mesh_tex,
    "pot_stack": lambda: pot_stack_tex(89),
    "pot_mouth": pot_mouth_tex,
    "tray_plastic": lambda: noise_tex(PLASTIC, 90, grain=0.8),
    "seedling_tray": lambda: tray_tex(91),
    "seedlings": lambda: seedlings_tex(92),
    "compost": lambda: compost_tex(93),
    "weathered_wood": lambda: noise_tex([(158, 142, 118), (142, 126, 104), (126, 112, 92),
                                         (110, 96, 80)], 94, grain=0.6),
    "barrow_green": lambda: noise_tex([(70, 128, 72), (60, 114, 62), (52, 100, 54)], 95,
                                      grain=0.7),
    "tire": lambda: solid((34, 34, 36)),
    "tilled_soil": lambda: tilled_tex(97),
    "lettuce": lambda: lettuce_tex(98),
    "tomato_plant": lambda: tomato_tex(99),
    "trellis_clematis": lambda: trellis_tex(100),
})


def facing_prop(registry, box6, names, models, collides=True, crop=False):
    java = ('new BlockParkCrop("%s", new int[]{%s})' if crop else
            'new BlockParkFacing("%s", new int[]{%s}, ' + ("true" if collides else "false") + ")")
    add(registry, java % (registry, ", ".join(str(v) for v in box6)), names, models,
        facing_state(registry))


# --- terracotta pots: a tapered body under a rolled rim ---
for size, (foot, body, rim, height), names in (
        ("small", ((5.5, 0, 2), (5, 2, 6), (4, 6, 8), 8),
         ("Small Terracotta Pot", "Kleiner Terrakottatopf", "Maceta de terracota pequeña",
          "Liten terrakottakruka")),
        ("large", ((4, 0, 3), (3, 3, 11), (2, 11, 13), 13),
         ("Large Terracotta Pot", "Großer Terrakottatopf", "Maceta de terracota grande",
          "Stor terrakottakruka"))):
    reg = "planter_terracotta_" + size
    tex = {"clay": T("terracotta"), "top": T("terracotta_top_" + size),
           "particle": T("terracotta")}
    (fi, fy0, fy1), (bi, by0, by1), (ri, ry0, ry1) = foot, body, rim
    sides = ("north", "south", "east", "west")
    els = [box([fi, fy0, fi], [16 - fi, fy1, 16 - fi], "clay", faces=sides + ("down",)),
           box([bi, by0, bi], [16 - bi, by1, 16 - bi], "clay", faces=sides + ("down",)),
           box([ri, ry0, ri], [16 - ri, ry1, 16 - ri], "clay", per={"up": "top"})]
    prop(reg, "PLANTER", height, int(ri), names, {reg: model(tex, els)}, simple_state(reg))

# --- a glazed ceramic urn: bellied, a narrow neck, a thick lip ---
urn_tex = {"glaze": T("glazed_urn"), "top": T("glazed_urn_top"), "particle": T("glazed_urn")}
prop("planter_glazed_urn", "PLANTER", 14, 2,
     ("Glazed Ceramic Urn", "Glasierte Keramikurne", "Urna de cerámica esmaltada",
      "Glaserad keramikurna"),
     {"planter_glazed_urn": model(urn_tex, [
         box([5, 0, 5], [11, 1.5, 11], "glaze", faces=("north", "south", "east", "west", "down")),
         box([3.5, 1.5, 3.5], [12.5, 4, 12.5], "glaze",
             faces=("north", "south", "east", "west", "down")),
         box([2.5, 4, 2.5], [13.5, 10, 13.5], "glaze"),
         box([3.5, 10, 3.5], [12.5, 12, 12.5], "glaze"),
         box([4, 12, 4], [12, 14, 12], "glaze", per={"up": "top"}),
     ])}, simple_state("planter_glazed_urn"))

# --- a concrete bowl on a pedestal, planted with bedding flowers ---
bowl_tex = {"concrete": T("planter_concrete"), "top": T("planter_concrete_top"),
            "flowers": T("mixed_basket"), "particle": T("planter_concrete")}
prop("planter_concrete_bowl", "PLANTER", 8, 1,
     ("Concrete Bowl Planter", "Beton-Pflanzschale", "Jardinera de cuenco de hormigón",
      "Planteringsskål av betong"),
     {"planter_concrete_bowl": model(bowl_tex, [
         box([5, 0, 5], [11, 1, 11], "concrete"),
         box([6, 1, 6], [10, 3, 10], "concrete", faces=("north", "south", "east", "west")),
         box([3, 3, 3], [13, 5, 13], "concrete"),
         box([1, 5, 1], [15, 8, 15], "concrete", per={"up": "top"}),
         # Bedding flowers mounded in it, a little short of its rim.
         box([2, 8, 2], [14, 10.5, 14], "flowers", faces=("north", "south", "east", "west", "up")),
         box([4, 10.5, 4], [12, 11.5, 12], "flowers", faces=("north", "south", "east", "west", "up")),
     ], ao=False)}, simple_state("planter_concrete_bowl"))

# --- a half whiskey barrel: oak staves, two iron hoops standing just proud of them ---
barrel_tex = {"staves": T("barrel_staves"), "top": T("barrel_top"), "iron": T("steel"),
              "particle": T("barrel_staves")}
hoop = ("north", "south", "east", "west", "up", "down")
prop("planter_half_barrel", "PLANTER", 10, 1,
     ("Half Barrel Planter", "Halbfass-Pflanzkübel", "Jardinera de medio barril",
      "Halvtunna för plantering"),
     {"planter_half_barrel": model(barrel_tex, [
         box([1.5, 0, 1.5], [14.5, 10, 14.5], "staves", per={"up": "top"}),
         box([1.2, 1.5, 1.2], [14.8, 2.5, 14.8], "iron", faces=hoop[:4] + ("up", "down")),
         box([1.2, 7, 1.2], [14.8, 8, 14.8], "iron", faces=hoop[:4] + ("up", "down")),
     ])}, simple_state("planter_half_barrel"))

# --- a window box: a cedar trough on two brackets against the wall (+Z), trailing flowers ---
wbox_tex = {"wood": T("planter_wood"), "iron": T("steel"), "flowers": T("mixed_basket"),
            "trail": T("petunia"), "particle": T("planter_wood")}
facing_prop("window_box_flowers", [1, 0, 10, 15, 10, 16],
            ("Window Box with Flowers", "Blumenkasten", "Jardinera de ventana con flores",
             "Blomlåda"),
            {"window_box_flowers": model(wbox_tex, [
                box([2.5, 0, 14], [3.5, 2, 16], "iron", faces=("north", "east", "west", "down")),
                box([12.5, 0, 14], [13.5, 2, 16], "iron", faces=("north", "east", "west", "down")),
                box([1, 2, 10], [15, 7, 16], "wood"),
                # The flowers heaped in the box, and trailing over its front.
                box([1.5, 7, 10.5], [14.5, 10, 15.5], "flowers",
                    faces=("north", "east", "west", "up")),
                plane([1, 4, 9.5], [15, 8, 9.5], "trail", uv=(1, 8, 15, 12)),
            ], ao=False)})

# --- the nursery bench: a steel frame a block high, an expanded-mesh top; joins into a run ---
bench_tex = {"steel": T("galvanized"), "mesh": T("bench_mesh"), "particle": T("galvanized")}
RAIL = (14.5, 16)
bench_post = model(bench_tex, [
    {"from": [0, 15.6, 0], "to": [16, 15.6, 16], "shade": True,
     "faces": {"up": face("mesh", [0, 0, 16, 16]), "down": face("mesh", [0, 0, 16, 16])}},
])
# A side's rail stops short of the corners (joining() draws corner pieces), and each side
# carries the leg at its left-hand corner, so a run has one leg a block along each edge and
# every corner of the whole bench has one.
bench_side = model(bench_tex, [
    box([1, RAIL[0], 0], [15, RAIL[1], 1], "steel", faces=("north", "south", "up", "down")),
    box([1, 0, 1], [2.5, RAIL[0], 2.5], "steel", faces=("north", "south", "east", "west")),
])
bench_corner = model(bench_tex, [box([15, RAIL[0], 0], [16, RAIL[1], 1], "steel")])
bench_item = ([box([0, RAIL[0], 0], [16, RAIL[1], 1], "steel"),
               box([0, RAIL[0], 15], [16, RAIL[1], 16], "steel"),
               box([0, RAIL[0], 1], [1, RAIL[1], 15], "steel", faces=hoop[:4] + ("up", "down")),
               box([15, RAIL[0], 1], [16, RAIL[1], 15], "steel", faces=hoop[:4] + ("up", "down"))]
              + [box([x, 0, z], [x + 1.5, RAIL[0], z + 1.5], "steel",
                     faces=("north", "south", "east", "west"))
                 for x in (1, 13.5) for z in (1, 13.5)])
joining("nursery_bench", "TABLE", 16, 16,
        ("Nursery Growing Bench", "Gärtnerei-Kulturtisch", "Mesa de cultivo de vivero",
         "Odlingsbord för plantskola"),
        bench_post, bench_side, item_extra=bench_item, side_when="false", corner=bench_corner)

# --- the potting bench: a cedar worktop with a splashback, a shelf of pots underneath ---
pb_tex = {"wood": T("planter_wood"), "legs": T("stake"), "soil": T("soil"),
          "clay": T("terracotta"), "top": T("terracotta_top_small"), "pot": T("nursery_pot"),
          "pot_top": T("nursery_pot_top"), "particle": T("planter_wood")}
def full_top(el):
    """A small pot's top: its whole rim-and-soil texture, not the window under the pot."""
    el["faces"]["up"]["uv"] = [4, 4, 12, 12]
    return el


legs = [box([x, 0, z], [x + 1.5, 13, z + 1.5], "legs", faces=("north", "south", "east", "west"))
        for x in (0.5, 14) for z in (3.5, 14)]
facing_prop("potting_bench", [0, 0, 3, 16, 16, 16],
            ("Potting Bench", "Pflanztisch", "Mesa de trasplante", "Planteringsbänk"),
            {"potting_bench": model(pb_tex, legs + [
                box([0, 3, 4], [16, 4, 15.5], "wood"),
                box([0, 13, 3], [16, 14.5, 16], "wood"),
                box([0, 14.5, 15], [16, 16, 16], "wood", faces=hoop[:5]),
                # A heap of potting soil, and pots on the worktop and the shelf below.
                box([1.5, 14.5, 7], [7.5, 15.5, 13.5], "soil", faces=hoop[:5]),
                box([2.5, 15.5, 8], [6, 16, 12], "soil", faces=hoop[:5]),
                box([10, 14.5, 7], [13, 16, 10], "clay", faces=hoop[:4]),
                full_top(wbox([9.5, 16, 6.5], [13.5, 17, 10.5], "clay", per={"up": "top"})),
                full_top(box([2, 4, 6], [7, 7.5, 11], "pot", per={"up": "pot_top"})),
                full_top(box([9, 4, 7], [13, 7, 11], "pot", per={"up": "pot_top"})),
            ])})

# --- stacks of empty nursery pots, nested ---
stack_tex = {"pot": T("pot_stack"), "mouth": T("pot_mouth"), "particle": T("nursery_pot")}


def pot_column(x0, z0, w, h):
    el = box([x0, 0, z0], [x0 + w, h, z0 + w], "pot", faces=("north", "south", "east", "west",
                                                             "up"))
    el["faces"]["up"] = face("mouth", [0, 0, 16, 16])
    return el


prop("nursery_pot_stack", "PLANTER", 12, 1,
     ("Stacked Nursery Pots", "Gestapelte Pflanztöpfe", "Macetas de vivero apiladas",
      "Staplade plantskolekrukor"),
     {"nursery_pot_stack": model(stack_tex, [
         pot_column(1.5, 1.5, 6, 10), pot_column(9, 2, 5.5, 7), pot_column(4.5, 9, 6, 12),
     ])}, simple_state("nursery_pot_stack"))

# --- seedling flats: two plug trays of seedlings, rows of seed leaves along each ---
flat_tex = {"tray": T("tray_plastic"), "cells": T("seedling_tray"), "sprouts": T("seedlings"),
            "particle": T("seedling_tray")}
flats = []
for x0, x1 in ((0.5, 7.75), (8.25, 15.5)):
    flats.append(box([x0, 0, 0.5], [x1, 2, 15.5], "tray", per={"up": "cells"}))
    for z in (2.5, 5.5, 8.5, 11.5, 14):
        flats.append(plane([x0 + 0.25, 2, z], [x1 - 0.25, 5, z], "sprouts",
                           uv=(x0 + 0.25, 13, x1 - 0.25, 16)))
prop("seedling_flats", "PLANTER", 3, 0,
     ("Seedling Flats", "Anzuchtschalen mit Sämlingen", "Bandejas de plántulas",
      "Plantbrätten med småplantor"),
     {"seedling_flats": model(flat_tex, flats, ao=False)}, simple_state("seedling_flats"))

# --- a slatted compost bin, full of compost ---
compost_tex_map = {"wood": T("weathered_wood"), "compost": T("compost"),
                   "particle": T("weathered_wood")}
compost = [box([x, 0, z], [x + 2, 14, z + 2], "wood") for x in (0, 14) for z in (0, 14)]
for y in range(1, 13, 3):
    compost += [box([2, y, 0.5], [14, y + 2, 1.5], "wood", faces=("north", "south", "up", "down")),
                box([2, y, 14.5], [14, y + 2, 15.5], "wood",
                    faces=("north", "south", "up", "down")),
                box([0.5, y, 2], [1.5, y + 2, 14], "wood", faces=("east", "west", "up", "down")),
                box([14.5, y, 2], [15.5, y + 2, 14], "wood", faces=("east", "west", "up", "down"))]
compost.append(box([2, 0, 2], [14, 10, 14], "compost", faces=hoop[:5]))
prop("compost_bin", "PLANTER", 14, 0,
     ("Wooden Compost Bin", "Holzkomposter", "Compostador de madera", "Kompostlåda av trä"),
     {"compost_bin": model(compost_tex_map, compost)}, simple_state("compost_bin"))

# --- a wheelbarrow of soil: a green steel tray, the wheel at the front (north) ---
barrow_tex = {"paint": T("barrow_green"), "steel": T("steel"), "tire": T("tire"),
              "soil": T("soil"), "particle": T("barrow_green")}
# The wheel is an exact octagon: a cross of two rectangles and the same cross turned 45 degrees
# (a square and the square turned would be a star). The tyre is one flat colour, so where the
# four put their sides on one plane they show the same pixels.
WHEEL_C, WHEEL_R = (2.5, 2.75), 2.25
WHEEL_H = WHEEL_R * math.tan(math.pi / 8)
wheel = []
for turn in (0, 45):
    for dy, dz in ((WHEEL_H, WHEEL_R), (WHEEL_R, WHEEL_H)):
        el = box([7, WHEEL_C[0] - dy, WHEEL_C[1] - dz], [9, WHEEL_C[0] + dy, WHEEL_C[1] + dz],
                 "tire")
        if turn:
            el["rotation"] = {"origin": [8, WHEEL_C[0], WHEEL_C[1]], "axis": "x", "angle": turn,
                              "rescale": False}
        wheel.append(el)
handle = [box([x, 6, 11], [x + 1, 7, 16], "steel", faces=("east", "west", "up", "down", "south"))
          for x in (2.5, 12.5)]
for h in handle:
    h["rotation"] = {"origin": [h["from"][0], 6, 11], "axis": "x", "angle": -22.5,
                     "rescale": False}
facing_prop("wheelbarrow", [2, 0, 0, 14, 10, 16],
            ("Wheelbarrow", "Schubkarre", "Carretilla", "Skottkärra"),
            {"wheelbarrow": model(barrow_tex, [
                box([7.5, 2, 2], [8.5, 6, 3], "steel", faces=("east", "west", "north", "south")),
                box([3, 5, 2.5], [13, 6, 12], "paint", faces=("down", "north", "south", "east",
                                                              "west")),
                box([2.5, 6, 1.5], [13.5, 10, 2.5], "paint"),
                box([2.5, 6, 11.5], [13.5, 10, 12.5], "paint"),
                box([2.5, 6, 2.5], [3.5, 10, 11.5], "paint", faces=("east", "west", "up")),
                box([12.5, 6, 2.5], [13.5, 10, 11.5], "paint", faces=("east", "west", "up")),
                box([3.5, 6, 2.5], [12.5, 9, 11.5], "soil", faces=("up",)),
                box([3.5, 0, 10], [4.5, 5, 11], "steel", faces=("north", "south", "east", "west")),
                box([11.5, 0, 10], [12.5, 5, 11], "steel",
                    faces=("north", "south", "east", "west")),
            ] + wheel + handle)})

# --- crop rows: a ridge of tilled soil along the row, and the crop on it ---
row_soil = [box([0, 0, 2], [16, 2, 14], "soil", per={"up": "tilled"})]
lettuce = []
for x in (1, 6, 11):
    for z in (3, 9):
        lettuce += [box([x + 0.5, 2, z + 0.5], [x + 3.5, 4, z + 3.5], "leaf", faces=hoop[:5]),
                    box([x + 1, 4, z + 1], [x + 3, 5, z + 3], "leaf", faces=hoop[:5])]
facing_prop("crop_row_lettuce", [0, 0, 2, 16, 5, 14],
            ("Lettuce Row", "Salatreihe", "Hilera de lechugas", "Salladsrad"),
            {"crop_row_lettuce": model({"soil": T("soil"), "tilled": T("tilled_soil"),
                                        "leaf": T("lettuce"), "particle": T("lettuce")},
                                       row_soil + lettuce)}, crop=True)
tomato = []
for x in (3, 8, 13):
    tomato += [box([x - 0.5, 2, 9], [x + 0.5, 16, 10], "stake", faces=hoop[:5]),
               plane([x - 3.5, 2, 8], [x + 3.5, 16, 8], "plant", uv=(4.5, 2, 11.5, 16),
                     angle=45, origin=[x, 8, 8]),
               plane([x - 3.5, 2, 8], [x + 3.5, 16, 8], "plant", uv=(4.5, 2, 11.5, 16),
                     angle=-45, origin=[x, 8, 8])]
facing_prop("crop_row_tomato", [0, 0, 2, 16, 16, 14],
            ("Staked Tomato Row", "Tomatenreihe mit Stäben", "Hilera de tomates con tutores",
             "Tomatrad med stöd"),
            {"crop_row_tomato": model({"soil": T("soil"), "tilled": T("tilled_soil"),
                                       "stake": T("stake"), "plant": T("tomato_plant"),
                                       "particle": T("tomato_plant")},
                                      row_soil + tomato, ao=False)}, crop=True)

# --- a garden trellis with a clematis on it, between two posts ---
facing_prop("trellis_clematis", [0, 0, 7, 16, 16, 9],
            ("Garden Trellis with Clematis", "Rankgitter mit Clematis",
             "Enrejado de jardín con clemátide", "Spaljé med klematis"),
            {"trellis_clematis": model({"post": T("stake"), "lattice": T("trellis_clematis"),
                                        "particle": T("stake")}, [
                box([0, 0, 7], [1.5, 16, 9], "post"),
                box([14.5, 0, 7], [16, 16, 9], "post"),
                plane([1.5, 0, 8], [14.5, 16, 8], "lattice", uv=(1.5, 0, 14.5, 16)),
            ], ao=False)}, collides=False)

# --- more crop rows (GitHub #250): pole lima beans, pumpkin and watermelon patches, and
# boysenberries trained on wires ---
lima = []
for x in (3, 8, 13):
    lima += [box([x - 0.5, 2, 9], [x + 0.5, 16, 10], "stake", faces=hoop[:5]),
             plane([x - 3.5, 2, 8], [x + 3.5, 16, 8], "plant", uv=(4.5, 2, 11.5, 16),
                   angle=45, origin=[x, 8, 8]),
             plane([x - 3.5, 2, 8], [x + 3.5, 16, 8], "plant", uv=(4.5, 2, 11.5, 16),
                   angle=-45, origin=[x, 8, 8])]
facing_prop("crop_row_lima_bean", [0, 0, 2, 16, 16, 14],
            ("Pole Lima Bean Row", "Limabohnenreihe an Stangen",
             "Hilera de habas de lima con tutores", "Limabönrad med stänger"),
            {"crop_row_lima_bean": model({"soil": T("soil"), "tilled": T("tilled_soil"),
                                          "stake": T("stake"), "plant": T("lima_bean_plant"),
                                          "particle": T("lima_bean_plant")},
                                         row_soil + lima, ao=False)}, crop=True)


def patch_row(vine_rows, fruit, along=(5.5, 10.5), diag=(8,)):
    """A cucurbit patch on the ridge: vine cards along and across it, the fruit lying on it
    (a low melon with the diagonal cards only, so cards along the row do not hide it)."""
    lo = 16 - vine_rows
    els = [plane([0, 2, z], [16, 2 + vine_rows, z], "vine", uv=(0, lo, 16, 16)) for z in along]
    for cx in diag:
        for turn in (45, -45):
            els.append(plane([cx - 5, 2, 8], [cx + 5, 2 + vine_rows, 8], "vine",
                             uv=(3, lo, 13, 16), angle=turn, origin=[cx, 8, 8]))
    return row_soil + els + fruit


facing_prop("crop_row_pumpkin", [0, 0, 2, 16, 10, 14],
            ("Pumpkin Patch Row", "Kürbisreihe", "Hilera de calabazas", "Pumparad"),
            {"crop_row_pumpkin": model(
                {"soil": T("soil"), "tilled": T("tilled_soil"), "vine": T("pumpkin_vine"),
                 "fruit": T("pumpkin"), "stem": T("tie"), "particle": T("pumpkin")},
                patch_row(8, [box([2, 2, 3.5], [7.5, 6.5, 9], "fruit", faces=hoop[:5]),
                              box([4.25, 6.5, 5.75], [5.25, 7.5, 6.75], "stem", faces=hoop[:5]),
                              box([10, 2, 8], [14, 5, 12], "fruit", faces=hoop[:5]),
                              box([11.5, 5, 9.5], [12.5, 6, 10.5], "stem", faces=hoop[:5])]),
                ao=False)}, crop=True)
facing_prop("crop_row_watermelon", [0, 0, 2, 16, 8, 14],
            ("Watermelon Patch Row", "Wassermelonenreihe", "Hilera de sandías",
             "Vattenmelonrad"),
            {"crop_row_watermelon": model(
                {"soil": T("soil"), "tilled": T("tilled_soil"), "vine": T("watermelon_vine"),
                 "fruit": T("watermelon"), "particle": T("watermelon")},
                patch_row(6, [box([1.5, 2, 6], [8.5, 6, 10.5], "fruit", faces=hoop[:5]),
                              box([9.5, 2, 3.5], [15, 5.5, 7.5], "fruit", faces=hoop[:5])],
                          along=(), diag=(2.5, 8, 13.5)),
                ao=False)}, crop=True)
facing_prop("crop_row_boysenberry", [0, 0, 2, 16, 15, 14],
            ("Trained Boysenberry Row", "Boysenbeerenreihe am Spalier",
             "Hilera de moras boysen en espaldera", "Boysenbärsrad på spaljé"),
            {"crop_row_boysenberry": model(
                {"soil": T("soil"), "tilled": T("tilled_soil"), "post": T("stake"),
                 "canes": T("boysenberry_canes"), "particle": T("boysenberry_canes")},
                row_soil + [box([0.5, 2, 7.25], [2, 15, 8.75], "post", faces=hoop[:5]),
                            box([14, 2, 7.25], [15.5, 15, 8.75], "post", faces=hoop[:5]),
                            plane([0, 2, 7.5], [16, 15, 7.5], "canes", uv=(0, 3, 16, 16)),
                            plane([0, 2, 8.5], [16, 15, 8.5], "canes", uv=(0, 3, 16, 16))],
                ao=False)}, crop=True)


# ------------------------------------------------------------------------------------------
# Output
# ------------------------------------------------------------------------------------------
def dump(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


def lang_entries():
    out = {loc: {} for loc in LOCALES}
    for b in BLOCKS:
        for i, loc in enumerate(LOCALES):
            out[loc]["tile.%s.name" % b["registry"]] = b["names"][i]
    return out


def generate(assets):
    written = []
    for name, draw in TEXTURES.items():
        rel = "textures/blocks/parks/landscape/%s.png" % name
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
    for b in BLOCKS:
        for name, data in b["models"].items():
            rel = "models/block/parks/landscape/%s.json" % name
            dump(os.path.join(assets, rel), data)
            written.append(rel)
        rel = "blockstates/%s.json" % b["registry"]
        dump(os.path.join(assets, rel), b["blockstate"])
        written.append(rel)
        if b["item"]:
            rel = "models/item/%s.json" % b["registry"]
            dump(os.path.join(assets, rel), b["item"])
            written.append(rel)
    gen_trees.write_lang(os.path.join(assets, "lang"), lang_entries())
    written += ["lang/%s.lang" % loc for loc in LOCALES]
    model_depth.separate(assets, written, dump)
    return written


def fragments():
    return "\n".join("    initTabBlock(%s);" % b["java"] for b in BLOCKS)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    args = ap.parse_args()
    if args.fragments:
        print(fragments())
        return 0
    if not args.check:
        written = generate(ASSETS)
        print("wrote %d files under %s" % (len(written), ASSETS))
        return 0
    tmp = tempfile.mkdtemp(prefix="plantings_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not gen_trees.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("park plantings are up to date")
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
