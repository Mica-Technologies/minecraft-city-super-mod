#!/usr/bin/env python3
"""Generate the programmable-visibility (PV) lens atlas from the signal light atlas.

A 3M / McCain optically programmed head puts a diffusing optical system in front of its LED
array, so from the road the lens reads as one smooth glowing disc rather than a grid of dots,
while the legend -- an arrow, a bicycle, the edge of the lens itself -- stays sharp. The signal
head renderer draws a PROGRAMMABLE section's lens from ``atlas_pv.png`` instead of
``atlas.png``; this script makes that file.

The filter is an edge-preserving "smart blur", not a plain one. A plain Gaussian softens the
arrow as much as the dots, and the user could see it. Instead each tile is blurred, and then
the blurred and original tiles are mixed by an edge mask: where the tile has a strong,
large-scale luminance edge (a legend boundary, the lens rim) the original pixels are kept;
everywhere else (the LED dot texture, which no longer exists after a small blur) the blurred
pixels are used. The mask is grown by a couple of pixels so an edge keeps its crisp shoulder.

Each tile is filtered on its own: the atlas is an 8x8 grid of 128 px tiles and a blur across
tile edges would fringe one lens with its neighbour's colour. Alpha is handled premultiplied.

Usage (from the repository root)::

    python dev-env-utils/scripts/gen_pv_lens_atlas.py [--check] [--preview PATH]

``--check`` writes nothing and exits 1 if the committed file differs from what the script
would produce. ``--preview`` also writes a before/after strip of a few tiles for eyeballing.
"""
from __future__ import annotations

import argparse
import io
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

REPO = Path(__file__).resolve().parents[2]
LIGHTS = REPO / "modules/roads/src/main/resources/assets/csm/textures/blocks/trafficsignals/lights"
SOURCE = LIGHTS / "atlas.png"
TARGET = LIGHTS / "atlas_pv.png"

TILES_PER_SIDE = 8

# Radius of the smoothing blur, in source pixels. The LED dots in a 128 px tile sit about 6 px
# apart, so 3 merges them into a continuous glow.
SMOOTH_RADIUS = 3.0

# Radius of the blur the edge detector looks through. Big enough that the dot texture has gone
# before gradients are measured, small enough that a legend edge is still a step.
EDGE_LOOK_RADIUS = 1.6

# Luminance gradient (per pixel, 0..255 scale) below which a pixel is texture and above which it
# is an edge; linear between the two.
EDGE_LOW = 12.0
EDGE_HIGH = 40.0

# A second, finer look for thin strokes. The bicycle legend is drawn in one- and two-pixel
# lines that the coarse look above smears to almost nothing, so they are found instead as very
# strong gradients through a small blur -- stronger than any LED dot on its background manages.
FINE_LOOK_RADIUS = 0.8
FINE_LOW = 55.0
FINE_HIGH = 110.0

# How far the edge mask is grown, in pixels, so the crisp shoulder of an edge survives.
EDGE_GROW = 2

# Tiles shown in the --preview strip: red LED ball on, LED left arrow on (green), bike on (red).
PREVIEW_TILES = (33, 13, 5)


def premultiply(rgba: np.ndarray) -> np.ndarray:
    out = rgba.astype(np.float32)
    a = out[..., 3:4] / 255.0
    out[..., :3] *= a
    return out


def unpremultiply(pre: np.ndarray) -> np.ndarray:
    out = pre.copy()
    a = out[..., 3:4] / 255.0
    with np.errstate(divide="ignore", invalid="ignore"):
        rgb = np.where(a > 0, out[..., :3] / a, 0.0)
    out[..., :3] = rgb
    return np.clip(out, 0, 255)


def gaussian(pre: np.ndarray, radius: float) -> np.ndarray:
    """Gaussian blur of a premultiplied float RGBA array, through Pillow."""
    img = Image.fromarray(np.clip(pre, 0, 255).astype(np.uint8), "RGBA")
    return np.asarray(img.filter(ImageFilter.GaussianBlur(radius)), dtype=np.float32)


def luminance(pre: np.ndarray) -> np.ndarray:
    return 0.299 * pre[..., 0] + 0.587 * pre[..., 1] + 0.114 * pre[..., 2]


def gradient_mask(pre: np.ndarray, look_radius: float, low: float, high: float) -> np.ndarray:
    """Colour gradient through a blur of ``look_radius``, mapped linearly to 0..1.

    Measured per channel and the strongest channel taken, not on luminance: a red legend on a
    near-black disc is a large step in red and a small one in luminance, and a luminance
    detector let the bicycle blur away.
    """
    blurred = gaussian(pre, look_radius)
    magnitude = np.zeros(blurred.shape[:2], dtype=np.float32)
    for channel in range(3):
        gy, gx = np.gradient(blurred[..., channel])
        magnitude = np.maximum(magnitude, np.hypot(gx, gy))
    return np.clip((magnitude - low) / (high - low), 0.0, 1.0)


def edge_mask(pre: np.ndarray) -> np.ndarray:
    """0 where a pixel is fine texture, 1 where it sits on a legend edge or the lens rim."""
    mask = np.maximum(
        gradient_mask(pre, EDGE_LOOK_RADIUS, EDGE_LOW, EDGE_HIGH),
        gradient_mask(pre, FINE_LOOK_RADIUS, FINE_LOW, FINE_HIGH))
    if EDGE_GROW > 0:
        grown = Image.fromarray((mask * 255).astype(np.uint8), "L")
        grown = grown.filter(ImageFilter.MaxFilter(EDGE_GROW * 2 + 1))
        mask = np.asarray(grown, dtype=np.float32) / 255.0
    return mask


def smooth_tile(tile: Image.Image) -> Image.Image:
    """Smooth the lens texture inside its disc; leave the disc's outline and surround untouched.

    The tile is transparent outside the lens disc. A blur that ran over that edge would carry
    lens colour and alpha out past the rim, and in game that showed as a faint glow outside the
    visor. So the blur is a normalised convolution over the disc only (transparent pixels
    contribute nothing and are not darkened into), and the tile's own alpha is kept verbatim.
    """
    rgba = np.asarray(tile.convert("RGBA")).astype(np.float32)
    alpha = rgba[..., 3:4]
    inside = (alpha > 0).astype(np.float32)[..., 0]

    # Normalised convolution: blur colour weighted by the disc mask, then divide by the blurred
    # mask so pixels near the rim average only over disc pixels.
    weighted = np.zeros_like(rgba)
    weighted[..., :3] = rgba[..., :3] * inside[..., None]
    weighted[..., 3] = inside * 255.0
    blurred = gaussian(weighted, SMOOTH_RADIUS)
    weight = blurred[..., 3:4] / 255.0
    with np.errstate(divide="ignore", invalid="ignore"):
        blurred_rgb = np.where(weight > 1e-3, blurred[..., :3] / weight, rgba[..., :3])

    mask = edge_mask(premultiply(rgba))[..., None]
    mixed_rgb = mask * rgba[..., :3] + (1.0 - mask) * blurred_rgb
    out = rgba.copy()
    out[..., :3] = np.where(inside[..., None] > 0, mixed_rgb, rgba[..., :3])
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


def build(source: Image.Image) -> Image.Image:
    source = source.convert("RGBA")
    w, h = source.size
    tw, th = w // TILES_PER_SIDE, h // TILES_PER_SIDE
    out = Image.new("RGBA", source.size, (0, 0, 0, 0))
    for row in range(TILES_PER_SIDE):
        for col in range(TILES_PER_SIDE):
            box = (col * tw, row * th, (col + 1) * tw, (row + 1) * th)
            out.paste(smooth_tile(source.crop(box)), box)
    return out


def tile_box(image: Image.Image, index: int) -> tuple[int, int, int, int]:
    tw, th = image.size[0] // TILES_PER_SIDE, image.size[1] // TILES_PER_SIDE
    row, col = divmod(index, TILES_PER_SIDE)
    return col * tw, row * th, (col + 1) * tw, (row + 1) * th


def preview(source: Image.Image, result: Image.Image, path: Path) -> None:
    tw = source.size[0] // TILES_PER_SIDE
    th = source.size[1] // TILES_PER_SIDE
    strip = Image.new("RGBA", (tw * 2 * len(PREVIEW_TILES), th), (40, 40, 40, 255))
    for i, index in enumerate(PREVIEW_TILES):
        box = tile_box(source, index)
        strip.paste(source.crop(box), (i * 2 * tw, 0), source.crop(box))
        strip.paste(result.crop(box), (i * 2 * tw + tw, 0), result.crop(box))
    strip.save(path)


def encode(image: Image.Image) -> bytes:
    buf = io.BytesIO()
    image.save(buf, format="PNG", optimize=True)
    return buf.getvalue()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--check", action="store_true",
                        help="write nothing; exit 1 if the committed atlas_pv.png is stale")
    parser.add_argument("--preview", type=Path, default=None,
                        help="also write a before/after strip of a few tiles to this path")
    args = parser.parse_args()

    source = Image.open(SOURCE).convert("RGBA")
    result = build(source)
    if args.preview:
        preview(source, result, args.preview)
        print(f"wrote preview {args.preview}")
    if args.check:
        if not TARGET.exists():
            print(f"missing: {TARGET}")
            return 1
        existing = Image.open(TARGET).convert("RGBA")
        if existing.size != result.size or existing.tobytes() != result.tobytes():
            print(f"stale: {TARGET} differs from what {Path(__file__).name} would write")
            return 1
        print("atlas_pv.png is up to date")
        return 0

    TARGET.write_bytes(encode(result))
    print(f"wrote {TARGET} ({result.size[0]}x{result.size[1]})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
