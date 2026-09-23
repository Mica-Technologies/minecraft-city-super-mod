#!/usr/bin/env python3
"""Builds the HVAC test lab in a flat world loaded in the dev client, over MCMCP.

Every scenario the thermal simulation has to get right, side by side along +X, each with a sign:
a plain room on a cabinet heater, a closet on a rooftop unit, a tall atrium, a long hallway, an
L-shaped room, a two-storey building with an open stair, a strip of three flats sharing walls
(the middle one unheated), an office of three zones off a hallway, a replica of a big-box store
with four zones and a rooftop-unit primary that has no vents of its own, rooms with an open door
and a window hole, a room held by an unlinked space heater, and a thermostat under the open sky.

Run it in "HVAC Lab Cold" (a Cold Taiga superflat, about -49F at the ground) for heating, and with
--cooling in "HVAC Lab Desert" (about 176F) for the cooling set. Equipment is linked through its
saved data, so nothing needs clicking with the linker, and powered by redstone blocks.

    python build_hvac_lab.py [--x X] [--z Z] [--cooling] [--only NAME]

Scenario origins are printed; read a room with `/csmhvac info` standing in it, and step time with
`/csmhvac ff <seconds>`.
"""

import argparse
import json
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_bench as B  # noqa: E402 -- its MCMCP client and token reader


class _Session:
    def __init__(self, url):
        self.mcp = B.Mcp(url, B.read_token()).connect()

    def call(self, name, **args):
        out = self.mcp.call(name, args)
        return out if isinstance(out, str) else json.dumps(out)


ap = argparse.ArgumentParser(description="Build the HVAC test lab.")
ap.add_argument("--x", type=int, default=0, help="west edge of the first scenario")
ap.add_argument("--z", type=int, default=0, help="north edge of the scenarios")
ap.add_argument("--cooling", action="store_true", help="coolers instead of heaters")
ap.add_argument("--only", default=None, help="build only the scenario with this key")
ap.add_argument("--clear", action="store_true", help="clear each plot to the ground first")
ARGS = ap.parse_args()
def _server_url():
    """The dev client's server endpoint, from its MCMCP config (the port can be moved off the
    default when another game on this machine already holds it)."""
    import re
    with open(B.CONFIG_PATH, encoding='utf-8', errors='replace') as handle:
        m = re.search(r'I:serverPort=(\d+)', handle.read())
    return 'http://127.0.0.1:%s/mcp' % (m.group(1) if m else '25586')


SERVER_URL = _server_url()
server = _Session(SERVER_URL)

# Refuse to build anywhere but a lab world: the server endpoint's port is shared by every game on
# this machine, and whichever started first holds it.
_info = json.loads(server.call('server_world_info'))
if 'HVAC Lab' not in _info.get('motd', ''):
    raise SystemExit('refusing to build: the server on %s is %r, not an HVAC Lab world'
                     % (SERVER_URL, _info.get('motd')))

PLAYER = json.loads(server.call('server_list_players'))['players'][0]['name']
G = 4            # the ground (grass) layer of the lab worlds; floors replace it, air starts at 5
SPACING = 110    # between scenario origins along +X

# NSEWUD facing metadata (EnumFacing index).
D, U, N, S, W, E = 0, 1, 2, 3, 4, 5

HEATER = 'hvac_cooler' if ARGS.cooling else 'hvac_heater'
RTU = 'hvac_rtu_cooler' if ARGS.cooling else 'hvac_rtu_heater'
LOW, HIGH = (65, 80) if not ARGS.cooling else (65, 78)

blocks = []


def put(x, y, z, block, meta=0, nbt=None):
    b = {"x": x, "y": y, "z": z, "block": block if ':' in block else 'csm:' + block,
         "metadata": meta}
    if nbt:
        b["nbt"] = nbt
    blocks.append(b)


def fill(x0, y0, z0, x1, y1, z1, block, meta=0):
    server.call('server_set_blocks', mode='fill', block=block, metadata=meta,
                x=x0, y=y0, z=z0, toX=x1, toY=y1, toZ=z1)


def flush():
    global blocks
    for i in range(0, len(blocks), 900):
        server.call('server_set_blocks', mode='list', blocks=blocks[i:i + 900])
    blocks = []


def pos_list(ps):
    return '[' + ','.join('{x:%d,y:%d,z:%d}' % p for p in ps) + ']'


def sign(x, z, lines, y=G + 1):
    nbt = {}
    for i, text in enumerate(lines[:4]):
        nbt['Text%d' % (i + 1)] = json.dumps({"text": text})
    snbt = '{' + ','.join('%s:%s' % (k, json.dumps(v)) for k, v in nbt.items()) + '}'
    put(x, y, z, 'minecraft:standing_sign', 8, snbt)


def room(x0, z0, w, d, h, wall='minecraft:concrete', wall_meta=0, roof='minecraft:concrete'):
    """A closed box whose inside is x0+1..x0+w, G+1..G+h, z0+1..z0+d. Returns the roof y."""
    x1, z1, y1 = x0 + w + 1, z0 + d + 1, G + h + 1
    fill(x0, G, z0, x1, G, z1, 'minecraft:concrete', 8)            # floor slab
    fill(x0, G + 1, z0, x1, y1 - 1, z0, wall, wall_meta)
    fill(x0, G + 1, z1, x1, y1 - 1, z1, wall, wall_meta)
    fill(x0, G + 1, z0, x0, y1 - 1, z1, wall, wall_meta)
    fill(x1, G + 1, z0, x1, y1 - 1, z1, wall, wall_meta)
    fill(x0, y1, z0, x1, y1, z1, roof)
    fill(x0 + 1, G + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1, 'minecraft:air')
    return y1


def thermostat(x, y, z, facing, units=(), vents=(), zones=(), low=LOW, high=HIGH):
    put(x, y, z, 'hvac_thermostat', facing,
        '{tLo:%d,tHi:%d,lU:%s,lV:%s,lZ:%s}' % (low, high, pos_list(units), pos_list(vents),
                                               pos_list(zones)))


def zone(x, y, z, facing, primary, vents=(), low=LOW, high=HIGH):
    put(x, y, z, 'hvac_zone_thermostat', facing,
        '{tLo:%d,tHi:%d,hP:1b,lP:{x:%d,y:%d,z:%d},lV:%s}' % ((low, high) + primary
                                                            + (pos_list(vents),)))


def vent(x, y, z, owner):
    put(x, y, z, 'hvac_vent_relay', N, '{hasLink:1b,linkX:%d,linkY:%d,linkZ:%d}' % owner)


def unit(x, y, z, kind=None, powered=True):
    """A unit with a redstone block under it (a cabinet unit replaces the floor there)."""
    put(x, y, z, kind or HEATER, N)
    if powered:
        put(x, y - 1, z, 'minecraft:redstone_block')


def rtu(x, roof_y, z, powered=True):
    put(x, roof_y + 1, z, RTU, N)
    if powered:
        put(x, roof_y, z, 'minecraft:redstone_block')  # the roof block under the unit
    return (x, roof_y + 1, z)


SCENARIOS = []


def scenario(key):
    def wrap(fn):
        SCENARIOS.append((key, fn))
        return fn
    return wrap


@scenario('room')
def small_room(x, z):
    """10 x 4 x 10, one cabinet unit and a thermostat, no vents."""
    room(x, z, 10, 10, 4)
    unit(x + 2, G + 1, z + 2)
    thermostat(x + 9, G + 2, z + 10, N, units=[(x + 2, G + 1, z + 2)])
    sign(x + 5, z - 2, ['A: small room', '10x4x10', '1 cabinet unit', 'no vents'])


@scenario('closet')
def closet(x, z):
    """A 2 x 3 x 2 closet on a rooftop unit and one vent: the controller must not ring."""
    roof = room(x, z, 2, 2, 3)
    t = (x + 1, G + 2, z + 1)
    v = (x + 2, G + 3, z + 2)
    r = rtu(x + 1, roof, z + 1)
    thermostat(*t, facing=S, units=[r], vents=[v])
    vent(*v, owner=t)
    sign(x + 2, z - 2, ['B: closet', '2x3x2', 'rooftop unit', '1 vent'])


@scenario('atrium')
def atrium(x, z):
    """12 x 20 x 12, four vents under the roof, thermostat at head height."""
    roof = room(x, z, 12, 12, 20)
    t = (x + 1, G + 2, z + 6)
    vents = [(x + 3, roof - 1, z + 3), (x + 10, roof - 1, z + 3), (x + 3, roof - 1, z + 10),
             (x + 10, roof - 1, z + 10)]
    units = [rtu(x + 2, roof, z + 2), rtu(x + 8, roof, z + 8)]
    thermostat(*t, facing=E, units=units, vents=vents)
    for v in vents:
        vent(*v, owner=t)
    sign(x + 6, z - 2, ['C: tall atrium', '12x20x12', '4 ceiling vents', 'tstat at floor'])


@scenario('hallway')
def hallway(x, z):
    """40 x 3 x 3: the unit at one end, the thermostat at the other."""
    room(x, z, 40, 3, 3)
    unit(x + 1, G + 1, z + 2)
    thermostat(x + 40, G + 2, z + 2, W, units=[(x + 1, G + 1, z + 2)])
    sign(x + 20, z - 2, ['D: long hallway', '40x3x3', 'unit at far end'])


@scenario('lshape')
def l_shape(x, z):
    """An L of two 12 x 4 x 6 wings; unit in one wing, thermostat round the corner."""
    room(x, z, 12, 18, 4)
    fill(x + 7, G + 1, z + 7, x + 12, G + 4, z + 18, 'minecraft:concrete')  # cut out the L
    unit(x + 11, G + 1, z + 2)
    thermostat(x + 1, G + 2, z + 17, E, units=[(x + 11, G + 1, z + 2)])
    sign(x + 6, z - 2, ['E: L-shaped room', 'unit in one wing', 'tstat round corner'])


@scenario('twostorey')
def two_storey(x, z):
    """Two 16 x 4 x 16 floors, each its own rooftop system, joined by a 2 x 3 stair opening."""
    fill(x, G, z, x + 17, G, z + 17, 'minecraft:concrete', 8)
    fill(x, G + 1, z, x + 17, G + 10, z + 17, 'minecraft:concrete')
    fill(x + 1, G + 1, z + 1, x + 16, G + 4, z + 16, 'minecraft:air')       # ground floor
    fill(x + 1, G + 6, z + 1, x + 16, G + 9, z + 16, 'minecraft:air')       # upper floor
    fill(x + 14, G + 5, z + 13, x + 15, G + 5, z + 15, 'minecraft:air')     # stair opening
    roof = G + 10
    t1 = (x + 1, G + 2, z + 8)
    t2 = (x + 1, G + 7, z + 8)
    v1 = [(x + 5, G + 4, z + 5), (x + 12, G + 4, z + 12)]
    v2 = [(x + 5, G + 9, z + 5), (x + 12, G + 9, z + 12)]
    r1 = rtu(x + 3, roof, z + 3)
    r2 = rtu(x + 10, roof, z + 10)
    thermostat(*t1, facing=E, units=[r1], vents=v1, low=70, high=80)
    thermostat(*t2, facing=E, units=[r2], vents=v2, low=65, high=80)
    for v in v1:
        vent(*v, owner=t1)
    for v in v2:
        vent(*v, owner=t2)
    sign(x + 8, z - 2, ['F: two storeys', 'own RTU each', 'ground 70 / up 65', 'open stair'])


@scenario('flats')
def flats(x, z):
    """Three 12 x 4 x 12 flats in a row sharing walls; the middle one has no heat."""
    fill(x, G, z, x + 40, G, z + 13, 'minecraft:concrete', 8)
    fill(x, G + 1, z, x + 40, G + 5, z + 13, 'minecraft:brick_block')
    for i in range(3):
        x0 = x + i * 13
        fill(x0 + 1, G + 1, z + 1, x0 + 12, G + 4, z + 12, 'minecraft:air')
        if i != 1:
            unit(x0 + 2, G + 1, z + 2)
            thermostat(x0 + 11, G + 2, z + 12, N, units=[(x0 + 2, G + 1, z + 2)])
        else:
            thermostat(x0 + 11, G + 2, z + 12, N, units=[(x0 + 2, G + 1, z + 2)])
            unit(x0 + 2, G + 1, z + 2, powered=False)
    sign(x + 20, z - 2, ['G: three flats', 'shared walls', 'middle unheated', '(unit unpowered)'])


@scenario('office')
def office(x, z):
    """A hallway with the primary (no vents) and three offices with a zone each; one door open."""
    fill(x, G, z, x + 31, G, z + 17, 'minecraft:concrete', 8)
    fill(x, G + 1, z, x + 31, G + 5, z + 17, 'minecraft:concrete')
    fill(x + 1, G + 1, z + 1, x + 30, G + 4, z + 3, 'minecraft:air')        # hallway
    roof = G + 5
    p = (x + 1, G + 2, z + 2)
    zones = []
    units = [rtu(x + 4, roof, z + 2), rtu(x + 14, roof, z + 2)]
    setpoints = [(70, 76), (65, 80), (72, 78)]
    for i in range(3):
        x0 = x + 1 + i * 10
        fill(x0, G + 1, z + 5, x0 + 8, G + 4, z + 16, 'minecraft:air')      # office
        put(x0 + 4, G + 1, z + 4, 'minecraft:wooden_door', 5 if i == 1 else 1)  # south, 4=open
        put(x0 + 4, G + 2, z + 4, 'minecraft:wooden_door', 8)
        zt = (x0, G + 2, z + 10)
        vents = [(x0 + 2, G + 4, z + 7), (x0 + 6, G + 4, z + 13)]
        zone(*zt, facing=E, primary=p, vents=vents, low=setpoints[i][0], high=setpoints[i][1])
        for v in vents:
            vent(*v, owner=zt)
        zones.append(zt)
    thermostat(*p, facing=E, units=units, zones=zones)
    sign(x + 15, z - 2, ['H: office', 'primary in hall', '3 zones 70/65/72', 'middle door open'])


@scenario('store')
def store(x, z):
    """The AltoTEST store: one 86 x 13 x 23 space, glass front, 8 rooftop units on a primary
    with no vents of its own, four zones (70/65/65/65) with vents hung at y+6."""
    w, d, h = 86, 23, 13
    roof = room(x, z, w, d, h, wall='minecraft:brick_block')
    fill(x + 4, G + 2, z, x + w - 3, G + 4, z, 'minecraft:stained_glass')   # shop front
    p = (x + 1, G + 2, z + 18)
    units = [rtu(x + 6 + i * 10, roof, z + 6) for i in range(8)]
    zones = []
    for i, low in enumerate((70, 65, 65, 65)):
        cx = x + 11 + i * 21
        zt = (cx, G + 2, z + d)
        vents = [(cx + dx, G + 7, z + dz) for dx in (-8, -2, 4, 9) for dz in (5, 12, 19)]
        zone(*zt, facing=N, primary=p, vents=vents, low=low, high=80)
        for v in vents:
            vent(*v, owner=zt)
        zones.append(zt)
    thermostat(*p, facing=E, units=units, zones=zones)
    sign(x + 43, z - 2, ['I: store replica', '86x13x23', '8 RTU, 4 zones', 'zone 1 at 70'])


@scenario('openings')
def openings(x, z):
    """Two identical rooms, one with an open doorway and a window hole."""
    for i in range(2):
        x0 = x + i * 16
        room(x0, z, 12, 12, 4)
        unit(x0 + 2, G + 1, z + 2)
        thermostat(x0 + 11, G + 2, z + 12, N, units=[(x0 + 2, G + 1, z + 2)])
        if i == 1:
            fill(x0 + 6, G + 1, z, x0 + 7, G + 2, z, 'minecraft:air')       # doorway
            fill(x0 + 13, G + 2, z + 6, x0 + 13, G + 3, z + 7, 'minecraft:air')  # window hole
    sign(x + 14, z - 2, ['J: openings', 'left sealed', 'right: open door', '+ window hole'])


@scenario('standalone')
def standalone(x, z):
    """A room held by an unlinked, powered unit; no thermostat."""
    room(x, z, 10, 10, 4)
    unit(x + 5, G + 1, z + 5)
    sign(x + 5, z - 2, ['K: standalone unit', 'no thermostat', 'holds ~70 / 74'])


@scenario('outdoors')
def outdoors(x, z):
    """A thermostat on a post under the open sky, with a heater beside it."""
    fill(x + 2, G + 1, z + 2, x + 2, G + 3, z + 2, 'minecraft:concrete')
    unit(x + 4, G + 1, z + 2)
    thermostat(x + 3, G + 2, z + 2, E, units=[(x + 4, G + 1, z + 2)])
    sign(x + 3, z - 2, ['L: outdoors', 'reads the biome', 'warns not enclosed'])


def main():
    for cmd in ('gamerule doDaylightCycle false', 'gamerule doWeatherCycle false',
                'gamerule doMobSpawning false', 'difficulty peaceful', 'time set 6000',
                'weather clear 1000000'):
        server.call('server_run_command', command=cmd)
    for i, (key, fn) in enumerate(SCENARIOS):
        if ARGS.only and ARGS.only != key:
            continue
        x = ARGS.x + i * SPACING
        z = ARGS.z
        # Stand over the plot so its chunks are loaded before anything is written to them.
        server.call('server_teleport_player', player=PLAYER, x=x + 40, y=G + 30, z=z - 10, fly=True)
        time.sleep(3)
        if ARGS.clear:
            for x0 in range(x - 3, x + SPACING - 10, 16):
                fill(x0, G + 1, z - 3, min(x0 + 15, x + SPACING - 10), G + 26, z + 30,
                     'minecraft:air')
            fill(x - 3, G, z - 3, x + SPACING - 10, G, z + 30, 'minecraft:grass')
        fn(x, z)
        flush()
        print('%-10s at x=%d z=%d  (%s)' % (key, x, z, fn.__doc__.split('\n')[0]))


main()
