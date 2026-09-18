#!/usr/bin/env python3
"""Every asset the stucco sets ship.

Three finishes -- smooth, sand float and knockdown -- each a block/stairs/slab/fence set: one
texture per finish, and the set blockstates and models from gen_cmu.py pointed at it. ``--check``
writes nothing and fails on drift.

    python dev-env-utils/scripts/gen_stucco.py
    python dev-env-utils/scripts/gen_stucco.py --check
    python dev-env-utils/scripts/gen_stucco.py --fragments   # lang and tab lines to paste

Stucco has no joints and no direction, so a finish is one texture on every face, top included,
and the three differ only in what the trowel left: a smooth coat mottles as it cures, a sand float
coat shows its aggregate, and a knockdown coat is splattered on and flattened into pads. All three
are one warm off-white; the finish is the choice, not the colour.

Drawn at 32 px so that sand reads as sand rather than as the vanilla concrete-powder speckle a 16
px grain turns into. Everything wraps around the tile's edges, so a wall of it has no seams.
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
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "buildingmaterials", "stucco")
MODEL_DIR = os.path.join(MODULE, "models", "block", "buildingmaterials")
STATE_DIR = os.path.join(MODULE, "blockstates")

TEX_REF = "csm:blocks/buildingmaterials/stucco/%s"

SIZE = 32
BASE = (222, 214, 198)

# seed      an explicit, stable seed. Python's hash() is salted per process and cannot be used
#           for anything a --check has to reproduce
FINISHES = {
    "stucco_smooth": dict(
        seed=20261001,
        lang=("Smooth Stucco", "Estuco Liso", "Glattputz", "Slät Puts")),
    "stucco_sandfloat": dict(
        seed=20261002,
        lang=("Sand Float Stucco", "Estuco Fratasado", "Scheibenputz", "Filtad Puts")),
    "stucco_knockdown": dict(
        seed=20261003,
        lang=("Knockdown Stucco", "Estuco Aplanado", "Spritzputz", "Stänkputs")),
}

ORDER = ["stucco_smooth", "stucco_sandfloat", "stucco_knockdown"]


# --------------------------------------------------------------------------------------------
# Drawing
# --------------------------------------------------------------------------------------------

def _clamp(values):
    return tuple(max(0, min(255, int(round(v)))) for v in values) + (255,)


def _mottle(rng, cells):
    """Value noise that tiles: random heights on a ``cells`` x ``cells`` lattice, eased between
    with a cosine and read with wrap-around, so the right edge meets the left. Returns a SIZE x
    SIZE grid in -1..1."""
    lattice = [[rng.uniform(-1, 1) for _ in range(cells)] for _ in range(cells)]
    step = SIZE / cells
    out = [[0.0] * SIZE for _ in range(SIZE)]
    for y in range(SIZE):
        gy, fy = divmod(y / step, 1)
        wy = (1 - math.cos(fy * math.pi)) / 2
        y0, y1 = int(gy) % cells, (int(gy) + 1) % cells
        for x in range(SIZE):
            gx, fx = divmod(x / step, 1)
            wx = (1 - math.cos(fx * math.pi)) / 2
            x0, x1 = int(gx) % cells, (int(gx) + 1) % cells
            top = lattice[y0][x0] * (1 - wx) + lattice[y0][x1] * wx
            bottom = lattice[y1][x0] * (1 - wx) + lattice[y1][x1] * wx
            out[y][x] = top * (1 - wy) + bottom * wy
    return out


def _field(rng, mottle, grain):
    """The coat itself: broad mottling from two octaves of noise, plus per-pixel grain. The two
    lattices are 3 and 5 cells, neither of which divides the tile, so their cell edges never
    coincide and the mottling does not read as a grid of squares -- which it does at 4 and 8."""
    broad, fine = _mottle(rng, 3), _mottle(rng, 5)
    return [[(broad[y][x] * 0.65 + fine[y][x] * 0.35) * mottle + rng.uniform(-grain, grain)
             for x in range(SIZE)] for y in range(SIZE)]


def _paint(field):
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = _clamp([c + field[y][x] for c in BASE])
    return img


def smooth(rng):
    """A steel-troweled coat: no aggregate to speak of, only the uneven cure a large flat wall
    shows in raking light."""
    return _paint(_field(rng, mottle=7, grain=1.5))


def sandfloat(rng):
    """A floated coat: the float drags the sand to the surface, so the face is an even field of
    grains, some proud and catching light, some pulled out and leaving a pit."""
    field = _field(rng, mottle=4, grain=5)
    for y in range(SIZE):
        for x in range(SIZE):
            roll = rng.random()
            if roll < 0.10:
                field[y][x] += 12
            elif roll < 0.20:
                field[y][x] -= 14
    return _paint(field)


def knockdown(rng):
    """Splattered on in blobs and knocked flat with a trowel before it sets: raised pads with flat,
    slightly brighter tops, each casting a one-pixel shadow down and to the right, lit from the
    upper left, and the thinner coat showing between them."""
    field = _field(rng, mottle=3, grain=3)
    raised = [[False] * SIZE for _ in range(SIZE)]
    # Each splat is two or three overlapping drops, so a pad is irregular rather than an ellipse,
    # and they are small and many: at 32 px a real knockdown's pads are two to six pixels across
    # and cover a little under half the wall -- more, and they merge into sheets.
    for _ in range(34):
        cx, cy = rng.uniform(0, SIZE), rng.uniform(0, SIZE)
        for _ in range(rng.randint(2, 3)):
            ox, oy = cx + rng.uniform(-2, 2), cy + rng.uniform(-1.5, 1.5)
            rx, ry = rng.uniform(1.2, 2.8), rng.uniform(1.0, 2.2)
            for dy in range(-3, 4):
                for dx in range(-3, 4):
                    if (dx / rx) ** 2 + (dy / ry) ** 2 <= 1:
                        raised[int(oy + dy) % SIZE][int(ox + dx) % SIZE] = True
    for y in range(SIZE):
        for x in range(SIZE):
            # The shadow is drawn on the coat just below and right of a pad, not on the pad's own
            # edge: drawn on the pad it came out no darker than the thin coat around it, and the
            # pads read as flat patches of a lighter paint rather than as anything raised.
            if raised[y][x]:
                field[y][x] += 6
                if not raised[(y - 1) % SIZE][x]:
                    field[y][x] += 5
            elif raised[(y - 1) % SIZE][x] or raised[y][(x - 1) % SIZE]:
                field[y][x] -= 16
            else:
                field[y][x] -= 4
    return _paint(field)


DRAW = {"stucco_smooth": smooth, "stucco_sandfloat": sandfloat, "stucco_knockdown": knockdown}


def texture(name):
    return DRAW[name](random.Random(FINISHES[name]["seed"]))


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    for name in ORDER:
        texture(name).save(os.path.join(tex_dir, name + ".png"))
        written.append(("tex", name + ".png"))
        for state, body in sorted(gen_cmu.blockstates(name, TEX_REF, top_suffix="").items()):
            gen_cmu._write_json(os.path.join(state_dir, state + ".json"), body)
            written.append(("state", state + ".json"))
        for model, body in sorted(gen_cmu.models(name, TEX_REF, top_suffix="").items()):
            gen_cmu._write_json(os.path.join(model_dir, model + ".json"), body)
            written.append(("model", model + ".json"))
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    """(lang key, per-language name) for every block a stucco set registers."""
    out = []
    for name in ORDER:
        base = dict(zip(LANGS, FINISHES[name]["lang"]))
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
    lines.append("# tab registration, in CsmTabBuildingMaterials, AFTER the brick sets")
    lines.append("")
    for name in ORDER:
        lines.append("    initTabBlock(%s.class,"
                     "\n        fmlPreInitializationEvent); // %s Set (Block, Fence, Slab, Stairs)"
                     % (gen_cmu.class_name(name), FINISHES[name]["lang"][0]))
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
        print("Wrote %d files for %d finish(es)" % (len(written), len(ORDER)))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_stucco_")
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
        print("%d generated stucco files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
