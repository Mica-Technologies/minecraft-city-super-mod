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

    python audit_model_weight.py --relief [--depth 1.0] [--max-area 64] [--variants blocks.csv]
                                 [--csv out.csv]

Lists fine relief that could be painted into the texture instead: an unrotated box no deeper than
--depth pixels (a zero-thickness decal plane counts too) whose back sits flush on, or is sunk into,
a face of a larger box of the same model, with a footprint of at most --max-area square pixels --
a keypad key, a card slot, a vent, a button. Flattening one removes its quads from every bake of
the model, so each candidate is weighted by how many baked variants draw it: the variants the
block has (models plus item models from a `/csm memstats dump` blocks.csv, else 1), times the
share of the blockstate's variant entries that name the model (1 for a Forge `defaults` model).
Ranked by quads saved times variants. OBJ models are not covered. A candidate is a suggestion,
not a verdict: whether a detail still reads when painted is decided on a before/after sheet.
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


def _model_refs(node, out, in_defaults=False):
    """Collect (model name, named in Forge defaults) from a blockstate JSON, at any depth."""
    if isinstance(node, dict):
        for k, v in node.items():
            if k == "model" and isinstance(v, str):
                out.append((v, in_defaults))
            elif k == "apply":
                for m in (v if isinstance(v, list) else [v]):
                    if isinstance(m, dict) and isinstance(m.get("model"), str):
                        out.append((m["model"], in_defaults))
            else:
                _model_refs(v, out, in_defaults or k == "defaults")
    elif isinstance(node, list):
        for v in node:
            _model_refs(v, out, in_defaults)


def _variant_counts(path):
    """registry name -> baked variants (block models + item models), from a memstats blocks.csv."""
    import csv
    out = {}
    with open(path, encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            if row["id"].startswith("csm:"):
                out[row["id"][4:]] = max(1, int(row["models"] or 0)
                                         + int(row["itemLocations"] or 0))
    return out


def _box(el):
    f, t = el["from"], el["to"]
    return [min(f[k], t[k]) for k in range(3)], [max(f[k], t[k]) for k in range(3)]


def relief_candidates(roots, depth, max_area, variants, only_module=None):
    """[(baked quads saved, quads, variants, model rel, element index, detail, blocks)]"""
    store = md._Store(roots["core"])
    weight = collections.defaultdict(float)
    users = collections.defaultdict(set)
    for mod, root in sorted(roots.items()):
        if only_module and mod != only_module:
            continue
        for p in glob.glob(os.path.join(root, "blockstates", "*.json")):
            block = os.path.splitext(os.path.basename(p))[0]
            try:
                d = json.load(open(p, encoding="utf-8"))
            except ValueError:
                continue
            refs = []
            _model_refs(d, refs)
            refs = [(m, dflt) for m, dflt in refs if not m.endswith((".obj", ".b3d"))]
            if not refs:
                continue
            names = set(m for m, _ in refs)
            entries = sum(1 for _, dflt in refs if not dflt) or 1
            per_model = collections.Counter(m for m, dflt in refs if not dflt)
            defaults = set(m for m, dflt in refs if dflt)
            bakes = variants.get(block, 1)
            # a Forge defaults model that some property replaces for every value is only drawn
            # by the variants that property does not reach (the inventory item, in practice)
            overridden = isinstance(d.get("variants"), dict) and any(
                isinstance(vals, dict) and vals and all(
                    isinstance(v, dict) and "model" in v for v in vals.values())
                for vals in d["variants"].values())
            for m in names:
                if m in defaults and overridden:
                    share = 1.0 / bakes
                elif m in defaults or len(names) == 1:
                    share = 1.0
                else:
                    share = per_model[m] / float(entries)
                erel, els, _ = store.chain(m)
                if erel and els:
                    weight[erel] += bakes * share
                    users[erel].add(block)
    out = []
    for erel in weight:
        d = store.models.get(erel)
        els = (d or {}).get("elements") or []
        for i, el in enumerate(els):
            if (el.get("rotation") or {}).get("angle") or not el.get("faces"):
                continue
            lo, hi = _box(el)
            dims = [hi[k] - lo[k] for k in range(3)]
            a = min(range(3), key=lambda k: dims[k])
            if dims[a] > depth:
                continue
            u, v = [k for k in range(3) if k != a]
            area = dims[u] * dims[v]
            if area > max_area or area <= 0:
                continue
            base = None
            for j, other in enumerate(els):
                if j == i or (other.get("rotation") or {}).get("angle"):
                    continue
                olo, ohi = _box(other)
                if (ohi[u] - olo[u]) * (ohi[v] - olo[v]) <= area:
                    continue
                if not all(olo[k] - 0.05 <= lo[k] and hi[k] <= ohi[k] + 0.05 for k in (u, v)):
                    continue
                # stands proud of the base's max face, or of its min face
                if (abs(lo[a] - ohi[a]) <= 0.1 or olo[a] <= lo[a] <= ohi[a] < hi[a]) or \
                        (abs(hi[a] - olo[a]) <= 0.1 or lo[a] < olo[a] <= hi[a] <= ohi[a]):
                    base = j
                    break
            if base is None:
                continue
            quads = len(el["faces"])
            w = weight[erel]
            label = el.get("name") or el.get("__comment") or ""
            detail = "%.2f px deep, %.1f x %.1f px, on element %d%s" % (
                dims[a], dims[u], dims[v], base, (" (%s)" % label) if label else "")
            out.append((quads * w, quads, w, erel, i, detail, sorted(users[erel])[:3]))
    out.sort(key=lambda r: -r[0])
    return out


def relief_report(args, roots):
    variants = _variant_counts(args.variants) if args.variants else {}
    cands = relief_candidates(roots, args.depth, args.max_area, variants, args.module)
    per_model = collections.defaultdict(lambda: [0, 0, 0.0, 0.0, []])
    for wq, q, w, erel, i, detail, blocks in cands:
        pm = per_model[erel]
        pm[0] += 1
        pm[1] += q
        pm[2] = w
        pm[3] += wq
        pm[4] = blocks
    total_q = sum(c[1] for c in cands)
    total_wq = sum(c[0] for c in cands)
    print("relief candidates: %d elements, %d quads in the model files, ~%d baked quads"
          " (~%.1f MB at 168 bytes a quad)%s" % (
              len(cands), total_q, total_wq, total_wq * 168 / 1048576.0,
              "" if variants else "; unweighted (pass --variants blocks.csv)"))
    print("\nby model (baked quads saved = quads x variants):")
    for erel, (n, q, w, wq, blocks) in sorted(per_model.items(),
                                             key=lambda kv: -kv[1][3])[:args.top]:
        print("  %9d  %4d elements %5d quads x %7.0f variants  %s  [%s]" % (
            wq, n, q, w, erel.replace(os.sep, "/"), ", ".join(blocks)))
    print("\ntop elements:")
    for wq, q, w, erel, i, detail, blocks in cands[:args.top]:
        print("  %9d  %d quads x %6.0f  %s #%d  %s" % (
            wq, q, w, erel.replace(os.sep, "/"), i, detail))
    if args.csv:
        import csv
        with open(args.csv, "w", newline="", encoding="utf-8") as fh:
            wr = csv.writer(fh)
            wr.writerow(["bakedQuadsSaved", "quads", "variants", "model", "element", "detail",
                         "blocks"])
            for wq, q, w, erel, i, detail, blocks in cands:
                wr.writerow([int(round(wq)), q, round(w, 2), erel.replace(os.sep, "/"), i,
                             detail, " ".join(blocks)])
    return 0


def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--top", type=int, default=30)
    ap.add_argument("--module", help="limit to one module folder (or 'core')")
    ap.add_argument("--relief", action="store_true",
                    help="list fine relief that could be painted into the texture instead")
    ap.add_argument("--depth", type=float, default=1.0,
                    help="--relief: deepest detail listed, in pixels (default 1.0)")
    ap.add_argument("--max-area", type=float, default=64.0,
                    help="--relief: largest footprint listed, in square pixels (default 64)")
    ap.add_argument("--variants", help="--relief: a /csm memstats dump blocks.csv, to weight "
                                       "each model by the variants that bake it")
    ap.add_argument("--csv", help="--relief: also write every candidate to this CSV")
    args = ap.parse_args(argv)
    roots = {"core": md._TREES[0]}
    for t in md._TREES[1:]:
        roots[t.split(os.sep)[-6]] = t
    if args.relief:
        return relief_report(args, roots)
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
