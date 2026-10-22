#!/usr/bin/env python3
"""Builds the modern ceilings demo world in a flat creative world named "Ceilings Demo".

    python dev-env-utils/scripts/build_ceilings_demo.py [--instance run-f2eb58] [--create]

Through the MCMCP orchestrator by instance id (mcmcp_orchestrator.py), in a dev client with every
module. Refuses any world not named "Ceilings Demo"; --create makes one (flat, creative) first.

- **A drop-ceiling corridor** (x 0..84, z 0..8): a five-block section of each of the seventeen
  ceiling finishes, every troffer set in the lay-in tiles (the rebuilt 2ESL among them), and over
  the open cell and exposed grid sections a plenum with pipes and a duct in it.
- **An open office** (x 0..44, z 14..34): a dark ceiling with light grey baffles over hung black
  linear runs on cable hangers, a sage cloud with a white louvered run under it, a galvanized
  spiral duct with a tee and a drop, a black rectangular duct round a corner, and a pipe rack.
- **Standpipe pass-throughs** (x 60..76, z 14..30): through a brick wall and up through a stone
  floor, dressed as each, and bare ones beside them.
- **A gallery** (x 0..53, z 40..62): every linear light finish (opal and louvered), every baffle
  and cloud finish, and every duct and pipe colour with an elbow.

It sets blocks only, then checks every block the five generators register was placed, and leaves
the player at the corridor's entrance.
"""

import argparse
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmcp_orchestrator import Orchestrator  # noqa: E402

import gen_acoustic_ceilings  # noqa: E402
import gen_ceilings  # noqa: E402
import gen_exposed_services  # noqa: E402
import gen_linear_lights  # noqa: E402
import gen_troffers  # noqa: E402

WORLD = "Ceilings Demo"
FLOOR = 3            # the flat world's top block
AX, AY, AZ = 0, 1, 2
# bright light metadata: facing index * 4 + state (3 = manually on)
LIT_N, LIT_E = 2 * 4 + 3, 3 * 4 + 3

FINISHES = list(gen_linear_lights.FINISHES)


class Build:
    def __init__(self):
        self.blocks = []
        self.placed = set()
        # pass-throughs to dress once placed: (x, y, z, block, meta)
        self.dress = []

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


def corridor(b):
    """The drop-ceiling corridor: a section of each finish, troffers in the lay-in tiles."""
    ceil = FLOOR + 5
    regs = list(gen_ceilings.CATALOGUE)
    troffers = (["l2esl2", "l2esl4"] + [r for r in gen_troffers.catalogue()])
    t = 0
    b.fill(-1, FLOOR, -1, len(regs) * 5, FLOOR, 9, "minecraft:quartz_block")
    for i, reg in enumerate(regs):
        x0 = i * 5
        meta = 1 if ("_slat_" in reg or "_corrugated_" in reg) else 0
        b.fill(x0, ceil, 0, x0 + 4, ceil, 8, reg, meta)
        if "_grid_" in reg or "_open_cell_" in reg:
            # a plenum: a deck above, a pipe and a duct running through it
            b.fill(x0, ceil + 3, 0, x0 + 4, ceil + 3, 8, "minecraft:concrete", 15)
            for x in range(x0, x0 + 5):
                b.put(x, ceil + 2, 2, "pipe_red", AX)
                b.put(x, ceil + 1, 6, "duct_round_galvanized", AX)
        elif kind_plain(reg):
            # two troffers down the middle of each lay-in section
            for z in (2, 6):
                if t >= len(troffers):
                    break
                reg_t = troffers[t]
                t += 1
                b.put(x0 + 2, ceil, z, reg_t, LIT_N)
                if reg_t.endswith("2x4") or reg_t == "l2esl4":
                    b.put(x0 + 2, ceil, z - 1, "minecraft:air")
    # the corridor's walls, glass on the south side
    b.fill(-1, FLOOR + 1, -1, len(regs) * 5, ceil, -1, "minecraft:concrete", 0)
    b.fill(-1, FLOOR + 1, 9, len(regs) * 5, ceil, 9, "minecraft:glass")
    return t, len(troffers)


def kind_plain(reg):
    return gen_ceilings.kind(reg) == "plain"


def office(b):
    """The open office: baffles over hung linear runs, a cloud, ducts and pipes."""
    z0, ceil = 14, FLOOR + 7
    b.fill(-1, FLOOR, z0, 45, FLOOR, z0 + 20, "minecraft:concrete", 8)
    b.fill(-1, ceil, z0, 45, ceil, z0 + 20, "minecraft:concrete", 15)
    # light grey baffles along z every other block, over two hung black runs along x
    for x in range(0, 20, 2):
        for z in range(z0 + 1, z0 + 13):
            b.put(x, ceil - 1, z, "acoustic_baffle_light_grey", 0)
    for z in (z0 + 4, z0 + 9):
        for x in range(-1, 21):
            b.put(x, ceil - 3, z, "linear_light_black", LIT_E)
        for x in (-1, 3, 7, 11, 15, 19):
            b.put(x, ceil - 1, z, "linear_light_hanger")
            b.put(x, ceil - 2, z, "linear_light_hanger")
    # a sage cloud with a white louvered run under it
    for x in range(26, 32):
        for z in range(z0 + 2, z0 + 6):
            b.put(x, ceil - 1, z, "acoustic_cloud_sage")
    for x in range(26, 32):
        b.put(x, ceil - 2, z0 + 4, "linear_light_louver_white", LIT_E)
    # a walnut cloud over a walnut run
    for x in range(34, 38):
        for z in range(z0 + 2, z0 + 5):
            b.put(x, ceil - 1, z, "acoustic_cloud_walnut")
    for x in range(34, 38):
        b.put(x, ceil - 2, z0 + 3, "linear_light_walnut", LIT_E)
    # a galvanized spiral duct along x with a tee north and a drop at its end
    for x in range(22, 44):
        b.put(x, ceil - 1, z0 + 10, "duct_round_galvanized", AX)
    for z in range(z0 + 7, z0 + 10):
        b.put(30, ceil - 1, z, "duct_round_galvanized", AZ)
    for y in range(ceil - 3, ceil - 1):
        b.put(30, y, z0 + 7, "duct_round_galvanized", AY)
    # a black rectangular duct round a corner
    for x in range(22, 36):
        b.put(x, ceil - 1, z0 + 14, "duct_rect_black", AX)
    for z in range(z0 + 15, z0 + 19):
        b.put(35, ceil - 1, z, "duct_rect_black", AZ)
    # a galvanized duct hung two blocks lower on threaded rod hangers
    for x in range(22, 36):
        b.put(x, ceil - 3, z0 + 18, "duct_round_galvanized", AX)
    for x in (22, 26, 30, 34):
        b.put(x, ceil - 1, z0 + 18, "exposed_service_hanger")
        b.put(x, ceil - 2, z0 + 18, "exposed_service_hanger")
    # a pipe rack: four colours side by side
    for i, c in enumerate(("black", "white", "red", "silver")):
        for x in range(0, 20):
            b.put(x, ceil - 1, z0 + 15 + i, "pipe_" + c, AX)


def gallery(b):
    """Every finish and colour side by side."""
    z0, ceil = 40, FLOOR + 6
    b.fill(-1, FLOOR, z0, 53, FLOOR, z0 + 22, "minecraft:quartz_block")
    b.fill(-1, ceil, z0, 53, ceil, z0 + 22, "minecraft:concrete", 15)
    for i, f in enumerate(FINISHES):
        x = i * 2
        for z in range(z0 + 1, z0 + 5):
            b.put(x, ceil - 1, z, "linear_light_%s" % f, LIT_N)
            b.put(x, ceil - 1, z + 5, "linear_light_louver_%s" % f, LIT_N)
        for z in range(z0 + 11, z0 + 15):
            b.put(x, ceil - 1, z, "acoustic_baffle_%s" % f, 0)
        b.put(x, ceil - 1, z0 + 17, "acoustic_cloud_%s" % f)
        b.put(x, ceil - 1, z0 + 18, "acoustic_cloud_%s" % f)
    # each duct and pipe a short run with an elbow, three apart so neighbours of a kind do not
    # join
    for i, reg in enumerate(gen_exposed_services.registries()):
        x = 22 + i * 3
        for z in range(z0 + 1, z0 + 6):
            b.put(x, ceil - 1, z, reg, AZ)
        b.put(x + 1, ceil - 1, z0 + 5, reg, AX)


def passthroughs(b):
    """Standpipe pass-throughs (x 60..76, z 14..30): a red main through a brick wall, dressed as
    the brick; a silver main up through a stone floor, dressed as the stone; and a bare one, to
    see before and after. The pass-throughs are dressed by their tile entity's NBT."""
    x0, z0 = 60, 14
    b.fill(x0, FLOOR, z0, x0 + 16, FLOOR, z0 + 16, "minecraft:quartz_block")
    # a brick wall across x at x0 + 8, a red main through it at y FLOOR + 3
    b.fill(x0 + 8, FLOOR + 1, z0, x0 + 8, FLOOR + 6, z0 + 8, "minecraft:brick_block")
    for x in range(x0 + 2, x0 + 15):
        b.put(x, FLOOR + 3, z0 + 4, "standpipe_pipe_main_red")
    b.put(x0 + 8, FLOOR + 3, z0 + 4, "standpipe_passthrough_main_red", 1)
    b.dress.append((x0 + 8, FLOOR + 3, z0 + 4, "minecraft:brick_block", 0))
    # a stone floor slab at FLOOR + 4 over the far half, a silver main rising through it
    b.fill(x0, FLOOR + 4, z0 + 10, x0 + 16, FLOOR + 4, z0 + 16, "minecraft:stone")
    for y in range(FLOOR + 1, FLOOR + 9):
        b.put(x0 + 8, y, z0 + 13, "standpipe_pipe_main_silver")
    b.put(x0 + 8, FLOOR + 4, z0 + 13, "standpipe_passthrough_main_silver", 1)
    b.dress.append((x0 + 8, FLOOR + 4, z0 + 13, "minecraft:stone", 0))
    # and bare ones through the same slab, beside it
    for y in range(FLOOR + 1, FLOOR + 9):
        b.put(x0 + 4, y, z0 + 13, "standpipe_pipe_branch_red")
    b.put(x0 + 4, FLOOR + 4, z0 + 13, "standpipe_passthrough_branch_red", 0)
    # a silver branch through the brick wall, left bare
    for x in range(x0 + 2, x0 + 15):
        b.put(x, FLOOR + 5, z0 + 2, "standpipe_pipe_branch_silver")
    b.put(x0 + 8, FLOOR + 5, z0 + 2, "standpipe_passthrough_branch_silver", 0)


def expected():
    out = set(gen_ceilings.CATALOGUE) | set(gen_troffers.catalogue()) | {"l2esl2", "l2esl4"}
    out |= set(gen_linear_lights.registries()) | {gen_linear_lights.HANGER}
    out |= set(gen_acoustic_ceilings.registries()) | set(gen_exposed_services.registries())
    out |= {gen_exposed_services.HANGER}
    out |= {"standpipe_passthrough_%s_%s" % (s, f) for s in ("main", "branch")
            for f in ("red", "silver")}
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--instance", default="run-f2eb58")
    ap.add_argument("--create", action="store_true")
    a = ap.parse_args()
    client, server = a.instance + ".client", a.instance + ".server"
    o = Orchestrator()
    if a.create:
        o.call("client_world_create", instance=client, name=WORLD, worldType="flat")
        o.call("client_wait", instance=client, waitFor="worldLoaded", ticks=1200)
    # an integrated server's motd is "<player> - <world name>"
    motd = o.call("server_world_info", instance=server).get("motd", "")
    if not motd.endswith(" - " + WORLD):
        sys.exit("refusing: the loaded world is %r, not %r" % (motd, WORLD))
    for cmd in ("/difficulty peaceful", "/gamerule doMobSpawning false",
                "/gamerule doDaylightCycle false", "/time set 6000",
                "/kill @e[type=!player]", "/tp @p 20 5 -8"):
        o.call("server_run_command", instance=server, command=cmd)
    time.sleep(4)

    b = Build()
    placed_troffers, all_troffers = corridor(b)
    office(b)
    gallery(b)
    passthroughs(b)
    # clear the volume first, then write everything in one call
    o.call("server_set_blocks", instance=server, mode="fill", block="minecraft:air", x=-2,
           y=FLOOR + 1, z=-2, toX=90, toY=FLOOR + 12, toZ=64, load=True)
    r = o.call("server_set_blocks", instance=server, mode="list", blocks=b.blocks,
               pairTwoPart=False, load=True)
    print("placed %s of %d" % (r.get("written"), len(b.blocks)))
    for x, y, z, block, meta in b.dress:
        o.call("server_set_blocks", instance=server, mode="list", pairTwoPart=False, blocks=[
            {"x": x, "y": y, "z": z, "nbt": '{mb:"%s",mm:%db}' % (block, meta)}])
    missing = sorted(expected() - b.placed)
    if placed_troffers < all_troffers:
        print("troffer slots short: %d of %d placed" % (placed_troffers, all_troffers))
    print("every new block placed" if not missing else "not placed: %s" % ", ".join(missing))
    o.call("server_run_command", instance=server,
           command="/tp @p -4.5 %d 4.5 -90 -15" % (FLOOR + 1))
    o.close()
    return 0 if not missing else 1


if __name__ == "__main__":
    sys.exit(main())
