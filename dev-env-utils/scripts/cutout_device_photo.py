#!/usr/bin/env python3
"""
Turn a photograph of an appliance on a plain background into a block texture.

The fire alarm textures in this mod are photographs, and new ones keep arriving as product shots on
white. This does the mechanical part: lift the background to alpha, crop to the device, and fit it
into a square power-of-two texture at its TRUE aspect, centred.

Aspect is the part worth being careful about. Squeezing a tall appliance into a square texture and
mapping the whole square onto a square block face is how the Simplex TrueAlert ended up rendering
at 1.12 wide-to-tall when the real unit is 0.66 -- visibly squat. Fitting the art to its own aspect
inside the square and letting the model trace the silhouette keeps the proportions honest, since
the model's front face is then sized from the opaque region rather than from the whole texture.

The background is lifted by flood-filling inward from the border, NOT by thresholding the whole
image: these devices have white lettering, chrome reflectors and pale logos on them, and a global
threshold punches holes straight through all of it.

Stock photographs often carry a seller's watermark somewhere in the margin. It is NOT background
as far as the flood fill is concerned -- it is dark, so the fill cannot reach it, and it survives
as a floating blob that also stretches the crop box and squashes the device. `--erase` paints a
rectangle of the source white first, which is what to do with one.

Usage:
    python cutout_device_photo.py <source-image> <output-name> [--size 256] [--threshold 228]
                                  [--margin 0.02] [--erase x0,y0,x1,y1]

    <output-name> is the file stem written under textures/blocks/lifesafety/, no extension;
    pass a path with slashes to place it in a subfolder such as shared_textures/.
"""

import argparse
import os
import sys
from collections import deque

import numpy as np
from PIL import Image, ImageDraw

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import csm_layout as layout  # noqa: E402

REPO_ROOT = layout.REPO_ROOT
LIFESAFETY_OWNER = layout.owner_of_folder("lifesafety")


def lift_background(rgb, threshold):
    """Alpha mask of the device: everything the border flood fill could NOT reach.

    Returns a float mask in 0..1 so the device's own edge stays soft instead of aliasing.
    """
    height, width, _ = rgb.shape
    pale = rgb.min(2) > threshold
    reached = np.zeros((height, width), dtype=bool)
    queue = deque()
    for x in range(width):
        for y in (0, height - 1):
            if pale[y, x] and not reached[y, x]:
                reached[y, x] = True
                queue.append((y, x))
    for y in range(height):
        for x in (0, width - 1):
            if pale[y, x] and not reached[y, x]:
                reached[y, x] = True
                queue.append((y, x))
    while queue:
        y, x = queue.popleft()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < height and 0 <= nx < width and pale[ny, nx] and not reached[ny, nx]:
                reached[ny, nx] = True
                queue.append((ny, nx))

    alpha = (~reached).astype(np.float64)
    # Feather one pixel so the cut edge is not a hard staircase; the model's silhouette trace
    # reads this edge, and a soft one traces more smoothly.
    padded = np.pad(alpha, 1, mode="edge")
    neighbours = sum(padded[1 + dy:1 + dy + alpha.shape[0], 1 + dx:1 + dx + alpha.shape[1]]
                     for dy in (-1, 0, 1) for dx in (-1, 0, 1)) / 9.0
    return np.clip(alpha * 0.72 + neighbours * 0.28, 0.0, 1.0)


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("source")
    parser.add_argument("output")
    parser.add_argument("--size", type=int, default=256)
    parser.add_argument("--threshold", type=int, default=228)
    parser.add_argument("--margin", type=float, default=0.02,
                        help="fraction of the canvas left clear around the device")
    parser.add_argument("--erase", action="append", default=None,
                        metavar="X0,Y0,X1,Y1",
                        help="paint this rectangle of the SOURCE white before lifting the "
                             "background, for a watermark or a label outside the device; repeat "
                             "for more than one")
    args = parser.parse_args()

    photo = Image.open(args.source)
    if args.erase:
        photo = photo.convert("RGBA")
        painter = ImageDraw.Draw(photo)
        for rect in args.erase:
            painter.rectangle([int(v) for v in rect.split(",")], fill=(255, 255, 255, 255))
    rgba = np.asarray(photo.convert("RGBA"), dtype=np.float64)
    rgb = rgba[..., :3]
    if rgba[..., 3].min() < 250:
        # Already cut out. Some product shots arrive as PNGs whose background is transparent
        # rather than white, and flattening one to RGB first hands the flood fill whatever colour
        # happens to sit under the transparency -- usually a dark fringe, which it cannot lift.
        alpha = rgba[..., 3] / 255.0
    else:
        alpha = lift_background(rgb, args.threshold)
    rows, cols = np.nonzero(alpha > 0.5)
    if len(rows) == 0:
        print("nothing survived the background lift -- try a lower --threshold", file=sys.stderr)
        return 1
    top, bottom, left, right = rows.min(), rows.max() + 1, cols.min(), cols.max() + 1

    cut = np.dstack([rgb, alpha * 255.0]).astype(np.uint8)
    device = Image.fromarray(cut, "RGBA").crop((left, top, right, bottom))
    aspect = device.size[0] / device.size[1]
    print("device %dx%d, aspect %.3f" % (device.size[0], device.size[1], aspect))

    usable = int(args.size * (1.0 - 2.0 * args.margin))
    if aspect >= 1.0:
        target = (usable, max(1, int(round(usable / aspect))))
    else:
        target = (max(1, int(round(usable * aspect))), usable)
    device = device.resize(target, Image.LANCZOS)

    canvas = Image.new("RGBA", (args.size, args.size), (0, 0, 0, 0))
    canvas.paste(device, ((args.size - target[0]) // 2, (args.size - target[1]) // 2))

    rel = "textures/blocks/lifesafety/" + args.output + ".png"
    path = layout.asset_for_write(LIFESAFETY_OWNER, rel)[:-len(".png")]
    os.makedirs(os.path.dirname(path), exist_ok=True)
    canvas.save(path + ".png")
    opaque = np.asarray(canvas)[..., 3] > 128
    print("wrote %s.png  (%dx%d, device fills %.1f%% at aspect %.3f)"
          % (path, args.size, args.size, opaque.mean() * 100, aspect))
    return 0


if __name__ == "__main__":
    sys.exit(main())
