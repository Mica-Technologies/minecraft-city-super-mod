"""Phase 6 busy states, set without /setblock: blocks placed with their metadata through
server_set_blocks, then /blockdata. Per-position costs kept for the bimodality question.

    py states2.py out.json [name,...]
"""
import sys, json, time, statistics
from common import *
OUT = sys.argv[1]
ONLY = set(sys.argv[2].split(",")) if len(sys.argv) > 2 else None
X0, Z0, Y = -100, -400, 4

SC = []
def sc(name, block, meta, nbt, spacing=10, wait=3.0):
    SC.append(dict(name=name, block=block, meta=meta, nbt=nbt, spacing=spacing, wait=wait))

sc("blankout_idle", "csm:blankout_box", 0, None)
sc("blankout_lit_train", "csm:blankout_box", 2, "boT:5,vT:1,mT:0")
sc("crosswalk_idle", "csm:controllablecrosswalksingle", 0, None)
sc("crosswalk_countdown", "csm:controllablecrosswalksingle", 1, "cCd:25,lCS:1,lCT:500")
sc("thermostat_idle", "csm:hvac_thermostat", 0, None)
sc("thermostat_calling", "csm:hvac_thermostat", 0, "tLo:68,tHi:76,cT:62.0f,cL:1b,cM:1")
sc("polemount_speed_idle", "csm:polemount_speed_limit_sign", 0, None)
sc("polemount_speed_busy", "csm:polemount_speed_limit_sign", 0, "speedVal:35,hColor:0")

res = {}
try: res = json.load(open(OUT))
except Exception: pass

def profile():
    C.call("client_profile_rendering", {"duration_seconds": 6, "top": 50})
    r = C.call("client_profile_rendering", {"duration_seconds": 5, "top": 50})
    pos = [(e["block"], e["pos"]["x"], e["pos"]["z"], e["microsPerFrame"], e.get("framesDrawn"), e.get("microsPerCall")) for e in r["tileEntities"]["costliest"]]
    return {e["block"]: e for e in r["tileEntities"]["byType"]}, pos, r.get("frames")

for s in SC:
    if ONLY and s["name"] not in ONLY: continue
    sp = s["spacing"]; n = 8
    for yy in range(Y, Y + 14):
        S.call("server_set_blocks", {"mode": "fill", "block": "minecraft:air", "x": X0 - 10, "y": yy, "z": Z0 - 10, "toX": X0 + 8 * 30 + 10, "toY": yy, "toZ": Z0 + 10})
    time.sleep(1)
    pos = [(X0 + i * sp, Y, Z0) for i in range(n)]
    S.call("server_set_blocks", {"mode": "list", "blocks": [{"x": x, "y": y, "z": z, "block": s["block"], "metadata": s["meta"]} for x, y, z in pos]})
    time.sleep(1)
    fails = 0; last = None
    if s["nbt"]:
        for x, y, z in pos:
            r = cmd("blockdata %d %d %d {%s}" % (x, y, z, s["nbt"]))
            if not r.get("succeeded"): fails += 1; last = r
    span = sp * (n - 1)
    dist = max(30, span * 0.55)
    cmd("tp @p %.1f 4 %.1f 180 0" % (X0 + span / 2.0, Z0 + dist)); time.sleep(0.8)
    C.call("client_look", {"yaw": 180, "pitch": 6})
    time.sleep(s["wait"])
    by, pl, frames = profile()
    e = by.get(s["block"])
    mine = [p for p in pl if p[0] == s["block"]]
    vals = [p[3] for p in mine]
    states = [S.call("server_get_block", {"x": x, "y": y, "z": z}).get("state") for x, y, z in pos]
    res[s["name"]] = {"block": s["block"], "meta": s["meta"], "nbt": s["nbt"], "cmd_failures": fails,
                      "rendered": e["count"] if e else 0, "us_median": round(statistics.median(vals), 2) if vals else None,
                      "us_min": min(vals) if vals else None, "us_max": max(vals) if vals else None,
                      "per_position": [{"x": p[1], "us": p[3], "framesDrawn": p[4], "perCall": p[5]} for p in sorted(mine, key=lambda q: q[1])],
                      "frames": frames, "states": states}
    if fails: res[s["name"]]["last_fail"] = str(last)[:300]
    json.dump(res, open(OUT, "w"), indent=1)
    print(s["name"], {k: v for k, v in res[s["name"]].items() if k not in ("per_position", "states")}, flush=True)
    print("   per position:", [(p["x"], p["us"], p["framesDrawn"]) for p in res[s["name"]]["per_position"]], flush=True)
print("DONE")
