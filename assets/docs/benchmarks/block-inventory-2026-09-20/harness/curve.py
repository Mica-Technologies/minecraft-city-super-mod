import sys, json, time
from common import *
bid=sys.argv[1]; counts=[int(x) for x in sys.argv[2].split(",")]; META=int(sys.argv[3]) if len(sys.argv)>3 else 0
G=16; SP=3; X0=-110; Z0=-420; Y=4
def clear():
    S.call("server_set_blocks",{"mode":"fill","block":"minecraft:air","x":X0-2,"y":Y,"z":Z0-2,"toX":X0+G*SP+2,"toY":Y+7,"toZ":Z0+G*SP+2})
def cam():
    cmd("tp DEV_USERNAME %.1f 20 %d 180 0" % (X0+G*SP/2.0, Z0+G*SP+30))
    time.sleep(0.5)
    try: C.call("client_look",{"yaw":180,"pitch":22})
    except Exception as e: pass
def sample(sec=6):
    C.call("client_frame_stats",{"sample_seconds":2})
    r=C.call("client_frame_stats",{"sample_seconds":sec})["sampled"]
    return 1000.0/r["fps"], r["frame"]["meanMs"]
order=[(i*97)%(G*G) for i in range(G*G)]
clear(); cam(); time.sleep(6)
base=[sample()]
out=[]
for n in counts:
    clear()
    pl=[{"x":X0+(o%G)*SP,"y":Y,"z":Z0+(o//G)*SP,"block":bid,"metadata":META} for o in order[:n]]
    if pl: S.call("server_set_blocks",{"mode":"list","blocks":pl})
    cam(); time.sleep(6)
    f,m=sample()
    out.append((n,f,m))
    print(bid,n,"1/fps=%.3f mean=%.3f  delta_us=%.1f  per_block_us=%.2f"%(f,m,(f-base[0][0])*1000,(f-base[0][0])*1000/max(n,1)),flush=True)
clear(); time.sleep(2); base.append(sample()); print("baselines",base)
