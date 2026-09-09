"""Generate the lane control controller cabinet's textures.

A lane-use control cabinet is the same roadside enclosure as a signal controller, so it reuses
the signal controller's OBJ. What it does not share is the livery: a McCain ATC-style cabinet
in traffic yellow reads as a different machine at a glance, which is the whole point when the
two stand within sight of each other.

Rather than draw a second cabinet, this recolours the existing textures. Every panel, louvre and
door line is preserved exactly, and only the hue moves -- so if the signal controller's artwork
is ever redrawn, re-running this keeps the two in step instead of leaving one stale.

Writes, into modules/roads/src/main/resources/assets/csm/textures/blocks/trafficsignals/:
    lane_control_cabinet_front.png
    lane_control_cabinet_side.png

Run from the repo root:  python dev-env-utils/scripts/gen_lane_control_cabinet_textures.py
"""

import colorsys
import os

from PIL import Image

DIR = os.path.join('modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm',
                   'textures', 'blocks', 'trafficsignals')

SOURCES = {
    'signal_controller_front.png': 'lane_control_cabinet_front.png',
    'signal_controller_side.png': 'lane_control_cabinet_side.png',
}

# Traffic yellow, as a hue in the 0-1 space colorsys uses.
TARGET_HUE = 0.13

# Yellow is only legible as yellow when it is both bright and not quite fully saturated: at full
# saturation it reads as gold, and at the source's own brightness it reads as olive. Neither is
# fixable with a gain, because the source cabinet's paint occupies a narrow dark band -- so its
# range is measured and stretched into the bright band instead, which keeps the panel shading in
# proportion rather than flattening or clipping it.
VALUE_FLOOR = 0.85
VALUE_CEILING = 1.0
SATURATION_CAP = 0.95

# Pixels this dark are shadow lines and panel gaps rather than paint. Recolouring them turns
# every crease into a brown smear, so they are left alone.
SHADOW_VALUE = 0.18


def is_paint(pixel):
    """Whether a pixel is painted cabinet rather than a shadow line or a grey fitting.

    Shadows and neutrals are left alone deliberately: a cabinet is not yellow all over, and
    recolouring the creases turns every panel gap into a smear.
    """
    r, g, b, a = pixel
    if a == 0:
        return False
    _, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
    return v > SHADOW_VALUE and s >= 0.08


def paint_value_range(pixels):
    """The value range the source's paint actually occupies, which is what gets stretched."""
    values = []
    for r, g, b, a in pixels:
        if is_paint((r, g, b, a)):
            values.append(colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)[2])
    if not values:
        raise SystemExit('no painted pixels found -- is the source texture what it used to be?')
    return min(values), max(values)


def recolour(pixel, low, high):
    r, g, b, a = pixel
    if not is_paint(pixel):
        return pixel
    h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
    span = high - low
    fraction = 0.5 if span <= 0.0 else (v - low) / span
    v = VALUE_FLOOR + fraction * (VALUE_CEILING - VALUE_FLOOR)
    nr, ng, nb = colorsys.hsv_to_rgb(TARGET_HUE, min(SATURATION_CAP, max(s, 0.85)), v)
    return (int(round(nr * 255)), int(round(ng * 255)), int(round(nb * 255)), a)


def main():
    if not os.path.isdir(DIR):
        raise SystemExit('run from the repo root: %s not found' % DIR)

    for source, target in SOURCES.items():
        path = os.path.join(DIR, source)
        if not os.path.isfile(path):
            raise SystemExit('missing source texture: %s' % path)
        image = Image.open(path).convert('RGBA')
        pixels = list(image.getdata())
        low, high = paint_value_range(pixels)
        recoloured = [recolour(p, low, high) for p in pixels]

        changed = sum(1 for a, b in zip(pixels, recoloured) if a != b)
        if changed == 0:
            raise SystemExit('%s: nothing was recoloured -- the hue shift did nothing' % source)

        out = Image.new('RGBA', image.size)
        out.putdata(recoloured)
        out_path = os.path.join(DIR, target)
        out.save(out_path)
        print('wrote %s  %dx%d  (%d of %d pixels recoloured)'
              % (out_path, out.width, out.height, changed, len(pixels)))


if __name__ == '__main__':
    main()
