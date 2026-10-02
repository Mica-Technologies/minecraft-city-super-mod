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
                                              boot (TreePalmGeometry), and the coconut palm's
                                              nuts in the fourth cell, plus a _icon for the item;
                                              the Joshua tree's rosette is a crown sheet too
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
    ("birch", "BIRCH", ("Paper Birch", "Papierbirke", "abedul blanco", "pappersbjörk"), "birch"),
    ("maple", "MAPLE", ("Japanese Maple", "Fächerahorn", "arce japonés", "japansk lönn"),
     "smooth_grey"),
    ("spruce", "SPRUCE", ("Blue Spruce", "Stechfichte", "pícea azul", "blågran"), "scaly"),
    ("pine", "PINE", ("Scots Pine", "Waldkiefer", "pino silvestre", "tall"), "scaly_orange"),
    ("beech", "BEECH", ("European Beech", "Rotbuche", "haya", "bok"), "smooth_silver"),
    ("sabal", "SABAL", ("Cabbage Palm", "Palmettopalme", "palmito de Florida", "kålpalm"),
     "booted"),
    ("redwood", "REDWOOD", ("Coast Redwood", "Küstenmammutbaum", "secuoya roja", "kustsequoia"),
     "fibrous_red"),
    ("whitepine", "WHITE_PINE", ("Eastern White Pine", "Weymouth-Kiefer", "pino blanco",
                                 "weymouthtall"), "furrowed_deep"),
    ("oak", "OAK", ("English Oak", "Stieleiche", "roble común", "skogsek"), "furrowed_oak"),
    ("camphor", "CAMPHOR", ("Camphor Tree", "Kampferbaum", "alcanforero", "kamferträd"),
     "fissured_brown"),
    # GitHub #250, West and desert.
    ("sequoia", "SEQUOIA", ("Giant Sequoia", "Riesenmammutbaum", "secuoya gigante",
                            "jättesequoia"), "fibrous_cinnamon"),
    ("joshua", "JOSHUA", ("Joshua Tree", "Josua-Palmlilie", "árbol de Josué", "josuaträd"),
     "shaggy"),
    ("canary", "CANARY", ("Canary Island Date Palm", "Kanarische Dattelpalme", "palmera canaria",
                          "kanariedadelpalm"), "diamond"),
    ("palmgrey", "PALM_GREY", ("Grey Palm", "Graue Palme", "palmera gris", "grå palm"),
     "ringed_grey"),
    ("douglasfir", "DOUGLAS_FIR", ("Douglas Fir", "Douglasie", "abeto de Douglas",
                                   "douglasgran"), "furrowed_douglas"),
    ("bristlecone", "BRISTLECONE", ("Bristlecone Pine", "Grannenkiefer", "pino longevo",
                                    "borsttall"), "twisted_silver"),
    ("sycamore", "SYCAMORE", ("California Sycamore", "Kalifornische Platane",
                              "plátano de California", "kalifornisk platan"), "mottled_white"),
    ("bluegum", "BLUE_GUM", ("Blue Gum", "Blauer Eukalyptus", "eucalipto azul",
                             "blå eukalyptus"), "streaky"),
    # GitHub #250, fruit trees. The banana's "log" is its pseudostem of leaf sheaths.
    ("citrus", "CITRUS", ("Citrus", "Zitrus", "cítrico", "citrus"), "smooth_brown"),
    ("avocado", "AVOCADO", ("Avocado", "Avocado", "aguacate", "avokado"), "fissured_dark"),
    ("olive", "OLIVE", ("Olive", "Olivenbaum", "olivo", "olivträd"), "twisted_grey"),
    ("apple", "APPLE", ("Apple", "Apfelbaum", "manzano", "äppelträd"), "scaly_grey"),
    ("mulberry", "MULBERRY", ("Mulberry", "Maulbeerbaum", "morera", "mullbärsträd"),
     "furrowed_orange"),
    ("banana", "BANANA", ("Banana", "Banane", "platanera", "banan"), "sheath"),
    # GitHub #250, ornamental trees. The white willow wears the weeping willow's bark.
    ("sugarmaple", "SUGAR_MAPLE", ("Sugar Maple", "Zuckerahorn", "arce azucarero", "sockerlönn"),
     "furrowed_grey"),
    ("magnolia", "MAGNOLIA", ("Southern Magnolia", "Immergrüne Magnolie", "magnolio",
                              "storblommig magnolia"), "smooth_grey"),
    ("mahogany", "MAHOGANY", ("Mahogany", "Mahagoni", "caoba", "mahogny"), "furrowed_red"),
    ("crapemyrtle", "CRAPE_MYRTLE", ("Crape Myrtle", "Kräuselmyrte", "árbol de Júpiter",
                                     "kräppmyrten"), "mottled_pink"),
    ("chestnut", "CHESTNUT", ("Chinese Chestnut", "Chinesische Kastanie", "castaño chino",
                              "kinesisk kastanj"), "furrowed_deep"),
    ("tridentmaple", "TRIDENT_MAPLE", ("Trident Maple", "Dreispitz-Ahorn", "arce tridente",
                                       "treuddig lönn"), "peeling_orange"),
    ("plumeria", "PLUMERIA", ("Plumeria", "Frangipani", "plumeria", "frangipani"),
     "smooth_succulent"),
    ("cherry", "CHERRY", ("Japanese Cherry", "Japanische Kirsche", "cerezo japonés",
                          "japanskt körsbär"), "lenticel"),
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
FRUITING_NAMES = ("Fruiting {w} Leaves", "Laub mit Früchten ({w})", "Hojas con fruto de {w}",
                  "Löv med frukt ({w})")
FLOWERING_NAMES = ("Flowering {w} Leaves", "Laub mit Blüten ({w})", "Hojas con flores de {w}",
                   "Löv med blommor ({w})")
WHITE_WILLOW_NAMES = ("White Willow", "Silberweide", "sauce blanco", "vitpil")
YOSHINO_NAMES = ("Yoshino Cherry", "Yoshino-Kirsche", "cerezo Yoshino", "Yoshino-körsbär")
KANZAN_NAMES = ("Kanzan Cherry", "Kanzan-Kirsche", "cerezo Kanzan", "Kanzan-körsbär")
WEEPING_CHERRY_NAMES = ("Weeping Cherry", "Hänge-Kirsche", "cerezo llorón", "hängkörsbär")
ORANGE_NAMES = ("Orange", "Orange", "naranjo", "apelsin")
LEMON_NAMES = ("Lemon", "Zitrone", "limonero", "citron")
LIME_NAMES = ("Lime", "Limette", "limero", "lime")
GRAPEFRUIT_NAMES = ("Grapefruit", "Grapefruit", "pomelo", "grapefrukt")
HONEYCRISP_NAMES = ("Honeycrisp Apple", "Apfel 'Honeycrisp'", "manzano Honeycrisp",
                    "äpple 'Honeycrisp'")
GRANNY_NAMES = ("Granny Smith Apple", "Apfel 'Granny Smith'", "manzano Granny Smith",
                "äpple 'Granny Smith'")
GOLDEN_NAMES = ("Golden Delicious Apple", "Apfel 'Golden Delicious'",
                "manzano Golden Delicious", "äpple 'Golden Delicious'")

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
    ("birch", "BROADLEAF", WOODS[15][2], "broad",
     [(146, 184, 82), (122, 164, 68), (100, 142, 56), (80, 118, 46)], LEAF_NAMES),
    ("birch_autumn", "BROADLEAF", WOODS[15][2], "broad",
     [(250, 222, 96), (236, 200, 70), (212, 172, 52), (176, 140, 40)], AUTUMN_NAMES),
    ("maple_japanese", "BROADLEAF", WOODS[16][2], "palmate",
     [(176, 44, 50), (150, 32, 44), (122, 26, 40), (94, 22, 34)], LEAF_NAMES),
    ("maple_japanese_autumn", "BROADLEAF", WOODS[16][2], "palmate",
     [(242, 84, 40), (224, 58, 34), (196, 40, 30), (158, 30, 28)], AUTUMN_NAMES),
    ("spruce_blue", "BROADLEAF", WOODS[17][2], "needle_wide",
     [(166, 194, 204), (138, 168, 182), (110, 142, 158), (84, 114, 130)], NEEDLE_NAMES),
    ("pine", "BROADLEAF", WOODS[18][2], "needle_wide",
     [(112, 140, 102), (92, 120, 86), (74, 100, 70), (58, 80, 56)], NEEDLE_NAMES),
    ("beech", "BROADLEAF", WOODS[19][2], "broad",
     [(128, 170, 70), (106, 150, 58), (86, 128, 48), (66, 106, 38)], LEAF_NAMES),
    ("beech_autumn", "BROADLEAF", WOODS[19][2], "broad",
     [(210, 136, 60), (186, 110, 46), (158, 86, 36), (124, 64, 28)], AUTUMN_NAMES),
    ("redwood", "BROADLEAF", WOODS[21][2], "needle_wide",
     [(86, 116, 68), (68, 98, 56), (52, 80, 44), (38, 62, 34)], NEEDLE_NAMES),
    ("pine_white", "BROADLEAF", WOODS[22][2], "needle_wide",
     [(132, 162, 130), (110, 142, 110), (90, 122, 92), (70, 100, 74)], NEEDLE_NAMES),
    ("oak", "BROADLEAF", WOODS[23][2], "broad",
     [(104, 142, 58), (84, 122, 48), (66, 102, 40), (50, 82, 32)], LEAF_NAMES),
    ("oak_autumn", "BROADLEAF", WOODS[23][2], "broad",
     [(184, 124, 60), (158, 100, 46), (130, 80, 36), (100, 62, 28)], AUTUMN_NAMES),
    ("camphor", "BROADLEAF", WOODS[24][2], "broad_large",
     [(150, 184, 86), (124, 164, 70), (100, 142, 58), (78, 118, 46)], LEAF_NAMES),
    # GitHub #250, West and desert. The giant sequoia's scale-leaved cords are grey-blue-green,
    # the Douglas fir's soft needles a deep green, the bristlecone's foxtails dark and flecked
    # with resin, the blue gum's adult leaves dark grey-green sickles hanging straight down.
    ("sequoia", "BROADLEAF", WOODS[25][2], "cord",
     [(150, 172, 134), (126, 150, 114), (102, 128, 96), (80, 106, 78)], NEEDLE_NAMES),
    ("douglasfir", "BROADLEAF", WOODS[29][2], "needle_wide",
     [(80, 118, 76), (62, 98, 62), (46, 80, 50), (34, 62, 40)], NEEDLE_NAMES),
    ("bristlecone", "BROADLEAF", WOODS[30][2], "bottlebrush",
     [(92, 120, 92), (72, 100, 76), (56, 82, 62), (42, 64, 50)], NEEDLE_NAMES),
    ("bluegum", "AIRY", WOODS[32][2], "sickle",
     [(126, 146, 118), (104, 126, 100), (84, 106, 84), (66, 86, 68)], LEAF_NAMES),
    # GitHub #250, fruit trees. A fruiting set is its plain sibling's sprite (drawn with the same
    # seed, SEASON_OF) with the fruit dotted over it (FRUIT), so a crown of it bears fruit on
    # every face that shows.
    ("citrus", "BROADLEAF", WOODS[33][2], "broad",
     [(84, 130, 56), (64, 110, 44), (48, 90, 36), (34, 70, 28)], LEAF_NAMES),
    ("citrus_orange", "BROADLEAF", ORANGE_NAMES, "broad",
     [(84, 130, 56), (64, 110, 44), (48, 90, 36), (34, 70, 28)], FRUITING_NAMES),
    ("citrus_lemon", "BROADLEAF", LEMON_NAMES, "broad",
     [(84, 130, 56), (64, 110, 44), (48, 90, 36), (34, 70, 28)], FRUITING_NAMES),
    ("citrus_lime", "BROADLEAF", LIME_NAMES, "broad",
     [(84, 130, 56), (64, 110, 44), (48, 90, 36), (34, 70, 28)], FRUITING_NAMES),
    ("citrus_grapefruit", "BROADLEAF", GRAPEFRUIT_NAMES, "broad",
     [(84, 130, 56), (64, 110, 44), (48, 90, 36), (34, 70, 28)], FRUITING_NAMES),
    ("avocado", "BROADLEAF", WOODS[34][2], "broad_large",
     [(88, 124, 54), (68, 104, 44), (52, 86, 36), (38, 66, 28)], FRUITING_NAMES),
    ("olive", "AIRY", WOODS[35][2], "lanceolate",
     [(184, 192, 168), (154, 166, 138), (122, 138, 108), (94, 110, 84)], LEAF_NAMES),
    ("apple", "BROADLEAF", WOODS[36][2], "broad",
     [(116, 154, 68), (96, 134, 56), (76, 114, 46), (60, 94, 38)], LEAF_NAMES),
    ("apple_honeycrisp", "BROADLEAF", HONEYCRISP_NAMES, "broad",
     [(116, 154, 68), (96, 134, 56), (76, 114, 46), (60, 94, 38)], FRUITING_NAMES),
    ("apple_granny", "BROADLEAF", GRANNY_NAMES, "broad",
     [(116, 154, 68), (96, 134, 56), (76, 114, 46), (60, 94, 38)], FRUITING_NAMES),
    ("apple_golden", "BROADLEAF", GOLDEN_NAMES, "broad",
     [(116, 154, 68), (96, 134, 56), (76, 114, 46), (60, 94, 38)], FRUITING_NAMES),
    ("mulberry", "BROADLEAF", WOODS[37][2], "broad_large",
     [(108, 156, 66), (86, 136, 54), (68, 114, 44), (52, 94, 36)], FRUITING_NAMES),
    # GitHub #250, ornamental trees.
    ("sugarmaple", "BROADLEAF", WOODS[39][2], "maple",
     [(116, 160, 66), (96, 140, 54), (76, 120, 44), (60, 98, 36)], LEAF_NAMES),
    ("sugarmaple_autumn", "BROADLEAF", WOODS[39][2], "maple",
     [(246, 150, 44), (232, 104, 36), (204, 64, 34), (156, 40, 30)], AUTUMN_NAMES),
    ("magnolia", "BROADLEAF", WOODS[40][2], "broad_large",
     [(78, 112, 52), (60, 92, 42), (46, 74, 34), (36, 58, 26)], FLOWERING_NAMES),
    ("whitewillow", "BROADLEAF", WHITE_WILLOW_NAMES, "lanceolate",
     [(178, 198, 150), (150, 174, 124), (122, 150, 100), (96, 124, 80)], LEAF_NAMES),
    ("mahogany", "BROADLEAF", WOODS[41][2], "pinnate",
     [(108, 150, 62), (88, 130, 52), (70, 110, 42), (54, 90, 34)], LEAF_NAMES),
    ("crapemyrtle", "BROADLEAF", WOODS[42][2], "crape",
     [(240, 118, 196), (220, 84, 174), (186, 58, 146), (86, 124, 66)], FLOWERING_NAMES),
    ("chestnut", "BROADLEAF", WOODS[43][2], "broad",
     [(112, 152, 62), (92, 132, 52), (72, 112, 42), (56, 92, 34)], FRUITING_NAMES),
    ("tridentmaple", "BROADLEAF", WOODS[44][2], "trilobe",
     [(122, 164, 72), (100, 144, 58), (80, 122, 48), (62, 100, 38)], LEAF_NAMES),
    ("tridentmaple_autumn", "BROADLEAF", WOODS[44][2], "trilobe",
     [(248, 140, 50), (230, 96, 40), (200, 60, 36), (150, 40, 32)], AUTUMN_NAMES),
    # The cherries in blossom: palette from the palest petal to the deepest pink at a flower's
    # heart, then the anthers' dark rose.
    ("cherry_yoshino", "BROADLEAF", YOSHINO_NAMES, "sakura",
     [(255, 232, 242), (255, 204, 226), (252, 178, 212), (246, 148, 194), (222, 104, 154)],
     BLOSSOM_NAMES),
    ("cherry_kanzan", "BROADLEAF", KANZAN_NAMES, "sakura_double",
     [(255, 196, 224), (253, 160, 204), (248, 128, 186), (236, 98, 170), (204, 66, 134)],
     BLOSSOM_NAMES),
    ("cherry_weeping", "WEEPING", WEEPING_CHERRY_NAMES, "sakura",
     [(255, 222, 238), (254, 190, 218), (250, 162, 202), (240, 132, 186), (214, 88, 146)],
     BLOSSOM_NAMES),
]
SEASON_OF = {"elm_autumn": "elm", "plane_autumn": "plane", "honeylocust_autumn": "honeylocust",
             "ginkgo_autumn": "ginkgo", "poplar_autumn": "poplar",
             "sweetgum_autumn": "sweetgum", "birch_autumn": "birch",
             "maple_japanese_autumn": "maple_japanese", "beech_autumn": "beech",
             "oak_autumn": "oak",
             "citrus_orange": "citrus", "citrus_lemon": "citrus", "citrus_lime": "citrus",
             "citrus_grapefruit": "citrus", "apple_honeycrisp": "apple",
             "apple_granny": "apple", "apple_golden": "apple",
             "sugarmaple_autumn": "sugarmaple", "tridentmaple_autumn": "tridentmaple"}

# Fruit dotted over a fruiting set's sprite: leaf id -> [(light, mid, dark, radius across,
# radius down, how many)], each a little shaded ball (or a pear, or a berry) on the leaves.
FRUIT = {
    "citrus_orange": [((252, 176, 52), (238, 140, 26), (196, 102, 18), 1.6, 1.6, 9)],
    "citrus_lemon": [((254, 240, 104), (238, 214, 52), (194, 168, 30), 1.4, 1.8, 9)],
    "citrus_lime": [((156, 206, 76), (114, 172, 46), (78, 128, 32), 1.2, 1.3, 9)],
    "citrus_grapefruit": [((254, 220, 132), (242, 188, 108), (212, 138, 96), 2.2, 2.2, 6)],
    "avocado": [((104, 118, 60), (62, 76, 38), (32, 40, 22), 1.5, 2.3, 7)],
    "olive": [((92, 74, 92), (60, 46, 62), (36, 26, 38), 0.8, 1.0, 6)],
    "apple_honeycrisp": [((236, 104, 64), (202, 48, 42), (144, 30, 32), 1.8, 1.7, 8)],
    "apple_granny": [((178, 220, 98), (142, 194, 68), (102, 150, 46), 1.8, 1.7, 8)],
    "apple_golden": [((248, 226, 110), (230, 198, 70), (186, 156, 48), 1.8, 1.7, 8)],
    "mulberry": [((112, 44, 84), (74, 24, 52), (42, 12, 30), 0.9, 1.4, 11),
                 ((214, 64, 70), (182, 36, 52), (130, 24, 40), 0.9, 1.4, 4)],
    # Not fruit: the magnolia's big white cup flowers, the chestnut's spiny burs.
    # The magnolia's leaves turned to show their rusty undersides, then its flowers.
    "magnolia": [((156, 110, 66), (130, 88, 50), (100, 66, 38), 1.2, 2.6, 6),
                 ((255, 254, 246), (240, 236, 220), (204, 196, 170), 2.5, 2.3, 5)],
    "chestnut": [((206, 214, 116), (160, 180, 70), (112, 132, 44), 1.9, 1.9, 7)],
}

# Palm crowns: (id, TreeLeafType constant, sheet, names en/de/es/sv). Order = tab order.
PALMS = [
    ("palm_fan", "PALM_FAN", "fan",
     ("Fan Palm Crown", "Fächerpalmen-Krone", "Copa de palmera de abanico", "Solfjäderspalmkrona")),
    ("palm_fan_skirt", "PALM_FAN_SKIRT", "fan",
     ("Fan Palm Crown with Skirt", "Fächerpalmen-Krone mit Trockenwedeln",
      "Copa de palmera de abanico con faldón", "Solfjäderspalmkrona med kjol")),
    ("palm_feather", "PALM_FEATHER", "feather",
     ("Feather Palm Crown", "Fiederpalmen-Krone", "Copa de palmera de pluma", "Fjäderpalmkrona")),
    ("palm_cabbage", "PALM_CABBAGE", "cabbage",
     ("Cabbage Palm Crown", "Palmettopalmen-Krone", "Copa de palmito de Florida",
      "Kålpalmkrona")),
    ("palm_cabbage_skirt", "PALM_CABBAGE_SKIRT", "cabbage",
     ("Cabbage Palm Crown with Skirt", "Palmettopalmen-Krone mit Trockenwedeln",
      "Copa de palmito de Florida con faldón", "Kålpalmkrona med kjol")),
    # GitHub #250. The Joshua tree's rosette is drawn by the same machinery as a palm crown.
    ("palm_canary", "PALM_CANARY", "canary",
     ("Canary Island Date Palm Crown", "Kanarische Dattelpalmen-Krone",
      "Copa de palmera canaria", "Kanariedadelpalmkrona")),
    ("palm_coconut", "PALM_COCONUT", "coconut",
     ("Coconut Palm Crown", "Kokospalmen-Krone", "Copa de cocotero", "Kokospalmkrona")),
    ("palm_king", "PALM_KING", "king",
     ("King Palm Crown", "Alexanderpalmen-Krone", "Copa de palmera real australiana",
      "Kungspalmkrona")),
    ("joshua", "ROSETTE", "joshua",
     ("Joshua Tree Rosette", "Josua-Palmlilien-Rosette", "Roseta de árbol de Josué",
      "Josuaträdsrosett")),
    ("banana", "PALM_BANANA", "banana",
     ("Banana Crown", "Bananenstauden-Krone", "Copa de platanera", "Bananplantkrona")),
    ("banana_fruit", "PALM_BANANA_FRUIT", "banana",
     ("Banana Crown with Fruit", "Bananenstauden-Krone mit Früchten",
      "Copa de platanera con racimo", "Bananplantkrona med klase")),
    ("plumeria", "PALM_PLUMERIA", "plumeria",
     ("Plumeria Leaf Tuft", "Frangipani-Blattschopf", "Penacho de plumeria",
      "Frangipanitofs")),
]
PALM_SHEETS = ["fan", "feather", "cabbage", "canary", "coconut", "king", "joshua", "banana",
               "plumeria"]

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
    ("paperbirch", ("Paper Birch", "Papierbirke", "Abedul blanco", "Pappersbjörk")),
    ("bluespruce", ("Colorado Blue Spruce", "Blau-Fichte", "Pícea azul de Colorado",
                    "Blågran")),
    ("cabbagepalm", ("Cabbage Palm", "Palmettopalme", "Palmito de Florida", "Kålpalm")),
    ("japanesemaple", ("Japanese Maple", "Fächerahorn", "Arce japonés", "Japansk lönn")),
    ("scotspine", ("Scots Pine", "Waldkiefer", "Pino silvestre", "Tall")),
    ("beech", ("European Beech", "Rotbuche", "Haya europea", "Bok")),
    ("coastredwood", ("Coast Redwood", "Küstenmammutbaum", "Secuoya roja", "Kustsequoia")),
    ("whitepine", ("Eastern White Pine", "Weymouth-Kiefer", "Pino blanco del este",
                   "Weymouthtall")),
    ("englishoak", ("Old English Oak", "Alte Stieleiche", "Roble común centenario",
                    "Gammal skogsek")),
    ("camphor", ("Camphor Tree", "Kampferbaum", "Alcanforero", "Kamferträd")),
    # GitHub #250, West and desert.
    ("giantsequoia", ("Giant Sequoia", "Riesenmammutbaum", "Secuoya gigante", "Jättesequoia")),
    ("joshuatree", ("Joshua Tree", "Josua-Palmlilie", "Árbol de Josué", "Josuaträd")),
    ("youngjoshua", ("Young Joshua Tree", "Junge Josua-Palmlilie", "Árbol de Josué joven",
                     "Ungt josuaträd")),
    ("canarypalm", ("Canary Island Date Palm", "Kanarische Dattelpalme", "Palmera canaria",
                    "Kanariedadelpalm")),
    ("coconutpalm", ("Coconut Palm", "Kokospalme", "Cocotero", "Kokospalm")),
    ("kingpalm", ("King Palm", "Alexanderpalme", "Palmera real australiana", "Kungspalm")),
    ("douglasfir", ("Douglas Fir", "Douglasie", "Abeto de Douglas", "Douglasgran")),
    ("bristlecone", ("Great Basin Bristlecone Pine", "Langlebige Grannenkiefer",
                     "Pino longevo de la Gran Cuenca", "Borsttall")),
    ("sycamore", ("California Sycamore", "Kalifornische Platane", "Plátano de California",
                  "Kalifornisk platan")),
    ("bluegum", ("Blue Gum Eucalyptus", "Blauer Eukalyptus", "Eucalipto azul",
                 "Blå eukalyptus")),
    # GitHub #250, fruit trees.
    ("orange", ("Orange Tree", "Orangenbaum", "Naranjo", "Apelsinträd")),
    ("lemon", ("Lemon Tree", "Zitronenbaum", "Limonero", "Citronträd")),
    ("lime", ("Lime Tree", "Limettenbaum", "Limero", "Limeträd")),
    ("grapefruit", ("Grapefruit Tree", "Grapefruitbaum", "Pomelo", "Grapefruktträd")),
    ("avocado", ("Avocado Tree", "Avocadobaum", "Aguacate", "Avokadoträd")),
    ("olive", ("Olive Tree", "Olivenbaum", "Olivo", "Olivträd")),
    ("banana", ("Banana Plant", "Bananenstaude", "Platanera", "Bananplanta")),
    ("applehoneycrisp", ("Honeycrisp Apple Tree", "Apfelbaum 'Honeycrisp'",
                         "Manzano Honeycrisp", "Äppelträd 'Honeycrisp'")),
    ("applegranny", ("Granny Smith Apple Tree", "Apfelbaum 'Granny Smith'",
                     "Manzano Granny Smith", "Äppelträd 'Granny Smith'")),
    ("applegolden", ("Golden Delicious Apple Tree", "Apfelbaum 'Golden Delicious'",
                     "Manzano Golden Delicious", "Äppelträd 'Golden Delicious'")),
    ("mulberry", ("Mulberry Tree", "Maulbeerbaum", "Morera", "Mullbärsträd")),
    # GitHub #250, ornamental trees.
    ("sugarmaple", ("Sugar Maple", "Zuckerahorn", "Arce azucarero", "Sockerlönn")),
    ("magnolia", ("Southern Magnolia", "Immergrüne Magnolie", "Magnolio",
                  "Storblommig magnolia")),
    ("whitewillow", ("White Willow", "Silberweide", "Sauce blanco", "Vitpil")),
    ("mahogany", ("West Indies Mahogany", "Echtes Mahagoni", "Caoba antillana",
                  "Västindisk mahogny")),
    ("crapemyrtle", ("Crape Myrtle", "Kräuselmyrte", "Árbol de Júpiter", "Kräppmyrten")),
    ("crapemyrtledwarf", ("Dwarf Crape Myrtle", "Zwerg-Kräuselmyrte", "Árbol de Júpiter enano",
                          "Dvärgkräppmyrten")),
    ("chinesechestnut", ("Chinese Chestnut", "Chinesische Kastanie", "Castaño chino",
                         "Kinesisk kastanj")),
    ("tridentmaple", ("Trident Maple", "Dreispitz-Ahorn", "Arce tridente", "Treuddig lönn")),
    ("dwarfjacaranda", ("Dwarf Jacaranda", "Zwerg-Jacaranda", "Jacarandá enano",
                        "Dvärgjakaranda")),
    ("plumeria", ("Plumeria", "Frangipani", "Plumeria", "Frangipani")),
    ("cherryyoshino", ("Yoshino Cherry", "Yoshino-Kirsche", "Cerezo Yoshino",
                       "Yoshino-körsbär")),
    ("cherrykanzan", ("Kanzan Cherry", "Kanzan-Kirsche", "Cerezo Kanzan", "Kanzan-körsbär")),
    ("cherryweeping", ("Weeping Cherry", "Hänge-Kirsche", "Cerezo llorón", "Hängkörsbär")),
]

# The tool's own lines: key -> en/de/es/sv.
TOOL_LANG = {
    "item.tree_planting_tool.name": (
        "Tree Planting Tool", "Baumpflanzwerkzeug", "Herramienta para plantar árboles",
        "Trädplanteringsverktyg"),
    "csm.parks.planting.mode": (
        "Planting: %s", "Pflanzen: %s", "Plantar: %s", "Planterar: %s"),
    "csm.parks.planting.noroom": (
        "Can't plant %s here: no room for its trunk",
        "%s kann hier nicht gepflanzt werden: kein Platz für den Stamm",
        "No se puede plantar %s aquí: no hay sitio para el tronco",
        "Kan inte plantera %s här: ingen plats för stammen"),
    "csm.parks.planting.trimmed": (
        "Planted %s, pruned to fit (%s blocks cut back)",
        "%s gepflanzt, passend zurückgeschnitten (%s Blöcke)",
        "%s plantado, podado para que quepa (%s bloques recortados)",
        "%s planterad, beskuren för att få plats (%s block bortklippta)"),
    "csm.parks.planting.tooltip.use": (
        "Right-click a block to plant a tree, leaning the way you face or away from walls",
        "Rechtsklick auf einen Block pflanzt einen Baum, der sich in Blickrichtung oder von Wänden weg neigt",
        "Clic derecho en un bloque para plantar un árbol inclinado hacia donde miras o lejos de las paredes",
        "Högerklicka på ett block för att plantera ett träd som lutar åt det håll du tittar eller bort från väggar"),
    "csm.parks.planting.tooltip.fell": (
        "Breaking a log fells what it held up; sneak to break just that block",
        "Ein abgebauter Stamm fällt, was er trug; schleichen baut nur diesen Block ab",
        "Romper un tronco derriba lo que sostenía; agáchate para romper solo ese bloque",
        "Att bryta en stam fäller det den bar upp; smyg för att bryta bara det blocket"),
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
    elif recipe == "birch":
        # Chalk-white paper bark: dark horizontal lenticels and a few black scars.
        base, spread = (224, 220, 210), 6
        field = [[0.0] * SIZE for _ in range(SIZE)]
        for _ in range(9):
            x, y, length = rng.randrange(SIZE), rng.randrange(SIZE), rng.randint(2, 5)
            for k in range(length):
                field[y][(x + k) % SIZE] -= 150
        for _ in range(3):
            x, y = rng.randrange(SIZE), rng.randrange(SIZE)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (2, 0)):
                field[(y + dy) % SIZE][(x + dx) % SIZE] -= 170
    elif recipe == "booted":
        # Cabbage palm: the criss-cross of old frond bases ("boots") left on the trunk.
        base, spread = (122, 108, 82), 8
        field = [[0.0] * SIZE for _ in range(SIZE)]
        for y in range(SIZE):
            for x in range(SIZE):
                if (x + y) % 8 < 2 or (x - y) % 8 < 2:
                    field[y][x] -= 34
                elif ((x + y) // 8 + (x - y) // 8) % 2:
                    field[y][x] += 10
    elif recipe == "fibrous_red":
        # Coast redwood: thick, soft, red-brown bark in long vertical fibres.
        base, spread = (146, 78, 56), 8
        field = _furrows(rng, 8, 26, jitter=0)
    elif recipe == "furrowed_oak":
        # Old oak: deep grey ridges broken across into blocks.
        base, spread = (110, 102, 90), 10
        field = _furrows(rng, 6, 40)
        for y in range(0, SIZE, 4):
            for x in range(SIZE):
                if rng.random() < 0.35:
                    field[(y + x // 5) % SIZE][x] -= 18
    elif recipe == "smooth_silver":
        base, spread = (160, 160, 154), 8
        field = _furrows(rng, 2, 8, jitter=0)
    elif recipe in ("scaly", "scaly_orange", "scaly_grey"):
        # Small flaking plates: short dark cracks between patches. Apple: grey-brown flakes.
        base, spread = ((98, 84, 74), 10) if recipe == "scaly" else ((176, 104, 62), 12) \
            if recipe == "scaly_orange" else ((122, 112, 100), 10)
        field = [[0.0] * SIZE for _ in range(SIZE)]
        for y in range(0, SIZE, 3):
            off = rng.randrange(4)
            for x in range(SIZE):
                if (x + off) % 4 == 0 or rng.random() < 0.15:
                    field[y][x] -= 30
        for x in range(0, SIZE, 4):
            for y in range(SIZE):
                if rng.random() < 0.35:
                    field[y][(x + (y // 3) % 2 * 2) % SIZE] -= 22
    elif recipe == "fibrous_cinnamon":
        # Giant sequoia: soft cinnamon-orange bark in broad rounded ridges between deep furrows,
        # brighter and redder than the coast redwood's.
        base, spread = (180, 96, 60), 16
        field = _furrows(rng, 5, 52)
        edge = [[False] * SIZE for _ in range(SIZE)]
        for y in range(SIZE):
            for x in range(SIZE):
                # Each ridge is shaded beside its furrows and lit along its crown.
                edge[y][x] = field[y][(x - 1) % SIZE] < -20 or field[y][(x + 1) % SIZE] < -20
        for y in range(SIZE):
            for x in range(SIZE):
                if field[y][x] > -20:
                    field[y][x] += -16 if edge[y][x] else 10 + rng.choice((0, 0, 8, -8))
    elif recipe == "shaggy":
        # Joshua tree: the trunk wears its dead leaves, a thatch of grey-brown blades pointing
        # down, over dark gaps.
        base, spread = (64, 56, 46), 6
        field = [[0.0] * SIZE for _ in range(SIZE)]
        for row in range(0, SIZE, 3):
            off = rng.randrange(4)
            for x0 in range(off, SIZE + off, 3):
                length = rng.randint(3, 5)
                lean = rng.choice((-1, 0, 1))
                light = rng.randint(54, 86)
                for k in range(length):
                    x = (x0 + (lean * k) // 2) % SIZE
                    y = (row + k) % SIZE
                    field[y][x] = max(field[y][x], light - k * 6)
    elif recipe == "diamond":
        # Canary Island date palm: the "pineapple" of old leaf bases trimmed flush, diamonds with
        # a lit upper edge and a shadowed lower one.
        base, spread = (124, 104, 76), 6
        field = [[0.0] * SIZE for _ in range(SIZE)]
        for y in range(SIZE):
            for x in range(SIZE):
                a, b = (x + y) % 8, (x - y) % 8
                if a == 0 or b == 0:
                    field[y][x] -= 40
                elif a == 1 or b == 7:
                    field[y][x] += 22
                elif a >= 6 or b <= 2:
                    field[y][x] -= 12
    elif recipe == "ringed_grey":
        # Coconut and king palms: smooth pale grey, a leaf-scar ring every few pixels.
        base, spread = (156, 152, 142), 6
        field = [[0.0] * SIZE for _ in range(SIZE)]
        for y in range(SIZE):
            for x in range(SIZE):
                if y % 4 == 0:
                    field[y][x] -= 30 if (x + y // 4) % 7 else 16
                elif y % 4 == 1:
                    field[y][x] += 8
        for _ in range(4):
            x = rng.randrange(SIZE)
            for y in range(SIZE):
                field[y][x] -= 6
    elif recipe == "furrowed_douglas":
        # Douglas fir: thick, corky, dark brown bark in deep furrows, red-brown in their bottoms.
        base, spread = (96, 72, 58), 10
        field = _furrows(rng, 6, 44)
        for y in range(0, SIZE, 5):
            for x in range(SIZE):
                if rng.random() < 0.3:
                    field[(y + x // 4) % SIZE][x] -= 16
    elif recipe == "twisted_silver":
        # Bristlecone pine: weathered silver deadwood twisted into a spiral, with ochre and rust
        # where a strip of living bark still runs up it, and dark grain between. The bands climb
        # one pixel across for every two up, so they repeat every 8 px and tile both ways.
        bands = [(198, 194, 182), (212, 208, 196), (164, 156, 142), (204, 200, 188),
                 (182, 148, 102), (156, 112, 74), (128, 120, 108), (196, 192, 180)]
        img = Image.new("RGBA", (SIZE, SIZE))
        px = img.load()
        for y in range(SIZE):
            for x in range(SIZE):
                c = bands[(x - y // 2) % 8]
                px[x, y] = tuple(max(0, min(255, ch + rng.randint(-7, 7))) for ch in c) + (255,)
        return img
    elif recipe == "streaky":
        # Blue gum: bark shedding in long ribbons, streaks of white, grey, tan and blue-grey.
        base, spread = (172, 164, 146), 6
        field = [[0.0] * SIZE for _ in range(SIZE)]
        x = 0
        while x < SIZE:
            w = rng.randint(1, 3)
            shade = rng.choice((44, 30, -30, -46, 12, -14))
            for k in range(w):
                for y in range(SIZE):
                    field[y][(x + k) % SIZE] += shade
            x += w
        for _ in range(3):
            x, y0 = rng.randrange(SIZE), rng.randrange(SIZE)
            for k in range(rng.randint(4, 9)):
                field[(y0 + k) % SIZE][x] -= 40
    elif recipe == "smooth_brown":
        # Citrus: smooth grey-brown bark, finely fissured.
        base, spread = (118, 108, 92), 8
        field = _furrows(rng, 3, 16)
    elif recipe == "fissured_dark":
        # Avocado: dark grey-brown, roughly fissured.
        base, spread = (94, 86, 78), 10
        field = _furrows(rng, 5, 30)
    elif recipe == "furrowed_orange":
        # Mulberry: brown bark with an orange cast in its furrows' bottoms.
        base, spread = (124, 94, 70), 10
        field = _furrows(rng, 6, 32)
    elif recipe == "twisted_grey":
        # Olive: grey bark twisted into a spiral, deeply grooved; the bands climb one pixel
        # across for every two up, repeating every 8 px, so it tiles both ways.
        bands = [(150, 146, 134), (128, 124, 114), (92, 88, 80), (140, 136, 124),
                 (112, 106, 96), (76, 72, 64), (134, 130, 120), (120, 114, 104)]
        img = Image.new("RGBA", (SIZE, SIZE))
        px = img.load()
        for y in range(SIZE):
            for x in range(SIZE):
                c = bands[(x - y // 2) % 8]
                px[x, y] = tuple(max(0, min(255, ch + rng.randint(-8, 8))) for ch in c) + (255,)
        return img
    elif recipe == "sheath":
        # Banana pseudostem: overlapping leaf sheaths, green streaked lengthwise, with brown,
        # dried strips of the old outer sheaths.
        img = Image.new("RGBA", (SIZE, SIZE))
        px = img.load()
        cols = []
        x = 0
        while x < SIZE:
            w = rng.randint(2, 4)
            dry = rng.random() < 0.3
            cols += [dry] * w
            x += w
        for y in range(SIZE):
            for x in range(SIZE):
                if cols[x]:
                    c = (132, 108, 72) if (x + y) % 5 else (104, 84, 56)
                else:
                    c = (112, 146, 72) if x % 3 else (92, 126, 60)
                if y % 8 == (x // 4 * 3) % 8:
                    c = tuple(ch - 26 for ch in c)  # a sheath's edge
                px[x, y] = tuple(max(0, min(255, ch + rng.randint(-6, 6))) for ch in c) + (255,)
        return img
    elif recipe == "furrowed_red":
        # Mahogany: dark reddish-brown, furrowed and breaking into scaly plates.
        base, spread = (110, 66, 50), 10
        field = _furrows(rng, 6, 34)
        for y in range(0, SIZE, 4):
            for x in range(SIZE):
                if rng.random() < 0.3:
                    field[(y + x // 4) % SIZE][x] -= 16
    elif recipe == "lenticel":
        # Japanese cherry: glossy, dark reddish-brown bark, banded across with pale lenticels.
        img = Image.new("RGBA", (SIZE, SIZE))
        px = img.load()
        shine = [rng.randint(-6, 6) for _ in range(SIZE)]
        for y in range(SIZE):
            for x in range(SIZE):
                c = (98, 54, 48)
                if x % 8 in (2, 3):
                    c = (118, 70, 60)  # the gloss down the curve of the stem
                px[x, y] = tuple(max(0, min(255, ch + shine[x] + rng.randint(-5, 5)))
                                 for ch in c) + (255,)
        for row in range(1, SIZE, 4):
            x = rng.randrange(SIZE)
            while x < SIZE + 16:
                length = rng.randint(3, 6)
                for k in range(length):
                    px[(x + k) % SIZE, row] = (168, 128, 112, 255)
                    px[(x + k) % SIZE, (row + 1) % SIZE] = (70, 36, 34, 255)
                x += length + rng.randint(2, 5)
        return img
    elif recipe == "smooth_succulent":
        # Plumeria: smooth, grey, swollen and succulent, ringed faintly with old leaf scars.
        base, spread = (152, 154, 142), 8
        field = _furrows(rng, 2, 8, jitter=0)
        for y in range(0, SIZE, 6):
            for x in range(SIZE):
                if (x + y) % 5:
                    field[y][x] -= 16
    elif recipe in ("mottled_pink", "peeling_orange"):
        base, spread = (180, 150, 130), 0
        field = [[0.0] * SIZE for _ in range(SIZE)]
    elif recipe == "mottled_white":
        base, spread = (210, 204, 188), 0
        field = [[0.0] * SIZE for _ in range(SIZE)]
    else:
        raise ValueError(recipe)
    grain = _noise(rng, 4)
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    if recipe in ("mottled", "white_smooth", "mottled_white", "mottled_pink", "peeling_orange"):
        # London plane: flaking patches of cream, olive and grey. Lemon-scented gum: powdery
        # white with pink and grey where the old bark has just shed. California sycamore: mostly
        # bright white, in big patches of tan and grey.
        patches = ([(186, 180, 150), (150, 146, 104), (128, 124, 116), (196, 188, 160)]
                   if recipe == "mottled" else
                   [(222, 218, 210), (236, 232, 226), (206, 196, 196), (228, 222, 214)]
                   if recipe == "white_smooth" else
                   [(150, 146, 136), (186, 164, 128), (232, 228, 216), (240, 238, 228)]
                   if recipe == "mottled_white" else
                   # Crape myrtle: smooth, shedding in patches of pinkish tan, cinnamon and grey.
                   [(150, 146, 140), (198, 160, 136), (172, 132, 108), (218, 188, 164)]
                   if recipe == "mottled_pink" else
                   # Trident maple: grey bark peeling away in orange-brown plates.
                   [(126, 118, 108), (178, 118, 70), (150, 96, 60), (196, 140, 92)])
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
    elif style == "palmate":
        # Japanese maple: small five-pointed stars, finely cut, a little light between them.
        for _ in range(70):
            cx, cy = rng.uniform(0, LEAF_SIZE), rng.uniform(0, LEAF_SIZE)
            a = rng.uniform(0, 2 * math.pi)
            shade = rng.randrange(len(palette) - 1)
            put(int(cx), int(cy), palette[shade + 1])
            for lobe in range(5):
                ang = a + lobe * 2 * math.pi / 5
                for r in (1, 2, 3):
                    put(int(round(cx + r * math.cos(ang))), int(round(cy + r * math.sin(ang))),
                        palette[shade + (1 if r == 3 else 0)])
    elif style in ("needle", "needle_wide"):
        # needle_wide: the same needles over a whole cluster, for a conifer's broad crown.
        for _ in range(420 if style == "needle" else 560):
            x, y = rng.randrange(LEAF_SIZE), rng.randrange(LEAF_SIZE)
            shade = rng.randrange(len(palette))
            for d in range(3):
                put(x, y + d, palette[min(len(palette) - 1, shade + (1 if d == 2 else 0))])
    elif style in ("maple", "trilobe"):
        # Sugar maple: big five-lobed leaves. Trident maple: smaller, three-lobed.
        lobes = 5 if style == "maple" else 3
        for _ in range(48 if style == "maple" else 70):
            cx, cy = rng.uniform(0, LEAF_SIZE), rng.uniform(0, LEAF_SIZE)
            a = rng.uniform(0, 2 * math.pi)
            shade = rng.randrange(len(palette) - 1)
            reach = (1, 2, 3, 4) if style == "maple" else (1, 2, 3)
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    put(int(cx) + dx, int(cy) + dy, palette[shade])
            for lobe in range(lobes):
                ang = a + lobe * 2 * math.pi / lobes
                for r in reach:
                    for w in (0, 1) if r < reach[-1] else (0,):
                        put(int(round(cx + r * math.cos(ang) - w * math.sin(ang))),
                            int(round(cy + r * math.sin(ang) + w * math.cos(ang))),
                            palette[shade + (1 if r == reach[-1] else 0)])
    elif style in ("sakura", "sakura_double"):
        # Cherry blossom: a cloud of small five-petalled flowers (single, Yoshino) or full
        # pom-poms of them (double, Kanzan), layered from near-white to blush, each a little
        # deeper at its heart with a few rose anthers, and hardly a leaf. The edge is left loose:
        # flowers spill past the cloud's outline, so a crown's sheets read as fluffy, not cut.
        double = style == "sakura_double"
        light, pale, mid, deep, anther = palette

        def flower(cx, cy, shade):
            # Five round petals about a heart one pixel across.
            petals = [light, pale, mid][shade]
            a0 = rng.uniform(0, 2 * math.pi)
            for k in range(5):
                ang = a0 + k * 2 * math.pi / 5
                put(int(round(cx + 1.2 * math.cos(ang))), int(round(cy + 1.2 * math.sin(ang))),
                    petals)
            put(int(round(cx)), int(round(cy)), deep if shade else mid)
            if rng.random() < 0.15:
                put(int(round(cx + rng.choice((-1, 1)))), int(round(cy)), anther)

        # Underneath: soft pink, so the gaps between flowers are blossom, not sky.
        for _ in range(420 if double else 380):
            x, y = rng.randrange(LEAF_SIZE), rng.randrange(LEAF_SIZE)
            put(x, y, mid if rng.random() < 0.5 else pale)
        if double:
            for _ in range(30):
                cx, cy = rng.uniform(3, 29), rng.uniform(3, 29)
                for k in range(9):
                    r = 2.4 * math.sqrt(rng.random())
                    a = rng.uniform(0, 2 * math.pi)
                    flower(cx + r * math.cos(a), cy + r * math.sin(a),
                           0 if r > 1.6 else rng.choice((1, 2)))
        else:
            for _ in range(130):
                flower(rng.uniform(1, 31), rng.uniform(1, 31), rng.choice((0, 0, 1, 1, 2)))
        # A very few young leaves, bronze-green, as on a tree in full bloom.
        for _ in range(4):
            x, y = rng.randrange(LEAF_SIZE), rng.randrange(LEAF_SIZE)
            put(x, y, (150, 132, 92))
    elif style == "crape":
        # Crape myrtle in flower: small leaves almost hidden under big panicles of crinkled
        # flowers, bright pink to magenta.
        greens = [(96, 134, 70), (78, 114, 58), (62, 96, 46)]
        for _ in range(240):
            cx, cy = rng.uniform(0, LEAF_SIZE), rng.uniform(0, LEAF_SIZE)
            a = rng.uniform(0, math.pi)
            g = greens[rng.randrange(3)]
            for t in (-1, 0, 1):
                put(int(round(cx + t * math.cos(a))), int(round(cy + t * math.sin(a))), g)
        for _ in range(40):
            cx, cy = rng.uniform(2, 30), rng.uniform(2, 30)
            shade = rng.randrange(3)
            for _ in range(16):
                dx, dy = rng.randint(-3, 3), rng.randint(-2, 2)
                if dx * dx / 10.0 + dy * dy / 5.0 <= 1:
                    put(int(cx) + dx, int(cy) + dy,
                        palette[min(2, shade + rng.choice((0, 0, 1)))])
    elif style == "pinnate":
        # Mahogany: small paired leaflets on short stalks, denser than the honey locust's.
        for _ in range(34):
            x, y = rng.uniform(2, 30), rng.uniform(2, 30)
            a = rng.uniform(0, math.pi)
            for step in range(9):
                x += math.cos(a)
                y += math.sin(a)
                put(int(x), int(y), palette[-1])
                for side in (-1, 1):
                    for k in (1, 2):
                        lx = x + math.cos(a + side * 1.3) * k
                        ly = y + math.sin(a + side * 1.3) * k
                        put(int(round(lx)), int(round(ly)), palette[rng.randrange(3)])
    elif style == "lanceolate":
        # Olive: narrow willow-like leaves, silver-grey above and paler beneath, light between.
        for _ in range(230):
            cx, cy = rng.uniform(0, LEAF_SIZE), rng.uniform(0, LEAF_SIZE)
            a = rng.uniform(0, math.pi)
            shade = rng.randrange(len(palette))
            for t in range(-2, 3):
                put(int(round(cx + t * math.cos(a))), int(round(cy + t * math.sin(a))),
                    palette[min(len(palette) - 1, shade + (1 if abs(t) == 2 else 0))])
    elif style == "cord":
        # Giant sequoia: short scale-leaved cords, branching every way, in clumps with light
        # between them.
        for _ in range(130):
            x, y = rng.uniform(0, LEAF_SIZE), rng.uniform(0, LEAF_SIZE)
            a = rng.uniform(0, 2 * math.pi)
            shade = rng.randrange(len(palette) - 1)
            for step in range(rng.randint(4, 8)):
                put(int(x), int(y), palette[shade + (1 if step % 3 == 2 else 0)])
                put(int(x) + 1, int(y), palette[min(3, shade + 1)])
                x += math.cos(a)
                y += math.sin(a)
                a += rng.uniform(-0.6, 0.6)
    elif style == "bottlebrush":
        # Bristlecone pine: needles packed all round the twig in a dense foxtail, flecked white
        # with the resin each needle carries.
        for _ in range(620):
            x, y = rng.randrange(LEAF_SIZE), rng.randrange(LEAF_SIZE)
            shade = rng.randrange(len(palette))
            dx = rng.choice((-1, 0, 1))
            for d in range(2):
                put(x + dx * d, y + d, palette[min(len(palette) - 1, shade + d)])
        for _ in range(26):
            put(rng.randrange(LEAF_SIZE), rng.randrange(LEAF_SIZE), (226, 228, 216))
    elif style == "sickle":
        # Blue gum: long, narrow, sickle-curved adult leaves hanging straight down from their
        # twigs, mostly gaps between them.
        for _ in range(22):
            x0, y0 = rng.uniform(0, 26), rng.uniform(2, 20)
            # The twig, then leaves hanging off it.
            twig = rng.uniform(-0.5, 0.5)
            for k in range(rng.randint(4, 7)):
                tx, ty = x0 + k * 1.6, y0 + k * twig
                put(int(tx), int(ty), (92, 74, 60))
                if k % 2 == 0:
                    continue
                length = rng.randint(8, 13)
                bend = rng.choice((-1, 1)) * rng.uniform(1.5, 3.0)
                shade = rng.randrange(len(palette) - 1)
                for j in range(length):
                    t = j / float(length)
                    x = tx + bend * math.sin(t * math.pi) * (1 - t * 0.3)
                    y = ty + 1 + j
                    put(int(round(x)), int(y), palette[shade + (1 if j > length - 3 else 0)])
                    if 2 <= j < length - 3:
                        put(int(round(x)) + 1, int(y), palette[shade])
    else:
        raise ValueError(style)
    return img


def add_fruit(img, fruits, seed):
    """Dots fruit over a leaf sprite, only on leaves, each a shaded ball, pear or berry."""
    rng = random.Random(seed * 31 + 7)
    px = img.load()
    for light, mid, dark, rx, ry, count in fruits:
        placed = 0
        for _ in range(count * 40):
            if placed >= count:
                break
            cx, cy = rng.uniform(3, LEAF_SIZE - 3), rng.uniform(3, LEAF_SIZE - 3)
            cells = [(x, y) for y in range(int(cy - ry - 1), int(cy + ry + 2))
                     for x in range(int(cx - rx - 1), int(cx + rx + 2))
                     if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1]
            if not cells or any(not (0 <= x < LEAF_SIZE and 0 <= y < LEAF_SIZE)
                                or px[x, y][3] == 0 for x, y in cells):
                continue
            for x, y in cells:
                u, v = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
                c = light if u + v < -0.6 else dark if u + v > 0.7 else mid
                px[x, y] = c + (255,)
            placed += 1
    return img


# ------------------------------------------------------------------------------------------
# Palm crowns: a 64 px sheet of four 32 px cells (TreePalmGeometry's regions)
# ------------------------------------------------------------------------------------------
FROND_GREEN = [(118, 150, 70), (96, 128, 56), (76, 106, 46), (58, 84, 36)]
FROND_FEATHER = [(124, 156, 72), (100, 134, 58), (80, 112, 48), (70, 92, 40)]
FROND_DEAD = [(196, 170, 118), (172, 146, 96), (146, 120, 76), (118, 94, 60)]
FROND_SABAL = [(100, 134, 74), (80, 114, 62), (62, 94, 50), (46, 74, 40)]
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


def _feather_frond(px, ox, oy, palette, rng, pitch=2, lift=0.45):
    """A pinnate frond: a midrib bottom to top, leaflets angled toward the tip (by lift), one
    pair every pitch rows."""
    cx = ox + 16
    for y in range(oy, oy + 32):
        px[cx, y] = palette[3] + (255,)
    for y in range(oy + 30, oy + 1, -pitch):
        t = (oy + 31 - y) / 31.0
        length = 15 * math.sin(math.pi * min(1.0, t * 1.1 + 0.08)) ** 0.7
        for side in (-1, 1):
            shade = rng.randrange(3)
            for k in range(1, int(length) + 1):
                x = cx + side * k
                yy = y - int(k * lift) + (1 if k > length * 0.7 else 0)
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


FROND_CANARY = [(98, 134, 62), (80, 114, 50), (62, 94, 40), (50, 74, 32)]
FROND_COCONUT = [(156, 176, 80), (132, 156, 66), (108, 132, 52), (88, 108, 42)]
FROND_KING = [(112, 154, 70), (90, 134, 58), (72, 112, 48), (58, 90, 38)]
FROND_DEAD_FEATHER = [(170, 136, 92), (146, 112, 74), (122, 92, 60), (98, 74, 48)]
YUCCA_LIVE = [(156, 176, 134), (128, 152, 112), (104, 128, 94), (80, 104, 76)]
YUCCA_DEAD = [(168, 152, 122), (142, 126, 100), (116, 102, 80), (90, 78, 62)]


def _diamond_boot(px, ox, oy, rng):
    """The Canary Island date palm's "pineapple": trimmed leaf bases in diamonds, lit above."""
    for y in range(oy, oy + 32):
        for x in range(ox, ox + 32):
            u, v = x - ox, y - oy
            a, b = (u + v) % 8, (u - v) % 8
            c = (128, 108, 78)
            if a == 0 or b == 0:
                c = (84, 66, 46)
            elif a == 1 or b == 7:
                c = (156, 136, 100)
            elif a >= 6 or b <= 2:
                c = (110, 92, 66)
            px[x, y] = tuple(max(0, min(255, ch + rng.randint(-6, 6)))
                             for ch in c) + (255,)


def _crownshaft(px, ox, oy, rng):
    """The king palm's crownshaft: smooth, waxy, bright green, faintly streaked lengthwise."""
    streak = [rng.randint(-10, 10) for _ in range(32)]
    for y in range(oy, oy + 32):
        for x in range(ox, ox + 32):
            u = x - ox
            c = (118, 168, 74)
            px[x, y] = tuple(max(0, min(255, ch + streak[u] + rng.randint(-4, 4)))
                             for ch in c) + (255,)


def _shaggy_boot(px, ox, oy, rng):
    """The Joshua tree's thatch of dead leaves round the end of a branch."""
    for y in range(oy, oy + 32):
        for x in range(ox, ox + 32):
            px[x, y] = (66, 58, 48, 255)
    for row in range(0, 32, 3):
        for x0 in range(rng.randrange(4), 32, 3):
            light = rng.randint(120, 160)
            for k in range(rng.randint(4, 7)):
                x, y = ox + (x0 + k // 3) % 32, oy + (row + k) % 32
                g = light - k * 7
                px[x, y] = (g, g - 10, g - 26, 255)


def _coconuts(px, ox, oy, rng):
    """A coconut's husk: green, ripening to yellow-brown in patches."""
    for y in range(oy, oy + 32):
        for x in range(ox, ox + 32):
            u, v = x - ox, y - oy
            c = (112, 140, 52) if ((u // 6) + (v // 5)) % 3 else (164, 142, 62)
            edge = u % 16 in (0, 15) or v % 16 in (0, 15)
            if edge:
                c = (84, 104, 40)
            px[x, y] = tuple(max(0, min(255, ch + rng.randint(-8, 8))) for ch in c) + (255,)


def _daggers(px, ox, oy, palette, rng, ragged):
    """Three stiff, pointed yucca leaves from the cell's bottom edge, fanning slightly."""
    for base_x, tip_x in ((7, 3), (16, 16), (25, 29)):
        tip_y = rng.randint(4, 9) if ragged else rng.randint(0, 2)
        for y in range(oy + tip_y, oy + 32):
            t = (oy + 31 - y) / float(31 - tip_y)
            cx = base_x + (tip_x - base_x) * t
            half = 4.2 * (1 - t) + 0.4
            for x in range(int(cx - half - 1), int(cx + half + 2)):
                d = abs(x + 0.5 - cx)
                if d > half or not 0 <= x < 32:
                    continue
                shade = 0 if d < half * 0.3 else 1 if d < half * 0.75 else 2
                if ragged and rng.random() < 0.2:
                    shade = 3
                px[ox + x, y] = palette[shade] + (255,)


BANANA_LEAF = [(136, 184, 70), (114, 164, 56), (92, 140, 46), (70, 112, 38)]
BANANA_DRY = [(176, 150, 98), (150, 124, 78), (122, 98, 62), (96, 76, 48)]


def _paddle(px, ox, oy, palette, rng, torn):
    """A banana leaf: a long paddle either side of a pale midrib, torn across into strips."""
    cx = ox + 16
    # Tears run from the edge to the midrib, square across the blade, on one side at a time.
    tears = {v: rng.choice((-1, 1)) for v in rng.sample(range(5, 28), 7 if torn else 3)}
    for y in range(oy, oy + 32):
        v = y - oy
        t = (31 - v) / 31.0
        # Nearly the same width all along: narrowing at the stalk, rounded at the tip.
        tip = 1.0 if t < 0.82 else math.sqrt(max(0.0, 1 - ((t - 0.82) / 0.18) ** 2))
        half = 15 * min(1.0, (t + 0.03) * 5) * tip
        for x in range(ox, ox + 32):
            d = abs(x + 0.5 - cx)
            if d > half:
                continue
            if torn and v in tears and d > 1.5 and (x + 0.5 - cx) * tears[v] > 0:
                continue
            if not torn and d > 1.5 and int(d) % 3 == 0:
                continue  # a dry leaf is shredded lengthwise, into ribbons
            if d < 1:
                c = palette[0]
            else:
                c = palette[1 if (d // 2) % 2 else 2]
                if d > half - 1:
                    c = palette[3]
            px[x, y] = tuple(max(0, min(255, ch + rng.randint(-5, 5))) for ch in c) + (255,)


def _banana_fruit(px, ox, oy, rng):
    """Top half: a bunch of green bananas, row on row of curved fingers. Bottom half: the
    purple bell (the male flower bud) that hangs below the bunch."""
    for y in range(oy, oy + 16):
        for x in range(ox, ox + 32):
            u, v = x - ox, y - oy
            c = (126, 164, 66) if (u % 4) else (84, 116, 46)
            if v % 8 == 7:
                c = (70, 92, 40)
            px[x, y] = tuple(max(0, min(255, ch + rng.randint(-6, 6))) for ch in c) + (255,)
    for y in range(oy + 16, oy + 32):
        for x in range(ox, ox + 32):
            u = x - ox
            c = (112, 42, 76) if (u // 4 + (y - oy) // 4) % 2 else (90, 30, 62)
            px[x, y] = tuple(max(0, min(255, ch + rng.randint(-6, 6))) for ch in c) + (255,)


PLUMERIA_LEAF = [(98, 150, 64), (76, 128, 50), (58, 106, 40), (44, 84, 32)]


def _oblong_leaf(px, ox, oy, palette, rng):
    """A plumeria leaf: long, oblong and blunt, a pale midrib and parallel veins."""
    cx = ox + 16
    for y in range(oy, oy + 32):
        v = y - oy
        t = (31 - v) / 31.0
        tip = 1.0 if t < 0.8 else math.sqrt(max(0.0, 1 - ((t - 0.8) / 0.2) ** 2))
        half = 13 * min(1.0, (t + 0.05) * 3.5) * tip
        for x in range(ox, ox + 32):
            d = abs(x + 0.5 - cx)
            if d > half:
                continue
            c = palette[0] if d < 1 else palette[3] if d > half - 1 else \
                palette[2] if (v + int(d)) % 5 == 0 else palette[1]
            px[x, y] = tuple(max(0, min(255, ch + rng.randint(-4, 4))) for ch in c) + (255,)


def _plumeria_flowers(px, ox, oy, rng):
    """Plumeria flowers, seen from above: five broad white petals turned like a pinwheel round
    a yellow throat, four to a cell."""
    for fx, fy in ((9, 9), (23, 10), (10, 23), (23, 23)):
        a0 = rng.uniform(0, 2 * math.pi)
        for y in range(oy + fy - 7, oy + fy + 8):
            for x in range(ox + fx - 7, ox + fx + 8):
                dx, dy = x + 0.5 - (ox + fx), y + 0.5 - (oy + fy)
                r = math.hypot(dx, dy)
                ang = (math.atan2(dy, dx) - a0) % (2 * math.pi / 5)
                # Each petal is widest a little off its centre line: the pinwheel twist.
                petal = 6.5 * math.sin(math.pi * min(1.0, ang / (2 * math.pi / 5) * 1.1))
                if r > petal + 0.5 or r > 7:
                    continue
                if r < 1.8:
                    c = (246, 206, 62)
                elif r < 3.2:
                    c = (252, 236, 150)
                else:
                    c = (252, 250, 244) if (x + y) % 3 else (236, 232, 222)
                px[x, y] = c + (255,)


def palm_sheet(style, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    if style == "plumeria":
        _oblong_leaf(px, 0, 0, PLUMERIA_LEAF, rng)
        _plumeria_flowers(px, 32, 0, rng)
        for y in range(32, 64):
            for x in range(0, 32):
                g = (150, 152, 140) if (y % 6) else (130, 132, 120)
                px[x, y] = tuple(ch + rng.randint(-5, 5) for ch in g) + (255,)
        return img
    if style == "banana":
        _paddle(px, 0, 0, BANANA_LEAF, rng, torn=True)
        _paddle(px, 32, 0, BANANA_DRY, rng, torn=False)
        for y in range(32, 64):
            for x in range(0, 32):
                g = (108, 142, 70) if x % 4 else (88, 120, 56)
                px[x, y] = tuple(ch + rng.randint(-6, 6) for ch in g) + (255,)
        _banana_fruit(px, 32, 32, rng)
        return img
    if style == "canary":
        # Stiff, dense leaflets in a V; the boot is the pineapple of trimmed leaf bases.
        _feather_frond(px, 0, 0, FROND_CANARY, rng, lift=0.6)
        _diamond_boot(px, 0, 32, rng)
        return img
    if style == "coconut":
        _feather_frond(px, 0, 0, FROND_COCONUT, rng, lift=0.3)
        _feather_frond(px, 32, 0, FROND_DEAD_FEATHER, rng, lift=0.15)
        _boot(px, 0, 32, rng)
        _coconuts(px, 32, 32, rng)
        return img
    if style == "king":
        # Long leaflets standing nearly square to the rachis; the boot is the crownshaft.
        _feather_frond(px, 0, 0, FROND_KING, rng, lift=0.15)
        _crownshaft(px, 0, 32, rng)
        return img
    if style == "joshua":
        _daggers(px, 0, 0, YUCCA_LIVE, rng, ragged=False)
        _daggers(px, 32, 0, YUCCA_DEAD, rng, ragged=True)
        _shaggy_boot(px, 0, 32, rng)
        return img
    if style == "fan":
        _fan_frond(px, 0, 0, FROND_GREEN, rng, ragged=False)
    elif style == "cabbage":
        _fan_frond(px, 0, 0, FROND_SABAL, rng, ragged=False)
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
        sprite = leaf_cluster(style, palette, seed)
        if leaf_id in FRUIT:
            sprite = add_fruit(sprite, FRUIT[leaf_id], seed_index[leaf_id])
        save_png(os.path.join(assets, rel), sprite)
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
