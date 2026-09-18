#!/usr/bin/env python3
"""Every asset the stone veneer sets ship.

Three veneers -- ashlar, fieldstone and cast stone -- each a block/stairs/slab/fence set in the
stone it is most often seen in: the side and top textures, and the set blockstates and models from
gen_cmu.py pointed at them. ``--check`` writes nothing and fails on drift.

    python dev-env-utils/scripts/gen_stone.py
    python dev-env-utils/scripts/gen_stone.py --check
    python dev-env-utils/scripts/gen_stone.py --fragments   # lang and tab lines to paste

Three different kinds of order. Ashlar is dressed stone in level courses of UNEQUAL height with
fine joints, which is what separates it from brick or block. Fieldstone has no courses at all: it
is drawn as a Voronoi partition of the tile, measured on a torus so the cells wrap and the wall has
no seam, each cell a stone of its own colour, rounded by shading, set in recessed mortar. Cast
stone is manufactured, so it is ashlar's regularity with none of its variation.

All 32 px, lit from the upper left like every other set in the tab.
"""

import argparse
import filecmp
import math
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

import gen_cmu

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODULE = os.path.join(REPO, "modules", "building", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "buildingmaterials", "stone")
MODEL_DIR = os.path.join(MODULE, "models", "block", "buildingmaterials")
STATE_DIR = os.path.join(MODULE, "blockstates")

TEX_REF = "csm:blocks/buildingmaterials/stone/%s"

SIZE = 32

# body      the face colour
# mortar    the joint
# seed      an explicit, stable seed. Python's hash() is salted per process and cannot be used
#           for anything a --check has to reproduce
VENEERS = {
    "stone_ashlar": dict(
        seed=20261030, body=(196, 186, 164), mortar=(214, 208, 194),
        lang=("Ashlar Stone Veneer", "Chapado de Sillería", "Quadermauerwerk",
              "Kvaderstensfasad")),
    "stone_fieldstone": dict(
        seed=20261031, body=(140, 132, 120), mortar=(112, 108, 102),
        lang=("Fieldstone Veneer", "Chapado de Mampostería", "Bruchsteinmauerwerk",
              "Natursten Fasad")),
    "stone_cast": dict(
        seed=20261032, body=(206, 196, 172), mortar=(190, 182, 164),
        lang=("Cast Stone Veneer", "Chapado de Piedra Artificial", "Betonwerkstein",
              "Gjuten Sten Fasad")),
}

ORDER = ["stone_ashlar", "stone_fieldstone", "stone_cast"]

# Ashlar's courses: unequal, and summing to the tile so they stack block on block.
ASHLAR_COURSES = (12, 8, 12)
# Fieldstone's stones, one colour each: the greys, tans and rusts of a field wall.
FIELDSTONE_PALETTE = [(150, 144, 134), (128, 124, 118), (164, 150, 126), (140, 118, 96),
                      (118, 112, 104), (172, 164, 150), (146, 128, 110)]
FIELDSTONE_STONES = 13
# Cast stone: running bond, one unit a block wide and half a block tall. Real units are big; at
# 16 x 8 in this bond it read as buff brick.
CAST_W, CAST_H = 32, 16


# --------------------------------------------------------------------------------------------
# Drawing
# --------------------------------------------------------------------------------------------

def _clamp(colour):
    return tuple(max(0, min(255, int(round(c)))) for c in colour[:3]) + (255,)


def _shift(colour, d):
    return _clamp([c + d for c in colour[:3]])


def _stone(px, rng, colour, x0, y0, w, h, grain, relief):
    """One dressed stone, x0/y0 its top-left inside the joints; wraps horizontally."""
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            d = rng.uniform(-grain, grain)
            if y == y0:
                d += relief
            elif y == y0 + h - 1:
                d -= relief
            if x == x0:
                d += relief / 2
            elif x == x0 + w - 1:
                d -= relief / 2
            px[x % SIZE, y] = _shift(colour, d)


def ashlar(rng, spec):
    """Coursed ashlar: level courses of unequal height, each stone a random length, fine joints,
    and a stone-to-stone variation in shade that limestone has and cast stone does not."""
    img = Image.new("RGBA", (SIZE, SIZE), _clamp(spec["mortar"]))
    px = img.load()
    y0 = 0
    for height in ASHLAR_COURSES:
        x = rng.randrange(SIZE)
        end = x + SIZE
        while x < end:
            w = min(rng.randint(10, 20), end - x)
            if end - x - w < 8:
                w = end - x  # never leave a sliver of a stone at the end of the course
            colour = _shift(spec["body"], rng.uniform(-9, 9))
            _stone(px, rng, colour, x + 1, y0 + 1, w - 1, height - 1, grain=4, relief=8)
            x += w
        y0 += height
    return img


def _torus_distance(ax, ay, bx, by):
    dx = abs(ax - bx)
    dy = abs(ay - by)
    return math.hypot(min(dx, SIZE - dx), min(dy, SIZE - dy))


def fieldstone(rng, spec):
    """Fieldstone: a Voronoi partition of the tile on a torus, so the stones wrap and the wall has
    no seam. A pixel near the boundary between two cells is mortar; a stone is rounded by
    darkening it toward its edge and lighting it toward its upper left; the mortar is recessed and
    in shadow."""
    seeds = []
    while len(seeds) < FIELDSTONE_STONES:
        p = (rng.uniform(0, SIZE), rng.uniform(0, SIZE))
        # Keep seeds apart, so no stone comes out a sliver.
        if all(_torus_distance(p[0], p[1], q[0], q[1]) > 6 for q in seeds):
            seeds.append(p)
    colours = [rng.choice(FIELDSTONE_PALETTE) for _ in seeds]
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            cx, cy = x + 0.5, y + 0.5
            ranked = sorted((_torus_distance(cx, cy, sx, sy), i)
                            for i, (sx, sy) in enumerate(seeds))
            (d0, i0), (d1, _) = ranked[0], ranked[1]
            edge = d1 - d0
            if edge < 1.6:
                px[x, y] = _shift(spec["mortar"], rng.uniform(-4, 4))
                continue
            sx, sy = seeds[i0]
            # Upper-left light on a rounded face: brighter on the side toward the light.
            ox = ((cx - sx + SIZE / 2) % SIZE) - SIZE / 2
            oy = ((cy - sy + SIZE / 2) % SIZE) - SIZE / 2
            lit = -(ox + oy) * 1.4
            rim = -14 if edge < 2.6 else 0
            px[x, y] = _shift(colours[i0], lit + rim + rng.uniform(-5, 5))
    return img


def cast(rng, spec):
    """Cast stone: manufactured units in running bond, all one colour, fine even grain, crisp
    arrises and thin joints -- ashlar's order without any of its variation."""
    img = Image.new("RGBA", (SIZE, SIZE), _clamp(spec["mortar"]))
    px = img.load()
    for course in range(SIZE // CAST_H):
        offset = CAST_W // 2 if course % 2 else 0
        for unit in range(SIZE // CAST_W):
            x0 = offset + unit * CAST_W
            _stone(px, rng, spec["body"], x0 + 1, course * CAST_H + 1, CAST_W - 1, CAST_H - 1,
                   grain=2.5, relief=7)
    return img


def top(rng, spec):
    """The top of a veneered wall: the stone's bed, flat and a little darker than the face."""
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = _shift(spec["body"], -10 + rng.uniform(-5, 5))
    return img


DRAW = {"stone_ashlar": ashlar, "stone_fieldstone": fieldstone, "stone_cast": cast}


def side_texture(name):
    spec = VENEERS[name]
    return DRAW[name](random.Random(spec["seed"]), spec)


def top_texture(name):
    spec = VENEERS[name]
    return top(random.Random(spec["seed"] + 7), spec)


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    for name in ORDER:
        side_texture(name).save(os.path.join(tex_dir, name + ".png"))
        top_texture(name).save(os.path.join(tex_dir, name + "_top.png"))
        written += [("tex", name + ".png"), ("tex", name + "_top.png")]
        for state, body in sorted(gen_cmu.blockstates(name, TEX_REF).items()):
            gen_cmu._write_json(os.path.join(state_dir, state + ".json"), body)
            written.append(("state", state + ".json"))
        for model, body in sorted(gen_cmu.models(name, TEX_REF).items()):
            gen_cmu._write_json(os.path.join(model_dir, model + ".json"), body)
            written.append(("model", model + ".json"))
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    """(lang key, per-language name) for every block a stone veneer set registers."""
    out = []
    for name in ORDER:
        base = dict(zip(LANGS, VENEERS[name]["lang"]))
        for suffix, _ in gen_cmu.VARIANTS:
            out.append(("tile.%s%s.name" % (name, suffix),
                        {lang: (base[lang] if not suffix
                                else "%s %s" % (base[lang], gen_cmu.VARIANT_WORDS[lang][suffix]))
                         for lang in LANGS}))
    return out


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    lines.append("# tab registration, in CsmTabBuildingMaterials, AFTER the cladding sets")
    lines.append("")
    for name in ORDER:
        lines.append("    initTabBlock(%s.class,"
                     "\n        fmlPreInitializationEvent); // %s Set (Block, Fence, Slab, Stairs)"
                     % (gen_cmu.class_name(name), VENEERS[name]["lang"][0]))
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

    if not args.check:
        written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR)
        print("Wrote %d files for %d veneer(s)" % (len(written), len(ORDER)))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_stone_")
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
        print("%d generated stone files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
