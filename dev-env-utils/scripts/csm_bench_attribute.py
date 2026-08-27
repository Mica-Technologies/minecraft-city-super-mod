#!/usr/bin/env python3
"""Attribute client frame time to individual CSM renderers, by removing them.

The Phase 7 harness answers "did this change help". This answers the prior question: "where does
the frame time actually go", so effort lands on the renderer that costs something rather than the
one that looks expensive in the source.

Method is subtractive, and deliberately so. There is no profiler in the loop and no instrumentation
in the mod: it measures the scene, deletes one category of block, measures again, and attributes
the difference to what it removed. That has one property worth more than precision -- it cannot be
fooled by a mistaken belief about which code runs. If deleting every guide sign does not move the
frame time, the guide sign renderer does not cost anything here, whatever its source looks like.

Frame time, not FPS, is what gets differenced. FPS is a rate; the reciprocals add, the rates do
not, and "120 to 110 fps" and "60 to 55 fps" are not the same amount of work despite both being
a ten-fps drop.

Run against a client with the csm_bench scene already built:

    python csm_bench_attribute.py
"""

import os
import statistics
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_bench as B  # noqa: E402

# Categories are removed cumulatively, heaviest suspicion last, so each step's delta belongs to
# the category just removed.
CATEGORIES = [
    ("dynamic signs (guide + street)", [B.GUIDE_SIGN, B.STREET_SIGN]),
    ("crosswalk signals", [B.CROSSWALK]),
    ("signal heads", [B.SIGNAL]),
    ("controllers", [B.CONTROLLER]),
]

POSE = {"name": "intersection_close", "x": -6.5, "y": 11.0, "z": -6.5,
        "yaw": 315.0, "pitch": 5.0}
SAMPLES = 14
SETTLE_TICKS = 200


def positions_for(blocks):
    """Every position in the built scene holding one of `blocks`."""
    out = []
    ox, _, oz = B.ORIGIN
    for gx in range(B.GRID):
        for gz in range(B.GRID):
            for entry in B.intersection_blocks(ox + gx * B.SPACING, oz + gz * B.SPACING):
                if entry["block"] in blocks:
                    out.append(entry)
    return out


def measure_frame_ms(client, label):
    client.call("client_wait", {"ticks": SETTLE_TICKS})
    fps = []
    for _ in range(SAMPLES):
        fps.append(client.call("client_runtime_info", {}).get("fps", 0))
        time.sleep(0.4)
    median_fps = statistics.median(fps)
    frame_ms = 1000.0 / median_fps if median_fps else float("inf")
    print("    %-34s %6.1f fps   %6.3f ms/frame" % (label, median_fps, frame_ms))
    return frame_ms


def main():
    token = B.read_token()
    client = B.Mcp(B.CLIENT_URL, token).connect()
    server = B.Mcp(B.SERVER_URL, token).connect()

    B.pin_world_conditions(client)
    client.call("client_send_chat",
                {"message": "/tp %.2f %.2f %.2f" % (POSE["x"], POSE["y"], POSE["z"])})
    client.call("client_look", {"yaw": POSE["yaw"], "pitch": POSE["pitch"]})

    print("==> pose %s, %d intersections" % (POSE["name"], B.GRID * B.GRID))
    baseline = measure_frame_ms(client, "full scene")

    removed = []
    previous = baseline
    steps = []
    for name, blocks in CATEGORIES:
        entries = positions_for(blocks)
        # Delete in one call per category so the chunk rebuild happens once.
        client_blocks = [{"x": e["x"], "y": e["y"], "z": e["z"],
                          "block": "minecraft:air", "metadata": 0} for e in entries]
        for chunk_start in range(0, len(client_blocks), 400):
            server.call("server_set_blocks",
                        {"mode": "list", "blocks": client_blocks[chunk_start:chunk_start + 400]})
        removed.append(name)
        now = measure_frame_ms(client, "minus " + name)
        steps.append((name, len(entries), previous - now))
        previous = now

    empty = previous
    print()
    print("==> attribution of client frame time at this pose")
    print("    %-34s %8s %10s %8s" % ("category", "blocks", "ms/frame", "share"))
    total_csm = baseline - empty
    for name, count, delta in steps:
        share = (delta / total_csm * 100.0) if total_csm else 0.0
        print("    %-34s %8d %10.3f %7.1f%%" % (name, count, delta, share))
    print("    %-34s %8s %10.3f" % ("-- all CSM content", "", total_csm))
    print("    %-34s %8s %10.3f" % ("-- bare terrain floor", "", empty))
    print()
    print("    full scene %.3f ms/frame, of which CSM is %.3f ms (%.0f%%)"
          % (baseline, total_csm, total_csm / baseline * 100.0 if baseline else 0))
    print()
    print("    Rebuild the scene before benchmarking again: python csm_bench.py build")


if __name__ == "__main__":
    main()
