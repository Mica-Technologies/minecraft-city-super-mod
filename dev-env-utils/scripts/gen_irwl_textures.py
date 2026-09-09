"""Generate the in-roadway warning light's textures.

An in-roadway warning light is a fixture set into the pavement at a crosswalk, flashing up and
along the road at approaching drivers. A row of them is installed across the approach, and the
units alternate rather than firing in unison -- so this writes the flash strip twice, the second
copy phase-shifted by half the cycle. The block picks between them from its position, which is
why the alternation needs no configuration and cannot be set up wrong.

The sequence is the RRFB's, deliberately: an in-roadway light is normally wired to the same
crossing call as the RRFB above it, and two fixtures on one call flashing to different rhythms
would look broken. It is imported from gen_rrfb_textures rather than restated, so the two cannot
drift apart -- which is the same reason that script reads its housing colours out of the Java
enum instead of listing them.

Minecraft advances texture animations off the global tick counter, so every fixture in the world
stays in step with every other and with the RRFB.

Writes, into modules/roads/src/main/resources/assets/csm/textures/blocks/trafficsignals/
shared_textures/:
    irwl_lens_off.png            dark            (the controller's OFF state)
    irwl_lens_<pattern>_a.png    one strip per pattern (any called state)
    irwl_lens_<pattern>_a.png.mcmeta
    irwl_lens_<pattern>_b.png    the same, half a cycle later
    irwl_lens_<pattern>_b.png.mcmeta
    irwl_body.png                the pavement-coloured housing the lens sits in

Run from the repo root:  python dev-env-utils/scripts/gen_irwl_textures.py
"""

import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from gen_rrfb_textures import SEQUENCE  # noqa: E402  (path set above)

FRAME = 32

# The fixture: a dark housing set into the road with an amber lens bar across it. Kept narrow
# in Y because the block draws it on a plate one sixteenth tall -- anything taller reads as a
# kerb rather than something flush.
BODY = (26, 26, 28, 255)
BODY_EDGE = (16, 16, 18, 255)
LENS_DARK = (44, 34, 12, 255)
LENS_LIT = (255, 186, 40, 255)

LENS_X1, LENS_X2 = 4, FRAME - 5
LENS_Y1, LENS_Y2 = 10, FRAME - 11

OUT_DIR = os.path.join('modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm',
                       'textures', 'blocks', 'trafficsignals', 'shared_textures')

MCMETA = '{\n  "animation": {\n    "frametime": 1\n  }\n}\n'


def draw_frame(lit):
    """One frame: the housing, with the lens bar lit or dark."""
    img = Image.new('RGBA', (FRAME, FRAME), BODY)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, FRAME - 1, FRAME - 1], outline=BODY_EDGE)
    d.rectangle([LENS_X1, LENS_Y1, LENS_X2, LENS_Y2],
                fill=LENS_LIT if lit else LENS_DARK, outline=BODY_EDGE)
    return img


def write_strip(name, lit_by_frame):
    # Sized from the list, not from the RRFB's frame count: the slower patterns are longer,
    # and a fixed height silently clipped them to the RRFB's 16 frames.
    frames = len(lit_by_frame)
    strip = Image.new('RGBA', (FRAME, FRAME * frames), (0, 0, 0, 0))
    for i, lit in enumerate(lit_by_frame):
        strip.paste(draw_frame(lit), (0, i * FRAME))
    path = os.path.join(OUT_DIR, name + '.png')
    strip.save(path)
    with open(path + '.mcmeta', 'w', encoding='utf-8', newline='\n') as handle:
        handle.write(MCMETA)
    print('wrote %s  %dx%d  (%d frames)' % (path, strip.width, strip.height, frames))


def even_flash_frames():
    """Half a second lit, half a second dark.

    Both the wig-wag and the plain flash run on this. What separates them is not the rhythm but
    whether neighbouring fixtures share it: the wig-wag's second phase is shifted by half a
    cycle so a pair alternates, and the plain flash's is not, so a row fires together.
    """
    return [True] * 10 + [False] * 10


def main():
    if not os.path.isdir(OUT_DIR):
        raise SystemExit('run from the repo root: %s not found' % OUT_DIR)

    # A pavement fixture has one lamp, not the RRFB's pair, so it is lit on any frame where the
    # RRFB shows anything at all.
    patterns = {
        'rrfb': [left or right for left, right in SEQUENCE],
        'wig_wag': even_flash_frames(),
        'flash': even_flash_frames(),
    }
    # FLASH is the one pattern where the fixtures fire together rather than alternating -- that
    # is what "regular flash" means, and shifting it would just be a second wig-wag.
    unshifted = {'flash'}

    for name, frames in patterns.items():
        half = len(frames) // 2
        shifted = frames if name in unshifted else frames[half:] + frames[:half]
        write_strip('irwl_lens_%s_a' % name, frames)
        write_strip('irwl_lens_%s_b' % name, shifted)
        if name not in unshifted:
            differing = sum(1 for a, b in zip(frames, shifted) if a != b)
            if differing == 0:
                raise SystemExit('%s: the two phases are identical -- the shift did nothing'
                                 % name)
            print('  %s: phases differ on %d of %d frames' % (name, differing, len(frames)))

    dark = draw_frame(False)
    dark.save(os.path.join(OUT_DIR, 'irwl_lens_off.png'))
    print('wrote irwl_lens_off.png')

    body = Image.new('RGBA', (FRAME, FRAME), BODY)
    ImageDraw.Draw(body).rectangle([0, 0, FRAME - 1, FRAME - 1], outline=BODY_EDGE)
    body.save(os.path.join(OUT_DIR, 'irwl_body.png'))
    print('wrote irwl_body.png')


if __name__ == '__main__':
    main()
