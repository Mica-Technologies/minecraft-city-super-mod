#!/usr/bin/env python3
"""Diff two registry dumps written by MCMCP's ``game_dump_registries`` tool.

Usage::

    python diff_registry_dumps.py golden.json candidate.json [--ignore-tab-index] [--ignore-class] [--unordered-sounds] [--unordered-hidden]
    python diff_registry_dumps.py golden.json candidate.json --subset

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
* ``--unordered-sounds`` compares the sound-event list as a set. Sound events are registered by
  each module from its own preInit once the sounds are split by owner, so their registry order
  (and numeric ids) follow mod load order rather than one enum. Forge persists those ids per
  world and remaps them silently on load, so the order is not player-visible; the names still
  must match exactly.
* ``--unordered-hidden`` checks registry order only among blocks and items that have a creative
  tab. Hidden (retiring) blocks are registered first by hidden tabs that now exist per module,
  so their relative order follows tab order rather than one list; nothing renders them, so the
  order is not player-visible. Every hidden entry must still be present.
* The ``mods`` list and every version string are ignored; the point is what was registered, not
  which jar it came from.

``--subset`` switches to the check a partial install needs: the candidate must be an exact
*subset* of the golden dump rather than equal to it. Every candidate block, item and tile-entity
id must exist in golden with identical fields (the creative tab index and the class name are
always ignored here — a module owns its own tab, and its classes moved package), the candidate's
registry order must equal golden's order restricted to the ids the candidate has, and every
candidate sound event, recipe and entity id must exist in golden. What golden has and the
candidate lacks is exactly what the absent modules would have registered, so it is reported as an
informational count, not a difference. Nothing may be *added* or *changed*: an id the golden dump
does not know, a field that reads differently, or two ids that swapped places all still fail.
``--subset`` ignores the other flags.
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


SUBSET_IGNORED_FIELDS = frozenset(("creativeTabIndex", "class"))


def compare_subset(golden, candidate, problems, absent):
    """Check that ``candidate`` registers a subset of what ``golden`` registers."""
    for name in REGISTRIES_WITH_ENTRIES:
        golden_entries, candidate_entries = golden.get(name, []), candidate.get(name, [])
        golden_by_id = {key_of(e): e for e in golden_entries}
        candidate_ids = [key_of(e) for e in candidate_entries]
        candidate_id_set = set(candidate_ids)
        for entry in candidate_entries:
            other = golden_by_id.get(key_of(entry))
            if other is None:
                problems.append("%s: not in golden: %s" % (name, key_of(entry)))
        compare_entries(name, [e for e in golden_entries if key_of(e) in candidate_id_set],
                        candidate_entries, SUBSET_IGNORED_FIELDS, problems)
        expected_order = [i for i in (key_of(e) for e in golden_entries) if i in candidate_id_set]
        present_order = [i for i in candidate_ids if i in golden_by_id]
        if expected_order != present_order:
            for position, (g, c) in enumerate(zip(expected_order, present_order)):
                if g != c:
                    problems.append("%s: order differs from position %d: golden %s, candidate %s"
                                    % (name, position, g, c))
                    break
        absent[name] = len(golden_entries) - len(candidate_id_set & set(golden_by_id))

    for name in REGISTRIES_WITH_IDS:
        golden_ids = set(golden.get(name, []))
        candidate_ids = candidate.get(name, [])
        for extra in sorted(set(candidate_ids) - golden_ids):
            problems.append("%s: not in golden: %s" % (name, extra))
        absent[name] = len(golden_ids - set(candidate_ids))


def main(argv):
    args = [a for a in argv if not a.startswith("--")]
    flags = set(a for a in argv if a.startswith("--"))
    if len(args) != 2:
        print(__doc__)
        return 2
    golden, candidate = load(args[0]), load(args[1])

    if "--subset" in flags:
        problems, absent = [], {}
        if golden.get("namespace") != candidate.get("namespace"):
            problems.append("namespace differs: %s vs %s"
                            % (golden.get("namespace"), candidate.get("namespace")))
        if not candidate.get("counts", {}).get("tileEntityRegistryReadable", True):
            problems.append("candidate could not read the tile entity registry; "
                            "its tileEntities list is empty")
        compare_subset(golden, candidate, problems, absent)
        print("golden: %s | candidate: %s (subset mode)" % (args[0], args[1]))
        print("candidate counts: " + ", ".join(
            "%s=%s" % (k, v) for k, v in candidate.get("counts", {}).items()))
        print("only in golden (the absent modules' content): " + ", ".join(
            "%s=%d" % (k, absent[k]) for k in REGISTRIES_WITH_ENTRIES + REGISTRIES_WITH_IDS))
        if problems:
            print("%d difference(s):" % len(problems))
            for problem in problems[:200]:
                print("  " + problem)
            if len(problems) > 200:
                print("  ... %d more" % (len(problems) - 200))
            return 1
        print("SUBSET: every candidate entry exists in golden, in golden's order, with the same "
              "fields (ignoring %s)" % ", ".join(sorted(SUBSET_IGNORED_FIELDS)))
        return 0

    ignore_fields = set()
    if "--ignore-tab-index" in flags:
        ignore_fields.add("creativeTabIndex")
    if "--ignore-class" in flags:
        ignore_fields.add("class")
    if "--unordered-sounds" in flags:
        ignore_fields.add("(sound order)")
    if "--unordered-hidden" in flags:
        ignore_fields.add("(hidden-block order)")

    problems = []
    if golden.get("namespace") != candidate.get("namespace"):
        problems.append("namespace differs: %s vs %s" % (golden.get("namespace"), candidate.get("namespace")))
    if not candidate.get("counts", {}).get("tileEntityRegistryReadable", True):
        problems.append("candidate could not read the tile entity registry; its tileEntities list is empty")

    for name in REGISTRIES_WITH_ENTRIES:
        g, c = golden.get(name, []), candidate.get(name, [])
        if "--unordered-hidden" in flags and name in ("blocks", "items"):
            g_ids, c_ids = set(key_of(e) for e in g), set(key_of(e) for e in c)
            for missing in sorted(g_ids - c_ids):
                problems.append("%s: missing in candidate: %s" % (name, missing))
            for extra in sorted(c_ids - g_ids):
                problems.append("%s: unexpected in candidate: %s" % (name, extra))
            visible = lambda entries: [key_of(e) for e in entries if e.get("creativeTab") is not None]
            compare_ordered_ids(name + " (visible)", visible(g), visible(c), problems)
        else:
            compare_ordered_ids(name, [key_of(e) for e in g], [key_of(e) for e in c], problems)
        compare_entries(name, g, c, ignore_fields, problems)
    for name in REGISTRIES_WITH_IDS:
        if name == "soundEvents" and "--unordered-sounds" in flags:
            g, c = set(golden.get(name, [])), set(candidate.get(name, []))
            for missing in sorted(g - c):
                problems.append("%s: missing in candidate: %s" % (name, missing))
            for extra in sorted(c - g):
                problems.append("%s: unexpected in candidate: %s" % (name, extra))
            continue
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
