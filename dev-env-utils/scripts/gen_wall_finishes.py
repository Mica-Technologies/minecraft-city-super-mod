#!/usr/bin/env python3
"""Every asset the wall finishes and corner guards ship.

    python dev-env-utils/scripts/gen_wall_finishes.py
    python dev-env-utils/scripts/gen_wall_finishes.py --check
    python dev-env-utils/scripts/gen_wall_finishes.py --fragments   # lang and tab lines

Wall finishes (BlockWallFinish, one class constructed by registry name) are thin panels hung on
the face of any wall, as the blinds hang against a window: painted drywall in six colours, ceramic
wall tile, fabric acoustic panels, beadboard and a wood slat wall. Each is drawn here with the wall
to the NORTH (the finish on the wall's south face, its front facing south) and turned by the
blockstate to the way it faces.

Finishes of one kind on the same wall join into one surface, and trim is drawn only where it
belongs: a tile's bullnose cap and a beadboard's chair rail along the top course of the run
(up = false), an edge trim or an acoustic panel's frame at the run's ends (left / right = false,
seen from the room, facing the wall). Paint needs none. In an inside corner the corner cell also
draws the other wall's run on its side (corner_left / corner_right), so a run can turn the corner;
no two faces in any state share a plane (see the note above _but).

Corner guards (BlockCornerGuard, by name) are an angle on the outside corner of a wall: one flange
on the wall face the guard hangs on, the other round the corner on the wall's end face -- which is
outside the guard's own cell, so it reaches into the neighbouring one.
"""

import argparse
import os
import random
import sys

from PIL import Image

import gen_cmu
import gen_logistics
import gen_scaffold as sc

REPO = sc.REPO
MODULE = sc.MODULE
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "wallfinish")
MODEL_DIR = os.path.join(MODULE, "models", "block", "wallfinish")
STATE_DIR = sc.STATE_DIR
TEX_REF = "csm:blocks/wallfinish/%s"
MODEL_REF = "csm:wallfinish/%s"

# registry name -> (kind, colour, name in each language). Order is creative order.
FINISHES = {
    "wall_paint_white": ("paint", (238, 238, 234), (
        "Painted Drywall (White)", "Pladur Pintado (Blanco)", "Gestrichene Trockenbauwand (Weiß)",
        "Målad Gipsvägg (Vit)")),
    "wall_paint_offwhite": ("paint", (234, 228, 212), (
        "Painted Drywall (Off-White)", "Pladur Pintado (Blanco Roto)",
        "Gestrichene Trockenbauwand (Altweiß)", "Målad Gipsvägg (Bruten Vit)")),
    "wall_paint_greige": ("paint", (196, 188, 174), (
        "Painted Drywall (Greige)", "Pladur Pintado (Greige)",
        "Gestrichene Trockenbauwand (Greige)", "Målad Gipsvägg (Greige)")),
    "wall_paint_grey": ("paint", (186, 188, 190), (
        "Painted Drywall (Light Grey)", "Pladur Pintado (Gris Claro)",
        "Gestrichene Trockenbauwand (Hellgrau)", "Målad Gipsvägg (Ljusgrå)")),
    "wall_paint_blue": ("paint", (182, 200, 214), (
        "Painted Drywall (Pale Blue)", "Pladur Pintado (Azul Pálido)",
        "Gestrichene Trockenbauwand (Hellblau)", "Målad Gipsvägg (Ljusblå)")),
    "wall_paint_sage": ("paint", (170, 184, 160), (
        "Painted Drywall (Sage)", "Pladur Pintado (Salvia)", "Gestrichene Trockenbauwand (Salbei)",
        "Målad Gipsvägg (Salvia)")),
    "wall_tile_subway_white": ("subway", (236, 236, 232), (
        "Ceramic Wall Tile (White Subway)", "Azulejo Cerámico (Metro Blanco)",
        "Keramische Wandfliese (Metro, Weiß)", "Kakel (Vit Tunnelbana)")),
    "wall_tile_subway_green": ("subway", (74, 120, 100), (
        "Ceramic Wall Tile (Green Subway)", "Azulejo Cerámico (Metro Verde)",
        "Keramische Wandfliese (Metro, Grün)", "Kakel (Grön Tunnelbana)")),
    "wall_tile_square_white": ("square", (234, 234, 230), (
        "Ceramic Wall Tile (White Square)", "Azulejo Cerámico (Cuadrado Blanco)",
        "Keramische Wandfliese (Quadrat, Weiß)", "Kakel (Vit Kvadrat)")),
    "wall_acoustic_grey": ("acoustic", (138, 138, 140), (
        "Acoustic Wall Panel (Grey)", "Panel Acústico (Gris)", "Akustikpaneel (Grau)",
        "Akustikpanel (Grå)")),
    "wall_acoustic_blue": ("acoustic", (72, 94, 130), (
        "Acoustic Wall Panel (Blue)", "Panel Acústico (Azul)", "Akustikpaneel (Blau)",
        "Akustikpanel (Blå)")),
    "wall_acoustic_charcoal": ("acoustic", (58, 58, 62), (
        "Acoustic Wall Panel (Charcoal)", "Panel Acústico (Carbón)", "Akustikpaneel (Anthrazit)",
        "Akustikpanel (Koksgrå)")),
    "wall_beadboard_white": ("beadboard", (238, 238, 232), (
        "Beadboard (White)", "Friso Machihembrado (Blanco)", "Profilholz-Täfelung (Weiß)",
        "Pärlspont (Vit)")),
    "wall_slatwall_oak": ("slatwall", (182, 138, 88), (
        "Wood Slat Wall (Oak)", "Pared de Listones (Roble)", "Lamellenwand (Eiche)",
        "Ribbvägg (Ek)")),
}

GUARDS = {
    "corner_guard_steel": ((190, 194, 198), (
        "Corner Guard (Stainless)", "Guardavivos (Acero Inoxidable)",
        "Kantenschutz (Edelstahl)", "Hörnskydd (Rostfritt)")),
    "corner_guard_white": ((236, 236, 232), (
        "Corner Guard (White Vinyl)", "Guardavivos (Vinilo Blanco)", "Kantenschutz (Weißes Vinyl)",
        "Hörnskydd (Vit Vinyl)")),
}

# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

_shift = gen_logistics._shift


def _seed(name):
    return sum(ord(c) * (i + 3) for i, c in enumerate(name))


def _flat(rgb, rng, amount):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(rgb, rng.uniform(-amount, amount))
    return img, px


def paint(rgb, rng):
    """Painted drywall: an eggshell finish, the faintest orange-peel."""
    return _flat(rgb, rng, 2.5)[0]


def _grout(rgb):
    return (196, 196, 190) if sum(rgb) > 500 else (206, 206, 200)


def subway(rgb, rng):
    """Subway tile: 3 x 6 in bricks, 8 x 4 px, laid running bond, with grout lines and a lit top
    edge on each tile. The bond repeats every block, so a wall of it has no seam."""
    img, px = _flat(rgb, rng, 2)
    g = _grout(rgb)
    for y in range(16):
        course = y // 4
        off = 4 if course % 2 else 0
        for x in range(16):
            if y % 4 == 3 or (x + off) % 8 == 7:
                px[x, y] = _shift(g, rng.uniform(-2, 2))
            elif y % 4 == 0:
                px[x, y] = _shift(rgb, 10)
    return img


def square(rgb, rng):
    """4 x 4 in square tile: four across a block, a grout line on two edges of each."""
    img, px = _flat(rgb, rng, 2)
    g = _grout(rgb)
    for y in range(16):
        for x in range(16):
            if x % 4 == 3 or y % 4 == 3:
                px[x, y] = _shift(g, rng.uniform(-2, 2))
            elif x % 4 == 0 or y % 4 == 0:
                px[x, y] = _shift(rgb, 8)
    return img


def cap(rgb, rng):
    """A bullnose cap or edge trim: the tile colour, lit along its rounded face."""
    img, px = _flat(rgb, rng, 2)
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(rgb, 12 - abs(x - 5) * 3 - abs(y - 5) * 0)
    return img


def acoustic(rgb, rng):
    """Fabric over a fibreglass core: an even weave, no pattern to repeat."""
    img, px = _flat(rgb, rng, 5)
    for y in range(0, 16, 2):
        for x in range(0, 16, 2):
            px[x, y] = _shift(rgb, -6 + rng.uniform(-2, 2))
    return img


def frame(rgb, rng):
    """An acoustic panel's frame, where the fabric wraps its edge: a shade darker."""
    return _flat(_shift(rgb, -22)[:3], rng, 3)[0]


def beadboard(rgb, rng):
    """Beadboard: vertical boards 4 px wide, each with a bead -- a groove shadowed on one side and
    lit on the other."""
    img, px = _flat(rgb, rng, 2)
    for x in range(0, 16, 4):
        for y in range(16):
            px[x, y] = _shift(rgb, -34)
            px[(x + 1) % 16, y] = _shift(rgb, 10)
    return img


def felt(rgb, rng):
    """The black acoustic felt behind a slat wall."""
    return _flat((34, 34, 36), rng, 3)[0]


def slat(rgb, rng):
    """Oak slat: grain running up the slat."""
    img, px = _flat(rgb, rng, 3)
    for x in range(16):
        tone = rng.uniform(-10, 10)
        for y in range(16):
            px[x, y] = _shift(rgb, tone + rng.uniform(-3, 3))
    return img


def guard(rgb, rng):
    """A corner guard: brushed stainless streaked up its length, or plain white vinyl."""
    img, px = _flat(rgb, rng, 2)
    if sum(rgb) < 650:
        for x in range(16):
            tone = rng.uniform(-8, 8)
            for y in range(16):
                px[x, y] = _shift(rgb, tone + rng.uniform(-2, 2))
    return img


DRAW = {"paint": paint, "subway": subway, "square": square, "acoustic": acoustic,
        "beadboard": beadboard, "slatwall": slat}


def textures():
    out = {}
    for name, (kind, rgb, _) in FINISHES.items():
        rng = random.Random(_seed(name))
        out[name] = DRAW[kind](rgb, rng)
        if kind in ("subway", "square", "beadboard"):
            out[name + "_cap"] = cap(rgb, random.Random(_seed(name) + 1))
        elif kind == "acoustic":
            out[name + "_frame"] = frame(rgb, random.Random(_seed(name) + 1))
        elif kind == "slatwall":
            out["wall_slatwall_felt"] = felt(rgb, random.Random(_seed(name) + 1))
    for name, (rgb, _) in GUARDS.items():
        out[name] = guard(rgb, random.Random(_seed(name)))
    return out

# --------------------------------------------------------------------------------------------
# Models -- the wall to the north, the finish on its south face
# --------------------------------------------------------------------------------------------

box = sc._box
mirror_x = sc._mirror_x

# How far each kind stands off the wall, in px.
DEPTH = {"paint": 0.25, "subway": 1.0, "square": 1.0, "acoustic": 1.5, "beadboard": 1.0,
         "slatwall": 0.5}

# A trimmed kind's trim, in px: (edge width, edge depth, cap height, cap depth). The cap is the top
# course's trim -- a bullnose cap on tile, a chair rail on beadboard, a top rail on a slat wall --
# and the edge the trim at an end of the run.
TRIM = {"subway": (0.75, 1.75, 1.0, 1.75), "square": (0.75, 1.75, 1.0, 1.75),
        "beadboard": (0.75, 1.75, 1.5, 2.0), "slatwall": (1.0, 1.75, 1.0, 1.75)}
TRIM_TEX = {"subway": "#cap", "square": "#cap", "beadboard": "#cap", "slatwall": "#slat"}

# An acoustic panel's frame: its width, and the lip it stands on the fabric's face (z from, to).
FRAME_W = 0.5
FRAME_Z = (1.5, 1.6)

# Where a slat wall's slats stand along the run (x), and how far proud of the felt (z).
SLATS = (1, 5, 9, 13)
SLAT_Z = (0.5, 1.5)

ALL_FACES = ("north", "south", "east", "west", "up", "down")

# No two faces, in any state, may lie in one plane facing the same way over the same area: the
# depth test cannot choose between them and the two textures flicker through each other (#218 --
# at the end of every slat wall run the felt's end face and the edge trim's were one plane). So a
# face some part always covers is left off, and a part that would overlap another only in some
# states is cut, and each piece drawn only in the states it shows in:
#
# - On a trimmed kind the panel has no end or top faces: at an end of the run the edge trim covers
#   them, at a join the next panel does, and on top the cap or the course above.
# - The strip of panel an edge trim stands over is its own part, drawn only where the run goes on
#   (left / right = true); under the trim, its bottom face and the trim's were one plane.
# - The top course's trim runs the full width and owns the corner, so an edge trim stops under it
#   and its top piece is drawn only where the run goes on up. An acoustic panel's frame is split
#   the same way round its four corners.
# - The cap has no back face: the panel's and the edge trims' already cover the wall behind it,
#   and hung on glass the two were seen overlapping from the far side.
#
# An inside corner: the cell in the corner hangs on one wall, and when the run on the other wall
# reaches it (corner_left / corner_right), it draws that run's last stretch on its own side too,
# butted against the face of its own panel rather than through it, so the two runs meet without a
# face in common. Both runs count the corner as a join (left / right = true), so neither draws an
# edge trim there.


def _but(*faces):
    return tuple(f for f in ALL_FACES if f not in faces)


def _mirror(elements):
    """The right-hand twin of a left-hand part. An element that leaves off one of its end faces
    leaves off the other end once it is mirrored, so the two swap names, and take their UVs again
    from where they now lie."""
    out = mirror_x(elements)
    for e in out:
        f = e["faces"]
        if ("east" in f) != ("west" in f):
            e["faces"] = {{"east": "west", "west": "east"}.get(k, k): v for k, v in f.items()}
            sc._reuv(e)
    return out


def _against(el):
    """The face against the wall, culled when the wall is solid."""
    el["faces"]["north"]["cullface"] = "north"
    return el


def _item(kind):
    """The whole panel with no trim: the inventory's model, and paint's and an acoustic panel's
    panel in the world, whose ends show."""
    d = DEPTH[kind]
    el = _against(box(0, 0, 0, 16, 16, d, "#face",
                      faces=("south", "north", "up", "down", "east", "west")))
    if kind != "slatwall":
        return [el]
    # A slat wall: slats standing proud of the felt.
    return [el] + [box(x, 0, SLAT_Z[0], x + 2, 16, SLAT_Z[1], "#slat") for x in SLATS]


def _slats(kind, faces):
    if kind != "slatwall":
        return []
    return [box(x, 0, SLAT_Z[0], x + 2, 16, SLAT_Z[1], "#slat", faces=faces) for x in SLATS]


def _side_slats(kind, y0, y1, faces):
    """The slats of the run on the left wall, drawn in a corner cell: a slat at x along that run
    lies at z = 16 - x here. The one nearest the corner starts in front of this cell's own slats,
    which it would otherwise pass through."""
    if kind != "slatwall":
        return []
    return [box(SLAT_Z[0], y0, max(14 - x, SLAT_Z[1]), SLAT_Z[1], y1, 16 - x, "#slat", faces=faces)
            for x in SLATS]


def _trimmed(kind):
    """A trimmed kind's parts, by model suffix; each left part's right twin is its mirror."""
    w, ed, h, cd = TRIM[kind]
    d = DEPTH[kind]
    tex = TRIM_TEX[kind]
    return {
        "_body": [_against(box(w, 0, 0, 16 - w, 16, d, "#face", faces=("south", "north", "down")))]
        + _slats(kind, _but("up")),
        "_top": [box(0, 16 - h, 0, 16, 16, cd, tex, faces=_but("north"))],
        "_join_left": [_against(box(0, 0, 0, w, 16, d, "#face",
                                    faces=("south", "north", "down")))],
        "_left": [_against(box(0, 0, 0, w, 16 - h, ed, tex, faces=_but("up")))],
        "_left_upper": [_against(box(0, 16 - h, 0, w, 16, ed, tex, faces=_but("down")))],
        "_corner_left": [box(0, 0, d, d, 16 - h, 16, "#face", faces=("east", "down"))]
        + _side_slats(kind, 0, 16 - h, _but("up")),
        "_corner_left_upper": [box(0, 16 - h, d, d, 16, 16, "#face", faces=("east", "up"))]
        + _side_slats(kind, 16 - h, 16, _but("down")),
        "_corner_left_top": [box(0, 16 - h, cd, cd, 16, 16, tex,
                                 faces=("east", "up", "down", "south"))],
    }


def _acoustic():
    """An acoustic panel's frame, round the outside of the whole panel, by model suffix."""
    d = DEPTH["acoustic"]
    fw = FRAME_W
    z0, z1 = FRAME_Z
    lip = _but("north")
    return {
        "_top": [box(d, 16 - fw, z0, 16 - d, 16, z1, "#frame", faces=lip)],
        "_bottom": [box(d, 0, z0, 16 - d, fw, z1, "#frame", faces=lip)],
        "_top_left": [box(0, 16 - fw, z0, d, 16, z1, "#frame", faces=lip)],
        "_bottom_left": [box(0, 0, z0, d, fw, z1, "#frame", faces=lip)],
        "_left": [box(0, fw, z0, fw, 16 - fw, z1, "#frame", faces=lip)],
        "_left_upper": [box(0, 16 - fw, z0, fw, 16, z1, "#frame", faces=lip)],
        "_left_lower": [box(0, 0, z0, fw, fw, z1, "#frame", faces=lip)],
        "_corner_left": [box(0, 0, d, d, 16, 16, "#face", faces=("east", "up", "down"))],
        "_corner_left_top": [box(d, 16 - fw, z1, z1, 16, 16, "#frame", faces=_but("west"))],
        "_corner_left_bottom": [box(d, 0, z1, z1, fw, 16, "#frame", faces=_but("west"))],
    }


def _paint():
    d = DEPTH["paint"]
    return {"_corner_left": [box(0, 0, d, d, 16, 16, "#face", faces=("east", "up", "down"))]}


def _model(elements, textures):
    t = dict(textures)
    t["particle"] = next(iter(textures.values()))
    return {"textures": t, "elements": elements}


def _tex(name, kind):
    t = {"face": TEX_REF % name}
    if kind in ("subway", "square", "beadboard"):
        t["cap"] = TEX_REF % (name + "_cap")
    elif kind == "acoustic":
        t["frame"] = TEX_REF % (name + "_frame")
    elif kind == "slatwall":
        t = {"face": TEX_REF % "wall_slatwall_felt", "slat": TEX_REF % name}
    return t


def guard_model(rgb_name):
    """The left-hand guard (at x = 0): a flange on the face it hangs on and one round the corner on
    the wall's end face, which lies in the cell to the west of the wall's."""
    return _model([box(0, 0, 0, 1.5, 16, 0.75, "#guard"),
                   box(-0.75, 0, -1.5, 0, 16, 0.75, "#guard")],
                  {"guard": TEX_REF % rgb_name})


def finish_parts(kind):
    """Every part of a finish but the whole panel, by model suffix, the right-hand ones mirrored
    from the left."""
    if kind == "paint":
        parts = _paint()
    elif kind == "acoustic":
        parts = _acoustic()
    else:
        parts = _trimmed(kind)
    out = {}
    for suffix, elements in parts.items():
        out[suffix] = elements
        if "left" in suffix:
            out[suffix.replace("left", "right")] = _mirror(elements)
    return out


def models():
    out = {}
    for name, (kind, _, _) in FINISHES.items():
        t = _tex(name, kind)
        out[name] = _model(_item(kind), t)
        for suffix, elements in finish_parts(kind).items():
            out[name + suffix] = _model(elements, t)
    for name in GUARDS:
        left = guard_model(name)
        out[name + "_left"] = left
        out[name + "_right"] = _model(mirror_x(left["elements"]), {"guard": TEX_REF % name})
    return out

# --------------------------------------------------------------------------------------------
# Blockstates
# --------------------------------------------------------------------------------------------

SIDES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def _parts(conds):
    parts = []
    for when, model in conds:
        for side, rot in SIDES:
            w = dict(when)
            w["facing"] = side
            a = {"model": MODEL_REF % model}
            if rot:
                a["y"] = rot
            parts.append({"when": w, "apply": a})
    return parts


def finish_state(name):
    """Which parts each state draws. Left and right are joined (true) where the run goes on, to the
    next cell or round an inside corner; corner_left / corner_right where this cell is the corner
    and draws the other wall's run on that side."""
    kind = FINISHES[name][0]
    if kind == "paint":
        conds = [({}, name)]
        for s in ("left", "right"):
            conds.append(({"corner_" + s: "true"}, "%s_corner_%s" % (name, s)))
    elif kind == "acoustic":
        conds = [({}, name), ({"up": "false"}, name + "_top"),
                 ({"down": "false"}, name + "_bottom")]
        for s in ("left", "right"):
            c = "corner_" + s
            conds += [({"up": "false", c: "false"}, "%s_top_%s" % (name, s)),
                      ({"down": "false", c: "false"}, "%s_bottom_%s" % (name, s)),
                      ({s: "false"}, "%s_%s" % (name, s)),
                      ({s: "false", "up": "true"}, "%s_%s_upper" % (name, s)),
                      ({s: "false", "down": "true"}, "%s_%s_lower" % (name, s)),
                      ({c: "true"}, "%s_corner_%s" % (name, s)),
                      ({c: "true", "up": "false"}, "%s_corner_%s_top" % (name, s)),
                      ({c: "true", "down": "false"}, "%s_corner_%s_bottom" % (name, s))]
    else:
        conds = [({}, name + "_body"), ({"up": "false"}, name + "_top")]
        for s in ("left", "right"):
            c = "corner_" + s
            conds += [({s: "true"}, "%s_join_%s" % (name, s)),
                      ({s: "false"}, "%s_%s" % (name, s)),
                      ({s: "false", "up": "true"}, "%s_%s_upper" % (name, s)),
                      ({c: "true"}, "%s_corner_%s" % (name, s)),
                      ({c: "true", "up": "true"}, "%s_corner_%s_upper" % (name, s)),
                      ({c: "true", "up": "false"}, "%s_corner_%s_top" % (name, s))]
    return {"variants": {"inventory": {"model": MODEL_REF % name}}, "multipart": _parts(conds)}


def guard_state(name):
    variants = {"inventory": {"model": MODEL_REF % (name + "_left")}}
    for side, rot in SIDES:
        for edge in ("left", "right"):
            v = {"model": MODEL_REF % (name + "_" + edge)}
            if rot:
                v["y"] = rot
            variants["edge=%s,facing=%s" % (edge, side)] = v
    return {"variants": variants}


def blockstates():
    out = {name: finish_state(name) for name in FINISHES}
    out.update({name: guard_state(name) for name in GUARDS})
    return out

# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------


def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    for name, img in sorted(textures().items()):
        img.save(os.path.join(tex_dir, name + ".png"))
        written.append(("tex", name + ".png"))
    for name, body in sorted(models().items()):
        gen_cmu._write_json(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    for name, body in sorted(blockstates().items()):
        gen_cmu._write_json(os.path.join(state_dir, name + ".json"), body)
        written.append(("state", name + ".json"))
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    out = [("tile.%s.name" % name, dict(zip(LANGS, names)))
           for name, (_, _, names) in FINISHES.items()]
    out += [("tile.%s.name" % name, dict(zip(LANGS, names))) for name, (_, names) in GUARDS.items()]
    return out


def tab_lines():
    lines = ['    initTabBlock(new BlockWallFinish("%s")); // %s' % (n, v[2][0])
             for n, v in FINISHES.items()]
    lines += ['    initTabBlock(new BlockCornerGuard("%s")); // %s' % (n, v[1][0])
              for n, v in GUARDS.items()]
    return lines


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    lines.append("# tab registration, in CsmTabInteriorFinishes")
    lines += tab_lines()
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    ap.add_argument("--fragments", action="store_true",
                    help="print the lang and tab-registration lines and exit")
    args = ap.parse_args()
    if args.fragments:
        print(fragments())
        return 0
    roots = {"tex": TEX_DIR, "model": MODEL_DIR, "state": STATE_DIR}
    if args.check:
        return gen_logistics.check("wall finish", write_all, roots)
    written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR)
    print("Wrote %d wall finish files" % len(written))
    return 0


if __name__ == "__main__":
    sys.exit(main())
