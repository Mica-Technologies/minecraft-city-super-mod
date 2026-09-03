#!/usr/bin/env python3
"""Generate the inventory/particle texture for the dynamic_street_sign block.

The dynamic street sign is drawn entirely by its TESR, so this texture never appears on the
placed block -- it exists only so the item in the creative tab and in a hand looks like what
it places. It is the counterpart to `dynamic_guide_sign.png`, which shows a miniature guide
sign; this one shows a green street blade hanging from two mast-arm hangers.

Run from the repo root:
    python dev-env-utils/scripts/gen_dynamic_street_sign_texture.py
"""

import os
import sys
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

REPO_ROOT = layout.REPO_ROOT
FONT_PATH = layout.resolve_asset("fonts/highway_gothic_wide.ttf")
OUT_PATH = layout.asset_for_write(layout.owner_of_folder("trafficaccessories"),
                                  "textures/blocks/trafficaccessories/dynamic_street_sign.png")

SIZE = 128
# Rendered at 4x and downsampled, so the Highway Gothic legend keeps its shapes instead of
# turning to mush at 128px -- the same trick the other sign texture generators use.
SUPERSAMPLE = 4

FIELD_GREEN = (11, 82, 45, 255)
LEGEND_WHITE = (245, 245, 240, 255)
FRAME_DARK = (38, 38, 42, 255)
HANGER_DARK = (46, 46, 50, 255)

NAME = "MAIN"
SUFFIX = "ST"


def main():
    scale = SUPERSAMPLE
    size = SIZE * scale
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    blade_left = 4 * scale
    blade_right = (SIZE - 4) * scale
    blade_top = 42 * scale
    blade_bottom = 92 * scale

    # Hangers: a clamp at the top where the mast arm would run, a rod, and a shoe on the
    # blade's top edge -- the same three pieces the renderer builds in 3D.
    for fraction in (0.24, 0.76):
        hx = blade_left + (blade_right - blade_left) * fraction
        clamp_w = 7 * scale
        draw.rectangle([hx - clamp_w, 8 * scale, hx + clamp_w, 15 * scale], fill=HANGER_DARK)
        draw.rectangle([hx - 2 * scale, 14 * scale, hx + 2 * scale, blade_top],
                       fill=HANGER_DARK)
        draw.rectangle([hx - 5 * scale, blade_top - 5 * scale, hx + 5 * scale, blade_top],
                       fill=HANGER_DARK)

    # Blade: dark extrusion, green field, white legend border.
    draw.rectangle([blade_left, blade_top, blade_right, blade_bottom], fill=FRAME_DARK)
    inset = 3 * scale
    draw.rectangle([blade_left + inset, blade_top + inset,
                    blade_right - inset, blade_bottom - inset], fill=LEGEND_WHITE)
    border = 2 * scale
    draw.rectangle([blade_left + inset + border, blade_top + inset + border,
                    blade_right - inset - border, blade_bottom - inset - border],
                   fill=FIELD_GREEN)

    name_font = ImageFont.truetype(FONT_PATH, 26 * scale)
    suffix_font = ImageFont.truetype(FONT_PATH, 14 * scale)

    name_box = draw.textbbox((0, 0), NAME, font=name_font)
    suffix_box = draw.textbbox((0, 0), SUFFIX, font=suffix_font)
    gap = 4 * scale
    total_width = (name_box[2] - name_box[0]) + gap + (suffix_box[2] - suffix_box[0])

    center_x = (blade_left + blade_right) / 2
    center_y = (blade_top + blade_bottom) / 2
    pen_x = center_x - total_width / 2

    # The name is centered on the blade; the suffix hangs from the name's cap line, which is
    # the raised look a real blade has.
    name_y = center_y - (name_box[3] + name_box[1]) / 2
    draw.text((pen_x - name_box[0], name_y), NAME, font=name_font, fill=LEGEND_WHITE)
    cap_top = name_y + name_box[1]
    pen_x += (name_box[2] - name_box[0]) + gap
    draw.text((pen_x - suffix_box[0], cap_top - suffix_box[1]), SUFFIX, font=suffix_font,
              fill=LEGEND_WHITE)

    image = image.resize((SIZE, SIZE), Image.LANCZOS)
    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    image.save(OUT_PATH)
    print("Wrote " + OUT_PATH)


if __name__ == "__main__":
    main()
