#!/usr/bin/env python3
"""
build_utilities_demo.py -- builds the Utilities demo world, over MCMCP.

Load a fresh FLAT creative world named "Utilities Demo" in a dev client with every module
(Utilities needs Roads; the compound fences are Building's chain-link and the small cells stand on
Roads' traffic poles), then run this. It lays out the Utilities tab to walk round, with the Power
Grid tab's pole line alongside:

  board    (x -9)            the spawn overview board, facing the spawn
  meters   (x 0..40,  z 0..16)   a street of three buildings with their meters: electric and gas
                                 banks and the switchboard, single meters, the service disconnect,
                                 the main breaker panel (one open), water meter setters, labels
  poles    (x 0..44,  z 20)      a short Power Grid pole line
  sewer    (x 0..46,  z 24..66)  a storm street with curb inlets (one a combination inlet); the
                                 lift station compound (wet well hatch open over its grate, valve
                                 vault hatch shut over a vault with its valve, the control panel
                                 on a lever, the standby generator running); an outfall into a
                                 pond with riprap, the pond outlet riser and the emergency
                                 spillway; a manhole cutaway beside a trench
  water    (x 55..140, z -12..40) a lattice (multi-leg) tower and a pedestal tower in both sizes,
                                 each with its town band set differently; the two ground tanks;
                                 the pump station (supply main on supports, suction header, two
                                 split-case pumps with their valves, the discharge header with
                                 the flow meter and air release valve, a booster pump with its
                                 pressure tank, the control panel, the treatment room) and, outside
                                 it, the air release enclosure and vault and the backflow hot box
  gas      (x 145..160, z 0..13) a regulator yard in a chain-link compound with its signs
  telecom  (x 165..180, z -4..14) a cell site: the monopole with two antenna arrays, the ice
                                 bridge on its stanchions, the cabinets, the GPS antenna, the
                                 generator; the fibre cabinet outside with its doors open
  cells    (x 185..201, z 0..6)  small cells on the three pole widths along a street
  plinths  (z -16)               every Utilities block not shown elsewhere, each with a sign

The ground is raised to y 12 so the wet well, the valve vault, the pond and the manhole have room
below it. The tanks are placed as a player places them (the item places the whole tank); every
other block is set directly, the utility box parts included. Sea lanterns light the pavements.

The site is placed relative to the player (--x/--z to move it). The client and server endpoints
default to the dev client's (25585/25586, token from run/config/mcmcp.cfg); --client-port,
--server-port and --config point it at another client. --only runs some areas (for iterating).

Usage:
    python build_utilities_demo.py [--x X --z Z] [--client-port P --server-port P --config CFG]
                                   [--only ground,meters,poles,sewer,water,tanks,gas,telecom,
                                    cells,plinths,board]
"""
import argparse
import json
import math
import os
import re
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_bench as B  # noqa: E402 -- its MCMCP client

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TAB = os.path.join(ROOT, "modules", "powergrid", "src", "main", "java", "com", "micatechnologies",
                   "minecraft", "csm", "tabs", "CsmTabUtilities.java")
AREAS = ('ground', 'meters', 'poles', 'sewer', 'water', 'tanks', 'gas', 'telecom', 'cells',
         'plinths', 'board')

ap = argparse.ArgumentParser(description="Build the Utilities demo world.")
ap.add_argument("--x", type=int, default=None, help="west edge of the site (default: player)")
ap.add_argument("--z", type=int, default=None, help="north edge of the site (default: player)")
ap.add_argument("--client-port", type=int, default=25585)
ap.add_argument("--server-port", type=int, default=25586)
ap.add_argument("--config", default=B.CONFIG_PATH, help="mcmcp.cfg holding the auth token")
ap.add_argument("--only", default=",".join(AREAS), help="comma-separated areas to build")
ARGS = ap.parse_args()
ONLY = set(ARGS.only.split(","))
TOKEN = re.search(r"S:authToken=(\S+)", open(ARGS.config, encoding="utf-8").read()).group(1)


class _Session:
    def __init__(self, port):
        self.mcp = B.Mcp("http://127.0.0.1:%d/mcp" % port, TOKEN).connect()

    def call(self, name, **args):
        out = self.mcp.call(name, args)
        return out if isinstance(out, str) else json.dumps(out)


client = _Session(ARGS.client_port)
server = _Session(ARGS.server_port)
_me = json.loads(client.call('client_player_state'))
PLAYER = _me['name']
X0 = ARGS.x if ARGS.x is not None else int(_me['position']['x']) + 12
Z0 = ARGS.z if ARGS.z is not None else int(_me['position']['z']) - 22

G, Y = 12, 13      # the ground block, and where things stand on it

# Horizontal facing metadata (the way the block's front faces): south, west, north, east.
S, W, N, E = 0, 1, 2, 3
ON = 4             # the stored bit above the facing: a panel's door open, a hatch open, a unit on

blocks = []
placed = set()


def put(x, y, z, block, meta=0, nbt=None):
    """A block at site coordinates (x, z relative to the site, y absolute)."""
    name = block if ':' in block else 'csm:' + block
    b = {"x": X0 + x, "y": y, "z": Z0 + z, "block": name, "metadata": meta}
    if nbt:
        b["nbt"] = nbt
    blocks.append(b)
    if name.startswith('csm:'):
        placed.add(name[4:])


def flush():
    global blocks
    for i in range(0, len(blocks), 500):
        r = server.call('server_set_blocks', mode='list', blocks=blocks[i:i + 500])
        if '"success": false' in r or 'rror' in r[:200]:
            print('set_blocks:', r[:300])
    blocks = []


def fill(x0, y0, z0, x1, y1, z1, block, meta=0):
    """A fill, split into pieces under MCMCP's volume limit."""
    flush()
    name = block if ':' in block else 'csm:' + block
    xs = sorted((x0, x1))
    ys = sorted((y0, y1))
    zs = sorted((z0, z1))
    layer = (xs[1] - xs[0] + 1) * (zs[1] - zs[0] + 1)
    step = max(1, 30000 // layer)
    if step >= 1 and layer <= 30000:
        for y in range(ys[0], ys[1] + 1, step):
            server.call('server_set_blocks', mode='fill', block=name, metadata=meta,
                        x=X0 + xs[0], y=y, z=Z0 + zs[0], toX=X0 + xs[1],
                        toY=min(ys[1], y + step - 1), toZ=Z0 + zs[1])
    else:
        for x in range(xs[0], xs[1] + 1, 100):
            fill(x, ys[0], zs[0], min(xs[1], x + 99), ys[1], zs[1], block, meta)
    if name.startswith('csm:'):
        placed.add(name[4:])


def cmd(c):
    return json.loads(server.call('server_run_command', command=c))


def sign(x, y, z, lines, rot=0):
    """A standing sign; rot 0 faces south, 4 west, 8 north, 12 east."""
    put(x, y, z, 'minecraft:standing_sign', rot, _sign_nbt(lines))


def wall_sign(x, y, z, lines, facing):
    """A wall sign; facing 2 north, 3 south, 4 west, 5 east."""
    put(x, y, z, 'minecraft:wall_sign', facing, _sign_nbt(lines))


def _sign_nbt(lines):
    nbt = {'Text%d' % (i + 1): json.dumps({"text": t}) for i, t in enumerate(lines[:4])}
    return '{' + ','.join('%s:%s' % (k, json.dumps(v)) for k, v in nbt.items()) + '}'


def _turn(dx, dz, facing):
    """A north-facing offset turned to facing (y: 90 takes north to east)."""
    for _ in range({N: 0, E: 1, S: 2, W: 3}[facing]):
        dx, dz = -dz, dx
    return dx, dz


def unit(reg, x, y, z, facing, w=1, d=1, h=1, meta_extra=0):
    """Roads' utility box (or a unit built on it): the root, and the invisible parts in the
    other cells. A wider unit grows toward the placing player's right, a deeper one away from
    them, a taller one up (UtilityBoxSpec)."""
    put(x, y, z, reg, facing | meta_extra)
    for dy in range(h):
        for dxn in range(0, -w, -1):
            for dzn in range(d):
                if dy == 0 and dxn == 0 and dzn == 0:
                    continue
                ox, oz = _turn(dxn, dzn, facing)
                put(x + ox, y + dy, z + oz, 'utility_box_part')


def aim(eye, target):
    dx, dy, dz = (target[i] - eye[i] for i in range(3))
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    return yaw, pitch


def use_item(item, target, stand, meta=0):
    """Put item in hand (or nothing), stand at stand (site coordinates, feet), look at target
    (site coordinates, a point), right-click."""
    flush()
    if item:
        cmd('replaceitem entity %s slot.hotbar.0 %s 1 %d' % (PLAYER, item, meta))
    else:
        cmd('replaceitem entity %s slot.hotbar.0 minecraft:air' % PLAYER)
    client.call('client_select_slot', slot=0)
    eye = (stand[0], stand[1] + 1.62, stand[2])
    yaw, pitch = aim(eye, target)
    server.call('server_teleport_player', player=PLAYER, x=X0 + stand[0], y=stand[1],
                z=Z0 + stand[2], yaw=yaw, pitch=pitch, fly=True)
    time.sleep(0.5)
    client.call('client_look', yaw=yaw, pitch=pitch)
    time.sleep(0.3)
    client.call('client_interact', action='use', ticks=1)
    time.sleep(0.8)


def block_at(x, y, z):
    return server.call('server_get_block', x=X0 + x, y=y, z=Z0 + z)


def lanterns(x0, x1, z, every=6, y=G):
    for x in range(x0, x1 + 1, every):
        put(x, y, z, 'minecraft:sea_lantern')


def fence_ring(x0, z0, x1, z1, gap=(), y=Y):
    """Building's chain-link fence two high with its barbed-wire top, round a compound."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x in (x0, x1) or z in (z0, z1)) and (x, z) not in gap:
                put(x, y, z, 'chainlink_fence')
                put(x, y + 1, z, 'chainlink_fence')
                put(x, y + 2, z, 'chainlink_barbed_top')


# ------------------------------------------------------------------------------------------
# World rules and the ground
# ------------------------------------------------------------------------------------------
def ground():
    for c in ('gamerule doDaylightCycle false', 'gamerule doWeatherCycle false',
              'gamerule doMobSpawning false', 'difficulty peaceful', 'time set 6000',
              'weather clear 1000000', 'kill @e[type=!player]'):
        cmd(c)
    server.call('server_teleport_player', player=PLAYER, x=X0 - 12.5, y=60, z=Z0 + 22.5,
                fly=True)
    fill(-24, G + 1, -24, 212, G + 40, 76, 'minecraft:air')
    fill(-24, 1, -24, 212, G - 1, 76, 'minecraft:stone')
    fill(-24, G - 3, -24, 212, G - 1, 76, 'minecraft:dirt')
    fill(-24, G, -24, 212, G, 76, 'minecraft:grass')


# ------------------------------------------------------------------------------------------
# The meters street: buildings z 0..5, sidewalk z 6..9, road z 10..15
# ------------------------------------------------------------------------------------------
def building(x0, x1, block, meta=0, windows=()):
    fill(x0, Y, 0, x1, Y + 4, 5, block, meta)
    fill(x0 + 1, Y, 1, x1 - 1, Y + 3, 4, 'minecraft:air')
    fill(x0 - 1, Y + 5, -1, x1 + 1, Y + 5, 6, 'minecraft:stone_slab', 0)
    for x in windows:
        put(x, Y + 2, 5, 'minecraft:glass_pane')
        put(x, Y + 3, 5, 'minecraft:glass_pane')


def meters():
    fill(-2, G, 6, 42, G, 9, 'minecraft:concrete', 8)
    fill(-2, G, 10, 42, G, 15, 'minecraft:concrete', 15)
    for x in range(-2, 43):
        if x % 4 < 2:
            put(x, G, 12, 'minecraft:concrete', 4)
    lanterns(0, 42, 9)
    flush()
    # 1: an apartment block of brick -- the meter room's wall on the street
    building(2, 12, 'minecraft:brick_block', windows=(3, 11))
    for x in range(4, 8):
        put(x, Y + 1, 6, 'electric_meter_bank', S)
    put(5, Y + 3, 6, 'utility_label_electric', S)
    unit('electric_switchboard', 8, Y, 6, S, h=2)
    for x in (9, 10, 11):
        put(x, Y, 6, 'gas_meter_bank', S)
    put(10, Y + 1, 6, 'utility_label_gas', S)
    sign(3, Y, 8, ["Apartments", "electric meter", "bank, switchboard,", "gas meter bank"], 0)
    # 2: a house of stone brick -- one of each single meter, the panel open
    building(15, 25, 'minecraft:stonebrick', windows=(16, 24))
    put(17, Y + 1, 6, 'electric_meter_digital', S)
    put(18, Y + 1, 6, 'service_disconnect', S)
    put(18, Y + 2, 6, 'utility_label_disconnect', S)
    put(20, Y + 1, 6, 'main_panel', S | ON)
    put(22, Y, 6, 'gas_meter', S)
    put(22, Y + 1, 6, 'utility_label_gas', S)
    put(23, Y, 6, 'water_meter_setter', S)
    put(23, Y + 1, 6, 'utility_label_water', S)
    sign(16, Y, 8, ["House", "digital meter,", "disconnect, panel", "(click its door)"], 0)
    # 3: a shop of concrete -- the analog meter, a blank socket, the panel shut
    building(28, 38, 'minecraft:concrete', 0, windows=(29, 37))
    put(30, Y + 1, 6, 'electric_meter_analog', S)
    put(31, Y + 1, 6, 'electric_meter_socket', S)
    put(30, Y + 2, 6, 'utility_label_electric', S)
    put(33, Y + 1, 6, 'main_panel', S)
    put(35, Y, 6, 'gas_meter', S)
    put(36, Y, 6, 'water_meter_setter', S)
    sign(29, Y, 8, ["Shop", "analog meter,", "blank socket,", "panel shut"], 0)
    flush()


# ------------------------------------------------------------------------------------------
# A short Power Grid pole line between the meters street and the storm street
# ------------------------------------------------------------------------------------------
def poles():
    UP = 1
    for x in (2, 16, 30, 44):
        put(x, Y, 20, 'fgpolebottom', UP)
        for y in range(Y + 1, Y + 7):
            put(x, y, 20, 'fgpolemiddle', UP)
        put(x, Y + 7, 20, 'fgpoletop', UP)
        for i in range(7):      # the crossarm: seven one-block segments across the line
            put(x - 3 + i, Y + 7, 19, 'oldbrooksxarm%d' % (i + 1), 4)
    put(16, Y + 5, 21, 'transformermount', S)
    put(30, Y + 2, 21, 'polehvsign', S)
    sign(0, Y, 21, ["Power Grid tab", "a pole line:", "poles, arms,", "mounts, signs"], 0)
    flush()


# ------------------------------------------------------------------------------------------
# Sewer and stormwater
# ------------------------------------------------------------------------------------------
def sewer():
    # the storm street: road (smooth stone slabs, a curb's height below the walk) z 24..29,
    # sidewalk z 30..32 with the curb inlets in its edge
    fill(-2, G, 24, 46, G, 29, 'minecraft:stone_slab', 0)
    fill(-2, G, 30, 46, G, 32, 'minecraft:concrete', 8)
    lanterns(0, 46, 32)
    for x in (6, 7, 8):
        put(x, G, 30, 'curb_inlet', N)
    put(7, Y, 30, 'storm_drain_marker', N)
    put(20, G, 30, 'curb_inlet', N)
    put(34, G, 30, 'curb_inlet', N)
    put(34, Y, 29, 'gutter_inlet_grate', N)
    sign(4, Y, 31, ["Storm street", "a three-block", "curb inlet"], 8)
    sign(32, Y, 31, ["Combination", "inlet: the curb", "inlet and the", "gutter grate"], 8)
    flush()

    # the lift station compound, x 2..16, z 36..50
    fill(2, G, 36, 16, G, 50, 'minecraft:concrete', 7)
    fence_ring(2, 36, 16, 50, gap={(9, 36), (10, 36)})
    for x, z in ((5, 39), (13, 39), (9, 44), (5, 49), (13, 49)):
        put(x, G, z, 'minecraft:sea_lantern')
    flush()
    # the wet well under its hatch: a chamber with water in its bottom; the hatch open
    fill(4, G - 8, 41, 7, G - 1, 43, 'minecraft:concrete', 7)
    fill(5, G - 7, 42, 6, G - 1, 42, 'minecraft:air')
    fill(5, G - 7, 42, 6, G - 6, 42, 'minecraft:water')
    put(5, G, 42, 'wet_well_hatch', N | ON)
    put(6, G, 42, 'wet_well_hatch', S | ON)
    # the valve vault under its hatch, shut: a run of pipe and a gate valve inside
    fill(10, G - 6, 41, 13, G - 1, 43, 'minecraft:concrete', 7)
    fill(11, G - 5, 42, 12, G - 1, 42, 'minecraft:air')
    put(11, G - 5, 42, 'water_pipe')
    put(12, G - 5, 42, 'water_gate_valve', E)
    put(11, G, 42, 'valve_vault_hatch', N)
    put(12, G, 42, 'valve_vault_hatch', S)
    # the control panel, alarmed by the lever beside it; the generator running
    unit('lift_station_control_panel', 5, Y, 47, N, h=2, meta_extra=ON)
    put(6, Y, 47, 'minecraft:lever', 5 | 8)
    unit('lift_station_generator', 13, Y, 47, N, w=2, h=2, meta_extra=ON)
    for x, z in ((3, 37), (15, 37), (3, 49), (15, 49)):
        put(x, Y, z, 'bollard_pipe_sleeve', N)
    sign(8, Y, 35, ["Lift station", "wet well open,", "valve vault shut:", "click a hatch"], 8)
    sign(3, Y, 45, ["Control panel:", "the lever alarms", "it (beacon and", "flood light)"], 8)
    sign(15, Y, 45, ["Standby generator", "running: click", "it to stop or", "start it"], 8)
    flush()

    # the pond: a storm outfall into it from the north bank, its outlet riser, the spillway
    fill(19, G, 39, 43, G, 56, 'minecraft:air')
    fill(20, G - 4, 41, 42, G - 4, 56, 'minecraft:sand')
    fill(20, G - 3, 41, 42, G - 1, 56, 'minecraft:water')
    fill(19, G - 3, 39, 43, G - 2, 40, 'minecraft:dirt')
    fill(19, G - 1, 39, 43, G - 1, 40, 'minecraft:grass')
    for x, reg in ((22, 'outfall_headwall'), (23, 'outfall_headwall'),
                   (24, 'outfall_headwall_flap_gate'), (25, 'outfall_headwall'),
                   (26, 'outfall_headwall'), (34, 'outfall_headwall'),
                   (35, 'outfall_headwall_pipe'), (36, 'outfall_headwall')):
        put(x, G, 38, reg, S)
    for x in (22, 26, 34, 36):
        put(x, G, 39, 'outfall_wingwall', S)
        put(x, G, 40, 'outfall_wingwall', S)
    for x in (23, 24, 25, 35):
        for z in (39, 40):
            put(x, G, z, 'riprap')
    for y in range(G - 3, G + 1):
        put(31, y, 50, 'outlet_riser', N)
    # the embankment on the south side, the spillway in a notch through it, riprap below
    fill(18, G + 1, 57, 44, G + 1, 59, 'minecraft:grass')
    fill(18, G, 57, 44, G, 59, 'minecraft:dirt')
    for x in (24, 25, 26):
        for z in (57, 58, 59):
            put(x, G + 1, z, 'emergency_spillway', S)
        put(x, G + 1, 60, 'riprap')
    sign(20, Y, 37, ["Storm outfall", "headwall, flap", "gate, wingwalls,", "riprap apron"], 0)
    sign(30, Y + 1, 60, ["Detention pond", "outlet riser and", "emergency", "spillway"], 0)
    flush()

    # a manhole cut away beside a trench, its cover on the ground above; whole sections beside
    fill(2, G - 5, 55, 9, G - 1, 62, 'minecraft:stone')
    fill(3, G - 4, 55, 8, G, 58, 'minecraft:air')
    for y in range(G - 4, G):
        put(5, y, 59, 'manhole_riser_cutaway', N)
    put(5, G, 59, 'manhole_cone_cutaway', N)
    put(5, Y, 59, 'manhole_sewer', N)
    put(3, G - 4, 57, 'minecraft:sea_lantern')
    put(8, G - 4, 57, 'minecraft:sea_lantern')
    put(11, Y, 59, 'manhole_riser')
    put(11, Y + 1, 59, 'manhole_riser')
    put(11, Y + 2, 59, 'manhole_cone')
    put(12, Y, 59, 'manhole_riser')
    put(12, Y + 1, 59, 'manhole_riser')
    sign(4, Y, 54, ["Manhole cutaway", "riser sections,", "cone, cover:", "look from the trench"], 0)
    flush()


# ------------------------------------------------------------------------------------------
# The water system: towers and ground tanks (the frames here, the tanks in tanks()), pump station
# ------------------------------------------------------------------------------------------
LEG, RISER = 'water_tower_leg', 'water_tower_riser'


def multileg(ax, az, leg, top, panels):
    """Four legs and the riser from Y to top, struts at the panel points, X bracing between
    them on all four sides, and the caged ladder up the east side of the south-east leg."""
    for dx in (-leg, leg):
        for dz in (-leg, leg):
            for y in range(Y, top + 1):
                put(ax + dx, y, az + dz, LEG)
    for y in range(Y, top + 1):
        put(ax, y, az, RISER)
    span = 2 * leg
    for h in panels:
        for side in (-leg, leg):
            for i in range(1, span):
                put(ax - leg + i, h, az + side, 'water_tower_strut', 0)
                put(ax + side, h, az - leg + i, 'water_tower_strut', 1)
        if h + span <= top:
            for side in (-leg, leg):
                for i in range(1, span):
                    for by in (h + i, h + span - i):
                        put(ax - leg + i, by, az + side, 'water_tower_brace', 0)
                        put(ax + side, by, az - leg + i, 'water_tower_brace', 1)
    for y in range(Y, top + 1):
        put(ax + leg + 1, y, az + leg, 'caged_ladder', E)


def pedestal(ax, az, top):
    for y in range(Y, top + 1):
        put(ax, y, az, 'water_tower_pedestal', W)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if dx or dz:
                    put(ax + dx, y, az + dz, 'water_tank_part')


TOWERS = {  # key: (axis x, axis z, top of the column, tank, clicks on the band)
    'lattice_medium': (66, 8, Y + 21, 'water_tower_bowl_medium', 1),
    'pedestal_medium': (92, 8, Y + 17, 'water_tower_spheroid_medium', 3),
    'lattice_small': (66, 32, Y + 13, 'water_tower_bowl_small', 5),
    'pedestal_small': (92, 32, Y + 11, 'water_tower_spheroid_small', 6),
}


def water():
    ax, az, top, _, _ = TOWERS['lattice_medium']
    multileg(ax, az, 5, top, (Y + 1, Y + 11, Y + 21))
    ax, az, top, _, _ = TOWERS['lattice_small']
    multileg(ax, az, 3, top, (Y + 1, Y + 7, Y + 13))
    for key in ('pedestal_medium', 'pedestal_small'):
        ax, az, top, _, _ = TOWERS[key]
        pedestal(ax, az, top)
    flush()
    sign(56, Y, 8, ["Lattice tower", "legs, riser,", "bracing, struts,", "caged ladder"], 4)
    sign(82, Y, 8, ["Pedestal tower", "click a tank to", "change the town", "on its band"], 4)
    sign(56, Y, 32, ["Small lattice", "tower"], 4)
    sign(82, Y, 32, ["Small pedestal", "tower"], 4)

    # the pump station: x 104..132, z 14..32, brick with a glass roof
    fill(104, G, 14, 132, G, 32, 'minecraft:concrete', 8)
    fill(104, Y, 14, 132, Y + 4, 32, 'minecraft:brick_block')
    fill(105, Y, 15, 131, Y + 4, 31, 'minecraft:air')
    fill(104, Y + 5, 14, 132, Y + 5, 32, 'minecraft:glass')
    for x in range(107, 131, 5):
        for z in (17, 25, 30):
            put(x, Y + 5, z, 'minecraft:sea_lantern')
    fill(104, Y, 22, 104, Y + 2, 24, 'minecraft:air')             # the door, west
    for z in range(16, 31, 3):
        put(104, Y + 2, z, 'minecraft:glass_pane')
        put(132, Y + 2, z, 'minecraft:glass_pane')
    flush()
    # the supply main from the large ground tank, on supports, down into the suction header
    for z in range(8, 18):
        put(126, Y + 1, z, 'water_pipe')
    for z in (9, 12, 16):
        put(126, Y, z, 'water_pipe_support', N)
    put(126, Y + 1, 18, 'water_pipe')
    for x in range(106, 127):
        put(x, Y, 18, 'water_pipe')
    # two split-case pumps, each with its suction valve, check valve and discharge valve
    for px in (110, 118):
        put(px, Y, 19, 'water_butterfly_valve', N)
        unit('pump_split_case', px, Y, 20, S, w=2)
        put(px, Y, 21, 'water_check_valve', S)
        put(px, Y, 22, 'water_gate_valve', S)
    # the discharge header: flow meter, air release valve, out through the east wall
    for x in range(110, 136):
        if x == 122:
            put(x, Y, 23, 'water_flow_meter', E)
        elif x == 125:
            put(x, Y, 23, 'water_air_release_valve', E)
        else:
            put(x, Y, 23, 'water_pipe')
    # a booster: the vertical inline pump on a branch, the pressure tank on it
    for z in range(24, 28):
        put(113, Y, z, 'water_pipe')
    put(113, Y, 28, 'water_pipe')
    unit('pump_vertical_inline', 114, Y, 28, S, h=2)
    for x in (115, 116, 117):
        put(x, Y, 28, 'water_pipe')
    put(118, Y, 28, 'water_gate_valve', E)
    put(119, Y, 28, 'water_pipe')
    put(116, Y, 29, 'water_pipe')
    unit('water_hydropneumatic_tank', 116, Y, 30, S, h=2)
    unit('pump_control_panel', 105, Y, 27, E, h=2)
    # the treatment room's two pieces
    unit('chemical_feed_skid', 128, Y, 30, N, w=2, h=2)
    unit('chlorine_cylinder_scale', 130, Y, 30, N, h=2)
    sign(103, Y, 21, ["Pump station", "pumps, valves,", "flow meter, HMI", "panel, treatment"], 4)
    flush()
    # outside, on the main: the air release pieces and the backflow hot box
    fill(134, G, 18, 140, G, 32, 'minecraft:concrete', 8)
    unit('air_release_enclosure', 137, Y, 20, W)
    unit('air_release_vault', 137, Y, 23, W)
    unit('backflow_enclosure', 137, Y, 27, W, w=2)
    sign(139, Y, 18, ["Above ground", "air release", "enclosure, vault,", "backflow hot box"], 12)
    flush()


def tanks():
    """The tanks, placed as a player places them; then the band clicked to its town."""
    flush()
    for key, (ax, az, top, reg, clicks) in TOWERS.items():
        # standing a little east of the axis, on the column's top, looking down at its top face
        use_item('csm:' + reg, (ax + 0.5, top + 1.0, az + 0.5), (ax + 2.5, top + 2, az + 0.5))
        server.call('server_teleport_player', player=PLAYER, x=X0 + ax + 0.5, y=top + 40,
                    z=Z0 + az + 0.5, fly=True)
        time.sleep(0.5)
        if reg not in block_at(ax, top + 2, az):
            print('tank not placed:', key, block_at(ax, top + 2, az)[:200])
        placed.add(reg)
        if 'bowl' in reg:
            ladder_to_balcony(ax, az, 5 if 'medium' in reg else 3, top)
    # the ground tanks: the small one two layers, the large one layer
    for (ax, az, reg, layers) in ((110, 0, 'ground_tank_small', 2),
                                  (124, 0, 'ground_tank_large', 1)):
        for k in range(layers):
            yb = G + 3 * k
            use_item('csm:' + reg, (ax + 0.5, yb + 1.0, az + 0.5), (ax + 0.5, yb + 2, az - 1.5))
            server.call('server_teleport_player', player=PLAYER, x=X0 + ax + 0.5, y=yb + 40,
                        z=Z0 + az + 0.5, fly=True)
            time.sleep(0.5)
        placed.add(reg)
    sign(103, Y, 0, ["Ground storage", "tanks: small, two", "layers; large,", "one layer"], 4)
    flush()
    cmd('clear %s' % PLAYER)


def _walkway(x, y, z):
    j = json.loads(block_at(x, y, z))
    return j['block'] == 'csm:water_tank_part' and j['metadata'] >= 1


def ladder_to_balcony(ax, az, leg, top):
    """The caged ladder carried on up past the leg's top beside the bowl to its balcony: the
    walkway's railing opens where the ladder comes up beside it."""
    lx, lz = ax + leg + 1, az + leg
    for y in range(top + 1, top + 16):
        if json.loads(block_at(lx, y, lz))['block'] != 'minecraft:air':
            break
        put(lx, y, lz, 'caged_ladder', E)
        flush()
        if _walkway(lx - 1, y, lz) or _walkway(lx, y, lz - 1):
            break


def bands():
    """Each tank's band clicked round to a different town: click the band on its near side."""
    for key, (ax, az, top, reg, clicks) in TOWERS.items():
        half = 7 if 'medium' in reg else 4
        for _ in range(clicks):
            use_item(None, (ax + 0.5, top + 4.5, az + 0.5), (ax - half - 1.5, top + 3, az + 0.5))


# ------------------------------------------------------------------------------------------
# The gas yard, the cell site, small cells
# ------------------------------------------------------------------------------------------
GX = 145


def gas():
    x0 = GX
    fill(x0, G, 0, x0 + 14, G, 12, 'minecraft:gravel')
    fill(x0 + 3, G, 3, x0 + 11, G, 7, 'gas_station_skid')
    fence_ring(x0, 0, x0 + 14, 12, gap={(x0 + 7, 12), (x0 + 8, 12)})
    flush()
    # inlet riser, valve, line heater, regulator, meter, valve, outlet riser
    run = [(2, 'gas_pipe', 0), (3, 'gas_ball_valve', E), (4, 'gas_pipe', 0),
           (6, 'gas_pipe', 0), (7, 'gas_pressure_regulator', E), (8, 'gas_pipe', 0),
           (9, 'gas_turbine_meter', E), (10, 'gas_ball_valve', E), (11, 'gas_pipe', 0),
           (12, 'gas_pipe', 0)]
    for dx, reg, m in run:
        put(x0 + dx, Y, 4, reg, m)
    put(x0 + 2, G, 4, 'gas_pipe')
    put(x0 + 12, G, 4, 'gas_pipe')
    unit('gas_line_heater', x0 + 5, Y, 4, E, w=2, h=2)
    for y in range(Y + 1, Y + 5):
        put(x0 + 8, y, 4, 'gas_pipe')
    put(x0 + 8, Y + 5, 4, 'gas_vent_stack', N)
    unit('gas_odorant_tank', x0 + 5, Y, 6, N, w=2, h=2)
    put(x0 + 8, Y, 6, 'gas_pipe')
    put(x0 + 9, Y, 6, 'gas_pressure_regulator', E)
    put(x0 + 10, Y, 6, 'gas_pipe')
    # the signs on the fence, facing out, and on the gate's posts
    put(x0 + 3, Y + 1, -1, 'gas_sign_station', N)
    put(x0 + 6, Y + 1, -1, 'gas_sign_warning', N)
    put(x0 + 9, Y + 1, -1, 'gas_sign_no_smoking', N)
    put(x0 + 12, Y + 1, -1, 'gas_sign_emergency', N)
    put(x0 + 6, Y + 1, 13, 'gas_sign_authorized', S)
    put(x0 + 9, Y + 1, 13, 'gas_sign_warning', S)
    put(x0 + 1, Y, -2, 'utility_marker_gas', N)
    sign(x0 + 4, Y, 14, ["Gas regulator", "station: skid set", "in the ground,", "heater, meter run"], 0)
    flush()


def telecom():
    x0 = GX + 20
    fill(x0, G, 0, x0 + 14, G, 14, 'minecraft:gravel')
    fence_ring(x0, 0, x0 + 14, 14, gap={(x0 + 6, 14), (x0 + 7, 14)})
    flush()
    mx, mz = x0 + 10, 9
    for y in range(Y, Y + 10):
        put(mx, y, mz, 'monopole_section')
    put(mx, Y + 10, mz, 'monopole_antenna_array', S)
    put(mx, Y + 11, mz, 'monopole_section')
    put(mx, Y + 12, mz, 'monopole_antenna_array', W)
    fill(x0 + 2, G, 7, x0 + 6, G, 8, 'minecraft:concrete', 8)
    unit('cell_equipment_cabinet', x0 + 3, Y, 7, N, h=2)
    unit('cell_equipment_cabinet', x0 + 4, Y, 7, N, h=2)
    unit('cell_battery_cabinet', x0 + 5, Y, 7, N, h=2)
    for x in range(x0 + 3, mx):
        put(x, Y + 2, 9, 'ice_bridge', 1)
    for x in (x0 + 3, mx - 1):
        put(x, Y, 9, 'ice_bridge_stanchion')
        put(x, Y + 1, 9, 'ice_bridge_stanchion')
    put(x0 + 3, Y + 1, 10, 'gps_antenna', S)
    fill(x0 + 2, G, 3, x0 + 5, G, 4, 'minecraft:concrete', 8)
    unit('lift_station_generator', x0 + 4, Y, 3, N, w=2, h=2)
    put(x0 + 3, Y + 1, -1, 'cell_site_sign', N)
    put(x0 + 9, Y + 1, -1, 'gas_sign_authorized', N)
    fill(x0 + 9, G, -4, x0 + 12, G, -3, 'minecraft:concrete', 8)
    unit('fiber_distribution_cabinet', x0 + 11, Y, -3, N, w=2, h=2, meta_extra=ON)
    put(x0 + 1, Y, -2, 'utility_marker_telecom', N)
    sign(x0 + 7, Y, -6, ["Cell site", "monopole, arrays,", "ice bridge,", "cabinets, GPS"], 8)
    sign(x0 + 13, Y, -6, ["Fibre cabinet", "doors open:", "click to shut"], 8)
    flush()


def cells():
    x0 = GX + 40
    fill(x0, G, 0, x0 + 16, G, 3, 'minecraft:concrete', 15)
    fill(x0, G, 4, x0 + 16, G, 6, 'minecraft:concrete', 8)
    for x, reg in ((x0 + 2, 'trafficpolevertical'), (x0 + 7, 'trafficpoleverticalconcrete'),
                   (x0 + 12, 'trafficpolepedestalsilver')):
        for y in range(Y, Y + 5):
            put(x, y, 5, reg, 1)
        put(x, Y + 5, 5, 'small_cell_antenna')
        put(x, Y + 3, 4, 'small_cell_radio', N)
    sign(x0 + 4, Y, 6, ["Small cells", "canister on the", "pole top, radio", "on its side"], 0)
    flush()


# ------------------------------------------------------------------------------------------
# Plinths: every Utilities block not shown elsewhere; the overview board
# ------------------------------------------------------------------------------------------
def plinths():
    tab = open(TAB, encoding='utf-8').read()
    registered = re.findall(r'initTabBlock\(new \w+\("([a-z0-9_]+)"', tab)
    missing = [r for r in registered if r not in placed]
    z = -16
    for i, reg in enumerate(missing):
        x = i * 3
        put(x, Y, z, 'minecraft:stonebrick')
        put(x, Y + 1, z, reg, S)
        words, lines = reg.split('_'), ['']
        for w in words:
            if len(lines[-1]) + len(w) + 1 > 15:
                lines.append('')
            lines[-1] = (lines[-1] + ' ' + w).strip()
        sign(x, Y, z + 1, lines[:4], 0)
    flush()
    still = [r for r in registered if r not in placed]
    print('registered %d, shown %d, on plinths %s, missing %s'
          % (len(registered), len(registered) - len(still), missing, still))


def _at(x, z):
    """World coordinates of a site point, for the board."""
    return "%d, %d" % (X0 + x, Z0 + z)


BOARD = [
    ["Utilities Demo", "every piece of", "the Utilities", "tab (x, z)"],
    ["Meters street", _at(20, 8), "meters, panels,", "labels"],
    ["Power Grid", "pole line", _at(20, 20)],
    ["Sewer & storm", _at(20, 40), "lift station:", "lever alarms"],
    ["Water towers", _at(79, 20), "click a tank for", "its town name"],
    ["Pump station", _at(118, 23), "ground tanks", _at(117, 0)],
    ["Gas yard", _at(152, 6), "Cell site", _at(172, 7)],
    ["Small cells", _at(193, 5), "Fibre cabinet", _at(176, -3)],
    ["Things to click", "hatches, panel", "door, generator,", "fibre cabinet"],
]


def board():
    fill(-9, Y, 16, -9, Y + 2, 28, 'minecraft:stonebrick')
    for i, lines in enumerate(BOARD):
        wall_sign(-10, Y + 1, 18 + i, lines, 4)
    put(-9, Y + 3, 22, 'minecraft:sea_lantern')
    flush()


# ------------------------------------------------------------------------------------------
if 'ground' in ONLY:
    ground()
for area, fn in (('meters', meters), ('poles', poles), ('sewer', sewer), ('water', water),
                 ('gas', gas), ('telecom', telecom), ('cells', cells)):
    if area in ONLY:
        fn()
        print(area, 'done')
if 'tanks' in ONLY:
    tanks()
    bands()
    print('tanks done')
if 'board' in ONLY:
    board()
if 'plinths' in ONLY:
    if ONLY != set(AREAS):
        print('(plinths: the check is only meaningful when every area was built in this run)')
    plinths()

# ------------------------------------------------------------------------------------------
# Handover: the daylight cycle back on, the spawn at the board
# ------------------------------------------------------------------------------------------
if ONLY == set(AREAS):
    cmd('gamerule doDaylightCycle true')
    cmd('time set 1000')
    cmd('setworldspawn %d %d %d' % (X0 - 13, Y, Z0 + 22))
    server.call('server_teleport_player', player=PLAYER, x=X0 - 12.5, y=Y, z=Z0 + 22.5,
                yaw=-90, pitch=0, fly=False)
print('done')
