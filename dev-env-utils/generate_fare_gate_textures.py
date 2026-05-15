#!/usr/bin/env python3
"""
Generates placeholder 16x16 textures for the realistic fare gate model:

    fare_gate_body.png            — plain brushed-stainless body (pillars, sign back)
    fare_gate_glass.png           — translucent-looking pane with a steel frame
    fare_gate_indicator_red.png   — top-of-sign band, arrow icon (closed state)
    fare_gate_indicator_green.png — top-of-sign band, arrow icon (entry-open state)
    fare_gate_indicator_x.png     — top-of-sign band, X icon (exit-open state)

The sign element on the model UV-maps the TOP 4 rows of the indicator textures to its
front/back faces; the rest of each indicator texture is dark filler that never gets
sampled. Drop in artist-quality replacements at the same paths whenever ready.

Usage:
    python generate_fare_gate_textures.py
"""

import os
from PIL import Image

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
TEX_DIR = os.path.join(
    PROJECT_ROOT, "src", "main", "resources", "assets", "csm",
    "textures", "blocks", "technology"
)

SIZE = 16

# Stainless steel body palette.
BODY_LIGHT = (190, 196, 204, 255)
BODY_DARK = (140, 146, 154, 255)
BODY_OUTLINE = (90, 96, 104, 255)

# Indicator band background + symbols.
BAND_BG = (24, 26, 30, 255)
ARROW_RED = (220, 60, 60, 255)
ARROW_GREEN = (70, 215, 90, 255)

# Glass — frosted pale blue with darker frame.
GLASS_FILL = (180, 210, 230, 255)
GLASS_HIGHLIGHT = (210, 230, 245, 255)
GLASS_FRAME = (110, 116, 124, 255)


def _draw_brushed_metal(px, x0=0, y0=0, x1=SIZE, y1=SIZE):
    """Vertical brushed-metal stripe pattern over a rectangular region."""
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = BODY_DARK if (x % 3) == 0 else BODY_LIGHT


def _draw_outline(px, x0=0, y0=0, x1=SIZE, y1=SIZE):
    for x in range(x0, x1):
        px[x, y0] = BODY_OUTLINE
        px[x, y1 - 1] = BODY_OUTLINE
    for y in range(y0, y1):
        px[x0, y] = BODY_OUTLINE
        px[x1 - 1, y] = BODY_OUTLINE


def make_body():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    _draw_brushed_metal(px)
    _draw_outline(px)
    return img


def make_glass():
    """Frosted-glass-style pane with a steel frame around the edge."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    # Body fill.
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = GLASS_FILL
    # A few brighter highlight pixels to suggest reflection.
    for y in (3, 7, 11):
        for x in (4, 5):
            px[x, y] = GLASS_HIGHLIGHT
    # Steel frame around the edge.
    _draw_outline(px)
    # Inner thin frame too — sells the "panel held in a metal frame" look.
    for x in range(2, SIZE - 2):
        px[x, 1] = GLASS_FRAME
        px[x, SIZE - 2] = GLASS_FRAME
    for y in range(2, SIZE - 2):
        px[1, y] = GLASS_FRAME
        px[SIZE - 2, y] = GLASS_FRAME
    return img


# Height of the indicator band at the top of the texture, in pixels. The model UV-samples
# the top BAND_HEIGHT rows of the texture onto the top-sign element's front face. The
# top-sign element is 10 units tall, so a 10-row band UV-maps 1:1 with no stretching.
BAND_HEIGHT = 10


def _draw_band_top(px, height=BAND_HEIGHT):
    """Dark indicator background filling the top {height} rows."""
    for y in range(height):
        for x in range(SIZE):
            px[x, y] = BAND_BG


def _draw_filler(px, top_band_height=BAND_HEIGHT):
    """Dark filler below the indicator band — not UV-sampled by the sign element."""
    for y in range(top_band_height, SIZE):
        for x in range(SIZE):
            px[x, y] = BAND_BG


def _draw_arrow_in_band(px, color, band_height=BAND_HEIGHT):
    """Right-pointing arrow filling most of the indicator band: shaft + triangular tip."""
    cy = band_height // 2
    # Shaft: 3 rows tall, columns 1..10
    for y in range(cy - 1, cy + 2):
        for x in range(1, 11):
            if 0 <= y < band_height:
                px[x, y] = color
    # Triangular tip: tapers from full thickness at the center to a single pixel at the
    # vertical extremes, extending the arrow's apex three pixels past the shaft's end.
    for dy in range(-(cy - 2), (cy - 2) + 1):
        y = cy + dy
        x_end = 12 - abs(dy)
        for x in range(8, x_end + 1):
            if 0 <= y < band_height and 0 <= x < SIZE:
                px[x, y] = color


def _draw_x_in_band(px, color, band_height=BAND_HEIGHT):
    """Bold X centered in the indicator band — 3-pixel-thick diagonal strokes."""
    cx = SIZE // 2
    cy = band_height // 2
    half = min(cy - 1, cx - 1, 4)  # half-arm length, clamped to fit the band
    for d in range(-half, half + 1):
        for thick in range(-1, 2):
            x = cx + d
            y_a = cy + d + thick
            y_b = cy - d + thick
            if 0 <= x < SIZE and 0 <= y_a < band_height:
                px[x, y_a] = color
            if 0 <= x < SIZE and 0 <= y_b < band_height:
                px[x, y_b] = color


def make_indicator_arrow(color):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    _draw_band_top(px)
    _draw_filler(px)
    _draw_arrow_in_band(px, color)
    return img


def make_indicator_x():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    _draw_band_top(px)
    _draw_filler(px)
    _draw_x_in_band(px, ARROW_RED)
    return img


def main():
    os.makedirs(TEX_DIR, exist_ok=True)
    targets = [
        ("fare_gate_body.png",            make_body()),
        ("fare_gate_glass.png",           make_glass()),
        ("fare_gate_indicator_red.png",   make_indicator_arrow(ARROW_RED)),
        ("fare_gate_indicator_green.png", make_indicator_arrow(ARROW_GREEN)),
        ("fare_gate_indicator_x.png",     make_indicator_x()),
    ]
    for filename, img in targets:
        path = os.path.join(TEX_DIR, filename)
        img.save(path, optimize=True)
        print(f"  wrote {path}")


if __name__ == "__main__":
    main()
