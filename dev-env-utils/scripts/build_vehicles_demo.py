#!/usr/bin/env python3
"""
build_vehicles_demo.py -- builds the Vehicles demo world, over MCMCP.

Load a fresh FLAT creative world named "Vehicles Demo" in a dev client with every module (CSM:
Vehicles needs Roads and Immersive Vehicles; the bus stop is Transit's, and the chip bed in the
parts chest is only a part with Parks), stand where the intersection should go, and run this. It
lays out:

  intersection  Main St (north-south, one lane each way) crossing Transit Ave (east-west, two
                lanes each way), signals on far-side mast arms, an ADVANCED controller, a preempt
                detector clamped on top of every arm looking up its approach, and a Transit Queue Jump add-on
                under the curb lane head of each Transit Ave approach. The controller runs N-S and
                E-W on max recall; a detector seeing emergency lights preempts its approach (and
                lights the red confirmation beacons on that circuit's mast poles), and
                one seeing a bus's TSP emitter calls transit priority for E-W, with a 5 s queue
                jump
  bus stop      on Transit Ave's westbound curb east of the junction: a CITYLINE stop and shelter
  parking lot   south-east of the junction, entered from Transit Ave: every livery of every CSM
                vehicle, emergency fleet in the north row, work trucks and buses in the south
                row, and a chest of the pack's parts by the entrance

Vehicles are placed with their items, as a player does, so they arrive with their default
parts (a vehicle item given NBT spawns without them). A placed vehicle faces 90 degrees from its
placer, so each is placed from beside its bay with the camera aimed by client_look, filling the
row from the far end so the placer never stands in a parked vehicle.

Usage:
    python build_vehicles_demo.py [--x X --z Z] [--only roads|lot|vehicles]
                                  [--client-port P --server-port P --config CFG]
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
OBJ_DIR = os.path.join(ROOT, "modules", "vehicles", "src", "main", "resources", "assets",
                       "csmvehicles", "objmodels", "vehicles")

ap = argparse.ArgumentParser(description="Build the Vehicles demo world.")
ap.add_argument("--x", type=int, default=None, help="intersection centre x (default: player)")
ap.add_argument("--z", type=int, default=None, help="intersection centre z (default: player)")
ap.add_argument("--only", choices=("roads", "lot", "vehicles"), default=None)
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
X0 = ARGS.x if ARGS.x is not None else int(_me['position']['x'])
Z0 = ARGS.z if ARGS.z is not None else int(_me['position']['z'])

G, Y = 3, 4          # the flat world's ground block, and where things stand
ARM = Y + 6          # mast arm height; heads hang one below it

# horizontal facing index (S W N E) for detectors and signs; a signal head's metadata is it * 4
S, W, N, E = 0, 1, 2, 3
# EnumFacing index for the poles (rotatable on all six faces)
UP, F_NORTH, F_SOUTH, F_WEST, F_EAST = 1, 2, 3, 4, 5

blocks = []


def put(x, y, z, block, meta=0, nbt=None):
    """A block at site coordinates (x, z relative to the intersection centre, y absolute)."""
    name = block if ':' in block else 'csm:' + block
    b = {"x": X0 + x, "y": y, "z": Z0 + z, "block": name, "metadata": meta}
    if nbt:
        b["nbt"] = nbt
    blocks.append(b)


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
    server.call('server_set_blocks', mode='fill', block=name, metadata=meta,
                x=X0 + min(x0, x1), y=y0, z=Z0 + min(z0, z1),
                toX=X0 + max(x0, x1), toY=y1, toZ=Z0 + max(z0, z1))


def cmd(c):
    return json.loads(server.call('server_run_command', command=c))


def sign(x, z, lines, rot=0, y=Y):
    """A standing sign; rot 0 faces south, 4 west, 8 north, 12 east."""
    nbt = {'Text%d' % (i + 1): json.dumps({"text": t}) for i, t in enumerate(lines[:4])}
    snbt = '{' + ','.join('%s:%s' % (k, json.dumps(v)) for k, v in nbt.items()) + '}'
    put(x, y, z, 'minecraft:standing_sign', rot, snbt)


def L(x, y, z):
    """A site position as the long BlockPos.toLong writes."""
    x, z = X0 + x, Z0 + z
    v = ((x & 0x3FFFFFF) << 38) | ((y & 0xFFF) << 26) | (z & 0x3FFFFFF)
    return v - (1 << 64) if v >= 1 << 63 else v


# ------------------------------------------------------------------------------------------
# Roads

MAIN_HALF, MAIN_LEN = 3, 130      # Main St: x -3..3, one lane each way
AVE_HALF, AVE_LEN = 5, 170        # Transit Ave: z -5..5, two lanes each way
WALK = 3                          # sidewalk width


def roads():
    asphalt, line_w, line_y, walk = ('minecraft:concrete', 7), 0, 4, ('minecraft:concrete', 8)
    # sidewalks first, roads over them where they cross
    fill(-MAIN_HALF - WALK, G, -MAIN_LEN, MAIN_HALF + WALK, G, MAIN_LEN, *walk)
    fill(-AVE_LEN, G, -AVE_HALF - WALK, AVE_LEN, G, AVE_HALF + WALK, *walk)
    fill(-MAIN_HALF, G, -MAIN_LEN, MAIN_HALF, G, MAIN_LEN, *asphalt)
    fill(-AVE_LEN, G, -AVE_HALF, AVE_LEN, G, AVE_HALF, *asphalt)
    box = AVE_HALF + WALK          # the crosswalks and the junction lie inside this
    # Main St: double yellow down the middle, outside the junction
    for z0, z1 in ((-MAIN_LEN, -box - 3), (box + 3, MAIN_LEN)):
        fill(0, G, z0, 0, G, z1, 'minecraft:concrete', line_y)
    # Transit Ave: yellow centre, dashed white between each direction's two lanes
    for x0, x1 in ((-AVE_LEN, -MAIN_HALF - WALK - 3), (MAIN_HALF + WALK + 3, AVE_LEN)):
        fill(x0, G, 0, x1, G, 0, 'minecraft:concrete', line_y)
        for x in range(x0, x1 + 1):
            if x % 6 < 3:
                put(x, G, -3, 'minecraft:concrete', line_w)
                put(x, G, 3, 'minecraft:concrete', line_w)
    # crosswalks (ladder stripes) and stop bars
    for x in range(-MAIN_HALF - WALK, -MAIN_HALF):
        for z in range(-AVE_HALF, AVE_HALF + 1, 2):
            put(x, G, z, 'minecraft:concrete', line_w)
            put(-x, G, z, 'minecraft:concrete', line_w)
    for z in range(-AVE_HALF - WALK, -AVE_HALF):
        for x in range(-MAIN_HALF, MAIN_HALF + 1, 2):
            put(x, G, z, 'minecraft:concrete', line_w)
            put(x, G, -z, 'minecraft:concrete', line_w)
    stop = MAIN_HALF + WALK + 2
    fill(stop, G, -AVE_HALF, stop, G, -1, 'minecraft:concrete', line_w)      # westbound
    fill(-stop, G, 1, -stop, G, AVE_HALF, 'minecraft:concrete', line_w)      # eastbound
    stop = AVE_HALF + WALK + 2
    fill(-MAIN_HALF, G, -stop, -1, G, -stop, 'minecraft:concrete', line_w)   # southbound
    fill(1, G, stop, MAIN_HALF, G, stop, 'minecraft:concrete', line_w)       # northbound
    # sea lanterns in the sidewalks so it reads at night
    for x in range(-AVE_LEN, AVE_LEN + 1, 10):
        if abs(x) > MAIN_HALF + WALK:
            put(x, G, -AVE_HALF - WALK, 'minecraft:sea_lantern')
            put(x, G, AVE_HALF + WALK, 'minecraft:sea_lantern')
    for z in range(-MAIN_LEN, MAIN_LEN + 1, 10):
        if abs(z) > AVE_HALF + WALK:
            put(-MAIN_HALF - WALK, G, z, 'minecraft:sea_lantern')
            put(MAIN_HALF + WALK, G, z, 'minecraft:sea_lantern')
    flush()


# ------------------------------------------------------------------------------------------
# The signals. Far-side mast arms: each approach's heads hang over its lanes beyond the junction,
# facing the oncoming traffic, from a pole on that corner.

HEAD = 'controllableverticalsolidsignal'
QUEUE_JUMP = 'controllableverticalqueuejumpaddonsignal'
DETECTOR = 'preempt_detector'
BEACON = 'tlpreemptbeacon'
POLE_V, POLE_H = 'trafficpolevertical', 'trafficpolehorizontal'

PX, PZ = MAIN_HALF + 2, AVE_HALF + 2      # the corner poles' offsets from the centre


def mast(pole, cells, arm_facing):
    """A pole at `pole` (x, z) up to the arm, and the arm over `cells`."""
    px, pz = pole
    for y in range(Y, ARM):
        put(px, y, pz, POLE_V, UP)
    put(px, ARM, pz, POLE_V, UP)
    for x, z in cells:
        put(x, ARM, z, POLE_H, arm_facing)


def signals():
    heads, dets, jumps, beacons = {}, {}, {}, {}
    # westbound (from the east): NW pole, arm running south over the westbound lanes
    mast((-PX, -PZ), [(-PX, z) for z in range(-PZ + 1, 0)], F_SOUTH)
    heads['wb'] = [(-PX, -4), (-PX, -1)]
    put(-PX, ARM - 1, -4, HEAD, E * 4)
    put(-PX, ARM - 1, -1, HEAD, E * 4)
    put(-PX, ARM - 2, -4, QUEUE_JUMP, E * 4)
    jumps['wb'] = (-PX, ARM - 2, -4)
    put(-PX, ARM + 1, -2, DETECTOR, E)              # on top of the arm, looking up the approach
    dets['wb'] = (-PX, ARM + 1, -2)
    # its confirmation beacon on the mast pole under the arm, its bracket clipped to the pole
    put(-PX, ARM - 1, -PZ + 1, BEACON, F_EAST)
    beacons['wb'] = (-PX, ARM - 1, -PZ + 1)
    put(-PX, ARM + 1, -4, 'preempt_confirm_par_white', F_EAST)   # and a PAR lamp by the detector
    beacons['wb_par'] = (-PX, ARM + 1, -4)
    # eastbound (from the west): SE pole, arm running north over the eastbound lanes
    mast((PX, PZ), [(PX, z) for z in range(PZ - 1, 0, -1)], F_NORTH)
    heads['eb'] = [(PX, 4), (PX, 1)]
    put(PX, ARM - 1, 4, HEAD, W * 4)
    put(PX, ARM - 1, 1, HEAD, W * 4)
    put(PX, ARM - 2, 4, QUEUE_JUMP, W * 4)
    jumps['eb'] = (PX, ARM - 2, 4)
    put(PX, ARM + 1, 2, DETECTOR, W)
    dets['eb'] = (PX, ARM + 1, 2)
    put(PX, ARM - 1, PZ - 1, BEACON, F_WEST)
    beacons['eb'] = (PX, ARM - 1, PZ - 1)
    put(PX, ARM + 1, 4, 'preempt_confirm_dome_red', F_WEST)      # and a 360 degree dome
    beacons['eb_dome'] = (PX, ARM + 1, 4)
    # southbound (from the north): SW pole, arm running east over the southbound lane
    mast((-PX, PZ), [(x, PZ) for x in range(-PX + 1, 0)], F_EAST)
    heads['sb'] = [(-2, PZ)]
    put(-2, ARM - 1, PZ, HEAD, N * 4)
    put(-1, ARM + 1, PZ, DETECTOR, N)
    dets['sb'] = (-1, ARM + 1, PZ)
    put(-PX + 1, ARM - 1, PZ, BEACON, F_NORTH)
    beacons['sb'] = (-PX + 1, ARM - 1, PZ)
    # northbound (from the south): NE pole, arm running west over the northbound lane
    mast((PX, -PZ), [(x, -PZ) for x in range(PX - 1, 0, -1)], F_WEST)
    heads['nb'] = [(2, -PZ)]
    put(2, ARM - 1, -PZ, HEAD, S * 4)
    put(1, ARM + 1, -PZ, DETECTOR, S)
    dets['nb'] = (1, ARM + 1, -PZ)
    put(PX - 1, ARM - 1, -PZ, BEACON, F_SOUTH)
    beacons['nb'] = (PX - 1, ARM - 1, -PZ)

    def longs(cells, y=ARM - 1):
        return ','.join('%dL' % L(x, y, z) for x, z in cells)

    def at(p):
        return '%dL' % L(*p)

    # Phase 2 = Main St (circuit 0), phase 4 = Transit Ave (circuit 1), both on max recall so the
    # junction cycles with nobody there. Each circuit's detectors call its preempt; circuit 1's,
    # seeing a bus, call transit priority for phase 4 with a 5 s queue jump.
    ph = []
    for n in range(1, 9):
        ci = {2: 0, 4: 1}.get(n, -1)
        xg = {2: 500, 4: 400}.get(n, 400)
        ph.append('{n:%d,rg:%d,ba:%d,ci:%d,mv:0,en:%s,rm:%d,mg:160L,ye:80L,rc:40L,xg:%dL,pa:40L}'
                  % (n, 1 if n <= 4 else 2, 1 if n in (1, 2, 5, 6) else 2, ci,
                     '1b' if ci >= 0 else '0b', 2 if ci >= 0 else 0, xg))
    pe = ('{en:1b,ty:1,tc:0,tm:0,td:1b,dw:[I;2],tk:[I;],ex:[I;],md:200L,sc:[I;]},'
          '{en:1b,ty:1,tc:1,tm:0,td:1b,dw:[I;4],tk:[I;],ex:[I;],md:200L,sc:[I;]}')
    pri = '{en:1b,tc:1,tm:0,ph:4,ex:200L,er:200L,mc:0,td:1b,qj:100L}'
    crc = ('{"0":{th:[L;%s],se:[L;%s,%s],pib:[L;%s,%s]},'
           '"1":{th:[L;%s],se:[L;%s,%s],qjs:[L;%s,%s],pib:[L;%s,%s,%s,%s]}}'
           % (longs(heads['sb'] + heads['nb']), at(dets['sb']), at(dets['nb']),
              at(beacons['sb']), at(beacons['nb']),
              longs(heads['wb'] + heads['eb']), at(dets['wb']), at(dets['eb']),
              at(jumps['wb']), at(jumps['eb']), at(beacons['wb']), at(beacons['eb']),
              at(beacons['wb_par']), at(beacons['eb_dome'])))
    adv = ('{ph:[%s],r1:[I;1,2,3,4],r2:[I;5,6,7,8],co:{md:0},pe:[%s],pri:%s}'
           % (','.join(ph), pe, pri))
    cx, cz = PX + 3, PZ + 3
    put(cx, Y, cz, 'signalcontroller', 0, '{tcMode:9,tcOm:9,tcCrc:%s,tcAdv:%s}' % (crc, adv))
    put(cx + 1, Y, cz, 'minecraft:redstone_block')
    sign(cx, cz + 1, ["Signal controller", "ADVANCED mode", "Right-click: ASC-3", "TSP: E-W"], 0)
    flush()


def bus_stop():
    """A CITYLINE stop on the westbound curb east of the junction, and its shelter."""
    x, z = 26, -AVE_HALF - 1
    put(x, Y, z, 'signpost', E)
    put(x, Y + 1, z, 'signpost', E)
    put(x, Y + 2, z, 'bus_stop_flag_cityline', E, '{r:[B;1b,7b,0b]}')
    for sx in range(x + 2, x + 5):
        put(sx, Y, -AVE_HALF - WALK, 'bus_shelter_glass_cityline', S)
        put(sx, Y + 1, -AVE_HALF - WALK, 'bus_shelter_glass_cityline', S | 4 | 8)
    flush()


def info_signs():
    sign(-PX - 2, -PZ - 2, ["Main St", "x", "Transit Ave"], 8)
    sign(12, PZ + 1, ["Turn on EMERLTS", "and drive at the", "signal: it clears", "and holds green"], 0)
    sign(14, PZ + 1, ["Metro bus: TSP", "is on. Drive it", "west in the curb", "lane: queue jump"], 0)
    sign(16, PZ + 1, ["Detectors see", "120 blocks up", "their approach,", "20 deg either side"], 0)
    sign(60, -AVE_HALF - WALK, ["Bus stop", "Drive the bus", "west from here"], 0)
    flush()


# ------------------------------------------------------------------------------------------
# The parking lot

LOT_X0, LOT_X1 = 14, 122
NOSE_A, NOSE_B = 14, 52           # the north row's nose line, and the south row's
LOT_Z0, LOT_Z1 = AVE_HALF + WALK + 1, NOSE_B + 1
BAY = 5
CENTRE0 = LOT_X0 + 4              # the first bay's centre column

ROW_A = (['csm_fire_engine_' + s for s in ('red', 'lime', 'blackred', 'white', 'airport')]
         + ['csm_ladder_truck_' + s for s in ('red', 'lime', 'blackred', 'white')]
         + ['csm_ambulance_' + s for s in ('red', 'orange', 'blue', 'green', 'yellow')]
         + ['csm_police_suv_' + s for s in ('blackwhite', 'white', 'blue', 'sheriff', 'state',
                                            'unmarked')])
ROW_B = (['csm_transit_bus_' + s for s in ('metro', 'cityline', 'riverway', 'verdant',
                                           'emberline')]
         + ['csm_tow_truck_' + s for s in ('white', 'red', 'yellow', 'black')]
         + ['csm_dpw_truck_' + s for s in ('orange', 'yellow', 'white')]
         + ['csm_bucket_truck_' + s for s in ('white', 'yellow', 'green')]
         + ['csm_chipper_trailer_orange', 'csm_equipment_trailer_black',
            'csm_stump_grinder_orange'])

PARTS = ['csm_lightbar_led_redblue', 'csm_lightbar_led_red', 'csm_lightbar_led_redwhite',
         'csm_lightbar_led_amber', 'csm_preempt_emitter', 'csm_siren_speaker',
         'csm_wheel_truck', 'csm_wheel_van', 'csm_wheel_car', 'csm_engine_diesel',
         'csm_engine_petrol', 'csm_vehicle_seat', 'csm_chipbed']


def extent(model):
    """The vehicle model's (rear, front) along its forward axis, from its OBJ."""
    zs = [float(l.split()[3]) for l in open(os.path.join(OBJ_DIR, model + '.obj'))
          if l.startswith('v ')]
    return min(zs), max(zs)


def model_of(item):
    for m in ('fire_engine', 'ladder_truck', 'ambulance', 'police_suv', 'transit_bus',
              'tow_truck', 'dpw_truck', 'bucket_truck', 'chipper_trailer',
              'equipment_trailer', 'stump_grinder'):
        if item.startswith('csm_' + m):
            return 'csm_' + m
    raise ValueError(item)


def lot():
    fill(LOT_X0, G, LOT_Z0, LOT_X1, G, LOT_Z1, 'minecraft:concrete', 7)
    fill(LOT_X0, G, AVE_HALF + 1, LOT_X0 + 6, G, LOT_Z0, 'minecraft:concrete', 7)  # driveway
    # bay lines, each row to its deepest vehicle
    depth_a = max(extent(model_of(i))[1] - extent(model_of(i))[0] for i in ROW_A)
    depth_b = max(extent(model_of(i))[1] - extent(model_of(i))[0] for i in ROW_B)
    for k in range(max(len(ROW_A), len(ROW_B)) + 1):
        x = CENTRE0 - 3 + k * BAY
        if k <= len(ROW_A):
            fill(x, G, NOSE_A, x, G, int(NOSE_A + depth_a), 'minecraft:concrete', 0)
        if k <= len(ROW_B):
            fill(x, G, int(NOSE_B - depth_b), x, G, NOSE_B, 'minecraft:concrete', 0)
    # curb stops along both nose lines, lanterns down the aisle
    fill(LOT_X0 + 7, G + 1, NOSE_A - 2, LOT_X1, G + 1, NOSE_A - 2, 'minecraft:stone_slab', 0)
    fill(LOT_X0, G + 1, NOSE_B + 2, LOT_X1, G + 1, NOSE_B + 2, 'minecraft:stone_slab', 0)
    aisle = (int(NOSE_A + depth_a) + int(NOSE_B - depth_b)) // 2
    for x in range(LOT_X0 + 4, LOT_X1, 8):
        put(x, G, aisle, 'minecraft:sea_lantern')
    # the parts chest by the entrance
    items = ','.join('{Slot:%db,id:"mts:csmvehicles.%s",Count:%db}' % (i, p, 4 if 'wheel' in p
                                                                           else 1)
                     for i, p in enumerate(PARTS))
    put(LOT_X0 + 8, Y, NOSE_A - 4, 'minecraft:chest', 3, '{Items:[%s]}' % items)
    sign(LOT_X0 + 9, NOSE_A - 4, ["Parts: lightbars,", "emitter, siren,", "wheels, engines,",
                                  "chip bed"], 0)
    sign(LOT_X0 + 2, NOSE_A + 2, ["CSM: Vehicles", "Emergency fleet", "(north row)"], 12)
    sign(LOT_X0 + 2, NOSE_B - 2, ["CSM: Vehicles", "Buses and work", "trucks (south row)"], 12)
    flush()


def place_vehicle(item, x, z, player_facing):
    """Places `item` with its origin on (x, z), the placer facing `player_facing` (E or W)."""
    cmd('/clear %s' % PLAYER)
    cmd('/give %s mts:csmvehicles.%s' % (PLAYER, item))
    client.call('client_select_slot', slot=0)
    dx = -3 if player_facing == E else 3
    cmd('/tp %s %.1f %d %.1f' % (PLAYER, X0 + x + 0.5 + dx, Y, Z0 + z + 0.5))
    # Wait for the client to hold the ground there: teleported into chunks it has not loaded
    # yet, the player falls through them, and the click then aims at nothing.
    client.call('client_wait', waitFor='chunksLoaded', radius=16, ticks=200)
    time.sleep(0.3)
    client.call('client_look', lookAtX=X0 + x + 0.5, lookAtY=Y, lookAtZ=Z0 + z + 0.5)
    client.call('client_interact', action='use')
    time.sleep(0.4)


def vehicles():
    client.call('client_input_lock', locked=True, reason='parking the CSM: Vehicles fleet',
                seconds=600)
    try:
        # north row noses north: the placer faces east, so fill from the east end westward
        for k in reversed(range(len(ROW_A))):
            rear, front = extent(model_of(ROW_A[k]))
            place_vehicle(ROW_A[k], CENTRE0 + k * BAY, round(NOSE_A + front), E)
        # south row noses south: the placer faces west, so fill from the west end eastward
        for k in range(len(ROW_B)):
            rear, front = extent(model_of(ROW_B[k]))
            place_vehicle(ROW_B[k], CENTRE0 + k * BAY, round(NOSE_B - front), W)
    finally:
        client.call('client_input_lock', locked=False)
    cmd('/clear %s' % PLAYER)
    cmd('/tp %s %d %d %d' % (PLAYER, X0 + 10, Y + 1, Z0 + AVE_HALF + WALK + 2))


if __name__ == '__main__':
    for c in ('/gamerule doDaylightCycle false', '/time set 6000', '/weather clear 1000000',
              '/gamerule doMobSpawning false', '/difficulty peaceful'):
        cmd(c)
    if ARGS.only in (None, 'roads'):
        roads()
        signals()
        bus_stop()
        info_signs()
    if ARGS.only in (None, 'lot'):
        lot()
    if ARGS.only in (None, 'vehicles'):
        vehicles()
    print('built at', X0, Z0)
