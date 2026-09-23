#!/usr/bin/env python3
"""
gen_trees.py -- every asset of the Parks & Greenery tree kit.

Trees are built block by block from log and leaves blocks, as vanilla trees are. The log is drawn
in Java (TreeLogGeometry / TreeLogBakedModel) from what surrounds it, so what this script writes
for a log is only what the game needs from files:

  * textures/blocks/parks/bark_<wood>.png   one bark per wood, tileable both ways
  * models/block/parks/log_<width>.json     a straight log of that width: the item's model, and
                                            the placeholder the baked model replaces in the world
  * blockstates/tree_log_<wood>_<width>.json axis=x/y/z and inventory, all that one model with
                                            the wood's bark
  * lang lines (tile.tree_log_*.name) in all four languages, kept in place by key

The woods and widths must match TreeWood and TreeLogWidth, in the same order; the tab lines
(--fragments) are the other place the catalogue is spelled out.

Usage:
    python gen_trees.py              # write everything
    python gen_trees.py --check      # regenerate into a temp dir; fail if the tree has drifted
    python gen_trees.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import argparse
import json
import math
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "modules", "parks", "src", "main", "resources", "assets", "csm")
LOCALES = ["en_us", "de_de", "es_es", "sv_se"]

# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
# id -> (Java constant, names in en/de/es/sv, bark recipe). Order = TreeWood order.
WOODS = [
    ("liveoak", "LIVE_OAK", ("Live Oak", "Virginia-Eiche", "roble de Virginia", "virginiaek"),
     "furrowed_dark"),
    ("elm", "ELM", ("Elm", "Ulme", "olmo", "alm"), "furrowed_grey"),
    ("plane", "PLANE", ("London Plane", "Platane", "plátano de sombra", "platan"), "mottled"),
    ("honeylocust", "HONEY_LOCUST",
     ("Honey Locust", "Gleditschie", "acacia de tres espinas", "korstörne"), "plated"),
    ("cypress", "CYPRESS",
     ("Italian Cypress", "Säulenzypresse", "ciprés italiano", "pelarcypress"), "fibrous"),
    ("ginkgo", "GINKGO", ("Ginkgo", "Ginkgo", "ginkgo", "ginkgo"), "fissured"),
    ("palm", "PALM", ("Palm", "Palme", "palmera", "palm"), "ringed"),
]

# id -> (Java constant, pixels across, name patterns en/de/es/sv). Order = TreeLogWidth order.
WIDTHS = [
    ("twig", "TWIG", 2, ("{w} Twig", "Zweig ({w})", "Rama de {w}", "Kvist ({w})")),
    ("thin", "THIN", 4, ("Thin {w} Log", "Dünner Stamm ({w})", "Tronco delgado de {w}",
                         "Tunn stam ({w})")),
    ("medium", "MEDIUM", 8, ("{w} Log", "Stamm ({w})", "Tronco de {w}", "Stam ({w})")),
    ("thick", "THICK", 12, ("Thick {w} Log", "Dicker Stamm ({w})", "Tronco grueso de {w}",
                            "Tjock stam ({w})")),
    ("full", "FULL", 16, ("Full {w} Log", "Voller Stamm ({w})", "Tronco completo de {w}",
                          "Hel stam ({w})")),
]


def cap_first(s):
    return s[:1].upper() + s[1:]


def log_name(wood, width):
    return "tree_log_%s_%s" % (wood, width)


# ------------------------------------------------------------------------------------------
# Bark
# ------------------------------------------------------------------------------------------
SIZE = 16


def _noise(rng, cells):
    """Tileable value noise on a cells x cells lattice, eased, wrapped. SIZE x SIZE in -1..1."""
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
            bot = lattice[y1][x0] * (1 - wx) + lattice[y1][x1] * wx
            out[y][x] = top * (1 - wy) + bot * wy
    return out


def _furrows(rng, count, dark, jitter=1):
    """Vertical furrows that wander a pixel side to side and wrap top to bottom."""
    field = [[0.0] * SIZE for _ in range(SIZE)]
    for _ in range(count):
        x = rng.randrange(SIZE)
        for y in range(SIZE):
            field[y][x % SIZE] -= dark
            if rng.random() < 0.3:
                x += rng.choice((-jitter, jitter))
        # keep the wrap seamless: the furrow ends where it began
    return field


def bark(recipe, seed):
    rng = random.Random(seed)
    if recipe == "furrowed_dark":
        base, spread = (92, 80, 68), 10
        field = _furrows(rng, 5, 34)
    elif recipe == "furrowed_grey":
        base, spread = (112, 104, 94), 10
        field = _furrows(rng, 6, 28)
    elif recipe == "plated":
        base, spread = (84, 72, 62), 8
        field = _furrows(rng, 4, 30)
        for y in range(0, SIZE, 5):
            for x in range(SIZE):
                field[(y + (x // 4) % 2 * 2) % SIZE][x] -= 16
    elif recipe == "fibrous":
        base, spread = (126, 78, 56), 6
        field = _furrows(rng, 7, 20, jitter=0)
    elif recipe == "fissured":
        base, spread = (128, 122, 112), 8
        field = _furrows(rng, 4, 22)
    elif recipe == "ringed":
        base, spread = (150, 128, 96), 6
        field = [[0.0] * SIZE for _ in range(SIZE)]
        for y in range(SIZE):
            if y % 4 == 0:
                for x in range(SIZE):
                    field[y][x] -= 28
            elif y % 4 == 1:
                for x in range(SIZE):
                    field[y][x] += 10
    elif recipe == "mottled":
        base, spread = (168, 160, 132), 0
        field = [[0.0] * SIZE for _ in range(SIZE)]
    else:
        raise ValueError(recipe)
    grain = _noise(rng, 4)
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    if recipe == "mottled":
        # London plane: flaking patches of cream, olive and grey.
        patches = [(186, 180, 150), (150, 146, 104), (128, 124, 116), (196, 188, 160)]
        patch = _noise(rng, 3)
        patch2 = _noise(rng, 5)
        for y in range(SIZE):
            for x in range(SIZE):
                v = patch[y][x] * 0.7 + patch2[y][x] * 0.5
                i = 0 if v < -0.35 else 1 if v < 0 else 2 if v < 0.35 else 3
                c = patches[i]
                px[x, y] = tuple(max(0, min(255, int(ch + rng.uniform(-6, 6)))) for ch in c) + (255,)
        return img
    for y in range(SIZE):
        for x in range(SIZE):
            v = field[y][x] + grain[y][x] * spread + rng.uniform(-5, 5)
            px[x, y] = tuple(max(0, min(255, int(round(c + v)))) for c in base) + (255,)
    return img


# ------------------------------------------------------------------------------------------
# Models and blockstates
# ------------------------------------------------------------------------------------------
def log_model(pixels):
    lo, hi = 8 - pixels / 2, 8 + pixels / 2
    side = {"uv": [lo, 0, hi, 16], "texture": "#bark"}
    end = {"uv": [lo, lo, hi, hi], "texture": "#bark"}
    return {
        "parent": "block/block",
        "textures": {"particle": "#bark"},
        "elements": [{
            "from": [lo, 0, lo], "to": [hi, 16, hi],
            "faces": {"north": side, "south": side, "east": side, "west": side,
                      "up": end, "down": end},
        }],
    }


def log_blockstate(wood, width):
    texture = "csm:blocks/parks/bark_%s" % wood
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "csm:parks/log_%s" % width,
            "textures": {"bark": texture, "particle": texture},
        },
        "variants": {
            # The world variants are replaced by TreeLogBakedModel at bake time (TreeModels);
            # the model here is what an item, and a missing-model fallback, show.
            "axis": {"x": {}, "y": {}, "z": {}},
            "inventory": [{}],
        },
    }


# ------------------------------------------------------------------------------------------
# Lang
# ------------------------------------------------------------------------------------------
def lang_entries():
    """{locale: {key: value}} for every block this script owns."""
    out = {loc: {} for loc in LOCALES}
    for wood, _, names, _ in WOODS:
        for width, _, _, patterns in WIDTHS:
            key = "tile.%s.name" % log_name(wood, width)
            for i, loc in enumerate(LOCALES):
                w = names[i]
                value = patterns[i].format(w=w)
                out[loc][key] = cap_first(value)
    return out


def write_lang(lang_dir, entries):
    """Keeps each owned key's line in place (or appends it), leaving every other line alone."""
    for loc, values in entries.items():
        path = os.path.join(lang_dir, loc + ".lang")
        lines = []
        if os.path.exists(path):
            with open(path, encoding="utf-8", newline="") as fh:
                lines = fh.read().replace("\r\n", "\n").split("\n")
            if lines and lines[-1] == "":
                lines.pop()
        seen = set()
        for i, line in enumerate(lines):
            key = line.split("=", 1)[0]
            if key in values:
                lines[i] = "%s=%s" % (key, values[key])
                seen.add(key)
        for key, value in values.items():
            if key not in seen:
                lines.append("%s=%s" % (key, value))
        os.makedirs(lang_dir, exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            fh.write("\n".join(lines) + "\n")


# ------------------------------------------------------------------------------------------
# Output
# ------------------------------------------------------------------------------------------
def dump(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


def generate(assets):
    """Writes everything under an assets/csm root; returns the relative paths written."""
    written = []
    for i, (wood, _, _, recipe) in enumerate(WOODS):
        rel = "textures/blocks/parks/bark_%s.png" % wood
        os.makedirs(os.path.dirname(os.path.join(assets, rel)), exist_ok=True)
        bark(recipe, 20260922 + i).save(os.path.join(assets, rel))
        written.append(rel)
    for width, _, pixels, _ in WIDTHS:
        rel = "models/block/parks/log_%s.json" % width
        dump(os.path.join(assets, rel), log_model(pixels))
        written.append(rel)
    for wood, _, _, _ in WOODS:
        for width, _, _, _ in WIDTHS:
            rel = "blockstates/%s.json" % log_name(wood, width)
            dump(os.path.join(assets, rel), log_blockstate(wood, width))
            written.append(rel)
    write_lang(os.path.join(assets, "lang"), lang_entries())
    written += ["lang/%s.lang" % loc for loc in LOCALES]
    return written


def fragments():
    lines = []
    for wood, wconst, _, _ in WOODS:
        for width, dconst, _, _ in WIDTHS:
            lines.append('    initTabBlock(new BlockTreeLog("%s", TreeWood.%s, TreeLogWidth.%s));'
                         % (log_name(wood, width), wconst, dconst))
    return "\n".join(lines)


def same_file(a, b):
    if not os.path.exists(b):
        return False
    if a.endswith(".png"):
        return (Image.open(a).convert("RGBA").tobytes()
                == Image.open(b).convert("RGBA").tobytes())
    return layout.same_generated_text(a, b)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    args = ap.parse_args()
    if args.fragments:
        print(fragments())
        return 0
    if not args.check:
        written = generate(ASSETS)
        print("wrote %d files under %s" % (len(written), ASSETS))
        return 0
    tmp = tempfile.mkdtemp(prefix="trees_")
    try:
        # Lang files hold other lines too: start the temp copy from the tree's own.
        shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
        written = generate(tmp)
        stale = [rel for rel in written
                 if not same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("tree kit assets are up to date")
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
