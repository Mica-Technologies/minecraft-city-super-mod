#!/usr/bin/env python3
"""
gen_preempt_detector.py -- the preempt detector's model and blockstate (Roads).

The detector is drawn after an optical preemption detector: a small black head on a clamp that
straddles the top of a mast arm, its lens looking up the approach and a sun-shield fin off one
side. It sits in the cell ABOVE a thin traffic pole (`trafficpolehorizontal`, 8 px across, so its
top is 4 px under this cell's floor): the clamp reaches down round the pole, and the head stands
on it.

Model space: the lens faces north (-z), the arm runs east-west under it, which is how it stands
on an arm across the approach it watches; the blockstate turns it with the block's facing.
Round parts are exact octagons, four boxes each, two of them turned 45 degrees.

Usage:
    python gen_preempt_detector.py [--check]
"""
import json
import math
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, 'modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm')
MODEL = os.path.join(ASSETS, 'models', 'block', 'trafficsignals', 'shared_models',
                     'preempt_detector.json')
BLOCKSTATE = os.path.join(ASSETS, 'blockstates', 'preempt_detector.json')

TEX = 'csm:blocks/trafficsignals/shared_textures/'
BODY, METAL, LENS = '#body', '#metal', '#lens'
TAN = math.tan(math.radians(22.5))


def r(v):
    return round(v, 4)


def box(lo, hi, tex, faces='nsewud', rot=None, lens_face=None):
    """A box, every face showing the part of its texture its own size (clipped to the sheet)."""
    (x0, y0, z0), (x1, y1, z1) = lo, hi

    def uv(a0, b0, a1, b1):
        a0, a1 = sorted((min(max(a0, 0), 16), min(max(a1, 0), 16)))
        b0, b1 = sorted((min(max(b0, 0), 16), min(max(b1, 0), 16)))
        if a1 - a0 < 0.01:
            a1 = a0 + 0.5 if a0 < 15.5 else a0 - 0.5
        if b1 - b0 < 0.01:
            b1 = b0 + 0.5 if b0 < 15.5 else b0 - 0.5
        return [r(a0), r(b0), r(a1), r(b1)]

    names = {'n': 'north', 's': 'south', 'e': 'east', 'w': 'west', 'u': 'up', 'd': 'down'}
    uvs = {'n': uv(16 - x1, 16 - y1, 16 - x0, 16 - y0), 's': uv(x0, 16 - y1, x1, 16 - y0),
           'e': uv(16 - z1, 16 - y1, 16 - z0, 16 - y0), 'w': uv(z0, 16 - y1, z1, 16 - y0),
           'u': uv(x0, z0, x1, z1), 'd': uv(x0, 16 - z1, x1, 16 - z0)}
    el = {'from': [r(x0), r(y0), r(z0)], 'to': [r(x1), r(y1), r(z1)], 'faces': {}}
    for f in faces:
        face = {'uv': uvs[f], 'texture': tex}
        if f == lens_face:
            face = {'uv': [0, 0, 16, 16], 'texture': LENS}
        el['faces'][names[f]] = face
    if rot:
        el['rotation'] = rot
    return el


def octagon(a, y0, y1, tex=BODY, cx=8.0, cz=8.0):
    """An octagonal prism, apothem `a`, from y0 to y1: two crossed boxes and the same pair turned
    45 degrees, which together make the exact octagon."""
    s = a * TAN
    out = []
    for rot in (None, {'angle': 45, 'axis': 'y', 'origin': [cx, 8, cz]}):
        out.append(box((cx - a, y0, cz - s), (cx + a, y1, cz + s), tex, rot=rot))
        out.append(box((cx - s, y0, cz - a), (cx + s, y1, cz + a), tex, rot=rot))
    return out


def elements():
    els = []
    # the clamp: a saddle on top of the arm, cheeks down its sides and a strap under it, then
    # the mounting plate and stem the head stands on
    els.append(box((6.5, -4.0, 3.5), (9.5, -3.0, 12.5), METAL))
    els.append(box((6.5, -8.0, 3.5), (9.5, -4.0, 4.0), METAL))
    els.append(box((6.5, -8.0, 12.0), (9.5, -4.0, 12.5), METAL))
    els.append(box((7.5, -12.5, 3.5), (8.5, -12.0, 12.5), METAL))
    els.append(box((7.5, -12.0, 3.5), (8.5, -8.0, 3.9), METAL, faces='nsewd'))
    els.append(box((7.5, -12.0, 12.1), (8.5, -8.0, 12.5), METAL, faces='nsewd'))
    els.append(box((6.0, -3.0, 6.0), (10.0, -2.2, 10.0), METAL))
    els.append(box((7.25, -2.2, 7.25), (8.75, 2.0, 8.75), METAL, faces='nsew'))
    # the head: knuckle, body, collar, the lens housing and its cap
    els += octagon(1.4, 2.0, 3.5)
    els += octagon(2.4, 3.5, 9.5)
    els += octagon(2.7, 9.5, 10.0)
    els += octagon(2.5, 10.0, 14.0)
    els += octagon(2.7, 14.0, 14.6)
    # the lens, in a bezel standing proud of the housing's north face (at z 5.5)
    els.append(box((6.4, 10.6, 5.2), (9.6, 13.4, 5.6), BODY, faces='nsewud'))
    els.append(box((6.7, 10.9, 5.15), (9.3, 13.1, 5.2), BODY, faces='n', lens_face='n'))
    # the cable connector on the body's front
    els.append(box((7.2, 5.0, 5.35), (8.8, 6.6, 5.7), METAL, faces='nsewud'))
    # the sun-shield fin off the east side, tapering in steps
    els.append(box((10.4, 11.3, 7.2), (12.5, 11.8, 8.8), BODY))
    els.append(box((12.5, 11.3, 7.5), (14.0, 11.8, 8.5), BODY))
    els.append(box((14.0, 11.3, 7.8), (15.2, 11.8, 8.2), BODY))
    return els


def model():
    return {
        'parent': 'block/block',
        'textures': {'particle': TEX + 'metal_black', 'body': TEX + 'metal_black',
                     'metal': TEX + 'metal_silver', 'lens': TEX + 'camera_lens'},
        'elements': elements(),
    }


def blockstate():
    return {
        'forge_marker': 1,
        'defaults': {'model': 'csm:trafficsignals/shared_models/preempt_detector'},
        'variants': {
            'facing': {'north': {}, 'east': {'y': 90}, 'south': {'y': 180},
                       'west': {'y': 270}},
            'inventory': [{'transform': 'forge:default-block'}],
        },
    }


def text(obj):
    return json.dumps(obj, indent=2) + '\n'


def same(path, data):
    if not os.path.exists(path):
        return False
    with open(path, encoding='utf-8') as fh:
        return fh.read().replace('\r\n', '\n') == data


def main():
    check = '--check' in sys.argv
    files = {MODEL: text(model()), BLOCKSTATE: text(blockstate())}
    drift = [p for p, d in files.items() if not same(p, d)]
    if check:
        if drift:
            print('preempt detector has drifted:\n  ' + '\n  '.join(drift))
            sys.exit(1)
        print('preempt detector is up to date')
        return
    for p in drift:
        with open(p, 'w', encoding='utf-8', newline='\n') as fh:
            fh.write(files[p])
        print('wrote', os.path.relpath(p, ROOT))


if __name__ == '__main__':
    main()
