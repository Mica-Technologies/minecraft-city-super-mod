#!/usr/bin/env python3
"""
Repaint an appliance's housing to a different colour, leaving its lens and legend alone.

Used to make colour variants that no photograph exists for -- the black special-edition L-Series
LED speaker strobe is the white one with its housing darkened, keeping the red FIRE legend and the
chrome lens exactly as they are.

Only the near-neutral moulding is touched, and only its VALUE: the housing's shading, its mould
lines and the shadow under its lens all survive, because the structure is remapped rather than
replaced. Saturated pixels (a red legend) keep their colour, and a rectangle can be fenced off
entirely for a lens, whose chrome is near-neutral too and would otherwise be repainted with the
body around it.

Usage:
    python recolor_housing.py <source> <output> --range LOW HIGH [--keep u0 v0 u1 v1]
                              [--legend-boost 1.0]

    <source>/<output> are file stems under textures/blocks/lifesafety/, no extension.
    --range is the value band the housing is mapped into, 0-255. White housing sits around 220;
    a matte black wants something like 22 58.

The black L-Series LED family is made with these, kept here so a new member of it matches the ones
already shipped rather than being re-guessed -- the wall unit's arguments went unrecorded the first
time and had to be recovered from the texture, which is the mistake this block exists to prevent.
--keep is the lens circle measure_lens() reports for the RED texture of the pair, grown a little so
the halo of body immediately around the lens stays bright the way it does on a real one:

    python recolor_housing.py system_sensor_l_series_led_white_speaker_strobe
         system_sensor_l_series_led_black_speaker_strobe
         --range 8 34 --keep 5.703 3.965 10.148 8.285 --legend-boost 1.35

    python recolor_housing.py system_sensor_l_series_led_white_ceiling_speaker_strobe
         system_sensor_l_series_led_black_ceiling_speaker_strobe
         --range 8 34 --keep 5.768 5.870 10.118 10.095 --legend-boost 1.35

The ceiling unit carries no FIRE legend, so --legend-boost only reaches its LED die there -- which
is the same thing it does to the wall unit's die, and is why both keep it.
"""

import argparse
import os
import sys

import numpy as np
from PIL import Image

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TEX_DIR = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "csm",
                       "textures", "blocks", "lifesafety")


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("source")
    parser.add_argument("output")
    parser.add_argument("--range", nargs=2, type=float, required=True, metavar=("LOW", "HIGH"))
    parser.add_argument("--keep", nargs=4, type=float, default=None,
                        metavar=("U0", "V0", "U1", "V1"),
                        help="UV rect left untouched, for a lens")
    parser.add_argument("--legend-boost", type=float, default=1.0,
                        help="brighten saturated pixels, for a legend that must read on a dark body")
    args = parser.parse_args()

    data = np.asarray(Image.open(os.path.join(TEX_DIR, *args.source.split("/")) + ".png")
                      .convert("RGBA"), dtype=np.float64)
    colour, alpha = data[..., :3], data[..., 3]
    height, width, _ = colour.shape
    scale = width / 16.0

    high = colour.max(2)
    low = colour.min(2)
    saturation = np.where(high > 1e-6, (high - low) / np.maximum(high, 1.0), 0.0)

    # Housing: near-neutral, opaque, and not inside the fenced-off lens.
    weight = np.clip((0.26 - saturation) / 0.14, 0.0, 1.0) * (alpha > 200)
    if args.keep:
        # The fence is the ELLIPSE inscribed in the rect, not the rect. A round lens fenced off
        # squarely leaves its four corners the old housing colour -- a white square framing the
        # lens on a body that has gone black. Feathered by a pixel so the join is not a hard step.
        u0, v0, u1, v1 = args.keep
        centre_u, centre_v = (u0 + u1) / 2.0 * scale, (v0 + v1) / 2.0 * scale
        radius_u, radius_v = max(1e-6, (u1 - u0) / 2.0 * scale), max(1e-6, (v1 - v0) / 2.0 * scale)
        yy, xx = np.mgrid[0:height, 0:width]
        distance = np.sqrt(((xx - centre_u) / radius_u) ** 2 + ((yy - centre_v) / radius_v) ** 2)
        weight *= np.clip((distance - 1.0) / 0.06, 0.0, 1.0)

    lit = high[weight > 0.5]
    if lit.size < 100:
        print("almost nothing matched as housing -- check --keep", file=sys.stderr)
        return 1
    source_low, source_high = float(lit.min()), float(lit.max())
    target_low, target_high = args.range
    span = max(1e-6, source_high - source_low)
    scaled = target_low + (high - source_low) / span * (target_high - target_low)
    factor = np.where(high > 1e-6, np.clip(scaled, 0.0, 255.0) / np.maximum(high, 1e-6), 1.0)
    factor = 1.0 + weight * (factor - 1.0)
    out = colour * factor[..., None]

    if args.legend_boost != 1.0:
        legend = np.clip((saturation - 0.30) / 0.15, 0.0, 1.0) * (alpha > 200)
        out = out * (1.0 + legend * (args.legend_boost - 1.0))[..., None]

    result = np.dstack([np.clip(out, 0, 255), alpha]).astype(np.uint8)
    path = os.path.join(TEX_DIR, *args.output.split("/")) + ".png"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(result, "RGBA").save(path)
    print("wrote %s  (housing %d..%d -> %d..%d, %d px repainted)"
          % (path, source_low, source_high, target_low, target_high, int((weight > 0.5).sum())))
    return 0


if __name__ == "__main__":
    sys.exit(main())
