"""Generate the RRFB (Rectangular Rapid Flashing Beacon) block textures.

An RRFB is two rectangular amber indications in one housing that fire alternating rapid
bursts while a pedestrian is crossing, and sit dark the rest of the time. The alternation
is internal to the unit, so it is drawn as a single animated texture rather than being
driven per-lamp at render time -- Minecraft advances texture animations off the global
tick counter, so every RRFB in the world stays in step with every other one.

The animation is the flash sequence FHWA Interim Approval IA-21 requires, at 75 flashing
sequences per minute -- one 800 ms sequence built entirely from 50 ms units, which is one
Minecraft tick, so it lands exactly on the animation clock:

    left 50, dark 50, right 50, dark 50, left 50, dark 50, right 50, dark 50,
    both 50, dark 50, both 50, dark 250

A wig-wag, then a simultaneous double flash, then a long dark gap. Note that the two
indications *do* fire together twice per sequence: an implementation that keeps them
strictly alternating is not an RRFB.

    https://mutcd.fhwa.dot.gov/resources/interim_approval/ia21/index.htm

Writes, into modules/roads/src/main/resources/assets/csm/textures/blocks/trafficsignals/
shared_textures/:
    rrfb_off.png     one frame, both lamps dark  (the controller's OFF state)
    rrfb_flash.png   16-frame strip              (any called state)
    rrfb_flash.png.mcmeta

Run from the repo root:  python dev-env-utils/scripts/gen_rrfb_textures.py
"""

import os

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

# Solid housing swatch in the frame's top-left corner, sampled by the model's side/top/bottom
# faces (uv [0, 0, 1, 1]). Kept outside the band so it is never near a lens.
SWATCH = 4

HOUSING = (26, 26, 28, 255)
HOUSING_EDGE = (14, 14, 15, 255)
HOUSING_HILIGHT = (44, 44, 47, 255)
LENS_DARK = (48, 34, 10, 255)
LENS_DARK_EDGE = (28, 20, 6, 255)
LENS_LIT = (255, 176, 20, 255)
LENS_LIT_CORE = (255, 226, 150, 255)

OUT_DIR = os.path.join('modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm',
                       'textures', 'blocks', 'trafficsignals', 'shared_textures')

# Lens rectangles at each end of the bar. The gap between them is deliberately wide -- on a
# real unit the middle carries the maker's plate, not lamps.
LENS_W = 14
LENS_H = 8
LENS_PAD = 4
LENS_Y = BAND_TOP + (BAND_BOTTOM - BAND_TOP + 1 - LENS_H) // 2
LENS_X_LEFT = BAND_LEFT + LENS_PAD
LENS_X_RIGHT = BAND_RIGHT - LENS_PAD - LENS_W + 1


def draw_frame(left_lit, right_lit):
    """One frame: the housing band with each lamp lit or dark."""
    img = Image.new('RGBA', (FRAME, FRAME), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Housing plate across the band, with a darker edge and a top highlight so the unit
    # reads as a physical box rather than a flat decal.
    d.rectangle([BAND_LEFT, BAND_TOP, BAND_RIGHT, BAND_BOTTOM],
                fill=HOUSING, outline=HOUSING_EDGE)
    d.line([BAND_LEFT + 1, BAND_TOP + 1, BAND_RIGHT - 1, BAND_TOP + 1], fill=HOUSING_HILIGHT)

    # Plain-housing swatch in the corner, outside the band the front face samples. The model's
    # side, top and bottom faces map to this, so they stay dark instead of picking up whatever
    # happens to be under them in the band -- sampling the band directly made the housing edges
    # glow amber every time the lamp beneath them lit.
    d.rectangle([0, 0, SWATCH - 1, SWATCH - 1], fill=HOUSING)

    for x, lit in ((LENS_X_LEFT, left_lit), (LENS_X_RIGHT, right_lit)):
        box = [x, LENS_Y, x + LENS_W - 1, LENS_Y + LENS_H - 1]
        if lit:
            d.rectangle(box, fill=LENS_LIT, outline=HOUSING_EDGE)
            # Bright core, inset, so a lit lamp has some depth to it.
            d.rectangle([x + 3, LENS_Y + 2, x + LENS_W - 4, LENS_Y + LENS_H - 3],
                        fill=LENS_LIT_CORE)
        else:
            d.rectangle(box, fill=LENS_DARK, outline=LENS_DARK_EDGE)

    return img


def main():
    if not os.path.isdir(OUT_DIR):
        raise SystemExit('run from the repo root: %s not found' % OUT_DIR)

    off = draw_frame(False, False)
    off_path = os.path.join(OUT_DIR, 'rrfb_off.png')
    off.save(off_path)
    print('wrote %s  %dx%d' % (off_path, off.width, off.height))

    strip = Image.new('RGBA', (FRAME, FRAME * CYCLE_FRAMES), (0, 0, 0, 0))
    for i, (left, right) in enumerate(SEQUENCE):
        strip.paste(draw_frame(left, right), (0, i * FRAME))
    strip_path = os.path.join(OUT_DIR, 'rrfb_flash.png')
    strip.save(strip_path)
    print('wrote %s  %dx%d  (%d frames)'
          % (strip_path, strip.width, strip.height, CYCLE_FRAMES))

    # frametime 1 = one tick = 50 ms, which is the pulse width the pattern is built on.
    meta_path = strip_path + '.mcmeta'
    with open(meta_path, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write('{\n  "animation": {\n    "frametime": 1\n  }\n}\n')
    print('wrote %s' % meta_path)

    # Check the sequence against IA-21 rather than against an idea of what it should be.
    assert CYCLE_FRAMES * 50 == 800, 'sequence must be 800 ms, got %d' % (CYCLE_FRAMES * 50)
    left = sum(1 for l, _r in SEQUENCE if l)
    right = sum(1 for _l, r in SEQUENCE if r)
    both = sum(1 for l, r in SEQUENCE if l and r)
    dark_tail = 0
    for l, r in reversed(SEQUENCE):
        if l or r:
            break
        dark_tail += 1
    assert both == 2, 'IA-21 ends its wig-wag with two simultaneous flashes, got %d' % both
    assert dark_tail * 50 == 250, 'trailing dark gap must be 250 ms, got %d' % (dark_tail * 50)
    print('checked: 800 ms sequence, %d left / %d right lit frames, %d simultaneous, '
          '%d ms dark tail' % (left, right, both, dark_tail * 50))


if __name__ == '__main__':
    main()
