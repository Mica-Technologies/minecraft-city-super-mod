#!/usr/bin/env python3
"""The octagon (STOP) sign's back: one face cut to the octagon, in place of the overlapping
faces of the four boxes the octagon is built from (GitHub #358).

    python dev-env-utils/scripts/gen_octagon_sign_back.py           # write
    python dev-env-utils/scripts/gen_octagon_sign_back.py --check   # fail on drift

An octagon plate is four boxes: an upright and a level rectangle and the same two turned 45
degrees, each 0.5 px deep. Every box drew its own back face on the plate's back plane, so where
two boxes overlap two faces share one plane, and the depth buffer cannot tell which is in front:
the back of every STOP sign flickered in a bow-tie where the turned boxes cross. The front never
did, because the art is one separate plane in front of the boxes.

So the boxes keep their edges and lose their flat faces on the plate's planes, and one plane,
the octagon's whole square, takes them over, its texture (``octagon_sign_back``) the plain sign
back cut to the octagon by the stop art's own alpha (``pole_stop``), so the back's outline is the
front's exactly. The sign is drawn in the cutout layer, which is what cuts it.

Models patched (all hand-made Blockbench models; patched in place, keeping their style):
  front:      lhs_stop_sign_not_compliant, metal_signpost_octagon_sign_setback,
              metal_signpost_octagon_sign_back_to_back  -- the back face only (the art is the front)
  back-only:  metal_signpostback_octagon_setback, metal_signpostback_octagon_back_to_back,
              sign_pole_octagon_back  -- both faces (a blank back each way)
"""
import json
import os
import sys

from PIL import Image

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ROADS = os.path.join(REPO, "modules", "roads", "src", "main", "resources", "assets", "csm")
CORE = os.path.join(REPO, "src", "main", "resources", "assets", "csm")
MODELS = os.path.join(ROADS, "models", "block", "trafficsigns")
ART = os.path.join(ROADS, "textures", "blocks", "trafficsigns", "pole_stop.png")
PLAIN_BACK = os.path.join(CORE, "textures", "blocks", "trafficsigns", "absolutely_nothing_sign.png")
OUT_TEXTURE = os.path.join(ROADS, "textures", "blocks", "trafficsigns", "octagon_sign_back.png")
BACK_REF = "csm:blocks/trafficsigns/octagon_sign_back"

FRONT = ["lhs_stop_sign_not_compliant", "metal_signpost_octagon_sign_setback",
         "metal_signpost_octagon_sign_back_to_back"]
BACK_ONLY = ["metal_signpostback_octagon_setback", "metal_signpostback_octagon_back_to_back",
             "sign_pole_octagon_back"]

SQUARE = (-4.0, -4.0, 20.0, 20.0)   # the octagon's square, as the art plane spans it
SKIN = 0.01                          # the plane's depth


def texture():
    """The plain back where the stop art is opaque, clear elsewhere (cutout: 0 or 255)."""
    art = Image.open(ART).convert("RGBA")
    plain = Image.open(PLAIN_BACK).convert("RGBA").resize(art.size, Image.NEAREST)
    out = Image.new("RGBA", art.size, (0, 0, 0, 0))
    a = art.split()[3]
    po, pa, pp = out.load(), a.load(), plain.load()
    for y in range(art.size[1]):
        for x in range(art.size[0]):
            if pa[x, y] >= 128:
                r, g, b, _ = pp[x, y]
                po[x, y] = (r, g, b, 255)
    return out


def is_plate(el):
    rot = el.get("rotation") or {}
    return rot.get("axis") == "z" and abs((el["to"][2] - el["from"][2]) - 0.5) < 1e-6


def plane(z0, z1, face):
    x0, y0, x1, y1 = SQUARE
    return {"name": "octagon_" + face, "from": [x0, y0, z0], "to": [x1, y1, z1],
            "faces": {face: {"uv": [0, 0, 16, 16], "texture": "#back"}}}


def patch(model, back_only):
    """The patched model, or None if it is already patched."""
    els = model["elements"]
    if any(e.get("name", "").startswith("octagon_south") for e in els):
        return None
    plates = [e for e in els if is_plate(e)]
    assert len(plates) == 4, (len(plates), "plate boxes")
    z0 = plates[0]["from"][2]
    z1 = plates[0]["to"][2]
    assert all(p["from"][2] == z0 and p["to"][2] == z1 for p in plates)
    drop = ("north", "south") if back_only else ("south",)
    for p in plates:
        for f in drop:
            p["faces"].pop(f, None)
    new = [plane(z1 - SKIN, z1, "south")]
    if back_only:
        new.insert(0, plane(z0, z0 + SKIN, "north"))
    at = els.index(plates[-1]) + 1
    model["elements"] = els[:at] + new + els[at:]
    model.setdefault("textures", {})["back"] = BACK_REF
    return model


# --- Blockbench's style: tabs, CRLF, a face or rotation on one line, arrays inline -----------
def _num(v):
    if isinstance(v, float) and v.is_integer():
        return str(int(v))
    return json.dumps(v)


def _inline(v):
    if isinstance(v, dict):
        return "{" + ", ".join('"%s": %s' % (k, _inline(x)) for k, x in v.items()) + "}"
    if isinstance(v, list):
        return "[" + ", ".join(_inline(x) for x in v) + "]"
    if isinstance(v, (int, float)) and not isinstance(v, bool):
        return _num(v)
    return json.dumps(v)


def _dump(v, depth, key=None):
    t = "\t" * depth
    if isinstance(v, dict) and key not in ("rotation",) and not _is_face(key, v):
        items = ['%s\t"%s": %s' % (t, k, _dump(x, depth + 1, k)) for k, x in v.items()]
        return "{\n" + ",\n".join(items) + "\n" + t + "}"
    if isinstance(v, list) and v and isinstance(v[0], dict):
        items = ["%s\t%s" % (t, _dump(x, depth + 1)) for x in v]
        return "[\n" + ",\n".join(items) + "\n" + t + "]"
    return _inline(v)


def _is_face(key, v):
    return key in ("north", "south", "east", "west", "up", "down") and "texture" in v


def dump(model, raw):
    """In the file's own style: Blockbench's compact one, or fully expanded (json with tabs),
    with its own line endings."""
    expanded = '"from": [\n' in raw.replace("\r\n", "\n")
    text = json.dumps(model, indent="\t") if expanded else _dump(model, 0)
    eol = "\r\n" if "\r\n" in raw else "\n"
    return text.replace("\n", eol) + (eol if raw.endswith("\n") else "")


def main():
    check = "--check" in sys.argv
    stale = []
    tex = texture()
    if not os.path.exists(OUT_TEXTURE) or Image.open(OUT_TEXTURE).convert("RGBA").tobytes() \
            != tex.tobytes():
        stale.append(OUT_TEXTURE)
        if not check:
            tex.save(OUT_TEXTURE)
    for name in FRONT + BACK_ONLY:
        path = os.path.join(MODELS, name + ".json")
        with open(path, "rb") as fh:
            raw = fh.read().decode("utf-8")
        patched = patch(json.loads(raw), name in BACK_ONLY)
        if patched is None:
            continue
        stale.append(path)
        if not check:
            with open(path, "wb") as fh:
                fh.write(dump(patched, raw).encode("utf-8"))
    if check:
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("gen_octagon_sign_back.py: up to date")
        return 0
    print("wrote %d files" % len(stale))
    return 0


if __name__ == "__main__":
    sys.exit(main())
