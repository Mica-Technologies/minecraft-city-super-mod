#!/usr/bin/env python3
"""
Build the flank textures for the Wheelock E50 speaker strobe.

The E50's plate texture is a straight-on photograph, so it has no art at all for the sides of the
enclosure -- and the sides are where a real E50 carries its FIRE legend, running down each flank in
white on the red unit and red on the white one. The models had nowhere to get that from and simply
did not show it.

Unlike the E7070's strobe module, which is white on both variants and so can share one material,
this legend inverts between them: the red flank needs white letters, the white flank red ones. So
this writes one texture per colour and the model wears whichever its blockstate names.

Atlas layout, in the 16-unit UV space:

    u[0, 2.4]  v[0, 16]  the enclosure flank: housing colour with FIRE down it, drawn at the real
                         flank's own tall aspect so the letters are not stretched.
    u[3, 7]    v[0, 4]   plain housing, for the top and bottom caps and the hidden back.

Run:  python gen_e50_flank_texture.py
"""

import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TEX_DIR = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "csm",
                       "textures", "blocks", "lifesafety")

SIZE = 256
UNIT = SIZE // 16

#: (output name, housing colour, legend colour). Sampled off the plate photographs.
VARIANTS = [
    ("wheelock_e50_flank_red", (200, 50, 45), (242, 240, 236)),
    ("wheelock_e50_flank_white", (245, 239, 240), (188, 28, 34)),
]

FLANK_WIDTH_UNITS = 2.4


def _font(size):
    for candidate in (r"C:\Windows\Fonts\arialbd.ttf", r"C:\Windows\Fonts\Arialbd.ttf",
                      "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
                      "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"):
        if os.path.exists(candidate):
            return ImageFont.truetype(candidate, size)
    return ImageFont.load_default()


def draw_flank(width, height, housing, legend):
    """The enclosure flank: plain moulding with FIRE stacked down it, letters upright."""
    panel = Image.new("RGB", (width, height), housing)
    draw = ImageDraw.Draw(panel)
    # A shallow gradient across the depth, so the flank does not read as flat paint under a light
    # that is coming from the front.
    for x in range(width):
        shade = 1.0 - 0.10 * (x / max(1, width - 1))
        draw.line([(x, 0), (x, height)], fill=tuple(int(c * shade) for c in housing))

    letters = "FIRE"
    cell = height / (len(letters) + 1.6)
    # Width-limited, not cell-limited: the flank is 2.4 units wide and 16 tall, so what caps the
    # letter is how much of the depth it may cover. A real E50's legend leaves a clear margin of
    # moulding either side of it; at 0.70 the letters ran nearly edge to edge and read as oversized
    # against the enclosure.
    size = int(min(width * 0.58, cell * 0.78))
    font = _font(size)
    for index, letter in enumerate(letters):
        box = draw.textbbox((0, 0), letter, font=font)
        centre_y = cell * (index + 1.3)
        draw.text((width / 2 - (box[2] - box[0]) / 2 - box[0],
                   centre_y - (box[3] - box[1]) / 2 - box[1]), letter, font=font, fill=legend)
    return panel


def main():
    rng = np.random.RandomState(11)     # fixed, so regenerating gives the same files
    for name, housing, legend in VARIANTS:
        atlas = Image.new("RGBA", (SIZE, SIZE), housing + (255,))
        flank = draw_flank(int(FLANK_WIDTH_UNITS * UNIT), 16 * UNIT, housing, legend)
        atlas.paste(flank.convert("RGBA"), (0, 0))

        plain = np.full((4 * UNIT, 4 * UNIT, 3), housing, dtype=np.int16)
        plain = np.clip(plain + rng.randint(-3, 4, plain.shape), 0, 255).astype(np.uint8)
        atlas.paste(Image.fromarray(plain, "RGB").convert("RGBA"), (3 * UNIT, 0))

        path = os.path.join(TEX_DIR, name + ".png")
        atlas.save(path)
        print("wrote %s (%dx%d)" % (path, SIZE, SIZE))
    return 0


if __name__ == "__main__":
    sys.exit(main())
