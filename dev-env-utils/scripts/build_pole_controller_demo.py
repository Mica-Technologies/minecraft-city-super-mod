#!/usr/bin/env python3
"""
build_pole_controller_demo.py -- builds the Pole Controller demo world, over the MCMCP
orchestrator: the traffic signal controller in its pole or wall cabinet (#360,
`signalcontroller_pole_<colour>`).

Create a fresh FLAT creative world named "Pole Controller Demo" in the dev client (Roads is all it
needs; --create does it for you), and run this with the client's instance id. It sets creative,
peaceful, a fixed midday and clear weather, and lays out, west to east along the x axis (x and z
relative to the site origin, which is the player's position unless --x/--z say otherwise):

  corner     (x 0)    a tight NYC-style corner: two two-lane streets, sidewalks three blocks deep
                      between the curb and the buildings, a mast arm pole on each corner. The
                      galvanized cabinet is clamped to the north-east mast pole a block above the
                      walk, door to the sidewalk, and runs the junction (NORMAL mode, two circuits)
  pedestal   (x 50)   the same junction on black pedestal poles with the heads on their sides; the
                      black cabinet is clamped to the south-west pedestal pole
  wall       (x 100)  a one-way street past a shop front: the green cabinet sits flat on the
                      building face beside the door and runs the two heads on thin poles
  colours    (x 140)  all four colours side by side: on large poles, on thin poles, on pedestal
                      poles and flat on a wall, with a sign naming each

Every cabinet stands a block above the walk (its bottom about 1 m up, its top at head height).
A cabinet needs redstone power to run, as the ground controller does. A pole cabinet also takes
it from the foot of its pole: a redstone block under the walk, below each running pole's base,
feeds it as a utility service comes up the pole. The wall one is powered by a lever on the inside
face of the wall it is bolted to. To see the feed matter, break the redstone block under the
north-east mast pole of the corner (or swap it for a lever, and switch it): the junction goes dark
within a second.

Everything goes through the orchestrator by instance id; the script never opens a game port, and
it refuses any world not named "Pole Controller Demo".

Usage:
    py -3 build_pole_controller_demo.py --instance run-f2eb58 [--create] [--x X --z Z]
                                        [--only corner,pedestal,wall,colours]
"""
import argparse
import json
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmcp_orchestrator import Orchestrator  # noqa: E402

PARTS = ('corner', 'pedestal', 'wall', 'colours')
ap = argparse.ArgumentParser(description='Build the Pole Controller demo world.')
ap.add_argument('--instance', required=True, help="the dev game's id, e.g. run-f2eb58")
ap.add_argument('--create', action='store_true', help='create and load the flat world first')
ap.add_argument('--x', type=int, default=None, help='site origin x (default: player)')
ap.add_argument('--z', type=int, default=None, help='site origin z (default: player)')
ap.add_argument('--only', default=None, help='comma-separated parts: ' + ','.join(PARTS))
ARGS = ap.parse_args()
ONLY = set(PARTS) if not ARGS.only else {p.strip() for p in ARGS.only.split(',')}
for _p in ONLY:
    if _p not in PARTS:
        sys.exit('unknown part %r (parts: %s)' % (_p, ', '.join(PARTS)))

WORLD_NAME = 'Pole Controller Demo'
O = Orchestrator()
CLIENT, SERVER = ARGS.instance + '.client', ARGS.instance + '.server'
if ARGS.create:
    O.call('client_world_create', instance=CLIENT, name=WORLD_NAME, worldType='flat',
           generateStructures=False, seed='360')
    time.sleep(15)
_info = O.call('server_world_info', instance=SERVER)
if WORLD_NAME not in json.dumps(_info):
    sys.exit('refusing: the world on %s is not "%s"' % (SERVER, WORLD_NAME))
_players = O.call('server_list_players', instance=SERVER)['players']
PLAYER = _players[0] if isinstance(_players[0], str) else _players[0]['name']
_me = O.call('server_player_state', instance=SERVER, player=PLAYER)
X0 = ARGS.x if ARGS.x is not None else int(_me['position']['x'])
Z0 = ARGS.z if ARGS.z is not None else int(_me['position']['z'])

G, Y = 3, 4                    # the flat world's ground block, and where things stand
CAB = Y + 1                    # a cabinet's cell: its bottom a block above the walk
ARM = Y + 6                    # mast arm height; heads hang one below it
S, W, N, E = 0, 1, 2, 3        # horizontal facing index: a cabinet's metadata, a head's x4
UP, F_NORTH, F_SOUTH, F_WEST, F_EAST = 1, 2, 3, 4, 5    # EnumFacing index (poles)
REAR = 1                       # signal head mount: the bracket reaches the pole behind
HEAD = 'controllableverticalsolidsignal'
ASPHALT, WALK, WHITE, YELLOW = ('minecraft:concrete', 7), ('minecraft:concrete', 8), 0, 4
BUILDING = ('minecraft:stonebrick', 0)

blocks = []


def put(x, y, z, block, meta=0, nbt=None):
    """A block at site coordinates (x, z relative to the origin, y absolute)."""
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
    name = block if ':' in block else 'csm:' + block
    xa, xb = min(x0, x1), max(x0, x1)
    for x in range(xa, xb + 1, 24):             # in slabs of x, under the batch limit
        O.call('server_set_blocks', instance=SERVER, mode='fill', load=True, block=name,
               metadata=meta, x=X0 + x, y=y0, z=Z0 + min(z0, z1), toX=X0 + min(xb, x + 23),
               toY=y1, toZ=Z0 + max(z0, z1))


def cmds(*cs):
    return O.call('server_run_commands', instance=SERVER, commands=list(cs), load=False,
                  allResults=True)


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


def controller_nbt(circuits):
    """NORMAL mode, two circuits taking turns, short timings so the cycle shows in a minute."""
    crc = '{%s}' % ','.join('"%d":{th:[L;%s],pS:[L;],pA:[L;]}' % (i, longs(c))
                            for i, c in enumerate(circuits))
    return ('{tcMode:1,tcOm:1,tcPhF:0,tcOvp:1b,tcYt:60L,tcArt:40L,tcFdw:100L,tcMnG:200L,'
            'tcMxG:300L,tcMnGs:200L,tcMxGs:300L,tcCrc:%s}' % crc)


# The step from a cabinet to the pole behind it, by the way the cabinet faces.
BEHIND = {N: (0, 1), S: (0, -1), E: (-1, 0), W: (1, 0)}
# Where earlier builds of this world put a cabinet's power (a torch under it, a lever on its
# right side), cleared when a part is rebuilt in place.
OLD_LEVER = {N: (1, 0), E: (0, 1), S: (-1, 0), W: (0, -1)}


def pole_cabinet(x, z, facing, colour, nbt=None, power=True):
    """A cabinet in the cell in front of a pole, a block above the walk, facing away from it.
    With power, a redstone block under the walk below the pole's foot runs it: the feed comes up
    the pole, as a real cabinet's does."""
    put(x, CAB, z, 'signalcontroller_pole_' + colour, facing, nbt)
    put(x, Y, z, 'minecraft:air')
    put(x + OLD_LEVER[facing][0], CAB, z + OLD_LEVER[facing][1], 'minecraft:air')
    if power:
        bx, bz = BEHIND[facing]
        put(x + bx, G - 1, z + bz, 'minecraft:redstone_block')


# ------------------------------------------------------------------------------------------
# A junction: two two-lane streets (along x and z, lanes -HALF..HALF), sidewalks SIDE deep,
# buildings behind them on the four corners.

HALF, SIDE, ROAD = 3, 3, 20
P = HALF + 1                   # a corner pole's offset from the centre: at the curb


def streets(cx, buildings=True):
    edge = HALF + SIDE
    fill(cx - ROAD, G, -ROAD, cx + ROAD, G, ROAD, *WALK)
    fill(cx - HALF, G, -ROAD, cx + HALF, G, ROAD, *ASPHALT)
    fill(cx - ROAD, G, -HALF, cx + ROAD, G, HALF, *ASPHALT)
    for a, b in ((-ROAD, -edge - 2), (edge + 2, ROAD)):
        fill(cx, G, a, cx, G, b, 'minecraft:concrete', YELLOW)
        fill(cx + a, G, 0, cx + b, G, 0, 'minecraft:concrete', YELLOW)
    for k in range(HALF + 1, edge + 1):                     # crosswalks
        for j in range(-HALF, HALF + 1, 2):
            for sx in (-1, 1):
                put(cx + sx * k, G, j, 'minecraft:concrete', WHITE)
                put(cx + j, G, sx * k, 'minecraft:concrete', WHITE)
    if buildings:
        b0 = edge + 1
        for sx in (-1, 1):
            for sz in (-1, 1):
                xs = sorted((cx + sx * b0, cx + sx * ROAD))
                zs = sorted((sz * b0, sz * ROAD))
                fill(xs[0], Y, zs[0], xs[1], Y + 7, zs[1], *BUILDING)
                # a ground-floor shop window band facing each street
                wx = cx + sx * b0
                fill(wx, Y + 1, zs[0] + 1, wx, Y + 2, zs[1] - 1, 'minecraft:glass_pane')
                wz = sz * b0
                fill(xs[0] + 1, Y + 1, wz, xs[1] - 1, Y + 2, wz, 'minecraft:glass_pane')
    flush()


def mast(px, pz, cells, arm_facing, pole='trafficpolevertical'):
    for y in range(Y, ARM + 1):
        put(px, y, pz, pole, UP)
    for x, z in cells:
        put(x, ARM, z, 'trafficpolehorizontal', arm_facing)


def corner():
    """The NYC corner: four mast arms, far-side heads, the galvanized cabinet on the NE pole."""
    cx = 0
    streets(cx)
    ns, ew = [], []                 # circuit 0: traffic along z; circuit 1: traffic along x
    # NE pole, arm west over the street along z: heads face south (northbound traffic)
    mast(cx + P, -P, [(cx + x, -P) for x in range(P - 1, -HALF - 1, -1)], F_WEST)
    for x in (cx + 2, cx - 2):
        put(x, ARM - 1, -P, HEAD, S * 4)
        ns.append((x, ARM - 1, -P))
    # SW pole, arm east: heads face north (southbound)
    mast(cx - P, P, [(cx + x, P) for x in range(-P + 1, HALF + 1)], F_EAST)
    for x in (cx - 2, cx + 2):
        put(x, ARM - 1, P, HEAD, N * 4)
        ns.append((x, ARM - 1, P))
    # NW pole, arm south over the street along x: heads face east (westbound)
    mast(cx - P, -P, [(cx - P, z) for z in range(-P + 1, HALF + 1)], F_SOUTH)
    for z in (-2, 2):
        put(cx - P, ARM - 1, z, HEAD, E * 4)
        ew.append((cx - P, ARM - 1, z))
    # SE pole, arm north: heads face west (eastbound)
    mast(cx + P, P, [(cx + P, z) for z in range(P - 1, -HALF - 1, -1)], F_NORTH)
    for z in (2, -2):
        put(cx + P, ARM - 1, z, HEAD, W * 4)
        ew.append((cx + P, ARM - 1, z))
    flush()
    # the cabinet on the NE pole's north face, door to the sidewalk
    pole_cabinet(cx + P, -P - 1, N, 'galvanized', controller_nbt([ns, ew]))
    sign(cx + P + 2, -P - 2, ['Pole cabinet', 'galvanized', 'on a mast pole', 'NORMAL mode'], 8)
    sign(cx - P - 2, P + 2, ['Pole Controller', 'Demo', '#360', 'walk east'], 0)
    flush()


def pedestal():
    """The same junction on black pedestal poles, heads on their sides, the black cabinet on the
    south-west pole."""
    cx = 50
    streets(cx)
    ns, ew = [], []
    pole = 'trafficpolepedestalblack'
    # each corner: a pedestal pole with a head on the face that looks across the junction at
    # the traffic it serves (far-side, as on the mast arms)
    corners = {(1, -1): ((0, 1), S, ns), (-1, 1): ((0, -1), N, ns),
               (-1, -1): ((1, 0), E, ew), (1, 1): ((-1, 0), W, ew)}
    for (sx, sz), ((dx, dz), facing, circuit) in corners.items():
        px, pz = cx + sx * P, sz * P
        for y in range(Y, Y + 4):
            put(px, y, pz, pole, UP)
        hx, hz = px + dx, pz + dz
        put(hx, Y + 2, hz, HEAD, facing * 4, '{mT:%d}' % REAR)
        circuit.append((hx, Y + 2, hz))
    flush()
    # the cabinet on the SW pole's west face, door to the sidewalk along the street
    pole_cabinet(cx - P - 1, P, W, 'black', controller_nbt([ns, ew]))
    sign(cx - P - 2, P + 2, ['Pole cabinet', 'black', 'on a pedestal', 'pole'], 0)
    flush()


def wall():
    """A one-way street past a shop: the green cabinet flat on the building face."""
    cx, bz = 100, -5                      # the building's street face is at z = bz
    fill(cx - 20, G, -4, cx + 20, G, 6, *WALK)
    fill(cx - 20, G, -1, cx + 20, G, 3, *ASPHALT)
    fill(cx - 3, G, -1, cx - 3, G, 3, 'minecraft:concrete', WHITE)        # the stop line
    # the building: hollow, with a lit interior, a shop window and a door
    fill(cx - 12, Y, bz - 8, cx + 12, Y + 6, bz, *BUILDING)
    fill(cx - 11, Y, bz - 7, cx + 11, Y + 5, bz - 1, 'minecraft:air')
    fill(cx - 11, Y - 1, bz - 7, cx + 11, Y - 1, bz - 1, 'minecraft:planks', 5)
    for x in range(cx - 10, cx + 11, 4):
        put(x, Y + 5, bz - 4, 'minecraft:glowstone')
    fill(cx + 3, Y + 1, bz, cx + 9, Y + 2, bz, 'minecraft:glass_pane')
    put(cx + 1, Y, bz, 'minecraft:air')
    put(cx + 1, Y + 1, bz, 'minecraft:air')
    # two thin poles past the stop line, one each side, a head on each facing the traffic that
    # comes from the east (the street is one-way westbound)
    heads = []
    for z in (-2, 4):
        for y in range(Y, Y + 4):
            put(cx - 5, y, z, 'trafficpolehorizontal', UP)
        put(cx - 4, Y + 3, z, HEAD, E * 4, '{mT:%d}' % REAR)
        heads.append((cx - 4, Y + 3, z))
    # and a pair further on, one each side facing across the street (a driveway's signal, say),
    # so the plan has a second circuit to give way to; each pole is behind its head
    cross = []
    for z, facing in ((-3, S), (5, N)):
        for y in range(Y, Y + 3):
            put(cx - 10, y, z + (-1 if facing == S else 1), 'trafficpolehorizontal', UP)
        put(cx - 10, Y + 2, z, HEAD, facing * 4, '{mT:%d}' % REAR)
        cross.append((cx - 10, Y + 2, z))
    flush()
    # the cabinet on the wall beside the door, powered by a lever inside on the same wall block
    put(cx - 1, CAB, bz + 1, 'signalcontroller_pole_green', S, controller_nbt([heads, cross]))
    put(cx - 1, CAB, bz - 1, 'minecraft:lever', 4 | 8)     # on the wall's inside face, on
    sign(cx - 3, bz + 2, ['Wall cabinet', 'green', 'beside the door', 'lever inside'], 0)
    flush()


def colours():
    """The four colours side by side, on each mount."""
    cx = 140
    fill(cx - 4, G, -8, cx + 22, G, 12, *WALK)
    fill(cx - 4, Y, 10, cx + 22, Y + 4, 11, *BUILDING)                # a wall to the south
    order = ('galvanized', 'black', 'green', 'brown')
    rows = (('trafficpolevertical', -6, 'large pole'),
            ('trafficpolehorizontal', -1, 'thin pole'),
            ('trafficpolepedestalsilver', 4, 'pedestal pole'))
    for pole, z, label in rows:
        for i, colour in enumerate(order):
            x = cx + 5 * i
            for y in range(Y, Y + 4):
                put(x, y, z, pole, UP)
            pole_cabinet(x, z - 1, N, colour, power=False)
            sign(x + 2, z, [colour.capitalize(), label], 8)
    for i, colour in enumerate(order):
        x = cx + 5 * i
        put(x, CAB, 9, 'signalcontroller_pole_' + colour, N)
        sign(x + 2, 8, [colour.capitalize(), 'on a wall'], 8)
    sign(cx - 2, -9, ['Pole/wall cabinet', 'four colours', 'three pole widths', 'and a wall'], 8)
    flush()


# ------------------------------------------------------------------------------------------
cmds('gamemode 1 %s' % PLAYER, 'difficulty peaceful', 'gamerule doMobSpawning false',
     'gamerule doDaylightCycle false', 'gamerule doWeatherCycle false', 'weather clear 1000000',
     'time set 6000')
BUILD = {'corner': corner, 'pedestal': pedestal, 'wall': wall, 'colours': colours}
for name in PARTS:
    if name in ONLY:
        print('building', name)
        BUILD[name]()
cmds('tp %s %d %d %d' % (PLAYER, X0 + P + 2, Y, Z0 - P - 3))
O.close()
print('done: site origin at', X0, Z0)
