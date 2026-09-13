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
    modules/roads/.../textures/blocks/trafficsigns/<name>.png         two frames, one above the other
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

SIZE = 128        # the face texture's side; set per sign by configure(), plaques and the paddle are 256
SS = 4            # supersampling for round dots at this resolution

# One blink per second: a 20-tick cycle with the lit frame holding for 2 ticks (100 ms).
CYCLE_TICKS = 20
LIT_TICKS = 2

# Dot geometry in texture pixels. A 4 px dot on the 24-unit stop plate is about 0.8 units:
# larger than the real thing in proportion, deliberately, because a red point on a red
# panel disappears at any distance otherwise. The lit dot is mostly white-hot core with the
# red carried by the bloom round it, which is how a lens photographs anyway.
BASE_SIZE = 128
BASE_DOT_R = 2.1
BASE_EDGE_INSET = 5.5     # dot centre this far inside the panel edge: on the white border
BASE_DENSE_SPACING = 5.5  # centre-to-centre along the border for the dense ring
DOT_R = BASE_DOT_R
EDGE_INSET = BASE_EDGE_INSET
DENSE_SPACING = BASE_DENSE_SPACING


def configure(size):
    """Scale the dot geometry to a face texture of ``size`` px a side, so a 256 px base gets
    dots the same size in the world as a 128 px one."""
    global SIZE, DOT_R, EDGE_INSET, DENSE_SPACING
    k = size / float(BASE_SIZE)
    SIZE, DOT_R = size, BASE_DOT_R * k
    EDGE_INSET, DENSE_SPACING = BASE_EDGE_INSET * k, BASE_DENSE_SPACING * k

# LED colours: (dark, dark edge, lit, lit core, halo). Regulatory signs carry red LEDs; the
# warning diamonds carry amber, matching the sign's own colour family.
RED_LEDS = ((74, 20, 16, 255), (40, 12, 10, 255), (255, 72, 48, 255), (255, 238, 228, 255),
            (255, 60, 40))
AMBER_LEDS = ((70, 46, 10, 255), (38, 24, 6, 255), (255, 186, 40, 255), (255, 244, 205, 255),
              (255, 176, 30))

# name, base sign, layout, plate aspect (width / height in block units), LED colours, display
CATALOGUE = [
    ('signpoststopsignflashingled', 'signpoststopsign', 'pole_stop',
     'octagon_vertices', 1.0, RED_LEDS, 'Stop Sign (Flashing LED)'),
    ('signpoststopsignflashingleddense', 'signpoststopsign', 'pole_stop',
     'octagon_dense', 1.0, RED_LEDS, 'Stop Sign (Flashing LED, Dense)'),
    ('signwrongwayflashingled', 'signwrongway', 'wrong_way',
     'rect_eight', 22.0 / 16.0, RED_LEDS, 'Wrong Way Sign (Flashing LED)'),
    ('signdonotenterflashingled', 'signdonotenter', 'pole_do_not_enter',
     'rect_eight', 1.0, RED_LEDS, 'Do Not Enter Sign (Flashing LED)'),
    ('signpedestrianflashingled', 'signpedestrian', 'pole_pedestrian',
     'diamond_eight', 1.0, AMBER_LEDS, 'Pedestrian Sign (Flashing LED)'),
    # The W16-7P arrow plaques that hang under the pedestrian diamond, on a 16 x 8 plate
    ('signarrowplaquefloyellowdownleftflashingled', 'signarrowplaquefloyellowdownleft',
     'arrow_sign_plaque_down_left_fluorescent_yellow', 'rect_eight', 2.0, AMBER_LEDS,
     'Arrow Sign (Plaque) (Down Left) (Fluorescent Yellow, Flashing LED)'),
    ('signarrowplaquefloyellowdownrightflashingled', 'signarrowplaquefloyellowdownright',
     'arrow_sign_plaque_down_right_fluorescent_yellow', 'rect_eight', 2.0, AMBER_LEDS,
     'Arrow Sign (Plaque) (Down Right) (Fluorescent Yellow, Flashing LED)'),
    # The pedestrian companions from gen_gap_signs.py: the school pentagon, the in-street
    # paddle and the yield-here sign, all amber as the real LED units are
    ('signschoolcrossingflashingled', 'signschoolcrossing', 'signschoolcrossing',
     'pentagon_ten', 1.0, AMBER_LEDS, 'School Crossing Sign (Flashing LED)'),
    ('signstatelawstopforpedsflashingled', 'signstatelawstopforpeds', 'signstatelawstopforpeds',
     'rect_eight', 8.0 / 24.0, AMBER_LEDS,
     'State Law Stop For Pedestrians In Crosswalk Sign (Flashing LED)'),
    ('signyieldheretopedsflashingled', 'signyieldheretopeds', 'signyieldheretopeds',
     'rect_eight', 1.0, AMBER_LEDS, 'Yield Here To Pedestrians Sign (Flashing LED)'),
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


def diamond_vertices(mask):
    """The four points of a warning diamond: the middle of its outline's first and last opaque
    rows and columns. Read off the opaque extent rather than the texture edge, because a
    diamond with rounded tips (the official drawings) does not reach the edge with any
    opaque pixel."""
    rows = np.where(mask.any(axis=1))[0]
    cols = np.where(mask.any(axis=0))[0]
    top_y, bottom_y, left_x, right_x = rows.min(), rows.max(), cols.min(), cols.max()
    top = np.where(mask[top_y])[0]
    right = np.where(mask[:, right_x])[0]
    bottom = np.where(mask[bottom_y])[0]
    left = np.where(mask[:, left_x])[0]
    return [((top.min() + top.max()) / 2.0, top_y), (right_x, (right.min() + right.max()) / 2.0),
            ((bottom.min() + bottom.max()) / 2.0, bottom_y), (left_x, (left.min() + left.max()) / 2.0)]


def inset_polygon(points, inset, apothem=None):
    """Shrink a regular polygon about the texture centre so every edge moves inward by
    ``inset``. The apothem is the centre-to-edge distance: half the texture for a shape whose
    flats touch the edge (the octagon), and that over root two for the diamond, whose edges run
    corner to corner."""
    c = (SIZE - 1) / 2.0
    if apothem is None:
        apothem = c
    s = 1.0 - inset / apothem
    return [(c + (x - c) * s, c + (y - c) * s) for x, y in points]


def with_edge_midpoints(points):
    """The polygon's vertices with the midpoint of each edge slotted in after it."""
    out = []
    for i, (x0, y0) in enumerate(points):
        x1, y1 = points[(i + 1) % len(points)]
        out.append((x0, y0))
        out.append(((x0 + x1) / 2.0, (y0 + y1) / 2.0))
    return out


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


def rect_eight(mask, inset, aspect):
    """Four corners and four side midpoints of the panel, inset from its opaque bounds. A plate
    wider than it is tall squashes the texture's rows, so the vertical inset is scaled up by the
    aspect to come out the same distance from the edge in the world as the horizontal one."""
    ys, xs = np.where(mask)
    x0, x1 = xs.min() + inset, xs.max() - inset
    y0, y1 = ys.min() + inset * aspect, ys.max() - inset * aspect
    xm, ym = (x0 + x1) / 2.0, (y0 + y1) / 2.0
    return [(x0, y0), (xm, y0), (x1, y0), (x1, ym), (x1, y1), (xm, y1), (x0, y1), (x0, ym)]


def layout(kind, mask, aspect):
    if kind == 'octagon_vertices':
        return inset_polygon(octagon_vertices(mask), EDGE_INSET + 1.0)
    if kind == 'octagon_dense':
        return along_perimeter(inset_polygon(octagon_vertices(mask), EDGE_INSET),
                               DENSE_SPACING)
    if kind == 'rect_eight':
        return rect_eight(mask, EDGE_INSET, aspect)
    if kind == 'diamond_eight':
        # A vertex inset from both of its edges lands further from the tip than an edge
        # midpoint does from its edge, which is how the real ones sit.
        apothem = (SIZE - 1) / 2.0 / math.sqrt(2)
        return with_edge_midpoints(inset_polygon(diamond_vertices(mask), EDGE_INSET + 0.5,
                                                 apothem))
    if kind == 'pentagon_ten':
        return with_edge_midpoints(inset_pentagon(pentagon_vertices(mask), EDGE_INSET + 0.5))
    raise ValueError(kind)


def pentagon_vertices(mask):
    """The school sign's five points: the apex on the top edge, the two shoulders where the
    sides reach the texture edge, and the bottom corners."""
    # The pentagon is drawn inset from the texture edge, so read its outline off the first
    # and last opaque rows and columns rather than the texture's own edges.
    rows = np.where(mask.any(axis=1))[0]
    cols = np.where(mask.any(axis=0))[0]
    top_y, bottom_y = rows.min(), rows.max()
    left_x, right_x = cols.min(), cols.max()
    top = np.where(mask[top_y])[0]
    bottom = np.where(mask[bottom_y])[0]
    left = np.where(mask[:, left_x])[0]
    right = np.where(mask[:, right_x])[0]
    return [((top.min() + top.max()) / 2.0, top_y), (right_x, right.min()),
            (bottom.max(), bottom_y), (bottom.min(), bottom_y), (left_x, left.min())]


def inset_pentagon(points, inset):
    """Move each vertex toward the shape's centroid so its edges come in by about ``inset``.
    The pentagon is not regular, so this uses the mean centre-to-edge distance."""
    cx = sum(x for x, _ in points) / len(points)
    cy = sum(y for _, y in points) / len(points)
    apothem = sum(math.hypot(x - cx, y - cy) for x, y in points) / len(points) * 0.85
    s = 1.0 - inset / apothem
    return [(cx + (x - cx) * s, cy + (y - cy) * s) for x, y in points]


# A pixel the LED layer covers at least this much is part of the emissive overlay. The
# overlay has to be opaque where it exists at all -- the sign is a cutout, so a soft alpha
# edge would be discarded -- so the bloom is included as the fully blended colour, out to
# where it has faded to nearly nothing.
EMISSIVE_MIN_ALPHA = 24


def led_layer(dots, aspect, colours, lit):
    """Just the LEDs, on transparency, supersampled so a 4 px dot comes out round."""
    dark, dark_edge, lit_colour, lit_core, halo = colours
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
                ellipse(cx, cy, r, halo + (alpha,))
    for cx, cy in dots:
        if lit:
            ellipse(cx, cy, DOT_R, lit_colour)
            ellipse(cx, cy, DOT_R * 0.7, lit_core)
        else:
            ellipse(cx, cy, DOT_R, dark, dark_edge)
    return layer.resize((SIZE, SIZE), Image.LANCZOS)


def draw_dots(base, dots, aspect, colours, lit):
    """The base with the LEDs composited on. The result keeps the base's own alpha: nothing,
    not even the lit halo, is drawn off the panel, because the sign renders as a cutout and a
    solid blob past the edge would not read as light."""
    out = base.copy()
    out.alpha_composite(led_layer(dots, aspect, colours, lit))
    out.putalpha(base.getchannel('A'))
    return out


def draw_emissive(base, dots, aspect, colours):
    """The lit frame's LEDs alone: the composited result where the LED layer reaches, opaque,
    and transparent everywhere else -- the shape of overlay ``marker_emissive`` makes for the
    pavement markers."""
    lit = draw_dots(base, dots, aspect, colours, True)
    coverage = np.array(led_layer(dots, aspect, colours, True))[:, :, 3]
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
    for name, base_name, base_texture, kind, aspect, colours, display in CATALOGUE:
        base = Image.open(os.path.join(TEXTURES, base_texture + '.png')).convert('RGBA')
        if base.size[0] != base.size[1]:
            raise SystemExit('%s is %s, expected square' % (base_texture, base.size))
        configure(base.size[0])
        mask = np.array(base)[:, :, 3] > 127
        dots = layout(kind, mask, aspect)

        strip = Image.new('RGBA', (SIZE, SIZE * 2), (0, 0, 0, 0))
        strip.paste(draw_dots(base, dots, aspect, colours, False), (0, 0))
        strip.paste(draw_dots(base, dots, aspect, colours, True), (0, SIZE))
        png = os.path.join(TEXTURES, name + '.png')
        strip.save(png)
        write_mcmeta(png + '.mcmeta')

        # The emissive companion on the same clock: nothing while dark, the lit LEDs alone
        # while lit.
        emissive = Image.new('RGBA', (SIZE, SIZE * 2), (0, 0, 0, 0))
        emissive.paste(draw_emissive(base, dots, aspect, colours), (0, SIZE))
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
