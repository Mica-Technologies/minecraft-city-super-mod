#!/usr/bin/env python3
"""Every asset the construction site's earthworks ship.

    python dev-env-utils/scripts/gen_earthworks.py
    python dev-env-utils/scripts/gen_earthworks.py --check
    python dev-env-utils/scripts/gen_earthworks.py --fragments   # lang lines to paste

Five blocks:

- the trench plate (BlockTrenchPlate), the steel road plate laid over an open trench. Plates side
  by side draw as one plate: the raised edge bar runs only along a side with no plate beside it;
- the trench box (BlockTrenchBox), two steel side panels held apart by spreader pipes, drawn
  running along x and turned by the blockstate. Boxes end to end and stacked are one box; the top
  rail and lifting lugs go on the top course only;
- soil, gravel and sand stockpiles (BlockStockpile), eight layers like vanilla snow, so a heap is
  shaped by hand.

The element helpers are gen_scaffold's, with fitted UVs.
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
    "trench_plate": ("Trench Plate", "Placa de Zanja", "Grabenabdeckplatte", "Körplåt"),
    "trench_box": ("Trench Box", "Caja de Entibación", "Grabenverbaubox", "Schaktlåda"),
    "stockpile_soil": ("Soil Stockpile", "Acopio de Tierra", "Erdhaufen", "Jordhög"),
    "stockpile_gravel": ("Gravel Stockpile", "Acopio de Grava", "Kieshaufen", "Grushög"),
    "stockpile_sand": ("Sand Stockpile", "Acopio de Arena", "Sandhaufen", "Sandhög"),
}
STOCKPILES = ("soil", "gravel", "sand")


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

def _shift(colour, d):
    return tuple(max(0, min(255, int(round(c + d)))) for c in colour[:3]) + (255,)


def plate_texture():
    """A trench plate: dark mill-scaled steel, polished lighter in streaks where tyres run over it
    and flecked with rust."""
    rng = random.Random(20261301)
    streak = [rng.uniform(-4, 10) if rng.random() < 0.4 else 0 for _ in range(16)]
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift((88, 90, 92), streak[y] + rng.uniform(-6, 6))
    # A few dull rust flecks only: bright ones repeat block to block as an obvious pattern.
    for _ in range(4):
        px[rng.randrange(16), rng.randrange(16)] = _shift((104, 86, 72), rng.uniform(-6, 6))
    return img


def panel_texture():
    """A trench box panel: painted steel, grey-blue, with a weld seam across it every half block
    and the odd scrape through the paint."""
    rng = random.Random(20261302)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        seam = -22 if y % 8 == 0 else 0
        for x in range(16):
            px[x, y] = _shift((78, 90, 102), seam + rng.uniform(-5, 5))
    for _ in range(6):
        px[rng.randrange(16), rng.randrange(16)] = _shift((128, 104, 84), rng.uniform(-8, 8))
    return img


def spreader_texture():
    """A spreader: a safety-orange steel pipe, lit down its middle."""
    rng = random.Random(20261303)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = abs(y - 7.5) / 7.5
            px[x, y] = _shift((214, 104, 36), 16 - 44 * d * d + rng.uniform(-4, 4))
    return img


def soil_texture():
    """Excavated soil: darker and redder than grass-top dirt, with clods and the odd stone."""
    rng = random.Random(20261311)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift((104, 76, 54), rng.uniform(-12, 12))
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = _shift((74, 54, 38), rng.uniform(-6, 6))
        px[(x + 1) % 16, y] = _shift((82, 60, 42), rng.uniform(-6, 6))
    for _ in range(5):
        px[rng.randrange(16), rng.randrange(16)] = _shift((132, 128, 120), rng.uniform(-10, 10))
    return img


def gravel_texture():
    """Crushed-stone gravel: angular grey chips, each a two-by-two of light over dark, which reads
    as crushed aggregate rather than vanilla's rounded pebbles."""
    rng = random.Random(20261312)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift((96, 96, 94), rng.uniform(-8, 8))
    for _ in range(34):
        x, y = rng.randrange(16), rng.randrange(16)
        base = rng.uniform(-14, 34)
        px[x, y] = _shift((140, 138, 134), base + 12)
        px[(x + 1) % 16, y] = _shift((140, 138, 134), base)
        px[x, (y + 1) % 16] = _shift((140, 138, 134), base - 26)
    return img


def sand_texture():
    """Washed construction sand: warmer and a little more orange than vanilla sand, fine-grained."""
    rng = random.Random(20261313)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift((204, 172, 120), rng.uniform(-10, 10))
    for _ in range(12):
        px[rng.randrange(16), rng.randrange(16)] = _shift((170, 138, 92), rng.uniform(-6, 6))
    return img


TEXTURES = {"trench_plate": plate_texture, "trench_panel": panel_texture,
            "trench_spreader": spreader_texture, "stockpile_soil": soil_texture,
            "stockpile_gravel": gravel_texture, "stockpile_sand": sand_texture}


# --------------------------------------------------------------------------------------------
# Geometry
# --------------------------------------------------------------------------------------------

box = sc._box
FACES = sc.FACES
PLATE_TOP = 1.0       # the plate is a sixteenth thick
EDGE_TOP = 1.25       # the edge bar stands a quarter pixel proud of it
EDGE_WIDTH = 1.0


def plate_body():
    """The plate's top and underside only; its sides come with the edge bars, and only where a
    side is open."""
    return [box(0, 0, 0, 16, PLATE_TOP, 16, "#plate", faces=("up", "down"))]


def plate_edge():
    """The edge bar along the north side, with the plate's side face under it."""
    return [box(0, 0, 0, 16, EDGE_TOP, EDGE_WIDTH, "#plate",
                faces=("north", "south", "east", "west", "up"))]


PANEL = 1.5           # side panel thickness


def trench_box_body():
    """A box running along x: the two side panels, a stiffener rib inside each, and a spreader
    pipe across at the rib with a collar at each end."""
    els = [box(0, 0, 0, 16, 16, PANEL, "#panel"),
           box(0, 0, 16 - PANEL, 16, 16, 16, "#panel"),
           box(7, 0, PANEL, 9, 16, PANEL + 0.6, "#panel"),
           box(7, 0, 16 - PANEL - 0.6, 9, 16, 16 - PANEL, "#panel")]
    y = 12.0
    els.append(box(7.2, y - 0.8, PANEL + 0.6, 8.8, y + 0.8, 16 - PANEL - 0.6, "#spreader"))
    for z0, z1 in ((PANEL + 0.6, PANEL + 1.6), (16 - PANEL - 1.6, 16 - PANEL - 0.6)):
        els.append(box(6.8, y - 1.2, z0, 9.2, y + 1.2, z1, "#spreader"))
    return els


def trench_box_top():
    """The top course's rail along each panel, and a lifting lug on each."""
    els = []
    for z0, z1 in ((-0.25, PANEL + 0.25), (16 - PANEL - 0.25, 16.25)):
        els.append(box(0, 16, z0, 16, 16.75, z1, "#panel"))
        els.append(box(6.5, 16.75, z0 + 0.5, 9.5, 18.25, z1 - 0.5, "#panel"))
    return els


def stockpile(layers):
    """A pile {layers} eighths of a block high. Faces that lie on the cell's boundary cull against
    a solid neighbour; the top of a partial pile never does."""
    el = box(0, 0, 0, 16, 2 * layers, 16, "#all")
    for face in FACES:
        if face != "up" or layers == 8:
            el["faces"][face]["cullface"] = face
    return [el]


# --------------------------------------------------------------------------------------------
# Models and blockstates
# --------------------------------------------------------------------------------------------

def _model(elements, textures, particle, parent=None):
    t = dict(textures)
    t["particle"] = textures[particle]
    m = {"textures": t, "elements": elements}
    if parent:
        m = {"parent": parent, **m}
    return m


PLATE_TEX = {"plate": TEX_REF % "trench_plate"}
BOX_TEX = {"panel": TEX_REF % "trench_panel", "spreader": TEX_REF % "trench_spreader"}


def _turned(elements, quarter):
    """The elements turned {quarter} quarter-turns clockwise seen from above, for an inventory
    model that needs every edge bar at once (a blockstate does this with its y rotation)."""
    out = []
    for el in elements:
        f, t = el["from"], el["to"]
        x0, z0, x1, z1 = f[0], f[2], t[0], t[2]
        for _ in range(quarter):
            x0, z0, x1, z1 = 16 - z1, x0, 16 - z0, x1
        faces = tuple(el["faces"].keys())
        turned = {"north": "east", "east": "south", "south": "west", "west": "north",
                  "up": "up", "down": "down"}
        for _ in range(quarter):
            faces = tuple(turned[x] for x in faces)
        out.append(box(x0, f[1], z0, x1, t[1], z1, next(iter(el["faces"].values()))["texture"],
                       faces=faces))
    return out


def models():
    out = {"trench_plate_body": _model(plate_body(), PLATE_TEX, "plate"),
           "trench_plate_edge": _model(plate_edge(), PLATE_TEX, "plate")}
    edges = []
    for q in range(4):
        edges += _turned(plate_edge(), q)
    out["trench_plate_inventory"] = _model(plate_body() + edges, PLATE_TEX, "plate",
                                           parent="block/block")
    out["trench_box_body"] = _model(trench_box_body(), BOX_TEX, "panel")
    out["trench_box_top"] = _model(trench_box_top(), BOX_TEX, "panel")
    out["trench_box_inventory"] = _model(trench_box_body() + trench_box_top(), BOX_TEX, "panel",
                                         parent="block/block")
    for mat in STOCKPILES:
        tex = {"all": TEX_REF % ("stockpile_" + mat)}
        for k in range(1, 9):
            out["stockpile_%s_%d" % (mat, k)] = _model(stockpile(k), tex, "all",
                                                       parent="block/block")
    return out


def _ref(model):
    return MODEL_REF % model


# The edge bar model is drawn on the north side; the y rotation that turns it onto each side.
EDGE_SIDES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def blockstates():
    out = {}
    parts = [{"apply": {"model": _ref("trench_plate_body")}}]
    for side, rot in EDGE_SIDES:
        apply = {"model": _ref("trench_plate_edge")}
        if rot:
            apply["y"] = rot
        parts.append({"when": {side: "false"}, "apply": apply})
    out["trench_plate"] = {"variants": {"inventory": {"model": _ref("trench_plate_inventory")}},
                           "multipart": parts}
    out["trench_box"] = {
        "variants": {"inventory": {"model": _ref("trench_box_inventory")}},
        "multipart": [
            {"when": {"axis": "x"}, "apply": {"model": _ref("trench_box_body")}},
            {"when": {"axis": "z"}, "apply": {"model": _ref("trench_box_body"), "y": 90}},
            {"when": {"axis": "x", "up": "false"}, "apply": {"model": _ref("trench_box_top")}},
            {"when": {"axis": "z", "up": "false"},
             "apply": {"model": _ref("trench_box_top"), "y": 90}},
        ]}
    for mat in STOCKPILES:
        variants = {"layers=%d" % k: {"model": _ref("stockpile_%s_%d" % (mat, k))}
                    for k in range(1, 9)}
        variants["inventory"] = {"model": _ref("stockpile_%s_4" % mat)}
        out["stockpile_" + mat] = {"variants": variants}
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
        print("Wrote %d earthworks files" % len(written))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_earthworks_")
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
        print("%d generated earthworks files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
