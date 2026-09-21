"""Busy-state profile: for each scenario place 8 copies, profile idle, apply the busy state, profile again."""
import sys, json, time
from common import *
OUT = sys.argv[1]
ONLY = set(sys.argv[2].split(",")) if len(sys.argv) > 2 else None

X0, Z0, Y = -100, -400, 4

def snbt_str(s):
    return s.replace("\\", "\\\\").replace('"', '\\"')

GUIDE = {"version":1,"signColor":0,"postType":2,"borderWidth":1,"cornerStyle":0,"minWidth":192,"minHeight":80,"lightType":1,"lightMode":1,
 "panels":[{"rows":[{"alignment":1,"elements":[{"type":1,"shieldType":0,"routeNumber":"95","bannerType":1},{"type":0,"text":"DOWNTOWN","textScale":1.5},{"type":2,"arrowType":0}]},
                    {"alignment":1,"elements":[{"type":0,"text":"CITY CENTER","textScale":1.0}]}],
            "exitTab":{"position":2,"text":"EXIT 12A","color":0,"toll":False,"wide":True}},
           {"rows":[{"alignment":1,"elements":[{"type":1,"shieldType":2,"routeNumber":"66"},{"type":0,"text":"WEST","textScale":1.5},{"type":2,"arrowType":2}]},
                    {"alignment":1,"elements":[{"type":0,"text":"AIRPORT 12 MI","textScale":1.0}]}]}]}
STREET = {"version":1,"prefix":"W","streetName":"BROADWAY","suffix":"AVE","cityText":"DOWNTOWN","blockNumber":"400","blockPosition":2,"emblemKind":1,"emblemPosition":1,
 "shieldType":2,"shieldRoute":"66","arrowPosition":0,"signColor":0,"borderWidth":1,"mountType":0,"extrudedFrame":True,"internalLight":True,"lightMode":1,"minWidth":96,"minHeight":16,
 "lowerBlade":{"prefix":"","streetName":"OAK","suffix":"ST","cityText":"OLD TOWN","blockNumber":"","blockPosition":0,"emblemKind":2,"emblemPosition":1,"logoType":0,"arrowPosition":2,"arrowType":3}}
MSG = 'flashMode:2,speed:2,color:0,angle:0,hColor:0,pages:[{l0:"ROAD WORK",l1:"AHEAD",l2:"1 MILE"},{l0:"RIGHT LANE",l1:"CLOSED",l2:"MERGE LEFT"},{l0:"EXPECT",l1:"DELAYS",l2:"USE ALT ROUTE"}]'

def bd(nbt): return lambda x, y, z: ["blockdata %d %d %d {%s}" % (x, y, z, nbt)]
def setb(block, meta, redstone=False):
    def f(x, y, z):
        c = ["setblock %d %d %d %s %d" % (x, y, z, block, meta)]
        if redstone: c.append("setblock %d %d %d minecraft:redstone_block" % (x + 1, y, z))
        return c
    return f

SC = []
def sc(name, block, meta, busy, spacing=10, wait=2.5, extra_wait=0):
    SC.append(dict(name=name, block=block, meta=meta, busy=busy, spacing=spacing, wait=wait, extra_wait=extra_wait))

sc("guide_sign_busy", "csm:dynamic_guide_sign", 0, bd('signData:"%s",lightPowered:1b' % snbt_str(json.dumps(GUIDE, separators=(",", ":")))), spacing=30)
sc("street_sign_busy", "csm:dynamic_street_sign", 0, bd('signData:"%s",lightPowered:1b' % snbt_str(json.dumps(STREET, separators=(",", ":")))), spacing=14)
sc("portable_message_busy", "csm:portable_message_sign", 0, bd(MSG), spacing=14)
sc("overhead_message_busy", "csm:overhead_message_sign", 0, bd(MSG), spacing=16)
sc("portable_speed_busy", "csm:portable_speed_limit_sign", 0, bd("speedVal:45,flashMode:2,color:0,angle:0,hColor:0"), spacing=14)
sc("overhead_speed_busy", "csm:overhead_speed_limit_sign", 0, bd("speedVal:65,fScr:1b,hColor:0"), spacing=10)
sc("polemount_speed_busy", "csm:polemount_speed_limit_sign", 0, bd("speedVal:35,hColor:0"), spacing=10)
sc("radar_sign_configured", "csm:radar_speed_sign", 0, bd("spd:25,mul:3,scl:2,hdr:1b,face:1"), spacing=10)
for p in range(7):
    sc("arrow_board_pattern%d" % p, "csm:arrow_board", 0, bd("pattern:%d" % p), spacing=14)
sc("school_beacon_2above_flashing", "csm:school_zone_beacon", 0, bd("spd:20,scl:3,arr:2,bsz:1,bst:0,vsr:0,hcl:5,bnc:1,mode:2"), spacing=12)
sc("school_beacon_1above1below_flashing", "csm:school_zone_beacon", 0, bd("spd:20,scl:3,arr:0,bsz:1,bst:0,vsr:0,hcl:5,bnc:1,mode:2"), spacing=12)
sc("lane_control_redx", "csm:lane_control_signal", 0, bd("sT:2,bC:0,vC:0,vT:1,mT:0,tlt:0"))
sc("lane_control_greenarrow", "csm:lane_control_signal", 0, bd("sT:0,bC:0,vC:0,vT:1,mT:0,tlt:0"))
sc("rr_gate3_powered", "csm:railroad_crossing_gate_3", 0, setb("csm:railroad_crossing_gate_3", 4, True), spacing=12, extra_wait=10)
sc("rr_gate1_powered", "csm:railroad_crossing_gate_1", 0, setb("csm:railroad_crossing_gate_1", 4, True), spacing=12, extra_wait=10)
sc("rr_flasher_powered", "csm:railroad_crossing_flasher", 0, setb("csm:railroad_crossing_flasher", 4, True), spacing=10)
sc("barricade1_flashers_sign", "csm:barricade_type_1_left", 0, bd('flashers:3,sign:"csm:signbikelane"'))
sc("preempt_beacon_powered", "csm:tlpreemptbeacon", 0, setb("csm:tlpreemptbeacon", 10, True))
sc("snow_beacon_powered", "csm:tlsnowbeacon", 0, setb("csm:tlsnowbeacon", 10, True))
sc("tattle_red", "csm:controllabletattletalebeacon", 0, setb("csm:controllabletattletalebeacon", 8))
sc("blankout_lit_train", "csm:blankout_box", 0, lambda x, y, z: setb("csm:blankout_box", 2)(x, y, z) + ["blockdata %d %d %d {boT:5,vT:1,mT:0}" % (x, y, z)])
sc("crosswalk_countdown", "csm:controllablecrosswalksingle", 0, lambda x, y, z: setb("csm:controllablecrosswalksingle", 1)(x, y, z) + ["blockdata %d %d %d {cCd:25,lCS:1,lCT:500}" % (x, y, z)])
sc("thermostat_calling", "csm:hvac_thermostat", 0, bd("tLo:68,tHi:76,cT:62.0f,cL:1b,cM:1"))

sc("route_marker_interstate95", "csm:dynamic_route_marker_sign", 0, bd('rt:"95",sh:0'), spacing=8)
sc("route_marker_us66", "csm:dynamic_route_marker_sign", 0, bd('rt:"66",sh:2'), spacing=8)

res = {}
try: res = json.load(open(OUT))
except Exception: pass

def profile():
    C.call("client_profile_rendering", {"duration_seconds": 6, "top": 50})
    r = C.call("client_profile_rendering", {"duration_seconds": 5, "top": 50})
    global LASTPOS
    LASTPOS = [(e["block"], e["pos"]["x"], e["pos"]["z"], e["microsPerFrame"]) for e in r["tileEntities"]["costliest"]]
    return {e["block"]: e for e in r["tileEntities"]["byType"]}, r.get("meanRenderWorkMs")

import statistics
NC = {"guide_sign_busy": 4}
for s in SC:
    if ONLY and s["name"] not in ONLY: continue
    if s["name"] in res: continue
    sp = s["spacing"]; n = NC.get(s["name"], 8)
    for yy in range(Y, Y + 14):
        S.call("server_set_blocks", {"mode": "fill", "block": "minecraft:air", "x": X0 - 10, "y": yy, "z": Z0 - 10, "toX": X0 + 8 * 30 + 10, "toY": yy, "toZ": Z0 + 10})
    pos = [(X0 + i * sp, Y, Z0) for i in range(n)]
    S.call("server_set_blocks", {"mode": "list", "blocks": [{"x": x, "y": y, "z": z, "block": s["block"], "metadata": s["meta"]} for x, y, z in pos]})
    span = sp * (n - 1)
    dist = max(30, span * 0.55)
    cmd("tp DEV_USERNAME %.1f 4 %.1f 180 0" % (X0 + span / 2.0, Z0 + dist)); time.sleep(0.8)
    try: C.call("client_look", {"yaw": 180, "pitch": 6})
    except Exception: pass
    time.sleep(s["wait"])
    idle, _ = profile()
    ie = idle.get(s["block"]); idle_pos = [p[3] for p in LASTPOS if p[0] == s["block"]]
    fails = 0
    for x, y, z in pos:
        for c in s["busy"](x, y, z):
            r = cmd(c)
            if not r.get("succeeded"): fails += 1; last = r
    time.sleep(2.0 + s["extra_wait"])
    busy, work = profile()
    be = busy.get(s["block"]); busy_pos = [p[3] for p in LASTPOS if p[0] == s["block"]]
    res[s["name"]] = {
        "block": s["block"], "n": n, "cmd_failures": fails,
        "idle_rendered": ie["count"] if ie else 0, "idle_us_median": round(statistics.median(idle_pos), 2) if idle_pos else None,
        "busy_rendered": be["count"] if be else 0, "busy_us_median": round(statistics.median(busy_pos), 2) if busy_pos else None,
        "busy_us_min": min(busy_pos) if busy_pos else None, "busy_us_max": max(busy_pos) if busy_pos else None,
    }
    if fails: res[s["name"]]["last_fail"] = str(last)[:300]
    json.dump(res, open(OUT, "w"), indent=1)
    print(s["name"], res[s["name"]], flush=True)
print("DONE")
