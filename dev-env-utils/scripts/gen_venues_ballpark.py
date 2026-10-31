#!/usr/bin/env python3
"""
gen_venues_ballpark.py -- the Sports & Venues Ballpark tab: baseball and softball.

    py -3 dev-env-utils/scripts/gen_venues_ballpark.py                # write
    py -3 dev-env-utils/scripts/gen_venues_ballpark.py --check        # fail on drift
    py -3 dev-env-utils/scripts/gen_venues_ballpark.py --fragments    # the tab's lines
    py -3 dev-env-utils/scripts/gen_venues_ballpark.py --layout baseball   # a field's blocks
    py -3 dev-env-utils/scripts/gen_venues_ballpark.py --doc          # anchors and tables

The field is drawn from one geometry, ballpark_field.py: a full-size diamond laid with its centre
line along the grid, one block a metre. Every cell of it is one of:

  * a plain surface (BlockArenaFloor: ``ballpark_turf`` in a light or dark mowing stripe,
    ``ballpark_clay``: infield skin, dragged skin, packing clay, warning track);
  * a standard piece, the same in every field: a turf/clay or turf/track edge corner to corner
    (``ballpark_field_edge``, ``ballpark_track_edge``), a chalk line across a corner or down the
    middle (``ballpark_chalk_turf``, ``ballpark_chalk_clay``): the foul lines run at 45 degrees,
    a band across one corner of each cell they cross, two cells a step;
  * a picture round a marking anchor (``ballpark_marking_anchor`` and the arc and softball
    anchors, each with a third base side twin that draws its pictures mirrored): the arena's
    painted floor, a ``ballpark_painted`` cell showing the piece of its nearest anchor's picture
    that falls on it. The home plate area with its batter's and catcher's boxes, the base
    cutouts, the coach's boxes, the on-deck circles, the infield arc in segments, softball's home
    area and pitcher's circle;
  * the pitcher's mound: ``pitchers_mound`` (the rubber's cell) and ``pitchers_mound_slope``
    round it, each cell a heightfield OBJ up to 10 in (4 px) high.

Then the fixtures (home plate, bases, the rubber), the dugout (bench, rail, canopy, helmet and bat
racks, water cooler), nets (backstop and batting cage panels, the roof net), the outfield wall
(straight, 45 degree, the distance marker) and foul pole, the line score scoreboard and the
count board, and the pitching machine.

Every model faces north (the field, the batter, the reader) with its back at +Z. No brand.
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
import ballpark_field as bf  # noqa: E402

GEN = "gen_venues_ballpark.py"
C = vc.catalogue(GEN, "ballpark")
T = C.T
M = C.M
box = vc.box
lsc = vc.lsc
wbox = lsc.pp.wbox

TAB = "Sports & Venues - Ballpark"
FACINGS = vc.FACINGS
RES = bf.RES
JAVA_DIR = os.path.join(vc.REPO, "modules", "venues", "src", "main", "java", "com",
                        "micatechnologies", "minecraft", "csm", "venues", "ballpark")
JAVA = {}
DOC = {}

# ------------------------------------------------------------------------------------------
# Palette
# ------------------------------------------------------------------------------------------
TURF_LIGHT = (98, 160, 62)
TURF_DARK = (72, 128, 46)
CLAY = (184, 116, 72)
PACK = (156, 86, 56)
TRACK = (118, 72, 58)
CHALK = (244, 244, 238)
RUBBER = (250, 250, 250)
SOIL = (118, 84, 58)
PAD_GREEN = (30, 72, 46)
YELLOW = (250, 206, 24)
NAVY = (26, 40, 84)
STEEL = (196, 200, 204)
STEEL_DARK = (88, 92, 98)
BLACK = (26, 26, 28)
NET = (34, 36, 34)
CONCRETE = (168, 166, 160)
BOARD_GREEN = (24, 58, 40)


def rgba(c, a=255):
    return tuple(c[:3]) + (a,)


def ys(y):
    return {"y": y} if y else {}


def only(el, *faces):
    el["faces"] = {k: v for k, v in el["faces"].items() if k in faces}
    return el


def rot(el, axis, angle, origin):
    el["rotation"] = {"origin": [round(v, 3) for v in origin], "axis": axis, "angle": angle}
    return el


def icon_display(scale, dy=0.0, rot_y=225):
    return {"gui": {"rotation": [30, rot_y, 0], "translation": [0, dy, 0],
                    "scale": [scale, scale, scale]},
            "ground": {"translation": [0, 3, 0], "scale": [scale * 0.5] * 3},
            "fixed": {"translation": [0, dy, 0], "scale": [scale, scale, scale]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                      "scale": [scale * 0.6] * 3},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [scale * 0.7] * 3}}


def item_of(model_name, scale, dy=0.0):
    return {"parent": "csm:block/venues/ballpark/" + model_name, "display": icon_display(scale, dy)}


# ------------------------------------------------------------------------------------------
# Surface textures: isotropic grain, so a cell turned or mirrored meets its neighbours unseen
# ------------------------------------------------------------------------------------------
def grain(colour, seed, spread=0.07, flecks=(), size=RES):
    rng = random.Random(seed)
    arr = np.zeros((size, size, 3))
    for y in range(size):
        for x in range(size):
            k = 1.0 + rng.uniform(-spread, spread)
            c = colour
            for (fc, p) in flecks:
                if rng.random() < p:
                    c = fc
                    break
            arr[y, x] = [v * k for v in c]
    return np.clip(arr, 0, 255)


def turf(colour, seed=701):
    """Grass: blades of a darker and a lighter green through the base, no direction."""
    return grain(colour, seed, 0.06, ((vc.shade(colour, 0.78), 0.16),
                                      (vc.shade(colour, 1.16), 0.14)))


SURF = {
    "turf": turf(TURF_LIGHT),
    "turf_dark": turf(TURF_DARK),
    "clay": grain(CLAY, 702, 0.05, ((vc.shade(CLAY, 0.86), 0.12), (vc.shade(CLAY, 1.1), 0.08))),
    "pack": grain(PACK, 703, 0.035, ((vc.shade(PACK, 0.9), 0.08),)),
    "track": grain(TRACK, 704, 0.06, ((vc.shade(TRACK, 0.78), 0.18), (vc.shade(TRACK, 1.22),
                                                                      0.12))),
    "chalk": grain(CHALK, 705, 0.025, ((vc.shade(CHALK, 0.92), 0.12),)),
    "rubber": grain(RUBBER, 706, 0.01),
}
# the label order of ballpark_field: turf, clay, pack, track, chalk, rubber
LAYER = ["turf", "clay", "pack", "track", "chalk", "rubber"]


def compose(cov, dark=False):
    """A cell's top from its label coverage: each surface's texture where it covers."""
    out = np.zeros((RES, RES, 3))
    for k, name in enumerate(LAYER):
        tex = SURF["turf_dark" if dark and name == "turf" else name]
        out += cov[k][..., None] * tex
    img = np.concatenate([np.clip(np.rint(out), 0, 255), np.full((RES, RES, 1), 255)], axis=2)
    return Image.fromarray(img.astype(np.uint8), "RGBA")


def plain(name):
    img = np.concatenate([np.rint(SURF[name]), np.full((RES, RES, 1), 255)], axis=2)
    return Image.fromarray(img.astype(np.uint8), "RGBA")


def dragged():
    """Infield skin after the drag mat: faint grooves along the texture's height."""
    arr = SURF["clay"].copy()
    for x in range(0, RES, 4):
        arr[:, x] *= 0.9
        arr[:, (x + 1) % RES] *= 1.05
    img = np.concatenate([np.clip(np.rint(arr), 0, 255), np.full((RES, RES, 1), 255)], axis=2)
    return Image.fromarray(img.astype(np.uint8), "RGBA")


def split_turf():
    """Two mowing stripes meeting corner to corner: dark on the north-west half."""
    U = (np.arange(RES) + 0.5)[None, :] / RES
    V = (np.arange(RES) + 0.5)[:, None] / RES
    m = (U + V < 1)[..., None]
    arr = np.where(m, SURF["turf_dark"], SURF["turf"])
    img = np.concatenate([np.rint(arr), np.full((RES, RES, 1), 255)], axis=2)
    return Image.fromarray(img.astype(np.uint8), "RGBA")


@C.texture("soil")
def _soil():
    return vc.swatch(SOIL, 710, grain=6)


for _n in ("turf", "turf_dark", "clay", "pack", "track"):
    C.textures[_n] = (lambda n: (lambda: plain(n)))(_n)
C.textures["clay_dragged"] = dragged
C.textures["turf_split"] = split_turf

PIECE_TEX = {}


def piece_texture(name, dark=False):
    """A standard piece's texture, drawn facing north."""
    key = name + ("_dark" if dark else "")
    if key not in PIECE_TEX:
        cov = bf.piece_coverage(bf.PIECES[name], 0)
        PIECE_TEX[key] = compose(cov, dark)
        C.textures["piece_" + key] = (lambda k: (lambda: PIECE_TEX[k]))(key)
    return T("piece_" + key)


# ------------------------------------------------------------------------------------------
# Floor blocks
# ------------------------------------------------------------------------------------------
FLOOR_MODELS = {}
SIDE = T("soil")


def floor_model(top, mirror=False):
    if not mirror:
        return {"parent": "block/cube",
                "textures": {"particle": top, "down": SIDE, "up": top, "north": SIDE,
                             "south": SIDE, "east": SIDE, "west": SIDE}}
    faces = {f: {"texture": "#side", "cullface": f} for f in ("north", "south", "east", "west",
                                                              "down")}
    faces["up"] = {"texture": "#up", "uv": [16, 0, 0, 16], "cullface": "up"}
    return {"parent": "block/block", "textures": {"particle": top, "up": top, "side": SIDE},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]}


def fmodel(name, top, mirror=False):
    FLOOR_MODELS[name] = floor_model(top, mirror)
    return M(name)


def facing_y(meta):
    return {0: 180, 1: 270, 2: 0, 3: 90}[meta & 3]


def facing_state(models_by_kind, inventory):
    """A BlockArenaFloor with Orient.FACING: metadata facing | kind << 2."""
    v = {}
    for kind, m in enumerate(models_by_kind):
        for f in range(4):
            y = facing_y(f)
            v["variant=%d" % (kind * 4 + f)] = [dict({"model": m}, **ys(y))]
    v["inventory"] = [{"model": inventory}]
    return {"variants": v}


def add_floor(reg, java, names, models_by_kind):
    C.add(reg, java, names, dict(FLOOR_MODELS), facing_state(models_by_kind, models_by_kind[0]),
          tab=TAB)
    FLOOR_MODELS.clear()


add_floor("ballpark_turf",
          'new BlockArenaFloor("ballpark_turf", 3, BlockArenaFloor.Orient.FACING, '
          'BlockArenaFloor.Surface.TURF)',
          ("Ballpark Turf", "Spielfeldrasen", "Césped de Campo de Béisbol", "Bollplansgräs"),
          [fmodel("turf_light", T("turf")), fmodel("turf_dark", T("turf_dark")),
           fmodel("turf_split", T("turf_split"))])
add_floor("ballpark_clay",
          'new BlockArenaFloor("ballpark_clay", 4, BlockArenaFloor.Orient.FACING, '
          'BlockArenaFloor.Surface.CLAY)',
          ("Ballpark Clay", "Spielfeldlehm", "Arcilla de Campo de Béisbol", "Bollplanslera"),
          [fmodel("clay", T("clay")), fmodel("clay_dragged", T("clay_dragged")),
           fmodel("pack", T("pack")), fmodel("track", T("track"))])
add_floor("ballpark_field_edge",
          'new BlockArenaFloor("ballpark_field_edge", 4, BlockArenaFloor.Orient.FACING, '
          'BlockArenaFloor.Surface.TURF)',
          ("Ballpark Grass Edge", "Rasenkante (Spielfeld)", "Borde de Césped de Campo",
           "Gräskant för Bollplan"),
          [fmodel("edge_diag", piece_texture("edge_diag")),
           fmodel("edge_half", piece_texture("edge_half")),
           fmodel("edge_quarter", piece_texture("edge_quarter")),
           fmodel("edge_quarter_in", piece_texture("edge_quarter_in"))])
add_floor("ballpark_track_edge",
          'new BlockArenaFloor("ballpark_track_edge", 4, BlockArenaFloor.Orient.FACING, '
          'BlockArenaFloor.Surface.TURF)',
          ("Warning Track Edge", "Warnspurkante", "Borde de Pista de Advertencia",
           "Varningsbanans Kant"),
          [fmodel("track_diag", piece_texture("track_diag")),
           fmodel("track_diag_dark", piece_texture("track_diag", True)),
           fmodel("track_half", piece_texture("track_half")),
           fmodel("track_half_dark", piece_texture("track_half", True))])
add_floor("ballpark_chalk_turf",
          'new BlockArenaFloor("ballpark_chalk_turf", 4, BlockArenaFloor.Orient.FACING, '
          'BlockArenaFloor.Surface.TURF)',
          ("Chalk Line on Turf", "Kreidelinie auf Rasen", "Línea de Cal sobre Césped",
           "Kritlinje på Gräs"),
          [fmodel("chalk_turf_band", piece_texture("chalk_turf_band")),
           fmodel("chalk_turf_band_dark", piece_texture("chalk_turf_band", True)),
           fmodel("chalk_turf_mid", piece_texture("chalk_turf_mid")),
           fmodel("chalk_turf_mid_dark", piece_texture("chalk_turf_mid", True))])
add_floor("ballpark_chalk_clay",
          'new BlockArenaFloor("ballpark_chalk_clay", 4, BlockArenaFloor.Orient.FACING, '
          'BlockArenaFloor.Surface.CLAY)',
          ("Chalk Line on Clay", "Kreidelinie auf Lehm", "Línea de Cal sobre Arcilla",
           "Kritlinje på Lera"),
          [fmodel("chalk_clay_band", piece_texture("chalk_clay_band")),
           fmodel("chalk_track_band", piece_texture("chalk_track_band")),
           fmodel("chalk_clay_mid", piece_texture("chalk_clay_mid")),
           fmodel("chalk_track_mid", piece_texture("chalk_track_mid"))])

# which block, kind and tone draws each standard piece
PIECE_BLOCK = {
    "turf": ("ballpark_turf", 0), "clay": ("ballpark_clay", 0), "pack": ("ballpark_clay", 2),
    "track": ("ballpark_clay", 3),
    "edge_diag": ("ballpark_field_edge", 0), "edge_half": ("ballpark_field_edge", 1),
    "edge_quarter": ("ballpark_field_edge", 2), "edge_quarter_in": ("ballpark_field_edge", 3),
    "track_diag": ("ballpark_track_edge", 0), "track_half": ("ballpark_track_edge", 2),
    "chalk_turf_band": ("ballpark_chalk_turf", 0), "chalk_turf_mid": ("ballpark_chalk_turf", 2),
    "chalk_clay_band": ("ballpark_chalk_clay", 0), "chalk_track_band": ("ballpark_chalk_clay", 1),
    "chalk_clay_mid": ("ballpark_chalk_clay", 2), "chalk_track_mid": ("ballpark_chalk_clay", 3),
}
# pieces with a dark-stripe kind one above the light one
TONED = {"turf", "track_diag", "track_half", "chalk_turf_band", "chalk_turf_mid"}
FACING_META = {"south": 0, "west": 1, "north": 2, "east": 3}


# ------------------------------------------------------------------------------------------
# The fields: census, anchors, pictures
# ------------------------------------------------------------------------------------------
FIELDS = {}
for _kind in ("baseball", "softball"):
    _f = bf.Field(_kind)
    _cen = bf.census(_f, *bf.EXTENT[_kind])
    _anchors, _owner, _m_anchor = bf.plan(_f, _cen)
    FIELDS[_kind] = {"field": _f, "census": _cen, "anchors": _anchors, "owner": _owner,
                     "mound": _m_anchor}


def cell_cov(kind, c):
    v = FIELDS[kind]["census"][c]
    if v[0] == "picture":
        return v[1]
    return bf.coverage(FIELDS[kind]["field"], *c)


# The anchor groups: each a block (and its third base side twin) of up to four kinds.
GROUPS = [
    ("ballpark_marking_anchor", [("baseball", "bb_home"), ("baseball", "bb_cutout"),
                                 ("baseball", "coach"), ("baseball", "on_deck")]),
    ("ballpark_arc_anchor", [("baseball", "arc0"), ("baseball", "arc1"), ("baseball", "arc2")]),
    ("softball_marking_anchor", [("softball", "sb_home"), ("softball", "sb_circle"),
                                 ("softball", "arc0"), ("softball", "arc1")]),
]
KIND_NAMES = {
    "bb_home": ("home plate area", "Schlagmal-Bereich", "área del plato", "hemplattsområde"),
    "bb_cutout": ("base cutout", "Base-Aussparung", "recorte de base", "basurtag"),
    "coach": ("coach's box", "Coach-Box", "cajón del coach", "coachruta"),
    "on_deck": ("on-deck circle", "On-Deck-Kreis", "círculo de espera", "väntcirkel"),
    "arc0": ("infield arc, centre", "Infield-Bogen, Mitte", "arco del cuadro, centro",
             "innerplansbåge, mitten"),
    "arc1": ("infield arc, middle", "Infield-Bogen, Mittelstück", "arco del cuadro, medio",
             "innerplansbåge, mellan"),
    "arc2": ("infield arc, end", "Infield-Bogen, Ende", "arco del cuadro, extremo",
             "innerplansbåge, ände"),
    "sb_home": ("softball home plate area", "Softball-Schlagmal-Bereich",
                "área del plato de sóftbol", "softbollens hemplattsområde"),
    "sb_circle": ("pitcher's circle", "Pitcher-Kreis", "círculo del lanzador", "kastcirkel"),
}

PICS = []               # (key, cov) in index order; 0 plain clay, 1 plain turf
PIC_INDEX = {}
MIRRORED = {}           # picture index -> its mirrored model's cell index
CELL_MODELS = []        # cell index -> (picture index, mirrored)


def picture(cov):
    key = bf.picture_key(cov)
    if key not in PIC_INDEX:
        PIC_INDEX[key] = len(PICS)
        PICS.append(cov)
        CELL_MODELS.append((PIC_INDEX[key], False))
    return PIC_INDEX[key]


picture(bf.piece_coverage(bf.PIECES["clay"]))
picture(bf.piece_coverage(bf.PIECES["turf"]))


def mirrored_cell(p):
    """The cell showing picture p mirrored: a picture that already is its mirror (home's left
    and right, a symmetric cell) where there is one, else a mirrored model of p."""
    if p not in MIRRORED:
        flipped = PICS[p][:, :, ::-1]
        for q in range(len(PICS)):
            d = np.abs(PICS[q] - flipped)
            if d.max() <= 0.34 and d.mean() <= 0.004:
                MIRRORED[p] = q
                break
        else:
            MIRRORED[p] = len(CELL_MODELS)
            CELL_MODELS.append((p, True))
    return MIRRORED[p]


def anchor_of(kind, key, mirror=False):
    for a in FIELDS[kind]["anchors"]:
        if a["key"] == key and a["mirror"] == mirror:
            return a
    raise KeyError((kind, key, mirror))


TABLES = []             # per group * 4 + kind: {(i, j): (cell, mirrored cell)}
for _block, _kinds in GROUPS:
    for _k in range(4):
        if _k >= len(_kinds):
            TABLES.append({})
            continue
        _fk, _key = _kinds[_k]
        _a = anchor_of(_fk, _key)
        _ax, _az = _a["cell"]
        _t = {}
        for (_cx, _cz) in sorted(_a["cells"] | {_a["cell"]}):
            _p = picture(cell_cov(_fk, (_cx, _cz)))
            _t[(_cx - _ax, -(_cz - _az))] = _p
        TABLES.append(_t)
# the mirrored cells, for the kinds a third base side twin draws: every kind, since a twin
# carries all four of its block's kinds
for _t in TABLES:
    for _ij, _p in list(_t.items()):
        _t[_ij] = (_p, mirrored_cell(_p))


def verify_mirrors():
    """Every third base side anchor's cells are the mirror of its first base side kind's."""
    for kind, fd in FIELDS.items():
        for a in fd["anchors"]:
            if not a["mirror"]:
                continue
            base = anchor_of(kind, a["key"])
            ax, az = a["cell"]
            for (cx, cz) in a["cells"]:
                i, j = cx - ax, -(cz - az)
                mine = cell_cov(kind, (cx, cz))[:, :, ::-1]
                bx, bz = base["cell"]
                other = cell_cov(kind, (bx - i, bz - j))
                d = np.abs(mine - other)
                # the samples are jittered, so a mirror differs by a sample at an edge
                if d.max() > 0.34 or d.mean() > 0.004:
                    raise SystemExit("%s %s: cell (%d, %d) is not the mirror of its twin"
                                     % (kind, a["name"], cx, cz))


verify_mirrors()


@C.texture("pic_000")
def _pic0():
    return compose(PICS[0])


for _p in range(1, len(PICS)):
    C.textures["pic_%03d" % _p] = (lambda p: (lambda: compose(PICS[p])))(_p)

PAINT_MODELS = {}
for _c, (_p, _mir) in enumerate(CELL_MODELS):
    PAINT_MODELS["cell_%03d" % _c] = floor_model(T("pic_%03d" % _p), _mir)


def anchor_state(group, third):
    kinds = GROUPS[group][1]
    variants = {}
    for k in range(4):
        t = TABLES[group * 4 + k]
        cell = t.get((0, 0), (0, 0))[1 if third else 0] if t else 0
        for name, y in FACINGS:
            variants["facing=%s,kind=%d" % (name, k)] = [dict({"model": M("cell_%03d" % cell)},
                                                             **ys(y))]
    first = TABLES[group * 4].get((0, 0), (0, 0))[1 if third else 0]
    variants["inventory"] = [{"model": M("cell_%03d" % first)}]
    return {"forge_marker": 1, "variants": variants}


ANCHOR_NAMES = {
    "ballpark_marking_anchor": ("Ballpark Marking Anchor", "Spielfeld-Markierungsanker",
                                "Ancla de Marca de Campo", "Ankare för Planmarkering"),
    "ballpark_arc_anchor": ("Infield Arc Anchor", "Infield-Bogen-Anker",
                            "Ancla del Arco del Cuadro", "Ankare för Innerplansbåge"),
    "softball_marking_anchor": ("Softball Marking Anchor", "Softball-Markierungsanker",
                                "Ancla de Marca de Sóftbol", "Ankare för Softbollsmarkering"),
}
THIRD = (" (Third Base Side)", " (Third-Base-Seite)", " (Lado de Tercera)", " (Tredjebassidan)")

for _g, (_block, _kinds) in enumerate(GROUPS):
    for _third in (False, True):
        _reg = _block + ("_third" if _third else "")
        _names = tuple(n + (THIRD[i] if _third else "") for i, n in
                       enumerate(ANCHOR_NAMES[_block]))
        C.add(_reg, 'new BlockBallparkAnchor("%s", BallparkLayouts.FIELD, %d, %s, %d)'
              % (_reg, _g, "true" if _third else "false", len(_kinds)), _names,
              dict(PAINT_MODELS) if (_g == 0 and not _third) else {},
              anchor_state(_g, _third), tab=TAB)


def painted_state():
    n = len(CELL_MODELS)
    return {"forge_marker": 1,
            "defaults": {"model": M("cell_000")},
            "variants": {"cell": {str(k): {"model": M("cell_%03d" % k)} for k in range(n)},
                         "rot": {"0": {}, "1": {"y": 90}, "2": {"y": 180}, "3": {"y": 270}},
                         "inventory": [{"model": M("cell_%03d" % TABLES[0][(1, 1)][0])}]}}


C.add("ballpark_painted", 'new BlockBallparkPainted("ballpark_painted", BallparkLayouts.FIELD)',
      ("Ballpark Clay, Painted Marking", "Spielfeldlehm, gemalte Markierung",
       "Arcilla de Campo, Marca Pintada", "Bollplanslera, Målad Markering"),
      {}, painted_state(), tab=TAB)


# ------------------------------------------------------------------------------------------
# The pitcher's mound: a heightfield a cell, round the rubber's cell
# ------------------------------------------------------------------------------------------
BB = FIELDS["baseball"]["field"]
MOUND_ANCHOR, MOUND_CELLS = bf.mound_cells(BB)
H_TOP = 4.0                      # 10 in, in pixels
TABLE_HALF = 2.5 * bf.FT         # the table is 5 ft wide
TABLE_FRONT = BB.rubber_front[1] + 6 * bf.IN       # 6 in in front of the rubber, towards home
TABLE_BACK = TABLE_FRONT - 34 * bf.IN


def mound_height(x, z):
    """The mound's height in pixels at a world point (blocks)."""
    dx = max(0.0, abs(x - 0.5) - TABLE_HALF)
    dz = max(0.0, TABLE_BACK - z, z - TABLE_FRONT)
    d = math.hypot(dx, dz)
    r = math.hypot(x - BB.mound[0], z - BB.mound[1])
    rest = BB.mound_r - r
    if rest <= 0:
        return 0.0
    if d <= 0:
        return H_TOP
    t = d / (d + rest)
    return H_TOP * (1 - t) ** 1.35


RUBBER_BOX = (0.5 - 12 * bf.IN, 0.5 + 12 * bf.IN, BB.rubber_front[1] - 6 * bf.IN,
              BB.rubber_front[1])
GRID = 8


def mound_polys(cx, cz):
    """A mound cell: its heightfield top (a picture mapped flat on it), and its sides and
    bottom as a full block's. Pixels in the cell's own frame."""
    P = []
    hs = [[mound_height(cx + i / GRID, cz + j / GRID) for i in range(GRID + 1)]
          for j in range(GRID + 1)]
    s = 16.0 / GRID
    for j in range(GRID):
        for i in range(GRID):
            pts = [(i * s, 16 + hs[j][i], j * s), (i * s, 16 + hs[j + 1][i], (j + 1) * s),
                   ((i + 1) * s, 16 + hs[j + 1][i + 1], (j + 1) * s),
                   ((i + 1) * s, 16 + hs[j][i + 1], j * s)]
            uvs = [(p[0] / 16.0, p[2] / 16.0) for p in pts]
            # the quad's normal from its diagonals
            a = (pts[2][0] - pts[0][0], pts[2][1] - pts[0][1], pts[2][2] - pts[0][2])
            b = (pts[3][0] - pts[1][0], pts[3][1] - pts[1][1], pts[3][2] - pts[1][2])
            n = (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
            if n[1] < 0:
                n = (-n[0], -n[1], -n[2])
            P.append(vc.Poly(pts, "top", n, uvs))
    for side in ("n", "s", "e", "w"):
        P += [p for p in vc.obox(0, 16, 0, 16, 0, 16, "side", side)]
    P += vc.obox(0, 16, 0, 16, 0, 16, "side", "d")
    if (cx, cz) == MOUND_ANCHOR:
        x0, x1, z0, z1 = RUBBER_BOX
        h = H_TOP + 0.35
        P += vc.obox((x0 - cx) * 16, (x1 - cx) * 16, 16 + H_TOP - 0.5, 16 + h,
                     (z0 - cz) * 16, (z1 - cz) * 16, "rubber", "nsewu")
    return P


MOUND_TABLE = {}        # (i, j) -> cell index
MOUND_HMAX = []
_ax, _az = MOUND_ANCHOR
for _c in sorted(MOUND_CELLS, key=lambda c: (c[1], c[0])):
    _idx = len(MOUND_HMAX)
    MOUND_TABLE[(_c[0] - _ax, -(_c[1] - _az))] = _idx
    _hm = max(mound_height(_c[0] + i / 8.0, _c[1] + j / 8.0) for i in range(9) for j in range(9))
    if _c == MOUND_ANCHOR:
        _hm = H_TOP + 0.35
    MOUND_HMAX.append(int(math.ceil(_hm)))
    C.textures["mound_%02d" % _idx] = (lambda c: (lambda: compose(cell_cov("baseball", c))))(_c)
    _mtl = "mound_%02d" % _idx
    C.extra["models/block/venues/ballpark/%s.mtl" % _mtl] = vc.mtl_text(GEN, {
        "top": T(_mtl), "side": SIDE, "rubber": T("rubber_white")})
    _o = vc.Obj(GEN, _mtl, _mtl)
    for _poly in mound_polys(*_c):
        _o.add(_poly)
    C.extra["models/block/venues/ballpark/%s.obj" % _mtl] = _o.text()

C.textures["rubber_white"] = lambda: vc.swatch(RUBBER, 711, grain=2)


def mound_state(anchor):
    n = len(MOUND_HMAX)
    first = MOUND_TABLE[(0, 0)]
    if anchor:
        variants = {"facing": {f: ys(y) for f, y in FACINGS},
                    "inventory": [{"model": M("mound_item")}]}
        return {"forge_marker": 1, "defaults": {"model": M("mound_%02d" % first) + ".obj"},
                "variants": variants}
    return {"forge_marker": 1, "defaults": {"model": M("mound_%02d" % first) + ".obj"},
            "variants": {"cell": {str(k): {"model": M("mound_%02d" % k) + ".obj"}
                                  for k in range(n)},
                         "rot": {"0": {}, "1": {"y": 90}, "2": {"y": 180}, "3": {"y": 270}},
                         "inventory": [{"model": M("mound_item")}]}}


# the items show a plain cube topped with the rubber's cell: an OBJ item has no GUI transform
MOUND_ITEM = {"mound_item": floor_model(T("mound_%02d" % MOUND_TABLE[(0, 0)]))}

C.add("pitchers_mound", 'new BlockPitchersMound("pitchers_mound")',
      ("Pitcher's Mound (Rubber)", "Pitcherhügel (Pitcherplatte)",
       "Montículo del Lanzador (Goma)", "Kastkulle (Kastplatta)"),
      MOUND_ITEM, mound_state(True), tab=TAB)
C.add("pitchers_mound_slope",
      'new BlockMoundSlope("pitchers_mound_slope", BallparkLayouts.MOUND, '
      'BallparkLayouts.MOUND_HEIGHT)',
      ("Pitcher's Mound Slope", "Pitcherhügel-Hang", "Ladera del Montículo",
       "Kastkullens Sluttning"),
      {}, mound_state(False), tab=TAB)


# ------------------------------------------------------------------------------------------
# BallparkLayouts.java
# ------------------------------------------------------------------------------------------
def wrap(text):
    wrapped = []
    for line in text.split("\n"):
        while len(line) > 100:
            cut = line.rfind(", ", 0, 99)
            wrapped.append(line[:cut + 1])
            line = "          " + line[cut + 2:]
        wrapped.append(line)
    return "\n".join(wrapped)


def layouts_java():
    out = ["package com.micatechnologies.minecraft.csm.venues.ballpark;\n",
           "/**",
           " * Where every painted ballpark cell sits round its anchor, and the pitcher's mound's",
           " * cells round the rubber: generated by {@code dev-env-utils/scripts/gen_venues_ballpark.py}",
           " * from the field it draws ({@code ballpark_field.py}), so the models and this table cannot",
           " * disagree. Do not edit.",
           " *",
           " * <p>{@link #FIELD}: for each anchor group (0 the marking anchor, 1 the infield arc",
           " * anchor, 2 the softball anchor) and kind, quadruplets of {@code i} (cells to the",
           " * anchor's right), {@code j} (cells forward), the painted block's {@code cell} there and",
           " * the cell its third base side twin shows there, mirrored. {@link #MOUND}: the mound's",
           " * cells round the rubber, {@link #MOUND_HEIGHT} each cell's highest point in pixels.</p>",
           " *",
           " * @since 2026.10",
           " */",
           "public final class BallparkLayouts {\n"]
    rows = []
    for t in TABLES:
        trip = []
        for (i, j), (c, mc) in sorted(t.items(), key=lambda kv: (kv[0][1], kv[0][0])):
            trip += [i, j, c, mc]
        rows.append("      new int[]{%s}" % ", ".join(str(x) for x in trip))
    out.append("  /** The field's markings: %d cells (pictures and their mirrors). */"
               % len(CELL_MODELS))
    out.append("  public static final BallparkFamily FIELD = new BallparkFamily(%d, %d, true,"
               % (len(CELL_MODELS), bf.REACH))
    out.append(",\n".join(rows) + ");\n")
    trip = []
    for (i, j), c in sorted(MOUND_TABLE.items(), key=lambda kv: (kv[0][1], kv[0][0])):
        trip += [i, j, c, c]
    out.append("  /** The pitcher's mound: %d cells round the rubber's. */" % len(MOUND_HMAX))
    out.append("  public static final BallparkFamily MOUND = new BallparkFamily(%d, 3, false,"
               % len(MOUND_HMAX))
    out.append("      new int[]{%s});\n" % ", ".join(str(x) for x in trip))
    out.append("  /** Each mound cell's highest point over its block, in pixels. */")
    out.append("  public static final int[] MOUND_HEIGHT = {%s};\n"
               % ", ".join(str(h) for h in MOUND_HMAX))
    out.append("  private BallparkLayouts() {")
    out.append("  }")
    out.append("}")
    return wrap("\n".join(out) + "\n")


JAVA["BallparkLayouts.java"] = layouts_java()


# ------------------------------------------------------------------------------------------
# Fixtures: home plate, the bases, the rubber
# ------------------------------------------------------------------------------------------
@C.texture("canvas")
def _canvas():
    img = vc.swatch((246, 246, 242), 720, grain=3)
    vc.frame(img, 0, 0, 16, 16, (214, 214, 208))
    return img


@C.texture("canvas_orange")
def _canvas_orange():
    img = vc.swatch((240, 110, 30), 721, grain=3)
    vc.frame(img, 0, 0, 16, 16, (204, 88, 22))
    return img


C.textures["rubber_black"] = lambda: vc.swatch((30, 30, 32), 722, grain=2)

PLATE_W = 17 * bf.IN * 16       # 6.91 px
PLATE_SIDE = 8.5 * bf.IN * 16   # 3.45 px
PLATE_DEPTH = 17 * bf.IN * 16


def plate_outline(dz=0.0, inset=0.0):
    """Home plate seen from above, its point at z 16 (+ dz): front edge 17 in, straight sides
    8.5 in, two 12 in edges to the point. Inset shrinks it all round."""
    h = PLATE_W / 2.0 - inset
    zf = 16 - PLATE_DEPTH + dz + inset
    zs = zf + PLATE_SIDE - inset * 0.4
    tip = 16 + dz - inset * 1.41
    return [(8 - h, zf), (8 + h, zf), (8 + h, zs), (8, tip), (8 - h, zs)]


def plate_polys(dz):
    out = []
    pts = plate_outline(dz)
    for a, b in zip(pts, pts[1:] + pts[:1]):
        n = (b[1] - a[1], -(b[0] - a[0]))
        # outward: away from the plate's middle
        mx = sum(p[0] for p in pts) / 5.0
        mz = sum(p[1] for p in pts) / 5.0
        if n[0] * ((a[0] + b[0]) / 2 - mx) + n[1] * ((a[1] + b[1]) / 2 - mz) < 0:
            n = (-n[0], -n[1])
        out.append(vc.ovquad(a, b, 0, 0.45, "black", n))
    out.append(vc.ohpoly(pts, 0.45, "black"))
    out.append(vc.ohpoly(plate_outline(dz, 0.35), 0.5, "white"))
    return out


def write_obj(name, mtl, polys):
    o = vc.Obj(GEN, mtl, name)
    for p in polys:
        o.add(p)
    C.extra["models/block/venues/ballpark/%s.obj" % name] = o.text()


C.extra["models/block/venues/ballpark/plate.mtl"] = vc.mtl_text(GEN, {
    "white": T("rubber_white"), "black": T("rubber_black")})
write_obj("home_plate", "plate", plate_polys(0.0))
write_obj("home_plate_centred", "plate", plate_polys(-(16 - PLATE_DEPTH) / 2.0))
C.add("home_plate", 'new BlockBallparkFixture("home_plate", BlockBallparkFixture.Kind.HOME_PLATE)',
      ("Home Plate", "Home Plate", "Plato de Home", "Hemplatta"),
      {"home_plate_item": vc.model({"white": T("rubber_white"), "black": T("rubber_black"),
                                    "particle": T("rubber_white")},
                                   [box([4.5, 0, 6], [11.5, 1, 13], "white")])},
      {"forge_marker": 1,
       "defaults": {"model": M("home_plate") + ".obj"},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "variant": {"0": {}, "1": {"model": M("home_plate_centred") + ".obj"},
                                "2": {}, "3": {}},
                    "inventory": [{"model": M("home_plate_item")}]}},
      item=item_of("home_plate_item", 1.0, 2), tab=TAB)


def bag_offsets(kind):
    f = FIELDS[kind]["field"]
    out = []
    for name in ("first", "second", "third"):
        bx, bz = f.bags[name]
        cx, cz = math.floor(bx), math.floor(bz)
        out.append(((bx - cx) * 16, (bz - cz) * 16))
    return out


BAG = 15 * bf.IN * 16           # 6.1 px
BAG_H = 1.6


def bag(cx, cz, tex="canvas", turn=True):
    h = BAG / 2.0
    el = box([cx - h, 0, cz - h], [cx + h, BAG_H, cz + h], tex)
    if turn:
        rot(el, "y", 45, [cx, 0, cz])
    return el


BASE_TEX = {"canvas": T("canvas"), "orange": T("canvas_orange"), "particle": T("canvas")}


def base_models(prefix, kind, double_first):
    models = {}
    offs = bag_offsets(kind)
    for i, (ox, oz) in enumerate(offs):
        els = [bag(ox, oz)]
        if i == 0 and double_first:
            # the orange safety bag beside the white one, in foul ground: 15 in out along the
            # foul line's normal, (1, 1) / sqrt 2
            d = BAG / math.sqrt(2)
            els.append(bag(ox + d, oz + d, "orange"))
        models["%s_%d" % (prefix, i)] = vc.model(BASE_TEX, els)
    models["%s_3" % prefix] = vc.model(BASE_TEX, [bag(8, 8, turn=False)])
    return models, offs


def base_state(prefix):
    return {"forge_marker": 1, "defaults": {"model": M(prefix + "_3")},
            "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                         "variant": {str(i): {"model": M("%s_%d" % (prefix, i))}
                                     for i in range(4)},
                         "inventory": [{"model": M(prefix + "_3")}]}}


_bm, BB_OFFS = base_models("base", "baseball", False)
C.add("baseball_base", 'new BlockBallparkFixture("baseball_base", BlockBallparkFixture.Kind.BASE)',
      ("Baseball Base", "Baseball-Base", "Base de Béisbol", "Basebollbas"),
      _bm, base_state("base"), item=item_of("base_3", 3.0, 5), tab=TAB)
_sm, SB_OFFS = base_models("sbase", "softball", True)
C.add("softball_base", 'new BlockBallparkFixture("softball_base", BlockBallparkFixture.Kind.BASE)',
      ("Softball Base", "Softball-Base", "Base de Sóftbol", "Softbollsbas"),
      dict(_sm, sbase_item=vc.model(BASE_TEX, [bag(6.5, 6.5), bag(9.5, 9.5, "orange")])),
      base_state("sbase"), item=item_of("sbase_item", 2.4, 5), tab=TAB)
DOC["bag_offsets_px"] = {"baseball": BB_OFFS, "softball": SB_OFFS}

RUB_W, RUB_D = 24 * bf.IN * 16, 6 * bf.IN * 16
C.add("pitchers_rubber",
      'new BlockBallparkFixture("pitchers_rubber", BlockBallparkFixture.Kind.RUBBER)',
      ("Pitcher's Rubber", "Pitcherplatte", "Goma del Lanzador", "Kastplatta"),
      {"rubber": vc.model({"w": T("rubber_white"), "particle": T("rubber_white")},
                          [box([8 - RUB_W / 2, 0, 8 - RUB_D / 2], [8 + RUB_W / 2, 0.6,
                                                                    8 + RUB_D / 2], "w")])},
      vc.facing_state(M("rubber")), item=item_of("rubber", 2.6, 5), tab=TAB)


# ------------------------------------------------------------------------------------------
# The dugout: bench, rail and canopy runs (BlockArenaRun), racks and the water cooler
# ------------------------------------------------------------------------------------------
C.textures["alu"] = lambda: vc.brushed((200, 204, 206), 730)
C.textures["frame_green"] = lambda: vc.swatch(PAD_GREEN, 731, grain=2)
C.textures["pad_navy"] = lambda: vc.swatch(NAVY, 732, grain=2)
C.textures["steel"] = lambda: vc.brushed(STEEL, 733)
C.textures["steel_dark"] = lambda: vc.brushed(STEEL_DARK, 734)
C.textures["black"] = lambda: vc.swatch(BLACK, 735, grain=2)
C.textures["yellow"] = lambda: vc.swatch(YELLOW, 736, grain=3)
C.textures["concrete"] = lambda: vc.swatch(CONCRETE, 737, grain=5)


@C.texture("roof_deck")
def _roof_deck():
    img = vc.swatch((70, 74, 78), 738, grain=2)
    for x in range(0, 16, 4):
        vc.rect(img, x, 0, x + 1, 16, (54, 58, 62))
    return img


RUN_TEX = {"alu": T("alu"), "frame": T("frame_green"), "pad": T("pad_navy"),
           "steel": T("steel"), "roof": T("roof_deck"), "particle": T("frame_green")}


def dugout_bench_parts():
    els = []
    for z0 in (3, 6, 9):
        els.append(box([0, 8, z0], [16, 9.25, z0 + 2.5], "alu"))
    els += [box([0, 12, 13], [16, 14.5, 14.25], "alu"),
            box([0, 15.5, 13], [16, 18, 14.25], "alu"),
            box([0, 7, 4], [16, 8, 5], "frame", faces=("north", "south", "down")),
            box([0, 7, 10], [16, 8, 11], "frame", faces=("north", "south", "down")),
            box([7.25, 0, 7], [8.75, 7, 8.5], "frame"),
            box([7.25, 0, 14.25], [8.75, 18, 15.5], "frame"),
            box([5, 0, 6.5], [11, 0.75, 15.5], "frame")]
    return els


def dugout_bench_end():
    return [box([0, 0, 3], [1.25, 8, 14.25], "frame"),
            box([0, 8, 13], [1.25, 18.5, 15.5], "frame")]


def rail_parts():
    return [box([0, 14, 6], [16, 18, 10], "pad"),
            box([0, 12.5, 7], [16, 14, 9], "steel"),
            box([0, 6, 7.5], [16, 7, 8.5], "steel", faces=("north", "south", "up", "down")),
            box([7.25, 0, 7.25], [8.75, 12.5, 8.75], "steel"),
            box([6.5, 0, 6.5], [9.5, 0.5, 9.5], "steel")]


def rail_end():
    return [box([0, 0, 7.25], [1.5, 14, 8.75], "steel"),
            box([0, 14, 6], [1, 18, 10], "pad")]


def canopy_parts():
    return [box([0, 13.5, 0], [16, 15, 16], "roof"),
            box([0, 15, 0], [16, 16, 16], "steel", faces=("up", "north", "south")),
            box([0, 11, 0], [16, 16, 1.5], "pad")]


def canopy_end():
    return [box([0, 11, 0], [1, 16, 16], "pad")]


def run_models(prefix, parts, end):
    return {prefix: vc.model(RUN_TEX, parts),
            prefix + "_end_l": vc.model(RUN_TEX, end),
            prefix + "_end_r": vc.model(RUN_TEX, [vc.mirror_x(e) for e in end]),
            prefix + "_item": vc.model(RUN_TEX, parts + end + [vc.mirror_x(e) for e in end])}


def run_state(prefix):
    rules = []
    for facing, y in FACINGS:
        def ap(name):
            return dict({"model": M(name)}, **ys(y))
        rules.append({"when": {"facing": facing}, "apply": ap(prefix)})
        rules.append({"when": {"facing": facing, "end_l": "true"}, "apply": ap(prefix + "_end_l")})
        rules.append({"when": {"facing": facing, "end_r": "true"}, "apply": ap(prefix + "_end_r")})
    return {"multipart": rules}


for _reg, _kind, _names, _parts, _end in (
        ("dugout_bench", "DUGOUT_BENCH",
         ("Dugout Bench", "Dugout-Bank", "Banca del Dugout", "Dugoutbänk"),
         dugout_bench_parts(), dugout_bench_end()),
        ("dugout_rail", "DUGOUT_RAIL",
         ("Dugout Rail", "Dugout-Geländer", "Baranda del Dugout", "Dugoutrigel"),
         rail_parts(), rail_end()),
        ("dugout_canopy", "DUGOUT_CANOPY",
         ("Dugout Canopy", "Dugout-Dach", "Techo del Dugout", "Dugouttak"),
         canopy_parts(), canopy_end())):
    C.add(_reg, 'new BlockArenaRun("%s", BlockArenaRun.Kind.%s)' % (_reg, _kind), _names,
          run_models(_reg, _parts, _end), run_state(_reg),
          item={"parent": "csm:block/venues/ballpark/%s_item" % _reg}, tab=TAB)


@C.texture("helmet")
def _helmet():
    img = vc.swatch(NAVY, 740, grain=2)
    vc.rect(img, 3, 3, 6, 5, vc.shade(NAVY, 1.5))
    return img


@C.texture("cubby")
def _cubby():
    img = vc.swatch((52, 56, 60), 741, grain=2)
    vc.frame(img, 0, 0, 16, 16, (36, 38, 42))
    return img


@C.texture("bat")
def _bat():
    img = vc.swatch((196, 156, 104), 742, grain=4)
    vc.rect(img, 0, 0, 16, 3, (34, 34, 36))
    return img


@C.texture("cooler")
def _cooler():
    img = vc.swatch((236, 112, 26), 743, grain=3)
    vc.rect(img, 0, 2, 16, 3, vc.shade((236, 112, 26), 0.8))
    vc.rect(img, 0, 13, 16, 14, vc.shade((236, 112, 26), 0.8))
    return img


FIX_TEX = {"cubby": T("cubby"), "helmet": T("helmet"), "black": T("black"), "bat": T("bat"),
           "steel": T("steel"), "steel_dark": T("steel_dark"), "cooler": T("cooler"),
           "white": T("rubber_white"), "frame": T("frame_green"), "particle": T("cubby")}


def helmet(x, y, z):
    """A batting helmet in a cubby, its ear flap to one side: a rounded shell and a peak."""
    return [box([x, y, z], [x + 3.5, y + 2.6, z + 4], "helmet"),
            box([x + 0.5, y + 2.6, z + 0.5], [x + 3, y + 3.2, z + 3.5], "helmet"),
            box([x + 0.3, y + 0.8, z - 0.8], [x + 3.2, y + 1.3, z], "black"),
            box([x - 0.3, y, z + 1.5], [x, y + 1.8, z + 3.2], "helmet")]


def helmet_rack():
    els = [box([0.5, 2, 10], [15.5, 15, 16], "cubby", faces=("north", "east", "west", "up",
                                                              "down")),
           box([0.5, 8.25, 10], [15.5, 8.75, 16], "steel_dark", faces=("up", "down", "north"))]
    for x in (5.25, 10.25):
        els.append(box([x, 2, 10], [x + 0.5, 15, 16], "steel_dark", faces=("east", "west",
                                                                           "north")))
    for y in (2.5, 8.75):
        for x in (1.25, 6.25, 11.25):
            els += helmet(x, y, 11)
    return els


def bat_rack():
    els = [box([1, 0, 11], [15, 2.5, 15.5], "frame"),
           box([1, 11, 13.5], [15, 12.5, 15.5], "frame"),
           box([1, 2.5, 14.5], [2, 11, 15.5], "frame"),
           box([14, 2.5, 14.5], [15, 11, 15.5], "frame")]
    for i in range(7):
        x = 2.4 + i * 1.65
        els.append(box([x, 1, 12.6], [x + 1.1, 9.5, 13.7], "bat"))
        els.append(box([x + 0.2, 9.5, 12.8], [x + 0.9, 14, 13.5], "bat"))
        els.append(box([x + 0.05, 14, 12.65], [x + 1.05, 14.6, 13.65], "black"))
    return els


def water_cooler():
    els = [box([3, 0, 4], [13, 7, 12], "steel_dark", faces=("north", "south", "east", "west",
                                                             "up")),
           box([3.5, 0, 4.5], [4.5, 7, 5.5], "steel"), box([11.5, 0, 4.5], [12.5, 7, 5.5],
                                                              "steel")]
    els += lsc.post(8, 8, 4.2, 7, 14, "cooler", top=False)
    els += lsc.post(8, 8, 4.4, 14, 15.5, "white")
    els += [box([7.25, 8.5, 2.6], [8.75, 9.5, 4.2], "black"),
            box([7.5, 7.6, 2.8], [8.5, 8.5, 3.6], "black")]
    els += lsc.post(13.6, 8, 1.1, 7, 14.5, "white")
    return els


for _reg, _kind, _names, _els, _scale in (
        ("dugout_helmet_rack", "HELMET_RACK",
         ("Helmet Rack", "Helmregal", "Estante de Cascos", "Hjälmhylla"), helmet_rack(), 0.9),
        ("dugout_bat_rack", "BAT_RACK",
         ("Bat Rack", "Schlägerständer", "Bastonera", "Trästället"), bat_rack(), 0.9),
        ("dugout_water_cooler", "WATER_COOLER",
         ("Dugout Water Cooler", "Dugout-Wasserkühler", "Hielera del Dugout",
          "Vattenkylare för Dugout"), water_cooler(), 1.0)):
    _m = _reg.replace("dugout_", "")
    C.add(_reg, 'new BlockBallparkFixture("%s", BlockBallparkFixture.Kind.%s)' % (_reg, _kind),
          _names, {_m: vc.model(FIX_TEX, _els)}, vc.facing_state(M(_m)),
          item=item_of(_m, _scale), tab=TAB)


# ------------------------------------------------------------------------------------------
# Nets: backstop and batting cage panels, the roof net
# ------------------------------------------------------------------------------------------
@C.texture("net")
def _net():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(0, 16, 4):
        vc.rect(img, i, 0, i + 1, 16, rgba(NET))
        vc.rect(img, 0, i, 16, i + 1, rgba(NET))
    return img


C.textures["galv"] = lambda: vc.brushed((172, 178, 182), 750)
NET_TEX = {"net": T("net"), "pole": T("galv"), "cable": T("black"), "particle": T("galv")}


def net_plane():
    return [{"from": [0, 0, 8], "to": [16, 16, 8], "shade": False,
             "faces": {"north": {"texture": "#net", "uv": [0, 0, 16, 16]},
                       "south": {"texture": "#net", "uv": [0, 0, 16, 16]}}}]


def net_models():
    plane = net_plane()
    left = lsc.post(1.25, 8, 1.25, 0, 16, "pole", top=False)
    right = lsc.post(14.75, 8, 1.25, 0, 16, "pole", top=False)
    top = [box([0, 15.25, 7.6], [16, 16, 8.4], "cable")]
    return {"net": vc.model(NET_TEX, plane), "net_pole_l": vc.model(NET_TEX, left),
            "net_pole_r": vc.model(NET_TEX, right), "net_top": vc.model(NET_TEX, top),
            "net_item": vc.model(NET_TEX, plane + left + right + top)}


def net_state():
    rules = []
    for facing, y in FACINGS:
        w = {"facing": facing}

        def ap(name):
            return dict({"model": M(name)}, **ys(y))
        rules.append({"when": dict(w), "apply": ap("net")})
        rules.append({"when": {"OR": [dict(w, pole="true"), dict(w, end_l="true")]},
                      "apply": ap("net_pole_l")})
        rules.append({"when": dict(w, end_r="true"), "apply": ap("net_pole_r")})
        rules.append({"when": dict(w, top="true"), "apply": ap("net_top")})
    return {"multipart": rules}


C.add("ballpark_net", 'new BlockBallparkNet("ballpark_net", false)',
      ("Backstop Net", "Fangnetz", "Red de Protección", "Skyddsnät"),
      net_models(), net_state(), item={"parent": "csm:block/venues/ballpark/net_item"}, tab=TAB)

C.extra["models/block/venues/ballpark/net_diagonal.mtl"] = vc.mtl_text(GEN, {
    "net": T("net"), "pole": T("galv"), "cable": T("black")})


def diagonal_net_polys(pole, top):
    """The net across the cell from its north-west corner to its south-east one, drawn as at a
    rink's north-west corner (facing north: the net looks north-east)."""
    P = []
    a, b = (0.0, 0.0), (16.0, 16.0)
    span = math.hypot(16, 16) / 16.0
    q = vc.ovquad(a, b, 0, 16, "net", (1, -1))
    q.uvs = [(0, 1), (span, 1), (span, 0), (0, 0)]
    P.append(q)
    q2 = vc.ovquad(b, a, 0, 16, "net", (-1, 1))
    q2.uvs = [(0, 1), (span, 1), (span, 0), (0, 0)]
    P.append(q2)
    if pole:
        P += vc.opost(8, 8, 1.25, 0, 16, "pole", cap=False)
    if top:
        d = 0.5
        P += [vc.ovquad((a[0] + d, a[1] - d), (b[0] + d, b[1] - d), 15.25, 16, "cable", (1, -1)),
              vc.ovquad((b[0] - d, b[1] + d), (a[0] - d, a[1] + d), 15.25, 16, "cable", (-1, 1)),
              vc.ohpoly([(a[0] + d, a[1] - d), (b[0] + d, b[1] - d), (b[0] - d, b[1] + d),
                         (a[0] - d, a[1] + d)], 16, "cable")]
    return P


for _pole in (False, True):
    for _top in (False, True):
        write_obj("net_diagonal_%d%d" % (_pole, _top), "net_diagonal",
                  diagonal_net_polys(_pole, _top))
_dn = {"forge_marker": 1,
       "defaults": {"model": M("net_diagonal_00") + ".obj", "custom": {"ambient": False}},
       "variants": {}}
for _f, _y in FACINGS:
    for _pole in (False, True):
        for _top in (False, True):
            _dn["variants"]["facing=%s,pole=%s,top=%s" % (_f, str(_pole).lower(),
                                                          str(_top).lower())] = [
                dict({"model": M("net_diagonal_%d%d" % (_pole, _top)) + ".obj"}, **ys(_y))]
_dn["variants"]["inventory"] = [{"model": M("net_item"), "custom": {}}]
C.add("ballpark_net_diagonal", 'new BlockBallparkNet("ballpark_net_diagonal", true)',
      ("Backstop Net (45 Degree)", "Fangnetz (45 Grad)", "Red de Protección (45 Grados)",
       "Skyddsnät (45 Grader)"), {}, _dn, tab=TAB)

C.add("ballpark_net_roof",
      'new BlockBallparkFixture("ballpark_net_roof", BlockBallparkFixture.Kind.NET_ROOF)',
      ("Batting Cage Roof Net", "Dachnetz (Schlagkäfig)", "Red de Techo de Jaula de Bateo",
       "Taknät för Slagbur"),
      {"net_roof": vc.model(NET_TEX, [{"from": [0, 15.5, 0], "to": [16, 15.5, 16],
                                       "shade": False,
                                       "faces": {"up": {"texture": "#net",
                                                        "uv": [0, 0, 16, 16]},
                                                 "down": {"texture": "#net",
                                                          "uv": [0, 0, 16, 16]}}}])},
      vc.facing_state(M("net_roof")), item=item_of("net_roof", 1.0), tab=TAB)


# ------------------------------------------------------------------------------------------
# The outfield wall, its distance marker, the foul pole
# ------------------------------------------------------------------------------------------
@C.texture("wall_pad")
def _wall_pad():
    img = vc.swatch(PAD_GREEN, 760, grain=2)
    vc.rect(img, 0, 0, 1, 16, vc.shade(PAD_GREEN, 0.75))
    vc.rect(img, 0, 0, 16, 1, vc.shade(PAD_GREEN, 1.15))
    return img


@C.texture("wall_back")
def _wall_back():
    img = vc.swatch((54, 70, 60), 761, grain=3)
    for y in (5, 11):
        vc.rect(img, 0, y, 16, y + 1, (44, 58, 50))
    return img


WALL_TEX = {"pad": T("wall_pad"), "back": T("wall_back"), "yellow": T("yellow"),
            "particle": T("wall_pad")}
PAD_T = 3.5


def wall_models():
    body = [box([0, 0, 0], [16, 16, PAD_T], "pad", faces=("north", "east", "west", "up",
                                                          "down")),
            box([0, 0, PAD_T], [16, 16, 16], "back", faces=("south", "east", "west", "up",
                                                             "down"))]
    top = [box([0, 14.5, -0.05], [16, 16, PAD_T], "yellow", faces=("north", "up")),
           ]
    return {"wall": vc.model(WALL_TEX, body), "wall_top": vc.model(WALL_TEX, top),
            "wall_item": vc.model(WALL_TEX, body + top)}


def wall_state(prefix):
    rules = []
    for facing, y in FACINGS:
        rules.append({"when": {"facing": facing}, "apply": dict({"model": M(prefix)}, **ys(y))})
        rules.append({"when": {"facing": facing, "top": "true"},
                      "apply": dict({"model": M(prefix + "_top")}, **ys(y))})
    return {"multipart": rules}


C.add("outfield_wall", 'new BlockOutfieldWall("outfield_wall", false)',
      ("Outfield Wall", "Outfield-Wand", "Barda de los Jardines", "Utmarksvägg"),
      wall_models(), wall_state("wall"), item={"parent": "csm:block/venues/ballpark/wall_item"},
      tab=TAB)

C.extra["models/block/venues/ballpark/wall_diagonal.mtl"] = vc.mtl_text(GEN, {
    "pad": T("wall_pad"), "back": T("wall_back"), "yellow": T("yellow")})


WALL_T = 12.0      # the 45 degree wall's thickness behind its face, pad included


def diagonal_wall_polys(top):
    """The wall across its cell from the north-west corner to the south-east one, its padded
    face on that diagonal looking north-east (the dasher corner's convention: facing north looks
    north-east), the pad and the wall behind it a band to the south-west, its ends square to
    the face at the cell's corners, so a 45 degree run is one straight wall front and back
    (the band reaches past its cell to do it)."""
    P = []
    k = 1 / math.sqrt(2)

    def back(t):
        return lambda p: (p[0] - t * k, p[1] + t * k)
    a, b = (0.0, 0.0), (16.0, 16.0)
    a1, b1 = back(PAD_T)(a), back(PAD_T)(b)
    a2, b2 = back(WALL_T)(a), back(WALL_T)(b)
    span = math.hypot(16, 16) / 16.0
    q = vc.ovquad(a, b, 0, 16, "pad", (1, -1))
    q.uvs = [(0, 1), (span, 1), (span, 0), (0, 0)]
    P.append(q)
    q = vc.ovquad(b2, a2, 0, 16, "back", (-1, 1))
    q.uvs = [(0, 1), (span, 1), (span, 0), (0, 0)]
    P.append(q)
    pad = [a, b, b1, a1]
    body = [a1, b1, b2, a2]
    for poly, mat in ((pad, "pad"), (body, "back")):
        P.append(vc.ohpoly(poly, 16, mat))
        P.append(vc.ohpoly(poly, 0, mat, up=False))
    # the ends, square to the face: drawn, since a run's end shows them
    P.append(vc.ovquad(a2, a, 0, 16, "back", (-1, -1)))
    P.append(vc.ovquad(b, b2, 0, 16, "back", (1, 1)))
    if top:
        d = 0.05
        P.append(vc.ovquad((d, -d), (16 + d, 16 - d), 14.5, 16, "yellow", (1, -1)))
        P.append(vc.ohpoly(pad, 16.05, "yellow"))
    return P


for _top in (False, True):
    write_obj("wall_diagonal" + ("_top" if _top else ""), "wall_diagonal",
              diagonal_wall_polys(_top))


def diag_wall_state():
    rules = []
    for facing, y in FACINGS:
        rules.append({"when": {"facing": facing},
                      "apply": dict({"model": M("wall_diagonal") + ".obj"}, **ys(y))})
        rules.append({"when": {"facing": facing, "top": "true"},
                      "apply": dict({"model": M("wall_diagonal_top") + ".obj"}, **ys(y))})
    return {"multipart": rules}


C.add("outfield_wall_diagonal", 'new BlockOutfieldWall("outfield_wall_diagonal", true)',
      ("Outfield Wall (45 Degree)", "Outfield-Wand (45 Grad)",
       "Barda de los Jardines (45 Grados)", "Utmarksvägg (45 Grader)"), {}, diag_wall_state(),
      item={"parent": "csm:block/venues/ballpark/wall_item"}, tab=TAB)


def marker_state():
    rules = []
    for facing, y in FACINGS:
        for diag in ("false", "true"):
            base = "wall_diagonal" if diag == "true" else "wall"
            ext = ".obj" if diag == "true" else ""
            w = {"facing": facing, "diagonal": diag}
            rules.append({"when": dict(w), "apply": dict({"model": M(base) + ext}, **ys(y))})
            rules.append({"when": dict(w, top="true"),
                          "apply": dict({"model": M(base + "_top") + ext}, **ys(y))})
    return {"multipart": rules}


@C.texture("marker_icon")
def _marker_icon():
    img = vc.swatch(PAD_GREEN, 762, grain=2)
    vc.draw_text_centred(img, "400", 8, 5, (246, 246, 240))
    return img


C.add("outfield_wall_marker", 'new BlockOutfieldWallMarker("outfield_wall_marker")',
      ("Outfield Wall Distance Marker", "Outfield-Wand mit Entfernungsangabe",
       "Barda con Marca de Distancia", "Utmarksvägg med Avståndsmarkering"),
      {"marker_item": vc.model(dict(WALL_TEX, face=T("marker_icon")),
                               [box([0, 0, 0], [16, 16, PAD_T], "pad",
                                    per={"north": "face"}),
                                box([0, 0, PAD_T], [16, 16, 16], "back")])},
      marker_state(), item={"parent": "csm:block/venues/ballpark/marker_item",
                            "display": icon_display(0.625, 0, 45)}, tab=TAB)


@C.texture("foul_mesh")
def _foul_mesh():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(0, 16, 3):
        vc.rect(img, i, 0, i + 1, 16, rgba(YELLOW))
        vc.rect(img, 0, i, 16, i + 1, rgba(YELLOW))
    return img


POLE_TEX = {"pole": T("yellow"), "mesh": T("foul_mesh"), "base": T("concrete"),
            "particle": T("yellow")}


def pole_models():
    shaft = lsc.post(8, 8, 3, 0, 16, "pole", top=False)
    cap = lsc.post(8, 8, 3.6, 15, 16.5, "pole")
    base = [box([3, 0, 3], [13, 2, 13], "base")]
    wing = [{"from": [8, 0, -16], "to": [8, 16, 5], "shade": False,
             "faces": {"east": {"texture": "#mesh", "uv": [0, 0, 16, 16]},
                       "west": {"texture": "#mesh", "uv": [0, 0, 16, 16]}}},
            box([7.25, 0, -16], [8.75, 16, -15], "pole")]
    return {"pole": vc.model(POLE_TEX, shaft), "pole_cap": vc.model(POLE_TEX, cap),
            "pole_base": vc.model(POLE_TEX, base), "pole_wing": vc.model(POLE_TEX, wing),
            "pole_item": vc.model(POLE_TEX, shaft + cap + wing)}


def pole_state():
    rules = []
    for facing, y in FACINGS:
        w = {"facing": facing}
        rules.append({"when": dict(w), "apply": dict({"model": M("pole")}, **ys(y))})
        for prop, m in (("cap", "pole_cap"), ("base", "pole_base"), ("wing", "pole_wing")):
            rules.append({"when": dict(w, **{prop: "true"}), "apply": dict({"model": M(m)},
                                                                           **ys(y))})
    return {"multipart": rules}


C.add("foul_pole", 'new BlockFoulPole("foul_pole")',
      ("Foul Pole", "Foul-Pole", "Poste de Foul", "Foulstolpe"),
      pole_models(), pole_state(), item=item_of("pole_item", 0.8), tab=TAB)


# ------------------------------------------------------------------------------------------
# The line score scoreboard and the count board (units; their words are a renderer's)
# ------------------------------------------------------------------------------------------
@C.texture("board_face")
def _board_face():
    img = vc.swatch(BOARD_GREEN, 770, grain=2)
    return img


@C.texture("board_cab")
def _board_cab():
    img = vc.swatch((40, 44, 46), 771, grain=2)
    for y in (0, 8):
        vc.rect(img, 0, y, 16, y + 1, (30, 32, 34))
    return img


SB_MTL = {"face": T("board_face"), "cab": T("board_cab"), "steel": T("steel_dark"),
          "cap": T("steel"), "base": T("concrete")}
C.extra["models/block/venues/ballpark/scoreboard.mtl"] = vc.mtl_text(GEN, SB_MTL)
# shared with TileEntityBallparkScoreboardRenderer: the face's rectangle in the master's frame
SB_LARGE = {"x0": -16, "x1": 176, "y0": 48, "y1": 128, "z0": 2, "z1": 14}
SB_SMALL = {"x0": 0, "x1": 48, "y0": 0, "y1": 32, "z0": 10, "z1": 16}


def board_polys(b, legs):
    P = []
    P += vc.obox(b["x0"], b["x1"], b["y0"], b["y1"], b["z0"], b["z1"], "cab", "sewud")
    P += vc.obox(b["x0"], b["x1"], b["y0"], b["y1"], b["z0"], b["z1"], "face", "n")
    # a cap along the top and a lip round the face
    P += vc.obox(b["x0"] - 1, b["x1"] + 1, b["y1"], b["y1"] + 2, b["z0"] - 1, b["z1"] + 1, "cap")
    P += vc.obox(b["x0"] - 1, b["x1"] + 1, b["y0"] - 1, b["y0"], b["z0"] - 1, b["z1"], "cap")
    P += vc.obox(b["x0"] - 1, b["x0"], b["y0"], b["y1"], b["z0"] - 1, b["z1"], "cap", "nsewd")
    P += vc.obox(b["x1"], b["x1"] + 1, b["y0"], b["y1"], b["z0"] - 1, b["z1"], "cap", "nsewd")
    for (x0, x1) in legs:
        P += vc.obox(x0, x1, 2, b["y0"] - 1, 5, 11, "steel", "nsew")
        P += vc.obox(x0 - 3, x1 + 3, 0, 2, 2, 14, "base")
    return P


SB_LEGS = [(5, 11), (149, 155)]
write_obj("scoreboard", "scoreboard", board_polys(SB_LARGE, SB_LEGS))
write_obj("count_board", "scoreboard", board_polys(SB_SMALL, []))


def unit_cells(boxes_px):
    """The arena generator's rule: every cell a box reaches 6 px into, but the master."""
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


def unit_java(cls, reg, boxes_px, extra=""):
    flat_c = ", ".join("%d, %d, %d" % c for c in unit_cells(boxes_px))
    flat_b = ", ".join(", ".join("%d" % int(round(v)) for v in b) for b in boxes_px)
    return 'new %s("%s"%s, new int[]{%s}, new int[]{%s})' % (cls, reg, extra, flat_c, flat_b)


def sb_boxes(b, legs):
    out = [(b["x0"], b["y0"], b["z0"], b["x1"], b["y1"], b["z1"])]
    for (x0, x1) in legs:
        out.append((x0, 0, 5, x1, b["y0"], 11))
    return out


@C.texture("scoreboard_icon")
def _sb_icon():
    img = vc.swatch(BOARD_GREEN, 772, grain=2)
    for i in range(6):
        vc.rect(img, 4 + i * 2, 3, 5 + i * 2, 5, (230, 230, 220))
        vc.rect(img, 4 + i * 2, 7, 5 + i * 2, 9, (250, 200, 40))
    vc.rect(img, 1, 3, 3, 5, (240, 240, 240))
    vc.rect(img, 1, 7, 3, 9, (240, 240, 240))
    vc.rect(img, 2, 12, 4, 14, (60, 220, 80))
    vc.rect(img, 6, 12, 8, 14, (240, 60, 40))
    vc.rect(img, 10, 12, 12, 14, (240, 60, 40))
    return img


SB_ICON_TEX = {"face": T("scoreboard_icon"), "cab": T("board_cab"), "steel": T("steel_dark"),
               "particle": T("board_cab")}
SB_ICON = vc.model(SB_ICON_TEX, [box([0, 6, 6], [16, 15, 10], "cab", per={"north": "face"}),
                                 box([3, 0, 7], [4, 6, 9], "steel"),
                                 box([12, 0, 7], [13, 6, 9], "steel")])
CB_ICON = vc.model(SB_ICON_TEX, [box([1, 3, 10], [15, 13, 16], "cab", per={"north": "face"})])


def unit_state(obj, icon):
    return {"forge_marker": 1,
            "defaults": {"model": M(obj) + ".obj", "custom": {"ambient": False}},
            "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                         "inventory": [{"model": M(icon), "custom": {}}]}}


C.add("ballpark_scoreboard",
      unit_java("BlockBallparkScoreboard", "ballpark_scoreboard", sb_boxes(SB_LARGE, SB_LEGS),
                ", false"),
      ("Ballpark Scoreboard", "Baseball-Anzeigetafel", "Marcador de Béisbol",
       "Basebollens Resultattavla"),
      {"scoreboard_item": SB_ICON}, unit_state("scoreboard", "scoreboard_item"),
      item=item_of("scoreboard_item", 1.0), tab=TAB)
C.add("ballpark_count_board",
      unit_java("BlockBallparkScoreboard", "ballpark_count_board", sb_boxes(SB_SMALL, []),
                ", true"),
      ("Count and Pitch Speed Board", "Zähl- und Wurfgeschwindigkeitstafel",
       "Tablero de Cuenta y Velocidad", "Räknings- och Kasthastighetstavla"),
      {"count_board_item": CB_ICON}, unit_state("count_board", "count_board_item"),
      item=item_of("count_board_item", 1.0), tab=TAB)
DOC["scoreboard_cells"] = unit_cells(sb_boxes(SB_LARGE, SB_LEGS))


# ------------------------------------------------------------------------------------------
# The pitching machine (decor)
# ------------------------------------------------------------------------------------------
@C.texture("machine")
def _machine():
    img = vc.swatch((40, 74, 140), 780, grain=2)
    vc.rect(img, 0, 0, 16, 1, (60, 96, 170))
    return img


def machine():
    els = [box([2, 0, 7.25], [14, 1, 8.75], "steel_dark"),
           box([7.25, 0, 2], [8.75, 1, 14], "steel_dark"),
           box([7.25, 1, 7.25], [8.75, 9, 8.75], "steel"),
           box([3.5, 9, 5], [12.5, 10.5, 11], "machine"),
           box([7, 10.5, 9], [9, 12.5, 15], "steel"),
           rot(box([7.25, 12.5, 10], [8.75, 13.5, 16.5], "steel"), "x", -22.5, [8, 12.5, 10])]
    els += lsc.post(5.25, 8, 2.6, 10.5, 12.5, "black")
    els += lsc.post(10.75, 8, 2.6, 10.5, 12.5, "black")
    return els


C.add("pitching_machine",
      'new BlockBallparkFixture("pitching_machine", BlockBallparkFixture.Kind.PITCHING_MACHINE)',
      ("Pitching Machine", "Wurfmaschine", "Máquina Lanzadora", "Kastmaskin"),
      {"pitching_machine": vc.model(dict(FIX_TEX, machine=T("machine")), machine())},
      vc.facing_state(M("pitching_machine")), item=item_of("pitching_machine", 1.0), tab=TAB)


# ------------------------------------------------------------------------------------------
# Messages
# ------------------------------------------------------------------------------------------
L = C.add_lang
L("itemGroup.tabvenuesballpark", ("CSM: Sports & Venues - Ballpark",
                                  "CSM: Sport & Veranstaltungsstätten - Ballpark",
                                  "CSM: Deportes y Recintos - Estadio de Béisbol",
                                  "CSM: Sport & Arenor - Bollplan"))
for _key, _names in KIND_NAMES.items():
    L("csm.venues.ballpark.kind." + _key, _names)
for _i, _n in enumerate((("Away team", "Gastmannschaft", "Equipo visitante", "Bortalag"),
                         ("Home team", "Heimmannschaft", "Equipo local", "Hemmalag"),
                         ("Away runs by inning", "Gast: Runs je Inning",
                          "Visitante: carreras", "Borta: poäng per inning"),
                         ("Home runs by inning", "Heim: Runs je Inning",
                          "Local: carreras", "Hemma: poäng per inning"),
                         ("H E away, H E home", "H E Gast, H E Heim",
                          "H E visitante, H E local", "T F borta, T F hemma"),
                         ("Balls strikes outs", "Balls Strikes Outs",
                          "Bolas strikes outs", "Bollar strikes brända"),
                         ("Pitch speed (mph)", "Wurfgeschwindigkeit (mph)",
                          "Velocidad del lanzamiento (mph)", "Kasthastighet (mph)"))):
    L("gui.csm.venue_text.ballpark.%d" % _i, _n)
L("gui.csm.venue_text.choice.away", ("Away colour: %s", "Gastfarbe: %s", "Color visitante: %s",
                                     "Bortafärg: %s"))
L("gui.csm.venue_text.choice.home", ("Home colour: %s", "Heimfarbe: %s", "Color local: %s",
                                     "Hemmafärg: %s"))
L("gui.csm.venue_text.choice.batting", ("Batting: %s", "Am Schlag: %s", "Al bate: %s",
                                        "Slår: %s"))
L("csm.venues.ballpark.batting.0", ("away", "Gast", "visitante", "borta"))
L("csm.venues.ballpark.batting.1", ("home", "Heim", "local", "hemma"))
L("gui.csm.venue_text.marker.0", ("Distance", "Entfernung", "Distancia", "Avstånd"))
L("csm.venues.ballpark.variant", ("Set to: %s", "Eingestellt: %s", "Ajustado: %s", "Inställt: %s"))
for _k, _vals in (("home_plate", (("standard: point on the block's back edge",
                                   "Standard: Spitze an der Hinterkante",
                                   "estándar: punta en el borde trasero",
                                   "standard: spetsen vid bakkanten"),
                                  ("centred", "mittig", "centrado", "centrerad"))),
                  ("base", (("first base", "First Base", "primera base", "första bas"),
                            ("second base", "Second Base", "segunda base", "andra bas"),
                            ("third base", "Third Base", "tercera base", "tredje bas"),
                            ("centred, square to the block", "mittig, achsparallel",
                             "centrada, alineada con el bloque",
                             "centrerad, i linje med blocket")))):
    for _i, _n in enumerate(_vals):
        L("csm.venues.ballpark.variant.%s.%d" % (_k, _i), _n)


# ------------------------------------------------------------------------------------------
# A field's blocks, for a builder (or a build script): --layout baseball|softball
# ------------------------------------------------------------------------------------------
def stripe_dark(field, cx, cz):
    """The layout's mowing: outfield turf in stripes four blocks deep across the field, from
    three blocks past the top of the infield arc."""
    x, z = cx + 0.5, cz + 0.5
    if bf.n1(x, z) > 0 or bf.n3(x, z) > 0:
        return False
    # rows from three blocks past the arc's top: a straight edge, and clear of the arc's
    # pictures, which are drawn in the light tone
    if cz > field.arc_c[1] - field.arc_r - 3.0:
        return False
    return ((-cz) // 4) % 2 == 1


GROUP_OF = {}
for _g, (_block, _kinds) in enumerate(GROUPS):
    for _k, (_fk, _key) in enumerate(_kinds):
        GROUP_OF[(_fk, _key)] = (_block, _k)
# the coach's box and on-deck circle are drawn on plain turf, so softball uses baseball's
GROUP_OF[("softball", "coach")] = GROUP_OF[("baseball", "coach")]
GROUP_OF[("softball", "on_deck")] = GROUP_OF[("baseball", "on_deck")]


def verify_layout(kind, ground):
    """Every painted cell of a field's layout shows, through the anchors round it as the Java
    finds them, exactly the field's drawing there."""
    anchors = {}
    for (c, (b, m)) in ground.items():
        for g, (block, _kinds) in enumerate(GROUPS):
            if b in (block, block + "_third"):
                anchors[c] = (g * 4 + (m >> 2), b.endswith("_third"))
    bad = []
    for (c, (b, m)) in ground.items():
        if b != "ballpark_painted":
            continue
        found = None
        for ring in range(1, bf.REACH + 1):
            for dz in range(-ring, ring + 1):
                for dx in range(-ring, ring + 1):
                    if max(abs(dx), abs(dz)) != ring:
                        continue
                    a = (c[0] + dx, c[1] + dz)
                    if a not in anchors:
                        continue
                    kidx, third = anchors[a]
                    i, j = c[0] - a[0], -(c[1] - a[1])
                    t = TABLES[kidx].get((-i if third else i, j))
                    if t is not None:
                        found = t[1] if third else t[0]
                        break
                if found is not None:
                    break
            if found is not None:
                break
        if found is None:
            bad.append((c, "no anchor draws it"))
            continue
        p, mir = CELL_MODELS[found]
        pic = PICS[p][:, :, ::-1] if mir else PICS[p]
        d = np.abs(pic - cell_cov(kind, c))
        if d.max() > 0.34 or d.mean() > 0.004:
            bad.append((c, "shows another picture"))
    if bad:
        raise SystemExit("%s layout: %d painted cells wrong, e.g. %s" % (kind, len(bad), bad[:6]))


def layout(kind):
    fd = FIELDS[kind]
    f, cen, anchors = fd["field"], fd["census"], fd["anchors"]
    ground = {}
    for (cx, cz), v in cen.items():
        dark = stripe_dark(f, cx, cz)
        if v[0] == "uniform":
            name = bf.LABELS[v[1]]
            block, k = PIECE_BLOCK[name]
            if name == "turf" and dark:
                k += 1
            ground[(cx, cz)] = (block, 2 | k << 2)
        elif v[0] == "piece":
            block, k = PIECE_BLOCK[v[1]]
            if v[1] in TONED and dark:
                k += 1
            ground[(cx, cz)] = (block, FACING_META[v[2]] | k << 2)
        else:
            ground[(cx, cz)] = ("ballpark_painted", 0)
    for a in anchors:
        block, k = GROUP_OF[(kind, a["key"])]
        if a["mirror"]:
            block += "_third"
        ground[a["cell"]] = (block, 2 | k << 2)
    if fd["mound"] is not None:
        for c in MOUND_CELLS:
            ground[c] = ("pitchers_mound_slope", 0)
        ground[fd["mound"]] = ("pitchers_mound", 2)
    verify_layout(kind, ground)
    objects = [(0, 1, 0, "home_plate", 2, None)]
    base = "baseball_base" if kind == "baseball" else "softball_base"
    for i, name in enumerate(("first", "second", "third")):
        bx, bz = f.bags[name]
        objects.append((int(math.floor(bx)), 1, int(math.floor(bz)), base, 2 | i << 2, None))
    for (x, z, piece, facing) in f.walls():
        block = "outfield_wall" if piece == "straight" else "outfield_wall_diagonal"
        for dy in (1, 2):
            objects.append((x, dy, z, block, FACING_META[facing], None))
    if kind == "baseball":
        for (x, z, text) in ((72, -74, "330"), (-72, -74, "330"), (44, -105, "375"),
                             (-44, -105, "375"), (0, -122, "400")):
            piece = "straight" if abs(x) == 72 or z == -122 else "diagonal"
            facing = {72: "west", -72: "east"}.get(x, "south" if z == -122 else (
                "south" if x > 0 else "east"))
            meta = FACING_META[facing] | (4 if piece == "diagonal" else 0)
            objects.append((x, 1, z, "outfield_wall_marker", meta, '{txt:"%s"}' % text))
        for (x, facing) in ((73, "west"), (-73, "east")):
            for dy in range(1, 19):
                meta = FACING_META[facing] | (4 if dy >= 3 else 0)
                objects.append((x, dy, -73, "foul_pole", meta, None))
    return ground, objects


def layout_json(kind):
    ground, objects = layout(kind)
    return {"field": kind, "frame": "home plate's cell at (0, 0), the field to the north (-z); "
                                     "ground blocks at the floor level, objects dy above it",
            "ground": [[x, z, b, m] for (x, z), (b, m) in sorted(ground.items(),
                                                                 key=lambda kv: (kv[0][1],
                                                                                 kv[0][0]))],
            "objects": [list(o) for o in objects]}


def doc():
    out = {"cells": len(CELL_MODELS), "pictures": len(PICS), "mound_cells": len(MOUND_HMAX),
           "mound_anchor": MOUND_ANCHOR}
    for kind, fd in FIELDS.items():
        rows = []
        for a in fd["anchors"]:
            block, k = GROUP_OF[(kind, a["key"])]
            if a["mirror"]:
                block += "_third"
            rows.append({"name": a["name"], "cell": a["cell"], "block": block,
                         "meta (facing north)": 2 | k << 2, "cells": len(a["cells"])})
        out[kind] = {"anchors": rows, "bags": fd["field"].bags,
                     "foul_line_start": fd["field"].foul_start}
        g, _o = layout(kind)
        counts = {}
        for (_c, (b, _m)) in g.items():
            counts[b] = counts.get(b, 0) + 1
        out[kind]["ground_blocks"] = counts
    out.update(DOC)
    return out


# ------------------------------------------------------------------------------------------
# Main
# ------------------------------------------------------------------------------------------
def java_files():
    return {os.path.join(JAVA_DIR, name): text for name, text in JAVA.items()}


def main():
    if "--layout" in sys.argv:
        kind = sys.argv[sys.argv.index("--layout") + 1]
        print(json.dumps(layout_json(kind)))
        return 0
    if "--doc" in sys.argv:
        print(json.dumps(doc(), indent=1, default=str))
        return 0
    if "--check" in sys.argv:
        stale = []
        for path, text in java_files().items():
            if not os.path.exists(path):
                stale.append(path)
                continue
            with open(path, encoding="utf-8") as fh:
                if fh.read() != text:
                    stale.append(path)
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
    rc = C.main()
    if rc == 0 and "--fragments" not in sys.argv and "--check" not in sys.argv:
        for path, text in java_files().items():
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8", newline="\n") as fh:
                fh.write(text)
        vc.check_json([os.path.join(C.assets, rel) for rel in
                       [os.path.join(dp, fn) for dp, _, fs in os.walk(C.assets) for fn in fs]])
    return rc


if __name__ == "__main__":
    sys.exit(main())
