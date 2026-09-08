"""Generate the RRFB (Rectangular Rapid Flashing Beacon) block textures.

An RRFB is two rectangular amber indications in one housing that fire rapid bursts while a
pedestrian is crossing, and sit dark the rest of the time.

The animation is the flash sequence FHWA Interim Approval IA-21 requires, at 75 flashing
sequences per minute -- one 800 ms sequence built entirely from 50 ms units, which is one
Minecraft tick, so it lands exactly on the animation clock:

    left 50, dark 50, right 50, dark 50, left 50, dark 50, right 50, dark 50,
    both 50, dark 50, both 50, dark 250

A wig-wag, then a simultaneous double flash, then a long dark gap. Note that the two
indications *do* fire together twice per sequence: an implementation that keeps them
strictly alternating is not an RRFB.

    https://mutcd.fhwa.dot.gov/resources/interim_approval/ia21/index.htm

The sequence is drawn into the texture rather than driven per-lamp at render time, because
Minecraft advances texture animations off the global tick counter -- so every RRFB in the
world stays in step, which is what a pair facing each other across a crossing needs.

Housing and lamps are separate textures so the housing can be recoloured without repeating
the 16-frame animation once per colour. The housing colours are read out of
TrafficSignalBodyColor.java rather than restated here, so the two cannot drift apart.

Writes, into modules/roads/src/main/resources/assets/csm/textures/blocks/trafficsignals/
shared_textures/:
    rrfb_housing_<colour>.png   one per TrafficSignalBodyColor value
    rrfb_lens_off.png           both lamps dark   (the controller's OFF state)
    rrfb_lens_flash.png         16-frame strip    (any called state)
    rrfb_lens_flash.png.mcmeta

Run from the repo root:  python dev-env-utils/scripts/gen_rrfb_textures.py
"""

import os
import re

from PIL import Image, ImageDraw

FRAME = 64  # one animation frame, square as Minecraft requires

# The IA-21 sequence, one entry per 50 ms frame, as (left lit, right lit). Written out in
# full rather than computed so it can be read straight against the standard's wording.
L, R, B, _ = (True, False), (False, True), (True, True), (False, False)
SEQUENCE = [
    L, _,   # left 50, dark 50
    R, _,   # right 50, dark 50
    L, _,   # left 50, dark 50
    R, _,   # right 50, dark 50
    B, _,   # both 50, dark 50
    B,      # both 50
    _, _, _, _, _,  # dark 250
]
CYCLE_FRAMES = len(SEQUENCE)

# The block model's face is 15 x 4 block units and samples uv [0.5, 6, 15.5, 10] of 16,
# which at 4 px per unit is exactly x 2..61, y 24..39 -- so the unit maps to the face one
# texture pixel per model pixel, with no stretch. Wide and shallow, with the lamps pushed
# out to the ends and a broad dark centre, the way a real bar is built. Half a unit of
# inset each side keeps the housing off the block boundary, where coplanar faces z-fight.
BAND_LEFT = 2
BAND_RIGHT = 61
BAND_TOP = 24
BAND_BOTTOM = 39

# Solid housing swatch in the frame's top-left corner, sampled by the model's side, top and
# bottom faces (uv [0, 0, 1, 1]). Kept well outside the band so it is never near a lamp.
SWATCH = 4

# Lamp rectangles at each end of the bar. The gap between them is deliberately wide -- on a
# real unit the middle carries the maker's plate, not lamps.
LENS_W = 14
LENS_H = 8
LENS_PAD = 4
LENS_Y = BAND_TOP + (BAND_BOTTOM - BAND_TOP + 1 - LENS_H) // 2
LENS_X_LEFT = BAND_LEFT + LENS_PAD
LENS_X_RIGHT = BAND_RIGHT - LENS_PAD - LENS_W + 1

LENS_DARK = (48, 34, 10, 255)
LENS_DARK_EDGE = (28, 20, 6, 255)
LENS_LIT = (255, 176, 20, 255)
LENS_LIT_CORE = (255, 226, 150, 255)

REPO_TEXTURES = os.path.join('modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm',
                             'textures', 'blocks', 'trafficsignals', 'shared_textures')
COLOR_ENUM = os.path.join('modules', 'roads', 'src', 'main', 'java', 'com', 'micatechnologies',
                          'minecraft', 'csm', 'trafficsignals', 'logic',
                          'TrafficSignalBodyColor.java')

ENUM_RE = re.compile(
    r'^\s*[A-Z_0-9]+\(\s*"([a-z_0-9]+)"\s*,\s*"[^"]*"\s*,\s*'
    r'([0-9.]+)F\s*,\s*([0-9.]+)F\s*,\s*([0-9.]+)F\s*\)', re.M)


def read_colors():
    """Housing colours, straight out of the Java enum so the two cannot drift."""
    with open(COLOR_ENUM, encoding='utf-8', newline='') as fh:
        source = fh.read()
    colors = [(name, (int(round(float(r) * 255)), int(round(float(g) * 255)),
                      int(round(float(b) * 255))))
              for name, r, g, b in ENUM_RE.findall(source)]
    if not colors:
        raise SystemExit('parsed no colours out of %s' % COLOR_ENUM)
    return colors


def shade(rgb, factor):
    return tuple(min(255, max(0, int(round(c * factor)))) for c in rgb) + (255,)


def draw_housing(rgb):
    """The bar itself in one colour, with no lamps -- those are a separate texture."""
    img = Image.new('RGBA', (FRAME, FRAME), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    body = shade(rgb, 1.0)
    # A darker edge and a lighter top line so the bar reads as a physical box. Both are
    # derived from the colour rather than fixed, so they work for the near-black finishes
    # and the pale ones alike -- a fixed highlight vanishes on off-white and blows out on
    # glossy black.
    d.rectangle([BAND_LEFT, BAND_TOP, BAND_RIGHT, BAND_BOTTOM],
                fill=body, outline=shade(rgb, 0.55))
    d.line([BAND_LEFT + 1, BAND_TOP + 1, BAND_RIGHT - 1, BAND_TOP + 1], fill=shade(rgb, 1.45))
    # No lamp wells are drawn here. The lamp plate covers this area on any face that has
    # lamps, so a well is never visible there -- but the housing's back face samples the same
    # band, so drawing them put dead lamps on the back of a single-sided unit, which is the
    # one thing single-sided is supposed to avoid.
    d.rectangle([0, 0, SWATCH - 1, SWATCH - 1], fill=body)
    return img


def draw_lens(left_lit, right_lit):
    """Just the two lamps, transparent everywhere else so the housing shows through."""
    img = Image.new('RGBA', (FRAME, FRAME), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for x, lit in ((LENS_X_LEFT, left_lit), (LENS_X_RIGHT, right_lit)):
        box = [x, LENS_Y, x + LENS_W - 1, LENS_Y + LENS_H - 1]
        if lit:
            d.rectangle(box, fill=LENS_LIT, outline=LENS_DARK_EDGE)
            d.rectangle([x + 3, LENS_Y + 2, x + LENS_W - 4, LENS_Y + LENS_H - 3],
                        fill=LENS_LIT_CORE)
        else:
            d.rectangle(box, fill=LENS_DARK, outline=LENS_DARK_EDGE)
    return img


def main():
    if not os.path.isdir(REPO_TEXTURES):
        raise SystemExit('run from the repo root: %s not found' % REPO_TEXTURES)

    colors = read_colors()
    for name, rgb in colors:
        draw_housing(rgb).save(os.path.join(REPO_TEXTURES, 'rrfb_housing_%s.png' % name))
    print('wrote %d housing textures (%s ... %s)' % (len(colors), colors[0][0], colors[-1][0]))

    off_path = os.path.join(REPO_TEXTURES, 'rrfb_lens_off.png')
    draw_lens(False, False).save(off_path)
    print('wrote %s' % off_path)

    strip = Image.new('RGBA', (FRAME, FRAME * CYCLE_FRAMES), (0, 0, 0, 0))
    for i, (left, right) in enumerate(SEQUENCE):
        strip.paste(draw_lens(left, right), (0, i * FRAME))
    strip_path = os.path.join(REPO_TEXTURES, 'rrfb_lens_flash.png')
    strip.save(strip_path)
    print('wrote %s  %dx%d  (%d frames)'
          % (strip_path, strip.width, strip.height, CYCLE_FRAMES))

    # frametime 1 = one tick = 50 ms, which is the pulse width the sequence is built on.
    with open(strip_path + '.mcmeta', 'w', encoding='utf-8', newline='\n') as fh:
        fh.write('{\n  "animation": {\n    "frametime": 1\n  }\n}\n')

    # Check the sequence against IA-21 rather than against an idea of what it should be.
    assert CYCLE_FRAMES * 50 == 800, 'sequence must be 800 ms, got %d' % (CYCLE_FRAMES * 50)
    both = sum(1 for l, r in SEQUENCE if l and r)
    dark_tail = 0
    for l, r in reversed(SEQUENCE):
        if l or r:
            break
        dark_tail += 1
    assert both == 2, 'IA-21 ends its wig-wag with two simultaneous flashes, got %d' % both
    assert dark_tail * 50 == 250, 'trailing dark gap must be 250 ms, got %d' % (dark_tail * 50)
    print('checked: 800 ms sequence, %d simultaneous flashes, %d ms dark tail'
          % (both, dark_tail * 50))


if __name__ == '__main__':
    main()
