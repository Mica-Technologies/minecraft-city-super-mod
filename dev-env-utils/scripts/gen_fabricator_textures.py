#!/usr/bin/env python3
"""Generate the block textures for the CSM Fabricator.

Emits three 32x32 faces into assets/csm/textures/blocks/materials/:

    fabricator_front.png   control panel with screen and buttons
    fabricator_side.png    riveted steel panel with a vent
    fabricator_top.png     steel work surface

Run from anywhere:

    python3 dev-env-utils/scripts/gen_fabricator_textures.py
"""

import os
import sys

from PIL import Image, ImageDraw

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import csm_layout as layout  # noqa: E402

REPO_ROOT = layout.REPO_ROOT
TEX_OWNER = layout.owner_of_folder("materials")
TEX_DIR = layout.asset_dir_for_write(TEX_OWNER, "textures/blocks/materials")

SIZE = 32

STEEL_HI = (188, 195, 202, 255)
STEEL = (146, 154, 162, 255)
STEEL_LO = (104, 112, 120, 255)
OUTLINE = (52, 57, 63, 255)
DARK = (34, 37, 42, 255)
SCREEN = (74, 156, 132, 255)
SCREEN_HI = (128, 214, 184, 255)
LED_RED = (222, 78, 66, 255)
LED_GRN = (86, 200, 104, 255)
LED_AMB = (232, 168, 54, 255)


def base_panel(draw, tone=STEEL):
    draw.rectangle([0, 0, SIZE - 1, SIZE - 1], fill=tone)
    draw.rectangle([0, 0, SIZE - 1, 0], fill=STEEL_HI)
    draw.rectangle([0, SIZE - 1, SIZE - 1, SIZE - 1], fill=STEEL_LO)
    draw.rectangle([0, 0, 0, SIZE - 1], fill=STEEL_HI)
    draw.rectangle([SIZE - 1, 0, SIZE - 1, SIZE - 1], fill=STEEL_LO)


def rivets(draw, inset=3):
    for x in (inset, SIZE - 1 - inset):
        for y in (inset, SIZE - 1 - inset):
            draw.point((x, y), fill=STEEL_LO)
            draw.point((x - 1, y - 1), fill=STEEL_HI)


def draw_front():
    img = Image.new("RGBA", (SIZE, SIZE))
    d = ImageDraw.Draw(img)
    base_panel(d)
    # Recessed screen.
    d.rectangle([5, 5, 26, 16], fill=DARK, outline=OUTLINE)
    d.rectangle([7, 7, 24, 14], fill=SCREEN)
    for y in range(8, 14, 2):
        d.line([(8, y), (20, y)], fill=SCREEN_HI)
    # Status lights.
    for i, col in enumerate((LED_GRN, LED_AMB, LED_RED)):
        d.rectangle([6 + i * 4, 19, 8 + i * 4, 21], fill=col, outline=OUTLINE)
    # Output chute.
    d.rectangle([18, 19, 26, 27], fill=STEEL_LO, outline=OUTLINE)
    for y in range(21, 27, 2):
        d.line([(19, y), (25, y)], fill=DARK)
    rivets(d)
    return img


def draw_side():
    img = Image.new("RGBA", (SIZE, SIZE))
    d = ImageDraw.Draw(img)
    base_panel(d)
    # Ventilation louvres.
    d.rectangle([7, 8, 24, 23], fill=STEEL_LO, outline=OUTLINE)
    for y in range(10, 23, 3):
        d.line([(9, y), (22, y)], fill=DARK)
        d.line([(9, y + 1), (22, y + 1)], fill=STEEL_HI)
    rivets(d)
    return img


def draw_top():
    img = Image.new("RGBA", (SIZE, SIZE))
    d = ImageDraw.Draw(img)
    base_panel(d, STEEL_HI)
    # Work surface inset with a tread pattern.
    d.rectangle([4, 4, 27, 27], fill=STEEL, outline=OUTLINE)
    for y in range(7, 26, 4):
        for x in range(7, 26, 4):
            d.point((x, y), fill=STEEL_LO)
            d.point((x + 1, y + 1), fill=STEEL_HI)
    rivets(d, inset=2)
    return img


def main():
    os.makedirs(TEX_DIR, exist_ok=True)
    faces = {
        "fabricator_front": draw_front(),
        "fabricator_side": draw_side(),
        "fabricator_top": draw_top(),
    }
    for name, img in faces.items():
        path = layout.asset_for_write(TEX_OWNER, "textures/blocks/materials/" + name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
    print("Generated {0} Fabricator textures -> {1}".format(len(faces), TEX_DIR))


if __name__ == "__main__":
    main()
