"""The CSM: Vehicles fleet: our own fire engine, ambulance and police SUV for Immersive Vehicles,
and the engines, wheels and seat they come with.

Written into the same Immersive Vehicles pack as gen_vehicle_parts.py (pack id csmvehicles), from
one catalogue, so every vehicle spawns ready to drive with nothing but Immersive Vehicles
installed: its wheels, seats and engine are this pack's parts, set as default parts, and it spawns
fuelled.

Each vehicle is a spec of boxes, each wearing a named cell of the vehicle's 128 x 128 texture.
Liveries are only textures: the same cells painted differently, one PNG per livery (Immersive
Vehicles' `<model><subName>.png`), so one OBJ serves every livery. Lamps are OBJ objects named
with a leading '&' and listed in `rendering.lightObjects` from the same spec, so the model and
its lights cannot disagree:

  - the emergency lights flash on EMERLTS, the variable IV's own pack and UNU use, which is also
    what CSM: Vehicles' IvPreemptSource looks for: a fleet vehicle running its lights towards an
    intersection with a CSM preempt detector calls the emergency preempt;
  - the siren's three tones are the siren speaker's (`siren`, `siren_yelp`, `siren_hilo`), so the
    default panel's four custom switches are the lights and the three tones;
  - headlights, brake, turn and reverse lights run on IV's own variables.

Immersive Vehicles' coordinates: +z forward, +y up, +x the vehicle's LEFT. A wheel slot's pos is
its axle; the vehicle settles on its wheels, so here the rear axle is the origin and the ground is
at y = -(wheel radius). Wheel and engine slots carry minValue/maxValue around the part's height
or fuel consumption, without which IV rejects the part (default parts included); the engine's
linkedParts are the driven wheels' 1-based slot numbers.

Writes under modules/vehicles/src/main/resources/assets/: csmvehicles/jsondefs/{vehicles,parts}/,
csmvehicles/objmodels/{vehicles,parts}/, csmvehicles/textures/{vehicles,parts}/,
csmvehicles/textures/items/{vehicles,parts}/ and mts/models/item/. The engine and horn sounds are
gen_vehicle_sounds.py's. Run from the repo root:

    python dev-env-utils/scripts/gen_vehicle_fleet.py           # write
    python dev-env-utils/scripts/gen_vehicle_fleet.py --check   # exit 1 if the tree has drifted
"""

import math
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_vehicle_parts as vp  # noqa: E402
import life_safety_gen_common as lc  # noqa: E402

PACK = vp.PACK
ROOT = vp.ROOT
PACK_DIR = vp.PACK_DIR
T = 128  # vehicle textures are 128 x 128, cells 16 px


def cell(cx, cy, w=1, h=1):
    return (cx * 16, cy * 16, (cx + w) * 16, (cy + h) * 16)


# Named cells of every vehicle texture.
CELLS = {
    'paint': cell(0, 0), 'paint2': cell(1, 0), 'stripe': cell(2, 0), 'chrome': cell(3, 0),
    'black': cell(4, 0), 'glass': cell(5, 0), 'interior': cell(6, 0), 'door': cell(7, 0),
    'grille': cell(0, 1), 'headlight': cell(1, 1), 'tail': cell(2, 1), 'amber': cell(3, 1),
    'white': cell(4, 1), 'lamp_red': cell(5, 1), 'lamp_blue': cell(6, 1),
    'lamp_white': cell(7, 1), 'decal': cell(0, 2, 4, 1), 'emitter': cell(4, 2),
    'hosebed': cell(5, 2), 'plate': cell(6, 2), 'rubber': cell(7, 2),
    'lamp_amber': cell(0, 3), 'bed': cell(1, 3), 'bucket': cell(2, 3), 'blade': cell(3, 3),
    'sign': cell(4, 3, 4, 1),
}

LAMP_LIT = {'lamp_red': '#FF1E10', 'lamp_blue': '#1E46FF', 'lamp_white': '#EEF4FF',
            'lamp_amber': '#FFA000',
            'emitter': '#DCEBFF'}


# ------------------------------------------------------------------------------------------
# Building a vehicle
# ------------------------------------------------------------------------------------------

class Vehicle:
    """A vehicle's mesh, lights and icon outline, built box by box."""

    def __init__(self):
        self.obj = vp.Obj(tex=T)
        self.lights = []      # (object name, light definition)
        self.outline = []     # (lo, hi, cell name) for the item icon
        self.count = 0

    def box(self, lo, hi, name, faces=('n', 's', 'e', 'w', 'u', 'd'), obj=None):
        self.count += 1
        self.obj.box(obj or 'p%d_%s' % (self.count, name), lo, hi, CELLS[name], faces)
        self.outline.append((lo, hi, name))

    def group(self, obj, boxes):
        """Boxes `(lo, hi, cell name)` as one OBJ object named `obj`, which IV animates as one."""
        for i, (lo, hi, name) in enumerate(boxes):
            self.obj.box(obj if i == 0 else None, lo, hi, CELLS[name])
            self.outline.append((lo, hi, name))

    def both(self, lo, hi, name, faces=('n', 's', 'e', 'w', 'u', 'd')):
        """The box and its mirror image across x = 0 (left and right)."""
        self.box(lo, hi, name, faces)
        self.box((-hi[0], lo[1], lo[2]), (-lo[0], hi[1], hi[2]), name, faces)

    def lamp(self, name, lo, hi, cell_name, colour, animations, axis, size=0.35,
             faces=('n', 's', 'e', 'w', 'u', 'd')):
        self.box(lo, hi, cell_name, faces, obj='&' + name)
        centre = [round((lo[k] + hi[k]) / 2, 4) for k in range(3)]
        light = {'objectName': '&' + name, 'emissive': True, 'isElectric': True,
                 'color': colour, 'brightnessAnimations': animations}
        if axis:
            light['blendableComponents'] = [vp.flare(centre, axis, size)]
        self.lights.append(light)

    def lightbar(self, x_half, y, z, colours, variable='EMERLTS', emitter=True):
        """A roof lightbar: eight modules flashing in pairs, left then right, the emitter in the
        middle. `colours` are eight lamp cells, left (+x) to right. Emergency vehicles run it on
        EMERLTS and carry the emitter; work trucks run amber on their own switch (BEACONS), which
        is not an emergency switch, so they never preempt a signal."""
        depth = 0.16
        self.box((-x_half, y, z - depth), (x_half, y + 0.05, z + depth), 'black')
        width = (2 * x_half - 0.14) / 8
        for i, colour in enumerate(colours):
            left = x_half - 0.02 - i * width - (0.10 if i >= 4 else 0)
            flashes = vp.LEFT_FLASHES if i < 4 else vp.RIGHT_FLASHES
            self.lamp('Lamp_%d' % (i + 1), (left - width + 0.01, y + 0.05, z - depth + 0.01),
                      (left - 0.01, y + 0.15, z + depth - 0.01), colour, LAMP_LIT[colour],
                      [vp.visible_on(variable)] + [vp.lit_by(f) for f in flashes],
                      [0, 0, 1], 0.45)
        if not emitter:
            return
        self.lamp('Emitter', (-0.03, y + 0.07, z + depth - 0.01), (0.03, y + 0.13, z + depth + 0.01),
                  'emitter', LAMP_LIT['emitter'],
                  [vp.visible_on('EMERLTS'), vp.lit_by(vp.EMITTER_FLASH)], [0, 0, 1], 0.3,
                  faces=('s', 'e', 'w', 'u', 'd'))

    def warning(self, name, lo, hi, colour, phase, axis, variable='EMERLTS'):
        """A body-mounted warning lamp, flashing on its own phase of the lightbar's 16 ticks."""
        cycle = ['0_5_11_cycle', '8_5_3_cycle'][phase]
        self.lamp(name, lo, hi, colour, LAMP_LIT[colour],
                  [vp.visible_on(variable), vp.lit_by(cycle)], axis)

    def road_lights(self, front_z, rear_z, x_out, head_y, tail_y):
        """Headlights, tail and brake lights, turn signals and reversing lights."""
        f, r = front_z, rear_z
        self.lamp('HeadlightL', (x_out - 0.35, head_y, f), (x_out - 0.05, head_y + 0.2, f + 0.03),
                  'headlight', '#FFF6E0', [vp.visible_on('headlight')], [0, 0, 1], 0.6,
                  faces=('s', 'e', 'w', 'u', 'd'))
        self.lamp('HeadlightR', (-x_out + 0.05, head_y, f), (-x_out + 0.35, head_y + 0.2, f + 0.03),
                  'headlight', '#FFF6E0', [vp.visible_on('headlight')], [0, 0, 1], 0.6,
                  faces=('s', 'e', 'w', 'u', 'd'))
        brake = [{'animationType': 'translation', 'axis': [0, 0.5, 0],
                  'variable': 'running_light'},
                 {'animationType': 'translation', 'axis': [0, 1, 0], 'variable': 'brake'}]
        back = ('n', 'e', 'w', 'u', 'd')
        for side, sx in (('L', 1), ('R', -1)):
            xs = sorted((sx * (x_out - 0.05), sx * (x_out - 0.30)))
            self.lamp('Brake' + side, (xs[0], tail_y, r - 0.03), (xs[1], tail_y + 0.2, r),
                      'tail', '#FF0000', brake, [0, 0, -1], faces=back)
            self.lamp('Turn' + side, (xs[0], tail_y + 0.22, r - 0.03),
                      (xs[1], tail_y + 0.34, r), 'amber', '#FFA000',
                      [vp.visible_on('left_turn_signal' if sx > 0 else 'right_turn_signal'),
                       vp.lit_by('10_10_0_cycle')], [0, 0, -1], faces=back)
            fxs = sorted((sx * (x_out - 0.05), sx * (x_out - 0.25)))
            self.lamp('TurnFront' + side, (fxs[0], head_y - 0.16, f), (fxs[1], head_y - 0.04, f + 0.03),
                      'amber', '#FFA000',
                      [vp.visible_on('left_turn_signal' if sx > 0 else 'right_turn_signal'),
                       vp.lit_by('10_10_0_cycle')], [0, 0, 1], faces=('s', 'e', 'w', 'u', 'd'))
        self.lamp('Reverse', (-0.15, tail_y, r - 0.03), (0.15, tail_y + 0.1, r), 'white', '#FFFFFF',
                  [vp.lit_by('engine_reversed_1')], [0, 0, -1], faces=back)


def fire_engine():
    """A custom-cab pumper: tilt cab, compartments with roll-up doors, a hose bed, a pump panel."""
    v = Vehicle()
    g = -0.55                                  # the ground, below the axle
    w = 1.25
    v.box((-0.5, -0.15, -2.3), (0.5, 0.25, 6.9), 'black')                      # frame
    v.box((-w, 0.25, 3.65), (w, 2.55, 6.75), 'paint')                          # cab
    v.box((-w + 0.05, 2.55, 3.7), (w - 0.05, 2.62, 6.7), 'paint2')             # white roof
    v.box((-1.15, 1.55, 6.75), (1.15, 2.45, 6.77), 'glass', faces=('s',))     # windshield
    for z0, z1 in ((4.65, 6.55), (3.75, 4.55)):
        v.both((w, 1.55, z0), (w + 0.02, 2.4, z1), 'glass', faces=('e', 'w', 'u', 'd', 'n', 's'))
    v.both((w, 0.6, 4.75), (w + 0.025, 1.0, 6.4), 'decal', faces=('e', 'w'))  # door lettering
    v.box((-0.7, 0.45, 6.75), (0.7, 1.35, 6.79), 'grille', faces=('s',))
    v.box((-1.3, -0.05, 6.75), (1.3, 0.35, 7.05), 'chrome')                    # front bumper
    v.both((w, 1.9, 6.4), (w + 0.25, 2.3, 6.5), 'chrome')                      # mirrors
    # the body: compartments with roll-up doors, the pump panel, the hose bed
    v.box((-w, 0.25, -2.3), (w, 2.35, 3.6), 'paint')
    for z0, z1 in ((-2.2, -0.9), (-0.8, 0.9), (1.0, 2.4)):
        v.both((w, 0.4, z0), (w + 0.01, 2.2, z1), 'door', faces=('e', 'w'))
    v.both((w, 0.5, 2.5), (w + 0.02, 2.2, 3.5), 'chrome', faces=('e', 'w'))   # pump panel
    v.both((w + 0.025, 1.05, -2.3), (w + 0.035, 1.25, 3.6), 'stripe', faces=('e', 'w'))
    v.both((w + 0.025, 1.05, 3.65), (w + 0.03, 1.25, 4.6), 'stripe', faces=('e', 'w'))
    v.box((-w + 0.1, 2.35, -2.2), (w - 0.1, 2.5, 2.4), 'hosebed', faces=('u',))
    v.box((-w, 2.35, -2.3), (w, 2.6, -2.2), 'chrome')                          # rear rail
    v.both((w - 0.1, 2.35, -2.3), (w, 2.6, 2.4), 'chrome')                     # side rails
    v.box((-1.3, -0.1, -2.6), (1.3, 0.2, -2.3), 'chrome')                      # rear step
    v.both((w - 0.2, 0.0, 3.0), (w, 0.25, 3.6), 'black')                       # cab step
    v.box((-0.5, 0.05, -2.31), (0.5, 0.35, -2.3), 'plate', faces=('n',))
    # lights
    v.lightbar(1.05, 2.62, 6.35, ['lamp_red', 'lamp_white', 'lamp_red', 'lamp_red',
                                  'lamp_red', 'lamp_red', 'lamp_white', 'lamp_red'])
    for side, sx in (('L', 1), ('R', -1)):
        xs = sorted((sx * w, sx * (w - 0.2)))
        v.warning('Beacon' + side, (xs[0], 2.6, -2.25), (xs[1], 2.8, -2.05), 'lamp_red',
                  0 if sx > 0 else 1, [0, 0, -1])
        sxs = sorted((sx * w, sx * (w + 0.03)))
        v.warning('Side' + side, (sxs[0], 1.5, 3.62), (sxs[1], 1.7, 3.64), 'lamp_red',
                  1 if sx > 0 else 0, [sx, 0, 0])
        fx = sorted((sx * 0.75, sx * 0.95))
        v.warning('Grille' + side, (fx[0], 1.0, 6.75), (fx[1], 1.15, 6.78), 'lamp_red',
                  0 if sx > 0 else 1, [0, 0, 1])
    v.road_lights(6.75, -2.3, w, 0.7, 0.45)
    return v, g


def ambulance():
    """A Type III ambulance: a van cab and a modular box behind it."""
    v = Vehicle()
    g = -0.4
    v.box((-0.45, -0.1, -1.95), (0.45, 0.2, 5.1), 'black')
    # the van cab and its hood
    v.box((-1.0, 0.2, 2.95), (1.0, 2.05, 4.0), 'paint2')
    v.box((-0.95, 0.2, 4.0), (0.95, 1.0, 5.0), 'paint2')
    v.box((-0.9, 1.05, 4.0), (0.9, 1.95, 4.02), 'glass', faces=('s',))
    v.both((1.0, 1.1, 3.05), (1.02, 1.9, 3.9), 'glass')
    v.box((-0.6, 0.35, 5.0), (0.6, 0.85, 5.02), 'grille', faces=('s',))
    v.box((-1.0, 0.05, 5.0), (1.0, 0.3, 5.2), 'black')
    v.both((1.0, 1.4, 3.85), (1.2, 1.75, 3.95), 'black')
    # the patient module
    w = 1.15
    v.box((-w, 0.25, -1.95), (w, 2.6, 2.9), 'paint2')
    v.both((w + 0.01, 0.9, -1.95), (w + 0.02, 1.2, 2.9), 'stripe', faces=('e', 'w'))
    v.both((1.0, 0.9, 2.95), (1.01, 1.2, 4.0), 'stripe', faces=('e', 'w'))
    v.both((w + 0.01, 1.35, -0.4), (w + 0.02, 1.85, 1.1), 'decal', faces=('e', 'w'))
    v.both((w, 0.35, 1.2), (w + 0.01, 2.3, 2.6), 'door', faces=('e', 'w'))   # side entry
    v.box((-1.0, 1.4, -1.97), (1.0, 2.2, -1.95), 'glass', faces=('n',))    # rear windows
    v.box((-0.02, 0.3, -1.97), (0.02, 2.3, -1.95), 'black', faces=('n',))
    v.box((-1.2, -0.05, -2.2), (1.2, 0.2, -1.95), 'chrome')
    v.box((-0.4, 0.3, -1.96), (0.4, 0.5, -1.95), 'plate', faces=('n',))
    # lights: the cab's bar, and the module's corner lamps
    v.lightbar(0.85, 2.05, 3.6, ['lamp_red', 'lamp_white', 'lamp_red', 'lamp_red',
                                 'lamp_red', 'lamp_red', 'lamp_white', 'lamp_red'])
    for side, sx in (('L', 1), ('R', -1)):
        xs = sorted((sx * w, sx * (w - 0.3)))
        v.warning('FrontCorner' + side, (xs[0], 2.35, 2.9), (xs[1], 2.55, 2.93), 'lamp_red',
                  0 if sx > 0 else 1, [0, 0, 1])
        v.warning('RearCorner' + side, (xs[0], 2.35, -1.98), (xs[1], 2.55, -1.95), 'lamp_red',
                  1 if sx > 0 else 0, [0, 0, -1])
        sx0 = sorted((sx * w, sx * (w + 0.03)))
        v.warning('Side' + side, (sx0[0], 2.3, 1.0), (sx0[1], 2.5, 1.4), 'lamp_red',
                  0 if sx > 0 else 1, [sx, 0, 0])
    v.road_lights(5.0, -1.95, 0.95, 0.55, 0.5)
    return v, g


def police_suv():
    """A police utility vehicle: two-tone body, push bumper, a low lightbar."""
    v = Vehicle()
    g = -0.39
    w = 1.0
    v.box((-w, -0.05, -1.05), (w, 0.75, 4.1), 'paint2')                       # lower body
    v.box((-w + 0.02, 0.75, 2.9), (w - 0.02, 0.95, 4.1), 'paint')             # hood
    v.box((-w + 0.02, 0.75, -1.0), (w - 0.02, 0.95, -0.5), 'paint')           # tailgate top
    # the greenhouse: pillars, glass and roof
    v.box((-0.95, 0.95, -0.95), (0.95, 1.55, 2.85), 'interior', faces=('u', 'd'))
    v.box((-0.9, 0.97, 2.85), (0.9, 1.5, 2.87), 'glass', faces=('s',))
    v.box((-0.9, 1.0, -0.97), (0.9, 1.5, -0.95), 'glass', faces=('n',))
    v.both((0.95, 1.0, -0.9), (0.96, 1.5, 2.8), 'glass', faces=('e', 'w'))
    for z0, z1 in ((2.75, 2.9), (1.2, 1.3), (-0.95, -0.8)):
        v.both((0.9, 0.95, z0), (0.97, 1.55, z1), 'paint')
    v.box((-0.97, 1.55, -0.97), (0.97, 1.62, 2.9), 'paint')                   # roof
    v.both((w, 0.2, 0.3), (w + 0.015, 0.6, 2.4), 'decal', faces=('e', 'w'))
    v.both((w, 0.62, -1.0), (w + 0.01, 0.68, 4.05), 'stripe', faces=('e', 'w'))
    v.box((-0.7, 0.4, 4.1), (0.7, 0.7, 4.12), 'grille', faces=('s',))
    v.box((-0.85, -0.05, 4.1), (0.85, 0.45, 4.35), 'black')                   # push bumper
    v.box((-0.85, 0.45, 4.25), (-0.7, 0.85, 4.35), 'black')
    v.box((0.7, 0.45, 4.25), (0.85, 0.85, 4.35), 'black')
    v.both((w, 1.0, 2.6), (w + 0.18, 1.2, 2.7), 'black')                      # mirrors
    v.box((-0.35, 0.2, -1.06), (0.35, 0.4, -1.05), 'plate', faces=('n',))
    v.lightbar(0.8, 1.62, 1.6, ['lamp_red', 'lamp_red', 'lamp_white', 'lamp_red',
                                'lamp_blue', 'lamp_white', 'lamp_blue', 'lamp_blue'])
    for side, sx, col in (('L', 1, 'lamp_red'), ('R', -1, 'lamp_blue')):
        xs = sorted((sx * 0.3, sx * 0.5))
        v.warning('Grille' + side, (xs[0], 0.5, 4.12), (xs[1], 0.62, 4.14), col,
                  0 if sx > 0 else 1, [0, 0, 1])
        v.warning('Rear' + side, (xs[0], 1.4, -0.98), (xs[1], 1.48, -0.96), col,
                  1 if sx > 0 else 0, [0, 0, -1])
    v.road_lights(4.1, -1.05, w, 0.5, 0.45)
    return v, g


# The aerial's deployment, on the one AERIAL switch: raise, then swing to the left, then extend;
# stowing runs it backwards (retract, swing back, lower). Ticks.
AERIAL_RAISE = (0, 100, 200)     # forwards delay, duration, reverse delay
AERIAL_TURN = (100, 80, 120)
AERIAL_EXTEND = (180, 120, 0)
AERIAL_PIVOT = (0.0, 2.95, -2.2)  # where the ladder's heel pins to the turntable
AERIAL_REACH = 8.0               # how far the fly section runs out


def aerial_animation(kind, axis, timing, centre=None, variable='AERIAL'):
    """One step of a deployment on a 0-1 switch: `timing` is (forwards delay, duration, reverse
    delay) in ticks, so one switch runs several steps in order and back."""
    forwards, duration, reverse = timing
    anim = {'animationType': kind, 'variable': variable, 'axis': axis, 'duration': duration,
            'forwardsDelay': forwards, 'reverseDelay': reverse}
    if centre is not None:
        anim['centerPoint'] = list(centre)
    return anim


def ladder_truck():
    """A rear-mount aerial ladder: the pumper's cab, a lower body, a turntable at the back and a
    two-section ladder bedded forward over the cab, which the AERIAL switch raises to 60
    degrees, swings to the left and runs out 8 m."""
    v = Vehicle()
    g = -0.55
    w = 1.25
    v.box((-0.5, -0.15, -3.2), (0.5, 0.25, 8.0), 'black')                      # frame
    v.box((-w, 0.25, 4.65), (w, 2.55, 7.75), 'paint')                          # cab
    v.box((-w + 0.05, 2.55, 4.7), (w - 0.05, 2.62, 7.7), 'paint2')
    v.box((-1.15, 1.55, 7.75), (1.15, 2.45, 7.77), 'glass', faces=('s',))
    for z0, z1 in ((5.65, 7.55), (4.75, 5.55)):
        v.both((w, 1.55, z0), (w + 0.02, 2.4, z1), 'glass')
    v.both((w, 0.6, 5.75), (w + 0.025, 1.0, 7.4), 'decal', faces=('e', 'w'))
    v.box((-0.7, 0.45, 7.75), (0.7, 1.35, 7.79), 'grille', faces=('s',))
    v.box((-1.3, -0.05, 7.75), (1.3, 0.35, 8.05), 'chrome')
    v.both((w, 1.9, 7.4), (w + 0.25, 2.3, 7.5), 'chrome')
    # the body, lower than the pumper's so the ladder clears the cab
    v.box((-w, 0.25, -3.2), (w, 2.0, 4.6), 'paint')
    for z0, z1 in ((-3.1, -1.6), (-1.5, 0.6), (0.7, 2.6), (2.7, 4.5)):
        v.both((w, 0.4, z0), (w + 0.01, 1.85, z1), 'door', faces=('e', 'w'))
    v.both((w + 0.025, 1.0, -3.2), (w + 0.035, 1.18, 4.6), 'stripe', faces=('e', 'w'))
    v.box((-w + 0.05, 2.0, -3.15), (w - 0.05, 2.05, 4.55), 'black', faces=('u',))
    v.both((w + 0.05, 0.0, -3.0), (w + 0.6, 0.15, -2.6), 'chrome')            # outriggers
    v.box((-0.25, 2.62, 7.0), (0.25, 2.95, 7.2), 'chrome')                     # ladder rest
    v.box((-1.3, -0.1, -3.5), (1.3, 0.2, -3.2), 'chrome')
    v.box((-0.5, 0.05, -3.21), (0.5, 0.35, -3.2), 'plate', faces=('n',))
    # the aerial: turntable, ladder (base section) and fly, each one animated object
    px, py, pz = AERIAL_PIVOT
    v.group('turntable', [((-0.85, 2.0, pz - 0.85), (0.85, 2.3, pz + 0.85), 'chrome'),
                          ((-0.35, 2.3, pz - 0.35), (0.35, 2.85, pz + 0.35), 'paint'),
                          ((0.45, 2.3, pz - 0.3), (0.8, 3.0, pz + 0.3), 'paint2')])
    length = 9.8
    base = [((0.42, py, pz), (0.5, py + 0.3, pz + length), 'chrome'),
            ((-0.5, py, pz), (-0.42, py + 0.3, pz + length), 'chrome'),
            ((0.42, py + 0.3, pz), (0.5, py + 0.34, pz + length), 'paint'),
            ((-0.5, py + 0.3, pz), (-0.42, py + 0.34, pz + length), 'paint')]
    for k in range(int(length / 0.5)):
        z = pz + 0.25 + k * 0.5
        base.append(((-0.42, py + 0.02, z), (0.42, py + 0.05, z + 0.05), 'chrome'))
    v.group('ladder_base', base)
    fly = [((0.33, py + 0.04, pz + 0.4), (0.4, py + 0.28, pz + length - 0.1), 'chrome'),
           ((-0.4, py + 0.04, pz + 0.4), (-0.33, py + 0.28, pz + length - 0.1), 'chrome')]
    for k in range(int((length - 0.6) / 0.5)):
        z = pz + 0.65 + k * 0.5
        fly.append(((-0.33, py + 0.08, z), (0.33, py + 0.11, z + 0.05), 'chrome'))
    fly.append(((-0.42, py + 0.04, pz + length - 0.15), (0.42, py + 0.32, pz + length),
                'paint2'))                                                     # the tip
    v.group('ladder_fly', fly)
    # lights
    v.lightbar(1.05, 2.62, 7.35, ['lamp_red', 'lamp_white', 'lamp_red', 'lamp_red',
                                  'lamp_red', 'lamp_red', 'lamp_white', 'lamp_red'])
    for side, sx in (('L', 1), ('R', -1)):
        xs = sorted((sx * w, sx * (w - 0.2)))
        v.warning('Beacon' + side, (xs[0], 2.0, -3.15), (xs[1], 2.2, -2.95), 'lamp_red',
                  0 if sx > 0 else 1, [0, 0, -1])
        sxs = sorted((sx * w, sx * (w + 0.03)))
        v.warning('Side' + side, (sxs[0], 1.4, 4.62), (sxs[1], 1.6, 4.64), 'lamp_red',
                  1 if sx > 0 else 0, [sx, 0, 0])
        fx = sorted((sx * 0.75, sx * 0.95))
        v.warning('Grille' + side, (fx[0], 1.0, 7.75), (fx[1], 1.15, 7.78), 'lamp_red',
                  0 if sx > 0 else 1, [0, 0, 1])
    v.road_lights(7.75, -3.2, w, 0.7, 0.45)
    return v, g


def ladder_animations():
    """The aerial's animated objects: the turntable swings, the ladder raises on it, the fly runs
    out along the ladder. applyAfter carries each with the one it rides on."""
    pivot = AERIAL_PIVOT
    return [
        {'objectName': 'turntable',
         'animations': [aerial_animation('rotation', [0, 90, 0], AERIAL_TURN, pivot)]},
        {'objectName': 'ladder_base', 'applyAfter': 'turntable',
         'animations': [aerial_animation('rotation', [-60, 0, 0], AERIAL_RAISE, pivot)]},
        {'objectName': 'ladder_fly', 'applyAfter': 'ladder_base',
         'animations': [aerial_animation('translation', [0, 0, AERIAL_REACH], AERIAL_EXTEND)]},
    ]


def conventional_cab(v, w, front_z):
    """A conventional cab: a long hood, the cab behind it, the windshield above the hood."""
    hood = front_z - 1.8
    back = hood - 1.5
    v.box((-w, 0.25, hood), (w, 1.45, front_z), 'paint')                       # hood
    v.box((-w, 0.25, back), (w, 2.45, hood), 'paint')                          # cab
    v.box((-w + 0.05, 2.45, back + 0.05), (w - 0.05, 2.5, hood - 0.05), 'paint2')
    v.box((-w + 0.1, 1.5, hood), (w - 0.1, 2.35, hood + 0.02), 'glass', faces=('s',))
    v.both((w, 1.5, back + 0.2), (w + 0.02, 2.3, hood - 0.15), 'glass')
    v.both((w, 0.55, back + 0.25), (w + 0.025, 0.95, hood - 0.2), 'decal', faces=('e', 'w'))
    v.box((-0.7, 0.4, front_z), (0.7, 1.35, front_z + 0.03), 'grille', faces=('s',))
    v.box((-w - 0.05, -0.05, front_z), (w + 0.05, 0.3, front_z + 0.25), 'chrome')
    v.both((w, 0.3, hood + 0.2), (w + 0.12, 0.9, front_z - 0.3), 'black')     # fenders
    v.both((w, 1.85, hood - 0.1), (w + 0.25, 2.25, hood), 'chrome')            # mirrors
    return back


DUMP_HINGE = (0.0, 0.95, -2.5)


def dpw_truck():
    """A public works dump truck: a conventional cab, a plow blade, amber beacons, and a dump
    body the DUMP switch tips up about its back edge."""
    v = Vehicle()
    g = -0.55
    w = 1.15
    v.box((-0.5, -0.15, -2.7), (0.5, 0.25, 6.5), 'black')
    back = conventional_cab(v, w, 6.3)
    v.box((-0.5, 0.25, back - 0.6), (0.5, 0.95, back), 'black')               # cab-to-body gap
    # the plow on its frame mount
    v.box((-0.3, 0.0, 6.55), (0.3, 0.4, 6.75), 'black')
    v.box((-1.55, -0.45, 6.75), (1.55, 0.55, 6.95), 'blade')
    hx, hy, hz = DUMP_HINGE
    v.group('dump_body', [
        ((-1.25, hy, hz), (1.25, hy + 0.1, 2.55), 'bed'),                       # floor
        ((1.17, hy + 0.1, hz), (1.25, hy + 1.15, 2.55), 'paint'),               # sides
        ((-1.25, hy + 0.1, hz), (-1.17, hy + 1.15, 2.55), 'paint'),
        ((-1.25, hy + 0.1, 2.5), (1.25, hy + 1.5, 2.6), 'paint'),               # headboard
        ((-1.25, hy + 1.4, 2.6), (1.25, hy + 1.5, back - 0.05), 'paint'),       # cab shield
        ((-1.2, hy + 0.1, hz - 0.08), (1.2, hy + 1.1, hz), 'paint'),            # tailgate
        ((1.25, hy + 0.5, hz + 0.2), (1.27, hy + 0.7, 2.4), 'stripe'),
        ((-1.27, hy + 0.5, hz + 0.2), (-1.25, hy + 0.7, 2.4), 'stripe'),
    ])
    v.lightbar(0.85, 2.5, back + 0.6, ['lamp_amber'] * 8, variable='BEACONS', emitter=False)
    for side, sx in (('L', 1), ('R', -1)):
        xs = sorted((sx * 0.75, sx * 1.0))
        v.warning('RearBeacon' + side, (xs[0], 0.55, -2.75), (xs[1], 0.75, -2.7), 'lamp_amber',
                  0 if sx > 0 else 1, [0, 0, -1], variable='BEACONS')
    v.road_lights(6.33, -2.7, 0.95, 0.75, 0.3)
    return v, g


def dump_animations():
    return [{'objectName': 'dump_body', 'animations': [
        aerial_animation('rotation', [-45, 0, 0], (0, 80, 0), DUMP_HINGE, variable='DUMP')]}]


BOOM_PIVOT = (0.0, 2.65, -1.6)
BOOM_TIP = (0.0, 2.8, 5.0)
BOOM_RAISE = (0, 100, 180)
BOOM_TURN = (100, 80, 100)
BOOM_EXTEND = (180, 100, 0)


def bucket_truck():
    """A power company bucket truck: a conventional cab, a service body with compartments, and a
    boom with an insulated bucket. The BOOM switch raises it, swings it to the left and runs the
    upper boom out, the bucket staying level all the way."""
    v = Vehicle()
    g = -0.55
    w = 1.15
    v.box((-0.5, -0.15, -2.5), (0.5, 0.25, 6.3), 'black')
    back = conventional_cab(v, w, 6.1)
    v.box((-1.22, 0.25, -2.5), (1.22, 1.5, back - 0.1), 'paint2')             # service body
    for z0, z1 in ((-2.4, -1.2), (-1.1, 0.4), (0.5, 2.0), (2.1, back - 0.2)):
        v.both((1.22, 0.35, z0), (1.23, 1.4, z1), 'door', faces=('e', 'w'))
    v.both((1.23, 1.15, -2.5), (1.24, 1.3, back - 0.1), 'stripe', faces=('e', 'w'))
    v.box((-1.22, 1.5, -2.5), (1.22, 1.55, back - 0.1), 'black', faces=('u',))
    v.both((1.22, 0.0, -2.4), (1.75, 0.15, -2.0), 'chrome')                    # outriggers
    v.box((-0.2, 2.5, 3.2), (0.2, 2.65, 3.4), 'chrome')                        # boom rest
    px, py, pz = BOOM_PIVOT
    v.group('boom_turret', [((-0.5, 1.55, pz - 0.5), (0.5, 1.8, pz + 0.5), 'chrome'),
                            ((-0.25, 1.8, pz - 0.25), (0.25, py, pz + 0.25), 'paint2')])
    v.group('boom', [((-0.18, py, pz - 0.2), (0.18, py + 0.32, 4.6), 'paint2')])
    v.group('boom_upper', [((-0.13, py + 0.04, pz + 0.3), (0.13, py + 0.28, 4.95), 'bucket')])
    tx, ty, tz = BOOM_TIP
    v.group('bucket', [((-0.4, ty - 0.25, tz), (0.4, ty + 0.75, tz + 0.75), 'bucket'),
                       ((-0.42, ty + 0.7, tz - 0.02), (0.42, ty + 0.78, tz + 0.77), 'black')])
    v.lightbar(0.85, 2.5, back + 0.6, ['lamp_amber'] * 8, variable='BEACONS', emitter=False)
    for side, sx in (('L', 1), ('R', -1)):
        xs = sorted((sx * 1.0, sx * 1.2))
        v.warning('RearBeacon' + side, (xs[0], 1.55, -2.5), (xs[1], 1.75, -2.3), 'lamp_amber',
                  0 if sx > 0 else 1, [0, 0, -1], variable='BEACONS')
    v.road_lights(6.13, -2.5, 1.1, 0.75, 0.45)
    return v, g


def boom_animations():
    pivot, tip = BOOM_PIVOT, BOOM_TIP
    return [
        {'objectName': 'boom_turret', 'animations': [
            aerial_animation('rotation', [0, 90, 0], BOOM_TURN, pivot, 'BOOM')]},
        {'objectName': 'boom', 'applyAfter': 'boom_turret', 'animations': [
            aerial_animation('rotation', [-55, 0, 0], BOOM_RAISE, pivot, 'BOOM')]},
        {'objectName': 'boom_upper', 'applyAfter': 'boom', 'animations': [
            aerial_animation('translation', [0, 0, 4.0], BOOM_EXTEND, None, 'BOOM')]},
        # the bucket turns back as the boom raises, so it stays level
        {'objectName': 'bucket', 'applyAfter': 'boom_upper', 'animations': [
            aerial_animation('rotation', [55, 0, 0], BOOM_RAISE, tip, 'BOOM')]},
    ]


# The tow truck's bed: it slides back, then tilts its back end down onto the road.
BED_SLIDE = 2.6
BED_PIVOT = (0.0, 1.0, -3.3 + BED_SLIDE)   # the truck's tail, in the bed's resting frame
BED_DECK_Y = 1.12
BED_FRONT_Z = 3.3


def tow_truck():
    """A rollback carrier: a conventional cab, a flatbed the BED switch slides back and tilts,
    and a wheel lift under the tail. The flatbed carries a vehicle (a mounted tow_flatbed hitch)
    and the wheel lift drags one (tow_wheel), both hookups other packs' cars already offer."""
    v = Vehicle()
    g = -0.55
    w = 1.15
    v.box((-0.5, -0.15, -3.4), (0.5, 0.25, 6.6), 'black')
    back = conventional_cab(v, w, 6.4)
    v.box((-1.0, 0.25, -3.3), (1.0, 1.0, back - 0.1), 'paint')                # subframe body
    v.box((-1.25, 1.0, 3.45), (1.25, 2.55, 3.55), 'paint')                     # headboard
    v.box((-1.25, 2.55, 3.4), (1.25, 2.65, 3.6), 'chrome')
    v.group('bed_slide', [((0.6, 0.95, -3.3), (0.8, 1.0, 3.3), 'black'),
                          ((-0.8, 0.95, -3.3), (-0.6, 1.0, 3.3), 'black')])
    v.group('flatbed', [((-1.25, 1.0, -3.3), (1.25, BED_DECK_Y, BED_FRONT_Z), 'bed'),
                        ((1.17, BED_DECK_Y, -3.3), (1.25, 1.25, BED_FRONT_Z), 'chrome'),
                        ((-1.25, BED_DECK_Y, -3.3), (-1.17, 1.25, BED_FRONT_Z), 'chrome'),
                        ((-1.25, 1.0, -3.4), (1.25, 1.1, -3.3), 'blade')])
    v.box((-0.2, -0.25, -4.0), (0.2, 0.05, -3.3), 'black')                     # wheel lift
    v.box((-0.9, -0.3, -4.2), (0.9, -0.1, -4.0), 'blade')
    v.both((1.0, 0.3, -2.0), (1.01, 0.9, 2.0), 'door', faces=('e', 'w'))      # tool boxes
    v.lightbar(0.85, 2.5, back + 0.6, ['lamp_amber'] * 8, variable='BEACONS', emitter=False)
    for side, sx in (('L', 1), ('R', -1)):
        xs = sorted((sx * 1.0, sx * 1.2))
        v.warning('HeadboardBeacon' + side, (xs[0], 2.65, 3.4), (xs[1], 2.85, 3.6), 'lamp_amber',
                  0 if sx > 0 else 1, [0, 0, -1], variable='BEACONS')
    v.road_lights(6.43, -3.3, 0.95, 0.75, 0.4)
    return v, g


def bed_animations():
    return [
        {'objectName': 'bed_slide', 'animations': [
            aerial_animation('translation', [0, 0, -BED_SLIDE], (0, 80, 60), None, 'BED')]},
        {'objectName': 'flatbed', 'applyAfter': 'bed_slide', 'animations': [
            aerial_animation('rotation', [-14, 0, 0], (80, 60, 0), BED_PIVOT, 'BED')]},
    ]


def tow_hitches(v):
    return [
        {'groupName': 'Flatbed', 'isHitch': True, 'canInitiateConnections': True,
         'connections': [{'type': 'tow_flatbed', 'mounted': True, 'distance': 3,
                          'pos': [0, BED_DECK_Y, BED_FRONT_Z - 0.4]}]},
        {'groupName': 'Wheel Lift', 'isHitch': True, 'canInitiateConnections': True,
         'connections': [{'type': 'tow_wheel', 'distance': 2, 'pos': [0, -0.2, -4.1]}]},
    ]


def transit_bus():
    """A 40 ft low-floor city bus: a window band with pillars, a destination sign, front and
    rear doors the DOORS switch opens, a roof air conditioner and a bike rack."""
    v = Vehicle()
    g = -0.55
    w = 1.27
    front, rear = 9.8, -3.4
    v.box((-w, -0.25, rear), (w, 0.6, front), 'paint')                         # skirt
    v.box((-w, 0.6, rear), (w, 2.75, front), 'paint2')                         # body
    v.box((-w + 0.05, 2.75, rear + 0.1), (w - 0.05, 2.85, front - 0.1), 'paint')
    v.box((-0.8, 2.85, 0.5), (0.8, 3.15, 4.0), 'chrome')                       # air conditioner
    v.both((w, 0.62, rear), (w + 0.01, 0.82, front), 'stripe', faces=('e', 'w'))
    v.both((w, 1.25, -2.8), (w + 0.015, 2.4, 8.1), 'glass', faces=('e', 'w'))
    for k in range(9):
        z = -2.75 + k * 1.35
        v.both((w + 0.015, 1.25, z), (w + 0.025, 2.4, z + 0.1), 'paint2', faces=('e', 'w'))
    v.both((w + 0.01, 0.9, 0.6), (w + 0.02, 1.2, 3.0), 'decal', faces=('e', 'w'))
    v.box((-1.2, 0.9, front), (1.2, 2.4, front + 0.02), 'glass', faces=('s',))  # windshield
    v.box((-0.95, 2.45, front), (0.95, 2.7, front + 0.03), 'sign', faces=('s',))
    v.box((-0.9, 2.45, rear - 0.02), (0.9, 2.65, rear), 'sign', faces=('n',))
    v.box((-0.7, -0.15, front), (0.7, 0.2, front + 0.35), 'black')             # bike rack
    v.box((-0.8, 0.9, rear - 0.02), (0.8, 2.3, rear), 'grille', faces=('n',))  # engine grille
    v.box((-0.5, 0.15, rear - 0.01), (0.5, 0.4, rear), 'plate', faces=('n',))
    # the doors, on the right (-x), each one leaf that the DOORS switch slides out and back
    # along the outside of the body (forward, the front leaf would stand past the nose)
    v.group('door_front', [((-w - 0.03, 0.0, 8.35), (-w - 0.01, 2.35, 9.4), 'glass'),
                           ((-w - 0.04, 0.0, 8.35), (-w - 0.03, 2.35, 8.42), 'black')])
    v.group('door_rear', [((-w - 0.03, 0.0, 3.2), (-w - 0.01, 2.35, 4.3), 'glass'),
                          ((-w - 0.04, 0.0, 4.23), (-w - 0.03, 2.35, 4.3), 'black')])
    v.road_lights(front + 0.02, rear, 1.15, 0.45, 0.6)
    return v, g


def door_animations():
    return [
        {'objectName': 'door_front', 'animations': [
            aerial_animation('translation', [-0.15, 0, -0.9], (0, 20, 0), None, 'DOORS')]},
        {'objectName': 'door_rear', 'animations': [
            aerial_animation('translation', [-0.15, 0, -0.9], (0, 20, 0), None, 'DOORS')]},
    ]


# ------------------------------------------------------------------------------------------
# Liveries and textures
# ------------------------------------------------------------------------------------------

def rgb(h):
    return tuple(int(h[k:k + 2], 16) for k in (1, 3, 5))


# Each livery: paint, second colour (roof, module), stripe, decal text, decal colour.
LIVERIES = {
    'csm_fire_engine': [
        ('_red', 'Fire Engine', '#B01818', '#F2F2EE', '#F2F2EE', 'FIRE RESCUE', '#F2D24A'),
        ('_lime', 'Fire Engine (Lime)', '#C9D82A', '#F2F2EE', '#1C1C1C', 'FIRE RESCUE',
         '#1C1C1C'),
        ('_blackred', 'Fire Engine (Black over Red)', '#B01818', '#161618', '#F2D24A',
         'FIRE RESCUE', '#F2D24A'),
        ('_white', 'Fire Engine (White)', '#F2F2EE', '#B01818', '#B01818', 'FIRE RESCUE',
         '#B01818'),
        ('_airport', 'Fire Engine (Airport)', '#D9D21E', '#F2F2EE', '#1C1C1C', 'AIRPORT FIRE',
         '#1C1C1C'),
    ],
    'csm_ladder_truck': [
        ('_red', 'Ladder Truck', '#B01818', '#F2F2EE', '#F2F2EE', 'FIRE RESCUE', '#F2D24A'),
        ('_lime', 'Ladder Truck (Lime)', '#C9D82A', '#F2F2EE', '#1C1C1C', 'FIRE RESCUE',
         '#1C1C1C'),
        ('_blackred', 'Ladder Truck (Black over Red)', '#B01818', '#161618', '#F2D24A',
         'FIRE RESCUE', '#F2D24A'),
        ('_white', 'Ladder Truck (White)', '#F2F2EE', '#B01818', '#B01818', 'FIRE RESCUE',
         '#B01818'),
    ],
    'csm_ambulance': [
        ('_red', 'Ambulance', '#F2F2EE', '#F2F2EE', '#C01A1A', 'AMBULANCE', '#C01A1A'),
        ('_orange', 'Ambulance (Orange Stripe)', '#F2F2EE', '#F2F2EE', '#E07818', 'AMBULANCE',
         '#1E4AA0'),
        ('_blue', 'Ambulance (Blue Stripe)', '#F2F2EE', '#F2F2EE', '#1E4AA0', 'AMBULANCE',
         '#1E4AA0'),
        ('_green', 'Ambulance (Green Stripe)', '#F2F2EE', '#F2F2EE', '#1E7A3A', 'EMS',
         '#1E7A3A'),
        ('_yellow', 'Ambulance (High-Vis)', '#E2E22A', '#E2E22A', '#1E7A3A', 'AMBULANCE',
         '#1E7A3A'),
    ],
    'csm_police_suv': [
        ('_blackwhite', 'Police SUV', '#151517', '#F2F2EE', '#151517', 'POLICE', '#151517'),
        ('_white', 'Police SUV (White)', '#F2F2EE', '#F2F2EE', '#1E4AA0', 'POLICE', '#1E4AA0'),
        ('_blue', 'Police SUV (Blue)', '#1B2A4A', '#F2F2EE', '#1B2A4A', 'POLICE', '#1B2A4A'),
        ('_sheriff', 'Sheriff SUV', '#2F4A2A', '#D9C9A0', '#2F4A2A', 'SHERIFF', '#2F4A2A'),
        ('_state', 'State Police SUV', '#4A4E54', '#8A8E94', '#1E4AA0', 'STATE POLICE',
         '#F2F2EE'),
        ('_unmarked', 'Unmarked SUV', '#26282C', '#26282C', '#26282C', '', '#26282C'),
    ],
    'csm_dpw_truck': [
        ('_orange', 'Public Works Dump Truck', '#E8761C', '#F2F2EE', '#1C1C1C', 'PUBLIC WORKS',
         '#1C1C1C'),
        ('_yellow', 'Public Works Dump Truck (Yellow)', '#E8C21A', '#F2F2EE', '#1C1C1C',
         'PUBLIC WORKS', '#1C1C1C'),
        ('_white', 'Public Works Dump Truck (White)', '#F2F2EE', '#F2F2EE', '#E8761C',
         'PUBLIC WORKS', '#E8761C'),
    ],
    'csm_tow_truck': [
        ('_white', 'Tow Truck', '#F2F2EE', '#F2F2EE', '#C01A1A', 'TOWING', '#C01A1A'),
        ('_red', 'Tow Truck (Red)', '#B01818', '#F2F2EE', '#F2F2EE', 'TOWING', '#F2F2EE'),
        ('_yellow', 'Tow Truck (Yellow)', '#E8C21A', '#E8C21A', '#1C1C1C', 'TOWING', '#1C1C1C'),
        ('_black', 'Tow Truck (Black)', '#1C1C1E', '#F2F2EE', '#E8C21A', 'TOWING', '#E8C21A'),
    ],
    'csm_transit_bus': [
        ('_metro', 'Metro Bus', '#1E4AA0', '#F2F2EE', '#D8202A', 'METRO', '#1E4AA0',
         'METRO BUS'),
        ('_cityline', 'Transit Bus (CITYLINE)', '#0E5E6F', '#F2F2EE', '#F4C428', 'CITYLINE',
         '#0E5E6F', '1 CITY CENTER'),
        ('_riverway', 'Transit Bus (RIVERWAY)', '#1F3A73', '#F2F2EE', '#F08A24', 'RIVERWAY',
         '#1F3A73', '7 RIVERSIDE'),
        ('_verdant', 'Transit Bus (VERDANT)', '#24743C', '#ECF0E8', '#24743C', 'VERDANT',
         '#24743C', '12 PARK LOOP'),
        ('_emberline', 'Transit Bus (EMBERLINE)', '#B02420', '#F2F2EE', '#383A3E', 'EMBERLINE',
         '#B02420', '20 EXPRESS'),
    ],
    'csm_bucket_truck': [
        ('_white', 'Power Company Bucket Truck', '#F2F2EE', '#F2F2EE', '#1E4AA0', 'CITY POWER',
         '#1E4AA0'),
        ('_yellow', 'Power Company Bucket Truck (Yellow)', '#E8C21A', '#E8C21A', '#1C1C1C',
         'CITY POWER', '#1C1C1C'),
        ('_green', 'Power Company Bucket Truck (Green)', '#2E6B3A', '#F2F2EE', '#2E6B3A',
         'CITY POWER', '#F2F2EE'),
    ],
}

# Where each vehicle's door lettering sits: on the main paint, or on the second colour.
DECAL_ON = {'csm_fire_engine': 'paint', 'csm_ladder_truck': 'paint', 'csm_ambulance': 'paint2',
            'csm_police_suv': 'paint2', 'csm_dpw_truck': 'paint', 'csm_bucket_truck': 'paint',
            'csm_tow_truck': 'paint', 'csm_transit_bus': 'paint2'}


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def vehicle_texture(livery, decal_on='paint'):
    _, _, paint, paint2, stripe, text, text_colour = livery[:7]
    sign = livery[7] if len(livery) > 7 else ''
    img = Image.new('RGBA', (T, T), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def fill(name, colour):
        x0, y0, x1, y1 = CELLS[name]
        d.rectangle((x0, y0, x1 - 1, y1 - 1), fill=colour + (255,))

    p, p2, st = rgb(paint), rgb(paint2), rgb(stripe)
    fill('paint', p)
    fill('paint2', p2)
    fill('stripe', st)
    fill('chrome', (196, 200, 206))
    d.line((48, 0, 63, 0), fill=(236, 238, 242, 255))
    fill('black', (24, 24, 26))
    fill('glass', (40, 54, 66))
    d.line((80, 2, 88, 10), fill=(70, 88, 104, 255))
    fill('interior', (52, 50, 48))
    fill('door', p)
    for y in range(1, 16, 3):                      # roll-up door slats
        d.line((112, y, 127, y), fill=shade(p, 0.8) + (255,))
    d.line((112, 15, 127, 15), fill=(40, 40, 40, 255))
    fill('grille', (30, 30, 32))
    for y in range(17, 32, 2):
        d.line((1, y, 14, y), fill=(150, 154, 160, 255))
    fill('headlight', (210, 214, 218))
    fill('tail', (130, 18, 16))
    fill('amber', (150, 92, 12))
    fill('white', (180, 182, 186))
    fill('lamp_red', (120, 18, 14))
    fill('lamp_blue', (18, 34, 120))
    fill('lamp_white', (150, 152, 158))
    fill('emitter', (70, 76, 88))
    fill('hosebed', (32, 30, 28))
    for x in range(81, 96, 3):                     # hose folds
        d.line((x, 33, x, 46), fill=(170, 150, 60, 255))
    fill('plate', (236, 236, 228))
    d.rectangle((97, 37, 110, 42), fill=(40, 60, 120, 255))
    fill('rubber', (20, 20, 20))
    # the decal: the livery's word in the pixel font, on the body colour of where it goes
    x0, y0, x1, y1 = CELLS['decal']
    under = p if decal_on == 'paint' else p2
    d.rectangle((x0, y0, x1 - 1, y1 - 1), fill=under + (255,))
    scale = 2 if lc.text_width(text, 2) <= 62 else 1
    if text:
        lc.draw_text_centred(img, text, 32, y0 + (16 - 5 * scale) // 2, rgb(text_colour), scale)
    # the work trucks' cells
    fill('lamp_amber', (130, 82, 10))
    fill('bed', (70, 72, 76))
    for x in range(17, 32, 4):
        d.line((x, 48, x, 63), fill=(58, 60, 64, 255))
    fill('bucket', (226, 206, 60))
    fill('blade', (232, 118, 28))
    d.rectangle((48, 48, 63, 51), fill=(30, 30, 30, 255))
    # the destination sign: amber LEDs on black
    sx0, sy0, sx1, sy1 = CELLS['sign']
    d.rectangle((sx0, sy0, sx1 - 1, sy1 - 1), fill=(16, 16, 16, 255))
    if sign:
        lc.draw_text_centred(img, sign, (sx0 + sx1) // 2, sy0 + 5, (255, 170, 20), 1)
    return img


def vehicle_icon(v, ground, texture):
    """A side view of the vehicle from its left, projected off its own boxes."""
    zs = [b[0][2] for b in v.outline] + [b[1][2] for b in v.outline]
    ys = [b[1][1] for b in v.outline] + [ground]
    z0, z1, top = min(zs), max(zs), max(ys)
    span = max(z1 - z0, top - ground)
    k = 15.0 / span
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    px = texture.load()
    for lo, hi, name in sorted(v.outline, key=lambda b: b[1][0]):
        if hi[0] < 0.05:
            continue  # the far side cannot be seen
        cx0, cy0, cx1, cy1 = CELLS[name]
        colour = px[(cx0 + cx1) // 2, (cy0 + cy1) // 2]
        a = int((z1 - hi[2]) * k)
        b = int((z1 - lo[2]) * k)
        c = int(15 - (hi[1] - ground) * k)
        e = int(15 - (lo[1] - ground) * k)
        d.rectangle((min(a, b), min(c, e), max(a, b), max(c, e)), fill=colour)
    return img


# ------------------------------------------------------------------------------------------
# The vehicles' parts: wheels, seat and engines
# ------------------------------------------------------------------------------------------

WHEELS = {
    # name: (label, diameter, tyre width, rim colour)
    'csm_wheel_truck': ('CSM Truck Wheel', 1.1, 0.34, (196, 200, 206)),
    'csm_wheel_van': ('CSM Van Wheel', 0.8, 0.26, (196, 200, 206)),
    'csm_wheel_car': ('CSM Car Wheel', 0.78, 0.26, (40, 40, 44)),
}
WT = 32  # wheel textures


def wheel_obj(diameter, width, sides=16):
    """A tyre and rim centred on the axle (the part's origin), turning about +x."""
    r = diameter / 2
    lines = ['# generated by dev-env-utils/scripts/gen_vehicle_fleet.py']
    v = vt = vn = 0

    def quad(name, pts, uvs, normal):
        nonlocal v, vt, vn
        lines.append('o %s' % name)
        for p in pts:
            lines.append('v %.5f %.5f %.5f' % p)
        for u, w in uvs:
            lines.append('vt %.6f %.6f' % (u, 1.0 - w))
        lines.append('vn %.4f %.4f %.4f' % normal)
        vn += 1
        lines.append('f ' + ' '.join('%d/%d/%d' % (v + i + 1, vt + i + 1, vn)
                                     for i in range(len(pts))))
        v += len(pts)
        vt += len(uvs)

    hw = width / 2
    for i in range(sides):
        a0 = 2 * math.pi * i / sides
        a1 = 2 * math.pi * (i + 1) / sides
        y0, z0, y1, z1 = r * math.cos(a0), r * math.sin(a0), r * math.cos(a1), r * math.sin(a1)
        am = (a0 + a1) / 2
        # the tread, facing out from the axle
        quad('tread%d' % i, [(hw, y0, z0), (-hw, y0, z0), (-hw, y1, z1), (hw, y1, z1)],
             [(0.02, 0.02), (0.48, 0.02), (0.48, 0.48), (0.02, 0.48)],
             (0, math.cos(am), math.sin(am)))
        # the sidewalls and rim faces: a fan from the hub, the inner disc in rim colour
        for sx, name in ((1, 'out'), (-1, 'in')):
            pts = [(sx * hw, 0, 0), (sx * hw, y0, z0), (sx * hw, y1, z1)]
            if sx < 0:
                pts = [pts[0], pts[2], pts[1]]
            uv = [(0.75, 0.75), (0.75 + 0.24 * math.cos(a0), 0.75 + 0.24 * math.sin(a0)),
                  (0.75 + 0.24 * math.cos(a1), 0.75 + 0.24 * math.sin(a1))]
            if sx < 0:
                uv = [uv[0], uv[2], uv[1]]
            quad('side_%s%d' % (name, i), pts, uv, (sx, 0, 0))
    return '\n'.join(lines) + '\n'


def wheel_texture(rim):
    img = Image.new('RGBA', (WT, WT), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=(26, 26, 28, 255))           # tread
    for y in range(1, 16, 3):
        d.line((0, y, 15, y), fill=(14, 14, 16, 255))
    d.ellipse((16, 16, 31, 31), fill=(30, 30, 32, 255))           # sidewall
    d.ellipse((19, 19, 28, 28), fill=rim + (255,))                # rim
    d.ellipse((22, 22, 25, 25), fill=shade(rim, 0.6) + (255,))    # hub
    return img


def wheel_json(name):
    label, diameter, width, _ = WHEELS[name]
    return {
        'definitions': [{'name': label, 'subName': '', 'extraMaterialLists': [[]]}],
        'general': {'description': 'Comes on the CSM fleet\'s vehicles; fits any wheel slot it '
                                   'is the right size for.',
                    'stackSize': 4, 'materialLists': [['minecraft:iron_ingot:0:1',
                                                       'minecraft:slime_ball:0:2']]},
        'generic': {'type': 'ground_wheel', 'width': width, 'height': diameter},
        'ground': {'isWheel': True, 'width': width, 'height': diameter,
                   'motiveFriction': 0.7, 'lateralFriction': 0.85,
                   'frictionModifiers': {'ice': -0.25, 'snow': -0.2}},
        'rendering': {'modelType': 'obj'},
    }


def seat_obj():
    o = vp.Obj(tex=32)
    o.box('cushion', (-0.25, 0.0, -0.25), (0.25, 0.12, 0.25), (0, 0, 16, 16))
    o.box('back', (-0.25, 0.12, -0.32), (0.25, 0.75, -0.2), (0, 0, 16, 16))
    o.box('frame', (-0.2, -0.15, -0.2), (0.2, 0.0, 0.2), (16, 0, 32, 16))
    return o.text().replace('gen_vehicle_parts.py', 'gen_vehicle_fleet.py')


def seat_texture():
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=(36, 36, 40, 255))
    for x in range(2, 16, 4):
        d.line((x, 0, x, 15), fill=(28, 28, 32, 255))
    d.rectangle((16, 0, 31, 15), fill=(80, 82, 86, 255))
    return img


def seat_json():
    return {
        'definitions': [{'name': 'CSM Vehicle Seat', 'subName': '', 'extraMaterialLists': [[]]}],
        'general': {'description': 'Comes in the CSM fleet\'s vehicles.', 'stackSize': 4,
                    'materialLists': [['minecraft:wool:0:2', 'minecraft:iron_ingot:0:1']]},
        'generic': {'type': 'seat'},
        'seat': {},
        'rendering': {'modelType': 'obj'},
    }


ENGINES = {
    # name: (label, fuel type, fuel consumption (the power), max, safe, idle, start, stall rpm,
    #        gear ratios, sound prefix)
    'csm_engine_diesel': ('CSM Diesel Engine', 'diesel', 0.9, 3500, 3000, 500, 600, 325,
                          [-2.8, 0.0, 4.69, 2.6, 1.5, 1.0, 0.75], 'diesel'),
    'csm_engine_petrol': ('CSM V8 Engine', 'gasoline', 0.6, 6500, 5200, 650, 750, 420,
                          [-2.4, 0.0, 3.0, 1.8, 1.25, 1.0, 0.7], 'petrol'),
}


def engine_json(name):
    label, fuel, consumption, mx, safe, idle, start, stall, gears, sound = ENGINES[name]
    snd = lambda s: '%s:%s_%s' % (PACK, sound, s)  # noqa: E731
    return {
        'definitions': [{'name': label, 'subName': '', 'extraMaterialLists': [[]]}],
        'general': {'description': 'Comes in the CSM fleet\'s vehicles.', 'stackSize': 1,
                    'materialLists': [['minecraft:iron_block:0:2', 'minecraft:piston:0:4',
                                       'minecraft:redstone:0:8']]},
        'generic': {'type': 'engine_car', 'width': 0.8, 'height': 0.8,
                    'forwardsDamageMultiplier': 1.0},
        'engine': {'type': 'normal', 'isAutomatic': True, 'starterPower': 40, 'maxRPM': mx,
                   'maxSafeRPM': safe, 'idleRPM': idle, 'startRPM': start, 'stallRPM': stall,
                   'fuelConsumption': consumption, 'gearRatios': gears, 'fuelType': fuel},
        'rendering': {'modelType': 'none', 'sounds': [
            {'name': snd('idle'), 'looping': True,
             'activeAnimations': [vp.visible_on('engine_powered')],
             'pitchAnimations': [{'animationType': 'translation',
                                  'variable': 'engine_rpm_percent_safe',
                                  'axis': [0, 1.2, 0], 'offset': 0.6}]},
            {'name': snd('start'), 'activeAnimations': [vp.visible_on('engine_running')]},
            {'name': snd('crank'), 'looping': True,
             'activeAnimations': [vp.visible_on('engine_starter')]},
        ]},
    }


# ------------------------------------------------------------------------------------------
# The vehicles' definitions
# ------------------------------------------------------------------------------------------

FLEET = {
    # name: (builder, description, mass kg, wheel, engine, horn, wheel slots, seats, engine pos,
    #        collision boxes (z centres), box width, box height, box centre y)
    'csm_fire_engine': dict(
        build=fire_engine,
        description='A custom-cab pumper. Switch EMERLTS on and it runs its lights, and '
                    'intersections with a CSM preempt detector give it the green; siren, yelp '
                    'and hi-lo each have a switch.',
        mass=7500, wheel='csm_wheel_truck', engine='csm_engine_diesel', horn='horn_air',
        wheels=[(1.0, 5.0, True), (0.95, 0.0, False), (0.62, 0.0, False)],
        seats=[(0.6, 0.85, 5.7, True), (-0.6, 0.85, 5.7, False),
               (0.6, 0.85, 4.15, False), (-0.6, 0.85, 4.15, False)],
        engine_pos=(0.0, 0.6, 5.2), boxes=[-1.4, 1.1, 3.6, 5.9], box_width=2.5,
        box_height=2.7, box_y=1.15),
    'csm_ladder_truck': dict(
        build=ladder_truck,
        description='A rear-mount aerial ladder truck. The AERIAL switch raises the ladder, '
                    'swings it to the left and runs it out; switch it off to stow it. EMERLTS '
                    'runs the lights, and intersections with a CSM preempt detector give it '
                    'the green.',
        mass=9500, wheel='csm_wheel_truck', engine='csm_engine_diesel', horn='horn_air',
        wheels=[(1.0, 6.0, True), (0.95, 0.0, False), (0.62, 0.0, False)],
        seats=[(0.6, 0.85, 6.7, True), (-0.6, 0.85, 6.7, False),
               (0.6, 0.85, 5.15, False), (-0.6, 0.85, 5.15, False)],
        engine_pos=(0.0, 0.6, 6.2), boxes=[-2.1, 0.4, 2.9, 5.4, 7.1], box_width=2.5,
        box_height=2.3, box_y=1.0,
        switches=['EMERLTS', 'siren', 'siren_yelp', 'AERIAL'], animated=ladder_animations),
    'csm_dpw_truck': dict(
        build=dpw_truck,
        description='A public works dump truck with a plow. BEACONS runs its amber lights (they '
                    'are not emergency lights, so it does not preempt signals); DUMP tips the '
                    'body.',
        mass=8000, wheel='csm_wheel_truck', engine='csm_engine_diesel', horn='horn_air',
        wheels=[(0.95, 4.6, True), (0.95, 0.0, False), (0.62, 0.0, False)],
        seats=[(0.45, 0.85, 3.7, True), (-0.45, 0.85, 3.7, False)],
        engine_pos=(0.0, 0.6, 5.4), boxes=[-1.5, 1.0, 3.5, 5.6], box_width=2.5,
        box_height=2.6, box_y=1.1, switches=['BEACONS', 'DUMP'], animated=dump_animations),
    'csm_bucket_truck': dict(
        build=bucket_truck,
        description='A power company bucket truck. BEACONS runs its amber lights (not emergency '
                    'lights, so it does not preempt signals); BOOM raises the boom, swings it to '
                    'the left and runs it out, the bucket staying level.',
        mass=7000, wheel='csm_wheel_truck', engine='csm_engine_diesel', horn='horn_air',
        wheels=[(0.95, 4.4, True), (0.95, 0.0, False), (0.62, 0.0, False)],
        seats=[(0.45, 0.85, 3.5, True), (-0.45, 0.85, 3.5, False)],
        engine_pos=(0.0, 0.6, 5.2), boxes=[-1.5, 1.0, 3.4, 5.4], box_width=2.5,
        box_height=2.4, box_y=1.0, switches=['BEACONS', 'BOOM'], animated=boom_animations),
    'csm_tow_truck': dict(
        build=tow_truck,
        description='A rollback tow truck. BED slides the flatbed back and tilts it; the panel '
                    'connects a vehicle onto the flatbed or onto the wheel lift. BEACONS runs '
                    'its amber lights, which do not preempt signals.',
        mass=7500, wheel='csm_wheel_truck', engine='csm_engine_diesel', horn='horn_air',
        wheels=[(0.95, 4.7, True), (0.95, 0.0, False), (0.62, 0.0, False)],
        seats=[(0.45, 0.85, 3.8, True), (-0.45, 0.85, 3.8, False)],
        engine_pos=(0.0, 0.6, 5.5), boxes=[-2.0, 0.5, 3.0, 5.5], box_width=2.5,
        box_height=2.4, box_y=1.0, switches=['BEACONS', 'BED'], animated=bed_animations,
        hitches=tow_hitches),
    'csm_transit_bus': dict(
        build=transit_bus,
        description='A 40 ft low-floor city bus. DOORS opens the front and rear doors.',
        mass=12000, wheel='csm_wheel_truck', engine='csm_engine_diesel', horn='horn_air',
        wheels=[(1.0, 7.2, True), (0.95, 0.0, False), (0.62, 0.0, False)],
        seats=[(0.75, 0.45, 8.7, True)] +
              [(sx * 0.75, 0.55, z, False) for z in (6.4, 5.2, 2.0, 0.8, -0.4, -1.6)
               for sx in (1, -1)],
        engine_pos=(0.0, 0.5, -2.8), boxes=[-2.2, 0.3, 2.8, 5.3, 7.8, 9.3], box_width=2.55,
        box_height=3.1, box_y=1.0, switches=['DOORS'], animated=door_animations),
    'csm_ambulance': dict(
        build=ambulance,
        description='A Type III ambulance. Switch EMERLTS on and it runs its lights, and '
                    'intersections with a CSM preempt detector give it the green.',
        mass=4200, wheel='csm_wheel_van', engine='csm_engine_diesel', horn='horn_air',
        wheels=[(0.82, 3.6, True), (0.78, 0.0, False), (0.52, 0.0, False)],
        seats=[(0.45, 0.65, 3.4, True), (-0.45, 0.65, 3.4, False), (0.0, 0.7, 0.6, False)],
        engine_pos=(0.0, 0.45, 4.4), boxes=[-1.0, 1.3, 3.5], box_width=2.3, box_height=2.8,
        box_y=1.1),
    'csm_police_suv': dict(
        build=police_suv,
        description='A police utility vehicle. Switch EMERLTS on and it runs its lights, and '
                    'intersections with a CSM preempt detector give it the green.',
        mass=2300, wheel='csm_wheel_car', engine='csm_engine_petrol', horn='horn_car',
        wheels=[(0.85, 3.0, True), (0.85, 0.0, False)],
        seats=[(0.42, 0.3, 1.7, True), (-0.42, 0.3, 1.7, False),
               (0.42, 0.3, 0.45, False), (-0.42, 0.3, 0.45, False)],
        engine_pos=(0.0, 0.4, 3.4), boxes=[-0.2, 1.6, 3.3], box_width=2.0, box_height=1.8,
        box_y=0.6),
}


def vehicle_json(name, v):
    spec = FLEET[name]
    diameter = WHEELS[spec['wheel']][1]
    wheel = '%s:%s' % (PACK, spec['wheel'])
    parts = []
    driven = []
    for x, z, steer in spec['wheels']:
        for sx in (1, -1):
            slot = {'pos': [sx * x, 0.0, z], 'minValue': round(diameter * 0.6, 3),
                    'maxValue': round(diameter * 1.2, 3), 'types': ['ground_wheel'],
                    'defaultPart': wheel}
            if sx < 0:
                slot['rot'] = [0, 180, 0]
                slot['isMirrored'] = True
            if steer:
                slot['turnsWithSteer'] = True
            parts.append(slot)
            if not steer:
                driven.append(len(parts))
    for x, y, z, controller in spec['seats']:
        slot = {'pos': [x, y, z], 'types': ['seat'], 'defaultPart': '%s:csm_vehicle_seat' % PACK,
                'dismountPos': [x + (1.6 if x >= 0 else -1.6), 0.0, z]}
        if controller:
            slot['isController'] = True
        parts.append(slot)
    parts.append({'pos': list(spec['engine_pos']), 'minValue': 0.25, 'maxValue': 1.0,
                  'types': ['engine_car'], 'defaultPart': '%s:%s' % (PACK, spec['engine']),
                  'linkedParts': driven})
    switches = spec.get('switches', ['EMERLTS', 'siren', 'siren_yelp', 'siren_hilo'])
    tones = [t for t in (('siren', 'siren_wail'), ('siren_yelp', 'siren_yelp'),
                         ('siren_hilo', 'siren_hilo')) if t[0] in switches]
    definitions = [{'name': livery[1], 'subName': livery[0], 'extraMaterialLists': [[]]}
                   for livery in LIVERIES[name]]
    definition = {
        'definitions': definitions,
        'general': {'description': spec['description'],
                    'materialLists': [['minecraft:iron_block:0:6', 'minecraft:glass_pane:0:6',
                                       'minecraft:redstone_block:0:1']]},
        'motorized': {'emptyMass': spec['mass'], 'fuelCapacity': 15000,
                      'defaultFuelQty': 15000, 'axleRatio': 3.55, 'brakingFactor': 1.0,
                      'dragCoefficient': 0.5, 'hasHeadlights': True, 'hasRunningLights': True,
                      'hasTurnSignals': True, 'litVariable': 'headlight',
                      'panel': 'mts:default_car'},
        'parts': parts,
        'collisionGroups': [{
            'collisionTypes': ['block', 'entity', 'attack', 'click'],
            'collisions': [{'pos': [0, spec['box_y'], z], 'width': spec['box_width'],
                            'height': spec['box_height']} for z in spec['boxes']],
        }],
        'rendering': {
            'modelType': 'obj',
            'customVariables': switches,
            'sounds': [{'name': '%s:%s' % (PACK, spec['horn']), 'looping': True,
                        'activeAnimations': [vp.visible_on('horn')]}] +
                      [{'name': '%s:%s' % (PACK, sound), 'looping': True,
                        'activeAnimations': [vp.visible_on(variable)]}
                       for variable, sound in tones],
            'lightObjects': v.lights,
        },
    }
    if 'animated' in spec:
        definition['rendering']['animatedObjects'] = spec['animated']()
    # Every vehicle can be towed, by the hookups other packs' tow trucks look for (and ours):
    # the bumper, the front axle for a wheel lift, and the front for a flatbed's winch.
    front_z = max(b[1][2] for b in v.outline)
    axle_z = max(z for _, z, steer in spec['wheels'] if steer)
    ground = -WHEELS[spec['wheel']][1] / 2
    groups = [{'groupName': 'Towing', 'isHookup': True, 'connections': [
        {'type': 'tow_bumper', 'pos': [0, round(ground + 0.5, 3), round(front_z, 3)],
         'distance': 2},
        {'type': 'tow_wheel', 'pos': [0, round(ground + 0.15, 3), axle_z], 'distance': 2},
        {'type': 'tow_flatbed', 'pos': [0, round(ground + 0.1, 3), round(front_z, 3)],
         'distance': 2},
    ]}]
    if 'hitches' in spec:
        groups = spec['hitches'](v) + groups
    definition['connectionGroups'] = groups
    return definition


# ------------------------------------------------------------------------------------------
# Catalogue and writing
# ------------------------------------------------------------------------------------------

def catalogue():
    files = {}

    def put(path, data):
        files[path] = data

    def item(kind, name, icon):
        put(os.path.join(PACK_DIR, 'textures', 'items', kind, name + '.png'), vp.png_bytes(icon))
        put(os.path.join(ROOT, 'mts', 'models', 'item', '%s.%s.json' % (PACK, name)),
            vp.json_bytes({'parent': 'minecraft:item/generated',
                           'textures': {'layer0': '%s:items/%s/%s' % (PACK, kind, name)}}))

    for name in FLEET:
        v, ground = FLEET[name]['build']()
        put(os.path.join(PACK_DIR, 'objmodels', 'vehicles', name + '.obj'),
            v.obj.text().replace('gen_vehicle_parts.py', 'gen_vehicle_fleet.py').encode('utf-8'))
        put(os.path.join(PACK_DIR, 'jsondefs', 'vehicles', name + '.json'),
            vp.json_bytes(vehicle_json(name, v)))
        for livery in LIVERIES[name]:
            texture = vehicle_texture(livery, DECAL_ON[name])
            put(os.path.join(PACK_DIR, 'textures', 'vehicles', name + livery[0] + '.png'),
                vp.png_bytes(texture))
            item('vehicles', name + livery[0], vehicle_icon(v, ground, texture))

    for name, (label, diameter, width, rim) in WHEELS.items():
        put(os.path.join(PACK_DIR, 'objmodels', 'parts', name + '.obj'),
            wheel_obj(diameter, width).encode('utf-8'))
        put(os.path.join(PACK_DIR, 'textures', 'parts', name + '.png'),
            vp.png_bytes(wheel_texture(rim)))
        put(os.path.join(PACK_DIR, 'jsondefs', 'parts', name + '.json'),
            vp.json_bytes(wheel_json(name)))
        icon = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        dd = ImageDraw.Draw(icon)
        dd.ellipse((1, 1, 14, 14), fill=(26, 26, 28, 255))
        dd.ellipse((5, 5, 10, 10), fill=rim + (255,))
        item('parts', name, icon)

    put(os.path.join(PACK_DIR, 'objmodels', 'parts', 'csm_vehicle_seat.obj'),
        seat_obj().encode('utf-8'))
    put(os.path.join(PACK_DIR, 'textures', 'parts', 'csm_vehicle_seat.png'),
        vp.png_bytes(seat_texture()))
    put(os.path.join(PACK_DIR, 'jsondefs', 'parts', 'csm_vehicle_seat.json'),
        vp.json_bytes(seat_json()))
    seat_icon = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    ImageDraw.Draw(seat_icon).rectangle((4, 2, 7, 12), fill=(36, 36, 40, 255))
    ImageDraw.Draw(seat_icon).rectangle((4, 10, 12, 12), fill=(36, 36, 40, 255))
    item('parts', 'csm_vehicle_seat', seat_icon)

    for name in ENGINES:
        put(os.path.join(PACK_DIR, 'jsondefs', 'parts', name + '.json'),
            vp.json_bytes(engine_json(name)))
        icon = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        dd = ImageDraw.Draw(icon)
        dd.rectangle((2, 5, 13, 12), fill=(70, 72, 76, 255))
        dd.rectangle((4, 2, 11, 5), fill=(120, 124, 130, 255))
        item('parts', name, icon)
    return files


def main():
    check = '--check' in sys.argv
    files = catalogue()
    drift = []
    for path, data in sorted(files.items()):
        if vp.same(path, data):
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
        print('vehicle fleet is up to date (%d files)' % len(files))


if __name__ == '__main__':
    main()
