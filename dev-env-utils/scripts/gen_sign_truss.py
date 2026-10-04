#!/usr/bin/env python3
"""Every asset the overhead sign trusses ship (CSM: Roads & Traffic): textures, part models and
blockstates.

    python dev-env-utils/scripts/gen_sign_truss.py
    python dev-env-utils/scripts/gen_sign_truss.py --check
    python dev-env-utils/scripts/gen_sign_truss.py --fragments   # lang and tab lines to paste

A galvanized box truss, the common US sign bridge and cantilever: four round chords at the corners
and zig-zag lacing on every face. As on the tower crane's mast (gen_crane.py, whose approach this
borrows), the chords are geometry and the lacing is a cutout texture on a plane between them, so a
diagonal can run chord to chord at any angle and lines up from block to block.

THE 1x1 TRUSS (`sign_truss`). One block across. Its axis is stored (x, y or z): along x or z it
spans, along y it stands as a support. Each part is drawn standing up (along y) and the
blockstate lays it down for the other axes, the way a log is turned. Where the truss does not
carry on, an end frame closes it: that is also the cantilever's end. A truss standing on
something that is not truss stands on a base plate with anchor bolts instead.

THE 2x2 TRUSS (`sign_truss_large`). Two blocks across each way: each block is one quarter of the
section and works out which quarter from its neighbours, as the crane's 2x2 mast does, so a quarter
is drawn once (the low-a, low-b corner, standing up) and the blockstate turns it into the other
three. Each face of the section is two blocks wide; it shows one X two blocks wide a block of
length, each block its half across, so nothing has to count blocks along the truss.

ROTATIONS. A part standing up (along y) is laid along z by x 90 (its bottom then faces south) and
along x by x 90 then y 90 (its bottom faces west). BlockSignTruss names its ends in the world (neg
and pos along the axis), and the blockstate maps them onto the part's bottom and top here.
"""

import argparse
import filecmp
import json
import os
import random
import shutil
import sys
import tempfile

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_scaffold as sc  # noqa: E402 -- its element helper fits every face's UVs

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "modules", "roads", "src", "main", "resources", "assets", "csm")
FOLDER = "trafficaccessories/truss"
TEX_DIR = os.path.join(ASSETS, "textures", "blocks", FOLDER)
MODEL_DIR = os.path.join(ASSETS, "models", "block", FOLDER)
STATE_DIR = os.path.join(ASSETS, "blockstates")
TEX_REF = "csm:blocks/" + FOLDER + "/%s"
MODEL_REF = "csm:" + FOLDER + "/%s"

SMALL = "sign_truss"
LARGE = "sign_truss_large"
CATWALK = "sign_truss_catwalk"
LIGHT = "sign_truss_light"
NAMES = {
    SMALL: ("Overhead Sign Truss", "Celosía para Señales Elevadas", "Schilderbrücken-Fachwerk",
            "Fackverk för Portalskyltar"),
    LARGE: ("Overhead Sign Truss (2x2)", "Celosía para Señales Elevadas (2x2)",
            "Schilderbrücken-Fachwerk (2x2)", "Fackverk för Portalskyltar (2x2)"),
    LIGHT: ("Low-Profile Sign Truss", "Celosía Ligera para Señales Elevadas",
            "Leichtes Schilderbrücken-Fachwerk", "Lätt Fackverk för Portalskyltar"),
    CATWALK: ("Sign Truss Catwalk", "Pasarela de Celosía para Señales",
              "Laufsteg für Schilderbrücken", "Gångbrygga för Portalskyltar"),
}
LANGS = ("en_us", "es_es", "de_de", "sv_se")

GALV = (172, 177, 181)            # galvanized steel
CHORD = (0.5, 2.0)                # a 1x1 chord's near and far edge from the cell's corner
FACE_INSET = 1.25                 # its lacing plane, mid-chord
LARGE_CHORD = (0.5, 3.0)          # a 2x2 section's chord, at its outer corner only
LACE_SIZE = 32
LACE_W = 2

box = sc._box


def _shift(colour, d):
    return tuple(max(0, min(255, int(round(c + d)))) for c in colour[:3]) + (255,)


def chord_texture():
    """Galvanized tube: banded lengthwise so a chord reads round, with the spangle's faint mottle."""
    rng = random.Random(20261004)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    band = [12, 8, 3, -3, -9, -6, 0, 6] * 2
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(GALV, band[x] + rng.uniform(-4, 4))
    return img


def lacing_texture(wide):
    """One face's lacing on a clear ground for the cutout layer: an X chord to chord and a strut
    along the bottom. `wide` draws the X two blocks across a block of length, for the 2x2's
    faces, whose two blocks each show half of it."""
    s = LACE_SIZE
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    fill = _shift(GALV, -10)
    edge = _shift(GALV, -34)
    draw.rectangle([0, s - LACE_W, s - 1, s - 1], fill=fill)
    for x0, y0, x1, y1 in ((0, s - 1, s - 1, 0), (0, 0, s - 1, s - 1)):
        draw.line([(x0, y0), (x1, y1)], fill=fill, width=LACE_W)
    px = img.load()
    for y in range(s - 1, 0, -1):
        for x in range(s):
            if px[x, y][3] == 0 and px[x, y - 1][3] != 0 and px[x, y - 1][:3] == fill[:3]:
                px[x, y] = edge
    return img


def plate_texture():
    """Hot-dip galvanized plate: flatter than the tubes, a little darker, with spangle."""
    rng = random.Random(4102026)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(GALV, -14 + rng.uniform(-6, 6))
    return img


def grate_texture():
    """Bar grating, seen from above: bearing bars across, cross rods along, on a clear ground
    for the cutout layer, so the road shows through the catwalk as it does through a real one."""
    s = 32
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    bar = _shift(GALV, -6)
    rod = _shift(GALV, -22)
    for x in range(0, s, 4):
        draw.rectangle([x, 0, x + 1, s - 1], fill=bar)
    for y in range(2, s, 8):
        draw.rectangle([0, y, s - 1, y], fill=rod)
    draw.rectangle([0, 0, s - 1, 0], fill=bar)
    draw.rectangle([0, s - 1, s - 1, s - 1], fill=bar)
    return img


def _line_texture(lines, strut=None, width=2):
    """Light truss webbing on a clear ground: each line a thin pipe from point to point, on a
    32 px panel stretched over the face between the chords."""
    s = 32
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    fill = _shift(GALV, -8)
    for x0, y0, x1, y1 in lines:
        draw.line([(x0, y0), (x1, y1)], fill=fill, width=width)
    if strut:
        draw.rectangle(strut, fill=_shift(GALV, -2))
    return img


TEXTURES = {
    # a vertical at the panel's west edge and a V between, so a run reads as verticals and
    # Warren diagonals
    "truss_light_lace": lambda: _line_texture([(1, 31, 16, 0), (16, 0, 30, 31)],
                                              strut=[0, 0, 1, 31]),
    # the horizontal bracing across the top and bottom: one diagonal a panel
    "truss_light_brace": lambda: _line_texture([(1, 31, 30, 0)], strut=[0, 0, 1, 31]),
    # a support frame's ladder: a rung at the foot of each block and a diagonal
    "truss_ladder": lambda: _line_texture([(2, 29, 29, 2)], strut=[0, 28, 31, 31], width=5),
    "truss_grate": grate_texture,
    "truss_chord": chord_texture,
    "truss_lace": lambda: lacing_texture(False),
    "truss_lace_wide": lambda: lacing_texture(True),
    "truss_plate": plate_texture,
}


def _tex(lace="truss_lace"):
    return {"chord": TEX_REF % "truss_chord", "lace": TEX_REF % lace,
            "plate": TEX_REF % "truss_plate", "particle": TEX_REF % "truss_chord"}


# --------------------------------------------------------------------------------------------
# 1x1 parts, standing up
# --------------------------------------------------------------------------------------------

def chords():
    c0, c1 = CHORD
    out = []
    for x0, x1 in ((c0, c1), (16 - c1, 16 - c0)):
        for z0, z1 in ((c0, c1), (16 - c1, 16 - c0)):
            out.append(box(x0, 0, z0, x1, 16, z1, "#chord"))
    return out


def lacing():
    a, b = CHORD[1], 16 - CHORD[1]
    i, o = FACE_INSET, 16 - FACE_INSET
    uv = [0, 0, 16, 16]
    return [
        box(a, 0, i, b, 16, i, "#lace", faces=("north", "south"), uv=uv),
        box(a, 0, o, b, 16, o, "#lace", faces=("north", "south"), uv=uv),
        box(i, 0, a, i, 16, b, "#lace", faces=("east", "west"), uv=uv),
        box(o, 0, a, o, 16, b, "#lace", faces=("east", "west"), uv=uv),
    ]


def end_frame(y0, y1):
    """The square frame closing an end the truss does not carry on from, with a cross brace:
    what a cantilever's arm ends in, and the end of every span."""
    c0, c1 = CHORD
    a, b = c1, 16 - c1
    out = [box(a, y0, c0, b, y1, c1, "#plate"), box(a, y0, 16 - c1, b, y1, 16 - c0, "#plate"),
           box(c0, y0, a, c1, y1, b, "#plate"), box(16 - c1, y0, a, 16 - c0, y1, b, "#plate")]
    return out


def base_plate():
    """Standing on something that is not truss: a plate with an anchor bolt by each chord."""
    out = [box(0, 0, 0, 16, 1, 16, "#plate")]
    for x in (2.5, 12.0):
        for z in (2.5, 12.0):
            out.append(box(x, 1, z, x + 1.5, 2.5, z + 1.5, "#plate"))
    return out


# --------------------------------------------------------------------------------------------
# 2x2 quarter parts: the low-a, low-b quarter (north-west when standing up)
# --------------------------------------------------------------------------------------------

def large_chord():
    c0, c1 = LARGE_CHORD
    return [box(c0, 0, c0, c1, 16, c1, "#chord")]


def large_lacing():
    """Its share of its two outer faces: from its chord to the cell's edge, half the wide X
    across. The two faces take opposite halves, so turned into the other corners each face is
    whole."""
    c = LARGE_CHORD[1]
    i = (LARGE_CHORD[0] + LARGE_CHORD[1]) / 2
    return [
        box(c, 0, i, 16, 16, i, "#lace", faces=("north", "south"), uv=[0, 0, 8, 16]),
        box(i, 0, c, i, 16, 16, "#lace", faces=("east", "west"), uv=[8, 0, 16, 16]),
    ]


def large_end(y0, y1):
    c0, c1 = LARGE_CHORD
    return [box(c1, y0, c0, 16, y1, c1, "#plate"), box(c0, y0, c1, c1, y1, 16, "#plate")]


def large_base():
    return [box(0, 0, 0, 8, 1, 8, "#plate"), box(4, 1, 4, 5.5, 2.5, 5.5, "#plate")]


def large_inventory():
    """A whole 2x2 section, centred on the item's cell (-8..24), for the item icon."""
    c0, c1 = LARGE_CHORD
    lo, hi = c0 - 8, 32 - c0 - 8
    a, b = c1 - 8, 32 - c1 - 8
    i, o = (c0 + c1) / 2 - 8, 32 - (c0 + c1) / 2 - 8
    els = []
    for x0, x1 in ((lo, a), (b, hi)):
        for z0, z1 in ((lo, a), (b, hi)):
            els.append(box(x0, 0, z0, x1, 16, z1, "#chord"))
    full = [0, 0, 16, 16]
    els += [
        box(a, 0, i, b, 16, i, "#lace", faces=("north", "south"), uv=full),
        box(a, 0, o, b, 16, o, "#lace", faces=("north", "south"), uv=full),
        box(i, 0, a, i, 16, b, "#lace", faces=("east", "west"), uv=full),
        box(o, 0, a, o, 16, b, "#lace", faces=("east", "west"), uv=full),
    ]
    return els


LARGE_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.32, 0.32, 0.32]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.13, 0.13, 0.13]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.25, 0.25, 0.25]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                              "scale": [0.19, 0.19, 0.19]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                              "scale": [0.2, 0.2, 0.2]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                             "scale": [0.2, 0.2, 0.2]},
}


# --------------------------------------------------------------------------------------------
# The catwalk: drawn facing north, its railing on the north (outer) edge and the truss to the
# south, behind it
# --------------------------------------------------------------------------------------------

CW_FLOOR = 1.0       # the grating's top
CW_RAIL = 15.0       # the top rail's underside: about the 42 inches a guardrail stands
CW_MID = 8.0
CW_POST = (0.5, 1.5)  # a post's thickness band, along x and z


def catwalk_body():
    """Grating the full block, the toe board and railing along the outer edge (a post at the
    block's west end, so joined blocks share one post a block), and two brackets under the
    floor reaching back into the truss behind."""
    p0, p1 = CW_POST
    out = [box(0, 0, 0, 16, CW_FLOOR, 16, "#grate", faces=("up", "down"),
               uv=[0, 0, 16, 16]),
           box(0, 0, 0, 16, CW_FLOOR, 16, "#plate", faces=("north", "south", "east", "west")),
           box(0, CW_FLOOR, p0, 16, CW_FLOOR + 2.5, p1, "#plate"),             # toe board
           box(0, CW_RAIL, p0, 16, CW_RAIL + 1, p1, "#chord"),                  # top rail
           box(0, CW_MID, p0, 16, CW_MID + 0.75, p1, "#chord"),                 # mid rail
           box(p0, CW_FLOOR, p0, p1, CW_RAIL, p1, "#chord")]                    # post
    for x in (3.0, 12.0):
        out += [box(x, -2.0, 4, x + 1, 0, 16, "#plate"),                          # bracket
                box(x, -2.0, 16, x + 1, CW_FLOOR, 17.5, "#plate")]                # to the truss
    return out


def catwalk_end(east):
    """The railing across an end of the catwalk that nothing carries on from."""
    x0, x1 = (16 - CW_POST[1], 16 - CW_POST[0]) if east else CW_POST
    out = [box(x0, CW_RAIL, CW_POST[1], x1, CW_RAIL + 1, 16, "#chord"),
           box(x0, CW_MID, CW_POST[1], x1, CW_MID + 0.75, 16, "#chord"),
           box(x0, CW_FLOOR, 15, x1, CW_RAIL, 16, "#chord")]
    if east:
        out.append(box(x0, CW_FLOOR, CW_POST[0], x1, CW_RAIL, CW_POST[1], "#chord"))
    return out


# --------------------------------------------------------------------------------------------
# The low-profile truss: a span drawn along x, a support frame drawn to carry a span along x
# (its posts spaced along z). BlockSignTrussLight's SPAN_TOP and SPAN_FRONT are LT_Y[1][1] and
# LT_Z[0][0].
# --------------------------------------------------------------------------------------------

LT_Y = ((2.0, 3.25), (12.75, 14.0))      # bottom and top chords
LT_Z = ((3.0, 4.25), (11.75, 13.0))      # front and back chords
LT_POST_X = (7.0, 9.0)                   # a frame's posts, centred in its cell
LT_POST_Z = ((2.75, 4.75), (11.25, 13.25))


def light_chords(x0, x1):
    return [box(x0, y0, z0, x1, y1, z1, "#chord") for y0, y1 in LT_Y for z0, z1 in LT_Z]


def light_span():
    (b0, b1), (t0, t1) = LT_Y
    (f0, f1), (k0, k1) = LT_Z
    fz, kz = (f0 + f1) / 2, (k0 + k1) / 2
    by, ty = (b0 + b1) / 2, (t0 + t1) / 2
    full = [0, 0, 16, 16]
    return light_chords(0, 16) + [
        box(0, b1, fz, 16, t0, fz, "#lace", faces=("north", "south"), uv=full),
        box(0, b1, kz, 16, t0, kz, "#lace", faces=("north", "south"), uv=full),
        box(0, by, f1, 16, by, k0, "#brace", faces=("up", "down"), uv=full),
        box(0, ty, f1, 16, ty, k0, "#brace", faces=("up", "down"), uv=full),
    ]


def light_end(x0, x1):
    """The frame closing a span's end that nothing carries on from: a cantilever's tip."""
    (b0, b1), (t0, t1) = LT_Y
    (f0, f1), (k0, k1) = LT_Z
    return [box(x0, b1, f0, x1, t0, f1, "#plate"), box(x0, b1, k0, x1, t0, k1, "#plate"),
            box(x0, b0, f1, x1, b1, k0, "#plate"), box(x0, t0, f1, x1, t1, k0, "#plate")]


def light_join(neg):
    """A span's chords run on into the posts of the frame beside it."""
    reach = 16 - LT_POST_X[1]
    return light_chords(-reach, 0) if neg else light_chords(16, 16 + reach)


def light_frame():
    x0, x1 = LT_POST_X
    mid = (x0 + x1) / 2
    (a0, a1), (c0, c1) = LT_POST_Z
    return [box(x0, 0, a0, x1, 16, a1, "#chord"), box(x0, 0, c0, x1, 16, c1, "#chord"),
            box(mid, 0, a1, mid, 16, c0, "#ladder", faces=("east", "west"),
                uv=[0, 0, 16, 16])]


def light_frame_top():
    x0, x1 = LT_POST_X
    (a0, a1), (c0, c1) = LT_POST_Z
    return [box(x0 - 0.5, 15.5, a0 - 0.5, x1 + 0.5, 16.5, a1 + 0.5, "#plate"),
            box(x0 - 0.5, 15.5, c0 - 0.5, x1 + 0.5, 16.5, c1 + 0.5, "#plate"),
            box(x0 + 0.25, 14.0, a1, x1 - 0.25, 15.0, c0, "#chord")]


def light_frame_base():
    out = []
    for z0, z1 in LT_POST_Z:
        out.append(box(5.5, 0, z0 - 1.5, 10.5, 1, z1 + 1.5, "#plate"))
        for x in (5.75, 9.25):
            out.append(box(x, 1, (z0 + z1) / 2 - 0.5, x + 1, 2, (z0 + z1) / 2 + 0.5, "#plate"))
    return out


def _light_tex():
    return {"chord": TEX_REF % "truss_chord", "plate": TEX_REF % "truss_plate",
            "lace": TEX_REF % "truss_light_lace", "brace": TEX_REF % "truss_light_brace",
            "ladder": TEX_REF % "truss_ladder", "particle": TEX_REF % "truss_chord"}


def part_models():
    m = {}

    def model(els, lace="truss_lace", parent=None):
        out = {"textures": _tex(lace), "elements": els}
        if parent:
            out = {"parent": parent, **out}
        return out
    m[SMALL + "_chords"] = model(chords())
    m[SMALL + "_lacing"] = model(lacing())
    m[SMALL + "_end_bottom"] = model(end_frame(0, 1))
    m[SMALL + "_end_top"] = model(end_frame(15, 16))
    m[SMALL + "_base"] = model(base_plate())
    m[SMALL + "_inventory"] = model(chords() + lacing() + end_frame(0, 1) + end_frame(15, 16),
                                    parent="block/block")
    m[LARGE + "_chord"] = model(large_chord(), "truss_lace_wide")
    m[LARGE + "_lacing"] = model(large_lacing(), "truss_lace_wide")
    m[LARGE + "_end_bottom"] = model(large_end(0, 1), "truss_lace_wide")
    m[LARGE + "_end_top"] = model(large_end(15, 16), "truss_lace_wide")
    m[LARGE + "_base"] = model(large_base(), "truss_lace_wide")
    cw_tex = dict(_tex(), grate=TEX_REF % "truss_grate")
    m[CATWALK + "_body"] = {"textures": cw_tex, "elements": catwalk_body()}
    m[CATWALK + "_end_west"] = {"textures": cw_tex, "elements": catwalk_end(False)}
    m[CATWALK + "_end_east"] = {"textures": cw_tex, "elements": catwalk_end(True)}
    m[CATWALK + "_inventory"] = {"parent": "block/block", "textures": cw_tex,
                                 "elements": catwalk_body() + catwalk_end(False)
                                 + catwalk_end(True)}
    lt = _light_tex()
    for name, els in (("span", light_span()), ("end_neg", light_end(0, 1.25)),
                      ("end_pos", light_end(14.75, 16)), ("join_neg", light_join(True)),
                      ("join_pos", light_join(False)), ("frame", light_frame()),
                      ("frame_top", light_frame_top()), ("frame_base", light_frame_base())):
        m[LIGHT + "_" + name] = {"textures": lt, "elements": els}
    m[LIGHT + "_inventory"] = {"parent": "block/block", "textures": lt,
                               "elements": light_span() + light_end(0, 1.25)
                               + light_end(14.75, 16)}
    inv = model(large_inventory(), "truss_lace_wide", parent="block/block")
    inv["display"] = LARGE_DISPLAY
    m[LARGE + "_inventory"] = inv
    return m


# --------------------------------------------------------------------------------------------
# Blockstates
# --------------------------------------------------------------------------------------------

# How a part standing up is turned to an axis, and which world end its bottom and top become.
# x 90 rotates north to down, down to south: a part's bottom then faces south, the positive end
# along z. y 90 after that turns south to west: its bottom faces west, the negative end along x.
AXES = {
    "y": ({}, "neg", "pos"),
    "z": ({"x": 90}, "pos", "neg"),
    "x": ({"x": 90, "y": 90}, "neg", "pos"),
}


def small_blockstate():
    rules = []
    for axis, (rot, bottom, top) in AXES.items():
        def ap(name, rot=rot):
            return dict({"model": MODEL_REF % name}, **rot)
        w = {"axis": axis}
        rules += [{"when": dict(w), "apply": ap(SMALL + "_chords")},
                  {"when": dict(w), "apply": ap(SMALL + "_lacing")},
                  {"when": dict(w, **{"end_" + top: "true"}), "apply": ap(SMALL + "_end_top")}]
        if axis == "y":
            rules += [{"when": dict(w, end_neg="true", base="false"),
                       "apply": ap(SMALL + "_end_bottom")},
                      {"when": dict(w, base="true"), "apply": ap(SMALL + "_base")}]
        else:
            rules.append({"when": dict(w, **{"end_" + bottom: "true"}),
                          "apply": ap(SMALL + "_end_bottom")})
    return {"variants": {"inventory": {"model": MODEL_REF % (SMALL + "_inventory")}},
            "multipart": rules}


# A 2x2 quarter is drawn for the low-a, low-b corner standing up, a = x and b = z. Along z the
# section's plane is (x, y): x 90 carries the drawn low z to low y, x 270 to high y, and y 180
# after either mirrors x. Along x the plane is (z, y): y 90 carries the drawn low x to low z, y 270
# to high z. (a_high, b_high) -> extra rotation, per axis.
LARGE_CORNERS = {
    "y": {(False, False): {}, (True, False): {"y": 90}, (True, True): {"y": 180},
          (False, True): {"y": 270}},
    "z": {(False, False): {"x": 90}, (False, True): {"x": 270},
          (True, False): {"x": 90, "y": 180}, (True, True): {"x": 270, "y": 180}},
    "x": {(False, False): {"x": 90, "y": 90}, (False, True): {"x": 270, "y": 90},
          (True, False): {"x": 90, "y": 270}, (True, True): {"x": 270, "y": 270}},
}


def _large_ends(axis, rot):
    """Which world end a quarter's drawn bottom and top face, under its rotation."""
    if axis == "y":
        return "neg", "pos"
    x = rot.get("x", 0)
    y = rot.get("y", 0)
    if axis == "z":
        # x 90: bottom south (+z); x 270: bottom north (-z). y 180 mirrors z as well.
        bottom = "pos" if x == 90 else "neg"
        if y == 180:
            bottom = "neg" if bottom == "pos" else "pos"
    else:
        # laid along z then turned: y 90 sends +z to -x, y 270 sends +z to +x
        along_z = "pos" if x == 90 else "neg"
        if y == 90:
            bottom = "neg" if along_z == "pos" else "pos"
        else:
            bottom = along_z
    return bottom, ("neg" if bottom == "pos" else "pos")


def large_blockstate():
    rules = []
    for axis, corners in LARGE_CORNERS.items():
        for (a_high, b_high), rot in corners.items():
            def ap(name, rot=rot):
                return dict({"model": MODEL_REF % name}, **rot)
            w = {"axis": axis, "a_high": str(a_high).lower(), "b_high": str(b_high).lower()}
            bottom, top = _large_ends(axis, rot)
            rules += [{"when": dict(w), "apply": ap(LARGE + "_chord")},
                      {"when": dict(w), "apply": ap(LARGE + "_lacing")},
                      {"when": dict(w, **{"end_" + top: "true"}),
                       "apply": ap(LARGE + "_end_top")}]
            if axis == "y":
                rules += [{"when": dict(w, end_neg="true", base="false"),
                           "apply": ap(LARGE + "_end_bottom")},
                          {"when": dict(w, base="true"), "apply": ap(LARGE + "_base")}]
            else:
                rules.append({"when": dict(w, **{"end_" + bottom: "true"}),
                              "apply": ap(LARGE + "_end_bottom")})
    return {"variants": {"inventory": {"model": MODEL_REF % (LARGE + "_inventory")}},
            "multipart": rules}


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def catwalk_blockstate():
    rules = []
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        def ap(name, y=y):
            out = {"model": MODEL_REF % name}
            if y:
                out["y"] = y
            return out
        rules += [{"when": {"facing": facing}, "apply": ap(CATWALK + "_body")},
                  {"when": {"facing": facing, "end_west": "true"},
                   "apply": ap(CATWALK + "_end_west")},
                  {"when": {"facing": facing, "end_east": "true"},
                   "apply": ap(CATWALK + "_end_east")}]
    return {"variants": {"inventory": {"model": MODEL_REF % (CATWALK + "_inventory")}},
            "multipart": rules}


def light_blockstate():
    rules = []
    # y 90 turns the drawn +x end to +z, so a span's ends keep their names
    for kind, rot in (("span_x", {}), ("span_z", {"y": 90})):
        def ap(name, rot=rot):
            return dict({"model": MODEL_REF % (LIGHT + "_" + name)}, **rot)
        rules.append({"when": {"kind": kind}, "apply": ap("span")})
        for prop in ("end_neg", "end_pos", "join_neg", "join_pos"):
            rules.append({"when": {"kind": kind, prop: "true"}, "apply": ap(prop)})
    for kind, rot in (("frame_x", {}), ("frame_z", {"y": 90})):
        def ap(name, rot=rot):
            return dict({"model": MODEL_REF % (LIGHT + "_" + name)}, **rot)
        rules += [{"when": {"kind": kind}, "apply": ap("frame")},
                  {"when": {"kind": kind, "end_pos": "true"}, "apply": ap("frame_top")},
                  {"when": {"kind": kind, "base": "true"}, "apply": ap("frame_base")}]
    return {"variants": {"inventory": {"model": MODEL_REF % (LIGHT + "_inventory")}},
            "multipart": rules}


def _dump(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n", encoding="utf-8") as fh:
        fh.write(json.dumps(data, indent=2) + "\n")


def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    for name, draw in sorted(TEXTURES.items()):
        draw().save(os.path.join(tex_dir, name + ".png"))
        written.append(("tex", name + ".png"))
    for name, body in sorted(part_models().items()):
        _dump(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    _dump(os.path.join(state_dir, SMALL + ".json"), small_blockstate())
    _dump(os.path.join(state_dir, LARGE + ".json"), large_blockstate())
    _dump(os.path.join(state_dir, CATWALK + ".json"), catwalk_blockstate())
    _dump(os.path.join(state_dir, LIGHT + ".json"), light_blockstate())
    written += [("state", SMALL + ".json"), ("state", LARGE + ".json"),
                ("state", CATWALK + ".json"), ("state", LIGHT + ".json")]
    return written


def fragments():
    lines = []
    for lang_i, lang in enumerate(LANGS):
        lines.append("## " + lang)
        for reg in (SMALL, LARGE, LIGHT, CATWALK):
            lines.append("tile.%s.name=%s" % (reg, NAMES[reg][lang_i]))
        lines.append("")
    lines.append("## tab")
    lines.append('    initTabBlock(new BlockSignTruss("%s", false));' % SMALL)
    lines.append('    initTabBlock(new BlockSignTruss("%s", true));' % LARGE)
    lines.append('    initTabBlock(new BlockSignTrussLight("%s"));' % LIGHT)
    lines.append('    initTabBlock(new BlockTrussCatwalk("%s"));' % CATWALK)
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    args = ap.parse_args()
    if args.fragments:
        print(fragments())
        return 0
    roots = {"tex": TEX_DIR, "model": MODEL_DIR, "state": STATE_DIR}
    if not args.check:
        print("Wrote %d sign truss files" % len(write_all(TEX_DIR, MODEL_DIR, STATE_DIR)))
        return 0
    tmp = tempfile.mkdtemp(prefix="csm_truss_")
    try:
        t = {k: os.path.join(tmp, k) for k in roots}
        written = write_all(t["tex"], t["model"], t["state"])
        drift = [os.path.relpath(os.path.join(roots[k], f), REPO) for k, f in written
                 if not os.path.exists(os.path.join(roots[k], f))
                 or not filecmp.cmp(os.path.join(roots[k], f), os.path.join(t[k], f),
                                    shallow=False)]
        if drift:
            print("DRIFT: %d file(s) differ from the generator:" % len(drift))
            for p in drift:
                print("  " + p)
            return 1
        print("%d generated sign truss files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
