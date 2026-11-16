#!/usr/bin/env python3
"""
gen_pole_controller.py -- the pole and wall mounted traffic signal controller cabinets (Roads,
#360): models, blockstates and the green paint texture.

The cabinet is drawn after the slim upright controller boxes clamped to signal poles on tight
New York sidewalks: a box 10 px wide, 14 tall and 7 deep (about 0.6 x 0.9 x 0.45 m) under a rain
cap, a full-height door hung on three hinge knuckles with a handle and lock on the other side, a
police panel in the door's upper half, louvres in its lower half, a data plate, side vents, and a
conduit that drops out of the floor and turns back into the pole or wall below it.

Model space: the door faces north (-z) and the pole or wall is behind it (south, +z); the
blockstate turns it with the block's facing. The cabinet body is the same in every model, so the
block's box (BlockTrafficSignalControllerPole.NORTH_BOUNDING_BOX) never depends on the mount.
What differs is the mount, the block's `mount` actual-state property:

  wall      two rails across the back to the wall face (z 16), the conduit into the wall
  large     two arms back to a pole whose centre line is one cell behind (z 24), each ending in a
  thin      clamp band round it, sized for the 12-across pole family, the 8-across thin pole and
  pedestal  the 6-across pedestal pole (CsmPoleFit's radii); the conduit runs into the pole

The four colours are the NYC signal arms' finishes, each a texture swapped in by its blockstate:
galvanized (metal_white, as the arms' galvanized), black, brown, and a green this writes, recoloured
from the brown so it carries the same grain.

Every model goes through model_depth.py (separate, then prune), as every JSON-model generator's.

Usage:
    py -3 gen_pole_controller.py [--check] [--fragments]
"""
import io
import json
import os
import shutil
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import model_depth  # noqa: E402
from gen_preempt_detector import box  # noqa: E402

from PIL import Image  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, 'modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm')
MODEL_DIR = os.path.join('models', 'block', 'trafficsignals', 'pole_controller')
MODEL_REF = 'csm:trafficsignals/pole_controller/'

TEX = 'csm:blocks/trafficsignals/shared_textures/'
BODY, DARK, HW = '#body', '#dark', '#hardware'

# registry name suffix: body texture
COLOURS = {
    'galvanized': TEX + 'metal_white',
    'black': TEX + 'metal_black',
    'green': TEX + 'metal_green',
    'brown': TEX + 'metal_brown',
}
NAMES = {
    'galvanized': ('Galvanized', 'Verzinkt', 'Galvanizado', 'Galvaniserad'),
    'black': ('Black', 'Schwarz', 'Negro', 'Svart'),
    'green': ('Green', 'Grün', 'Verde', 'Grön'),
    'brown': ('Brown', 'Braun', 'Marrón', 'Brun'),
}
# en_us, de_de, es_es, sv_se
LANG_NAME = ('Traffic Signal Controller (Pole/Wall Cabinet, %s)',
             'Verkehrssignal-Steuergerät (Mast-/Wandschrank, %s)',
             'Controlador de Semáforo (Gabinete de Poste/Pared, %s)',
             'Trafiksignal-Styrenhet (Stolp-/Väggskåp, %s)')

# The green: the brown texture's grain in NYC's dark signal green.
BROWN_PNG = os.path.join(ASSETS, 'textures', 'blocks', 'trafficsignals', 'shared_textures',
                         'metal_brown.png')
GREEN_PNG = os.path.join('textures', 'blocks', 'trafficsignals', 'shared_textures',
                         'metal_green.png')
BROWN_BASE = (88, 56, 42)
GREEN_BASE = (40, 72, 50)

# The pole behind: its centre line one cell back, and each mount's tube radius (CsmPoleFit).
POLE_Z = 24.0
POLE_RADII = {'large': 6.0, 'thin': 4.0, 'pedestal': 3.0}
MOUNTS = ('wall', 'large', 'thin', 'pedestal')
# The two mounting heights (arms or rails), bottom and top of each.
MOUNT_YS = ((2.6, 4.0), (12.0, 13.4))


def cabinet():
    """The cabinet: body, rain cap, door and its hardware, louvres, vents and the conduit's drop
    out of the floor."""
    els = [
        box((3.0, 1.0, 8.0), (13.0, 15.0, 15.0), BODY),                     # the body
        box((2.6, 15.0, 7.4), (13.4, 16.0, 15.4), BODY),                    # rain cap
        box((3.4, 0.5, 8.4), (12.6, 1.0, 14.6), BODY, faces='nsewd'),       # bottom pan
        box((3.4, 1.4, 7.6), (12.6, 14.4, 8.0), BODY, faces='newud'),       # the door
        # louvres in the door's lower half: a dark recess and four slats across it
        box((5.4, 2.2, 7.5), (11.6, 6.2, 7.6), DARK, faces='newud'),
    ]
    for y in (2.4, 3.35, 4.3, 5.25):
        els.append(box((5.4, y, 7.25), (11.6, y + 0.45, 7.5), BODY, faces='nud'))
    # the police panel, its keyhole, and the data plate under it
    els.append(box((6.2, 10.2, 7.45), (10.4, 13.4, 7.6), BODY, faces='newud'))
    els.append(box((9.4, 11.6, 7.4), (9.9, 12.1, 7.45), DARK, faces='n'))
    els.append(box((6.8, 7.6, 7.5), (9.8, 8.8, 7.6), HW, faces='newud'))
    # three hinge knuckles on the east edge
    for y in (2.4, 7.2, 12.0):
        els.append(box((12.6, y, 7.5), (13.2, y + 1.6, 8.2), HW))
    # the handle on two standoffs, and the lock above it, on the west edge
    els.append(box((4.2, 6.0, 7.0), (4.8, 10.0, 7.3), HW))
    els.append(box((4.3, 6.2, 7.3), (4.7, 6.8, 7.6), HW, faces='ewud'))
    els.append(box((4.3, 9.2, 7.3), (4.7, 9.8, 7.6), HW, faces='ewud'))
    els.append(box((4.0, 10.6, 7.4), (5.0, 11.6, 7.6), HW, faces='newud'))
    # a vent high on each side
    els.append(box((2.9, 11.0, 9.5), (3.0, 13.4, 13.5), DARK, faces='wud'))
    els.append(box((13.0, 11.0, 9.5), (13.1, 13.4, 13.5), DARK, faces='eud'))
    # the conduit's drop out of the floor, and the fitting it turns back in
    els.append(box((7.3, -1.2, 12.2), (8.7, 0.5, 13.6), BODY, faces='nsew'))
    els.append(box((7.0, -2.9, 11.9), (9.0, -1.2, 13.9), BODY))
    return els


def conduit_back(z1):
    """The conduit from the fitting under the cabinet back to z1 (the wall or the pole)."""
    return [box((7.3, -2.75, 13.9), (8.7, -1.35, z1), BODY, faces='ewud')]


def wall_mount():
    """Two rails across the cabinet's back to the wall face, and the conduit into the wall."""
    els = [box((2.4, y0, 15.0), (13.6, y1, 16.0), HW, faces='newud') for y0, y1 in MOUNT_YS]
    return els + conduit_back(16.0)


def pole_mount(radius):
    """A strut up the cabinet's back, two arms from it to the pole's skin, each ending in a clamp
    band of four straps round the pole, and the conduit into the pole."""
    skin = POLE_Z - radius
    band = radius + 0.4
    els = [box((7.0, 2.0, 15.0), (9.0, 14.0, 15.6), HW, faces='newud')]
    for y0, y1 in MOUNT_YS:
        els.append(box((7.3, y0, 15.6), (8.7, y1, skin), HW, faces='ewud'))
        b0, b1 = y0 - 0.4, y1 + 0.4
        lo, hi = POLE_Z - band - 0.6, POLE_Z + band + 0.6
        els.append(box((8 - band - 0.6, b0, lo), (8 + band + 0.6, b1, POLE_Z - band), HW))
        els.append(box((8 - band - 0.6, b0, POLE_Z + band), (8 + band + 0.6, b1, hi), HW))
        els.append(box((8 - band - 0.6, b0, POLE_Z - band), (8 - band, b1, POLE_Z + band), HW))
        els.append(box((8 + band, b0, POLE_Z - band), (8 + band + 0.6, b1, POLE_Z + band), HW))
    return els + conduit_back(skin + 0.5)


def model(mount):
    els = cabinet() + (wall_mount() if mount == 'wall' else pole_mount(POLE_RADII[mount]))
    return {
        'parent': 'block/block',
        'textures': {'particle': COLOURS['galvanized'], 'body': COLOURS['galvanized'],
                     'dark': TEX + 'metal_black', 'hardware': TEX + 'metal_silver'},
        'elements': els,
    }


def model_name(mount):
    return 'signalcontroller_pole_' + mount


def blockstate(colour):
    tex = COLOURS[colour]
    return {
        'forge_marker': 1,
        'defaults': {'model': MODEL_REF + model_name('wall'),
                     'textures': {'body': tex, 'particle': tex}},
        'variants': {
            'facing': {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}},
            'powered': {'false': {}, 'true': {}},
            'mount': {m: {'model': MODEL_REF + model_name(m)} for m in MOUNTS},
            'inventory': [{'model': MODEL_REF + model_name('wall'),
                           'transform': 'forge:default-block'}],
        },
    }


def green_png():
    """The brown texture recoloured, channel by channel, to the green."""
    src = Image.open(BROWN_PNG).convert('RGBA')
    out = Image.new('RGBA', src.size)
    px, po = src.load(), out.load()
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            r, g, b, a = px[x, y]
            po[x, y] = tuple(max(0, min(255, round(c * t / s)))
                             for c, t, s in zip((r, g, b), GREEN_BASE, BROWN_BASE)) + (a,)
    buf = io.BytesIO()
    out.save(buf, format='PNG', optimize=True)
    return buf.getvalue()


def text(obj):
    return json.dumps(obj, indent=2) + '\n'


def dump(path, data):
    """Writes a JSON file as every file here is written (model_depth.py writes back through it)."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write(text(data))


def files():
    """Every JSON file this writes: path relative to the assets root, and its data."""
    out = {}
    for m in MOUNTS:
        out[os.path.join(MODEL_DIR, model_name(m) + '.json')] = model(m)
    for c in COLOURS:
        out[os.path.join('blockstates', 'signalcontroller_pole_%s.json' % c)] = blockstate(c)
    return out


def generate(root):
    """Writes every file under root, the green texture first (model_depth.py reads the textures
    the models use), then separates and prunes the models."""
    os.makedirs(os.path.dirname(os.path.join(root, GREEN_PNG)), exist_ok=True)
    with open(os.path.join(root, GREEN_PNG), 'wb') as fh:
        fh.write(green_png())
    written = []
    for rel, data in sorted(files().items()):
        dump(os.path.join(root, rel), data)
        written.append(rel)
    model_depth.separate(root, written, dump)
    return written


def same_json(a, b):
    if not os.path.exists(b):
        return False
    with open(a, encoding='utf-8') as fa, open(b, encoding='utf-8') as fb:
        return fa.read().replace('\r\n', '\n') == fb.read().replace('\r\n', '\n')


def same_png(data, path):
    """Pixels, not bytes: a PNG encoder may write the same image differently."""
    if not os.path.exists(path):
        return False
    a = Image.open(io.BytesIO(data)).convert('RGBA')
    b = Image.open(path).convert('RGBA')
    return a.size == b.size and a.tobytes() == b.tobytes()


def fragments():
    langs = ('en_us', 'de_de', 'es_es', 'sv_se')
    for i, lang in enumerate(langs):
        print('# %s.lang' % lang)
        for c in COLOURS:
            print('tile.signalcontroller_pole_%s.name=%s' % (c, LANG_NAME[i] % NAMES[c][i]))
    print('# CsmTabTrafficSignals')
    for c in COLOURS:
        print('    initTabBlock(new BlockTrafficSignalControllerPole("signalcontroller_pole_%s"));'
              % c)


def main():
    if '--fragments' in sys.argv:
        fragments()
        return
    check = '--check' in sys.argv
    tmp = tempfile.mkdtemp(prefix='pole_controller_')
    try:
        written = generate(tmp)
        drift = [r for r in written
                 if not same_json(os.path.join(tmp, r), os.path.join(ASSETS, r))]
        green = green_png()
        green_drift = not same_png(green, os.path.join(ASSETS, GREEN_PNG))
        if check:
            if drift or green_drift:
                print('pole controller has drifted:\n  '
                      + '\n  '.join(drift + ([GREEN_PNG] if green_drift else [])))
                sys.exit(1)
            print('pole controller is up to date')
            return
        for rel in drift:
            dst = os.path.join(ASSETS, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copyfile(os.path.join(tmp, rel), dst)
            print('wrote', os.path.relpath(dst, ROOT))
        if green_drift:
            dst = os.path.join(ASSETS, GREEN_PNG)
            with open(dst, 'wb') as fh:
                fh.write(green)
            print('wrote', os.path.relpath(dst, ROOT))
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == '__main__':
    main()
