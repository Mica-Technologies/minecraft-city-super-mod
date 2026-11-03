#!/usr/bin/env python3
"""
gen_furniture_lodge.py -- the Residential tab's lodge set in the Furniture & Novelties module, for
a mountain lodge or a hotel lobby, lounge and breakfast room: a canoe hung from the ceiling, an
antler chandelier, a wrought-iron drum pendant with an alabaster bowl, crossed snowshoes, crossed
skis and a framed trout print for the walls, a bronze horse for a table top (standing and
rearing), a glass sneeze guard for a buffet counter and a glass beverage dispenser.

The brown leather sofas and armchair (gen_furniture_residential.py), the louvred buffet cabinet
and the cherry and tan granite finish (gen_furniture_kitchen.py), the forest-green beds and the
Southwest rug (gen_furniture_bedroom.py) are drawn by the generators of their families; this one
draws only what has no family of its own.

Borrows gen_furniture_residential.py's element helpers and output, gen_furniture_kitchen.py's
chrome, and gen_furniture_appliances.py's surfaces (a countertop piece is drawn once per
SurfaceRest, dropped to stand on what is under it), imported, not copied, and writes, under
modules/furnishings/src/main/resources/assets/csm:

  * textures/blocks/furniture/lodge/*.png  cedar strip and painted canvas hulls, rope, antler,
    wrought iron, alabaster (lit and unlit), candle sleeves, snowshoe lacing, ski, the trout
    print, bronze, a black marble plinth, sneeze guard glass and the dispensers' drinks
  * models/block/furniture/lodge/base/*.json  the geometry
  * models/block/furniture/lodge/<registry>_<part>.json  a finish's copy of each part the canoe's
    multipart picks
  * blockstates/<registry>.json, and models/item/<registry>.json for the canoe
  * the tile names in all four languages, by key

Every model faces north with its back at +Z, as the rest of the Residential set. What each piece
is, in Java:

  * the canoe is a BlockResidentialRun: canoes placed side by side, facing the same way, join
    into one long canoe, a bow drawn where the run stops at either end and rope slings round its
    hull just inside each bow (a run of one is a short canoe with a bow at both ends);
  * the chandelier and the pendant are BlockKitchenLight: placed lit, switched by a click or by
    redstone, the lit textures swapped by the blockstate. The pendant's iron ring is three blocks
    across, drawn by its one block from -16 to 32;
  * the wall pieces are BlockLivingDecor, standing against the wall behind them;
  * the horses, the sneeze guard and the dispensers are BlockCounterPiece, standing on whatever
    they are placed on (the guard and the dispensers in the translucent layer).

Usage:
    python gen_furniture_lodge.py              # write everything
    python gen_furniture_lodge.py --check      # fail if the tree has drifted
    python gen_furniture_lodge.py --fragments  # print the tab registration lines

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
import gen_furniture_appliances as A  # noqa: E402
import forge_multipart as FM  # noqa: E402

ASSETS = R.ASSETS
SUB = "furniture/lodge"
TEX = "csm:blocks/%s/" % SUB
MODEL = "csm:%s/" % SUB
BASE = MODEL + "base/"
ROT = R.ROT
el = R.el
ALL = R.ALL
SIDES = ("north", "south", "east", "west")


def T(name):
    return TEX + name


shade, clamp = R.shade, R.clamp


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def blank(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def strips(base, seed, size=16):
    """A cedar strip hull: narrow strips along the canoe, each its own shade, with a fine seam
    between them."""
    img = R.wood(base, seed, size=size, streak=0.03, line=0.92)
    px = img.load()
    rng = random.Random(seed)
    tone = [1.0 + rng.uniform(-0.08, 0.08) for _ in range(size)]
    for y in range(size):
        for x in range(size):
            k = tone[y] * (0.86 if y % 3 == 0 else 1.0)
            px[x, y] = shade(px[x, y][:3], k) + (255,)
    return img


def ribs(base, seed, size=16):
    """The inside of a hull: the planking, and a darker rib across it every four texels."""
    img = strips(base, seed, size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if x % 4 == 0:
                px[x, y] = shade(px[x, y][:3], 0.72) + (255,)
            elif x % 4 == 1:
                px[x, y] = shade(px[x, y][:3], 0.9) + (255,)
    return img


def rope(seed, size=16):
    """Manila rope: a twist of three strands, each a diagonal band light on one side."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            t = (x + y) % 4
            k = (1.1, 1.0, 0.86, 0.94)[t] * (1.0 + rng.uniform(-0.03, 0.03))
            px[x, y] = shade((196, 168, 118), k) + (255,)
    return img


def antler(seed, size=16):
    """Antler: bone cream towards the tips, browner and pitted towards the burr."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            t = x / (size - 1)
            c = tuple(a + (b - a) * t for a, b in zip((132, 100, 70), (222, 208, 180)))
            k = 1.0 + rng.uniform(-0.05, 0.05)
            if rng.random() < 0.08 * (1 - t):
                k *= 0.78
            px[x, y] = shade(c, k) + (255,)
    return img


def wrought(seed, size=16):
    """Wrought iron: near black, a hammered mottle and a little rust in the low spots."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            c = (40, 38, 38)
            k = 1.0 + rng.uniform(-0.12, 0.12)
            if rng.random() < 0.05:
                c = (86, 54, 36)
            px[x, y] = shade(c, k) + (255,)
    return img


def alabaster(lit, seed, size=16):
    """Alabaster: a warm translucent stone, veined. Lit, it glows from within, brightest in the
    middle of the bowl."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    base = (250, 226, 180) if lit else (206, 196, 180)
    for y in range(size):
        for x in range(size):
            k = 1.0 + rng.uniform(-0.03, 0.03)
            if lit:
                k *= 1.0 - 0.18 * abs(x - 7.5) / 7.5
            vein = abs(math.sin(x * 0.7 + y * 0.35 + math.sin(y * 0.9) * 1.5))
            if vein < 0.14:
                k *= 0.88
            px[x, y] = shade(base, k) + (255,)
    return img


def candle(lit, seed, size=16):
    """A candle sleeve's bulb: a warm flame shape on a cream sleeve, or a dark bulb when off."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if lit:
                d = math.hypot((x - 7.5) / 4.5, (y - 6) / 7.5)
                c = (255, 236, 168) if d < 1 else (246, 220, 170)
            else:
                c = (176, 168, 152)
            px[x, y] = shade(c, 1.0 + rng.uniform(-0.02, 0.02)) + (255,)
    return img


def snowshoe(size=16):
    """A traditional snowshoe on a transparent ground: a bent ash frame, teardrop shaped, the
    tail at the bottom, two crossbars, and a rawhide lacing lattice inside the frame."""
    img = blank(size)
    px = img.load()
    frame, hide, bar = (164, 118, 70), (214, 196, 156), (128, 88, 52)
    for y in range(size):
        for x in range(size):
            # The teardrop: an ellipse above, narrowing to the tail at the bottom
            v = y / (size - 1)
            half = 7.5 * math.sqrt(max(0.0, 1 - ((v - 0.38) / 0.4) ** 2)) if v < 0.78 else \
                7.5 * 0.55 * (1 - (v - 0.78) / 0.22)
            if v >= 0.78 and half < 0.6:
                half = 0.6
            dx = abs(x - 7.5)
            if dx > half + 0.5:
                continue
            if dx > half - 1.0 or y == 0:
                px[x, y] = frame + (255,)
            elif y in (4, 11):
                px[x, y] = bar + (255,)
            elif (x + y) % 3 == 0 or (x - y) % 3 == 0:
                px[x, y] = hide + (255,)
    return img


def ski(seed, size=16):
    """A wooden ski's face: lacquered hickory along it, a red stripe down the middle."""
    img = R.wood((176, 120, 66), seed, vertical=True, size=size, streak=0.05)
    px = img.load()
    for y in range(size):
        for x in (7, 8):
            px[x, y] = shade((168, 40, 36), 1.0 + (0.04 if y % 2 else -0.04)) + (255,)
    return img


def trout_print(seed, size=32):
    """A sporting print of a brook trout: an olive back with pale worm marks, a pink lateral
    band with red spots, a cream belly, on an ivory mat."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = shade((232, 224, 204), 1.0 + rng.uniform(-0.02, 0.02)) + (255,)
    cx, cy, a, b = 15.5, 16, 12.5, 4.6
    for y in range(size):
        for x in range(size):
            u = (x - cx) / a
            v = (y - cy) / b
            body = u * u + v * v <= 1.0
            tail = x > cx + a - 2 and abs(y - cy) <= (x - (cx + a - 2)) * 1.2 + 0.5 and x < 31
            if not (body or tail):
                continue
            if v < -0.25:
                c = (84, 92, 60)
                if (x * 3 + y * 5) % 7 == 0:
                    c = (170, 168, 120)
            elif v < 0.25:
                c = (196, 120, 110)
                if (x + y * 3) % 6 == 0:
                    c = (176, 40, 40)
            else:
                c = (228, 196, 150)
            px[x, y] = shade(c, 1.0 + rng.uniform(-0.04, 0.04)) + (255,)
    # The eye and the fins
    px[5, 15] = (20, 20, 20, 255)
    for x in range(12, 17):
        px[x, 21] = (200, 110, 70, 255)
    for x in range(14, 19):
        px[x, 11] = (90, 96, 64, 255)
    return img


def bronze(seed, size=16):
    """Cast bronze: a warm brown with lighter worn highlights and darker patina."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = 1.0 + rng.uniform(-0.08, 0.08)
            c = (122, 84, 46)
            r = rng.random()
            if r < 0.06:
                c = (176, 130, 76)
            elif r < 0.1:
                c = (70, 62, 44)
            px[x, y] = shade(c, k) + (255,)
    return img


def guard_glass(size=16):
    """Clear glass for a sneeze guard: almost all see-through, a polished edge all round and a
    faint diagonal sheen."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if x in (0, size - 1) or y in (0, size - 1):
                px[x, y] = (214, 232, 230, 170)
            elif (x + y) % 13 in (0, 1):
                px[x, y] = (236, 246, 246, 90)
            else:
                px[x, y] = (210, 232, 232, 72)
    return img


def drink(colour, alpha, seed, size=16):
    """A drink seen through glass: its colour, a little lighter at the top, the odd bubble."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = 1.08 - 0.12 * y / (size - 1) + rng.uniform(-0.02, 0.02)
            c = shade(colour, k)
            a = alpha
            if rng.random() < 0.03:
                c, a = shade(colour, 1.3), min(255, alpha + 30)
            px[x, y] = c + (a,)
    return img


TEXTURES = {
    "canoe_cedar": lambda: strips((178, 108, 62), 701),
    "canoe_cedar_inside": lambda: ribs((190, 124, 74), 702),
    "canoe_green": lambda: R.flat((52, 92, 64), 703, grain=5),
    "canoe_green_inside": lambda: ribs((196, 156, 104), 704),
    "canoe_rail": lambda: R.wood((104, 64, 38), 705, size=16),
    "rope": lambda: rope(706),
    "antler": lambda: antler(707),
    "wrought_iron": lambda: wrought(708),
    "alabaster_on": lambda: alabaster(True, 709),
    "alabaster_off": lambda: alabaster(False, 709),
    "candle_on": lambda: candle(True, 710),
    "candle_off": lambda: candle(False, 710),
    "snowshoe": snowshoe,
    "ski": lambda: ski(711),
    "print_frame": lambda: R.wood((70, 46, 30), 712, size=16),
    "trout_print": lambda: trout_print(713),
    "bronze": lambda: bronze(714),
    "plinth_marble": lambda: R.flat((34, 32, 34), 715, grain=6),
    "guard_glass": guard_glass,
    "drink_orange_glass": lambda: drink((236, 140, 30), 225, 716),
    "drink_water_glass": lambda: drink((196, 226, 236), 120, 717),
}

DEFAULT_TEX = {
    "hull": T("canoe_cedar"), "inside": T("canoe_cedar_inside"), "rail": T("canoe_rail"),
    "rope": T("rope"), "antler": T("antler"), "iron": T("wrought_iron"),
    "lens": T("candle_on"), "snowshoe": T("snowshoe"), "ski": T("ski"),
    "frame": T("print_frame"), "print": T("trout_print"), "bronze": T("bronze"),
    "plinth": T("plinth_marble"), "glass": T("guard_glass"), "drink": T("drink_orange_glass"),
    "chrome": R.T("chrome"),
}


def geometry(specs, particle, dy=0.0, display=None, centre=False):
    """A base model of the specs, dropped by dy, or centred on the block for an item."""
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
    # The texture itself, never "#key": a "#" reference to a sibling in the same model is an
    # upward reference to vanilla, which logs it and breaks the block into missingno particles
    tex["particle"] = DEFAULT_TEX[particle]
    out = {"parent": "block/block", "textures": tex, "elements": built}
    if display:
        out["display"] = display
    return out


# ------------------------------------------------------------------------------------------
# The canoe: a 0.85 m beam, its length along x (the run), hung in the block under a ceiling
# ------------------------------------------------------------------------------------------
def hull(x0, x1, pinch=0.0, lift=0.0, ends=True):
    """A length of hull from x0 to x1: keel, bilges, sides and gunwales, pinched in by `pinch`
    on each side and its bottom lifted by `lift` (towards a bow). `ends` closes its two ends,
    where a pinched length steps in from the next; a run's middle leaves them open."""
    p, l = pinch, lift
    e = ("east", "west") if ends else ()
    out = [el([x0, 5 + l, 4 + p], [x1, 6 + l, 12 - p], "hull", ("down", "up") + e,
              {"up": "inside"}),
           el([x0, 5.5 + l, 2.5 + p], [x1, 7.5 + l, 4 + p], "hull",
              ("down", "north", "up", "south") + e, {"up": "inside", "south": "inside"}),
           el([x0, 5.5 + l, 12 - p], [x1, 7.5 + l, 13.5 - p], "hull",
              ("down", "south", "up", "north") + e, {"up": "inside", "north": "inside"}),
           el([x0, 7.5 + l, 1.75 + p], [x1, 10.5, 2.5 + p], "hull", ("north", "south", "down") + e,
              {"south": "inside"}),
           el([x0, 7.5 + l, 13.5 - p], [x1, 10.5, 14.25 - p], "hull", ("north", "south", "down") + e,
              {"north": "inside"}),
           el([x0, 10.5, 1.5 + p], [x1, 11, 2.75 + p], "rail", ALL),
           el([x0, 10.5, 13.25 - p], [x1, 11, 14.5 - p], "rail", ALL)]
    return out


def sling(x):
    """A rope sling round the hull at x: under the keel and up both sides to the ceiling."""
    return [el([x, 4.5, 2], [x + 0.6, 5, 14], "rope", ("down", "north", "south", "east", "west")),
            el([x, 5, 1.25], [x + 0.6, 16, 1.75], "rope", SIDES),
            el([x, 5, 14.25], [x + 0.6, 16, 14.75], "rope", SIDES)]


# A bow, its tip at x 0: the hull narrowing and rising in steps to a stem that curls up.
BOW = (hull(8, 16, ends=False) + hull(4, 8, pinch=2.25, lift=0.75) + hull(1.5, 4, pinch=4.5, lift=2)
       + [el([0, 8, 7], [1.5, 13, 9], "rail", ALL),
          el([1.5, 7.5, 7.25], [3, 8.5, 8.75], "hull", ALL)])
CANOE_BODY = hull(0, 16, ends=False) + [el([7, 9.75, 2.5], [9, 10.25, 13.5], "rail", ALL)]
CANOE_END_LEFT = BOW + sling(11.5)
CANOE_END_RIGHT = R.mirror_x(CANOE_END_LEFT)
# A run of one: a short canoe, a bow at each end and a sling in the middle.
CANOE_SOLO = ([s for s in hull(6, 10)]
              + hull(3.5, 6, pinch=2.25, lift=0.75) + hull(1.5, 3.5, pinch=4.5, lift=2)
              + [el([0.25, 8, 7], [1.5, 12.5, 9], "rail", ALL)])
CANOE_SOLO = (CANOE_SOLO + R.mirror_x([s for s in CANOE_SOLO if s["from"][0] < 6])
              + sling(7.7))
CANOE_PARTS = {"body": CANOE_BODY, "end_left": CANOE_END_LEFT,
               "end_right": CANOE_END_RIGHT, "solo": CANOE_SOLO}
CANOE_RULES = R.faced([("body", {"left": "true", "right": "true"}),
                       ("end_left", {"left": "false", "right": "true"}),
                       ("end_right", {"left": "true", "right": "false"}),
                       ("solo", {"left": "false", "right": "false"})])


# ------------------------------------------------------------------------------------------
# The antler chandelier: eight antlers round an iron hub on a chain, a candle on each tip
# ------------------------------------------------------------------------------------------
def antler_arm(x_flip=False, along_z=False, sign=1):
    """One antler drawn along +x from the hub, tilted 22.5 degrees up, with two tines and a
    candle sleeve on its tip; mirrored or turned onto another axis."""
    parts = [el([8.5, 6.25, 7.5], [17, 7.25, 8.5], "antler", ALL),
             el([11.5, 7.25, 7.6], [12.3, 9.5, 8.4], "antler", ALL),
             el([14, 7.25, 7.6], [14.8, 8.75, 8.4], "antler", ALL),
             el([16, 7.25, 7.25], [17.25, 9.25, 8.75], "lens", ALL)]
    out = []
    for s in parts:
        x0, y0, z0 = s["from"]
        x1, y1, z1 = s["to"]
        if x_flip:
            x0, x1 = 16 - x1, 16 - x0
        if along_z:
            frm, to = [z0, y0, x0], [z1, y1, x1]
            axis, angle = "x", (-22.5 if not x_flip else 22.5)
        else:
            frm, to = [x0, y0, z0], [x1, y1, z1]
            axis, angle = "z", (22.5 if not x_flip else -22.5)
        out.append(el(frm, to, s["tex"], ALL, rot=(axis, angle, [8, 6.75, 8])))
    return out


def diagonal_arm(turned):
    """A shorter antler on a diagonal, level: turned 45 degrees about the chain."""
    parts = [el([8.5, 6.5, 7.5], [15, 7.5, 8.5], "antler", ALL),
             el([12.5, 7.5, 7.6], [13.3, 9.25, 8.4], "antler", ALL),
             el([14, 7.5, 7.25], [15.25, 9.5, 8.75], "lens", ALL)]
    out = []
    for s in parts:
        x0, y0, z0 = s["from"]
        x1, y1, z1 = s["to"]
        if turned:
            x0, x1 = 16 - x1, 16 - x0
        out.append(el([x0, y0, z0], [x1, y1, z1], s["tex"], ALL, rot=("y", 45, [8, 7, 8])))
    return out


CHANDELIER = (R.octagon(8, 8, 2, 5.5, 8, "iron")
              + [el([7.6, 8, 7.6], [8.4, 16, 8.4], "iron", SIDES)]
              + antler_arm() + antler_arm(x_flip=True)
              + antler_arm(along_z=True) + antler_arm(x_flip=True, along_z=True)
              + diagonal_arm(False) + diagonal_arm(True))


# ------------------------------------------------------------------------------------------
# The drum pendant: a wrought-iron ring three blocks across, on spokes to a centre rod, and an
# alabaster bowl hung under its middle
# ------------------------------------------------------------------------------------------
def drum_ring(r, y0, y1, t=0.75):
    """An octagonal band of inradius r: eight plates, four of them turned 45 degrees."""
    w = r * math.tan(math.pi / 8) + 0.05
    out = []
    for turned in (False, True):
        rot = ("y", 45, [8, y0, 8]) if turned else None
        out += [el([8 - w, y0, 8 - r - t], [8 + w, y1, 8 - r], "iron", ALL, rot=rot),
                el([8 - w, y0, 8 + r], [8 + w, y1, 8 + r + t], "iron", ALL, rot=rot),
                el([8 - r - t, y0, 8 - w], [8 - r, y1, 8 + w], "iron", ALL, rot=rot),
                el([8 + r, y0, 8 - w], [8 + r + t, y1, 8 + w], "iron", ALL, rot=rot)]
    return out


DRUM = (drum_ring(22, 4, 9)
        + drum_ring(22.2, 8.6, 9.2, t=0.4)
        + [el([-14, 8.75, 7.75], [30, 9.25, 8.25], "iron", ALL),
           el([7.75, 8.75, -14], [8.25, 9.25, 30], "iron", ALL),
           el([-11, 8.75, 7.75], [27, 9.25, 8.25], "iron", ALL, rot=("y", 45, [8, 9, 8])),
           el([7.75, 8.75, -11], [8.25, 9.25, 27], "iron", ALL, rot=("y", 45, [8, 9, 8])),
           el([7.5, 9.25, 7.5], [8.5, 16, 8.5], "iron", SIDES),
           el([7.75, 3, 7.75], [8.25, 8.75, 8.25], "iron", SIDES)]
        + R.octagon(8, 8, 4.5, 1, 3, "lens")
        + R.octagon(8, 8, 1.25, 3, 3.5, "iron", caps=("up",)))


# ------------------------------------------------------------------------------------------
# Wall pieces, against the wall at +Z
# ------------------------------------------------------------------------------------------
def crossed(panel_from, panel_to, tex, faces, depth=0.25):
    """Two of a flat thing crossed at 22.5 degrees each way, one just in front of the other."""
    out = []
    for i, angle in enumerate((22.5, -22.5)):
        z0 = panel_from[2] - i * (depth + 0.05)
        frm = [panel_from[0], panel_from[1], z0]
        to = [panel_to[0], panel_to[1], z0 + depth]
        # the whole texture on each face: the panel is the snowshoe, stretched to it
        out.append(el(frm, to, tex, faces, uv={f: [0, 0, 16, 16] for f in faces},
                      rot=("z", angle, [8, 8, z0 + depth / 2])))
    return out


SNOWSHOES = crossed([5, 0.5, 15.5], [11, 15.5, 15.75], "snowshoe", ("north", "south"))
SKIS = []
for _i, _angle in enumerate((22.5, -22.5)):
    _z = 15.4 - _i * 0.6
    _rot = ("z", _angle, [8, 8, _z])
    # the ski's face is the whole texture's width, so its stripe runs down the middle
    _uv = {"north": [4, 0, 12, 16], "south": [4, 0, 12, 16]}
    SKIS += [el([7.25, -0.5, _z], [8.75, 15, _z + 0.4], "ski", ALL, uv=_uv, rot=_rot),
             el([7.25, 15, _z - 0.6], [8.75, 16.25, _z], "ski", ALL,
                uv={"north": [4, 0, 12, 2], "south": [4, 0, 12, 2]}, rot=_rot),
             el([7.4, 6, _z - 0.5], [8.6, 7.25, _z], "rail", ALL, rot=_rot)]
TROUT = ([el([2, 3.5, 15.5], [14, 12.5, 15.75], "print", ("north",)),
          el([1.25, 2.75, 15.25], [14.75, 3.5, 16], "frame", ALL),
          el([1.25, 12.5, 15.25], [14.75, 13.25, 16], "frame", ALL),
          el([1.25, 3.5, 15.25], [2, 12.5, 16], "frame", ALL),
          el([14, 3.5, 15.25], [14.75, 12.5, 16], "frame", ALL)])
TROUT[0]["uv"] = {"north": [0, 2.5, 16, 13.5]}


# ------------------------------------------------------------------------------------------
# Table and counter pieces, standing on the floor of their block (dropped onto a surface)
# ------------------------------------------------------------------------------------------
def horse_parts(rear=False):
    """A horse in profile along x, its head towards -x, on a plinth. Rearing, its body, neck,
    head and forelegs turn 45 degrees up about its hindquarters and the forelegs fold."""
    plinth = [el([3, 0, 5.5], [13, 1, 10.5], "plinth", ALL)]
    hind = [el([9.2, 1, 6.9], [10.1, 4.6, 7.8], "bronze", ALL),
            el([9.2, 1, 8.2], [10.1, 4.6, 9.1], "bronze", ALL)]
    tail = [el([10.6, 3.5, 7.7], [11.3, 6.6, 8.3], "bronze", ALL,
               rot=("z", -22.5, [10.6, 6.6, 8]))]
    body = [el([4.5, 4.5, 6.75], [10.6, 7.5, 9.25], "bronze", ALL),
            el([3.6, 6.4, 7.25], [5.3, 10.2, 8.75], "bronze", ALL),
            el([2.0, 9.0, 7.35], [4.6, 10.5, 8.65], "bronze", ALL),
            el([4.1, 10.2, 7.5], [4.6, 11.0, 7.9], "bronze", ALL),
            el([4.1, 10.2, 8.1], [4.6, 11.0, 8.5], "bronze", ALL)]
    if rear:
        fore = [el([4.8, 2.6, 6.9], [5.7, 4.6, 7.8], "bronze", ALL),
                el([5.4, 3.0, 8.2], [6.3, 4.6, 9.1], "bronze", ALL)]
        rot = ("z", -45, [10, 4.5, 8])
        body = [el(s["from"], s["to"], s["tex"], ALL, rot=rot) for s in body + fore]
        return plinth + hind + tail + body
    fore = [el([4.8, 1, 6.9], [5.7, 4.6, 7.8], "bronze", ALL),
            el([4.8, 1, 8.2], [5.7, 4.6, 9.1], "bronze", ALL)]
    neck = [dict(body[1], rot=("z", 22.5, [4.45, 6.4, 8]))]
    return plinth + hind + fore + tail + [body[0]] + neck + body[2:]


HORSE = horse_parts()
HORSE_REARING = horse_parts(rear=True)

# Sneeze guard: two chrome posts, a glass shelf on top, and a front pane leaning towards the
# customer at the top, so a buffet run of them reads as one guard.
SNEEZE_GUARD = ([el([x, 0, 6.25], [x + 0.75, 9.5, 7], "chrome", ALL) for x in (0.5, 14.75)]
                + [el([0, 9.5, 3], [16, 9.75, 10], "glass", ALL),
                   el([0, 4.25, 3], [16, 9.5, 3.25], "glass", ALL,
                      rot=("x", -22.5, [8, 9.5, 3.12])),
                   el([0.5, 9.25, 2.75], [15.5, 9.5, 3.25], "chrome", ALL)])

# Beverage dispenser: a glass jar on a chrome stand with a tap at the front.
DISPENSER = (R.octagon(8, 8, 3.4, 0, 2.5, "chrome")
             + R.octagon(8, 8, 3.2, 2.6, 8.5, "drink")
             + R.octagon(8, 8, 3.4, 2.5, 11, "glass")
             + R.octagon(8, 8, 3.5, 11, 12, "chrome")
             + [el([7.4, 3.0, 3.4], [8.6, 3.8, 4.8], "chrome", ALL),
                el([7.6, 2.4, 3.6], [8.4, 3.0, 4.2], "chrome", ALL),
                el([7.6, 12, 7.6], [8.4, 12.75, 8.4], "chrome", ALL)])


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
def fin(fid, tex, *names):
    return (fid, tex) + names


CANOE_FINISHES = [
    fin("cedar", {"hull": T("canoe_cedar"), "inside": T("canoe_cedar_inside")},
        "Cedar Strip", "Zedernleisten", "listones de cedro", "cederlist"),
    fin("green", {"hull": T("canoe_green"), "inside": T("canoe_green_inside")},
        "Green Canvas", "Grünes Segeltuch", "lona verde", "grön kanvas"),
]
BRONZE = [fin("bronze", {}, "Bronze", "Bronze", "bronce", "brons")]
IRON = [fin("iron", {}, "Wrought Iron", "Schmiedeeisen", "hierro forjado", "smidesjärn")]
ANTLER = [fin("natural", {}, "Natural", "Natur", "natural", "natur")]
NATURAL = [fin("wood", {}, "Wood", "Holz", "madera", "trä")]
GLASS = [fin("glass", {}, "Glass", "Glas", "vidrio", "glas")]
DRINKS = [fin("orange", {"drink": T("drink_orange_glass")}, "Orange Juice", "Orangensaft",
              "zumo de naranja", "apelsinjuice"),
          fin("water", {"drink": T("drink_water_glass")}, "Water", "Wasser", "agua", "vatten")]

# piece -> (kind, finishes, names, spec)
#   kind "canoe": spec java
#   kind "light": geo, particle, box, light, lit textures (on, off)
#   kind "wall":  geo, particle, box, layer, material/sound
#   kind "counter": geo, particle, material/sound, layer
PIECES = [
    ("hanging_canoe", "canoe", CANOE_FINISHES,
     ("Hanging Canoe", "Hängendes Kanu", "Canoa colgante", "Hängande kanot"),
     {"java": 'new BlockResidentialRun("%s", new int[]{0, 4, 1, 16, 16, 15}, false)'}),
    ("antler_chandelier", "light", ANTLER,
     ("Antler Chandelier", "Geweihleuchter", "Lámpara de astas", "Hornkrona"),
     {"geo": CHANDELIER, "particle": "antler", "box": [1, 5, 1, 15, 16, 15], "light": 13,
      "lit": (T("candle_on"), T("candle_off"))}),
    ("drum_pendant", "light", IRON,
     ("Drum Pendant (Alabaster Bowl)", "Trommelpendelleuchte (Alabasterschale)",
      "Lámpara de tambor (cuenco de alabastro)", "Trumpendel (alabasterskål)"),
     {"geo": DRUM, "particle": "iron", "box": [3, 0, 3, 13, 16, 13], "light": 15,
      "lit": (T("alabaster_on"), T("alabaster_off"))}),
    ("wall_snowshoes", "wall", NATURAL,
     ("Crossed Snowshoes", "Gekreuzte Schneeschuhe", "Raquetas de nieve cruzadas",
      "Korsade snöskor"),
     {"geo": SNOWSHOES, "particle": "snowshoe", "box": [2, 0, 14, 14, 16, 16],
      "layer": "CUTOUT", "material": "Material.WOOD, SoundType.WOOD"}),
    ("wall_skis", "wall", NATURAL,
     ("Crossed Wooden Skis", "Gekreuzte Holzski", "Esquís de madera cruzados",
      "Korsade träskidor"),
     {"geo": SKIS, "particle": "ski", "box": [2, 0, 13, 14, 16, 16],
      "layer": "SOLID", "material": "Material.WOOD, SoundType.WOOD"}),
    ("trout_print", "wall", NATURAL,
     ("Framed Trout Print", "Gerahmter Forellendruck", "Lámina enmarcada de trucha",
      "Inramat öringtryck"),
     {"geo": TROUT, "particle": "frame", "box": [1, 2, 15, 15, 14, 16],
      "layer": "SOLID", "material": "Material.WOOD, SoundType.WOOD"}),
    ("horse_statue", "counter", BRONZE,
     ("Horse Statue", "Pferdestatue", "Estatua de caballo", "Hästskulptur"),
     {"geo": HORSE, "particle": "bronze", "material": "Material.IRON, SoundType.METAL",
      "layer": "SOLID"}),
    ("rearing_horse_statue", "counter", BRONZE,
     ("Rearing Horse Statue", "Steigende Pferdestatue", "Estatua de caballo encabritado",
      "Stegrande hästskulptur"),
     {"geo": HORSE_REARING, "particle": "bronze", "material": "Material.IRON, SoundType.METAL",
      "layer": "SOLID"}),
    ("sneeze_guard", "counter", GLASS,
     ("Buffet Sneeze Guard", "Buffet-Spuckschutz", "Protector de buffet", "Buffé-nysskydd"),
     {"geo": SNEEZE_GUARD, "particle": "chrome", "material": "Material.GLASS, SoundType.GLASS",
      "layer": "TRANSLUCENT"}),
    ("beverage_dispenser", "counter", DRINKS,
     ("Beverage Dispenser", "Getränkespender", "Dispensador de bebidas", "Dryckesdispenser"),
     {"geo": DISPENSER, "particle": "chrome", "material": "Material.GLASS, SoundType.GLASS",
      "layer": "TRANSLUCENT"}),
]
PIECE = {p: (kind, fins, names, spec) for p, kind, fins, names, spec in PIECES}


def names_with(names, fnames):
    return ["%s (%s)" % (n, f) for n, f in zip(names, fnames)]


def box_of(specs):
    """The Java box of a piece standing on the floor, from its elements, whole pixels."""
    built = [R.build(s) for s in specs]
    lo = [max(0, int(math.floor(min(e["from"][i] for e in built)))) for i in range(3)]
    hi = [min(16, int(math.ceil(max(e["to"][i] for e in built)))) for i in range(3)]
    return lo + hi


def jbox(box):
    return ", ".join(R._num(v) for v in box)


def java_line(piece, reg):
    kind, _f, _n, spec = PIECE[piece]
    if kind == "canoe":
        return spec["java"] % reg
    if kind == "light":
        return 'new BlockKitchenLight("%s", new int[]{%s}, %d)' % (reg, jbox(spec["box"]),
                                                                   spec["light"])
    if kind == "wall":
        return ('new BlockLivingDecor("%s", new int[]{%s}, %s, 0.8F, BlockRenderLayer.%s)'
                % (reg, jbox(spec["box"]), spec["material"], spec["layer"]))
    return ('new BlockCounterPiece("%s", new int[]{%s}, %s, BlockRenderLayer.%s)'
            % (reg, jbox(box_of(spec["geo"])), spec["material"], spec["layer"]))


def entries():
    """Every block: (registry, piece, finish textures, names, java)."""
    out = []
    for piece, _kind, fins, names, _spec in PIECES:
        for fid, ftex, *fnames in fins:
            reg = "%s_%s" % (piece, fid)
            out.append((reg, piece, ftex, names_with(names, fnames), java_line(piece, reg)))
    return out


# ------------------------------------------------------------------------------------------
# Output
# ------------------------------------------------------------------------------------------
def base_models():
    out = []
    for piece, kind, _fins, _names, spec in PIECES:
        if kind == "canoe":
            for part, geo in CANOE_PARTS.items():
                out.append(("hanging_canoe_%s" % part, geometry(geo, "hull")))
            out.append(("hanging_canoe_item", geometry(CANOE_SOLO, "hull")))
        elif kind == "counter":
            for rest, drop in A.RESTS:
                out.append(("%s_%s" % (piece, rest), geometry(spec["geo"], spec["particle"],
                                                              drop)))
            out.append(("%s_item" % piece, geometry(spec["geo"], spec["particle"],
                                                    display=A.small_display(spec["geo"]),
                                                    centre=True)))
        else:
            display = None
            if piece == "drum_pendant":
                display = {"gui": {"rotation": [30, 225, 0], "scale": [0.22, 0.22, 0.22]},
                           "ground": {"translation": [0, 3, 0], "scale": [0.1, 0.1, 0.1]},
                           "fixed": {"scale": [0.2, 0.2, 0.2]},
                           "thirdperson_righthand": {"rotation": [75, 45, 0],
                                                     "translation": [0, 2.5, 0],
                                                     "scale": [0.15, 0.15, 0.15]},
                           "firstperson_righthand": {"rotation": [0, 45, 0],
                                                     "scale": [0.16, 0.16, 0.16]},
                           "firstperson_lefthand": {"rotation": [0, 225, 0],
                                                    "scale": [0.16, 0.16, 0.16]}}
            out.append((piece, geometry(spec["geo"], spec["particle"], display=display)))
    return out


def facing_variants():
    return {f: ({"y": r} if r else {}) for f, r in ROT.items()}


def light_state(piece, ftex, on, off):
    return {"forge_marker": 1,
            "defaults": {"model": BASE + piece, "textures": dict(ftex, lens=on)},
            "variants": {"facing": facing_variants(),
                         "lit": {"true": {"textures": {"lens": on}},
                                 "false": {"textures": {"lens": off}}},
                         # Whether redstone last powered it: stored, drawn the same either way.
                         "powered": {"true": {}, "false": {}},
                         "inventory": [{}]}}


def counter_state(piece, ftex):
    return {"forge_marker": 1,
            "defaults": {"model": BASE + "%s_floor" % piece, "textures": dict(ftex)},
            "variants": {"facing": facing_variants(),
                         "rest": {rest: {"model": BASE + "%s_%s" % (piece, rest)}
                                  for rest, _d in A.RESTS},
                         "inventory": [{"model": BASE + "%s_item" % piece}]}}


def single_state(piece, ftex):
    return {"forge_marker": 1,
            "defaults": {"model": BASE + piece, "textures": dict(ftex)},
            "variants": {"facing": facing_variants(), "inventory": [{}]}}


def multipart_state(reg, rules):
    parts = []
    for part, when, r in rules:
        apply = {"model": MODEL + "%s_%s" % (reg, part)}
        if r:
            apply["y"] = r
        parts.append({"when": when, "apply": apply} if when else {"apply": apply})
    return {"multipart": parts}


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

    for name, draw in TEXTURES.items():
        rel = "textures/blocks/%s/%s.png" % (SUB, name)
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
    for name, data in base_models():
        dump("models/block/%s/base/%s.json" % (SUB, name), data)
    for reg, piece, ftex, _names, _java in entries():
        kind, _f, _n, spec = PIECE[piece]
        if kind == "canoe":
            for part in CANOE_PARTS:
                dump("models/block/%s/%s_%s.json" % (SUB, reg, part),
                     {"parent": "csm:block/%s/base/hanging_canoe_%s" % (SUB, part),
                      "textures": dict(ftex)})
            dump("models/item/%s.json" % reg,
                 {"parent": "csm:block/%s/base/hanging_canoe_item" % SUB,
                  "textures": dict(ftex)})
            state = multipart_state(reg, CANOE_RULES)
        elif kind == "light":
            state = light_state(piece, ftex, *spec["lit"])
        elif kind == "counter":
            state = counter_state(piece, ftex)
        else:
            state = single_state(piece, ftex)
        # a property that only swaps textures: one multipart location (forge_multipart)
        state = FM.convert_if_texture_only(reg, state, MODEL[:-1], dump)
        dump("blockstates/%s.json" % reg, state)
    R.write_lang(os.path.join(assets, "lang"), lang_entries())
    written += ["lang/%s.lang" % loc for loc in R.LOCALES]
    R.separate_faces(assets, written)
    return written


def fragments():
    lines = ["    // ---- Lodge ----"]
    last = None
    for reg, piece, _t, names, java in entries():
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
            r = e.get("rotation")
            if r and r["angle"] not in (-45, -22.5, 0, 22.5, 45):
                bad.append("%s: rotation the game cannot draw: %s" % (name, e))
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
    tmp = tempfile.mkdtemp(prefix="lodge_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not R.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("lodge furniture is up to date (%d files)" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
