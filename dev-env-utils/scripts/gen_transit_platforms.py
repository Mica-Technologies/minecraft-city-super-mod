#!/usr/bin/env python3
"""Every asset the Transit tab's station and platform fit-out ships: tactile paving, platform
furniture, station signs, wall tile, columns, the canopy and the ticket validator.

    python dev-env-utils/scripts/gen_transit_platforms.py
    python dev-env-utils/scripts/gen_transit_platforms.py --check
    python dev-env-utils/scripts/gen_transit_platforms.py --fragments   # tab lines to paste

The set is made to fit out stations for RCMC (Rails & Coasters), the author's own train mod,
which already has the platforms themselves (Station Platform and Platform Edge, at exactly car
floor height), line map signs, arrival boards, station speakers and its spline track. Nothing
here repeats those: no platform or edge block, no speaker, no board, nothing laid along the
track. What is here, and the class that places each (package transit.platform):

- **Tactile paving** (BlockTactilePaving): one-pixel overlays laid on any floor -- RCMC's
  platform decking included -- as vanilla carpet is: warning domes and guidance bars, in yellow
  and in grey with stainless studs. As with Building's floor finishes the blockstate picks one of
  two drawings and a turn per block position, so a floor of it shows no repeat; the bars keep
  running the way they were laid.
- **Furniture**: a platform bench of perforated steel seats with armrests on a beam
  (BlockPlatformBench: benches join into one, an armrest at each joint and end armrests only at
  its ends), a lean perch (BlockPlatformRun), a help point column with an information and an
  emergency button and a red wall emergency point (BlockPlatformHelpPoint), CCTV (a ceiling dome
  and a camera on a wall bracket), a hanging double-faced clock (BlockPlatformClock, whose hands
  are its renderer's) and a clear-bag litter bin.
- **Signs**: the hanging platform number sign (BlockPlatformNumberSign, 1 to 20, a texture per
  number), hanging wayfinding signs (TO TRAINS and EXIT with their arrows, line bullets), the
  gap warning, a station name panel (BlockStationNameSign, ten invented names) and a network map
  board with no place names.
- **Architecture**: glazed subway tile, plain and with a frieze band in each agency's colour
  (BlockStationTile), columns tiled or steel that stack with a plinth and a capital only at the
  ends (BlockPlatformColumn), one with a platform number band (BlockPlatformColumnNumber), and a
  canopy that joins on all four sides with a fascia only round its outside (BlockPlatformCanopy).
- **The ticket validator** (BlockPlatformValidator): takes the fare gates' Fare Ticket or a trip
  off a Transit Card, its screen green or red for a moment.

Every model faces north: the bench's sitter, a sign's front and the validator's screen all look
north, and a wall-mounted piece has its wall at z = 16. Wording, station names and line bullets
are invented; the four agencies are gen_transit_stops.py's.

Faces that carry a picture use a window of their square texture at the face's own aspect, so a
texel is square; plain surfaces take their uv from the face's own place in the block, moved by
whole blocks into 0..16 where a part reaches past its cell.
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")

C = lc.Catalogue("gen_transit_platforms.py", "transit/platforms", "transit/platforms",
                 assets=ASSETS)
TAB = "CsmTabTransit"

WHITE = (240, 242, 240)
BLACK = (18, 20, 22)
YELLOW = (236, 188, 28)
GRAPHITE = (48, 52, 56)
STEEL = (70, 74, 78)
STAINLESS = (188, 192, 196)
NAVY = (24, 40, 76)
SIGN_EDGE = (150, 156, 160)

# the four agencies' line colours (gen_transit_stops.py)
CITYLINE = (14, 94, 111)
RIVERWAY_ORANGE = (240, 138, 36)
VERDANT = (36, 116, 60)
EMBERLINE = (176, 36, 32)

ALL = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")


# ------------------------------------------------------------------------------------------
# Drawing helpers
# ------------------------------------------------------------------------------------------
def grain(size, colour, amount, seed, alpha=255):
    return lc.fill(colour, size, amount, seed, alpha)


def speckle(img, rng, rate, lighter, darker, region=None):
    """Aggregate: odd texels a shade lighter or darker."""
    px = img.load()
    x0, y0, x1, y1 = region or (0, 0, img.width, img.height)
    for y in range(y0, y1):
        for x in range(x0, x1):
            r = rng.random()
            if r < rate:
                px[x, y] = lc.clamp(lighter) + (255,)
            elif r < rate * 2:
                px[x, y] = lc.clamp(darker) + (255,)


def line(img, x0, y0, x1, y1, colour, width=1.0):
    """A straight stroke, width texels wide."""
    px = img.load()
    c = lc.clamp(colour[:3]) + (255,)
    dx, dy = x1 - x0, y1 - y0
    n = max(1, int(math.hypot(dx, dy) * 2))
    for i in range(n + 1):
        cx = x0 + dx * i / n
        cy = y0 + dy * i / n
        r = width / 2.0
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                if 0 <= x < img.width and 0 <= y < img.height and \
                        (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                    px[x, y] = c


def arrow(img, x, y, w, h, colour, right=True):
    """A bold arrow, w x h texels with its top-left at (x, y), pointing right or left."""
    shaft = max(2, h // 3)
    head = h // 2 + 1
    sy = y + (h - shaft) // 2
    if right:
        lc.rect(img, x, sy, x + w - head, sy + shaft, colour)
        for i in range(head):
            half = int(round((h / 2.0) * (1 - i / float(head))))
            lc.rect(img, x + w - head + i, y + h // 2 - half, x + w - head + i + 1,
                    y + h // 2 + half + (h % 2), colour)
    else:
        lc.rect(img, x + head, sy, x + w, sy + shaft, colour)
        for i in range(head):
            half = int(round((h / 2.0) * (1 - i / float(head))))
            lc.rect(img, x + head - i - 1, y + h // 2 - half, x + head - i,
                    y + h // 2 + half + (h % 2), colour)


def bullet(img, cx, cy, r, colour, text, scale):
    """A line bullet: a coloured disc with its line's letter or number in white."""
    lc.disc(img, cx, cy, r, colour)
    lc.draw_text_centred(img, text, cx, int(round(cy - 2.5 * scale)), WHITE, scale)


def domes(img, region, base, dome, rng, pitch=8, r=2.9, jitter=6):
    """Truncated domes on a square grid, pitch texels apart, centred in their cells so the grid
    tiles and turns: each lit from the top left with a flat top and a shadow at the bottom
    right, every dome a little different."""
    x0, y0, x1, y1 = region
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = lc.clamp(tuple(v + rng.uniform(-4, 4) for v in base)) + (255,)
    for cy in range(y0 + pitch // 2, y1, pitch):
        for cx in range(x0 + pitch // 2, x1, pitch):
            d = rng.uniform(-jitter, jitter)
            for y in range(cy - pitch // 2, cy + pitch // 2):
                for x in range(cx - pitch // 2, cx + pitch // 2):
                    if not (y0 <= y < y1 and x0 <= x < x1):
                        continue
                    dx, dy = x + 0.5 - cx, y + 0.5 - cy
                    dist = math.hypot(dx, dy)
                    if dist <= r:
                        if dist < r * 0.5:
                            k = 1.12
                        else:
                            k = 1.0 - 0.18 * (dx + dy) / r
                        px[x, y] = lc.shade(tuple(v + d for v in dome), k) + (255,)
                    elif dist <= r + 1.0 and dx + dy > 0:
                        px[x, y] = lc.shade(base, 0.8) + (255,)


def bars(img, region, base, bar, rng, pitch=8, width=5, seg=28, period=32, offset=0):
    """Guidance bars running along u, pitch texels apart, each seg long in a period (which
    divides the texture, so a run of blocks is seamless), with a lit top edge and a shadow."""
    x0, y0, x1, y1 = region
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = lc.clamp(tuple(v + rng.uniform(-4, 4) for v in base)) + (255,)
    for row, by in enumerate(range(y0 + (pitch - width) // 2, y1, pitch)):
        d = rng.uniform(-5, 5)
        start = (offset + row * 11) % period
        for x in range(x0, x1):
            along = (x - start) % period
            if along >= seg:
                continue
            for y in range(by, by + width):
                k = 1.0
                if y == by:
                    k = 1.18
                elif y == by + width - 1:
                    k = 0.84
                if along in (0, seg - 1):
                    k *= 0.9
                px[x, y] = lc.shade(tuple(v + d for v in bar), k) + (255,)
            if by + width < y1:
                px[x, by + width] = lc.shade(base, 0.8) + (255,)


# ------------------------------------------------------------------------------------------
# Tactile paving and station tile
# ------------------------------------------------------------------------------------------
TEX = 64    # texels a block on the floor and wall surfaces: 4 a pixel


TACTILE = [
    # id, pattern, base, raised, names
    ("tactile_warning_yellow", "domes", YELLOW, lc.shade(YELLOW, 1.04),
     ("Tactile Paving (Warning, Yellow)", "Taktile Bodenplatte (Warnfeld, Gelb)",
      "Pavimento Táctil (Advertencia, Amarillo)", "Taktila Plattor (Varning, Gul)")),
    ("tactile_warning_grey", "domes", (86, 88, 90), (204, 208, 212),
     ("Tactile Paving (Warning, Grey)", "Taktile Bodenplatte (Warnfeld, Grau)",
      "Pavimento Táctil (Advertencia, Gris)", "Taktila Plattor (Varning, Grå)")),
    ("tactile_guidance_yellow", "bars", YELLOW, lc.shade(YELLOW, 1.04),
     ("Tactile Paving (Guidance, Yellow)", "Taktile Bodenplatte (Leitlinie, Gelb)",
      "Pavimento Táctil (Guía, Amarillo)", "Taktila Plattor (Ledstråk, Gul)")),
    ("tactile_guidance_grey", "bars", (86, 88, 90), (204, 208, 212),
     ("Tactile Paving (Guidance, Grey)", "Taktile Bodenplatte (Leitlinie, Grau)",
      "Pavimento Táctil (Guía, Gris)", "Taktila Plattor (Ledstråk, Grå)")),
]


def tactile(entry, variant):
    reg, pattern, base, raised, _ = entry
    rng = random.Random(sum(map(ord, reg)) * 7 + variant)
    img = Image.new("RGBA", (TEX, TEX))
    if pattern == "domes":
        domes(img, (0, 0, TEX, TEX), base, raised, rng)
    else:
        bars(img, (0, 0, TEX, TEX), base, raised, rng, offset=variant * 13)
    return img


# ------------------------------------------------------------------------------------------
# Sign faces and the other pictures
# ------------------------------------------------------------------------------------------
SIGN = 128   # texels on a sign's texture


def sign_blank(colour, w, h, seed):
    img = grain(SIGN, colour, 2, seed)
    lc.frame(img, 0, 0, w, h, lc.shade(colour, 1.5))
    return img


# the hanging wayfinding signs: a 16 x 6 panel, 128 x 48 texels of its texture
WAY_W, WAY_H = 128, 48


def way_trains(right):
    img = sign_blank(NAVY, WAY_W, WAY_H, 401)
    aw = 24
    if right:
        lc.draw_text(img, "TO", 6, 20, WHITE, 2)
        lc.draw_text(img, "TRAINS", 24, 16, WHITE, 3)
        arrow(img, WAY_W - aw - 4, 13, aw, 22, YELLOW, True)
    else:
        arrow(img, 4, 13, aw, 22, YELLOW, False)
        lc.draw_text(img, "TO", 34, 20, WHITE, 2)
        lc.draw_text(img, "TRAINS", 52, 16, WHITE, 3)
    return img


def way_exit(right):
    img = sign_blank(GRAPHITE, WAY_W, WAY_H, 402)
    aw = 26
    tw = lc.text_width("EXIT", 4)
    if right:
        lc.draw_text(img, "EXIT", 14, 14, YELLOW, 4)
        arrow(img, 14 + tw + 16, 13, aw, 22, YELLOW, True)
    else:
        arrow(img, WAY_W - 14 - tw - 16 - aw, 13, aw, 22, YELLOW, False)
        lc.draw_text(img, "EXIT", WAY_W - 14 - tw, 14, YELLOW, 4)
    return img


LINES = [("1", CITYLINE), ("4", RIVERWAY_ORANGE), ("7", VERDANT), ("E", EMBERLINE)]


def way_lines():
    """The lines this platform serves: four invented line bullets in the agencies' colours."""
    img = sign_blank(GRAPHITE, WAY_W, WAY_H, 403)
    for i, (text, colour) in enumerate(LINES):
        bullet(img, 19 + i * 30, 24, 12.5, colour, text, 3)
    return img


# the platform number sign: a 12 x 10 panel, 128 x 107 texels
NUM_W, NUM_H = 128, 107


def number_face(n):
    img = sign_blank(NAVY, NUM_W, NUM_H, 404)
    lc.rect(img, 3, 3, NUM_W - 3, 25, lc.shade(NAVY, 1.35))
    lc.draw_text_centred(img, "PLATFORM", NUM_W / 2.0, 7, YELLOW, 3)
    lc.draw_text_centred(img, str(n), NUM_W / 2.0, 36, WHITE, 12 if n < 10 else 10)
    return img


# the gap warning: a 14 x 8 panel on the wall, 128 x 73 texels
GAP_W, GAP_H = 128, 73


def gap_sign():
    img = grain(SIGN, YELLOW, 2, 405)
    lc.frame(img, 0, 0, GAP_W, GAP_H, BLACK, 3)
    lc.rect(img, 3, 3, GAP_W - 3, 19, BLACK)
    lc.draw_text_centred(img, "CAUTION", GAP_W / 2.0, 6, YELLOW, 2)
    # the pictogram: a figure striding from the platform over the gap into the car
    lc.rect(img, 8, 60, 20, 64, BLACK)          # the platform
    lc.rect(img, 27, 57, 40, 64, BLACK)         # the car's floor, a step up
    lc.disc(img, 22, 27, 3.2, BLACK)            # head
    line(img, 22, 30, 21, 43, BLACK, 3.2)       # body
    line(img, 21, 43, 15, 59, BLACK, 2.8)       # back leg, on the platform
    line(img, 21, 43, 31, 50, BLACK, 2.8)       # front leg, striding
    line(img, 31, 50, 32, 56, BLACK, 2.6)
    line(img, 22, 33, 15, 40, BLACK, 2.2)       # arms
    line(img, 22, 33, 29, 38, BLACK, 2.2)
    for x in range(21, 27, 2):                  # the gap, hatched
        lc.rect(img, x, 58, x + 1, 64, (120, 96, 16))
    lc.draw_text(img, "STEP OVER", 46, 30, BLACK, 2)
    lc.draw_text(img, "THE GAP", 46, 45, BLACK, 2)
    return img


def clock_dial():
    """A station clock's dial: white, a black minute track and bold hour bars, on a transparent
    square (the rim behind it shows at the corners)."""
    s = 64
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    c = s / 2.0
    lc.disc(img, c, c, 30.5, (26, 28, 30))
    lc.disc(img, c, c, 29.0, (246, 246, 242))
    for m in range(60):
        a = math.radians(m * 6)
        inner = 21.0 if m % 5 == 0 else 25.5
        width = 2.6 if m % 5 == 0 else 1.0
        line(img, c + math.sin(a) * inner, c - math.cos(a) * inner,
             c + math.sin(a) * 27.5, c - math.cos(a) * 27.5, (24, 24, 26), width)
    return img


def help_header():
    """The help point's lit header: HELP POINT in white on green, 7 x 4 units (64 x 37)."""
    img = grain(64, (18, 128, 96), 2, 406)
    lc.draw_text_centred(img, "HELP", 32, 5, WHITE, 3)
    lc.draw_text_centred(img, "POINT", 32, 23, WHITE, 2)
    return img


def help_face():
    """The help point's speaker grille, between its two buttons."""
    img = grain(64, (214, 216, 214), 2, 407)
    for y in range(4, 62, 6):
        for x in range(4, 62, 6):
            lc.rect(img, x, y, x + 4, y + 4, (70, 72, 74))
    return img


def button(colour, glyph):
    img = grain(16, (40, 42, 44), 1, 408 + len(glyph))
    lc.disc(img, 8, 8, 7, lc.shade(colour, 0.7))
    lc.disc(img, 8, 8, 6, colour)
    if glyph == "i":
        lc.rect(img, 7, 4, 9, 6, WHITE)
        lc.rect(img, 7, 7, 9, 12, WHITE)
    else:
        lc.rect(img, 7, 3, 9, 9, WHITE)
        lc.rect(img, 7, 10, 9, 12, WHITE)
    return img


def cctv_lens():
    img = grain(16, (30, 32, 36), 1, 409)
    lc.disc(img, 8, 8, 5.5, (12, 14, 18))
    lc.disc(img, 8, 8, 3.2, (40, 60, 90))
    lc.rect(img, 6, 6, 7, 7, (180, 200, 220))
    return img


def smoked():
    img = grain(16, (44, 48, 56), 3, 410)
    for y in range(3, 7):
        lc.rect(img, 4 + y, y, 7 + y, y + 1, (110, 118, 130))
    return img


def perforated(colour, seed):
    img = grain(16, colour, 3, seed)
    for y in range(1, 16, 2):
        for x in range(y // 2 % 2, 16, 2):
            img.putpixel((x, y), lc.shade(colour, 0.55) + (255,))
    return img


def bag():
    """The clear litter bag: faintly grey, with a crease or two."""
    img = lc.fill((226, 230, 232), 16, 3, 412, alpha=96)
    px = img.load()
    for y in range(16):
        x = (y * 3 // 4 + 5) % 16
        r, g, b, a = px[x, y]
        px[x, y] = (r, g, b, 150)
    return img


def litter():
    img = grain(16, (206, 196, 170), 20, 413)
    rng = random.Random(414)
    px = img.load()
    for _ in range(40):
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = lc.clamp(rng.choice([(200, 60, 50), (60, 90, 160), (240, 240, 236),
                                        (90, 70, 50)])) + (255,)
    return img


def bin_lid():
    img = grain(16, STAINLESS, 3, 415)
    lc.disc(img, 8, 8, 4.5, (24, 26, 28))
    lc.frame(img, 0, 0, 16, 16, lc.shade(STAINLESS, 0.8))
    return img


def lens(colour, glow):
    img = grain(16, colour, 3, 417)
    lc.disc(img, 8, 8, 5, lc.shade(colour, glow))
    return img


# --- station tile ---------------------------------------------------------------------------
TILE_W, TILE_H = 16, 8  # a subway tile in texels: 250 x 125 mm, running bond


def tiles(base, rng, band=None, grout=(190, 190, 184)):
    """Glazed subway tile in running bond, each tile its own shade with a lit top edge, the
    grout kept soft so a wall of it does not shimmer at a distance; `band` a colour gives a frieze
    of two courses (rows 24 to 40) between two dark pencil liners."""
    img = Image.new("RGBA", (TEX, TEX))
    px = img.load()
    for row in range(TEX // TILE_H):
        y0 = row * TILE_H
        colour = band if band is not None and 24 <= y0 < 40 else base
        shift = (TILE_W // 2) * (row % 2)
        for col in range(-1, TEX // TILE_W + 1):
            x0 = col * TILE_W + shift
            d = rng.uniform(-5, 5)
            for y in range(y0, y0 + TILE_H):
                for x in range(x0, x0 + TILE_W):
                    if not 0 <= x < TEX:
                        continue
                    if x == x0 + TILE_W - 1 or y == y0 + TILE_H - 1:
                        c = grout if colour is base else lc.shade(colour, 0.8)
                        px[x, y] = lc.clamp(tuple(v + rng.uniform(-3, 3) for v in c)) + (255,)
                        continue
                    k = 1.07 if y == y0 else (0.96 if y == y0 + TILE_H - 2 else 1.0)
                    px[x, y] = lc.shade(tuple(v + d + rng.uniform(-2, 2) for v in colour),
                                        k) + (255,)
    if band is not None:
        for y in (22, 23, 40, 41):
            for x in range(TEX):
                px[x, y] = lc.clamp(tuple(v + rng.uniform(-3, 3) for v in (40, 42, 46))) + (255,)
    return img


def tile_texture(band=None, seed=0):
    return tiles((236, 236, 228), random.Random(500 + seed), band)


BANDS = [("cityline", CITYLINE, "CITYLINE Teal", "CITYLINE-Petrol", "Verde Azulado CITYLINE",
          "CITYLINE Petrol"),
         ("riverway", RIVERWAY_ORANGE, "RIVERWAY Orange", "RIVERWAY-Orange",
          "Naranja RIVERWAY", "RIVERWAY Orange"),
         ("verdant", VERDANT, "VERDANT Green", "VERDANT-Grün", "Verde VERDANT", "VERDANT Grön"),
         ("emberline", EMBERLINE, "EMBERLINE Red", "EMBERLINE-Rot", "Rojo EMBERLINE",
          "EMBERLINE Röd")]


# --- the number band round a column, the station name signs, the network map -----------------
COLUMN_BAND_W, COLUMN_BAND_H = 64, 30   # 10.6 x 5 units on a 64 px texture


def column_number(n):
    img = grain(64, NAVY, 2, 510)
    lc.rect(img, 0, 0, COLUMN_BAND_W, 1, WHITE)
    lc.rect(img, 0, COLUMN_BAND_H - 1, COLUMN_BAND_W, COLUMN_BAND_H, WHITE)
    lc.draw_text_centred(img, "PLATFORM", 32, 3, YELLOW, 1)
    lc.draw_text_centred(img, str(n), 32, 9, WHITE, 4)
    return img


# BlockStationNameSign: invented names, in this order (its NAME property counts from 1)
STATION_NAMES = ["ALDER PARK", "CIVIC SQUARE", "FOUNDRY ROW", "HARBOR LIGHTS", "KESTREL HILL",
                 "LANTERN QUAY", "MILLSTONE", "ORCHARD END", "SAXTON CROSS", "WILLOW BEND"]
NAME_W, NAME_H = 128, 56   # a 16 x 7 panel


def station_name(name):
    """A porcelain-enamel name panel: white capitals on navy inside a white rule, the name on
    two lines when it has two words."""
    img = grain(SIGN, NAVY, 2, 511)
    lc.frame(img, 0, 0, NAME_W, NAME_H, lc.shade(NAVY, 0.7), 2)
    lc.frame(img, 4, 4, NAME_W - 4, NAME_H - 4, WHITE, 1)
    words = name.split(" ")
    if len(words) == 1:
        lc.draw_text_centred(img, name, NAME_W / 2.0, 20, WHITE, 3)
    else:
        lc.draw_text_centred(img, words[0], NAME_W / 2.0, 10, WHITE, 3)
        lc.draw_text_centred(img, " ".join(words[1:]), NAME_W / 2.0, 31, WHITE, 3)
    return img


MAP_W, MAP_H = 128, 96   # a 16 x 12 board


def network_map():
    """A generic network diagram: four lines in the agencies' colours over a river, stations as
    ticks, interchanges as rings, a "you are here" dot and a legend of line bullets. No place
    names: the city is whatever the player builds."""
    img = grain(SIGN, (244, 242, 234), 2, 512)
    w, h = MAP_W, MAP_H
    # the river
    for x in range(w):
        y = int(58 + 10 * math.sin(x / 19.0))
        lc.rect(img, x, y, x + 1, y + 6, (178, 210, 232))
    lc.rect(img, 88, 18, 116, 38, (204, 228, 190))   # a park
    lc.rect(img, 0, 0, w, 12, NAVY)
    lc.draw_text_centred(img, "NETWORK MAP", w / 2.0, 3, WHITE, 1)
    lines = [(CITYLINE, [(8, 30), (40, 30), (64, 54), (120, 54)]),
             (RIVERWAY_ORANGE, [(24, 84), (24, 66), (64, 54), (64, 20), (100, 20)]),
             (VERDANT, [(8, 76), (44, 76), (64, 54), (92, 82), (120, 82)]),
             (EMBERLINE, [(40, 16), (40, 30), (40, 44), (92, 44), (92, 82)])]
    stops = []
    for colour, pts in lines:
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            line(img, x0, y0, x1, y1, colour, 3.0)
            n = max(1, int(math.hypot(x1 - x0, y1 - y0) // 12))
            for i in range(n + 1):
                stops.append((x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n, colour))
    for x, y, colour in stops:
        lc.disc(img, x, y, 1.8, WHITE)
    for x, y in ((64, 54), (40, 30), (92, 82)):
        lc.disc(img, x, y, 4.2, BLACK)
        lc.disc(img, x, y, 3.0, WHITE)
    lc.disc(img, 40, 44, 3.6, (220, 36, 36))
    lc.disc(img, 40, 44, 1.5, WHITE)
    lc.rect(img, 0, h - 11, w, h, (228, 226, 218))
    for i, (text, colour) in enumerate(LINES):
        bullet(img, 12 + i * 16, h - 6, 4.5, colour, text, 1)
    lc.draw_text(img, "YOU ARE HERE", 74, h - 8, (200, 36, 36), 1)
    lc.disc(img, 70, h - 6, 2.2, (220, 36, 36))
    # the glass over it
    px = img.load()
    for k in (0.28, 0.4):
        for y in range(h):
            x0 = int(w * k + y * 0.5 - h * 0.2)
            for x in range(x0, x0 + 3):
                if 0 <= x < w:
                    r, g, b, a = px[x, y]
                    px[x, y] = lc.clamp((r + (255 - r) * 0.12, g + (255 - g) * 0.12,
                                         b + (255 - b) * 0.12)) + (a,)
    lc.frame(img, 0, 0, w, h, (90, 94, 98), 1)
    return img


# --- the emergency point -------------------------------------------------------------------
EMERG_W, EMERG_H = 98, 128   # a 10 x 13 cabinet front
RED = (190, 30, 28)


def emergency_front():
    img = grain(SIGN, RED, 3, 513)
    w, h = EMERG_W, EMERG_H
    lc.rect(img, 0, 0, w, 22, lc.shade(RED, 0.7))
    lc.draw_text_centred(img, "EMERGENCY", w / 2.0, 6, WHITE, 2)
    lc.draw_text(img, "PHONE", 8, 30, WHITE, 2)
    lc.draw_text(img, "LIFT TO", 8, 44, WHITE, 1)
    lc.draw_text(img, "TALK", 8, 51, WHITE, 1)
    # the glass door over the extinguisher
    lc.rect(img, 6, 62, w - 6, h - 6, (150, 170, 184))
    lc.frame(img, 6, 62, w - 6, h - 6, (230, 230, 226), 2)
    cx = w / 2.0
    lc.rect(img, int(cx - 9), 76, int(cx + 9), 116, (200, 30, 26))
    lc.rect(img, int(cx - 9), 76, int(cx - 6), 116, (230, 80, 70))
    lc.rect(img, int(cx - 5), 70, int(cx + 5), 76, (40, 40, 42))
    line(img, cx + 4, 72, cx + 16, 90, (30, 30, 32), 2.2)
    lc.disc(img, cx, 88, 4, (236, 236, 230))
    lc.draw_text(img, "FIRE", 10, 66, WHITE, 1)
    px = img.load()
    for y in range(64, h - 8):
        x0 = int(20 + (y - 64) * 0.4)
        for x in range(x0, x0 + 3):
            r, g, b, a = px[x, y]
            px[x, y] = lc.clamp((r + 30, g + 30, b + 30)) + (a,)
    return img


# --- the validator's screen -----------------------------------------------------------------
def validator_screen(state):
    img = grain(16, (22, 24, 28), 1, 514 + state)
    if state == 0:
        lc.rect(img, 4, 5, 12, 11, (170, 176, 184))
        lc.rect(img, 5, 6, 8, 8, (90, 150, 200))
    elif state == 1:
        lc.rect(img, 1, 1, 15, 15, (30, 160, 70))
        line(img, 4, 8.5, 7, 11.5, WHITE, 1.6)
        line(img, 7, 11.5, 12.5, 4.5, WHITE, 1.6)
    else:
        lc.rect(img, 1, 1, 15, 15, (200, 36, 30))
        line(img, 4.5, 4.5, 11.5, 11.5, WHITE, 1.6)
        line(img, 11.5, 4.5, 4.5, 11.5, WHITE, 1.6)
    return img


def validator_target():
    """The reader's face: a card with contactless waves on a dark ring."""
    img = grain(16, (36, 38, 42), 1, 518)
    lc.disc(img, 8, 8, 7, (60, 64, 70))
    lc.disc(img, 8, 8, 6, (14, 94, 111))
    for x0, h in ((5, 2), (7, 4), (9, 6), (11, 8)):
        lc.rect(img, x0, 8 - h // 2, x0 + 1, 8 + h // 2, WHITE)
    return img


def register_textures():
    tex = {}
    for t in TACTILE:
        tex[t[0]] = (lambda t=t: tactile(t, 0))
        tex[t[0] + "_b"] = (lambda t=t: tactile(t, 1))
    tex["tile_white"] = lambda: tile_texture()
    for bid, colour, *_ in BANDS:
        tex["tile_band_" + bid] = (lambda c=colour, b=bid: tile_texture(c, len(b)))
    tex.update({
        "steel": lambda: grain(16, STEEL, 3, 420),
        "stainless": lambda: grain(16, STAINLESS, 3, 421),
        "graphite": lambda: grain(16, GRAPHITE, 2, 422),
        "white": lambda: grain(16, (226, 228, 226), 2, 423),
        "black": lambda: grain(16, (30, 32, 34), 2, 424),
        "seat": lambda: perforated((186, 190, 194), 425),
        "pad": lambda: grain(16, (38, 40, 44), 2, 426),
        "way_trains": lambda: way_trains(True),
        "way_trains_back": lambda: way_trains(False),
        "way_exit": lambda: way_exit(True),
        "way_exit_back": lambda: way_exit(False),
        "way_lines": way_lines,
        "gap_sign": gap_sign,
        "clock_dial": clock_dial,
        "help_header": help_header,
        "help_face": help_face,
        "help_body": lambda: grain(16, (54, 60, 66), 2, 427),
        "button_info": lambda: button((30, 150, 70), "i"),
        "button_sos": lambda: button((200, 36, 30), "!"),
        "cctv_lens": cctv_lens,
        "smoked": smoked,
        "bag": bag,
        "litter": litter,
        "bin_lid": bin_lid,
        "column_paint": lambda: grain(16, (62, 92, 78), 4, 430),
        "plinth": lambda: grain(16, (70, 72, 76), 5, 431),
        "roof": lambda: grain(16, (128, 132, 134), 5, 432),
        "soffit": lambda: grain(16, (218, 220, 216), 2, 433),
        "fascia": lambda: canopy_fascia(),
        "lamp_on": lambda: lens((250, 246, 226), 1.05),
        "network_map": network_map,
        "emergency_front": emergency_front,
        "emergency_body": lambda: grain(16, lc.shade(RED, 0.85), 3, 434),
        "validator_target": validator_target,
    })
    for s, name in enumerate(("idle", "ok", "no")):
        tex["validator_" + name] = (lambda s=s: validator_screen(s))
    for n in range(1, 21):
        tex["number_%d" % n] = (lambda n=n: number_face(n))
        tex["column_number_%d" % n] = (lambda n=n: column_number(n))
    for i, name in enumerate(STATION_NAMES):
        tex["station_name_%d" % (i + 1)] = (lambda name=name: station_name(name))
    for name, draw in tex.items():
        C.texture(name)(draw)


def canopy_fascia():
    """The canopy's fascia: graphite steel with a white rule along it."""
    img = grain(16, (58, 62, 66), 2, 435)
    lc.rect(img, 0, 11, 16, 12, (228, 228, 224))
    return img


# ------------------------------------------------------------------------------------------
# Elements
# ------------------------------------------------------------------------------------------
def _span(a, b):
    """A face's extent along one axis as uv, moved by whole blocks into 0..16; a span longer
    than a block is squeezed into the sprite."""
    shift = 16 * (min(a, b) // 16)
    if max(a, b) - shift > 16:
        return 0.0, min(16.0, abs(b - a))
    return a - shift, b - shift


def _uv(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    vy = _span(16 - y1, 16 - y0)
    if face == "north":
        u = _span(16 - x1, 16 - x0)
    elif face == "south":
        u = _span(x0, x1)
    elif face == "east":
        u = _span(16 - z1, 16 - z0)
    elif face == "west":
        u = _span(z0, z1)
    elif face == "up":
        u, vy = _span(x0, x1), _span(z0, z1)
    else:
        u, vy = _span(x0, x1), _span(16 - z1, 16 - z0)
    uv = [u[0], vy[0], u[1], vy[1]]
    if uv[0] == uv[2]:
        uv[2] = uv[0] + 0.01
    if uv[1] == uv[3]:
        uv[3] = uv[1] + 0.01
    return [round(v, 4) for v in uv]


def B(frm, to, tex, faces=ALL, per=None, uv=None, cull=None, rot=None, shade=True):
    """A box: faces with uv from their place (or given), `per` gives faces another texture,
    `cull` a cullface per face, `rot` an element rotation (axis, angle, origin)."""
    out = {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": {}}
    for f in faces:
        u = (uv or {}).get(f) or _uv(f, frm, to)
        face = {"texture": "#" + (per or {}).get(f, tex), "uv": [round(v, 4) for v in u]}
        if cull and f in cull:
            face["cullface"] = cull[f]
        out["faces"][f] = face
    if rot:
        out["rotation"] = {"origin": [round(v, 4) for v in rot[2]], "axis": rot[0],
                           "angle": rot[1]}
    if not shade:
        out["shade"] = False
    return out


def win(w_px, h_px, size):
    """The uv of a picture window w_px x h_px texels at the top-left of a size-px texture."""
    return [0, 0, round(w_px * 16.0 / size, 4), round(h_px * 16.0 / size, 4)]


def flip(uv):
    """The same window read mirrored, for a face whose u runs the other way."""
    return [uv[2], uv[1], uv[0], uv[3]]


def octagon_y(cx, cz, r, y0, y1, tex, top=True, bottom=True):
    return lc.post(cx, cz, r, y0, y1, tex, top=top, bottom=bottom)


def octagon_z(cx, cy, r, z0, z1, tex, front=True, back=True):
    return lc._octagon("z", cx, cy, r, z0, z1, tex, front or back, cap_front=front,
                       cap_back=back)


def rod(x, z, y0, y1, tex="steel", r=0.35):
    return B((x - r, y0, z - r), (x + r, y1, z + r), tex, SIDES)


def ceiling_plate(x, z, w=1.0):
    return B((x - w, 15.6, z - w), (x + w, 16, z + w), "steel", SIDES + ("down",))


FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def names_of(*n):
    assert len(n) == 4
    return n


def multipart(parts, prefix, textures, facings=True):
    """Models and multipart rules for parts [(name, conditions, elements)]; with facings, each
    part is drawn turned to each of the four ways (conditions over actual-state booleans or
    other properties' values)."""
    models = {}
    rules = []
    for name, cond, els in parts:
        mname = "%s_%s" % (prefix, name)
        if isinstance(els, str):          # a child model of another part, named here
            models[mname] = els_child(els, textures)
        else:
            models[mname] = lc.model(textures, els)
        for f, y in (FACINGS if facings else ((None, 0),)):
            when = {"facing": f} if f else {}
            for k, v in cond.items():
                when[k] = ("true" if v else "false") if isinstance(v, bool) else str(v)
            apply = {"model": C.M(mname)}
            if y:
                apply["y"] = y
            # a part drawn always has no "when": an empty one is a parse error in game
            rules.append({"when": when, "apply": apply} if when else {"apply": apply})
    return models, {"multipart": rules}


def els_child(parent, textures):
    return {"parent": "csm:block/" + C.M(parent).split(":", 1)[1], "textures": textures}


def item_of(parts, textures, keep=lambda cond: all(v is False for v in cond.values()),
            display=None):
    els = []
    for name, cond, e in parts:
        if keep(cond) and not isinstance(e, str):
            els += e
    m = lc.model(textures, els)
    if display:
        m["display"] = display
    return m


# ------------------------------------------------------------------------------------------
# Blocks
# ------------------------------------------------------------------------------------------
def tactile_models():
    for reg, pattern, _, _, names in TACTILE:
        models = {}
        for key in (reg, reg + "_b"):
            el = B((0, 0, 0), (16, 1, 16), "floor", ALL, uv={"up": [0, 0, 16, 16]},
                   cull={"down": "down"})
            models[key] = {"parent": "block/thin_block",
                           "textures": {"floor": C.T(key), "particle": C.T(key)},
                           "elements": [el]}

        def pick(turns):
            out = []
            for key in (reg, reg + "_b"):
                for r in turns:
                    v = {"model": C.M(key)}
                    if r:
                        v["y"] = r
                    out.append(v)
            return out
        if pattern == "bars":
            # the bars run the way the player faced when laying them, turned only end for end
            x, z = pick((0, 180)), pick((90, 270))
        else:
            x = z = pick((0, 90, 180, 270))
        state = {"variants": {"axis=x": x, "axis=z": z, "inventory": {"model": C.M(reg)}}}
        C.add(reg, 'new BlockTactilePaving("%s")' % reg, names, models, state, tab=TAB)


def station_tiles():
    names = names_of("Station Wall Tile (White)", "Bahnhofs-Wandfliese (Weiß)",
                     "Azulejo de Estación (Blanco)", "Stationskakel (Vit)")
    reg = "station_tile_white"
    m = {"parent": "block/cube_all", "textures": {"all": C.T("tile_white")}}
    C.add(reg, 'new BlockStationTile("%s")' % reg, names, {reg: m},
          {"variants": {"normal": {"model": C.M(reg)}, "inventory": {"model": C.M(reg)}}},
          tab=TAB)
    for bid, _, en, de, es, sv in BANDS:
        reg = "station_tile_band_" + bid
        m = {"parent": "block/cube_bottom_top",
             "textures": {"side": C.T("tile_band_" + bid), "top": C.T("tile_white"),
                          "bottom": C.T("tile_white")}}
        C.add(reg, 'new BlockStationTile("%s")' % reg,
              names_of("Station Wall Tile (Band, %s)" % en,
                       "Bahnhofs-Wandfliese (Fries, %s)" % de,
                       "Azulejo de Estación (Franja, %s)" % es,
                       "Stationskakel (Bård, %s)" % sv),
              {reg: m},
              {"variants": {"normal": {"model": C.M(reg)}, "inventory": {"model": C.M(reg)}}},
              tab=TAB)


# --- the bench and the perch ----------------------------------------------------------------
SEATS = ((1.5, 7.2), (8.8, 14.5))


def armrest(x):
    """An armrest centred on x: a stanchion at the front and at the back, a padded top."""
    return [B((x - 0.4, 7.75, 4.2), (x + 0.4, 10.4, 5.0), "steel", SIDES),
            B((x - 0.4, 5.5, 8.6), (x + 0.4, 10.4, 9.8), "steel", SIDES),
            B((x - 0.6, 10.4, 3.6), (x + 0.6, 11.1, 10.0), "pad")]


def bench_parts():
    F, T = False, True
    beam_faces = ("north", "south", "up", "down")
    body = [B((1, 4, 8.5), (15, 5.5, 10), "steel", beam_faces),
            B((7, 0, 8.25), (9, 4, 10.25), "steel", SIDES),
            B((6, 0, 7), (10, 0.5, 11.5), "steel", SIDES + ("up",))]
    for x0, x1 in SEATS:
        xc = (x0 + x1) / 2
        body += [B((x0, 7, 3), (x1, 7.75, 10), "seat"),
                 B((xc - 1, 5.5, 5.5), (xc + 1, 7, 10), "steel", SIDES + ("down",)),
                 B((x0, 8.2, 10.1), (x1, 15, 10.85), "seat",
                   rot=("x", -22.5, (xc, 8.2, 10.1)))]
    body += armrest(8)   # the middle armrest, between the block's two seats
    return [("body", {}, body),
            ("join_left", {"left": T}, [B((0, 4, 8.5), (1, 5.5, 10), "steel", beam_faces)]
             + armrest(0)),
            ("join_right", {"right": T}, [B((15, 4, 8.5), (16, 5.5, 10), "steel",
                                            beam_faces)]),
            ("end_left", {"left": F}, [B((0.5, 4, 8.5), (1, 5.5, 10), "steel",
                                         beam_faces + ("west",))] + armrest(0.8)),
            ("end_right", {"right": F}, [B((15, 4, 8.5), (15.5, 5.5, 10), "steel",
                                           beam_faces + ("east",))] + armrest(15.2))]


def perch_parts():
    F, T = False, True
    rail_faces = ("north", "south", "up", "down")
    pad_rot = ("x", -22.5, (8, 11, 8.5))

    def pad(x0, x1, faces=rail_faces):
        return B((x0, 10.5, 6), (x1, 11.6, 11.5), "pad", faces, rot=pad_rot)

    def rail(x0, x1, faces=rail_faces):
        return B((x0, 9.2, 8.2), (x1, 10.1, 9.2), "stainless", faces)
    body = [pad(1, 15), rail(1, 15),
            B((7.1, 0, 8.1), (8.9, 9.2, 9.9), "steel", SIDES),
            B((6.2, 0, 7), (9.8, 0.5, 11), "steel", SIDES + ("up",))]
    return [("body", {}, body),
            ("join_left", {"left": T}, [pad(0, 1), rail(0, 1)]),
            ("join_right", {"right": T}, [pad(15, 16), rail(15, 16)]),
            ("end_left", {"left": F}, [pad(0.4, 1, rail_faces + ("west",)),
                                       rail(0.6, 1, rail_faces + ("west",))]),
            ("end_right", {"right": F}, [pad(15, 15.6, rail_faces + ("east",)),
                                         rail(15, 15.4, rail_faces + ("east",))])]


def seating():
    tex = {"steel": C.T("steel"), "seat": C.T("seat"), "pad": C.T("pad"),
           "particle": C.T("seat")}
    parts = bench_parts()
    models, state = multipart(parts, "platform_bench", tex)
    C.add("platform_bench",
          'new BlockPlatformBench("platform_bench", new double[]{0, 0, 3, 16, 15, 11})',
          names_of("Platform Bench", "Bahnsteigbank", "Banco de Andén", "Plattformsbänk"),
          models, state, item=item_of(parts, tex), tab=TAB)
    parts = perch_parts()
    tex = {"steel": C.T("steel"), "pad": C.T("pad"), "stainless": C.T("stainless"),
           "particle": C.T("pad")}
    models, state = multipart(parts, "platform_perch", tex)
    C.add("platform_perch",
          'new BlockPlatformRun("platform_perch", new double[]{0, 0, 6, 16, 12, 12})',
          names_of("Platform Perch", "Bahnsteig-Anlehnbank", "Apoyo Isquiático de Andén",
                   "Ståstödsbänk"),
          models, state, item=item_of(parts, tex), tab=TAB)


# --- fixtures with a plain facing blockstate -----------------------------------------------
def fixture(reg, java, names, els, textures, ao=True, display=None, extra_state=None):
    m = lc.model(textures, els, ao=ao)
    if display:
        m["display"] = display
    C.add(reg, java, names, {reg: m}, lc.facing_state(C.M(reg), extra_state), tab=TAB)


def gui_display(scale=0.625, y=0.0, rot=(30, 225, 0)):
    return {
        "gui": {"rotation": list(rot), "translation": [0, y, 0], "scale": [scale] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, y, 0], "scale": [scale] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.375] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                  "scale": [0.4] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                 "scale": [0.4] * 3},
    }


def box_java(box):
    return "new double[]{%s}" % ", ".join(("%g" % v) for v in box)


# BlockPlatformHelpPoint: the column's box, and the height between its two buttons (in
# sixteenths): a click on the front above it is the information button, below it emergency
HELP_BOX = (4, 0, 5, 12, 26, 11)
HELP_SPLIT_Y = 12.6


def help_point():
    els = [B((4, 0, 5), (12, 0.5, 11), "body", SIDES + ("up",)),
           B((5, 0.5, 6), (11, 22, 10), "body", SIDES),
           # the lit header over the column
           B((4.5, 22, 5.5), (11.5, 26, 10.5), "body", ALL, per={"north": "header"},
             uv={"north": win(64, 37, 64)}),
           B((4.3, 26, 5.3), (11.7, 26.4, 10.7), "body"),
           # the front panel, all within the lower block (a click reaches only the block the
           # ray passes through): the information button over a speaker grille, the guarded
           # emergency button under it, a plain panel up to the header
           B((5.5, 6.2, 5.6), (10.5, 21.5, 6), "body", ("north", "east", "west", "up", "down")),
           B((5.9, 9.8, 5.5), (10.1, 12.4, 5.6), "face", ("north",), uv={"north": [0, 0, 16, 16]}),
           B((6.5, 12.8, 5.2), (9.5, 15.6, 5.6), "info", ("north", "east", "west", "up", "down"),
             uv={"north": [0, 0, 16, 16]}),
           B((7, 7.2, 5.3), (9, 9.2, 5.6), "sos", ("north", "east", "west", "up", "down"),
             uv={"north": [0, 0, 16, 16]}),
           B((6.6, 6.8, 5.1), (9.4, 7.2, 5.6), "body", ("north", "up", "down", "east", "west")),
           B((6.6, 9.2, 5.1), (9.4, 9.6, 5.6), "body", ("north", "up", "down", "east", "west")),
           # a small camera lens over the information button
           B((7.4, 20.7, 5.35), (8.6, 21.3, 5.6), "lens", ("north",), uv={"north": [0, 0, 16, 16]})]
    tex = {"body": C.T("help_body"), "header": C.T("help_header"), "face": C.T("help_face"),
           "info": C.T("button_info"), "sos": C.T("button_sos"), "lens": C.T("cctv_lens"),
           "particle": C.T("help_body")}
    fixture("platform_help_point",
            'new BlockPlatformHelpPoint("platform_help_point", %s, false)' % box_java(HELP_BOX),
            names_of("Help Point", "Notruf- und Infosäule", "Punto de Ayuda", "Hjälppunkt"),
            els, tex, ao=False, display=gui_display(0.42, -2.0))


def emergency_point():
    """A red wall cabinet: EMERGENCY lit over a phone handset and, behind glass, an
    extinguisher. A click is an emergency call, like the help point's red button."""
    reg = "station_emergency_point"
    front = win(EMERG_W, EMERG_H, SIGN)
    els = [B((3, 1, 13), (13, 14, 16), "body", ALL, per={"north": "front"},
             uv={"north": front}),
           # the handset on its hook, and the cord
           B((4.2, 8.6, 12.4), (6.0, 11.6, 13), "black"),
           B((4.5, 9.2, 12.1), (5.7, 11.0, 12.4), "black", ("north", "east", "west", "up",
                                                             "down")),
           # the glass door's handle
           B((11.4, 3.5, 12.7), (11.9, 6.0, 13), "white", ("north", "east", "west", "up",
                                                            "down"))]
    tex = {"body": C.T("emergency_body"), "front": C.T("emergency_front"),
           "black": C.T("black"), "white": C.T("white"), "particle": C.T("emergency_body")}
    fixture(reg, 'new BlockPlatformHelpPoint("%s", %s, true)' % (reg, box_java((3, 1, 12, 13,
                                                                                14, 16))),
            names_of("Station Emergency Point", "Bahnhofs-Notfallpunkt",
                     "Punto de Emergencia de Estación", "Nödpunkt för Station"),
            els, tex, display=gui_display(0.7, 0.0, (0, 180, 0)))


def cctv():
    # a dome hung from the ceiling on a short pipe
    els = [ceiling_plate(8, 8, 1.4), rod(8, 8, 11.2, 15.6, "white", 0.5),
           B((5.4, 10.2, 5.4), (10.6, 11.2, 10.6), "white")]
    els += octagon_y(8, 8, 2.5, 8.9, 10.2, "smoked", top=False, bottom=False)
    els += octagon_y(8, 8, 2.0, 8.1, 8.9, "smoked", top=False, bottom=False)
    els += octagon_y(8, 8, 1.2, 7.6, 8.1, "smoked", top=False, bottom=True)
    tex = {"white": C.T("white"), "steel": C.T("steel"), "smoked": C.T("smoked"),
           "particle": C.T("white")}
    fixture("platform_cctv_dome",
            'new BlockPlatformFixture("platform_cctv_dome", new double[]{5, 7, 5, 11, 16, 11})',
            names_of("CCTV Dome Camera", "Kuppelkamera (Videoüberwachung)",
                     "Cámara Domo de CCTV", "Kupolkamera (Övervakning)"),
            els, tex, display=gui_display(0.8, -3.0))
    # a bullet camera on a wall bracket, looking down the platform
    tilt = ("x", -22.5, (8, 11.5, 9))
    els = [B((6.5, 9, 15.5), (9.5, 14, 16), "white", SIDES + ("up", "down")),
           B((7.5, 11, 9), (8.5, 12, 15.5), "white", ("north", "east", "west", "up", "down")),
           B((6.6, 9.6, 1.5), (9.4, 12.4, 9.4), "white", ALL, per={"north": "lens"},
             uv={"north": [0, 0, 16, 16]}, rot=tilt),
           B((6.3, 12.4, 0.8), (9.7, 12.9, 9.6), "white", rot=tilt)]
    tex = {"white": C.T("white"), "lens": C.T("cctv_lens"), "particle": C.T("white")}
    fixture("platform_cctv_camera",
            'new BlockPlatformFixture("platform_cctv_camera", new double[]{6, 8, 0, 10, 14, 16})',
            names_of("CCTV Camera (Wall Bracket)", "Überwachungskamera (Wandarm)",
                     "Cámara de CCTV (Soporte de Pared)", "Övervakningskamera (Väggfäste)"),
            els, tex, display=gui_display(0.7, -1.0))


def hanging_panel(x0, x1, y0, y1, face_uv, back_uv=None, rods=(3.0, 13.0), z0=7.3, z1=8.7):
    """A sign hung from the ceiling on two rods: the panel's front (north) and back (south) are
    the art, the rest its edge."""
    # a south face's u already runs left to right as seen from behind, so the same window
    # reads the right way round there without mirroring
    uvs = {"north": face_uv, "south": back_uv or face_uv}
    per = {"north": "face", "south": "back" if back_uv else "face"}
    els = [B((x0, y0, z0), (x1, y1, z1), "edge", ALL, per=per, uv=uvs)]
    for x in rods:
        els += [rod(x, 8, y1, 15.6), ceiling_plate(x, 8, 0.8)]
    return els


def hanging_signs():
    specs = [
        ("platform_sign_to_trains", "way_trains", "way_trains_back",
         names_of("Hanging Sign (To Trains)", "Hängeschild (Zu den Zügen)",
                  "Letrero Colgante (A los Trenes)", "Hängande Skylt (Till Tågen)")),
        ("platform_sign_exit", "way_exit", "way_exit_back",
         names_of("Hanging Sign (Exit)", "Hängeschild (Ausgang)", "Letrero Colgante (Salida)",
                  "Hängande Skylt (Utgång)")),
        ("platform_sign_lines", "way_lines", None,
         names_of("Hanging Sign (Line Bullets)", "Hängeschild (Liniensymbole)",
                  "Letrero Colgante (Símbolos de Línea)", "Hängande Skylt (Linjesymboler)")),
    ]
    face = win(WAY_W, WAY_H, SIGN)
    for reg, front, back, names in specs:
        # the back face shows the same window, which reads the right way round from behind; a
        # sign with an arrow has its own back art, the arrow turned to point the same way in the
        # world
        els = hanging_panel(0, 16, 5, 11, face, face if back else None)
        tex = {"face": C.T(front), "edge": C.T("graphite"), "steel": C.T("steel"),
               "particle": C.T(front)}
        if back:
            tex["back"] = C.T(back)
        fixture(reg, 'new BlockPlatformFixture("%s", new double[]{0, 5, 7, 16, 16, 9})' % reg,
                names, els, tex, display=gui_display(0.6, 0.0))


def number_sign():
    reg = "platform_number_sign"
    face = win(NUM_W, NUM_H, SIGN)
    els = hanging_panel(2, 14, 1, 11, face, rods=(4.0, 12.0))
    tex = {"face": C.T("number_1"), "edge": C.T("graphite"), "steel": C.T("steel"),
           "particle": C.T("number_1")}
    numbers = {str(n): {"textures": {"face": C.T("number_%d" % n)}} for n in range(1, 21)}
    fixture(reg, 'new BlockPlatformNumberSign("%s", new double[]{2, 1, 7, 14, 16, 9})' % reg,
            names_of("Platform Number Sign", "Gleisnummernschild", "Letrero de Número de Andén",
                     "Spårnummerskylt"),
            els, tex, display=gui_display(0.6, 1.0), extra_state={"number": numbers})


def station_name_sign():
    reg = "station_name_sign"
    els = [B((0, 5, 15.2), (16, 12, 16), "edge", ALL, per={"north": "face"},
             uv={"north": win(NAME_W, NAME_H, SIGN)})]
    tex = {"face": C.T("station_name_1"), "edge": C.T("graphite"),
           "particle": C.T("station_name_1")}
    names = {str(i + 1): {"textures": {"face": C.T("station_name_%d" % (i + 1))}}
             for i in range(len(STATION_NAMES))}
    fixture(reg, 'new BlockStationNameSign("%s", new double[]{0, 5, 15, 16, 12, 16})' % reg,
            names_of("Station Name Sign", "Stationsnamensschild", "Letrero de Nombre de Estación",
                     "Stationsnamnskylt"),
            els, tex, display=gui_display(0.8, 0.0, (0, 180, 0)), extra_state={"name": names})


def network_map_board():
    reg = "station_network_map"
    els = [B((0.5, 2, 15), (15.5, 14, 16), "edge", ALL, per={"north": "face"},
             uv={"north": win(MAP_W, MAP_H, SIGN)}),
           B((0, 1.5, 14.6), (16, 2, 16), "edge"), B((0, 14, 14.6), (16, 14.5, 16), "edge"),
           B((0, 2, 14.6), (0.5, 14, 16), "edge", ("north", "west", "south", "east")),
           B((15.5, 2, 14.6), (16, 14, 16), "edge", ("north", "east", "south", "west"))]
    tex = {"face": C.T("network_map"), "edge": C.T("stainless"),
           "particle": C.T("network_map")}
    fixture(reg, 'new BlockPlatformFixture("%s", new double[]{0, 1, 14, 16, 15, 16})' % reg,
            names_of("Network Map Board", "Liniennetzplan", "Plano de la Red",
                     "Linjenätskarta"),
            els, tex, display=gui_display(0.8, 0.0, (0, 180, 0)))


def gap_wall_sign():
    reg = "platform_gap_sign"
    els = [B((1, 4, 15.2), (15, 12, 16), "edge", ALL, per={"north": "face"},
             uv={"north": win(GAP_W, GAP_H, SIGN)})]
    tex = {"face": C.T("gap_sign"), "edge": C.T("graphite"), "particle": C.T("gap_sign")}
    fixture(reg, 'new BlockPlatformFixture("%s", new double[]{1, 4, 15, 15, 12, 16})' % reg,
            names_of("Gap Warning Sign", "Warnschild (Spalt)", "Señal de Aviso de Hueco",
                     "Varningsskylt (Glipa)"),
            els, tex, display=gui_display(0.8, 0.0, (0, 180, 0)))


# BlockPlatformClock / TileEntityPlatformClockRenderer: the dial's middle and its two faces
CLOCK_CX, CLOCK_CY, CLOCK_R = 8.0, 8.0, 4.2
CLOCK_Z0, CLOCK_Z1 = 6.6, 9.4       # the drum
DIAL_FRONT, DIAL_BACK = 6.3, 9.7    # the dials, a little proud of the drum's ends


def clock():
    reg = "platform_clock"
    dial = [0, 0, 16, 16]
    els = octagon_z(CLOCK_CX, CLOCK_CY, CLOCK_R, CLOCK_Z0, CLOCK_Z1, "rim")
    d = 3.95
    els += [B((CLOCK_CX - d, CLOCK_CY - d, DIAL_FRONT), (CLOCK_CX + d, CLOCK_CY + d, DIAL_FRONT),
              "dial", ("north",), uv={"north": dial}),
            B((CLOCK_CX - d, CLOCK_CY - d, DIAL_BACK), (CLOCK_CX + d, CLOCK_CY + d, DIAL_BACK),
              "dial", ("south",), uv={"south": dial}),
            B((7.4, CLOCK_CY + CLOCK_R - 0.3, 7.4), (8.6, 14.5, 8.6), "rim", SIDES),
            B((6.5, 14.5, 7), (9.5, 15.6, 9), "rim", SIDES + ("down",)),
            ceiling_plate(8, 8, 1.6)]
    tex = {"rim": C.T("black"), "dial": C.T("clock_dial"), "steel": C.T("steel"),
           "particle": C.T("black")}
    fixture(reg, 'new BlockPlatformClock("%s", new double[]{3, 3, 6, 13, 16, 10})' % reg,
            names_of("Platform Clock", "Bahnsteiguhr", "Reloj de Andén", "Perrongklocka"),
            els, tex, display=gui_display(0.8, 0.0, (0, 180, 0)))


def litter_bin():
    """A clear-bag litter bin, as stations use so nothing can be hidden in it: a steel hoop on
    a post holds a clear bag, under a lid ring with a round opening."""
    reg = "platform_litter_bin"
    els = [B((6.5, 0, 12.2), (9.5, 0.4, 15.4), "stainless", SIDES + ("up",)),
           B((7.3, 0.4, 12.9), (8.7, 14.2, 14.3), "stainless", SIDES + ("up",)),
           # the bag, see-through, with what is in it
           B((4.4, 1.5, 4.4), (11.6, 13.2, 11.6), "bag", ("north", "south", "east", "west",
                                                          "down")),
           B((5.0, 1.6, 5.0), (11.0, 4.6, 11.0), "litter", ("north", "south", "east", "west",
                                                            "up")),
           # the hoop and the lid ring
           B((4.0, 12.8, 4.0), (12.0, 13.6, 12.0), "stainless", SIDES + ("down",)),
           B((3.8, 13.6, 3.8), (12.2, 14.2, 12.2), "stainless", ALL, per={"up": "lid"},
             uv={"up": [0, 0, 16, 16]}),
           B((7.3, 12.8, 12.0), (8.7, 13.6, 12.9), "stainless", ("east", "west", "up",
                                                                  "down"))]
    tex = {"stainless": C.T("stainless"), "bag": C.T("bag"), "litter": C.T("litter"),
           "lid": C.T("bin_lid"), "particle": C.T("stainless")}
    fixture(reg, 'new BlockPlatformFixture("%s", new double[]{4, 0, 4, 12, 14, 15}, true)' % reg,
            names_of("Platform Litter Bin (Clear Bag)",
                     "Bahnsteig-Abfallbehälter (Klarsichtbeutel)",
                     "Papelera de Andén (Bolsa Transparente)",
                     "Papperskorg för Plattform (Genomskinlig Påse)"),
            els, tex, display=gui_display(0.7, 0.0))


# --- columns -------------------------------------------------------------------------------
def column_parts(style):
    """A column one block of shaft at a time: a plinth where no column stands below, a
    capital where none stands above (BlockPlatformColumn's `down` and `up`)."""
    F = False
    if style == "steel":
        shaft = octagon_y(8, 8, 3.0, 0, 16, "paint", top=False, bottom=False)
        base = [B((4, 0, 4), (12, 0.6, 12), "plinth", SIDES + ("up",))] + \
            octagon_y(8, 8, 3.6, 0.6, 2.0, "paint", top=True, bottom=False)
        cap = [B((4.2, 14.8, 4.2), (11.8, 16, 11.8), "paint", SIDES + ("down",))] + \
            octagon_y(8, 8, 3.5, 13.8, 14.8, "paint", top=False, bottom=True)
    else:
        shaft = [B((3, 0, 3), (13, 16, 13), "tile", SIDES)]
        base = [B((2.6, 0, 2.6), (13.4, 1.8, 13.4), "plinth", SIDES + ("up",))]
        cap = [B((2.6, 14.6, 2.6), (13.4, 16, 13.4), "plinth", SIDES + ("down",))]
    return [("shaft", {}, shaft), ("base", {"down": F}, base), ("cap", {"up": F}, cap)]


BAND_Y0, BAND_Y1 = 8.0, 13.0


def columns():
    for style, reg, names in (
            ("tile", "platform_column_tile",
             names_of("Platform Column (Tiled)", "Bahnsteigstütze (Gefliest)",
                      "Columna de Andén (Alicatada)", "Plattformspelare (Kaklad)")),
            ("steel", "platform_column_steel",
             names_of("Platform Column (Steel)", "Bahnsteigstütze (Stahl)",
                      "Columna de Andén (Acero)", "Plattformspelare (Stål)"))):
        tex = {"tile": C.T("tile_white"), "plinth": C.T("plinth"), "paint": C.T("column_paint"),
               "particle": C.T("tile_white" if style == "tile" else "column_paint")}
        parts = column_parts(style)
        models, state = multipart(parts, reg, tex)
        box = (2.5, 0, 2.5, 13.5, 16, 13.5) if style == "tile" else (4, 0, 4, 12, 16, 12)
        C.add(reg, 'new BlockPlatformColumn("%s", %s)' % (reg, box_java(box)), names, models,
              state, item=item_of(parts, tex), tab=TAB)
    # the tiled column with a platform number band round it, clicked to step its number
    reg = "platform_column_number"
    tex = {"tile": C.T("tile_white"), "plinth": C.T("plinth"), "band": C.T("column_number_1"),
           "edge": C.T("graphite"), "particle": C.T("tile_white")}
    parts = column_parts("tile")
    band_uv = win(COLUMN_BAND_W, COLUMN_BAND_H, 64)
    band = [B((2.7, BAND_Y0, 2.7), (13.3, BAND_Y1, 13.3), "band", ALL,
              per={"up": "edge", "down": "edge"},
              uv={f: band_uv for f in SIDES})]
    parts.append(("band_1", {"number": 1}, band))
    for n in range(2, 21):
        parts.append(("band_%d" % n, {"number": n}, "platform_column_number_band_1"))
    child = {}
    models, state = multipart([p for p in parts if not isinstance(p[2], str)], reg, tex)
    for name, cond, parent in parts:
        if isinstance(parent, str):
            n = cond["number"]
            mname = "%s_%s" % (reg, name)
            models[mname] = {"parent": "csm:block/" + C.M(parent).split(":", 1)[1],
                             "textures": {"band": C.T("column_number_%d" % n),
                                          "particle": C.T("tile_white")}}
            for f, y in FACINGS:
                apply = {"model": C.M(mname)}
                if y:
                    apply["y"] = y
                state["multipart"].append({"when": {"facing": f, "number": str(n)},
                                           "apply": apply})
    del child
    item = item_of(parts, tex, keep=lambda c: all(v is False or v == 1 for v in c.values()))
    C.add(reg, 'new BlockPlatformColumnNumber("%s", %s)' % (reg, box_java((2.5, 0, 2.5, 13.5,
                                                                          16, 13.5))),
          names_of("Platform Column (Number Band)", "Bahnsteigstütze (Gleisnummer)",
                   "Columna de Andén (Número de Andén)", "Plattformspelare (Spårnummer)"),
          models, state, item=item, tab=TAB)


# --- the canopy -----------------------------------------------------------------------------
FASCIA_T = 0.6


def canopy():
    """A platform canopy, one block of roof at a time, set on columns: a steel deck with a
    white soffit and a light strip along the platform (`axis`, the way the player faced), and a
    fascia on each side where no canopy continues (`north`..`west`, world sides, actual
    state)."""
    reg = "platform_canopy"
    parts = [("deck", {}, [B((0, 0, 0), (16, 2, 16), "soffit", ("up", "down"),
                             per={"up": "roof"})])]
    fascia_faces = ALL
    y0, y1 = -0.8, 3.2
    sides = {"north": ((0, y0, 0), (16, y1, FASCIA_T)),
             "south": ((0, y0, 16 - FASCIA_T), (16, y1, 16)),
             "west": ((0, y0, 0), (FASCIA_T, y1, 16)),
             "east": ((16 - FASCIA_T, y0, 0), (16, y1, 16))}
    inner = {"north": "south", "south": "north", "west": "east", "east": "west"}
    for side, (f, t) in sides.items():
        per = {inner[side]: "soffit", "up": "roof", "down": "soffit"}
        parts.append(("fascia_" + side, {side: False}, [B(f, t, "fascia", fascia_faces,
                                                          per=per)]))
    lamp = lambda f, t, faces: B(f, t, "lamp", faces, shade=False)  # noqa: E731
    parts.append(("light_x", {"axis": "x"},
                  [lamp((0, -0.4, 6.8), (16, 0, 9.2), ("down", "north", "south"))]))
    parts.append(("light_z", {"axis": "z"},
                  [lamp((6.8, -0.4, 0), (9.2, 0, 16), ("down", "east", "west"))]))
    tex = {"soffit": C.T("soffit"), "roof": C.T("roof"), "fascia": C.T("fascia"),
           "lamp": C.T("lamp_on"), "particle": C.T("fascia")}
    models, state = multipart(parts, reg, tex, facings=False)
    item = item_of(parts, tex, keep=lambda c: all(v is False or v == "x" for v in c.values()))
    C.add(reg, 'new BlockPlatformCanopy("%s")' % reg,
          names_of("Platform Canopy", "Bahnsteigdach", "Marquesina de Andén", "Plattformstak"),
          models, state, item=item, tab=TAB)


# --- the ticket validator --------------------------------------------------------------------
def validator():
    """A tap validator on a post: a Fare Ticket or Transit Card held to it is accepted as at
    the fare gates, and its screen shows a green tick or a red cross for a moment
    (BlockPlatformValidator's `light`: 0 idle, 1 accepted, 2 refused)."""
    reg = "platform_validator"
    tilt = ("x", 22.5, (8, 11, 8))
    els = [B((5.5, 0, 5.5), (10.5, 0.4, 10.5), "stainless", SIDES + ("up",)),
           B((7, 0.4, 7.2), (9, 10.5, 9.2), "stainless", SIDES),
           B((5, 10.5, 5.2), (11, 14.5, 10.6), "body", ALL,
             per={"north": "screen", "up": "target"},
             uv={"north": [0, 0, 16, 16], "up": [0, 0, 16, 16]}, rot=tilt)]
    tex = {"stainless": C.T("stainless"), "body": C.T("graphite"),
           "screen": C.T("validator_idle"), "target": C.T("validator_target"),
           "particle": C.T("graphite")}
    lights = {"0": {"textures": {"screen": C.T("validator_idle")}},
              "1": {"textures": {"screen": C.T("validator_ok")}},
              "2": {"textures": {"screen": C.T("validator_no")}}}
    fixture(reg, 'new BlockPlatformValidator("%s", new double[]{5, 0, 5, 11, 15, 11})' % reg,
            names_of("Ticket Validator", "Fahrkartenentwerter", "Validadora de Billetes",
                     "Biljettvalidator"),
            els, tex, display=gui_display(0.8, 0.0), extra_state={"light": lights})


C.add_lang("csm.transit.help.info", (
    "Help point: a member of station staff will answer shortly.",
    "Notrufsäule: Das Bahnhofspersonal meldet sich gleich.",
    "Punto de ayuda: el personal de la estación le atenderá en breve.",
    "Hjälppunkt: stationspersonalen svarar strax."))
C.add_lang("csm.transit.help.emergency", (
    "Emergency call placed: stay where you are, help is on its way.",
    "Notruf abgesetzt: Bleiben Sie, wo Sie sind, Hilfe ist unterwegs.",
    "Llamada de emergencia realizada: quédese donde está, la ayuda está en camino.",
    "Nödsamtal ringt: stanna där du är, hjälp är på väg."))
C.add_lang("csm.transit.platform.number", (
    "Platform %s", "Gleis %s", "Andén %s", "Spår %s"))
C.add_lang("csm.transit.station.name", (
    "Station: %s", "Station: %s", "Estación: %s", "Station: %s"))
C.add_lang("csm.transit.validator.ticket", (
    "Ticket validated", "Fahrkarte entwertet", "Billete validado", "Biljetten validerad"))
C.add_lang("csm.transit.validator.card", (
    "Card validated: %s trips left", "Karte entwertet: noch %s Fahrten",
    "Tarjeta validada: quedan %s viajes", "Kortet validerat: %s resor kvar"))
C.add_lang("csm.transit.validator.empty", (
    "No trips left on this card: reload it at a vending machine",
    "Keine Fahrten mehr auf der Karte: am Automaten aufladen",
    "No quedan viajes en esta tarjeta: recárguela en una máquina",
    "Inga resor kvar på kortet: ladda det i en automat"))
C.add_lang("csm.transit.validator.none", (
    "Hold a fare ticket or transit card to the validator",
    "Halten Sie eine Fahrkarte oder Fahrgastkarte an den Entwerter",
    "Acerque un billete o una tarjeta de transporte a la validadora",
    "Håll en biljett eller ett resekort mot validatorn"))

register_textures()
tactile_models()
seating()
help_point()
emergency_point()
cctv()
clock()
litter_bin()
number_sign()
hanging_signs()
gap_wall_sign()
station_name_sign()
network_map_board()
station_tiles()
columns()
canopy()
validator()

if __name__ == "__main__":
    sys.exit(C.main())
