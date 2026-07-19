#!/usr/bin/env python3
"""Render blankout box display faces in the mod's dot-matrix LED style.

The existing four faces (dw_bo, nlt_bo, nrt_bo, dne_bo) are 256x256 LED panels: a
black background, a red dotted prohibition ring where applicable, and a white
dotted legend. Dots are ~4 px across on a ~5.15 px pitch, anti-aliased.

This script renders the two additional legends in that same style:

  nut_bo   No U-Turn  -- the ring and slash are lifted pixel-for-pixel from
                         nlt_bo.png so they line up exactly with the rest of the
                         family; only the legend is redrawn.
  train_bo Train      -- a dotted locomotive outline over a TRAIN wordmark set in
                         a 5x7 LED matrix font. No ring (it is a warning, not a
                         prohibition).

Each legend also gets an ``_off`` variant: the same geometry rendered as unlit
LEDs (near-black grey), matching how the existing ``*_off`` textures read.

Run from the repo root:  python dev-env-utils/scripts/render_blankout_faces.py
Then regenerate the atlas with the "Generate Blankout Box Atlas" run config.
"""

import os
import math
from PIL import Image, ImageDraw

SIZE = 256
SS = 4  # supersample factor for anti-aliasing

DOT_PITCH = 5.15  # centre-to-centre spacing along a stroke
DOT_R = 2.0       # legend dot radius
TEXT_DOT_R = 1.7  # wordmark dot radius

WHITE = (255, 255, 255)
OFF_MAX = 6  # unlit LEDs top out at this grey value, as in the existing *_off art

TEX_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "..", "src", "main", "resources", "assets", "csm",
    "textures", "blocks", "trafficsignals", "blankout_boxes")


# --------------------------------------------------------------------------
# dot plotting
# --------------------------------------------------------------------------

def dot(draw, x, y, r, color):
    draw.ellipse([(x - r) * SS, (y - r) * SS, (x + r) * SS, (y + r) * SS], fill=color)


def dotted_path(draw, pts, color, r=DOT_R, pitch=DOT_PITCH, closed=True):
    """Walk a polyline and drop evenly spaced dots along it."""
    pts = list(pts)
    if closed:
        pts.append(pts[0])
    # Total length, then place dots at a pitch that divides it evenly so the
    # spacing stays uniform all the way around a closed path.
    total = sum(math.dist(pts[i], pts[i + 1]) for i in range(len(pts) - 1))
    count = max(1, round(total / pitch))
    step = total / count
    placed, carry = 0, 0.0
    for i in range(len(pts) - 1):
        (x1, y1), (x2, y2) = pts[i], pts[i + 1]
        seg = math.dist((x1, y1), (x2, y2))
        if seg <= 0:
            continue
        d = carry
        while d < seg and placed <= count:
            t = d / seg
            dot(draw, x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, r, color)
            placed += 1
            d += step
        carry = d - seg
    return placed


def arc_pts(cx, cy, radius, a0, a1, steps=96):
    return [(cx + radius * math.cos(math.radians(a)),
             cy + radius * math.sin(math.radians(a)))
            for a in (a0 + (a1 - a0) * i / steps for i in range(steps + 1))]


def rounded_top_rect(x0, y0, x1, y1, radius, steps=32):
    """Outline of a rectangle whose top corners are rounded (locomotive body)."""
    pts = [(x0, y1), (x0, y0 + radius)]
    pts += arc_pts(x0 + radius, y0 + radius, radius, 180, 270, steps)
    pts += [(x1 - radius, y0)]
    pts += arc_pts(x1 - radius, y0 + radius, radius, 270, 360, steps)
    pts += [(x1, y1)]
    return pts


# --------------------------------------------------------------------------
# 5x7 LED matrix font (only the glyphs the wordmark needs)
# --------------------------------------------------------------------------

FONT_5X7 = {
    "T": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
    "R": ["####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"],
    "A": [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "I": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"],
    "N": ["#...#", "##..#", "#.#.#", "#.#.#", "#..##", "#...#", "#...#"],
}


def draw_text(draw, text, cx, top, color, pitch=DOT_PITCH, r=TEXT_DOT_R, gap=1):
    glyphs = [FONT_5X7[ch] for ch in text]
    cols = len(glyphs) * 5 + (len(glyphs) - 1) * gap
    left = cx - (cols - 1) * pitch / 2.0
    x0 = left
    for g in glyphs:
        for row, bits in enumerate(g):
            for col, bit in enumerate(bits):
                if bit == "#":
                    dot(draw, x0 + col * pitch, top + row * pitch, r, color)
        x0 += (5 + gap) * pitch


# --------------------------------------------------------------------------
# legends
# --------------------------------------------------------------------------

def u_turn_outline():
    """Outline of a U-turn arrow: up the right leg, over the bend, down into a
    downward arrowhead on the left. Sized to sit inside the prohibition ring."""
    cx, yc = 128.0, 106.0     # centre of the bend
    radius, half = 34.0, 8.5  # centreline radius, half stroke width
    y_bot = 190.0             # bottom of the right leg
    y_wing = 152.0            # where the arrowhead wings start
    x_left, x_right = cx - radius, cx + radius
    wing = 19.0

    pts = [(x_right + half, y_bot), (x_right + half, yc)]
    pts += arc_pts(cx, yc, radius + half, 0, -180)          # outer bend
    pts += [(x_left - half, y_wing), (x_left - wing, y_wing),
            (x_left, y_bot),                                 # arrow tip
            (x_left + wing, y_wing), (x_left + half, y_wing)]
    pts += arc_pts(cx, yc, radius - half, -180, 0)           # inner bend
    pts += [(x_right - half, y_bot)]
    return pts


def render_no_u_turn():
    """Ring + slash lifted from nlt_bo, legend redrawn as a U-turn arrow."""
    src = Image.open(os.path.join(TEX_DIR, "nlt_bo.png")).convert("RGB")
    base = Image.new("RGB", (SIZE, SIZE), (0, 0, 0))
    # Keep only the red ring/slash: the existing white legend dots are dropped by
    # discarding any pixel whose channels are all lit (i.e. white, not red).
    px_src, px_dst = src.load(), base.load()
    for y in range(SIZE):
        for x in range(SIZE):
            r, g, b = px_src[x, y]
            px_dst[x, y] = (0, 0, 0) if min(r, g, b) > 24 else (r, g, b)

    layer = Image.new("RGB", (SIZE * SS, SIZE * SS), (0, 0, 0))
    dotted_path(ImageDraw.Draw(layer), u_turn_outline(), WHITE)
    legend = layer.resize((SIZE, SIZE), Image.LANCZOS)
    return Image.fromarray(_max_blend(base, legend))


def render_train():
    """Dotted locomotive outline above a TRAIN wordmark."""
    layer = Image.new("RGB", (SIZE * SS, SIZE * SS), (0, 0, 0))
    d = ImageDraw.Draw(layer)

    # Locomotive body: rounded-top box on a straight sill.
    dotted_path(d, rounded_top_rect(74, 44, 182, 150, 30), WHITE)
    # Windshield.
    dotted_path(d, rounded_top_rect(94, 62, 162, 104, 12), WHITE)
    # Headlights.
    dotted_path(d, arc_pts(101, 128, 8, 0, 360), WHITE, closed=False)
    dotted_path(d, arc_pts(155, 128, 8, 0, 360), WHITE, closed=False)
    # Sill / pilot below the body.
    dotted_path(d, [(64, 160), (192, 160)], WHITE, closed=False)

    draw_text(d, "TRAIN", 128, 184, WHITE)
    return layer.resize((SIZE, SIZE), Image.LANCZOS)


def _max_blend(a, b):
    import numpy as np
    return np.maximum(np.array(a), np.array(b))


def make_off(img):
    """Unlit version: same geometry, dropped to the near-black grey of the
    existing *_off textures."""
    import numpy as np
    a = np.array(img).astype(float)
    lum = a.max(axis=2) / 255.0
    grey = np.round(lum * OFF_MAX).astype("uint8")
    return Image.fromarray(np.dstack([grey, grey, grey]))


def main():
    os.makedirs(TEX_DIR, exist_ok=True)
    for name, img in (("nut_bo", render_no_u_turn()), ("train_bo", render_train())):
        img.convert("RGBA").save(os.path.join(TEX_DIR, name + ".png"))
        make_off(img).convert("RGBA").save(os.path.join(TEX_DIR, name + "_off.png"))
        print("wrote", name + ".png", "and", name + "_off.png")


if __name__ == "__main__":
    main()
