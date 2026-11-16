#!/usr/bin/env python3
"""
build_bus_station_demo.py -- builds the Bus Station demo world, over the MCMCP orchestrator.

Create a fresh FLAT creative world (for example "Bus Station Demo") in a dev client with Transit
loaded (Signage for the shelters' ad panels and Vehicles for the buses are used when present),
then run this. It lays out the real-scale bus shelters and the station canopy kit to walk round:

  shelters  (z 0..16)   a street: every shelter style in every livery, two rows deep or one, three
                        to six blocks tall, Signage's ad panel against each glass shelter, a stop
                        flag at the curb and a sign saying what each one is
  canopies  (z 22..80)  the station canopy kit, each laid by the Canopy Tool as a player would:
                        a 30 x 14 flat two-bus bay at 9 clear with an island and a bus lane, a
                        glass canopy spanning a two-lane road, the sawtooth, butterfly and vault
                        styles, a small 4-clear canopy and a 12-clear one; photocells (inverted
                        daylight sensors) on the roofs of the bay and the vault, so their lights
                        come on at night; transit buses parked under the bay and the road canopy

The site is placed relative to the player (--x/--z to move it). The game is named by its MCMCP
instance id (--instance, default the CSM dev client 'run-f2eb58'), never by port.

Usage:
    python build_bus_station_demo.py [--instance run-f2eb58] [--x X --z Z] [--no-buses]
"""
import argparse
import json
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmcp_orchestrator import Orchestrator, OrchestratorError  # noqa: E402

ap = argparse.ArgumentParser(description="Build the Bus Station demo world.")
ap.add_argument("--instance", default="run-f2eb58", help="MCMCP game id (without .client)")
ap.add_argument("--x", type=int, default=None)
ap.add_argument("--z", type=int, default=None)
ap.add_argument("--no-buses", action="store_true")
ARGS = ap.parse_args()

O = Orchestrator()
CLIENT = ARGS.instance + ".client"
SERVER = ARGS.instance + ".server"


def server(tool, **kw):
    return O.call(tool, instance=SERVER, **kw)


def client(tool, **kw):
    return O.call(tool, instance=CLIENT, **kw)


_players = server("server_list_players")["players"]
PLAYER = _players[0] if isinstance(_players[0], str) else _players[0]["name"]
_me = server("server_player_state", player=PLAYER)
X0 = ARGS.x if ARGS.x is not None else int(_me["position"]["x"]) + 4
Z0 = ARGS.z if ARGS.z is not None else int(_me["position"]["z"]) + 4

G, Y = 3, 4        # the flat world's ground block, and where things stand
S, W, N, E = 0, 1, 2, 3          # horizontal facing index (shelters)
SIGN_FACING = {S: 0, W: 1, N: 2, E: 3}

blocks = []


def put(x, y, z, block, meta=0, nbt=None):
    name = block if ":" in block else "csm:" + block
    b = {"x": X0 + x, "y": y, "z": Z0 + z, "block": name, "metadata": meta}
    if nbt:
        b["nbt"] = nbt
    blocks.append(b)


def flush():
    global blocks
    for i in range(0, len(blocks), 800):
        server("server_set_blocks", mode="list", blocks=blocks[i:i + 800], pairTwoPart=False,
               load=True)
    blocks = []


def fill(x0, y0, z0, x1, y1, z1, block, meta=0):
    flush()
    name = block if ":" in block else "csm:" + block
    # in slabs of x, so no single fill passes the server's batch limit
    for xa in range(x0, x1 + 1, 24):
        xb = min(x1, xa + 23)
        server("server_set_blocks", mode="fill", block=name, metadata=meta, x=X0 + xa, y=y0,
               z=Z0 + z0, toX=X0 + xb, toY=y1, toZ=Z0 + z1, load=True)


def cmds(*cs):
    return server("server_run_commands", commands=list(cs), load=False, allResults=True)


def sign(x, z, lines, rot=0):
    """A standing sign; rot 0 faces south, 8 north."""
    nbt = {"Text%d" % (i + 1): json.dumps({"text": t}) for i, t in enumerate(lines[:4])}
    snbt = "{" + ",".join("%s:%s" % (k, json.dumps(v)) for k, v in nbt.items()) + "}"
    put(x, Y, z, "minecraft:standing_sign", rot, snbt)


# ------------------------------------------------------------------------------------------
# World rules and the ground
# ------------------------------------------------------------------------------------------
# creative: the Canopy Tool and the stop pole take their pieces from the inventory in survival
cmds("gamemode 1 %s" % PLAYER)
cmds("gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
     "gamerule doMobSpawning false", "difficulty peaceful", "time set 6000",
     "weather clear 1000000", "kill @e[type=!player]")
server("server_teleport_player", player=PLAYER, x=X0 + 10.5, y=30, z=Z0 - 10.5, fly=True)

SX0, SX1, SZ0, SZ1 = -6, 132, -6, 84
fill(SX0, Y, SZ0, SX1, Y + 24, SZ1, "minecraft:air")
fill(SX0, G, SZ0, SX1, G, SZ1, "minecraft:concrete", 8)          # paving everywhere


def road(x0, x1, z0, z1, lanes_dashed=True):
    fill(x0, G, z0, x1, G, z1, "minecraft:concrete", 15)
    for x in range(x0, x1 + 1):
        put(x, G, z0, "minecraft:concrete", 0)
        put(x, G, z1, "minecraft:concrete", 0)
        if lanes_dashed and x % 4 < 2:
            put(x, G, (z0 + z1) // 2, "minecraft:concrete", 4)


# ------------------------------------------------------------------------------------------
# The shelter street: sidewalk z 0..9, road z 10..16; shelters face the road (south)
# ------------------------------------------------------------------------------------------
road(SX0, SX1, 10, 16)

SHELTERS = [
    # style, agency, height, rows, x0
    ("glass", "cityline", 3, 2, 2),
    ("glass", "riverway", 4, 2, 10),
    ("glass", "verdant", 3, 1, 18),
    ("glass", "emberline", 6, 2, 26),
    ("cantilever", "cityline", 3, 2, 36),
    ("cantilever", "riverway", 4, 2, 44),
    ("cantilever", "verdant", 3, 1, 52),
    ("cantilever", "emberline", 5, 2, 60),
    ("flat", "cityline", 3, 2, 70),
    ("flat", "riverway", 3, 1, 78),
    ("flat", "verdant", 4, 2, 86),
    ("flat", "emberline", 6, 2, 94),
]
FRONT_Z = 7      # the front row; a second row stands behind it at z 6
LENGTH = 4

for style, agency, height, rows, x0 in SHELTERS:
    reg = "bus_shelter_%s_%s" % (style, agency)
    for x in range(x0, x0 + LENGTH):
        for r in range(rows):
            z = FRONT_Z - r
            for y in range(Y, Y + height - 1):
                put(x, y, z, reg, S)
            put(x, Y + height - 1, z, reg, S | 4 | 8)
    sign(x0 + 1, 9, ["%s shelter" % style.capitalize(), agency.upper(),
                     "%d tall, %d deep" % (height, rows), "click: taller"], 0)
    # a stop flag at the curb, west of the shelter
    put(x0 - 1, Y, 9, "signpost", E)
    put(x0 - 1, Y + 1, 9, "bus_stop_timetable_case", E)
    put(x0 - 1, Y + 2, 9, "bus_stop_flag_" + agency, E, "{r:[B;%db,%db,0b]}" % (x0 % 40 + 2,
                                                                               x0 % 17 + 30))
flush()
sign(-3, 9, ["Bus shelters at", "real scale: roof", "2.75 m up at 3", "tall, 2 rows deep"], 0)
flush()


def place_as_player(item, target, face, stand, yaw, pitch):
    """Put item in hand, stand at `stand`, look, and right-click `target`'s face."""
    cmds("replaceitem entity %s slot.hotbar.0 %s 1 0" % (PLAYER, item))
    client("client_select_slot", slot=0)
    server("server_teleport_player", player=PLAYER, x=X0 + stand[0] + 0.5, y=stand[1],
           z=Z0 + stand[2] + 0.5, fly=True)
    time.sleep(0.3)
    try:
        client("client_use_on_block", x=X0 + target[0], y=target[1], z=Z0 + target[2],
               face=face, yaw=yaw, pitch=pitch)
    except OrchestratorError as e:
        print("  place %s: %s" % (item, e))
    time.sleep(0.4)
    try:
        client("client_gui_close")
    except OrchestratorError:
        pass


# Signage's ad panel against each glass shelter's slot: the sitter's left is east here, so the
# frame is at the east end of the back row and the panel stands in the block east of it.
client("client_input_lock", locked=True, reason="building the bus station demo", seconds=900)
try:
    for style, agency, height, rows, x0 in SHELTERS:
        if style != "glass":
            continue
        back = FRONT_Z - rows + 1
        place_as_player("csm:ad_shelter_panel", (x0 + LENGTH - 1, Y, back), "east",
                        (x0 + LENGTH + 1, Y, back), 90, 10)

    # --------------------------------------------------------------------------------------
    # The canopies, each laid by the Canopy Tool
    # --------------------------------------------------------------------------------------
    def canopy(deck, clearance, x0, z0, x1, z1, column="canopy_column_steel", lights=0):
        """lights: 0 downlights, 1 a strip down the middle, 2 strips along the long edges."""
        cmds("replaceitem entity %s slot.hotbar.0 csm:canopy_tool 1 0 {clearance:%d,lights:%d}"
             % (PLAYER, clearance, lights),
             "replaceitem entity %s slot.hotbar.1 csm:%s 64" % (PLAYER, column),
             "replaceitem entity %s slot.hotbar.2 csm:%s 64"
             % (PLAYER, "canopy_light_strip" if lights else "canopy_downlight"),
             "replaceitem entity %s slot.weapon.offhand csm:%s 64" % (PLAYER, deck))
        server("server_teleport_player", player=PLAYER, x=X0 + x0 - 2.5, y=Y,
               z=Z0 + z0 - 2.5, fly=True)
        for (x, z) in ((x0, z0), (x1, z1)):
            r = server("server_use_item_on_block", player=PLAYER, x=X0 + x, y=G, z=Z0 + z,
                       face="up", slot=0)
            if r.get("result") != "success":
                print("  canopy %s: %s" % (deck, r))
        cmds("replaceitem entity %s slot.weapon.offhand minecraft:air" % PLAYER)

    # the two-bus bay: an island z 22..25, a bus lane z 26..32, a sidewalk z 33..35, under a
    # flat canopy at 9 clear, 14 deep, whose two column rows (two in from each edge) stand on
    # the island and the far sidewalk, clear of the lane
    road(0, 29, 26, 32, lanes_dashed=False)
    canopy("canopy_deck_flat_cityline", 9, 0, 22, 29, 35)
    # a road spanning glass canopy at 6 clear over two lanes
    road(36, 63, 24, 31)
    canopy("canopy_deck_glass_riverway", 6, 38, 22, 61, 33, "canopy_column_white")
    canopy("canopy_deck_sawtooth_verdant", 6, 70, 22, 87, 30)
    canopy("canopy_deck_butterfly_emberline", 7, 94, 22, 117, 29, "canopy_column_graphite")
    canopy("canopy_deck_vault_cityline", 8, 0, 44, 29, 55, "canopy_column_white")
    canopy("canopy_deck_flat_verdant", 4, 38, 44, 45, 49)
    canopy("canopy_deck_glass_emberline", 12, 52, 44, 67, 55, "canopy_column_graphite")
    cmds("clear %s" % PLAYER)
    # photocells: inverted daylight sensors on the bay's and the vault's roofs, each over a
    # light (the tool hangs them from a block in, every third block; under the vault only at
    # the springing). The lights are placed lit; switch each canopy off once so the photocell
    # starts in step with the daylight.
    for (x, clearance, z) in ((1, 9, 23), (1, 8, 49)):
        put(x, Y + clearance + 1, z, "minecraft:daylight_detector_inverted")
        flush()
        server("server_activate_block", player=PLAYER, x=X0 + x, y=Y + clearance - 1,
               z=Z0 + z, face="down")

    sign(-3, 24, ["Two-bus bay", "Flat, 30 x 14", "9 clear", "photocell on roof"], 0)
    sign(36, 22, ["Over the road", "Glass skylight", "24 x 12, 6 clear"], 0)
    sign(68, 22, ["Sawtooth", "3-block module", "18 x 9, 6 clear"], 0)
    sign(92, 22, ["Butterfly", "4-block module", "24 x 8, 7 clear"], 0)
    sign(-3, 44, ["Barrel vault", "6-block module", "30 x 12, 8 clear", "photocell on roof"], 0)
    sign(36, 44, ["Flat, small", "8 x 6, 4 clear"], 0)
    sign(50, 44, ["Glass skylight", "16 x 12", "12 clear"], 0)
    sign(-3, 60, ["Canopy Tool:", "deck in off hand,", "click 2 corners;", "sneak-air: height"], 0)
    flush()

    # --------------------------------------------------------------------------------------
    # A full-size stop (z 64..76), facing south, as the live UWS bays are built: a strip-lit
    # canopy with a two-sided 5x2 arrival board hung under it, the full-size pole beside it, an
    # info totem, the outdoor fare machine and a compact shelter
    # --------------------------------------------------------------------------------------
    def use(item, target, face, nbt=""):
        """Right-click target's face holding item, as the player standing south of it, facing
        north, so what is placed faces south."""
        cmds("replaceitem entity %s slot.hotbar.0 %s 1 0 %s" % (PLAYER, item, nbt))
        server("server_teleport_player", player=PLAYER, x=X0 + target[0] + 0.5, y=Y,
               z=Z0 + target[2] + 6.5, yaw=180, pitch=20, fly=True)
        r = server("server_use_item_on_block", player=PLAYER, x=X0 + target[0], y=target[1],
                   z=Z0 + target[2], face=face, slot=0)
        if r.get("result") != "success":
            print("  %s: %s" % (item, r))

    canopy("canopy_deck_flat_cityline", 8, 0, 66, 9, 72, lights=1)
    deck_y = Y + 8
    # the board, hung from the deck's underside two blocks from the strip; the master is the
    # bottom-centre cell
    use("csm:bus_arrival_board_5x2", (5, deck_y, 70), "down")
    board = ('{ag:0b,sn:"Riverside",dr:"Northbound",rt:"B 30",'
             'dt:["Upper West Side","Downtown"]}')
    cmds("blockdata %d %d %d %s" % (X0 + 5, deck_y - 2, Z0 + 70, board))
    # the pole: posts at x 12, the flag beside them at x 13 (the reader's right), routes B 30
    use("csm:bus_stop_pole_flag", (12, G, 70), "up")
    flag = ('{l1:"BUS",l2:"STOP",l3:"CITYLINE",l4:"B 30",ts3:0.55f,ts4:0.8f,il1:"bus",'
            'p:"none",a:"none",s:"transit",d:1b,al:"c",m:"ceiling",bg:"#0E5E6F",fg:"#F4C428"}')
    blade = ('{l1:"RIVERSIDE",l2:"CITYLINE",p:"none",a:"none",s:"transit",d:1b,al:"c",'
             'm:"ceiling",bg:"#0E5E6F",fg:"#F4C428"}')
    for y in (Y + 2, Y + 3):
        cmds("blockdata %d %d %d %s" % (X0 + 13, y, Z0 + 70, flag))
    for x in (12, 13):
        cmds("blockdata %d %d %d %s" % (X0 + x, Y + 4, Z0 + 70, blade))
    # the totem, the outdoor fare machine and a compact (3 x 1) glass shelter
    use("csm:bus_info_totem", (16, G, 70), "up")
    cmds("blockdata %d %d %d %s" % (X0 + 16, Y, Z0 + 70, board))
    use("csm:fare_vending_machine_outdoor", (18, G, 70), "up")
    use("csm:bus_shelter_glass_cityline", (22, G, 70), "up", "{compact:1b}")
    cmds("clear %s" % PLAYER)
    sign(-3, 66, ["Full-size stop:", "strip-lit canopy,", "5x2 arrival board,", "pole, totem"], 0)
    sign(21, 72, ["Compact shelter", "3 x 1, 3 tall", "sneak-use air", "to switch"], 0)
    flush()

    # ------------------------------------------------------------------------------------------
    # Buses under the bay and the road canopy (Vehicles), placed as a player places them
    # ------------------------------------------------------------------------------------------
    def bus(agency, x, z):
        r = cmds("give %s mts:csmvehicles.csm_transit_bus_%s" % (PLAYER, agency))
        if not r["results"] or not r["results"][0].get("succeeded"):
            return False
        client("client_select_slot", slot=0)
        server("server_teleport_player", player=PLAYER, x=X0 + x - 3 + 0.5, y=Y,
               z=Z0 + z + 0.5, yaw=270, pitch=30, fly=False)
        client("client_wait", waitFor="chunksLoaded", radius=16, ticks=200)
        time.sleep(0.3)
        client("client_look", lookAtX=X0 + x + 0.5, lookAtY=Y, lookAtZ=Z0 + z + 0.5)
        client("client_interact", action="use")
        time.sleep(0.5)
        cmds("clear %s" % PLAYER)
        return True

    if not ARGS.no_buses:
        cmds("clear %s" % PLAYER)
        placed = [bus("cityline", 8, 29), bus("riverway", 22, 29), bus("emberline", 46, 26),
                  bus("verdant", 54, 29)]
        print("buses placed:", sum(placed))
finally:
    client("client_input_lock", locked=False)

server("server_teleport_player", player=PLAYER, x=X0 - 4.5, y=Y + 1, z=Z0 + 20.5, yaw=-60,
       pitch=10, fly=True)
print("built at", X0, Z0)
O.close()
