#!/usr/bin/env python3
"""
Generates placeholder 16x16 textures for the fare gate block in its closed and open
states. The closed variant has a red status arrow band; the open variant a green band.
Replace with artist-quality textures at the same paths whenever ready — the blockstate
references them by filename only.

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

# Stainless steel body color, slightly cool.
BODY_LIGHT = (190, 196, 204, 255)
BODY_DARK = (140, 146, 154, 255)
BODY_OUTLINE = (90, 96, 104, 255)
ARROW_RED = (220, 60, 60, 255)
ARROW_GREEN = (60, 200, 80, 255)
ARROW_BG = (30, 32, 36, 255)


def _draw_body_base(px):
    """Brushed-metal body fill + outline, no symbol band."""
    for y in range(SIZE):
        for x in range(SIZE):
            if (x % 3) == 0:
                px[x, y] = BODY_DARK
            else:
                px[x, y] = BODY_LIGHT
    for x in range(SIZE):
        px[x, 0] = BODY_OUTLINE
        px[x, SIZE - 1] = BODY_OUTLINE
    for y in range(SIZE):
        px[0, y] = BODY_OUTLINE
        px[SIZE - 1, y] = BODY_OUTLINE


def _draw_band(px, band_top=5, band_bottom=11):
    """Dark plate background for the status indicator."""
    for x in range(2, SIZE - 2):
        for y in range(band_top, band_bottom):
            px[x, y] = ARROW_BG


def make_body_arrow(arrow_color):
    """Body with a single arrow (right-pointing) inside the indicator band."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    _draw_body_base(px)
    _draw_band(px)
    arrow_pts = [
        (5, 8), (6, 8), (7, 8), (8, 8), (9, 8),  # shaft
        (8, 6), (8, 7), (8, 9), (8, 10),         # tip wings (vertical)
        (9, 7), (9, 9),                          # tip outer wings
        (10, 8),                                 # tip apex
    ]
    for (x, y) in arrow_pts:
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = arrow_color
    return img


def make_body_x():
    """Body with a red X inside the indicator band — used for the exit-mode state."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    _draw_body_base(px)
    _draw_band(px)
    # Two diagonal strokes forming an X centered on the band.
    for d in range(-2, 3):
        cx = 8 + d
        cy = 8 + d
        if 0 <= cx < SIZE and 0 <= cy < SIZE:
            px[cx, cy] = ARROW_RED
        cx2 = 8 + d
        cy2 = 8 - d
        if 0 <= cx2 < SIZE and 0 <= cy2 < SIZE:
            px[cx2, cy2] = ARROW_RED
    return img


def main():
    os.makedirs(TEX_DIR, exist_ok=True)
    targets = [
        ("fare_gate_closed.png", make_body_arrow(ARROW_RED)),
        ("fare_gate_open.png",   make_body_arrow(ARROW_GREEN)),
        ("fare_gate_exit.png",   make_body_x()),
    ]
    for filename, img in targets:
        path = os.path.join(TEX_DIR, filename)
        img.save(path, optimize=True)
        print(f"  wrote {path}")


if __name__ == "__main__":
    main()
