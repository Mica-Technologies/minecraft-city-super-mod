#!/usr/bin/env python3
"""
gen_pattern_select_switch.py -- the pattern select switch's models, textures and blockstate (Roads).

A small grey cabinet switch box on a wall: a toggle that rests down on a clock mark (the
controller's own choice of coordination pattern) and is thrown up to a flag mark (the selected
pattern, an event plan say), with a pilot lamp over it that lights amber while the switch is on.
See BlockTrafficSignalPatternSwitch and ADVANCED_MODE_ASC3.md, "Pattern select".

Two models, off and on: the toggle moves, so the on state is geometry and not a retexture. The
blockstate's `powered` property (the redstone edge memory) draws nothing.

The marks are drawn, not lettered, so the output does not depend on which fonts are installed.

Usage:
    python gen_pattern_select_switch.py [--check]
"""
import io
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import model_depth  # noqa: E402

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, 'modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm')
TEX_DIR = os.path.join(ASSETS, 'textures', 'blocks', 'trafficsignals', 'shared_textures')
TEX = 'csm:blocks/trafficsignals/shared_textures/'
MODEL = 'trafficsignals/shared_models/pattern_select_switch'
REG = 'traffic_signal_pattern_switch'

# --- geometry (model units; the front faces north, the back sits on the block's south face) ---
BODY = ([4.5, 3.0, 13.5], [11.5, 13.0, 16.0])
COLLAR = ([7.0, 6.4, 13.2], [9.0, 8.6, 13.5])          # the toggle's bezel
HANDLE_OFF = ([7.55, 4.9, 12.5], [8.45, 7.5, 13.2])    # down, on the clock
HANDLE_ON = ([7.55, 7.5, 12.5], [8.45, 10.1, 13.2])    # up, on the flag
LAMP = ([7.2, 10.9, 13.2], [8.8, 12.3, 13.5])
# The bounding box BlockTrafficSignalPatternSwitch returns, facing north
BOUNDS = (BODY[0][0], BODY[0][1], 12.5, BODY[1][0], BODY[1][1], 16.0)

GREY = (150, 154, 158)
GREY_LIGHT = (178, 182, 186)
GREY_DARK = (96, 100, 106)
PANEL = (44, 46, 52)
WHITE = (236, 238, 240)
AMBER = (255, 176, 32)

OVERSAMPLE = 4
SIZE = 64


def front_texture():
    """The box's front: a brushed grey door, a dark legend plate, the clock and the flag."""
    s = OVERSAMPLE
    n = SIZE * s
    img = Image.new('RGBA', (n, n), GREY + (255,))
    d = ImageDraw.Draw(img)
    for x in range(n):                                   # brushed sheen across the door
        t = 1 - abs(x / n - 0.42) * 1.6
        t = max(0.0, min(1.0, t))
        c = tuple(int(GREY[i] + (GREY_LIGHT[i] - GREY[i]) * t) for i in range(3))
        d.line([(x, 0), (x, n)], fill=c + (255,))

    def r(x0, y0, x1, y1, **kw):
        d.rectangle([x0 * s, y0 * s, x1 * s, y1 * s], **kw)

    def disc(cx, cy, rad, **kw):
        d.ellipse([(cx - rad) * s, (cy - rad) * s, (cx + rad) * s, (cy + rad) * s], **kw)

    def line(points, width, fill=WHITE):
        d.line([(x * s, y * s) for x, y in points], fill=fill, width=int(width * s))

    r(0, 0, 63, 63, outline=GREY_DARK + (255,), width=2 * s)       # the door's edge
    r(8, 4, 55, 59, fill=PANEL + (255,))                           # the legend plate
    for sx, sy in ((4, 4), (59, 4), (4, 59), (59, 59)):            # corner screws
        disc(sx, sy, 1.6, fill=GREY_DARK + (255,))

    # up: a flag on a staff (the selected pattern). The face is 7 x 10, so one texture pixel is
    # wider than it is tall on the model; the marks are drawn narrow to come out right.
    line([(13, 18), (13, 31)], 1.6)
    d.polygon([(14 * s, 18 * s), (22 * s, 21 * s), (14 * s, 24 * s)], fill=AMBER + (255,))
    # down: a clock (the controller's own choice)
    disc(15.5, 46, 5.5, outline=WHITE + (255,), width=int(1.5 * s))
    line([(15.5, 46), (15.5, 42.5)], 1.2)
    line([(15.5, 46), (18.5, 47.5)], 1.2)
    # the two throws, as tick marks beside the toggle
    line([(40, 24.5), (46, 24.5)], 1.4, AMBER)
    line([(40, 46), (46, 46)], 1.4)
    return img.resize((SIZE, SIZE), Image.LANCZOS)


def side_texture():
    """The box's sides, top and back: plain grey with a darker edge."""
    img = Image.new('RGBA', (16, 16), GREY + (255,))
    ImageDraw.Draw(img).rectangle([0, 0, 15, 15], outline=GREY_DARK + (255,))
    return img


def flat_texture(colour, edge=None):
    img = Image.new('RGBA', (16, 16), colour + (255,))
    if edge:
        ImageDraw.Draw(img).rectangle([0, 0, 15, 15], outline=edge + (255,))
    return img


def lamp_texture(lit):
    img = Image.new('RGBA', (16, 16), ((70, 48, 14) if not lit else AMBER) + (255,))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(30, 30, 34, 255))
    d.ellipse([4, 4, 11, 11], fill=((255, 232, 150) if lit else (104, 74, 24)) + (255,))
    return img


def model(on):
    def faces(tex, north=None, skip=()):
        f = {side: {'texture': tex, 'uv': [0, 0, 16, 16]}
             for side in ('north', 'south', 'east', 'west', 'up', 'down') if side not in skip}
        if north:
            f['north'] = {'texture': north, 'uv': [0, 0, 16, 16]}
        return f

    handle = HANDLE_ON if on else HANDLE_OFF
    return {
        'textures': {
            'particle': TEX + 'pattern_switch_side',
            'front': TEX + 'pattern_switch_front',
            'side': TEX + 'pattern_switch_side',
            'collar': TEX + 'pattern_switch_collar',
            'handle': TEX + 'pattern_switch_handle',
            'lamp': TEX + ('pattern_switch_lamp_on' if on else 'pattern_switch_lamp_off'),
        },
        'elements': [
            {'name': 'body', 'from': BODY[0], 'to': BODY[1], 'faces': faces('#side', '#front')},
            {'name': 'collar', 'from': COLLAR[0], 'to': COLLAR[1],
             'faces': faces('#collar', skip=('south',))},
            {'name': 'handle', 'from': handle[0], 'to': handle[1],
             'faces': faces('#handle', skip=('south',))},
            {'name': 'lamp', 'from': LAMP[0], 'to': LAMP[1],
             'faces': faces('#collar', '#lamp', skip=('south',))},
        ],
        'display': {
            'gui': {'rotation': [20, 200, 0], 'translation': [0, 0, 0], 'scale': [1.5, 1.5, 1.5]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, -6], 'scale': [1.4, 1.4, 1.4]},
            'thirdperson_righthand': {'rotation': [75, 225, 0], 'translation': [0, 2.5, 0],
                                      'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 4],
                                      'scale': [0.5, 0.5, 0.5]},
        },
    }


def blockstate():
    return {
        'forge_marker': 1,
        'defaults': {'model': 'csm:' + MODEL + '_off'},
        'variants': {
            'facing': {'north': {'y': 0}, 'east': {'y': 90}, 'south': {'y': 180},
                       'west': {'y': 270}},
            'on': {'false': {}, 'true': {'model': 'csm:' + MODEL + '_on'}},
            'powered': {'false': {}, 'true': {}},
            'inventory': [{}],
        },
    }


def outputs():
    out = {}

    def png(name, img):
        buf = io.BytesIO()
        img.save(buf, 'PNG', optimize=True)
        out[os.path.join(TEX_DIR, name + '.png')] = buf.getvalue()

    png('pattern_switch_front', front_texture())
    png('pattern_switch_side', side_texture())
    png('pattern_switch_collar', flat_texture((34, 36, 40), (20, 20, 24)))
    png('pattern_switch_handle', flat_texture((206, 210, 214), (150, 154, 160)))
    png('pattern_switch_lamp_off', lamp_texture(False))
    png('pattern_switch_lamp_on', lamp_texture(True))

    def js(path, data):
        out[path] = (json.dumps(data, indent=2) + '\n').encode('utf-8')

    js(os.path.join(ASSETS, 'models', 'block', MODEL + '_off.json'), model(False))
    js(os.path.join(ASSETS, 'models', 'block', MODEL + '_on.json'), model(True))
    js(os.path.join(ASSETS, 'blockstates', REG + '.json'), blockstate())
    return out


def main():
    print('bounding box (x0, y0, z0, x1, y1, z1 in model units):', BOUNDS)
    check = '--check' in sys.argv
    out = outputs()
    # separate coplanar faces and drop hidden ones, as every JSON-model generator does
    model_depth.separate_outputs(out, ASSETS, lambda data: (json.dumps(data, indent=2) + '\n')
                                 .encode('utf-8'))
    drift = []
    for path, data in out.items():
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
