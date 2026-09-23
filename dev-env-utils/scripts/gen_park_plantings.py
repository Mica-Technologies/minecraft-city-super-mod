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
def joining(registry, kind, height, width, names, post, side, item_extra=(), side_when="true"):
    """post: elements always drawn; side: elements drawn toward north, turned for the others.

    A hedge or fence draws a side where it joins (side_when "true"); a bed draws its wall where
    it does not ("false"), so a run of beds is one bed walled only round the outside."""
    models = {registry + "_post": post, registry + "_side": side}
    parts = [{"apply": {"model": MODEL + registry + "_post"}}]
    for direction, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        apply = {"model": MODEL + registry + "_side"}
        if rot:
            apply["y"] = rot
            apply["uvlock"] = False
        parts.append({"when": {direction: side_when}, "apply": apply})
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
    post = model(tex, [box([0, 0, 0], [16, 10, 16], "soil", per={"up": "soil"})])
    side = model(tex, [box([0, 0, 0], [16, 12, 2], "wall")])
    # The item is a single bed: walls on all four sides.
    walls = [box([0, 0, 0], [16, 12, 2], "wall"), box([0, 0, 14], [16, 12, 16], "wall"),
             box([0, 0, 2], [2, 12, 14], "wall"), box([14, 0, 2], [16, 12, 14], "wall")]
    joining(reg, "BED", 12, 16,
            ("Raised %s Planting Bed" % names_m[0], "Hochbeet (%s)" % names_m[1],
             "Bancal elevado de %s" % names_m[2], "Upphöjd odlingsbädd av %s" % names_m[3]),
            post, side, item_extra=walls, side_when="false")
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
add("tree_stake", 'new BlockParkFacing("tree_stake", new int[]{7, 0, 11, 9, 24, 13}, true)',
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
