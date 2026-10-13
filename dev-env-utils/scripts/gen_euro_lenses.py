#!/usr/bin/env python3
"""Generate the European signal lens tiles for the signal light atlas.

A European LED head (the modular polycarbonate kind) shows a lens that reads as one evenly lit
disc from the road: a diffuser in front of the LEDs, with a faint Fresnel ring structure and a
slightly brighter centre, and no visible dots. Dark, the lens is a deep tint of its colour with
the same rings and a soft reflection, built to show no "phantom" in low sun. An arrow is a
black mask over the whole disc with only the arrow lit: a thick stem and a broad triangular
head, drawn pointing LEFT and centred, because the renderer turns that one tile for every
other direction. The pedestrian lenses are masked the same way round a figure: a standing
figure in red, a walking one in green (a generic pictogram, not any city's own figure).

Sixteen 128 px tiles, appended to the atlas by ``ImageTilerTool`` (cells 64-79), in the order
``TrafficSignalTextureMap`` reads them: ball off G/Y/R, ball on G/Y/R, arrow off G/Y/R, arrow
on G/Y/R, then standing figure off/on (red) and walking figure off/on (green). Drawn at four times the size and reduced by area averaging, so edges are smooth.
Nothing random: the same tiles come out on every machine.

Usage (from the repository root)::

    python dev-env-utils/scripts/gen_euro_lenses.py [--check] [--sheet PATH]

``--check`` writes nothing and exits 1 if a committed tile differs from what the script would
write. ``--sheet`` also writes a contact sheet of the twelve tiles on grey.
"""
from __future__ import annotations

import argparse
import io
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

REPO = Path(__file__).resolve().parents[2]
LIGHTS = REPO / "modules/roads/src/main/resources/assets/csm/textures/blocks/trafficsignals/lights"
SHARED = LIGHTS.parent / "shared_textures"

# The inventory models paint the head's body from a corner of the lens texture, so their lens
# textures are opaque: the lit lens on the dark ground the other inventory lenses use.
ICON_GROUND = (24, 24, 24, 255)

TILE = 128
SCALE = 4
BIG = TILE * SCALE

# Lens colours, lit and dark, in the atlas's colour order (green, yellow, red). The lit colours
# follow the European signal colours: a blue-leaning green, an orange-leaning amber.
COLOURS = {
    "green": ((22, 236, 146), (6, 40, 28)),
    "yellow": ((255, 168, 12), (48, 30, 6)),
    "red": ((255, 40, 26), (46, 9, 8)),
}
COLOUR_ORDER = ("green", "yellow", "red")

BEZEL = (70, 72, 74)        # thin rim round the lens, as the other lens tiles have
MASK = (14, 14, 15)         # the black mask an arrow lens is lit through
BEZEL_WIDTH = 2.0           # in tile pixels
RING_PITCH = 7.0            # Fresnel ring spacing, in tile pixels
ARROW_OFF_LEVEL = 0.16      # how much of its colour a dark arrow still shows

# The arrow, pointing left, in tile pixels: the head's tip and base, and the stem.
ARROW_TIP_X = 18.0
ARROW_HEAD_BASE_X = 60.0
ARROW_HEAD_HALF = 36.0
ARROW_STEM_END_X = 108.0
ARROW_STEM_HALF = 12.0


def grid():
    """Pixel-centre coordinates of the big canvas, in tile pixels, centred on the tile."""
    c = (np.arange(BIG, dtype=np.float64) + 0.5) / SCALE - TILE / 2
    return np.meshgrid(c, c)  # x, y (y down)


def arrow_mask() -> np.ndarray:
    """1 inside the left-pointing arrow, 0 outside, on the big canvas."""
    img = Image.new("L", (BIG, BIG), 0)
    d = ImageDraw.Draw(img)
    cy = TILE / 2
    s = SCALE
    d.polygon([(ARROW_TIP_X * s, cy * s),
               (ARROW_HEAD_BASE_X * s, (cy - ARROW_HEAD_HALF) * s),
               (ARROW_HEAD_BASE_X * s, (cy + ARROW_HEAD_HALF) * s)], fill=255)
    d.rectangle([(ARROW_HEAD_BASE_X - 1) * s, (cy - ARROW_STEM_HALF) * s,
                 ARROW_STEM_END_X * s, (cy + ARROW_STEM_HALF) * s], fill=255)
    return np.asarray(img, dtype=np.float64) / 255.0


def stroke_mask(strokes, dots) -> np.ndarray:
    """1 inside a figure drawn as round-capped strokes and round dots, on the big canvas.

    ``strokes`` are (points, width) in tile pixels, ``dots`` (centre, radius).
    """
    img = Image.new("L", (BIG, BIG), 0)
    d = ImageDraw.Draw(img)
    s = SCALE
    for points, width in strokes:
        pts = [(x * s, y * s) for x, y in points]
        d.line(pts, fill=255, width=int(round(width * s)), joint="curve")
        for x, y in pts:
            r = width * s / 2
            d.ellipse([x - r, y - r, x + r, y + r], fill=255)
    for (x, y), r in dots:
        d.ellipse([(x - r) * s, (y - r) * s, (x + r) * s, (y + r) * s], fill=255)
    return np.asarray(img, dtype=np.float64) / 255.0


def standing_mask() -> np.ndarray:
    """The standing figure: facing the viewer, arms at its sides, feet together."""
    return stroke_mask([
        ([(64, 42), (64, 70)], 22),           # torso, shoulder to hip
        ([(50, 42), (47, 72)], 7),            # arms
        ([(78, 42), (81, 72)], 7),
        ([(58, 76), (58, 106)], 10),          # legs
        ([(70, 76), (70, 106)], 10),
    ], [((64, 25), 10)])


def walking_mask() -> np.ndarray:
    """The walking figure: side on, mid-stride, arms swinging, heading right."""
    return stroke_mask([
        ([(66, 41), (60, 70)], 18),           # torso, leaning into the stride
        ([(65, 44), (78, 57), (85, 70)], 9),  # front arm
        ([(62, 44), (49, 56), (43, 68)], 9),  # back arm
        ([(60, 72), (73, 89), (80, 107)], 12),  # front leg
        ([(60, 72), (51, 91), (38, 104)], 12),  # back leg
    ], [((70, 24), 10)])


LEGENDS = {"arrow": arrow_mask, "standing": standing_mask, "walking": walking_mask}


def lens(colour: str, lit: bool, arrow) -> Image.Image:
    """One lens tile. ``arrow`` is False for a plain disc, True for the arrow, or the name of a
    masked legend in ``LEGENDS``."""
    on, off = COLOURS[colour]
    x, y = grid()
    r = np.hypot(x, y)
    outer = TILE / 2 - 0.5
    inner = outer - BEZEL_WIDTH
    rn = np.clip(r / inner, 0.0, 1.0)
    rings = np.cos(2 * np.pi * r / RING_PITCH)

    if lit:
        base = np.array(on, dtype=np.float64)
        core = base + (255.0 - base) * 0.45
        glow = 0.8 * np.exp(-(rn / 0.35) ** 2)
        rgb = base[None, None, :] * (1 - glow[..., None]) + core[None, None, :] * glow[..., None]
        rgb *= (1.0 + 0.05 * rings)[..., None]
        rgb *= (1.0 - 0.18 * rn ** 4)[..., None]
    else:
        rgb = np.broadcast_to(np.array(off, dtype=np.float64), r.shape + (3,)).copy()
        rgb *= (1.0 + 0.10 * rings)[..., None]

    if arrow:
        legend = LEGENDS["arrow" if arrow is True else arrow]()
        mask_rgb = np.array(MASK, dtype=np.float64)[None, None, :] * (1.0 + 0.08 * rings)[..., None]
        if lit:
            legend_rgb = rgb
        else:
            legend_rgb = np.array(on, dtype=np.float64)[None, None, :] * ARROW_OFF_LEVEL \
                * (1.0 + 0.10 * rings)[..., None]
        rgb = legend_rgb * legend[..., None] + mask_rgb * (1 - legend[..., None])

    if not lit:
        # A soft reflection up and to the left: the lens is glossy, not a hole.
        sx, sy = -0.32 * inner, -0.36 * inner
        spec = 0.10 * np.exp(-((x - sx) ** 2 + (y - sy) ** 2) / (0.28 * inner) ** 2)
        rgb = rgb * (1 - spec[..., None]) + 255.0 * spec[..., None]

    bezel = (r > inner) & (r <= outer)
    rgb[bezel] = BEZEL
    alpha = np.where(r <= outer, 255.0, 0.0)

    rgba = np.dstack([np.clip(rgb, 0, 255), alpha])
    # Reduce by area averaging, colour weighted by alpha so the rim does not darken.
    pre = rgba.copy()
    pre[..., :3] *= pre[..., 3:4] / 255.0
    pre = pre.reshape(TILE, SCALE, TILE, SCALE, 4).mean(axis=(1, 3))
    a = pre[..., 3:4]
    with np.errstate(divide="ignore", invalid="ignore"):
        pre[..., :3] = np.where(a > 0, pre[..., :3] * 255.0 / a, 0.0)
    return Image.fromarray(np.clip(np.rint(pre), 0, 255).astype(np.uint8), "RGBA")


def tiles() -> list[tuple[str, Image.Image]]:
    """The twelve tiles, in atlas order."""
    out = []
    for arrow in (False, True):
        for lit in (False, True):
            for colour in COLOUR_ORDER:
                kind = "euro_arrow" if arrow else "euro"
                name = f"{kind}_{colour}_{'on' if lit else 'off'}"
                out.append((name, lens(colour, lit, arrow)))
    for legend, colour in (("standing", "red"), ("walking", "green")):
        for lit in (False, True):
            out.append((f"euro_ped_{legend}_{'on' if lit else 'off'}", lens(colour, lit, legend)))
    return out


def icons() -> list[tuple[Path, Image.Image]]:
    """The pedestrian heads' inventory lens textures: each figure lit, on an opaque ground."""
    out = []
    for legend, colour in (("standing", "red"), ("walking", "green")):
        icon = Image.new("RGBA", (TILE, TILE), ICON_GROUND)
        icon.alpha_composite(lens(colour, True, legend))
        out.append((SHARED / f"euro_ped_{legend}_item.png", icon.convert("RGB").convert("RGBA")))
    return out


def encode(image: Image.Image) -> bytes:
    buf = io.BytesIO()
    image.save(buf, format="PNG", optimize=True)
    return buf.getvalue()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--check", action="store_true",
                        help="write nothing; exit 1 if a committed tile is stale")
    parser.add_argument("--sheet", type=Path, default=None,
                        help="also write a contact sheet of the tiles to this path")
    args = parser.parse_args()

    made = tiles()
    if args.sheet:
        rows = (len(made) + 5) // 6
        sheet = Image.new("RGBA", (6 * (TILE + 4), rows * (TILE + 4)), (70, 70, 70, 255))
        for i, (_, img) in enumerate(made):
            sheet.alpha_composite(img, ((i % 6) * (TILE + 4), (i // 6) * (TILE + 4)))
        sheet.save(args.sheet)
        print(f"wrote sheet {args.sheet}")

    stale = 0
    outputs = [(LIGHTS / f"{name}.png", img) for name, img in made] + icons()
    for path, img in outputs:
        if args.check:
            if not path.exists():
                print(f"missing: {path}")
                stale += 1
                continue
            existing = Image.open(path).convert("RGBA")
            if existing.size != img.size or existing.tobytes() != img.tobytes():
                print(f"stale: {path}")
                stale += 1
        else:
            path.write_bytes(encode(img))
    if args.check:
        if stale:
            return 1
        print("European lens tiles are up to date")
    else:
        print(f"wrote {len(made)} tiles to {LIGHTS} and {len(outputs) - len(made)} icon lenses")
    return 0


if __name__ == "__main__":
    sys.exit(main())
