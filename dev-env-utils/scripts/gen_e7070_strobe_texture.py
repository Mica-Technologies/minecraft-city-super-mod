#!/usr/bin/env python3
"""
Build the texture for the Wheelock E7070's strobe module.

The E7070's plate texture is a straight-on photograph, so it has the lens seen head-on and nothing
else: no art at all for the sides of the module, which on the real appliance are a white plastic
body carrying a vertical red FIRE legend with the clear lens cap wrapping around its front. The
shipped model had no way to show that and smeared the head-on lens photo over all six faces of a
box instead.

This draws the missing art as a small atlas that the model wears as a SECOND material, so the
plate can still be retextured red or white per variant while the module -- which is white on both
-- stays put.

Atlas layout, in the 16-unit UV space, each region at its part's real aspect so nothing is drawn
pre-stretched:

    u[0, 6]     v[0, 16]  flank with the lens cap at the LEFT of the rect, for the WEST face.
    u[6, 12]    v[0, 16]  flank with the cap at the RIGHT, for the EAST face -- relaid, not
                          mirrored, so its legend still reads upright.
    u[12, 15.4] v[0, 16]  front: the lens head-on, lifted from the plate photograph so the glass is
                          the real thing rather than something drawn.
    u[15.4, 16] v[0, 16]  plain white body, for the top and bottom caps.

Two variants are needed because the flanks have to satisfy two things at once: the cap forward on
BOTH sides, and the legend readable on both. East and west run their u in opposite senses relative
to depth, so the cap has to sit at opposite ENDS of their rects -- but simply reversing a rect, or
mirroring the art, flips the legend with it. Drawing the reversed layout with upright letters is
what separates the two.

Run:  python gen_e7070_strobe_texture.py
"""

import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TEX_DIR = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "csm",
                       "textures", "blocks", "lifesafety")

SOURCE_PLATE = "wheelock_et70_red_speaker_strobe"
OUTPUT = "wheelock_e7070_strobe_module"

SIZE = 256                     # 16 px per UV unit, enough for a legible legend
UNIT = SIZE // 16

#: Where the lens sits in the plate photograph, in UV units.
PLATE_LENS_UV = (6.0, 1.6, 9.2, 14.4)

#: How much of the flank's depth the glass cap covers, as a fraction from the front.
GLASS_FRACTION = 0.46

BODY_WHITE = (238, 237, 233)
LEGEND_RED = (196, 22, 28)


def _font(size):
    for candidate in (r"C:\Windows\Fonts\arialbd.ttf", r"C:\Windows\Fonts\Arialbd.ttf",
                      "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
                      "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"):
        if os.path.exists(candidate):
            return ImageFont.truetype(candidate, size)
    return ImageFont.load_default()


def lens_crop():
    """The lens, cut out of the plate photograph."""
    plate = Image.open(os.path.join(TEX_DIR, SOURCE_PLATE + ".png")).convert("RGB")
    scale = plate.size[0] / 16.0
    u0, v0, u1, v1 = PLATE_LENS_UV
    return plate.crop((int(u0 * scale), int(v0 * scale), int(u1 * scale), int(v1 * scale)))


def draw_flank(width, height, lens, cap_side="left"):
    """The module from the side: glass cap at one end, white body with FIRE at the other.

    `cap_side` re-lays the panel rather than mirroring it, which matters: the two flanks need the
    cap at opposite ENDS of their UV rect (their u runs in opposite senses relative to depth) while
    both still read FIRE the right way round. A mirrored copy would flip the legend along with the
    layout, so the reversed variant is drawn from scratch with the letters upright.
    """
    if cap_side == "right":
        panel = draw_flank(width, height, lens, "left").transpose(Image.FLIP_LEFT_RIGHT)
        # Undo the mirror on the legend only: repaint it upright over the flipped body.
        draw = ImageDraw.Draw(panel)
        glass_width = int(width * GLASS_FRACTION)
        body_left, body_right = 0, width - glass_width
        _blank_and_letter(panel, draw, body_left, body_right, height, width)
        return panel

    panel = Image.new("RGB", (width, height), BODY_WHITE)
    draw = ImageDraw.Draw(panel)

    # Body shading -- a moulding lit from the front reads slightly darker toward the plate.
    for x in range(width):
        shade = 1.0 - 0.12 * (x / max(1, width - 1))
        draw.line([(x, 0), (x, height)], fill=tuple(int(c * shade) for c in BODY_WHITE))

    glass_width = int(width * GLASS_FRACTION)
    # Seen edge-on the lens cap is the same glass, compressed; squeezing the head-on crop is a
    # better likeness than anything drawn from scratch, and keeps the two faces the same material.
    # Straight from the plate it comes out a dark smear, though: head-on the crop is mostly the
    # shadowed reflector behind the glass, and edge-on you are looking THROUGH the cap, where the
    # light catches it. So lift it toward white and lay a specular streak down the front corner.
    glass = lens.resize((glass_width, height), Image.LANCZOS)
    glass = glass.filter(ImageFilter.GaussianBlur(0.8))
    lifted = np.asarray(glass, dtype=np.float64)
    lifted = 255.0 - (255.0 - lifted) * 0.42            # lift toward white, keep the structure
    grey = lifted.mean(2, keepdims=True)
    lifted = lifted * 0.45 + grey * 0.55                # glass is near-neutral, not tinted
    columns = np.linspace(0.0, 1.0, glass_width)[None, :, None]
    highlight = np.exp(-((columns - 0.22) ** 2) / 0.012) * 42.0
    lifted = np.clip(lifted + highlight, 0, 255)
    panel.paste(Image.fromarray(lifted.astype(np.uint8), "RGB"), (0, 0))

    # The seam where the cap clips onto the body.
    draw.line([(glass_width, 0), (glass_width, height)], fill=(176, 176, 172), width=max(1, width // 40))

    _blank_and_letter(panel, draw, glass_width, width, height, width)
    return panel


def _blank_and_letter(panel, draw, body_left, body_right, height, width):
    """Clear the body area back to plain plastic and paint FIRE down it, upright."""
    body_width = body_right - body_left
    for x in range(body_left, body_right):
        shade = 1.0 - 0.12 * (x / max(1, width - 1))
        draw.line([(x, 0), (x, height)], fill=tuple(int(c * shade) for c in BODY_WHITE))
    letters = "FIRE"
    cell = height / (len(letters) + 0.7)
    size = int(min(body_width * 0.72, cell * 0.80))
    font = _font(size)
    for index, letter in enumerate(letters):
        box = draw.textbbox((0, 0), letter, font=font)
        centre_y = cell * (index + 0.85)
        draw.text((body_left + body_width / 2 - (box[2] - box[0]) / 2 - box[0],
                   centre_y - (box[3] - box[1]) / 2 - box[1]), letter, font=font, fill=LEGEND_RED)


def main():
    lens = lens_crop()
    atlas = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))

    atlas.paste(draw_flank(6 * UNIT, 16 * UNIT, lens, "left").convert("RGBA"), (0, 0))
    atlas.paste(draw_flank(6 * UNIT, 16 * UNIT, lens, "right").convert("RGBA"), (6 * UNIT, 0))

    front = lens.resize((int(3.4 * UNIT), 16 * UNIT), Image.LANCZOS)
    atlas.paste(front.convert("RGBA"), (12 * UNIT, 0))

    plain = Image.new("RGB", (int(0.6 * UNIT) + 1, 16 * UNIT), BODY_WHITE)
    noise = np.asarray(plain, dtype=np.int16)
    rng = np.random.RandomState(7)      # fixed, so regenerating gives the same file
    noise = np.clip(noise + rng.randint(-3, 4, noise.shape), 0, 255).astype(np.uint8)
    atlas.paste(Image.fromarray(noise, "RGB").convert("RGBA"), (int(15.4 * UNIT), 0))

    # Everything the model samples is inside those three regions; fill the rest with body white so
    # a stray sample can never land on transparent.
    filled = Image.new("RGBA", (SIZE, SIZE), BODY_WHITE + (255,))
    filled.alpha_composite(atlas)

    path = os.path.join(TEX_DIR, OUTPUT + ".png")
    filled.save(path)
    print("wrote %s (%dx%d)" % (path, SIZE, SIZE))
    return 0


if __name__ == "__main__":
    sys.exit(main())
