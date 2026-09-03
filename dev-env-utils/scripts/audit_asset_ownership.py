#!/usr/bin/env python3
"""Asset ownership audit for the CSM resources.

Resolves every blockstate to an owning module by its creative tab (via ``csm_block_index``),
follows each blockstate to the models, parents, OBJ/MTL files and textures it reaches, and
reports (1) per-owner totals, (2) every place a block reaches into a models/textures folder named
for a different subsystem -- the shared assets a module split has to move into Core -- and
(3) unreached models, unreferenced textures, sound-event and lang-key ownership. Written for the
modularization work; see ``assets/docs/agent_progress/``. Run from anywhere; paths are derived
from this file's location.
"""
import os, re, sys, json, collections
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import csm_block_index as cbi
REPO = os.path.abspath(os.path.join(SCRIPT_DIR, "..", "..")); A=os.path.join(REPO,"src","main","resources","assets","csm")
BS=os.path.join(A,"blockstates"); MB=os.path.join(A,"models","block"); TB=os.path.join(A,"textures","blocks")
index,tabs,classes=cbi.build_index()
TABMOD={"tabbuildingmaterials":"buildingmaterials","tabhvac":"hvac","tablifesafety":"lifesafety","tablighting":"lighting","tabnovelties":"novelties","tabpowergrid":"powergrid","tabroadsigns":"trafficsigns","tabtechnology":"technology","tabtrafficaccessories":"trafficaccessories","tabtrafficsignals":"trafficsignals","tabfurniture":"furniture","tabgaming":"novelties","tabmaterials":"materials","none":"(hidden)","tabroadshidden":"(hidden)","tablightinghidden":"(hidden)",None:"(hidden)"}
def load(p):
    try: return json.load(open(p,encoding="utf-8"))
    except Exception: return None
def walk(o,key,out):
    if isinstance(o,dict):
        for k,v in o.items():
            if k==key and isinstance(v,str): out.append(v)
            elif k=="textures" and isinstance(v,dict) and key=="__tex__":
                out.extend(x for x in v.values() if isinstance(x,str) and not x.startswith("#"))
            else: walk(v,key,out)
    elif isinstance(o,list):
        for v in o: walk(v,key,out)
def model_path(ref):
    r=ref.split(":",1)[1] if ":" in ref else ref
    if r.startswith("block/"): r=r[6:]
    for ext in ("",".json",".obj"):
        p=os.path.normpath(os.path.join(MB,r+ext))
        if os.path.isfile(p): return p
    return None
def tex_folder(ref):
    r=ref.split(":",1)[1] if ":" in ref else ref
    if r.startswith("blocks/"):
        rest=r[7:]; return rest.split("/")[0] if "/" in rest else "(textures/blocks root)"
    if r.startswith("items/"): return "(items)"
    return "(other:"+r.split("/")[0]+")"
def model_folder(p):
    rel=os.path.relpath(p,MB).replace("\\","/"); parts=rel.split("/")
    if parts[0]=="shared_models": return "shared_models/"+(parts[1] if len(parts)>2 else "(root)")
    return parts[0] if len(parts)>1 else "(models/block root)"
def collect_model(p,models,texs,seen):
    if p in seen: return
    seen.add(p); models.add(p)
    if p.endswith(".obj"):
        d=os.path.dirname(p)
        for line in open(p,encoding="utf-8",errors="replace"):
            if line.startswith("mtllib"):
                mp=os.path.normpath(os.path.join(d,line.split(None,1)[1].strip()))
                if os.path.isfile(mp):
                    for l2 in open(mp,encoding="utf-8",errors="replace"):
                        if l2.startswith("map_Kd"): texs.add(l2.split(None,1)[1].strip())
        return
    j=load(p)
    if not j: return
    for v in (j.get("textures") or {}).values():
        if isinstance(v,str) and not v.startswith("#"): texs.add(v)
    par=j.get("parent")
    if par:
        pp=model_path(par)
        if pp: collect_model(pp,models,texs,seen)
cross=collections.Counter(); ex=collections.defaultdict(set)
per=collections.defaultdict(lambda: {"blocks":0,"models":set(),"texs":set()})
orphans=[]; used_models=set(); used_texs=set()
for f in sorted(os.listdir(BS)):
    if not f.endswith(".json"): continue
    name=f[:-5]; info=index.get(name); owner=TABMOD.get((info or {}).get("tab"),"(unindexed)")
    if owner=="(unindexed)" and name.endswith("_slab_double"): owner="buildingmaterials"
    if owner=="(unindexed)": orphans.append(name)
    j=load(os.path.join(BS,f))
    if j is None: continue
    refs=[]; walk(j,"model",refs); texs=[]; walk(j,"__tex__",texs)
    models=set(); tset=set(texs); seen=set()
    for r in refs:
        p=model_path(r)
        if p: collect_model(p,models,tset,seen)
    per[owner]["blocks"]+=1; per[owner]["models"]|=models; per[owner]["texs"]|=tset
    used_models|=models; used_texs|=tset
    for m in models:
        mf=model_folder(m)
        base=mf.replace("shared_models/","")
        if base!=owner and not base.startswith("("):
            cross[(owner,"model:"+mf)]+=1; ex[(owner,"model:"+mf)].add(name+" -> "+os.path.relpath(m,MB).replace("\\","/"))
    for t in tset:
        tf=tex_folder(t)
        if tf!=owner and not tf.startswith("("):
            cross[(owner,"texture:"+tf)]+=1; ex[(owner,"texture:"+tf)].add(name+" -> "+t)
print("Blockstates:",sum(v['blocks'] for v in per.values()),"| unindexed:",len(orphans), orphans[:15])
print("\nPER OWNER PACKAGE: blocks / distinct models reached / distinct textures reached")
for k in sorted(per): print(f"  {k:20s} {per[k]['blocks']:5d} {len(per[k]['models']):5d} {len(per[k]['texs']):5d}")
print("\nCROSS-SUBSYSTEM ASSET REFERENCES (owner package -> asset folder of another subsystem)")
for (o,t),n in sorted(cross.items(), key=lambda x:-x[1]):
    print(f"  {o:20s} -> {t:40s} x{n}")
    for e in sorted(ex[(o,t)])[:3]: print("       ",e)
# unused
allm=set(); 
for dp,dn,fn in os.walk(MB):
    for f in fn:
        if f.endswith((".json",".obj")): allm.add(os.path.normpath(os.path.join(dp,f)))
print("\nModel files total:",len(allm),"| reached from a blockstate:",len(used_models&allm),"| unreached:",len(allm-used_models))
unr=collections.Counter(model_folder(m) for m in allm-used_models); print("  unreached by folder:",dict(unr.most_common(12)))
alltex=set()
for dp,dn,fn in os.walk(TB):
    for f in fn:
        if f.endswith(".png"): alltex.add("csm:blocks/"+os.path.relpath(os.path.join(dp,f),TB).replace("\\","/")[:-4])
def norm(t):
    t=t if ":" in t else "csm:"+t
    return t
ut={norm(t) for t in used_texs}
print("Block textures total:",len(alltex),"| referenced:",len(alltex&ut),"| unreferenced:",len(alltex-ut))
print("  unreferenced by folder:",dict(collections.Counter(tex_folder(t) for t in alltex-ut).most_common(12)))
# sounds
sj=load(os.path.join(A,"sounds.json")) or {}
JAVA=os.path.join(REPO,"src","main","java","com","micatechnologies","minecraft","csm")
srcs={}
for dp,dn,fn in os.walk(JAVA):
    for f in fn:
        if f.endswith(".java"):
            pkg=os.path.relpath(dp,JAVA).replace("\\","/").split("/")[0]
            if pkg==".": pkg="(root)"
            srcs.setdefault(pkg,[]).append(open(os.path.join(dp,f),encoding="utf-8",errors="replace").read())
snd_users=collections.defaultdict(set); unref=[]
for key in sj:
    hit=False
    for pkg,texts in srcs.items():
        if pkg in ("(root)","codeutils","tabs"): continue
        if any(('"'+key+'"') in t or ("csm:"+key) in t for t in texts): snd_users[key].add(pkg); hit=True
    if not hit: unref.append(key)
print("\nSOUND EVENTS:",len(sj),"| referenced only via a module sound enum / root or unreferenced by literal:",len(unref))
cnt=collections.Counter()
for k,v in snd_users.items():
    for p in v: cnt[p]+=1
print("  literal users per package:",dict(cnt)); print("  multi-package sounds:",[k for k,v in snd_users.items() if len(v)>1][:20])
print("  no-literal sounds (enum-only?):",unref[:60])
# lang
lang=os.path.join(A,"lang","en_us.lang"); keys=collections.Counter(); other=[]
for line in open(lang,encoding="utf-8",errors="replace"):
    if "=" not in line or line.startswith("#"): continue
    k=line.split("=",1)[0].strip(); pre=k.split(".")[0]; keys[pre]+=1
    if pre not in ("tile","item","itemGroup"): other.append(k)
print("\nLANG en_us prefixes:",dict(keys)); print("  other keys sample:",other[:40], "... total", len(other))
print("  lang files:",os.listdir(os.path.join(A,"lang")))
