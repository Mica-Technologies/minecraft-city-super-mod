"""Per-block TESR inventory: place N copies of each tile-entity block, profile CPU submit time.

Usage: python inv.py <out.json> [state]   state = idle (default)
"""
import sys, json, time
from common import *

OUT = sys.argv[1]
ONLY = None
if len(sys.argv) > 2:
    ONLY = set(open(sys.argv[2]).read().split())
META = int(sys.argv[3]) if len(sys.argv) > 3 else 0

dump = json.load(open(DUMP, encoding="utf-8"))
blocks = [b for b in dump["blocks"] if b.get("hasTileEntity")]
if ONLY is not None:
    blocks = [b for b in blocks if b["id"] in ONLY]

PER_BATCH = 12
COPIES_X = (0, 2)
COPIES_Z = (0, 2, 4, 6)
CELL = 6
X0, Z0, Y = -100, -390, 4
N = len(COPIES_X) * len(COPIES_Z)

results = {}
try:
    results = json.load(open(OUT))
except Exception:
    pass

def clear():
    for y in range(Y, Y + 16, 8):
        S.call("server_set_blocks", {"mode": "fill", "block": "minecraft:air",
                                     "x": X0 - 4, "y": y, "z": Z0 - 4,
                                     "toX": X0 + CELL * PER_BATCH + 4, "toY": y + 7, "toZ": Z0 + 12})

batches = [blocks[i:i + PER_BATCH] for i in range(0, len(blocks), PER_BATCH)]
t0 = time.time()
for bi, batch in enumerate(batches):
    if all(b["id"] in results for b in batch):
        continue
    clear()
    placements = []
    for ti, b in enumerate(batch):
        for dx in COPIES_X:
            for dz in COPIES_Z:
                placements.append({"x": X0 + ti * CELL + dx, "y": Y, "z": Z0 + dz, "block": b["id"], "metadata": META})
    S.call("server_set_blocks", {"mode": "list", "blocks": placements})
    cx = X0 + (len(batch) * CELL) / 2.0
    cmd("tp DEV_USERNAME %.1f 9 %d 180 10" % (cx, Z0 + 36))
    time.sleep(2.5)
    cen = {e["block"]: e["count"] for e in S.call("server_census", {"top": 50})["dimensions"][0]["tileEntities"]["byType"]}
    C.call("client_profile_rendering", {"duration_seconds": 6, "top": 50})     # warm-up, discarded
    r = C.call("client_profile_rendering", {"duration_seconds": 6, "top": 50})
    by = {e["block"]: e for e in r.get("tileEntities", {}).get("byType", [])}
    for b in batch:
        e = by.get(b["id"])
        results[b["id"]] = {
            "class": b["class"].split(".")[-1], "tab": b.get("creativeTab"), "name": b.get("displayName"),
            "placed": N, "census": cen.get(b["id"], 0),
            "rendered": e["count"] if e else 0,
            "renderer": (e.get("renderer") or "unknown").split(".")[-1] if e else None,
            "us_total": e["totalMicrosPerFrame"] if e else 0.0,
            "us_each": round(e["totalMicrosPerFrame"] / e["count"], 2) if e and e["count"] else 0.0,
            "us_worst": e["worstMicrosPerFrame"] if e else 0.0,
        }
    json.dump(results, open(OUT, "w"), indent=1)
    print("batch %d/%d done  %.0fs  (%d types)  meanRenderWorkMs=%s" % (bi + 1, len(batches), time.time() - t0, len(by), r.get("meanRenderWorkMs")), flush=True)
print("DONE")
