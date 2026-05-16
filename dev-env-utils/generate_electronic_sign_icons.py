#!/usr/bin/env python3
"""
Generates 32x32 inventory-icon PNGs for the five electronic sign blocks:

    overhead_message_sign.png       — dark housing, rows of amber LED dots
    portable_message_sign.png       — orange trailer, dark sign, amber LED dots
    overhead_speed_limit_sign.png   — dark housing, white panel, "SPEED LIMIT" + number
    portable_speed_limit_sign.png   — orange trailer, white panel, speed number
    polemount_speed_limit_sign.png  — speed-limit panel on a pole stub

Each is meant to be rendered via item/generated as a flat 2D sprite in the inventory
slot, replacing the prior solid-color rectangle look.

Usage:
    python generate_electronic_sign_icons.py
"""

import os
from PIL import Image, ImageDraw

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
TEX_DIR = os.path.join(
    PROJECT_ROOT, "src", "main", "resources", "assets", "csm",
    "textures", "blocks", "trafficaccessories"
)

SIZE = 32
TRANSPARENT = (0, 0, 0, 0)

# Palette
HOUSING_DARK = (40, 42, 46, 255)
HOUSING_OUTLINE = (18, 19, 22, 255)
HOUSING_HIGHLIGHT = (70, 72, 76, 255)

SIGN_BG_GRAY = (210, 210, 210, 255)
SIGN_BG_WHITE = (240, 240, 240, 255)
SIGN_BORDER_BLACK = (15, 15, 15, 255)
TEXT_BLACK = (15, 15, 15, 255)

LED_AMBER = (255, 178, 0, 255)
LED_AMBER_DIM = (140, 95, 0, 255)
LED_BG = (12, 12, 14, 255)

TRAILER_ORANGE = (235, 110, 18, 255)
TRAILER_ORANGE_DARK = (170, 78, 12, 255)
WHEEL_BLACK = (28, 28, 28, 255)
WHEEL_RIM = (110, 110, 110, 255)
TRAILER_TONGUE = (90, 90, 92, 255)
SOLAR_DARK = (28, 38, 80, 255)


def _new_canvas():
    return Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)


def _rect(draw, x0, y0, x1, y1, fill, outline=None):
    """Inclusive rect with optional outline (1px)."""
    draw.rectangle([x0, y0, x1, y1], fill=fill, outline=outline)


def _draw_digit(draw, x, y, w, h, digit, color):
    """Crude 7-segment-ish digit at (x,y) with width w, height h. Used for sub-10px
    speed numerals where TrueType rendering would be illegible."""
    seg = max(1, w // 4)
    mid_y = y + h // 2
    segments = {
        'top':    (x, y,        x + w - 1, y + seg - 1),
        'mid':    (x, mid_y - seg // 2, x + w - 1, mid_y + seg - seg // 2 - 1),
        'bot':    (x, y + h - seg, x + w - 1, y + h - 1),
        'tl':     (x, y,            x + seg - 1,  mid_y),
        'tr':     (x + w - seg, y,  x + w - 1,    mid_y),
        'bl':     (x, mid_y,        x + seg - 1,  y + h - 1),
        'br':     (x + w - seg, mid_y, x + w - 1, y + h - 1),
    }
    on = {
        '0': ['top', 'bot', 'tl', 'tr', 'bl', 'br'],
        '1': ['tr', 'br'],
        '2': ['top', 'tr', 'mid', 'bl', 'bot'],
        '3': ['top', 'tr', 'mid', 'br', 'bot'],
        '4': ['tl', 'tr', 'mid', 'br'],
        '5': ['top', 'tl', 'mid', 'br', 'bot'],
        '6': ['top', 'tl', 'mid', 'bl', 'br', 'bot'],
        '7': ['top', 'tr', 'br'],
        '8': ['top', 'tl', 'tr', 'mid', 'bl', 'br', 'bot'],
        '9': ['top', 'tl', 'tr', 'mid', 'br', 'bot'],
    }.get(digit, [])
    for s in on:
        _rect(draw, *segments[s], fill=color)


def _draw_two_digit_number(draw, cx, cy, w, h, value, color):
    """Renders a 2-digit number centered on (cx, cy)."""
    s = str(value).rjust(2)
    digit_w = w
    digit_h = h
    gap = max(1, w // 3)
    total_w = digit_w * 2 + gap
    x0 = cx - total_w // 2
    for i, ch in enumerate(s):
        dx = x0 + i * (digit_w + gap)
        if ch.isdigit():
            _draw_digit(draw, dx, cy - digit_h // 2, digit_w, digit_h, ch, color)


def _draw_speed_limit_panel(draw, x0, y0, x1, y1, speed=35):
    """White-faced US speed limit sign panel with black border, 'SPEED LIMIT'
    placeholder bars, and a 2-digit number in the lower half."""
    _rect(draw, x0, y0, x1, y1, fill=SIGN_BORDER_BLACK)
    _rect(draw, x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill=SIGN_BG_WHITE)
    panel_w = x1 - x0
    panel_h = y1 - y0
    divider = y0 + panel_h * 4 // 10
    # "SPEED" + "LIMIT" placeholder bars (too small to render real text at this scale)
    bar_color = TEXT_BLACK
    bar_y1 = y0 + 3
    bar_y2 = y0 + 5
    bar_y3 = y0 + 7
    bar_x_left = x0 + 3
    bar_x_right = x1 - 3
    if panel_h >= 12:
        draw.line([(bar_x_left, bar_y1), (bar_x_right, bar_y1)], fill=bar_color)
        draw.line([(bar_x_left + 1, bar_y3), (bar_x_right - 1, bar_y3)], fill=bar_color)
    # Speed digits in the lower portion
    digit_h = max(6, (y1 - divider) - 3)
    digit_w = max(3, digit_h // 2)
    cy = divider + (y1 - divider) // 2
    cx = (x0 + x1) // 2
    _draw_two_digit_number(draw, cx, cy, digit_w, digit_h, speed, TEXT_BLACK)


def _draw_led_dots(draw, x0, y0, x1, y1):
    """Fills a dark rect with a grid of amber dots — fakes an LED message display.
    Uses a fixed pattern that suggests two rows of text rather than rendering
    real characters at this scale."""
    _rect(draw, x0, y0, x1, y1, fill=LED_BG)
    # Pattern bitmap — 1 = lit dot, 0 = off. Two rows of "text-like" blocks.
    # Pattern is 14 cols wide, 5 rows tall. We'll center it inside the box.
    pattern = [
        "11101110011100",
        "10001000101010",
        "11101110101010",
        "10001000101010",
        "11100100011100",
    ]
    cols = len(pattern[0])
    rows = len(pattern)
    avail_w = x1 - x0 - 2
    avail_h = y1 - y0 - 2
    dot = min(avail_w // cols, avail_h // rows)
    if dot < 1:
        # Too small — just dot a few amber pixels.
        draw.point((x0 + 2, y0 + 2), fill=LED_AMBER)
        draw.point((x0 + 4, y0 + 2), fill=LED_AMBER)
        return
    grid_w = cols * dot
    grid_h = rows * dot
    ox = x0 + (x1 - x0 - grid_w) // 2
    oy = y0 + (y1 - y0 - grid_h) // 2
    for r in range(rows):
        for c in range(cols):
            if pattern[r][c] == '1':
                px = ox + c * dot
                py = oy + r * dot
                _rect(draw, px, py, px + dot - 1, py + dot - 1, fill=LED_AMBER)


def _draw_trailer_base(draw, top_y):
    """Orange trailer body + tongue + wheels along the bottom of the canvas, with
    'top_y' marking the top edge of the trailer body."""
    trailer_h = 7
    body_top = top_y
    body_bot = body_top + trailer_h - 1
    body_x0 = 4
    body_x1 = SIZE - 5
    _rect(draw, body_x0, body_top, body_x1, body_bot,
          fill=TRAILER_ORANGE, outline=TRAILER_ORANGE_DARK)
    # Trailer tongue protruding to the right
    _rect(draw, body_x1, body_top + 2, body_x1 + 2, body_top + 3, fill=TRAILER_TONGUE)
    # Wheels
    wheel_y0 = body_bot - 1
    wheel_y1 = body_bot + 3
    _rect(draw, body_x0 - 1, wheel_y0, body_x0 + 2, wheel_y1,
          fill=WHEEL_BLACK, outline=WHEEL_RIM)
    _rect(draw, body_x1 - 2, wheel_y0, body_x1 + 1, wheel_y1,
          fill=WHEEL_BLACK, outline=WHEEL_RIM)


# === Icon makers ===

def make_overhead_speed_limit():
    img = _new_canvas()
    d = ImageDraw.Draw(img)
    # Dark cabinet housing taking up most of the canvas
    box_x0, box_y0, box_x1, box_y1 = 2, 2, SIZE - 3, SIZE - 3
    _rect(d, box_x0, box_y0, box_x1, box_y1,
          fill=HOUSING_DARK, outline=HOUSING_OUTLINE)
    # Top edge highlight to suggest a 3D top
    d.line([(box_x0 + 1, box_y0 + 1), (box_x1 - 1, box_y0 + 1)], fill=HOUSING_HIGHLIGHT)
    # Inset speed-limit panel
    p_x0 = box_x0 + 4
    p_x1 = box_x1 - 4
    p_y0 = box_y0 + 3
    p_y1 = box_y1 - 3
    _draw_speed_limit_panel(d, p_x0, p_y0, p_x1, p_y1, speed=35)
    return img


def make_portable_speed_limit():
    img = _new_canvas()
    d = ImageDraw.Draw(img)
    trailer_top = SIZE - 11
    # Mast
    _rect(d, SIZE // 2 - 1, 8, SIZE // 2, trailer_top - 1, fill=HOUSING_DARK)
    # Speed-limit panel atop the mast
    p_x0, p_y0, p_x1, p_y1 = 7, 1, SIZE - 8, trailer_top - 2
    _draw_speed_limit_panel(d, p_x0, p_y0, p_x1, p_y1, speed=35)
    _draw_trailer_base(d, trailer_top)
    return img


def make_polemount_speed_limit():
    img = _new_canvas()
    d = ImageDraw.Draw(img)
    # Vertical pole on the right side
    pole_x0 = SIZE - 5
    pole_x1 = SIZE - 3
    _rect(d, pole_x0, 1, pole_x1, SIZE - 2,
          fill=HOUSING_DARK, outline=HOUSING_OUTLINE)
    # Two mounting bands connecting panel to pole
    band_x0 = pole_x0 - 4
    _rect(d, band_x0, 6, pole_x0, 8, fill=HOUSING_DARK)
    _rect(d, band_x0, SIZE - 9, pole_x0, SIZE - 7, fill=HOUSING_DARK)
    # Speed-limit panel — bigger area, on the left/center
    p_x0, p_y0, p_x1, p_y1 = 2, 3, band_x0 - 1, SIZE - 4
    _draw_speed_limit_panel(d, p_x0, p_y0, p_x1, p_y1, speed=35)
    return img


def make_overhead_message_sign():
    img = _new_canvas()
    d = ImageDraw.Draw(img)
    # Wide rectangular cabinet — wider aspect than speed limit version
    box_x0, box_y0 = 1, 6
    box_x1, box_y1 = SIZE - 2, SIZE - 7
    _rect(d, box_x0, box_y0, box_x1, box_y1,
          fill=HOUSING_DARK, outline=HOUSING_OUTLINE)
    d.line([(box_x0 + 1, box_y0 + 1), (box_x1 - 1, box_y0 + 1)], fill=HOUSING_HIGHLIGHT)
    # LED matrix area
    led_x0 = box_x0 + 2
    led_y0 = box_y0 + 2
    led_x1 = box_x1 - 2
    led_y1 = box_y1 - 2
    _draw_led_dots(d, led_x0, led_y0, led_x1, led_y1)
    # Two short mounting tabs on the top (suggesting overhead hanging brackets)
    _rect(d, box_x0 + 4, box_y0 - 3, box_x0 + 6, box_y0, fill=HOUSING_DARK)
    _rect(d, box_x1 - 6, box_y0 - 3, box_x1 - 4, box_y0, fill=HOUSING_DARK)
    return img


def make_portable_message_sign():
    img = _new_canvas()
    d = ImageDraw.Draw(img)
    trailer_top = SIZE - 11
    # Mast
    _rect(d, SIZE // 2 - 1, 12, SIZE // 2, trailer_top - 1, fill=HOUSING_DARK)
    # Sign panel — wider, shorter than speed limit
    p_x0, p_y0, p_x1, p_y1 = 3, 1, SIZE - 4, 11
    _rect(d, p_x0, p_y0, p_x1, p_y1,
          fill=HOUSING_DARK, outline=HOUSING_OUTLINE)
    # LED matrix inside panel
    _draw_led_dots(d, p_x0 + 2, p_y0 + 2, p_x1 - 2, p_y1 - 2)
    # Solar panel suggestion on top
    _rect(d, p_x0 + 4, p_y0 - 3, p_x1 - 4, p_y0 - 1, fill=SOLAR_DARK)
    _draw_trailer_base(d, trailer_top)
    return img


# === Main ===

def main():
    os.makedirs(TEX_DIR, exist_ok=True)
    targets = [
        ("overhead_speed_limit_sign.png",  make_overhead_speed_limit()),
        ("portable_speed_limit_sign.png",  make_portable_speed_limit()),
        ("polemount_speed_limit_sign.png", make_polemount_speed_limit()),
        ("overhead_message_sign.png",      make_overhead_message_sign()),
        ("portable_message_sign.png",      make_portable_message_sign()),
    ]
    for filename, img in targets:
        path = os.path.join(TEX_DIR, filename)
        img.save(path, optimize=True)
        print(f"  wrote {path}")


if __name__ == "__main__":
    main()
