import sys, json, time
from common import *
# day vs night, 16 copies each, profile
X0,Z0,Y=-110,-420,4
ids=sys.argv[1].split(",")
def prof():
    C.call("client_profile_rendering",{"duration_seconds":3,"top":50})
    r=C.call("client_profile_rendering",{"duration_seconds":5,"top":50})
    return {e["block"]:e for e in r["tileEntities"]["byType"]}
for bid in ids:
    for yy in range(Y,Y+14):
        S.call("server_set_blocks",{"mode":"fill","block":"minecraft:air","x":X0-4,"y":yy,"z":Z0-4,"toX":X0+80,"toY":yy,"toZ":Z0+40})
    pos=[(X0+(i%8)*8,Y,Z0+(i//8)*8) for i in range(16)]
    S.call("server_set_blocks",{"mode":"list","blocks":[{"x":x,"y":y,"z":z,"block":bid,"metadata":0} for x,y,z in pos]})
    cmd("tp DEV_USERNAME %.1f 12 %d 180 0"%(X0+30,Z0+40)); time.sleep(0.6)
    try: C.call("client_look",{"yaw":180,"pitch":10})
    except Exception: pass
    out={}
    for label,t in (("day",6000),("night",18000)):
        cmd("time set %d"%t); time.sleep(3)
        p=prof().get(bid)
        out[label]=(p["count"],round(p["totalMicrosPerFrame"]/p["count"],2)) if p else None
    print(bid,out,flush=True)
cmd("time set 6000")
