#!/usr/bin/env python3
"""
gen_furniture_living.py -- the Residential tab's living extras in the Furniture & Novelties
module: flat-screen TVs on a stand or on the wall, in one- and two-block sizes and the big
3 x 2 and 4 x 2 screens, and an old tube TV, all changing channel on a click; a hi-fi stereo
that plays music discs, bookshelf speakers and a subwoofer; an upright piano that plays and its bench; a bedside digital clock and a wall
clock that tell the world's time; photo frames, wall art and house plants; a fireplace, a
ceiling fan with a light, floor and table lamps and candles; a door mat, a light switch, a
doorbell and a storage crate.

Borrows gen_furniture_residential.py's element helpers, woods and output, the bedroom's cut into
cells, rug geometry and item displays, and the appliances' surface rests and small-piece
displays (all imported, not copied), and writes, under
modules/furnishings/src/main/resources/assets/csm:

  * textures/blocks/furniture/living/*.png  the TV channels (animated, with their .mcmeta: news,
    sports, nature, colour bars, snow and the dark screen), the hi-fi's front, display and
    turntable, speaker fronts, the piano's keys and lacquer, the fireplace's stone, firebrick,
    logs and animated flame, the clock dial, photos and artworks, house plant leaves, pots,
    lamp shades, wax, coir, and the rest
  * models/block/furniture/living/base/*.json  the geometry
  * models/block/furniture/living/<registry>_<part>.json  a finish's copy of each part a
    multipart blockstate picks
  * blockstates/<registry>.json, and models/item/<registry>.json where the state has no
    inventory variant
  * the tile names in all four languages, and the light switch's messages, by key

Every model faces north with its back at +Z, as the rest of the tab's do. Real-world scale,
1 block = 1 m, small things at one and a quarter to one and a half times real size as the
kitchen's are. What each piece does (a TV's channel, the stereo's disc, the piano's keys, the
clocks' time, the fan's blades) is its Java class's; the positions those classes and the clock
and fan renderers read are named here: PIANO_KEYS, FIREBOX, DIGITAL_DISPLAY, WALL_CLOCK_DIAL.

Two-block pieces (the large TVs, the piano, the fireplace, the wide wall art) are drawn whole,
two blocks wide, and cut into the two blocks (the big TVs into every block of their grid,
cut_grid); a picture drawn across both (a screen, the keys, the flames, a canvas) keeps one
texture across the cut (spanned UVs).

Usage:
    python gen_furniture_living.py              # write everything
    python gen_furniture_living.py --check      # fail if the tree has drifted
    python gen_furniture_living.py --fragments  # print the tab registration lines

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

ASSETS = R.ASSETS
SUB = "furniture/living"
MODEL = "csm:%s/" % SUB
BASE = MODEL + "base/"
ROT = R.ROT
RT = R.T
el, board, octagon = R.el, R.board, R.octagon
ALL = R.ALL
SIDES = ("north", "south", "east", "west")
NO_DOWN = SIDES + ("up",)
NO_BACK = ("north", "east", "west", "up", "down")
shade, clamp = R.shade, R.clamp


def T(name):
    return "csm:blocks/%s/%s" % (SUB, name)


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def blank(size):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def solid(colour, seed, size=16, grain=3):
    return R.flat(colour, seed, size=size, grain=grain)


def glossy(base, seed, size=16):
    """Glossy lacquer or plastic: nearly flat, a soft diagonal sheen across it."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = 1.0 + rng.uniform(-0.015, 0.015)
            d = (x + y) % size
            if d in (4, 5):
                k += 0.35
            elif d == 6:
                k += 0.15
            px[x, y] = clamp(tuple(v * k + (8 if d in (4, 5) else 0) for v in base)) + (255,)
    return img


def strip(frames):
    """Frames of one size stacked into an animation strip."""
    size = frames[0].size[0]
    out = Image.new("RGBA", (size, size * len(frames)))
    for i, f in enumerate(frames):
        out.paste(f, (0, i * size))
    return out


def lerp(a, b, t):
    return clamp(tuple(a[i] + (b[i] - a[i]) * t for i in range(3)))


def ellipse_in(x, y, cx, cy, rx, ry):
    return ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1.0


# --- TV channels: 64 px frames. A flat screen shows rows 14 to 50 (16:9); a tube TV also
# --- crops columns 8 to 56 (4:3). What matters is drawn inside those.
SCREEN_169 = (0, 3.5, 16, 12.5)
SCREEN_43 = (2, 3.5, 14, 12.5)
CH = 64


def tv_off():
    img = blank(CH)
    px = img.load()
    for y in range(CH):
        for x in range(CH):
            c = (13, 15, 19)
            d = x - y * 0.7
            if 18 < d < 30:
                c = (24, 27, 33)
            elif 30 <= d < 33:
                c = (19, 21, 26)
            px[x, y] = c + (255,)
    return img


def tv_static():
    frames = []
    for f in range(8):
        rng = random.Random(900 + f)
        img = blank(CH)
        px = img.load()
        band = (f * 9) % CH
        for y in range(CH):
            lift = 28 if (y - band) % CH < 5 else 0
            for x in range(CH):
                v = min(255, rng.randint(24, 225) + lift)
                px[x, y] = (v, v, v, 255)
        frames.append(img)
    return strip(frames)


def tv_bars():
    img = blank(CH)
    px = img.load()
    top = [(192, 192, 192), (192, 192, 0), (0, 192, 192), (0, 192, 0), (192, 0, 192),
           (192, 0, 0), (0, 0, 192)]
    mid = [(0, 0, 192), (19, 19, 19), (192, 0, 192), (19, 19, 19), (0, 192, 192), (19, 19, 19),
           (192, 192, 192)]
    low = [(0, 0, 11, (0, 33, 76)), (11, 0, 22, (235, 235, 235)), (22, 0, 33, (50, 0, 106)),
           (33, 0, 46, (19, 19, 19)), (46, 0, 49, (9, 9, 9)), (49, 0, 52, (19, 19, 19)),
           (52, 0, 55, (29, 29, 29)), (55, 0, 64, (19, 19, 19))]
    for y in range(CH):
        for x in range(CH):
            i = min(6, x * 7 // CH)
            if y < 38:
                c = top[i]
            elif y < 41:
                c = mid[i]
            else:
                c = next(col for x0, _z, x1, col in low if x0 <= x < x1)
            px[x, y] = c + (255,)
    return img


def tv_nature():
    rng = random.Random(911)
    clouds = [(rng.uniform(0, 64), rng.uniform(15, 23), rng.uniform(4, 7), rng.uniform(1.4, 2.4))
              for _ in range(4)]
    trees = sorted(rng.sample(range(2, 62), 14))
    frames = []
    for f in range(16):
        img = blank(CH)
        px = img.load()
        for y in range(CH):
            for x in range(CH):
                # Sky, lighter to the horizon.
                c = lerp((96, 150, 214), (196, 220, 236), max(0.0, min(1.0, (y - 12) / 20.0)))
                for cx, cy, rx, ry in clouds:
                    for wrap in (-64, 0, 64):
                        if ellipse_in(x, y, cx + f * 4 + wrap, cy, rx, ry):
                            c = (246, 248, 250) if y < cy else (222, 228, 236)
                # Far mountains, snow on their peaks.
                ridge = 27 - 5 * math.sin(x * 0.16 + 1.0) - 3 * math.sin(x * 0.41 + 2.2)
                if y >= ridge and y < 34:
                    c = (230, 236, 242) if y < ridge + 1.5 and ridge < 23.5 else \
                        lerp((118, 136, 168), (96, 116, 146), (y - ridge) / 10.0)
                # Near hills.
                hill = 32.5 - 2 * math.sin(x * 0.23 + 0.4) - 1.2 * math.sin(x * 0.55)
                if y >= hill and y < 36:
                    c = (52, 94, 56) if (x + y) % 5 else (60, 104, 62)
                if y >= 36:
                    # The lake: the hills' dark reflection near the shore, the sky's further out,
                    # and a shimmer running across it.
                    t = (y - 36) / 28.0
                    c = lerp((56, 104, 142), (34, 72, 108), t)
                    if y < 39:
                        c = lerp((42, 76, 64), c, (y - 36) / 3.0)
                    # Short glints on every third row, drifting along it.
                    if y > 38 and y % 3 == 0 and ((x - f * 2 + y * 11) // 3) % 7 == 0:
                        c = shade(c, 1.45)
                px[x, y] = c + (255,)
        # Pines along the far shore.
        for tx in trees:
            h = 3 + (tx * 7) % 3
            for dy in range(h):
                w = dy // 2
                for dx in range(-w, w + 1):
                    if 0 <= tx + dx < CH:
                        px[tx + dx, 35 - h + 1 + dy] = (30, 62, 40, 255)
        frames.append(img)
    return strip(frames)


def tv_sports():
    rng = random.Random(912)
    crowd = [[rng.choice([(180, 60, 60), (60, 80, 170), (220, 200, 90), (230, 230, 230),
                          (60, 60, 60), (140, 110, 90)]) for _x in range(CH)] for _y in range(19)]
    players = [((200, 36, 40) if i < 5 else (36, 66, 196), rng.uniform(8, 56),
                rng.uniform(25, 48), rng.uniform(3, 9), rng.uniform(1, 4), rng.uniform(0, 6.28))
               for i in range(10)]
    frames = []
    for f in range(16):
        img = blank(CH)
        px = img.load()
        ph = f / 16.0 * 2 * math.pi
        for y in range(CH):
            for x in range(CH):
                if y < 17:
                    c = crowd[y][x]
                    if (x + y + f) % 7 == 0:
                        c = shade(c, 1.15)
                    c = shade(c, 0.75)
                elif y < 19:
                    c = [(210, 40, 40), (240, 240, 240), (30, 90, 200)][(x // 11 + y) % 3]
                else:
                    c = (62, 142, 62) if ((x + int((y - 19) * 0.4)) // 8) % 2 else (72, 156, 70)
                    if y == 20 or x == 32 or ((x - 32) / 9.0) ** 2 + ((y - 36) / 5.5) ** 2 < 1.0 \
                            <= ((x - 32) / 8.0) ** 2 + ((y - 36) / 4.5) ** 2:
                        c = (236, 240, 236)
                    if (x in (5, 58) and 26 <= y <= 46) or (y in (26, 46) and (x <= 5 or x >= 58)):
                        c = (236, 240, 236)
                px[x, y] = c + (255,)
        for colour, bx, by, ax, ay, p in players:
            x = int(round(bx + ax * math.sin(ph + p)))
            y = int(round(by + ay * math.sin(2 * ph + p)))
            for dx, dy, col in ((0, 0, (230, 190, 150)), (0, 1, colour), (1, 1, colour),
                                (0, 2, colour), (1, 2, colour), (0, 3, (30, 30, 30))):
                if 0 <= x + dx < CH and 0 <= y + dy < CH:
                    px[x + dx, y + dy] = col + (255,)
        bx = int(round(32 + 14 * math.sin(ph)))
        by = int(round(37 + 6 * math.sin(2 * ph + 1.0)))
        px[bx, by] = (255, 255, 255, 255)
        # The score in the corner: two teams' chips and the score.
        for y in range(15, 19):
            for x in range(3, 23):
                c = (22, 22, 34)
                if y in (16, 17):
                    if 4 <= x <= 5:
                        c = (200, 36, 40)
                    elif 13 <= x <= 14:
                        c = (36, 66, 196)
                    elif x in (7, 8, 10, 16, 17, 19) or (x == 21 and y == 16):
                        c = (240, 240, 240)
                px[x, y] = c + (255,)
        frames.append(img)
    return strip(frames)


def tv_news():
    rng = random.Random(913)
    words = []
    x = 0
    while x < CH:
        w = rng.randint(3, 7)
        words.append((x, w))
        x += w + rng.randint(2, 4)
    frames = []
    for f in range(16):
        img = blank(CH)
        px = img.load()
        for y in range(CH):
            for x in range(CH):
                c = lerp((14, 30, 72), (34, 74, 136), max(0.0, min(1.0, (y - 10) / 30.0)))
                # A globe's lines behind the anchor, to the right.
                r = math.hypot(x - 50, y - 26)
                if 14 < r < 15 or (r < 15 and (abs(x - 50) in (5, 10) or (y - 26) in (-7, 0, 7))):
                    c = shade(c, 1.35)
                px[x, y] = c + (255,)
        # The anchor: head and hair, the suit with a white collar and a red tie.
        for y in range(18, 42):
            for x in range(16, 46):
                col = None
                if ellipse_in(x, y, 30.5, 24.5, 4.2, 5.0):
                    col = (226, 186, 152)
                    if y < 22 or (y < 25 and abs(x + 0.5 - 30.5) > 3.2):
                        col = (58, 40, 30)
                    if y == 27 and 29 <= x <= 31 and f % 4 in (1, 2):
                        col = (120, 60, 60)
                    if y == 24 and x in (29, 32):
                        col = (40, 30, 30)
                w = 5 + (y - 29) * 0.8
                if y >= 29 and abs(x + 0.5 - 30.5) <= w:
                    col = (36, 40, 56)
                    if y < 34 and abs(x + 0.5 - 30.5) <= (34 - y) * 0.6:
                        col = (236, 236, 240)
                    if y >= 30 and abs(x + 0.5 - 30.5) < 0.9:
                        col = (180, 30, 36)
                if y == 29 and abs(x + 0.5 - 30.5) <= 2:
                    col = (226, 186, 152)
                if col:
                    px[x, y] = col + (255,)
        for y in range(38, CH):
            for x in range(CH):
                if y < 40:
                    c = (70, 78, 98) if y == 38 else (44, 50, 66)
                elif y < 45:
                    c = (190, 28, 38) if x < 46 else (240, 240, 240)
                    if y in (41, 43) and x < 44 and (x // 4) % 3 != 2:
                        c = (250, 250, 250)
                    if 47 <= x <= 52 and y in (41, 42, 43) and (x + y) % 2:
                        c = (190, 28, 38)
                else:
                    c = (14, 14, 20)
                    u = (x + f * 4) % CH
                    if y in (47, 48) and any(wx <= u < wx + ww for wx, ww in words):
                        c = (236, 236, 236)
                px[x, y] = c + (255,)
        for y in range(15, 19):
            for x in range(47, 55):
                if y in (15, 18) or x in (47, 54) or (y == 16 and x % 2):
                    px[x, y] = (236, 236, 240, 255)
        frames.append(img)
    return strip(frames)


# --- the tube TV, the hi-fi and speakers --------------------------------------------------
def crt_panel():
    """A tube TV's control strip, drawn to be stretched onto a panel 2 wide and 7 high: two
    knobs and three buttons."""
    img = solid((128, 128, 124), 921)
    px = img.load()
    for y in range(16):
        for x in range(16):
            for cy in (2.5, 6.0):
                if ellipse_in(x, y, 8, cy, 5.5, 1.6):
                    px[x, y] = (40, 40, 42, 255) if ellipse_in(x, y, 8, cy, 3.5, 1.0) else \
                        (70, 70, 72, 255)
            if 9 <= y <= 14 and y % 2 == 1 and 4 <= x <= 11:
                px[x, y] = (60, 60, 62, 255)
    return img


def grille():
    img = solid((52, 52, 54), 922, grain=2)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if y % 3 == 1 and 1 <= x <= 14:
                px[x, y] = (22, 22, 24, 255)
    return img


def hifi_front():
    """The receiver's face, stretched onto 12 by 4: a slot for the tuner's dial, a row of
    buttons, the brand-free badge and a headphone socket."""
    img = R.metal((40, 40, 44), 923, size=32)
    px = img.load()
    for y in range(32):
        for x in range(32):
            if y in (4, 5) and 4 <= x <= 22:
                px[x, y] = (18, 18, 20, 255)
            if y in (24, 25, 26) and 3 <= x <= 22 and x % 4 in (0, 1, 2):
                px[x, y] = (150, 150, 156, 255) if y != 26 else (100, 100, 104, 255)
            if 27 <= x <= 29 and 22 <= y <= 27:
                px[x, y] = (20, 20, 22, 255) if ellipse_in(x, y, 28.5, 25, 1.5, 3) else px[x, y]
    return img


def display(on):
    img = blank(16)
    px = img.load()
    rng = random.Random(924)
    heights = [rng.randint(3, 13) for _ in range(16)]
    for y in range(16):
        for x in range(16):
            c = (16, 26, 30)
            if on:
                c = (10, 28, 36)
                if x % 2 == 0 and 16 - y <= heights[x]:
                    c = (90, 220, 240) if 16 - y < heights[x] - 2 else (200, 250, 255)
            px[x, y] = c + (255,)
    return img


def platter(record):
    """The turntable's top: a rubber mat, or a record on it, a disc centred where the platter
    stands (7.25, 8) in the model, radius 3.25 px, on a 32 px texture."""
    img = blank(32)
    px = img.load()
    cx, cy = 14.5, 16.0
    for y in range(32):
        for x in range(32):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if record:
                c = (20, 20, 22) if int(r * 2) % 2 else (30, 30, 33)
                if r < 2.4:
                    c = (178, 36, 40) if r > 0.8 else (220, 220, 220)
                if r < 0.5:
                    c = (60, 60, 60)
            else:
                c = (46, 46, 50) if int(r) % 2 else (54, 54, 58)
                if r < 0.8:
                    c = (190, 190, 196)
            px[x, y] = c + (255,)
    return img


def speaker_front(size=32):
    """A bookshelf speaker's baffle, stretched onto 5.5 by 8.5: a tweeter dome over a woofer."""
    img = solid((24, 24, 26), 925, size=size, grain=2)
    px = img.load()
    kx, ky = size / 5.5, size / 8.5
    for y in range(size):
        for x in range(size):
            tx, ty = 2.75 * kx, 1.9 * ky
            if ellipse_in(x, y, tx, ty, 1.0 * kx, 1.0 * ky):
                px[x, y] = (70, 70, 74, 255) if not ellipse_in(x, y, tx, ty, 0.65 * kx,
                                                               0.65 * ky) else (170, 170, 176, 255)
            wx, wy = 2.75 * kx, 5.4 * ky
            if ellipse_in(x, y, wx, wy, 2.3 * kx, 2.3 * ky):
                if not ellipse_in(x, y, wx, wy, 2.0 * kx, 2.0 * ky):
                    c = (74, 74, 78)
                elif ellipse_in(x, y, wx, wy, 0.7 * kx, 0.7 * ky):
                    c = (34, 34, 36)
                else:
                    d = math.hypot((x + 0.5 - wx) / kx, (y + 0.5 - wy) / ky)
                    c = shade((52, 52, 56), 0.8 + 0.12 * d)
                px[x, y] = c + (255,)
    return img


def sub_front():
    img = solid((24, 24, 26), 926, size=32, grain=2)
    px = img.load()
    for y in range(32):
        for x in range(32):
            r = math.hypot(x + 0.5 - 16, y + 0.5 - 16)
            if r < 14.2:
                if r > 12.6:
                    c = (72, 72, 76)
                elif r < 4.8:
                    c = (36, 36, 38) if r < 4.0 else (60, 60, 64)
                else:
                    c = shade((50, 50, 54), 0.8 + 0.03 * r)
                px[x, y] = c + (255,)
    return img


# --- the piano ------------------------------------------------------------------------------
def piano_keys():
    """Fifteen white keys across 64 texels, front edge at the top: ivory, a dark line between
    each, and the back of the keys in the fallboard's shadow."""
    img = blank(64)
    px = img.load()
    seams = {round(k * 64 / 15) for k in range(1, 15)}
    for y in range(64):
        for x in range(64):
            c = (244, 240, 228) if y < 52 else (214, 208, 194)
            if x in seams:
                c = (120, 116, 108)
            px[x, y] = shade(c, 1.0 - 0.02 * ((x * 7 + y) % 3)) + (255,)
    return img


# --- clocks -----------------------------------------------------------------------------------
def clock_dial():
    """A round dial on a 32 px square, transparent outside the circle: a dark rim, twelve
    hour marks, heavier at the quarters."""
    img = blank(32)
    px = img.load()
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - 16, y + 0.5 - 16
            r = math.hypot(dx, dy)
            if r > 15.6:
                continue
            c = (246, 244, 238)
            if r > 14.6:
                c = (54, 52, 50)
            else:
                a = math.degrees(math.atan2(dx, -dy)) % 360
                k = round(a / 30) % 12
                off = abs((a - k * 30 + 180) % 360 - 180)
                heavy = k % 3 == 0
                if (11.2 if heavy else 12.2) < r < 14.0 and off * r * math.pi / 180 < (
                        0.95 if heavy else 0.55):
                    c = (34, 32, 30)
            px[x, y] = c + (255,)
    return img


# --- pictures -----------------------------------------------------------------------------
def photo(kind, size=32):
    rng = random.Random(930 + ["beach", "mountain", "portrait", "dog"].index(kind))
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            t = y / (size - 1.0)
            if kind == "beach":
                c = lerp((238, 150, 90), (250, 206, 150), t * 1.8) if y < 17 else (
                    lerp((50, 100, 150), (70, 130, 170), (y - 17) / 6.0) if y < 23 else
                    lerp((226, 200, 150), (200, 170, 120), (y - 23) / 9.0))
                if ellipse_in(x, y, 20, 14, 3.5, 3.5):
                    c = (255, 236, 180)
                if 17 <= y < 23 and abs(x - 20) < 2 - (y - 17) * 0.2 and (x + y) % 2:
                    c = (250, 210, 150)
                for sx, sh in ((9, 7), (12, 6)):
                    if sx <= x < sx + 2 and 22 - sh <= y < 23 or \
                            ellipse_in(x, y, sx + 1, 22 - sh - 1, 1.2, 1.2):
                        c = (40, 30, 34)
            elif kind == "mountain":
                c = lerp((110, 160, 220), (190, 215, 235), t * 2)
                ridge = 15 - 4 * math.sin(x * 0.3 + 0.5) - 2 * math.sin(x * 0.7)
                if y >= ridge:
                    c = (236, 240, 244) if y < ridge + 1.5 and ridge < 12 else (110, 124, 150)
                if y >= 20:
                    c = (64, 120, 64) if y < 25 else lerp((60, 110, 150), (40, 80, 120),
                                                          (y - 25) / 7.0)
            elif kind == "portrait":
                c = lerp((120, 140, 170), (70, 86, 112), math.hypot(x - 16, y - 12) / 22)
                if ellipse_in(x, y, 16, 12, 5.5, 6.8):
                    c = (228, 190, 160)
                    if y < 9 or (y < 13 and abs(x + 0.5 - 16) > 4.3):
                        c = (96, 62, 40)
                    if y == 12 and x in (14, 18):
                        c = (50, 40, 36)
                    if y == 15 and 15 <= x <= 17:
                        c = (170, 90, 90)
                if y >= 19 and abs(x + 0.5 - 16) < 5 + (y - 19) * 1.1:
                    c = (80, 110, 150) if abs(x + 0.5 - 16) > 2 or y > 24 else (228, 190, 160)
            else:  # dog
                c = lerp((140, 190, 235), (200, 225, 240), t * 2) if y < 17 else \
                    lerp((90, 160, 70), (70, 130, 56), (y - 17) / 15.0)
                if ellipse_in(x, y, 15, 21, 6, 5) or ellipse_in(x, y, 12, 13, 4, 4) or \
                        (8 <= y <= 12 and 8 <= x <= 9) or (x >= 20 and 16 <= y <= 18 and
                                                           x - 20 < 4 - (18 - y)):
                    c = (170, 110, 60) if (x + y) % 5 else (150, 94, 50)
                    if y == 12 and x == 11:
                        c = (30, 24, 20)
                    if 13 <= y <= 14 and 8 <= x <= 9:
                        c = (40, 30, 28)
            k = 1.0 + rng.uniform(-0.03, 0.03)
            px[x, y] = shade(c, k) + (255,)
    return img


PALETTE = [(196, 98, 64), (214, 168, 62), (48, 112, 118), (40, 52, 90), (226, 170, 150),
           (120, 150, 110)]


def art(kind, wide=False):
    """An artwork on a 64 px texture. A wide one (2:1) is drawn in rows 16 to 48, which is
    what its canvas shows; the rest is its ground colour."""
    rng = random.Random({"abstract": 940, "landscape": 941, "geometric": 942}[kind]
                        + (10 if wide else 0))
    w, h = 64, 32 if wide else 64
    canvas = Image.new("RGBA", (w, h))
    px = canvas.load()
    if kind == "abstract":
        blobs = [(rng.uniform(0, w), rng.uniform(0, h), rng.uniform(6, 16), rng.uniform(5, 13),
                  rng.choice(PALETTE)) for _ in range(9 if wide else 8)]
        strokes = [(rng.uniform(0, w), rng.uniform(0, h), rng.uniform(0, 6.28),
                    rng.uniform(12, 26)) for _ in range(3)]
        for y in range(h):
            for x in range(w):
                c = (236, 228, 210)
                for bx, by, rx, ry, col in blobs:
                    wob = 0.18 * math.sin(x * 0.5 + by) + 0.15 * math.sin(y * 0.6 + bx)
                    if ((x - bx) / rx) ** 2 + ((y - by) / ry) ** 2 < 1.0 + wob:
                        c = col
                for sx, sy, a, ln in strokes:
                    ux, uy = math.cos(a), math.sin(a)
                    s = (x - sx) * ux + (y - sy) * uy
                    d = -(x - sx) * uy + (y - sy) * ux + 3 * math.sin(s * 0.25)
                    if 0 < s < ln and abs(d) < 0.8:
                        c = (30, 28, 30)
                px[x, y] = shade(c, 1.0 + rng.uniform(-0.04, 0.04)) + (255,)
    elif kind == "landscape":
        horizon = h * 0.42
        tree_x = w * 0.22
        for y in range(h):
            for x in range(w):
                if y < horizon:
                    c = lerp((150, 186, 214), (236, 222, 190), y / horizon)
                    if ((x * 3 + y * 5) % 17) == 0:
                        c = shade(c, 1.07)
                else:
                    hill = horizon + 3 * math.sin(x * 0.11 + 1) + 2 * math.sin(x * 0.29)
                    if y < hill + 3:
                        c = (104, 140, 150)
                    elif y < h * 0.66:
                        c = (140, 168, 90) if (x // 5 + y // 3) % 3 else (160, 180, 96)
                    else:
                        c = (206, 176, 86) if (x + y * 2) % 7 else (188, 150, 70)
                        if abs(x - (w * 0.6 + (y - h) * 0.9)) < 2 + (y - h * 0.66) * 0.25:
                            c = (214, 196, 160)
                if ellipse_in(x, y, tree_x, horizon + 1, 6, 7):
                    c = (54, 92, 60) if (x + y) % 4 else (70, 110, 68)
                if abs(x + 0.5 - tree_x) < 1 and horizon + 6 <= y < horizon + 12:
                    c = (80, 60, 44)
                px[x, y] = shade(c, 1.0 + rng.uniform(-0.05, 0.05)) + (255,)
    else:  # geometric
        shapes = [("circle", w * 0.3, h * 0.38, min(w, h) * 0.22, PALETTE[1]),
                  ("half", w * 0.72, h * 0.62, min(w, h) * 0.26, PALETTE[0]),
                  ("tri", w * 0.55, h * 0.2, min(w, h) * 0.2, PALETTE[3]),
                  ("rect", w * 0.12, h * 0.7, min(w, h) * 0.16, PALETTE[2])]
        for y in range(h):
            for x in range(w):
                c = (238, 232, 218)
                for kind2, sx, sy, r, col in shapes:
                    if kind2 == "circle" and math.hypot(x + 0.5 - sx, y + 0.5 - sy) < r:
                        c = col
                    elif kind2 == "half" and math.hypot(x + 0.5 - sx, y + 0.5 - sy) < r \
                            and y + 0.5 < sy:
                        c = col
                    elif kind2 == "tri" and sy - r < y < sy + r and abs(x + 0.5 - sx) < (
                            y - (sy - r)) * 0.6:
                        c = col
                    elif kind2 == "rect" and abs(x + 0.5 - sx) < r * 1.4 and abs(
                            y + 0.5 - sy) < r * 0.7:
                        c = col
                if x in (int(w * 0.46), int(w * 0.47)) or y == int(h * 0.55):
                    c = (30, 28, 30)
                px[x, y] = shade(c, 1.0 + rng.uniform(-0.02, 0.02)) + (255,)
    if not wide:
        return canvas
    img = Image.new("RGBA", (64, 64), px[0, 0])
    img.paste(canvas, (0, 16))
    return img


# --- plants -------------------------------------------------------------------------------
def leaf_colour(rng, base=(44, 112, 58)):
    return shade(base, 1.0 + rng.uniform(-0.1, 0.1))


def monstera_leaf():
    rng = random.Random(950)
    img = blank(32)
    px = img.load()
    slits = [-2.2, -1.5, -0.8, 0.8, 1.5, 2.2]
    for y in range(32):
        for x in range(32):
            nx, ny = (x + 0.5 - 16) / 13.5, (y + 0.5 - 17.5) / 14.0
            rho = math.hypot(nx, ny)
            if rho > 1.0 or (y < 7 and abs(x + 0.5 - 16) < (7 - y) * 0.7):
                continue
            th = math.atan2(nx, -ny)
            if rho > 0.5 and any(abs(th - s) < 0.07 for s in slits):
                continue
            if 0.28 < rho < 0.42 and any(abs(th - (s + 0.35 * (1 if s > 0 else -1))) < 0.12
                                         for s in slits[1:5]):
                continue
            c = leaf_colour(rng)
            if abs(x + 0.5 - 16) < 0.8 or any(abs(th - s - 0.35) < 0.03 for s in slits):
                c = (78, 148, 84)
            px[x, y] = c + (255,)
    return img


def snake_leaves():
    rng = random.Random(951)
    img = blank(32)
    px = img.load()
    for cx, width, tip in ((6, 8.5, 4), (16, 9.5, 0), (26, 8.5, 7)):
        for y in range(tip, 32):
            half = width / 2 * min(1.0, (y - tip) / 9.0 + 0.1)
            for x in range(32):
                d = abs(x + 0.5 - cx - 0.6 * math.sin(y * 0.2))
                if d <= half:
                    band = (y + int(abs(x + 0.5 - cx) * 1.5)) % 5
                    c = (86, 126, 72) if band == 0 else (36, 78, 44)
                    if d > half - 0.9:
                        c = (206, 192, 86)
                    px[x, y] = shade(c, 1.0 + rng.uniform(-0.05, 0.05)) + (255,)
    return img


def fig_leaves():
    rng = random.Random(952)
    img = blank(32)
    px = img.load()
    leaves = [(rng.uniform(3, 29), rng.uniform(3, 29), rng.uniform(0, 6.28)) for _ in range(20)]
    for cx, cy, a in leaves:
        ca, sa = math.cos(a), math.sin(a)
        base = leaf_colour(rng, (38, 96, 44))
        for y in range(32):
            for x in range(32):
                u = (x + 0.5 - cx) * ca + (y + 0.5 - cy) * sa
                v = -(x + 0.5 - cx) * sa + (y + 0.5 - cy) * ca
                width = 2.6 * (1.0 - 0.25 * math.cos((u + 4.5) / 9.0 * 2 * math.pi))
                if abs(u) < 4.5 and abs(v) < width * math.sqrt(max(0.0, 1 - (u / 4.5) ** 2)):
                    c = base if abs(v) > 0.5 else (90, 150, 84)
                    px[x, y] = c + (255,)
    return img


def succulent_top():
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r, th = math.hypot(dx, dy), math.atan2(dy, dx)
            edge = 7.2 * (0.72 + 0.28 * abs(math.cos(4 * th)))
            if r < edge:
                c = lerp((150, 196, 170), (104, 150, 128), r / 7.2)
                if r > edge - 1.0:
                    c = (206, 132, 140)
                if abs(math.cos(4 * th)) > 0.97 and r > 2:
                    c = shade(c, 0.85)
                px[x, y] = c + (255,)
    return img


def succulent_side():
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if y >= 7 and ((x + 0.5 - 8) / 7.5) ** 2 + ((y + 0.5 - 16) / 9) ** 2 < 1:
                c = (128, 176, 150) if (x // 2) % 2 else (112, 160, 136)
                if y < 9:
                    c = (206, 132, 140)
                px[x, y] = c + (255,)
    return img


def cactus():
    img = solid((66, 128, 70), 953, grain=4)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x % 4 == 0:
                px[x, y] = (92, 156, 92, 255)
            if x % 4 == 0 and y % 3 == 1:
                px[x, y] = (236, 232, 200, 255)
    return img


def pothos(top):
    rng = random.Random(954 + (1 if top else 0))
    img = blank(32)
    px = img.load()
    leaves = []
    if top:
        for _ in range(26):
            leaves.append((rng.uniform(2, 30), rng.uniform(10, 30)))
    else:
        for vx in (5, 13, 20, 27):
            length = rng.randint(18, 30)
            for y in range(0, length, 3):
                lx = vx + 1.8 * math.sin(y * 0.35 + vx)
                leaves.append((lx + (1.3 if (y // 3) % 2 else -1.3), y + 1.5))
                for yy in range(y, min(32, y + 3)):
                    xx = int(vx + 1.8 * math.sin(yy * 0.35 + vx))
                    px[xx, yy] = (60, 104, 50, 255)
    for cx, cy in leaves:
        variegated = rng.random() < 0.35
        for y in range(32):
            for x in range(32):
                if ellipse_in(x, y, cx, cy, 1.9, 2.3) and not (y + 0.5 < cy - 1.2 and
                                                                abs(x + 0.5 - cx) < 0.5):
                    c = leaf_colour(rng, (54, 128, 60))
                    if variegated and (x + y) % 3 == 0:
                        c = (206, 210, 120)
                    px[x, y] = c + (255,)
    return img


# --- the fireplace ------------------------------------------------------------------------
def limestone():
    rng = random.Random(960)
    img = blank(32)
    px = img.load()
    for y in range(32):
        for x in range(32):
            c = shade((214, 204, 184), 1.0 + rng.uniform(-0.04, 0.04)
                      + 0.03 * math.sin(x * 0.4 + y * 0.3))
            if y % 16 == 15 or (x + (8 if y // 16 else 0)) % 32 == 31:
                c = (186, 176, 156)
            px[x, y] = c + (255,)
    return img


def firebrick():
    rng = random.Random(961)
    img = blank(32)
    px = img.load()
    shades = {}
    for y in range(32):
        for x in range(32):
            row = y // 4
            bx = (x + (4 if row % 2 else 0)) // 8
            if (row, bx) not in shades:
                shades[(row, bx)] = rng.uniform(0.85, 1.12)
            if y % 4 == 3 or (x + (4 if row % 2 else 0)) % 8 == 7:
                c = (64, 56, 52)
            else:
                c = shade((124, 64, 48), shades[(row, bx)])
            soot = 0.42 + 0.5 * (y / 31.0)
            px[x, y] = shade(c, soot) + (255,)
    return img


def log_end():
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            c = (190, 152, 104) if int(r) % 2 else (162, 124, 82)
            if r > 6.8:
                c = (84, 60, 40)
            px[x, y] = c + (255,)
    return img


def flame(size=16, frames=8, tongues=4, seed=962):
    """An animated fire: tongues of flame whose heights flicker, white-yellow at the root,
    orange, then red at the tips; transparent above them."""
    rng = random.Random(seed)
    phases = [(rng.uniform(0, 6.28), rng.uniform(0.8, 1.4)) for _ in range(tongues)]
    out = []
    for f in range(frames):
        img = blank(size)
        px = img.load()
        t = f / float(frames) * 2 * math.pi
        for x in range(size):
            h = 0.0
            for i, (p, sp) in enumerate(phases):
                cx = (i + 0.5) * size / tongues
                width = size / tongues * 0.75
                peak = size * (0.62 + 0.28 * math.sin(t * sp + p))
                h = max(h, peak * max(0.0, 1 - ((x + 0.5 - cx) / width) ** 2))
            h = max(h, size * 0.18)
            for y in range(size):
                up = size - y
                if up <= h:
                    q = up / max(h, 1e-6)
                    if q < 0.35:
                        c = lerp((255, 244, 190), (255, 196, 70), q / 0.35)
                    elif q < 0.75:
                        c = lerp((255, 196, 70), (240, 110, 30), (q - 0.35) / 0.4)
                    else:
                        c = lerp((240, 110, 30), (190, 50, 20), (q - 0.75) / 0.25)
                    px[x, y] = c + (255,)
        out.append(img)
    return strip(out)


def embers(on):
    rng = random.Random(963 + (1 if on else 0))
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if on:
                c = (40, 14, 8)
                if rng.random() < 0.35:
                    c = rng.choice([(250, 120, 30), (255, 180, 60), (200, 60, 20)])
            else:
                c = shade((92, 90, 86), 1.0 + rng.uniform(-0.15, 0.15))
            px[x, y] = c + (255,)
    return img


def candle_flame():
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            up = 15.5 - y
            w = 3.6 * math.sin(math.pi * min(1.0, up / 15.0)) ** 0.8 if up > 0 else 0
            if abs(x + 0.5 - 8) < w:
                q = up / 15.0
                c = (120, 150, 255) if q < 0.12 else lerp((255, 250, 220), (255, 160, 40), q)
                if abs(x + 0.5 - 8) > w - 1.1 and q > 0.12:
                    c = (250, 140, 40)
                px[x, y] = c + (255,)
    return img


def crate(base, seed, laminate=False):
    img = (R.laminate(base, seed, size=16) if laminate else R.wood(base, seed, size=16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if y % 5 == 4:
                px[x, y] = shade(px[x, y][:3], 0.42) + (255,)
    return img


def coir():
    rng = random.Random(964)
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = shade((184, 144, 92), 1.0 + rng.uniform(-0.18, 0.12))
            if (x + y) % 4 == 0 or (x - y) % 5 == 0:
                c = shade(c, 0.86)
            px[x, y] = c + (255,)
    return img


def ribbed(base, seed):
    img = solid(base, seed, grain=3)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if y % 2 == 0:
                px[x, y] = shade(px[x, y][:3], 1.25) + (255,)
    return img


def macrame():
    img = blank(16)
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = shade((232, 222, 198), 0.9 if (x + y) % 3 == 0 else 1.0) + (255,)
    return img


TEXTURES = {
    # (draw, frametime or None)
    "tv_bezel": (lambda: glossy((20, 20, 22), 901), None),
    "tv_back": (lambda: solid((40, 40, 44), 902), None),
    "tv_off": (tv_off, None),
    "tv_static": (tv_static, 1),
    "tv_bars": (tv_bars, None),
    "tv_nature": (tv_nature, 6),
    "tv_sports": (tv_sports, 3),
    "tv_news": (tv_news, 4),
    "crt_grey": (lambda: solid((150, 150, 146), 903, grain=2), None),
    "crt_dark": (lambda: solid((60, 60, 62), 904, grain=2), None),
    "crt_panel": (crt_panel, None),
    "grille": (grille, None),
    "hifi_black": (lambda: R.metal((34, 34, 38), 905), None),
    "hifi_front": (hifi_front, None),
    "display_off": (lambda: display(False), None),
    "display_on": (lambda: display(True), None),
    "platter_mat": (lambda: platter(False), None),
    "platter_record": (lambda: platter(True), None),
    "vinyl_black": (lambda: solid((28, 28, 30), 906, grain=2), None),
    "speaker_front": (speaker_front, None),
    "sub_front": (sub_front, None),
    "lacquer_black": (lambda: glossy((16, 16, 18), 907), None),
    "piano_keys": (piano_keys, None),
    "keys_black": (lambda: glossy((22, 22, 24), 908), None),
    "felt_red": (lambda: R.fabric((150, 30, 36), 909), None),
    "leather_black": (lambda: solid((34, 32, 32), 910, grain=3), None),
    "clock_black": (lambda: glossy((26, 26, 28), 914), None),
    "lcd": (lambda: solid((34, 10, 10), 915, grain=1), None),
    "clock_dial": (clock_dial, None),
    "photo_beach": (lambda: photo("beach"), None),
    "photo_mountain": (lambda: photo("mountain"), None),
    "photo_portrait": (lambda: photo("portrait"), None),
    "photo_dog": (lambda: photo("dog"), None),
    "photo_mat": (lambda: solid((236, 232, 222), 916, grain=2), None),
    "art_abstract": (lambda: art("abstract"), None),
    "art_landscape": (lambda: art("landscape"), None),
    "art_geometric": (lambda: art("geometric"), None),
    "art_abstract_wide": (lambda: art("abstract", True), None),
    "art_landscape_wide": (lambda: art("landscape", True), None),
    "art_geometric_wide": (lambda: art("geometric", True), None),
    "canvas_side": (lambda: solid((232, 226, 212), 917, grain=2), None),
    "terracotta": (lambda: solid((184, 98, 62), 918, grain=5), None),
    "concrete_grey": (lambda: solid((150, 150, 146), 919, grain=7), None),
    "soil": (lambda: solid((70, 50, 36), 920, grain=10), None),
    "monstera_leaf": (monstera_leaf, None),
    "snake_leaves": (snake_leaves, None),
    "fig_leaves": (fig_leaves, None),
    "succulent_top": (succulent_top, None),
    "succulent_side": (succulent_side, None),
    "cactus": (cactus, None),
    "pothos": (lambda: pothos(False), None),
    "pothos_top": (lambda: pothos(True), None),
    "trunk": (lambda: R.wood((110, 82, 58), 927, vertical=True, size=16), None),
    "stem": (lambda: solid((62, 112, 52), 928), None),
    "macrame": (macrame, None),
    "limestone": (limestone, None),
    "firebrick": (firebrick, None),
    "slate": (lambda: solid((70, 72, 76), 929, grain=8), None),
    "soot": (lambda: solid((30, 28, 26), 931, grain=3), None),
    "log_bark": (lambda: R.wood((92, 66, 46), 932, size=16, streak=0.15, line=0.7), None),
    "log_end": (log_end, None),
    "flame": (flame, 2),
    "embers_on": (lambda: embers(True), None),
    "embers_off": (lambda: embers(False), None),
    "clear": (lambda: blank(16), None),
    "bowl_off": (lambda: solid((214, 214, 208), 933, grain=2), None),
    "bowl_on": (lambda: solid((255, 246, 222), 934, grain=2), None),
    "linen_off": (lambda: solid((206, 196, 174), 935, grain=3), None),
    "linen_on": (lambda: solid((255, 242, 206), 936, grain=3), None),
    "wax_white": (lambda: solid((242, 236, 220), 937, grain=3), None),
    "candle_flame": (candle_flame, None),
    "coir": (coir, None),
    "coir_border": (lambda: solid((80, 56, 36), 938, grain=5), None),
    "rubber_mat": (lambda: ribbed((52, 52, 56), 939), None),
    "rubber_mat_border": (lambda: solid((34, 34, 36), 943, grain=2), None),
    "switch_white": (lambda: solid((238, 236, 230), 944, grain=2), None),
    "bell_button": (lambda: solid((236, 176, 70), 945, grain=4), None),
    "crate_oak": (lambda: crate(R.OAK, 946), None),
    "crate_walnut": (lambda: crate(R.WALNUT, 947), None),
    "crate_white": (lambda: crate(R.WHITE, 948, laminate=True), None),
}

DEFAULT_TEX = {
    "wood": RT("oak"), "wood_v": RT("oak_v"), "edge": RT("oak_edge"),
    "bezel": T("tv_bezel"), "back": T("tv_back"), "screen": T("tv_off"),
    "crt": T("crt_grey"), "crt_dark": T("crt_dark"), "crt_panel": T("crt_panel"),
    "grille": T("grille"), "chrome": RT("chrome"), "hifi": T("hifi_black"),
    "hifi_front": T("hifi_front"), "glow": T("display_off"), "platter": T("platter_mat"),
    "platter_side": RT("metal_steel"), "brushed": RT("metal_steel"), "plinth": RT("walnut"),
    "metal_black": RT("metal_black"), "cab": T("vinyl_black"), "speaker_front": T("speaker_front"),
    "sub_front": T("sub_front"), "lacquer": T("lacquer_black"), "keys": T("piano_keys"),
    "keys_black": T("keys_black"), "felt": T("felt_red"), "brass": RT("brass"),
    "leather": T("leather_black"), "clock_body": T("clock_black"), "lcd": T("lcd"),
    "dial": T("clock_dial"), "photo": T("photo_beach"), "photo_b": T("photo_mountain"),
    "photo_c": T("photo_portrait"), "photo_d": T("photo_dog"), "mat": T("photo_mat"),
    "art": T("art_abstract"), "canvas_side": T("canvas_side"), "pot": RT("ceramic_white"),
    "soil": T("soil"), "leaf": T("monstera_leaf"), "stem": T("stem"), "trunk": T("trunk"),
    "succulent_top": T("succulent_top"), "succulent_side": T("succulent_side"),
    "cactus": T("cactus"), "pothos": T("pothos"), "pothos_top": T("pothos_top"),
    "cord": T("macrame"), "stone": T("limestone"), "firebrick": T("firebrick"),
    "slate": T("slate"), "soot": T("soot"), "log": T("log_bark"), "log_end": T("log_end"),
    "flame": T("clear"), "embers": T("embers_off"), "iron": RT("metal_black"),
    "nickel": RT("metal_steel"), "bowl": T("bowl_off"), "metal": RT("brass"),
    "shade": T("linen_on"), "base": RT("ceramic_white"), "wax": T("wax_white"),
    "wick": RT("metal_black"), "candle_flame": T("candle_flame"), "tray": RT("metal_black"),
    "plate": T("switch_white"), "button": T("bell_button"), "crate": T("crate_oak"),
    "rug": T("coir"), "rug_border": T("coir_border"),
}


# ------------------------------------------------------------------------------------------
# Element helpers
# ------------------------------------------------------------------------------------------
# Each face's texture axes: (element axis, +1 if u (or v) grows with it, else -1).
SPAN_AXES = {"north": ((0, -1), (1, -1)), "south": ((0, 1), (1, -1)),
             "east": ((2, -1), (1, -1)), "west": ((2, 1), (1, -1)),
             "up": ((0, 1), (2, 1)), "down": ((0, 1), (2, -1))}


def span(spec, faces, region, rect=(0, 0, 16, 16)):
    """Maps the texture rect (u0, v0, u1, v1) across region (a0, a1, b0, b1) -- the face's u
    and v axes' extent in model coordinates, which may be wider than the element -- so a
    picture drawn across several elements, or cut across two blocks, stays one picture."""
    spec.setdefault("span", {})
    for f in faces:
        spec["span"][f] = (tuple(region), tuple(rect))
    fit_span(spec)
    return spec


def fit_span(spec):
    for f, (region, rect) in spec.get("span", {}).items():
        if f not in spec["faces"]:
            continue
        (ua, us), (va, vs) = SPAN_AXES[f]
        a0, a1, b0, b1 = region
        u0, v0, u1, v1 = rect

        def u_at(c):
            t = (c - a0) / (a1 - a0) if us > 0 else (a1 - c) / (a1 - a0)
            return u0 + t * (u1 - u0)

        def v_at(c):
            t = (c - b0) / (b1 - b0) if vs > 0 else (b1 - c) / (b1 - b0)
            return v0 + t * (v1 - v0)
        ustart = spec["from"][ua] if us > 0 else spec["to"][ua]
        uend = spec["to"][ua] if us > 0 else spec["from"][ua]
        vstart = spec["from"][va] if vs > 0 else spec["to"][va]
        vend = spec["to"][va] if vs > 0 else spec["from"][va]
        spec["uv"][f] = [u_at(ustart), v_at(vstart), u_at(uend), v_at(vend)]


def full(spec, faces):
    """The whole texture on each of faces."""
    return A.ALL_UV(spec, faces)


def move(specs, dx=0.0, dy=0.0, dz=0.0):
    """Moved, keeping every UV (a moved picture shows the same picture)."""
    out = []
    for s in specs:
        n = copy.deepcopy(s)
        n["from"] = [n["from"][0] + dx, n["from"][1] + dy, n["from"][2] + dz]
        n["to"] = [n["to"][0] + dx, n["to"][1] + dy, n["to"][2] + dz]
        if n["rot"]:
            axis, angle, origin = n["rot"]
            n["rot"] = (axis, angle, [origin[0] + dx, origin[1] + dy, origin[2] + dz])
        out.append(n)
    return out


def cut_wide(specs):
    """A piece drawn across two blocks (x 0 to 32) cut into its two blocks' models. Spanned
    faces keep their picture; others are fitted afresh in their own block, as the bedroom's
    cells are."""
    cells = []
    for c in (0, 1):
        part = B.clip(specs, 0, 16 * c, 16 * c + 16)
        for s in part:
            if s.get("span"):
                fit_span(s)
        cells.append(move(part, -16 * c))
    return cells


def octagon_z(cx, cy, r, z0, z1, tex, side=None, caps=("north", "south")):
    """An octagon facing north (its axis along z) of inradius r about (cx, cy): four
    rectangles, two turned 45 degrees about z, as octagon() is about y."""
    a = r * R.TAN_22_5
    out = []
    for k, (h1, h2, turned) in enumerate(((r, a, False), (a, r, False), (r, a, True),
                                          (a, r, True))):
        eps = 0.004 * k
        frm = [cx - h1, cy - h2, z0 - eps]
        to = [cx + h1, cy + h2, z1 + eps]
        sides = ["east", "west"] if h1 == r else ["up", "down"]
        per = {f: side for f in sides} if side else {}
        rot = ("z", 45, [cx, cy, z0]) if turned else None
        out.append(el(frm, to, tex, sides + list(caps), per, rot=rot))
    return out


def plane_z(x0, x1, y0, y1, z, tex, rot=None):
    """A zero-thickness sheet facing north and south, the whole texture on each side."""
    return full(el([x0, y0, z], [x1, y1, z], tex, ("north", "south"), rot=rot),
                ["north", "south"])


def plane_x(z0, z1, y0, y1, x, tex, rot=None):
    return full(el([x, y0, z0], [x, y1, z1], tex, ("east", "west"), rot=rot), ["east", "west"])


def plane_y(x0, x1, z0, z1, y, tex, rot=None):
    return full(el([x0, y, z0], [x1, y, z1], tex, ("up", "down"), rot=rot), ["up", "down"])


def pot(cx, cz, r, h, tex="pot", soil=True):
    """A round pot with a rolled rim, filled with soil to just under it."""
    out = octagon(cx, cz, r * 0.86, 0, h - 0.75, tex, caps=("down",))
    out += octagon(cx, cz, r, h - 0.75, h, tex, caps=())
    if soil:
        out += octagon(cx, cz, r * 0.86 - 0.05, h - 1.4, h - 0.5, "soil", caps=("up",))
    return out


# ------------------------------------------------------------------------------------------
# The pieces, each drawn facing north (front at -Z, back at +Z), in pixels
# ------------------------------------------------------------------------------------------
# --- TVs -------------------------------------------------------------------------------------
def tv_panel(x0, x1, y0, y1, z0, z1, rect=SCREEN_169):
    """A flat panel: its bezel, and the screen just proud of it across the whole picture."""
    screen = el([x0 + 0.25, y0 + 0.25, z0 - 0.1], [x1 - 0.25, y1 - 0.25, z0], "screen",
                ("north",))
    span(screen, ["north"], (x0 + 0.25, x1 - 0.25, y0 + 0.25, y1 - 0.25), rect)
    return [el([x0, y0, z0], [x1, y1, z1], "bezel"), screen]


# 43 in on its stand: 0.95 x 0.55 m.
TV_STAND_1 = (tv_panel(0.75, 15.25, 2.0, 10.75, 7.5, 8.25)
              + [el([2.5, 3, 8.25], [13.5, 9.5, 9.25], "back", NO_BACK[1:] + ("south",)),
                 el([7, 0.5, 8.0], [9, 2.0, 8.75], "bezel", SIDES),
                 el([4.5, 0, 5.75], [11.5, 0.5, 10.25], "bezel")])
TV_WALL_1 = (tv_panel(0.75, 15.25, 3.5, 12.25, 14.5, 15.25)
             + [el([5, 5, 15.25], [11, 11, 16], "back", SIDES[2:] + ("up", "down"))])
# 65 in, two blocks wide: 1.5 x 0.85 m.
TV_STAND_2 = (tv_panel(3.75, 28.25, 2.0, 15.5, 7.5, 8.25)
              + [el([8, 4, 8.25], [24, 13.5, 9.75], "back", ("east", "west", "up", "down",
                                                            "south"))]
              + [e for x in (7, 24) for e in (
                  el([x, 0.5, 8.0], [x + 1, 2.0, 9.0], "bezel", SIDES),
                  el([x - 0.5, 0, 6.0], [x + 1.5, 0.5, 10.5], "bezel"))])
TV_WALL_2 = (tv_panel(3.75, 28.25, 1.5, 15.0, 14.5, 15.25)
             + [el([10, 4, 15.25], [22, 12, 16], "back", ("east", "west", "up", "down"))])

# The big screens, several blocks wide and two high, drawn whole (x 0 to 16 * cols, y 0 to 32)
# and cut into a model per cell (cut_grid), the picture spanned across all of them. The picture
# is exactly 16:9 in a quarter-pixel bezel: 46 x 25.875 px across three blocks (the screen
# fills the width), 52 x 29.25 px across four (the height is what limits it: a 16:9 picture
# filling four blocks' width would be 35 px tall, more than two blocks). A stand TV stands on
# a centre pedestal whose foot is in column (cols - 1) // 2, the column BlockLargeTelevision
# reads the rest under; the panel is set back over a TV stand's top (z 9 to 16).
BIG_TV_ROWS = 2
BIG_RESTS = [("floor", 0.0), ("tv_stand", 8.0)]


def big_tv(cols, pic_w, wall):
    pic_h = pic_w * 9.0 / 16.0
    pw, ph = pic_w + 0.5, pic_h + 0.5
    x0 = (16 * cols - pw) / 2.0
    x1 = x0 + pw
    cx = 8.0 * cols
    if wall:
        y0 = (16 * BIG_TV_ROWS - ph) / 2.0
        y1 = y0 + ph
        return (tv_panel(x0, x1, y0, y1, 14.5, 15.25)
                + [el([x0 + 5, y0 + 4, 15.25], [x1 - 5, y1 - 4, 15.75], "back",
                      ("east", "west", "up", "down")),
                   el([cx - 6, y0 + 8, 15.75], [cx + 6, y1 - 8, 16], "back",
                      ("east", "west", "up", "down"))])
    y0 = 2.0
    y1 = y0 + ph
    foot = 16 * ((cols - 1) // 2) + 8 if cols % 2 else cx
    return (tv_panel(x0, x1, y0, y1, 10.25, 11.0)
            + [el([x0 + 6, y0 + 3, 11.0], [x1 - 6, y1 - 4, 12.25], "back",
                  ("east", "west", "up", "down", "south")),
               el([foot - 2.5, 0.5, 11.25], [foot + 2.5, y0 + 3, 12.25], "bezel", SIDES),
               el([foot - 8, 0, 9.0], [foot + 8, 0.5, 15.0], "bezel")])


def cut_grid(specs, cols, rows=BIG_TV_ROWS):
    """A piece drawn across cols x rows blocks cut into its cells' models, {(col, row): specs},
    each moved into its own block; spanned faces keep their share of the one picture."""
    cells = {}
    for c in range(cols):
        for r in range(rows):
            part = B.clip(B.clip(specs, 0, 16 * c, 16 * c + 16), 1, 16 * r, 16 * r + 16)
            for s in part:
                if s.get("span"):
                    fit_span(s)
            cells[(c, r)] = move(part, -16 * c, -16 * r)
    return cells


def scaled(specs, k):
    """The whole piece at k times its size about its footprint's middle, standing on y 0, its
    middle on the block's: a big TV's item, which would not fit the -16 to 32 an element may
    reach. Spanned pictures keep their UVs."""
    lo, hi = B.extent(specs)
    mx, mz = (lo[0] + hi[0]) / 2.0, (lo[2] + hi[2]) / 2.0
    out = []
    for s in specs:
        n = copy.deepcopy(s)
        if n.get("span"):
            fit_span(n)
            del n["span"]
        n["from"] = [8 + (n["from"][0] - mx) * k, n["from"][1] * k, 8 + (n["from"][2] - mz) * k]
        n["to"] = [8 + (n["to"][0] - mx) * k, n["to"][1] * k, 8 + (n["to"][2] - mz) * k]
        out.append(n)
    return out

# A 21 in tube TV, deep behind its screen, rabbit ears on top.
_CRT_SCREEN = el([2.5, 3.5, 2.6], [11.5, 10.5, 3.0], "screen", NO_BACK,
                 {f: "crt_dark" for f in ("east", "west", "up", "down")})
span(_CRT_SCREEN, ["north"], (2.5, 11.5, 3.5, 10.5), SCREEN_43)
CRT = ([el([1.5, 1, 3], [14.5, 12, 8], "crt"), _CRT_SCREEN,
        full(el([12, 3.5, 2.8], [14, 10.5, 3], "crt_panel", NO_BACK, {
            f: "crt_dark" for f in ("east", "west", "up", "down")}), ["north"]),
        full(el([2.5, 1.5, 2.8], [11.5, 3, 3], "grille", ("north",)), ["north"]),
        el([3, 2, 8], [13, 11, 12], "crt", ("east", "west", "up", "down", "south")),
        el([5, 3.5, 12], [11, 9.5, 13.5], "crt", ("east", "west", "up", "down", "south")),
        el([2, 0, 3.5], [3.5, 1, 7.5], "crt_dark", SIDES),
        el([12.5, 0, 3.5], [14, 1, 7.5], "crt_dark", SIDES),
        el([6.5, 12, 7], [9.5, 12.75, 10], "crt_dark", NO_DOWN),
        el([7.6, 12.75, 8.3], [8.0, 18.5, 8.7], "chrome", NO_DOWN,
           rot=("z", 22.5, [7.8, 12.75, 8.5])),
        el([8.0, 12.75, 8.3], [8.4, 18.5, 8.7], "chrome", NO_DOWN,
           rot=("z", -22.5, [8.2, 12.75, 8.5]))])

# --- audio -------------------------------------------------------------------------------------
STEREO = ([full(el([2, 0.5, 3.5], [14, 4.5, 12.5], "hifi", ALL, {"north": "hifi_front"}),
                ["north"]),
           full(el([4, 2.25, 3.4], [8.5, 3.5, 3.5], "glow", ("north",)), ["north"]),
           el([11.25, 1.5, 3.0], [12.75, 3.0, 3.5], "metal_black", NO_BACK)]
          + [el([x, 0, z], [x + 1, 0.5, z + 1], "metal_black", SIDES)
             for x in (2.5, 12.5) for z in (4, 11)]
          + [el([2.75, 4.5, 4.25], [13.25, 5.5, 11.75], "plinth")]
          + octagon(7.25, 8, 3.25, 5.5, 6.25, "platter", side="platter_side", caps=("up",))
          + [el([7.1, 6.25, 7.85], [7.4, 6.75, 8.15], "brushed", NO_DOWN),
             el([11, 5.5, 9.25], [12.25, 6.5, 10.5], "brushed", NO_DOWN),
             el([11.5, 6.5, 5.25], [11.8, 6.8, 10], "brushed"),
             el([11.1, 6.3, 4.75], [12.1, 6.7, 5.5], "metal_black"),
             el([3.5, 5.5, 10.5], [4.5, 5.7, 11.25], "brushed", NO_DOWN)])
SPEAKER = [full(el([5.25, 0, 5], [10.75, 8.5, 11], "cab", ALL, {"north": "speaker_front"}),
                ["north"])]
SUBWOOFER = ([full(el([3.5, 0.75, 3.5], [12.5, 9.75, 12.5], "cab", ALL,
                      {"north": "sub_front"}), ["north"])]
             + [el([x, 0, z], [x + 1, 0.75, z + 1], "metal_black", SIDES)
                for x in (4, 11) for z in (4, 11)])

# --- the upright piano, two blocks wide (x 4 to 28), 1 m high, its back on the wall -----------
PIANO_KEYS = {"x0": 5, "x1": 27, "front": 3.5, "back": 7.5, "top": 11.75}
_W = (PIANO_KEYS["x1"] - PIANO_KEYS["x0"]) / 15.0
_WHITE = el([5, 11.0, 3.5], [27, 11.75, 7.5], "keys", ("north", "up"))
span(_WHITE, ["up"], (5, 27, 3.5, 7.5))
span(_WHITE, ["north"], (5, 27, 11.0, 11.75), (0, 0, 16, 2))
PIANO = ([el([4, 9.5, 9.5], [28, 15.5, 15.75], "lacquer"),
          el([3.75, 15.5, 9.25], [28.25, 16, 16], "lacquer"),
          el([5, 0.5, 9.0], [27, 9.5, 15.75], "lacquer", ALL),
          el([4, 0, 8.5], [28, 0.5, 15.75], "lacquer", ALL),
          el([4, 0, 3.0], [5, 12.5, 9.5], "lacquer", NO_BACK),
          el([27, 0, 3.0], [28, 12.5, 9.5], "lacquer", NO_BACK),
          el([5, 9.5, 3.0], [27, 11.0, 9.0], "lacquer", ("north", "up", "down")),
          _WHITE,
          el([5, 11.0, 7.5], [27, 13.75, 8.25], "lacquer", ("north", "up")),
          el([5, 11.75, 7.35], [27, 12.0, 7.5], "felt", ("north", "up")),
          el([8, 13.75, 8.0], [24, 15.25, 9.5], "lacquer", NO_BACK),
          el([13.25, 0.5, 7.5], [14.25, 1.0, 9.0], "brass", NO_BACK),
          el([17.75, 0.5, 7.5], [18.75, 1.0, 9.0], "brass", NO_BACK)]
         + [el([27 - (i + 1) * _W - 0.45, 11.75, 5.5], [27 - (i + 1) * _W + 0.45, 12.5, 7.5],
               "keys_black", ("north", "east", "west", "up"))
            for i in range(14) if i % 7 in (0, 1, 3, 4, 5)])
PIANO_BENCH = ([el([2, 7.5, 5], [14, 8.5, 11], "lacquer"),
                el([2.5, 8.5, 5.5], [13.5, 9.25, 10.5], "leather", NO_DOWN),
                el([3.5, 6.5, 5.75], [12.5, 7.5, 6.25], "lacquer", ("north", "south", "down")),
                el([3.5, 6.5, 9.75], [12.5, 7.5, 10.25], "lacquer", ("north", "south", "down"))]
               + [el([x, 0, z], [x + 1, 7.5, z + 1], "lacquer", SIDES + ("down",))
                  for x in (2.5, 12.5) for z in (5.5, 9.5)])

# --- clocks ------------------------------------------------------------------------------------
DIGITAL_DISPLAY = {"x": 8.0, "y": 1.75, "face_z": 6.4}
DIGITAL_CLOCK = [el([4.5, 0, 6.5], [11.5, 3.5, 9.5], "clock_body"),
                 el([5.25, 0.75, 6.4], [10.75, 2.75, 6.5], "lcd", ("north",)),
                 el([6, 3.5, 7.25], [10, 3.8, 8.75], "clock_body", NO_DOWN)]
WALL_CLOCK_DIAL = {"x": 8.0, "y": 8.0, "face_z": 14.4}
WALL_CLOCK = (octagon_z(8, 8, 6.4, 14.5, 16, "wood", side="edge", caps=("north",))
              + [full(el([2, 2, 14.4], [14, 14, 14.5], "dial", ("north",)), ["north"]),
                 el([7.7, 7.7, 14.2], [8.3, 8.3, 14.4], "metal_black", NO_BACK)])

# --- pictures ----------------------------------------------------------------------------------
_LEAN = ("x", 22.5, [8, 0.5, 8.0])
PHOTO_FRAME = [el([4.5, 0.5, 7.5], [11.5, 6.0, 8.25], "wood", ALL, rot=_LEAN),
               full(el([5.25, 1.25, 7.4], [10.75, 5.25, 7.5], "photo", ("north",), rot=_LEAN),
                    ["north"]),
               el([7.5, 0, 8.6], [8.5, 0.5, 10.5], "wood", NO_DOWN)]


def framed(x0, y0, x1, y1, tex, border=0.6, mat=False):
    out = [el([x0, y0, 15.25], [x1, y1, 16], "wood", NO_BACK)]
    inset = border
    if mat:
        out.append(el([x0 + border, y0 + border, 15.15], [x1 - border, y1 - border, 15.25],
                      "mat", ("north",)))
        inset = border * 2.1
    out.append(full(el([x0 + inset, y0 + inset, 15.05 if mat else 15.15],
                       [x1 - inset, y1 - inset, 15.15 if mat else 15.25], tex, ("north",)),
                    ["north"]))
    return out


WALL_PHOTOS = (framed(2.5, 4, 9.5, 12.5, "photo_b", 0.7, mat=True)
               + framed(10.5, 8.5, 14.5, 12.5, "photo_c", 0.55)
               + framed(10.5, 3.5, 14.5, 7.5, "photo_d", 0.55))
WALL_ART = [full(el([1.5, 1.5, 15], [14.5, 14.5, 16], "canvas_side", NO_BACK,
                    {"north": "art"}), ["north"])]
_WIDE_CANVAS = el([2, 1.5, 15], [30, 15.5, 16], "canvas_side", NO_BACK, {"north": "art"})
span(_WIDE_CANVAS, ["north"], (2, 30, 1.5, 15.5), (0, 4, 16, 12))
WIDE_ART = [_WIDE_CANVAS]

# --- house plants ------------------------------------------------------------------------------
MONSTERA = (pot(8, 8, 3.4, 6)
            + [el([7.8, 5.5, 7.8], [8.2, 12, 8.2], "stem", SIDES,
                  rot=(ax, ang, [8, 5.5, 8]))
               for ax, ang in (("x", 22.5), ("x", -22.5), ("z", 22.5), ("z", -22.5))]
            + [plane_y(x0, x1, z0, z1, y, "leaf", rot=(ax, ang, [(x0 + x1) / 2, y,
                                                                (z0 + z1) / 2]))
               for x0, x1, z0, z1, y, ax, ang in (
                   (1.5, 9.5, 0.5, 8.5, 11.0, "x", 22.5),
                   (7.5, 15.5, 3.5, 11.5, 12.5, "z", -22.5),
                   (0.5, 8.5, 7.5, 15.5, 10.0, "z", 22.5),
                   (7.0, 15.0, 8.0, 16.0, 11.5, "x", -22.5),
                   (4.5, 11.5, 4.0, 11.0, 14.5, "x", 22.5))])
SNAKE_PLANT = ([el([5, 0, 5], [11, 5, 11], "pot", SIDES + ("down",)),
                el([5.05, 4.6, 5.05], [10.95, 4.8, 10.95], "soil", ("up",))]
               + [plane_z(4.5, 11.5, 4.6, 16.0, 8, "leaf", rot=("y", a, [8, 4.6, 8]))
                  for a in (22.5, -22.5)]
               + [plane_x(4.5, 11.5, 4.6, 14.5, 8, "leaf", rot=("y", a, [8, 4.6, 8]))
                  for a in (22.5, -22.5)])
FIDDLE_FIG = (pot(8, 8, 3.6, 6.5)
              + [el([7.7, 6, 7.7], [8.3, 17, 8.3], "trunk", SIDES)]
              + [plane_z(2, 14, 11, 26, 8, "leaf", rot=("y", a, [8, 11, 8])) for a in (45, -45)]
              + [plane_z(3, 13, 13, 25, 8, "leaf"), plane_x(3, 13, 13, 25, 8, "leaf")]
              + [plane_y(2.5, 13.5, 2.5, 13.5, 19, "leaf")])
SUCCULENTS = []
for _cx, _cz, _r, _h, _kind in ((5.5, 8.5, 1.8, 3, "rosette"), (9.5, 6.0, 1.5, 2.5, "rosette"),
                                (10.5, 10.5, 1.6, 3, "cactus")):
    SUCCULENTS += pot(_cx, _cz, _r, _h)
    if _kind == "rosette":
        SUCCULENTS.append(plane_y(_cx - 1.6, _cx + 1.6, _cz - 1.6, _cz + 1.6, _h + 0.2,
                                  "succulent_top"))
        SUCCULENTS.append(plane_z(_cx - 1.5, _cx + 1.5, _h - 0.6, _h + 1.2, _cz,
                                  "succulent_side"))
        SUCCULENTS.append(plane_x(_cz - 1.5, _cz + 1.5, _h - 0.6, _h + 1.2, _cx,
                                  "succulent_side"))
    else:
        SUCCULENTS += octagon(_cx, _cz, 0.8, _h - 0.6, _h + 2.6, "cactus", caps=("up",))
HANGING_PLANT = ([el([7.5, 15.5, 7.5], [8.5, 16, 8.5], "brass", ALL)]
                 + [el([7.9, 9.9, 7.9], [8.1, 15.5, 8.1], "cord", SIDES,
                       rot=(ax, ang, [8, 15.5, 8]))
                    for ax, ang in (("x", 22.5), ("x", -22.5), ("z", 22.5), ("z", -22.5))]
                 + move(pot(8, 8, 2.6, 3), 0, 7.5)
                 + [plane_z(4.5, 11.5, 1.0, 10.5, z, "pothos") for z in (5.2, 10.8)]
                 + [plane_x(4.5, 11.5, 1.5, 10.5, x, "pothos") for x in (5.2, 10.8)]
                 + [plane_z(4, 12, 9.5, 13.5, 8, "pothos_top", rot=("y", a, [8, 9.5, 8]))
                    for a in (45, -45)])

# --- fireplace, two blocks wide, its back on the wall -----------------------------------------
FIREBOX = {"x0": 12, "x1": 20, "z0": 11.5, "z1": 13.5, "y": 2.5}


def _flame(x0, x1, z, y1):
    f = el([x0, 1.4, z], [x1, y1, z], "flame", ("north", "south"))
    return span(f, ["north", "south"], (x0, x1, 1.4, y1))


FIREPLACE = ([el([1.5, 0, 3.5], [30.5, 1, 16], "slate", NO_BACK),
              el([3, 1, 9.5], [9, 14.5, 16], "stone", ALL, {"east": "soot"}),
              el([23, 1, 9.5], [29, 14.5, 16], "stone", ALL, {"west": "soot"}),
              el([9, 10.5, 9.5], [23, 14.5, 16], "stone", ("north", "down", "south"),
                 {"down": "soot"}),
              el([9, 1, 15], [23, 10.5, 16], "firebrick", ("north", "south"),
                 {"south": "stone"}),
              el([9, 1, 10], [9.25, 10.5, 15], "firebrick", ("east",)),
              el([22.75, 1, 10], [23, 10.5, 15], "firebrick", ("west",)),
              el([2, 14.5, 8.5], [30, 16, 16], "wood", ALL),
              board([3, 11.75, 9.2], [29, 14.5, 9.5], faces=("north", "down")),
              el([3, 1, 9.2], [5, 11.75, 9.5], "wood_v", ("north", "east", "west")),
              el([27, 1, 9.2], [29, 11.75, 9.5], "wood_v", ("north", "east", "west")),
              el([11.5, 1, 11.25], [20.5, 1.5, 11.75], "iron", NO_DOWN),
              el([11.5, 1, 13.25], [20.5, 1.5, 13.75], "iron", NO_DOWN),
              el([11.75, 1, 10.75], [12.25, 3, 11.25], "iron", NO_DOWN),
              el([19.75, 1, 10.75], [20.25, 3, 11.25], "iron", NO_DOWN),
              el([11, 1.5, 11.5], [21, 3, 13], "log", ALL, {"east": "log_end", "west": "log_end"}),
              el([12.5, 1.5, 13], [19.5, 3, 14.5], "log", NO_DOWN,
                 {"east": "log_end", "west": "log_end"}),
              el([13, 3, 12.25], [19, 4.25, 13.5], "log", NO_DOWN,
                 {"east": "log_end", "west": "log_end"}),
              el([10, 1.1, 10.5], [22, 1.15, 14.75], "embers", ("up",)),
              _flame(11.5, 20.5, 12.0, 9.0),   # clear of the top log's front at 12.25
              _flame(12.5, 19.5, 13.4, 8.0)])

# --- lighting ----------------------------------------------------------------------------------
FAN_BODY = (octagon(8, 8, 2, 15, 16, "nickel", caps=("down",))
            + [el([7.6, 12.5, 7.6], [8.4, 15, 8.4], "nickel", SIDES)]
            + octagon(8, 8, 2.75, 10.5, 12.5, "nickel")
            + octagon(8, 8, 2.0, 9.25, 10.5, "nickel", caps=("down",)))
FAN_BOWL = octagon(8, 8, 2.25, 7.75, 9.25, "bowl", caps=("down",))
_BLADE = [el([10.25, 10.8, 7.4], [11.75, 11.1, 8.6], "nickel"),
          board([11.5, 10.85, 6.6], [18.5, 11.05, 9.4])]
FAN_BLADES = [s for r in (0, 90, 180, 270) for s in R.turn(_BLADE, r)]
FLOOR_LAMP = (octagon(8, 8, 3, 0, 0.75, "metal")
              + [el([7.65, 0.75, 7.65], [8.35, 17.5, 8.35], "metal", SIDES),
                 el([8.35, 15.5, 7.8], [8.85, 16, 8.2], "metal", ("north", "south", "east",
                                                                  "up", "down"))]
              + octagon(8, 8, 4, 17.5, 24.5, "shade"))
TABLE_LAMP = (octagon(8, 8, 2.5, 0, 0.5, "metal")
              + octagon(8, 8, 2.1, 0.5, 5.0, "base")
              + [el([7.6, 5, 7.6], [8.4, 7, 8.4], "metal", SIDES)]
              + octagon(8, 8, 3.4, 6.5, 11.0, "shade"))


def candle(cx, cz, r, top, bottom=0.5):
    out = octagon(cx, cz, r, bottom, top, "wax", caps=("up",))
    out.append(el([cx - 0.1, top, cz - 0.1], [cx + 0.1, top + 0.5, cz + 0.1], "wick",
                  NO_DOWN))
    out.append(plane_z(cx - 0.8, cx + 0.8, top + 0.2, top + 2.4, cz, "candle_flame"))
    out.append(plane_x(cz - 0.8, cz + 0.8, top + 0.2, top + 2.4, cx, "candle_flame"))
    return out


PILLARS = [(6.25, 8.75, 1.35, 7.0), (9.75, 9.25, 1.2, 5.0), (8.25, 6.25, 1.0, 3.5)]
PILLAR_CANDLES = (octagon(8, 8, 4, 0, 0.5, "tray")
                  + [s for c in PILLARS for s in candle(*c)])
CANDLESTICK = (octagon(8, 8, 2, 0, 0.5, "brass") + octagon(8, 8, 0.6, 0.5, 4.5, "brass")
               + octagon(8, 8, 1.4, 4.5, 5.0, "brass") + candle(8, 8, 0.6, 12, bottom=5.0))

# --- around the house ----------------------------------------------------------------------------
_ROCKER = [el([7.25, 7.0, 15.05], [8.75, 9.0, 15.5], "plate", NO_BACK)]
SWITCH_PLATE = [el([6.25, 5.5, 15.5], [9.75, 10.5, 16], "plate", NO_BACK)]
SWITCH_ON = SWITCH_PLATE + [dict(s, rot=("x", 22.5, [8, 8, 15.5])) for s in _ROCKER]
SWITCH_OFF = SWITCH_PLATE + [dict(s, rot=("x", -22.5, [8, 8, 15.5])) for s in _ROCKER]
_BELL = [el([7, 6, 15.5], [9, 10, 16], "plate", NO_BACK),
         el([7.4, 7.9, 15.35], [8.6, 9.1, 15.5], "brass", NO_BACK)]
DOORBELL_UP = _BELL + [el([7.65, 8.15, 15.05], [8.35, 8.85, 15.35], "button", NO_BACK)]
DOORBELL_DOWN = _BELL + [el([7.65, 8.15, 15.25], [8.35, 8.85, 15.35], "button", NO_BACK)]
CRATE = ([el([1, 0.5, 1], [15, 13.5, 15], "crate")]
         + [el([x, 0, z], [x + 1.5, 14, z + 1.5], "wood_v", SIDES + ("up", "down"))
            for x in (0.75, 13.75) for z in (0.75, 13.75)]
         + [board([2.25, y, 0.75], [13.75, y + 1, 2.25]) for y in (0, 13)]
         + [board([2.25, y, 13.75], [13.75, y + 1, 15.25]) for y in (0, 13)]
         + [board([0.75, y, 2.25], [2.25, y + 1, 13.75]) for y in (0, 13)]
         + [board([13.75, y, 2.25], [15.25, y + 1, 13.75]) for y in (0, 13)])


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
def fin(fid, tex, *names):
    return (fid, dict(tex)) + names


BLACK = fin("black", {"bezel": T("tv_bezel")}, "Black", "Schwarz", "negro", "svart")
WOODS = [(f, dict(t), *n) for f, t, *n in R.WOODS]
PIECE_WOOD = "Material.WOOD, SoundType.WOOD, BlockRenderLayer.SOLID"
PIECE_METAL = "Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID"
PIECE_PLANT = "Material.PLANTS, SoundType.PLANT, BlockRenderLayer.CUTOUT"


def jbox(box):
    return ", ".join(R._num(v) for v in box)


def box_of(specs, wide=False):
    """The Java box of a piece: its elements' extent in whole pixels, inside the block (or both
    blocks, x 0 to 32, for a two-block piece)."""
    built = [R.build(s) for s in specs]
    lo = [min(e["from"][i] for e in built) for i in range(3)]
    hi = [max(e["to"][i] for e in built) for i in range(3)]
    lo = [max(0, int(math.floor(v))) for v in lo]
    hi = [min(32 if (wide and i == 0) else 16, int(math.ceil(v))) for i, v in enumerate(hi)]
    return lo + hi


# (piece, kind, finishes, names, spec). Kinds: tv, counter, single, wide, fan, lamp, switch,
# doorbell, mat, crate. The spec's java is a format of (registry, box); the box is the elements'
# extent unless the spec gives one (a plant's leaves, a tube TV's aerial are not its box).
PIECES = [
    ("flat_screen_tv", "tv", [BLACK],
     ("Flat-Screen TV", "Flachbildfernseher", "Televisor de pantalla plana", "Platt-TV"),
     {"geo": TV_STAND_1, "wide": False, "wall": False}),
    ("wall_tv", "tv", [BLACK],
     ("Wall-Mounted TV", "Wandfernseher", "Televisor de pared", "Väggmonterad TV"),
     {"geo": TV_WALL_1, "wide": False, "wall": True}),
    ("large_flat_screen_tv", "tv", [BLACK],
     ("Large Flat-Screen TV", "Großer Flachbildfernseher", "Televisor de pantalla plana grande",
      "Stor platt-TV"),
     {"geo": TV_STAND_2, "wide": True, "wall": False}),
    ("large_wall_tv", "tv", [BLACK],
     ("Large Wall-Mounted TV", "Großer Wandfernseher", "Televisor de pared grande",
      "Stor väggmonterad TV"),
     {"geo": TV_WALL_2, "wide": True, "wall": True}),
    ("xl_flat_screen_tv", "bigtv", [BLACK],
     ("Extra-Large Flat-Screen TV", "Extragroßer Flachbildfernseher",
      "Televisor de pantalla plana extragrande", "Extra stor platt-TV"),
     {"geo": big_tv(3, 46.0, False), "cols": 3, "wall": False}),
    ("xl_wall_tv", "bigtv", [BLACK],
     ("Extra-Large Wall-Mounted TV", "Extragroßer Wandfernseher",
      "Televisor de pared extragrande", "Extra stor väggmonterad TV"),
     {"geo": big_tv(3, 46.0, True), "cols": 3, "wall": True}),
    ("giant_flat_screen_tv", "bigtv", [BLACK],
     ("Giant Flat-Screen TV", "Riesiger Flachbildfernseher",
      "Televisor de pantalla plana gigante", "Jättestor platt-TV"),
     {"geo": big_tv(4, 52.0, False), "cols": 4, "wall": False}),
    ("giant_wall_tv", "bigtv", [BLACK],
     ("Giant Wall-Mounted TV", "Riesiger Wandfernseher", "Televisor de pared gigante",
      "Jättestor väggmonterad TV"),
     {"geo": big_tv(4, 52.0, True), "cols": 4, "wall": True}),
    ("crt_tv", "tv", [fin("grey", {"crt": T("crt_grey")}, "Grey", "Grau", "gris", "grå")],
     ("CRT TV", "Röhrenfernseher", "Televisor de tubo", "Tjock-TV"),
     {"geo": CRT, "wide": False, "wall": False, "particle": "crt", "box": [1, 0, 2, 15, 13, 14]}),
    ("stereo", "counter", [fin("black", {"hifi": T("hifi_black")}, "Black", "Schwarz", "negro",
                                "svart")],
     ("Hi-Fi Stereo", "Hi-Fi-Stereoanlage", "Equipo de alta fidelidad", "Hi-fi-stereo"),
     {"geo": STEREO, "particle": "hifi", "record": True,
      "java": 'new BlockStereo("%s", new int[]{%s})'}),
    ("bookshelf_speaker", "counter",
     [fin("black", {"cab": T("vinyl_black")}, "Black", "Schwarz", "negro", "svart"),
      fin("walnut", {"cab": RT("walnut")}, "Walnut", "Nussbaum", "nogal", "valnöt")],
     ("Bookshelf Speaker", "Regallautsprecher", "Altavoz de estantería", "Bokhyllehögtalare"),
     {"geo": SPEAKER, "particle": "cab",
      "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s)' % PIECE_WOOD}),
    ("subwoofer", "counter", [fin("black", {"cab": T("vinyl_black")}, "Black", "Schwarz",
                                   "negro", "svart")],
     ("Subwoofer", "Subwoofer", "Subwoofer", "Subwoofer"),
     {"geo": SUBWOOFER, "particle": "cab",
      "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s)' % PIECE_WOOD}),
    # ---- music ----
    ("upright_piano", "wide",
     [fin("black", {"lacquer": T("lacquer_black")}, "Black", "Schwarz", "negro", "svart"),
      fin("walnut", {"lacquer": RT("walnut")}, "Walnut", "Nussbaum", "nogal", "valnöt")],
     ("Upright Piano", "Klavier", "Piano vertical", "Piano"),
     {"geo": PIANO, "particle": "lacquer",
      "java": 'new BlockUprightPiano("%s", new int[]{%s})'}),
    ("piano_bench", "single",
     [fin("black", {"lacquer": T("lacquer_black")}, "Black", "Schwarz", "negro", "svart"),
      fin("walnut", {"lacquer": RT("walnut")}, "Walnut", "Nussbaum", "nogal", "valnöt")],
     ("Piano Bench", "Klavierbank", "Banqueta de piano", "Pianopall"),
     {"geo": PIANO_BENCH, "particle": "lacquer",
      "java": 'new BlockPianoBench("%s", new int[]{%s}, 9.25)'}),
    # ---- clocks, pictures and plants ----
    ("digital_clock", "counter", [fin("black", {"clock_body": T("clock_black")}, "Black",
                                       "Schwarz", "negro", "svart")],
     ("Digital Alarm Clock", "Digitaler Wecker", "Despertador digital",
      "Digital väckarklocka"),
     {"geo": DIGITAL_CLOCK, "particle": "clock_body",
      "java": 'new BlockDigitalClock("%s", new int[]{%s})'}),
    ("wall_clock", "single", WOODS,
     ("Wall Clock", "Wanduhr", "Reloj de pared", "Väggklocka"),
     {"geo": WALL_CLOCK, "particle": "wood", "java": 'new BlockWallClock("%s", new int[]{%s})'}),
    ("photo_frame", "counter", WOODS,
     ("Photo Frame", "Bilderrahmen", "Portarretratos", "Fotoram"),
     {"geo": PHOTO_FRAME, "particle": "wood",
      "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s)' % PIECE_WOOD}),
    ("wall_photo_frames", "single", WOODS,
     ("Wall Photo Frames", "Wand-Bilderrahmen", "Marcos de fotos de pared",
      "Fotoramar för vägg"),
     {"geo": WALL_PHOTOS, "particle": "wood",
      "java": 'new BlockLivingDecor("%s", new int[]{%s}, Material.WOOD, SoundType.WOOD, 0.8F, '
              'BlockRenderLayer.SOLID)'}),
    ("wall_art", "single",
     [fin(k, {"art": T("art_" + k)}, *n) for k, n in (
         ("abstract", ("Abstract", "Abstrakt", "abstracto", "abstrakt")),
         ("landscape", ("Landscape", "Landschaft", "paisaje", "landskap")),
         ("geometric", ("Geometric", "Geometrisch", "geométrico", "geometrisk")))],
     ("Wall Art", "Wandbild", "Cuadro", "Tavla"),
     {"geo": WALL_ART, "particle": "canvas_side",
      "java": 'new BlockLivingDecor("%s", new int[]{%s}, Material.WOOD, SoundType.CLOTH, 0.8F, '
              'BlockRenderLayer.SOLID)'}),
    ("wide_wall_art", "wide",
     [fin(k, {"art": T("art_%s_wide" % k)}, *n) for k, n in (
         ("abstract", ("Abstract", "Abstrakt", "abstracto", "abstrakt")),
         ("landscape", ("Landscape", "Landschaft", "paisaje", "landskap")),
         ("geometric", ("Geometric", "Geometrisch", "geométrico", "geometrisk")))],
     ("Wide Wall Art", "Breites Wandbild", "Cuadro ancho", "Bred tavla"),
     {"geo": WIDE_ART, "particle": "canvas_side",
      "java": 'new BlockResidentialWide("%s", new int[]{%s})'}),
    ("monstera_plant", "counter",
     [fin("white", {"pot": RT("ceramic_white"), "leaf": T("monstera_leaf")}, "White Pot",
          "weißer Topf", "maceta blanca", "vit kruka")],
     ("Monstera", "Monstera", "Monstera", "Monstera"),
     {"geo": MONSTERA, "particle": "leaf", "tall": True, "box": [3, 0, 3, 13, 14, 13],
      "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s)' % PIECE_PLANT}),
    ("snake_plant", "counter",
     [fin("grey", {"pot": T("concrete_grey"), "leaf": T("snake_leaves")}, "Grey Pot",
          "grauer Topf", "maceta gris", "grå kruka")],
     ("Snake Plant", "Bogenhanf", "Sansevieria", "Svärmorstunga"),
     {"geo": SNAKE_PLANT, "particle": "leaf", "box": [5, 0, 5, 11, 15, 11],
      "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s)' % PIECE_PLANT}),
    ("fiddle_leaf_fig", "counter",
     [fin("terracotta", {"pot": T("terracotta"), "leaf": T("fig_leaves")}, "Terracotta Pot",
          "Terrakottatopf", "maceta de terracota", "terrakottakruka")],
     ("Fiddle-Leaf Fig", "Geigenfeige", "Ficus lira", "Fiolfikus"),
     {"geo": FIDDLE_FIG, "particle": "leaf", "tall": True, "box": [4, 0, 4, 12, 16, 12],
      "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s)' % PIECE_PLANT}),
    ("succulent_pots", "counter",
     [fin("terracotta", {"pot": T("terracotta")}, "Terracotta", "Terrakotta", "terracota",
          "terrakotta")],
     ("Succulent Pots", "Sukkulententöpfe", "Macetas de suculentas", "Suckulentkrukor"),
     {"geo": SUCCULENTS, "particle": "pot",
      "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s)' % PIECE_PLANT}),
    ("hanging_plant", "single",
     [fin("white", {"pot": RT("ceramic_white")}, "White Pot", "weißer Topf", "maceta blanca",
          "vit kruka")],
     ("Hanging Plant", "Hängepflanze", "Planta colgante", "Hängväxt"),
     {"geo": HANGING_PLANT, "particle": "pothos_top",
      "java": 'new BlockLivingDecor("%s", new int[]{%s}, Material.PLANTS, SoundType.PLANT, '
              '0.3F, BlockRenderLayer.CUTOUT)'}),
    # ---- fireplace and lighting ----
    ("fireplace", "wide", WOODS,
     ("Fireplace", "Kamin", "Chimenea", "Öppen spis"),
     {"geo": FIREPLACE, "particle": "stone", "lit": True,
      "java": 'new BlockFireplace("%s", new int[]{%s})'}),
    ("ceiling_fan", "fan", WOODS,
     ("Ceiling Fan", "Deckenventilator", "Ventilador de techo", "Takfläkt"),
     {"java": 'new BlockCeilingFan("%s", new int[]{%s})'}),
    ("floor_lamp", "lamp",
     [fin("brass", {"metal": RT("brass")}, "Brass", "Messing", "latón", "mässing"),
      fin("black", {"metal": RT("metal_black")}, "Black", "Schwarz", "negro", "svart")],
     ("Floor Lamp", "Stehlampe", "Lámpara de pie", "Golvlampa"),
     {"geo": FLOOR_LAMP, "particle": "metal",
      "java": 'new BlockKitchenLight("%s", new int[]{%s}, 14)'}),
    ("table_lamp", "counter",
     [fin("brass", {"metal": RT("brass"), "base": RT("brass")}, "Brass", "Messing", "latón",
          "mässing"),
      fin("white", {"metal": RT("brass"), "base": RT("ceramic_white")}, "White", "Weiß",
          "blanco", "vit")],
     ("Table Lamp", "Tischlampe", "Lámpara de mesa", "Bordslampa"),
     {"geo": TABLE_LAMP, "particle": "base", "glow": ("shade", T("linen_off"), T("linen_on")),
      "java": 'new BlockCounterLight("%s", new int[]{%s}, Material.WOOD, SoundType.GLASS, '
              'BlockRenderLayer.SOLID, 13)'}),
    ("pillar_candles", "counter",
     [fin("white", {"wax": T("wax_white")}, "White", "Weiß", "blancas", "vita")],
     ("Pillar Candles", "Stumpenkerzen", "Velas de columna", "Blockljus"),
     {"geo": PILLAR_CANDLES, "particle": "wax",
      "glow": ("candle_flame", T("clear"), T("candle_flame")),
      "wicks": [(c[0], c[3] + 2.0, c[1]) for c in PILLARS],
      "java": 'new BlockCandle("%s", new int[]{%s}, 9, new double[][]{%s})'}),
    ("candlestick", "counter",
     [fin("brass", {"brass": RT("brass")}, "Brass", "Messing", "latón", "mässing")],
     ("Candlestick", "Kerzenständer", "Candelabro", "Ljusstake"),
     {"geo": CANDLESTICK, "particle": "brass",
      "glow": ("candle_flame", T("clear"), T("candle_flame")),
      "wicks": [(8, 14.0, 8)],
      "java": 'new BlockCandle("%s", new int[]{%s}, 7, new double[][]{%s})'}),
    # ---- around the house ----
    ("door_mat", "mat",
     [fin("coir", {"rug": T("coir"), "rug_border": T("coir_border")}, "Coir", "Kokos", "coco",
          "kokos"),
      fin("grey", {"rug": T("rubber_mat"), "rug_border": T("rubber_mat_border")}, "Grey",
          "Grau", "gris", "grå")],
     ("Door Mat", "Fußmatte", "Felpudo", "Dörrmatta"),
     {"java": 'new BlockRug("%s")'}),
    ("light_switch", "switch",
     [fin("white", {"plate": T("switch_white")}, "White", "Weiß", "blanco", "vit")],
     ("Light Switch", "Lichtschalter", "Interruptor de luz", "Strömbrytare"),
     {"on": SWITCH_ON, "off": SWITCH_OFF, "java": 'new BlockLightSwitch("%s", new int[]{%s})'}),
    ("doorbell", "switch",
     [fin("white", {"plate": T("switch_white")}, "White", "Weiß", "blanco", "vit")],
     ("Doorbell", "Türklingel", "Timbre", "Dörrklocka"),
     {"on": DOORBELL_DOWN, "off": DOORBELL_UP,
      "java": 'new BlockDoorbell("%s", new int[]{%s})'}),
    ("storage_crate", "crate",
     [(f, dict(t, crate=T("crate_" + f)), *n) for f, t, *n in R.WOODS],
     ("Storage Crate", "Aufbewahrungskiste", "Caja de almacenaje", "Förvaringslåda"),
     {"geo": CRATE, "particle": "crate",
      "java": 'new BlockResidentialStorage("%s", new int[]{%s}, 27, '
              'FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE)'}),
]
PIECE = {p: (kind, fins, names, spec) for p, kind, fins, names, spec in PIECES}
GROUPS = {"flat_screen_tv": "Living room: TV and audio", "upright_piano": "Music",
          "digital_clock": "Clocks, pictures and plants", "fireplace": "Fireplace and lighting",
          "door_mat": "Around the house"}
FAN_ALL = FAN_BODY + FAN_BOWL + FAN_BLADES


def names_with(names, fnames):
    return ["%s (%s)" % (n, f) for n, f in zip(names, fnames)]


def java_for(piece, kind, spec, reg):
    j = spec.get("java")
    if kind == "mat":
        return j % reg
    if kind == "tv":
        box = spec.get("box") or box_of(spec["geo"], spec["wide"])
        return 'new BlockTelevision("%s", new int[]{%s}, %s, %s)' % (
            reg, jbox(box), "true" if spec["wide"] else "false",
            "true" if spec["wall"] else "false")
    if kind == "bigtv":
        built = [R.build(s) for s in spec["geo"]]
        lo = [max(0, int(math.floor(min(e["from"][i] for e in built)))) for i in range(3)]
        hi = [int(math.ceil(max(e["to"][i] for e in built))) for i in range(3)]
        return 'new BlockLargeTelevision("%s", new int[]{%s}, %d, %d, %s)' % (
            reg, jbox(lo + hi), spec["cols"], BIG_TV_ROWS, "true" if spec["wall"] else "false")
    if kind == "fan":
        return j % (reg, jbox(box_of(FAN_BODY + FAN_BOWL)))
    if kind == "switch":
        return j % (reg, jbox(box_of(spec["on"])))
    box = spec.get("box") or box_of(spec["geo"], kind == "wide")
    if "wicks" in spec:
        wicks = ", ".join("{%s}" % jbox(w) for w in spec["wicks"])
        return j % (reg, jbox(box), wicks)
    return j % (reg, jbox(box))


def entries():
    """Every block: (registry, piece, kind, finish textures, names, java)."""
    out = []
    for piece, kind, fins, names, spec in PIECES:
        for fid, ftex, *fnames in fins:
            reg = "%s_%s" % (piece, fid)
            out.append((reg, piece, kind, dict(ftex), names_with(names, fnames),
                        java_for(piece, kind, spec, reg)))
    return out


# ------------------------------------------------------------------------------------------
# Models
# ------------------------------------------------------------------------------------------
def geometry(specs, particle, dy=0.0, display=None, centre=False):
    """A base model: the elements built where they stand (so their UVs fit there), then moved
    down by dy -- or, for an item, moved so the piece's middle is the block's."""
    built = [R.build(s) for s in specs]
    if centre:
        lo = [min(e["from"][i] for e in built) for i in range(3)]
        hi = [max(e["to"][i] for e in built) for i in range(3)]
        shift = [8 - (lo[i] + hi[i]) / 2.0 for i in range(3)]
    else:
        shift = [0.0, -dy, 0.0]
    if any(shift):
        for e in built:
            for key in ("from", "to"):
                e[key] = [round(e[key][i] + shift[i], 3) for i in range(3)]
            if "rotation" in e:
                o = e["rotation"]["origin"]
                e["rotation"]["origin"] = [round(o[i] + shift[i], 3) for i in range(3)]
    tex = {k: DEFAULT_TEX[k] for k in sorted(R.used_keys(specs) | {particle})}
    tex["particle"] = "#" + particle
    out = {"parent": "block/block", "textures": tex, "elements": built}
    if display:
        out["display"] = display
    return out


def item_display(specs, tall=False):
    if tall:
        return B.big_display(B.centred(specs))
    return A.small_display(specs)


def base_models():
    """Every base geometry model: (name, geometry json)."""
    out = []
    for piece, kind, _fins, _names, spec in PIECES:
        geo = spec.get("geo")
        particle = spec.get("particle", "bezel")
        if kind == "tv":
            cells = cut_wide(geo) if spec["wide"] else [geo]
            rests = [("floor", 0.0)] if spec["wall"] else A.RESTS
            for i, cell in enumerate(cells):
                for rest, drop in rests:
                    name = piece
                    if spec["wide"]:
                        name += "_cell%d" % i
                    if not spec["wall"]:
                        name += "_" + rest
                    out.append((name, geometry(cell, particle, drop)))
            item = move(geo, -8) if spec["wide"] else geo
            disp = B.big_display(item) if spec["wide"] else None
            out.append(("%s_item" % piece, geometry(item, particle, display=disp,
                                                    centre=not spec["wide"])))
        elif kind == "bigtv":
            rests = [("", 0.0)] if spec["wall"] else BIG_RESTS
            for (c, r), cell in sorted(cut_grid(geo, spec["cols"]).items()):
                for rest, drop in rests:
                    name = "%s_c%dr%d" % (piece, c, r) + ("_" + rest if rest else "")
                    out.append((name, geometry(cell, particle, drop)))
            item = scaled(geo, 0.5)
            out.append(("%s_item" % piece, geometry(item, particle,
                                                    display=B.big_display(item))))
        elif kind == "counter":
            for rest, drop in A.RESTS:
                out.append(("%s_%s" % (piece, rest), geometry(geo, particle, drop)))
            out.append(("%s_item" % piece, geometry(geo, particle, display=item_display(
                geo, spec.get("tall")), centre=not spec.get("tall"))))
        elif kind in ("single", "lamp"):
            tall = max(R.build(s)["to"][1] for s in geo) > 17
            out.append((piece, geometry(geo, particle,
                                        display=B.big_display(B.centred(geo)) if tall else None)))
        elif kind == "wide":
            for i, cell in enumerate(cut_wide(geo)):
                out.append(("%s_cell%d" % (piece, i), geometry(cell, particle)))
            item = move(geo, -8)
            out.append(("%s_item" % piece, geometry(item, particle,
                                                    display=B.big_display(item))))
        elif kind == "fan":
            out.append(("ceiling_fan_body", geometry(FAN_BODY, "nickel")))
            out.append(("ceiling_fan_blades", geometry(FAN_BLADES, "wood")))
            out.append(("ceiling_fan_bowl", geometry(FAN_BOWL, "bowl")))
            out.append(("ceiling_fan_item", geometry(FAN_ALL, "nickel",
                                                     display=B.big_display(FAN_ALL))))
        elif kind == "switch":
            out.append(("%s_on" % piece, geometry(spec["on"], "plate")))
            out.append(("%s_off" % piece, geometry(spec["off"], "plate")))
        elif kind == "crate":
            out.append(("%s_body" % piece, geometry(geo, particle)))
            out.append(("%s_item" % piece, geometry(geo, particle)))
    return out


# ------------------------------------------------------------------------------------------
# Blockstates
# ------------------------------------------------------------------------------------------
CHANNELS = ["off", "news", "sports", "nature", "bars", "static"]


def facing_variants():
    return {f: ({"y": r} if r else {}) for f, r in ROT.items()}


def switch_tex(key, off, on):
    return {"true": {"textures": {key: on}}, "false": {"textures": {key: off}}}


def empty(values):
    return {v: {} for v in values}


def forge(model, ftex, variants, item_model=None, item_tex=None):
    inv = {}
    if item_model:
        inv["model"] = item_model
    if item_tex:
        inv["textures"] = dict(item_tex)
    variants = dict(variants)
    variants["inventory"] = [inv]
    return {"forge_marker": 1, "defaults": {"model": model, "textures": dict(ftex)},
            "variants": variants}


def tv_state(piece, spec, ftex):
    tex = dict(ftex, screen=T("tv_off"))
    channel = {c: {"textures": {"screen": T("tv_" + c)}} for c in CHANNELS}
    item = BASE + piece + "_item"
    item_tex = {"screen": T("tv_nature")}
    if spec["wide"] and not spec["wall"]:
        # Both the block and the rest pick the model: every combination written out.
        variants = {}
        for c in CHANNELS:
            for f, r in sorted(ROT.items()):
                for part in (0, 1):
                    for rest, _d in A.RESTS:
                        v = {"model": BASE + "%s_cell%d_%s" % (piece, part, rest),
                             "textures": {"screen": T("tv_" + c)}}
                        if r:
                            v["y"] = r
                        variants["channel=%s,facing=%s,part=%d,rest=%s" % (c, f, part, rest)] = [v]
        state = forge(BASE + piece + "_cell0_floor", tex, {}, item, item_tex)
        state["variants"].update(variants)
        return state
    variants = {"channel": channel, "facing": facing_variants()}
    if spec["wide"]:
        variants["part"] = {str(p): {"model": BASE + "%s_cell%d" % (piece, p)} for p in (0, 1)}
        variants["rest"] = empty(r for r, _d in A.RESTS)
        model = BASE + piece + "_cell0"
    elif spec["wall"]:
        variants["part"] = empty(("0", "1"))
        variants["rest"] = empty(r for r, _d in A.RESTS)
        model = BASE + piece
    else:
        variants["part"] = empty(("0", "1"))
        variants["rest"] = {r: {"model": BASE + "%s_%s" % (piece, r)} for r, _d in A.RESTS}
        model = BASE + piece + "_floor"
    return forge(model, tex, variants, item, item_tex)


def big_tv_state(piece, spec, ftex):
    """A big TV's cell (row * cols + col, actual state) picks its model; on a stand the rest
    does too, so those combinations are written out, as the large stand TV's are."""
    tex = dict(ftex, screen=T("tv_off"))
    cols = spec["cols"]
    cells = range(cols * BIG_TV_ROWS)
    item = BASE + piece + "_item"
    item_tex = {"screen": T("tv_nature")}

    def cell_model(i, rest=""):
        return BASE + "%s_c%dr%d" % (piece, i % cols, i // cols) + ("_" + rest if rest else "")
    if spec["wall"]:
        variants = {"cell": {str(i): {"model": cell_model(i)} for i in cells},
                    "channel": {c: {"textures": {"screen": T("tv_" + c)}} for c in CHANNELS},
                    "facing": facing_variants()}
        return forge(cell_model(0), tex, variants, item, item_tex)
    state = forge(cell_model(0, "floor"), tex, {}, item, item_tex)
    for i in cells:
        for c in CHANNELS:
            for f, r in sorted(ROT.items()):
                for rest, _d in BIG_RESTS:
                    v = {"model": cell_model(i, rest), "textures": {"screen": T("tv_" + c)}}
                    if r:
                        v["y"] = r
                    state["variants"]["cell=%d,channel=%s,facing=%s,rest=%s" % (
                        i, c, f, rest)] = [v]
    return state


def counter_state(piece, spec, ftex):
    tex = dict(ftex)
    variants = {"facing": facing_variants(),
                "rest": {r: {"model": BASE + "%s_%s" % (piece, r)} for r, _d in A.RESTS}}
    item_tex = None
    if spec.get("record"):
        tex.update(platter=T("platter_mat"), glow=T("display_off"))
        variants["record"] = {
            "true": {"textures": {"platter": T("platter_record"), "glow": T("display_on")}},
            "false": {"textures": {"platter": T("platter_mat"), "glow": T("display_off")}}}
        item_tex = {"platter": T("platter_record"), "glow": T("display_on")}
    if spec.get("glow"):
        key, off, on = spec["glow"]
        tex[key] = on
        variants["lit"] = switch_tex(key, off, on)
        variants["powered"] = empty(("true", "false"))
    return forge(BASE + piece + "_floor", tex, variants, BASE + piece + "_item", item_tex)


def multipart(reg, rules):
    parts = []
    for part, when, r in rules:
        apply = {"model": MODEL + "%s_%s" % (reg, part)}
        if r:
            apply["y"] = r
        parts.append({"when": when, "apply": apply} if when else {"apply": apply})
    return {"multipart": parts}


FAN_RULES = [("body", {}, 0), ("blades", {"fan": "false"}, 0),
             ("bowl_on", {"light": "true"}, 0), ("bowl_off", {"light": "false"}, 0)]

EXTRA_LANG = {
    "tile.switch_relay.name": ("Light Switch Relay", "Lichtschalter-Relais",
                               "Relé de interruptor", "Strömbrytarrelä"),
    "csm.furnishings.switch.linked": (
        "Switch linked to %s at %s %s %s", "Schalter verbunden mit %s bei %s %s %s",
        "Interruptor vinculado a %s en %s %s %s", "Strömbrytaren kopplad till %s vid %s %s %s"),
    "csm.furnishings.switch.cleared": (
        "Switch link cleared", "Schalterverbindung gelöscht",
        "Vínculo del interruptor borrado", "Strömbrytarens koppling borttagen"),
    "csm.furnishings.switch.link_info": (
        "This switch is linked to %s at %s %s %s", "Dieser Schalter ist mit %s bei %s %s %s verbunden",
        "Este interruptor está vinculado a %s en %s %s %s",
        "Den här strömbrytaren är kopplad till %s vid %s %s %s"),
    "csm.furnishings.switch.not_linked": (
        "This switch is not linked: it powers only what is beside it",
        "Dieser Schalter ist nicht verbunden: er versorgt nur, was neben ihm ist",
        "Este interruptor no está vinculado: solo alimenta lo que tiene al lado",
        "Den här strömbrytaren är inte kopplad: den driver bara det som är bredvid den"),
    "csm.furnishings.switch.too_far": (
        "The linked block is %s blocks away (at most %s): this switch is not linked",
        "Der verbundene Block ist %s Blöcke entfernt (höchstens %s): dieser Schalter ist nicht verbunden",
        "El bloque vinculado está a %s bloques (como mucho %s): este interruptor no está vinculado",
        "Det kopplade blocket är %s block bort (högst %s): strömbrytaren är inte kopplad"),
    "csm.furnishings.switch.other_dimension": (
        "The linked block is in another dimension: this switch is not linked",
        "Der verbundene Block ist in einer anderen Dimension: dieser Schalter ist nicht verbunden",
        "El bloque vinculado está en otra dimensión: este interruptor no está vinculado",
        "Det kopplade blocket är i en annan dimension: strömbrytaren är inte kopplad"),
    "csm.furnishings.switch.no_room": (
        "No free space beside %s to power it from",
        "Kein freier Platz neben %s, um ihn mit Strom zu versorgen",
        "No hay espacio libre junto a %s para alimentarlo",
        "Ingen ledig plats bredvid %s att driva den från"),
    "csm.furnishings.switch.unloaded": (
        "The linked block is not loaded", "Der verbundene Block ist nicht geladen",
        "El bloque vinculado no está cargado", "Det kopplade blocket är inte laddat"),
    "csm.furnishings.switch.gone": (
        "The linked block is gone", "Der verbundene Block ist weg",
        "El bloque vinculado ya no está", "Det kopplade blocket är borta"),
    "csm.furnishings.switch.tooltip": (
        "Linked to %s %s %s", "Verbunden mit %s %s %s", "Vinculado a %s %s %s",
        "Kopplad till %s %s %s"),
    "csm.furnishings.switch.hint": (
        "Right-click a lamp, door or other redstone block to link",
        "Rechtsklick auf eine Lampe, Tür oder einen anderen Redstone-Block zum Verbinden",
        "Clic derecho en una lámpara, puerta u otro bloque de redstone para vincular",
        "Högerklicka på en lampa, dörr eller annat redstoneblock för att koppla"),
}


def lang_entries():
    out = {loc: {} for loc in R.LOCALES}
    for key, names in EXTRA_LANG.items():
        for i, loc in enumerate(R.LOCALES):
            out[loc][key] = names[i]
    for reg, _p, _k, _t, names, _j in entries():
        for i, loc in enumerate(R.LOCALES):
            out[loc]["tile.%s.name" % reg] = names[i]
    return out


# The hidden relay a linked light switch places: invisible, but it needs a model to load.
RELAY = "switch_relay"
RELAY_MODEL = {"parent": "block/block", "textures": {"particle": T("clear")}, "elements": []}
RELAY_STATE = {"forge_marker": 1,
               "defaults": {"model": BASE + "switch_relay"},
               "variants": {"facing": empty(("down", "up", "north", "south", "west", "east")),
                            "inventory": [{}]}}


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
    dump("models/block/%s/base/switch_relay.json" % SUB, RELAY_MODEL)
    dump("blockstates/%s.json" % RELAY, RELAY_STATE)
    for reg, piece, kind, ftex, _names, _java in entries():
        spec = PIECE[piece][3]
        # Every finish names the texture its models' particle points to, or the game logs an
        # upward reference and draws the missing texture.
        for name, key in particles.items():
            if (name == piece or name.startswith(piece + "_")) and key not in ftex:
                ftex[key] = DEFAULT_TEX[key]
        blk = "models/block/%s/%s_%%s.json" % (SUB, reg)
        item = "models/item/%s.json" % reg
        if kind == "tv":
            state = tv_state(piece, spec, ftex)
        elif kind == "bigtv":
            state = big_tv_state(piece, spec, ftex)
        elif kind == "counter":
            state = counter_state(piece, spec, ftex)
        elif kind == "single":
            state = forge(BASE + piece, ftex, {"facing": facing_variants()})
        elif kind == "lamp":
            tex = dict(ftex, shade=T("linen_on"))
            state = forge(BASE + piece, tex, {
                "facing": facing_variants(),
                "lit": switch_tex("shade", T("linen_off"), T("linen_on")),
                "powered": empty(("true", "false"))})
        elif kind == "wide":
            variants = {"facing": facing_variants(),
                        "part": {str(p): {"model": BASE + "%s_cell%d" % (piece, p)}
                                 for p in (0, 1)}}
            tex = dict(ftex)
            item_tex = None
            if spec.get("lit"):
                tex.update(flame=T("clear"), embers=T("embers_off"))
                variants["lit"] = {
                    "true": {"textures": {"flame": T("flame"), "embers": T("embers_on")}},
                    "false": {"textures": {"flame": T("clear"), "embers": T("embers_off")}}}
                item_tex = {"flame": T("flame"), "embers": T("embers_on")}
            state = forge(BASE + piece + "_cell0", tex, variants, BASE + piece + "_item",
                          item_tex)
        elif kind == "fan":
            copy_model(blk % "body", "ceiling_fan_body", {"nickel": RT("metal_steel")})
            copy_model(blk % "blades", "ceiling_fan_blades", ftex)
            copy_model(blk % "bowl_on", "ceiling_fan_bowl", {"bowl": T("bowl_on")})
            copy_model(blk % "bowl_off", "ceiling_fan_bowl", {"bowl": T("bowl_off")})
            copy_model(item, "ceiling_fan_item", ftex)
            state = multipart(reg, FAN_RULES)
        elif kind == "switch":
            state = forge(BASE + piece + "_off", ftex, {
                "facing": facing_variants(),
                "powered": {"true": {"model": BASE + piece + "_on"},
                            "false": {"model": BASE + piece + "_off"}}})
        elif kind == "mat":
            for part in ("field", "side", "corner"):
                copy_model(blk % part, "rug_%s" % part, ftex, R.SUB)
            copy_model(item, "rug_item", ftex, R.SUB)
            state = multipart(reg, B.rug_rules())
        elif kind == "crate":
            copy_model(blk % "body", "%s_body" % piece, ftex)
            copy_model(item, "%s_item" % piece, ftex)
            state = multipart(reg, R.faced([("body", {})]))
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
    for reg, piece, _k, _t, names, java in entries():
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
    tmp = tempfile.mkdtemp(prefix="living_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not R.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("living extras are up to date (%d files)" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
