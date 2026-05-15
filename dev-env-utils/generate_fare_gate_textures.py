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


def make_body(arrow_color):
    """Stainless body with vertical brushed-metal stripes and an arrow status band."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    # Body fill with a vertical brushed-metal stripe pattern: alternating light/dark
    # columns give the static texture a hint of metal grain.
    for y in range(SIZE):
        for x in range(SIZE):
            if (x % 3) == 0:
                px[x, y] = BODY_DARK
            else:
                px[x, y] = BODY_LIGHT
    # Outline
    for x in range(SIZE):
        px[x, 0] = BODY_OUTLINE
        px[x, SIZE - 1] = BODY_OUTLINE
    for y in range(SIZE):
        px[0, y] = BODY_OUTLINE
        px[SIZE - 1, y] = BODY_OUTLINE
    # Status arrow band: a 6-pixel-tall dark plate centered vertically with an arrow shape.
    band_top = 5
    band_bottom = 11
    for x in range(2, SIZE - 2):
        for y in range(band_top, band_bottom):
            px[x, y] = ARROW_BG
    # Arrow shape (pointing right) inside the band.
    arrow_pts = [
        (5, 8), (6, 8), (7, 8), (8, 8), (9, 8),  # shaft
        (8, 6), (8, 7), (8, 9), (8, 10),  # tip wings (vertical)
        (9, 7), (9, 9),                    # tip outer wings
        (10, 8),                           # tip apex
    ]
    for (x, y) in arrow_pts:
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = arrow_color
    return img


def main():
    os.makedirs(TEX_DIR, exist_ok=True)
    closed_path = os.path.join(TEX_DIR, "fare_gate_closed.png")
    open_path = os.path.join(TEX_DIR, "fare_gate_open.png")
    make_body(ARROW_RED).save(closed_path, optimize=True)
    print(f"  wrote {closed_path}")
    make_body(ARROW_GREEN).save(open_path, optimize=True)
    print(f"  wrote {open_path}")


if __name__ == "__main__":
    main()
