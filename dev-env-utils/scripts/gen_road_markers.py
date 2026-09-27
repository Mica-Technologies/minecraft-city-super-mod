#!/usr/bin/env python3
"""Mile markers and post markers for the Roads module (issue #237).

Two families, one catalogue each, everything they ship written from here.

MILE MARKERS (MUTCD 2H.05, the D10 reference location signs), road signs on the mod's sign
posts, in the Road Signs tab:

    mile_marker_sign_1 / _2 / _3                 D10-1, D10-2, D10-3: MILE over 1, 2 or 3
                                                 stacked digits
    mile_marker_sign_1_intermediate / _2_ / _3_  D10-1a, D10-2a, D10-3a: the same with a
                                                 tenth-of-a-mile panel under a divider
    mile_marker_sign_enhanced                    D10-4: direction, route shield, MILE, number
    mile_marker_sign_enhanced_intermediate       D10-5: the same with its tenth panel

The plate carries only what never changes -- the panel, its border, MILE and the divider --
drawn from the book's dimensions (Guide chapter pp. 3-80 to 3-83) with MILE set in the real
FHWA series (B on the 10 in plates, C on the 18 in ones) through shs_signs. The number, the
tenth, the direction word, the route shield and its route number are the tile entity's, and
are drawn by MileMarkerBakedModel as quads baked into the chunk mesh from one glyph sheet:

    textures/blocks/trafficsigns/mile_marker_<plate>.png   the plate, cut into near-square
                                                           cells (see below)
    textures/blocks/trafficsigns/mile_marker_glyphs.png    Series D numerals and point, the
                                                           guide sign font's numerals (cut
                                                           from textures/fonts/guide_sign_font
                                                           .png, so a route number here is
                                                           the route marker sign's), and
                                                           NORTH/SOUTH/EAST/WEST in Series B
    models/block/trafficsigns/mile_marker_<plate>{,_setback,_back_to_back}.json
    blockstates/<registry>.json
    java .../trafficsigns/MileMarkerLayout.java            every slot's position and every
                                                           glyph's cell and metrics, so the
                                                           baked model cannot disagree with
                                                           the textures it draws from

Why cells. A 10 x 48 in plate squished into a square texture has five times the texels across
as down, and Minecraft picks the mip level from the denser axis: MILE turned to mush a few
blocks out. So a plate's face is cut into 2 to 4 cells of about its width square, each drawn
into a quarter of the texture and painted on its own face; texel density is then about the same
both ways, and the texture stays at the size sign_texture_size gives the plate.

POST MARKERS, ground devices that settle onto the road like the work zone devices
(BlockWorkZoneDeviceDiagonal, all eight facings):

    delineator_uchannel_{white,yellow,red}, _{white,yellow}_double   Streetscape tab: a
                    reflector plate (two, stacked, on the double) on a green steel U-channel
    delineator_flexible_{white,yellow,red}                          Streetscape tab: a flat
                    composite marker post with a band of reflective sheeting
    object_marker_om1_1 .. om3_r, end_of_road_marker_om4_1 .. _3    Road Signs tab (warning
                    group): the OM1, OM2, OM3 object markers and the OM4 end of roadway
                    markers, each on its own U-channel post

Object marker faces are the book's drawings (Markers chapter pp. 11-1 to 11-4) through
shs_signs.book_sign, recoloured onto the mod palette; OM3-L is OM3-R mirrored, and the OM1-1
and OM4-1, which the book draws as outlines on white, are filled in its colour.

Usage:
    python gen_road_markers.py              write the tree (textures, models, blockstates, Java)
    python gen_road_markers.py --apply      also insert the lang lines into all four languages
    python gen_road_markers.py --check      write nothing; exit 1 on drift (lang included)
    python gen_road_markers.py --fragments  print the tab registration lines
    python gen_road_markers.py --sheet out.png   a contact sheet of every face and the glyphs
"""

import argparse
import filecmp
import json
import os
import shutil
import sys
import tempfile

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import shs_signs as shs  # noqa: E402
import sign_texture_size as sts  # noqa: E402

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ROADS = os.path.join(REPO, "modules", "roads", "src", "main", "resources", "assets", "csm")
SIGN_TEX = os.path.join(ROADS, "textures", "blocks", "trafficsigns")
SIGN_MODELS = os.path.join(ROADS, "models", "block", "trafficsigns")
MARK_TEX = os.path.join(ROADS, "textures", "blocks", "streetscape", "markers")
MARK_MODELS = os.path.join(ROADS, "models", "block", "streetscape", "markers")
STATES = os.path.join(ROADS, "blockstates")
LANG_DIR = os.path.join(ROADS, "lang")
FONT_PNG = os.path.join(ROADS, "textures", "fonts", "guide_sign_font.png")
FONT_JSON = os.path.join(ROADS, "fonts", "guide_sign_font.json")
JAVA = os.path.join(REPO, "modules", "roads", "src", "main", "java", "com", "micatechnologies",
                    "minecraft", "csm", "trafficsigns", "MileMarkerLayout.java")

GREEN = shs.MOD_COLOURS["green"]
WHITE = shs.MOD_COLOURS["white"]
YELLOW = shs.MOD_COLOURS["yellow"]
RED = shs.MOD_COLOURS["red"]
BLACK = shs.MOD_COLOURS["black"]
BLANK = "csm:blocks/trafficsigns/absolutely_nothing_sign"
GLYPHS = "mile_marker_glyphs"

FACINGS = (("n", 0), ("nw", 45), ("w", 90), ("sw", 135),
           ("s", 180), ("se", 225), ("e", 270), ("ne", 315))

LOCALES = ("en_us", "es_es", "de_de", "sv_se")


def r4(v):
    v = round(float(v), 4)
    return int(v) if v == int(v) else v


# ============================================================================================
# Mile markers: the catalogue
# ============================================================================================

IN_PER_UNIT = 1.5          # the sign catalogue's scale: a 12 x 36 in paddle is 8 x 24 units
ART_Z = {"none": 0.0, "setback": 12.5, "backtoback": 28.49}

# Every plate in inches, measured off the book: w x h, border and outer corner radius (the
# 10 in plates measured off the D10-1 drawing, the 18 in ones as dimensioned), MILE (top,
# cap, series), the stacked digit slots (top of each 6 in D numeral), the divider (centre)
# and the tenth panel's numeral (top, cap), and the enhanced layout's bands.
PLATES = [
    dict(key="d10_1", registry="mile_marker_sign_1", code="D10-1", w=10, h=18, cells=2,
         digits=[9]),
    dict(key="d10_2", registry="mile_marker_sign_2", code="D10-2", w=10, h=27, cells=3,
         digits=[9, 18]),
    dict(key="d10_3", registry="mile_marker_sign_3", code="D10-3", w=10, h=36, cells=4,
         digits=[9, 18, 27]),
    dict(key="d10_1a", registry="mile_marker_sign_1_intermediate", code="D10-1a", w=10, h=27,
         cells=3, digits=[9], divider=16.5, tenth=(18, 6)),
    dict(key="d10_2a", registry="mile_marker_sign_2_intermediate", code="D10-2a", w=10, h=36,
         cells=4, digits=[9, 18], divider=25.5, tenth=(27, 6)),
    dict(key="d10_3a", registry="mile_marker_sign_3_intermediate", code="D10-3a", w=10, h=48,
         cells=4, cuts=(0, 12, 24, 37, 48), digits=[9, 18, 27], divider=36.0, tenth=(39, 6)),
    dict(key="d10_4", registry="mile_marker_sign_enhanced", code="D10-4", w=18, h=54, cells=3,
         cuts=(0, 12, 29, 54),
         enhanced=dict(direction=(4, 6), shield=(14, 13), mile=(31, 6), number=(41, 8))),
    dict(key="d10_5", registry="mile_marker_sign_enhanced_intermediate", code="D10-5", w=18,
         h=60, cells=3,
         enhanced=dict(direction=(2.25, 6), shield=(12.25, 13), mile=(29.25, 6),
                       number=(37.25, 8)),
         divider=47.5, tenth=(49.75, 8)),
]

MILE_NAMES = {
    "d10_1": ("Mile Marker Sign (1 Digit)", "Señal de Marcador de Milla (1 Dígito)",
              "Meilenmarkierungsschild (1 Ziffer)", "Milstolpsskylt (1 Siffra)"),
    "d10_2": ("Mile Marker Sign (2 Digits)", "Señal de Marcador de Milla (2 Dígitos)",
              "Meilenmarkierungsschild (2 Ziffern)", "Milstolpsskylt (2 Siffror)"),
    "d10_3": ("Mile Marker Sign (3 Digits)", "Señal de Marcador de Milla (3 Dígitos)",
              "Meilenmarkierungsschild (3 Ziffern)", "Milstolpsskylt (3 Siffror)"),
    "d10_1a": ("Intermediate Mile Marker Sign (1 Digit)",
               "Señal de Marcador de Milla Intermedio (1 Dígito)",
               "Zwischen-Meilenmarkierungsschild (1 Ziffer)",
               "Mellanliggande Milstolpsskylt (1 Siffra)"),
    "d10_2a": ("Intermediate Mile Marker Sign (2 Digits)",
               "Señal de Marcador de Milla Intermedio (2 Dígitos)",
               "Zwischen-Meilenmarkierungsschild (2 Ziffern)",
               "Mellanliggande Milstolpsskylt (2 Siffror)"),
    "d10_3a": ("Intermediate Mile Marker Sign (3 Digits)",
               "Señal de Marcador de Milla Intermedio (3 Dígitos)",
               "Zwischen-Meilenmarkierungsschild (3 Ziffern)",
               "Mellanliggande Milstolpsskylt (3 Siffror)"),
    "d10_4": ("Enhanced Mile Marker Sign", "Señal de Marcador de Milla Mejorada",
              "Erweitertes Meilenmarkierungsschild", "Utökad Milstolpsskylt"),
    "d10_5": ("Enhanced Intermediate Mile Marker Sign",
              "Señal de Marcador de Milla Intermedio Mejorada",
              "Erweitertes Zwischen-Meilenmarkierungsschild",
              "Utökad Mellanliggande Milstolpsskylt"),
}


def border_of(p):
    """(border, outer corner radius) in inches: measured off the D10-1 drawing for the 10 in
    plates (0.4 and 1.15, the book's table giving no radius), as dimensioned (0.5 and 1.5) on
    the 18 in D10-4 and D10-5."""
    return (0.5, 1.5) if p["w"] >= 18 else (0.4, 1.15)


def plate_units(p):
    """The plate's rectangle in model units: centred across the block and about y 8, except
    that a plate shorter than the post reaches its top, so no post shows above it."""
    w, h = p["w"] / IN_PER_UNIT, p["h"] / IN_PER_UNIT
    top = max(8 + h / 2, 16.0)
    return 8 - w / 2, top - h, 8 + w / 2, top


def y_of(p, inches):
    """A distance from the plate's top edge, in inches, as a model y."""
    return plate_units(p)[3] - inches / IN_PER_UNIT


# --------------------------------------------------------------------------------- the face

RENDER_PPI = 60


def mile_face(p):
    """The whole plate at RENDER_PPI: panel, border, MILE, divider. No numerals."""
    ppi = RENDER_PPI
    W, H = int(round(p["w"] * ppi)), int(round(p["h"] * ppi))
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    border, radius = border_of(p)
    d.rounded_rectangle((0, 0, W - 1, H - 1), radius=radius * ppi, fill=WHITE)
    b = border * ppi
    d.rounded_rectangle((b, b, W - 1 - b, H - 1 - b), radius=(radius - border) * ppi, fill=GREEN)
    if "divider" in p:
        y = p["divider"] * ppi
        d.rectangle((b, y - 0.25 * ppi, W - 1 - b, y + 0.25 * ppi), fill=WHITE)
    enh = p.get("enhanced")
    if enh:
        top, cap = enh["mile"]
        series = ("C",)
    else:
        top, cap = 3, 4
        series = ("B",)
    shs.set_legend_line(img, "MILE", W / 2, (top + cap / 2) * ppi, cap * ppi,
                        (p["w"] - 2 * border - 1) * ppi, WHITE, series)
    return img


def texture_size(p):
    """The size sign_texture_size's rule gives a plate whose cells are a quarter each."""
    cuts = cuts_of(p)
    cell_w = p["w"] / IN_PER_UNIT / 16
    cell_h = max(b - a for a, b in zip(cuts, cuts[1:])) / IN_PER_UNIT / 16
    return max(sts.FLOOR, sts.rule_size(2 * max(cell_w, cell_h)))


def cuts_of(p):
    """Where a plate's face is cut into cells, in inches from the top: equal cells unless the
    catalogue moves a cut off a line of legend or the divider, so no seam runs through one."""
    if "cuts" in p:
        return p["cuts"]
    n = p["cells"]
    return tuple(p["h"] * k / n for k in range(n + 1))


def cell_slot(k):
    """Where cell k (0 at the top of the plate) is drawn in the texture: (col, row) of 2 x 2."""
    return k % 2, k // 2


def to_cells(face, n, size, axis="y", fractions=None):
    """The face cut into ``n`` cells along ``axis`` (equal, or at ``fractions`` of its length)
    and each squished into a quarter of a square texture, reduced to ``size`` with the sign
    filter."""
    big = size * 4
    half = big // 2
    out = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    fr = fractions or [k / n for k in range(n + 1)]
    for k in range(n):
        if axis == "y":
            y0 = round(face.height * fr[k])
            y1 = round(face.height * fr[k + 1])
            piece = face.crop((0, y0, face.width, y1))
        else:
            x0 = round(face.width * k / n)
            x1 = round(face.width * (k + 1) / n)
            piece = face.crop((x0, 0, x1, face.height))
        c, r = cell_slot(k)
        out.paste(piece.resize((half, half), Image.LANCZOS), (c * half, r * half))
    return sts.reduce(out, size)


def mile_texture(p):
    return to_cells(mile_face(p), p["cells"], texture_size(p),
                    fractions=[c / p["h"] for c in cuts_of(p)])


# --------------------------------------------------------------------------------- the glyphs

SHEET = 256
SS = 4                       # the sheet is drawn at four times and reduced with the sign filter
D_CHARS = "0123456789."
D_CELL = (32, 48)
D_CAP, D_BASE = 28, 40
G_CHARS = "0123456789"
G_CELL = (32, 36)
G_TOP = 96
DIR_WORDS = ("NORTH", "SOUTH", "EAST", "WEST")
DIR_CELL = (128, 32)
DIR_TOP = 192
DIR_CAP, DIR_BASE = 20, 26


def _font_metrics():
    with open(FONT_JSON, encoding="utf-8") as fh:
        return json.load(fh)


def glyph_sheet():
    """The sheet, and every glyph's record for the Java table:
    (u0, v0, u1, v1, cellW, cellH, originX, baseline, cap, advance), sheet pixels."""
    big = Image.new("RGBA", (SHEET * SS, SHEET * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    recs = {"D": [], "G": [], "DIR": []}

    font = shs.series_font("D", D_CAP * SS)
    top = font.getbbox("H")[1]
    for i, ch in enumerate(D_CHARS):
        x, y = (i % 8) * D_CELL[0], (i // 8) * D_CELL[1]
        adv = font.getlength(ch) / SS
        origin = (D_CELL[0] - adv) / 2
        d.text(((x + origin) * SS, (y + D_BASE - D_CAP) * SS - top), ch, font=font, fill=WHITE)
        recs["D"].append((x, y, D_CELL[0], D_CELL[1], origin, D_BASE, D_CAP, adv))

    metrics = _font_metrics()
    atlas = Image.open(FONT_PNG).convert("RGBA")
    cw, ch_ = int(metrics["cellWidth"]), int(metrics["cellHeight"])
    k = G_CELL[0] / metrics["cellWidth"]
    for i, c in enumerate(G_CHARS):
        g = metrics["glyphs"][c]
        cell = atlas.crop((g["col"] * cw, g["row"] * ch_, (g["col"] + 1) * cw,
                           (g["row"] + 1) * ch_))
        alpha = cell.getchannel("A")
        white = Image.new("RGBA", cell.size, WHITE)
        white.putalpha(alpha)
        white = white.resize((G_CELL[0] * SS, G_CELL[1] * SS), Image.LANCZOS)
        x, y = (i % 8) * G_CELL[0], G_TOP + (i // 8) * 48
        big.alpha_composite(white, (x * SS, y * SS))
        recs["G"].append((x, y, G_CELL[0], G_CELL[1], metrics["originX"] * k,
                          metrics["baseline"] * k, metrics["capHeight"] * k, g["advance"] * k))

    for i, word in enumerate(DIR_WORDS):
        x, y = (i % 2) * DIR_CELL[0], DIR_TOP + (i // 2) * DIR_CELL[1]
        w = shs.legend_width(word, "B", DIR_CAP * SS) / SS
        shs.set_legend_line(big, word, (x + DIR_CELL[0] / 2) * SS,
                            (y + DIR_BASE - DIR_CAP / 2) * SS, DIR_CAP * SS,
                            DIR_CELL[0] * SS, WHITE, ("B",))
        recs["DIR"].append((x, y, DIR_CELL[0], DIR_CELL[1], (DIR_CELL[0] - w) / 2, DIR_BASE,
                            DIR_CAP, w))
    return sts.reduce(big, SHEET), recs


# --------------------------------------------------------------------------------- models

def _post(z0):
    """The standard five-bar sign post every sign model carries, front face at ``z0``."""
    bars = ((7.25, 8.75, 0.75, 3.25, None), (7.0, 9.0, 1.0, 3.0, None),
            (6.5, 9.5, 1.5, 2.5, None), (6.75, 9.25, 1.25, 2.75, None),
            (7.5, 8.5, 0.5, 3.5, "north"))
    els = []
    for x0, x1, za, zb, drop in bars:
        faces = {}
        for side in ("north", "east", "south", "west", "up", "down"):
            if side == drop:
                continue
            face = {"uv": [0, 0, 4, 16], "texture": "#0"}
            if drop == "north" and side in ("up", "down"):
                face["rotation"] = 90 if side == "up" else 270
            faces[side] = face
        els.append({"from": [x0, 0, r4(za + z0)], "to": [x1, 16, r4(zb + z0)],
                    "rotation": {"angle": 0, "axis": "y", "origin": [8, 0, 8]}, "faces": faces})
    return els


def _cells(p, z0, z1):
    x0, y0, x1, y1 = plate_units(p)
    cuts = cuts_of(p)
    out = []
    for k in range(p["cells"]):
        top = y_of(p, cuts[k])
        bot = y_of(p, cuts[k + 1])
        c, r = cell_slot(k)
        out.append({"from": [r4(x0), r4(bot), r4(z0)], "to": [r4(x1), r4(top), r4(z1)],
                    "faces": {"north": {"uv": [c * 8, r * 8, c * 8 + 8, r * 8 + 8],
                                        "texture": "#1"}}})
    return out


def _plate_box(p, z0, z1):
    x0, y0, x1, y1 = plate_units(p)
    faces = {s: {"uv": [0, 0, 16, 16], "texture": "#0"}
             for s in ("east", "south", "west", "up", "down")}
    return {"from": [r4(x0), r4(y0), r4(z0)], "to": [r4(x1), r4(y1), r4(z1)], "faces": faces}


# Every element carries an explicit zero rotation. Vanilla's FaceBakery squares up a face whose
# element has no rotation (applyFacing rebuilds its corners from their bounding box, along the
# nearest axis), which is harmless at the four cardinal facings but flattens a face turned 45
# degrees by the blockstate onto one side of its block: the diagonal facings drew the plate and
# the marker posts square to the grid while the legend, baked separately, stood turned. A zero
# rotation is drawn exactly as none is, and skips that step.
_ZERO_TURN = {"angle": 0, "axis": "y", "origin": [8, 0, 8]}


def _turnable(elements):
    for e in elements:
        e.setdefault("rotation", dict(_ZERO_TURN))
    return elements


def _gui_scale(h_units):
    return r4(0.625 * min(1.0, 24.0 / max(h_units, 16.0)))


def _sign_model(p, elements):
    h = p["h"] / IN_PER_UNIT
    s = _gui_scale(h)
    return {"credit": "CSM mile marker %s (numbers drawn by MileMarkerBakedModel); "
                      "gen_road_markers.py" % p["code"],
            "textures": {"0": BLANK},
            "elements": _turnable(elements),
            "display": {"gui": {"rotation": [0, 180, 0], "scale": [s, s, s]},
                        "fixed": {"rotation": [0, 180, 0], "scale": [s, s, s]}}}


def mile_models(p):
    base = "mile_marker_" + p["key"]
    out = {}
    out[base] = _sign_model(p, [_plate_box(p, 0, 0.5)] + _cells(p, 0, 0.01) + _post(0))
    out[base + "_setback"] = _sign_model(p, [_plate_box(p, 12.5, 13)] + _cells(p, 12.5, 12.51)
                                         + _post(12.5))
    x0, y0, x1, y1 = plate_units(p)
    backing = {"from": [r4(x0), r4(y0), 28.9], "to": [r4(x1), r4(y1), 29],
               "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#0"}}}
    out[base + "_back_to_back"] = _sign_model(p, [_plate_box(p, 28.5, 29), backing]
                                              + _cells(p, 28.49, 28.5))
    return out


def _facing_variants():
    out = {}
    for name, angle in FACINGS:
        out[name] = ({"transform": {"rotation": [{"x": 0}, {"y": angle}, {"z": 0}]}}
                     if angle else {})
    return out


def mile_blockstate(p):
    base = "csm:trafficsigns/mile_marker_" + p["key"]
    tex = "csm:blocks/trafficsigns/mile_marker_" + p["key"]
    return {
        "forge_marker": 1,
        "defaults": {
            "model": base,
            "textures": {"all": tex, "particle": tex, "0": BLANK, "1": tex,
                         # Not drawn by any face: named so the sheet is on the atlas and every
                         # asset tool sees it. MileMarkerBakedModel draws from it.
                         "glyphs": "csm:blocks/trafficsigns/" + GLYPHS},
        },
        "variants": {
            "facing": _facing_variants(),
            "inventory": [{}],
            "downward": {"false": {}, "true": {"submodel": {"extension": {
                "model": "csm:trafficsigns/sign_pole",
                "transform": {"translation": [0.0, -1.0, 0.0]}}}}},
            "shift": {"none": {}, "setback": {"model": base + "_setback"},
                      "backtoback": {"model": base + "_back_to_back"}},
            "normal": [{}],
        },
    }


# --------------------------------------------------------------------------------- the Java

def _slots(p):
    """Model-unit slot data: mile (stacked: cy, cap per digit; run: cy, cap, maxW), tenth,
    direction (cy, cap, maxW), shield (cy, size)."""
    U = 1 / IN_PER_UNIT
    border, _r = border_of(p)
    inner = (p["w"] - 2 * border - 1.0) * U
    enh = p.get("enhanced")
    mile, tenth, direction, shield = [], None, None, None
    if enh:
        top, cap = enh["number"]
        mile = [y_of(p, top + cap / 2), cap * U, inner]
        top, cap = enh["direction"]
        direction = [y_of(p, top + cap / 2), cap * U, inner]
        top, size = enh["shield"]
        shield = [y_of(p, top + size / 2), size * U]
    else:
        for top in p["digits"]:
            mile += [y_of(p, top + 3), 6 * U]
    if "tenth" in p:
        top, cap = p["tenth"]
        tenth = [y_of(p, top + cap / 2), cap * U, inner]
    return mile, tenth, direction, shield


def _farr(vals):
    if vals is None:
        return "null"
    return "new float[] {" + ", ".join("%.4ff" % v for v in vals) + "}"


def java_source(recs):
    lines = []
    for p in PLATES:
        mile, tenth, direction, shield = _slots(p)
        digits = 3 if p.get("enhanced") else len(p["digits"])
        lines.append('  %s("%s", "%s", %d, %s, %s,\n      %s,\n      %s,\n      %s,\n      %s)'
                     % (p["key"].upper(), p["registry"], p["code"], digits,
                        "false" if p.get("enhanced") else "true",
                        "true" if "tenth" in p else "false",
                        _farr(mile), _farr(tenth), _farr(direction), _farr(shield)))
    body = ",\n".join(lines) + ";"

    def table(name, entries, doc):
        rows = []
        for x, y, w, h, origin, base, cap, adv in entries:
            rows.append("      {%.4ff, %.4ff, %.4ff, %.4ff, %.4ff, %.4ff, %.4ff, %.4ff, %.4ff, %.4ff}"
                        % (x / 16, y / 16, (x + w) / 16, (y + h) / 16, w, h, origin, base, cap,
                           adv))
        return ("  /** %s */\n  public static final float[][] %s = {\n%s\n  };\n"
                % (doc, name, ",\n".join(rows)))

    return """package com.micatechnologies.minecraft.csm.trafficsigns;

// GENERATED by dev-env-utils/scripts/gen_road_markers.py -- do not edit by hand; change the
// catalogue there and re-run it (its --check fails when this file and the textures disagree).

/**
 * The eight mile marker plates (MUTCD D10-1 to D10-5) and where each draws what its tile entity
 * holds, in model units of the unturned model (the plate faces north, the reader stands north of
 * it, and the reader's x runs the other way from the model's: {@code modelX = 16 - readerX}).
 * Every legend is centred on the plate's centre line.
 *
 * <p>{@code mile} is, for a stacked plate, one {@code (centreY, cap)} pair per digit from the
 * top; for an enhanced plate one {@code (centreY, cap, maxWidth)} run. {@code tenth} and
 * {@code direction} are {@code (centreY, cap, maxWidth)} runs, {@code shield} a
 * {@code (centreY, size)} square. The glyph tables give each glyph's cell on the sheet
 * ({@code u0, v0, u1, v1} in sprite units) and its metrics in sheet pixels
 * ({@code cellW, cellH, originX, baseline, cap, advance}), the convention
 * {@code GuideSignFontRenderer} draws in.</p>
 *
 * @since 2026.9
 */
public enum MileMarkerLayout {

%s

  /** The glyph sheet's sprite. */
  public static final String SHEET = "csm:blocks/trafficsigns/%s";

  /** The characters of {@link #SERIES_D}, in order. */
  public static final String SERIES_D_CHARS = "%s";

%s
%s
%s
  private final String registryName;
  private final String code;
  private final int digits;
  private final boolean stacked;
  private final boolean tenth;
  private final float[] mileSlot;
  private final float[] tenthSlot;
  private final float[] directionSlot;
  private final float[] shieldSlot;

  MileMarkerLayout(String registryName, String code, int digits, boolean stacked,
      boolean tenth, float[] mileSlot, float[] tenthSlot, float[] directionSlot,
      float[] shieldSlot) {
    this.registryName = registryName;
    this.code = code;
    this.digits = digits;
    this.stacked = stacked;
    this.tenth = tenth;
    this.mileSlot = mileSlot;
    this.tenthSlot = tenthSlot;
    this.directionSlot = directionSlot;
    this.shieldSlot = shieldSlot;
  }

  public String getRegistryName() {
    return registryName;
  }

  /** The MUTCD sign code, for the editor's title. */
  public String getCode() {
    return code;
  }

  /** How many digits the mile number has: exactly this many on a stacked plate, at most this
   * many on an enhanced one. */
  public int getDigits() {
    return digits;
  }

  /** Whether the digits are stacked one above the other (D10-1 to D10-3a). */
  public boolean isStacked() {
    return stacked;
  }

  /** Whether the plate has a tenth-of-a-mile panel. */
  public boolean hasTenth() {
    return tenth;
  }

  /** Whether the plate carries a direction, a route shield and a route number. */
  public boolean isEnhanced() {
    return shieldSlot != null;
  }

  public float[] getMileSlot() {
    return mileSlot;
  }

  public float[] getTenthSlot() {
    return tenthSlot;
  }

  public float[] getDirectionSlot() {
    return directionSlot;
  }

  public float[] getShieldSlot() {
    return shieldSlot;
  }

  /** The smallest mile number the plate carries: a two-digit plate starts at 10. */
  public int getMinMile() {
    return stacked && digits > 1 ? (int) Math.pow(10, digits - 1) : 0;
  }

  /** The largest mile number the plate carries. */
  public int getMaxMile() {
    return (int) Math.pow(10, digits) - 1;
  }

  /** The number a newly placed marker shows. */
  public int getDefaultMile() {
    return Math.max(getMinMile(), Math.min(getMaxMile(), isEnhanced() ? 260 : 142));
  }
}
""" % (body, GLYPHS, D_CHARS,
       table("SERIES_D", recs["D"], "The mile number's FHWA Series D numerals and point."),
       table("GUIDE", recs["G"], "The guide sign font's numerals, for a route number on a "
             "shield,\n   * exactly as the route marker sign sets it."),
       table("DIRECTION", recs["DIR"], "NORTH, SOUTH, EAST and WEST in Series B, in that "
             "order."))


# ============================================================================================
# Post markers: the catalogue
# ============================================================================================

POST_TEX = "csm:blocks/streetscape/markers/uchannel_green"
FLEX_TEX = "csm:blocks/streetscape/markers/flexible_post_white"
BASE_TEX = "csm:blocks/streetscape/markers/flexible_post_base"
BACK_GRAY = (150, 150, 150, 255)

# name: (kind, source, plate size in units (w, h), plate top y, cells along the long side)
OBJECT_MARKERS = [
    ("object_marker_om1_1", "OM1-1", ("Markers", 0, 0, "fill_yellow"), (12.73, 12.73), 27.4, 1),
    ("object_marker_om1_2", "OM1-2", ("Markers", 0, 1, None), (12.73, 12.73), 27.4, 1),
    ("object_marker_om1_3", "OM1-3", ("Markers", 0, 2, None), (12.73, 12.73), 27.4, 1),
    ("object_marker_om2_1v", "OM2-1V", ("Markers", 1, 0, None), (3, 6), 22, 2),
    ("object_marker_om2_2v", "OM2-2V", ("Markers", 1, 1, None), (3, 6), 22, 2),
    ("object_marker_om2_1h", "OM2-1H", ("Markers", 1, 2, None), (6, 3), 21, 2),
    ("object_marker_om2_2h", "OM2-2H", ("Markers", 1, 3, None), (6, 3), 21, 2),
    ("object_marker_om3_l", "OM3-L", ("Markers", 2, 0, "mirror"), (6, 18), 28, 3),
    ("object_marker_om3_c", "OM3-C", ("Markers", 2, 1, None), (6, 18), 28, 3),
    ("object_marker_om3_r", "OM3-R", ("Markers", 2, 0, None), (6, 18), 28, 3),
    ("end_of_road_marker_om4_1", "OM4-1", ("Markers", 3, 0, "fill_red"), (12.73, 12.73), 27.4, 1),
    ("end_of_road_marker_om4_2", "OM4-2", ("Markers", 3, 1, None), (12.73, 12.73), 27.4, 1),
    ("end_of_road_marker_om4_3", "OM4-3", ("Markers", 3, 2, None), (12.73, 12.73), 27.4, 1),
]

_T1 = ("Type 1 Object Marker", "Marcador de Objeto Tipo 1", "Objektmarkierung Typ 1",
       "Objektmarkering Typ 1")
_T2 = ("Type 2 Object Marker", "Marcador de Objeto Tipo 2", "Objektmarkierung Typ 2",
       "Objektmarkering Typ 2")
_T3 = ("Type 3 Object Marker", "Marcador de Objeto Tipo 3", "Objektmarkierung Typ 3",
       "Objektmarkering Typ 3")
_T4 = ("End of Roadway Marker", "Marcador de Fin de Vía", "Fahrbahnende-Markierung",
       "Vägslutsmarkering")
_NINE = ("Nine Reflectors", "Nueve Reflectores", "Neun Reflektoren", "Nio Reflektorer")
_BLACK_NINE = ("Black, Nine Reflectors", "Negro, Nueve Reflectores", "Schwarz, Neun Reflektoren",
               "Svart, Nio Reflektorer")
_PLAIN = ("Plain", "Liso", "Einfach", "Enkel")
_V3 = ("Vertical, Three Reflectors", "Vertical, Tres Reflectores", "Senkrecht, Drei Reflektoren",
       "Vertikal, Tre Reflektorer")
_H3 = ("Horizontal, Three Reflectors", "Horizontal, Tres Reflectores",
       "Waagerecht, Drei Reflektoren", "Horisontell, Tre Reflektorer")
_VP = ("Vertical, Plain", "Vertical, Liso", "Senkrecht, Einfach", "Vertikal, Enkel")
_HP = ("Horizontal, Plain", "Horizontal, Liso", "Waagerecht, Einfach", "Horisontell, Enkel")
_L = ("Left", "Izquierda", "Links", "Vänster")
_C = ("Center", "Centro", "Mitte", "Mitten")
_R = ("Right", "Derecha", "Rechts", "Höger")


def _nm(base, detail, code):
    return tuple("%s (%s, %s)" % (b, d, code) for b, d in zip(base, detail))


OM_NAMES = {
    "object_marker_om1_1": _nm(_T1, _NINE, "OM1-1"),
    "object_marker_om1_2": _nm(_T1, _BLACK_NINE, "OM1-2"),
    "object_marker_om1_3": _nm(_T1, _PLAIN, "OM1-3"),
    "object_marker_om2_1v": _nm(_T2, _V3, "OM2-1V"),
    "object_marker_om2_2v": _nm(_T2, _VP, "OM2-2V"),
    "object_marker_om2_1h": _nm(_T2, _H3, "OM2-1H"),
    "object_marker_om2_2h": _nm(_T2, _HP, "OM2-2H"),
    "object_marker_om3_l": _nm(_T3, _L, "OM3-L"),
    "object_marker_om3_c": _nm(_T3, _C, "OM3-C"),
    "object_marker_om3_r": _nm(_T3, _R, "OM3-R"),
    "end_of_road_marker_om4_1": _nm(_T4, _NINE, "OM4-1"),
    "end_of_road_marker_om4_2": _nm(_T4, _BLACK_NINE, "OM4-2"),
    "end_of_road_marker_om4_3": _nm(_T4, _PLAIN, "OM4-3"),
}

# name: (kind, reflector colour, double)
DELINEATORS = [
    ("delineator_uchannel_white", "uchannel", "white", False),
    ("delineator_uchannel_yellow", "uchannel", "yellow", False),
    ("delineator_uchannel_red", "uchannel", "red", False),
    ("delineator_uchannel_white_double", "uchannel", "white", True),
    ("delineator_uchannel_yellow_double", "uchannel", "yellow", True),
    ("delineator_flexible_white", "flexible", "white", False),
    ("delineator_flexible_yellow", "flexible", "yellow", False),
    ("delineator_flexible_red", "flexible", "red", False),
]

_COLOUR_NAMES = {"white": ("White", "Blanco", "Weiß", "Vit"),
                 "yellow": ("Yellow", "Amarillo", "Gelb", "Gul"),
                 "red": ("Red", "Rojo", "Rot", "Röd")}


def delineator_names(name, kind, colour, double):
    c = _COLOUR_NAMES[colour]
    if kind == "flexible":
        return tuple("%s (%s)" % (b, cc) for b, cc in zip(
            ("Flexible Marker Post", "Poste Marcador Flexible", "Flexibler Markierungspfosten",
             "Flexibel Markeringsstolpe"), c))
    ch = ("U-Channel", "Canal en U", "U-Profil", "U-Profil")
    dbl = ("Double ", "Doble ", "Doppelt ", "Dubbel ") if double else ("", "", "", "")
    return tuple("%s (%s, %s%s)" % (b, h, d, cc) for b, h, d, cc in zip(
        ("Delineator Post", "Poste Delineador", "Leitpfosten", "Kantstolpe"), ch, dbl, c))


REFLECTOR = {"white": (235, 235, 228, 255), "yellow": YELLOW, "red": RED}


# --------------------------------------------------------------------------------- textures

def om_face(src):
    chapter, page, pick, treat = src
    face = shs.book_sign(chapter, page, pick)
    if treat == "fill_yellow":
        # The book draws OM1-1's panel as an outline on the page's white; the marker is yellow,
        # its reflectors told apart only by their rims.
        face = shs.recolour(face, {(255, 255, 255): YELLOW, (119, 119, 119): (196, 158, 10),
                                   (255, 245, 0): (196, 158, 10)}, tol=90)
    elif treat == "fill_red":
        face = shs.recolour(face, {(255, 255, 255): RED, (119, 119, 119): (120, 18, 22),
                                   (217, 38, 28): (120, 18, 22)}, tol=90)
    else:
        face = shs.recolour(face, shs.SHS_PALETTE)
    if treat == "mirror":
        face = face.transpose(Image.FLIP_LEFT_RIGHT)
    return face


def om_textures(entry):
    name, _code, src, (w, h), _top, cells = entry
    face = om_face(src)
    if cells == 1:
        size = 128
        big = face.resize((size * 4, size * 4), Image.LANCZOS)
        front = sts.reduce(big, size)
    else:
        size = 64 if max(w, h) <= 6 else 128
        front = to_cells(face, cells, size, axis="y" if h >= w else "x")
    back = Image.new("RGBA", front.size, BACK_GRAY)
    back.putalpha(front.getchannel("A"))
    # The back of each cell, seen from behind: every cell mirrored where it lies.
    half = front.width // 2
    if cells == 1:
        back = back.transpose(Image.FLIP_LEFT_RIGHT)
    else:
        out = Image.new("RGBA", back.size, (0, 0, 0, 0))
        for k in range(4):
            c, r = cell_slot(k)
            box = (c * half, r * half, (c + 1) * half, (r + 1) * half)
            out.paste(back.crop(box).transpose(Image.FLIP_LEFT_RIGHT), box[:2])
        back = out
    return front, back


def reflector_texture(colour):
    """A delineator's reflector: a prismatic cube-corner field in its colour, rim darker."""
    size = 32
    base = REFLECTOR[colour]
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rim = tuple(int(c * 0.72) for c in base[:3]) + (255,)
    d.rounded_rectangle((0, 0, size - 1, size - 1), radius=4, fill=rim)
    d.rounded_rectangle((2, 2, size - 3, size - 3), radius=3, fill=base)
    for y in range(3, size - 3, 4):
        for x in range(3 + (y // 4 % 2) * 2, size - 3, 4):
            hi = tuple(min(255, int(c * 1.12 + 12)) for c in base[:3]) + (255,)
            d.point((x, y), fill=hi)
            d.point((x + 1, y + 1), fill=tuple(int(c * 0.86) for c in base[:3]) + (255,))
    return img


def sheeting_texture(colour):
    """A flexible post's band of reflective sheeting: flat colour with a fine grain."""
    size = 16
    base = REFLECTOR[colour]
    img = Image.new("RGBA", (size, size), base)
    for y in range(size):
        for x in range(size):
            if (x * 7 + y * 13) % 5 == 0:
                img.putpixel((x, y), tuple(min(255, int(c * 1.08 + 6)) for c in base[:3])
                             + (255,))
    # the band's edge, so white sheeting still reads on a white post
    edge = tuple(int(c * 0.7) for c in base[:3]) + (255,)
    for i in range(size):
        for x, y in ((i, 0), (i, size - 1), (0, i), (size - 1, i)):
            img.putpixel((x, y), edge)
    return img


def uchannel_texture():
    """Green-painted steel U-channel sign post with its 3/8 in holes at 1 in centres."""
    size = 16
    green = (58, 92, 64, 255)
    img = Image.new("RGBA", (size, size), green)
    for y in range(size):
        for x in range(size):
            k = 1.0 + 0.06 * ((x * 5 + y * 3) % 3 - 1)
            img.putpixel((x, y), tuple(int(c * k) for c in green[:3]) + (255,))
    for y in range(1, size, 4):
        for x in (7, 8):
            img.putpixel((x, y), (22, 26, 22, 255))
    return img


def flexible_texture():
    size = 16
    img = Image.new("RGBA", (size, size), (238, 238, 234, 255))
    for x in range(size):
        k = 0.94 + 0.06 * (1 - abs(x - 7.5) / 7.5)
        for y in range(size):
            img.putpixel((x, y), tuple(int(c * k) for c in (238, 238, 234)) + (255,))
    return img


def base_texture():
    return Image.new("RGBA", (16, 16), (46, 46, 48, 255))


def marker_textures():
    out = {"uchannel_green": uchannel_texture(), "flexible_post_white": flexible_texture(),
           "flexible_post_base": base_texture()}
    for colour in REFLECTOR:
        out["delineator_reflector_" + colour] = reflector_texture(colour)
        out["delineator_reflector_back"] = Image.new("RGBA", (16, 16), (170, 172, 170, 255))
        out["sheeting_" + colour] = sheeting_texture(colour)
    for entry in OBJECT_MARKERS:
        front, back = om_textures(entry)
        out[entry[0]] = front
        out[entry[0] + "_back"] = back
    return out


# --------------------------------------------------------------------------------- models

def _uv(side, f, t):
    """The UV Minecraft would take from a face's position, with y taken within its block."""
    x0, y0, z0 = f
    x1, y1, z1 = t
    k = 16 * int(y0 // 16) if y0 >= 0 else 0
    y0, y1 = y0 - k, y1 - k
    return {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
            "south": [x0, 16 - y1, x1, 16 - y0],
            "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
            "west": [z0, 16 - y1, z1, 16 - y0],
            "up": [x0, z0, x1, z1],
            "down": [x0, 16 - z1, x1, 16 - z0]}[side]


def box(f, t, tex, sides=("north", "east", "south", "west", "up", "down")):
    return {"from": [r4(v) for v in f], "to": [r4(v) for v in t],
            "faces": {s: {"uv": [r4(v) for v in _uv(s, f, t)], "texture": tex} for s in sides}}


def column(x0, x1, z0, z1, y0, y1, tex, sides=("north", "east", "south", "west", "up", "down")):
    """A box split at every block line, so no face's UVs run past 0..16."""
    out = []
    y = y0
    while y < y1 - 1e-6:
        top = min(y1, (int(y // 16) + 1) * 16)
        s = [x for x in sides if (x != "up" or top >= y1 - 1e-6) and (x != "down" or y <= y0)]
        out.append(box((x0, y, z0), (x1, top, z1), tex, s))
        y = top
    return out


def uchannel(top):
    """A steel U-channel post from the ground to ``top``: its web faces the front."""
    return (column(7.4, 8.6, 8.0, 8.2, 0, top, "#post")
            + column(7.4, 7.6, 8.2, 8.8, 0, top, "#post", ("east", "south", "west", "up", "down"))
            + column(8.4, 8.6, 8.2, 8.8, 0, top, "#post", ("east", "south", "west", "up", "down")))


def silhouette(x0, y0, x1, y1, z, cells, horizontal=False):
    """A plate cut to its outline: the face on #face, the gray back on #back, in cells."""
    out = []
    for k in range(cells):
        if cells == 1:
            uv = [0, 0, 16, 16]
            fx0, fy0, fx1, fy1 = x0, y0, x1, y1
        else:
            c, r = cell_slot(k)
            uv = [c * 8, r * 8, c * 8 + 8, r * 8 + 8]
            if horizontal:
                # cell 0 is the reader's left, which is the model's high x
                fx1 = x1 - (x1 - x0) * k / cells
                fx0 = x1 - (x1 - x0) * (k + 1) / cells
                fy0, fy1 = y0, y1
            else:
                fy1 = y1 - (y1 - y0) * k / cells
                fy0 = y1 - (y1 - y0) * (k + 1) / cells
                fx0, fx1 = x0, x1
        # The back texture is already mirrored (cell by cell), so the south face reads it
        # through the same window: a south face runs u the other way from a north one.
        out.append({"from": [r4(fx0), r4(fy0), r4(z)], "to": [r4(fx1), r4(fy1), r4(z + 0.25)],
                    "faces": {"north": {"uv": uv, "texture": "#face"},
                              "south": {"uv": uv, "texture": "#back"}}})
    return out


def _device_model(elements, textures, credit):
    ys = [e["to"][1] for e in elements] + [e["from"][1] for e in elements]
    lo, hi = min(ys), max(ys)
    s = r4(0.625 * min(1.0, 16.0 / max(16.0, hi - lo)))
    cy = (lo + hi) / 2
    return {"credit": credit + "; gen_road_markers.py",
            "ambientocclusion": False,
            "textures": dict(textures, particle=textures.get("post", textures.get("body"))),
            "elements": _turnable(elements),
            "display": {"gui": {"rotation": [10, 200, 0], "translation": [0, r4(-(cy - 8) * s), 0],
                                "scale": [s, s, s]},
                        "fixed": {"rotation": [0, 180, 0], "translation": [0, r4(-(cy - 8) * s), 0],
                                  "scale": [s, s, s]}}}


def om_model(entry):
    name, code, _src, (w, h), top, cells = entry
    x0, x1 = 8 - w / 2, 8 + w / 2
    y0 = top - h
    els = silhouette(x0, y0, x1, top, 7.7, cells, horizontal=w > h and cells > 1)
    post_top = (y0 + top) / 2 if cells == 1 else top - 0.5
    els += uchannel(post_top)
    tex = "csm:blocks/streetscape/markers/" + name
    return _device_model(els, {"face": tex, "back": tex + "_back", "post": POST_TEX},
                         "CSM object marker " + code)


def delineator_model(name, kind, colour, double):
    if kind == "uchannel":
        els = []
        for top in ((20, 15) if double else (20,)):
            els += silhouette(7.0, top - 4, 9.0, top, 7.7, 1)
        els += uchannel(19.5)
        textures = {"face": "csm:blocks/streetscape/markers/delineator_reflector_" + colour,
                    "back": "csm:blocks/streetscape/markers/delineator_reflector_back",
                    "post": POST_TEX}
        return _device_model(els, textures, "CSM delineator post, U-channel")
    els = column(7.1, 8.9, 7.8, 8.2, 0, 20, "#body")
    els.append(box((7.25, 14, 7.6), (8.75, 19, 7.8), "#sheeting", ("north",)))
    els.append(box((6.8, 0, 7.4), (9.2, 0.75, 8.6), "#base", ("north", "east", "south", "west",
                                                            "up")))
    textures = {"body": FLEX_TEX, "base": BASE_TEX,
                "sheeting": "csm:blocks/streetscape/markers/sheeting_" + colour}
    return _device_model(els, textures, "CSM flexible marker post")


def device_blockstate(name):
    return {"forge_marker": 1,
            "defaults": {"model": "csm:streetscape/markers/" + name},
            "variants": {"facing": _facing_variants(), "normal": [{}], "inventory": [{}]}}


def device_bbox(model):
    xs, ys, zs = [], [], []
    for e in model["elements"]:
        xs += [e["from"][0], e["to"][0]]
        ys += [e["from"][1], e["to"][1]]
        zs += [e["from"][2], e["to"][2]]
    def widen(lo, hi, least=0.25):
        # a plate a quarter of a unit thick is too thin to click or to bump into
        if hi - lo >= least:
            return lo, hi
        mid = (lo + hi) / 2
        return mid - least / 2, mid + least / 2
    x0, x1 = widen(min(xs) / 16, max(xs) / 16)
    z0, z1 = widen(min(zs) / 16, max(zs) / 16)
    return (x0, 0.0, z0, x1, max(ys) / 16, z1)


# ============================================================================================
# Writing
# ============================================================================================

def _dump_json(path, body):
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(body, fh, indent=2, ensure_ascii=False)
        fh.write("\n")


def everything():
    """{(kind, filename): payload} for every file the generator owns."""
    files = {}
    sheet, recs = glyph_sheet()
    files[("sign_tex", GLYPHS + ".png")] = sheet
    for p in PLATES:
        files[("sign_tex", "mile_marker_%s.png" % p["key"])] = mile_texture(p)
        for name, body in mile_models(p).items():
            files[("sign_model", name + ".json")] = body
        files[("state", p["registry"] + ".json")] = mile_blockstate(p)
    for name, img in marker_textures().items():
        files[("mark_tex", name + ".png")] = img
    for entry in OBJECT_MARKERS:
        files[("mark_model", entry[0] + ".json")] = om_model(entry)
        files[("state", entry[0] + ".json")] = device_blockstate(entry[0])
    for name, kind, colour, double in DELINEATORS:
        files[("mark_model", name + ".json")] = delineator_model(name, kind, colour, double)
        files[("state", name + ".json")] = device_blockstate(name)
    files[("java", os.path.basename(JAVA))] = java_source(recs)
    return files


ROOTS = {"sign_tex": SIGN_TEX, "sign_model": SIGN_MODELS, "mark_tex": MARK_TEX,
         "mark_model": MARK_MODELS, "state": STATES, "java": os.path.dirname(JAVA)}


def write(files, roots):
    for (kind, fname), payload in files.items():
        os.makedirs(roots[kind], exist_ok=True)
        path = os.path.join(roots[kind], fname)
        if isinstance(payload, Image.Image):
            payload.save(path)
        elif isinstance(payload, str):
            with open(path, "w", encoding="utf-8", newline="\n") as fh:
                fh.write(payload)
        else:
            _dump_json(path, payload)


def lang_lines():
    """{locale: [(key, value)]} in catalogue order."""
    out = {loc: [] for loc in LOCALES}
    for p in PLATES:
        for loc, text in zip(LOCALES, MILE_NAMES[p["key"]]):
            out[loc].append(("tile.%s.name" % p["registry"], text))
    for entry in OBJECT_MARKERS:
        for loc, text in zip(LOCALES, OM_NAMES[entry[0]]):
            out[loc].append(("tile.%s.name" % entry[0], text))
    for name, kind, colour, double in DELINEATORS:
        for loc, text in zip(LOCALES, delineator_names(name, kind, colour, double)):
            out[loc].append(("tile.%s.name" % name, text))
    return out


# Each group of lang lines goes after an existing key, so the files stay grouped by subject.
LANG_ANCHORS = (("mile_marker_sign_", "tile.dynamic_route_marker_sign.name"),
                ("object_marker_", "tile.signyieldahead.name"),
                ("end_of_road_marker_", "tile.signyieldahead.name"),
                ("delineator_uchannel_", "tile.delineator_zebra.name"),
                ("delineator_flexible_", "tile.delineator_zebra.name"))


def apply_lang(check=False):
    """Inserts the lang lines missing from each language file (CRLF kept). Under ``check``
    writes nothing and returns the keys that are missing or say something else."""
    problems = []
    for loc, pairs in lang_lines().items():
        path = os.path.join(LANG_DIR, loc + ".lang")
        with open(path, "rb") as fh:
            raw = fh.read().decode("utf-8")
        eol = "\r\n" if "\r\n" in raw else "\n"
        lines = raw.split(eol)
        have = {ln.split("=", 1)[0]: ln.split("=", 1)[1] for ln in lines if "=" in ln}
        changed = False
        # insert group by group, in catalogue order, after the group's anchor (or the last
        # line of that group already present)
        for key, value in pairs:
            if key in have:
                if have[key] != value:
                    if check:
                        problems.append("%s: %s says %r" % (loc, key, have[key]))
                    else:
                        lines = [key + "=" + value if ln.startswith(key + "=") else ln
                                 for ln in lines]
                        changed = True
                continue
            if check:
                problems.append("%s: %s missing" % (loc, key))
                continue
            prefix = next(a for a in LANG_ANCHORS if key.startswith("tile." + a[0]))
            idx = None
            for i, ln in enumerate(lines):
                if ln.startswith(prefix[1] + "="):
                    idx = i
                if ln.startswith("tile." + prefix[0]):
                    idx = i
            lines.insert(idx + 1, key + "=" + value)
            have[key] = value
            changed = True
        if changed:
            with open(path, "wb") as fh:
                fh.write(eol.join(lines).encode("utf-8"))
    return problems


def fragments():
    print("// Road Signs tab, guide group, after the route marker:")
    for p in PLATES:
        print("    initTabBlock(new BlockMileMarkerSign(MileMarkerLayout.%s));" % p["key"].upper())
    print("\n// Road Signs tab, end of the warning group:")
    for entry in OBJECT_MARKERS:
        bb = device_bbox(om_model(entry))
        print('    initTabBlock(new BlockWorkZoneDeviceDiagonal("%s",\n'
              '        new AxisAlignedBB(%.6f, %.6f, %.6f, %.6f, %.6f, %.6f)));' % ((entry[0],) + bb))
    print("\n// Streetscape tab, after the delineators:")
    for name, kind, colour, double in DELINEATORS:
        bb = device_bbox(delineator_model(name, kind, colour, double))
        print('    initTabBlock(new BlockWorkZoneDeviceDiagonal("%s",\n'
              '        new AxisAlignedBB(%.6f, %.6f, %.6f, %.6f, %.6f, %.6f)));' % ((name,) + bb))


def sheet(out):
    files = everything()
    imgs = [(k[1], v) for k, v in files.items() if isinstance(v, Image.Image)
            and not k[1].endswith("_back.png")]
    cell = 140
    cols = 8
    rows = (len(imgs) + cols - 1) // cols
    canvas = Image.new("RGBA", (cols * cell, rows * (cell + 14)), (70, 72, 78, 255))
    d = ImageDraw.Draw(canvas)
    for i, (name, img) in enumerate(imgs):
        x, y = (i % cols) * cell, (i // cols) * (cell + 14)
        im = img.copy()
        im.thumbnail((cell - 8, cell - 8), Image.NEAREST)
        canvas.alpha_composite(im, (x + 4, y + 4))
        d.text((x + 4, y + cell - 2), name[:22], fill=(230, 230, 230, 255))
    canvas.save(out)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    ap.add_argument("--sheet")
    args = ap.parse_args()
    if args.fragments:
        fragments()
        return 0
    if args.sheet:
        sheet(args.sheet)
        return 0
    files = everything()
    if not args.check:
        write(files, ROOTS)
        if args.apply:
            apply_lang()
        print("Wrote %d mile marker and post marker files" % len(files))
        return 0
    tmp = tempfile.mkdtemp(prefix="csm_road_markers_")
    try:
        roots = {k: os.path.join(tmp, k) for k in ROOTS}
        write(files, roots)
        drift = []
        for kind, fname in files:
            here = os.path.join(ROOTS[kind], fname)
            there = os.path.join(roots[kind], fname)
            if not os.path.exists(here):
                drift.append(os.path.relpath(here, REPO))
            elif fname.endswith(".png"):
                if not filecmp.cmp(here, there, shallow=False):
                    drift.append(os.path.relpath(here, REPO))
            else:
                # text: a checkout may have turned the generator's LF into CRLF
                with open(here, "rb") as a, open(there, "rb") as b:
                    if a.read().replace(b"\r\n", b"\n") != b.read().replace(b"\r\n", b"\n"):
                        drift.append(os.path.relpath(here, REPO))
        drift += apply_lang(check=True)
        if drift:
            print("DRIFT: %d difference(s) from the generator:" % len(drift))
            for line in drift:
                print("  " + line)
            return 1
        print("%d generated mile marker and post marker files are up to date" % len(files))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
