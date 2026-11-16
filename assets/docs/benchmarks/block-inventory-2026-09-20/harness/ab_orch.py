"""In-session A/B/A of a render toggle, by instance id: py -3 ab_orch.py <toggle> <block id>[,<id>...] [crop_top]

Phases baked, perframe, baked, perframe, baked: profiler (6 s warm-up, 6 s window) for the block
types, renderWork mean over 6 s, and three screenshots per phase. Then pixel diffs of the frame
below crop_top (fraction of the height, default 0.0) within phases, same-path and cross-path.
The camera must already be placed and input locked.
"""
import os
import sys
import time

sys.path.insert(0, r'E:\gitRepos\minecraft-city-super-mod\dev-env-utils\scripts')
from mcmcp_orchestrator import Orchestrator  # noqa: E402
from PIL import Image, ImageChops  # noqa: E402

CL = 'run-f2eb58.client'
TOG = sys.argv[1]
TYPES = sys.argv[2].split(',')
TOP = float(sys.argv[3]) if len(sys.argv) > 3 else 0.0
SHOTS = r'E:\gitRepos\minecraft-city-super-mod\run\screenshots'
o = Orchestrator()


def chat(c):
    o.call('client_run_commands', instance=CL, commands=[c])


def prof():
    r = o.call('client_profile_rendering', instance=CL, duration_seconds=6, warmup_seconds=6,
               top=80)
    bt = r.get('tileEntities', {}).get('byType', {})
    cols = bt.get('columns', [])
    tot, n = 0.0, 0
    for row in bt.get('rows', []):
        e = dict(zip(cols, row))
        if e['block'] in TYPES:
            tot += e['totalMicrosPerFrame']
            n += e['count']
    return tot, n


def work():
    r = o.call('client_frame_stats', instance=CL, sample_seconds=6)
    return r['sampled']['renderWork']['meanMs'] if 'sampled' in r else r['last5s']['renderWork']['meanMs']


shots = []
for i, perframe in enumerate([False, True, False, True, False]):
    chat('/csm renderpass %s %s' % ('skip' if perframe else 'draw', TOG))
    time.sleep(2)
    names = []
    for k in range(3):
        nm = 'ab_%s_%d_%d' % (TOG, i, k)
        o.call('client_screenshot', instance=CL, name=nm, hideHud=True)
        names.append(os.path.join(SHOTS, nm + '.png'))
        time.sleep(0.3)
    shots.append((perframe, names))
    t, n = prof()
    w = work()
    print('%-9s profiler %.1f us over %d blocks (%.2f each)  renderWork %.3f ms' % (
        'PERFRAME' if perframe else 'BAKED', t, n, t / n if n else 0, w), flush=True)
chat('/csm renderpass draw %s' % TOG)


def load(p):
    im = Image.open(p).convert('RGB')
    w, h = im.size
    return im.crop((0, int(h * TOP), w, h))


def diff(a, b):
    d = ImageChops.difference(load(a), load(b)).convert('L')
    hist = d.histogram()
    changed = sum(hist) - hist[0]
    return changed, (max(i for i, v in enumerate(hist) if v) if changed else 0)


within = [diff(n[0], n[1]) for _, n in shots]
same, cross = [], []
for i in range(len(shots)):
    for j in range(i + 1, len(shots)):
        (same if shots[i][0] == shots[j][0] else cross).append(diff(shots[i][1][0], shots[j][1][0]))
print('within-phase (changed px, max delta):', within)
print('same-path:', same)
print('cross-path:', cross)
