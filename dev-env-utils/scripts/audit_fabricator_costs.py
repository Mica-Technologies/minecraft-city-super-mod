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


ELECTRONIC_MARKERS = ["signal", "light", "lamp", "sign", "beacon", "strobe", "horn", "speaker",
                      "camera", "detector", "meter", "display", "message", "screen", "monitor",
                      "radar"]
MOUNTING_SUFFIXES = ["mount", "mounts", "mountkit", "bracket", "base", "backplate", "cover",
                     "visor", "hanger", "plate", "clamp"]
OPTICAL_MARKERS = ["camera", "alpr", "radar", "lidar"]
METAL_FURNITURE = ["hydrant", "anchor", "chain", "barbedwire", "radiator", "grill", "grate", "rail"]
ELECTRONIC_NOVELTY = ["record", "player", "jukebox", "radio", "arcade", "tv"]


def _any(name, frags):
    return any(f in name for f in frags)


def _ends(name, sfx):
    return any(name.endswith(s) for s in sfx)


def _metal_dye(name):
    if name.startswith("iridescent"):
        return "prismarine_crystals"
    for prefix in ("black", "red", "green", "blue", "purple", "silver", "pink", "lime",
                   "yellow", "lightblue", "magenta", "orange", "copper", "white"):
        if name.startswith(prefix):
            return "dye:" + prefix
    return "dye:white"


def structural_cost(name, ancestors):
    if "AbstractBlockTrafficPole" in ancestors or "AbstractBlockTrafficPoleDiagonal" in ancestors:
        return ("POLE_SECTION", "FASTENER_KIT")
    # Ahead of the electronic guard: "signpost" contains "sign".
    if "signpost" in name:
        return ("POLE_SECTION", "FASTENER_KIT")
    if _any(name, ELECTRONIC_MARKERS):
        return None
    if "xarm" in name:
        return ("planks x2", "FASTENER_KIT")
    if "pole" in name:
        if "concrete" in name:
            return ("POLE_SECTION", "CONCRETE_MIX")
        return ("POLE_SECTION x2",)
    if _ends(name, MOUNTING_SUFFIXES):
        return ("SHEET_METAL", "FASTENER_KIT")
    return None


def cost_for(registry, info, ancestors):
    """Mirror of CsmFabricatorCosts.getCost, returning a tuple of ingredient labels or None."""
    tab = info["tab"]
    if tab in NON_FABRICABLE_TABS:
        return None
    name = registry.lower()

    def has(*names):
        return any(n in ancestors for n in names)

    if tab == "tabroadsigns":
        if "signpost" in name:
            return ("POLE_SECTION", "FASTENER_KIT")
        return ("SIGN_BLANK",)

    if tab == "tabbuildingmaterials":
        if "metal" in name:
            dye = _metal_dye(name)
            if has("AbstractBlockSlab"):
                return ("SHEET_METAL", dye)
            if has("AbstractBlockStairs"):
                return ("SHEET_METAL x2", dye)
            if has("AbstractBlockFence"):
                return ("SHEET_METAL", "FASTENER_KIT", dye)
            return ("SHEET_METAL x2", dye)
        if has("AbstractBlockSlab"):
            return ("CONCRETE_MIX",)
        return ("CONCRETE_MIX x2",)

    structural = structural_cost(name, ancestors)
    if structural:
        return structural

    if _any(name, OPTICAL_MARKERS):
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
        if _any(name, ELECTRONIC_NOVELTY):
            return ("CONTROL_BOARD", "SHEET_METAL")
        return ("clay_ball x2", "dye:any")
    if tab == "tabgaming":
        return ("SHEET_METAL x2", "CONTROL_BOARD", "LED_MODULE")
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
        if _any(name, METAL_FURNITURE):
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

    # Blocks whose name hints at a physical form that its cost does not reflect.
    print()
    print("Possible mismatches (name hint vs cost):")
    hints = {
        "pole": "POLE_SECTION",
        "mast": "POLE_SECTION",
        "signpost": "POLE_SECTION",
        "duct": "DUCTING",
        "camera": "OPTICAL_SENSOR",
        "sensor": "OPTICAL_SENSOR",
    }
    for needle, expected in sorted(hints.items()):
        bad = sorted(r for r, (c, _i) in priced.items()
                     if needle in r.lower() and expected not in " ".join(c))
        if bad:
            print("  '{0}' without {1}: {2} blocks (e.g. {3})".format(
                needle, expected, len(bad), ", ".join(bad[:4])))


if __name__ == "__main__":
    main()
