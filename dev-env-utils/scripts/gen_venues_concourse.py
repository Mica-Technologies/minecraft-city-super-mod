#!/usr/bin/env python3
"""
gen_venues_concourse.py -- the Sports & Venues arena's concourse and premium spaces.

    py -3 dev-env-utils/scripts/gen_venues_concourse.py               # write
    py -3 dev-env-utils/scripts/gen_venues_concourse.py --check       # fail on drift
    py -3 dev-env-utils/scripts/gen_venues_concourse.py --fragments   # the tab's lines

The blocks (the Sports & Venues - Concourse tab, CsmTabVenuesConcourse). What a concourse
shares with a supermarket, a kitchen or a station (coolers, the fountain drink machine, the hot
food case, registers and card terminals, bar stools, TVs, the walk-through metal detector,
queue stanchions, the X-ray) is already in Furnishings, Life Safety and Transit and is not
repeated here; VENUES_SYSTEM.md lists it for the arena builder. What is here is what a stadium
has and they do not, or what reads wrong borrowed:

  * **Concession counters** (BlockConcessionCounter): a block-high front counter whose top
    overhangs a front panel in four finishes (stainless, navy, crimson, black) with an LED strip
    under the lip; straights join into a run and close themselves with end panels (actual
    state), and an outside corner (BlockConcessionCounterCorner) turns the run. The back
    counter (BlockConcourseRun) is stainless cabinets and worktop with a backsplash.
  * **The concession sign** is Core's wayfinding panel (header legends as presets); the
    **digital menu board** (BlockMenuBoard) is a 3 x 2 screen whose four menus are 256 px
    pictures drawn here, baked.
  * **Machines** (BlockConcessionAppliance): hot dog roller grill, popcorn machine, pretzel
    warmer, nacho station and soft-serve machine, each a model on and off; on, their moving
    parts are animated textures. Furnishings' appliance framework is not Core's, so they do
    not cook.
  * **The portable beer kiosk** (BlockBeerKiosk): a 2 x 1 x 3 unit, one OBJ.
  * **The stadium turnstile** (BlockStadiumTurnstile): a tripod turnstile whose three arms are
    mutually square on a hub tilted 54.7 degrees (the cube-diagonal geometry real tripods use),
    so one arm is always level across the lane; an OBJ at rest and indexed 60 degrees round,
    with its indicator green (free or open) or red (locked). The ticket scanner pedestal
    (BlockTicketScanner) and the Venue Ticket item go with it.
  * **Suite fixtures** (BlockConcourseRun): the suite bar counter and the drink rail.
  * **Team store** (BlockTeamStoreFixture, BlockJerseyRack): jersey wall rack, cap wall, apparel
    table, jersey mannequin; one grey set of models, the garments tinted 0 (team colour) and 1
    (trim) from a tile entity.

Every model faces north (the customer, reader or shopper) with its back at +Z. No brand: the
beer kiosk says COLD BEER and DRAFTS, the menus list food and prices.
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_venues_common as vc  # noqa: E402

GEN = "gen_venues_concourse.py"
C = vc.catalogue(GEN, "concourse")
T = C.T
M = C.M
box = vc.box
lsc = vc.lsc

CONC = "Sports & Venues - Concourse"
FACINGS = vc.FACINGS

# ------------------------------------------------------------------------------------------
# Palette
# ------------------------------------------------------------------------------------------
STEEL = (198, 202, 206)
STEEL_DARK = (118, 122, 128)
BLACK = (26, 26, 28)
KICK = (42, 42, 46)
TOP = (214, 210, 202)
LED = (255, 240, 212)
RED = (196, 30, 36)
YELLOW = (250, 206, 40)
NAVY = (28, 42, 88)
CRIMSON = (150, 30, 44)
GOLD = (232, 176, 40)
WALNUT = (92, 60, 38)
STONE = (46, 46, 50)
BRASS = (196, 160, 72)
OAK = (196, 158, 108)
CHARCOAL = (54, 56, 60)
WHITE = (240, 240, 236)
FINISHES = [("stainless", None), ("navy", NAVY), ("crimson", CRIMSON), ("black", (34, 34, 38))]


def rgba(c, a=255):
    return tuple(c[:3]) + (a,)


def tint(el, idx):
    """Marks every face of an element with a tint index (0 team colour, 1 trim)."""
    for f in el["faces"].values():
        f["tintindex"] = idx
    return el


def only(el, *faces):
    """Keeps just the named faces of an element."""
    el["faces"] = {k: v for k, v in el["faces"].items() if k in faces}
    return el


def uv(el, face, tex, rect):
    """Gives one face its own texture and uv window."""
    el["faces"][face] = {"texture": "#" + tex, "uv": [round(v, 3) for v in rect]}
    return el


def unit_cells(boxes_px):
    """The arena generator's rule: every cell a box reaches 6 px into, but the master."""
    def axis(lo, hi, tol=6.0):
        out = []
        for c in range(int(math.floor(lo / 16.0)), int(math.ceil(hi / 16.0))):
            ov = min(hi, 16.0 * (c + 1)) - max(lo, 16.0 * c)
            if ov >= min(tol, hi - lo - 1e-6):
                out.append(c)
        return out
    cells = set()
    for b in boxes_px:
        for cx in axis(b[0], b[3]):
            for cy in axis(b[1], b[4]):
                for cz in axis(b[2], b[5]):
                    cells.add((cx, cy, cz))
    cells.discard((0, 0, 0))
    return sorted(cells, key=lambda c: (c[1], c[2], c[0]))


def unit_java(cls, reg, boxes_px):
    """The tab line of a unit: its footprint written from its boxes."""
    flat_c = ", ".join("%d, %d, %d" % c for c in unit_cells(boxes_px))
    flat_b = ", ".join(", ".join("%d" % int(round(v)) for v in b) for b in boxes_px)
    return 'new %s("%s", new int[]{%s}, new int[]{%s})' % (cls, reg, flat_c, flat_b)


def ys(y):
    return {"y": y} if y else {}


# ------------------------------------------------------------------------------------------
# Shared textures
# ------------------------------------------------------------------------------------------
C.textures["steel"] = lambda: vc.brushed(STEEL, 401)
C.textures["steel_dark"] = lambda: vc.brushed(STEEL_DARK, 402)
C.textures["black"] = lambda: vc.swatch(BLACK, 403, grain=2)
C.textures["kick"] = lambda: vc.swatch(KICK, 404, grain=2)
C.textures["chrome"] = lambda: vc.brushed((222, 226, 230), 405)
C.textures["brass"] = lambda: vc.brushed(BRASS, 406)


@C.texture("top")
def _top():
    rng = random.Random(407)
    img = vc.swatch(TOP, 407, grain=3)
    px = img.load()
    for _ in range(26):
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = rgba(vc.shade(TOP, rng.choice((0.7, 0.8, 1.08))))
    return img


@C.texture("led")
def _led():
    img = Image.new("RGBA", (16, 16), rgba(LED))
    for x in range(0, 16, 2):
        vc.rect(img, x, 0, x + 1, 16, (255, 250, 236))
    return img


@C.texture("glass")
def _glass():
    img = Image.new("RGBA", (16, 16), (205, 228, 236, 48))
    for i in range(5, 12):
        img.putpixel((i, 16 - i), (240, 250, 255, 84))
    return img


def panel_finish(colour, seed):
    """A counter's front panel: a painted (or brushed) sheet with a reveal near its foot and a
    seam at the block's edge, so a run reads as panels."""
    def draw():
        if colour is None:
            img = Image.new("RGBA", (16, 16))
            rng = random.Random(seed)
            px = img.load()
            cols = [rng.uniform(-0.06, 0.06) for _ in range(16)]
            for y in range(16):
                for x in range(16):
                    px[x, y] = rgba(vc.shade(STEEL, 0.97 + cols[x] + rng.uniform(-0.01, 0.01)))
            base = STEEL
        else:
            img = vc.swatch(colour, seed, grain=3)
            base = colour
        vc.rect(img, 0, 12, 16, 13, vc.shade(base, 0.62))
        vc.rect(img, 15, 0, 16, 16, vc.shade(base, 0.7))
        vc.rect(img, 0, 0, 1, 16, vc.shade(base, 1.12))
        return img
    return draw


for _i, (_fid, _col) in enumerate(FINISHES):
    C.textures["front_" + _fid] = panel_finish(_col, 410 + _i)


@C.texture("doors")
def _doors():
    img = vc.brushed(STEEL, 415)
    vc.rect(img, 7, 0, 9, 16, vc.shade(STEEL, 0.55))
    vc.rect(img, 0, 0, 16, 1, vc.shade(STEEL, 0.7))
    for x in (5, 10):
        vc.rect(img, x, 3, x + 1, 9, (70, 72, 76))
    return img


@C.texture("walnut")
def _walnut():
    rng = random.Random(416)
    img = vc.swatch(WALNUT, 416, grain=4)
    px = img.load()
    for x in range(16):
        k = 0.9 + 0.2 * math.sin(x * 1.7 + 1.3)
        for y in range(16):
            r, g, b, _ = px[x, y]
            px[x, y] = rgba(vc.shade((r, g, b), k + rng.uniform(-0.03, 0.03)))
    for x in (0, 4, 8, 12):
        vc.rect(img, x, 0, x + 1, 16, vc.shade(WALNUT, 0.62))
    return img


@C.texture("stone")
def _stone():
    rng = random.Random(417)
    img = vc.swatch(STONE, 417, grain=4)
    px = img.load()
    for _ in range(40):
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = rgba(vc.shade(STONE, rng.choice((0.6, 1.5, 1.9, 2.4))))
    return img


@C.texture("oak")
def _oak():
    rng = random.Random(418)
    img = vc.swatch(OAK, 418, grain=3)
    px = img.load()
    for y in range(16):
        k = 0.93 + 0.1 * math.sin(y * 1.3 + 0.5)
        for x in range(16):
            r, g, b, _ = px[x, y]
            px[x, y] = rgba(vc.shade((r, g, b), k + rng.uniform(-0.02, 0.02)))
    return img


@C.texture("rail_panel")
def _rail_panel():
    img = vc.swatch(CHARCOAL, 419, grain=2)
    for x in range(0, 16, 4):
        vc.rect(img, x, 0, x + 1, 16, vc.shade(CHARCOAL, 0.85))
    return img


# ------------------------------------------------------------------------------------------
# Concession counters
# ------------------------------------------------------------------------------------------
def counter_parts():
    return [box([0, 15, 0], [16, 16, 16], "top"),
            only(box([0, 1, 1], [16, 14.5, 2.5], "front"), "north", "south", "down"),
            only(box([0, 14.5, 1.25], [16, 15, 2], "led"), "north", "down"),
            box([0, 0, 1.5], [16, 1, 2.5], "kick", faces=("north", "south", "up")),
            box([0, 6, 2.5], [16, 7, 15], "shelf", faces=("up", "down", "south")),
            box([0, 13.5, 14.5], [16, 15, 16], "steel", faces=("north", "south", "down"))]


def counter_end():
    return [box([0, 0, 1], [1, 15, 16], "front")]


def corner_parts():
    els = [box([0, 15, 0], [16, 16, 16], "top"),
           only(box([0, 1, 1], [16, 14.5, 2.5], "front"), "north", "south", "down"),
           only(box([1, 1, 2.5], [2.5, 14.5, 16], "front"), "west", "east", "down"),
           only(box([0, 14.5, 1.25], [16, 15, 2], "led"), "north", "down"),
           only(box([1.25, 14.5, 2], [2, 15, 16], "led"), "west", "down"),
           box([0, 0, 1.5], [16, 1, 2.5], "kick", faces=("north", "up")),
           box([1.5, 0, 2.5], [2.5, 1, 16], "kick", faces=("west", "up")),
           box([2.5, 6, 2.5], [16, 7, 15], "shelf", faces=("up", "down", "south")),
           box([2.5, 13.5, 14.5], [16, 15, 16], "steel", faces=("north", "south", "down"))]
    return els


def ctex(finish):
    return {"top": T("top"), "front": T("front_" + finish), "led": T("led"),
            "kick": T("kick"), "shelf": T("steel_dark"), "steel": T("steel"),
            "particle": T("front_" + finish)}


def counter_models():
    out = {}
    for fid, _c in FINISHES:
        tex = ctex(fid)
        out["counter_" + fid] = vc.model(tex, counter_parts())
        out["counter_end_l_" + fid] = vc.model(tex, counter_end())
        out["counter_end_r_" + fid] = vc.model(tex, [vc.mirror_x(e) for e in counter_end()])
        out["counter_corner_" + fid] = vc.model(tex, corner_parts())
    out["counter_item"] = vc.model(ctex("navy"), counter_parts() + counter_end()
                                   + [vc.mirror_x(e) for e in counter_end()])
    out["counter_corner_item"] = vc.model(ctex("navy"), corner_parts())
    return out


def counter_state():
    rules = []
    for facing, y in FACINGS:
        for i, (fid, _c) in enumerate(FINISHES):
            w = {"facing": facing, "finish": str(i)}
            rules.append({"when": dict(w), "apply": dict(model=M("counter_" + fid), **ys(y))})
            rules.append({"when": dict(w, end_l="true"),
                          "apply": dict(model=M("counter_end_l_" + fid), **ys(y))})
            rules.append({"when": dict(w, end_r="true"),
                          "apply": dict(model=M("counter_end_r_" + fid), **ys(y))})
    return {"multipart": rules}


def corner_state():
    rules = []
    for facing, y in FACINGS:
        for i, (fid, _c) in enumerate(FINISHES):
            rules.append({"when": {"facing": facing, "finish": str(i)},
                          "apply": dict(model=M("counter_corner_" + fid), **ys(y))})
    return {"multipart": rules}


_cm = counter_models()
C.add("concession_counter", 'new BlockConcessionCounter("concession_counter")',
      ("Concession Counter", "Verkaufstresen", "Mostrador de Concesión", "Försäljningsdisk"),
      {k: v for k, v in _cm.items() if not k.startswith("counter_corner")},
      counter_state(), item={"parent": "csm:block/venues/concourse/counter_item"}, tab=CONC)
C.add("concession_counter_corner",
      'new BlockConcessionCounterCorner("concession_counter_corner")',
      ("Concession Counter (Corner)", "Verkaufstresen (Ecke)",
       "Mostrador de Concesión (Esquina)", "Försäljningsdisk (Hörn)"),
      {k: v for k, v in _cm.items() if k.startswith("counter_corner")},
      corner_state(), item={"parent": "csm:block/venues/concourse/counter_corner_item"}, tab=CONC)


# ------------------------------------------------------------------------------------------
# Runs: the back counter, the suite bar, the drink rail (BlockConcourseRun)
# ------------------------------------------------------------------------------------------
RUN_TEX = {"steel": T("steel"), "steel_dark": T("steel_dark"), "doors": T("doors"),
           "kick": T("kick"), "walnut": T("walnut"), "stone": T("stone"), "led": T("led"),
           "brass": T("brass"), "black": T("black"), "oak": T("oak"),
           "panel": T("rail_panel"), "particle": T("steel")}


def back_counter_parts():
    return [box([0, 15, 0.5], [16, 16, 16], "steel"),
            only(box([0, 1.5, 1], [16, 15, 2], "doors"), "north"),
            box([0, 0, 2], [16, 1.5, 3], "kick", faces=("north", "up")),
            box([0, 1.5, 2], [16, 15, 16], "steel_dark", faces=("south",)),
            box([0, 16, 15], [16, 20, 16], "steel", faces=("north", "south", "up"))]


def back_counter_end():
    return [box([0, 0, 1], [0.75, 15, 16], "steel"),
            box([0, 16, 15], [0.75, 20, 16], "steel", faces=("west", "east"))]


def bar_parts():
    els = [box([0, 15, 0], [16, 16, 9], "stone"),
           only(box([0, 1.5, 2], [16, 15, 3.5], "walnut"), "north", "down"),
           only(box([0, 14.5, 1.5], [16, 15, 2], "led"), "north", "down"),
           box([0, 0, 2.5], [16, 1.5, 3.5], "black", faces=("north", "up")),
           box([7.5, 2.25, 1], [8.5, 3, 2], "brass", faces=("east", "west", "up", "down"))]
    els += lsc.pipe_x(2.6, 0.9, 0.6, 0, 16, "brass", ends=False)
    els += [box([0, 13, 9], [16, 14, 16], "steel"),
            box([0, 0, 9], [16, 13, 15.5], "steel_dark", faces=("south",)),
            box([0, 14, 9], [16, 15, 9.5], "steel_dark", faces=("south",))]
    return els


def bar_end():
    return [box([0, 0, 2], [1, 15, 16], "walnut")]


def rail_parts():
    els = [box([0, 15, 1], [16, 16, 9], "oak"),
           box([0, 0, 1.5], [16, 15, 3], "panel", faces=("north", "south")),
           box([7.5, 2.5, 3], [8.5, 3.25, 5.5], "steel", faces=("east", "west", "up", "down"))]
    els += lsc.pipe_x(3, 6, 0.55, 0, 16, "steel", ends=False)
    return els


def rail_end():
    return [box([0, 0, 1.25], [0.75, 15.5, 3.25], "steel")]


def run_models(prefix, parts, end):
    return {prefix: vc.model(RUN_TEX, parts),
            prefix + "_end_l": vc.model(RUN_TEX, end),
            prefix + "_end_r": vc.model(RUN_TEX, [vc.mirror_x(e) for e in end]),
            prefix + "_item": vc.model(RUN_TEX, parts + end + [vc.mirror_x(e) for e in end])}


def run_state(prefix):
    rules = []
    for facing, y in FACINGS:
        rules.append({"when": {"facing": facing}, "apply": dict(model=M(prefix), **ys(y))})
        rules.append({"when": {"facing": facing, "end_l": "true"},
                      "apply": dict(model=M(prefix + "_end_l"), **ys(y))})
        rules.append({"when": {"facing": facing, "end_r": "true"},
                      "apply": dict(model=M(prefix + "_end_r"), **ys(y))})
    return {"multipart": rules}


def add_run(reg, prefix, kind, names, parts, end):
    C.add(reg, 'new BlockConcourseRun("%s", BlockConcourseRun.Kind.%s)' % (reg, kind), names,
          run_models(prefix, parts, end), run_state(prefix),
          item={"parent": "csm:block/venues/concourse/%s_item" % prefix}, tab=CONC)


add_run("concession_back_counter", "back_counter", "BACK_COUNTER",
        ("Concession Back Counter", "Rückwärtiger Arbeitstresen", "Encimera Trasera de Concesión",
         "Bakre Arbetsbänk"), back_counter_parts(), back_counter_end())


# ------------------------------------------------------------------------------------------
# The concession sign: Core's wayfinding panel
# ------------------------------------------------------------------------------------------
@C.texture("sign_icon")
def _sign_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    vc.rect(img, 0, 4, 16, 12, (178, 32, 28))
    vc.rect(img, 0, 4, 16, 5, (110, 20, 18))
    vc.draw_text_centred(img, "HOT", 8, 6, YELLOW)
    return img


C.add("concession_sign", 'new BlockConcessionSign("concession_sign")',
      ("Concession Sign", "Verkaufsstand-Schild", "Letrero de Puesto", "Kioskskylt"),
      {"concession_sign_item": vc.model({"face": T("sign_icon"), "back": T("steel_dark"),
                                         "particle": T("sign_icon")},
                                        [box([0, 4, 7], [16, 12, 9], "back"),
                                         uv(only(box([0, 4, 6.9], [16, 12, 7], "face"),
                                                 "north"), "north", "face", [0, 4, 16, 12])])},
      {"forge_marker": 1, "defaults": {"model": "csm:wayfinding/wayfinding_panel_base"},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "inventory": [{"model": M("concession_sign_item")}]}},
      tab=CONC)


# ------------------------------------------------------------------------------------------
# The digital menu board (3 x 2, on the wall): four 256 px menus
# ------------------------------------------------------------------------------------------
MENU_W, MENU_H = 256, 168   # the screen's 47 x 31 px face, at its own aspect


def icon(img, kind, x, y):
    """A 20 x 20 food pictogram with its top-left at (x, y)."""
    R = lambda x0, y0, x1, y1, c: vc.rect(img, x + x0, y + y0, x + x1, y + y1, c)  # noqa: E731
    D = lambda cx, cy, r, c: vc.disc(img, x + cx, y + cy, r, c)  # noqa: E731
    if kind == "hotdog":
        R(1, 10, 19, 15, (222, 170, 96)); R(1, 9, 19, 10, (238, 196, 120))
        R(2, 9, 18, 12, (176, 66, 44)); R(4, 10, 16, 11, (232, 196, 40))
        R(0, 11, 2, 13, (176, 66, 44)); R(18, 11, 20, 13, (176, 66, 44))
    elif kind == "burger":
        R(3, 5, 17, 9, (214, 150, 70)); R(4, 4, 16, 5, (214, 150, 70))
        R(2, 9, 18, 10, (90, 170, 60)); R(2, 10, 18, 12, (112, 60, 36))
        R(2, 12, 18, 13, (240, 190, 40)); R(3, 13, 17, 16, (214, 150, 70))
        for sx in (6, 10, 13):
            R(sx, 6, sx + 1, 7, (250, 236, 200))
    elif kind == "fries":
        R(4, 9, 16, 19, (204, 34, 36)); R(5, 10, 15, 11, (230, 70, 60))
        for i, h in enumerate((5, 3, 6, 2, 4)):
            R(5 + i * 2, h, 6 + i * 2, 10, (246, 206, 70))
    elif kind == "chicken":
        R(3, 8, 17, 17, (190, 120, 50)); R(4, 7, 16, 8, (210, 140, 60))
        R(5, 10, 9, 13, (220, 160, 80)); R(11, 12, 15, 15, (220, 160, 80))
    elif kind == "beer":
        R(5, 5, 14, 18, (236, 170, 40)); R(5, 3, 14, 6, (250, 248, 240))
        R(14, 8, 17, 9, (200, 200, 200)); R(16, 8, 17, 15, (200, 200, 200))
        R(14, 14, 17, 15, (200, 200, 200)); R(7, 8, 8, 16, (250, 210, 90))
    elif kind == "soda":
        R(5, 6, 15, 19, (204, 34, 36)); R(4, 4, 16, 6, (240, 240, 240))
        R(10, 0, 11, 5, (240, 240, 240)); R(6, 10, 14, 13, (250, 250, 250))
    elif kind == "water":
        R(7, 2, 13, 4, (40, 110, 200)); R(6, 4, 14, 19, (170, 210, 240))
        R(6, 9, 14, 13, (40, 110, 200))
    elif kind == "cider":
        R(6, 4, 13, 18, (250, 220, 120)); R(6, 4, 13, 6, (250, 248, 240))
        D(15, 13, 3.2, (200, 40, 40)); R(15, 9, 16, 11, (90, 60, 30))
    elif kind == "pizza":
        for row in range(14):
            w = 14 - row
            R(10 - w // 2 - 1, 4 + row, 10 + w // 2 + 1, 5 + row, (244, 200, 90))
        R(2, 3, 18, 5, (200, 140, 60))
        for cx, cy in ((7, 7), (12, 8), (10, 12)):
            D(cx, cy, 1.6, (190, 40, 30))
    elif kind == "pretzel":
        for cx in (7, 13):
            D(cx, 9, 5, (150, 84, 34)); D(cx, 9, 2.6, (0, 0, 0, 0))
        R(8, 12, 12, 16, (150, 84, 34)); R(6, 15, 14, 17, (150, 84, 34))
        for sx, sy in ((5, 6), (14, 7), (9, 15), (12, 5)):
            R(sx, sy, sx + 1, sy + 1, (250, 250, 250))
    elif kind == "nachos":
        R(2, 13, 18, 18, (40, 40, 40)); R(3, 12, 17, 13, (60, 60, 60))
        for i in range(5):
            R(3 + i * 3, 8 + (i % 2) * 2, 6 + i * 3, 13, (238, 190, 70))
        R(5, 9, 15, 11, (250, 160, 20))
    elif kind == "popcorn":
        for i in range(4):
            R(5 + i * 3, 8, 6 + i * 3 + 1, 19, (204, 34, 36) if i % 2 == 0 else (250, 250, 250))
        for cx, cy in ((7, 6), (10, 5), (13, 6), (9, 3), (12, 3), (6, 4), (14, 4)):
            D(cx, cy, 2, (252, 244, 210))
    elif kind == "peanuts":
        R(4, 6, 16, 18, (220, 200, 160)); R(4, 6, 16, 9, (180, 40, 40))
        for cx, cy in ((7, 12), (12, 14), (10, 11)):
            D(cx, cy, 1.6, (176, 128, 70))
    elif kind == "cone":
        D(10, 6, 5, (250, 238, 220)); D(10, 3, 3, (250, 246, 236))
        for row in range(10):
            w = 9 - row
            R(10 - w // 2 - 1, 9 + row, 10 + w // 2 + 1, 10 + row, (214, 160, 90))
    elif kind == "cotton":
        D(10, 7, 6, (246, 160, 200)); D(7, 5, 3, (250, 190, 220))
        R(9, 12, 11, 19, (240, 240, 230))
    elif kind == "churro":
        for i in range(12):
            R(6 + i // 2, 3 + i, 9 + i // 2, 5 + i, (200, 130, 60))
        R(4, 15, 16, 19, (204, 34, 36))
    elif kind == "candy":
        R(6, 6, 14, 14, (200, 40, 160)); R(2, 8, 6, 12, (240, 240, 240))
        R(14, 8, 18, 12, (240, 240, 240)); R(8, 8, 12, 12, (250, 220, 60))
    elif kind == "sundae":
        R(6, 15, 14, 19, (220, 220, 230)); R(4, 9, 16, 15, (240, 240, 245))
        D(10, 8, 5, (250, 240, 220)); R(6, 6, 14, 8, (120, 70, 40)); D(10, 3, 1.6, (200, 30, 30))


MENUS = [
    ("HOT DOGS & GRILL", RED, [("hotdog", "ALL BEEF HOT DOG", "6.50"),
                               ("hotdog", "FOOT LONG DOG", "9.00"),
                               ("burger", "CHEESEBURGER", "11.00"),
                               ("chicken", "CHICKEN TENDERS", "12.00"),
                               ("fries", "FRIES", "6.00")]),
    ("BEER & DRINKS", GOLD, [("beer", "DOMESTIC DRAFT", "11.00"),
                             ("beer", "CRAFT DRAFT", "13.50"),
                             ("cider", "HARD CIDER", "12.00"),
                             ("soda", "FOUNTAIN SODA", "6.00"),
                             ("water", "BOTTLED WATER", "5.00")]),
    ("PIZZA & SNACKS", (232, 110, 30), [("pizza", "CHEESE SLICE", "8.00"),
                                        ("pizza", "PEPPERONI SLICE", "9.00"),
                                        ("pretzel", "SOFT PRETZEL", "7.00"),
                                        ("nachos", "NACHOS", "8.50"),
                                        ("popcorn", "POPCORN", "7.50")]),
    ("SWEETS", (210, 70, 140), [("cone", "SOFT SERVE", "6.50"),
                                ("sundae", "SUNDAE", "8.00"),
                                ("cotton", "COTTON CANDY", "6.00"),
                                ("churro", "CHURRO", "5.50"),
                                ("candy", "CANDY", "4.50")]),
]


def menu_picture(layout, lit_only=False):
    def draw():
        title, accent, rows = MENUS[layout]
        img = Image.new("RGBA", (256, 256), (0, 0, 0, 0) if lit_only else (8, 8, 10, 255))
        bg = (16, 18, 24)
        vc.rect(img, 0, 0, MENU_W, MENU_H, bg)
        # the header band in the stand's colour, its title centred in white
        vc.rect(img, 0, 0, MENU_W, 28, accent)
        vc.rect(img, 0, 28, MENU_W, 30, vc.shade(accent, 0.6))
        vc.draw_text_centred(img, title, MENU_W / 2, 7, (255, 255, 255), scale=3)
        for i, (kind, name, price) in enumerate(rows):
            y = 36 + i * 26
            if i % 2 == 1:
                vc.rect(img, 0, y - 3, MENU_W, y + 23, (24, 27, 34))
            icon(img, kind, 8, y)
            vc.draw_text(img, name, 36, y + 5, (236, 236, 230), scale=2)
            vc.draw_text(img, price, MENU_W - 10 - vc.text_width(price, 3), y + 2, YELLOW,
                         scale=3)
        vc.rect(img, 0, MENU_H - 4, MENU_W, MENU_H, vc.shade(accent, 0.6))
        return img
    return draw


for _l in range(4):
    C.textures["menu_%d" % _l] = menu_picture(_l)
    C.textures["menu_%d_e" % _l] = menu_picture(_l)
C.textures["board_frame"] = lambda: vc.swatch((20, 20, 22), 430, grain=2)

MENU_UV = [0, 0, 16, round(16.0 * MENU_H / 256.0, 3)]


def menu_board_model():
    els = [box([-16, 0, 14], [32, 32, 16], "frame", faces=("north", "south", "east", "west",
                                                            "up", "down"))]
    for f in ("north", "south"):
        els[0]["faces"][f]["uv"] = [0, 0, 16, 16]
    for f in ("up", "down"):
        els[0]["faces"][f]["uv"] = [0, 0, 16, 2]
    for f in ("east", "west"):
        els[0]["faces"][f]["uv"] = [0, 0, 2, 16]
    screen = {"from": [-15.5, 0.5, 13.9], "to": [31.5, 31.5, 14],
              "faces": {"north": {"texture": "#screen", "uv": MENU_UV}}}
    return vc.model({"frame": T("board_frame"), "screen": T("menu_0"),
                     "particle": T("board_frame")}, els + [screen])


def menu_board_icon():
    els = [box([0, 3, 14], [16, 13.7, 16], "frame")]
    els.append(uv(only(box([0.5, 3.5, 13.9], [15.5, 13.2, 14], "screen"), "north"), "north",
                  "screen", MENU_UV))
    return vc.model({"frame": T("board_frame"), "screen": T("menu_0"),
                     "particle": T("board_frame")}, els)


def menu_board_models():
    base = menu_board_model()
    out = {"menu_board": base, "menu_board_item": menu_board_icon()}
    for l in range(4):
        out["menu_board_%d" % l] = {"parent": "csm:block/venues/concourse/menu_board",
                                    "textures": {"screen": T("menu_%d" % l)}}
    return out


def menu_board_state():
    rules = []
    for facing, y in FACINGS:
        for l in range(4):
            rules.append({"when": {"facing": facing, "layout": str(l)},
                          "apply": dict(model=M("menu_board_%d" % l), **ys(y))})
    return {"multipart": rules}


C.add("concession_menu_board",
      unit_java("BlockMenuBoard", "concession_menu_board", [(-16, 0, 13, 32, 32, 16)]),
      ("Digital Menu Board", "Digitale Menütafel", "Menú Digital", "Digital Menytavla"),
      menu_board_models(), menu_board_state(),
      item={"parent": "csm:block/venues/concourse/menu_board_item"}, tab=CONC)


# ------------------------------------------------------------------------------------------
# The machines (BlockConcessionAppliance): a model on and a model off
# ------------------------------------------------------------------------------------------
def label(text_lines, bg, fg, size=32, top=2, scale=1, gap=2):
    """A painted label: lines of text centred from the top of a square texture."""
    def draw():
        img = vc.swatch(bg, sum(map(ord, "".join(text_lines))) % 997, grain=2, size=size)
        y = top
        for line in text_lines:
            vc.draw_text_centred(img, line, size / 2, y, fg, scale=scale)
            y += 5 * scale + gap
        return img
    return draw


C.textures["popcorn_red"] = lambda: vc.swatch(RED, 440, grain=4)
C.textures["popcorn_base"] = label(["POPCORN"], RED, YELLOW, top=2)
C.textures["popcorn_sign"] = label(["POPCORN"], RED, YELLOW, top=1)
C.textures["warmer_sign"] = label(["PRETZELS"], (170, 26, 30), YELLOW, top=1)
C.textures["chip_sign"] = label(["CHIPS"], (40, 40, 44), (250, 170, 30), top=1)


@C.texture("popcorn")
def _popcorn():
    rng = random.Random(441)
    img = vc.swatch((246, 230, 176), 441, grain=6)
    for _ in range(14):
        vc.disc(img, rng.uniform(0, 16), rng.uniform(0, 16), rng.uniform(1.0, 1.8),
                (254, 250, 236))
    px = img.load()
    for _ in range(10):
        px[rng.randrange(16), rng.randrange(16)] = (214, 150, 60, 255)
    return img


def popping_frame(k):
    def draw():
        rng = random.Random(4420 + k)
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for _ in range(9):
            x, y = rng.randrange(1, 15), rng.randrange(1, 15)
            vc.rect(img, x, y, x + 2, y + 1, (254, 248, 226))
            vc.rect(img, x, y + 1, x + 1, y + 2, (250, 232, 170))
        return img
    return draw


vc.animation(C, "popping", [popping_frame(k) for k in range(6)], 2)
C.textures["kettle"] = lambda: vc.brushed((210, 212, 214), 443)


def grill_panel(on):
    def draw():
        img = vc.swatch((40, 40, 44), 444, grain=2)
        for cx in (4, 12):
            vc.disc(img, cx, 8, 2.5, (160, 164, 168))
            vc.rect(img, cx, 6, cx + 1, 8, (60, 60, 60))
        vc.disc(img, 8, 8, 1.3, (255, 170, 30) if on else (70, 50, 30))
        return img
    return draw


C.textures["grill_panel_on"] = grill_panel(True)
C.textures["grill_panel_off"] = grill_panel(False)


@C.texture("rollers")
def _rollers():
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        c = (228, 230, 234) if x % 2 == 0 else (130, 134, 140)
        vc.rect(img, x, 0, x + 1, 16, c)
    return img


def sausage_frame(k, on):
    def draw():
        img = vc.swatch((168, 74, 46), 445, grain=4)
        if on:
            hx = (k * 2) % 16
            vc.rect(img, hx, 0, hx + 2, 16, (214, 120, 84))
            vc.rect(img, (hx + 8) % 16, 0, (hx + 8) % 16 + 1, 16, (196, 100, 70))
        vc.rect(img, 0, 0, 16, 1, (130, 50, 30))
        return img
    return draw


vc.animation(C, "sausage_on", [sausage_frame(k, True) for k in range(8)], 2)
C.textures["sausage_off"] = sausage_frame(0, False)
C.textures["lamp_on"] = lambda: vc.swatch((255, 196, 120), 446, grain=4)
C.textures["lamp_off"] = lambda: vc.swatch((84, 76, 66), 446, grain=2)
C.textures["glow_on"] = lambda: vc.swatch((255, 196, 110), 447, grain=6)
C.textures["glow_off"] = lambda: vc.swatch((60, 40, 30), 447, grain=3)


@C.texture("pretzels")
def _pretzels():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for i, cx in enumerate((5.5, 16, 26.5)):
        icon_y = 14
        for dx in (-2.4, 2.4):
            vc.disc(img, cx + dx, icon_y, 3.6, (150, 84, 34))
        for dx in (-2.4, 2.4):
            vc.disc(img, cx + dx, icon_y, 1.6, (0, 0, 0, 0))
        vc.rect(img, int(cx) - 1, icon_y + 2, int(cx) + 2, icon_y + 7, (150, 84, 34))
        vc.rect(img, int(cx) - 4, icon_y + 6, int(cx) + 5, icon_y + 8, (150, 84, 34))
        px = img.load()
        rng = random.Random(448 + i)
        for _ in range(6):
            x, y = int(cx) + rng.randrange(-4, 5), icon_y + rng.randrange(-3, 7)
            if 0 <= x < 32 and px[x, y][3]:
                px[x, y] = (250, 250, 250, 255)
    return img


@C.texture("chips")
def _chips():
    rng = random.Random(449)
    img = vc.swatch((226, 176, 70), 449, grain=8)
    px = img.load()
    for _ in range(20):
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = rgba(rng.choice(((246, 206, 110), (190, 130, 50))))
    return img


C.textures["nacho_label"] = label(["NACHO", "CHEESE"], (236, 126, 20), (255, 255, 255), top=14,
                                  gap=3)


def cheese_light(on):
    return lambda: vc.swatch((255, 140, 20) if on else (90, 50, 20), 450, grain=2)


C.textures["cheese_on"] = cheese_light(True)
C.textures["cheese_off"] = cheese_light(False)


def softserve_panel(on):
    def draw():
        # the face shows the top 32 x 14.7 of this texture: a readout, then a blue band
        img = vc.brushed(STEEL, 451, size=32)
        vc.rect(img, 3, 2, 29, 8, (20, 22, 26))
        for i, x in enumerate((5, 11, 17, 23)):
            c = ((90, 240, 120) if i < 3 else (250, 190, 60)) if on else (40, 46, 44)
            vc.rect(img, x, 4, x + 4, 6, c)
        vc.rect(img, 0, 10, 32, 13, (40, 70, 140))
        return img
    return draw


C.textures["softserve_on"] = softserve_panel(True)
C.textures["softserve_off"] = softserve_panel(False)


@C.texture("drip")
def _drip():
    img = vc.swatch((60, 62, 66), 452, grain=2)
    for y in range(1, 16, 3):
        vc.rect(img, 1, y, 15, y + 1, (20, 20, 22))
    return img


@C.texture("vent")
def _vent():
    img = vc.brushed(STEEL, 453)
    for y in range(3, 14, 2):
        vc.rect(img, 3, y, 13, y + 1, (60, 62, 66))
    return img


MACHINE_TEX = {"steel": T("steel"), "steel_dark": T("steel_dark"), "black": T("black"),
               "glass": T("glass"), "chrome": T("chrome"), "red": T("popcorn_red"),
               "base_label": T("popcorn_base"), "roof_sign": T("popcorn_sign"),
               "popcorn": T("popcorn"), "popping": T("popping"), "kettle": T("kettle"),
               "rollers": T("rollers"), "pretzels": T("pretzels"), "warmer_sign": T("warmer_sign"),
               "chips": T("chips"), "chip_sign": T("chip_sign"), "nacho": T("nacho_label"),
               "drip": T("drip"), "vent": T("vent"), "particle": T("steel")}


def state_tex(on, **pairs):
    t = dict(MACHINE_TEX)
    for k, base in pairs.items():
        t[k] = T(base + ("_on" if on else "_off"))
    return t


def glass(frm, to, faces):
    return only(box(frm, to, "glass"), *faces)


def hot_dog(on):
    els = [box([1.5, 0.5, 3], [14.5, 3, 13], "steel")]
    for x0, z0 in ((2, 3.5), (13, 3.5), (2, 11.5), (13, 11.5)):
        els.append(box([x0, 0, z0], [x0 + 1, 0.5, z0 + 1], "black"))
    els.append(only(box([1.5, 0.5, 2.75], [14.5, 3, 3], "panel"), "north"))
    els.append(only(box([2.5, 3, 4], [13.5, 3.5, 12], "rollers"), "up"))
    for i in range(5):
        x0 = 3.2 + i * 2.0
        els.append(box([x0, 3.5, 5], [x0 + 1.4, 4.7, 11], "sausage"))
    for x0 in (1.5, 14):
        for z0 in (3, 12.5):
            els.append(box([x0, 3, z0], [x0 + 0.5, 8.5, z0 + 0.5], "chrome"))
    els.append(only(box([1.75, 8, 3.25], [14.25, 8.5, 12.75], "glass"), "up", "down"))
    els.append(glass([2, 4.5, 3.1], [14, 8, 3.3], ("north", "south")))
    els.append(glass([1.6, 4.5, 3.5], [1.9, 8, 12.5], ("east", "west")))
    els.append(glass([14.1, 4.5, 3.5], [14.4, 8, 12.5], ("east", "west")))
    els.append(only(box([2.5, 7.6, 4.5], [13.5, 8, 11.5], "lamp"), "down"))
    return els, state_tex(on, panel="grill_panel", sausage="sausage", lamp="lamp")


def popcorn(on):
    els = [box([1.5, 0, 2], [14.5, 3.5, 14], "red")]
    uv(els[0], "north", "base_label", [0, 0, 16, 4.3])
    for x0 in (1.5, 13.5):
        for z0 in (2, 13):
            els.append(box([x0, 3.5, z0], [x0 + 1, 13, z0 + 1], "red"))
    els += [glass([2.5, 3.5, 2.2], [13.5, 13, 2.5], ("north", "south")),
            glass([2.5, 3.5, 13.5], [13.5, 13, 13.8], ("north", "south")),
            glass([1.6, 3.5, 3], [1.9, 13, 13], ("east", "west")),
            glass([14.1, 3.5, 3], [14.4, 13, 13], ("east", "west")),
            box([1, 13, 1.5], [15, 14, 14.5], "red"),
            box([7.6, 11.5, 7.6], [8.4, 13, 8.4], "chrome", faces=("north", "south", "east",
                                                                   "west"))]
    sign = box([2, 14, 7.25], [14, 16.5, 8.75], "red")
    uv(sign, "north", "roof_sign", [0, 0, 16, 3.33])
    uv(sign, "south", "roof_sign", [0, 0, 16, 3.33])
    els.append(sign)
    els += lsc.post(8, 8, 2.4, 8.5, 11.5, "kettle")
    els.append(box([2.5, 3.5, 3], [13.5, 5.5, 13], "popcorn"))
    if on:
        for z in (5, 10.5):
            els.append({"from": [2.6, 5.5, z], "to": [13.4, 12.5, z],
                        "faces": {"north": {"texture": "#popping", "uv": [0, 0, 16, 10]},
                                  "south": {"texture": "#popping", "uv": [0, 0, 16, 10]}}})
    return els, dict(MACHINE_TEX)


def pretzel(on):
    els = [box([2, 0, 3], [14, 2, 13], "black")]
    for x0 in (2, 13.25):
        for z0 in (3, 12.25):
            els.append(box([x0, 2, z0], [x0 + 0.75, 11.5, z0 + 0.75], "black"))
    els += [glass([2.75, 2, 3.1], [13.25, 11.5, 3.4], ("north", "south")),
            glass([2.1, 2, 3.75], [2.4, 11.5, 12.25], ("east", "west")),
            glass([13.6, 2, 3.75], [13.9, 11.5, 12.25], ("east", "west")),
            only(box([2.75, 2, 12.4], [13.25, 11.5, 12.9], "glow"), "north", "south"),
            box([2.75, 6.5, 4], [13.25, 7, 12.25], "steel", faces=("up", "down", "north"))]
    for y0 in (2, 7):
        els.append({"from": [3, y0, 8], "to": [13, y0 + 4.2, 8],
                    "faces": {"north": {"texture": "#pretzels", "uv": [0, 5, 16, 13]},
                              "south": {"texture": "#pretzels", "uv": [16, 5, 0, 13]}}})
    top = box([2, 11.5, 3], [14, 13, 13], "black")
    uv(top, "north", "warmer_sign", [0, 0, 16, 2.33])
    els.append(top)
    return els, state_tex(on, glow="glow")


def nacho(on):
    els = [box([1, 0, 3.5], [8, 1.5, 13], "steel"),
           glass([1.5, 1.5, 3.6], [7.5, 9, 3.9], ("north", "south")),
           glass([1.1, 1.5, 4], [1.4, 9, 12.5], ("east", "west")),
           box([1, 1.5, 12.5], [8, 9, 13], "steel", faces=("north", "south", "east", "west")),
           box([1.5, 1.5, 4], [7.5, 4.5, 12.5], "chips", faces=("up", "north", "east", "west")),
           box([1, 9, 3.5], [8, 10.5, 13], "steel")]
    uv(els[-1], "north", "chip_sign", [0, 0, 16, 3.4])
    body = box([9, 0, 5], [15, 9, 13], "steel")
    uv(body, "north", "nacho", [0, 0, 16, 16])
    els += [body,
            box([10.5, 9, 7], [13.5, 10.5, 10], "black"),
            box([11.5, 10.5, 8], [12.5, 12, 9], "chrome"),
            box([10, 12, 7.75], [14, 12.75, 9.25], "black"),
            box([11.5, 7, 3.5], [12.5, 8, 7], "chrome"),
            box([9.5, 0, 2.5], [14.5, 1, 5], "drip", faces=("up", "north", "east", "west")),
            only(box([11, 9.5, 6.9], [13, 10.25, 7], "light"), "north")]
    return els, state_tex(on, light="cheese")


def soft_serve(on):
    els = [box([2, 1, 3], [14, 15, 14], "steel")]
    for f in ("east", "west"):
        els[0]["faces"][f]["texture"] = "#vent"
    for x0, z0 in ((2.5, 3.5), (12.5, 3.5), (2.5, 12.5), (12.5, 12.5)):
        els.append(box([x0, 0, z0], [x0 + 1, 1, z0 + 1], "black"))
    panel = only(box([2, 9.5, 2.75], [14, 15, 3], "panel"), "north")
    uv(panel, "north", "panel", [0, 0, 16, 7.33])
    els += [panel,
            box([3, 6, 1.5], [13, 9.5, 3], "steel"),
            box([3, 2, 0.5], [13, 3, 3], "drip", faces=("up", "north", "east", "west")),
            box([3, 15, 4], [7.5, 15.5, 12], "steel_dark"),
            box([8.5, 15, 4], [13, 15.5, 12], "steel_dark")]
    for x0 in (4.5, 7.5, 10.5):
        els += [box([x0, 9.5, 1], [x0 + 1, 11.5, 2], "black"),
                box([x0 - 0.25, 11.5, 0.75], [x0 + 1.25, 12.5, 2.25], "black"),
                box([x0, 5, 1.75], [x0 + 1, 6, 2.5], "chrome")]
    return els, state_tex(on, panel="softserve")


APPLIANCES = [
    ("hot_dog_roller_grill", "HOT_DOG_ROLLER", hot_dog,
     ("Hot Dog Roller Grill", "Hotdog-Rollengrill", "Parrilla de Rodillos para Perritos",
      "Korvgrill med Rullar")),
    ("popcorn_machine", "POPCORN", popcorn,
     ("Popcorn Machine", "Popcornmaschine", "Máquina de Palomitas", "Popcornmaskin")),
    ("pretzel_warmer", "PRETZEL_WARMER", pretzel,
     ("Pretzel Warmer", "Brezelwärmer", "Calentador de Pretzels", "Kringelvärmare")),
    ("nacho_cheese_dispenser", "NACHO", nacho,
     ("Nacho Station", "Nacho-Station", "Estación de Nachos", "Nachostation")),
    ("soft_serve_machine", "SOFT_SERVE", soft_serve,
     ("Soft-Serve Machine", "Softeismaschine", "Máquina de Helado Suave", "Mjukglassmaskin")),
]
for _reg, _kind, _fn, _names in APPLIANCES:
    _models = {}
    for _on in (True, False):
        _els, _tex = _fn(_on)
        _models["%s_%s" % (_reg, "on" if _on else "off")] = vc.model(_tex, _els)
    _rules = []
    for _f, _y in FACINGS:
        for _on in ("true", "false"):
            _rules.append({"when": {"facing": _f, "on": _on},
                           "apply": dict(model=M("%s_%s" % (_reg, "on" if _on == "true"
                                                             else "off")), **ys(_y))})
    C.add(_reg, 'new BlockConcessionAppliance("%s", BlockConcessionAppliance.Kind.%s)'
          % (_reg, _kind), _names, _models, {"multipart": _rules},
          item={"parent": "csm:block/venues/concourse/%s_on" % _reg}, tab=CONC)


# ------------------------------------------------------------------------------------------
# The portable beer kiosk (OBJ: 2 wide, 3 tall)
# ------------------------------------------------------------------------------------------
@C.texture("kiosk_front")
def _kiosk_front():
    img = Image.new("RGBA", (64, 64), rgba(NAVY))
    vc.rect(img, 0, 0, 64, 28, NAVY)
    for y in (2, 24):
        vc.rect(img, 0, y, 64, y + 2, GOLD)
    vc.draw_text_centred(img, "ICE COLD", 32, 8, (255, 255, 255), scale=2)
    vc.draw_text_centred(img, "DRAFTS", 32, 17, GOLD)
    return img


@C.texture("kiosk_board")
def _kiosk_board():
    img = Image.new("RGBA", (64, 64), (14, 14, 16, 255))
    vc.rect(img, 0, 0, 64, 10, GOLD)
    vc.draw_text_centred(img, "DRAFTS", 32, 2, (20, 20, 20))
    rows = (("LAGER", "11"), ("IPA", "13"), ("LIGHT", "11"), ("STOUT", "13"), ("CIDER", "12"))
    for i, (name, price) in enumerate(rows):
        y = 14 + i * 9
        vc.draw_text(img, name, 4, y, (236, 236, 230))
        vc.draw_text(img, price, 60 - vc.text_width(price), y, YELLOW)
    return img


@C.texture("kiosk_canopy_sign")
def _canopy_sign():
    img = Image.new("RGBA", (128, 128), rgba(RED))
    vc.rect(img, 0, 0, 128, 28, RED)
    vc.rect(img, 0, 0, 128, 2, YELLOW)
    vc.rect(img, 0, 26, 128, 28, YELLOW)
    vc.draw_text_centred(img, "COLD BEER", 64, 6, (255, 255, 255), scale=3)
    return img


C.textures["kiosk_canopy_sign_e"] = _canopy_sign
C.textures["kiosk_navy"] = lambda: vc.swatch(NAVY, 460, grain=3)
C.textures["kiosk_red"] = lambda: vc.swatch(RED, 461, grain=3)
C.textures["kiosk_light"] = lambda: vc.swatch((255, 246, 220), 462, grain=2)
for _n, _c in (("handle_red", (190, 30, 30)), ("handle_gold", GOLD),
               ("handle_green", (30, 120, 60)), ("handle_black", (30, 30, 30))):
    C.textures[_n] = (lambda c: (lambda: vc.swatch(c, 463, grain=3)))(_c)

KIOSK_MTL = {"steel": T("steel"), "steel_dark": T("steel_dark"), "black": T("black"),
             "chrome": T("chrome"), "front": T("kiosk_front"), "board": T("kiosk_board"),
             "navy": T("kiosk_navy"), "red": T("kiosk_red"), "sign": T("kiosk_canopy_sign"),
             "light": T("kiosk_light"), "h_red": T("handle_red"), "h_gold": T("handle_gold"),
             "h_green": T("handle_green"), "h_black": T("handle_black")}


def obox(x0, x1, y0, y1, z0, z1, mat, faces="nsewud", over=None):
    """vc.obox, with some faces given another material and uv rectangle:
    over = {"n": (mat, (u0, v0, u1, v1))}."""
    polys = vc.obox(x0, x1, y0, y1, z0, z1, mat, faces)
    if over:
        order = [f for f in "nsewud" if f in faces]
        for p, f in zip(polys, order):
            if f in over:
                m, (u0, v0, u1, v1) = over[f]
                p.mat = m
                # the corners as obox lists them: north (x1,y0) (x0,y0) (x0,y1) (x1,y1) seen
                # from the front: bottom-left, bottom-right, top-right, top-left; v runs down
                p.uvs = [(u0, v1), (u1, v1), (u1, v0), (u0, v0)]
    return polys


def kiosk_polys():
    P = []
    for x0, z0 in ((3, 5), (27, 5), (3, 10), (27, 10)):
        P += vc.obox(x0, x0 + 2, 0, 1.5, z0, z0 + 2, "black")
    P += obox(1, 31, 1.5, 15, 3, 14, "navy", over={"n": ("front", (0, 0, 1, 28 / 64.0))})
    P += vc.obox(0, 32, 15, 16.5, 2, 15, "steel")
    P += vc.obox(10, 22, 16.5, 17, 3.5, 7, "steel_dark", faces="nsewu")
    P += vc.obox(15, 17, 16.5, 22, 7, 9, "chrome", faces="nsewu")
    P += vc.obox(9, 23, 22, 24, 7, 9, "chrome")
    for i, (xc, h) in enumerate(((10.5, "h_red"), (14.25, "h_gold"), (17.75, "h_green"),
                                 (21.5, "h_black"))):
        P += vc.obox(xc - 0.5, xc + 0.5, 20.5, 22, 5.5, 7, "chrome", faces="nsewd")
        P += vc.obox(xc - 0.75, xc + 0.75, 24, 28.5, 7.25, 8.75, h)
    P += obox(1, 31, 16.5, 38, 13, 14.5, "navy", over={"n": ("board", (0, 0, 1, 1))})
    for x0 in (0, 31):
        P += vc.obox(x0, x0 + 1, 16.5, 40, 13, 15, "steel")
    P += obox(0, 32, 40, 46, 0, 16, "red", faces="nsewud",
              over={"n": ("sign", (0, 0, 1, 28 / 128.0)), "d": ("light", (0, 0, 1, 1))})
    return P


_kiosk = vc.Obj(GEN, "kiosk", "beer_kiosk")
for _p in kiosk_polys():
    _kiosk.add(_p)
C.extra["models/block/venues/concourse/beer_kiosk.obj"] = _kiosk.text()
C.extra["models/block/venues/concourse/kiosk.mtl"] = vc.mtl_text(GEN, KIOSK_MTL)


def kiosk_icon():
    tex = {"navy": T("kiosk_navy"), "front": T("kiosk_front"), "steel": T("steel"),
           "red": T("kiosk_red"), "sign": T("kiosk_canopy_sign"), "chrome": T("chrome"),
           "particle": T("kiosk_navy")}
    body = box([1, 1, 5], [15, 7, 11], "navy")
    uv(body, "north", "front", [0, 0, 16, 7])
    canopy = box([0.5, 13, 4], [15.5, 15.5, 12], "red")
    uv(canopy, "north", "sign", [0, 0, 16, 3.5])
    return vc.model(tex, [body, box([0.5, 7, 4.5], [15.5, 7.75, 11.5], "steel"),
                          box([7, 7.75, 7], [9, 10.5, 8], "chrome"),
                          box([1, 7.75, 10], [2, 13, 11], "steel"),
                          box([14, 7.75, 10], [15, 13, 11], "steel"), canopy])


C.add("beer_kiosk",
      unit_java("BlockBeerKiosk", "beer_kiosk", [(1, 0, 3, 31, 17, 14), (0, 17, 13, 32, 40, 15),
                                                 (0, 40, 0, 32, 46, 16)]),
      ("Portable Beer Kiosk", "Mobiler Bierstand", "Quiosco de Cerveza Portátil",
       "Mobil Ölkiosk"),
      {"beer_kiosk_item": kiosk_icon()},
      {"forge_marker": 1, "defaults": {"model": M("beer_kiosk") + ".obj",
                                       "custom": {"ambient": False}},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "inventory": [{"model": M("beer_kiosk_item"), "custom": {}}]}},
      tab=CONC)


# ------------------------------------------------------------------------------------------
# The stadium turnstile (OBJ: cabinet, tilted hub, three square arms) and ticket scanner
# ------------------------------------------------------------------------------------------
@C.texture("turnstile_top")
def _turnstile_top():
    img = vc.swatch((52, 54, 58), 470, grain=2)
    vc.frame(img, 0, 0, 16, 16, (196, 200, 204))
    vc.rect(img, 4, 3, 12, 8, (18, 26, 34))
    vc.rect(img, 5, 4, 11, 5, (60, 90, 120))
    return img


def indicator(kind):
    def draw():
        img = Image.new("RGBA", (16, 16), (12, 12, 14, 255))
        vc.frame(img, 0, 0, 16, 16, (90, 92, 96))
        if kind == "green":
            c = (60, 230, 90)
            vc.rect(img, 7, 6, 9, 13, c)
            for i in range(4):
                vc.rect(img, 4 + i, 6 - i, 12 - i, 7 - i, c)
        else:
            c = (240, 50, 40)
            for i in range(9):
                vc.rect(img, 4 + i, 4 + i, 6 + i, 5 + i, c)
                vc.rect(img, 11 - i, 4 + i, 13 - i, 5 + i, c)
        return img
    return draw


C.textures["indicator_green"] = indicator("green")
C.textures["indicator_red"] = indicator("red")

TS_MTL = {"steel": T("steel"), "steel_dark": T("steel_dark"), "black": T("black"),
          "top": T("turnstile_top"), "green": T("indicator_green"), "red": T("indicator_red"),
          "chrome": T("chrome")}

HUB = (5.25, 14.0, 8.0)
_TH = math.acos(1 / math.sqrt(3))       # 54.74 degrees: the arms are mutually square
AXIS = (math.cos(_TH), -math.sin(_TH), 0.0)
ARMS = [(1.0, 0.0, 0.0), (0.0, -math.sqrt(0.5), math.sqrt(0.5)),
        (0.0, -math.sqrt(0.5), -math.sqrt(0.5))]


def _rot(v, k, ang):
    """v turned ang radians about the unit axis k (Rodrigues)."""
    c, s = math.cos(ang), math.sin(ang)
    kx = (k[1] * v[2] - k[2] * v[1], k[2] * v[0] - k[0] * v[2], k[0] * v[1] - k[1] * v[0])
    kd = k[0] * v[0] + k[1] * v[1] + k[2] * v[2]
    return tuple(v[i] * c + kx[i] * s + k[i] * kd * (1 - c) for i in range(3))


def _basis(d):
    """Two unit vectors square to d and to each other."""
    a = (0.0, 0.0, 1.0) if abs(d[2]) < 0.9 else (1.0, 0.0, 0.0)
    u = vc._norm(vc._cross(d, a))
    w = vc._cross(d, u)
    return u, w


def prism(p0, d, length, r, mat, sides=8, caps=(True, True)):
    """A round bar (an n-gon prism) from p0 along unit d."""
    u, w = _basis(d)
    p1 = tuple(p0[i] + d[i] * length for i in range(3))
    ring = []
    for i in range(sides):
        a = 2 * math.pi * (i + 0.5) / sides
        ring.append(tuple(u[j] * r * math.cos(a) + w[j] * r * math.sin(a) for j in range(3)))
    P = []
    for i in range(sides):
        a, b = ring[i], ring[(i + 1) % sides]
        n = tuple((a[j] + b[j]) / 2 for j in range(3))
        P.append(vc.Poly([tuple(p0[j] + a[j] for j in range(3)),
                          tuple(p0[j] + b[j] for j in range(3)),
                          tuple(p1[j] + b[j] for j in range(3)),
                          tuple(p1[j] + a[j] for j in range(3))], mat, n))
    if caps[0]:
        P.append(vc.Poly([tuple(p0[j] + q[j] for j in range(3)) for q in ring], mat,
                         tuple(-x for x in d)))
    if caps[1]:
        P.append(vc.Poly([tuple(p1[j] + q[j] for j in range(3)) for q in ring], mat, d))
    return P


def turnstile_polys(turned, lamp):
    P = []
    P += vc.obox(1, 5, 0, 14.5, 1.5, 14.5, "steel")
    P += obox(0.75, 5.25, 14.5, 15.5, 1.25, 14.75, "steel_dark", faces="nsewud",
              over={"u": ("top", (0, 0, 1, 1))})
    P += obox(1.75, 4.5, 10.5, 13.5, 1.4, 1.5, lamp, faces="n", over={"n": (lamp, (0, 0, 1, 1))})
    P += obox(1.75, 4.5, 10.5, 13.5, 14.5, 14.6, "red", faces="s")
    P += vc.obox(0.75, 5.25, 0, 0.75, 1.25, 14.75, "black", faces="nsewu")
    hub0 = tuple(HUB[i] - AXIS[i] * 0.4 for i in range(3))
    P += prism(hub0, AXIS, 1.8, 1.7, "steel_dark")
    h = tuple(HUB[i] + AXIS[i] * 1.2 for i in range(3))
    ang = math.radians(60) if turned else 0.0
    for arm in ARMS:
        a = _rot(arm, AXIS, ang)
        P += prism(h, a, 9.2, 0.55, "chrome", caps=(False, False))
        tip = tuple(h[i] + a[i] * 9.2 for i in range(3))
        P += prism(tip, a, 0.9, 0.7, "black")
    return P


for _name, _turned, _lamp in (("turnstile_rest_green", False, "green"),
                              ("turnstile_turned_green", True, "green"),
                              ("turnstile_rest_red", False, "red")):
    _o = vc.Obj(GEN, "turnstile", _name)
    for _p in turnstile_polys(_turned, _lamp):
        _o.add(_p)
    C.extra["models/block/venues/concourse/%s.obj" % _name] = _o.text()
C.extra["models/block/venues/concourse/turnstile.mtl"] = vc.mtl_text(GEN, TS_MTL)


def turnstile_icon():
    tex = {"steel": T("steel"), "dark": T("steel_dark"), "chrome": T("chrome"),
           "lamp": T("indicator_green"), "particle": T("steel")}
    els = [box([1, 0, 3], [5, 14, 13], "steel"), box([0.75, 14, 2.75], [5.25, 15, 13.25], "dark"),
           only(box([1.75, 10, 2.9], [4.5, 13, 3], "lamp"), "north"),
           box([5, 12.5, 7.5], [15.5, 13.5, 8.5], "chrome"),
           box([5, 6, 12], [6, 13, 13], "chrome"), box([5, 6, 3], [6, 13, 4], "chrome")]
    return vc.model(tex, els)


def turnstile_state():
    def choose(v):
        if v["open"] == "true":
            name = "turnstile_turned_green"
        elif v["ticketed"] == "true":
            name = "turnstile_rest_red"
        else:
            name = "turnstile_rest_green"
        return dict(model=M(name) + ".obj", **ys(dict(FACINGS)[v["facing"]]))
    st = vc.keyed_state([("facing", [f for f, _ in FACINGS]), ("ticketed", ["false", "true"]),
                         ("open", ["false", "true"])], choose,
                        inventory={"model": M("turnstile_item"), "custom": {}})
    st["defaults"] = {"custom": {"ambient": False}}
    return st


C.add("stadium_turnstile", 'new BlockStadiumTurnstile("stadium_turnstile")',
      ("Stadium Turnstile", "Stadion-Drehkreuz", "Torniquete de Estadio", "Arenavändkors"),
      {"turnstile_item": turnstile_icon()}, turnstile_state(), tab=CONC)


def scanner_window(result):
    def draw():
        img = Image.new("RGBA", (16, 16), (20, 22, 26, 255))
        ring = {0: (150, 160, 170), 1: (60, 240, 90), 2: (240, 50, 40)}[result]
        vc.frame(img, 1, 1, 15, 15, ring)
        vc.rect(img, 3, 3, 13, 9, (10, 30, 40) if result == 0 else vc.shade(ring, 0.35))
        vc.rect(img, 4, 6, 12, 7, (255, 60, 60) if result != 1 else (120, 255, 140))
        vc.rect(img, 5, 11, 11, 13, (60, 64, 70))
        return img
    return draw


for _r in range(3):
    C.textures["scanner_window_%d" % _r] = scanner_window(_r)
C.textures["scanner_body"] = lambda: vc.swatch((44, 46, 50), 471, grain=2)


def scanner_model(result):
    els = [box([5, 0, 5], [11, 0.5, 11], "dark")]
    els += lsc.post(8, 8, 1.1, 0.5, 13.5, "steel", top=False, bottom=False)
    head = box([5, 13, 5.5], [11, 17.5, 10], "body")
    head["faces"]["north"] = {"texture": "#window", "uv": [0, 0, 16, 16]}
    head["rotation"] = {"origin": [8, 13, 8], "axis": "x", "angle": -22.5}
    els.append(head)
    return vc.model({"dark": T("steel_dark"), "steel": T("steel"), "body": T("scanner_body"),
                     "window": T("scanner_window_%d" % result),
                     "particle": T("scanner_body")}, els)


C.add("stadium_ticket_scanner", 'new BlockTicketScanner("stadium_ticket_scanner")',
      ("Ticket Scanner", "Ticketscanner", "Escáner de Entradas", "Biljettskanner"),
      {"scanner_%d" % r: scanner_model(r) for r in range(3)},
      {"multipart": [{"when": {"facing": f, "result": str(r)},
                      "apply": dict(model=M("scanner_%d" % r), **ys(y))}
                     for f, y in FACINGS for r in range(3)]},
      item={"parent": "csm:block/venues/concourse/scanner_0"}, tab=CONC)


@C.texture("venue_ticket")
def _ticket():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    vc.rect(img, 1, 4, 15, 12, (244, 238, 220))
    vc.rect(img, 1, 4, 15, 6, (176, 30, 40))
    for y in range(6, 12, 2):
        vc.rect(img, 11, y, 12, y + 1, (150, 140, 120))
    vc.rect(img, 3, 8, 9, 9, (60, 60, 70))
    vc.rect(img, 3, 10, 7, 11, (60, 60, 70))
    vc.rect(img, 1, 7, 2, 9, (0, 0, 0, 0))
    vc.rect(img, 14, 7, 15, 9, (0, 0, 0, 0))
    return img


C.add_item("venue_ticket", "new ItemVenueTicket()",
           ("Venue Ticket", "Eintrittskarte", "Entrada", "Biljett"), "venue_ticket", tab=CONC)


# ------------------------------------------------------------------------------------------
# Suite fixtures
# ------------------------------------------------------------------------------------------
add_run("suite_bar_counter", "suite_bar", "SUITE_BAR",
        ("Suite Bar Counter", "Logen-Bartresen", "Barra de Palco", "Logebardisk"),
        bar_parts(), bar_end())
add_run("suite_drink_rail", "drink_rail", "DRINK_RAIL",
        ("Suite Drink Rail", "Logen-Getränkeablage", "Repisa para Bebidas de Palco",
         "Logens Dryckeshylla"), rail_parts(), rail_end())


# ------------------------------------------------------------------------------------------
# Team store (tint 0 the team colour, 1 the trim)
# ------------------------------------------------------------------------------------------
@C.texture("jersey")
def _jersey():
    img = Image.new("RGBA", (16, 16), (236, 236, 236, 255))
    for y in range(16):
        for x in range(16):
            if (x + y) % 2 == 0:
                img.putpixel((x, y), (224, 224, 224, 255))
    return img


C.textures["trim"] = lambda: Image.new("RGBA", (16, 16), (246, 246, 246, 255))


@C.texture("jersey_number")
def _jersey_number():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    vc.draw_text_centred(img, "19", 16, 8, (250, 250, 250), scale=3)
    return img


@C.texture("standard")
def _standard():
    img = vc.swatch((70, 72, 78), 480, grain=2)
    for y in range(1, 16, 3):
        vc.rect(img, 7, y, 9, y + 2, (20, 20, 22))
    return img


@C.texture("slatwall")
def _slatwall():
    img = vc.swatch((218, 216, 210), 481, grain=2)
    for y in range(3, 16, 4):
        vc.rect(img, 0, y, 16, y + 1, (120, 118, 112))
        vc.rect(img, 0, y + 1, 16, y + 2, (176, 174, 168))
    return img


@C.texture("folded")
def _folded():
    img = Image.new("RGBA", (16, 16), (240, 240, 240, 255))
    for y in range(0, 16, 3):
        vc.rect(img, 0, y, 16, y + 1, (206, 206, 206))
    vc.rect(img, 7, 0, 9, 16, (226, 226, 226))
    return img


C.textures["skin"] = lambda: vc.swatch((236, 236, 232), 482, grain=2)
C.textures["pants"] = lambda: vc.swatch((46, 48, 54), 483, grain=2)
C.textures["light_wood"] = lambda: vc.swatch((214, 186, 146), 484, grain=4)

STORE_TEX = {"jersey": T("jersey"), "trim": T("trim"), "number": T("jersey_number"),
             "chrome": T("chrome"), "standard": T("standard"), "slatwall": T("slatwall"),
             "folded": T("folded"), "skin": T("skin"), "pants": T("pants"),
             "wood": T("light_wood"), "steel_dark": T("steel_dark"), "particle": T("slatwall")}


def jersey(z, dy, numbered, front=True):
    """A jersey facing out on a hanger, its front at z: body, sleeves, trim and number."""
    t = 0.5
    els = [tint(box([4, 2 + dy, z], [12, 13 + dy, z + t], "jersey"), 0),
           tint(box([1.5, 9 + dy, z], [4, 13 + dy, z + t], "jersey"), 0),
           tint(box([12, 9 + dy, z], [14.5, 13 + dy, z + t], "jersey"), 0)]
    if front:
        f = z - 0.06
        els += [tint(only(box([1.5, 9 + dy, f], [4, 10 + dy, z], "trim"), "north"), 1),
                tint(only(box([12, 9 + dy, f], [14.5, 10 + dy, z], "trim"), "north"), 1),
                tint(only(box([4, 3.5 + dy, f], [12, 4.5 + dy, z], "trim"), "north"), 1),
                tint(only(box([4, 5 + dy, f], [12, 5.5 + dy, z], "trim"), "north"), 1),
                tint(only(box([6.5, 12.25 + dy, f], [9.5, 13 + dy, z], "trim"), "north"), 1)]
        if numbered:
            num = {"from": [5, 6 + dy, z - 0.1], "to": [11, 11.5 + dy, z - 0.1],
                   "faces": {"north": {"texture": "#number", "uv": [2, 3.5, 14, 13.5],
                                       "tintindex": 1}}}
            els.append(num)
    els += [box([5, 13 + dy, z + 0.1], [11, 13.5 + dy, z + 0.4], "chrome"),
            box([7.75, 13.5 + dy, z + 0.1], [8.25, 15 + dy, z + 0.4], "chrome")]
    return els


def jersey_rack(numbered):
    els = [box([6.5, 0, 15], [9.5, 16, 16], "standard", faces=("north", "east", "west", "up",
                                                              "down"))]
    els += [box([7.5, 14.5, 10], [8.5, 15.5, 15], "chrome"),
            box([7.5, 14, 6.5], [8.5, 15, 10], "chrome"),
            box([7.5, 13.5, 3], [8.5, 14.5, 6.5], "chrome")]
    els += jersey(11, 1, False, front=False)
    els += jersey(7.5, 0.5, False, front=False)
    els += jersey(4, 0, numbered)
    return els


def cap(x0, y0):
    return [tint(box([x0, y0 + 1, 11], [x0 + 4, y0 + 3.5, 14.5], "jersey"), 0),
            tint(box([x0 + 0.5, y0 + 3.5, 11.5], [x0 + 3.5, y0 + 4, 14], "jersey"), 0),
            tint(box([x0, y0 + 1, 8.5], [x0 + 4, y0 + 1.5, 11], "trim"), 1),
            tint(only(box([x0 + 1.25, y0 + 2, 10.94], [x0 + 2.75, y0 + 3, 11], "trim"),
                      "north"), 1),
            box([x0 + 1.75, y0 + 2, 14.5], [x0 + 2.25, y0 + 2.5, 15], "chrome")]


def hat_wall():
    els = [box([0, 0, 15], [16, 16, 16], "slatwall", faces=("north", "south", "east", "west",
                                                             "up", "down"))]
    for i in range(3):
        for j in range(3):
            els += cap(1 + 5 * i, 0.5 + 5 * j)
    return els


def apparel_table():
    els = [box([0, 10.5, 1], [16, 11.5, 15], "wood"),
           box([0.5, 3, 1.5], [15.5, 3.5, 14.5], "wood")]
    for x0, z0 in ((0.5, 1.5), (14.5, 1.5), (0.5, 13.5), (14.5, 13.5)):
        els.append(box([x0, 0, z0], [x0 + 1, 10.5, z0 + 1], "wood"))
    k = 0
    for row, z0 in enumerate((2.5, 8.5)):
        for col, x0 in enumerate((1.5, 6, 10.5)):
            h = (3.0, 2.5, 3.5, 2.0, 3.0, 2.5)[k]
            els.append(tint(box([x0, 11.5, z0], [x0 + 4, 11.5 + h, z0 + 4.5], "folded"), k % 2))
            k += 1
    for x0 in (1.5, 8.5):
        els.append(tint(box([x0, 3.5, 3], [x0 + 6, 5.5, 13], "folded"), 0 if x0 < 5 else 1))
    return els


def mannequin():
    els = lsc.post(8, 8.5, 3, 0, 0.5, "chrome", bottom=True)
    els += [box([7.75, 0.5, 9.25], [8.25, 6, 9.75], "chrome"),
            box([5.25, 1, 6.5], [7.5, 2, 9.75], "skin"),
            box([8.5, 1, 6.5], [10.75, 2, 9.75], "skin"),
            box([5.25, 2, 6.75], [7.5, 14.5, 9.25], "pants"),
            box([8.5, 2, 6.75], [10.75, 14.5, 9.25], "pants"),
            tint(box([4.75, 13.5, 6.25], [11.25, 24, 9.75], "jersey"), 0),
            tint(box([2.75, 18.5, 6.75], [4.75, 24, 9.25], "jersey"), 0),
            tint(box([11.25, 18.5, 6.75], [13.25, 24, 9.25], "jersey"), 0),
            tint(box([2.7, 18.5, 6.7], [4.8, 19.5, 9.3], "trim"), 1),
            tint(box([11.2, 18.5, 6.7], [13.3, 19.5, 9.3], "trim"), 1),
            tint(box([4.7, 15, 6.2], [11.3, 16, 9.8], "trim"), 1),
            tint(box([4.7, 16.5, 6.2], [11.3, 17, 9.8], "trim"), 1),
            tint(box([6.5, 23.5, 6.19], [9.5, 24.1, 9.81], "trim"), 1),
            tint(only(box([7, 19.5, 6.15], [9, 21.5, 6.25], "trim"), "north"), 1),
            {"from": [5.5, 17.5, 9.85], "to": [10.5, 23, 9.85],
             "faces": {"south": {"texture": "#number", "uv": [2, 3.5, 14, 13.5],
                                 "tintindex": 1}}},
            box([3.1, 14.75, 7.1], [4.4, 18.5, 8.65], "skin", faces=("north", "south", "east",
                                                                    "west", "down")),
            box([11.6, 14.75, 7.1], [12.9, 18.5, 8.65], "skin", faces=("north", "south", "east",
                                                                      "west", "down")),
            box([7.25, 24, 7.25], [8.75, 25, 8.75], "skin", faces=("north", "south", "east",
                                                                  "west")),
            box([6.5, 25, 6.5], [9.5, 29, 9.5], "skin")]
    return els


def store_state(model):
    return {"multipart": [{"when": {"facing": f}, "apply": dict(model=M(model), **ys(y))}
                          for f, y in FACINGS]}


def rack_state():
    rules = []
    for f, y in FACINGS:
        for n in ("true", "false"):
            rules.append({"when": {"facing": f, "numbered": n},
                          "apply": dict(model=M("jersey_rack" if n == "true"
                                                else "jersey_rack_blank"), **ys(y))})
    return {"multipart": rules}


C.add("jersey_wall_rack", 'new BlockJerseyRack("jersey_wall_rack")',
      ("Jersey Wall Rack", "Trikot-Wandständer", "Exhibidor de Camisetas",
       "Väggställ för Matchtröjor"),
      {"jersey_rack": vc.model(STORE_TEX, jersey_rack(True)),
       "jersey_rack_blank": vc.model(STORE_TEX, jersey_rack(False))},
      rack_state(), item={"parent": "csm:block/venues/concourse/jersey_rack"}, tab=CONC)
C.add("hat_wall", 'new BlockTeamStoreFixture("hat_wall", BlockTeamStoreFixture.Kind.HAT_WALL)',
      ("Cap Wall", "Kappenwand", "Pared de Gorras", "Kepsvägg"),
      {"hat_wall": vc.model(STORE_TEX, hat_wall())}, store_state("hat_wall"),
      item={"parent": "csm:block/venues/concourse/hat_wall"}, tab=CONC)
C.add("team_apparel_table",
      'new BlockTeamStoreFixture("team_apparel_table", '
      'BlockTeamStoreFixture.Kind.APPAREL_TABLE)',
      ("Team Apparel Table", "Fanartikel-Tisch", "Mesa de Ropa del Equipo",
       "Bord med Lagkläder"),
      {"apparel_table": vc.model(STORE_TEX, apparel_table())}, store_state("apparel_table"),
      item={"parent": "csm:block/venues/concourse/apparel_table"}, tab=CONC)
C.add("jersey_mannequin",
      'new BlockTeamStoreFixture("jersey_mannequin", BlockTeamStoreFixture.Kind.MANNEQUIN)',
      ("Jersey Mannequin", "Schaufensterpuppe mit Trikot", "Maniquí con Camiseta",
       "Skyltdocka med Matchtröja"),
      {"jersey_mannequin": vc.model(STORE_TEX, mannequin())}, store_state("jersey_mannequin"),
      item={"parent": "csm:block/venues/concourse/jersey_mannequin_item"}, tab=CONC)
C.blocks[-1]["models"]["jersey_mannequin_item"] = {
    "parent": "csm:block/venues/concourse/jersey_mannequin",
    "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, -2.5, 0],
                        "scale": [0.42, 0.42, 0.42]},
                "fixed": {"translation": [0, -3, 0], "scale": [0.45, 0.45, 0.45]}}}


# ------------------------------------------------------------------------------------------
# Messages
# ------------------------------------------------------------------------------------------
L = C.add_lang
L("itemGroup.tabvenuesconcourse", ("CSM: Sports & Venues - Concourse",
                                   "CSM: Sport & Veranstaltungsstätten - Promenade",
                                   "CSM: Deportes y Recintos - Explanada",
                                   "CSM: Sport & Arenor - Promenad"))
L("csm.venues.counter.finish", ("Finish: %s", "Ausführung: %s", "Acabado: %s", "Finish: %s"))
for _i, _n in enumerate((("Stainless", "Edelstahl", "Acero inoxidable", "Rostfritt"),
                         ("Navy", "Marineblau", "Azul marino", "Marinblå"),
                         ("Crimson", "Karmesinrot", "Carmesí", "Karmosinröd"),
                         ("Black", "Schwarz", "Negro", "Svart"))):
    L("csm.venues.counter.finish.%d" % _i, _n)
L("csm.venues.menu_board.layout", ("Menu: %s", "Menü: %s", "Menú: %s", "Meny: %s"))
for _i, _n in enumerate((("Hot dogs and grill", "Hotdogs und Grill", "Perritos y parrilla",
                          "Varmkorv och grill"),
                         ("Beer and drinks", "Bier und Getränke", "Cerveza y bebidas",
                          "Öl och drycker"),
                         ("Pizza and snacks", "Pizza und Snacks", "Pizza y aperitivos",
                          "Pizza och snacks"),
                         ("Sweets", "Süßes", "Dulces", "Sötsaker"))):
    L("csm.venues.menu_board.layout.%d" % _i, _n)
L("csm.venues.turnstile.free", ("Turnstile: free, one way", "Drehkreuz: frei, in eine Richtung",
                                "Torniquete: libre, en un sentido",
                                "Vändkors: fritt, åt ett håll"))
L("csm.venues.turnstile.ticketed", ("Turnstile: ticketed (scan a Venue Ticket)",
                                    "Drehkreuz: nur mit Karte (Eintrittskarte scannen)",
                                    "Torniquete: con entrada (escanea una Entrada)",
                                    "Vändkors: biljett krävs (skanna en Biljett)"))
L("csm.venues.turnstile.creative_only",
  ("Only a creative player can change the turnstile's mode",
   "Nur ein Spieler im Kreativmodus kann den Modus des Drehkreuzes ändern",
   "Solo un jugador en modo creativo puede cambiar el modo del torniquete",
   "Endast en spelare i kreativt läge kan ändra vändkorsets läge"))
L("csm.venues.ticket.admit", ("ADMIT ONE", "EINLASS FÜR EINE PERSON", "ADMITE A UNA PERSONA",
                              "GÄLLER EN PERSON"))
L("csm.venues.ticket.valid", ("Not yet scanned", "Noch nicht gescannt", "Sin escanear",
                              "Inte skannad än"))
L("csm.venues.ticket.scanned", ("Scanned: stub only", "Gescannt: nur noch Abschnitt",
                                "Escaneada: solo el resguardo", "Skannad: bara stubben"))
L("csm.venues.ticket.used", ("This ticket has already been scanned",
                             "Diese Karte wurde schon gescannt",
                             "Esta entrada ya se ha escaneado",
                             "Den här biljetten är redan skannad"))
L("csm.venues.ticket.none", ("Scan a Venue Ticket", "Eine Eintrittskarte scannen",
                             "Escanea una Entrada", "Skanna en Biljett"))
L("csm.venues.ticket.no_gate", ("No ticketed turnstile waiting within two blocks",
                                "Kein wartendes Karten-Drehkreuz innerhalb von zwei Blöcken",
                                "Ningún torniquete con entrada esperando a dos bloques",
                                "Inget väntande biljettvändkors inom två block"))
L("csm.venues.ticket.accepted", ("Ticket accepted: go through", "Karte angenommen: bitte eintreten",
                                 "Entrada aceptada: adelante", "Biljett godkänd: gå in"))
L("csm.venues.store.colour", ("Team colour: %s", "Teamfarbe: %s", "Color del equipo: %s",
                              "Lagfärg: %s"))
L("csm.venues.store.trim", ("Trim: %s", "Besatz: %s", "Ribete: %s", "Kant: %s"))


# ------------------------------------------------------------------------------------------
# Main
# ------------------------------------------------------------------------------------------
def main():
    rc = C.main()
    if rc == 0 and "--fragments" not in sys.argv and "--check" not in sys.argv:
        vc.check_json([os.path.join(C.assets, rel) for rel in
                       [os.path.join(dp, f) for dp, _, fs in os.walk(C.assets) for f in fs]])
    return rc


if __name__ == "__main__":
    sys.exit(main())
