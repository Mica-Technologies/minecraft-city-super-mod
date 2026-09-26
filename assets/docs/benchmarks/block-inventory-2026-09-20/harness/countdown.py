"""Crosswalk countdown with a countdown that is actually running, against idle, same session.

8 single crosswalks (meta 1 = clearance / flashing hand, facing south), then /blockdata puts a
99-second countdown on each (cCd:99, lCS:1 so no colour change is seen, lCT learned). The server
decrements it once a second, so the digits change (and a new cached list is compiled) every
second through the whole profile. The countdown value is read back before and after, and a
screenshot is taken, so the reading is known to be of lit digits.

    py countdown.py out.json
"""
import sys, json, time, statistics
from common import *
OUT = sys.argv[1]
X0, Z0, Y = -100, -400, 4
BID = "csm:controllablecrosswalksingle"


def profile():
    C.call("client_profile_rendering", {"duration_seconds": 6, "top": 50})
    r = C.call("client_profile_rendering", {"duration_seconds": 5, "top": 50})
    mine = [e for e in r["tileEntities"]["costliest"] if e["block"] == BID]
    return sorted(mine, key=lambda e: e["pos"]["x"])


def te(x, y, z):
    r = cmd("blockdata %d %d %d {}" % (x, y, z))
    return str(r)[:400]


res = {}
for name, meta, nbt in [("idle", 0, None), ("countdown", 1, "cCd:99,lCS:1,lCT:1980"), ("idle_again", 0, None)]:
    for yy in range(Y, Y + 6):
        S.call("server_set_blocks", {"mode": "fill", "block": "minecraft:air", "x": X0 - 10, "y": yy,
                                     "z": Z0 - 10, "toX": X0 + 90, "toY": yy, "toZ": Z0 + 10})
    time.sleep(1)
    pos = [(X0 + i * 10, Y, Z0) for i in range(8)]
    S.call("server_set_blocks", {"mode": "list", "blocks": [
        {"x": x, "y": y, "z": z, "block": BID, "metadata": meta} for x, y, z in pos]})
    time.sleep(1.5)
    fails = 0
    if nbt:
        for x, y, z in pos:
            if not cmd("blockdata %d %d %d {%s}" % (x, y, z, nbt)).get("succeeded"):
                fails += 1
    cmd("tp @p %.1f 4 %.1f 180 0" % (X0 + 35, Z0 + 38.5)); time.sleep(0.8)
    C.call("client_look", {"yaw": 180, "pitch": 6})
    time.sleep(2)
    before = te(*pos[3])
    mine = profile()
    after = te(*pos[3])
    shot = C.call("client_screenshot", {})
    vals = [e["microsPerFrame"] for e in mine]
    res[name] = {"meta": meta, "nbt": nbt, "fails": fails, "rendered": len(mine),
                 "us_median": round(statistics.median(vals), 2) if vals else None,
                 "per_call": [e.get("microsPerCall") for e in mine],
                 "per_position": [(e["pos"]["x"], e["microsPerFrame"]) for e in mine],
                 "te_before": before, "te_after": after, "shot": str(shot)[:300]}
    print(name, json.dumps(res[name], indent=None)[:1500], flush=True)
    json.dump(res, open(OUT, "w"), indent=1)
print("DONE")
