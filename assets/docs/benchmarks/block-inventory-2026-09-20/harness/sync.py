import sys, json, time, threading
from common import *
# one section, x -112..-97, y 16..31, z -432..-417; thermostat at its centre; flood blockdata syncs
SX,SY,SZ=-112,16,-432
TX,TY,TZ=SX+8,SY+8,SZ+8
fill=sys.argv[1]      # block to fill the rest of the section with (or 'air')
def build():
    S.call("server_set_blocks",{"mode":"fill","block":"minecraft:air","x":SX-4,"y":SY-2,"z":SZ-4,"toX":SX+19,"toY":SY+17,"toZ":SZ+19})
    if fill!="air":
        S.call("server_set_blocks",{"mode":"fill","block":fill,"metadata":0,"x":SX,"y":SY,"z":SZ,"toX":SX+15,"toY":SY+15,"toZ":SZ+15})
    S.call("server_set_blocks",{"mode":"list","blocks":[{"x":TX,"y":TY,"z":TZ,"block":"csm:hvac_thermostat","metadata":0}]})
build()
cmd("tp DEV_USERNAME %.1f %d %d 180 0"%(SX+8,SY+8,SZ+40)); time.sleep(0.6)
try: C.call("client_look",{"yaw":180,"pitch":0})
except Exception: pass
time.sleep(5)
stop=False; n=[0]
def flood(rate):
    v=0
    while not stop:
        v+=1
        cmd("blockdata %d %d %d {cT:%d.5f}"%(TX,TY,TZ,50+(v%40)))
        n[0]+=1
        if rate: time.sleep(rate)
def stats():
    C.call("client_frame_stats",{"sample_seconds":2})
    r=C.call("client_frame_stats",{"sample_seconds":8})["sampled"]
    return round(1000.0/r["fps"],3), r["frame"]["p95Ms"], r["frame"]["maxMs"]
def sections():
    r=C.call("client_profile_sections",{"duration_seconds":8,"min_percent":0.5,"max_depth":6})
    return r
quiet=stats()
print("fill=%s quiet frame ms/p95/max"%fill,quiet,flush=True)
t=threading.Thread(target=flood,args=(0,)); t.start()
time.sleep(1)
busy=stats(); syncs=n[0]
stop=True; t.join()
print("fill=%s flood frame ms/p95/max"%fill,busy," blockdata calls during run ~",syncs,flush=True)
# section profile quiet vs flood
q=sections()
stop=False; n[0]=0
t=threading.Thread(target=flood,args=(0,)); t.start(); time.sleep(1)
f=sections(); stop=True; t.join()
def txt(r):
    return r.get("text") or json.dumps(r)[:1500]
print("---- sections quiet ----"); print(txt(q)[:1500])
print("---- sections flood ----"); print(txt(f)[:1500])
