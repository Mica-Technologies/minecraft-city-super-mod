#!/usr/bin/env python3
"""Every asset the metal and panel cladding sets ship.

Four profiles -- corrugated steel, standing seam, insulated panel and composite panel -- each a
block/stairs/slab/fence set in the finish that profile is most often seen in: the side and top
textures, and the set blockstates and models from gen_cmu.py pointed at them. ``--check`` writes
nothing and fails on drift.

    python dev-env-utils/scripts/gen_cladding.py
    python dev-env-utils/scripts/gen_cladding.py --check
    python dev-env-utils/scripts/gen_cladding.py --fragments   # lang and tab lines to paste

PROFILE IN THE TEXTURE, NOT THE MODEL. The framing family's roof deck draws its ribs as model
geometry, which is right for a deck seen from below and at its edge. A wall of cladding is seen
face on, where shading alone reads as the profile, and a full opaque cube keeps the set's stairs,
slab and fence and costs nothing to render. What carries over from the deck is the rule that the
rib pitch divides the block: every pitch here divides 32, so a run of blocks has no bunched or
split rib at a seam.

Lit from the upper left, like every other set in the tab: a rib's left flank is bright, its right
flank and whatever it overhangs are in shadow.
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
import gen_framing

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODULE = os.path.join(REPO, "modules", "building", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "buildingmaterials", "cladding")
MODEL_DIR = os.path.join(MODULE, "models", "block", "buildingmaterials")
STATE_DIR = os.path.join(MODULE, "blockstates")

TEX_REF = "csm:blocks/buildingmaterials/cladding/%s"

SIZE = 32
CORRUGATION = 4    # corrugated pitch: eight to a block
SEAM_PITCH = 16    # standing seam pans: two to a block
PANEL_H = 16       # insulated and composite panels: two courses to a block

# body      the face colour
# seed      an explicit, stable seed. Python's hash() is salted per process and cannot be used
#           for anything a --check has to reproduce
PROFILES = {
    "cladding_corrugated": dict(
        # The framing family's galvanised steel, so a corrugated wall and a steel stud behind it
        # are the same metal.
        seed=20261020, body=gen_framing.STEEL_BASE[:3],
        lang=("Corrugated Steel Cladding", "Revestimiento de Acero Corrugado",
              "Wellblechverkleidung", "Korrugerad Plåtfasad")),
    "cladding_standingseam": dict(
        seed=20261021, body=(72, 76, 80),
        lang=("Standing Seam Cladding", "Revestimiento de Junta Alzada",
              "Stehfalzverkleidung", "Falsad Plåtfasad")),
    "cladding_insulated": dict(
        seed=20261022, body=(226, 228, 226),
        lang=("Insulated Panel Cladding", "Revestimiento de Panel Aislado",
              "Sandwichpaneelverkleidung", "Sandwichpanelfasad")),
    "cladding_composite": dict(
        seed=20261023, body=(176, 180, 184),
        lang=("Composite Panel Cladding", "Revestimiento de Panel Compuesto",
              "Verbundplattenverkleidung", "Kompositpanelfasad")),
}

ORDER = ["cladding_corrugated", "cladding_standingseam", "cladding_insulated",
         "cladding_composite"]


# --------------------------------------------------------------------------------------------
# Drawing
# --------------------------------------------------------------------------------------------

def _clamp(colour):
    return tuple(max(0, min(255, int(round(c)))) for c in colour[:3]) + (255,)


def _shift(colour, d):
    return _clamp([c + d for c in colour[:3]])


def _grid(fn):
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = fn(x, y)
    return img


def corrugated(rng, body):
    """Galvanised corrugated sheet, ribs running plumb. The profile is a sine, so each pitch is
    shaded by the cosine of its phase -- bright on the flank facing the light, dark on the one
    turned away -- over the faint spangle of the zinc coat."""
    spangle = [[rng.uniform(-4, 4) for _ in range(SIZE)] for _ in range(SIZE)]
    # Spangle comes in crystals, not pixels: smear each value into its right-hand neighbour.
    for y in range(SIZE):
        for x in range(SIZE):
            if rng.random() < 0.5:
                spangle[y][(x + 1) % SIZE] = spangle[y][x]

    def shade(x, y):
        phase = (x % CORRUGATION + 0.5) / CORRUGATION * 2 * math.pi
        # At +/-16 the ribs read up close but mipmap away to flat grey a few blocks off; the
        # pitch is right for real corrugated at this scale, so the contrast is what gives.
        return _shift(body, 26 * math.cos(phase) + spangle[y][x])
    return _grid(shade)


def standing_seam(rng, body):
    """Standing seam: flat pans between tall folded seams. The pans oil-can a little -- a slight
    waviness across their width that is what makes a flat dark sheet read as metal rather than as
    paint -- and each seam throws a shadow onto the pan to its right."""
    wave = [rng.uniform(-3, 3) for _ in range(SIZE)]
    profile = {0: 26, 1: -20, 2: -12, 3: -5}

    def shade(x, y):
        offset = x % SEAM_PITCH
        d = profile.get(offset, 0) + wave[x] + rng.uniform(-1.5, 1.5)
        # A shallow stiffening rib at the middle of each pan.
        if offset == SEAM_PITCH // 2 + 2:
            d -= 4
        return _shift(body, d)
    return _grid(shade)


def insulated(rng, body):
    """Insulated metal panels laid horizontally: a tight tongue-and-groove joint between panels,
    and the fine striations the face is rolled with so it does not oil-can."""
    rows = {0: -40, 1: -14, PANEL_H - 1: 6}

    def shade(x, y):
        r = y % PANEL_H
        d = rows.get(r, 0)
        if r not in rows and r % 4 == 2:
            d -= 5
        return _shift(body, d + rng.uniform(-1.5, 1.5))
    return _grid(shade)


def composite(rng, body):
    """Aluminium composite panels in stack bond, each a block wide and half a block tall, with an
    open reveal joint round each. Lit from the upper left, the reveal's shadow falls on its top
    and left. Panels from different sheets differ slightly in sheen, and each is a little brighter
    toward the top."""
    tints = [rng.uniform(-5, 5) for _ in range(SIZE // PANEL_H)]

    def shade(x, y):
        r, c = y % PANEL_H, x
        if r == 0 or c == 0:
            d = -62
        elif r == 1 or c == 1:
            d = -30
        else:
            d = tints[y // PANEL_H] + (PANEL_H / 2 - r) * 0.8
        return _shift(body, d + rng.uniform(-1, 1))
    return _grid(shade)


def top(rng, body):
    """The top of a clad wall: the sheet's cut edge over its girts, flat and a little darker."""
    return _grid(lambda x, y: _shift(body, -12 + rng.uniform(-3, 3)))


DRAW = {"cladding_corrugated": corrugated, "cladding_standingseam": standing_seam,
        "cladding_insulated": insulated, "cladding_composite": composite}


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
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    """(lang key, per-language name) for every block a cladding set registers."""
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
    lines.append("# tab registration, in CsmTabBuildingMaterials, AFTER the siding sets")
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

    tmp = tempfile.mkdtemp(prefix="csm_cladding_")
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
        print("%d generated cladding files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
