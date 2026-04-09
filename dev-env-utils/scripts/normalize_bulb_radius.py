#!/usr/bin/env python3
"""
Normalize the circular radius of all traffic signal bulb textures.

Each 128x128 bulb texture is scaled so its circular content fills a consistent
target radius, then a clean circular alpha mask is applied to remove stray
edge pixels and ensure uniform circle size across all bulb styles.

Reference: GE GTX textures have the best radius/edge quality. Target radius
is slightly larger than GTX's measured max (~60.6 pixels).

Usage:
    python normalize_bulb_radius.py [--target-radius N] [--dry-run]

Reads/writes textures in:
    src/main/resources/assets/csm/textures/blocks/trafficsignals/lights/
"""

import argparse
import math
import os
import sys
from PIL import Image

# All 55 atlas input textures (from ImageTilerTool.INPUT_IMAGE_NAMES)
TEXTURE_NAMES = [
    "biled_green_off", "biled_yellow_off", "biled_red_off",
    "biled_green", "biled_yellow", "biled_red",
    "led_leftarrowoff", "led_greenarrowleft", "led_yellowarrowleft", "led_redarrowleft",
    "greenarrowleftoff", "yellowarrowleftoff", "redarrowleftoff",
    "greenarrowleft", "yellowarrowleft", "redarrowleft",
    "greenuturnoff", "yellowuturnoff", "reduturnoff",
    "greenuturn", "yellowuturn", "reduturn",
    "wled_green_off", "wled_yellow_off", "wled_red_line_off",
    "wled_green", "wled_yellow", "wled_red_line",
    "iled_green_off", "iled_yellow_off", "iled_red_off",
    "iled_green", "iled_yellow", "iled_red",
    "inca_green_off", "inca_yellow_off", "inca_red_off",
    "inca_green_on", "inca_yellow_on", "inca_red_on",
    "inca_arrow_green_off", "inca_arrow_yellow_off", "inca_arrow_red_off",
    "inca_arrow_green_on", "inca_arrow_yellow_on", "inca_arrow_red_on",
    "eled_off", "eled_green_on", "eled_yellow_on", "eled_red_on",
    "gtx_off", "gtx_green_on", "gtx_yellow_on", "gtx_red_on",
    "wled_red_x",
]

IMG_SIZE = 128
CENTER = (IMG_SIZE - 1) / 2.0  # 63.5
DEFAULT_TARGET_RADIUS = 61.0


def measure_effective_radius(img):
    """Measure the p99 radius of non-transparent pixels from the image center."""
    w, h = img.size
    cx, cy = CENTER, CENTER
    radii = []
    pixels = img.load()
    for y in range(h):
        for x in range(w):
            if pixels[x, y][3] > 10:
                r = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
                radii.append(r)
    if not radii:
        return 0.0
    radii.sort()
    return radii[int(len(radii) * 0.99)]


def scale_image_from_center(img, scale_factor):
    """Scale image content around the center point using bicubic interpolation."""
    if abs(scale_factor - 1.0) < 0.001:
        return img.copy()

    w, h = img.size
    cx, cy = CENTER, CENTER

    # Create a larger canvas, paste scaled content centered
    new_w = round(w * scale_factor)
    new_h = round(h * scale_factor)
    scaled = img.resize((new_w, new_h), Image.BICUBIC)

    # Paste centered on a transparent canvas of original size
    result = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    offset_x = round(cx - (new_w - 1) / 2.0)
    offset_y = round(cy - (new_h - 1) / 2.0)
    result.paste(scaled, (offset_x, offset_y))
    return result


def apply_circular_mask(img, target_radius):
    """Apply a clean circular alpha mask with 1px anti-aliased feathering."""
    w, h = img.size
    cx, cy = CENTER, CENTER
    pixels = img.load()
    inner_r = target_radius - 1.0  # fully opaque inside this
    outer_r = target_radius + 1.0  # fully transparent outside this

    for y in range(h):
        for x in range(w):
            dist = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if dist > outer_r:
                pixels[x, y] = (0, 0, 0, 0)
            elif dist > inner_r:
                # Anti-alias feather zone
                t = (outer_r - dist) / (outer_r - inner_r)
                r, g, b, a = pixels[x, y]
                pixels[x, y] = (r, g, b, round(a * t))
    return img


def process_texture(path, target_radius, dry_run=False):
    """Process a single bulb texture: scale to target radius and clean edges."""
    img = Image.open(path).convert("RGBA")
    current_radius = measure_effective_radius(img)

    if current_radius < 1.0:
        return current_radius, target_radius, "SKIP (no content)"

    scale_factor = target_radius / current_radius
    scaled = scale_image_from_center(img, scale_factor)
    result = apply_circular_mask(scaled, target_radius)

    status = f"scale={scale_factor:.4f}"
    if not dry_run:
        result.save(path)
        status += " SAVED"
    else:
        status += " (dry-run)"

    return current_radius, target_radius, status


def main():
    parser = argparse.ArgumentParser(description="Normalize bulb texture radii")
    parser.add_argument("--target-radius", type=float, default=DEFAULT_TARGET_RADIUS,
                        help=f"Target circle radius in pixels (default: {DEFAULT_TARGET_RADIUS})")
    parser.add_argument("--dry-run", action="store_true",
                        help="Measure and report without modifying files")
    args = parser.parse_args()

    # Find the textures directory relative to the repo root
    script_dir = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.abspath(os.path.join(script_dir, "..", ".."))
    tex_dir = os.path.join(repo_root, "src", "main", "resources", "assets", "csm",
                           "textures", "blocks", "trafficsignals", "lights")

    if not os.path.isdir(tex_dir):
        print(f"Error: texture directory not found: {tex_dir}")
        sys.exit(1)

    print(f"Target radius: {args.target_radius} pixels")
    print(f"Texture dir:   {tex_dir}")
    print(f"{'Dry run' if args.dry_run else 'Processing'}...\n")
    print(f"{'Texture':<35s} {'Before':>7s} {'After':>7s} {'Status'}")
    print("-" * 80)

    processed = 0
    for name in TEXTURE_NAMES:
        path = os.path.join(tex_dir, name + ".png")
        if not os.path.exists(path):
            print(f"{name + '.png':<35s} {'--':>7s} {'--':>7s} MISSING")
            continue
        before, after, status = process_texture(path, args.target_radius, args.dry_run)
        print(f"{name + '.png':<35s} {before:7.1f} {after:7.1f} {status}")
        processed += 1

    print(f"\n{'Dry-run complete' if args.dry_run else 'Done'}: {processed}/{len(TEXTURE_NAMES)} textures processed")


if __name__ == "__main__":
    main()
