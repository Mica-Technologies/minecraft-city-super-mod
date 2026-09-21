#!/usr/bin/env python3
"""Every asset the advertising boards' blocks ship, except the ads themselves (gen_ads.py).

    python dev-env-utils/scripts/gen_ad_boards.py
    python dev-env-utils/scripts/gen_ad_boards.py --check

A board is a controller block and the part blocks it builds round itself, and both draw the same
thing: a backing plate and, only on the blocks at the board's edge, a frame. The ad is drawn over
the whole board by the controller's renderer, so nothing here knows about it.

Models are drawn with the face to the south and the back of the board against the block's north
side, and turned by the multipart blockstate. Which sides a block has a frame on is its actual
state (left, right, up, down: whether the block that way, seen from the front, is the same
board). The frame's corners are drawn by whichever strip runs past them -- the left and right
strips run the full height, the top and bottom strips stop short of them, and a top or bottom
strip continuing into the next block over is finished by a cap -- so no two frame pieces ever
overlap and z-fight.

Numbers that must match AdBoardKind: the frame width (1 px), the depth (2.5 px). The face the
renderer draws sits at 1.5 px, between the backing (1 px) and the front of the frame.
"""

import argparse
import io
import json
import os
import random
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "modules", "signage", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(ASSETS, "textures", "blocks", "signage")
ITEM_TEX_DIR = os.path.join(ASSETS, "textures", "items", "signage")
MODEL_DIR = os.path.join(ASSETS, "models", "block", "signage")
ITEM_MODEL_DIR = os.path.join(ASSETS, "models", "item")
STATE_DIR = os.path.join(ASSETS, "blockstates")
ADS_DIR = os.path.join(ASSETS, "textures", "ads", "parody")

# kind -> the board's geometry, in pixels, and the ad its item icon shows
KINDS = {
    "ad_poster_board": dict(prefix="poster", frame=1.0, back=1.0, depth=2.5,
                            frame_tex="board_frame_aluminium", back_tex="board_back",
                            icon_ad="cube_burger_poster"),
}

# facing -> blockstate y rotation of a model drawn facing south
ROTATION = {"south": 0, "west": 90, "north": 180, "east": 270}


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

def aluminium():
    """Brushed aluminium: horizontal grain in a light cool grey, seeded so --check agrees."""
    rng = random.Random(20260921)
    image = Image.new("RGBA", (16, 16))
    for y in range(16):
        row = rng.uniform(-6, 6)
        for x in range(16):
            v = 196 + row + rng.uniform(-3, 3)
            image.putpixel((x, y), (int(v), int(v + 3), int(v + 8), 255))
    return image


def backing():
    """Dark painted sheet metal with a faint grain, for the back and the plate behind the ad."""
    rng = random.Random(20260922)
    image = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = 62 + rng.uniform(-3, 3)
            image.putpixel((x, y), (int(v), int(v + 2), int(v + 5), 255))
    return image


def icon(ad_name, frame_colour=(200, 204, 212)):
    """The item icon: the board as a small framed poster showing one of the ads."""
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    left, top, right, bottom = 1, 8, 31, 24
    for x in range(left, right):
        for y in range(top, bottom):
            image.putpixel((x, y), frame_colour + (255,))
    ad = Image.open(os.path.join(ADS_DIR, ad_name + ".png")).convert("RGBA")
    inner = ad.resize((right - left - 2, bottom - top - 2), Image.LANCZOS)
    image.paste(inner, (left + 1, top + 1))
    return image


# --------------------------------------------------------------------------------------------
# Models
# --------------------------------------------------------------------------------------------

def _box(a, b, tex):
    faces = {side: {"texture": tex} for side in ("north", "south", "east", "west", "up", "down")}
    return {"from": list(a), "to": list(b), "faces": faces}


def models(kind):
    k = KINDS[kind]
    f, d, back = k["frame"], k["depth"], k["back"]
    hi = 16 - f
    textures = {"frame": "csm:blocks/signage/" + k["frame_tex"],
                "back": "csm:blocks/signage/" + k["back_tex"],
                "particle": "csm:blocks/signage/" + k["frame_tex"]}
    pieces = {
        "back": _box((0, 0, 0), (16, 16, back), "#back"),
        "frame_left": _box((0, 0, 0), (f, 16, d), "#frame"),
        "frame_right": _box((hi, 0, 0), (16, 16, d), "#frame"),
        "frame_bottom": _box((f, 0, 0), (hi, f, d), "#frame"),
        "frame_top": _box((f, hi, 0), (hi, 16, d), "#frame"),
        "cap_bottom_left": _box((0, 0, 0), (f, f, d), "#frame"),
        "cap_bottom_right": _box((hi, 0, 0), (16, f, d), "#frame"),
        "cap_top_left": _box((0, hi, 0), (f, 16, d), "#frame"),
        "cap_top_right": _box((hi, hi, 0), (16, 16, d), "#frame"),
    }
    out = {}
    for name, element in pieces.items():
        out["%s_%s" % (k["prefix"], name)] = {"textures": textures, "elements": [element]}
    return out


# piece -> the actual-state condition it is drawn under
CONDITIONS = {
    "back": {},
    "frame_left": {"left": "false"},
    "frame_right": {"right": "false"},
    "frame_bottom": {"down": "false"},
    "frame_top": {"up": "false"},
    "cap_bottom_left": {"down": "false", "left": "true"},
    "cap_bottom_right": {"down": "false", "right": "true"},
    "cap_top_left": {"up": "false", "left": "true"},
    "cap_top_right": {"up": "false", "right": "true"},
}


def blockstate(kind):
    prefix = KINDS[kind]["prefix"]
    multipart = []
    for facing, y in ROTATION.items():
        for piece, when in CONDITIONS.items():
            apply = {"model": "csm:signage/%s_%s" % (prefix, piece)}
            if y:
                apply["y"] = y
            multipart.append({"when": dict({"facing": facing}, **when), "apply": apply})
    return {"multipart": multipart}


# --------------------------------------------------------------------------------------------
# Output
# --------------------------------------------------------------------------------------------

def _json(doc):
    return (json.dumps(doc, indent=2) + "\n").encode("utf-8")


def _png(image):
    buffer = io.BytesIO()
    image.save(buffer, format="PNG", optimize=True)
    return buffer.getvalue()


def outputs():
    files = {}
    files[os.path.join(TEX_DIR, "board_frame_aluminium.png")] = _png(aluminium())
    files[os.path.join(TEX_DIR, "board_back.png")] = _png(backing())
    for kind, k in KINDS.items():
        for name, model in models(kind).items():
            files[os.path.join(MODEL_DIR, name + ".json")] = _json(model)
        state = _json(blockstate(kind))
        files[os.path.join(STATE_DIR, kind + ".json")] = state
        files[os.path.join(STATE_DIR, kind + "_part.json")] = state
        files[os.path.join(ITEM_TEX_DIR, kind + ".png")] = _png(icon(k["icon_ad"]))
        item = _json({"parent": "item/generated",
                      "textures": {"layer0": "csm:items/signage/" + kind}})
        files[os.path.join(ITEM_MODEL_DIR, kind + ".json")] = item
        files[os.path.join(ITEM_MODEL_DIR, kind + "_part.json")] = item
    return files


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files = outputs()
    stale = [p for p, data in files.items()
             if not os.path.exists(p) or open(p, "rb").read() != data]
    if args.check:
        for p in stale:
            print("stale: " + os.path.relpath(p, REPO))
        return 1 if stale else 0
    for p in stale:
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as out:
            out.write(files[p])
    print("%d files, %d written" % (len(files), len(stale)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
