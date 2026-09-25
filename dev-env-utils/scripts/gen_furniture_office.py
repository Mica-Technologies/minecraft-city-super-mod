#!/usr/bin/env python3
"""
gen_furniture_office.py -- the Commercial & Office tab in the Furniture & Novelties module: office
desks that join into long desks, their drawer pedestals and the L-desk corner that turns a run,
the reception desk, filing cabinet, office shelving of binders and the conference table, in three
laminates (white, a light grey on a dark frame, walnut); cubicle panels in two heights and three
fabrics; the task, guest, conference and gaming chairs and the waiting-room bench in the living
room's fabrics; the whiteboard, chalkboard, cork notice board and pull-down projector screen; the
school desk with its chair, the teacher's desk and a row of lockers; the things that sit on a
desk (a desktop computer, a computer tower, a retro computer, a laptop, a desk phone, a fax, a
pen holder, a paper tray, a desk lamp); the copier that copies written books; and a streamer's
green screen, ring light and studio camera.

Borrows gen_furniture_residential.py's element helpers, woods, fabrics, output and the bookcase's
geometry (the office shelving is the living room's bookcase with binders on its shelves), the
kitchen's run and corner rules, bar pulls and the cut of a two-block piece, the bedroom's cut
into halves and item displays, and the appliances' surface rests, finishes and lamps (all
imported, not copied), and writes, under modules/furnishings/src/main/resources/assets/csm:

  * textures/blocks/furniture/office/*.png  the grey laminate and its edge band, white powder
    coat, binders, the whiteboard, chalkboard and cork, mesh and vinyl, screens (off and on),
    keyboards and keypads, beige and grey plastics, locker steel and vents, chroma green, the
    projector screen, paper
  * models/block/furniture/office/base/*.json  the geometry
  * models/block/furniture/office/<registry>_<part>.json  a finish's copy of each part a
    multipart blockstate picks
  * blockstates/<registry>.json, and models/item/<registry>.json where the state has no
    inventory variant
  * the tile and tab names in all four languages, and the copier's hint, by key

Every model faces north with its back at +Z, as the Residential tab's do: a desk's user sits at
its front, a board hangs on the wall behind it. Real-world scale, 1 block = 1 m: desks and the
conference table at 0.75 m (so the things on a desk rest on it as on a dining table), the
reception desk's transaction counter at a block, a filing cabinet 1.3 m, lockers 1.8 m; the
things on a desk at about one and a quarter times real size, as the kitchen's are.

What joins (the Java classes compute it as actual state; nothing is stored):

  * office desks and pedestals of one laminate join left and right into one desktop, a T-leg
    only where the run stops; the L-desk corner turns the run as the kitchen corner turns a
    countertop, a corner leg under the L;
  * reception desks join into one counter, side panels only at the ends;
  * conference tables join in any direction like the dining table, legs at the outer corners;
  * the office shelving joins and stacks like the bookcase;
  * cubicle panels join in any plan: an arm towards each panel or wall, a post only at a
    corner, tee or cross, and a run's last block carried on to the edge with an end post there;
    a full panel with another on it leaves off its top cap;
  * whiteboards, chalkboards and cork boards join into one long board, the frame's ends (and
    the markers or chalk on the tray) only where it stops;
  * the waiting-room bench joins into a row of seats on one beam, legs and arms at its ends;
  * lockers join into a row, end panels only at its ends.

Usage:
    python gen_furniture_office.py              # write everything
    python gen_furniture_office.py --check      # fail if the tree has drifted
    python gen_furniture_office.py --fragments  # print the tab registration lines

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
import gen_furniture_bedroom as B  # noqa: E402
import gen_furniture_appliances as A  # noqa: E402

ASSETS = R.ASSETS
SUB = "furniture/office"
MODEL = "csm:%s/" % SUB
BASE = MODEL + "base/"
T = R.T  # the Residential tab's textures


def OT(name):
    """One of this tab's own textures."""
    return "csm:blocks/%s/%s" % (SUB, name)


ROT = R.ROT
el, board, leg, octagon = R.el, R.board, R.leg, R.octagon
mirror_x, swap_xz, turn = R.mirror_x, R.swap_xz, R.turn
ALL = R.ALL
ALL_UV = A.ALL_UV
SIDES = ("north", "south", "east", "west")
NO_DOWN = SIDES + ("up",)
NO_UP = SIDES + ("down",)
NO_BACK = ("north", "east", "west", "up", "down")
shift, clip = B.shift, B.clip


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


BINDER_COLOURS = [(30, 30, 34), (38, 58, 110), (232, 232, 228), (150, 34, 38), (110, 114, 120),
                  (40, 96, 64), (30, 30, 34), (38, 58, 110)]


def binders(seed, size=32):
    """Ring binders on a shelf: spines two to three texels wide, each with a paper label
    window near its head and a finger hole below it. v runs down from the top of the tallest;
    a shelf's binders use the top fourteen rows."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    x = 0
    while x < size:
        w = rng.choice((2, 3, 3, 4))
        c = rng.choice(BINDER_COLOURS)
        label = rng.random() < 0.85
        for xx in range(x, min(size, x + w)):
            for y in range(size):
                col = R.shade(c, 1.0 + rng.uniform(-0.03, 0.03))
                inner = x < xx < x + w - 1 or w == 2
                if y < 1:
                    col = R.shade(c, 0.7)
                elif label and 3 <= y <= 8 and inner:
                    col = (236, 232, 220)
                    if y in (5, 6) and xx % 2 == 0:
                        col = (70, 70, 80)
                elif 10 <= y <= 11 and xx == x + w // 2:
                    col = R.shade(c, 0.45)
                if xx == x:
                    col = R.shade(col, 0.78)
                px[xx, y] = col + (255,)
        x += w
    return img


def binder_tops(seed, size=16):
    """Binders from above: a cover's edge each side of cream paper, a darker spine at the back."""
    rng = random.Random(seed)
    img = blank(size)
    px = img.load()
    x = 0
    while x < size:
        w = rng.choice((2, 2, 3))
        c = rng.choice(BINDER_COLOURS)
        for xx in range(x, min(size, x + w)):
            for y in range(size):
                if y < 2 or xx == x:
                    col = c
                else:
                    col = R.shade((228, 226, 216), 1.0 + rng.uniform(-0.04, 0.03))
                px[xx, y] = R.clamp(col) + (255,)
        x += w
    return img


def whiteboard(size=16):
    """A whiteboard's glossy white with a soft diagonal sheen."""
    rng = random.Random(611)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = 1.0 + 0.015 * math.sin((x + y) * 0.5) + rng.uniform(-0.006, 0.006)
            c = (244, 246, 247)
            if (x - y) % 16 in (3, 4):
                k += 0.01
            px[x, y] = R.shade(c, k) + (255,)
    return img


def chalkboard(size=32):
    """Green slate with chalk dust wiped across it in broad strokes, and a few words' ghost."""
    rng = random.Random(612)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = 1.0 + rng.uniform(-0.04, 0.04)
            wipe = 0.08 * max(0.0, math.sin(x * 0.35 + y * 0.12) ** 8)
            c = R.shade((46, 72, 58), k)
            c = R.clamp(tuple(c[i] + wipe * (200 - c[i]) for i in range(3)))
            px[x, y] = c + (255,)
    for row in (9, 15):
        x = 4
        while x < 26:
            ln = rng.randint(2, 5)
            for i in range(ln):
                if rng.random() < 0.75:
                    px[x + i, row] = (170, 186, 176, 255)
            x += ln + 2
    return img


def cork_notes(size=32):
    """Cork board: a warm speckled brown, with notes pinned to it -- a yellow square, a lined
    white sheet, a blue card -- each held by a coloured pin."""
    rng = random.Random(613)
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            c = rng.choice(((176, 128, 80), (160, 112, 68), (190, 142, 92), (150, 104, 62)))
            px[x, y] = R.shade(c, 1.0 + rng.uniform(-0.05, 0.05)) + (255,)
    notes = [((3, 4, 9, 10), (238, 214, 96), False, (200, 40, 40)),
             ((14, 2, 23, 14), (242, 242, 236), True, (40, 90, 190)),
             ((21, 18, 29, 25), (150, 196, 230), False, (40, 160, 70)),
             ((5, 17, 13, 27), (242, 242, 236), True, (220, 180, 40))]
    for (x0, y0, x1, y1), col, lined, pin in notes:
        for y in range(y0, y1):
            for x in range(x0, x1):
                c = col
                if lined and y > y0 + 1 and (y - y0) % 2 == 0 and x0 < x < x1 - 1:
                    c = (150, 160, 180)
                if x == x1 - 1 or y == y1 - 1:
                    c = R.shade(col, 0.86)
                px[x, y] = c + (255,)
        cx = (x0 + x1) // 2
        px[cx, y0 + 1] = pin + (255,)
        px[cx, y0] = R.shade(pin, 1.3) + (255,)
    return img


def mesh(size=32):
    """A task chair's mesh: a fine dark weave with a little light showing through it."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            c = (36, 36, 40) if (x % 2 == 0 or y % 2 == 0) else (54, 56, 60)
            px[x, y] = c + (255,)
    return img


def screen(kind, on, size=16):
    """A screen: dark glass with a reflection; lit, a desktop (windows on a wallpaper and a
    taskbar), a laptop's teal desktop, or an old CRT's green text."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if not on:
                col = (20, 22, 26) if kind != "crt" else (46, 54, 50)
                if 0 <= (x - y) % 16 < 2 and x > y - 12:
                    col = R.shade(col, 1.9)
            elif kind == "crt":
                col = (12, 20, 14)
                if y % 3 == 1 and 2 <= x < 3 + (7 * y + 5) % 11 and y < 14:
                    col = (80, 230, 110)
            else:
                top = (60, 120, 200) if kind == "desktop" else (40, 150, 150)
                bot = (20, 50, 120) if kind == "desktop" else (20, 80, 90)
                t = y / (size - 1)
                col = tuple(int(top[i] * (1 - t) + bot[i] * t) for i in range(3))
                if y == size - 1:
                    col = (30, 30, 36)
                win = (3, 3, 10, 9) if kind == "desktop" else (5, 4, 12, 11)
                if win[0] <= x < win[2] and win[1] <= y < win[3]:
                    col = (60, 110, 190) if y == win[1] else (236, 238, 240)
                    if y > win[1] + 1 and (y - win[1]) % 2 == 0 and x < win[2] - 2:
                        col = (170, 176, 186)
                if kind == "desktop" and 8 <= x < 14 and 6 <= y < 12:
                    col = (60, 110, 190) if y == 6 else (250, 250, 250)
            px[x, y] = tuple(col) + (255,)
    return img


def keys(base, key, size=16):
    """A keyboard from above: rows of keys in a frame."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            col = base
            if 1 <= y <= 13 and 1 <= x <= 14 and (y - 1) % 3 != 2 and (x - 1) % 2 == 0:
                col = key
            if y in (13, 14) and 4 <= x <= 11:
                col = key
            px[x, y] = col + (255,)
    return img


def keypad(size=16):
    """A desk phone's face: a small grey display over a 3 x 4 grid of keys, speed-dial keys to
    the side."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            col = (30, 30, 34)
            if 1 <= y <= 3 and 2 <= x <= 12:
                col = (150, 170, 150)
            elif 5 <= y <= 14 and 2 <= x <= 10 and (y - 5) % 3 != 2 and (x - 2) % 3 != 2:
                col = (200, 202, 206)
            elif 5 <= y <= 14 and x == 13 and y % 2 == 1:
                col = (120, 124, 130)
            px[x, y] = col + (255,)
    return img


def tower_front(size=16):
    """A tower PC's face: a drive bay, a power button with a blue ring, vents below."""
    img = noisy((30, 30, 34), 621, size, 0.03)
    px = img.load()
    for x in range(3, 13):
        px[x, 2] = (60, 60, 66, 255)
        px[x, 3] = (18, 18, 20, 255)
    for dx, dy in ((0, -1), (1, 0), (0, 1), (-1, 0)):
        px[8 + dx, 6 + dy] = (70, 160, 255, 255)
    for y in range(9, 15, 2):
        for x in range(3, 13):
            px[x, y] = (14, 14, 16, 255)
    return img


def retro_front(size=16):
    """An old desktop case's face: beige plastic, two floppy slots, a vent and a red LED."""
    img = noisy((214, 206, 184), 622, size, 0.02)
    px = img.load()
    for x in range(2, 8):
        px[x, 5] = (60, 58, 52, 255)
        px[x, 10] = (60, 58, 52, 255)
    for y in range(4, 12):
        for x in range(10, 14):
            if y % 2 == 0:
                px[x, y] = (150, 144, 128, 255)
    px[13, 13] = (220, 50, 40, 255)
    return img


def label_card(size=16):
    """A white card in a holder with a line of print on it: a drawer's or a locker's label."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            col = (236, 236, 230)
            if x in (0, size - 1) or y in (0, size - 1):
                col = (150, 152, 156)
            elif y in (7, 8) and 3 <= x <= 12 and x % 3 != 0:
                col = (60, 60, 70)
            px[x, y] = col + (255,)
    return img


def paper_stack(size=16):
    """The edges of a stack of paper seen from the side."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        k = 1.0 if y % 2 == 0 else 0.9
        for x in range(size):
            px[x, y] = R.shade((242, 242, 236), k) + (255,)
    return img


def vents(size=16):
    """A locker door's vent louvres: dark slots between bars of steel."""
    img = blank(size)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if y % 3 == 0:
                col = (150, 154, 160)
            elif y % 3 == 1:
                col = (28, 28, 32)
            else:
                col = (60, 62, 68)
            px[x, y] = col + (255,)
    return img


def powder(base, seed, size=16):
    """Powder-coated steel: flat colour with a fine orange-peel texture."""
    return noisy(base, seed, size, 0.025)


def lens_glass(size=16):
    """A camera lens seen from the front: dark glass, a coated blue-violet sheen, a rim."""
    def draw(r, a):
        if r > 0.88:
            return (40, 40, 44)
        k = 1.0 - 0.5 * r
        col = R.shade((40, 44, 90), 0.6 + k)
        if 0.35 < r < 0.5 and 0.5 < a < 1.6:
            col = (150, 140, 220)
        return col
    img = A.disc(size, draw)
    px = img.load()
    for y in range(size):
        for x in range(size):
            if px[x, y][3] == 0:
                px[x, y] = (26, 26, 30, 255)
    return img


TEXTURES = {
    "laminate_grey": lambda: R.laminate((200, 202, 204), 601),
    "laminate_grey_edge": lambda: R.flat((78, 80, 84), 602, grain=3),
    "metal_white": lambda: R.metal((228, 230, 230), 603),
    "binders": lambda: binders(604),
    "binders_b": lambda: binders(605),
    "binder_tops": lambda: binder_tops(606),
    "whiteboard": whiteboard,
    "chalkboard": chalkboard,
    "cork_notes": cork_notes,
    "mesh": mesh,
    "vinyl_black": lambda: noisy((36, 36, 40), 607, 16, 0.04),
    "screen_off": lambda: screen("desktop", False),
    "screen_desktop": lambda: screen("desktop", True),
    "screen_laptop": lambda: screen("laptop", True),
    "crt_off": lambda: screen("crt", False),
    "crt_on": lambda: screen("crt", True),
    "keys_black": lambda: keys((24, 24, 28), (56, 56, 62)),
    "keys_beige": lambda: keys((196, 188, 166), (226, 220, 202)),
    "keys_silver": lambda: keys((170, 174, 178), (40, 40, 44)),
    "keypad": keypad,
    "tower_front": tower_front,
    "retro_front": retro_front,
    "plastic_beige": lambda: noisy((214, 206, 184), 608),
    "plastic_light_grey": lambda: noisy((208, 210, 212), 609, 16, 0.02),
    "plastic_dark_grey": lambda: noisy((86, 88, 92), 610, 16, 0.02),
    "plastic_blue": lambda: noisy((58, 98, 168), 614, 16, 0.025),
    "plastic_red": lambda: noisy((172, 48, 42), 615, 16, 0.025),
    "silver": lambda: K.brushed((196, 200, 204), 616),
    "label_card": label_card,
    "paper": lambda: R.flat((244, 244, 240), 617, grain=2),
    "paper_stack": paper_stack,
    "locker_blue": lambda: powder((62, 94, 148), 618),
    "locker_grey": lambda: powder((140, 146, 152), 619),
    "locker_red": lambda: powder((156, 44, 40), 620),
    "locker_vents": vents,
    "chroma_green": lambda: noisy((40, 176, 72), 623, 16, 0.02),
    "projector_screen": lambda: R.flat((242, 242, 238), 624, grain=2),
    "case_black": lambda: noisy((40, 40, 44), 625),
    "lens_glass": lens_glass,
    "chalk": lambda: R.flat((240, 240, 232), 626, grain=3),
    "felt_grey": lambda: R.fabric((120, 122, 126), 627),
    "panel_trim": lambda: R.metal((150, 152, 156), 628),
    "lamp_red": lambda: A.lamp((230, 40, 40), 8),
}

# ------------------------------------------------------------------------------------------
# Finishes
# ------------------------------------------------------------------------------------------
LAMINATES = [
    # (id, textures, en, de, es, sv)
    ("white", {"wood": T("white"), "wood_v": T("white_v"), "edge": T("white_edge"),
               "frame": OT("metal_white")},
     "White", "Weiß", "blanco", "vit"),
    ("grey", {"wood": OT("laminate_grey"), "wood_v": OT("laminate_grey"),
              "edge": OT("laminate_grey_edge"), "frame": T("metal_black")},
     "Grey", "Grau", "gris", "grå"),
    ("walnut", {"wood": T("walnut"), "wood_v": T("walnut_v"), "edge": T("walnut_edge"),
                "frame": T("metal_black")},
     "Walnut", "Nussbaum", "nogal", "valnöt"),
]
SEAT_FABRICS = [f for f in R.FABRICS if f[0] in ("charcoal", "navy", "red")]
PANEL_FABRICS = [f for f in R.FABRICS if f[0] in ("charcoal", "navy", "oatmeal")]
BINDERS = {"books": OT("binders"), "books_b": OT("binders_b"), "book_tops": OT("binder_tops")}


def fin(fid, tex, *names):
    return (fid, dict(tex)) + names


BLACK = fin("black", {"plastic": T("appliance_black"), "keys": OT("keys_black")},
            "Black", "Schwarz", "negro", "svart")
WHITE_LAMP = fin("white", {"plastic": T("appliance_white"), "keys": OT("keys_black")},
                 "White", "Weiß", "blanco", "vit")

DEFAULT_TEX = dict(B.DEFAULT_TEX)
DEFAULT_TEX.update({k: v for k, v in A.DEFAULT_TEX.items() if k not in DEFAULT_TEX})
DEFAULT_TEX.update({
    "wood": OT("laminate_grey"), "wood_v": OT("laminate_grey"), "edge": OT("laminate_grey_edge"),
    "frame": T("metal_black"), "trim": OT("panel_trim"), "mesh": OT("mesh"),
    "vinyl": OT("vinyl_black"), "plastic": T("appliance_black"), "keys": OT("keys_black"),
    "glow": OT("screen_off"), "board": OT("whiteboard"), "steel": OT("locker_grey"),
    "vents": OT("locker_vents"), "label": OT("label_card"), "paper": OT("paper"),
    "stack": OT("paper_stack"), "shelf": OT("laminate_grey"), "screen": OT("projector_screen"),
    "case": OT("case_black"), "chroma": OT("chroma_green"), "lens": T("lens_on"),
    "glass": OT("lens_glass"), "chalk": OT("chalk"), "felt": OT("felt_grey"),
    "keypad": OT("keypad"), "face": OT("tower_front"), "tally": OT("lamp_red"),
    "pen_a": T("ceramic_blue"), "pen_b": T("ceramic_red"), "pen_c": T("appliance_black"),
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


def bar(cx, y, zf, w=3.0, tex="frame"):
    """A bar pull in the frame's finish, w wide, centred on cx, on a front whose face is at zf."""
    return [el([cx - w / 2, y, zf - 0.5], [cx + w / 2, y + 0.4, zf], tex,
               ("north", "east", "west", "up", "down"))]


def bar_south(cx, y, zf, w=3.0, tex="frame"):
    """A bar pull on a front facing south, whose face is at zf."""
    return [el([cx - w / 2, y, zf], [cx + w / 2, y + 0.4, zf + 0.5], tex,
               ("south", "east", "west", "up", "down"))]


def front(x0, x1, y0, y1, zf, grain="h"):
    """A slab drawer front, a quarter pixel proud of the carcass behind it."""
    return [board([x0, y0, zf], [x1, y1, zf + 0.5], grain=grain, faces=NO_BACK)]


# ------------------------------------------------------------------------------------------
# Desks: the top at 0.75 m, 0.75 px thick, running the full depth so that a corner can turn it
# ------------------------------------------------------------------------------------------
DESK_TOP = [board([0, 11.25, 0.5], [16, 12, 16])]
DESK_BODY = (DESK_TOP
             + [board([0, 3.5, 14.75], [16, 11.25, 15.25], grain="v",
                      faces=("north", "south", "down")),
                el([0, 10.5, 1.25], [16, 11.25, 2], "frame", ("north", "south", "down"))])
# A T-leg where the run stops: a column on a foot running front to back, an arm under the top.
DESK_END = [el([0.75, 0.75, 7.25], [2.25, 10.5, 8.75], "frame", SIDES),
            el([0.75, 0, 1.5], [2.25, 0.75, 14.5], "frame", NO_UP + ("up",)),
            el([0.75, 10.5, 1.25], [2.25, 11.25, 15], "frame", NO_UP),
            el([0, 11.25, 0.5], [0.01, 12, 16], "edge", ("west",))]
# The pedestal: three drawers under the top at the right, a box drawer over a file drawer.
PZ = 1.25
PEDESTAL = ([board([9.25, 0.5, 1.75], [9.75, 11.25, 14.75], grain="v", faces=("west", "north")),
             board([15.25, 0.5, 1.75], [15.75, 11.25, 14.75], grain="v",
                   faces=("east", "north")),
             el([9.5, 0, 2.5], [15.5, 0.5, 14], "edge", ("north",))]
            + front(9.75, 15.25, 8.25, 10.75, PZ) + bar(12.5, 9.3, PZ)
            + front(9.75, 15.25, 5.25, 8, PZ) + bar(12.5, 6.4, PZ)
            + front(9.75, 15.25, 0.75, 5, PZ) + bar(12.5, 3.6, PZ))
PEDESTAL_BODY = DESK_BODY + PEDESTAL

# The L-desk corner, its backs along +Z and +X like the kitchen corner: with both sides joined
# the top is whole, a modesty panel runs along both backs, and a leg stands in the back corner.
DESK_BLIND = (DESK_TOP
              + [board([0, 3.5, 14.75], [14.75, 11.25, 15.25], grain="v",
                       faces=("north", "down")),
                 board([14.75, 3.5, 0], [15.25, 11.25, 14.75], grain="v",
                       faces=("west", "down")),
                 el([13, 0, 13], [14.5, 11.25, 14.5], "frame", SIDES),
                 el([12.5, 0, 12.5], [15, 0.5, 15], "frame", NO_DOWN)])
CORNER_PARTS = {"north": DESK_BODY, "west": swap_xz(DESK_BODY), "blind": DESK_BLIND,
                "end_right": mirror_x(DESK_END), "end_left": DESK_END,
                "end_back": swap_xz(mirror_x(DESK_END))}

# --- reception desk: a transaction counter at a block over the front panel, the work surface
# behind it at desk height, a drawer bank facing the receptionist ------------------------------
RECEPTION_BODY = ([board([0, 0, 1.5], [16, 15.25, 2.25], grain="v", faces=("north", "south")),
                   el([0, 8.5, 1.4], [16, 9.75, 1.5], "frame", ("north",)),
                   board([0, 15.25, 0], [16, 16, 5.5]),
                   board([0, 12, 5], [16, 15.25, 5.5], grain="v", faces=("south",)),
                   board([0, 11.25, 2.25], [16, 12, 14.5]),
                   board([9.25, 0.5, 2.25], [9.75, 11.25, 14], grain="v", faces=("west",))]
                  + [board([9.75, 0, 13.75], [15.5, 0.5, 14], faces=("south",))]
                  + [board([9.75, y0, 13.5], [15.5, y1, 14], faces=("south", "east", "west",
                                                                     "up", "down"))
                     for y0, y1 in ((8, 10.75), (4.5, 7.75), (0.75, 4.25))]
                  + [s for y in (9.2, 5.9, 2.3) for s in bar_south(12.6, y, 14)])
RECEPTION_END = [board([0, 0, 1.5], [0.75, 15.25, 5.5], grain="v"),
                 board([0, 0, 5.5], [0.75, 11.25, 14.5], grain="v"),
                 el([0, 15.25, 0], [0.01, 16, 5.5], "edge", ("west",))]

# --- filing cabinet: 1.3 m, four drawers with label holders and recessed pulls ----------------
FILING = ([board([1, 0.5, 3], [1.75, 20.5, 16], grain="v", faces=("west", "north", "south")),
           board([14.25, 0.5, 3], [15, 20.5, 16], grain="v", faces=("east", "north", "south")),
           board([1.75, 0.5, 15.25], [14.25, 20.5, 16], grain="v", faces=("south",)),
           board([0.75, 20.5, 2.5], [15.25, 21, 16]),
           el([1.25, 0, 3.5], [14.75, 0.5, 15.5], "edge", SIDES)])
for _y0, _y1 in ((0.75, 5.5), (5.75, 10.5), (10.75, 15.5), (15.75, 20.25)):
    FILING += front(1.75, 14.25, _y0, _y1, 2.5)
    FILING += [ALL_UV(el([6.5, _y1 - 1.75, 2.35], [9.5, _y1 - 0.75, 2.5], "label", ("north",)),
                      ["north"]),
               el([6, _y0 + 1, 2.2], [10, _y0 + 1.5, 2.5], "frame",
                  ("north", "east", "west", "up", "down"))]

# --- conference table: a thick top, a steel rail under it, square steel legs -----------------
CT_TOP = [board([0, 11, 0], [16, 12, 16])]
CT_APRON = [el([2.5, 10, 2], [13.5, 11, 2.75], "frame", ("north", "south", "down"))]
CT_LEG = [el([1.5, 0, 1.5], [3.5, 11, 3.5], "frame", NO_UP),
          el([1.25, 0, 1.25], [3.75, 0.25, 3.75], "frame", NO_DOWN)]
CT_APRON_A = [el([0, 10, 2], [1.5, 11, 2.75], "frame", ("north", "south", "down"))]
CT_APRON_B = [el([2, 10, 0], [2.75, 11, 1.5], "frame", ("east", "west", "down"))]

# --- teacher's desk: a double-pedestal desk, drawers facing the teacher, a modesty panel ------
TEACHER = ([board([0, 11.25, 0.75], [16, 12, 16])]
           + [board([x0, 0.5, 1.75], [x0 + 0.5, 11.25, 15.25], grain="v")
              for x0 in (0.5, 4.75, 10.75, 15)]
           + [el([x0, 0, 2.25], [x1, 0.5, 14.75], "edge", ("north",))
              for x0, x1 in ((0.75, 5), (11, 15.25))]
           + [board([5.25, 3, 14.5], [10.75, 11.25, 15], grain="v", faces=("north", "south",
                                                                            "down"))]
           + front(1, 4.75, 6, 10.75, 1.25) + bar(2.9, 8.5, 1.25, w=2)
           + front(1, 4.75, 0.75, 5.75, 1.25) + bar(2.9, 3.4, 1.25, w=2)
           + front(11.25, 15, 6, 10.75, 1.25) + bar(13.1, 8.5, 1.25, w=2)
           + front(11.25, 15, 0.75, 5.75, 1.25) + bar(13.1, 3.4, 1.25, w=2)
           + front(5.25, 10.75, 9.75, 11.25, 1.25) + bar(8, 10.3, 1.25, w=2)
           # a stack of exercise books and an apple for the teacher
           + [el([2, 12, 9], [6, 13, 13], "paper", NO_DOWN, {"north": "stack", "south": "stack",
                                                            "east": "stack", "west": "stack"}),
              el([12, 12, 4], [13.5, 13.5, 5.5], "pen_b", NO_DOWN)])

# ------------------------------------------------------------------------------------------
# Cubicle panels: a fabric panel 1.5 px thick in a steel trim, full (16) or half (8) height
# ------------------------------------------------------------------------------------------


def panel_parts(h):
    """The parts of a panel h high, each drawn for the north side (turned for the others)."""
    arm = [el([7.25, 0.75, 0], [8.75, h - 0.75, 8], "fabric", ("east", "west")),
           el([7, 0, 0], [9, 0.75, 8], "trim", ("east", "west", "up"))]
    cap = [el([7, h - 0.75, 0], [9, h, 8], "trim", ("east", "west", "up", "down"))]
    fill = [el([7.25, h - 0.75, 0], [8.75, h, 8], "fabric", ("east", "west"))]
    end = [el([6.75, 0, 0], [9.25, h, 1.25], "trim", NO_DOWN)]
    post = [el([6.75, 0, 6.75], [9.25, h, 9.25], "trim", NO_DOWN)]
    sy = h - 4.5
    shelf = [el([1, sy, 1.5], [15, sy + 0.75, 7], "shelf"),
             el([1, sy + 0.75, 1.5], [15, sy + 1.75, 2], "shelf", NO_DOWN),
             el([2, sy - 1.5, 6], [2.5, sy, 7], "trim", NO_UP),
             el([13.5, sy - 1.5, 6], [14, sy, 7], "trim", NO_UP)]
    return {"arm": arm, "cap": cap, "fill": fill, "end": end, "post": post, "shelf": shelf}


PANEL_FULL = panel_parts(16)
PANEL_HALF = panel_parts(8)
SIDE_VALUES = "panel|end"


def panel_rules():
    rules = []
    for side, r in ROT.items():
        rules += [("arm", {side: SIDE_VALUES}, r),
                  ("cap", {side: SIDE_VALUES, "up": "false"}, r),
                  ("fill", {side: SIDE_VALUES, "up": "true"}, r),
                  ("end", {side: "end"}, r),
                  ("shelf", {"shelf": side}, r)]
    rules.append(("post", {"OR": [{"north": "panel", "east": "panel"},
                                  {"east": "panel", "south": "panel"},
                                  {"south": "panel", "west": "panel"},
                                  {"west": "panel", "north": "panel"}]}, 0))
    return rules


def panel_item(parts):
    lone = parts["arm"] + parts["cap"] + parts["end"]
    return turn(lone, 90) + turn(lone, 270)


# ------------------------------------------------------------------------------------------
# Seating: the living room's fabrics, black steel and plastic
# ------------------------------------------------------------------------------------------
def star_base(tex="metal_black", column=5.5):
    """Four spokes on casters and a gas-lift column: the office chairs' base."""
    return ([el([2.5, 1, 7.25], [13.5, 2, 8.75], tex),
             el([7.25, 1, 2.5], [8.75, 2, 13.5], tex)]
            + [el([x, 0, z], [x + 1.5, 1, z + 1.5], "rubber")
               for x, z in ((2.25, 7.25), (12.25, 7.25), (7.25, 2.25), (7.25, 12.25))]
            + [el([7.4, 2, 7.4], [8.6, column, 8.6], "metal", SIDES)])


def arms(post, pad, tex="metal_black"):
    """A pair of arms, (post box) and (pad box) given for the left one."""
    one = [el(post[0], post[1], tex, SIDES), el(pad[0], pad[1], tex)]
    return one + mirror_x(one)


TASK_CHAIR = (star_base()
              + [el([5, 5.5, 5], [11, 6.25, 11], "metal_black"),
                 el([3, 6.25, 3.5], [13, 8, 12.5], "fabric"),
                 el([7.25, 6.25, 12.5], [8.75, 10, 13.75], "metal_black"),
                 # the mesh back in its frame, and a lumbar pad in the fabric
                 el([4.25, 9.5, 13], [11.75, 16.5, 13.25], "mesh", ("north", "south")),
                 el([3.5, 9, 12.75], [4.25, 17, 13.5], "metal_black"),
                 el([11.75, 9, 12.75], [12.5, 17, 13.5], "metal_black"),
                 el([4.25, 16.25, 12.75], [11.75, 17, 13.5], "metal_black"),
                 el([4.25, 9, 12.75], [11.75, 9.5, 13.5], "metal_black"),
                 el([5, 10.5, 12.5], [11, 12, 13], "fabric")]
              + arms(([2.25, 6.5, 7.25], [3, 10, 8.5]), ([1.75, 10, 5], [3.25, 10.75, 10.5])))
CONFERENCE_CHAIR = (star_base("metal")
                    + [el([5, 5.5, 5], [11, 6.25, 11], "metal_black"),
                       el([3, 6.25, 3.25], [13, 8.25, 12.5], "fabric"),
                       el([3.25, 8.25, 12.5], [12.75, 19, 14.25], "fabric", ALL,
                          {"south": "fabric_dark"}),
                       el([4, 17.5, 12.25], [12, 18.75, 12.5], "fabric_dark", ("north",))]
                    + [el([3.3, y, 12.45], [12.7, y + 0.25, 12.5], "fabric_dark", ("north",))
                       for y in (11, 13.5, 16)]
                    + arms(([2.25, 6.5, 4], [2.75, 10, 4.5]), ([2.25, 10, 4], [2.75, 10.5, 12]),
                           "metal")
                    + [el([2.25, 7, 11.5], [2.75, 10, 12], "metal", SIDES),
                       el([13.25, 7, 11.5], [13.75, 10, 12], "metal", SIDES)])
_SLED = [el([2.5, 0, 3], [3, 0.5, 13], "metal"),
         el([2.5, 0.5, 3], [3, 10, 3.5], "metal", SIDES),
         el([2.5, 10, 3], [3, 10.5, 12.5], "metal"),
         el([2.5, 0.5, 12.5], [3, 15, 13], "metal", SIDES + ("up",))]
GUEST_CHAIR = (_SLED + mirror_x(_SLED)
               + [el([3, 6, 3.75], [13, 6.5, 12.5], "metal_black"),
                  el([3.25, 6.5, 3.5], [12.75, 8, 12.5], "fabric"),
                  el([3.5, 8.5, 12.75], [12.5, 15, 13.75], "fabric", ALL,
                     {"south": "fabric_dark"})])
GAMING_CHAIR = (star_base()
                + [el([5, 5.5, 5], [11, 6.25, 11], "metal_black"),
                   el([3, 6.25, 3.5], [13, 7.5, 12.5], "vinyl"),
                   el([4.5, 7.5, 4], [11.5, 8, 12], "fabric"),
                   el([3, 7.5, 3.5], [4.5, 9, 12.25], "vinyl", ALL, {"up": "fabric"}),
                   el([11.5, 7.5, 3.5], [13, 9, 12.25], "vinyl", ALL, {"up": "fabric"}),
                   el([3.5, 8, 12.25], [12.5, 21, 13.75], "vinyl"),
                   el([5, 9, 12.2], [11, 17.5, 12.25], "fabric", ("north",)),
                   el([3, 12.5, 11.5], [4.5, 19, 13.75], "vinyl", ALL, {"north": "fabric"}),
                   el([11.5, 12.5, 11.5], [13, 19, 13.75], "vinyl", ALL, {"north": "fabric"}),
                   el([5.5, 18, 11.5], [10.5, 20, 12.25], "vinyl"),
                   el([5.5, 9.5, 11.5], [10.5, 11.5, 12.25], "fabric")]
                + arms(([2, 6.5, 7.5], [2.75, 10.5, 8.5]), ([1.5, 10.5, 5], [3.25, 11.25, 10.5])))

# The waiting-room bench: a seat and back each block on a steel beam; legs and arms at the ends.
BENCH_BODY = [el([0, 3.5, 7], [16, 4.75, 9], "metal", ("north", "south", "up", "down")),
              el([7, 4.75, 5.5], [9, 5.5, 10.5], "metal_black"),
              el([1, 5.5, 3], [15, 7.25, 11.5], "fabric"),
              el([7.25, 5.5, 11], [8.75, 11, 12], "metal_black"),
              el([1, 8, 11.75], [15, 14, 13], "fabric", ALL, {"south": "fabric_dark"})]
BENCH_END = [el([0, 3.5, 7], [0.01, 4.75, 9], "metal", ("west",)),
             el([1.25, 0, 3.5], [2.75, 0.75, 12.5], "metal"),
             el([1.5, 0.75, 7.25], [2.5, 3.5, 8.75], "metal", SIDES),
             el([0.25, 4.75, 4.75], [1, 10, 5.75], "metal", SIDES),
             el([0.25, 4.75, 9.5], [1, 10, 10.5], "metal", SIDES),
             el([0, 10, 4.25], [1.25, 10.75, 11.25], "metal_black")]

# School desk and chair in one: the top in front, a book tray under it, the seat behind.
_SCHOOL_FRAME = [el([3, 0, 1], [3.75, 0.75, 15], "frame"),
                 el([3, 0.75, 1.5], [3.75, 11, 2.25], "frame", SIDES),
                 el([3, 0.75, 13.5], [3.75, 14, 14.25], "frame", SIDES)]
SCHOOL_DESK = (_SCHOOL_FRAME + mirror_x(_SCHOOL_FRAME)
               + [board([2.5, 11, 0.5], [13.5, 11.75, 7]),
                  el([3.75, 9.25, 1.25], [12.25, 9.75, 6.5], "frame"),
                  el([3.75, 9.75, 6.25], [12.25, 11, 6.5], "frame", ("north", "south")),
                  el([12.25, 6, 2.25], [12.75, 6.75, 13.5], "frame"),
                  el([3.75, 6, 8.5], [12.25, 6.75, 9.25], "frame", ("north", "south", "down")),
                  el([3.75, 6.75, 7.75], [12.25, 7.5, 13.5], "plastic"),
                  el([4, 9.5, 14.25], [12, 14, 15], "plastic"),
                  el([3.75, 1.5, 8.5], [12.25, 2, 13], "frame", ("north", "south", "up",
                                                                  "down"))])

# ------------------------------------------------------------------------------------------
# Boards on the wall: a surface in a frame, hung at 0.2 to 0.9 m... of the block's height
# ------------------------------------------------------------------------------------------


def wall_board(tray, frame_tex):
    body = [el([0, 3, 15], [16, 14, 15.75], "board", ("north", "south"), {"south": frame_tex}),
            el([0, 14, 14.75], [16, 14.75, 16], frame_tex, ("north", "up", "down")),
            el([0, 2.25, 14.75], [16, 3, 16], frame_tex, ("north", "up", "down"))]
    end = [el([0, 2.25, 14.75], [0.75, 14.75, 16], frame_tex, ("north", "west", "up", "down"))]
    if tray:
        body += [el([0, 2, 13], [16, 2.25, 16], frame_tex, ("north", "up", "down")),
                 el([0, 2.25, 13], [16, 2.75, 13.5], frame_tex, ("north", "south", "up"))]
        end += [el([0, 2, 13], [0.75, 2.75, 14.75], frame_tex, ("west", "north", "up"))]
    return body, end


WHITEBOARD_BODY, WHITEBOARD_END = wall_board(True, "trim")
CHALKBOARD_BODY, CHALKBOARD_END = wall_board(True, "wood")
CORK_BODY, CORK_END = wall_board(False, "wood")
# What lies on the tray at the right-hand end: markers and an eraser, or chalk and a duster.
MARKERS = [el([10, 2.25, 13.75], [12, 2.75, 14.25], "pen_a", NO_DOWN),
           el([10.5, 2.25, 14.5], [12.5, 2.75, 14.75], "pen_b", NO_DOWN),
           el([12.75, 2.25, 13.6], [14.75, 3.5, 14.75], "felt", NO_DOWN, {"up": "pen_c"})]
CHALK = [el([10, 2.25, 13.75], [11.75, 2.6, 14.1], "chalk", NO_DOWN),
         el([11, 2.25, 14.3], [12.25, 2.6, 14.65], "chalk", NO_DOWN),
         el([12.75, 2.25, 13.6], [14.75, 3.5, 14.75], "felt", NO_DOWN, {"up": "wood"})]

# The projector screen: its case at the top of the block, the screen let down a metre and a half.
PROJ_CASE = [el([0.5, 14, 13.5], [15.5, 16, 16], "case"),
             el([0.25, 13.75, 13.25], [0.75, 16, 16], "case"),
             el([15.25, 13.75, 13.25], [15.75, 16, 16], "case")]
PROJ_CLOSED = PROJ_CASE + [el([7, 13.25, 14], [9, 14, 14.5], "case")]
PROJ_OPEN = (PROJ_CASE
             + [el([1, -11, 14.5], [15, 14, 14.75], "screen", ("north", "south")),
                el([1, 11, 14.45], [15, 14, 14.5], "case", ("north",)),
                el([0.75, -11.75, 14.25], [15.25, -11, 15], "case"),
                el([7, -12.75, 14.5], [9, -11.75, 14.75], "case", ("north", "south", "east",
                                                                   "west", "down"))])

# ------------------------------------------------------------------------------------------
# Lockers: 1.8 m, two doors a block, vents top and bottom, drawn whole and cut in two
# ------------------------------------------------------------------------------------------
LOCKER_BODY = [el([0, 0, 15.5], [16, 29, 16], "steel", ("south",)),
               el([0, 28.25, 7], [16, 29, 16], "steel", ("north", "up", "down")),
               el([0, 0, 7.5], [16, 1.5, 15.5], "vinyl", ("north",)),
               el([7.9, 1.5, 7.25], [8.1, 28.25, 15.5], "vinyl", ("north",))]
for _x0, _x1 in ((0.25, 7.9), (8.1, 15.75)):
    LOCKER_BODY += [el([_x0, 1.5, 7], [_x1, 28.25, 7.5], "steel", NO_BACK),
                    el([_x0 + 1, 24.5, 6.95], [_x1 - 1, 27, 7], "vents", ("north",)),
                    el([_x0 + 1, 2.5, 6.95], [_x1 - 1, 5, 7], "vents", ("north",)),
                    ALL_UV(el([_x0 + 1.5, 21.5, 6.9], [_x0 + 3.5, 22.5, 7], "label",
                              ("north",)), ["north"])]
LOCKER_BODY += [el([6.25, 13, 6.4], [7.25, 17, 7], "frame", NO_BACK),
                el([8.75, 13, 6.4], [9.75, 17, 7], "frame", NO_BACK)]
LOCKER_END = [el([0, 0, 7], [0.25, 29, 16], "steel", ("west", "north", "up", "down"))]

# ------------------------------------------------------------------------------------------
# On the desk: drawn standing on the floor of the block, dropped by SurfaceRest onto a desk
# ------------------------------------------------------------------------------------------
DESKTOP_COMPUTER = [el([6, 0, 8.5], [10, 0.25, 11.5], "plastic"),
                    el([7.5, 0.25, 9.75], [8.5, 3.5, 10.5], "plastic", SIDES),
                    el([2, 3, 9], [14, 10.5, 9.75], "plastic"),
                    ALL_UV(el([2.4, 3.4, 8.95], [13.6, 10.1, 9], "glow", ("north",)), ["north"]),
                    el([3.5, 0, 3], [11.5, 0.5, 6], "plastic", ALL, {"up": "keys"}),
                    el([12.5, 0, 4], [13.5, 0.5, 5.5], "plastic")]
for _s in DESKTOP_COMPUTER[4:5]:
    ALL_UV(_s, ["up"])
COMPUTER_TOWER = [ALL_UV(el([5.5, 0.25, 3], [10.5, 10.5, 13], "plastic", ALL,
                            {"north": "face"}), ["north"])]
COMPUTER_TOWER += [el([x, 0, z], [x + 1, 0.25, z + 1], "rubber", SIDES)
                   for x in (5.75, 9.25) for z in (3.5, 11.5)]
RETRO_COMPUTER = [ALL_UV(el([2.5, 0, 5], [13.5, 2.5, 13.5], "plastic", ALL, {"north": "face"}),
                         ["north"]),
                  el([4, 2.5, 6], [12, 9.5, 12.5], "plastic"),
                  el([5, 3, 12.5], [11, 8.5, 14.5], "plastic", NO_BACK + ("south",)),
                  ALL_UV(el([4.75, 3.25, 5.95], [11.25, 8.75, 6], "glow", ("north",)), ["north"]),
                  ALL_UV(el([3, 0, 1], [13, 0.75, 4.5], "plastic", ALL, {"up": "keys"}), ["up"])]
_LID = el([3.5, 0.5, 9.25], [12.5, 7, 9.75], "shell", ALL, {"north": "glow"},
          rot=("x", 22.5, [8, 0.5, 9.5]))
ALL_UV(_LID, ["north"])
LAPTOP = [ALL_UV(el([3.5, 0, 3], [12.5, 0.5, 9.75], "shell", ALL, {"up": "keys"}), ["up"]),
          _LID]
DESK_PHONE = [el([4.5, 0, 5], [11.5, 1.5, 11], "plastic"),
              ALL_UV(el([7, 1.5, 5.5], [11, 1.75, 10.5], "plastic", NO_DOWN,
                        {"up": "keypad"}), ["up"]),
              el([4.5, 1.5, 5], [6.75, 2.25, 11], "plastic", NO_DOWN),
              el([4.6, 2.25, 4.75], [6.6, 3.25, 11.25], "plastic")]
FAX_MACHINE = [el([3, 0, 4], [13, 3, 12], "shell"),
               ALL_UV(el([8.5, 3, 4.5], [12.5, 3.25, 7.5], "control", ("up", "north", "east",
                                                                       "west")), ["up"]),
               el([3.5, 3, 4.5], [5.75, 4, 11.5], "plastic"),
               el([6.5, 3, 9], [12, 6.5, 9.25], "paper", ("north", "south", "east", "west",
                                                          "up"),
                  rot=("x", -22.5, [9, 3, 9])),
               el([6, 1.5, 3.5], [12.5, 1.75, 4], "paper", NO_DOWN)]
PEN_HOLDER = (octagon(8, 8, 1.75, 0, 3.5, "mesh", caps=("down",))
              + [el([7.25, 3, 7.4], [7.75, 5.75, 7.9], "pen_a", NO_DOWN),
                 el([8.2, 3, 7.3], [8.7, 5.25, 7.8], "pen_b", NO_DOWN),
                 el([7.7, 3, 8.4], [8.2, 6, 8.9], "pen_c", NO_DOWN),
                 el([8.3, 3, 8.5], [8.6, 4.75, 8.8], "frame", NO_DOWN)])
PAPER_TRAY = []
for _y in (0, 2.75):
    PAPER_TRAY += [el([3, _y, 4], [13, _y + 0.25, 12], "plastic"),
                   el([3, _y + 0.25, 4], [3.5, _y + 1.25, 12], "plastic", NO_DOWN),
                   el([12.5, _y + 0.25, 4], [13, _y + 1.25, 12], "plastic", NO_DOWN),
                   el([3.5, _y + 0.25, 11.5], [12.5, _y + 1.25, 12], "plastic", NO_DOWN),
                   el([4, _y + 0.25, 4.5], [12, _y + 0.75, 11.25], "paper", NO_DOWN,
                      {"north": "stack", "east": "stack", "west": "stack"})]
PAPER_TRAY += [el([x, 1.25, 11.5], [x + 0.5, 2.75, 12], "plastic", SIDES) for x in (3, 12.5)]
# The desk lamp: a weighted base at the back, a post, an arm reaching forward over the desk and
# a shade whose lens faces down.
DESK_LAMP = [el([5.5, 0, 9], [10.5, 0.75, 13.5], "plastic"),
             el([7.5, 0.75, 10.75], [8.5, 9.5, 11.75], "plastic", SIDES),
             el([7.25, 8.75, 10.5], [8.75, 10.25, 12], "metal"),
             el([7.5, 9, 4.5], [8.5, 10, 10.5], "plastic", NO_BACK),
             el([6.5, 8, 3], [9.5, 9.25, 6], "plastic", NO_DOWN),
             el([5.5, 6.5, 2], [10.5, 8, 7], "plastic"),
             ALL_UV(el([5.9, 6.45, 2.4], [10.1, 6.5, 6.6], "glow", ("down",)), ["down"])]

# ------------------------------------------------------------------------------------------
# The copier: a floor-standing machine a block high, paper drawers, the scanner and its lid,
# the control panel with its status lamp, an output tray with a sheet on it
# ------------------------------------------------------------------------------------------
COPIER = ([el([1, 0.5, 2], [15, 13, 15], "shell", SIDES),
           el([1.5, 0, 2.5], [14.5, 0.5, 14.5], "rubber", ("north", "east", "west")),
           el([1, 13, 2], [15, 14.75, 15], "shell"),
           el([1.25, 14.75, 2.75], [14.75, 15.5, 14.75], "trim"),
           el([9, 14.75, 1], [14.75, 15.75, 4.5], "trim"),
           ALL_UV(el([9.5, 15.75, 1.5], [12.5, 15.8, 4], "control", ("up",)), ["up"]),
           ALL_UV(el([13, 15.75, 2], [14, 15.8, 3], "glow", ("up",)), ["up"]),
           el([-1.5, 9, 4], [1, 9.5, 13], "trim", NO_BACK + ("south",)),
           el([-1.25, 9.5, 5], [0.75, 9.6, 12], "paper", ("up",)),
           el([1.5, 9.75, 1.95], [14.5, 10.5, 2], "trim", ("north",))]
          + [s for y0, y1 in ((1, 4.75), (5, 8.75))
             for s in [el([1.25, y0, 1.5], [14.75, y1, 2], "trim", NO_BACK),
                       el([5.5, y1 - 1.25, 1], [10.5, y1 - 0.75, 1.5], "shell", NO_BACK)]])

# ------------------------------------------------------------------------------------------
# The streamer's set: the pull-up green screen, the ring light and the studio camera
# ------------------------------------------------------------------------------------------
GS_CASE = [el([1, 0, 6.5], [15, 1.75, 9.5], "case"),
           el([1.5, 0, 6], [2.5, 0.5, 10], "case"), el([13.5, 0, 6], [14.5, 0.5, 10], "case")]
GS_CLOSED = GS_CASE
GS_OPEN = GS_CASE + [el([1.25, 1.75, 7.9], [14.75, 29.5, 8.1], "chroma", ("north", "south")),
                     el([1, 29.5, 7.5], [15, 30.25, 8.5], "case"),
                     el([7.6, 1.75, 8.5], [8.4, 29.5, 9.1], "case", SIDES)]


def leg_splayed(axis, angle, hub_y, cx=8.0, cz=8.0):
    """A tripod leg: a rod from the floor to the hub, leaning outwards by angle."""
    return el([cx - 0.4, 0, cz - 0.4], [cx + 0.4, hub_y, cz + 0.4], "metal_black", SIDES,
              rot=(axis, angle, [cx, hub_y, cz]))


TRIPOD = [leg_splayed("z", 22.5, 9), leg_splayed("z", -22.5, 9), leg_splayed("x", 22.5, 9)]


def ring(cx, cy, z0, z1, r, w=1.0):
    """An upright octagonal ring in the XY plane facing north: four straight bars and four
    turned 45 degrees about Z, their north faces lit."""
    a = (r + w / 2) * R.TAN_22_5
    out = []
    for k, turned in enumerate((False, True)):
        rot = ("z", 45, [cx, cy, (z0 + z1) / 2]) if turned else None
        eps = 0.01 * k
        out += [el([cx - a, cy + r - w / 2, z0 + eps], [cx + a, cy + r + w / 2, z1 - eps],
                   "metal_black", ALL, {"north": "lens"}, rot=rot),
                el([cx - a, cy - r - w / 2, z0 + eps], [cx + a, cy - r + w / 2, z1 - eps],
                   "metal_black", ALL, {"north": "lens"}, rot=rot),
                el([cx - r - w / 2, cy - a, z0 + eps], [cx - r + w / 2, cy + a, z1 - eps],
                   "metal_black", ALL, {"north": "lens"}, rot=rot),
                el([cx + r - w / 2, cy - a, z0 + eps], [cx + r + w / 2, cy + a, z1 - eps],
                   "metal_black", ALL, {"north": "lens"}, rot=rot)]
    return out


RING_LIGHT = (TRIPOD
              + [el([7.5, 9, 7.5], [8.5, 23, 8.5], "metal_black", SIDES),
                 el([7.25, 8.5, 7.25], [8.75, 9.5, 8.75], "metal_black"),
                 el([7.6, 23, 7.6], [8.4, 24, 8.4], "metal_black", SIDES),
                 # the phone holder in the middle of the ring
                 el([6.5, 25.5, 7.5], [9.5, 28.5, 8], "case")]
              + ring(8, 27, 7.5, 8.5, 3.75))
STUDIO_CAMERA = (TRIPOD
                 + [el([7.25, 8.5, 7.25], [8.75, 13, 8.75], "metal_black"),
                    el([6.5, 13, 6.5], [9.5, 14.5, 9.5], "metal_black"),
                    el([8.5, 14, 11], [9, 14.5, 16], "metal_black"),
                    el([5.75, 14.5, 4], [10.25, 19, 12.5], "case"),
                    el([6.25, 15, 1], [9.75, 18.5, 4], "case", NO_BACK),
                    ALL_UV(el([5.75, 14.5, 0.25], [10.25, 19, 1], "metal", NO_BACK + ("south",),
                              {"north": "glass"}), ["north"]),
                    el([7, 19, 6], [9, 20.5, 10], "case"),
                    el([10.25, 15.5, 8], [12.25, 18, 12], "case", ALL, {"south": "glow"}),
                    ALL_UV(el([7.6, 20.5, 6.4], [8.4, 21, 7.2], "tally", NO_DOWN), NO_DOWN)])
ALL_UV(STUDIO_CAMERA[-2], ["south"])

# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
DRAWERS = "FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE"
LOCKER_SOUNDS = "FurnishingsSounds.LOCKER_DOOR_OPEN, FurnishingsSounds.LOCKER_DOOR_CLOSE"
PIECE_METAL = "Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID"
PIECE_CUTOUT = "Material.WOOD, SoundType.METAL, BlockRenderLayer.CUTOUT"


def jbox(box):
    return ", ".join(R._num(v) for v in box)


def box_of(specs):
    """The Java box of a piece: its elements' extent in whole pixels, inside -16..32."""
    built = [R.build(s) for s in specs]
    lo = [max(0, int(math.floor(min(e["from"][i] for e in built)))) for i in range(3)]
    hi = [min(32, int(math.ceil(max(e["to"][i] for e in built)))) for i in range(3)]
    hi[0] = min(16, hi[0])
    hi[2] = min(16, hi[2])
    return lo + hi


def run(body, end, java, extra=None):
    """A piece joining left and right: body, end parts where the run stops."""
    parts = {"body": body, "left": end, "right": mirror_x(end)}
    rules = [("body", {}), ("left", {"left": "false"}), ("right", {"right": "false"})]
    item = body + end + mirror_x(end)
    if extra:
        parts["tools"] = extra
        rules.append(("tools", {"right": "false"}))
        item = item + extra
    return {"parts": parts, "item": item, "rules": R.faced(rules), "java": java}


CAB = 'new BlockKitchenCabinet("%%s", new int[]{%s}, KitchenLine.%s, %d, KitchenFront.%s)'

# Every piece: (piece, kind, finishes, names, spec). kinds:
#   run: a multipart run (spec has parts, item, rules, java)
#   corner: the L-desk corner
#   table: joins four ways like the dining table
#   shelving: the residential bookcase's parts with binders
#   tall: two blocks, cut in halves (geo, box, java)
#   locker: a tall run
#   panel: a cubicle panel (parts, height)
#   storage: a faced storage block drawn whole (geo, java)
#   single: one model turned by facing (geo, java), with an optional display
#   folding: closed and open models (closed, open, java)
#   light: a lamp that switches, lens swapped (geo, java)
#   counter: rests on what is under it (geo, java); counter_lit: and switches
#   appliance: the copier (geo, java)
PIECES = [
    ("office_desk", "run", LAMINATES,
     ("Office Desk", "Bürotisch", "Escritorio de oficina", "Kontorsskrivbord"),
     run(DESK_BODY, DESK_END, CAB % ("0, 0, 0, 16, 12, 16", "DESK", 0, "OPEN"))),
    ("office_desk_pedestal", "run", LAMINATES,
     ("Office Desk with Pedestal", "Bürotisch mit Schubladenelement",
      "Escritorio con cajonera", "Skrivbord med lådhurts"),
     run(PEDESTAL_BODY, DESK_END, CAB % ("0, 0, 0, 16, 12, 16", "DESK", 18, "DRAWERS"))),
    ("office_desk_corner", "corner", LAMINATES,
     ("L-Desk Corner", "Winkelschreibtisch-Eckteil", "Esquina de escritorio en L",
      "Hörnskrivbord"),
     {"parts": CORNER_PARTS, "item": DESK_BODY + DESK_END + mirror_x(DESK_END),
      "rules": K.corner_rules(),
      "java": 'new BlockKitchenCorner("%s", new int[]{0, 0, 0, 16, 12, 16}, KitchenLine.DESK, '
              '0, KitchenFront.OPEN)'}),
    ("reception_desk", "run", LAMINATES,
     ("Reception Desk", "Empfangstheke", "Mostrador de recepción", "Receptionsdisk"),
     run(RECEPTION_BODY, RECEPTION_END,
         CAB % ("0, 0, 0, 16, 16, 15", "RECEPTION", 9, "DRAWERS"))),
    ("filing_cabinet", "tall", LAMINATES,
     ("Filing Cabinet", "Aktenschrank", "Archivador", "Arkivskåp"),
     {"geo": FILING, "java": 'new BlockResidentialTall("%%s", new int[]{%s}, false, 27, '
                             + DRAWERS + ')'}),
    ("office_shelving", "shelving", LAMINATES,
     ("Office Shelving", "Büroregal", "Estantería de oficina", "Kontorshylla"),
     {"java": 'new BlockBookcase("%s", new int[]{0, 0, 10, 16, 16, 16})'}),
    ("conference_table", "table", LAMINATES,
     ("Conference Table", "Konferenztisch", "Mesa de reuniones", "Konferensbord"),
     {"parts": {"top": CT_TOP, "apron": CT_APRON, "leg": CT_LEG, "apron_a": CT_APRON_A,
                "apron_b": CT_APRON_B},
      "item": CT_TOP + [s for r in (0, 90, 180, 270) for s in turn(CT_APRON + CT_LEG, r)],
      "rules": R.table_rules(), "java": 'new BlockDiningTable("%s")'}),
    ("teacher_desk", "storage", LAMINATES,
     ("Teacher's Desk", "Lehrerpult", "Escritorio del profesor", "Lärarkateder"),
     {"geo": TEACHER, "java": 'new BlockResidentialStorage("%%s", new int[]{%s}, 18, '
                              + DRAWERS + ')'}),
    # ---- cubicles ----
    ("cubicle_panel", "panel", PANEL_FABRICS,
     ("Cubicle Panel", "Stellwand", "Panel de cubículo", "Skärmvägg"),
     {"parts": PANEL_FULL, "java": 'new BlockCubiclePanel("%s", 16)'}),
    ("cubicle_panel_half", "panel", PANEL_FABRICS,
     ("Half-Height Cubicle Panel", "Halbhohe Stellwand", "Panel de cubículo bajo",
      "Låg skärmvägg"),
     {"parts": PANEL_HALF, "java": 'new BlockCubiclePanel("%s", 8)'}),
    # ---- seating ----
    ("task_chair", "single", SEAT_FABRICS,
     ("Task Chair", "Bürodrehstuhl", "Silla de oficina", "Kontorsstol"),
     {"geo": TASK_CHAIR, "particle": "fabric", "seat": (8, 0.5, 0), "upholstered": True}),
    ("guest_chair", "single", SEAT_FABRICS,
     ("Guest Chair", "Besucherstuhl", "Silla de visita", "Besöksstol"),
     {"geo": GUEST_CHAIR, "particle": "fabric", "seat": (8, 0.25, 0), "upholstered": True}),
    ("conference_chair", "single", SEAT_FABRICS,
     ("Conference Chair", "Konferenzstuhl", "Silla de conferencias", "Konferensstol"),
     {"geo": CONFERENCE_CHAIR, "particle": "fabric", "seat": (8.25, 0.5, 0),
      "upholstered": True}),
    ("gaming_chair", "single", SEAT_FABRICS,
     ("Gaming Chair", "Gaming-Stuhl", "Silla gaming", "Gamingstol"),
     {"geo": GAMING_CHAIR, "particle": "vinyl", "seat": (8, 0.5, 0), "upholstered": True}),
    ("waiting_bench", "run", SEAT_FABRICS,
     ("Waiting Room Bench", "Wartebank", "Banco de sala de espera", "Väntrumsbänk"),
     run(BENCH_BODY, BENCH_END,
         'new BlockResidentialRun("%s", new int[]{0, 0, 3, 16, 14, 13}, true, 7.25, 0.75)')),
    # ---- boards ----
    ("whiteboard", "run",
     [fin("aluminium", {"trim": T("stainless")}, "Aluminium", "Aluminium", "aluminio",
          "aluminium")],
     ("Whiteboard", "Whiteboard", "Pizarra blanca", "Whiteboard"),
     run(WHITEBOARD_BODY, WHITEBOARD_END,
         'new BlockResidentialRun("%s", new int[]{0, 2, 13, 16, 15, 16}, false)', MARKERS)),
    ("chalkboard", "run",
     [fin("oak", {"wood": T("oak"), "wood_v": T("oak_v"), "edge": T("oak_edge"),
                  "board": OT("chalkboard")}, "Oak", "Eiche", "roble", "ek")],
     ("Chalkboard", "Kreidetafel", "Pizarra de tiza", "Krittavla"),
     run(CHALKBOARD_BODY, CHALKBOARD_END,
         'new BlockResidentialRun("%s", new int[]{0, 2, 13, 16, 15, 16}, false)', CHALK)),
    ("cork_board", "run",
     [fin("oak", {"wood": T("oak"), "wood_v": T("oak_v"), "edge": T("oak_edge"),
                  "board": OT("cork_notes")}, "Oak", "Eiche", "roble", "ek")],
     ("Cork Notice Board", "Pinnwand", "Tablón de corcho", "Anslagstavla"),
     run(CORK_BODY, CORK_END,
         'new BlockResidentialRun("%s", new int[]{0, 2, 14, 16, 15, 16}, false)')),
    ("projector_screen", "folding",
     [fin("white", {"screen": OT("projector_screen")}, "White", "Weiß", "blanca", "vit")],
     ("Projector Screen", "Projektionsleinwand", "Pantalla de proyección", "Projektorduk"),
     {"closed": PROJ_CLOSED, "open": PROJ_OPEN, "particle": "case",
      "java": 'new BlockFoldingFixture("%s", new int[]{0, 13, 13, 16, 16, 16}, '
              'new int[]{0, 0, 13, 16, 16, 16}, FixtureMaterial.PLASTIC)'}),
    # ---- school ----
    ("school_desk", "single",
     [fin("blue", {"plastic": OT("plastic_blue"), "frame": T("metal_steel"), "wood": T("oak"),
                   "edge": T("oak_edge")}, "Blue", "Blau", "azul", "blå"),
      fin("red", {"plastic": OT("plastic_red"), "frame": T("metal_steel"), "wood": T("oak"),
                  "edge": T("oak_edge")}, "Red", "Rot", "rojo", "röd")],
     ("School Desk and Chair", "Schulbank mit Stuhl", "Pupitre con silla", "Skolbänk med stol"),
     {"geo": SCHOOL_DESK, "particle": "plastic", "seat": (7.5, -2.75, 0),
      "upholstered": False}),
    ("locker", "locker",
     [fin(c, {"steel": OT("locker_" + c)}, *n)
      for c, n in (("blue", ("Blue", "Blau", "azul", "blå")),
                   ("grey", ("Grey", "Grau", "gris", "grå")),
                   ("red", ("Red", "Rot", "rojo", "röd")))],
     ("Locker", "Spind", "Taquilla", "Klädskåp"),
     {"java": 'new BlockCloset("%s", new int[]{0, 0, 7, 16, 29, 16}, 9, ' + LOCKER_SOUNDS
              + ')'}),
    # ---- on the desk ----
    ("desktop_computer", "counter_lit", [BLACK],
     ("Desktop Computer", "Desktop-Computer", "Ordenador de sobremesa", "Stationär dator"),
     {"geo": DESKTOP_COMPUTER, "particle": "plastic",
      "glow": (OT("screen_off"), OT("screen_desktop")),
      "java": 'new BlockCounterLight("%%s", new int[]{%%s}, %s, 3)' % PIECE_METAL}),
    ("computer_tower", "counter", [BLACK],
     ("Computer Tower", "Tower-PC", "Torre de ordenador", "Datorlåda"),
     {"geo": COMPUTER_TOWER, "particle": "plastic",
      "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s)' % PIECE_METAL}),
    ("retro_computer", "counter_lit",
     [fin("beige", {"plastic": OT("plastic_beige"), "keys": OT("keys_beige"),
                    "face": OT("retro_front")}, "Beige", "Beige", "beige", "beige")],
     ("Retro Computer", "Retro-Computer", "Ordenador retro", "Retrodator"),
     {"geo": RETRO_COMPUTER, "particle": "plastic", "glow": (OT("crt_off"), OT("crt_on")),
      "java": 'new BlockCounterLight("%%s", new int[]{%%s}, %s, 3)' % PIECE_METAL}),
    ("laptop", "counter_lit",
     [fin("silver", {"shell": OT("silver"), "keys": OT("keys_silver")}, "Silver", "Silber",
          "plateado", "silver")],
     ("Laptop", "Laptop", "Portátil", "Bärbar dator"),
     {"geo": LAPTOP, "particle": "shell", "glow": (OT("screen_off"), OT("screen_laptop")),
      "java": 'new BlockCounterLight("%%s", new int[]{%%s}, %s, 2)' % PIECE_METAL}),
    ("desk_phone", "counter", [BLACK],
     ("Desk Phone", "Tischtelefon", "Teléfono de escritorio", "Bordstelefon"),
     {"geo": DESK_PHONE, "particle": "plastic",
      "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s, FurnishingsSounds.APPLIANCE_BEEP, '
              '1.6F, null)' % PIECE_METAL}),
    ("fax_machine", "counter",
     [fin("grey", {"shell": OT("plastic_light_grey"), "plastic": OT("plastic_dark_grey")},
          "Grey", "Grau", "gris", "grå")],
     ("Fax Machine", "Faxgerät", "Fax", "Faxmaskin"),
     {"geo": FAX_MACHINE, "particle": "shell",
      "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s, FurnishingsSounds.PRINTER_RUN, '
              '1.3F, null)' % PIECE_METAL}),
    ("pen_holder", "counter", [fin("black", {"mesh": OT("mesh")}, "Black", "Schwarz", "negro",
                                   "svart")],
     ("Pen Holder", "Stiftehalter", "Portalápices", "Pennställ"),
     {"geo": PEN_HOLDER, "particle": "mesh",
      "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s)' % PIECE_METAL}),
    ("paper_tray", "counter", [BLACK],
     ("Paper Tray", "Briefablage", "Bandeja para papel", "Brevkorg"),
     {"geo": PAPER_TRAY, "particle": "plastic",
      "java": 'new BlockCounterPiece("%%s", new int[]{%%s}, %s)' % PIECE_METAL}),
    ("desk_lamp", "counter_lit", [BLACK, WHITE_LAMP],
     ("Desk Lamp", "Schreibtischlampe", "Lámpara de escritorio", "Skrivbordslampa"),
     {"geo": DESK_LAMP, "particle": "plastic", "glow": (T("lens_off"), T("lens_on")),
      "java": 'new BlockCounterLight("%%s", new int[]{%%s}, %s, 12)' % PIECE_METAL}),
    ("copier", "appliance",
     [fin("grey", {"shell": OT("plastic_light_grey"), "trim": OT("plastic_dark_grey")},
          "Grey", "Grau", "gris", "grå")],
     ("Copier", "Kopierer", "Fotocopiadora", "Kopiator"),
     {"geo": COPIER, "particle": "shell", "glow": (T("lamp_green_off"), T("lamp_green_on")),
      "java": 'new BlockBuiltInAppliance("%s", new int[]{0, 0, 1, 16, 16, 15}, '
              'OfficeAppliances.COPIER, null)'}),
    # ---- streaming ----
    ("green_screen", "folding",
     [fin("chroma", {"chroma": OT("chroma_green")}, "Chroma Green", "Chroma-Grün",
          "verde croma", "kromagrön")],
     ("Green Screen", "Greenscreen", "Pantalla verde", "Greenscreen"),
     {"closed": GS_CLOSED, "open": GS_OPEN, "particle": "case",
      "java": 'new BlockFoldingFixture("%s", new int[]{1, 0, 6, 15, 2, 10}, '
              'new int[]{1, 0, 6, 15, 31, 10}, FixtureMaterial.PLASTIC)'}),
    ("ring_light", "light", [fin("black", {"metal_black": T("metal_black")}, "Black", "Schwarz",
                                 "negro", "svart")],
     ("Ring Light", "Ringlicht", "Aro de luz", "Ringlampa"),
     {"geo": RING_LIGHT, "particle": "metal_black",
      "java": 'new BlockKitchenLight("%s", new int[]{4, 0, 4, 12, 31, 12}, 14)'}),
    ("studio_camera", "single", [fin("black", {"case": OT("case_black")}, "Black", "Schwarz",
                                     "negra", "svart")],
     ("Studio Camera", "Studiokamera", "Cámara de estudio", "Studiokamera"),
     {"geo": STUDIO_CAMERA, "particle": "case",
      "java": 'new BlockBathroomFixture("%s", new int[]{4, 0, 1, 12, 21, 16}, '
              'FixtureMaterial.METAL)'}),
]
PIECE = {p[0]: p for p in PIECES}
GROUPS = {"office_desk": "Office", "cubicle_panel": "Cubicles", "task_chair": "Seating",
          "whiteboard": "Boards", "school_desk": "School", "desktop_computer": "On the desk",
          "copier": "Copier", "green_screen": "Streaming"}
GROUP_OF = {}
_g = None
for _p in PIECES:
    _g = GROUPS.get(_p[0], _g)
    GROUP_OF[_p[0]] = _g


def names_with(names, fnames):
    return ["%s (%s)" % (n, f) for n, f in zip(names, fnames)]


def java_for(piece, reg):
    _p, kind, _f, _n, spec = PIECE[piece]
    if kind in ("counter", "counter_lit"):
        return spec["java"] % (reg, jbox(box_of(spec["geo"])))
    if kind == "tall":
        return spec["java"] % jbox(box_of(spec["geo"])) % reg
    if kind == "storage":
        return spec["java"] % jbox(box_of(spec["geo"])) % reg
    if kind == "single" and "seat" in spec:
        seat = spec["seat"]
        return ('new BlockResidentialFurniture("%s", new int[]{%s}, %s, %s, %s, %s)'
                % (reg, jbox(box_of(spec["geo"])), "true" if spec["upholstered"] else "false",
                   R._num(seat[0]), R._num(seat[1]), R._num(seat[2])))
    return spec["java"] % reg


def entries():
    """Every block: (registry, piece, kind, finish textures, names, java)."""
    out = []
    for piece, kind, finishes, names, _spec in PIECES:
        for fid, ftex, *fnames in finishes:
            reg = "%s_%s" % (piece, fid)
            tex = dict(ftex)
            if kind == "shelving":
                tex.update(BINDERS)
            out.append((reg, piece, kind, tex, names_with(names, fnames), java_for(piece, reg)))
    return out


# ------------------------------------------------------------------------------------------
# Models and blockstates
# ------------------------------------------------------------------------------------------
def big(specs):
    return B.big_display(B.centred(specs))


def base_models():
    """Every base geometry model: (name, geometry json)."""
    out = []
    for piece, kind, _f, _n, spec in PIECES:
        if kind in ("run", "corner", "table"):
            particle = "fabric" if piece == "waiting_bench" else (
                "trim" if piece == "whiteboard" else "wood")
            for part, geo in spec["parts"].items():
                out.append(("%s_%s" % (piece, part), geometry(geo, particle)))
            out.append(("%s_item" % piece, geometry(spec["item"], particle)))
        elif kind == "tall":
            lower, upper = K.split_y(copy.deepcopy(spec["geo"]))
            out.append(("%s_lower" % piece, geometry(lower, "wood")))
            out.append(("%s_upper" % piece, geometry(upper, "wood")))
            out.append(("%s_item" % piece, geometry(spec["geo"], "wood", display=big(spec["geo"]))))
        elif kind == "storage":
            out.append(("%s_body" % piece, geometry(spec["geo"], "wood")))
            out.append(("%s_item" % piece, geometry(spec["geo"], "wood")))
        elif kind == "panel":
            for part, geo in spec["parts"].items():
                out.append(("%s_%s" % (piece, part), geometry(geo, "fabric")))
            out.append(("%s_item" % piece, geometry(panel_item(spec["parts"]), "fabric")))
        elif kind in ("single", "light", "appliance"):
            geo = spec["geo"]
            lo, hi = B.extent(geo)
            tall = hi[1] > 18
            out.append((piece, geometry(geo, spec.get("particle", "wood"),
                                        display=big(geo) if tall else None)))
        elif kind == "folding":
            out.append(("%s_closed" % piece, geometry(spec["closed"], spec["particle"])))
            out.append(("%s_open" % piece, geometry(spec["open"], spec["particle"],
                                                    display=big(spec["open"]))))
        elif kind in ("counter", "counter_lit"):
            geo, particle = spec["geo"], spec["particle"]
            for rest, drop in A.RESTS:
                out.append(("%s_%s" % (piece, rest), geometry(geo, particle, drop)))
            out.append(("%s_item" % piece, geometry(geo, particle, display=A.small_display(geo),
                                                    centre=True)))
    lower, upper = K.split_y(copy.deepcopy(LOCKER_BODY))
    end_l, end_u = K.split_y(copy.deepcopy(LOCKER_END))
    r_l, r_u = K.split_y(copy.deepcopy(mirror_x(LOCKER_END)))
    for name, geo in (("body_lower", lower), ("body_upper", upper), ("left_lower", end_l),
                      ("left_upper", end_u), ("right_lower", r_l), ("right_upper", r_u)):
        out.append(("locker_" + name, geometry(geo, "steel")))
    item = LOCKER_BODY + LOCKER_END + mirror_x(LOCKER_END)
    out.append(("locker_item", geometry(item, "steel", display=big(item))))
    return out


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


def single_state(piece, ftex, extra=None):
    variants = {"facing": facing_variants(), "inventory": [{}]}
    if extra:
        variants.update(extra)
    return {"forge_marker": 1, "defaults": {"model": BASE + piece, "textures": dict(ftex)},
            "variants": variants}


def switch(off, on, key):
    return {"true": {"textures": {key: on}}, "false": {"textures": {key: off}}}


def counter_state(piece, ftex, glow=None):
    tex = dict(ftex)
    variants = {"facing": facing_variants(),
                "rest": {rest: {"model": BASE + "%s_%s" % (piece, rest)} for rest, _d in A.RESTS},
                "inventory": [{"model": BASE + "%s_item" % piece}]}
    if glow:
        tex["glow"] = glow[1]
        variants["lit"] = switch(glow[0], glow[1], "glow")
        # Whether redstone last powered it: stored, drawn the same either way.
        variants["powered"] = {"true": {}, "false": {}}
    return {"forge_marker": 1,
            "defaults": {"model": BASE + "%s_floor" % piece, "textures": tex},
            "variants": variants}


def tall_state(reg):
    variants = {}
    for f, r in ROT.items():
        for upper in ("false", "true"):
            v = {"model": MODEL + "%s_%s" % (reg, "upper" if upper == "true" else "lower")}
            if r:
                v["y"] = r
            variants["facing=%s,upper=%s" % (f, upper)] = v
    return {"variants": variants}


def locker_rules():
    rules = []
    for half in ("lower", "upper"):
        up = "true" if half == "upper" else "false"
        rules += R.faced([("body_" + half, {"upper": up}),
                          ("left_" + half, {"upper": up, "left": "false"}),
                          ("right_" + half, {"upper": up, "right": "false"})])
    return rules


def rules_for(piece, kind, spec):
    if kind in ("run", "corner"):
        return spec["rules"]
    if kind == "table":
        return spec["rules"]
    if kind == "shelving":
        return R.MULTI["bookcase"]["rules"]
    if kind == "panel":
        return panel_rules()
    if kind == "storage":
        return R.faced([("body", {})])
    if kind == "locker":
        return locker_rules()
    raise ValueError(kind)


EXTRA_LANG = {
    "itemGroup.tabcommercialoffice": ("CSM: Commercial & Office", "CSM: Gewerbe & Büro",
                                      "CSM: Comercial y oficina", "CSM: Kommersiellt & kontor"),
    "csm.furnishings.appliance.supply.copier": (
        "A book and quill for each copy", "Ein Buch und Feder je Kopie",
        "Un libro y pluma por cada copia", "En bok och fjäderpenna per kopia"),
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
    for name, data in base_models():
        dump("models/block/%s/base/%s.json" % (SUB, name), data)
    particles = {}
    for name, data in base_models():
        particles[name] = data["textures"]["particle"][1:]
    for reg, piece, kind, ftex, _names, _java in entries():
        spec = PIECE[piece][4]
        # Every finish names the texture its models' particle points to, or the game logs an
        # upward reference and draws the missing texture.
        for name, key in particles.items():
            if (name == piece or name.startswith(piece + "_")) and key not in ftex:
                ftex = dict(ftex)
                ftex[key] = DEFAULT_TEX[key]
        blk = "models/block/%s/%s_%%s.json" % (SUB, reg)
        item = "models/item/%s.json" % reg
        if kind in ("run", "corner", "table", "panel"):
            for part in spec["parts"]:
                copy_model(blk % part, "%s_%s" % (piece, part), ftex)
            copy_model(item, "%s_item" % piece, ftex)
            state = multipart_state(reg, rules_for(piece, kind, spec))
        elif kind == "shelving":
            for part in R.MULTI["bookcase"]["parts"]:
                copy_model(blk % part, "bookcase_%s" % part, ftex, R.SUB)
            copy_model(item, "bookcase_item", ftex, R.SUB)
            state = multipart_state(reg, rules_for(piece, kind, spec))
        elif kind == "storage":
            copy_model(blk % "body", "%s_body" % piece, ftex)
            copy_model(item, "%s_item" % piece, ftex)
            state = multipart_state(reg, rules_for(piece, kind, spec))
        elif kind == "locker":
            for side in ("body", "left", "right"):
                for half in ("lower", "upper"):
                    copy_model(blk % ("%s_%s" % (side, half)), "locker_%s_%s" % (side, half),
                               ftex)
            copy_model(item, "locker_item", ftex)
            state = multipart_state(reg, locker_rules())
        elif kind == "tall":
            for half in ("lower", "upper"):
                copy_model(blk % half, "%s_%s" % (piece, half), ftex)
            copy_model(item, "%s_item" % piece, ftex)
            state = tall_state(reg)
        elif kind == "single":
            state = single_state(piece, ftex)
        elif kind == "light":
            state = single_state(piece, dict(ftex, lens=T("lens_on")),
                                 {"lit": switch(T("lens_off"), T("lens_on"), "lens"),
                                  "powered": {"true": {}, "false": {}}})
        elif kind == "appliance":
            off, on = spec["glow"]
            state = single_state(piece, dict(ftex, glow=off),
                                 {"running": switch(off, on, "glow")})
        elif kind == "folding":
            state = {"forge_marker": 1,
                     "defaults": {"model": BASE + piece + "_closed", "textures": dict(ftex)},
                     "variants": {"facing": facing_variants(),
                                  "open": {"true": {"model": BASE + piece + "_open"},
                                           "false": {"model": BASE + piece + "_closed"}},
                                  "inventory": [{"model": BASE + piece + "_open"}]}}
        elif kind in ("counter", "counter_lit"):
            state = counter_state(piece, ftex, spec.get("glow"))
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
    group = None
    for reg, piece, _k, _t, names, java in entries():
        if piece != last:
            if last is not None:
                lines.append("")
            if GROUP_OF[piece] != group:
                group = GROUP_OF[piece]
                lines.append("    // ---- %s ----" % group)
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
    tmp = tempfile.mkdtemp(prefix="office_")
    try:
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not R.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("office furniture is up to date (%d files)" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
