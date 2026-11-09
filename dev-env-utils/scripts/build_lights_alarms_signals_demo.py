#!/usr/bin/env python3
"""
build_lights_alarms_signals_demo.py -- builds the demo world for the lights, alarms, signals and
HVAC batch, over the MCMCP orchestrator.

Load (or let --create make) a FLAT creative world named "Lights Alarms Signals Demo" in the dev
client with every module, and run this with the client's instance id. Flat ground's top is y 3;
everything stands on y 4. The site, in world blocks (the origin is fixed so --only rebuilds land in
place):

  area       x          z          what
  board      -14..-10   -2..20     the sign board at the spawn (-8, 4, 9)
  hvac       0..50      0..34      the outdoor condensers / heat pumps (heater and cooler, three
                                   finishes), a roof with the packaged rooftop units and the three
                                   large rooftop units, two hotel rooms with a PTAC each (white and
                                   almond, grille outside) on a thermostat
  alarms     0..50      44..80     an office (two wings: zone 1 west, zone 2 east, one panel), a
                                   house whose smoke and CO alarms are interconnected with no panel,
                                   and a house on a home fire keypad; command block buttons trip
                                   each one and reset the panel
  doors      60..124    0..40      every automatic sliding door: standard, wide, telescoping and
                                   their 3-block-tall versions, in all six finishes, bi-parting
  building   60..124    44..64     a holding cell door locked by a door keypad, the store trash
                                   compactor (receivers in four colours), the bear-proof lockers
  arms       0..170     84..112    a street of the 36 luminaire arms (Caltrans and truss, single and
                                   double, galvanized north side, silver south side), the 22 new
                                   street lights on their tips
  historic   0..140     116..150   the Los Angeles upsweep arms and the Anaheim pole on a second
                                   street; the decorative poles with all 17 post tops, lit
  floods     0..80      156..176   the 15 floodlights on every mounting: wall, ground, ceiling,
                                   pole top
  signals    180..270   0..70      an ADVANCED intersection running the real Alto corridor plan
                                   for Elm (coordinated, FYA lefts, ped recall, rest in walk), the
                                   pole-side preempt detectors and confirmation lights, and an
                                   accessible parking space with the R7-8P van accessible plaque

Placements that need the game's own placement logic (arms, fixtures on arm tips, floodlights, the
large rooftop units, doors, pole-side preempt pieces, residential alarms) are clicks through
server_use_item_on_blocks; links are made by command (/csmfirealarm link) or by clicks with the
linker tools. Everything goes through the orchestrator by instance id, and the script refuses any
world not named "Lights Alarms Signals Demo".

Usage:
    python build_lights_alarms_signals_demo.py --instance run-f2eb58 [--create]
                                               [--only hvac,alarms,...] [--night]
"""
import argparse
import json
import os
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from mcmcp_orchestrator import Orchestrator  # noqa: E402

AREAS = ('hvac', 'alarms', 'doors', 'building', 'arms', 'historic', 'floods', 'signals', 'board')

ap = argparse.ArgumentParser(description='Build the lights, alarms and signals demo world.')
ap.add_argument('--instance', required=True, help="the dev game's id, e.g. run-f2eb58")
ap.add_argument('--create', action='store_true', help='create and load the flat world first')
ap.add_argument('--only', default=None, help='comma-separated areas: ' + ','.join(AREAS))
ap.add_argument('--night', action='store_true', help='finish at night (time 14000)')
ARGS = ap.parse_args()
ONLY = set(AREAS) if not ARGS.only else set(a.strip() for a in ARGS.only.split(','))
for _a in ONLY:
    if _a not in AREAS:
        sys.exit('unknown area %r (areas: %s)' % (_a, ', '.join(AREAS)))

WORLD_NAME = 'Lights Alarms Signals Demo'
O = Orchestrator()
CLIENT, SERVER = ARGS.instance + '.client', ARGS.instance + '.server'
if ARGS.create:
    O.call('client_world_create', instance=CLIENT, name=WORLD_NAME, worldType='flat',
           generateStructures=False, seed='1109')
    time.sleep(15)
_info = O.call('server_world_info', instance=SERVER)
if WORLD_NAME not in json.dumps(_info):
    sys.exit('refusing: the world on %s is not "%s"' % (SERVER, WORLD_NAME))
PLAYER = O.call('server_list_players', instance=SERVER)['players'][0]['name']

G, Y = 3, 4
S, W, N, E = 0, 1, 2, 3                       # horizontal index (HVAC, props, signal heads x4)
UD = {'down': 0, 'up': 1, N: 2, S: 3, W: 4, E: 5}   # EnumFacing index (NSEWUD blocks)
UPPOLE = 1                                    # a vertical pole's metadata
REAR = 1                                      # signal head mount: bracket to the pole behind

blocks = []


def put(x, y, z, block, meta=0, nbt=None, upper=None):
    b = {'x': x, 'y': y, 'z': z, 'block': block if ':' in block else 'csm:' + block,
         'metadata': meta}
    if nbt:
        b['nbt'] = nbt
    if upper is not None:
        b['upper'] = upper
    blocks.append(b)


def flush():
    global blocks
    for i in range(0, len(blocks), 800):
        O.call('server_set_blocks', instance=SERVER, mode='list', blocks=blocks[i:i + 800],
               load=True)
    blocks = []


def fill(x0, y0, z0, x1, y1, z1, block, meta=0):
    flush()
    O.call('server_set_blocks', instance=SERVER, mode='fill', load=True,
           block=block if ':' in block else 'csm:' + block, metadata=meta,
           x=min(x0, x1), y=min(y0, y1), z=min(z0, z1),
           toX=max(x0, x1), toY=max(y0, y1), toZ=max(z0, z1))


def cmds(lines):
    flush()
    r = O.call('server_run_commands', instance=SERVER, commands=lines, allResults=True)
    for res in r.get('results', []):
        if not res.get('succeeded'):
            print('  command failed:', lines[res['i']], res.get('output'))
    return r


def tp(x, y, z, yaw=180, pitch=20):
    O.call('server_teleport_player', instance=SERVER, player=PLAYER, x=x, y=y, z=z, yaw=yaw,
           pitch=pitch, fly=True)
    time.sleep(0.3)


def uses(clicks, item=None):
    """Clicks in order; each {x, y, z, face?, sneak?, item?}; item: a block/item id lent for all."""
    flush()
    out = []
    for i in range(0, len(clicks), 400):
        args = dict(instance=SERVER, player=PLAYER, uses=clicks[i:i + 400], readBack=True,
                    load=True)
        if item:
            args['item'] = {'id': item if ':' in item else 'csm:' + item}
        r = O.call('server_use_item_on_blocks', **args)
        for u in r.get('uses', []):
            if u.get('result') != 'success':
                print('  use failed:', json.dumps(u)[:300])
        out += r.get('uses', [])
    return out


def click(x, y, z, item, face='up', sneak=False):
    c = {'x': x, 'y': y, 'z': z, 'face': face, 'item': {'id': item if ':' in item
                                                        else 'csm:' + item}}
    if sneak:
        c['sneak'] = True
    return c


def toggle(positions, times=2):
    """Right-click each block `times` times (a stick in hand still runs the block's own click):
    a CSM light goes RS_OFF -> MAN_OFF -> MAN_ON."""
    clicks = []
    for (x, y, z) in positions:
        for _ in range(times):
            clicks.append({'x': x, 'y': y, 'z': z, 'face': 'north'})
    if clicks:
        uses(clicks, 'minecraft:stick')


def find(box, patterns):
    x0, y0, z0, x1, y1, z1 = box
    r = O.call('server_find_blocks', instance=SERVER, x=x0, y=y0, z=z0, toX=x1, toY=y1, toZ=z1,
               blocks=patterns, load=True, limit=5000)
    pal = r.get('palette', [])
    return [(p[0], p[1], p[2], pal[p[3]]['block']) for p in r.get('positions', [])]


def sign_nbt(lines):
    parts = ['Text%d:%s' % (i + 1, json.dumps(json.dumps({'text': t})))
             for i, t in enumerate(lines[:4])]
    return '{' + ','.join(parts) + '}'


def sign(x, y, z, lines, rot=0):
    """A standing sign; rot 0 faces south, 4 west, 8 north, 12 east."""
    put(x, y, z, 'minecraft:standing_sign', rot, sign_nbt(lines))


def wall_sign(x, y, z, lines, facing):
    """A wall sign; facing 2 north, 3 south, 4 west, 5 east."""
    put(x, y, z, 'minecraft:wall_sign', facing, sign_nbt(lines))


def button(x, z, command, lines, rot=0):
    """A command block under the ground with a stone button on a post above it and a sign."""
    put(x, G, z, 'minecraft:command_block', 0, '{Command:%s}' % json.dumps(command))
    put(x, Y, z, 'minecraft:stonebrick')
    put(x, Y + 1, z, 'minecraft:stone_button', 5)       # on top of the post
    sign(x, Y, z + (1 if rot == 0 else -1), lines, rot)


def clear(x0, z0, x1, z1, top=30, ground=('minecraft:grass', 0)):
    fill(x0, Y, z0, x1, top, z1, 'minecraft:air')
    fill(x0, G, z0, x1, G, z1, *ground)


def box_walls(x0, z0, x1, z1, y0, y1, block='minecraft:stonebrick', meta=0):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(x, y, z0, block, meta)
            put(x, y, z1, block, meta)
        for z in range(z0 + 1, z1):
            put(x0, y, z, block, meta)
            put(x1, y, z, block, meta)


# ------------------------------------------------------------------------------------------
# HVAC
# ------------------------------------------------------------------------------------------
def build_hvac():
    clear(0, 0, 50, 34)
    # Condensers / heat pumps on a pad, facing south toward the board's path
    fill(0, G, 2, 18, G, 4, 'minecraft:concrete', 8)
    x = 1
    for kind in ('heater', 'cooler'):
        for finish in ('', '_black', '_silver'):
            put(x, Y, 3, 'hvac_%s%s' % (kind, finish), S)
            x += 3
    sign(1, Y, 6, ['Condensing unit', 'heat pump:', 'heater / cooler,', 'sandstone, black, silver'][:4], 0)
    sign(10, Y, 6, ['Placed units', 'keep their links', 'and NBT: only', 'the look changed'], 0)

    # The roof: packaged rooftop units (old ids, new look) and the large rooftop units
    fill(22, G, 0, 50, G, 20, 'minecraft:concrete', 8)
    box_walls(22, 0, 50, 20, Y, 7, 'minecraft:concrete', 0)
    fill(22, 8, 0, 50, 8, 20, 'minecraft:concrete', 8)
    x = 24
    for kind in ('heater', 'cooler'):
        for finish in ('', '_black', '_silver'):
            put(x, 9, 2, 'hvac_rtu_%s%s' % (kind, finish), S)
            x += 4
    flush()
    tp(36, 10, 30, 180, 30)
    uses([click(25 + 9 * i, 8, 12, 'hvac_rtu_large' + f) for i, f in
          enumerate(('', '_black', '_silver'))])
    sign(24, 9, 4, ['Packaged RTUs', 'heater / cooler', 'x3 finishes', '(same ids)'], 0)
    sign(23, 9, 18, ['Large RTU', '3 x 2 x 5, heats', 'and cools,', 'ducted only'], 0)
    for y in range(Y, 9):
        put(21, y, 10, 'minecraft:ladder', 4)        # up the west wall to the roof

    # Two hotel rooms, each with a PTAC in the south wall (grille outside) on its thermostat
    fill(0, G, 14, 16, G, 24, 'minecraft:planks', 1)
    box_walls(0, 14, 16, 24, Y, 7, 'minecraft:stonebrick')
    for z in range(15, 24):
        for y in range(Y, 8):
            put(8, y, z, 'minecraft:stonebrick')
    fill(0, 8, 14, 16, 8, 24, 'minecraft:stone_slab', 8)
    for rx, unit in ((4, 'hvac_ptac'), (12, 'hvac_ptac_almond')):
        put(rx, Y, 23, unit, N)                      # flush against the south wall, into the room
        put(rx, Y, 25, 'hvac_ptac_wall_grille', S)   # the sleeve's grille on the outside face
        put(rx - 2, Y + 1, 15, 'hvac_thermostat', S)  # on the north wall, facing into the room
        put(rx, Y + 2, 14, 'minecraft:glass_pane')
        put(rx + 1, Y + 2, 14, 'minecraft:glass_pane')
        put(rx + 2, Y, 14, 'minecraft:air')          # a doorway on the north side
        put(rx + 2, Y + 1, 14, 'minecraft:air')
    sign(4, Y, 27, ['Hotel PTAC', 'heats and cools', 'grille outside'], 0)
    sign(12, Y, 27, ['PTAC (almond)', 'linked to the', 'room thermostat'], 0)
    flush()
    for rx in (4, 12):
        uses([click(rx - 2, Y + 1, 15, 'hvaclinker', 'south'),
              click(rx, Y, 23, 'hvaclinker', 'north')])
    print('hvac done')


# ------------------------------------------------------------------------------------------
# Fire alarms
# ------------------------------------------------------------------------------------------
OFFICE = (0, 44, 30, 58)
HOUSE1 = (34, 44, 46, 56)
HOUSE2 = (34, 64, 46, 76)
PANEL = (4, Y + 1, 57)


def build_alarms():
    clear(0, 42, 50, 82)
    # The office: two wings divided at x 15, a glass ceiling so it shows from above
    x0, z0, x1, z1 = OFFICE
    fill(x0, G, z0, x1, G, z1, 'minecraft:concrete', 8)
    box_walls(x0, z0, x1, z1, Y, 7, 'minecraft:concrete', 0)
    for z in range(z0 + 1, z1):
        for y in range(Y, 8):
            put(15, y, z, 'minecraft:concrete', 0)
    put(15, Y, 51, 'minecraft:air')
    put(15, Y + 1, 51, 'minecraft:air')
    fill(x0, 8, z0, x1, 8, z1, 'minecraft:glass')
    for x in (7, 22):                                   # front doors (south wall)
        put(x, Y, z1, 'minecraft:air')
        put(x, Y + 1, z1, 'minecraft:air')
    flush()
    put(*PANEL, block='firealarmcontrolpanel', meta=N)    # on the south wall, facing in
    for wx in (4, 19):                                   # each wing: pull, horn strobe, smoke
        put(wx + 6, Y + 1, z1 - 1, 'firealarmgenericpullstation', UD[N])
        put(wx + 4, Y + 2, z0 + 1, 'firealarmsystemsensorlseriesledhornstrobered', UD[S])
        put(wx + 4, 7, 51, 'smoke_detector_photoelectric', UD['down'])
    flush()
    cmds(['csmfirealarm link %d %d %d %d %d %d %d %d %d %d' % (PANEL + (1, Y, z0 + 1, 14, 7, z1 - 1, 1)),
          'csmfirealarm link %d %d %d %d %d %d %d %d %d %d' % (PANEL + (16, Y, z0 + 1, 29, 7, z1 - 1, 2))])
    button(8, 61, 'csmfirealarm trigger 10 %d %d' % (Y + 1, z1 - 1),
           ['Pull: WEST WING', 'zone 1: only', 'the west horns', 'sound'])
    button(23, 61, 'csmfirealarm trigger 25 %d %d' % (Y + 1, z1 - 1),
           ['Pull: EAST WING', 'zone 2 joins', 'the alarm', '(ZONES 1,2)'])
    button(15, 61, 'csmfirealarm reset %d %d %d' % PANEL, ['RESET', 'the panel'])
    button(2, 61, 'csmfirealarm alarm %d %d %d 2' % PANEL, ['Panel alarm', 'zone 2 only'])

    # House 1: Kidde smoke x2, ADT CO, Nest Protect, interconnected with no panel
    for (hx0, hz0, hx1, hz1) in (HOUSE1, HOUSE2):
        fill(hx0, G, hz0, hx1, G, hz1, 'minecraft:planks', 0)
        box_walls(hx0, hz0, hx1, hz1, Y, 6, 'minecraft:brick_block')
        for z in range(hz0 + 1, hz1):
            for y in range(Y, 7):
                put(40, y, z, 'minecraft:planks', 5)
        put(40, Y, hz0 + 6, 'minecraft:air')
        put(40, Y + 1, hz0 + 6, 'minecraft:air')
        fill(hx0, 7, hz0, hx1, 7, hz1, 'minecraft:glass')
        put(43, Y, hz1, 'minecraft:air')
        put(43, Y + 1, hz1, 'minecraft:air')
    flush()
    tp(40, 12, 60, 0, 60)
    u = uses([click(37, 7, 48, 'kiddesmoke', 'down'), click(43, 7, 48, 'kiddesmoke', 'down'),
              click(43, 7, 53, 'nestprotect', 'down'), click(36, Y + 1, 44, 'adt_co_detector', 'south')])
    cmds(['csmfirealarm link 37 6 48 35 %d 45 45 6 55' % Y])
    button(36, 59, 'csmfirealarm trigger 43 6 48', ['Smoke in the', 'bedroom: every', 'alarm in the', 'house sounds'])
    sign(44, Y, 59, ['Kidde, Nest, ADT', 'no panel: click', 'an alarm to', 'test / hush'], 0)

    # House 2: the home fire & security keypad by the front door, alarms linked to it
    put(42, Y + 1, 75, 'home_fire_alarm_keypad', N)
    flush()
    uses([click(37, 7, 68, 'kiddesmoke', 'down'), click(43, 7, 68, 'kiddesmoke', 'down'),
          click(36, Y + 1, 64, 'adt_co_detector', 'south')])
    cmds(['csmfirealarm link 42 %d 75 35 %d 65 45 6 75' % (Y + 1, Y)])
    button(36, 79, 'csmfirealarm trigger 43 6 68', ['Smoke: the', 'keypad reports', 'and the alarms', 'sound'])
    button(44, 79, 'csmfirealarm reset 42 %d 75' % (Y + 1), ['RESET', 'the keypad'])
    print('alarms done')


# ------------------------------------------------------------------------------------------
# Automatic sliding doors
# ------------------------------------------------------------------------------------------
FINISHES = ('clear', 'bronze', 'black', 'white', 'stainless', 'frameless')
# pattern: S sidelite, D door cell; the opening's west half hinges left (8), east half right (9)
STYLES = (
    ('slider', 'sidelite', 'SDDS', ''),
    ('wide_slider', 'sidelite', 'SSDDDDSS', ''),
    ('tele_slider', 'sidelite', 'SDDDDS', ''),
    ('slider', 'sidelite', 'SDDS', 'tall_'),
    ('wide_slider', 'sidelite', 'SSDDDDSS', 'tall_'),
    ('tele_slider', 'sidelite', 'SDDDDS', 'tall_'),
)
STYLE_NAMES = ('Bi-parting', 'Wide', 'Telescoping', 'Tall', 'Tall wide', 'Tall telescoping')


def build_doors():
    clear(60, 0, 126, 42)
    fill(60, G, 0, 126, G, 42, 'minecraft:concrete', 8)
    for si, (door, side, pattern, tall) in enumerate(STYLES):
        z = 4 + si * 6
        tp(92, Y, z + 4, 180, 30)
        clicks, fixes = [], []
        for fi, fin in enumerate(FINISHES):
            x0 = 62 + fi * 10
            doors = [i for i, c in enumerate(pattern) if c == 'D']
            half = len(doors) // 2
            for i, c in enumerate(pattern):
                reg = 'auto_door_%s%s_%s' % (tall, door if c == 'D' else side, fin)
                clicks.append(click(x0 + i, G, z, reg))
                if c == 'D':
                    hinge = 8 if doors.index(i) < half else 9
                    fixes.append((x0 + i, z, reg, hinge))
        uses(clicks)
        for (x, zz, reg, hinge) in fixes:
            put(x, Y + 1, zz, reg, hinge)
        flush()
        sign(60, Y, z, [STYLE_NAMES[si], 'walk up to one:', 'the sensor opens', 'it'], 4)
    for fi, fin in enumerate(FINISHES):
        sign(62 + fi * 10, Y, 40, ['Finish:', fin], 0)
    flush()
    print('doors done')


# ------------------------------------------------------------------------------------------
# Holding cell, trash compactor, bear lockers
# ------------------------------------------------------------------------------------------
def build_building():
    clear(60, 44, 126, 66)
    # holding cell: a 4 x 4 cell, the door and the keypad beside it, outside
    box_walls(62, 46, 67, 51, Y, 6, 'minecraft:iron_block')
    fill(62, 7, 46, 67, 7, 51, 'minecraft:iron_block')
    flush()
    put(64, Y, 51, 'holding_cell_door', N)
    put(64, Y + 1, 51, 'holding_cell_door', N)
    put(66, Y + 1, 52, 'garage_door_keypad', S)
    flush()
    tp(64, Y, 56, 180, 10)
    for (x, y, z) in ((66, Y + 1, 52), (64, Y + 1, 51)):
        O.call('server_activate_block', instance=SERVER, player=PLAYER, x=x, y=y, z=z,
               face='south', sneak=True)
        time.sleep(0.3)
    sign(62, Y, 54, ['Holding cell', 'locked by the', 'door keypad:', 'code or redstone'], 0)

    # trash compactor: control station, power unit, charge box + hopper, receiver; four colours
    fill(74, G, 44, 120, G, 64, 'minecraft:concrete', 7)
    # The props are drawn with the operator's end north (facing N); the receiver is built to
    # size, 2 wide x 2 tall x 7 long, facing south so its rear door is the far end.
    for i, col in enumerate(('blue', 'maroon', 'green', 'grey')):
        x = 78 + i * 10
        put(x, Y, 46, 'trash_compactor_power_unit', N)
        put(x, Y, 47, 'trash_compactor_charge_box', N)
        put(x, Y + 1, 47, 'trash_compactor_hopper', N)
        put(x - 2, Y, 46, 'trash_compactor_control', N)
        for dx in (0, 1):
            for y in (Y, Y + 1):
                for z in range(48, 55):
                    put(x + dx, y, z, 'compactor_container_' + col, S)
    sign(76, Y, 44, ['Store trash', 'compactor:', 'power unit, charge', 'box, receiver'], 8)

    # bear-proof food lockers
    put(112, Y, 46, 'bear_proof_food_locker_brown', S)
    put(115, Y, 46, 'bear_proof_food_locker_galvanized', S)
    sign(113, Y, 48, ['Bear-proof', 'food lockers'], 0)
    flush()
    print('building done')


# ------------------------------------------------------------------------------------------
# Luminaire arms and the new street lights
# ------------------------------------------------------------------------------------------
FIXTURES = ['ltkc0', 'ltkc0hss', 'ltkc1', 'ltkc1hss', 'ltkc2', 'ltkc2hss', 'aeacm', 'aeacmnp',
            'gem250a', 'gem250acu', 'gem250acunp', 'whov25', 'whov25early', 'whov25hps', 'whl250',
            'whl250hps', 'smsmxll5', 'geeals', 'geealsarm', 'hpmongoose', 'gem400a',
            'gem400acunp']


def arm_ids(finish):
    out = []
    for n in (6, 8, 10, 12, 15):
        out += ['luminairearmcaltrans%dft%s' % (n, finish),
                'luminairearmcaltrans%dftdouble%s' % (n, finish)]
    for n in (8, 10, 12, 15):
        out += ['luminairearmtruss%dft%s' % (n, finish), 'luminairearmtruss%dftdouble%s' % (n, finish)]
    return out


def place_arm(x, y, z, arm, face):
    """A curved arm takes its direction from where the placer stands, not the face clicked: stand
    in front of the pole on that face's side, square to it, and click."""
    dz = {'north': -4, 'south': 4}[face]
    tp(x + 0.5, y, z + 0.5 + dz, 180 if dz > 0 else 0, 0)   # facing the pole
    uses([click(x, y, z, arm, face)])


def tips(box, pole):
    """The tip cells of the arm(s) on a pole at `pole` (x, z): the cells farthest from it along z."""
    cells = [c for c in find(box, ['csm:luminairearm*']) if c[0] == pole[0]]
    out = []
    for side in (-1, 1):
        mine = [c for c in cells if (c[2] - pole[1]) * side > 0]
        if mine:
            far = max(mine, key=lambda c: (abs(c[2] - pole[1]), c[1]))
            out.append((far[0], far[1], far[2], 'south' if side > 0 else 'north'))
    return out


def street(z0, z1, x1, sidewalk=8):
    fill(0, G, z0, x1, G, z1, 'minecraft:concrete', 7)
    fill(0, G, z0 - 3, x1, G, z0 - 1, 'minecraft:concrete', 8)
    fill(0, G, z1 + 1, x1, G, z1 + 3, 'minecraft:concrete', 8)
    fill(0, G, (z0 + z1) // 2, x1, G, (z0 + z1) // 2, 'minecraft:concrete', 4)


def build_arms():
    clear(0, 81, 172, 114, 40)
    street(92, 104, 170)
    fx = 0
    lights = []
    for side, (pz, face, finish, pole) in enumerate(
            ((89, 'south', 'galvanized', 'trafficpolevertdblunpainted'),
             (107, 'north', 'silver', 'trafficpolevertdblsilver'))):
        ids = arm_ids(finish)
        for i, arm in enumerate(ids):
            x = 4 + i * 9
            fill(x, Y, pz, x, Y + 8, pz, pole, UPPOLE)
        for i, arm in enumerate(ids):
            place_arm(4 + i * 9, Y + 7, pz, arm, face)
        for i, arm in enumerate(ids):
            x = 4 + i * 9
            sign(x + 1, Y, pz + (-1 if side == 0 else 1),
                 [a for a in (arm.replace('luminairearm', '')[:15], arm[-15:])], 8 if side == 0 else 0)
        flush()
        clicks = []
        for i, arm in enumerate(ids):
            x = 4 + i * 9
            for (tx, ty, tz, tface) in tips((x, Y, pz - 8, x, Y + 14, pz + 8), (x, pz)):
                fix = FIXTURES[fx % len(FIXTURES)]
                fx += 1
                clicks.append(click(tx, ty, tz, fix, tface))
                lights.append((tx, ty, tz + (1 if tface == 'south' else -1)))
        uses(clicks)
    toggle(lights)
    print('arms done (%d street lights)' % len(lights))


# ------------------------------------------------------------------------------------------
# Historic poles: LA upsweep arms, the Anaheim pole, every post top
# ------------------------------------------------------------------------------------------
POST_TOPS = [
    ('decopolelafluted', 'lawilshirespecial'), ('decopolelafluted', 'lametropolitan'),
    ('decopolelawestwood', 'lawestwoodlantern'), ('decopolelabroadwayrose', 'labroadwayrosehead'),
    ('decopolelaconcrete', 'lage104globe'), ('decopolelaconcrete', 'lage198globe'),
    ('decopolelafluted', 'laparagonjr'), ('decopolelaconcrete', 'lallewellyn5'),
    ('decopolelafluted', 'lagenie'), ('decopolelabroadwayrose', 'lavinedouble'),
    ('decopolelahollywood', 'lahollywoodstar'), ('decopolelahollywood', 'lahollywoodwof'),
    ('decopolereprofluted', 'reproacorn'), ('decopolereprofluted', 'reprowashingtonglobe'),
    ('decopolereprofluted', 'reprotwin20'), ('decopolereprofluted', 'reprobishopscrook'),
    ('decopolereprofluted', 'reprotypef'),
]
LA_ARMS = ['luminairearmlaupsweep%dft%s' % (n, f) for n in (8, 10, 12)
           for f in ('black', 'galvanized', 'silver')]
LA_HEADS = ['whov25early', 'whl250', 'gem250a', 'whov25', 'gem400a', 'whl250hps', 'gem250acu',
            'ltkc1', 'aeacm']


def build_historic():
    clear(0, 116, 140, 152, 40)
    street(118, 126, 132)
    # LA upsweep arms on LA concrete poles (south side, arms over the road), the Anaheim pole
    poles = []
    for i, arm in enumerate(LA_ARMS):
        x = 4 + i * 12
        fill(x, Y, 129, x, Y + 7, 129, 'decopolelaconcrete', UPPOLE)
        poles.append((x, arm))
    ax = 4 + len(LA_ARMS) * 12
    fill(ax, Y, 129, ax, Y + 6, 129, 'decopoleanaheim', UPPOLE)
    flush()
    for x, arm in poles:
        place_arm(x, Y + 6, 129, arm, 'north')
    place_arm(ax, Y + 5, 129, 'luminairearmanaheim8ftgreen', 'north')
    clicks, lights = [], []
    for i, (x, arm) in enumerate(poles + [(ax, 'anaheim')]):
        for (tx, ty, tz, tface) in tips((x, Y, 120, x, Y + 14, 132), (x, 129)):
            head = LA_HEADS[i % len(LA_HEADS)] if arm != 'anaheim' else 'gem400a'
            clicks.append(click(tx, ty, tz, head, tface))
            lights.append((tx, ty, tz - 1))
        sign(x + 1, Y, 130, [arm.replace('luminairearm', '')[:15], 'arm'] if arm != 'anaheim'
             else ['Anaheim Resort', 'pole + 8 ft', 'scroll arm'], 0)
    uses(clicks)
    flush()

    # the post tops, switched on by hand (redstone under a pole does not reach its head)
    for i, (pole, top) in enumerate(POST_TOPS):
        x = 4 + i * 6
        for y in range(Y, Y + 4):
            put(x, y, 140, pole, UPPOLE)
        sign(x, Y, 142, [top[:15], 'on ' + pole.replace('decopole', '')[:12]], 0)
    flush()
    tp(52, Y + 2, 148, 180, 10)
    uses([click(4 + i * 6, Y + 3, 140, top, 'up') for i, (pole, top) in enumerate(POST_TOPS)])
    toggle(lights + [(4 + i * 6, Y + 4, 140) for i in range(len(POST_TOPS))])
    print('historic done')


# ------------------------------------------------------------------------------------------
# Floodlights on every mounting
# ------------------------------------------------------------------------------------------
FLOODS = ('lithdsxf1', 'lithdsxf2', 'lithdsxf3', 'lithtfl', 'geefm')


def build_floods():
    clear(0, 154, 80, 180, 30)
    fill(0, G, 154, 80, G, 180, 'minecraft:concrete', 8)
    clicks = []
    for m, f in enumerate(FLOODS):
        x = 4 + m * 15
        # a wall (knuckle on its face), a canopy (yoke under it), a pole (slipfitter on top)
        fill(x, Y, 170, x + 2, Y + 4, 170, 'minecraft:stonebrick')
        fill(x + 5, Y + 4, 164, x + 7, Y + 4, 166, 'minecraft:stone_slab', 8)
        for y in range(Y, Y + 4):
            put(x + 5, y, 166, 'minecraft:cobblestone_wall')
        for y in range(Y, Y + 5):
            put(x + 10, y, 168, 'trafficpolevertical', UPPOLE)
        flush()
        clicks += [click(x + 1, Y + 3, 170, f + 'kn', 'north'),       # knuckle, wall
                   click(x + 1, G, 163, f + 'kn', 'up'),              # knuckle, ground
                   click(x + 3, G, 163, f + 'yk', 'up'),              # yoke, ground
                   click(x + 6, Y + 4, 165, f + 'yk', 'down'),        # yoke, ceiling
                   click(x + 10, Y + 4, 168, f + 'sf', 'up'),         # slipfitter, pole top
                   click(x + 12, G, 163, f + 'sf', 'up')]             # slipfitter, ground
        sign(x + 1, Y, 160, [f.upper(), 'knuckle, yoke', 'slipfitter on', 'wall/ground/pole'], 0)
    tp(40, Y + 6, 150, 0, 30)
    uses(clicks)
    flush()
    lights = [(x, y, z) for (x, y, z, b) in find((0, Y, 154, 80, Y + 8, 180),
                                                 ['csm:lith*', 'csm:geefm*'])]
    toggle(lights)
    print('floods done (%d)' % len(lights))


# ------------------------------------------------------------------------------------------
# Signals: the Elm plan on an ADVANCED intersection
# ------------------------------------------------------------------------------------------
CX, CZ = 225, 30
HALF, WALK = 3, 3
P = HALF + 2
ARM_LEN = 32
ALTO = os.path.join(HERE, '..', '..', 'modules', 'roads', 'src', 'test', 'resources', 'alto',
                    'main_corridor_tcadv_20261109.json')


def snbt(v):
    """JSON -> SNBT the way the engine's own fixture reader does (AltoCorridorPlans.nbt):
    number arrays are int arrays, other arrays lists, numbers longs, booleans bytes."""
    if isinstance(v, dict):
        return '{' + ','.join('%s:%s' % (json.dumps(k) if not k.isidentifier() else k, snbt(x))
                              for k, x in v.items()) + '}'
    if isinstance(v, list):
        if all(isinstance(x, (int, float)) and not isinstance(x, bool) for x in v):
            return '[I;' + ','.join(str(int(x)) for x in v) + ']'
        return '[' + ','.join(snbt(x) for x in v) + ']'
    if isinstance(v, bool):
        return '1b' if v else '0b'
    if isinstance(v, (int, float)):
        return '%dL' % int(v)
    return json.dumps(v)


def L(x, y, z):
    v = ((x & 0x3FFFFFF) << 38) | ((y & 0xFFF) << 26) | (z & 0x3FFFFFF)
    return v - (1 << 64) if v >= 1 << 63 else v


def build_signals():
    clear(CX - ARM_LEN - 8, CZ - ARM_LEN - 8, CX + ARM_LEN + 8, CZ + ARM_LEN + 8, 30)
    box = HALF + WALK
    walk, asphalt = 8, 7
    fill(CX - box, G, CZ - ARM_LEN, CX + box, G, CZ + ARM_LEN, 'minecraft:concrete', walk)
    fill(CX - ARM_LEN, G, CZ - box, CX + ARM_LEN, G, CZ + box, 'minecraft:concrete', walk)
    fill(CX - HALF, G, CZ - ARM_LEN, CX + HALF, G, CZ + ARM_LEN, 'minecraft:concrete', asphalt)
    fill(CX - ARM_LEN, G, CZ - HALF, CX + ARM_LEN, G, CZ + HALF, 'minecraft:concrete', asphalt)
    for a, b in ((-ARM_LEN, -box - 3), (box + 3, ARM_LEN)):
        fill(CX, G, CZ + a, CX, G, CZ + b, 'minecraft:concrete', 4)
        fill(CX + a, G, CZ, CX + b, G, CZ, 'minecraft:concrete', 4)
    for k in range(HALF + 1, box):
        for j in range(-HALF, HALF + 1, 2):
            for sx in (-1, 1):
                put(CX + sx * k, G, CZ + j, 'minecraft:concrete', 0)
                put(CX + j, G, CZ + sx * k, 'minecraft:concrete', 0)
    flush()

    head = 'controllableverticalsolidsignal'
    fya = 'controllableverticalbimodalyellowleftsignal'
    ped, btn = 'controllablecrosswalksingle', 'controllablecrosswalkbuttonaudible'
    # lists as Elm's real controller has them: an FYA left head in fL and sL
    crc = {i: {'th': [], 'fL': [], 'sL': [], 'fR': [], 'sR': [], 'pS': [], 'pA': []}
           for i in range(9)}
    hn = '{mT:%d}' % REAR
    # corner poles (6 tall: the preempt detectors go up top)
    for sx in (-1, 1):
        for sz in (-1, 1):
            for y in range(Y, Y + 7):
                put(CX + sx * P, y, CZ + sz * P, 'trafficpolevertical', UPPOLE)
            # peds walking along z cross the main street's east (phase 4) or west (phase 3) leg;
            # peds walking along x cross the side street's north (phase 2) or south (phase 6) leg
            for (dx, dz, facing, ci) in ((0, -sz, N if sz > 0 else S, 4 if sx > 0 else 6),
                                         (-sx, 0, W if sx > 0 else E, 0 if sz < 0 else 2)):
                pos = (CX + sx * P + dx, Y + 1, CZ + sz * P + dz)
                put(*pos, block=ped, meta=facing * 4, nbt=hn)
                crc[ci]['pS'].append(pos)
                put(pos[0], Y, pos[2], btn, facing * 4)
                crc[ci]['pA'].append((pos[0], Y, pos[2]))
    # through heads on the near right corner; FYA lefts on a median pole before the stop line
    for corner, step, facing, ci, left_ci, median in (
            ((-1, 1), (-1, 0), W, 0, 1, (-1, 0)),       # eastbound: phase 2, left phase 5
            ((1, -1), (1, 0), E, 2, 3, (1, 0)),         # westbound: phase 6, left phase 1
            ((1, 1), (0, 1), S, 4, None, None),         # northbound: phase 4
            ((-1, -1), (0, -1), N, 6, None, None)):     # southbound: phase 3
        px, pz = CX + corner[0] * P + step[0], CZ + corner[1] * P + step[1]
        put(px, Y + 3, pz, head, facing * 4, hn)
        crc[ci]['th'].append((px, Y + 3, pz))
        # the overlaps, on the same pole below: side-street left arrows (circuits 5 and 7,
        # overlaps of phases 4 and 3) and the eastbound right-turn FYA (circuit 8)
        extra = {4: (5, 'controllableverticalleftsignal', ('sL',)),
                 6: (7, 'controllableverticalleftsignal', ('sL',)),
                 0: (8, 'controllableverticalbimodalyellowrightsignal', ('fR', 'sR'))}.get(ci)
        if extra:
            put(px, Y + 1, pz, extra[1], facing * 4, hn)
            for key in extra[2]:
                crc[extra[0]][key].append((px, Y + 1, pz))
        if left_ci is not None:
            mx = CX + median[0] * (box + 2)
            for y in range(Y, Y + 4):
                put(mx, y, CZ, 'trafficpolevertical', UPPOLE)
            lpos = (mx + median[0], Y + 3, CZ)
            put(*lpos, block=fya, meta=facing * 4, nbt=hn)
            crc[left_ci]['fL'].append(lpos)
            crc[left_ci]['sL'].append(lpos)
    flush()

    plan = json.load(open(ALTO, encoding='utf-8'))['after']['elm']
    crc_nbt = '{%s}' % ','.join(
        '"%d":{%s}' % (i, ','.join('%s:[L;%s]' % (k, ','.join('%dL' % L(*p) for p in v))
                                   for k, v in c.items()))
        for i, c in sorted(crc.items()))
    nbt = '{tcMode:9,tcOm:9,tcPhF:0,tcCrc:%s,tcAdv:%s}' % (crc_nbt, snbt(plan))
    kx, kz = CX + P + 5, CZ + P + 5
    put(kx, Y, kz, 'signalcontroller', 0, nbt)
    put(kx + 1, Y, kz, 'minecraft:redstone_block')
    sign(kx, Y, kz + 1, ['ADVANCED: Elm', 'Alto corridor', 'plan: coord,', 'FYA, ped recall'], 0)
    sign(kx + 2, Y, kz + 1, ['/csm signal', 'export elm', 'saves this plan', 'as SPATT JSON'], 0)
    flush()

    # pole-side preempt detectors facing each approach, a confirmation light above each
    tp(CX, Y + 12, CZ + 20, 180, 40)
    lights = ('preempt_confirm_par_white_pole', 'preempt_confirm_par_blue_pole',
              'preempt_confirm_dome_red_pole', 'preempt_confirm_par_white_pole')
    clicks = []
    for i, (sx, sz, face) in enumerate(((-1, 1, 'west'), (1, -1, 'east'), (1, 1, 'south'),
                                        (-1, -1, 'north'))):
        x, z = CX + sx * P, CZ + sz * P
        clicks += [click(x, Y + 5, z, 'preempt_detector_pole', face),
                   click(x, Y + 6, z, lights[i], face)]
    uses(clicks)

    # an accessible parking space with the R7-8 sign and the new R7-8P plaque
    bx, bz = CX + P + 10, CZ + P + 10
    fill(bx - 2, G, bz, bx + 4, G, bz + 6, 'minecraft:concrete', 15)
    fill(bx + 1, G, bz + 1, bx + 1, G, bz + 6, 'minecraft:concrete', 0)
    fill(bx + 3, G, bz + 1, bx + 3, G, bz + 6, 'minecraft:concrete', 0)
    for y in range(Y, Y + 3):
        put(bx, y, bz, 'trafficpolevertical', UPPOLE)
    flush()
    tp(bx, Y + 1, bz + 6, 180, 10)
    uses([click(bx, Y + 2, bz, 'signhandicapreservedparkingaccessibleicon', 'south'),
          click(bx, Y + 1, bz, 'signvanaccessibleplaque', 'south')])
    print('signals done')


# ------------------------------------------------------------------------------------------
# The board
# ------------------------------------------------------------------------------------------
BOARD = [
    ['Lights, alarms,', 'signals & more', 'demo (x, z)'],
    ['HVAC', 'condensers, RTUs', 'large RTU, PTAC', '0..50, 0..34'],
    ['Fire alarms', 'zones, home', 'interconnect', '0..50, 44..80'],
    ['Sliding doors', 'all styles and', 'finishes', '60..124, 0..40'],
    ['Cell door lock', 'trash compactor', 'bear lockers', '60..124, 44..64'],
    ['Luminaire arms', '36 arms, 22 new', 'street lights', '0..170, 84..112'],
    ['Historic poles', 'LA, Anaheim,', '17 post tops', '0..140, 116..150'],
    ['Floodlights', '15 on every', 'mounting', '0..80, 154..180'],
    ['ADVANCED signal', 'Elm plan, FYA,', 'preempt, R7-8P', '180..270, 0..70'],
]


def build_board():
    fill(-14, Y, -2, -10, Y + 4, 20, 'minecraft:air')
    fill(-12, Y, 0, -12, Y + 2, 18, 'minecraft:stonebrick')
    for i, lines in enumerate(BOARD):
        wall_sign(-11, Y + 1, 1 + i * 2, lines, 5)
    put(-12, Y + 3, 9, 'minecraft:sea_lantern')
    flush()


# ------------------------------------------------------------------------------------------
cmds(['difficulty peaceful', 'gamerule doMobSpawning false', 'gamerule doWeatherCycle false',
      'gamerule doDaylightCycle false', 'weather clear 1000000', 'time set 6000'])
for name, fn in (('hvac', build_hvac), ('alarms', build_alarms), ('doors', build_doors),
                 ('building', build_building), ('arms', build_arms),
                 ('historic', build_historic), ('floods', build_floods),
                 ('signals', build_signals), ('board', build_board)):
    if name in ONLY:
        fn()
if ONLY == set(AREAS):
    cmds(['setworldspawn -8 %d 9' % Y])
cmds(['time set %d' % (14000 if ARGS.night else 6000)])
tp(-8, Y, 9, -90, 0)
O.close()
print('done')
