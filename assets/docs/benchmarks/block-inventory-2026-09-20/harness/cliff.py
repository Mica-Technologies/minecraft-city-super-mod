import sys, json, time
from common import *
bid=sys.argv[1]; counts=[int(x) for x in sys.argv[2].split(",")]
G=40; SP=2; X0=-130; Z0=-440; Y=4
def clear():
    for yy in range(Y,Y+6):
        S.call("server_set_blocks",{"mode":"fill","block":"minecraft:air","x":X0-2,"y":yy,"z":Z0-2,"toX":X0+G*SP+2,"toY":yy,"toZ":Z0+G*SP+2})
def cam():
    cmd("tp DEV_USERNAME %.1f 22 %d 180 0" % (X0+G*SP/2.0, Z0+G*SP+34)); time.sleep(0.6)
    try: C.call("client_look",{"yaw":180,"pitch":20})
    except Exception: pass
def sample():
    C.call("client_frame_stats",{"sample_seconds":2})
    r=C.call("client_frame_stats",{"sample_seconds":6})["sampled"]
    return 1000.0/r["fps"]
order=[(i*997)%(G*G) for i in range(G*G)]
clear(); cam(); time.sleep(3); base=sample(); print("base",round(base,3))
for n in counts:
    clear()
    S.call("server_set_blocks",{"mode":"list","blocks":[{"x":X0+(o%G)*SP,"y":Y,"z":Z0+(o//G)*SP,"block":bid,"metadata":0} for o in order[:n]]})
    cam(); time.sleep(6)
    r=C.call("client_profile_rendering",{"duration_seconds":4,"top":5})
    row=[e for e in r["tileEntities"]["byType"] if e["block"]==bid]
    rend=row[0]["count"] if row else 0; us=row[0]["totalMicrosPerFrame"] if row else 0
    f=sample()
    print("%s placed=%d rendered=%d profiler_total=%.0fus (%.2f each)  frame=%.3fms  delta=%.0fus  frame_delta_per_block=%.2fus"%(bid,n,rend,us,us/max(rend,1),f,(f-base)*1000,(f-base)*1000/n),flush=True)
clear()
