#!/usr/bin/env python3
"""Every asset the construction site's formwork, shoring and rebar ship.

    python dev-env-utils/scripts/gen_formwork.py
    python dev-env-utils/scripts/gen_formwork.py --check
    python dev-env-utils/scripts/gen_formwork.py --fragments   # lang and tab lines to paste

Seven blocks: wall and column formwork, a post shore, and rebar -- a slab mat, dowels standing out
of a pour, a tied column cage and a bundle on dunnage. What they have in common is that they are
the concrete half of a building site, the stage between the scaffold and the finished wall, and
that none of them is a full cube.

The element helpers are gen_scaffold's: every face carries explicit UVs fitted into the texture,
because several parts reach past their cell, and an unfitted UV there samples the neighbouring
sprites on the atlas (see _fit in gen_scaffold.py for the two ways that went wrong).

Directional blocks are drawn running along x and turned a quarter by the blockstate for axis z.
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
import gen_scaffold as sc

REPO = sc.REPO
MODULE = sc.MODULE
TEX_DIR = sc.TEX_DIR
MODEL_DIR = sc.MODEL_DIR
STATE_DIR = sc.STATE_DIR
TEX_REF = sc.TEX_REF
MODEL_REF = sc.MODEL_REF

SIZE = 16

# Registry name -> Java class, and the name in each language. Order is creative order.
BLOCKS = {
    "formwork_wall": ("BlockFormworkWall",
                      ("Wall Formwork", "Encofrado de Muro", "Wandschalung", "Väggform")),
    "formwork_column": ("BlockSiteProp",
                        ("Column Formwork", "Encofrado de Columna", "Stützenschalung",
                         "Pelarform")),
    "shore_post": ("BlockPostShore",
                   ("Post Shore", "Puntal", "Deckenstütze", "Stämp")),
    "rebar_mat": ("BlockSiteProp",
                  ("Rebar Mat", "Parrilla de Armadura", "Bewehrungsmatte", "Armeringsnät")),
    "rebar_dowels": ("BlockSiteProp",
                     ("Rebar Dowels", "Esperas de Armadura", "Anschlussbewehrung",
                      "Utstickande Armering")),
    "rebar_cage": ("BlockSiteProp",
                   ("Rebar Column Cage", "Armadura de Columna", "Stützenbewehrungskorb",
                    "Pelararmering")),
    "rebar_bundle": ("BlockRebarBundle",
                     ("Rebar Bundle", "Paquete de Varillas", "Bewehrungsstahlbündel",
                      "Armeringsbunt")),
}


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

def _shift(colour, d):
    return tuple(max(0, min(255, int(round(c + d)))) for c in colour[:3]) + (255,)


def _noise(seed, base, amount, rows=None):
    rng = random.Random(seed)
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    for y in range(SIZE):
        bias = rows[y] if rows else 0
        for x in range(SIZE):
            px[x, y] = _shift(base, bias + rng.uniform(-amount, amount))
    return img


def ply_texture():
    """Film-faced form plywood: the brown phenolic face formwork is actually made of, smooth, with
    only a faint grain showing through the film."""
    rng = random.Random(20261050)
    rows = [rng.uniform(-4, 4) for _ in range(SIZE)]
    return _noise(20261051, (132, 82, 52), 3, rows)


def lumber_texture():
    """Waler timber: rough-sawn softwood, grain along the member."""
    rng = random.Random(20261052)
    rows = [rng.uniform(-9, 9) for _ in range(SIZE)]
    return _noise(20261053, (184, 150, 102), 4, rows)


def concrete_texture():
    """Fresh concrete: darker and a touch greener than cured, with the odd aggregate fleck."""
    img = _noise(20261054, (138, 140, 134), 5)
    px = img.load()
    rng = random.Random(20261055)
    for _ in range(18):
        px[rng.randrange(SIZE), rng.randrange(SIZE)] = _shift((168, 166, 158), rng.uniform(-6, 6))
    return img


def rebar_texture():
    """Rebar: rusted steel with the ribs rolled into it, across the bar."""
    rng = random.Random(20261056)
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            rib = -18 if x % 3 == 0 else 0
            px[x, y] = _shift((120, 70, 46), rib + rng.uniform(-8, 8))
    return img


def cap_texture():
    """Rebar safety cap: the orange mushroom every protruding bar on a real site wears."""
    return _noise(20261057, (236, 118, 32), 5)


def clamp_texture():
    """Column clamps and form-tie cones: painted steel."""
    return _noise(20261058, (70, 74, 80), 4)


TEXTURES = {"form_ply": ply_texture, "form_lumber": lumber_texture,
            "form_concrete": concrete_texture, "rebar": rebar_texture,
            "rebar_cap": cap_texture, "form_clamp": clamp_texture}

TEXTURE_KEYS = {"ply": TEX_REF % "form_ply", "lumber": TEX_REF % "form_lumber",
                "concrete": TEX_REF % "form_concrete", "rebar": TEX_REF % "rebar",
                "cap": TEX_REF % "rebar_cap", "clamp": TEX_REF % "form_clamp",
                "steel": TEX_REF % "scaffold_tube", "plank": TEX_REF % "scaffold_plank"}

box = sc._box


def _model(elements, particle, parent=None):
    textures = dict(TEXTURE_KEYS)
    textures["particle"] = TEXTURE_KEYS[particle]
    m = {"textures": textures, "elements": elements}
    if parent:
        m = {"parent": parent, **m}
    return m


# --------------------------------------------------------------------------------------------
# Geometry
# --------------------------------------------------------------------------------------------

def formwork_wall():
    """A wall form running along x: concrete between two plywood faces, two horizontal walers on
    each face, and the form-tie cones poking out past the walers where the ties pass through.
    The concrete stops a sixteenth below the forms, so its top reads as a fresh pour."""
    els = [
        box(0, 0, 4, 16, 15, 12, "#concrete"),
        box(0, 0, 3, 16, 16, 4, "#ply"),
        box(0, 0, 12, 16, 16, 13, "#ply"),
    ]
    for y in (3, 11):
        els.append(box(0, y, 1.5, 16, y + 2, 3, "#lumber"))
        els.append(box(0, y, 13, 16, y + 2, 14.5, "#lumber"))
        for x in (4, 12):
            els.append(box(x - 0.5, y + 0.5, 0.75, x + 0.5, y + 1.5, 1.5, "#clamp"))
            els.append(box(x - 0.5, y + 0.5, 14.5, x + 0.5, y + 1.5, 15.25, "#clamp"))
    return els


def formwork_column():
    """A square column form: concrete inside four plywood faces, held by two steel clamps. The
    east and west clamp bars stop short of the north and south ones, so no two share a face."""
    els = [
        box(4, 0, 4, 12, 15, 12, "#concrete"),
        box(3, 0, 3, 13, 16, 4, "#ply"),
        box(3, 0, 12, 13, 16, 13, "#ply"),
        box(3, 0, 4, 4, 16, 12, "#ply"),
        box(12, 0, 4, 13, 16, 12, "#ply"),
    ]
    for y in (3, 11):
        els += [
            box(2, y, 2, 14, y + 1, 3, "#clamp"),
            box(2, y, 13, 14, y + 1, 14, "#clamp"),
            box(2, y, 3, 3, y + 1, 13, "#clamp"),
            box(13, y, 3, 14, y + 1, 13, "#clamp"),
        ]
    return els


SHORE_OUTER = (7.25, 8.75)
SHORE_INNER = (7.5, 8.5)


def shore_tube():
    """The outer tube of a post shore, the full height of a cell, for a shore that carries on up."""
    a, b = SHORE_OUTER
    return [box(a, 0, a, b, 16, b, "#steel")]


def shore_top():
    """The top of a shore: the outer tube up to the adjusting collar and its pin, the inner tube
    above, and the U-head the timber beam sits in."""
    a, b = SHORE_OUTER
    c, d = SHORE_INNER
    return [
        box(a, 0, a, b, 8, b, "#steel"),
        box(6.75, 8, 6.75, 9.25, 9, 9.25, "#clamp"),
        box(5.75, 8.3, 7.8, 10.25, 8.7, 8.2, "#clamp"),
        box(c, 9, c, d, 15, d, "#steel"),
        box(5, 15, 6, 11, 15.5, 10, "#clamp"),
        box(5, 15.5, 6, 11, 16, 6.5, "#clamp"),
        box(5, 15.5, 9.5, 11, 16, 10, "#clamp"),
    ]


def shore_base():
    """The base plate at the foot of a shore stack."""
    return [box(5, 0, 5, 11, 0.5, 11, "#clamp")]


BAR = 0.5
GRID = (2, 6, 10, 14)


def rebar_mat():
    """A slab mat: bars both ways at a four-pixel pitch, one layer on the other, at chair height.
    The pitch divides the block, so a floor of mats is one continuous grid."""
    h = BAR / 2
    els = [box(0, 1.5, z - h, 16, 2, z + h, "#rebar") for z in GRID]
    els += [box(x - h, 2, 0, x + h, 2.5, 16, "#rebar") for x in GRID]
    return els


def rebar_dowels():
    """Four dowels standing out of a pour, each with its orange safety cap."""
    h = BAR / 2
    els = []
    for x in (4, 12):
        for z in (4, 12):
            els.append(box(x - h, 0, z - h, x + h, 13.5, z + h, "#rebar"))
            els.append(box(x - 0.8, 13.2, z - 0.8, x + 0.8, 14.4, z + 0.8, "#cap"))
    return els


def rebar_cage():
    """A tied column cage: eight bars, the full height so cages stack into one, with a stirrup
    tied round them every four pixels. Each stirrup's east and west legs stop short of its north
    and south ones, so no two share a face."""
    h = BAR / 2
    els = []
    for x, z in ((4, 4), (4, 12), (12, 4), (12, 12), (8, 4), (8, 12), (4, 8), (12, 8)):
        els.append(box(x - h, 0, z - h, x + h, 16, z + h, "#rebar"))
    t = 0.3
    lo, hi = 4 - h - t, 12 + h + t
    for y in (2, 6, 10, 14):
        els += [
            box(lo, y, lo, hi, y + t, lo + t, "#rebar"),
            box(lo, y, hi - t, hi, y + t, hi, "#rebar"),
            box(lo, y, lo + t, lo + t, y + t, hi - t, "#rebar"),
            box(hi - t, y, lo + t, hi, y + t, hi - t, "#rebar"),
        ]
    return els


def rebar_bundle():
    """A bundle of bars lying along x on two timbers, three rows high, with two wire bands. The
    bars run the full length of the cell, so bundles laid end to end read as one long bundle."""
    s, p = 0.9, 1.0
    els = [box(2, 0, 2, 4, 1.5, 14, "#plank"), box(12, 0, 2, 14, 1.5, 14, "#plank")]
    rows = ((1.5, [5.5, 6.5, 7.5, 8.5, 9.5]), (1.5 + s, [6.0, 7.0, 8.0, 9.0]),
            (1.5 + 2 * s, [6.5, 7.5, 8.5]))
    for y, zs in rows:
        for z in zs:
            els.append(box(0, y, z, 16, y + s, z + s, "#rebar"))
    for x in (5, 11):
        els.append(box(x, 1.4, 5.4, x + 0.4, 1.5 + 3 * s + 0.1, 10.5, "#clamp"))
    return els


GEOMETRY = {"formwork_wall": (formwork_wall, "ply"), "formwork_column": (formwork_column, "ply"),
            "rebar_mat": (rebar_mat, "rebar"), "rebar_dowels": (rebar_dowels, "rebar"),
            "rebar_cage": (rebar_cage, "rebar"), "rebar_bundle": (rebar_bundle, "rebar")}


def models():
    out = {}
    for name, (fn, particle) in GEOMETRY.items():
        out[name] = _model(fn(), particle, parent="block/block")
    out["shore_post_tube"] = _model(shore_tube(), "steel")
    out["shore_post_top"] = _model(shore_top(), "steel")
    out["shore_post_base"] = _model(shore_base(), "steel")
    out["shore_post_inventory"] = _model(shore_top() + shore_base(), "steel", parent="block/block")
    return out


# --------------------------------------------------------------------------------------------
# Blockstates
# --------------------------------------------------------------------------------------------

def _ref(model):
    return MODEL_REF % model


def blockstates():
    out = {}
    for name in ("formwork_column", "rebar_mat", "rebar_dowels", "rebar_cage"):
        out[name] = {"variants": {"normal": {"model": _ref(name)},
                                  "inventory": {"model": _ref(name)}}}
    for name in ("formwork_wall", "rebar_bundle"):
        out[name] = {"variants": {"axis=x": {"model": _ref(name)},
                                  "axis=z": {"model": _ref(name), "y": 90},
                                  "inventory": {"model": _ref(name)}}}
    out["shore_post"] = {
        "variants": {"inventory": {"model": _ref("shore_post_inventory")}},
        "multipart": [
            {"when": {"up": "true"}, "apply": {"model": _ref("shore_post_tube")}},
            {"when": {"up": "false"}, "apply": {"model": _ref("shore_post_top")}},
            {"when": {"down": "false"}, "apply": {"model": _ref("shore_post_base")}},
        ],
    }
    return out


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    for name, fn in sorted(TEXTURES.items()):
        fn().save(os.path.join(tex_dir, name + ".png"))
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
    return [("tile.%s.name" % name, dict(zip(LANGS, names)))
            for name, (_, names) in BLOCKS.items()]


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    lines.append("# tab registration, in CsmTabConstructionSite, after the scaffold add-ons")
    for name, (cls, names) in BLOCKS.items():
        lines.append("    // %s: %s" % (names[0], cls))
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
        print("Wrote %d formwork files" % len(written))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_formwork_")
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
        print("%d generated formwork files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
