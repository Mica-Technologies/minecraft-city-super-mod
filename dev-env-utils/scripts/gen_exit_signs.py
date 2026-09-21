#!/usr/bin/env python3
"""Face artwork for the traditional and specialty exit signs.

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
CORNER_RADIUS = 14


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

def write_all(tex_dir):
    os.makedirs(tex_dir, exist_ok=True)
    written = []
    for name, img in sorted(sheets().items()):
        img.save(os.path.join(tex_dir, name), optimize=False)
        written.append(name)
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
        written = write_all(TEX_DIR)
        print("Wrote %d exit sign textures" % len(written))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_exit_signs_")
    try:
        drifted = []
        for name in write_all(tmp):
            here = os.path.join(TEX_DIR, name)
            if not os.path.exists(here) or not filecmp.cmp(here, os.path.join(tmp, name),
                                                           shallow=False):
                drifted.append(os.path.relpath(here, REPO))
        if drifted:
            print("DRIFT: %d file(s) differ from the generator:" % len(drifted))
            for path in drifted:
                print("  " + path)
            return 1
        print("exit sign textures are up to date")
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
