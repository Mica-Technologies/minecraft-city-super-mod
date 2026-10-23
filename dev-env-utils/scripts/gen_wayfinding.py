#!/usr/bin/env python3
"""
gen_wayfinding.py -- the wayfinding panels' sheets, their Java layout and the three blocks' assets.

One baked engine in Core (codeutils.wayfinding) draws all three wayfinding panels: Transit's
airport_wayfinding_panel, Healthcare's hospital_wayfinding_panel and Building's generic
wayfinding_panel (GitHub #307). Everything it draws comes from two sheets this script writes:

  * wayfinding_glyphs.png, 512 x 512: the printable ASCII glyphs of the committed highway font
    (Series D, 32 px capitals) -- the fourteen it lacks (% ; < > [ \\ ] ^ _ ` { | } ~) drawn here
    in its stroke and proportions -- white on clear, and a white swatch;
  * wayfinding_icons.png, 512 x 512: 64 px cells, 8 x 8, each an ALPHA MASK (white, the art
    opaque, the ground and the cut-outs inside the art clear): the two arrows in cells 0 and 1
    (straight pointing right, diagonal up and right), then the pictograms. The engine draws the
    tile behind a pictogram itself, so one mask serves black art on a tile and coloured art plain.

The pictograms are generic signage figures, bold shapes drawn four times larger and reduced: no
brand, no real product, and never a red cross (a protected emblem; first aid is a kit).

WayfindingSheet.java (Core, codeutils.wayfinding) is written from the same numbers, so the Java and
the sheets cannot drift. Also written: Core's empty base model every placed panel uses (the engine
bakes the slab, frame, legend and mounts) with its graphite particle texture, and per block the
Forge blockstate (facing only) and an item model: a framed slab with a pictogram, in the block's
colours, from small textures in its own module's tree.

Writes:
  src/main/resources/assets/csm/textures/blocks/wayfinding/wayfinding_{glyphs,icons,frame}.png
  src/main/resources/assets/csm/models/block/wayfinding/wayfinding_panel_base.json
  src/main/java/.../csm/codeutils/wayfinding/WayfindingSheet.java
  modules/{transit,healthcare,building}/src/main/resources/assets/csm/
      blockstates/<registry>.json, models/item/<registry>.json,
      models/block/wayfinding/<registry>_item.json,
      textures/blocks/wayfinding/<registry>_{face,rim}.png

The tile names stay with the blocks' old generators (gen_transit_airport.py,
gen_healthcare_systems.py), which keep their lang lines and tab fragments; the generic panel's
name is in Building's lang files.

Usage:
    py gen_wayfinding.py                 # write everything
    py gen_wayfinding.py --check         # write nothing, exit 1 on drift
    py gen_wayfinding.py --sheet <png>   # a review sheet of every icon, large, tiled and plain

Requires Pillow and fontTools.
"""
import argparse
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFont

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CORE = os.path.join(REPO, "src", "main", "resources", "assets", "csm")
FONT_DIR = os.path.join(REPO, "assets", "fonts")
JAVA = os.path.join(REPO, "src", "main", "java", "com", "micatechnologies", "minecraft", "csm",
                    "codeutils", "wayfinding", "WayfindingSheet.java")


def module_assets(name):
    return os.path.join(REPO, "modules", name, "src", "main", "resources", "assets", "csm")


# ==========================================================================================
# The glyph sheet
# ==========================================================================================
SHEET = 512
CAP = 32            # the glyphs' cap height on the sheet, px
GLYPH_ROW = 50      # a row of glyphs: ascenders, caps, descenders and padding
ICON = 64           # an icon's (and an arrow's) square, px
GLYPH_LIMIT = SHEET - 2 * ICON   # the glyphs stop above this (the old sheet's pictogram rows)
SUPER = 4           # drawn this many times larger and reduced
FONT = os.path.join(FONT_DIR, "MicaOpenHighway-SeriesD.ttf")
# A white swatch for the coloured backgrounds, sampled at its middle: x, y, size.
WHITE_CELL = (SHEET - 16, GLYPH_LIMIT - 16, 12)


def _font(size):
    return ImageFont.truetype(FONT, size)


def _cap_size():
    """The font size whose capitals are CAP * SUPER px tall."""
    f = _font(400)
    b = f.getbbox("H")
    return int(round(400 * CAP * SUPER / float(b[3] - b[1])))


# The glyphs the committed Series D font lacks, drawn in its stroke and proportions: each a list of
# strokes in units of the cap height H from the pen (x right, y up from the baseline), a stroke
# being a polyline drawn the font's stem wide with round joints and ends ("ring" an ellipse
# outline, "dot" a filled square of the font's full stop), and its advance in H. Brackets, braces
# and the bar reach from the cap line to below the baseline as the font's parentheses do; the
# angle brackets, caret and tilde sit on the mathematical axis, half the cap height.
def _brace(sign):
    """{ (sign 1) or } (sign -1): a stem pinched to a point at the axis, hooked at its ends."""
    x0, xs, xp = (0.36, 0.22, 0.07) if sign > 0 else (0.10, 0.24, 0.39)
    return [[(x0, 1.06), (xs, 1.0), (xs, 0.6), (xp, 0.45), (xs, 0.3), (xs, -0.1), (x0, -0.16)]]


DRAWN = {
    "%": ([("ring", 0.04, 0.58, 0.36, 1.0), ("ring", 0.42, 0.0, 0.74, 0.42),
           [(0.62, 1.0), (0.16, 0.0)]], 0.86),
    ";": ([("dot", 0.18, 0.62), [(0.24, 0.12), (0.18, -0.2)], ("dot", 0.18, 0.12)], 0.48),
    "<": ([[(0.56, 0.78), (0.1, 0.5), (0.56, 0.22)]], 0.68),
    ">": ([[(0.12, 0.78), (0.58, 0.5), (0.12, 0.22)]], 0.68),
    "[": ([[(0.38, 1.06), (0.14, 1.06), (0.14, -0.16), (0.38, -0.16)]], 0.48),
    "]": ([[(0.1, 1.06), (0.34, 1.06), (0.34, -0.16), (0.1, -0.16)]], 0.48),
    "\\": ([[(0.06, 1.0), (0.52, 0.0)]], 0.6),
    "^": ([[(0.08, 0.58), (0.32, 1.0), (0.56, 0.58)]], 0.66),
    "_": ([[(0.04, -0.14), (0.62, -0.14)]], 0.66),
    "`": ([[(0.12, 1.02), (0.26, 0.84)]], 0.42),
    "{": (_brace(1), 0.48),
    "}": (_brace(-1), 0.48),
    "|": ([[(0.2, 1.06), (0.2, -0.16)]], 0.42),
    "~": ([[(0.06 + 0.56 * i / 12.0, 0.5 + 0.08 * math.sin(math.pi * 2 * i / 12.0))
            for i in range(13)]], 0.72),
}


def _stem(f):
    """The font's stem width, px at drawing size: the inked width of its I."""
    return _ink_width(f, "I")


def _ink_width(f, ch):
    """A glyph's inked width, px (getbbox counts its side bearings)."""
    img = Image.new("L", (f.size * 2, f.size * 2), 0)
    ImageDraw.Draw(img).text((f.size // 2, f.size), ch, font=f, fill=255, anchor="ls")
    b = img.getbbox()
    return b[2] - b[0]


def _drawn_glyph(ch, f, size):
    """One of DRAWN on a canvas as the font's glyphs are drawn: (canvas, pen_x, base_y)."""
    strokes, _ = DRAWN[ch]
    h = CAP * SUPER
    w = _stem(f)
    dot_w = _ink_width(f, ".")
    pen_x, base_y = 4 * SUPER * 4, size * 2
    canvas = Image.new("L", (size * 3, size * 3), 0)
    d = ImageDraw.Draw(canvas)

    def at(x, y):
        return pen_x + x * h, base_y - y * h

    for st in strokes:
        if isinstance(st, tuple) and st[0] == "ring":
            x0, y0 = at(st[1], st[4])
            x1, y1 = at(st[3], st[2])
            d.ellipse([x0, y0, x1, y1], outline=255, width=int(round(w * 0.8)))
        elif isinstance(st, tuple) and st[0] == "dot":
            cx, cy = at(st[1], st[2])
            d.rectangle([cx - dot_w / 2.0, cy - dot_w / 2.0, cx + dot_w / 2.0,
                         cy + dot_w / 2.0], fill=255)
        else:
            pts = [at(x, y) for x, y in st]
            d.line(pts, fill=255, width=int(round(w)))
            for x, y in pts:
                d.ellipse([x - w / 2.0, y - w / 2.0, x + w / 2.0, y + w / 2.0], fill=255)
    return canvas, pen_x, base_y


def glyph_images():
    """Each printable ASCII character drawn white on clear at CAP px capitals: its sheet image
    (with a one px clear margin), the image's left edge from the pen and top from the cap line,
    in px, and its advance. The characters the font lacks are drawn from DRAWN."""
    size = _cap_size()
    f = _font(size)
    cap_top = f.getbbox("H", anchor="ls")[1]   # negative: the cap line above the baseline
    from fontTools.ttLib import TTFont
    cmap = TTFont(FONT).getBestCmap()
    out = {}
    for code in range(32, 127):
        ch = chr(code)
        if code not in cmap:
            canvas, pen_x, base_y = _drawn_glyph(ch, f, size)
            adv = DRAWN[ch][1] * CAP
        else:
            adv = f.getlength(ch) / SUPER
            if ch == " ":
                out[ch] = (None, 0, 0, adv)
                continue
            pen_x, base_y = 4 * SUPER * 4, size * 2
            canvas = Image.new("L", (size * 3, size * 3), 0)
            ImageDraw.Draw(canvas).text((pen_x, base_y), ch, font=f, fill=255, anchor="ls")
        bbox = canvas.getbbox()
        if bbox is None:
            out[ch] = (None, 0, 0, adv)
            continue
        cap_y = base_y + cap_top
        x0 = int(math.floor((bbox[0] - pen_x) / float(SUPER))) - 1
        y0 = int(math.floor((bbox[1] - cap_y) / float(SUPER))) - 1
        x1 = int(math.ceil((bbox[2] - pen_x) / float(SUPER))) + 1
        y1 = int(math.ceil((bbox[3] - cap_y) / float(SUPER))) + 1
        crop = canvas.crop((pen_x + x0 * SUPER, cap_y + y0 * SUPER,
                            pen_x + x1 * SUPER, cap_y + y1 * SUPER))
        small = crop.resize((x1 - x0, y1 - y0), Image.BOX)
        out[ch] = (small, x0, y0, adv)
    return out


def pack_glyphs(glyphs):
    """Lays the glyphs out in rows from the top of the sheet: {char: (x, y)}."""
    pos = {}
    x, y = 0, 0
    for code in range(33, 127):
        img = glyphs[chr(code)][0]
        if img is None:
            continue
        if x + img.width > SHEET:
            x, y = 0, y + GLYPH_ROW
        pos[chr(code)] = (x, y + 2)
        x += img.width + 2
    assert y + GLYPH_ROW <= GLYPH_LIMIT - 16, "the glyphs overflow the sheet"
    return pos


_GLYPHS = None


def glyphs():
    global _GLYPHS
    if _GLYPHS is None:
        _GLYPHS = glyph_images()
    return _GLYPHS


def glyph_sheet():
    g = glyphs()
    # white everywhere, clear where nothing is drawn, so a reduced glyph has no dark fringe
    sheet = Image.new("RGBA", (SHEET, SHEET), (255, 255, 255, 0))
    for ch, (x, y) in pack_glyphs(g).items():
        img = g[ch][0]
        white = Image.new("RGBA", img.size, (255, 255, 255, 255))
        white.putalpha(img)
        sheet.paste(white, (x, y))
    wx, wy, ws = WHITE_CELL
    sheet.paste(Image.new("RGBA", (ws, ws), (255, 255, 255, 255)), (wx, wy))
    return sheet


# ==========================================================================================
# The icons: drawn at SUPER x 64 in black on white, then the black taken as the mask
# ==========================================================================================
S = ICON * SUPER
BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)


def _figure(d, cx, top, h, walk=0.0):
    """A standing person, the generic signage figure: a round head and a rounded body with
    legs, h px tall from ``top``; ``walk`` spreads the legs for a walking figure."""
    r = h * 0.09
    d.ellipse([cx - r, top, cx + r, top + 2 * r], fill=BLACK)
    bt = top + 2 * r + h * 0.04
    bw = h * 0.13
    d.rounded_rectangle([cx - bw, bt, cx + bw, bt + h * 0.38], radius=bw * 0.8, fill=BLACK)
    lw = h * 0.055
    hip = bt + h * 0.34
    for side in (-1, 1):
        fx = cx + side * (bw * 0.55 + walk * h * 0.18)
        d.line([(cx + side * bw * 0.45, hip), (fx, top + h)], fill=BLACK, width=int(lw * 2))
    # arms
    for side in (-1, 1):
        sx = cx + side * bw
        d.line([(sx, bt + h * 0.05), (sx + side * (h * 0.06 + walk * h * 0.12), bt + h * 0.3)],
               fill=BLACK, width=int(lw * 1.6))


def _woman(d, cx, top, h):
    """The generic signage woman: a round head, a flared dress, arms and legs, h px tall."""
    r = h * 0.09
    d.ellipse([cx - r, top, cx + r, top + 2 * r], fill=BLACK)
    bt = top + 2 * r + h * 0.04
    hem = bt + h * 0.46
    d.polygon([(cx - h * 0.1, bt), (cx + h * 0.1, bt), (cx + h * 0.22, hem), (cx - h * 0.22, hem)],
              fill=BLACK)
    d.ellipse([cx - h * 0.1, bt - h * 0.02, cx + h * 0.1, bt + h * 0.1], fill=BLACK)
    lw = h * 0.055
    for side in (-1, 1):
        lx = cx + side * h * 0.075
        d.rectangle([lx - lw, hem - 2, lx + lw, top + h], fill=BLACK)
        sx = cx + side * h * 0.1
        d.line([(sx, bt + h * 0.04), (sx + side * h * 0.08, bt + h * 0.3)], fill=BLACK,
               width=int(lw * 1.6))


def _text(d, text, cx, cy, size, series="D", fill=BLACK):
    f = ImageFont.truetype(os.path.join(FONT_DIR, "MicaOpenHighway-Series%s.ttf" % series), size)
    d.text((cx, cy), text, font=f, fill=fill, anchor="mm")


def _arrow(d, x0, y0, x1, y1, w, head, fill=BLACK):
    """A bold arrow from (x0, y0) to its point at (x1, y1): shaft w wide, head ``head`` long."""
    a = math.atan2(y1 - y0, x1 - x0)
    ca, sa = math.cos(a), math.sin(a)
    bx, by = x1 - ca * head, y1 - sa * head
    d.line([(x0, y0), (bx, by)], fill=fill, width=int(w))
    hw = head * 0.8
    d.polygon([(x1, y1), (bx - sa * hw, by + ca * hw), (bx + sa * hw, by - ca * hw)], fill=fill)


def _image(d):
    return d._image


# --- the hospital set (moved from gen_healthcare_systems.py) --------------------------------
def picto_emergency(d):
    """An ambulance from the side, its light bar on the cab, a six-armed star on the box."""
    d.rectangle([20, 70, 170, 180], fill=BLACK)                      # box
    d.polygon([(170, 100), (205, 100), (236, 140), (236, 180), (170, 180)], fill=BLACK)
    d.polygon([(178, 108), (203, 108), (226, 138), (178, 138)], fill=WHITE)   # windscreen
    d.rectangle([150, 58, 190, 70], fill=BLACK)                      # light bar
    for wx in (62, 196):
        d.ellipse([wx - 24, 160, wx + 24, 208], fill=BLACK)
        d.ellipse([wx - 11, 173, wx + 11, 195], fill=WHITE)
    cx, cy, r = 95, 122, 36
    for a in (90, 30, 150):
        t = math.radians(a)
        dx, dy = math.cos(t) * r, -math.sin(t) * r
        d.line([(cx - dx, cy - dy), (cx + dx, cy + dy)], fill=WHITE, width=20)


def picto_xray(d):
    """A chest film: the ribs and spine light on dark."""
    d.rounded_rectangle([36, 28, 220, 228], radius=14, fill=BLACK)
    d.rectangle([122, 44, 134, 212], fill=WHITE)                     # spine
    for i in range(6):
        y = 64 + i * 24
        for side in (-1, 1):
            x0 = 128 + side * 14
            pts = [(x0, y), (x0 + side * 40, y - 10), (x0 + side * 72, y + 6),
                   (x0 + side * 76, y + 26)]
            d.line(pts, fill=WHITE, width=9, joint="curve")


def picto_lab(d):
    """A conical lab flask with liquid and two bubbles."""
    d.rectangle([108, 26, 148, 40], fill=BLACK)
    d.polygon([(112, 40), (144, 40), (144, 100), (214, 214), (42, 214), (112, 100)], fill=BLACK)
    d.polygon([(124, 52), (132, 52), (132, 104), (190, 200), (66, 200), (124, 104)], fill=WHITE)
    d.polygon([(88, 150), (168, 150), (190, 200), (66, 200)], fill=BLACK)
    d.ellipse([110, 116, 126, 132], fill=BLACK)
    d.ellipse([136, 92, 146, 102], fill=BLACK)


def picto_pharmacy(d):
    """Rx, the prescription mark, and a capsule under it."""
    _text(d, "Rx", 128, 104, 150, series="E")
    d.rounded_rectangle([56, 176, 200, 220], radius=22, fill=BLACK)
    d.rounded_rectangle([132, 184, 192, 212], radius=14, fill=WHITE)


def picto_maternity(d):
    """A parent holding a baby."""
    d.ellipse([104, 22, 152, 70], fill=BLACK)                        # head
    d.polygon([(96, 80), (160, 80), (196, 236), (60, 236)], fill=BLACK)   # dress
    d.ellipse([132, 96, 196, 160], fill=WHITE)                       # the arm's cradle
    d.ellipse([140, 104, 188, 152], fill=BLACK)                      # the baby's bundle
    d.ellipse([168, 86, 200, 118], fill=BLACK)                       # the baby's head
    d.ellipse([174, 92, 194, 112], fill=BLACK)


def picto_elevator(d):
    """A lift car with a person inside, up and down triangles beside it."""
    d.rectangle([28, 28, 168, 228], fill=BLACK)
    d.rectangle([42, 42, 154, 214], fill=WHITE)
    _figure(d, 98, 62, 140)
    d.polygon([(186, 110), (228, 110), (207, 72)], fill=BLACK)
    d.polygon([(186, 146), (228, 146), (207, 184)], fill=BLACK)


def picto_stairs(d):
    """A flight of stairs rising to the right with a person climbing it."""
    pts = [(20, 236)]
    x, y = 20, 236
    for _ in range(4):
        y -= 40
        pts.append((x, y))
        x += 54
        pts.append((x, y))
    pts += [(236, y), (236, 236)]
    d.polygon(pts, fill=BLACK)
    _figure(d, 112, 40, 110, walk=0.6)


def picto_restroom(d):
    """A man and a woman either side of a bar."""
    _figure(d, 66, 34, 196)
    d.rectangle([124, 30, 132, 226], fill=BLACK)
    # the woman: a head and a dress
    d.ellipse([172, 34, 208, 70], fill=BLACK)
    d.polygon([(170, 80), (210, 80), (234, 176), (146, 176)], fill=BLACK)
    for lx in (176, 204):
        d.rectangle([lx - 6, 176, lx + 6, 230], fill=BLACK)


def picto_cafe(d):
    """A cup of coffee on a saucer, steam rising."""
    d.rounded_rectangle([40, 214, 216, 230], radius=8, fill=BLACK)
    d.rectangle([60, 120, 172, 206], fill=BLACK)
    d.ellipse([60, 180, 172, 220], fill=BLACK)
    d.ellipse([150, 128, 214, 186], fill=BLACK)
    d.ellipse([166, 142, 198, 172], fill=WHITE)
    for sx in (86, 116, 146):
        d.line([(sx, 104), (sx - 10, 80), (sx + 4, 58), (sx - 6, 34)], fill=BLACK, width=10,
               joint="curve")


def picto_chapel(d):
    """A lit candle on a holder: a quiet room, of no one faith."""
    d.ellipse([110, 22, 146, 92], fill=BLACK)                        # flame
    d.ellipse([120, 52, 136, 86], fill=WHITE)
    d.rectangle([126, 92, 130, 104], fill=BLACK)                     # wick
    d.rectangle([98, 104, 158, 206], fill=BLACK)                     # candle
    d.rounded_rectangle([56, 206, 200, 226], radius=8, fill=BLACK)


def picto_parking(d):
    """P, for parking."""
    d.rounded_rectangle([34, 24, 222, 232], radius=18, fill=BLACK)
    d.rounded_rectangle([46, 36, 210, 220], radius=12, fill=WHITE)
    _text(d, "P", 128, 132, 210, series="E")


def picto_info(d):
    """i in a ring: information."""
    d.ellipse([22, 22, 234, 234], fill=BLACK)
    d.ellipse([40, 40, 216, 216], fill=WHITE)
    d.ellipse([112, 56, 144, 88], fill=BLACK)
    d.rectangle([112, 104, 144, 196], fill=BLACK)
    d.rectangle([98, 104, 144, 116], fill=BLACK)
    d.rectangle([98, 186, 158, 198], fill=BLACK)


def picto_accessible(d):
    """A person in a wheelchair: step-free access."""
    d.ellipse([100, 18, 136, 54], fill=BLACK)
    d.line([(112, 66), (104, 146)], fill=BLACK, width=26)            # back
    d.line([(108, 140), (176, 140), (196, 210)], fill=BLACK, width=22, joint="curve")
    d.line([(110, 92), (166, 92)], fill=BLACK, width=18)             # arm
    d.ellipse([46, 128, 166, 248], outline=BLACK, width=18)          # wheel
    d.ellipse([194, 200, 222, 228], fill=BLACK)


def picto_exit(d):
    """A person walking out through a doorway, an arrow leading out."""
    d.rectangle([140, 24, 222, 232], fill=BLACK)
    d.rectangle([154, 38, 208, 232], fill=WHITE)
    _figure(d, 182, 52, 170, walk=0.7)
    d.polygon([(26, 128), (86, 96), (86, 116), (126, 116), (126, 140), (86, 140), (86, 160)],
              fill=BLACK)


# --- the transit set (redrawn from gen_transit_airport.py's 64 px pictograms) ---------------
_PLANE = [(1.0, 0.0), (0.84, 0.1), (0.2, 0.1), (-0.22, 0.86), (-0.42, 0.86), (-0.2, 0.1),
          (-0.7, 0.1), (-0.86, 0.36), (-0.98, 0.36), (-0.9, 0.0)]


def _plane(d, cx, cy, size, angle):
    """A plane seen from above, nose along angle (degrees, 0 = right, positive = down)."""
    pts = _PLANE + [(x, -y) for x, y in reversed(_PLANE[1:])]
    a = math.radians(angle)
    ca, sa = math.cos(a), math.sin(a)
    d.polygon([(cx + (x * ca - y * sa) * size, cy + (x * sa + y * ca) * size) for x, y in pts],
              fill=BLACK)


def _bus_side(d, x0, y0, x1, y1):
    """A bus from the side, facing right: body, a band of windows, two wheels."""
    w, h = x1 - x0, y1 - y0
    d.rounded_rectangle([x0, y0, x1, y0 + h * 0.84], radius=h * 0.12, fill=BLACK)
    n = 4
    gap = w * 0.035
    ww = (w * 0.72 - gap * (n - 1)) / n
    for i in range(n):
        wx = x0 + w * 0.06 + i * (ww + gap)
        d.rectangle([wx, y0 + h * 0.14, wx + ww, y0 + h * 0.44], fill=WHITE)
    d.rectangle([x0 + w * 0.84, y0 + h * 0.14, x1 - w * 0.04, y0 + h * 0.62], fill=WHITE)
    r = h * 0.17
    for fx in (0.22, 0.76):
        cx, cy = x0 + w * fx, y0 + h * 0.83
        d.ellipse([cx - r - h * 0.05, cy - r - h * 0.05, cx + r + h * 0.05, cy + r + h * 0.05],
                  fill=WHITE)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=BLACK)


def _taxi_side(d, x0, y0, x1, y1):
    """A car from the side with a roof sign: the generic taxi."""
    w, h = x1 - x0, y1 - y0
    d.rectangle([x0 + w * 0.42, y0, x0 + w * 0.6, y0 + h * 0.16], fill=BLACK)     # roof sign
    d.polygon([(x0 + w * 0.24, y0 + h * 0.46), (x0 + w * 0.34, y0 + h * 0.2),
               (x0 + w * 0.7, y0 + h * 0.2), (x0 + w * 0.82, y0 + h * 0.46)], fill=BLACK)
    d.rounded_rectangle([x0, y0 + h * 0.42, x1, y0 + h * 0.8], radius=h * 0.1, fill=BLACK)
    d.polygon([(x0 + w * 0.31, y0 + h * 0.44), (x0 + w * 0.37, y0 + h * 0.27),
               (x0 + w * 0.5, y0 + h * 0.27), (x0 + w * 0.5, y0 + h * 0.44)], fill=WHITE)
    d.polygon([(x0 + w * 0.54, y0 + h * 0.44), (x0 + w * 0.54, y0 + h * 0.27),
               (x0 + w * 0.67, y0 + h * 0.27), (x0 + w * 0.74, y0 + h * 0.44)], fill=WHITE)
    r = h * 0.15
    for fx in (0.22, 0.78):
        cx, cy = x0 + w * fx, y0 + h * 0.8
        d.ellipse([cx - r - h * 0.05, cy - r - h * 0.05, cx + r + h * 0.05, cy + r + h * 0.05],
                  fill=WHITE)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=BLACK)


def _rail_front(d, x0, x1, top, body_bottom, ground):
    """The legs of a rail vehicle seen from the front, and the rail under it."""
    mid = (x0 + x1) / 2.0
    half = (x1 - x0) / 2.0
    for side in (-1, 1):
        d.line([(mid + side * half * 0.55, body_bottom - 4), (mid + side * half * 1.0, ground)],
               fill=BLACK, width=16)
    d.rectangle([mid - half * 1.45, ground - 4, mid + half * 1.45, ground + 12], fill=BLACK)


def picto_depart(d):
    """A plane climbing away to the upper right."""
    _plane(d, 128, 124, 110, -30)


def picto_arrive(d):
    """A plane descending to the lower right, the runway under it."""
    _plane(d, 128, 106, 102, 30)
    d.rectangle([20, 214, 236, 230], fill=BLACK)


def picto_checkin(d):
    """A person at a counter."""
    _figure(d, 70, 26, 206)
    d.rectangle([116, 116, 240, 136], fill=BLACK)
    d.rectangle([132, 136, 226, 232], fill=BLACK)


def picto_baggage(d):
    """A suitcase on the claim belt and its rollers."""
    d.rounded_rectangle([96, 26, 160, 66], radius=10, outline=BLACK, width=14)
    d.rounded_rectangle([58, 56, 198, 172], radius=12, fill=BLACK)
    d.rectangle([18, 188, 238, 206], fill=BLACK)
    for i in range(4):
        cx = 38 + i * 60
        d.ellipse([cx - 11, 216, cx + 11, 238], fill=BLACK)


def picto_ground(d):
    """Ground transport: a bus over a taxi."""
    _bus_side(d, 26, 14, 230, 118)
    _taxi_side(d, 40, 132, 216, 238)


def picto_train(d):
    """A train seen from the front: body, windscreen, two lamps, on rails."""
    d.rounded_rectangle([64, 22, 192, 190], radius=24, fill=BLACK)
    d.rounded_rectangle([80, 40, 176, 104], radius=8, fill=WHITE)
    for cx in (92, 164):
        d.ellipse([cx - 12, 140, cx + 12, 164], fill=WHITE)
    _rail_front(d, 64, 192, 22, 190, 222)


def picto_bus(d):
    """A bus from the side."""
    _bus_side(d, 14, 56, 242, 204)


def picto_taxi(d):
    """A car with a roof sign: the generic taxi."""
    _taxi_side(d, 14, 48, 242, 214)


# --- the new set (#307) ---------------------------------------------------------------------
def picto_escalator(d):
    """A moving stair rising to the right with a person standing on it."""
    d.polygon([(16, 192), (64, 192), (184, 72), (240, 72), (240, 110), (200, 110), (80, 230),
               (16, 230)], fill=BLACK)
    _figure(d, 124, 20, 116)


def picto_restroom_men(d):
    """The generic man."""
    _figure(d, 128, 16, 224)


def picto_restroom_women(d):
    """The generic woman."""
    _woman(d, 128, 16, 224)


def picto_restroom_family(d):
    """A man, a woman and a child between them."""
    _figure(d, 54, 26, 204)
    _woman(d, 196, 26, 204)
    _figure(d, 126, 118, 112)


def picto_metro(d):
    """M in a ring: an underground railway (no one city's mark)."""
    d.ellipse([14, 14, 242, 242], fill=BLACK)
    d.ellipse([36, 36, 220, 220], fill=WHITE)
    _text(d, "M", 128, 132, 168, series="E")


def picto_tram(d):
    """A tram from the front under its wire, its pantograph raised."""
    d.rectangle([12, 14, 244, 24], fill=BLACK)                      # the overhead wire
    d.line([(128, 70), (98, 46), (128, 24), (158, 46), (128, 70)], fill=BLACK, width=10)
    d.rounded_rectangle([60, 68, 196, 200], radius=18, fill=BLACK)
    d.rounded_rectangle([76, 84, 180, 138], radius=8, fill=WHITE)
    for cx in (90, 166):
        d.ellipse([cx - 11, 160, cx + 11, 182], fill=WHITE)
    _rail_front(d, 60, 196, 68, 200, 226)


def picto_food(d):
    """A fork and a knife: a place to eat."""
    for x in (60, 82, 104):                                          # the fork's tines
        d.rectangle([x, 22, x + 12, 100], fill=BLACK)
    d.rounded_rectangle([60, 88, 116, 132], radius=16, fill=BLACK)
    d.rounded_rectangle([78, 120, 98, 236], radius=8, fill=BLACK)
    d.polygon([(150, 22), (164, 22), (196, 64), (196, 134), (150, 134)], fill=BLACK)  # blade
    d.rounded_rectangle([154, 128, 186, 236], radius=10, fill=BLACK)


def picto_ticket(d):
    """A ticket: notched at its ends, a tear line across it."""
    d.rounded_rectangle([14, 60, 242, 196], radius=10, fill=BLACK)
    d.ellipse([-8, 106, 36, 150], fill=WHITE)
    d.ellipse([220, 106, 264, 150], fill=WHITE)
    for y in range(72, 190, 22):
        d.rectangle([170, y, 180, y + 11], fill=WHITE)
    d.rectangle([44, 92, 148, 108], fill=WHITE)
    d.rectangle([44, 124, 128, 140], fill=WHITE)
    d.rectangle([44, 156, 140, 172], fill=WHITE)


def picto_first_aid(d):
    """A first aid kit: a case with a handle and a cross cut out of it (never a bare cross)."""
    d.rounded_rectangle([82, 40, 174, 104], radius=16, outline=BLACK, width=18)
    d.rounded_rectangle([20, 82, 236, 226], radius=22, fill=BLACK)
    d.rectangle([112, 112, 144, 200], fill=WHITE)
    d.rectangle([84, 140, 172, 172], fill=WHITE)


def picto_fire_extinguisher(d):
    """A fire extinguisher: cylinder, valve and lever, and the hose."""
    d.rounded_rectangle([96, 76, 172, 238], radius=30, fill=BLACK)  # cylinder
    d.rectangle([118, 52, 150, 80], fill=BLACK)                     # neck
    d.rounded_rectangle([104, 34, 164, 56], radius=6, fill=BLACK)   # valve
    d.line([(152, 38), (206, 20)], fill=BLACK, width=14)            # lever
    d.line([(112, 44), (72, 52), (56, 92), (58, 150), (70, 182)], fill=BLACK, width=16,
           joint="curve")                                           # hose
    d.polygon([(58, 176), (84, 176), (80, 206), (62, 206)], fill=BLACK)   # nozzle
    d.rectangle([108, 130, 160, 168], fill=WHITE)                   # the label band


def _heart(cx, cy, k):
    pts = []
    for i in range(72):
        t = math.pi * 2 * i / 72.0
        x = 16 * math.sin(t) ** 3
        y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        pts.append((cx + x * k, cy - y * k))
    return pts


def picto_aed(d):
    """A heart with a lightning bolt: an automated external defibrillator."""
    d.polygon(_heart(128, 120, 7.2), fill=BLACK)
    d.polygon([(146, 84), (94, 152), (124, 152), (106, 204), (168, 128), (138, 128),
               (160, 84)], fill=WHITE)


def picto_wifi(d):
    """Wireless internet: a dot and three arcs over it."""
    cx, cy = 128, 204
    d.ellipse([cx - 22, cy - 22, cx + 22, cy + 22], fill=BLACK)
    for r in (70, 114, 158):
        d.arc([cx - r, cy - r, cx + r, cy + r], 225, 315, fill=BLACK, width=26)


def picto_drinking_water(d):
    """A tap running into a glass."""
    d.rounded_rectangle([60, 18, 120, 34], radius=6, fill=BLACK)     # handle
    d.rectangle([82, 30, 98, 50], fill=BLACK)
    d.rounded_rectangle([20, 46, 172, 76], radius=10, fill=BLACK)    # body
    d.rounded_rectangle([140, 46, 172, 110], radius=10, fill=BLACK)  # spout
    d.polygon([(156, 122), (142, 150), (170, 150)], fill=BLACK)      # the drop
    d.ellipse([141, 138, 171, 166], fill=BLACK)
    d.polygon([(104, 180), (208, 180), (194, 238), (118, 238)], fill=BLACK)   # glass
    d.polygon([(116, 192), (196, 192), (192, 208), (120, 208)], fill=WHITE)


def picto_telephone(d):
    """A telephone handset."""
    layer = Image.new("L", (S, S), 0)
    ld = ImageDraw.Draw(layer)
    ld.rounded_rectangle([44, 96, 212, 130], radius=16, fill=255)    # handle
    ld.rounded_rectangle([24, 104, 92, 176], radius=24, fill=255)    # ear piece
    ld.rounded_rectangle([164, 104, 232, 176], radius=24, fill=255)  # mouth piece
    layer = layer.rotate(-35, resample=Image.BICUBIC, center=(128, 138))
    _image(d).paste(Image.new("RGBA", (S, S), BLACK), (0, 0), layer)


def picto_lockers(d):
    """Three lockers side by side, vents and a handle on each door."""
    for x in (18, 92, 166):
        d.rectangle([x, 22, x + 70, 234], fill=BLACK)
        for vy in (42, 58, 74):
            d.rectangle([x + 14, vy, x + 56, vy + 7], fill=WHITE)
        d.rectangle([x + 50, 118, x + 60, 156], fill=WHITE)


def picto_baby_change(d):
    """A baby lying on a changing table."""
    d.ellipse([46, 78, 102, 134], fill=BLACK)                        # head
    d.rounded_rectangle([98, 96, 196, 136], radius=20, fill=BLACK)   # body
    d.line([(150, 100), (164, 72)], fill=BLACK, width=14)            # a raised arm
    d.line([(184, 110), (220, 88)], fill=BLACK, width=16)            # a raised leg
    d.rectangle([20, 146, 236, 168], fill=BLACK)                     # table top
    d.rectangle([20, 118, 34, 168], fill=BLACK)                      # its raised ends
    d.rectangle([222, 118, 236, 168], fill=BLACK)
    d.rectangle([34, 168, 52, 236], fill=BLACK)                      # legs
    d.rectangle([204, 168, 222, 236], fill=BLACK)


def picto_no_smoking(d):
    """A lit cigarette under a circle and slash."""
    d.rectangle([34, 132, 178, 162], fill=BLACK)
    d.rectangle([186, 132, 214, 162], fill=BLACK)
    for sx in (190, 206):
        d.line([(sx, 120), (sx - 8, 100), (sx + 4, 80), (sx - 4, 62)], fill=BLACK, width=8,
               joint="curve")
    d.line([(52, 52), (204, 204)], fill=WHITE, width=46)
    d.ellipse([10, 10, 246, 246], outline=BLACK, width=24)
    d.line([(46, 46), (210, 210)], fill=BLACK, width=24)


def picto_bicycle(d):
    """A bicycle from the side."""
    rear, front = (62, 168), (194, 168)
    for cx, cy in (rear, front):
        d.ellipse([cx - 52, cy - 52, cx + 52, cy + 52], outline=BLACK, width=14)
    crank, seat, head = (120, 168), (104, 98), (182, 98)
    for a, b in ((rear, crank), (crank, seat), (seat, rear), (seat, head), (crank, head),
                 (head, front)):
        d.line([a, b], fill=BLACK, width=12)
    d.line([(84, 88), (126, 88)], fill=BLACK, width=14)              # saddle
    d.line([(176, 80), (182, 98)], fill=BLACK, width=12)
    d.line([(166, 76), (204, 76)], fill=BLACK, width=14)             # bars
    d.ellipse([110, 158, 130, 178], fill=BLACK)


def picto_cash(d):
    """A banknote and a stack of coins: cash machine, or change."""
    d.rectangle([14, 40, 206, 144], fill=BLACK)
    d.rectangle([26, 52, 194, 132], outline=WHITE, width=5)
    d.ellipse([84, 62, 136, 114], fill=WHITE)
    d.ellipse([98, 76, 122, 100], fill=BLACK)
    for i in range(4):
        y = 222 - i * 22
        d.ellipse([138, y - 18, 242, y + 18], fill=BLACK, outline=WHITE, width=6)


def picto_shop(d):
    """A shopping bag with its handles."""
    d.arc([86, 26, 170, 126], 180, 360, fill=BLACK, width=16)
    d.polygon([(46, 88), (210, 88), (228, 234), (28, 234)], fill=BLACK)
    d.ellipse([88, 104, 104, 120], fill=WHITE)
    d.ellipse([152, 104, 168, 120], fill=WHITE)


def picto_gallery(d):
    """A framed picture: hills and the sun."""
    d.rectangle([18, 38, 238, 218], fill=BLACK)
    d.rectangle([36, 56, 220, 200], fill=WHITE)
    d.polygon([(36, 200), (100, 112), (140, 164), (172, 124), (220, 186), (220, 200)],
              fill=BLACK)
    d.ellipse([162, 70, 196, 104], fill=BLACK)


def picto_charging(d):
    """A battery with a lightning bolt: somewhere to charge a device."""
    d.rectangle([102, 18, 154, 46], fill=BLACK)
    d.rounded_rectangle([66, 40, 190, 236], radius=18, outline=BLACK, width=18)
    d.polygon([(142, 66), (94, 150), (124, 150), (110, 214), (164, 124), (134, 124),
               (154, 66)], fill=BLACK)


def picto_assembly_point(d):
    """Four arrows pointing in at a group of people: the assembly point."""
    for (x0, y0), (x1, y1) in (((22, 22), (78, 78)), ((234, 22), (178, 78)),
                               ((22, 234), (78, 178)), ((234, 234), (178, 178))):
        _arrow(d, x0, y0, x1, y1, 16, 34)
    _figure(d, 128, 84, 96)
    _figure(d, 94, 106, 78)
    _figure(d, 162, 106, 78)


def picto_security(d):
    """A shield with a tick: a security checkpoint or office."""
    d.polygon([(128, 16), (226, 48), (220, 132), (196, 186), (128, 240), (60, 186), (36, 132),
               (30, 48)], fill=BLACK)
    d.line([(80, 128), (116, 166), (180, 90)], fill=WHITE, width=28, joint="curve")


def arrow_icon(kind):
    """The arrows: straight points right, the diagonal up and to the right; the engine turns
    them."""
    big = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(big)
    shaft, head = 40, 90
    pts = [(28, 128 - shaft / 2), (150, 128 - shaft / 2), (150, 128 - head),
           (232, 128), (150, 128 + head), (150, 128 + shaft / 2), (28, 128 + shaft / 2)]
    d.polygon(pts, fill=255)
    if kind == "arrow_diagonal":
        big = big.rotate(45, resample=Image.BICUBIC, center=(128, 128))
    return big.resize((ICON, ICON), Image.LANCZOS)


# Every icon by id, in sheet order after the two arrows. Ids are saved in NBT: append only.
ICON_DRAW = [
    # the hospital panel's (#293)
    ("emergency", picto_emergency), ("xray", picto_xray), ("lab", picto_lab),
    ("pharmacy", picto_pharmacy), ("maternity", picto_maternity), ("elevator", picto_elevator),
    ("stairs", picto_stairs), ("restroom", picto_restroom), ("cafe", picto_cafe),
    ("chapel", picto_chapel), ("parking", picto_parking), ("info", picto_info),
    ("accessible", picto_accessible), ("exit", picto_exit),
    # the airport panel's
    ("depart", picto_depart), ("arrive", picto_arrive), ("checkin", picto_checkin),
    ("baggage", picto_baggage), ("ground", picto_ground), ("train", picto_train),
    ("bus", picto_bus), ("taxi", picto_taxi),
    # #307
    ("escalator", picto_escalator), ("restroom_men", picto_restroom_men),
    ("restroom_women", picto_restroom_women), ("restroom_family", picto_restroom_family),
    ("metro", picto_metro), ("tram", picto_tram), ("food", picto_food), ("ticket", picto_ticket),
    ("first_aid", picto_first_aid), ("fire_extinguisher", picto_fire_extinguisher),
    ("aed", picto_aed), ("wifi", picto_wifi),
    ("drinking_water", picto_drinking_water), ("telephone", picto_telephone),
    ("lockers", picto_lockers), ("baby_change", picto_baby_change),
    ("no_smoking", picto_no_smoking), ("bicycle", picto_bicycle), ("cash", picto_cash),
    ("shop", picto_shop), ("gallery", picto_gallery), ("charging", picto_charging),
    ("assembly_point", picto_assembly_point), ("security", picto_security),
]
ICON_IDS = [i for i, _ in ICON_DRAW]
ARROWS = ("arrow_straight", "arrow_diagonal")
assert len(ARROWS) + len(ICON_IDS) <= (SHEET // ICON) ** 2
assert len(set(ICON_IDS)) == len(ICON_IDS)


def icon_mask(name, size=ICON):
    """An icon's mask: L, 255 where the art is."""
    if name in ARROWS:
        m = arrow_icon(name)
        return m if size == ICON else m.resize((size, size), Image.LANCZOS)
    big = Image.new("RGBA", (S, S), WHITE)
    dict(ICON_DRAW)[name](ImageDraw.Draw(big))
    alpha = Image.eval(big.convert("L"), lambda v: 255 - v)
    return alpha.resize((size, size), Image.LANCZOS)


def icon_cells():
    """Each arrow's and icon's cell's top-left corner on the icon sheet: {name: (x, y)}."""
    names = list(ARROWS) + ICON_IDS
    per = SHEET // ICON
    return {n: ((i % per) * ICON, (i // per) * ICON) for i, n in enumerate(names)}


def white_mask(mask):
    out = Image.new("RGBA", mask.size, (255, 255, 255, 0))
    out.putalpha(mask)
    return out


def icon_sheet():
    sheet = Image.new("RGBA", (SHEET, SHEET), (255, 255, 255, 0))
    for name, (x, y) in icon_cells().items():
        sheet.paste(white_mask(icon_mask(name)), (x, y))
    return sheet


# ==========================================================================================
# WayfindingSheet.java
# ==========================================================================================
def _wrap_list(items, indent, width=100):
    """Comma-separated items wrapped at width, each line starting with indent."""
    lines, cur = [], indent
    for i, it in enumerate(items):
        piece = it + ("," if i < len(items) - 1 else "")
        if cur.strip() and len(cur) + 1 + len(piece) > width:
            lines.append(cur.rstrip())
            cur = indent
        cur += ("" if cur == indent else " ") + piece
    lines.append(cur.rstrip())
    return "\n".join(lines)


def sheet_java():
    g = glyphs()
    pos = pack_glyphs(g)
    rows = []
    for code in range(32, 127):
        ch = chr(code)
        img, x0, y0, adv = g[ch]
        if img is None:
            rows.append("      0, 0, 0, 0, 0, 0, %d, // %r" % (int(round(adv * 16)), ch))
        else:
            x, y = pos[ch]
            rows.append("      %d, %d, %d, %d, %d, %d, %d, // %s" % (
                x, y, img.width, img.height, x0, y0, int(round(adv * 16)),
                "'\\\\'" if ch == "\\" else repr(ch)))
    cells = icon_cells()
    ids = _wrap_list(['"%s"' % i for i in ICON_IDS], "      ")
    ixy = _wrap_list(["%d, %d" % cells[i] for i in ICON_IDS], "      ")
    wx, wy, ws = WHITE_CELL
    return """package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

/**
 * The wayfinding panels' two sheets: where each glyph is on {@code wayfinding_glyphs.png} and how
 * the glyphs are set, and where each arrow and icon is on {@code wayfinding_icons.png} (both under
 * {@code textures/blocks/wayfinding/}).
 *
 * <p>GENERATED by {@code dev-env-utils/scripts/gen_wayfinding.py} with the sheets; do not edit by
 * hand (its {@code --check} fails if the Java and the sheets disagree). An icon is an alpha mask:
 * white, the art opaque and its ground clear, so the engine draws the tile behind it and tints
 * the art.
 *
 * @since 2026.10
 */
public final class WayfindingSheet {

  /** The glyph sheet's sprite. */
  public static final String GLYPH_SPRITE = "csm:blocks/wayfinding/wayfinding_glyphs";
  /** The icon sheet's sprite. */
  public static final String ICON_SPRITE = "csm:blocks/wayfinding/wayfinding_icons";
  /** Both sheets' size, px. */
  public static final int SHEET = %d;
  /** The glyphs' cap height on the sheet, px: the unit every glyph number below is in. */
  public static final int CAP = %d;
  /** An icon's or an arrow's square on the icon sheet, px. */
  public static final int ICON = %d;

  /**
   * Per printable ASCII character from 32 (space) to 126, seven numbers: its image's x and y on
   * the glyph sheet, its width and height (0 for none), its left edge from the pen and its top
   * below the cap line (px, the top negative when it reaches above the capitals), and its advance
   * in sixteenths of a px.
   */
  static final int[] GLYPHS = {
%s
  };

  /** The icons' ids, in sheet order; saved in NBT by id, so only ever appended to. */
  static final String[] ICON_IDS = {
%s
  };
  /** Each icon's cell's top-left corner on the icon sheet, x then y. */
  static final int[] ICON_XY = {
%s
  };
  /** The straight arrow (pointing right) on the icon sheet: x, y. */
  static final int[] ARROW_STRAIGHT = {%d, %d};
  /** The diagonal arrow (up and right) on the icon sheet: x, y. */
  static final int[] ARROW_DIAGONAL = {%d, %d};
  /** A white swatch on the glyph sheet, sampled at its middle: x, y, size. */
  static final int[] WHITE = {%d, %d, %d};

  private WayfindingSheet() {
  }

  /**
   * The index of a character in {@link #GLYPHS} (times seven), or -1 for one the sheet lacks.
   *
   * @param c a character
   * @return its offset into {@link #GLYPHS}, or -1
   */
  static int offset(char c) {
    return c < 32 || c > 126 ? -1 : (c - 32) * 7;
  }

  /**
   * An icon's cell by id.
   *
   * @param id the icon's id
   * @return its cell's x and y on the icon sheet, or null for an unknown id
   */
  static int[] icon(String id) {
    for (int i = 0; i < ICON_IDS.length; i++) {
      if (ICON_IDS[i].equals(id)) {
        return new int[]{ICON_XY[2 * i], ICON_XY[2 * i + 1]};
      }
    }
    return null;
  }
}
""" % (SHEET, CAP, ICON, "\n".join(rows), ids, ixy, cells["arrow_straight"][0],
       cells["arrow_straight"][1], cells["arrow_diagonal"][0], cells["arrow_diagonal"][1],
       wx, wy, ws)


# ==========================================================================================
# The blocks' assets
# ==========================================================================================
GRAPHITE = (0x2A, 0x2C, 0x30)
YELLOW = (0xFA, 0xC8, 0x1E)
NAVY = (0x1C, 0x2A, 0x44)
STAINLESS = (186, 190, 194)
ALUMINIUM = (204, 208, 212)
ALUMINIUM_RIM = (128, 132, 138)

# registry, module, panel colour, rim colour, icon, tile colour (None: the art plain), art colour
BLOCKS = [
    ("airport_wayfinding_panel", "transit", GRAPHITE, STAINLESS, "depart", YELLOW, (0, 0, 0)),
    ("hospital_wayfinding_panel", "healthcare", NAVY, STAINLESS, "info", (250, 250, 250),
     (0, 0, 0)),
    ("wayfinding_panel", "building", ALUMINIUM, ALUMINIUM_RIM, "info", None, GRAPHITE),
]

BASE_MODEL = "csm:wayfinding/wayfinding_panel_base"
FACE_Z0, FACE_Z1 = 6.0, 10.0    # the item's slab, 4 px deep
RIM, LIP = 0.8, 0.6             # the item's frame: its width and how far it stands proud


def swatch(colour, size=16, grain=3, seed=1):
    """A flat colour with a little per-texel grain."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            dv = rng.uniform(-grain, grain)
            px[x, y] = tuple(max(0, min(255, int(round(v + dv)))) for v in colour) + (255,)
    return img


def item_face(panel, icon, tile, art):
    """The item's front: the panel colour with the pictogram on its tile (or plain)."""
    img = swatch(panel, size=64, grain=2, seed=7)
    if tile is not None:
        t0, t1 = 12, 52
        img.paste(Image.new("RGBA", (t1 - t0, t1 - t0), tile + (255,)), (t0, t0))
        a0, size = t0 + 3, (t1 - t0) - 6
    else:
        a0, size = 10, 44
    mask = icon_mask(icon, size)
    img.paste(Image.new("RGBA", (size, size), art + (255,)), (a0, a0), mask)
    return img


def display():
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.6] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.6] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.375] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                  "scale": [0.4] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                 "scale": [0.4] * 3},
    }


def _box(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv=None):
    out = {"from": list(frm), "to": list(to), "faces": {}}
    for f in faces:
        face = {"texture": tex}
        if uv:
            face["uv"] = list(uv)
        out["faces"][f] = face
    return out


def item_model(reg):
    z0, z1 = FACE_Z0 - LIP, FACE_Z1 + LIP
    f = RIM
    return {
        "parent": "block/block",
        "textures": {"face": "csm:blocks/wayfinding/%s_face" % reg,
                     "rim": "csm:blocks/wayfinding/%s_rim" % reg,
                     "particle": "csm:blocks/wayfinding/%s_rim" % reg},
        "elements": [
            _box((0, 0, FACE_Z0), (16, 16, FACE_Z1), "#face", ("north", "south"),
                 uv=(0, 0, 16, 16)),
            _box((0, 0, z0), (f, 16, z1), "#rim"),
            _box((16 - f, 0, z0), (16, 16, z1), "#rim"),
            _box((f, 16 - f, z0), (16 - f, 16, z1), "#rim"),
            _box((f, 0, z0), (16 - f, f, z1), "#rim"),
        ],
        "display": display(),
    }


def blockstate(reg):
    return {
        "forge_marker": 1,
        "defaults": {"model": BASE_MODEL},
        "variants": {
            "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}},
            "inventory": [{"model": "csm:wayfinding/%s_item" % reg}],
        },
    }


def base_model():
    # The sheets are named too, though nothing here draws them: a model's textures are stitched
    # onto the block atlas, which is where the engine's baked quads find them.
    return {"textures": {"particle": "csm:blocks/wayfinding/wayfinding_frame",
                         "glyphs": "csm:blocks/wayfinding/wayfinding_glyphs",
                         "icons": "csm:blocks/wayfinding/wayfinding_icons"}}


# ==========================================================================================
# Output
# ==========================================================================================
def json_text(data):
    return json.dumps(data, indent=2) + "\n"


def outputs():
    """Every file this script writes: {absolute path: PIL image or text}."""
    out = {}
    tex = os.path.join(CORE, "textures", "blocks", "wayfinding")
    out[os.path.join(tex, "wayfinding_glyphs.png")] = glyph_sheet()
    out[os.path.join(tex, "wayfinding_icons.png")] = icon_sheet()
    out[os.path.join(tex, "wayfinding_frame.png")] = swatch(GRAPHITE, seed=307)
    out[os.path.join(CORE, "models", "block", "wayfinding", "wayfinding_panel_base.json")] = \
        json_text(base_model())
    out[JAVA] = sheet_java()
    for reg, module, panel, rim, icon, tile, art in BLOCKS:
        root = module_assets(module)
        out[os.path.join(root, "blockstates", reg + ".json")] = json_text(blockstate(reg))
        out[os.path.join(root, "models", "block", "wayfinding", reg + "_item.json")] = \
            json_text(item_model(reg))
        out[os.path.join(root, "models", "item", reg + ".json")] = \
            json_text({"parent": "csm:block/wayfinding/%s_item" % reg})
        t = os.path.join(root, "textures", "blocks", "wayfinding")
        out[os.path.join(t, reg + "_face.png")] = item_face(panel, icon, tile, art)
        out[os.path.join(t, reg + "_rim.png")] = swatch(rim, seed=len(reg))
    return out


def _same(path, data):
    if not os.path.exists(path):
        return False
    if isinstance(data, Image.Image):
        old = Image.open(path).convert("RGBA")
        return old.size == data.size and old.tobytes() == data.convert("RGBA").tobytes()
    with open(path, encoding="utf-8", newline="") as fh:
        return fh.read().replace("\r\n", "\n") == data


def _write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if isinstance(data, Image.Image):
        data.save(path)
    else:
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(data)


# ==========================================================================================
# Review sheet
# ==========================================================================================
def review_sheet(path):
    big = 160
    small = 24
    names = list(ARROWS) + ICON_IDS
    cols = 4
    group_w = big * 4 + small * 2 + 60
    group_h = big + 34
    rows = (len(names) + cols - 1) // cols
    out = Image.new("RGBA", (cols * group_w, rows * group_h), (128, 128, 128, 255))
    d = ImageDraw.Draw(out)
    label = ImageFont.truetype(os.path.join(FONT_DIR, "MicaOpenHighway-SeriesD.ttf"), 20)
    dark, light = (32, 34, 38), (236, 236, 232)
    for i, n in enumerate(names):
        gx, gy = (i % cols) * group_w + 6, (i // cols) * group_h + 4
        d.text((gx, gy), n, font=label, fill=(255, 255, 255))
        y = gy + 26
        # read the mask back off the sheet's own 64 px cell, then scale up for viewing
        m = icon_mask(n)
        mb = m.resize((big - 16, big - 16), Image.NEAREST)
        variants = [(dark, YELLOW, (0, 0, 0)), (light, (250, 250, 250), (0, 0, 0)),
                    (dark, None, (250, 250, 250)), (light, None, GRAPHITE)]
        for k, (bg, tile, art) in enumerate(variants):
            x = gx + k * big
            d.rectangle([x, y, x + big - 4, y + big - 4], fill=bg)
            if tile is not None:
                d.rectangle([x + 4, y + 4, x + big - 8, y + big - 8], fill=tile)
            out.paste(Image.new("RGBA", mb.size, art + (255,)), (x + 6, y + 6), mb)
        # how it reads small: as it would be a third of a block across at a few blocks away
        for k, (bg, tile, art) in enumerate(variants[:2]):
            x = gx + 4 * big + 8 + k * (small + 10)
            d.rectangle([x, y, x + small + 3, y + small + 3], fill=bg)
            d.rectangle([x + 1, y + 1, x + small + 2, y + small + 2], fill=tile)
            ms = m.resize((small, small), Image.LANCZOS)
            out.paste(Image.new("RGBA", ms.size, art + (255,)), (x + 2, y + 2), ms)
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    out.save(path)
    print("wrote " + path)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--sheet", metavar="PNG")
    args = ap.parse_args()
    if args.sheet:
        review_sheet(args.sheet)
        return 0
    files = outputs()
    if args.check:
        stale = [p for p, data in files.items() if not _same(p, data)]
        if stale:
            print("out of date (re-run without --check):\n  " +
                  "\n  ".join(os.path.relpath(p, REPO) for p in stale))
            return 1
        print("gen_wayfinding.py: up to date")
        return 0
    for p, data in files.items():
        _write(p, data)
    print("wrote %d files" % len(files))
    return 0


if __name__ == "__main__":
    sys.exit(main())
