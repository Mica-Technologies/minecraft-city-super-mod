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
The fresh departments (butcher and seafood cases, bread racks, the pastry and hot food cases, the
rotisserie, floral, the coffee station) and the front of the store (the pharmacy's counters,
shelf wall and signs, the tobacco case, the lottery's dispenser and terminal, the coin, photo and
movie kiosks, and the ice, propane and firewood merchandisers outside) follow them.

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
    scanner and bagging counters are one lane, the cart corral joins itself;
  * the pharmacy's drop-off and pick-up counters of a finish are one counter (the reception
    desk's line), the pharmacy shelves join and stack into a wall, and the tobacco cases, ice
    merchandisers, propane cages and firewood racks each join their own kind.

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


# --- the fresh departments' stock: sliced bread, muffins and rolls, hot food ----------------
def col_bread_bag(px, x0, ht, crust, crumb, band, rng):
    """Sliced bread in its bag seen end on: the crust round the slices' crumb, the bag's
    printed band across it, the gathered neck and its twist tie on top."""
    for dy in range(ht):
        y = 31 - dy
        for dx in range(7):
            if dy >= ht - 2:
                if not 2 <= dx <= 4 or (dy == ht - 1 and dx != 3):
                    continue
                col = (240, 200, 60) if dy == ht - 1 else shade(band, 1.1)
            elif dx in (0, 6) or dy == ht - 3:
                col = crust
            elif ht // 3 <= dy < ht // 3 + 2:
                col = band
            else:
                col = shade(crumb, 1.0 + 0.06 * ((dx + dy) % 2))
            put(px, x0 + dx, y, shade(col, 0.88 if dx == 0 else 1.0))


def top_bread_bag(px, x0, crust, band, rng):
    """Bagged loaves from above, lying front to back: the top crust under the bag's sheen, the
    band across each, a gap to the next bag."""
    for y in range(32):
        for dx in range(8):
            col = (0, 0, 0, 0) if dx == 7 or y % 8 == 7 else shade(crust, 1.05 - 0.1 * (dx in (0, 6)))
            if len(col) == 3 and y % 8 in (3, 4):
                col = band
            if len(col) == 3 and dx == 2 and y % 8 in (1, 5):
                col = shade(col, 1.3)
            put(px, x0 + dx, y, col)


def col_muffin(px, x0, ht, cup, top, rng):
    """Muffins two abreast, three texels wide: a pleated paper cup, a domed top with berries."""
    for bx in (0, 4):
        for dy in range(ht):
            for dx in range(3):
                if dy < ht // 2:
                    col = shade(cup, 0.85 if dx == 1 else 1.0)
                else:
                    if dy == ht - 1 and dx != 1:
                        continue
                    col = top if rng.random() > 0.12 else (90, 40, 110)
                put(px, x0 + bx + dx, 31 - dy, shade(col, 0.9 + 0.1 * (dx == 0)))


def col_rounds(px, x0, ht, body, accent, rng):
    """Low round bakes in rows, cinnamon rolls or cookies: each three texels wide, a line of
    their accent (the icing, the chips) through them."""
    for dy in range(ht):
        for dx in range(8):
            if dx % 4 == 3:
                continue
            if dy == ht - 1 and dx % 4 != 1:
                continue
            col = accent if (dy == ht // 2 and dx % 2 == 0) else body
            put(px, x0 + dx, 31 - dy, shade(col, 0.85 + 0.12 * (dx % 4 == 1) + 0.05 * dy))


def top_rounds(px, x0, body, accent, rng, hole=False):
    """Round bakes from above on a tray: discs every five texels, the accent scattered on them
    (icing, chips, berries), a ring with a hole for a bagel."""
    for y in range(32):
        for dx in range(8):
            put(px, x0 + dx, y, (0, 0, 0, 0))
    for cy in range(2, 32, 5):
        for dx in range(8):
            for dy in range(-2, 3):
                d = math.hypot(dx - 3.5, dy)
                if d > 3.2 or (hole and d < 1.0):
                    continue
                col = shade(body, 1.15 - 0.1 * d)
                if rng.random() < 0.25:
                    col = accent
                put(px, x0 + dx, cy + dy, col)


def top_bagels(px, x0, body, accent, rng):
    top_rounds(px, x0, body, accent, rng, hole=True)


def col_chicken(px, x0, ht, rng):
    """A rotisserie chicken in its clamshell: the black base, the golden bird, the clear dome
    over it (translucent, the case draws in the translucent layer)."""
    for dy in range(ht):
        y = 31 - dy
        for dx in range(8):
            if dy < 1:
                col = (28, 28, 30)
            elif dx in (0, 7) or dy == ht - 1:
                if dy == ht - 1 and dx in (0, 7):
                    continue
                col = (226, 236, 240, 110)
            else:
                t = (dy - 0.5) / max(1.0, ht - 2)
                w = 3.1 * math.sqrt(max(0.0, 1 - ((t - 0.3) / 0.75) ** 2))
                if abs(dx + 0.5 - 4) > w:
                    col = (226, 236, 240, 70)
                else:
                    col = shade((190, 108, 40), 0.72 + 0.5 * t - 0.1 * abs(dx + 0.5 - 4))
                    if dy == 1 and dx in (1, 6):
                        col = (150, 80, 30)
            put(px, x0 + dx, y, col)


def top_chicken(px, x0, rng):
    for y in range(32):
        for dx in range(8):
            ly = y % 8
            d = math.hypot((dx - 3.5) / 3.2, (ly - 3.5) / 3.2)
            if ly == 7 or dx == 7:
                col = (0, 0, 0, 0)
            elif d < 0.8:
                col = shade((200, 124, 50), 1.2 - 0.4 * d)
            else:
                col = (226, 236, 240, 110)
            put(px, x0 + dx, y, col)


def col_tray(px, x0, ht, food, accent, rng):
    """A foil tray heaped with hot food: wings, potato wedges, macaroni."""
    for dy in range(ht):
        for dx in range(8):
            if dx == 7:
                continue
            if dy == 0:
                col = (170, 172, 176)
            else:
                if dy == ht - 1 and dx in (0, 6):
                    continue
                col = accent if rng.random() < 0.25 else food
                col = shade(col, 0.85 + 0.2 * dy / max(1, ht))
            put(px, x0 + dx, 31 - dy, col)


def top_tray(px, x0, food, accent, rng):
    for y in range(32):
        for dx in range(8):
            if dx == 7 or y % 11 == 10:
                col = (170, 172, 176) if dx != 7 else (0, 0, 0, 0)
            else:
                col = shade(accent if rng.random() < 0.3 else food, rng.uniform(0.9, 1.1))
            put(px, x0 + dx, y, col)


def col_pills(px, x0, ht, body, cap, rng):
    """Pill bottles three abreast, a pixel wide each: the bottle's colour (amber or white)
    with a white label round its middle and a snap cap on top."""
    for bx in (0, 3, 6):
        for dy in range(ht):
            y = 31 - dy
            for dx in range(2):
                x = x0 + bx + dx
                if dy == ht - 1:
                    col = cap
                elif 1 <= dy < ht - 2 and dy % 3 != 0:
                    col = WHITE
                else:
                    col = body
                put(px, x, y, shade(col, 1.08 if dx == 0 else 0.9))


def col_packs(px, x0, ht, body, flip, rng):
    """Packs of cigarettes in tiers of three texels, two abreast: a white pack with its
    colour's band and the flip top in that colour. No names, no marks."""
    for dy in range(ht):
        y = 31 - dy
        t = dy % 3
        for dx in range(8):
            if dx in (3, 7):
                continue
            col = WHITE if t == 0 else (body if t == 1 else flip)
            if dx % 4 == 0:
                col = shade(col, 0.82)
            put(px, x0 + dx, y, col)


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
    # the bread rack, the self-serve pastry case and the hot food case (every height fits
    # under the shelf above it)
    "bread": [(_c(col_bread_bag, (170, 104, 44), (236, 222, 186), (210, 50, 40)),
               _t(top_bread_bag, (176, 110, 50), (210, 50, 40)), 4),
              (_c(col_bread, (196, 130, 60)), _t(top_bread, (204, 140, 66)), 2.5),
              (_c(col_bread_bag, (120, 70, 34), (176, 126, 76), (44, 120, 60)),
               _t(top_bread_bag, (130, 78, 38), (44, 120, 60)), 4),
              (_c(col_bread, (150, 92, 44)), _t(top_bread, (160, 100, 50)), 3)],
    "pastry": [(_c(col_muffin, (214, 120, 150), (190, 140, 80)),
                _t(top_rounds, (196, 146, 84), (90, 40, 110)), 2.5),
               (_c(col_rounds, (206, 150, 84), (246, 240, 226)),
                _t(top_rounds, (206, 150, 84), (246, 240, 226)), 1.5),
               (_c(col_rounds, (190, 136, 72), (70, 40, 26)),
                _t(top_rounds, (196, 142, 78), (70, 40, 26)), 1.5),
               (_c(col_rounds, (210, 170, 110), (230, 210, 170)),
                _t(top_bagels, (214, 168, 104), (240, 230, 200)), 1.5)],
    "hot_food": [(_c(col_chicken), _t(top_chicken), 3.5),
                 (_c(col_tray, (170, 70, 36), (120, 40, 20)),
                  _t(top_tray, (170, 70, 36), (120, 40, 20)), 2),
                 (_c(col_tray, (226, 176, 80), (190, 130, 50)),
                  _t(top_tray, (226, 176, 80), (190, 130, 50)), 2),
                 (_c(col_tray, (244, 206, 70), (232, 170, 40)),
                  _t(top_tray, (244, 206, 70), (232, 170, 40)), 2)],
    # the pharmacy's shelf wall: generic medicine cartons and pill bottles, no names, no
    # crosses (every height fits under the shelf above it)
    "pharmacy": [(_c(col_small_boxes, WHITE, TEAL), _t(top_boxes, WHITE), 3),
                 (_c(col_pills, (196, 120, 40), WHITE), _t(top_bottles, WHITE), 2),
                 (_c(col_small_boxes, (236, 240, 246), BLUE), _t(top_boxes, WHITE), 4),
                 (_c(col_shampoo, (240, 240, 236), (60, 150, 190)), _t(top_bottles, WHITE), 3.5)],
    "pharmacy_b": [(_c(col_small_boxes, (240, 236, 226), ORANGE), _t(top_boxes, WHITE), 3.5),
                   (_c(col_pills, (238, 238, 232), (40, 120, 200)), _t(top_bottles, BLUE), 2.5),
                   (_c(col_small_boxes, WHITE, PURPLE), _t(top_boxes, WHITE), 2),
                   (_c(col_small_boxes, (230, 244, 232), GREEN), _t(top_boxes, WHITE), 4)],
    # the tobacco case: plain packs and cartons in colour bands
    "packs": [(_c(col_packs, RED, RED), _t(top_boxes, WHITE), 3),
              (_c(col_packs, (40, 90, 170), (40, 90, 170)), _t(top_boxes, WHITE), 3),
              (_c(col_packs, (200, 170, 70), WHITE), _t(top_boxes, (200, 170, 70)), 3),
              (_c(col_packs, (60, 130, 80), (60, 130, 80)), _t(top_boxes, WHITE), 3)],
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


def cage_mesh(colour, size=32):
    """A propane cage's welded mesh: a wire every fourth texel (12 cm), open between, so the
    cylinders show through."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if x % 4 == 0 or y % 4 == 0:
                px[x, y] = shade(colour, 1.12 if (x + y) % 2 else 0.94) + (255,)
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


# --- the fresh departments --------------------------------------------------------------
PARSLEY = ((40, 128, 52), (70, 160, 70), (30, 104, 44))


def tray_grid(draw, seed, size=32, divider=None):
    """Eight trays from above, two rows of four, 8 x 16 texels each, with what draw(i, x, y,
    rng) puts in tray i; divider draws the edge between trays (a butcher's green plastic
    parsley), or None to let the trays run into one another (an ice bed)."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            i = (x // 8) + 4 * (y // 16)
            lx, ly = x % 8, y % 16
            if divider and (lx == 0 or ly in (0, 15)):
                col = divider[(x * 7 + y * 3) % len(divider)]
            else:
                col = draw(i, lx, ly, rng)
            px[x, y] = tuple(clamp(col)) + (255,)
    return img


def meat_cut(i, lx, ly, rng):
    """What a butcher's tray holds: steaks marbled with a fat cap, pork chops, mince, chicken
    breasts, sausages, ribs, lamb chops and a tied roast."""
    fat = (240, 226, 214)
    bone = (236, 228, 204)
    k = 0.92 + 0.14 * rng.random()
    if i == 0:
        if lx == 1 or ly in (1, 8):
            return fat
        return shade(fat, 0.95) if rng.random() < 0.12 else shade((176, 34, 42), k)
    if i == 1:
        if lx == 7 or ly % 7 == 1:
            return fat
        if lx in (2, 3) and ly % 7 in (3, 4):
            return bone
        return shade((222, 138, 136), k)
    if i == 2:
        return shade((196, 58, 62) if rng.random() > 0.2 else (226, 120, 118), k)
    if i == 3:
        return shade((238, 198, 178), 0.9 + 0.12 * math.sin(lx + ly * 0.8))
    if i == 4:
        return shade((150, 70, 50) if ly % 3 == 0 else (200, 112, 82), k)
    if i == 5:
        return bone if lx % 2 == 1 and ly % 14 > 1 else shade((150, 50, 40), k)
    if i == 6:
        if lx in (5, 6) and ly % 5 == 2:
            return bone
        return fat if ly % 5 == 4 else shade((150, 38, 50), k)
    return (236, 226, 196) if ly % 4 == 2 else shade((168, 68, 58), k)


def seafood_on_ice(i, lx, ly, rng):
    """An ice bed from above: crushed ice everywhere, and on it whole fish, salmon fillets,
    shrimp, crab legs, mussels, tuna steaks, trout and lemons."""
    ice = shade((220, 234, 244), 0.92 + 0.14 * rng.random())
    if rng.random() < 0.08:
        ice = (252, 253, 255)
    if i in (0, 6):
        # a fish lying head up the tray: dark back, silver belly, an eye, a forked tail
        cx = 3.5
        t = (ly - 1) / 12.0
        if 0 <= t <= 1:
            w = 2.6 * math.sin(math.pi * min(1.0, t * 1.15))
            if abs(lx - cx) <= w:
                if ly == 3 and lx == 3:
                    return (20, 20, 24)
                back = (60, 72, 90) if i == 0 else (120, 110, 90)
                return back if lx < cx - 0.5 else ((200, 206, 214) if i == 0 else (226, 170, 160))
        if ly >= 13 and abs(lx - cx) <= (ly - 12) * 1.1 and abs(lx - cx) >= (ly - 13) * 0.6:
            return (90, 100, 116)
        return ice
    if i == 1:
        if 1 <= lx <= 6 and ly % 7 not in (0,):
            return (252, 196, 176) if (lx + ly) % 3 == 0 else (242, 130, 98)
        return ice
    if i == 2:
        if rng.random() < 0.6:
            return (246, 142, 112) if rng.random() > 0.3 else (252, 180, 150)
        return ice
    if i == 3:
        if lx in (1, 2, 4, 5):
            return (250, 236, 226) if ly % 5 == 0 else (206, 52, 42)
        return ice
    if i == 4:
        if (lx + ly) % 3 != 0 and rng.random() < 0.75:
            return (40, 40, 60) if rng.random() > 0.2 else (70, 90, 130)
        return ice
    if i == 5:
        if 1 <= lx <= 6 and ly % 6 not in (0, 5):
            return (160, 40, 50) if (lx * ly) % 5 else (120, 26, 36)
        return ice
    if (lx + ly * 2) % 5 < 2:
        return (250, 222, 60) if rng.random() > 0.3 else (70, 160, 70)
    return ice


def heat_lamp(on, size=16):
    """A hot case's heat lamp strip: amber, brightest down its middle; off, a dull brown."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            d = abs(y - 7.5) / 8.0
            col = shade((255, 170, 70), 1.12 - 0.35 * d) if on else shade((74, 52, 44), 1 - 0.2 * d)
            px[x, y] = col + (255,)
    return img


def scale_lcd(size=16):
    """A deli scale's display: dark bezel, a pale green LCD with its weight."""
    img = noisy((40, 44, 42), 801, size, 0.02)
    px = img.load()
    for y in range(3, 12):
        for x in range(1, 15):
            px[x, y] = (150, 188, 140, 255)
    LS.draw_text(img, "1.25", 1, 5, (28, 44, 30))
    return img


def rotisserie_window(on, size=32):
    """The rotisserie's glass door: three spits of golden chickens turning in front of the
    heating element; lit, the whole oven glows amber, off it is dark."""
    img = blank(size)
    px = img.load()
    k = 1.0 if on else 0.42
    for y in range(size):
        for x in range(size):
            base = (150, 80, 36) if on else (40, 34, 30)
            col = shade(base, 1.2 - 0.5 * y / size)
            if y in (2, 3) and 2 <= x <= 29:
                col = (255, 170, 60) if on else (86, 56, 44)
            if (x + y) % 23 in (0, 1):
                col = shade(col, 1.25)
            if x in (0, 31) or y in (0, 31):
                col = (70, 72, 76)
            px[x, y] = col + (255,)
    for sy in (10, 18, 26):
        for x in range(1, 31):
            px[x, sy] = (200, 200, 206, 255)
        for cx in (8.5, 22.5):
            for y in range(sy - 4, sy + 4):
                for x in range(int(cx) - 6, int(cx) + 7):
                    d = math.hypot((x - cx) / 5.2, (y + 0.5 - sy) / 3.3)
                    if d <= 1.0:
                        col = shade((198, 120, 48), (1.25 - 0.45 * d) * k)
                        if y == sy:
                            col = shade((200, 200, 206), k)
                        px[x, y] = tuple(clamp(col)) + (255,)
    return img


def fountain_art(base, emblems, size=32):
    """A fountain drink machine's lit merchandiser: six drinks' tiles in a row, each an
    invented brand's emblem (a wave, a star, a burst, a leaf, a bubble ring, a bolt) on its
    colour; drawn in rows 1 to 9, the part the panel maps (u 1 to 15)."""
    img = noisy(base, hash_seed(str(base)), size, 0.02)
    px = img.load()
    for k, (bg, fg, mark) in enumerate(emblems):
        x0 = 3 + k * 4.5
        for y in range(1, 10):
            for x in range(int(x0), int(x0) + 4):
                lx, ly = x - int(x0), y - 1
                col = bg
                if mark == "wave" and ly in (3, 4, 5) and (lx + ly) % 3 != 0:
                    col = fg
                elif mark == "star" and (lx in (1, 2) or ly == 4) and 2 <= ly <= 6:
                    col = fg
                elif mark == "burst" and (lx + ly) % 2 == 0 and 2 <= ly <= 6:
                    col = fg
                elif mark == "leaf" and abs(lx - 1.5) < 1.6 - abs(ly - 4) * 0.4:
                    col = fg
                elif mark == "ring" and ly in (2, 6) and lx in (1, 2) or (
                        mark == "ring" and lx in (0, 3) and 3 <= ly <= 5):
                    col = fg
                elif mark == "bolt" and ((ly < 4 and lx == 2) or ly == 4 or (ly > 4 and lx == 1)):
                    col = fg
                if ly in (0, 8):
                    col = shade(bg, 0.7)
                px[x, y] = tuple(clamp(col)) + (255,)
    for x in range(size):
        px[x, 10] = (240, 240, 236, 255)
    return img


FOUNTAIN_EMBLEMS = [((150, 30, 36), (250, 250, 250), "wave"),
                    ((30, 30, 34), (240, 60, 50), "star"),
                    ((70, 170, 60), (250, 240, 120), "leaf"),
                    ((240, 150, 30), (250, 250, 250), "burst"),
                    ((40, 110, 200), (250, 250, 250), "ring"),
                    ((250, 214, 40), (40, 40, 44), "bolt")]


def condiments(size=16):
    """A coffee bar's condiment caddy from above, in rows 0 to 3: bins of sugar, sweetener in
    pink, blue and yellow packets, stirrers, black dividers between."""
    img = noisy((36, 36, 38), 802, size, 0.02)
    px = img.load()
    bins = [(246, 244, 238), (236, 140, 170), (80, 140, 220), (246, 214, 70)]
    for x in range(size):
        for y in range(4):
            if x % 4 == 0:
                continue
            col = bins[x // 4]
            if y % 2 == 1:
                col = shade(col, 0.88)
            px[x, y] = col + (255,)
    for x in range(size):
        for y in range(4, size):
            col = (214, 190, 150) if x % 2 == 0 else (36, 36, 38)
            px[x, y] = col + (255,)
    return img


def lids(size=16):
    """A lid organiser from above: stacks of white cup lids in their slots, a black tray."""
    img = noisy((30, 30, 32), 803, size, 0.02)
    px = img.load()
    for cy in (3, 11):
        for cx in (3, 11):
            for y in range(size):
                for x in range(size):
                    d = math.hypot(x + 0.5 - (cx + 0.5), y + 0.5 - (cy + 0.5))
                    if d < 3.3:
                        px[x, y] = shade((244, 244, 240), 1.0 if d > 2.2 or d < 1.2 else 0.86) \
                            + (255,)
    return img


def cup_stack(size=16):
    """A column of paper cups nested in their dispenser: white, a rim every two texels."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            col = (244, 244, 240) if y % 2 else (206, 206, 202)
            px[x, y] = shade(col, 0.94 + 0.06 * math.sin(x * 0.8)) + (255,)
    return img


# Bunches of flowers: each a quadrant (16 x 16 texels) of a 32 px sheet, the heads in its top
# half, stems below, the rest clear; a crossed pair of planes shows one in a bucket.
# (heads [(petal, centre or None)], head radius, wrapped in kraft paper)
BUNCHES = {
    "flowers_a": [([((190, 20, 40), (120, 10, 26))], 1.6, False),               # roses
                  ([((250, 210, 40), None), ((240, 150, 40), None)], 1.4, False),  # tulips
                  ([((250, 196, 30), (90, 50, 20))], 2.4, False),                # sunflowers
                  ([((250, 250, 245), (240, 200, 40))], 1.5, False)],             # daisies
    "flowers_b": [([((240, 120, 160), None), ((250, 176, 204), None)], 1.6, False),  # carnations
                  ([((250, 246, 240), (240, 180, 60))], 1.8, False),              # lilies
                  ([((200, 40, 80), None), ((250, 220, 60), None), ((150, 90, 200), None),
                    ((250, 250, 250), (240, 200, 40))], 1.5, True),               # bouquet
                  ([((130, 80, 190), None), ((100, 60, 160), None)], 1.2, False)],  # irises
}
HEAD_SPOTS = [(3, 3.5), (6.5, 2), (10, 2.5), (13, 3.5), (4.5, 6.5), (8, 5), (11.5, 6.5),
              (8, 1.5), (2, 6.5), (14, 6.5)]


def flower_sheet(name, size=32):
    rng = random.Random(hash_seed(name))
    img = blank(size)
    px = img.load()
    for q, (heads, r, wrapped) in enumerate(BUNCHES[name]):
        ox, oy = 16 * (q % 2), 16 * (q // 2)
        for sx in (4, 6, 7, 9, 10, 12):
            for y in range(6, 16):
                x = sx + (1 if (y % 4 == 0 and sx % 2) else 0)
                px[ox + x, oy + y] = shade((64, 132, 52), 0.85 + 0.25 * rng.random()) + (255,)
            if rng.random() < 0.7:
                ly = rng.randrange(9, 13)
                for d in (1, 2):
                    x = min(15, sx + d)
                    px[ox + x, oy + ly - d + 1] = (84, 156, 64, 255)
        spots = HEAD_SPOTS if r < 2 else HEAD_SPOTS[:4] + [(8, 6)]
        for i, (hx, hy) in enumerate(spots):
            petal, centre = heads[i % len(heads)]
            rr = r * rng.uniform(0.85, 1.1)
            for y in range(int(hy - rr - 1), int(hy + rr + 2)):
                for x in range(int(hx - rr - 1), int(hx + rr + 2)):
                    if not (0 <= x < 16 and 0 <= y < 16):
                        continue
                    d = math.hypot(x + 0.5 - hx, y + 0.5 - hy)
                    if d > rr:
                        continue
                    col = shade(petal, 1.15 - 0.3 * d / rr)
                    if centre and d < rr * 0.42:
                        col = centre
                    px[ox + x, oy + y] = tuple(clamp(col)) + (255,)
        if wrapped:
            for y in range(8, 16):
                half = 7 - (y - 8) * 0.5
                for x in range(16):
                    if abs(x + 0.5 - 8) <= half:
                        col = shade((198, 158, 108), 0.9 + 0.15 * ((x + y) % 3 == 0))
                        px[ox + x, oy + y] = col + (255,)
    return img


# --- the front of the store: pharmacy, tobacco and lottery, outdoor merchandisers, kiosks ------
PHARMACY_TEAL = (22, 122, 122)


def sign_icon(px, kind, x, y, col, size=64):
    """A small pictogram on a sign band at (x, y): an Rx, a two-tone capsule, a star, a coin, a
    camera, a strip of film, a flame or a snowflake. No crosses: a red cross is a protected
    emblem, so the pharmacy has an Rx and a capsule instead."""
    maps = {
        "rx": ["XXX....", "X..X...", "XXX....", "X.XX.X.", "X..XX..", "...X.X."],
        "pill": [".WWOOO.", "WWWOOOO", ".WWOOO."],
        "star": ["..X..", "XXXXX", ".XXX.", ".X.X."],
        "coin": [".XXX.", "XXOXX", "XOXOX", "XXOXX", ".XXX."],
        "camera": [".XX....", "XXXXXXX", "XX.O.XX", "X.OOO.X", "XX.O.XX", "XXXXXXX"],
        "film": ["XOXOX", "XXXXX", "X...X", "XXXXX", "XOXOX"],
        "flame": ["..X..", ".XX..", ".XXX.", "XXOXX", "XOOOX", ".XXX."],
        "snow": ["X.X.X", ".XXX.", "XXOXX", ".XXX.", "X.X.X"],
    }
    other = {"pill": (236, 120, 40), "coin": (250, 236, 150), "camera": (40, 40, 44),
             "film": (40, 40, 44), "flame": (250, 220, 90), "snow": (250, 250, 250)}
    for dy, row in enumerate(maps[kind]):
        for dx, c in enumerate(row):
            if c == ".":
                continue
            put(px, x + dx, y + dy, col if c in "XW" else other.get(kind, col), size)


def sign_sheet(bands):
    """Four signs on one 64 px sheet, a band 64 x 16 texels each (a 4:1 panel, so its letters
    stay square): up to two lines of the pixel font in fg on bg inside a keyline, and the
    band's pictograms. Every sign of these families shares three sheets."""
    img = blank(64)
    px = img.load()
    for k, (lines, bg, fg, icons, keyline) in enumerate(bands):
        y0 = 16 * k
        rng = random.Random(hash_seed("".join(lines)))
        for y in range(y0, y0 + 16):
            for x in range(64):
                col = shade(bg, 1.0 + rng.uniform(-0.025, 0.025))
                if keyline and (x in (0, 63) or y in (y0, y0 + 15)):
                    col = shade(fg, 0.92)
                put(px, x, y, col, 64)
        tops = [y0 + 5] if len(lines) == 1 else [y0 + 2, y0 + 9]
        for line, top in zip(lines, tops):
            LS.draw_text_centred(img, line, 32, top, fg)
        for kind, x, dy, col in icons:
            sign_icon(px, kind, x, y0 + dy, col or fg)
    return img


WHITE_SIGN = (250, 250, 250)
SIGNS = {
    "store_signs_a": [
        (["PHARMACY"], PHARMACY_TEAL, WHITE_SIGN, [("rx", 6, 5, None), ("pill", 52, 6, None)],
         True),
        (["PRIVATE", "CONSULTATION"], PHARMACY_TEAL, WHITE_SIGN, [], True),
        (["PRESCRIPTION", "DROP OFF"], PHARMACY_TEAL, WHITE_SIGN, [], True),
        (["PRESCRIPTION", "PICK UP"], PHARMACY_TEAL, WHITE_SIGN, [], True),
    ],
    "store_signs_b": [
        (["WE CHECK ID", "21 AND OVER"], (150, 28, 32), WHITE_SIGN, [], True),
        (["LUCKY CITY", "LOTTERY"], (84, 40, 140), (250, 214, 60),
         [("star", 5, 5, None), ("star", 54, 5, None)], True),
        (["JACKPOT", "12 000 000"], (16, 16, 18), (255, 170, 40), [], False),
        (["PROPANE", "EXCHANGE"], (176, 34, 34), WHITE_SIGN,
         [("flame", 8, 5, None), ("flame", 51, 5, None)], True),
    ],
    "store_signs_c": [
        (["FIREWOOD", "BUNDLES"], (46, 84, 46), (242, 228, 196), [], True),
        (["COIN COUNTER"], (36, 86, 170), WHITE_SIGN,
         [("coin", 2, 6, (226, 180, 40)), ("coin", 57, 6, (226, 180, 40))], True),
        (["PHOTO", "PRINTS"], (226, 110, 30), WHITE_SIGN,
         [("camera", 9, 5, None), ("camera", 48, 5, None)], True),
        (["MOVIES", "NEW RELEASES"], (70, 36, 110), WHITE_SIGN,
         [("film", 4, 3, None), ("film", 55, 3, None)], True),
    ],
}


def kiosk_screen(kind, size=16):
    """A kiosk's lit touchscreen: the coin counter's running total, the photo kiosk's grid of
    pictures, the movie kiosk's row of covers, the lottery terminal's game keys."""
    rng = random.Random(hash_seed(kind))
    img = noisy((230, 234, 240), hash_seed(kind), size, 0.01)
    px = img.load()
    head = {"coins": (36, 86, 170), "photo": (226, 110, 30), "movies": (70, 36, 110),
            "lotto": (84, 40, 140)}[kind]
    for y in range(size):
        for x in range(size):
            col = None
            if y < 3:
                col = head
            elif kind == "coins":
                if 12 <= y <= 14 and 4 <= x <= 11:
                    col = (60, 170, 80)
            elif kind == "photo":
                if 4 <= y <= 14 and x % 5 != 0 and (y - 4) % 4 != 3 and 1 <= x <= 14:
                    col = rng.choice(((200, 150, 120), (90, 140, 200), (80, 160, 90),
                                      (220, 190, 150), (150, 100, 160), (240, 200, 90)))
            elif kind == "movies":
                if 4 <= y <= 11 and x % 4 != 0:
                    col = ((180, 40, 50), (40, 60, 120), (30, 30, 34), (220, 150, 40))[x // 4]
                    if y in (9, 10):
                        col = shade(col, 1.4)
                elif y == 13 and 2 <= x <= 13:
                    col = (60, 170, 80)
            elif kind == "lotto":
                if 4 <= y <= 14 and x % 4 != 0 and (y - 4) % 4 != 3:
                    col = ((250, 214, 60), (60, 170, 80), (40, 110, 200), (220, 60, 60))[
                        ((x // 4) + (y - 4) // 4) % 4]
            if col:
                px[x, y] = tuple(clamp(col)) + (255,)
    if kind == "coins":
        LS.draw_text_centred(img, "9.75", 8, 5, (30, 34, 40))
    return img


# Invented scratch games on the lottery dispenser: (background, stripe, symbol colour, symbol)
TICKETS = [((236, 70, 60), (250, 214, 60), (250, 250, 250), "7"),
           ((60, 150, 80), (240, 240, 220), (250, 214, 60), "clover"),
           ((40, 100, 200), (160, 210, 250), (250, 250, 250), "star"),
           ((250, 190, 40), (230, 110, 30), (120, 50, 20), "coin")]


def lottery_tickets(size=32):
    """The dispenser's four games, each a column 8 texels wide of fan-folded tickets hanging
    from its slot: a ticket every 8 rows with its game's colour, a scratch panel in silver,
    its symbol and the perforation to the next."""
    img = blank(size)
    px = img.load()
    for c, (bg, stripe, sym, kind) in enumerate(TICKETS):
        for y in range(size):
            for dx in range(8):
                x = 8 * c + dx
                ly = y % 8
                col = bg
                if dx == 7:
                    col = (30, 30, 34)
                elif ly == 7:
                    col = shade(bg, 0.7) if dx % 2 else (240, 240, 236)
                elif ly == 0:
                    col = stripe
                elif 4 <= ly <= 5 and 1 <= dx <= 5:
                    col = (196, 198, 204) if (dx + ly) % 2 else (170, 172, 178)
                elif 1 <= ly <= 3 and 2 <= dx <= 4:
                    mark = {"7": ((0, 0), (1, 0), (2, 0), (2, 1), (1, 2)),
                            "clover": ((1, 0), (0, 1), (2, 1), (1, 1), (1, 2)),
                            "star": ((1, 0), (0, 1), (1, 1), (2, 1), (0, 2), (2, 2)),
                            "coin": ((1, 0), (0, 1), (2, 1), (1, 2))}[kind]
                    if (dx - 2, ly - 1) in mark:
                        col = sym
                put(px, x, y, col)
    return img


def movie_posters(size=32):
    """Four invented films' posters, two by two: a sky, a silhouette against it and a title
    bar."""
    rng = random.Random(8601)
    img = blank(size)
    px = img.load()
    skies = [((30, 40, 90), (220, 100, 60)), ((10, 10, 20), (60, 170, 200)),
             ((120, 20, 30), (250, 180, 60)), ((30, 90, 60), (200, 230, 150))]
    for q, (top, bottom) in enumerate(skies):
        ox, oy = 16 * (q % 2), 16 * (q // 2)
        hill = [11 + int(2.5 * math.sin((x + q * 3) * 0.5)) for x in range(16)]
        for y in range(16):
            for x in range(16):
                t = y / 15.0
                col = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
                if y >= hill[x]:
                    col = (16, 16, 20)
                if y >= 13:
                    col = (240, 240, 236) if y == 14 and 3 <= x <= 12 and x % 3 else (12, 12, 14)
                if x in (0, 15) or y == 0:
                    col = (40, 40, 44)
                px[ox + x, oy + y] = tuple(clamp(shade(col, 1 + rng.uniform(-0.03, 0.03)))) \
                    + (255,)
    return img


def coin_tray(size=16):
    """The coin counter's tray from above: brushed steel with a scatter of silver, copper and
    brass coins, the slot at the back."""
    rng = random.Random(8611)
    img = R.metal((178, 182, 188), 8612, size)
    px = img.load()
    for _ in range(18):
        cx, cy = rng.randrange(1, size - 1), rng.randrange(1, size - 3)
        col = rng.choice(((210, 212, 216), (184, 110, 70), (214, 180, 90)))
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if rng.random() < 0.85:
                px[min(size - 1, cx + dx), cy + dy] = shade(col, 1.1 if dx == dy == 0 else 0.95) \
                    + (255,)
    for x in range(3, size - 3):
        px[x, size - 2] = (20, 20, 22, 255)
    return img


def ice_door(body, letter, size=32):
    """An ice merchandiser's door: CITY ICE (an invented brand) over snowflakes, BAGGED below,
    on the cabinet's colour, a frosty band across the top."""
    img = noisy(body, hash_seed(str(body)), size, 0.015)
    px = img.load()
    for y in range(0, 4):
        for x in range(size):
            px[x, y] = shade(letter, 1.0 - 0.04 * y) + (255,)
    LS.draw_text_centred(img, "CITY", 16, 7, letter)
    LS.draw_text_centred(img, "ICE", 16, 14, letter, scale=2)
    LS.draw_text_centred(img, "BAGGED", 16, 26, letter)
    for x, y in ((2, 9), (26, 9), (3, 19), (25, 19)):
        sign_icon(px, "snow", x, y, shade(letter, 0.85), size)
    for y in range(size):
        for x in range(size):
            if x in (0, size - 1) or y == size - 1:
                px[x, y] = shade(body, 0.8) + (255,)
    return img


def firewood_ends(size=16):
    """A bundle of firewood end on: split logs' end grain, pale with a ring or two and a rim of
    bark, dark gaps between, in a clear wrap."""
    rng = random.Random(8621)
    img = noisy((40, 30, 22), 8622, size, 0.05)
    px = img.load()
    logs = [(3.5, 3.5, 3.2), (10.5, 3, 3.4), (4, 10.5, 3.4), (11, 10.5, 3.3), (7.5, 7, 2.4),
            (14.5, 8, 1.8), (1, 7.5, 1.6), (7.5, 14.5, 1.8)]
    for cx, cy, r in logs:
        tone = rng.uniform(0.9, 1.1)
        for y in range(size):
            for x in range(size):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d > r:
                    continue
                if d > r - 0.8:
                    col = (86, 60, 40)
                else:
                    col = shade((214, 170, 112), tone * (0.93 if int(d * 1.6) % 2 else 1.03))
                px[x, y] = tuple(clamp(col)) + (255,)
    return img


def firewood_bark(size=16):
    """The bundle's side: rough split logs lying lengthwise under the wrap's sheen."""
    rng = random.Random(8623)
    img = blank(size)
    px = img.load()
    for y in range(size):
        log = y // 4
        base = shade((110, 80, 54), 0.9 + 0.2 * ((log * 7) % 3) / 2.0)
        for x in range(size):
            col = shade(base, 1.0 + rng.uniform(-0.08, 0.08))
            if y % 4 == 3:
                col = (46, 34, 26)
            if (x + 2 * y) % 13 == 0:
                col = shade(col, 1.25)
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
    # the fresh departments
    "meat_trays": lambda: tray_grid(meat_cut, 811, divider=PARSLEY),
    "seafood_ice": lambda: tray_grid(seafood_on_ice, 812),
    "heat_lamp_on": lambda: heat_lamp(True),
    "heat_lamp_off": lambda: heat_lamp(False),
    "scale_lcd": scale_lcd,
    "rotisserie_window_on": lambda: rotisserie_window(True),
    "rotisserie_window_off": lambda: rotisserie_window(False),
    "fountain_art_red": lambda: fountain_art((176, 30, 34), FOUNTAIN_EMBLEMS),
    "fountain_art_blue": lambda: fountain_art((30, 70, 150), FOUNTAIN_EMBLEMS),
    "condiments": condiments,
    "lids": lids,
    "cup_stack": cup_stack,
    "flowers_a": lambda: flower_sheet("flowers_a"),
    "flowers_b": lambda: flower_sheet("flowers_b"),
    "header_hot_on": lambda: header("HOT FOOD", (176, 62, 28), True),
    "header_hot_off": lambda: header("HOT FOOD", (176, 62, 28), False),
    "header_flowers_on": lambda: header("FRESH FLOWERS", (150, 56, 110), True),
    "header_flowers_off": lambda: header("FRESH FLOWERS", (150, 56, 110), False),
    "bread_sign": lambda: label_text(["BREAD"], (120, 74, 36), (246, 236, 214), top=13),
    "coffee_sign": lambda: label_text(["COFFEE"], (66, 42, 30), (240, 222, 190), top=13),
    # the front of the store
    "laminate_teal": lambda: R.laminate(PHARMACY_TEAL, 8631),
    "plastic_purple": lambda: noisy((88, 44, 130), 8632, 16, 0.025),
    "plastic_yellow": lambda: noisy((244, 196, 40), 8633, 16, 0.025),
    "glass_smoke": lambda: glass(120, (70, 74, 82)),
    "mesh_cage": lambda: cage_mesh((92, 96, 100)),
    "screen_coins": lambda: kiosk_screen("coins"),
    "screen_photo": lambda: kiosk_screen("photo"),
    "screen_movies": lambda: kiosk_screen("movies"),
    "screen_lotto": lambda: kiosk_screen("lotto"),
    "lottery_tickets": lottery_tickets,
    "movie_posters": movie_posters,
    "coin_tray": coin_tray,
    "ice_door_white": lambda: ice_door((238, 242, 246), (30, 90, 170)),
    "ice_door_blue": lambda: ice_door((40, 96, 180), (246, 248, 250)),
    "firewood_ends": firewood_ends,
    "firewood_bark": firewood_bark,
}
for _sheet, _bands in SIGNS.items():
    TEXTURES[_sheet] = (lambda b=_bands: sign_sheet(b))
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
    # the fresh departments
    "meat": MT("meat_trays"), "ice": MT("seafood_ice"), "lcd": MT("scale_lcd"),
    "flowers_a": MT("flowers_a"), "flowers_b": MT("flowers_b"), "bucket": T("metal_steel"),
    "art": MT("fountain_art_red"), "condiments": MT("condiments"), "lids": MT("lids"),
    "cups": MT("cup_stack"), "white": MT("case_white"),
    # the front of the store
    "signs_a": MT("store_signs_a"), "signs_b": MT("store_signs_b"),
    "signs_c": MT("store_signs_c"), "tickets": MT("lottery_tickets"),
    "door": MT("ice_door_white"), "tank": MT("plastic_grey"), "bark": MT("firewood_bark"),
    "ends": MT("firewood_ends"), "accent": MT("plastic_yellow"), "coins": MT("coin_tray"),
    "posters": MT("movie_posters"),
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
    body = reach_in_frame()
    levels = [3.0, 9.5, 16.0, 22.5]
    for i, y in enumerate(levels):
        if i:
            body += [el([0.5, y - 0.4, 3.4], [15.5, y, 15.25], "shelf", ("up", "down")),
                     strip_el(0.5, 15.5, y - 0.9, y, 3.1, 3.4)]
        key = "stock_a" if i % 2 == 0 else "stock_b"
        kind = stock_a if key == "stock_a" else stock_b
        body += stock_row(0.75, 15.25, y, 3.75, 15.25, heights(kind), 100 + i, key)
    return body, REACH_IN_END


def reach_in_frame():
    """The reach-in's cabinet, door, header and lights, without what is on show behind it."""
    return [el([0, 0, 15.75], [16, 32, 16], "shell", ("south",)),
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


REACH_IN_END = [el([0, 0, 2], [0.5, 32, 16], "shell", ("west", "north", "up", "down"))]


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
# The fresh departments: butcher and seafood, bakery and hot food, floral, coffee and drinks
# ------------------------------------------------------------------------------------------
# The butcher's and the fishmonger's cases have the deli case's section (its base, curved glass
# and canopy), so a deli, a butcher and a seafood case set in a line are one service counter.
SERVICE_SEGS = [(4.0, 0), (4.0, 22.5), (3.0, 45)]
_, (SERVICE_TOP, _) = curved_glass(0, 16, 9, 2.6, SERVICE_SEGS, top_to=11)


def top_scale(y):
    """A service scale standing on the case's top: a white body with a steel platter over the
    glass, its display on a post at the back, read from both sides."""
    return [el([10, y, 7], [15, y + 1.25, 11.5], "white"),
            el([10.4, y + 1.25, 7.4], [14.6, y + 1.5, 11.1], "steel", NO_DOWN),
            el([11.75, y + 1.25, 11.5], [13.25, y + 2.5, 12.25], "white", SIDES),
            el([10.75, y + 2.5, 11.25], [14.25, y + 4.5, 12.5], "white"),
            ALL_UV(el([11, y + 2.75, 11.2], [14, y + 4.25, 11.25], "lcd", ("north",)), ["north"]),
            ALL_UV(el([11, y + 2.75, 12.5], [14, y + 4.25, 12.55], "lcd", ("south",)), ["south"])]


def price_cards(y, z, xs=(2.5, 10.5)):
    """Price cards standing in the front trays, facing the shopper."""
    return [el([x, y, z], [x + 3.2, y + 1.2, z + 0.1], "price", ("north",),
               uv={"north": [0, 4, 16, 10]}) for x in xs]


BUTCHER_INSIDE = ([el([0, 8.5, 3.2], [16, 8.6, 12], "meat", ("up",), uv={"up": [0, 0, 16, 16]}),
                   el([0, 12.8, 6.5], [16, 13, 12], "glass", ("up", "down")),
                   el([0, 13, 6.7], [16, 13.1, 11.8], "meat", ("up",), uv={"up": [0, 0, 16, 8]}),
                   el([0, 12, 12.5], [16, 12.5, 16], "steel", ("up", "north", "south", "down"))]
                  + price_cards(8.6, 3.5) + top_scale(SERVICE_TOP))
BUTCHER, BUTCHER_END, _ = service_case(9, SERVICE_SEGS, 11, BUTCHER_INSIDE, 11, 8.4)
# The ice bed slopes up from the glass, as the produce stand's bed does, its back closed by a
# wall the server sees.
_ICE_TILT = ("x", -22.5, [8, 8.5, 3.2])
SEAFOOD_INSIDE = ([el([0, 8.5, 3.2], [16, 9.5, 12.2], "ice", ("up",), uv={"up": [0, 0, 16, 16]},
                      rot=_ICE_TILT),
                   el([0, 8.4, 11.1], [16, 12.9, 11.5], "liner", ("south", "up")),
                   el([0, 12, 12.5], [16, 12.5, 16], "steel", ("up", "north", "south", "down"))]
                  + price_cards(9.85, 3.8) + top_scale(SERVICE_TOP))
SEAFOOD, SEAFOOD_END, _ = service_case(9, SERVICE_SEGS, 11, SEAFOOD_INSIDE, 11, 8.4)

# The bread rack: three shelves stepping back as they rise, each with a lip, bagged and crusty
# loaves on the outer two and the bakery's on the middle, a BREAD sign over the back.
BREAD_LEVELS = ((1.5, 2.5), (6.5, 5.5), (11.5, 8.5))


def bread_rack():
    body = [board([0, 0, 15], [16, 19.5, 15.75], grain="v",
                  faces=("north", "south", "up", "east", "west")),
            el([0, 0, 2.25], [16, 0.75, 2.75], "kick", ("north", "up"))]
    for i, (y, zf) in enumerate(BREAD_LEVELS):
        body += [board([0, y - 0.75, zf], [16, y, 15], faces=("north", "up", "down")),
                 board([0, y, zf], [16, y + 1, zf + 0.5], faces=("north", "south", "up"))]
        key, kind = ("stock_b", "bakery") if i == 1 else ("stock_a", "bread")
        body += stock_row(0, 16, y, zf + 0.75, 15, heights(kind), 900 + i, key)
    body.append(el([2, 15.5, 14.9], [14, 19.5, 15], "sign", ("north",),
                   uv={"north": [2, 5.5, 14, 9.5]}))
    return body


BREAD_RACK = bread_rack()
BREAD_RACK_END = [board([0, 0, 2.25], [0.75, 4, 15.75], grain="v"),
                  board([0, 4, 5.25], [0.75, 9, 15.75], grain="v"),
                  board([0, 9, 8.25], [0.75, 19.5, 15.75], grain="v")]

# The self-serve pastry case: a cabinet with an acrylic case on it, two tiers of trays behind
# doors hinged at the top, each door with its knob.
PASTRY_BODY = ([board([0, 1, 2.5], [16, 7.25, 3.25], grain="v", faces=("north",)),
                el([0, 0, 3], [16, 1, 13.5], "kick", ("north", "south")),
                board([0, 1, 13], [16, 7.25, 13.75], grain="v", faces=("south",)),
                board([0, 7.25, 2.25], [16, 8, 14], faces=("north", "south", "up", "down"))]
               + stock_row(0.25, 15.75, 8, 3.75, 13, heights("pastry"), 910, "stock_a")
               + [el([0, 11.75, 3.25], [16, 12, 13.25], "clear", ("up", "down"))]
               + stock_row(0.25, 15.75, 12, 4, 13, heights("bakery_b"), 911, "stock_b")
               + [el([0, 8, 2.75], [16, 16, 3], "clear", ("north", "south")),
                  el([0, 8, 13.25], [16, 16, 13.5], "clear", ("north", "south")),
                  el([0, 15.75, 3], [16, 16, 13.25], "clear", ("up", "down")),
                  el([0, 8, 2.5], [16, 8.5, 2.75], "case", ("north", "up")),
                  el([0, 11.75, 2.5], [16, 12.25, 2.75], "case", ("north", "up", "down")),
                  el([0, 15.75, 2.5], [16, 16.25, 3], "case", ("north", "up", "down"))]
               + [el([x, y, 2.1], [x + 1, y + 0.75, 2.5], "chrome", NO_BACK)
                  for x in (3.5, 11.5) for y in (8.75, 12.5)])
PASTRY_END = [board([0, 0, 2.25], [0.75, 8, 14], grain="v"),
              el([0, 8, 2.5], [0.5, 16.25, 3], "case", ("west", "north", "east", "up")),
              el([0, 8, 3], [0.25, 15.75, 13.25], "clear", ("west", "east")),
              el([0, 8, 13.25], [0.5, 16.25, 13.75], "case", ("west", "south", "east", "up"))]

# The hot food case: the bakery case's straight glass on a steel base, a heated well and a
# shelf of rotisserie chickens and hot trays under amber heat lamps, a lit HOT FOOD header.
HOT_INSIDE = (stock_row(0, 16, 9, 3.6, 12, heights("hot_food"), 920, "stock_a",
                        pattern=[0, 1, 0, 2])
              + [el([0, 14.25, 5], [16, 14.5, 13], "steel", ("north", "up", "down")),
                 el([0.5, 14, 5.5], [15.5, 14.25, 6.5], "glow", ("down",))]
              + stock_row(0, 16, 14.5, 5.5, 12.5, heights("hot_food"), 921, "stock_b",
                          pattern=[0, 3, 0, 1])
              + [el([0, 20, 2.4], [16, 22.25, 3.4], "shell", ("south", "up", "east", "west")),
                 header_el(0.5, 15.5, 20.25, 22, 2.4)])
HOT_FOOD, HOT_FOOD_END, _ = service_case(9, [(11.0, 0)], 12.5, HOT_INSIDE, 12.5, 8.9)

# The rotisserie oven, on the counter: a steel box on feet, its glass door (the window, drawn
# with the spits of chickens, lit while it roasts), a bar handle and a timer knob.
ROTISSERIE = (A.feet(1.5, 14.5, 4, 12)
              + [el([1, 0.25, 4], [15, 12, 12.5], "shell", ("south", "east", "west", "up")),
                 el([1, 0.25, 3.5], [15, 12, 4], "trim", NO_BACK),
                 ALL_UV(el([2.25, 1.5, 3.45], [13.75, 10.75, 3.5], "glow", ("north",)),
                        ["north"]),
                 el([3.5, 11, 2.5], [12.5, 11.5, 3], "handle"),
                 el([3.5, 11, 3], [4, 11.5, 3.5], "handle", SIDES),
                 el([12, 11, 3], [12.5, 11.5, 3.5], "handle", SIDES),
                 el([12.5, 12, 5], [14, 12.75, 6.5], "rubber", NO_DOWN)])


def airpot(cx, cz):
    """A pump airpot: a steel body, a black lid, the pump head on it and a spout at the front."""
    return (R.octagon(cx, cz, 1.7, 0.75, 7.75, "steel", caps=())
            + R.octagon(cx, cz, 1.8, 7.75, 9.1, "rubber", caps=("up",))
            + [el([cx - 0.4, 5.5, cz - 2.4], [cx + 0.4, 6.25, cz - 1.6], "rubber", NO_BACK),
               el([cx - 0.75, 9.1, cz - 0.75], [cx + 0.75, 9.6, cz + 0.75], "rubber", NO_DOWN)])


# The commercial brewer: a base tray, the tower at the back, the hood over two brew heads, two
# airpots under them, a control strip and a lamp lit while it brews.
BREWER = ([el([2, 0, 3.5], [14, 0.75, 12.5], "trim"),
           el([2, 0.75, 9.5], [14, 13, 12.5], "shell", ("north", "south", "east", "west", "up")),
           el([2, 10.5, 3.5], [14, 13, 9.5], "shell", ("north", "east", "west", "up", "down")),
           el([3.25, 9.75, 5], [6.75, 10.5, 8.5], "rubber", ("north", "east", "west", "down")),
           el([9.25, 9.75, 5], [12.75, 10.5, 8.5], "rubber", ("north", "east", "west", "down")),
           el([5, 11, 3.45], [11, 12.5, 3.5], "control", ("north",)),
           ALL_UV(el([11.5, 11.5, 3.4], [12.25, 12.25, 3.45], "glow", ("north",)), ["north"])]
          + airpot(5, 6.75) + airpot(11, 6.75))

# The fountain drink machine: a cabinet with its lit merchandiser of six invented drinks, six
# valves with their levers either side of the ice chute, a splash back and a drip tray.
FOUNTAIN = ([el([1, 1, 5], [15, 13, 12.5], "shell", ("south", "east", "west", "up")),
             el([1, 8.5, 4.5], [15, 13, 5], "shell", ("north", "east", "west", "up", "down"),
                {"north": "art"}, uv={"north": [1, 0, 15, 4.5]}),
             el([1, 1, 4.9], [15, 8.5, 5], "rubber", ("north",)),
             el([1, 0, 2.5], [15, 1, 12.5], "trim"),
             el([1.5, 1, 3], [14.5, 1.05, 4.75], "grille", ("up",)),
             el([1.5, 8.25, 4.4], [14.5, 8.5, 4.5], "glow", ("north", "down")),
             el([7.25, 4.5, 3.75], [8.75, 8.25, 4.9], "shell", NO_BACK),
             el([7.5, 4.25, 4], [8.5, 4.5, 4.75], "rubber", ("north", "east", "west", "down"))]
            + [e for x in (1.75, 3.5, 5.25, 9.5, 11.25, 13)
               for e in (el([x, 6.25, 3.5], [x + 1.25, 8.25, 4.9], "chrome", NO_BACK),
                         el([x + 0.4, 5.5, 3.9], [x + 0.85, 6.25, 4.35], "chrome",
                            ("north", "east", "west", "down")),
                         el([x + 0.25, 5.25, 3.25], [x + 1, 6.25, 3.5], "rubber", NO_BACK))])


# The coffee station's counters: a cabinet a countertop high with a stone top, joining into one
# station, a back panel along it.
def drink_counter(doors=True):
    out = [board([0, 1, 1.75], [16, 13.75, 2.5], grain="v", faces=("north",)),
           el([0, 0, 2.25], [16, 1, 14.5], "kick", ("north", "south")),
           board([0, 1, 14], [16, 13.75, 14.75], grain="v", faces=("south",)),
           el([0, 13.75, 1], [16, 14.5, 15.25], "counter",
              ("north", "south", "east", "west", "up", "down")),
           board([0, 14.5, 15.25], [16, 20, 15.75], grain="v",
                 faces=("north", "south", "east", "west", "up"))]
    if doors:
        out += [el([7.9, 1.5, 1.7], [8.1, 13.25, 1.75], "case", ("north",)),
                el([6.5, 11, 1.25], [7.25, 11.75, 1.75], "chrome", NO_BACK),
                el([8.75, 11, 1.25], [9.5, 11.75, 1.75], "chrome", NO_BACK)]
    return out


DRINK_END = [board([0, 0, 1.75], [0.75, 13.75, 14.75], grain="v",
                   faces=("west", "north", "south", "east"))]
COFFEE_BAR = (drink_counter()
              + [el([0.5, 14.5, 12.5], [15.5, 16, 15.25], "case", ("north", "east", "west", "up"),
                    {"up": "condiments"}, uv={"up": [0, 0, 15, 2.75]}),
                 el([2, 15.75, 15.2], [14, 19.75, 15.25], "sign", ("north",),
                    uv={"north": [2, 5.5, 14, 9.5]})])
CUP_COUNTER = (drink_counter(doors=False)
               + [el([4, 8.5, 1.7], [12, 10.75, 1.75], "rubber", ("north",)),
                  el([12.5, 14.5, 3], [15.5, 16.5, 6.5], "case", ("north", "east", "west", "up"),
                     {"up": "lids"}, uv={"up": [0, 0, 16, 16]}),
                  el([12.5, 14.5, 6.5], [15.5, 17.5, 10], "case", ("north", "east", "west", "up",
                                                                   "south"),
                     {"up": "lids"}, uv={"up": [0, 0, 16, 16]}),
                  el([2, 14.5, 10.5], [9, 15.75, 13], "white", NO_DOWN)]
               + [e for cx, r in ((3, 1.3), (6.25, 1.6), (9.75, 1.9))
                  for e in R.octagon(cx, 6, r, 14.5, 19, "cups", caps=("up",))]
               + [el([x, 15.75, 11.5], [x + 0.3, 17.5, 11.8], "plastic", SIDES + ("up",))
                  for x in (2.6, 3.6, 4.6, 5.6, 6.6, 7.6, 8.4)])


# Floral: buckets of flowers, each bunch a crossed pair of planes standing in its bucket.
def bucket(cx, cz, y0, r=1.9, h=4.0):
    return (R.octagon(cx, cz, r, y0, y0 + h, "bucket", caps=())
            + [el([cx - r * 0.7, y0 + h - 1, cz - r * 0.7], [cx + r * 0.7, y0 + h - 0.9,
                                                             cz + r * 0.7], "water", ("up",))])


def bunch(cx, cz, y0, h, key, q, w=5.0):
    """A bunch of flowers: quadrant q of the sheet on two planes crossed at 45 degrees."""
    uv = [8 * (q % 2), 8 * (q // 2), 8 * (q % 2) + 8, 8 * (q // 2) + 8]
    return [el([cx - w / 2, y0, cz], [cx + w / 2, y0 + h, cz], key, ("north", "south"),
               uv={"north": uv, "south": uv}, rot=("y", a, [cx, y0, cz])) for a in (45, -45)]


FLOWER_TIERS = ((3.5, 1.5, 6), (7.5, 6, 10.5), (11.5, 10.5, 15))


def flower_stand():
    out = []
    for i, (y, z0, z1) in enumerate(FLOWER_TIERS):
        out.append(el([1, y - 0.75, z0], [15, y, z1], "frame", ("north", "up", "down")))
        for x in (0.25, 15):
            out.append(el([x, 0, z0], [x + 0.75, y, z1], "frame",
                          ("north", "east", "west", "up") + (("south",) if i == 2 else ())))
        cz = (z0 + z1) / 2
        for k, cx in enumerate((3.75, 8, 12.25)):
            out += bucket(cx, cz, y, 1.9, 3.5)
            key = "flowers_a" if (i + k) % 2 == 0 else "flowers_b"
            out += bunch(cx, cz, y + 1, 7.5, key, (i * 3 + k) % 4)
    # a back panel closes the steps from behind
    out.append(el([1, 0, 14.25], [15, 10.75, 15], "frame", ("south", "north")))
    return out


FLOWER_STAND = flower_stand()


def floral_interior():
    """The floral cooler's shelves: on each, a front row of buckets and a back row raised on a
    step, bunches and wrapped bouquets in them."""
    out = []
    for i, y in enumerate((3.0, 11.0, 19.0)):
        if i:
            out += [el([0.5, y - 0.4, 3.4], [15.5, y, 15.25], "shelf", ("up", "down")),
                    strip_el(0.5, 15.5, y - 0.9, y, 3.1, 3.4)]
        out.append(el([0.5, y, 10], [15.5, y + 1.75, 15.25], "shelf", ("north", "up")))
        for k, cx in enumerate((3.5, 8, 12.5)):
            for row, (cz, dy, h) in enumerate(((6.5, 0.0, 6.0), (12.5, 1.75, 4.75))):
                out += bucket(cx, cz, y + dy, 1.7, 3.0)
                key = "flowers_a" if (i + k + row) % 2 == 0 else "flowers_b"
                out += bunch(cx, cz, y + dy + 1, h, key, (i + 2 * k + row) % 4, w=4.5)
    return out


# ------------------------------------------------------------------------------------------
# The front of the store: the pharmacy, the tobacco case and the lottery, the outdoor
# merchandisers and the kiosks
# ------------------------------------------------------------------------------------------
def sign_print(x0, x1, y0, y1, z, key, band, face="north"):
    """A sign's printed face: band 0 to 3 of a sign sheet (64 x 16 texels, a 4:1 panel, so
    x1 - x0 should be four times y1 - y0), a hair proud of the plane z."""
    uv = {face: [0, 4 * band, 16, 4 * band + 4]}
    if face == "north":
        return el([x0, y0, z - 0.05], [x1, y1, z], key, (face,), uv=uv)
    return el([x0, y0, z], [x1, y1, z + 0.05], key, (face,), uv=uv)


def sign_panel(x0, x1, y0, y1, zc, key, band, t=0.5, both=True):
    """A sign in a frame a quarter pixel wider than its print, t thick about z zc, printed on
    its front (and its back, reading the same way round, when both)."""
    out = [el([x0 - 0.25, y0 - 0.25, zc - t / 2], [x1 + 0.25, y1 + 0.25, zc + t / 2], "case")]
    out.append(sign_print(x0, x1, y0, y1, zc - t / 2, key, band))
    if both:
        out.append(sign_print(x0, x1, y0, y1, zc + t / 2, key, band, "south"))
    return out


# The pharmacy counter is the office's reception desk (its transaction counter a block high
# over the work surface), and a sign on a post at the back of its top reads PRESCRIPTION DROP
# OFF or PICK UP to both sides.
def pharmacy_post(band):
    return ([el([7.6, 16, 4.1], [8.4, 21, 4.9], "chrome", SIDES),
             el([6.5, 16, 3.25], [9.5, 16.25, 5.75], "chrome", NO_DOWN)]
            + sign_panel(2, 14, 21, 24, 4.5, "signs_a", band))


RECEPTION_ITEM = O.RECEPTION_BODY + O.RECEPTION_END + mirror_x(O.RECEPTION_END)


def hanging_sign(key, band):
    """A sign hung from the ceiling on two rods, printed both sides."""
    return ([el([2.5, 12.75, 7.75], [3, 16, 8.25], "chrome", SIDES),
             el([13, 12.75, 7.75], [13.5, 16, 8.25], "chrome", SIDES)]
            + sign_panel(1, 15, 9, 12.5, 8, key, band))


# The pharmacy's shelf wall: open shelves on a back panel, a shelf every third of a block with
# its price strip, cartons and pill bottles on each. It has no top or base of its own, so
# blocks stacked on one another are one wall; the end panels come only at a run's ends.
PSHELF_LEVELS = (0.0, 5.25, 10.5)


def pharmacy_shelf():
    body = [el([0, 0, 15], [16, 16, 15.75], "shell", ("north", "south"))]
    for i, y in enumerate(PSHELF_LEVELS):
        if y:
            body += [el([0, y, 9], [16, y + 0.5, 15], "shell", ("north", "up", "down")),
                     strip_el(0, 16, y - 0.25, y + 0.75, 8.75, 9)]
        else:
            body += [el([0, 0, 9], [16, 0.5, 15], "shell", ("north", "up")),
                     strip_el(0, 16, 0, 1, 8.75, 9)]
        key, kind = ("stock_a", "pharmacy") if i % 2 == 0 else ("stock_b", "pharmacy_b")
        body += stock_row(0, 16, y + 0.5, 9.5, 15, heights(kind), 1000 + i, key)
    return body


PHARMACY_SHELF = pharmacy_shelf()
PHARMACY_SHELF_END = [el([0, 0, 8.5], [0.75, 16, 15.75], "shell", ("west", "east", "north", "up"))]

# The tobacco case, hung on the wall behind the register: a cabinet 40 cm deep with three rows
# of packs and cartons behind sliding smoked-glass doors, a lock where they meet, and a frieze
# over them that carries WE CHECK ID at the run's end on the shopper's left.
TOBACCO_BODY = ([el([0, 0, 15.5], [16, 16, 16], "shell", ("north", "south")),
                 el([0, 0, 9.5], [16, 0.75, 15.5], "shell", ("north", "up", "down")),
                 el([0, 12.25, 9.25], [16, 16, 15.5], "shell", ("north", "up", "down"))]
                + [e for y in (4.5, 8.25)
                   for e in (el([0, y - 0.4, 10], [16, y, 15.5], "shelf", ("up", "down")),
                             strip_el(0, 16, y - 0.9, y, 9.75, 10))]
                + stock_row(0, 16, 0.75, 10.25, 15.5, heights("packs"), 1010, "stock_a")
                + stock_row(0, 16, 4.5, 10.25, 15.5, heights("packs"), 1011, "stock_a")
                + stock_row(0, 16, 8.25, 10.25, 15.5, heights("packs"), 1012, "stock_a")
                + [el([0, 0.75, 9.6], [8.25, 12.25, 9.7], "glass", ("north", "south")),
                   el([7.75, 0.75, 9.8], [16, 12.25, 9.9], "glass", ("north", "south")),
                   el([0, 0.75, 9.3], [16, 1.25, 10], "trim", ("north", "up")),
                   el([0, 11.75, 9.3], [16, 12.25, 10], "trim", ("north", "down")),
                   el([7.75, 1.25, 9.5], [8.25, 11.75, 10], "trim", ("north", "east", "west")),
                   el([7.6, 5.5, 9.2], [8.4, 6.5, 9.5], "chrome", NO_BACK)])
# (the sign goes on the block with no neighbour to its own right: the shopper's left)
_TOBACCO_SIDE = [el([0, 0, 9.25], [0.5, 16, 16], "shell", ("west", "east", "north", "up", "down"))]
TOBACCO_LEFT = _TOBACCO_SIDE
TOBACCO_RIGHT = (mirror_x(_TOBACCO_SIDE)
                 + [sign_print(0.75, 15.25, 12.4, 15.85, 9.25, "signs_b", 0)])

# The lottery ticket dispenser, on the counter: four games of scratch tickets hanging from
# their slots behind a clear front, a tear bar over them, the (invented) LUCKY CITY LOTTERY
# header on top.
LOTTO_DISPENSER = ([el([2, 0, 5], [14, 0.5, 12], "case"),
                    el([2, 0.5, 11.5], [14, 8, 12], "case", ("north", "south", "east", "west",
                                                            "up")),
                    el([2, 0.5, 5], [2.5, 8, 11.5], "case", ("west", "east", "north", "up")),
                    el([13.5, 0.5, 5], [14, 8, 11.5], "case", ("west", "east", "north", "up")),
                    el([2.5, 1, 6], [13.5, 7.5, 6.05], "tickets", ("north",),
                       uv={"north": [0, 0, 16, 9.5]}),
                    el([2.5, 7.5, 5], [13.5, 8, 5.75], "chrome"),
                    el([2.5, 0.5, 5.1], [13.5, 7.5, 5.2], "clear", ("north", "south"))]
                   + [el([x - 0.2, 0.5, 5.2], [x + 0.2, 7.5, 11.5], "case",
                         ("north", "east", "west")) for x in (5.25, 8, 10.75)]
                   + sign_panel(2.25, 13.75, 8.5, 11.375, 11.75, "signs_b", 1, both=False))

# The lottery terminal: a play-slip reader and the ticket slot on the front of its base, the
# clerk's touchscreen tilted towards the back, and a customer display on a post showing the
# (invented) jackpot.
LOTTO_TERMINAL = ([el([3, 0, 5], [13, 3.5, 12.5], "case"),
                   el([4, 3.5, 5.25], [9, 4.25, 8], "case", NO_DOWN),
                   el([4.5, 4.25, 6], [8.5, 4.3, 6.5], "rubber", ("up",)),
                   el([10, 3.5, 6], [12.5, 3.55, 7.5], "rubber", ("up",)),
                   ALL_UV(el([3.5, 3.5, 10.5], [12.5, 9, 11.5], "case", ALL, {"south": "screen"},
                             rot=("x", -22.5, [8, 3.5, 11])), ["south"]),
                   el([13, 0, 9], [14, 12, 10], "case", SIDES),
                   el([6.5, 12, 9], [14.5, 14.5, 10], "case")]
                  + [sign_print(6.75, 14.25, 12.25, 14.125, 9, "signs_b", 2)])

# The ice merchandiser outside the door: an insulated chest 1.25 m high with a door printed
# CITY ICE (an invented brand), a hasp over it and a handle; the chests of a run stand as one.
ICE_BODY = [el([0, 0, 3], [16, 1, 15], "kick", ("north", "south")),
            el([0, 1, 2.5], [16, 19, 15.5], "shell", ("north", "south")),
            el([0, 19, 2], [16, 20, 16], "shell", ("north", "south", "up", "down")),
            ALL_UV(el([1, 1.75, 2.25], [15, 18.25, 2.5], "door", ALL,
                      {"east": "shell", "west": "shell", "up": "shell", "down": "shell"}),
                   ["north"]),
            el([12.5, 7.5, 1.5], [13.25, 12.5, 2.25], "chrome", NO_BACK),
            el([7.5, 17.25, 1.75], [8.5, 18, 2.25], "chrome", NO_BACK)]
ICE_END = [el([0, 0, 2.5], [0.5, 19, 15.5], "shell", ("west", "north", "south")),
           el([0, 19, 2], [0.01, 20, 16], "shell", ("west",))]


# The propane exchange cage: a grey steel cage of open mesh, two tiers of cylinders, each an
# octagon with its collar and valve, a PROPANE EXCHANGE sign on the roof. The mullions are
# drawn half in each block, so the cages of a run stand as one with a door each.
def cylinder(cx, cz, y0, full=True):
    out = R.octagon(cx, cz, 2.2, y0, y0 + 6, "tank", caps=("up",))
    if full:
        out += (R.octagon(cx, cz, 1.5, y0 + 6, y0 + 7.5, "tank", caps=())
                + [el([cx - 0.4, y0 + 6, cz - 0.4], [cx + 0.4, y0 + 7.25, cz + 0.4], "brass",
                      NO_DOWN)])
    return out


PROPANE_BODY = ([el([0, 0, 2.5], [16, 1, 14.5], "frame", ("north", "up", "south")),
                 el([0, 11, 2.5], [16, 11.5, 14.5], "frame", ("north", "up", "down", "south")),
                 el([0, 21.5, 2.5], [16, 22, 14.5], "frame", ("north", "up", "down", "south"))]
                + [el([0, y0, z], [16, y1, z + 0.1], "mesh", ("north", "south"))
                   for y0, y1 in ((1, 11), (11.5, 21.5)) for z in (2.5, 14.4)]
                + [el([0, 1, 2.25], [0.5, 21.5, 2.85], "frame", ("north", "east", "south")),
                   el([15.5, 1, 2.25], [16, 21.5, 2.85], "frame", ("north", "west", "south")),
                   el([7.25, 9, 1.9], [8.75, 10, 2.25], "chrome", NO_BACK)]
                + [e for y0 in (1, 11.5) for cx in (3, 8, 13)
                   for e in cylinder(cx, 5.25, y0) + cylinder(cx, 11, y0, full=False)]
                + sign_panel(0.5, 15.5, 22.25, 26, 2.75, "signs_b", 3, both=False))
PROPANE_END = [el([0, 0, 2.25], [0.75, 22, 3], "frame", NO_DOWN),
               el([0, 0, 14], [0.75, 22, 14.75], "frame", NO_DOWN),
               el([0.25, 1, 3], [0.35, 11, 14], "mesh", ("east", "west")),
               el([0.25, 11.5, 3], [0.35, 21.5, 14], "mesh", ("east", "west")),
               el([0, 0, 3], [0.01, 1, 14], "frame", ("west",)),
               el([0, 11, 3], [0.01, 11.5, 14], "frame", ("west",)),
               el([0, 21.5, 3], [0.01, 22, 14], "frame", ("west",))]


# The firewood rack: a black steel rack of two tiers of shrink-wrapped bundles, four to a
# tier, a FIREWOOD BUNDLES sign on top.
def bundle(x0, y0, z0):
    uv = {"north": [0, 3, 16, 13], "south": [0, 3, 16, 13]}
    return el([x0, y0, z0], [x0 + 7, y0 + 4.5, z0 + 5.5], "bark", NO_DOWN,
              {"north": "ends", "south": "ends"}, uv=uv)


FIREWOOD_BODY = ([el([0, y, 2], [16, y + 0.5, 14.5], "frame", ("north", "up", "down", "south"))
                  for y in (0.5, 9.5, 17.5)]
                 + [el([0, y0, 14.4], [16, y1, 14.5], "mesh", ("north", "south"))
                    for y0, y1 in ((1, 9.5), (10, 17.5))]
                 + [bundle(x, y, z) for y in (1, 10) for x in (0.75, 8.25) for z in (2.5, 8.5)]
                 + sign_panel(0.5, 15.5, 18.25, 22, 2.5, "signs_c", 0, both=False))
FIREWOOD_END = ([el([0, 0, z], [0.75, 18, z + 0.75], "frame", SIDES + ("up",))
                 for z in (2, 13.75)]
                + [el([0, y, 2.75], [0.75, y + 0.5, 13.75], "frame", ("west", "up", "down"))
                   for y in (0.5, 9.5, 17.5)])

# The kiosks, two blocks tall and lit (their upper half gives light 7): a coin counter with its
# tray on a ledge, a photo kiosk with a tilted touchscreen over its printer, and a movie rental
# kiosk of posters and a screen under a lit header. No brands: each wears a generic header.
COIN_KIOSK = ([el([2, 0, 4], [14, 1, 14], "kick", NO_DOWN),
               el([2.5, 1, 4.5], [13.5, 13, 14], "shell", NO_DOWN),
               el([2.5, 9.25, 4.4], [13.5, 10.25, 4.5], "accent", ("north", "east", "west")),
               el([5.5, 3, 4.25], [10.5, 7.5, 4.5], "trim", NO_BACK),
               el([6.5, 6.5, 4], [9.5, 7, 4.25], "rubber", NO_BACK),
               el([2.25, 13, 3.25], [13.75, 14, 10], "shell"),
               ALL_UV(el([3, 14, 3.75], [13, 14.05, 9.5], "coins", ("up",)), ["up"]),
               el([2.5, 14, 9.5], [13.5, 24, 14], "shell", NO_DOWN),
               ALL_UV(el([4, 16, 9.4], [12, 22, 9.5], "screen", ("north",)), ["north"]),
               el([2, 24, 9], [14, 24.5, 14.5], "shell")]
              + sign_panel(2.5, 13.5, 24.75, 27.5, 9.75, "signs_c", 1, both=False))
PHOTO_KIOSK = ([el([2, 0, 6], [14, 1, 14], "kick", NO_DOWN),
                el([2.5, 1, 6.5], [13.5, 12.5, 14], "shell", NO_DOWN),
                el([4, 2.5, 6.25], [12, 5, 6.5], "trim", NO_BACK),
                el([4.5, 7, 5], [11.5, 7.5, 6.5], "case", NO_BACK),
                el([1.5, 12.5, 3], [14.5, 13.25, 14], "accent"),
                el([3, 13.25, 3.75], [7, 13.75, 6], "case", NO_DOWN),
                el([3.5, 13.75, 4.5], [6.5, 13.8, 5.5], "rubber", ("up",)),
                el([2.5, 13.25, 10], [13.5, 24, 14], "shell", NO_DOWN),
                ALL_UV(el([3.5, 14, 7], [12.5, 20, 8], "shell", ALL, {"north": "screen"},
                          rot=("x", 22.5, [8, 14, 7.5])), ["north"]),
                el([2, 24, 9.5], [14, 24.5, 14.5], "shell")]
               + sign_panel(2.5, 13.5, 24.75, 27.5, 10.25, "signs_c", 2, both=False))
DVD_KIOSK = [el([1.5, 0, 5.5], [14.5, 1, 14.5], "kick", NO_DOWN),
             el([1, 1, 5], [15, 28.5, 15], "shell", NO_DOWN),
             ALL_UV(el([1.75, 15, 4.9], [14.25, 27.25, 5], "posters", ("north",)), ["north"]),
             ALL_UV(el([3, 8.5, 4.9], [9.5, 13.5, 5], "screen", ("north",)), ["north"]),
             el([3.5, 6.5, 4.6], [9, 7.25, 5], "rubber", NO_BACK),
             el([10.5, 9, 4.25], [13, 12.5, 5], "case", NO_BACK),
             el([10.5, 6.5, 4.5], [13, 8.5, 5], "trim", NO_BACK),
             el([3.5, 3, 4.6], [12.5, 3.75, 5], "rubber", NO_BACK),
             el([0.5, 28.5, 3], [15.5, 32, 15.5], "shell"),
             sign_print(1.5, 14.5, 28.75, 32, 3, "signs_c", 3)]


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
# The deli, butcher and seafood cases share a section, and join one another as one counter.
SERVICE_JAVA = 'new BlockDisplayCase("%%s", new int[]{%s}, "service_case")'
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
     "java": SERVICE_JAVA % jbox(box_of(DELI))},
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

# --- the fresh departments ---------------------------------------------------------------
WALNUT = {"wood": T("walnut"), "wood_v": T("walnut_v"), "edge": T("walnut_edge")}
WALNUT_N = ("Walnut", "Nussbaum", "nogal", "valnöt")
OAK_N = ("Oak", "Eiche", "roble", "ek")
STEEL_N = ("Steel", "Stahl", "acero", "stål")
GLOW_HOT = {"glow": (MT("heat_lamp_off"), MT("heat_lamp_on")),
            "header": (MT("header_hot_off"), MT("header_hot_on"))}
GLOW_FLOWERS = {"glow": (GLOW_OFF, GLOW_ON),
                "header": (MT("header_flowers_off"), MT("header_flowers_on"))}
APPLIANCE = 'new BlockCounterAppliance("%%s", new int[]{%%s}, StoreAppliances.%s)'
DRINK_RUN = (RUN_FULL % ("%s", "%s", "drink_station", 9, "FurnishingsSounds.CABINET_OPEN",
                         "FurnishingsSounds.CABINET_CLOSE", "null", "1.0", "SOLID"))
DRINK_FINS = [
    fin("grey", {"wood": OT("laminate_grey"), "wood_v": OT("laminate_grey"),
                 "edge": OT("laminate_grey_edge"), "kick": MT("steel_grey"),
                 "counter": T("counter_quartz"), "sign": MT("coffee_sign")},
        "Grey", "Grau", "gris", "grå"),
    fin("walnut", dict(WALNUT, kick=MT("steel_grey"), counter=T("counter_granite"),
                       sign=MT("coffee_sign")), *WALNUT_N),
]

add("butcher_case", "run", [fin("white", CASE_WHITE, *WHITE_N), fin("black", CASE_BLACK, *BLACK_N)],
    ("Butcher Case", "Fleischtheke", "Vitrina de carnicería", "Köttdisk"),
    {"body": BUTCHER, "end": BUTCHER_END, "glow": GLOW_ONLY, "particle": "shell",
     "java": SERVICE_JAVA % jbox(box_of(BUTCHER))},
    "Butcher & Seafood",
    "Curved glass over trays of steaks, chops, mince, chicken, sausages, ribs and a roast "
    "between parsley, a scale on top; joins the deli and seafood cases; 27 slots; lit")
add("seafood_case", "run", [fin("white", CASE_WHITE, *WHITE_N), fin("black", CASE_BLACK, *BLACK_N)],
    ("Seafood Case", "Fischtheke", "Vitrina de pescadería", "Fiskdisk"),
    {"body": SEAFOOD, "end": SEAFOOD_END, "glow": GLOW_ONLY, "particle": "shell",
     "java": SERVICE_JAVA % jbox(box_of(SEAFOOD))},
    "Butcher & Seafood",
    "Curved glass over a sloping ice bed of fish, fillets, shrimp, crab and mussels, a scale on "
    "top; joins the deli and butcher cases; 27 slots; lit")

add("bread_rack", "run",
    [fin("oak", dict(OAK, sign=MT("bread_sign")), *OAK_N),
     fin("walnut", dict(WALNUT, sign=MT("bread_sign")), *WALNUT_N)],
    ("Bread Rack", "Brotregal", "Estantería de pan", "Brödhylla"),
    {"body": BREAD_RACK, "end": BREAD_RACK_END, "particle": "wood",
     "tex": stock_tex("bread", "bakery"),
     "java": RUN % ("%s", jbox(box_of(BREAD_RACK)), "bread_rack", "BlockRenderLayer.CUTOUT")},
    "Bakery & Hot Food",
    "Stepped shelves of bagged and crusty loaves under a BREAD sign; joins any bread rack")
add("pastry_case", "run",
    [fin("white", {"wood": T("white"), "wood_v": T("white"), "edge": T("white_edge")}, *WHITE_N),
     fin("walnut", WALNUT, *WALNUT_N)],
    ("Self-Serve Pastry Case", "Selbstbedienungs-Gebäckvitrine",
     "Vitrina de bollería de autoservicio", "Självbetjäningsmonter för bakverk"),
    {"body": PASTRY_BODY, "end": PASTRY_END, "particle": "wood",
     "tex": stock_tex("pastry", "bakery_b"),
     "java": RUN % ("%s", jbox(box_of(PASTRY_BODY)), "pastry_case",
                    "BlockRenderLayer.TRANSLUCENT")},
    "Bakery & Hot Food",
    "Acrylic doors over muffins, cinnamon rolls, cookies, bagels and doughnuts; joins")
add("hot_food_case", "run",
    [fin("steel", {"shell": T("stainless"), "frame": T("stainless"), "liner": MT("liner"),
                   "trim": T("stainless_dark"), "kick": MT("steel_grey"),
                   "case": T("stainless_dark")}, *STEEL_N)],
    ("Hot Food Case", "Warmhaltevitrine", "Vitrina caliente", "Varmhållningsmonter"),
    {"body": HOT_FOOD, "end": HOT_FOOD_END, "glow": GLOW_HOT, "particle": "shell",
     "tex": stock_tex("hot_food", "hot_food"),
     "java": 'new BlockDisplayCase("%%s", new int[]{%s}, "hot_food_case")'
             % jbox(box_of(HOT_FOOD))},
    "Bakery & Hot Food",
    "Heated glass case of rotisserie chickens and hot trays under amber lamps and a lit HOT FOOD "
    "sign; joins; 27 slots; lit, switched like the deli case")
add("rotisserie_oven", "appliance",
    [fin("steel", {"shell": T("stainless"), "trim": T("stainless_dark"), "handle": T("chrome")},
         *STEEL_N)],
    ("Rotisserie Oven", "Hähnchengrill", "Asador de pollos", "Kycklinggrill"),
    {"geo": ROTISSERIE, "particle": "shell",
     "glow": (MT("rotisserie_window_off"), MT("rotisserie_window_on")),
     "java": APPLIANCE % "ROTISSERIE"},
    "Bakery & Hot Food",
    "Roasts raw meat and fish, eight at a time; its window glows while it turns; rests on "
    "counters")

add("flower_stand", "single",
    [fin("black", {"frame": T("metal_black")}, *BLACK_N), fin("oak", {"frame": T("oak")}, *OAK_N)],
    ("Flower Bucket Stand", "Blumeneimer-Ständer", "Expositor de cubos de flores",
     "Blomsterhinkställ"),
    {"geo": FLOWER_STAND, "particle": "frame",
     "java": FIX % ("%s", jbox(box_of(FLOWER_STAND)), "METAL")},
    "Floral", "Three tiers of buckets of roses, tulips, sunflowers, daisies, carnations, lilies, "
              "irises and wrapped bouquets")
add("floral_cooler", "tall_run", cooler_fins({"glass": MT("glass_clear")}),
    ("Floral Cooler", "Blumenkühlschrank", "Nevera de flores", "Blomkyl"),
    {"body": reach_in_frame() + floral_interior(), "end": REACH_IN_END, "glow": GLOW_FLOWERS,
     "particle": "shell",
     "java": 'new BlockDisplayCooler("%%s", new int[]{%s})' % jbox(COOLER_BOX)},
    "Floral", "2 blocks tall; a glass door over stepped buckets of flowers and bouquets under a "
              "lit FRESH FLOWERS sign; joins; 27 slots; lit, switched like the cooler")

add("coffee_bar", "run", DRINK_FINS,
    ("Self-Serve Coffee Bar", "Selbstbedienungs-Kaffeebar", "Barra de café de autoservicio",
     "Självbetjäningskaffebar"),
    {"body": COFFEE_BAR, "end": DRINK_END, "particle": "wood",
     "java": DRINK_RUN % ("%s", jbox(box_of(COFFEE_BAR)))},
    "Coffee & Drinks",
    "A counter with a condiment caddy under a COFFEE sign; joins the cup counter into one "
    "station; 9 slots; brewers and fountains rest on it")
add("cup_counter", "run", DRINK_FINS,
    ("Cup and Lid Counter", "Becher- und Deckeltheke", "Mostrador de vasos y tapas",
     "Mugg- och lockdisk"),
    {"body": CUP_COUNTER, "end": DRINK_END, "particle": "wood",
     "java": DRINK_RUN % ("%s", jbox(box_of(CUP_COUNTER)))},
    "Coffee & Drinks",
    "Cup dispensers in three sizes, a lid organiser, straws and a bin flap; joins the coffee bar; "
    "9 slots")
add("coffee_brewer", "appliance",
    [fin("steel", {"shell": T("stainless"), "trim": T("stainless_dark")}, *STEEL_N),
     fin("black", {"shell": T("appliance_black"), "trim": T("appliance_black_trim")}, *BLACK_N)],
    ("Commercial Coffee Brewer", "Gewerbliche Kaffeemaschine", "Cafetera industrial",
     "Storkaffebryggare"),
    {"geo": BREWER, "particle": "shell", "glow": A.GLOW["green"],
     "java": APPLIANCE % "COFFEE_BREWER"},
    "Coffee & Drinks",
    "Twin airpots; brews coffee from cocoa beans and water, 32 at a time; rests on counters")
add("fountain_machine", "appliance",
    [fin("red", {"shell": MT("plastic_red"), "trim": T("stainless_dark"),
                 "art": MT("fountain_art_red")}, "Red", "Rot", "rojo", "röd"),
     fin("blue", {"shell": MT("plastic_blue"), "trim": T("stainless_dark"),
                  "art": MT("fountain_art_blue")}, "Blue", "Blau", "azul", "blå")],
    ("Fountain Drink Machine", "Getränkespender", "Máquina de refrescos", "Läskautomat"),
    {"geo": FOUNTAIN, "particle": "shell", "glow": A.GLOW["blue"],
     "java": APPLIANCE % "FOUNTAIN"},
    "Coffee & Drinks",
    "Six invented drinks; pours a fountain drink from sugar and water; rests on counters")

# --- the front of the store ----------------------------------------------------------------
TEAL_N = ("Teal", "Petrol", "verde azulado", "blågrön")
PHARMACY_FINS = [
    fin("teal", {"wood": T("white"), "wood_v": MT("laminate_teal"), "edge": T("white_edge"),
                 "frame": T("metal_black")}, *TEAL_N),
    fin("walnut", dict(WALNUT, frame=T("metal_black")), *WALNUT_N),
]
PHARMACY_JAVA = ('new BlockKitchenCabinet("%s", new int[]{0, 0, 0, 16, 16, 15}, '
                 'KitchenLine.RECEPTION, 9, KitchenFront.DRAWERS)')
PHARMACY_DESK = ("The reception desk as a pharmacy counter: joins the drop-off and pick-up "
                 "counters of its finish into one; 9 slots in drawers; a sign on a post reads "
                 "PRESCRIPTION %s both ways")

add("pharmacy_dropoff", "pharmacy", PHARMACY_FINS,
    ("Pharmacy Drop-Off Counter", "Apothekentheke Rezeptannahme",
     "Mostrador de farmacia para entregar recetas", "Apoteksdisk för receptinlämning"),
    {"geo": pharmacy_post(2), "java": PHARMACY_JAVA}, "Pharmacy", PHARMACY_DESK % "DROP OFF")
add("pharmacy_pickup", "pharmacy", PHARMACY_FINS,
    ("Pharmacy Pick-Up Counter", "Apothekentheke Rezeptausgabe",
     "Mostrador de farmacia para recoger recetas", "Apoteksdisk för uthämtning"),
    {"geo": pharmacy_post(3), "java": PHARMACY_JAVA}, "Pharmacy", PHARMACY_DESK % "PICK UP")
add("pharmacy_shelf", "run", [fin("white", {"shell": MT("case_white")}, *WHITE_N)],
    ("Pharmacy Shelf Wall", "Apothekenregal", "Estantería de farmacia", "Apotekshylla"),
    {"body": PHARMACY_SHELF, "end": PHARMACY_SHELF_END, "particle": "shell",
     "tex": stock_tex("pharmacy", "pharmacy_b"),
     "java": RUN % ("%s", jbox(box_of(PHARMACY_SHELF)), "pharmacy_shelf",
                    "BlockRenderLayer.CUTOUT")},
    "Pharmacy", "Shelves of generic cartons and pill bottles; joins any pharmacy shelf, and "
                "stacks into a wall (end panels only at a run's ends)")
_HANG = FIX % ("%s", jbox(box_of(hanging_sign("signs_a", 0))), "METAL")
add("pharmacy_sign", "single", [fin("teal", {}, *TEAL_N)],
    ("Pharmacy Sign", "Apothekenschild", "Cartel de farmacia", "Apoteksskylt"),
    {"geo": hanging_sign("signs_a", 0), "particle": "case", "java": _HANG},
    "Pharmacy", "Hangs from the ceiling: PHARMACY with an Rx and a capsule, both sides")
add("consultation_sign", "single", [fin("teal", {}, *TEAL_N)],
    ("Consultation Sign", "Beratungsschild", "Cartel de consulta", "Rådgivningsskylt"),
    {"geo": hanging_sign("signs_a", 1), "particle": "case", "java": _HANG},
    "Pharmacy", "Hangs from the ceiling: PRIVATE CONSULTATION, both sides")

add("tobacco_case", "run",
    [fin("black", {"shell": MT("case_black"), "trim": T("stainless_dark"),
                   "shelf": MT("steel_grey")}, *BLACK_N),
     fin("walnut", {"shell": T("walnut"), "trim": T("stainless_dark"),
                    "shelf": MT("steel_grey")}, *WALNUT_N)],
    ("Tobacco Case", "Tabakwarenschrank", "Vitrina de tabaco", "Tobaksskåp"),
    {"body": TOBACCO_BODY, "end": TOBACCO_LEFT, "end_right": TOBACCO_RIGHT, "particle": "shell",
     "tex": dict(stock_tex("packs", "packs"), glass=MT("glass_smoke")),
     "java": RUN_FULL % ("%s", jbox(box_of(TOBACCO_BODY + TOBACCO_RIGHT)), "tobacco_case", 9,
                         "FurnishingsSounds.CABINET_OPEN", "FurnishingsSounds.CABINET_CLOSE",
                         "null", "1.0", "TRANSLUCENT")},
    "Tobacco & Lottery",
    "Hangs on the wall behind the register: plain packs behind sliding smoked glass, WE CHECK "
    "ID over the run's end on the shopper's left; joins into a run; 9 slots")
add("lottery_dispenser", "counter",
    [fin("clear", {}, "Clear", "Transparent", "transparente", "genomskinlig")],
    ("Lottery Ticket Dispenser", "Rubbellos-Spender", "Dispensador de boletos de lotería",
     "Skraplottsställ"),
    {"geo": LOTTO_DISPENSER, "particle": "case",
     "java": 'new BlockCounterPiece("%s", new int[]{%s}, Material.WOOD, SoundType.METAL, '
             'BlockRenderLayer.TRANSLUCENT)'},
    "Tobacco & Lottery",
    "Four invented scratch games behind a clear front under a LUCKY CITY LOTTERY header; "
    "rests on counters")
add("lottery_terminal", "counter", [fin("black", {"screen": MT("screen_lotto")}, *BLACK_N)],
    ("Lottery Terminal", "Lotto-Terminal", "Terminal de lotería", "Lottoterminal"),
    {"geo": LOTTO_TERMINAL, "particle": "case",
     "java": ('new BlockCounterPiece("%%s", new int[]{%%s}, %s, FurnishingsSounds.PRINTER_RUN, '
              '1.8F, null)' % PIECE_METAL)},
    "Tobacco & Lottery",
    "Play-slip reader, the clerk's screen and a jackpot display; prints a ticket on click; rests "
    "on counters")

add("ice_merchandiser", "run",
    [fin("white", {"shell": MT("case_white"), "kick": MT("steel_grey"),
                   "door": MT("ice_door_white")}, *WHITE_N),
     fin("blue", {"shell": MT("plastic_blue"), "kick": MT("steel_grey"),
                  "door": MT("ice_door_blue")}, "Blue", "Blau", "azul", "blå")],
    ("Ice Merchandiser", "Eistruhe für Eiswürfel", "Arcón de hielo", "Isfrys"),
    {"body": ICE_BODY, "end": ICE_END, "particle": "shell",
     "java": RUN_FULL % ("%s", jbox(box_of(ICE_BODY)), "ice_merchandiser", 27,
                         "FurnishingsSounds.FRIDGE_OPEN", "FurnishingsSounds.FRIDGE_CLOSE",
                         "null", "1.0", "SOLID")},
    "Outdoor", "An insulated chest of bagged CITY ICE (an invented brand); joins into a run; "
               "27 slots, fridge door sounds")
add("propane_cage", "run",
    [fin("grey", {"frame": MT("steel_grey"), "mesh": MT("mesh_cage"),
                  "tank": MT("steel_white")}, "Grey", "Grau", "gris", "grå")],
    ("Propane Exchange Cage", "Propangas-Tauschkäfig", "Jaula de intercambio de propano",
     "Gasolbytesbur"),
    {"body": PROPANE_BODY, "end": PROPANE_END, "particle": "frame",
     "java": RUN % ("%s", jbox(box_of(PROPANE_BODY)), "propane_cage",
                    "BlockRenderLayer.CUTOUT")},
    "Outdoor", "Two tiers of cylinders behind open mesh under a PROPANE EXCHANGE sign; joins "
               "into a run")
add("firewood_rack", "run", [fin("black", {"frame": T("metal_black"), "mesh": MT("mesh_black")},
                                 *BLACK_N)],
    ("Firewood Rack", "Brennholzregal", "Estante de leña", "Vedställ"),
    {"body": FIREWOOD_BODY, "end": FIREWOOD_END, "particle": "frame",
     "java": RUN % ("%s", jbox(box_of(FIREWOOD_BODY)), "firewood_rack",
                    "BlockRenderLayer.CUTOUT")},
    "Outdoor", "Two tiers of wrapped firewood bundles under a FIREWOOD sign; joins into a run")

KIOSK_JAVA = 'new BlockMarketTall("%%s", new int[]{%s}, 7, FurnishingsSounds.%s)'
add("coin_kiosk", "tall",
    [fin("blue", {"shell": MT("plastic_blue"), "kick": MT("steel_grey"),
                  "accent": MT("plastic_yellow"), "trim": T("stainless_dark"),
                  "screen": MT("screen_coins")}, "Blue", "Blau", "azul", "blå")],
    ("Coin Counting Kiosk", "Münzzählautomat", "Quiosco contador de monedas",
     "Myntinräkningsautomat"),
    {"geo": COIN_KIOSK, "particle": "shell",
     "java": KIOSK_JAVA % (jbox(box_of(COIN_KIOSK, tall=True)), "APPLIANCE_BEEP")},
    "Kiosks", "2 blocks tall, decorative: a coin tray and a lit screen (light 7); beeps on click")
add("photo_kiosk", "tall",
    [fin("white", {"shell": MT("case_white"), "kick": MT("steel_grey"),
                   "accent": MT("plastic_red"), "trim": T("stainless_dark"),
                   "screen": MT("screen_photo")}, *WHITE_N)],
    ("Photo Printing Kiosk", "Fotodruckautomat", "Quiosco de impresión de fotos", "Fotoautomat"),
    {"geo": PHOTO_KIOSK, "particle": "shell",
     "java": KIOSK_JAVA % (jbox(box_of(PHOTO_KIOSK, tall=True)), "PRINTER_RUN")},
    "Kiosks", "2 blocks tall, decorative: a tilted touchscreen over a print tray (light 7); "
              "prints on click")
add("dvd_kiosk", "tall",
    [fin("purple", {"shell": MT("plastic_purple"), "kick": MT("steel_grey"),
                    "trim": T("stainless_dark"), "screen": MT("screen_movies")},
         "Purple", "Lila", "morado", "lila")],
    ("DVD Rental Kiosk", "DVD-Verleihautomat", "Quiosco de alquiler de DVD",
     "DVD-uthyrningsautomat"),
    {"geo": DVD_KIOSK, "particle": "shell",
     "java": KIOSK_JAVA % (jbox(box_of(DVD_KIOSK, tall=True)), "APPLIANCE_BEEP")},
    "Kiosks", "2 blocks tall, decorative: invented films' posters, a screen and a disc slot "
              "under a lit header (light 7); beeps on click")

PIECE = {p[0]: p for p in PIECES}


def names_with(names, fnames):
    return ["%s (%s)" % (n, f) for n, f in zip(names, fnames)]


def java_for(piece, reg):
    _p, kind, _f, _n, spec, _g, _b = PIECE[piece]
    if kind in ("counter", "appliance"):
        return spec["java"] % (reg, jbox(box_of(spec["geo"])))
    return spec["java"] % reg


def entries():
    """Every block: (registry, piece, kind, finish textures, names, java)."""
    out = []
    for piece, kind, finishes, names, spec, _g, _b in PIECES:
        for fid, ftex, *fnames in finishes:
            reg = "%s_%s" % (piece, fid)
            tex = dict(spec.get("tex", {}))
            if kind == "appliance":
                tex["glow"] = spec["glow"][0]
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
        elif kind == "pharmacy":
            out.append(("%s_sign" % piece, geometry(spec["geo"], "case")))
            item = RECEPTION_ITEM + spec["geo"]
            out.append(("%s_item" % piece, geometry(item, "wood", display=big(item))))
        elif kind in ("single", "sign"):
            geo = spec["geo"]
            lo, hi = B.extent(geo)
            out.append((piece, geometry(geo, p, display=big(geo) if hi[1] > 18 else None)))
        elif kind == "tall":
            lower, upper = split_tall(spec["geo"])
            out.append(("%s_lower" % piece, geometry(lower, p)))
            out.append(("%s_upper" % piece, geometry(upper, p)))
            out.append(("%s_item" % piece, geometry(spec["geo"], p, display=big(spec["geo"]))))
        elif kind in ("counter", "appliance"):
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
    "item.fountain_drink.name": ("Fountain Drink", "Softdrink vom Zapfhahn", "Refresco de grifo",
                                 "Läsk från fontän"),
}

# The fountain drink the fountain drink machine pours: a paper cup, its lid and a straw, drawn
# as the kitchen's drinks are (gen_furniture_appliances.py's sprite rows).
ITEM_SUB = "market"
SODA_ROWS = [
    ".........ss.....",
    "........ss......",
    ".......ss.......",
    "....OOOsOOOO....",
    "...OwwwwwwwwO...",
    "...OOOOOOOOOO...",
    "....OrrrrrrO....",
    "....OlrrrrrO....",
    "....OwwwwwwO....",
    "....OrwwwwrO....",
    ".....OrrrrO.....",
    ".....OlrrrO.....",
    ".....OrrrrO.....",
    ".....OrrrrO.....",
    ".....OOOOOO.....",
    "................",
]
SODA_PAL = {"O": (90, 24, 28), "w": (246, 246, 244), "r": (206, 40, 44), "l": (236, 96, 96),
            "s": (240, 200, 60)}
ITEM_TEXTURES = {"fountain_drink": lambda: A.sprite(SODA_ROWS, SODA_PAL)}


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
        if kind == "appliance":
            off, on = glow
            glow = {}
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
        elif kind == "pharmacy":
            # the reception desk's parts, and this counter's sign on every block
            for part in ("body", "left", "right"):
                copy_model(blk % part, "reception_desk_%s" % part, ftex, O.SUB)
            copy_model(blk % "sign", "%s_sign" % piece, ftex)
            copy_model(item, "%s_item" % piece, ftex)
            state = multipart_state(reg, run_rules() + R.faced([("sign", {})]))
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
        elif kind in ("counter", "appliance"):
            state = {"forge_marker": 1,
                     "defaults": {"model": BASE + "%s_floor" % piece, "textures": ftex},
                     "variants": {"facing": facing_variants(),
                                  "rest": {r: {"model": BASE + "%s_%s" % (piece, r)}
                                           for r, _d in A.RESTS},
                                  "inventory": [{"model": BASE + "%s_item" % piece}]}}
            if kind == "appliance":
                # RUNNING lights its window or lamp: the one texture swapped, as the kitchen's
                state["variants"]["running"] = {"true": {"textures": {"glow": on}},
                                                "false": {"textures": {"glow": off}}}
        else:
            raise ValueError(kind)
        dump("blockstates/%s.json" % reg, state)
    dump("blockstates/vf915.json", verifone_state())
    for name, draw in ITEM_TEXTURES.items():
        rel = "textures/items/%s/%s.png" % (ITEM_SUB, name)
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
        dump("models/item/%s.json" % name,
             {"parent": "item/generated", "textures": {"layer0": "csm:items/%s/%s" % (ITEM_SUB,
                                                                                    name)}})
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


class StoreDrink:
    """The fountain drink's registry name (StoreAppliances.FOUNTAIN_DRINK in Java)."""
    REGISTRY = "fountain_drink"
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
GROUP_ORDER = ["Refrigerated", "Shelving", "Produce", "Butcher & Seafood", "Bakery & Hot Food",
               "Floral", "Coffee & Drinks", "Checkout", "Tobacco & Lottery", "Pharmacy",
               "Store", "Kiosks", "Outdoor"]


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
        if group == "Coffee & Drinks":
            lines.append("")
            lines.append("    // Fountain Drink (poured by the fountain drink machine)")
            lines.append('    initTabItem(new ItemResidentialFood("%s", 2, 0.3F, true, null));'
                         % StoreDrink.REGISTRY)
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
