#!/usr/bin/env python3
"""
Generates placeholder 16x16 textures for the fare ticket and transit card items.
These are intentionally simple — you can drop in artist-quality replacements later by
overwriting the same filenames; the item models reference the texture by name only.

Usage:
    python generate_fare_item_textures.py
"""

import os
from PIL import Image

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
TEX_DIR = os.path.join(
    PROJECT_ROOT, "src", "main", "resources", "assets", "csm", "textures", "items"
)

SIZE = 16


def make_fare_ticket():
    """Yellow paper ticket with a perforated stub on the left and a 'TKT' band."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    body = (240, 200, 80, 255)        # warm yellow ticket body
    body_dark = (180, 130, 50, 255)   # darker outline
    band = (60, 60, 60, 255)          # dark stripe across the middle
    perf = (200, 160, 60, 255)        # perforation column color

    # Outer body
    for y in range(2, 14):
        for x in range(1, 15):
            px[x, y] = body
    # Outline
    for x in range(1, 15):
        px[x, 2] = body_dark
        px[x, 13] = body_dark
    for y in range(2, 14):
        px[1, y] = body_dark
        px[14, y] = body_dark
    # Perforation column on the left
    for y in range(3, 13):
        if y % 2 == 0:
            px[4, y] = perf
    # Black "stripe" / mag-stripe band across the middle
    for x in range(5, 14):
        px[x, 7] = band
        px[x, 8] = band
    return img


def make_transit_card():
    """Blue plastic card with rounded corners hint, white chip, and a stripe."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    body = (40, 110, 180, 255)        # transit blue
    body_dark = (20, 70, 130, 255)    # outline / shadow
    chip = (220, 220, 220, 255)       # silver chip
    chip_dark = (170, 170, 170, 255)
    stripe = (15, 30, 50, 255)        # dark bottom stripe

    # Body
    for y in range(3, 13):
        for x in range(1, 15):
            px[x, y] = body
    # Rounded-corner hint: clear the four extreme corner pixels
    for (cx, cy) in [(1, 3), (14, 3), (1, 12), (14, 12)]:
        px[cx, cy] = body_dark
    # Outline
    for x in range(1, 15):
        px[x, 3] = body_dark
        px[x, 12] = body_dark
    for y in range(3, 13):
        px[1, y] = body_dark
        px[14, y] = body_dark
    # Bottom stripe (mag stripe)
    for x in range(2, 14):
        px[x, 11] = stripe
    # Silver chip in upper-left area
    for y in range(5, 8):
        for x in range(3, 6):
            px[x, y] = chip
    # Chip shading
    for x in range(3, 6):
        px[x, 7] = chip_dark
    return img


def main():
    os.makedirs(TEX_DIR, exist_ok=True)
    fareticket_path = os.path.join(TEX_DIR, "fareticket.png")
    transitcard_path = os.path.join(TEX_DIR, "transitcard.png")
    make_fare_ticket().save(fareticket_path, optimize=True)
    print(f"  wrote {fareticket_path}")
    make_transit_card().save(transitcard_path, optimize=True)
    print(f"  wrote {transitcard_path}")


if __name__ == "__main__":
    main()
