#!/usr/bin/env python3
"""
build_parks_demo.py -- builds the Parks & Greenery demo world, over MCMCP.

Load a fresh FLAT creative world in the dev client (Core + Parks is enough; nothing here needs
another module), then run this. It lays out everything the module ships and plants every tree
with the real Tree Planting Tool, driven as a player would: tool in hand, camera aimed at the
ground, right-click.

Areas (ground is y=3, things stand at y=4):
  street    a road with two sidewalks: leaning street trees on grates arching over the road,
            pleached lindens, a staked young tree, a hoop-fenced pit, benches, bins, baskets
  park      south of the street: hedged, paths, fountain plaza, playground, pergola with
            picnic tables, gardens, pond with a willow, lawn with irrigation
  arboretum west of the park: one of every Tree Planting Tool preset, each with a sign
  kit       north of the street: every leaves block and crown on a plinth, hand-built log
            leans in every width, moss and willow strands

The site is placed relative to the world spawn (--x/--z to move it). Planting is random, so a
tree whose spot is taken by a neighbour's crown is retried a few times, a block to either side,
and any that still will not fit are listed at the end.

Aim with client_look, never with a teleport's yaw and pitch: those can leave the client looking
somewhere else entirely, and a click then plants a tree wherever the camera happens to point.

Usage:
    python build_parks_demo.py [--x X --z Z]
"""
import argparse
import json
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_bench as B  # noqa: E402 -- its MCMCP client and token reader


class _Session:
    """csm_bench's client with keyword arguments and text results, as this script uses it."""

    def __init__(self, url):
        self.mcp = B.Mcp(url, B.read_token()).connect()

    def call(self, name, **args):
        out = self.mcp.call(name, args)
        return out if isinstance(out, str) else json.dumps(out)


ap = argparse.ArgumentParser(description="Build the Parks & Greenery demo world.")
ap.add_argument("--x", type=int, default=None, help="west end of the street (default: spawn)")
ap.add_argument("--z", type=int, default=None, help="north edge of the street (default: spawn)")
ARGS = ap.parse_args()
client = _Session(B.CLIENT_URL)
server = _Session(B.SERVER_URL)

_me = json.loads(client.call('client_player_state'))
X0 = ARGS.x if ARGS.x is not None else int(_me['position']['x']) - 15
Z0 = ARGS.z if ARGS.z is not None else int(_me['position']['z']) - 20
Y = 4           # standing height on a default flat world
G = 3           # ground

blocks = []


def put(x, y, z, block, meta=0, nbt=None):
    b = {"x": x, "y": y, "z": z, "block": block if ':' in block else 'csm:' + block,
         "metadata": meta}
    if nbt:
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


# Facing metadata for the NSEW blocks (horizontal index) and the NSEWUD ones (EnumFacing index).
S, W, N, E = 0, 1, 2, 3
UD = {N: 2, S: 3, W: 4, E: 5}
YAW = {S: 0, W: 90, N: 180, E: -90}
DIR = {S: (0, 1), W: (-1, 0), N: (0, -1), E: (1, 0)}

# ------------------------------------------------------------------------------------------
# World rules
# ------------------------------------------------------------------------------------------
for cmd in ('gamerule doDaylightCycle false', 'gamerule doWeatherCycle false',
            'gamerule doMobSpawning false', 'difficulty peaceful', 'time set 6000',
            'weather clear 1000000', 'kill @e[type=!player]'):
    server.call('server_run_command', command=cmd)

# ------------------------------------------------------------------------------------------
# Street: north sidewalk z0..3, road z4..11, south sidewalk z12..15
# ------------------------------------------------------------------------------------------
XS, XE = X0 - 5, X0 + 75
fill(XS, G, Z0, XE, G, Z0 + 3, 'minecraft:concrete', 8)
fill(XS, G, Z0 + 4, XE, G, Z0 + 11, 'minecraft:concrete', 15)
fill(XS, G, Z0 + 12, XE, G, Z0 + 15, 'minecraft:concrete', 8)
for x in range(XS, XE + 1):
    if x % 4 < 2:
        put(x, G, Z0 + 7, 'minecraft:concrete', 4)
    put(x, G, Z0 + 4, 'minecraft:concrete', 0)
    put(x, G, Z0 + 11, 'minecraft:concrete', 0)

# North sidewalk: street trees on grates and pits, every 11 blocks.
north_trees = []
for i, x in enumerate(range(X0, X0 + 70, 11)):
    grate = ['tree_grate_square', 'tree_grate_round', 'tree_pit_mulch'][i % 3]
    put(x, G, Z0 + 1, grate)
    north_trees.append((x, Z0 + 1))
# A hoop fence round the pit at the third tree.
px, pz = north_trees[2]
for dx in (-1, 0, 1):
    for dz in (-1, 0, 1):
        if dx or dz:
            put(px + dx, Y, pz + dz, 'tree_pit_fence')
# Benches between the trees, facing the road, and bins.
for x, _ in north_trees[:-1]:
    put(x + 4, Y, Z0 + 2, 'park_bench_wood', S)
    put(x + 5, Y, Z0 + 2, 'park_bench_wood', S)
put(X0 + 9, Y, Z0 + 3, 'recycling_bin', S)
put(X0 + 31, Y, Z0 + 3, 'trash_recycling_station', S)
put(X0 + 53, Y, Z0 + 3, 'parktrashcan', UD[S])

# South sidewalk: a row of pleached lindens that joins into a hedge on stilts, and a young
# staked tree at each end.
south_lindens = [(x, Z0 + 14) for x in range(X0 + 10, X0 + 56, 5)]
for x, z in south_lindens:
    put(x, G, z, 'tree_grate_square')
young = [(X0 + 2, Z0 + 14), (X0 + 64, Z0 + 14)]
for x, z in young:
    put(x, G, z, 'tree_pit_mulch')
    for y in range(Y, Y + 4):
        put(x, y, z, 'tree_log_ginkgo_thin', 1)
    put(x, Y + 4, z, 'tree_leaves_ginkgo')
    put(x, Y + 5, z, 'tree_leaves_ginkgo')
    put(x - 1, Y + 4, z, 'tree_leaves_ginkgo')
    put(x + 1, Y + 4, z, 'tree_leaves_ginkgo')
    put(x - 1, Y, z, 'tree_stake', E)
    put(x + 1, Y, z, 'tree_stake', W)
# Brick pillars at the street's ends with hanging baskets.
for x in (XS + 1, XE - 1):
    for z in (Z0 + 1, Z0 + 14):
        for y in range(Y, Y + 4):
            put(x, y, z, 'minecraft:stonebrick')
        put(x, Y + 2, z - 1, 'hanging_basket_petunia', N)
        put(x, Y + 2, z + 1, 'hanging_basket_mixed', S)
sign(X0 - 3, Z0 - 1, ["Parks & Greenery", "Street trees:", "leaning over", "the road"], 8)
flush()

# ------------------------------------------------------------------------------------------
# Park: z 18..60, x X0..X0+70
# ------------------------------------------------------------------------------------------
PZ0, PZ1 = Z0 + 18, Z0 + 62
PX0, PX1 = X0, X0 + 70
CX, CZ = X0 + 35, Z0 + 40      # the fountain plaza's centre
# Hedge border with gaps at the paths.
for x in range(PX0, PX1 + 1):
    for z in (PZ0, PZ1):
        if abs(x - CX) > 2:
            put(x, Y, z, 'hedge_boxwood_low')
for z in range(PZ0, PZ1 + 1):
    for x in (PX0, PX1):
        if abs(z - CZ) > 2:
            put(x, Y, z, 'hedge_boxwood_low')
# Paths of decomposed granite, a pea gravel plaza.
for x in range(PX0 + 1, PX1):
    for dz in (-1, 0, 1):
        put(x, Y, CZ + dz, 'ground_decomposed_granite')
for z in range(PZ0, PZ1 + 1):
    for dx in (-1, 0, 1):
        put(CX + dx, Y, z, 'ground_decomposed_granite')
for x in range(CX - 6, CX + 7):
    for z in range(CZ - 6, CZ + 7):
        put(x, Y, z, 'ground_pea_gravel')
# Fountain: a 5x5 basin with the tiered fountain in the middle.
for x in range(CX - 2, CX + 3):
    for z in range(CZ - 2, CZ + 3):
        put(x, Y, z, 'fountain_tiered' if (x, z) == (CX, CZ) else 'fountain_basin')
# Benches round the plaza, facing the fountain, in runs of three.
for dx in (-1, 0, 1):
    put(CX + dx, Y, CZ - 5, 'park_bench_wood', S)
    put(CX + dx, Y, CZ + 5, 'park_bench_wood', N)
for dz in (-1, 0, 1):
    put(CX - 5, Y, CZ + dz, 'park_bench_steel', E)
    put(CX + 5, Y, CZ + dz, 'park_bench_steel', W)
# Flower beds at the plaza's corners, planters at the path mouths.
for sx in (-1, 1):
    for sz in (-1, 1):
        for a in range(2):
            for b in range(2):
                put(CX + sx * (5 + a), Y, CZ + sz * (5 + b),
                    ['flower_bed_red', 'flower_bed_yellow', 'flower_bed_purple',
                     'flower_bed_mixed'][(sx > 0) * 2 + (sz > 0)])
for x, z in ((CX - 2, PZ0 + 1), (CX + 2, PZ0 + 1), (CX - 2, PZ1 - 1), (CX + 2, PZ1 - 1)):
    put(x, Y, z, 'planter_corten')
for x, z in ((PX0 + 1, CZ - 2), (PX0 + 1, CZ + 2), (PX1 - 1, CZ - 2), (PX1 - 1, CZ + 2)):
    put(x, Y, z, 'largeflowerpot', UD[N])
flush()

# NW quadrant: the playground.
for x in range(PX0 + 4, PX0 + 22):
    for z in range(PZ0 + 3, PZ0 + 15):
        put(x, Y, z, 'ground_rubber_safety')
put(PX0 + 6, Y, PZ0 + 8, 'playground_slide', N)
put(PX0 + 10, Y, PZ0 + 6, 'parkswinga', UD[S])
put(PX0 + 13, Y, PZ0 + 6, 'parkswingb', UD[S])
put(PX0 + 17, Y, PZ0 + 7, 'spring_rider', S)
put(PX0 + 19, Y, PZ0 + 7, 'spring_rider', S)
put(PX0 + 15, Y, PZ0 + 12, 'teetertotter', UD[S])
for x in range(PX0 + 8, PX0 + 11):
    put(x, Y, PZ0 + 16, 'park_bench_backless', N)
put(PX0 + 12, Y, PZ0 + 16, 'recycling_bin', N)
put(PX0 + 22, Y, PZ0 + 16, 'wbt', UD[N])
put(PX0 + 23, Y, PZ0 + 16, 'wbs', UD[N])
sign(PX0 + 4, PZ0 + 16, ["Playground"], 8)

# NE quadrant: a 7x7 pergola with picnic tables under it, a garden round it.
GX0, GZ0 = PX1 - 22, PZ0 + 4
for x in (GX0, GX0 + 6):
    for z in (GZ0, GZ0 + 6):
        for y in range(Y, Y + 3):
            put(x, y, z, 'pergola_post')
for x in range(GX0, GX0 + 7):
    for z in range(GZ0, GZ0 + 7):
        put(x, Y + 3, z, 'pergola_top')
        if (x, z) not in ((GX0, GZ0), (GX0 + 6, GZ0), (GX0, GZ0 + 6), (GX0 + 6, GZ0 + 6)):
            put(x, Y, z, 'ground_turf')
for x in (GX0 + 2, GX0 + 3):
    put(x, Y, GZ0 + 2, 'picnic_table_wood', E)
    put(x, Y, GZ0 + 4, 'picnic_table_wood', E)
put(GX0 + 5, Y, GZ0 + 3, 'picnic_table_steel', N)
for z in range(GZ0 - 1, GZ0 + 8):
    put(GX0 - 1, Y, z, 'hedge_privet_tall')
    put(GX0 + 7, Y, z, 'hedge_privet_tall')
for x in range(GX0 + 8, GX0 + 18, 2):
    put(x, Y, GZ0, ['shrub_boxwood', 'shrub_hydrangea', 'shrub_juniper'][x % 3])
    put(x, Y, GZ0 + 3, ['grass_fountain', 'grass_feather_reed', 'grass_blue_fescue'][x % 3])
    put(x, Y, GZ0 + 6, 'shrub_hydrangea')
put(GX0 + 10, Y, GZ0 + 9, 'birdbath', UD[N])
put(GX0 + 14, Y, GZ0 + 9, 'roundflowerpot', UD[N])
sign(GX0 + 3, GZ0 - 2, ["Pergola &", "picnic tables"], 8)

# SW quadrant: a community garden of raised beds, and a pond.
BX0, BZ0 = PX0 + 4, CZ + 5
for i, mat in enumerate(('concrete', 'wood', 'corten')):
    for x in range(BX0 + i * 6, BX0 + i * 6 + 4):
        for z in range(BZ0, BZ0 + 2):
            put(x, Y, z, 'raised_bed_' + mat)
    put(BX0 + i * 6 + 1, Y, BZ0 + 3, 'planter_' + mat)
for x in range(BX0, BX0 + 16):
    put(x, Y, BZ0 + 5, 'ground_mulch')
    if x % 2 == 0:
        put(x, Y, BZ0 + 6, 'grass_blue_fescue')
sign(BX0, BZ0 - 1, ["Community", "garden"], 8)
# The pond: water sunk one block into the lawn, a willow beside it.
QX, QZ = PX0 + 8, PZ1 - 9
for x in range(QX, QX + 7):
    for z in range(QZ, QZ + 5):
        put(x, G, z, 'minecraft:water')
        put(x, G - 1, z, 'minecraft:dirt')

# SE quadrant: a lawn watered on the morning schedule, and the dog-walkers' corner.
LX0, LZ0 = CX + 13, CZ + 6
for y in range(Y, Y + 2):
    for x in (LX0, LX0 + 1):
        put(x, y, LZ0, 'minecraft:stonebrick')
put(LX0, Y + 1, LZ0 + 1, 'irrigation_controller', S)
put(LX0 + 1, Y, LZ0 + 1, 'backflow_preventer', S)
for z in range(LZ0 + 1, LZ0 + 12):
    put(LX0, Y, z, 'minecraft:redstone_wire')
for z in range(LZ0 + 3, LZ0 + 11, 4):
    put(LX0 - 1, Y, z, 'irrigation_sprinkler')
    put(LX0 + 1, Y, z, 'irrigation_sprinkler')
for x in range(LX0, LX0 + 11):
    put(x, Y, LZ0 + 11, 'minecraft:redstone_wire')
for x in range(LX0 + 4, LX0 + 11, 4):
    put(x, Y, LZ0 + 10, 'irrigation_sprinkler')
    put(x, Y, LZ0 + 12, 'irrigation_sprinkler')
sign(LX0 + 3, LZ0, ["Irrigation:", "5-7 am, or", "right-click the", "controller"], 8)
put(PX1 - 4, Y, PZ1 - 3, 'dog_waste_station', W)
put(PX1 - 4, Y, PZ1 - 6, 'parktrashcan', UD[W])
for z in (PZ1 - 5, PZ1 - 4):
    put(PX1 - 6, Y, z, 'park_bench_backless', E)
flush()

# ------------------------------------------------------------------------------------------
# The tree kit, north of the street: every leaves block on a plinth, hand-built leans
# ------------------------------------------------------------------------------------------
KZ = Z0 - 12
LEAVES = ['liveoak', 'elm', 'elm_autumn', 'plane', 'plane_autumn', 'honeylocust',
          'honeylocust_autumn', 'ginkgo', 'ginkgo_autumn', 'cypress', 'jacaranda',
          'jacaranda_blossom', 'pepper', 'poplar', 'poplar_autumn', 'sweetgum', 'sweetgum_autumn',
          'hornbeam', 'gum', 'willow', 'arborvitae', 'linden', 'linden_clipped']
for i, leaf in enumerate(LEAVES):
    x = X0 - 4 + i * 3
    put(x, Y, KZ, 'minecraft:stonebrick')
    put(x, Y + 1, KZ, 'tree_leaves_' + leaf)
    sign(x, KZ + 1, [leaf.replace('_', ' ').title()], 0)
for i, crown in enumerate(('palm_fan', 'palm_fan_skirt', 'palm_feather')):
    x = X0 + 66 + i * 5
    for y in range(Y, Y + 3):
        put(x, y, KZ, 'tree_log_palm_thin', 1)
    put(x, Y + 3, KZ, 'tree_crown_' + crown)
    sign(x, KZ + 1, [crown.replace('_', ' ').title()], 0)
# Hand-built leans, one per width, stepping diagonally north: the kit bridges each step.
WOODS = ['liveoak', 'elm', 'plane', 'honeylocust', 'cypress', 'ginkgo', 'palm', 'jacaranda',
         'pepper', 'poplar', 'sweetgum', 'hornbeam', 'gum', 'willow', 'linden']
for i, width in enumerate(('twig', 'thin', 'medium', 'thick', 'full')):
    x = X0 + 2 + i * 6
    wood = WOODS[i * 3]
    steps = [(0, 0), (0, 1), (0, 2), (-1, 3), (-1, 4), (-2, 5), (-3, 6)]
    for dz, dy in steps:
        put(x, Y + dy, KZ - 6 + dz, 'tree_log_%s_%s' % (wood, width), 1)
    put(x, Y + 7, KZ - 9, 'tree_leaves_' + wood if wood != 'palm' else 'tree_crown_palm_fan')
    sign(x + 1, KZ - 5, [width.title() + ' log', wood.title(), 'leaning by', 'diagonal steps'], 0)
# Every bark, one medium post each, with moss and willow strands hung from a gallows.
for i, wood in enumerate(WOODS):
    x = X0 + 34 + i * 2
    for y in range(Y, Y + 3):
        put(x, y, KZ - 6, 'tree_log_%s_medium' % wood, 1)
    sign(x, KZ - 5, [wood.title()], 0)
for x in range(X0 + 66, X0 + 72):
    put(x, Y + 5, KZ - 6, 'tree_log_liveoak_medium', 0)
for y in range(Y, Y + 5):
    put(X0 + 66, y, KZ - 6, 'tree_log_liveoak_medium', 1)
for y in range(Y + 1, Y + 5):
    put(X0 + 68, y, KZ - 6, 'spanish_moss')
    put(X0 + 70, y, KZ - 6, 'willow_strands')
flush()

# ------------------------------------------------------------------------------------------
# Planting: the Tree Planting Tool, driven as a player would
# ------------------------------------------------------------------------------------------
PRESETS = ['liveoak', 'elm', 'plane', 'honeylocust', 'cypress', 'ginkgo', 'fanpalm',
           'leaningpalm', 'lollipopplane', 'jacaranda', 'peppertree', 'coastliveoak',
           'weepingwillow', 'poplar', 'sweetgum', 'hornbeam', 'queenpalm', 'lemongum',
           'arborvitae', 'pleachedlinden', 'pollardedplane']
NAMES = ['Southern Live Oak', 'American Elm', 'London Plane', 'Honey Locust', 'Italian Cypress',
         'Ginkgo', 'Mexican Fan Palm', 'Leaning Feather Palm', 'Ball-Head Plane', 'Jacaranda',
         'Pepper Tree', 'Coast Live Oak', 'Weeping Willow', 'Lombardy Poplar', 'Slender Sweetgum',
         'Columnar Hornbeam', 'Queen Palm', 'Lemon-scented Gum', 'Emerald Arborvitae',
         'Pleached Linden', 'Pollarded Plane']
PLAYER = _me['name']
client.call('client_select_slot', slot=0)
client.call('client_view', hideHud=True)
planted, refused = [], []


def _aimed(x, z):
    got = json.loads(client.call('client_looking_at'))
    p = (got.get('block') or {}).get('position') or {}
    return (p.get('x'), p.get('z')) == (x, z) and got.get('face') == 'up'


def _try(preset, x, z, facing):
    server.call('server_run_command',
                command='replaceitem entity @p slot.hotbar.0 csm:tree_planting_tool 1 0 '
                        '{csm_tree_preset:%d}' % PRESETS.index(preset))
    dx, dz = DIR[facing]
    server.call('server_teleport_player', player=PLAYER, x=x + 0.5 - dx, y=Y,
                z=z + 0.5 - dz, fly=True)
    time.sleep(0.15)
    client.call('client_look', yaw=YAW[facing], pitch=60)
    time.sleep(0.1)
    if not _aimed(x, z):
        client.call('client_look', lookAtX=x + 0.5, lookAtY=G + 1.0, lookAtZ=z + 0.5)
        time.sleep(0.1)
        if not _aimed(x, z):
            return False
    client.call('client_interact', action='use')
    time.sleep(0.25)
    got = server.call('server_get_block', x=x, y=Y, z=z)
    return 'tree_log' in got or 'tree_crown' in got


def plant(preset, x, z, facing):
    """Plants, retrying a block either side across the facing if the spot will not take it."""
    sx, sz = -DIR[facing][1], DIR[facing][0]
    for step in (0, 0, 1, -1, 2, -2):
        if _try(preset, x + sx * step, z + sz * step, facing):
            planted.append((preset, x + sx * step, z + sz * step))
            return True
    refused.append((preset, x, z))
    return False


# Street: the south row first (small), then the leaning trees on the north sidewalk.
for x, z in south_lindens:
    plant('pleachedlinden', x, z, N)
for (x, z), preset in zip(north_trees, ['liveoak', 'elm', 'honeylocust', 'plane', 'jacaranda',
                                         'liveoak', 'elm']):
    plant(preset, x, z, S)
# Park trees.
plant('weepingwillow', QX + 3, QZ - 3, S)
plant('coastliveoak', PX0 + 30, PZ1 - 6, W)
plant('peppertree', PX1 - 12, PZ1 - 12, N)
for z in range(PZ0 + 4, PZ1 - 2, 6):
    plant('poplar', PX1 - 2, z, W)
for x in range(PX0 + 26, PX0 + 32, 5):
    plant('hornbeam', x, PZ0 + 4, S)
plant('lemongum', GX0 - 5, GZ0 + 14, S)
plant('lollipopplane', CX - 9, CZ - 9, S)
plant('lollipopplane', CX + 9, CZ - 9, S)
plant('pollardedplane', CX - 9, CZ + 9, S)
plant('pollardedplane', CX + 9, CZ + 9, S)
# The arboretum: every preset, three rows of seven, each with a sign.
AX0, AZ0 = X0 - 108, Z0 + 20
for i, preset in enumerate(PRESETS):
    x = AX0 + (i % 7) * 14
    z = AZ0 + (i // 7) * 16
    plant(preset, x, z, S)
    sign(x + 2, z - 1, [NAMES[i], "(preset %d)" % i], 8)
    flush()
sign(AX0 - 3, AZ0 - 3, ["Arboretum:", "every Tree", "Planting Tool", "preset"], 8)
flush()
print('planted %d, refused %d' % (len(planted), len(refused)))
for r in refused:
    print('  refused', r)

# ------------------------------------------------------------------------------------------
# Spawn: on the north sidewalk, looking down the street
# ------------------------------------------------------------------------------------------
server.call('server_run_command', command='setworldspawn %d %d %d' % (X0 - 3, Y, Z0 - 2))
server.call('server_teleport_player', player=PLAYER, x=X0 - 3.5, y=Y, z=Z0 - 1.5, fly=False)
client.call('client_look', yaw=-90, pitch=5)
print('done')
