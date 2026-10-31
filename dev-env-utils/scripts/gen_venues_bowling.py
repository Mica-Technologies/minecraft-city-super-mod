#!/usr/bin/env python3
"""
gen_venues_bowling.py -- the Sports & Venues Bowling tab.

    py -3 dev-env-utils/scripts/gen_venues_bowling.py                  # write
    py -3 dev-env-utils/scripts/gen_venues_bowling.py --check          # fail on drift
    py -3 dev-env-utils/scripts/gen_venues_bowling.py --fragments      # the tab's lines
    py -3 dev-env-utils/scripts/gen_venues_bowling.py --layout [--lanes N]   # a centre (JSON)
    py -3 dev-env-utils/scripts/gen_venues_bowling.py --layout --commands X Y Z [--lanes N]
                                                                         # ... as commands

A ten-pin bowling centre at one block a metre. A lane is two blocks wide: its centre is the block
edge between its two columns, so a block is one half of it (the capping between lanes, the gutter
and half the bed; a real lane is 41.5 in with 9.25 in gutters, 1.52 m, and lanes stand about
1.75 m apart, so two blocks is 0.25 m generous). It is one metre a block along its length: 20
lane blocks from the foul line put the head pin at 60 ft (18.29 m) on the 19th and the pit at the
end of the 20th; the approach is 5 blocks (15 ft and a bit).

Lane and approach blocks read their place along the lane (BlockBowlingLane), so the markings fall
where they really do with no stored kind: the foul line, the dots at 7 ft, the targeting arrows
from 14 to 16 ft, the range finders at 40 ft, the pin deck with its ten spots, the tail plank and
the pit; the approach's locator dots at the foul line, 12 ft and 15 ft. Each is a 64 px picture
of the half lane, every board (39 across, 1.06 in each) where it falls. Then the pins (OBJ
lathes), the pinsetter and masking unit (four lit graphics), the ball return, the settee and its
curved corner, the scorer's console, the overhead scoring monitors (a renderer draws the scores),
the house ball rack, the shoe counter, cubbies and shoe spray.

Every model faces north with its back at +Z. Invented graphics only; no brand.
"""
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_venues_common as vc  # noqa: E402

GEN = "gen_venues_bowling.py"
C = vc.catalogue(GEN, "bowling")
T = C.T
M = C.M
box = vc.box

TAB = "Sports & Venues - Bowling"
FACINGS = vc.FACINGS
SUB = "venues/bowling"

# ------------------------------------------------------------------------------------------
# The lane, in metres and in pixels (16 a block)
# ------------------------------------------------------------------------------------------
IN = 0.0254
FT = 0.3048
LANE_W = 41.5 * IN                     # 1.054 m
GUTTER_W = 9.25 * IN                   # 0.235 m
BOARD = LANE_W / 39.0                  # 39 boards
LANE_EDGE = 16 - LANE_W / 2 * 16       # 7.568: the bed's outer edge in a left half
CAP_EDGE = LANE_EDGE - GUTTER_W * 16   # 3.808: the capping's edge
GUTTER_Y = 14.6                        # the gutter's floor
CAP_TOP = 16.25                        # the capping stands a little proud of the bed
PIT_Y = 9.0
TAIL_Z = 16 - 0.2 * 16                 # the tail plank: the deck ends 0.2 m into the last block
CURTAIN_Z = 16 - 0.75 * 16             # the pit curtain, 0.75 m into the last block
TEX = 64                               # texels a block for the lane pictures
PIN_SPACING = 12 * IN
PIN_ROW = 12 * IN * math.sqrt(3) / 2   # 10.39 in between rows
HEAD_PIN = 60 * FT                     # foul line to the head pin
ROWS_TO_HEAD = 18                      # the head pin is in the 19th lane block (index 18)

# where things fall along the lane, metres from the foul line
DOTS_AT = 7 * FT
ARROW_TIP_CENTRE = 16 * FT             # board 20's arrow; the others step 8 in nearer
ARROW_STEP = 8 * IN
ARROW_LEN = 6.5 * IN
ARROW_HALF_W = 0.3 / 16                # drawn 1.4 boards wide, to read
RANGE_FROM = 12.15
RANGE_TO = 12.95
APPROACH_DOTS = {"dots_foul": 3 * IN, "dots_12": 12 * FT, "dots_15": 15 * FT}

LANE_MARKS = ["none", "foul", "dots", "arrows", "range", "deck_front", "deck_back"]
APPROACH_MARKS = ["none", "dots_foul", "dots_12", "dots_15"]
FINISHES = {"maple": False, "synthetic": True}


def board_x(k):
    """The x (pixels, a left half) of board k's centre: board 20 is on the lane's centre, the
    block's east edge; board 1 is at the lane's edge."""
    return 16 - (20 - k) * BOARD * 16


def pin_spots():
    """The ten pins' spots, (x, d): x in a left half's pixels (16 the lane's centre), d metres
    from the foul line."""
    out = []
    for row in range(4):
        d = HEAD_PIN + row * PIN_ROW
        for i in range(row + 1):
            off = (i - row / 2.0) * PIN_SPACING
            out.append((16 + off * 16, d))
    return out


# ------------------------------------------------------------------------------------------
# Palette
# ------------------------------------------------------------------------------------------
MAPLE = (222, 186, 134)
MAPLE_APPROACH = (230, 198, 150)
MAPLE_DECK = (234, 206, 162)
SYN = (232, 210, 170)
SYN_APPROACH = (236, 216, 180)
SYN_DECK = (238, 234, 224)
CAPPING = {"maple": (64, 40, 34), "synthetic": (34, 34, 40)}
GUTTER = (52, 54, 60)
MARK = {"maple": (40, 24, 20), "synthetic": (24, 26, 36)}
SPOT = {"maple": (96, 60, 40), "synthetic": (120, 40, 40)}
CHROME = (206, 210, 214)
BLACK = (22, 22, 24)
STEEL_DARK = (60, 62, 66)


def rgba(c, a=255):
    return tuple(int(v) for v in c[:3]) + (a,)


def ys(y):
    return {"y": y} if y else {}


def icon_display(scale, dy=0.0, rot_y=225):
    return {"gui": {"rotation": [30, rot_y, 0], "translation": [0, dy, 0],
                    "scale": [scale, scale, scale]},
            "ground": {"translation": [0, 3, 0], "scale": [scale * 0.5] * 3},
            "fixed": {"translation": [0, dy, 0], "scale": [scale, scale, scale]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                      "scale": [scale * 0.6] * 3},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [scale * 0.7] * 3}}


def item_of(model_name, scale=0.625, dy=0.0):
    return {"parent": "csm:block/%s/%s" % (SUB, model_name), "display": icon_display(scale, dy)}


# ------------------------------------------------------------------------------------------
# The lane pictures: one 64 px top a marking, drawn three samples a texel each way
# ------------------------------------------------------------------------------------------
SS = 3


def _samples():
    n = TEX * SS
    c = (np.arange(n) + 0.5) / (n / 16.0)
    X, Z = np.meshgrid(c, c)
    return X, Z


def _board_index(X):
    """The board each sample is on, counted from the centre board (0 = board 20)."""
    o = (16 - X) / 16.0
    return np.floor((o + BOARD / 2) / BOARD).astype(int)


def _hash(j, salt):
    j = np.asarray(j)
    h = (j * 2654435761 + salt * 40503) & 0xFFFF
    return h / 65535.0 * 2 - 1


def _wood(X, Z, colour, contrast, grain, salt):
    """Boards along z: a tone a board, a seam between boards, a grain that repeats every block
    (so plain blocks laid end to end meet unseen)."""
    j = _board_index(X)
    base = np.array(colour, dtype=float)
    k = 1.0 + contrast * _hash(j, salt)
    t = Z / 16.0
    g = (grain * np.sin(2 * np.pi * (2 * t + (_hash(j, salt + 7) + 1) * 0.5))
         + grain * 0.6 * np.sin(2 * np.pi * (5 * t + (_hash(j, salt + 11) + 1) * 0.5)))
    k = k + g
    o = (16 - X) / 16.0
    frac = np.mod(o + BOARD / 2, BOARD) / BOARD
    seam = (frac < 0.07) | (frac > 0.93)
    k = np.where(seam, k * 0.86, k)
    return base[None, None, :] * k[..., None]


def _gutter(X):
    t = np.clip((X - CAP_EDGE) / (LANE_EDGE - CAP_EDGE), 0, 1)
    k = 0.6 + 0.42 * np.sin(np.pi * t)
    return np.array(GUTTER, dtype=float)[None, None, :] * k[..., None]


def _flat(colour, shape):
    return np.broadcast_to(np.array(colour, dtype=float), shape + (3,)).copy()


def _disc(img, X, Z, cx, cz, r, colour):
    m = (X - cx) ** 2 + (Z - cz) ** 2 <= r * r
    img[m] = colour


def _tri(img, X, Z, p, q, r, colour):
    def side(a, b):
        return (b[0] - a[0]) * (Z - a[1]) - (b[1] - a[1]) * (X - a[0])
    s1, s2, s3 = side(p, q), side(q, r), side(r, p)
    m = ((s1 >= 0) & (s2 >= 0) & (s3 >= 0)) | ((s1 <= 0) & (s2 <= 0) & (s3 <= 0))
    img[m] = colour


def _z_of(d, row):
    """The z (pixels) at d metres from the foul line, in lane block ``row`` (row 0 the first):
    the foul line is the south edge of row 0, the pins to the north."""
    return 16 - (d - row) * 16


def _down(arr):
    n = TEX
    a = arr.reshape(n, SS, n, SS, 3).mean(axis=(1, 3))
    return Image.fromarray(np.clip(np.round(a), 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def lane_top(finish, mark):
    X, Z = _samples()
    syn = FINISHES[finish]
    deck = mark in ("deck_front", "deck_back")
    if deck:
        wood = _wood(X, Z, SYN_DECK if syn else MAPLE_DECK, 0.02 if syn else 0.05,
                     0.006 if syn else 0.018, 31)
    else:
        wood = _wood(X, Z, SYN if syn else MAPLE, 0.035 if syn else 0.07,
                     0.01 if syn else 0.03, 17)
    img = wood
    g = X < LANE_EDGE
    img[g] = _gutter(X)[g]
    cap = X < CAP_EDGE
    img[cap] = _flat(CAPPING[finish], X.shape)[cap] * (1 + 0.04 * np.sin(Z * 0.9))[cap][:, None]
    ink = np.array(MARK[finish], dtype=float)
    bed = X >= LANE_EDGE
    if mark == "foul":
        img[Z >= 16 - 0.45] = ink
    elif mark == "dots":
        z = _z_of(DOTS_AT, 2)
        for k in (3, 5, 8, 11, 14):
            _disc(img, X, Z, board_x(k), z, 0.34, ink)
    elif mark == "arrows":
        for k in (5, 10, 15, 20):
            tip = ARROW_TIP_CENTRE - (20 - k) / 5.0 * ARROW_STEP
            zt = _z_of(tip, 4)
            zb = _z_of(tip - ARROW_LEN, 4)
            x = board_x(k)
            hw = ARROW_HALF_W * 16
            _tri(img, X, Z, (x, zt), (x - hw, zb), (x + hw, zb), ink)
    elif mark == "range":
        x = board_x(10)
        m = (np.abs(X - x) <= 0.13) & (Z >= _z_of(RANGE_TO, 12)) & (Z <= _z_of(RANGE_FROM, 12))
        img[m] = ink
    elif deck:
        row = 18 if mark == "deck_front" else 19
        spot = np.array(SPOT[finish], dtype=float)
        for (x, d) in pin_spots():
            z = _z_of(d, row)
            _disc(img, X, Z, x, z, 0.47, spot)
            _disc(img, X, Z, x, z, 0.3, spot * 0.75)
        if mark == "deck_back":
            tail = (Z >= TAIL_Z) & (Z <= TAIL_Z + 0.45) & bed
            img[tail] = img[tail] * 0.7
    return _down(img)


def approach_top(finish, mark):
    X, Z = _samples()
    syn = FINISHES[finish]
    img = _wood(X, Z, SYN_APPROACH if syn else MAPLE_APPROACH, 0.03 if syn else 0.06,
                0.01 if syn else 0.025, 53)
    cap = X < CAP_EDGE
    img[cap] = _flat(CAPPING[finish], X.shape)[cap]
    ink = np.array(MARK[finish], dtype=float)
    if mark in APPROACH_DOTS:
        behind = APPROACH_DOTS[mark]
        row = int(math.floor(behind))
        z = (behind - row) * 16                 # the foul line is the north edge of the first
        for k in (3, 5, 8, 11, 14):
            _disc(img, X, Z, board_x(k), z, 0.38, ink)
        _disc(img, X, Z, 16.0, z, 0.5, ink)     # board 20, on the lane's centre
    return _down(img)


for _fin in FINISHES:
    for _m in LANE_MARKS:
        C.textures["lane_%s_%s" % (_fin, _m)] = (lambda f=_fin, m=_m: lane_top(f, m))
    for _m in APPROACH_MARKS:
        C.textures["approach_%s_%s" % (_fin, _m)] = (lambda f=_fin, m=_m: approach_top(f, m))


@C.texture("lane_side")
def _lane_side():
    img = vc.swatch((70, 56, 46), 901, grain=3)
    for y in (5, 11):
        vc.rect(img, 0, y, 16, y + 1, (58, 46, 38))
    return img


@C.texture("lane_edge")
def _lane_edge():
    return vc.swatch((198, 164, 118), 902, grain=4)


@C.texture("capping_maple")
def _cap_maple():
    return vc.swatch(CAPPING["maple"], 903, grain=2)


@C.texture("capping_synthetic")
def _cap_syn():
    return vc.swatch(CAPPING["synthetic"], 904, grain=2)


@C.texture("pit")
def _pit():
    img = vc.swatch((30, 30, 32), 905, grain=3)
    for x in range(0, 16, 4):
        vc.rect(img, x, 0, x + 1, 16, (22, 22, 24))
    return img


@C.texture("curtain")
def _curtain():
    img = vc.swatch((14, 14, 16), 906, grain=2)
    for x in range(1, 16, 3):
        vc.rect(img, x, 0, x + 1, 16, (24, 24, 26))
    return img


@C.texture("bumper")
def _bumper():
    return vc.swatch((40, 104, 196), 907, grain=2)


# ------------------------------------------------------------------------------------------
# Lane models: a left half drawn, the right half its mirror (the picture flipped with it)
# ------------------------------------------------------------------------------------------
def _uv(name, x0, y0, z0, x1, y1, z1):
    return {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
            "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
            "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}[name]


def el(frm, to, faces, right=False):
    """An element of a left half; ``faces`` name -> (texture, cull or None). With ``right`` it is
    mirrored across x = 8 and every face's u flipped, so the right half shows the left's picture
    mirrored."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    out = {}
    for name, (tex, cull) in faces.items():
        u = _uv(name, x0, y0, z0, x1, y1, z1)
        u = [round(min(16, max(0, v)), 3) for v in u]
        if right and name in ("north", "south", "up", "down"):
            u = [u[2], u[1], u[0], u[3]]
        f = {"texture": "#" + tex, "uv": u}
        if cull:
            f["cullface"] = cull
        out[name] = f
    if right:
        x0, x1 = 16 - x1, 16 - x0
        swap = {"east": "west", "west": "east"}
        out = {swap.get(k, k): v for k, v in out.items()}
        for v in out.values():
            if v.get("cullface") in swap:
                v["cullface"] = swap[v["cullface"]]
    return {"from": [round(x0, 3), round(y0, 3), round(z0, 3)],
            "to": [round(x1, 3), round(y1, 3), round(z1, 3)], "faces": out}


def lane_geometry(right, deck_back=False):
    E = []

    def add(frm, to, faces):
        E.append(el(frm, to, faces, right))
    sides = {"north": ("side", "north"), "south": ("side", "south"), "east": ("side", "east"),
             "west": ("side", "west"), "down": ("side", "down")}
    if not deck_back:
        add([0, 0, 0], [16, GUTTER_Y, 16], sides)
        add([0, GUTTER_Y, 0], [CAP_EDGE, CAP_TOP, 16],
            {"up": ("top", None), "east": ("cap", None), "west": ("cap", "west"),
             "north": ("cap", "north"), "south": ("cap", "south")})
        add([CAP_EDGE, GUTTER_Y - 0.6, 0], [LANE_EDGE, GUTTER_Y, 16], {"up": ("top", None)})
        add([LANE_EDGE, GUTTER_Y, 0], [16, 16, 16],
            {"up": ("top", None), "west": ("edge", None), "east": ("edge", "east"),
             "north": ("edge", "north"), "south": ("edge", "south")})
        return E
    # the last block: the deck to the tail plank, the pit, the wall under the curtain
    add([0, 0, 0], [16, PIT_Y, 16], sides)
    add([0, PIT_Y, 0], [CAP_EDGE, GUTTER_Y, 16],
        {"east": ("side", None), "west": ("side", "west"), "north": ("side", "north"),
         "south": ("side", "south")})
    add([0, GUTTER_Y, 0], [CAP_EDGE, CAP_TOP, 16],
        {"up": ("top", None), "east": ("cap", None), "west": ("cap", "west"),
         "north": ("cap", "north"), "south": ("cap", "south")})
    add([CAP_EDGE, PIT_Y - 0.5, CURTAIN_Z], [16, PIT_Y, TAIL_Z], {"up": ("pit", None)})
    add([CAP_EDGE, PIT_Y, TAIL_Z], [LANE_EDGE, GUTTER_Y, 16],
        {"up": ("top", None), "north": ("side", None), "south": ("side", "south")})
    add([LANE_EDGE, PIT_Y, TAIL_Z], [16, 16, 16],
        {"up": ("top", None), "north": ("edge", None), "west": ("edge", None),
         "east": ("edge", "east"), "south": ("edge", "south")})
    add([CAP_EDGE, PIT_Y, 0], [16, 16, CURTAIN_Z],
        {"up": ("curtain", None), "south": ("curtain", None), "north": ("curtain", "north"),
         "east": ("curtain", "east")})
    return E


def approach_geometry(right):
    return [el([0, 0, 0], [16, 16, 16],
               {"up": ("top", None), "north": ("side", "north"), "south": ("side", "south"),
                "east": ("side", "east"), "west": ("side", "west"), "down": ("side", "down")},
               right)]


def bumper_geometry(right):
    # a padded rail about 4 in over the bed, standing in the gutter against its outer wall
    return [el([CAP_EDGE + 0.2, GUTTER_Y, 0], [CAP_EDGE + 1.6, 17.0, 16],
               {"up": ("b", None), "east": ("b", None)}, right)]


SIDE_NAMES = {False: "l", True: "r"}


def lane_models():
    models = {}
    for right in (False, True):
        s = SIDE_NAMES[right]
        models["lane_geo_" + s] = {"parent": "block/block", "textures": {},
                                   "elements": lane_geometry(right)}
        models["deck_back_geo_" + s] = {"parent": "block/block", "textures": {},
                                        "elements": lane_geometry(right, True)}
        models["approach_geo_" + s] = {"parent": "block/block", "textures": {},
                                       "elements": approach_geometry(right)}
        models["bumper_" + s] = vc.model({"b": T("bumper"), "particle": T("bumper")},
                                         bumper_geometry(right))
    return models


def lane_child(fin, mark, right):
    geo = "deck_back_geo_" if mark == "deck_back" else "lane_geo_"
    tex = {"top": T("lane_%s_%s" % (fin, mark)), "side": T("lane_side"), "edge": T("lane_edge"),
           "cap": T("capping_" + fin), "pit": T("pit"), "curtain": T("curtain"),
           "particle": T("lane_%s_none" % fin)}
    return {"parent": "csm:block/%s/%s%s" % (SUB, geo, SIDE_NAMES[right]), "textures": tex}


def approach_child(fin, mark, right):
    return {"parent": "csm:block/%s/approach_geo_%s" % (SUB, SIDE_NAMES[right]),
            "textures": {"top": T("approach_%s_%s" % (fin, mark)), "side": T("lane_side"),
                         "particle": T("approach_%s_none" % fin)}}


def lane_state(fin, approach):
    marks = APPROACH_MARKS if approach else LANE_MARKS
    prefix = "approach" if approach else "lane"
    rules = []
    for facing, y in FACINGS:
        for right in (False, True):
            for i, m in enumerate(marks):
                rules.append({"when": {"facing": facing, "right": str(right).lower(),
                                       "mark": str(i)},
                              "apply": dict({"model": M("%s_%s_%s_%s" % (prefix, fin, m,
                                                                         SIDE_NAMES[right]))},
                                            **ys(y))})
            if not approach:
                rules.append({"when": {"facing": facing, "right": str(right).lower(),
                                       "bumpers": "true", "mark": "0|1|2|3|4"},
                              "apply": dict({"model": M("bumper_" + SIDE_NAMES[right])}, **ys(y))})
    return {"multipart": rules}


_LANE_NAMES = {
    ("lane", "maple"): ("Bowling Lane (Maple)", "Bowlingbahn (Ahorn)", "Pista de Bolos (Arce)",
                        "Bowlingbana (Lönn)"),
    ("lane", "synthetic"): ("Bowling Lane (Synthetic)", "Bowlingbahn (Kunststoff)",
                            "Pista de Bolos (Sintética)", "Bowlingbana (Syntet)"),
    ("approach", "maple"): ("Bowling Approach (Maple)", "Bowling-Anlauf (Ahorn)",
                            "Zona de Aproximación (Arce)", "Bowlingansats (Lönn)"),
    ("approach", "synthetic"): ("Bowling Approach (Synthetic)", "Bowling-Anlauf (Kunststoff)",
                                "Zona de Aproximación (Sintética)", "Bowlingansats (Syntet)"),
}

_first = True
for _kind in ("lane", "approach"):
    for _fin in FINISHES:
        _reg = "bowling_%s%s" % (_kind, "_synthetic" if _fin == "synthetic" else "")
        _models = lane_models() if _first else {}
        _first = False
        _marks = APPROACH_MARKS if _kind == "approach" else LANE_MARKS
        for _r in (False, True):
            for _m in _marks:
                _name = "%s_%s_%s_%s" % (_kind, _fin, _m, SIDE_NAMES[_r])
                _models[_name] = (approach_child if _kind == "approach" else lane_child)(
                    _fin, _m, _r)
        C.add(_reg, 'new BlockBowlingLane("%s", BlockBowlingLane.Kind.%s, %s)' % (
            _reg, _kind.upper(), "true" if _fin == "synthetic" else "false"),
            _LANE_NAMES[(_kind, _fin)], _models, lane_state(_fin, _kind == "approach"),
            item={"parent": "csm:block/%s/%s_%s_none_l" % (SUB, _kind, _fin),
                  "display": icon_display(0.625)}, tab=TAB)


# ------------------------------------------------------------------------------------------
# OBJ helpers
# ------------------------------------------------------------------------------------------
def write_obj(name, mtl, polys):
    o = vc.Obj(GEN, mtl, name)
    for p in polys:
        o.add(p)
    C.extra["models/block/%s/%s.obj" % (SUB, name)] = o.text()


def mtl(name, mats):
    C.extra["models/block/%s/%s.mtl" % (SUB, name)] = vc.mtl_text(GEN, mats)


def lathe(profile, sides, mat, place, cap_top=True, cap_bottom=True):
    """A turned shape: ``profile`` (height, radius) pairs bottom to top in its own frame (axis up
    y), ``place(lx, ly, lz)`` taking a point of it to the model. v runs down the texture from the
    top of the profile; u round it."""
    h0, h1 = profile[0][0], profile[-1][0]
    span = (h1 - h0) or 1.0
    out = []

    def ring(i, k):
        h, r = profile[i]
        a = 2 * math.pi * k / sides
        return place(r * math.cos(a), h, r * math.sin(a))

    def axis_at(h):
        return place(0, h, 0)
    for i in range(len(profile) - 1):
        for k in range(sides):
            pts = [ring(i, k), ring(i, k + 1), ring(i + 1, k + 1), ring(i + 1, k)]
            uvs = [(k / float(sides), (h1 - profile[i][0]) / span),
                   ((k + 1) / float(sides), (h1 - profile[i][0]) / span),
                   ((k + 1) / float(sides), (h1 - profile[i + 1][0]) / span),
                   (k / float(sides), (h1 - profile[i + 1][0]) / span)]
            if profile[i][1] < 1e-6 and profile[i + 1][1] < 1e-6:
                continue
            cen = tuple(sum(p[c] for p in pts) / 4.0 for c in range(3))
            hm = (profile[i][0] + profile[i + 1][0]) / 2.0
            ax = axis_at(hm)
            outward = (cen[0] - ax[0], cen[1] - ax[1], cen[2] - ax[2])
            # drop the degenerate corner of a segment that closes to a point
            if profile[i][1] < 1e-6:
                pts, uvs = [pts[0], pts[2], pts[3]], [uvs[0], uvs[2], uvs[3]]
            elif profile[i + 1][1] < 1e-6:
                pts, uvs = pts[:3], uvs[:3]
            out.append(vc.Poly(pts, mat, outward, uvs))
    if cap_bottom and profile[0][1] > 1e-6:
        pts = [ring(0, k) for k in range(sides)]
        a0, a1 = axis_at(h0), axis_at(h0 + 1)
        out.append(vc.Poly(pts, mat, (a0[0] - a1[0], a0[1] - a1[1], a0[2] - a1[2]),
                           [(0.5, 0.98)] * sides))
    if cap_top and profile[-1][1] > 1e-6:
        pts = [ring(len(profile) - 1, k) for k in range(sides)]
        a0, a1 = axis_at(h1), axis_at(h1 - 1)
        out.append(vc.Poly(pts, mat, (a0[0] - a1[0], a0[1] - a1[1], a0[2] - a1[2]),
                           [(0.5, 0.02)] * sides))
    return out


def sphere(cx, cy, cz, r, mat, lon=8, lat=4):
    """A low ball: ``lat`` bands of ``lon`` faces, the polar ones triangles."""
    out = []

    def p(i, k):
        phi = math.pi * i / lat - math.pi / 2
        th = 2 * math.pi * k / lon
        return (cx + r * math.cos(phi) * math.cos(th), cy + r * math.sin(phi),
                cz + r * math.cos(phi) * math.sin(th))
    for i in range(lat):
        for k in range(lon):
            a, b, c, d = p(i, k), p(i, k + 1), p(i + 1, k + 1), p(i + 1, k)
            uv = [(k / float(lon), 1 - i / float(lat)), ((k + 1) / float(lon), 1 - i / float(lat)),
                  ((k + 1) / float(lon), 1 - (i + 1) / float(lat)),
                  (k / float(lon), 1 - (i + 1) / float(lat))]
            pts = [a, b, c, d]
            if i == 0:
                pts, uv = [a, c, d], [uv[0], uv[2], uv[3]]
            elif i == lat - 1:
                pts, uv = [a, b, c], uv[:3]
            cen = tuple(sum(q[j] for q in pts) / len(pts) for j in range(3))
            out.append(vc.Poly(pts, mat, (cen[0] - cx, cen[1] - cy, cen[2] - cz), uv))
    return out


BALL_R = 8.5 * IN / 2 * 16             # 1.73 px
BALL_COLOURS = {"ball_red": (196, 36, 40), "ball_blue": (30, 74, 196),
                "ball_green": (40, 150, 70), "ball_purple": (120, 50, 170),
                "ball_black": (36, 36, 40), "ball_orange": (232, 120, 30),
                "ball_pink": (226, 90, 160), "ball_teal": (30, 160, 160)}


def _ball_tex(colour, seed):
    def draw():
        rng = random.Random(seed)
        img = Image.new("RGBA", (16, 16))
        px = img.load()
        for y in range(16):
            for x in range(16):
                swirl = math.sin(x * 0.7 + math.sin(y * 0.5 + seed) * 2.2)
                k = 0.86 + 0.2 * swirl + rng.uniform(-0.03, 0.03)
                px[x, y] = rgba(vc.shade(colour, k))
        # the finger holes and a shine
        for (hx, hy) in ((6, 6), (9, 6), (7, 9)):
            px[hx, hy] = rgba((14, 14, 16))
        px[3, 3] = rgba(vc.shade(colour, 1.6))
        return img
    return draw


for _i, (_n, _c) in enumerate(sorted(BALL_COLOURS.items())):
    C.textures[_n] = _ball_tex(_c, 300 + _i)


# ------------------------------------------------------------------------------------------
# The pins: ten lathed pins, standing or knocked down
# ------------------------------------------------------------------------------------------
PIN_PROFILE_IN = [(0, 1.0), (1.4, 2.0), (4.5, 2.38), (7.0, 1.95), (9.5, 0.92), (11.4, 1.1),
                  (13.2, 1.27), (14.5, 0.86), (15.0, 0.0)]
PIN_PROFILE = [(h * IN * 16, r * IN * 16) for h, r in PIN_PROFILE_IN]
PIN_H = PIN_PROFILE[-1][0]
PIN_BELLY = max(r for _h, r in PIN_PROFILE)


@C.texture("pin")
def _pin():
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    rng = random.Random(910)
    for y in range(32):
        for x in range(32):
            px[x, y] = rgba(vc.shade((242, 240, 234), 1 + rng.uniform(-0.02, 0.02)))
    # two red neck stripes, about 10 and 10.8 in up a 15 in pin (v runs down from the crown)
    for h in (10.1, 9.3):
        row = int(round((1 - h / 15.0) * 32))
        vc.rect(img, 0, row, 32, row + 1, (200, 30, 36))
    return img


def standing_pins(dx):
    out = []
    for (x, d) in pin_spots():
        z = _z_of(d, ROWS_TO_HEAD)
        out += lathe(PIN_PROFILE, 8, "pin",
                     lambda lx, ly, lz, x=x, z=z: (x + dx + lx, ly, z + lz))
    return out


FALLEN = [(10.6, 9.6, 30), (14.2, 12.6, 160), (18.5, 10.4, 75), (21.4, 6.2, 120),
          (12.0, 4.6, 205), (16.6, 6.8, 340), (9.0, 0.4, 100), (19.4, 1.6, 15),
          (13.6, -1.4, 255), (22.6, -0.6, 140)]


def fallen_pins(dx):
    out = []
    for (x, z, ang) in FALLEN:
        a = math.radians(ang)
        ax = (math.cos(a), 0.0, math.sin(a))
        side = (-math.sin(a), 0.0, math.cos(a))
        y = PIN_BELLY * 0.92
        bx, bz = x + dx - ax[0] * PIN_H / 2, z - ax[2] * PIN_H / 2

        def place(lx, ly, lz, bx=bx, bz=bz, ax=ax, side=side, y=y):
            # the pin's own axis (ly) laid along ``ax``; its round section in side and up
            return (bx + ax[0] * ly + side[0] * lx, y + lz, bz + ax[2] * ly + side[2] * lx)
        out += lathe(PIN_PROFILE, 8, "pin", place)
    return out


mtl("pins", {"pin": T("pin")})
for _r, _dx in ((False, 0.0), (True, -16.0)):
    write_obj("pins_standing_" + SIDE_NAMES[_r], "pins", standing_pins(_dx))
    write_obj("pins_fallen_" + SIDE_NAMES[_r], "pins", fallen_pins(_dx))

_PIN_ICON = vc.model({"p": T("pin"), "particle": T("pin")}, [
    box([7, 0, 7], [9, 6, 9], "p"), box([6.5, 1, 6.5], [9.5, 3.5, 9.5], "p"),
    box([7.3, 6, 7.3], [8.7, 7.5, 8.7], "p")])


def pins_state():
    def choose(v):
        r = v["right"] == "true"
        f = v["fallen"] == "true"
        y = dict(FACINGS)[v["facing"]]
        d = {"model": M("pins_%s_%s" % ("fallen" if f else "standing", SIDE_NAMES[r])) + ".obj"}
        d.update(ys(y))
        return d
    st = vc.keyed_state([("facing", [f for f, _y in FACINGS]), ("fallen", ["false", "true"]),
                         ("right", ["false", "true"])], choose,
                        inventory={"model": M("pins_item")})
    return st


C.add("bowling_pins", 'new BlockBowlingPins("bowling_pins")',
      ("Bowling Pins", "Bowling-Pins", "Bolos", "Bowlingkäglor"),
      {"pins_item": _PIN_ICON}, pins_state(), item=item_of("pins_item", 1.4, -1), tab=TAB)


# ------------------------------------------------------------------------------------------
# Units (BlockVenueUnit): footprint from boxes, as the other venue generators write it
# ------------------------------------------------------------------------------------------
def unit_cells(boxes_px):
    def axis(lo, hi, tol=6.0):
        out = []
        for c in range(int(math.floor(lo / 16.0)), int(math.ceil(hi / 16.0))):
            ov = min(hi, 16.0 * (c + 1)) - max(lo, 16.0 * c)
            if ov >= min(tol, hi - lo - 1e-6):
                out.append(c)
        return out
    cells = set()
    for b in boxes_px:
        for cx in axis(b[0], b[3]):
            for cy in axis(b[1], b[4]):
                for cz in axis(b[2], b[5]):
                    cells.add((cx, cy, cz))
    cells.discard((0, 0, 0))
    return sorted(cells, key=lambda c: (c[1], c[2], c[0]))


def check_connected(reg, cells):
    todo, seen = [(0, 0, 0)], {(0, 0, 0)}
    cs = set(cells)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in cs and n not in seen:
                seen.add(n)
                todo.append(n)
    lost = cs - seen
    if lost:
        raise SystemExit("%s: unit cells not connected to the master: %s" % (reg, sorted(lost)))


def unit_java(cls, reg, boxes_px):
    check_connected(reg, unit_cells(boxes_px))
    flat_c = ", ".join("%d, %d, %d" % c for c in unit_cells(boxes_px))
    flat_b = ", ".join(", ".join("%d" % int(round(v)) for v in b) for b in boxes_px)
    return 'new %s("%s", new int[]{%s}, new int[]{%s})' % (cls, reg, flat_c, flat_b)


def quad_n(x0, x1, y0, y1, z, mat, uv=None):
    """A north-facing quad at z (u from the reader's left, v down)."""
    pts = [(x1, y0, z), (x0, y0, z), (x0, y1, z), (x1, y1, z)]
    return vc.Poly(pts, mat, (0, 0, -1), uv or [(0, 1), (1, 1), (1, 0), (0, 0)])


# ------------------------------------------------------------------------------------------
# The pinsetter and masking unit: a pair of lanes' end (BlockBowlingPinsetter)
# ------------------------------------------------------------------------------------------
# Master frame (pixels): the model faces north, to the bowlers; x from the master's west edge
# across both lanes (0..64: the reader's right lane is x 0..32, its centre at 16); z = 0 the
# start of the last lane block (19 m), so d metres from the foul line is z = (d - 19) * 16.
def _dz(d):
    return (d - 19) * 16


PS_CAB = (0, 64, 0, 35, _dz(19.75), _dz(21.75))
PS_PANEL = (0, 64, 16, 32, _dz(18.2), _dz(18.5))
PS_TOP = (0, 64, 32, 33, _dz(18.2), _dz(19.75))
PS_KICK = [(0.2, 1.8), (31.2, 32.8), (62.2, 63.8)]
MASK_DESIGNS = ["starburst", "cosmic", "retro", "neon"]


def pinsetter_polys(design):
    P = []
    x0, x1, y0, y1, z0, z1 = PS_CAB
    P += vc.obox(x0, x1, y0, y1, z0, z1, "cab", "sewu")
    P += vc.obox(x0, x1, y0, y1, z0, z1, "curtain", "n")
    x0, x1, y0, y1, z0, z1 = PS_TOP
    P += vc.obox(x0, x1, y0, y1, z0, z1, "cab", "newu")
    x0, x1, y0, y1, z0, z1 = PS_PANEL
    P += vc.obox(x0, x1, y0, y1, z0, z1, "frame", "sew")
    P += vc.obox(x0, x1, y0 - 0.6, y0, z0, z1, "lamp", "d")
    P += vc.obox(x0, x1, y0 - 0.6, y0, z0, z1, "frame", "nsew")
    for i in range(4):
        # sprite i is the i-th block from the reader's left: the panel's high x end first
        xa, xb = 64 - 16 * (i + 1), 64 - 16 * i
        P.append(quad_n(xa, xb, y0, y1, z0, "m%d" % i))
    for (a, b) in PS_KICK:
        P += vc.obox(a, b, 0, 16, _dz(17.9), _dz(19.75), "kick", "nsewu")
    return P


for _d, _name in enumerate(MASK_DESIGNS):
    mtl("pinsetter_%d" % _d, dict({"cab": T("pinsetter_cab"), "curtain": T("curtain"),
                                   "frame": T("mask_frame"), "lamp": T("mask_lamp"),
                                   "kick": T("kickback")},
                                  **{"m%d" % i: T("mask_%s_%d" % (_name, i)) for i in range(4)}))
    write_obj("pinsetter_%d" % _d, "pinsetter_%d" % _d, pinsetter_polys(_d))


@C.texture("pinsetter_cab")
def _ps_cab():
    img = vc.swatch((44, 46, 50), 920, grain=2)
    for y in (0, 8):
        vc.rect(img, 0, y, 16, y + 1, (34, 36, 40))
    return img


@C.texture("mask_frame")
def _mask_frame():
    return vc.brushed((150, 154, 160), 921)


@C.texture("mask_lamp")
def _mask_lamp():
    return vc.swatch((252, 248, 236), 922, grain=1)


@C.texture("kickback")
def _kickback():
    img = vc.swatch((52, 40, 36), 923, grain=2)
    vc.rect(img, 0, 2, 16, 3, (190, 160, 60))
    return img


def _design(name):
    """A masking graphic, 256 x 64: four blocks across one block."""
    W, H = 256, 64
    rng = random.Random(MASK_DESIGNS.index(name) * 97 + 5)
    img = Image.new("RGBA", (W, H))
    px = img.load()
    if name == "starburst":
        for y in range(H):
            for x in range(W):
                t = y / float(H)
                c = (20 + 30 * t, 30 + 20 * t, 90 + 60 * t)
                ang = math.atan2(y - 34, x - 128)
                if int((ang + math.pi) / (2 * math.pi) * 28) % 2 == 0:
                    c = vc.shade(c, 1.35)
                px[x, y] = rgba(c)
        for y in range(H):
            for x in range(128 - 40, 128 + 40):
                dd = math.sqrt((x - 128) ** 2 + ((y - 34) * 1.3) ** 2)
                if dd <= 30:
                    px[x, y] = rgba((255, 150 + 90 * (1 - dd / 30.0), 40 + 60 * (1 - dd / 30.0)))
        _pin_shape(img, 112, 14, (246, 246, 240))
        _pin_shape(img, 136, 14, (246, 246, 240))
        _ball_shape(img, 128, 44, 9, (190, 30, 40))
        vc.draw_text_centred(img, "STRIKE", 48, 24, (255, 222, 60), scale=3)
        vc.draw_text_centred(img, "STRIKE", 208, 24, (255, 222, 60), scale=3)
    elif name == "cosmic":
        for y in range(H):
            for x in range(W):
                px[x, y] = rgba((8 + 10 * y / H, 6, 24 + 20 * y / H))
        for _ in range(260):
            x, y = rng.randrange(W), rng.randrange(H)
            px[x, y] = rgba(rng.choice([(255, 255, 255), (160, 220, 255), (255, 180, 240)]))
        for (cx, cy, r, col) in ((40, 30, 13, (255, 60, 200)), (196, 38, 10, (40, 230, 240)),
                                 (124, 24, 7, (250, 240, 60)), (232, 12, 4, (120, 255, 120))):
            for y in range(H):
                for x in range(W):
                    dd = (x - cx) ** 2 + (y - cy) ** 2
                    if dd <= r * r:
                        k = 0.7 + 0.5 * (1 - math.sqrt(dd) / r)
                        px[x, y] = rgba(vc.shade(col, k))
                    elif abs(((x - cx) / 2.2) ** 2 + ((y - cy) * 1.1) ** 2 - (r * 1.15) ** 2) < r:
                        if dd > r * r and abs(y - cy) < r * 0.6:
                            px[x, y] = rgba(vc.shade(col, 0.9))
        for i in range(40):
            x = 70 + i
            y = 52 - i // 3
            px[x, y] = rgba((255, 255, 255 - i * 3))
        _ball_shape(img, 68, 53, 4, (255, 255, 255))
    elif name == "retro":
        for y in range(H):
            for x in range(W):
                px[x, y] = rgba((236, 226, 196))
        for (cx, cy, s, col) in ((32, 20, 1.0, (20, 150, 150)), (100, 44, 0.8, (236, 110, 40)),
                                 (176, 18, 0.9, (20, 150, 150)), (230, 42, 0.7, (236, 110, 40))):
            for y in range(H):
                for x in range(W):
                    u, v = (x - cx) / (22 * s), (y - cy) / (10 * s)
                    if u * u + v * v <= 1 and not ((u - 0.35) ** 2 + (v + 0.3) ** 2 <= 0.45):
                        px[x, y] = rgba(col)
        for (cx, cy) in ((66, 16), (140, 30), (210, 52)):
            for k in range(8):
                a = k * math.pi / 4
                for t in range(9):
                    x = int(round(cx + math.cos(a) * t))
                    y = int(round(cy + math.sin(a) * t))
                    if 0 <= x < W and 0 <= y < H:
                        px[x, y] = rgba((30, 30, 30))
        _pin_shape(img, 120, 12, (250, 250, 246), outline=(30, 30, 30))
    else:  # neon waves
        for y in range(H):
            for x in range(W):
                t = x / float(W)
                px[x, y] = rgba((60 + 120 * t, 20, 120 - 40 * t))
        for (amp, ph, col, yc) in ((8, 0.0, (40, 240, 255), 22), (10, 1.7, (255, 60, 220), 40),
                                   (6, 3.1, (255, 250, 120), 52)):
            for x in range(W):
                y = int(round(yc + amp * math.sin(x / 18.0 + ph)))
                for dy in (-1, 0, 1):
                    if 0 <= y + dy < H:
                        px[x, y + dy] = rgba(col if dy == 0 else vc.shade(col, 0.7))
        vc.draw_text_centred(img, "LETS BOWL", 128, 6, (255, 255, 255), scale=2)
    # a chrome edge top and bottom
    for x in range(W):
        for y in (0, 1, H - 2, H - 1):
            px[x, y] = rgba((196, 200, 206) if y in (0, H - 1) else (120, 124, 130))
    return img


def _pin_shape(img, cx, top, col, outline=None):
    px = img.load()
    prof = [r for _h, r in PIN_PROFILE_IN]
    hs = [h for h, _r in PIN_PROFILE_IN]
    H = 36
    for y in range(H):
        h = 15.0 * (1 - y / float(H - 1))
        r = float(np.interp(h, hs, prof)) * 2.0
        for x in range(int(cx - r), int(cx + r) + 1):
            yy = top + y
            if 0 <= x < img.width and 0 <= yy < img.height:
                edge = outline is not None and (abs(x - cx) >= r - 1)
                px[x, yy] = rgba(outline if edge else col)


def _ball_shape(img, cx, cy, r, col):
    px = img.load()
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r and 0 <= x < img.width and \
                    0 <= y < img.height:
                px[x, y] = rgba(vc.shade(col, 1.2 if (x - cx + y - cy) < -r * 0.6 else 1.0))


_DESIGN_CACHE = {}


def _sprite(name, i):
    def draw():
        if name not in _DESIGN_CACHE:
            _DESIGN_CACHE[name] = _design(name)
        return _DESIGN_CACHE[name].crop((64 * i, 0, 64 * (i + 1), 64))
    return draw


for _name in MASK_DESIGNS:
    for _i in range(4):
        C.textures["mask_%s_%d" % (_name, _i)] = _sprite(_name, _i)
        # OptiFine's emissive companion: the whole graphic glows
        C.textures["mask_%s_%d_e" % (_name, _i)] = _sprite(_name, _i)

PS_BOXES = [(PS_CAB[0], PS_CAB[2], PS_CAB[4], PS_CAB[1], PS_CAB[3], PS_CAB[5]),
            (PS_PANEL[0], PS_PANEL[2], PS_PANEL[4], PS_PANEL[1], 33, PS_TOP[5])]


def _ps_icon():
    els = [box([0, 2, 9], [16, 14, 16], "cab"), box([0, 8, 4], [16, 14, 6], "frame",
                                                    per={"north": "m"}),
           box([0, 14, 4], [16, 15, 16], "cab")]
    return vc.model({"cab": T("pinsetter_cab"), "frame": T("mask_frame"),
                     "m": T("mask_starburst_1"), "particle": T("pinsetter_cab")}, els)


C.add("bowling_pinsetter", unit_java("BlockBowlingPinsetter", "bowling_pinsetter", PS_BOXES),
      ("Pinsetter and Masking Unit", "Pinsetter mit Blende", "Colocador de Bolos y Panel",
       "Käglresare med Maskering"),
      {"pinsetter_item": _ps_icon()},
      {"forge_marker": 1, "defaults": {"model": M("pinsetter_0") + ".obj",
                                        "custom": {"ambient": False}},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "design": {str(d): {"model": M("pinsetter_%d" % d) + ".obj"}
                               for d in range(4)},
                    "inventory": [{"model": M("pinsetter_item"), "custom": {}}]}},
      item=item_of("pinsetter_item", 0.625), tab=TAB)


# ------------------------------------------------------------------------------------------
# The ball return: a hood over the track and a rack of balls, on the seam between a pair
# ------------------------------------------------------------------------------------------
# Master frame: the model faces north, to the settee; its middle is the master's east edge
# (x = 16), so the unit is the master and the block to its east.
BR_X0, BR_X1 = 10.5, 21.5


def ball_return_polys():
    P = []
    # the hood: upright sides and a half-octagon arch over the track, at the back
    z0, z1 = 6.0, 16.0
    yb = 8.0
    cx, r = 16.0, (BR_X1 - BR_X0) / 2
    P += vc.obox(BR_X0, BR_X1, 0, 1, z0, z1, "base", "nsewu")
    P += vc.obox(BR_X0, BR_X0 + 0.8, 1, yb, z0, z1, "hood", "nsewu")
    P += vc.obox(BR_X1 - 0.8, BR_X1, 1, yb, z0, z1, "hood", "nsewu")
    arch = [(cx + r * math.cos(math.radians(a)), yb + r * 0.62 * math.sin(math.radians(a)))
            for a in range(0, 181, 30)]
    for i in range(len(arch) - 1):
        (xa, ya), (xb, yb2) = arch[i], arch[i + 1]
        mx, my = (xa + xb) / 2 - cx, (ya + yb2) / 2 - yb
        P.append(vc.Poly([(xa, ya, z1), (xb, yb2, z1), (xb, yb2, z0), (xa, ya, z0)], "hood",
                         (mx, my, 0)))
        P.append(vc.Poly([(xa, ya, z0), (xb, yb2, z0), (cx, yb, z0)], "hood", (0, 0, -1)))
        P.append(vc.Poly([(xa, ya, z1), (xb, yb2, z1), (cx, yb, z1)], "hood", (0, 0, 1)))
    # the back, where the track comes up from under the approach, is closed
    P.append(vc.Poly([(BR_X0, 1, z1), (BR_X1, 1, z1), (BR_X1, yb, z1), (BR_X0, yb, z1)], "hood",
                     (0, 0, 1)))
    # the dark mouth under the arch, where the balls come out
    P.append(quad_n(BR_X0 + 0.8, BR_X1 - 0.8, 1, yb, z0 + 0.4, "mouth"))
    # the hand dryer's grille on top
    P += vc.obox(14, 18, yb + r * 0.62 - 0.3, yb + r * 0.62 + 0.6, 10, 14, "grille", "nsewu")
    # the rack: a pedestal and two rails falling to the front, with a stop
    P += vc.obox(12, 20, 0, 5.2, 0.5, z0, "base", "nsewu")
    for x in (13.4, 18.0):
        P += vc.obar_z(x, x + 0.6, lambda z: 5.2 + (z - 0.5) * 0.16, lambda z: 6.0 + (z - 0.5) * 0.16,
                       0.5, z0, "rail")
    P += vc.obox(13.4, 18.6, 5.2, 7.2, 0.2, 0.7, "rail", "nsewu")
    for i, (bz, mat) in enumerate(((2.6, "b0"), (6.0, "b1"), (9.6, "b2"))):
        y = 6.0 + (bz - 0.5) * 0.16 + BALL_R * 0.72 if bz < z0 else 1 + BALL_R
        P += sphere(16.0, y, bz, BALL_R, mat)
    return P


mtl("ball_return", {"base": T("ball_return_base"), "hood": T("ball_return_hood"),
                    "mouth": T("curtain"), "grille": T("grille"), "rail": T("chrome"),
                    "b0": T("ball_red"), "b1": T("ball_blue"), "b2": T("ball_green")})
write_obj("ball_return", "ball_return", ball_return_polys())


@C.texture("ball_return_base")
def _br_base():
    return vc.swatch((40, 42, 46), 930, grain=2)


@C.texture("ball_return_hood")
def _br_hood():
    img = vc.swatch((150, 30, 46), 931, grain=2)
    vc.rect(img, 0, 3, 16, 4, (230, 200, 90))
    return img


@C.texture("grille")
def _grille():
    img = vc.swatch((70, 72, 76), 932, grain=2)
    for x in range(1, 16, 2):
        vc.rect(img, x, 0, x + 1, 16, (30, 30, 34))
    return img


@C.texture("chrome")
def _chrome():
    return vc.brushed(CHROME, 933)


BR_BOXES = [(10, 0, 0, 22, 13, 16)]           # whole pixels: 6 into each cell, so both are claimed


def _br_icon():
    els = [box([4, 0, 6], [12, 9, 16], "hood"), box([5, 0, 0], [11, 5, 6], "base"),
           box([6.5, 5, 1], [9.5, 8, 4], "ball")]
    return vc.model({"hood": T("ball_return_hood"), "base": T("ball_return_base"),
                     "ball": T("ball_red"), "particle": T("ball_return_hood")}, els)


C.add("bowling_ball_return", unit_java("BlockVenueUnit", "bowling_ball_return", BR_BOXES),
      ("Ball Return", "Kugelrücklauf", "Retorno de Bolas", "Klotretur"),
      {"ball_return_item": _br_icon()},
      {"forge_marker": 1, "defaults": {"model": M("ball_return") + ".obj"},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "inventory": [{"model": M("ball_return_item"), "custom": {}}]}},
      item=item_of("ball_return_item", 0.8), tab=TAB)


# ------------------------------------------------------------------------------------------
# The settee: straights that join, a curved corner, and the scorer's console
# ------------------------------------------------------------------------------------------
@C.texture("vinyl")
def _vinyl():
    return vc.swatch((96, 40, 120), 940, grain=3)


@C.texture("vinyl_top")
def _vinyl_top():
    img = vc.swatch((110, 48, 136), 941, grain=3)
    for x in (5, 10):
        vc.rect(img, x, 0, x + 1, 16, (84, 34, 104))
    return img


@C.texture("vinyl_back")
def _vinyl_back():
    img = vc.swatch((104, 44, 128), 942, grain=3)
    for x in range(0, 16, 4):
        vc.rect(img, x, 0, x + 1, 16, (80, 32, 100))
    return img


SETTEE_TEX = {"v": T("vinyl"), "vt": T("vinyl_top"), "vb": T("vinyl_back"), "c": T("chrome"),
              "particle": T("vinyl")}


def settee_parts():
    return [box([0, 0, 5], [16, 2.5, 14], "c"),
            box([0, 2.5, 3], [16, 7.5, 14.5], "v", per={"up": "vt"}),
            box([0, 7.5, 13], [16, 17, 16], "v", per={"north": "vb"}),
            box([0, 17, 13.3], [16, 18, 15.8], "v")]


def settee_end():
    return [box([0, 2.5, 3], [1.6, 11, 16], "v"), box([0, 11, 3], [1.6, 11.5, 13], "c")]


C.add("bowling_settee", 'new BlockBowlingSettee("bowling_settee", false)',
      ("Bowling Settee", "Bowling-Sitzbank", "Banco de Bolera", "Bowlingsoffa"),
      {"settee": vc.model(SETTEE_TEX, settee_parts()),
       "settee_end_l": vc.model(SETTEE_TEX, settee_end()),
       "settee_end_r": vc.model(SETTEE_TEX, [vc.mirror_x(e) for e in settee_end()]),
       "settee_item": vc.model(SETTEE_TEX, settee_parts() + settee_end()
                               + [vc.mirror_x(e) for e in settee_end()])},
      {"multipart": [r for f, y in FACINGS for r in (
          {"when": {"facing": f}, "apply": dict({"model": M("settee")}, **ys(y))},
          {"when": {"facing": f, "end_l": "true"},
           "apply": dict({"model": M("settee_end_l")}, **ys(y))},
          {"when": {"facing": f, "end_r": "true"},
           "apply": dict({"model": M("settee_end_r")}, **ys(y))})]},
      item=item_of("settee_item", 0.625), tab=TAB)


def ring_band(r0, r1, y0, y1, mat, top_mat=None, segs=6, inner=True, outer=True, top=True):
    """An annular band round the block's north-east corner (16, 0), a quarter turn from south
    (+z) to west (-x): the settee corner's curve."""
    P = []
    cx, cz = 16.0, 0.0

    def pt(r, a, y):
        return (cx - r * math.sin(a), y, cz + r * math.cos(a))
    for i in range(segs):
        a0 = math.pi / 2 * i / segs
        a1 = math.pi / 2 * (i + 1) / segs
        am = (a0 + a1) / 2
        rad = (-math.sin(am), 0, math.cos(am))
        if outer:
            P.append(vc.Poly([pt(r1, a0, y0), pt(r1, a1, y0), pt(r1, a1, y1), pt(r1, a0, y1)], mat,
                             rad, [(i / segs, 1), ((i + 1) / segs, 1), ((i + 1) / segs, 0),
                                   (i / segs, 0)]))
        if inner:
            P.append(vc.Poly([pt(r0, a0, y0), pt(r0, a1, y0), pt(r0, a1, y1), pt(r0, a0, y1)],
                             mat, (-rad[0], 0, -rad[2]),
                             [(i / segs, 1), ((i + 1) / segs, 1), ((i + 1) / segs, 0),
                              (i / segs, 0)]))
        if top:
            pts = [pt(r0, a0, y1), pt(r1, a0, y1), pt(r1, a1, y1), pt(r0, a1, y1)]
            P.append(vc.Poly(pts, top_mat or mat, (0, 1, 0),
                             [(p[0] / 16.0, p[2] / 16.0) for p in pts]))
    return P


def settee_corner_polys():
    P = []
    P += ring_band(5, 14, 0, 2.5, "c", top=False)
    P += ring_band(3, 14.5, 2.5, 7.5, "v", "vt", outer=False)
    P += ring_band(13, 16, 7.5, 17, "v", top=False, inner=False)
    P += ring_band(13, 16, 7.5, 17, "vb", top=False, outer=False)
    P += ring_band(13.3, 15.8, 17, 18, "v")
    return P


mtl("settee_corner", {"v": T("vinyl"), "vt": T("vinyl_top"), "vb": T("vinyl_back"),
                      "c": T("chrome")})
write_obj("settee_corner", "settee_corner", settee_corner_polys())
C.add("bowling_settee_corner", 'new BlockBowlingSettee("bowling_settee_corner", true)',
      ("Bowling Settee (Curved Corner)", "Bowling-Sitzbank (Bogenecke)",
       "Banco de Bolera (Esquina Curva)", "Bowlingsoffa (Rundat Hörn)"),
      {"settee_corner_item": vc.model(SETTEE_TEX, [
          box([0, 0, 0], [16, 7.5, 16], "v", per={"up": "vt"}),
          box([0, 7.5, 12], [16, 18, 16], "v"), box([0, 7.5, 0], [4, 18, 12], "v")])},
      {"forge_marker": 1, "defaults": {"model": M("settee_corner") + ".obj"},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "inventory": [{"model": M("settee_corner_item"), "custom": {}}]}},
      item=item_of("settee_corner_item", 0.625), tab=TAB)


@C.texture("console_screen")
def _console_screen():
    img = vc.swatch((14, 20, 40), 950, grain=1)
    vc.rect(img, 1, 1, 15, 3, (40, 110, 220))
    for r in range(3):
        for c in range(5):
            vc.rect(img, 1 + c * 3, 4 + r * 3, 3 + c * 3, 6 + r * 3,
                    (220, 226, 236) if (r + c) % 3 else (255, 196, 60))
    vc.rect(img, 1, 13, 15, 15, (30, 160, 90))
    return img


@C.texture("console_body")
def _console_body():
    return vc.swatch((54, 56, 62), 951, grain=2)


def console_parts():
    head = box([3, 9.6, 3.5], [13, 10.8, 12], "b", per={"up": "s"})
    head["rotation"] = {"origin": [8, 10.2, 7.75], "axis": "x", "angle": -22.5}
    return [box([4, 0, 4], [12, 0.6, 12], "b"), box([7, 0.6, 7], [9, 10, 9], "c"), head,
            box([5, 8.2, 8.4], [11, 9.6, 10.6], "b")]


C.add("bowling_scorer_console",
      'new BlockBowlingFixture("bowling_scorer_console", BlockBowlingFixture.Kind.CONSOLE)',
      ("Scorer's Console", "Spielerkonsole", "Consola del Anotador", "Poängkonsol"),
      {"console": vc.model({"b": T("console_body"), "s": T("console_screen"), "c": T("chrome"),
                            "particle": T("console_body")}, console_parts())},
      vc.facing_state(M("console")), item=item_of("console", 0.8), tab=TAB)


# ------------------------------------------------------------------------------------------
# The overhead scoring monitors (BlockBowlingMonitor): a renderer draws the scores
# ------------------------------------------------------------------------------------------
# Shared with TileEntityBowlingMonitorRenderer: the face (x0, x1, y0, y1, z) and the screens.
MON_FACE = (0, 64, 6, 28, 6)
MON_SCREENS = [(33, 63), (1, 31)]       # model x: the reader's left screen first
MON_DEPTH = 3


def monitor_polys():
    P = []
    x0f, x1f, y0, y1, z = MON_FACE
    for (a, b) in MON_SCREENS:
        P += vc.obox(a, b, y0, y1, z, z + MON_DEPTH, "case", "sewud")
        P.append(quad_n(a, b, y0, y1, z, "glass"))
        P += vc.obox(a + 13, b - 13, y1, y1 + 0.6, z + 1, z + 2.2, "case", "nsewu")
    P += vc.obox(0, 64, 28.6, 30, z + 0.9, z + 2.3, "bar", "nsewud")
    for x in (8, 24, 40, 56):
        P += vc.obox(x - 0.4, x + 0.4, 30, 32, z + 1.2, z + 2.0, "bar", "nsew")
    return P


mtl("monitor", {"case": T("monitor_case"), "glass": T("monitor_glass"), "bar": T("chrome")})
write_obj("monitor", "monitor", monitor_polys())


@C.texture("monitor_case")
def _mon_case():
    return vc.swatch((24, 24, 28), 960, grain=2)


@C.texture("monitor_glass")
def _mon_glass():
    return vc.swatch((10, 12, 18), 961, grain=1)


MON_BOXES = [(1, MON_FACE[2], MON_FACE[4], 63, 32, MON_FACE[4] + MON_DEPTH)]


@C.texture("monitor_icon")
def _mon_icon():
    img = vc.swatch((24, 30, 60), 962, grain=1)
    vc.rect(img, 0, 0, 16, 3, (60, 110, 220))
    for r in range(3):
        vc.rect(img, 1, 4 + r * 4, 4, 7 + r * 4, (255, 180, 40) if r == 0 else (40, 46, 70))
        for c in range(5):
            vc.rect(img, 5 + c * 2, 4 + r * 4, 6 + c * 2, 7 + r * 4, (230, 234, 240))
    return img


C.add("bowling_scoring_monitor",
      unit_java("BlockBowlingMonitor", "bowling_scoring_monitor", MON_BOXES),
      ("Overhead Scoring Monitors", "Spielstandsmonitore", "Monitores de Puntuación",
       "Poängskärmar"),
      {"monitor_item": vc.model({"c": T("monitor_case"), "g": T("monitor_icon"),
                                 "particle": T("monitor_case")},
                                [box([0, 4, 6], [16, 14, 9], "c", per={"north": "g"}),
                                 box([7.5, 14, 7], [8.5, 16, 8], "c")])},
      {"forge_marker": 1, "defaults": {"model": M("monitor") + ".obj",
                                        "custom": {"ambient": False}},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "inventory": [{"model": M("monitor_item"), "custom": {}}]}},
      item=item_of("monitor_item", 0.8), tab=TAB)


# ------------------------------------------------------------------------------------------
# The house ball rack and the shoe counter (BlockArenaRun), the cubbies and the shoe spray
# ------------------------------------------------------------------------------------------
RACK_SHELVES = (0.6, 5.6, 10.6)
RACK_BALLS = sorted(BALL_COLOURS)


def rack_parts():
    els = [box([0, 0, 13], [16, 16, 14], "steel")]
    for y in RACK_SHELVES:
        els.append(box([0, y - 0.6, 3], [16, y, 13], "steel"))
        els.append(box([0, y, 2.6], [16, y + 0.9, 3.2], "c"))
    return els


def rack_balls():
    P = []
    n = 0
    for y in RACK_SHELVES:
        for x in (2.1, 6.0, 10.0, 13.9):
            P += sphere(x, y + BALL_R, 8.0, BALL_R, "b%d" % (n % len(RACK_BALLS)))
            n += 3
    return P


def rack_end():
    return [box([0, 0, 2.5], [0.8, 15, 14], "steel")]


mtl("rack_balls", {"b%d" % i: T(n) for i, n in enumerate(RACK_BALLS)})
write_obj("rack_balls", "rack_balls", rack_balls())

RUN_TEX = {"steel": T("rack_steel"), "c": T("chrome"), "particle": T("rack_steel")}


@C.texture("rack_steel")
def _rack_steel():
    return vc.swatch((70, 74, 82), 970, grain=2)


def run_state(prefix, extra=None):
    rules = []
    for facing, y in FACINGS:
        def ap(name, obj=False):
            return dict({"model": M(name) + (".obj" if obj else "")}, **ys(y))
        rules.append({"when": {"facing": facing}, "apply": ap(prefix)})
        if extra:
            rules.append({"when": {"facing": facing}, "apply": ap(extra, True)})
        rules.append({"when": {"facing": facing, "end_l": "true"}, "apply": ap(prefix + "_end_l")})
        rules.append({"when": {"facing": facing, "end_r": "true"}, "apply": ap(prefix + "_end_r")})
    return {"multipart": rules}


def _rack_icon():
    els = rack_parts() + rack_end() + [vc.mirror_x(e) for e in rack_end()]
    for i, y in enumerate(RACK_SHELVES):
        for x in (2, 6, 10, 14):
            els.append(box([x - 1.5, y, 6.5], [x + 1.5, y + 3, 9.5], "b%d" % ((i + x) % 3)))
    return vc.model(dict(RUN_TEX, b0=T("ball_red"), b1=T("ball_blue"), b2=T("ball_green")), els)


C.add("bowling_ball_rack", 'new BlockArenaRun("bowling_ball_rack", BlockArenaRun.Kind.BALL_RACK)',
      ("House Ball Rack", "Kugelregal", "Estante de Bolas", "Klotställ"),
      {"rack": vc.model(RUN_TEX, rack_parts()), "rack_end_l": vc.model(RUN_TEX, rack_end()),
       "rack_end_r": vc.model(RUN_TEX, [vc.mirror_x(e) for e in rack_end()]),
       "rack_item": _rack_icon()},
      run_state("rack", "rack_balls"), item=item_of("rack_item", 0.625), tab=TAB)


@C.texture("counter_front")
def _counter_front():
    img = vc.swatch((40, 44, 90), 980, grain=2)
    vc.rect(img, 0, 3, 16, 4, (250, 200, 60))
    vc.rect(img, 0, 5, 16, 6, (230, 60, 150))
    return img


@C.texture("counter_top")
def _counter_top():
    return vc.swatch((30, 30, 34), 981, grain=2)


@C.texture("counter_back")
def _counter_back():
    img = vc.swatch((150, 120, 90), 982, grain=3)
    vc.rect(img, 0, 7, 16, 8, (110, 86, 64))
    return img


COUNTER_TEX = {"f": T("counter_front"), "t": T("counter_top"), "k": T("counter_back"),
               "particle": T("counter_front")}


def counter_parts():
    return [box([0, 0, 2], [16, 15, 15.5], "f", per={"south": "k", "up": "t"}),
            box([0, 15, 0], [16, 16, 16], "t"), box([0, 0, 2.3], [16, 0.6, 15.2], "t")]


def counter_end():
    return [box([0, 0, 2], [0.6, 15, 15.5], "f")]


C.add("bowling_shoe_counter",
      'new BlockArenaRun("bowling_shoe_counter", BlockArenaRun.Kind.SHOE_COUNTER)',
      ("Shoe Counter", "Schuhtheke", "Mostrador de Zapatos", "Skodisk"),
      {"counter": vc.model(COUNTER_TEX, counter_parts()),
       "counter_end_l": vc.model(COUNTER_TEX, counter_end()),
       "counter_end_r": vc.model(COUNTER_TEX, [vc.mirror_x(e) for e in counter_end()]),
       "counter_item": vc.model(COUNTER_TEX, counter_parts() + counter_end()
                                + [vc.mirror_x(e) for e in counter_end()])},
      run_state("counter"), item=item_of("counter_item", 0.625), tab=TAB)

SHOE_COLOURS = [(170, 30, 36), (26, 50, 120), (150, 110, 70), (30, 120, 70)]


def _shoe_tex(col, seed):
    def draw():
        img = vc.swatch(col, seed, grain=2)
        vc.rect(img, 0, 12, 16, 16, (238, 236, 228))         # the sole
        vc.rect(img, 0, 4, 16, 6, (238, 236, 228))           # the two-tone saddle
        return img
    return draw


for _i, _c in enumerate(SHOE_COLOURS):
    C.textures["shoe_%d" % _i] = _shoe_tex(_c, 990 + _i)


@C.texture("cubby_wood")
def _cubby_wood():
    return vc.swatch((176, 140, 96), 995, grain=3)


def cubby_parts():
    els = [box([0, 0, 15], [16, 16, 16], "w"),
           box([0, 0, 5], [0.8, 16, 15], "w"), box([15.2, 0, 5], [16, 16, 15], "w"),
           box([0.8, 15.2, 5], [15.2, 16, 15], "w"), box([0.8, 0, 5], [15.2, 0.8, 15], "w")]
    for y in (5.4, 10.2):
        els.append(box([0.8, y, 5], [15.2, y + 0.6, 15], "w"))
    for x in (3.95, 7.7, 11.45):
        els.append(box([x, 0.8, 5], [x + 0.6, 15.2, 15], "w"))
    lefts = [0.8, 4.55, 8.3, 12.05]
    for r, y in enumerate((0.8, 6.0, 10.8)):
        for c, xl in enumerate(lefts):
            t = "s%d" % ((r * 4 + c * 3) % len(SHOE_COLOURS))
            for k in range(2):
                x0 = xl + 0.2 + k * 1.45
                els.append(box([x0, y, 9], [x0 + 1.3, y + 1.6, 14.5], t))
    return els


C.add("bowling_shoe_cubbies",
      'new BlockBowlingFixture("bowling_shoe_cubbies", BlockBowlingFixture.Kind.CUBBIES)',
      ("Shoe Cubbies", "Schuhfächer", "Casilleros de Zapatos", "Skofack"),
      {"cubbies": vc.model(dict({"w": T("cubby_wood"), "particle": T("cubby_wood")},
                                **{"s%d" % i: T("shoe_%d" % i)
                                   for i in range(len(SHOE_COLOURS))}), cubby_parts())},
      vc.facing_state(M("cubbies")), item=item_of("cubbies", 0.625), tab=TAB)


@C.texture("spray_can")
def _spray_can():
    img = vc.swatch((236, 236, 240), 1000, grain=1)
    vc.rect(img, 0, 3, 16, 10, (30, 120, 200))
    vc.rect(img, 0, 6, 16, 7, (250, 250, 250))
    return img


@C.texture("spray_card")
def _spray_card():
    img = vc.swatch((250, 214, 60), 1001, grain=1, size=32)
    vc.draw_text_centred(img, "SHOE", 16, 8, (30, 30, 30), scale=1)
    vc.draw_text_centred(img, "SPRAY", 16, 16, (30, 30, 30), scale=1)
    return img


def spray_parts():
    els = [box([4, 0, 5], [12, 0.6, 11], "t")]
    for x in (5, 9):
        els += [box([x, 0.6, 6.5], [x + 2, 5, 8.5], "can"),
                box([x + 0.4, 5, 6.9], [x + 1.6, 5.8, 8.1], "t")]
    els.append(box([4.5, 0.6, 10], [11.5, 5.4, 10.4], "card"))
    return els


C.add("bowling_shoe_spray",
      'new BlockBowlingFixture("bowling_shoe_spray", BlockBowlingFixture.Kind.SPRAY)',
      ("Shoe Spray", "Schuhspray", "Spray para Zapatos", "Skospray"),
      {"spray": vc.model({"t": T("counter_top"), "can": T("spray_can"), "card": T("spray_card"),
                          "particle": T("spray_can")}, spray_parts())},
      vc.facing_state(M("spray")), item=item_of("spray", 1.4, -2), tab=TAB)


# ------------------------------------------------------------------------------------------
# Messages
# ------------------------------------------------------------------------------------------
L = C.add_lang
L("itemGroup.tabvenuesbowling", ("CSM: Sports & Venues - Bowling",
                                 "CSM: Sport & Veranstaltungsstätten - Bowling",
                                 "CSM: Deportes y Recintos - Bolos",
                                 "CSM: Sport & Arenor - Bowling"))
L("csm.venues.bowling.bumpers_up", ("Bumpers up (%s blocks)", "Bumper hoch (%s Blöcke)",
                                    "Barreras arriba (%s bloques)", "Rännskydd uppe (%s block)"))
L("csm.venues.bowling.bumpers_down", ("Bumpers down (%s blocks)", "Bumper runter (%s Blöcke)",
                                      "Barreras abajo (%s bloques)", "Rännskydd nere (%s block)"))
L("csm.venues.bowling.masking", ("Masking graphic: %s", "Blendengrafik: %s",
                                 "Gráfico del panel: %s", "Maskeringsgrafik: %s"))
for _i, _n in enumerate((("Starburst", "Sternenexplosion", "Estallido", "Stjärnsmäll"),
                         ("Cosmic", "Kosmisch", "Cósmico", "Kosmisk"),
                         ("Retro", "Retro", "Retro", "Retro"),
                         ("Neon waves", "Neonwellen", "Ondas de neón", "Neonvågor"))):
    L("csm.venues.bowling.masking.%d" % _i, _n)
for _i, _n in enumerate((("Left lane 1", "Linke Bahn 1", "Pista izq. 1", "Vänster bana 1"),
                         ("Left lane 2", "Linke Bahn 2", "Pista izq. 2", "Vänster bana 2"),
                         ("Left lane 3", "Linke Bahn 3", "Pista izq. 3", "Vänster bana 3"),
                         ("Right lane 1", "Rechte Bahn 1", "Pista der. 1", "Höger bana 1"),
                         ("Right lane 2", "Rechte Bahn 2", "Pista der. 2", "Höger bana 2"),
                         ("Right lane 3", "Rechte Bahn 3", "Pista der. 3", "Höger bana 3"),
                         ("Left lane no.", "Nr. linke Bahn", "Nº pista izq.",
                          "Nr vänster bana"))):
    L("gui.csm.venue_text.bowling.%d" % _i, _n)
L("gui.csm.venue_text.choice.screen", ("Screens: %s", "Bildschirme: %s", "Pantallas: %s",
                                       "Skärmar: %s"))
L("gui.csm.venue_text.choice.up_left", ("Up, left: %s", "Am Zug, links: %s",
                                        "Turno, izq.: %s", "På tur, vänster: %s"))
L("gui.csm.venue_text.choice.up_right", ("Up, right: %s", "Am Zug, rechts: %s",
                                         "Turno, der.: %s", "På tur, höger: %s"))
for _i, _n in enumerate((("nobody", "niemand", "nadie", "ingen"),
                         ("bowler 1", "Spieler 1", "jugador 1", "spelare 1"),
                         ("bowler 2", "Spieler 2", "jugador 2", "spelare 2"),
                         ("bowler 3", "Spieler 3", "jugador 3", "spelare 3"))):
    L("csm.venues.bowling.up.%d" % _i, _n)


# ------------------------------------------------------------------------------------------
# A bowling centre's blocks, for a builder (or a build script): --layout
# ------------------------------------------------------------------------------------------
LANE_BLOCKS = 20
APPROACH_BLOCKS = 5
DEMO_BOWLERS = [
    (("ALEX: X 9/ 8- X X 72 9/ X", "JO: 81 9/ X 7/ 9- X 8/"),
     ("SAM: X X X 9- 8/ X 7", "RIVER: 9/ 9/ X 8- 7/")),
    (("KIT: X 81 X 9/ X X X X X X X X", "MO: 7/ X X X 9- X"),
     ("DEE: 6/ 8- X 9/ 7", "PAT: X 9/ 9- 81 72 X 9/ X 8- 9/X")),
    (("LEE: 9- 9- 8/", ""), ()),
    ((), ()),
]


def layout(lanes=8):
    """(ground, objects): ground [x, z, block, meta] at the floor's level, objects
    [x, dy, z, block, meta, nbt] above it. The canonical frame: lane 1 the westmost (columns x 0
    and 1), the pins to the north, the foul line on the block edge z = 0, the approach z 0..4,
    the settee area from z 5."""
    if lanes % 2:
        raise SystemExit("--lanes must be even (lanes come in pairs)")
    ground, objs = [], []
    width = 2 * lanes
    for L_ in range(lanes):
        syn = L_ >= lanes // 2
        bump = 8 if L_ == 2 else 0
        for c in range(2):
            x = 2 * L_ + c
            meta = 2 | (4 if c else 0)
            lane = "bowling_lane_synthetic" if syn else "bowling_lane"
            appr = "bowling_approach_synthetic" if syn else "bowling_approach"
            for k in range(LANE_BLOCKS):
                ground.append([x, -1 - k, lane, meta | bump])
            for k in range(APPROACH_BLOCKS):
                ground.append([x, k, appr, meta])
        objs.append([2 * L_, 1, -ROWS_TO_HEAD - 1, "bowling_pins", 2, None])
    for x in range(-1, width + 1):
        for z in (-21, -22):
            ground.append([x, z, "concrete", 15])
        for z in range(5, 15):
            ground.append([x, z, "wool", 11])
    for z in range(-22, 5):
        for x in (-1, width):
            ground.append([x, z, "concrete", 15])
    for P in range(lanes // 2):
        x0 = 4 * P
        objs.append([x0 + 3, 1, -20, "bowling_pinsetter", 0 | (P % 4) << 2, None])
        objs.append([x0 + 2, 1, 5, "bowling_ball_return", 0, None])
        objs += [[x0, 1, 8, "bowling_settee_corner", 2, None],
                 [x0 + 1, 1, 8, "bowling_settee", 2, None],
                 [x0 + 2, 1, 8, "bowling_settee", 2, None],
                 [x0 + 3, 1, 8, "bowling_settee_corner", 1, None],
                 [x0, 1, 7, "bowling_settee", 3, None],
                 [x0 + 3, 1, 7, "bowling_settee", 1, None],
                 [x0 + 1, 1, 6, "bowling_scorer_console", 0, None]]
        left, right = DEMO_BOWLERS[P % len(DEMO_BOWLERS)]
        keys = ["ln:%d" % (2 * P + 1), "c:%d" % [1, 9, 0, 4][P % 4],
                "ua:%d" % (1 if left else 0), "ub:%d" % (2 if len(right) > 1 else 0)]
        for i, line in enumerate(left):
            keys.append('b%d:"%s"' % (i, line))
        for i, line in enumerate(right):
            keys.append('b%d:"%s"' % (3 + i, line))
        nbt = "{" + ",".join(keys) + "}"
        objs.append([x0 + 3, 3, 1, "bowling_scoring_monitor", 0, nbt])
    # the beam the monitors hang from, on two columns
    for x in range(-1, width + 1):
        objs.append([x, 5, 1, "concrete", 15, None])
    for x in (-1, width):
        for dy in range(1, 5):
            objs.append([x, dy, 1, "concrete", 15, None])
    # the side walls, a ledge high, and the back wall
    for z in range(-22, 5):
        if z == 1:
            continue
        for x in (-1, width):
            objs.append([x, 1, z, "concrete", 15, None])
    for x in range(-1, width + 1):
        for dy in (1, 2, 3):
            objs.append([x, dy, 14, "concrete", 15, None])
    # the house ball racks along the back wall, the shoe counter with its cubbies and spray
    for x in range(0, min(6, width)):
        objs.append([x, 1, 13, "bowling_ball_rack", 2, None])
    cx = min(width - 6, 9)
    for x in range(cx, cx + 5):
        objs.append([x, 1, 11, "bowling_shoe_counter", 2, None])
        objs.append([x, 1, 13, "bowling_shoe_cubbies", 2, None])
        objs.append([x, 2, 13, "bowling_shoe_cubbies", 2, None])
    objs.append([cx + 1, 2, 11, "bowling_shoe_spray", 2, None])
    return ground, objs


def commands(x0, y0, z0, lanes=8):
    ground, objs = layout(lanes)
    cells = {}
    for x, z, b, m in ground:
        cells[(x, z)] = (b, m)
    out = []
    ns = lambda b: b if ":" in b else (("minecraft:" if b in ("concrete", "wool") else "csm:")
                                        + b)
    for wz in sorted(set(z for _x, z in cells)):
        row = sorted(x for x, z in cells if z == wz)
        i = 0
        while i < len(row):
            b, m = cells[(row[i], wz)]
            j = i
            while (j + 1 < len(row) and row[j + 1] == row[j] + 1
                   and cells[(row[j + 1], wz)] == (b, m)):
                j += 1
            if j > i:
                out.append("fill %d %d %d %d %d %d %s %d" % (
                    x0 + row[i], y0, z0 + wz, x0 + row[j], y0, z0 + wz, ns(b), m))
            else:
                out.append("setblock %d %d %d %s %d" % (x0 + row[i], y0, z0 + wz, ns(b), m))
            i = j + 1
    for (x, dy, z, b, m, nbt) in objs:
        out.append("setblock %d %d %d %s %d%s" % (x0 + x, y0 + dy, z0 + z, ns(b), m,
                                                   " replace " + nbt if nbt else ""))
    return out


# ------------------------------------------------------------------------------------------
# Main
# ------------------------------------------------------------------------------------------
def main():
    if "--layout" in sys.argv:
        lanes = 8
        if "--lanes" in sys.argv:
            lanes = int(sys.argv[sys.argv.index("--lanes") + 1])
        if "--commands" in sys.argv:
            i = sys.argv.index("--commands")
            x, y, z = (int(v) for v in sys.argv[i + 1:i + 4])
            print("\n".join(commands(x, y, z, lanes)))
        else:
            g, o = layout(lanes)
            print(json.dumps({"lanes": lanes,
                              "frame": "lane 1 the westmost (x 0, 1), the pins to the north, "
                                       "the foul line on z = 0; ground at the floor's level, "
                                       "objects dy above it",
                              "ground": g, "objects": o}))
        return 0
    rc = C.main()
    if rc == 0 and "--fragments" not in sys.argv and "--check" not in sys.argv:
        vc.check_json([os.path.join(C.assets, rel) for rel in
                       [os.path.join(dp, fn) for dp, _, fs in os.walk(C.assets) for fn in fs]])
    return rc


if __name__ == "__main__":
    sys.exit(main())
