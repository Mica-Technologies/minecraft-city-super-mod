#!/usr/bin/env python3
"""Textures for the span wire system.

Currently just the span wire tool item icon: a reel of messenger cable with a length of it
paid out. The paid-out length is drawn as an actual catenary using the same relation the
in-game solver uses, so the icon shows the curve the tool produces rather than a guessed arc.

Run directly; requires Pillow.
"""

import math
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

# Drawn at 4x and downsampled -- the only anti-aliasing that looks right for a 128px icon.
SUPERSAMPLE = 4
SIZE = 128
S = SIZE * SUPERSAMPLE

OUT_DIR = layout.asset_dir_for_write(layout.owner_of("spanwiretool"), "textures/items")

# Galvanized reel, dark steel cable. Deliberately not the yellows and reds the other tool
# icons use, so the span wire tool is distinguishable at inventory size.
FLANGE = (108, 116, 124, 255)
FLANGE_DARK = (72, 79, 86, 255)
FLANGE_EDGE = (44, 49, 54, 255)
DRUM = (86, 93, 100, 255)
CABLE = (38, 40, 44, 255)
CABLE_HI = (96, 100, 108, 255)
HANDLE = (150, 122, 60, 255)
HANDLE_DARK = (104, 84, 40, 255)


def solve_catenary(dw, dy, slack):
    """Same solve as SpanWireCatenary: returns (a, x0, y0) for y(x)=a*cosh((x-x0)/a)+y0."""
    chord = math.hypot(dw, dy)
    length = chord * slack
    target = math.sqrt(length * length - dy * dy) / dw
    if target <= 1.0:
        return None
    l = 1.0
    for _ in range(60):
        sinh, cosh = math.sinh(l), math.cosh(l)
        f = sinh / l - target
        fp = (l * cosh - sinh) / (l * l)
        step = f / fp
        l -= step
        l = max(l, 1e-12)
        if abs(step) < 1e-14:
            break
    a = dw / (2.0 * l)
    x0 = dw / 2.0 - (a / 2.0) * math.log((length + dy) / (length - dy))
    y0 = -a * math.cosh(x0 / a)
    return a, x0, y0


def catenary_points(x_start, y_start, x_end, y_end, slack, steps=64):
    """Screen-space points along a catenary. Screen y grows downward, so sag adds to y."""
    dw = x_end - x_start
    dy = y_end - y_start
    solved = solve_catenary(abs(dw), -dy, slack)
    if solved is None:
        return [(x_start, y_start), (x_end, y_end)]
    a, x0, y0 = solved
    points = []
    for i in range(steps + 1):
        t = i / steps
        x = x_start + dw * t
        y = y_start - (a * math.cosh((abs(dw) * t - x0) / a) + y0)
        points.append((x, y))
    return points


def draw_tool(draw):
    cx, cy = 0.46 * S, 0.62 * S
    flange_w = 0.085 * S
    flange_h = 0.46 * S
    drum_gap = 0.20 * S

    left_x = cx - drum_gap / 2 - flange_w
    right_x = cx + drum_gap / 2

    # Cable paid out from the top of the reel toward the upper right, sagging on the way.
    cable_pts = catenary_points(cx, cy - flange_h * 0.52, 0.95 * S, 0.20 * S, 1.10)
    draw.line(cable_pts, fill=CABLE, width=int(0.030 * S), joint="curve")
    draw.line([(x, y - 0.008 * S) for x, y in cable_pts], fill=CABLE_HI,
              width=int(0.010 * S), joint="curve")

    # Drum, with the wound cable on it.
    draw.rectangle([cx - drum_gap / 2, cy - flange_h * 0.34,
                    cx + drum_gap / 2, cy + flange_h * 0.34], fill=DRUM)
    winding = int(0.022 * S)
    y = cy - flange_h * 0.32
    while y < cy + flange_h * 0.32:
        draw.line([(cx - drum_gap / 2, y), (cx + drum_gap / 2, y)], fill=CABLE, width=winding)
        y += winding * 2

    # Flanges either side.
    for x in (left_x, right_x):
        draw.ellipse([x, cy - flange_h / 2, x + flange_w, cy + flange_h / 2], fill=FLANGE,
                     outline=FLANGE_EDGE, width=int(0.010 * S))
        draw.ellipse([x + flange_w * 0.30, cy - flange_h * 0.16,
                      x + flange_w * 0.70, cy + flange_h * 0.16], fill=FLANGE_DARK)

    # Crank handle off the right flange.
    hub_x = right_x + flange_w * 0.5
    draw.line([(hub_x, cy), (hub_x + 0.11 * S, cy - 0.11 * S)], fill=FLANGE_EDGE,
              width=int(0.026 * S))
    draw.line([(hub_x + 0.11 * S, cy - 0.11 * S), (hub_x + 0.11 * S, cy - 0.02 * S)],
              fill=HANDLE, width=int(0.034 * S))
    draw.line([(hub_x + 0.11 * S, cy - 0.075 * S), (hub_x + 0.11 * S, cy - 0.03 * S)],
              fill=HANDLE_DARK, width=int(0.034 * S))


def main():
    image = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    draw_tool(ImageDraw.Draw(image))
    image = image.resize((SIZE, SIZE), Image.LANCZOS)

    out_path = os.path.abspath(os.path.join(OUT_DIR, "span_wire_tool.png"))
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    image.save(out_path)
    print("wrote", out_path, image.size, image.mode)


if __name__ == "__main__":
    main()
