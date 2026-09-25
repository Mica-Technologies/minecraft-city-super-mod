#!/usr/bin/env python3
"""Every asset the Transit tab's bus stops ship: the stop poles, the agency flag signs with their
route plates, the timetable and route map cases, the real-time arrival display and the curb
plaque.

    python dev-env-utils/scripts/gen_transit_stops.py
    python dev-env-utils/scripts/gen_transit_stops.py --check
    python dev-env-utils/scripts/gen_transit_stops.py --fragments   # tab lines to paste

A stop is a stack. The pole block (BlockBusStopPole) is a length of pole; everything that clamps
to a pole (BlockBusStopFitting: the flag, the cases, the arrival display) is a length of pole too,
with its fitting on it, so a stop is built by stacking them: pole, timetable case, arrival
display, flag. Each draws the pole from three parts per pole style -- the shaft, the cap (only
where nothing of the stack is above) and the base (only where nothing is below) -- and a fitting
takes its pole style from the pole below it, so a flag on a square red pole has a square red
shaft. Those are actual state (`pole`, `cap`, `base`), picked by a multipart blockstate.

The pole styles are the constants of BusStopPoleStyle.java, in its order; the generator reads
them from there and stops if the two lists differ.

Every agency is invented: CITYLINE (teal and yellow, the fare machine's livery), RIVERWAY,
VERDANT and EMBERLINE. A flag is double-sided and stands out sideways from the pole like a real
stop flag. Its three route plates hang under it; each is shown only while it carries a number
(`route1`..`route3`, from TileEntityBusStopFlag), and the number itself is drawn by
TileEntityBusStopFlagRenderer on both faces, at the plate centres written into the Java below
(BULLET_*). The arrival display's screen is dark in the model; its text is the renderer's.

Faces that carry a picture use a window of their texture at the face's own aspect, so a texel is
square: the flag art is 49 x 64 texels on a 7 x 9.2 plate.
"""
import copy
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402
import gen_streetscape_utility as gu  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")
JAVA = os.path.join(REPO, "modules", "transit", "src", "main", "java", "com", "micatechnologies",
                    "minecraft", "csm", "transit", "stop")

C = lc.Catalogue("gen_transit_stops.py", "transit/stops", "transit/stops", assets=ASSETS)
TAB = "CsmTabTransit"
box = lc.box
post = lc.post

WHITE = (240, 242, 240)
BLACK = (18, 20, 22)
GALV = (158, 164, 162)
STEEL = (122, 128, 132)
GRAPHITE = (48, 52, 56)
CHARCOAL = (34, 36, 40)
ISA_BLUE = (18, 82, 160)
LED_OFF = (40, 26, 8)

# ------------------------------------------------------------------------------------------
# Agencies and pole styles
# ------------------------------------------------------------------------------------------
AGENCIES = [
    # id, name on the flag, primary, accent, layout
    ("cityline", "CITYLINE", (14, 94, 111), (244, 196, 40), "band",
     ("Bus Stop Flag (CITYLINE)", "Bushaltestellenschild (CITYLINE)",
      "Señal de Parada de Autobús (CITYLINE)", "Busshållplatsskylt (CITYLINE)")),
    ("riverway", "RIVERWAY", (31, 58, 115), (240, 138, 36), "stripe",
     ("Bus Stop Flag (RIVERWAY)", "Bushaltestellenschild (RIVERWAY)",
      "Señal de Parada de Autobús (RIVERWAY)", "Busshållplatsskylt (RIVERWAY)")),
    ("verdant", "VERDANT", (36, 116, 60), (236, 240, 232), "panel",
     ("Bus Stop Flag (VERDANT)", "Bushaltestellenschild (VERDANT)",
      "Señal de Parada de Autobús (VERDANT)", "Busshållplatsskylt (VERDANT)")),
    ("emberline", "EMBERLINE", (176, 36, 32), (56, 58, 62), "split",
     ("Bus Stop Flag (EMBERLINE)", "Bushaltestellenschild (EMBERLINE)",
      "Señal de Parada de Autobús (EMBERLINE)", "Busshållplatsskylt (EMBERLINE)")),
]

# name, shape, paint colour, names
STYLES = [
    ("round_galvanized", "round", GALV,
     ("Bus Stop Pole (Round, Galvanized)", "Haltestellenmast (Rund, Verzinkt)",
      "Poste de Parada (Redondo, Galvanizado)", "Hållplatsstolpe (Rund, Galvaniserad)")),
    ("square_galvanized", "square", GALV,
     ("Bus Stop Pole (Square, Galvanized)", "Haltestellenmast (Eckig, Verzinkt)",
      "Poste de Parada (Cuadrado, Galvanizado)", "Hållplatsstolpe (Fyrkantig, Galvaniserad)")),
    ("round_teal", "round", (14, 94, 111),
     ("Bus Stop Pole (Round, Teal)", "Haltestellenmast (Rund, Petrol)",
      "Poste de Parada (Redondo, Verde Azulado)", "Hållplatsstolpe (Rund, Petrol)")),
    ("square_navy", "square", (31, 58, 115),
     ("Bus Stop Pole (Square, Navy)", "Haltestellenmast (Eckig, Marineblau)",
      "Poste de Parada (Cuadrado, Azul Marino)", "Hållplatsstolpe (Fyrkantig, Marinblå)")),
    ("round_green", "round", (36, 116, 60),
     ("Bus Stop Pole (Round, Green)", "Haltestellenmast (Rund, Grün)",
      "Poste de Parada (Redondo, Verde)", "Hållplatsstolpe (Rund, Grön)")),
    ("square_red", "square", (176, 36, 32),
     ("Bus Stop Pole (Square, Red)", "Haltestellenmast (Eckig, Rot)",
      "Poste de Parada (Cuadrado, Rojo)", "Hållplatsstolpe (Fyrkantig, Röd)")),
]


def check_styles_against_java():
    """The pole styles must be BusStopPoleStyle's constants, in its order: the blockstate's
    `pole` values are those constants' names."""
    path = os.path.join(JAVA, "BusStopPoleStyle.java")
    text = open(path, encoding="utf-8").read()
    start = text.index("{", text.index("enum BusStopPoleStyle"))
    body = text[start + 1:text.index(";", start)]
    names = [m.lower() for m in re.findall(r"^\s*([A-Z_]+)\s*\(", body, re.M)]
    ours = [s[0] for s in STYLES]
    if names != ours:
        raise SystemExit("BusStopPoleStyle.java has %s, this generator %s" % (names, ours))


# ------------------------------------------------------------------------------------------
# Pixel art
# ------------------------------------------------------------------------------------------
def bus_icon(img, x, y, colour, window, wheel=BLACK):
    """A bus seen side on, facing left, 26 x 13 texels with (x, y) its top-left: a body with
    rounded corners, a row of windows, the windscreen and front door, two wheels."""
    lc.rect(img, x + 1, y, x + 25, y + 11, colour)
    lc.rect(img, x, y + 1, x + 26, y + 10, colour)
    # windscreen, door, then the side windows
    lc.rect(img, x + 1, y + 2, x + 4, y + 7, window)
    lc.rect(img, x + 5, y + 2, x + 8, y + 10, window)
    lc.rect(img, x + 6, y + 2, x + 7, y + 10, colour)
    for wx in range(10, 24, 4):
        lc.rect(img, x + wx, y + 2, x + wx + 3, y + 6, window)
    # destination sign over the windscreen
    lc.rect(img, x + 1, y + 1, x + 4, y + 2, lc.shade(window, 0.8))
    for cx in (x + 6.5, x + 20.5):
        lc.disc(img, cx, y + 11, 2.2, wheel)
        lc.disc(img, cx, y + 11, 0.9, lc.shade(colour, 1.0) if wheel == BLACK else colour)


def small_bus(img, x, y, colour, window):
    """A bus 9 x 6 texels, for a route plate."""
    lc.rect(img, x, y, x + 9, y + 5, colour)
    lc.rect(img, x + 1, y + 1, x + 3, y + 3, window)
    lc.rect(img, x + 4, y + 1, x + 8, y + 3, window)
    lc.rect(img, x + 1, y + 5, x + 3, y + 6, colour)
    lc.rect(img, x + 6, y + 5, x + 8, y + 6, colour)


ISA = [
    "....##...",
    "....##...",
    ".........",
    "....#....",
    "....####.",
    "..#.#....",
    ".#..####.",
    ".#.....#.",
    "..###..##",
]


def isa(img, x, y):
    """The accessibility symbol: a white figure in a wheelchair on blue, 11 x 11 texels."""
    lc.rect(img, x, y, x + 11, y + 11, ISA_BLUE)
    for r, line in enumerate(ISA):
        for c, ch in enumerate(line):
            if ch == "#":
                lc.rect(img, x + 1 + c, y + 1 + r, x + 2 + c, y + 2 + r, WHITE)


FLAG_W, FLAG_H = 49, 64  # the flag art's window on its 64 px texture


def flag_art(agency):
    """A flag's face. Four layouts, one per agency, so the four read as four agencies."""
    _, name, primary, accent, layout, _ = agency
    img = lc.fill(WHITE, 64, 2, 50 + [a[1] for a in AGENCIES].index(name))
    w, h = FLAG_W, FLAG_H
    cx = w / 2.0
    if layout == "band":
        lc.rect(img, 0, 0, w, 12, primary)
        lc.draw_text_centred(img, name, cx, 4, accent)
        lc.rect(img, 0, 12, w, 14, accent)
        bus_icon(img, int(cx - 13), 17, primary, WHITE)
        lc.draw_text_centred(img, "BUS", cx, 32, primary, 2)
        lc.draw_text_centred(img, "STOP", cx, 43, primary, 2)
        lc.rect(img, 0, 55, w, h, primary)
        isa(img, w - 13, 53)
    elif layout == "stripe":
        lc.rect(img, 0, 0, w, h, primary)
        lc.rect(img, 0, 0, 5, h, accent)
        lc.draw_text_centred(img, name, cx + 2.5, 4, WHITE)
        lc.rect(img, 8, 11, w - 3, 12, accent)
        bus_icon(img, int(cx - 10), 16, WHITE, primary)
        lc.draw_text_centred(img, "BUS", cx + 2.5, 31, WHITE, 2)
        lc.draw_text_centred(img, "STOP", cx + 2.5, 42, WHITE, 2)
        lc.rect(img, 8, 54, w - 16, 55, accent)
        isa(img, w - 14, 51)
    elif layout == "panel":
        lc.rect(img, 0, 0, w, h, primary)
        lc.draw_text_centred(img, name, cx, 4, WHITE)
        lc.rect(img, 4, 12, w - 4, 52, accent)
        for (px, py) in ((4, 12), (w - 5, 12), (4, 51), (w - 5, 51)):
            lc.rect(img, px, py, px + 1, py + 1, primary)
        bus_icon(img, int(cx - 13), 15, primary, accent)
        lc.draw_text_centred(img, "BUS", cx, 30, primary, 2)
        lc.draw_text_centred(img, "STOP", cx, 41, primary, 2)
        isa(img, w - 15, 53)
        lc.rect(img, 0, 60, w - 16, h, lc.shade(primary, 0.8))
    else:  # split
        lc.rect(img, 0, 0, w, 31, primary)
        lc.draw_text_centred(img, name, cx, 4, WHITE)
        bus_icon(img, int(cx - 13), 14, WHITE, primary, wheel=accent)
        lc.rect(img, 0, 31, w, 34, accent)
        lc.draw_text_centred(img, "BUS", cx - 5, 37, primary, 2)
        lc.draw_text_centred(img, "STOP", cx - 5, 49, primary, 2)
        isa(img, w - 13, 44)
    # the sheeting's edge, a texel of pale grey all round
    lc.frame(img, 0, 0, w, h, (206, 210, 212))
    return img


BULLET_W, BULLET_H = 64, 16  # a route plate's window: 7 x 1.8 units


def bullet_art(agency):
    """A route plate: the agency colour, a white edge and a small bus; the renderer draws the
    number in the middle."""
    _, name, primary, accent, _, _ = agency
    img = lc.fill(primary, 64, 2, 60 + [a[1] for a in AGENCIES].index(name))
    lc.frame(img, 0, 0, BULLET_W, BULLET_H, WHITE)
    small_bus(img, 4, 5, WHITE, primary)
    lc.rect(img, 15, 3, 16, 13, lc.shade(WHITE, 0.9))
    return img


def galvanised(seed, size=16):
    """Hot-dip zinc: a pale grey with the faint spangle of the coating."""
    img = lc.fill(GALV, size, 5, seed)
    px = img.load()
    import random
    rng = random.Random(seed)
    for _ in range(size * size // 6):
        x, y = rng.randrange(size), rng.randrange(size)
        px[x, y] = lc.shade(GALV, rng.choice((0.88, 1.1, 1.16))) + (255,)
    return img


# --- the posters ---------------------------------------------------------------------------
TT_W, TT_H = 51, 64      # timetable window (8 x 10 units)
MAP_W, MAP_H = 64, 57    # route map window (9 x 8 units)
HEAD = (38, 50, 64)


def timetable():
    img = lc.fill((246, 246, 240), 64, 2, 41)
    w = TT_W
    lc.rect(img, 0, 0, w, 11, HEAD)
    lc.draw_text_centred(img, "TIMETABLE", w / 2.0, 4, WHITE)
    lc.draw_text(img, "WEEKDAYS", 5, 13, (110, 116, 122))
    minutes = ["05 25 45", "00 20 40", "10 30 50", "05 25 45", "15 35 55", "00 30",
               "10 40"]
    for i, row in enumerate(minutes):
        y = 19 + i * 6
        if i % 2 == 0:
            lc.rect(img, 3, y - 1, w - 3, y + 5, (226, 232, 236))
        lc.rect(img, 3, y - 1, 13, y + 5, lc.shade(HEAD, 1.35) if i % 2 else HEAD)
        lc.draw_text_centred(img, str(6 + i * 2), 8, y, WHITE)
        lc.draw_text(img, row, 15, y, (40, 44, 48))
    glass_sheen(img, w, TT_H)
    return img


def route_map():
    img = lc.fill((242, 238, 226), 64, 2, 42)
    w, h = MAP_W, MAP_H
    # a park and a river, the ground the lines run over
    lc.rect(img, 38, 14, 56, 28, (196, 222, 180))
    for x in range(0, w):
        y = int(37 + (x - 30) * 0.35)
        lc.rect(img, x, y, x + 1, y + 4, (170, 204, 230))
    lc.rect(img, 0, 0, w, 10, HEAD)
    lc.draw_text_centred(img, "ROUTE MAP", w / 2.0, 3, WHITE)
    lines = [((14, 94, 111), [(5, 20), (22, 20), (32, 30), (58, 30)]),
             ((240, 138, 36), [(10, 42), (10, 36), (32, 30), (32, 14)]),
             ((36, 116, 60), [(5, 41), (20, 41), (32, 30), (45, 41), (58, 41)])]
    for colour, pts in lines:
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            steps = max(abs(x1 - x0), abs(y1 - y0))
            for s in range(steps + 1):
                x = round(x0 + (x1 - x0) * s / float(steps))
                y = round(y0 + (y1 - y0) * s / float(steps))
                lc.rect(img, x - 1, y - 1, x + 1, y + 1, colour)
        for x, y in pts:
            lc.rect(img, x - 1, y - 1, x + 2, y + 2, WHITE)
            lc.frame(img, x - 2, y - 2, x + 3, y + 3, colour)
    # the interchange, and "you are here"
    lc.rect(img, 30, 28, 35, 33, WHITE)
    lc.frame(img, 29, 27, 36, 34, BLACK)
    lc.disc(img, 22.5, 20.5, 3.2, (220, 40, 40))
    lc.disc(img, 22.5, 20.5, 1.4, WHITE)
    # legend
    for i, (colour, num) in enumerate((((14, 94, 111), "12"), ((240, 138, 36), "40"),
                                       ((36, 116, 60), "7"))):
        x = 6 + i * 18
        lc.rect(img, x, 46, x + 13, 52, colour)
        lc.draw_text_centred(img, num, x + 6.5, 47, WHITE)
    glass_sheen(img, w, h)
    return img


def glass_sheen(img, w, h):
    """The case's glass: two faint diagonal highlights across the poster."""
    px = img.load()
    for k in (0.3, 0.42):
        for y in range(h):
            x0 = int(w * k + y * 0.55 - h * 0.25)
            for x in range(x0, x0 + (3 if k == 0.3 else 2)):
                if 0 <= x < w:
                    r, g, b, a = px[x, y]
                    px[x, y] = lc.clamp((r + (255 - r) * 0.14, g + (255 - g) * 0.14,
                                         b + (255 - b) * 0.14)) + (a,)


DISPLAY_W, DISPLAY_H = 64, 29  # the display's front: 14 x 6.4 units, 4.57 texels a unit


def display_front():
    """The display's face: charcoal housing, a black screen with a faint dot grid (the renderer
    lights the text), a small stop-info label under it."""
    img = lc.fill(CHARCOAL, 64, 2, 43)
    lc.rect(img, 3, 3, 61, 23, BLACK)
    px = img.load()
    for y in range(4, 22, 2):
        for x in range(4, 60, 2):
            px[x, y] = LED_OFF + (255,)
    lc.frame(img, 2, 2, 62, 24, lc.shade(CHARCOAL, 1.4))
    lc.draw_text(img, "NEXT BUS", 4, 24, (150, 156, 160))
    lc.rect(img, 54, 25, 60, 27, (40, 170, 90))
    return img


PLAQUE = (142, 104, 58)


def plaque():
    """A cast bronze plate for the curb: a raised border, BUS STOP and a bus, drawn upside down
    because the plate's top face shows the texture turned 180 to the player who placed it."""
    from PIL import Image
    img = lc.fill(PLAQUE, 64, 5, 44)
    lc.frame(img, 2, 2, 62, 62, lc.shade(PLAQUE, 1.35), 2)
    lc.frame(img, 4, 4, 60, 60, lc.shade(PLAQUE, 0.7), 1)
    raised = lc.shade(PLAQUE, 1.3)
    shadow = lc.shade(PLAQUE, 0.62)
    for dx, dy, col in ((1, 1, shadow), (0, 0, raised)):
        bus_icon(img, 19 + dx, 12 + dy, col, lc.shade(PLAQUE, 0.85), wheel=col)
        lc.draw_text_centred(img, "BUS", 32 + dx, 30 + dy, col, 2)
        lc.draw_text_centred(img, "STOP", 32 + dx, 44 + dy, col, 2)
    return img.transpose(Image.ROTATE_180)


def register_textures():
    tex = {
        "clamp": lc.fill(STEEL, 16, 4, 5),
        "edge": lc.fill((196, 200, 202), 16, 3, 6),
        "case": lc.fill(GRAPHITE, 16, 3, 7),
        "timetable": timetable(),
        "route_map": route_map(),
        "display_housing": lc.fill(CHARCOAL, 16, 3, 8),
        "display_front": display_front(),
        "plaque": plaque(),
        "plaque_edge": lc.fill(lc.shade(PLAQUE, 0.8), 16, 4, 9),
    }
    for i, (name, _, colour, _) in enumerate(STYLES):
        tex["pole_" + name] = galvanised(20 + i) if colour == GALV else lc.fill(colour, 16, 4,
                                                                                 20 + i)
    for a in AGENCIES:
        tex["flag_" + a[0]] = flag_art(a)
        tex["route_" + a[0]] = bullet_art(a)
    for name, img in tex.items():
        C.texture(name)(lambda img=img: img)


# ------------------------------------------------------------------------------------------
# Geometry (every fitting faces north, the pole at the middle of the block)
# ------------------------------------------------------------------------------------------
R_ROUND = 1.0     # the round pole's inradius
H_SQUARE = 0.9    # half the square pole's width
SIDES = ("north", "south", "east", "west")


def shaft(shape):
    """A block's length of pole, with no ends: stacked lengths meet, and an open end is covered
    by the cap or the base."""
    if shape == "round":
        return post(8, 8, R_ROUND, 0, 16, "pole", top=False, bottom=False)
    return [box([8 - H_SQUARE, 0, 8 - H_SQUARE], [8 + H_SQUARE, 16, 8 + H_SQUARE], "pole",
                faces=SIDES)]


def cap(shape):
    """The top of a pole: a sleeve over its last sixteenth and a low dome on it."""
    if shape == "round":
        return (post(8, 8, 1.25, 15, 16, "pole")
                + post(8, 8, 0.75, 16, 16.4, "pole", bottom=False))
    return [box([6.85, 15, 6.85], [9.15, 16, 9.15], "pole"),
            box([7.4, 16, 7.4], [8.6, 16.3, 8.6], "pole", faces=SIDES + ("up",))]


def base(shape):
    """Where a pole meets the ground: a flange with four bolts and a collar."""
    if shape == "round":
        els = post(8, 8, 2.1, 0, 0.3, "pole", bottom=False) + post(8, 8, 1.3, 0.3, 1.4, "pole",
                                                                  bottom=False)
    else:
        els = [box([5.9, 0, 5.9], [10.1, 0.3, 10.1], "pole", faces=SIDES + ("up",)),
               box([6.7, 0.3, 6.7], [9.3, 1.4, 9.3], "pole", faces=SIDES + ("up",))]
    for x, z in ((6.4, 6.4), (9.2, 6.4), (6.4, 9.2), (9.2, 9.2)):
        els.append(box([x, 0.3, z], [x + 0.4, 0.6, z + 0.4], "clamp",
                       faces=SIDES + ("up",)))
    return els


def band(y0, y1, front=True):
    """A clamp band round the pole, big enough for either pole shape."""
    faces = SIDES + ("up", "down") if front else ("south", "east", "west", "up", "down")
    return box([6.7, y0, 6.7], [9.3, y1, 9.3], "clamp", faces=faces)


def face(tex, uv):
    return {"texture": "#" + tex, "uv": [round(v, 4) for v in uv]}


def win(w_px, h_px):
    """The uv of a picture window w_px x h_px texels at the top-left of a 64 px texture."""
    return [0, 0, round(w_px / 4.0, 4), round(h_px / 4.0, 4)]


def mirrored(uv):
    return [uv[2], uv[1], uv[0], uv[3]]


# --- the flag -------------------------------------------------------------------------------
PX0, PX1 = 9.6, 16.6            # the plate, sticking out east of the pole
PY0, PY1 = 6.4, 15.6
PZ0, PZ1 = 7.8, 8.2
BULLET_TOPS = (6.1, 4.1, 2.1)   # each route plate's top; it is 1.8 tall
BULLET_HT = 1.8
BZ0, BZ1 = 7.85, 8.15


def flag_elements():
    """The flag plate, art on both faces, and its two brackets to the pole."""
    art = win(FLAG_W, FLAG_H)
    els = [{"from": [PX0, PY0, PZ0], "to": [PX1, PY1, PZ1], "faces": {
        "north": face("art", art),
        # seen from the south the plate is on the viewer's right: the same art, read the same way
        "south": face("art", art),
        "east": face("edge", [0, 0, 0.4, 9.2]), "west": face("edge", [0, 0, 0.4, 9.2]),
        "up": face("edge", [0, 0, 7, 0.4]), "down": face("edge", [0, 0, 7, 0.4])}}]
    for y in (14.3, 7.4):
        els.append(band(y, y + 0.8))
        els.append(box([9.3, y + 0.15, 7.65], [PX0, y + 0.65, 8.35], "clamp",
                       faces=("north", "south", "up", "down")))
    return els


def bullet_elements(i):
    top = BULLET_TOPS[i]
    y0 = top - BULLET_HT
    art = win(BULLET_W, BULLET_H)
    return [
        {"from": [PX0, y0, BZ0], "to": [PX1, top, BZ1], "faces": {
            "north": face("art", art), "south": face("art", art),
            "east": face("edge", [0, 0, 0.3, 1.8]), "west": face("edge", [0, 0, 0.3, 1.8]),
            "up": face("edge", [0, 0, 7, 0.3]), "down": face("edge", [0, 0, 7, 0.3])}},
        box([8.9, y0 + 0.6, 7.75], [PX0, y0 + 1.2, 8.25], "clamp",
            faces=("north", "south", "up", "down")),
    ]


# --- the cases -------------------------------------------------------------------------------
def case_elements(x0, x1, y0, y1, poster_w, poster_h):
    """A poster case clamped to the front of the pole: a shallow box whose front is the poster,
    a frame standing proud of it, two bands round the pole behind."""
    z0, z1 = 5.8, 6.6
    rim = 0.35
    els = [{"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": {
        "north": face("art", win(poster_w, poster_h)),
        "south": face("case", [x0, 16 - y1, x1, 16 - y0]),
        "east": face("case", [z0, 16 - y1, z1, 16 - y0]),
        "west": face("case", [z0, 16 - y1, z1, 16 - y0]),
        "up": face("case", [x0, z0, x1, z1]), "down": face("case", [x0, z0, x1, z1])}}]
    fz0 = z0 - 0.3
    rims = [([x0, y1 - rim, fz0], [x1, y1, z0]), ([x0, y0, fz0], [x1, y0 + rim, z0]),
            ([x0, y0 + rim, fz0], [x0 + rim, y1 - rim, z0]),
            ([x1 - rim, y0 + rim, fz0], [x1, y1 - rim, z0])]
    for frm, to in rims:
        els.append(box(frm, to, "case", faces=("north", "east", "west", "up", "down")))
    for y in (y0 + 1.5, y1 - 2.3):
        els.append(band(y, y + 0.8, front=False))
    return els


# --- the arrival display ------------------------------------------------------------------
DX0, DX1, DY0, DY1, DZ0, DZ1 = 1.0, 15.0, 5.6, 12.0, 4.6, 6.6


def display_elements():
    els = [{"from": [DX0, DY0, DZ0], "to": [DX1, DY1, DZ1], "faces": {
        "north": face("front", win(DISPLAY_W, DISPLAY_H)),
        "south": face("housing", [DX0, 16 - DY1, DX1, 16 - DY0]),
        "east": face("housing", [DZ0, 16 - DY1, DZ1, 16 - DY0]),
        "west": face("housing", [DZ0, 16 - DY1, DZ1, 16 - DY0]),
        "up": face("housing", [DX0, DZ0, DX1, DZ1]),
        "down": face("housing", [DX0, DZ0, DX1, DZ1])}}]
    # a hood over the screen, and the bracket plate behind
    els.append(box([DX0 - 0.3, DY1, DZ0 - 0.9], [DX1 + 0.3, DY1 + 0.4, DZ1], "housing"))
    els.append(box([6.2, DY0 + 0.8, DZ1], [9.8, DY1 - 0.8, 6.7], "clamp",
                   faces=("east", "west", "up", "down")))
    for y in (DY0 + 1.0, DY1 - 1.8):
        els.append(band(y, y + 0.8, front=False))
    return els


# ------------------------------------------------------------------------------------------
# Blockstates and catalogue
# ------------------------------------------------------------------------------------------
FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def pole_parts(when=None):
    """The multipart rules that draw the pole: for a pole block its own style (when=None), for
    a fitting the style its `pole` property names."""
    rules = []
    for name, _, _, _ in STYLES:
        cond = {} if when == name else {"pole": name}
        if when is not None and when != name:
            continue
        rules.append(dict({"apply": {"model": C.M("pole_shaft_" + name)}},
                          **({"when": cond} if cond else {})))
        rules.append({"when": dict(cond, cap="true"), "apply": {"model": C.M("pole_cap_" + name)}})
        rules.append({"when": dict(cond, base="true"),
                      "apply": {"model": C.M("pole_base_" + name)}})
    return rules


def facing_parts(model, extra=None):
    rules = []
    for f, y in FACINGS:
        cond = dict({"facing": f}, **(extra or {}))
        apply = {"model": model}
        if y:
            apply["y"] = y
        rules.append({"when": cond, "apply": apply})
    return rules


def textures_for(tex):
    t = {k: C.T(v) for k, v in tex.items()}
    t["particle"] = t[list(tex)[0]]
    return t


def model(tex, els, ao=True):
    return lc.model(textures_for(tex), els, ao=ao)


def item_model(tex, els, display=None):
    m = lc.model(textures_for(tex), els)
    if display:
        m["display"] = display
    return m


def bounds(els):
    x0, y0, z0, x1, y1, z1 = gu.bounds(els)
    clip = lambda v: max(0.0, min(16.0, round(v, 2)))  # noqa: E731
    return [clip(x0), clip(y0), clip(z0), clip(x1), clip(y1), clip(z1)]


def jbox(b):
    return "new double[]{%s}" % ", ".join("%s" % (int(v) if v == int(v) else v) for v in b)


def poles():
    for name, shape, _, names in STYLES:
        reg = "bus_stop_pole_" + name
        tex = {"pole": "pole_" + name, "clamp": "clamp"}
        models = {"pole_shaft_" + name: model(tex, shaft(shape)),
                  "pole_cap_" + name: model(tex, cap(shape)),
                  "pole_base_" + name: model(tex, base(shape))}
        state = {"multipart": pole_parts(when=name)}
        item = item_model(tex, base(shape) + shaft(shape) + cap(shape))
        java = 'new BlockBusStopPole("%s", BusStopPoleStyle.%s)' % (reg, name.upper())
        C.add(reg, java, names, models, state, item=item, tab=TAB)


def fitting_item(tex, els):
    """A fitting's item: its fitting on a length of galvanized round pole with a cap."""
    t = dict(tex, pole="pole_round_galvanized")
    return item_model(t, shaft("round") + cap("round") + els)


def flags():
    for a in AGENCIES:
        aid, _, _, _, _, names = a
        reg = "bus_stop_flag_" + aid
        tex = {"art": "flag_" + aid, "edge": "edge", "clamp": "clamp"}
        rtex = {"art": "route_" + aid, "edge": "edge", "clamp": "clamp"}
        models = {"flag_" + aid: model(tex, flag_elements())}
        for i in range(3):
            models["flag_%s_route%d" % (aid, i + 1)] = model(rtex, bullet_elements(i))
        rules = pole_parts() + facing_parts(C.M("flag_" + aid))
        for i in range(3):
            rules += facing_parts(C.M("flag_%s_route%d" % (aid, i + 1)),
                                  {"route%d" % (i + 1): "true"})
        # the item: pole, flag and one route plate, moved west so the whole sits in the slot
        els = copy.deepcopy(shaft("round") + cap("round") + flag_elements()
                            + bullet_elements(0))
        item_tex = {"pole": "pole_round_galvanized", "art": "flag_" + aid, "edge": "edge",
                    "clamp": "clamp"}
        # the route plate's art is a different texture: give its faces their own key
        for el in els[-2:]:
            for f in el["faces"].values():
                if f["texture"] == "#art":
                    f["texture"] = "#route"
        item_tex["route"] = "route_" + aid
        for el in els:
            el["from"][0] = round(el["from"][0] - 4.3, 4)
            el["to"][0] = round(el["to"][0] - 4.3, 4)
            if "rotation" in el:
                el["rotation"]["origin"][0] = round(el["rotation"]["origin"][0] - 4.3, 4)
        item = item_model(item_tex, els)
        b = bounds(shaft("round") + flag_elements() + bullet_elements(2))
        java = 'new BlockBusStopFlag("%s", %s)' % (reg, jbox(b))
        C.add(reg, java, names, models, {"multipart": rules}, item=item, tab=TAB)


def cases():
    specs = [
        ("bus_stop_timetable_case", "timetable", (4.0, 12.0, 3.0, 13.0), (TT_W, TT_H),
         ("Bus Stop Timetable Case", "Fahrplanvitrine", "Vitrina de Horarios",
          "Tidtabellsskåp")),
        ("bus_stop_route_map_case", "route_map", (3.5, 12.5, 4.0, 12.0), (MAP_W, MAP_H),
         ("Bus Stop Route Map Case", "Liniennetzvitrine", "Vitrina de Mapa de Rutas",
          "Linjekartsskåp")),
    ]
    for reg, poster, (x0, x1, y0, y1), (pw, ph), names in specs:
        els = case_elements(x0, x1, y0, y1, pw, ph)
        tex = {"art": poster, "case": "case", "clamp": "clamp"}
        models = {reg: model(tex, els)}
        rules = pole_parts() + facing_parts(C.M(reg))
        java = 'new BlockBusStopFitting("%s", %s)' % (reg, jbox(bounds(shaft("round") + els)))
        C.add(reg, java, names, models, {"multipart": rules}, item=fitting_item(tex, els),
              tab=TAB)


def display():
    reg = "bus_stop_arrival_display"
    els = display_elements()
    tex = {"front": "display_front", "housing": "display_housing", "clamp": "clamp"}
    models = {reg: model(tex, els)}
    rules = pole_parts() + facing_parts(C.M(reg))
    java = 'new BlockBusArrivalDisplay("%s", %s)' % (reg, jbox(bounds(shaft("round") + els)))
    C.add(reg, java, ("Bus Arrival Display", "Abfahrtsanzeige (Bus)",
                      "Pantalla de Llegadas de Autobús", "Avgångsskylt (Buss)"),
          models, {"multipart": rules}, item=fitting_item(tex, els), tab=TAB)


def curb_plaque():
    reg = "bus_stop_curb_plaque"
    els = [{"from": [4, 0, 4], "to": [12, 0.4, 12], "faces": {
        "up": face("art", [0, 0, 16, 16]),
        "north": face("edge", [4, 15.6, 12, 16]), "south": face("edge", [4, 15.6, 12, 16]),
        "east": face("edge", [4, 15.6, 12, 16]), "west": face("edge", [4, 15.6, 12, 16])}}]
    tex = {"art": "plaque", "edge": "plaque_edge"}
    m = model(tex, els)
    m["display"] = {
        # face up to the viewer, turned back the half turn the texture is stored in
        "gui": {"rotation": [90, 180, 0], "translation": [0, 0, 0], "scale": [1.3, 1.3, 1.3]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [90, 180, 0], "translation": [0, 0, 0], "scale": [1.3, 1.3, 1.3]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.375, 0.375, 0.375]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 4, 0],
                                  "scale": [0.4, 0.4, 0.4]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 4, 0],
                                 "scale": [0.4, 0.4, 0.4]},
    }
    state = lc.facing_state(C.M(reg))
    C.add(reg, 'new BlockBusStopPlaque("%s")' % reg,
          ("Bus Stop Curb Plaque", "Haltestellen-Bordsteinplakette",
           "Placa de Parada en el Bordillo", "Hållplatsplakett för Trottoarkant"),
          {reg: m}, state, tab=TAB)


C.add_lang("csm.transit.flag.route", ("Route plate %s: %s", "Linienschild %s: %s",
                                      "Placa de ruta %s: %s", "Linjeskylt %s: %s"))
C.add_lang("csm.transit.flag.route_none", ("Route plate %s: none", "Linienschild %s: keins",
                                           "Placa de ruta %s: ninguna", "Linjeskylt %s: ingen"))

register_textures()
poles()
flags()
cases()
display()
curb_plaque()

if __name__ == "__main__":
    check_styles_against_java()
    sys.exit(C.main())
