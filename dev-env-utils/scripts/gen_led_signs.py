"""Generate the LED-enhanced flashing sign blocks: STOP, WRONG WAY and DO NOT ENTER with a ring
of red LEDs round the border that blink once a second.

These are the solar "LED-enhanced" signs (MUTCD 2A.07 / 2B.10): the ordinary sign panel with
LEDs mounted on the face along its edge, flashing to draw the eye at a crossing or a ramp with
a wrong-way problem. The LEDs run whenever the unit is installed, so the blocks have no
control -- placing one is enough.

The blink is drawn into the texture rather than driven per block at render time, for the
same reason the RRFB's sequence is: Minecraft advances texture animations off the global
tick counter, so every LED sign in the world blinks in step, as a bank of them on the same
solar controller does. Each sign is a two-frame strip -- LEDs dark, LEDs lit -- and the
.mcmeta gives the lit frame a short slot in a 20-tick cycle: one 100 ms blink per second.

Nothing about the sign geometry changes. The LEDs are composited into the sign's own face
texture, so the LED block uses exactly the blockstate of the plain sign with the texture
paths swapped, and the plain sign's model, shift and downward behaviour all carry over.

Each strip also gets an OptiFine emissive companion, ``<name>_e``, animated on the same
timing: an empty frame while the LEDs are dark and, for the lit frame, the lit LEDs and
their bloom alone on transparency. OptiFine draws that over the base at full brightness, so
the blink glows at night the way the backplate borders and pavement markers do. Sprites tick
in step from the same counter, so the two strips cannot drift apart. Without OptiFine the
companion is never referenced and the base strip's own lit frame still blinks, just shaded.

The LED positions are read off each base texture rather than hard-coded: the octagon's
vertices are where its outline meets the edge of the texture, and a rectangle's panel is its
opaque bounding box, so the dots land on the border whatever the artwork's exact margins.
The wide WRONG WAY plate is not square (22 x 16 units for a square texture), so its dots
are drawn taller than they are wide by that ratio to come out round on the block.

Writes:
    modules/roads/.../textures/blocks/trafficsigns/<name>.png         128 x 256, two frames
    modules/roads/.../textures/blocks/trafficsigns/<name>.png.mcmeta   the blink timing
    modules/roads/.../textures/blocks/trafficsigns/<name>_e.png       the emissive companion
    modules/roads/.../textures/blocks/trafficsigns/<name>_e.png.mcmeta the same timing
    modules/roads/.../blockstates/<name>.json                         the base's, retextured

and prints the lang lines and the tab registrations, which are added by hand next to the
plain sign's own (the tab is in creative order, not alphabetical, so a script cannot place
them). Run from the repo root:  python dev-env-utils/scripts/gen_led_signs.py
"""

import math
import os

import numpy as np
from PIL import Image, ImageDraw

ROADS = os.path.join('modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm')
TEXTURES = os.path.join(ROADS, 'textures', 'blocks', 'trafficsigns')
BLOCKSTATES = os.path.join(ROADS, 'blockstates')

SIZE = 128        # every sign face texture is 128 x 128
SS = 4            # supersampling for round dots at this resolution

# One blink per second: a 20-tick cycle with the lit frame holding for 2 ticks (100 ms).
CYCLE_TICKS = 20
LIT_TICKS = 2

# Dot geometry in texture pixels. A 4 px dot on the 24-unit stop plate is about 0.8 units:
# larger than the real thing in proportion, deliberately, because a red point on a red
# panel disappears at any distance otherwise. The lit dot is mostly white-hot core with the
# red carried by the bloom round it, which is how a lens photographs anyway.
DOT_R = 2.1
EDGE_INSET = 5.5          # dot centre this far inside the panel edge: on the white border
DENSE_SPACING = 5.5       # centre-to-centre along the border for the dense ring

LED_DARK = (74, 20, 16, 255)
LED_DARK_EDGE = (40, 12, 10, 255)
LED_LIT = (255, 72, 48, 255)
LED_LIT_CORE = (255, 238, 228, 255)
LED_HALO = (255, 60, 40)

# name, base sign, layout, plate aspect (width / height in block units), display name
CATALOGUE = [
    ('signpoststopsignflashingled', 'signpoststopsign', 'pole_stop',
     'octagon_vertices', 1.0, 'Stop Sign (Flashing LED)'),
    ('signpoststopsignflashingleddense', 'signpoststopsign', 'pole_stop',
     'octagon_dense', 1.0, 'Stop Sign (Flashing LED, Dense)'),
    ('signwrongwayflashingled', 'signwrongway', 'wrong_way',
     'rect_eight', 22.0 / 16.0, 'Wrong Way Sign (Flashing LED)'),
    ('signdonotenterflashingled', 'signdonotenter', 'pole_do_not_enter',
     'rect_eight', 1.0, 'Do Not Enter Sign (Flashing LED)'),
]


def octagon_vertices(mask):
    """The eight corners of the octagon, read off where its outline meets the texture edge."""
    top = np.where(mask[0])[0]
    left = np.where(mask[:, 0])[0]
    bottom = np.where(mask[-1])[0]
    right = np.where(mask[:, -1])[0]
    last = SIZE - 1
    return [(top.min(), 0), (top.max(), 0), (last, right.min()), (last, right.max()),
            (bottom.max(), last), (bottom.min(), last), (0, left.max()), (0, left.min())]


def inset_polygon(points, inset):
    """Shrink a polygon about the texture centre so every edge moves inward by ``inset``."""
    c = (SIZE - 1) / 2.0
    apothem = c  # the octagon's flats touch the texture edge
    s = 1.0 - inset / apothem
    return [(c + (x - c) * s, c + (y - c) * s) for x, y in points]


def along_perimeter(points, spacing):
    """Dots evenly spaced round a closed polygon, an integer count per side so the corners
    each get a dot and the spacing is uniform within a side."""
    out = []
    n = len(points)
    for i in range(n):
        (x0, y0), (x1, y1) = points[i], points[(i + 1) % n]
        length = math.hypot(x1 - x0, y1 - y0)
        steps = max(1, int(round(length / spacing)))
        for k in range(steps):  # the end of one side is the start of the next
            t = k / float(steps)
            out.append((x0 + (x1 - x0) * t, y0 + (y1 - y0) * t))
    return out


def rect_eight(mask, inset):
    """Four corners and four side midpoints of the panel, inset from its opaque bounds."""
    ys, xs = np.where(mask)
    x0, x1 = xs.min() + inset, xs.max() - inset
    y0, y1 = ys.min() + inset, ys.max() - inset
    xm, ym = (x0 + x1) / 2.0, (y0 + y1) / 2.0
    return [(x0, y0), (xm, y0), (x1, y0), (x1, ym), (x1, y1), (xm, y1), (x0, y1), (x0, ym)]


def layout(kind, mask):
    if kind == 'octagon_vertices':
        return inset_polygon(octagon_vertices(mask), EDGE_INSET + 1.0)
    if kind == 'octagon_dense':
        return along_perimeter(inset_polygon(octagon_vertices(mask), EDGE_INSET),
                               DENSE_SPACING)
    if kind == 'rect_eight':
        return rect_eight(mask, EDGE_INSET)
    raise ValueError(kind)


# A pixel the LED layer covers at least this much is part of the emissive overlay. The
# overlay has to be opaque where it exists at all -- the sign is a cutout, so a soft alpha
# edge would be discarded -- so the bloom is included as the fully blended colour, out to
# where it has faded to nearly nothing.
EMISSIVE_MIN_ALPHA = 24


def led_layer(dots, aspect, lit):
    """Just the LEDs, on transparency, supersampled so a 4 px dot comes out round."""
    layer = Image.new('RGBA', (SIZE * SS, SIZE * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)

    def ellipse(cx, cy, r, fill, outline=None):
        rx, ry = r * SS, r * aspect * SS
        d.ellipse([(cx + 0.5) * SS - rx, (cy + 0.5) * SS - ry,
                   (cx + 0.5) * SS + rx, (cy + 0.5) * SS + ry], fill=fill, outline=outline)

    if lit:
        # Halo first, in two soft steps, then the lamp over it.
        for r, alpha in ((DOT_R * 3.0, 70), (DOT_R * 2.0, 150)):
            for cx, cy in dots:
                ellipse(cx, cy, r, LED_HALO + (alpha,))
    for cx, cy in dots:
        if lit:
            ellipse(cx, cy, DOT_R, LED_LIT)
            ellipse(cx, cy, DOT_R * 0.7, LED_LIT_CORE)
        else:
            ellipse(cx, cy, DOT_R, LED_DARK, LED_DARK_EDGE)
    return layer.resize((SIZE, SIZE), Image.LANCZOS)


def draw_dots(base, dots, aspect, lit):
    """The base with the LEDs composited on. The result keeps the base's own alpha: nothing,
    not even the lit halo, is drawn off the panel, because the sign renders as a cutout and a
    solid red blob past the edge would not read as light."""
    out = base.copy()
    out.alpha_composite(led_layer(dots, aspect, lit))
    out.putalpha(base.getchannel('A'))
    return out


def draw_emissive(base, dots, aspect):
    """The lit frame's LEDs alone: the composited result where the LED layer reaches, opaque,
    and transparent everywhere else -- the shape of overlay ``marker_emissive`` makes for the
    pavement markers."""
    lit = draw_dots(base, dots, aspect, True)
    coverage = np.array(led_layer(dots, aspect, True))[:, :, 3]
    keep = (coverage >= EMISSIVE_MIN_ALPHA) & (np.array(base)[:, :, 3] > 127)
    px = np.array(lit)
    px[:, :, 3] = np.where(keep, 255, 0)
    return Image.fromarray(px, 'RGBA')


def write_mcmeta(path):
    """The blink: a 20-tick cycle with the lit frame holding for LIT_TICKS of it."""
    with open(path, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write('{\n  "animation": {\n    "frames": [\n'
                 '      {"index": 0, "time": %d},\n'
                 '      {"index": 1, "time": %d}\n'
                 '    ]\n  }\n}\n' % (CYCLE_TICKS - LIT_TICKS, LIT_TICKS))


def write_blockstate(name, base_name, base_texture):
    """The plain sign's blockstate with every reference to its face texture pointed at the
    LED strip. Done as text so the file keeps its formatting and line endings."""
    src = os.path.join(BLOCKSTATES, base_name + '.json')
    with open(src, 'rb') as fh:
        text = fh.read().decode('utf-8')
    old = '"csm:blocks/trafficsigns/%s"' % base_texture
    new = '"csm:blocks/trafficsigns/%s"' % name
    if old not in text:
        raise SystemExit('%s does not reference %s' % (src, old))
    dst = os.path.join(BLOCKSTATES, name + '.json')
    with open(dst, 'wb') as fh:
        fh.write(text.replace(old, new).encode('utf-8'))
    return dst


def main():
    if not os.path.isdir(TEXTURES):
        raise SystemExit('run from the repo root: %s not found' % TEXTURES)

    lang, tab = [], []
    for name, base_name, base_texture, kind, aspect, display in CATALOGUE:
        base = Image.open(os.path.join(TEXTURES, base_texture + '.png')).convert('RGBA')
        if base.size != (SIZE, SIZE):
            raise SystemExit('%s is %s, expected %dx%d' % (base_texture, base.size, SIZE, SIZE))
        mask = np.array(base)[:, :, 3] > 127
        dots = layout(kind, mask)

        strip = Image.new('RGBA', (SIZE, SIZE * 2), (0, 0, 0, 0))
        strip.paste(draw_dots(base, dots, aspect, False), (0, 0))
        strip.paste(draw_dots(base, dots, aspect, True), (0, SIZE))
        png = os.path.join(TEXTURES, name + '.png')
        strip.save(png)
        write_mcmeta(png + '.mcmeta')

        # The emissive companion on the same clock: nothing while dark, the lit LEDs alone
        # while lit.
        emissive = Image.new('RGBA', (SIZE, SIZE * 2), (0, 0, 0, 0))
        emissive.paste(draw_emissive(base, dots, aspect), (0, SIZE))
        png_e = os.path.join(TEXTURES, name + '_e.png')
        emissive.save(png_e)
        write_mcmeta(png_e + '.mcmeta')

        bs = write_blockstate(name, base_name, base_texture)
        print('wrote %s (%d LEDs) + .mcmeta, %s + .mcmeta, %s'
              % (png, len(dots), png_e, bs))

        lang.append('tile.%s.name=%s' % (name, display))
        tab.append('    initTabBlock(new BlockTrafficSign("%s"));  // after %s'
                   % (name, base_name))

    assert CYCLE_TICKS * 50 == 1000, 'the cycle must be one second'
    print('\nlang (en_us.lang, next to the plain sign):\n' + '\n'.join(lang))
    print('\ntab (CsmTabRoadSigns.initTabElements):\n' + '\n'.join(tab))


if __name__ == '__main__':
    main()
