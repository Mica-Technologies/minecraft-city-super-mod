#!/usr/bin/env python3
"""
Generates "screen off" texture variants for the iMac, iMac Pro, and MacBook Pro
blocks. Reads the original PNGs that ship with the mod, paints over the screen
UV region with a near-black, slightly reflective LCD-off color, and writes the
result beside the original as `<name>_off.png`.

The original textures are treated as the "screen on" state (they already show a
desktop wallpaper). The block flips between the two textures based on its
POWERED property — placement defaults to off (cosmetic), right-clicking turns
the screen on.

Pixel regions are derived from the Blockbench model UV coordinates in
`src/main/resources/assets/csm/models/block/technology/shared_models/{imac,imacpro,macbook_pro}.json`.
All three source textures are 128x128 with a 16-unit Blockbench UV grid, so
1 UV unit = 8 pixels.

Usage:
    python generate_computer_off_textures.py
"""

import os
from PIL import Image

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
TEX_DIR = os.path.join(
    PROJECT_ROOT, "src", "main", "resources", "assets", "csm",
    "textures", "blocks", "technology"
)

# 1 UV unit = 8 pixels at 128x128 with a 16-unit Blockbench grid.
U = 8

# Screen UV regions per model file, in Blockbench UV units. The on/off swap
# only needs to repaint the front-facing screen face — the side slivers are
# the bezel from a flat angle and can keep the original art.
TARGETS = {
    "imac": {
        "source": "imac.png",
        "out": "imac_off.png",
        # Front face (north) of the monitor element: UV (0,0)–(8,4).
        "regions": [(0, 0, 8, 4)],
    },
    "imac_pro": {
        "source": "imac_pro.png",
        "out": "imac_pro_off.png",
        "regions": [(0, 0, 8, 4)],
    },
    "macbook_pro": {
        "source": "macbook_pro.png",
        "out": "macbook_pro_off.png",
        # The MacBook screen element's north face (lid front) is UV (0,0)–(8,3.25).
        "regions": [(0, 0, 8, 3.25)],
    },
}

# Off-screen LCD color: near-black with the faintest cool tint, plus a single
# brighter highlight pixel near the top so it reads as glass rather than
# painted-on plastic. Same look across all three monitors.
SCREEN_OFF_BASE = (12, 14, 18, 255)
SCREEN_OFF_HIGHLIGHT = (32, 36, 44, 255)


def paint_region(img, region):
    """Paint a "screen off" look into the given UV region of the image."""
    x0 = int(round(region[0] * U))
    y0 = int(round(region[1] * U))
    x1 = int(round(region[2] * U))
    y1 = int(round(region[3] * U))
    px = img.load()
    width = x1 - x0
    height = y1 - y0

    # Solid dark base across the whole region.
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = SCREEN_OFF_BASE

    # Subtle horizontal highlight band near the top: gives the off panel a
    # faint "glass reflection" line so the player can tell at a glance that
    # they're looking at a screen, not a black sticker.
    band_y = y0 + max(1, int(height * 0.18))
    band_x_start = x0 + max(1, int(width * 0.06))
    band_x_end = x1 - max(1, int(width * 0.45))
    if band_x_end > band_x_start and band_y < y1:
        for x in range(band_x_start, band_x_end):
            px[x, band_y] = SCREEN_OFF_HIGHLIGHT


def main():
    for key, spec in TARGETS.items():
        src = os.path.join(TEX_DIR, spec["source"])
        dst = os.path.join(TEX_DIR, spec["out"])
        if not os.path.exists(src):
            print(f"  [skip] missing source: {src}")
            continue
        img = Image.open(src).convert("RGBA")
        for region in spec["regions"]:
            paint_region(img, region)
        img.save(dst, optimize=True)
        print(f"  wrote {dst}")


if __name__ == "__main__":
    main()
