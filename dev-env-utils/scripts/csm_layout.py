#!/usr/bin/env python3
"""The multi-tree source layout of the mod, for the Python tooling.

CSM is built from several source trees: Core at ``src/main`` plus one per optional module under
``modules/<name>/src/main`` (see ``modules.gradle``). Every tree puts its resources under the
same ``assets/csm`` domain, because Forge merges a resource domain across every jar that declares
it. That has two consequences for any script that touches the tree:

* Anything that **reads** an asset must look at the union of the trees. A script that only opens
  ``src/main/resources/assets/csm`` now sees Core alone -- a handful of files -- and happily
  reports on a mod it never looked at.
* Anything that **writes** an asset must write it into the tree that ships it. Writing to Core
  instead would put a second copy of the file in a second jar, and which one the game loads is
  then down to classpath order.

Ownership of a block's assets follows the block: the tree holding its blockstate owns it, and for
a block whose blockstate does not exist yet, the tree holding the creative tab that registers it,
falling back to the tree holding its Java class.

Typical use::

    import csm_layout as layout

    for path in layout.asset_dirs("blockstates"):     # read the union
        ...
    out = layout.asset_for_write(layout.owner_of("mysign"), "blockstates/mysign.json")

``asset_for_write`` returns an existing copy of the file wherever it already lives, so a
generator can never create a duplicate in a second jar by nominating the wrong owner.
"""

import os
import re
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)

import csm_block_index as cbi  # noqa: E402

REPO_ROOT = cbi.REPO_ROOT

#: The module name used for Core, whose tree is the repository's own ``src/main``.
CORE = "core"

#: The Java package every source tree shares below its ``src/main/java`` root.
JAVA_PACKAGE = os.path.join("com", "micatechnologies", "minecraft", "csm")

#: The asset domain every tree writes into. Modules do not get a domain of their own.
ASSETS_CSM = os.path.join("assets", "csm")

# Which module ships the contents of each creative tab. Only needed for the handful of callers
# that reason about tabs directly; ownership of a registry name comes from the tab *file's* tree,
# which needs no table. Hidden tabs are named for the module whose classes they hold.
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

# Subsystem asset folder -> module, for a file no blockstate reaches. The folder names predate
# the module split, so several map onto one module.
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

_owner_cache = None
_sound_owner_cache = None


def modules():
    """Return every module name, ``"core"`` first then the optional modules in directory order."""
    return [CORE] + cbi.module_names()


def tree_root(module):
    """Return the folder holding the specified module's ``src/main``."""
    if module == CORE:
        return REPO_ROOT
    return os.path.join(REPO_ROOT, "modules", module)


def trees():
    """Return ``[(module, tree_root)]`` for Core and every module, Core first."""
    return [(name, tree_root(name)) for name in modules()]


def resource_roots():
    """Return ``[(module, src/main/resources)]`` for every tree that has one, Core first."""
    roots = []
    for name, root in trees():
        path = os.path.join(root, "src", "main", "resources")
        if os.path.isdir(path):
            roots.append((name, path))
    return roots


def java_roots():
    """Return ``[(module, src/main/java)]`` for every tree that has one, Core first."""
    roots = []
    for name, root in trees():
        path = os.path.join(root, "src", "main", "java")
        if os.path.isdir(path):
            roots.append((name, path))
    return roots


def java_package_roots():
    """Return ``[(module, src/main/java/com/micatechnologies/minecraft/csm)]``, Core first."""
    roots = []
    for name, root in java_roots():
        path = os.path.join(root, JAVA_PACKAGE)
        if os.path.isdir(path):
            roots.append((name, path))
    return roots


def asset_roots():
    """Return ``[(module, src/main/resources/assets/csm)]`` for every tree, Core first."""
    return cbi.resource_roots()


def _rel(relative_path):
    """Normalise a forward-slash relative path to this platform's separator."""
    return relative_path.replace("/", os.sep)


def asset_dirs(relative_path=""):
    """Return every existing folder at ``assets/csm/<relative_path>``, Core first.

    Pass ``"blockstates"`` to get every tree's blockstate folder, ``""`` for the asset roots.
    """
    dirs = []
    for _module, assets in asset_roots():
        path = os.path.join(assets, _rel(relative_path)) if relative_path else assets
        if os.path.isdir(path):
            dirs.append(path)
    return dirs


def asset_files(relative_path):
    """Return every existing file at ``assets/csm/<relative_path>``, Core first.

    Several trees legitimately carry a file of the same name: each module ships its own
    ``sounds.json`` and its own ``lang/en_us.lang``, and the game merges them.
    """
    files = []
    for _module, assets in asset_roots():
        path = os.path.join(assets, _rel(relative_path))
        if os.path.isfile(path):
            files.append(path)
    return files


def resolve_asset(relative_path):
    """Return the first tree's ``assets/csm/<relative_path>`` that exists, or None."""
    for _module, assets in asset_roots():
        path = os.path.join(assets, _rel(relative_path))
        if os.path.exists(path):
            return path
    return None


def resolve_resource(relative_path):
    """Return the first tree's ``src/main/resources/<relative_path>`` that exists, or None.

    For the few things outside the ``csm`` domain: ``pack.mcmeta``, ``marytts/``.
    """
    for _module, root in resource_roots():
        path = os.path.join(root, _rel(relative_path))
        if os.path.exists(path):
            return path
    return None


def asset_for_write(owner, relative_path):
    """Return where ``assets/csm/<relative_path>`` should be written.

    If any tree already has the file, that copy is returned whatever the nominated owner: a tool
    must never leave two jars shipping the same resource path. Otherwise the owner's tree is
    used. Parent folders are not created; the caller does that when it writes.
    """
    existing = resolve_asset(relative_path)
    if existing is not None:
        return existing
    return os.path.join(tree_root(owner), "src", "main", "resources", ASSETS_CSM,
                        _rel(relative_path))


def asset_dir_for_write(owner, relative_path=""):
    """Return the folder ``assets/csm/<relative_path>`` in the owner's tree, creating nothing.

    Unlike :func:`asset_for_write` this does not redirect to an existing tree, because a folder
    exists in several trees at once; a caller writing a *file* should use
    :func:`asset_for_write`.
    """
    base = os.path.join(tree_root(owner), "src", "main", "resources", ASSETS_CSM)
    return os.path.join(base, _rel(relative_path)) if relative_path else base


def resolve_source(relative_path):
    """Return the first tree's ``src/main/java/.../csm/<relative_path>`` that exists, or None."""
    for _module, root in java_package_roots():
        path = os.path.join(root, _rel(relative_path))
        if os.path.exists(path):
            return path
    return None


def source_dirs(relative_path=""):
    """Return every existing ``src/main/java/.../csm/<relative_path>``, Core first."""
    dirs = []
    for _module, root in java_package_roots():
        path = os.path.join(root, _rel(relative_path)) if relative_path else root
        if os.path.isdir(path):
            dirs.append(path)
    return dirs


def source_for_write(owner, relative_path):
    """Return where ``src/main/java/.../csm/<relative_path>`` should be written.

    Like :func:`asset_for_write`, an existing file wins over the nominated owner: a generated
    Java source is only ever regenerated in place.
    """
    existing = resolve_source(relative_path)
    if existing is not None:
        return existing
    return os.path.join(tree_root(owner), "src", "main", "java", JAVA_PACKAGE,
                        _rel(relative_path))


def module_of_path(path):
    """Return which tree the specified path lives in, or None."""
    target = os.path.abspath(path)
    best, best_len = None, -1
    for name, root in trees():
        root = os.path.abspath(root)
        if (target == root or target.startswith(root + os.sep)) and len(root) > best_len:
            best, best_len = name, len(root)
    return best


def blockstate_file(registry_name):
    """Return the blockstate file for the registry name, in whichever tree holds it, or None."""
    return cbi.blockstate_path(registry_name)


def known_blockstates():
    """Return every registry name that has a blockstate file in any tree."""
    return cbi.known_blockstates()


def owner_from_registration(registry_name):
    """Return the module whose creative tab registers the name, ignoring where its assets sit.

    A tab class lives in the module that ships its blocks, so the tab file's own tree is the
    answer for everything it registers. ``initTabBlockIfLoaded`` names its class as a string
    precisely because that class ships in a *different* module from the tab, so for those the
    class's own tree wins. Falls back to the tree declaring the block's or item's Java class,
    which is how a block in no tab at all resolves. Returns None if nothing registers the name.
    """
    global _owner_cache
    if _owner_cache is None:
        _owner_cache = _build_owner_map()
    return _owner_cache.get(registry_name)


def owner_of(registry_name):
    """Return the module that owns the block's or item's assets, never None.

    Resolution order: the tree holding the blockstate (authoritative once the block exists), then
    the tree that registers it, then Core -- Core being the only jar guaranteed to be installed.
    """
    path = blockstate_file(registry_name)
    if path is not None:
        module = module_of_path(path)
        if module:
            return module
    return owner_from_registration(registry_name) or CORE


def _build_owner_map():
    """Map every registry name to the module that registers it."""
    classes = cbi.scan_sources()
    owners = {}

    def add_by_class(qualified_name, module):
        info = classes.get(qualified_name.rsplit(".", 1)[-1])
        if not info:
            return
        if info["registry"]:
            owners.setdefault(info["registry"], module)
            if info["is_block_set"]:
                for suffix in cbi.BLOCK_SET_SUFFIXES:
                    owners.setdefault(info["registry"] + suffix, module)
        if info["item_registry"]:
            owners.setdefault(info["item_registry"], module)

    for module, path in cbi.tab_files():
        with open(path, "r", encoding="utf-8", errors="replace") as handle:
            text = re.sub(r"\s+", " ", handle.read())

        for pattern in (cbi._TAB_CLASS_RE, cbi._TAB_ITEM_CLASS_RE):
            for match in pattern.finditer(text):
                add_by_class(match.group(1), module)

        # The class named as a string ships in another module: its own tree owns it.
        for pattern in (cbi._TAB_CLASS_IF_LOADED_RE, cbi._TAB_ITEM_CLASS_IF_LOADED_RE):
            for match in pattern.finditer(text):
                info = classes.get(match.group(1).rsplit(".", 1)[-1])
                add_by_class(match.group(1), info["module"] if info else module)

        for pattern in (cbi._TAB_CTOR_RE, cbi._TAB_ITEM_CTOR_RE):
            for match in pattern.finditer(text):
                if match.group(2):
                    owners.setdefault(match.group(2), module)
                else:
                    add_by_class(match.group(1), module)

        for match in cbi._TAB_CONST_RE.finditer(text):
            info = classes.get(match.group(1))
            if info:
                registry = info["constants"].get(match.group(2))
                if registry:
                    owners.setdefault(registry, module)

    # Anything the tabs never named still has a home: the tree that declares its class.
    for info in classes.values():
        if info["registry"]:
            owners.setdefault(info["registry"], info["module"])
        if info["item_registry"]:
            owners.setdefault(info["item_registry"], info["module"])
    return owners


def owner_of_folder(folder_name, default=CORE):
    """Return the module a subsystem asset folder belongs to (``trafficsigns`` -> ``roads``)."""
    return MODULE_OF_FOLDER.get(folder_name, default)


def sounds_json_files():
    """Return every ``sounds.json`` in the repository, Core first; the game merges them."""
    return asset_files("sounds.json")


def owner_of_sound(sound_name):
    """Return the module whose ``sounds.json`` declares the sound event, or None.

    That is the tree the ``.ogg`` belongs in, because a module registers exactly the sound events
    its own sound enum lists.
    """
    global _sound_owner_cache
    if _sound_owner_cache is None:
        import json
        _sound_owner_cache = {}
        for path in sounds_json_files():
            module = module_of_path(path)
            with open(path, "r", encoding="utf-8") as handle:
                for event in json.load(handle):
                    _sound_owner_cache.setdefault(event, module)
    return _sound_owner_cache.get(sound_name)


def lang_files(locale="en_us"):
    """Return every ``lang/<locale>.lang`` in the repository, Core first."""
    return cbi.lang_files(locale)


def all_lang_files():
    """Return every ``.lang`` file in every tree, Core first, sorted by name within a tree."""
    files = []
    for directory in asset_dirs("lang"):
        for name in sorted(os.listdir(directory)):
            if name.endswith(".lang"):
                files.append(os.path.join(directory, name))
    return files


def locales():
    """Return every locale that has a ``.lang`` file in any tree, sorted."""
    found = set()
    for path in all_lang_files():
        found.add(os.path.basename(path)[:-len(".lang")])
    return sorted(found)


def walk_assets(relative_path="", suffixes=None):
    """Yield ``(module, absolute_path, path_relative_to_assets_csm)`` over every tree.

    ``suffixes`` filters by file extension, e.g. ``(".png",)``.
    """
    for module, assets in asset_roots():
        base = os.path.join(assets, _rel(relative_path)) if relative_path else assets
        if not os.path.isdir(base):
            continue
        for dirpath, _dirnames, filenames in os.walk(base):
            for filename in sorted(filenames):
                if suffixes and not filename.endswith(tuple(suffixes)):
                    continue
                path = os.path.join(dirpath, filename)
                yield module, path, os.path.relpath(path, assets).replace(os.sep, "/")


def main():
    """Print a summary of the layout, as a smoke test."""
    print("Repository root : {0}".format(REPO_ROOT))
    print("Modules         : {0}".format(", ".join(modules())))
    print("Resource roots  : {0}".format(len(resource_roots())))
    print("Java roots      : {0}".format(len(java_roots())))
    print("Blockstate dirs : {0}".format(len(asset_dirs("blockstates"))))
    print("Blockstates     : {0}".format(len(known_blockstates())))
    print("Lang files      : {0} in locales {1}".format(len(all_lang_files()),
                                                        ", ".join(locales())))
    print("sounds.json     : {0}".format(len(sounds_json_files())))
    counts = {}
    for name in known_blockstates():
        counts[owner_of(name)] = counts.get(owner_of(name), 0) + 1
    print("Owners          : {0}".format(
        ", ".join("{0}={1}".format(k, counts[k]) for k in sorted(counts))))


if __name__ == "__main__":
    main()
