#!/usr/bin/env python3
"""
build_venues_demo.py -- builds the Sports & Venues demo world, over the MCMCP orchestrator.

Load (or let --create make) a FLAT creative world named "Venues Demo" in the dev client with every
module (the concourse uses Furnishings' bar stools; the bridges are Roads' bridge cables) and run
this with the client's instance id. Flat ground's top is y 3; everything stands on y 4, and a
floor that needs a surface replaces y 3. The site plan, in blocks from the site origin (--x, --z,
default 0, 0; the origin is fixed rather than the player's so an --only rebuild lands in place):

  area       x            z            what
  board      -27..-19     -4..8        the sign board at the spawn (setworldspawn -20 4 2)
  arena      -16..40      -8..76       a 61 x 26 rink (ice x 0..25, z 0..60) as VENUES_SYSTEM.md's
                                       worked example: rubber corners, dasher straights, curves,
                                       glass, bench gates, penalty doors, benches, penalty boxes,
                                       scorer's table, lines, anchors and painted cells, goals; a
                                       four-row bowl on each long side (seats on stepped concrete,
                                       two aisles with stairs, handrails and step lights, aisle-end
                                       seats, a wheelchair space, glass guards on the fascia,
                                       section and portal signs); an LED ribbon round the fascia
                                       with 45 degree corners and a text run; the centre-hung
                                       scoreboard over centre ice under a catwalk, two line arrays,
                                       four banners on the north wall, a camera platform on the
                                       south deck, the resurfacer and ice plant out the back
  concourse  41..88       -8..76       a concourse strip east of the bowl: a concession stand
                                       (navy counters, corners, back counter, two menu boards,
                                       hot dog roller, popcorn, pretzel, nacho, soft serve, a
                                       sign), a beer kiosk, a suite bar and drink rail with stools,
                                       team store pieces; the outer wall with a 4-lane ticketed
                                       turnstile entry and scanners, four box office windows, a
                                       marquee on the wall and one on a pylon, gate and section
                                       blades on brackets
  bowling    100..119     16..56       gen_venues_bowling.py --layout (8 lanes), foul line's west
                                       end at (102, 40)
  courts     124..184     20..60       gen_venues_courts.py tennis_pickleball (134, 40),
                                       basketball (156, 40), sand_volleyball (176, 40)
  pool       190..247     14..50       gen_venues_pool.py pool_25m (NW water cell (196, 20)) and
                                       diving_well ((230, 20)), the deck at y 6 on a stone base
                                       (the diving well is five deep: at y 3 its floor would be
                                       under bedrock)
  golf       250..316     10..86       gen_venues_golf.py green (262, 20), minigolf (282, 20),
                                       range (300, 80)
  skate      323..342     17..33       gen_venues_skate.py park at (325, 20)
  ballpark   5..155       138..302     gen_venues_ballpark.py --layout baseball (JSON), home plate
                                       at (80, 270), the field north; dugouts, a backstop net, the
                                       line score over centre field; aluminium bleachers, press box,
                                       ticket booth and a small-head pole behind home; four aimed
                                       light towers (two monopoles, two lattices, large heads)
  football   200..326     106..190     gen_venues_football.py football, centre (260, 140); its
                                       scoreboard; six lattice towers with two aimed heads each
                                       (the doc's layout); bleachers, press box, booth, a
                                       small-head pole south of it
  soccer     203..326     200..280     gen_venues_football.py soccer, centre (260, 240); scoreboard
  track      348..532     100..228     gen_venues_track.py track_400m, centre cell (440, 150) (the
                                       football field does not nest in its infield: the layouts
                                       overlap by 1,308 cells), long_jump (360, 210), pole_vault
                                       (360, 220)
  bridges    -2..202      314..330     a cable-stayed bridge (deck x 0..60 at y 20, an H pylon at
                                       x 30, 24 stays from sockets to deck anchors) and a
                                       self-anchored suspension bridge (deck x 100..200, towers at
                                       x 125 and 175, saddles owning the main span (sag 12,
                                       hangers every 5) and the side spans down to the deck ends)
  plinths    -2..152      343..362     every registered Venues block the build did not place, on
                                       a signed plinth (always rebuilt)

Areas 3-11 are not hand-placed: the builder runs each generator's own --layout (offline, through
subprocess) and sends what it prints, commands in batches through server_run_commands and the
ballpark's JSON as block lists. Each area clears its box to air above y 3 and resets the ground
first, so --only is re-runnable. The plinth check always counts every area (each area's blocks
are worked out even when only some are sent), so it is meaningful after --only too.

Everything goes through the orchestrator by instance id; the script never opens a game port, and
it refuses any world not named "Venues Demo". It sets blocks and runs commands only.

Usage:
    python build_venues_demo.py --instance run-f2eb58 [--create] [--only arena,concourse,...]
                                [--x X --z Z] [--dry-run]
"""
import argparse
import json
import math
import os
import re
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

AREA_NAMES = ('board', 'arena', 'concourse', 'bowling', 'courts', 'pool', 'golf', 'skate',
              'ballpark', 'football', 'soccer', 'track', 'bridges')

ap = argparse.ArgumentParser(description='Build the Sports & Venues demo world.')
ap.add_argument('--instance', required=True, help="the dev game's id, e.g. run-f2eb58")
ap.add_argument('--create', action='store_true', help='create and load a flat "Venues Demo" first')
ap.add_argument('--only', default=None,
                help='comma-separated areas to build: ' + ','.join(AREA_NAMES))
ap.add_argument('--x', type=int, default=0, help='site origin x (default 0)')
ap.add_argument('--z', type=int, default=0, help='site origin z (default 0)')
ap.add_argument('--dry-run', action='store_true',
                help='work everything out and print the counts, without touching a game')
ARGS = ap.parse_args()
ONLY = set(AREA_NAMES) if not ARGS.only else set(a.strip() for a in ARGS.only.split(','))
for _a in ONLY:
    if _a not in AREA_NAMES:
        sys.exit('unknown area %r (areas: %s)' % (_a, ', '.join(AREA_NAMES)))

WORLD_NAME = 'Venues Demo'
X0, Z0 = ARGS.x, ARGS.z
G = 3   # the flat world's ground top
Y = 4   # what stands on it

REPO = os.path.abspath(os.path.join(HERE, '..', '..'))
CSM_RE = re.compile(r'\bcsm:([a-z0-9_]+)')

# ------------------------------------------------------------------------------------------
# Plumbing: every area queues its operations; only the selected areas are sent
# ------------------------------------------------------------------------------------------
placed = set()


class Area:
    def __init__(self, name, box, ytop, label):
        self.name, self.box, self.ytop, self.label = name, box, ytop, label
        self.ops = []      # ('blocks', [...]) | ('fill', dict) | ('cmds', [...])
        self.pending = []

    def push(self):
        if self.pending:
            self.ops.append(('blocks', self.pending))
            self.pending = []


CUR = None
AREAS = []


def area(name, box, ytop, label):
    global CUR
    CUR = Area(name, box, ytop, label)
    AREAS.append(CUR)
    return CUR


def name_of(block):
    return block if ':' in block else 'csm:' + block


def put(x, y, z, block, meta=0, nbt=None):
    """A block at site coordinates (x, z from the site origin, y absolute)."""
    b = {'x': X0 + x, 'y': y, 'z': Z0 + z, 'block': name_of(block), 'metadata': meta}
    if nbt:
        b['nbt'] = nbt
    CUR.pending.append(b)
    if b['block'].startswith('csm:'):
        placed.add(b['block'][4:])


def fill(x0, y0, z0, x1, y1, z1, block, meta=0):
    """A cuboid, sent as fills of at most 16,384 cells (split by layer and by z strips)."""
    CUR.push()
    xs, ys, zs = sorted((x0, x1)), sorted((y0, y1)), sorted((z0, z1))
    width = xs[1] - xs[0] + 1
    step = max(1, 16384 // width)
    for y in range(ys[0], ys[1] + 1):
        z = zs[0]
        while z <= zs[1]:
            z2 = min(zs[1], z + step - 1)
            CUR.ops.append(('fill', dict(block=name_of(block), metadata=meta, x=X0 + xs[0], y=y,
                                         z=Z0 + z, toX=X0 + xs[1], toY=y, toZ=Z0 + z2)))
            z = z2 + 1
    if name_of(block).startswith('csm:'):
        placed.add(name_of(block)[4:])


def cmds(lines):
    """World commands (already in world coordinates), sent through server_run_commands."""
    CUR.push()
    lines = [l.strip() for l in lines if l.strip()]
    CUR.ops.append(('cmds', lines))
    for l in lines:
        for m in CSM_RE.finditer(l):
            placed.add(m.group(1))


def clear(dig=False, base=None):
    """Clear the current area's box to air above the ground and reset the ground."""
    x0, z0, x1, z1 = CUR.box
    fill(x0, Y, z0, x1, CUR.ytop, z1, 'minecraft:air')
    if dig:
        fill(x0, 1, z0, x1, 2, z1, 'minecraft:dirt')
    fill(x0, G, z0, x1, G, z1, 'minecraft:grass')
    if base:
        fill(x0, Y, z0, x1, base, z1, 'minecraft:stone')


def snbt_str(s):
    return json.dumps(s)


def sign_nbt(lines):
    parts = ['Text%d:%s' % (i + 1, json.dumps(json.dumps({'text': t})))
             for i, t in enumerate(lines[:4])]
    return '{' + ','.join(parts) + '}'


def sign(x, y, z, lines, rot=0):
    """A standing sign; rot 0 faces south, 4 west, 8 north, 12 east."""
    put(x, y, z, 'minecraft:standing_sign', rot, sign_nbt(lines))


def wall_sign(x, y, z, lines, out):
    """A wall sign; out 2 north, 3 south, 4 west, 5 east."""
    put(x, y, z, 'minecraft:wall_sign', out, sign_nbt(lines))


def split_words(text, width=15, sep='_'):
    lines = ['']
    for w in text.split(sep):
        if lines[-1] and len(lines[-1]) + len(w) + 1 > width:
            lines.append('')
        lines[-1] = (lines[-1] + ' ' + w).strip()
    return lines[:4]


_GEN_CACHE = {}


def generator(script, *args):
    """Run a Venues generator offline and return its stdout."""
    key = (script,) + tuple(str(a) for a in args)
    if key not in _GEN_CACHE:
        path = os.path.join(HERE, script)
        r = subprocess.run([sys.executable, path] + [str(a) for a in args], cwd=REPO,
                           capture_output=True, text=True)
        if r.returncode != 0:
            sys.exit('%s %s failed:\n%s' % (script, ' '.join(map(str, args)), r.stderr[-2000:]))
        _GEN_CACHE[key] = r.stdout
    return _GEN_CACHE[key]


def layout_commands(script, *args):
    """A generator's --layout ... --commands output, as a list of command lines."""
    return [l for l in generator(script, *args).splitlines() if l.strip()]


# Facings. Venues pieces: S 0, W 1, N 2, E 3. EnumFacing (NSEWUD anchors, wall signs): N 2,
# S 3, W 4, E 5.
S, W, N, E = 0, 1, 2, 3


# ------------------------------------------------------------------------------------------
# 14. The board at the spawn
# ------------------------------------------------------------------------------------------
def build_board():
    area('board', (-27, -4, -19, 8), 8, 'spawn board')
    clear()
    fill(-25, Y, -3, -25, Y + 3, 7, 'minecraft:stonebrick')
    rows = [['Venues Demo', 'CSM: Sports &', 'Venues; boxes', 'x..x / z..z']]
    for a in AREAS_PLAN:
        name, (x0, z0, x1, z1), _top, label = a
        if name in ('board',):
            continue
        rows.append([name.upper(), 'x %d..%d' % (X0 + x0, X0 + x1),
                     'z %d..%d' % (Z0 + z0, Z0 + z1), label[:15]])
    for i, lines in enumerate(rows[:22]):
        y = Y + (i // 11) * 1 + 1
        z = -3 + (i % 11)
        wall_sign(-24, y, z, lines, 5)
    put(-25, Y + 4, 2, 'minecraft:sea_lantern')


# ------------------------------------------------------------------------------------------
# 1. The arena (ice x 0..25, z 0..60 as the doc's worked example)
# ------------------------------------------------------------------------------------------
CURVE = [(0, 7), (0, 6), (0, 5), (0, 4), (1, 4), (1, 3), (1, 2), (2, 2), (2, 1), (3, 1), (4, 1),
         (4, 0), (5, 0), (6, 0), (7, 0)]


def corner_cell(corner, dx, dz):
    return {'nw': (dx, dz), 'ne': (25 - dz, dx), 'se': (25 - dx, 60 - dz),
            'sw': (dz, 60 - dx)}[corner]


def ring_inside(x, z):
    """Inside the ribbon ring (x -5..30, z -4..64 with 45 degree corners over 4 cells)."""
    if not (-5 <= x <= 30 and -4 <= z <= 64):
        return False
    if (x + 5) + (z + 4) < 3 or (30 - x) + (z + 4) < 3:
        return False
    if (30 - x) + (64 - z) < 3 or (x + 5) + (64 - z) < 3:
        return False
    return True


def build_arena():
    area('arena', (-16, -8, 40, 76), 27, 'rink and bowl')
    clear()
    # Floors: rubber inside the ring, ice over the rink, rubber outside each curve.
    for x in range(-5, 31):
        for z in range(-4, 65):
            if ring_inside(x, z) and not (0 <= x <= 25 and 0 <= z <= 60):
                put(x, G, z, 'rink_rubber_floor')
    fill(0, G, 0, 25, G, 60, 'rink_ice', 0)
    rubber = set()
    for corner in ('nw', 'ne', 'se', 'sw'):
        for dx in range(8):
            for dz in range(8):
                if math.hypot(8 - (dx + 1), 8 - (dz + 1)) >= 8:
                    rubber.add(corner_cell(corner, dx, dz))
    for (x, z) in sorted(rubber):
        put(x, G, z, 'rink_rubber_floor')

    def ice(x, z):
        return 0 <= x <= 25 and 0 <= z <= 60 and (x, z) not in rubber

    # Painted cells round each anchor's picture, then the lines, then the anchors.
    boxes = [(8, 17, 25, 35), (8, 17, 0, 5), (8, 17, 55, 60), (1, 11, 4, 14), (14, 24, 4, 14),
             (1, 11, 46, 56), (14, 24, 46, 56)]
    painted = set()
    for (xa, xb, za, zb) in boxes:
        for x in range(xa, xb + 1):
            for z in range(za, zb + 1):
                if ice(x, z):
                    painted.add((x, z))
    for (x, z) in sorted(painted):
        put(x, G, z, 'rink_ice_painted', 0)
    for x in list(range(0, 8)) + list(range(18, 26)):
        put(x, G, 30, 'rink_ice_line', 0)            # red centre line, across x
    for z in (22, 38):
        for x in range(0, 26):
            put(x, G, z, 'rink_ice_line', 2)         # blue lines
    for z in (3, 57):
        for x in list(range(0, 8)) + list(range(18, 26)):
            if ice(x, z):
                put(x, G, z, 'rink_ice_line', 4)     # goal lines
    for (x, z, m) in ((13, 30, 10), (12, 3, 12), (13, 57, 14), (6, 9, 0), (19, 9, 0),
                      (6, 51, 2), (19, 51, 2), (6, 24, 4), (19, 24, 4), (6, 36, 4), (19, 36, 4)):
        put(x, G, z, 'rink_ice_spot', m)

    # Dasher straights and glass (two high on the sides, three on the ends).
    east_open = set(range(23, 29)) | set(range(32, 38))
    for z in range(8, 53):
        if z in (27, 33):
            put(-1, Y, z, 'dasher_penalty_door', 3 if z == 27 else 11)
        else:
            put(-1, Y, z, 'dasher_board', 3)
            put(-1, Y + 1, z, 'dasher_glass', 3)
            put(-1, Y + 2, z, 'dasher_glass', 3)
        if z in (23, 32):
            put(26, Y, z, 'dasher_bench_gate', 1)
        elif z in (28, 37):
            put(26, Y, z, 'dasher_bench_gate', 9)
        else:
            put(26, Y, z, 'dasher_board', 1)
        if z not in east_open:
            put(26, Y + 1, z, 'dasher_glass', 1)
            put(26, Y + 2, z, 'dasher_glass', 1)
    for x in range(8, 18):
        for (z, m) in ((-1, 0), (61, 2)):
            put(x, Y, z, 'dasher_board', m)
            for dy in (1, 2, 3):
                put(x, Y + dy, z, 'dasher_glass', m)
    for corner in ('nw', 'ne', 'se', 'sw'):
        for k, (dx, dz) in enumerate(CURVE):
            x, z = corner_cell(corner, dx, dz)
            put(x, Y, z, 'dasher_board_curve_' + corner, k)
            put(x, Y + 1, z, 'dasher_glass_curve_' + corner, k)
            put(x, Y + 2, z, 'dasher_glass_curve_' + corner, k)

    # Benches, penalty boxes, the scorer's table, goals.
    for z in list(range(24, 28)) + list(range(33, 37)):
        put(28, Y, z, 'player_bench', 1)
    for z in list(range(23, 27)) + list(range(34, 38)):
        put(-3, Y, z, 'penalty_bench', 3)
    for z in range(29, 32):
        put(-3, Y, z, 'scorers_table', 3)
    put(13, Y, 3, 'hockey_goal', 0)
    put(12, Y, 57, 'hockey_goal', 2)

    # The fascia round the ring (2 high), end walls, the south camera deck.
    for x in range(-6, 32):
        for z in range(-5, 66):
            if ring_inside(x, z):
                continue
            if any(ring_inside(x + a, z + b) for a in (-1, 0, 1) for b in (-1, 0, 1)):
                put(x, Y, z, 'minecraft:concrete', 8)
                put(x, Y + 1, z, 'minecraft:concrete', 8)
    fill(-1, Y + 2, -5, 26, 14, -5, 'minecraft:concrete', 8)
    fill(-1, Y + 2, 65, 26, Y + 4, 65, 'minecraft:concrete', 8)
    fill(8, Y, 65, 17, Y + 4, 68, 'minecraft:concrete', 7)

    # The bowls: four 2-deep rows on each long side.
    for side in ('west', 'east'):
        sgn = -1 if side == 'west' else 1
        face = E if side == 'west' else W           # seats face the ice
        stair = 1 if side == 'west' else 0          # vanilla stairs rising away from the ice
        colours = (('navy', 'charcoal', 'ice_blue', 'gold') if side == 'west'
                   else ('crimson', 'silver', 'black', 'red'))
        fascia_x = -6 if side == 'west' else 31
        wall_x = -15 if side == 'west' else 40
        for z in range(0, 61):
            put(fascia_x, Y + 2, z, 'stadium_glass_guard', face)
        for k in range(4):
            xf = fascia_x + sgn * (1 + 2 * k)
            xb = xf + sgn
            fill(xf, Y, -1, xb, Y + 1 + k, 61, 'minecraft:concrete', 7)
            ys = Y + 2 + k                          # the air cell over the row's floor
            for z in range(2, 59):
                if z in (20, 40):
                    # the aisle: tread (front) then a stair (back)
                    put(xb, ys, z, 'minecraft:stone_brick_stairs', stair)
                    if z == 20:
                        put(xf, ys, z, 'stadium_aisle_handrail', face | 2 << 2)
                        put(xb, ys + 1, z, 'stadium_aisle_handrail', face | 3 << 2)
                    else:
                        put(xf, ys, z, 'stadium_aisle_step_light', face)
                    continue
                if z in (2, 58, 19, 21, 39, 41):
                    put(xb, ys, z, 'stadium_seat_aisle_end', k)      # rows A-D
                elif k == 0 and z == 50 and side == 'west':
                    put(xb, ys, z, 'stadium_wheelchair_space', face)
                elif k == 0 and z == 51 and side == 'west':
                    put(xb, ys, z, 'stadium_companion_seat', face)
                else:
                    put(xb, ys, z, 'stadium_seat_' + colours[k], face)
        fill(wall_x, Y, -2, wall_x, 13, 62, 'minecraft:concrete', 8)
        sx = wall_x - sgn
        nums = ('101', '102', '103') if side == 'west' else ('106', '105', '104')
        for z, num in zip((10, 30, 50), nums):
            nbt = '{l1:"%s",s:"neutral",m:"wall",al:"c",d:0b,lt:0b}' % num
            put(sx, 10, z, 'stadium_section_sign', face, nbt)
            put(sx, 11, z, 'stadium_section_sign', face, nbt)
        for z in (20, 40):
            put(sx, 11, z, 'stadium_portal_sign', face)

    # The LED ribbon in front of the fascia, y 5, with 45 degree corners.
    ry = Y + 1
    for x in range(-1, 27):
        put(x, ry, -4, 'led_ribbon_board', S | 0 << 2)
        put(x, ry, 64, 'led_ribbon_board', N | 1 << 2)
    for z in range(0, 61):
        put(-5, ry, z, 'led_ribbon_board', E | 2 << 2)
        put(30, ry, z, 'led_ribbon_board', W | 3 << 2)
    put(-5, ry, 60, 'led_ribbon_board_text', E, '{txt:"WELCOME TO THE ARENA",fg:6,bg:12,spd:2}')
    for i in range(4):
        put(-2 - i, ry, -4 + i, 'led_ribbon_board_corner', 3 | 0 << 2)     # north-west
        put(27 + i, ry, -4 + i, 'led_ribbon_board_corner', 0 | 0 << 2)     # north-east
        put(30 - i, ry, 61 + i, 'led_ribbon_board_corner', 1 | 1 << 2)     # south-east
        put(-5 + i, ry, 61 + i, 'led_ribbon_board_corner', 2 | 1 << 2)     # south-west

    # Hung over the ice: the scoreboard (13 x 9 cells, the long faces east and west), a catwalk
    # over its rigging, two line arrays; banners on the north wall; the camera on the south deck.
    put(13, 16, 30, 'centre_hung_scoreboard', E,
        '{home:"WOLVES",away:"BIRDS",hs:3,as:2,hsh:31,ash:24,per:"3",clk:7215,hc:0,ac:9}')
    for z in range(22, 39):
        put(13, 25, z, 'venue_catwalk')
    put(13, 18, 20, 'line_array_speaker', S)
    put(13, 18, 40, 'line_array_speaker', N)
    put(10, 11, -4, 'championship_banner', S,
        '{l1:"CHAMPIONS",l2:"2026",l3:"NATIONAL HOCKEY CUP",bc:0,tc:6,fc:14}')
    put(17, 11, -4, 'retired_number_banner', S, '{l1:"SMITH",l2:"19",l3:"1998 - 2012",bc:9,tc:14,fc:14}')
    put(4, 10, -4, 'championship_banner_small', S)
    put(21, 10, -4, 'retired_number_banner_small', S)
    put(12, Y + 5, 66, 'broadcast_camera_platform', N)

    # Back of house.
    fill(0, G, 69, 12, G, 75, 'minecraft:concrete', 7)
    put(3, Y, 72, 'ice_resurfacer', N)
    put(8, Y, 71, 'ice_plant', N)


# ------------------------------------------------------------------------------------------
# 2. The concourse and the exterior
# ------------------------------------------------------------------------------------------
def build_concourse():
    area('concourse', (41, -8, 88, 76), 14, 'concourse, entry')
    clear()
    fill(41, G, -2, 65, G, 62, 'minecraft:concrete', 8)
    fill(67, G, -2, 88, G, 62, 'minecraft:stone', 0)
    turnstiles = (51, 53, 55, 57)
    windows = {44: 1, 45: 0, 46: 0, 47: 2}          # z: legend (WILL CALL, BOX OFFICE, TICKETS)
    for z in range(-2, 63):
        for y in range(Y, 13):
            if z in turnstiles and y <= Y + 2:
                continue
            if z in windows and y <= Y + 1:
                continue
            put(66, y, z, 'minecraft:concrete', 0)

    # Concession stand against the bowl wall, customers to the east.
    for z in range(4, 13):
        put(41, Y, z, 'concession_back_counter', E)
    put(41, Y + 2, 8, 'concession_menu_board', E | 0 << 2)
    put(41, Y + 2, 5, 'concession_menu_board', E | 1 << 2)
    put(45, Y, 3, 'concession_counter_corner', E | 1 << 2)
    put(45, Y, 13, 'concession_counter_corner', S | 1 << 2)
    for z in range(4, 13):
        put(45, Y, z, 'concession_counter', E | 1 << 2)
    for x in range(42, 45):
        put(x, Y, 3, 'concession_counter', N | 1 << 2)
        put(x, Y, 13, 'concession_counter', S | 1 << 2)
    put(45, Y + 1, 5, 'hot_dog_roller_grill', E | 4)
    put(45, Y + 1, 7, 'popcorn_machine', E | 4)
    put(45, Y + 1, 9, 'pretzel_warmer', E | 4)
    put(45, Y + 1, 11, 'nacho_cheese_dispenser', E | 4)
    put(43, Y + 1, 13, 'soft_serve_machine', S | 4)
    for z in (7, 8):
        put(45, Y + 3, z, 'concession_sign', E, '{l1:"HOT DOGS",al:"c"}')

    put(60, Y, 20, 'beer_kiosk', W)                  # its second cell is (60, 19)

    # A suite bar and drink rail with stools.
    for z in range(30, 35):
        put(56, Y, z, 'suite_bar_counter', E)
    for z in range(28, 37):
        put(50, Y, z, 'suite_drink_rail', W)
        put(51, Y, z, 'csm:bar_stool_oak', 0)

    # Team store against the outer wall.
    put(65, Y, 6, 'hat_wall', W, '{c:4,t:14}')
    put(65, Y, 8, 'jersey_wall_rack', W | 4)
    put(65, Y, 9, 'jersey_wall_rack', W)
    put(62, Y, 7, 'team_apparel_table', W, '{c:9,t:6}')
    put(62, Y, 10, 'jersey_mannequin', W, '{c:0,t:14}')
    for z in (7, 8):
        put(65, Y + 3, z, 'concession_sign', W, '{l1:"TEAM STORE",al:"c"}')

    # The entry: ticketed turnstiles in the wall (passing west), scanners outside.
    for z in turnstiles:
        put(66, Y, z, 'stadium_turnstile', E | 4)
    for z in (50, 52, 54, 56):
        put(67, Y, z, 'stadium_ticket_scanner', E)
    for z, legend in windows.items():
        put(66, Y, z, 'box_office_window', E | legend << 2)

    # A marquee on the wall (10 x 4, reader looking west: the controller at the south end).
    for z in range(20, 30):
        for y in range(Y + 2, Y + 6):
            put(67, y, z, 'marquee_board', E)
    put(67, Y + 2, 29, 'marquee_board_controller', E,
        '{l1:"TONIGHT 7PM",l2:"WOLVES VS BIRDS",l3:"TICKETS AT THE BOX OFFICE",fg:6,bg:0}')
    # A double-sided board on a pylon in the plaza.
    for z in range(10, 14):
        for y in range(Y, Y + 3):
            put(80, y, z, 'marquee_pylon', E)
    for z in range(9, 15):
        for y in range(Y + 3, Y + 6):
            put(80, y, z, 'marquee_board', E | 4)
    put(80, Y + 3, 14, 'marquee_board_controller', E | 4,
        '{l1:"CSM ARENA",l2:"VENUES DEMO",l3:"",fg:14,bg:0}')
    # Blades on brackets.
    put(67, Y + 5, 35, 'venue_gate_blade', S)
    put(67, Y + 6, 35, 'venue_gate_blade', S)
    put(67, Y + 7, 35, 'venue_blade_bracket', E)
    for x in (67, 68):
        put(x, Y + 6, 40, 'venue_section_blade', S)
        put(x, Y + 7, 40, 'venue_blade_bracket', E)


# ------------------------------------------------------------------------------------------
# 12. Shared outdoor pieces
# ------------------------------------------------------------------------------------------
def bleacher_stand(x0, x1, zf, aisle):
    """Bleachers facing north from row z zf (two 2-deep rows, three 1-deep), a press box behind
    them, a ticket booth beside and a small-head light pole."""
    rows = [(Y, zf, 'deck'), (Y, zf + 1, 'seat'), (Y + 1, zf + 2, 'deck'), (Y + 1, zf + 3, 'seat'),
            (Y + 2, zf + 4, 'seat'), (Y + 3, zf + 5, 'seat'), (Y + 4, zf + 6, 'seat')]
    for x in range(x0, x1 + 1):
        for (y, z, kind) in rows:
            for fy in range(Y, y):
                put(x, fy, z, 'aluminium_bleacher_frame', N)
            if kind == 'deck':
                put(x, y, z, 'aluminium_bleacher', N)
            elif x == aisle:
                put(x, y, z, 'aluminium_bleacher_aisle', N)
            else:
                put(x, y, z, 'aluminium_bleacher', N | 4)
    # Press box: a 5 x 2 box on a platform, built clockwise (corners face the run leaving them).
    c, zp = aisle, zf + 8
    fill(c - 2, Y, zp, c + 2, Y + 4, zp + 1, 'minecraft:stonebrick')
    py = Y + 5
    put(c - 2, py, zp, 'press_box_module', N | 8)          # NW corner faces north
    for x in range(c - 1, c + 2):
        put(x, py, zp, 'press_box_module', N)             # windows
        put(x, py, zp + 1, 'press_box_module', S | 4)     # back walls
    put(c + 2, py, zp, 'press_box_module', E | 8)          # NE faces east
    put(c + 2, py, zp + 1, 'press_box_module', S | 8)      # SE faces south
    put(c - 2, py, zp + 1, 'press_box_module', W | 8)      # SW faces west
    fill(c - 2, py + 2, zp, c + 2, py + 2, zp + 1, 'minecraft:stone_slab', 0)
    put(x1 + 4, Y, zf, 'ticket_booth', N)
    for y in range(Y, Y + 10):
        put(x0 - 4, y, zf + 2, 'stadium_light_pole', N)
    put(x0 - 4, Y + 10, zf + 2, 'stadium_light_head_small', N | 4)


def tower(x, z, kind, face, heads):
    """A light tower 20 shafts high; heads: [(meta, (aimx, aimz) or None)], stacked 3 apart."""
    for y in range(Y, Y + 20):
        put(x, y, z, kind, face)
    for i, (meta, aim) in enumerate(heads):
        nbt = '{aim:[I;%d,%d]}' % aim if aim else None
        put(x, Y + 20 + 3 * i, z, 'stadium_light_head', meta, nbt)


# ------------------------------------------------------------------------------------------
# 3-11. Fields from the generators' own layouts
# ------------------------------------------------------------------------------------------
def build_ballpark():
    area('ballpark', (5, 138, 155, 302), 28, 'baseball, lights')
    clear()
    hx, hz = 80, 270
    data = json.loads(generator('gen_venues_ballpark.py', '--layout', 'baseball'))
    for (x, z, b, m) in data['ground']:
        put(hx + x, G, hz + z, b, m)
    for (x, dy, z, b, m, nbt) in data['objects']:
        put(hx + x, G + dy, hz + z, b, m, nbt)
    # Dugouts (facing the field), a backstop net, the line score over centre field.
    for sgn, face in ((1, W), (-1, E)):
        bx = hx + sgn * 19
        fill(hx + sgn * 21, Y, hz - 4, hx + sgn * 21, Y + 2, hz + 5, 'minecraft:concrete', 7)
        for z in range(hz - 3, hz + 4):
            put(bx, Y, z, 'dugout_bench', face)
        for z in range(hz - 3, hz + 5):
            put(hx + sgn * 17, Y, z, 'dugout_rail', face)
        for x in range(min(hx + sgn * 17, hx + sgn * 20), max(hx + sgn * 17, hx + sgn * 20) + 1):
            for z in range(hz - 4, hz + 6):
                put(x, Y + 2, z, 'dugout_canopy', face)
        put(hx + sgn * 20, Y, hz + 4, 'dugout_helmet_rack', face)
        put(hx + sgn * 20, Y, hz + 5, 'dugout_bat_rack', face)
        put(bx, Y, hz + 5, 'dugout_water_cooler', face)
    for x in range(hx - 10, hx + 11):
        for y in range(Y, Y + 4):
            put(x, y, hz + 13, 'ballpark_net', N | (4 if (x - hx) % 10 == 0 else 0))
    put(hx + 5, Y, hz - 128, 'ballpark_scoreboard', S,
        '{away:"VISITORS",ai:"0 1 0 0 2 0 0",hi:"1 0 0 3 0 0",ah:7,ae:1,hh:9,b:2,s:1,o:1,mph:94,bat:1}')
    # Four aimed towers on the infield, then the stand behind home.
    tower(hx - 30, hz - 8, 'stadium_light_pole', E, [(E | 4, (30, -11))])
    tower(hx + 30, hz - 8, 'stadium_light_pole', W, [(W | 4, (-30, -11))])
    tower(hx - 36, hz - 38, 'stadium_light_lattice', E, [(E | 4, (36, 19))])
    tower(hx + 36, hz - 38, 'stadium_light_lattice', W, [(W | 4, (-36, 19))])
    bleacher_stand(hx - 8, hx + 8, hz + 18, hx)


def build_football():
    area('football', (200, 106, 326, 190), 32, 'football, lights')
    clear()
    cx, cz = 260, 140
    cmds(layout_commands('gen_venues_football.py', '--layout', 'football', '--commands',
                         X0 + cx, G, Z0 + cz))
    put(cx + 62, Y, cz - 5, 'football_scoreboard', W,
        '{home:"WOLVES",away:"BIRDS",hs:21,as:17,per:"3",clk:4235,dn:3,dist:"7",ball:"35",'
        'hto:2,ato:3,pos:1}')
    aims = [((13, 18), (40, 30)), ((-13, 18), (14, 18)), ((-12, 18), (-40, 30))]
    for i, dx in enumerate((-54, 0, 54)):
        lo, hi = aims[i]
        tower(cx + dx, cz - 30, 'stadium_light_lattice', S, [(S | 4, lo), (S | 4, hi)])
        tower(cx + dx, cz + 31, 'stadium_light_lattice', N,
              [(N | 4, (lo[0], -lo[1])), (N | 4, (hi[0], -hi[1]))])
    bleacher_stand(cx - 12, cx + 12, cz + 35, cx)


def build_soccer():
    area('soccer', (203, 200, 326, 280), 12, 'soccer pitch')
    clear()
    cx, cz = 260, 240
    cmds(layout_commands('gen_venues_football.py', '--layout', 'soccer', '--commands',
                         X0 + cx, G, Z0 + cz))
    put(cx + 62, Y, cz - 5, 'soccer_scoreboard', W,
        '{home:"CITY",away:"ROVERS",hs:2,as:1,per:"2",clk:43800,add:4,hc:4,ac:12}')


def build_bowling():
    area('bowling', (100, 16, 119, 56), 10, '8-lane centre')
    clear()
    cmds(layout_commands('gen_venues_bowling.py', '--layout', '--commands', X0 + 102, G, Z0 + 40))


def build_courts():
    area('courts', (124, 20, 184, 60), 10, 'tennis, hoops, sand')
    clear()
    for name, x in (('tennis_pickleball', 134), ('basketball', 156), ('sand_volleyball', 176)):
        cmds(layout_commands('gen_venues_courts.py', '--layout', name, '--commands',
                             X0 + x, G, Z0 + 40))


def build_track():
    area('track', (348, 100, 532, 228), 12, '400 m track, jumps')
    clear()
    cmds(layout_commands('gen_venues_track.py', '--layout', 'track_400m', '--commands',
                         X0 + 440, G, Z0 + 150))
    cmds(layout_commands('gen_venues_track.py', '--layout', 'long_jump', '--commands',
                         X0 + 360, G, Z0 + 210))
    cmds(layout_commands('gen_venues_track.py', '--layout', 'pole_vault', '--commands',
                         X0 + 360, G, Z0 + 220))


def build_pool():
    area('pool', (190, 14, 247, 50), 18, '25 m pool, diving')
    deck = 6
    clear(dig=True, base=deck - 1)
    cmds(layout_commands('gen_venues_pool.py', '--layout', 'pool_25m', '--commands',
                         X0 + 196, deck, Z0 + 20))
    cmds(layout_commands('gen_venues_pool.py', '--layout', 'diving_well', '--commands',
                         X0 + 230, deck, Z0 + 20))


def build_golf():
    area('golf', (250, 10, 316, 86), 10, 'green, range, mini')
    clear()
    cmds(layout_commands('gen_venues_golf.py', '--layout', 'green', '--commands',
                         X0 + 262, G, Z0 + 20))
    cmds(layout_commands('gen_venues_golf.py', '--layout', 'minigolf', '--commands',
                         X0 + 282, G, Z0 + 20))
    cmds(layout_commands('gen_venues_golf.py', '--layout', 'range', '--commands',
                         X0 + 300, G, Z0 + 80))


def build_skate():
    area('skate', (323, 17, 342, 33), 10, 'skate park')
    clear()
    cmds(layout_commands('gen_venues_skate.py', '--layout', 'park', '--commands',
                         X0 + 325, G, Z0 + 20))


# ------------------------------------------------------------------------------------------
# 13. Bridges (Roads' bridge cables)
# ------------------------------------------------------------------------------------------
def cable(x, y, z, target, **kw):
    """One cable's NBT (the single-cable form) for an anchor at (x, y, z), site coordinates."""
    tx, ty, tz = target
    keys = ['target:[%d,%d,%d]' % (X0 + tx, ty, Z0 + tz)]
    for k, v in kw.items():
        keys.append('%s:%s' % (k, json.dumps(v) if isinstance(v, str) else v))
    return '{' + ','.join(keys) + '}'


def cable_list(cables):
    out = []
    for target, kw in cables:
        out.append(cable(0, 0, 0, target, **kw))
    return '{cables:[' + ','.join(out) + ']}'


def deck_and_piers(xa, xb, piers, za=319, zb=325):
    fill(xa, 20, za, xb, 20, zb, 'minecraft:concrete', 8)
    for px in piers:
        fill(px, Y, za + 1, px, 19, zb - 1, 'minecraft:stonebrick')


def build_bridges():
    area('bridges', (-2, 314, 202, 330), 50, 'cable bridges')
    clear()
    # Cable-stayed: an H pylon at x 30, six stays each way from each leg.
    deck_and_piers(0, 60, (2, 15, 45, 58))
    for zl in (318, 326):
        fill(30, Y, zl, 30, 46, zl, 'minecraft:concrete', 0)
    fill(30, 19, 318, 30, 19, 326, 'minecraft:concrete', 0)
    fill(30, 46, 318, 30, 46, 326, 'minecraft:concrete', 0)
    anchors = []
    for zl, edge in ((318, 319), (326, 325)):
        for sgn, facing in ((1, 5), (-1, 4)):
            for i in range(6):
                sx, sy = 30 + sgn, 26 + 3 * i
                tgt = (30 + sgn * (5 + 5 * i), 21, edge)
                anchors.append(tgt)
                put(sx, sy, zl, 'bridge_cable_socket', facing,
                    cable(sx, sy, zl, tgt, style='stay', thick=4, color='white'))
    # Self-anchored suspension: towers at x 125 and 175, saddles over the deck edges.
    deck_and_piers(100, 200, (100, 140, 160, 200))
    for tx in (125, 175):
        for zl in (318, 326):
            fill(tx, Y, zl, tx, 40, zl, 'minecraft:concrete', 0)
        fill(tx, 19, 318, tx, 19, 326, 'minecraft:concrete', 0)
        fill(tx, 40, 318, tx, 40, 326, 'minecraft:concrete', 0)
    main = dict(style='main', thick=8, color='international_orange', hangers=5, hangerThick=2)
    for edge in (319, 325):
        west = cable_list([((175, 41, edge), dict(main, sag=12)),
                           ((101, 21, edge), dict(main, sag=1))])
        put(125, 41, edge, 'bridge_cable_saddle', 5, west)
        put(175, 41, edge, 'bridge_cable_saddle', 5, cable_list([((199, 21, edge), dict(main, sag=1))]))
        for x in [101, 199] + list(range(130, 171, 5)) + [120, 115, 110, 105, 180, 185, 190, 195]:
            anchors.append((x, 21, edge))
    for (x, y, z) in anchors:
        put(x, y, z, 'bridge_cable_deck_anchor', 2)


# ------------------------------------------------------------------------------------------
# Finally: every registered Venues block not placed goes on a plinth
# ------------------------------------------------------------------------------------------
def registered():
    import csm_block_index as cbi
    tabs, _classes = cbi.scan_tabs()
    names = []
    for _module, path in cbi._tab_files():
        f = os.path.basename(path)
        if not f.startswith('CsmTabVenues') or f == 'CsmTabVenuesHidden.java':
            continue
        with open(path, encoding='utf-8', errors='replace') as fh:
            m = cbi._TAB_ID_RE.search(cbi._normalize(fh.read()))
        tab_id = m.group(1) if m else 'tab' + f[len('CsmTab'):-len('.java')].lower()
        for reg, cls in tabs.get(tab_id, []):
            if reg is None:
                print('  ? cannot name a block of class %s in %s' % (cls, f))
            elif reg not in names:
                names.append(reg)
    return names


def build_plinths():
    area('plinths', (-2, 343, 152, 362), 8, 'unplaced blocks')
    clear()
    names = registered()
    missing = [r for r in names if r not in placed]
    for i, reg in enumerate(missing):
        x, z = (i % 50) * 3, 345 + (i // 50) * 5
        put(x, Y, z, 'minecraft:stonebrick')
        put(x, Y + 1, z, reg, 0)
        sign(x, Y, z + 1, split_words(reg), 0)
    return names, missing


# ------------------------------------------------------------------------------------------
# The site plan (also what the board lists)
# ------------------------------------------------------------------------------------------
AREAS_PLAN = [
    ('arena', (-16, -8, 40, 76), 27, 'rink and bowl'),
    ('concourse', (41, -8, 88, 76), 14, 'concourse, entry'),
    ('bowling', (100, 16, 119, 56), 10, '8-lane centre'),
    ('courts', (124, 20, 184, 60), 10, 'tennis, hoops'),
    ('pool', (190, 14, 247, 50), 18, 'pool, diving'),
    ('golf', (250, 10, 316, 86), 10, 'green, range'),
    ('skate', (323, 17, 342, 33), 10, 'skate park'),
    ('ballpark', (5, 138, 155, 302), 28, 'baseball'),
    ('football', (200, 106, 326, 190), 32, 'football'),
    ('soccer', (203, 200, 326, 280), 12, 'soccer'),
    ('track', (348, 100, 532, 228), 12, '400 m track'),
    ('bridges', (-2, 314, 202, 330), 50, 'cable bridges'),
    ('plinths', (-2, 343, 152, 362), 8, 'unplaced'),
]

BUILDERS = [('board', build_board), ('arena', build_arena), ('concourse', build_concourse),
            ('bowling', build_bowling), ('courts', build_courts), ('pool', build_pool),
            ('golf', build_golf), ('skate', build_skate), ('ballpark', build_ballpark),
            ('football', build_football), ('soccer', build_soccer), ('track', build_track),
            ('bridges', build_bridges)]


# ------------------------------------------------------------------------------------------
# Sending
# ------------------------------------------------------------------------------------------
class Sender:
    def __init__(self, o, server):
        self.o, self.server = o, server

    def blocks(self, blocks):
        for i in range(0, len(blocks), 800):
            r = self.o.call('server_set_blocks', instance=self.server, mode='list',
                            blocks=blocks[i:i + 800], load=True)
            if isinstance(r, dict):
                for key in ('notKept', 'unloaded', 'errors', 'failed'):
                    v = r.get(key)
                    if v:
                        print('  ! set_blocks %s: %s' % (key, json.dumps(v)[:300]))

    def fill(self, args):
        self.o.call('server_set_blocks', instance=self.server, mode='fill', load=True, **args)

    def commands(self, lines):
        for i in range(0, len(lines), 500):
            batch = lines[i:i + 500]
            start = 0
            while start < len(batch):
                r = self.o.call('server_run_commands', instance=self.server,
                                commands=batch[start:])
                if not isinstance(r, dict):
                    break
                if r.get('warning'):
                    print('  ! %s' % str(r.get('warning'))[:300])
                bad = [x for x in r.get('results', []) if isinstance(x, dict)
                       and (x.get('error') or x.get('succeeded') is False)]
                for x in bad[:5]:
                    print('  ! %s' % json.dumps(x)[:300])
                nxt = r.get('nextIndex')
                if nxt is None:
                    break
                start += int(nxt)

    def run(self, a):
        a.push()
        for kind, payload in a.ops:
            if kind == 'blocks':
                self.blocks(payload)
            elif kind == 'fill':
                self.fill(payload)
            else:
                self.commands(payload)


def stats(a):
    a.push()
    nb = sum(len(p) for k, p in a.ops if k == 'blocks')
    nf = sum(1 for k, p in a.ops if k == 'fill')
    nc = sum(len(p) for k, p in a.ops if k == 'cmds')
    return '%d blocks, %d fills, %d commands' % (nb, nf, nc)


def box_text(a):
    x0, z0, x1, z1 = a.box
    return 'x %d..%d, z %d..%d, y up to %d' % (X0 + x0, X0 + x1, Z0 + z0, Z0 + z1, a.ytop)


def main():
    # Work every area out first (offline), so the plinth check counts the whole site.
    print('working the site out (running the generators) ...')
    for name, fn in BUILDERS:
        fn()
        print('  %-10s %s' % (name, stats(CUR)))
    names, missing = build_plinths()
    plinths = CUR

    sender = None
    if not ARGS.dry_run:
        from mcmcp_orchestrator import Orchestrator
        o = Orchestrator()
        client, server = ARGS.instance + '.client', ARGS.instance + '.server'
        if ARGS.create:
            print('creating "%s" ...' % WORLD_NAME)
            o.call('client_world_create', instance=client, name=WORLD_NAME, worldType='flat',
                   gameMode='creative', allowCheats=True)
            o.call('client_wait', instance=client, waitFor='worldLoaded', ticks=2400)
            o.call('client_wait', instance=client, ticks=60)
        info = o.call('server_world_info', instance=server)
        if WORLD_NAME not in json.dumps(info):
            sys.exit('refusing: the world on %s is not "%s"' % (server, WORLD_NAME))
        sender = Sender(o, server)
        sender.commands(['gamerule doDaylightCycle false', 'gamerule doMobSpawning false',
                         'difficulty peaceful', 'time set 6000', 'weather clear 1000000'])
        # A write never generates terrain: cells in chunks that do not exist yet come back
        # "unloaded". Generate the whole site first (chunks already on disk are skipped).
        xs = [v for a in AREAS for v in (a.box[0], a.box[2])]
        zs = [v for a in AREAS for v in (a.box[1], a.box[3])]
        r =o.call('server_pregen_chunks', instance=server, x=X0 + min(xs) - 16,
                   z=Z0 + min(zs) - 16, toX=X0 + max(xs) + 16, toZ=Z0 + max(zs) + 16,
                   chunksPerTick=8)
        print('site chunks: %s' % json.dumps(r)[:160])

    t0 = time.time()
    for a in AREAS:
        if a is plinths or a.name not in ONLY:
            continue
        print('%s: %s (%s)' % (a.name, box_text(a), stats(a)))
        if sender:
            t = time.time()
            sender.run(a)
            print('  %s built in %.0f s' % (a.name, time.time() - t))
    print('plinths: %s (%s)' % (box_text(plinths), stats(plinths)))
    if sender:
        sender.run(plinths)
        if 'board' in ONLY:
            sender.commands(['setworldspawn %d %d %d' % (X0 - 20, Y, Z0 + 2),
                             'tp @a %d %d %d 90 0' % (X0 - 20, Y, Z0 + 2)])
    print('registered %d, placed %d, on plinths %d%s'
          % (len(names), len(names) - len(missing), len(missing),
             (': ' + ', '.join(missing)) if missing else ''))
    if sender:
        print('done in %.0f s' % (time.time() - t0))


if __name__ == '__main__':
    main()
