#!/usr/bin/env python3
"""Every asset the siding sets ship.

Four profiles -- fiber cement lap, board and batten, cedar shingle and vinyl -- each a
block/stairs/slab/fence set in the colour that profile is most often seen in: the side and top
textures, and the set blockstates and models from gen_cmu.py pointed at them. ``--check`` writes
nothing and fails on drift.

Also here, because it is drawn the same way: knotty cedar V-groove paneling, the tongue-and-groove
boards of a lodge or cabin interior, as a full-block set (registered in Interior Finishes, not
with the siding).

    python dev-env-utils/scripts/gen_siding.py
    python dev-env-utils/scripts/gen_siding.py --check
    python dev-env-utils/scripts/gen_siding.py --fragments   # lang and tab lines to paste

DIRECTION. Siding, unlike brick or stucco, has a grain: lap boards and shingle courses run
level, board and batten runs plumb. A cube_bottom_top model maps the side texture upright on all
four faces, and a stairs or fence blockstate only ever rotates about the vertical axis, so the
texture's up is the wall's up on every face of every block in a set. Nothing here has to fight
the model for its orientation.

COURSING. Lap, vinyl and shingle courses are 8 px on a 32 px texture: four to a block, and two to
a slab, so a slab's side lands on a course line rather than halfway up a board. Each course is
drawn with the shadow line on its TOP row, because the butt of the board above overhangs it and
the light comes from above.
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
import model_depth  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODULE = os.path.join(REPO, "modules", "building", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "buildingmaterials", "siding")
MODEL_DIR = os.path.join(MODULE, "models", "block", "buildingmaterials")
STATE_DIR = os.path.join(MODULE, "blockstates")

TEX_REF = "csm:blocks/buildingmaterials/siding/%s"

SIZE = 32
COURSE = 8       # a lap, vinyl or shingle course, shadow line included
BATTEN_PITCH = 16
BATTEN_AT = 6    # where the first batten starts; any value works, the pattern wraps

# body      the face colour
# seed      an explicit, stable seed. Python's hash() is salted per process and cannot be used
#           for anything a --check has to reproduce
PROFILES = {
    "siding_lap": dict(
        seed=20261010, body=(108, 124, 138),
        lang=("Fiber Cement Lap Siding", "Revestimiento de Fibrocemento",
              "Faserzement-Stulpschalung", "Fibercementpanel")),
    "siding_boardbatten": dict(
        seed=20261011, body=(232, 230, 222),
        lang=("Board and Batten Siding", "Revestimiento de Tabla y Listón",
              "Boden-Deckel-Schalung", "Lockpanel")),
    "siding_shingle": dict(
        seed=20261012, body=(160, 106, 66),
        lang=("Cedar Shingle Siding", "Revestimiento de Tejuelas de Cedro", "Zedernschindeln",
              "Cederspån")),
    "siding_vinyl": dict(
        seed=20261013, body=(216, 206, 184),
        lang=("Vinyl Siding", "Revestimiento de Vinilo", "Vinylverkleidung", "Vinylpanel")),
}

# Interior paneling, drawn as siding is; registered in CsmTabInteriorFinishes, not with the siding
PANELING = {
    "paneling_knotty_cedar": dict(
        seed=20261101, body=(178, 114, 70),
        lang=("Knotty Cedar V-Groove Paneling", "Friso de Cedro Nudoso Machihembrado",
              "Astige Zedern-Profilholzschalung", "Kvistig Cederpanel med V-Fog")),
}
PROFILES.update(PANELING)

ORDER = ["siding_lap", "siding_boardbatten", "siding_shingle", "siding_vinyl",
         "paneling_knotty_cedar"]

VGROOVE_PITCH = 8  # a board, groove included: four to a block, about a 9 in board


# --------------------------------------------------------------------------------------------
# Drawing
# --------------------------------------------------------------------------------------------

def _clamp(colour):
    return tuple(max(0, min(255, int(round(c)))) for c in colour[:3]) + (255,)


def _shift(colour, d):
    return _clamp([c + d for c in colour[:3]])


def _blank(body):
    return Image.new("RGBA", (SIZE, SIZE), _clamp(body))


def _streaks(rng, length, count, depth):
    """A row's worth of grain: ``count`` darker streaks of up to ``length`` pixels, wrapping."""
    row = [0.0] * SIZE
    for _ in range(count):
        start, run = rng.randrange(SIZE), rng.randint(3, length)
        for i in range(run):
            row[(start + i) % SIZE] -= depth
    return row


def lap(rng, body):
    """Fiber cement lap boards: a shadow line under each butt, the board face with the embossed
    cedar grain the product is pressed with, and the butt edge along the bottom catching the
    light."""
    img = _blank(body)
    px = img.load()
    for course in range(SIZE // COURSE):
        y0 = course * COURSE
        for r in range(COURSE):
            grain = _streaks(rng, 12, 3, 5) if 2 <= r <= 6 else [0.0] * SIZE
            bias = {0: -34, 1: -14, COURSE - 1: 8}.get(r, 0)
            for x in range(SIZE):
                px[x, y0 + r] = _shift(body, bias + grain[x] + rng.uniform(-3, 3))
    return img


def vinyl(rng, body):
    """Vinyl in a Dutch lap profile: each course has a concave cove along its top before the flat
    face, which is the one thing that tells it from lap siding at a distance. Extruded, so almost
    no grain."""
    img = _blank(body)
    px = img.load()
    cove = {0: -30, 1: -20, 2: -12, 3: -5, COURSE - 1: 7}
    for course in range(SIZE // COURSE):
        y0 = course * COURSE
        for r in range(COURSE):
            grain = _streaks(rng, 10, 2, 2) if 3 <= r <= 6 else [0.0] * SIZE
            for x in range(SIZE):
                px[x, y0 + r] = _shift(body, cove.get(r, 0) + grain[x] + rng.uniform(-1.5, 1.5))
    return img


def board_batten(rng, body):
    """Wide boards run plumb with a narrow batten over every joint. Lit from the upper left: a
    batten's left edge catches the light, its right edge and the board just beside it are in its
    shadow. The boards carry a faint vertical grain through the paint."""
    img = _blank(body)
    px = img.load()
    column = [rng.uniform(-3, 3) for _ in range(SIZE)]
    # White on white: the shadow has to be deep or the battens vanish into the boards.
    profile = {0: 10, 1: 4, 2: -26, 3: -16, 4: -6}
    for x in range(SIZE):
        offset = (x - BATTEN_AT) % BATTEN_PITCH
        bias = profile.get(offset, 0) if offset < len(profile) else 0
        for y in range(SIZE):
            px[x, y] = _shift(body, bias + column[x] + rng.uniform(-2, 2))
    return img


def shingle(rng, body):
    """Cedar shingles: courses of random-width shingles, a dark keyway between neighbours, a
    shadow under the butts of the course above, and cedar's strong shingle-to-shingle colour
    variation. Each course starts at a random offset so no keyway lines up with the one below."""
    img = _blank(body)
    px = img.load()
    for course in range(SIZE // COURSE):
        y0 = course * COURSE
        widths = []
        # Real shingles run three to fourteen inches wide; at this scale four to twelve pixels.
        # Narrower and evener than that, a course reads as parquet rather than as shingles.
        while sum(widths) < SIZE:
            widths.append(rng.randint(4, 12))
        overshoot = sum(widths) - SIZE
        # Take the overshoot back out of the widest shingles, so none drops under four pixels.
        while overshoot:
            i = widths.index(max(widths))
            widths[i] -= 1
            overshoot -= 1
        x = rng.randrange(SIZE)
        for w in widths:
            # Cedar varies more along a shingle's grain than from one shingle to the next. Tinted
            # harder than this, each shingle is a flat patch between straight lines, and a course
            # reads as tiles.
            tint = rng.randint(-10, 10)
            # The butt of the course above is not a ruled line: some shingles hang a pixel lower
            # and throw a longer shadow.
            ragged = rng.random() < 0.4
            # Split-cedar grain runs down the shingle in streaks a pixel or two wide.
            grain, level = [], 0.0
            for _ in range(w):
                if rng.random() < 0.55:
                    level = rng.uniform(-14, 14)
                grain.append(level)
            for i in range(w):
                for r in range(COURSE):
                    if r == 0:
                        d = -30
                    elif r == 1 and ragged:
                        d = -16
                    elif i == 0:
                        d = -24  # the keyway: the gap between two shingles
                    else:
                        # Lighter toward the butt, which weathers, darker up under the course above.
                        d = tint + grain[i] + (r - 4) * 1.5 + (6 if r == COURSE - 1 else 0)
                    px[(x + i) % SIZE, y0 + r] = _shift(body, d + rng.uniform(-3, 3))
            x += w
    return img


def vgroove(rng, body):
    """Knotty cedar tongue-and-groove boards run plumb, a V-groove at every joint: the left bevel in
    shadow, the right catching the light (lit from the upper left). Each board is its own shade of
    cedar with a grain streaking up it, and the knots -- a dark core in a darker ring, with the grain
    bending round it -- fall where they like, the occasional one cut by a groove, as on real boards.
    Knots wrap at the edges, so the pattern tiles up and across a wall."""
    img = _blank(body)
    px = img.load()
    boards = SIZE // VGROOVE_PITCH
    tints = [rng.randint(-14, 14) for _ in range(boards)]
    grain = [rng.uniform(-8, 8) for _ in range(SIZE)]
    # Knots: (x, y, radius)
    knots = [(rng.randrange(SIZE), rng.randrange(SIZE), rng.choice((1.2, 1.6, 2.0)))
             for _ in range(rng.randint(3, 4))]

    def wrap(d):
        return min(d % SIZE, -d % SIZE)

    for x in range(SIZE):
        offset = x % VGROOVE_PITCH
        bias = {0: -40, 1: -18, VGROOVE_PITCH - 1: 12}.get(offset, 0)
        for y in range(SIZE):
            d = tints[x // VGROOVE_PITCH] + bias + grain[x] + rng.uniform(-4, 4)
            # Grain swirls darker round each knot, and the knot itself is near-black brown
            for kx, ky, r in knots:
                dist = (wrap(x - kx) ** 2 + wrap(y - ky) ** 2) ** 0.5
                if dist <= r * 0.6:
                    d = -78 + rng.uniform(-4, 4)
                elif dist <= r:
                    d = min(d, -52)
                elif dist <= r + 2.2:
                    d -= 16 * (r + 2.2 - dist) / 2.2
            px[x, y] = _shift(body, d)
    return img


def top(rng, body):
    """The top of a sided wall: the sheathing's edge, flat and a little darker than the face."""
    img = _blank(body)
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = _shift(body, -10 + rng.uniform(-4, 4))
    return img


DRAW = {"siding_lap": lap, "siding_boardbatten": board_batten, "siding_shingle": shingle,
        "siding_vinyl": vinyl, "paneling_knotty_cedar": vgroove}


def side_texture(name):
    spec = PROFILES[name]
    return DRAW[name](random.Random(spec["seed"]), spec["body"])


def top_texture(name):
    spec = PROFILES[name]
    return top(random.Random(spec["seed"] + 7), spec["body"])


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
    model_depth.separate_dirs([(tex_dir, TEX_DIR), (model_dir, MODEL_DIR), (state_dir, STATE_DIR)], written, gen_cmu._write_json)
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    """(lang key, per-language name) for every block a siding set registers."""
    out = []
    for name in ORDER:
        base = dict(zip(LANGS, PROFILES[name]["lang"]))
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
    lines.append("# tab registration, in CsmTabBuildingMaterials, AFTER the stucco sets")
    lines.append("# (the paneling goes in CsmTabInteriorFinishes, after the hardwood sets)")
    lines.append("")
    for name in ORDER:
        lines.append("    initTabBlock(%s.class,"
                     "\n        fmlPreInitializationEvent); // %s Set (Block, Fence, Slab, Stairs)"
                     % (gen_cmu.class_name(name), PROFILES[name]["lang"][0]))
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
        print("Wrote %d files for %d profile(s)" % (len(written), len(ORDER)))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_siding_")
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
        print("%d generated siding files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
