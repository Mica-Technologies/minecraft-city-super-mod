"""Checks that blockstates forge_multipart converted draw every state as their Forge form did.

For each blockstate named, the Forge-format version at a git revision (the one before the
conversion) is resolved state by state the way Forge resolves it (defaults, then each property's
part, or the fully named variant), and the multipart version in the working tree is resolved the
way vanilla does (every rule whose ``when`` matches). Each state must match exactly one rule, and
that rule must name the same model, drawn with the same textures (resolved through every parent,
so a child model's own textures count), turned the same way; the item likewise. Every state must also
have had the same particle texture, since a multipart model's is its first part's. Anything else
is printed and the script exits 1.

Usage:
    py verify_multipart_conversion.py <rev> <blockstate path> [...]
    py verify_multipart_conversion.py HEAD~1 modules/furnishings/src/main/resources/assets/csm/blockstates/wall_tv_black.json
"""
import glob
import json
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forge_multipart as FM  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
TREES = [os.path.join(REPO, "src", "main", "resources", "assets", "csm")] + sorted(
    glob.glob(os.path.join(REPO, "modules", "*", "src", "main", "resources", "assets", "csm")))


def model_file(name):
    """``csm:block/a/b`` or a blockstate's ``csm:a/b`` as a file in some module tree."""
    path = name.split(":", 1)[1]
    if not path.startswith("block/"):
        path = "block/" + path
    for tree in TREES:
        p = os.path.join(tree, "models", path + ".json")
        if os.path.exists(p):
            return p
    return None


def textures(name, over=None):
    """The model's textures through its parents, then ``over``; ``#refs`` left as written."""
    chain = []
    n = name
    while n:
        p = model_file(n)
        if p is None:
            break
        data = json.load(open(p, encoding="utf-8"))
        chain.append(data)
        n = data.get("parent")
        if n and not n.startswith("csm:"):
            break
    tex = {}
    for data in reversed(chain):
        tex.update(data.get("textures", {}))
    tex.update(over or {})
    return tex


def elements_owner(name):
    """The model in the chain whose elements are drawn (the first that has any)."""
    n = name
    while n:
        p = model_file(n)
        if p is None:
            return None
        data = json.load(open(p, encoding="utf-8"))
        if "elements" in data:
            return os.path.normcase(p)
        n = data.get("parent")
    return None


def drawn(model, over, x, y, uvlock):
    return (elements_owner(model), tuple(sorted(textures(model, over).items())), x or 0, y or 0,
            bool(uvlock))


def matches(when, combo):
    if "OR" in when:
        return any(matches(w, combo) for w in when["OR"])
    return all(combo.get(k) in str(v).split("|") for k, v in when.items())


def check(rev, path):
    rel = os.path.relpath(os.path.abspath(path), REPO).replace(os.sep, "/")
    old = json.loads(subprocess.check_output(["git", "show", "%s:%s" % (rev, rel)], cwd=REPO))
    new = json.load(open(path, encoding="utf-8"))
    bad = []
    props, table = FM.resolve(old)
    # a multipart model's particle is its first part's: every state must have had the same one
    particles = {dict(drawn(v["model"], v.get("textures"), 0, 0, False)[1]).get("particle")
                 for v in table.values()}
    if len(particles) > 1:
        bad.append("%s: the states' particles differ: %s" % (rel, sorted(map(str, particles))))
    for key, v in table.items():
        combo = dict(key)
        hits = [r["apply"] for r in new["multipart"] if matches(r.get("when", {}), combo)]
        if len(hits) != 1:
            bad.append("%s %s: %d rules match" % (rel, key, len(hits)))
            continue
        a = hits[0]
        want = drawn(v["model"], v.get("textures"), v.get("x"), v.get("y"), v.get("uvlock"))
        got = drawn(a["model"], None, a.get("x"), a.get("y"), a.get("uvlock"))
        if want != got:
            bad.append("%s %s:\n  was %s\n  now %s" % (rel, key, want, got))
    inv = FM.inventory(old)
    if inv is not None:
        a = new.get("variants", {}).get("inventory")
        a = a[0] if isinstance(a, list) else a
        want = drawn(inv["model"], inv.get("textures"), inv.get("x"), inv.get("y"),
                     inv.get("uvlock"))
        got = drawn(a["model"], None, a.get("x"), a.get("y"), a.get("uvlock")) if a else None
        if want != got:
            bad.append("%s inventory:\n  was %s\n  now %s" % (rel, want, got))
    return len(table), bad


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    rev, paths = argv[0], argv[1:]
    if paths == ["--changed"]:
        # every blockstate changed since rev that was Forge-format then and is multipart now
        names = subprocess.check_output(["git", "diff", "--name-only", rev, "--"], cwd=REPO,
                                        text=True, stderr=subprocess.DEVNULL).split()
        paths = []
        for rel in names:
            p = os.path.join(REPO, rel)
            if "/blockstates/" not in rel or not os.path.exists(p):
                continue
            try:
                was = json.loads(subprocess.check_output(["git", "show", "%s:%s" % (rev, rel)],
                                                         cwd=REPO, stderr=subprocess.DEVNULL))
            except subprocess.CalledProcessError:
                continue
            if was.get("forge_marker") and "multipart" in json.load(open(p, encoding="utf-8")):
                paths.append(p)
    states = 0
    bad = []
    for p in paths:
        n, b = check(rev, p)
        states += n
        bad += b
    if bad:
        print("\n".join(bad))
        return 1
    print("%d blockstates, %d states: every one drawn as at %s" % (len(paths), states, rev))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
