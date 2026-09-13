"""Generate the railroad crossing hardware's assets: the flasher mast's lens strips and models,
the gate cabinet's model, and the four blockstates.

The flasher's lamps wig-wag the way the RRFB's lamps run their sequence: in an animated
texture, so every crossing in the world flashes in step. The strip is two frames -- left lamp
lit, right lamp lit -- at ten ticks a frame, 60 flashes a minute, inside the MUTCD's 35 to 65.
An OptiFine ``_e`` companion carries the lit lamp alone on the same clock, and the block emits
no light, the RRFB rule. Unpowered, the blockstate points the lens slot at the dark texture.

The flasher block is the mast HEAD: crossarm, lamps, bell and the crossbuck above, on a stub
of pole, so it goes on top of any traffic pole. The crossbuck plate wears the R15-1 sign's own
texture, so the two cannot differ. The gate's block is only the mechanism cabinet; its arm is
drawn by the tile entity's renderer.

Writes into modules/roads/src/main/resources/assets/csm/:
    textures/blocks/trafficaccessories/rail/rr_lens_off.png, rr_lens_flash.png (+ .mcmeta),
        rr_lens_flash_e.png (+ .mcmeta), rr_hardware.png
    models/block/trafficaccessories/shared_models/rail_crossing_flasher.json,
        rail_crossing_gate.json
    blockstates/railroad_crossing_flasher.json, railroad_crossing_gate_{1,2,3}.json

Run from the repo root:  python dev-env-utils/scripts/gen_rail_crossing.py
"""

import json
import os

from PIL import Image, ImageDraw

ROADS = os.path.join('modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm')
TEX = os.path.join(ROADS, 'textures', 'blocks', 'trafficaccessories', 'rail')
MODELS = os.path.join(ROADS, 'models', 'block', 'trafficaccessories', 'shared_models')
BLOCKSTATES = os.path.join(ROADS, 'blockstates')

FRAME = 64
FRAMETIME = 10  # ticks a frame: two frames = one second, 60 flashes a minute

LENS_DARK = (48, 12, 10, 255)
LENS_DARK_EDGE = (26, 6, 5, 255)
LENS_LIT = (255, 52, 36, 255)
LENS_LIT_CORE = (255, 190, 170, 255)
HOOD = (24, 24, 26, 255)

ALUMINIUM = (168, 170, 174, 255)
ALUMINIUM_DARK = (120, 122, 126, 255)
BLACK = (28, 28, 30, 255)
CABINET = (150, 152, 156, 255)


def lens_frame(left_lit, right_lit, lit_only=False):
    """Two round lenses, in hoods, side by side in one 64 px frame."""
    img = Image.new('RGBA', (FRAME, FRAME), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for cx, lit in ((16, left_lit), (48, right_lit)):
        if lit_only and not lit:
            continue
        r = 13
        if not lit_only:
            d.ellipse((cx - r - 2, 32 - r - 2, cx + r + 2, 32 + r + 2), fill=HOOD)
        if lit:
            d.ellipse((cx - r, 32 - r, cx + r, 32 + r), fill=LENS_LIT)
            d.ellipse((cx - r * 0.5, 32 - r * 0.5, cx + r * 0.5, 32 + r * 0.5), fill=LENS_LIT_CORE)
        else:
            d.ellipse((cx - r, 32 - r, cx + r, 32 + r), fill=LENS_DARK, outline=LENS_DARK_EDGE)
    return img


def strip(frames, lit_only=False):
    out = Image.new('RGBA', (FRAME, FRAME * len(frames)), (0, 0, 0, 0))
    for i, (l, r) in enumerate(frames):
        out.paste(lens_frame(l, r, lit_only), (0, i * FRAME))
    return out


def write_mcmeta(path):
    with open(path, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write('{\n  "animation": {\n    "frametime": %d\n  }\n}\n' % FRAMETIME)


def hardware_swatch():
    """16 x 16 of flat colours: aluminium in the top half, black below, cabinet gray in a strip
    at the very bottom -- the model's faces pick a region by uv."""
    img = Image.new('RGBA', (16, 16), ALUMINIUM)
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 1), fill=ALUMINIUM_DARK)
    d.rectangle((0, 8, 15, 13), fill=BLACK)
    d.rectangle((0, 14, 15, 15), fill=CABINET)
    return img


def box(frm, to, tex, uv=None):
    faces = {}
    for f in ('north', 'south', 'east', 'west', 'up', 'down'):
        faces[f] = {'uv': uv or [0, 0, 4, 4], 'texture': tex}
    return {'from': frm, 'to': to, 'faces': faces}


ALU = [0, 2, 4, 6]
BLK = [0, 9, 4, 13]
CAB = [0, 14, 4, 16]


def flasher_model():
    els = [
        box([7, 0, 7], [9, 22, 9], '#hardware', ALU),            # pole stub, up through the crossbuck
        box([1, 6, 7.25], [15, 8, 8.75], '#hardware', ALU),      # crossarm
        box([6.5, 1.5, 6.5], [9.5, 5, 9.5], '#hardware', BLK),   # bell
        box([1, 3.5, 6.5], [6, 10.5, 9.5], '#hardware', BLK),    # left lamp hood
        box([10, 3.5, 6.5], [15, 10.5, 9.5], '#hardware', BLK),  # right lamp hood
    ]
    # Lens faces: a thin plate a hair proud of each hood, north and south, with the left hood
    # sampling the left lamp of the strip and the right hood the right lamp. Seen from the
    # south the lamps swap sides, which is what a real double-sided head does.
    for x0, x1, uv in ((1.5, 5.5, [0, 0, 8, 16]), (10.5, 14.5, [8, 0, 16, 16])):
        els.append({'from': [x0, 4, 6.4], 'to': [x1, 10, 6.5],
                    'faces': {'north': {'uv': uv, 'texture': '#lens'}}})
        els.append({'from': [x0, 4, 9.5], 'to': [x1, 10, 9.6],
                    'faces': {'south': {'uv': uv, 'texture': '#lens'}}})
    # Crossbuck: a flat plate above the crossarm carrying the sign texture on both faces; the
    # south face mirrored so its legend reads the right way round. A JSON element may reach
    # no further than 32, so the plate is 22 units square, its top at the limit.
    els.append({'from': [-3, 10, 7.9], 'to': [19, 32, 8.0],
                'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#crossbuck'}}})
    els.append({'from': [-3, 10, 8.0], 'to': [19, 32, 8.1],
                'faces': {'south': {'uv': [16, 0, 0, 16], 'texture': '#crossbuck'}}})
    return {
        'credit': 'Generated by dev-env-utils/scripts/gen_rail_crossing.py',
        'textures': {
            'hardware': 'csm:blocks/trafficaccessories/rail/rr_hardware',
            'lens': 'csm:blocks/trafficaccessories/rail/rr_lens_off',
            'crossbuck': 'csm:blocks/trafficsigns/signrailroadcrossbuck',
            'particle': 'csm:blocks/trafficaccessories/rail/rr_hardware',
        },
        'elements': els,
        'display': {
            'gui': {'rotation': [30, 45, 0], 'translation': [0, -1.5, 0], 'scale': [0.45, 0.45, 0.45]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
            'fixed': {'scale': [0.5, 0.5, 0.5]},
        },
    }


def gate_model():
    els = [
        box([4, 0, 4], [12, 12, 12], '#hardware', CAB),          # mechanism cabinet
        box([3.5, 0, 3.5], [12.5, 1, 12.5], '#hardware', BLK),   # base
        box([12, 9, 7], [14, 11, 9], '#hardware', BLK),          # pivot shaft, to the right
        box([5, 12, 5], [11, 12.5, 11], '#hardware', ALU),       # lid
    ]
    return {
        'credit': 'Generated by dev-env-utils/scripts/gen_rail_crossing.py',
        'textures': {
            'hardware': 'csm:blocks/trafficaccessories/rail/rr_hardware',
            'particle': 'csm:blocks/trafficaccessories/rail/rr_hardware',
        },
        'elements': els,
        'display': {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
            'fixed': {'scale': [0.5, 0.5, 0.5]},
        },
    }


def facing_variants():
    return {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}}


def flasher_blockstate():
    return {
        'forge_marker': 1,
        'defaults': {'model': 'csm:trafficaccessories/shared_models/rail_crossing_flasher'},
        'variants': {
            'facing': facing_variants(),
            'powered': {
                'false': {},
                'true': {'textures': {'lens': 'csm:blocks/trafficaccessories/rail/rr_lens_flash'}},
            },
            'inventory': [{'textures': {'lens': 'csm:blocks/trafficaccessories/rail/rr_lens_flash'}}],
        },
    }


def gate_blockstate():
    return {
        'forge_marker': 1,
        'defaults': {'model': 'csm:trafficaccessories/shared_models/rail_crossing_gate'},
        'variants': {
            'facing': facing_variants(),
            'powered': {'false': {}, 'true': {}},
            'inventory': [{}],
        },
    }


def dump(path, data):
    with open(path, 'w', encoding='utf-8', newline='\n') as fh:
        json.dump(data, fh, indent=2)
        fh.write('\n')


def main():
    for d in (TEX, MODELS, BLOCKSTATES):
        os.makedirs(d, exist_ok=True)
    strip([(False, False)]).save(os.path.join(TEX, 'rr_lens_off.png'))
    frames = [(True, False), (False, True)]
    flash = os.path.join(TEX, 'rr_lens_flash.png')
    strip(frames).save(flash)
    write_mcmeta(flash + '.mcmeta')
    emissive = os.path.join(TEX, 'rr_lens_flash_e.png')
    strip(frames, lit_only=True).save(emissive)
    write_mcmeta(emissive + '.mcmeta')
    hardware_swatch().save(os.path.join(TEX, 'rr_hardware.png'))
    dump(os.path.join(MODELS, 'rail_crossing_flasher.json'), flasher_model())
    dump(os.path.join(MODELS, 'rail_crossing_gate.json'), gate_model())
    dump(os.path.join(BLOCKSTATES, 'railroad_crossing_flasher.json'), flasher_blockstate())
    for lanes in (1, 2, 3):
        dump(os.path.join(BLOCKSTATES, 'railroad_crossing_gate_%d.json' % lanes), gate_blockstate())
    print('wrote the rail crossing textures, models and blockstates')


if __name__ == '__main__':
    main()
