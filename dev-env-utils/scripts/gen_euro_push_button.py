#!/usr/bin/env python3
"""
gen_euro_push_button.py -- the European push button's model, textures and blockstates (Roads).

A tall blue box on the pole with darker rounded caps, its front carrying (top to bottom) the zebra
chevron, a pointing hand in a circle, the pedestrian or bicycle the button serves and a small
signal mark, and under them the yellow LED request lamp that lights once the button is pressed
(the `requested` actual state; see AbstractBlockTrafficSignalAPSEuro). Two blocks share one model
and differ only in the front texture:

  controllablecrosswalkbuttoneuro       pedestrian
  controllablecrosswalkbuttoneurobike   bicycle

The front is drawn at its true 5.8 x 13.4 aspect, 4x oversampled, then squeezed into a square
texture the model stretches back out (block textures must be square). Lettering is left off:
the symbols are the language.

Usage:
    python gen_euro_push_button.py [--check]
"""
import io
import json
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, 'modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm')
TEX_DIR = os.path.join(ASSETS, 'textures', 'blocks', 'trafficsignals', 'shared_textures')
TEX = 'csm:blocks/trafficsignals/shared_textures/'
MODEL = 'trafficsignals/euro_push_button'

BLOCKS = {
    'controllablecrosswalkbuttoneuro': 'pedestrian',
    'controllablecrosswalkbuttoneurobike': 'bicycle',
}

# --- geometry (model units; the front faces north, the back sits on the block's south face) ---
# Authored at full block height, then scaled by SCALE about the back of the block's bottom centre
# and lifted by LIFT: a real button is about 300 mm, some 60% of a 200 mm pedestrian head, and
# stands at hand height on the pole rather than from the ground up.
SCALE, LIFT = 0.75, 1.5


def _fit(box):
    lo, hi = box
    return ([round(8 + (lo[0] - 8) * SCALE, 4), round(lo[1] * SCALE + LIFT, 4),
             round(16 + (lo[2] - 16) * SCALE, 4)],
            [round(8 + (hi[0] - 8) * SCALE, 4), round(hi[1] * SCALE + LIFT, 4),
             round(16 + (hi[2] - 16) * SCALE, 4)])


BODY = _fit(([5.1, 1.0, 11.6], [10.9, 14.4, 16.0]))
CAP_BOTTOM = _fit(([4.9, 0.0, 11.3], [11.1, 1.0, 16.0]))
CAP_TOP = _fit(([4.9, 14.4, 11.3], [11.1, 15.4, 16.0]))
DOME = _fit(([5.3, 15.4, 11.8], [10.7, 15.8, 16.0]))
LAMP = _fit(([6.0, 1.6, 11.45], [10.0, 4.4, 11.6]))
# The bounding box AbstractBlockTrafficSignalAPSEuro.getBlockBoundingBox returns
BOUNDS = (CAP_BOTTOM[0][0], CAP_BOTTOM[0][1], CAP_BOTTOM[0][2], CAP_BOTTOM[1][0], DOME[1][1], 16)

BLUE = (31, 96, 196)
BLUE_LIGHT = (58, 128, 222)
PANEL = (22, 66, 150)
NAVY = (26, 38, 96)
WHITE = (240, 244, 250)
BACK = (40, 42, 48)

OVERSAMPLE = 4
FRONT_W, FRONT_H = 58, 134          # 10 px a unit across the body's 5.8 x 13.4 face
SIZE = 128


def front_texture(kind):
    """The body's front: blue with a darker panel, the symbols, and the lamp's recess."""
    s = OVERSAMPLE
    img = Image.new('RGBA', (FRONT_W * s, FRONT_H * s), BLUE + (255,))
    d = ImageDraw.Draw(img)
    # brushed sheen: a lighter band down the middle of the face
    for x in range(FRONT_W * s):
        t = 1 - abs(x / (FRONT_W * s) - 0.5) * 2
        c = tuple(int(BLUE[i] + (BLUE_LIGHT[i] - BLUE[i]) * t * 0.6) for i in range(3))
        d.line([(x, 0), (x, FRONT_H * s)], fill=c + (255,))

    def r(x0, y0, x1, y1, **kw):
        d.rectangle([x0 * s, y0 * s, x1 * s, y1 * s], **kw)

    def line(points, width):
        d.line([(x * s, y * s) for x, y in points], fill=WHITE, width=int(width * s),
               joint='curve')

    def disc(cx, cy, rad, **kw):
        d.ellipse([(cx - rad) * s, (cy - rad) * s, (cx + rad) * s, (cy + rad) * s], **kw)

    r(5, 4, 53, 97, fill=PANEL)                       # the symbol panel
    r(7, 99, 51, 131, fill=(18, 20, 26))              # the lamp's recess (the lamp sits on it)

    # zebra chevron: five V stripes, spaced apart, narrowing to a point
    cx = 29
    for k in range(5):
        half = 16 - k * 3.4
        y = 9 + k * 5.0
        line([(cx - half, y), (cx, y + half * 0.45), (cx + half, y)], 1.7)

    # pointing hand in a circle: the index finger up from the left of the palm, the other three
    # curled into knuckles across its top, the thumb folded against the side
    disc(29, 55, 11.5, outline=WHITE, width=int(1.6 * s))
    d.rounded_rectangle([25.2 * s, 45.0 * s, 28.2 * s, 56 * s], radius=int(1.5 * s), fill=WHITE)
    d.rounded_rectangle([25.2 * s, 54.0 * s, 34.6 * s, 63.0 * s], radius=int(2.4 * s),
                        fill=WHITE)
    for kx in (29.6, 31.9, 34.0):                     # the curled fingers' knuckles
        disc(kx, 54.2, 1.25, fill=WHITE)
    for kx in (28.8, 31.0, 33.2):                     # and the gaps between them
        line([(kx, 53.6), (kx, 56.2)], 0.45)
    d.rounded_rectangle([22.6 * s, 56.0 * s, 26.4 * s, 60.0 * s], radius=int(1.6 * s),
                        fill=WHITE)                   # thumb

    if kind == 'pedestrian':
        disc(30.5, 71.5, 2.4, fill=WHITE)             # head
        line([(29.5, 75), (28, 82.5)], 2.4)           # body
        line([(29, 77), (25, 80.5)], 1.6)             # arms
        line([(29.5, 77), (33.5, 79.5)], 1.6)
        line([(28, 82.5), (24.5, 89)], 1.8)           # legs
        line([(28, 82.5), (32.5, 88.5)], 1.8)
    else:
        for wx in (21.5, 36.5):                       # wheels
            disc(wx, 84, 4.6, outline=WHITE, width=int(1.3 * s))
        line([(21.5, 84), (27.5, 84), (33, 78), (36.5, 84)], 1.3)   # frame
        line([(27.5, 84), (25.5, 78), (33, 78)], 1.3)
        disc(31.5, 70.5, 2.2, fill=WHITE)             # rider
        line([(30.5, 73.5), (26.5, 78.5)], 2.0)
        line([(30.5, 74.5), (33.5, 77.5)], 1.3)

    # the small signal mark over the lamp: three short chevrons
    for k in range(3):
        half = 4 - k * 1.2
        y = 91 + k * 1.6
        line([(13 - half, y), (13, y + half * 0.6), (13 + half, y)], 0.8)

    return img.resize((SIZE, SIZE), Image.LANCZOS)


def side_texture():
    """The ribbed blue sides."""
    img = Image.new('RGBA', (32, 32), BLUE + (255,))
    d = ImageDraw.Draw(img)
    for x in range(0, 32, 4):
        d.line([(x, 0), (x, 31)], fill=(20, 70, 160, 255))
        d.line([(x + 1, 0), (x + 1, 31)], fill=BLUE_LIGHT + (255,))
    return img


def flat_texture(colour, edge=None):
    img = Image.new('RGBA', (16, 16), colour + (255,))
    if edge:
        ImageDraw.Draw(img).rectangle([0, 0, 15, 15], outline=edge + (255,))
    return img


def lamp_texture(lit):
    """The LED request lamp: a grid of amber LEDs, dark or lit."""
    img = Image.new('RGBA', (64, 64), ((236, 168, 24) if lit else (34, 28, 20)) + (255,))
    d = ImageDraw.Draw(img)
    for row in range(7):
        for col in range(9):
            x, y = 4 + col * 7, 5 + row * 9
            if lit:
                d.ellipse([x - 1, y - 1, x + 5, y + 5], fill=(255, 214, 70, 255))
                d.ellipse([x, y, x + 4, y + 4], fill=(255, 250, 200, 255))
            else:
                d.ellipse([x, y, x + 4, y + 4], fill=(92, 70, 30, 255))
    return img


def model():
    def faces(tex, north=None):
        f = {side: {'texture': tex, 'uv': [0, 0, 16, 16]}
             for side in ('north', 'south', 'east', 'west', 'up', 'down')}
        if north:
            f['north'] = {'texture': north, 'uv': [0, 0, 16, 16]}
        return f

    body = faces('#side', '#front')
    body['south'] = {'texture': '#back', 'uv': [0, 0, 16, 16]}
    body['up'] = body['down'] = {'texture': '#cap', 'uv': [0, 0, 16, 16]}
    elements = [
        {'name': 'body', 'from': BODY[0], 'to': BODY[1], 'faces': body},
        {'name': 'cap_bottom', 'from': CAP_BOTTOM[0], 'to': CAP_BOTTOM[1], 'faces': faces('#cap')},
        {'name': 'cap_top', 'from': CAP_TOP[0], 'to': CAP_TOP[1], 'faces': faces('#cap')},
        {'name': 'dome', 'from': DOME[0], 'to': DOME[1], 'faces': faces('#cap')},
        {'name': 'lamp', 'from': LAMP[0], 'to': LAMP[1], 'faces': faces('#cap', '#lamp')},
    ]
    for e in elements:
        if e['name'] != 'body':          # against the pole, behind the body: never seen
            e['faces'].pop('south', None)
    return {
        'textures': {'particle': TEX + 'euro_button_side'},
        'elements': elements,
        'display': {
            'gui': {'rotation': [0, 180, 0], 'translation': [0, 0, -2], 'scale': [1.1, 1.1, 1.1]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 180, 0], 'scale': [1, 1, 1]},
            'thirdperson_righthand': {'rotation': [75, 225, 0], 'translation': [0, 2.5, 0],
                                      'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]},
        },
    }


def blockstate(kind):
    lamp_off, lamp_on = TEX + 'euro_button_lamp_off', TEX + 'euro_button_lamp_on'
    return {
        'forge_marker': 1,
        'defaults': {
            'model': 'csm:' + MODEL,
            'textures': {
                'front': TEX + 'euro_button_front_' + kind,
                'side': TEX + 'euro_button_side',
                'cap': TEX + 'euro_button_cap',
                'back': TEX + 'euro_button_back',
                'lamp': lamp_off,
                'particle': TEX + 'euro_button_side',
            },
        },
        'variants': {
            'inventory': [{'textures': {'lamp': lamp_on}}],
            'facing': {'north': {'y': 0}, 'south': {'y': 180}, 'west': {'y': 270},
                       'east': {'y': 90}},
            'color': {str(c): {} for c in range(4)},
            'arrowdir': {str(a): {} for a in range(4)},
            'requested': {'false': {}, 'true': {'textures': {'lamp': lamp_on}}},
        },
    }


def outputs():
    out = {}

    def png(name, img):
        buf = io.BytesIO()
        img.save(buf, 'PNG', optimize=True)
        out[os.path.join(TEX_DIR, name + '.png')] = buf.getvalue()

    for kind in sorted(set(BLOCKS.values())):
        png('euro_button_front_' + kind, front_texture(kind))
    png('euro_button_side', side_texture())
    png('euro_button_cap', flat_texture(NAVY, (20, 28, 72)))
    png('euro_button_back', flat_texture(BACK))
    png('euro_button_lamp_off', lamp_texture(False))
    png('euro_button_lamp_on', lamp_texture(True))

    def js(path, data):
        out[path] = (json.dumps(data, indent=2) + '\n').encode('utf-8')

    js(os.path.join(ASSETS, 'models', 'block', MODEL + '.json'), model())
    for reg, kind in BLOCKS.items():
        js(os.path.join(ASSETS, 'blockstates', reg + '.json'), blockstate(kind))
    return out


def main():
    print('bounding box (x0, y0, z0, x1, y1, z1 in model units):', BOUNDS)
    check = '--check' in sys.argv
    drift = []
    for path, data in outputs().items():
        old = open(path, 'rb').read() if os.path.exists(path) else None
        if path.endswith('.json') and old is not None:
            # git may check text out with CRLF (core.autocrlf): compare without it, and keep the
            # file's own line endings when rewriting it
            if old.replace(b'\r\n', b'\n') == data:
                continue
            if b'\r\n' in old:
                data = data.replace(b'\n', b'\r\n')
        if old == data:
            continue
        if check:
            drift.append(path)
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            open(path, 'wb').write(data)
            print('wrote', os.path.relpath(path, ROOT))
    if check and drift:
        print('out of date:\n  ' + '\n  '.join(os.path.relpath(p, ROOT) for p in drift))
        sys.exit(1)


if __name__ == '__main__':
    main()
