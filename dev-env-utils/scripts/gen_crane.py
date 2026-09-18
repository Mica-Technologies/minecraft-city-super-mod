#!/usr/bin/env python3
"""Every asset the tower crane's mast ships: textures, part models and blockstates.

    python dev-env-utils/scripts/gen_crane.py
    python dev-env-utils/scripts/gen_crane.py --check
    python dev-env-utils/scripts/gen_crane.py --fragments   # lang lines to paste

THE MAST. Four corner chords, the full height of the cell so stacked sections are one mast, and
the lacing between them on each face. The chords are geometry; the lacing is not. A face's
diagonals run from chord to chord, and at 1x1 that is not 45 degrees -- the only angle an element
can be turned to that would reach -- so a modelled diagonal either stops short of the chords or
runs past them at every block. Drawn instead into a cutout texture on a plane between the chords,
a diagonal can be any angle and lands on the chord exactly at the cell's edge, so the lacing is
continuous up the whole mast. From any distance it reads as lattice, and it is two quads a face
on a mast that may be two hundred and fifty blocks tall.

THE 2x2 MAST. Each block is one quarter of a 2x2 section and works out which quarter from its
neighbours, so nothing is stored but the livery. Only the north-west quarter is drawn; the
blockstate turns it for the other three. A 2x2 face is two blocks wide, so its X spans a panel
two blocks high: each block draws its half across and its half up, the half up chosen by whether
its y is odd or even, so stacked sections stay in step wherever the mast starts.

A ladder runs up the inside of the north face, as on the user's first reference, and a heavier
base section -- wider chords and anchor plates -- is drawn where the mast stands on something
that is not mast.

LIVERIES. Yellow, red and white, the colours real tower cranes wear. Every part model is written
once per livery with that livery's textures; the blockstate picks by the stored livery.

The element helpers are gen_scaffold's, so every face carries fitted UVs (see _fit there).
"""

import argparse
import filecmp
import math
import os
import random
import shutil
import sys
import tempfile

from PIL import Image, ImageDraw

import gen_cmu
import gen_scaffold as sc

REPO = sc.REPO
TEX_DIR = sc.TEX_DIR
MODEL_DIR = sc.MODEL_DIR
STATE_DIR = sc.STATE_DIR
TEX_REF = sc.TEX_REF
MODEL_REF = sc.MODEL_REF

NAME = "crane_mast"
LARGE = "crane_mast_large"

# livery -> (paint colour, name in each language)
LIVERIES = {
    "yellow": ((232, 178, 28), ("Yellow", "Amarillo", "Gelb", "Gul")),
    "red": ((186, 38, 34), ("Red", "Rojo", "Rot", "Röd")),
    "white": ((226, 226, 220), ("White", "Blanco", "Weiß", "Vit")),
}
LIVERY_ORDER = ["yellow", "red", "white"]
LANG = ("Tower Crane Mast", "Mástil de Grúa Torre", "Turmdrehkran-Mast", "Tornkranmast")
HEAD = "crane_head"
HEAD_LANG = ("Tower Crane Head", "Cabeza de Grúa Torre", "Turmdrehkran-Oberteil",
             "Tornkranstopp")
LARGE_LANG = ("Tower Crane Mast (2x2)", "Mástil de Grúa Torre (2x2)", "Turmdrehkran-Mast (2x2)",
              "Tornkranmast (2x2)")

# --------------------------------------------------------------------------------------------
# Geometry catalogue, in sixteenths
# --------------------------------------------------------------------------------------------

CHORD = (0.5, 2.0)        # a chord's near and far edge from the cell's corner
BASE_CHORD = (0.25, 2.5)  # the base section's heavier chord
FACE_INSET = 1.25         # the lacing plane sits mid-chord, this far in from the face
LADDER_RAILS = (6.0, 10.0)
LADDER_Z = 2.25           # inside the north face's lacing
RUNG_PITCH = 4.0

LARGE_CHORD = (0.5, 3.0)  # a 2x2 section's chord, at its outer corner only
LARGE_BASE_CHORD = (0.25, 3.5)

LACE_SIZE = 32            # lacing texture resolution
LACE_W = 2                # lacing member width in texture pixels


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

def _shift(colour, d):
    return tuple(max(0, min(255, int(round(c + d)))) for c in colour[:3]) + (255,)


def chord_texture(livery):
    """Painted steel, banded lengthwise so a chord reads as a tube."""
    colour = LIVERIES[livery][0]
    rng = random.Random(20261060 + LIVERY_ORDER.index(livery))
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    band = [10, 6, 2, -4, -10, -6, 0, 6] * 2
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(colour, band[x] + rng.uniform(-3, 3))
    return img


def lacing_texture(livery):
    """One face's lacing: a strut along the bottom and an X of diagonals from chord to chord, on
    a clear ground, drawn for the cutout layer. The diagonals meet the cell's corners exactly, so
    stacked faces join into one continuous lattice. The texture is mapped onto the face with the
    UVs of its position, so a pixel's column is a position across the face."""
    colour = LIVERIES[livery][0]
    s = LACE_SIZE
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    fill = _shift(colour, -8)
    edge = _shift(colour, -30)
    # Strut along the bottom of the panel.
    draw.rectangle([0, s - LACE_W, s - 1, s - 1], fill=fill)
    # The X, corner to corner, drawn as two lines of member width.
    for x0, y0, x1, y1 in ((0, s - 1, s - 1, 0), (0, 0, s - 1, s - 1)):
        draw.line([(x0, y0), (x1, y1)], fill=fill, width=LACE_W)
    # A darker edge on the lower side of each member, lit from above like everything else.
    px = img.load()
    for y in range(s - 1, 0, -1):
        for x in range(s):
            if px[x, y][3] == 0 and px[x, y - 1][3] != 0 and px[x, y - 1][:3] == fill[:3]:
                px[x, y] = edge
    return img


# --------------------------------------------------------------------------------------------
# Models
# --------------------------------------------------------------------------------------------

box = sc._box


GLOW_SIZE = 64            # the aviation light's halo texture


def glow_texture():
    """The halo drawn around an aviation light while it is lit: white, so the renderer tints it,
    with alpha falling off from a hot centre. A tight core on a long soft tail, rather than one
    gaussian, is what makes a small quad read as light spilling out of a lamp; the tail reaches
    zero before the edge so the quad's square outline never shows."""
    n = GLOW_SIZE
    img = Image.new("RGBA", (n, n), (255, 255, 255, 0))
    px = img.load()
    c = (n - 1) / 2.0
    for y in range(n):
        for x in range(n):
            r = math.hypot(x - c, y - c) / (n / 2.0)
            if r >= 1.0:
                continue
            core = math.exp(-(r / 0.15) ** 2)
            tail = (1.0 - r) ** 1.8
            a = min(1.0, 0.8 * core + 0.85 * tail)
            px[x, y] = (255, 255, 255, int(round(a * 255)))
    return img


def _chords(c0, c1):
    """Four corner chords the full height of the cell."""
    out = []
    for x0, x1 in ((c0, c1), (16 - c1, 16 - c0)):
        for z0, z1 in ((c0, c1), (16 - c1, 16 - c0)):
            out.append(box(x0, 0, z0, x1, 16, z1, "#chord"))
    return out


def lacing():
    """The lacing planes, one per face, between the chords at mid-chord depth. Each plane is
    drawn both ways so it reads from inside the mast as well as out."""
    a, b = CHORD[1], 16 - CHORD[1]
    i, o = FACE_INSET, 16 - FACE_INSET
    # The whole lacing texture on the plane, not the window its position would give: the plane
    # spans only the gap between the chords, and a position window would crop the X off before
    # its diagonals reached them.
    uv = [0, 0, 16, 16]
    return [
        box(a, 0, i, b, 16, i, "#lace", faces=("north", "south"), uv=uv),
        box(a, 0, o, b, 16, o, "#lace", faces=("north", "south"), uv=uv),
        box(i, 0, a, i, 16, b, "#lace", faces=("east", "west"), uv=uv),
        box(o, 0, a, o, 16, b, "#lace", faces=("east", "west"), uv=uv),
    ]


def ladder():
    """A ladder up the inside of the north face: two rails the full height, rungs at a pitch that
    divides the cell so stacked ladders are one."""
    t = "#ladder"
    z0, z1 = LADDER_Z, LADDER_Z + 0.5
    r0, r1 = LADDER_RAILS
    out = [box(r0 - 0.5, 0, z0, r0, 16, z1, t), box(r1, 0, z0, r1 + 0.5, 16, z1, t)]
    y = 1.0
    while y < 16:
        out.append(box(r0, y, z0 + 0.1, r1, y + 0.4, z1 - 0.1, t))
        y += RUNG_PITCH
    return out


def base():
    """The base section, where the mast stands on the ground: heavier chords over the full
    height, and an anchor plate under each."""
    out = _chords(*BASE_CHORD)
    for x0, x1 in ((0, 3), (13, 16)):
        for z0, z1 in ((0, 3), (13, 16)):
            out.append(box(x0, 0, z0, x1, 1, z1, "#anchor"))
    return out


def large_chord():
    """The north-west quarter's chord, at the section's outer corner."""
    c0, c1 = LARGE_CHORD
    return [box(c0, 0, c0, c1, 16, c1, "#chord")]


def large_base():
    """The north-west quarter's base: a heavier chord on an anchor plate."""
    c0, c1 = LARGE_BASE_CHORD
    return [box(c0, 0, c0, c1, 16, c1, "#chord"), box(0, 0, 0, 4, 1, 4, "#anchor")]


def large_lacing(upper):
    """The north-west quarter's share of its two outer faces' lacing: from its chord to the cell
    edge, where the next quarter's share carries on. Each face's X spans the whole two-block face
    and two blocks up, so this quarter shows half of it across and, by ``upper``, half of it up.
    The two faces take opposite halves across, which is what makes each face whole once the
    quarter is turned into the other three corners."""
    c = LARGE_CHORD[1]
    i = (LARGE_CHORD[0] + LARGE_CHORD[1]) / 2
    v0, v1 = (0, 8) if upper else (8, 16)
    return [
        box(c, 0, i, 16, 16, i, "#lace", faces=("north", "south"), uv=[0, v0, 8, v1]),
        box(i, 0, c, i, 16, 16, "#lace", faces=("east", "west"), uv=[8, v0, 16, v1]),
    ]


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


# The item icon of a 2x2 section is twice the size of a block, so every view is scaled by half.
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


def _textures(livery):
    return {"chord": TEX_REF % ("crane_chord_" + livery),
            "lace": TEX_REF % ("crane_lace_" + livery),
            "ladder": TEX_REF % "scaffold_tube",
            "anchor": TEX_REF % "scaffold_jack",
            "particle": TEX_REF % ("crane_chord_" + livery)}


def _model(elements, livery, parent=None):
    m = {"textures": _textures(livery), "elements": elements}
    if parent:
        m = {"parent": parent, **m}
    return m


def part_models():
    out = {}
    for liv in LIVERY_ORDER:
        out["%s_%s_chords" % (NAME, liv)] = _model(_chords(*CHORD), liv)
        out["%s_%s_lacing" % (NAME, liv)] = _model(lacing(), liv)
        out["%s_%s_base" % (NAME, liv)] = _model(base(), liv)
        out["%s_%s_inventory" % (NAME, liv)] = _model(
            _chords(*CHORD) + lacing() + ladder(), liv, parent="block/block")
    for liv in LIVERY_ORDER:
        out["%s_%s_chord" % (LARGE, liv)] = _model(large_chord(), liv)
        out["%s_%s_base" % (LARGE, liv)] = _model(large_base(), liv)
        out["%s_%s_lacing_lower" % (LARGE, liv)] = _model(large_lacing(False), liv)
        out["%s_%s_lacing_upper" % (LARGE, liv)] = _model(large_lacing(True), liv)
        inv = _model(large_inventory(), liv, parent="block/block")
        inv["display"] = LARGE_DISPLAY
        out["%s_%s_inventory" % (LARGE, liv)] = inv
    # The head block draws nothing in the world -- its renderer draws the slewing unit -- so this
    # model is only the item icon: the slewing ring under its platform.
    out[HEAD] = _model([box(2, 0, 2, 14, 11, 14, "#anchor"),
                        box(0, 11, 0, 16, 16, 16, "#chord")], "yellow", parent="block/block")
    out[NAME + "_ladder"] = {"textures": {"ladder": TEX_REF % "scaffold_tube",
                                          "particle": TEX_REF % "scaffold_tube"},
                             "elements": ladder()}
    return out


# A quarter drawn for the north-west corner, turned into the others. y 90 carries north-west to
# north-east, 180 to south-east, 270 to south-west.
CORNERS = (("nw", None), ("ne", 90), ("se", 180), ("sw", 270))


def large_blockstate():
    parts = []
    for corner, y in CORNERS:
        def ap(model):
            a = {"model": MODEL_REF % model}
            if y is not None:
                a["y"] = y
            return a
        if corner == "nw":
            parts.append({"when": {"corner": "nw"}, "apply": ap(NAME + "_ladder")})
        for liv in LIVERY_ORDER:
            w = {"livery": liv, "corner": corner}
            parts += [
                {"when": dict(w, down="true"), "apply": ap("%s_%s_chord" % (LARGE, liv))},
                {"when": dict(w, down="false"), "apply": ap("%s_%s_base" % (LARGE, liv))},
                {"when": dict(w, half="lower"),
                 "apply": ap("%s_%s_lacing_lower" % (LARGE, liv))},
                {"when": dict(w, half="upper"),
                 "apply": ap("%s_%s_lacing_upper" % (LARGE, liv))},
            ]
    variants = {"inventory_" + liv: {"model": MODEL_REF % ("%s_%s_inventory" % (LARGE, liv))}
                for liv in LIVERY_ORDER}
    return {"variants": variants, "multipart": parts}


def blockstate():
    parts = [{"apply": {"model": MODEL_REF % (NAME + "_ladder")}}]
    for liv in LIVERY_ORDER:
        parts += [
            {"when": {"livery": liv, "down": "true"},
             "apply": {"model": MODEL_REF % ("%s_%s_chords" % (NAME, liv))}},
            {"when": {"livery": liv, "down": "false"},
             "apply": {"model": MODEL_REF % ("%s_%s_base" % (NAME, liv))}},
            {"when": {"livery": liv},
             "apply": {"model": MODEL_REF % ("%s_%s_lacing" % (NAME, liv))}},
        ]
    variants = {"inventory_" + liv: {"model": MODEL_REF % ("%s_%s_inventory" % (NAME, liv))}
                for liv in LIVERY_ORDER}
    return {"variants": variants, "multipart": parts}


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    for liv in LIVERY_ORDER:
        chord_texture(liv).save(os.path.join(tex_dir, "crane_chord_%s.png" % liv))
        lacing_texture(liv).save(os.path.join(tex_dir, "crane_lace_%s.png" % liv))
        written += [("tex", "crane_chord_%s.png" % liv), ("tex", "crane_lace_%s.png" % liv)]
    glow_texture().save(os.path.join(tex_dir, "crane_glow.png"))
    written.append(("tex", "crane_glow.png"))
    for name, body in sorted(part_models().items()):
        gen_cmu._write_json(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    gen_cmu._write_json(os.path.join(state_dir, NAME + ".json"), blockstate())
    gen_cmu._write_json(os.path.join(state_dir, LARGE + ".json"), large_blockstate())
    gen_cmu._write_json(os.path.join(state_dir, HEAD + ".json"),
                        {"variants": {"normal": {"model": MODEL_REF % HEAD},
                                      "inventory": {"model": MODEL_REF % HEAD}}})
    written += [("state", NAME + ".json"), ("state", LARGE + ".json"), ("state", HEAD + ".json")]
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    """Every mast name: the base name the guidebook and pricing look up, and one per livery."""
    out = []
    out.append(("tile.%s.name" % HEAD, dict(zip(LANGS, HEAD_LANG))))
    for name, names in ((NAME, LANG), (LARGE, LARGE_LANG)):
        base_name = dict(zip(LANGS, names))
        out.append(("tile.%s.name" % name, base_name))
        for liv in LIVERY_ORDER:
            colour = dict(zip(LANGS, LIVERIES[liv][1]))
            out.append(("tile.%s.%s.name" % (name, liv),
                        {lang: sc_join(base_name[lang], colour[lang]) for lang in LANGS}))
    return out


def sc_join(base_name, colour):
    """A livery folded into the name's own bracket if it has one: "Mast (2x2, Yellow)"."""
    if base_name.endswith(")"):
        return base_name[:-1] + ", " + colour + ")"
    return "%s (%s)" % (base_name, colour)


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    ap.add_argument("--fragments", action="store_true",
                    help="print the lang lines and exit")
    args = ap.parse_args()

    if args.fragments:
        print(fragments())
        return 0

    roots = {"tex": TEX_DIR, "model": MODEL_DIR, "state": STATE_DIR}

    if not args.check:
        written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR)
        print("Wrote %d crane files" % len(written))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_crane_")
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
        print("%d generated crane files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
