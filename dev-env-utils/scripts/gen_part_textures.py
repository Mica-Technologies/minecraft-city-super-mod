#!/usr/bin/env python3
"""Generate the item textures, item models and vanilla-tier recipes for the CSM crafting parts.

The parts are the intermediate tier of the mod's survival crafting chain (vanilla ores and
ingots -> CSM parts -> CSM blocks). This script owns three parallel sets of generated files,
one entry per part:

  assets/csm/textures/items/<name>.png   32x32 RGBA pixel-art icon
  assets/csm/models/item/<name>.json     standard item/generated model
  assets/csm/recipes/<name>.json         forge:ore_shaped / ore_shapeless vanilla-tier recipe

Re-running the script is idempotent. Run it from anywhere:

    python3 dev-env-utils/scripts/gen_part_textures.py
"""

import json
import os

from PIL import Image, ImageDraw

# --------------------------------------------------------------------------------------
# Paths
# --------------------------------------------------------------------------------------

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, "..", ".."))
ASSETS = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(ASSETS, "textures", "items")
MODEL_DIR = os.path.join(ASSETS, "models", "item")
RECIPE_DIR = os.path.join(ASSETS, "recipes")

SIZE = 32

# --------------------------------------------------------------------------------------
# Palette
# --------------------------------------------------------------------------------------

CLEAR = (0, 0, 0, 0)
OUTLINE = (42, 46, 51, 255)

STEEL_HI = (206, 212, 218, 255)
STEEL = (154, 161, 168, 255)
STEEL_LO = (107, 114, 120, 255)

GOLD = (212, 166, 60, 255)
COPPER = (193, 104, 46, 255)

PCB_HI = (46, 139, 79, 255)
PCB_LO = (26, 92, 52, 255)

WIRE_RED = (196, 62, 52, 255)
WIRE_BLK = (58, 58, 62, 255)
WIRE_BLU = (58, 104, 178, 255)

LED_RED = (238, 62, 56, 255)
LED_AMB = (243, 168, 38, 255)
LED_GRN = (60, 198, 88, 255)
GLOW = (255, 246, 214, 255)

WHITE = (240, 241, 243, 255)
CONCRETE = (150, 150, 143, 255)
CONCRETE_LO = (110, 110, 104, 255)
DARK = (34, 36, 40, 255)
LENS_AMB = (240, 176, 60, 255)
LENS_HI = (255, 226, 160, 255)
GLASS = (150, 196, 214, 255)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), CLEAR)


# --------------------------------------------------------------------------------------
# Per-part drawing routines
# --------------------------------------------------------------------------------------


def draw_sheet_metal(d):
    """Three stacked steel sheets seen at a shallow angle."""
    for i, top in enumerate((17, 12, 7)):
        shade = (STEEL_LO, STEEL, STEEL_HI)[i]
        d.polygon([(4, top + 5), (16, top), (28, top + 5), (16, top + 10)], fill=shade,
                  outline=OUTLINE)
    d.line([(9, 10), (16, 7)], fill=WHITE)


def draw_fastener_kit(d):
    """Two large hex nuts and a bolt, drawn big enough to read at inventory size."""
    for cx, cy, r in ((11, 10, 8), (22, 19, 7)):
        d.regular_polygon((cx, cy, r), 6, rotation=0, fill=STEEL, outline=OUTLINE)
        d.regular_polygon((cx, cy, r - 3), 6, rotation=0, fill=STEEL_HI, outline=OUTLINE)
        d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=DARK, outline=OUTLINE)
    # A bolt lying across the lower left, head at the left end.
    d.rectangle([4, 23, 15, 27], fill=STEEL, outline=OUTLINE)
    for x in range(6, 15, 2):
        d.line([(x, 24), (x, 26)], fill=STEEL_LO)
    d.rectangle([2, 21, 6, 29], fill=STEEL_HI, outline=OUTLINE)


def draw_pole_section(d):
    """A vertical length of steel tube."""
    d.rectangle([11, 4, 20, 28], fill=STEEL, outline=OUTLINE)
    d.rectangle([13, 4, 15, 28], fill=STEEL_HI)
    d.rectangle([18, 4, 19, 28], fill=STEEL_LO)
    d.ellipse([11, 2, 20, 7], fill=STEEL_HI, outline=OUTLINE)
    d.ellipse([14, 4, 17, 6], fill=DARK)


def draw_enclosure_shell(d):
    """A weatherproof equipment cabinet."""
    d.rectangle([6, 5, 25, 27], fill=STEEL, outline=OUTLINE)
    d.rectangle([8, 7, 23, 25], fill=STEEL_LO, outline=OUTLINE)
    d.line([(16, 7), (16, 25)], fill=OUTLINE)
    d.rectangle([14, 14, 15, 17], fill=STEEL_HI)
    d.rectangle([17, 14, 18, 17], fill=STEEL_HI)
    d.rectangle([6, 3, 25, 5], fill=STEEL_HI, outline=OUTLINE)


def draw_wiring_harness(d):
    """A bundle of insulated conductors with a tie wrap."""
    for off, col in ((0, WIRE_RED), (4, WIRE_BLK), (8, WIRE_BLU)):
        d.line([(4, 9 + off), (12, 11 + off), (20, 15 + off), (28, 13 + off)], fill=col, width=3)
    d.rectangle([13, 8, 17, 26], fill=STEEL_LO, outline=OUTLINE)
    d.rectangle([13, 15, 17, 18], fill=STEEL_HI, outline=OUTLINE)


def draw_control_board(d):
    """A populated printed circuit board."""
    d.rectangle([4, 7, 27, 24], fill=PCB_LO, outline=OUTLINE)
    d.rectangle([6, 9, 25, 22], fill=PCB_HI)
    d.rectangle([11, 12, 19, 18], fill=DARK, outline=OUTLINE)
    for x in range(12, 20, 2):
        d.line([(x, 10), (x, 11)], fill=GOLD)
        d.line([(x, 19), (x, 20)], fill=GOLD)
    for y in (11, 14, 20):
        d.line([(7, y), (10, y)], fill=GOLD)
    d.rectangle([21, 11, 24, 13], fill=GOLD)
    d.rectangle([21, 17, 24, 21], fill=WIRE_BLK, outline=OUTLINE)


def draw_led_module(d):
    """A carrier board with a row of lit diodes."""
    d.rectangle([3, 11, 28, 21], fill=WIRE_BLK, outline=OUTLINE)
    for i, col in enumerate((LED_RED, LED_AMB, LED_GRN, LED_AMB)):
        cx = 7 + i * 6
        d.ellipse([cx - 3, 13, cx + 3, 19], fill=col, outline=OUTLINE)
        d.point((cx - 1, 15), fill=GLOW)
    d.line([(3, 22), (28, 22)], fill=GOLD)


def draw_lens_assembly(d):
    """An amber signal lens in its gasket."""
    d.ellipse([4, 4, 27, 27], fill=STEEL_LO, outline=OUTLINE)
    d.ellipse([6, 6, 25, 25], fill=LENS_AMB, outline=OUTLINE)
    d.ellipse([9, 9, 22, 22], fill=LENS_HI)
    d.ellipse([12, 12, 19, 19], fill=LENS_AMB)
    d.ellipse([10, 10, 14, 14], fill=WHITE)


def draw_sounder_driver(d):
    """A loudspeaker driver seen face on."""
    d.ellipse([3, 3, 28, 28], fill=STEEL, outline=OUTLINE)
    d.ellipse([6, 6, 25, 25], fill=STEEL_LO, outline=OUTLINE)
    d.ellipse([10, 10, 21, 21], fill=DARK, outline=OUTLINE)
    d.ellipse([13, 13, 18, 18], fill=STEEL_HI)
    for xy in ((5, 5), (25, 5), (5, 25), (25, 25)):
        d.point(xy, fill=STEEL_HI)


def draw_reflective_sheeting(d):
    """A roll of retroreflective film with a length of sheet feeding off it."""
    # The sheet feeding off to the upper right, with sheen bands across it.
    d.polygon([(15, 21), (23, 5), (29, 5), (21, 21)], fill=WHITE, outline=OUTLINE)
    for i in range(3):
        d.line([(17 + i * 3, 20), (25 + i * 2, 6)], fill=GLASS)
    # The roll itself, drawn last so it sits in front of the sheet.
    d.ellipse([3, 12, 21, 30], fill=STEEL_HI, outline=OUTLINE)
    d.ellipse([7, 16, 17, 26], fill=WHITE, outline=OUTLINE)
    d.ellipse([10, 19, 14, 23], fill=STEEL_LO, outline=OUTLINE)
    d.arc([5, 14, 19, 28], start=200, end=320, fill=GLASS)


def draw_sign_blank(d):
    """An undecorated sign panel with mounting holes."""
    d.rounded_rectangle([5, 5, 26, 26], radius=3, fill=WHITE, outline=OUTLINE)
    d.rounded_rectangle([7, 7, 24, 24], radius=2, outline=STEEL_LO)
    for xy in ((10, 10), (21, 10), (10, 21), (21, 21)):
        d.point(xy, fill=STEEL_LO)
    d.line([(8, 23), (23, 8)], fill=(255, 255, 255, 90))


def draw_concrete_mix(d):
    """A sack of dry concrete mix."""
    d.polygon([(7, 8), (24, 8), (26, 27), (5, 27)], fill=CONCRETE, outline=OUTLINE)
    d.polygon([(7, 8), (24, 8), (23, 5), (8, 5)], fill=CONCRETE_LO, outline=OUTLINE)
    d.rectangle([10, 13, 21, 20], fill=CONCRETE_LO)
    for xy in ((12, 15), (15, 17), (18, 15), (14, 19), (19, 18)):
        d.point(xy, fill=STEEL_HI)


def draw_ducting(d):
    """A horizontal run of round spiral duct, with flange ends and rib rings.

    Deliberately drawn as a ribbed cylinder rather than a stack of plates so it cannot be
    confused with the sheet metal icon.
    """
    # Barrel of the duct.
    d.rectangle([5, 10, 27, 23], fill=STEEL, outline=None)
    d.rectangle([5, 10, 27, 13], fill=STEEL_HI, outline=None)
    d.rectangle([5, 21, 27, 23], fill=STEEL_LO, outline=None)
    d.line([(5, 9), (27, 9)], fill=OUTLINE)
    d.line([(5, 24), (27, 24)], fill=OUTLINE)
    # Rib rings along the barrel.
    for x in (11, 17, 23):
        d.line([(x, 10), (x, 23)], fill=STEEL_LO)
        d.line([(x + 1, 10), (x + 1, 23)], fill=STEEL_HI)
    # Flanged ends: near end open, far end capped.
    d.ellipse([2, 8, 9, 25], fill=STEEL_HI, outline=OUTLINE)
    d.ellipse([4, 11, 7, 22], fill=DARK, outline=OUTLINE)
    d.ellipse([24, 8, 30, 25], fill=STEEL_LO, outline=OUTLINE)


def draw_optical_sensor(d):
    """A traffic detection camera head."""
    d.rounded_rectangle([4, 9, 27, 23], radius=3, fill=STEEL, outline=OUTLINE)
    d.rectangle([4, 9, 27, 12], fill=STEEL_HI)
    d.ellipse([15, 10, 26, 22], fill=DARK, outline=OUTLINE)
    d.ellipse([17, 12, 24, 20], fill=GLASS, outline=OUTLINE)
    d.ellipse([19, 14, 21, 16], fill=WHITE)
    d.rectangle([6, 13, 12, 19], fill=STEEL_LO, outline=OUTLINE)
    d.point((9, 16), fill=LED_RED)


# --------------------------------------------------------------------------------------
# Part table: registry name -> (drawing routine, recipe dict)
# --------------------------------------------------------------------------------------


def shaped(pattern, key, count):
    return {"type": "forge:ore_shaped", "pattern": pattern, "key": key,
            "result": {"item": "csm:PLACEHOLDER", "count": count}}


def shapeless(ingredients, count):
    return {"type": "forge:ore_shapeless", "ingredients": ingredients,
            "result": {"item": "csm:PLACEHOLDER", "count": count}}


PARTS = [
    ("part_sheet_metal", draw_sheet_metal,
     shaped(["III"], {"I": {"type": "forge:ore_dict", "ore": "ingotIron"}}, 4)),

    ("part_fastener_kit", draw_fastener_kit,
     shaped(["NN", "NN"], {"N": {"type": "forge:ore_dict", "ore": "nuggetIron"}}, 4)),

    ("part_pole_section", draw_pole_section,
     shaped(["S", "S", "S"], {"S": {"item": "csm:part_sheet_metal"}}, 2)),

    ("part_enclosure_shell", draw_enclosure_shell,
     shaped(["SSS", "SBS", "SSS"],
            {"S": {"item": "csm:part_sheet_metal"}, "B": {"item": "minecraft:iron_bars"}}, 1)),

    ("part_wiring_harness", draw_wiring_harness,
     shapeless([{"type": "forge:ore_dict", "ore": "dustRedstone"},
                {"type": "forge:ore_dict", "ore": "nuggetIron"},
                {"item": "minecraft:string"}], 2)),

    ("part_control_board", draw_control_board,
     shaped([" G ", "RQR", " G "],
            {"G": {"type": "forge:ore_dict", "ore": "nuggetGold"},
             "R": {"type": "forge:ore_dict", "ore": "dustRedstone"},
             "Q": {"type": "forge:ore_dict", "ore": "gemQuartz"}}, 1)),

    ("part_led_module", draw_led_module,
     shaped(["GRG", "PPP"],
            {"G": {"type": "forge:ore_dict", "ore": "dustGlowstone"},
             "R": {"type": "forge:ore_dict", "ore": "dustRedstone"},
             "P": {"type": "forge:ore_dict", "ore": "paneGlass"}}, 2)),

    # Metadata 32767 is OreDictionary.WILDCARD_VALUE, so any dye colour is accepted. There is no
    # bare "dye" ore dictionary entry in Forge (only per-colour dyeRed, dyeWhite, ...), so the
    # wildcard item form is used instead of forge:ore_dict here.
    ("part_lens_assembly", draw_lens_assembly,
     shapeless([{"type": "forge:ore_dict", "ore": "paneGlass"},
                {"type": "forge:ore_dict", "ore": "paneGlass"},
                {"item": "minecraft:dye", "data": 32767}], 2)),

    ("part_sounder_driver", draw_sounder_driver,
     shaped([" S ", "SNS", " R "],
            {"S": {"item": "csm:part_sheet_metal"},
             "N": {"item": "minecraft:noteblock"},
             "R": {"type": "forge:ore_dict", "ore": "dustRedstone"}}, 1)),

    ("part_reflective_sheeting", draw_reflective_sheeting,
     shapeless([{"type": "forge:ore_dict", "ore": "gemQuartz"},
                {"type": "forge:ore_dict", "ore": "dyeWhite"}], 2)),

    ("part_sign_blank", draw_sign_blank,
     shapeless([{"item": "csm:part_sheet_metal"},
                {"item": "csm:part_reflective_sheeting"}], 2)),

    # Sand is used via the ore dictionary rather than as minecraft:sand: it has subtypes
    # (regular and red), and Forge's CraftingHelper rejects a subtyped item given without
    # explicit metadata ("Missing data for item 'minecraft:sand'"). The ore entry covers both
    # colours and other mods' sands too.
    ("part_concrete_mix", draw_concrete_mix,
     shapeless([{"type": "forge:ore_dict", "ore": "gravel"},
                {"type": "forge:ore_dict", "ore": "sand"},
                {"item": "minecraft:clay_ball"}], 4)),

    ("part_ducting", draw_ducting,
     shaped([" S ", "S S", " S "], {"S": {"item": "csm:part_sheet_metal"}}, 4)),

    ("part_optical_sensor", draw_optical_sensor,
     shapeless([{"item": "csm:part_lens_assembly"},
                {"item": "csm:part_control_board"}], 1)),
]


# --------------------------------------------------------------------------------------
# Emit
# --------------------------------------------------------------------------------------


# Forge ore dictionary entries this script is allowed to reference. Forge registers these in
# OreDictionary.initVanillaEntries(); anything outside this set would make the recipe fail to
# load silently at runtime, which the Gradle build cannot catch because JSON recipes are only
# parsed when the game starts.
KNOWN_ORES = {
    "ingotIron", "ingotGold", "nuggetIron", "nuggetGold", "dustRedstone", "dustGlowstone",
    "gemQuartz", "paneGlass", "blockGlass", "dyeWhite", "dyeBlack", "dyeYellow", "dyeRed",
    "sand", "gravel",
}

# Vanilla item ids referenced by the part recipes.
KNOWN_VANILLA = {
    "minecraft:iron_bars", "minecraft:string", "minecraft:noteblock", "minecraft:gravel",
    "minecraft:sand", "minecraft:clay_ball", "minecraft:dye",
}

# Vanilla items whose getHasSubtypes() is true. Forge's CraftingHelper.getItemStackBasic throws
# "Missing data for item '<id>'" if one of these is used as an ingredient without an explicit
# "data" field, which silently drops the whole recipe at load time. Use data 32767
# (OreDictionary.WILDCARD_VALUE) to accept any subtype.
SUBTYPED_VANILLA = {
    "minecraft:sand", "minecraft:dye", "minecraft:wool", "minecraft:planks", "minecraft:log",
    "minecraft:stone", "minecraft:stained_glass", "minecraft:stained_glass_pane",
    "minecraft:concrete", "minecraft:concrete_powder",
}


def validate():
    """Check every ingredient resolves to something that will exist at runtime."""
    part_ids = {"csm:" + name for name, _, _ in PARTS}
    errors = []

    def check_ingredient(ing, where):
        if "type" in ing and ing["type"] == "forge:ore_dict":
            if ing["ore"] not in KNOWN_ORES:
                errors.append("{0}: unknown ore dictionary entry '{1}'".format(where, ing["ore"]))
        elif "item" in ing:
            item = ing["item"]
            if item.startswith("csm:"):
                if item not in part_ids:
                    errors.append("{0}: references unknown CSM item '{1}'".format(where, item))
            elif item not in KNOWN_VANILLA:
                errors.append("{0}: references unvetted vanilla item '{1}'".format(where, item))
            if item in SUBTYPED_VANILLA and "data" not in ing:
                errors.append(
                    "{0}: '{1}' has subtypes and needs an explicit \"data\" field, or Forge "
                    "will drop the recipe at load time".format(where, item))
        else:
            errors.append("{0}: ingredient has neither 'type' nor 'item'".format(where))

    for name, _, recipe in PARTS:
        if recipe["type"] == "forge:ore_shaped":
            width = {len(row) for row in recipe["pattern"]}
            if len(width) != 1:
                errors.append("{0}: pattern rows are not all the same width".format(name))
            used = {ch for row in recipe["pattern"] for ch in row if ch != " "}
            declared = set(recipe["key"])
            for missing in used - declared:
                errors.append("{0}: pattern uses '{1}' with no key entry".format(name, missing))
            for unused in declared - used:
                errors.append("{0}: key declares '{1}' unused by pattern".format(name, unused))
            for symbol, ing in recipe["key"].items():
                check_ingredient(ing, "{0} key '{1}'".format(name, symbol))
        else:
            for i, ing in enumerate(recipe["ingredients"]):
                check_ingredient(ing, "{0} ingredient {1}".format(name, i))

    if errors:
        for err in errors:
            print("ERROR: " + err)
        raise SystemExit("Recipe validation failed with {0} error(s).".format(len(errors)))
    print("Validated {0} recipes; all ingredients resolve.".format(len(PARTS)))


def main():
    validate()

    for directory in (TEX_DIR, MODEL_DIR, RECIPE_DIR):
        os.makedirs(directory, exist_ok=True)

    for name, draw_fn, recipe in PARTS:
        img = new_img()
        draw_fn(ImageDraw.Draw(img))
        img.save(os.path.join(TEX_DIR, name + ".png"))

        model = {"parent": "item/generated", "textures": {"layer0": "csm:items/" + name}}
        with open(os.path.join(MODEL_DIR, name + ".json"), "w", encoding="utf-8") as handle:
            json.dump(model, handle, indent=2)
            handle.write("\n")

        recipe = json.loads(json.dumps(recipe))  # deep copy so re-runs stay clean
        recipe["result"]["item"] = "csm:" + name
        with open(os.path.join(RECIPE_DIR, name + ".json"), "w", encoding="utf-8") as handle:
            json.dump(recipe, handle, indent=2)
            handle.write("\n")

    print("Generated {0} textures, models and recipes.".format(len(PARTS)))
    print("  textures -> {0}".format(TEX_DIR))
    print("  models   -> {0}".format(MODEL_DIR))
    print("  recipes  -> {0}".format(RECIPE_DIR))


if __name__ == "__main__":
    main()
