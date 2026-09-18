#!/usr/bin/env python3
"""Every asset the concrete masonry (CMU) sets ship.

Four finishes, each a block/stairs/slab/fence set: the textures, the five blockstates and the six
model files per set, from one catalogue. ``--check`` writes nothing and fails on drift.

    python dev-env-utils/scripts/gen_cmu.py
    python dev-env-utils/scripts/gen_cmu.py --check
    python dev-env-utils/scripts/gen_cmu.py --fragments   # lang and tab lines to paste

COURSING. A CMU is nominally 8 x 8 x 16 inches. At sixteen pixels to a roughly one-metre block that
is about 6.5 px long and 3.3 px high, which tiles badly. The pattern is drawn instead at 8 px long
and 4 px high -- two units across a block and four courses up it -- so it divides the block exactly
and repeats seamlessly in both directions. Alternate courses step half a unit, which is the running
bond every masonry wall is laid in and the single thing that stops the texture reading as tile.
"""

import argparse
import filecmp
import io
import json
import os
import random
import shutil
import sys
import tempfile

from PIL import Image, ImageDraw

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODULE = os.path.join(REPO, "modules", "building", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "buildingmaterials", "cmu")
MODEL_DIR = os.path.join(MODULE, "models", "block", "buildingmaterials")
STATE_DIR = os.path.join(MODULE, "blockstates")

TEX_REF = "csm:blocks/buildingmaterials/cmu/%s"
MODEL_REF = "csm:buildingmaterials/%s"

SIZE = 16
UNIT_W = 8      # a masonry unit's length, in texture pixels
COURSE_H = 4    # a course's height
JOINT = (0, 0, 0, 0)  # the mortar joint is drawn as a colour per finish, not as a constant


# --------------------------------------------------------------------------------------------
# Finishes
# --------------------------------------------------------------------------------------------
#
# body      the face colour
# grain     how far each pixel may wander from the body colour (0 = flat)
# mortar    the joint, always paler and flatter than the unit
# speckle   (colour, chance) for an aggregate fleck, or None
# relief    how strongly the top edge of a unit catches light -- what makes split face look rough
# seed      an explicit, stable seed. Python's hash() is salted per process and cannot be used
#           for anything a --check has to reproduce

FINISHES = {
    "cmu_standard": dict(
        seed=20260925, body=(150, 148, 143), grain=10, mortar=(176, 174, 169), speckle=None, relief=6,
        lang=("Concrete Block", "Bloque de Hormigón", "Betonstein", "Betongblock")),
    "cmu_splitface": dict(
        seed=20260926, body=(142, 138, 130), grain=26, mortar=(172, 169, 163),
        speckle=((110, 106, 99), 0.18), relief=16,
        lang=("Split-Face Block", "Bloque de Cara Partida", "Spaltstein",
              "Spräckt Betongblock")),
    "cmu_groundface": dict(
        seed=20260927, body=(158, 152, 142), grain=8, mortar=(178, 174, 167),
        speckle=((196, 190, 178), 0.22), relief=4,
        lang=("Ground-Face Block", "Bloque Pulido", "Geschliffener Betonstein",
              "Slipat Betongblock")),
    "cmu_glazed": dict(
        seed=20260928, body=(214, 206, 186), grain=3, mortar=(186, 180, 166), speckle=None, relief=10,
        lang=("Glazed Block", "Bloque Vidriado", "Glasierter Betonstein",
              "Glaserat Betongblock")),
}

ORDER = ["cmu_standard", "cmu_splitface", "cmu_groundface", "cmu_glazed"]


def _shade(rng, colour, amount):
    if amount <= 0:
        return tuple(colour) + (255,)
    d = rng.randint(-amount, amount)
    return tuple(max(0, min(255, c + d)) for c in colour) + (255,)


def face_texture(name):
    """The side of a wall: units in running bond, with a mortar joint round each."""
    spec = FINISHES[name]
    rng = random.Random(spec["seed"])
    img = Image.new("RGBA", (SIZE, SIZE), tuple(spec["mortar"]) + (255,))
    px = img.load()

    for course in range(SIZE // COURSE_H):
        y0 = course * COURSE_H
        # Running bond: every other course steps half a unit, so no two head joints line up.
        offset = (UNIT_W // 2) if course % 2 else 0
        for unit in range(-1, SIZE // UNIT_W + 1):
            x0 = unit * UNIT_W + offset
            for y in range(y0 + 1, y0 + COURSE_H):
                for x in range(x0 + 1, x0 + UNIT_W):
                    if not (0 <= x < SIZE and 0 <= y < SIZE):
                        continue
                    colour = _shade(rng, spec["body"], spec["grain"])
                    if spec["speckle"] and rng.random() < spec["speckle"][1]:
                        colour = tuple(spec["speckle"][0]) + (255,)
                    # The top course of each unit catches the light; on split face this is the
                    # fractured edge and is what makes it read as rough rather than as noise.
                    if y == y0 + 1:
                        colour = _shade(rng, [min(255, c + spec["relief"])
                                              for c in colour[:3]], spec["grain"] // 2)
                    px[x, y] = colour
    return img


def top_texture(name):
    """The top of a wall: the units seen end on, each with its two cores."""
    spec = FINISHES[name]
    rng = random.Random(spec["seed"] + 7)
    img = Image.new("RGBA", (SIZE, SIZE), tuple(spec["mortar"]) + (255,))
    draw = ImageDraw.Draw(img)
    core = tuple(max(0, c - 34) for c in spec["body"]) + (255,)
    for unit in range(SIZE // UNIT_W):
        x0 = unit * UNIT_W
        for y in range(1, SIZE - 1):
            for x in range(x0 + 1, x0 + UNIT_W):
                img.putpixel((x, y), _shade(rng, spec["body"], spec["grain"]))
        # Two hollow cores per unit, which is what a concrete block actually is.
        for cx in (x0 + 2, x0 + 5):
            draw.rectangle([cx, 5, cx + 1, 10], fill=core)
    return img


# --------------------------------------------------------------------------------------------
# Blockstates and models
# --------------------------------------------------------------------------------------------

def _faces(name):
    side, top = TEX_REF % name, TEX_REF % (name + "_top")
    return side, top


def blockstates(name):
    side, top = _faces(name)
    three = {"bottom": top, "top": top, "side": side}
    return {
        name: {
            "forge_marker": 1,
            "defaults": {"model": "cube_bottom_top",
                         "textures": {"top": top, "bottom": top, "side": side,
                                      "particle": side}},
            "variants": {"inventory": [{}], "normal": [{}]},
        },
        name + "_slab_double": {
            "forge_marker": 1,
            "defaults": {"model": "cube_bottom_top",
                         "textures": {"top": top, "bottom": top, "side": side,
                                      "particle": side}},
            "variants": {"variant": {"default": {}}, "inventory": [{}], "normal": [{}]},
        },
        name + "_slab": {
            "forge_marker": 1,
            "defaults": {"model": "half_slab", "textures": dict(three)},
            "variants": {
                "half": {"bottom": {}, "top": {"model": MODEL_REF % (name + "_slab_top")}},
                "variant": {"default": {}},
                "inventory": [{}], "normal": [{}],
            },
        },
        name + "_stairs": {
            "forge_marker": 1,
            "defaults": {"model": "stairs", "textures": dict(three)},
            "variants": {
                "facing": {"east": {}, "north": {"y": 270}, "south": {"y": 90},
                           "west": {"y": 180}},
                "half": {"bottom": {}, "top": {}},
                "shape": {
                    "inner_left": {"model": MODEL_REF % (name + "_stairs_inner")},
                    "inner_right": {"model": MODEL_REF % (name + "_stairs_inner")},
                    "outer_left": {"model": MODEL_REF % (name + "_stairs_outer")},
                    "outer_right": {"model": MODEL_REF % (name + "_stairs_outer")},
                    "straight": {},
                },
                "inventory": [{}], "normal": [{}],
            },
        },
        name + "_fence": {
            "variants": {"inventory": {"model": MODEL_REF % (name + "_fence_inventory")}},
            "multipart": [
                {"apply": {"model": MODEL_REF % (name + "_fence_post")}},
                {"when": {"north": "true"},
                 "apply": {"model": MODEL_REF % (name + "_fence"), "uvlock": True}},
                {"when": {"south": "true"},
                 "apply": {"model": MODEL_REF % (name + "_fence"), "y": 180, "uvlock": True}},
                {"when": {"west": "true"},
                 "apply": {"model": MODEL_REF % (name + "_fence"), "y": 270, "uvlock": True}},
                {"when": {"east": "true"},
                 "apply": {"model": MODEL_REF % (name + "_fence"), "y": 90, "uvlock": True}},
            ],
        },
    }


def models(name):
    side, top = _faces(name)
    three = {"bottom": top, "top": top, "side": side}
    fence = {"texture": side, "particle": side}
    return {
        name + "_fence": {"parent": "block/fence_side", "textures": dict(fence)},
        name + "_fence_inventory": {"parent": "block/fence_inventory", "textures": dict(fence)},
        name + "_fence_post": {"parent": "block/fence_post", "textures": dict(fence)},
        name + "_slab_top": {"parent": "block/upper_slab", "textures": dict(three)},
        name + "_stairs_inner": {"parent": "block/inner_stairs", "textures": dict(three)},
        name + "_stairs_outer": {"parent": "block/outer_stairs", "textures": dict(three)},
    }


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def _write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    for name in ORDER:
        face_texture(name).save(os.path.join(tex_dir, name + ".png"))
        top_texture(name).save(os.path.join(tex_dir, name + "_top.png"))
        written += [("tex", name + ".png"), ("tex", name + "_top.png")]
        for state, body in sorted(blockstates(name).items()):
            _write_json(os.path.join(state_dir, state + ".json"), body)
            written.append(("state", state + ".json"))
        for model, body in sorted(models(name).items()):
            _write_json(os.path.join(model_dir, model + ".json"), body)
            written.append(("model", model + ".json"))
    return written


LANGS = ("en_us", "es_es", "de_de", "sv_se")
# The variants the set base class generates, and what each is called after the set's own name.
VARIANTS = [("", ""), ("_stairs", " Stairs"), ("_slab", " Slab"),
            ("_slab_double", " Double Slab"), ("_fence", " Fence")]
VARIANT_WORDS = {
    "en_us": {"_stairs": "Stairs", "_slab": "Slab", "_slab_double": "Double Slab",
              "_fence": "Fence"},
    "es_es": {"_stairs": "Escaleras", "_slab": "Losa", "_slab_double": "Losa Doble",
              "_fence": "Valla"},
    "de_de": {"_stairs": "Treppe", "_slab": "Stufe", "_slab_double": "Doppelstufe",
              "_fence": "Zaun"},
    "sv_se": {"_stairs": "Trappa", "_slab": "Platta", "_slab_double": "Dubbelplatta",
              "_fence": "Staket"},
}


def lang_entries():
    """(lang key, per-language name) for every block a CMU set registers."""
    out = []
    for name in ORDER:
        base = dict(zip(LANGS, FINISHES[name]["lang"]))
        for suffix, _ in VARIANTS:
            key = "tile.%s%s.name" % (name, suffix)
            names = {}
            for lang in LANGS:
                names[lang] = (base[lang] if not suffix
                               else "%s %s" % (base[lang], VARIANT_WORDS[lang][suffix]))
            out.append((key, names))
    return out


def class_name(name):
    return "BlockSet" + "".join(p.capitalize() for p in name.split("_"))


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    lines.append("# tab registration, in CsmTabBuildingMaterials, AFTER the metal sets")
    lines.append("")
    for name in ORDER:
        lines.append("    initTabBlock(%s.class,"
                     "\n        fmlPreInitializationEvent); // %s Set (Block, Fence, Slab, Stairs)"
                     % (class_name(name), FINISHES[name]["lang"][0]))
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
        print("Wrote %d files for %d set(s)" % (len(written), len(ORDER)))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_cmu_")
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
        print("%d generated CMU files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
