#!/usr/bin/env python3
"""
gen_preempt_detector.py -- the preempt detector and the confirmation lights (Roads): models and
blockstates.

The detector is drawn after an optical preemption detector: a small black head on a mount stub on
top of a mast arm, its lens looking up the approach under a sun-shield fin. It sits in
the cell ABOVE a thin traffic pole (`trafficpolehorizontal`, 8 px across, so its top is 4 px under
this cell's floor): the stub reaches down to the pole, and into a full block if one is below.

Model space: the lens faces north (-z), the arm runs east-west under it, which is how it stands
on an arm across the approach it watches; the blockstate turns it with the block's facing.
Round parts are exact octagons, four boxes each, two of them turned 45 degrees.

The confirmation lights stand on the same stub: a PAR lamp in a yoke (white and blue), its lens
looking up the approach as the detector's does, and a 360 degree dome beacon (red). Their glow is
the traffic beacon renderer's, which needs each lens box in BlockPreemptConfirmationLight's
subclasses to match LIGHTS below.

Usage:
    python gen_preempt_detector.py [--check]
"""
import json
import math
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, 'modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm')
MODELS = os.path.join(ASSETS, 'models', 'block', 'trafficsignals', 'shared_models')
MODEL = os.path.join(MODELS, 'preempt_detector.json')
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


def octagon_z(a, z0, z1, tex=BODY, cx=8.0, cy=8.0, lens_face=None):
    """An octagonal prism lying along z, apothem `a`, from z0 to z1 (a lamp can)."""
    s = a * TAN
    out = []
    for rot in (None, {'angle': 45, 'axis': 'z', 'origin': [cx, cy, 8]}):
        out.append(box((cx - a, cy - s, z0), (cx + a, cy + s, z1), tex, rot=rot,
                       lens_face=lens_face))
        out.append(box((cx - s, cy - a, z0), (cx + s, cy + a, z1), tex, rot=rot,
                       lens_face=lens_face))
    return out


def stub(top):
    """The mount: a square stub from the top of a thin pole below (4 px under this cell's floor)
    up to `top`, on a small foot. It meets a thin pole and sinks into the top of a full block
    below. (A clamp drawn round the pole never matched its round contour.)"""
    return [box((6.6, -4.0, 6.6), (9.4, -3.5, 9.4), METAL),
            box((7.3, -3.5, 7.3), (8.7, top, 8.7), METAL, faces='nsew')]


def par_elements():
    """A PAR lamp in a yoke on the stub, its lens north: the confirmation light that faces the
    approach."""
    els = stub(-2.2)
    els.append(box((4.6, -2.2, 7.2), (11.4, -1.4, 8.8), METAL))                # yoke base
    els.append(box((4.6, -1.4, 7.2), (5.2, 4.6, 8.8), METAL))                  # yoke arms
    els.append(box((10.8, -1.4, 7.2), (11.4, 4.6, 8.8), METAL))
    els += octagon_z(2.9, 5.2, 10.8, cy=3.8)                                  # the can
    els += octagon_z(3.2, 4.8, 5.2, cy=3.8)                                   # its front ring
    els += octagon_z(2.5, 4.7, 4.8, tex=LENS, cy=3.8, lens_face='n')          # the lens
    return els


def dome_elements():
    """A 360 degree dome beacon on the stub."""
    els = stub(-2.2)
    els += octagon(3.0, -2.2, -0.6)                                          # base
    els += [dict(e, faces={k: dict(v, texture=LENS) for k, v in e['faces'].items()})
            for e in octagon(2.5, -0.6, 4.4, tex=LENS)]                      # the dome
    els += [dict(e, faces={k: dict(v, texture=LENS) for k, v in e['faces'].items()})
            for e in octagon(1.6, 4.4, 5.2, tex=LENS)]                       # its crown
    els += octagon(1.0, 5.2, 5.6)                                            # cap
    return els


# registry name: (elements, lens texture)
LIGHTS = {
    'preempt_confirm_par_white': (par_elements,
                                  'csm:blocks/trafficaccessories/enforcement/flash_lens'),
    'preempt_confirm_par_blue': (par_elements, TEX + 'blue_beacon'),
    'preempt_confirm_dome_red': (dome_elements,
                                 'csm:blocks/trafficaccessories/shared_textures/red_beacon'),
}


def light_model(name):
    build, lens = LIGHTS[name]
    return {
        'parent': 'block/block',
        'textures': {'particle': TEX + 'metal_black', 'body': TEX + 'metal_black',
                     'metal': TEX + 'metal_silver', 'lens': lens},
        'elements': build(),
    }


def light_blockstate(name):
    facing = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270},
              'up': {}, 'down': {}}
    return {
        'forge_marker': 1,
        'defaults': {'model': 'csm:trafficsignals/shared_models/' + name},
        'variants': {'facing': facing, 'powered': {'true': {}, 'false': {}},
                     'inventory': [{'transform': 'forge:default-block'}]},
    }


# The head is drawn at half the size it was first drawn at (it read as far too big on an arm),
# scaled about the point it was first drawn standing on.
HEAD_SCALE = 0.5
PIVOT = (8.0, -2.2, 8.0)


def _s(p):
    return tuple(PIVOT[k] + (p[k] - PIVOT[k]) * HEAD_SCALE for k in range(3))


def _sy(y):
    return PIVOT[1] + (y - PIVOT[1]) * HEAD_SCALE


def elements():
    # the mount stub, up to where the head's knuckle starts
    els = stub(_sy(2.0))
    # the head, drawn full size here and scaled: stem, knuckle, body, collar, the lens housing
    # and its cap
    def b(lo, hi, tex, **kw):
        return box(_s(lo), _s(hi), tex, **kw)

    def o(a, y0, y1):
        return octagon(a * HEAD_SCALE, _sy(y0), _sy(y1))

    els += o(1.4, 2.0, 3.5)
    els += o(2.4, 3.5, 9.5)
    els += o(2.7, 9.5, 10.0)
    els += o(2.5, 10.0, 14.0)
    els += o(2.7, 14.0, 14.6)
    # the lens, in a bezel standing proud of the housing's north face (at z 5.5)
    els.append(b((6.4, 10.6, 5.2), (9.6, 13.4, 5.6), BODY, faces='nsewud'))
    els.append(b((6.7, 10.9, 5.15), (9.3, 13.1, 5.2), BODY, faces='n', lens_face='n'))
    # the cable connector on the body's front
    els.append(b((7.2, 5.0, 5.35), (8.8, 6.6, 5.7), METAL, faces='nsewud'))
    # the sun-shield fin over the lens, reaching out toward the approach (north, the way the
    # lens looks and so toward whoever placed it) and tapering in steps
    els.append(b((7.2, 13.5, 3.5), (8.8, 14.0, 5.6), BODY))
    els.append(b((7.5, 13.5, 2.0), (8.5, 14.0, 3.5), BODY))
    els.append(b((7.8, 13.5, 0.8), (8.2, 14.0, 2.0), BODY))
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
    for name in LIGHTS:
        files[os.path.join(MODELS, name + '.json')] = text(light_model(name))
        files[os.path.join(ASSETS, 'blockstates', name + '.json')] = text(light_blockstate(name))
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
