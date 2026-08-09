#!/usr/bin/env python3
"""Generate the textures for the CSM multi-game arcade cabinet.

The cabinet reuses the existing ``arcade_cabinet`` Blockbench model, so both textures have to keep
that model's UV layout. Two files are produced:

``shared_textures/arcade_multigame_cabinet.png``
    The cabinet body. Built from the existing cabinet texture's *luminance* only, then re-tinted
    into a distinct teal/amber CSM livery. Working from luminance keeps every panel edge, vent and
    shadow exactly where the UVs expect it while giving the machine its own colour scheme rather
    than a recolour of another cabinet's artwork.

``arcade_multigame_screen.png``
    The attract screen, drawn from scratch. Only the band the model actually samples is filled;
    the model's north face maps v=1.938..14.062 of 16, i.e. rows 15..113 of the 128px image.

Run directly; requires Pillow.

    python gen_arcade_multigame_textures.py
"""

import os
import colorsys

from PIL import Image, ImageDraw, ImageFont

# --- Paths -------------------------------------------------------------------------------------

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
BLOCKS_DIR = os.path.normpath(
    os.path.join(
        SCRIPT_DIR, "..", "..", "src", "main", "resources", "assets", "csm",
        "textures", "blocks", "novelties",
    )
)
SOURCE_BODY = os.path.join(BLOCKS_DIR, "shared_textures", "arcade_cabinet.png")
OUT_BODY = os.path.join(BLOCKS_DIR, "shared_textures", "arcade_multigame_cabinet.png")
OUT_SCREEN = os.path.join(BLOCKS_DIR, "arcade_multigame_screen.png")

SIZE = 128

# --- Livery ------------------------------------------------------------------------------------

# The body is rebuilt by mapping luminance onto this ramp: deep teal in the shadows through to a
# warm amber highlight, which reads clearly against the grey decorative cabinets.
BODY_RAMP = [
    (0.00, (10, 18, 26)),
    (0.28, (18, 44, 58)),
    (0.52, (26, 78, 96)),
    (0.72, (58, 140, 150)),
    (0.88, (196, 150, 70)),
    (1.00, (255, 214, 130)),
]

# How far around the hue wheel the source artwork's colours are turned. Half a turn takes the
# original cabinet's reds to cyan, which is what separates this machine from the others on sight.
HUE_ROTATION = 0.5

# How much of the source's colour intensity survives the rotation.
BODY_DESATURATION = 0.35

# The row at which the source cabinet's painted side panel begins. Everything below is repainted.
SIDE_ART_TOP = 64

SCREEN_BG = (4, 8, 14, 255)
SCREEN_GRID = (14, 30, 44, 255)
SCREEN_CYAN = (64, 224, 255, 255)
SCREEN_AMBER = (255, 208, 64, 255)
SCREEN_DIM = (96, 132, 156, 255)

# The bars down the attract screen stand in for the seven installed titles.
TITLE_BAR_COLORS = [
    (255, 200, 64),
    (96, 224, 160),
    (96, 176, 255),
    (64, 208, 232),
    (255, 152, 96),
    (200, 144, 255),
    (128, 232, 128),
]

# The model's north face samples v = 1.938 .. 14.062 of 16.
SCREEN_TOP = round(SIZE * 1.938 / 16.0)
SCREEN_BOTTOM = round(SIZE * 14.062 / 16.0)


def ramp_color(level):
    """Map a 0..1 luminance onto :data:`BODY_RAMP`, interpolating between stops."""
    for i in range(len(BODY_RAMP) - 1):
        low_stop, low_color = BODY_RAMP[i]
        high_stop, high_color = BODY_RAMP[i + 1]
        if level <= high_stop:
            span = high_stop - low_stop
            t = 0.0 if span <= 0 else (level - low_stop) / span
            return tuple(round(low_color[c] + (high_color[c] - low_color[c]) * t)
                         for c in range(3))
    return BODY_RAMP[-1][1]


def build_body():
    """Rebuild the cabinet body in the CSM livery, preserving the source's shading and alpha.

    Two different treatments, chosen per pixel by saturation:

    * **Neutral pixels** — the cabinet's structure — are near-grey, carry all the panel and vent
      detail, and are re-tinted along :data:`BODY_RAMP` so the machine reads teal rather than grey.
    * **Coloured pixels** — the decorative artwork — keep their own value and saturation and are
      only rotated around the hue wheel. Flattening them to one accent colour would erase the
      artwork's internal shapes, which is what a first pass at this did.
    """
    source = Image.open(SOURCE_BODY).convert("RGBA")
    width, height = source.size
    out = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    src = source.load()
    dst = out.load()

    for y in range(height):
        for x in range(width):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            hue, saturation, value = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            if saturation < 0.18:
                level = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
                dst[x, y] = ramp_color(level) + (a,)
                continue
            shifted = (hue + HUE_ROTATION) % 1.0
            # Pulling the saturation right down turns the source's woodgrain into brushed steel
            # instead of blue timber, which is what a straight hue rotation produces.
            nr, ng, nb = colorsys.hsv_to_rgb(shifted, saturation * BODY_DESATURATION,
                                             min(1.0, value * 1.08))
            dst[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)

    paint_side_art(out)

    out.save(OUT_BODY)
    print("wrote %s (%dx%d)" % (OUT_BODY, width, height))


def paint_side_art(out):
    """Replace the lower half of the body texture with original CSM side art.

    The source cabinet carries its own painted side panel in ``y = 64..127``; this cabinet is a
    different machine, so that band is repainted from scratch rather than recoloured.
    """
    draw = ImageDraw.Draw(out)
    top = SIDE_ART_TOP
    bottom = SIZE - 1

    # Night-sky gradient behind the artwork.
    for y in range(top, bottom + 1):
        t = (y - top) / float(bottom - top)
        shade = (
            round(8 + 14 * t),
            round(18 + 34 * t),
            round(30 + 46 * t),
        )
        draw.line([(0, y), (SIZE - 1, y)], fill=shade + (255,))

    # Title block across the top of the panel, drawn small and scaled up with nearest neighbour so
    # it stays crisp pixel art rather than going soft.
    _pixel_text(out, "CSM", SIZE // 2, top + 5, (255, 208, 64, 255), scale=2)
    _pixel_text(out, "ARCADE", SIZE // 2, top + 27, (64, 224, 255, 255), scale=1)

    # Livery rule under the title, clear of both lines of text.
    draw.rectangle([14, top + 40, SIZE - 15, top + 40], fill=(255, 176, 48, 255))

    # A skyline along the bottom of the panel, built from towers of varied height and width.
    skyline_base = bottom
    x = 0
    index = 0
    while x < SIZE:
        tower_width = 7 + (index * 5) % 11
        tower_height = 8 + (index * 13) % 16
        left_x = x
        right_x = min(SIZE - 1, x + tower_width - 1)
        tower_top = skyline_base - tower_height
        draw.rectangle([left_x, tower_top, right_x, skyline_base], fill=(30, 78, 100, 255))
        # A lighter parapet, so overlapping towers stay readable against each other.
        draw.rectangle([left_x, tower_top, right_x, tower_top], fill=(72, 150, 176, 255))
        for window_y in range(tower_top + 3, skyline_base - 1, 4):
            for window_x in range(left_x + 2, right_x - 1, 3):
                if (window_x * 7 + window_y * 3 + index) % 5 < 2:
                    draw.rectangle([window_x, window_y, window_x, window_y],
                                   fill=(255, 214, 130, 255))
        x += tower_width + 1
        index += 1


def _pixel_text(target, text, center_x, top_y, color, scale=1):
    """Draw ``text`` centred on ``center_x``, rendered small then scaled up by ``scale``."""
    font = ImageFont.load_default()
    measure = ImageDraw.Draw(Image.new("RGBA", (1, 1)))
    left, top, right, bottom = measure.textbbox((0, 0), text, font=font)
    width = max(1, right - left)
    height = max(1, bottom - top)
    stamp = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    ImageDraw.Draw(stamp).text((-left, -top), text, font=font, fill=color)
    if scale != 1:
        stamp = stamp.resize((width * scale, height * scale), Image.NEAREST)
    target.alpha_composite(stamp, (int(center_x - stamp.width / 2), int(top_y)))


def build_screen():
    """Draw the attract screen: a title block over a list of the installed titles."""
    out = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(out)
    font = ImageFont.load_default()

    draw.rectangle([0, SCREEN_TOP, SIZE - 1, SCREEN_BOTTOM], fill=SCREEN_BG)

    # A faint scanline grid, so the glass reads as a CRT rather than a flat panel.
    for y in range(SCREEN_TOP, SCREEN_BOTTOM + 1, 3):
        draw.line([(2, y), (SIZE - 3, y)], fill=SCREEN_GRID)

    # Title block. The box is sized to clear both lines of text with a pixel to spare.
    draw.rectangle([6, SCREEN_TOP + 4, SIZE - 7, SCREEN_TOP + 26], outline=SCREEN_CYAN)
    _centered(draw, font, "CSM", SIZE // 2, SCREEN_TOP + 5, SCREEN_AMBER)
    _centered(draw, font, "MULTI-GAME", SIZE // 2, SCREEN_TOP + 15, SCREEN_CYAN)

    # Seven bars for the seven installed titles, each with a lit "record" pip.
    top = SCREEN_TOP + 30
    bar_height = 5
    gap = 2
    for index, color in enumerate(TITLE_BAR_COLORS):
        y = top + index * (bar_height + gap)
        # Ragged bar lengths keep the list from reading as a plain equaliser.
        length = 58 + (index * 13) % 34
        draw.rectangle([10, y, 10 + length, y + bar_height - 1], fill=color + (255,))
        draw.rectangle([10, y, 12, y + bar_height - 1], fill=(255, 255, 255, 255))
        draw.rectangle([SIZE - 18, y + 1, SIZE - 14, y + bar_height - 2],
                       fill=SCREEN_DIM if index % 3 else SCREEN_AMBER)

    _centered(draw, font, "SELECT GAME", SIZE // 2, SCREEN_BOTTOM - 11, SCREEN_DIM)

    # Bezel glow around the live area.
    draw.rectangle([0, SCREEN_TOP, SIZE - 1, SCREEN_BOTTOM], outline=(24, 60, 80, 255))

    out.save(OUT_SCREEN)
    print("wrote %s (%dx%d)" % (OUT_SCREEN, SIZE, SIZE))


def _centered(draw, font, text, center_x, y, fill):
    """Draw ``text`` horizontally centred on ``center_x``."""
    left, top, right, bottom = draw.textbbox((0, 0), text, font=font)
    draw.text((center_x - (right - left) / 2, y), text, font=font, fill=fill)


if __name__ == "__main__":
    build_body()
    build_screen()
