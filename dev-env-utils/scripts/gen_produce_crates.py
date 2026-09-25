#!/usr/bin/env python3
"""Every asset the market-stall produce crates ship (Furniture & Novelties, Furniture tab).

    python dev-env-utils/scripts/gen_produce_crates.py
    python dev-env-utils/scripts/gen_produce_crates.py --check    # write nothing; exit 1 on drift
    python dev-env-utils/scripts/gen_produce_crates.py --boxes    # print each block's box

Fifteen blocks, one catalogue entry each. Twelve are the same crate -- a slatted pine produce
crate with an open top and solid ends with a hand hole, 0.875 x 0.44 x 0.69 m, a little larger
than a real one so it reads in a block -- filled with a heap of one kind of produce: a flat bed
of the produce (a mash texture) and a mound of single pieces on top. A piece of round fruit is a
cube wearing a whole shaded texture on every face with its corners cut away, which is as round
as a cube gets. The other three are a small wooden barrel of carrots, the same crate of golden
apples, and a closed two-block shipping crate.

Everything is drawn here: no geometry, UVs or pixels come from anywhere else. The heaps are laid
out by a seeded random, so a re-run writes the same bytes and --check means something.

Every model faces north (its long side along x) and is turned by the blockstate's six-way
`facing`, as the blocks' NSEWUD class expects.
"""

import argparse
import json
import math
import os
import random
import shutil
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import model_depth  # noqa: E402

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
ASSETS = os.path.join(REPO, "modules", "furnishings", "src", "main", "resources", "assets", "csm")
SUB = "furniture/produce"
TEX_REF = "csm:blocks/" + SUB + "/%s"
MODEL_REF = "csm:" + SUB + "/%s"


# ------------------------------------------------------------------------------------------
# Texture helpers
# ------------------------------------------------------------------------------------------

def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def shade(c, k):
    return clamp(tuple(v * k for v in c[:3]))


def mix(a, b, t):
    return clamp(tuple(a[i] + (b[i] - a[i]) * t for i in range(3)))


def canvas(base, seed, grain=5, size=16):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            d = rng.uniform(-grain, grain)
            px[x, y] = clamp(tuple(v + d for v in base)) + (255,)
    return img, px, rng


def round_tex(base, seed, hi=1.28, lo=0.66, speck=None, speck_n=0, streak=None, cut=True):
    """One piece of round fruit seen from any side: lit upper left, dark at the rim, the corners
    cut away so the cube it is painted on reads as round."""
    img, px, rng = canvas(base, seed, 4)
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r = math.hypot(dx, dy) / 8.0
            lit = math.hypot(x + 0.5 - 5.5, y + 0.5 - 5.0) / 11.0
            k = hi - (hi - lo) * min(1.0, 0.55 * lit + 0.45 * r * r)
            c = px[x, y]
            if streak and (x * 3 + y * 5 + rng.randint(0, 3)) % 7 == 0:
                c = mix(c, streak, 0.45)
            px[x, y] = shade(c, k) + (255,)
            ax, ay = abs(dx), abs(dy)
            if cut and ax > 5 and ay > 5 and (ax - 5) ** 2 + (ay - 5) ** 2 > 2.9 ** 2:
                px[x, y] = (0, 0, 0, 0)
    for _ in range(speck_n):
        x, y = rng.randrange(2, 14), rng.randrange(2, 14)
        px[x, y] = clamp(speck) + (255,)
    # a small specular glint
    for x, y in ((5, 4), (4, 5)):
        px[x, y] = mix(px[x, y], (255, 255, 240), 0.45) + (255,)
    return img


def mash(colours, seed, dark, cell=4, size=16):
    """A bed of produce seen from above: rounded lumps in the given colours with dark crevices.
    The lattice wraps, so it tiles."""
    rng = random.Random(seed)
    pts = []
    for gy in range(0, size, cell):
        for gx in range(0, size, cell):
            pts.append((gx + rng.uniform(0, cell), gy + rng.uniform(0, cell),
                        rng.choice(colours), rng.uniform(0.85, 1.12)))
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            ds = []
            for (cx, cy, c, k) in pts:
                ddx = min(abs(x + 0.5 - cx), size - abs(x + 0.5 - cx))
                ddy = min(abs(y + 0.5 - cy), size - abs(y + 0.5 - cy))
                ds.append((math.hypot(ddx, ddy), c, k, cx, cy))
            ds.sort(key=lambda t: t[0])
            d0, c, k, cx, cy = ds[0]
            edge = ds[1][0] - d0
            if edge < 0.7:
                px[x, y] = clamp(tuple(v + rng.uniform(-5, 5) for v in dark)) + (255,)
            else:
                f = k * (1.18 - 0.14 * d0)
                if (x + 0.5 - cx) < -0.8 and (y + 0.5 - cy) < -0.8:
                    f *= 1.1
                px[x, y] = shade(clamp(tuple(v + rng.uniform(-6, 6) for v in c)), f) + (255,)
    return img


def planks(base, seed, rows=(), seam=None, grain=6):
    """Pine boards with grain along u; `rows` are v positions of dark seams between boards."""
    rng = random.Random(seed)
    tone = [rng.uniform(-grain, grain) for _ in range(16)]
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = clamp(tuple(v + tone[y] + rng.uniform(-4, 4) for v in base)) + (255,)
    for _ in range(3):
        x, y = rng.randrange(1, 15), rng.randrange(0, 16)
        px[x, y] = shade(base, 0.72) + (255,)
    for y in rows:
        for x in range(16):
            px[x, y] = clamp(tuple(v + rng.uniform(-4, 4) for v in (seam or shade(base, 0.55)))) \
                + (255,)
    return img


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
PINE = (206, 170, 116)
PINE_DARK = (120, 88, 54)

TEXTURES = {}


def texture(name):
    def reg(fn):
        TEXTURES[name] = fn
        return fn
    return reg


@texture("crate_slat")
def crate_slat():
    return planks(PINE, 20260901)


@texture("crate_end")
def crate_end():
    """The crate's end board, seen on the east and west faces (v = 9..16 is the 7 px tall end):
    two boards and a dark hand hole cut near the top."""
    img = planks(PINE, 20260902, rows=(12,))
    px = img.load()
    for y in (10, 11):
        for x in range(6, 10):
            px[x, y] = (46, 34, 24, 255)
    for x in range(6, 10):
        px[x, 9] = shade(PINE, 1.1) + (255,)
    return img


@texture("crate_floor")
def crate_floor():
    return planks(shade(PINE, 0.85), 20260903, rows=(3, 7, 11, 15))


@texture("shipping_side")
def shipping_side():
    """A shipping crate's sheathing: rough spruce boards laid horizontally."""
    return planks((170, 132, 88), 20260904, rows=(3, 7, 11, 15), seam=(92, 66, 42), grain=8)


@texture("shipping_frame")
def shipping_frame():
    img = planks((150, 112, 72), 20260905, grain=5)
    px = img.load()
    rng = random.Random(20260906)
    for _ in range(2):  # nail heads
        x, y = rng.randrange(3, 13), rng.randrange(3, 13)
        px[x, y] = (70, 70, 72, 255)
    return img


@texture("barrel_side")
def barrel_side():
    """Oak staves, vertical, with three iron hoops (the barrel spans v = 5..16)."""
    rng = random.Random(20260907)
    img, px, _ = canvas((132, 92, 56), 20260907, 5)
    for x in range(16):
        tone = rng.uniform(-10, 10)
        for y in range(16):
            c = px[x, y]
            px[x, y] = clamp(tuple(v + tone for v in c[:3])) + (255,)
            if x % 4 == 3:
                px[x, y] = shade(c, 0.62) + (255,)
    for rows in ((5, 6), (7, 8), (14, 15)):
        for y in rows:
            for x in range(16):
                px[x, y] = clamp(tuple(v + rng.uniform(-6, 6) for v in (66, 66, 70))) + (255,)
        for x in range(16):
            px[x, rows[0]] = (96, 96, 102, 255)
    return img


@texture("barrel_head")
def barrel_head():
    return planks((142, 100, 62), 20260908, rows=(4, 8, 12))


# --- produce (each a whole round piece, plus the bed it lies on) ---------------------------

@texture("apple_red")
def apple_red():
    return round_tex((178, 32, 34), 20260910, streak=(222, 176, 70), speck=(236, 214, 140),
                     speck_n=4)


@texture("apple_green")
def apple_green():
    return round_tex((134, 188, 62), 20260911, speck=(206, 226, 150), speck_n=5)


@texture("apple_gold")
def apple_gold():
    return round_tex((236, 192, 46), 20260912, hi=1.4, lo=0.7, streak=(255, 244, 150))


@texture("orange")
def orange():
    img = round_tex((236, 128, 26), 20260913)
    px = img.load()
    rng = random.Random(20260914)
    for _ in range(18):  # dimpled peel
        x, y = rng.randrange(1, 15), rng.randrange(1, 15)
        if px[x, y][3]:
            px[x, y] = shade(px[x, y], 0.86) + (255,)
    return img


@texture("pear")
def pear():
    return round_tex((170, 190, 70), 20260915, speck=(126, 110, 50), speck_n=9)


@texture("tomato")
def tomato():
    return round_tex((210, 38, 28), 20260916, hi=1.32)


@texture("tomato_top")
def tomato_top():
    """The top of a tomato: its green star calyx."""
    img = round_tex((210, 38, 28), 20260917, hi=1.32)
    px = img.load()
    green = (60, 126, 40)
    for i in range(5):
        a = i * 2 * math.pi / 5 + 0.3
        for t in range(0, 6):
            x = int(round(7.5 + math.cos(a) * t))
            y = int(round(7.5 + math.sin(a) * t))
            px[x, y] = shade(green, 1.0 - t * 0.05) + (255,)
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px[x, y] = (46, 96, 30, 255)
    return img


@texture("potato")
def potato():
    img = round_tex((168, 128, 80), 20260918, hi=1.18, lo=0.72, speck=(110, 80, 50), speck_n=8)
    return img


@texture("onion_brown")
def onion_brown():
    img = round_tex((184, 116, 50), 20260919, hi=1.25)
    px = img.load()
    for x in (5, 9, 12):  # papery skin lines, top to bottom
        for y in range(2, 14):
            if px[x, y][3]:
                px[x, y] = shade(px[x, y], 0.82) + (255,)
    return img


@texture("onion_white")
def onion_white():
    img = round_tex((228, 222, 200), 20260920, hi=1.08, lo=0.76)
    px = img.load()
    for x in (4, 8, 11):
        for y in range(2, 14):
            if px[x, y][3]:
                px[x, y] = shade(px[x, y], 0.9) + (255,)
    return img


@texture("onion_neck")
def onion_neck():
    return canvas((196, 160, 104), 20260921, 8)[0]


@texture("lettuce")
def lettuce():
    """A lettuce head: pale heart, darker frilled outer leaves, light veins."""
    img = round_tex((110, 170, 60), 20260922, hi=1.3, lo=0.72, cut=True)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if not px[x, y][3]:
                continue
            r = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if r < 3.2:
                px[x, y] = mix(px[x, y], (206, 228, 140), 0.55) + (255,)
            if (x + y) % 5 == 0 and r > 3:
                px[x, y] = mix(px[x, y], (190, 220, 150), 0.4) + (255,)
    return img


@texture("lettuce_leaf")
def lettuce_leaf():
    """The loose outer leaves round a head's base: dark green with a frilled, cut-out top edge."""
    img, px, rng = canvas((72, 136, 44), 20260923, 8)
    for x in range(16):
        top = 3 + int(2 * abs(math.sin(x * 1.3))) + rng.randint(0, 1)
        for y in range(16):
            if y < top:
                px[x, y] = (0, 0, 0, 0)
            elif (x * 7 + y) % 9 == 0:
                px[x, y] = mix(px[x, y], (170, 206, 120), 0.5) + (255,)
    return img


@texture("beet")
def beet():
    return round_tex((118, 22, 50), 20260924, hi=1.3, lo=0.62, streak=(160, 60, 90))


@texture("beet_greens")
def beet_greens():
    """Beet tops on a cutout plane: red stalks at the bottom fanning into green leaves."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(20260925)
    for sx in (4, 8, 12):
        lean = (sx - 8) / 8.0
        for y in range(8, 16):
            x = int(round(8 + (sx - 8) * (16 - y) / 8.0 * 0.5 + lean))
            px[x, y] = clamp(tuple(v + rng.uniform(-8, 8) for v in (160, 40, 70))) + (255,)
        cx = 8 + (sx - 8) * 0.9
        for y in range(0, 10):
            w = 2.6 * math.sin(math.pi * (y + 0.5) / 10.0)
            for x in range(16):
                if abs(x + 0.5 - cx) <= w:
                    c = (58, 124, 46) if abs(x + 0.5 - cx) > 0.6 else (150, 50, 70)
                    px[x, y] = clamp(tuple(v + rng.uniform(-8, 8) for v in c)) + (255,)
    return img


@texture("carrot")
def carrot():
    """A carrot's side: orange with the fine rings across it."""
    img, px, rng = canvas((232, 118, 28), 20260926, 6)
    for y in range(16):
        k = 1.18 - 0.4 * abs(y + 0.5 - 6) / 10.0
        for x in range(16):
            px[x, y] = shade(px[x, y], k) + (255,)
    for x in (2, 5, 9, 13):
        for y in range(16):
            if rng.random() < 0.7:
                px[x, y] = shade(px[x, y], 0.8) + (255,)
    return img


@texture("carrot_top")
def carrot_top():
    """Carrot greens: feathery, several greens, a cutout fringe."""
    img, px, rng = canvas((64, 140, 44), 20260927, 12)
    for y in range(16):
        for x in range(16):
            if (x + y * 3) % 4 == 0 and rng.random() < 0.5:
                px[x, y] = (0, 0, 0, 0)
            elif rng.random() < 0.2:
                px[x, y] = (110, 176, 70, 255)
    return img


@texture("corn_husk")
def corn_husk():
    """An ear of corn with its husk pulled back along the top: green husk streaked along u, a band
    of yellow kernels across the middle rows."""
    img, px, rng = canvas((116, 160, 70), 20260928, 6)
    for y in range(16):
        for x in range(16):
            if y % 3 == 0:
                px[x, y] = shade(px[x, y], 0.82) + (255,)
            if 5 <= y <= 10 and 2 <= x <= 14:
                kern = (236, 196, 60) if (x + (y % 2)) % 2 else (212, 168, 40)
                px[x, y] = clamp(tuple(v + rng.uniform(-6, 6) for v in kern)) + (255,)
    return img


@texture("corn_end")
def corn_end():
    img, px, rng = canvas((190, 196, 130), 20260929, 8)
    for y in range(16):
        for x in range(16):
            if math.hypot(x + 0.5 - 8, y + 0.5 - 8) < 3.5:
                px[x, y] = (214, 206, 150, 255)
    return img


@texture("corn_silk")
def corn_silk():
    img, px, rng = canvas((150, 104, 56), 20260930, 14)
    return img


@texture("banana")
def banana():
    """A banana's side: yellow, faint green toward the stem, brown freckles."""
    img, px, rng = canvas((238, 210, 60), 20260931, 6)
    for y in range(16):
        k = 1.15 - 0.3 * abs(y + 0.5 - 6) / 10.0
        for x in range(16):
            c = px[x, y]
            if x < 3:
                c = mix(c, (150, 180, 60), 0.4)
            px[x, y] = shade(c, k) + (255,)
    for _ in range(8):
        x, y = rng.randrange(3, 16), rng.randrange(0, 16)
        px[x, y] = (120, 84, 36, 255)
    return img


@texture("banana_tip")
def banana_tip():
    return canvas((70, 52, 30), 20260932, 6)[0]


@texture("banana_crown")
def banana_crown():
    return canvas((124, 124, 58), 20260933, 10)[0]


@texture("stem")
def stem():
    return canvas((96, 66, 36), 20260934, 8)[0]


@texture("leaf")
def leaf():
    img, px, rng = canvas((64, 136, 48), 20260935, 8)
    for x in range(16):
        px[x, 8] = (120, 176, 90, 255)
    return img


# The beds, one per produce: a mash of that produce in its colours
BEDS = {
    "bed_apple_red": ([(176, 32, 34), (150, 26, 30), (196, 60, 40)], (60, 14, 14)),
    "bed_apple_green": ([(134, 188, 62), (118, 170, 56), (160, 198, 80)], (46, 66, 22)),
    "bed_apple_gold": ([(236, 192, 46), (220, 170, 36), (250, 214, 90)], (112, 80, 16)),
    "bed_orange": ([(236, 128, 26), (222, 112, 22), (244, 150, 40)], (100, 44, 8)),
    "bed_pear": ([(170, 190, 70), (150, 172, 60), (188, 196, 90)], (62, 66, 24)),
    "bed_tomato": ([(210, 38, 28), (186, 30, 24), (60, 126, 40)], (70, 14, 10)),
    "bed_potato": ([(168, 128, 80), (150, 110, 66), (182, 144, 94)], (70, 50, 30)),
    "bed_onion": ([(184, 116, 50), (164, 100, 40), (228, 222, 200)], (74, 46, 20)),
    "bed_lettuce": ([(110, 170, 60), (86, 150, 50), (160, 200, 100)], (36, 64, 22)),
    "bed_beet": ([(118, 22, 50), (96, 18, 40), (64, 124, 46)], (40, 10, 18)),
    "bed_carrot": ([(232, 118, 28), (214, 100, 22), (72, 140, 46)], (96, 44, 10)),
    "bed_corn": ([(116, 160, 70), (236, 196, 60), (98, 140, 58)], (44, 60, 22)),
    "bed_banana": ([(238, 210, 60), (220, 190, 50), (196, 196, 70)], (100, 80, 20)),
}
for _i, (_name, (_cols, _dark)) in enumerate(sorted(BEDS.items())):
    TEXTURES[_name] = (lambda c=_cols, d=_dark, s=20261000 + _i: mash(c, s, d))


# ------------------------------------------------------------------------------------------
# Elements
# ------------------------------------------------------------------------------------------
ALL = ("north", "south", "east", "west", "up", "down")


def r3(v):
    return round(v, 3)


def box(frm, to, tex, faces=ALL, per=None, full=False):
    """An element. Its UVs are fitted to its own size (so a board's grain is not stretched), or,
    with full=True, every face wears the whole texture (a piece of fruit, whose texture is one
    whole round piece)."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    uv = {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
          "south": [x0, 16 - y1, x1, 16 - y0],
          "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
          "west": [z0, 16 - y1, z1, 16 - y0],
          "up": [x0, z0, x1, z1],
          "down": [x0, 16 - z1, x1, 16 - z0]}
    out = {}
    for f in faces:
        u = [0, 0, 16, 16] if full else [min(16, max(0, v)) for v in uv[f]]
        if u[0] == u[2]:
            u[2] = u[0] + 0.01
        if u[1] == u[3]:
            u[3] = u[1] + 0.01
        out[f] = {"texture": (per or {}).get(f, tex), "uv": [r3(v) for v in u]}
    return {"from": [r3(v) for v in frm], "to": [r3(v) for v in to], "faces": out}


def rot(el, axis, angle, origin):
    if angle:
        el["rotation"] = {"origin": [r3(v) for v in origin], "axis": axis, "angle": angle}
    return el


def plane_x(x0, x1, y0, y1, z, tex):
    """A zero-thickness upright plane along x (north and south faces), whole texture."""
    return box((x0, y0, z), (x1, y1, z), tex, faces=("north", "south"), full=True)


def shifted(els, dx):
    out = []
    for el in els:
        el = json.loads(json.dumps(el))
        el["from"][0] = r3(el["from"][0] + dx)
        el["to"][0] = r3(el["to"][0] + dx)
        if "rotation" in el:
            el["rotation"]["origin"][0] = r3(el["rotation"]["origin"][0] + dx)
        out.append(el)
    return out


# --- the produce crate -------------------------------------------------------------------
CX0, CX1 = 1.0, 15.0        # outside of the end boards
CZ0, CZ1 = 2.5, 13.5        # outside of the side slats
CH = 7.0                    # rim height
END_T, SLAT_T = 1.0, 0.6
IX0, IX1 = CX0 + END_T, CX1 - END_T          # inside, along x
IZ0, IZ1 = CZ0 + SLAT_T, CZ1 - SLAT_T        # inside, along z
BED = 5.6                   # top of the bed of produce


def crate(bed_tex):
    els = []
    for x0 in (CX0, CX1 - END_T):
        els.append(box((x0, 0, CZ0), (x0 + END_T, CH, CZ1), "#slat",
                       per={"east": "#end", "west": "#end"}))
    for z0 in (CZ0, CZ1 - SLAT_T):
        for y0, y1 in ((0.3, 2.3), (2.7, 4.7), (5.1, 6.9)):
            els.append(box((IX0, y0, z0), (IX1, y1, z0 + SLAT_T), "#slat"))
    els.append(box((IX0, 0.3, IZ0), (IX1, 0.9, IZ1), "#floor", faces=("up", "down")))
    els.append(box((IX0, 0.9, IZ0), (IX1, BED, IZ1), bed_tex,
                   faces=("north", "south", "east", "west", "up")))
    return els


def heap(rng, piece, size, rows=2, cols=4, top=(2, 3)):
    """Pieces laid on the bed in a jittered grid, and a smaller layer on top of them, so the
    crate is heaped rather than level. piece(x, y, z, rng) -> elements, (x, z) its centre and y
    its bottom."""
    els = []
    sx = (IX1 - IX0 - 0.6) / cols
    sz = (IZ1 - IZ0 - 0.6) / rows
    for i in range(cols):
        for j in range(rows):
            x = IX0 + 0.3 + sx * (i + 0.5) + rng.uniform(-0.3, 0.3)
            z = IZ0 + 0.3 + sz * (j + 0.5) + rng.uniform(-0.3, 0.3)
            els += piece(x, BED - size * 0.4 + rng.uniform(-0.2, 0.2), z, rng)
    tr, tc = top
    for i in range(tc):
        for j in range(tr):
            x = 8 + (i - (tc - 1) / 2.0) * sx + rng.uniform(-0.3, 0.3)
            z = 8 + (j - (tr - 1) / 2.0) * sz + rng.uniform(-0.3, 0.3)
            els += piece(x, BED + size * 0.45, z, rng)
    return els


def fruit(tex, s, stem=True, leaf_chance=0.0, top_tex=None, neck=None):
    """A round piece of size s: one cube, maybe a stem, a leaf, a neck (a pear's) or its own top
    texture (a tomato's calyx)."""
    def piece(x, y, z, rng):
        h = s * rng.uniform(0.9, 1.0)
        angle = rng.choice((0, 22.5, -22.5))
        per = {"up": top_tex} if top_tex else None
        els = [rot(box((x - s / 2, y, z - s / 2), (x + s / 2, y + h, z + s / 2), tex,
                       faces=("north", "south", "east", "west", "up"), per=per, full=True),
                   "y", angle, (x, y, z))]
        top = y + h
        if neck:
            n = neck
            els.append(rot(box((x - n / 2, top - 0.3, z - n / 2), (x + n / 2, top + n * 0.8,
                                                                   z + n / 2), tex,
                               faces=("north", "south", "east", "west", "up"), full=True),
                           "y", angle, (x, y, z)))
            top += n * 0.8 - 0.3
        if stem:
            els.append(box((x - 0.2, top - 0.2, z - 0.2), (x + 0.2, top + 0.7, z + 0.2), "#stem",
                           faces=("north", "south", "east", "west", "up")))
        if rng.random() < leaf_chance:
            els.append(rot(box((x, top + 0.2, z - 0.45), (x + 1.6, top + 0.35, z + 0.45),
                               "#leaf", faces=("up", "down")),
                           "z", 22.5, (x, top + 0.2, z)))
        return els
    return piece


def onion(x, y, z, rng):
    s = 2.9
    tex = "#item" if rng.random() < 0.68 else "#item2"
    angle = rng.choice((0, 22.5, -22.5))
    els = [rot(box((x - s / 2, y, z - s / 2), (x + s / 2, y + s, z + s / 2), tex,
                   faces=("north", "south", "east", "west", "up"), full=True),
               "y", angle, (x, y, z))]
    els.append(box((x - 0.35, y + s - 0.2, z - 0.35), (x + 0.35, y + s + 0.7, z + 0.35),
                   "#neck", faces=("north", "south", "east", "west", "up")))
    return els


def potato(x, y, z, rng):
    lx, ly, lz = rng.uniform(3.2, 3.8), rng.uniform(2.0, 2.4), rng.uniform(2.3, 2.7)
    angle = rng.choice((0, 22.5, -22.5, 45))
    return [rot(box((x - lx / 2, y, z - lz / 2), (x + lx / 2, y + ly, z + lz / 2), "#item",
                    faces=("north", "south", "east", "west", "up"), full=True),
                "y", angle, (x, y, z))]


def lettuce_heap(rng):
    els = []
    s = 4.4
    for i, x in enumerate((4.8, 8.0, 11.2)):
        for j, z in enumerate((6.0, 10.0)):
            x1 = x + rng.uniform(-0.1, 0.1)
            z1 = z + rng.uniform(-0.1, 0.1)
            y = BED - 1.4 + (0.6 if i == 1 else 0)
            h = s * rng.uniform(0.85, 0.95)
            els.append(rot(box((x1 - s / 2, y, z1 - s / 2), (x1 + s / 2, y + h, z1 + s / 2),
                               "#item", faces=("north", "south", "east", "west", "up"),
                               full=True), "y", rng.choice((0, 22.5, -22.5)), (x1, y, z1)))
            o = 1.9  # turned 45 degrees it reaches 2.7 out: inside the crate
            els.append(rot(box((x1 - o, y - 0.2, z1 - o), (x1 + o, y + 2.2, z1 + o), "#leafy",
                               faces=("north", "south", "east", "west"), full=True),
                           "y", 45, (x1, y, z1)))
    return els


def beet_piece(x, y, z, rng, greens):
    s = 2.8
    els = [rot(box((x - s / 2, y, z - s / 2), (x + s / 2, y + s, z + s / 2), "#item",
                   faces=("north", "south", "east", "west", "up"), full=True),
               "y", rng.choice((0, 22.5, -22.5)), (x, y, z))]
    if greens:
        top = y + s - 0.4
        for angle in (45, -45):
            els.append(rot(plane_x(x - 1.8, x + 1.8, top, top + 4.2, z, "#greens"),
                           "y", angle, (x, top, z)))
    return els


def beets(rng):
    els = []
    count = [0]

    def piece(x, y, z, r):
        count[0] += 1
        return beet_piece(x, y, z, r, count[0] % 3 == 0)
    els += heap(rng, piece, 2.8)
    return els


def carrot_piece(x0, y, z, flip):
    """A carrot lying along x: its thick end at x0 with its greens, its tip toward +x (or -x)."""
    body = [box((0, y, z - 0.85), (4.6, y + 1.7, z + 0.85), "#item",
                faces=("north", "south", "up", "east", "west"), full=True),
            box((4.6, y + 0.35, z - 0.5), (6.2, y + 1.35, z + 0.5), "#item",
                faces=("north", "south", "up", "east"), full=True),
            box((-1.8, y + 0.45, z - 0.45), (0, y + 1.25, z + 0.45), "#greens",
                faces=("north", "south", "up", "west"), full=True)]
    out = []
    for el in body:
        if flip:  # mirror about x = 0 (faces of a symmetric texture need no remap)
            f, t = el["from"][0], el["to"][0]
            el["from"][0], el["to"][0] = r3(-t), r3(-f)
            faces = el["faces"]
            if "east" in faces or "west" in faces:
                e, w = faces.pop("east", None), faces.pop("west", None)
                if e:
                    faces["west"] = e
                if w:
                    faces["east"] = w
        out.append(el)
    return shifted(out, x0)


def carrots(rng):
    """One carrot to a row, greens at alternate ends, and three more lying across the top."""
    els = []
    zs = [IZ0 + 1.0 + k * 1.95 for k in range(5)]
    for k, z in enumerate(zs):
        y = BED - 0.9 + rng.uniform(-0.15, 0.15)
        if k % 2:
            els += carrot_piece(IX1 - 2.0 - rng.uniform(0, 1.6), y, z, True)
        else:
            els += carrot_piece(IX0 + 2.0 + rng.uniform(0, 1.6), y, z, False)
    for k, z in enumerate((zs[1] + 0.9, zs[2] + 1.0, zs[3] + 0.9)):
        if k % 2:
            els += carrot_piece(4.6 + rng.uniform(0, 0.6), BED + 0.7, z, False)
        else:
            els += carrot_piece(11.4 - rng.uniform(0, 0.6), BED + 0.7, z, True)
    return els


def cob(x0, y, z, flip, rng):
    length = 7.6
    d = 2.3
    els = [box((0, y, z - d / 2), (length, y + d, z + d / 2), "#item",
               per={"east": "#cobend", "west": "#cobend"},
               faces=("north", "south", "up", "east", "west"), full=True),
           box((length, y + 0.6, z - 0.55), (length + 1.3, y + 1.7, z + 0.55), "#silk",
               faces=("north", "south", "up", "east"), full=True)]
    if flip:
        for el in els:
            f, t = el["from"][0], el["to"][0]
            el["from"][0], el["to"][0] = r3(-t), r3(-f)
            faces = el["faces"]
            e, w = faces.pop("east", None), faces.pop("west", None)
            if e:
                faces["west"] = e
            if w:
                faces["east"] = w
    return shifted(els, x0)


def corn(rng):
    els = []
    zs = [IZ0 + 1.35 + k * 2.45 for k in range(4)]
    for k, z in enumerate(zs):
        y = BED - 1.3 + rng.uniform(-0.1, 0.1)
        if k % 2:
            els += cob(IX1 - 0.2 - rng.uniform(0, 1.2), y, z, True, rng)
        else:
            els += cob(IX0 + 0.2 + rng.uniform(0, 1.2), y, z, False, rng)
    for k, z in enumerate((zs[0] + 1.2, zs[1] + 1.2, zs[2] + 1.2)):
        if k % 2:
            els += cob(IX1 - 1.4, BED + 0.8, z, True, rng)
        else:
            els += cob(IX0 + 1.4, BED + 0.8, z, False, rng)
    return els


def banana_hand(cx, y, z0, flip):
    """A hand of four bananas lying on their sides: a crown at the stem end, each finger straight
    for most of its length and then turned up toward its tip."""
    els = [box((cx - 4.2, y + 0.2, z0 - 0.1), (cx - 3.2, y + 1.5, z0 + 5.0), "#crown")]
    for i in range(4):
        zc = z0 + 0.55 + i * 1.25
        els.append(box((cx - 3.2, y, zc - 0.55), (cx + 0.8, y + 1.1, zc + 0.55), "#item",
                       faces=("north", "south", "up", "down"), full=True))
        els.append(rot(box((cx + 0.8, y, zc - 0.55), (cx + 3.3, y + 1.1, zc + 0.55), "#item",
                           per={"east": "#tip"}, faces=("north", "south", "up", "down", "east"),
                           full=True), "z", 22.5, (cx + 0.8, y, zc)))
    if flip:
        for el in els:
            f, t = el["from"][0], el["to"][0]
            el["from"][0], el["to"][0] = r3(2 * cx - t), r3(2 * cx - f)
            faces = el["faces"]
            e, w = faces.pop("east", None), faces.pop("west", None)
            if e:
                faces["west"] = e
            if w:
                faces["east"] = w
            if "rotation" in el:
                el["rotation"]["origin"][0] = r3(2 * cx - el["rotation"]["origin"][0])
                el["rotation"]["angle"] = -el["rotation"]["angle"]
    return els


def bananas(rng):
    els = []
    els += banana_hand(6.6, BED - 0.5, IZ0 + 0.4, False)
    els += banana_hand(9.4, BED - 0.5, IZ1 - 5.4, True)
    els += banana_hand(8.4, BED + 0.6, 5.6, False)
    return els


# --- the carrot barrel -------------------------------------------------------------------
TAN_22_5 = math.tan(math.pi / 8)


def octagon(cx, cz, r, y0, y1, tex, top=None, bottom=None):
    """An upright regular octagon of inradius r: four rectangles, two square and two turned 45
    degrees, each drawing only its long sides and, if given, its caps (a hair apart so the four
    coplanar caps do not fight)."""
    a = r * TAN_22_5
    els = []
    for k, (h1, h2, turned) in enumerate(((r, a, False), (a, r, False), (r, a, True),
                                          (a, r, True))):
        eps = 0.004 * k
        faces = ["east", "west"] if h1 == r else ["north", "south"]
        per = {}
        if top:
            faces.append("up")
            per["up"] = top
        if bottom:
            faces.append("down")
            per["down"] = bottom
        el = box((cx - h1, y0 - eps, cz - h2), (cx + h1, y1 + eps, cz + h2), tex, faces=faces,
                 per=per)
        if turned:
            rot(el, "y", 45, (cx, y0, cz))
        els.append(el)
    return els


def barrel(rng):
    els = []
    els += octagon(8, 8, 5.5, 0, 1.5, "#side", bottom="#head")
    els += octagon(8, 8, 6.0, 1.5, 9.5, "#side", top="#head", bottom="#head")
    els += octagon(8, 8, 5.5, 9.5, 11.0, "#side", top="#bed")
    # carrots standing in the barrel, greens up, leaning every way
    spots = ((6.0, 6.2, "x", 22.5), (10.0, 6.4, "z", -22.5), (8.2, 8.4, "x", 0),
             (5.8, 10.0, "z", 22.5), (10.2, 9.8, "x", -22.5), (8.0, 11.6, "z", 0),
             (8.2, 4.8, "z", 22.5))
    for x, z, axis, angle in spots:
        y = 9.6 + rng.uniform(-0.3, 0.3)
        el = box((x - 0.8, y, z - 0.8), (x + 0.8, y + 2.2, z + 0.8), "#item",
                 faces=("north", "south", "east", "west"), full=True)
        els.append(rot(el, axis, angle, (x, 11, z)))
        g = box((x - 0.5, y + 2.2, z - 0.5), (x + 0.5, y + 4.4, z + 0.5), "#greens",
                faces=("north", "south", "east", "west", "up"), full=True)
        els.append(rot(g, axis, angle, (x, 11, z)))
    # a couple lying on the heap
    els += carrot_piece(4.2, 11.0, 7.0, False)
    els += carrot_piece(11.6, 11.0, 9.4, True)
    return els


# --- the shipping crate (two blocks long, x 0..32) ----------------------------------------
def shipping_half(west):
    """One cell of the closed crate, in that cell's own coordinates, with the crate's end at x = 0
    (west) or x = 16 (east). Nothing crosses x = 16, so every face's UVs stay within the cell."""
    def X(a, b):
        return (a, b) if west else (16 - b, 16 - a)
    els = []
    x0, x1 = X(0.5, 16)
    faces = ["north", "south", "up", "down", "west" if west else "east"]
    els.append(box((x0, 0.5, 1.0), (x1, 14.5, 15.0), "#side", faces=faces))
    # corner posts at the end
    ex0, ex1 = X(0, 2)
    for z0, z1 in ((0.5, 2.5), (13.5, 15.5)):
        els.append(box((ex0, 0, z0), (ex1, 15, z1), "#frame"))
    # end cross members, bottom and top, between the posts
    for y0, y1 in ((0, 2), (13, 15)):
        els.append(box((ex0, y0, 2.5), (ex1, y1, 13.5), "#frame"))
    # a mid rail across the end
    els.append(box((ex0, 6.5, 2.5), (ex1, 8.5, 13.5), "#frame", faces=("east", "west", "up",
                                                                        "down")))
    # long rails along the four long edges, from the post to the middle
    rx0, rx1 = X(2, 16)
    for y0, y1 in ((0, 2), (13, 15)):
        for z0, z1 in ((0.5, 2.5), (13.5, 15.5)):
            els.append(box((rx0, y0, z0), (rx1, y1, z1), "#frame",
                           faces=("north", "south", "up", "down")))
    # the middle upright on each long side and the middle batten across the top
    mx0, mx1 = X(15, 16)
    for z0, z1 in ((0.5, 1.0), (15.0, 15.5)):
        els.append(box((mx0, 2, z0), (mx1, 13, z1), "#frame",
                       faces=("north", "south", "west" if west else "east")))
    els.append(box((mx0, 14.5, 2.5), (mx1, 15, 13.5), "#frame",
                   faces=("up", "north", "south", "west" if west else "east")))
    # mid rail along each long side
    for z0, z1 in ((0.5, 1.0), (15.0, 15.5)):
        mr0, mr1 = X(2, 15)
        els.append(box((mr0, 6.75, z0), (mr1, 8.25, z1), "#frame",
                       faces=("north", "south", "up", "down")))
    return els


def shipping_crate():
    return shipping_half(True) + shifted(shipping_half(False), 16)


# ------------------------------------------------------------------------------------------
# Catalogue: registry -> (builder, textures, seed). Order is the tab's.
# ------------------------------------------------------------------------------------------
def T(name):
    return TEX_REF % name


CRATE_TEX = {"slat": T("crate_slat"), "end": T("crate_end"), "floor": T("crate_floor"),
             "stem": T("stem"), "leaf": T("leaf")}


def crate_of(piece_or_heap, item, bed, extra=None, size=3.0, whole=False):
    def build(rng):
        els = crate("#bed")
        els += piece_or_heap(rng) if whole else heap(rng, piece_or_heap, size)
        return els
    tex = dict(CRATE_TEX)
    tex.update({"item": T(item), "bed": T(bed)})
    tex.update(extra or {})
    return build, tex


CATALOGUE = {
    "applecrate": crate_of(fruit("#item", 3.0, leaf_chance=0.35), "apple_red", "bed_apple_red"),
    "bananacrate": crate_of(bananas, "banana", "bed_banana",
                            {"tip": T("banana_tip"), "crown": T("banana_crown")}, whole=True),
    "beetcrate": crate_of(beets, "beet", "bed_beet", {"greens": T("beet_greens")}, whole=True),
    "carrotbarrel": (barrel, {"side": T("barrel_side"), "head": T("barrel_head"),
                              "bed": T("bed_carrot"), "item": T("carrot"),
                              "greens": T("carrot_top")}),
    "carrotcrate": crate_of(carrots, "carrot", "bed_carrot", {"greens": T("carrot_top")},
                            whole=True),
    "corncrate": crate_of(corn, "corn_husk", "bed_corn",
                          {"cobend": T("corn_end"), "silk": T("corn_silk")}, whole=True),
    "goldenapples": crate_of(fruit("#item", 3.0, leaf_chance=0.25), "apple_gold",
                             "bed_apple_gold"),
    "greenapplecrate": crate_of(fruit("#item", 3.0, leaf_chance=0.3), "apple_green",
                                "bed_apple_green"),
    "largecrate": (lambda rng: shipping_crate(),
                   {"side": T("shipping_side"), "frame": T("shipping_frame")}),
    "lettucecrate": crate_of(lettuce_heap, "lettuce", "bed_lettuce",
                             {"leafy": T("lettuce_leaf")}, whole=True),
    "onioncrate": crate_of(onion, "onion_brown", "bed_onion",
                           {"item2": T("onion_white"), "neck": T("onion_neck")}, size=2.9),
    "orangecrate": crate_of(fruit("#item", 3.0, stem=False), "orange", "bed_orange"),
    "pearcrate": crate_of(fruit("#item", 2.8, neck=1.8), "pear", "bed_pear", size=2.8),
    "potatoecrate": crate_of(potato, "potato", "bed_potato", size=2.3),
    "tomatoecrate": crate_of(fruit("#item", 2.9, stem=False, top_tex="#top"), "tomato",
                             "bed_tomato", {"top": T("tomato_top")}, size=2.9),
}

# The large crate is two blocks long: its inventory icon is scaled and pulled back to centre.
LARGE_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [2.3, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.18, 0.18, 0.18]},
    "fixed": {"rotation": [0, 0, 0], "translation": [-4, 0, 0], "scale": [0.35, 0.35, 0.35]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                              "scale": [0.25, 0.25, 0.25]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                              "scale": [0.28, 0.28, 0.28]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                             "scale": [0.28, 0.28, 0.28]},
}


def models():
    out = {}
    for i, (name, (build, tex)) in enumerate(CATALOGUE.items()):
        rng = random.Random(20260950 + i)
        els = build(rng)
        textures = dict(tex)
        textures["particle"] = tex.get("slat", tex.get("side"))
        # only the textures the model's faces use
        used = {f["texture"][1:] for el in els for f in el["faces"].values()}
        textures = {k: v for k, v in textures.items() if k in used or k == "particle"}
        body = {"parent": "block/block", "textures": textures, "elements": els}
        if name == "largecrate":
            body["display"] = LARGE_DISPLAY
        for el in els:
            for v in el["from"] + el["to"]:
                assert -16 <= v <= 32, (name, el)
        out[name] = body
    return out


def blockstate(name):
    return {"forge_marker": 1,
            "defaults": {"model": MODEL_REF % name},
            "variants": {
                "facing": {"down": {"x": 90}, "east": {"y": 90}, "north": {}, "south": {"y": 180},
                           "up": {"x": 270}, "west": {"y": 270}},
                "inventory": [{}], "normal": [{}]}}


def bounds(els):
    """The unrotated extent of the solid parts (not leaves, greens or silk)."""
    lo, hi = [99.0] * 3, [-99.0] * 3
    for el in els:
        texs = {f["texture"] for f in el["faces"].values()}
        if texs & {"#leaf", "#greens", "#silk", "#stem", "#neck", "#leafy"}:
            continue
        for k in range(3):
            lo[k] = min(lo[k], el["from"][k])
            hi[k] = max(hi[k], el["to"][k])
    return lo, hi


# ------------------------------------------------------------------------------------------
# Writing
# ------------------------------------------------------------------------------------------
def dump(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


def generate(assets):
    written = []
    for name, fn in sorted(TEXTURES.items()):
        rel = "textures/blocks/%s/%s.png" % (SUB, name)
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        written.append(rel)
    for name, body in models().items():
        rel = "models/block/%s/%s.json" % (SUB, name)
        dump(os.path.join(assets, rel), body)
        written.append(rel)
        rel = "blockstates/%s.json" % name
        dump(os.path.join(assets, rel), blockstate(name))
        written.append(rel)
    model_depth.separate(assets, written, dump)
    return written


def same(a, b):
    if not os.path.exists(a) or not os.path.exists(b):
        return False
    with open(a, "rb") as fa, open(b, "rb") as fb:
        return fa.read() == fb.read()


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    ap.add_argument("--boxes", action="store_true", help="print each block's solid extent")
    args = ap.parse_args()
    if args.boxes:
        for name, body in models().items():
            lo, hi = bounds(body["elements"])
            print("%-16s new AxisAlignedBB(%s)" % (
                name, ", ".join("%.4f" % (v / 16.0) for v in lo + hi)))
        return 0
    if args.check:
        tmp = tempfile.mkdtemp(prefix="csm_produce_")
        try:
            written = generate(tmp)
            stale = [rel for rel in written
                     if not same(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
            if stale:
                print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
                return 1
            print("%d produce crate files are up to date" % len(written))
            return 0
        finally:
            shutil.rmtree(tmp, ignore_errors=True)
    written = generate(ASSETS)
    print("wrote %d produce crate files" % len(written))
    return 0


if __name__ == "__main__":
    sys.exit(main())
