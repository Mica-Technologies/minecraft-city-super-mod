#!/usr/bin/env python3
"""Every asset the construction site's logistics props ship: the loads that make a site look
occupied rather than staged.

    python dev-env-utils/scripts/gen_logistics.py
    python dev-env-utils/scripts/gen_logistics.py --check
    python dev-env-utils/scripts/gen_logistics.py --fragments   # lang lines to paste

Nine blocks, all BlockSiteAxialProp (one class by registry name, turned to lie along the
placer's line of sight): pallets of brick, concrete block, drywall and bagged concrete; a lumber
stack on dunnage; insulation rolls; PVC pipe and conduit bundles; and a wire spool.

Each is drawn running along x and turned by the blockstate for z. Round things -- rolls, pipes, a
spool's flanges -- are a square and the same square turned 45 degrees, which is as round as a
block model gets and reads as round at any distance you see a pallet from. Bundles run the full
length of the cell, so two laid end to end are one long bundle.
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
TEX_DIR = sc.TEX_DIR
MODEL_DIR = sc.MODEL_DIR
STATE_DIR = sc.STATE_DIR
TEX_REF = sc.TEX_REF
MODEL_REF = sc.MODEL_REF

# Registry name -> name in each language. Order is creative order.
BLOCKS = {
    "pallet_brick": ("Brick Pallet", "Palé de Ladrillos", "Ziegelpalette", "Tegelpall"),
    "pallet_cmu": ("Concrete Block Pallet", "Palé de Bloques de Hormigón", "Betonsteinpalette",
                   "Betongblockpall"),
    "pallet_drywall": ("Drywall Pallet", "Palé de Placas de Yeso", "Gipskartonpalette",
                       "Gipsskivepall"),
    "pallet_concrete_bags": ("Bagged Concrete Pallet", "Palé de Sacos de Hormigón",
                             "Sackbetonpalette", "Pall med Säckbetong"),
    "lumber_stack": ("Lumber Stack", "Pila de Madera", "Schnittholzstapel", "Virkesstapel"),
    "insulation_rolls": ("Insulation Rolls", "Rollos de Aislamiento", "Dämmstoffrollen",
                         "Isoleringsrullar"),
    "pipe_bundle": ("PVC Pipe Bundle", "Haz de Tubos de PVC", "PVC-Rohrbündel", "PVC-rörbunt"),
    "conduit_bundle": ("Conduit Bundle", "Haz de Conductos Eléctricos",
                       "Elektroinstallationsrohrbündel", "Elrörsbunt"),
    "wire_spool": ("Wire Spool", "Carrete de Cable", "Kabeltrommel", "Kabeltrumma"),
}

# Each block's box, running along x, in sixteenths -- the Java registration uses these.
BOXES = {
    "pallet_brick": (0.5, 0, 0.5, 15.5, 12.6, 15.5),
    "pallet_cmu": (0.5, 0, 0.5, 15.5, 13.5, 15.5),
    "pallet_drywall": (0.5, 0, 0.5, 15.5, 10, 15.5),
    "pallet_concrete_bags": (0.5, 0, 0.5, 15.5, 11.5, 15.5),
    "lumber_stack": (0, 0, 1, 16, 11.5, 15),
    "insulation_rolls": (0, 0, 0, 16, 16, 16),
    "pipe_bundle": (0, 0, 2, 16, 9, 14),
    "conduit_bundle": (0, 0, 4, 16, 6, 12),
    "wire_spool": (1, 0, 1, 15, 15, 15),
}


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

def _shift(colour, d):
    return tuple(max(0, min(255, int(round(c + d)))) for c in colour[:3]) + (255,)


def _canvas(seed, base, amount):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(base, rng.uniform(-amount, amount))
    return img, px, rng


def pallet_wood():
    """Raw pine pallet boards: pale, with grain along x."""
    rng = random.Random(20261401)
    rows = [rng.uniform(-8, 8) for _ in range(16)]
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift((196, 164, 116), rows[y] + rng.uniform(-4, 4))
    return img


def load_brick():
    """A cube of bricks as a pallet carries them: tight rows of small bricks, no mortar, the joints
    showing as light gaps."""
    img, px, rng = _canvas(20261402, (150, 62, 44), 10)
    for y in range(16):
        for x in range(16):
            if y % 2 == 1 or (x + (4 if (y // 2) % 2 else 0)) % 8 == 7:
                px[x, y] = _shift((188, 150, 128), rng.uniform(-6, 6))
    return img


def load_cmu():
    """Concrete blocks stacked dry: grey faces with dark joints, each unit eight pixels by four."""
    img, px, rng = _canvas(20261403, (150, 150, 144), 7)
    for y in range(16):
        for x in range(16):
            if y % 4 == 3 or x % 8 == 7:
                px[x, y] = _shift((96, 96, 92), rng.uniform(-4, 4))
    return img


def load_cmu_top():
    """The top of the stack: each unit's two cores, dark."""
    img, px, rng = _canvas(20261404, (150, 150, 144), 7)
    for y in range(16):
        for x in range(16):
            if y % 8 == 7 or x % 8 == 7:
                px[x, y] = _shift((96, 96, 92), rng.uniform(-4, 4))
            elif 1 <= y % 8 <= 5 and (1 <= x % 8 <= 2 or 4 <= x % 8 <= 5):
                px[x, y] = _shift((64, 64, 62), rng.uniform(-4, 4))
    return img


def drywall_side():
    """Sheet edges: thin white-grey paper faces with the gypsum core between."""
    img, px, rng = _canvas(20261405, (226, 224, 216), 4)
    for y in range(16):
        for x in range(16):
            if y % 2 == 1:
                px[x, y] = _shift((188, 186, 178), rng.uniform(-4, 4))
    return img


def drywall_top():
    """The top sheet's face: off-white paper with the maker's blue print line."""
    img, px, rng = _canvas(20261406, (232, 230, 222), 3)
    for x in range(16):
        px[x, 5] = _shift((80, 112, 170), rng.uniform(-6, 6))
    return img


def bags_side():
    """Bagged concrete, stacked: grey paper bags three pixels high, each with a red band."""
    img, px, rng = _canvas(20261407, (196, 190, 176), 6)
    for y in range(16):
        for x in range(16):
            row = y // 3
            if y % 3 == 2 or (x + (4 if row % 2 else 0)) % 8 == 7:
                px[x, y] = _shift((150, 146, 134), rng.uniform(-4, 4))
            elif y % 3 == 1 and (x + (4 if row % 2 else 0)) % 8 in (2, 3, 4):
                px[x, y] = _shift((178, 46, 40), rng.uniform(-6, 6))
    return img


def bags_top():
    """The top layer of bags, crosswise, each with its printed band."""
    img, px, rng = _canvas(20261408, (200, 194, 180), 6)
    for y in range(16):
        for x in range(16):
            if x % 8 == 7 or y % 4 == 3:
                px[x, y] = _shift((150, 146, 134), rng.uniform(-4, 4))
            elif x % 8 in (3, 4):
                px[x, y] = _shift((178, 46, 40), rng.uniform(-6, 6))
    return img


def lumber_side():
    """A lumber bundle's side: boards two pixels thick, grain along x, dark gaps between."""
    rng = random.Random(20261409)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        tone = rng.uniform(-10, 10)
        for x in range(16):
            gap = y % 2 == 1 and rng.random() < 0.8
            px[x, y] = _shift((112, 86, 58) if gap else (206, 172, 118),
                              (0 if gap else tone) + rng.uniform(-4, 4))
    return img


def lumber_end():
    """The end of the bundle: rows of two-by-four ends, end grain, with gaps."""
    img, px, rng = _canvas(20261410, (214, 180, 126), 8)
    for y in range(16):
        for x in range(16):
            if y % 2 == 1 or x % 4 == 3:
                px[x, y] = _shift((100, 76, 50), rng.uniform(-4, 4))
            elif (x % 4, y % 2) == (1, 0):
                px[x, y] = _shift((180, 140, 92), rng.uniform(-4, 4))
    return img


def insulation_side():
    """Kraft-free pink fibreglass under a clear wrap: pink, fluffy, a streak of sheen."""
    img, px, rng = _canvas(20261411, (232, 150, 164), 12)
    for x in range(16):
        px[x, 3] = _shift((246, 196, 206), rng.uniform(-6, 6))
    return img


def insulation_end():
    """A roll's end: the batt wound into a spiral, drawn as rings."""
    img, px, rng = _canvas(20261412, (232, 150, 164), 8)
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if int(r) % 3 == 0:
                px[x, y] = _shift((196, 112, 128), rng.uniform(-6, 6))
    return img


def pvc_side():
    """White PVC pipe, with the maker's print line."""
    img, px, rng = _canvas(20261413, (232, 234, 232), 3)
    for x in range(16):
        if x % 6 < 4:
            px[x, 7] = _shift((64, 80, 140), rng.uniform(-6, 6))
    return img


def pvc_end():
    """A pipe end: a white ring round the dark bore."""
    img, px, rng = _canvas(20261414, (232, 234, 232), 3)
    for y in range(16):
        for x in range(16):
            if 3 <= x <= 12 and 3 <= y <= 12:
                px[x, y] = _shift((52, 54, 56), rng.uniform(-4, 4))
    return img


def conduit_end():
    """Conduit ends: galvanized rims round dark bores, too small to draw as rings."""
    return _canvas(20261415, (70, 72, 74), 6)[0]


def cable():
    """Black cable wound round the drum: turns across x."""
    img, px, rng = _canvas(20261416, (34, 34, 36), 4)
    for y in range(16):
        for x in range(16):
            if x % 2 == 1:
                px[x, y] = _shift((58, 58, 62), rng.uniform(-4, 4))
    return img


def spool_flange():
    """A spool flange's face: plywood with the arbor hole and bolt heads."""
    img, px, rng = _canvas(20261417, (180, 146, 100), 8)
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if r < 1.6:
                px[x, y] = (30, 26, 22, 255)
            elif 4.5 < r < 5.3 and (x + y) % 5 == 0:
                px[x, y] = _shift((90, 92, 96), rng.uniform(-6, 6))
    return img


TEXTURES = {"pallet_wood": pallet_wood, "load_brick": load_brick, "load_cmu": load_cmu,
            "load_cmu_top": load_cmu_top, "load_drywall_side": drywall_side,
            "load_drywall_top": drywall_top, "load_bags_side": bags_side,
            "load_bags_top": bags_top, "lumber_side": lumber_side, "lumber_end": lumber_end,
            "insulation_side": insulation_side, "insulation_end": insulation_end,
            "pvc_side": pvc_side, "pvc_end": pvc_end, "conduit_end": conduit_end,
            "spool_cable": cable, "spool_flange": spool_flange}


# --------------------------------------------------------------------------------------------
# Geometry -- drawn running along x
# --------------------------------------------------------------------------------------------

box = sc._box


def retex(el, faces):
    """Gives some of an element's faces another texture."""
    for face, tex in faces.items():
        if face in el["faces"]:
            el["faces"][face]["texture"] = tex
    return el


PALLET_TOP = 2.5


def pallet():
    """A pallet: three stringers along x, deck boards across them."""
    els = [box(1, 0, z, 15, 1.5, z + 2, "#wood") for z in (1, 7, 13)]
    for x0, x1 in ((0.5, 3.5), (4.5, 7.5), (8.5, 11.5), (12.5, 15.5)):
        els.append(box(x0, 1.5, 0.5, x1, PALLET_TOP, 15.5, "#wood"))
    return els


def bands(top, x_at=(4, 12), z0=0.9, z1=15.1):
    """Two steel strapping bands over a load, across it."""
    return [box(x - 0.2, PALLET_TOP, z0, x + 0.2, top + 0.1, z1, "#band") for x in x_at]


def palletised(top, side, top_tex, inset=1.0):
    el = retex(box(inset, PALLET_TOP, inset, 16 - inset, top, 16 - inset, side),
               {"up": top_tex})
    return pallet() + [el] + bands(top, z0=inset - 0.1, z1=16 - inset + 0.1)


def lumber():
    els = [box(2, 0, 1, 4, 1.5, 15, "#wood"), box(12, 0, 1, 14, 1.5, 15, "#wood")]
    els.append(retex(box(0, 1.5, 1.5, 16, 11.5, 14.5, "#side"),
                     {"east": "#end", "west": "#end"}))
    els += [box(x - 0.2, 1.5, 1.4, x + 0.2, 11.6, 14.6, "#band") for x in (5, 11)]
    return els


def roll(y, z, s, side="#side", end="#end"):
    """A round thing lying along x about (y, z): a square of half-side s and the same square turned
    45 degrees, which reaches s * 1.41 at its points."""
    a = retex(box(0, y - s, z - s, 16, y + s, z + s, side), {"east": end, "west": end})
    b = retex(box(0, y - s, z - s, 16, y + s, z + s, side), {"east": end, "west": end})
    b["rotation"] = {"origin": [8, y, z], "axis": "x", "angle": 45}
    return [a, b]


def insulation():
    els = []
    for y in (4.1, 12.0):
        for z in (4.1, 11.9):
            els += roll(y, z, 2.9)
    return els


def pipes():
    """White PVC pipes on two timbers, four, three and two high."""
    els = [box(2, 0, 2, 4, 1, 14, "#wood"), box(12, 0, 2, 14, 1, 14, "#wood")]
    d = 2.6
    rows = ((1 + d / 2, [4.1, 6.7, 9.3, 11.9]), (1 + 1.5 * d, [5.4, 8.0, 10.6]),
            (1 + 2.5 * d, [6.7, 9.3]))
    for y, zs in rows:
        for z in zs:
            els.append(retex(box(0, y - d / 2, z - d / 2, 16, y + d / 2, z + d / 2, "#side"),
                             {"east": "#end", "west": "#end"}))
    return els


def conduit():
    """A banded bundle of galvanized conduit, three rows of five."""
    els = []
    d = 1.4
    for i, y in enumerate((0.7, 0.7 + d, 0.7 + 2 * d)):
        for j in range(5):
            z = 5.2 + j * d + (0.7 if i % 2 else 0)
            els.append(retex(box(0, y - d / 2 + 0.05, z - d / 2 + 0.05, 16, y + d / 2 - 0.05,
                                 z + d / 2 - 0.05, "#side"), {"east": "#end", "west": "#end"}))
    els += [box(x - 0.2, 0, 4.5, x + 0.2, 4.9, 12.2, "#band") for x in (4, 12)]
    return els


def spool():
    """A cable reel standing on its flanges, its axis along x."""
    yc, zc = 7.5, 8.0
    els = []
    for x0, x1 in ((1, 2.5), (13.5, 15)):
        for rot in (None, 45):
            el = retex(box(x0, yc - 5.3, zc - 5.3, x1, yc + 5.3, zc + 5.3, "#wood"),
                       {"east": "#flange", "west": "#flange"})
            if rot:
                el["rotation"] = {"origin": [8, yc, zc], "axis": "x", "angle": 45}
            els.append(el)
        els.append(retex(box(x0, yc - 7.4, zc - 3.0, x1, yc + 7.4, zc + 3.0, "#wood"),
                         {"east": "#flange", "west": "#flange"}))
        els.append(retex(box(x0, yc - 3.0, zc - 7.4, x1, yc + 3.0, zc + 7.4, "#wood"),
                         {"east": "#flange", "west": "#flange"}))
    for rot in (None, 45):
        el = box(2.5, yc - 4.2, zc - 4.2, 13.5, yc + 4.2, zc + 4.2, "#cable",
                 faces=("north", "south", "up", "down"))
        if rot:
            el["rotation"] = {"origin": [8, yc, zc], "axis": "x", "angle": 45}
        els.append(el)
    return els


def _t(name):
    return TEX_REF % name


GEOMETRY = {
    "pallet_brick": (lambda: palletised(12.5, "#load", "#load"),
                     {"load": _t("load_brick")}),
    "pallet_cmu": (lambda: palletised(13.5, "#load", "#loadtop"),
                   {"load": _t("load_cmu"), "loadtop": _t("load_cmu_top")}),
    "pallet_drywall": (lambda: palletised(10, "#load", "#loadtop", inset=0.5),
                       {"load": _t("load_drywall_side"), "loadtop": _t("load_drywall_top")}),
    "pallet_concrete_bags": (lambda: palletised(11.5, "#load", "#loadtop"),
                             {"load": _t("load_bags_side"), "loadtop": _t("load_bags_top")}),
    "lumber_stack": (lumber, {"side": _t("lumber_side"), "end": _t("lumber_end")}),
    "insulation_rolls": (insulation, {"side": _t("insulation_side"),
                                      "end": _t("insulation_end")}),
    "pipe_bundle": (pipes, {"side": _t("pvc_side"), "end": _t("pvc_end")}),
    "conduit_bundle": (conduit, {"side": _t("chainlink_post_galv"), "end": _t("conduit_end")}),
    "wire_spool": (spool, {"cable": _t("spool_cable"), "flange": _t("spool_flange")}),
}
COMMON = {"wood": _t("pallet_wood"), "band": _t("form_clamp")}


def models():
    out = {}
    for name, (fn, tex) in GEOMETRY.items():
        textures = dict(COMMON)
        textures.update(tex)
        first = next(iter(tex))
        textures["particle"] = tex[first]
        out[name] = {"parent": "block/block", "textures": textures, "elements": fn()}
    return out


def blockstates():
    out = {}
    for name in BLOCKS:
        ref = MODEL_REF % name
        out[name] = {"variants": {"axis=x": {"model": ref},
                                  "axis=z": {"model": ref, "y": 90},
                                  "inventory": {"model": ref}}}
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
    return [("tile.%s.name" % name, dict(zip(LANGS, names))) for name, names in BLOCKS.items()]


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    return "\n".join(lines)


def check(label, write_fn, roots):
    tmp = tempfile.mkdtemp(prefix="csm_%s_" % label)
    try:
        tmp_roots = {k: os.path.join(tmp, k) for k in roots}
        for path in tmp_roots.values():
            os.makedirs(path, exist_ok=True)
        written = write_fn(tmp_roots["tex"], tmp_roots["model"], tmp_roots["state"])
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
        print("%d generated %s files are up to date" % (len(written), label))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


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
    if args.check:
        return check("logistics", write_all, roots)
    written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR)
    print("Wrote %d logistics files" % len(written))
    return 0


if __name__ == "__main__":
    sys.exit(main())
