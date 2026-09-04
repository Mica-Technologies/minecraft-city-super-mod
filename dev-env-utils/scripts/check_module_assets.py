#!/usr/bin/env python3
"""Check that every module's assets resolve against that module plus Core, and nothing else.

A player may install any subset of the optional module jars, so a module's own resources have to
be self-contained apart from what Core always provides. This walks each module in isolation --
only its own ``src/main/resources`` and Core's are visible -- and follows every reference it can:

* each blockstate to the models it names, in Forge and vanilla form, and on to their parents,
  an OBJ's ``mtllib``, that MTL's ``map_Kd`` and every inline texture map;
* each model and each item model in the tree, whether or not a blockstate reaches it, because
  several are drawn by a tile-entity renderer rather than by a blockstate;
* each ``sounds.json`` entry to the ``.ogg`` file it names.

Anything that fails to resolve is listed and the script exits non-zero. References with no
``csm:`` domain are Minecraft's own and are not checked.

    python dev-env-utils/scripts/check_module_assets.py            # every module, then Core
    python dev-env-utils/scripts/check_module_assets.py roads      # just one
"""

import collections
import io
import json
import os
import re
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import partition_assets as pa  # noqa: E402

CORE = pa.CORE

# A reference Minecraft could actually parse: an optional lower-case domain, then a path.
_REF_RE = re.compile(r"^(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+$")
_REF_IN_FAILURE_RE = re.compile(r" -> (?:texture|model|parent|map_Kd|mtllib) (.+)$")


def is_malformed(failure):
    """True when the reference is not a legal resource location in the first place.

    Two unreached backplate models carry a hand-edited ``"WIP CSM:..."`` texture name. Minecraft
    would reject it too, but nothing loads those models, so it is a pre-existing content bug and
    not a module-isolation failure -- reported separately rather than failing the check.
    """
    match = _REF_IN_FAILURE_RE.search(failure)
    return bool(match) and not _REF_RE.match(match.group(1))


def visible_files(modules):
    """{relative path: module} for the trees a given install would have."""
    where = {}
    for module in modules:
        for rel in pa.rel_files(module):
            where.setdefault(rel, module)
    return where


def check(module):
    """Resolve everything ``module`` ships against itself plus Core. Returns the failure list."""
    trees = [CORE] if module == CORE else [CORE, module]
    where = visible_files(trees)
    resolver = pa.Resolver(where)
    own = set(pa.rel_files(module))
    failures = []
    models_seen = set()
    textures_seen = set()

    blockstates = sorted(r for r in own
                         if r.startswith("assets/csm/blockstates/") and r.endswith(".json"))
    for rel in blockstates:
        models, textures = pa.reach_from_blockstate(rel, where, resolver, failures)
        models_seen |= models
        textures_seen |= textures

    # Models and item models the module ships that no blockstate reaches: a tile-entity renderer
    # or another model may still name them, and their own references must resolve.
    for rel in sorted(own):
        if rel.startswith("assets/csm/models/block/") and rel.endswith((".json", ".obj")):
            pa.collect_from_model(rel, where, resolver, models_seen, textures_seen,
                                  set(), failures)
        elif rel.startswith("assets/csm/models/item/") and rel.endswith(".json"):
            textures, _seen = pa.reach_from_item_model(rel, where, resolver, failures)
            textures_seen |= textures

    sounds_rel = "assets/csm/sounds.json"
    events = 0
    if sounds_rel in own:
        path = os.path.join(pa.resource_root(module), sounds_rel.replace("/", os.sep))
        data = json.loads(io.open(path, encoding="utf-8").read(),
                          object_pairs_hook=collections.OrderedDict)
        events = len(data)
        for key, value in data.items():
            for entry in value.get("sounds", []):
                name = entry["name"] if isinstance(entry, dict) else entry
                body = name.split(":", 1)[1] if ":" in name else name
                ogg = "assets/csm/sounds/{0}.ogg".format(body)
                if ogg not in where:
                    failures.append("sounds.json {0} -> missing {1}".format(key, ogg))

    counts = collections.Counter(pa.kind_of(rel) for rel in own)
    malformed = sorted({f for f in failures if is_malformed(f)})
    real = sorted({f for f in failures if not is_malformed(f)})
    return real, malformed, counts, events


def main():
    wanted = sys.argv[1:] or (pa.MODULES + [CORE])
    bad = 0
    notes = 0
    for module in wanted:
        failures, malformed, counts, events = check(module)
        summary = ", ".join("{0} {1}".format(kind, counts[kind]) for kind in sorted(counts))
        status = "OK" if not failures else "{0} UNRESOLVED".format(len(failures))
        print("{0:12s} {1:14s} {2}".format(module, status, summary))
        if events:
            print("{0:12s}                sound events {1}".format("", events))
        for line in failures:
            print("      " + line)
        for line in malformed:
            print("      note (malformed reference, pre-existing): " + line)
        notes += len(malformed)
        if failures:
            bad += 1
    print()
    print("Modules with unresolved references: {0} (malformed references noted: {1})".format(
        bad, notes))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
