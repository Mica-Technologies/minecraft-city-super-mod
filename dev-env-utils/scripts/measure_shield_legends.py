#!/usr/bin/env python3
"""Measure where each state, DC and province route marker leaves room for its number.

The dynamic guide and street signs draw a route marker from the shared sign atlas and then
set the route number over it with the guide sign font. A generic marker (Interstate, the
circle) has its number in the middle, but a real state marker does not: Texas sets it above
the word TEXAS, Colorado below the flag, Idaho in the corner the state's outline leaves free.
So every marker in MARKERS carries four numbers in GuideSignShieldType, all as fractions of the
shield's size:

  routeTextMaxFraction   widest the number may be before it shrinks
  routeTextCapFraction   how tall its digits are
  routeTextCenterX/Y     where it is centred, measured from the cell's top-left

This script reads them off the atlas rather than by eye. For each marker it takes the region
of the face the number is printed on -- the connected run of the colour under the marker's
seed point, bridged over any thin outline stroke the real sign prints the number across --
and finds the largest two-digit number that fits inside it with a margin, searching around
the seed for the centre that allows the largest one (never taller than the generic shields'
0.42). Its height is the cap fraction and its width the width fraction. A real marker sets one-
and two-digit routes at the same height, so a single digit is not blown up to fill the space
a pair would take (on DC's marker it would run into the letters above it), and a three-digit
route shrinks to the same width, so it stays inside the outline too.

Run it after regenerating sign_atlas.png with GuideSignAtlasTool:

  python dev-env-utils/scripts/measure_shield_legends.py          # rewrite the enum
  python dev-env-utils/scripts/measure_shield_legends.py --check  # exit 1 on drift

Only the constants listed in MARKERS are touched; the generic shields keep their values.
"""
import argparse
import json
import re
import sys
from collections import deque
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image

REPO = Path(__file__).resolve().parents[2]
ROADS = REPO / "modules" / "roads" / "src" / "main"
ATLAS = (ROADS / "resources" / "assets" / "csm" / "textures" / "blocks" / "trafficaccessories"
         / "guidesign" / "sign_atlas.png")
FONT = ROADS / "resources" / "assets" / "csm" / "fonts" / "guide_sign_font.json"
ENUM = (ROADS / "java" / "com" / "micatechnologies" / "minecraft" / "csm" / "trafficaccessories"
        / "guidesign" / "GuideSignShieldType.java")

CELL = 64
# The generic shields' route number cap height (the renderers' former global constant). A
# state marker's number is never set taller than this, only shorter where the face is tight.
CAP_MAX = 0.42
# Clear space kept between the number's box and the edge of the region it is printed on.
MARGIN = 2.5
# Fraction of the number's width (and height) cut off each corner of its footprint.
CHAMFER = 0.12
# How far (px) the search may move the number's centre from the seed, and what each pixel of
# drift costs against the cap height it buys, so a near-symmetric face keeps its number
# centred rather than chasing a sub-pixel gain.
SEARCH = 6.0
STEP = 0.5
DRIFT_COST = 0.15
# Colour distance (max channel difference) still counted as the face under the seed.
COLOUR_TOLERANCE = 56

BLACK = 0x101010
WHITE = 0xFFFFFF


@dataclass(frozen=True)
class Marker:
    """Where a marker's number goes (cell pixels, 0-64) and what colour it is printed in."""
    seed: tuple
    color: int = BLACK
    # Radius (px) of the closing that bridges a thin outline stroke the real sign prints its
    # number straight across (Florida's and Oklahoma's state outlines, DC's diamond).
    bridge: int = 0
    # How far (px) the centre may move from the seed. Bridging also swallows DC's thin "DC"
    # letters, so DC's number is held where the real sign sets it instead of drifting up
    # over them.
    search: float = SEARCH


MARKERS = {
    "CALIFORNIA": Marker((32, 36), WHITE),
    "TEXAS": Marker((32, 25)),
    "FLORIDA": Marker((32, 34), bridge=2),
    "NEW_YORK": Marker((32, 32)),
    "CONNECTICUT": Marker((32, 32)),
    "MASSACHUSETTS": Marker((32, 32)),
    "MAINE": Marker((32, 32)),
    "NEW_HAMPSHIRE": Marker((36, 32)),
    "RHODE_ISLAND": Marker((32, 39)),
    "VERMONT": Marker((32, 40), 0x006B54),
    "ALABAMA": Marker((32, 27)),
    "ALASKA": Marker((43, 33)),
    "ARIZONA": Marker((32, 36)),
    "ARKANSAS": Marker((31, 32)),
    "COLORADO": Marker((32, 45)),
    "DELAWARE": Marker((32, 32)),
    "GEORGIA": Marker((30, 34)),
    "HAWAII": Marker((32, 42)),
    "IDAHO": Marker((43, 19)),
    "ILLINOIS": Marker((32, 39)),
    "INDIANA": Marker((32, 39)),
    "IOWA": Marker((32, 32)),
    "KANSAS": Marker((32, 32)),
    "KENTUCKY": Marker((32, 32)),
    "LOUISIANA": Marker((33, 42)),
    "MARYLAND": Marker((32, 42)),
    "MICHIGAN": Marker((32, 36)),
    "MINNESOTA": Marker((32, 39), WHITE),
    "MISSISSIPPI": Marker((32, 32)),
    "MISSOURI": Marker((31, 29)),
    "MONTANA": Marker((32, 38)),
    "NEBRASKA": Marker((32, 27)),
    "NEVADA": Marker((33, 22)),
    "NEW_JERSEY": Marker((32, 32)),
    "NEW_MEXICO": Marker((32, 32)),
    "NORTH_CAROLINA": Marker((32, 32)),
    "NORTH_DAKOTA": Marker((31, 39)),
    "OHIO": Marker((31, 32)),
    "OKLAHOMA": Marker((32, 39), bridge=2),
    "OREGON": Marker((32, 29)),
    "PENNSYLVANIA": Marker((32, 33)),
    "SOUTH_CAROLINA": Marker((32, 40), 0x003478),
    "SOUTH_DAKOTA": Marker((31, 31)),
    "TENNESSEE": Marker((32, 24)),
    "UTAH": Marker((32, 33)),
    "VIRGINIA": Marker((32, 28)),
    "WASHINGTON": Marker((33, 30)),
    "WEST_VIRGINIA": Marker((32, 32)),
    "WISCONSIN": Marker((32, 31)),
    "WYOMING": Marker((32, 35)),
    "DISTRICT_OF_COLUMBIA": Marker((32, 39), bridge=2, search=1.0),
    "ONTARIO": Marker((32, 38)),
    "QUEBEC": Marker((32, 40), WHITE),
    "NEW_BRUNSWICK": Marker((31, 33)),
    "NOVA_SCOTIA": Marker((32, 32)),
    "NEWFOUNDLAND": Marker((32, 40)),
    "PRINCE_EDWARD_ISLAND": Marker((32, 41)),
}

ENUM_LINE = re.compile(
    r"^(?P<indent>\s+)(?P<name>[A-Z_]+)\((?P<col>\d+), (?P<row>\d+), (?P<friendly>\"[^\"]*\"), "
    r"0x[0-9A-Fa-f]{6}, [0-9.]+f(?:, [0-9.]+f){0,3}\)(?P<end>[,;])(?P<rest>.*)$")


def digit_width_per_cap():
    """Mean advance of a digit, per unit of cap height, in the font the renderer sets numbers in."""
    font = json.loads(FONT.read_text(encoding="utf-8"))
    advances = [font["glyphs"][d]["advance"] for d in "0123456789"]
    return sum(advances) / len(advances) / font["capHeight"]


def face_region(cell, marker):
    """Boolean mask of the connected face region the number is printed on."""
    sx, sy = marker.seed
    rgb = cell[:, :, :3].astype(int)
    alpha = cell[:, :, 3]
    ref = np.median(rgb[sy - 1:sy + 2, sx - 1:sx + 2].reshape(-1, 3), axis=0)
    mask = (alpha >= 250) & (np.abs(rgb - ref).max(axis=2) <= COLOUR_TOLERANCE)
    if marker.bridge:
        mask = _erode(_dilate(mask, marker.bridge), marker.bridge)
    if not mask[sy, sx]:
        raise ValueError(f"seed {marker.seed} is not on a face region")
    region = np.zeros_like(mask)
    queue = deque([(sy, sx)])
    region[sy, sx] = True
    while queue:
        y, x = queue.popleft()
        for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
            if 0 <= ny < CELL and 0 <= nx < CELL and mask[ny, nx] and not region[ny, nx]:
                region[ny, nx] = True
                queue.append((ny, nx))
    return region


def _dilate(mask, r):
    out = np.zeros_like(mask)
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            out |= np.roll(np.roll(mask, dy, axis=0), dx, axis=1)
    return out


def _erode(mask, r):
    return ~_dilate(~mask, r)


class BoxTester:
    """Answers "does a number of this size, plus the margin, lie wholly inside the region?"

    A set of digits is not a rectangle: its outer figures are rounded (0, 3, 8) or open (7, 4)
    at the corners. Testing the full rectangle would shrink the number on every diamond,
    circle and tapering outline to far below what the real sign sets, so the number's
    footprint is taken as the rectangle with its corners chamfered -- the union of a full-height
    box inset at the sides and a full-width box inset at the top and bottom. Each is an O(1)
    summed-area lookup.
    """

    def __init__(self, region):
        self.sat = np.pad(region.astype(np.int32), ((1, 0), (1, 0))).cumsum(0).cumsum(1)

    def fits(self, cx, cy, w, h):
        return (self._rect(cx, cy, w * (1 - 2 * CHAMFER), h)
                and self._rect(cx, cy, w, h * (1 - 2 * CHAMFER)))

    def _rect(self, cx, cy, w, h):
        # A pixel counts as covered when its centre lies inside the margin-expanded box.
        x0 = int(np.ceil(cx - w / 2 - MARGIN - 0.5))
        x1 = int(np.floor(cx + w / 2 + MARGIN - 0.5))
        y0 = int(np.ceil(cy - h / 2 - MARGIN - 0.5))
        y1 = int(np.floor(cy + h / 2 + MARGIN - 0.5))
        if x0 < 0 or y0 < 0 or x1 >= CELL or y1 >= CELL:
            return False
        area = (x1 - x0 + 1) * (y1 - y0 + 1)
        s = self.sat
        inside = s[y1 + 1, x1 + 1] - s[y0, x1 + 1] - s[y1 + 1, x0] + s[y0, x0]
        return inside == area


def largest_cap(tester, cx, cy, width_per_cap, limit):
    """Largest cap height (px, to 0.1) whose box of width cap * width_per_cap fits at (cx, cy)."""
    lo, hi = 0.0, limit
    if tester.fits(cx, cy, hi * width_per_cap, hi):
        return hi
    while hi - lo > 0.1:
        mid = (lo + hi) / 2
        if tester.fits(cx, cy, mid * width_per_cap, mid):
            lo = mid
        else:
            hi = mid
    return lo


def measure(atlas, col, row, marker, digit_w):
    cell = atlas[row * CELL:(row + 1) * CELL, col * CELL:(col + 1) * CELL]
    tester = BoxTester(face_region(cell, marker))
    limit = CAP_MAX * CELL
    two_digit = 2 * digit_w
    sx, sy = marker.seed
    best = None
    steps = int(marker.search / STEP)
    for iy in range(-steps, steps + 1):
        for ix in range(-steps, steps + 1):
            cx, cy = sx + ix * STEP, sy + iy * STEP
            cap = largest_cap(tester, cx, cy, two_digit, limit)
            drift = (ix * ix + iy * iy) ** 0.5 * STEP
            score = cap - DRIFT_COST * drift
            if best is None or score > best[0] + 1e-9:
                best = (score, cap, cx, cy)
    _, cap2, cx, cy = best
    if cap2 < 4:
        raise ValueError(f"no room for a two-digit number near {marker.seed}")
    return {
        "max": _floor2(two_digit * cap2 / CELL),
        "cap": _floor2(cap2 / CELL),
        "cx": round(cx / CELL, 3),
        "cy": round(cy / CELL, 3),
    }


def _floor2(v):
    return int(v * 100) / 100


def _fmt(v):
    s = f"{v:.3f}".rstrip("0")
    return s + "0f" if s.endswith(".") else s + "f"


def rewrite(enum_text, results):
    newline = "\r\n" if "\r\n" in enum_text else "\n"
    out = []
    seen = set()
    for line in enum_text.split(newline):
        m = ENUM_LINE.match(line)
        if m and m.group("name") in results:
            r = results[m.group("name")]
            color = MARKERS[m.group("name")].color
            line = (f"{m.group('indent')}{m.group('name')}({m.group('col')}, {m.group('row')}, "
                    f"{m.group('friendly')}, 0x{color:06X}, {_fmt(r['max'])}, {_fmt(r['cap'])}, "
                    f"{_fmt(r['cx'])}, {_fmt(r['cy'])}){m.group('end')}{m.group('rest')}")
            seen.add(m.group("name"))
        out.append(line)
    missing = set(results) - seen
    if missing:
        raise ValueError(f"constants not found in {ENUM.name}: {sorted(missing)}")
    return newline.join(out)


def cells_from_enum(enum_text):
    cells = {}
    for line in enum_text.splitlines():
        m = ENUM_LINE.match(line)
        if m:
            cells[m.group("name")] = (int(m.group("col")), int(m.group("row")))
    return cells


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if GuideSignShieldType differs from the atlas")
    args = ap.parse_args()

    enum_text = ENUM.read_bytes().decode("utf-8")
    atlas = np.asarray(Image.open(ATLAS).convert("RGBA"))
    cells = cells_from_enum(enum_text)
    digit_w = digit_width_per_cap()
    results = {}
    for name, marker in MARKERS.items():
        col, row = cells[name]
        results[name] = measure(atlas, col, row, marker, digit_w)
        r = results[name]
        print(f"{name:22s} max={r['max']:.2f} cap={r['cap']:.2f} centre=({r['cx']:.3f}, {r['cy']:.3f})")

    new_text = rewrite(enum_text, results)
    if args.check:
        if new_text != enum_text:
            print(f"DRIFT: {ENUM.name} does not match the atlas; re-run without --check",
                  file=sys.stderr)
            return 1
        print("OK: route number placement matches the atlas")
        return 0
    if new_text != enum_text:
        ENUM.write_bytes(new_text.encode("utf-8"))
        print(f"wrote {ENUM.relative_to(REPO)}")
    else:
        print("unchanged")
    return 0


if __name__ == "__main__":
    sys.exit(main())
