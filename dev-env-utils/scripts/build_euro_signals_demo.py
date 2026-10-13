#!/usr/bin/env python3
"""
build_euro_signals_demo.py -- builds the European signals demo world, over the MCMCP orchestrator.

Load a fresh FLAT creative world named "Euro Signals Demo" in the dev client (Roads is all it
needs), stand where the first junction should go, and run this with the client's instance id. It
lays out, west to east along the x axis:

  gallery    every European preset head on a pole with a sign: the 300, 200 and 300/200/200 mm
             heads, the left, right and straight-ahead arrows, the bicycle head, the 100 mm
             repeater, the three pedestrian heads, a 300 mm head painted anthracite, a US head
             fitted with European lenses only, an anthracite RRFB and the two European push
             buttons (pedestrian and bicycle). All of it runs off one
             NORMAL controller with red+amber and the green flash both on, so the whole sequence
             cycles in front of you: red, red+amber, green, flashing green, amber
  germany    a crossroads in the German style: NORMAL controller, red+amber for 1 s before every
             green, heads at the stop line on the near right corner with a 100 mm repeater below,
             pedestrian heads whose clearance is a steady red figure, a European push button
             under each (press one: its request lamp lights until the walk comes)
  mexico     the same crossroads with US heads, run by an ADVANCED (ring-barrier) controller with a
             3 s green flash at the end of every green, and pedestrian heads whose clearance is a
             flashing green figure
  austria    European heads on an ADVANCED controller with both: red+amber before green and a 4 s
             green flash before amber, flashing green pedestrian clearance

Everything goes through the orchestrator by instance id; the script never opens a game port, and
it refuses any world not named "Euro Signals Demo".

Usage:
    python build_euro_signals_demo.py --instance run-f2eb58 [--x X --z Z]
                                      [--only gallery|germany|mexico|austria]
"""
import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmcp_orchestrator import Orchestrator  # noqa: E402

ap = argparse.ArgumentParser(description='Build the European signals demo world.')
ap.add_argument('--instance', required=True, help="the dev game's id, e.g. run-f2eb58")
ap.add_argument('--x', type=int, default=None, help='first junction centre x (default: player)')
ap.add_argument('--z', type=int, default=None, help='first junction centre z (default: player)')
ap.add_argument('--only', choices=('gallery', 'germany', 'mexico', 'austria'), default=None)
ARGS = ap.parse_args()
WORLD_NAME = 'Euro Signals Demo'

O = Orchestrator()
CLIENT, SERVER = ARGS.instance + '.client', ARGS.instance + '.server'
_info = O.call('server_world_info', instance=SERVER)
if WORLD_NAME not in json.dumps(_info):
    sys.exit('refusing: the world on %s is not "%s"' % (SERVER, WORLD_NAME))
_me = O.call('client_player_state', instance=CLIENT)
X0 = ARGS.x if ARGS.x is not None else int(_me['position']['x'])
Z0 = ARGS.z if ARGS.z is not None else int(_me['position']['z'])

G, Y = 3, 4                    # the flat world's ground, and where things stand
S, W, N, E = 0, 1, 2, 3        # horizontal facing index; a head's metadata is facing * 4
UP = 1                         # EnumFacing UP, for a vertical pole
REAR = 1                       # SignalHeadMountType.REAR: the bracket reaches the pole behind

blocks = []


def put(x, y, z, block, meta=0, nbt=None):
    """A block at site coordinates (x, z relative to the first junction, y absolute)."""
    b = {'x': X0 + x, 'y': y, 'z': Z0 + z, 'block': block if ':' in block else 'csm:' + block,
         'metadata': meta}
    if nbt:
        b['nbt'] = nbt
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
           x=X0 + min(x0, x1), y=y0, z=Z0 + min(z0, z1),
           toX=X0 + max(x0, x1), toY=y1, toZ=Z0 + max(z0, z1))


def sign(x, z, lines, rot=0, y=Y):
    """A standing sign; rot 0 faces south, 4 west, 8 north, 12 east."""
    parts = ['Text%d:%s' % (i + 1, json.dumps(json.dumps({'text': t})))
             for i, t in enumerate(lines[:4])]
    put(x, y, z, 'minecraft:standing_sign', rot, '{' + ','.join(parts) + '}')


def L(x, y, z):
    """A site position as BlockPos.toLong writes it."""
    x, z = X0 + x, Z0 + z
    v = ((x & 0x3FFFFFF) << 38) | ((y & 0xFFF) << 26) | (z & 0x3FFFFFF)
    return v - (1 << 64) if v >= 1 << 63 else v


def longs(cells):
    return ','.join('%dL' % L(*c) for c in cells)


def sections(*rows):
    """A head's section NBT: each row [body, door, visor, visorType, bulbStyle, bulbType,
    bulbColor, customColor, lit, flashing, bodyStyle, bimodal], top section first."""
    s = ','.join('s_%d:[I;%s]' % (i, ','.join(map(str, r))) for i, r in enumerate(rows))
    return 'sInfs:{%s,count:%d}' % (s, len(rows))


def head_nbt(extra=None):
    return '{mT:%d%s}' % (REAR, ',' + extra if extra else '')


# ------------------------------------------------------------------------------------------
# A crossroads: two two-lane roads (x and z, lanes -HALF..HALF) with sidewalks, a pole on each
# corner. Right-hand traffic: each approach's head stands on its near right corner facing the
# drivers, the pedestrian heads on each corner face across the two crosswalks.

HALF, WALK = 3, 3
P = HALF + 2                   # the corner poles' offset from the centre
ARM_LEN = 40                   # road length either side

# approach -> (corner signs, outward step to the head, the head's facing, circuit)
APPROACHES = {
    'from_south': ((1, 1), (0, 1), S, 0),
    'from_north': ((-1, -1), (0, -1), N, 0),
    'from_east': ((1, -1), (1, 0), E, 1),
    'from_west': ((-1, 1), (-1, 0), W, 1),
}


def roads(cx):
    asphalt, white, yellow, walk = ('minecraft:concrete', 7), 0, 4, ('minecraft:concrete', 8)
    box = HALF + WALK
    fill(cx - box, G, -ARM_LEN, cx + box, G, ARM_LEN, *walk)
    fill(cx - ARM_LEN, G, -box, cx + ARM_LEN, G, box, *walk)
    fill(cx - HALF, G, -ARM_LEN, cx + HALF, G, ARM_LEN, *asphalt)
    fill(cx - ARM_LEN, G, -HALF, cx + ARM_LEN, G, HALF, *asphalt)
    for a, b in ((-ARM_LEN, -box - 3), (box + 3, ARM_LEN)):
        fill(cx, G, a, cx, G, b, 'minecraft:concrete', yellow)
        fill(cx + a, G, 0, cx + b, G, 0, 'minecraft:concrete', yellow)
    for k in range(HALF + 1, box):                          # zebra crossings
        for j in range(-HALF, HALF + 1, 2):
            for sx in (-1, 1):
                put(cx + sx * k, G, j, 'minecraft:concrete', white)
                put(cx + j, G, sx * k, 'minecraft:concrete', white)
    stop = box + 1                                          # stop lines
    fill(cx + 1, G, stop, cx + HALF, G, stop, 'minecraft:concrete', white)
    fill(cx - HALF, G, -stop, cx - 1, G, -stop, 'minecraft:concrete', white)
    fill(cx + stop, G, -HALF, cx + stop, G, -1, 'minecraft:concrete', white)
    fill(cx - stop, G, 1, cx - stop, G, HALF, 'minecraft:concrete', white)
    flush()


def crossroads(cx, head, repeater, ped, ped_in_own_circuit, button=None):
    """Poles, heads and peds for a crossroads centred at (cx, 0). Returns the circuits as
    {index: {'th': [...], 'pS': [...]}}. `ped_in_own_circuit` is the ADVANCED convention (a
    circuit's pedestrians walk with its own phase); NORMAL lists them under the circuit whose
    street they cross, so they walk while the other one is green."""
    circuits = {0: {'th': [], 'pS': [], 'pA': []}, 1: {'th': [], 'pS': [], 'pA': []}}
    for sx in (-1, 1):
        for sz in (-1, 1):
            for y in range(Y, Y + 5):
                put(cx + sx * P, y, sz * P, 'trafficpolevertical', UP)
            # pedestrian heads facing across the crosswalk along z (walking with N-S traffic)
            # and along x (walking with E-W traffic)
            for (dx, dz, facing, parallel) in ((0, -sz, N if sz > 0 else S, 0),
                                               (-sx, 0, W if sx > 0 else E, 1)):
                pos = (cx + sx * P + dx, Y + 1, sz * P + dz)
                put(*pos, ped, facing * 4, head_nbt())
                circuit = parallel if ped_in_own_circuit else 1 - parallel
                circuits[circuit]['pS'].append(pos)
                if button:
                    # the push button under the head, on the same face of the pole
                    under = (pos[0], Y, pos[2])
                    put(*under, button, facing * 4)
                    circuits[circuit]['pA'].append(under)
    for corner, step, facing, circuit in APPROACHES.values():
        px, pz = cx + corner[0] * P + step[0], corner[1] * P + step[1]
        put(px, Y + 3, pz, head, facing * 4, head_nbt())
        circuits[circuit]['th'].append((px, Y + 3, pz))
        if repeater:
            put(px, Y + 1, pz, repeater, facing * 4, head_nbt())
            circuits[circuit]['th'].append((px, Y + 1, pz))
    return circuits


def crc_nbt(circuits):
    return '{%s}' % ','.join('"%d":{th:[L;%s],pS:[L;%s],pA:[L;%s]}'
                             % (i, longs(c['th']), longs(c['pS']), longs(c.get('pA', [])))
                             for i, c in sorted(circuits.items()))


def normal_controller(x, z, circuits, extra):
    nbt = ('{tcMode:1,tcOm:1,tcPhF:0,tcOvp:1b,tcYt:60L,tcArt:40L,tcFdw:100L,tcMnG:200L,'
           'tcMxG:300L,tcMnGs:200L,tcMxGs:300L,tcCrc:%s%s}' % (crc_nbt(circuits), extra))
    put(x, Y, z, 'signalcontroller', 0, nbt)
    put(x + 1, Y, z, 'minecraft:redstone_block')


def advanced_controller(x, z, circuits, extra):
    # Phase 2 serves circuit 0 (north-south), phase 4 circuit 1 (east-west), both on max recall
    # with pedestrian recall, so the junction cycles with nobody there.
    ph = []
    for n in range(1, 9):
        ci = {2: 0, 4: 1}.get(n, -1)
        ph.append('{n:%d,rg:%d,ba:%d,ci:%d,mv:0,en:%s,rm:%d,pr:%s,mg:160L,xg:300L,pa:40L,'
                  'ye:60L,rc:40L,wk:100L,pc:100L}'
                  % (n, 1 if n <= 4 else 2, 1 if n in (1, 2, 5, 6) else 2, ci,
                     '1b' if ci >= 0 else '0b', 2 if ci >= 0 else 0, '1b' if ci >= 0 else '0b'))
    adv = '{ph:[%s],r1:[I;1,2,3,4],r2:[I;5,6,7,8],co:{md:0}}' % ','.join(ph)
    nbt = '{tcMode:9,tcOm:9,tcPhF:0,tcCrc:%s,tcAdv:%s%s}' % (crc_nbt(circuits), adv, extra)
    put(x, Y, z, 'signalcontroller', 0, nbt)
    put(x + 1, Y, z, 'minecraft:redstone_block')


def junction(cx, title, head, repeater, ped, advanced, extra, lines, button=None):
    roads(cx)
    circuits = crossroads(cx, head, repeater, ped, advanced, button)
    kx, kz = cx + P + 4, P + 4
    (advanced_controller if advanced else normal_controller)(kx, kz, circuits, extra)
    sign(kx, kz + 1, [title] + lines, 0)
    flush()


# ------------------------------------------------------------------------------------------
# The gallery: a row of poles north of the German junction, each with one head and a sign.

EURO_ANTHRACITE = sections(*[[16, 16, 16, 10, 5, 5, c, c, 0, 0, 3, 0] for c in (2, 1, 0)])
US_EURO_LENS = sections(*[[0, 0, 0, 0, 5, 5, c, c, 0, 0, 0, 0] for c in (2, 1, 0)])

GALLERY = [
    ('controllableeurosignal', 'v', None, ['300 mm']),
    ('controllableeurosignal200', 'v', None, ['200 mm']),
    ('controllableeurosignal300200', 'v', None, ['300/200/200', 'mm']),
    ('controllableeurosignalleft', 'v', None, ['Left arrow', 'masked lens']),
    ('controllableeurosignalright', 'v', None, ['Right arrow']),
    ('controllableeurosignalahead', 'v', None, ['Straight', 'ahead arrow']),
    ('controllableeurosignalbike', 'v', None, ['Bicycle', '200 mm']),
    ('controllableeurosignalrepeater', 'v', None, ['100 mm', 'repeater']),
    ('controllableeurosignal', 'v', EURO_ANTHRACITE, ['300 mm in', 'anthracite', 'RAL 7016']),
    ('controllableverticalsolidsignal', 'v', US_EURO_LENS, ['US head with', 'European', 'lenses']),
    ('controllableeuropedsignal', 'p', None, ['Pedestrian', 'steady red', 'clearance']),
    ('controllableeuropedsignalflashinggreen', 'p', None, ['Pedestrian', 'flashing green', 'clearance']),
    ('controllableeuropedsignal4inch', 'p', None, ['Pedestrian', '100 mm', 'repeater']),
    ('controllablerrfb', 'r', None, ['RRFB in', 'anthracite']),
    ('controllablecrosswalkbuttoneuro', 'b', None, ['Push button', 'pedestrian']),
    ('controllablecrosswalkbuttoneurobike', 'b', None, ['Push button', 'bicycle']),
]


def gallery():
    gz = -(ARM_LEN // 2) - 2          # north of the German junction, beside its north road
    x0 = HALF + WALK + 3
    fill(x0 - 2, G, gz - 2, x0 + 3 * len(GALLERY) + 4, G, gz + 4, 'minecraft:concrete', 8)
    vehicles, peds, buttons = [], [], []
    for i, (block, kind, nbt, lines) in enumerate(GALLERY):
        x = x0 + 3 * i
        for y in range(Y, Y + 3):
            put(x, y, gz, 'trafficpolevertical', UP)
        if kind == 'r':
            put(x, Y + 2, gz + 1, block, S * 4, '{hsC:16}')
        elif kind == 'b':
            put(x, Y, gz + 1, block, S * 4)       # on the pole at hand height
            buttons.append((x, Y, gz + 1))
        elif kind == 'p':
            put(x, Y + 1, gz + 1, block, S * 4, head_nbt(nbt))
            peds.append((x, Y + 1, gz + 1))
        else:
            put(x, Y + 2, gz + 1, block, S * 4, head_nbt(nbt))
            vehicles.append((x, Y + 2, gz + 1))
        sign(x, gz + 3, lines, 0)
    # One controller runs the whole row as circuit 0; circuit 1 is a single US head beside it, so
    # the row turns red when that one turns green. The pedestrian heads walk while the row is red.
    kx = x0 + 3 * len(GALLERY) + 2
    other = (kx, Y + 2, gz + 1)
    for y in range(Y, Y + 3):
        put(kx, y, gz, 'trafficpolevertical', UP)
    put(*other, 'controllableverticalsolidsignal', S * 4, head_nbt())
    circuits = {0: {'th': vehicles, 'pS': peds, 'pA': buttons}, 1: {'th': [other], 'pS': []}}
    normal_controller(kx + 2, gz, circuits, ',tcRaT:20L,tcGfT:60L')
    sign(kx + 2, gz + 1, ['Gallery', 'red+amber 1 s', 'green flash 3 s', 'NORMAL mode'], 0)
    flush()


SPACING = 2 * ARM_LEN + 10
PARTS = {
    'gallery': gallery,
    'germany': lambda: junction(0, 'Germany', 'controllableeurosignal',
                                'controllableeurosignalrepeater', 'controllableeuropedsignal',
                                False, ',tcRaT:20L', ['NORMAL mode', 'red+amber 1 s',
                                                      'steady red peds'],
                                'controllablecrosswalkbuttoneuro'),
    'mexico': lambda: junction(SPACING, 'Mexico', 'controllableverticalsolidsignal', None,
                               'controllableeuropedsignalflashinggreen', True, ',tcGfT:60L',
                               ['ADVANCED mode', 'green flash 3 s', 'flashing peds']),
    'austria': lambda: junction(2 * SPACING, 'Austria', 'controllableeurosignal',
                                'controllableeurosignalrepeater',
                                'controllableeuropedsignalflashinggreen', True,
                                ',tcRaT:20L,tcGfT:80L', ['ADVANCED mode', 'R+A 1s, flash 4s',
                                                         'flashing peds'],
                                'controllablecrosswalkbuttoneuro'),
}

for name, build in PARTS.items():
    if ARGS.only in (None, name):
        print('building', name)
        build()
O.close()
print('done: first junction at', X0, Z0)
