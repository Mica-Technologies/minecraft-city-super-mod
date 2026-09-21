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
    "ad_billboard": dict(prefix="billboard", frame=2.0, cabinet=True, catwalk=True,
                         frame_tex="board_trim_steel", back_tex="board_cabinet_steel",
                         icon_ad="mile_high_pizza_bulletin", icon_frame=(190, 194, 200)),
    "ad_digital_billboard": dict(prefix="digital", frame=2.0, cabinet=True, catwalk=False,
                                 frame_tex="board_bezel_black", back_tex="board_cabinet_steel",
                                 icon_ad="couch_potato_plus_bulletin", icon_frame=(22, 22, 26)),
    # post_half: half the post's width, the AdBoardKind post numbers in AbstractBlockAdBoard.post
    "ad_kiosk": dict(prefix="kiosk", frame=2.0, cabinet=True, post_half=3,
                     frame_tex="board_kiosk_bezel", back_tex="board_kiosk_body",
                     icon_ad="cube_burger_portrait", icon_frame=(52, 55, 60)),
    "ad_kiosk_large": dict(prefix="kiosklarge", frame=3.0, cabinet=True, post_half=4,
                           frame_tex="board_kiosk_bezel", back_tex="board_kiosk_body",
                           icon_ad="tundra_creamery_portrait", icon_frame=(52, 55, 60)),
}

# How far a cabinet board's frame stands proud of its box, front and back.
LIP = 1.0

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


def flat(seed, base, spread=3):
    """A plain painted or anodised surface with a faint grain."""
    rng = random.Random(seed)
    image = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = rng.uniform(-spread, spread)
            image.putpixel((x, y), tuple(int(max(0, min(255, c + d))) for c in base) + (255,))
    return image


def grate():
    """Catwalk grating: a steel grid over the dark beneath it."""
    image = Image.new("RGBA", (16, 16), (28, 30, 33, 255))
    for y in range(16):
        for x in range(16):
            if x % 4 == 0 or y % 4 == 0:
                v = 120 + (7 if (x + y) % 8 == 0 else 0)
                image.putpixel((x, y), (v, v + 2, v + 5, 255))
    return image


def lamp_lens():
    """A floodlight's lens: warm white, brightest in the middle."""
    image = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 / 10.6
            v = 255 - int(60 * d)
            image.putpixel((x, y), (v, v, int(v * 0.86), 255))
    return image


def icon_kiosk(ad_name, body):
    """A kiosk's item icon: a portrait ad in a dark cabinet on a post."""
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for x in range(8, 24):
        for y in range(1, 26):
            image.putpixel((x, y), body + (255,))
    for x in range(14, 18):
        for y in range(26, 31):
            image.putpixel((x, y), body + (255,))
    for x in range(11, 21):
        image.putpixel((x, 31), body + (255,))
    ad = Image.open(os.path.join(ADS_DIR, ad_name + ".png")).convert("RGBA")
    image.paste(ad.resize((14, 23), Image.LANCZOS), (9, 2))
    return image


def icon(ad_name, frame_colour=(200, 204, 212), wide=False):
    """The item icon: the board as a small framed board showing one of the ads."""
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    left, top, right, bottom = (1, 10, 31, 22) if wide else (1, 8, 31, 24)
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


def _cube(tex):
    faces = {side: {"texture": tex, "cullface": side}
             for side in ("north", "south", "east", "west", "up", "down")}
    return {"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}


def _lips(f, z0, z1, tex):
    """A frame strip set standing proud of the box between z0 and z1, laid out as the poster
    frame is, so no two strips overlap."""
    hi = 16 - f
    return {
        "frame_left": _box((0, 0, z0), (f, 16, z1), tex),
        "frame_right": _box((hi, 0, z0), (16, 16, z1), tex),
        "frame_bottom": _box((f, 0, z0), (hi, f, z1), tex),
        "frame_top": _box((f, hi, z0), (hi, 16, z1), tex),
        "cap_bottom_left": _box((0, 0, z0), (f, f, z1), tex),
        "cap_bottom_right": _box((hi, 0, z0), (16, f, z1), tex),
        "cap_top_left": _box((0, hi, z0), (f, 16, z1), tex),
        "cap_top_right": _box((hi, hi, z0), (16, 16, z1), tex),
    }


def cabinet_models(kind):
    k = KINDS[kind]
    f = k["frame"]
    textures = {"frame": "csm:blocks/signage/" + k["frame_tex"],
                "back": "csm:blocks/signage/" + k["back_tex"],
                "grate": "csm:blocks/signage/board_catwalk_grate",
                "rail": "csm:blocks/signage/board_rail_steel",
                "lens": "csm:blocks/signage/board_lamp_lens",
                "particle": "csm:blocks/signage/" + k["back_tex"]}
    pieces = {"back": [_cube("#back")]}
    # One model per strip holding both the front and the back lip, so the multipart has no more
    # entries than the poster board has.
    front = _lips(f, 16, 16 + LIP, "#frame")
    rear = _lips(f, -LIP, 0, "#frame")
    for name in front:
        pieces[name] = [front[name], rear[name]]
    if k.get("catwalk"):
        pieces.update(service_pieces())
    if k.get("post_half"):
        pieces.update(post_pieces(k["post_half"]))
    out = {}
    for name, elements in pieces.items():
        for element in elements:
            _fit_uvs(element)
        out["%s_%s" % (k["prefix"], name)] = {"textures": textures, "elements": elements}
    return out


def service_pieces():
    """A printed billboard's service row, the bottom row of its blocks: the cabinet above stops
    at the top of the cell, and under it hangers drop to a catwalk deck that runs back under the
    cabinet and out past the face, with a floodlight on an arm out in front every third block.
    Railings only at the row's two ends: a real catwalk has none along the face, which it would
    stand in front of. The numbers under "deck" and "rail" are AbstractBlockAdBoard's collision
    boxes."""
    head = _box((6, 2.5, 28), (10, 5.5, 32), "#rail")
    head["faces"]["up"] = {"texture": "#lens"}
    head["faces"]["north"] = {"texture": "#lens"}
    head["rotation"] = {"origin": [8, 4, 30], "axis": "x", "angle": -22.5}
    return {
        "service": [
            # hangers from the cabinet's underside to the deck
            _box((3, 3.5, 6), (4, 16, 7), "#rail"),
            _box((12, 3.5, 6), (13, 16, 7), "#rail"),
            _box((3, 3.5, 9), (4, 16, 10), "#rail"),
            _box((12, 3.5, 9), (13, 16, 10), "#rail"),
            # the deck and the two stringers under it
            _box((0, 2, 4), (16, 3.5, 28), "#grate"),
            _box((0, 1, 5), (16, 2, 7), "#rail"),
            _box((0, 1, 25), (16, 2, 27), "#rail"),
        ],
        "rail_left": [
            _box((0, 3.5, 5), (1, 13, 6), "#rail"),
            _box((0, 3.5, 26), (1, 13, 27), "#rail"),
            _box((0, 13, 4), (1, 14, 28), "#rail"),
            _box((0, 8, 4.5), (1, 9, 27.5), "#rail"),
        ],
        "rail_right": [
            _box((15, 3.5, 5), (16, 13, 6), "#rail"),
            _box((15, 3.5, 26), (16, 13, 27), "#rail"),
            _box((15, 13, 4), (16, 14, 28), "#rail"),
            _box((15, 8, 4.5), (16, 9, 27.5), "#rail"),
        ],
        "lamp": [
            _box((7.5, 2.5, 28), (8.5, 3.5, 30), "#rail"),
            head,
        ],
        # the controller's column, joining the pole it stands on to the cabinet
        "column": [_box((6, 0, 6), (10, 16, 10), "#rail")],
    }


def post_pieces(half):
    """A kiosk's post, in its service rows: centred on the block's right-hand edge, which is the
    middle of the kiosk, so the block beside it (which is empty) holds nothing; and on the
    controller, a base plate to stand on. The cabinet above covers the post's top."""
    return {
        "post": [_box((16 - half, 0, 8 - half), (16 + half, 16, 8 + half), "#back")],
        "base": [_box((16 - half - 3, 0, 8 - half - 3), (16 + half + 3, 1, 8 + half + 3),
                      "#back")],
    }


def _span(a, b):
    """A face's extent along one axis as UVs: shifted by whole blocks into 0..16, never clamped,
    so the texture's grain carries on across the block edge."""
    shift = 16 * ((min(a, b)) // 16)
    if max(a, b) - shift > 16:
        # A span across a block edge cannot be shifted inside the sprite: start it at 0, and
        # squeeze one longer than a block into the sprite rather than read past its edge.
        return 0, min(16, abs(b - a))
    return a - shift, b - shift


def _fit_uvs(element):
    """Explicit UVs for an element that reaches outside its cell: left to itself Minecraft maps a
    face by its coordinates, and past 0..16 that samples the neighbouring sprites in the atlas."""
    (x0, y0, z0), (x1, y1, z1) = element["from"], element["to"]
    if min(x0, y0, z0) >= 0 and max(x1, y1, z1) <= 16:
        return
    ux = _span(x0, x1)
    uz = _span(z0, z1)
    vy = _span(16 - y1, 16 - y0)
    for side, face in element["faces"].items():
        if side in ("north", "south"):
            u, v = ux, vy
        elif side in ("east", "west"):
            u, v = uz, vy
        else:
            u, v = ux, uz
        face["uv"] = [u[0], v[0], u[1], v[1]]


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
    "service": {},
    "rail_left": {"left": "false"},
    "rail_right": {"right": "false"},
    "lamp": {"lamp": "true"},
    "column": {},
    "post": {},
    "base": {},
}

# The pieces each block of a board with a service row draws.
CABINET_PIECES = ("back", "frame_left", "frame_right", "frame_bottom", "frame_top",
                  "cap_bottom_left", "cap_bottom_right", "cap_top_left", "cap_top_right")
SERVICE_PIECES = ("service", "rail_left", "rail_right", "lamp")


def blockstate(kind, pieces, only=None):
    prefix = KINDS[kind]["prefix"]
    multipart = []
    for facing, y in ROTATION.items():
        for piece, when in CONDITIONS.items():
            if "%s_%s" % (prefix, piece) not in pieces or (only and piece not in only):
                continue
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
    files[os.path.join(TEX_DIR, "board_cabinet_steel.png")] = _png(flat(20260923, (58, 61, 66)))
    files[os.path.join(TEX_DIR, "board_trim_steel.png")] = _png(flat(20260924, (184, 188, 194)))
    files[os.path.join(TEX_DIR, "board_bezel_black.png")] = _png(flat(20260925, (20, 20, 23), 2))
    files[os.path.join(TEX_DIR, "board_rail_steel.png")] = _png(flat(20260926, (96, 100, 106)))
    files[os.path.join(TEX_DIR, "board_catwalk_grate.png")] = _png(grate())
    files[os.path.join(TEX_DIR, "board_lamp_lens.png")] = _png(lamp_lens())
    files[os.path.join(TEX_DIR, "board_kiosk_bezel.png")] = _png(flat(20260927, (44, 47, 52), 2))
    files[os.path.join(TEX_DIR, "board_kiosk_body.png")] = _png(flat(20260928, (60, 64, 70)))
    for kind, k in KINDS.items():
        kind_models = cabinet_models(kind) if k.get("cabinet") else models(kind)
        for name, model in kind_models.items():
            files[os.path.join(MODEL_DIR, name + ".json")] = _json(model)
        if k.get("post_half"):
            # The controller is the bottom of the post, on its base plate.
            files[os.path.join(STATE_DIR, kind + ".json")] = _json(
                blockstate(kind, kind_models, ("post", "base")))
            files[os.path.join(STATE_DIR, kind + "_service.json")] = _json(
                blockstate(kind, kind_models, ("post",)))
            files[os.path.join(STATE_DIR, kind + "_part.json")] = _json(
                blockstate(kind, kind_models, CABINET_PIECES))
            files[os.path.join(ITEM_MODEL_DIR, kind + "_service.json")] = _json(
                {"parent": "item/generated", "textures": {"layer0": "csm:items/signage/" + kind}})
        elif k.get("catwalk"):
            # The controller is always in the service row, and stands on the column.
            files[os.path.join(STATE_DIR, kind + ".json")] = _json(
                blockstate(kind, kind_models, SERVICE_PIECES + ("column",)))
            files[os.path.join(STATE_DIR, kind + "_service.json")] = _json(
                blockstate(kind, kind_models, SERVICE_PIECES))
            files[os.path.join(STATE_DIR, kind + "_part.json")] = _json(
                blockstate(kind, kind_models, CABINET_PIECES))
            files[os.path.join(ITEM_MODEL_DIR, kind + "_service.json")] = _json(
                {"parent": "item/generated", "textures": {"layer0": "csm:items/signage/" + kind}})
        else:
            state = _json(blockstate(kind, kind_models))
            files[os.path.join(STATE_DIR, kind + ".json")] = state
            files[os.path.join(STATE_DIR, kind + "_part.json")] = state
        if k.get("post_half"):
            files[os.path.join(ITEM_TEX_DIR, kind + ".png")] = _png(
                icon_kiosk(k["icon_ad"], k["icon_frame"]))
        else:
            files[os.path.join(ITEM_TEX_DIR, kind + ".png")] = _png(icon(
                k["icon_ad"], k.get("icon_frame", (200, 204, 212)), wide=k.get("cabinet", False)))
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
