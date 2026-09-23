#!/usr/bin/env python3
"""
gen_trees.py -- every asset of the Parks & Greenery tree kit.

Trees are built block by block from log and leaves blocks, as vanilla trees are. Logs and leaves
are drawn in Java (TreeLogGeometry, TreeLeavesGeometry and their baked models) from what
surrounds them, so what this script writes is only what the game needs from files:

  * textures/blocks/parks/bark_<wood>.png     one bark per wood, tileable both ways
  * textures/blocks/parks/leaves_<leaf>.png   one leaf-cluster sprite per leaves block, drawn to
                                              its leaf type (broad, large, fan, airy, needle)
  * models/block/parks/log_<width>.json       a straight log of that width: the item's model, and
                                              the placeholder the baked model replaces in the world
  * models/block/parks/leaves_<leaf>.json     a leaves cube with the cluster: the item's model and
                                              the world placeholder
  * textures/blocks/parks/palm_crown_<style>.png  a palm crown's 2x2 sheet: live frond, dead frond,
                                              boot (TreePalmGeometry), plus a _icon for the item
  * textures/blocks/parks/<moss>[_tip].png     the hanging moss strands and a curtain's ragged tip
  * blockstates for every log, leaves, crown and moss block
  * textures/items/parks/tree_planting_tool.png and its item model
  * lang lines (tile.tree_log_* / tile.tree_leaves_*) in all four languages, kept in place by key

The woods, widths and leaves must match TreeWood, TreeLogWidth and the tab registrations, in the
same order (--fragments prints the tab lines).

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
    ("jacaranda", "JACARANDA", ("Jacaranda", "Jacaranda", "jacarandá", "jakaranda"),
     "fissured_brown"),
    ("pepper", "PEPPER", ("Pepper Tree", "Pfefferbaum", "pimentero", "pepparträd"), "gnarled"),
    ("poplar", "POPLAR", ("Lombardy Poplar", "Pyramidenpappel", "chopo lombardo",
                          "pyramidpoppel"), "furrowed_grey"),
    ("sweetgum", "SWEETGUM", ("Sweetgum", "Amberbaum", "liquidámbar", "ambraträd"), "corky"),
    ("hornbeam", "HORNBEAM", ("Hornbeam", "Hainbuche", "carpe", "avenbok"), "smooth_grey"),
    ("gum", "GUM", ("Lemon-scented Gum", "Zitroneneukalyptus", "eucalipto limón",
                    "citroneukalyptus"), "white_smooth"),
    ("willow", "WILLOW", ("Weeping Willow", "Trauerweide", "sauce llorón", "tårpil"),
     "furrowed_deep"),
    ("linden", "LINDEN", ("Linden", "Linde", "tilo", "lind"), "smooth_grey"),
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

LEAF_NAMES = ("{w} Leaves", "Laub ({w})", "Hojas de {w}", "Löv ({w})")
NEEDLE_NAMES = ("{w} Foliage", "Nadeln ({w})", "Follaje de {w}", "Barr ({w})")

BLOSSOM_NAMES = ("{w} Blossom", "Blüten ({w})", "Flores de {w}", "Blommor ({w})")
CLIPPED_NAMES = ("Clipped {w} Leaves", "Geschnittenes Laub ({w})", "Hojas recortadas de {w}",
                 "Klippt löv ({w})")
ARBORVITAE = ("Arborvitae", "Lebensbaum", "tuya", "tuja")
AUTUMN_NAMES = ("Autumn {w} Leaves", "Herbstlaub ({w})", "Hojas otoñales de {w}",
                "Höstlöv ({w})")

# Leaves: (id, TreeLeafType constant, species names en/de/es/sv, texture style, palette light to
# dark, name patterns). Order = tab order. A season is a separate block whose sprite is drawn
# with its summer sibling's seed (SEASON_OF), so the leaves are the same shapes in a new colour.
LEAVES = [
    ("liveoak", "BROADLEAF", WOODS[0][2], "broad",
     [(96, 124, 58), (76, 104, 46), (58, 84, 38), (44, 66, 30)], LEAF_NAMES),
    ("elm", "BROADLEAF", WOODS[1][2], "broad",
     [(112, 150, 64), (90, 128, 52), (70, 106, 42), (52, 84, 34)], LEAF_NAMES),
    ("plane", "BROADLEAF", WOODS[2][2], "broad_large",
     [(120, 154, 70), (98, 134, 58), (78, 112, 48), (60, 90, 38)], LEAF_NAMES),
    ("honeylocust", "AIRY", WOODS[3][2], "airy",
     [(168, 184, 84), (142, 164, 70), (116, 140, 58), (92, 116, 46)], LEAF_NAMES),
    ("ginkgo", "BROADLEAF", WOODS[5][2], "fan",
     [(142, 178, 82), (118, 158, 68), (96, 136, 56), (76, 112, 46)], LEAF_NAMES),
    ("cypress", "NEEDLE", WOODS[4][2], "needle",
     [(70, 100, 66), (56, 84, 54), (42, 68, 44), (32, 54, 36)], NEEDLE_NAMES),
    ("elm_autumn", "BROADLEAF", WOODS[1][2], "broad",
     [(222, 196, 86), (198, 168, 62), (168, 138, 48), (130, 104, 38)], AUTUMN_NAMES),
    ("plane_autumn", "BROADLEAF", WOODS[2][2], "broad_large",
     [(206, 170, 88), (182, 140, 68), (152, 112, 52), (118, 86, 40)], AUTUMN_NAMES),
    ("honeylocust_autumn", "AIRY", WOODS[3][2], "airy",
     [(238, 208, 92), (216, 182, 70), (188, 152, 56), (152, 120, 44)], AUTUMN_NAMES),
    ("ginkgo_autumn", "BROADLEAF", WOODS[5][2], "fan",
     [(248, 222, 88), (234, 198, 60), (208, 170, 46), (172, 138, 36)], AUTUMN_NAMES),
    ("jacaranda", "AIRY", WOODS[7][2], "airy",
     [(126, 170, 84), (104, 150, 70), (84, 128, 58), (66, 106, 46)], LEAF_NAMES),
    ("jacaranda_blossom", "AIRY", WOODS[7][2], "blossom",
     [(178, 150, 222), (150, 120, 204), (124, 96, 182), (96, 120, 70)], BLOSSOM_NAMES),
    ("pepper", "WEEPING", WOODS[8][2], "airy",
     [(142, 170, 92), (120, 150, 76), (98, 128, 62), (78, 106, 50)], LEAF_NAMES),
    ("poplar", "BROADLEAF", WOODS[9][2], "broad",
     [(120, 164, 70), (98, 144, 58), (78, 122, 48), (60, 100, 38)], LEAF_NAMES),
    ("poplar_autumn", "BROADLEAF", WOODS[9][2], "broad",
     [(240, 206, 76), (222, 182, 56), (196, 156, 44), (160, 124, 36)], AUTUMN_NAMES),
    ("sweetgum", "BROADLEAF", WOODS[10][2], "broad_large",
     [(98, 146, 64), (80, 126, 54), (64, 106, 44), (50, 88, 36)], LEAF_NAMES),
    ("sweetgum_autumn", "BROADLEAF", WOODS[10][2], "broad_large",
     [(214, 70, 52), (180, 46, 58), (134, 36, 70), (96, 30, 60)], AUTUMN_NAMES),
    ("hornbeam", "BROADLEAF", WOODS[11][2], "broad",
     [(110, 150, 66), (90, 130, 54), (72, 110, 44), (56, 90, 36)], LEAF_NAMES),
    ("gum", "AIRY", WOODS[12][2], "airy",
     [(150, 170, 140), (126, 150, 120), (104, 130, 102), (84, 110, 86)], LEAF_NAMES),
    ("willow", "WEEPING", WOODS[13][2], "willow",
     [(170, 190, 92), (146, 170, 76), (122, 150, 62), (100, 128, 50)], LEAF_NAMES),
    ("arborvitae", "NEEDLE", ARBORVITAE, "needle",
     [(104, 150, 64), (86, 130, 54), (68, 110, 44), (54, 92, 36)], NEEDLE_NAMES),
    ("linden", "BROADLEAF", WOODS[14][2], "broad",
     [(114, 156, 70), (94, 136, 58), (76, 116, 48), (60, 96, 38)], LEAF_NAMES),
    ("linden_clipped", "CLIPPED", WOODS[14][2], "clipped",
     [(100, 144, 62), (84, 126, 52), (68, 108, 44), (54, 90, 36)], CLIPPED_NAMES),
]
SEASON_OF = {"elm_autumn": "elm", "plane_autumn": "plane", "honeylocust_autumn": "honeylocust",
             "ginkgo_autumn": "ginkgo", "poplar_autumn": "poplar",
             "sweetgum_autumn": "sweetgum"}

# Palm crowns: (id, TreeLeafType constant, sheet, names en/de/es/sv). Order = tab order.
PALMS = [
    ("palm_fan", "PALM_FAN", "fan",
     ("Fan Palm Crown", "Fächerpalmen-Krone", "Copa de palmera de abanico", "Solfjäderspalmkrona")),
    ("palm_fan_skirt", "PALM_FAN_SKIRT", "fan",
     ("Fan Palm Crown with Skirt", "Fächerpalmen-Krone mit Trockenwedeln",
      "Copa de palmera de abanico con faldón", "Solfjäderspalmkrona med kjol")),
    ("palm_feather", "PALM_FEATHER", "feather",
     ("Feather Palm Crown", "Fiederpalmen-Krone", "Copa de palmera de pluma", "Fjäderpalmkrona")),
]
PALM_SHEETS = ["fan", "feather"]

# Tree Planting Tool presets: TreePreset id -> names en/de/es/sv. Order = TreePreset order.
PRESETS = [
    ("liveoak", ("Southern Live Oak", "Virginia-Eiche", "Roble de Virginia", "Virginiaek")),
    ("elm", ("American Elm", "Amerikanische Ulme", "Olmo americano", "Amerikansk alm")),
    ("plane", ("London Plane", "Ahornblättrige Platane", "Plátano de sombra", "Londonplatan")),
    ("honeylocust", ("Honey Locust", "Gleditschie", "Acacia de tres espinas", "Korstörne")),
    ("cypress", ("Italian Cypress", "Säulenzypresse", "Ciprés italiano", "Pelarcypress")),
    ("ginkgo", ("Ginkgo", "Ginkgo", "Ginkgo", "Ginkgo")),
    ("fanpalm", ("Mexican Fan Palm", "Mexikanische Washingtonpalme", "Palmera mexicana de abanico",
                 "Mexikansk solfjäderspalm")),
    ("leaningpalm", ("Leaning Feather Palm", "Geneigte Fiederpalme",
                     "Palmera de pluma inclinada", "Lutande fjäderpalm")),
    ("lollipopplane", ("Clipped Ball-Head Plane", "Kugelplatane", "Plátano de copa esférica",
                       "Klotformad platan")),
    ("jacaranda", ("Jacaranda", "Jacaranda", "Jacarandá", "Jakaranda")),
    ("peppertree", ("California Pepper Tree", "Kalifornischer Pfefferbaum",
                    "Pimentero californiano", "Kaliforniskt pepparträd")),
    ("coastliveoak", ("Coast Live Oak", "Kalifornische Eiche", "Encino de la costa",
                      "Kustek")),
    ("weepingwillow", ("Weeping Willow", "Trauerweide", "Sauce llorón", "Tårpil")),
    ("poplar", ("Lombardy Poplar", "Pyramidenpappel", "Chopo lombardo", "Pyramidpoppel")),
    ("sweetgum", ("Slender Sweetgum", "Säulen-Amberbaum", "Liquidámbar columnar",
                  "Pelarambraträd")),
    ("hornbeam", ("Columnar Hornbeam", "Säulen-Hainbuche", "Carpe columnar",
                  "Pelaravenbok")),
    ("queenpalm", ("Queen Palm", "Königinpalme", "Palmera reina", "Drottningpalm")),
    ("lemongum", ("Lemon-scented Gum", "Zitroneneukalyptus", "Eucalipto limón",
                  "Citroneukalyptus")),
    ("arborvitae", ("Emerald Arborvitae", "Smaragd-Lebensbaum", "Tuya esmeralda",
                    "Smaragdtuja")),
    ("pleachedlinden", ("Pleached Linden", "Spalierlinde", "Tilo en espaldera",
                        "Spaljerad lind")),
    ("pollardedplane", ("Pollarded Plane", "Kopfplatane", "Plátano desmochado",
                        "Hamlad platan")),
]

# The tool's own lines: key -> en/de/es/sv.
TOOL_LANG = {
    "item.tree_planting_tool.name": (
        "Tree Planting Tool", "Baumpflanzwerkzeug", "Herramienta para plantar árboles",
        "Trädplanteringsverktyg"),
    "csm.parks.planting.mode": (
        "Planting: %s", "Pflanzen: %s", "Plantar: %s", "Planterar: %s"),
    "csm.parks.planting.blocked": (
        "Can't plant %s here: %s blocks in the way",
        "%s kann hier nicht gepflanzt werden: %s Blöcke im Weg",
        "No se puede plantar %s aquí: %s bloques estorban",
        "Kan inte plantera %s här: %s block i vägen"),
    "csm.parks.planting.tooltip.use": (
        "Right-click a block to plant a tree, leaning the way you face",
        "Rechtsklick auf einen Block pflanzt einen Baum, der sich in Blickrichtung neigt",
        "Clic derecho en un bloque para plantar un árbol inclinado hacia donde miras",
        "Högerklicka på ett block för att plantera ett träd som lutar åt det håll du tittar"),
    "csm.parks.planting.tooltip.cycle": (
        "Sneak + right-click to change species", "Schleichen + Rechtsklick wechselt die Art",
        "Agáchate + clic derecho para cambiar de especie", "Smyg + högerklicka för att byta art"),
    "csm.parks.planting.tooltip.current": (
        "Species: %s", "Art: %s", "Especie: %s", "Art: %s"),
}

# Hanging moss: (id, names en/de/es/sv).
MOSSES = [
    ("spanish_moss", ("Spanish Moss", "Spanisches Moos", "Musgo español", "Spansk mossa"), None),
    ("willow_strands", ("Willow Strands", "Hängende Weidenruten", "Ramas colgantes de sauce",
                        "Hängande pilgrenar"),
     [(186, 204, 100), (160, 184, 82), (134, 162, 66), (108, 138, 52)]),
]


def cap_first(s):
    return s[:1].upper() + s[1:]


def log_name(wood, width):
    return "tree_log_%s_%s" % (wood, width)


def leaves_name(leaf_id):
    return "tree_leaves_%s" % leaf_id


def crown_name(palm_id):
    return "tree_crown_%s" % palm_id


# ------------------------------------------------------------------------------------------
# Bark
# ------------------------------------------------------------------------------------------
SIZE = 16


def _noise(rng, cells, size=SIZE):
    """Tileable value noise on a cells x cells lattice, eased, wrapped. size x size in -1..1."""
    lattice = [[rng.uniform(-1, 1) for _ in range(cells)] for _ in range(cells)]
    step = size / cells
    out = [[0.0] * size for _ in range(size)]
    for y in range(size):
        gy, fy = divmod(y / step, 1)
        wy = (1 - math.cos(fy * math.pi)) / 2
        y0, y1 = int(gy) % cells, (int(gy) + 1) % cells
        for x in range(size):
            gx, fx = divmod(x / step, 1)
            wx = (1 - math.cos(fx * math.pi)) / 2
            x0, x1 = int(gx) % cells, (int(gx) + 1) % cells
            top = lattice[y0][x0] * (1 - wx) + lattice[y0][x1] * wx
            bot = lattice[y1][x0] * (1 - wx) + lattice[y1][x1] * wx
            out[y][x] = top * (1 - wy) + bot * wy
    return out


def _furrows(rng, count, dark, jitter=1):
    """Vertical furrows that wander a pixel side to side."""
    field = [[0.0] * SIZE for _ in range(SIZE)]
    for _ in range(count):
        x = rng.randrange(SIZE)
        for y in range(SIZE):
            field[y][x % SIZE] -= dark
            if rng.random() < 0.3:
                x += rng.choice((-jitter, jitter))
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
    elif recipe == "fissured_brown":
        base, spread = (116, 104, 92), 8
        field = _furrows(rng, 6, 18)
    elif recipe == "gnarled":
        base, spread = (122, 88, 66), 12
        field = _furrows(rng, 6, 30, jitter=2)
    elif recipe == "corky":
        base, spread = (104, 92, 80), 10
        field = _furrows(rng, 5, 34)
        for y in range(0, SIZE, 3):
            for x in range(SIZE):
                field[(y + (x // 3) % 2) % SIZE][x] -= 12
    elif recipe == "smooth_grey":
        base, spread = (140, 138, 132), 6
        field = _furrows(rng, 3, 10, jitter=0)
    elif recipe == "furrowed_deep":
        base, spread = (98, 90, 80), 10
        field = _furrows(rng, 7, 36)
    elif recipe in ("mottled", "white_smooth"):
        base, spread = (168, 160, 132), 0
        field = [[0.0] * SIZE for _ in range(SIZE)]
    else:
        raise ValueError(recipe)
    grain = _noise(rng, 4)
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    if recipe in ("mottled", "white_smooth"):
        # London plane: flaking patches of cream, olive and grey. Lemon-scented gum: powdery
        # white with pink and grey where the old bark has just shed.
        patches = ([(186, 180, 150), (150, 146, 104), (128, 124, 116), (196, 188, 160)]
                   if recipe == "mottled" else
                   [(222, 218, 210), (236, 232, 226), (206, 196, 196), (228, 222, 214)])
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
# Leaf clusters
# ------------------------------------------------------------------------------------------
LEAF_SIZE = 32


def _cluster_mask(rng, narrow=False):
    """A lumpy outline, so a card's edge is foliage rather than a square."""
    mask = [[False] * LEAF_SIZE for _ in range(LEAF_SIZE)]
    lobes = [(rng.uniform(9, 23), rng.uniform(9, 23), rng.uniform(7, 11)) for _ in range(6)]
    for y in range(LEAF_SIZE):
        for x in range(LEAF_SIZE):
            xx = 16 + (x - 16) * (1.9 if narrow else 1.0)
            if any((xx - cx) ** 2 + (y - cy) ** 2 < r * r for cx, cy, r in lobes):
                mask[y][x] = True
    return mask


def leaf_cluster(style, palette, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (LEAF_SIZE, LEAF_SIZE), (0, 0, 0, 0))
    px = img.load()
    mask = _cluster_mask(rng, narrow=(style == "needle"))

    def put(x, y, colour):
        if 0 <= x < LEAF_SIZE and 0 <= y < LEAF_SIZE and mask[y][x]:
            px[x, y] = tuple(colour) + (255,)

    if style in ("broad", "broad_large"):
        big = style == "broad_large"
        for _ in range(95 if big else 140):
            cx, cy = rng.uniform(0, LEAF_SIZE), rng.uniform(0, LEAF_SIZE)
            a = rng.uniform(0, math.pi)
            length = rng.uniform(3.0, 4.5) if big else rng.uniform(2.0, 3.4)
            width = length * 0.55
            shade = rng.randrange(len(palette))
            for t in range(-int(length * 2), int(length * 2) + 1):
                for w in range(-int(width * 2), int(width * 2) + 1):
                    u, v = t / 2.0, w / 2.0
                    if (u / length) ** 2 + (v / width) ** 2 <= 1:
                        x = int(round(cx + u * math.cos(a) - v * math.sin(a)))
                        y = int(round(cy + u * math.sin(a) + v * math.cos(a)))
                        edge = (u / length) ** 2 + (v / width) ** 2 > 0.6
                        put(x, y, palette[min(len(palette) - 1, shade + (1 if edge else 0))])
    elif style == "fan":
        for _ in range(90):
            cx, cy = rng.uniform(0, LEAF_SIZE), rng.uniform(0, LEAF_SIZE)
            a = rng.uniform(0, 2 * math.pi)
            shade = rng.randrange(len(palette) - 1)
            for r in range(0, 4):
                for k in range(-r, r + 1):
                    ang = a + k * 0.35 / max(1, r)
                    put(int(round(cx + r * math.cos(ang))), int(round(cy + r * math.sin(ang))),
                        palette[shade + (1 if r == 3 else 0)])
    elif style == "airy":
        for _ in range(18):
            x, y = rng.uniform(4, 28), rng.uniform(4, 28)
            a = rng.uniform(0, math.pi)
            for step in range(16):
                x += math.cos(a)
                y += math.sin(a)
                put(int(x), int(y), palette[-1])
                if step % 2 == 0 or rng.random() < 0.5:
                    for side in (-1, 1):
                        lx = x + math.cos(a + side * 1.2) * 1.6
                        ly = y + math.sin(a + side * 1.2) * 1.6
                        put(int(round(lx)), int(round(ly)), palette[rng.randrange(3)])
    elif style == "blossom":
        # Jacaranda in flower: panicles of violet bells over a little fern-green.
        for _ in range(60):
            x, y = rng.randrange(LEAF_SIZE), rng.randrange(LEAF_SIZE)
            put(x, y, palette[3])
        for _ in range(70):
            cx, cy = rng.uniform(0, LEAF_SIZE), rng.uniform(0, LEAF_SIZE)
            shade = rng.randrange(3)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1), (1, 1)):
                if rng.random() < 0.8:
                    put(int(cx) + dx, int(cy) + dy, palette[min(2, shade + (dx + dy) % 2)])
    elif style == "willow":
        # Long narrow leaves on hanging strands.
        for _ in range(26):
            x = rng.uniform(1, 31)
            for y in range(rng.randrange(0, 8), LEAF_SIZE):
                if rng.random() < 0.85:
                    put(int(x), y, palette[rng.randrange(len(palette))])
                if rng.random() < 0.2:
                    x += rng.choice((-1, 1)) * 0.5
    elif style == "clipped":
        # Clipped dense: the whole card is leaf, no gaps, for a flat topiary face.
        for y in range(LEAF_SIZE):
            for x in range(LEAF_SIZE):
                px[x, y] = tuple(palette[rng.randrange(len(palette))]) + (255,)
        for _ in range(120):
            cx, cy = rng.randrange(LEAF_SIZE), rng.randrange(LEAF_SIZE)
            px[cx, cy] = tuple(palette[0]) + (255,)
    elif style == "needle":
        for _ in range(420):
            x, y = rng.randrange(LEAF_SIZE), rng.randrange(LEAF_SIZE)
            shade = rng.randrange(len(palette))
            for d in range(3):
                put(x, y + d, palette[min(len(palette) - 1, shade + (1 if d == 2 else 0))])
    else:
        raise ValueError(style)
    return img


# ------------------------------------------------------------------------------------------
# Palm crowns: a 64 px sheet of four 32 px cells (TreePalmGeometry's regions)
# ------------------------------------------------------------------------------------------
FROND_GREEN = [(118, 150, 70), (96, 128, 56), (76, 106, 46), (58, 84, 36)]
FROND_FEATHER = [(124, 156, 72), (100, 134, 58), (80, 112, 48), (70, 92, 40)]
FROND_DEAD = [(196, 170, 118), (172, 146, 96), (146, 120, 76), (118, 94, 60)]
STALK = (104, 110, 60)


def _fan_frond(px, ox, oy, palette, rng, ragged):
    """A palmate fan on a short stalk: stalk at the cell's bottom centre, fan opening upward."""
    cx, cy = ox + 16, oy + 19
    stalk = palette[2] if ragged else STALK
    for y in range(oy + 19, oy + 32):
        for dx in (0, 1):
            px[cx - 1 + dx, y] = stalk + (255,)
    segments = 15
    for y in range(oy, oy + 32):
        for x in range(ox, ox + 32):
            dx, dy = x + 0.5 - cx, cy - (y + 0.5)
            r = math.hypot(dx, dy)
            if r > 15.5 or r < 1:
                continue
            ang = math.atan2(dx, dy)  # 0 = straight up
            if abs(ang) > math.radians(82):
                continue
            seg = (ang + math.radians(82)) / math.radians(164) * segments
            frac = seg - int(seg)
            # Split tips: the outer part of each segment is cut down its middle.
            if r > 10.5 and abs(frac - 0.5) < 0.12:
                continue
            reach = 15.5
            if ragged and (int(seg) * 7) % 3 == 0:
                reach -= 3.5
            if abs(frac - 0.5) > 0.38:
                reach -= 1.5
            if r > reach:
                continue
            shade = 1 if abs(frac - 0.5) < 0.3 else 2
            if r < 4:
                shade = 2
            if frac < 0.06 or frac > 0.94:
                shade = 3
            if rng.random() < 0.15:
                shade += 1
            elif rng.random() < 0.15:
                shade -= 1
            px[x, y] = palette[max(0, min(3, shade))] + (255,)


def _feather_frond(px, ox, oy, palette, rng):
    """A pinnate frond: a midrib bottom to top, leaflets angled toward the tip."""
    cx = ox + 16
    for y in range(oy, oy + 32):
        px[cx, y] = palette[3] + (255,)
    for y in range(oy + 30, oy + 1, -2):
        t = (oy + 31 - y) / 31.0
        length = 15 * math.sin(math.pi * min(1.0, t * 1.1 + 0.08)) ** 0.7
        for side in (-1, 1):
            shade = rng.randrange(3)
            for k in range(1, int(length) + 1):
                x = cx + side * k
                yy = y - int(k * 0.45) + (1 if k > length * 0.7 else 0)
                if ox <= x < ox + 32 and oy <= yy < oy + 32:
                    px[x, yy] = palette[shade] + (255,)
                    if k < length * 0.6 and oy <= yy + 1 < oy + 32 and rng.random() < 0.5:
                        px[x, yy + 1] = palette[min(3, shade + 1)] + (255,)


def _boot(px, ox, oy, rng):
    """Woven frond bases: a lattice of fibre over brown and olive."""
    for y in range(oy, oy + 32):
        for x in range(ox, ox + 32):
            u, v = x - ox, y - oy
            if (u + v) % 6 < 2 or (u - v) % 6 < 2:
                c = (88, 70, 48)
            else:
                c = (126, 112, 70) if ((u // 6) + (v // 6)) % 2 else (110, 96, 60)
            px[x, y] = tuple(max(0, min(255, ch + rng.randint(-8, 8))) for ch in c) + (255,)


def palm_sheet(style, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    if style == "fan":
        _fan_frond(px, 0, 0, FROND_GREEN, rng, ragged=False)
    else:
        _feather_frond(px, 0, 0, FROND_FEATHER, rng)
    _fan_frond(px, 32, 0, FROND_DEAD, rng, ragged=True)
    _boot(px, 0, 32, rng)
    return img


def palm_icon(sheet):
    """The item icon: the live frond cell, halved."""
    return sheet.crop((0, 0, 32, 32)).resize((16, 16), Image.NEAREST)


# ------------------------------------------------------------------------------------------
# Hanging moss
# ------------------------------------------------------------------------------------------
MOSS_GREY = [(172, 180, 158), (148, 158, 136), (124, 136, 114), (100, 112, 92)]


def moss(tip, seed, palette=None):
    """Strands hanging the full height (a curtain block), or ending raggedly (the tip)."""
    MOSS = palette or MOSS_GREY
    rng = random.Random(seed)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    for _ in range(9):
        x = rng.randrange(SIZE)
        end = rng.randint(6, 15) if tip else SIZE
        for y in range(end):
            px[x % SIZE, y] = MOSS[rng.randrange(len(MOSS))] + (255,)
            if rng.random() < 0.35:
                px[(x + rng.choice((-1, 1))) % SIZE, y] = MOSS[rng.randrange(1, len(MOSS))] + (255,)
            if rng.random() < 0.25:
                x += rng.choice((-1, 1))
    return img


# ------------------------------------------------------------------------------------------
# The Tree Planting Tool's icon: a spade and a sapling
# ------------------------------------------------------------------------------------------
def planting_tool_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    handle = [(122, 88, 52), (98, 70, 40)]
    for i in range(8):  # handle, top right down toward the blade
        x, y = 13 - i, 2 + i
        px[x, y] = handle[0] + (255,)
        px[x + 1, y] = handle[1] + (255,)
    px[14, 1] = (70, 70, 70, 255)
    px[13, 1] = (70, 70, 70, 255)
    px[14, 2] = (70, 70, 70, 255)
    blade = [(196, 200, 204), (160, 164, 170), (120, 124, 130)]
    for y in range(9, 15):
        for x in range(1, 8):
            # A rounded spade blade, point at the bottom left.
            u, v = x - 4.5, y - 11
            if u * u / 9 + v * v / 11 <= 1 and x + (14 - y) >= 3:
                shade = 0 if x < 4 else 1 if x < 6 else 2
                px[x, y] = blade[shade] + (255,)
    leaf = [(96, 150, 58), (70, 120, 44)]
    for y in range(3, 9):
        px[3, y] = (110, 84, 52, 255)
    for x, y, c in [(2, 3, 0), (1, 2, 1), (4, 3, 0), (5, 2, 1), (2, 5, 1), (4, 5, 0), (5, 4, 0),
                    (1, 4, 0), (3, 2, 0), (3, 1, 1)]:
        px[x, y] = leaf[c] + (255,)
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


def leaves_model(leaf_id):
    return {"parent": "block/leaves", "textures": {"all": "csm:blocks/parks/leaves_%s" % leaf_id}}


def leaves_blockstate(leaf_id):
    model = "csm:parks/leaves_%s" % leaf_id
    return {
        "variants": {
            # "normal" is replaced by TreeLeavesBakedModel at bake time (TreeModels).
            "normal": {"model": model},
            "inventory": {"model": model},
        },
    }


def crown_model(style):
    tex = "csm:blocks/parks/palm_crown_%s" % style
    return {"parent": "block/cross", "textures": {"cross": tex, "particle": tex}}


def flat_item_model(texture):
    return {"parent": "item/generated", "textures": {"layer0": texture}}


def crown_blockstate(style):
    return {
        "variants": {
            # "normal" is replaced by TreeLeavesBakedModel at bake time (TreeModels).
            "normal": {"model": "csm:parks/palm_crown_%s" % style},
            "inventory": {"model": "csm:parks/palm_crown_%s_item" % style},
        },
    }


def moss_model(moss_id, tip):
    tex = "csm:blocks/parks/%s%s" % (moss_id, "_tip" if tip else "")
    return {"parent": "block/cross", "textures": {"cross": tex, "particle": tex}}


def moss_blockstate(moss_id):
    return {
        "forge_marker": 1,
        "defaults": {"model": "csm:parks/%s" % moss_id},
        "variants": {
            "tip": {"false": {}, "true": {"model": "csm:parks/%s_tip" % moss_id}},
            "inventory": [{"model": "csm:parks/%s_item" % moss_id}],
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
                out[loc][key] = cap_first(patterns[i].format(w=names[i]))
    for leaf_id, _, names, _, _, patterns in LEAVES:
        key = "tile.%s.name" % leaves_name(leaf_id)
        for i, loc in enumerate(LOCALES):
            out[loc][key] = cap_first(patterns[i].format(w=names[i]))
    for palm_id, _, _, names in PALMS:
        for i, loc in enumerate(LOCALES):
            out[loc]["tile.%s.name" % crown_name(palm_id)] = names[i]
    for moss_id, names, _ in MOSSES:
        for i, loc in enumerate(LOCALES):
            out[loc]["tile.%s.name" % moss_id] = names[i]
    for key, names in TOOL_LANG.items():
        for i, loc in enumerate(LOCALES):
            out[loc][key] = names[i]
    for preset_id, names in PRESETS:
        for i, loc in enumerate(LOCALES):
            out[loc]["csm.parks.preset.%s" % preset_id] = names[i]
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


def save_png(path, img):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def generate(assets):
    """Writes everything under an assets/csm root; returns the relative paths written."""
    written = []
    for i, (wood, _, _, recipe) in enumerate(WOODS):
        rel = "textures/blocks/parks/bark_%s.png" % wood
        save_png(os.path.join(assets, rel), bark(recipe, 20260922 + i))
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
    seed_index = {leaf[0]: i for i, leaf in enumerate(LEAVES)}
    for leaf_id, _, _, style, palette, _ in LEAVES:
        seed = 20260923 + seed_index[SEASON_OF.get(leaf_id, leaf_id)]
        rel = "textures/blocks/parks/leaves_%s.png" % leaf_id
        save_png(os.path.join(assets, rel), leaf_cluster(style, palette, seed))
        written.append(rel)
        rel = "models/block/parks/leaves_%s.json" % leaf_id
        dump(os.path.join(assets, rel), leaves_model(leaf_id))
        written.append(rel)
        rel = "blockstates/%s.json" % leaves_name(leaf_id)
        dump(os.path.join(assets, rel), leaves_blockstate(leaf_id))
        written.append(rel)
    for i, style in enumerate(PALM_SHEETS):
        sheet = palm_sheet(style, 20260924 + i)
        rel = "textures/blocks/parks/palm_crown_%s.png" % style
        save_png(os.path.join(assets, rel), sheet)
        written.append(rel)
        rel = "textures/blocks/parks/palm_crown_%s_icon.png" % style
        save_png(os.path.join(assets, rel), palm_icon(sheet))
        written.append(rel)
        rel = "models/block/parks/palm_crown_%s.json" % style
        dump(os.path.join(assets, rel), crown_model(style))
        written.append(rel)
        rel = "models/block/parks/palm_crown_%s_item.json" % style
        dump(os.path.join(assets, rel),
             flat_item_model("csm:blocks/parks/palm_crown_%s_icon" % style))
        written.append(rel)
    for palm_id, _, style, _ in PALMS:
        rel = "blockstates/%s.json" % crown_name(palm_id)
        dump(os.path.join(assets, rel), crown_blockstate(style))
        written.append(rel)
    for i, (moss_id, _, palette) in enumerate(MOSSES):
        for tip in (False, True):
            suffix = "_tip" if tip else ""
            rel = "textures/blocks/parks/%s%s.png" % (moss_id, suffix)
            save_png(os.path.join(assets, rel), moss(tip, 20260925 + 2 * i + tip, palette))
            written.append(rel)
            rel = "models/block/parks/%s%s.json" % (moss_id, suffix)
            dump(os.path.join(assets, rel), moss_model(moss_id, tip))
            written.append(rel)
        rel = "models/block/parks/%s_item.json" % moss_id
        dump(os.path.join(assets, rel), flat_item_model("csm:blocks/parks/%s_tip" % moss_id))
        written.append(rel)
        rel = "blockstates/%s.json" % moss_id
        dump(os.path.join(assets, rel), moss_blockstate(moss_id))
        written.append(rel)
    rel = "textures/items/parks/tree_planting_tool.png"
    save_png(os.path.join(assets, rel), planting_tool_icon())
    written.append(rel)
    rel = "models/item/tree_planting_tool.json"
    dump(os.path.join(assets, rel), flat_item_model("csm:items/parks/tree_planting_tool"))
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
    for leaf_id, ltype, _, _, _, _ in LEAVES:
        lines.append('    initTabBlock(new BlockTreeLeaves("%s", TreeLeafType.%s,'
                     % (leaves_name(leaf_id), ltype))
        lines.append('        "csm:blocks/parks/leaves_%s"));' % leaf_id)
    for palm_id, ptype, style, _ in PALMS:
        lines.append('    initTabBlock(new BlockTreeLeaves("%s", TreeLeafType.%s,'
                     % (crown_name(palm_id), ptype))
        lines.append('        "csm:blocks/parks/palm_crown_%s"));' % style)
    for moss_id, _, _ in MOSSES:
        lines.append('    initTabBlock(new BlockHangingMoss("%s"));' % moss_id)
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
