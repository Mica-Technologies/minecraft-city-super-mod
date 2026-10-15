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
    python dev-env-utils/scripts/gen_vehicle_fleet.py --speeds  # each vehicle's top speed, 0-60

Speed: IV scales car motion by carSpeedFactor (0.35) but applies drag to the unscaled motion, so
each vehicle is geared to reach its engine's maximum rpm at a set ceiling (`ceiling_mph`) and
given the power to get close to it; simulate() models IV's code to tune and check that.
"""

import json
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
    # the tree crew's cells, drawn only into the textures of the vehicles that use them
    'hazard': cell(0, 4), 'deck': cell(1, 4), 'cutter': cell(2, 4), 'track': cell(3, 4),
    'mulch': cell(4, 4),
    # the pickups' cells: bumpers and bars (chrome or black by livery), grille mesh, badge
    'trim': cell(5, 4), 'mesh': cell(6, 4), 'badge': cell(7, 4),
    'script': cell(0, 5, 4, 1), 'gate': cell(4, 5, 4, 1),
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
        self.lights = []      # light definitions, one per lamp object
        self.lamp_groups = {}  # (cell, colour, animations) -> that lamp object's light
        self.outline = []     # (lo, hi, cell name) for the item icon
        self.count = 0

    def box(self, lo, hi, name, faces=('n', 's', 'e', 'w', 'u', 'd'), obj=None):
        self.count += 1
        self.obj.box(obj or 'body', lo, hi, CELLS[name], faces)
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
        """A lamp. Lamps of one cell and colour that light on the same animations are one object
        (named for the first of them) carrying every one's flare: they could not be told apart
        anyway, and each object is a draw of its own every frame."""
        key = (cell_name, colour, json.dumps(animations, sort_keys=True))
        light = self.lamp_groups.get(key)
        if light is None:
            light = {'objectName': '&' + name, 'emissive': True, 'isElectric': True,
                     'color': colour, 'brightnessAnimations': animations}
            self.lamp_groups[key] = light
            self.lights.append(light)
        self.box(lo, hi, cell_name, faces, obj=light['objectName'])
        if axis:
            centre = [round((lo[k] + hi[k]) / 2, 4) for k in range(3)]
            light.setdefault('blendableComponents', []).append(vp.flare(centre, axis, size))

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


def _spans(z0, z1, cut):
    """[z0, z1] less the spans in `cut`, as a list of (a, b)."""
    out = [(z0, z1)]
    for c0, c1 in sorted(cut):
        nxt = []
        for a, b in out:
            if c1 <= a or c0 >= b:
                nxt.append((a, b))
                continue
            if c0 > a:
                nxt.append((a, c0))
            if c1 < b:
                nxt.append((c1, b))
        out = nxt
    return [(a, b) for a, b in out if b - a > 0.005]


def _clip(spans, z0, z1):
    return [(max(a, z0), min(b, z1)) for a, b in spans if min(b, z1) - max(a, z0) > 0.005]


WALL = 0.05


def cabin(v, w, floor, belt, top, z0, z1, lower='paint', upper=None, windows=(), win_top=None,
          front='windshield', front_belt=None, rear='wall', right_gaps=(), driver_x=None):
    """A cabin a rider sits in: floor, roof, walls up to the belt line, pillars between the
    windows, a header above them, the front and rear walls, a dash and a steering wheel.

    It is hollow because the rider's eye is in it. A solid box with glass painted on the outside
    is drawn only from outside (faces cull from behind), so from the seat every wall vanished and
    the trim floated in the air. Windows are IV `window` objects: IV draws them in its own glass,
    from both sides when the player's config asks for inner windows. `right_gaps` are door
    openings in the right (-x) wall, from the floor to the window top."""
    upper = upper or lower
    win_top = win_top or top - 0.12
    fb = front_belt or belt
    t = WALL
    # the floor stands a hundredth proud of the sill or skirt under it, whose top is at `floor`
    v.box((-w + t, floor - 0.04, z0 + t), (w - t, floor + 0.01, z1 - t), 'black')  # floor
    v.box((-w, top - 0.05, z0), (w, top, z1), upper)                            # roof
    for sx in (1, -1):
        xa, xb = sorted((sx * (w - t), sx * w))
        gaps = list(right_gaps) if sx < 0 else []
        for a, b in _spans(z0, z1, gaps):
            v.box((xa, floor, a), (xb, belt, b), lower)
        glass = [g for win in _clip(windows, z0, z1) for g in _spans(win[0], win[1], gaps)]
        for a, b in glass:
            ga, gb = sorted((sx * (w - t * 0.7), sx * (w - t * 0.3)))
            v.box((ga, belt, a), (gb, win_top, b), 'glass', obj='window')
        for a, b in _spans(z0, z1, list(windows) + gaps):
            v.box((xa, belt, a), (xb, win_top, b), upper)                        # pillars
        v.box((xa, win_top, z0), (xb, top - 0.05, z1), upper)                    # header
    # the front: a wall to the belt, then the windshield (or a wall, or nothing)
    if front != 'open':
        v.box((-w + t, floor, z1 - t), (w - t, fb, z1), lower)
        if front == 'windshield':
            v.box((-w + t, fb, z1 - t * 0.7), (w - t, win_top, z1 - t * 0.3), 'glass',
                  obj='window')
            v.box((-w + t, win_top, z1 - t), (w - t, top - 0.05, z1), upper)
        else:
            v.box((-w + t, fb, z1 - t), (w - t, top - 0.05, z1), upper)
    if rear == 'wall':
        v.box((-w + t, floor, z0), (w - t, top - 0.05, z0 + t), lower)
    elif rear == 'window':
        v.box((-w + t, floor, z0), (w - t, belt, z0 + t), lower)
        v.box((-w + t, belt, z0 + t * 0.3), (w - t, win_top, z0 + t * 0.7), 'glass', obj='window')
        v.box((-w + t, win_top, z0), (w - t, top - 0.05, z0 + t), upper)
    if driver_x is not None:
        v.box((-w + t, fb - 0.3, z1 - 0.5), (w - t, fb - 0.02, z1 - t), 'black')  # dash
        # the steering wheel: a ring, so the road shows through it
        wx0, wx1, wy0, wy1, wz = driver_x - 0.16, driver_x + 0.16, fb - 0.2, fb + 0.08, z1 - 0.6
        for lo, hi in (((wx0, wy0, wz - 0.02), (wx1, wy0 + 0.035, wz + 0.02)),
                       ((wx0, wy1 - 0.035, wz - 0.02), (wx1, wy1, wz + 0.02)),
                       ((wx0, wy0 + 0.035, wz - 0.02), (wx0 + 0.035, wy1 - 0.035, wz + 0.02)),
                       ((wx1 - 0.035, wy0 + 0.035, wz - 0.02), (wx1, wy1 - 0.035, wz + 0.02))):
            v.box(lo, hi, 'black')


def fire_engine():
    """A custom-cab pumper: tilt cab, compartments with roll-up doors, a hose bed, a pump panel."""
    v = Vehicle()
    g = -0.55                                  # the ground, below the axle
    w = 1.25
    v.box((-0.5, -0.15, -2.3), (0.5, 0.25, 6.9), 'black')                      # frame
    v.box((-w, 0.25, 3.65), (w, 0.4, 6.75), 'paint')                           # cab sill
    cabin(v, w, 0.4, 1.55, 2.55, 3.65, 6.75, windows=[(3.75, 4.55), (4.65, 6.55)],
          win_top=2.4, driver_x=0.6)
    v.box((-w + 0.05, 2.55, 3.7), (w - 0.05, 2.62, 6.7), 'paint2')             # white roof
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
    v.box((-1.0, 0.2, 2.95), (1.0, 0.3, 4.0), 'paint2')                        # cab sill
    cabin(v, 1.0, 0.3, 1.05, 2.05, 2.95, 4.0, lower='paint2', windows=[(3.05, 3.9)],
          win_top=1.95, rear='open', driver_x=0.45)
    v.box((-0.95, 0.2, 4.0), (0.95, 1.0, 5.0), 'paint2')                       # hood
    v.box((-0.6, 0.35, 5.0), (0.6, 0.85, 5.02), 'grille', faces=('s',))
    v.box((-1.0, 0.05, 5.0), (1.0, 0.3, 5.2), 'black')
    v.both((1.0, 1.4, 3.85), (1.2, 1.75, 3.95), 'black')
    # the patient module
    w = 1.15
    v.box((-w, 0.25, -1.95), (w, 0.3, 2.9), 'paint2')                          # module sill
    # hollow: the attendant rides in it; a pass-through window to the cab, the rear windows
    cabin(v, w, 0.3, 1.4, 2.6, -1.95, 2.9, lower='paint2', win_top=2.2, front_belt=1.1,
          rear='window')
    v.both((w + 0.01, 0.9, -1.95), (w + 0.02, 1.2, 2.9), 'stripe', faces=('e', 'w'))
    v.both((1.0, 0.9, 2.95), (1.01, 1.2, 4.0), 'stripe', faces=('e', 'w'))
    v.both((w + 0.01, 1.35, -0.4), (w + 0.02, 1.85, 1.1), 'decal', faces=('e', 'w'))
    v.both((w, 0.35, 1.2), (w + 0.01, 2.3, 2.6), 'door', faces=('e', 'w'))   # side entry
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
    v.box((-w, -0.05, 2.85), (w, 0.75, 4.1), 'paint2')                        # engine bay
    v.box((-w, -0.05, -1.05), (w, 0.75, -0.95), 'paint2')                     # rear sill
    v.box((-w, -0.05, -0.95), (w, 0.0, 2.85), 'paint2')                       # underbody
    v.box((-w + 0.02, 0.75, 2.9), (w - 0.02, 0.95, 4.1), 'paint')             # hood
    # the cabin: lower body in the second colour, the greenhouse in the first
    cabin(v, w, 0.05, 0.95, 1.62, -0.95, 2.85, lower='paint2', upper='paint',
          windows=[(-0.85, 1.2), (1.3, 2.75)], win_top=1.52, rear='window', driver_x=0.42)
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
    v.box((-w, 0.25, 4.65), (w, 0.4, 7.75), 'paint')                           # cab sill
    cabin(v, w, 0.4, 1.55, 2.55, 4.65, 7.75, windows=[(4.75, 5.55), (5.65, 7.55)],
          win_top=2.4, driver_x=0.6)
    v.box((-w + 0.05, 2.55, 4.7), (w - 0.05, 2.62, 7.7), 'paint2')
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
    v.box((-w, 0.25, back), (w, 0.4, hood), 'paint')                           # cab sill
    cabin(v, w, 0.4, 1.5, 2.45, back, hood, windows=[(back + 0.2, hood - 0.15)], win_top=2.35,
          rear='window', driver_x=0.45)
    v.box((-w + 0.05, 2.45, back + 0.05), (w - 0.05, 2.5, hood - 0.05), 'paint2')
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


def trailer_lights(v, rear_z, x_out, y):
    """A trailer's tail, brake and turn lamps, lit from the towing vehicle (its hookup
    variables carry the running lights, brakes and turn signals over the hitch)."""
    brake = [{'animationType': 'translation', 'axis': [0, 0.5, 0], 'variable': 'running_light'},
             {'animationType': 'translation', 'axis': [0, 1, 0], 'variable': 'brake'}]
    back = ('n', 'e', 'w', 'u', 'd')
    for side, sx in (('L', 1), ('R', -1)):
        xs = sorted((sx * (x_out - 0.05), sx * (x_out - 0.25)))
        v.lamp('Brake' + side, (xs[0], y, rear_z - 0.03), (xs[1], y + 0.14, rear_z),
               'tail', '#FF0000', brake, [0, 0, -1], faces=back)
        v.lamp('Turn' + side, (xs[0], y + 0.16, rear_z - 0.03), (xs[1], y + 0.26, rear_z),
               'amber', '#FFA000',
               [vp.visible_on('left_turn_signal' if sx > 0 else 'right_turn_signal'),
                vp.lit_by('10_10_0_cycle')], [0, 0, -1], faces=back)


def trailer_tongue(v, front_z, coupler_z, y=0.0):
    """An A-frame tongue from the front of a trailer's frame to its coupler."""
    for sx in (1, -1):
        xs = sorted((sx * 0.08, sx * 0.45))
        v.box((xs[0], y, front_z), (xs[1], y + 0.12, front_z + 0.5), 'black')
    v.box((-0.08, y, front_z + 0.5), (0.08, y + 0.12, coupler_z - 0.2), 'black')
    v.box((-0.11, y - 0.02, coupler_z - 0.2), (0.11, y + 0.18, coupler_z), 'chrome')  # coupler
    v.box((0.12, y - 0.45, front_z + 0.75), (0.2, y + 0.3, front_z + 0.83), 'black')  # jack


# The chipper trailer: the chip box sits over the axle, its intake 3.9 blocks behind it (the
# chip box's own sub-part offsets, shared with the chip bed), so the body is laid out round them.
CHIP_BOX_Z = 0.8          # the chip box part's centre on the trailer
CHIPPER_LEVER = (0.74, 0.75, -0.55)   # the start lever, on the left of the engine housing


def chipper_trailer():
    """A towed brush chipper: tongue and coupler at the front, the chip hopper over the axle,
    the engine, the drum with its discharge chute blowing forward into the hopper, and the
    infeed hood and feed table at the back, where the crew drops the brush. A lever on the
    engine housing starts it (CHIPPER): the panel cannot, since nobody rides in a trailer."""
    v = Vehicle()
    g = -0.4
    v.box((-0.45, -0.05, -2.6), (0.45, 0.12, 1.75), 'black')                  # frame
    trailer_tongue(v, 1.75, 3.45)
    v.both((0.82, 0.3, -1.0), (1.12, 0.38, -0.05), 'black')                    # fenders
    v.both((1.08, -0.05, -1.0), (1.12, 0.3, -0.92), 'black')
    # the hopper: floor, walls, and the chips heaped inside
    h0, h1 = CHIP_BOX_Z - 0.85, CHIP_BOX_Z + 0.85
    v.box((-0.82, 0.12, h0), (0.82, 0.2, h1), 'paint')
    v.both((0.74, 0.2, h0), (0.82, 1.45, h1), 'paint')
    v.box((-0.74, 0.2, h1 - 0.08), (0.74, 1.6, h1), 'paint')
    v.box((-0.74, 0.2, h0), (0.74, 1.3, h0 + 0.08), 'paint')
    v.box((-0.74, 0.2, h0 + 0.08), (0.74, 1.0, h1 - 0.08), 'mulch', faces=('u',))
    v.both((0.82, 0.9, h0 + 0.1), (0.835, 1.15, h1 - 0.1), 'decal', faces=('e', 'w'))
    # the engine housing behind the hopper, vents on its sides
    v.box((-0.7, 0.12, -1.15), (0.7, 1.05, h0), 'paint2')
    v.both((0.7, 0.35, -1.05), (0.71, 0.85, h0 - 0.15), 'grille', faces=('e', 'w'))
    v.box((-0.1, 1.05, -0.95), (0.1, 1.45, -0.75), 'black')                    # exhaust
    # the start lever, with a yellow knob
    lx, ly, lz = CHIPPER_LEVER
    v.box((0.70, ly - 0.1, lz - 0.12), (0.73, ly + 0.1, lz + 0.12), 'black')
    v.box((0.73, ly - 0.04, lz - 0.03), (0.80, ly + 0.25, lz + 0.03), 'chrome')
    v.box((0.71, ly + 0.25, lz - 0.06), (0.83, ly + 0.33, lz + 0.06), 'blade')
    # the drum housing and the discharge chute, up and forward over the hopper
    v.box((-0.55, 0.15, -1.95), (0.55, 1.2, -1.15), 'paint')
    v.box((-0.16, 1.2, -1.75), (0.16, 1.95, -1.4), 'paint')
    v.box((-0.16, 1.7, -1.4), (0.16, 1.95, CHIP_BOX_Z + 0.2), 'paint')
    v.box((-0.2, 1.55, CHIP_BOX_Z + 0.2), (0.2, 1.95, CHIP_BOX_Z + 0.32), 'paint')  # deflector
    # the infeed: a hood with a dark throat, the feed table and the control bar round it
    v.both((0.5, 0.25, -2.75), (0.6, 1.05, -1.95), 'paint')
    v.box((-0.6, 1.05, -2.75), (0.6, 1.12, -1.95), 'paint')
    v.box((-0.48, 0.3, -2.0), (0.48, 1.0, -1.95), 'black', faces=('n',))
    v.box((-0.62, 0.25, -3.2), (0.62, 0.32, -1.95), 'hazard')
    v.both((0.55, 0.32, -3.2), (0.62, 0.65, -2.75), 'paint')
    v.box((-0.64, 0.95, -3.15), (0.64, 1.0, -3.05), 'blade')                    # control bar
    v.both((0.6, 0.65, -3.15), (0.64, 0.95, -3.05), 'blade')
    trailer_lights(v, -2.6, 0.95, 0.15)
    return v, g


EQUIPMENT_DECK_Y = 0.42
EQUIPMENT_DECK = (-2.0, 1.8)      # rear and front of the deck


def equipment_trailer():
    """A low equipment trailer: a tread-plate deck between fenders, ramps stood up at the tail,
    and a tie-down point that carries a machine (a mounted tow_flatbed hitch, as the tow
    truck's bed carries a car). Click the tie-down at the front of the deck to load what is
    standing on it."""
    v = Vehicle()
    g = -0.4
    z0, z1 = EQUIPMENT_DECK
    y = EQUIPMENT_DECK_Y
    v.box((-0.45, 0.05, z0), (0.45, 0.22, z1), 'black')                       # frame
    v.box((-0.95, 0.22, z0), (0.95, y, z1), 'deck')
    v.both((0.95, 0.22, z0), (1.0, y + 0.08, z1), 'black')                    # rub rails
    v.both((0.98, 0.3, -1.0), (1.25, 0.55, 0.2), 'black')                     # fenders
    v.both((1.21, 0.0, -1.0), (1.25, 0.3, -0.92), 'black')
    trailer_tongue(v, z1, z1 + 1.6, y=0.05)
    v.box((-0.95, y, z1 - 0.1), (0.95, y + 0.55, z1), 'black')                # headboard
    v.box((-0.12, y, z1 - 0.45), (0.12, y + 0.08, z1 - 0.25), 'hazard')       # tie-down
    for sx in (1, -1):                                                        # ramps, stowed
        xs = sorted((sx * 0.25, sx * 0.8))
        v.box((xs[0], y, z0 - 0.08), (xs[1], y + 1.0, z0), 'deck')
    trailer_lights(v, z0, 0.95, 0.15)
    return v, g


GRIND_PIVOT = (0.0, 0.55, 0.45)    # where the cutter boom pins to the body
GRIND_LOWER = 20                   # degrees the boom drops to put the wheel on the ground


def stump_grinder():
    """A self-propelled stump grinder: a compact body on rubber tracks, the engine, a stand-on
    platform at the back, and the cutter wheel on a boom at the front. GRIND lowers the boom and
    puts the wheel to the stump in front of it (CSM: Vehicles' StumpGrinderMachines grinds it by
    Parks' stump rules); raised, it travels."""
    v = Vehicle()
    g = -0.39
    v.both((0.3, g, -0.85), (0.57, 0.4, 0.85), 'track')
    v.box((-0.3, 0.05, -0.85), (0.3, 0.75, 0.5), 'paint')                     # engine body
    v.box((-0.26, 0.75, -0.6), (0.26, 0.78, 0.3), 'grille', faces=('u',))
    v.box((-0.08, 0.75, -0.75), (0.08, 1.05, -0.62), 'black')                 # exhaust
    v.box((-0.45, 0.0, -1.3), (0.45, 0.08, -0.85), 'deck')                    # platform
    v.box((-0.32, 0.75, -0.9), (0.32, 1.2, -0.8), 'paint2')                   # control tower
    v.box((-0.25, 0.85, -0.91), (0.25, 1.1, -0.9), 'black', faces=('n',))
    v.box((-0.35, 1.2, -1.0), (0.35, 1.25, -0.95), 'chrome')                  # handle bar
    v.both((0.32, 1.0, -1.0), (0.35, 1.25, -0.8), 'chrome')
    v.box((-0.28, 0.4, 0.5), (0.28, 0.7, 0.62), 'black')                      # boom mount
    v.group('cutter_boom', [
        ((-0.12, 0.45, 0.45), (0.12, 0.65, 1.45), 'paint'),                   # arm
        ((-0.05, 0.0, 1.25), (0.07, 0.7, 1.95), 'cutter'),                    # the wheel
        ((-0.2, 0.55, 1.2), (0.2, 0.8, 2.0), 'paint'),                        # guard
        ((0.2, 0.25, 1.2), (0.23, 0.8, 2.0), 'paint'),
        ((-0.23, 0.25, 1.2), (-0.2, 0.8, 2.0), 'paint'),
        ((-0.2, 0.25, 1.95), (0.2, 0.55, 2.0), 'rubber'),                     # chip curtain
    ])
    v.lamp('Beacon', (-0.06, 1.25, -0.98), (0.06, 1.37, -0.86), 'lamp_amber', LAMP_LIT['lamp_amber'],
           [vp.visible_on('GRIND'), vp.lit_by('0_5_11_cycle')], [0, 1, 0], 0.4)
    return v, g


def grinder_animations():
    return [{'objectName': 'cutter_boom', 'animations': [
        aerial_animation('rotation', [GRIND_LOWER, 0, 0], (0, 40, 0), GRIND_PIVOT,
                         variable='GRIND')]}]


def trailer_hitch(v):
    """A rear trailer hitch (trailer_standard, the type UNU's hitch bumpers offer), for the work
    trucks that tow the tree crew's trailers. Connected from the panel."""
    rear = min(b[0][2] for b in v.outline)
    return {'groupName': 'Trailer', 'isHitch': True, 'canInitiateConnections': True,
            'connections': [{'type': 'trailer_standard', 'pos': [0, -0.25, round(rear - 0.15, 3)],
                             'distance': 2}]}


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
    v.box((-w, 0.6, rear), (w, 2.75, -2.2), 'paint2')                          # engine bay
    pillars = [-2.75 + k * 1.35 for k in range(9)]
    windows = [(p + 0.1, q) for p, q in zip(pillars, pillars[1:])] + [(pillars[-1] + 0.1, 9.6)]
    cabin(v, w, 0.6, 1.25, 2.75, -2.2, front, lower='paint2', windows=windows, win_top=2.4,
          front_belt=0.9, right_gaps=[(3.2, 4.3), (8.35, 9.4)], driver_x=0.75)
    v.box((-w + 0.05, 2.75, rear + 0.1), (w - 0.05, 2.85, front - 0.1), 'paint')
    v.box((-0.8, 2.85, 0.5), (0.8, 3.15, 4.0), 'chrome')                       # air conditioner
    v.both((w, 0.62, rear), (w + 0.01, 0.82, front), 'stripe', faces=('e', 'w'))
    v.both((w + 0.01, 0.9, 0.6), (w + 0.02, 1.2, 3.0), 'decal', faces=('e', 'w'))
    v.box((-0.95, 2.45, front), (0.95, 2.7, front + 0.03), 'sign', faces=('s',))
    v.box((-0.9, 2.45, rear - 0.02), (0.9, 2.65, rear), 'sign', faces=('n',))
    v.box((-0.7, -0.15, front), (0.7, 0.2, front + 0.35), 'black')             # bike rack
    v.box((-0.12, 2.85, front - 0.35), (0.12, 2.97, front - 0.1), 'black')     # TSP emitter
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
# Pickups: civilian half-ton crew cabs
# ------------------------------------------------------------------------------------------

PICKUP_FRONT, PICKUP_REAR = 4.63, -1.25   # a 5.9 m truck on a 3.68 m wheelbase
PICKUP_CAB = (0.5, 3.0)                   # the cab's back wall and the cowl
PICKUP_BED_FRONT = 0.45                   # a 5.5 ft bed, from here back to the tailgate
PICKUP_ARCHES = [(-0.48, 0.48), (3.2, 4.16)]   # the wheel openings (axles at 0 and 3.68)
PICKUP_SEATS = [(0.42, 0.35, 1.6, True), (-0.42, 0.35, 1.6, False),
                (0.42, 0.35, 0.85, False), (-0.42, 0.35, 0.85, False)]


def raked_cab(v, w, belt, top, rake, steps=6):
    """A crew cab whose windshield leans back `rake` from the cowl to the roof, as a real
    truck's does rather than standing upright like the fleet's cab-overs. The greenhouse behind
    the windshield is the ordinary hollow cabin; the windshield, the A-pillars and the glass
    under them are written as `steps` steps."""
    c0, c1 = PICKUP_CAB
    t = WALL
    roof_front = c1 - rake
    win_top = top - 0.11
    v.box((-w, 0.0, c0), (w, 0.1, c1), 'paint')                               # sill
    cabin(v, w, 0.1, belt, top, c0, roof_front, windows=[(c0 + 0.15, 1.6), (1.7, roof_front)],
          win_top=win_top, front='open', rear='window')
    for sx in (1, -1):
        xa, xb = sorted((sx * (w - t), sx * w))
        v.box((xa, 0.1, roof_front), (xb, belt, c1), 'paint')                 # cowl sides
    v.box((-w + t, 0.1, c1 - t), (w - t, belt, c1), 'paint')                  # firewall
    v.box((-w + t, win_top, roof_front - t), (w - t, top - 0.05, roof_front), 'paint')
    for i in range(steps):
        y0 = belt + (win_top - belt) * i / steps
        y1 = belt + (win_top - belt) * (i + 1) / steps
        za, zb = c1 - rake * (i + 1) / steps, c1 - rake * i / steps
        v.box((-w + t, y0, za), (w - t, y1, zb), 'glass', obj='window')       # windshield
        for sx in (1, -1):
            xa, xb = sorted((sx * (w - t), sx * w))
            v.box((xa, y0, za), (xb, y1, zb), 'paint')                        # A-pillar
            if za - roof_front > 0.005:
                ga, gb = sorted((sx * (w - t * 0.7), sx * (w - t * 0.3)))
                v.box((ga, y0, roof_front), (gb, y1, za), 'glass', obj='window')
    # the dash, under the windshield, and the steering wheel: a ring, so the road shows through
    v.box((-w + t, belt - 0.3, roof_front - 0.3), (w - t, belt - 0.02, c1 - t), 'black')
    dx, wz = 0.42, roof_front - 0.38
    wx0, wx1, wy0, wy1 = dx - 0.16, dx + 0.16, belt - 0.2, belt + 0.08
    for lo, hi in (((wx0, wy0, wz - 0.02), (wx1, wy0 + 0.035, wz + 0.02)),
                   ((wx0, wy1 - 0.035, wz - 0.02), (wx1, wy1, wz + 0.02)),
                   ((wx0, wy0 + 0.035, wz - 0.02), (wx0 + 0.035, wy1 - 0.035, wz + 0.02)),
                   ((wx1 - 0.035, wy0 + 0.035, wz - 0.02), (wx1, wy1 - 0.035, wz + 0.02))):
        v.box(lo, hi, 'black')
    return roof_front


def pickup_bed(v, w, rail, arch):
    """The bed: sides cut round the rear wheels, wheel tubs, floor, front wall and tailgate."""
    r, bf = PICKUP_REAR, PICKUP_BED_FRONT
    for sx in (1, -1):
        xa, xb = sorted((sx * (w - WALL), sx * w))
        for a, b in _spans(r, bf, PICKUP_ARCHES[:1]):
            v.box((xa, 0.0, a), (xb, arch, b), 'paint')
        v.box((xa, arch, r), (xb, rail, bf), 'paint')
        ca, cb = sorted((sx * (w - 0.07), sx * w))
        v.box((ca, rail, r), (cb, rail + 0.02, bf), 'black')                  # rail cap
        ta, tb = sorted((sx * 0.6, sx * (w - WALL)))
        v.box((ta, 0.45, PICKUP_ARCHES[0][0]), (tb, 0.72, PICKUP_ARCHES[0][1]), 'black')
    v.box((-w + WALL, 0.4, r + WALL), (w - WALL, 0.45, bf - WALL), 'black')    # bed floor
    v.box((-w + WALL, 0.45, bf - WALL), (w - WALL, rail, bf), 'paint')         # front wall
    v.box((-w + WALL, 0.15, r), (w - WALL, rail, r + WALL), 'paint')           # tailgate
    v.box((-0.12, rail - 0.12, r - 0.01), (0.12, rail - 0.07, r), 'trim', faces=('n',))
    v.box((-0.25, 0.02, r - 0.16), (0.25, 0.17, r - 0.15), 'plate', faces=('n',))


def front_fenders(v, w, arch, top, c1):
    """The front: fenders round the front wheels, the engine bay between them, the wheel
    wells' black lining, and the hood box up to `top`."""
    a0, a1 = PICKUP_ARCHES[1]
    for a, b in _spans(c1, PICKUP_FRONT, [(a0, a1)]):
        v.box((-w, 0.0, a), (w, arch, b), 'paint')
    v.box((-0.6, 0.0, a0), (0.6, arch, a1), 'black')
    v.box((-w + 0.01, arch - 0.03, a0), (w - 0.01, arch - 0.005, a1), 'black')


def pickup_lights(v, f, r, head, turn_front, tails):
    """Headlights `head` and front turn lamps `turn_front` as (x0, x1, y0, y1) on the left (+x)
    side, mirrored to the right; `tails` as (kind, (x0, x1, y0, y1)) for kind 'brake', 'turn'
    or 'reverse', also given for the left side. IV's own variables, as road_lights uses."""
    front = ('s', 'e', 'w', 'u', 'd')
    back = ('n', 'e', 'w', 'u', 'd')
    brake = [{'animationType': 'translation', 'axis': [0, 0.5, 0], 'variable': 'running_light'},
             {'animationType': 'translation', 'axis': [0, 1, 0], 'variable': 'brake'}]
    for side, sx in (('L', 1), ('R', -1)):
        def xs(box):
            return sorted((sx * box[0], sx * box[1]))
        hx = xs(head)
        v.lamp('Headlight' + side, (hx[0], head[2], f), (hx[1], head[3], f + 0.03),
               'headlight', '#FFF6E0', [vp.visible_on('headlight')], [0, 0, 1], 0.6, faces=front)
        signal = [vp.visible_on('left_turn_signal' if sx > 0 else 'right_turn_signal'),
                  vp.lit_by('10_10_0_cycle')]
        tx = xs(turn_front)
        v.lamp('TurnFront' + side, (tx[0], turn_front[2], f), (tx[1], turn_front[3], f + 0.03),
               'amber', '#FFA000', signal, [0, 0, 1], faces=front)
        for k, (kind, box) in enumerate(tails):
            bx = xs(box)
            lo, hi = (bx[0], box[2], r - 0.03), (bx[1], box[3], r)
            if kind == 'brake':
                v.lamp('Brake' + side, lo, hi, 'tail', '#FF0000', brake, [0, 0, -1], faces=back)
            elif kind == 'turn':
                v.lamp('Turn' + side, lo, hi, 'amber', '#FFA000', signal, [0, 0, -1],
                       faces=back)
            else:
                v.lamp('Reverse' + side, lo, hi, 'white', '#FFFFFF',
                       [vp.lit_by('engine_reversed_1')], [0, 0, -1], faces=back)


def pickup_h150():
    """The Halden H-150, after a 2018 F-150 crew cab: a hood that steps down to its nose with
    two raised ribs along it; a tall grille crossed by two bars, the headlights set in a C of
    lens trim that runs down beside the grille; round-cornered wheel openings; a crease low
    along the sides; tall C-shaped tail lamps and the maker's name stamped across the
    tailgate; the model's badge on the front doors. The `trim` cell (bumpers, bars, handles)
    is chrome or black by livery, so one model is both a work-truck XL and a chrome XLT."""
    v = Vehicle()
    g = -0.4
    w = 1.0
    f, r = PICKUP_FRONT, PICKUP_REAR
    c0, c1 = PICKUP_CAB
    arch, rail = 0.55, 0.9
    v.box((-0.8, -0.12, r + 0.1), (0.8, 0.0, f - 0.2), 'black')                # frame
    front_fenders(v, w, arch, rail, c1)
    v.box((-w, arch, c1), (w, rail - 0.04, f - 0.35), 'paint')                # hood
    v.box((-w, arch, f - 0.35), (w, rail - 0.08, f), 'paint')                 # nose, stepped
    for sx in (1, -1):
        xa, xb = sorted((sx * 0.3, sx * 0.36))
        v.box((xa, rail - 0.04, c1 + 0.05), (xb, rail - 0.025, f - 0.4), 'paint')  # ribs
    roof_front = raked_cab(v, w, 0.95, 1.56, 0.55)
    pickup_bed(v, w, rail, arch)
    # round-cornered wheel openings: the top corners filled in
    for a0, a1 in PICKUP_ARCHES:
        for z0, z1 in ((a0, a0 + 0.12), (a1 - 0.12, a1)):
            v.both((w - WALL, arch - 0.1, z0), (w, arch, z1), 'paint')
            v.both((w - WALL, arch - 0.16, z0 + (0.06 if z0 == a0 else 0)),
                   (w, arch - 0.1, z1 - (0.06 if z1 == a1 else 0)), 'paint')
    # the sides
    for z in (1.3, c1 - 0.85):
        v.both((w, 0.8, z), (w + 0.012, 0.84, z + 0.14), 'trim', faces=('e', 'w'))
    v.both((w, 0.92, roof_front + 0.04), (w + 0.2, 1.14, roof_front + 0.14), 'black')
    for a, b in _spans(r, f, PICKUP_ARCHES):
        v.both((w, 0.3, a), (w + 0.01, 0.33, b), 'paint')                     # body crease
        v.both((w, 0.12, a), (w + 0.008, 0.2, b), 'stripe', faces=('e', 'w'))
    v.both((w, 0.36, c0 + 0.35), (w + 0.012, 0.74, c0 + 1.75), 'decal', faces=('e', 'w'))
    v.both((w, 0.6, c1 - 0.48), (w + 0.012, 0.68, c1 - 0.16), 'script', faces=('e', 'w'))
    # the front end
    v.box((-0.6, 0.3, f), (0.6, rail - 0.1, f + 0.02), 'mesh', faces=('s',))
    v.box((-0.64, rail - 0.1, f), (0.64, rail - 0.06, f + 0.05), 'trim')       # surround
    v.box((-0.64, 0.28, f), (0.64, 0.31, f + 0.05), 'trim')
    for y0, y1 in ((0.42, 0.48), (0.6, 0.67)):
        v.box((-0.62, y0, f + 0.02), (0.62, y1, f + 0.05), 'trim')             # bars
    v.box((-0.11, 0.495, f + 0.05), (0.11, 0.595, f + 0.06), 'badge', faces=('s',))
    for sx in (1, -1):                                                         # the C trim
        xa, xb = sorted((sx * 0.62, sx * 0.665))
        v.box((xa, 0.38, f), (xb, rail - 0.1, f + 0.035), 'chrome')
        xa, xb = sorted((sx * 0.62, sx * 0.97))
        v.box((xa, 0.38, f), (xb, 0.43, f + 0.035), 'chrome')
    v.box((-w, 0.0, f), (w, 0.28, f + 0.12), 'trim')                          # bumper
    v.box((-0.8, -0.06, f - 0.05), (0.8, 0.0, f + 0.08), 'black')             # air dam
    pickup_lights(v, f, r, (0.68, 0.97, 0.6, 0.8), (0.68, 0.97, 0.45, 0.57),
                  [('brake', (0.88, 0.99, 0.45, 0.86)), ('brake', (0.76, 0.88, 0.78, 0.86)),
                   ('turn', (0.76, 0.88, 0.45, 0.53)), ('reverse', (0.8, 0.88, 0.58, 0.7))])
    # the tailgate's stamped name
    v.box((-0.72, 0.5, r - 0.006), (0.72, 0.74, r), 'gate', faces=('n',))
    return v, g


def pickup_silverline():
    """The Chevrolay Silverline 1500, after a 2015 Silverado crew cab: a taller, flatter front
    with a power dome down the hood; a two-tier grille split by a heavy chrome bar that runs
    out through the headlights, which are square and stacked; square wheel openings under
    flares that stand proud of the body; a shoulder crease the length of the truck; tall
    mirrors and a step under the doors; stepped tail lamps, a spoiler lip on the tailgate and
    the badge in its middle; the model's name on the front doors."""
    v = Vehicle()
    g = -0.4
    w = 1.0
    f, r = PICKUP_FRONT, PICKUP_REAR
    c0, c1 = PICKUP_CAB
    arch, rail = 0.58, 0.98
    v.box((-0.8, -0.12, r + 0.1), (0.8, 0.0, f - 0.2), 'black')                # frame
    front_fenders(v, w, arch, rail, c1)
    v.box((-w, arch, c1), (w, rail - 0.01, f), 'paint')                       # hood, flat
    v.box((-0.42, rail - 0.01, c1 + 0.05), (0.42, rail + 0.015, f - 0.06), 'paint')  # dome
    roof_front = raked_cab(v, w, 1.0, 1.6, 0.5)
    pickup_bed(v, w, rail, arch)
    # square wheel openings under flares that stand proud of the body
    for a0, a1 in PICKUP_ARCHES:
        v.both((w, arch, a0 - 0.08), (w + 0.04, arch + 0.08, a1 + 0.08), 'paint')
        v.both((w, 0.06, a0 - 0.08), (w + 0.04, arch, a0), 'paint')
        v.both((w, 0.06, a1), (w + 0.04, arch, a1 + 0.08), 'paint')
    # the sides: the shoulder crease, handles, mirrors, the step
    for a, b in _spans(r, f, [(a0 - 0.08, a1 + 0.08) for a0, a1 in PICKUP_ARCHES]):
        v.both((w, 0.7, a), (w + 0.015, 0.74, b), 'paint')
        v.both((w, 0.12, a), (w + 0.008, 0.2, b), 'stripe', faces=('e', 'w'))
    for z in (1.3, c1 - 0.85):
        v.both((w, 0.84, z), (w + 0.012, 0.88, z + 0.14), 'trim', faces=('e', 'w'))
    v.both((w, 0.98, roof_front + 0.02), (w + 0.26, 1.3, roof_front + 0.14), 'paint')
    v.both((w - 0.1, 0.02, c0 + 0.1), (w + 0.06, 0.07, c1 - 0.1), 'trim')    # step
    v.both((w, 0.5, c1 - 0.58), (w + 0.012, 0.58, c1 - 0.26), 'script', faces=('e', 'w'))
    # the front end: two grille tiers, the bar through the headlights
    v.box((-0.6, 0.7, f), (0.6, rail - 0.04, f + 0.02), 'mesh', faces=('s',))
    v.box((-0.6, 0.36, f), (0.6, 0.63, f + 0.02), 'mesh', faces=('s',))
    v.box((-0.98, 0.63, f + 0.02), (0.98, 0.7, f + 0.06), 'trim')             # the bar
    v.box((-0.62, rail - 0.04, f), (0.62, rail - 0.01, f + 0.04), 'trim')     # surround
    v.box((-0.62, 0.33, f), (0.62, 0.36, f + 0.04), 'trim')
    for sx in (1, -1):
        xa, xb = sorted((sx * 0.6, sx * 0.63))
        v.box((xa, 0.33, f), (xb, rail - 0.01, f + 0.04), 'trim')
        xa, xb = sorted((sx * 0.64, sx * 0.8))
        v.box((xa, 0.5, f), (xb, 0.63, f + 0.025), 'headlight', faces=('s',))  # lower lamp
    v.box((-0.13, 0.6, f + 0.06), (0.13, 0.73, f + 0.07), 'badge', faces=('s',))
    v.box((-w, 0.0, f), (w, 0.33, f + 0.15), 'trim')                          # bumper
    v.box((-w + 0.05, 0.12, f + 0.15), (w - 0.05, 0.16, f + 0.17), 'black')   # step pad
    for sx in (1, -1):
        xa, xb = sorted((sx * 0.55, sx * 0.75))
        v.box((xa, 0.18, f + 0.15), (xb, 0.26, f + 0.155), 'headlight', faces=('s',))  # fogs
    v.box((-0.85, -0.06, f - 0.05), (0.85, 0.0, f + 0.1), 'black')            # air dam
    pickup_lights(v, f, r, (0.64, 0.97, 0.72, 0.94), (0.8, 0.97, 0.5, 0.63),
                  [('brake', (0.8, 0.99, 0.62, 0.92)), ('brake', (0.88, 0.99, 0.5, 0.62)),
                   ('turn', (0.8, 0.88, 0.5, 0.56)), ('reverse', (0.8, 0.88, 0.56, 0.62))])
    # the tailgate: a spoiler lip and the badge
    v.box((-w + WALL, rail - 0.02, r - 0.05), (w - WALL, rail + 0.02, r), 'black')
    v.box((-0.11, 0.6, r - 0.008), (0.11, 0.72, r), 'badge', faces=('n',))
    return v, g


# The utility cap: a part for the pickups' bed slot, so it goes on any of them, those already
# on the road included. Its origin is the slot's: the bed's middle, on top of the bed rails.
PICKUP_CAP_SLOT = (0.0, 0.9, round((PICKUP_REAR + PICKUP_BED_FRONT) / 2, 3))
PICKUP_CAP_TYPE = 'generic_csm_pickup_cap'
PICKUP_CAP_LIVERY = ('', 'Utility Cap', '#ECECE8', '#D6D6D2', '#ECECE8', '', '#ECECE8')


def pickup_cap():
    """A commercial bed cap: a white shell on the bed rails with a compartment door down each
    side and a rear door, an aluminium ladder rack and an amber beacon on its front corner. The
    beacon runs on BEACONS, the work trucks' switch, which never preempts a signal."""
    v = Vehicle()
    w = 1.0
    half = (PICKUP_BED_FRONT - PICKUP_REAR) / 2
    r, f = -half, half - 0.02
    top = 0.72
    v.box((-w, 0.02, r), (w, 0.05, f), 'black')                               # rail seal
    v.box((-w, 0.05, r), (w, top, f), 'paint')                                # shell
    v.both((w, 0.12, r + 0.1), (w + 0.008, top - 0.1, f - 0.23), 'paint2', faces=('e', 'w'))
    v.both((w + 0.008, 0.34, f - 0.38), (w + 0.02, 0.4, f - 0.28), 'chrome')  # T-handles
    v.box((-0.9, 0.1, r - 0.008), (0.9, top - 0.06, r), 'paint2', faces=('n',))  # rear door
    v.box((-0.08, 0.36, r - 0.02), (0.08, 0.42, r - 0.008), 'chrome')
    brake = [{'animationType': 'translation', 'axis': [0, 1, 0], 'variable': 'brake'}]
    v.lamp('CapBrake', (-0.2, top - 0.05, r - 0.012), (0.2, top - 0.01, r), 'tail', '#FF0000',
           brake, [0, 0, -1], faces=('n', 'e', 'w', 'u', 'd'))
    # the ladder rack: four uprights, two crossbars, a rail down each side
    for z in (r + 0.15, f - 0.15):
        v.both((0.85, top, z - 0.04), (0.9, top + 0.14, z + 0.04), 'chrome')
        v.box((-0.92, top + 0.14, z - 0.06), (0.92, top + 0.18, z + 0.06), 'chrome')
    v.both((0.86, top + 0.18, r + 0.05), (0.92, top + 0.22, f - 0.05), 'chrome')
    # the beacon, on the driver's front corner of the rack
    bz = f - 0.15
    v.box((0.55, top + 0.18, bz - 0.06), (0.7, top + 0.21, bz + 0.06), 'black')
    v.lamp('Beacon', (0.57, top + 0.21, bz - 0.05), (0.68, top + 0.33, bz + 0.05),
           'lamp_amber', LAMP_LIT['lamp_amber'],
           [vp.visible_on('BEACONS'), vp.lit_by('0_5_11_cycle')], [0, 0, 1], 0.4)
    return v


# ------------------------------------------------------------------------------------------
# Cars: the Subaroo hatchback and sedan
# ------------------------------------------------------------------------------------------

CAR_SEATS = [(0.36, 0.2, 0.75, True), (-0.36, 0.2, 0.75, False),
             (0.36, 0.2, -0.05, False), (-0.36, 0.2, -0.05, False)]


def rake(v, w, belt, win_top, z_foot, z_head, steps, side_glass):
    """A raked window from its foot (the belt line at z_foot) to its head (the window top at
    z_head), as steps: the glass across, a pillar each side, and under the pillar, back to
    z_head, side glass or (with `side_glass` false) body."""
    t = WALL
    for i in range(steps):
        y0 = belt + (win_top - belt) * i / steps
        y1 = belt + (win_top - belt) * (i + 1) / steps
        za = z_foot + (z_head - z_foot) * i / steps
        zb = z_foot + (z_head - z_foot) * (i + 1) / steps
        lo, hi = sorted((za, zb))
        v.box((-w + t, y0, lo), (w - t, y1, hi), 'glass', obj='window')
        for sx in (1, -1):
            xa, xb = sorted((sx * (w - t), sx * w))
            v.box((xa, y0, lo), (xb, y1, hi), 'paint')
            fa, fb = sorted((zb, z_head))
            if fb - fa > 0.005:
                if side_glass:
                    ga, gb = sorted((sx * (w - t * 0.7), sx * (w - t * 0.3)))
                    v.box((ga, y0, fa), (gb, y1, fb), 'glass', obj='window')
                else:
                    v.box((xa, y0, fa), (xb, y1, fb), 'paint')


def solid_section(v, w, z0, z1, bottom, top, arches, arch_top):
    """A closed piece of body from z0 to z1 (the nose, the tail): the wheel openings cut from
    it below arch_top, the wheel well between them in black."""
    for a, b in _spans(z0, z1, arches):
        v.box((-w, bottom, a), (w, arch_top, b), 'paint')
    for a, b in _clip(arches, z0, z1):
        v.box((-0.55, bottom, a), (0.55, arch_top, b), 'black')
        v.box((-w + 0.01, arch_top - 0.03, a), (w - 0.01, arch_top - 0.005, b), 'black')
    if top > arch_top:
        v.box((-w, arch_top, z0), (w, top, z1), 'paint')


def car_cabin(v, c):
    """A car's hollow cabin from the foot of its rear glass (zf) to the cowl (zc): the floor and
    sills cut round the wheels, wheel tubs inside, the sides, the side windows between the roof's
    ends (zr, zfr), the roof, the windshield and rear glass raked, the dash and a wheel."""
    w, t = c['w'], WALL
    floor, belt, win_top, top = c['floor'], c['belt'], c['win_top'], c['top']
    zf, zr, zfr, zc = c['zf'], c['zr'], c['zfr'], c['zc']
    arches, arch_top = c['arches'], c['arch_top']
    cut = _clip(arches, zf, zc)
    v.box((-0.55, c['rocker'], zf), (0.55, floor, zc), 'black')               # underbody
    for a, b in _spans(zf, zc, cut):
        v.box((-w, c['rocker'], a), (w, floor, b), 'paint')                   # sills
        v.box((-w + t, floor - 0.04, a), (w - t, floor + 0.01, b), 'black')   # floor
    v.box((-0.55, floor - 0.04, zf), (0.55, floor + 0.01, zc), 'black')
    for a, b in cut:
        for sx in (1, -1):
            ta, tb = sorted((sx * 0.55, sx * (w - t)))
            v.box((ta, c['rocker'], a), (tb, arch_top, b), 'black')           # wheel tub
    for sx in (1, -1):
        xa, xb = sorted((sx * (w - t), sx * w))
        for a, b in _spans(zf, zc, cut):
            v.box((xa, floor, a), (xb, arch_top, b), 'paint')
        v.box((xa, arch_top, zf), (xb, belt, zc), 'paint')
        for a, b in c['windows']:
            ga, gb = sorted((sx * (w - t * 0.7), sx * (w - t * 0.3)))
            v.box((ga, belt, a), (gb, win_top, b), 'glass', obj='window')
        for a, b in _spans(zr, zfr, c['windows']):
            v.box((xa, belt, a), (xb, win_top, b), 'paint')                   # pillars
        v.box((xa, win_top, zr), (xb, top - 0.05, zfr), 'paint')              # header
    v.box((-w, top - 0.05, zr), (w, top, zfr), 'paint')                       # roof
    v.box((-w + t, win_top, zfr - t), (w - t, top - 0.05, zfr), 'paint')
    v.box((-w + t, win_top, zr), (w - t, top - 0.05, zr + t), 'paint')
    rake(v, w, belt, win_top, zc, zfr, c.get('steps', 5), True)
    rake(v, w, belt, win_top, zf, zr, c.get('steps', 5), c['quarter_glass'])
    # the dash under the windshield and the steering wheel, a ring
    v.box((-w + t, belt - 0.25, zfr - 0.02), (w - t, belt - 0.02, zc), 'black')
    dx, wz = 0.36, zfr - 0.1
    wx0, wx1, wy0, wy1 = dx - 0.15, dx + 0.15, belt - 0.22, belt + 0.05
    for lo, hi in (((wx0, wy0, wz - 0.02), (wx1, wy0 + 0.035, wz + 0.02)),
                   ((wx0, wy1 - 0.035, wz - 0.02), (wx1, wy1, wz + 0.02)),
                   ((wx0, wy0 + 0.035, wz - 0.02), (wx0 + 0.035, wy1 - 0.035, wz + 0.02)),
                   ((wx1 - 0.035, wy0 + 0.035, wz - 0.02), (wx1, wy1 - 0.035, wz + 0.02))):
        v.box(lo, hi, 'black')


def sloped_hood(v, w, arch_top, z0, z1, h0, h1, steps=4):
    """A hood falling from h0 at the cowl (z0) to h1 at the nose (z1), in steps."""
    for i in range(steps):
        a = z0 + (z1 - z0) * i / steps
        b = z0 + (z1 - z0) * (i + 1) / steps
        h = h0 + (h1 - h0) * i / (steps - 1)
        v.box((-w, arch_top, a), (w, h, b), 'paint')


def car_sides(v, w, zf_handles, mirror_z, belt, crease_y, body):
    """Door handles, mirrors and a crease line along the body (z spans `body`)."""
    for z in zf_handles:
        v.both((w, belt - 0.12, z), (w + 0.012, belt - 0.09, z + 0.12), 'chrome',
               faces=('e', 'w'))
    v.both((w, belt, mirror_z), (w + 0.16, belt + 0.13, mirror_z + 0.12), 'paint')
    for a, b in body:
        v.both((w, crease_y, a), (w + 0.01, crease_y + 0.025, b), 'paint')


IMPRESSA = dict(w=0.89, floor=-0.05, rocker=-0.15, belt=0.63, win_top=1.04, top=1.16,
                zf=-0.7, zr=-0.3, zfr=1.25, zc=2.1, arch_top=0.38,
                arches=[(-0.4, 0.4), (2.27, 3.07)], quarter_glass=False,
                windows=[(-0.15, 0.45), (0.55, 1.25)])
IMPRESSA_FRONT, IMPRESSA_REAR = 3.59, -0.87

STX = dict(w=0.9, floor=-0.05, rocker=-0.15, belt=0.6, win_top=1.02, top=1.15,
           zf=-0.45, zr=0.0, zfr=1.25, zc=2.1, arch_top=0.38,
           arches=[(-0.4, 0.4), (2.25, 3.05)], quarter_glass=False,
           windows=[(0.08, 0.5), (0.6, 1.25)])
STX_FRONT, STX_REAR = 3.6, -0.95


def impressa():
    """The Subaroo Impressa 5-door, after a 2019 Impreza hatchback: a long sloped hood, a
    hexagonal grille crossed by a chrome wing that runs out to swept headlights, fog lamps in
    chrome-edged pods, a steep hatch under a roof spoiler, roof rails, and tail lamps that
    wrap the corners."""
    v = Vehicle()
    g = -0.32
    c = IMPRESSA
    w, f, r = c['w'], IMPRESSA_FRONT, IMPRESSA_REAR
    car_cabin(v, c)
    solid_section(v, w, c['zc'], f, c['rocker'], c['arch_top'], c['arches'], c['arch_top'])
    sloped_hood(v, w, c['arch_top'], c['zc'], f, c['belt'] - 0.03, 0.42)
    solid_section(v, w, r, c['zf'], c['rocker'], c['belt'], c['arches'], c['arch_top'])
    # the hatch: a roof spoiler over the glass, the plate, the name
    v.box((-0.78, c['top'] - 0.05, c['zr'] - 0.16), (0.78, c['top'], c['zr']), 'paint')
    v.box((-0.25, 0.15, r - 0.01), (0.25, 0.28, r), 'plate', faces=('n',))
    v.box((-0.3, 0.3, r - 0.005), (0.3, 0.45, r), 'gate', faces=('n',))
    v.box((-0.85, -0.17, r - 0.03), (0.85, -0.04, r + 0.06), 'black')         # diffuser
    for sx in (1, -1):                                                         # roof rails
        xa, xb = sorted((sx * 0.62, sx * 0.68))
        v.box((xa, c['top'], c['zr'] + 0.05), (xb, c['top'] + 0.04, c['zfr'] - 0.05), 'chrome')
    car_sides(v, w, (0.2, 1.05), c['zfr'] + 0.05, c['belt'], 0.42,
              _spans(r, f, c['arches']))
    v.both((w - 0.01, -0.15, 0.45), (w + 0.015, -0.09, 2.22), 'black')         # skirts
    # the front: grille, the chrome wing, headlights, the lower intake and fog lamp pods
    v.box((-0.3, 0.18, f), (0.3, 0.39, f + 0.015), 'mesh', faces=('s',))
    v.box((-0.44, 0.285, f + 0.015), (0.44, 0.315, f + 0.03), 'trim')
    v.box((-0.075, 0.26, f + 0.03), (0.075, 0.34, f + 0.036), 'badge', faces=('s',))
    v.box((-0.45, -0.09, f), (0.45, 0.1, f + 0.01), 'mesh', faces=('s',))
    for sx in (1, -1):
        xa, xb = sorted((sx * 0.55, sx * 0.74))
        v.box((xa, -0.04, f), (xb, 0.06, f + 0.01), 'black', faces=('s',))
        v.box((xa, 0.06, f), (xb, 0.085, f + 0.02), 'chrome')
    v.box((-0.82, -0.18, f - 0.05), (0.82, -0.14, f + 0.03), 'black')         # lip
    pickup_lights(v, f, r, (0.44, 0.86, 0.32, 0.41), (0.7, 0.86, 0.25, 0.31),
                  [('brake', (0.55, 0.88, 0.4, 0.5)), ('brake', (0.8, 0.88, 0.5, 0.56)),
                   ('turn', (0.7, 0.88, 0.33, 0.39)), ('reverse', (0.55, 0.68, 0.33, 0.39))])
    return v, g


def wrz_stx():
    """The Subaroo WRZ STX, after a 2019 WRX STI sedan: a hood scoop, a hexagonal grille with a
    red pinstripe, big intakes either side of the bumper over a black splitter, flared wheel
    arches and side skirts, a trunk carrying a tall rear wing on its uprights, a black
    diffuser with four exhaust tips, and the STX badge in red on the front doors."""
    v = Vehicle()
    g = -0.33
    c = STX
    w, f, r = c['w'], STX_FRONT, STX_REAR
    car_cabin(v, c)
    solid_section(v, w, c['zc'], f, c['rocker'], c['arch_top'], c['arches'], c['arch_top'])
    sloped_hood(v, w, c['arch_top'], c['zc'], f, c['belt'] - 0.03, 0.42)
    deck = c['belt'] + 0.02
    solid_section(v, w, r, c['zf'], c['rocker'], deck, c['arches'], c['arch_top'])
    # the hood scoop, on the hood's second step
    hy = c['belt'] - 0.03 + (0.42 - c['belt'] + 0.03) / 3
    v.box((-0.27, hy, 2.42), (0.27, hy + 0.07, 2.95), 'paint')
    v.box((-0.23, hy + 0.01, 2.95), (0.23, hy + 0.06, 2.955), 'black', faces=('s',))
    # flares round both wheels, side skirts
    for a0, a1 in c['arches']:
        v.both((w, c['arch_top'], a0 - 0.06), (w + 0.03, c['arch_top'] + 0.06, a1 + 0.06),
               'paint')
        v.both((w, -0.1, a0 - 0.06), (w + 0.03, c['arch_top'], a0), 'paint')
        v.both((w, -0.1, a1), (w + 0.03, c['arch_top'], a1 + 0.06), 'paint')
    v.both((w - 0.01, -0.16, 0.46), (w + 0.03, -0.08, 2.19), 'black')
    car_sides(v, w, (0.3, 1.05), c['zfr'] + 0.05, c['belt'], 0.4,
              _spans(r, f, [(a0 - 0.06, a1 + 0.06) for a0, a1 in c['arches']]))
    v.both((w, 0.42, 1.72), (w + 0.012, 0.495, 2.02), 'script', faces=('e', 'w'))
    # the rear wing on its uprights, the trunk's name, plate, diffuser and exhausts
    for sx in (1, -1):
        xa, xb = sorted((sx * 0.56, sx * 0.62))
        v.box((xa, deck, r + 0.12), (xb, deck + 0.22, r + 0.24), 'black')
        xa, xb = sorted((sx * 0.84, sx * 0.86))
        v.box((xa, deck + 0.14, r + 0.02), (xb, deck + 0.3, r + 0.3), 'paint')
    v.box((-0.84, deck + 0.22, r + 0.02), (0.84, deck + 0.26, r + 0.3), 'paint')
    v.box((-0.25, 0.22, r - 0.01), (0.25, 0.35, r), 'plate', faces=('n',))
    v.box((-0.3, 0.38, r - 0.005), (0.3, 0.53, r), 'gate', faces=('n',))
    v.box((-0.8, -0.18, r - 0.04), (0.8, -0.04, r + 0.04), 'black')
    for x in (0.4, 0.56):
        for sx in (1, -1):
            xa, xb = sorted((sx * (x - 0.055), sx * (x + 0.055)))
            v.box((xa, -0.14, r - 0.08), (xb, -0.05, r - 0.04), 'chrome')
    # the front: grille and pinstripe, big intakes, splitter
    v.box((-0.32, 0.18, f), (0.32, 0.39, f + 0.015), 'mesh', faces=('s',))
    v.box((-0.3, 0.205, f + 0.015), (0.3, 0.22, f + 0.02), 'tail')
    v.box((-0.075, 0.27, f + 0.015), (0.075, 0.35, f + 0.021), 'badge', faces=('s',))
    v.box((-0.38, -0.1, f), (0.38, 0.1, f + 0.01), 'mesh', faces=('s',))
    for sx in (1, -1):
        xa, xb = sorted((sx * 0.5, sx * 0.82))
        v.box((xa, -0.1, f), (xb, 0.16, f + 0.01), 'mesh', faces=('s',))
        v.box((xa, 0.16, f), (xb, 0.19, f + 0.03), 'black')
    v.box((-0.86, -0.19, f - 0.06), (0.86, -0.15, f + 0.09), 'black')         # splitter
    pickup_lights(v, f, r, (0.42, 0.86, 0.32, 0.42), (0.7, 0.86, 0.25, 0.31),
                  [('brake', (0.58, 0.88, 0.44, 0.55)), ('brake', (0.5, 0.58, 0.5, 0.55)),
                   ('turn', (0.74, 0.88, 0.39, 0.44)), ('reverse', (0.5, 0.58, 0.44, 0.49))])
    return v, g


def pickup_cap_json(v):
    return {
        'definitions': [{'name': 'CSM Pickup Utility Cap', 'subName': '',
                         'extraMaterialLists': [[]]}],
        'general': {
            'description': 'A work cap for a CSM pickup\'s bed: compartment doors down both '
                           'sides, a ladder rack and an amber beacon on the BEACONS switch (not '
                           'an emergency light, so it does not preempt signals). Fits the '
                           'H-150 and the Silverline 1500.',
            'type': PICKUP_CAP_TYPE,
            'stackSize': 1,
            'materialLists': [['minecraft:iron_ingot:0:8', 'minecraft:quartz_block:0:2',
                               'minecraft:redstone:0:1']]},
        'generic': {'width': 2.0, 'height': 1.0},
        'rendering': {'customVariables': ['BEACONS'], 'lightObjects': v.lights},
    }


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

LIVERIES.update({
    'csm_chipper_trailer': [
        ('_orange', 'Brush Chipper Trailer', '#E8761C', '#2E2E30', '#1C1C1C', 'TREE CREW',
         '#1C1C1C'),
        ('_yellow', 'Brush Chipper Trailer (Yellow)', '#E8C21A', '#2E2E30', '#1C1C1C',
         'TREE CREW', '#1C1C1C'),
        ('_green', 'Brush Chipper Trailer (Green)', '#2E6B3A', '#2E2E30', '#F2F2EE', 'TREE CREW',
         '#F2F2EE'),
    ],
    'csm_equipment_trailer': [
        ('_black', 'Equipment Trailer', '#2A2A2C', '#2A2A2C', '#E8C21A', '', '#2A2A2C'),
        ('_orange', 'Equipment Trailer (Orange)', '#E8761C', '#2A2A2C', '#1C1C1C', '',
         '#E8761C'),
    ],
    'csm_stump_grinder': [
        ('_orange', 'Stump Grinder', '#E8761C', '#2E2E30', '#1C1C1C', '', '#E8761C'),
        ('_yellow', 'Stump Grinder (Yellow)', '#E8C21A', '#2E2E30', '#1C1C1C', '', '#E8C21A'),
        ('_green', 'Stump Grinder (Green)', '#2E6B3A', '#2E2E30', '#F2F2EE', '', '#2E6B3A'),
    ],
})

# The pickups: civilian colours, and a public works truck lettered on its doors.
LIVERIES.update({
    'csm_pickup_h150_xl': [
        ('_white', 'Halden H-150 XL (Oxford White)', '#ECECE8', '#ECECE8', '#ECECE8', '',
         '#ECECE8'),
    ],
    'csm_pickup_h150_xlt': [
        ('_blue', 'Halden H-150 XLT (Blue Jeans)', '#1D3A6E', '#1D3A6E', '#1D3A6E', '',
         '#1D3A6E'),
        ('_black', 'Halden H-150 XLT (Shadow Black)', '#151618', '#151618', '#151618', '',
         '#151618'),
        ('_red', 'Halden H-150 XLT (Race Red)', '#B3121B', '#B3121B', '#B3121B', '',
         '#B3121B'),
        ('_publicworks', 'Halden H-150 (Public Works)', '#ECECE8', '#ECECE8', '#E8701A',
         'PUBLIC WORKS', '#3A3A3C'),
    ],
    'csm_pickup_h150_service': [
        ('_publicworks', 'Halden H-150 Service (Public Works)', '#ECECE8', '#ECECE8', '#E8701A',
         'PUBLIC WORKS', '#3A3A3C'),
        ('_white', 'Halden H-150 Service (White)', '#ECECE8', '#ECECE8', '#ECECE8', '',
         '#ECECE8'),
    ],
    'csm_pickup_silverline': [
        ('_black', 'Chevrolay Silverline 1500 (Black)', '#141517', '#141517', '#141517', '',
         '#141517'),
        ('_blue', 'Chevrolay Silverline 1500 (Deep Ocean Blue)', '#1F3F78', '#1F3F78',
         '#1F3F78', '', '#1F3F78'),
        ('_white', 'Chevrolay Silverline 1500 (Summit White)', '#F0F0EC', '#F0F0EC', '#F0F0EC',
         '', '#F0F0EC'),
        ('_silver', 'Chevrolay Silverline 1500 (Silver Ice)', '#B4B8BE', '#B4B8BE', '#B4B8BE',
         '', '#B4B8BE'),
        ('_red', 'Chevrolay Silverline 1500 (Victory Red)', '#A8141A', '#A8141A', '#A8141A', '',
         '#A8141A'),
    ],
})

# The Subaroos.
LIVERIES.update({
    'csm_subaroo_impressa': [
        ('_black', 'Subaroo Impressa 5-Door (Crystal Black)', '#141518', '#141518', '#141518',
         '', '#141518'),
        ('_blue', 'Subaroo Impressa 5-Door (Lapis Blue)', '#22407E', '#22407E', '#22407E', '',
         '#22407E'),
        ('_red', 'Subaroo Impressa 5-Door (Pure Red)', '#B0151C', '#B0151C', '#B0151C', '',
         '#B0151C'),
        ('_white', 'Subaroo Impressa 5-Door (Crystal White)', '#EEEEEA', '#EEEEEA', '#EEEEEA',
         '', '#EEEEEA'),
        ('_silver', 'Subaroo Impressa 5-Door (Ice Silver)', '#B8BCC2', '#B8BCC2', '#B8BCC2', '',
         '#B8BCC2'),
    ],
    'csm_subaroo_wrz_stx': [
        ('_blue', 'Subaroo WRZ STX (WR Blue)', '#1F4FA8', '#1F4FA8', '#1F4FA8', '', '#1F4FA8'),
        ('_gray', 'Subaroo WRZ STX (Magnetite Gray)', '#3C4148', '#3C4148', '#3C4148', '',
         '#3C4148'),
        ('_red', 'Subaroo WRZ STX (Pure Red)', '#B0151C', '#B0151C', '#B0151C', '', '#B0151C'),
        ('_white', 'Subaroo WRZ STX (Crystal White)', '#EEEEEA', '#EEEEEA', '#EEEEEA', '',
         '#EEEEEA'),
        ('_black', 'Subaroo WRZ STX (Crystal Black)', '#141518', '#141518', '#141518', '',
         '#141518'),
    ],
    'csm_subaroo_wrz_stx_gold': [
        ('_blue', 'Subaroo WRZ STX (WR Blue, Gold Wheels)', '#1F4FA8', '#1F4FA8', '#1F4FA8', '',
         '#1F4FA8'),
    ],
})

# Where each vehicle's door lettering sits: on the main paint, or on the second colour.
DECAL_ON = {'csm_chipper_trailer': 'paint', 'csm_equipment_trailer': 'paint',
            'csm_stump_grinder': 'paint',
            'csm_fire_engine': 'paint', 'csm_ladder_truck': 'paint', 'csm_ambulance': 'paint2',
            'csm_police_suv': 'paint2', 'csm_dpw_truck': 'paint', 'csm_bucket_truck': 'paint',
            'csm_tow_truck': 'paint', 'csm_transit_bus': 'paint2',
            'csm_pickup_h150_xl': 'paint', 'csm_pickup_h150_xlt': 'paint',
            'csm_pickup_silverline': 'paint', 'csm_pickup_h150_service': 'paint',
            'csm_subaroo_impressa': 'paint', 'csm_subaroo_wrz_stx': 'paint',
            'csm_subaroo_wrz_stx_gold': 'paint'}


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def vehicle_texture(livery, decal_on='paint', extras=False, pickup=None):
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
    if text and pickup and pickup.get('seal'):
        # a round seal ringed in the stripe's colour, the lettering beside it
        d.ellipse((x0 + 1, y0 + 2, x0 + 12, y0 + 13), fill=st + (255,))
        d.ellipse((x0 + 3, y0 + 4, x0 + 10, y0 + 11), fill=(40, 70, 120, 255))
        d.rectangle((x0 + 4, y0 + 8, x0 + 9, y0 + 9), fill=(232, 232, 228, 255))
        lc.draw_text_centred(img, text, 39, y0 + 5, rgb(text_colour), 1)
    elif text:
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
    if extras:
        tree_crew_cells(img)
    if pickup:
        pickup_cells(img, pickup)
    return img


def pickup_cells(img, pickup):
    """The pickups' cells: `trim` chrome or black, the grille's honeycomb mesh, and the badge
    (`halden`: a chrome oval with an H; `chevrolay`: a gold diamond edged in chrome)."""
    d = ImageDraw.Draw(img)

    def box(name):
        x0, y0, x1, y1 = CELLS[name]
        return x0, y0, x1 - 1, y1 - 1

    chrome = pickup['trim'] == 'chrome'
    x0, y0, x1, y1 = box('trim')
    d.rectangle((x0, y0, x1, y1), fill=(196, 200, 206, 255) if chrome else (30, 30, 32, 255))
    d.line((x0, y0, x1, y0), fill=(240, 242, 246, 255) if chrome else (52, 52, 56, 255))
    d.line((x0, y1, x1, y1), fill=(150, 154, 160, 255) if chrome else (20, 20, 22, 255))
    x0, y0, x1, y1 = box('mesh')
    d.rectangle((x0, y0, x1, y1), fill=(18, 18, 20, 255))
    for j in range(0, 16, 3):                     # honeycomb: offset rows of cells
        for i in range(0, 16, 4):
            cx = x0 + i + (2 if (j // 3) % 2 else 0)
            d.rectangle((cx, y0 + j, cx + 2, y0 + j + 1), fill=(46, 46, 50, 255))
    x0, y0, x1, y1 = box('badge')
    corner = (196, 200, 206, 255) if pickup['badge'] == 'chevrolay' else (18, 18, 20, 255)
    d.rectangle((x0, y0, x1, y1), fill=corner)
    if pickup['badge'] == 'subaroo':
        d.rectangle((x0, y0, x1, y1), fill=(18, 18, 20, 255))
        d.ellipse((x0, y0 + 2, x1, y1 - 2), fill=(200, 204, 210, 255))
        d.ellipse((x0 + 1, y0 + 3, x1 - 1, y1 - 3), fill=(26, 58, 140, 255))
        for px, py in ((7, 5), (10, 6), (10, 9), (7, 10), (4, 9), (4, 6)):
            d.point((x0 + px, y0 + py), fill=(240, 242, 246, 255))
    elif pickup['badge'] == 'halden':
        d.ellipse((x0, y0 + 2, x1, y1 - 2), fill=(206, 210, 216, 255))
        d.ellipse((x0 + 2, y0 + 4, x1 - 2, y1 - 4), fill=(28, 30, 34, 255))
        d.rectangle((x0 + 5, y0 + 6, x0 + 5, y0 + 9), fill=(220, 224, 230, 255))
        d.rectangle((x0 + 10, y0 + 6, x0 + 10, y0 + 9), fill=(220, 224, 230, 255))
        d.rectangle((x0 + 5, y0 + 7, x0 + 10, y0 + 8), fill=(220, 224, 230, 255))
    else:
        cx, cy = x0 + 7.5, y0 + 7.5
        d.polygon([(cx, y0), (x1, cy), (cx, y1), (x0, cy)], fill=(150, 154, 160, 255))
        d.polygon([(cx, y0 + 2), (x1 - 2, cy), (cx, y1 - 2), (x0 + 2, cy)],
                  fill=(214, 172, 58, 255))
    # the model's badge (chrome letters on the paint) and the name across the tailgate (the
    # trim's colour, on the paint a shade darker, as a stamped panel catches less light)
    paint = rgb(pickup['paint'])
    x0, y0, x1, y1 = box('script')
    d.rectangle((x0, y0, x1, y1), fill=paint + (255,))
    if pickup['script']:
        lc.draw_text_centred(img, pickup['script'], (x0 + x1 + 1) // 2, y0 + 5,
                             rgb(pickup['script_colour']), 1)
    x0, y0, x1, y1 = box('gate')
    d.rectangle((x0, y0, x1, y1), fill=shade(paint, 0.86) + (255,))
    if pickup['gate']:
        scale = 2 if lc.text_width(pickup['gate'], 2) <= 62 else 1
        lc.draw_text_centred(img, pickup['gate'], (x0 + x1 + 1) // 2, y0 + (16 - 5 * scale) // 2,
                             (206, 210, 216) if chrome else (34, 34, 38), scale)


def tree_crew_cells(img):
    """The tree crew's cells: hazard stripes, tread plate, the grinder's toothed wheel, rubber
    track and a heap of chips. Only the trailers' and the grinder's textures carry them, so the
    other vehicles' textures stay as they were."""
    d = ImageDraw.Draw(img)

    def box(name):
        x0, y0, x1, y1 = CELLS[name]
        return x0, y0, x1 - 1, y1 - 1

    x0, y0, x1, y1 = box('hazard')
    stripes = Image.new('RGBA', (16, 16), (232, 168, 28, 255))
    sd = ImageDraw.Draw(stripes)
    for k in range(-16, 16, 6):
        sd.polygon([(k, 15), (k + 3, 15), (k + 18, 0), (k + 15, 0)], fill=(24, 24, 24, 255))
    img.paste(stripes, (x0, y0))
    x0, y0, x1, y1 = box('deck')
    d.rectangle((x0, y0, x1, y1), fill=(118, 122, 128, 255))
    for i in range(0, 16, 4):                     # tread plate: raised diamonds
        for j in range(0, 16, 4):
            dx, dy = x0 + i + (2 if (j // 4) % 2 else 0), y0 + j
            d.line((dx, dy + 1, dx + 1, dy), fill=(168, 172, 178, 255))
    x0, y0, x1, y1 = box('cutter')
    d.rectangle((x0, y0, x1, y1), fill=(52, 54, 58, 255))
    d.ellipse((x0 + 1, y0 + 1, x1 - 1, y1 - 1), fill=(96, 98, 104, 255))
    for k in range(8):                            # carbide teeth round the rim
        a = k * math.pi / 4
        tx, ty = x0 + 7.5 + 6.5 * math.cos(a), y0 + 7.5 + 6.5 * math.sin(a)
        d.rectangle((int(tx), int(ty), int(tx) + 1, int(ty) + 1), fill=(220, 222, 226, 255))
    d.ellipse((x0 + 6, y0 + 6, x0 + 9, y0 + 9), fill=(30, 30, 32, 255))
    x0, y0, x1, y1 = box('track')
    d.rectangle((x0, y0, x1, y1), fill=(30, 30, 30, 255))
    for i in range(0, 16, 3):                     # grousers
        d.line((x0 + i, y0, x0 + i, y1), fill=(58, 58, 60, 255))
    x0, y0, x1, y1 = box('mulch')
    d.rectangle((x0, y0, x1, y1), fill=(112, 76, 44, 255))
    for x, y in [(3, 2), (9, 5), (13, 1), (6, 10), (1, 13), (11, 12), (14, 8), (4, 6), (8, 14)]:
        d.point((x0 + x, y0 + y), fill=(150, 108, 64, 255))
        d.point((x0 + (x + 7) % 16, y0 + (y + 5) % 16), fill=(84, 56, 32, 255))


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
    'csm_wheel_compact': ('CSM Compact Alloy Wheel', 0.64, 0.22, (196, 200, 206)),
    'csm_wheel_sport': ('CSM Sport Wheel', 0.66, 0.25, (64, 66, 70)),
    'csm_wheel_sport_gold': ('CSM Sport Wheel (Gold)', 0.66, 0.25, (200, 164, 72)),
}
WT = 32  # wheel textures


def wheel_obj(diameter, width, sides=16):
    """A tyre and rim centred on the axle (the part's origin), turning about +x."""
    r = diameter / 2
    lines = ['# generated by dev-env-utils/scripts/gen_vehicle_fleet.py']
    v = vt = vn = 0

    def quad(name, pts, uvs, normal):
        nonlocal v, vt, vn
        if v == 0:
            lines.append('o wheel')  # one object: IV draws each object on its own
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
    o.box('body', (-0.25, 0.0, -0.25), (0.25, 0.12, 0.25), (0, 0, 16, 16))
    o.box('body', (-0.25, 0.12, -0.32), (0.25, 0.75, -0.2), (0, 0, 16, 16))
    o.box('body', (-0.2, -0.15, -0.2), (0.2, 0.0, 0.2), (16, 0, 32, 16))
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
    # fuelConsumption is also IV's measure of an engine's power, so these burn fuel in proportion;
    # the fleet's tanks (FUEL_CAPACITY) are sized to match. Six-speed automatics, the diesel's
    # ratios an Allison-style 3.49 to 0.65 overdrive. See SPEED below.
    'csm_engine_diesel': ('CSM Diesel Engine', 'diesel', 4.0, 3000, 2700, 600, 700, 400,
                          [-4.0, 0.0, 3.49, 1.86, 1.41, 1.0, 0.75, 0.65], 'diesel'),
    'csm_engine_petrol': ('CSM V8 Engine', 'gasoline', 2.2, 6000, 5500, 700, 800, 450,
                          [-3.0, 0.0, 4.0, 2.4, 1.6, 1.15, 0.85, 0.67], 'petrol'),
    # the Subaroos': a small flat-four, and a turbocharged one with a close six-speed
    'csm_engine_flat4': ('CSM Flat-Four Engine', 'gasoline', 1.4, 6500, 6000, 750, 850, 450,
                         [-3.3, 0.0, 3.5, 2.1, 1.45, 1.1, 0.85, 0.68], 'petrol'),
    'csm_engine_turbo4': ('CSM Turbo Flat-Four Engine', 'gasoline', 3.2, 7000, 6700, 850, 950,
                          500, [-3.5, 0.0, 3.64, 2.24, 1.52, 1.13, 0.97, 0.76], 'petrol'),
}

# A full tank: about twenty minutes at full throttle, as the old, weaker engines gave.
FUEL_CAPACITY = 60000

# ------------------------------------------------------------------------------------------
# Speed
#
# Immersive Vehicles moves a car by the motion its physics computes, times the server's
# carSpeedFactor (0.35 unless an operator changes it), and the speedometer shows that world
# speed. Its drag, though, acts on the unscaled motion, so a car must be built to push through
# about eight times the drag its world speed suggests. The fleet's first engines met it near
# 40 mph. Each vehicle is now geared so its engine reaches its maximum rpm in top gear at
# `ceiling_mph`, and given the power and drag to run up close to that: a governed top speed,
# much as a real truck's, that does not hinge on the drag figures. simulate() is a straight,
# level, full-throttle model of IV's code (LDMTS mccore: PartEngine's rpm, automatic shifts and
# wheel force, EntityVehicleF_Physics' drag), used to tune these numbers and reported by
# --speeds; --check fails if a road vehicle's top speed leaves SPEED_BAND.
# ------------------------------------------------------------------------------------------

SPEED_FACTOR = 0.35          # IV's default carSpeedFactor
SPEED_BAND = (60.0, 82.0)    # mph a road vehicle's simulated top speed must fall in
TICK_MPH = SPEED_FACTOR * 20 * 2.23694  # internal metres per tick -> world mph


def axle_ratio(spec):
    """The axle ratio that puts the engine at its maximum rpm, in top gear, at the ceiling."""
    if 'axle' in spec:
        return spec['axle']
    _, _, _, mx, _, _, _, _, gears, _ = ENGINES[spec['engine']]
    diameter = WHEELS[spec['wheel']][1]
    v = spec['ceiling_mph'] / TICK_MPH
    return round(mx * math.pi * diameter / (1200 * v * min(g for g in gears if g > 0)), 3)


def simulate(spec, seconds=240):
    """(top speed mph, seconds from rest to 60 mph or None) on level ground at full throttle."""
    _, _, power, mx, safe, idle, _, stall, gears, _ = ENGINES[spec['engine']]
    gears = [g for g in gears if g > 0]
    diameter = WHEELS[spec['wheel']][1]
    axle = axle_ratio(spec)
    cd, area = spec.get('drag', (0.5, 0.0))
    area = area if area > 0 else 5.0  # IV's default frontal area
    driven = 2 * sum(1 for _, _, steer in spec['wheels'] if spec.get('awd') or not steer)
    friction = 0.7 * driven  # the wheels' motiveFriction
    mass = spec['mass']
    v, rpm, gear, cooldown, top, t60 = 0.0, float(idle), 0, 0, 0.0, None
    for tick in range(seconds * 20):
        rpm += (v / (diameter * math.pi) * 1200 * gears[gear] * axle - rpm) / 10.0  # revResistance
        rpm = max(rpm, idle - (idle - stall) * 0.5)
        if cooldown:
            cooldown -= 1
        elif gear < len(gears) - 1 and rpm > safe * 0.9:  # automatic, full throttle
            gear, cooldown = gear + 1, 20
        elif gear > 0 and rpm < safe * 0.45:
            gear, cooldown = gear - 1, 20
        force = (mx - rpm) / mx * gears[gear] * axle * power * 18
        if abs(force / 300.0) > friction:  # wheelspin
            force *= mass / 100000.0 * friction / abs(force / 300.0)
        v = max(0.0, v + (force - 0.5 * 1.225 * v * v * area * cd) / mass)
        mph = v * TICK_MPH
        top = max(top, mph)
        if t60 is None and mph >= 60:
            t60 = tick / 20.0
    return top, t60


def speed_report():
    """Each motorized vehicle's gearing and simulated performance, and whether it is in band."""
    lines, bad = [], []
    for name, spec in FLEET.items():
        if spec.get('trailer'):
            continue
        top, t60 = simulate(spec)
        road = 'ceiling_mph' in spec
        lines.append('%-22s axle %5.2f  top %5.1f mph  0-60 %s%s' % (
            name, axle_ratio(spec), top, '%5.1f s' % t60 if t60 else '   -   ',
            '' if road else '  (off-road, not banded)'))
        band = spec.get('band', SPEED_BAND)
        if road and not band[0] <= top <= band[1]:
            bad.append(name)
    return lines, bad


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
        ceiling_mph=85, drag=(0.5, 2.0),
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
        ceiling_mph=80, drag=(0.55, 2.0),
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
        ceiling_mph=82, drag=(0.5, 2.0),
        wheels=[(0.95, 4.6, True), (0.95, 0.0, False), (0.62, 0.0, False)],
        seats=[(0.45, 0.85, 3.7, True), (-0.45, 0.85, 3.7, False)],
        engine_pos=(0.0, 0.6, 5.4), boxes=[-1.5, 1.0, 3.5, 5.6], box_width=2.5,
        box_height=2.6, box_y=1.1, switches=['BEACONS', 'DUMP'], animated=dump_animations,
        trailer_hitch=True),
    'csm_bucket_truck': dict(
        build=bucket_truck,
        description='A power company bucket truck. BEACONS runs its amber lights (not emergency '
                    'lights, so it does not preempt signals); BOOM raises the boom, swings it to '
                    'the left and runs it out, the bucket staying level.',
        mass=7000, wheel='csm_wheel_truck', engine='csm_engine_diesel', horn='horn_air',
        ceiling_mph=82, drag=(0.5, 2.0),
        wheels=[(0.95, 4.4, True), (0.95, 0.0, False), (0.62, 0.0, False)],
        seats=[(0.45, 0.85, 3.5, True), (-0.45, 0.85, 3.5, False)],
        engine_pos=(0.0, 0.6, 5.2), boxes=[-1.5, 1.0, 3.4, 5.4], box_width=2.5,
        box_height=2.4, box_y=1.0, switches=['BEACONS', 'BOOM'], animated=boom_animations,
        trailer_hitch=True),
    'csm_tow_truck': dict(
        build=tow_truck,
        description='A rollback tow truck. BED slides the flatbed back and tilts it; the panel '
                    'connects a vehicle onto the flatbed or onto the wheel lift. BEACONS runs '
                    'its amber lights, which do not preempt signals.',
        mass=7500, wheel='csm_wheel_truck', engine='csm_engine_diesel', horn='horn_air',
        ceiling_mph=84, drag=(0.5, 2.0),
        wheels=[(0.95, 4.7, True), (0.95, 0.0, False), (0.62, 0.0, False)],
        seats=[(0.45, 0.85, 3.8, True), (-0.45, 0.85, 3.8, False)],
        engine_pos=(0.0, 0.6, 5.5), boxes=[-2.0, 0.5, 3.0, 5.5], box_width=2.5,
        box_height=2.4, box_y=1.0, switches=['BEACONS', 'BED'], animated=bed_animations,
        hitches=tow_hitches, trailer_hitch=True),
    'csm_transit_bus': dict(
        build=transit_bus,
        description='A 40 ft low-floor city bus. DOORS opens the front and rear doors. TSP, on '
                    'from the start, is its transit priority emitter: signals with preempt '
                    'detectors and transit priority give it a longer green or a queue jump.',
        mass=12000, wheel='csm_wheel_truck', engine='csm_engine_diesel', horn='horn_air',
        ceiling_mph=76, drag=(0.6, 2.2),
        wheels=[(1.0, 7.2, True), (0.95, 0.0, False), (0.62, 0.0, False)],
        seats=[(0.75, 0.45, 8.7, True)] +
              [(sx * 0.75, 0.55, z, False) for z in (6.4, 5.2, 2.0, 0.8, -0.4, -1.6)
               for sx in (1, -1)],
        engine_pos=(0.0, 0.5, -2.8), boxes=[-2.2, 0.3, 2.8, 5.3, 7.8, 9.3], box_width=2.55,
        box_height=2.9, box_y=1.1, switches=['DOORS', 'TSP'], initially_on=['TSP'],
        animated=door_animations),
    'csm_ambulance': dict(
        build=ambulance,
        description='A Type III ambulance. Switch EMERLTS on and it runs its lights, and '
                    'intersections with a CSM preempt detector give it the green.',
        mass=4200, wheel='csm_wheel_van', engine='csm_engine_diesel', horn='horn_air',
        ceiling_mph=88, drag=(0.45, 2.0),
        wheels=[(0.82, 3.6, True), (0.78, 0.0, False), (0.52, 0.0, False)],
        seats=[(0.45, 0.65, 3.4, True), (-0.45, 0.65, 3.4, False), (0.0, 0.7, 0.6, False)],
        engine_pos=(0.0, 0.45, 4.4), boxes=[-1.0, 1.3, 3.5], box_width=2.3, box_height=2.8,
        box_y=1.1),
    'csm_police_suv': dict(
        build=police_suv,
        description='A police utility vehicle. Switch EMERLTS on and it runs its lights, and '
                    'intersections with a CSM preempt detector give it the green.',
        mass=2300, wheel='csm_wheel_car', engine='csm_engine_petrol', horn='horn_car',
        ceiling_mph=92, drag=(0.35, 1.6),
        wheels=[(0.85, 3.0, True), (0.85, 0.0, False)],
        seats=[(0.42, 0.3, 1.7, True), (-0.42, 0.3, 1.7, False),
               (0.42, 0.3, 0.45, False), (-0.42, 0.3, 0.45, False)],
        engine_pos=(0.0, 0.4, 3.4), boxes=[-0.2, 1.6, 3.3], box_width=2.0, box_height=1.8,
        box_y=0.6),
    # The pickups: civilian crew cabs, no switches. The XL and the XLT share one model
    # (`model`: the XLT's definitions name the XL's OBJ) and differ in their wheels, which a
    # livery cannot change: black steel on the XL, silver alloys on the XLT. `trim` sets each
    # livery's bumpers and bars, chrome unless listed.
    'csm_pickup_h150_xl': dict(
        cap='slot', script='H-150', gate='HALDEN',
        build=pickup_h150, badge='halden', trim={'_white': 'black'},
        description='A half-ton crew cab pickup in work-truck trim: black grille, bumpers and '
                    'steel wheels.',
        mass=2200, wheel='csm_wheel_car', engine='csm_engine_petrol', horn='horn_car',
        ceiling_mph=90, drag=(0.4, 1.8),
        wheels=[(0.85, 3.68, True), (0.85, 0.0, False)], seats=PICKUP_SEATS,
        engine_pos=(0.0, 0.45, 3.7), boxes=[-0.3, 1.6, 3.6], box_width=2.0, box_height=1.8,
        box_y=0.7, switches=[]),
    'csm_pickup_h150_xlt': dict(
        cap='slot', script='H-150', gate='HALDEN',
        build=pickup_h150, badge='halden', model='csm_pickup_h150_xl',
        trim={'_publicworks': 'black'},
        description='A half-ton crew cab pickup with chrome trim and alloy wheels; the public '
                    'works truck keeps its black work-truck bumpers.',
        mass=2200, wheel='csm_wheel_van', engine='csm_engine_petrol', horn='horn_car',
        ceiling_mph=90, drag=(0.4, 1.8),
        wheels=[(0.85, 3.68, True), (0.85, 0.0, False)], seats=PICKUP_SEATS,
        engine_pos=(0.0, 0.45, 3.7), boxes=[-0.3, 1.6, 3.6], box_width=2.0, box_height=1.8,
        box_y=0.7, switches=[]),
    'csm_pickup_silverline': dict(
        cap='slot', rail=0.98, script='SILVERLINE',
        build=pickup_silverline, badge='chevrolay',
        description='A half-ton crew cab pickup: chrome two-tier grille, chrome bumper and step, '
                    'alloy wheels.',
        mass=2300, wheel='csm_wheel_van', engine='csm_engine_petrol', horn='horn_car',
        ceiling_mph=90, drag=(0.4, 1.8),
        wheels=[(0.85, 3.68, True), (0.85, 0.0, False)], seats=PICKUP_SEATS,
        engine_pos=(0.0, 0.45, 3.7), boxes=[-0.3, 1.6, 3.6], box_width=2.0, box_height=1.8,
        box_y=0.7, switches=[]),
    'csm_pickup_h150_service': dict(
        build=pickup_h150, badge='halden', model='csm_pickup_h150_xl', cap='fitted',
        script='H-150', gate='HALDEN',
        trim={'_publicworks': 'black', '_white': 'black'},
        description='A work-trim half-ton crew cab that comes with the utility cap: '
                    'compartments, a ladder rack and an amber beacon on BEACONS.',
        mass=2400, wheel='csm_wheel_van', engine='csm_engine_petrol', horn='horn_car',
        ceiling_mph=90, drag=(0.45, 1.9),
        wheels=[(0.85, 3.68, True), (0.85, 0.0, False)], seats=PICKUP_SEATS,
        engine_pos=(0.0, 0.45, 3.7), boxes=[-0.3, 1.6, 3.6], box_width=2.0, box_height=1.95,
        box_y=0.775, switches=[]),
    # The Subaroos: a hatchback and a sports sedan, both all-wheel drive. The STX is the
    # fleet's fastest, geared for about 100 mph; its gold-wheeled twin shares its model.
    'csm_subaroo_impressa': dict(
        build=impressa, badge='subaroo', gate='IMPRESSA', awd=True,
        description='A five-door compact hatchback with all-wheel drive.',
        mass=1400, wheel='csm_wheel_compact', engine='csm_engine_flat4', horn='horn_car',
        ceiling_mph=88, drag=(0.32, 1.4),
        wheels=[(0.76, 2.67, True), (0.76, 0.0, False)], seats=CAR_SEATS,
        engine_pos=(0.0, 0.2, 3.0), boxes=[-0.1, 1.4, 2.8], box_width=1.8, box_height=1.36,
        box_y=0.48, switches=[]),
    'csm_subaroo_wrz_stx': dict(
        build=wrz_stx, badge='subaroo', gate='WRZ STX', script='STX', script_colour='#D8202A',
        trim={'*': 'black'}, awd=True, band=(94.0, 104.0),
        description='A turbocharged all-wheel-drive sports sedan with a hood scoop and a tall '
                    'rear wing: the fleet\'s fastest, at about 100 mph.',
        mass=1550, wheel='csm_wheel_sport', engine='csm_engine_turbo4', horn='horn_car',
        ceiling_mph=112, drag=(0.3, 1.35),
        wheels=[(0.77, 2.65, True), (0.77, 0.0, False)], seats=CAR_SEATS,
        engine_pos=(0.0, 0.2, 3.0), boxes=[-0.2, 1.4, 2.8], box_width=1.8, box_height=1.36,
        box_y=0.47, switches=[]),
    'csm_subaroo_wrz_stx_gold': dict(
        build=wrz_stx, badge='subaroo', gate='WRZ STX', script='STX', script_colour='#D8202A',
        trim={'*': 'black'}, awd=True, band=(94.0, 104.0), model='csm_subaroo_wrz_stx',
        description='The WRZ STX in rally blue on gold wheels.',
        mass=1550, wheel='csm_wheel_sport_gold', engine='csm_engine_turbo4', horn='horn_car',
        ceiling_mph=112, drag=(0.3, 1.35),
        wheels=[(0.77, 2.65, True), (0.77, 0.0, False)], seats=CAR_SEATS,
        engine_pos=(0.0, 0.2, 3.0), boxes=[-0.2, 1.4, 2.8], box_width=1.8, box_height=1.36,
        box_y=0.47, switches=[]),
    # The tree crew (#251): a towed chipper, an equipment trailer and a stump grinder. The
    # chipper and the grinder load only with CSM: Parks & Greenery (the pack's treecrew
    # subfolder), since one makes Parks' Mulch and the other grinds by Parks' stump rules.
    'csm_chipper_trailer': dict(
        build=chipper_trailer, trailer=True, subfolder='treecrew', extras=True,
        description='A towed brush chipper. Hitch it to a truck (trailer_standard, as UNU\'s '
                    'hitch bumpers and CSM\'s work trucks offer), pull the lever on the engine '
                    'housing to start it, and drop logs, brush and leaves on the feed table: they '
                    'come out as Mulch in the hopper. Click the hopper to open it. It takes '
                    'anything dropped behind it, so stop it when you are done.',
        mass=1200, wheel='csm_wheel_van', wheels=[(0.95, -0.55, False)],
        hookup=(0.05, 3.45), boxes=[-2.4, -0.8, 0.8, 2.4], box_width=1.9, box_height=1.8,
        box_y=0.65, switches=['CHIPPER'],
        chip_box=(0.0, 0.5, CHIP_BOX_Z),
        clicks=[(CHIPPER_LEVER, 0.35, 0.5, 'CHIPPER', 'toggle')],
        sounds=[('chipper_run', 'CHIPPER')]),
    'csm_equipment_trailer': dict(
        build=equipment_trailer, trailer=True, extras=True,
        description='A low equipment trailer for small machines, the stump grinder among them. '
                    'Hitch it to a truck (trailer_standard), drive the machine onto the deck and '
                    'click the striped tie-down at the front of the deck to load it; click it '
                    'again to unload.',
        mass=900, wheel='csm_wheel_van', wheels=[(1.1, -0.4, False)],
        hookup=(0.1, 3.4), boxes=[-1.3, 0.5, 2.6], box_width=2.1, box_height=0.8,
        box_y=0.1, switches=[],
        hitches=lambda v: [{'groupName': 'Deck', 'isHitch': True,
                            'canInitiateConnections': True,
                            'connections': [{'type': 'tow_flatbed', 'mounted': True,
                                             'distance': 3,
                                             'pos': [0, EQUIPMENT_DECK_Y,
                                                     EQUIPMENT_DECK[1] - 0.6]}]}],
        clicks=[((0.0, EQUIPMENT_DECK_Y + 0.05, EQUIPMENT_DECK[1] - 0.35), 0.4, 0.3,
                 'connection_requested', 'set1')]),
    'csm_stump_grinder': dict(
        build=stump_grinder, subfolder='treecrew', extras=True,
        description='A self-propelled stump grinder on rubber tracks, driven from the platform '
                    'at its back. Drive the cutter wheel up to a stump and switch GRIND on: the '
                    'boom lowers and grinds the stump and its roots away, leaving Mulch, by the '
                    'same rules as the hand-guided grinder (a stump in the ground, never a '
                    'standing tree or a log on a floor). Carry it on the equipment trailer.',
        mass=700, wheel='csm_wheel_car', engine='csm_engine_petrol', horn='horn_car',
        axle=7.0,
        wheels=[(0.42, 0.55, True), (0.42, -0.55, False)],
        seats=[(0.0, 0.45, -1.05, True)],
        engine_pos=(0.0, 0.4, -0.2), boxes=[-0.6, 0.6], box_width=1.2, box_height=1.07,
        box_y=0.265, switches=['GRIND'], animated=grinder_animations,
        sounds=[('csm:stump_grinder_grind', 'GRIND')]),
}


def vehicle_json(name, v):
    spec = FLEET[name]
    if spec.get('trailer'):
        return trailer_json(name, v)
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
            if spec.get('awd') or not steer:
                driven.append(len(parts))
    for x, y, z, controller in spec['seats']:
        slot = {'pos': [x, y, z], 'types': ['seat'], 'defaultPart': '%s:csm_vehicle_seat' % PACK,
                'dismountPos': [x + (1.6 if x >= 0 else -1.6), 0.0, z]}
        if controller:
            slot['isController'] = True
        parts.append(slot)
    if 'cap' in spec:
        x, y, z = PICKUP_CAP_SLOT
        slot = {'pos': [x, spec.get('rail', y), z], 'types': [PICKUP_CAP_TYPE]}
        if spec['cap'] == 'fitted':
            slot['defaultPart'] = '%s:csm_pickup_cap' % PACK
        parts.append(slot)
    # Fuel consumption is an engine's power: the fleet's own run to 4, other packs' below 1.
    parts.append({'pos': list(spec['engine_pos']), 'minValue': 0.25, 'maxValue': 5.0,
                  'types': ['engine_car'], 'defaultPart': '%s:%s' % (PACK, spec['engine']),
                  'linkedParts': driven})
    switches = spec.get('switches', ['EMERLTS', 'siren', 'siren_yelp', 'siren_hilo'])
    tones = [t for t in (('siren', 'siren_wail'), ('siren_yelp', 'siren_yelp'),
                         ('siren_hilo', 'siren_hilo')) if t[0] in switches]
    definitions = [{'name': livery[1], 'subName': livery[0], 'extraMaterialLists': [[]]}
                   for livery in LIVERIES[name]]
    if 'model' in spec:
        for d in definitions:
            d['modelName'] = spec['model']
    definition = {
        'definitions': definitions,
        'general': {'description': spec['description'],
                    'materialLists': [['minecraft:iron_block:0:6', 'minecraft:glass_pane:0:6',
                                       'minecraft:redstone_block:0:1']]},
        'motorized': {'emptyMass': spec['mass'], 'fuelCapacity': FUEL_CAPACITY,
                      'defaultFuelQty': FUEL_CAPACITY, 'axleRatio': axle_ratio(spec),
                      'brakingFactor': 1.0, 'dragCoefficient': spec.get('drag', (0.5, 0))[0],
                      'crossSectionalArea': spec.get('drag', (0.5, 0))[1],
                      'hasHeadlights': True, 'hasRunningLights': True,
                      'hasTurnSignals': True, 'litVariable': 'headlight',
                      'panel': 'mts:default_car',
                      'hudTexture': '%s:textures/guis/%s_hud.png' % (PACK, name)},
        'parts': parts,
        'collisionGroups': [{
            # No 'click': the body's boxes enclose the seats, and a body that took clicks
            # swallowed every right-click before it reached a seat, so nobody could get in.
            'collisionTypes': ['block', 'entity', 'attack'],
            'collisions': [{'pos': [0, spec['box_y'], z], 'width': spec['box_width'],
                            'height': spec['box_height']} for z in spec['boxes']],
        }] + click_groups(spec),
        'rendering': {
            'modelType': 'obj',
            'customVariables': switches,
            'sounds': [{'name': '%s:%s' % (PACK, spec['horn']), 'looping': True,
                        'activeAnimations': [vp.visible_on('horn')]}] +
                      [{'name': '%s:%s' % (PACK, sound), 'looping': True,
                        'activeAnimations': [vp.visible_on(variable)]}
                       for variable, sound in tones] + extra_sounds(spec),
            'lightObjects': v.lights,
        },
    }
    definition['instruments'] = instruments(v)
    if 'animated' in spec:
        definition['rendering']['animatedObjects'] = spec['animated']()
    if 'initially_on' in spec:
        definition['rendering']['initialVariables'] = spec['initially_on']
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
    if spec.get('trailer_hitch'):
        groups = [trailer_hitch(v)] + groups
    definition['connectionGroups'] = groups
    return definition


def extra_sounds(spec):
    """A vehicle's own looping sounds, each on while its switch is: (sound, variable). A sound
    named without a domain is this pack's."""
    return [{'name': sound if ':' in sound else '%s:%s' % (PACK, sound), 'looping': True,
             'activeAnimations': [vp.visible_on(variable)]}
            for sound, variable in spec.get('sounds', [])]


def click_groups(spec):
    """The clickable boxes on a vehicle: (centre, width, height, variable, action), where the
    action is 'toggle' or 'set<value>'. IV runs the action on a click, as on a door."""
    clicks = []
    for centre, width, height, variable, action in spec.get('clicks', []):
        if action == 'toggle':
            act = {'action': 'toggle', 'variable': variable, 'value': 1}
        else:
            act = {'action': 'set', 'variable': variable, 'value': int(action[3:])}
        clicks.append({'pos': [round(c, 3) for c in centre], 'width': width, 'height': height,
                       'action': act})
    return [{'collisionTypes': ['click'], 'collisions': clicks}] if clicks else []


def trailer_json(name, v):
    """A trailer: no engine or seats, the towing vehicle's lights and signals carried over the
    hitch, a trailer_standard hookup on its tongue, and whatever it carries as parts."""
    spec = FLEET[name]
    diameter = WHEELS[spec['wheel']][1]
    wheel = '%s:%s' % (PACK, spec['wheel'])
    parts = []
    for x, z, _ in spec['wheels']:
        for sx in (1, -1):
            slot = {'pos': [sx * x, 0.0, z], 'minValue': round(diameter * 0.6, 3),
                    'maxValue': round(diameter * 1.2, 3), 'types': ['ground_wheel'],
                    'defaultPart': wheel}
            if sx < 0:
                slot['rot'] = [0, 180, 0]
                slot['isMirrored'] = True
            parts.append(slot)
    if 'chip_box' in spec:
        parts.append({'pos': list(spec['chip_box']), 'types': ['interactable_csm_chipbox'],
                      'minValue': vp.CHIPBOX_UNITS, 'maxValue': vp.CHIPBOX_UNITS,
                      'defaultPart': '%s:csm_chipbox' % PACK})
    definitions = [{'name': livery[1], 'subName': livery[0], 'extraMaterialLists': [[]]}
                   for livery in LIVERIES[name]]
    hy, hz = spec['hookup']
    definition = {
        'definitions': definitions,
        'general': {'description': spec['description'],
                    'materialLists': [['minecraft:iron_block:0:3', 'minecraft:iron_ingot:0:8']]},
        'motorized': {'isTrailer': True, 'emptyMass': spec['mass'],
                      'hookupVariables': ['electric_power', 'engine_reversed_1', 'engines_on',
                                          'right_turn_signal', 'left_turn_signal',
                                          'running_light', 'headlight', 'brake'],
                      'fuelCapacity': 0, 'defaultFuelQty': 0, 'axleRatio': 0,
                      'brakingFactor': 1.0, 'dragCoefficient': 0.5},
        'parts': parts,
        'collisionGroups': [{
            'collisionTypes': ['block', 'entity', 'attack'],
            'collisions': [{'pos': [0, spec['box_y'], z], 'width': spec['box_width'],
                            'height': spec['box_height']} for z in spec['boxes']],
        }] + click_groups(spec),
        'rendering': {
            'modelType': 'obj',
            'customVariables': spec.get('switches', []),
            'sounds': extra_sounds(spec),
            'lightObjects': v.lights,
        },
    }
    groups = [{'groupName': 'Trailer', 'isHookup': True,
               'connections': [{'type': 'trailer_standard', 'pos': [0, hy, hz],
                                'distance': 2}]}]
    if 'hitches' in spec:
        groups = spec['hitches'](v) + groups
    definition['connectionGroups'] = groups
    return definition


# ------------------------------------------------------------------------------------------
# Instruments: our own, so the HUD is not blank without another pack's

INSTRUMENT_SHEET = 1024           # IV's instrument sheets are this size
DIAL = 128                        # each dial's cell, centred at (64 + 128 i, 64)
NEEDLE = (8, 128)                 # the needle's cell, centred at (1012, 64)
INSTRUMENTS = {
    # name: (label, variable, degrees a unit, offset, ticks: (count, sweep), legend, description)
    'csm_instrument_speedometer': ('CSM Speedometer', 'speed', 5.966, -120.0, (10, 240), 'MPH',
                                   'Speed, 0 to 90 miles an hour.'),
    'csm_instrument_tachometer': ('CSM Tachometer', 'engine_rpm', 0.045, -135.0, (7, 270), 'RPM',
                                  'Engine speed, 0 to 6,000 revolutions a minute.'),
    'csm_instrument_fuel': ('CSM Fuel Gauge', 'fuel', 90.0, -45.0, (5, 90), 'FUEL',
                            'Fuel left, empty to full.'),
}


def instrument_sheet():
    img = Image.new('RGBA', (INSTRUMENT_SHEET, INSTRUMENT_SHEET), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for i, (name, (_, _, _, offset, (count, sweep), legend, _)) in enumerate(INSTRUMENTS.items()):
        cx, cy, r = 64 + DIAL * i, 64, 60
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(40, 42, 46, 255))
        d.ellipse((cx - r + 4, cy - r + 4, cx + r - 4, cy + r - 4), fill=(16, 16, 18, 255))
        for k in range(count):
            a = math.radians(offset + sweep * k / (count - 1))
            x0, y0 = cx + math.sin(a) * (r - 18), cy - math.cos(a) * (r - 18)
            x1, y1 = cx + math.sin(a) * (r - 7), cy - math.cos(a) * (r - 7)
            d.line((x0, y0, x1, y1), fill=(235, 235, 235, 255), width=3)
            for kk in (0.5,):
                if k < count - 1:
                    b = math.radians(offset + sweep * (k + kk) / (count - 1))
                    d.line((cx + math.sin(b) * (r - 12), cy - math.cos(b) * (r - 12),
                            cx + math.sin(b) * (r - 7), cy - math.cos(b) * (r - 7)),
                           fill=(170, 170, 170, 255), width=2)
        if legend == 'FUEL':
            d.line((cx + math.sin(math.radians(offset)) * (r - 18),
                    cy - math.cos(math.radians(offset)) * (r - 18),
                    cx + math.sin(math.radians(offset)) * (r - 7),
                    cy - math.cos(math.radians(offset)) * (r - 7)),
                   fill=(220, 40, 30, 255), width=4)
        lc.draw_text_centred(img, legend, cx, cy + 24, (220, 220, 220), 2)
    # the needle: pointing up from the centre of its cell, which IV turns about that centre
    nx, ny = 1012, 64
    d.rectangle((nx - 2, ny - 52, nx + 1, ny), fill=(255, 90, 40, 255))
    d.ellipse((nx - 4, ny - 4, nx + 3, ny + 3), fill=(200, 200, 200, 255))
    return img


# The HUD: our own panel, with the keys that work the vehicle printed either side of the gauges.
# IV draws its top half by default (the half-HUD), so everything sits in y 0 to 70.
SWITCH_LABELS = {'EMERLTS': 'LIGHTBAR', 'siren': 'WAIL', 'siren_yelp': 'YELP',
                 'siren_hilo': 'HI-LO', 'AERIAL': 'LADDER', 'BEACONS': 'BEACONS',
                 'DUMP': 'DUMP BODY', 'BOOM': 'BOOM', 'BED': 'BED', 'DOORS': 'DOORS',
                 'TSP': 'TSP'}
CONTROLS = [('W S', 'GAS BRAKE'), ('G', 'LIGHTS'), ('C', 'HORN'), ('N', 'PARK'),
            ('X', 'VIEW'), ('SHIFT', 'GET OUT')]


def hud_texture(switches, lit=False):
    img = Image.new('RGBA', (512, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    base = (58, 62, 68) if not lit else (70, 74, 82)
    for y in range(140):
        k = 1.0 - 0.18 * y / 140
        d.line((0, y, 399, y), fill=tuple(int(c * k) for c in base) + (255,))
    d.rectangle((0, 0, 399, 139), outline=(24, 26, 30, 255), width=3)
    d.rectangle((3, 3, 396, 136), outline=(96, 100, 108, 255), width=1)
    for x, y in ((7, 7), (392, 7), (7, 132), (392, 132)):
        d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=(130, 134, 140, 255))
    key = (255, 200, 40) if not lit else (255, 220, 90)
    text = (225, 228, 232)
    dim = (160, 166, 174)
    # left: the panel and what it switches
    d.rectangle((10, 8, 112, 66), fill=(30, 32, 36, 255))
    lc.draw_text(img, 'U', 15, 12, key, 1)
    lc.draw_text(img, 'PANEL', 27, 12, text, 1)
    for i, sw in enumerate(switches[:5]):
        lc.draw_text(img, SWITCH_LABELS.get(sw, sw.upper()), 27, 22 + 8 * i, dim, 1)
    # right: the controls
    d.rectangle((288, 8, 390, 66), fill=(30, 32, 36, 255))
    for i, (k, what) in enumerate(CONTROLS):
        lc.draw_text(img, k, 293, 12 + 9 * i, key, 1)
        lc.draw_text(img, what, 293 + lc.text_width(k) + 6, 12 + 9 * i, text, 1)
    return img


def instrument_json(name):
    label, variable, factor, offset, _, _, description = INSTRUMENTS[name]
    i = list(INSTRUMENTS).index(name)
    return {
        'components': [
            {'scale': 1.0, 'textureXCenter': 64 + DIAL * i, 'textureYCenter': 64,
             'textureWidth': DIAL, 'textureHeight': DIAL},
            {'scale': 1.0, 'textureXCenter': 1012, 'textureYCenter': 64,
             'textureWidth': NEEDLE[0], 'textureHeight': NEEDLE[1],
             'animations': [{'animationType': 'rotation', 'variable': variable,
                             'centerPoint': [0.0, 0.0, 0.0], 'axis': [0.0, 0.0, factor],
                             'offset': offset}]},
        ],
        'textureName': 'instruments.png',
        'general': {'name': label, 'description': description, 'stackSize': 64,
                    'materialLists': [['minecraft:iron_ingot:0:1', 'minecraft:glass_pane:0:1',
                                       'minecraft:redstone:0:1']]},
    }


def instrument_icon(sheet, name):
    i = list(INSTRUMENTS).index(name)
    dial = sheet.crop((DIAL * i, 0, DIAL * (i + 1), DIAL))
    needle = sheet.crop((1008, 0, 1016, DIAL))
    dial.alpha_composite(needle.rotate(-30, center=(4, 64)), (60, 0))
    return dial.resize((16, 16), Image.LANCZOS)


def instruments(v):
    """The speedometer, tachometer and fuel gauge on the HUD."""
    out = []
    for k, name in enumerate(INSTRUMENTS):
        # HUD only: scale 0 draws nothing in the cab. Placed on the dash they never showed in
        # game, whichever way they were turned, so they stay off it until that is understood.
        out.append({'hudX': 140 + 60 * k, 'hudY': 38, 'hudScale': 0.4,
                    'defaultInstrument': '%s:%s' % (PACK, name), 'pos': [0, 0, 0], 'scale': 0.0})
    return out


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
        if 'model' not in FLEET[name]:
            put(os.path.join(PACK_DIR, 'objmodels', 'vehicles', name + '.obj'),
                v.obj.text().replace('gen_vehicle_parts.py',
                                     'gen_vehicle_fleet.py').encode('utf-8'))
        sub = FLEET[name].get('subfolder', '')
        put(os.path.join(PACK_DIR, sub, 'jsondefs', 'vehicles', name + '.json'),
            vp.json_bytes(vehicle_json(name, v)))
        for livery in LIVERIES[name]:
            spec = FLEET[name]
            trims = spec.get('trim', {})
            trim = {'trim': trims.get(livery[0], trims.get('*', 'chrome')),
                    'script_colour': spec.get('script_colour', '#D6DAE0'),
                    'badge': spec['badge'], 'seal': bool(livery[5]),
                    'script': spec.get('script', ''), 'gate': spec.get('gate', ''),
                    'paint': livery[2]} if 'badge' in spec else None
            texture = vehicle_texture(livery, DECAL_ON[name], spec.get('extras', False), trim)
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

    cap = pickup_cap()
    put(os.path.join(PACK_DIR, 'objmodels', 'parts', 'csm_pickup_cap.obj'),
        cap.obj.text().replace('gen_vehicle_parts.py', 'gen_vehicle_fleet.py').encode('utf-8'))
    cap_texture = vehicle_texture(PICKUP_CAP_LIVERY)
    put(os.path.join(PACK_DIR, 'textures', 'parts', 'csm_pickup_cap.png'),
        vp.png_bytes(cap_texture))
    put(os.path.join(PACK_DIR, 'jsondefs', 'parts', 'csm_pickup_cap.json'),
        vp.json_bytes(pickup_cap_json(cap)))
    item('parts', 'csm_pickup_cap', vehicle_icon(cap, 0.0, cap_texture))

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

    for name in FLEET:
        if FLEET[name].get('trailer'):
            continue  # nobody drives a trailer: no HUD
        switches = FLEET[name].get('switches', ['EMERLTS', 'siren', 'siren_yelp', 'siren_hilo'])
        put(os.path.join(PACK_DIR, 'textures', 'guis', name + '_hud.png'),
            vp.png_bytes(hud_texture(switches)))
        put(os.path.join(PACK_DIR, 'textures', 'guis', name + '_hud_lit.png'),
            vp.png_bytes(hud_texture(switches, lit=True)))

    sheet = instrument_sheet()
    put(os.path.join(PACK_DIR, 'textures', 'instruments.png'), vp.png_bytes(sheet))
    for name in INSTRUMENTS:
        put(os.path.join(PACK_DIR, 'jsondefs', 'instruments', name + '.json'),
            vp.json_bytes(instrument_json(name)))
        item('instruments', name, instrument_icon(sheet, name))

    for name in ENGINES:
        put(os.path.join(PACK_DIR, 'jsondefs', 'parts', name + '.json'),
            vp.json_bytes(engine_json(name)))
        icon = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        dd = ImageDraw.Draw(icon)
        dd.rectangle((2, 5, 13, 12), fill=(70, 72, 76, 255))
        dd.rectangle((4, 2, 11, 5), fill=(120, 124, 130, 255))
        item('parts', name, icon)
    return files


# How far a vehicle's block collision boxes must stand off the ground. IV stops a vehicle whose
# box touches a block (AEntityVehicleD_Moving.correctCollidingMovement), so a box down to the
# wheels' contact patch pins it where it stands: the engine revs and nothing moves. The fleet's
# oldest drivers stand 0.09 m clear.
BOX_CLEARANCE = 0.08


def grounded_boxes():
    """The vehicles whose collision boxes reach within BOX_CLEARANCE of the ground."""
    out = []
    for name, spec in FLEET.items():
        ground = -WHEELS[spec['wheel']][1] / 2
        bottom = spec['box_y'] - spec['box_height'] / 2
        if bottom - ground < BOX_CLEARANCE:
            out.append('%s (%.2f m clear)' % (name, bottom - ground))
    return out


def main():
    check = '--check' in sys.argv
    grounded = grounded_boxes()
    if grounded:
        print('collision boxes too near the ground (IV would pin these in place): '
              + ', '.join(grounded))
        sys.exit(1)
    lines, bad = speed_report()
    if '--speeds' in sys.argv:
        print('\n'.join(lines))
        return
    if bad:
        print('\n'.join(lines))
        print('top speed outside its band (%.0f-%.0f mph unless the spec sets one): %s'
              % (SPEED_BAND + (', '.join(bad),)))
        sys.exit(1)
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
