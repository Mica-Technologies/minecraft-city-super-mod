#!/usr/bin/env python3
"""Deterministic traffic-sign generator.

Reads a classification JSON ({"results":[{file,category,shape,humanName,special}...]})
produced by the classify-staged-signs workflow and emits, for each road sign:
  - texture copy        -> assets/csm/textures/blocks/trafficsigns/<textureBase>.png
  - blockstate JSON     -> assets/csm/blockstates/<registry>.json   (from per-shape template)
  - lang line           -> appended to en_us.lang
  - tab registration    -> inserted into CsmTabRoadSigns.initTabElements

Non-road-signs and flagged state-pairs are reported and skipped (handled separately).
Run from repo root:  python dev-env-utils/scripts/gen_signs.py <classification.json> [--apply]
Without --apply it does a dry run (prints the plan, writes nothing).
"""
import json, os, re, sys, shutil, copy

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
STAGED = os.path.join(REPO, "assets/to-be-added-to-mod/textures/blocks")
ASSETS = os.path.join(REPO, "src/main/resources/assets/csm")
BLOCKSTATES = os.path.join(ASSETS, "blockstates")
TEX_OUT = os.path.join(ASSETS, "textures/blocks/trafficsigns")
LANG = os.path.join(ASSETS, "lang/en_us.lang")
TAB = os.path.join(REPO, "src/main/java/com/micatechnologies/minecraft/csm/tabs/CsmTabRoadSigns.java")
TEMPLATE_BLOCKSTATE = os.path.join(BLOCKSTATES, "beachclosedsign.json")  # diamond structural template

# agent shape enum -> (front model, setback model, back_to_back model)  [all under csm:trafficsigns/]
SHAPE_MAP = {
    "diamond":        ("absolutely_nothing_sign",              "metal_signpostback_diamond_sign_setback",          "metal_signpostback_diamond_sign_back_to_back"),
    "portrait_rect":  ("bikes_use_ped_signal_sign",            "metal_signpostback_tall_sign_setback",             "metal_signpostback_tall_sign_back_to_back"),
    "landscape_rect": ("ahead_straight_arrow_sign",            "metal_signpostback_sign_setback",                  "metal_signpostback_sign_back_to_back"),
    "wide_short_rect":("begin_left_lane_yield_to_bikes_sign",  "metal_signpostback_largewidesign_setback",         "metal_signpostback_largewidesign_back_to_back"),
    "square":         ("bikes_allowed_to_use_full_lane_sign_large","metal_signpostback_medium_square_sign_setback","metal_signpostback_medium_square_sign_back_to_back"),
    "circle":         ("fdc_standpipe_bullseye_sign",          "metal_signpostback_circle_sign_setback",           "metal_signpostback_circle_sign_back_to_back"),
    "octagon":        ("lhs_stop_sign_not_compliant",          "metal_signpost_octagon_sign_setback",              "metal_signpost_octagon_sign_back_to_back"),
    "strip":          ("sign_strip_green",                     "metal_signpostback_strip_setback",                 "metal_signpostback_strip_back_to_back"),
}

# Electronic/illuminated 2-state signs — deferred for a powered on/off block (Phase 1 follow-up).
DEFER = {
    "adaptive_no_uturn_sign",
    "adaptive_train_sign",
    "lights_out_no_power_sign_on",
    "lights_out_no_power_sign_off",
}

def registry_name(texture_base):
    return re.sub(r"[^a-z0-9]", "", texture_base.lower())

def build_blockstate(template, model, setback, b2b, tex_ref):
    d = copy.deepcopy(template)
    d["defaults"]["model"] = f"csm:trafficsigns/{model}"
    t = d["defaults"]["textures"]
    t["all"] = tex_ref
    t["particle"] = tex_ref
    t["1"] = tex_ref
    # "0" (post) stays csm:blocks/trafficsigns/absolutely_nothing_sign (constant across shapes)
    sh = d["variants"]["shift"]
    sh["setback"]["model"] = f"csm:trafficsigns/{setback}"
    sh["backtoback"]["model"] = f"csm:trafficsigns/{b2b}"
    return d

def main():
    if len(sys.argv) < 2:
        print("usage: gen_signs.py <classification.json> [--apply]"); sys.exit(1)
    apply = "--apply" in sys.argv
    data = json.load(open(sys.argv[1], encoding="utf-8"))
    results = data["results"] if isinstance(data, dict) else data
    template = json.load(open(TEMPLATE_BLOCKSTATE, encoding="utf-8"))
    existing_bs = {f[:-5] for f in os.listdir(BLOCKSTATES) if f.endswith(".json")}

    signs, skipped, problems = [], [], []
    seen = set()
    for r in results:
        f = r["file"]; base = os.path.splitext(f)[0]
        if r.get("category") != "roadsign":
            skipped.append((f, r.get("category"), r.get("special",""))); continue
        # Only defer the genuinely electronic/illuminated 2-state signs (need a powered block).
        # Color/mirror companions (_rr orange, _rw, etc.) are plain independent signs.
        if base in DEFER:
            skipped.append((f, "electronic 2-state (deferred)", r.get("special",""))); continue
        shape = r.get("shape")
        if shape not in SHAPE_MAP:
            problems.append((f, f"unmapped shape '{shape}'")); continue
        reg = registry_name(base)
        if reg in existing_bs or reg in seen:
            problems.append((f, f"registry collision '{reg}'")); continue
        seen.add(reg)
        hn = (r["humanName"] or "").replace("&amp;", "&").strip()
        signs.append((f, base, reg, shape, hn))

    print(f"== {len(signs)} signs to generate, {len(skipped)} skipped, {len(problems)} problems ==")
    for f, why in problems: print(f"  PROBLEM {f}: {why}")
    for f, cat, note in skipped: print(f"  skip    {f}: {cat} {note}")

    if not apply:
        print("\n-- dry run; pass --apply to write --")
        for f, base, reg, shape, hn in signs:
            print(f"  {reg:34} <- {f:40} [{shape}] '{hn}'")
        return

    os.makedirs(TEX_OUT, exist_ok=True)
    lang_lines, tab_lines = [], []
    from PIL import Image
    for f, base, reg, shape, hn in signs:
        model, setback, b2b = SHAPE_MAP[shape]
        tex_ref = f"csm:blocks/trafficsigns/{base}"
        # texture (convert jpg->png)
        src = os.path.join(STAGED, f); dst = os.path.join(TEX_OUT, base + ".png")
        if f.lower().endswith(".jpg"):
            Image.open(src).convert("RGBA").save(dst)
        else:
            shutil.copyfile(src, dst)
        # blockstate
        bs = build_blockstate(template, model, setback, b2b, tex_ref)
        with open(os.path.join(BLOCKSTATES, reg + ".json"), "w", encoding="utf-8") as out:
            json.dump(bs, out, indent=2)
        lang_lines.append(f"tile.{reg}.name={hn}")
        tab_lines.append(f'    initTabBlock(new BlockTrafficSign("{reg}"));')

    # append lang
    with open(LANG, "a", encoding="utf-8") as lf:
        lf.write("\n# --- staged traffic signs batch ---\n" + "\n".join(sorted(lang_lines)) + "\n")
    # insert tab registrations before the closing brace of initTabElements
    src = open(TAB, encoding="utf-8").read()
    marker = "    initTabBlock(new BlockTrafficSign("
    idx = src.rfind(marker)
    eol = src.index("\n", idx)
    src = src[:eol+1] + "\n".join(tab_lines) + "\n" + src[eol+1:]
    open(TAB, "w", encoding="utf-8").write(src)
    print(f"WROTE {len(signs)} signs (textures, blockstates, {len(lang_lines)} lang, {len(tab_lines)} tab regs)")

if __name__ == "__main__":
    main()
