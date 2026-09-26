"""Moving doors: garage doors while they move and stopped part-way, and the swing of the fixed
doors, measured with the profiler (per call and per frame), 8 copies each.

A garage door is free at rest; only while it moves or stands stopped part-way does its anchor hold
a TileEntityGarageDoor with a renderer. A fixed door's upper half holds a TileEntityDoorSwing on the
client for the 8 ticks of a swing. So the movers are kept moving by a second thread for the whole
profile: garage doors are toggled again (redstone rising edge) as soon as each move ends, doors
are opened and shut by redstone every 0.6 s.

    py doors.py out.json [garage|swing|all]
"""
import sys, json, time, threading, statistics
from common import *
OUT = sys.argv[1]
WHAT = sys.argv[2] if len(sys.argv) > 2 else "all"
X0, Z0, Y = -100, -400, 4
res = {}
try:
    res = json.load(open(OUT))
except Exception:
    pass

S2 = cb.Mcp(cb.SERVER_URL, tok).connect()


def clear():
    for yy in range(Y, Y + 14):
        S.call("server_set_blocks", {"mode": "fill", "block": "minecraft:air", "x": X0 - 12, "y": yy,
                                     "z": Z0 - 12, "toX": X0 + 60, "toY": yy, "toZ": Z0 + 40})
    S.call("server_set_blocks", {"mode": "fill", "block": "minecraft:grass", "x": X0 - 12, "y": Y - 1,
                                 "z": Z0 - 12, "toX": X0 + 60, "toY": Y - 1, "toZ": Z0 + 40})
    time.sleep(1)


def cam(cx, dist):
    cmd("tp @p %.1f 4 %.1f 180 0" % (cx, Z0 + dist)); time.sleep(0.8)
    C.call("client_look", {"yaw": 180, "pitch": 4}); time.sleep(2)


def profile(bid):
    C.call("client_profile_rendering", {"duration_seconds": 6, "top": 50})
    r = C.call("client_profile_rendering", {"duration_seconds": 6, "top": 50})
    bt = [e for e in r["tileEntities"]["byType"] if e["block"] == bid]
    mine = [e for e in r["tileEntities"]["costliest"] if e["block"] == bid]
    return {"byType": bt[0] if bt else None,
            "per_position": [(e["pos"]["x"], e["microsPerFrame"], e.get("framesDrawn"), e.get("microsPerCall")) for e in mine],
            "frames": r.get("frames")}


def pulse(server, x, y, z):
    server.call("server_set_block", {"x": x, "y": y, "z": z, "block": "minecraft:redstone_block"})
    time.sleep(0.15)
    server.call("server_set_block", {"x": x, "y": y, "z": z, "block": "minecraft:grass"})


def pulse_all(anchors):
    for b in ("minecraft:redstone_block", "minecraft:grass"):
        S.call("server_set_blocks", {"mode": "list", "blocks": [
            {"x": a[0], "y": a[1] - 1, "z": a[2], "block": b} for a in anchors]})
        time.sleep(0.15)


# --- garage doors ---------------------------------------------------------------------------------
GW, GH = 3, 3


def garage(bid):
    clear()
    anchors = []
    blocks = []
    for d in range(8):
        x0 = X0 + d * (GW + 3)
        for i in range(GW):
            for j in range(GH):
                # meta: facing north (2), motion closed (0)
                blocks.append({"x": x0 + i, "y": Y + j, "z": Z0, "block": bid, "metadata": 2})
        anchors.append((x0, Y, Z0))
    S.call("server_set_blocks", {"mode": "list", "blocks": blocks})
    time.sleep(1.5)
    cx = X0 + (8 * (GW + 3)) / 2.0
    cam(cx, 34)
    out = {}
    out["rest"] = profile(bid)

    def motion(a):
        return S2.call("server_get_block", {"x": a[0], "y": a[1], "z": a[2]})["state"].get("motion", "").lower()

    # moving: toggle each door again as soon as its move is over
    stop = threading.Event()
    moves = [0]

    def keep_moving():
        while not stop.is_set():
            for a in anchors:
                m = motion(a)
                if m in ("open", "closed"):
                    pulse(S2, a[0], a[1] - 1, a[2])
                    moves[0] += 1
            time.sleep(0.1)

    t = threading.Thread(target=keep_moving); t.start()
    time.sleep(2)
    out["moving"] = profile(bid)
    out["moving"]["moves_started"] = moves[0]
    stop.set(); t.join()
    # let everything come to rest, then start all and stop them part-way
    for _ in range(100):
        if all(motion(a) in ("open", "closed") for a in anchors):
            break
        time.sleep(0.2)
    pulse_all(anchors)
    time.sleep(1.0)
    pulse_all(anchors)
    time.sleep(1)
    out["stopped_states"] = [motion(a) for a in anchors]
    out["stopped"] = profile(bid)
    out["stopped_states_after"] = [motion(a) for a in anchors]
    out["shot"] = C.call("client_screenshot", {})["path"]
    return out


# --- swinging doors -------------------------------------------------------------------------------
def swing(bid, closer):
    clear()
    lows = []
    for d in range(8):
        x = X0 + d * 3
        cmd("setblock %d %d %d %s facing=north,half=lower,open=false" % (x, Y, Z0, bid))
        cmd("setblock %d %d %d %s half=upper,hinge=left,closer=%s" % (x, Y + 1, Z0, bid, "true" if closer else "false"))
        lows.append((x, Y, Z0))
    time.sleep(1.5)
    st = S.call("server_get_block", {"x": lows[0][0], "y": Y + 1, "z": Z0})["state"]
    cx = X0 + 10.5
    cam(cx, 20)
    stop = threading.Event()
    n = [0]

    def keep_swinging():
        on = False
        while not stop.is_set():
            on = not on
            S2.call("server_set_blocks", {"mode": "list", "blocks": [
                {"x": x - 1, "y": y, "z": z, "block": "minecraft:redstone_block" if on else "minecraft:air"} for x, y, z in lows]})
            n[0] += 1
            time.sleep(0.6)

    t = threading.Thread(target=keep_swinging); t.start()
    time.sleep(2)
    out = profile(bid)
    out["toggles"] = n[0]
    out["upper_state"] = st
    out["shot"] = C.call("client_screenshot", {})["path"]
    stop.set(); t.join()
    return out


if WHAT in ("garage", "all"):
    for bid in sys.argv[3].split(",") if len(sys.argv) > 3 else ["csm:garage_door_sectional_white", "csm:garage_door_rollup_galvanized"]:
        res["garage " + bid] = garage(bid)
        json.dump(res, open(OUT, "w"), indent=1)
        r = res["garage " + bid]
        for k in ("rest", "moving", "stopped"):
            print(bid, k, r[k]["byType"], flush=True)
        print("  stopped states", r["stopped_states"], "moves", r["moving"].get("moves_started"), flush=True)
if WHAT in ("swing", "all"):
    for bid, closer in [("csm:door_wood_oak", False), ("csm:door_metal_exit", True)]:
        key = "swing %s closer=%s" % (bid, closer)
        res[key] = swing(bid, closer)
        json.dump(res, open(OUT, "w"), indent=1)
        print(key, res[key]["byType"], "toggles", res[key]["toggles"], res[key]["upper_state"], flush=True)
        print("   ", res[key]["per_position"], flush=True)
print("DONE")
