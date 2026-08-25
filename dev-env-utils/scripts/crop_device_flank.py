#!/usr/bin/env python3
"""
Turn a side-profile photograph of an appliance into a flank texture.

A device photographed straight on has no art at all for the sides of its enclosure, and on plenty
of fire alarm appliances the sides are where the FIRE legend lives. gen_e50_flank_texture.py draws
the Wheelock E50's from scratch because no side photograph of one was to hand. When there IS one --
as for the Edwards EST 202-8A, whose grey body carries FIRE in rotated red capitals down each
flank -- cropping the real band beats redrawing it: the typography, the moulding's shading and the
paint's texture all come along for free.

Output is a 256-unit-square atlas in the same layout gen_e50_flank_texture.py uses, so the models
consume either kind identically:

    u[0, --units]   v[0, 16]   the enclosure flank, the crop resized to fill it
    u[3.5, 7.5]     v[0, 4]    plain moulding, for the top and bottom caps and the hidden back

Pick --units so the strip's aspect matches the flank's own depth-to-height ratio on the model; the
strip is stretched across whatever u range the model's `side_strips` names, so a mismatch there is
what stretches the lettering.

The crop box wants to hold ONLY the flank: no clear lens at the front, no mounting plate at the
back, and no background above or below. Both ends of the band are sampled for the plain patch, so
a sliver of background left in the box tints every cap on the model.

Be strict about the front end in particular. A clear lens meets its housing at a bright highlight
over a dark line, and a couple of columns of that left in the box become a hard band down the
strip -- which a model's `side_strips` reversal then lands at the BACK of one flank, where it
reads as the lens bleeding onto the housing behind it. Walk the columns first: the moulding is the
run where each column's min and max sit close together, and the lens is the run where they are 0
and 255.

Usage:
    python crop_device_flank.py <side-photo> <output-name> --box x0,y0,x1,y1 [--units 2.9]
                                [--size 256] [--flip]

Recorded invocations:
    # Edwards EST 202-8A grey body, from the 202-8A-T side profile. The -T and the -TW share this
    # moulding exactly -- only the mounting plate behind it changes colour -- so one texture serves
    # both blocks.
    python crop_device_flank.py 202-8a-t_1_edited.png edwards_est_202_8a_flank \
        --box 214,150,374,995 --units 2.9
"""

import argparse
import os
import sys

import numpy as np
from PIL import Image

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TEX_DIR = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "csm",
                       "textures", "blocks", "lifesafety")

#: Where the plain patch goes, in the 16-unit UV space. Matches gen_e50_flank_texture.py.
PATCH_RECT = (3.5, 0.0, 7.5, 4.0)


def plain_colour(crop):
    """The moulding's own colour, taken from the clear bands above and below the lettering.

    A median rather than a mean: a stray highlight or a letter's antialiased edge caught in the
    sample moves a mean and does not move a median.
    """
    pixels = np.asarray(crop.convert("RGB"), dtype=np.int16)
    height = pixels.shape[0]
    band = max(1, height // 12)
    ends = np.concatenate([pixels[:band].reshape(-1, 3), pixels[-band:].reshape(-1, 3)])
    return tuple(int(c) for c in np.median(ends, axis=0))


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("source")
    parser.add_argument("output")
    parser.add_argument("--box", required=True,
                        help="crop of the flank band in the side photo: x0,y0,x1,y1")
    parser.add_argument("--units", type=float, default=2.9,
                        help="width of the flank strip in the 16-unit UV space")
    parser.add_argument("--size", type=int, default=256)
    parser.add_argument("--flip", action="store_true",
                        help="mirror the crop, for a photograph taken of the other flank")
    args = parser.parse_args()

    try:
        left, top, right, bottom = (int(v) for v in args.box.split(","))
    except ValueError:
        print("--box wants four integers: x0,y0,x1,y1", file=sys.stderr)
        return 1

    crop = Image.open(args.source).convert("RGB").crop((left, top, right, bottom))
    if args.flip:
        crop = crop.transpose(Image.FLIP_LEFT_RIGHT)
    print("flank band %dx%d, aspect %.3f (strip aspect %.3f at --units %.2f)"
          % (crop.size[0], crop.size[1], crop.size[0] / crop.size[1],
             args.units / 16.0, args.units))

    unit = args.size / 16.0
    housing = plain_colour(crop)
    atlas = Image.new("RGBA", (args.size, args.size), housing + (255,))

    strip_width = max(1, int(round(args.units * unit)))
    atlas.paste(crop.resize((strip_width, args.size), Image.LANCZOS).convert("RGBA"), (0, 0))

    rng = np.random.RandomState(11)      # fixed, so regenerating gives the same file
    pu0, pv0, pu1, pv1 = PATCH_RECT
    patch_size = (int(round((pu1 - pu0) * unit)), int(round((pv1 - pv0) * unit)))
    plain = np.full((patch_size[1], patch_size[0], 3), housing, dtype=np.int16)
    plain = np.clip(plain + rng.randint(-3, 4, plain.shape), 0, 255).astype(np.uint8)
    atlas.paste(Image.fromarray(plain, "RGB").convert("RGBA"),
                (int(round(pu0 * unit)), int(round(pv0 * unit))))

    path = os.path.join(TEX_DIR, *args.output.split("/"))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    atlas.save(path + ".png")
    print("wrote %s.png (%dx%d, strip %d px wide, moulding rgb%s)"
          % (path, args.size, args.size, strip_width, housing))
    return 0


if __name__ == "__main__":
    sys.exit(main())
