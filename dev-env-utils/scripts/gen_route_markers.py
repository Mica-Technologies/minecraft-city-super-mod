#!/usr/bin/env python3
"""Assets for the Dynamic Route Marker Sign -- the standalone route shield a route is posted
under, carrying any of the markers the dynamic highway guide sign offers.

What it writes, all under the roads module:

    textures/blocks/trafficsigns/route_marker_<shield>.png        the marker's face
    textures/blocks/trafficsigns/route_marker_<shield>_back.png   its unpainted gray back
    models/block/trafficsigns/route_marker_sign.json              plate + the standard sign post
    models/block/trafficsigns/route_marker_sign_setback.json      the same, 12.5 back
    models/block/trafficsigns/route_marker_sign_back_to_back.json the plate alone, 28.5 back
    blockstates/dynamic_route_marker_sign.json                    facing/downward/shift/shield

The marker faces are cut from the guide sign atlas that already ships
(`textures/blocks/trafficaccessories/guidesign/sign_atlas.png`), one 64 px cell per shield,
so a marker on a post and the same marker on a guide sign are the same artwork and can never
drift apart. The cells are already alpha-cut to the marker's outline, which is what lets the
sign be built on the yield sign's silhouette model -- a double-sided plate with the face on
slot "1" and a gray back on slot "2" -- rather than on a rectangular plate whose bare metal
would show around a shield.

Nothing here draws the route number. That is the tile entity's, painted over the plate by
TileEntityDynamicRouteMarkerSignRenderer at the position each shield's own
GuideSignShieldType entry gives, so one texture serves every route number.

Usage:
    python gen_route_markers.py            # write the tree
    python gen_route_markers.py --check    # write nothing; exit 1 on drift
"""

import argparse
import filecmp
import json
import os
import re
import shutil
import sys
import tempfile

from PIL import Image

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ROADS = os.path.join(REPO, "modules", "roads", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(ROADS, "textures", "blocks", "trafficsigns")
MODEL_DIR = os.path.join(ROADS, "models", "block", "trafficsigns")
STATE_DIR = os.path.join(ROADS, "blockstates")
ATLAS = os.path.join(ROADS, "textures", "blocks", "trafficaccessories", "guidesign",
                     "sign_atlas.png")
SHIELD_ENUM = os.path.join(
    REPO, "modules", "roads", "src", "main", "java", "com", "micatechnologies", "minecraft",
    "csm", "trafficaccessories", "guidesign", "GuideSignShieldType.java")

REGISTRY = "dynamic_route_marker_sign"
TEX_PREFIX = "route_marker_"
MODEL = "route_marker_sign"
CELL = 64
SIZE = 128                      # the mod's ordinary sign texture size
BACK_GRAY = (150, 150, 150, 255)   # shs_signs.back_texture's unpainted gray

TEX_REF = "csm:blocks/trafficsigns/%s"
MODEL_REF = "csm:trafficsigns/%s"
BLANK = TEX_REF % "absolutely_nothing_sign"

# The eight facings and the y rotation each one needs, exactly as every other sign's
# blockstate lists them.
FACINGS = (("n", 0), ("nw", 45), ("w", 90), ("sw", 135),
           ("s", 180), ("se", 225), ("e", 270), ("ne", 315))


# --------------------------------------------------------------------------------------------
# The shields, read from the enum that defines them
# --------------------------------------------------------------------------------------------

def shields():
    """Every GuideSignShieldType, in declaration order, as (property name, atlas col, row).

    Parsed from the enum rather than duplicated here: the ordinals are serialized in sign
    documents, so the list must be the enum's and in the enum's order.
    """
    with open(SHIELD_ENUM, "r", encoding="utf-8") as fh:
        source = fh.read()
    head = re.search(r"public enum GuideSignShieldType[^{]*\{", source)
    if head is None:
        raise SystemExit("cannot find the enum body in " + SHIELD_ENUM)
    body = source[head.end():]
    found = []
    for line in body.splitlines():
        stripped = line.strip()
        if stripped.startswith("//") or stripped.startswith("*"):
            continue
        match = re.match(r"^([A-Z][A-Z0-9_]*)\(\s*(\d+)\s*,\s*(\d+)\s*,\s*\"", stripped)
        if match:
            found.append((match.group(1).lower(), int(match.group(2)), int(match.group(3))))
        if stripped.startswith("public static final"):
            break
    if not found:
        raise SystemExit("no shields parsed out of " + SHIELD_ENUM)
    return found


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

def _atlas():
    return Image.open(ATLAS).convert("RGBA")


def face_texture(atlas, col, row):
    """One marker, cut from its atlas cell and scaled to the mod's sign texture size."""
    cell = atlas.crop((col * CELL, row * CELL, (col + 1) * CELL, (row + 1) * CELL))
    return cell.resize((SIZE, SIZE), Image.LANCZOS)


def back_texture(face):
    """The back of a silhouette sign: the face's outline in unpainted gray, mirrored the way
    the model's south face reads its UVs."""
    back = Image.new("RGBA", face.size, BACK_GRAY)
    back.putalpha(face.getchannel("A"))
    return back.transpose(Image.FLIP_LEFT_RIGHT)


def textures():
    atlas = _atlas()
    out = {}
    for name, col, row in shields():
        face = face_texture(atlas, col, row)
        out[TEX_PREFIX + name] = face
        out[TEX_PREFIX + name + "_back"] = back_texture(face)
    return out


# --------------------------------------------------------------------------------------------
# Models
# --------------------------------------------------------------------------------------------

def _post(z0):
    """The standard sign post, the five nested bars every sign model carries, with its front
    face at ``z0``. Copied unchanged from the shared sign models so a route marker stands on
    exactly the post its neighbours do."""
    bars = ((7.25, 8.75, 0.75, 3.25, None),
            (7.0, 9.0, 1.0, 3.0, None),
            (6.5, 9.5, 1.5, 2.5, None),
            (6.75, 9.25, 1.25, 2.75, None),
            (7.5, 8.5, 0.5, 3.5, "north"))
    els = []
    for x0, x1, za, zb, drop in bars:
        faces = {}
        for side in ("north", "east", "south", "west", "up", "down"):
            if side == drop:
                continue
            face = {"uv": [0, 0, 4, 16], "texture": "#0"}
            if drop == "north" and side in ("up", "down"):
                # The centre bar's end caps keep the quarter turn the shared models give them.
                face["rotation"] = 90 if side == "up" else 270
            faces[side] = face
        els.append({"from": [x0, 0, za + z0], "to": [x1, 16, zb + z0],
                    "rotation": {"angle": 0, "axis": "y", "origin": [8, 0, 8]},
                    "faces": faces})
    return els


def _plate(z0):
    """The marker itself: a double-sided plate one block square, its face on slot "1" and its
    gray back on slot "2", as the yield sign's silhouette model is built. There is no metal
    box behind it -- a route marker is cut to its outline, and a rectangular plate would show
    bare metal in the corners a shield leaves empty."""
    return [{"from": [0, 0, z0 + 0.485], "to": [16, 16, z0 + 0.495],
             "rotation": {"angle": 0, "axis": "y", "origin": [8, 0, 8]},
             "faces": {"north": {"uv": [0, 0, 15.999, 15.999], "texture": "#1"},
                       "south": {"uv": [15.999, 0, 0, 15.999], "texture": "#2"}}}]


def _model(elements):
    return {"credit": "CSM dynamic route marker sign (route number drawn by the tile entity)",
            "textures": {"0": BLANK, "2": TEX_REF % (TEX_PREFIX + "interstate_back")},
            "elements": elements,
            "display": {"gui": {"rotation": [0, 180, 0], "scale": [0.625, 0.625, 0.625]}}}


def models():
    out = {}
    out[MODEL] = _model(_plate(0.0) + _post(0.0))
    # Setback: the whole assembly a plate's depth behind the signal arm hardware, 12.5 back.
    out[MODEL + "_setback"] = _model(_plate(12.5) + _post(12.5))
    # Back to back: the plate alone, in the partner's block on the far side of its post, at
    # the 28.5 every sign in the mod uses. No post -- the partner's already stands here.
    back = _plate(28.5)
    back[0]["from"][2] = 28.49
    back[0]["to"][2] = 28.5
    out[MODEL + "_back_to_back"] = _model(back)
    return out


# --------------------------------------------------------------------------------------------
# Blockstate
# --------------------------------------------------------------------------------------------

def blockstate():
    facing = {}
    for name, angle in FACINGS:
        if angle:
            facing[name] = {"transform": {"rotation": [{"x": 0}, {"y": angle}, {"z": 0}]}}
        else:
            facing[name] = {}
    shield = {}
    for name, _col, _row in shields():
        shield[name] = {"textures": {"1": TEX_REF % (TEX_PREFIX + name),
                                     "2": TEX_REF % (TEX_PREFIX + name + "_back")}}
    default = shields()[0][0]
    return {
        "forge_marker": 1,
        "defaults": {
            "model": MODEL_REF % MODEL,
            "textures": {
                "all": TEX_REF % (TEX_PREFIX + default),
                "particle": TEX_REF % (TEX_PREFIX + default),
                "0": BLANK,
                "1": TEX_REF % (TEX_PREFIX + default),
                "2": TEX_REF % (TEX_PREFIX + default + "_back"),
            },
        },
        "variants": {
            "facing": facing,
            "inventory": [{}],
            "downward": {
                "false": {},
                "true": {"submodel": {"extension": {
                    "model": MODEL_REF % "sign_pole",
                    "transform": {"translation": [0.0, -1.0, 0.0]}}}},
            },
            "shift": {
                "none": {},
                "setback": {"model": MODEL_REF % (MODEL + "_setback")},
                "backtoback": {"model": MODEL_REF % (MODEL + "_back_to_back")},
            },
            "shield": shield,
            "normal": [{}],
        },
    }


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def _write_json(path, body):
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(body, fh, indent=2)
        fh.write("\n")


def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    for name, img in sorted(textures().items()):
        img.save(os.path.join(tex_dir, name + ".png"))
        written.append(("tex", name + ".png"))
    for name, body in sorted(models().items()):
        _write_json(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    _write_json(os.path.join(state_dir, REGISTRY + ".json"), blockstate())
    written.append(("state", REGISTRY + ".json"))
    return written


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    args = ap.parse_args()

    roots = {"tex": TEX_DIR, "model": MODEL_DIR, "state": STATE_DIR}
    if not args.check:
        written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR)
        print("Wrote %d route marker files" % len(written))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_route_markers_")
    try:
        tmp_roots = {k: os.path.join(tmp, k) for k in roots}
        for path in tmp_roots.values():
            os.makedirs(path, exist_ok=True)
        written = write_all(tmp_roots["tex"], tmp_roots["model"], tmp_roots["state"])
        drifted = []
        for kind, filename in written:
            here = os.path.join(roots[kind], filename)
            there = os.path.join(tmp_roots[kind], filename)
            if not os.path.exists(here) or not filecmp.cmp(here, there, shallow=False):
                drifted.append(os.path.relpath(here, REPO))
        if drifted:
            print("DRIFT: %d file(s) differ from the generator:" % len(drifted))
            for path in drifted:
                print("  " + path)
            return 1
        print("%d generated route marker files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
