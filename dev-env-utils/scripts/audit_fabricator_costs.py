#!/usr/bin/env python3
"""Audit the Fabricator cost model statically, without launching the game.

Replicates the rules in materials/CsmFabricatorCosts.java against the block index, resolving full
ancestry chains so the Java `instanceof` checks are reproduced faithfully rather than approximated
by direct superclass.

Use it to sanity check that costs make sense for what each block actually is:

    python3 dev-env-utils/scripts/audit_fabricator_costs.py            # summary by cost
    python3 dev-env-utils/scripts/audit_fabricator_costs.py --list POLE_SECTION
    python3 dev-env-utils/scripts/audit_fabricator_costs.py --grep pole
"""

import collections
import io
import sys

import csm_block_index as index_mod

# Tabs whose blocks are never fabricable.
NON_FABRICABLE_TABS = {"tabnone", "tabmaterials"}


def build_ancestry(classes):
    """Return {class_name: set_of_all_ancestor_class_names_including_itself}."""
    ancestry = {}

    def resolve(name, seen):
        if name in ancestry:
            return ancestry[name]
        if name in seen:  # defensive: a cycle would otherwise recurse forever
            return {name}
        seen.add(name)
        chain = {name}
        info = classes.get(name)
        if info and info.get("extends"):
            chain |= resolve(info["extends"], seen)
        ancestry[name] = chain
        return chain

    for class_name in list(classes):
        resolve(class_name, set())
    return ancestry


import os
import re

LANG = os.path.join(index_mod.REPO_ROOT, "src", "main", "resources", "assets", "csm", "lang",
                    "en_us.lang")

POLE_NOUNS = {"pole", "mast", "crossarm", "standard", "post"}
MOUNT_NOUNS = {"mount", "bracket", "backplate", "cover", "visor", "clamp", "hanger", "adapter",
               "coupler", "cap", "top", "base", "plate", "arm"}
OPTICAL_WORDS = ["camera", "alpr", "radar", "lidar"]
METAL_FURNITURE_WORDS = ["hydrant", "anchor", "chain", "chains", "barbed", "radiator", "grill",
                         "grate", "rail"]
ELECTRONIC_NOVELTY_WORDS = ["record", "player", "jukebox", "radio", "television", "tv"]


def load_display_names():
    """Mirror of CsmBlockDisplayNames: registry name -> normalized display name."""
    names = {}
    with io.open(LANG, encoding="utf-8") as handle:
        for line in handle:
            match = re.match(r"tile\.([^=]+)\.name=(.*)", line.strip())
            if match:
                names[match.group(1)] = re.sub(r"\([^)]*\)|\[[^\]]*\]", " ",
                                              match.group(2)).strip().lower()
    return names


DISPLAY = load_display_names()


def last_word(registry):
    words = re.findall(r"[a-z]+", DISPLAY.get(registry, ""))
    return words[-1] if words else ""


def words_of(registry):
    """Whole words of the display name.

    Token matching rather than substring, mirroring CsmBlockDisplayNames.hasWord: it is what
    keeps "alarm" from matching "arm" and "christmas" from matching "mast".
    """
    return set(re.findall(r"[a-z]+", DISPLAY.get(registry, "")))


def has_word(registry, word):
    return word in words_of(registry)


def has_any(registry, words):
    return any(has_word(registry, w) for w in words)


def _metal_dye(registry):
    if has_word(registry, "iridescent"):
        return "prismarine_crystals"
    if has_word(registry, "light") and has_word(registry, "blue"):
        return "dye:lightblue"
    for colour in ("black", "red", "green", "blue", "purple", "silver", "pink", "lime",
                   "yellow", "magenta", "orange", "copper"):
        if has_word(registry, colour):
            return "dye:" + colour
    return "dye:white"


def cost_for(registry, info, ancestors):
    """Mirror of CsmFabricatorCosts.getCost, returning a tuple of ingredient labels or None."""
    tab = info["tab"]
    if tab in NON_FABRICABLE_TABS:
        return None

    def has(*names):
        return any(n in ancestors for n in names)

    noun = last_word(registry)

    if noun in POLE_NOUNS:
        if has_word(registry, "concrete"):
            return ("POLE_SECTION", "CONCRETE_MIX")
        if has_word(registry, "wood") or has_word(registry, "wooden") or noun == "crossarm":
            return ("planks x2", "FASTENER_KIT")
        return ("POLE_SECTION x2",)

    if tab == "tabroadsigns":
        return ("SIGN_BLANK",)

    if tab == "tabbuildingmaterials":
        if has_word(registry, "metal"):
            dye = _metal_dye(registry)
            if has("AbstractBlockSlab"):
                return ("SHEET_METAL", dye)
            if has("AbstractBlockStairs"):
                return ("SHEET_METAL x2", dye)
            if has("AbstractBlockFence"):
                return ("SHEET_METAL", "FASTENER_KIT", dye)
            return ("SHEET_METAL x2", dye)
        return ("CONCRETE_MIX", "clay_ball")

    if noun in MOUNT_NOUNS:
        return ("SHEET_METAL", "FASTENER_KIT")

    if has_any(registry, OPTICAL_WORDS):
        return ("OPTICAL_SENSOR", "CONTROL_BOARD")

    if tab == "tabhvac":
        return ("DUCTING", "SHEET_METAL")
    if tab == "tablifesafety":
        if has("AbstractBlockFireAlarmSounder", "AbstractBlockFireAlarmSounderVoiceEvac"):
            return ("SOUNDER_DRIVER", "ENCLOSURE_SHELL")
        if has("AbstractBlockFireAlarmActivator"):
            return ("CONTROL_BOARD", "SHEET_METAL")
        if has("AbstractBlockFireAlarmDetector"):
            return ("OPTICAL_SENSOR", "CONTROL_BOARD")
        return ("SHEET_METAL", "WIRING_HARNESS")
    if tab == "tablighting":
        return ("LED_MODULE", "SHEET_METAL", "WIRING_HARNESS")
    if tab == "tabnovelties":
        if has_any(registry, ELECTRONIC_NOVELTY_WORDS):
            return ("CONTROL_BOARD", "SHEET_METAL")
        return ("clay_ball x2", "dye:any")
    if tab == "tabgaming":
        if has_word(registry, "arcade"):
            return ("SHEET_METAL x2", "CONTROL_BOARD", "LED_MODULE")
        if noun in ("cards", "deck") or has_word(registry, "card"):
            return ("paper x3",)
        return ("planks x2", "FASTENER_KIT")
    if tab == "tabpowergrid":
        return ("POLE_SECTION", "WIRING_HARNESS")
    if tab == "tabtechnology":
        return ("CONTROL_BOARD", "SHEET_METAL", "WIRING_HARNESS")
    if tab == "tabtrafficaccessories":
        return ("SHEET_METAL", "FASTENER_KIT")
    if tab == "tabtrafficsignals":
        if has("AbstractBlockTrafficSignalSensor", "AbstractBlockTrafficSignalSensorHZEight"):
            return ("OPTICAL_SENSOR", "CONTROL_BOARD")
        if has("BlockTrafficSignalController"):
            return ("ENCLOSURE_SHELL", "CONTROL_BOARD x2", "WIRING_HARNESS")
        if has("AbstractBlockControllableSignal"):
            return ("LED_MODULE", "LENS_ASSEMBLY", "SHEET_METAL")
        return ("SHEET_METAL", "WIRING_HARNESS")
    if tab == "tabfurniture":
        if has_any(registry, METAL_FURNITURE_WORDS):
            return ("SHEET_METAL", "FASTENER_KIT")
        return ("planks x2", "FASTENER_KIT")
    return ("SHEET_METAL", "FASTENER_KIT")


def main():
    index, _tabs, classes = index_mod.build_index()
    ancestry = build_ancestry(classes)

    priced = {}
    for registry, info in index.items():
        ancestors = set(ancestry.get(info["class"], {info["class"]}))
        # AbstractBlockSetBasic generates its fence/slab/stairs variants as inner classes, so the
        # index maps those registry names to the outer set class. At runtime the block really is a
        # BlockSetVariant*, so add the base the game would actually see or the audit misprices them.
        if "AbstractBlockSetBasic" in ancestors:
            for suffix, base in (("_slab", "AbstractBlockSlab"),
                                 ("_stairs", "AbstractBlockStairs"),
                                 ("_fence", "AbstractBlockFence")):
                if registry.endswith(suffix):
                    ancestors.add(base)
        cost = cost_for(registry, info, ancestors)
        if cost is not None:
            priced[registry] = (cost, info)

    args = sys.argv[1:]

    if "--grep" in args:
        needle = args[args.index("--grep") + 1].lower()
        rows = sorted((r, c) for r, (c, _i) in priced.items() if needle in r.lower())
        print("{0} fabricable blocks matching '{1}':".format(len(rows), needle))
        for registry, cost in rows:
            print("  {0:52s} {1}".format(registry, " + ".join(cost)))
        return

    if "--list" in args:
        needle = args[args.index("--list") + 1]
        rows = sorted(r for r, (c, _i) in priced.items() if needle in " ".join(c))
        print("{0} blocks whose cost includes {1}:".format(len(rows), needle))
        for registry in rows:
            print("  {0}".format(registry))
        return

    counter = collections.Counter(cost for cost, _info in priced.values())
    print("Fabricable blocks: {0}".format(len(priced)))
    print()
    print("Cost recipes in use, by block count:")
    for cost, count in counter.most_common():
        print("  {0:5d}  {1}".format(count, " + ".join(cost)))
    print()
    print("Distinct cost recipes: {0}".format(len(counter)))

    # Blocks whose DISPLAY name says they are one thing while their cost says another.
    #
    # This checks the last word of the display name, the same signal the cost rules use, so it
    # only reports genuine disagreements. Checking registry names here would be worse than
    # useless: they are not descriptive and have been reused across different blocks, which is
    # exactly why the cost rules stopped consulting them.
    print()
    print("Possible mismatches (display-name noun vs cost):")
    hints = {
        "pole": "POLE_SECTION",
        "mast": "POLE_SECTION",
        "crossarm": "planks",
        "duct": "DUCTING",
        "camera": "OPTICAL_SENSOR",
    }
    found_any = False
    for noun, expected in sorted(hints.items()):
        bad = sorted(r for r, (c, _i) in priced.items()
                     if last_word(r) == noun and expected not in " ".join(c))
        if bad:
            found_any = True
            print("  '{0}' without {1}: {2} blocks (e.g. {3})".format(
                noun, expected, len(bad), ", ".join(bad[:4])))
    if not found_any:
        print("  none")


if __name__ == "__main__":
    main()
