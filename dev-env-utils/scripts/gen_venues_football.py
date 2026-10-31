#!/usr/bin/env python3
"""
gen_venues_football.py -- the Sports & Venues Football & Soccer tab.

    py -3 dev-env-utils/scripts/gen_venues_football.py                  # write
    py -3 dev-env-utils/scripts/gen_venues_football.py --check          # fail on drift
    py -3 dev-env-utils/scripts/gen_venues_football.py --fragments      # the tab's lines
    py -3 dev-env-utils/scripts/gen_venues_football.py --layout football   # a field's blocks (JSON)
    py -3 dev-env-utils/scripts/gen_venues_football.py --layout soccer --commands X Y Z [--east]
                                                                         # ... as commands
    py -3 dev-env-utils/scripts/gen_venues_football.py --doc            # anchors and counts

The fields are drawn from one geometry, football_field.py: a pro American football field
(120 x 53 1/3 yd) and a 105 x 68 m soccer pitch, one block a metre. Every cell of a field is:

  * a plain surface: ``field_turf`` (turf in the light or the dark mowing stripe, white paint, the
    surround off the field) or ``football_end_zone`` (an end zone's colour, any of the sixteen
    venue colours);
  * or a picture round a marking anchor (``football_marking_anchor``, ``_2``;
    ``soccer_marking_anchor``, ``_2``): the ballpark's painted cells, a ``football_painted`` or
    ``soccer_painted`` cell showing the piece of the field that falls on it. Lines are drawn where
    they really fall in their cell (to the texel), so the 5 yard rhythm (4.572 m) is exact.

A picture is drawn once, in the light tone: the mowing stripe is a tint, taken from the plain
turf beside the cell, and a cell a stripe edge crosses (under every 5 yard line) is cut at the
edge into a west part and an east part, each taking its own side's tone. The end zone's colour
is a tint too. So the 162 football pictures and 80 soccer pictures serve any mowing and any
end zone colour.

Then the goals (the football goal post, the soccer goal, the small-sided goal), the sideline
(pylons, team benches, heater and fan, the down box and chain set, corner flags, the
substitutes' shelter, the fourth official's board), the end zone lettering, and the football and
soccer scoreboards.

Every model faces north with its back at +Z. No brand.
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
import football_field as ff  # noqa: E402

GEN = "gen_venues_football.py"
C = vc.catalogue(GEN, "football")
T = C.T
M = C.M
box = vc.box
lsc = vc.lsc

TAB = "Sports & Venues - Football & Soccer"
FACINGS = vc.FACINGS
RES = ff.RES
JAVA_DIR = os.path.join(vc.REPO, "modules", "venues", "src", "main", "java", "com",
                        "micatechnologies", "minecraft", "csm", "venues", "field")
JAVA = {}
DOC = {}

# ------------------------------------------------------------------------------------------
# Palette
# ------------------------------------------------------------------------------------------
TURF = (92, 156, 60)
PAINT = (246, 246, 240)
ZONE_PAINT = (236, 236, 232)    # under the end zone tint
SOIL = (118, 84, 58)
YELLOW = (250, 206, 24)
ORANGE = (246, 112, 24)
STEEL = (196, 200, 204)
STEEL_DARK = (88, 92, 98)
BLACK = (26, 26, 28)
NAVY = (26, 40, 84)
WHITE = (240, 240, 236)
ALUMINIUM = (192, 196, 200)
NET = (236, 236, 236)


def rgba(c, a=255):
    return tuple(c[:3]) + (a,)


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


def item_of(model_name, scale, dy=0.0):
    return {"parent": "csm:block/venues/football/" + model_name,
            "display": icon_display(scale, dy)}


# ------------------------------------------------------------------------------------------
# Surface textures: isotropic grain, so a cell turned meets its neighbours unseen
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


SURF = {
    "turf": grain(TURF, 801, 0.06, ((vc.shade(TURF, 0.8), 0.16), (vc.shade(TURF, 1.14), 0.14))),
    "white": grain(PAINT, 802, 0.02, ((vc.shade(PAINT, 0.93), 0.1),)),
    "zone": grain(ZONE_PAINT, 803, 0.03, ((vc.shade(ZONE_PAINT, 0.9), 0.12),)),
}
# football_field's labels: turf, turf_dark (merged into turf), white, zone
LAYER = ["turf", "turf", "white", "zone"]


def to_img(arr):
    img = np.concatenate([np.clip(np.rint(arr), 0, 255), np.full((RES, RES, 1), 255)], axis=2)
    return Image.fromarray(img.astype(np.uint8), "RGBA")


def compose(cov):
    out = np.zeros((RES, RES, 3))
    for k, name in enumerate(LAYER):
        out += cov[k][..., None] * SURF[name]
    return to_img(out)


@C.texture("soil")
def _soil():
    return vc.swatch(SOIL, 810, grain=6)


C.textures["turf"] = lambda: to_img(SURF["turf"])
C.textures["paint"] = lambda: to_img(SURF["white"])
C.textures["zone_paint"] = lambda: to_img(SURF["zone"])
SIDE = T("soil")


# ------------------------------------------------------------------------------------------
# Plain field blocks
# ------------------------------------------------------------------------------------------
def cube(top, tint=False):
    faces = {f: {"texture": "#side", "cullface": f} for f in ("north", "south", "east", "west",
                                                              "down")}
    faces["up"] = {"texture": "#up", "cullface": "up"}
    if tint:
        faces["up"]["tintindex"] = 0
    return {"parent": "block/block", "textures": {"particle": top, "up": top, "side": SIDE},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]}


TURF_KINDS = [("turf_light", T("turf"), False), ("turf_dark", T("turf"), True),
              ("paint_white", T("paint"), False), ("turf_surround", T("turf"), False)]
C.add("field_turf", 'new BlockFieldTurf("field_turf", BlockFieldTurf.Kind.TURF)',
      ("Field Turf", "Spielfeldrasen", "Césped de Campo", "Planens Gräs"),
      {n: cube(t, tint) for n, t, tint in TURF_KINDS},
      {"forge_marker": 1, "defaults": {"model": M("turf_light")},
       "variants": {"kind": {str(k): {"model": M(n)} for k, (n, _t, _i) in enumerate(TURF_KINDS)},
                    "inventory": [{"model": M("turf_light")}]}},
      tab=TAB)
C.add("football_end_zone", 'new BlockFieldTurf("football_end_zone", BlockFieldTurf.Kind.END_ZONE)',
      ("Football End Zone", "Football-Endzone", "Zona de Anotación", "Fotbollens Målområde"),
      {"end_zone": cube(T("zone_paint"), True)},
      {"forge_marker": 1, "defaults": {"model": M("end_zone")},
       "variants": {"colour": {str(k): {} for k in range(16)},
                    "inventory": [{"model": M("end_zone")}]}},
      tab=TAB)


# ------------------------------------------------------------------------------------------
# The fields: census, pictures, anchors
# ------------------------------------------------------------------------------------------
class Family(object):
    """One sport's pictures, anchors and tables."""

    def __init__(self, field, prefix):
        self.field = field
        self.prefix = prefix
        self.cv = ff.canvas(field)
        self.cen = ff.census(field, self.cv)
        self.plan = ff.plan(field, self.cen)
        self.pics = []          # (cov, cm) as stored
        self.index = {}
        self.tables = []        # per anchor kind: {(i, j): (cell, half)}
        for (ax, az), cells in self.plan:
            t = {}
            for c in sorted(set(cells) | {(ax, az)}):
                t[(c[0] - ax, -(c[1] - az))] = self.picture_at(c)
            self.tables.append(t)

    def picture_at(self, c):
        v = self.cen[c]
        if v[0] == "picture":
            cov, cm = v[1], v[2]
        else:
            cov, cm = ff.plain_picture(v[1], v[2])
        key, half, scov, scm = ff.canonical(cov, cm)
        if key not in self.index:
            self.index[key] = len(self.pics)
            self.pics.append((scov, scm))
        return self.index[key], half


def class_rects(cm):
    """The class map as rectangles of one class each: (c0, r0, c1, r1, class), texels."""
    used = np.zeros(cm.shape, dtype=bool)
    out = []
    n = cm.shape[0]
    for r in range(n):
        for c in range(n):
            if used[r, c]:
                continue
            v = cm[r, c]
            c1 = c
            while c1 < n and cm[r, c1] == v and not used[r, c1]:
                c1 += 1
            r1 = r + 1
            while r1 < n and (cm[r1, c:c1] == v).all() and not used[r1, c:c1].any():
                r1 += 1
            used[r:r1, c:c1] = True
            out.append((c, r, c1, r1, int(v)))
    return out


def cell_model(tex, cm):
    """A painted cell: a soil body, and its top cut into one face for each tint class's
    rectangle, each a window of the one picture texture."""
    faces = {f: {"texture": "#side", "cullface": f} for f in ("north", "south", "east", "west",
                                                              "down")}
    els = [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]
    for (c0, r0, c1, r1, v) in class_rects(cm):
        x0, z0, x1, z1 = c0 / 2.0, r0 / 2.0, c1 / 2.0, r1 / 2.0
        up = {"texture": "#pic", "uv": [x0, z0, x1, z1], "cullface": "up"}
        if v != ff.FIXED:
            up["tintindex"] = v
        els.append({"from": [x0, 0, z0], "to": [x1, 16, z1], "faces": {"up": up}})
    return {"parent": "block/block", "textures": {"particle": tex, "pic": tex, "side": SIDE},
            "elements": els}


FAMILIES = {"football": Family(ff.FootballField(), "fb"),
            "soccer": Family(ff.SoccerPitch(), "sc")}
SPORT_NAMES = {
    "football": (("Football Marking Anchor", "Football-Markierungsanker",
                  "Ancla de Marca de Fútbol Americano", "Ankare för Fotbollsmarkering"),
                 ("Football Field, Painted Marking", "Footballfeld, gemalte Markierung",
                  "Campo de Fútbol Americano, Marca Pintada", "Fotbollsplan, Målad Markering")),
    "soccer": (("Soccer Marking Anchor", "Fußball-Markierungsanker",
                "Ancla de Marca de Fútbol", "Ankare för Fotbollsplansmarkering"),
               ("Soccer Pitch, Painted Marking", "Fußballplatz, gemalte Markierung",
                "Campo de Fútbol, Marca Pintada", "Fotbollsplan (Soccer), Målad Markering")),
}
SECOND = (" (Second Set)", " (Zweiter Satz)", " (Segundo Juego)", " (Andra Uppsättningen)")
KINDS_PER_ANCHOR = 8

for _sport, _fam in FAMILIES.items():
    _p = _fam.prefix
    for _i, (_cov, _cm) in enumerate(_fam.pics):
        C.textures["%s_pic_%03d" % (_p, _i)] = (lambda c: (lambda: compose(c)))(_cov)
    _models = {"%s_cell_%03d" % (_p, _i): cell_model(T("%s_pic_%03d" % (_p, _i)), _cm)
               for _i, (_cov, _cm) in enumerate(_fam.pics)}
    _fam_java = "FieldLayouts.%s" % _sport.upper()
    _nk = len(_fam.tables)
    _blocks = int(math.ceil(_nk / float(KINDS_PER_ANCHOR)))
    _fam.anchor_blocks = []
    for _b in range(_blocks):
        _reg = "%s_marking_anchor%s" % (_sport, "" if _b == 0 else "_%d" % (_b + 1))
        _first = _b * KINDS_PER_ANCHOR
        _kinds = min(KINDS_PER_ANCHOR, _nk - _first)
        _fam.anchor_blocks.append((_reg, _first, _kinds))
        _variants = {}
        for _k in range(_kinds):
            _t = _fam.tables[_first + _k]
            _cell, _half = _t[(0, 0)]
            for _east in (False, True):
                _q = (1 if _east else 0) + 2 * _half
                _variants["east=%s,kind=%d" % ("true" if _east else "false", _k)] = [
                    dict({"model": M("%s_cell_%03d" % (_p, _cell))}, **ys((_q % 4) * 90))]
        _c0, _h0 = _fam.tables[_first][(0, 0)]
        _variants["inventory"] = [{"model": M("%s_cell_%03d" % (_p, _c0))}]
        _names = tuple(n + (SECOND[i] if _b else "") for i, n in
                       enumerate(SPORT_NAMES[_sport][0]))
        C.add(_reg, 'new BlockFieldAnchor("%s", %s, %d, %d)' % (_reg, _fam_java, _first, _kinds),
              _names, dict(_models) if _b == 0 else {},
              {"forge_marker": 1, "variants": _variants}, tab=TAB)
    _n = len(_fam.pics)
    _inv = _fam.tables[0][(0, 0)][0]
    C.add("%s_painted" % _sport, 'new BlockFieldPainted("%s_painted", %s)' % (_sport, _fam_java),
          SPORT_NAMES[_sport][1], {},
          {"forge_marker": 1, "defaults": {"model": M("%s_cell_000" % _p)},
           "variants": {"cell": {str(k): {"model": M("%s_cell_%03d" % (_p, k))} for k in range(_n)},
                        "rot": {"0": {}, "1": {"y": 90}, "2": {"y": 180}, "3": {"y": 270}},
                        "inventory": [{"model": M("%s_cell_%03d" % (_p, _inv))}]}},
          tab=TAB)


# ------------------------------------------------------------------------------------------
# FieldLayouts.java
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
    out = ["package com.micatechnologies.minecraft.csm.venues.field;\n",
           "/**",
           " * Where every painted football and soccer cell sits round its anchor: generated by",
           " * {@code dev-env-utils/scripts/gen_venues_football.py} from the fields it draws",
           " * ({@code football_field.py}), so the models and this table cannot disagree. Do not edit.",
           " *",
           " * <p>For each anchor kind, triplets of {@code i} (cells to the anchor's right), {@code j}",
           " * (cells forward) and the painted cell there: {@code cell * 2 + half}, where {@code half}",
           " * is 1 if the stored picture is shown turned 180 degrees.</p>",
           " *",
           " * @since 2026.10",
           " */",
           "public final class FieldLayouts {\n"]
    for sport, fam in FAMILIES.items():
        rows = []
        for t in fam.tables:
            trip = []
            for (i, j), (c, h) in sorted(t.items(), key=lambda kv: (kv[0][1], kv[0][0])):
                trip += [i, j, c * 2 + h]
            rows.append("      new int[]{%s}" % ", ".join(str(x) for x in trip))
        out.append("  /** The %s field: %d pictures, %d anchor kinds. */"
                   % (sport, len(fam.pics), len(fam.tables)))
        out.append("  public static final FieldFamily %s = new FieldFamily(%d, %d,"
                   % (sport.upper(), len(fam.pics), ff.REACH))
        out.append(",\n".join(rows) + ");\n")
    out.append("  private FieldLayouts() {")
    out.append("  }")
    out.append("}")
    return wrap("\n".join(out) + "\n")


JAVA["FieldLayouts.java"] = layouts_java()


# ------------------------------------------------------------------------------------------
# Shared swatches and helpers for the fixtures
# ------------------------------------------------------------------------------------------
C.textures["yellow"] = lambda: vc.swatch(YELLOW, 820, grain=2)
C.textures["orange"] = lambda: vc.swatch(ORANGE, 821, grain=2)
C.textures["white"] = lambda: vc.swatch(WHITE, 822, grain=2)
C.textures["steel"] = lambda: vc.brushed(STEEL, 823)
C.textures["steel_dark"] = lambda: vc.brushed(STEEL_DARK, 824)
C.textures["black"] = lambda: vc.swatch(BLACK, 825, grain=2)
C.textures["navy_pad"] = lambda: vc.swatch(NAVY, 826, grain=3)
C.textures["aluminium"] = lambda: vc.brushed(ALUMINIUM, 827)
C.textures["concrete"] = lambda: vc.swatch((168, 166, 160), 828, grain=6)
C.textures["red"] = lambda: vc.swatch((210, 36, 36), 829, grain=2)


@C.texture("net")
def _net():
    """A goal net: 12 cm mesh (every 4 texels of a 32 px block), the rest cut out."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for y in range(32):
        for x in range(32):
            if x % 4 == 0 or y % 4 == 0:
                px[x, y] = rgba(vc.shade(NET, 1.0 - 0.06 * ((x + y) % 3)))
    return img


@C.texture("acrylic")
def _acrylic():
    """Clear acrylic in the cutout layer: a frame band, a few highlights, the rest cut out."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for x in range(16):
        px[x, 0] = rgba((150, 160, 168))
        px[x, 15] = rgba((150, 160, 168))
    for k in range(5):
        px[3 + k, 4 + k] = rgba((226, 236, 242))
        px[8 + k, 3 + k] = rgba((214, 226, 234))
    return img


def write_obj(name, mtl, polys):
    o = vc.Obj(GEN, mtl, name)
    for p in polys:
        o.add(p)
    C.extra["models/block/venues/football/%s.obj" % name] = o.text()


def mtl(name, mats):
    C.extra["models/block/venues/football/%s.mtl" % name] = vc.mtl_text(GEN, mats)


def _perp(d):
    """Two unit vectors square to d and to each other."""
    a = (0.0, 1.0, 0.0) if abs(d[1]) < 0.9 else (1.0, 0.0, 0.0)
    u = vc._norm(vc._cross(d, a))
    v = vc._norm(vc._cross(d, u))
    return u, v


def tube(p0, p1, r, mat, caps=True):
    """A round member from p0 to p1 (pixels): an octagonal prism."""
    d = vc._norm(vc._sub(p1, p0))
    u, v = _perp(d)
    ring = []
    for i in range(8):
        a = math.radians(22.5 + 45 * i)
        ring.append((u[0] * math.cos(a) + v[0] * math.sin(a),
                     u[1] * math.cos(a) + v[1] * math.sin(a),
                     u[2] * math.cos(a) + v[2] * math.sin(a)))
    out = []
    for i in range(8):
        a, b = ring[i], ring[(i + 1) % 8]
        n = ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2)
        pts = [tuple(p0[k] + a[k] * r for k in range(3)), tuple(p0[k] + b[k] * r for k in range(3)),
               tuple(p1[k] + b[k] * r for k in range(3)), tuple(p1[k] + a[k] * r for k in range(3))]
        out.append(vc.Poly(pts, mat, n))
    if caps:
        out.append(vc.Poly([tuple(p0[k] + c[k] * r for k in range(3)) for c in ring], mat,
                           tuple(-x for x in d)))
        out.append(vc.Poly([tuple(p1[k] + c[k] * r for k in range(3)) for c in ring], mat, d))
    return out


def net_quad(a, b, c, d, outward, mat="net"):
    """A net panel a-b-c-d (a convex quad in pixels), cut into pieces about a block each so its
    texture repeats a block at a time (an OBJ's uv may not leave 0..1); both faces."""
    def lerp(p, q, t):
        return tuple(p[k] + (q[k] - p[k]) * t for k in range(3))
    nu = max(1, int(math.ceil(math.dist(a, b) / 16.0)))
    nv = max(1, int(math.ceil(math.dist(a, d) / 16.0)))
    out = []
    for i in range(nu):
        for j in range(nv):
            def at(s, t):
                return lerp(lerp(a, b, s), lerp(d, c, s), t)
            s0, s1, t0, t1 = i / float(nu), (i + 1) / float(nu), j / float(nv), (j + 1) / float(nv)
            pts = [at(s0, t0), at(s1, t0), at(s1, t1), at(s0, t1)]
            uvs = [(0, 0), (1, 0), (1, 1), (0, 1)]
            out.append(vc.Poly(pts, mat, outward, uvs))
            out.append(vc.Poly(list(reversed(pts)), mat, tuple(-x for x in outward),
                               list(reversed(uvs))))
    return out


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


def check_connected(reg, cells):
    """BlockVenueUnit's rule: every cell reaches the master through others."""
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


def unit_java(cls, reg, boxes_px, extra=""):
    check_connected(reg, unit_cells(boxes_px))
    flat_c = ", ".join("%d, %d, %d" % c for c in unit_cells(boxes_px))
    flat_b = ", ".join(", ".join("%d" % int(round(v)) for v in b) for b in boxes_px)
    return 'new %s("%s"%s, new int[]{%s}, new int[]{%s})' % (cls, reg, extra, flat_c, flat_b)


def unit_state(obj, icon, ambient=True):
    d = {"model": M(obj) + ".obj"}
    if not ambient:
        d["custom"] = {"ambient": False}
    return {"forge_marker": 1, "defaults": d,
            "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                         "inventory": [{"model": M(icon), "custom": {}}]}}


# ------------------------------------------------------------------------------------------
# The football goal post: an offset gooseneck, real size
# ------------------------------------------------------------------------------------------
# Master frame (pixels, the field to the north): the base post 6 ft behind the plane of the
# uprights, placed so that on the canonical field (master at the cell the base stands in) the
# uprights' inner face is the end line's inner edge. The field is 180 degree symmetric about a
# cell's middle, so one model serves both ends.
_FB = FAMILIES["football"].field
GP_CELL_X = int(math.floor(_FB.ex[0] - 6 * ff.FT))              # the west post's master cell
GP_BASE_Z = 16 - ((_FB.ex[0] - 6 * ff.FT) - GP_CELL_X) * 16     # model z of the base post
GP_PLANE_Z = 16 - (_FB.ex[0] - GP_CELL_X) * 16                  # model z of the uprights' inside
GP_R_POST, GP_R_BAR = 1.8, 1.2
GP_BAR_Y = 10 * ff.FT * 16                                     # 10 ft
GP_HALF = 18.5 / 2 * ff.FT * 16                                 # half the inside width
GP_TOP = GP_BAR_Y + 35 * ff.FT * 16                            # uprights 35 ft over the bar
GP_UPR_Z = GP_PLANE_Z + GP_R_BAR
DOC["goal_post"] = {"cell_x_west": GP_CELL_X, "base_z_px": GP_BASE_Z, "plane_z_px": GP_PLANE_Z}


def goal_post_polys():
    P = []
    bz, uz = GP_BASE_Z, GP_UPR_Z
    bend = 16.8
    y_bend = GP_BAR_Y - bend
    P += tube((8, 0, bz), (8, y_bend, bz), GP_R_POST, "yellow")
    P += tube((8, 0, bz), (8, min(29.3, y_bend), bz), GP_R_POST + 1.4, "pad")
    prev = (8, y_bend, bz)
    for k in range(1, 7):
        a = math.radians(90.0 * k / 6)
        p = (8, y_bend + bend * math.sin(a), bz - bend * (1 - math.cos(a)))
        P += tube(prev, p, GP_R_POST, "yellow")
        prev = p
    P += tube(prev, (8, GP_BAR_Y, uz), GP_R_POST, "yellow")
    P += tube((8 - GP_HALF - GP_R_BAR, GP_BAR_Y, uz), (8 + GP_HALF + GP_R_BAR, GP_BAR_Y, uz),
              GP_R_BAR, "yellow")
    for sx in (-1, 1):
        x = 8 + sx * (GP_HALF + GP_R_BAR)
        P += tube((x, GP_BAR_Y - GP_R_BAR, uz), (x, GP_TOP, uz), GP_R_BAR, "yellow")
        # a wind ribbon off the top, hanging a little out and back
        P += [vc.Poly([(x - 0.8, GP_TOP - 1, uz + 1.3), (x + 0.8, GP_TOP - 1, uz + 1.3),
                       (x + 0.8, GP_TOP - 16, uz + 3.5), (x - 0.8, GP_TOP - 16, uz + 3.5)],
                      "ribbon", (0, 0, -1)),
              vc.Poly([(x - 0.8, GP_TOP - 16, uz + 3.5), (x + 0.8, GP_TOP - 16, uz + 3.5),
                       (x + 0.8, GP_TOP - 1, uz + 1.3), (x - 0.8, GP_TOP - 1, uz + 1.3)],
                      "ribbon", (0, 0, 1))]
    P += vc.obox(8 - 4, 8 + 4, 0, 0.6, bz - 4, bz + 4, "base", "nsewu")
    return P


mtl("goal_post", {"yellow": T("yellow"), "pad": T("navy_pad"), "ribbon": T("orange"),
                  "base": T("concrete")})
write_obj("goal_post", "goal_post", goal_post_polys())
GP_BOXES = [(6, 0, GP_BASE_Z - 2, 10, GP_BAR_Y, GP_BASE_Z + 2),
            (6, 32, GP_UPR_Z - 2, 10, 54, GP_BASE_Z + 2),
            (8 - GP_HALF - 2 * GP_R_BAR, 48, GP_UPR_Z - 2, 8 + GP_HALF + 2 * GP_R_BAR,
             54, GP_UPR_Z + 2)]
for _sx in (-1, 1):
    _x = 8 + _sx * (GP_HALF + GP_R_BAR)
    GP_BOXES.append((_x - 1.5, GP_BAR_Y, GP_UPR_Z - 1.5, _x + 1.5, GP_TOP, GP_UPR_Z + 1.5))
GP_ICON = vc.model({"yellow": T("yellow"), "pad": T("navy_pad"), "particle": T("yellow")},
                   [box([7, 0, 7], [9, 7, 9], "yellow"), box([6.5, 0, 6.5], [9.5, 4, 9.5], "pad"),
                    box([7, 6, 4], [9, 7.5, 9], "yellow"), box([1, 6, 3.5], [15, 7.5, 5], "yellow"),
                    box([1, 7.5, 3.5], [2.5, 16, 5], "yellow"),
                    box([13.5, 7.5, 3.5], [15, 16, 5], "yellow")])
C.add("football_goal_post", unit_java("BlockVenueUnit", "football_goal_post", GP_BOXES),
      ("Football Goal Post", "Football-Torstange", "Poste de Gol de Fútbol Americano",
       "Fotbollsmålstolpe"),
      {"goal_post_item": GP_ICON}, unit_state("goal_post", "goal_post_item"),
      item=item_of("goal_post_item", 1.0), tab=TAB)
DOC["goal_post_cells"] = len(unit_cells(GP_BOXES))


# ------------------------------------------------------------------------------------------
# The soccer goal (7.32 x 2.44 m) and a small-sided goal
# ------------------------------------------------------------------------------------------
def goal_polys(half, height, r, front_z, depth_top, depth_bottom, cx):
    """A goal: posts and bar square to the front at front_z (pixels), the posts' inside faces
    ``half`` either side of x = cx; the net back to a top bar depth_top behind and a ground bar
    depth_bottom behind."""
    P = []
    pz = front_z + r
    xl, xr = cx - half - r, cx + half + r
    top = height + r
    for x in (xl, xr):
        P += tube((x, 0, pz), (x, top + r, pz), r, "post")
    P += tube((xl - r, top, pz), (xr + r, top, pz), r, "post")
    tz, gz = pz + depth_top, pz + depth_bottom
    ty = top - 2
    # back frame: ground bars, back top bar, the stanchions from the posts' tops to it
    P += tube((xl, 0.6, pz), (xl, 0.6, gz), 0.6, "frame")
    P += tube((xr, 0.6, pz), (xr, 0.6, gz), 0.6, "frame")
    P += tube((xl, 0.6, gz), (xr, 0.6, gz), 0.6, "frame")
    P += tube((xl, top, pz), (xl, ty, tz), 0.6, "frame")
    P += tube((xr, top, pz), (xr, ty, tz), 0.6, "frame")
    P += tube((xl, ty, tz), (xl, 0.6, gz), 0.6, "frame")
    P += tube((xr, ty, tz), (xr, 0.6, gz), 0.6, "frame")
    # the net: roof, back, two sides
    P += net_quad((xl, top, pz), (xr, top, pz), (xr, ty, tz), (xl, ty, tz), (0, 1, 0))
    P += net_quad((xl, ty, tz), (xr, ty, tz), (xr, 0, gz), (xl, 0, gz), (0, 0.4, 1))
    for x, s in ((xl, -1), (xr, 1)):
        P += net_quad((x, 0, pz), (x, top, pz), (x, ty, tz), (x, 0, gz), (s, 0, 0))
    return P


mtl("goal", {"post": T("white"), "frame": T("steel"), "net": T("net")})
# the soccer goal on the canonical pitch: posts on the goal line (the line's 12 cm), centred on
# the pitch's middle, a block edge, which is the master cell's west edge (model x 0)
SG_HALF = 3.66 * 16
SG_H = 2.44 * 16
SG_R = 0.06 * 16
SG_FRONT = 16 - 0.12 * 16
write_obj("soccer_goal", "goal", goal_polys(SG_HALF, SG_H, SG_R, SG_FRONT, 16, 32, 0))
SG_BOXES = [(-SG_HALF - 2 * SG_R, 0, SG_FRONT, -SG_HALF, SG_H + 2 * SG_R, SG_FRONT + 2 * SG_R),
            (SG_HALF, 0, SG_FRONT, SG_HALF + 2 * SG_R, SG_H + 2 * SG_R, SG_FRONT + 2 * SG_R),
            (-SG_HALF, SG_H, SG_FRONT, SG_HALF, SG_H + 2 * SG_R, SG_FRONT + 2 * SG_R),
            (-SG_HALF, 0, SG_FRONT + 31, SG_HALF, SG_H - 4, SG_FRONT + 34),
            (-SG_HALF - 2, 0, SG_FRONT + 2, -SG_HALF, SG_H - 4, SG_FRONT + 31),
            (SG_HALF, 0, SG_FRONT + 2, SG_HALF + 2, SG_H - 4, SG_FRONT + 31),
            (-SG_HALF, 0, SG_FRONT, SG_HALF, 1, SG_FRONT + 2 * SG_R)]
GOAL_ICON_TEX = {"post": T("white"), "net": T("net"), "particle": T("white")}
SG_ICON = vc.model(GOAL_ICON_TEX, [
    box([0, 0, 4], [1.5, 10, 5.5], "post"), box([14.5, 0, 4], [16, 10, 5.5], "post"),
    box([0, 10, 4], [16, 11.5, 5.5], "post"),
    box([1.5, 0, 12], [14.5, 9, 12], "net", per={}),
    box([1.5, 10, 5.5], [14.5, 10, 12], "net")])
C.add("soccer_goal", unit_java("BlockVenueUnit", "soccer_goal", SG_BOXES),
      ("Soccer Goal", "Fußballtor", "Portería de Fútbol", "Fotbollsmål"),
      {"soccer_goal_item": SG_ICON}, unit_state("soccer_goal", "soccer_goal_item"),
      item=item_of("soccer_goal_item", 1.0), tab=TAB)
# a portable small-sided goal, 12 x 6 ft, centred on its cell, its posts at the front edge
SM_HALF = 6 * ff.FT * 16
SM_H = 6 * ff.FT * 16
SM_R = 0.9
write_obj("soccer_goal_small", "goal", goal_polys(SM_HALF, SM_H, SM_R, 0.5, 8, 18, 8))
SM_BOXES = [(8 - SM_HALF - 2 * SM_R, 0, 0, 8 - SM_HALF, SM_H + 2 * SM_R, 2.5),
            (8 + SM_HALF, 0, 0, 8 + SM_HALF + 2 * SM_R, SM_H + 2 * SM_R, 2.5),
            (8 - SM_HALF, SM_H, 0, 8 + SM_HALF, SM_H + 2 * SM_R, 2.5)]
C.add("soccer_goal_small", unit_java("BlockVenueUnit", "soccer_goal_small", SM_BOXES),
      ("Small-Sided Goal", "Kleinfeldtor", "Portería de Fútbol Reducido", "Smålagsmål"),
      {"soccer_goal_small_item": SG_ICON}, unit_state("soccer_goal_small", "soccer_goal_item"),
      item=item_of("soccer_goal_item", 1.0), tab=TAB)


# ------------------------------------------------------------------------------------------
# The sideline: fixtures (BlockFieldFixture)
# ------------------------------------------------------------------------------------------
FIX_TEX = {"orange": T("orange"), "white": T("white"), "steel": T("steel"),
           "steel_dark": T("steel_dark"), "black": T("black"), "red": T("red"),
           "yellow": T("yellow"), "particle": T("steel")}


def digit_tex(d):
    def draw():
        img = vc.swatch(ORANGE, 830 + d, grain=2)
        vc.rect(img, 2, 2, 14, 14, (24, 24, 26))
        vc.draw_text(img, str(d), 5, 3, (250, 250, 246), scale=2)
        return img
    return draw


for _d in range(1, 5):
    C.textures["down_%d" % _d] = digit_tex(_d)


@C.texture("flag")
def _flag():
    img = vc.swatch((250, 208, 30), 840, grain=1)
    for y in range(16):
        for x in range(16):
            if (x // 4 + y // 4) % 2:
                img.putpixel((x, y), rgba((222, 34, 40)))
    return img


@C.texture("board_led")
def _board_led():
    """The board's LED face in the middle half of a square texture (rows 8-23): a sprite must
    be square, or the atlas takes it for an animation."""
    img = Image.new("RGBA", (32, 32), rgba((18, 18, 20)))
    vc.rect(img, 0, 8, 32, 9, (60, 60, 64))
    vc.rect(img, 0, 23, 32, 24, (60, 60, 64))
    vc.draw_text(img, "+3", 6, 11, (80, 240, 90), scale=2)
    return img


@C.texture("fan_grille")
def _fan_grille():
    img = vc.swatch((40, 42, 46), 841, grain=2)
    for r in range(2, 8, 2):
        for a in range(64):
            t = a / 64.0 * math.tau
            x, y = int(round(7.5 + r * math.cos(t))), int(round(7.5 + r * math.sin(t)))
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), rgba((150, 154, 160)))
    vc.rect(img, 7, 0, 9, 16, (110, 114, 120))
    vc.rect(img, 0, 7, 16, 9, (110, 114, 120))
    return img


def pylon():
    return [box([6.75, 0, 6.75], [9.25, 7.5, 9.25], "orange")]


def heater():
    els = lsc.post(8, 8, 3.5, 0, 1, "steel_dark")
    els += lsc.post(8, 8, 2.6, 1, 16, "steel")          # the propane cylinder's housing
    els += lsc.post(8, 8, 0.7, 16, 26, "steel_dark")
    els += lsc.post(8, 8, 1.8, 26, 28.5, "black")       # the burner
    els += lsc.post(8, 8, 7.5, 29, 30, "steel")         # the reflector
    return els


def fan():
    els = [box([2, 0, 10], [4, 1, 14], "black"), box([12, 0, 10], [14, 1, 14], "black"),
           box([2, 0, 3], [14, 1, 5], "steel_dark"),
           box([3, 1, 7], [4, 6, 9], "steel_dark"), box([12, 1, 7], [13, 6, 9], "steel_dark"),
           box([1.5, 4, 6], [14.5, 17, 10], "steel_dark", per={"north": "grille", "south": "grille"}),
           box([5, 6, 10], [11, 15, 12.5], "black")]
    for f in ("north", "south"):
        els[5]["faces"][f]["uv"] = [0, 0, 16, 16]
    return els


def down_box(d):
    els = lsc.post(8, 8, 0.8, 0, 26, "steel")
    els += [box([5.5, 26, 5.5], [10.5, 31, 10.5], "orange",
                per={"north": "num", "south": "num"})]
    for f in ("north", "south"):
        els[-1]["faces"][f]["uv"] = [0, 0, 16, 16]
    return els


def chain_stake():
    els = lsc.post(8, 8, 0.8, 0, 27, "steel")
    els += lsc.post(8, 8, 1.4, 27, 31, "red")
    els += lsc.post(8, 8, 1.0, 0, 1.5, "black")
    return els


def chain():
    els = []
    for k in range(8):
        z0 = k * 2
        if k % 2 == 0:
            els.append(box([7.4, 0, z0], [8.6, 0.4, z0 + 2.2], "steel"))
        else:
            els.append(box([7.8, 0, z0], [8.2, 0.9, z0 + 2.2], "steel"))
    return els


def corner_flag():
    els = lsc.post(8, 8, 0.5, 0, 24, "white")
    els.append(box([8, 18, 8], [14, 24, 8], "flag", per={}))
    els[-1]["faces"] = {f: {"texture": "#flag", "uv": [0, 0, 12, 12]} for f in ("north", "south")}
    return els


def official_board():
    els = lsc.post(8, 8, 0.5, 0, 13, "black")
    els += [box([4, 0, 6], [12, 0.6, 10], "black"),
            box([3, 13, 7.25], [13, 18, 8.75], "black", per={"north": "led", "south": "led"})]
    for f in ("north", "south"):
        els[-1]["faces"][f]["uv"] = [0, 4, 16, 12]
    return els


def fixture(reg, kind, names, els, tex=None, variants=None, scale=1.0, dy=0.0):
    t = dict(FIX_TEX)
    if tex:
        t.update(tex)
    models = {}
    if variants:
        for v, extra in enumerate(variants):
            tt = dict(t)
            tt.update(extra)
            models["%s_%d" % (reg, v)] = vc.model(tt, els)
        state = {"forge_marker": 1, "defaults": {"model": M(reg + "_0")},
                 "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                              "variant": {str(v): {"model": M("%s_%d" % (reg, v))}
                                          for v in range(len(variants))},
                              "inventory": [{"model": M(reg + "_0")}]}}
        item = item_of(reg + "_0", scale, dy)
    else:
        models[reg] = vc.model(t, els)
        state = vc.facing_state(M(reg))
        item = item_of(reg, scale, dy)
    C.add(reg, 'new BlockFieldFixture("%s", BlockFieldFixture.Kind.%s)' % (reg, kind), names,
          models, state, item=item, tab=TAB)


fixture("end_zone_pylon", "PYLON", ("End Zone Pylon", "Endzonen-Pylon", "Pilón de Zona de Anotación",
                                    "Målområdespylon"), pylon(), scale=1.6, dy=-2)
fixture("football_down_box", "DOWN_BOX", ("Down Box", "Down-Anzeiger", "Indicador de Down",
                                          "Downmarkör"), down_box(1),
        variants=[{"num": T("down_%d" % d)} for d in range(1, 5)], scale=0.6, dy=-4)
fixture("football_chain_stake", "CHAIN_STAKE", ("Chain Stake", "Kettenstange", "Poste de Cadena",
                                                "Kedjestake"), chain_stake(), scale=0.6, dy=-4)
fixture("football_chain", "CHAIN", ("Chain (Ten Yards)", "Kette (zehn Yards)",
                                    "Cadena (Diez Yardas)", "Kedja (Tio Yards)"), chain())
fixture("sideline_heater", "HEATER", ("Sideline Heater", "Spielfeldrand-Heizstrahler",
                                      "Calefactor de Banda", "Sidlinjevärmare"), heater(),
        scale=0.55, dy=-4)
fixture("sideline_fan", "FAN", ("Sideline Misting Fan", "Spielfeldrand-Sprühnebellüfter",
                                "Ventilador Nebulizador de Banda", "Sidlinjefläkt med Dimma"),
        fan(), tex={"grille": T("fan_grille")}, scale=0.8)
fixture("soccer_corner_flag", "CORNER_FLAG", ("Corner Flag", "Eckfahne", "Banderín de Córner",
                                              "Hörnflagga"), corner_flag(),
        tex={"flag": T("flag")}, scale=0.7, dy=-3)
fixture("fourth_official_board", "BOARD", ("Fourth Official's Board", "Auswechseltafel",
                                           "Tablero del Cuarto Árbitro", "Fjärdedomarens Tavla"),
        official_board(), tex={"led": T("board_led")}, scale=1.0, dy=-2)


# ------------------------------------------------------------------------------------------
# Runs: the team bench and the substitutes' shelter (BlockArenaRun)
# ------------------------------------------------------------------------------------------
RUN_TEX = {"alu": T("aluminium"), "steel": T("steel_dark"), "black": T("black"),
           "acrylic": T("acrylic"), "particle": T("aluminium")}


def bench_parts():
    els = []
    for z0 in (4, 7):
        els.append(box([0, 7.5, z0], [16, 8.5, z0 + 2.5], "alu"))
    els += [box([0, 11, 12.5], [16, 13.5, 13.5], "alu"), box([0, 14.5, 12.5], [16, 16.5, 13.5],
                                                                  "alu"),
            box([7.25, 0, 6], [8.75, 7.5, 8], "steel"), box([7.25, 0, 13.5], [8.75, 17, 14.75],
                                                            "steel"),
            box([5, 0, 5], [11, 0.6, 15], "steel")]
    return els


def bench_end():
    return [box([0, 0, 4], [1, 8.5, 13.5], "steel"), box([0, 8.5, 12.5], [1, 17, 13.5], "steel")]


def shelter_parts():
    els = [box([0, 0, 14.5], [16, 30, 16], "steel"),                       # back panel
           box([0, 0, 6], [16, 0.5, 14.5], "black"),
           box([0, 8, 7], [16, 9, 13], "black"),                            # the seat shells
           box([0, 9, 12], [16, 15, 13.5], "black"),
           box([7.25, 0, 9], [8.75, 8, 11], "steel"),
           box([0, 30, 1], [16, 31, 16], "acrylic", per={}),                  # the clear roof
           box([0, 29.5, 0], [16, 31.5, 1.5], "steel")]
    for f in ("up", "down"):
        els[5]["faces"][f] = {"texture": "#acrylic", "uv": [0, 0, 16, 16]}
    return els


def shelter_end():
    return [box([0, 0, 1], [0.5, 30, 14.5], "acrylic", per={}),
            box([0, 0, 0], [1, 31.5, 1.5], "steel")]


def _clear_faces(els):
    for e in els:
        if e["faces"] == {}:
            for f in ("east", "west"):
                e["faces"][f] = {"texture": "#acrylic", "uv": [0, 0, 16, 16]}
    return els


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


for _reg, _kind, _names, _parts, _end, _scale in (
        ("football_team_bench", "TEAM_BENCH",
         ("Team Bench", "Mannschaftsbank", "Banca del Equipo", "Lagbänk"),
         bench_parts(), bench_end(), 1.0),
        ("soccer_substitutes_shelter", "SUB_SHELTER",
         ("Substitutes' Shelter", "Auswechselbank mit Dach", "Banquillo con Techo",
          "Avbytarbås med Tak"),
         shelter_parts(), _clear_faces(shelter_end()), 0.55)):
    C.add(_reg, 'new BlockArenaRun("%s", BlockArenaRun.Kind.%s)' % (_reg, _kind), _names,
          run_models(_reg, _parts, _end), run_state(_reg),
          item={"parent": "csm:block/venues/football/%s_item" % _reg,
                "display": icon_display(0.625 * _scale, -1 if _scale < 1 else 0)}, tab=TAB)


# ------------------------------------------------------------------------------------------
# The end zone lettering (a renderer's; the block is a marker in the air over the end zone)
# ------------------------------------------------------------------------------------------
@C.texture("lettering")
def _lettering():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    vc.draw_text(img, "EZ", 2, 5, (250, 250, 246), scale=2)
    return img


C.add("football_end_zone_lettering",
      'new BlockEndZoneLettering("football_end_zone_lettering")',
      ("End Zone Lettering", "Endzonen-Schriftzug", "Letras de Zona de Anotación",
       "Målområdestext"),
      {"lettering": vc.model({"t": T("lettering"), "particle": T("white")},
                             [box([0, 0.05, 0], [16, 0.05, 16], "t", per={})])},
      {"forge_marker": 1, "defaults": {"model": M("lettering_world")},
       "variants": {"facing": {f: {} for f, _y in FACINGS},
                    "inventory": [{"model": M("lettering")}]}},
      item=item_of("lettering", 1.0), tab=TAB)
C.blocks[-1]["models"]["lettering"]["elements"][0]["faces"] = {
    "up": {"texture": "#t", "uv": [0, 0, 16, 16]}}
C.blocks[-1]["models"]["lettering_world"] = {"textures": {"particle": T("white")},
                                              "elements": []}


# ------------------------------------------------------------------------------------------
# The football and soccer scoreboards (BlockFieldScoreboard): one board, two renderers' layouts
# ------------------------------------------------------------------------------------------
@C.texture("board_face")
def _board_face():
    return vc.swatch((16, 18, 20), 850, grain=2)


@C.texture("board_cab")
def _board_cab():
    img = vc.swatch((40, 44, 46), 851, grain=2)
    for y in (0, 8):
        vc.rect(img, 0, y, 16, y + 1, (30, 32, 34))
    return img


mtl("scoreboard", {"face": T("board_face"), "cab": T("board_cab"), "steel": T("steel_dark"),
                   "cap": T("steel"), "base": T("concrete")})
# shared with TileEntityFieldScoreboardRenderer: the face's rectangle in the master's frame
FB_BOARD = {"x0": -16, "x1": 176, "y0": 48, "y1": 128, "z0": 2, "z1": 14}
FB_LEGS = [(5, 11), (149, 155)]


def board_polys(b, legs):
    P = []
    P += vc.obox(b["x0"], b["x1"], b["y0"], b["y1"], b["z0"], b["z1"], "cab", "sewud")
    P += vc.obox(b["x0"], b["x1"], b["y0"], b["y1"], b["z0"], b["z1"], "face", "n")
    P += vc.obox(b["x0"] - 1, b["x1"] + 1, b["y1"], b["y1"] + 2, b["z0"] - 1, b["z1"] + 1, "cap")
    P += vc.obox(b["x0"] - 1, b["x1"] + 1, b["y0"] - 1, b["y0"], b["z0"] - 1, b["z1"], "cap")
    P += vc.obox(b["x0"] - 1, b["x0"], b["y0"], b["y1"], b["z0"] - 1, b["z1"], "cap", "nsewd")
    P += vc.obox(b["x1"], b["x1"] + 1, b["y0"], b["y1"], b["z0"] - 1, b["z1"], "cap", "nsewd")
    for (x0, x1) in legs:
        P += vc.obox(x0, x1, 2, b["y0"] - 1, 5, 11, "steel", "nsew")
        P += vc.obox(x0 - 3, x1 + 3, 0, 2, 2, 14, "base")
    return P


write_obj("field_scoreboard", "scoreboard", board_polys(FB_BOARD, FB_LEGS))


def sb_boxes(b, legs):
    out = [(b["x0"], b["y0"], b["z0"], b["x1"], b["y1"], b["z1"])]
    for (x0, x1) in legs:
        out.append((x0, 0, 5, x1, b["y0"], 11))
    return out


def _sb_icon(kind):
    def draw():
        img = vc.swatch((16, 18, 20), 852, grain=2)
        vc.rect(img, 1, 2, 6, 4, (26, 40, 84) if kind == "fb" else (24, 128, 56))
        vc.rect(img, 10, 2, 15, 4, (148, 30, 44))
        vc.rect(img, 2, 6, 5, 9, (255, 200, 48))
        vc.rect(img, 11, 6, 14, 9, (255, 200, 48))
        vc.rect(img, 6, 6, 10, 8, (250, 230, 60))
        if kind == "fb":
            for i in range(3):
                vc.rect(img, 2 + i * 4, 12, 4 + i * 4, 14, (255, 200, 48))
        return img
    return draw


for _k, _reg, _soccer, _names in (
        ("fb", "football_scoreboard", "false",
         ("Football Scoreboard", "Football-Anzeigetafel", "Marcador de Fútbol Americano",
          "Fotbollens Resultattavla")),
        ("sc", "soccer_scoreboard", "true",
         ("Soccer Scoreboard", "Fußball-Anzeigetafel", "Marcador de Fútbol",
          "Fotbollsplanens Resultattavla"))):
    C.textures["scoreboard_icon_" + _k] = _sb_icon(_k)
    _icon = vc.model({"face": T("scoreboard_icon_" + _k), "cab": T("board_cab"),
                      "steel": T("steel_dark"), "particle": T("board_cab")},
                     [box([0, 6, 6], [16, 15, 10], "cab", per={"north": "face"}),
                      box([3, 0, 7], [4, 6, 9], "steel"), box([12, 0, 7], [13, 6, 9], "steel")])
    C.add(_reg, unit_java("BlockFieldScoreboard", _reg, sb_boxes(FB_BOARD, FB_LEGS),
                          ", " + _soccer),
          _names, {_reg + "_item": _icon},
          unit_state("field_scoreboard", _reg + "_item", ambient=False),
          item=item_of(_reg + "_item", 1.0), tab=TAB)


# ------------------------------------------------------------------------------------------
# Where the fixtures go on a canonical field (--layout)
# ------------------------------------------------------------------------------------------
def football_objects():
    f = FAMILIES["football"].field
    out = []
    # goal posts: west faces east (meta 3), east faces west (meta 1); 180 degree symmetric
    out.append((GP_CELL_X, 1, 0, "football_goal_post", 3, None))
    out.append((-GP_CELL_X, 1, 0, "football_goal_post", 1, None))
    # pylons: the four corners of each end zone, just outside the field of play
    for gx in (f.gx[0] - 0.1, f.ex[0] + 0.1, f.gx[1] + 0.1, f.ex[1] - 0.1):
        for sz in (f.sz[0] - 0.15, f.sz[1] + 0.15):
            out.append((int(math.floor(gx)), 1, int(math.floor(sz)), "end_zone_pylon", 2, None))
    # team benches on the south (home) sideline's team area, between the 30s, 8 blocks out
    zb = int(math.floor(f.sz[1] + f.border)) + 6
    for x in list(range(-17, -4)) + list(range(6, 19)):
        out.append((x, 1, zb, "football_team_bench", 2, None))
    out.append((-18, 1, zb, "sideline_heater", 2, None))
    out.append((19, 1, zb, "sideline_fan", 2, None))
    # the chain crew on the north sideline: the ball on the home 35, ten yards to go
    zc = int(math.floor(f.sz[0] - f.border)) - 1
    x35 = f.gx[0] + 35 * ff.YD
    x45 = f.gx[0] + 45 * ff.YD
    out.append((int(math.floor(x35)), 1, zc, "football_chain_stake", 0, None))
    out.append((int(math.floor(x45)), 1, zc, "football_chain_stake", 0, None))
    for x in range(int(math.floor(x35)) + 1, int(math.floor(x45))):
        out.append((x, 1, zc, "football_chain", 1, None))
    out.append((int(math.floor(x35)) + 2, 1, zc, "football_down_box", 0 | 2 << 2, None))
    # end zone lettering: over each end zone's middle, facing the field
    for x, meta, txt in ((int(math.floor((f.ex[0] + f.gx[0]) / 2)), 3, "HOME"),
                         (int(math.floor((f.ex[1] + f.gx[1]) / 2)), 1, "AWAY")):
        out.append((x, 1, 0, "football_end_zone_lettering", meta, '{txt:"%s"}' % txt))
    return out


def soccer_objects():
    out = [(-52, 1, 0, "soccer_goal", 3, None), (52, 1, -1, "soccer_goal", 1, None)]
    for (x, z, m) in ((-52, -34, 3), (52, -34, 1), (-52, 33, 3), (52, 33, 1)):
        out.append((x, 1, z, "soccer_corner_flag", m, None))
    # substitutes' shelters on the south side, either side of halfway, 3 blocks off the line
    for x in list(range(-14, -4)) + list(range(6, 16)):
        out.append((x, 1, 37, "soccer_substitutes_shelter", 2, None))
    out.append((0, 1, 36, "fourth_official_board", 2, None))
    return out


OBJECTS = {"football": football_objects, "soccer": soccer_objects}
TURNS_WITH_FACING = {"football_goal_post", "soccer_goal", "soccer_goal_small", "end_zone_pylon",
                     "football_team_bench", "sideline_heater", "sideline_fan",
                     "football_chain_stake", "football_chain", "football_down_box",
                     "football_end_zone_lettering", "soccer_corner_flag",
                     "soccer_substitutes_shelter", "fourth_official_board"}


# ------------------------------------------------------------------------------------------
# Messages
# ------------------------------------------------------------------------------------------
L = C.add_lang
L("itemGroup.tabvenuesfootball", ("CSM: Sports & Venues - Football & Soccer",
                                  "CSM: Sport & Veranstaltungsstätten - Football & Fußball",
                                  "CSM: Deportes y Recintos - Fútbol Americano y Fútbol",
                                  "CSM: Sport & Arenor - Amerikansk Fotboll & Fotboll"))
for _i, _n in enumerate((("Home team", "Heimmannschaft", "Equipo local", "Hemmalag"),
                         ("Away team", "Gastmannschaft", "Equipo visitante", "Bortalag"),
                         ("Score: home away", "Spielstand: Heim Gast", "Marcador: local visitante",
                          "Ställning: hemma borta"),
                         ("Quarter", "Viertel", "Cuarto", "Period"),
                         ("Clock (m:ss; stop it to set it)", "Uhr (m:ss; zum Stellen anhalten)",
                          "Reloj (m:ss; deténgalo para ajustarlo)",
                          "Klocka (m:ss; stoppa den för att ställa)"),
                         ("Down, distance, ball on", "Down, Distanz, Ball an der",
                          "Down, distancia, balón en", "Down, distans, bollen på"),
                         ("Time-outs: home away", "Auszeiten: Heim Gast",
                          "Tiempos muertos: local visitante", "Timeouts: hemma borta"))):
    L("gui.csm.venue_text.football.%d" % _i, _n)
for _i, _n in enumerate((("Home team", "Heimmannschaft", "Equipo local", "Hemmalag"),
                         ("Away team", "Gastmannschaft", "Equipo visitante", "Bortalag"),
                         ("Score: home away", "Spielstand: Heim Gast", "Marcador: local visitante",
                          "Ställning: hemma borta"),
                         ("Half", "Halbzeit", "Tiempo", "Halvlek"),
                         ("Clock (mm:ss played; stop it to set it)",
                          "Uhr (mm:ss gespielt; zum Stellen anhalten)",
                          "Reloj (mm:ss jugados; deténgalo para ajustarlo)",
                          "Klocka (mm:ss spelat; stoppa den för att ställa)"),
                         ("Added minutes", "Nachspielzeit (Minuten)", "Minutos añadidos",
                          "Tillägg (minuter)"))):
    L("gui.csm.venue_text.soccer.%d" % _i, _n)
for _i, _n in enumerate((("Lettering", "Schriftzug", "Letras", "Text"),
                         ("Letter height (blocks)", "Buchstabenhöhe (Blöcke)",
                          "Altura de las letras (bloques)", "Bokstavshöjd (block)"),
                         ("Widest (blocks)", "Größte Breite (Blöcke)", "Ancho máximo (bloques)",
                          "Största bredd (block)"))):
    L("gui.csm.venue_text.lettering.%d" % _i, _n)
L("gui.csm.venue_text.choice.possession", ("Possession: %s", "Ballbesitz: %s", "Posesión: %s",
                                           "Bollinnehav: %s"))
L("gui.csm.venue_text.choice.lettering", ("Colour: %s", "Farbe: %s", "Color: %s", "Färg: %s"))
L("gui.csm.venue_text.choice.outline", ("Outline: %s", "Umriss: %s", "Contorno: %s",
                                        "Kontur: %s"))
L("csm.venues.colour.none", ("none", "keiner", "ninguno", "ingen"))
for _i, _n in enumerate((("nobody", "niemand", "nadie", "ingen"),
                         ("home", "Heim", "local", "hemma"),
                         ("away", "Gast", "visitante", "borta"))):
    L("csm.venues.football.possession.%d" % _i, _n)
L("csm.venues.football.down", ("Down: %s", "Down: %s", "Down: %s", "Down: %s"))


# ------------------------------------------------------------------------------------------
# A field's blocks, for a builder (or a build script): --layout football|soccer
# ------------------------------------------------------------------------------------------
ZONE_DEFAULT = 0        # navy


def ground(sport, zone_colour=ZONE_DEFAULT):
    """{(x, z): (block, meta)} for every cell of a field, in the canonical frame."""
    fam = FAMILIES[sport]
    f = fam.field
    out = {}
    for (cx, cz), v in fam.cen.items():
        if v[0] == "picture":
            out[(cx, cz)] = ("%s_painted" % sport, 0)
            continue
        label, cls = v[1], v[2]
        if cls == ff.ZONE:
            out[(cx, cz)] = ("football_end_zone", zone_colour)
        elif label == ff.WHITE:
            out[(cx, cz)] = ("field_turf", 2)
        elif cls == ff.FIXED:
            out[(cx, cz)] = ("field_turf", 3)
        else:
            out[(cx, cz)] = ("field_turf", 1 if f.stripe_dark(cx + 0.5) else 0)
    for k, ((ax, az), _cells) in enumerate(fam.plan):
        b = k // KINDS_PER_ANCHOR
        reg = fam.anchor_blocks[b][0]
        out[(ax, az)] = (reg, (k - b * KINDS_PER_ANCHOR) << 1)
    return out


def verify_layout(sport, g):
    """Every painted cell finds, as the Java searches, the picture the field draws there."""
    fam = FAMILIES[sport]
    anchors = {}
    for (c, (b, m)) in g.items():
        for (reg, first, _n) in fam.anchor_blocks:
            if b == reg:
                anchors[c] = first + (m >> 1)
    bad = []
    for (c, (b, _m)) in g.items():
        if b != "%s_painted" % sport:
            continue
        found = None
        for ring in range(1, ff.REACH + 1):
            for dz in range(-ring, ring + 1):
                edge = dz in (-ring, ring)
                for dx in (range(-ring, ring + 1) if edge else (-ring, ring)):
                    a = (c[0] + dx, c[1] + dz)
                    if a in anchors:
                        t = fam.tables[anchors[a]].get((c[0] - a[0], -(c[1] - a[1])))
                        if t is not None:
                            found = t
                            break
                if found:
                    break
            if found:
                break
        want = fam.picture_at(c)
        if found != want:
            bad.append(c)
    if bad:
        raise SystemExit("%s layout: %d painted cells wrong, e.g. %s" % (sport, len(bad), bad[:6]))


def objects(sport):
    """The field's fixtures: (x, dy, z, block, meta, nbt) in the canonical frame."""
    return OBJECTS[sport]()




def rotate(x, z, east):
    """The canonical frame turned a quarter clockwise (a field laid north-south)."""
    return (-z, x) if east else (x, z)


def turn_meta(block, meta, east):
    """A fixture's facing metadata turned a quarter clockwise with the field: S 0 -> W 1 ->
    N 2 -> E 3 -> S 0."""
    if not east or block not in TURNS_WITH_FACING:
        return meta
    return (meta & ~3) | (((meta & 3) + 1) % 4)


def commands(sport, x0, y0, z0, east=False, zone=ZONE_DEFAULT):
    """Commands laying the field with its centre cell at (x0, y0, z0) (the ground's level),
    runs along x merged into /fill."""
    g = ground(sport, zone)
    verify_layout(sport, g)
    cells = {}
    for (x, z), (b, m) in g.items():
        if "_marking_anchor" in b and east:
            m |= 1
        cells[rotate(x, z, east)] = (b, m)
    out = []
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
                out.append("fill %d %d %d %d %d %d csm:%s %d" % (
                    x0 + row[i], y0, z0 + wz, x0 + row[j], y0, z0 + wz, b, m))
            else:
                out.append("setblock %d %d %d csm:%s %d" % (x0 + row[i], y0, z0 + wz, b, m))
            i = j + 1
    for (x, dy, z, b, m, nbt) in objects(sport):
        wx, wz = rotate(x, z, east)
        m = turn_meta(b, m, east)
        out.append("setblock %d %d %d csm:%s %d%s" % (x0 + wx, y0 + dy, z0 + wz, b, m,
                                                       " replace " + nbt if nbt else ""))
    return out


def layout_json(sport, zone=ZONE_DEFAULT):
    g = ground(sport, zone)
    verify_layout(sport, g)
    return {"field": sport,
            "frame": "the field's centre cell at (0, 0), its length east-west; ground blocks at "
                     "the floor level, objects dy above it",
            "ground": [[x, z, b, m] for (x, z), (b, m) in sorted(
                g.items(), key=lambda kv: (kv[0][1], kv[0][0]))],
            "objects": [list(o) for o in objects(sport)]}


def doc():
    out = {}
    for sport, fam in FAMILIES.items():
        g = ground(sport)
        counts = {}
        for (_c, (b, _m)) in g.items():
            counts[b] = counts.get(b, 0) + 1
        out[sport] = {"pictures": len(fam.pics), "anchor_kinds": len(fam.tables),
                      "anchors": [{"kind": k, "cell": a} for k, (a, _c) in enumerate(fam.plan)],
                      "anchor_blocks": fam.anchor_blocks, "ground_blocks": counts,
                      "extent": fam.field.extent}
    out.update(DOC)
    return out


# ------------------------------------------------------------------------------------------
# Main
# ------------------------------------------------------------------------------------------
def java_files():
    return {os.path.join(JAVA_DIR, name): text for name, text in JAVA.items()}


def main():
    if "--layout" in sys.argv:
        sport = sys.argv[sys.argv.index("--layout") + 1]
        zone = ZONE_DEFAULT
        if "--zone" in sys.argv:
            zone = int(sys.argv[sys.argv.index("--zone") + 1])
        if "--commands" in sys.argv:
            i = sys.argv.index("--commands")
            x, y, z = (int(v) for v in sys.argv[i + 1:i + 4])
            print("\n".join(commands(sport, x, y, z, "--east" in sys.argv, zone)))
        else:
            print(json.dumps(layout_json(sport, zone)))
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
