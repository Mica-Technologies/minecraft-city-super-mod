"""Audits how heavy the JSON block models are, and how much of that weight can never be seen.

For every JSON model under the asset trees it counts the quads (element faces) and finds the
hidden ones: a face whose whole area sits against the inside of another closed opaque box (all six
faces present, every one opaque) of the same model (the bottom of a can standing on a shelf, the back of a panel pressed to a wall of the same
piece). A hidden face is never drawn on screen but is still baked, still sits in every chunk
mesh the block is in, and still costs memory and upload time. The faces of the same model are
always drawn together, so a face hidden by its own model is hidden in every state.

"Opaque" is judged off the texture: a covering box counts only if the pixels of its faces that
the hidden face would have shown through are all opaque, so glass, cutouts and see-through
textures never hide anything.

    python audit_model_weight.py [--top 40] [--module furnishings]

Prints the heaviest models, the models with the most hidden quads, and totals per module.
"""
import argparse
import collections
import glob
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import model_depth as md  # noqa: E402

STEP = 0.5     # sampling grid over a face, in pixels
EPS = 0.02     # how far in front of a face a sample is taken


def _inside(el, p):
    """Is point p strictly inside element el's box (after its rotation)?"""
    lp = md._rot(p, el.get("rotation"), -1)
    f, t = el["from"], el["to"]
    return all(min(f[k], t[k]) + 1e-4 < lp[k] < max(f[k], t[k]) - 1e-4 for k in range(3))


def _box_opaque(store, el, tex):
    """Whether every face texture of a box is fully opaque over the area it samples."""
    for fn, face in el.get("faces", {}).items():
        im = store.image(md._resolve(tex, face.get("texture")))
        if im is None:
            return False
        uv = face.get("uv") or md._default_uv(fn, el["from"], el["to"])
        w, h = im.size
        x0, x1 = sorted((uv[0] / 16 * w, uv[2] / 16 * w))
        y0, y1 = sorted((uv[1] / 16 * h, uv[3] / 16 * h))
        box = (int(x0), int(y0), max(int(x0) + 1, int(round(x1))), max(int(y0) + 1, int(round(y1))))
        alpha = im.crop(box).getchannel("A")
        if alpha.getextrema()[0] < 250:
            return False
    return True


def hidden_faces(store, name):
    """[(element index, face name)] of the faces in model `name` its own opaque boxes hide."""
    erel, els, tex = store.chain(name)
    if not els:
        return erel, 0, []
    opaque = [i for i, el in enumerate(els)
              if len(el.get("faces", {})) == 6 and _box_opaque(store, el, tex)]
    out = []
    total = 0
    for i, el in enumerate(els):
        for fn in el.get("faces", {}):
            total += 1
            q = md._Face(store, erel, i, el, fn, tex)
            us = [p[q.ax[0]] for p in q.pts]
            vs = [p[q.ax[1]] for p in q.pts]
            u0, u1, v0, v1 = min(us), max(us), min(vs), max(vs)
            if u1 - u0 < 1e-3 or v1 - v0 < 1e-3:
                continue
            covered = True
            u = u0 + min(STEP, (u1 - u0) / 2) / 2
            nu = max(1, int((u1 - u0) / STEP))
            nv = max(1, int((v1 - v0) / STEP))
            for a in range(nu + 1):
                if not covered:
                    break
                u = u0 + (u1 - u0) * (a + 0.5) / (nu + 1)
                for b in range(nv + 1):
                    v = v0 + (v1 - v0) * (b + 0.5) / (nv + 1)
                    p = [0.0, 0.0, 0.0]
                    p[q.ax[0]], p[q.ax[1]] = u, v
                    p[q.drop] = (q.d - sum(p[k] * q.n[k] for k in q.ax)) / q.n[q.drop]
                    if not q.inside(p):
                        continue
                    front = [p[k] + q.n[k] * EPS for k in range(3)]
                    if not any(j != i and _inside(els[j], front) for j in opaque):
                        covered = False
                        break
            if covered:
                out.append((i, fn))
    return erel, total, out


def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--top", type=int, default=30)
    ap.add_argument("--module", help="limit to one module folder (or 'core')")
    args = ap.parse_args(argv)
    roots = {"core": md._TREES[0]}
    for t in md._TREES[1:]:
        roots[t.split(os.sep)[-6]] = t
    rows = []
    for mod, root in sorted(roots.items()):
        if args.module and mod != args.module:
            continue
        store = md._Store(root)
        for p in sorted(glob.glob(os.path.join(root, "models", "block", "**", "*.json"), recursive=True)):
            rel = os.path.relpath(p, os.path.join(root, "models", "block")).replace(os.sep, "/")
            name = "csm:" + rel[:-5]
            try:
                d = json.load(open(p, encoding="utf-8"))
            except ValueError:
                continue
            if "elements" not in d:
                continue
            erel, total, hid = hidden_faces(store, name)
            rows.append((mod, rel, total, len(hid)))
    per = collections.defaultdict(lambda: [0, 0, 0])
    for mod, rel, total, hid in rows:
        per[mod][0] += 1
        per[mod][1] += total
        per[mod][2] += hid
    print("module          models   quads   hidden  (share)")
    for mod, (n, q, h) in sorted(per.items()):
        print("%-14s %7d %7d %8d  %5.1f%%" % (mod, n, q, h, 100.0 * h / max(q, 1)))
    print("\nheaviest models (quads):")
    for mod, rel, total, hid in sorted(rows, key=lambda r: -r[2])[:args.top]:
        print("  %6d  %4d hidden  %s/%s" % (total, hid, mod, rel))
    print("\nmost hidden quads:")
    for mod, rel, total, hid in sorted(rows, key=lambda r: -r[3])[:args.top]:
        print("  %4d of %5d  %s/%s" % (hid, total, mod, rel))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
