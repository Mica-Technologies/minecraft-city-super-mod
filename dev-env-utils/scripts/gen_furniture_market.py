#!/usr/bin/env python3
"""
gen_furniture_market.py -- the Market & Store tab in the Furniture & Novelties module: the
refrigerated displays of a grocery store (glass-door reach-in coolers and freezers, the open
multideck dairy case, the island freezer, the ice cream dipping cabinet and the deli and bakery
service cases), all stocked, lit and holding things; gondola shelving stocked six ways and bare;
the produce stand, the hanging produce scale and the bulk bins; the checkout lane (belt counter,
scanner counter, bagging end), the POS terminal and the old cash register, the receipt printer,
the card terminal on its stand, the self-checkout kiosk, the customer service desk, the candy
rack and the bagging carousel; and the shop floor's fixtures: shopping carts and the cart corral,
basket stacks, security gates, hanging aisle signs, a magazine rack and a bottle return machine.

It also writes the Verifone MX915's blockstate and its models standing on a counter: the
terminal moved to this tab from the Technology module (its hand-made model and texture keep
their paths), and it now rests on what is under it as the counter pieces do.

Borrows gen_furniture_residential.py's element helpers, woods, output and lang writer, the
kitchen's cut of a two-block piece and its finishes, the bedroom's clip, shift and item
displays, the appliances' surface rests and lamps, the office's laminates and its reception desk
(the customer service desk is that desk in a store's colours) (all imported, not copied), and
the Life Safety generators' 3 x 5 pixel font, and writes, under
modules/furnishings/src/main/resources/assets/csm:

  * textures/blocks/furniture/market/*.png  the stock (each a sheet of four product columns
    seen from the front, and one of their tops), glass, liners and grilles, lit header signs,
    the conveyor belt (animated, with its .mcmeta), scanner glass, screens, the aisle numbers,
    wire mesh, plastics, magazine covers and the bulk bins' contents
  * models/block/furniture/market/base/*.json  the geometry
  * models/block/furniture/market/<registry>_<part>.json  a finish's copy of each part a
    multipart blockstate picks (lit and unlit copies of a lit part)
  * blockstates/<registry>.json, and models/item/<registry>.json where the state has no
    inventory variant
  * the tile and tab names in all four languages, by key

Every model faces north with its back at +Z, as the Residential tab's do: a shopper stands at
its front. Real-world scale, 1 block = 1 m: reach-in coolers 2 m, the checkout counter's top at
0.9 m (a countertop's height, so the terminal and the register rest on it), gondola shelving a
metre a block with a shelf at the half.

What joins (the Java classes compute it as actual state; nothing is stored):

  * reach-in coolers, freezers and dairy cases of one block join into one line of doors, the
    end panels only where it stops; so do the island freezers, the ice cream cabinets and the
    deli and bakery cases;
  * gondola shelving joins and stacks with any gondola, whatever its stock: uprights only at a
    run's ends, the base deck only at the bottom of a stack, the top cap only at its head;
  * produce stands join any produce stand, the bulk bins any bulk bins, the checkout's belt,
    scanner and bagging counters are one lane, the cart corral joins itself.

Usage:
    python gen_furniture_market.py              # write everything
    python gen_furniture_market.py --check      # fail if the tree has drifted
    python gen_furniture_market.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import argparse
import copy
import json
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
import gen_furniture_office as O  # noqa: E402
import life_safety_gen_common as LS  # noqa: E402

ASSETS = R.ASSETS
SUB = "furniture/market"
MODEL = "csm:%s/" % SUB
BASE = MODEL + "base/"
T = R.T  # the Residential tab's textures
OT = O.OT  # the office's


def MT(name):
    """One of this tab's own textures."""
    return "csm:blocks/%s/%s" % (SUB, name)


PRODUCE_TEX = "csm:blocks/furniture/produce/%s"
ROT = R.ROT
el, board = R.el, R.board
mirror_x, turn = R.mirror_x, R.turn
ALL = R.ALL
ALL_UV = A.ALL_UV
SIDES = ("north", "south", "east", "west")
NO_DOWN = SIDES + ("up",)
NO_UP = SIDES + ("down",)
NO_BACK = ("north", "east", "west", "up", "down")
FRONT_UP = ("north", "up")
shift, clip = B.shift, B.clip
shade, clamp = R.shade, R.clamp


# ------------------------------------------------------------------------------------------
# Texture helpers
# ------------------------------------------------------------------------------------------
def blank(size):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def noisy(base, seed, size=16, grain=0.03, alpha=255):
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = shade(base, 1.0 + rng.uniform(-grain, grain)) + (alpha,)
    return img


def put(px, x, y, col, size=32):
    if 0 <= x < size and 0 <= y < size:
        px[x, y] = tuple(clamp(col[:3])) + ((col[3],) if len(col) == 4 else (255,))


def fill_rect(px, x0, y0, x1, y1, col, size=32):
    for y in range(y0, y1):
        for x in range(x0, x1):
            put(px, x, y, col, size)


RED = (200, 44, 44)
BLUE = (44, 92, 188)
GREEN = (54, 150, 72)
YELLOW = (236, 192, 44)
ORANGE = (232, 124, 36)
PURPLE = (122, 64, 164)
TEAL = (34, 150, 150)
PINK = (222, 96, 146)
BROWN = (120, 72, 40)
CREAM = (242, 234, 212)
WHITE = (242, 242, 238)
BLACK = (32, 32, 36)
SILVER = (188, 192, 198)
LIME = (140, 196, 60)


# --- product columns: each draws one product, repeated, into a column 8 texels wide (4 px) on
# a 32 px sheet, standing on the bottom of the sheet, ht texels tall (twice its height in px).
def col_cans(px, x0, ht, body, band, rng):
    """Cans three abreast, stacked in tiers two pixels high: the body in its colour with a band
    round it and a silver lid; the tier above stands on it."""
    for cx in (0, 3, 6):
        if cx + 2 > 8:
            continue
        for tier in range(ht // 4):
            yb = 31 - tier * 4
            for dx in range(2):
                x = x0 + cx + dx
                k = 0.86 + 0.18 * (dx == 0)
                put(px, x, yb, shade(body, 0.7 * k))
                put(px, x, yb - 1, shade(body, k))
                put(px, x, yb - 2, shade(band, k))
                put(px, x, yb - 3, shade(SILVER, 1.0 + 0.1 * (dx == 0)))


def top_cans(px, x0, rng):
    """Can tops from above: silver discs in rows going back, dark between."""
    for y in range(32):
        for dx in range(8):
            x = x0 + dx
            inside = (dx % 3 != 2) and (y % 3 != 2)
            put(px, x, y, shade(SILVER, 1.1 if (dx % 3 == 0 and y % 3 == 0) else 0.95)
                if inside else (40, 40, 44, 0))


def col_bottles(px, x0, ht, liquid, cap, label, rng):
    """Bottles two abreast, 1.5 px wide: the body (liquid seen through it, a label round its
    middle), the shoulder, a neck and a cap."""
    for bx in (0, 4):
        neck = 3
        body_h = ht - neck - 2
        for dy in range(ht):
            y = 31 - dy
            for dx in range(3):
                x = x0 + bx + dx
                k = 0.9 + 0.12 * (dx == 0) - 0.08 * (dx == 2)
                if dy < body_h:
                    col = liquid
                    if body_h * 0.35 <= dy < body_h * 0.35 + 2:
                        col = label
                    put(px, x, y, shade(col, k))
                elif dy < body_h + 1:
                    put(px, x, y, shade(liquid, k * 1.1))
                elif dy < ht - 1:
                    if dx == 1:
                        put(px, x, y, shade(liquid, 1.15))
                else:
                    if dx == 1:
                        put(px, x, y, cap)


def top_bottles(px, x0, cap, rng):
    """Bottles from above: a cap on each, transparent between (the shelf shows through)."""
    for y in range(32):
        for dx in range(8):
            put(px, x0 + dx, y, (0, 0, 0, 0))
    for y in range(1, 32, 4):
        for bx in (1, 5):
            put(px, x0 + bx, y, cap)
            put(px, x0 + bx, y + 1, shade(cap, 0.8))


def col_boxes(px, x0, ht, body, accent, rng):
    """A box front 3.5 px wide: the colour, a title band near the top with a few dark marks for
    the (invented) name, and a bowl of something in the middle."""
    for dy in range(ht):
        y = 31 - dy
        for dx in range(7):
            x = x0 + dx
            col = body
            if ht - 4 <= dy < ht - 1:
                col = accent
                if dy == ht - 3 and dx in (1, 2, 4, 5):
                    col = shade(body, 0.55)
            elif 2 <= dy < 2 + max(2, ht // 3) and 1 <= dx <= 5:
                col = WHITE if dy < 3 or dx in (1, 5) else (220, 170, 90)
            if dx == 0:
                col = shade(col, 0.78)
            if dy == 0:
                col = shade(col, 0.7)
            put(px, x, y, col)


def top_boxes(px, x0, body, rng):
    """Box tops going back, one box every two pixels, each with its darker edge."""
    for y in range(32):
        for dx in range(8):
            col = body if dx < 7 else (0, 0, 0, 0)
            if dx < 7 and y % 4 == 3:
                col = shade(body, 0.7)
            put(px, x0 + dx, y, col if len(col) == 4 else shade(col, 1.08))


def col_bags(px, x0, ht, body, window, rng):
    """A bag of crisps: crimped seams top and bottom, the colour, a window of crisps."""
    for dy in range(ht):
        y = 31 - dy
        for dx in range(7):
            x = x0 + dx
            col = body
            if dy >= ht - 2 or dy < 1:
                col = shade(SILVER, 1.0) if (dx + dy) % 2 else shade(body, 1.2)
                if dy == ht - 1 and dx in (0, 6):
                    continue
            elif abs(dx - 3) <= 1 and ht // 3 <= dy < ht // 3 + 3:
                col = window
            elif dy == ht - 4 and 1 <= dx <= 5:
                col = WHITE
            k = 1.0 - 0.1 * abs(dx - 3) / 3.0
            put(px, x, y, shade(col, k))


def top_bags(px, x0, body, rng):
    for y in range(32):
        for dx in range(8):
            col = (0, 0, 0, 0) if dx == 7 else shade(body, 0.9 if y % 3 == 0 else 1.05)
            put(px, x0 + dx, y, col)


def col_jugs(px, x0, ht, body, cap, rng):
    """A detergent jug: the handle's gap at the top right, the cap at the top left, a label."""
    for dy in range(ht):
        y = 31 - dy
        for dx in range(7):
            x = x0 + dx
            col = body
            if dy >= ht - 2:
                if dx <= 1:
                    col = cap
                elif dx >= 4 and dy == ht - 2 and dx != 5:
                    col = body
                elif dx >= 4:
                    col = body if dx in (4, 6) else None
                else:
                    col = None
            elif 2 <= dy < ht - 4 and 1 <= dx <= 5:
                col = WHITE if dy != 3 else cap
            if col is None:
                continue
            put(px, x, y, shade(col, 1.05 - 0.05 * (dx == 6)))


def col_spray(px, x0, ht, body, rng):
    """Spray bottles two abreast: a slim bottle and its trigger head."""
    for bx in (0, 4):
        for dy in range(ht):
            y = 31 - dy
            for dx in range(3):
                x = x0 + bx + dx
                if dy < ht - 3:
                    col = body if not (ht // 3 <= dy < ht // 3 + 2) else WHITE
                    put(px, x, y, shade(col, 0.92 + 0.1 * (dx == 0)))
                elif dy < ht - 1:
                    if dx == 1 or (dy == ht - 2 and dx == 0):
                        put(px, x, y, (230, 230, 230))
                else:
                    if dx < 2:
                        put(px, x, y, (230, 230, 230))


def col_rolls(px, x0, ht, band, rng):
    """A pack of kitchen roll: two white rolls side by side in their wrap, a coloured band."""
    for dy in range(ht):
        y = 31 - dy
        for dx in range(8):
            x = x0 + dx
            r = dx % 4
            k = (0.86, 1.0, 1.02, 0.9)[r]
            col = WHITE
            if ht // 2 - 1 <= dy <= ht // 2:
                col = band
            put(px, x, y, shade(col, k))


def col_small_boxes(px, x0, ht, body, stripe, rng):
    """Small cartons stacked three pixels... in tiers of three texels: a stripe on each."""
    for dy in range(ht):
        y = 31 - dy
        t = dy % 3
        for dx in range(7):
            x = x0 + dx
            col = body if t != 1 else stripe
            if t == 2:
                col = shade(body, 1.12)
            if dx == 0:
                col = shade(col, 0.8)
            put(px, x, y, col)


def col_shampoo(px, x0, ht, body, cap, rng):
    """Shampoo bottles two abreast: rounded shoulders, a flip cap, a pale label."""
    for bx in (0, 4):
        for dy in range(ht):
            y = 31 - dy
            for dx in range(3):
                x = x0 + bx + dx
                if dy < ht - 2:
                    col = body
                    if 2 <= dy < ht - 4 and dx == 1:
                        col = WHITE
                    if dy == ht - 3 and dx != 1:
                        col = shade(body, 1.15)
                    put(px, x, y, shade(col, 0.95 + 0.08 * (dx == 0)))
                elif dx == 1 or dy == ht - 2:
                    put(px, x, y, cap)


def col_tubs(px, x0, ht, lid, band, rng):
    """Ice cream tubs: a coloured lid, a cream tub with a band of the flavour's colour."""
    for dy in range(ht):
        y = 31 - dy
        for dx in range(7):
            x = x0 + dx
            if dy >= ht - 2:
                col = lid if dy == ht - 2 else shade(lid, 1.15)
            elif 1 <= dy <= 2:
                col = band
            else:
                col = CREAM
            if dx in (0, 6) and dy < ht - 2:
                col = shade(col, 0.85)
            put(px, x, y, col)


def top_tubs(px, x0, lid, rng):
    for y in range(32):
        for dx in range(8):
            col = (0, 0, 0, 0) if dx == 7 or y % 8 == 7 else shade(lid, 1.1 - 0.15 * (y % 8 == 0))
            put(px, x0 + dx, y, col)


def col_frozen(px, x0, ht, body, rng):
    """Frozen food boxes lying flat and stacked, each three texels deep: frosted edges, a white
    window."""
    for dy in range(ht):
        y = 31 - dy
        t = dy % 3
        for dx in range(8):
            x = x0 + dx
            col = body
            if t == 1 and 2 <= dx <= 5:
                col = WHITE
            if t == 2:
                col = shade(body, 1.2)
            put(px, x, y, col)


def top_frozen(px, x0, body, accent, rng):
    """A frozen pizza or meal box from above: the colour, a picture in the middle."""
    for y in range(32):
        for dx in range(8):
            col = body
            if 2 <= dx <= 5 and 2 <= y % 8 <= 5:
                col = accent
            if y % 8 == 7:
                col = shade(body, 0.7)
            put(px, x0 + dx, y, col)


def col_milk(px, x0, ht, cap, label, rng):
    """Milk jugs two abreast: white, the cap in the colour of the milk, a label."""
    for bx in (0, 4):
        for dy in range(ht):
            y = 31 - dy
            for dx in range(4):
                if bx + dx > 7:
                    continue
                x = x0 + bx + dx
                if dx == 3:
                    continue
                if dy < ht - 2:
                    col = (238, 238, 232)
                    if ht // 3 <= dy < ht // 3 + 2:
                        col = label
                    put(px, x, y, shade(col, 0.92 + 0.1 * (dx == 0)))
                elif dx == 1:
                    put(px, x, y, cap)
                elif dy == ht - 2:
                    put(px, x, y, (230, 230, 224))


def col_yogurt(px, x0, ht, foil, rng):
    """Yoghurt pots in rows: white cups with a coloured foil lid, in tiers."""
    for dy in range(ht):
        y = 31 - dy
        t = dy % 3
        for dx in range(8):
            if dx % 2 == 1 and t == 2:
                continue
            x = x0 + dx
            col = foil if t == 2 else (WHITE if t == 1 else shade(foil, 0.9))
            put(px, x, y, col)


def col_cheese(px, x0, ht, rng):
    """Cheese in packs two abreast, stacked: orange and yellow blocks under clear wrap, each with
    a red label corner."""
    for dy in range(ht):
        y = 31 - dy
        t = dy % 3
        for dx in range(8):
            if dx in (3, 7):
                continue
            x = x0 + dx
            col = (238, 196, 70) if ((dy // 3) + (dx // 4)) % 2 == 0 else (232, 140, 44)
            if t == 2:
                col = shade(col, 1.15)
            if dx % 4 == 2 and t == 1:
                col = RED
            put(px, x, y, col)


def col_candy(px, x0, ht, box, rng):
    """A display box of chocolate bars: the box's front below, bars standing in it above, each
    in its own wrapper."""
    for dy in range(ht):
        y = 31 - dy
        for dx in range(8):
            x = x0 + dx
            if dy < ht // 2:
                col = box if dy != ht // 2 - 1 else shade(box, 1.25)
            else:
                col = rng.choice((RED, BLUE, YELLOW, BROWN, PURPLE, GREEN))
                if dx % 2 == 1:
                    col = shade(col, 0.8)
            put(px, x, y, col)


def col_bread(px, x0, ht, crust, rng):
    """A loaf seen end on: a domed golden crust, darker at the base, a gap to the next."""
    for dy in range(ht):
        y = 31 - dy
        for dx in range(7):
            x = x0 + dx
            if dy >= ht - 2 and dx in (0, 6):
                continue
            if dy == ht - 1 and dx in (1, 5):
                continue
            k = 0.72 + 0.38 * dy / max(1, ht - 1)
            col = shade(crust, k)
            if dy == ht - 2 and dx == 3:
                col = shade(crust, 1.3)
            put(px, x, y, col)


def top_bread(px, x0, crust, rng):
    """Loaves from above, lying front to back: rounded ends, slashes scored across the crust,
    the tray showing between."""
    for y in range(32):
        for dx in range(8):
            put(px, x0 + dx, y, (0, 0, 0, 0))
    for start in (1, 17):
        for y in range(start, start + 13):
            for dx in range(7):
                end = y in (start, start + 12)
                if end and dx in (0, 6):
                    continue
                col = shade(crust, 1.1 if dx in (2, 3) else 0.9)
                if (y - start) % 4 == 2 and 1 <= dx <= 5:
                    col = shade(crust, 1.35)
                put(px, x0 + dx, y, col)


def col_pastry(px, x0, ht, kind, rng):
    """Pastries on a tray: croissants (golden crescents) or iced doughnuts."""
    for dy in range(ht):
        y = 31 - dy
        for dx in range(8):
            x = x0 + dx
            if kind.startswith("donut"):
                icing = (236, 120, 170) if kind == "donut" else (100, 60, 40)
                col = (206, 150, 84) if dy < ht // 2 else icing
                if dx % 4 == 3:
                    continue
            else:
                col = shade((222, 162, 72), 0.8 + 0.1 * (dx % 3) + 0.1 * dy / max(1, ht))
                if dx % 4 == 3 and dy > 0:
                    continue
            put(px, x, y, col)


def top_pastry(px, x0, kind, rng):
    """Pastries from above on a tray: iced rings with a hole, or golden crescents."""
    for y in range(32):
        for dx in range(8):
            put(px, x0 + dx, y, (0, 0, 0, 0))
    icing = {"donut": (236, 120, 170), "donut_choc": (100, 60, 40)}.get(kind)
    for cy in range(2, 32, 5):
        for dx in range(8):
            for dy in range(-2, 3):
                d = math.hypot(dx - 3.5, dy)
                if d > 3.2:
                    continue
                if icing:
                    if d < 1.0:
                        continue
                    col = icing if d < 2.4 else (206, 150, 84)
                    if (dx + dy) % 3 == 0 and d < 2.4:
                        col = rng.choice(((250, 250, 250), (90, 180, 230), (250, 220, 60)))
                else:
                    if dy > 1:
                        continue
                    col = shade((222, 162, 72), 1.2 - 0.12 * abs(dy) - 0.05 * (dx % 2))
                put(px, x0 + dx, cy + dy, col)


def top_plain(px, x0, col, rng, grain=0.06):
    for y in range(32):
        for dx in range(8):
            put(px, x0 + dx, y, shade(col, 1.0 + rng.uniform(-grain, grain)))


def product_sheets(seed, columns):
    """The stock of a shelf: four product columns, each (front drawer, top drawer), as two 32 px
    sheets, the fronts and the tops."""
    rng = random.Random(seed)
    front, top = blank(32), blank(32)
    fp, tp = front.load(), top.load()
    for j, (draw_front, draw_top) in enumerate(columns):
        draw_front(fp, 8 * j, rng)
        draw_top(tp, 8 * j, rng)
    return front, top


# Each stock kind: four columns of (front drawer, top drawer, height in px). The heights decide
# the model's product rows (stock_row), so they live beside the drawings.
def _c(fn, *a):
    return lambda px, x0, rng, h: fn(px, x0, h, *a, rng)


def _t(fn, *a):
    return lambda px, x0, rng: fn(px, x0, *a, rng)


STOCK = {
    # the reach-in cooler: 2 l bottles, small bottles, cans two tiers high, water
    "drinks": [(_c(col_bottles, (96, 44, 24), RED, RED), _t(top_bottles, RED), 5),
               (_c(col_bottles, (170, 214, 110), (40, 150, 60), GREEN), _t(top_bottles, GREEN), 5),
               (_c(col_cans, BLUE, SILVER), _t(top_cans), 4),
               (_c(col_bottles, (150, 200, 236), BLUE, WHITE), _t(top_bottles, BLUE), 4)],
    "drinks_b": [(_c(col_cans, RED, WHITE), _t(top_cans), 4),
                 (_c(col_bottles, (236, 150, 40), ORANGE, ORANGE), _t(top_bottles, ORANGE), 5),
                 (_c(col_cans, (40, 160, 90), YELLOW), _t(top_cans), 4),
                 (_c(col_bottles, (80, 170, 230), (30, 90, 180), SILVER),
                  _t(top_bottles, (30, 90, 180)), 5)],
    # the reach-in freezer: ice cream tubs, frozen meal boxes
    "frozen": [(_c(col_tubs, PINK, (230, 150, 180)), _t(top_tubs, PINK), 3),
               (_c(col_frozen, RED), _t(top_frozen, RED, YELLOW), 4.5),
               (_c(col_tubs, (100, 60, 36), (150, 96, 60)), _t(top_tubs, (100, 60, 36)), 3),
               (_c(col_frozen, GREEN), _t(top_frozen, GREEN, (240, 200, 90)), 4.5)],
    "frozen_b": [(_c(col_frozen, BLUE), _t(top_frozen, BLUE, (230, 90, 60)), 4.5),
                 (_c(col_tubs, (90, 180, 120), (170, 230, 190)), _t(top_tubs, (90, 180, 120)), 3),
                 (_c(col_frozen, ORANGE), _t(top_frozen, ORANGE, WHITE), 4.5),
                 (_c(col_tubs, CREAM, YELLOW), _t(top_tubs, YELLOW), 3)],
    # the dairy case: milk jugs, yoghurt, cheese, butter
    "dairy": [(_c(col_milk, BLUE, BLUE), _t(top_bottles, BLUE), 4.5),
              (_c(col_milk, RED, RED), _t(top_bottles, RED), 4.5),
              (_c(col_yogurt, PINK), _t(top_plain, WHITE), 3),
              (_c(col_cheese), _t(top_plain, (238, 196, 70)), 3)],
    "dairy_b": [(_c(col_yogurt, (120, 90, 200)), _t(top_plain, WHITE), 3),
                (_c(col_milk, (60, 150, 70), GREEN), _t(top_bottles, GREEN), 4.5),
                (_c(col_small_boxes, YELLOW, (240, 240, 230)), _t(top_boxes, YELLOW), 3),
                (_c(col_milk, (250, 210, 40), YELLOW), _t(top_bottles, YELLOW), 4.5)],
    # gondola shelving
    "canned": [(_c(col_cans, RED, WHITE), _t(top_cans), 4),
               (_c(col_cans, GREEN, YELLOW), _t(top_cans), 4),
               (_c(col_cans, ORANGE, (240, 230, 200)), _t(top_cans), 4),
               (_c(col_cans, BLUE, WHITE), _t(top_cans), 4)],
    "canned_b": [(_c(col_cans, YELLOW, RED), _t(top_cans), 4),
                 (_c(col_cans, (150, 40, 40), (240, 200, 60)), _t(top_cans), 4),
                 (_c(col_cans, TEAL, WHITE), _t(top_cans), 4),
                 (_c(col_cans, BROWN, CREAM), _t(top_cans), 4)],
    "cereal": [(_c(col_boxes, RED, YELLOW), _t(top_boxes, RED), 5),
               (_c(col_boxes, YELLOW, BLUE), _t(top_boxes, YELLOW), 5.5),
               (_c(col_boxes, BLUE, WHITE), _t(top_boxes, BLUE), 5),
               (_c(col_boxes, GREEN, ORANGE), _t(top_boxes, GREEN), 4.5)],
    "cereal_b": [(_c(col_boxes, PURPLE, YELLOW), _t(top_boxes, PURPLE), 5),
                 (_c(col_boxes, ORANGE, WHITE), _t(top_boxes, ORANGE), 5.5),
                 (_c(col_boxes, BROWN, CREAM), _t(top_boxes, BROWN), 5),
                 (_c(col_boxes, WHITE, RED), _t(top_boxes, WHITE), 4.5)],
    "snacks": [(_c(col_bags, RED, (232, 190, 80)), _t(top_bags, RED), 5),
               (_c(col_bags, BLUE, (236, 200, 90)), _t(top_bags, BLUE), 5),
               (_c(col_bags, YELLOW, (210, 150, 60)), _t(top_bags, YELLOW), 4.5),
               (_c(col_bags, GREEN, (240, 210, 110)), _t(top_bags, GREEN), 5)],
    "snacks_b": [(_c(col_bags, PURPLE, (236, 200, 90)), _t(top_bags, PURPLE), 5),
                 (_c(col_bags, ORANGE, (240, 210, 110)), _t(top_bags, ORANGE), 4.5),
                 (_c(col_bags, BLACK, (232, 190, 80)), _t(top_bags, BLACK), 5),
                 (_c(col_bags, TEAL, (236, 200, 90)), _t(top_bags, TEAL), 5)],
    "bottled": [(_c(col_bottles, (96, 44, 24), RED, RED), _t(top_bottles, RED), 5),
                (_c(col_bottles, (150, 200, 236), BLUE, WHITE), _t(top_bottles, BLUE), 4),
                (_c(col_bottles, (236, 150, 40), ORANGE, ORANGE), _t(top_bottles, ORANGE), 5),
                (_c(col_bottles, (170, 214, 110), (40, 150, 60), GREEN), _t(top_bottles, GREEN),
                 5)],
    "bottled_b": [(_c(col_small_boxes, RED, WHITE), _t(top_boxes, RED), 3),
                  (_c(col_bottles, (60, 30, 20), (230, 200, 90), YELLOW), _t(top_bottles, YELLOW),
                   5),
                  (_c(col_small_boxes, BLUE, SILVER), _t(top_boxes, BLUE), 3),
                  (_c(col_bottles, (200, 60, 90), PINK, WHITE), _t(top_bottles, WHITE), 4)],
    "household": [(_c(col_jugs, ORANGE, BLUE), _t(top_plain, ORANGE), 5),
                  (_c(col_spray, (80, 170, 230)), _t(top_bottles, WHITE), 5),
                  (_c(col_rolls, BLUE), _t(top_plain, WHITE), 5.5),
                  (_c(col_jugs, BLUE, WHITE), _t(top_plain, BLUE), 5)],
    "household_b": [(_c(col_small_boxes, GREEN, YELLOW), _t(top_boxes, GREEN), 3),
                    (_c(col_spray, (240, 200, 60)), _t(top_bottles, WHITE), 5),
                    (_c(col_jugs, PURPLE, WHITE), _t(top_plain, PURPLE), 5),
                    (_c(col_rolls, RED), _t(top_plain, WHITE), 5.5)],
    "health": [(_c(col_shampoo, TEAL, WHITE), _t(top_bottles, WHITE), 4),
               (_c(col_small_boxes, WHITE, BLUE), _t(top_boxes, WHITE), 3),
               (_c(col_shampoo, PINK, (240, 230, 240)), _t(top_bottles, (240, 230, 240)), 4),
               (_c(col_small_boxes, (230, 230, 240), RED), _t(top_boxes, WHITE), 3)],
    "health_b": [(_c(col_shampoo, (60, 60, 70), SILVER), _t(top_bottles, SILVER), 4),
                 (_c(col_shampoo, (240, 240, 236), GREEN), _t(top_bottles, GREEN), 3.5),
                 (_c(col_small_boxes, PURPLE, WHITE), _t(top_boxes, PURPLE), 3),
                 (_c(col_shampoo, YELLOW, WHITE), _t(top_bottles, WHITE), 4)],
    # the candy rack and the bakery
    "candy": [(_c(col_candy, RED), _t(top_plain, BROWN), 3),
              (_c(col_candy, BLUE), _t(top_plain, BROWN), 3),
              (_c(col_candy, YELLOW), _t(top_plain, BROWN), 3),
              (_c(col_candy, GREEN), _t(top_plain, BROWN), 3)],
    "bakery": [(_c(col_bread, (196, 130, 60)), _t(top_bread, (204, 140, 66)), 2.5),
               (_c(col_bread, (150, 92, 44)), _t(top_bread, (160, 100, 50)), 2.5),
               (_c(col_pastry, "croissant"), _t(top_pastry, "croissant"), 1.5),
               (_c(col_bread, (214, 170, 100)), _t(top_bread, (220, 178, 108)), 2)],
    "bakery_b": [(_c(col_pastry, "donut"), _t(top_pastry, "donut"), 1.5),
                 (_c(col_pastry, "croissant"), _t(top_pastry, "croissant"), 1.5),
                 (_c(col_pastry, "donut_choc"), _t(top_pastry, "donut_choc"), 1.5),
                 (_c(col_bread, (196, 130, 60)), _t(top_bread, (204, 140, 66)), 2)],
}


def stock_sheets(kind):
    cols = [(lambda px, x0, rng, f=f, h=h: f(px, x0, rng, int(round(h * 2))), t)
            for f, t, h in STOCK[kind]]
    return product_sheets(hash_seed(kind), cols)


def hash_seed(text):
    return sum((i + 1) * ord(c) for i, c in enumerate(text))


# --- surfaces ---------------------------------------------------------------------------
def glass(alpha=60, tint=(214, 234, 244), frost=False, size=16):
    """Clear glass: faint blue-white with a streak of reflection; frosted, whiter and more
    opaque, the frost thickest at the edges."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            s = (x + y) % 16
            a = alpha
            c = tint
            if s in (5, 6):
                c, a = (244, 250, 252), alpha + 30
            elif s == 12:
                a = alpha + 14
            if frost:
                edge = min(x, y, size - 1 - x, size - 1 - y)
                a += max(0, 60 - edge * 22)
                c = shade(c, 1.03)
            px[x, y] = tuple(clamp(c)) + (min(255, a),)
    return img


def grille(size=16):
    """A refrigerated case's kick grille: black louvres."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            col = (22, 22, 24) if y % 2 else (58, 58, 62)
            px[x, y] = col + (255,)
    return img


def header(text, colour, lit, size=64):
    """A lit header sign across the top of a case, its word in white on the colour; unlit, the
    same darker. Drawn in the top rows of a 64 px texture; the model maps just those."""
    img = blank(size)
    px = img.load()
    k = 1.0 if lit else 0.45
    for y in range(size):
        for x in range(size):
            col = shade(colour, k * (1.08 if y < 2 else 1.0))
            px[x, y] = col + (255,)
    LS.draw_text_centred(img, text, 32, 2, shade((255, 255, 255), k if lit else 0.6))
    return img


def pegboard(base, size=16):
    """Gondola back: powder-coated steel pegboard, a hole every two texels."""
    img = noisy(base, 701, size, 0.02)
    px = img.load()
    for y in range(1, size, 2):
        for x in range(1, size, 2):
            px[x, y] = shade(base, 0.55) + (255,)
    return img


def price_strip(size=16):
    """A shelf edge's price strip: a white channel with yellow and white tags on it."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            col = (236, 236, 232)
            if x % 8 in (1, 2, 3) or x % 8 == 5:
                col = (250, 220, 60) if x % 8 != 5 else (250, 250, 250)
                if y in (6, 9) and x % 8 in (1, 2):
                    col = (40, 40, 40)
            if y in (0, 15):
                col = (170, 172, 176)
            px[x, y] = col + (255,)
    return img


def belt(frame, size=32):
    """A frame of the checkout belt: dark rubber with a ridge every four texels (an eighth of a
    metre), moved a texel a frame along u so the belt runs towards the scanner."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            r = (x + frame) % 4
            col = (32, 32, 34) if r else (58, 58, 62)
            if r == 1:
                col = (24, 24, 26)
            px[x, y] = col + (255,)
    return img


def belt_strip():
    """The belt's four frames stacked, animated by its .mcmeta."""
    img = Image.new("RGBA", (32, 128))
    for f in range(4):
        img.paste(belt(3 - f), (0, 32 * f))
    return img


def scanner_glass(vertical=False, size=16):
    """A scanner's window: dark glass with the red crossing lines of its laser."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            col = (26, 30, 34)
            if (x - y) % 16 == 0 or (x + y) % 16 == 15 or (vertical and y == 8):
                col = (220, 40, 40)
            if x in (0, size - 1) or y in (0, size - 1):
                col = (70, 72, 76)
            px[x, y] = col + (255,)
    return img


def ui_screen(kind, size=16):
    """A lit touchscreen: a POS till's grid of item keys, or a self-checkout's welcome."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            col = (230, 234, 240)
            if kind == "pos":
                if y < 2:
                    col = (40, 90, 170)
                elif x >= 11:
                    col = (250, 250, 250) if y % 3 else (200, 204, 210)
                    if y >= 13:
                        col = (60, 170, 80)
                else:
                    cell = ((x // 3) + (y // 3)) % 4
                    col = ((236, 120, 60), (60, 140, 220), (240, 200, 60), (120, 190, 90))[cell]
                    if x % 3 == 2 or y % 3 == 1:
                        col = (30, 34, 40)
            elif kind == "kiosk":
                if y < 3:
                    col = (30, 110, 70)
                elif 9 <= y <= 12 and 3 <= x <= 12:
                    col = (60, 180, 90)
                elif y in (5, 6) and 2 <= x <= 13 and x % 2 == 0:
                    col = (60, 64, 70)
            elif kind == "terminal":
                if y < 6:
                    col = (120, 190, 220)
                    if y in (2, 3) and 3 <= x <= 12 and x % 2:
                        col = (30, 60, 90)
                else:
                    col = (40, 40, 44)
                    if (x % 4 in (1, 2)) and (y % 3 in (1,)) and y < 15:
                        col = (190, 192, 196)
                    if y >= 13 and x % 4 in (1, 2):
                        col = ((200, 50, 50), (240, 200, 40), (60, 170, 80), (60, 170, 80))[x // 4]
            px[x, y] = col + (255,)
    return img


def keypad_brass(size=16):
    """An old register's keyboard from above: rows of round keys in a brass bed."""
    img = noisy((176, 138, 64), 721, size, 0.05)
    px = img.load()
    for y in range(1, size, 3):
        for x in range(1, size, 3):
            px[x, y] = (236, 232, 220, 255)
            px[x + 1 if x + 1 < size else x, y] = (200, 196, 186, 255)
    return img


def register_flags(size=16):
    """The old register's pop-up amount flags behind their glass: white cards with figures."""
    img = noisy((176, 138, 64), 722, size, 0.05)
    px = img.load()
    for x in range(2, 14):
        for y in range(4, 11):
            px[x, y] = (240, 236, 222, 255)
    LS.draw_text(img, "125", 2, 5, (30, 30, 30))
    return img


def aisle_face(n, size=32):
    """An aisle sign's face: white on the store's blue, AISLE over the number, a white keyline.
    Drawn in rows 7 to 24 (the panel's face), the rest left blue."""
    blue = (32, 84, 170)
    img = noisy(blue, 730 + n, size, 0.02)
    px = img.load()
    for x in range(size):
        for y in (7, 24):
            px[x, y] = (236, 240, 246, 255)
    for y in range(7, 25):
        px[0, y] = (236, 240, 246, 255)
        px[size - 1, y] = (236, 240, 246, 255)
    LS.draw_text_centred(img, "AISLE", 16, 9, (240, 244, 250))
    LS.draw_text_centred(img, str(n), 16.5, 13, (255, 255, 255), scale=2)
    return img


def label_text(lines, bg, fg, size=32, top=2):
    """A sign or label: lines of the pixel font centred on a flat colour."""
    img = noisy(bg, hash_seed("".join(lines)), size, 0.02)
    y = top
    for line in lines:
        LS.draw_text_centred(img, line, size / 2.0, y, fg)
        y += 7
    return img


def wire_mesh(colour, size=32):
    """Wire mesh for the carts and the candy rack: a wire every third texel (3 cm), holes
    between."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if x % 3 == 0 or y % 3 == 0:
                k = 1.15 if (x + y) % 2 else 0.95
                px[x, y] = shade(colour, k) + (255,)
    return img


def magazine(seed, masthead, size=16):
    """A magazine cover: a coloured masthead with a white name bar, a picture, cover lines."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    photo = [rng.choice(((200, 150, 120), (90, 140, 200), (80, 160, 90), (220, 190, 150),
                         (150, 100, 160))) for _ in range(3)]
    for y in range(size):
        for x in range(size):
            if y < 4:
                col = masthead
                if y in (1, 2) and 2 <= x <= 12 and x % 3 != 1:
                    col = WHITE
            else:
                col = photo[(x // 6 + y // 5) % 3]
                col = shade(col, 1.0 + 0.1 * math.sin(x * 0.7 + y * 0.4))
                if x < 6 and y in (7, 10, 13):
                    col = WHITE
            px[x, y] = tuple(clamp(col)) + (255,)
    return img


def bulk(colours, seed, size=16, speck=0.0):
    """What a bulk bin holds seen through its clear front: nuts, beans, sweets, oats -- a heap of
    two-texel pieces in a few colours."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = shade(colours[0], 0.75) + (255,)
    for y in range(0, size, 2):
        off = rng.randrange(2)
        for x in range(off - 1, size, 2):
            c = rng.choice(colours)
            for dy in range(2):
                for dx in range(2):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < size and 0 <= yy < size:
                        k = 1.12 if dy == 0 and dx == 0 else (0.85 if dy and dx else 1.0)
                        px[xx, yy] = shade(c, k * (1 + rng.uniform(-0.04, 0.04))) + (255,)
    return img


def dial(size=16):
    """A produce scale's dial: white face, a ring of marks, a red needle."""
    def draw(r, a):
        if r > 0.9:
            return (60, 62, 66)
        col = (246, 246, 242)
        if 0.72 < r < 0.86 and int((a + math.pi) / (2 * math.pi) * 24) % 2 == 0:
            col = (40, 40, 44)
        if abs(math.sin(a - 0.9)) < 0.18 and math.cos(a - 0.9) > 0 and r < 0.75:
            col = (210, 40, 40)
        return col
    img = A.disc(size, draw)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if px[x, y][3] == 0:
                px[x, y] = (0, 0, 0, 0)
    return img


def eas_panel(size=16):
    """A security gate's panel: pale grey acrylic with the antenna loop showing in it and the
    (invented) maker's green stripe."""
    img = noisy((226, 230, 234), 716, size, 0.01)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if (x in (2, 13) and 2 <= y <= 13) or (y in (2, 13) and 2 <= x <= 13):
                px[x, y] = (150, 156, 164, 255)
            if y == 8 and 3 <= x <= 12:
                px[x, y] = (60, 170, 110, 255)
    return img


def carrier_bag(size=16):
    """A white carrier bag: plastic with soft folds, a printed stripe."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = 0.92 + 0.08 * math.sin(x * 1.3) + 0.03 * math.sin(y * 0.8)
            col = (244, 244, 240)
            if y in (9, 10):
                col = (40, 120, 200)
            px[x, y] = shade(col, k) + (255,)
    return img


def slotted(base, seed, size=16):
    """A plastic basket's side: the colour with a grid of slots."""
    img = noisy(base, seed, size, 0.03)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if y % 4 in (1, 2) and x % 3 == 1:
                px[x, y] = (0, 0, 0, 0)
            if y % 4 == 0:
                px[x, y] = shade(base, 1.18) + (255,)
    return img


def return_front(size=32):
    """The bottle return machine's face: its name over the round intake, a slot and a button."""
    body = (60, 132, 86)
    img = noisy(body, 741, size, 0.02)
    px = img.load()
    LS.draw_text_centred(img, "BOTTLES", 16, 2, (250, 250, 250))
    LS.draw_text_centred(img, "CANS", 16, 9, (250, 250, 250))
    for y in range(size):
        for x in range(size):
            d = math.hypot(x + 0.5 - 16, y + 0.5 - 22)
            if d < 6.5:
                px[x, y] = (40, 42, 44, 255) if d > 5 else (10, 10, 12, 255)
    return img


def ice_cream_pans(size=32):
    """An ice cream cabinet's well from above: eight pans of flavours, two rows of four, each
    scooped into and rimmed in steel."""
    flavours = [(246, 214, 222), (110, 66, 40), (250, 244, 214), (168, 220, 170), (240, 180, 90),
                (190, 40, 70), (222, 190, 140), (120, 180, 230)]
    rng = random.Random(751)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            i = (x // 8) + 4 * (y // 16)
            col = flavours[i]
            if x % 8 == 0 or y % 16 in (0, 15):
                col = (180, 184, 188)
            else:
                col = shade(col, 1.0 + rng.uniform(-0.08, 0.08)
                            - (0.12 if (x % 8 in (3, 4) and y % 16 in (6, 7, 8)) else 0))
            px[x, y] = col + (255,)
    return img


def deli_trays(size=32):
    """A deli case's deck from above: trays of sliced meats, a ham, salads and a wheel of
    cheese, each with a price flag."""
    rng = random.Random(761)
    trays = [((224, 140, 140), (210, 110, 110)), ((180, 60, 60), (150, 40, 50)),
             ((240, 214, 170), (230, 190, 130)), ((120, 170, 90), (230, 220, 180)),
             ((238, 200, 80), (226, 180, 60)), ((200, 110, 90), (240, 170, 150)),
             ((230, 230, 200), (140, 190, 90)), ((170, 90, 70), (210, 140, 110))]
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            i = (x // 8) + 4 * (y // 16)
            a, b = trays[i]
            col = a if (x + y // 2) % 3 else b
            col = shade(col, 1.0 + rng.uniform(-0.05, 0.05))
            if x % 8 == 0 or y % 16 in (0, 15):
                col = (236, 236, 232)
            if y % 16 == 1 and x % 8 in (5, 6):
                col = (250, 250, 250)
            px[x, y] = col + (255,)
    return img


TEXTURES = {
    "glass_clear": lambda: glass(),
    "glass_frost": lambda: glass(96, (226, 238, 246), frost=True),
    "grille": grille,
    "pegboard": lambda: pegboard((226, 226, 222)),
    "price_strip": price_strip,
    "scanner_top": scanner_glass,
    "scanner_window": lambda: scanner_glass(True),
    "screen_pos": lambda: ui_screen("pos"),
    "screen_kiosk": lambda: ui_screen("kiosk"),
    "screen_terminal": lambda: ui_screen("terminal"),
    "keys_brass": keypad_brass,
    "register_flags": register_flags,
    "brass": lambda: K.brushed((188, 150, 72), 723),
    "brass_dark": lambda: K.brushed((140, 108, 48), 724),
    "case_black": lambda: noisy((40, 40, 44), 702, 16, 0.025),
    "case_white": lambda: noisy((232, 234, 234), 703, 16, 0.015),
    "liner": lambda: noisy((206, 210, 214), 704, 16, 0.02),
    "liner_dark": lambda: noisy((70, 72, 76), 705, 16, 0.02),
    "steel_white": lambda: R.metal((226, 226, 222), 706),
    "steel_grey": lambda: R.metal((120, 122, 126), 707),
    "plastic_grey": lambda: noisy((168, 170, 172), 708, 16, 0.02),
    "plastic_red": lambda: noisy((196, 40, 36), 709, 16, 0.025),
    "plastic_blue": lambda: noisy((40, 90, 180), 710, 16, 0.025),
    "plastic_green": lambda: noisy((50, 150, 70), 711, 16, 0.025),
    "rubber": lambda: noisy((30, 30, 32), 712, 16, 0.03),
    "bumper": lambda: noisy((22, 22, 24), 713, 16, 0.02),
    "mesh_chrome": lambda: wire_mesh((196, 200, 206)),
    "mesh_black": lambda: wire_mesh((50, 50, 54)),
    "laminate_blue": lambda: R.laminate((40, 90, 170), 714),
    "laminate_red": lambda: R.laminate((176, 40, 40), 715),
    "eas_panel": eas_panel,
    "carrier_bag": carrier_bag,
    "basket_red": lambda: slotted((196, 40, 36), 717),
    "basket_green": lambda: slotted((50, 150, 70), 718),
    "scale_dial": dial,
    "ice_cream": ice_cream_pans,
    "deli_trays": deli_trays,
    "return_front": return_front,
    "corral_sign": lambda: label_text(["CART", "RETURN"], (32, 84, 170), (250, 250, 250),
                                      top=10),
    "basket_sign": lambda: label_text(["BASKETS"], (196, 40, 36), (250, 250, 250), top=13),
    "service_sign": lambda: label_text(["CUSTOMER", "SERVICE"], (32, 84, 170), (250, 250, 250),
                                       top=10),
    "service_sign_red": lambda: label_text(["CUSTOMER", "SERVICE"], (176, 40, 40),
                                           (250, 250, 250), top=10),
    "price_card": lambda: label_text(["1.99"], (40, 44, 42), (240, 240, 232), size=16, top=5),
    "header_cold_on": lambda: header("ICE COLD DRINKS", (30, 110, 190), True),
    "header_cold_off": lambda: header("ICE COLD DRINKS", (30, 110, 190), False),
    "header_frozen_on": lambda: header("FROZEN FOODS", (60, 150, 200), True),
    "header_frozen_off": lambda: header("FROZEN FOODS", (60, 150, 200), False),
    "header_dairy_on": lambda: header("FRESH DAIRY", (40, 120, 60), True),
    "header_dairy_off": lambda: header("FRESH DAIRY", (40, 120, 60), False),
    "mag_a": lambda: magazine(781, (200, 40, 40)),
    "mag_b": lambda: magazine(782, (40, 90, 180)),
    "mag_c": lambda: magazine(783, (30, 30, 34)),
    "mag_d": lambda: magazine(784, (230, 180, 30)),
    "bulk_almond": lambda: bulk([(170, 110, 64), (150, 94, 52), (186, 126, 76)], 791),
    "bulk_peanut": lambda: bulk([(210, 170, 110), (196, 150, 96), (226, 190, 130)], 792),
    "bulk_trail": lambda: bulk([(170, 110, 64), (80, 50, 36), (230, 200, 120), (150, 40, 50)],
                               793),
    "bulk_oats": lambda: bulk([(222, 200, 150), (206, 184, 136), (236, 220, 180)], 794),
    "bulk_jelly": lambda: bulk([RED, YELLOW, GREEN, ORANGE, PURPLE, PINK], 795),
    "bulk_gummy": lambda: bulk([(230, 60, 60), (250, 200, 40), (90, 200, 90), (250, 140, 40)],
                               796),
    "bulk_choc": lambda: bulk([(90, 56, 36), (110, 70, 44), (70, 44, 30)], 797),
    "bulk_mint": lambda: bulk([(240, 240, 236), (170, 226, 196), (240, 200, 210)], 798),
}
for _kind in STOCK:
    TEXTURES["stock_%s" % _kind] = (lambda k=_kind: stock_sheets(k)[0])
    TEXTURES["stock_%s_top" % _kind] = (lambda k=_kind: stock_sheets(k)[1])
for _n in range(1, 17):
    TEXTURES["aisle_%d" % _n] = (lambda n=_n: aisle_face(n))
# Animated textures: (strip drawer, mcmeta)
ANIMATED = {"belt": (belt_strip, {"animation": {"frametime": 2}})}

GLOW_ON, GLOW_OFF = T("lens_on"), T("lens_off")

# ------------------------------------------------------------------------------------------
# Default textures and geometry
# ------------------------------------------------------------------------------------------
DEFAULT_TEX = dict(O.DEFAULT_TEX)
DEFAULT_TEX.update({
    "shell": MT("case_black"), "liner": MT("liner"), "frame": MT("case_black"),
    "glass": MT("glass_clear"), "grille": MT("grille"), "glow": GLOW_ON,
    "header": MT("header_cold_on"), "shelf": MT("steel_white"), "strip": MT("price_strip"),
    "stock_a": MT("stock_drinks"), "stock_a_top": MT("stock_drinks_top"),
    "stock_b": MT("stock_drinks_b"), "stock_b_top": MT("stock_drinks_b_top"),
    "peg": MT("pegboard"), "kick": MT("steel_grey"), "steel": T("stainless"),
    "chrome": T("chrome"), "bumper": MT("bumper"), "belt": MT("belt"),
    "scanner": MT("scanner_top"), "scanner_v": MT("scanner_window"), "case": MT("case_black"),
    "screen": MT("screen_pos"), "brass": MT("brass"), "brass_dark": MT("brass_dark"),
    "keys": MT("keys_brass"), "flags": MT("register_flags"), "bag": MT("carrier_bag"),
    "terminal": MT("screen_terminal"), "mesh": MT("mesh_chrome"), "plastic": MT("plastic_red"),
    "rubber": MT("rubber"), "sign": MT("corral_sign"), "basket": MT("basket_red"),
    "panel": MT("eas_panel"), "face": MT("aisle_1"), "mag_a": MT("mag_a"), "mag_b": MT("mag_b"),
    "mag_c": MT("mag_c"), "mag_d": MT("mag_d"), "bulk_1": MT("bulk_almond"),
    "bulk_2": MT("bulk_peanut"), "bulk_3": MT("bulk_trail"), "bulk_4": MT("bulk_oats"),
    "clear": MT("glass_clear"), "label": OT("label_card"), "produce": PRODUCE_TEX % "bed_apple_red",
    "price": MT("price_card"), "dial": MT("scale_dial"), "pans": MT("ice_cream"),
    "trays": MT("deli_trays"), "front": MT("return_front"), "tubs": MT("ice_cream"),
})


def geometry(specs, particle, display=None, centre=False, dy=0.0):
    """A base model: the elements built where they stand (so their UVs fit there), then moved
    down by dy -- or, for an item, moved so the piece's middle is the block's."""
    built = [R.build(s) for s in specs]
    move = [0.0, -dy, 0.0]
    if centre:
        lo = [min(e["from"][i] for e in built) for i in range(3)]
        hi = [max(e["to"][i] for e in built) for i in range(3)]
        move = [8 - (lo[i] + hi[i]) / 2.0 for i in range(3)]
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


def stock_row(x0, x1, y0, zf, zb, kind_heights, seed, key="stock_a", pattern=None,
              faces=("north", "up", "east", "west")):
    """Products standing on a shelf at y0 between x0 and x1, from the front zf back to zb: groups
    four pixels wide (one product column of the key's sheet each), each as tall as that
    product, a front set a little in or out so the groups read apart. kind_heights are the four
    columns' heights in px; pattern picks which column each group shows. A group's side on
    the block's edge is left off: an end panel covers it there, and the next block's stock
    continues it anywhere else."""
    rng = random.Random(seed)
    out = []
    x = x0
    g = 0
    while x < x1 - 0.2:
        w = min(4.0, x1 - x)
        c = pattern[g % len(pattern)] if pattern else rng.randrange(4)
        h = kind_heights[c]
        f = zf + rng.choice((0.0, 0.25, 0.5))
        depth = min(16.0, zb - f)
        u0 = 4 * c + (4 - w)
        uv = {"north": [u0, 16 - h, u0 + w, 16], "east": [4 * c, 16 - h, 4 * c + min(4, depth), 16],
              "west": [4 * c, 16 - h, 4 * c + min(4, depth), 16],
              "up": [4 * c, 0, 4 * c + w, depth]}
        sides = tuple(s for s in faces if not ((s == "west" and x <= 0.001)
                                               or (s == "east" and x + w >= 15.999)))
        uv = {k: v for k, v in uv.items() if k in sides}
        out.append(el([x, y0, f], [x + w, y0 + h, zb], key, sides, {"up": key + "_top"}, uv=uv))
        x += w
        g += 1
    return out


def heights(kind):
    return [h for _f, _t, h in STOCK[kind]]


def curved_glass(x0, x1, y0, z0, segs, tex="glass", top_to=None):
    """A service case's curved front glass: panes from (y0, z0) at the front edge, each segs
    (length, lean in degrees back from upright) leaning further back than the last, and a flat
    top pane from where they end back to top_to. JSON elements turn only by 22.5 degrees, so
    three facets make the curve."""
    out = []
    y, z = y0, z0
    for length, lean in segs:
        rot = ("x", lean, [8, y, z]) if lean else None
        out.append(el([x0, y, z], [x1, y + length, z + 0.2], tex, ("north", "south"), rot=rot))
        a = math.radians(lean)
        y += length * math.cos(a)
        z += length * math.sin(a)
    if top_to:
        out.append(el([x0, y - 0.2, z], [x1, y, top_to], tex, ("up", "down")))
    return out, (round(y, 3), round(z, 3))


def split_tall(specs, cut=16.0):
    """The part of each element below cut, and the part above it moved down a block, keeping an
    explicitly mapped face's texture where it was (its v range cut in proportion). A turned
    element goes whole to the half its middle is in."""
    lower, upper = [], []
    for s in specs:
        y0, y1 = s["from"][1], s["to"][1]
        if s["rot"]:
            if (y0 + y1) / 2 < cut:
                lower.append(s)
            else:
                moved = copy.deepcopy(s)
                moved["from"][1] -= cut
                moved["to"][1] -= cut
                axis, angle, origin = moved["rot"]
                moved["rot"] = (axis, angle, [origin[0], origin[1] - cut, origin[2]])
                upper.append(moved)
            continue
        if y1 <= cut:
            lower.append(s)
            continue
        if y0 >= cut:
            u = copy.deepcopy(s)
            u["from"][1] -= cut
            u["to"][1] -= cut
            upper.append(u)
            continue
        t = (cut - y0) / (y1 - y0)
        a, b = copy.deepcopy(s), copy.deepcopy(s)
        a["to"][1] = cut
        a["faces"] = tuple(f for f in s["faces"] if f != "up")
        b["from"][1] = 0
        b["to"][1] = y1 - cut
        b["faces"] = tuple(f for f in s["faces"] if f != "down")
        for f, uv in s["uv"].items():
            if f in ("up", "down"):
                continue
            u0, v0, u1, v1 = uv
            vm = v1 + (v0 - v1) * t
            a["uv"][f] = [u0, vm, u1, v1]
            b["uv"][f] = [u0, v0, u1, vm]
        for part in (a, b):
            for f in list(part["uv"]):
                if f not in part["faces"]:
                    del part["uv"][f]
        lower.append(a)
        upper.append(b)
    return lower, upper


# ------------------------------------------------------------------------------------------
# Refrigerated displays
# ------------------------------------------------------------------------------------------
def header_el(x0, x1, y0, y1, z):
    """A lit header sign's face: the top rows of its 64 px texture, where the word is."""
    return el([x0, y0, z - 0.1], [x1, y1, z], "header", ("north",),
              uv={"north": [0, 0, 16, 2.5]})


def strip_el(x0, x1, y0, y1, z0, z1):
    return el([x0, y0, z0], [x1, y1, z1], "strip", ("north", "up", "down"),
              uv={"north": [0, 4, 16, 8]})


def reach_in(stock_a, stock_b):
    """A glass-door reach-in cooler or freezer, 2 m tall: a door of glass in a black frame over
    four shelves of stock, lit by LED strips on the mullions, a lit header sign above the door
    and a kick grille below. The mullion is shared: each block draws half of it at either side,
    so a line of doors has one between each pair."""
    body = [el([0, 0, 15.75], [16, 32, 16], "shell", ("south",)),
            el([0, 3, 15.25], [16, 29.5, 15.75], "liner", ("north",)),
            el([0, 0, 2.25], [16, 3, 15.25], "liner", ("north", "up"), {"north": "grille"}),
            el([0, 29.5, 2], [16, 32, 15.75], "shell", ("north", "up", "down"), {"down": "liner"}),
            header_el(0.75, 15.25, 30, 31.75, 2),
            el([0, 3, 2], [0.375, 29.5, 2.75], "frame", ("north", "east")),
            el([15.625, 3, 2], [16, 29.5, 2.75], "frame", ("north", "west")),
            el([0.5, 3, 2], [15.5, 4, 2.5], "frame", NO_BACK),
            el([0.5, 28.5, 2], [15.5, 29.5, 2.5], "frame", NO_BACK),
            el([0.5, 4, 2], [1.25, 28.5, 2.5], "frame", ("north", "east", "west")),
            el([14.75, 4, 2], [15.5, 28.5, 2.5], "frame", ("north", "east", "west")),
            el([1.25, 4, 2.2], [14.75, 28.5, 2.3], "glass", ("north",)),
            el([13.1, 9, 0.9], [13.7, 23, 1.5], "chrome"),
            el([13.1, 9.5, 1.5], [13.7, 10.3, 2], "chrome", SIDES),
            el([13.1, 21.7, 1.5], [13.7, 22.5, 2], "chrome", SIDES),
            el([0.9, 4, 2.8], [1.3, 28.5, 3.1], "glow", ("north", "east")),
            el([14.7, 4, 2.8], [15.1, 28.5, 3.1], "glow", ("north", "west"))]
    levels = [3.0, 9.5, 16.0, 22.5]
    for i, y in enumerate(levels):
        if i:
            body += [el([0.5, y - 0.4, 3.4], [15.5, y, 15.25], "shelf", ("up", "down")),
                     strip_el(0.5, 15.5, y - 0.9, y, 3.1, 3.4)]
        key = "stock_a" if i % 2 == 0 else "stock_b"
        kind = stock_a if key == "stock_a" else stock_b
        body += stock_row(0.75, 15.25, y, 3.75, 15.25, heights(kind), 100 + i, key)
    end = [el([0, 0, 2], [0.5, 32, 16], "shell", ("west", "north", "up", "down"))]
    return body, end


def multideck(stock_a, stock_b):
    """An open multideck dairy case, 2 m tall: a front riser with its bumper and air grille, the
    deck and three shelves stepping back as they rise, each with its price strip and a light
    under its lip, a canopy with a lit header and a light over the front."""
    body = [el([0, 0, 15.75], [16, 32, 16], "shell", ("south",)),
            el([0, 8, 15.25], [16, 29.5, 15.75], "liner", ("north",)),
            el([0, 0, 1], [16, 8, 2], "shell", ("north",)),
            el([0, 3, 0.5], [16, 4.5, 1], "bumper", ("north", "up", "down")),
            el([0, 7.75, 1], [16, 8, 4.25], "grille", ("up", "north")),
            el([0, 7.5, 4.25], [16, 8, 15.25], "liner", ("up",)),
            el([0, 29.5, 4], [16, 32, 15.75], "shell", ("north", "up", "down"), {"down": "liner"}),
            header_el(0, 16, 30, 31.75, 4),
            el([0.5, 29.25, 4.5], [15.5, 29.5, 6.5], "glow", ("down",))]
    body += stock_row(0.25, 15.75, 8, 4.75, 15.25, heights(stock_a), 200, "stock_a")
    for i, (y, zf) in enumerate(((13.5, 6.0), (19.0, 7.5), (24.5, 9.0))):
        body += [el([0, y - 0.5, zf], [16, y, 15.25], "shelf", ("up", "down")),
                 strip_el(0, 16, y - 1, y, zf - 0.25, zf),
                 el([0.5, y - 1.2, zf], [15.5, y - 1, zf + 0.6], "glow", ("down",))]
        key = "stock_b" if i % 2 == 0 else "stock_a"
        kind = stock_b if key == "stock_b" else stock_a
        body += stock_row(0.25, 15.75, y, zf + 0.5, 15.25, heights(kind), 201 + i, key)
    end = [el([0, 0, 0.5], [0.5, 8, 16], "shell", ("west", "north", "up")),
           el([0, 8, 9], [0.5, 29.5, 16], "shell", ("west", "north")),
           el([0.1, 8, 2], [0.4, 29.5, 9], "glass", ("west", "east")),
           el([0, 29.5, 4], [0.5, 32, 16], "shell", ("west", "north", "up", "down"))]
    return body, end


# The island freezer: a tub 0.85 m high, stocked lying flat, two sliding glass lids.
ISLAND = ([el([0, 1, 1], [16, 13, 2.5], "shell", ("north", "up", "south"), {"south": "liner"}),
           el([0, 1, 13.5], [16, 13, 15], "shell", ("north", "up", "south"), {"north": "liner"}),
           el([0, 0, 1.5], [16, 1, 14], "kick", ("north", "south")),
           el([0, 7.75, 2.5], [16, 8, 13.5], "liner", ("up",)),
           el([0, 13, 1], [16, 13.75, 2.5], "trim", ("north", "up", "south", "down")),
           el([0, 13, 13.5], [16, 13.75, 15], "trim", ("north", "up", "south", "down")),
           el([0, 13, 7.5], [16, 13.5, 8.5], "trim", ("north", "south", "up", "down")),
           el([0, 13.25, 2.5], [16, 13.4, 8], "glass", ("up", "down")),
           el([0, 13.55, 8], [16, 13.7, 13.5], "glass", ("up", "down")),
           el([6, 13.4, 6.5], [10, 13.8, 7.25], "chrome", NO_DOWN),
           el([6, 13.7, 8.75], [10, 14.1, 9.5], "chrome", NO_DOWN),
           el([0, 12.2, 2.5], [16, 12.7, 2.9], "glow", ("south", "down")),
           el([0, 12.2, 13.1], [16, 12.7, 13.5], "glow", ("north", "down"))]
          + stock_row(0.75, 15.25, 8, 3, 7.5, heights("frozen"), 300, "stock_a")
          + stock_row(0.75, 15.25, 8, 8.5, 13.25, heights("frozen_b"), 301, "stock_b"))
ISLAND_END = [el([0, 0, 1], [0.75, 13.75, 15], "shell", ("west", "north", "south", "up"))]


def service_case(base_top, segs, top_to, inside, canopy_z, deck, base_tex="shell"):
    """A service case: a base cabinet, a curved (or straight) glass front on it, a flat glass
    top back to a light canopy over the server's side, and whatever is on show inside."""
    glass_els, (gy, gz) = curved_glass(0, 16, base_top, 2.6, segs, top_to=top_to)
    body = ([el([0, 0, 2.2], [16, 1, 2.5], "kick", ("north",)),
             el([0, 0, 2.5], [16, base_top, 3.2], base_tex, ("north", "up", "south"),
                {"south": "liner"}),
             el([0, 0, 3.2], [16, deck, 15], base_tex, ("south",)),
             el([0, deck - 0.1, 3.2], [16, deck, 15], "liner", ("up",)),
             el([0, gy - 2, canopy_z], [16, gy, canopy_z + 1.5], "case",
                ("north", "south", "up", "down")),
             el([0.5, gy - 2.1, canopy_z + 0.1], [15.5, gy - 2, canopy_z + 1.4], "glow",
                ("down",))]
            + glass_els + inside)
    # the end: a panel beside the base, glass stepping back with the curve
    end = [el([0, 0, 2.5], [0.5, base_top + 0.5, 16], base_tex, ("west", "north", "up", "south"))]
    y, z = base_top, 2.6
    for length, lean in segs:
        a = math.radians(lean)
        y1 = y + length * math.cos(a)
        z_next = z + length * math.sin(a)
        end.append(el([0.1, y, z], [0.4, y1, canopy_z], "glass", ("west", "east")))
        y, z = y1, z_next
    return body, end, (gy, gz)


ICE_CREAM_INSIDE = [el([0, 9, 3.2], [16, 9.1, 11], "pans", ("up",), uv={"up": [0, 0, 16, 16]}),
                    el([0, 11.5, 11], [16, 12, 16], "steel", ("up", "north", "south", "down"))]
ICE_CREAM, ICE_CREAM_END, _ = service_case(11, [(2.5, 0), (3.0, 22.5), (3.5, 45)], 10.5,
                                           ICE_CREAM_INSIDE, 10.5, 8.9)
DELI_INSIDE = [el([0, 8.5, 3.2], [16, 8.6, 12], "trays", ("up",), uv={"up": [0, 0, 16, 16]}),
               el([0, 12.8, 6.5], [16, 13, 12], "glass", ("up", "down")),
               el([0, 13, 6.7], [16, 13.1, 11.8], "trays", ("up",), uv={"up": [0, 8, 16, 16]}),
               el([0, 12, 12.5], [16, 12.5, 16], "steel", ("up", "north", "south", "down"))]
DELI, DELI_END, _ = service_case(9, [(4.0, 0), (4.0, 22.5), (3.0, 45)], 11, DELI_INSIDE, 11,
                                 8.4)
BAKERY_INSIDE = (stock_row(0, 16, 9, 3.5, 13, heights("bakery"), 400, "stock_a")
                 + [el([0, 13.3, 3.5], [16, 13.5, 13], "glass", ("up", "down"))]
                 + stock_row(0, 16, 13.5, 4, 12.5, heights("bakery_b"), 401, "stock_b")
                 + [el([0, 9, 13.5], [16, 16.5, 13.7], "glass", ("north", "south")),
                    el([0, 9, 13.7], [16, 9.5, 16], "wood", ("up", "north", "south"))])
BAKERY, BAKERY_END, _ = service_case(9, [(9.5, 0)], 12.5, BAKERY_INSIDE, 12.5, 8.9,
                                     base_tex="wood")

# ------------------------------------------------------------------------------------------
# Gondola shelving: pegboard back at z 15, a shelf at the half, a base deck at the bottom of a
# stack; stock is two rows, one on the shelf and one below it
# ------------------------------------------------------------------------------------------
GONDOLA_PEG = [el([0, 0, 15], [16, 16, 15.75], "peg", ("north", "south"), {"south": "shelf"})]
GONDOLA_SHELF = [el([0, 7.5, 7], [16, 8, 15], "shelf", ("up", "down")),
                 strip_el(0, 16, 7, 8, 6.75, 7)]
GONDOLA_DECK = [el([0, 2, 6.75], [16, 2.5, 15], "shelf", ("up",)),
                el([0, 0, 6.5], [16, 2.5, 6.75], "kick", ("north",)),
                strip_el(0, 16, 1.5, 2.5, 6.25, 6.5)]
GONDOLA_STACKED = [el([0, 0, 7], [16, 0.5, 15], "shelf", ("up", "down")),
                   strip_el(0, 16, -0.25, 0.5, 6.75, 7)]
GONDOLA_TOP = [el([0, 15.5, 14.5], [16, 16, 16], "shelf", ("north", "up", "south"))]
GONDOLA_END = [el([0, 0, 14.75], [1, 16, 16], "kick", ("west", "north", "south")),
               el([0, 0, 6.25], [0.5, 16, 14.75], "shelf", ("west", "east", "north"))]
GONDOLA_KINDS = [
    # (variant, stock above, stock below, en, de, es, sv)
    ("canned", "canned", "canned_b", "Canned Goods", "Konserven", "conservas", "konserver"),
    ("cereal", "cereal", "cereal_b", "Cereal", "Frühstücksflocken", "cereales", "flingor"),
    ("snacks", "snacks", "snacks_b", "Snacks", "Snacks", "aperitivos", "snacks"),
    ("drinks", "bottled", "bottled_b", "Bottled Drinks", "Getränke", "bebidas", "drycker"),
    ("household", "household", "household_b", "Household", "Haushaltswaren", "droguería",
     "hushåll"),
    ("health", "health", "health_b", "Health & Beauty", "Drogerie", "cuidado personal",
     "hälsa & skönhet"),
    ("empty", None, None, "Empty", "Leer", "vacía", "tom"),
]


def gondola_parts(above, below, seed):
    body = GONDOLA_PEG + GONDOLA_SHELF
    plinth = list(GONDOLA_DECK)
    stacked = list(GONDOLA_STACKED)
    if above:
        body = body + stock_row(0, 16, 8, 7.25, 15, heights(above), seed, "stock_a")
        plinth += stock_row(0, 16, 2.5, 7.0, 15, heights(below), seed + 1, "stock_b")
        stacked += stock_row(0, 16, 0.5, 7.25, 15, heights(below), seed + 2, "stock_b")
    return {"body": body, "plinth": plinth, "stacked": stacked, "top": GONDOLA_TOP,
            "left": GONDOLA_END, "right": mirror_x(GONDOLA_END)}


GONDOLA_RULES = R.faced([("body", {}), ("plinth", {"down": "false"}), ("stacked", {"down": "true"}),
                         ("top", {"up": "false"}), ("left", {"left": "false"}),
                         ("right", {"right": "false"})])

# ------------------------------------------------------------------------------------------
# Produce and bulk
# ------------------------------------------------------------------------------------------
_L = 13.5
_TILT = ("x", -22.5, [8, 8.5, 1.75])
PRODUCE_BODY = [board([0, 7.5, 1], [16, 8.5, 1.75], faces=("north", "down")),
                board([0, 7.5, 14.25], [16, 8.5, 15], faces=("south", "down")),
                R.leg([7.25, 0, 1.25], [8.75, 7.5, 2.25]),
                R.leg([7.25, 0, 13.75], [8.75, 7.5, 14.75]),
                board([0, 1.5, 2.25], [16, 2.25, 13.75], faces=("up", "north", "south")),
                el([0, 8.5, 1.75], [16, 9.25, 1.75 + _L], "wood", ("down",), rot=_TILT),
                el([0, 9.25, 1.75], [16, 10.75, 1.75 + _L], "produce", ("up",),
                   uv={"up": [0, 0, 16, 16]}, rot=_TILT),
                board([0, 8.5, 1], [16, 11, 1.75], faces=("north", "up")),
                el([6, 9.1, 0.9], [10, 10.6, 1], "price", ("north",), uv={"north": [0, 4, 16, 10]}),
                board([0, 8.5, 14.25], [16, 15.5, 15], grain="v",
                      faces=("north", "south", "up"))]
PRODUCE_END = [board([0, 8.5, 1], [0.75, 11, 15], grain="v"),
               board([0, 11, 5.5], [0.75, 13.25, 15], grain="v", faces=("west", "east", "north",
                                                                          "up")),
               board([0, 13.25, 10], [0.75, 15.5, 15], grain="v", faces=("west", "east", "north",
                                                                           "up")),
               R.leg([0.25, 0, 1], [1.5, 8.5, 2.25]),
               R.leg([0.25, 0, 13.75], [1.5, 8.5, 15])]
PRODUCE_KINDS = [
    ("apple", "bed_apple_red", "Apples", "Äpfel", "manzanas", "äpplen"),
    ("orange", "bed_orange", "Oranges", "Orangen", "naranjas", "apelsiner"),
    ("lettuce", "bed_lettuce", "Lettuce", "Salat", "lechuga", "sallad"),
    ("tomato", "bed_tomato", "Tomatoes", "Tomaten", "tomates", "tomater"),
    ("potato", "bed_potato", "Potatoes", "Kartoffeln", "patatas", "potatis"),
    ("banana", "bed_banana", "Bananas", "Bananen", "plátanos", "bananer"),
]

PRODUCE_SCALE = ([el([7.75, 12.5, 7.75], [8.25, 16, 8.25], "chrome", SIDES),
                  el([7.5, 12, 7.5], [8.5, 12.5, 8.5], "chrome"),
                  el([5, 6, 7], [11, 12, 9], "shell"),
                  el([5.3, 6.3, 6.95], [10.7, 11.7, 7], "dial", ("north",),
                     uv={"north": [0, 0, 16, 16]}),
                  el([5.3, 6.3, 9], [10.7, 11.7, 9.05], "dial", ("south",),
                     uv={"south": [0, 0, 16, 16]}),
                  el([7.75, 4.5, 7.75], [8.25, 6, 8.25], "chrome", SIDES),
                  el([4, 1, 5], [12, 1.5, 11], "steel"),
                  el([4, 1.5, 5], [12, 2.5, 5.5], "steel", NO_DOWN),
                  el([4, 1.5, 10.5], [12, 2.5, 11], "steel", NO_DOWN),
                  el([4, 1.5, 5.5], [4.5, 2.5, 10.5], "steel", NO_DOWN),
                  el([11.5, 1.5, 5.5], [12, 2.5, 10.5], "steel", NO_DOWN)]
                 + [el([x, 2.5, z], [x + 0.25, 4.5, z + 0.25], "chrome", SIDES)
                    for x, z in ((4.4, 5.4), (11.35, 5.4), (4.4, 10.35), (11.35, 10.35))]
                 + [el([4.4, 4.5, 5.4], [11.6, 4.75, 10.6], "chrome", ("up", "down"))])


def bulk_body():
    out = [board([0, 0, 14.5], [16, 16, 15.25], grain="v", faces=("north", "south")),
           board([0, 15.25, 6.5], [16, 16, 15.25], faces=("north", "up", "down")),
           board([0, 0, 6.5], [16, 1.5, 14.5], faces=("north", "up")),
           board([0, 8, 6.5], [16, 8.5, 14.5], faces=("north", "up", "down"))]
    for yb, keys in ((1.5, ("bulk_1", "bulk_2")), (8.5, ("bulk_3", "bulk_4"))):
        for x0, key in zip((0.75, 8.25), keys):
            out += [el([x0 + 0.3, yb + 1.3, 7.5], [x0 + 6.7, yb + 5.2, 13.8], key,
                       ("north", "up", "east", "west")),
                    el([x0, yb + 1, 7], [x0 + 7, yb + 6.25, 14], "clear",
                       ("north", "east", "west")),
                    el([x0, yb + 6.25, 7], [x0 + 7, yb + 6.5, 14], "case", NO_BACK),
                    el([x0 + 2.5, yb, 6.25], [x0 + 4.5, yb + 1, 7.25], "case", NO_BACK),
                    el([x0 + 4.75, yb + 0.25, 6], [x0 + 6, yb + 0.75, 7], "chrome", NO_BACK),
                    ALL_UV(el([x0 + 0.5, yb + 1.2, 6.9], [x0 + 2.5, yb + 2.1, 7], "label",
                              ("north",)), ["north"])]
    return out


BULK_BODY = bulk_body()
BULK_END = [board([0, 0, 6.5], [0.75, 16, 15.25], grain="v")]
BULK_KINDS = [
    ("nuts", ("bulk_almond", "bulk_peanut", "bulk_trail", "bulk_oats"),
     "Nuts & Grains", "Nüsse & Getreide", "frutos secos y cereales", "nötter & gryn"),
    ("candy", ("bulk_jelly", "bulk_gummy", "bulk_choc", "bulk_mint"),
     "Candy", "Süßigkeiten", "golosinas", "godis"),
]

# ------------------------------------------------------------------------------------------
# Checkout: a cabinet whose top is a belt, a scanner or a bagging well
# ------------------------------------------------------------------------------------------


def cabinet(top):
    return [board([0, 1, 3], [16, top - 0.75, 3.75], grain="v", faces=("north",)),
            el([0, 0, 3.5], [16, 1, 13.5], "kick", ("north", "south")),
            board([0, 1, 13.25], [16, top - 0.75, 14], grain="v", faces=("south",)),
            el([0, top - 1.75, 2.4], [16, top - 0.75, 3], "bumper", ("north", "up", "down"))]


def cabinet_end(top):
    return [board([0, 0, 2.25], [0.75, top, 14], grain="v", faces=("west", "north", "south",
                                                                    "up"))]


CHECKOUT_BELT = (cabinet(14.5)
                 + [el([0, 13.75, 2.25], [16, 14.5, 4], "steel", ("north", "up", "down")),
                    el([0, 13.75, 12.25], [16, 14.5, 14], "steel", ("south", "up", "down")),
                    el([0, 13.75, 4], [16, 14.25, 12.25], "belt", ("up",)),
                    el([4, 14.25, 4.5], [4.75, 14.9, 11.75], "plastic", NO_DOWN)])
CHECKOUT_SCANNER = (cabinet(14.5)
                    + [el([0, 13.75, 2.25], [16, 14.5, 14], "steel", ("north", "south", "up",
                                                                      "down")),
                       el([5, 14.5, 6.5], [11, 14.52, 12], "scanner", ("up",),
                          uv={"up": [0, 0, 16, 16]}),
                       el([4.5, 14.5, 3.5], [11.5, 19, 6], "case", NO_DOWN),
                       el([5.25, 15, 6], [10.75, 18.5, 6.05], "scanner_v", ("south",),
                          uv={"south": [0, 0, 16, 16]})])
CHECKOUT_BAGGING = (cabinet(12)
                    + [el([0, 11.25, 2.25], [16, 12, 14], "steel", ("north", "south", "up",
                                                                    "down"))]
                    + [el([x, 12, z], [x + 0.5, 17.5, z + 0.5], "chrome", SIDES)
                       for x in (1, 14.5) for z in (6.5, 10)]
                    + [el([1, 17.5, z], [15, 18, z + 0.5], "chrome") for z in (6.5, 10)]
                    + [el([2, 12.5, 6.75], [7.5, 17.5, 10.25], "bag"),
                       el([8.5, 12.5, 6.75], [14, 17.5, 10.25], "bag")])

POS_TERMINAL = [el([2.5, 0, 3.5], [13.5, 3, 13], "case"),
                el([3, 0.5, 3.25], [13, 2.5, 3.5], "case", NO_BACK),
                el([7.5, 1.25, 3], [8.5, 1.75, 3.25], "chrome", NO_BACK),
                el([7.25, 3, 9], [8.75, 6, 10.5], "case", SIDES),
                ALL_UV(el([3.5, 5.5, 8.25], [12.5, 11.5, 9.25], "case", ALL, {"north": "screen"},
                          rot=("x", 22.5, [8, 5.5, 8.75])), ["north"]),
                el([11.75, 3, 11.5], [12.75, 8, 12.5], "case", SIDES),
                ALL_UV(el([10.5, 8, 11.25], [14, 10, 12.75], "case", ALL, {"south": "screen"}),
                       ["south"])]
CASH_REGISTER = [el([3, 0, 4], [13, 5, 13], "brass"),
                 el([3, 0, 3.5], [13, 2.5, 4], "brass_dark", NO_BACK),
                 el([7.5, 1, 3], [8.5, 1.5, 3.5], "chrome", NO_BACK),
                 ALL_UV(el([3.5, 4, 4.5], [12.5, 5.5, 9.5], "brass", ALL, {"up": "keys"},
                           rot=("x", -22.5, [8, 4, 4.5])), ["up"]),
                 ALL_UV(el([4, 5, 9], [12, 9, 12.5], "brass", ALL, {"north": "flags"}), ["north"]),
                 el([13, 2, 7.5], [13.75, 3, 8.5], "brass_dark"),
                 el([13.75, 1, 7.75], [14.5, 3, 8.25], "brass_dark")]
RECEIPT_PRINTER = [el([5, 0, 4.5], [11, 3.5, 11.5], "case"),
                   el([5.5, 3.5, 6], [10.5, 3.6, 6.4], "chrome", ("up",)),
                   el([6, 3.5, 6.5], [10, 6, 6.7], "paper", ("north", "south", "up"),
                      rot=("x", -22.5, [8, 3.5, 6.5]))]
CARD_STAND = [el([5.5, 0, 5.5], [10.5, 0.5, 10.5], "case"),
              el([7.5, 0.5, 7.5], [8.5, 5, 8.5], "case", SIDES),
              el([7, 5, 7], [9, 5.75, 9], "case"),
              ALL_UV(el([5, 5.5, 5.5], [11, 6.75, 11], "case", ALL, {"up": "terminal"},
                        rot=("x", -22.5, [8, 5.5, 8])), ["up"])]
BAG_CAROUSEL = [el([4, 0, 4], [12, 0.5, 12], "steel"),
                el([7.5, 0.5, 7.5], [8.5, 11, 8.5], "steel", SIDES),
                el([3.5, 10.5, 7.6], [12.5, 11, 8.4], "steel"),
                el([7.6, 10.5, 3.5], [8.4, 11, 7.6], "steel", NO_BACK),
                el([7.6, 10.5, 8.4], [8.4, 11, 12.5], "steel", ("south", "east", "west", "up",
                                                                "down")),
                el([2.5, 3, 6], [5, 10.5, 10], "bag"), el([11, 3, 6], [13.5, 10.5, 10], "bag"),
                el([6, 3, 2.5], [10, 10.5, 5], "bag"), el([6, 3, 11], [10, 10.5, 13.5], "bag")]

SELF_CHECKOUT = [el([1, 0, 3], [15, 14, 14], "shell"),
                 el([1.5, 0, 2.75], [14.5, 1, 3], "kick", NO_BACK),
                 el([0.5, 14, 2.5], [15.5, 14.5, 14], "steel"),
                 el([4, 14.5, 4], [10, 14.52, 9], "scanner", ("up",), uv={"up": [0, 0, 16, 16]}),
                 el([2, 14.5, 10], [14, 17.5, 14], "shell", NO_DOWN),
                 el([4.5, 14.75, 9.95], [9.5, 16, 10], "scanner_v", ("north",),
                    uv={"north": [0, 0, 16, 16]}),
                 el([7, 17.5, 11.5], [9, 21.5, 13], "shell", SIDES),
                 ALL_UV(el([3, 21, 10.75], [13, 29, 11.75], "shell", ALL, {"north": "screen"},
                           rot=("x", 22.5, [8, 21, 11.25])), ["north"]),
                 ALL_UV(el([11.5, 14.5, 4.5], [14, 15.75, 7.5], "case", ALL, {"up": "terminal"}),
                        ["up"]),
                 el([1.5, 14.5, 12.5], [2.5, 29, 13.5], "steel", SIDES),
                 ALL_UV(el([0.75, 29, 11.75], [3.25, 31, 14.25], "shell", ALL,
                           {f: "glow" for f in SIDES}), list(SIDES))]
BOTTLE_RETURN = [el([1, 0, 3.5], [15, 28, 16], "shell"),
                 el([1.5, 0, 3.25], [14.5, 2, 3.5], "kick", NO_BACK),
                 el([0.5, 28, 3], [15.5, 29, 16], "shell"),
                 el([2, 12, 3.4], [14, 26, 3.5], "front", ("north",), uv={"north": [0, 0, 16, 16]}),
                 el([5, 9, 3.2], [11, 9.75, 3.5], "case", NO_BACK),
                 el([11.5, 9, 3.2], [12.75, 10.25, 3.5], "glow", NO_BACK)]

IMPULSE_RACK = ([el([1, 0, 5], [15, 1, 13], "case"),
                 el([1, 1, 5], [1.3, 20, 13], "mesh", ("east", "west")),
                 el([14.7, 1, 5], [15, 20, 13], "mesh", ("east", "west")),
                 el([1.3, 1, 12.7], [14.7, 20, 13], "mesh", ("north", "south")),
                 el([1.5, 19, 10], [14.5, 20.5, 12.5], "case")])
for _i in range(4):
    _y = 1 + _i * 4.5
    _zf = 5 + _i * 1.5
    IMPULSE_RACK += [el([1.3, _y, _zf], [14.7, _y + 0.5, 12.7], "case", ("north", "up", "down"))]
    IMPULSE_RACK += stock_row(1.5, 14.5, _y + 0.5, _zf + 0.25, 12.5, heights("candy"), 500 + _i,
                              "stock_a")

# ------------------------------------------------------------------------------------------
# Store fixtures
# ------------------------------------------------------------------------------------------
_CART_SIDE = [el([2.5, 7, 1], [2.75, 13.5, 13.75], "mesh", ("east", "west")),
              el([2.25, 13.5, 1.25], [2.75, 14, 14], "chrome"),
              el([3, 1.5, 1.5], [3.75, 2.25, 14.5], "chrome"),
              el([3, 2.25, 13.75], [3.75, 14.5, 14.5], "chrome", SIDES),
              el([2.5, 13, 14], [3, 15, 15], "chrome"),
              el([2.25, 12, 0.5], [3.5, 14, 1.5], "plastic")]
SHOPPING_CART = (_CART_SIDE + mirror_x(_CART_SIDE)
                 + [el([x, 0, z], [x + 1, 1.5, z + 1.5], "rubber")
                    for x in (2.75, 12.25) for z in (1.5, 13)]
                 + [el([3.75, 1.5, 2], [12.25, 2.25, 2.75], "chrome", ("north", "south", "up",
                                                                       "down")),
                    el([3.75, 1.5, 13.5], [12.25, 2.25, 14.25], "chrome", ("north", "south",
                                                                           "up", "down")),
                    el([3.75, 3, 5], [12.25, 3.2, 13.5], "mesh", ("up", "down")),
                    el([2.75, 7, 1.25], [13.25, 7.2, 13.75], "mesh", ("up", "down")),
                    el([2.75, 7, 1], [13.25, 13.5, 1.25], "mesh", ("north", "south")),
                    el([2.75, 7, 13.75], [13.25, 13.5, 14], "mesh", ("north", "south")),
                    el([2.75, 13.5, 0.75], [13.25, 14, 1.25], "chrome"),
                    el([2.75, 13.5, 13.75], [13.25, 14, 14.25], "chrome"),
                    el([3, 9, 13.4], [13, 13.25, 13.7], "plastic", ("north", "south", "up")),
                    el([2, 14.5, 14.75], [14, 15.5, 15.75], "plastic")])

_POST = [el([0.5, 0, 1.25], [1.25, 14, 2], "chrome", SIDES),
         el([0.5, 0, 14], [1.25, 14, 14.75], "chrome", SIDES)]
CORRAL_BODY = []
for _z in (1.25, 14):
    CORRAL_BODY += [el([0, 5, _z], [16, 5.75, _z + 0.75], "chrome",
                       ("north", "south", "up", "down")),
                    el([0, 11.5, _z], [16, 12.25, _z + 0.75], "chrome",
                       ("north", "south", "up", "down")),
                    el([7.6, 0, _z], [8.4, 11.5, _z + 0.75], "chrome", SIDES),
                    el([0, 0, _z - 0.25], [16, 0.5, _z + 1], "chrome", ("north", "south", "up"))]
CORRAL_LEFT = (_POST
               + [el([0.5, 13.25, 2], [1.25, 14, 14], "chrome", ("east", "west", "up", "down")),
                  el([0.6, 14, 2.5], [1.15, 20, 13.5], "sign", ("west", "east", "up"),
                     uv={"west": [2, 4.75, 14, 10.75], "east": [2, 4.75, 14, 10.75]})])
CORRAL_RIGHT = [el([15.25, 0, 1.25], [16, 12.25, 2], "chrome", SIDES + ("up",)),
                el([15.25, 0, 14], [16, 12.25, 14.75], "chrome", SIDES + ("up",))]

BASKET_STACK = ([el([2.5, 0, 3.5], [13.5, 0.75, 12.5], "case"),
                 el([3, 0.75, 4], [13, 10.75, 12], "basket", SIDES),
                 el([3.5, 10.4, 4.5], [12.5, 10.5, 11.5], "rubber", ("up",)),
                 el([3, 10.75, 4], [13, 11.25, 4.5], "basket", NO_DOWN),
                 el([3, 10.75, 11.5], [13, 11.25, 12], "basket", NO_DOWN),
                 el([3, 10.75, 4.5], [3.5, 11.25, 11.5], "basket", ("east", "west", "up")),
                 el([12.5, 10.75, 4.5], [13, 11.25, 11.5], "basket", ("east", "west", "up")),
                 el([3.5, 11, 7.5], [4, 14, 8.5], "case", SIDES),
                 el([12, 11, 7.5], [12.5, 14, 8.5], "case", SIDES),
                 el([4, 13.5, 7.5], [12, 14, 8.5], "case"),
                 el([7.5, 0.75, 12.5], [8.5, 16, 13.5], "case", SIDES),
                 el([3, 16, 12.6], [13, 20, 13.4], "sign", ALL,
                    uv={"north": [0.5, 4.5, 15.5, 10.5], "south": [0.5, 4.5, 15.5, 10.5]})])

SECURITY_GATE = [el([6, 0, 2], [10, 1, 14], "case"),
                 el([7, 1, 2], [9, 23, 3.5], "shell"),
                 el([7, 1, 12.5], [9, 23, 14], "shell"),
                 el([7, 21.5, 3.5], [9, 23, 12.5], "shell", ("east", "west", "up", "down")),
                 el([7.6, 1, 3.5], [8.4, 21.5, 12.5], "panel", ("east", "west")),
                 el([6.9, 19, 2.4], [9.1, 20, 3.1], "glow", ("north", "east", "west", "up"))]

AISLE_SIGN = [el([2, 13.5, 7.75], [2.5, 16, 8.25], "chrome", SIDES),
              el([13.5, 13.5, 7.75], [14, 16, 8.25], "chrome", SIDES),
              el([0.5, 13, 7.25], [15.5, 13.5, 8.75], "case"),
              el([0, 4.5, 7.5], [16, 13, 8.5], "face", ALL,
                 {"east": "case", "west": "case", "up": "case", "down": "case"},
                 uv={"north": [0, 3.75, 16, 12.25], "south": [0, 3.75, 16, 12.25]})]

_MAG_KEYS = ("mag_a", "mag_b", "mag_c", "mag_d")
MAGAZINE_RACK = [el([0.5, 0, 14.5], [15.5, 20, 15], "case", ("north", "south")),
                 el([0.5, 0, 9], [1, 20, 15], "case", NO_DOWN),
                 el([15, 0, 9], [15.5, 20, 15], "case", NO_DOWN)]
for _i, _y in enumerate((1, 7.5, 14)):
    MAGAZINE_RACK += [el([1, _y, 9.5], [15, _y + 1.5, 10], "case"),
                      el([1, _y, 10], [15, _y + 0.25, 14.5], "case", ("up",))]
    for _k, _x in enumerate((1.4, 5.9, 10.4)):
        MAGAZINE_RACK.append(ALL_UV(el([_x, _y + 0.25, 10.4], [_x + 4.25, _y + 6, 10.6],
                                       _MAG_KEYS[(_i + _k) % 4], ("north", "up", "east", "west"),
                                       rot=("x", 22.5, [_x + 2, _y + 0.25, 10.5])), ["north"]))


# ------------------------------------------------------------------------------------------
# Finishes
# ------------------------------------------------------------------------------------------
def fin(fid, tex, *names):
    return (fid, dict(tex)) + names


CASE_BLACK = {"shell": MT("case_black"), "frame": MT("case_black"), "liner": MT("liner"),
              "trim": T("stainless_dark"), "kick": MT("steel_grey")}
CASE_WHITE = {"shell": MT("case_white"), "frame": MT("case_white"), "liner": MT("liner"),
              "trim": T("stainless_dark"), "kick": MT("steel_grey")}
BLACK_N = ("Black", "Schwarz", "negro", "svart")
WHITE_N = ("White", "Weiß", "blanco", "vit")


def stock_tex(a, b):
    return {"stock_a": MT("stock_" + a), "stock_a_top": MT("stock_%s_top" % a),
            "stock_b": MT("stock_" + b), "stock_b_top": MT("stock_%s_top" % b)}


def cooler_fins(extra):
    return [fin("black", dict(CASE_BLACK, **extra), *BLACK_N),
            fin("white", dict(CASE_WHITE, **extra), *WHITE_N)]


COOLER_TEX = dict(stock_tex("drinks", "drinks_b"), glass=MT("glass_clear"))
FREEZER_TEX = dict(stock_tex("frozen", "frozen_b"), glass=MT("glass_frost"))
DAIRY_TEX = dict(stock_tex("dairy", "dairy_b"), glass=MT("glass_clear"))
# Lit parts swap these keys between their unlit and lit textures.
GLOW_COLD = {"glow": (GLOW_OFF, GLOW_ON), "header": (MT("header_cold_off"), MT("header_cold_on"))}
GLOW_FROZEN = {"glow": (GLOW_OFF, GLOW_ON),
               "header": (MT("header_frozen_off"), MT("header_frozen_on"))}
GLOW_DAIRY = {"glow": (GLOW_OFF, GLOW_ON),
              "header": (MT("header_dairy_off"), MT("header_dairy_on"))}
GLOW_ONLY = {"glow": (GLOW_OFF, GLOW_ON)}

CHECKOUT_FINS = [
    fin("grey", {"wood": OT("laminate_grey"), "wood_v": OT("laminate_grey"),
                 "edge": OT("laminate_grey_edge"), "kick": MT("steel_grey"),
                 "plastic": MT("plastic_grey")}, "Grey", "Grau", "gris", "grå"),
    fin("walnut", {"wood": T("walnut"), "wood_v": T("walnut_v"), "edge": T("walnut_edge"),
                   "kick": MT("steel_grey"), "plastic": MT("plastic_grey")},
        "Walnut", "Nussbaum", "nogal", "valnöt"),
]
SERVICE_FINS = [
    fin("blue", {"wood": T("white"), "wood_v": MT("laminate_blue"), "edge": T("white_edge"),
                 "frame": T("metal_black"), "sign": MT("service_sign")},
        "Blue", "Blau", "azul", "blå"),
    fin("red", {"wood": T("white"), "wood_v": MT("laminate_red"), "edge": T("white_edge"),
                "frame": T("metal_black"), "sign": MT("service_sign_red")},
        "Red", "Rot", "rojo", "röd"),
]
# The sign on the reception desk's raised front, over its black band.
SERVICE_SIGN = [el([2, 10.25, 1.35], [14, 14.75, 1.5], "sign", ("north",),
                   uv={"north": [0, 4.5, 16, 11]})]
CART_FINS = [fin(c, {"plastic": MT("plastic_" + c)}, *n)
             for c, n in (("red", ("Red", "Rot", "rojo", "röd")),
                          ("blue", ("Blue", "Blau", "azul", "blå")),
                          ("grey", ("Grey", "Grau", "gris", "grå")))]
OAK = {"wood": T("oak"), "wood_v": T("oak_v"), "edge": T("oak_edge")}

# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
PIECE_METAL = "Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID"
RUN = 'new BlockMarketRun("%s", new int[]{%s}, "%s", %s)'
RUN_FULL = ('new BlockMarketRun("%s", new int[]{%s}, "%s", %d, %s, %s, %s, %sF, '
            'BlockRenderLayer.%s)')
FIX = 'new BlockBathroomFixture("%s", new int[]{%s}, FixtureMaterial.%s)'
FIX_SOUND = 'new BlockBathroomFixture("%s", new int[]{%s}, FixtureMaterial.%s, %s, %sF)'


def jbox(box):
    return ", ".join(R._num(v) for v in box)


def box_of(specs, tall=False):
    """The Java box of a piece: its elements' extent in whole pixels, inside -16..32."""
    built = [R.build(s) for s in specs if not s["rot"]]
    lo = [max(0, int(math.floor(min(e["from"][i] for e in built)))) for i in range(3)]
    hi = [min(32, int(math.ceil(max(e["to"][i] for e in built)))) for i in range(3)]
    hi[0] = min(16, hi[0])
    hi[2] = min(16, hi[2])
    if not tall:
        hi[1] = min(hi[1], 24)
    return lo + hi


def big(specs):
    return B.big_display(B.centred(specs))


# Every piece: (piece, kind, finishes, names, spec, group, behaviour). kinds:
#   tall_run: two blocks, joins left/right (body/end parts per half), lit parts swap textures
#   run: one block joining left/right (body/left/right), optional lit parts
#   gondola: the gondola's parts and rules
#   single: one model turned by facing
#   tall: two blocks cut in halves (forge variants facing x upper)
#   counter: rests on what is under it
#   service: the office's reception desk parts in a store's colours
#   sign: the aisle sign, its face picked by number
PIECES = []


def add(piece, kind, finishes, names, spec, group, behaviour):
    PIECES.append((piece, kind, finishes, names, spec, group, behaviour))


COOLER_BOX = [0, 0, 2, 16, 32, 16]
_body, _end = reach_in("drinks", "drinks_b")
add("reach_in_cooler", "tall_run", cooler_fins(COOLER_TEX),
    ("Reach-In Cooler", "Getränkekühlschrank mit Glastür", "Nevera expositora de puerta de cristal",
     "Kyl med glasdörr"),
    {"body": _body, "end": _end, "glow": GLOW_COLD, "particle": "shell",
     "java": 'new BlockDisplayCooler("%%s", new int[]{%s})' % jbox(COOLER_BOX)},
    "Refrigerated",
    "2 blocks tall; joins into a line of doors (end panels only at the ends); 27 slots, fridge "
    "door sounds; lit (light 10), sneak-click with an empty hand or redstone switches the lights")
_body, _end = reach_in("frozen", "frozen_b")
add("reach_in_freezer", "tall_run", cooler_fins(FREEZER_TEX),
    ("Reach-In Freezer", "Tiefkühlschrank mit Glastür", "Congelador expositor de puerta de cristal",
     "Frys med glasdörr"),
    {"body": _body, "end": _end, "glow": GLOW_FROZEN, "particle": "shell",
     "java": 'new BlockDisplayCooler("%%s", new int[]{%s})' % jbox(COOLER_BOX)},
    "Refrigerated", "As the cooler, frosted glass over ice cream and frozen meals")
_body, _end = multideck("dairy", "dairy_b")
add("dairy_case", "tall_run", cooler_fins(DAIRY_TEX),
    ("Open Dairy Case", "Offenes Molkereikühlregal", "Mural refrigerado de lácteos",
     "Öppen mejerikyl"),
    {"body": _body, "end": _end, "glow": GLOW_DAIRY, "particle": "shell",
     "java": 'new BlockDisplayCooler("%s", new int[]{0, 0, 0, 16, 32, 16})'},
    "Refrigerated",
    "Open multideck, 2 blocks tall; joins into a run; 27 slots; lit, switches like the cooler")
add("island_freezer", "run", [fin("white", CASE_WHITE, *WHITE_N)],
    ("Island Freezer", "Tiefkühlinsel", "Arcón congelador de isla", "Frysdisk"),
    {"body": ISLAND, "end": ISLAND_END, "glow": GLOW_ONLY, "particle": "shell",
     "tex": stock_tex("frozen", "frozen_b"),
     "java": 'new BlockDisplayCase("%%s", new int[]{%s})' % jbox(box_of(ISLAND))},
    "Refrigerated", "Joins end to end; sliding glass lids over frozen goods; 27 slots; lit, "
                    "sneak-click or redstone switches it")
add("ice_cream_case", "run", [fin("white", CASE_WHITE, *WHITE_N)],
    ("Ice Cream Display Freezer", "Eistheke", "Vitrina de helados", "Glassdisk"),
    {"body": ICE_CREAM, "end": ICE_CREAM_END, "glow": GLOW_ONLY, "particle": "shell",
     "java": 'new BlockDisplayCase("%%s", new int[]{%s})' % jbox(box_of(ICE_CREAM))},
    "Refrigerated", "Curved glass over eight flavours; joins; 27 slots; lit")
add("deli_case", "run", [fin("black", CASE_BLACK, *BLACK_N)],
    ("Deli Service Case", "Feinkosttheke", "Vitrina de charcutería", "Delikatessdisk"),
    {"body": DELI, "end": DELI_END, "glow": GLOW_ONLY, "particle": "shell",
     "java": 'new BlockDisplayCase("%%s", new int[]{%s})' % jbox(box_of(DELI))},
    "Refrigerated", "Curved glass over meats and salads; joins; 27 slots; lit")
add("bakery_case", "run", [fin("oak", dict(CASE_BLACK, **OAK), "Oak", "Eiche", "roble", "ek")],
    ("Bakery Display Case", "Backwarenvitrine", "Vitrina de panadería", "Bagerimonter"),
    {"body": BAKERY, "end": BAKERY_END, "glow": GLOW_ONLY, "particle": "wood",
     "tex": stock_tex("bakery", "bakery_b"),
     "java": 'new BlockDisplayCase("%%s", new int[]{%s})' % jbox(box_of(BAKERY))},
    "Refrigerated", "Glass case of breads and pastries on an oak base; joins; 27 slots; lit")

add("gondola_shelf", "gondola",
    [fin(v, stock_tex(a, b) if a else {}, *n) for v, a, b, *n in GONDOLA_KINDS],
    ("Gondola Shelving", "Gondelregal", "Góndola", "Gondolhylla"),
    {"particle": "shelf", "java": 'new BlockGondola("%s", new int[]{0, 0, 6, 16, 16, 16})'},
    "Shelving",
    "Joins and stacks with any gondola, whatever its stock: uprights only at run ends, base deck "
    "at the bottom of a stack, top cap at its head; back to back makes an island")

add("produce_stand", "run",
    [fin(v, dict(OAK, produce=PRODUCE_TEX % bed), *n) for v, bed, *n in PRODUCE_KINDS],
    ("Produce Stand", "Obst- und Gemüsestand", "Puesto de frutas y verduras",
     "Frukt- och grönsaksdisk"),
    {"body": PRODUCE_BODY, "end": PRODUCE_END, "particle": "wood",
     "java": RUN % ("%s", jbox(box_of(PRODUCE_BODY)), "produce_stand",
                    "BlockRenderLayer.CUTOUT")},
    "Produce", "Angled oak stand; joins any produce stand (end boards only at the ends)")
add("produce_scale", "single", [fin("steel", {"shell": MT("case_white")}, "Steel", "Stahl",
                                    "acero", "stål")],
    ("Hanging Produce Scale", "Hängewaage", "Báscula colgante", "Hängvåg"),
    {"geo": PRODUCE_SCALE, "particle": "steel",
     "java": FIX % ("%s", jbox(box_of(PRODUCE_SCALE)), "METAL")},
    "Produce", "Hangs from the block above")
add("bulk_bins", "run",
    [fin(v, dict(OAK, **{"bulk_%d" % (i + 1): MT(t) for i, t in enumerate(texs)}), *n)
     for v, texs, *n in BULK_KINDS],
    ("Bulk Bins", "Schüttgutspender", "Dispensadores a granel", "Lösviktsbehållare"),
    {"body": BULK_BODY, "end": BULK_END, "particle": "wood",
     "java": RUN % ("%s", jbox(box_of(BULK_BODY)), "bulk_bins", "BlockRenderLayer.TRANSLUCENT")},
    "Produce", "Four gravity bins behind clear fronts; joins any bulk bins")

_CO = CHECKOUT_FINS
add("checkout_belt", "run", _CO,
    ("Checkout Counter with Belt", "Kassentisch mit Förderband", "Caja con cinta transportadora",
     "Kassadisk med band"),
    {"body": CHECKOUT_BELT, "end": cabinet_end(14.5), "particle": "wood",
     "java": RUN % ("%s", jbox(box_of(CHECKOUT_BELT)), "checkout", "BlockRenderLayer.SOLID")},
    "Checkout", "Animated belt; joins the scanner counter and bagging end into one lane; "
                "counter pieces rest on its top")
add("checkout_scanner", "run", _CO,
    ("Checkout Scanner Counter", "Kassentisch mit Scanner", "Caja con escáner",
     "Kassadisk med skanner"),
    {"body": CHECKOUT_SCANNER, "end": cabinet_end(14.5), "particle": "wood",
     "java": RUN_FULL % ("%s", jbox(box_of(CHECKOUT_SCANNER)), "checkout", 0, "null", "null",
                         "FurnishingsSounds.SCANNER_BEEP", "1.0", "SOLID")},
    "Checkout", "Flatbed and tower scanner; beeps on click; part of the checkout lane")
add("checkout_bagging", "run", _CO,
    ("Checkout Bagging End", "Kassentisch mit Packablage", "Zona de embolsado de caja",
     "Packbord vid kassan"),
    {"body": CHECKOUT_BAGGING, "end": cabinet_end(12), "particle": "wood",
     "java": RUN_FULL % ("%s", jbox(box_of(CHECKOUT_BAGGING)), "checkout", 9,
                         "FurnishingsSounds.CABINET_OPEN", "FurnishingsSounds.CABINET_CLOSE",
                         "null", "1.0", "SOLID")},
    "Checkout", "Lower bagging well with a rack of bags; 9 slots; ends the checkout lane")
add("pos_terminal", "counter", [fin("black", {}, *BLACK_N)],
    ("POS Terminal", "Kassenterminal", "Terminal de punto de venta", "Kassaterminal"),
    {"geo": POS_TERMINAL, "particle": "case",
     "java": 'new BlockCashRegister("%s", new int[]{%s})'},
    "Checkout", "Touchscreen on a cash drawer; 9 slots, the drawer rings open (synthesised)")
add("cash_register", "counter", [fin("brass", {}, "Brass", "Messing", "latón", "mässing")],
    ("Vintage Cash Register", "Nostalgische Registrierkasse", "Caja registradora antigua",
     "Gammal kassaapparat"),
    {"geo": CASH_REGISTER, "particle": "brass",
     "java": 'new BlockCashRegister("%s", new int[]{%s})'},
    "Checkout", "Keys, amount flags and a drawer; 9 slots, ka-ching on opening")
add("receipt_printer", "counter", [fin("black", {}, *BLACK_N)],
    ("Receipt Printer", "Bondrucker", "Impresora de tickets", "Kvittoskrivare"),
    {"geo": RECEIPT_PRINTER, "particle": "case",
     "java": ('new BlockCounterPiece("%%s", new int[]{%%s}, %s, FurnishingsSounds.PRINTER_RUN, '
              '1.8F, null)' % PIECE_METAL)},
    "Checkout", "Prints (the copier's sound, faster) on click")
add("card_terminal_stand", "counter", [fin("black", {}, *BLACK_N)],
    ("Card Terminal on Stand", "Kartenterminal mit Ständer", "Terminal de tarjetas con soporte",
     "Kortterminal på stativ"),
    {"geo": CARD_STAND, "particle": "case",
     "java": ('new BlockCounterPiece("%%s", new int[]{%%s}, %s, FurnishingsSounds.VERIFONE_MX915, '
              '1.0F, null)' % PIECE_METAL)},
    "Checkout", "Swivel stand facing the customer; the card terminal's beep on click")
add("bag_carousel", "counter", [fin("white", {"steel": T("chrome")}, *WHITE_N)],
    ("Bagging Carousel", "Tütenkarussell", "Carrusel de bolsas", "Påskarusell"),
    {"geo": BAG_CAROUSEL, "particle": "steel",
     "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s)' % PIECE_METAL},
    "Checkout", "")
add("self_checkout", "tall", [fin("grey", {"shell": MT("plastic_grey"),
                                          "screen": MT("screen_kiosk")}, "Grey", "Grau", "gris",
                                  "grå")],
    ("Self-Checkout Kiosk", "Selbstbedienungskasse", "Caja de autopago", "Självscanningskassa"),
    {"geo": SELF_CHECKOUT, "particle": "shell",
     "java": 'new BlockMarketTall("%%s", new int[]{%s}, 7, FurnishingsSounds.SCANNER_BEEP)'
             % jbox(box_of(SELF_CHECKOUT, tall=True))},
    "Checkout", "2 blocks tall; lit screen and lane light (light 7); scanner beep on click")
add("service_desk", "service", SERVICE_FINS,
    ("Customer Service Desk", "Kundendiensttheke", "Mostrador de atención al cliente",
     "Kundtjänstdisk"),
    {"java": 'new BlockKitchenCabinet("%s", new int[]{0, 0, 0, 16, 16, 15}, KitchenLine.RECEPTION, '
             '9, KitchenFront.DRAWERS)'},
    "Checkout", "The reception desk in store colours: joins into one counter; 9 slots in drawers")
add("impulse_rack", "single", [fin("black", {"mesh": MT("mesh_black")}, *BLACK_N)],
    ("Checkout Candy Rack", "Süßwarenregal an der Kasse", "Expositor de golosinas",
     "Godisställ vid kassan"),
    {"geo": IMPULSE_RACK, "particle": "case", "tex": stock_tex("candy", "candy"),
     "java": FIX % ("%s", jbox(box_of(IMPULSE_RACK)), "METAL")},
    "Checkout", "")

add("shopping_cart", "single", CART_FINS,
    ("Shopping Cart", "Einkaufswagen", "Carrito de la compra", "Kundvagn"),
    {"geo": SHOPPING_CART, "particle": "chrome",
     "java": FIX % ("%s", jbox(box_of(SHOPPING_CART)), "METAL")}, "Store", "")
add("cart_corral", "run", [fin("steel", {}, "Steel", "Stahl", "acero", "stål")],
    ("Cart Corral", "Einkaufswagen-Sammelbox", "Punto de recogida de carritos",
     "Kundvagnsgård"),
    {"body": CORRAL_BODY, "end": CORRAL_LEFT, "end_right": CORRAL_RIGHT, "particle": "chrome",
     "java": RUN % ("%s", jbox(box_of(CORRAL_BODY + CORRAL_LEFT)), "cart_corral",
                    "BlockRenderLayer.SOLID")},
    "Store", "Joins into a long corral; a closed hoop and CART RETURN sign at its left end, open "
             "at its right")
add("basket_stack", "single",
    [fin(c, {"basket": MT("basket_" + c), "sign": MT("basket_sign")}, *n)
     for c, n in (("red", ("Red", "Rot", "rojo", "röd")),
                  ("green", ("Green", "Grün", "verde", "grön")))],
    ("Shopping Basket Stack", "Einkaufskorbstapel", "Pila de cestas de la compra", "Korgtrave"),
    {"geo": BASKET_STACK, "particle": "basket",
     "java": FIX % ("%s", jbox(box_of(BASKET_STACK)), "PLASTIC")}, "Store", "")
add("security_gate", "single", [fin("grey", {"shell": MT("plastic_grey")}, "Grey", "Grau", "gris",
                                    "grå")],
    ("Security Gate", "Warensicherungsantenne", "Antena antihurto", "Larmbåge"),
    {"geo": SECURITY_GATE, "particle": "shell",
     "java": FIX_SOUND % ("%s", jbox(box_of(SECURITY_GATE)), "PLASTIC",
                          "FurnishingsSounds.APPLIANCE_BEEP", "1.4")},
    "Store", "A pedestal of the pair at a door; chirps on click")
add("aisle_sign", "sign", [fin("blue", {}, "Blue", "Blau", "azul", "blå")],
    ("Aisle Sign", "Gangschild", "Cartel de pasillo", "Gångskylt"),
    {"geo": AISLE_SIGN, "particle": "case",
     "java": 'new BlockAisleSign("%%s", new int[]{%s})' % jbox(box_of(AISLE_SIGN))},
    "Store", "Hangs from the ceiling, numbered both sides; click steps the number 1-16, "
             "sneak-click steps back")
add("magazine_rack", "single", [fin("black", {}, *BLACK_N)],
    ("Magazine Rack", "Zeitschriftenregal", "Revistero", "Tidningsställ"),
    {"geo": MAGAZINE_RACK, "particle": "case",
     "java": FIX % ("%s", jbox(box_of(MAGAZINE_RACK)), "METAL")}, "Store", "")
add("bottle_return_machine", "tall", [fin("green", {"shell": MT("plastic_green")}, "Green", "Grün",
                                          "verde", "grön")],
    ("Bottle Return Machine", "Pfandautomat", "Máquina de devolución de envases", "Pantautomat"),
    {"geo": BOTTLE_RETURN, "particle": "shell",
     "java": 'new BlockMarketTall("%%s", new int[]{%s}, 4, null)'
             % jbox(box_of(BOTTLE_RETURN, tall=True))},
    "Store", "2 blocks tall, decorative")

PIECE = {p[0]: p for p in PIECES}


def names_with(names, fnames):
    return ["%s (%s)" % (n, f) for n, f in zip(names, fnames)]


def java_for(piece, reg):
    _p, kind, _f, _n, spec, _g, _b = PIECE[piece]
    if kind == "counter":
        return spec["java"] % (reg, jbox(box_of(spec["geo"])))
    return spec["java"] % reg


def entries():
    """Every block: (registry, piece, kind, finish textures, names, java)."""
    out = []
    for piece, kind, finishes, names, spec, _g, _b in PIECES:
        for fid, ftex, *fnames in finishes:
            reg = "%s_%s" % (piece, fid)
            tex = dict(spec.get("tex", {}))
            tex.update(ftex)
            out.append((reg, piece, kind, tex, names_with(names, fnames), java_for(piece, reg)))
    return out


# ------------------------------------------------------------------------------------------
# Models and blockstates
# ------------------------------------------------------------------------------------------
def base_models():
    """Every base geometry model: (name, geometry json)."""
    out = []
    for piece, kind, finishes, _n, spec, _g, _b in PIECES:
        p = spec.get("particle", "shell")
        if kind == "tall_run":
            body_l, body_u = split_tall(spec["body"])
            end_l, end_u = split_tall(spec["end"])
            for name, geo in (("body_lower", body_l), ("body_upper", body_u),
                              ("left_lower", end_l), ("left_upper", end_u),
                              ("right_lower", mirror_x(end_l)), ("right_upper", mirror_x(end_u))):
                out.append(("%s_%s" % (piece, name), geometry(geo, p)))
            item = spec["body"] + spec["end"] + mirror_x(spec["end"])
            out.append(("%s_item" % piece, geometry(item, p, display=big(item))))
        elif kind == "run":
            right = spec.get("end_right") or mirror_x(spec["end"])
            out.append(("%s_body" % piece, geometry(spec["body"], p)))
            out.append(("%s_left" % piece, geometry(spec["end"], p)))
            out.append(("%s_right" % piece, geometry(right, p)))
            item = spec["body"] + spec["end"] + right
            lo, hi = B.extent(item)
            out.append(("%s_item" % piece, geometry(item, p,
                                                    display=big(item) if hi[1] > 18 else None)))
        elif kind == "gondola":
            shared = gondola_parts(None, None, 0)
            for part in ("top", "left", "right"):
                out.append(("gondola_%s" % part, geometry(shared[part], p)))
            for v, above, below, *_n in GONDOLA_KINDS:
                parts = gondola_parts(above, below, 600 + hash_seed(v) % 97)
                for part in ("body", "plinth", "stacked"):
                    out.append(("gondola_%s_%s" % (v, part), geometry(parts[part], p)))
                item = (parts["body"] + parts["plinth"] + parts["top"] + parts["left"]
                        + parts["right"])
                out.append(("gondola_%s_item" % v, geometry(item, p)))
        elif kind == "service":
            out.append(("service_desk_sign", geometry(SERVICE_SIGN, "sign")))
        elif kind in ("single", "sign"):
            geo = spec["geo"]
            lo, hi = B.extent(geo)
            out.append((piece, geometry(geo, p, display=big(geo) if hi[1] > 18 else None)))
        elif kind == "tall":
            lower, upper = split_tall(spec["geo"])
            out.append(("%s_lower" % piece, geometry(lower, p)))
            out.append(("%s_upper" % piece, geometry(upper, p)))
            out.append(("%s_item" % piece, geometry(spec["geo"], p, display=big(spec["geo"]))))
        elif kind == "counter":
            geo = spec["geo"]
            for rest, drop in A.RESTS:
                out.append(("%s_%s" % (piece, rest), geometry(geo, p, dy=drop)))
            out.append(("%s_item" % piece, geometry(geo, p, display=A.small_display(geo),
                                                    centre=True)))
    out += verifone_models()
    return out


VERIFONE_SRC = "models/block/technology/shared_models/verifone_mx_915.json"


def verifone_models():
    """The Verifone's hand-made model, moved down onto each surface it can rest on."""
    path = os.path.join(ASSETS, VERIFONE_SRC)
    with open(path, encoding="utf-8") as fh:
        src = json.load(fh)
    out = []
    for rest, drop in A.RESTS:
        if not drop:
            continue
        m = {"parent": "csm:block/technology/shared_models/verifone_mx_915",
             "elements": copy.deepcopy(src["elements"])}
        for e in m["elements"]:
            e["from"][1] = round(e["from"][1] - drop, 3)
            e["to"][1] = round(e["to"][1] - drop, 3)
            if "rotation" in e:
                e["rotation"]["origin"][1] = round(e["rotation"]["origin"][1] - drop, 3)
        out.append(("vf915_%s" % rest, m))
    return out


def verifone_state():
    rest = {r: {"model": BASE + ("vf915_%s" % r)} for r, d in A.RESTS if d}
    rest["floor"] = {"model": "csm:technology/shared_models/verifone_mx_915"}
    return {"forge_marker": 1,
            "defaults": {"model": "csm:technology/shared_models/verifone_mx_915",
                         "textures": {"all": "csm:blocks/technology/verifone_mx_915",
                                      "particle": "csm:blocks/technology/verifone_mx_915",
                                      "0": "csm:blocks/technology/verifone_mx_915"}},
            "variants": {"facing": {"down": {"x": 90}, "east": {"y": 90}, "north": {},
                                    "south": {"y": 180}, "up": {"x": 270}, "west": {"y": 270}},
                         "rest": rest,
                         "inventory": [{"model": "csm:technology/shared_models/verifone_mx_915"}]}}


def facing_variants():
    return {f: ({"y": r} if r else {}) for f, r in ROT.items()}


def multipart_state(reg, rules):
    parts = []
    for part, when, r in rules:
        apply = {"model": MODEL + "%s_%s" % (reg, part)}
        if r:
            apply["y"] = r
        parts.append({"when": when, "apply": apply} if when else {"apply": apply})
    return {"multipart": parts}


def with_lit(rules, lit_parts):
    """Each rule for a lit part becomes two: its lit copy while lit, its unlit copy while not."""
    out = []
    for part, when, r in rules:
        if part in lit_parts:
            out.append((part + "_on", dict(when, lit="true"), r))
            out.append((part + "_off", dict(when, lit="false"), r))
        else:
            out.append((part, when, r))
    return out


def run_rules(tall=False):
    if not tall:
        return R.faced([("body", {}), ("left", {"left": "false"}), ("right", {"right": "false"})])
    rules = []
    for half in ("lower", "upper"):
        up = "true" if half == "upper" else "false"
        rules += R.faced([("body_" + half, {"upper": up}),
                          ("left_" + half, {"upper": up, "left": "false"}),
                          ("right_" + half, {"upper": up, "right": "false"})])
    return rules


def lit_tex(ftex, glow, on):
    t = dict(ftex)
    for key, (off_t, on_t) in glow.items():
        t[key] = on_t if on else off_t
    return t


EXTRA_LANG = {
    "itemGroup.tabmarketstore": ("CSM: Market & Store", "CSM: Markt & Laden",
                                 "CSM: Mercado y tienda", "CSM: Marknad & butik"),
    "tile.vf915.name": ("Verifone MX915",) * 4,
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


def generate(assets):
    written = []

    def dump(rel, data):
        R.dump(os.path.join(assets, rel), data)
        written.append(rel)

    def copy_model(rel, parent, ftex, base_sub=SUB):
        dump(rel, {"parent": "csm:block/%s/base/%s" % (base_sub, parent), "textures": dict(ftex)})

    for name, draw in TEXTURES.items():
        rel = "textures/blocks/%s/%s.png" % (SUB, name)
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
    for name, (draw, meta) in ANIMATED.items():
        rel = "textures/blocks/%s/%s.png" % (SUB, name)
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
        dump(rel + ".mcmeta", meta)
    models = base_models()
    particles = {}
    for name, data in models:
        dump("models/block/%s/base/%s.json" % (SUB, name), data)
        if "textures" in data:
            particles[name] = data["textures"]["particle"][1:]
    for reg, piece, kind, ftex, _names, _java in entries():
        spec = PIECE[piece][4]
        # Every finish names the texture its models' particle points to, or the game logs an
        # upward reference and draws the missing texture.
        stem = "gondola" if kind == "gondola" else piece
        for name, key in particles.items():
            if (name == stem or name.startswith(stem + "_")) and key not in ftex:
                ftex = dict(ftex)
                ftex[key] = DEFAULT_TEX[key]
        blk = "models/block/%s/%s_%%s.json" % (SUB, reg)
        item = "models/item/%s.json" % reg
        glow = spec.get("glow") or {}
        if kind in ("tall_run", "run"):
            tall = kind == "tall_run"
            parts = (["body_lower", "body_upper", "left_lower", "left_upper", "right_lower",
                      "right_upper"] if tall else ["body", "left", "right"])
            lit_parts = set()
            geo_of = dict(base_models_index(piece))
            for part in parts:
                if glow and R.used_keys(geo_of[part]) & set(glow):
                    lit_parts.add(part)
                    copy_model(blk % (part + "_on"), "%s_%s" % (piece, part),
                               lit_tex(ftex, glow, True))
                    copy_model(blk % (part + "_off"), "%s_%s" % (piece, part),
                               lit_tex(ftex, glow, False))
                else:
                    copy_model(blk % part, "%s_%s" % (piece, part), ftex)
            copy_model(item, "%s_item" % piece, lit_tex(ftex, glow, True) if glow else ftex)
            rules = run_rules(tall)
            if glow:
                rules = with_lit(rules, lit_parts)
            state = multipart_state(reg, rules)
            if glow and not lit_parts:
                raise ValueError("%s has no lit part" % reg)
        elif kind == "gondola":
            v = reg[len("gondola_shelf_"):]
            for part in ("body", "plinth", "stacked"):
                copy_model(blk % part, "gondola_%s_%s" % (v, part), ftex)
            for part in ("top", "left", "right"):
                copy_model(blk % part, "gondola_%s" % part, ftex)
            copy_model(item, "gondola_%s_item" % v, ftex)
            state = multipart_state(reg, GONDOLA_RULES)
        elif kind == "service":
            for part in ("body", "left", "right"):
                copy_model(blk % part, "reception_desk_%s" % part, ftex, O.SUB)
            copy_model(blk % "sign", "service_desk_sign", ftex)
            copy_model(item, "reception_desk_item", ftex, O.SUB)
            state = multipart_state(reg, run_rules() + R.faced([("sign", {"right": "false"})]))
        elif kind == "single":
            state = {"forge_marker": 1, "defaults": {"model": BASE + piece, "textures": ftex},
                     "variants": {"facing": facing_variants(), "inventory": [{}]}}
        elif kind == "sign":
            state = {"forge_marker": 1, "defaults": {"model": BASE + piece,
                                                     "textures": dict(ftex, face=MT("aisle_1"))},
                     "variants": {"facing": facing_variants(),
                                  "number": {str(n): {"textures": {"face": MT("aisle_%d" % n)}}
                                             for n in range(1, 17)},
                                  "inventory": [{}]}}
        elif kind == "tall":
            for half in ("lower", "upper"):
                copy_model(blk % half, "%s_%s" % (piece, half), ftex)
            copy_model(item, "%s_item" % piece, ftex)
            variants = {}
            for f, r in ROT.items():
                for up in ("false", "true"):
                    v = {"model": MODEL + "%s_%s" % (reg, "upper" if up == "true" else "lower")}
                    if r:
                        v["y"] = r
                    variants["facing=%s,upper=%s" % (f, up)] = v
            state = {"variants": variants}
        elif kind == "counter":
            state = {"forge_marker": 1,
                     "defaults": {"model": BASE + "%s_floor" % piece, "textures": ftex},
                     "variants": {"facing": facing_variants(),
                                  "rest": {r: {"model": BASE + "%s_%s" % (piece, r)}
                                           for r, _d in A.RESTS},
                                  "inventory": [{"model": BASE + "%s_item" % piece}]}}
        else:
            raise ValueError(kind)
        dump("blockstates/%s.json" % reg, state)
    dump("blockstates/vf915.json", verifone_state())
    R.write_lang(os.path.join(assets, "lang"), lang_entries())
    written += ["lang/%s.lang" % loc for loc in R.LOCALES]
    R.separate_faces(assets, written)
    return written


_BASE_CACHE = {}


def base_models_index(piece):
    """The raw geometry specs of a run piece's parts, by part name (for finding its lit parts)."""
    if piece not in _BASE_CACHE:
        _p, kind, _f, _n, spec, _g, _b = PIECE[piece]
        if kind == "tall_run":
            body_l, body_u = split_tall(spec["body"])
            end_l, end_u = split_tall(spec["end"])
            parts = {"body_lower": body_l, "body_upper": body_u, "left_lower": end_l,
                     "left_upper": end_u, "right_lower": mirror_x(end_l),
                     "right_upper": mirror_x(end_u)}
        else:
            parts = {"body": spec["body"], "left": spec["end"],
                     "right": spec.get("end_right") or mirror_x(spec["end"])}
        _BASE_CACHE[piece] = parts
    return _BASE_CACHE[piece].items()

# ------------------------------------------------------------------------------------------
# Tab lines: the moved blocks (the produce crates from the Furniture tab, the Verifone from
# Technology) keep their classes and registry names
# ------------------------------------------------------------------------------------------
EVENT = "fmlPreInitializationEvent"
MOVED_PRODUCE = ["BlockAppleCrate", "BlockBananaCrate", "BlockBeetCrate", "BlockCarrotBarrel",
                 "BlockCarrotCrate", "BlockCornCrate", "BlockGoldenApples",
                 "BlockGreenAppleCrate", "BlockLargeCrate", "BlockLettuceCrate",
                 "BlockOnionCrate", "BlockOrangeCrate", "BlockPearCrate", "BlockPotatoeCrate",
                 "BlockTomatoeCrate"]
MOVED_NAMES = {"BlockAppleCrate": "Apple Crate", "BlockBananaCrate": "Banana Crate",
               "BlockBeetCrate": "Beet Crate", "BlockCarrotBarrel": "Carrot Barrel",
               "BlockCarrotCrate": "Carrot Crate", "BlockCornCrate": "Corn Crate",
               "BlockGoldenApples": "Golden Apples", "BlockGreenAppleCrate": "Green Apple Crate",
               "BlockLargeCrate": "Large Crate", "BlockLettuceCrate": "Lettuce Crate",
               "BlockOnionCrate": "Onion Crate", "BlockOrangeCrate": "Orange Crate",
               "BlockPearCrate": "Pear Crate", "BlockPotatoeCrate": "Potato Crate",
               "BlockTomatoeCrate": "Tomato Crate", "BlockVerifoneMx915": "Verifone MX915"}
GROUP_ORDER = ["Refrigerated", "Shelving", "Produce", "Checkout", "Store"]


def fragments():
    lines = []
    for group in GROUP_ORDER:
        lines.append("    // ---- %s ----" % group)
        if group == "Produce":
            lines.append("    // Produce crates (moved from the Furniture tab)")
            for cls in MOVED_PRODUCE:
                lines.append("    initTabBlock(%s.class, %s); // %s" % (cls, EVENT,
                                                                      MOVED_NAMES[cls]))
            lines.append("")
        if group == "Checkout":
            lines.append("    // Verifone MX915 (moved from the Technology tab)")
            lines.append("    initTabBlock(BlockVerifoneMx915.class, %s);" % EVENT)
            lines.append("")
        last = None
        for reg, piece, _k, _t, names, java in entries():
            if PIECE[piece][5] != group:
                continue
            if piece != last:
                if last is not None:
                    lines.append("")
                lines.append("    // %s" % names[0].split(" (")[0])
                last = piece
            lines.append("    initTabBlock(%s);" % java)
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def check_bounds():
    bad = []
    for name, data in base_models():
        for e in data["elements"]:
            if any(v < -16 or v > 32 for v in e["from"] + e["to"]):
                bad.append("%s: element out of range: %s" % (name, e))
            for f in e["faces"].values():
                if "uv" in f and any(v < 0 or v > 16 for v in f["uv"]):
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
    tmp = tempfile.mkdtemp(prefix="market_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not R.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("market and store furniture is up to date (%d files)" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
