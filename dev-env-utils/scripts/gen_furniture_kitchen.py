#!/usr/bin/env python3
"""
gen_furniture_kitchen.py -- the Residential tab's kitchen cabinetry and cold storage in the
Furniture & Novelties module: base cabinets (two doors, a drawer bank, door and drawer), the
corner base that turns a run, the sink base, the island, wall cabinets and the open wall shelf,
each in the three wood finishes of gen_furniture_residential.py with a countertop to suit it;
and in stainless steel or white, the chimney and under-cabinet range hoods, the under-cabinet
light, the two-block refrigerator and the chest freezer.

Borrows gen_furniture_residential.py's element helpers, finishes and output (imported, not
copied), and writes, under modules/furnishings/src/main/resources/assets/csm:

  * textures/blocks/furniture/residential/*.png  countertop stone, stainless, appliance white,
    lamp lenses, the hood's filter
  * models/block/furniture/residential/base/kitchen_*.json (and the appliances') the geometry
  * models/block/furniture/residential/<registry>_<part>.json a finish's copy of each part a
    multipart or plain-variant blockstate picks
  * blockstates/<registry>.json, and models/item/<registry>.json where the state has no
    inventory variant
  * the tile names in all four languages, by key

Every model faces north with its back at +Z, against the wall, as in the living room set.
Real-world scale, 1 block = 1 m: a countertop at 0.91 m, wall cabinets filling the block two
above the counter's (a Minecraft kitchen keeps the block between free to work in, and the
under-cabinet light and the under-cabinet hood hang at the top of that block).

What joins, and how (the Java classes compute these as actual state, nothing is stored):

  * base cabinets of any kind -- two doors, drawers, door and drawer, sink -- join left and
    right when they share a finish and a facing: the countertop runs on through, and the end
    panels and the countertop's cut ends show only where the run stops;
  * the corner base turns a run: it is open on its left and at its front like the sofa corner,
    and a run meeting either side continues. With both sides joined it is a blind corner (the
    countertop and a notch of face frame, as a real one is); with one side joined it is the end
    of that run and closes itself with an end panel; alone it is a plain two-door base;
  * wall cabinets and open wall shelves join each other the same way; islands join islands.

The refrigerator is two blocks tall. It is drawn whole, up to 1.81 m, and cut at the block
line into a lower and an upper model, so each half is lit and culled in its own block.

Usage:
    python gen_furniture_kitchen.py              # write everything
    python gen_furniture_kitchen.py --check      # fail if the tree has drifted
    python gen_furniture_kitchen.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import argparse
import copy
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_furniture_residential as R  # noqa: E402

ASSETS = R.ASSETS
SUB = R.SUB
T = R.T
MODEL = R.MODEL
BASE = R.BASE
ROT = R.ROT
el, board, leg = R.el, R.board, R.leg
mirror_x, swap_xz, turn = R.mirror_x, R.swap_xz, R.turn
ALL = R.ALL


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def stone(base, specks, seed, density, size=32):
    """A polished engineered stone: a quiet mottle, and flecks of a few colours, some of them
    two texels, scattered at the given density."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = 1.0 + rng.uniform(-0.025, 0.025)
            px[x, y] = R.shade(base, k) + (255,)
    for y in range(size):
        for x in range(size):
            if rng.random() < density:
                c = R.shade(rng.choice(specks), 1.0 + rng.uniform(-0.05, 0.05)) + (255,)
                px[x, y] = c
                if rng.random() < 0.3:
                    px[(x + 1) % size, y] = c
    return img


def brushed(base, seed, vertical=False, size=32):
    """Brushed steel: fine streaks along the grain, each a little lighter or darker."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    streaks = [rng.uniform(-0.045, 0.045) for _ in range(size)]
    for y in range(size):
        for x in range(size):
            r = x if vertical else y
            px[x, y] = R.shade(base, 1.0 + streaks[r] + rng.uniform(-0.012, 0.012)) + (255,)
    return img


def filter_mesh(seed, size=16):
    """A hood's baffle filter from below: bright steel slats between dark slots."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            if x in (0, size - 1) or y in (0, size - 1):
                c = (150, 154, 158)
            elif y % 3 == 0:
                c = (58, 60, 62)
            else:
                c = R.shade((176, 180, 184), 1.0 + rng.uniform(-0.03, 0.03) - 0.04 * (y % 3))
            px[x, y] = tuple(c) + (255,)
    return img


def drain(size=16):
    """A sink's strainer: a steel ring round a dark cross of slots."""
    img = Image.new("RGBA", (size, size))
    px = img.load()
    c = (size - 1) / 2.0
    for y in range(size):
        for x in range(size):
            d = ((x - c) ** 2 + (y - c) ** 2) ** 0.5
            if d > 7.2:
                col = (160, 164, 168)
            elif d > 5.6:
                col = (196, 200, 204)
            elif abs(x - c) < 1 or abs(y - c) < 1:
                col = (40, 40, 42)
            else:
                col = (120, 124, 128)
            px[x, y] = col + (255,)
    return img


def control_strip(size=16):
    """Black glass with a row of small lit buttons: the hood's controls, the freezer's lamp."""
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            col = (26, 28, 30)
            if 6 <= y <= 9 and x % 4 == 1:
                col = (120, 200, 255) if x < 12 else (110, 220, 120)
            px[x, y] = col + (255,)
    return img


GRANITE = (60, 60, 64)
QUARTZ = (228, 226, 220)
STAINLESS = (186, 190, 194)

TEXTURES = {
    "counter_granite": lambda: stone(GRANITE, [(36, 36, 38), (92, 90, 88), (116, 112, 106),
                                               (84, 72, 64)], 401, 0.10),
    "counter_quartz": lambda: stone(QUARTZ, [(196, 194, 188), (174, 172, 168), (208, 202, 192),
                                             (150, 148, 146)], 402, 0.07),
    "stainless": lambda: brushed(STAINLESS, 403),
    "stainless_v": lambda: brushed(STAINLESS, 404, vertical=True),
    "stainless_dark": lambda: brushed((150, 154, 158), 405),
    "chrome": lambda: brushed((218, 222, 226), 406),
    "appliance_white": lambda: R.flat((238, 239, 238), 407, grain=2),
    "appliance_trim": lambda: R.flat((188, 190, 192), 408, grain=3),
    "lens_on": lambda: R.flat((255, 247, 218), 409, grain=3),
    "lens_off": lambda: R.flat((170, 170, 166), 410, grain=3),
    "hood_filter": lambda: filter_mesh(411),
    "sink_drain": drain,
    "control_strip": control_strip,
    "rubber_black": lambda: R.flat((34, 34, 36), 412, grain=3),
}

# The wood finishes, each with its countertop.
COUNTERS = {"oak": T("counter_granite"), "walnut": T("counter_quartz"),
            "white": T("counter_quartz")}
WOODS = [(fid, dict(ftex, counter=COUNTERS[fid]), *names) for fid, ftex, *names in R.WOODS]

STAINLESS_FINISH = ("stainless", {"shell": T("stainless"), "door": T("stainless_v"),
                                  "handle": T("chrome"), "trim": T("stainless_dark")},
                    "Stainless Steel", "Edelstahl", "acero inoxidable", "rostfritt stål")
WHITE_FINISH = ("white", {"shell": T("appliance_white"), "door": T("appliance_white"),
                          "handle": T("appliance_trim"), "trim": T("appliance_trim")},
                "White", "Weiß", "blanco", "vit")

DEFAULT_TEX = dict(R.DEFAULT_TEX)
DEFAULT_TEX.update({
    "counter": T("counter_granite"), "handle": T("metal_steel"), "basin": T("stainless_dark"),
    "drain": T("sink_drain"), "chrome": T("chrome"), "shell": T("stainless"),
    "door": T("stainless_v"), "trim": T("stainless_dark"), "lens": T("lens_on"),
    "filter": T("hood_filter"), "control": T("control_strip"), "rubber": T("rubber_black"),
})


def geometry(specs, particle, display=None):
    tex = {k: DEFAULT_TEX[k] for k in sorted(R.used_keys(specs) | {particle})}
    tex["particle"] = "#" + particle
    out = {"parent": "block/block", "textures": tex, "elements": [R.build(s) for s in specs]}
    if display:
        out["display"] = display
    return out


# ------------------------------------------------------------------------------------------
# Fronts: a Shaker door or drawer is a flat centre panel in a frame of stiles and rails a
# quarter pixel proud of it
# ------------------------------------------------------------------------------------------
def shaker(x0, x1, y0, y1, zf, grain="v", frame=1.25):
    """A Shaker front from x0 to x1 and y0 to y1, its frame's face at zf (the panel a quarter
    pixel behind it), backed onto the carcass half a pixel behind zf."""
    f = frame
    out = [board([x0, y0, zf + 0.25], [x1, y1, zf + 0.5], grain=grain,
                 faces=("north", "east", "west", "up", "down"))]
    fr = ("north", "east", "west", "up", "down")
    out += [board([x0, y0, zf], [x0 + f, y1, zf + 0.25], grain="v", faces=fr),
            board([x1 - f, y0, zf], [x1, y1, zf + 0.25], grain="v", faces=fr),
            board([x0 + f, y0, zf], [x1 - f, y0 + f, zf + 0.25], faces=fr),
            board([x0 + f, y1 - f, zf], [x1 - f, y1, zf + 0.25], faces=fr)]
    return out


def bar_v(x, y0, y1, zf):
    """A vertical bar pull standing on a front whose face is at zf."""
    return [el([x, y0, zf - 0.5], [x + 0.4, y1, zf], "handle",
               ("north", "east", "west", "up", "down"))]


def bar_h(cx, y, zf, w=4.0):
    """A horizontal bar pull, w wide, centred on cx."""
    return [el([cx - w / 2, y, zf - 0.5], [cx + w / 2, y + 0.4, zf], "handle",
               ("north", "east", "west", "up", "down"))]


# ------------------------------------------------------------------------------------------
# Base cabinets: toe kick 0.09 m, carcass to 0.86 m, a 4.7 cm countertop to 0.91 m that
# overhangs the doors by 5 cm. Doors and drawers are 1.25..1.75 deep, the carcass face at 1.75.
# ------------------------------------------------------------------------------------------
KICK = [el([0, 0, 2.5], [16, 1.5, 16], "edge", ("north",))]
CARCASS = [board([0, 1.5, 1.75], [16, 13.75, 16], grain="v", faces=("north", "south"))]
COUNTER = [el([0, 13.75, 0.5], [16, 14.5, 16], "counter", ("north", "south", "up", "down"))]
FRAME = KICK + CARCASS + COUNTER
ZF = 1.25  # the face of a base cabinet's doors

TWO_DOORS = (shaker(0.5, 7.9, 2, 13.25, ZF) + shaker(8.1, 15.5, 2, 13.25, ZF)
             + bar_v(6.7, 9.5, 12.5, ZF) + bar_v(8.9, 9.5, 12.5, ZF))
DRAWERS = []
for _y0, _y1 in ((2, 5.75), (6, 9.75), (10, 13.25)):
    DRAWERS += shaker(0.5, 15.5, _y0, _y1, ZF, grain="h", frame=1.0)
    DRAWERS += bar_h(8, (_y0 + _y1) / 2 - 0.2, ZF)
DOOR_DRAWER = (shaker(0.5, 15.5, 10, 13.25, ZF, grain="h", frame=1.0)
               + bar_h(8, 11.45, ZF)
               + shaker(0.5, 15.5, 2, 9.75, ZF)
               + bar_v(13.4, 6, 9, ZF))

BASE_BODY = FRAME + TWO_DOORS
DRAWER_BODY = FRAME + DRAWERS
DOOR_DRAWER_BODY = FRAME + DOOR_DRAWER

# Where a run stops: an end panel flush with the doors, and the countertop's cut end.
BASE_END = [board([0, 0, 1.25], [0.5, 13.75, 16], grain="v", faces=("west", "north")),
            el([0, 13.75, 0.5], [0.01, 14.5, 16], "counter", ("west",))]

# --- the sink base: a false drawer front over two doors, and an undermount stainless bowl
# 0.62 x 0.5 m and 0.2 m deep in a countertop cut round it, with a gooseneck tap behind -------
SINK_COUNTER = [
    el([0, 13.75, 0.5], [16, 14.5, 3], "counter", ("north", "south", "up", "down")),
    el([0, 13.75, 11], [16, 14.5, 16], "counter", ("north", "south", "up")),
    el([0, 13.75, 3], [3, 14.5, 11], "counter", ("east", "up")),
    el([13, 13.75, 3], [16, 14.5, 11], "counter", ("west", "up")),
]
BASIN = [el([3, 10.5, 2.75], [13, 13.75, 3], "basin", ("south",)),
         el([3, 10.5, 11], [13, 13.75, 11.25], "basin", ("north",)),
         el([2.75, 10.5, 3], [3, 13.75, 11], "basin", ("east",)),
         el([13, 10.5, 3], [13.25, 13.75, 11], "basin", ("west",)),
         el([3, 10.25, 3], [13, 10.5, 11], "basin", ("up",)),
         el([7.25, 10.5, 6.25], [8.75, 10.51, 7.75], "drain", ("up",))]
TAP = [el([7.25, 14.5, 12.25], [8.75, 15, 13.75], "chrome", ("north", "south", "east", "west",
                                                            "up")),
       el([7.6, 15, 12.6], [8.4, 19.5, 13.4], "chrome", ("north", "south", "east", "west")),
       el([7.6, 19.5, 8.75], [8.4, 20.25, 13.4], "chrome"),
       el([7.6, 18.25, 8.75], [8.4, 19.5, 9.55], "chrome", ("north", "south", "east", "west",
                                                           "down")),
       el([8.4, 17, 12.75], [9.9, 17.5, 13.25], "chrome", ("north", "south", "east", "up",
                                                          "down"))]
SINK_BODY = (KICK + CARCASS + SINK_COUNTER + BASIN + TAP
             + shaker(0.5, 15.5, 10.5, 13.25, ZF, grain="h", frame=1.0)
             + shaker(0.5, 7.9, 2, 10.25, ZF) + shaker(8.1, 15.5, 2, 10.25, ZF)
             + bar_v(6.7, 6.75, 9.5, ZF) + bar_v(8.9, 6.75, 9.5, ZF))

# --- the corner base: its backs along +Z and +X, open to its left (-X) and front (-Z) -----
# Both sides joined: a blind corner, the countertop whole but for the notch where the two
# runs' fronts meet, the carcass an L behind them.
CORNER_BLIND = [
    el([0, 13.75, 0.5], [16, 14.5, 16], "counter", ("north", "up", "down")),
    el([0.5, 13.75, 0], [16, 14.5, 0.5], "counter", ("west", "up", "down")),
    board([0, 1.5, 1.75], [16, 13.75, 16], grain="v", faces=("north",)),
    board([1.75, 1.5, 0], [16, 13.75, 1.75], grain="v", faces=("west",)),
    el([0, 0, 2.5], [16, 1.5, 16], "edge", ("north",)),
    el([2.5, 0, 0], [16, 1.5, 2.5], "edge", ("west",)),
]
CORNER_NORTH = BASE_BODY           # front open: the end of the left-hand run, doors north
CORNER_WEST = swap_xz(BASE_BODY)   # left open: the end of the front run, doors west
CORNER_END_RIGHT = mirror_x(BASE_END)
CORNER_END_LEFT = BASE_END
CORNER_END_BACK = swap_xz(CORNER_END_RIGHT)

# --- the island: doors to the kitchen, a finished back, and the countertop running 0.25 m
# past it for stools ------------------------------------------------------------------------
ISLAND_BODY = (KICK
               + [board([0, 1.5, 1.75], [16, 13.75, 11.5], grain="v", faces=("north",)),
                  board([0, 0, 11.5], [16, 13.75, 12], grain="v", faces=("south",)),
                  el([0, 13.75, 0.5], [16, 14.5, 16], "counter",
                     ("north", "south", "up", "down"))]
               + TWO_DOORS)
ISLAND_END = [board([0, 0, 1.25], [0.5, 13.75, 12.25], grain="v",
                    faces=("west", "north", "south")),
              el([0, 13.75, 0.5], [0.01, 14.5, 16], "counter", ("west",))]

# --- wall cabinets: 0.44 m deep, the height of the block, doors 0.5 px proud ---------------
WZ = 8.5
WALL_BODY = ([board([0, 0, 9], [16, 16, 16], grain="v", faces=("north", "south", "up",
                                                                 "down"))]
             + shaker(0.5, 7.9, 0.5, 15.5, WZ) + shaker(8.1, 15.5, 0.5, 15.5, WZ)
             + bar_v(6.7, 1.5, 4.5, WZ) + bar_v(8.9, 1.5, 4.5, WZ))
WALL_END = [board([0, 0, 8.5], [0.5, 16, 9], grain="v", faces=("west", "north", "up", "down")),
            board([0, 0, 9], [0.5, 16, 16], grain="v", faces=("west",))]

SHELF_BODY = [board([0, 0, 15.25], [16, 16, 16], grain="v",
                    faces=("north", "south", "up", "down")),
              board([0, 0, 9], [16, 0.75, 15.25], faces=("north", "up", "down")),
              board([0, 15.25, 9], [16, 16, 15.25], faces=("north", "up", "down")),
              board([0, 7.6, 9.5], [16, 8.35, 15.25], faces=("north", "up", "down"))]
SHELF_END = [board([0, 0, 9], [0.75, 16, 16], grain="v", faces=("west", "east", "north"))]


# ------------------------------------------------------------------------------------------
# Lights and hoods (stainless). The lens texture is swapped for the lit one by the blockstate.
# ------------------------------------------------------------------------------------------
# The under-cabinet light: an aluminium strip at the top of the block below a wall cabinet,
# just behind the cabinet's front, its lens facing down.
UNDER_LIGHT = [el([1, 15.25, 9.5], [15, 16, 11], "shell", ("north", "south", "east", "west")),
               el([1, 15.25, 9.5], [1.5, 15.3, 11], "shell", ("down",)),
               el([14.5, 15.25, 9.5], [15, 15.3, 11], "shell", ("down",)),
               el([1.5, 15.25, 9.5], [14.5, 15.3, 9.75], "shell", ("down",)),
               el([1.5, 15.25, 10.75], [14.5, 15.3, 11], "shell", ("down",)),
               el([1.5, 15.2, 9.75], [14.5, 15.3, 10.75], "lens", ("down",))]


def hood_lamps(y):
    """Two lamps and a baffle filter on a hood's underside at y."""
    return [el([2.5, y - 0.1, 8.5], [4.5, y, 10.5], "lens", ("down",)),
            el([11.5, y - 0.1, 8.5], [13.5, y, 10.5], "lens", ("down",)),
            el([5, y - 0.05, 8], [11, y, 14.5], "filter", ("down",))]


# Under-cabinet hood: a slim box at the top of the block under a wall cabinet, controls on
# its front lip.
UNDER_HOOD = ([el([0.5, 13, 7.5], [15.5, 16, 16], "shell", ("north", "south", "east", "west",
                                                             "down")),
               el([11, 13.6, 7.4], [14.5, 14.4, 7.5], "control", ("north",))]
              + hood_lamps(13))
# Chimney hood, for the wall cabinets' row: a canopy that dips 0.19 m below the block to sit
# 0.9 m over the cooktop, stepping in to a duct cover that runs to the top of the block.
CHIMNEY_HOOD = ([el([0.5, -3, 6], [15.5, -1, 16], "shell", ("north", "south", "east", "west",
                                                             "down")),
                 el([11, -2.4, 5.9], [14.5, -1.6, 6], "control", ("north",)),
                 el([1.25, -1, 7], [14.75, 0.5, 16], "shell", ("north", "east", "west", "up")),
                 el([2.75, 0.5, 8.5], [13.25, 2, 16], "shell", ("north", "east", "west", "up")),
                 el([4.25, 2, 10], [11.75, 3.5, 16], "shell", ("north", "east", "west", "up")),
                 el([5, 3.5, 10.5], [11, 16, 16], "shell", ("north", "east", "west", "up"))]
                + hood_lamps(-3))

# ------------------------------------------------------------------------------------------
# Refrigerator: 0.94 m wide, 1.81 m tall, French doors over a freezer drawer, drawn whole and
# cut into two blocks
# ------------------------------------------------------------------------------------------
FRIDGE = [
    el([0.5, 0.5, 2], [15.5, 29, 16], "shell", ("south", "east", "west", "up")),
    el([1, 0, 2.5], [15, 0.5, 15], "rubber", ("north", "east", "west")),
    # the freezer drawer and the two doors, a hair apart
    el([0.5, 0.5, 1], [15.5, 10.5, 2], "door", ("north", "east", "west", "up", "down")),
    el([0.5, 10.75, 1], [7.95, 29, 2], "door", ("north", "east", "west", "up", "down")),
    el([8.05, 10.75, 1], [15.5, 29, 2], "door", ("north", "east", "west", "up", "down")),
    # the dark gaps between them
    el([0.5, 10.5, 1.5], [15.5, 10.75, 2], "rubber", ("north",)),
    el([7.95, 10.75, 1.5], [8.05, 29, 2], "rubber", ("north",)),
    # the water and ice dispenser in the left-hand door
    el([2.25, 16.5, 0.9], [5.75, 22.5, 1], "rubber", ("north", "east", "west", "up", "down")),
    el([2.75, 21, 0.85], [5.25, 22, 0.9], "control", ("north",)),
]
FRIDGE += [el([6.6, 13, 0.25], [7.1, 26, 0.75], "handle"),
           el([6.6, 13.5, 0.75], [7.1, 14.25, 1], "handle", ("east", "west", "up", "down")),
           el([6.6, 24.75, 0.75], [7.1, 25.5, 1], "handle", ("east", "west", "up", "down")),
           el([8.9, 13, 0.25], [9.4, 26, 0.75], "handle"),
           el([8.9, 13.5, 0.75], [9.4, 14.25, 1], "handle", ("east", "west", "up", "down")),
           el([8.9, 24.75, 0.75], [9.4, 25.5, 1], "handle", ("east", "west", "up", "down")),
           el([3, 8.25, 0.25], [13, 8.75, 0.75], "handle"),
           el([3.5, 8.25, 0.75], [4.25, 8.75, 1], "handle", ("east", "west", "up", "down")),
           el([11.75, 8.25, 0.75], [12.5, 8.75, 1], "handle", ("east", "west", "up", "down"))]


def split_y(specs, cut=16):
    """The part of each element below cut, and the part above it moved down a block. The
    faces on the cut plane are dropped: they are inside the appliance."""
    lower, upper = [], []
    for s in specs:
        if s["rot"]:
            raise ValueError("cannot split a turned element")
        y0, y1 = s["from"][1], s["to"][1]
        if y1 <= cut:
            lower.append(s)
            continue
        if y0 >= cut:
            u = copy.deepcopy(s)
            u["from"][1] -= cut
            u["to"][1] -= cut
            upper.append(u)
            continue
        a = copy.deepcopy(s)
        a["to"][1] = cut
        a["faces"] = tuple(f for f in s["faces"] if f != "up")
        a["uv"] = {}
        b = copy.deepcopy(s)
        b["from"][1] = 0
        b["to"][1] = y1 - cut
        b["faces"] = tuple(f for f in s["faces"] if f != "down")
        b["uv"] = {}
        lower.append(a)
        upper.append(b)
    return lower, upper


FRIDGE_LOWER, FRIDGE_UPPER = split_y(FRIDGE)
# An item two blocks tall, drawn at a little over half a block's size.
TALL_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -3, 0], "scale": [0.36, 0.36, 0.36]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.18, 0.18, 0.18]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, -3, 0], "scale": [0.36, 0.36, 0.36]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, 0],
                              "scale": [0.25, 0.25, 0.25]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                              "scale": [0.28, 0.28, 0.28]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                             "scale": [0.28, 0.28, 0.28]},
}

# --- chest freezer: 0.94 x 0.7 m and 0.9 m high to the top of its lid -----------------------
FREEZER = [
    el([0.5, 0.75, 3], [15.5, 13, 14], "shell", ("north", "south", "east", "west")),
    el([1, 0, 3.5], [15, 0.75, 13.5], "rubber", ("north", "south", "east", "west")),
    el([0.5, 13, 3.25], [15.5, 13.25, 13.75], "rubber", ("north", "south", "east", "west")),
    el([0.25, 13.25, 2.75], [15.75, 14.5, 14.25], "shell"),
    el([6, 12.5, 2.25], [10, 13.25, 2.75], "trim"),
    el([12.5, 10.5, 2.9], [14.5, 11.5, 3], "control", ("north",)),
]

# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
# Cabinet runs: (piece, en, de, es, sv, java class line, parts, item, rules)
def run_rules(end_parts=("left", "right")):
    return R.faced([("body", {}), (end_parts[0], {"left": "false"}),
                    (end_parts[1], {"right": "false"})])


def corner_rules():
    return R.faced([("north", {"front": "false"}),
                    ("west", {"left": "false", "front": "true"}),
                    ("blind", {"left": "true", "front": "true"}),
                    ("end_right", {"front": "false"}),
                    ("end_left", {"left": "false", "front": "false"}),
                    ("end_back", {"left": "false", "front": "true"})])


def run_piece(body, end, java):
    return {"parts": {"body": body, "left": end, "right": mirror_x(end)},
            "item": body + end + mirror_x(end), "rules": run_rules(), "java": java}


CABINET = 'new BlockKitchenCabinet("%%s", new int[]{%s}, KitchenLine.%s, %d, KitchenFront.%s)'
BOX_BASE = "0, 0, 0, 16, 15, 16"
BOX_WALL = "0, 0, 8, 16, 16, 16"

RUNS = {
    "kitchen_base_cabinet": run_piece(BASE_BODY, BASE_END,
                                      CABINET % (BOX_BASE, "BASE", 18, "DOORS")),
    "kitchen_drawer_cabinet": run_piece(DRAWER_BODY, BASE_END,
                                        CABINET % (BOX_BASE, "BASE", 18, "DRAWERS")),
    "kitchen_door_drawer_cabinet": run_piece(DOOR_DRAWER_BODY, BASE_END,
                                             CABINET % (BOX_BASE, "BASE", 18, "DOORS")),
    "kitchen_sink_cabinet": run_piece(SINK_BODY, BASE_END, 'new BlockKitchenSink("%s")'),
    "kitchen_corner_cabinet": {
        "parts": {"north": CORNER_NORTH, "west": CORNER_WEST, "blind": CORNER_BLIND,
                  "end_right": CORNER_END_RIGHT, "end_left": CORNER_END_LEFT,
                  "end_back": CORNER_END_BACK},
        "item": CORNER_NORTH + CORNER_END_RIGHT + CORNER_END_LEFT,
        "rules": corner_rules(), "java": 'new BlockKitchenCorner("%s")'},
    "kitchen_island": run_piece(ISLAND_BODY, ISLAND_END,
                                CABINET % (BOX_BASE, "ISLAND", 18, "DOORS")),
    "kitchen_wall_cabinet": run_piece(WALL_BODY, WALL_END,
                                      CABINET % (BOX_WALL, "WALL", 9, "DOORS")),
    "kitchen_wall_shelf": run_piece(SHELF_BODY, SHELF_END,
                                    CABINET % (BOX_WALL, "WALL", 0, "OPEN")),
}

RUN_NAMES = [
    # (piece, en, de, es, sv)
    ("kitchen_base_cabinet", "Kitchen Base Cabinet", "Küchen-Unterschrank",
     "Mueble bajo de cocina", "Bänkskåp"),
    ("kitchen_drawer_cabinet", "Kitchen Drawer Cabinet", "Küchen-Schubladenschrank",
     "Cajonera de cocina", "Lådskåp"),
    ("kitchen_door_drawer_cabinet", "Kitchen Door and Drawer Cabinet",
     "Küchen-Unterschrank mit Schublade", "Mueble bajo con cajón", "Bänkskåp med låda"),
    ("kitchen_corner_cabinet", "Kitchen Corner Cabinet", "Küchen-Eckschrank",
     "Mueble esquinero de cocina", "Hörnbänkskåp"),
    ("kitchen_sink_cabinet", "Kitchen Sink Cabinet", "Spülenschrank",
     "Mueble de fregadero", "Diskbänksskåp"),
    ("kitchen_island", "Kitchen Island", "Kücheninsel", "Isla de cocina", "Köksö"),
    ("kitchen_wall_cabinet", "Kitchen Wall Cabinet", "Küchen-Hängeschrank",
     "Mueble alto de cocina", "Väggskåp"),
    ("kitchen_wall_shelf", "Open Kitchen Shelf", "Offenes Küchenregal",
     "Estante de cocina abierto", "Öppen kökshylla"),
]

# Single appliances: (registry, geometry, particle, finish textures, names, java, lit texture)
LIGHTS = [
    ("range_hood", CHIMNEY_HOOD, STAINLESS_FINISH,
     ("Range Hood", "Dunstabzugshaube", "Campana extractora", "Köksfläkt"),
     'new BlockKitchenLight("%s", new int[]{0, 0, 6, 16, 16, 16}, 13)'),
    ("range_hood_under_cabinet", UNDER_HOOD, STAINLESS_FINISH,
     ("Under-Cabinet Range Hood", "Unterbau-Dunstabzugshaube", "Campana bajo mueble",
      "Köksfläkt för överskåp"),
     'new BlockKitchenLight("%s", new int[]{0, 13, 7, 16, 16, 16}, 13)'),
    ("under_cabinet_light", UNDER_LIGHT, STAINLESS_FINISH,
     ("Under-Cabinet Light", "Unterbauleuchte", "Luz bajo mueble", "Bänkbelysning"),
     'new BlockKitchenLight("%s", new int[]{1, 15, 9, 15, 16, 11}, 10)'),
]
FRIDGES = [STAINLESS_FINISH, WHITE_FINISH]
FRIDGE_NAMES = ("Refrigerator", "Kühlschrank", "Frigorífico", "Kylskåp")
FREEZER_NAMES = ("Chest Freezer", "Gefriertruhe", "Arcón congelador", "Frysbox")


def names_with(names, fnames):
    return ["%s (%s)" % (n, f) for n, f in zip(names, fnames)]


def entries():
    """Every block: (registry, kind, piece, finish textures, names, java)."""
    out = []
    for piece, *names in RUN_NAMES:
        for fid, ftex, *fnames in WOODS:
            reg = "%s_%s" % (piece, fid)
            out.append((reg, "run", piece, ftex, names_with(names, fnames),
                        RUNS[piece]["java"] % reg))
    for piece, _geo, (fid, ftex, *fnames), names, java in LIGHTS:
        reg = "%s_%s" % (piece, fid)
        out.append((reg, "light", piece, ftex, names_with(names, fnames), java % reg))
    for fid, ftex, *fnames in FRIDGES:
        reg = "refrigerator_%s" % fid
        out.append((reg, "fridge", "refrigerator", ftex, names_with(FRIDGE_NAMES, fnames),
                    'new BlockRefrigerator("%s")' % reg))
    fid, ftex, *fnames = WHITE_FINISH
    reg = "chest_freezer_%s" % fid
    out.append((reg, "freezer", "chest_freezer", ftex, names_with(FREEZER_NAMES, fnames),
                'new BlockChestFreezer("%s", new int[]{0, 0, 2, 16, 15, 15})' % reg))
    return out


# ------------------------------------------------------------------------------------------
# Output
# ------------------------------------------------------------------------------------------
def multipart_state(reg, rules):
    parts = []
    for part, when, r in rules:
        apply = {"model": MODEL + "%s_%s" % (reg, part)}
        if r:
            apply["y"] = r
        parts.append({"when": when, "apply": apply} if when else {"apply": apply})
    return {"multipart": parts}


def light_state(piece, ftex):
    return {"forge_marker": 1,
            "defaults": {"model": BASE + piece, "textures": dict(ftex)},
            "variants": {"facing": {f: ({"y": r} if r else {}) for f, r in ROT.items()},
                         "lit": {"true": {"textures": {"lens": T("lens_on")}},
                                 "false": {"textures": {"lens": T("lens_off")}}},
                         # Whether redstone last powered it: stored, drawn the same either way.
                         "powered": {"true": {}, "false": {}},
                         "inventory": [{}]}}


def fridge_state(reg):
    variants = {}
    for f, r in ROT.items():
        for upper in ("false", "true"):
            v = {"model": MODEL + "%s_%s" % (reg, "upper" if upper == "true" else "lower")}
            if r:
                v["y"] = r
            variants["facing=%s,upper=%s" % (f, upper)] = v
    return {"variants": variants}


def lang_entries():
    out = {loc: {} for loc in R.LOCALES}
    for reg, _k, _p, _t, names, _j in entries():
        for i, loc in enumerate(R.LOCALES):
            out[loc]["tile.%s.name" % reg] = names[i]
    return out


def base_models():
    """Every base geometry model: (name, geometry json)."""
    out = []
    for piece, spec in RUNS.items():
        for part, geo in spec["parts"].items():
            out.append(("%s_%s" % (piece, part), geometry(geo, "wood")))
        out.append(("%s_item" % piece, geometry(spec["item"], "wood")))
    for piece, geo, *_ in LIGHTS:
        out.append((piece, geometry(geo, "shell")))
    out.append(("refrigerator_lower", geometry(FRIDGE_LOWER, "shell")))
    out.append(("refrigerator_upper", geometry(FRIDGE_UPPER, "shell")))
    out.append(("refrigerator_item", geometry(FRIDGE, "shell", TALL_DISPLAY)))
    out.append(("chest_freezer", geometry(FREEZER, "shell")))
    return out


def generate(assets):
    written = []

    def dump(rel, data):
        R.dump(os.path.join(assets, rel), data)
        written.append(rel)

    for name, draw in TEXTURES.items():
        rel = "textures/blocks/%s/%s.png" % (SUB, name)
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
    for name, data in base_models():
        dump("models/block/%s/base/%s.json" % (SUB, name), data)
    for reg, kind, piece, ftex, _names, _java in entries():
        if kind == "run":
            spec = RUNS[piece]
            for part in spec["parts"]:
                dump("models/block/%s/%s_%s.json" % (SUB, reg, part),
                     {"parent": "csm:block/%s/base/%s_%s" % (SUB, piece, part),
                      "textures": dict(ftex)})
            dump("models/item/%s.json" % reg,
                 {"parent": "csm:block/%s/base/%s_item" % (SUB, piece), "textures": dict(ftex)})
            state = multipart_state(reg, spec["rules"])
        elif kind == "light":
            state = light_state(piece, ftex)
        elif kind == "fridge":
            for half in ("lower", "upper"):
                dump("models/block/%s/%s_%s.json" % (SUB, reg, half),
                     {"parent": "csm:block/%s/base/refrigerator_%s" % (SUB, half),
                      "textures": dict(ftex)})
            dump("models/item/%s.json" % reg,
                 {"parent": "csm:block/%s/base/refrigerator_item" % SUB, "textures": dict(ftex)})
            state = fridge_state(reg)
        else:
            state = R.single_state(piece, ftex)
        dump("blockstates/%s.json" % reg, state)
    R.write_lang(os.path.join(assets, "lang"), lang_entries())
    written += ["lang/%s.lang" % loc for loc in R.LOCALES]
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
    tmp = tempfile.mkdtemp(prefix="kitchen_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not R.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("kitchen furniture is up to date (%d files)" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
