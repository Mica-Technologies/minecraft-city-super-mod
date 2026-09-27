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
]
REGIONS = [0, 5, 10, 16, 22, 27, 32]  # where each region starts in REGIONAL


def potted_names(names):
    return ("Potted " + names[0], names[1] + " im Topf", names[2] + " en maceta",
            names[3] + " i kruka")


for start, end in zip(REGIONS, REGIONS[1:]):
    for reg, kind, height, inset, names, (tex, els), item in REGIONAL[start:end]:
        ao = kind == "SHRUB"
        models = {reg: model(tex, els, ao=ao)}
        state = simple_state(reg)
        if item:
            models[reg + "_item"] = {"parent": "item/generated", "textures": {"layer0": T(item)}}
            state = simple_state(reg, item=MODEL + reg + "_item")
        prop(reg, kind, height, inset, names, models, state)
    # The same plants in nursery pots, after the region's plants.
    for reg, kind, height, inset, names, (tex, els), item in REGIONAL[start:end]:
        preg = "potted_" + reg
        ptex, pels = potted(tex, els)
        top = int(math.ceil(POT_LIFT + height * POT_SCALE))
        prop(preg, "SHRUB", min(16, top), 3, potted_names(names),
             {preg: model(ptex, pels, ao=False)}, simple_state(preg))


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
