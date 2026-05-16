#!/usr/bin/env python3
"""
Generates placeholder 16x16 textures for the realistic fare gate model:

    fare_gate_body.png            — plain brushed-stainless body (pillars, sign back)
    fare_gate_glass.png           — stippled pane (CUTOUT alpha) with an opaque steel frame
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

# Glass — pale blue tint, opaque pixels arranged in a stippled pattern so the pane
# reads as "frosted glass" while staying in the CUTOUT_MIPPED render layer (binary
# alpha only, but no depth-sort artifacts at close range that TRANSLUCENT would
# cause with this multi-element model). See make_glass() for the pattern.
GLASS_FILL = (180, 210, 230, 255)
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
    """Frosted-glass-style pane with a steel frame around the edge.

    The fill uses a stippled alpha pattern (opaque pixels on a fully-transparent
    background, one in every 2x2 block) to fake semi-transparency under CUTOUT_MIPPED
    rendering. 25% pixel coverage reads as ~25% opacity from a couple blocks away —
    close to the look of true alpha blending without the depth-sort artifacts the
    block would suffer in the TRANSLUCENT layer.
    """
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    # Stippled body fill — keep pixels where both x and y are even. Uniform grid, no
    # highlight overrides, so the dot spacing reads as consistent at any zoom.
    for y in range(0, SIZE, 2):
        for x in range(0, SIZE, 2):
            px[x, y] = GLASS_FILL
    # Steel frame around the edge — fully opaque, no stippling.
    _draw_outline(px)
    # Inner thin frame too — sells the "panel held in a metal frame" look.
    for x in range(2, SIZE - 2):
        px[x, 1] = GLASS_FRAME
        px[x, SIZE - 2] = GLASS_FRAME
    for y in range(2, SIZE - 2):
        px[1, y] = GLASS_FRAME
        px[SIZE - 2, y] = GLASS_FRAME
    return img


# Indicator band size at the top-left of the texture, in pixels. The model UV-samples this
# rectangular region onto the indicator panel embedded in the wider cabinet's facing side.
# The panel is 6 units wide × 10 units tall, so a matching texture region UV-maps 1:1 with
# no stretching. Pixels outside this region aren't sampled by the model and act as filler.
BAND_WIDTH = 6
BAND_HEIGHT = 10


def _fill_dark_background(px):
    """Dark background everywhere — outside the panel region this is filler that the model
    never samples; inside the panel region it's the contrast backdrop for the icon."""
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = BAND_BG


def _draw_arrow_in_panel(px, color):
    """Right-pointing arrow in the top-left BAND_WIDTH × BAND_HEIGHT region."""
    cy = BAND_HEIGHT // 2
    # Rectangular shaft: 3 rows tall, leftmost 4 columns.
    for y in range(cy - 1, cy + 2):
        for x in range(0, 4):
            if 0 <= y < BAND_HEIGHT:
                px[x, y] = color
    # Triangular tip: tapers from full thickness at the vertical center to a single
    # pixel at the extremes, extending the apex past the shaft to col BAND_WIDTH-1.
    tip_start_x = 3
    tip_end_x = BAND_WIDTH - 1
    for dy in range(-(tip_end_x - tip_start_x), (tip_end_x - tip_start_x) + 1):
        y = cy + dy
        x_end = tip_end_x - abs(dy)
        for x in range(tip_start_x, x_end + 1):
            if 0 <= y < BAND_HEIGHT and 0 <= x < BAND_WIDTH:
                px[x, y] = color


def _draw_x_in_panel(px, color):
    """Bold X centered in the top-left BAND_WIDTH × BAND_HEIGHT region — 2-px-thick arms."""
    cx = BAND_WIDTH // 2
    cy = BAND_HEIGHT // 2
    half = min(cx, BAND_WIDTH - 1 - cx, cy, BAND_HEIGHT - 1 - cy)
    for d in range(-half, half + 1):
        for thick in (0, 1):
            x = cx + d
            y_a = cy + d + thick - 1
            y_b = cy - d + thick - 1
            if 0 <= x < BAND_WIDTH and 0 <= y_a < BAND_HEIGHT:
                px[x, y_a] = color
            if 0 <= x < BAND_WIDTH and 0 <= y_b < BAND_HEIGHT:
                px[x, y_b] = color


def make_indicator_arrow(color):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    _fill_dark_background(px)
    _draw_arrow_in_panel(px, color)
    return img


def make_indicator_x():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    _fill_dark_background(px)
    _draw_x_in_panel(px, ARROW_RED)
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
