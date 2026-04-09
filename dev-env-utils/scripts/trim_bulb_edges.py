#!/usr/bin/env python3
"""
Trim discolored outer edge pixels from bulb textures and re-normalize.

Some bulb textures have a bright/discolored ring at the outer edge of the
circle (from the original source images). This script:
1. Groups textures by bulb style (so on/off pairs are treated together)
2. If ANY texture in a group has a discolored edge, trims ALL in the group
3. Trims by applying a smaller circular mask, then scales back up
4. Applies a clean circular mask at the target radius

This ensures on/off texture pairs stay aligned after processing.

Usage:
    python trim_bulb_edges.py [--trim-pixels N] [--threshold N] [--dry-run]
"""

import argparse
import math
import os
import sys
from PIL import Image

# Groups of textures that must be trimmed together (on/off/color variants)
TEXTURE_GROUPS = [
    # Bike LED
    ["biled_green_off", "biled_yellow_off", "biled_red_off",
     "biled_green", "biled_yellow", "biled_red"],
    # LED arrows
    ["led_leftarrowoff", "led_greenarrowleft", "led_yellowarrowleft", "led_redarrowleft"],
    # Incandescent arrows
    ["greenarrowleftoff", "yellowarrowleftoff", "redarrowleftoff",
     "greenarrowleft", "yellowarrowleft", "redarrowleft"],
    # U-turns
    ["greenuturnoff", "yellowuturnoff", "reduturnoff",
     "greenuturn", "yellowuturn", "reduturn"],
    # WLED
    ["wled_green_off", "wled_yellow_off", "wled_red_line_off",
     "wled_green", "wled_yellow", "wled_red_line"],
    # iLED
    ["iled_green_off", "iled_yellow_off", "iled_red_off",
     "iled_green", "iled_yellow", "iled_red"],
    # Incandescent ball
    ["inca_green_off", "inca_yellow_off", "inca_red_off",
     "inca_green_on", "inca_yellow_on", "inca_red_on"],
    # Incandescent arrow
    ["inca_arrow_green_off", "inca_arrow_yellow_off", "inca_arrow_red_off",
     "inca_arrow_green_on", "inca_arrow_yellow_on", "inca_arrow_red_on"],
    # eLED
    ["eled_off", "eled_green_on", "eled_yellow_on", "eled_red_on"],
    # GTX
    ["gtx_off", "gtx_green_on", "gtx_yellow_on", "gtx_red_on"],
    # WLED red X (standalone)
    ["wled_red_x"],
]

IMG_SIZE = 128
CENTER = (IMG_SIZE - 1) / 2.0  # 63.5
TARGET_RADIUS = 61.0


def analyze_edge_brightness(img):
    """Compare brightness of outer ring (r=57-60) vs inner ring (r=45-50)."""
    pixels = img.load()
    outer_bright = []
    inner_bright = []
    for y in range(IMG_SIZE):
        for x in range(IMG_SIZE):
            r, g, b, a = pixels[x, y]
            if a < 10:
                continue
            dist = math.sqrt((x - CENTER) ** 2 + (y - CENTER) ** 2)
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            if 57 <= dist <= 60:
                outer_bright.append(lum)
            elif 45 <= dist <= 50:
                inner_bright.append(lum)
    if not outer_bright or not inner_bright:
        return 0.0
    return sum(outer_bright) / len(outer_bright) - sum(inner_bright) / len(inner_bright)


def apply_circular_mask(img, radius):
    """Apply circular alpha mask with 1px anti-aliased feathering."""
    pixels = img.load()
    inner_r = radius - 1.0
    outer_r = radius + 1.0
    for y in range(IMG_SIZE):
        for x in range(IMG_SIZE):
            dist = math.sqrt((x - CENTER) ** 2 + (y - CENTER) ** 2)
            if dist > outer_r:
                pixels[x, y] = (0, 0, 0, 0)
            elif dist > inner_r:
                t = (outer_r - dist) / (outer_r - inner_r)
                r, g, b, a = pixels[x, y]
                pixels[x, y] = (r, g, b, round(a * t))
    return img


def scale_from_center(img, scale_factor):
    """Scale image content around center using bicubic interpolation."""
    if abs(scale_factor - 1.0) < 0.001:
        return img.copy()
    w, h = img.size
    new_w = round(w * scale_factor)
    new_h = round(h * scale_factor)
    scaled = img.resize((new_w, new_h), Image.BICUBIC)
    result = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    offset_x = round(CENTER - (new_w - 1) / 2.0)
    offset_y = round(CENTER - (new_h - 1) / 2.0)
    result.paste(scaled, (offset_x, offset_y))
    return result


def trim_and_rescale(path, trim_pixels):
    """Trim outer edge and rescale to target radius."""
    img = Image.open(path).convert("RGBA")
    trim_radius = TARGET_RADIUS - trim_pixels
    trimmed = apply_circular_mask(img, trim_radius)
    scale = TARGET_RADIUS / trim_radius
    scaled = scale_from_center(trimmed, scale)
    result = apply_circular_mask(scaled, TARGET_RADIUS)
    result.save(path)


def main():
    parser = argparse.ArgumentParser(description="Trim discolored bulb texture edges")
    parser.add_argument("--trim-pixels", type=float, default=3.0,
                        help="Pixels to trim from outer edge (default: 3.0)")
    parser.add_argument("--threshold", type=float, default=4.0,
                        help="Edge brightness diff threshold to trigger group trim (default: 4.0)")
    parser.add_argument("--dry-run", action="store_true",
                        help="Analyze only, don't modify files")
    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.abspath(os.path.join(script_dir, "..", ".."))
    tex_dir = os.path.join(repo_root, "src", "main", "resources", "assets", "csm",
                           "textures", "blocks", "trafficsignals", "lights")

    print(f"Trim: {args.trim_pixels}px | Threshold: {args.threshold} | {'Dry run' if args.dry_run else 'Processing'}")
    print(f"Groups are trimmed together so on/off pairs stay aligned.\n")

    total_trimmed = 0
    total_skipped = 0

    for group in TEXTURE_GROUPS:
        # Check if any texture in the group exceeds the threshold
        group_diffs = {}
        group_needs_trim = False
        for name in group:
            path = os.path.join(tex_dir, name + ".png")
            if not os.path.exists(path):
                group_diffs[name] = None
                continue
            img = Image.open(path).convert("RGBA")
            diff = analyze_edge_brightness(img)
            group_diffs[name] = diff
            if diff > args.threshold:
                group_needs_trim = True

        # Process the group
        action = "TRIM" if group_needs_trim else "skip"
        for name in group:
            diff = group_diffs[name]
            path = os.path.join(tex_dir, name + ".png")
            if diff is None:
                print(f"  {name + '.png':<35s}     --  MISSING")
                continue

            marker = "*" if diff > args.threshold else " "
            if group_needs_trim:
                if not args.dry_run:
                    trim_and_rescale(path, args.trim_pixels)
                print(f"  {name + '.png':<35s} {diff:7.1f}{marker} TRIM {'(dry-run)' if args.dry_run else 'SAVED'}")
                total_trimmed += 1
            else:
                print(f"  {name + '.png':<35s} {diff:7.1f}  skip")
                total_skipped += 1
        print()

    print(f"{'Dry-run' if args.dry_run else 'Done'}: {total_trimmed} trimmed, {total_skipped} skipped")


if __name__ == "__main__":
    main()
