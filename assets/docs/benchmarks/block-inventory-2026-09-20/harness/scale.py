import sys, json, time
from common import *
OUT=sys.argv[1]; IDS=open(sys.argv[2]).read().split()
META=int(sys.argv[3]) if len(sys.argv)>3 else 0
G=8; SP=5; X0=-110; Z0=-420; Y=4
def clear():
    S.call("server_set_blocks",{"mode":"fill","block":"minecraft:air","x":X0-2,"y":Y,"z":Z0-2,"toX":X0+G*SP+2,"toY":Y+7,"toZ":Z0+G*SP+2})
def cam():
    cmd("tp DEV_USERNAME %.1f 24 %d 180 32" % (X0+G*SP/2.0, Z0+G*SP+40))
def sample(sec=6):
    C.call("client_frame_stats",{"sample_seconds":2})
    r=C.call("client_frame_stats",{"sample_seconds":sec})["sampled"]
    return {"fps":r["fps"],"frame_mean":r["frame"]["meanMs"],"frame_med":r["frame"]["medianMs"],"frame_p95":r["frame"]["p95Ms"],
            "work_mean":r["renderWork"]["meanMs"],"work_med":r["renderWork"]["medianMs"],"low1":r["low1PercentFps"]}
res={"baselines":[], "blocks":{}}
clear(); cam(); time.sleep(3)
res["baselines"].append(sample())
for i,bid in enumerate(IDS):
    clear()
    pl=[{"x":X0+gx*SP,"y":Y,"z":Z0+gz*SP,"block":bid,"metadata":META} for gx in range(G) for gz in range(G)]
    S.call("server_set_blocks",{"mode":"list","blocks":pl})
    cam(); time.sleep(3)
    cen=[e["count"] for e in S.call("server_census",{"top":50})["dimensions"][0]["tileEntities"]["byType"] if e["block"]==bid]
    m=sample(); m["census"]=cen[0] if cen else 0
    res["blocks"][bid]=m
    print(bid,m,flush=True)
    if (i+1)%6==0:
        clear(); time.sleep(2); res["baselines"].append(sample()); print("baseline",res["baselines"][-1],flush=True)
    json.dump(res,open(OUT,"w"),indent=1)
clear(); time.sleep(2); res["baselines"].append(sample())
json.dump(res,open(OUT,"w"),indent=1)
print("DONE")
