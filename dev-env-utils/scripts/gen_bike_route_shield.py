#!/usr/bin/env python3
"""The bicycle route marker (MUTCD M1-8) for the shared sign atlas.

Every other marker in `sign_atlas.png` came from a public-domain SVG on Commons, but there is
no blank M1-8 there worth using and the book draws the sign itself, numerals and all. So this
takes the book's own M1-8 -- the white 12 x 18 plate, the green oval and the bicycle symbol --
paints out its sample "13" with the oval's green, since the renderer sets the number, and
squares the result on transparency for the atlas cell.

What it writes:

    dev-env-utils/src/main/resources/guidesign/shields/bikeroute.png   the source artwork
    .../textures/blocks/trafficaccessories/guidesign/sign_atlas.png    cell (5, 10) only

The atlas is `GuideSignAtlasTool`'s output and stays so -- the tool lists this PNG and
reproduces the cell on a full regeneration. This script stamps the one cell into the committed
atlas so the two cannot be out of step without `--check` saying so, and touches no other pixel.

It also prints the four placement numbers `GuideSignShieldType.BIKE_ROUTE` carries, measured
off the book's own numerals rather than guessed: the marker is not in
`measure_shield_legends.py`'s MARKERS (that script is for the state, DC and province markers),
so this is where they come from.

Usage:
    python gen_bike_route_shield.py            # write both files
    python gen_bike_route_shield.py --check    # write nothing; exit 1 on drift
"""

import argparse
import os
import sys
from collections import deque

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import shs_signs as shs  # noqa: E402

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SHIELD_PNG = os.path.join(REPO, "dev-env-utils", "src", "main", "resources", "guidesign",
                          "shields", "bikeroute.png")
ATLAS = os.path.join(REPO, "modules", "roads", "src", "main", "resources", "assets", "csm",
                     "textures", "blocks", "trafficaccessories", "guidesign", "sign_atlas.png")

# Where the marker lives in the atlas, and how GuideSignAtlasTool lays a cell out.
CELL_COL, CELL_ROW = 5, 10
CELL_SIZE = 64
SHIELD_PADDING = 2

#: Side of the squared source PNG. The tool stretches a PNG to fill the padded cell, so the
#: source is already square and the marker sits centred on transparency inside it.
SIDE = 256

#: The book page the M1-8 is drawn on, and which sign on it.
BOOK_CHAPTER, BOOK_PAGE, BOOK_PICK = "Guide", 8, 0


def _face():
    """The book's M1-8, palette-mapped onto the mod's colours as every other face is."""
    return shs.recolour(shs.book_sign(BOOK_CHAPTER, BOOK_PAGE, BOOK_PICK),
                        dict(shs.SHS_PALETTE))


def _inside_oval(px, is_green):
    """The marker's white artwork that is INSIDE the green oval: the bicycle and the numerals.

    Found by flooding the non-green pixels in from the border -- that reaches the white plate
    and everything outside it, and stops at the oval -- and taking what it never reached. A
    plain "white pixel" test cannot do this: the plate is white too.
    """
    h, w = is_green.shape
    not_green = ~is_green
    seen = np.zeros((h, w), bool)
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            if not_green[y, x] and not seen[y, x]:
                seen[y, x] = True
                q.append((y, x))
    for y in range(h):
        for x in (0, w - 1):
            if not_green[y, x] and not seen[y, x]:
                seen[y, x] = True
                q.append((y, x))
    while q:
        y, x = q.popleft()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and not seen[ny, nx] and not_green[ny, nx]:
                seen[ny, nx] = True
                q.append((ny, nx))
    return not_green & ~seen


def build():
    """The squared source PNG, and the placement the book's numerals imply."""
    face = _face()
    w, h = face.size
    px = np.array(face)
    green = np.array(shs.MOD_COLOURS["green"][:3])
    is_green = ((np.abs(px[:, :, :3].astype(int) - green).sum(axis=2) < 90)
                & (px[:, :, 3] > 200))
    inside = _inside_oval(px, is_green)

    ys, xs = np.where(is_green)
    oy0, oy1 = ys.min(), ys.max()

    # The bicycle is above the middle and the numerals below it, with a clear row between.
    per_row = inside.sum(axis=1)
    mid = (oy0 + oy1) // 2
    gap = next(y for y in range(mid - (oy1 - oy0) // 8, oy1) if per_row[y] == 0)
    numerals = np.zeros_like(inside)
    numerals[gap:, :] = inside[gap:, :]
    ys, xs = np.where(numerals)
    nx0, nx1, ny0, ny1 = xs.min(), xs.max(), ys.min(), ys.max()

    placement = {
        "cap": (ny1 - ny0 + 1) / float(h),
        "cy": ((ny0 + ny1) / 2.0) / float(h),
        "width": (nx1 - nx0 + 1) / float(w),
        "cx": ((nx0 + nx1) / 2.0) / float(w),
    }

    # Paint the sample number out with the oval's green.
    pad = int(0.010 * h)
    band = np.zeros_like(inside)
    band[max(oy0, ny0 - pad):min(oy1, ny1 + pad) + 1, :] = True
    paint = band & (inside | is_green)
    px[:, :, :3] = np.where(paint[:, :, None], green.astype(px.dtype), px[:, :, :3])
    clean = Image.fromarray(px, "RGBA")

    marker_w = int(round(w * SIDE / float(h)))
    square = Image.new("RGBA", (SIDE, SIDE), (0, 0, 0, 0))
    square.alpha_composite(clean.resize((marker_w, SIDE), Image.LANCZOS),
                           ((SIDE - marker_w) // 2, 0))
    placement["k"] = marker_w / float(SIDE)
    return square, placement


def enum_values(p):
    """The four numbers GuideSignShieldType.BIKE_ROUTE carries, as fractions of the CELL."""
    padf = SHIELD_PADDING / float(CELL_SIZE)
    span = (CELL_SIZE - 2 * SHIELD_PADDING) / float(CELL_SIZE)
    return {
        "routeTextMaxFraction": round(p["width"] * p["k"] * span, 2),
        "routeTextCapFraction": round(p["cap"] * span, 2),
        "routeTextCenterX": round(padf + (0.5 + (p["cx"] - 0.5) * p["k"]) * span, 3),
        "routeTextCenterY": round(padf + p["cy"] * span, 3),
    }


def stamped_atlas(square):
    """The committed atlas with cell (5, 10) replaced, and nothing else touched."""
    atlas = Image.open(ATLAS).convert("RGBA")
    cell = square.resize((CELL_SIZE - 2 * SHIELD_PADDING,) * 2, Image.LANCZOS)
    box = (CELL_COL * CELL_SIZE, CELL_ROW * CELL_SIZE,
           (CELL_COL + 1) * CELL_SIZE, (CELL_ROW + 1) * CELL_SIZE)
    blank = Image.new("RGBA", (CELL_SIZE, CELL_SIZE), (0, 0, 0, 0))
    blank.paste(cell, (SHIELD_PADDING, SHIELD_PADDING))
    atlas.paste(blank, box)
    return atlas


def _same(path, img):
    if not os.path.exists(path):
        return False
    cur = Image.open(path).convert("RGBA")
    return cur.size == img.size and cur.tobytes() == img.tobytes()


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    args = ap.parse_args()

    square, placement = build()
    atlas = stamped_atlas(square)
    values = enum_values(placement)

    if args.check:
        drift = [p for p, img in ((SHIELD_PNG, square), (ATLAS, atlas)) if not _same(p, img)]
        if drift:
            print("DRIFT: %d file(s) differ from the generator:" % len(drift))
            for p in drift:
                print("  " + os.path.relpath(p, REPO))
            return 1
        print("the bike route marker matches the generator")
        print("placement: " + ", ".join("%s=%s" % kv for kv in values.items()))
        return 0

    square.save(SHIELD_PNG)
    atlas.save(ATLAS)
    print("Wrote %s and stamped atlas cell (%d, %d)"
          % (os.path.relpath(SHIELD_PNG, REPO), CELL_COL, CELL_ROW))
    print("GuideSignShieldType.BIKE_ROUTE placement:")
    for k, v in values.items():
        print("  %-22s %s" % (k, v))
    return 0


if __name__ == "__main__":
    sys.exit(main())
