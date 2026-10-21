#!/usr/bin/env python3
"""
build_technology_demo.py -- builds the Technology demo world, over the MCMCP orchestrator.

Load a fresh FLAT creative world named "Technology Demo" in the dev client (every module: the
rooftop radios hang on Roads' traffic poles, and the office chairs are Furnishings') and run this
with the client's instance id. It builds an office building along the x axis, its roof and a
second, smaller building to the north that the roof's radios point at:

  office    three rows of desks, each workstation two blocks wide and two deep (a full block
            under every device: D8, a device sits at the bottom of its block), carrying the
            Computers & Peripherals tab's rigs, monitors (their screens set to desktop, game,
            code and idle by metadata), keyboards, mice, pads and desk extras; a gaming corner
            with the four consoles on a media unit facing a sofa of task chairs; ceiling access
            points, wall jacks and a floor box
  server    two rows of fillable 42U cabinets (glass and perforated doors) facing each other
            across a cold aisle, filled with rack-mount units through their tile entity's
            ``u`` byte array (TECHNOLOGY_SYSTEM.md, The rack), an open frame, two rows of the
            pre-filled racks behind them, a basket tray and a ladder tray hung over the rows
            with a cross tray making tees, and ceiling access points
  closet    the 12U wall cabinet, the desktop switches, gateway, Control Hub, modem and PoE
            injector on a bench, the wall and in-wall access points and the wall jacks
  roof      Roads traffic poles carrying both dishes, the sector antenna, the Station CPE, the
            outdoor mesh AP and one end of a Link 60 pair, and the Beam bridge on its wall arm
            on the parapet, all facing north at the second building, which carries the other
            Beam and the other Link 60

Finally it reads the two new Technology tabs' registration lists from the sources and puts every
block it did not place on a signed plinth south of the building (a block on a plinth is one this
script forgot), and prints the count. Rack-mount units and controllers are items, not placed.

Everything goes through the orchestrator by instance id; the script never opens a game port, and
it refuses any world not named "Technology Demo". It sets blocks and runs commands only.

Usage:
    python build_technology_demo.py [--instance run-f2eb58] [--x X --z Z]
"""
import argparse
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmcp_orchestrator import Orchestrator  # noqa: E402

ap = argparse.ArgumentParser(description='Build the Technology demo world.')
ap.add_argument('--instance', default='run-f2eb58', help="the dev game's id")
ap.add_argument('--x', type=int, default=None, help='site origin x (default: player)')
ap.add_argument('--z', type=int, default=None, help='site origin z (default: player)')
ARGS = ap.parse_args()
WORLD_NAME = 'Technology Demo'

O = Orchestrator()
CLIENT, SERVER = ARGS.instance + '.client', ARGS.instance + '.server'
_info = O.call('server_world_info', instance=SERVER)
if WORLD_NAME not in json.dumps(_info):
    sys.exit('refusing: the world on %s is not "%s"' % (SERVER, WORLD_NAME))
_me = O.call('client_player_state', instance=CLIENT)
X0 = ARGS.x if ARGS.x is not None else int(_me['position']['x'])
Z0 = ARGS.z if ARGS.z is not None else int(_me['position']['z'])

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
TABS = os.path.join(REPO, 'modules', 'technology', 'src', 'main', 'java', 'com',
                    'micatechnologies', 'minecraft', 'csm', 'tabs')
FURN_TABS = os.path.join(REPO, 'modules', 'furnishings', 'src', 'main', 'java', 'com',
                         'micatechnologies', 'minecraft', 'csm', 'tabs')

G = 3            # the flat world's ground: the floor slab
Y = 4            # the floor people stand on
CEIL = 9         # the ceiling (five clear blocks, y 4 to 8); the roof is walked on at y 10
ROOF = CEIL + 1
XMAX = 44        # the building's east wall
ZMAX = 24        # its south wall; the north wall is z 0

# Horizontal facing index, the way a device's front faces (AbstractBlockRotatableNSEW).
S, W, N, E = 0, 1, 2, 3
SCREENS = {'desktop': 0, 'game': 1, 'code': 2, 'idle': 3}
RING = {'blue': 0, 'white': 1, 'off': 2, 'adopting': 3}
LINK = {'green': 0, 'blue': 1, 'white': 2, 'off': 3}

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
    O.call('server_set_blocks', instance=SERVER, mode='fill', load=True,
           block=name_of(block), metadata=meta, x=X0 + xs[0], y=ys[0], z=Z0 + zs[0],
           toX=X0 + xs[1], toY=ys[1], toZ=Z0 + zs[1])


def cmd(c):
    flush()
    r = O.call('server_run_command', instance=SERVER, command=c)
    if isinstance(r, dict) and (r.get('error') or not r.get('succeeded', True)):
        print('  !', c, '->', r.get('error') or r.get('output'))
    return r


def _sign_nbt(lines):
    parts = ['Text%d:%s' % (i + 1, json.dumps(json.dumps({'text': t})))
             for i, t in enumerate(lines[:4])]
    return '{' + ','.join(parts) + '}'


def sign(x, y, z, lines, rot=0):
    """A standing sign; rot 0 faces south, 4 west, 8 north, 12 east."""
    put(x, y, z, 'minecraft:standing_sign', rot, _sign_nbt(lines))


def wall_sign(x, y, z, lines, out):
    """A wall sign; out 2 north, 3 south, 4 west, 5 east."""
    put(x, y, z, 'minecraft:wall_sign', out, _sign_nbt(lines))


# ------------------------------------------------------------------------------------------
# What the tabs register (and which desk devices have a screen)
# ------------------------------------------------------------------------------------------
def tab_blocks(path):
    out = []
    for line in open(path, encoding='utf-8'):
        m = re.search(r'initTabBlock\(new ([\w.]+)\("([a-z0-9_]+)"', line)
        if m:
            out.append((m.group(2), m.group(1)))
    return out


COMPUTERS = tab_blocks(os.path.join(TABS, 'CsmTabTechnologyComputers.java'))
NETWORKING = tab_blocks(os.path.join(TABS, 'CsmTabTechnologyNetworking.java'))
SCREEN_CLASS = {reg for reg, cls in COMPUTERS if cls == 'BlockDeskScreenFactory'}
REGISTERED = [r for r, _ in COMPUTERS + NETWORKING]


def furnishing(reg):
    """A Furnishings registry name if that module's tabs register it, else None."""
    for f in os.listdir(FURN_TABS):
        if ('"%s"' % reg) in open(os.path.join(FURN_TABS, f), encoding='utf-8').read():
            return reg
    return None


CHAIR = furnishing('task_chair_charcoal')
GAMING_CHAIR = furnishing('gaming_chair_red') or CHAIR


def device(reg, x, z, face, screen='desktop', y=Y + 1):
    """A desk device: bits 0-1 the facing; a screen's picture in bits 2-3."""
    meta = face | ((SCREENS[screen] << 2) if reg in SCREEN_CLASS else 0)
    put(x, y, z, reg, meta)


# ------------------------------------------------------------------------------------------
# The building
# ------------------------------------------------------------------------------------------
def shell():
    fill(-2, G, -2, XMAX + 2, G, ZMAX + 12, 'minecraft:concrete', 8)       # floor slab
    fill(0, Y, 0, XMAX, CEIL, ZMAX, 'minecraft:concrete', 0)               # solid box
    fill(1, Y, 1, XMAX - 1, CEIL - 1, ZMAX - 1, 'minecraft:air')            # hollowed
    fill(1, G, 1, 19, G, ZMAX - 1, 'minecraft:wool', 7)                    # office carpet
    fill(21, G, 1, 37, G, ZMAX - 1, 'minecraft:concrete', 0)               # raised floor
    fill(20, Y, 1, 20, CEIL - 1, ZMAX - 1, 'minecraft:concrete', 0)        # office | server
    fill(38, Y, 1, 38, CEIL - 1, ZMAX - 1, 'minecraft:concrete', 0)        # server | closet
    fill(20, Y, 21, 20, Y + 1, 22, 'minecraft:air')                        # doorways
    fill(38, Y, 21, 38, Y + 1, 22, 'minecraft:air')
    fill(39, Y, 12, XMAX - 1, CEIL - 1, 12, 'minecraft:concrete', 0)       # closet's south wall
    # windows along the south side, so the rooms can be seen into
    for x in range(2, XMAX - 1):
        if x not in (20, 38):
            fill(x, Y + 1, ZMAX, x, Y + 3, ZMAX, 'minecraft:glass')
    fill(-1, Y, 9, -1, Y, 10, 'minecraft:air')
    fill(0, Y, 9, 0, Y + 1, 10, 'minecraft:air')                           # the front door
    # ceiling lights
    for x in range(3, XMAX - 1, 4):
        for z in range(3, ZMAX - 1, 4):
            put(x, CEIL, z, 'minecraft:sea_lantern')
    # the parapet round the roof
    for x in range(0, XMAX + 1):
        put(x, ROOF, 0, 'minecraft:concrete', 7)
        put(x, ROOF, ZMAX, 'minecraft:concrete', 7)
    for z in range(0, ZMAX + 1):
        put(0, ROOF, z, 'minecraft:concrete', 7)
        put(XMAX, ROOF, z, 'minecraft:concrete', 7)
    flush()


# ------------------------------------------------------------------------------------------
# The office: workstations of four slots on a 2 x 2 desk (back row, then front row)
# ------------------------------------------------------------------------------------------
STATIONS = [
    # (back left, back right, front left, front right, screen)
    ('monitor_27_gaming_black', 'pc_tower_gaming_mid_black', 'keyboard_mech_rgb_black',
     'mouse_gaming_black', 'game'),
    ('monitor_24_office_webcam_black', 'pc_small_form_factor_black', 'keyboard_slim_silver',
     'mouse_travel_white', 'desktop'),
    ('monitor_34_ultrawide_curved_black', 'pc_tower_gaming_full_black', 'desk_mat_xxl_rgb',
     'keyboard_mech_tkl_black', 'game'),
    ('monitor_dual_arm_black', 'pc_workstation_silver', 'keyboard_split_ergonomic_black',
     'mouse_vertical_ergonomic_grey', 'code'),
    ('monitor_27_gaming_white', 'pc_tower_gaming_mid_white', 'keyboard_mech_full_white',
     'mouse_pad_black', 'desktop'),
    ('monitor_portrait_black', 'pc_mini_itx_black', 'keyboard_mech_60_white',
     'mouse_trackball_black', 'code'),
    ('monitor_24_office_black', 'pc_mini_desktop_grey', 'keyboard_wireless_compact_white',
     'numpad_white', 'idle'),
    ('laptop_gaming_open_black', 'docking_station_vertical_silver', 'keyboard_mech_tkl_pink',
     'numpad_black', 'game'),
    ('speakers_desktop_pair_black', 'pc_test_bench_open', 'keyboard_mech_60_retro',
     'macro_keypad_black', 'desktop'),
    ('microphone_boom_arm_black', 'webcam_tripod_black', 'keyboard_mech_full_black',
     'headset_on_stand_black', 'desktop'),
    ('external_drive_black', 'usb_hub_grey', 'keyboard_retro_beige', 'kvm_switch_grey',
     'desktop'),
    ('subwoofer_desktop_black', 'laptop_gaming_closed_black', 'desk_mat_xxl_black',
     'vr_headset_black', 'desktop'),
]


def office():
    rows = (3, 9, 15)
    for i, (bl, br, fl, fr, screen) in enumerate(STATIONS):
        row, col = divmod(i, 4)
        z = rows[row]
        x = 2 + col * 4
        for dx in (0, 1):
            for dz in (0, 1):
                put(x + dx, Y, z + dz, 'minecraft:planks', 2)      # a birch desk, full blocks
        for reg, dx, dz in ((bl, 0, 0), (br, 1, 0), (fl, 0, 1), (fr, 1, 1)):
            if reg in REGISTERED:
                device(reg, x + dx, z + dz, S, screen)
        if CHAIR:
            put(x, Y, z + 2, CHAIR, N)
    # the gaming corner: consoles on a media unit, a big screen above, chairs facing it
    for x in range(3, 13):
        put(x, Y, 21, 'minecraft:planks', 5)
    consoles = ['console_orbix_white', 'console_vertex_black', 'console_pocketeer_docked',
                'console_retro_grey']
    for i, reg in enumerate(consoles):
        device(reg, 4 + 2 * i, 21, N)
    device('monitor_34_ultrawide_curved_black', 12, 21, N, 'game')
    for x in (5, 7, 9, 11):
        if GAMING_CHAIR:
            put(x, Y, 18, GAMING_CHAIR, S)
    # the floor box and wall jacks along the north wall, an AP over each row
    put(10, Y, 13, 'floor_box_network', S)
    for i, reg in enumerate(('network_faceplate_1_port', 'network_faceplate_2_port',
                             'network_faceplate_4_port')):
        put(3 + 5 * i, Y + 1, 1, reg, S)
    for x, z, ring in ((5, 6, 'blue'), (14, 6, 'blue'), (9, 12, 'white'), (5, 18, 'adopting')):
        put(x, CEIL - 1, z, 'micafi_ap_ceiling', 0 | (RING[ring] << 1))
    put(15, CEIL - 1, 18, 'micafi_ap_ceiling_lite', 0)
    flush()


# ------------------------------------------------------------------------------------------
# The server room
# ------------------------------------------------------------------------------------------
UNIT = {name: i for i, name in enumerate([
    'SERVER_1U', 'SERVER_2U', 'STORAGE_4U', 'SWITCH_24_PORT', 'SWITCH_48_PORT', 'PATCH_PANEL',
    'FIREWALL', 'UPS_2U', 'PDU', 'KVM_DRAWER', 'CABLE_MANAGER', 'BLANKING_PLATE',
    'MICAFI_GATEWAY', 'GPU_SERVER_2U'])}
HEIGHT = {'SERVER_2U': 2, 'STORAGE_4U': 4, 'UPS_2U': 2, 'GPU_SERVER_2U': 2}


def rack_nbt(units, top=42):
    """The rack's ``u`` byte array: [unit ordinal, top U] pairs, mounted from ``top`` down."""
    out = []
    u = top
    for name in units:
        if name is None:            # a gap of 1U
            u -= 1
            continue
        h = HEIGHT.get(name, 1)
        if u - h + 1 < 1:
            break
        out += [UNIT[name], u]
        u -= h
    return '{u:[B;%s]}' % ','.join('%db' % v for v in out)


NET_TOP = ['PATCH_PANEL', 'CABLE_MANAGER', 'SWITCH_48_PORT', 'CABLE_MANAGER', 'SWITCH_24_PORT',
           'FIREWALL', 'MICAFI_GATEWAY', None]
FILLS = [
    NET_TOP + ['SERVER_1U'] * 14 + ['KVM_DRAWER'] + ['SERVER_2U'] * 4 + ['UPS_2U', 'UPS_2U'],
    NET_TOP + ['STORAGE_4U'] * 6 + [None, 'UPS_2U', 'UPS_2U'],
    NET_TOP + ['GPU_SERVER_2U'] * 10 + ['PDU', 'UPS_2U'],
    NET_TOP + ['SERVER_1U'] * 8 + ['BLANKING_PLATE'] * 6 + ['SERVER_2U'] * 3,
    ['PATCH_PANEL', 'SWITCH_48_PORT', 'SWITCH_48_PORT', 'CABLE_MANAGER', 'PATCH_PANEL',
     'SWITCH_24_PORT', 'SWITCH_24_PORT', 'CABLE_MANAGER', 'FIREWALL', 'FIREWALL', None, None,
     'SERVER_2U', 'SERVER_2U', 'KVM_DRAWER', 'UPS_2U'],
    ['SERVER_1U'] * 6 + [None] * 8 + ['SERVER_2U'] * 2 + ['STORAGE_4U'] * 2,
]


def rack(x, z, reg, face, nbt=None):
    put(x, Y, z, reg, face, nbt)
    put(x, Y + 1, z, reg, face | 4)


def server_room():
    # two fillable rows facing each other across a cold aisle (z 7 to 10)
    for i in range(10):
        x = 23 + i
        rack(x, 6, 'server_rack_42u_glass', S, rack_nbt(FILLS[i % len(FILLS)]))
        rack(x, 11, 'server_rack_42u_mesh', N, rack_nbt(FILLS[(i + 3) % len(FILLS)]))
    rack(35, 6, 'server_rack_42u_open', S, rack_nbt(FILLS[4]))
    rack(35, 11, 'server_rack_42u_open', N)                      # an empty open frame
    # two rows of pre-filled racks behind them
    statics = ['server_rack_42u_filled_compute', 'server_rack_42u_filled_mixed',
               'server_rack_42u_filled_storage', 'server_rack_42u_filled_network',
               'server_rack_42u_filled_gpu', 'server_rack_42u_filled_sparse']
    for i in range(12):
        rack(23 + i, 16, statics[i % len(statics)], S)
        rack(23 + i, 20, statics[(i + 2) % len(statics)], N)
    rack(36, 16, 'server_rack_42u_filled_network_open', S)
    # trays hung under the ceiling: a basket over the first row, a ladder over the second, the
    # basket fed by a cross tray at its west end (a corner), the ladder by one at its middle (a tee)
    for x in range(22, 35):
        put(x, CEIL - 1, 6, 'cable_tray_basket', 1 | 2)
        put(x, CEIL - 1, 11, 'cable_tray_ladder', 1 | 2)
    for z in range(2, 6):
        put(22, CEIL - 1, z, 'cable_tray_basket', 0 | 2)
    for z in range(12, 16):
        put(28, CEIL - 1, z, 'cable_tray_ladder', 0 | 2)
    # a basket tray lying on top of the pre-filled row
    for x in range(23, 35):
        put(x, Y + 2, 16, 'cable_tray_basket', 1)
    for x, z in ((28, 8), (28, 18)):
        put(x, CEIL - 1, z, 'micafi_ap_ceiling', 0)
    put(33, CEIL - 1, 9, 'micafi_ap_ceiling_drop', 0)
    flush()


# ------------------------------------------------------------------------------------------
# The network closet (x 39 to 43, z 1 to 11)
# ------------------------------------------------------------------------------------------
def closet():
    fill(39, Y, 12, 39, Y + 1, 12, 'minecraft:air')                        # its door
    put(41, Y + 2, 1, 'network_cabinet_12u_wall', S,
        rack_nbt(['PATCH_PANEL', 'CABLE_MANAGER', 'SWITCH_24_PORT', 'FIREWALL',
                  'MICAFI_GATEWAY', 'PDU'], top=12))
    for z in range(3, 9):
        put(43, Y, z, 'minecraft:planks', 1)                                # the bench, east wall
    for z, reg in zip(range(3, 9), ['micafi_switch_16_port', 'micafi_switch_8_port',
                                    'micafi_switch_5_port', 'micafi_gateway',
                                    'micafi_control_hub', 'corvane_cable_modem']):
        device(reg, 43, z, W)
    device('micafi_poe_injector', 42, 3, W, y=Y)
    device('micafi_switch_flex_mini', 42, 5, W, y=Y)
    put(39, Y + 2, 6, 'micafi_ap_wall', E)                                  # on the west wall
    put(39, Y + 1, 9, 'micafi_ap_in_wall', E | (RING['white'] << 2))
    put(41, Y + 1, 11, 'network_faceplate_2_port', N)                       # on the south wall
    put(41, CEIL - 1, 6, 'micafi_ap_ceiling_lite', 0 | (RING['blue'] << 1))
    flush()


# ------------------------------------------------------------------------------------------
# The roof and the second building
# ------------------------------------------------------------------------------------------
BX0, BX1, BZ0, BZ1 = 4, 24, -34, -22       # the second building, north of the first
BTOP = 12                                   # its roof, walked on at y 13


def pole(x, z, y0, y1, reg='trafficpolevertical'):
    for y in range(y0, y1 + 1):
        put(x, y, z, reg, 0)


def roof():
    # the second building
    fill(BX0, Y, BZ0, BX1, BTOP, BZ1, 'minecraft:concrete', 8)
    fill(BX0 + 1, Y, BZ0 + 1, BX1 - 1, BTOP - 1, BZ1 - 1, 'minecraft:air')
    for x in range(BX0, BX1 + 1, 2):
        fill(x, Y + 1, BZ1, x, BTOP - 1, BZ1, 'minecraft:glass')
    # on the first building's roof, north edge: poles one block in from the parapet; a pole
    # device sits on the pole's north side, facing north, its clamp reaching south into it
    for x, reg, top, link in ((6, 'micafi_dish_large', 14, 'green'),
                              (10, 'micafi_dish_small', 13, 'green'),
                              (14, 'micafi_airlink_60', 13, 'green'),
                              (18, 'micafi_sector_antenna', 13, 'blue'),
                              (22, 'micafi_station_outdoor', 13, 'white'),
                              (26, 'micafi_ap_outdoor_mesh', 13, None),
                              (30, 'micafi_beam_bridge_pole_fitted', 13, 'green')):
        pole(x, 2, ROOF, top + 1)
        meta = N | ((LINK[link] << 2) if link else 0)
        put(x, top, 1, reg, meta)
    # a pedestal pole carrying a second mesh AP, to show the clamp fitted to a thinner pole
    pole(34, 2, ROOF, ROOF + 2, 'trafficpolepedestalsilver')
    put(34, ROOF + 1, 1, 'micafi_ap_outdoor_mesh', N | (RING['adopting'] << 2))
    # the Beam bridge on its wall arm on a parapet pier, facing the second building
    fill(10, ROOF + 1, 0, 10, ROOF + 2, 0, 'minecraft:concrete', 7)
    put(10, ROOF + 2, -1, 'micafi_beam_bridge', N | (LINK['green'] << 2))
    # the far ends on the second building: a Beam on its south wall, a Link 60 on a pole
    put(10, BTOP - 1, BZ1 + 1, 'micafi_beam_bridge', S | (LINK['green'] << 2))
    pole(14, BZ1 - 1, BTOP + 1, BTOP + 3)
    put(14, BTOP + 2, BZ1, 'micafi_airlink_60', S | (LINK['green'] << 2))
    flush()


# ------------------------------------------------------------------------------------------
# Plinths for anything not placed, and an overview board
# ------------------------------------------------------------------------------------------
def plinths():
    missing = [r for r in REGISTERED if r not in placed]
    missing = list(dict.fromkeys(missing))
    for i, reg in enumerate(missing):
        x, z = i * 3, ZMAX + 4
        put(x, Y, z, 'minecraft:stonebrick')
        put(x, Y + 1, z, reg, 0)
        words, lines = reg.split('_'), ['']
        for w in words:
            if len(lines[-1]) + len(w) + 1 > 15:
                lines.append('')
            lines[-1] = (lines[-1] + ' ' + w).strip()
        sign(x, Y, z + 1, lines[:4], 0)
    flush()
    print('registered %d blocks, placed in the buildings %d, on plinths %d%s'
          % (len(set(REGISTERED)), len(set(REGISTERED)) - len(missing), len(missing),
             (': ' + ', '.join(missing)) if missing else ''))


BOARD = [
    ['Technology Demo', 'office (west)', 'server room', 'closet (east)'],
    ['Click a screen:', 'desktop, game,', 'code, idle', ''],
    ['Rack: click with', 'a unit to mount', 'sneak empty hand', 'removes at height'],
    ['Click an AP or', 'radio: its light', 'Roof: radios aim', 'at the north bldg'],
]


def board():
    fill(-4, Y, 4, -4, Y + 2, 7, 'minecraft:stonebrick')
    for i, lines in enumerate(BOARD):
        wall_sign(-5, Y + 1, 4 + i, lines, 4)
    flush()


# ------------------------------------------------------------------------------------------
cmd('gamerule doDaylightCycle false')
cmd('gamerule doMobSpawning false')
cmd('difficulty peaceful')
cmd('time set 6000')
cmd('weather clear')
shell()
print('shell done')
office()
print('office done')
server_room()
print('server room done')
closet()
roof()
print('roof done')
board()
plinths()
cmd('setworldspawn %d %d %d' % (X0 - 3, Y, Z0 + 10))
cmd('tp @p %d %d %d -90 0' % (X0 - 3, Y, Z0 + 10))
print('done')
