#!/usr/bin/env python3
"""
Pull a fire alarm texture's red housing onto the same red the rest of its family already uses.

Textures come from all sorts of sources and their reds do not agree. The Wheelock ET24, for
instance, arrived at hue -16 degrees and 0.76 saturation -- a crimson -- next to siblings that sit
at hue 0 and 0.86, so on a wall beside an MT or an ET70 it read as a different colour of plastic.

The correction is a weighted hue and saturation shift that leaves each pixel's VALUE alone, so the
moulding keeps its bevel, its highlights and the shadow under its screws; only the pigment moves.
The weight is how red-dominant and saturated a pixel already is, ramped smoothly to zero, so a
frosted lens, grey screw heads and the dark slots of a grille are untouched and there is no hard
seam where the housing meets them.

It measures the target from the family every time and shifts the difference, which makes it
idempotent: run it twice and the second pass finds the texture already on target and moves nothing.

Usage:
    python normalize_alarm_red.py <texture-name> [--family NAME ...] [--dry-run]

    <texture-name> is the file's stem under textures/blocks/lifesafety/, no extension.
    --family defaults to the Wheelock red appliances.
"""

import argparse
import colorsys
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

REPO_ROOT = layout.REPO_ROOT
LIFESAFETY_OWNER = layout.owner_of_folder("lifesafety")


def _tex_path(name):
    return (layout.resolve_asset("textures/blocks/lifesafety/" + name + ".png")
           or layout.asset_for_write(LIFESAFETY_OWNER,
                                     "textures/blocks/lifesafety/" + name + ".png"))

WHEELOCK_RED_FAMILY = [
    "wheelock_mt_red_horn_strobe", "wheelock_mt_red_horn", "wheelock_as_red_horn_strobe",
    "wheelock_e50_red_speaker_strobe", "wheelock_e70_red_speaker",
    "wheelock_et70_red_speaker_strobe", "wheelock_et80_red_horn_strobe",
    "wheelock_exceeder_red_horn_strobe", "wheelock_7002t_red_horn_strobe",
]


def _load(name):
    return np.asarray(Image.open(_tex_path(name)).convert("RGBA"),
                      dtype=np.float64)


def _hsv(rgb):
    """Vectorised RGB->HSV on a HxWx3 array of 0..1 floats. Hue is degrees in (-180, 180]."""
    high = rgb.max(2)
    low = rgb.min(2)
    span = high - low
    red, green, blue = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    hue = np.zeros_like(high)
    safe = span > 1e-9
    with np.errstate(invalid="ignore", divide="ignore"):
        hue = np.where(safe & (high == red), ((green - blue) / np.where(safe, span, 1)) % 6, hue)
        hue = np.where(safe & (high == green), (blue - red) / np.where(safe, span, 1) + 2, hue)
        hue = np.where(safe & (high == blue), (red - green) / np.where(safe, span, 1) + 4, hue)
    hue = (hue * 60.0 + 180.0) % 360.0 - 180.0
    saturation = np.where(high > 1e-9, span / np.where(high > 1e-9, high, 1), 0.0)
    return hue, saturation, high


def _rgb(hue, saturation, value):
    hue = (hue % 360.0) / 360.0
    flat = np.stack([hue.ravel(), saturation.ravel(), value.ravel()], axis=1)
    out = np.array([colorsys.hsv_to_rgb(h, s, v) for h, s, v in flat])
    return out.reshape(hue.shape + (3,))


def red_weight(rgb, alpha):
    """How much a pixel counts as red housing plastic: 1 deep in the moulding, 0 on the lens,
    the screws and the dark of a grille slot, ramped smoothly between so no seam appears."""
    dominance = rgb[..., 0] - np.maximum(rgb[..., 1], rgb[..., 2])
    _, saturation, value = _hsv(rgb)
    weight = np.clip(dominance / 0.20, 0.0, 1.0)
    weight *= np.clip((saturation - 0.25) / 0.25, 0.0, 1.0)
    weight *= np.clip((value - 0.06) / 0.10, 0.0, 1.0)
    return np.where(alpha > 250, weight, 0.0)


def measure(name):
    """Median hue and saturation of a texture's red housing, weighted as above."""
    data = _load(name)
    rgb = data[..., :3] / 255.0
    weight = red_weight(rgb, data[..., 3])
    hue, saturation, _ = _hsv(rgb)
    strong = weight > 0.6
    if strong.sum() < 200:
        return None
    return float(np.median(hue[strong])), float(np.median(saturation[strong])), int(strong.sum())


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("texture")
    parser.add_argument("--family", nargs="*", default=WHEELOCK_RED_FAMILY)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    # Never let the subject vote on the target it is being measured against -- it is usually a
    # member of its own family list, and self-inclusion drags the median toward whatever is wrong
    # with it.
    family = [n for n in args.family if n != args.texture]
    samples = [m for m in (measure(n) for n in family) if m]
    if not samples:
        print("no family member had enough red to measure", file=sys.stderr)
        return 1
    target_hue = float(np.median([s[0] for s in samples]))
    target_saturation = float(np.median([s[1] for s in samples]))
    print("family target   hue %+.2f deg  saturation %.3f   (%d textures)"
          % (target_hue, target_saturation, len(samples)))

    current = measure(args.texture)
    if current is None:
        print("%s has too little red to measure" % args.texture, file=sys.stderr)
        return 1
    print("%-32s hue %+.2f deg  saturation %.3f   (%d px)"
          % (args.texture, current[0], current[1], current[2]))

    hue_shift = target_hue - current[0]
    saturation_gain = target_saturation / current[1] if current[1] > 1e-6 else 1.0
    print("shift           hue %+.2f deg  saturation x%.3f" % (hue_shift, saturation_gain))
    if abs(hue_shift) < 0.25 and abs(saturation_gain - 1.0) < 0.01:
        print("already on target -- nothing to do")
        return 0
    if args.dry_run:
        return 0

    data = _load(args.texture)
    rgb = data[..., :3] / 255.0
    weight = red_weight(rgb, data[..., 3])
    hue, saturation, value = _hsv(rgb)
    # Value is deliberately left alone: it carries the moulding's shading.
    new_hue = hue + weight * hue_shift
    new_saturation = np.clip(saturation * (1.0 + weight * (saturation_gain - 1.0)), 0.0, 1.0)
    corrected = np.clip(_rgb(new_hue, new_saturation, value), 0.0, 1.0)

    out = data.copy()
    out[..., :3] = corrected * 255.0
    path = _tex_path(args.texture)
    Image.fromarray(np.round(out).astype(np.uint8), "RGBA").save(path)
    after = measure(args.texture)
    print("wrote %s -> hue %+.2f deg  saturation %.3f" % (path, after[0], after[1]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
