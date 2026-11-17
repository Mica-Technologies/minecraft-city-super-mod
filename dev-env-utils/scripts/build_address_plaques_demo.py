#!/usr/bin/env python3
"""Builds the address plaques test street (GitHub #364) in a flat world named
"Address Plaques 364".

    py -3 dev-env-utils/scripts/build_address_plaques_demo.py --instance run-xxxxxx [--create]

Through the MCMCP orchestrator by instance id (mcmcp_orchestrator.py), in a dev client with the
Building module. Refuses any world not named "Address Plaques 364"; --create makes one (flat)
first. Sets peaceful, no mob spawning, fixed time.

- **Three UWS-style brownstones** (x 0..17, the facade on z 0, the street to the south): each a
  stoop up to a CSM front door two blocks above the pavement, a transom pane over the door with
  gold-leaf numbers on it, a hood (top slabs) over the transom, a plaque beside the door at the
  door's upper half and an illuminated number box on the other side. The third house has a
  one-block pier between two windows carrying a vertical stack.
- **A gallery wall** (x 22..58, z 0): every plate style with 2, 3, 4 and 5 digits (one with a
  street line), each also as a vertical stack, and the stand-off numerals in four inks.
- **The windows street** (x 65..175, z 0; #357, ``windows()``): every glazing window finish in
  every pattern in a brick wall, 2- and 3-tall stacks at each depth with every sill and head,
  opening sashes and arched tops, a two-storey brownstone with a canted and a 90-degree bay
  (spandrel panels at the floor line), and a canted bay in every finish.

It places blocks, then writes each plaque's and window's NBT exactly as the editor or the
window's clicks would (a block placed by hand and set up reads the same), checks every plaque
and window block was placed, and leaves the player three blocks off the first stoop. For night
shots: ``/time set 18000``.
"""

import argparse
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmcp_orchestrator import Orchestrator  # noqa: E402

import gen_address_plaques  # noqa: E402
import gen_glazing_windows  # noqa: E402

WORLD = "Address Plaques 364"
FLOOR = 3            # the flat world's top block
WALL_N = 2           # plaque metadata: the wall is to the north (the facade at z 0)
BROWNSTONE = ("minecraft:stained_hardened_clay", 12)
STOOP = ("minecraft:red_sandstone", 0)
STAIR = "minecraft:red_sandstone_stairs"
STAIR_UP_NORTH = 3   # stairs climbing toward the door
HOOD = ("minecraft:stone_slab", 8 + 5)   # top-half stone brick slab


class Build:
    def __init__(self):
        self.blocks = []
        self.nbt = []
        self.placed = set()

    def put(self, x, y, z, block, meta=0):
        if not block.startswith("minecraft:"):
            self.placed.add(block)
            block = "csm:" + block
        self.blocks.append({"x": x, "y": y, "z": z, "block": block, "metadata": meta})

    def fill(self, x0, y0, z0, x1, y1, z1, block, meta=0):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.put(x, y, z, block, meta)

    def data(self, x, y, z, **nbt):
        """Tile entity data written after every block is placed: strings, else bytes."""
        parts = []
        for k, v in nbt.items():
            parts.append('%s:%s' % (k, '"%s"' % v if isinstance(v, str) else "%db" % v))
        self.nbt.append({"x": x, "y": y, "z": z, "nbt": "{%s}" % ",".join(parts)})

    def plaque(self, x, y, z, block, **nbt):
        self.put(x, y, z, block, WALL_N)
        self.data(x, y, z, **nbt)


def house(b, x0, number, plate, lamp_number, pier):
    """A five-wide brownstone front at x0..x0+4, its door at x0+2."""
    top = FLOOR + 10
    b.fill(x0, FLOOR + 1, 0, x0 + 4, top, 0, *BROWNSTONE)
    xd = x0 + 2
    # stoop: a landing at the door's sill and two steps down to the pavement
    b.fill(xd - 1, FLOOR + 1, 1, xd + 1, FLOOR + 2, 1, *STOOP)
    b.put(xd, FLOOR + 1, 2, STOOP[0], STOOP[1])
    b.put(xd, FLOOR + 2, 2, STAIR, STAIR_UP_NORTH)
    b.put(xd, FLOOR + 1, 3, STAIR, STAIR_UP_NORTH)
    # a front door (facing north, the inside: lower half first, then the upper, hinged left;
    # DOORS.md "Placing by command"), a transom pane over it, a hood over the transom
    b.put(xd, FLOOR + 3, 0, "door_front_dark_oak", 2)
    b.put(xd, FLOOR + 4, 0, "door_front_dark_oak", 8)
    b.put(xd, FLOOR + 5, 0, "minecraft:glass_pane")
    for x in (xd - 1, xd, xd + 1):
        b.put(x, FLOOR + 6, 1, *HOOD)
    # windows over the parlour floor
    for x in (x0 + 1, x0 + 3):
        b.fill(x, FLOOR + 7, 0, x, FLOOR + 8, 0, "minecraft:glass_pane")
    b.plaque(xd + 1, FLOOR + 4, 1, plate, num=number)
    b.plaque(xd - 1, FLOOR + 4, 1, "address_lamp_box", num=lamp_number)
    b.plaque(xd, FLOOR + 5, 1, "address_transom_decal", num=number, dz=7)
    if pier:
        # a narrow pier between two windows on the parlour floor, the number stacked up it
        b.put(x0 + 2, FLOOR + 7, 0, *BROWNSTONE)
        b.put(x0 + 2, FLOOR + 8, 0, *BROWNSTONE)
        b.plaque(x0 + 2, FLOOR + 8, 1, plate, num=number, lay="v", plate="s")
    return b


PLATES = ["address_plaque_brass", "address_plaque_cast_iron", "address_plaque_enamel",
          "address_plaque_stone", "address_plaque_wood", "address_plaque_steel",
          "address_lamp_box"]
NUMBERS = ["12", "245", "1847", "12345"]


def gallery(b):
    x0 = 22
    b.fill(x0 - 1, FLOOR + 1, 0, x0 + 4 * len(NUMBERS) + 2, FLOOR + 1 + 2 * len(PLATES) + 3, 0,
           *BROWNSTONE)
    for r, block in enumerate(PLATES):
        y = FLOOR + 2 + 2 * r
        for c, number in enumerate(NUMBERS):
            x = x0 + 2 * c
            extra = {"st": "West 86th St"} if c == 1 else {}
            b.plaque(x, y, 1, block, num=number, **extra)
            b.plaque(x + 2 * len(NUMBERS) + 1, y, 1, block, num=number, lay="v")
    y = FLOOR + 2 + 2 * len(PLATES)
    for c, ink in enumerate(["brass", "black", "steel", "white"]):
        b.plaque(x0 + 2 * c, y, 1, "address_numerals", num=NUMBERS[c], ink=ink)
    b.plaque(x0 + 9, y, 1, "address_numerals", num="1847", lay="v", font="modern")


# ------------------------------------------------------------------------------------------
# Glazing windows (#357, WALL_MATERIALS.md "Glazing windows")
# ------------------------------------------------------------------------------------------
# Metadata is facing | depth << 2 (facing S0 W1 N2 E3, the outside); everything else is the
# tile entity's optional bytes. Each stack is written as hand placement and clicks leave it:
# pattern and open state on every block, sill on the foot, arch and head on the top.
FACE_S, FACE_W, FACE_N, FACE_E = 0, 1, 2, 3
CENTRED, FLUSH, INSET = 0, 1, 2
PLAIN, HUNG_1_1, HUNG_6_6, CASEMENT = 0, 1, 2, 3
SILL_NONE, SILL_STONE, SILL_FRAME = 0, 1, 2
HEAD_NONE, HEAD_LINTEL, HEAD_KEYSTONE = 0, 1, 2
BRICK = "brick_brown"
FLOOR_BOARDS = ("minecraft:planks", 1)      # spruce
CORNICE = ("minecraft:stone_slab", 8 + 0)  # top-half smooth stone slab


def window_block(finish, canted=False):
    return "glass_window_%s%s" % (finish, "_canted" if canted else "")


def stack(b, x, y, z, height, finish, facing=FACE_S, depth=CENTRED, canted=False,
          pattern=PLAIN, opened=False, sill=SILL_NONE, head=HEAD_NONE, arch=False,
          panel=False):
    """A window `height` tall from (x, y, z) up, as one stack."""
    for i in range(height):
        b.put(x, y + i, z, window_block(finish, canted), facing | depth << 2)
        nbt = {"pt": pattern, "op": int(opened)}
        if i == 0:
            nbt["sl"] = sill
        if i == height - 1:
            nbt.update(hd=head, ar=int(arch))
        if panel:
            nbt["pn"] = 1
        b.data(x, y + i, z, **nbt)


def bay(b, x0, y0, z0, finish, canted, storey=2, storeys=2, pattern=HUNG_1_1,
        depth=CENTRED, spandrel="panel", sill=SILL_STONE, head=HEAD_LINTEL):
    """A UWS brownstone bay, three wide and one deep, at x0..x0+2 standing out from a facade
    whose outer face is the cells z0 (the bay is the cells z0 + 1; the street is +z).

    From y0 up: a base row, then for each storey a window `storey` tall and a spandrel row
    above it (the last one the bay's top). The base, spandrels and top are solid panels
    (`spandrel` "panel", the windows' own spandrel panel, which follows a canted corner's
    diagonal) or brick ("brick", square at every corner). Canted, the end cells are the 45-degree
    corners (the west one facing west, the east one south: each faces the quarter its outsides
    are). Behind the window rows the facade cells are left open into the room, so the bay's end
    windows turn back to the facade; behind the base and spandrels the facade is brick, the
    floor line. The caller builds the facade (its cells z0) before calling, and the floors.
    Returns the y of the bay's top row."""
    cells = [(x0, FACE_W if canted else FACE_S, canted), (x0 + 1, FACE_S, False),
             (x0 + 2, FACE_S, canted)]
    z = z0 + 1

    def solid_row(y):
        for x, facing, c in cells:
            if spandrel == "panel":
                stack(b, x, y, z, 1, finish, facing, depth, c, pattern, panel=True)
            else:
                b.put(x, y, z, BRICK)

    y = y0
    solid_row(y)
    for _ in range(storeys):
        y += 1
        for x, facing, c in cells:
            stack(b, x, y, z, storey, finish, facing, depth, c, pattern, sill=sill, head=head)
            for i in range(storey):
                b.put(x, y + i, z0, "minecraft:air")
        y += storey
        solid_row(y)
    return y


def facade(b, x0, x1, y0, y1, z0, floors=(), back=4):
    """A brick facade x0..x1, y0..y1 on z0 with a room behind it `back` deep: side and back
    walls, and a floor (spruce boards) at each y in `floors`."""
    b.fill(x0, y0, z0, x1, y1, z0, BRICK)
    b.fill(x0, y0, z0 - back, x0, y1, z0 - 1, BRICK)
    b.fill(x1, y0, z0 - back, x1, y1, z0 - 1, BRICK)
    b.fill(x0, y0, z0 - back, x1, y1, z0 - back, BRICK)
    for fy in floors:
        b.fill(x0 + 1, fy, z0 - back + 1, x1 - 1, fy, z0 - 1, *FLOOR_BOARDS)
    b.fill(x0, y1 + 1, z0 - back, x1, y1 + 1, z0, *CORNICE)
    b.fill(x0, y1 + 1, z0 + 1, x1, y1 + 1, z0 + 1, *CORNICE)


FINISHES = list(gen_glazing_windows.FINISHES)
PATTERNS = (PLAIN, HUNG_1_1, HUNG_6_6, CASEMENT)
WINDOWS_X = 66       # the windows street starts here, east of the gallery


def windows(b):
    """The windows street (#357), the facade on z 0 like the houses', street to the south.

    - x 66..80: a brick wall with every finish (columns, west to east in creative order) in
      every pattern (rows, plain at the foot): 2-tall windows, stone sills, flat lintels.
    - x 83..101: the depths, centred, flush and inset (west to east), each a 2-wide 2-tall run
      (one shared mullion) and a 3-tall single; sills none, stone, frame and heads none, flat
      lintel, keystone with them, in that order.
    - x 104..116: opening sashes (1/1, 6/6, casement, open, at the foot) and arched tops (the
      same three and a plain one, 3 tall, keystone heads, above).
    - x 120..138: a two-storey UWS brownstone house with a canted bay (x 123..125) and a
      90-degree bay (x 131..133), 3 wide and 1 deep, spandrel panels at the base, the floor
      line and the top, a room behind with its floors.
    - x 141..175: a one-storey canted bay in every finish, each in the next pattern.
    """
    x0 = WINDOWS_X
    b.fill(x0 - 1, FLOOR + 1, 0, x0 + 2 * len(FINISHES), FLOOR + 13, 0, BRICK)
    for c, finish in enumerate(FINISHES):
        for r, pattern in enumerate(PATTERNS):
            stack(b, x0 + 2 * c, FLOOR + 2 + 3 * r, 0, 2, finish, pattern=pattern,
                  sill=SILL_STONE, head=HEAD_LINTEL)

    x0 = 84
    b.fill(x0 - 1, FLOOR + 1, 0, x0 + 17, FLOOR + 8, 0, BRICK)
    for d, depth in enumerate((CENTRED, FLUSH, INSET)):
        x = x0 + 6 * d
        for dx in (0, 1):
            stack(b, x + dx, FLOOR + 3, 0, 2, "white", depth=depth, pattern=HUNG_1_1, sill=d,
                  head=d)
        stack(b, x + 3, FLOOR + 2, 0, 3, "white", depth=depth, pattern=HUNG_6_6, sill=d, head=d)

    x0 = 105
    b.fill(x0 - 1, FLOOR + 1, 0, x0 + 11, FLOOR + 10, 0, BRICK)
    for i, pattern in enumerate((HUNG_1_1, HUNG_6_6, CASEMENT)):
        x = x0 + 3 * i
        stack(b, x, FLOOR + 2, 0, 2, "oak", pattern=pattern, opened=True, sill=SILL_STONE,
              head=HEAD_LINTEL)
    for i, pattern in enumerate((HUNG_1_1, HUNG_6_6, CASEMENT, PLAIN)):
        x = x0 + 3 * i
        stack(b, x, FLOOR + 5, 0, 3, "darkoak", pattern=pattern, arch=True, sill=SILL_STONE,
              head=HEAD_KEYSTONE)

    # the house: base row at the pavement, storeys of 2, its top at FLOOR + 7
    top = FLOOR + 1 + 3 * 2
    facade(b, 120, 138, FLOOR + 1, top, 0, floors=(FLOOR + 1, FLOOR + 4))
    bay(b, 123, FLOOR + 1, 0, "walnut", True)
    bay(b, 131, FLOOR + 1, 0, "walnut", False)

    x0 = 142
    b.fill(x0 - 1, FLOOR + 1, 0, x0 + 5 * len(FINISHES) - 2, FLOOR + 5, 0, BRICK)
    for c, finish in enumerate(FINISHES):
        bay(b, x0 + 5 * c, FLOOR + 1, 0, finish, True, storeys=1,
            pattern=PATTERNS[c % len(PATTERNS)], depth=FLUSH)


def expected():
    return {name for name, *_ in gen_address_plaques.BLOCKS} | set(gen_glazing_windows.BLOCKS)


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--instance", required=True)
    ap.add_argument("--create", action="store_true")
    a = ap.parse_args()
    client, server = a.instance + ".client", a.instance + ".server"
    o = Orchestrator()
    if a.create:
        o.call("client_world_create", instance=client, name=WORLD, worldType="flat")
        o.call("client_wait", instance=client, waitFor="worldLoaded", ticks=1200)
    motd = o.call("server_world_info", instance=server).get("motd", "")
    if not motd.endswith(" - " + WORLD):
        sys.exit("refusing: the loaded world is %r, not %r" % (motd, WORLD))
    for cmd in ("/difficulty peaceful", "/gamerule doMobSpawning false",
                "/gamerule doDaylightCycle false", "/time set 6000",
                "/kill @e[type=!player]", "/tp @p 8 5 8"):
        o.call("server_run_command", instance=server, command=cmd)
    time.sleep(4)

    b = Build()
    house(b, 0, "245", "address_plaque_brass", "245", False)
    house(b, 6, "247", "address_plaque_cast_iron", "247", False)
    house(b, 12, "251", "address_plaque_enamel", "251", True)
    gallery(b)
    windows(b)
    o.call("server_set_blocks", instance=server, mode="fill", block="minecraft:air", x=-2,
           y=FLOOR + 1, z=-6, toX=180, toY=FLOOR + 20, toZ=8, load=True)
    r = o.call("server_set_blocks", instance=server, mode="list", blocks=b.blocks,
               pairTwoPart=False, load=True)
    print("placed %s of %d" % (r.get("written"), len(b.blocks)))
    o.call("server_set_blocks", instance=server, mode="list", blocks=b.nbt, pairTwoPart=False,
           load=True)
    missing = sorted(expected() - b.placed)
    print("every plaque and window block placed" if not missing
          else "not placed: %s" % ", ".join(missing))
    o.call("server_run_command", instance=server,
           command="/tp @p 2.5 %d 5.5 180 -5" % (FLOOR + 1))
    o.close()
    return 0 if not missing else 1


if __name__ == "__main__":
    sys.exit(main())
