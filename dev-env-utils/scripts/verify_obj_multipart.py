"""Checks that OBJ blockstates turned into multipart (``forge_multipart.convert_obj``) draw every
state, and the item, as their Forge form did at a git revision.

For each blockstate given (or every one changed since the revision, with ``--changed``) whose
current form is multipart and whose old form is a Forge blockstate of OBJ models, every state of
the old form is resolved the way Forge bakes it (the variant's model and each sub-model, the
sub-model's own ``transform`` rotation about the block's centre composed under the variant's
``x``/``y``, Forge's ``flip-v`` and retextures) and every state of the new one the way the
multipart rules and ``CsmObjModelLoader``'s location options say. Each part is then the set of
its triangles as drawn: corner positions, texture coordinates after flip-v, and the texture each
face is drawn with. The two must be the same set, state by state, files read at the revision for
the old form and from the tree for the new. The variant rotation is applied the same way to both
sides, so what is checked is everything the conversion changed: the rules, the options, the
mount clamps turned in their files and the double arms drawn from their single arm's cells. The
block's particle (a multipart model's is its first rule's part's last face) must be the one every
state of the Forge form had (its own model's last face).

    py -3 dev-env-utils/scripts/verify_obj_multipart.py <rev> --changed
    py -3 dev-env-utils/scripts/verify_obj_multipart.py <rev> <blockstate name> [...]
"""
import argparse
import collections
import itertools
import json
import math
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

REPO = layout.REPO_ROOT


def git_show(rev, rel):
    try:
        return subprocess.run(["git", "-C", REPO, "show", "%s:%s" % (rev, rel)], capture_output=True,
                              check=True).stdout.decode("utf-8")
    except subprocess.CalledProcessError:
        return None


def model_rel(ref):
    """``csm:a/b.obj`` -> the repository-relative path of the file, whichever tree holds it."""
    ns, path = ref.split(":", 1)
    assert ns == "csm", ref
    if not path.startswith("block/"):
        path = "block/" + path
    return path


class Files:
    """OBJ and MTL text, at a revision or from the tree, by model path under ``models/``."""

    def __init__(self, rev):
        self.rev = rev
        self.cache = {}

    def read(self, rel_under_assets):
        if rel_under_assets in self.cache:
            return self.cache[rel_under_assets]
        text = None
        for _module, root in layout.asset_roots():
            full = os.path.join(root, rel_under_assets.replace("/", os.sep))
            rel = os.path.relpath(full, REPO).replace(os.sep, "/")
            if self.rev:
                text = git_show(self.rev, rel)
            elif os.path.isfile(full):
                text = open(full, encoding="utf-8").read()
            if text is not None:
                break
        self.cache[rel_under_assets] = text
        return text


def parse_obj(files, model_path):
    """[(material, [(pos, uv)...])], {material: texture}"""
    text = files.read("models/" + model_path)
    if text is None:
        raise FileNotFoundError(model_path)
    v, vt, faces, mtl = [], [], [], {}
    mat = None
    for line in text.splitlines():
        t = line.split()
        if not t:
            continue
        if t[0] == "v":
            v.append(tuple(float(c) for c in t[1:4]))
        elif t[0] == "vt":
            vt.append(tuple(float(c) for c in t[1:3]))
        elif t[0] == "usemtl":
            mat = t[1]
        elif t[0] == "mtllib":
            lib = files.read("models/" + os.path.dirname(model_path) + "/" + t[1]) or ""
            cur = None
            for ml in lib.splitlines():
                m = ml.split()
                if m and m[0] == "newmtl":
                    cur = m[1]
                elif m and m[0] == "map_Kd":
                    mtl[cur] = m[1]
        elif t[0] == "f":
            corners = []
            for c in t[1:]:
                ix = c.split("/")
                corners.append((v[int(ix[0]) - 1], vt[int(ix[1]) - 1]))
            faces.append((mat, corners))
    return faces, mtl


def rot(axis, deg):
    """Right-handed rotation matrix about x, y or z."""
    c = round(math.cos(math.radians(deg)), 12)
    s = round(math.sin(math.radians(deg)), 12)
    if axis == "x":
        return ((1, 0, 0), (0, c, -s), (0, s, c))
    if axis == "y":
        return ((c, 0, s), (0, 1, 0), (-s, 0, c))
    return ((c, -s, 0), (s, c, 0), (0, 0, 1))


def mul(a, b):
    return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)) for i in range(3))


IDENT = ((1, 0, 0), (0, 1, 0), (0, 0, 1))


def variant_matrix(x, y):
    """A blockstate x/y; the same function serves both forms, so only its consistency matters."""
    return mul(rot("y", -y), rot("x", -x))


def forge_sub_matrix(rotation):
    """A Forge ``transform`` rotation list ([{"x": a}, {"y": b}, ...]): the quaternions
    multiplied in order, so the first is applied last."""
    m = IDENT
    for r in rotation:
        (axis, deg), = r.items()
        m = mul(m, rot(axis, deg))
    return m


def drawn(files, part):
    """The set of triangles a part draws: (texture, ((x, y, z, u, v) x 3)) in a canonical turn."""
    model, textures, flip, matrix = part
    faces, mtl = parse_obj(files, model_rel(model))
    out = collections.Counter()
    for mat, corners in faces:
        tex = mtl.get(mat, "?")
        if tex.startswith("#"):
            tex = textures.get(tex, textures.get("#" + mat, tex))
        elif ("#" + mat) in textures:
            tex = textures["#" + mat]
        pts = []
        for (px, py, pz), (u, vv) in corners:
            p = (px - 0.5, py - 0.5, pz - 0.5)
            q = tuple(sum(matrix[i][k] * p[k] for k in range(3)) + 0.5 for i in range(3))
            pts.append(tuple(round(c, 4) + 0.0 for c in q) + (round(u, 4), round(1 - vv if flip else vv, 4)))
        i = pts.index(min(pts))
        out[(tex, tuple(pts[i:] + pts[:i]))] += 1
    return frozenset(out.items())


TOLERANCE = 2e-4


def unmatched(only_a, only_b):
    """How many triangles of the two leftovers have no partner within ``TOLERANCE`` in every
    coordinate: rounding to 4 places splits a value lying on a rounding boundary, so the exact
    comparison leaves a few equal triangles on either side."""
    rest = list(only_b.elements())
    missing = 0
    for tex, tri in only_a.elements():
        for i, (tex_b, tri_b) in enumerate(rest):
            if tex_b == tex and any(
                    all(abs(p - q) <= TOLERANCE for c, d in zip(tri, tri_b[k:] + tri_b[:k])
                        for p, q in zip(c, d)) for k in range(len(tri_b))):
                del rest[i]
                break
        else:
            missing += 1
    return missing + len(rest)


def particle(files, part):
    """The texture of a part's last face, which is what Forge's OBJ bake reports as its particle
    once built (and Core's from the start); a multipart model's particle is its first rule's."""
    model, textures = part[0], part[1]
    faces, mtl = parse_obj(files, model_rel(model))
    if not faces:
        return None
    mat = faces[-1][0]
    tex = mtl.get(mat, "?")
    if tex.startswith("#"):
        return textures.get(tex, textures.get("#" + mat, tex))
    return textures.get("#" + mat, tex)


def one(v):
    return v[0] if isinstance(v, list) else v


def merge(base, part):
    out = dict(base)
    for k, v in part.items():
        if k in ("textures", "custom", "submodel"):
            out[k] = dict(out.get(k, {}), **v)
        else:
            out[k] = v
    return out


def forge_states(state):
    """{combo: [part]} for a Forge blockstate, and the item's part."""
    defaults = state.get("defaults", {})
    props = {k: list(v) for k, v in state["variants"].items() if k not in ("inventory", "normal")}
    names = sorted(props)
    out = {}
    for combo in itertools.product(*(props[n] for n in names)):
        v = dict(defaults)
        for n, val in zip(names, combo):
            v = merge(v, one(state["variants"][n][val]))
        vm = variant_matrix(v.get("x", 0), v.get("y", 0))
        parts = [(v["model"], v.get("textures", {}), bool(v.get("custom", {}).get("flip-v")), vm)]
        for sub in v.get("submodel", {}).values():
            sm = forge_sub_matrix(sub.get("transform", {}).get("rotation", []))
            parts.append((sub["model"], sub.get("textures", {}), bool(sub.get("custom", {}).get("flip-v")),
                          mul(vm, sm)))
        out[tuple(zip(names, combo))] = parts
    inv = state["variants"].get("inventory")
    item = None
    if inv is not None:
        v = merge(defaults, one(inv))
        item = (v["model"], v.get("textures", {}), bool(v.get("custom", {}).get("flip-v")), v.get("transform"))
    return props, out, item


def option_part(loc, x, y):
    head, _, opts = loc.partition("?")
    textures, flip, transform = {}, False, None
    for o in opts.split("&") if opts else []:
        k, eq, val = o.partition("=")
        if k == "flip-v":
            flip = (val or "true") == "true"
        elif k == "transform":
            transform = val
        elif k in ("ambient", "gui3d"):
            continue
        else:
            textures["#" + k] = val
    return head, textures, flip, variant_matrix(x, y), transform


def multipart_states(state, props):
    names = sorted(props)
    out = {}
    for combo in itertools.product(*(props[n] for n in names)):
        d = dict(zip(names, combo))
        parts = []
        for r in state["multipart"]:
            w = r.get("when", {})
            if all(d[n] in str(val).split("|") for n, val in w.items()):
                a = r["apply"]
                h, t, f, m, _tr = option_part(a["model"], a.get("x", 0), a.get("y", 0))
                parts.append((h, t, f, m))
        out[tuple(zip(names, combo))] = parts
    inv = state.get("variants", {}).get("inventory")
    item = None
    if inv is not None:
        h, t, f, _m, tr = option_part(inv["model"], 0, 0)
        item = (h, t, f, tr)
    return out, item


def key(files, part, memo):
    model, textures, flip, matrix = part
    k = (model, tuple(sorted((a, b) for a, b in textures.items() if a != "particle")), flip, matrix)
    if k not in memo:
        memo[k] = drawn(files, (model, dict(k[1]), flip, matrix))
    return memo[k]


def verify(rev, name, old_files, new_files, memo_old, memo_new):
    path = None
    for _m, root in layout.asset_roots():
        p = os.path.join(root, "blockstates", name + ".json")
        if os.path.isfile(p):
            path = p
    if path is None:
        return "missing"
    new = json.load(open(path, encoding="utf-8"))
    old_text = git_show(rev, os.path.relpath(path, REPO).replace(os.sep, "/"))
    if old_text is None or "multipart" not in new:
        return "skip"
    old = json.loads(old_text)
    if not old.get("forge_marker"):
        return "skip"
    props, old_states, old_item = forge_states(old)
    new_states, new_item = multipart_states(new, props)
    bad = 0
    for combo, parts in old_states.items():
        a = collections.Counter()
        for p in parts:
            for tri, n in key(old_files, p, memo_old):
                a[tri] += n
        b = collections.Counter()
        for p in new_states[combo]:
            for tri, n in key(new_files, p, memo_new):
                b[tri] += n
        left = unmatched(a - b, b - a)
        if left:
            bad += 1
            if bad <= 3:
                print("  %s %s: %d triangles differ" % (name, dict(combo), left))
    old_particles = {particle(old_files, parts[0]) for parts in old_states.values()}
    first = new["multipart"][0]["apply"]
    new_particle = particle(new_files, option_part(first["model"], 0, 0)[:2])
    if old_particles != {new_particle}:
        bad += 1
        print("  %s: particle %s, was %s" % (name, new_particle, sorted(old_particles)))
    if (old_item is None) != (new_item is None):
        bad += 1
        print("  %s: item variant present in one form only" % name)
    elif old_item is not None:
        oa = key(old_files, old_item[:3] + (IDENT,), memo_old)
        nb = key(new_files, new_item[:3] + (IDENT,), memo_new)
        if oa != nb or (old_item[3] or None) != (new_item[3] or None):
            bad += 1
            print("  %s: the item draws differently" % name)
    return "ok %d states" % len(old_states) if not bad else "FAIL %d states" % bad


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("rev")
    ap.add_argument("names", nargs="*")
    ap.add_argument("--changed", action="store_true")
    a = ap.parse_args(argv)
    names = list(a.names)
    if a.changed:
        out = subprocess.run(["git", "-C", REPO, "diff", "--name-only", a.rev, "--", "*blockstates/*.json"],
                             capture_output=True, text=True, check=True).stdout.split()
        names += [os.path.basename(p)[:-5] for p in out]
    old_files, new_files = Files(a.rev), Files(None)
    memo_old, memo_new = {}, {}
    failed = 0
    checked = 0
    for n in sorted(set(names)):
        r = verify(a.rev, n, old_files, new_files, memo_old, memo_new)
        if r.startswith("FAIL") or r == "missing":
            failed += 1
        if r != "skip":
            checked += 1
        print("%-48s %s" % (n, r))
    print("%d checked, %d failed" % (checked, failed))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
