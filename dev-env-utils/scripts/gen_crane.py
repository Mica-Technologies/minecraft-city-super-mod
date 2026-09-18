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

A ladder runs up the inside of the north face, as on the user's first reference, and a heavier
base section -- wider chords and anchor plates -- is drawn where the mast stands on something
that is not mast.

LIVERIES. Yellow, red and white, the colours real tower cranes wear. Every part model is written
once per livery with that livery's textures; the blockstate picks by the stored livery.

The element helpers are gen_scaffold's, so every face carries fitted UVs (see _fit there).
"""

import argparse
import filecmp
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

# livery -> (paint colour, name in each language)
LIVERIES = {
    "yellow": ((232, 178, 28), ("Yellow", "Amarillo", "Gelb", "Gul")),
    "red": ((186, 38, 34), ("Red", "Rojo", "Rot", "Röd")),
    "white": ((226, 226, 220), ("White", "Blanco", "Weiß", "Vit")),
}
LIVERY_ORDER = ["yellow", "red", "white"]
LANG = ("Tower Crane Mast", "Mástil de Grúa Torre", "Turmdrehkran-Mast", "Tornkranmast")

# --------------------------------------------------------------------------------------------
# Geometry catalogue, in sixteenths
# --------------------------------------------------------------------------------------------

CHORD = (0.5, 2.0)        # a chord's near and far edge from the cell's corner
BASE_CHORD = (0.25, 2.5)  # the base section's heavier chord
FACE_INSET = 1.25         # the lacing plane sits mid-chord, this far in from the face
LADDER_RAILS = (6.0, 10.0)
LADDER_Z = 2.25           # inside the north face's lacing
RUNG_PITCH = 4.0

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
    out[NAME + "_ladder"] = {"textures": {"ladder": TEX_REF % "scaffold_tube",
                                          "particle": TEX_REF % "scaffold_tube"},
                             "elements": ladder()}
    return out


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
    for name, body in sorted(part_models().items()):
        gen_cmu._write_json(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    gen_cmu._write_json(os.path.join(state_dir, NAME + ".json"), blockstate())
    written.append(("state", NAME + ".json"))
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    out = []
    for liv in LIVERY_ORDER:
        colour = dict(zip(LANGS, LIVERIES[liv][1]))
        base_name = dict(zip(LANGS, LANG))
        out.append(("tile.%s.%s.name" % (NAME, liv),
                    {lang: "%s (%s)" % (base_name[lang], colour[lang]) for lang in LANGS}))
    return out


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
