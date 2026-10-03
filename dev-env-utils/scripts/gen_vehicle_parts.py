"""The CSM: Vehicles module's Immersive Vehicles content pack (pack id csmvehicles).

Immersive Vehicles finds a pack by scanning every jar in the mods folder for an
assets/<packID>/packdefinition.json, so the module jar carries this pack and nothing registers
it. Everything the pack holds is written here from one catalogue:

  - the LED lightbar, in four colour schemes, for a vehicle's generic_lightbar slot. Eight lamp
    modules flash in pairs, left then right, with a preemption emitter strobing in the middle;
  - the preemption emitter on its own, for a generic_roofdevice slot, for vehicles that keep
    their own lights;
  - the siren speaker, for a generic_siren slot, with wail, yelp and hi-lo tones (the sounds are
    gen_vehicle_sounds.py's).

Each part's lamps light on EMERLTS, the variable Immersive Vehicles' own pack and UNU's use for
emergency lights, and declare it, so a part fitted to another pack's vehicle flashes with that
vehicle's switch: the panel toggles every variable of the same name together. The Vehicles
module's IvPreemptSource calls Roads' preempt detectors for any vehicle with that switch on.

Lamps are OBJ objects named with a leading '&', which Immersive Vehicles draws as lights. An
emissive light is drawn in its JSON colour while lit, so one model serves every colour scheme;
the scheme's own texture is only what the lenses look like dark. A light's brightness comes from
'a_b_c_cycle' variables, which are lit for b - 1 ticks of every a + b + c (not b), measured off
the parser in AEntityD_Definable.

Writes under modules/vehicles/src/main/resources/assets/: csmvehicles/jsondefs/parts/,
csmvehicles/objmodels/parts/, csmvehicles/textures/parts/ and textures/items/parts/, and the
item models under mts/models/item/ (Immersive Vehicles registers every pack item as
mts:<packID>.<name>). Run from the repo root:

    python dev-env-utils/scripts/gen_vehicle_parts.py           # write
    python dev-env-utils/scripts/gen_vehicle_parts.py --check   # exit 1 if the tree has drifted
"""

import io
import json
import os
import sys

from PIL import Image, ImageDraw

PACK = 'csmvehicles'
ROOT = os.path.join('modules', 'vehicles', 'src', 'main', 'resources', 'assets')
PACK_DIR = os.path.join(ROOT, PACK)
TEX = 64  # every part texture is 64 x 64

# Lamp colours, lit (the JSON colour) and dark (the lens as the texture shows it).
COLOURS = {
    'red': ('#FF1E10', (120, 18, 14)),
    'blue': ('#1E46FF', (18, 34, 120)),
    'white': ('#EEF4FF', (150, 152, 158)),
    'amber': ('#FFA000', (130, 82, 10)),
}
EMITTER = ('#DCEBFF', (70, 76, 88))

# Lightbar colour schemes: the eight modules, left (+x) to right.
SCHEMES = {
    'redblue': ('Red/Blue', ['red'] * 4 + ['blue'] * 4),
    'red': ('Red', ['red'] * 8),
    'redwhite': ('Red/White', ['red', 'white'] * 4),
    'amber': ('Amber', ['amber'] * 8),
}

# Texture cells, (u0, v0, u1, v1) in pixels on the 64 x 64 texture.
CELL_BODY = (0, 0, 16, 16)      # black powder coat
CELL_METAL = (16, 0, 32, 16)    # aluminium
CELL_GRILLE = (32, 0, 48, 16)   # speaker grille
CELL_EMITTER = (48, 0, 56, 8)   # emitter lens, dark


def lens_cell(i):
    """The texture cell of lightbar module i (0..7)."""
    return (i * 8, 16, i * 8 + 8, 24)


# ------------------------------------------------------------------------------------------
# OBJ writing
# ------------------------------------------------------------------------------------------

class Obj:
    """An OBJ under construction: named objects of boxes, with indices global to the file as
    Immersive Vehicles' parser expects."""

    def __init__(self):
        self.lines = []
        self.v = 0
        self.vt = 0

    def box(self, name, lo, hi, cell, faces=('n', 's', 'e', 'w', 'u', 'd')):
        """A box from lo to hi (metres; +x left, +y up, +z forward), every face showing `cell`.
        Faces wind counter-clockwise seen from outside."""
        self.lines.append('o %s' % name)
        (x0, y0, z0), (x1, y1, z1) = lo, hi
        quads = {
            's': ((x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1), (0, 0, 1)),
            'n': ((x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0), (0, 0, -1)),
            'e': ((x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (1, 0, 0)),
            'w': ((x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0), (-1, 0, 0)),
            'u': ((x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0), (0, 1, 0)),
            'd': ((x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1), (0, -1, 0)),
        }
        u0, v0, u1, v1 = (c / float(TEX) for c in cell)
        # an inset of a quarter texel keeps the sampler off the neighbouring cell
        e = 0.25 / TEX
        uvs = ((u0 + e, v1 - e), (u1 - e, v1 - e), (u1 - e, v0 + e), (u0 + e, v0 + e))
        for key in ('n', 's', 'e', 'w', 'u', 'd'):
            if key not in faces:
                continue
            *corners, normal = quads[key]
            for c in corners:
                self.lines.append('v %.5f %.5f %.5f' % c)
            for u, v in uvs:
                # OBJ texture coordinates run up from the bottom; the parser flips them back
                self.lines.append('vt %.6f %.6f' % (u, 1.0 - v))
            self.lines.append('vn %d %d %d' % normal)
            n = self.v // 4  # one normal per quad
            refs = ['%d/%d/%d' % (self.v + k + 1, self.vt + k + 1, n + 1) for k in range(4)]
            self.lines.append('f ' + ' '.join(refs))
            self.v += 4
            self.vt += 4

    def text(self):
        return '# generated by dev-env-utils/scripts/gen_vehicle_parts.py\n' + \
            '\n'.join(self.lines) + '\n'


# ------------------------------------------------------------------------------------------
# Geometry
# ------------------------------------------------------------------------------------------

BAR_HALF = 0.60    # half the lightbar's width
BAR_DEPTH = 0.15   # half its depth
MODULE_W = 0.14    # each lamp module's width
MODULE_GAP = 0.01


def module_x(i):
    """The x span of lightbar module i, 0 at the left (+x) end."""
    left = BAR_HALF - 0.02 - i * (MODULE_W + MODULE_GAP)
    if i >= 4:
        left -= 0.06  # the gap in the middle the emitter sits in
    return left - MODULE_W, left


def lightbar_obj():
    o = Obj()
    o.box('base', (-BAR_HALF, 0.03, -BAR_DEPTH), (BAR_HALF, 0.06, BAR_DEPTH), CELL_BODY)
    for x in (-0.45, 0.45):
        o.box('foot_%s' % ('l' if x > 0 else 'r'), (x - 0.05, 0.0, -0.10), (x + 0.05, 0.03, 0.10),
              CELL_BODY, faces=('n', 's', 'e', 'w', 'd'))
    o.box('end_l', (BAR_HALF - 0.02, 0.06, -BAR_DEPTH), (BAR_HALF, 0.14, BAR_DEPTH), CELL_METAL)
    o.box('end_r', (-BAR_HALF, 0.06, -BAR_DEPTH), (-BAR_HALF + 0.02, 0.14, BAR_DEPTH), CELL_METAL)
    o.box('cap', (-BAR_HALF, 0.14, -BAR_DEPTH), (BAR_HALF, 0.15, BAR_DEPTH), CELL_METAL,
          faces=('n', 's', 'u', 'd', 'e', 'w'))
    # the centre housing the emitter looks out of
    o.box('centre', (-0.04, 0.06, -BAR_DEPTH), (0.04, 0.14, BAR_DEPTH), CELL_BODY,
          faces=('n', 's'))
    for i in range(8):
        x0, x1 = module_x(i)
        o.box('&Lamp_%d' % (i + 1), (x0, 0.065, -BAR_DEPTH + 0.005),
              (x1, 0.135, BAR_DEPTH - 0.005), lens_cell(i))
    o.box('&Emitter', (-0.025, 0.08, BAR_DEPTH), (0.025, 0.12, BAR_DEPTH + 0.01), CELL_EMITTER,
          faces=('s', 'e', 'w', 'u', 'd'))
    return o.text()


def emitter_obj():
    o = Obj()
    o.box('housing', (-0.06, 0.0, -0.06), (0.06, 0.07, 0.05), CELL_BODY)
    o.box('bracket', (-0.08, 0.0, -0.04), (0.08, 0.01, 0.02), CELL_METAL)
    o.box('&Emitter', (-0.035, 0.015, 0.05), (0.035, 0.055, 0.06), CELL_EMITTER,
          faces=('s', 'e', 'w', 'u', 'd'))
    return o.text()


def siren_obj():
    o = Obj()
    o.box('housing', (-0.11, 0.0, -0.06), (0.11, 0.15, 0.04), CELL_BODY)
    o.box('grille', (-0.10, 0.01, 0.04), (0.10, 0.14, 0.05), CELL_GRILLE,
          faces=('s', 'e', 'w', 'u', 'd'))
    o.box('bracket', (-0.13, 0.0, -0.05), (0.13, 0.01, 0.03), CELL_METAL)
    return o.text()


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------

def base_texture():
    img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=(28, 29, 32, 255))
    for y in range(0, 16, 4):  # a faint sheen on the powder coat
        d.line((0, y, 15, y), fill=(34, 35, 39, 255))
    d.rectangle((16, 0, 31, 15), fill=(170, 174, 180, 255))
    d.line((16, 0, 31, 0), fill=(206, 210, 214, 255))
    d.rectangle((32, 0, 47, 15), fill=(22, 22, 24, 255))
    for y in range(1, 16, 2):  # grille slots
        d.line((33, y, 46, y), fill=(52, 54, 58, 255))
    d.rectangle((48, 0, 55, 7), fill=EMITTER[1] + (255,))
    d.point((50, 2), fill=(120, 126, 138, 255))
    return img


def scheme_texture(colours):
    img = base_texture()
    d = ImageDraw.Draw(img)
    for i, name in enumerate(colours):
        u0, v0, u1, v1 = lens_cell(i)
        dark = COLOURS[name][1]
        d.rectangle((u0, v0, u1 - 1, v1 - 1), fill=dark + (255,))
        # the LED rows behind the lens
        for y in range(v0 + 2, v1 - 1, 2):
            d.line((u0 + 1, y, u1 - 2, y), fill=tuple(min(255, c + 30) for c in dark) + (255,))
    return img


def icon(draw_fn):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(img))
    return img


def lightbar_icon(colours):
    def draw(d):
        d.rectangle((0, 7, 15, 11), fill=(28, 29, 32, 255))
        d.rectangle((0, 6, 15, 6), fill=(170, 174, 180, 255))
        for i, name in enumerate(colours):
            lit = COLOURS[name][0]
            rgb = tuple(int(lit[k:k + 2], 16) for k in (1, 3, 5))
            x = 1 + i * 2 if i < 4 else 2 + i * 2 - 1
            d.rectangle((x, 8, x + 1, 10), fill=rgb + (255,))
        d.rectangle((3, 12, 4, 12), fill=(28, 29, 32, 255))
        d.rectangle((11, 12, 12, 12), fill=(28, 29, 32, 255))
    return icon(draw)


def emitter_icon():
    def draw(d):
        d.rectangle((3, 6, 12, 12), fill=(28, 29, 32, 255))
        d.rectangle((5, 8, 10, 10), fill=(220, 235, 255, 255))
        d.rectangle((2, 13, 13, 13), fill=(170, 174, 180, 255))
    return icon(draw)


def siren_icon():
    def draw(d):
        d.rectangle((2, 4, 13, 13), fill=(28, 29, 32, 255))
        for y in range(5, 13, 2):
            d.line((3, y, 12, y), fill=(70, 72, 76, 255))
        d.rectangle((1, 14, 14, 14), fill=(170, 174, 180, 255))
    return icon(draw)


# ------------------------------------------------------------------------------------------
# Part definitions
# ------------------------------------------------------------------------------------------

MATERIALS = [
    ['minecraft:iron_ingot:0:2', 'minecraft:redstone:0:2', 'minecraft:glass_pane:0:2'],
    ['minecraft:iron_ingot:2', 'minecraft:redstone:2', 'minecraft:glass_pane:2'],
]

# Lightbar flash: modules flash in pairs, left then right, twice each, over 16 ticks.
LEFT_FLASHES = ['0_3_13_cycle', '3_3_10_cycle']
RIGHT_FLASHES = ['8_3_5_cycle', '11_3_2_cycle']
EMITTER_FLASH = '0_2_1_cycle'  # lit one tick in three, the fastest strobe a tick clock allows


def visible_on(variable):
    return {'animationType': 'visibility', 'variable': variable, 'clampMin': 1, 'clampMax': 1}


def lit_by(variable):
    return {'animationType': 'translation', 'axis': [0, 1, 0], 'variable': variable}


def flare(pos, axis, size):
    return {'pos': pos, 'axis': axis, 'flareHeight': size, 'flareWidth': size}


def emitter_light(front_z, y):
    return {
        'objectName': '&Emitter',
        'emissive': True,
        'isElectric': True,
        'color': EMITTER[0],
        'brightnessAnimations': [visible_on('EMERLTS'), lit_by(EMITTER_FLASH)],
        'blendableComponents': [flare([0, y, front_z], [0, 0, 1], 0.35)],
    }


def lightbar_json(key):
    label, colours = SCHEMES[key]
    lights = []
    for i, name in enumerate(colours):
        x0, x1 = module_x(i)
        cx = round((x0 + x1) / 2, 4)
        flashes = LEFT_FLASHES if i < 4 else RIGHT_FLASHES
        lights.append({
            'objectName': '&Lamp_%d' % (i + 1),
            'emissive': True,
            'isElectric': True,
            'color': COLOURS[name][0],
            'brightnessAnimations': [visible_on('EMERLTS')] + [lit_by(f) for f in flashes],
            'blendableComponents': [flare([cx, 0.1, BAR_DEPTH], [0, 0, 1], 0.45),
                                    flare([cx, 0.1, -BAR_DEPTH], [0, 0, -1], 0.45)],
        })
    lights.append(emitter_light(BAR_DEPTH + 0.01, 0.1))
    return {
        'definitions': [{
            'name': 'CSM LED Lightbar (%s)' % label,
            'subName': '',
            'modelName': 'csm_lightbar_led',
            'extraMaterialLists': [[], []],
        }],
        'general': {
            'description': 'A low LED lightbar with a preemption emitter in the middle. Flashes '
                           'with the vehicle\'s emergency lights switch (EMERLTS), and while it '
                           'does, intersections with a CSM preempt detector give it the green.',
            'type': 'generic_lightbar',
            'stackSize': 4,
            'materialLists': MATERIALS,
        },
        'generic': {'width': 1.2, 'height': 0.15},
        'rendering': {'customVariables': ['EMERLTS'], 'lightObjects': lights},
    }


def emitter_json():
    return {
        'definitions': [{'name': 'CSM Preemption Emitter', 'subName': '',
                         'extraMaterialLists': [[], []]}],
        'general': {
            'description': 'A forward-facing strobe for vehicles that keep their own lights. '
                           'Runs with the emergency lights switch (EMERLTS); intersections with '
                           'a CSM preempt detector give the vehicle the green.',
            'type': 'generic_roofdevice',
            'stackSize': 4,
            'materialLists': MATERIALS,
        },
        'generic': {'width': 0.16, 'height': 0.07},
        'rendering': {'customVariables': ['EMERLTS'],
                      'lightObjects': [emitter_light(0.06, 0.035)]},
    }


# Siren tones: (variable on the panel, sound name).
SIREN_TONES = [('siren', 'siren_wail'), ('siren_yelp', 'siren_yelp'),
               ('siren_hilo', 'siren_hilo')]


def siren_json():
    return {
        'definitions': [{'name': 'CSM Siren Speaker', 'subName': '',
                         'extraMaterialLists': [[], []]}],
        'general': {
            'description': 'A 100 W siren speaker with three tones, each its own switch on the '
                           'panel: wail (siren), yelp and hi-lo. Fit one per vehicle.',
            'type': 'generic_siren',
            'stackSize': 4,
            'materialLists': MATERIALS,
        },
        'generic': {'width': 0.26, 'height': 0.15},
        'rendering': {
            'customVariables': [v for v, _ in SIREN_TONES],
            'sounds': [{'name': '%s:%s' % (PACK, sound), 'looping': True,
                        'activeAnimations': [visible_on(variable)]}
                       for variable, sound in SIREN_TONES],
        },
    }


# ------------------------------------------------------------------------------------------
# Tree crew: the chip bed (loaded only with CSM: Parks & Greenery, whose Mulch it makes)
# ------------------------------------------------------------------------------------------
#
# A chip box for a UNU Contractor's bed slot, with a chipper on its tail. It is four parts
# because that is how Immersive Vehicles links them: an effector that is a sub-part of a crate
# pushes what it collects or makes into that crate, and a crafter pulls its inputs from it.
#
#   csm_chipbed          the body (the bed slot's part): box, chipper, tail lights, the CHIPPER
#                        switch and the chipper's running sound
#     csm_chipbox        the crate inside the box (no model; click inside the box to open it)
#       csm_chipper_intake    COLLECTOR: picks up dropped items behind the truck
#       csm_chipper_logs      CRAFTER: a log into two Mulch
#       csm_chipper_brush     CRAFTER: eight sticks into one Mulch
#       csm_chipper_leaves    CRAFTER: four leaves into one Mulch
#
# Every effector is active only while CHIPPER is on. Only the chip bed declares it; an effector
# asking for a variable it does not declare gets its parent's (APart.createComputedVariable). The chipper
# takes whatever falls behind the truck, so switch it off when it is not chipping.

TREECREW = 'treecrew'  # the pack subfolder, activated by csm_parks
MULCH = 'csm:ground_mulch:0:%d'

BED_HALF = 1.36    # half the box's width, inside UNU's bed envelope (x +/-1.40)
BED_FRONT = 1.45
BED_REAR = -1.50
BED_TOP = 1.75

# Texture cells on the chip bed's 64 x 64 texture.
CB_PAINT = (0, 0, 16, 16)
CB_STEEL = (16, 0, 32, 16)
CB_YELLOW = (32, 0, 48, 16)
CB_DARK = (48, 0, 64, 16)
CB_MULCH = (0, 16, 16, 32)
CB_RED = (16, 16, 24, 24)
CB_AMBER = (24, 16, 32, 24)
CB_WHITE = (32, 16, 40, 24)
CB_HAZARD = (48, 16, 64, 32)


def chipbed_obj():
    o = Obj()
    h, f, r, t = BED_HALF, BED_FRONT, BED_REAR, BED_TOP
    o.box('floor', (-h, 0.0, r), (h, 0.10, f), CB_STEEL)
    o.box('load', (-h + 0.08, 0.10, r + 0.08), (h - 0.08, 0.35, f - 0.08), CB_MULCH,
          faces=('u',))
    o.box('side_l', (h - 0.08, 0.10, r), (h, t, f), CB_PAINT)
    o.box('side_r', (-h, 0.10, r), (-h + 0.08, t, f), CB_PAINT)
    o.box('front', (-h + 0.08, 0.10, f - 0.08), (h - 0.08, t + 0.25, f), CB_PAINT)
    o.box('roof', (-h, t, 0.35), (h, t + 0.08, f), CB_PAINT)
    o.box('rail_l', (h - 0.10, t, r), (h, t + 0.06, 0.35), CB_STEEL)
    o.box('rail_r', (-h, t, r), (-h + 0.10, t + 0.06, 0.35), CB_STEEL)
    # the rear doors, shut, with a port at the top the chute blows through
    o.box('door_l', (0.02, 0.10, r), (h - 0.08, t - 0.45, r + 0.06), CB_PAINT)
    o.box('door_r', (-h + 0.08, 0.10, r), (-0.02, t - 0.45, r + 0.06), CB_PAINT)
    o.box('header', (-h + 0.08, t - 0.20, r), (h - 0.08, t, r + 0.06), CB_PAINT)
    # the chipper on the tail: hopper, feed tray, drum housing and the chute up into the box
    o.box('hopper', (-0.55, 0.15, r - 0.85), (0.55, 1.05, r - 0.02), CB_YELLOW)
    o.box('throat', (-0.40, 0.25, r - 1.20), (0.40, 0.75, r - 0.85), CB_DARK,
          faces=('n', 'e', 'w', 'u', 'd'))
    o.box('tray', (-0.60, 0.20, r - 1.45), (0.60, 0.28, r - 0.85), CB_HAZARD)
    o.box('tray_l', (0.52, 0.28, r - 1.45), (0.60, 0.70, r - 0.85), CB_YELLOW)
    o.box('tray_r', (-0.60, 0.28, r - 1.45), (-0.52, 0.70, r - 0.85), CB_YELLOW)
    o.box('drum', (-0.62, 0.35, r - 0.70), (-0.55, 0.95, r - 0.15), CB_STEEL)
    o.box('chute', (-0.12, 1.05, r - 0.55), (0.12, t - 0.05, r - 0.31), CB_YELLOW)
    o.box('chute_top', (-0.12, t - 0.29, r - 0.55), (0.12, t - 0.05, r + 0.30), CB_YELLOW)
    # tail lights in the rear corners
    o.box('&LightBrakeLeft', (h - 0.30, 0.20, r - 0.03), (h - 0.12, 0.40, r), CB_RED,
          faces=('n', 'e', 'w', 'u', 'd'))
    o.box('&LightBrakeRight', (-h + 0.12, 0.20, r - 0.03), (-h + 0.30, 0.40, r), CB_RED,
          faces=('n', 'e', 'w', 'u', 'd'))
    o.box('&LightTurnLeftBack', (h - 0.30, 0.42, r - 0.03), (h - 0.12, 0.54, r), CB_AMBER,
          faces=('n', 'e', 'w', 'u', 'd'))
    o.box('&LightTurnRightBack', (-h + 0.12, 0.42, r - 0.03), (-h + 0.30, 0.54, r), CB_AMBER,
          faces=('n', 'e', 'w', 'u', 'd'))
    o.box('&LightReverse', (h - 0.30, 0.56, r - 0.03), (h - 0.12, 0.64, r), CB_WHITE,
          faces=('n', 'e', 'w', 'u', 'd'))
    return o.text()


def chipbed_texture():
    img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=(46, 88, 58, 255))        # forestry green
    for x in range(0, 16, 4):
        d.line((x, 0, x, 15), fill=(40, 78, 51, 255))           # box ribs
    d.rectangle((16, 0, 31, 15), fill=(150, 154, 160, 255))
    d.line((16, 0, 31, 0), fill=(190, 194, 198, 255))
    d.rectangle((32, 0, 47, 15), fill=(232, 168, 28, 255))      # chipper yellow
    d.line((32, 15, 47, 15), fill=(190, 132, 18, 255))
    d.rectangle((48, 0, 63, 15), fill=(26, 24, 22, 255))
    rng = [(3, 2), (9, 5), (13, 1), (6, 10), (1, 13), (11, 12), (14, 8), (4, 6), (8, 14)]
    d.rectangle((0, 16, 15, 31), fill=(112, 76, 44, 255))       # chips
    for x, y in rng:
        d.point((x, 16 + y), fill=(150, 108, 64, 255))
        d.point(((x + 7) % 16, 16 + (y + 5) % 16), fill=(84, 56, 32, 255))
    d.rectangle((16, 16, 23, 23), fill=(140, 20, 16, 255))
    d.rectangle((24, 16, 31, 23), fill=(150, 92, 12, 255))
    d.rectangle((32, 16, 39, 23), fill=(170, 172, 176, 255))
    # hazard stripes, drawn on their own tile so the diagonals cannot spill into a neighbour
    stripes = Image.new('RGBA', (16, 16), (232, 168, 28, 255))
    sd = ImageDraw.Draw(stripes)
    for k in range(-16, 16, 6):
        sd.polygon([(k, 15), (k + 3, 15), (k + 18, 0), (k + 15, 0)], fill=(24, 24, 24, 255))
    img.paste(stripes, (48, 16))
    return img


def chipbed_icon():
    def draw(d):
        d.rectangle((1, 4, 11, 12), fill=(46, 88, 58, 255))
        d.rectangle((1, 4, 11, 4), fill=(150, 154, 160, 255))
        d.rectangle((11, 7, 14, 12), fill=(232, 168, 28, 255))
        d.rectangle((12, 3, 13, 7), fill=(232, 168, 28, 255))
        d.rectangle((1, 13, 14, 13), fill=(60, 60, 64, 255))
    return icon(draw)


def tail_light(name, colour, animations, pos):
    return {'objectName': name, 'emissive': True, 'isElectric': True, 'color': colour,
            'brightnessAnimations': animations,
            'blendableComponents': [flare(pos, [0, 0, -1], 0.35)]}


def chipbed_json():
    r = BED_REAR - 0.03
    brake = [{'animationType': 'translation', 'axis': [0, 0.5, 0], 'variable': 'running_light'},
             {'animationType': 'translation', 'axis': [0, 1, 0], 'variable': 'brake'}]
    return {
        'definitions': [{'name': 'CSM Tree Crew Chip Bed', 'subName': '',
                         'extraMaterialLists': [[], []]}],
        'general': {
            'description': 'A chip box with a chipper on its tail, for a UNU Contractor\'s bed. '
                           'Switch CHIPPER on and the chipper takes the logs, brush and leaves '
                           'dropped behind the truck and blows them into the box as Mulch. It '
                           'takes anything else dropped there too, so switch it off after.',
            'stackSize': 1,
            'materialLists': [
                ['minecraft:iron_ingot:0:12', 'minecraft:iron_block:0:1', 'minecraft:piston:0:2'],
                ['minecraft:iron_ingot:12', 'minecraft:iron_block:1', 'minecraft:piston:2']],
        },
        'generic': {'type': 'generic_bodypart_unu_truckbed_contractor', 'width': 2.7,
                    'height': 1.8},
        'subParts': [{
            'pos': [0, 0.1, 0],
            'types': ['interactable_csm_chipbox'],
            'defaultPart': '%s:csm_chipbox' % PACK,
            'allowParentRemoval': True,
        }],
        'rendering': {
            'customVariables': ['CHIPPER'],
            'sounds': [{'name': '%s:chipper_run' % PACK, 'looping': True,
                        'activeAnimations': [visible_on('CHIPPER')]}],
            'lightObjects': [
                tail_light('&LightBrakeLeft', '#FF0000', brake, [BED_HALF - 0.21, 0.30, r]),
                tail_light('&LightBrakeRight', '#FF0000', brake, [-BED_HALF + 0.21, 0.30, r]),
                tail_light('&LightTurnLeftBack', '#FFA000',
                           [visible_on('left_turn_signal'), lit_by('0_10_10_cycle')],
                           [BED_HALF - 0.21, 0.48, r]),
                tail_light('&LightTurnRightBack', '#FFA000',
                           [visible_on('right_turn_signal'), lit_by('0_10_10_cycle')],
                           [-BED_HALF + 0.21, 0.48, r]),
                tail_light('&LightReverse', '#FFFFFF',
                           [lit_by('transmission_reversed')], [BED_HALF - 0.21, 0.60, r]),
            ],
        },
    }


# The collector's reach, behind the feed tray: logs dropped there are taken in.
INTAKE_POS = [0, -0.5, BED_REAR - 2.4]


def effector_box(pos, width, height):
    return [{'collisionTypes': ['effector'], 'isInterior': True,
             'collisions': [{'pos': pos, 'width': width, 'height': height}]}]


def chipbox_json():
    mills = [('csm_chipper_logs', 'effector_csm_chipper_logs'),
             ('csm_chipper_brush', 'effector_csm_chipper_brush'),
             ('csm_chipper_leaves', 'effector_csm_chipper_leaves')]
    sub = [{'pos': INTAKE_POS, 'types': ['effector_csm_chipper_intake'],
            'defaultPart': '%s:csm_chipper_intake' % PACK, 'allowParentRemoval': True}]
    for name, slot in mills:
        sub.append({'pos': [0, 0.5, BED_REAR - 0.4], 'types': [slot],
                    'defaultPart': '%s:%s' % (PACK, name), 'allowParentRemoval': True})
    return {
        'definitions': [{'name': 'CSM Chip Box', 'subName': '', 'extraMaterialLists': [[], []]}],
        'general': {'description': 'What the chip bed holds. Comes with the chip bed.',
                    'stackSize': 1, 'materialLists': [['minecraft:chest:0:1'],
                                                      ['minecraft:chest:1']]},
        'generic': {'type': 'interactable_csm_chipbox', 'width': 2.4, 'height': 1.4},
        'interactable': {'interactionType': 'crate', 'inventoryUnits': 6,
                         'feedsVehicles': True},
        'subParts': sub,
        'rendering': {'modelType': 'none'},
    }


def effector_json(name, label, effector, box, sound=None):
    # CHIPPER is not declared here: an undeclared variable is looked up on the parents, so
    # every effector reads the chip bed's one switch.
    rendering = {'modelType': 'none'}
    if sound:
        rendering['sounds'] = [{'name': '%s:%s' % (PACK, sound),
                                'activeAnimations': [visible_on('effector_operated')]}]
    return {
        'definitions': [{'name': label, 'subName': '', 'extraMaterialLists': [[], []]}],
        'general': {'description': 'Part of the chip bed\'s chipper. Comes with the chip bed.',
                    'stackSize': 1, 'materialLists': [['minecraft:iron_ingot:0:1'],
                                                      ['minecraft:iron_ingot:1']]},
        'generic': {'type': 'effector_' + name, 'width': 0.25, 'height': 0.25,
                    'activeAnimations': [visible_on('CHIPPER')]},
        'effector': effector,
        'collisionGroups': box,
        'rendering': rendering,
    }


def treecrew_parts():
    """The chip bed's parts, as {name: definition}."""
    return {
        'csm_chipbed': chipbed_json(),
        'csm_chipbox': chipbox_json(),
        'csm_chipper_intake': effector_json(
            'csm_chipper_intake', 'CSM Chipper Intake',
            {'type': 'collector', 'operationDelay': 4},
            effector_box([0, 0, 0], 2.2, 1.4)),
        'csm_chipper_logs': effector_json(
            'csm_chipper_logs', 'CSM Chipper (Logs)',
            {'type': 'crafter', 'operationDelay': 10,
             'crafterInputs': ['oredict:logWood:1'], 'crafterOutputs': [MULCH % 2]},
            effector_box([0, 0, 0], 0.5, 0.5), sound='chipper_chip'),
        'csm_chipper_brush': effector_json(
            'csm_chipper_brush', 'CSM Chipper (Brush)',
            {'type': 'crafter', 'operationDelay': 10,
             'crafterInputs': ['minecraft:stick:0:8'], 'crafterOutputs': [MULCH % 1]},
            effector_box([0, 0, 0], 0.5, 0.5), sound='chipper_chip'),
        'csm_chipper_leaves': effector_json(
            'csm_chipper_leaves', 'CSM Chipper (Leaves)',
            {'type': 'crafter', 'operationDelay': 10,
             'crafterInputs': ['oredict:treeLeaves:4'], 'crafterOutputs': [MULCH % 1]},
            effector_box([0, 0, 0], 0.5, 0.5)),
    }


def pack_definition():
    return {
        'packID': PACK,
        'packName': 'CSM: Vehicles',
        'packItem': 'csm_lightbar_led_redblue',
        'fileStructure': 0,
        # The emergency parts load always; the tree crew only with CSM: Parks & Greenery, since
        # the chipper's output is Parks' Mulch.
        'activators': {'': [], TREECREW: ['csm_parks']},
    }


# ------------------------------------------------------------------------------------------
# Catalogue and writing
# ------------------------------------------------------------------------------------------

def png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, 'PNG', optimize=True)
    return buf.getvalue()


def json_bytes(obj):
    return (json.dumps(obj, indent=2) + '\n').encode('utf-8')


def item_model(name):
    return json_bytes({'parent': 'minecraft:item/generated',
                       'textures': {'layer0': '%s:items/parts/%s' % (PACK, name)}})


def catalogue():
    """Every file the pack holds, as {path: bytes}."""
    files = {}

    def part(name, definition, texture, item_icon):
        files[os.path.join(PACK_DIR, 'jsondefs', 'parts', name + '.json')] = \
            json_bytes(definition)
        files[os.path.join(PACK_DIR, 'textures', 'parts', name + '.png')] = png_bytes(texture)
        files[os.path.join(PACK_DIR, 'textures', 'items', 'parts', name + '.png')] = \
            png_bytes(item_icon)
        files[os.path.join(ROOT, 'mts', 'models', 'item', '%s.%s.json' % (PACK, name))] = \
            item_model(name)

    files[os.path.join(PACK_DIR, 'objmodels', 'parts', 'csm_lightbar_led.obj')] = \
        lightbar_obj().encode('utf-8')
    for key in SCHEMES:
        colours = SCHEMES[key][1]
        part('csm_lightbar_led_' + key, lightbar_json(key), scheme_texture(colours),
             lightbar_icon(colours))

    files[os.path.join(PACK_DIR, 'objmodels', 'parts', 'csm_preempt_emitter.obj')] = \
        emitter_obj().encode('utf-8')
    part('csm_preempt_emitter', emitter_json(), base_texture(), emitter_icon())

    files[os.path.join(PACK_DIR, 'objmodels', 'parts', 'csm_siren_speaker.obj')] = \
        siren_obj().encode('utf-8')
    part('csm_siren_speaker', siren_json(), base_texture(), siren_icon())

    # The tree crew, under the activated subfolder. Models and textures stay at the pack's
    # top level: Immersive Vehicles resolves them from the pack id, not the JSON's folder.
    files[os.path.join(PACK_DIR, 'objmodels', 'parts', 'csm_chipbed.obj')] = \
        chipbed_obj().encode('utf-8')
    for name, definition in treecrew_parts().items():
        files[os.path.join(PACK_DIR, TREECREW, 'jsondefs', 'parts', name + '.json')] = \
            json_bytes(definition)
        files[os.path.join(ROOT, 'mts', 'models', 'item', '%s.%s.json' % (PACK, name))] = \
            item_model(name)
        files[os.path.join(PACK_DIR, 'textures', 'items', 'parts', name + '.png')] = \
            png_bytes(chipbed_icon())
    files[os.path.join(PACK_DIR, 'textures', 'parts', 'csm_chipbed.png')] = \
        png_bytes(chipbed_texture())
    files[os.path.join(PACK_DIR, 'packdefinition.json')] = json_bytes(pack_definition())
    return files


def same(path, data):
    """Whether the file in the tree already holds `data`. Text is compared with line endings
    ignored: Git's core.autocrlf checks every text file out as CRLF on Windows, and a byte compare
    would call the whole pack drifted on a tree nobody touched (see csm_layout's
    same_generated_text)."""
    if not os.path.exists(path):
        return False
    with open(path, 'rb') as fh:
        old = fh.read()
    if path.endswith('.png'):
        return old == data
    return old.replace(b'\r\n', b'\n') == data.replace(b'\r\n', b'\n')


def main():
    check = '--check' in sys.argv
    files = catalogue()
    drift = []
    for path, data in sorted(files.items()):
        if same(path, data):
            continue
        if check:
            drift.append(path)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'wb') as fh:
            fh.write(data)
        print('wrote', path)
    if check:
        if drift:
            print('out of date (re-run without --check):')
            for p in drift:
                print('  ' + p)
            sys.exit(1)
        print('vehicle parts are up to date (%d files)' % len(files))


if __name__ == '__main__':
    main()
