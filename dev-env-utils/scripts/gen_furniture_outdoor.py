#!/usr/bin/env python3
"""
gen_furniture_outdoor.py -- the Residential tab's last section, outdoor and backyard, in the
Furniture & Novelties module: a patio set (a slatted table that joins like the dining table,
chairs, a free-standing umbrella and one for the table's middle, sun loungers, Adirondack chairs,
an outdoor sofa that joins like the sofa, outdoor rugs); cooking and fire (a gas grill and a
charcoal kettle grill that cook, fire pits, a cooler, a brick chimney stack); the garden (a white
picket fence and its gate, stepping stones, string lights); backyard fun (a trampoline and a
bounce castle that bounce, a springboard, a kiddie pool, pool float rings); pets (beds, bowls, a
litter box, a cat tree); and a garden hose reel and an outdoor wall light.

Borrows gen_furniture_residential.py's element helpers, woods, fabrics and output, the bedroom's
cut and item displays and rug parts, the appliances' small-piece displays, and the living room's
spanned UVs, planes, octagons and fire textures (all imported, not copied), and writes, under
modules/furnishings/src/main/resources/assets/csm:

  * textures/blocks/furniture/outdoor/*.png  teak and powder-coated steel, white paint, outdoor
    fabrics and rugs, grill enamel, fire pit stones, brick, flagstones, bulbs, trampoline mat and
    pads, the castle's vinyl and netting, pool vinyl and (animated) water, carpet and sisal, the
    hose, and the rest
  * models/block/furniture/outdoor/base/*.json  the geometry
  * models/block/furniture/outdoor/<registry>_<part>.json  a finish's copy of each part a
    multipart blockstate picks
  * blockstates/<registry>.json, and models/item/<registry>.json where the state has no
    inventory variant
  * the tile names in all four languages, by key

Every model faces north with its back at +Z, as the rest of the tab's do. Real-world scale,
1 block = 1 m. What each piece does is its Java class's (furniture/outdoor, and the residential
classes it reuses); the positions those classes read are named here: CASTLE (the castle's floor
top and walls, which BounceCastleLayout repeats), GRILL_VENT and KETTLE_VENT (where grill smoke
rises), CHIMNEY_POTS, FIRE_Y.

The bounce castle is drawn whole by its root block, the middle of its floor, reaching a block
past it on every side (-16 to 32, the most a JSON element may), two blocks high; its other
sixteen blocks are invisible parts with an empty model.

Usage:
    python gen_furniture_outdoor.py              # write everything
    python gen_furniture_outdoor.py --check      # fail if the tree has drifted
    python gen_furniture_outdoor.py --fragments  # print the tab registration lines

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
import gen_furniture_bedroom as B  # noqa: E402
import gen_furniture_appliances as A  # noqa: E402
import gen_furniture_living as L  # noqa: E402

ASSETS = R.ASSETS
SUB = "furniture/outdoor"
MODEL = "csm:%s/" % SUB
BASE = MODEL + "base/"
ROT = R.ROT
RT = R.T
LT = L.T
el, board, leg, octagon = R.el, R.board, R.leg, R.octagon
ALL = R.ALL
SIDES = ("north", "south", "east", "west")
NO_DOWN = SIDES + ("up",)
NO_BACK = ("north", "east", "west", "up", "down")
shade, clamp = R.shade, R.clamp
span, full, move = L.span, L.full, L.move
plane_x, plane_y, plane_z = L.plane_x, L.plane_y, L.plane_z
octagon_z = L.octagon_z
TAN = R.TAN_22_5


def T(name):
    return "csm:blocks/%s/%s" % (SUB, name)


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def blank(size):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def glossy(base, seed, size=16):
    return L.glossy(base, seed, size)


def noisy(base, seed, size=16, amount=0.05):
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = shade(base, 1.0 + rng.uniform(-amount, amount)) + (255,)
    return img


def slats(base, seed, size=32, pitch=8):
    """Horizontal boards with a dark gap between them, for a teak panel."""
    img = R.wood(base, seed, size=size)
    px = img.load()
    for y in range(size):
        if y % pitch == pitch - 1:
            for x in range(size):
                px[x, y] = shade(px[x, y][:3], 0.4) + (255,)
    return img


def stripes(colours, seed, size=16, along_x=False, widths=None):
    """Bands of colour across the texture (along v, or along u), a faint weave in them."""
    rng = random.Random(seed)
    widths = widths or [size // len(colours)] * len(colours)
    band = []
    for c, w in zip(colours, widths):
        band += [c] * w
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            c = band[(x if along_x else y) % len(band)]
            k = (1.03 if (x + y) % 2 else 0.97) * (1 + rng.uniform(-0.02, 0.02))
            px[x, y] = shade(c, k) + (255,)
    return img


def stones(seed, size=16):
    """A dry-laid ring of fieldstones: irregular grey stones in darker joints."""
    rng = random.Random(seed)
    pts = [(rng.uniform(0, size), rng.uniform(0, size), rng.choice(
        [(150, 146, 138), (128, 124, 118), (170, 164, 150), (138, 130, 120), (116, 116, 118)]))
        for _ in range(7)]
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            ds = []
            for (px0, py0, c) in pts:
                dx = min(abs(x + 0.5 - px0), size - abs(x + 0.5 - px0))
                dy = min(abs(y + 0.5 - py0), size - abs(y + 0.5 - py0))
                ds.append((math.hypot(dx, dy * 1.4), c))
            ds.sort(key=lambda t: t[0])
            c = ds[0][1]
            if ds[1][0] - ds[0][0] < 0.9:
                c = (66, 62, 58)
            px[x, y] = shade(c, 1.0 + rng.uniform(-0.06, 0.06)) + (255,)
    return img


def rust(seed, size=16):
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            c = (116, 62, 36) if rng.random() > 0.2 else (92, 48, 30)
            if rng.random() < 0.06:
                c = (150, 86, 44)
            px[x, y] = shade(c, 1.0 + rng.uniform(-0.06, 0.06)) + (255,)
    return img


def brick(seed, size=16):
    """Red brick in running bond: courses 4 texels (6.25 cm) high, a brick 8 long, the joints
    light grey; a block is four courses, so a stack keeps its bond."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    tones = {}
    for y in range(size):
        for x in range(size):
            row = y // 4
            off = 4 if row % 2 else 0
            if y % 4 == 3 or (x + off) % 8 == 7:
                c = (176, 170, 160)
            else:
                key = (row, ((x + off) // 8) % 2)
                if key not in tones:
                    tones[key] = rng.choice([(150, 58, 42), (138, 50, 38), (162, 70, 50),
                                             (126, 48, 40), (156, 64, 44)])
                c = tones[key]
            px[x, y] = shade(c, 1.0 + rng.uniform(-0.05, 0.05)) + (255,)
    return img


def flagstone(base, seed, size=16):
    """A flagstone's top: speckled, with a vein or two across it."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    veins = [(rng.uniform(0, size), rng.uniform(-0.6, 0.6)) for _ in range(2)]
    for y in range(size):
        for x in range(size):
            k = 1.0 + rng.uniform(-0.07, 0.07) + 0.04 * math.sin(x * 0.5 + y * 0.3)
            for c0, slope in veins:
                if abs(y - (c0 + slope * x)) < 0.5:
                    k *= 0.86
            px[x, y] = shade(base, k) + (255,)
    return img


def bulb(colour, on, seed):
    """A festoon bulb's glass: lit, bright at its middle; out, a tinted glass."""
    rng = random.Random(seed)
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - 8, y + 0.5 - 7)
            if on:
                c = R.clamp(tuple(v + max(0.0, 60 - r * 7) for v in colour))
            else:
                c = shade(colour, 0.9 + 0.05 * math.sin(x + y))
            px[x, y] = shade(c, 1.0 + rng.uniform(-0.02, 0.02)) + (255,)
    return img


def weave(base, seed, size=16):
    """A trampoline mat's tight polypropylene weave."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = 1.1 if (x % 2 == 0) != (y % 2 == 0) else 0.9
            px[x, y] = shade(base, k * (1 + rng.uniform(-0.04, 0.04))) + (255,)
    return img


def stitched(base, seed, size=16):
    """A padded vinyl cover: glossy, with a line of stitching near each edge."""
    img = glossy(base, seed, size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if (y in (1, size - 2) and x % 2 == 0) or (x in (1, size - 2) and y % 2 == 0):
                px[x, y] = shade(px[x, y][:3], 0.72) + (255,)
    return img


def netting(size=16):
    """Black netting, cutout: one-texel cords with one-texel holes."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if x % 2 == 0 or y % 2 == 0:
                px[x, y] = (26, 26, 28, 255)
    return img


def quilted(base, seed, size=16):
    """The castle's inflated floor from above: tubes, a seam every four texels."""
    img = glossy(base, seed, size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if y % 4 == 0:
                px[x, y] = shade(px[x, y][:3], 0.78) + (255,)
            elif y % 4 == 2:
                px[x, y] = shade(px[x, y][:3], 1.08) + (255,)
    return img


def water_frames(seed, frames=8, size=16):
    """Pool water: blue, with bright ripples drifting across it."""
    out = []
    for f in range(frames):
        img = blank(size)
        px = img.load()
        t = f / float(frames) * 2 * math.pi
        for y in range(size):
            for x in range(size):
                w = (math.sin(x * 0.8 + t) + math.sin(y * 0.7 - t + x * 0.3)
                     + math.sin((x + y) * 0.45 + 2 * t))
                c = (78, 166, 214)
                if w > 1.6:
                    c = (178, 226, 246)
                elif w > 1.0:
                    c = (120, 196, 232)
                px[x, y] = c + (255,)
        out.append(img)
    return L.strip(out)


def still_water():
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            w = math.sin(x * 0.8) + math.sin(y * 0.7 + x * 0.3)
            px[x, y] = ((150, 206, 236) if w > 1.3 else (92, 170, 214)) + (255,)
    return img


def kibble(seed):
    rng = random.Random(seed)
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = rng.choice([(128, 78, 40), (150, 96, 52), (104, 62, 32), (168, 112, 60)])
            if (x + 2 * y) % 5 == 0:
                c = shade(c, 0.6)
            px[x, y] = c + (255,)
    return img


def litter(seed):
    rng = random.Random(seed)
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = rng.choice([(202, 196, 184), (184, 178, 166), (216, 210, 198),
                                   (170, 164, 154)]) + (255,)
    return img


def carpet(base, seed, size=16):
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = shade(base, 1.0 + rng.uniform(-0.09, 0.09)) + (255,)
    return img


def cubby(base, seed):
    """The condo's front: carpet with a round doorway, dark inside."""
    img = carpet(base, seed)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if L.ellipse_in(x, y, 8, 9, 5.2, 5.2):
                px[x, y] = shade(base, 0.22) + (255,)
            elif L.ellipse_in(x, y, 8, 9, 6, 6):
                px[x, y] = shade(base, 0.75) + (255,)
    return img


def sisal(seed):
    rng = random.Random(seed)
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            k = 1.08 if y % 2 == 0 else 0.86
            if (x + y * 3) % 7 == 0:
                k *= 0.9
            px[x, y] = shade((196, 166, 116), k * (1 + rng.uniform(-0.04, 0.04))) + (255,)
    return img


def hose_coil(seed):
    """A hose wound on its drum: green turns with a dark groove between each."""
    rng = random.Random(seed)
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            k = 0.55 if y % 3 == 2 else (1.12 if y % 3 == 0 else 1.0)
            px[x, y] = shade((52, 128, 60), k * (1 + rng.uniform(-0.03, 0.03))) + (255,)
    return img


def galvanized(seed):
    rng = random.Random(seed)
    img = R.metal((168, 172, 170), seed)
    px = img.load()
    for _ in range(10):
        cx, cy = rng.randrange(16), rng.randrange(16)
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                x, y = (cx + dx) % 16, (cy + dy) % 16
                px[x, y] = shade(px[x, y][:3], 1.07 if (dx + dy) % 2 else 0.95) + (255,)
    return img


def rug_stripes(colours, widths, seed):
    return stripes(colours, seed, widths=widths)


def glow_strip(on):
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if on:
                c = (255, 150 + 40 * ((x * 7 + y * 3) % 3 == 0), 50)
            else:
                c = (30, 26, 24)
            px[x, y] = c + (255,)
    return img


FABRIC_COLOURS = {
    "sand": ((214, 198, 164), (186, 168, 134)),
    "slate": ((100, 106, 114), (80, 86, 94)),
    "teal": ((38, 112, 118), (28, 88, 92)),
    "cream": ((236, 226, 200), (208, 196, 170)),
    "navy": ((36, 52, 92), (28, 40, 72)),
}

TEXTURES = {
    # (draw, frametime or None)
    "teak": (lambda: R.wood((170, 112, 66), 601, line=0.8, figure=0.06), None),
    "teak_v": (lambda: R.wood((170, 112, 66), 601, vertical=True, line=0.8, figure=0.06), None),
    "teak_edge": (lambda: R.wood((140, 90, 52), 602, streak=0.05), None),
    "teak_slats": (lambda: slats((170, 112, 66), 603), None),
    "powder_black": (lambda: R.metal((42, 42, 44), 604), None),
    "powder_black_edge": (lambda: R.metal((58, 58, 60), 605), None),
    "powder_black_slats": (lambda: slats((44, 44, 46), 606), None),
    "paint_white": (lambda: R.laminate((240, 240, 234), 607), None),
    "paint_white_v": (lambda: R.wood((240, 240, 234), 607, vertical=True, streak=0.006,
                                     line=0.985, figure=0.0), None),
    "paint_white_edge": (lambda: R.flat((222, 222, 216), 608, grain=3), None),
    "pole_bronze": (lambda: R.metal((96, 80, 62), 609), None),
    "canopy_cream": (lambda: R.fabric(FABRIC_COLOURS["cream"][0], 610), None),
    "canopy_cream_dark": (lambda: R.fabric(FABRIC_COLOURS["cream"][1], 611), None),
    "canopy_navy": (lambda: R.fabric(FABRIC_COLOURS["navy"][0], 612), None),
    "canopy_navy_dark": (lambda: R.fabric(FABRIC_COLOURS["navy"][1], 613), None),
    "sand": (lambda: R.fabric(FABRIC_COLOURS["sand"][0], 614, heather=0.05), None),
    "sand_dark": (lambda: R.fabric(FABRIC_COLOURS["sand"][1], 615, heather=0.05), None),
    "slate": (lambda: R.fabric(FABRIC_COLOURS["slate"][0], 616), None),
    "slate_dark": (lambda: R.fabric(FABRIC_COLOURS["slate"][1], 617), None),
    "teal": (lambda: R.fabric(FABRIC_COLOURS["teal"][0], 618), None),
    "teal_dark": (lambda: R.fabric(FABRIC_COLOURS["teal"][1], 619), None),
    "rug_blue": (lambda: rug_stripes([(46, 86, 138), (232, 230, 220), (110, 150, 190),
                                      (232, 230, 220)], [5, 2, 7, 2], 620), None),
    "rug_border_blue": (lambda: R.fabric((30, 44, 78), 621), None),
    "rug_sand": (lambda: rug_stripes([(206, 184, 146), (178, 96, 64), (206, 184, 146),
                                      (236, 224, 196)], [6, 2, 6, 2], 622), None),
    "rug_border_sand": (lambda: R.fabric((110, 76, 50), 623), None),
    "enamel_black": (lambda: glossy((28, 28, 30), 624), None),
    "enamel_red": (lambda: glossy((154, 34, 32), 625), None),
    "tank_white": (lambda: R.flat((232, 232, 226), 626, grain=2), None),
    "grill_glow_off": (lambda: glow_strip(False), None),
    "grill_glow_on": (lambda: glow_strip(True), None),
    "firepit_stone": (lambda: stones(627), None),
    "steel_rust": (lambda: rust(628), None),
    "cooler_blue": (lambda: noisy((40, 100, 172), 629, amount=0.03), None),
    "cooler_red": (lambda: noisy((186, 40, 40), 630, amount=0.03), None),
    "cooler_white": (lambda: noisy((236, 236, 232), 631, amount=0.02), None),
    "brick_red": (lambda: brick(632), None),
    "flagstone_grey": (lambda: flagstone((150, 148, 142), 633), None),
    "flagstone_slate": (lambda: flagstone((88, 94, 102), 634), None),
    "wire_black": (lambda: R.flat((30, 30, 32), 635, grain=2), None),
    "bulb_warm_on": (lambda: bulb((255, 214, 140), True, 636), None),
    "bulb_warm_off": (lambda: bulb((196, 188, 170), False, 637), None),
    "bulb_red_on": (lambda: bulb((250, 70, 60), True, 638), None),
    "bulb_red_off": (lambda: bulb((140, 56, 52), False, 639), None),
    "bulb_green_on": (lambda: bulb((90, 230, 110), True, 640), None),
    "bulb_green_off": (lambda: bulb((58, 112, 64), False, 641), None),
    "bulb_blue_on": (lambda: bulb((90, 140, 255), True, 642), None),
    "bulb_blue_off": (lambda: bulb((58, 74, 130), False, 643), None),
    "mat_black": (lambda: weave((30, 30, 34), 644), None),
    "pad_blue": (lambda: stitched((40, 92, 190), 645), None),
    "pad_green": (lambda: stitched((50, 148, 72), 646), None),
    "vinyl_red": (lambda: glossy((212, 40, 40), 647), None),
    "vinyl_blue": (lambda: glossy((40, 92, 204), 648), None),
    "vinyl_yellow": (lambda: glossy((244, 202, 40), 649), None),
    "castle_floor": (lambda: quilted((212, 44, 44), 650), None),
    "castle_mesh": (netting, None),
    "board_white": (lambda: glossy((236, 240, 240), 651), None),
    "board_grip": (lambda: noisy((214, 200, 170), 652, amount=0.08), None),
    "pool_vinyl": (lambda: glossy((58, 138, 218), 653), None),
    "pool_vinyl_b": (lambda: stripes([(240, 240, 240), (58, 138, 218)], 654, along_x=True),
                     None),
    "pool_floor": (lambda: noisy((140, 206, 236), 655, amount=0.03), None),
    "pool_water": (lambda: water_frames(656), 3),
    "bowl_water": (still_water, None),
    "float_pink": (lambda: stripes([(240, 110, 160), (244, 240, 240)], 657, along_x=True),
                   None),
    "float_yellow": (lambda: stripes([(246, 206, 50), (244, 240, 240)], 658, along_x=True),
                     None),
    "kibble": (lambda: kibble(659), None),
    "litter": (lambda: litter(660), None),
    "scoop_blue": (lambda: glossy((60, 120, 200), 661), None),
    "carpet_beige": (lambda: carpet((206, 190, 160), 662), None),
    "carpet_grey": (lambda: carpet((146, 146, 150), 663), None),
    "cubby_beige": (lambda: cubby((206, 190, 160), 664), None),
    "cubby_grey": (lambda: cubby((146, 146, 150), 665), None),
    "sisal": (lambda: sisal(666), None),
    "toy_pom": (lambda: R.fabric((230, 80, 120), 667), None),
    "hose_coil": (lambda: hose_coil(668), None),
    "hose_green": (lambda: R.flat((52, 128, 60), 669, grain=3), None),
    "reel_green": (lambda: glossy((40, 104, 58), 670), None),
    "galvanized": (lambda: galvanized(671), None),
}

DEFAULT_TEX = {
    "wood": T("teak"), "wood_v": T("teak_v"), "edge": T("teak_edge"), "slats": T("teak_slats"),
    "fabric": T("sand"), "fabric_dark": T("sand_dark"), "metal": T("powder_black"),
    "chrome": RT("chrome"), "stainless": RT("stainless"), "brass": RT("brass"),
    "rubber": RT("rubber_black"), "pole": T("pole_bronze"), "base": T("powder_black"),
    "canopy": T("canopy_cream"), "canopy_dark": T("canopy_cream_dark"),
    "rug": T("rug_blue"), "rug_border": T("rug_border_blue"),
    "enamel": T("enamel_black"), "tank": T("tank_white"), "glow": T("grill_glow_off"),
    "stone": T("firepit_stone"), "steel": T("steel_rust"), "log": LT("log_bark"),
    "log_end": LT("log_end"), "flame": LT("clear"), "embers": LT("embers_off"),
    "shell": T("cooler_blue"), "lid": T("cooler_white"), "handle": RT("plastic_grey"),
    "brick": T("brick_red"), "crown": LT("concrete_grey"), "pot": LT("terracotta"),
    "soot": LT("soot"), "flag": T("flagstone_grey"), "wire": T("wire_black"),
    "b1": T("bulb_warm_on"), "b2": T("bulb_warm_on"), "b3": T("bulb_warm_on"),
    "mat": T("mat_black"), "pad": T("pad_blue"), "frame": RT("metal_steel"),
    "red": T("vinyl_red"), "blue": T("vinyl_blue"), "yellow": T("vinyl_yellow"),
    "floor": T("castle_floor"), "mesh": T("castle_mesh"), "board": T("board_white"),
    "grip": T("board_grip"), "vinyl": T("pool_vinyl"), "vinyl_b": T("pool_vinyl_b"),
    "pool_floor": T("pool_floor"), "water": T("pool_water"), "float": T("float_pink"),
    "cushion": RT("oatmeal"), "bowl": RT("stainless"), "kibble": T("kibble"),
    "bowl_water": T("bowl_water"), "plastic": RT("plastic_grey"), "litter": T("litter"),
    "scoop": T("scoop_blue"), "carpet": T("carpet_beige"), "cubby": T("cubby_beige"),
    "sisal": T("sisal"), "toy": T("toy_pom"), "hose": T("hose_coil"),
    "hose_green": T("hose_green"), "reel": T("reel_green"), "lens": RT("lens_on"),
}


# ------------------------------------------------------------------------------------------
# Element helpers
# ------------------------------------------------------------------------------------------
def octagon_x(cy, cz, r, x0, x1, tex, side=None, caps=("east", "west")):
    """An octagon lying along x (its axis left to right) of inradius r about (cy, cz): four
    rectangles, two turned 45 degrees about x, as R.octagon is about y."""
    a = r * TAN
    out = []
    for k, (h1, h2, turned) in enumerate(((r, a, False), (a, r, False), (r, a, True),
                                          (a, r, True))):
        eps = 0.004 * k
        frm = [x0 - eps, cy - h1, cz - h2]
        to = [x1 + eps, cy + h1, cz + h2]
        sides = ["up", "down"] if h1 == r else ["north", "south"]
        per = {f: side for f in sides} if side else {}
        rot = ("x", 45, [x0, cy, cz]) if turned else None
        out.append(el(frm, to, tex, sides + list(caps), per, rot=rot))
    return out


def ring(cx, cz, radius, tube, y0, y1, tex, faces=ALL):
    """A ring lying flat (an inflatable, a bolster, a ring of stones): eight straight segments
    round (cx, cz), their middles at radius, each tube thick across and long enough to close
    the corners; the diagonal four turned 45 degrees about y about their own middles. Each face
    shows the whole texture, so a striped texture reads as stripes round the ring."""
    length = 2 * (radius + tube / 2) * TAN + 0.2
    out = []
    for k in range(8):
        a = k * math.pi / 4
        mx, mz = cx + radius * math.cos(a), cz + radius * math.sin(a)
        if k % 2 == 0:
            if k % 4 == 0:
                frm, to = [mx - tube / 2, y0, mz - length / 2], [mx + tube / 2, y1, mz + length / 2]
            else:
                frm, to = [mx - length / 2, y0, mz - tube / 2], [mx + length / 2, y1, mz + tube / 2]
            rot = None
        else:
            frm, to = [mx - tube / 2, y0, mz - length / 2], [mx + tube / 2, y1, mz + length / 2]
            rot = ("y", -45 if k in (1, 5) else 45, [mx, (y0 + y1) / 2, mz])
        out.append(full(el(frm, to, tex, faces, rot=rot), list(faces)))
    return out


def rounded(x0, z0, x1, z1, y0, y1, tex, cut=1.0, faces=("up", "north", "south", "east",
                                                          "west")):
    """A slab with its corners cut back: three boxes that do not overlap."""
    return [el([x0 + cut, y0, z0], [x1 - cut, y1, z1], tex, faces),
            el([x0, y0, z0 + cut], [x0 + cut, y1, z1 - cut], tex, faces),
            el([x1 - cut, y0, z0 + cut], [x1, y1, z1 - cut], tex, faces)]


def swing(specs, hinge_x=0.0, hinge_z=8.0):
    """Turned a quarter about the vertical line (hinge_x, hinge_z), so that what ran along +x
    from the hinge runs along -z from it: a gate leaf swinging open toward the front. Turned
    elements have their axis and angle carried round with them."""
    fm = {"north": "west", "west": "south", "south": "east", "east": "north"}
    out = []
    for s in specs:
        def pt(x, y, z):
            return (hinge_x + (z - hinge_z), y, hinge_z - (x - hinge_x))
        a, b = pt(*s["from"]), pt(*s["to"])
        n = dict(s)
        n["from"] = [min(a[i], b[i]) for i in range(3)]
        n["to"] = [max(a[i], b[i]) for i in range(3)]
        n["faces"] = tuple(fm.get(f, f) for f in s["faces"])
        n["per"] = {fm.get(f, f): t for f, t in s["per"].items()}
        n["uv"] = {}
        if s["rot"]:
            axis, angle, origin = s["rot"]
            o = pt(*origin)
            if axis == "x":
                n["rot"] = ("z", -angle, list(o))
            elif axis == "z":
                n["rot"] = ("x", angle, list(o))
            else:
                n["rot"] = ("y", angle, list(o))
        out.append(n)
    return out


def box_of(specs):
    built = [R.build(s) for s in specs]
    lo = [max(0, int(math.floor(min(e["from"][i] for e in built)))) for i in range(3)]
    hi = [min(16, int(math.ceil(max(e["to"][i] for e in built)))) for i in range(3)]
    return lo + hi


def jbox(box):
    return ", ".join(R._num(v) for v in box)


def geometry(specs, particle, display=None, parent="block/block"):
    built = [R.build(s) for s in specs]
    tex = {k: DEFAULT_TEX[k] for k in sorted(R.used_keys(specs) | {particle})}
    tex["particle"] = "#" + particle
    out = {"parent": parent, "textures": tex, "elements": built}
    if display:
        out["display"] = display
    return out


# ------------------------------------------------------------------------------------------
# The pieces, each drawn facing north (front at -Z, back at +Z), in pixels
# ------------------------------------------------------------------------------------------
# --- patio table: 0.75 m, a slatted top, joining like the dining table ----------------------
PATIO_TOP = [board([0, 11, z], [16, 12, z + 3.5], faces=("up", "down", "north", "south"))
             for z in (0.25, 4.25, 8.25, 12.25)]
PATIO_TOP += [board([0.5, 10.25, z], [15.5, 11, z + 1], faces=("down", "north", "south"))
              for z in (1.5, 13.5)]
PATIO_APRON = [board([2.5, 9.5, 0.75], [13.5, 11, 1.5], faces=("north", "south", "down"))]
PATIO_LEG = [leg([0.75, 0, 0.75], [2.25, 11, 2.25])]
PATIO_APRON_A = [board([0, 9.5, 0.75], [2.5, 11, 1.5], faces=("north", "south", "down"))]
PATIO_APRON_B = [board([0.75, 9.5, 0], [1.5, 11, 2.5], faces=("east", "west", "down"))]

# --- patio chair: an armchair with a slatted seat and back, seat 0.45 m ----------------------
PATIO_CHAIR = ([leg([x, 0, 3.5], [x + 1, 10.5, 4.5]) for x in (2.75, 12.25)]
               + [leg([x, 0, 11.5], [x + 1, 15.5, 12.5], faces=ALL) for x in (2.75, 12.25)]
               + [board([3.75, 6.5, z], [12.25, 7.25, z + 1.6]) for z in (4.25, 6.25, 8.25,
                                                                        10.25)]
               + [board([3.75, 5.5, 3.75], [12.25, 6.5, 4.5]),
                  board([3.75, 5.5, 11.5], [12.25, 6.5, 12.25])]
               + [board([x, 5.5, 4.5], [x + 0.75, 6.5, 11.5]) for x in (3.75, 11.5)]
               + [board([2.25, 10.5, 3], [4.25, 11.25, 13]),
                  board([11.75, 10.5, 3], [13.75, 11.25, 13])]
               + [board([x, 8.5, 11.75], [x + 1.5, 14.5, 12.25], grain="v")
                  for x in (4.5, 6.5, 8.5, 10.5)]
               + [board([3.75, 14.5, 11.5], [12.25, 15.5, 12.5]),
                  board([3.75, 7.75, 11.6], [12.25, 8.5, 12.4])])

# --- Adirondack chair: a low sloping seat, wide flat arms, a fanned back ----------------------
_ADI_BACK_ORIGIN = [8, 5.5, 12.5]
_ADI_TILT = ("x", 22.5, _ADI_BACK_ORIGIN)
ADIRONDACK = ([board([2, 0, 2.5], [3.25, 9.5, 4], grain="v"),
               board([12.75, 0, 2.5], [14, 9.5, 4], grain="v"),
               board([2.75, 3.75, 2.5], [3.75, 5, 14.5]),
               board([12.25, 3.75, 2.5], [13.25, 5, 14.5]),
               board([3.75, 0, 12.5], [4.75, 5, 13.75], grain="v"),
               board([11.25, 0, 12.5], [12.25, 5, 13.75], grain="v"),
               board([1, 9.5, 2], [5, 10.25, 12.5]),
               board([11, 9.5, 2], [15, 10.25, 12.5]),
               board([1.5, 8.25, 4], [3, 9.5, 5.5]),
               board([13, 8.25, 4], [14.5, 9.5, 5.5])]
              + [board([3.75, 5, z], [12.25, 5.75, z + 1.6]) for z in (3, 5, 7, 9, 11)]
              + [dict(board([x, 5.5, 12.5], [x + 1.5, top, 13.25], grain="v"), rot=_ADI_TILT)
                 for x, top in ((3.75, 15.5), (5.5, 16.5), (7.25, 17.25), (9, 16.5),
                                (10.75, 15.5))]
              + [dict(board([3.25, y, 13.25], [12.75, y + 1, 14]), rot=_ADI_TILT)
                 for y in (8, 13)])

# --- sun lounger, two blocks long (x 0 to 32), its backrest raised at x 0 --------------------
_HINGE = [10.5, 5.5, 8]
_RECLINE = ("z", -45, _HINGE)
LOUNGER = ([board([1, 4, 2], [31, 5.5, 3]), board([1, 4, 13], [31, 5.5, 14])]
           + [leg([x, 0, z], [x + 1.25, 4, z + 1.25]) for x in (1.25, 29.5)
              for z in (2, 12.75)]
           + [board([x, 5, 3], [x + 1.6, 5.75, 13]) for x in [10.75 + 2 * i for i in range(10)]]
           + [el([10.75, 5.75, 3.25], [31, 7, 12.75], "fabric"),
              el([10.75, 5.9, 3.1], [31, 6.9, 3.25], "fabric_dark", ("north",)),
              el([10.75, 5.9, 12.75], [31, 6.9, 12.9], "fabric_dark", ("south",))]
           + [dict(board([x, 5, 3], [x + 1.6, 5.75, 13]), rot=_RECLINE)
              for x in (1.25, 3.25, 5.25, 7.25, 9.0)]
           + [dict(el([1, 5.75, 3.25], [10.5, 7, 12.75], "fabric"), rot=_RECLINE),
              leg([5.5, 4, 3.5], [6.5, 9, 4.5]), leg([5.5, 4, 11.5], [6.5, 9, 12.5])])

# --- outdoor sofa: a teak base and back frame, cushions in weather fabric -------------------
OSOFA_BODY = [el([0, 1, 1.5], [16, 4.5, 15.5], "slats", ("north", "south", "down")),
              el([0.1, 4.5, 2], [15.9, 7.25, 12.5], "fabric"),
              el([0, 4.5, 13.5], [16, 11.5, 15.5], "slats", ("north", "south")),
              board([0, 11.5, 13.25], [16, 12.25, 15.75], faces=("north", "south", "up",
                                                                   "down")),
              el([0.1, 7.25, 10.5], [15.9, 13.5, 13.5], "fabric"),
              leg([7.25, 0, 2], [8.75, 1, 3.5]), leg([7.25, 0, 13.5], [8.75, 1, 15])]
OSOFA_ARM = [el([0, 1, 1.25], [2.25, 10, 15.75], "slats", ALL, {"up": "edge"}),
             board([-0.25, 10, 1], [2.5, 10.75, 16]),
             leg([0.25, 0, 1.5], [1.75, 1, 3]), leg([0.25, 0, 14], [1.75, 1, 15.5])]
OCORNER_BODY = ([el([1.5, 1, 1.5], [15.5, 4.5, 15.5], "slats", ("south", "east", "down")),
                 el([0, 1, 1.5], [1.5, 4.5, 15.5], "slats", ("south", "down")),
                 el([1.5, 1, 0], [15.5, 4.5, 1.5], "slats", ("east", "down")),
                 el([2, 4.5, 2], [12.5, 7.25, 12.5], "fabric", ("up",)),
                 el([0.1, 4.5, 2], [2, 7.25, 12.5], "fabric", ("up", "north", "south")),
                 el([2, 4.5, 0.1], [12.5, 7.25, 2], "fabric", ("up", "north", "east", "west")),
                 el([0, 4.5, 13.5], [15.5, 11.5, 15.5], "slats", ("north", "south", "east")),
                 el([13.5, 4.5, 0], [15.5, 11.5, 13.5], "slats", ("west", "east")),
                 board([0, 11.5, 13.25], [15.75, 12.25, 15.75]),
                 board([13.25, 11.5, 0], [15.75, 12.25, 13.25], faces=("east", "west", "up",
                                                                        "down", "north")),
                 el([0.1, 7.25, 10.5], [13.5, 13.5, 13.5], "fabric"),
                 el([10.5, 7.25, 0.1], [13.5, 13.5, 10.5], "fabric",
                    ("north", "east", "west", "up"))]
                + [leg([x, 0, z], [x + 1.5, 1, z + 1.5], tex="wood_v")
                   for x, z in ((2, 2), (2, 13.75), (13.75, 2), (13.75, 13.75))])
OCORNER_CAPS = ([el([0, 1, 1.5], [0.01, 4.5, 15.5], "slats", ("west",)),
                 el([0, 4.5, 13.5], [0.01, 11.5, 15.5], "slats", ("west",))]
                + R.swap_xz([el([0, 1, 1.5], [0.01, 4.5, 15.5], "slats", ("west",)),
                             el([0, 4.5, 13.5], [0.01, 11.5, 15.5], "slats", ("west",))])
                + [el([0, 1, 0], [1.5, 4.5, 1.5], "slats", ("north", "west", "down")),
                   el([0.1, 4.5, 0.1], [2, 7.25, 2], "fabric", ("north", "west", "up"))])

# --- umbrellas: a square canopy 2.25 m across, its edge 2.05 m up -----------------------------
CANOPY_HALF = 18.0
CANOPY_EDGE = 17.0
CANOPY_APEX = CANOPY_EDGE + CANOPY_HALF * TAN
_COS = math.cos(math.pi / 8)


def canopy():
    """Four panels at 22.5 degrees, each laid as strips that narrow toward the top so that
    together they read as a pyramid from above and below; a valance round the edge, a rib
    under each panel, and the finial."""
    out = []
    steps = (0.0, 4.5, 9.0, 13.5, CANOPY_HALF)
    top = CANOPY_APEX
    o = [8, top, 8]
    for side in ("north", "south", "east", "west"):
        for a, b in zip(steps, steps[1:]):
            w = b + 0.25
            s0, s1 = a / _COS, b / _COS
            if side == "north":
                s = el([8 - w, top - 0.5, 8 - s1], [8 + w, top, 8 - s0], "canopy",
                       ("up", "down", "north"), rot=("x", -22.5, o))
            elif side == "south":
                s = el([8 - w, top - 0.5, 8 + s0], [8 + w, top, 8 + s1], "canopy",
                       ("up", "down", "south"), rot=("x", 22.5, o))
            elif side == "east":
                s = el([8 + s0, top - 0.5, 8 - w], [8 + s1, top, 8 + w], "canopy",
                       ("up", "down", "east"), rot=("z", -22.5, o))
            else:
                s = el([8 - s1, top - 0.5, 8 - w], [8 - s0, top, 8 + w], "canopy",
                       ("up", "down", "west"), rot=("z", 22.5, o))
            span(s, ["up", "down"], (8 - CANOPY_HALF / _COS, 8 + CANOPY_HALF / _COS,
                                     8 - CANOPY_HALF / _COS, 8 + CANOPY_HALF / _COS))
            out.append(s)
        # The rib under the panel's middle.
        if side in ("north", "south"):
            z0, z1 = (8 - CANOPY_HALF / _COS, 8) if side == "north" else (8, 8 + CANOPY_HALF / _COS)
            out.append(el([7.8, top - 1.0, z0], [8.2, top - 0.5, z1], "pole", ("down", "east",
                                                                             "west"),
                          rot=("x", -22.5 if side == "north" else 22.5, o)))
        else:
            x0, x1 = (8, 8 + CANOPY_HALF / _COS) if side == "east" else (8 - CANOPY_HALF / _COS, 8)
            out.append(el([x0, top - 1.0, 7.8], [x1, top - 0.5, 8.2], "pole", ("down", "north",
                                                                             "south"),
                          rot=("z", -22.5 if side == "east" else 22.5, o)))
    lo, hi = 8 - CANOPY_HALF, 8 + CANOPY_HALF
    val = CANOPY_EDGE
    for f, frm, to in (("north", [lo, val - 1.6, lo], [hi, val, lo + 0.1]),
                       ("south", [lo, val - 1.6, hi - 0.1], [hi, val, hi]),
                       ("west", [lo, val - 1.6, lo], [lo + 0.1, val, hi]),
                       ("east", [hi - 0.1, val - 1.6, lo], [hi, val, hi])):
        opp = {"north": "south", "south": "north", "east": "west", "west": "east"}[f]
        out.append(el(frm, to, "canopy_dark", (f, opp)))
    out += octagon(8, 8, 0.9, top - 0.2, top + 1.2, "pole")
    out += octagon(8, 8, 0.5, top + 1.2, top + 2.0, "pole")
    return out


def furled():
    """The canopy tied round the pole, its ribs' tips at the bottom."""
    top = CANOPY_APEX
    return ([el([6.2, 7, 6.2], [9.8, 12, 9.8], "canopy"),
             el([6.5, 12, 6.5], [9.5, 17, 9.5], "canopy"),
             el([6.9, 17, 6.9], [9.1, 21, 9.1], "canopy"),
             el([7.3, 21, 7.3], [8.7, top, 8.7], "canopy"),
             el([6.1, 13.5, 6.1], [9.9, 14.3, 9.9], "canopy_dark")]
            + octagon(8, 8, 0.9, top, top + 1.2, "pole")
            + octagon(8, 8, 0.5, top + 1.2, top + 2.0, "pole"))


def pole(y0, y1):
    return octagon(8, 8, 0.7, y0, y1, "pole", caps=())


UMBRELLA_LOWER = ([el([4.5, 0, 4.5], [11.5, 1.25, 11.5], "base"),
                   el([6, 1.25, 6], [10, 2.5, 10], "base")]
                  + pole(2.5, 16)
                  + [el([8.7, 13.25, 7.7], [10.2, 13.85, 8.3], "base"),
                     el([10.2, 12.2, 7.7], [10.7, 13.85, 8.3], "base")])
UMBRELLA_OPEN = pole(0, CANOPY_APEX) + canopy()
UMBRELLA_CLOSED = pole(0, 7) + furled()
TABLE_UMBRELLA_EXTRA = ([el([6, -16, 6], [10, -15.25, 10], "base")] + pole(-15.25, 0))

# --- gas grill on its cart, and the charcoal kettle -----------------------------------------
GRILL_VENT = (8, 14.5, 12)
GAS_GRILL = ([leg([x, 0, z], [x + 1, 7.5, z + 1], tex="metal") for x in (3, 12)
              for z in (3.75, 11.25)]
             + [el([3, 2, 3.75], [13, 2.5, 12.25], "metal"),
                el([3.25, 7.5, 3.5], [12.75, 11, 12.5], "enamel"),
                full(el([3.5, 7.75, 3.2], [12.5, 10.5, 3.5], "stainless", ("north",)),
                     ["north"]),
                el([3, 11, 3.5], [13, 13.5, 12.5], "enamel"),
                el([3, 13.5, 4.5], [13, 14.3, 11.5], "enamel"),
                el([3.25, 10.95, 3.35], [12.75, 11.15, 3.5], "glow", ("north",)),
                el([4, 12.5, 1.8], [12, 13.1, 2.4], "chrome"),
                el([4.3, 12.5, 2.4], [4.9, 13.1, 3.5], "chrome"),
                el([11.1, 12.5, 2.4], [11.7, 13.1, 3.5], "chrome"),
                el([7.4, 11.7, 3.3], [8.6, 12.9, 3.5], "chrome", ("north",)),
                el([0, 10.25, 4], [3, 10.75, 12], "stainless"),
                el([13, 10.25, 4], [16, 10.75, 12], "stainless"),
                el([1, 9, 5], [3, 10.25, 5.5], "metal"),
                el([13, 9, 5], [15, 10.25, 5.5], "metal"),
                el([7, 13, 12.5], [9, 14, 13.25], "enamel")]
             + [el([x, 8.4, 2.6], [x + 1.1, 9.5, 3.2], "metal") for x in (4.8, 7.45, 10.1)]
             + octagon(6.5, 8, 1.9, 2.5, 6.5, "tank")
             + [el([6.1, 6.5, 7.6], [6.9, 7.2, 8.4], "brass")])
KETTLE_VENT = (8, 15.2, 8)
_KETTLE_LEGS = []
for _i, _a in enumerate((90, 210, 330)):
    _lx = 8 + 4.3 * math.cos(math.radians(_a))
    _lz = 8 + 4.3 * math.sin(math.radians(_a))
    _KETTLE_LEGS.append(leg([_lx - 0.4, 0, _lz - 0.4], [_lx + 0.4, 7.5, _lz + 0.4], tex="metal"))
KETTLE_GRILL = (_KETTLE_LEGS
                + octagon(8, 8, 3.6, 2.5, 2.9, "enamel")
                + octagon(8, 8, 3.6, 6.5, 7.5, "enamel", caps=("down",))
                + octagon(8, 8, 4.7, 7.5, 9.0, "enamel", caps=("down",))
                + octagon(8, 8, 5.4, 9.0, 11.4, "enamel", caps=("down",))
                + octagon(8, 8, 5.45, 11.4, 11.6, "glow", caps=())
                + octagon(8, 8, 5.4, 11.6, 13.0, "enamel", caps=())
                + octagon(8, 8, 4.5, 13.0, 14.2, "enamel", caps=("up",))
                + octagon(8, 8, 3.0, 14.2, 15.0, "enamel", caps=("up",))
                + [el([6.8, 15.8, 7.6], [9.2, 16.4, 8.4], "handle"),
                   el([6.8, 15, 7.7], [7.3, 15.8, 8.3], "stainless"),
                   el([8.7, 15, 7.7], [9.2, 15.8, 8.3], "stainless"),
                   el([13.2, 9.6, 7.5], [14.4, 10.3, 8.5], "handle"),
                   el([1.6, 9.6, 7.5], [2.8, 10.3, 8.5], "handle")])

# --- fire pits: a ring of fieldstone, or a steel bowl on legs --------------------------------
FIRE_Y = {"stone": 0.8, "steel": 4.5}


def fire(y):
    """Logs laid crossways on a bed of embers, and two crossed sheets of flame over them."""
    logs = [el([4, y, 7.3], [12, y + 1.4, 8.7], "log", ALL, {"east": "log_end", "west": "log_end"},
               rot=("y", 22.5, [8, y, 8])),
            el([4.5, y + 1.0, 7.3], [11.5, y + 2.4, 8.7], "log", ALL,
               {"east": "log_end", "west": "log_end"}, rot=("y", -45, [8, y, 8])),
            el([7.3, y + 0.5, 4.5], [8.7, y + 1.9, 11.5], "log", ALL,
               {"north": "log_end", "south": "log_end"}, rot=("y", 22.5, [8, y, 8]))]
    flames = [plane_z(4, 12, y + 0.6, y + 10.6, 8, "flame", rot=("y", a, [8, y, 8]))
              for a in (45, -45)]
    return logs + flames


FIRE_PIT_STONE = (ring(8, 8, 6.0, 2.4, 0, 4.5, "stone")
                  + octagon(8, 8, 5.0, 0, 0.8, "embers", caps=("up",))
                  + fire(FIRE_Y["stone"]))
FIRE_PIT_STEEL = ([leg([x, 0, z], [x + 1, 3.5, z + 1], tex="metal")
                   for x, z in ((3, 3), (12, 3), (3, 12), (12, 12))]
                  + octagon(8, 8, 3.2, 2.8, 3.5, "steel")
                  + ring(8, 8, 6.2, 1.0, 3.5, 7.0, "steel")
                  + octagon(8, 8, 5.9, 3.5, 4.5, "embers", caps=("up", "down"))
                  + fire(FIRE_Y["steel"]))

# --- cooler: 0.7 x 0.4 x 0.5 m, a white lid ---------------------------------------------------
COOLER = [el([2.5, 0, 4.5], [13.5, 6.5, 11.5], "shell"),
          el([2.25, 6.5, 4.25], [13.75, 8, 11.75], "lid"),
          el([1.5, 4, 6.5], [2.5, 5, 9.5], "handle"),
          el([13.5, 4, 6.5], [14.5, 5, 9.5], "handle"),
          el([7, 5.5, 3.9], [9, 7, 4.25], "handle", NO_BACK),
          el([3.5, 0.5, 4.3], [4.5, 1.5, 4.5], "handle", ("north",))]

# --- chimney stack: 0.75 m square of brick, its top crowned, two clay pots -------------------
CHIMNEY_POTS = [(5, 8), (11, 8)]
CHIMNEY_COLUMN = [el([2, 0, 2], [14, 16, 14], "brick", SIDES)]
CHIMNEY_TOP = ([el([2, 0, 2], [14, 12, 14], "brick", SIDES),
                el([1, 12, 1], [15, 13.5, 15], "crown")]
               + [s for x, z in CHIMNEY_POTS for s in
                  octagon(x, z, 1.6, 13.5, 19, "pot", caps=())
                  + octagon(x, z, 1.9, 19, 19.8, "pot", caps=("down",))
                  + octagon(x, z, 1.3, 19.6, 19.85, "soot", caps=("up",))])

# --- white picket fence and gate --------------------------------------------------------------
def picket(x0, x1, z0, z1, y0, y1):
    """A picket with a pointed top, stepped to a point in two lifts."""
    out = [el([x0, y0, z0], [x1, y1, z1], "wood_v", ALL)]
    if x1 - x0 >= z1 - z0:
        out.append(el([x0 + 0.35, y1, z0], [x1 - 0.35, y1 + 0.6, z1], "wood_v", NO_DOWN))
        out.append(el([x0 + 0.75, y1 + 0.6, z0], [x1 - 0.75, y1 + 1.1, z1], "wood_v", NO_DOWN))
    else:
        out.append(el([x0, y1, z0 + 0.35], [x1, y1 + 0.6, z1 - 0.35], "wood_v", NO_DOWN))
        out.append(el([x0, y1 + 0.6, z0 + 0.75], [x1, y1 + 1.1, z1 - 0.75], "wood_v", NO_DOWN))
    return out


FENCE_POST = ([el([6.5, 0, 6.5], [9.5, 14, 9.5], "wood_v", ALL)]
              + [el([6.25, 14, 6.25], [9.75, 14.6, 9.75], "wood"),
                 el([7, 14.6, 7], [9, 15.4, 9], "wood")])
FENCE_SIDE = ([el([8.25, 3.5, 0], [9.25, 5, 6.5], "wood", ("east", "west", "up", "down")),
               el([8.25, 10, 0], [9.25, 11.5, 6.5], "wood", ("east", "west", "up", "down"))]
              + picket(7.25, 8.25, 0.75, 2.5, 1, 12.4)
              + picket(7.25, 8.25, 4.0, 5.75, 1, 12.4))
GATE_CLOSED = ([el([0, 3.5, 8.5], [16, 5, 9.5], "wood"), el([0, 10, 8.5], [16, 11.5, 9.5], "wood"),
                dict(el([1.5, 6.9, 8.6], [14.5, 7.9, 9.4], "wood"),
                     rot=("z", 22.5, [8, 7.4, 9]))]
               + [s for x in (0.5, 3.75, 7.0, 10.25, 13.5)
                  for s in picket(x, x + 1.75, 7.5, 8.5, 1, 12.4)]
               + [el([0, y, 8.3], [1.5, y + 1.9, 9.7], "metal") for y in (3.3, 9.8)]
               + [el([14.5, 7.4, 8.3], [16, 8.4, 9.7], "metal")])
GATE_OPEN = swing(GATE_CLOSED)

# --- stepping stones: three layouts, each turned four ways by the blockstate ---------------------
STONE_LAYOUTS = {
    "a": [(2, 1.5, 10, 8, 1.0), (6.5, 9.5, 14, 15, 0.9)],
    "b": [(1.5, 2, 8, 9.5, 1.0), (9.5, 5, 15, 12.5, 1.1), (3, 11, 8, 15, 0.85)],
    "c": [(3.5, 1, 11.5, 6.5, 1.0), (1.5, 8, 7.5, 15, 0.9), (9, 8.5, 14.5, 14, 1.1)],
}


def stepping(layout):
    return [s for x0, z0, x1, z1, h in STONE_LAYOUTS[layout]
            for s in rounded(x0, z0, x1, z1, 0, h, "flag")]


# --- string lights: a swag of wire across the top of the block, three bulbs on it ------------
_SAG = 4.6 * TAN
LIGHTS_WIRE = [dict(el([0, 14.85, 7.9], [5, 15.15, 8.1], "wire"), rot=("z", -22.5, [0, 15, 8])),
               el([4.55, 15 - _SAG - 0.15, 7.9], [11.45, 15 - _SAG + 0.15, 8.1], "wire"),
               dict(el([11, 14.85, 7.9], [16, 15.15, 8.1], "wire"),
                    rot=("z", 22.5, [16, 15, 8]))]
_BULB_AT = [(3.2, 15 - 3.2 * TAN, "b1"), (8, 15 - _SAG, "b2"), (12.8, 15 - 3.2 * TAN, "b3")]
LIGHTS_SOCKETS = [el([x - 0.45, y - 1.3, 7.55], [x + 0.45, y - 0.1, 8.45], "wire")
                  for x, y, _k in _BULB_AT]
LIGHTS_BULBS = [s for x, y, k in _BULB_AT for s in (
    el([x - 0.75, y - 3.1, 7.25], [x + 0.75, y - 1.3, 8.75], k),
    el([x - 0.45, y - 3.6, 7.55], [x + 0.45, y - 3.1, 8.45], k))]
LIGHTS_HOOK = [el([0, 14.2, 7.6], [0.5, 15.8, 8.4], "metal"),
               el([0.5, 14.2, 7.75], [1.0, 14.6, 8.25], "metal")]

# --- trampoline: mat 0.72 m up, a padded frame round the outside, legs at the corners ----------
TRAMP_MAT = [el([0, 11, 0], [16, 11.5, 16], "mat", ("up", "down"))]
TRAMP_SIDE = [el([3, 11, 0], [13, 12.25, 3], "pad", ("up", "north", "down")),
              el([3, 10, 0], [13, 11, 0.75], "frame", ("north", "south", "down"))]
_TRAMP_CORNER = [el([0, 11, 0], [3, 12.25, 3], "pad"),
                 el([0, 10, 0], [3, 11, 0.75], "frame", ("north", "down")),
                 el([0, 10, 0.75], [0.75, 11, 3], "frame", ("west", "down"))]
TRAMP_LEG = _TRAMP_CORNER + [leg([0.25, 0, 0.25], [1.5, 10, 1.5], tex="frame"),
                             el([0, 0, 0], [2, 0.5, 2], "frame")]
TRAMP_EXT_A = [el([0, 11, 0], [3, 12.25, 3], "pad", ("up", "north", "down")),
               el([0, 10, 0], [3, 11, 0.75], "frame", ("north", "south", "down"))]
TRAMP_EXT_B = [el([0, 11, 0], [3, 12.25, 3], "pad", ("up", "west", "down")),
               el([0, 10, 0], [0.75, 11, 3], "frame", ("east", "west", "down"))]

# --- bounce castle, drawn whole from its root, the middle of the floor ------------------------
CASTLE = {"floor_top": 5, "wall": 3, "wall_top": 29}


def castle():
    ft, w, wt = CASTLE["floor_top"], CASTLE["wall"], CASTLE["wall_top"]
    lo, hi = -16, 32
    out = [el([lo, 0, lo], [hi, ft, hi], "red", SIDES + ("down",)),
           el([lo, ft - 0.01, lo], [hi, ft, hi], "floor", ("up",))]
    out[1]["uv"]["up"] = [0, 0, 16, 16]

    def wall_x(x0, x1, z0, z1, mesh_z):
        """A wall running along x: a solid lower part, netting in panels between posts, a
        rolled top."""
        parts = [el([x0, ft, z0], [x1, 14, z1], "blue"),
                 el([x0, 26, z0], [x1, wt, z1], "yellow")]
        n = max(1, int(round((x1 - x0) / 14.0)))
        step = (x1 - x0) / n
        for i in range(n):
            a = x0 + i * step
            parts.append(full(el([a + 0.5, 14, mesh_z], [a + step - 0.5, 26, mesh_z], "mesh",
                                 ("north", "south")), ["north", "south"]))
            if i:
                parts.append(el([a - 1, 14, z0 + 0.5], [a + 1, 26, z1 - 0.5], "yellow"))
        return parts

    back = wall_x(-13, 29, 29, 32, 30.5)
    front_l = wall_x(-13, -4, -16, -13, -14.5)
    front_r = wall_x(20, 29, -16, -13, -14.5)
    arch = [el([-4, 26, -16], [20, wt, -13], "yellow"),
            el([-4, wt, -15.5], [20, 31, -13.5], "red")]
    # Side walls: the back wall turned to run along z, on each side.
    west = R.swap_xz([s for s in wall_x(-13, 29, -16, -13, -14.5) if not s["rot"]])
    east = R.swap_xz([s for s in wall_x(-13, 29, 29, 32, 30.5) if not s["rot"]])
    for s in west + east:
        if s["tex"] == "mesh":
            full(s, ["east", "west"])
    out += back + front_l + front_r + arch + west + east
    # Door posts either side of the doorway.
    out += [el([-5, ft, -16], [-3, 26, -13], "yellow"), el([19, ft, -16], [21, 26, -13], "yellow")]
    # Turrets at the corners, red with yellow caps.
    for cx in (-12, 28):
        for cz in (-12, 28):
            out += octagon(cx, cz, 3.5, 0, 29, "red", caps=())
            out += octagon(cx, cz, 3.9, 29, 30.5, "yellow")
            out += octagon(cx, cz, 2.2, 30.5, 31.9, "blue", caps=("up",))
    return out


CASTLE_MODEL = castle()

# --- diving board: a fibreglass board on a steel stand, its tip out past the front --------------
DIVING_BOARD = ([el([3.5, 0, 8], [12.5, 1, 16], "frame"),
                 el([4.5, 1, 9], [11.5, 6.5, 11], "frame"),
                 el([4.5, 1, 14], [11.5, 6.5, 15.5], "frame"),
                 el([4.75, 6.5, 10.5], [11.25, 7, 15], "rubber")]
                + [el([4.5, 7, -6], [11.5, 8, 16], "board", ALL, {"up": "grip"})])

# --- kiddie pool: two inflated rings round a shallow pool, 1 m across --------------------------
KIDDIE_POOL = (ring(8, 8, 6.6, 2.2, 0, 2.2, "vinyl")
               + ring(8, 8, 6.6, 2.2, 2.2, 4.4, "vinyl_b")
               + octagon(8, 8, 6.2, 0, 0.3, "pool_floor", caps=("up",))
               + octagon(8, 8, 5.9, 3.4, 3.45, "water", caps=("up",)))
FLOAT_RING = ring(8, 8, 4.2, 2.2, 0, 2.2, "float")

# --- pets ----------------------------------------------------------------------------------------
PET_BED = (octagon(8, 8, 6.8, 0, 0.6, "fabric_dark")
           + ring(8, 8, 5.6, 2.4, 0.6, 3.6, "fabric")
           + octagon(8, 8, 4.7, 0.6, 1.8, "cushion", caps=("up",)))


def bowl(cx, cz, content):
    return (octagon(cx, cz, 2.4, 0.3, 1.9, "bowl", caps=("down",))
            + octagon(cx, cz, 2.3, 1.55, 1.6, content, caps=("up",))
            + ring(cx, cz, 2.45, 0.6, 1.8, 2.2, "bowl"))


PET_BOWLS = ([el([1.5, 0, 4.5], [14.5, 0.3, 11.5], "rubber")]
             + bowl(5, 8, "kibble") + bowl(11, 8, "bowl_water"))
LITTER_BOX = [el([2.5, 0, 3.5], [13.5, 0.5, 12.5], "plastic"),
              el([2.5, 0.5, 3.5], [13.5, 3.5, 4.25], "plastic"),
              el([2.5, 0.5, 11.75], [13.5, 3.5, 12.5], "plastic"),
              el([2.5, 0.5, 4.25], [3.25, 3.5, 11.75], "plastic"),
              el([12.75, 0.5, 4.25], [13.5, 3.5, 11.75], "plastic"),
              el([3.25, 0.5, 4.25], [12.75, 2.2, 11.75], "litter", ("up",)),
              el([9.5, 3.5, 5], [12, 3.8, 7.5], "scoop"),
              el([12, 3.4, 5.8], [15, 3.7, 6.7], "scoop")]
CAT_TREE = ([el([1, 0, 1], [15, 1.5, 15], "carpet")]
            + [el([3.5, 1.5, 3.5], [6, 20, 6], "sisal", SIDES)]
            + [el([7, 1.5, 2], [15, 9, 10], "carpet", ALL, {"north": "cubby"}),
               full(el([7, 1.5, 1.99], [15, 9, 2], "cubby", ("north",)), ["north"]),
               el([6, 9, 1], [15, 10, 11], "carpet"),
               el([11, 10, 5.5], [13.5, 22, 8], "sisal", SIDES),
               el([1, 20, 2], [9, 21, 10], "carpet"),
               el([7.5, 22, 3], [15, 23, 11], "carpet")]
            + ring(11.25, 7, 3.2, 1.2, 23, 24.5, "carpet")
            + [el([3.9, 15, 7.9], [4.1, 20, 8.1], "sisal", SIDES),
               el([3.3, 13.8, 7.3], [4.7, 15.2, 8.7], "toy")])

# --- hose reel on the wall, over its tap ------------------------------------------------------------
HOSE_REEL = ([el([3, 3, 15], [13, 14, 16], "reel", NO_BACK),
              el([2.5, 6, 9.5], [3.5, 11, 15], "reel"),
              el([12.5, 6, 9.5], [13.5, 11, 15], "reel")]
             + octagon_x(8.5, 12, 3.3, 4, 12, "hose", caps=())
             + octagon_x(8.5, 12, 4.3, 3.5, 4, "reel")
             + octagon_x(8.5, 12, 4.3, 12, 12.5, "reel")
             + [el([12.5, 8.1, 11.6], [14, 8.9, 12.4], "reel"),
                el([13.3, 8.3, 9.2], [14, 9.1, 12], "reel"),
                el([7.5, 1.5, 8.2], [8.5, 8.5, 9.2], "hose_green"),
                el([7.3, 0.4, 8], [8.7, 1.5, 9.4], "metal"),
                el([7.3, 1.5, 13.8], [8.7, 2.9, 16], "brass"),
                el([7.6, 2.9, 14.3], [8.4, 3.6, 15], "brass")])

# --- outdoor wall light: a gooseneck arm and an enamel dish shade ---------------------------------
WALL_LIGHT = (octagon_z(8, 12, 2.2, 15.4, 16, "metal", caps=("north",))
              + [el([7.6, 11.6, 9.4], [8.4, 12.4, 15.4], "metal"),
                 el([7.6, 9.6, 8.6], [8.4, 12.4, 9.4], "metal")]
              + octagon(8, 9, 1.1, 8.8, 9.6, "metal")
              + octagon(8, 9, 2.6, 8.1, 8.8, "metal")
              + octagon(8, 9, 3.8, 7.5, 8.1, "metal", caps=("up",))
              + octagon(8, 9, 4.6, 7.0, 7.5, "metal", caps=("up",))
              + octagon(8, 9, 3.7, 7.35, 7.4, "lens", caps=("down",)))


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
def fin(fid, tex, *names):
    return (fid, dict(tex)) + names


TEAK = fin("teak", {"wood": T("teak"), "wood_v": T("teak_v"), "edge": T("teak_edge"),
                    "slats": T("teak_slats")}, "Teak", "Teak", "teca", "teak")
BLACK = fin("black", {"wood": T("powder_black"), "wood_v": T("powder_black"),
                      "edge": T("powder_black_edge"), "slats": T("powder_black_slats")},
            "Black", "Schwarz", "negro", "svart")
WHITE = fin("white", {"wood": T("paint_white"), "wood_v": T("paint_white_v"),
                      "edge": T("paint_white_edge")}, "White", "Weiß", "blanco", "vit")
OUT_FABRICS = [fin(k, {"fabric": T(k), "fabric_dark": T(k + "_dark")}, *n) for k, n in (
    ("sand", ("Sand", "Sand", "arena", "sand")),
    ("slate", ("Slate", "Schiefergrau", "pizarra", "skiffergrå")),
    ("teal", ("Teal", "Petrol", "verde azulado", "petrol")))]
CANOPIES = [fin(k, {"canopy": T("canopy_" + k), "canopy_dark": T("canopy_%s_dark" % k)}, *n)
            for k, n in (("cream", ("Cream", "Creme", "crema", "gräddvit")),
                         ("navy", ("Navy", "Marineblau", "azul marino", "marinblå")))]

PIECE_CLOTH = "Material.CLOTH, SoundType.CLOTH, 0.5F, BlockRenderLayer.SOLID"
PIECE_PLASTIC = "Material.WOOD, SoundType.WOOD, 0.8F, BlockRenderLayer.SOLID"
PIECE_METAL = "Material.IRON, SoundType.METAL, 1.0F, BlockRenderLayer.SOLID"
DECOR = 'new BlockLivingDecor("%%s", new int[]{%%s}, %s)'

# (piece, kind, finishes, names, spec)
PIECES = [
    # ---- patio ----
    ("patio_table", "table", [TEAK, BLACK],
     ("Patio Table", "Terrassentisch", "Mesa de patio", "Uteplatsbord"),
     {"parts": {"top": PATIO_TOP, "apron": PATIO_APRON, "leg": PATIO_LEG,
                "apron_a": PATIO_APRON_A, "apron_b": PATIO_APRON_B},
      "particle": "wood", "java": 'new BlockDiningTable("%s")'}),
    ("patio_chair", "single", [TEAK, BLACK],
     ("Patio Chair", "Terrassenstuhl", "Silla de patio", "Uteplatsstol"),
     {"geo": PATIO_CHAIR, "particle": "wood",
      "java": 'new BlockResidentialFurniture("%s", new int[]{%s}, false, 7.25, 0.5, 0)'}),
    ("patio_umbrella", "umbrella", CANOPIES,
     ("Patio Umbrella", "Sonnenschirm", "Sombrilla de patio", "Parasoll"),
     {"particle": "canopy", "box": [6, 0, 6, 10, 32, 10],
      "java": 'new BlockPatioUmbrella("%s", new int[]{%s})'}),
    ("table_umbrella", "table_umbrella", CANOPIES,
     ("Table Umbrella", "Tischschirm", "Sombrilla de mesa", "Bordsparasoll"),
     {"particle": "canopy", "box": [7, 0, 7, 9, 16, 9],
      "java": 'new BlockTableUmbrella("%s", new int[]{%s})'}),
    ("sun_lounger", "wide", [TEAK, BLACK],
     ("Sun Lounger", "Sonnenliege", "Tumbona", "Solsäng"),
     {"geo": LOUNGER, "particle": "wood", "box": [0, 0, 2, 32, 9, 14],
      "java": 'new BlockSunLounger("%s", new int[]{%s})'}),
    ("adirondack_chair", "single", [TEAK, WHITE],
     ("Adirondack Chair", "Adirondack-Stuhl", "Silla Adirondack", "Adirondackstol"),
     {"geo": ADIRONDACK, "particle": "wood", "box": [1, 0, 2, 15, 16, 16],
      "java": 'new BlockResidentialFurniture("%s", new int[]{%s}, false, 5.75, 0.5, 0)'}),
    ("outdoor_sofa", "sofa", OUT_FABRICS,
     ("Outdoor Sofa", "Outdoor-Sofa", "Sofá de exterior", "Utomhussoffa"),
     {"particle": "fabric", "java": 'new BlockSofa("%s", new int[]{0, 0, 1, 16, 14, 16})'}),
    ("outdoor_sofa_corner", "corner", OUT_FABRICS,
     ("Outdoor Sofa Corner", "Outdoor-Sofa-Eckelement", "Rinconera de exterior",
      "Utomhussoffhörn"),
     {"particle": "fabric", "java": 'new BlockSofaCorner("%s")'}),
    ("outdoor_rug", "rug",
     [fin("blue", {"rug": T("rug_blue"), "rug_border": T("rug_border_blue")}, "Blue Stripe",
          "blau gestreift", "rayas azules", "blårandig"),
      fin("sand", {"rug": T("rug_sand"), "rug_border": T("rug_border_sand")}, "Sand Stripe",
          "sandfarben gestreift", "rayas arena", "sandrandig")],
     ("Outdoor Rug", "Outdoor-Teppich", "Alfombra de exterior", "Utomhusmatta"),
     {"java": 'new BlockRug("%s")'}),
    # ---- cooking and fire ----
    ("gas_grill", "grill",
     [fin("black", {"enamel": T("enamel_black")}, "Black", "Schwarz", "negro", "svart"),
      fin("stainless", {"enamel": RT("stainless")}, "Stainless Steel", "Edelstahl",
          "acero inoxidable", "rostfritt stål")],
     ("Gas Grill", "Gasgrill", "Parrilla de gas", "Gasolgrill"),
     {"geo": GAS_GRILL, "particle": "enamel", "vent": GRILL_VENT,
      "java": 'new BlockOutdoorGrill("%s", new int[]{%s}, OutdoorAppliances.GAS_GRILL, '
              'new double[]{%s})'}),
    ("kettle_grill", "grill",
     [fin("black", {"enamel": T("enamel_black")}, "Black", "Schwarz", "negro", "svart"),
      fin("red", {"enamel": T("enamel_red")}, "Red", "Rot", "rojo", "röd")],
     ("Charcoal Kettle Grill", "Holzkohle-Kugelgrill", "Parrilla de carbón",
      "Kolgrill"),
     {"geo": KETTLE_GRILL, "particle": "enamel", "vent": KETTLE_VENT,
      "java": 'new BlockOutdoorGrill("%s", new int[]{%s}, OutdoorAppliances.CHARCOAL_GRILL, '
              'new double[]{%s})'}),
    ("fire_pit", "fire",
     [fin("stone", {}, "Stone", "Stein", "piedra", "sten"),
      fin("steel", {}, "Steel", "Stahl", "acero", "stål")],
     ("Fire Pit", "Feuerstelle", "Brasero", "Eldstad"),
     {"geos": {"stone": FIRE_PIT_STONE, "steel": FIRE_PIT_STEEL},
      "particles": {"stone": "stone", "steel": "steel"},
      "java": 'new BlockFirePit("%s", new int[]{%s}, %s, %s, 14)'}),
    ("cooler", "storage",
     [fin("blue", {"shell": T("cooler_blue")}, "Blue", "Blau", "azul", "blå"),
      fin("red", {"shell": T("cooler_red")}, "Red", "Rot", "rojo", "röd")],
     ("Cooler", "Kühlbox", "Nevera portátil", "Kylbox"),
     {"geo": COOLER, "particle": "shell",
      "java": 'new BlockResidentialStorage("%s", new int[]{%s}, 18, '
              'FurnishingsSounds.FRIDGE_OPEN, FurnishingsSounds.FRIDGE_CLOSE)'}),
    ("chimney", "chimney", [fin("brick", {}, "Brick", "Backstein", "ladrillo", "tegel")],
     ("Chimney Stack", "Schornstein", "Tiro de chimenea", "Skorsten"),
     {"particle": "brick", "box": [2, 0, 2, 14, 16, 14],
      "java": 'new BlockChimney("%s", new int[]{%s})'}),
    # ---- garden ----
    ("picket_fence", "fence", [WHITE],
     ("Picket Fence", "Lattenzaun", "Valla de estacas", "Staket"),
     {"particle": "wood_v", "java": 'new BlockPicketFence("%s")'}),
    ("picket_gate", "gate", [WHITE],
     ("Picket Gate", "Lattenzauntor", "Puerta de valla de estacas", "Staketgrind"),
     {"particle": "wood_v", "java": 'new BlockPicketGate("%s")'}),
    ("stepping_stones", "stones",
     [fin("grey", {"flag": T("flagstone_grey")}, "Grey", "Grau", "gris", "grå"),
      fin("slate", {"flag": T("flagstone_slate")}, "Slate", "Schiefer", "pizarra", "skiffer")],
     ("Stepping Stones", "Trittsteine", "Piedras de paso", "Stegplattor"),
     {"particle": "flag", "java": 'new BlockSteppingStones("%s")'}),
    ("string_lights", "lights",
     [fin("warm", {"b1": ("bulb_warm_on", "bulb_warm_off"), "b2": ("bulb_warm_on",
                                                                  "bulb_warm_off"),
                   "b3": ("bulb_warm_on", "bulb_warm_off")},
          "Warm White", "Warmweiß", "blanco cálido", "varmvit"),
      fin("multicolour", {"b1": ("bulb_red_on", "bulb_red_off"),
                          "b2": ("bulb_green_on", "bulb_green_off"),
                          "b3": ("bulb_blue_on", "bulb_blue_off")},
          "Multicolour", "Bunt", "multicolor", "flerfärgad")],
     ("String Lights", "Lichterkette", "Guirnalda de luces", "Ljusslinga"),
     {"particle": "wire", "box": [0, 10, 7, 16, 16, 9],
      "java": 'new BlockStringLights("%s", new int[]{%s})'}),
    # ---- backyard fun ----
    ("trampoline", "table",
     [fin("blue", {"pad": T("pad_blue")}, "Blue", "Blau", "azul", "blå"),
      fin("green", {"pad": T("pad_green")}, "Green", "Grün", "verde", "grön")],
     ("Trampoline", "Trampolin", "Cama elástica", "Studsmatta"),
     {"parts": {"top": TRAMP_MAT, "apron": TRAMP_SIDE, "leg": TRAMP_LEG,
                "apron_a": TRAMP_EXT_A, "apron_b": TRAMP_EXT_B},
      "particle": "pad", "java": 'new BlockTrampoline("%s")'}),
    ("bounce_castle", "castle",
     [fin("red", {}, "Red", "Rot", "rojo", "röd")],
     ("Bounce Castle", "Hüpfburg", "Castillo hinchable", "Hoppborg"),
     {"particle": "red", "box": [0, 0, 0, 16, 5, 16],
      "java": 'new BlockBounceCastle("%s", new int[]{%s})'}),
    ("diving_board", "single", [fin("white", {}, "White", "Weiß", "blanco", "vit")],
     ("Diving Board", "Sprungbrett", "Trampolín", "Svikt"),
     {"geo": DIVING_BOARD, "particle": "board", "box": [4, 0, 0, 12, 8, 16],
      "java": 'new BlockDivingBoard("%s", new int[]{%s})'}),
    ("kiddie_pool", "single", [fin("blue", {}, "Blue", "Blau", "azul", "blå")],
     ("Kiddie Pool", "Planschbecken", "Piscina infantil", "Plaskpool"),
     {"geo": KIDDIE_POOL, "particle": "vinyl", "box": [0, 0, 0, 16, 4, 16],
      "java": 'new BlockResidentialFurniture("%s", new int[]{%s}, true, 3.5, 0, 0)'}),
    ("pool_float", "single",
     [fin("pink", {"float": T("float_pink")}, "Pink", "Rosa", "rosa", "rosa"),
      fin("yellow", {"float": T("float_yellow")}, "Yellow", "Gelb", "amarillo", "gul")],
     ("Pool Float Ring", "Schwimmring", "Flotador", "Badring"),
     {"geo": FLOAT_RING, "particle": "float", "java": DECOR % PIECE_CLOTH}),
    # ---- pets ----
    ("pet_bed", "single",
     [(f, dict(t), *n) for f, t, *n in R.FABRICS if f in ("charcoal", "navy", "red")],
     ("Pet Bed", "Haustierbett", "Cama para mascotas", "Husdjursbädd"),
     {"geo": PET_BED, "particle": "fabric", "java": DECOR % PIECE_CLOTH}),
    ("pet_bowls", "single",
     [fin("steel", {"bowl": RT("stainless")}, "Steel", "Edelstahl", "acero", "stål"),
      fin("red", {"bowl": RT("ceramic_red")}, "Red", "Rot", "rojo", "röd")],
     ("Pet Bowls", "Futternäpfe", "Comederos para mascotas", "Matskålar"),
     {"geo": PET_BOWLS, "particle": "bowl", "java": DECOR % PIECE_METAL}),
    ("litter_box", "single", [fin("grey", {}, "Grey", "Grau", "gris", "grå")],
     ("Litter Box", "Katzenklo", "Arenero", "Kattlåda"),
     {"geo": LITTER_BOX, "particle": "plastic", "java": DECOR % PIECE_PLASTIC}),
    ("cat_tree", "tall",
     [fin("beige", {"carpet": T("carpet_beige"), "cubby": T("cubby_beige")}, "Beige", "Beige",
          "beige", "beige"),
      fin("grey", {"carpet": T("carpet_grey"), "cubby": T("cubby_grey")}, "Grey", "Grau",
          "gris", "grå")],
     ("Cat Tree", "Kratzbaum", "Rascador para gatos", "Klösträd"),
     {"geo": CAT_TREE, "particle": "carpet", "box": [1, 0, 1, 15, 25, 15],
      "java": 'new BlockResidentialTall("%s", new int[]{%s}, true, 0, null, null)'}),
    # ---- around the yard ----
    ("hose_reel", "single", [fin("green", {}, "Green", "Grün", "verde", "grön")],
     ("Garden Hose Reel", "Schlauchtrommel", "Enrollador de manguera", "Slangvinda"),
     {"geo": HOSE_REEL, "particle": "reel", "box": [2, 0, 8, 14, 14, 16],
      "java": 'new BlockHoseReel("%s", new int[]{%s})'}),
    ("outdoor_wall_light", "lamp",
     [fin("black", {"metal": T("powder_black")}, "Black", "Schwarz", "negro", "svart"),
      fin("galvanized", {"metal": T("galvanized")}, "Galvanized", "Verzinkt", "galvanizado",
          "galvaniserad")],
     ("Outdoor Wall Light", "Außenwandleuchte", "Aplique de exterior", "Utomhusvägglampa"),
     {"geo": WALL_LIGHT, "particle": "metal", "box": [3, 6, 4, 13, 15, 16],
      "java": 'new BlockKitchenLight("%s", new int[]{%s}, 12)'}),
]
PIECE = {p: (kind, fins, names, spec) for p, kind, fins, names, spec in PIECES}
GROUPS = {"patio_table": "Outdoor & Backyard: patio",
          "gas_grill": "Outdoor & Backyard: cooking and fire",
          "picket_fence": "Outdoor & Backyard: garden",
          "trampoline": "Outdoor & Backyard: backyard fun",
          "pet_bed": "Outdoor & Backyard: pets",
          "hose_reel": "Outdoor & Backyard: around the yard"}
PART_BLOCK = "bounce_castle_part"


def names_with(names, fnames):
    return ["%s (%s)" % (n, f) for n, f in zip(names, fnames)]


def java_for(piece, kind, spec, reg, fid):
    j = spec["java"]
    if kind in ("table", "sofa", "corner", "rug", "fence", "gate", "stones"):
        return j % reg
    if kind == "fire":
        geo = spec["geos"][fid]
        return j % (reg, jbox(box_of(geo)), "true" if fid == "steel" else "false",
                    R._num(FIRE_Y[fid]))
    if kind == "grill":
        return j % (reg, jbox(spec.get("box") or box_of(spec["geo"])), jbox(spec["vent"]))
    if kind == "wide":
        return j % (reg, jbox(spec["box"]))
    box = spec.get("box") or box_of(spec["geo"])
    return j % (reg, jbox(box))


def entries():
    """Every block: (registry, piece, kind, finish id, finish textures, names, java)."""
    out = []
    for piece, kind, fins, names, spec in PIECES:
        for fid, ftex, *fnames in fins:
            reg = "%s_%s" % (piece, fid)
            out.append((reg, piece, kind, fid, dict(ftex), names_with(names, fnames),
                        java_for(piece, kind, spec, reg, fid)))
    return out


# ------------------------------------------------------------------------------------------
# Models
# ------------------------------------------------------------------------------------------
def item_display(specs):
    lo, hi = B.extent(specs)
    if max(hi[i] - lo[i] for i in range(3)) > 16.5 or min(lo) < -0.5:
        return B.big_display(B.centred(specs))
    return None


def base_models():
    """Every base geometry model: (name, geometry json)."""
    out = []
    for piece, kind, _fins, _names, spec in PIECES:
        particle = spec.get("particle")
        if kind in ("single", "grill", "lamp", "storage"):
            geo = spec["geo"]
            out.append((piece, geometry(geo, particle, display=item_display(geo))))
        elif kind == "table":
            for part, geo in spec["parts"].items():
                out.append(("%s_%s" % (piece, part), geometry(geo, particle)))
            parts = spec["parts"]
            item = parts["top"] + [s for r in (0, 90, 180, 270)
                                   for s in R.turn(parts["apron"] + parts["leg"], r)]
            out.append(("%s_item" % piece, geometry(item, particle)))
        elif kind == "sofa":
            out.append(("%s_body" % piece, geometry(OSOFA_BODY, particle)))
            out.append(("%s_left" % piece, geometry(OSOFA_ARM, particle)))
            out.append(("%s_right" % piece, geometry(R.mirror_x(OSOFA_ARM), particle)))
            out.append(("%s_item" % piece, geometry(OSOFA_BODY + OSOFA_ARM
                                                    + R.mirror_x(OSOFA_ARM), particle)))
        elif kind == "corner":
            out.append(("%s_body" % piece, geometry(OCORNER_BODY, particle)))
            out.append(("%s_left" % piece, geometry(OSOFA_ARM, particle)))
            out.append(("%s_front" % piece, geometry(R.swap_xz(OSOFA_ARM), particle)))
            out.append(("%s_caps" % piece, geometry(OCORNER_CAPS, particle)))
            out.append(("%s_item" % piece, geometry(OCORNER_BODY + OCORNER_CAPS, particle)))
        elif kind == "umbrella":
            out.append(("patio_umbrella_lower", geometry(UMBRELLA_LOWER, "pole")))
            out.append(("patio_umbrella_open", geometry(UMBRELLA_OPEN, particle)))
            out.append(("patio_umbrella_closed", geometry(UMBRELLA_CLOSED, particle)))
            # Moved down so that the whole umbrella fits the -16..32 a model may reach.
            whole = move(UMBRELLA_LOWER + move(UMBRELLA_OPEN, 0, 16), 0, -11)
            out.append(("patio_umbrella_item", geometry(whole, particle,
                                                        display=B.big_display(whole))))
        elif kind == "table_umbrella":
            op = TABLE_UMBRELLA_EXTRA + UMBRELLA_OPEN
            cl = TABLE_UMBRELLA_EXTRA + UMBRELLA_CLOSED
            out.append(("table_umbrella_open", geometry(op, particle)))
            out.append(("table_umbrella_closed", geometry(cl, particle)))
            out.append(("table_umbrella_item", geometry(op, particle,
                                                        display=B.big_display(op))))
        elif kind == "wide":
            geo = spec["geo"]
            for i, cell in enumerate(L.cut_wide(geo)):
                out.append(("%s_cell%d" % (piece, i), geometry(cell, particle)))
            item = move(geo, -8)
            out.append(("%s_item" % piece, geometry(item, particle,
                                                    display=B.big_display(item))))
        elif kind == "tall":
            geo = spec["geo"]
            lower = B.clip(geo, 1, 0, 16)
            upper = move(B.clip(geo, 1, 16, 48), 0, -16)
            out.append(("%s_lower" % piece, geometry(lower, particle)))
            out.append(("%s_upper" % piece, geometry(upper, particle)))
            out.append(("%s_item" % piece, geometry(geo, particle,
                                                    display=B.big_display(geo))))
        elif kind == "fire":
            for fid, geo in spec["geos"].items():
                p = spec["particles"][fid]
                out.append(("fire_pit_%s" % fid, geometry(geo, p)))
        elif kind == "chimney":
            out.append(("chimney_column", geometry(CHIMNEY_COLUMN, particle)))
            out.append(("chimney_top", geometry(CHIMNEY_TOP, particle)))
        elif kind == "fence":
            out.append(("picket_fence_post", geometry(FENCE_POST, particle)))
            out.append(("picket_fence_side", geometry(FENCE_SIDE, particle)))
            item = FENCE_POST + FENCE_SIDE + R.turn(FENCE_SIDE, 180)
            out.append(("picket_fence_item", geometry(item, particle)))
        elif kind == "gate":
            out.append(("picket_gate_closed", geometry(GATE_CLOSED, particle)))
            out.append(("picket_gate_open", geometry(GATE_OPEN, particle)))
        elif kind == "stones":
            for layout in sorted(STONE_LAYOUTS):
                out.append(("stepping_stones_%s" % layout, geometry(stepping(layout),
                                                                    particle)))
        elif kind == "lights":
            out.append(("string_lights_strand", geometry(LIGHTS_WIRE + LIGHTS_SOCKETS,
                                                         particle)))
            out.append(("string_lights_bulbs", geometry(LIGHTS_BULBS, "b2")))
            out.append(("string_lights_hook_left", geometry(LIGHTS_HOOK, "metal")))
            out.append(("string_lights_hook_right", geometry(R.mirror_x(LIGHTS_HOOK), "metal")))
            item = LIGHTS_WIRE + LIGHTS_SOCKETS + LIGHTS_BULBS + LIGHTS_HOOK + R.mirror_x(
                LIGHTS_HOOK)
            out.append(("string_lights_item", geometry(item, particle)))
        elif kind == "castle":
            out.append(("bounce_castle", geometry(CASTLE_MODEL, particle)))
            out.append(("bounce_castle_item", geometry(CASTLE_MODEL, particle,
                                                       display=B.big_display(CASTLE_MODEL))))
        elif kind == "rug":
            pass
        else:
            raise ValueError(kind)
    return out


# ------------------------------------------------------------------------------------------
# Blockstates
# ------------------------------------------------------------------------------------------
facing_variants = L.facing_variants
empty = L.empty
forge = L.forge
switch_tex = L.switch_tex


def multipart(reg, rules):
    parts = []
    for part, when, r, *extra in rules:
        apply = {"model": MODEL + "%s_%s" % (reg, part)}
        if r:
            apply["y"] = r
        if extra and extra[0]:
            apply["uvlock"] = True
        parts.append({"when": when, "apply": apply} if when else {"apply": apply})
    return {"multipart": parts}


SOFA_RULES = R.faced([("body", {}), ("left", {"left": "false"}), ("right", {"right": "false"})])
CORNER_RULES = R.faced([("body", {}), ("left", {"left": "false", "front": "true"}),
                        ("front", {"front": "false", "left": "true"}),
                        ("caps", {"left": "false", "front": "false"})])
LIGHT_RULES = R.faced([("strand", {}), ("bulbs_on", {"lit": "true"}),
                       ("bulbs_off", {"lit": "false"}), ("hook_left", {"left": "false"}),
                       ("hook_right", {"right": "false"})])
FENCE_RULES = ([("post", {}, 0)]
               + [("side", {side: "true"}, r, True) for side, r in ROT.items()])


def umbrella_state(ftex):
    tex = dict(ftex)
    state = forge(BASE + "patio_umbrella_lower", tex, {}, BASE + "patio_umbrella_item")
    for f, r in sorted(ROT.items()):
        for open_ in ("true", "false"):
            for upper in ("true", "false"):
                model = ("patio_umbrella_lower" if upper == "false" else
                         "patio_umbrella_open" if open_ == "true" else "patio_umbrella_closed")
                v = {"model": BASE + model}
                if r:
                    v["y"] = r
                state["variants"]["facing=%s,open=%s,upper=%s" % (f, open_, upper)] = [v]
    return state


def stones_state(reg):
    variants = []
    for layout in sorted(STONE_LAYOUTS):
        for r in (0, 90, 180, 270):
            v = {"model": MODEL + "%s_%s" % (reg, layout)}
            if r:
                v["y"] = r
            variants.append(v)
    return {"variants": {"normal": variants}}


EXTRA_LANG = {
    "tile.%s.name" % PART_BLOCK: ("Bounce Castle Part", "Hüpfburg-Teil",
                                  "Parte del castillo hinchable", "Hoppborgsdel"),
}


def lang_entries():
    out = {loc: {} for loc in R.LOCALES}
    for key, names in EXTRA_LANG.items():
        for i, loc in enumerate(R.LOCALES):
            out[loc][key] = names[i]
    for reg, _p, _k, _f, _t, names, _j in entries():
        for i, loc in enumerate(R.LOCALES):
            out[loc]["tile.%s.name" % reg] = names[i]
    return out


PART_MODEL = {"parent": "block/block", "textures": {"particle": T("vinyl_red")}, "elements": []}
PART_STATE = {"forge_marker": 1, "defaults": {"model": BASE + PART_BLOCK},
              "variants": {"index": empty(str(i) for i in range(16)), "inventory": [{}]}}


def generate(assets):
    written = []

    def dump(rel, data):
        R.dump(os.path.join(assets, rel), data)
        written.append(rel)

    def copy_model(rel, parent, ftex, base_sub=SUB):
        dump(rel, {"parent": "csm:block/%s/base/%s" % (base_sub, parent), "textures": dict(ftex)})

    for name, (draw, frametime) in TEXTURES.items():
        rel = "textures/blocks/%s/%s.png" % (SUB, name)
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
        if frametime:
            dump(rel + ".mcmeta", {"animation": {"frametime": frametime}})
    models = base_models()
    particles = {}
    for name, data in models:
        dump("models/block/%s/base/%s.json" % (SUB, name), data)
        particles[name] = data["textures"]["particle"][1:]
    dump("models/block/%s/base/%s.json" % (SUB, PART_BLOCK), PART_MODEL)
    dump("blockstates/%s.json" % PART_BLOCK, PART_STATE)
    for reg, piece, kind, fid, ftex, _names, _java in entries():
        spec = PIECE[piece][3]
        if kind == "lights":
            on = {k: T(v[0]) for k, v in ftex.items()}
            off = {k: T(v[1]) for k, v in ftex.items()}
            ftex = {}
        # Every finish names the texture its models' particle points to, or the game logs an
        # upward reference and draws the missing texture.
        for name, key in particles.items():
            if name == piece or name.startswith(piece + "_"):
                ftex.setdefault(key, DEFAULT_TEX[key])
        blk = "models/block/%s/%s_%%s.json" % (SUB, reg)
        item = "models/item/%s.json" % reg
        if kind == "single":
            state = forge(BASE + piece, ftex, {"facing": facing_variants()})
        elif kind == "storage":
            copy_model(blk % "body", piece, ftex)
            copy_model(item, piece, ftex)
            state = multipart(reg, R.faced([("body", {})]))
        elif kind == "grill":
            tex = dict(ftex, glow=T("grill_glow_off"))
            state = forge(BASE + piece, tex, {"facing": facing_variants(),
                                              "running": switch_tex("glow", T("grill_glow_off"),
                                                                    T("grill_glow_on"))})
        elif kind == "lamp":
            tex = dict(ftex, lens=RT("lens_on"))
            state = forge(BASE + piece, tex, {"facing": facing_variants(),
                                              "lit": switch_tex("lens", RT("lens_off"),
                                                                RT("lens_on")),
                                              "powered": empty(("true", "false"))})
        elif kind == "fire":
            tex = dict(ftex, flame=LT("clear"), embers=LT("embers_off"))
            tex[spec["particles"][fid]] = DEFAULT_TEX[spec["particles"][fid]]
            state = forge(BASE + "fire_pit_%s" % fid, tex, {
                "facing": facing_variants(),
                "lit": {"true": {"textures": {"flame": LT("flame"), "embers": LT("embers_on")}},
                        "false": {"textures": {"flame": LT("clear"),
                                               "embers": LT("embers_off")}}},
                "powered": empty(("true", "false"))},
                item_tex={"flame": LT("flame"), "embers": LT("embers_on")})
        elif kind == "chimney":
            tex = dict(ftex, brick=T("brick_red"))
            state = forge(BASE + "chimney_top", tex, {
                "facing": facing_variants(),
                "up": {"true": {"model": BASE + "chimney_column"},
                       "false": {"model": BASE + "chimney_top"}},
                "lit": empty(("true", "false")), "powered": empty(("true", "false"))})
        elif kind == "table":
            for part in spec["parts"]:
                copy_model(blk % part, "%s_%s" % (piece, part), ftex)
            copy_model(item, "%s_item" % piece, ftex)
            state = multipart(reg, R.table_rules())
        elif kind == "sofa":
            for part in ("body", "left", "right"):
                copy_model(blk % part, "%s_%s" % (piece, part), ftex)
            copy_model(item, "%s_item" % piece, ftex)
            state = multipart(reg, SOFA_RULES)
        elif kind == "corner":
            for part in ("body", "left", "front", "caps"):
                copy_model(blk % part, "%s_%s" % (piece, part), ftex)
            copy_model(item, "%s_item" % piece, ftex)
            state = multipart(reg, CORNER_RULES)
        elif kind == "rug":
            for part in ("field", "side", "corner"):
                copy_model(blk % part, "rug_%s" % part, ftex, R.SUB)
            copy_model(item, "rug_item", ftex, R.SUB)
            state = multipart(reg, B.rug_rules())
        elif kind == "umbrella":
            state = umbrella_state(ftex)
        elif kind == "table_umbrella":
            state = forge(BASE + "table_umbrella_open", ftex, {
                "facing": facing_variants(),
                "open": {"true": {"model": BASE + "table_umbrella_open"},
                         "false": {"model": BASE + "table_umbrella_closed"}}},
                BASE + "table_umbrella_item")
        elif kind == "wide":
            state = forge(BASE + piece + "_cell0", ftex, {
                "facing": facing_variants(),
                "part": {str(p): {"model": BASE + "%s_cell%d" % (piece, p)} for p in (0, 1)}},
                BASE + piece + "_item")
        elif kind == "tall":
            state = forge(BASE + piece + "_lower", ftex, {
                "facing": facing_variants(),
                "upper": {"false": {"model": BASE + piece + "_lower"},
                          "true": {"model": BASE + piece + "_upper"}}},
                BASE + piece + "_item")
        elif kind == "fence":
            for part in ("post", "side"):
                copy_model(blk % part, "picket_fence_%s" % part, ftex)
            copy_model(item, "picket_fence_item", ftex)
            state = multipart(reg, FENCE_RULES)
        elif kind == "gate":
            state = forge(BASE + "picket_gate_closed", ftex, {
                "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                           "west": {"y": 270}},
                "open": {"false": {"model": BASE + "picket_gate_closed"},
                         "true": {"model": BASE + "picket_gate_open"}},
                "in_wall": empty(("true", "false")), "powered": empty(("true", "false"))})
        elif kind == "stones":
            for layout in sorted(STONE_LAYOUTS):
                copy_model(blk % layout, "stepping_stones_%s" % layout, ftex)
            copy_model(item, "stepping_stones_a", ftex)
            state = stones_state(reg)
        elif kind == "lights":
            base_tex = {"wire": T("wire_black"), "metal": T("powder_black")}
            copy_model(blk % "strand", "string_lights_strand", base_tex)
            copy_model(blk % "bulbs_on", "string_lights_bulbs", on)
            copy_model(blk % "bulbs_off", "string_lights_bulbs", off)
            copy_model(blk % "hook_left", "string_lights_hook_left", base_tex)
            copy_model(blk % "hook_right", "string_lights_hook_right", base_tex)
            copy_model(item, "string_lights_item", dict(base_tex, **on))
            state = multipart(reg, LIGHT_RULES)
        elif kind == "castle":
            state = forge(BASE + "bounce_castle", ftex, {"facing": facing_variants()},
                          BASE + "bounce_castle_item")
        else:
            raise ValueError(kind)
        dump("blockstates/%s.json" % reg, state)
    R.write_lang(os.path.join(assets, "lang"), lang_entries())
    written += ["lang/%s.lang" % loc for loc in R.LOCALES]
    return written


def fragments():
    lines = []
    last = None
    for reg, piece, _k, _f, _t, names, java in entries():
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
    tmp = tempfile.mkdtemp(prefix="outdoor_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not R.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("outdoor furniture is up to date (%d files)" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
