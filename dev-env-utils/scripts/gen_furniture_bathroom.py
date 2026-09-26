#!/usr/bin/env python3
"""
gen_furniture_bathroom.py -- the Residential tab's bathroom, restroom and laundry in the
Furniture & Novelties module: the toilet, toilet paper holder, pedestal sink, bathroom vanity
and mirror cabinet (in the three wood finishes), the two-block bathtub, the shower enclosure
and a wall shower head, towel rails, the bathroom radiator, wastebaskets, a toiletries tray, a
toilet brush and bath mats; for a commercial restroom the wall-hung urinal, soap and paper
towel dispensers, a grab bar and the fold-down baby changing station; and for the laundry the
washing machine and dryer, a steam iron and ironing board, laundry baskets and a laundry tub.

Borrows gen_furniture_residential.py's element helpers, finishes and output, the kitchen's
Shaker fronts, tap, stainless and white finishes and the cut of a two-block piece, the
bedroom's cut into cells and its rug geometry (a bath mat is a rug in terry), and the
appliances' surface rests (all imported, not copied), and writes, under
modules/furnishings/src/main/resources/assets/csm:

  * textures/blocks/furniture/residential/*.png  porcelain, bath water, shower glass (a
    translucent texture, as the drinking glass's is) and rose, terry towelling, toilet paper,
    the radiator's columns, the washer's and dryer's round door windows (a cutout on a square,
    off and lit), wicker, the ironing board's covers, the bath mats
  * models/block/furniture/residential/base/*.json  the geometry
  * models/block/furniture/residential/<registry>_<part>.json  a finish's copy of each part a
    multipart blockstate picks
  * blockstates/<registry>.json, and models/item/<registry>.json where the state has no
    inventory variant
  * the tile names in all four languages, by key

Every model faces north with its back at +Z, against the wall, as in the rest of the tab.
Real-world scale, 1 block = 1 m: a toilet seat at 0.42 m, a vanity top and the washing
machine's top at a kitchen countertop's 0.91 m (so the small pieces rest on them), the bath's
rim at 0.56 m. Wall pieces are drawn at the height they hang in the block they are placed in;
the shower head and the baby changing station are meant for the block above the floor's.

What joins or changes (the Java classes compute joins as actual state; the rest is stored):

  * the vanity joins left and right like kitchen base cabinets, one stone top over a run,
    end panels only where it stops (its own KitchenLine, so it never joins a kitchen);
  * the bathtub is two blocks, drawn whole and cut into cells like a bed, with its water
    surface a separate part shown while it is full;
  * the shower enclosure is two blocks tall, cut like the refrigerator;
  * the changing station has a folded-up and a folded-down model;
  * the washing machine and dryer light their door windows while they run;
  * bath mats join on all four sides like the bedroom's rugs.

Usage:
    python gen_furniture_bathroom.py              # write everything
    python gen_furniture_bathroom.py --check      # fail if the tree has drifted
    python gen_furniture_bathroom.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import argparse
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
import gen_furniture_bedroom as B  # noqa: E402
import gen_furniture_appliances as A  # noqa: E402

ASSETS = R.ASSETS
SUB = R.SUB
T = R.T
MODEL = R.MODEL
BASE = R.BASE
ROT = R.ROT
el, board, octagon = R.el, R.board, R.octagon
ALL = R.ALL
SIDES = ("north", "south", "east", "west")
NO_DOWN = ("north", "south", "east", "west", "up")
ALL_UV = A.ALL_UV


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def blank(size):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def porcelain(base, seed, size=16):
    """Glazed porcelain: nearly flat, a soft gleam running diagonally across it."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = 1.0 + rng.uniform(-0.008, 0.008)
            if (x + y) % 16 in (3, 4):
                k += 0.025
            px[x, y] = R.shade(base, k) + (255,)
    return img


def water_surface(seed, size=16):
    """A still water surface in a white tub: blue, lighter where the ripples catch the light."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            r = math.sin((x * 0.9 + y * 0.35) * 0.8) + 0.6 * math.sin((y * 1.1 - x * 0.4) * 0.7)
            k = 1.0 + 0.07 * r + rng.uniform(-0.02, 0.02)
            c = R.shade((70, 140, 200), k)
            if r > 1.3:
                c = R.shade((150, 200, 236), 1.0)
            px[x, y] = c + (255,)
    return img


def shower_glass(size=16):
    """Clear toughened glass: faint blue-white with a soft streak of reflection, the same all
    over so that panes cut at a block line or slid into the texture show no seam. Translucent;
    the shower draws in the translucent layer."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            s = (x + y) % 16
            if s in (5, 6):
                c, a = (244, 250, 252), 86
            elif s == 12:
                c, a = (236, 246, 250), 70
            else:
                c, a = (222, 236, 244), 54
            px[x, y] = c + (a,)
    return img


def rose(size=16):
    """A rain shower's rose from below: chrome, with rings of dark nozzles."""
    img = blank(size)
    px = img.load()
    c = (size - 1) / 2.0
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            col = R.shade((210, 214, 218), 1.0 - 0.02 * d)
            if d < 6.6 and x % 2 == 1 and y % 2 == 1:
                col = (60, 62, 66)
            px[x, y] = col + (255,)
    return img


def terry(base, seed, size=16):
    """Terry towelling: a soft pile of loops, each texel a little lighter or darker."""
    img = R.fabric(base, seed, size, heather=0.14)
    px = img.load()
    rng = random.Random(seed + 1)
    for y in range(size):
        for x in range(size):
            if y % 5 == 4 and rng.random() < 0.8:
                px[x, y] = R.shade(px[x, y][:3], 0.93) + (255,)
    return img


def toilet_paper(size=16):
    """A roll of toilet paper: soft white, perforated every quarter block."""
    img = R.flat((246, 246, 242), 601, size, grain=3)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if x % 4 == 0 and y % 2 == 0:
                px[x, y] = (214, 214, 210, 255)
    return img


def radiator(size=16):
    """A column radiator's front: rounded white columns with a shadow between each pair."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = (1.02, 0.96, 0.8)[x % 3] if 0 < y < size - 1 else 1.0
            px[x, y] = R.shade((238, 239, 238), k) + (255,)
    return img


def soap_window(size=16):
    """The soap's sight window: amber liquid, lighter at its top."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = 1.15 if y < 4 else 1.0 - 0.01 * y
            px[x, y] = R.shade((222, 162, 88), k) + (255,)
    return img


def paper_towel(size=16):
    """A paper towel: off-white, embossed in a diamond pattern."""
    img = R.flat((238, 236, 228), 602, size, grain=2)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                px[x, y] = R.shade(px[x, y][:3], 0.95) + (255,)
    return img


def changing_label(size=16):
    """The changing station's sign: a generic pictogram of a baby on a changing table, white on
    blue."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            col = (42, 98, 170)
            if x in (0, size - 1) or y in (0, size - 1):
                col = (232, 236, 240)
            # the table
            elif 11 <= y <= 12 and 2 <= x <= 13:
                col = (246, 246, 246)
            # the baby lying on it: a head and a body
            elif math.hypot(x + 0.5 - 4.5, y + 0.5 - 8.5) < 2.0:
                col = (246, 246, 246)
            elif 7 <= y <= 10 and 6 <= x <= 12 and not (y == 7 and x > 10):
                col = (246, 246, 246)
            # the adult's arm reaching over
            elif 3 <= y <= 4 and 5 <= x <= 12:
                col = (246, 246, 246)
            px[x, y] = col + (255,)
    return img


def porthole(kind, on, size=32):
    """A front loader's round door window on a square: a chrome ring, the gasket, and the drum
    behind the glass -- dark, or lit and running (the washer's water swirling, the dryer's
    clothes tumbling). Outside the ring it is transparent, so the door shows round."""
    rng = random.Random(610 + (5 if kind == "dryer" else 0) + (1 if on else 0))
    img = blank(size)
    px = img.load()
    c = (size - 1) / 2.0
    blobs = [(rng.uniform(6, 26), rng.uniform(12, 28), rng.uniform(2.5, 4.5),
              rng.choice([(196, 62, 58), (62, 104, 176), (236, 226, 196), (86, 150, 96),
                          (226, 180, 70)])) for _ in range(9)]
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            a = math.atan2(y - c, x - c)
            if d > 15.6:
                continue
            if d > 13.0:
                k = 1.05 - 0.12 * math.sin(a + 0.8)
                col = R.shade((206, 210, 214), k)
            elif d > 12.0:
                col = (44, 46, 48)
            else:
                gleam = 8.5 < d < 10.5 and -2.6 < a < -1.6
                if kind == "washer":
                    if on:
                        k = 1.0 + 0.16 * math.sin(3 * a + d * 0.55)
                        col = R.shade((64, 128, 196), k)
                        if y > c + 4 + 1.5 * math.sin(x * 0.5):
                            col = R.shade((120, 170, 220), k)
                        if y > c + 8:
                            col = (232, 240, 246)
                    else:
                        col = R.shade((40, 46, 54), 1.0 + 0.05 * math.sin(a * 5))
                else:
                    if on:
                        col = R.shade((238, 206, 150), 1.0 - 0.02 * d)
                        for bx, by, br, bc in blobs:
                            if math.hypot(x - bx, y - by) < br:
                                col = R.shade(bc, 1.0 - 0.015 * d)
                    else:
                        col = R.shade((34, 34, 38), 1.0 + 0.04 * math.sin(a * 5))
                if gleam:
                    col = R.shade(col, 1.45)
            px[x, y] = col + (255,)
    return img


def wicker(base, seed, size=16):
    """Woven wicker: strands two texels wide over and under each other, lit on top."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            over = ((x // 2) + (y // 2)) % 2 == 0
            k = (1.08 if y % 2 == 0 else 0.9) if over else (1.08 if x % 2 == 0 else 0.86)
            px[x, y] = R.shade(base, k + rng.uniform(-0.03, 0.03)) + (255,)
    return img


def basket_plastic(seed, size=16):
    """A plastic laundry basket's side: white, with a grid of ventilation slots."""
    img = R.flat((240, 240, 236), seed, size, grain=2)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if y % 4 in (1, 2) and x % 4 in (1, 2):
                px[x, y] = (196, 198, 200, 255)
    return img


def ironing_cover(base, dot, seed, size=16):
    """An ironing board's padded cover: a cotton print of small dots on a coloured ground."""
    img = R.fabric(base, seed, size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if (x + 2 * (y // 4)) % 4 == 0 and y % 4 == 0:
                px[x, y] = dot + (255,)
    return img


def clothes(seed, size=16):
    """Laundry piled in a basket, seen from above: folds of a few colours."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    palette = [(62, 96, 160), (230, 228, 220), (178, 56, 56), (90, 90, 96), (216, 190, 120),
               (120, 160, 110)]
    cells = {}
    for y in range(size):
        for x in range(size):
            key = ((x + (y // 5) * 2) // 5, y // 4)
            if key not in cells:
                cells[key] = rng.choice(palette)
            k = 1.0 - 0.1 * ((x + y) % 3 == 0)
            px[x, y] = R.shade(cells[key], k) + (255,)
    return img


# Bath mats: (field, binding, accent)
MAT_COLOURS = {
    "white": ((242, 242, 238), (200, 200, 196), (226, 226, 222)),
    "blue": ((86, 140, 196), (52, 92, 146), (170, 200, 230)),
    "grey": ((150, 152, 156), (104, 106, 110), (196, 198, 200)),
}

TEXTURES = {
    "porcelain": lambda: porcelain((246, 247, 248), 603),
    "porcelain_shade": lambda: porcelain((216, 220, 226), 604),
    "seat_white": lambda: R.flat((240, 240, 238), 605, grain=2),
    "bath_water": lambda: water_surface(606),
    "shower_glass": shower_glass,
    "shower_rose": rose,
    "towel_white": lambda: terry((244, 244, 240), 607),
    "toilet_paper": toilet_paper,
    "radiator_white": radiator,
    "soap_window": soap_window,
    "paper_towel": paper_towel,
    "changing_label": changing_label,
    "plastic_grey": lambda: R.flat((170, 174, 178), 609, grain=2),
    "washer_window_off": lambda: porthole("washer", False),
    "washer_window_on": lambda: porthole("washer", True),
    "dryer_window_off": lambda: porthole("dryer", False),
    "dryer_window_on": lambda: porthole("dryer", True),
    "wicker": lambda: wicker((190, 150, 96), 611),
    "wicker_dark": lambda: wicker((148, 108, 64), 612),
    "basket_white": lambda: basket_plastic(613),
    "basket_white_rim": lambda: R.flat((244, 244, 240), 614, grain=2),
    "ironing_cover_blue": lambda: ironing_cover((70, 118, 186), (236, 240, 246), 615),
    "ironing_cover_grey": lambda: ironing_cover((150, 152, 158), (220, 90, 90), 616),
    "iron_blue": lambda: R.flat((44, 112, 196), 617, grain=3),
    "laundry_clothes": lambda: clothes(618),
    "bottle_pink": lambda: R.flat((232, 150, 178), 619, grain=3),
    "bottle_teal": lambda: R.flat((62, 164, 166), 620, grain=3),
}
for _i, (_cid, (_f, _b, _a)) in enumerate(sorted(MAT_COLOURS.items())):
    TEXTURES["bath_mat_" + _cid] = (lambda f=_f, s=630 + _i: terry(f, s))
    TEXTURES["bath_mat_border_" + _cid] = (lambda f=_f, b=_b, a=_a, s=640 + _i:
                                           B.rug_border(b, a, f, s))

DEFAULT_TEX = dict(B.DEFAULT_TEX)
DEFAULT_TEX.update({k: v for k, v in A.DEFAULT_TEX.items() if k not in DEFAULT_TEX})
DEFAULT_TEX.update({
    "porcelain": T("porcelain"), "porcelain_shade": T("porcelain_shade"),
    "seat": T("seat_white"), "water": T("bath_water"), "glass": T("shower_glass"),
    "rose": T("shower_rose"), "towel": T("towel_white"), "paper": T("toilet_paper"),
    "radiator": T("radiator_white"), "soap": T("soap_window"), "tissue": T("paper_towel"),
    "label": T("changing_label"), "plastic": T("plastic_grey"), "pad": T("changing_pad"),
    "glow": T("washer_window_off"), "wicker": T("wicker"), "wicker_rim": T("wicker_dark"),
    "cover": T("ironing_cover_blue"), "iron": T("iron_blue"), "clothes": T("laundry_clothes"),
    "bottle_a": T("bottle_pink"), "bottle_b": T("bottle_teal"), "brush_a": T("ceramic_blue"),
    "brush_b": T("ceramic_red"),
})


def geometry(specs, particle, dy=0.0, display=None, centre=False):
    """A base model: the elements built where they stand (so their UVs fit there), then moved
    down by dy -- or, for an item, moved so the piece's middle is the block's."""
    built = [R.build(s) for s in specs]
    if centre:
        lo = [min(e["from"][i] for e in built) for i in range(3)]
        hi = [max(e["to"][i] for e in built) for i in range(3)]
        move = [8 - (lo[i] + hi[i]) / 2.0 for i in range(3)]
    else:
        move = [0.0, -dy, 0.0]
    if any(move):
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


# ------------------------------------------------------------------------------------------
# Shared shapes
# ------------------------------------------------------------------------------------------
def rim(x0, x1, z0, z1, ix0, ix1, iz0, iz1, y0, y1, tex, ends=True, back=True):
    """A slab from x0..x1, z0..z1 with a rectangular hole ix0..ix1, iz0..iz1 through it: the
    rim round a bowl, a countertop cut for a basin. `ends` draws its east and west sides (a
    vanity top joins its neighbours', so draws none); `back` its south side."""
    e = ("east", "west") if ends else ()
    s = ("south",) if back else ()
    return [el([x0, y0, z0], [x1, y1, iz0], tex, ("north", "up", "down") + e),
            el([x0, y0, iz1], [x1, y1, z1], tex, ("up", "down") + s + e),
            el([x0, y0, iz0], [ix0, y1, iz1], tex, ("up", "down") + (("west",) if ends else ())),
            el([ix1, y0, iz0], [x1, y1, iz1], tex, ("up", "down") + (("east",) if ends else ()))]


def bowl(x0, x1, z0, z1, y0, y1, tex="porcelain_shade", drain=True):
    """The inside of a square bowl open at the top: four walls facing in and its floor, with a
    strainer in the middle of the floor."""
    out = [el([x0, y0, z0 - 0.25], [x1, y1, z0], tex, ("south",)),
           el([x0, y0, z1], [x1, y1, z1 + 0.25], tex, ("north",)),
           el([x0 - 0.25, y0, z0], [x0, y1, z1], tex, ("east",)),
           el([x1, y0, z0], [x1 + 0.25, y1, z1], tex, ("west",)),
           el([x0, y0 - 0.25, z0], [x1, y0, z1], tex, ("up",))]
    if drain:
        cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
        out.append(ALL_UV(el([cx - 0.75, y0, cz - 0.75], [cx + 0.75, y0 + 0.01, cz + 0.75],
                             "drain", ("up",)), ["up"]))
    return out


def valve(cx, cy, z_wall=16.0):
    """A shower mixer on the wall: a round-cornered plate, a knob and a lever."""
    return [el([cx - 1.75, cy - 1.75, z_wall - 0.75], [cx + 1.75, cy + 1.75, z_wall], "chrome",
               ("north", "east", "west", "up", "down")),
            el([cx - 0.5, cy - 0.5, z_wall - 1.75], [cx + 0.5, cy + 0.5, z_wall - 0.75], "chrome",
               ("north", "east", "west", "up", "down")),
            el([cx - 1.1, cy - 0.15, z_wall - 2.1], [cx + 1.1, cy + 0.15, z_wall - 1.75],
               "chrome")]


# ------------------------------------------------------------------------------------------
# Bathroom
# ------------------------------------------------------------------------------------------
# --- toilet: close-coupled, the seat at 0.42 m, lid up against the cistern ---------------
TOILET = ([el([5.25, 0, 5], [10.75, 0.75, 12.5], "porcelain", NO_DOWN),
           el([5.75, 0.75, 5.25], [10.25, 4.5, 12.25], "porcelain", SIDES),
           el([4.5, 4.5, 2.75], [11.5, 5.5, 12.5], "porcelain", SIDES + ("down",)),
           el([5.25, 4.5, 2], [10.75, 6.25, 2.75], "porcelain", ("north", "east", "west", "up",
                                                                "down"))]
          + rim(4.5, 11.5, 2.75, 12.5, 6, 10, 4, 10.5, 5.5, 6.25, "porcelain")
          + bowl(6, 10, 4, 10.5, 4.9, 6.25, drain=False)
          + [el([6, 5.3, 4], [10, 5.31, 10.5], "water", ("up",)),
             # the seat, a ring round the opening
             el([4.75, 6.25, 2.25], [11.25, 6.75, 3.75], "seat"),
             el([4.75, 6.25, 3.75], [5.9, 6.75, 10.75], "seat"),
             el([10.1, 6.25, 3.75], [11.25, 6.75, 10.75], "seat"),
             el([4.75, 6.25, 10.75], [11.25, 6.75, 11.75], "seat"),
             # the lid, raised against the cistern
             el([4.75, 6.75, 11.5], [11.25, 14.25, 12.1], "seat"),
             # the cistern, its lid and the flush button
             el([3.5, 6.75, 12.25], [12.5, 14.25, 15.75], "porcelain", NO_DOWN + ("down",)),
             el([3.25, 14.25, 12], [12.75, 15, 16], "porcelain"),
             el([7.25, 15, 13.25], [8.75, 15.3, 14.75], "chrome", NO_DOWN)])

# --- toilet paper holder: two posts, a spindle and a roll, 0.6 m up the wall --------------
TP_HOLDER = [el([4.5, 9, 15.5], [5.5, 11.75, 16], "chrome", NO_DOWN + ("down",)),
             el([10.5, 9, 15.5], [11.5, 11.75, 16], "chrome", NO_DOWN + ("down",)),
             el([4.5, 10, 12.5], [5.25, 10.75, 15.5], "chrome"),
             el([10.75, 10, 12.5], [11.5, 10.75, 15.5], "chrome"),
             el([5.25, 10.2, 13.2], [10.75, 10.55, 13.55], "chrome", ("north", "south", "up",
                                                                     "down")),
             el([5.5, 8.4, 11.9], [10.5, 12.35, 14.85], "paper"),
             el([5.5, 8.0, 12.35], [10.5, 12.75, 14.4], "paper", ("north", "south", "up",
                                                                 "down")),
             el([5.5, 8.85, 11.45], [10.5, 11.9, 15.3], "paper", ("north", "south", "up",
                                                                 "down"))]

# --- pedestal sink: a column under a basin, its rim at 0.9 m, the tap behind the bowl -------
PEDESTAL = ([el([6, 0, 8.5], [10, 1, 14], "porcelain", NO_DOWN + ("down",)),
             el([6.25, 1, 9], [9.75, 10.5, 13.5], "porcelain", SIDES),
             el([2.5, 10.5, 4], [13.5, 13.75, 16], "porcelain", ("north", "east", "west", "down",
                                                                "south"))]
            + rim(2, 14, 3.5, 16, 4, 12, 5, 12.25, 13.75, 14.5, "porcelain")
            + bowl(4, 12, 5, 12.25, 11, 14.5)
            + K.TAP)

# --- bathroom vanity: a cabinet 0.6 m deep, its stone top at 0.91 m with a basin in it -------
VZ = 4.5  # the face of its doors
VANITY_BODY = ([el([0, 0, 5.5], [16, 1.5, 16], "edge", ("north",)),
                board([0, 1.5, 5], [16, 13.75, 16], grain="v", faces=("north", "south"))]
               + K.shaker(0.5, 7.9, 2, 13.25, VZ) + K.shaker(8.1, 15.5, 2, 13.25, VZ)
               + K.bar_v(6.7, 9.5, 12.5, VZ) + K.bar_v(8.9, 9.5, 12.5, VZ)
               + rim(0, 16, 3.5, 16, 3.5, 12.5, 5, 12, 13.75, 14.5, "counter", ends=False)
               + bowl(3.5, 12.5, 5, 12, 11, 14.5, tex="porcelain")
               + K.TAP)
VANITY_END = [board([0, 0, 4.5], [0.5, 13.75, 16], grain="v", faces=("west", "north")),
              el([0, 13.75, 3.5], [0.01, 14.5, 16], "counter", ("west",))]

# --- mirror cabinet: 0.88 x 0.81 m on the wall, two mirrored doors -------------------------
MIRROR_CABINET = ([board([1, 2, 12.75], [15, 15, 16], grain="v",
                         faces=("south", "east", "west", "up", "down"))]
                  + [el([x0, 2.25, 12.25], [x1, 14.75, 12.75], "edge",
                        ("east", "west", "up", "down")) for x0, x1 in ((1.25, 7.95), (8.05, 14.75))]
                  + [el([x0, 2.25, 12.25], [x1, 14.75, 12.26], "mirror", ("north",))
                     for x0, x1 in ((1.25, 7.95), (8.05, 14.75))]
                  + [board([1, 1.5, 12.25], [15, 2, 16])])

# --- bathtub: 1.94 x 0.84 m, its rim at 0.56 m, drawn whole across two blocks (+x) --------
TUB_BODY = ([el([0.5, 0, 2.5], [31.5, 8.25, 16], "porcelain", ("north", "east", "west",
                                                                "down"))]
            + rim(0.5, 31.5, 2.5, 16, 2.5, 29.5, 4.5, 14.25, 8.25, 9, "porcelain")
            + bowl(2.5, 29.5, 4.5, 14.25, 1.5, 8.25, drain=False)
            + [ALL_UV(el([4, 1.5, 8.5], [5.5, 1.51, 10], "drain", ("up",)), ["up"]),
               # the head end's backrest, sloping up to the rim
               el([27, 1.5, 4.5], [29.5, 4, 14.25], "porcelain_shade", ("west", "up")),
               # the mixer on the back rim at the tap end: pillar, spout and two handles
               el([4, 9, 14.5], [5, 11, 15.5], "chrome", NO_DOWN),
               el([4, 10.4, 11.5], [5, 11.15, 14.5], "chrome", ("north", "east", "west", "up",
                                                              "down")),
               el([4, 9.75, 11.5], [5, 10.4, 12.25], "chrome", ("north", "south", "east", "west",
                                                               "down")),
               el([2.5, 9, 14.75], [4, 9.75, 15.25], "chrome", NO_DOWN),
               el([5, 9, 14.75], [6.5, 9.75, 15.25], "chrome", NO_DOWN)])
TUB_WATER = [el([2.5, 6.5, 4.5], [29.5, 6.6, 14.25], "water", ("up",))]
TUB_CELLS = [(0, 0, 0), (1, 0, 0)]

# --- shower enclosure: a tray, glass on both sides and beside the entry, a rain head ---------
SHOWER = ([el([0.5, 0, 0.5], [15.5, 1.5, 15.5], "porcelain", NO_DOWN + ("down",)),
           el([2, 1.5, 2], [14, 1.51, 14], "porcelain_shade", ("up",)),
           ALL_UV(el([7.25, 1.52, 7.25], [8.75, 1.53, 8.75], "drain", ("up",)), ["up"]),
           # the glass: the two sides, and a fixed panel beside the open entry
           el([0.5, 1.5, 1.1], [1, 30, 15.5], "glass"),
           el([15, 1.5, 1.1], [15.5, 30, 15.5], "glass"),
           el([1.1, 1.5, 0.5], [5, 30, 1], "glass"),
           # the chrome frame: rails along the tops, posts at the front corners and the edge
           el([0.4, 30, 0.4], [1.1, 30.5, 15.6], "chrome"),
           el([14.9, 30, 0.4], [15.6, 30.5, 15.6], "chrome"),
           el([1.1, 30, 0.4], [5.6, 30.5, 1.1], "chrome"),
           el([0.4, 1.5, 0.4], [1.1, 30, 1.1], "chrome", SIDES),
           el([14.9, 1.5, 0.4], [15.6, 30, 1.1], "chrome", SIDES),
           el([5, 1.5, 0.4], [5.6, 30, 1.1], "chrome", SIDES),
           # the riser, the arm and the rain head on the wall behind
           el([7.6, 4, 15.25], [8.4, 26, 15.75], "chrome", ("north", "east", "west", "up")),
           el([7.6, 26, 11.75], [8.4, 26.75, 15.75], "chrome"),
           el([5.5, 25.25, 8.5], [10.5, 26, 13.5], "chrome", NO_DOWN),
           ALL_UV(el([5.5, 25.24, 8.5], [10.5, 25.25, 13.5], "rose", ("down",)), ["down"])]
          + valve(8, 16))
SHOWER_ROSE = (8, 25.25 - 16, 11)

# --- shower head: an arm from the wall and a round rain head near the top of the block, the
# mixer valve lower down -----------------------------------------------------------------
SHOWER_HEAD = ([el([7.6, 12.9, 12], [8.4, 13.7, 16], "chrome", ("north", "south", "east", "west",
                                                                  "up", "down")),
                el([7.6, 12.25, 11.25], [8.4, 13.7, 12], "chrome", SIDES + ("up",)),
                el([5.5, 11.5, 8], [10.5, 12.25, 13], "chrome", NO_DOWN),
                ALL_UV(el([5.5, 11.49, 8], [10.5, 11.5, 13], "rose", ("down",)), ["down"])]
               + valve(8, 4.75))
SHOWER_HEAD_ROSE = (8, 11.5, 10.5)

# --- towel rail: a bar on two brackets with a towel over it, 0.8 m up the wall ------------
TOWEL_RAIL = [el([2, 12, 14.5], [3, 13, 16], "chrome"),
              el([13, 12, 14.5], [14, 13, 16], "chrome"),
              el([2, 12.2, 13.7], [14, 12.8, 14.5], "chrome", ("north", "south", "up", "down")),
              el([3.5, 6, 13.1], [12.5, 13.2, 13.7], "towel", ("north", "east", "west", "down")),
              el([3.5, 8, 14.5], [12.5, 13.2, 15], "towel", ("south", "east", "west", "down")),
              el([3.5, 12.8, 13.1], [12.5, 13.3, 15], "towel", ("up", "east", "west"))]

# --- heated towel rail: a ladder of chrome rungs, a towel over the top rung ---------------
HEATED_RAIL = ([el([2, 1, 14], [3, 15.5, 15], "chrome"),
                el([13, 1, 14], [14, 15.5, 15], "chrome")]
               + [el([3, y, 14.1], [13, y + 0.6, 14.9], "chrome", ("north", "south", "up",
                                                                  "down"))
                  for y in (2.5, 5, 7.5, 10, 12.5, 14.75)]
               + [el([x, y, 15], [x + 0.5, y + 0.5, 16], "chrome", SIDES)
                  for x in (2.25, 13.25) for y in (4, 13)]
               + [el([1.25, 1.25, 14.1], [2, 2, 14.9], "chrome", NO_DOWN),
                  el([14, 1.25, 14.1], [14.75, 2, 14.9], "chrome", NO_DOWN),
                  el([4, 10, 13.4], [12, 15.8, 14.1], "towel", ("north", "east", "west", "down")),
                  el([4, 15.35, 13.4], [12, 15.9, 15.2], "towel", ("up", "east", "west")),
                  el([4, 12, 14.9], [12, 15.8, 15.2], "towel", ("south", "east", "west",
                                                               "down"))])

# --- bathroom radiator: a column radiator on the wall, valves at its feet -------------------
RADIATOR = [el([1, 2, 13], [15, 14, 15.25], "radiator"),
            el([1.75, 0, 13.75], [2.5, 2, 14.5], "radiator", SIDES),
            el([13.5, 0, 13.75], [14.25, 2, 14.5], "radiator", SIDES),
            el([0.25, 2.5, 13.6], [1, 3.5, 14.6], "chrome", NO_DOWN + ("down",)),
            el([15, 2.5, 13.6], [15.75, 3.5, 14.6], "chrome", NO_DOWN + ("down",)),
            el([0.35, 0, 13.85], [0.9, 2.5, 14.35], "chrome", SIDES),
            el([15.1, 0, 13.85], [15.65, 2.5, 14.35], "chrome", SIDES)]

# --- wastebasket: a pedal bin with a lid ---------------------------------------------------
WASTEBASKET = (octagon(8, 8.5, 3.25, 0.25, 7.5, "shell", caps=("down",))
               + octagon(8, 8.5, 3.35, 0, 0.25, "trim", caps=("down",))
               + octagon(8, 8.5, 3.4, 7.5, 8.25, "shell")
               + [el([6.5, 0.4, 4.4], [9.5, 0.9, 5.4], "trim"),
                  el([7, 7.5, 11.9], [9, 8.5, 12.3], "trim", ("south", "east", "west", "up"))])

# --- toiletries tray: a porcelain tray with a soap pump, a tumbler of toothbrushes, a jar and
# a scent bottle ----------------------------------------------------------------------------
TOILETRIES = ([el([3.5, 0, 5], [12.5, 0.5, 11], "porcelain"),
               el([4.5, 0.5, 6], [6.5, 4.5, 8], "bottle_a"),
               el([5.25, 4.5, 6.75], [5.75, 5.5, 7.25], "chrome", SIDES),
               el([5.25, 5.25, 5.75], [5.75, 5.5, 7.25], "chrome", NO_DOWN + ("down",))]
              + octagon(8.5, 8, 1.1, 0.5, 3.25, "porcelain_shade")
              + [el([8.1, 3, 7.6], [8.5, 5.75, 8], "brush_a", SIDES + ("up",)),
                 el([8.6, 3, 8.1], [9, 5.4, 8.5], "brush_b", SIDES + ("up",))]
              + octagon(10.75, 9.5, 1.2, 0.5, 2.25, "bottle_b", caps=())
              + octagon(10.75, 9.5, 1.3, 2.25, 2.75, "chrome", caps=("up", "down"))
              + [el([10, 0.5, 5.75], [11.5, 2.75, 7], "mirror"),
                 el([10.5, 2.75, 6.1], [11, 3.5, 6.6], "brass", NO_DOWN)])

# --- toilet brush: a stainless pot with the brush's handle standing out of it --------------
TOILET_BRUSH = (octagon(8, 8, 1.6, 0, 4, "shell", caps=("down",))
                + octagon(8, 8, 1.75, 3.6, 4.1, "trim")
                + [el([7.6, 4.1, 7.6], [8.4, 10, 8.4], "handle", SIDES),
                   el([7.35, 10, 7.35], [8.65, 10.6, 8.65], "handle")])

# ------------------------------------------------------------------------------------------
# Commercial restroom
# ------------------------------------------------------------------------------------------
# --- wall-hung urinal: its lip at 0.6 m, a flush valve on top ----------------------------
URINAL = [el([4.5, 3, 11], [11.5, 15, 16], "porcelain", ALL, {"north": "porcelain_shade"}),
          el([4.5, 3, 7.5], [11.5, 6.5, 11], "porcelain", ALL, {"up": "porcelain_shade"}),
          el([4.5, 6.5, 7.5], [5.5, 15, 11], "porcelain", ("north", "east", "west", "up"),
             {"east": "porcelain_shade"}),
          el([10.5, 6.5, 7.5], [11.5, 15, 11], "porcelain", ("north", "east", "west", "up"),
             {"west": "porcelain_shade"}),
          el([5.5, 14, 7.5], [10.5, 15, 11], "porcelain", ("north", "up", "down")),
          ALL_UV(el([7.25, 6.51, 8.75], [8.75, 6.52, 10.25], "drain", ("up",)), ["up"]),
          el([7.5, 15, 12.5], [8.5, 16, 15.5], "chrome", SIDES + ("up",)),
          el([8.5, 15.25, 12.75], [10, 15.75, 13.25], "chrome")]

# --- soap dispenser: a white box with a sight window and a push bar -----------------------
SOAP_DISPENSER = [el([6, 8, 13], [10, 13.5, 16], "shell"),
                  ALL_UV(el([6.75, 9.75, 12.95], [9.25, 12.25, 13], "soap", ("north",)),
                         ["north"]),
                  el([6.25, 7.5, 12.5], [9.75, 8.5, 15.5], "handle"),
                  el([7.6, 7, 14], [8.4, 7.5, 14.8], "handle", SIDES + ("down",))]

# --- paper towel dispenser: a stainless cabinet, a towel showing at the bottom ---------------
TOWEL_DISPENSER = [el([3, 5.5, 12], [13, 15, 16], "shell"),
                   el([3, 8.25, 11.95], [13, 8.35, 12], "trim", ("north",)),
                   ALL_UV(el([6.5, 10.5, 11.95], [9.5, 13, 12], "tissue", ("north",)), ["north"]),
                   el([5, 4, 12.6], [11, 5.5, 12.7], "tissue", ("north", "south", "east", "west",
                                                           "down"))]

# --- grab bar: a stainless bar on two flanges, 0.85 m up the wall -------------------------
GRAB_BAR = [el([1, 12.9, 13.25], [15, 13.9, 14.25], "shell"),
            el([1.25, 12.4, 14.25], [2.75, 14.4, 16], "shell", NO_DOWN + ("down",)),
            el([13.25, 12.4, 14.25], [14.75, 14.4, 16], "shell", NO_DOWN + ("down",))]

# --- baby changing station: folded up against the wall, or down as a bed with a pad; it is
# meant for the block above the floor's, so the bed is at 1.1 m --------------------------------
CHANGING_CLOSED = [el([1, 0.5, 12.75], [15, 15.5, 16], "plastic"),
                   ALL_UV(el([4.5, 5, 12.7], [11.5, 12, 12.75], "label", ("north",)), ["north"]),
                   el([1.5, 0.25, 12.5], [14.5, 1.25, 13], "plastic", ("north", "east", "west",
                                                                      "down"))]
CHANGING_OPEN = [el([1, 0.5, 14], [15, 3.5, 16], "plastic"),
                 el([1, 1, 2.5], [15, 2, 14], "plastic"),
                 el([1, 2, 2.5], [15, 3.5, 3.5], "plastic", ("north", "south", "east", "west",
                                                           "up")),
                 el([1, 2, 3.5], [2, 3.5, 14], "plastic", ("south", "east", "west", "up")),
                 el([14, 2, 3.5], [15, 3.5, 14], "plastic", ("south", "east", "west", "up")),
                 el([2, 2, 3.5], [14, 2.75, 14], "pad", ("up", "north")),
                 el([7.25, 2.75, 6], [8.75, 2.8, 11], "label", ("up",))]

# ------------------------------------------------------------------------------------------
# Laundry
# ------------------------------------------------------------------------------------------
# --- washing machine and dryer: 0.6 m front loaders drawn to the block's width, their tops at
# 0.91 m, a round door with its window lit while they run ----------------------------------
def front_loader(dryer):
    out = [el([0.75, 0, 1.75], [15.25, 0.5, 15.5], "rubber", ("north", "east", "west")),
           el([0.25, 0.5, 1.25], [15.75, 14.5, 16], "shell", NO_DOWN + ("down",)),
           ALL_UV(el([1, 12.25, 1.2], [15, 14.25, 1.25], "control", ("north",)), ["north"]),
           el([1.25, 12.35, 0.9], [5, 14.15, 1.25], "trim", ("north", "east", "west", "up",
                                                             "down")),
           el([10.75, 12.6, 0.75], [12, 13.85, 1.25], "handle", ("north", "east", "west", "up",
                                                                 "down")),
           # the door, its window a cutout circle on a square in front of it
           el([2.5, 1.25, 0.75], [13.5, 12, 1.25], "door", ("north", "east", "west", "up",
                                                            "down")),
           ALL_UV(el([3, 1.75, 0.7], [13, 11.75, 0.75], "glow", ("north",)), ["north"]),
           el([12.4, 5.25, 0.35], [13.1, 8.25, 0.75], "handle")]
    if dryer:
        # the lint filter's slot in the top, and a vent grille low on the front
        out += [el([4, 14.5, 10], [12, 14.55, 11], "rubber", ("up",)),
                ALL_UV(el([10, 0.75, 1.2], [14.5, 1.1, 1.25], "filter", ("north",)), ["north"])]
    return out


WASHER = front_loader(False)
DRYER = front_loader(True)

# --- steam iron: soleplate, body and handle, a water window at the back ------------------
IRON = [el([5.75, 0, 4.5], [10.25, 0.5, 11.5], "chrome"),
        el([6.5, 0, 3.5], [9.5, 0.5, 4.5], "chrome", ("north", "east", "west", "up", "down")),
        el([7.25, 0, 2.75], [8.75, 0.5, 3.5], "chrome", ("north", "east", "west", "up", "down")),
        el([6, 0.5, 5], [10, 2.25, 11.25], "iron"),
        el([6.75, 0.5, 4], [9.25, 2, 5], "iron", ("north", "east", "west", "up")),
        el([7.25, 0.5, 3.25], [8.75, 1.5, 4], "iron", ("north", "east", "west", "up")),
        ALL_UV(el([6.5, 0.9, 11.25], [9.5, 2, 11.3], "water", ("south",)), ["south"]),
        el([7.25, 2.25, 6], [8.75, 3, 7], "iron", ("north", "south", "east", "west")),
        el([7.25, 2.25, 10], [8.75, 3.75, 11], "iron", ("north", "south", "east", "west")),
        el([7.25, 3, 6], [8.75, 3.75, 10], "iron", ("north", "east", "west", "up", "down")),
        el([7.5, 2.25, 7.5], [8.5, 2.45, 8.5], "trim", NO_DOWN)]
IRON_STEAM = [8, 0.5, 3.25]

# --- ironing board: 1.4 m long along the block, its top at 0.91 m, on crossed legs --------
_LEG = 14.6
IRONING_BOARD = ([el([-2, 13.75, 5], [18.5, 14.5, 11], "cover", ("up", "north", "south", "east")),
                  el([-3.5, 13.75, 6], [-2, 14.5, 10], "cover", ("up", "north", "south", "west")),
                  el([-4.5, 13.75, 7], [-3.5, 14.5, 9], "cover", ("up", "north", "south", "west")),
                  el([-4.5, 13.5, 5], [18.5, 13.75, 11], "metal", ("down", "north", "south")),
                  # the iron rest at the square end
                  el([18.5, 13.75, 5.5], [20.5, 14.25, 10.5], "metal"),
                  # feet: a bar across under each pair of legs
                  el([4.25, 0, 5.75], [5.75, 0.5, 10.25], "rubber"),
                  el([10.25, 0, 5.75], [11.75, 0.5, 10.25], "rubber")]
                 + [el([7.6, 6.75 - _LEG / 2, z], [8.4, 6.75 + _LEG / 2, z + 0.8], "metal",
                       SIDES, rot=("z", a, [8, 6.75, z + 0.4]))
                    for z in (6, 9.2) for a in (22.5, -22.5)])

# --- laundry basket: a basket of folded laundry, handles in its ends ---------------------
BASKET = ([el([3, 0, 4], [13, 8, 12], "wicker", SIDES + ("down",))]
          + rim(2.75, 13.25, 3.75, 12.25, 3.5, 12.5, 4.5, 11.5, 8, 8.75, "wicker_rim")
          + bowl(3.5, 12.5, 4.5, 11.5, 6.5, 8.75, tex="wicker", drain=False)
          + [el([3.5, 7.25, 4.5], [12.5, 7.26, 11.5], "clothes", ("up",)),
             el([5, 5.5, 3.25], [8, 8.9, 3.75], "towel", ("north", "east", "west", "down")),
             el([5, 8.75, 3.25], [8, 8.95, 4.5], "towel", ("up", "east", "west")),
             el([2.6, 5.75, 6.5], [3, 7.25, 9.5], "wicker_rim", ("west", "up", "down")),
             el([13, 5.75, 6.5], [13.4, 7.25, 9.5], "wicker_rim", ("east", "up", "down"))])

# --- laundry tub: a deep plastic utility sink on four legs, its tap on the back rim --------
LAUNDRY_TUB = ([el([x, 0, z], [x + 1, 8.5, z + 1], "shell", SIDES)
                for x in (1.5, 13.5) for z in (3.5, 14)]
               + [el([1, 8.5, 3], [15, 13.75, 16], "shell", ("north", "south", "east", "west",
                                                            "down"))]
               + rim(1, 15, 3, 16, 2.5, 13.5, 4.5, 12.75, 13.75, 14.5, "shell")
               + bowl(2.5, 13.5, 4.5, 12.75, 9.25, 14.5, tex="porcelain_shade")
               + B.shift(K.TAP, 0, 0, 0.75))


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
def fin(fid, tex, *names):
    return (fid, dict(tex)) + names


# A finish always names at least one texture: a blockstate or part model whose "textures" is
# empty makes the game fail to resolve the base model's own textures (it logs an "upward
# reference" and draws the missing texture).
WHITE = fin("white", {"porcelain": T("porcelain")}, "White", "Weiß", "blanco", "vit")
CHROME = fin("chrome", {"chrome": T("chrome")}, "Chrome", "Chrom", "cromado", "krom")
GREY = fin("grey", {"plastic": T("plastic_grey")}, "Grey", "Grau", "gris", "grå")
STAINLESS = K.STAINLESS_FINISH
APPLIANCE_WHITE = K.WHITE_FINISH
WOODS = K.WOODS
MATS = [("white", {}, "White", "Weiß", "blanca", "vit"),
        ("blue", {}, "Blue", "Blau", "azul", "blå"),
        ("grey", {}, "Grey", "Grau", "gris", "grå")]
MATS = [(cid, {"rug": T("bath_mat_" + cid), "rug_border": T("bath_mat_border_" + cid)}, *n)
        for cid, _t, *n in MATS]

DOORS = "FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE"
PIECE_GLASS = "Material.GLASS, SoundType.GLASS, BlockRenderLayer.SOLID"
PIECE_METAL = "Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID"


def jbox(box):
    return ", ".join(R._num(v) for v in box)


def box(specs):
    return A.box_of(specs)


# Every piece: piece -> dict(kind, geo, finishes, names, java (a format of reg, box), ...)
# kinds: single (one model, turned by facing), storage (a storage run piece drawn whole),
# vanity (a run: body, left, right), tub (two cells), shower (two halves), head (single with
# on), folding (open/closed), laundry (glowing window), counter (rests), mat (a rug)
PIECES = [
    ("toilet", dict(kind="single", geo=TOILET, particle="porcelain", finishes=[WHITE],
                    names=("Toilet", "Toilette", "Inodoro", "Toalettstol"),
                    java='new BlockToilet("%s", new int[]{%s})')),
    ("toilet_paper_holder", dict(kind="single", geo=TP_HOLDER, particle="chrome",
                                 finishes=[CHROME],
                                 names=("Toilet Paper Holder", "Toilettenpapierhalter",
                                        "Portarrollos", "Toalettpappershållare"),
                                 java='new BlockBathroomFixture("%s", new int[]{%s}, '
                                      'FixtureMaterial.METAL)')),
    ("pedestal_sink", dict(kind="single", geo=PEDESTAL, particle="porcelain", finishes=[WHITE],
                           names=("Pedestal Sink", "Standwaschbecken", "Lavabo de pie",
                                  "Piedestalhandfat"),
                           java='new BlockBasin("%s", new int[]{%s}, FixtureMaterial.PORCELAIN)')),
    ("bathroom_vanity", dict(kind="vanity", finishes=WOODS,
                             names=("Bathroom Vanity", "Waschtischunterschrank",
                                    "Mueble de lavabo", "Tvättställsskåp"),
                             java='new BlockBathroomVanity("%s")')),
    ("mirror_cabinet", dict(kind="storage", geo=MIRROR_CABINET, particle="wood",
                            finishes=[(f, dict(t), *n) for f, t, *n in R.WOODS],
                            names=("Mirror Cabinet", "Spiegelschrank", "Armario con espejo",
                                   "Spegelskåp"),
                            java='new BlockResidentialStorage("%%s", new int[]{%%s}, 9, %s)'
                                 % DOORS)),
    ("bathtub", dict(kind="tub", finishes=[(WHITE[0], dict(WHITE[1], water=T("bath_water")), *WHITE[2:])],
                     names=("Bathtub", "Badewanne", "Bañera", "Badkar"),
                     java='new BlockBathtub("%s")')),
    ("shower_enclosure", dict(kind="shower", finishes=[CHROME],
                              names=("Shower Enclosure", "Duschkabine", "Mampara de ducha",
                                     "Duschkabin"),
                              java='new BlockShower("%s", new int[]{0, 0, 0, 16, 31, 16})')),
    ("shower_head", dict(kind="head", geo=SHOWER_HEAD, particle="chrome", finishes=[CHROME],
                         names=("Shower Head", "Duschkopf", "Rociador de ducha",
                                "Duschmunstycke"),
                         java='new BlockShowerHead("%s", new int[]{%s})')),
    ("towel_rail", dict(kind="single", geo=TOWEL_RAIL, particle="chrome", finishes=[CHROME],
                        names=("Towel Rail", "Handtuchstange", "Toallero", "Handduksstång"),
                        java='new BlockBathroomFixture("%s", new int[]{%s}, '
                             'FixtureMaterial.METAL)')),
    ("heated_towel_rail", dict(kind="single", geo=HEATED_RAIL, particle="chrome",
                               finishes=[CHROME],
                               names=("Heated Towel Rail", "Handtuchheizkörper",
                                      "Toallero eléctrico", "Handdukstork"),
                               java='new BlockBathroomFixture("%s", new int[]{%s}, '
                                    'FixtureMaterial.METAL)')),
    ("bathroom_radiator", dict(kind="single", geo=RADIATOR, particle="radiator",
                               finishes=[fin("white", {"radiator": T("radiator_white")}, "White",
                                             "Weiß", "blanco", "vit")],
                               names=("Bathroom Radiator", "Badheizkörper", "Radiador de baño",
                                      "Badrumselement"),
                               java='new BlockBathroomFixture("%s", new int[]{%s}, '
                                    'FixtureMaterial.METAL)')),
    ("wastebasket", dict(kind="storage", geo=WASTEBASKET, particle="shell",
                         finishes=[STAINLESS, APPLIANCE_WHITE],
                         names=("Wastebasket", "Abfalleimer", "Papelera", "Papperskorg"),
                         java='new BlockResidentialStorage("%s", new int[]{%s}, 9, '
                              'FurnishingsSounds.JAR_LID, null)')),
    ("toiletries_tray", dict(kind="counter", geo=TOILETRIES, particle="porcelain",
                             finishes=[WHITE],
                             names=("Toiletries Tray", "Kosmetiktablett",
                                    "Bandeja de aseo", "Bricka med hygienartiklar"),
                             java='new BlockCounterPiece("%%s", new int[]{%%s}, %s)'
                                  % PIECE_GLASS)),
    ("toilet_brush", dict(kind="counter", geo=TOILET_BRUSH, particle="shell",
                          finishes=[STAINLESS],
                          names=("Toilet Brush", "Toilettenbürste", "Escobilla de baño",
                                 "Toalettborste"),
                          java='new BlockCounterPiece("%%s", new int[]{%%s}, %s)'
                               % PIECE_METAL)),
    ("bath_mat", dict(kind="mat", finishes=MATS,
                      names=("Bath Mat", "Badematte", "Alfombrilla de baño", "Badrumsmatta"),
                      java='new BlockRug("%s")')),
    # ---- commercial restroom ----
    ("urinal", dict(kind="single", geo=URINAL, particle="porcelain", finishes=[WHITE],
                    names=("Urinal", "Urinal", "Urinario", "Urinoar"),
                    java='new BlockBathroomFixture("%s", new int[]{%s}, '
                         'FixtureMaterial.PORCELAIN, FurnishingsSounds.TOILET_FLUSH, 1.3F)')),
    ("soap_dispenser", dict(kind="single", geo=SOAP_DISPENSER, particle="shell",
                            finishes=[APPLIANCE_WHITE],
                            names=("Soap Dispenser", "Seifenspender", "Dispensador de jabón",
                                   "Tvålautomat"),
                            java='new BlockBathroomFixture("%s", new int[]{%s}, '
                                 'FixtureMaterial.PLASTIC)')),
    ("paper_towel_dispenser", dict(kind="single", geo=TOWEL_DISPENSER, particle="shell",
                                   finishes=[STAINLESS],
                                   names=("Paper Towel Dispenser", "Papierhandtuchspender",
                                          "Dispensador de toallas de papel",
                                          "Pappershanddukshållare"),
                                   java='new BlockBathroomFixture("%s", new int[]{%s}, '
                                        'FixtureMaterial.METAL)')),
    ("grab_bar", dict(kind="single", geo=GRAB_BAR, particle="shell", finishes=[STAINLESS],
                      names=("Grab Bar", "Haltegriff", "Barra de apoyo", "Stödhandtag"),
                      java='new BlockBathroomFixture("%s", new int[]{%s}, '
                           'FixtureMaterial.METAL)')),
    ("baby_changing_station", dict(kind="folding", finishes=[GREY], particle="plastic",
                                   names=("Baby Changing Station", "Wickelstation",
                                          "Cambiador de bebés", "Fällbart skötbord"),
                                   java='new BlockFoldingFixture("%s", new int[]{%s}, '
                                        'new int[]{%s}, FixtureMaterial.PLASTIC)')),
    # ---- laundry ----
    ("washing_machine", dict(kind="laundry", geo=WASHER, glow="washer",
                             finishes=[APPLIANCE_WHITE, STAINLESS],
                             names=("Washing Machine", "Waschmaschine", "Lavadora",
                                    "Tvättmaskin"),
                             java='new BlockLaundryAppliance("%s", new int[]{%s}, '
                                  'LaundryAppliances.WASHING_MACHINE)')),
    ("dryer", dict(kind="laundry", geo=DRYER, glow="dryer",
                   finishes=[APPLIANCE_WHITE, STAINLESS],
                   names=("Tumble Dryer", "Wäschetrockner", "Secadora", "Torktumlare"),
                   java='new BlockLaundryAppliance("%s", new int[]{%s}, LaundryAppliances.DRYER)')),
    ("steam_iron", dict(kind="counter", geo=IRON, particle="iron",
                        finishes=[fin("blue", {"iron": T("iron_blue")}, "Blue", "Blau", "azul", "blå")],
                        names=("Steam Iron", "Dampfbügeleisen", "Plancha de vapor",
                               "Ångstrykjärn"),
                        java='new BlockCounterPiece("%%s", new int[]{%%s}, %s, '
                             'FurnishingsSounds.IRON_STEAM, 1.0F, new double[]{%s})'
                             % (PIECE_METAL, jbox(IRON_STEAM)))),
    ("ironing_board", dict(kind="single", geo=IRONING_BOARD, particle="cover", big=True,
                           finishes=[fin("blue", {"cover": T("ironing_cover_blue")}, "Blue", "Blau", "azul",
                                         "blå"),
                                     fin("grey", {"cover": T("ironing_cover_grey")}, "Grey",
                                         "Grau", "gris", "grå")],
                           names=("Ironing Board", "Bügelbrett", "Tabla de planchar",
                                  "Strykbräda"),
                           java='new BlockBathroomFixture("%s", new int[]{%s}, '
                                'FixtureMaterial.METAL)')),
    ("laundry_basket", dict(kind="storage", geo=BASKET, particle="wicker",
                            finishes=[fin("wicker", {"wicker": T("wicker"), "wicker_rim": T("wicker_dark")},
                                          "Wicker", "Korbgeflecht", "mimbre",
                                          "flätad"),
                                      fin("white", {"wicker": T("basket_white"),
                                                    "wicker_rim": T("basket_white_rim")},
                                          "White", "Weiß", "blanco", "vit")],
                            names=("Laundry Basket", "Wäschekorb", "Cesto de la ropa",
                                   "Tvättkorg"),
                            java='new BlockResidentialStorage("%s", new int[]{%s}, 9)')),
    ("laundry_tub", dict(kind="single", geo=LAUNDRY_TUB, particle="shell",
                         finishes=[fin("white", {"shell": T("appliance_white")}, "White",
                                       "Weiß", "blanca", "vit")],
                         names=("Laundry Tub", "Ausgussbecken", "Pila de lavar", "Tvättho"),
                         java='new BlockBasin("%s", new int[]{%s}, FixtureMaterial.PLASTIC)')),
]
PIECE = dict(PIECES)
CHANGING_BOXES = ([1, 0, 12, 15, 16, 16], [1, 0, 2, 15, 4, 16])
GLOW = {"washer": (T("washer_window_off"), T("washer_window_on")),
        "dryer": (T("dryer_window_off"), T("dryer_window_on"))}

# Where each group starts in the tab, for the fragments' comments.
GROUPS = {"toilet": "Bathroom", "urinal": "Commercial restroom", "washing_machine": "Laundry"}


def names_with(names, fnames):
    return ["%s (%s)" % (n, f) for n, f in zip(names, fnames)]


def java_for(piece, spec, reg):
    j = spec["java"]
    kind = spec["kind"]
    if kind in ("vanity", "tub", "shower", "mat"):
        return j % reg
    if kind == "folding":
        return j % (reg, jbox(CHANGING_BOXES[0]), jbox(CHANGING_BOXES[1]))
    return j % (reg, jbox(box(spec["geo"])))


def entries():
    """Every block: (registry, piece, finish textures, names, java)."""
    out = []
    for piece, spec in PIECES:
        for fid, ftex, *fnames in spec["finishes"]:
            reg = "%s_%s" % (piece, fid)
            out.append((reg, piece, dict(ftex), names_with(spec["names"], fnames),
                        java_for(piece, spec, reg)))
    return out


# ------------------------------------------------------------------------------------------
# Models and blockstates
# ------------------------------------------------------------------------------------------
def full_uv(specs):
    """Maps the whole texture onto every strainer again after a cut or a move, which fits
    UVs afresh."""
    for s in specs:
        if s["tex"] == "drain":
            ALL_UV(s, s["faces"])
    return specs


TUB_ITEM = full_uv(B.centred(TUB_BODY))
SHOWER_LOWER, SHOWER_UPPER = K.split_y(SHOWER)
VANITY = K.run_piece(VANITY_BODY, VANITY_END, None)


def base_models():
    """Every base geometry model: (name, geometry json)."""
    out = []
    for piece, spec in PIECES:
        kind = spec["kind"]
        if kind in ("single", "head"):
            display = B.big_display(B.centred(spec["geo"])) if spec.get("big") else None
            out.append((piece, geometry(spec["geo"], spec["particle"], display=display)))
        elif kind == "storage":
            out.append(("%s_body" % piece, geometry(spec["geo"], spec["particle"])))
            out.append(("%s_item" % piece, geometry(spec["geo"], spec["particle"])))
        elif kind == "counter":
            geo, particle = spec["geo"], spec["particle"]
            for rest, drop in A.RESTS:
                out.append(("%s_%s" % (piece, rest), geometry(geo, particle, drop)))
            out.append(("%s_item" % piece, geometry(geo, particle, display=A.small_display(geo),
                                                    centre=True)))
        elif kind == "laundry":
            out.append((piece, geometry(spec["geo"], "shell")))
    for part, geo in VANITY["parts"].items():
        out.append(("bathroom_vanity_%s" % part, geometry(geo, "counter")))
    out.append(("bathroom_vanity_item", geometry(VANITY["item"], "counter")))
    for i, cell in enumerate(B.cut_cells(TUB_BODY, TUB_CELLS)):
        out.append(("bathtub_cell%d" % i, geometry(full_uv(cell), "porcelain")))
    for i, cell in enumerate(B.cut_cells(TUB_WATER, TUB_CELLS)):
        out.append(("bathtub_water%d" % i, geometry(cell, "water")))
    out.append(("bathtub_item", geometry(TUB_ITEM, "porcelain", display=B.big_display(TUB_ITEM))))
    out.append(("shower_enclosure_lower", geometry(SHOWER_LOWER, "chrome")))
    out.append(("shower_enclosure_upper", geometry(SHOWER_UPPER, "chrome")))
    out.append(("shower_enclosure_item", geometry(SHOWER, "chrome", display=K.TALL_DISPLAY)))
    out.append(("baby_changing_station_closed", geometry(CHANGING_CLOSED, "plastic")))
    out.append(("baby_changing_station_open", geometry(CHANGING_OPEN, "plastic")))
    return out


def facing_variants():
    return {f: ({"y": r} if r else {}) for f, r in ROT.items()}


def head_state(piece, ftex):
    return {"forge_marker": 1,
            "defaults": {"model": BASE + piece, "textures": dict(ftex)},
            "variants": {"facing": facing_variants(), "on": {"true": {}, "false": {}},
                         "inventory": [{}]}}


def folding_state(piece, ftex):
    return {"forge_marker": 1,
            "defaults": {"model": BASE + piece + "_closed", "textures": dict(ftex)},
            "variants": {"facing": facing_variants(),
                         "open": {"true": {"model": BASE + piece + "_open"},
                                  "false": {"model": BASE + piece + "_closed"}},
                         "inventory": [{}]}}


def laundry_state(piece, ftex, glow):
    off, on = GLOW[glow]
    return {"forge_marker": 1,
            "defaults": {"model": BASE + piece, "textures": dict(ftex, glow=off)},
            "variants": {"facing": facing_variants(),
                         "running": {"true": {"textures": {"glow": on}},
                                     "false": {"textures": {"glow": off}}},
                         "inventory": [{}]}}


def tub_rules():
    return R.faced([("cell0", {"head": "false"}), ("cell1", {"head": "true"}),
                    ("water0", {"head": "false", "water": "true"}),
                    ("water1", {"head": "true", "water": "true"})])


def shower_rules():
    return R.faced([("lower", {"upper": "false"}), ("upper", {"upper": "true"})])


def lang_entries():
    out = {loc: {} for loc in R.LOCALES}
    for reg, _p, _t, names, _j in entries():
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
    for reg, piece, ftex, _names, _java in entries():
        spec = PIECE[piece]
        kind = spec["kind"]
        blk = "models/block/%s/%s_%%s.json" % (SUB, reg)
        item = "models/item/%s.json" % reg
        if kind == "single":
            state = R.single_state(piece, ftex)
        elif kind == "head":
            state = head_state(piece, ftex)
        elif kind == "folding":
            state = folding_state(piece, ftex)
        elif kind == "laundry":
            state = laundry_state(piece, ftex, spec["glow"])
        elif kind == "counter":
            state = A.counter_state(piece, ftex, None)
        elif kind == "storage":
            copy_model(blk % "body", "%s_body" % piece, ftex)
            copy_model(item, "%s_item" % piece, ftex)
            state = B.multipart_state(reg, R.faced([("body", {})]))
        elif kind == "vanity":
            for part in VANITY["parts"]:
                copy_model(blk % part, "bathroom_vanity_%s" % part, ftex)
            copy_model(item, "bathroom_vanity_item", ftex)
            state = B.multipart_state(reg, VANITY["rules"])
        elif kind == "tub":
            for part in ("cell0", "cell1", "water0", "water1"):
                copy_model(blk % part, "bathtub_%s" % part, ftex)
            copy_model(item, "bathtub_item", ftex)
            state = B.multipart_state(reg, tub_rules())
        elif kind == "shower":
            for part in ("lower", "upper"):
                copy_model(blk % part, "shower_enclosure_%s" % part, ftex)
            copy_model(item, "shower_enclosure_item", ftex)
            state = B.multipart_state(reg, shower_rules())
        elif kind == "mat":
            for part in ("field", "side", "corner"):
                copy_model(blk % part, "rug_%s" % part, ftex)
            copy_model(item, "rug_item", ftex)
            state = B.multipart_state(reg, B.rug_rules())
        else:
            raise ValueError(kind)
        dump("blockstates/%s.json" % reg, state)
    R.write_lang(os.path.join(assets, "lang"), lang_entries())
    written += ["lang/%s.lang" % loc for loc in R.LOCALES]
    R.separate_faces(assets, written)
    return written


def fragments():
    lines = []
    last = None
    for reg, piece, _t, names, java in entries():
        if piece != last:
            if last is not None:
                lines.append("")
            if piece in GROUPS:
                lines.append("    // ---- %s ----" % GROUPS[piece])
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
    tmp = tempfile.mkdtemp(prefix="bathroom_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not R.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("bathroom and laundry furniture is up to date (%d files)" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
