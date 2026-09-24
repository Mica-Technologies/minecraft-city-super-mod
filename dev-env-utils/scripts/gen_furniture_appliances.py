#!/usr/bin/env python3
"""
gen_furniture_appliances.py -- the Residential tab's kitchen appliances, cooking and tableware in
the Furniture & Novelties module: the range and the wall oven, the cooktop base cabinet, the
dishwasher, the countertop appliances (microwave, toaster, air fryer, blender, coffee machine,
electric kettle, stand mixer), the cookie jar, the chopping board, and the tableware (dinner
plates, stacks of plates, coffee mugs, a glass of water, a cake on a stand); and the three foods
the appliances make (toast, a fruit smoothie, coffee).

Borrows gen_furniture_residential.py's element helpers and output and gen_furniture_kitchen.py's
finishes, textures and cabinet parts (imported, not copied), and writes, under
modules/furnishings/src/main/resources/assets/csm:

  * textures/blocks/furniture/residential/*.png  the cooktop glass, oven and microwave windows
    (off and lit), toaster slots, status lamps, black and red enamel, ceramics, plates, cakes,
    the blender jug and coffee carafe, glass and water
  * textures/items/residential/{toast,smoothie,coffee}.png and models/item/ for them
  * models/block/furniture/residential/base/<piece>_<rest>.json: each countertop piece once per
    surface it can stand on (see below), plus <piece>_item, the piece centred and scaled up for
    the inventory
  * the range, wall oven and dishwasher geometry, and the cooktop cabinet's parts per finish
  * blockstates/<registry>.json, models/item/<registry>.json where the state has no inventory
    variant, and the names in all four languages, by key

Every model faces north with its back at +Z, as the rest of the kitchen does.

**Standing on things.** A piece placed on a countertop is in the block above the counter, whose
floor is 1.5 px above the counter top (a counter is 14.5 px tall); on a dining table 4 px above
it. So each countertop piece is drawn once per SurfaceRest (Java) -- FLOOR, COUNTER, SIDEBOARD,
TABLE, SIDE_TABLE, TV_STAND, COFFEE_TABLE, dropped 0, 1.5, 2, 4, 7, 8 and 9 px -- and its
blockstate's "rest" variant, actual state from the block below, picks the one that stands on it.
The UVs are fitted at floor height and kept when the element is moved, so a banded texture (a
cake's layers) does not slide.

**Scale.** The full-size appliances are at real size (1 block = 1 m); the small things for the
counter are drawn at about one and a half times real size, as a mug 8 cm across would be under
two pixels.

**Joining.** The range and the dishwasher stand in a base-cabinet run: a cabinet beside one hides
its end panel (Java, IKitchenFitting), and the dishwasher carries the countertop of the cabinet
beside it (granite or quartz, "counter" actual state), or its own top when it stands alone.

Usage:
    python gen_furniture_appliances.py              # write everything
    python gen_furniture_appliances.py --check      # fail if the tree has drifted
    python gen_furniture_appliances.py --fragments  # print the tab registration lines

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
el, board, octagon = R.el, R.board, R.octagon
ITEM_TEX = "csm:items/residential/"

# (name, drop in px): the SurfaceRest enum's names and drops, in its order.
RESTS = [("floor", 0.0), ("counter", 1.5), ("sideboard", 2.0), ("table", 4.0),
         ("side_table", 7.0), ("tv_stand", 8.0), ("coffee_table", 9.0)]


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def blank(size):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def noisy(base, seed, size=16, grain=0.03):
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = R.shade(base, 1.0 + rng.uniform(-grain, grain)) + (255,)
    return img


def ring(px, size, cx, cy, r, width, colour):
    for y in range(size):
        for x in range(size):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r - width <= d <= r:
                px[x, y] = colour + (255,)


def cooktop(size=32):
    """Black glass ceramic from above, front (north) at the top: four burner rings of two
    sizes, and the touch controls at the front edge."""
    img = noisy((22, 22, 24), 501, size, 0.05)
    px = img.load()
    for cx, cy, r in ((8.5, 11, 6.2), (23.5, 11, 4.8), (8.5, 24, 4.8), (23.5, 24, 6.2)):
        ring(px, size, cx, cy, r, 1.0, (96, 96, 100))
        ring(px, size, cx, cy, r * 0.55, 0.7, (58, 58, 62))
    for x in range(12, 21, 2):
        px[x, 2] = (150, 150, 154, 255)
    px[21, 2] = (220, 60, 50, 255)
    return img


def window(on, kind, size=16):
    """An oven's or a microwave's door window, dark, or lit from inside."""
    rng = random.Random(510 + (1 if on else 0) + (10 if kind == "micro" else 0))
    img = blank(size)
    px = img.load()
    c = (size - 1) / 2.0
    for y in range(size):
        for x in range(size):
            if kind == "oven":
                if on:
                    k = 1.0 - 0.35 * (math.hypot(x - c, (y - c) * 1.2) / c)
                    col = R.shade((255, 150, 64), k)
                    if y in (6, 11):
                        col = R.shade((150, 70, 24), k)
                else:
                    col = (26, 26, 28)
                    if y in (6, 11):
                        col = (40, 40, 42)
                    if 0 < x - y < 3 and y < 7:
                        col = (58, 60, 64)
            else:
                if on:
                    k = 1.0 - 0.25 * (math.hypot(x - c, y - c) / c)
                    col = R.shade((250, 214, 128), k)
                    if y >= 11 and abs(x - c) < 5.5:
                        col = R.shade((255, 236, 190), k)
                    if (x + y) % 2 == 0:
                        col = R.shade(col, 0.88)
                else:
                    col = (24, 24, 26) if (x + y) % 2 else (36, 36, 38)
            col = R.shade(col, 1.0 + rng.uniform(-0.02, 0.02))
            px[x, y] = col + (255,)
    return img


def slot(on, size=16):
    """A toaster slot from above: dark, or its elements glowing."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if on:
                col = (255, 120, 44) if x % 3 else (190, 58, 22)
            else:
                col = (30, 28, 26) if x % 3 else (44, 40, 36)
            px[x, y] = col + (255,)
    return img


def lamp(colour, size=8):
    img = blank(size)
    px = img.load()
    c = (size - 1) / 2.0
    for y in range(size):
        for x in range(size):
            k = 1.15 - 0.3 * math.hypot(x - c, y - c) / c
            px[x, y] = R.shade(colour, k) + (255,)
    return img


def banded(rows, seed, size=16, highlight=None):
    """A texture of horizontal bands: rows is [(first row, colour)], each running to the next;
    an optional lighter column for glass's gleam."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    bands = sorted(rows)
    for y in range(size):
        col = [c for r, c in bands if r <= y][-1]
        for x in range(size):
            c = R.shade(col, 1.0 + rng.uniform(-0.03, 0.03))
            if highlight and x in highlight:
                c = R.shade(c, 1.18)
            px[x, y] = c + (255,)
    return img


def disc(size, draw):
    """A round thing seen from above on a transparent square: draw(r) gives the colour at r
    (0 at the centre, 1 at the rim) and the angle, or None outside it."""
    img = blank(size)
    px = img.load()
    c = size / 2.0
    for y in range(size):
        for x in range(size):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            r = math.hypot(dx, dy) / c
            if r > 1.0:
                continue
            col = draw(r, math.atan2(dy, dx))
            if col is not None:
                px[x, y] = tuple(col) + (255,)
    return img


PLATE = (246, 244, 238)
PLATE_BLUE = (46, 82, 160)


def plate_top(patterned, size=32):
    def draw(r, a):
        col = PLATE
        if 0.64 < r < 0.7:
            col = R.shade(PLATE, 0.86)
        elif r < 0.64:
            col = R.shade(PLATE, 0.97)
        if r > 0.95:
            col = R.shade(PLATE, 0.9)
        if patterned:
            if 0.73 < r < 0.77 or 0.9 < r < 0.94:
                col = PLATE_BLUE
            elif 0.79 < r < 0.88 and (math.degrees(a) % 30) < 11:
                col = R.shade(PLATE_BLUE, 1.2)
            elif r < 0.12:
                col = R.shade(PLATE_BLUE, 1.3)
        return col
    return disc(size, draw)


def stack_side(patterned, size=16):
    """Five plates' rims seen from the side, each 3.2 texels of the texture's height."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        k = (y % 3.2) / 3.2
        col = R.shade(PLATE, 1.02 if k < 0.35 else (0.95 if k < 0.7 else 0.8))
        if patterned and 0.35 <= k < 0.6:
            col = PLATE_BLUE
        for x in range(size):
            px[x, y] = col + (255,)
    return img


def mug_top(rim, size=32):
    def draw(r, a):
        if r > 0.86:
            return rim
        if r > 0.72:
            return (150, 104, 66)
        return R.shade((78, 48, 30), 1.0 - 0.15 * r)
    return disc(size, draw)


def glass(size=16):
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            edge = x in (0, size - 1) or y == size - 1
            gleam = x in (2, 3)
            a = 150 if edge else (120 if gleam else 70)
            c = (236, 244, 250) if (edge or gleam) else (214, 230, 240)
            px[x, y] = c + (a,)
    return img


def water(size=16):
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = ((96, 150, 214, 150) if y > 1 else (140, 186, 236, 170))
    return img


def cake(top, side_rows, seed, sprinkles=None):
    rng = random.Random(seed)
    t = noisy(top, seed, 16, 0.03)
    if sprinkles:
        px = t.load()
        for _ in range(26):
            px[rng.randrange(16), rng.randrange(16)] = rng.choice(sprinkles) + (255,)
    return t, banded(side_rows, seed + 1)


CHOC_TOP, CHOC_SIDE = cake((72, 40, 28), [(0, (72, 40, 28)), (3, (112, 68, 44)),
                                            (7, (58, 32, 22)), (9, (112, 68, 44)),
                                            (13, (72, 40, 28))], 540,
                           [(240, 210, 120), (230, 120, 160), (140, 200, 240), (250, 250, 250)])
BERRY_TOP, BERRY_SIDE = cake((248, 240, 230), [(0, (248, 240, 230)), (3, (238, 206, 140)),
                                                 (7, (186, 36, 58)), (9, (238, 206, 140)),
                                                 (13, (248, 240, 230))], 542,
                             [(226, 120, 150), (240, 170, 190)])


# --- item sprites ------------------------------------------------------------------------
def sprite(rows, palette):
    img = blank(16)
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = palette[ch] + (255,)
    return img


TOAST_ROWS = [
    "................",
    "...OOOO..OOOO...",
    "..OcccOOOOcccO..",
    ".OcttttccttttcO.",
    ".OcttttttttttcO.",
    ".OcttttttttttcO.",
    "..OcttttttttcO..",
    "..OctttdttttcO..",
    "..OcttttttttcO..",
    "..OcttttttdtcO..",
    "..OcttdtttttcO..",
    "..OcttttttttcO..",
    "..OctttttdttcO..",
    "..OccccccccccO..",
    "...OOOOOOOOOO...",
    "................",
]
TOAST_PAL = {"O": (92, 50, 20), "c": (156, 92, 38), "t": (224, 170, 96), "d": (196, 134, 66)}

SMOOTHIE_ROWS = [
    "..........gg....",
    ".........gg.....",
    "........gg......",
    "....OOOOgOOO....",
    "...OwwwwgwwwO...",
    "...OOOOOOOOOO...",
    "....OlpppppO....",
    "....OlpppppO....",
    "....OlpppppO....",
    ".....OlppppO....",
    ".....OlpppO.....",
    ".....OlpppO.....",
    ".....OlpppO.....",
    "......OppO......",
    "......OOOO......",
    "................",
]
SMOOTHIE_PAL = {"O": (120, 40, 70), "w": (246, 246, 246), "g": (70, 184, 96),
                "p": (228, 108, 150), "l": (244, 160, 190)}

COFFEE_ROWS = [
    "................",
    "................",
    "....OOOOOOOO....",
    "...OdddddddddO..",
    "...OOOOOOOOOO...",
    "....OwwwwwwwO...",
    "....OlwwwwwwO...",
    "....OsssssssO...",
    ".....OsssssO....",
    ".....OsssssO....",
    ".....OlwwwwO....",
    ".....OlwwwwO....",
    "......OwwwO.....",
    "......OwwwO.....",
    "......OOOOO.....",
    "................",
]
COFFEE_PAL = {"O": (70, 50, 36), "d": (56, 38, 28), "w": (244, 242, 236), "l": (255, 255, 255),
              "s": (150, 102, 64)}


TEXTURES = {
    "appliance_black": lambda: R.flat((34, 35, 37), 520, grain=2),
    "appliance_black_trim": lambda: R.flat((66, 68, 70), 521, grain=2),
    "appliance_red": lambda: R.flat((178, 32, 36), 522, grain=3),
    "cooktop_glass": cooktop,
    "oven_window_off": lambda: window(False, "oven"),
    "oven_window_on": lambda: window(True, "oven"),
    "microwave_window_off": lambda: window(False, "micro"),
    "microwave_window_on": lambda: window(True, "micro"),
    "toaster_slot_off": lambda: slot(False),
    "toaster_slot_on": lambda: slot(True),
    "lamp_green_off": lambda: lamp((36, 46, 40)),
    "lamp_green_on": lambda: lamp((96, 236, 126)),
    "lamp_blue_off": lambda: lamp((30, 36, 44)),
    "lamp_blue_on": lambda: lamp((70, 190, 255)),
    "ceramic_white": lambda: R.flat((240, 238, 232), 523, grain=2),
    "ceramic_red": lambda: R.flat((168, 40, 40), 524, grain=2),
    "ceramic_blue": lambda: R.flat((52, 86, 150), 525, grain=2),
    "cookie_jar_side": lambda: banded([(0, (236, 226, 204)), (12, (52, 86, 150)),
                                       (13, (236, 226, 204))], 526),
    "plate_plain": lambda: plate_top(False),
    "plate_patterned": lambda: plate_top(True),
    "plate_stack_side_plain": lambda: stack_side(False),
    "plate_stack_side_patterned": lambda: stack_side(True),
    "mug_top_white": lambda: mug_top((240, 238, 232)),
    "mug_top_red": lambda: mug_top((168, 40, 40)),
    "glass_clear": glass,
    "water_glass": water,
    "blender_jug": lambda: banded([(0, (206, 224, 234)), (4, (244, 150, 184)),
                                   (5, (226, 106, 148))], 527, highlight=(2, 3)),
    "coffee_carafe": lambda: banded([(0, (200, 216, 224)), (6, (96, 62, 40)),
                                     (7, (66, 40, 26))], 528, highlight=(2, 3)),
    "batter": lambda: R.flat((238, 226, 190), 529, grain=3),
    "cake_chocolate_top": lambda: CHOC_TOP,
    "cake_chocolate_side": lambda: CHOC_SIDE,
    "cake_strawberry_top": lambda: BERRY_TOP,
    "cake_strawberry_side": lambda: BERRY_SIDE,
    "topping_chocolate": lambda: R.flat((52, 28, 18), 530, grain=4),
    "topping_strawberry": lambda: R.flat((206, 30, 46), 531, grain=6),
}
ITEM_TEXTURES = {
    "toast": lambda: sprite(TOAST_ROWS, TOAST_PAL),
    "smoothie": lambda: sprite(SMOOTHIE_ROWS, SMOOTHIE_PAL),
    "coffee": lambda: sprite(COFFEE_ROWS, COFFEE_PAL),
}

DEFAULT_TEX = dict(K.DEFAULT_TEX)
DEFAULT_TEX.update({
    "cooktop": T("cooktop_glass"), "glow": T("oven_window_off"), "jug": T("blender_jug"),
    "carafe": T("coffee_carafe"), "batter": T("batter"), "jar": T("cookie_jar_side"),
    "lid": T("ceramic_blue"), "plate": T("plate_plain"), "plate_edge": T("ceramic_white"),
    "stack_side": T("plate_stack_side_plain"), "mug": T("ceramic_white"),
    "top": T("mug_top_white"), "glass": T("glass_clear"), "water": T("water_glass"),
    "stand": T("ceramic_white"), "cake_top": T("cake_chocolate_top"),
    "cake_side": T("cake_chocolate_side"), "topping": T("topping_chocolate"),
})

# Glow textures, off and on, for each kind of window or lamp.
GLOW = {
    "oven": (T("oven_window_off"), T("oven_window_on")),
    "micro": (T("microwave_window_off"), T("microwave_window_on")),
    "slot": (T("toaster_slot_off"), T("toaster_slot_on")),
    "green": (T("lamp_green_off"), T("lamp_green_on")),
    "blue": (T("lamp_blue_off"), T("lamp_blue_on")),
}

# Appliance finishes: (id, textures, en, de, es, sv)
STAINLESS = K.STAINLESS_FINISH
WHITE = K.WHITE_FINISH
BLACK = ("black", {"shell": T("appliance_black"), "door": T("appliance_black"),
                   "handle": T("chrome"), "trim": T("appliance_black_trim")},
         "Black", "Schwarz", "negro", "svart")
RED = ("red", {"shell": T("appliance_red"), "door": T("appliance_red"), "handle": T("chrome"),
               "trim": T("stainless_dark")},
       "Red", "Rot", "rojo", "röd")


def ALL_UV(spec, faces):
    """Maps the whole texture onto the given faces of spec (a label, a band, a cutout)."""
    for f in faces:
        spec["uv"][f] = [0, 0, 16, 16]
    return spec


def sides_of(specs):
    out = []
    for s in specs:
        out.append(ALL_UV(s, [f for f in s["faces"] if f not in ("up", "down")]))
    return out


# ------------------------------------------------------------------------------------------
# The full-size appliances
# ------------------------------------------------------------------------------------------
SIDES3 = ("north", "east", "west")
DOORF = ("north", "east", "west", "up", "down")


def bar_handle(x0, x1, y, z_face, depth=0.75):
    """A horizontal bar handle across a door whose face is at z_face, on two standoffs."""
    return [el([x0, y, z_face - depth], [x1, y + 0.6, z_face - depth + 0.4], "handle"),
            el([x0 + 0.5, y, z_face - depth + 0.4], [x0 + 1.1, y + 0.6, z_face], "handle",
               ("east", "west", "up", "down")),
            el([x1 - 1.1, y, z_face - depth + 0.4], [x1 - 0.5, y + 0.6, z_face], "handle",
               ("east", "west", "up", "down"))]


# Range: 0.76 m wide in a 1 m block, a glass ceramic top level with the countertops at 0.91 m,
# a backguard with the controls, the oven door with its window, and a storage drawer.
RANGE = ([el([0.5, 0, 2.5], [15.5, 1, 15.5], "rubber", SIDES3),
          el([0, 1, 1.5], [16, 14, 16], "shell", ("north", "south", "east", "west")),
          el([0, 14, 0.5], [16, 14.5, 14], "cooktop", ("up", "north", "east", "west"),
             per={"north": "trim", "east": "trim", "west": "trim"},
             uv={"up": [0, 0, 16, 16]}),
          el([0, 14, 14], [16, 18.5, 16], "shell"),
          el([1.5, 15.25, 13.95], [14.5, 17.75, 14], "control", ("north",)),
          el([0.25, 3.75, 0.75], [15.75, 13.5, 1.5], "door", DOORF),
          ALL_UV(el([2.5, 5.5, 0.7], [13.5, 11, 0.75], "glow", ("north",)), ["north"]),
          el([0.25, 1.25, 0.75], [15.75, 3.5, 1.5], "door", DOORF)]
         + bar_handle(2, 14, 12, 0.75)
         + bar_handle(5, 11, 2.1, 0.75, depth=0.6))

# Wall oven: a single oven built into the wall, a whole block.
WALL_OVEN = ([el([0, 0, 1.5], [16, 16, 16], "shell"),
              el([1, 13, 1.25], [15, 15.25, 1.5], "control", ("north",)),
              el([0.75, 1, 0.75], [15.25, 12.5, 1.5], "door", DOORF),
              ALL_UV(el([2.75, 3, 0.7], [13.25, 9.75, 0.75], "glow", ("north",)), ["north"])]
             + bar_handle(2, 14, 10.75, 0.75))

# Dishwasher: under the countertop of a base run, its controls on the top of the door.
DW_BODY = ([el([0.5, 0, 1.75], [15.5, 1.5, 2], "rubber", ("north",)),
            el([0, 1.5, 1.75], [16, 13.75, 16], "shell", ("south", "east", "west")),
            el([0.25, 1.5, 1], [15.75, 13.75, 1.75], "door", DOORF),
            el([0.25, 12.25, 0.95], [15.75, 13.5, 1], "control", ("north",))]
           + bar_handle(4, 12, 10.5, 1))
DW_LED = [ALL_UV(el([13.25, 12.6, 0.9], [14, 12.95, 0.95], "glow", ("north",)), ["north"])]
DW_COUNTER = [el([0, 13.75, 0.5], [16, 14.5, 16], "counter")]
DW_TOP = [el([0, 13.75, 1], [16, 14.5, 16], "shell", ("north", "south", "east", "west", "up"))]

# The cooktop base cabinet: the drawer bank, with a glass cooktop set into its countertop.
COOKTOP = [el([1.5, 14.5, 1.75], [14.5, 14.6, 12.75], "cooktop",
              ("up", "north", "south", "east", "west"),
              per={f: "rubber" for f in ("north", "south", "east", "west")},
              uv={"up": [0, 0, 16, 16]})]
COOKTOP_BODY = K.DRAWER_BODY + COOKTOP


# ------------------------------------------------------------------------------------------
# The countertop appliances and small things, standing on the floor of their block
# ------------------------------------------------------------------------------------------
def feet(x0, x1, z0, z1, h=0.25, w=0.75):
    return [el([x, 0, z], [x + w, h, z + w], "rubber", ("north", "south", "east", "west"))
            for x in (x0, x1 - w) for z in (z0, z1 - w)]


MICROWAVE = (feet(2.5, 13.5, 5, 13.5)
             + [el([2, 0.25, 4.5], [14, 7, 14], "shell", ("south", "east", "west", "up",
                                                          "down")),
                el([2, 0.25, 4], [10.75, 7, 4.5], "door", DOORF),
                ALL_UV(el([3, 1.25, 3.95], [9.25, 6, 4], "glow", ("north",)), ["north"]),
                el([10.75, 0.25, 4.25], [14, 7, 4.5], "trim", ("north", "east", "up", "down")),
                el([11.25, 3.5, 4.2], [13.5, 6.25, 4.25], "control", ("north",)),
                el([11.75, 1.25, 4.1], [13, 2.5, 4.25], "handle", ("north", "east", "west", "up",
                                                                   "down")),
                el([9.75, 1.5, 3.5], [10.25, 5.75, 4], "handle")])

# Toaster: two slots across the top, glowing when it toasts; lever on its side.
TOAST_TOP = [el([4.5, 5, z0], [11.5, 5.25, z1], "shell", ("up", "north", "south", "east", "west"))
             for z0, z1 in ((6, 7), (8, 9), (10, 11))]
TOAST_TOP += [el([x0, 5, z0], [x1, 5.25, z0 + 1], "shell", ("up", "east", "west"))
              for x0, x1 in ((4.5, 5.5), (10.5, 11.5)) for z0 in (7, 9)]
TOASTER = (feet(5, 11, 6.5, 10.5)
           + [el([4.5, 0.25, 6], [11.5, 5, 11], "shell", ("north", "south", "east", "west",
                                                          "down"))]
           + TOAST_TOP
           + [ALL_UV(el([5.5, 3.5, z0], [10.5, 3.51, z0 + 1], "glow", ("up",)), ["up"])
              for z0 in (7, 9)]
           + [el([5.5, 3.5, z0], [10.5, 5, z0 + 0.01], "rubber", ("south",)) for z0 in (7, 9)]
           + [el([5.5, 3.5, z0 + 0.99], [10.5, 5, z0 + 1], "rubber", ("north",))
              for z0 in (7, 9)]
           + [el([11.5, 3.25, 8], [12.1, 3.85, 9.25], "handle",
                 ("north", "south", "east", "up", "down")),
              el([7.25, 1, 5.6], [8.75, 2, 6], "trim", ("north", "east", "west", "up", "down"))])

# Air fryer: a rounded body, the basket's front and handle, a display on top of the front.
AIR_FRYER = (octagon(8, 9, 3.4, 0.25, 7, "shell", caps=("down",))
             + octagon(8, 9, 3.1, 7, 7.5, "trim", caps=("up",))
             + [el([5, 1, 5.3], [11, 4.75, 5.8], "trim", ("north", "east", "west", "up", "down")),
                el([7, 2.25, 3.9], [9, 3.25, 5.3], "handle", ("north", "east", "west", "up",
                                                              "down")),
                ALL_UV(el([6.5, 5.9, 5.55], [9.5, 6.5, 5.6], "glow", ("north",)), ["north"])])

# Blender: the motor base, a jug of smoothie with its lid and handle.
BLENDER = ([el([5.5, 0, 6.5], [10.5, 3, 11.5], "shell"),
            el([7.25, 1, 6.3], [8.75, 2, 6.5], "handle", DOORF),
            ALL_UV(el([9.1, 2.2, 6.45], [9.7, 2.6, 6.5], "glow", ("north",)), ["north"])]
           + sides_of([el([6, 3, 7], [10, 9.5, 11], "jug", ("north", "south", "east", "west"))])
           + [el([5.75, 9.5, 6.75], [10.25, 10.25, 11.25], "rubber"),
              el([10, 7.5, 8.5], [11.25, 8.25, 9.5], "rubber", ("north", "south", "east", "up",
                                                               "down")),
              el([10.5, 4.5, 8.5], [11.25, 7.5, 9.5], "rubber", ("north", "south", "east")),
              el([10, 4.5, 8.5], [10.5, 5.25, 9.5], "rubber", ("north", "south", "up", "down"))])

# Coffee machine: drip tray, the water tower at the back, the head over a carafe of coffee.
COFFEE_MACHINE = ([el([4.5, 0, 5], [11.5, 1, 13], "trim"),
                   el([4.5, 1, 9.5], [11.5, 9, 13], "shell", ("north", "south", "east", "west")),
                   el([4.5, 6.5, 5], [11.5, 9, 9.5], "shell", ("north", "east", "west", "down")),
                   el([4.5, 9, 5], [11.5, 9.25, 13], "trim", ("up", "north", "south", "east",
                                                              "west")),
                   el([7, 6.25, 6.5], [9, 6.5, 8], "trim", ("down", "north", "east", "west")),
                   el([5.5, 7.25, 4.95], [10.5, 8.25, 5], "control", ("north",)),
                   ALL_UV(el([9.25, 7.5, 4.9], [9.75, 8, 4.95], "glow", ("north",)), ["north"])]
                  + sides_of([el([6, 1, 5.5], [9.5, 4.5, 8.5], "carafe",
                                 ("north", "south", "east", "west"))])
                  + [el([5.75, 4.5, 5.25], [9.75, 5, 8.75], "rubber"),
                     el([9.5, 1.75, 6.5], [10.25, 4, 7.5], "rubber", ("north", "south", "east",
                                                                      "up", "down"))])

# Electric kettle: on its base, the handle at the back, the spout to the front.
KETTLE = (octagon(8, 9, 2.9, 0, 0.5, "rubber")
          + octagon(8, 9, 2.5, 0.5, 5, "shell", caps=("up",))
          + [el([7.5, 5, 8.5], [8.5, 5.6, 9.5], "rubber"),
             el([7.25, 1.5, 11.4], [8.75, 2.25, 12.75], "rubber", ("south", "east", "west",
                                                                  "up", "down")),
             el([7.25, 2.25, 12], [8.75, 4.5, 12.75], "rubber", ("south", "east", "west")),
             el([7.25, 4.5, 11.2], [8.75, 5.25, 12.75], "rubber", ("south", "east", "west",
                                                                  "up", "down")),
             el([7.4, 3, 5], [8.6, 4.2, 6.6], "shell", ("north", "east", "west", "up", "down")),
             el([7.25, 1, 6.45], [8.75, 1.6, 6.5], "trim", ("north",))])
KETTLE_STEAM = [8, 4.6, 4.8]

# Stand mixer: its base and column, the head over the bowl, the bowl with batter in it.
STAND_MIXER = ([el([5, 0, 5.5], [11, 1.25, 12.5], "shell"),
                el([6.5, 1.25, 10], [9.5, 7.25, 12.5], "shell", ("north", "south", "east",
                                                                 "west")),
                el([6.25, 6.25, 5], [9.75, 9, 12.75], "shell"),
                el([7.75, 4.75, 6.75], [8.25, 6.25, 7.25], "chrome",
                   ("north", "south", "east", "west")),
                el([9.5, 4, 11], [10, 4.75, 11.75], "trim", ("north", "south", "east", "up",
                                                            "down")),
                el([6.5, 1.25, 5.75], [9.5, 1.5, 9.75], "trim", ("up", "north", "east",
                                                                  "west"))]
               + octagon(8, 7.75, 2.4, 1.5, 4.75, "chrome", caps=("down",))
               + octagon(8, 7.75, 2.2, 3.6, 3.7, "batter", caps=("up",)))

COOKIE_JAR = (octagon(8, 8, 3, 0, 5.5, "jar", caps=("down",))
              + octagon(8, 8, 3.25, 5.5, 6.25, "lid")
              + octagon(8, 8, 0.9, 6.25, 7.25, "lid", caps=("up",)))

CHOPPING_BOARD = [board([3, 0, 4.5], [13, 0.75, 11.5]),
                  el([4.5, 0.75, 7.25], [9.5, 0.85, 8.25], "chrome",
                     ("up", "north", "south", "west")),
                  el([9.5, 0.75, 7.35], [12, 1.15, 8.15], "rubber",
                     ("up", "north", "south", "east", "west"))]


def plate_disc(y, size, face=("up", "down"), key="plate"):
    lo, hi = 8 - size / 2.0, 8 + size / 2.0
    return ALL_UV(el([lo, y, lo], [hi, y, hi], key, face), face)


DINNER_PLATE = (octagon(8, 8, 3.5, 0, 0.5, "plate_edge", caps=())
                + [plate_disc(0.5, 7.6, ("up",)), plate_disc(0.02, 7.6, ("down",))])
PLATE_STACK = (sides_of(octagon(8, 8, 3.5, 0, 2.5, "stack_side", caps=()))
               + [plate_disc(2.5, 7.6, ("up",)), plate_disc(0.02, 7.6, ("down",))])
COFFEE_MUG = (octagon(8, 8, 1.5, 0, 3, "mug", caps=("down",))
              + [plate_disc(3, 3.3, ("up",), "top"),
                 el([9.45, 0.6, 7.6], [10.5, 1.1, 8.4], "mug", ("north", "south", "east", "up",
                                                               "down")),
                 el([10, 1.1, 7.6], [10.5, 2.4, 8.4], "mug", ("north", "south", "east", "west")),
                 el([9.45, 2.4, 7.6], [10.5, 2.9, 8.4], "mug", ("north", "south", "east", "up",
                                                               "down"))])
DRINKING_GLASS = [el([7, 0, 7], [9, 3.5, 9], "glass", ("north", "south", "east", "west",
                                                      "down")),
                  el([7.2, 0.2, 7.2], [8.8, 2.6, 8.8], "water")]
CAKE_STAND = (octagon(8, 8, 1.6, 0, 0.4, "stand")
              + [el([7.4, 0.4, 7.4], [8.6, 2, 8.6], "stand", ("north", "south", "east", "west"))]
              + octagon(8, 8, 4, 2, 2.4, "stand")
              + sides_of(octagon(8, 8, 3.3, 2.4, 6.2, "cake_top", side="cake_side",
                                 caps=("up",)))
              + [el([x - 0.4, 6.2, z - 0.4], [x + 0.4, 6.8, z + 0.4], "topping",
                    ("north", "south", "east", "west", "up"))
                 for x, z in ((8, 5.8), (10.2, 8), (8, 10.2), (5.8, 8), (8, 8))])


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
def fin(fid, tex, *names):
    return (fid, tex) + names


def mug_finish(fid, colour_tex, top_tex, *names):
    return (fid, {"mug": T(colour_tex), "top": T(top_tex)}) + names


WOOD_FINISHES = [(fid, dict(ftex), *names) for fid, ftex, *names in R.WOODS]

# Countertop pieces: piece -> (geometry, particle, java kind, glow kind, finishes, names, extra)
# java kind: "appliance" (spec), "piece" (material, sound, layer[, sound, pitch, steam]),
# "jar".
COUNTER = {
    "microwave": (MICROWAVE, "shell", ("appliance", "MICROWAVE"), "micro",
                  [STAINLESS, WHITE, BLACK],
                  ("Microwave", "Mikrowelle", "Microondas", "Mikrovågsugn")),
    "toaster": (TOASTER, "shell", ("appliance", "TOASTER"), "slot", [STAINLESS, WHITE, RED],
                ("Toaster", "Toaster", "Tostadora", "Brödrost")),
    "air_fryer": (AIR_FRYER, "shell", ("appliance", "AIR_FRYER"), "blue", [BLACK, WHITE],
                  ("Air Fryer", "Heißluftfritteuse", "Freidora de aire", "Airfryer")),
    "blender": (BLENDER, "shell", ("appliance", "BLENDER"), "green", [STAINLESS, BLACK],
                ("Blender", "Standmixer", "Batidora de vaso", "Mixer")),
    "coffee_machine": (COFFEE_MACHINE, "shell", ("appliance", "COFFEE_MACHINE"), "green",
                       [STAINLESS, BLACK],
                       ("Coffee Machine", "Kaffeemaschine", "Cafetera", "Kaffebryggare")),
    "kettle": (KETTLE, "shell",
               ("piece", "Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, "
                         "FurnishingsSounds.KETTLE_WHISTLE, 1.0F, new double[]{%s}"
                % ", ".join(R._num(v) for v in KETTLE_STEAM)),
               None, [STAINLESS, WHITE, RED],
               ("Electric Kettle", "Wasserkocher", "Hervidor eléctrico", "Vattenkokare")),
    "stand_mixer": (STAND_MIXER, "shell",
                    ("piece", "Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, "
                              "FurnishingsSounds.BLENDER_WHIRR, 0.7F, null"),
                    None, [RED, WHITE, STAINLESS],
                    ("Stand Mixer", "Küchenmaschine", "Batidora amasadora", "Köksmaskin")),
    "cookie_jar": (COOKIE_JAR, "jar", ("jar",), None,
                   [fin("ceramic", {"jar": T("cookie_jar_side"), "lid": T("ceramic_blue")}, "Ceramic",
                        "Keramik", "cerámica", "keramik")],
                   ("Cookie Jar", "Keksdose", "Tarro de galletas", "Kakburk")),
    "chopping_board": (CHOPPING_BOARD, "wood",
                       ("piece", "Material.WOOD, SoundType.WOOD, BlockRenderLayer.SOLID"),
                       None, WOOD_FINISHES,
                       ("Chopping Board", "Schneidebrett", "Tabla de cortar", "Skärbräda")),
    "dinner_plate": (DINNER_PLATE, "plate_edge",
                     ("piece", "Material.GLASS, SoundType.GLASS, BlockRenderLayer.CUTOUT"), None,
                     [fin("plain", {"plate": T("plate_plain")}, "Plain", "Schlicht", "liso",
                          "enkel"),
                      fin("patterned", {"plate": T("plate_patterned")}, "Patterned",
                          "Gemustert", "decorado", "mönstrad")],
                     ("Dinner Plate", "Speiseteller", "Plato llano", "Middagstallrik")),
    "plate_stack": (PLATE_STACK, "plate_edge",
                    ("piece", "Material.GLASS, SoundType.GLASS, BlockRenderLayer.CUTOUT"), None,
                    [fin("plain", {"plate": T("plate_plain"),
                                   "stack_side": T("plate_stack_side_plain")},
                         "Plain", "Schlicht", "lisos", "enkla"),
                     fin("patterned", {"plate": T("plate_patterned"),
                                       "stack_side": T("plate_stack_side_patterned")},
                         "Patterned", "Gemustert", "decorados", "mönstrade")],
                    ("Stack of Plates", "Tellerstapel", "Pila de platos", "Tallrikstrave")),
    "coffee_mug": (COFFEE_MUG, "mug",
                   ("piece", "Material.GLASS, SoundType.GLASS, BlockRenderLayer.CUTOUT"), None,
                   [mug_finish("white", "ceramic_white", "mug_top_white", "White", "Weiß",
                               "blanca", "vit"),
                    mug_finish("red", "ceramic_red", "mug_top_red", "Red", "Rot", "roja",
                               "röd")],
                   ("Coffee Mug", "Kaffeebecher", "Taza de café", "Kaffemugg")),
    "drinking_glass": (DRINKING_GLASS, "glass",
                       ("piece", "Material.GLASS, SoundType.GLASS, BlockRenderLayer.TRANSLUCENT"),
                       None,
                       [fin("water", {"glass": T("glass_clear"), "water": T("water_glass")}, "Water",
                            "Wasser", "agua", "vatten")],
                       ("Drinking Glass", "Trinkglas", "Vaso", "Dricksglas")),
    "cake_stand": (CAKE_STAND, "stand",
                   ("piece", "Material.GLASS, SoundType.GLASS, BlockRenderLayer.SOLID"), None,
                   [fin("chocolate", {"cake_top": T("cake_chocolate_top"),
                                      "cake_side": T("cake_chocolate_side"),
                                      "topping": T("topping_chocolate")},
                        "Chocolate Cake", "Schokoladentorte", "tarta de chocolate",
                        "chokladtårta"),
                    fin("strawberry", {"cake_top": T("cake_strawberry_top"),
                                       "cake_side": T("cake_strawberry_side"),
                                       "topping": T("topping_strawberry")},
                        "Strawberry Cake", "Erdbeertorte", "tarta de fresa", "jordgubbstårta")],
                   ("Cake Stand", "Tortenständer", "Soporte para tartas", "Tårtfat")),
}

# The full-size appliances: piece -> (geometry or None, glow kind, finishes, names, java)
BUILT_IN = {
    "kitchen_range": (RANGE, "oven", [STAINLESS, WHITE],
                      ("Electric Range", "Elektroherd", "Cocina eléctrica", "Elspis"),
                      'new BlockBuiltInAppliance("%s", new int[]{0, 0, 0, 16, 16, 16}, '
                      'KitchenAppliances.OVEN, KitchenLine.BASE)'),
    "wall_oven": (WALL_OVEN, "oven", [STAINLESS, WHITE],
                  ("Wall Oven", "Einbaubackofen", "Horno de pared", "Inbyggnadsugn"),
                  'new BlockBuiltInAppliance("%s", new int[]{0, 0, 0, 16, 16, 16}, '
                  'KitchenAppliances.OVEN, null)'),
    "dishwasher": (None, "green", [STAINLESS, WHITE],
                   ("Dishwasher", "Geschirrspüler", "Lavavajillas", "Diskmaskin"),
                   'new BlockDishwasher("%s", KitchenAppliances.DISHWASHER)'),
}

COOKTOP_NAMES = ("Kitchen Cooktop Cabinet", "Küchen-Unterschrank mit Kochfeld",
                 "Mueble bajo con placa de cocción", "Bänkskåp med häll")
COOKTOP_JAVA = ('new BlockKitchenCabinet("%s", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, '
                '18, KitchenFront.DRAWERS)')

FOODS = [
    # (registry, hunger, saturation, drink, effect java, en, de, es, sv)
    ("toast", 6, 0.75, False, "null", "Toast", "Toast", "Tostada", "Rostat bröd"),
    ("smoothie", 4, 0.6, True, "null", "Fruit Smoothie", "Frucht-Smoothie", "Batido de frutas",
     "Fruktsmoothie"),
    ("coffee", 1, 0.2, True, "new PotionEffect(MobEffects.SPEED, 600, 0)", "Coffee", "Kaffee",
     "Café", "Kaffe"),
]

EXTRA_LANG = {
    "csm.furnishings.appliance.water": ("Water: %s of %s", "Wasser: %s von %s", "Agua: %s de %s",
                                        "Vatten: %s av %s"),
    "csm.furnishings.appliance.water.hint": (
        "Fill it with a water bucket, or stand it beside a sink",
        "Mit einem Wassereimer füllen oder neben eine Spüle stellen",
        "Llénalo con un cubo de agua o ponlo junto a un fregadero",
        "Fyll den med en vattenhink eller ställ den bredvid en diskbänk"),
    "csm.furnishings.appliance.water.empty": ("No water", "Kein Wasser", "Sin agua",
                                              "Inget vatten"),
}


def names_with(names, fnames):
    return ["%s (%s)" % (n, f) for n, f in zip(names, fnames)]


def box_of(specs, drop=0.0):
    """The Java box of a piece standing on the floor: its elements' extent, whole pixels."""
    built = [R.build(s) for s in specs]
    lo = [min(e["from"][i] for e in built) for i in range(3)]
    hi = [max(e["to"][i] for e in built) for i in range(3)]
    lo = [max(0, int(math.floor(v))) for v in lo]
    hi = [min(16, int(math.ceil(v))) for v in hi]
    return lo + hi


def counter_java(piece, reg):
    geo, _p, kind, _g, _f, _n = COUNTER[piece]
    box = ", ".join(str(v) for v in box_of(geo))
    if kind[0] == "appliance":
        return 'new BlockCounterAppliance("%s", new int[]{%s}, KitchenAppliances.%s)' % (
            reg, box, kind[1])
    if kind[0] == "jar":
        return 'new BlockCookieJar("%s", new int[]{%s})' % (reg, box)
    return 'new BlockCounterPiece("%s", new int[]{%s}, %s)' % (reg, box, kind[1])


def entries():
    """Every block: (registry, kind, piece, finish textures, names, java)."""
    out = []
    for piece, (_geo, glow, finishes, names, java) in BUILT_IN.items():
        for fid, ftex, *fnames in finishes:
            reg = "%s_%s" % (piece, fid)
            out.append((reg, "dishwasher" if piece == "dishwasher" else "built_in", piece,
                        ftex, names_with(names, fnames), java % reg))
        if piece == "kitchen_range":
            # The cooktop cabinet sits in the base run beside the range in the tab.
            for fid, ftex, *fnames in K.WOODS:
                reg = "kitchen_cooktop_cabinet_%s" % fid
                out.append((reg, "cooktop", "kitchen_cooktop_cabinet", ftex,
                            names_with(COOKTOP_NAMES, fnames), COOKTOP_JAVA % reg))
    for piece, (_geo, _p, _kind, _glow, finishes, names) in COUNTER.items():
        for fid, ftex, *fnames in finishes:
            reg = "%s_%s" % (piece, fid)
            out.append((reg, "counter", piece, ftex, names_with(names, fnames),
                        counter_java(piece, reg)))
    return out


# ------------------------------------------------------------------------------------------
# Models
# ------------------------------------------------------------------------------------------
def geometry_json(specs, particle, dy=0.0, display=None, centre=False):
    """A base model: the elements built at floor height (so their UVs fit there), then moved
    down by dy -- or, for an item, moved so the piece's middle is the block's."""
    built = [R.build(s) for s in specs]
    if centre:
        lo = [min(e["from"][i] for e in built) for i in range(3)]
        hi = [max(e["to"][i] for e in built) for i in range(3)]
        move = [8 - (lo[i] + hi[i]) / 2.0 for i in range(3)]
    else:
        move = [0.0, -dy, 0.0]
    for e in built:
        for key in ("from", "to"):
            e[key] = [round(e[key][i] + move[i], 3) for i in range(3)]
        if "rotation" in e:
            o = e["rotation"]["origin"]
            e["rotation"]["origin"] = [round(o[i] + move[i], 3) for i in range(3)]
    tex = {k: DEFAULT_TEX[k] for k in sorted(R.used_keys(specs) | {particle})}
    tex["particle"] = "#" + particle
    out = {"parent": "block/block", "textures": tex, "elements": built}
    if display:
        out["display"] = display
    return out


def small_display(specs):
    """The block display, scaled up for a small piece so it fills its slot."""
    built = [R.build(s) for s in specs]
    size = max(max(e["to"][i] for e in built) - min(e["from"][i] for e in built)
               for i in range(3))
    k = round(min(2.2, max(1.0, 11.0 / size)), 3)

    def d(rot, tr, s):
        return {"rotation": rot, "translation": tr, "scale": [round(s * k, 4)] * 3}
    return {"gui": d([30, 225, 0], [0, 0, 0], 0.625),
            "ground": d([0, 0, 0], [0, 3, 0], 0.25),
            "fixed": d([0, 0, 0], [0, 0, 0], 0.5),
            "thirdperson_righthand": d([75, 45, 0], [0, 2.5, 0], 0.375),
            "firstperson_righthand": d([0, 45, 0], [0, 0, 0], 0.4),
            "firstperson_lefthand": d([0, 225, 0], [0, 0, 0], 0.4)}


def base_models():
    """Every base geometry model: (name, geometry json)."""
    out = []
    for piece, (geo, particle, *_rest) in COUNTER.items():
        for rest, drop in RESTS:
            out.append(("%s_%s" % (piece, rest), geometry_json(geo, particle, drop)))
        out.append(("%s_item" % piece, geometry_json(geo, particle, display=small_display(geo),
                                                     centre=True)))
    out.append(("kitchen_range", geometry_json(RANGE, "shell")))
    out.append(("wall_oven", geometry_json(WALL_OVEN, "shell")))
    out.append(("dishwasher_body", geometry_json(DW_BODY, "shell")))
    out.append(("dishwasher_led", geometry_json(DW_LED, "shell")))
    out.append(("dishwasher_counter", geometry_json(DW_COUNTER, "counter")))
    out.append(("dishwasher_top", geometry_json(DW_TOP, "shell")))
    out.append(("dishwasher_item", geometry_json(DW_BODY + DW_LED + DW_TOP, "shell")))
    for part, geo in COOKTOP_RUN["parts"].items():
        out.append(("kitchen_cooktop_cabinet_%s" % part, geometry_json(geo, "wood")))
    out.append(("kitchen_cooktop_cabinet_item", geometry_json(COOKTOP_RUN["item"], "wood")))
    return out


COOKTOP_RUN = K.run_piece(COOKTOP_BODY, K.BASE_END, COOKTOP_JAVA)


# ------------------------------------------------------------------------------------------
# Blockstates
# ------------------------------------------------------------------------------------------
def facing_variants():
    return {f: ({"y": r} if r else {}) for f, r in ROT.items()}


def glow_variants(kind):
    off, on = GLOW[kind]
    return {"true": {"textures": {"glow": on}}, "false": {"textures": {"glow": off}}}


def counter_state(piece, ftex, glow):
    tex = dict(ftex)
    if glow:
        tex["glow"] = GLOW[glow][0]
    variants = {"facing": facing_variants(),
                "rest": {rest: {"model": BASE + "%s_%s" % (piece, rest)} for rest, _d in RESTS},
                "inventory": [{"model": BASE + "%s_item" % piece}]}
    if glow:
        variants["running"] = glow_variants(glow)
    return {"forge_marker": 1,
            "defaults": {"model": BASE + "%s_floor" % piece, "textures": tex},
            "variants": variants}


def built_in_state(piece, ftex, glow):
    tex = dict(ftex)
    tex["glow"] = GLOW[glow][0]
    return {"forge_marker": 1,
            "defaults": {"model": BASE + piece, "textures": tex},
            "variants": {"facing": facing_variants(), "running": glow_variants(glow),
                         "inventory": [{}]}}


DW_RULES = R.faced([("body", {}), ("led_on", {"running": "true"}),
                    ("led_off", {"running": "false"}), ("top", {"counter": "none"}),
                    ("counter_granite", {"counter": "granite"}),
                    ("counter_quartz", {"counter": "quartz"})])


def dishwasher_parts(ftex):
    """The dishwasher's part files: (part, base model, textures)."""
    off, on = GLOW["green"]
    return [("body", "dishwasher_body", dict(ftex)),
            ("led_on", "dishwasher_led", dict(ftex, glow=on)),
            ("led_off", "dishwasher_led", dict(ftex, glow=off)),
            ("top", "dishwasher_top", dict(ftex)),
            ("counter_granite", "dishwasher_counter", {"counter": T("counter_granite")}),
            ("counter_quartz", "dishwasher_counter", {"counter": T("counter_quartz")})]


def lang_entries():
    out = {loc: {} for loc in R.LOCALES}
    for key, names in EXTRA_LANG.items():
        for i, loc in enumerate(R.LOCALES):
            out[loc][key] = names[i]
    for reg, _k, _p, _t, names, _j in entries():
        for i, loc in enumerate(R.LOCALES):
            out[loc]["tile.%s.name" % reg] = names[i]
    for reg, _h, _s, _d, _e, *names in FOODS:
        for i, loc in enumerate(R.LOCALES):
            out[loc]["item.%s.name" % reg] = names[i]
    return out


def generate(assets):
    written = []

    def dump(rel, data):
        R.dump(os.path.join(assets, rel), data)
        written.append(rel)

    def save(rel, img):
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        written.append(rel)

    for name, draw in TEXTURES.items():
        save("textures/blocks/%s/%s.png" % (SUB, name), draw())
    for name, draw in ITEM_TEXTURES.items():
        save("textures/items/residential/%s.png" % name, draw())
    for name, data in base_models():
        dump("models/block/%s/base/%s.json" % (SUB, name), data)
    for reg, kind, piece, ftex, _names, _java in entries():
        if kind == "counter":
            glow = COUNTER[piece][3]
            state = counter_state(piece, ftex, glow)
        elif kind == "built_in":
            state = built_in_state(piece, ftex, BUILT_IN[piece][1])
        elif kind == "dishwasher":
            for part, base, tex in dishwasher_parts(ftex):
                dump("models/block/%s/%s_%s.json" % (SUB, reg, part),
                     {"parent": "csm:block/%s/base/%s" % (SUB, base), "textures": tex})
            tex = dict(ftex)
            tex["glow"] = GLOW["green"][0]
            dump("models/item/%s.json" % reg,
                 {"parent": "csm:block/%s/base/dishwasher_item" % SUB, "textures": tex})
            state = K.multipart_state(reg, DW_RULES)
        else:  # the cooktop cabinet, a kitchen run piece
            for part in COOKTOP_RUN["parts"]:
                dump("models/block/%s/%s_%s.json" % (SUB, reg, part),
                     {"parent": "csm:block/%s/base/%s_%s" % (SUB, piece, part),
                      "textures": dict(ftex)})
            dump("models/item/%s.json" % reg,
                 {"parent": "csm:block/%s/base/%s_item" % (SUB, piece), "textures": dict(ftex)})
            state = K.multipart_state(reg, COOKTOP_RUN["rules"])
        dump("blockstates/%s.json" % reg, state)
    for reg, *_rest in FOODS:
        dump("models/item/%s.json" % reg,
             {"parent": "item/generated", "textures": {"layer0": ITEM_TEX + reg}})
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
    lines.append("")
    lines.append("    // Food and drink the appliances make")
    for reg, hunger, sat, drink, effect, *_names in FOODS:
        lines.append("    initTabItem(new ItemResidentialFood(\"%s\", %d, %sF, %s, %s));"
                     % (reg, hunger, R._num(sat), "true" if drink else "false", effect))
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
    tmp = tempfile.mkdtemp(prefix="appliances_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not R.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("kitchen appliances are up to date (%d files)" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
