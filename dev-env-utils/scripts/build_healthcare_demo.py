#!/usr/bin/env python3
"""
build_healthcare_demo.py -- builds the Healthcare demo world, over the MCMCP orchestrator.

Load a fresh FLAT creative world named "Healthcare Demo" in the dev client (every module, since
the hospital also uses Building's doors and finishes, Life Safety's decon sink, Furnishings' pews,
altar, lectern and laundry, Parks' planters and Transit's helipad) and run this with the client's
instance id. It builds one hospital floor along the x axis: a corridor five blocks wide running
east from the main entrance, rooms to the north and south of it, a ceiling at y 9 lit by sea
lanterns, and a helipad on the roof over the emergency department.

  corridor  hospital doors with room signs and dome lights, wayfinding stripes, handrails, a run
            of crash rail by the ED, and hanging wayfinding panels of four sizes (1 x 1, 3 x 1,
            4 x 2 and 5 x 2), facing the main entrance
  north     a two-bed ward (beds, headwalls, reading lights, curtains on a ceiling track, nurse
            call linked to the station, one call left on), an ICU bay (behind automatic sliding
            doors: ICU bed, pendant, ventilator, ceiling lift, code blue button), the emergency
            department (tracking board bank, trauma bay, decon shower, surgery support, an
            ambulance entrance outside), imaging (CT, MRI, x-ray room, C-arm, mammography, lead
            door, IN USE lights, MRI zone signs), the lab, the pharmacy and the morgue
  south     the nurses' station (counters, master panel, code blue annunciator, gas alarm), a
            clinic (exam room, eye lane, dental chair), dialysis and infusion, maternity and NICU
            (with the infant security panel at its door), the waiting room (beam seating, a
            check-in kiosk linked to a now-serving display), a chapel, a lecture hall (tiers,
            mannequin, skeleton, dissection table), a playroom, the laundry and a rehab gym
  roof      a 9 x 9 hospital helipad with its lights and a wind sock

Finally it reads the two Healthcare tabs' registration lists from the sources and puts every
block it did not place on a signed plinth south of the building (a block on a plinth is one this
script forgot), and prints the count.

Everything goes through the orchestrator by instance id; the script never opens a game port, and
it refuses any world not named "Healthcare Demo". It sets blocks and runs commands only; it never
moves the player's camera or clicks, so the client's input need not be locked while it runs (lock
it anyway if a person may be at the keyboard).

Usage:
    python build_healthcare_demo.py --instance run-f2eb58 [--x X --z Z]
"""
import argparse
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmcp_orchestrator import Orchestrator  # noqa: E402

ap = argparse.ArgumentParser(description='Build the Healthcare demo world.')
ap.add_argument('--instance', required=True, help="the dev game's id, e.g. run-f2eb58")
ap.add_argument('--x', type=int, default=None, help='site origin x (default: player)')
ap.add_argument('--z', type=int, default=None, help='site origin z (default: player)')
ARGS = ap.parse_args()
WORLD_NAME = 'Healthcare Demo'

O = Orchestrator()
CLIENT, SERVER = ARGS.instance + '.client', ARGS.instance + '.server'
_info = O.call('server_world_info', instance=SERVER)
if WORLD_NAME not in json.dumps(_info):
    sys.exit('refusing: the world on %s is not "%s"' % (SERVER, WORLD_NAME))
_me = O.call('client_player_state', instance=CLIENT)
X0 = ARGS.x if ARGS.x is not None else int(_me['position']['x'])
Z0 = ARGS.z if ARGS.z is not None else int(_me['position']['z'])

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
TABS = os.path.join(REPO, 'modules', 'healthcare', 'src', 'main', 'java', 'com',
                    'micatechnologies', 'minecraft', 'csm', 'tabs')

G = 3            # the flat world's ground: the floor slab
Y = 4            # the floor people stand on
CEIL = 9         # the ceiling-tile layer (five clear blocks, y 4 to 8)
XMAX = 111       # the building's east wall
ZN, ZS = -11, 15  # the north and south outer walls
CN, CS = -1, 5   # the corridor's walls; the corridor is z 0 to 4

# Floor pieces: the facing's horizontal index, the way the front faces (towards the placer).
S, W, N, E = 0, 1, 2, 3
# Wall pieces: EnumFacing index of the way out of the wall.
OUT = {'N': 2, 'S': 3, 'W': 4, 'E': 5}
STEP = {S: (0, 1), W: (-1, 0), N: (0, -1), E: (1, 0)}
CCW = {S: E, E: N, N: W, W: S}          # facing.rotateYCCW(): the viewer's right
WALL_TO_H = {2: N, 3: S, 4: W, 5: E}   # a wall facing as a horizontal index

blocks = []
placed = set()


# ------------------------------------------------------------------------------------------
# Plumbing
# ------------------------------------------------------------------------------------------
def name_of(block):
    return block if ':' in block else 'csm:' + block


def put(x, y, z, block, meta=0, nbt=None):
    """A block at site coordinates (x, z relative to the origin, y absolute)."""
    b = {'x': X0 + x, 'y': y, 'z': Z0 + z, 'block': name_of(block), 'metadata': meta}
    if nbt:
        b['nbt'] = nbt
    blocks.append(b)
    if b['block'].startswith('csm:'):
        placed.add(b['block'][4:])


def flush():
    global blocks
    for i in range(0, len(blocks), 800):
        O.call('server_set_blocks', instance=SERVER, mode='list', blocks=blocks[i:i + 800],
               load=True)
    blocks = []


def fill(x0, y0, z0, x1, y1, z1, block, meta=0):
    flush()
    xs, ys, zs = sorted((x0, x1)), sorted((y0, y1)), sorted((z0, z1))
    for y in range(ys[0], ys[1] + 1):
        O.call('server_set_blocks', instance=SERVER, mode='fill', load=True,
               block=name_of(block), metadata=meta, x=X0 + xs[0], y=y, z=Z0 + zs[0],
               toX=X0 + xs[1], toY=y, toZ=Z0 + zs[1])


def cmd(c):
    flush()
    r = O.call('server_run_command', instance=SERVER, command=c)
    if isinstance(r, dict):
        if r.get('error') or not r.get('succeeded', True):
            print('  !', c, '->', r.get('error') or r.get('output'))
        elif c.startswith('csm'):
            print('  ', c.split(' ')[0], c.split(' ')[1], '->', ' '.join(r.get('output', []))[:120])
    return r


def at(x, y, z):
    """World coordinates of a site point, for a command."""
    return '%d %d %d' % (X0 + x, y, Z0 + z)


def sign(x, y, z, lines, rot=0):
    """A standing sign; rot 0 faces south, 4 west, 8 north, 12 east."""
    put(x, y, z, 'minecraft:standing_sign', rot, _sign_nbt(lines))


def wall_sign(x, y, z, lines, out):
    """A wall sign; out 2 north, 3 south, 4 west, 5 east."""
    put(x, y, z, 'minecraft:wall_sign', out, _sign_nbt(lines))


def _sign_nbt(lines):
    parts = ['Text%d:%s' % (i + 1, json.dumps(json.dumps({'text': t})))
             for i, t in enumerate(lines[:4])]
    return '{' + ','.join(parts) + '}'


# ------------------------------------------------------------------------------------------
# The Healthcare placing conventions (HEALTHCARE_SYSTEM.md, Placing by command)
# ------------------------------------------------------------------------------------------
def floor(reg, x, z, face, flags=0, y=Y):
    """A floor piece: bits 0-1 the facing, flags from bit 2."""
    put(x, y, z, reg, face | (flags << 2))


def wall(reg, x, y, z, out, flag=0):
    """A wall piece in the wall-face cell: bits 0-2 the way out of the wall, bit 3 a flag."""
    put(x, y, z, reg, OUT[out] | (flag << 3))


def lit(reg, x, y, z, face, state=3):
    """A lit piece on Lighting's metadata: horizontal facing * 4 + state (3 lit by hand)."""
    put(x, y, z, reg, face * 4 + state)


def wall_lit(reg, x, y, z, out, state=3):
    lit(reg, x, y, z, WALL_TO_H[OUT[out]], state)


def wide(reg, x, z, face, flags=0, y=Y):
    """A two-wide floor pair: the second cell to the viewer's right."""
    dx, dz = STEP[CCW[face]]
    put(x, y, z, reg, face | (flags << 2))
    put(x + dx, y, z + dz, reg, face | 4 | (flags << 2))


def wide_wall(reg, x, y, z, out):
    h = WALL_TO_H[OUT[out]]
    dx, dz = STEP[CCW[h]]
    put(x, y, z, reg, OUT[out])
    put(x + dx, y, z + dz, reg, OUT[out] | 8)


def tall(reg, x, z, face, y=Y):
    put(x, y, z, reg, face)
    put(x, y + 1, z, reg, face | 4)


def tall_wall(reg, x, y, z, out):
    put(x, y, z, reg, OUT[out])
    put(x, y + 1, z, reg, OUT[out] | 8)


def bed(reg, x, z, face, rails=True, raised=False):
    """A hospital bed: the head cell against the wall, the foot one step the way it faces."""
    put(x, Y, z, reg, face | (4 if rails else 0) | (8 if raised else 0))
    dx, dz = STEP[face]
    put(x + dx, Y, z + dz, 'hospital_bed_foot', face)


def hung(reg, x, z, face, lift, meta=None):
    """A ceiling-hung head at its working height; the column is filled by command after."""
    put(x, Y + lift, z, reg, face if meta is None else meta)
    return (x, Y + lift, z)


def door(reg, x, z, face, hinge_right=False, open_=False):
    """A two-block Building door: lower = facing (the way in) + open; upper = 8 + hinge."""
    put(x, Y, z, reg, face | (4 if open_ else 0))
    put(x, Y + 1, z, reg, 8 | (1 if hinge_right else 0))


def run(reg, x0, x1, z, face, flags=0, y=Y):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        put(x, y, z, reg, face | (flags << 2))


# ------------------------------------------------------------------------------------------
# The shell: floor, walls, ceiling and lights
# ------------------------------------------------------------------------------------------
# Room partitions: x of the walls between rooms, north and south of the corridor.
NORTH_WALLS = [0, 12, 24, 40, 62, 74, 84, 94, XMAX]
SOUTH_WALLS = [0, 12, 22, 32, 44, 60, 70, 82, 90, 98, XMAX]


def shell():
    fill(-1, G, ZN, XMAX, G, ZS, 'minecraft:concrete', 0)           # room floors, white
    fill(0, G, 0, XMAX - 1, G, 4, 'minecraft:concrete', 8)          # the corridor, light grey
    fill(-1, Y, ZN, XMAX, CEIL - 1, ZS, 'minecraft:air')
    for z in (ZN, ZS, CN, CS):
        fill(-1, Y, z, XMAX, CEIL - 1, z, 'minecraft:concrete', 0)
    for x in (-1, XMAX):
        fill(x, Y, ZN, x, CEIL - 1, ZS, 'minecraft:concrete', 0)
    for x in NORTH_WALLS:
        fill(x, Y, ZN, x, CEIL - 1, CN, 'minecraft:concrete', 0)
    for x in SOUTH_WALLS:
        fill(x, Y, CS, x, CEIL - 1, ZS, 'minecraft:concrete', 0)
    fill(-1, CEIL, ZN, XMAX, CEIL, ZS, 'minecraft:concrete', 0)
    for x in range(1, XMAX, 4):
        for z in range(ZN + 2, ZS - 1, 4):
            put(x, CEIL, z, 'minecraft:sea_lantern')
    # the corridor lit down its middle
    for x in range(3, XMAX, 4):
        put(x, CEIL, 2, 'minecraft:sea_lantern')
    flush()


# ------------------------------------------------------------------------------------------
# The corridor
# ------------------------------------------------------------------------------------------
DOORS_N = set()   # the x of every door in the corridor's north wall (the rails break there)
DOORS_S = set()   # and in its south wall
OPEN_S = set(range(46, 59)) | set(range(2, 11))   # the waiting room and the station


def corridor_door(x, north, reg='door_hosp_maple_lite', num=0, name=0, dome=False,
                  hinge_right=False):
    z = CN if north else CS
    face = N if north else S
    door(reg, x, z, face, hinge_right)
    side = 0 if north else 4                       # the corridor cell beside the wall
    plate = 'room_plate_blue' if north else 'room_plate_charcoal'
    if num:
        put(x + 1, Y + 2, side, plate, N if north else S,
            '{num:%d,name:%d}' % (num, name))
    if dome:
        # over the door, on the corridor side: Lighting meta, facing out of the wall
        put(x, Y + 2, side, 'nurse_call_dome_light', (S if north else N) * 4 + 0)
    (DOORS_N if north else DOORS_S).add(x)


def corridor():
    # handrails at y 5 and a wayfinding stripe at y 7, on both walls, broken at the doors and
    # where the station and the waiting room open onto the corridor
    for x in range(0, XMAX):
        for north in (True, False):
            zc = 0 if north else 4
            if north:
                gap = x in DOORS_N or x in (17, 18)          # (17, 18): the ICU's sliders
            else:
                gap = x in DOORS_S or x in OPEN_S
            if not gap:
                put(x, Y + 1, zc, 'corridor_handrail', OUT['S'] if north else OUT['N'])
                if north and 25 <= x <= 39:
                    put(x, Y, zc, 'corridor_crash_rail', OUT['S'])
            if north or x not in OPEN_S:
                colour = 'wall_stripe_blue' if north else 'wall_stripe_teal'
                put(x, Y + 3, zc, colour, (N if north else S) | (1 << 2))
    # the main entrance in the west wall: a sliding pair between sidelites, a mat each side
    for z, reg, upper in ((1, 'auto_door_sidelite_clear', 8), (2, 'auto_door_slider_clear', 8),
                          (3, 'auto_door_slider_clear', 9), (4, 'auto_door_sidelite_clear', 8)):
        put(-1, Y, z, reg, E)
        put(-1, Y + 1, z, reg, upper)
    for z in (2, 3):
        put(-2, Y, z, 'auto_door_mat')
        put(0, Y, z, 'auto_door_mat')
    # a cross-corridor smoke barrier pair at x 64 (z 1 and 2), held open by redstone blocks
    # under the floor: take the redstone away and they shut
    fill(64, Y, 0, 64, CEIL - 1, 4, 'minecraft:concrete', 0)
    door('door_smoke_metal_grey', 64, 1, E, False, True)
    door('door_smoke_metal_grey', 64, 2, E, True, True)
    for z in (1, 2):
        put(64, G, z, 'minecraft:redstone_block')
    flush()


WAYFINDING = []   # (cells, nbt) to write after the cells are set


def panel(x, z0, z1, y0, y1, nbt):
    """A hospital wayfinding panel across the corridor, facing west (towards the entrance):
    cells x, z0..z1, y0..y1; the NBT is written once to its bottom-left cell (as read)."""
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            put(x, y, z, 'hospital_wayfinding_panel', W)
    WAYFINDING.append(((x, y0, z0), nbt))


def wayfinding():
    panel(10, 0, 4, CEIL - 2, CEIL - 1,
          '{l1:"EMERGENCY",l2:"Imaging  Lab",p:"emergency",a:"right",s:"emergency"}')
    panel(36, 0, 3, CEIL - 2, CEIL - 1,
          '{l1:"WARDS  ICU",l2:"Beds 1-24",p:"elevator",a:"up",s:"inpatient"}')
    panel(56, 1, 3, CEIL - 1, CEIL - 1,
          '{l1:"WAITING",p:"info",a:"right",s:"outpatient"}')
    panel(80, 2, 2, CEIL - 1, CEIL - 1, '{l1:"WC",p:"restroom",a:"none",s:"neutral"}')
    # one more read from the far end, facing east
    for z in range(1, 4):
        put(100, CEIL - 1, z, 'hospital_wayfinding_panel', E)
    WAYFINDING.append(((100, CEIL - 1, 3), '{l1:"EXIT",p:"exit",a:"left",s:"exit"}'))
    flush()
    for (x, y, z), nbt in WAYFINDING:
        cmd('blockdata %s %s' % (at(x, y, z), nbt))


# ------------------------------------------------------------------------------------------
# North side
# ------------------------------------------------------------------------------------------
STATION_PANEL = (6, Y + 2, 14)       # the nurse call master panel, in the station
ANNUNCIATOR = (3, Y + 3, 14)
NURSE_DOMES = []
CALLS = []


def ward():
    """Two beds on the north wall with headwalls, a curtain between them and round bed 2."""
    for bx, reg in ((3, 'hospital_bed_blue'), (9, 'hospital_bed_white')):
        bed(reg, bx, -10, S)
        wide_wall('patient_headwall', bx, Y + 1, -10, 'S')
        wide_wall('patient_headwall_upper', bx, Y + 2, -10, 'S')
        wall_lit('patient_reading_light', bx + 1, Y + 3, -10, 'S')
        wall('nurse_call_pillow_speaker', bx - 1, Y + 1, -10, 'S')
        floor('overbed_table', bx + 1, -8, W)
        floor('iv_pole_pump', bx - 1, -9, S, 1)
        floor('visitor_chair', bx + 2, -6, W)
    floor('patient_recliner', 6, -4, N)
    floor('patient_monitor_stand', 2, -8, E, 1)
    wall('patient_monitor_wall', 11, Y + 2, -9, 'W', 1)
    wall('patient_room_tv', 5, Y + 2, -2, 'N', 1)
    wall('vitals_wall_unit', 1, Y + 1, -9, 'E', 1)
    wall('suction_regulator', 1, Y + 1, -8, 'E')
    wall('medical_gas_outlet_o2', 1, Y + 1, -7, 'E')
    wall('sharps_container', 1, Y + 1, -5, 'E')
    wall('glove_box_dispenser', 1, Y + 2, -5, 'E')
    wall('hand_sanitizer_dispenser', 1, Y + 1, -3, 'E')
    wall('nurse_call_button', 3, Y + 1, -2, 'N')
    wall('nurse_call_pull_cord', 11, Y + 1, -3, 'W')
    tall('patient_wardrobe', 10, -2, N)
    floor('biohazard_bin', 11, -5, W)
    # the curtain: a track between the beds (x 6) and round bed 2's foot (z -7)
    for z in range(-10, -6):
        put(6, CEIL, z, 'cubicle_curtain_track', 1)
    for x in range(6, 12):
        put(x, CEIL, -7, 'cubicle_curtain_track', 0)
    corridor_door(7, True, 'door_hosp_maple_lite', 214, 2, dome=True)
    flush()
    cmd('csmhealthcare curtain fill %s auto blue_check' % at(6, CEIL, -9))
    NURSE_DOMES.append((7, Y + 2, 0))
    CALLS.extend([((2, Y + 1, -10), (7, Y + 2, 0)), ((8, Y + 1, -10), (7, Y + 2, 0)),
                  ((3, Y + 1, -2), (7, Y + 2, 0)), ((11, Y + 1, -3), (7, Y + 2, 0))])


def icu():
    bed('hospital_bed_icu', 18, -10, S)
    wide_wall('patient_headwall', 18, Y + 1, -10, 'S')
    wide_wall('patient_headwall_upper', 18, Y + 2, -10, 'S')
    heads = [hung('icu_pendant', 16, -8, E, 1, E | 4)]
    floor('icu_ventilator', 20, -8, W, 1)
    floor('patient_monitor_stand', 20, -9, W, 1)
    floor('iv_pole_pump', 16, -10, S, 1)
    floor('crash_cart', 22, -4, W, 1)
    floor('defibrillator', 22, -5, W, 1)
    floor('workstation_on_wheels', 14, -4, E, 1)
    put(18, CEIL, -8, 'ceiling_patient_lift', S)
    put(18, CEIL, -9, 'ceiling_patient_lift', S)
    wall('code_blue_button', 23, Y + 1, -7, 'W')
    wall('medical_gas_outlet_o2', 13, Y + 1, -7, 'E')
    wall('suction_regulator', 13, Y + 1, -6, 'E')
    floor('ppe_isolation_cart', 21, -2, N)
    bed('hospital_bed_bariatric', 22, -10, S)
    # automatic sliding doors from the corridor and a mat each side
    put(17, Y, CN, 'auto_door_slider_clear', N)
    put(17, Y + 1, CN, 'auto_door_slider_clear', 8)
    put(18, Y, CN, 'auto_door_slider_clear', N)
    put(18, Y + 1, CN, 'auto_door_slider_clear', 9)
    for x in (17, 18):
        put(x, Y, 0, 'auto_door_mat')
        put(x, Y, -2, 'auto_door_mat')
    put(19, Y + 2, 0, 'room_plate_blue', N, '{num:220,name:0}')
    flush()
    return heads


def ed():
    # the tracking board bank on the north wall: two boards side by side
    for x in (31, 32):
        put(x, Y + 2, -10, 'ed_tracking_board', OUT['S'], '{beds:14,seed:3}')
    wide_wall('patient_status_board', 28, Y + 2, -10, 'S')
    floor('triage_desk', 27, -4, E, 1)
    floor('wheelchair', 26, -2, E)
    bed('hospital_bed_white', 35, -10, S, rails=False, raised=True)
    heads = [hung('trauma_exam_light', 35, -8, S, 2, S * 4 + 3),
             hung('surgical_equipment_boom', 37, -8, W, 1, W | 4)]
    floor('crash_cart', 38, -10, W, 1)
    tall_wall('decontamination_shower', 39, Y, -5, 'W')
    floor('decon_sink', 39, -6, W)
    floor('case_cart', 37, -3, N)
    floor('autoclave', 33, -3, N, 1)
    tall('blanket_warming_cabinet', 30, -2, N)
    tall('scrub_attire_dispenser', 29, -2, N)
    tall('medication_dispensing_cabinet', 31, -2, N)
    floor('medication_cart', 25, -9, E)
    floor('medication_fridge', 25, -10, E)
    wall('ambulance_only_sign', 30, Y + 2, ZN - 1, 'N')
    # the ambulance entrance in the north wall: a bronze sliding pair, a mat each side
    for x, hinge in ((32, 8), (33, 9)):
        put(x, Y, ZN, 'auto_door_slider_bronze', S)
        put(x, Y + 1, ZN, 'auto_door_slider_bronze', hinge)
        put(x, Y, ZN - 1, 'auto_door_mat')
        put(x, Y, ZN + 1, 'auto_door_mat')
    fill(26, G, ZN - 8, 39, G, ZN - 1, 'minecraft:concrete', 7)
    wide('ambulance_only_marking', 35, ZN - 4, N)
    wide('ambulance_only_marking', 28, ZN - 4, N)
    wall('wheelchair_corral_sign', 25, Y + 2, -2, 'N')
    floor('wheelchair_storage_rack', 25, -3, E)
    corridor_door(27, True, 'door_hosp_metal_blue_halfglass', 101, 1)
    corridor_door(28, True, 'door_hosp_metal_blue_halfglass', 0, 0, hinge_right=True)
    flush()
    return heads


def imaging():
    put(44, Y, -6, 'ct_scanner', S)
    put(52, Y, -6, 'mri_scanner', S)
    # the x-ray room at the east end: table, ceiling tube, wall bucky
    wide('xray_table', 57, -7, S)
    heads = [hung('xray_ceiling_tube', 57, -7, S, 2)]
    tall('xray_wall_bucky', 60, -10, S)
    tall('c_arm', 42, -2, N)
    floor('c_arm_monitor_cart', 41, -3, N, 1)
    tall('mammography_unit', 47, -10, S)
    wall('lead_apron_rack', 61, Y + 1, -4, 'W')
    wide('radiology_reading_workstation', 56, -2, N)
    floor('visitor_chair', 56, -3, S)
    wall('radiation_warning_sign', 46, Y + 2, 0, 'S')
    for i, x in enumerate((50, 51, 52, 53)):
        wall('mri_zone_%d_sign' % (i + 1), x, Y + 2, 0, 'S')
    wall_lit('xray_in_use_light', 45, Y + 2, 0, 'S', 3)
    wall_lit('mri_in_use_light', 49, Y + 2, 0, 'S', 3)
    corridor_door(44, True, 'door_lead_lined')
    corridor_door(48, True, 'door_lead_lined', hinge_right=True)
    flush()
    return heads


def lab():
    run('lab_bench', 64, 72, -10, S)
    put(68, Y, -10, 'lab_bench_sink', S)
    put(64, Y + 1, -10, 'lab_centrifuge', S)
    put(66, Y + 1, -10, 'lab_microscope', S)
    put(70, Y + 1, -10, 'haematology_analyser', S | 4)
    floor('chemistry_analyser', 73, -8, W, 1)
    tall('fume_hood', 63, -7, E)
    tall('biosafety_cabinet', 63, -5, E)
    tall('blood_bank_fridge', 73, -4, W)
    wall('specimen_pass_through', 66, Y + 1, -2, 'N')
    wall('pneumatic_tube_station', 70, Y, -2, 'N')
    corridor_door(68, True, 'door_hosp_metal_sage_lite', 310, 8)
    flush()


def pharmacy():
    tall('iv_compounding_hood', 76, -10, S)
    tall('pharmacy_bin_shelving', 78, -10, S)
    tall('pharmacy_bin_shelving', 79, -10, S)
    tall('pharmacy_unit_dose_cabinet', 81, -10, S)
    tall('medication_dispensing_cabinet', 83, -7, W)
    floor('medication_fridge', 83, -5, W)
    floor('medication_cart', 77, -5, E)
    corridor_door(79, True, 'door_hosp_metal_sage_lite', 315, 7)
    flush()


def morgue():
    for x in (86, 87, 88):
        put(x, Y, -10, 'morgue_cabinet', S)
        put(x, Y + 1, -10, 'morgue_cabinet', S | 4)
    put(87, Y, -10, 'morgue_cabinet', S | 8)          # one door open, tray out
    wide('autopsy_table', 88, -6, S)
    wide('body_cart', 91, -3, N)
    wall('hand_sanitizer_dispenser', 85, Y + 1, -6, 'E')
    corridor_door(89, True, 'door_hosp_metal_blue_lite', 0, 6)
    flush()


# ------------------------------------------------------------------------------------------
# South side
# ------------------------------------------------------------------------------------------
def station():
    """The nurses' station opens onto the corridor: counters along z 7 facing the corridor."""
    fill(2, Y, CS, 10, CEIL - 1, CS, 'minecraft:air')
    run('nurse_station_counter', 3, 9, 7, N)
    put(5, Y, 7, 'nurse_station_counter_monitors', N)
    put(7, Y, 7, 'nurse_station_counter_monitors', N)
    put(2, Y, 7, 'nurse_station_corner', N)
    put(2, Y, 8, 'nurse_station_counter', W)   # a corner's south side takes facing.rotateYCCW()
    put(2, Y, 9, 'nurse_station_counter', W)
    put(4, Y + 1, 7, 'nurse_screen_clear', N)
    put(8, Y + 1, 7, 'nurse_screen_clear', N)
    put(*STATION_PANEL, block='nurse_call_master_panel', meta=OUT['N'])
    put(*ANNUNCIATOR, block='code_blue_annunciator', meta=OUT['N'])
    put(4, Y + 2, 14, 'medical_gas_master_alarm', N * 4 + 0)
    wide_wall('patient_status_board', 9, Y + 2, 14, 'N')
    floor('workstation_on_wheels', 4, 10, S, 1)
    floor('medication_cart', 9, 11, N)
    wall('medical_gas_zone_valve_box', 11, Y + 1, 10, 'W')
    wall('medical_gas_zone_sign', 11, Y + 3, 10, 'W')
    wall('hospital_outlet_normal', 1, Y, 10, 'E')
    wall('hospital_outlet_emergency', 1, Y, 11, 'E')
    wall('hospital_outlet_pair', 1, Y, 12, 'E')
    floor('clean_linen_cart', 10, 13, N)
    floor('soiled_linen_hamper', 11, 13, N)
    flush()


def clinic():
    wide('exam_table', 14, 12, N)
    lit('clinic_exam_light', 17, Y, 11, W)
    floor('clinic_step_stool', 15, 10, N)
    wall('wall_diagnostic_set', 13, Y + 1, 9, 'E')
    floor('patient_scale', 13, 7, E, 1)
    floor('phlebotomy_chair', 16, 7, S)
    # the eye lane: the chair facing the chart on the far wall
    floor('refraction_chair', 20, 13, N)
    wall('visual_acuity_chart', 20, Y + 1, 6, 'S')
    floor('dental_chair', 18, 8, E, 1)
    corridor_door(19, False, 'door_hosp_maple_lite', 118, 1)
    flush()


def dialysis():
    for x in (24, 27):
        floor('dialysis_chair', x, 13, N)
        floor('dialysis_machine', x + 1, 13, N, 1)
    floor('infusion_recliner', 30, 13, N, 1)
    floor('endoscopy_tower', 30, 9, W, 1)
    tall('dialysis_ro_unit', 23, 8, E)
    corridor_door(27, False, 'door_hosp_maple_halfglass', 130, 0)
    flush()


def maternity():
    bed('hospital_bed_labor', 36, 14, N)
    wide_wall('patient_headwall', 36, Y + 1, 14, 'N')
    lit('infant_warmer', 39, Y, 13, N)
    floor('bassinet', 34, 12, E)
    floor('nicu_incubator', 41, 13, N, 1)
    floor('nicu_incubator', 42, 13, N, 1)
    floor('visitor_chair', 33, 9, E)
    bed('hospital_bed_mauve', 33, 14, N)          # a postpartum bed beside the labour bed
    floor('iv_pole', 35, 12, N)
    corridor_door(38, False, 'door_hosp_metal_sage_halfglass', 140, 2)
    wall('infant_security_panel', 40, Y + 1, 4, 'N')
    flush()


def waiting():
    fill(46, Y, CS, 58, CEIL - 2, CS, 'minecraft:air')
    for z, reg in ((8, 'waiting_beam_chair_blue'), (10, 'waiting_beam_chair_teal'),
                   (12, 'waiting_beam_chair_charcoal')):
        run(reg, 46, 50, z, S)                      # facing the displays on the back wall
        run(reg, 53, 57, z, S)
    put(52, Y, 12, 'waiting_beam_chair_blue', S)   # a colour in another row joins
    floor('check_in_kiosk', 58, 7, W)
    run('nurse_station_counter', 47, 49, 14, N)
    put(48, Y + 1, 14, 'queue_desk_console', N)
    put(50, Y + 2, 14, 'now_serving_display', OUT['N'], '{desk:3}')
    put(56, Y + 2, 14, 'now_serving_display', OUT['N'], '{desk:1}')
    for x in (45, 59):
        put(x, Y, 14, 'planter_terracotta_large')
    flush()
    cmd('csmhealthcare queue link %s %s' % (at(58, Y, 7), at(56, Y + 2, 14)))
    cmd('csmhealthcare queue link %s %s' % (at(48, Y + 1, 14), at(50, Y + 2, 14)))
    cmd('csmhealthcare queue set %s 42' % at(58, Y, 7))
    cmd('csmhealthcare queue set %s 117' % at(48, Y + 1, 14))


def chapel():
    for z in (7, 9, 11):
        run('pew_oak', 61, 64, z, S)
        run('pew_walnut', 66, 69, z, S)
    floor('altar_table_oak', 65, 13, N)
    floor('lectern_oak', 67, 13, N)
    corridor_door(65, False, 'door_hosp_maple_lite', 0, 0)
    flush()


def lecture():
    """Tiered seating facing the front (the south wall): the front row on the floor, the row
    behind it on a half riser, then a block up and so on."""
    rows = [(12, 0, 0), (11, 0, 1), (10, 1, 0), (9, 1, 1), (8, 2, 0)]
    for z, up, raised in rows:
        if up:
            fill(71, Y, z, 81, Y + up - 1, z, 'minecraft:concrete', 8)
        run('lecture_hall_seat', 72, 80, z, S, 1 if raised else 0, y=Y + up)
    # the front: lectern, a bed with the mannequin, the skeleton and a dissection table
    floor('lectern_walnut', 76, 14, N)
    bed('hospital_bed_green', 72, 14, N)
    put(72, Y + 1, 14, 'simulation_mannequin', N)
    tall('skeleton_stand', 79, 14, N)
    wide('dissection_table', 81, 13, W)
    put(81, Y + 1, 13, 'anatomical_torso_model', W)
    corridor_door(76, False, 'door_hosp_maple_halfglass', 0, 0)
    flush()


def playroom():
    floor('kids_play_table', 86, 10, N)
    floor('kids_chair_red', 86, 9, S)
    floor('kids_chair_blue', 85, 10, E)
    floor('kids_chair_green', 87, 10, W)
    floor('playroom_toy_box', 86, 14, N)
    corridor_door(86, False, 'door_hosp_maple_lite', 0, 0)
    flush()


def laundry():
    for x, reg in ((91, 'commercial_washer_stainless'), (92, 'commercial_washer_stainless'),
                   (94, 'commercial_dryer_stainless'), (95, 'commercial_dryer_stainless')):
        floor(reg, x, 14, N)
    floor('soiled_linen_hamper', 97, 12, W)
    floor('clean_linen_cart', 97, 10, W)
    corridor_door(93, False, 'door_hosp_metal_blue_lite', 0, 3)
    flush()


def rehab():
    run('parallel_bars', 100, 104, 12, N)
    wide('treatment_plinth', 106, 13, N)
    wide('exercise_stairs', 101, 8, N)
    tall('patient_lift_mobile', 109, 10, W)
    floor('walker', 104, 9, N)
    floor('wheelchair', 108, 7, W)
    put(102, CEIL, 12, 'ceiling_patient_lift', E)
    put(103, CEIL, 12, 'ceiling_patient_lift', E)
    corridor_door(104, False, 'door_hosp_maple_halfglass', 0, 0)
    flush()


# ------------------------------------------------------------------------------------------
# Wiring and the roof
# ------------------------------------------------------------------------------------------
def wiring(heads):
    for h in heads:
        cmd('csmhealthcare boom fill %s' % at(*h))
    panel_at = at(*STATION_PANEL)
    for dome in NURSE_DOMES:
        cmd('csmhealthcare nursecall link %s %s' % (panel_at, at(*dome)))
    for device, dome in CALLS:
        cmd('csmhealthcare nursecall link %s %s' % (at(*dome), at(*device)))
    # one call left on: bed 1's pillow speaker (set it from air with the calling bit)
    x, y, z = CALLS[0][0]
    put(x, y, z, 'minecraft:air')
    flush()
    put(x, y, z, 'nurse_call_pillow_speaker', OUT['S'] | 8)
    flush()
    # code blue: the ICU's button calls the station's annunciator
    cmd('csmhealthcare codeblue link %s %s' % (at(23, Y + 1, -7), at(*ANNUNCIATOR)))


def roof():
    """A 9 x 9 hospital pad on the roof over the ED, laid by Transit's command, with green edge
    lights round it, two floodlights and the wind sock, all on the helipad circuit."""
    cx, cz = 32, -6
    # the pad needs a solid deck: the ceiling's sea lanterns under it are not one
    fill(cx - 4, CEIL, cz - 4, cx + 4, CEIL, cz + 4, 'minecraft:concrete', 0)
    cmd('csmtransit helipad %s 9 hospital north D12 4T' % at(cx, CEIL + 1, cz))
    for dx, dz in ((-5, -5), (5, -5), (-5, 5), (5, 5), (0, -5), (0, 5), (-5, 0), (5, 0)):
        put(cx + dx, CEIL + 1, cz + dz, 'airport_helipad_edge_light', 4)
    put(cx - 6, CEIL + 1, cz + 2, 'airport_helipad_floodlight', E | 4)
    put(cx + 6, CEIL + 1, cz - 2, 'airport_helipad_floodlight', W | 4)
    put(cx + 7, CEIL + 1, cz - 4, 'airport_wind_sock', 0)
    flush()


# ------------------------------------------------------------------------------------------
# The check: every block of the two Healthcare tabs, placed or on a plinth
# ------------------------------------------------------------------------------------------
CLASS_REGISTRY = {
    'BlockNurseCallMasterPanel': 'nurse_call_master_panel',
    'BlockCurtainTrack': 'cubicle_curtain_track',
    'BlockEdTrackingBoard': 'ed_tracking_board',
    'BlockMorgueCabinet': 'morgue_cabinet',
    'BlockSimulationMannequin': 'simulation_mannequin',
    'BlockLectureSeating': 'lecture_hall_seat',
    'BlockExerciseStairs': 'exercise_stairs',
    'BlockNowServingDisplay': 'now_serving_display',
    'BlockHospitalWayfindingPanel': 'hospital_wayfinding_panel',
}


def registered():
    names = []
    for tab in ('CsmTabHealthcarePatientCare.java', 'CsmTabHealthcareClinical.java'):
        for line in open(os.path.join(TABS, tab), encoding='utf-8'):
            m = re.search(r'initTabBlock\(new (\w+)\((.*)', line)
            if not m:
                continue
            cls, rest = m.group(1), m.group(2)
            s = re.match(r'"([a-z0-9_]+)"', rest)
            v = re.match(r'HospitalBedVariant\.(\w+)', rest)
            if s:
                names.append(s.group(1))
            elif v:
                names.append('hospital_bed_' + v.group(1).lower())
            elif cls in CLASS_REGISTRY:
                names.append(CLASS_REGISTRY[cls])
            else:
                print('  ? cannot name %s' % line.strip())
    return names


def plinths():
    names = registered()
    missing = [r for r in names if r not in placed]
    for i, reg in enumerate(missing):
        x, z = i * 3, ZS + 4
        put(x, Y, z, 'minecraft:stonebrick')
        put(x, Y + 1, z, reg, 0)
        words, lines = reg.split('_'), ['']
        for w in words:
            if len(lines[-1]) + len(w) + 1 > 15:
                lines.append('')
            lines[-1] = (lines[-1] + ' ' + w).strip()
        sign(x, Y, z + 1, lines[:4], 0)
    flush()
    print('registered %d, placed in the hospital %d, on plinths %d%s'
          % (len(names), len(names) - len(missing), len(missing),
             (': ' + ', '.join(missing)) if missing else ''))


BOARD = [
    ['Healthcare Demo', 'one hospital', 'floor (x, z)', ''],
    ['North: ward', 'ICU, ED, imaging', 'lab, pharmacy,', 'morgue'],
    ['South: station', 'clinic, dialysis', 'maternity, wait,', 'chapel, lecture'],
    ['Then playroom,', 'laundry, rehab;', 'helipad on the', 'roof over the ED'],
    ['Try: beds (rails', 'sneak: height),', 'curtains, kiosk,', 'code blue button'],
]


def board():
    fill(-6, Y, -2, -6, Y + 2, 7, 'minecraft:stonebrick')
    for i, lines in enumerate(BOARD):
        wall_sign(-7, Y + 1, 0 + i, lines, OUT['W'])
    put(-6, Y + 3, 2, 'minecraft:sea_lantern')
    flush()


# ------------------------------------------------------------------------------------------
cmd('gamerule doDaylightCycle false')
cmd('gamerule doMobSpawning false')
cmd('difficulty peaceful')
cmd('time set 6000')
cmd('weather clear')
shell()
print('shell done')
heads = []
ward()
heads += icu()
heads += ed()
heads += imaging()
lab()
pharmacy()
morgue()
print('north done')
station()
clinic()
dialysis()
maternity()
waiting()
chapel()
lecture()
playroom()
laundry()
rehab()
print('south done')
corridor()          # after the rooms, so it knows where their doors are
wayfinding()
wiring(heads)
roof()
board()
plinths()
cmd('setworldspawn %d %d %d' % (X0 - 4, Y, Z0 + 2))
cmd('tp @p %d %d %d -90 0' % (X0 - 4, Y, Z0 + 2))
print('done')
