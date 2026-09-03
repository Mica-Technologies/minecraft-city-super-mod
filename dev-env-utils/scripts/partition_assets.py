#!/usr/bin/env python3
"""Partition the CSM resources between Core and the optional module jars.

Core's ``src/main/resources`` still holds nearly every asset while the Java sources already live
in ``modules/<name>/src/main/java``. This script works out which module each resource belongs to
and moves it into that module's resource tree, keeping its path inside ``assets/csm`` byte for
byte. Forge merges the ``csm`` resource domain across every jar that declares it, so a file's
resource location is unchanged by the move and nothing that references it has to be rewritten.

Design decisions this implements
--------------------------------
* **Nothing is rewritten.** Not a blockstate, not a model, not an OBJ or MTL. A file keeps its
  ``assets/csm/...`` path; only the source tree that ships it changes.
* **Shared assets go to Core, where they are.** A model or texture reached from the blockstates
  of two or more modules stays at its current path -- including the subsystem folder name, which
  may now read as misleading -- but is left in Core's tree, so every module resolves it whatever
  else is installed. Renaming those folders would mean rewriting the JSON that names them, which
  is the one thing this step must not do.
* **Ownership comes from the creative tab.** A blockstate belongs to the module that owns the tab
  its block is registered in (``MODULE_OF_TAB``), with the Redstone TTS block and its linker item
  carved out of Technology because they ship in the optional Text to Speech module.
* **Reachability decides models and textures.** Everything a blockstate can reach -- model refs
  in every Forge and vanilla blockstate form, parent chains, an OBJ's ``mtllib`` and that MTL's
  ``map_Kd``, and inline texture maps -- is owned by that blockstate's module, or by Core if two
  modules reach it.
* **Unreachable files follow their folder.** ``models/block`` and ``textures/blocks`` contain
  files no blockstate reaches (pre-existing; see the audit script). They are assigned by the
  subsystem folder they sit in, and the root-level OBJ/MTL pairs by whether their name matches a
  Furniture or Novelties block.
* **Sounds follow the module sound enums.** ``RoadsSounds``, ``LifeSafetySounds``,
  ``FurnishingsSounds``, ``TechnologySounds`` and ``HvacSounds`` are the definition of which
  module registers which sound event, so ``sounds.json`` is split along them and each ``.ogg``
  follows the entries that name it.
* **Lang follows the owner of the key.** ``tile.*`` by block, ``item.*`` by item, ``itemGroup.*``
  by tab, and the handful of ``csm.*`` keys by which module's Java formats them. The split is
  checked: Core's remaining lines plus the lines added to the modules are exactly the lines the
  file started with, in the same relative order within each destination.

Usage
-----
    python dev-env-utils/scripts/partition_assets.py --dry-run     # full plan, moves nothing
    python dev-env-utils/scripts/partition_assets.py               # git mv + write the splits
    python dev-env-utils/scripts/partition_assets.py --moves-only  # only the pure git mv step
    python dev-env-utils/scripts/partition_assets.py --content-only  # only sounds.json + lang

The two halves are separable because the move commit is meant to be pure renames (so
``git log --follow`` keeps working) and the content commit holds the split ``sounds.json`` and
lang files. Re-running is safe: anything already in its destination is skipped.
"""

import argparse
import collections
import io
import json
import os
import re
import subprocess
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import csm_block_index as cbi  # noqa: E402

REPO = cbi.REPO_ROOT
CORE = "core"

# Every optional module, in the order modules.gradle lists them.
MODULES = ["powergrid", "roads", "lifesafety", "hvac", "lighting", "building", "furnishings",
           "technology", "tts"]

# Which module ships the contents of each creative tab. Hidden tabs are named for the module
# whose classes they hold, so they map the same way.
MODULE_OF_TAB = {
    "tabbuildingmaterials": "building",
    "tabhvac": "hvac",
    "tablifesafety": "lifesafety",
    "tablighting": "lighting",
    "tablightinghidden": "lighting",
    "tabnovelties": "furnishings",
    "tabfurniture": "furnishings",
    "tabgaming": "furnishings",
    "tabpowergrid": "powergrid",
    "tabroadsigns": "roads",
    "tabtrafficaccessories": "roads",
    "tabtrafficsignals": "roads",
    "tabroadshidden": "roads",
    "tabtechnology": "technology",
    "tabmaterials": CORE,
}

# The Technology tab registers these two through initTabBlockIfLoaded/initTabItemIfLoaded because
# they ship in the Text to Speech module, which requires Technology.
BLOCK_MODULE_OVERRIDES = {"redstonetts": "tts"}
ITEM_MODULE_OVERRIDES = {"ttslinker": "tts"}

# Subsystem asset folder -> module, for files no blockstate reaches. The folder names predate the
# module split, so several map onto one module.
MODULE_OF_FOLDER = {
    "buildingmaterials": "building",
    "furniture": "furnishings",
    "novelties": "furnishings",
    "hvac": "hvac",
    "lifesafety": "lifesafety",
    "lighting": "lighting",
    "materials": CORE,
    "powergrid": "powergrid",
    "technology": "technology",
    "trafficaccessories": "roads",
    "trafficsignals": "roads",
    "trafficsigns": "roads",
}

# Whole subtrees whose owner is not worth deriving.
FIXED_TREES = [
    ("assets/csm/fonts", "roads"),            # the guide-sign legend font, read by CsmFontRenderer
    ("assets/csm/textures/fonts", "roads"),   # ...and its glyph sheet
    ("marytts", "tts"),                       # the MaryTTS engine config
]
# Files that stay in Core no matter what reaches them.
FIXED_CORE = [
    "assets/csm/recipes",          # only Core's mod container is asked for recipes (F5)
    "assets/csm/textures/csm_icon.png",  # every module jar carries its own copy already
]

# The module sound enums, as (module, path-relative-to-repo).
SOUND_ENUMS = [
    ("roads", "modules/roads/src/main/java/com/micatechnologies/minecraft/csm/trafficsignals/"
              "RoadsSounds.java"),
    ("lifesafety", "modules/lifesafety/src/main/java/com/micatechnologies/minecraft/csm/"
                   "lifesafety/LifeSafetySounds.java"),
    ("furnishings", "modules/furnishings/src/main/java/com/micatechnologies/minecraft/csm/"
                    "novelties/FurnishingsSounds.java"),
    ("technology", "modules/technology/src/main/java/com/micatechnologies/minecraft/csm/"
                   "technology/TechnologySounds.java"),
    ("hvac", "modules/hvac/src/main/java/com/micatechnologies/minecraft/csm/hvac/"
             "HvacSounds.java"),
]
_SOUND_CONST_RE = re.compile(r'([A-Z][A-Z0-9_]*)\s*\(\s*"([^"]+)"\s*\)')

# The non-tile/item/itemGroup lang keys, resolved by grepping the Java for the literal.
LANG_KEY_PREFIX_RE = re.compile(r'^(tile|item|itemGroup)\.([A-Za-z0-9_]+)\.name=')
LANG_ITEMGROUP_RE = re.compile(r'^itemGroup\.([A-Za-z0-9_]+)=')


# ---------------------------------------------------------------------------------------------
# Tree helpers
# ---------------------------------------------------------------------------------------------

def resource_root(module):
    """The ``src/main/resources`` directory of Core or a module."""
    if module == CORE:
        return os.path.join(REPO, "src", "main", "resources")
    return os.path.join(REPO, "modules", module, "src", "main", "resources")


def all_trees():
    """[(module, resources root)] for Core and every module, Core first."""
    return [(CORE, resource_root(CORE))] + [(m, resource_root(m)) for m in MODULES]


def rel_files(module):
    """Every file in one tree, as paths relative to that tree's resources root."""
    root = resource_root(module)
    out = []
    for dirpath, _dirs, filenames in os.walk(root):
        for name in filenames:
            path = os.path.join(dirpath, name)
            out.append(os.path.relpath(path, root).replace(os.sep, "/"))
    return out


def build_file_map():
    """{relative path: module} over every tree; the location of every resource today."""
    where = {}
    for module, _root in all_trees():
        for rel in rel_files(module):
            where.setdefault(rel, module)
    return where


def under(rel, prefix):
    return rel == prefix or rel.startswith(prefix + "/")


# ---------------------------------------------------------------------------------------------
# Ownership of blocks and items
# ---------------------------------------------------------------------------------------------

def block_owners():
    """{registry name: module} for every block that has a blockstate anywhere."""
    index, _tabs, classes = cbi.build_index()
    owners = {}
    problems = []
    for name, info in index.items():
        module = MODULE_OF_TAB.get(info["tab"])
        if module is None:
            problems.append("block {0}: unknown tab {1}".format(name, info["tab"]))
            continue
        override = BLOCK_MODULE_OVERRIDES.get(name)
        if override:
            module = override
        else:
            # The declaring class's own source tree must agree with the tab's module, or the
            # block is in a module the tab does not ship and needs an explicit override.
            declared = (classes.get(info["class"]) or {}).get("module")
            if declared in MODULES and declared != module:
                problems.append(
                    "block {0}: tab {1} maps to {2} but class {3} lives in {4}".format(
                        name, info["tab"], module, info["class"], declared))
        owners[name] = module

    # AbstractBlockSetBasic generates a double-slab state that no tab registers.
    for name in cbi.known_blockstates():
        if name in owners:
            continue
        if name.endswith("_slab_double") and name[:-len("_double")] in owners:
            owners[name] = owners[name[:-len("_double")]]
        else:
            problems.append("blockstate {0}: no owning tab".format(name))
    return owners, problems


def item_owners():
    """{registry name: module} for every item registered in a tab."""
    tabs, _classes = cbi.scan_item_tabs()
    owners = {}
    problems = []
    for tab_id, entries in tabs.items():
        module = MODULE_OF_TAB.get(tab_id)
        if module is None:
            problems.append("item tab {0}: unknown".format(tab_id))
            continue
        for registry, class_name in entries:
            if registry is None:
                # ItemCraftingPart takes its name from a CsmParts constant; those are Core's and
                # are covered by the models/item fallback below.
                continue
            owners[registry] = ITEM_MODULE_OVERRIDES.get(registry, module)
    return owners, problems


# ---------------------------------------------------------------------------------------------
# Reference resolution (same chain as audit_asset_ownership.py, but across every tree)
# ---------------------------------------------------------------------------------------------

class Resolver(object):
    """Resolves model and texture references to a path relative to a resources root."""

    def __init__(self, where):
        self.where = where
        self.cache = {}

    def _exists(self, rel):
        return rel in self.where

    def model(self, ref, kind="block"):
        """A ``csm:block/foo`` style reference -> ``assets/csm/models/block/foo.json`` or None.

        A blockstate's inventory variant may name an item model outright
        (``csm:item/portable_message_sign``), so an explicit ``item/`` or ``block/`` prefix wins
        over the caller's default.
        """
        body = ref.split(":", 1)[1] if ":" in ref else ref
        for prefix in ("block", "item"):
            if body.startswith(prefix + "/"):
                kind, body = prefix, body[len(prefix) + 1:]
                break
        base = "assets/csm/models/{0}/{1}".format(kind, body)
        for candidate in (base, base + ".json", base + ".obj"):
            if self._exists(candidate):
                return candidate
        return None

    def texture(self, ref):
        """A ``csm:blocks/foo`` style reference -> ``assets/csm/textures/blocks/foo.png``."""
        body = ref.split(":", 1)[1] if ":" in ref else ref
        candidate = "assets/csm/textures/{0}.png".format(body)
        return candidate if self._exists(candidate) else None


def json_of(rel, where):
    path = os.path.join(resource_root(where[rel]), rel.replace("/", os.sep))
    try:
        with io.open(path, encoding="utf-8") as handle:
            return json.load(handle)
    except Exception:
        return None


def text_of(rel, where):
    path = os.path.join(resource_root(where[rel]), rel.replace("/", os.sep))
    with io.open(path, encoding="utf-8", errors="replace") as handle:
        return handle.read()


def walk_json(node, key, out):
    """Collect every string under ``key``; ``__tex__`` means the values of a textures map."""
    if isinstance(node, dict):
        for name, value in node.items():
            if name == key and isinstance(value, str):
                out.append(value)
            elif name == "textures" and isinstance(value, dict) and key == "__tex__":
                out.extend(v for v in value.values()
                           if isinstance(v, str) and not v.startswith("#"))
            else:
                walk_json(value, key, out)
    elif isinstance(node, list):
        for value in node:
            walk_json(value, key, out)


def collect_from_model(rel, where, resolver, models, textures, seen, unresolved):
    """Follow one model to its parents, MTL, and textures. Adds to ``models``/``textures``."""
    if rel in seen:
        return
    seen.add(rel)
    models.add(rel)
    if rel.endswith(".obj"):
        directory = rel.rsplit("/", 1)[0]
        for line in text_of(rel, where).splitlines():
            if not line.startswith("mtllib"):
                continue
            mtl = "{0}/{1}".format(directory, line.split(None, 1)[1].strip())
            mtl = os.path.normpath(mtl).replace(os.sep, "/")
            if mtl not in where:
                unresolved.append("{0} -> mtllib {1}".format(rel, mtl))
                continue
            models.add(mtl)
            for mline in text_of(mtl, where).splitlines():
                if mline.startswith("map_Kd"):
                    ref = mline.split(None, 1)[1].strip()
                    resolved = resolver.texture(ref)
                    if resolved:
                        textures.add(resolved)
                    else:
                        unresolved.append("{0} -> map_Kd {1}".format(mtl, ref))
        return
    data = json_of(rel, where)
    if not data:
        return
    for value in (data.get("textures") or {}).values():
        if isinstance(value, str) and not value.startswith("#"):
            resolved = resolver.texture(value)
            if resolved:
                textures.add(resolved)
            else:
                unresolved.append("{0} -> texture {1}".format(rel, value))
    parent = data.get("parent")
    if parent:
        resolved = resolver.model(parent)
        if resolved:
            collect_from_model(resolved, where, resolver, models, textures, seen, unresolved)
        elif parent.startswith("csm:"):
            unresolved.append("{0} -> parent {1}".format(rel, parent))


def reach_from_blockstate(rel, where, resolver, unresolved):
    """(models, textures) reachable from one blockstate file."""
    data = json_of(rel, where)
    if data is None:
        return set(), set()
    refs = []
    walk_json(data, "model", refs)
    raw_textures = []
    walk_json(data, "__tex__", raw_textures)
    models, textures, seen = set(), set(), set()
    for ref in raw_textures:
        resolved = resolver.texture(ref)
        if resolved:
            textures.add(resolved)
        else:
            unresolved.append("{0} -> texture {1}".format(rel, ref))
    for ref in refs:
        resolved = resolver.model(ref)
        if resolved:
            collect_from_model(resolved, where, resolver, models, textures, seen, unresolved)
        elif ref.startswith("csm:"):
            unresolved.append("{0} -> model {1}".format(rel, ref))
    return models, textures


def reach_from_item_model(rel, where, resolver, unresolved):
    """Textures reachable from one models/item file, following its parent chain."""
    textures, seen = set(), set()
    stack = [rel]
    while stack:
        current = stack.pop()
        if current in seen:
            continue
        seen.add(current)
        data = json_of(current, where)
        if not data:
            continue
        for value in (data.get("textures") or {}).values():
            if isinstance(value, str) and not value.startswith("#"):
                resolved = resolver.texture(value)
                if resolved:
                    textures.add(resolved)
                else:
                    unresolved.append("{0} -> texture {1}".format(current, value))
        parent = data.get("parent")
        if parent and parent.startswith("csm:"):
            for kind in ("item", "block"):
                resolved = resolver.model(parent, kind)
                if resolved:
                    stack.append(resolved)
                    break
    return textures, seen


# ---------------------------------------------------------------------------------------------
# Folder-based fallback
# ---------------------------------------------------------------------------------------------

def folder_module(rel):
    """Module implied by the subsystem folder a models/block or textures/blocks file sits in."""
    for prefix in ("assets/csm/models/block/", "assets/csm/textures/blocks/"):
        if rel.startswith(prefix):
            rest = rel[len(prefix):]
            if "/" in rest:
                return MODULE_OF_FOLDER.get(rest.split("/", 1)[0])
            return None  # a root-level file: the caller decides
    return None


# ---------------------------------------------------------------------------------------------
# The plan
# ---------------------------------------------------------------------------------------------

class Plan(object):
    def __init__(self):
        self.assign = {}          # rel path -> module
        self.reason = {}          # rel path -> why
        self.shared = {}          # rel path -> set of modules that reach it
        self.by_folder = []       # (rel, module) assigned by folder name
        self.unattributed = []    # left in Core with nothing pointing at it
        self.unresolved = []      # references that resolve to no file
        self.problems = []

    def set(self, rel, module, reason):
        self.assign[rel] = module
        self.reason[rel] = reason


def build_plan():
    where = build_file_map()
    resolver = Resolver(where)
    plan = Plan()

    owners, problems = block_owners()
    plan.problems.extend(problems)
    items, item_problems = item_owners()
    plan.problems.extend(item_problems)

    # --- blockstates, and everything they reach -------------------------------------------
    reach = collections.defaultdict(set)   # asset rel -> {module, ...}
    blockstates = sorted(r for r in where if r.startswith("assets/csm/blockstates/")
                         and r.endswith(".json"))
    for rel in blockstates:
        name = rel.rsplit("/", 1)[1][:-len(".json")]
        module = owners.get(name)
        if module is None:
            plan.problems.append("blockstate {0}: no owner".format(name))
            continue
        plan.set(rel, module, "blockstate of a {0} block".format(module))
        models, textures = reach_from_blockstate(rel, where, resolver, plan.unresolved)
        for asset in models | textures:
            reach[asset].add(module)

    # --- models/item, and the item textures they reach ------------------------------------
    item_models = sorted(r for r in where if r.startswith("assets/csm/models/item/"))
    for rel in item_models:
        name = rel.rsplit("/", 1)[1]
        name = name[:-len(".json")] if name.endswith(".json") else name
        if name in owners:
            module, why = owners[name], "item model of a {0} block".format(owners[name])
        elif name in items:
            module, why = items[name], "item model of a {0} item".format(items[name])
        else:
            module, why = CORE, "item model with no module owner"
            plan.unattributed.append(rel)
        plan.set(rel, module, why)
        textures, _seen = reach_from_item_model(rel, where, resolver, plan.unresolved)
        for asset in textures:
            reach[asset].add(module)

    # --- resolve the reached set: one module keeps it, two or more leave it in Core --------
    for rel, modules in sorted(reach.items()):
        if rel in plan.assign:
            continue
        modules = {m for m in modules if m is not None}
        if len(modules) == 1:
            module = next(iter(modules))
            plan.set(rel, module, "reached only from {0}".format(module))
        else:
            plan.shared[rel] = modules
            plan.set(rel, CORE, "shared: reached from " + ", ".join(sorted(modules)))

    # --- files nothing reaches: assign by folder, OBJ/MTL pairs together -------------------
    unreached = sorted(rel for rel in where
                       if (rel.startswith("assets/csm/models/block/")
                           or rel.startswith("assets/csm/textures/blocks/"))
                       and rel not in plan.assign)
    for rel in unreached:
        module = folder_module(rel)
        if module is None:
            module = root_level_module(rel, owners)
        plan.set(rel, module, "unreached; assigned by folder")
        plan.by_folder.append((rel, module))

    # An OBJ and its MTL are one model, and a .png.mcmeta is part of its texture. Companions
    # follow whatever their principal was assigned, so a shared texture cannot lose its
    # animation to a module jar.
    for rel, module in list(plan.assign.items()):
        if rel.endswith(".obj"):
            companion = rel[:-len(".obj")] + ".mtl"
            label = "MTL of "
        elif rel.endswith(".png"):
            companion = rel + ".mcmeta"
            label = "animation of "
        else:
            continue
        if companion in where and plan.assign.get(companion) != module:
            plan.set(companion, module, label + rel.rsplit("/", 1)[1])
            plan.by_folder = [(r, m) for r, m in plan.by_folder if r != companion]

    # --- fixed trees ----------------------------------------------------------------------
    for prefix, module in FIXED_TREES:
        for rel in where:
            if under(rel, prefix):
                plan.set(rel, module, "fixed: {0} belongs to {1}".format(prefix, module))
    for prefix in FIXED_CORE:
        for rel in where:
            if under(rel, prefix):
                plan.set(rel, CORE, "fixed: {0} stays in Core".format(prefix))

    # --- everything left in Core that nobody claimed ---------------------------------------
    for rel in sorted(where):
        if rel in plan.assign:
            continue
        if where[rel] != CORE:
            continue          # already placed by an earlier step
        if rel.startswith("assets/csm/lang/") or rel.startswith("assets/csm/sounds/"):
            continue          # handled by the content split below
        if rel in ("assets/csm/sounds.json", "mcmod.info", "pack.mcmeta"):
            continue
        if rel.startswith("assets/csm/textures/items/"):
            plan.set(rel, CORE, "item texture nothing references")
            plan.unattributed.append(rel)
            continue
        plan.set(rel, CORE, "unclaimed; stays in Core")
        plan.unattributed.append(rel)

    return plan, where, owners, items


def root_level_module(rel, owners):
    """Owner of a root-level models/block or textures/blocks file no blockstate reaches.

    The root of ``models/block`` holds the crate/dollhouse OBJ family, whose file names match the
    Furniture and Novelties blocks that use them; anything else stays in Core.
    """
    stem = rel.rsplit("/", 1)[1]
    stem = re.sub(r"\.(json|obj|mtl|png|mcmeta)$", "", stem)
    module = owners.get(stem)
    if module:
        return module
    # The crate/dollhouse OBJ names drop the underscores their block ids carry.
    squashed = stem.replace("_", "")
    for candidate, owner in owners.items():
        if owner == "furnishings" and candidate.replace("_", "") == squashed:
            return "furnishings"
    return CORE


# ---------------------------------------------------------------------------------------------
# sounds.json and the .ogg files
# ---------------------------------------------------------------------------------------------

def sound_owners():
    """{sound event key: module} from the module sound enums."""
    owners = {}
    problems = []
    for module, rel in SOUND_ENUMS:
        path = os.path.join(REPO, rel.replace("/", os.sep))
        if not os.path.isfile(path):
            problems.append("missing sound enum " + rel)
            continue
        text = io.open(path, encoding="utf-8").read()
        marker = "implements ICsmSound {"
        if marker not in text:
            problems.append("no enum body in " + rel)
            continue
        body = text.split(marker, 1)[1].split(";", 1)[0]
        for _const, key in _SOUND_CONST_RE.findall(body):
            if key in owners:
                problems.append("sound {0} claimed by {1} and {2}".format(
                    key, owners[key], module))
            owners[key] = module
    return owners, problems


def split_sounds(where, apply_changes, report):
    """Split sounds.json per module and return {ogg rel path: module}."""
    # Before the split every event is in Core's file; afterwards they are spread across the
    # module files. Either way the mod's sound list is their union, which is what Minecraft
    # merges at runtime, so reading all of them makes the script re-runnable.
    core_json = "assets/csm/sounds.json"
    path = os.path.join(resource_root(CORE), core_json.replace("/", os.sep))
    sources = [path] if os.path.isfile(path) else [
        os.path.join(resource_root(module), core_json.replace("/", os.sep))
        for module in MODULES]
    newline = "\r\n"
    data = collections.OrderedDict()
    for source in sources:
        if not os.path.isfile(source):
            continue
        raw = io.open(source, "rb").read()
        newline = "\r\n" if b"\r\n" in raw else "\n"
        data.update(json.loads(raw.decode("utf-8"),
                               object_pairs_hook=collections.OrderedDict))

    owners, problems = sound_owners()
    report("SOUNDS")
    for problem in problems:
        report("  PROBLEM: " + problem)

    per_module = collections.OrderedDict()
    unowned = []
    for key, value in data.items():
        module = owners.get(key)
        if module is None:
            unowned.append(key)
            module = CORE
        per_module.setdefault(module, collections.OrderedDict())[key] = value

    ogg_owner = {}
    for module, events in per_module.items():
        for key, value in events.items():
            for entry in value.get("sounds", []):
                name = entry["name"] if isinstance(entry, dict) else entry
                body = name.split(":", 1)[1] if ":" in name else name
                rel = "assets/csm/sounds/{0}.ogg".format(body)
                if rel in where:
                    ogg_owner.setdefault(rel, module)
                else:
                    report("  PROBLEM: sounds.json {0} names missing file {1}".format(key, rel))

    for module in sorted(per_module):
        report("  {0:12s} {1:3d} events".format(module, len(per_module[module])))
    if unowned:
        report("  events with no owning enum (left in Core): " + ", ".join(unowned))

    stray = sorted(rel for rel in where
                   if rel.startswith("assets/csm/sounds/") and rel not in ogg_owner)
    if stray:
        report("  .ogg files no sounds.json entry names (stay in Core): {0}".format(len(stray)))
        for rel in stray:
            report("      " + rel)

    if apply_changes:
        for module, events in per_module.items():
            if module == CORE:
                continue
            out = os.path.join(resource_root(module), "assets", "csm", "sounds.json")
            write_json(out, events, newline)
        core_events = per_module.get(CORE, collections.OrderedDict())
        if core_events:
            write_json(path, core_events, newline)
            report("  Core keeps sounds.json with {0} events".format(len(core_events)))
        elif os.path.isfile(path):
            git(["rm", "-q", "--", os.path.relpath(path, REPO).replace(os.sep, "/")])
            report("  Core's sounds.json is now empty and has been deleted")
        else:
            report("  Core has no sounds.json, as intended")
    return ogg_owner


def write_json(path, data, newline):
    ensure_dir(os.path.dirname(path))
    text = json.dumps(data, indent=2, ensure_ascii=False)
    with io.open(path, "w", encoding="utf-8", newline=newline) as handle:
        handle.write(text + "\n")


# ---------------------------------------------------------------------------------------------
# Lang files
# ---------------------------------------------------------------------------------------------

def csm_key_owners():
    """{csm.* lang key: module} from which module's Java names the key."""
    keys = set()
    core_lang = os.path.join(resource_root(CORE), "assets", "csm", "lang", "en_us.lang")
    for line in io.open(core_lang, encoding="utf-8"):
        if "=" in line and not line.startswith(("tile.", "item.", "itemGroup.", "#")):
            keys.add(line.split("=", 1)[0].strip())
    owners = {}
    java_by_module = collections.defaultdict(list)
    for module, root in cbi.source_roots():
        for dirpath, _dirs, filenames in os.walk(root):
            for name in filenames:
                if name.endswith(".java"):
                    java_by_module[module].append(
                        io.open(os.path.join(dirpath, name), encoding="utf-8",
                                errors="replace").read())
    for key in sorted(keys):
        users = {module for module, texts in java_by_module.items()
                 if any('"' + key in text for text in texts)}
        users.discard(CORE)
        owners[key] = sorted(users)[0] if len(users) == 1 else CORE
    return owners


def split_lang(block_owner, item_owner, apply_changes, report):
    """Split every Core lang file by key owner; assert the split loses nothing."""
    lang_dir = os.path.join(resource_root(CORE), "assets", "csm", "lang")
    csm_keys = csm_key_owners()
    report("LANG")
    for key, module in sorted(csm_keys.items()):
        report("  {0:42s} -> {1}".format(key, module))

    tab_owner = {tab: module for tab, module in MODULE_OF_TAB.items()}
    totals = collections.Counter()
    for filename in sorted(os.listdir(lang_dir)):
        if not filename.endswith(".lang"):
            continue
        path = os.path.join(lang_dir, filename)
        raw = io.open(path, "rb").read()
        newline = "\r\n" if b"\r\n" in raw else "\n"
        lines = raw.decode("utf-8").split("\n")
        trailing = lines.pop() if lines and lines[-1] == "" else None
        lines = [line[:-1] if line.endswith("\r") else line for line in lines]

        buckets = collections.defaultdict(list)
        pending = []          # comments/blanks attach to the key line they precede
        for line in lines:
            match = LANG_KEY_PREFIX_RE.match(line)
            if match:
                kind, name = match.group(1), match.group(2)
                module = (block_owner.get(name, CORE) if kind == "tile"
                          else item_owner.get(name, CORE))
            else:
                group = LANG_ITEMGROUP_RE.match(line)
                if group:
                    module = tab_owner.get(group.group(1), CORE)
                elif "=" in line and not line.lstrip().startswith("#"):
                    module = csm_keys.get(line.split("=", 1)[0].strip(), CORE)
                else:
                    pending.append(line)      # a comment or a blank line
                    continue
            buckets[module].extend(pending)
            pending = []
            buckets[module].append(line)
        buckets[CORE].extend(pending)

        # The split must be lossless: same lines, same multiset.
        rebuilt = collections.Counter()
        for module in buckets:
            rebuilt.update(buckets[module])
        assert rebuilt == collections.Counter(lines), (
            "lang split lost or duplicated lines in " + filename)

        for module in sorted(buckets):
            if module != CORE:
                totals[module] += len(buckets[module])
        totals[CORE] += len(buckets[CORE])

        report("  {0}: core {1}, {2}".format(
            filename, len(buckets[CORE]),
            ", ".join("{0} {1}".format(m, len(buckets[m]))
                      for m in sorted(buckets) if m != CORE)))

        if not apply_changes:
            continue
        for module in sorted(buckets):
            if module == CORE:
                continue
            out = os.path.join(resource_root(module), "assets", "csm", "lang", filename)
            existing = []
            if os.path.isfile(out):
                existing_raw = io.open(out, "rb").read().decode("utf-8").split("\n")
                if existing_raw and existing_raw[-1] == "":
                    existing_raw.pop()
                existing = [l[:-1] if l.endswith("\r") else l for l in existing_raw]
            write_lines(out, existing + buckets[module], newline)
        write_lines(path, buckets[CORE], newline)
    report("  total lines per destination: " + ", ".join(
        "{0} {1}".format(m, totals[m]) for m in sorted(totals)))


def write_lines(path, lines, newline):
    ensure_dir(os.path.dirname(path))
    with io.open(path, "w", encoding="utf-8", newline=newline) as handle:
        handle.write("\n".join(lines) + "\n")


# ---------------------------------------------------------------------------------------------
# Applying the plan
# ---------------------------------------------------------------------------------------------

def ensure_dir(path):
    if path and not os.path.isdir(path):
        os.makedirs(path)


def git(args):
    subprocess.check_call(["git"] + args, cwd=REPO)


def apply_moves(moves, report):
    """``git mv`` every planned move, so history follows the file."""
    for source_rel, module, rel in moves:
        src = os.path.join("src", "main", "resources", rel.replace("/", os.sep))
        if source_rel != CORE:
            src = os.path.join("modules", source_rel, "src", "main", "resources",
                               rel.replace("/", os.sep))
        dst = os.path.join("modules", module, "src", "main", "resources",
                           rel.replace("/", os.sep))
        ensure_dir(os.path.dirname(os.path.join(REPO, dst)))
        git(["mv", "--", src.replace(os.sep, "/"), dst.replace(os.sep, "/")])
    report("Moved {0} files.".format(len(moves)))


# ---------------------------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--dry-run", action="store_true",
                        help="print the full plan and change nothing")
    parser.add_argument("--moves-only", action="store_true",
                        help="only perform the git mv step")
    parser.add_argument("--content-only", action="store_true",
                        help="only write the split sounds.json and lang files")
    parser.add_argument("--verbose", action="store_true",
                        help="list every planned move, not just the summary")
    args = parser.parse_args()

    out = []

    def report(line=""):
        out.append(line)
        print(line)

    plan, where, block_owner, item_owner = build_plan()

    ogg_owner = {}
    do_moves = not args.content_only
    do_content = not args.moves_only

    # sounds.json has to be parsed to know which .ogg goes where, even on a dry run.
    ogg_owner = split_sounds(where, apply_changes=(do_content and not args.dry_run), report=report)
    for rel, module in ogg_owner.items():
        plan.set(rel, module, "sound file of a {0} event".format(module))

    split_lang(block_owner, item_owner,
               apply_changes=(do_content and not args.dry_run), report=report)

    report()
    report("SHARED SET (left at its current path in Core; reached from two or more modules)")
    if not plan.shared:
        report("  none")
    for rel in sorted(plan.shared):
        report("  {0}".format(rel))
        report("      reached from: " + ", ".join(sorted(plan.shared[rel])))

    report()
    report("UNREACHED FILES ASSIGNED BY FOLDER ({0})".format(len(plan.by_folder)))
    by_module = collections.defaultdict(list)
    for rel, module in plan.by_folder:
        by_module[module].append(rel)
    for module in sorted(by_module):
        report("  {0} ({1})".format(module, len(by_module[module])))
        for rel in sorted(by_module[module]):
            report("      " + rel)

    report()
    report("LEFT IN CORE, UNATTRIBUTED ({0})".format(len(plan.unattributed)))
    for rel in sorted(plan.unattributed):
        report("  " + rel)

    if plan.unresolved:
        report()
        report("UNRESOLVED REFERENCES ({0})".format(len(plan.unresolved)))
        for line in sorted(set(plan.unresolved)):
            report("  " + line)

    if plan.problems:
        report()
        report("PROBLEMS ({0})".format(len(plan.problems)))
        for line in plan.problems:
            report("  " + line)

    # Work out the actual moves: a file only moves if it is not already in its destination.
    moves = []
    stuck = []
    for rel in sorted(plan.assign):
        target = plan.assign[rel]
        current = where.get(rel)
        if current is None or current == target:
            continue
        if current != CORE:
            stuck.append((rel, current, target))
            continue
        moves.append((current, target, rel))

    report()
    report("MOVE PLAN")
    counts = collections.Counter(target for _src, target, _rel in moves)
    for module in sorted(counts):
        report("  {0:12s} {1:5d} files".format(module, counts[module]))
    report("  {0:12s} {1:5d} files".format("TOTAL", len(moves)))
    kinds = collections.Counter()
    for _src, target, rel in moves:
        kinds[(target, kind_of(rel))] += 1
    report()
    report("  by kind:")
    for (module, kind) in sorted(kinds):
        report("    {0:12s} {1:16s} {2:5d}".format(module, kind, kinds[(module, kind)]))

    if stuck:
        report()
        report("ALREADY IN A MODULE BUT ASSIGNED ELSEWHERE ({0})".format(len(stuck)))
        for rel, current, target in stuck:
            report("  {0}: in {1}, assigned {2}".format(rel, current, target))

    if args.verbose or args.dry_run:
        report()
        report("EVERY MOVE")
        for _src, target, rel in moves:
            report("  {0:12s} {1}".format(target, rel))

    remaining = collections.Counter()
    for rel in sorted(where):
        if where[rel] != CORE:
            continue
        if plan.assign.get(rel, CORE) == CORE:
            remaining[kind_of(rel)] += 1
    report()
    report("CORE KEEPS")
    for kind in sorted(remaining):
        report("  {0:20s} {1:5d}".format(kind, remaining[kind]))

    if args.dry_run:
        report()
        report("Dry run: nothing was changed.")
        return 0

    if do_moves:
        apply_moves(moves, report)
    return 0


def kind_of(rel):
    for prefix, kind in (
            ("assets/csm/blockstates/", "blockstates"),
            ("assets/csm/models/block/", "models/block"),
            ("assets/csm/models/item/", "models/item"),
            ("assets/csm/textures/blocks/", "textures/blocks"),
            ("assets/csm/textures/items/", "textures/items"),
            ("assets/csm/textures/fonts/", "textures/fonts"),
            ("assets/csm/sounds/", "sounds"),
            ("assets/csm/fonts/", "fonts"),
            ("assets/csm/recipes/", "recipes"),
            ("assets/csm/lang/", "lang"),
            ("marytts/", "marytts")):
        if rel.startswith(prefix):
            return kind
    return "other"


if __name__ == "__main__":
    sys.exit(main())
