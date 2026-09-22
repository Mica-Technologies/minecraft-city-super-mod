#!/usr/bin/env python3
"""Every asset the configurable exit signs ship: face artwork, lamp-head lenses, models,
multipart blockstates and item icons, from one catalogue (STYLES) that repeats what each block's
ExitSignSpec offers. ExitSignBlockstateTest fails the build if the two disagree.

One texture sheet per housing finish (white, black, brushed aluminium). Each sheet holds every
piece a sign's face is built from, and the models pick pieces with UVs:

    legend cells   EXIT and SALIDA, each lit red and lit green
    arrow cells    a chevron lit red, lit green, or unlit (embossed in the housing), at the
                   EXIT and the SALIDA cell width, square-cornered and rounded
    housing        a plain patch of the finish, for the body, canopy and back plate

A face is three cells side by side -- left arrow, legend, right arrow -- so a sign's four arrow
choices are two multipart parts instead of four textures, and the right cell is the left one
with its UVs mirrored. The cells meet edge to edge and never overlap, so there is nothing to
z-fight. Each sheet also has an ``_e`` companion carrying only the lit legend and chevrons, for
OptiFine's emissive rendering.

    python dev-env-utils/scripts/gen_exit_signs.py
    python dev-env-utils/scripts/gen_exit_signs.py --check
    python dev-env-utils/scripts/gen_exit_signs.py --sheet out.png   # review contact sheet

The letterforms are measured off a head-on photo of a Dual-Lite thermoplastic sign, in units of
the letter height H: stroke 0.12 H, E 0.31 H wide, X 0.32 H, T 0.32 H, letter spacing 0.068 H,
and a chevron 0.25 H tall set a stroke's width from the housing edge. They are drawn as polygons,
not set in a font, since no font ships the narrow sign-maker's EXIT these units use. SALIDA has
six letters in the room EXIT uses four, so its cell is wider, its chevron cells narrower and its
letters shorter; a real bilingual unit makes the same trade.

Drawn at 16 texels to the model pixel: the face is 16 x 10.5 px, so a stroke is exactly one
model pixel wide.
"""

import argparse
import filecmp
import json
import os
import shutil
import sys
import tempfile

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODULE = os.path.join(REPO, "modules", "lifesafety", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "lifesafety", "exit_signs")
TEX_REF = "csm:blocks/lifesafety/exit_signs/%s"

SHEET = 512
TEXELS_PER_PX = 16
FACE_H = 168  # 10.5 model px
SS = 8  # supersampling for the polygon masks

# --------------------------------------------------------------------------------------------
# Palette
# --------------------------------------------------------------------------------------------

# base      the finish's colour
# grain     per-texel noise amplitude
# streak    horizontal brushing amplitude (brushed metal only)
# emboss    (highlight, shadow) an unlit chevron's raised edge adds and takes away
# tint      what the unlit chevron's face adds to the housing colour
# seed      stable seed for the grain, so --check reproduces it
FINISHES = {
    "white": dict(base=(240, 240, 236), grain=2.5, streak=0, emboss=(10, 40), tint=-5, seed=20260921),
    "black": dict(base=(30, 30, 31), grain=2.0, streak=0, emboss=(36, 14), tint=7, seed=20260922),
    "brushed": dict(base=(182, 185, 189), grain=3.0, streak=9, emboss=(24, 34), tint=-4, seed=20260923),
}
FINISH_ORDER = ["white", "black", "brushed"]

# Measured off the reference photos: the green of a Dual-Lite LED sign, the red of a thermoplastic
# LED sign. Both are what the diffuser shows lit; the sign has no unlit legend state.
LETTER_COLOURS = {
    "red": (255, 40, 40),
    "green": (50, 232, 30),
}
COLOUR_ORDER = ["red", "green"]

# --------------------------------------------------------------------------------------------
# Layout (texels on the sheet)
# --------------------------------------------------------------------------------------------

# legend    cell width, letter height, stroke, letter spacing
# arrow     cell width, chevron height, chevron width, horizontal stroke, tip inset
LEGENDS = {
    "exit": dict(text="EXIT", cell=184, height=128, stroke=15, gap=9,
                 arrow=dict(cell=36, height=32, width=27, stroke=12, inset=7)),
    "salida": dict(text="SALIDA", cell=208, height=96, stroke=12, gap=6,
                   arrow=dict(cell=24, height=26, width=16, stroke=9, inset=5)),
}
LEGEND_ORDER = ["exit", "salida"]

ARROW_KINDS = ["red", "green", "unlit"]
# 1.5 model px: the rounded housing's corner, which its model steps to follow
CORNER_RADIUS = 24


def _layout():
    """Every region on the sheet, as name -> (x0, y0, x1, y1) in texels."""
    regions = {}
    y = 0
    for legend in LEGEND_ORDER:
        x = 0
        for colour in COLOUR_ORDER:
            w = LEGENDS[legend]["cell"]
            regions["legend_%s_%s" % (legend, colour)] = (x, y, x + w, y + FACE_H)
            x += w
        y += FACE_H
    x = 0
    for legend in LEGEND_ORDER:
        w = LEGENDS[legend]["arrow"]["cell"]
        for rounded in (False, True):
            for kind in ARROW_KINDS:
                name = "arrow_%s_%s%s" % (legend, kind, "_rounded" if rounded else "")
                regions[name] = (x, y, x + w, y + FACE_H)
                x += w
    regions["housing"] = (SHEET - 128, y, SHEET, y + 128)
    for name, (x0, y0, x1, y1) in regions.items():
        assert x1 <= SHEET and y1 <= SHEET, name
    return regions


REGIONS = _layout()


def uv(region, mirror=False):
    """A region's UVs in the 0..16 units a model face takes. ``mirror`` swaps u, which is how
    the right arrow cell reuses the left one."""
    x0, y0, x1, y1 = REGIONS[region]
    k = 16.0 / SHEET
    u0, u1 = x0 * k, x1 * k
    if mirror:
        u0, u1 = u1, u0
    return [u0, y0 * k, u1, y1 * k]


# --------------------------------------------------------------------------------------------
# Glyphs, in texels, origin at the glyph's top left
# --------------------------------------------------------------------------------------------

class Pen:
    """Draws into a supersampled mask; every coordinate is in sheet texels."""

    def __init__(self, draw, ox, oy):
        self.draw, self.ox, self.oy = draw, ox, oy

    def _p(self, x, y):
        return ((self.ox + x) * SS, (self.oy + y) * SS)

    def poly(self, pts, fill=255):
        self.draw.polygon([self._p(x, y) for x, y in pts], fill=fill)

    def rect(self, x0, y0, x1, y1, fill=255):
        # a polygon includes its far edge, which would spill one supersample into the next cell
        a, b = self._p(x0, y0), self._p(x1, y1)
        self.draw.rectangle([a, (b[0] - 1, b[1] - 1)], fill=fill)

    def rrect(self, x0, y0, x1, y1, r, corners, fill=255):
        a, b = self._p(x0, y0), self._p(x1, y1)
        r = min(r, (x1 - x0) / 2, (y1 - y0) / 2)
        self.draw.rounded_rectangle([a, (b[0] - 1, b[1] - 1)], radius=r * SS, fill=fill,
                                    corners=corners)


def glyph_width(ch, h, s):
    return {"E": 0.31 * h, "X": 0.32 * h, "I": s, "T": 0.32 * h,
            "S": 0.30 * h, "A": 0.40 * h, "L": 0.27 * h, "D": 0.31 * h}[ch]


def draw_glyph(pen, ch, w, h, s):
    if ch == "E":
        pen.rect(0, 0, s, h)
        pen.rect(0, 0, w, s)
        pen.rect(0, (h - s) / 2, w * 0.92, (h + s) / 2)
        pen.rect(0, h - s, w, h)
    elif ch == "X":
        # Two bands, each a stroke wide measured across, crossing where they are nearest to
        # vertical: the pinched waist and the deep V notches of the photographed signs.
        sx = s * 1.05
        pen.poly([(0, 0), (sx, 0), (w, h), (w - sx, h)])
        pen.poly([(w - sx, 0), (w, 0), (sx, h), (0, h)])
    elif ch == "I":
        pen.rect(0, 0, s, h)
    elif ch == "T":
        pen.rect(0, 0, w, s)
        pen.rect((w - s) / 2, 0, (w + s) / 2, h)
    elif ch == "L":
        pen.rect(0, 0, s, h)
        pen.rect(0, h - s, w, h)
    elif ch == "D":
        r = w * 0.45
        pen.rrect(0, 0, w, h, r, (False, True, True, False))
        pen.rrect(s, s, w - s, h - s, max(r - s, 1), (False, True, True, False), fill=0)
    elif ch == "A":
        sx = s * 1.1
        top = 0.20 * w  # a flat-topped A: a pointed one this narrow has no room for a counter

        def left(y):
            return top * (1 - y / h)

        def right(y):
            return w - top * (1 - y / h)

        pen.poly([(0, h), (left(0), 0), (right(0), 0), (w, h)])
        # The counter is a triangle under the top bar, its apex where the legs' inner edges
        # meet -- lower than the bar itself when the legs are this heavy.
        apex = max(s * 0.9, h * (1 - (w - 2 * sx) / (2 * top)))
        pen.poly([(w / 2, apex), (right(h) - sx, h), (left(h) + sx, h)], fill=0)
        cy = h * 0.62
        pen.rect(left(cy), cy, right(cy), cy + s * 0.9)
    elif ch == "S":
        # A sign-maker's squared S: three bars and two half-height stems, the outer corners
        # rounded so it does not read as a 5 or a 2.
        r = s * 1.2
        mid0, mid1 = (h - s) / 2, (h + s) / 2
        pen.rrect(0, 0, w, s, r, (True, True, False, False))
        pen.rrect(0, 0, s, mid1, r, (True, False, False, True))
        pen.rrect(0, mid0, w, mid1, r, (False, True, False, True))
        pen.rrect(w - s, mid0, w, h, r, (False, True, True, False))
        pen.rrect(0, h - s, w, h, r, (False, False, True, True))
    else:
        raise ValueError(ch)


def draw_legend(pen, legend, cell_w):
    spec = LEGENDS[legend]
    h, s, gap = spec["height"], spec["stroke"], spec["gap"]
    widths = [glyph_width(c, h, s) for c in spec["text"]]
    total = sum(widths) + gap * (len(widths) - 1)
    # Centred on what the eye weighs, not on the bounding box: a final T's crossbar is left to
    # overhang toward the right arrow, as on the photographed signs, and the stems are centred.
    # Centring the box instead reads as a legend shoved to the left.
    overhang = (widths[-1] - s) / 2 if spec["text"].endswith("T") else 0
    x = (cell_w - (total - overhang)) / 2
    assert x >= 2 and x + total <= cell_w - 2,         "%s does not fit its cell (%.1f of %d)" % (legend, total, cell_w)
    y = (FACE_H - h) / 2
    for ch, w in zip(spec["text"], widths):
        draw_glyph(Pen(pen.draw, pen.ox + x, pen.oy + y), ch, w, h, s)
        x += w + gap


def draw_chevron(pen, legend):
    """The left-pointing chevron, its tip ``inset`` from the cell's outer edge."""
    a = LEGENDS[legend]["arrow"]
    tx, cy = a["inset"], FACE_H / 2
    hh, w, sx = a["height"] / 2, a["width"], a["stroke"]
    pen.poly([(tx, cy), (tx + sx, cy), (tx + w, cy - hh), (tx + w - sx, cy - hh)])
    pen.poly([(tx, cy), (tx + sx, cy), (tx + w, cy + hh), (tx + w - sx, cy + hh)])


# --------------------------------------------------------------------------------------------
# Painting
# --------------------------------------------------------------------------------------------

def _mask(draw_fn):
    """Run ``draw_fn(pen)`` on a supersampled sheet-sized canvas; return a SHEET x SHEET float
    coverage array in 0..1."""
    img = Image.new("L", (SHEET * SS, SHEET * SS), 0)
    draw_fn(Pen(ImageDraw.Draw(img), 0, 0))
    return np.asarray(img.resize((SHEET, SHEET), Image.BOX), dtype=np.float64) / 255.0


def _finish_field(finish):
    spec = FINISHES[finish]
    rng = np.random.default_rng(spec["seed"])
    field = np.zeros((SHEET, SHEET, 3)) + np.array(spec["base"], dtype=np.float64)
    noise = rng.uniform(-spec["grain"], spec["grain"], (SHEET, SHEET))
    if spec["streak"]:
        rows = rng.uniform(-1, 1, SHEET)
        # smooth the row noise a little so the brushing reads as long strokes, not scanlines
        rows = np.convolve(np.concatenate([rows[-2:], rows, rows[:2]]),
                           np.ones(5) / 5, mode="valid")
        noise = noise + rows[:, None] * spec["streak"] + \
            rng.uniform(-1, 1, (SHEET, SHEET)) * spec["streak"] * 0.25
    return field + noise[:, :, None]


def _glow(mask):
    """A lit stroke is brightest along its middle, where the diffuser is thickest over the LEDs."""
    img = Image.fromarray(np.uint8(np.clip(mask * 255, 0, 255)))
    blurred = np.asarray(img.filter(ImageFilter.GaussianBlur(3)), dtype=np.float64) / 255.0
    return 0.86 + 0.14 * blurred


def _emboss(mask, highlight, shadow, tint):
    """A chevron moulded into the housing, lit from the upper left: the slope of its edge that
    faces up-left catches the light, the one facing down-right falls into shadow, and the face
    of it is a shade off the housing around it."""
    img = Image.fromarray(np.uint8(np.clip(mask * 255, 0, 255)))
    soft = np.asarray(img.filter(ImageFilter.GaussianBlur(1.2)), dtype=np.float64) / 255.0
    gy, gx = np.gradient(soft)
    slope = -(gx + gy) / np.sqrt(2)  # > 0 where the surface rises towards the upper left
    lit = np.clip(-slope, 0, None) * 2.2
    dark = np.clip(slope, 0, None) * 2.2
    return np.clip(lit, 0, 1) * highlight - np.clip(dark, 0, 1) * shadow + mask * tint


def _region_pen(pen, region):
    x0, y0, _, _ = REGIONS[region]
    return Pen(pen.draw, x0, y0)


def _masks():
    """The coverage masks every sheet shares: lit legend by colour, lit chevrons by colour,
    unlit chevrons, and the rounded corners' cut."""

    def legends(colour):
        def fn(pen):
            for legend in LEGEND_ORDER:
                region = "legend_%s_%s" % (legend, colour)
                draw_legend(_region_pen(pen, region), legend, LEGENDS[legend]["cell"])
        return fn

    def chevrons(kind):
        def fn(pen):
            for legend in LEGEND_ORDER:
                for suffix in ("", "_rounded"):
                    draw_chevron(_region_pen(pen, "arrow_%s_%s%s" % (legend, kind, suffix)),
                                 legend)
        return fn

    def corners(pen):
        # coverage of the housing: everywhere, except outside a rounded cell's outer corners
        pen.rect(0, 0, SHEET, SHEET)
        for legend in LEGEND_ORDER:
            for kind in ARROW_KINDS:
                x0, y0, x1, y1 = REGIONS["arrow_%s_%s_rounded" % (legend, kind)]
                pen.rect(x0, y0, x1, y1, fill=0)
                pen.rrect(x0, y0, x1, y1, CORNER_RADIUS, (True, False, False, True))

    out = {"legend_" + c: _mask(legends(c)) for c in COLOUR_ORDER}
    for kind in ARROW_KINDS:
        out["chevron_" + kind] = _mask(chevrons(kind))
    out["corners"] = _mask(corners)
    return out


def sheets(masks=None):
    """{filename: Image} for every finish: the sheet and its emissive companion."""
    masks = masks or _masks()
    out = {}
    for finish in FINISH_ORDER:
        spec = FINISHES[finish]
        rgb = _finish_field(finish)
        rgb += _emboss(masks["chevron_unlit"], *spec["emboss"], spec["tint"])[:, :, None]
        emissive = np.zeros((SHEET, SHEET, 4))
        for colour in COLOUR_ORDER:
            lit = np.array(LETTER_COLOURS[colour], dtype=np.float64)
            m = np.clip(masks["legend_" + colour] + masks["chevron_" + colour], 0, 1)
            shade = _glow(m)[:, :, None]
            rgb = rgb * (1 - m[:, :, None]) + lit * shade * m[:, :, None]
            emissive[:, :, :3] += lit * shade * m[:, :, None]
            emissive[:, :, 3] += m * 255
        alpha = masks["corners"] * 255
        img = np.dstack([np.clip(rgb, 0, 255), alpha])
        out[finish + ".png"] = Image.fromarray(np.uint8(np.round(img)), "RGBA")
        out[finish + "_e.png"] = Image.fromarray(
            np.uint8(np.round(np.clip(emissive, 0, 255))), "RGBA")
    return out


# --------------------------------------------------------------------------------------------
# Emergency head lenses
# --------------------------------------------------------------------------------------------

HEADS_SHEET = 128
# name -> (x0, y0, x1, y1) on the heads sheet; a lens face is 3 x 3 model px
HEAD_REGIONS = {
    "square_unlit": (0, 0, 64, 64),
    "square_lit": (64, 0, 128, 64),
    "round_unlit": (0, 64, 64, 128),
    "round_lit": (64, 64, 128, 128),
}


# The specialty housings' trim, on a sheet of its own: the vandal-resistant sign's clear
# polycarbonate shield (a cutout rim and glints), its gasket, and the bare metal of
# screws, bolts and hanging rods.
TRIM_SHEET = 64
TRIM_REGIONS = {
    "shield": (0, 0, 32, 32),
    "gasket": (32, 0, 48, 16),
    "metal": (48, 0, 64, 16),
}


def trim_uv(region):
    x0, y0, x1, y1 = TRIM_REGIONS[region]
    k = 16.0 / TRIM_SHEET
    return [x0 * k, y0 * k, x1 * k, y1 * k]


def trim_sheet():
    img = np.zeros((TRIM_SHEET, TRIM_SHEET, 4))
    # shield: clear, drawn as cutout -- only its moulded rim and two short glints in one corner
    # are opaque, and the rest is fully transparent. It was a faint translucent haze once, and
    # that put the whole block in the translucent layer, where faces are sorted by their centres:
    # from some angles the shield's large faces sorted in front of the sign's end cell, wrote
    # depth first, and cut the end of the sign off.
    x0, y0, x1, y1 = TRIM_REGIONS["shield"]
    n = x1 - x0
    yy, xx = np.mgrid[0:n, 0:n]
    rim = (xx < 1) | (yy < 1) | (xx >= n - 1) | (yy >= n - 1)
    glint = ((np.abs(xx - yy - 3) < 1) | (np.abs(xx - yy - 7) < 1)) & (xx + yy < 16) & ~rim
    rgb = np.zeros((n, n, 3))
    rgb[rim] = (206.0, 213.0, 219.0)
    rgb[glint] = (246.0, 248.0, 250.0)
    img[y0:y1, x0:x1, :3] = rgb
    img[y0:y1, x0:x1, 3] = np.where(rim | glint, 255.0, 0.0)
    x0, y0, x1, y1 = TRIM_REGIONS["gasket"]
    img[y0:y1, x0:x1] = (58.0, 58.0, 60.0, 255.0)
    x0, y0, x1, y1 = TRIM_REGIONS["metal"]
    m = x1 - x0
    shade = np.linspace(196.0, 150.0, m)[:, None] * np.ones((1, m))
    img[y0:y1, x0:x1, 0] = shade
    img[y0:y1, x0:x1, 1] = shade
    img[y0:y1, x0:x1, 2] = shade + 4
    img[y0:y1, x0:x1, 3] = 255.0
    return {"trim.png": Image.fromarray(np.uint8(np.round(img)), "RGBA")}


def head_uv(region):
    x0, y0, x1, y1 = HEAD_REGIONS[region]
    k = 16.0 / HEADS_SHEET
    return [x0 * k, y0 * k, x1 * k, y1 * k]


def _disc(size, r, cx, cy):
    yy, xx = np.mgrid[0:size * SS, 0:size * SS]
    inside = ((xx + 0.5) / SS - cx) ** 2 + ((yy + 0.5) / SS - cy) ** 2 <= r * r
    img = Image.fromarray(np.uint8(inside * 255))
    return np.asarray(img.resize((size, size), Image.BOX), dtype=np.float64) / 255.0


def head_sheets():
    """The lamp heads' lenses, dark and lit: a square LED head is a clear prismatic cover over a
    grid of twelve LEDs; a round lamp a clear dome over a ribbed reflector and one LED. The lit
    ones and nothing else go in the ``_e`` companion."""
    n = 64
    rgb = np.zeros((HEADS_SHEET, HEADS_SHEET, 3))
    alpha = np.zeros((HEADS_SHEET, HEADS_SHEET))
    emissive = np.zeros((HEADS_SHEET, HEADS_SHEET, 4))
    yy, xx = np.mgrid[0:n, 0:n]
    for lit in (False, True):
        # square: 4 x 3 LEDs on a prismatic cover
        x0, y0, _, _ = HEAD_REGIONS["square_lit" if lit else "square_unlit"]
        prism = ((xx // 4 + yy // 4) % 2) * 6.0
        cell = np.full((n, n, 3), 205.0 if not lit else 238.0) + prism[:, :, None]
        leds = np.zeros((n, n))
        for i in range(4):
            for j in range(3):
                leds = np.maximum(leds, _disc(n, 4.2, 11 + i * 14, 16 + j * 16))
        led_colour = np.array([255.0, 255.0, 246.0]) if lit else np.array([150.0, 150.0, 150.0])
        cell = cell * (1 - leds[:, :, None]) + led_colour * leds[:, :, None]
        frame = (xx < 3) | (yy < 3) | (xx >= n - 3) | (yy >= n - 3)
        cell[frame] = 170.0 if not lit else 215.0
        rgb[y0:y0 + n, x0:x0 + n] = cell
        alpha[y0:y0 + n, x0:x0 + n] = 1.0
        if lit:
            emissive[y0:y0 + n, x0:x0 + n, :3] = cell
            emissive[y0:y0 + n, x0:x0 + n, 3] = np.maximum(leds, 0.35) * 255 * ~frame
        # round: a dome over a ribbed reflector with one LED in the middle
        x0, y0, _, _ = HEAD_REGIONS["round_lit" if lit else "round_unlit"]
        c = n / 2.0
        r = np.sqrt((xx + 0.5 - c) ** 2 + (yy + 0.5 - c) ** 2)
        base = 200.0 if not lit else 236.0
        ribs = np.cos(r * 1.1) * 12.0
        cell = np.full((n, n, 3), base) + ribs[:, :, None]
        cell += (-(xx + yy - n) / n * 14.0)[:, :, None]  # the dome's highlight, upper left
        led = _disc(n, 6.0, c, c)
        led_colour = np.array([255.0, 255.0, 246.0]) if lit else np.array([140.0, 140.0, 140.0])
        cell = cell * (1 - led[:, :, None]) + led_colour * led[:, :, None]
        rim = (r > c - 3.5) & (r <= c)
        cell[rim] = 165.0 if not lit else 212.0
        disc = _disc(n, c - 0.5, c, c)
        rgb[y0:y0 + n, x0:x0 + n] = cell
        alpha[y0:y0 + n, x0:x0 + n] = disc
        if lit:
            emissive[y0:y0 + n, x0:x0 + n, :3] = cell
            emissive[y0:y0 + n, x0:x0 + n, 3] = disc * np.maximum(led, 0.35) * 255
    img = np.dstack([np.clip(rgb, 0, 255), alpha * 255])
    return {
        "heads.png": Image.fromarray(np.uint8(np.round(img)), "RGBA"),
        "heads_e.png": Image.fromarray(np.uint8(np.round(np.clip(emissive, 0, 255))), "RGBA"),
    }


# --------------------------------------------------------------------------------------------
# Models and blockstates
# --------------------------------------------------------------------------------------------
#
# Every model is drawn facing north, the face at the low-z side, and the blockstate turns it to
# the sign's facing. A wall-mounted sign sits against the block's south face; a hung one (ceiling
# or end mount) down the middle of the block, with the legend on both faces. The face is 16 px
# wide and 10.5 tall, and is built from the sheet's cells: an arrow cell each end and the legend
# between. The east cell is the viewer's left from the front, so it holds the left arrow, and
# from the back it is the viewer's right: an arrow keeps pointing the same way in the world,
# which is what a real double-faced sign's knockout does.
#
# The blockstate is vanilla multipart: each part is applied when its option values match, so
# one model file covers every state that shows it. Multipart cannot retexture, so a part is
# written once per housing finish.

MODEL_DIR = os.path.join(MODULE, "models", "block", "lifesafety", "exit_signs")
ITEM_DIR = os.path.join(MODULE, "models", "item")
STATE_DIR = os.path.join(MODULE, "blockstates")
MODEL_REF = "csm:lifesafety/exit_signs/%s"

FACE_PX = FACE_H / TEXELS_PER_PX  # 10.5
DEPTH = {"wall": (14.0, 16.0), "hung": (7.0, 9.0)}
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
ARROWS = ["none", "left", "right", "both"]
MOUNTS = ["wall", "ceiling", "end_left", "end_right"]
# Which arrow values light each end cell. "left" is the viewer's left from the front: east.
LIT_ON = {"east": ("left", "both"), "west": ("right", "both")}

# What each block offers, in the order its ExitSignSpec lists them (the first value is the
# default). An option with one value has no property, so it never appears in a condition.
# ExitSignBlockstateTest fails the build if this and the Java specs disagree.
#   y0           the face's bottom edge, px
#   rounded      rounded housing corners: alpha on the face, a stepped end on the model
#   heads_at     "ends": lamp heads on arms off each end; "top": above the top corners
#   kind         the housing: "plastic" (flat, rounded, combo), "diecast", "vandal", "panel"
#                (photoluminescent) or "xp" (explosion-proof); picks the extras and mounts
#   depth        the body's z range, px, wall-mounted and hung (default DEPTH)
STYLES = [
    dict(name="flat", block="exit_sign_traditional_flat", rounded=False, y0=4.5,
         heads_at="ends", finishes=["white", "black"], heads=["none", "square", "round"],
         mounts=MOUNTS, letters=["red", "green"], legends=["exit", "salida"]),
    dict(name="rounded", block="exit_sign_traditional_rounded", rounded=True, y0=4.5,
         heads_at="ends", finishes=["white", "black"], heads=["none", "square", "round"],
         mounts=MOUNTS, letters=["red", "green"], legends=["exit", "salida"]),
    dict(name="combo", block="exit_sign_combo_compact", rounded=False, y0=2.0,
         heads_at="top", finishes=["white", "black"], heads=["square", "round"],
         mounts=MOUNTS, letters=["red", "green"], legends=["exit", "salida"]),
    dict(name="diecast", block="exit_sign_diecast", kind="diecast", rounded=False, y0=4.5,
         depth={"wall": (13.0, 16.0), "hung": (6.5, 9.5)},
         finishes=["brushed", "black", "white"], heads=["none"], mounts=MOUNTS,
         letters=["red", "green"], legends=["exit", "salida"]),
    dict(name="vandal", block="exit_sign_vandal_resistant", kind="vandal", rounded=False, y0=4.5,
         finishes=["white", "black"], heads=["none"], mounts=["wall", "ceiling"],
         letters=["red", "green"], legends=["exit", "salida"]),
    dict(name="panel", block="exit_sign_photoluminescent", kind="panel", rounded=False, y0=4.5,
         depth={"wall": (15.5, 16.0), "hung": (7.75, 8.25)},
         finishes=["white", "black"], heads=["none"], mounts=["wall", "ceiling"],
         letters=["green", "red"], legends=["exit", "salida"]),
    dict(name="xp", block="exit_sign_explosion_proof", kind="xp", rounded=False, y0=3.5,
         depth={"wall": (12.0, 16.0), "hung": (6.0, 10.0)},
         finishes=["brushed"], heads=["none"], mounts=["wall", "ceiling"],
         letters=["red", "green"], legends=["exit", "salida"]),
]


def _depth(style, mc):
    return style.get("depth", DEPTH)[mc]


def _kind(style):
    return style.get("kind", "plastic")
SIX = ("north", "south", "east", "west", "up", "down")


def _n(v):
    """A coordinate or UV, rounded so the JSON is stable and readable."""
    v = round(v, 4)
    return int(v) if v == int(v) else v


def _uv_texels(x0, y0, x1, y1, sheet=SHEET):
    k = 16.0 / sheet
    return [_n(x0 * k), _n(y0 * k), _n(x1 * k), _n(y1 * k)]


HOUSING_UV = uv("housing")


def _face(uv_box, texture="#sheet", cull=None):
    face = {"uv": [_n(u) for u in uv_box], "texture": texture}
    if cull:
        face["cullface"] = cull
    return face


def _box(frm, to, faces):
    return {"from": [_n(v) for v in frm], "to": [_n(v) for v in to], "faces": faces}


def _housing_faces(names, cull=None):
    cull = cull or {}
    return {n: _face(HOUSING_UV, cull=cull.get(n)) for n in names}


def _back(mc, faces, back_uv):
    """The face's back: the legend or cell again on a hung sign, bare housing culled against
    the wall on a wall-mounted one."""
    if mc == "hung":
        faces["south"] = _face(back_uv)
    else:
        faces["south"] = _face(HOUSING_UV, cull="south")
    return faces


def legend_elements(style, legend, letters, mc):
    cw = LEGENDS[legend]["arrow"]["cell"] / TEXELS_PER_PX
    y0, top = style["y0"], style["y0"] + FACE_PX
    z0, z1 = _depth(style, mc)
    x0, v0, x1, v1 = REGIONS["legend_%s_%s" % (legend, letters)]
    legend_uv = _uv_texels(x0, v0, x1, v1)
    faces = {"north": _face(legend_uv)}
    faces.update(_housing_faces(("up", "down")))
    _back(mc, faces, legend_uv)
    return [_box((cw, y0, z0), (16 - cw, top, z1), faces)]


def cell_elements(style, legend, kind, side, mc):
    """One end cell of the face, lit ``kind`` ('red', 'green') or 'unlit'. A rounded housing's
    cell is two boxes, the outermost half pixel stepped in a quarter pixel top and bottom, so
    the body follows the corner the face's alpha cuts."""
    cw = LEGENDS[legend]["arrow"]["cell"] / TEXELS_PER_PX
    y0, top = style["y0"], style["y0"] + FACE_PX
    z0, z1 = _depth(style, mc)
    region = "arrow_%s_%s%s" % (legend, kind, "_rounded" if style["rounded"] else "")
    rx, ry, _, _ = REGIONS[region]
    pieces = [(0.0, 0.5, 0.25), (0.5, cw, 0.0)] if style["rounded"] else [(0.0, cw, 0.0)]
    out = []
    for a, b, inset in pieces:  # a, b: distance from the cell's outer edge, px
        yb, yt = y0 + inset, top - inset
        vt, vb = ry + (top - yt) * TEXELS_PER_PX, ry + (top - yb) * TEXELS_PER_PX

        def cell_uv(d_left, d_right):
            # the texture columns at the viewer's left and right edges of the face
            return _uv_texels(rx + d_left * TEXELS_PER_PX, vt, rx + d_right * TEXELS_PER_PX, vb)

        if side == "east":
            x0, x1 = 16 - b, 16 - a
            north, south = cell_uv(a, b), cell_uv(b, a)
        else:
            x0, x1 = a, b
            north, south = cell_uv(b, a), cell_uv(a, b)
        faces = {"north": _face(north)}
        faces.update(_housing_faces(("up", "down", side)))
        _back(mc, faces, south)
        out.append(_box((x0, yb, z0), (x1, yt, z1), faces))
    return out


def _mirror_x(elements):
    """The same elements reflected across x = 8: the west twin of an east part."""
    out = []
    for e in elements:
        faces = {}
        for name, face in e["faces"].items():
            flipped = {"east": "west", "west": "east"}.get(name, name)
            face = dict(face)
            if face.get("cullface") in ("east", "west"):
                face["cullface"] = flipped
            faces[flipped] = face
        out.append(_box((16 - e["to"][0], e["from"][1], e["from"][2]),
                        (16 - e["from"][0], e["to"][1], e["to"][2]), faces))
    return out


def _trim(names, region, cull=None):
    cull = cull or {}
    return {n: _face(trim_uv(region), texture="#trim", cull=cull.get(n)) for n in names}


def extra_elements(style, mc):
    """What a specialty housing adds around the plain sign, whatever it shows:

    - die-cast: a cast lip standing a quarter pixel proud round the face's edge, clear of the
      chevrons' tips
    - vandal-resistant: a clear polycarbonate shield over the whole sign, on a gasketed back plate
      when it is on a wall
    - photoluminescent: a screw in each corner of the thin panel
    - explosion-proof: a heavy cast frame a pixel wide round the face, standing proud so the face
      sits recessed in it, a bolt at each corner, and a threaded conduit hub on top
    Returns (elements, uses the trim sheet)."""
    kind = _kind(style)
    y0, top = style["y0"], style["y0"] + FACE_PX
    z0, z1 = _depth(style, mc)
    hung = mc == "hung"
    out = []
    if kind == "diecast":
        lip = 0.25
        for zf, zb in ([(z0 - lip, z0)] + ([(z1, z1 + lip)] if hung else [])):
            out.append(_box((0, top - lip, zf), (16, top, zb), _housing_faces(SIX)))
            out.append(_box((0, y0, zf), (16, y0 + lip, zb), _housing_faces(SIX)))
            out.append(_box((0, y0 + lip, zf), (lip, top - lip, zb), _housing_faces(SIX)))
            out.append(_box((16 - lip, y0 + lip, zf), (16, top - lip, zb), _housing_faces(SIX)))
        return out, False
    if kind == "vandal":
        faces = ("north", "east", "west", "up", "down") + (("south",) if hung else ())
        back = z1 + 1.5 if hung else 15.5
        out.append(_box((-0.75, y0 - 0.75, z0 - 1.5), (16.75, top + 0.75, back),
                        _trim(faces, "shield")))
        if not hung:
            out.append(_box((-1, y0 - 1, 15.5), (17, top + 1, 16),
                            _trim(SIX, "gasket", cull={"south": "south"})))
        return out, True
    if kind == "panel":
        for x in (0.4, 15.1):
            for y in (y0 + 0.4, top - 0.9):
                out.append(_box((x, y, z0 - 0.125), (x + 0.5, y + 0.5, z0), _trim(SIX, "metal")))
                if hung:
                    out.append(_box((x, y, z1), (x + 0.5, y + 0.5, z1 + 0.125),
                                    _trim(SIX, "metal")))
        return out, True
    if kind == "xp":
        front = z0 - 0.75
        back = z1 + 0.75 if hung else z1
        cull = {} if hung else {"south": "south"}
        out.append(_box((-1, y0 - 1, front), (0, top + 1, back), _housing_faces(SIX, cull=cull)))
        out.append(_box((16, y0 - 1, front), (17, top + 1, back), _housing_faces(SIX, cull=cull)))
        out.append(_box((0, top, front), (16, top + 1, back), _housing_faces(SIX, cull=cull)))
        out.append(_box((0, y0 - 1, front), (16, y0, back), _housing_faces(SIX, cull=cull)))
        for x in (-0.75, 16.25):
            for y in (y0 - 0.75, top + 0.25):
                out.append(_box((x, y, front - 0.25), (x + 0.5, y + 0.5, front),
                                _trim(SIX, "metal")))
        # the conduit hub: a squat cylinder, two crossed boxes, up to the top of the block
        mid = (z0 + z1) / 2
        out.append(_box((7, top + 1, mid - 0.75), (9, 16, mid + 0.75),
                        _housing_faces(SIX, cull={"up": "up"})))
        out.append(_box((7.25, top + 1, mid - 1), (8.75, 16, mid + 1),
                        _housing_faces(SIX, cull={"up": "up"})))
        return out, True
    return out, False


def hardware_elements(style, mount):
    """The colour-matched mount: a canopy on the ceiling (with a stem down to a combo unit,
    whose heads leave no room for the canopy on the body), or a plate on the wall at one end.
    A photoluminescent panel hangs from two rods, a vandal-resistant sign's canopy sits on its
    shield, and an explosion-proof sign hangs from its own conduit hub, so has nothing more."""
    y0, top = style["y0"], style["y0"] + FACE_PX
    kind = _kind(style)
    if mount == "ceiling" and kind == "xp":
        return []
    if mount == "ceiling" and kind == "panel":
        z0, z1 = _depth(style, "hung")
        return [_box((x, top, z0), (x + 0.5, 16, z1), _trim(SIX, "metal", cull={"up": "up"}))
                for x in (2.75, 12.75)]
    if mount == "ceiling" and kind == "vandal":
        return [_box((4, top + 0.75, 6), (12, 16, 10), _housing_faces(SIX, cull={"up": "up"}))]
    if mount == "ceiling":
        out = []
        if top < 15:
            out.append(_box((7.25, top, 7.25), (8.75, 15, 8.75),
                            _housing_faces(("north", "south", "east", "west"))))
            out.append(_box((5, 15, 6.5), (11, 16, 9.5), _housing_faces(SIX, cull={"up": "up"})))
        else:
            out.append(_box((4, top, 6.5), (12, 16, 9.5), _housing_faces(SIX, cull={"up": "up"})))
        return out
    mid = y0 + FACE_PX / 2
    plate = [_box((15.75, mid - 3.25, 5.5), (16, mid + 3.25, 10.5),
                  _housing_faces(SIX, cull={"east": "east"}))]
    # end_left: the wall is on the viewer's left from the front, which is east
    return plate if mount == "end_left" else _mirror_x(plate)


def head_elements(style, head, mc, lit):
    """One lamp head, on the east side (``_mirror_x`` gives the west one). An end head sits on
    a short arm off the sign's end; a top head (combo) on a short arm above its top corner."""
    y0, top = style["y0"], style["y0"] + FACE_PX
    z0, z1 = _depth(style, mc)
    lens_uv = head_uv("%s_%s" % (head, "lit" if lit else "unlit"))
    back_cull = {"south": "south"} if mc == "wall" else {}
    if style["heads_at"] == "ends":
        c = y0 + FACE_PX / 2
        arm = _box((16, c - 0.5, z0 + 0.5), (16.5, c + 0.5, z1 - 0.5),
                   _housing_faces(("north", "south", "up", "down")))
        cx, cy = 18.25, c
    else:
        arm = _box((13.5, top, z0 + 0.5), (14.5, top + 0.25, z1 - 0.5),
                   _housing_faces(("north", "south", "east", "west")))
        cx, cy = 14.0, top + 0.25 + 1.75
    out = [arm]
    if head == "square":
        out.append(_box((cx - 1.75, cy - 1.75, z0 - 0.5), (cx + 1.75, cy + 1.75, z1),
                        _housing_faces(SIX, cull=back_cull)))
        lens = {"north": _face(lens_uv, texture="#lens")}
        lens.update(_housing_faces(("east", "west", "up", "down")))
        out.append(_box((cx - 1.5, cy - 1.5, z0 - 0.75), (cx + 1.5, cy + 1.5, z0 - 0.5), lens))
    else:
        # a round lamp: two crossed boxes for the barrel, and the lens a disc cut by alpha
        out.append(_box((cx - 1.5, cy - 1.0, z0 - 0.25), (cx + 1.5, cy + 1.0, z1 - 0.25),
                        _housing_faces(SIX)))
        out.append(_box((cx - 1.0, cy - 1.5, z0 - 0.25), (cx + 1.0, cy + 1.5, z1 - 0.25),
                        _housing_faces(SIX)))
        out.append(_box((cx - 1.5, cy - 1.5, z0 - 0.5), (cx + 1.5, cy + 1.5, z0 - 0.25),
                        {"north": _face(lens_uv, texture="#lens")}))
    return out


def _model(elements, finish, lens=False, trim=False):
    textures = {"sheet": TEX_REF % finish, "particle": TEX_REF % finish}
    if lens:
        textures["lens"] = TEX_REF % "heads"
    if trim:
        textures["trim"] = TEX_REF % "trim"
    return {"textures": textures, "elements": elements}


def part_models(style):
    """{model name: model} for every part of ``style``, one per finish."""
    out = {}
    for finish in style["finishes"]:
        p = "%s_%s_" % (style["name"], finish)
        for mc in DEPTH:
            for legend in style["legends"]:
                for letters in style["letters"]:
                    out[p + "legend_%s_%s_%s" % (legend, letters, mc)] = _model(
                        legend_elements(style, legend, letters, mc), finish)
                for kind in style["letters"] + ["unlit"]:
                    for side in ("east", "west"):
                        out[p + "cell_%s_%s_%s_%s" % (legend, kind, side, mc)] = _model(
                            cell_elements(style, legend, kind, side, mc), finish)
            for head in style["heads"]:
                if head == "none":
                    continue
                for lit in (False, True):
                    east = head_elements(style, head, mc, lit)
                    state = "lit" if lit else "dark"
                    out[p + "head_%s_%s_east_%s" % (head, state, mc)] = _model(
                        east, finish, lens=True)
                    out[p + "head_%s_%s_west_%s" % (head, state, mc)] = _model(
                        _mirror_x(east), finish, lens=True)
            extras, trim = extra_elements(style, mc)
            if extras:
                out[p + "extras_%s" % mc] = _model(extras, finish, trim=trim)
        for mount in style["mounts"]:
            hardware = hardware_elements(style, mount) if mount != "wall" else []
            if hardware:
                out[p + "mount_%s" % mount] = _model(hardware, finish, trim=_kind(style) == "panel")
    return out


def _when(style, **conditions):
    """A multipart condition, leaving out every option the block has only one value of."""
    offered = {"legend": style["legends"], "letters": style["letters"],
               "housing": style["finishes"], "mount": style["mounts"], "heads": style["heads"],
               "arrow": ARROWS}
    out = {}
    for key, values in conditions.items():
        values = [values] if isinstance(values, str) else list(values)
        if key in offered:
            if len(offered[key]) < 2:
                continue
            values = [v for v in values if v in offered[key]]
            assert values, (style["name"], key)
        out[key] = "|".join(values)
    return out


def blockstate(style):
    parts = []

    def add(model, **when):
        for facing, y in FACING_Y.items():
            apply = {"model": MODEL_REF % model}
            if y:
                apply["y"] = y
            parts.append({"when": _when(style, facing=facing, **when), "apply": apply})

    hung = [m for m in style["mounts"] if m != "wall"]
    for finish in style["finishes"]:
        p = "%s_%s_" % (style["name"], finish)
        for mc, mounts in (("wall", ["wall"]), ("hung", hung)):
            for legend in style["legends"]:
                for letters in style["letters"]:
                    add(p + "legend_%s_%s_%s" % (legend, letters, mc), housing=finish,
                        mount=mounts, legend=legend, letters=letters)
                for side in ("east", "west"):
                    lit = list(LIT_ON[side])
                    dark = [a for a in ARROWS if a not in lit]
                    for letters in style["letters"]:
                        add(p + "cell_%s_%s_%s_%s" % (legend, letters, side, mc),
                            housing=finish, mount=mounts, legend=legend, letters=letters,
                            arrow=lit)
                    add(p + "cell_%s_unlit_%s_%s" % (legend, side, mc), housing=finish,
                        mount=mounts, legend=legend, arrow=dark)
            for head in style["heads"]:
                if head == "none":
                    continue
                for side in ("east", "west"):
                    # an end head on the wall side of an end mount would be inside the wall
                    blocked = {"east": "end_left", "west": "end_right"}[side]
                    ok = [m for m in mounts if style["heads_at"] != "ends" or m != blocked]
                    for lit, powered in ((True, "false"), (False, "true")):
                        add(p + "head_%s_%s_%s_%s" % (head, "lit" if lit else "dark", side, mc),
                            housing=finish, mount=ok, heads=head, powered=powered)
            if extra_elements(style, mc)[0]:
                add(p + "extras_%s" % mc, housing=finish, mount=mounts)
        for mount in hung:
            if hardware_elements(style, mount):
                add(p + "mount_%s" % mount, housing=finish, mount=mount)
    return {"multipart": parts}


def item_models(style):
    """One icon per legend, letter colour, finish and heads, named as ExitSignItemModels asks
    for them. The icon is the hung sign with both arrows and the heads dark, so it sits in the
    middle of the slot the way a block's does."""
    out = {}
    for legend in style["legends"]:
        for letters in style["letters"]:
            for finish in style["finishes"]:
                for head in style["heads"]:
                    elements = legend_elements(style, legend, letters, "hung")
                    elements += cell_elements(style, legend, "unlit", "east", "hung")
                    elements += cell_elements(style, legend, "unlit", "west", "hung")
                    if head != "none":
                        heads = head_elements(style, head, "hung", False)
                        elements += heads + _mirror_x(heads)
                    extras, trim = extra_elements(style, "hung")
                    elements += extras
                    model = _model(elements, finish, lens=head != "none", trim=trim)
                    model = dict({"parent": "block/block"}, **model)
                    out["%s_%s_%s_%s_%s" % (style["block"], legend, letters, finish, head)] = model
    # The plain item model, named after the block: the default preset's icon. The game never
    # asks for it (the stack's setup picks its icon), but anything that looks an item's model up
    # by its registry name -- the integrity tool, another mod -- finds a sign and not nothing.
    out[style["block"]] = {"parent": "csm:item/%s_%s_%s_%s_%s" % (
        style["block"], style["legends"][0], style["letters"][0], style["finishes"][0],
        style["heads"][0])}
    return out


# --------------------------------------------------------------------------------------------
# Review sheet
# --------------------------------------------------------------------------------------------

def face(sheet, legend, colour, arrow, rounded=False):
    """Assemble one sign face from its cells, the way the model will: arrow cell, legend cell,
    arrow cell mirrored."""
    suffix = "_rounded" if rounded else ""
    left = colour if arrow in ("left", "both") else "unlit"
    right = colour if arrow in ("right", "both") else "unlit"
    lc = sheet.crop(REGIONS["arrow_%s_%s%s" % (legend, left, suffix)])
    rc = sheet.crop(REGIONS["arrow_%s_%s%s" % (legend, right, suffix)]) \
        .transpose(Image.FLIP_LEFT_RIGHT)
    mid = sheet.crop(REGIONS["legend_%s_%s" % (legend, colour)])
    out = Image.new("RGBA", (lc.width + mid.width + rc.width, FACE_H), (0, 0, 0, 0))
    out.paste(lc, (0, 0))
    out.paste(mid, (lc.width, 0))
    out.paste(rc, (lc.width + mid.width, 0))
    return out


def contact_sheet(path):
    imgs = sheets()
    arrows = ["none", "left", "right", "both"]
    pad, label_h = 12, 18
    cols = len(arrows)
    rows = [(f, l, c, r) for f in FINISH_ORDER for l in LEGEND_ORDER for c in COLOUR_ORDER
            for r in ((False, True) if f != "brushed" else (False,))]
    fw = 256
    W = pad + cols * (fw + pad) + 150
    H = pad + len(rows) * (FACE_H + label_h + pad)
    out = Image.new("RGBA", (W, H), (128, 128, 128, 255))
    draw = ImageDraw.Draw(out)
    font = ImageFont.load_default()
    for i, (finish, legend, colour, rounded) in enumerate(rows):
        y = pad + i * (FACE_H + label_h + pad)
        draw.text((pad, y), "%s / %s / %s%s" % (finish, legend.upper(), colour,
                                                " / rounded corners" if rounded else ""),
                  fill=(0, 0, 0, 255), font=font)
        for j, arrow in enumerate(arrows):
            f = face(imgs[finish + ".png"], legend, colour, arrow, rounded)
            x = pad + j * (fw + pad)
            out.alpha_composite(f, (x + (fw - f.width) // 2, y + label_h))
    # the emissive layers of the first finish, for a glance that they carry only the lit parts
    ex = imgs["white_e.png"].resize((128, 128), Image.BOX)
    out.alpha_composite(ex, (W - 140, pad + label_h))
    draw.text((W - 140, pad), "white_e.png", fill=(0, 0, 0, 255), font=font)
    out.save(path)
    return path


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def _write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


ROOTS = {"tex": TEX_DIR, "model": MODEL_DIR, "item": ITEM_DIR, "state": STATE_DIR}


def write_all(roots):
    """Writes every generated file under ``roots`` ({kind: directory}) and returns (kind,
    file name) for each."""
    written = []
    os.makedirs(roots["tex"], exist_ok=True)
    images = dict(sheets())
    images.update(head_sheets())
    images.update(trim_sheet())
    for name, img in sorted(images.items()):
        img.save(os.path.join(roots["tex"], name), optimize=False)
        written.append(("tex", name))
    for style in STYLES:
        for name, model in sorted(part_models(style).items()):
            _write_json(os.path.join(roots["model"], name + ".json"), model)
            written.append(("model", name + ".json"))
        for name, model in sorted(item_models(style).items()):
            _write_json(os.path.join(roots["item"], name + ".json"), model)
            written.append(("item", name + ".json"))
        _write_json(os.path.join(roots["state"], style["block"] + ".json"), blockstate(style))
        written.append(("state", style["block"] + ".json"))
    return written


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    ap.add_argument("--sheet", metavar="PNG",
                    help="write a review contact sheet of every face to PNG and exit")
    args = ap.parse_args()

    if args.sheet:
        print("Wrote " + contact_sheet(args.sheet))
        return 0

    if not args.check:
        written = write_all(ROOTS)
        print("Wrote %d exit sign files" % len(written))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_exit_signs_")
    try:
        tmp_roots = {kind: os.path.join(tmp, kind) for kind in ROOTS}
        drifted = []
        for kind, name in write_all(tmp_roots):
            here = os.path.join(ROOTS[kind], name)
            there = os.path.join(tmp_roots[kind], name)
            if not os.path.exists(here) or not filecmp.cmp(here, there, shallow=False):
                drifted.append(os.path.relpath(here, REPO))
        if drifted:
            print("DRIFT: %d file(s) differ from the generator:" % len(drifted))
            for path in drifted:
                print("  " + path)
            return 1
        print("exit sign files are up to date")
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
