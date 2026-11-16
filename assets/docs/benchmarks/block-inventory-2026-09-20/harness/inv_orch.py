"""New-renderer inventory through the MCMCP orchestrator, by instance id (never a port).

py -3 inv_orch.py <out.json> <id> [<id> ...]
Places 8 copies of each block (2x4, spacing 2) in batches of 12 at x=-100.., z=-390, y=4,
camera 36 blocks back, 6 s warm-up and 6 s window, client_profile_rendering byType.
"""
import json
import os
import sys
import time

sys.path.insert(0, r'E:\gitRepos\minecraft-city-super-mod\dev-env-utils\scripts')
from mcmcp_orchestrator import Orchestrator  # noqa: E402

CL = 'run-f2eb58.client'
SV = 'run-f2eb58.server'
OUT = sys.argv[1]
IDS = sys.argv[2:]
PER_BATCH = 12
CELL = 6
X0, Z0, Y = -100, -390, 4
COPIES = [(dx, dz) for dx in (0, 2) for dz in (0, 2, 4, 6)]

o = Orchestrator()


def cmd(c):
    return o.call('server_run_command', instance=SV, command=c)


def clear():
    o.call('server_set_blocks', instance=SV, mode='fill', block='minecraft:air',
           x=X0 - 4, y=Y, z=Z0 - 4, toX=X0 + CELL * PER_BATCH + 4, toY=Y + 15, toZ=Z0 + 12)


cmd('tp @a %d 9 %d 180 10' % (X0 + 36, Z0 + 36))
time.sleep(6)
results = {}
if os.path.exists(OUT):
    results = json.load(open(OUT, encoding='utf-8'))
batches = [IDS[i:i + PER_BATCH] for i in range(0, len(IDS), PER_BATCH)]
for bi, batch in enumerate(batches):
    clear()
    pl = []
    for ti, b in enumerate(batch):
        for dx, dz in COPIES:
            pl.append({'x': X0 + ti * CELL + dx, 'y': Y, 'z': Z0 + dz, 'block': b, 'metadata': 0})
    w = o.call('server_set_blocks', instance=SV, mode='list', blocks=pl)
    print('placed', w.get('written'), 'unloaded', w.get('unloaded'), flush=True)
    cx = X0 + len(batch) * CELL / 2.0
    cmd('tp @a %.1f 9 %d 180 10' % (cx, Z0 + 36))
    time.sleep(2.5)
    o.call('client_look', instance=CL, yaw=180, pitch=10)
    time.sleep(1)
    r = o.call('client_profile_rendering', instance=CL, duration_seconds=6, warmup_seconds=6, top=60)
    bt = r.get('tileEntities', {}).get('byType', {})
    cols = bt.get('columns', [])
    by = {}
    for row in bt.get('rows', []):
        e = dict(zip(cols, row))
        by[e['block']] = e
    for b in batch:
        e = by.get(b)
        results[b] = {
            'rendered': e['count'] if e else 0,
            'renderer': (e.get('renderer') or '').split('.')[-1] if e else None,
            'us_total': e['totalMicrosPerFrame'] if e else 0.0,
            'us_each': round(e['totalMicrosPerFrame'] / e['count'], 2) if e and e['count'] else 0.0,
            'per_call': e.get('microsPerCall') if e else None,
        }
        print('%-40s %s' % (b, results[b]), flush=True)
    json.dump(results, open(OUT, 'w', encoding='utf-8'), indent=1)
clear()
print('DONE')
