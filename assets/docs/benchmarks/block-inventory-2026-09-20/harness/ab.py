"""In-session A/B/A of a render toggle: python ab.py <block> <meta> <toggle> [n] [night] [pitch]

Places n copies (16x16 grid, spacing 3), then measures frame time and profiler cost with the
toggle off (baked, shipping), on (per frame), off again; takes screenshots of each phase and
reports pixel differences between same-path and cross-path frames on a crop with no sky.
"""
import sys, json, time, os
sys.path.insert(0, r"E:\gitRepos\minecraft-city-super-mod\assets\docs\benchmarks\block-inventory-2026-09-20\harness")
from common import *
from PIL import Image, ImageChops

bid = sys.argv[1]; META = int(sys.argv[2]); TOG = sys.argv[3]
N = int(sys.argv[4]) if len(sys.argv) > 4 else 256
NIGHT = len(sys.argv) > 5 and sys.argv[5] == "night"
PITCH = float(sys.argv[6]) if len(sys.argv) > 6 else 22
G = 16; SP = 3; X0 = -110; Z0 = -420; Y = 4
SHOTS = r"E:\gitRepos\minecraft-city-super-mod\run\screenshots"


def clear():
    S.call("server_set_blocks", {"mode": "fill", "block": "minecraft:air", "x": X0 - 2, "y": Y, "z": Z0 - 2,
                                 "toX": X0 + G * SP + 2, "toY": Y + 7, "toZ": Z0 + G * SP + 2})


def cam():
    cmd("tp DEV_USERNAME %.1f 20 %d 180 %d" % (X0 + G * SP / 2.0, Z0 + G * SP + 30, PITCH)); time.sleep(1)
    C.call("client_look", {"yaw": 180, "pitch": PITCH}); time.sleep(0.5)


def frame():
    C.call("client_frame_stats", {"sample_seconds": 2})
    r = C.call("client_frame_stats", {"sample_seconds": 6})["sampled"]
    return 1000.0 / r["fps"]


def prof():
    r = C.call("client_profile_rendering", {"duration_seconds": 4, "warmup_seconds": 4, "top": 1, "type": bid})
    bt = [e for e in r["tileEntities"]["byType"] if e["block"] == bid]
    return (bt[0]["totalMicrosPerFrame"], bt[0]["count"], bt[0].get("microsPerCall")) if bt else (0, 0, None)


def toggle(on):
    cmd("csm renderpass %s %s" % ("skip" if on else "draw", TOG))


cmd("time set %d" % (18000 if NIGHT else 6000))
clear(); cam(); time.sleep(3)
base = frame()
order = [(i * 97) % (G * G) for i in range(G * G)]
S.call("server_set_blocks", {"mode": "list", "blocks": [
    {"x": X0 + (o % G) * SP, "y": Y, "z": Z0 + (o // G) * SP, "block": bid, "metadata": META} for o in order[:N]]})
cam(); time.sleep(6)
res = []
shots = []
for i, on in enumerate([False, True, False, True, False]):
    toggle(on); time.sleep(2)
    names = []
    for k in range(3):
        nm = "ab_%s_%d_%d" % ("live" if on else "baked", i, k)
        C.call("client_screenshot", {"name": nm}); names.append(os.path.join(SHOTS, nm + ".png")); time.sleep(0.3)
    shots.append((on, names))
    f = frame(); p = prof()
    res.append((on, f, p))
    print("%-6s frame=%.3f ms (+%.1f us/block over base %.3f)  profiler total=%.0f us  n=%d  perCall=%s" % (
        "LIVE" if on else "BAKED", f, (f - base) * 1000 / N, base, p[0], p[1], p[2]), flush=True)
toggle(False)


def load(p):
    im = Image.open(p).convert("RGB"); w, h = im.size
    return im.crop((0, int(h * 0.42), w, h))  # below the horizon: no stars or clouds


def diff(a, b):
    d = ImageChops.difference(load(a), load(b)).convert("L")
    hist = d.histogram(); tot = sum(hist)
    changed = tot - hist[0]
    return changed, max(i for i, v in enumerate(hist) if v) if changed else 0


same = []; cross = []
for i in range(len(shots)):
    for j in range(len(shots)):
        if j <= i: continue
        for a in shots[i][1][:1]:
            for b in shots[j][1][:1]:
                (same if shots[i][0] == shots[j][0] else cross).append(diff(a, b))
within = [diff(n[0], n[1]) for _, n in shots]
print("within-phase diffs (changed px, max delta):", within)
print("same-path diffs:", same)
print("cross-path diffs:", cross)
if "--keep" not in sys.argv:
    clear()
