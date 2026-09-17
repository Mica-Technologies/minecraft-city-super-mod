#!/usr/bin/env python3
"""Textures for the structural framing family.

Draws the galvanised steel stud and its track. The stud's punched knockout is a real hole in the
alpha channel rather than a dark ellipse painted on, because the block renders on the CUTOUT layer
and the point of the content is seeing through the wall. A painted-on hole reads as a sticker the
moment there is anything behind it.

Run with no arguments to write the textures. ``--check`` regenerates into a temporary directory
and fails if the tree has drifted, which is the guard to run before committing anything under
``textures/blocks/buildingmaterials/framing/``.

    python dev-env-utils/scripts/gen_framing_textures.py
    python dev-env-utils/scripts/gen_framing_textures.py --check
"""

import argparse
import filecmp
import os
import random
import shutil
import sys
import tempfile

from PIL import Image, ImageDraw

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT_DIR = os.path.join(REPO, "modules", "building", "src", "main", "resources", "assets", "csm",
                       "textures", "blocks", "buildingmaterials", "framing")

SIZE = 16

# Galvanised steel: a cool grey with the faint uneven mottle of a spangled zinc coat. Kept narrow
# in range -- real galvanising is subtle, and a wide range reads as damage rather than finish.
STEEL_BASE = (163, 168, 172, 255)
STEEL_LIGHT = (188, 193, 197, 255)
STEEL_DARK = (137, 142, 147, 255)
STEEL_EDGE = (118, 123, 128, 255)

# The knockout: the punched service hole every steel stud web carries, for conduit and pipe.
#
# Sized and placed to land exactly on the four texture pixels the stud model samples. The web is
# 4/16 of a block wide, and the model gives it the default position-derived UV, so it shows
# texture columns 6-9 and nothing else. A knockout drawn wider than that is cropped by the model
# and reads as a slot running off the edge of the stud.
KNOCKOUT_X0, KNOCKOUT_X1 = 7, 8
KNOCKOUT_Y0, KNOCKOUT_Y1 = 5, 10
KNOCKOUT_RIM = (206, 210, 214, 255)


def _mottle(seed):
    """A deterministic spangle. Seeded so two runs produce byte-identical files."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (SIZE, SIZE), STEEL_BASE)
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            roll = rng.random()
            if roll < 0.12:
                px[x, y] = STEEL_LIGHT
            elif roll < 0.24:
                px[x, y] = STEEL_DARK
    return img


def steel_stud():
    """The stud web, seen face on: mottled steel, a punched knockout, a rolled edge each side."""
    img = _mottle(seed=20260917)
    draw = ImageDraw.Draw(img)

    # The rolled edges of the C, which catch the light along the stud's length.
    draw.line([(0, 0), (0, SIZE - 1)], fill=STEEL_EDGE)
    draw.line([(SIZE - 1, 0), (SIZE - 1, SIZE - 1)], fill=STEEL_EDGE)
    draw.line([(1, 0), (1, SIZE - 1)], fill=STEEL_LIGHT)
    draw.line([(SIZE - 2, 0), (SIZE - 2, SIZE - 1)], fill=STEEL_LIGHT)

    # The knockout: a slot punched clean through, with the turned lip the punch leaves round it.
    # The lip is drawn first and the hole cut out of it, so the lip survives as a one-pixel rim on
    # all four sides -- which is what keeps a 2px hole reading as a hole and not as dirt.
    draw.rectangle([KNOCKOUT_X0 - 1, KNOCKOUT_Y0 - 1, KNOCKOUT_X1 + 1, KNOCKOUT_Y1 + 1],
                   fill=KNOCKOUT_RIM)
    draw.rectangle([KNOCKOUT_X0, KNOCKOUT_Y0, KNOCKOUT_X1, KNOCKOUT_Y1], fill=(0, 0, 0, 0))
    return img


def steel_track():
    """The track: the same steel, no knockout. Track is punched only where a service crosses it."""
    img = _mottle(seed=20260918)
    draw = ImageDraw.Draw(img)
    # Track is a plain U, so it shows its two legs rather than a web's rolled edges.
    draw.line([(0, 0), (SIZE - 1, 0)], fill=STEEL_LIGHT)
    draw.line([(0, SIZE - 1), (SIZE - 1, SIZE - 1)], fill=STEEL_EDGE)
    return img


TEXTURES = {
    "steel_stud.png": steel_stud,
    "steel_track.png": steel_track,
}


def write_all(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    for name, fn in sorted(TEXTURES.items()):
        fn().save(os.path.join(out_dir, name))
    return sorted(TEXTURES)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    args = ap.parse_args()

    if not args.check:
        names = write_all(OUT_DIR)
        print("Wrote %d textures to %s" % (len(names), os.path.relpath(OUT_DIR, REPO)))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_framing_")
    try:
        names = write_all(tmp)
        drifted = []
        for name in names:
            here = os.path.join(OUT_DIR, name)
            if not os.path.exists(here) or not filecmp.cmp(here, os.path.join(tmp, name),
                                                           shallow=False):
                drifted.append(name)
        if drifted:
            print("DRIFT: %d texture(s) differ from the generator:" % len(drifted))
            for name in drifted:
                print("  " + name)
            return 1
        print("%d framing textures are up to date" % len(names))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
