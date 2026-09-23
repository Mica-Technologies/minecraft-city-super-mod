#!/usr/bin/env python3
"""
build_life_safety_demo.py -- builds the Life Safety demo world, over MCMCP.

Load a fresh FLAT creative world in the dev client (Core + Life Safety is enough; nothing here
needs another module), then run this. It builds a street of emergency services with everything
the module's Fire Protection, Exits & Emergency Lighting and Emergency Services tabs ship, and
the fire alarm devices wired to a panel, all linked through their saved data so it works on
arrival:

  fire station   apparatus bays (gear lockers, SCBA, hose, tools, extractor, compressor), a
                 mezzanine with the fire pole down through its floor opening, the watch room
                 (station alerting: controller, speakers, alert lights, a relay that strikes the
                 gong and lights a lamp, bay clearance lights), a fire alarm (panel, pull
                 station, horn strobe, smoke detector, sprinklers, a magnetic door holder on the
                 office door, the remote annunciator by the front door), the riser room, and
                 outside the fire department connection, water motor gong, post indicator valve
                 and backflow preventer
  police station the metal detector at the door, the front desk, holding cells with sliding
                 doors, booking, evidence and equipment lockers, the K-9 kennel, blue lamps, and
                 a taped-off scene on the street
  EMS station    stretchers, stair chair, backboards, supplies, oxygen, the medication safe,
                 decon sink and eyewash, first aid and AED cabinets
  community      a rotating and an electronic warning siren on poles, the dispatch office with
                 the siren controller, a blue-light call box and the shelter signs

Try: sneak-click the siren controller (it starts on Attack); sneak-click the station alerting
controller in the watch room to dispatch; pull the station by the office door to see the door
shut and the annunciator light; slide down the fire pole; walk through the metal detector
carrying a sword. The bays and the police building have no roof, so they show from above;
the watch room and office have a ceiling for their sprinklers, smoke detector and downlight.

The site is placed relative to the player (--x/--z to move it). World rules are set for a demo:
peaceful (a flat world spawns slimes), no daylight cycle, clear weather.

Usage:
    python build_life_safety_demo.py [--x X --z Z]
"""
import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_bench as B  # noqa: E402 -- its MCMCP client and token reader


class _Session:
    def __init__(self, url):
        self.mcp = B.Mcp(url, B.read_token()).connect()

    def call(self, name, **args):
        out = self.mcp.call(name, args)
        return out if isinstance(out, str) else json.dumps(out)


ap = argparse.ArgumentParser(description="Build the Life Safety demo world.")
ap.add_argument("--x", type=int, default=None, help="west end of the street (default: player)")
ap.add_argument("--z", type=int, default=None, help="north edge of the site (default: player)")
ARGS = ap.parse_args()
client = _Session(B.CLIENT_URL)
server = _Session(B.SERVER_URL)

_me = json.loads(client.call('client_player_state'))
X0 = ARGS.x if ARGS.x is not None else int(_me['position']['x']) - 10
Z0 = ARGS.z if ARGS.z is not None else int(_me['position']['z']) - 30
Y = 4           # standing height on a default flat world
G = 3           # ground

blocks = []

# NSEW props: horizontal index. NSEWUD fire alarm devices: EnumFacing index.
S, W, N, E = 0, 1, 2, 3
UD = {'down': 0, 'up': 1, N: 2, S: 3, W: 4, E: 5}


def put(x, y, z, block, meta=0, nbt=None):
    b = {"x": x, "y": y, "z": z, "block": block if ':' in block else 'csm:' + block,
         "metadata": meta}
    if nbt is not None:
        b["nbt"] = nbt
    blocks.append(b)


def fill(x0, y0, z0, x1, y1, z1, block, meta=0):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                put(x, y, z, block, meta)


def flush():
    global blocks
    for i in range(0, len(blocks), 900):
        server.call('server_set_blocks', mode='list', blocks=blocks[i:i + 900])
    blocks = []


def sign(x, z, lines, rot=0, y=Y):
    nbt = {}
    for i, text in enumerate(lines[:4]):
        nbt['Text%d' % (i + 1)] = json.dumps({"text": text})
    snbt = '{' + ','.join('%s:%s' % (k, json.dumps(v)) for k, v in nbt.items()) + '}'
    put(x, y, z, 'minecraft:standing_sign', rot, snbt)


def packed(positions):
    """Block positions as the controllers store them: each BlockPos.toLong split into two ints."""
    out = []
    for x, y, z in positions:
        v = ((x & ((1 << 26) - 1)) << 38) | ((y & ((1 << 12) - 1)) << 26) | (z & ((1 << 26) - 1))
        for part in ((v >> 32) & 0xFFFFFFFF, v & 0xFFFFFFFF):
            out.append(part - (1 << 32) if part >= 1 << 31 else part)
    return '[I;' + ','.join(str(p) for p in out) + ']'


def lp(x, y, z):
    """A fire alarm device's link to its panel."""
    return '{lp:[I;%d,%d,%d]}' % (x, y, z)


def building(x0, z0, x1, z1, top, floor='minecraft:concrete', fmeta=8, wall_meta=0):
    """Walls round a footprint from the ground up to top, and a floor at ground level."""
    fill(x0, G, z0, x1, G, z1, floor, fmeta)
    for y in range(Y, top + 1):
        for x in range(x0, x1 + 1):
            put(x, y, z0, 'minecraft:concrete', wall_meta)
            put(x, y, z1, 'minecraft:concrete', wall_meta)
        for z in range(z0 + 1, z1):
            put(x0, y, z, 'minecraft:concrete', wall_meta)
            put(x1, y, z, 'minecraft:concrete', wall_meta)


def opening(x0, x1, y0, y1, z, block='minecraft:air'):
    fill(x0, y0, z, x1, y1, z, block)


# ------------------------------------------------------------------------------------------
# World rules and the street
# ------------------------------------------------------------------------------------------
for cmd in ('gamerule doDaylightCycle false', 'gamerule doWeatherCycle false',
            'gamerule doMobSpawning false', 'difficulty peaceful', 'time set 6000',
            'weather clear 1000000', 'kill @e[type=!player]'):
    server.call('server_run_command', command=cmd)

XE = X0 + 104
fill(X0 - 4, G, Z0 + 18, XE, G, Z0 + 20, 'minecraft:concrete', 8)     # sidewalk
fill(X0 - 4, G, Z0 + 21, XE, G, Z0 + 28, 'minecraft:concrete', 15)    # road
for x in range(X0 - 4, XE + 1):
    if x % 4 < 2:
        put(x, G, Z0 + 24, 'minecraft:concrete', 4)
fill(X0 - 4, G, Z0 + 29, XE, G, Z0 + 30, 'minecraft:concrete', 8)
flush()

# ------------------------------------------------------------------------------------------
# Fire station: x X0..X0+23, z Z0..Z0+17; bays west of x+13, watch room and office east
# ------------------------------------------------------------------------------------------
FX = X0
building(FX, Z0, FX + 23, Z0 + 17, 9, wall_meta=14)           # red brick-ish concrete
fill(FX + 1, G, Z0 + 1, FX + 12, G, Z0 + 16, 'minecraft:concrete', 7)
for x0 in (FX + 2, FX + 7):                                  # two bay openings
    opening(x0, x0 + 3, Y, Y + 3, Z0 + 17)
for y in range(Y, 9):                                         # partition: bays | watch room
    for z in range(Z0 + 1, Z0 + 17):
        put(FX + 13, y, z, 'minecraft:concrete', 0)
# the office door, held open by a magnetic door holder above it
put(FX + 13, Y, Z0 + 8, 'minecraft:wooden_door', 0)
put(FX + 13, Y + 1, Z0 + 8, 'minecraft:wooden_door', 8)
PANEL = (FX + 17, 5, Z0 + 1)
put(FX + 13, Y + 2, Z0 + 8, 'magnetic_door_holder', E, lp(*PANEL))
# the front door of the office
opening(FX + 18, FX + 18, Y, Y + 1, Z0 + 17)
flush()

# bays: gear along the back wall, workshop along the west wall
for i in range(4):
    put(FX + 1 + i, Y, Z0 + 1, 'turnout_gear_locker', S)
put(FX + 5, Y, Z0 + 1, 'turnout_gear_locker_empty', S)
put(FX + 6, Y + 1, Z0 + 1, 'scba_wall_rack', S)
put(FX + 7, Y, Z0 + 1, 'scba_cylinder_cascade', S)
put(FX + 8, Y, Z0 + 1, 'scba_fill_station', S)
put(FX + 9, Y + 1, Z0 + 1, 'hose_rack_wall', S)
put(FX + 10, Y + 1, Z0 + 1, 'fire_tool_board', S)
put(FX + 1, Y, Z0 + 6, 'gear_extractor', E)
put(FX + 1, Y, Z0 + 8, 'air_compressor', E)
put(FX + 1, Y, Z0 + 10, 'hose_drying_rack', E)
put(FX + 1, Y, Z0 + 12, 'hose_rolls', E)
put(FX + 1, Y + 1, Z0 + 14, 'nozzle_rack', E)
put(FX + 1, Y + 1, Z0 + 4, 'extinguisher_cabinet_glass', E)
put(FX + 12, Y + 1, Z0 + 12, 'fire_extinguisher_abc', W)
put(FX + 12, Y + 1, Z0 + 10, 'extinguisher_sign', W)
# the mezzanine over the back of the bays, with the pole down through its opening
fill(FX + 1, 9, Z0 + 1, FX + 12, 9, Z0 + 5, 'minecraft:concrete', 7)
put(FX + 11, 9, Z0 + 4, 'fire_pole_hole')
for y in range(Y, 9):
    put(FX + 11, y, Z0 + 4, 'fire_pole')
put(FX + 3, 10, Z0 + 1, 'turnout_gear_locker', S)
put(FX + 4, 10, Z0 + 1, 'turnout_gear_locker', S)
# the watch room and office have a ceiling (the bays stay open to the sky, to be seen from above)
fill(FX + 14, 9, Z0 + 1, FX + 22, 9, Z0 + 16, 'minecraft:concrete', 0)
# sprinklers under the mezzanine and in the office, a smoke detector in the watch room
for x, z in ((FX + 3, Z0 + 3), (FX + 8, Z0 + 3), (FX + 16, Z0 + 12), (FX + 20, Z0 + 12)):
    put(x, 8, z, 'firealarmsprinklersilver', UD['down'], lp(*PANEL))
put(FX + 18, 8, Z0 + 8, 'smoke_detector_photoelectric', UD['down'], lp(*PANEL))
# watch room: fire alarm panel, station alerting, AED
HORN = (FX + 20, 7, Z0 + 1)
GONG = (FX + 24, 7, Z0 + 13)
put(*PANEL, block='firealarmcontrolpanel', meta=S,
    # SNBT has no \n escape: the panel keeps one position per line, so a real line feed in the
    # SNBT string, which the dict form would have escaped.
    nbt='{apps:"%d %d %d\n%d %d %d"}' % (HORN + GONG))
put(*HORN, block='firealarmsystemsensorlseriesledhornstrobered', meta=UD[S])
put(FX + 14, 5, Z0 + 16, 'firealarmgenericpullstation', UD[N], lp(*PANEL))
ALERT = (FX + 15, 5, Z0 + 1)
devices = [(FX + 6, 8, Z0 + 16), (FX + 16, 7, Z0 + 1),               # speakers
           (FX + 3, 8, Z0 + 16), (FX + 9, 8, Z0 + 16),              # red alert lights
           (FX + 21, 6, Z0 + 1),                                     # relay
           (FX + 1, 6, Z0 + 16), (FX + 12, 6, Z0 + 16)]              # bay clearance lights
put(*ALERT, block='station_alert_controller', meta=S,
    nbt='{zn:0,t:-1,d:%s}' % packed(devices))
put(*devices[0], block='station_alert_speaker', meta=N)
put(*devices[1], block='station_alert_speaker', meta=S)
put(*devices[2], block='station_alert_light_red', meta=N)
put(*devices[3], block='station_alert_light_red', meta=N)
put(*devices[4], block='station_alert_relay', meta=S)
put(FX + 22, 6, Z0 + 1, 'station_alarm_gong', S)                     # struck by the relay
put(FX + 21, 5, Z0 + 1, 'minecraft:redstone_lamp')                   # lit by the relay
put(*devices[5], block='bay_clearance_light', meta=N)
put(*devices[6], block='bay_clearance_light', meta=N)
put(FX + 22, 5, Z0 + 4, 'aed_cabinet', W)
put(FX + 22, 7, Z0 + 4, 'aed_sign', W)
put(FX + 22, 5, Z0 + 7, 'first_aid_cabinet', W)
# the riser room corner of the office
put(FX + 22, Y, Z0 + 11, 'sprinkler_alarm_valve', W)
put(FX + 22, Y, Z0 + 13, 'osy_gate_valve', W)
put(FX + 22, 6, Z0 + 12, 'riser_sign', W)
put(FX + 21, Y, Z0 + 15, 'standpipe_riser', W)
put(FX + 21, Y + 1, Z0 + 15, 'standpipe_riser', W)
put(FX + 20, 5, Z0 + 16, 'fire_hose_cabinet', N)
# exits and emergency lights over the office front door
put(FX + 18, 6, Z0 + 16, 'mclacodeapprovedexitsign', UD[N])
put(FX + 16, 7, Z0 + 16, 'emergency_light_twin_head_white', UD[N])
put(FX + 17, 8, Z0 + 5, 'emergency_downlight_recessed', UD['down'])
flush()

# the facade and the yard
put(FX + 6, 7, Z0 + 18, 'station_number_plaque', S, '{n:7}')
put(FX + 11, 7, Z0 + 18, 'maltese_cross_emblem', S)
put(FX + 19, 5, Z0 + 18, 'remote_annunciator', S, lp(*PANEL))
put(FX + 17, 6, Z0 + 18, 'knox_box', S)
put(FX + 24, 5, Z0 + 13, 'fdc_siamese_brass', E)
put(FX + 24, 6, Z0 + 11, 'fdc_sign', E)
put(*GONG, block='water_motor_gong', meta=UD[E])
put(FX + 26, Y, Z0 + 15, 'post_indicator_valve', W)
put(FX + 26, Y, Z0 + 9, 'fire_backflow_preventer', W)
put(FX + 26, Y, Z0 + 17, 'fdc_freestanding', S)
sign(FX + 5, Z0 + 19, ["Fire Station", "Sneak-click the", "controller in the", "watch room"])
sign(FX + 16, Z0 + 19, ["Pull the station", "inside the door:", "the office door", "shuts"])
flush()

# ------------------------------------------------------------------------------------------
# Police station: x X0+30..X0+48
# ------------------------------------------------------------------------------------------
PX = X0 + 30
building(PX, Z0, PX + 18, Z0 + 17, 8, wall_meta=11)
opening(PX + 9, PX + 9, Y, Y + 1, Z0 + 17)
put(PX + 9, Y, Z0 + 16, 'metal_detector', S)
for x in list(range(PX + 3, PX + 9)) + list(range(PX + 10, PX + 15)):
    put(x, Y, Z0 + 11, 'front_desk_counter', S)
put(PX + 6, Y + 1, Z0 + 11, 'pass_through_tray', S)
put(PX + 12, Y + 1, Z0 + 11, 'fingerprint_scanner', S)
put(PX + 1, Y + 1, Z0 + 15, 'lobby_phone', E)
put(PX + 17, Y + 1, Z0 + 15, 'extinguisher_cabinet_steel', W)
# holding cells along the back wall
for x in (PX + 5, PX + 9):
    for y in range(Y, 8):
        for z in range(Z0 + 1, Z0 + 6):
            put(x, y, z, 'minecraft:concrete', 0)
for x in range(PX + 1, PX + 13):
    if x in (PX + 5, PX + 9):
        continue
    for y in range(Y, 8):
        put(x, y, Z0 + 6, 'minecraft:iron_bars')
for x in (PX + 3, PX + 7, PX + 11):
    put(x, Y, Z0 + 6, 'holding_cell_door', S)
    put(x, Y + 1, Z0 + 6, 'holding_cell_door', S)
    put(x - 1, Y, Z0 + 1, 'holding_cell_toilet', S)
    put(x + 1, Y + 1, Z0 + 1, 'holding_cell_bench', S)
# booking and storage along the east wall
put(PX + 17, Y, Z0 + 2, 'height_chart', W)
put(PX + 17, Y + 1, Z0 + 2, 'height_chart', W)
put(PX + 15, Y, Z0 + 2, 'booking_camera', E)
put(PX + 17, Y, Z0 + 5, 'property_bins', W)
put(PX + 17, Y, Z0 + 7, 'evidence_locker', W)
put(PX + 17, Y, Z0 + 8, 'evidence_locker', W)
put(PX + 13, Y, Z0 + 1, 'equipment_locker', S)
put(PX + 14, Y, Z0 + 1, 'equipment_locker', S)
put(PX - 2, Y, Z0 + 4, 'k9_kennel', E)
put(PX + 7, 6, Z0 + 18, 'police_lamp', S)
put(PX + 11, 6, Z0 + 18, 'police_lamp', S)
put(PX + 9, 7, Z0 + 18, 'police_star_emblem', S)
# a taped-off scene on the road
put(PX + 3, Y, Z0 + 23, 'tape_stanchion')
for x in range(PX + 4, PX + 13):
    put(x, Y, Z0 + 23, 'police_line_tape')
put(PX + 13, Y, Z0 + 23, 'tape_stanchion')
for z in range(Z0 + 24, Z0 + 27):
    put(PX + 13, Y, z, 'police_line_tape')
put(PX + 13, Y, Z0 + 27, 'tape_stanchion')
sign(PX + 7, Z0 + 19, ["Police Station", "Walk through the", "metal detector", "with a sword"])
flush()

# ------------------------------------------------------------------------------------------
# EMS station: x X0+54..X0+70
# ------------------------------------------------------------------------------------------
EX = X0 + 54
building(EX, Z0, EX + 16, Z0 + 17, 8, wall_meta=0)
opening(EX + 3, EX + 8, Y, Y + 3, Z0 + 17)
put(EX + 3, Y, Z0 + 9, 'ems_stretcher', E)
put(EX + 6, Y, Z0 + 9, 'ems_stretcher_lowered', E)
put(EX + 1, Y, Z0 + 3, 'ems_stair_chair', E)
put(EX + 1, Y, Z0 + 5, 'ems_backboard_rack', E)
for x in range(EX + 3, EX + 8):
    put(x, Y, Z0 + 1, 'ems_supply_shelving', S)
put(EX + 9, Y, Z0 + 1, 'oxygen_cylinder_rack', S)
put(EX + 10, Y + 1, Z0 + 1, 'medication_safe', S)
put(EX + 15, Y, Z0 + 4, 'decon_sink', W)
put(EX + 15, Y + 1, Z0 + 6, 'eyewash_station', W)
put(EX + 15, Y + 1, Z0 + 9, 'first_aid_cabinet', W)
put(EX + 15, Y + 1, Z0 + 11, 'aed_cabinet', W)
put(EX + 11, 7, Z0 + 18, 'star_of_life_emblem', S)
sign(EX + 5, Z0 + 19, ["EMS Station", "Take a first aid", "kit from the", "cabinet"])
flush()

# ------------------------------------------------------------------------------------------
# Community warning and dispatch: x X0+76..
# ------------------------------------------------------------------------------------------
CX = X0 + 76
SIREN_R = (CX, 10, Z0 + 4)
SIREN_E = (CX + 20, 10, Z0 + 4)
for sx, _, sz in (SIREN_R, SIREN_E):
    for y in range(Y, 10):
        put(sx, y, sz, 'minecraft:concrete', 7)
put(*SIREN_R, block='warning_siren_rotating', meta=S)
put(*SIREN_E, block='warning_siren_electronic', meta=S)
DX = CX + 6
building(DX, Z0 + 6, DX + 10, Z0 + 17, 8, wall_meta=9)
opening(DX + 5, DX + 5, Y, Y + 1, Z0 + 17)
put(DX + 1, Y + 1, Z0 + 12, 'warning_siren_controller', E,
    '{c:1,w:1b,lt:-1L,s:%s}' % packed([SIREN_R, SIREN_E]))
put(DX + 3, Y, Z0 + 7, 'dispatch_console', S)
put(DX + 5, Y, Z0 + 7, 'dispatch_console', S)
put(DX + 7, Y, Z0 + 7, 'dispatch_console', S)
put(DX + 9, Y, Z0 + 12, 'radio_console_speaker', W)
put(DX + 9, Y + 1, Z0 + 15, 'fallout_shelter_sign', W)
put(DX + 3, 6, Z0 + 18, 'storm_shelter_sign', S)
put(DX + 7, 6, Z0 + 18, 'assembly_point_sign', S)
put(CX + 3, Y, Z0 + 19, 'blue_light_call_box', S)
sign(DX + 5, Z0 + 20, ["Dispatch", "Sneak-click the", "siren controller", "(Attack)"], y=Y)
sign(CX + 1, Z0 + 20, ["Warning sirens", "and a blue-light", "call box"], y=Y)
flush()

server.call('server_teleport_player', player=_me['name'], x=X0 + 16, y=Y + 6, z=Z0 + 40,
            fly=True)
client.call('client_look', yaw=180, pitch=15)
print("built the Life Safety demo at x %d..%d, z %d..%d" % (X0 - 4, XE, Z0, Z0 + 30))
