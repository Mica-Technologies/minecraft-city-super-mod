#!/usr/bin/env python3
"""
build_transit_demo.py -- builds the Transit demo world, over MCMCP.

Load a fresh FLAT creative world named "Transit Demo" in a dev client with every module (Transit
needs Roads; the shelter's ad panel is Signage's and the airport's walk-through metal detector is
Life Safety's, and both are skipped if those modules are missing), then run this. It lays out the
whole Transit tab to walk round:

  street   (z 0..17)  a bus street: stops for all four agencies on Roads' sign posts, flags hung
                      left and right with route plates set, a back-to-back pair and a flag set
                      back onto a traffic pole, the timetable and route map cases, arrival
                      displays, the curb plaque; glass shelters in each livery (the CITYLINE one
                      with Signage's shelter ad panel), flat-roof and cantilever shelters on the
                      south side; a bus concourse under a platform canopy with a departure board
                      bank and bay displays over two more bays
  station  (x 0..46, z 18..52) a subway station: the entrance kiosk over a stair, an open stair
                      with railings, name plates and globe lamps, fare vending machines; below, the
                      concourse with line bullets on a tiled wall, the fare line (standard and ADA
                      gates, railings, paid area plates, service gate) and the agent's booth, and
                      an open-cut platform with the whole platform fit-out
  terminal (x 50..100, z 22..52) check-in run and kiosks, queue stanchions, flight boards,
                      baggage carousel, carts and wayfinding; a security lane through the
                      partition (with Life Safety's metal detector); the gate lounge with desks,
                      scanner, gate sign, seating and a board
  airside  (z 54..101) four blocks below the terminal: a jet bridge from the gate to an empty
                      stand with its stand sign and ground equipment, the wind sock, beacon and
                      antenna mast; a taxiway, connector and runway with edge, centreline,
                      threshold, stop bar and approach lights and the airfield signs, each circuit
                      switched by a lever (labelled)
  plinths  (z -8)     every Transit block not shown elsewhere, each with a sign

The landside ground is raised to y 8 (so the station's lower level fits above bedrock); the
airside is the flat world's own ground at y 3. Lighting is sea lanterns in the pavements, the
station ceiling and the terminal roof, so the demo reads at night.

The site is placed relative to the player (--x/--z to move it). The client and server endpoints
default to the dev client's (25585/25586, token from run/config/mcmcp.cfg); --client-port,
--server-port and --config point it at another client.

Usage:
    python build_transit_demo.py [--x X --z Z] [--client-port P --server-port P --config CFG]
"""
import argparse
import json
import os
import re
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_bench as B  # noqa: E402 -- its MCMCP client

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TAB = os.path.join(ROOT, "modules", "transit", "src", "main", "java", "com", "micatechnologies",
                   "minecraft", "csm", "tabs", "CsmTabTransit.java")

ap = argparse.ArgumentParser(description="Build the Transit demo world.")
ap.add_argument("--x", type=int, default=None, help="west end of the street (default: player)")
ap.add_argument("--z", type=int, default=None, help="north edge of the street (default: player)")
ap.add_argument("--client-port", type=int, default=25585)
ap.add_argument("--server-port", type=int, default=25586)
ap.add_argument("--config", default=B.CONFIG_PATH, help="mcmcp.cfg holding the auth token")
ARGS = ap.parse_args()
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
X0 = ARGS.x if ARGS.x is not None else int(_me['position']['x']) - 4
Z0 = ARGS.z if ARGS.z is not None else int(_me['position']['z']) - 2

G, Y = 8, 9        # landside: the ground block, and where things stand
LF, YL = 2, 3      # the station's lower level: its floor block, and where things stand
AG, YA = 3, 4      # airside: the flat world's ground, and where things stand

LIT = 4 | 8          # an airfield light lit, and powered (its circuit's lever starts on)

# Facing metadata: horizontal index for the NSEW blocks, DirectionEight for road signs.
S, W, N, E = 0, 1, 2, 3
SIGN = {S: 0, W: 1, N: 2, E: 3}
EF = {N: 2, S: 3, W: 4, E: 5}      # EnumFacing index (the fare vending machine)

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
    for i in range(0, len(blocks), 800):
        r = server.call('server_set_blocks', mode='list', blocks=blocks[i:i + 800])
        if '"success": false' in r or 'rror' in r[:200]:
            print('set_blocks:', r[:300])
    blocks = []


def fill(x0, y0, z0, x1, y1, z1, block, meta=0):
    flush()
    name = block if ':' in block else 'csm:' + block
    server.call('server_set_blocks', mode='fill', block=name, metadata=meta, x=X0 + x0, y=y0,
                z=Z0 + z0, toX=X0 + x1, toY=y1, toZ=Z0 + z1)
    if name.startswith('csm:'):
        placed.add(name[4:])


def cmd(c):
    return json.loads(server.call('server_run_command', command=c))


def sign(x, y, z, lines, rot=0):
    """A standing sign; rot 0 faces south, 4 west, 8 north, 12 east."""
    nbt = {'Text%d' % (i + 1): json.dumps({"text": t}) for i, t in enumerate(lines[:4])}
    snbt = '{' + ','.join('%s:%s' % (k, json.dumps(v)) for k, v in nbt.items()) + '}'
    put(x, y, z, 'minecraft:standing_sign', rot, snbt)


def routes(a, b=0, c=0, left=False):
    return "{r:[B;%db,%db,%db]%s}" % (a, b, c, ",l:1b" if left else "")


def value(n):
    return "{n:%db}" % n


# ------------------------------------------------------------------------------------------
# World rules and the ground
# ------------------------------------------------------------------------------------------
for c in ('gamerule doDaylightCycle false', 'gamerule doWeatherCycle false',
          'gamerule doMobSpawning false', 'difficulty peaceful', 'time set 6000',
          'weather clear 1000000', 'kill @e[type=!player]'):
    cmd(c)
server.call('server_teleport_player', player=PLAYER, x=X0 + 2.5, y=40, z=Z0 - 12.5, fly=True)

LX0, LX1, LZ0, LZ1 = -14, 114, -26, 53          # the raised landside
fill(LX0, G + 1, LZ0, LX1, G + 12, LZ1, 'minecraft:air')
fill(LX0, 4, LZ0, LX1, G - 1, LZ1, 'minecraft:stone')
fill(LX0, G, LZ0, LX1, G, LZ1, 'minecraft:grass')
fill(LX0, 4, LZ1, LX1, G, LZ1, 'minecraft:concrete', 0)        # the apron wall
fill(-16, AG + 1, 54, 114, AG + 10, 104, 'minecraft:air')

# ------------------------------------------------------------------------------------------
# The street: north sidewalk z 0..3, road z 4..13, south sidewalk z 14..17
# ------------------------------------------------------------------------------------------
fill(-10, G, 0, 112, G, 3, 'minecraft:concrete', 8)
fill(-10, G, 4, 112, G, 13, 'minecraft:concrete', 15)
fill(-10, G, 14, 112, G, 17, 'minecraft:concrete', 8)
for x in range(-10, 113):
    put(x, G, 4, 'minecraft:concrete', 0)
    put(x, G, 13, 'minecraft:concrete', 0)
    if x % 4 < 2:
        put(x, G, 8, 'minecraft:concrete', 4)
    if x % 8 == 0:
        put(x, G, 0, 'minecraft:sea_lantern')
        put(x, G, 17, 'minecraft:sea_lantern')


def stop(x, z, agency, pieces, rts, facing=E, left=False):
    """A bus stop: its pieces from the ground up (signposts, cases, the display), the flag on
    top."""
    for i, p in enumerate(pieces):
        put(x, Y + i, z, p, SIGN[facing])
    put(x, Y + len(pieces), z, 'bus_stop_flag_' + agency, SIGN[facing], routes(*rts, left=left))


# North sidewalk: a stop for every agency, both flag sides, cases and displays; posts at the curb.
stop(4, 3, 'cityline', ['signpost', 'bus_stop_timetable_case', 'bus_stop_arrival_display'],
     (12, 40, 0))
stop(16, 3, 'riverway', ['signpost', 'bus_stop_route_map_case', 'bus_stop_arrival_display'],
     (5, 23, 0), left=True)
stop(28, 3, 'verdant', ['signpost', 'signpost'], (88, 14, 3))
stop(40, 3, 'emberline', ['signpost', 'bus_stop_timetable_case'], (7, 22, 63), left=True)
# Back to back: CITYLINE on the post facing east, RIVERWAY hung behind it facing west.
stop(52, 3, 'cityline', ['signpost', 'signpost'], (2, 9, 0))
put(51, Y + 2, 3, 'bus_stop_flag_riverway', SIGN[W], routes(31, 0, 0))
# Set back: a VERDANT flag hung on a traffic signal pole, which sets it back into the pole's line.
for y in range(Y, Y + 5):
    put(62, y, 3, 'trafficpolevertical')
put(63, Y + 2, 3, 'bus_stop_flag_verdant', SIGN[E], routes(18, 44, 0))
put(5, Y, 2, 'bus_stop_curb_plaque', S)

# Glass shelters in each livery, facing the road, three long.
for x0, agency in ((7, 'cityline'), (19, 'riverway'), (31, 'verdant'), (43, 'emberline')):
    for x in range(x0, x0 + 3):
        put(x, Y, 1, 'bus_shelter_glass_' + agency, S)
        put(x, Y + 1, 1, 'bus_shelter_glass_' + agency, S | 4 | 8)
# South sidewalk: flat-roof and cantilever shelters in each livery, facing the road, two long.
for i, agency in enumerate(('cityline', 'riverway', 'verdant', 'emberline')):
    for style, x0 in (('flat', 2 + i * 7), ('cantilever', 32 + i * 6)):
        for x in (x0, x0 + 1):
            put(x, Y, 16, 'bus_shelter_%s_%s' % (style, agency), N)
            put(x, Y + 1, 16, 'bus_shelter_%s_%s' % (style, agency), N | 4 | 8)

# The bus concourse: a platform canopy on steel columns, two bays, bay displays, a board bank.
for x in (58, 67, 76):
    for z in (14, 17):
        for y in range(Y, Y + 4):
            put(x, y, z, 'platform_column_steel')
fill(58, Y + 4, 14, 76, Y + 4, 17, 'platform_canopy', 0)
stop(61, 14, 'verdant', ['signpost', 'signpost'], (6, 27, 0))
stop(72, 14, 'emberline', ['signpost', 'bus_stop_arrival_display'], (50, 0, 0), left=True)
put(61, Y + 3, 15, 'bus_bay_display', N)
put(72, Y + 3, 15, 'bus_bay_display', N)
fill(63, Y, 18, 70, Y + 3, 18, 'station_tile_white')
fill(63, Y + 3, 18, 70, Y + 3, 18, 'station_tile_band_cityline')
for x in range(64, 70):
    for y in (Y + 1, Y + 2):
        put(x, y, 17, 'bus_departure_board', N, "{f:0b,a:0b}")
sign(-2, Y, 1, ["Transit Demo", "Bus street", "stops, shelters,", "departure boards"], 12)
sign(57, Y, 16, ["Bus concourse", "Click a board to", "filter by agency,", "sneak for speech"], 8)
flush()

# ------------------------------------------------------------------------------------------
# The station: a plaza over a lower level (floor y 2), an open-cut platform to the south
# ------------------------------------------------------------------------------------------
fill(0, G, 18, 46, G, 37, 'minecraft:concrete', 8)
# the lower concourse and paid area, under the plaza
fill(0, LF, 25, 46, G - 1, 38, 'station_tile_white')
fill(1, LF + 1, 26, 45, G - 2, 37, 'minecraft:air')
fill(1, LF, 26, 45, LF, 37, 'minecraft:concrete', 8)
fill(1, G - 1, 26, 45, G - 1, 37, 'minecraft:stone')
fill(0, G - 2, 25, 46, G - 2, 25, 'station_tile_band_cityline')
for x in range(3, 45, 4):
    for z in range(27, 38, 4):
        put(x, G - 1, z, 'minecraft:sea_lantern')
# the open cut: platform z 38..44, track bed z 45..49, retaining wall z 50
fill(0, 1, 38, 46, G, 51, 'station_tile_white')
fill(1, LF + 1, 38, 45, G + 1, 49, 'minecraft:air')
fill(1, LF, 38, 45, LF, 44, 'minecraft:concrete', 8)
fill(1, 1, 45, 45, LF, 49, 'minecraft:air')
fill(1, 1, 45, 45, 1, 49, 'minecraft:gravel')
fill(1, 2, 47, 45, 2, 47, 'minecraft:rail', 1)
fill(1, 4, 50, 45, 4, 50, 'station_tile_band_verdant')
fill(0, G, 50, 46, G, 51, 'minecraft:concrete', 8)
for x in range(1, 46):
    put(x, Y, 37, 'station_entrance_railing')
    put(x, Y, 50, 'station_entrance_railing')


def stair(xs, ztop, steps=6):
    """A stair down southward from the ground (the top step level with it) to the lower level:
    each step's standing space and headroom cut out of the ground above it."""
    for i in range(steps):
        y = G - i
        for x in xs:
            put(x, y + 1, ztop + i, 'minecraft:air')
            put(x, y + 2, ztop + i, 'minecraft:air')
            put(x, y, ztop + i, 'minecraft:stone_brick_stairs', 3)


# The entrance kiosk: glass on the rim of the well, a CITYLINE roof with its name boards.
stair((5, 6), 20)
for y in (Y, Y + 1):
    for z in range(20, 24):
        put(4, y, z, 'station_entrance_glass')
        put(7, y, z, 'station_entrance_glass')
    for x in (5, 6):
        put(x, y, 23, 'station_entrance_glass')
legends = {4: 1, 5: 5, 6: 2, 7: 1}          # read from the street: SUBWAY, CIVIC SQUARE
for x in range(4, 8):
    for z in range(19, 24):
        put(x, Y + 2, z, 'station_entrance_roof_cityline', 0,
            value(legends[x]) if z == 19 else value(1))
put(3, Y, 19, 'station_entrance_globe', 0)
put(8, Y, 19, 'station_entrance_globe', 0)
put(11, Y, 19, 'farevend', EF[N])
# The open stair: railings round the well, name plates, globes at its mouth.
stair((31, 32), 21)
for z in range(21, 25):
    put(30, Y, z, 'station_entrance_railing')
    put(33, Y, z, 'station_entrance_railing')
for x in (31, 32):
    put(x, Y, 24, 'station_entrance_railing')
put(30, Y, 22, 'station_entrance_railing_sign', 0, value(1))    # SUBWAY
put(33, Y, 22, 'station_entrance_railing_sign', 0, value(4))    # CIVIC SQUARE
put(30, Y, 20, 'station_entrance_globe', 1)                     # red
put(33, Y, 20, 'station_entrance_globe', 2)                     # white
put(36, Y, 20, 'farevend', EF[N])
sign(9, Y, 21, ["Station", "Entrance kiosk:", "click the roof", "to step names"], 8)
sign(35, Y, 22, ["Open stair", "Click a globe", "for its colour"], 8)

# Down below: line bullets on the north wall, the fare line, the booth.
for i, x in enumerate(range(10, 26)):
    put(x, YL + 1, 26, 'station_line_bullet', S, value(i + 1))
put(9, YL, 26, 'farevend', EF[S])
put(28, YL + 2, 29, 'platform_sign_to_trains', N)
fill(5, YL, 26, 6, YL, 31, 'tactile_guidance_yellow', 1)
FZ = 32
line = {2: 'station_fare_railing', 3: 'station_fare_railing_sign', 4: 'station_fare_railing',
        9: 'station_fare_railing', 12: 'station_fare_railing', 16: 'station_fare_railing',
        18: 'station_fare_railing', 19: 'station_fare_railing_sign'}
for x in range(20, 35):
    line[x] = 'station_fare_railing_sign' if x in (25, 30) else 'station_fare_railing'
line[1] = 'station_fare_railing'
for x, b in line.items():
    put(x, YL, FZ, b)
for x in (6, 7, 8):
    put(x, YL + 1, FZ, 'fare_gate', N)
put(10, YL + 1, FZ, 'fare_gate_ada_2', N)
put(14, YL + 1, FZ, 'fare_gate_ada_3', N)
put(17, YL, FZ, 'station_service_gate', N)
# the booth, its counter facing the unpaid side
for x in (36, 37, 38):
    put(x, YL, 31, 'station_booth_counter', N)
    put(x, YL + 1, 31, 'station_entrance_glass')
for y in (YL, YL + 1):
    for z in (31, 32, 33):
        put(35, y, z, 'station_entrance_glass')
        put(39, y, z, 'station_entrance_glass')
    for x in (36, 37):
        put(x, y, 33, 'station_entrance_glass')
for x in range(35, 40):
    for z in (31, 32, 33):
        put(x, YL + 2, z, 'station_entrance_roof_riverway', 0, value(1))
sign(3, YL, 29, ["Fare line", "Buy a ticket at", "the machine, then", "click a gate"], 4)

# The platform: fit-out complementing RCMC's stations.
fill(1, YL, 44, 45, YL, 44, 'tactile_warning_yellow', 0)
fill(2, YL, 42, 44, YL, 42, 'tactile_guidance_grey', 0)
for x in (10, 22, 34):
    for y in range(YL, YL + 4):
        put(x, y, 39, 'platform_column_steel')
for y in range(YL, YL + 4):
    put(28, y, 39, 'platform_column_number' if y == YL + 2 else 'platform_column_tile', 0,
        value(2) if y == YL + 2 else None)
fill(1, G - 1, 38, 45, G - 1, 44, 'platform_canopy', 0)
for x in (6, 7, 8):
    put(x, YL, 40, 'platform_bench', S)
for x in (12, 13):
    put(x, YL, 40, 'platform_perch', S)
put(16, YL, 40, 'platform_litter_bin', S)
put(19, YL, 40, 'platform_help_point', S)
for x in (24, 25):
    put(x, YL, 40, 'platform_bench', S)
put(38, YL, 41, 'platform_validator', S)
put(39, YL, 41, 'platform_validator', S)
put(15, YL + 3, 42, 'platform_number_sign', S, value(2))
put(27, YL + 3, 42, 'platform_clock', S)
put(8, YL + 3, 42, 'platform_sign_lines', S)
put(40, YL + 3, 40, 'platform_sign_exit', N)
put(31, YL + 3, 40, 'platform_cctv_dome')
for x in (10, 30):
    put(x, YL + 2, 49, 'station_name_sign', N, value(4))
put(20, YL + 1, 49, 'platform_gap_sign', N)
put(1, YL + 1, 35, 'station_network_map', E)
put(45, YL + 1, 40, 'station_emergency_point', W)
put(45, YL + 3, 38, 'platform_cctv_camera', W)
sign(44, YL, 43, ["Platform", "Help point,", "validators, bench:", "click them"], 4)
flush()

# ------------------------------------------------------------------------------------------
# The terminal: x 50..100, z 22..52, floor y 8
# ------------------------------------------------------------------------------------------
TX0, TX1, TZ0, TZ1 = 50, 100, 22, 52
TR = Y + 5                                   # the roof
fill(TX0, G, TZ0, TX1, G, TZ1, 'minecraft:quartz_block')
for x in range(TX0, TX1 + 1):
    for z in (TZ0, TZ1):
        for y in range(Y, TR):
            put(x, y, z, 'minecraft:concrete' if x % 5 == 0 else 'minecraft:glass_pane', 0)
for z in range(TZ0, TZ1 + 1):
    for x in (TX0, TX1):
        for y in range(Y, TR):
            put(x, y, z, 'minecraft:concrete' if z % 5 == 2 else 'minecraft:glass_pane', 0)
flush()
fill(TX0, TR, TZ0, TX1, TR, TZ1, 'minecraft:stained_glass', 0)
for x in range(TX0 + 2, TX1, 4):
    for z in range(TZ0 + 2, TZ1, 4):
        put(x, TR, z, 'minecraft:sea_lantern')
for x0 in (58, 84):                          # doors in from the street
    fill(x0, Y, TZ0, x0 + 2, Y + 1, TZ0, 'minecraft:air')
fill(69, Y, TZ1, 71, Y + 1, TZ1, 'minecraft:air')       # the gate door to the jet bridge
# the partition between landside and the gate lounge, with the security lane through it
PZ = 36
fill(TX0 + 1, Y, PZ, TX1 - 1, TR - 1, PZ, 'minecraft:concrete', 0)
fill(78, Y, PZ, 78, Y + 1, PZ, 'minecraft:air')
fill(80, Y, PZ, 82, Y + 2, PZ, 'minecraft:air')
fill(88, Y, PZ, 90, Y + 1, PZ, 'minecraft:air')          # the way back out
# check-in: desk, scale, desk ... facing the entrance, three airlines; kiosks beside
row = ['airport_checkin_desk', 'airport_checkin_scale'] * 3 + ['airport_checkin_desk']
for i, reg in enumerate(row):
    airline = min(i // 2, 3)
    put(53 + i, Y, 31, reg, N | ((airline << 2) if reg.endswith('desk') else 0))
put(62, Y, 31, 'airport_self_checkin_kiosk', N)
put(63, Y, 31, 'airport_self_checkin_kiosk', N)
for x in range(53, 60):
    put(x, Y, 28, 'airport_queue_stanchion_blue')
for x in range(54, 61):
    put(x, Y, 26, 'airport_queue_stanchion_blue')
for x in range(53, 58):
    put(x, Y, 24, 'airport_queue_stanchion_black')
# the boards on the partition, facing the entrance
for x in (66, 67, 68):
    for y in (Y + 2, Y + 3):
        put(x, y, PZ - 1, 'airport_flight_board_departures', N)
for x in (71, 72):
    for y in (Y + 2, Y + 3):
        put(x, y, PZ - 1, 'airport_flight_board_arrivals', N)
# security: a lane running south through the partition, the metal detector beside it
lane = [(33, 'airport_divest_table'), (34, 'airport_security_roller'),
        (35, 'airport_security_roller'), (36, 'airport_xray_scanner'),
        (37, 'airport_security_roller'), (38, 'airport_security_roller')]
for z, reg in lane:
    put(78, Y, z, reg, E)
put(78, Y + 1, 33, 'airport_security_trays', E)
put(78, Y + 1, 34, 'airport_security_tray_items', E)
put(76, Y, 32, 'airport_security_trays', E)
DETECTOR = 'metal_detector'
put(81, Y, PZ, DETECTOR, S)
for z in (31, 32, 33, 34, 35):
    put(80, Y, z, 'airport_queue_stanchion_black')
    put(82, Y, z, 'airport_queue_stanchion_black')
# baggage claim, carts
for x in range(88, 95):
    for z in (27, 28):
        put(x, Y, z, 'airport_baggage_carousel')
for x in (88, 89, 90):
    put(x, Y, 32, 'airport_cart_rack', N)
put(93, Y, 32, 'airport_luggage_cart', N)
put(95, Y, 32, 'airport_luggage_cart', W)
# wayfinding, hung from the roof
put(57, TR - 1, 27, 'airport_sign_check_in', N)
put(75, TR - 1, 30, 'airport_sign_gates', N)
put(91, TR - 1, 31, 'airport_sign_baggage_claim', N)
put(85, TR - 1, 24, 'airport_sign_ground_transport', S)
put(89, TR - 1, 38, 'airport_sign_arrivals', S)
# the gate lounge
put(65, Y, 49, 'airport_gate_desk', N)
put(66, Y, 49, 'airport_gate_desk', N)
put(68, Y, 50, 'airport_boarding_pass_scanner', N)
put(66, TR - 1, 50, 'airport_gate_sign', N, value(32))     # B12, over its desk by the door
for x in range(60, 72):
    put(x, Y, 42, 'airport_seating_black', S)
for x in range(60, 72):
    put(x, Y, 44, 'airport_seating_blue', S)
put(61, TR - 1, 47, 'airport_flight_board_departures', N)
put(62, TR - 1, 47, 'airport_flight_board_departures', N)
fill(52, Y, 39, 56, Y, 39, 'airport_cart_rack', S)
sign(59, Y, 23, ["Airport terminal", "Kiosk: click to", "print a boarding", "pass"], 0)
sign(72, Y, 49, ["Gate B12", "Hold a pass to", "the scanner, then", "out to the stand"], 8)
flush()

# ------------------------------------------------------------------------------------------
# Airside: the apron (y 3) and the jet bridge
# ------------------------------------------------------------------------------------------
fill(40, AG, 54, 112, AG, 77, 'minecraft:concrete', 8)
fill(-16, AG, 78, 114, AG, 84, 'minecraft:concrete', 7)       # taxiway
fill(57, AG, 85, 63, AG, 89, 'minecraft:concrete', 7)         # connector
fill(-6, AG, 90, 114, AG, 97, 'minecraft:concrete', 7)        # runway
for x in range(-4, 2, 2):
    for z in range(90, 98, 2):
        put(x, AG, z, 'minecraft:concrete', 0)                 # threshold bars
for x in range(6, 110, 8):
    for dx in range(3):
        put(x + dx, AG, 93, 'minecraft:concrete', 0)
        put(x + dx, AG, 94, 'minecraft:concrete', 0)
for x in range(44, 112, 10):
    for z in (58, 70):
        put(x, AG, z, 'minecraft:sea_lantern')
flush()
# the jet bridge, facing south to the stand: rotunda by the gate door, four tunnels, the cab
JX = 70
for y in range(YA, Y):
    put(JX, y, 55, 'airport_jet_bridge_column', S)
    put(JX, y, 59, 'airport_jet_bridge_drive', S)
put(JX, Y, 55, 'airport_jet_bridge_rotunda', S)
for z in range(57, 61):
    put(JX, Y, z, 'airport_jet_bridge_tunnel', S)
put(JX, Y, 61, 'airport_jet_bridge_cab', S)
# the stand: its sign, ground equipment
for y in (YA, YA + 1):
    put(76, y, 63, 'airport_airfield_mast', S)
put(76, YA + 2, 63, 'airport_stand_sign', S, value(32))


def unit(reg, x, z, deep, tall):
    """Roads' utility box: placed facing west it runs east from its root, and up."""
    put(x, YA, z, reg, W)
    for d in range(deep):
        for h in range(tall):
            if d or h:
                put(x + d, YA + h, z, 'utility_box_part')


unit('airport_air_stairs', 63, 66, 2, 2)
unit('airport_ground_power_unit', 63, 70, 2, 2)
unit('airport_baggage_tug', 78, 64, 2, 2)
unit('airport_baggage_cart', 78, 66, 2, 2)
unit('airport_baggage_cart', 78, 68, 2, 2)
unit('airport_wheel_chocks', 70, 70, 1, 1)
# the beacon on a tower, the wind sock, the antenna mast; each circuit's lever
for y in (YA, YA + 1):
    put(44, y, 58, 'minecraft:stonebrick')
put(44, YA + 2, 58, 'airport_beacon', S | LIT)
put(45, YA + 1, 58, 'minecraft:lever', 1 | 8)
for y in (YA, YA + 1):
    put(104, y, 60, 'airport_airfield_mast', W)
put(104, YA + 2, 60, 'airport_wind_sock', W | LIT)
put(106, YA, 62, 'airport_antenna_mast', W | LIT)
put(107, YA, 62, 'minecraft:lever', 5 | 8)
flush()

# taxiway: blue edges, green centreline; the connector with a stop bar and its signs
for x in range(-12, 112, 4):
    put(x, YA, 81, 'airport_taxiway_centreline_light', W | LIT)
    put(x, YA, 77, 'airport_taxiway_edge_light', W | LIT)
    if not 56 <= x <= 64:
        put(x, YA, 85, 'airport_taxiway_edge_light', W | LIT)
for z in (84, 86):
    put(60, YA, z, 'airport_taxiway_centreline_light', N | LIT)
for x in (59, 60, 61):
    put(x, YA, 88, 'airport_stop_bar_light', N | LIT)
put(56, YA, 88, 'airport_runway_holding_sign', N | LIT, value(2))       # 9-27
put(64, YA, 86, 'airport_taxiway_location_sign', N | LIT, value(2))     # B
put(55, YA, 86, 'airport_taxiway_direction_sign', N | LIT, value(11))   # C, arrow on its right
put(44, YA, 76, 'minecraft:lever', 5 | 8)
# runway: white edges and centreline, threshold at the west end, approach bars on masts
for x in range(-4, 112, 4):
    put(x, YA, 93, 'airport_runway_centreline_light', W | LIT)
    put(x, YA, 98, 'airport_runway_edge_light', W | LIT)
    if not 56 <= x <= 64:
        put(x, YA, 89, 'airport_runway_edge_light', W | LIT)
for z in range(90, 98):
    put(-6, YA, z, 'airport_runway_threshold_light', W | LIT)
for i, x in enumerate((-8, -10, -12)):
    for y in range(YA, YA + i + 1):
        put(x, y, 93, 'airport_airfield_mast', W)
    put(x, YA + i + 1, 93, 'airport_approach_light_bar', W | LIT)
for i, x in enumerate(range(20, 111, 20)):
    put(x, YA, 100, 'airport_runway_distance_sign', N | LIT, value(5 - i if 5 - i > 0 else 1))
put(-4, YA, 99, 'minecraft:lever', 5 | 8)
flush()
# A light placed takes the state of the circuit it joins, so a batch placed at once can come up
# dark. Throwing each circuit's lever off and on again switches the whole circuit on.
LEVERS = [(44, YA, 76, 5), (-4, YA, 99, 5), (107, YA, 62, 5), (45, YA + 1, 58, 1)]
for x, y, z, m in LEVERS:
    put(x, y, z, 'minecraft:lever', m)
flush()
time.sleep(0.5)
for x, y, z, m in LEVERS:
    put(x, y, z, 'minecraft:lever', m | 8)
flush()
put(44, YA + 2, 58, 'minecraft:air')
flush()
put(44, YA + 2, 58, 'airport_beacon', S | LIT)      # it stands on the tower the lever powers
sign(43, YA, 76, ["Taxiway lights", "and signs:", "this lever"], 8)
sign(-4, YA, 100, ["Runway lights", "and approach bars:", "this lever"], 0)
sign(107, YA, 63, ["Wind sock and", "antenna mast:", "this lever"], 0)
sign(45, YA, 59, ["Airport beacon:", "the lever on", "its tower"], 0)
sign(72, YA, 63, ["Stand B12", "Jet bridge from", "gate B12 above"], 0)
flush()

# ------------------------------------------------------------------------------------------
# The shelter ad panel (Signage), placed as a player places it, against the CITYLINE shelter
# ------------------------------------------------------------------------------------------
def place_as_player(item, x, y, z, from_x, from_z, yaw, pitch):
    """Put item in hand, stand at (from_x, from_z), look, click."""
    r = cmd('replaceitem entity %s slot.hotbar.0 %s 1 0' % (PLAYER, item))
    if not r.get('succeeded'):
        return False
    client.call('client_select_slot', slot=0)
    server.call('server_teleport_player', player=PLAYER, x=X0 + from_x + 0.5, y=Y,
                z=Z0 + from_z + 0.5, fly=True)
    time.sleep(0.3)
    client.call('client_look', yaw=yaw, pitch=pitch)
    time.sleep(0.2)
    client.call('client_interact', action='use')
    time.sleep(0.6)
    client.call('client_gui_close')     # the ad board opens its setup screen when placed
    got = server.call('server_get_block', x=X0 + x, y=y, z=Z0 + z)
    return item.split(':')[1] in got


if place_as_player('csm:ad_shelter_panel', 10, Y, 1, 12, 1, 90, 20):
    placed.add('ad_shelter_panel')
    print('shelter ad panel placed')
else:
    print('shelter ad panel not placed (Signage missing, or the click missed)')
cmd('clear %s' % PLAYER)

# ------------------------------------------------------------------------------------------
# The plinth row: every Transit block not shown elsewhere
# ------------------------------------------------------------------------------------------
CLASS_REG = {'BlockFareVendingMachine': 'farevend', 'BlockFareGate': 'fare_gate',
             'BlockFareGateAda2': 'fare_gate_ada_2', 'BlockFareGateAda3': 'fare_gate_ada_3'}
tab = open(TAB, encoding='utf-8').read()
registered = []
for m in re.finditer(r'initTabBlock\((?:new \w+\("([a-z0-9_]+)"|(\w+)\.class)', tab):
    registered.append(m.group(1) or CLASS_REG[m.group(2)])
missing = [r for r in registered if r not in placed]
PZR = -8
for i, reg in enumerate(missing):
    x = i * 3
    put(x, Y, PZR, 'minecraft:stonebrick')
    if reg.startswith('bus_shelter_'):
        put(x, Y + 1, PZR, reg, S)
        put(x, Y + 2, PZR, reg, S | 4 | 8)
    elif reg.startswith('fare_gate'):
        put(x, Y + 2, PZR, reg, S)
        put(x, Y + 1, PZR, 'minecraft:air')
    else:
        put(x, Y + 1, PZR, reg, S)
    words, lines = reg.split('_'), ['']
    for w in words:
        if len(lines[-1]) + len(w) + 1 > 15:
            lines.append('')
        lines[-1] = (lines[-1] + ' ' + w).strip()
    sign(x, Y, PZR + 1, lines[:4], 0)
if missing:
    sign(-3, Y, PZR + 1, ["Everything", "else in the", "Transit tab"], 0)
flush()
print('plinths:', len(missing), missing)
still = [r for r in registered if r not in placed]
print('registered %d, shown %d, missing %s' % (len(registered), len(registered) - len(still),
                                                still))

# ------------------------------------------------------------------------------------------
# Handover: the daylight cycle back on (the flight boards and clocks run on it), spawn
# ------------------------------------------------------------------------------------------
cmd('gamerule doDaylightCycle true')
cmd('time set 1000')
cmd('setworldspawn %d %d %d' % (X0 + 1, Y, Z0 + 1))
server.call('server_teleport_player', player=PLAYER, x=X0 + 1.5, y=Y, z=Z0 + 1.5, yaw=-90,
            pitch=5, fly=False)
print('done')
