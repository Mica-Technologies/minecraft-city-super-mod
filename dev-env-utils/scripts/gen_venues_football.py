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
# Messages
# ------------------------------------------------------------------------------------------
L = C.add_lang
L("itemGroup.tabvenuesfootball", ("CSM: Sports & Venues - Football & Soccer",
                                  "CSM: Sport & Veranstaltungsstätten - Football & Fußball",
                                  "CSM: Deportes y Recintos - Fútbol Americano y Fútbol",
                                  "CSM: Sport & Arenor - Amerikansk Fotboll & Fotboll"))


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


OBJECTS = {"football": lambda: [], "soccer": lambda: []}
# blocks whose metadata bits 0-1 are a facing (S 0, W 1, N 2, E 3), turned with the field
TURNS_WITH_FACING = set()


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
