#!/usr/bin/env python3
"""Recess the grey backing behind a road sign's face, so the two stop z-fighting at a distance.

The hand-built sign plates paint their art on a sliver a hundredth of a model unit in front of
the plate (the back-to-back convention needs the art there, see TRAFFIC_SIGNS.md), and the plate
drew its own front face as well. Two opaque faces 0.000625 blocks apart: a 24-bit depth buffer
with Minecraft's 0.05 near plane separates

    z * z / (0.05 * 2**24)   blocks at z blocks away,

which passes 0.000625 at about 23 blocks and is unreliable from half that -- so every such sign
flickered between its face and bare metal from a dozen blocks out (issue #212).

The plate's front face cannot simply go: most faces have rounded corners, and many do not fill
their plate, so the metal behind the art is part of how the sign looks. Instead the plate keeps
its sides and back, loses its front, and gets a BACKING element carrying that front face, 0.4
units further back (0.41 from the art, 0.026 blocks, good past 100 blocks -- where a sign is a
handful of pixels). From the front the sign is unchanged. The art does not move, so the 28.49
convention and SignShiftModelTest are untouched.

A second, smaller case rides along: a sign with no plate, whose post reaches forward to touch the
art and ends in a cap just behind it. See redundant_caps.

    python fix_sign_plate_backing.py            # list what would change
    python fix_sign_plate_backing.py --apply    # rewrite the models
    python fix_sign_plate_backing.py --check    # exit 1 if any model still has the defect

A new hand-built plate copied from an old revision brings the defect back; SignFaceDepthTest
fails the build on it, and --apply is the repair.
"""
import copy
import glob
import json
import os
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
TREES = [os.path.join(ROOT, "src", "main", "resources", "assets", "csm")] + sorted(
    glob.glob(os.path.join(ROOT, "modules", "*", "src", "main", "resources", "assets", "csm")))

SLIVER_MAX = 0.0101   # an art sliver is a hundredth of a unit thick
PLATE_MIN = 0.02      # anything thicker is a real plate
RECESS = 0.8          # the backing sits this far through the plate, front to back
MIN_GAP = 0.2         # SignFaceDepthTest's rule: overlapping faces at least this far apart


def num(v):
    """A number the way Blockbench (JavaScript) writes it."""
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, int):
        return str(v)
    if v == int(v) and abs(v) < 1e15:
        return str(int(v))
    return repr(v)


def dump(v, depth=0):
    """Blockbench's layout: tabs, arrays of plain values inline, and an object inline when it is
    three deep or more and holds nothing but plain values and arrays of them."""
    tab = "\t" * (depth + 1)
    if isinstance(v, dict):
        flat = all(not isinstance(x, dict) and not (isinstance(x, list) and any(
            isinstance(y, (dict, list)) for y in x)) for x in v.values())
        if depth >= 3 and flat:
            return "{" + ", ".join(json.dumps(k) + ": " + dump(x, depth + 1) for k, x in v.items()) + "}"
        if not v:
            return "{}"
        return "{\n" + ",\n".join(tab + json.dumps(k) + ": " + dump(x, depth + 1)
                                   for k, x in v.items()) + "\n" + "\t" * depth + "}"
    if isinstance(v, list):
        if all(not isinstance(x, (dict, list)) for x in v):
            return "[" + ", ".join(dump(x, depth + 1) for x in v) + "]"
        return "[\n" + ",\n".join(tab + dump(x, depth + 1) for x in v) + "\n" + "\t" * depth + "]"
    if isinstance(v, str):
        return json.dumps(v, ensure_ascii=False)
    if v is None:
        return "null"
    return num(v)


def dump_expanded(v):
    """The other layout in the tree: everything on its own line, tab indented."""
    return json.dumps(v, indent="\t", ensure_ascii=False)


def layout_of(model, body):
    """The writer that reproduces this file byte for byte, or None if neither does."""
    for writer in (dump, dump_expanded):
        if writer(model) == body:
            return writer
    return None


def z_range(e):
    return min(e["from"][2], e["to"][2]), max(e["from"][2], e["to"][2])


def xy_overlap(a, b):
    """Whether two elements' boxes overlap in x and y (unrotated extents; plates here are
    centred on their art, so a rotated plate's unrotated box still overlaps it)."""
    for i in (0, 1):
        lo = max(min(a["from"][i], a["to"][i]), min(b["from"][i], b["to"][i]))
        hi = min(max(a["from"][i], a["to"][i]), max(b["from"][i], b["to"][i]))
        if hi - lo <= 1e-6:
            return False
    return True


def plates_behind_art(model):
    """Indices of plate elements that draw a north face right behind an art sliver's."""
    els = model.get("elements", [])
    hit = []
    for s in els:
        z0, z1 = z_range(s)
        north = s.get("faces", {}).get("north")
        if north is None or z1 - z0 > SLIVER_MAX:
            continue
        for j, p in enumerate(els):
            if p is s or j in hit:
                continue
            pn = p.get("faces", {}).get("north")
            pz0, pz1 = z_range(p)
            if (pn is not None and pn.get("texture") != north.get("texture")
                    and pz1 - pz0 > PLATE_MIN and abs(pz0 - z1) < 1e-6 and xy_overlap(s, p)):
                hit.append(j)
    return sorted(hit)


def covers(a, b):
    """Whether unrotated element a's x-y rectangle covers b's."""
    if any((e.get("rotation") or {}).get("angle") for e in (a, b)):
        return False
    return all(min(a["from"][i], a["to"][i]) <= min(b["from"][i], b["to"][i]) + 1e-6
               and max(a["from"][i], a["to"][i]) >= max(b["from"][i], b["to"][i]) - 1e-6
               for i in (0, 1))


def redundant_caps(model):
    """Indices of elements whose north face sits just behind the art and is not needed.

    The yield-shaped signs have no plate: the art hangs on the post, and the post's thin centre
    bar reaches forward to touch it, its end cap 0.015 units behind the art. The next box of the
    post ends a quarter unit further back in the same metal and covers the bar's cap entirely, so
    wherever the art is see-through the eye lands on that cap instead and nothing changes -- the
    bar's cap can simply go.
    """
    els = model.get("elements", [])
    plates = set(plates_behind_art(model))
    hit = []
    for s in els:
        z0, z1 = z_range(s)
        north = s.get("faces", {}).get("north")
        if north is None or z1 - z0 > SLIVER_MAX:
            continue
        for j, p in enumerate(els):
            pn = p.get("faces", {}).get("north")
            if p is s or j in plates or j in hit or pn is None:
                continue
            pz0 = z_range(p)[0]
            if not (0 <= pz0 - z0 < MIN_GAP and pn.get("texture") != north.get("texture")
                    and xy_overlap(s, p)):
                continue
            for q in els:
                qn = q.get("faces", {}).get("north")
                if (q is not p and q is not s and qn is not None
                        and qn.get("texture") == pn.get("texture")
                        and MIN_GAP <= z_range(q)[0] - z0 <= MIN_GAP + 0.5 and covers(q, p)):
                    hit.append(j)
                    break
    return sorted(hit)


def defects(model):
    return len(plates_behind_art(model)) + len(redundant_caps(model))


def fix(model):
    """Moves each plate's north face onto a recessed backing element, and drops each redundant
    cap. Returns the count."""
    caps = redundant_caps(model)
    for j in caps:
        del model["elements"][j]["faces"]["north"]
    idx = plates_behind_art(model)
    els = model["elements"]
    for j in reversed(idx):
        plate = els[j]
        z0, z1 = z_range(plate)
        backing = {}
        for k, v in plate.items():
            if k == "faces":
                backing[k] = {"north": copy.deepcopy(v["north"])}
            elif k == "name":
                backing[k] = "backing"
            else:
                backing[k] = copy.deepcopy(v)
        zi_from = 2
        lo_key = "from" if plate["from"][2] <= plate["to"][2] else "to"
        backing[lo_key][zi_from] = round(z0 + (z1 - z0) * RECESS, 5)
        del plate["faces"]["north"]
        els.insert(j + 1, backing)
        # Blockbench groups name elements by index: keep them pointing at the same elements.
        shift_groups(model.get("groups"), j)
    return len(idx) + len(caps)


def shift_groups(groups, inserted_after):
    if not isinstance(groups, list):
        return
    for n, g in enumerate(groups):
        if isinstance(g, int):
            if g > inserted_after:
                groups[n] = g + 1
        elif isinstance(g, dict):
            shift_groups(g.get("children"), inserted_after)
    # the backing joins the group its plate is in
    for n, g in enumerate(list(groups)):
        if isinstance(g, int) and g == inserted_after:
            groups.insert(n + 1, inserted_after + 1)
            break


def model_files():
    for t in TREES:
        for p in sorted(glob.glob(os.path.join(t, "models", "block", "**", "*.json"), recursive=True)):
            yield p


def main():
    apply, check = "--apply" in sys.argv, "--check" in sys.argv
    only = [a[len("--only="):] for a in sys.argv if a.startswith("--only=")]
    found = changed = skipped = 0
    for path in model_files():
        if only and not any(o in os.path.basename(path) for o in only):
            continue
        with open(path, "r", encoding="utf-8", newline="") as fh:
            text = fh.read()
        try:
            model = json.loads(text)
        except ValueError:
            continue
        if not isinstance(model, dict) or not defects(model):
            continue
        found += 1
        rel = os.path.relpath(path, ROOT).replace("\\", "/")
        eol = "\r\n" if "\r\n" in text else "\n"
        body = text.replace("\r\n", "\n")
        writer = layout_of(model, body.rstrip("\n"))
        n = fix(model)
        if check:
            print("DEFECT  %s (%d face%s too close behind the art)" % (rel, n, "" if n == 1 else "s"))
            continue
        if writer is None:
            skipped += 1
            print("SKIP    %s -- in neither known layout, would reformat; fix by hand" % rel)
            continue
        print("%s %s (%d)" % ("FIXED  " if apply else "would  ", rel, n))
        if apply:
            out = writer(model) + body[len(body.rstrip("\n")):]
            json.loads(out)
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(out.replace("\n", eol))
            changed += 1
    print("%d model(s) with the defect, %d rewritten, %d skipped" % (found, changed, skipped))
    if check and found:
        sys.exit(1)
    if skipped:
        sys.exit(2)


if __name__ == "__main__":
    main()
