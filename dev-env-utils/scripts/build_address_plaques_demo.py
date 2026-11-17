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

It places blocks, then writes each plaque's NBT exactly as the editor would (a plaque placed by
hand and edited reads the same), checks every plaque block was placed, and leaves the player
three blocks off the first stoop. For night shots: ``/time set 18000``.
"""

import argparse
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmcp_orchestrator import Orchestrator  # noqa: E402

import gen_address_plaques  # noqa: E402

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

    def plaque(self, x, y, z, block, **nbt):
        self.put(x, y, z, block, WALL_N)
        parts = []
        for k, v in nbt.items():
            parts.append('%s:%s' % (k, '"%s"' % v if isinstance(v, str) else "%db" % v))
        self.nbt.append({"x": x, "y": y, "z": z, "nbt": "{%s}" % ",".join(parts)})


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


def expected():
    return {name for name, *_ in gen_address_plaques.BLOCKS}


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
    o.call("server_set_blocks", instance=server, mode="fill", block="minecraft:air", x=-2,
           y=FLOOR + 1, z=-1, toX=62, toY=FLOOR + 20, toZ=8, load=True)
    r = o.call("server_set_blocks", instance=server, mode="list", blocks=b.blocks,
               pairTwoPart=False, load=True)
    print("placed %s of %d" % (r.get("written"), len(b.blocks)))
    o.call("server_set_blocks", instance=server, mode="list", blocks=b.nbt, pairTwoPart=False,
           load=True)
    missing = sorted(expected() - b.placed)
    print("every plaque block placed" if not missing else "not placed: %s" % ", ".join(missing))
    o.call("server_run_command", instance=server,
           command="/tp @p 2.5 %d 5.5 180 -5" % (FLOOR + 1))
    o.close()
    return 0 if not missing else 1


if __name__ == "__main__":
    sys.exit(main())
