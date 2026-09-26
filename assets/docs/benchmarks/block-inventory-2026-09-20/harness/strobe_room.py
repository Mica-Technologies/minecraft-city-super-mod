"""Fire alarm strobes, alarm off vs on, one panel driving N strobes.

A strobe draws only while its position is in the client's ActiveStrobeRegistry, which the control
panel fills over the network when its alarm is on. Appliances come from the panel's `apps` NBT
string, one "x y z" per line. The separator is a real newline: SNBT rejects the two-character
escape backslash-n ("Invalid escape") but takes a literal line feed, which is what this sends.

Do NOT give each strobe its own panel: every panel sends on the shared strobe-only channel and a
new START replaces the channel's positions on the client, so only one strobe stays active (the
first version of this test measured exactly that and reported a cost 30x too small).

    python strobe.py [block] [count] [cols] [distance]       env NIGHT=1 for night

The profiler average covers every frame, including the ~85% of each second a strobe is dark (the
flash is 75 ms plus a 75 ms fade in each 1000 ms), so "per flash frame" divides by 0.15.
"""
import os, sys
from common import *

BLOCK = sys.argv[1] if len(sys.argv) > 1 else "csm:firealarmsystemsensorlserieshornstrobered"
N = int(sys.argv[2]) if len(sys.argv) > 2 else 30
COLS = int(sys.argv[3]) if len(sys.argv) > 3 else 15
DIST = float(sys.argv[4]) if len(sys.argv) > 4 else 16
NIGHT = os.environ.get("NIGHT") == "1"   # cone and light pools scale with darkness; noon draws mostly the lens
X0, Y, Z0 = -100, 4, -400
ROWS = (N + COLS - 1) // COLS

for yy in range(Y, Y + 14):
    S.call("server_set_blocks", {"mode": "fill", "block": "minecraft:air", "x": X0 - 8, "y": yy, "z": Z0 - 8,
                                 "toX": X0 + 80, "toY": yy, "toZ": Z0 + 3 * ROWS + 12})
ROOM = os.environ.get("ROOM") == "1"
if ROOM:
    # A closed stone room: strobes on the north wall's inner face, facing into the room (south),
    # one row per course; the camera stands inside at the far end.
    D = int(os.environ.get("DEPTH", "12"))
    RX0, RX1, RZ0, RZ1, RY1 = X0 - 3, X0 + COLS + 2, Z0 - 1, Z0 + D + 1, Y + ROWS + 3
    S.call("server_set_blocks", {"mode": "fill", "block": "minecraft:stone", "x": RX0, "y": Y - 1, "z": RZ0,
                                 "toX": RX1, "toY": RY1, "toZ": RZ1})
    S.call("server_set_blocks", {"mode": "fill", "block": "minecraft:air", "x": RX0 + 1, "y": Y, "z": RZ0 + 1,
                                 "toX": RX1 - 1, "toY": RY1 - 1, "toZ": RZ1 - 1})
    pos = [(X0 + (i % COLS), Y + 1 + (i // COLS), Z0) for i in range(N)]
    META = 3
else:
    pos = [(X0 + (i % COLS), Y, Z0 + 2 * (i // COLS)) for i in range(N)]
    META = 0
PANEL = (X0 - 2, Y, Z0 + 2) if ROOM else (X0 - 3, Y, Z0)
S.call("server_set_blocks", {"mode": "list", "blocks":
       [{"x": PANEL[0], "y": PANEL[1], "z": PANEL[2], "block": "csm:firealarmcontrolpanel", "metadata": 0}] +
       [{"x": x, "y": y, "z": z, "block": BLOCK, "metadata": META} for x, y, z in pos]})
time.sleep(1)
blob = "\n".join("%d %d %d" % p for p in pos)
cmd('blockdata %d %d %d {apps:"%s",aSi:1b}' % (PANEL + (blob,)))
cmd("time set %d" % (18000 if NIGHT else 6000))
if ROOM:
    cmd("tp @p %.1f %d %.1f 180 0" % (X0 + COLS / 2.0, Y, Z0 + D - 0.5)); time.sleep(0.8)
else:
    cmd("tp @p %.1f 4 %.1f 180 0" % (X0 + COLS / 2.0, Z0 + 2 * ROWS + DIST)); time.sleep(0.8)
try: C.call("client_look", {"yaw": 180, "pitch": 0})
except Exception: pass
time.sleep(2)


def prof():
    C.call("client_profile_rendering", {"duration_seconds": 6, "top": 50})
    r = C.call("client_profile_rendering", {"duration_seconds": 8, "top": 50})
    row = [e for e in r["tileEntities"]["byType"] if e["block"] == BLOCK]
    C.call("client_frame_stats", {"sample_seconds": 2})
    st = C.call("client_frame_stats", {"sample_seconds": 8})["sampled"]
    return (row[0]["count"], row[0]["totalMicrosPerFrame"]) if row else (0, 0.0), 1000.0 / st["fps"], st["frame"]["p99Ms"]


off, off_f, off_p99 = prof()
cmd("blockdata %d %d %d {a:1b}" % PANEL)
time.sleep(4)
on, on_f, on_p99 = prof()
cmd("blockdata %d %d %d {a:0b}" % PANEL)
cmd("time set 6000")
d = on[1] - off[1]
print(C.call("client_screenshot", {})["path"])
cmd("blockdata %d %d %d {a:1b}" % PANEL); time.sleep(0.3)
print(C.call("client_screenshot", {})["path"])
cmd("blockdata %d %d %d {a:0b}" % PANEL)
print("%s  N=%d dist=%d %s %s" % (BLOCK, N, DIST, "night" if NIGHT else "noon", "ROOM" if ROOM else "open"))
print("  alarm off: rendered=%d profiler=%.1f us/frame  frame=%.3f ms  p99=%.2f" % (off[0], off[1], off_f, off_p99))
print("  alarm on : rendered=%d profiler=%.1f us/frame  frame=%.3f ms  p99=%.2f" % (on[0], on[1], on_f, on_p99))
print("  added: %.1f us/frame avg = %.2f us per strobe avg = ~%.1f us per strobe on a flash frame; frame delta %.0f us"
      % (d, d / max(on[0], 1), d / max(on[0], 1) / 0.15, (on_f - off_f) * 1000))
