#!/usr/bin/env python3
"""Every asset the brick sets ship.

Four colours, each a block/stairs/slab/fence set plus three trim blocks -- a soldier course, a
header course and a course with weep holes -- from one catalogue: the textures, the blockstates and
the models. ``--check`` writes nothing and fails on drift.

    python dev-env-utils/scripts/gen_masonry.py
    python dev-env-utils/scripts/gen_masonry.py --check
    python dev-env-utils/scripts/gen_masonry.py --fragments   # lang and tab lines to paste

The set blockstates and models are gen_cmu.py's, pointed at this generator's textures; a brick set
and a CMU set differ only in what is drawn on them.

COURSING. A modular brick is nominally 8 x 2 2/3 inches, three to one, and a running bond only
tiles if a brick's length divides the texture. On a power-of-two texture that forces a unit of two
or four to one, counting the joint. At 16 px the only fit is 8 x 4, which is exactly the CMU's
coursing, so the two would differ only in colour. The brick is drawn instead at 32 px as 16 x 4
units: two bricks across a block and eight courses up it, a 15 x 3 face. That is about twice real
size, long and thin enough to read as brick beside the block, and still legible at a distance --
true scale is sixteen courses a block, and three-pixel bricks at that density shimmer.

Each course is laid with its mortar bed on its top row and each brick's head joint in its first
column, so a course is one row of mortar and three of brick. Alternate courses step half a brick.
"""

import argparse
import filecmp
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

import gen_cmu

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODULE = os.path.join(REPO, "modules", "building", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "buildingmaterials", "brick")
MODEL_DIR = os.path.join(MODULE, "models", "block", "buildingmaterials")
STATE_DIR = os.path.join(MODULE, "blockstates")

TEX_REF = "csm:blocks/buildingmaterials/brick/%s"

SIZE = 32
BRICK_W = 16    # a stretcher's length, joint included
COURSE_H = 4    # a course's height, bed joint included
HEADER_W = 8    # a header -- the brick's end -- is half a stretcher
COURSES = SIZE // COURSE_H


# --------------------------------------------------------------------------------------------
# Colours
# --------------------------------------------------------------------------------------------
#
# body      the face colour
# unit      how far a whole brick may differ from the next. This, not per-pixel noise, is what
#           makes a brick wall read as brick: every unit came out of the kiln a little different
# grain     how far each pixel may wander from its brick's colour
# mortar    the joint, always paler and flatter than the brick
# speckle   (colour, chance) for an iron spot or fleck, or None
# seed      an explicit, stable seed. Python's hash() is salted per process and cannot be used
#           for anything a --check has to reproduce

COLOURS = {
    "brick_red": dict(
        seed=20260930, body=(150, 66, 48), unit=14, grain=7, mortar=(176, 170, 158),
        speckle=((96, 42, 32), 0.05),
        lang=("Red Brick", "Ladrillo Rojo", "Roter Ziegelstein", "Rött Tegel")),
    "brick_brown": dict(
        seed=20260931, body=(108, 72, 52), unit=12, grain=7, mortar=(168, 162, 150),
        speckle=((70, 46, 34), 0.05),
        lang=("Brown Brick", "Ladrillo Marrón", "Brauner Ziegelstein", "Brunt Tegel")),
    "brick_buff": dict(
        seed=20260932, body=(204, 176, 128), unit=12, grain=6, mortar=(222, 214, 198),
        speckle=((170, 140, 98), 0.04),
        lang=("Buff Brick", "Ladrillo Beige", "Gelber Ziegelstein", "Gult Tegel")),
    "brick_grey": dict(
        seed=20260933, body=(126, 124, 120), unit=10, grain=6, mortar=(172, 170, 164),
        speckle=None,
        lang=("Grey Brick", "Ladrillo Gris", "Grauer Ziegelstein", "Grått Tegel")),
}

ORDER = ["brick_red", "brick_brown", "brick_buff", "brick_grey"]

# The trim blocks each colour carries, in creative order, with the name each language adds.
TRIMS = {
    "soldier": ("Soldier Course", "Hilada a Sardinel", "Grenadierschicht", "Stående Skift"),
    "header": ("Header Course", "Hilada a Tizón", "Binderschicht", "Koppskift"),
    "weep": ("Weep Holes", "Orificios de Drenaje", "Entwässerungsöffnungen", "Dräneringshål"),
}
TRIM_ORDER = ["soldier", "header", "weep"]

WEEP = (30, 26, 24, 255)


# --------------------------------------------------------------------------------------------
# Laying brick
# --------------------------------------------------------------------------------------------

def _clamp(colour):
    return tuple(max(0, min(255, int(c))) for c in colour[:3]) + (255,)


def _shift(colour, d):
    return _clamp([c + d for c in colour[:3]])


def _mortar(img, rng, spec):
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = _shift(spec["mortar"], rng.randint(-3, 3))


def _unit(px, rng, spec, x0, y0, w, h, shade=0):
    """One brick face, x0/y0 its top-left pixel inside the joints. Wraps horizontally, so a brick
    cut by the texture's edge is one brick with one colour when the texture tiles."""
    tint = rng.randint(-spec["unit"], spec["unit"]) + shade
    base = _shift(spec["body"], tint)
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            colour = _shift(base, rng.randint(-spec["grain"], spec["grain"]))
            if spec["speckle"] and rng.random() < spec["speckle"][1]:
                colour = _clamp(spec["speckle"][0])
            # The arris along the top catches the light and the one along the bottom sits in the
            # shadow of the bed joint below: the two rows that make a flat texture read as a brick
            # standing proud of its mortar.
            if y == y0:
                colour = _shift(colour, 10)
            elif y == y0 + h - 1:
                colour = _shift(colour, -10)
            px[x % SIZE, y] = colour


def lay_course(px, rng, spec, y0, height, unit_w, offset, shade=0):
    """One course: a bed joint on its top row, and a head joint at every ``unit_w`` from
    ``offset``, every brick shifted by ``shade``. Returns the x of each head joint."""
    joints = []
    for i in range(SIZE // unit_w):
        x = offset + i * unit_w
        joints.append(x % SIZE)
        _unit(px, rng, spec, x + 1, y0 + 1, unit_w - 1, height - 1, shade)
    return joints


def _stretcher(px, rng, spec, course):
    """Course ``course`` of the running bond. The half-brick step is taken from the course's
    position in the block, so any trim that keeps a course keeps it in bond with the wall."""
    offset = (BRICK_W // 2) if course % 2 else 0
    return lay_course(px, rng, spec, course * COURSE_H, COURSE_H, BRICK_W, offset)


def face_texture(name):
    """The plain wall: eight courses of running bond."""
    spec = COLOURS[name]
    rng = random.Random(spec["seed"])
    img = Image.new("RGBA", (SIZE, SIZE))
    _mortar(img, rng, spec)
    px = img.load()
    for course in range(COURSES):
        _stretcher(px, rng, spec, course)
    return img


def top_texture(name):
    """The top of a wall: the bricks' beds, half as wide as they are long, in the same bond."""
    spec = COLOURS[name]
    rng = random.Random(spec["seed"] + 7)
    img = Image.new("RGBA", (SIZE, SIZE))
    _mortar(img, rng, spec)
    px = img.load()
    for row in range(SIZE // 8):
        lay_course(px, rng, spec, row * 8, 8, BRICK_W, (BRICK_W // 2) if row % 2 else 0)
    return img


def soldier_texture(name):
    """A soldier course across the middle of the block: bricks stood on end, their faces showing,
    one brick-length tall. Two courses of running bond above and below, at the same positions as
    in the plain wall, so the band sits in a wall rather than on it."""
    spec = COLOURS[name]
    rng = random.Random(spec["seed"] + 11)
    img = Image.new("RGBA", (SIZE, SIZE))
    _mortar(img, rng, spec)
    px = img.load()
    for course in (0, 1, 6, 7):
        _stretcher(px, rng, spec, course)
    # A brick stood up is its own length tall and its own height wide: 3 px faces, 15 px tall.
    lay_course(px, rng, spec, 2 * COURSE_H, BRICK_W, COURSE_H, 0)
    return img


def header_texture(name):
    """Seven courses of stretchers and one of headers along the bottom. Stacked, that is common
    bond, which is what most real brick walls are laid in; in a single row it is a header band.
    The headers step a quarter brick off the stretchers' joints, as the closers at a real corner
    make them.

    The headers are drawn darker than the stretchers. A brick's end sits nearer the flame in the
    kiln and fires darker than its face, which is also what lets a header course be seen at all:
    drawn in the stretchers' colour, a row of half-length bricks reads only as extra joints."""
    spec = COLOURS[name]
    rng = random.Random(spec["seed"] + 13)
    img = Image.new("RGBA", (SIZE, SIZE))
    _mortar(img, rng, spec)
    px = img.load()
    for course in range(COURSES - 1):
        _stretcher(px, rng, spec, course)
    lay_course(px, rng, spec, (COURSES - 1) * COURSE_H, COURSE_H, HEADER_W, BRICK_W // 4,
               shade=-22)
    return img


def weep_texture(name):
    """The plain wall with the bottom course's head joints left open, as the course above the
    flashing is laid so the cavity can drain. One open joint a block, which is about a real
    weep's spacing at this scale."""
    spec = COLOURS[name]
    rng = random.Random(spec["seed"] + 17)
    img = Image.new("RGBA", (SIZE, SIZE))
    _mortar(img, rng, spec)
    px = img.load()
    joints = []
    for course in range(COURSES):
        joints = _stretcher(px, rng, spec, course)
    weep = joints[0]
    y0 = (COURSES - 1) * COURSE_H
    for y in range(y0 + 1, SIZE):
        px[weep, y] = WEEP
        # The open joint's inner end is in shadow; the brick beside it shows it.
        px[(weep + 1) % SIZE, y] = _shift(px[(weep + 1) % SIZE, y], -26)
    return img


TRIM_TEXTURES = {"soldier": soldier_texture, "header": header_texture, "weep": weep_texture}


# --------------------------------------------------------------------------------------------
# Blockstates
# --------------------------------------------------------------------------------------------

def trim_blockstate(name, trim):
    side, top = TEX_REF % ("%s_%s" % (name, trim)), TEX_REF % (name + "_top")
    return {
        "forge_marker": 1,
        "defaults": {"model": "cube_bottom_top",
                     "textures": {"top": top, "bottom": top, "side": side, "particle": side}},
        "variants": {"inventory": [{}], "normal": [{}]},
    }


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    for name in ORDER:
        textures = {name: face_texture(name), name + "_top": top_texture(name)}
        for trim in TRIM_ORDER:
            textures["%s_%s" % (name, trim)] = TRIM_TEXTURES[trim](name)
        for tex, img in sorted(textures.items()):
            img.save(os.path.join(tex_dir, tex + ".png"))
            written.append(("tex", tex + ".png"))
        states = dict(gen_cmu.blockstates(name, TEX_REF))
        for trim in TRIM_ORDER:
            states["%s_%s" % (name, trim)] = trim_blockstate(name, trim)
        for state, body in sorted(states.items()):
            gen_cmu._write_json(os.path.join(state_dir, state + ".json"), body)
            written.append(("state", state + ".json"))
        for model, body in sorted(gen_cmu.models(name, TEX_REF).items()):
            gen_cmu._write_json(os.path.join(model_dir, model + ".json"), body)
            written.append(("model", model + ".json"))
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    """(lang key, per-language name) for every block the brick sets register."""
    out = []
    for name in ORDER:
        base = dict(zip(LANGS, COLOURS[name]["lang"]))
        for suffix, _ in gen_cmu.VARIANTS:
            names = {lang: (base[lang] if not suffix
                            else "%s %s" % (base[lang], gen_cmu.VARIANT_WORDS[lang][suffix]))
                     for lang in LANGS}
            out.append(("tile.%s%s.name" % (name, suffix), names))
        for trim in TRIM_ORDER:
            words = dict(zip(LANGS, TRIMS[trim]))
            out.append(("tile.%s_%s.name" % (name, trim),
                        {lang: "%s (%s)" % (base[lang], words[lang]) for lang in LANGS}))
    return out


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    lines.append("# tab registration, in CsmTabBuildingMaterials, AFTER the CMU sets")
    lines.append("")
    for name in ORDER:
        en = COLOURS[name]["lang"][0]
        lines.append("    initTabBlock(%s.class,"
                     "\n        fmlPreInitializationEvent); // %s Set (Block, Fence, Slab, Stairs)"
                     % (gen_cmu.class_name(name), en))
        for trim in TRIM_ORDER:
            lines.append('    initTabBlock(new BlockBrickTrim("%s_%s")); // %s (%s)'
                         % (name, trim, en, TRIMS[trim][0]))
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
        print("Wrote %d files for %d colour(s)" % (len(written), len(ORDER)))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_masonry_")
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
        print("%d generated brick files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
