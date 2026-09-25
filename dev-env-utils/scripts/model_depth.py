"""Separates the coplanar faces in generated JSON block models, which z-fight in game.

A generated piece is built of boxes, and two of them often put a face on the same plane: a detail
laid flush on a body (a clock face, a door window, a checkout's end panel over its cabinet), or a
round part's square and the same square turned 45 degrees sharing a top. Where the two faces paint
the same pixels it does not matter; where they paint different ones the depth buffer cannot tell
which is in front, and the surface flickers between them as the camera moves.

``separate`` is run by the JSON-model generators at the end of ``generate``, over the files each
has just written. For every blockstate it wrote, it takes each set of models the blockstate can
draw at once (every combination of the properties its multipart rules name, facing north), finds
faces that face the same way on planes closer than ``GAP``, samples both faces' textures where
they overlap, and where both are opaque and differ, moves one of them -- the smaller, the detail --
along its normal until it stands ``GAP`` clear of every face it overlaps, by growing or shrinking
that face's box, so nothing else moves. Only models the generator itself wrote are changed, so a
generator never rewrites another's files, and the pass is deterministic, so ``--check`` still
compares like with like.

``GAP`` is ``SignFaceDepthTest``'s rule: 0.2 of a pixel, which a 24-bit depth buffer behind
Minecraft's near plane holds apart to about 100 blocks.

After separating, ``separate`` also runs ``prune``: every face that the model's own opaque boxes
cover completely is removed, in every texture set the model is drawn with (child models and Forge
blockstate textures included, so a box that turns see-through under one retexture never hides
anything). A hidden face is never seen but is still baked into every chunk mesh the block is in.
``model_depth.py <module> --apply --prune`` does both, once, for hand-made models, deleting only
the removed entries from the file's text.
"""
import glob
import json
import math
import os
from itertools import combinations

from PIL import Image

GAP = 0.2
_REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
_TREES = [os.path.join(_REPO, "src", "main", "resources", "assets", "csm")] + sorted(
    glob.glob(os.path.join(_REPO, "modules", "*", "src", "main", "resources", "assets", "csm")))

# per face: its axis, whether it is the box's max side, and which box corners it spans
_FACES = {
    "down": (1, False), "up": (1, True), "north": (2, False), "south": (2, True),
    "west": (0, False), "east": (0, True),
}
# per face: the axis u runs along and whether it runs from the box's min end (+1) or max (-1),
# then the same for v -- Minecraft's own layout, which the default UVs below follow
_UVMAP = {"up": (0, 1, 2, 1), "down": (0, 1, 2, -1), "north": (0, -1, 1, -1),
          "south": (0, 1, 1, -1), "west": (2, 1, 1, -1), "east": (2, -1, 1, -1)}


def _default_uv(fn, f, t):
    return {"down": [f[0], 16 - t[2], t[0], 16 - f[2]], "up": [f[0], f[2], t[0], t[2]],
            "north": [16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]],
            "south": [f[0], 16 - t[1], t[0], 16 - f[1]],
            "west": [f[2], 16 - t[1], t[2], 16 - f[1]],
            "east": [16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]]}[fn]


def _rot(pt, r, sign=1):
    if not r or not r.get("angle"):
        return tuple(pt)
    a = math.radians(r["angle"] * sign)
    o = r["origin"]
    x, y, z = (pt[i] - o[i] for i in range(3))
    c, s = math.cos(a), math.sin(a)
    if r["axis"] == "x":
        y, z = y * c - z * s, y * s + z * c
    elif r["axis"] == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + o[0], y + o[1], z + o[2])


def _corners(fn, f, t):
    axis, top = _FACES[fn]
    c = t[axis] if top else f[axis]
    a, b = [k for k in range(3) if k != axis]
    out = []
    for ua, ub in ((f[a], f[b]), (t[a], f[b]), (t[a], t[b]), (f[a], t[b])):
        p = [0, 0, 0]
        p[axis], p[a], p[b] = c, ua, ub
        out.append(tuple(p))
    return out


class _Store:
    """The generator's output root first, then the repository's asset trees."""

    def __init__(self, root, overlay=None):
        self.root = root
        self.overlay = overlay or {}
        self.models = {}
        self.images = {}

    def find(self, rel):
        rel = os.path.normpath(rel)
        if rel in self.overlay:
            return self.overlay[rel]
        for base in [self.root] + _TREES:
            p = os.path.join(base, rel)
            if os.path.exists(p):
                return p
        return None

    def model_rel(self, name):
        name = name.split(":", 1)[-1]
        if not name.startswith("block/"):
            name = "block/" + name
        return os.path.join("models", *("%s.json" % name).split("/"))

    def model(self, name):
        rel = self.model_rel(name)
        if rel not in self.models:
            p = self.find(rel)
            self.models[rel] = json.load(open(p, encoding="utf-8")) if p else None
        return rel, self.models[rel]

    def chain(self, name, depth=0):
        """(model rel owning the elements, elements, merged textures)"""
        rel, d = self.model(name)
        if d is None or depth > 10:
            return None, [], {}
        if "parent" in d and "elements" not in d:
            erel, els, tex = self.chain(d["parent"], depth + 1)
        elif "parent" in d:
            _, _, tex = self.chain(d["parent"], depth + 1)
            erel, els = rel, d["elements"]
        else:
            erel, els, tex = rel, d.get("elements", []), {}
        tex = dict(tex)
        tex.update(d.get("textures", {}))
        return erel, els, tex

    def image(self, ref):
        if ref not in self.images:
            im = None
            if ref and ref.startswith("csm:"):
                p = self.find(os.path.join("textures", *(ref[4:] + ".png").split("/")))
                if p:
                    im = Image.open(p).convert("RGBA")
                    w, h = im.size
                    if h > w:
                        im = im.crop((0, 0, w, w))
            self.images[ref] = im
        return self.images[ref]


def _resolve(tex, ref):
    for _ in range(10):
        if not ref or not ref.startswith("#"):
            break
        ref = tex.get(ref[1:])
    return ref


class _Face:
    def __init__(self, store, rel, idx, el, fn, tex):
        self.store, self.rel, self.idx, self.el, self.fn, self.tex = store, rel, idx, el, fn, tex
        r = el.get("rotation")
        self.pts = [_rot(c, r) for c in _corners(fn, el["from"], el["to"])]
        axis, top = _FACES[fn]
        n = [0, 0, 0]
        n[axis] = 1 if top else -1
        self.n = _rot(n, dict(r, origin=[0, 0, 0]) if r else None)
        self.d = sum(a * b for a, b in zip(self.pts[0], self.n))
        self.drop = max(range(3), key=lambda k: abs(self.n[k]))
        self.ax = [k for k in range(3) if k != self.drop]

    def area(self):
        us = [p[self.ax[0]] for p in self.pts]
        vs = [p[self.ax[1]] for p in self.pts]
        return (max(us) - min(us)) * (max(vs) - min(vs))

    def inside(self, p):
        pts = [(c[self.ax[0]], c[self.ax[1]]) for c in self.pts]
        x, y = p[self.ax[0]], p[self.ax[1]]
        sign = 0
        for i in range(4):
            (x1, y1), (x2, y2) = pts[i], pts[(i + 1) % 4]
            cr = (x2 - x1) * (y - y1) - (y2 - y1) * (x - x1)
            if abs(cr) < 1e-6:
                continue
            s = 1 if cr > 0 else -1
            if sign and s != sign:
                return False
            sign = s
        return True

    def texel(self, p):
        face = self.el["faces"][self.fn]
        if face.get("rotation"):
            return None
        im = self.store.image(_resolve(self.tex, face.get("texture")))
        if im is None:
            return None
        f, t = self.el["from"], self.el["to"]
        lp = _rot(p, self.el.get("rotation"), -1)
        uv = face.get("uv") or _default_uv(self.fn, f, t)
        ua, ud, va, vd = _UVMAP[self.fn]
        res = []
        for ax, dr, (c0, c1) in ((ua, ud, (uv[0], uv[2])), (va, vd, (uv[1], uv[3]))):
            ext = t[ax] - f[ax]
            if ext == 0:
                return None
            frac = (lp[ax] - f[ax]) / ext if dr > 0 else (t[ax] - lp[ax]) / ext
            res.append(c0 + (c1 - c0) * frac)
        w, h = im.size
        x = min(w - 1, max(0, int(res[0] / 16 * w)))
        y = min(h - 1, max(0, int(res[1] / 16 * h)))
        px = im.getpixel((x, y))
        return "clear" if px[3] < 8 else px[:3]


def _clash(a, b):
    """Whether two same-facing faces within GAP show different opaque pixels where they meet."""
    def box(q):
        us = [p[q.ax[0]] for p in q.pts]
        vs = [p[q.ax[1]] for p in q.pts]
        return min(us), max(us), min(vs), max(vs)
    ba, bb = box(a), box(b)
    u0, u1 = max(ba[0], bb[0]), min(ba[1], bb[1])
    v0, v1 = max(ba[2], bb[2]), min(ba[3], bb[3])
    if u1 - u0 < 1e-3 or v1 - v0 < 1e-3:
        return False
    step = 0.25
    u = u0 + step / 2
    while u < u1:
        v = v0 + step / 2
        while v < v1:
            p = [0.0, 0.0, 0.0]
            p[a.ax[0]], p[a.ax[1]] = u, v
            p[a.drop] = (a.d - sum(p[k] * a.n[k] for k in a.ax)) / a.n[a.drop]
            if a.inside(p) and b.inside(p):
                ca, cb = a.texel(p), b.texel(p)
                if ca is None or cb is None:
                    return True
                if (ca != "clear" and cb != "clear"
                        and sum(abs(x - y) for x, y in zip(ca, cb)) > 24):
                    return True
            v += step
        u += step
    return False


def _matches(w, st):
    if "OR" in w:
        return any(_matches(x, st) for x in w["OR"])
    return all(str(st.get(k)) in str(v).split("|") for k, v in w.items())


def _model_sets(state):
    """Each set of models a blockstate draws at once, facing north, as (name, textures) pairs;
    the textures are what a Forge blockstate lays over the model's own."""
    def names(apply):
        return [(a["model"], {}) for a in (apply if isinstance(apply, list) else [apply])
                if "model" in a and not a.get("x") and not a.get("y")]
    if "multipart" not in state:
        dflt = state.get("defaults", {})
        tex = dflt.get("textures", {})
        out = []
        if dflt.get("model"):
            out.append([(dflt["model"], tex)])
        for v in state.get("variants", {}).values():
            for vv in (v if isinstance(v, list) else [v]):
                if not isinstance(vv, dict):
                    continue
                if "model" in vv and not vv.get("y"):
                    out.append([(vv["model"], dict(tex, **vv.get("textures", {})))])
                elif "model" not in vv:
                    for inner in vv.values():
                        if isinstance(inner, dict) and inner.get("model") and not inner.get("y"):
                            out.append([(inner["model"],
                                         dict(tex, **inner.get("textures", {})))])
        return out
    props = {}

    def collect(w):
        for x in w.get("OR", [w]) if "OR" in w else [w]:
            for k, v in x.items():
                props.setdefault(k, set()).update(str(v).split("|"))
    for rule in state["multipart"]:
        collect(rule.get("when", {}))
    props["facing"] = {"north"}
    keys = sorted(props)
    out = set()

    def rec(i, st):
        if i == len(keys):
            ms = []
            for rule in state["multipart"]:
                if _matches(rule.get("when", {}), st):
                    ms += [n for n, _t in names(rule["apply"])]
            if ms:
                out.add(tuple(sorted(set(ms))))
            return
        for v in sorted(props[keys[i]]) + ["__other"]:
            st[keys[i]] = v
            rec(i + 1, st)
    rec(0, {})
    return [[(n, {}) for n in ms] for ms in sorted(out)]


def _grow(el, fn, by):
    """Moves one face of a box out along its normal by ``by`` pixels."""
    axis, top = _FACES[fn]
    if top:
        el["to"][axis] = round(el["to"][axis] + by, 4)
    else:
        el["from"][axis] = round(el["from"][axis] - by, 4)


def _place(q, faces):
    """How far to move face ``q`` along its normal to stand GAP clear of every face it overlaps
    with different pixels. It keeps the order it was drawn in -- out in front if it was level
    with or in front of the face it meets, back if it was a little behind -- unless that way is
    closed (past the model's limits, or more than its box is thick) or far longer than the
    other; None if neither way is open. Placing a face against all of them at once, rather than one clash at a time, is what
    lets a stack of faces fan out instead of trading places."""
    planes = [o.d for o in faces
              if (o.rel, o.idx) != (q.rel, q.idx)
              and sum(x * y for x, y in zip(o.n, q.n)) > 0.999 and _clash(q, o)]

    def clear(step):
        t = q.d
        for _ in range(len(planes) + 1):
            near = [p for p in planes if abs(t - p) < GAP - 1e-3]
            if not near:
                return t - q.d
            t = max(near) + GAP if step > 0 else min(near) - GAP
        return None
    axis = _FACES[q.fn][0]
    up = clear(1)
    if up is not None and not _fits(q, up):
        up = None
    down = clear(-1)
    if down is not None and q.el["to"][axis] - q.el["from"][axis] <= -down + 0.05:
        down = None
    # keep the order it was drawn in: a face already a little behind another stays behind it
    behind = any(q.d + 1e-6 < p < q.d + GAP for p in planes)
    first, second = (down, up) if behind else (up, down)
    if first is None or (second is not None and abs(first) > 2 * abs(second) + 0.1):
        first = second
    return None if first is None else round(first, 4)


def _fits(face, by):
    """Whether the face can move out ``by`` and its box stay inside the -16..32 a model's
    elements are clamped to."""
    axis, top = _FACES[face.fn]
    v = face.el["to"][axis] + by if top else face.el["from"][axis] - by
    return -16 <= v <= 32


def _movable(face):
    """Growing a box moves each face along its own normal whatever the box's rotation, since the
    rotation turns the face and its normal together -- unless the rotation rescales the box."""
    r = face.el.get("rotation")
    return not (r and r.get("rescale"))


HAND_MAX = 0.45
PRUNE = True  # hidden-face pruning in generators (turned on after the render diff passed)
GEN_MAX = 0.65


def _prism_caps(a, b):
    """Whether two faces are the end caps of slices of one round or cross-shaped solid: boxes
    with the same extent along the faces' axis, each turned only about that axis (or not at all).
    Their caps must share a plane, and separating them would stair-step the solid's end."""
    axis = _FACES[a.fn][0]
    if _FACES[b.fn][0] != axis:
        return False
    for f in (a, b):
        r = f.el.get("rotation")
        if r and r.get("angle") and "xyz".index(r["axis"]) != axis:
            return False
    return (abs(a.el["from"][axis] - b.el["from"][axis]) < 1e-3
            and abs(a.el["to"][axis] - b.el["to"][axis]) < 1e-3)


def _units(states):
    """Every model, and every pair of models, some blockstate draws at once. Checking these once
    each, rather than every combination of properties, keeps a 70,000-state blockstate cheap."""
    units = set()
    for state in states:
        for names in _model_sets(state):
            keyed = sorted({(n, tuple(sorted(o.items()))) for n, o in names})
            for k in keyed:
                units.add((k,))
            for pair in combinations(keyed, 2):
                units.add(pair)
    return sorted(units)


def _near_pairs(faces_a, faces_b=None):
    """Pairs of faces facing the same way on planes under GAP apart: within one list, or one
    from each of two lists. Faces are bucketed by normal and sorted by plane, so a model of
    thousands of faces is not compared all against all."""
    def key(f):
        return tuple(round(x, 3) for x in f.n)
    buckets = {}
    for side, faces in ((0, faces_a), (1, faces_b if faces_b is not None else [])):
        for f in faces:
            buckets.setdefault(key(f), []).append((f.d, side, f))
    for items in buckets.values():
        items.sort(key=lambda t: t[0])
        for i, (d1, s1, f1) in enumerate(items):
            for d2, s2, f2 in items[i + 1:]:
                if d2 - d1 >= GAP - 1e-3:
                    break
                if faces_b is not None and s1 == s2:
                    continue
                if (f1.rel, f1.idx) == (f2.rel, f2.idx):
                    continue
                yield f1, f2


def separate(root, written, dump, hand_made=False, overlay=None):
    """Separates clashing faces among the models under ``root`` that ``written`` names.

    :param root:    the generator's output root (the assets tree, or a temporary copy)
    :param written: the paths the generator wrote, relative to ``root``
    :param dump:    the generator's JSON writer, ``dump(path, data)``
    :param hand_made: the conservative rules for hand-made models: leave the end caps of a
                    sliced round shape alone (``_prism_caps``) and move no face more than
                    ``HAND_MAX`` in all -- fanning out a stack of caps would step a pole's end.
                    A generated model's faces move at most ``GEN_MAX`` in all; a clash that
                    needs more is left, since a bigger step shows more than the flicker
    :return: how many faces were moved
    """
    owned = {os.path.normpath(w) for w in written}
    store = _Store(root, overlay)
    states = [json.load(open(store.find(rel), encoding="utf-8"))
              for rel in sorted(w for w in owned if w.startswith("blockstates" + os.sep))]
    units = _units(states)
    changed = set()
    moved = 0
    dirty = None  # after the first round, only pairs with a model just moved need a look
    cap = HAND_MAX if hand_made else GEN_MAX
    total = {}  # how far each face has moved so far, over every round
    for _ in range(24):
        moves = {}
        touched = set()  # boxes this round already moves or measures against
        cache = {}

        def faces_of(key):
            if key not in cache:
                name, over = key
                erel, els, tex = store.chain(name)
                if over:
                    tex = dict(tex, **dict(over))
                cache[key] = [_Face(store, erel, i, el, fn, tex)
                              for i, el in enumerate(els) for fn in sorted(el.get("faces", {}))]
            return cache[key]
        for unit in units:
            if dirty is not None and not any(store.chain(k[0])[0] in dirty for k in unit):
                continue
            if len(unit) == 1:
                faces = faces_of(unit[0])
                pairs = _near_pairs(faces)
            else:
                fa, fb = faces_of(unit[0]), faces_of(unit[1])
                faces = fa + fb
                pairs = _near_pairs(fa, fb)
            for a, b in pairs:
                if (a.rel, a.idx) in touched or (b.rel, b.idx) in touched:
                    continue
                if hand_made and _prism_caps(a, b):
                    continue
                if not _clash(a, b):
                    continue
                # move the detail -- the smaller face, else the later box -- if this
                # generator wrote it: to the nearest place clear of every face it overlaps
                order = sorted((a, b), key=lambda q: (q.area(), -q.idx))
                for q in order:
                    if os.path.normpath(q.rel) not in owned or not _movable(q):
                        continue
                    by = _place(q, faces)
                    if by is not None and abs(total.get((q.rel, q.idx, q.fn), 0) + by) > cap:
                        by = None
                    if by is not None:
                        moves[(q.rel, q.idx, q.fn)] = by
                        touched.update({(a.rel, a.idx), (b.rel, b.idx)})
                        break
        if not moves:
            break
        dirty = {rel for rel, _i in touched}  # moved boxes and the ones they were measured against
        for (rel, idx, fn), by in sorted(moves.items()):
            total[(rel, idx, fn)] = total.get((rel, idx, fn), 0) + by
            el = store.models[rel]["elements"][idx]
            _grow(el, fn, by)
            changed.add(rel)
            moved += 1
    for rel in sorted(changed):
        dump(store.find(rel), store.models[rel])
    if PRUNE and not hand_made:
        prune(root, written, dump, overlay)
    return moved


def separate_dirs(dirs, written, dump):
    """``separate`` for the generators that write each kind of file to its own folder
    (``write_all(tex_dir, model_dir, state_dir, ...)``) and, under ``--check``, to temporary
    copies of those folders.

    :param dirs:    (folder written to, the real folder it stands for) pairs, block models,
                    blockstates and textures included, item folders marked by an "item" or
                    "items" in the real path
    :param written: the ``(kind, filename)`` pairs ``write_all`` returns
    :param dump:    the generator's JSON writer
    """
    def assets_of(path):
        parts = os.path.normpath(path).split(os.sep)
        for i in range(len(parts) - 1):
            if parts[i] == "assets" and parts[i + 1] == "csm":
                return os.sep.join(parts[:i + 2])
        raise ValueError(path)
    root = assets_of(dirs[0][1])

    def pick(kind):
        kind = kind.lower()
        want_item = kind.startswith("i") and not kind.startswith("int") or "item" in kind
        want_shared = kind == "shared"
        for actual, real in dirs:
            rel = os.path.relpath(real, root)
            is_item = rel.startswith(os.path.join("textures", "items")) or rel.startswith(
                os.path.join("models", "item"))
            is_shared = "shared_models" in rel.split(os.sep)
            if "state" in kind and rel == "blockstates":
                return actual, rel
            if "tex" in kind and rel.startswith("textures") and is_item == want_item:
                return actual, rel
            if (("model" in kind or want_shared) and rel.startswith("models")
                    and is_item == want_item and is_shared == want_shared):
                return actual, rel
        return None
    overlay, rels = {}, []
    for kind, name in written:
        hit = pick(kind)
        if hit is None:
            continue
        actual, rel_dir = hit
        rel = os.path.normpath(os.path.join(rel_dir, name))
        # only a file that is both where it was written and where it lives in this tree: a file
        # that lives in another tree (Core) is left alike in write and --check runs
        real = os.path.join(root, rel_dir, name)
        if os.path.exists(os.path.join(actual, name)) and os.path.exists(real):
            overlay[rel] = os.path.join(actual, name)
            rels.append(rel)
    return separate(root, rels, dump, overlay=overlay)


_ARRAY = None


def _num(v):
    return str(int(v)) if float(v).is_integer() else repr(round(float(v), 4))


def rewrite_numbers(path, data):
    """Writes a moved model back into a hand-made file by changing only the ``from`` and ``to``
    numbers that moved, leaving every other byte -- a Blockbench export's tabs and one-line
    arrays, its line endings -- as it was. Refuses (raises) unless the file's ``from``/``to``
    arrays pair up one-to-one with its elements, so it can never edit the wrong numbers."""
    import re
    global _ARRAY
    if _ARRAY is None:
        _ARRAY = re.compile(r'"(from|to)"(\s*:\s*)\[([^\]]*)\]')
    raw = open(path, encoding="utf-8", newline="").read()
    old = json.loads(raw)
    els_old, els_new = old.get("elements", []), data.get("elements", [])
    matches = list(_ARRAY.finditer(raw))
    if len(matches) != 2 * len(els_old) or len(els_old) != len(els_new):
        raise ValueError("%s: from/to arrays do not pair with elements" % path)
    out, pos = [], 0
    for i, m in enumerate(matches):
        el_old, el_new = els_old[i // 2], els_new[i // 2]
        key = m.group(1)
        vals = [float(x) for x in m.group(3).split(",")]
        if vals != [float(v) for v in el_old[key]] or ("from" if i % 2 == 0 else "to") != key:
            raise ValueError("%s: array %d is not element %d's %s" % (path, i, i // 2, key))
        out.append(raw[pos:m.start()])
        if el_new[key] == el_old[key]:
            out.append(m.group(0))
        else:
            parts = m.group(3).split(",")
            new = []
            for part, v_old, v_new in zip(parts, el_old[key], el_new[key]):
                if v_old == v_new:
                    new.append(part)
                else:
                    lead = part[:len(part) - len(part.lstrip())]
                    trail = part[len(part.rstrip()):]
                    new.append(lead + _num(v_new) + trail)
            out.append('"%s"%s[%s]' % (key, m.group(2), ",".join(new)))
        pos = m.end()
    out.append(raw[pos:])
    text = "".join(out)
    if json.loads(text) != data:
        raise ValueError("%s: rewrite does not reproduce the moved model" % path)
    with open(path, "w", encoding="utf-8", newline="") as fh:
        fh.write(text)


def main(argv):
    """``model_depth.py <module> [--apply]``: separates the hand-made models of one module (the
    models no generator writes), writing only the numbers that move. Without --apply it reports."""
    import argparse
    ap = argparse.ArgumentParser(description=main.__doc__)
    ap.add_argument("module", help="a module folder under modules/, or 'core'")
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--prune", action="store_true",
                    help="also remove the faces the models' own opaque boxes hide")
    ap.add_argument("--prune-only", action="store_true",
                    help="only remove hidden faces; move nothing (for models already separated)")
    ap.add_argument("--models", nargs="*", help="limit to these model paths (under the module's "
                                                "assets/csm); default: every model")
    args = ap.parse_args(argv)
    root = (os.path.join(_REPO, "src", "main", "resources", "assets", "csm") if args.module == "core"
            else os.path.join(_REPO, "modules", args.module, "src", "main", "resources", "assets",
                              "csm"))
    states = [os.path.relpath(p, root) for p in glob.glob(os.path.join(root, "blockstates", "*.json"))]
    models = args.models or [os.path.relpath(p, root) for p in glob.glob(
        os.path.join(root, "models", "**", "*.json"), recursive=True)]
    written = []

    def dump(path, data):
        written.append(path)
        if args.apply:
            rewrite_numbers(path, data)
    if not args.prune_only:
        n = separate(root, states + [os.path.normpath(m) for m in models], dump, hand_made=True)
        print("%s %d faces in %d models" % ("moved" if args.apply else "would move", n,
                                           len(written)))
    if args.prune or args.prune_only:
        pruned = []

        def pdump(path, data):
            pruned.append(path)
            if args.apply:
                rewrite_pruned(path, data)
        g = prune(root, [os.path.normpath(m) for m in models], pdump)
        print("%s %d hidden faces in %d models" % ("pruned" if args.apply else "would prune", g,
                                                  len(pruned)))
    return 0



# ------------------------------------------------------------------------------------------------
# Hidden faces
# ------------------------------------------------------------------------------------------------
_PRUNE_STEP = 0.25   # sampling grid over a face, in pixels
_PRUNE_EPS = 0.02    # how far in front of a face a sample is taken
_USERS = {}


def _inside_box(el, p):
    lp = _rot(p, el.get("rotation"), -1)
    f, t = el["from"], el["to"]
    return all(min(f[k], t[k]) + 1e-4 < lp[k] < max(f[k], t[k]) - 1e-4 for k in range(3))


def _opaque(store, el, tex):
    """Whether every face texture of a box is fully opaque over the area it samples."""
    for fn, face in el.get("faces", {}).items():
        im = store.image(_resolve(tex, face.get("texture")))
        if im is None:
            return False
        uv = face.get("uv") or _default_uv(fn, el["from"], el["to"])
        w, h = im.size
        x0, x1 = sorted((uv[0] / 16 * w, uv[2] / 16 * w))
        y0, y1 = sorted((uv[1] / 16 * h, uv[3] / 16 * h))
        box = (int(x0), int(y0), max(int(x0) + 1, int(round(x1))),
               max(int(y0) + 1, int(round(y1))))
        if im.crop(box).getchannel("A").getextrema()[0] < 250:
            return False
    return True


def texture_users(root):
    """{element model rel: [texture dicts it is drawn with]} over every model and blockstate in
    ``root`` and the asset trees: each child model that inherits the elements, and each Forge
    blockstate that lays textures over one. A face is only hidden if it is hidden in all of them."""
    key = os.path.normcase(os.path.abspath(root))
    if key in _USERS:
        return _USERS[key]
    store = _Store(root)
    users = {}

    def add(name, over):
        erel, els, tex = store.chain(name)
        if not els or erel is None:
            return
        t = dict(tex, **over) if over else tex
        users.setdefault(os.path.normpath(erel), {})[tuple(sorted(t.items()))] = t
    for base in [root] + _TREES:
        for p in glob.glob(os.path.join(base, "models", "block", "**", "*.json"), recursive=True):
            rel = os.path.relpath(p, os.path.join(base, "models", "block")).replace(os.sep, "/")
            add("csm:" + rel[:-5], None)
        for p in glob.glob(os.path.join(base, "blockstates", "*.json")):
            try:
                state = json.load(open(p, encoding="utf-8"))
            except ValueError:
                continue
            if not isinstance(state, dict) or "multipart" in state:
                continue
            for names in _model_sets(state):
                for name, over in names:
                    if over:
                        add(name, over)
    _USERS[key] = {k: list(v.values()) for k, v in users.items()}
    return _USERS[key]


def hidden_in(store, rel, els, texsets):
    """Faces (element index, face) of one model's elements hidden by its own closed, opaque boxes
    under every texture set it is drawn with: every point just in front of the face lies inside
    such a box, so no line of sight reaches it without crossing an opaque face."""
    hidden = None
    for tex in texsets:
        # only a closed box hides what is inside it: a box's volume occludes nothing, its faces
        # do, so a box missing a face (an open basin, a crate with no top) hides nothing
        opaque = [i for i, el in enumerate(els)
                  if len(el.get("faces", {})) == 6 and _opaque(store, el, tex)]
        found = set()
        if opaque:
            for i, el in enumerate(els):
                for fn in el.get("faces", {}):
                    q = _Face(store, rel, i, el, fn, tex)
                    us = [p[q.ax[0]] for p in q.pts]
                    vs = [p[q.ax[1]] for p in q.pts]
                    u0, u1, v0, v1 = min(us), max(us), min(vs), max(vs)
                    if u1 - u0 < 1e-3 or v1 - v0 < 1e-3:
                        continue
                    nu = max(1, int((u1 - u0) / _PRUNE_STEP))
                    nv = max(1, int((v1 - v0) / _PRUNE_STEP))
                    covered, seen = True, False
                    for a in range(nu + 1):
                        u = u0 + (u1 - u0) * a / nu
                        for b in range(nv + 1):
                            v = v0 + (v1 - v0) * b / nv
                            # pull edge samples a hair inside so the face's own rim counts
                            uu = min(max(u, u0 + 1e-3), u1 - 1e-3)
                            vv = min(max(v, v0 + 1e-3), v1 - 1e-3)
                            p = [0.0, 0.0, 0.0]
                            p[q.ax[0]], p[q.ax[1]] = uu, vv
                            p[q.drop] = (q.d - sum(p[k] * q.n[k] for k in q.ax)) / q.n[q.drop]
                            if not q.inside(p):
                                continue
                            seen = True
                            front = [p[k] + q.n[k] * _PRUNE_EPS for k in range(3)]
                            if not any(j != i and _inside_box(els[j], front) for j in opaque):
                                covered = False
                                break
                        if not covered:
                            break
                    if covered and seen:
                        found.add((i, fn))
        hidden = found if hidden is None else hidden & found
        if not hidden:
            return set()
    return hidden or set()


def prune(root, written, dump, overlay=None):
    """Removes, from the models ``written`` names that own elements, every face their own opaque
    boxes hide in every texture set they are drawn with, and any box left with no faces (Minecraft
    rejects a box without one). Returns how many faces went."""
    owned = {os.path.normpath(w) for w in written}
    store = _Store(root, overlay)
    users = texture_users(root)
    gone = 0
    for rel in sorted(r for r in owned if r.startswith(os.path.join("models", "block")) and r.endswith(".json")):
        path = store.find(rel)
        if not path:
            continue
        data = json.load(open(path, encoding="utf-8"))
        els = data.get("elements")
        if not els:
            continue
        texsets = users.get(rel)
        if not texsets:
            continue
        hid = hidden_in(store, rel, els, texsets)
        if not hid:
            continue
        for i, fn in hid:
            del els[i]["faces"][fn]
        data["elements"] = [el for el in els if el.get("faces")]
        dump(path, data)
        gone += len(hid)
    return gone


def _spans(text):
    """Parses JSON text into a tree of (kind, start, end, children): for an object, children are
    [(key, key_start, value_node)]; for an array, [value_node]. Offsets into ``text``."""
    import re
    ws = re.compile(r"\s*")
    strre = re.compile(r'"(?:[^"\\]|\\.)*"')
    numre = re.compile(r"-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?")

    def skip(i):
        return ws.match(text, i).end()

    def value(i):
        i = skip(i)
        c = text[i]
        if c == "{":
            start, kids = i, []
            i = skip(i + 1)
            if text[i] == "}":
                return ("obj", start, i + 1, kids), i + 1
            while True:
                i = skip(i)
                m = strre.match(text, i)
                key = json.loads(m.group(0))
                ks = i
                i = skip(m.end())
                assert text[i] == ":"
                node, i = value(i + 1)
                kids.append((key, ks, node))
                i = skip(i)
                if text[i] == ",":
                    i += 1
                    continue
                assert text[i] == "}"
                return ("obj", start, i + 1, kids), i + 1
        if c == "[":
            start, kids = i, []
            i = skip(i + 1)
            if text[i] == "]":
                return ("arr", start, i + 1, kids), i + 1
            while True:
                node, i = value(i)
                kids.append(node)
                i = skip(i)
                if text[i] == ",":
                    i += 1
                    continue
                assert text[i] == "]"
                return ("arr", start, i + 1, kids), i + 1
        if c == '"':
            m = strre.match(text, i)
            return ("str", i, m.end(), None), m.end()
        for lit in ("true", "false", "null"):
            if text.startswith(lit, i):
                return ("lit", i, i + len(lit), None), i + len(lit)
        m = numre.match(text, i)
        return ("num", i, m.end(), None), m.end()
    node, _ = value(0)
    return node


def rewrite_pruned(path, data):
    """Writes a pruned model back into a hand-made file by deleting only the removed face entries
    and emptied boxes from the text, leaving every other byte as it was; refuses unless the result
    parses to exactly ``data``."""
    raw = open(path, encoding="utf-8", newline="").read()
    old = json.loads(raw)
    root = _spans(raw)
    els_node = next(n for k, _ks, n in root[3] if k == "elements")
    new_els = data["elements"]
    # map old elements to new: an old element survives if some new element equals it minus faces
    cuts = []  # (start, end) text ranges to delete
    ni = 0
    for oi, (el_old, el_node) in enumerate(zip(old["elements"], els_node[3])):
        keep = ni < len(new_els) and all(new_els[ni].get(k) == el_old.get(k)
                                         for k in el_old if k != "faces")
        if keep and set(new_els[ni]["faces"]) <= set(el_old["faces"]) and new_els[ni]["faces"]:
            gone = set(el_old["faces"]) - set(new_els[ni]["faces"])
            if gone:
                faces_node = next(n for k, _ks, n in el_node[3] if k == "faces")
                entries = [(k, ks, n) for k, ks, n in faces_node[3]]
                cuts += _entry_cuts(raw, [(ks, n[2]) for k, ks, n in entries],
                                    [k in gone for k, ks, n in entries])
            ni += 1
        else:
            cuts.append(("el", oi))
    el_spans = [(n[1], n[2]) for n in els_node[3]]
    drop = [False] * len(el_spans)
    face_cuts = [c for c in cuts if c[0] != "el"]
    for c in cuts:
        if c[0] == "el":
            drop[c[1]] = True
    cuts = face_cuts + _entry_cuts(raw, el_spans, drop)
    out, pos = [], 0
    for s, e in sorted(cuts):
        out.append(raw[pos:s])
        pos = e
    out.append(raw[pos:])
    text = "".join(out)
    if json.loads(text) != data:
        raise ValueError("%s: pruned rewrite does not reproduce the model" % path)
    with open(path, "w", encoding="utf-8", newline="") as fh:
        fh.write(text)


def _entry_cuts(raw, spans, drop):
    """Text ranges to delete to remove the flagged entries of one object or array, whose entries
    are ``spans`` [(start, end)] in order: each dropped entry goes with the comma that separates it
    from a kept neighbour and the whitespace in between."""
    cuts = []
    n = len(spans)
    for i in range(n):
        if not drop[i]:
            continue
        s, e = spans[i]
        # prefer eating the comma after it (up to the next entry); for the last kept-less tail,
        # eat the comma before it instead
        nxt = next((j for j in range(i + 1, n) if not drop[j]), None)
        prv = next((j for j in range(i - 1, -1, -1) if not drop[j]), None)
        if nxt is not None:
            cuts.append((s, spans[i + 1][0]) if i + 1 < n else (s, e))
        elif prv is not None:
            # delete from the end of the previous entry (kept or dropped) through this one
            cuts.append((spans[i - 1][1], e))
        else:
            cuts.append((s, e))
    # merge overlapping cuts
    cuts.sort()
    merged = []
    for s, e in cuts:
        if merged and s <= merged[-1][1]:
            merged[-1] = (merged[-1][0], max(e, merged[-1][1]))
        else:
            merged.append((s, e))
    return merged


if __name__ == "__main__":
    import sys
    sys.exit(main(sys.argv[1:]))
