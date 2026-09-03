#!/usr/bin/env python3
"""Diff two registry dumps written by MCMCP's ``game_dump_registries`` tool.

Usage::

    python diff_registry_dumps.py golden.json candidate.json [--ignore-tab-index] [--ignore-class]

Exit status 0 when the candidate registers exactly what the golden dump registers, 1 otherwise.
The comparison is deliberately strict about the things a modularization must not change and
lenient about the things it legitimately does:

* Every block, item, tile-entity key, sound event, recipe and entity id must be present in both,
  in the same order. Order matters: the creative inventory renders a tab by walking the item
  registry in registry order.
* Per-entry fields (translation key, display name, creative tab label, tile entity flag, item
  flag) must match. The creative tab *index* is compared only without ``--ignore-tab-index``,
  since a tab's index is its creation order, which changes when tabs move between mods.
* The class name is compared only without ``--ignore-class``: a class moving package is fine
  once the split has happened, and this flag keeps the diff readable at that stage.
* The ``mods`` list and every version string are ignored; the point is what was registered, not
  which jar it came from.
"""
import json
import sys

REGISTRIES_WITH_ENTRIES = ("blocks", "items", "tileEntities")
REGISTRIES_WITH_IDS = ("soundEvents", "recipes", "entities")


def load(path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def key_of(entry):
    return entry.get("id") or entry.get("key")


def compare_ordered_ids(name, golden_ids, candidate_ids, problems):
    golden_set, candidate_set = set(golden_ids), set(candidate_ids)
    for missing in [i for i in golden_ids if i not in candidate_set]:
        problems.append("%s: missing in candidate: %s" % (name, missing))
    for extra in [i for i in candidate_ids if i not in golden_set]:
        problems.append("%s: unexpected in candidate: %s" % (name, extra))
    common_golden = [i for i in golden_ids if i in candidate_set]
    common_candidate = [i for i in candidate_ids if i in golden_set]
    if common_golden != common_candidate:
        for position, (g, c) in enumerate(zip(common_golden, common_candidate)):
            if g != c:
                problems.append("%s: order differs from position %d: golden %s, candidate %s"
                                % (name, position, g, c))
                break


def compare_entries(name, golden, candidate, ignore_fields, problems):
    candidate_by_id = {key_of(e): e for e in candidate}
    for entry in golden:
        other = candidate_by_id.get(key_of(entry))
        if other is None:
            continue  # reported by the id comparison
        for field in sorted(set(entry) | set(other)):
            if field in ignore_fields:
                continue
            if entry.get(field) != other.get(field):
                problems.append("%s %s: %s differs: golden %r, candidate %r"
                                % (name, key_of(entry), field, entry.get(field), other.get(field)))


def main(argv):
    args = [a for a in argv if not a.startswith("--")]
    flags = set(a for a in argv if a.startswith("--"))
    if len(args) != 2:
        print(__doc__)
        return 2
    golden, candidate = load(args[0]), load(args[1])
    ignore_fields = set()
    if "--ignore-tab-index" in flags:
        ignore_fields.add("creativeTabIndex")
    if "--ignore-class" in flags:
        ignore_fields.add("class")

    problems = []
    if golden.get("namespace") != candidate.get("namespace"):
        problems.append("namespace differs: %s vs %s" % (golden.get("namespace"), candidate.get("namespace")))
    if not candidate.get("counts", {}).get("tileEntityRegistryReadable", True):
        problems.append("candidate could not read the tile entity registry; its tileEntities list is empty")

    for name in REGISTRIES_WITH_ENTRIES:
        g, c = golden.get(name, []), candidate.get(name, [])
        compare_ordered_ids(name, [key_of(e) for e in g], [key_of(e) for e in c], problems)
        compare_entries(name, g, c, ignore_fields, problems)
    for name in REGISTRIES_WITH_IDS:
        compare_ordered_ids(name, golden.get(name, []), candidate.get(name, []), problems)

    counts = golden.get("counts", {})
    print("golden: %s | candidate: %s" % (args[0], args[1]))
    print("golden counts: " + ", ".join("%s=%s" % (k, v) for k, v in counts.items()))
    if problems:
        print("%d difference(s):" % len(problems))
        for problem in problems[:200]:
            print("  " + problem)
        if len(problems) > 200:
            print("  ... %d more" % (len(problems) - 200))
        return 1
    print("IDENTICAL: every registry entry present, in the same order, with the same fields"
          + (" (ignoring %s)" % ", ".join(sorted(ignore_fields)) if ignore_fields else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
