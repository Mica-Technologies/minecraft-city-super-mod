#!/usr/bin/env python3
"""Generate the block reference pages of the CSM guidebook, under ``docs/reference/``.

Why this is generated and not written
-------------------------------------

There are over sixteen hundred blocks and the mod gains more most weeks. A hand-maintained
catalogue is a catalogue that is wrong by the next release, and the old GitHub Wiki is the proof:
it was **415 blocks short**, including the entire Furniture tab, because its generator found blocks
only through ``initTabBlock(Foo.class)`` and never saw the ones registered as instances --

    initTabBlock(new BlockTrafficSign("stopahead"))

-- which is how most new content is registered. This builds on ``csm_block_index``, which handles
both forms and is already the shared source of truth for the recipe tooling, so the catalogue cannot
drift from the mod again.

Block stats
-----------

Only about 260 block classes call ``super(Material.…)`` themselves; the rest inherit the call from
an abstract base. So stats are resolved by walking the ``extends`` chain until a call is found --
every road sign, for example, gets its hardness from ``AbstractBlockSign`` four levels up. Where no
call can be found the columns are left blank rather than guessed at: a wrong number in a reference
is worse than an absent one.
"""

import io
import os
import re
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)

import csm_block_index as index_tool  # noqa: E402  (path set above)

REPO_ROOT = index_tool.REPO_ROOT
LANG_FILES = index_tool.lang_files("en_us")
OUT_DIR = os.path.join(REPO_ROOT, "docs", "reference")

# The AbstractBlock constructor every stat in the old wiki came from:
#     super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0)
#            material      sound            tool       lvl hard  resist
#
# Matched by position rather than by shape, because the shape varies. A factory writes
#     super(initRegistryName(name), SoundType.STONE, "pickaxe", 1, hardness, 10F, 0F, 0)
# where the first argument is a call and the hardness is a constructor parameter. Splitting the
# argument list keeps the four literals that are still there instead of throwing the call away.
_SUPER_CALL_RE = re.compile(r'super\(')
_NUMBER_RE = re.compile(r'^-?\d+(?:\.\d+)?[FfDd]?$')
_TOOL_RE = re.compile(r'^(?:null|"[^"]*")$')


def _split_args(text, start):
    """Split the argument list of a call whose '(' is at ``start``, on top-level commas."""
    depth = 0
    args = []
    current = []
    for i in range(start, len(text)):
        ch = text[i]
        if ch in "([":
            depth += 1
            if depth == 1:
                continue
        elif ch in ")]":
            depth -= 1
            if depth == 0:
                args.append("".join(current).strip())
                return args
        if depth == 1 and ch == ",":
            args.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
    return None


def _number(arg):
    """A literal number as written, or None when the argument is a name rather than a value."""
    return arg.rstrip("FfDd") if _NUMBER_RE.match(arg) else None

# tile.<registry>.name=Display Name
_LANG_RE = re.compile(r'^tile\.([a-z0-9_]+)\.name=(.*)$')

# tab id -> (page slug, human title, one-line description)
TABS = {
    "tabbuildingmaterials": ("building-materials", "Building Materials",
                             "Concrete, brick, metal and tile, with matching stairs, slabs and "
                             "fences."),
    "tabfurniture": ("furniture", "Furniture",
                     "Interior fittings: seating, counters, appliances and fixtures."),
    "tabgaming": ("gaming", "Gaming",
                  "Arcade cabinets and the playable machines that go with them."),
    "tabhvac": ("hvac", "HVAC",
                "Heating and cooling that actually simulates room temperature."),
    "tablifesafety": ("life-safety", "Life Safety",
                      "Fire alarm horns, strobes, pull stations, panels and exit signage."),
    "tablighting": ("lighting", "Lighting",
                    "Street lights, floodlights, pendants and sconces, all switchable."),
    "tabmaterials": ("materials", "Materials",
                     "The CSM Fabricator, which turns vanilla ingots into CSM blocks."),
    "tabnovelties": ("novelties", "Novelties",
                     "Decorative oddities that did not belong anywhere else."),
    "tabpowergrid": ("power-grid", "Power Grid",
                     "Utility poles, transformers and the Forge Energy that runs through them."),
    "tabroadsigns": ("road-signs", "Road Signs",
                     "The MUTCD sign set, grouped the way the manual groups it."),
    "tabtechnology": ("technology", "Technology",
                      "Servers, routers, screens and consumer electronics."),
    "tabtrafficaccessories": ("traffic-accessories", "Traffic Accessories",
                              "Poles, mounts, mast arms, span wire hardware, backplates and "
                              "cameras."),
    "tabtrafficsignals": ("traffic-signals", "Traffic Signals",
                          "Signal heads, crosswalk signals and the controllers that drive them."),
    "tabnone": ("unlisted", "Unlisted",
                "Retired and internal blocks kept so old worlds still load. Not in any creative "
                "tab."),
}

PAGE_ORDER = ["tabbuildingmaterials", "tabfurniture", "tabgaming", "tabhvac", "tablifesafety",
              "tablighting", "tabmaterials", "tabnovelties", "tabpowergrid", "tabroadsigns",
              "tabtechnology", "tabtrafficaccessories", "tabtrafficsignals", "tabnone"]


def read_lang():
    """Display names, keyed by registry name."""
    names = {}
    for path in LANG_FILES:
        with io.open(path, encoding="utf-8") as handle:
            for line in handle:
                match = _LANG_RE.match(line.strip())
                if match:
                    names[match.group(1)] = match.group(2).strip()
    return names


def read_class_stats():
    """{class_name: (material, tool, harvest_level, hardness, resistance)} where declared."""
    stats = {}
    for dirpath, _dirs, filenames in (
            entry
            for _module, root in index_tool.source_roots()
            for entry in os.walk(root)):
        for filename in filenames:
            if not filename.endswith(".java"):
                continue
            with io.open(os.path.join(dirpath, filename), encoding="utf-8",
                         errors="replace") as handle:
                text = re.sub(r"\s+", " ", handle.read())
            for call in _SUPER_CALL_RE.finditer(text):
                args = _split_args(text, call.end() - 1)
                # tool at 2 and harvest level at 3 are what identify this as the AbstractBlock
                # constructor rather than some other super() in the file.
                if not args or len(args) < 6:
                    continue
                if not _TOOL_RE.match(args[2]) or _number(args[3]) is None:
                    continue
                stats[filename[:-5]] = {
                    "tool": "" if args[2] == "null" else args[2].strip('"'),
                    "harvest": _number(args[3]) or "",
                    "hardness": _number(args[4]) or "",
                    "resistance": _number(args[5]) or "",
                }
                break
    return stats


def resolve_stats(class_name, classes, stats):
    """Walk the extends chain until a class declares the stats, or give up."""
    seen = set()
    current = class_name
    while current and current not in seen:
        seen.add(current)
        if current in stats:
            return stats[current]
        current = (classes.get(current) or {}).get("extends")
    return None


def escape(text):
    """Keep a display name from breaking the table it sits in."""
    return text.replace("|", "\\|")


def write_page(tab_id, entries, names, classes, stats):
    slug, title, blurb = TABS[tab_id]
    lines = [
        "# {0}".format(title),
        "",
        blurb,
        "",
        '!!! info "{0} block{1} in this tab"'.format(len(entries),
                                                     "" if len(entries) == 1 else "s"),
        "",
        "    Every block below is registered in the mod and has a blockstate on disk. This page is",
        "    generated from the source, so it cannot fall behind what the mod actually ships.",
        "",
        # Wrapped so the site can style *these* tables -- sticky header, monospace id column,
        # nowrap -- without that styling leaking onto every other table in the guidebook. It did
        # leak once, and turned a prose table on the landing page into clipped monospace.
        '<div class="block-table" markdown>',
        "",
        "| Block | Registry ID | Hardness | Resistance | Tool | Harvest |",
        "|---|---|---|---|---|---|",
    ]

    for registry in entries:
        info = classes.get(registry, {})
        display = names.get(registry, registry)
        resolved = resolve_stats(info.get("class"), classes, stats) or {}
        lines.append("| {0} | `csm:{1}` | {2} | {3} | {4} | {5} |".format(
            escape(display),
            registry,
            resolved.get("hardness", ""),
            resolved.get("resistance", ""),
            resolved.get("tool", ""),
            resolved.get("harvest", ""),
        ))

    lines.append("")
    lines.append("</div>")
    lines.append("")
    with io.open(os.path.join(OUT_DIR, slug + ".md"), "w", encoding="utf-8",
                 newline="\n") as handle:
        handle.write("\n".join(lines))
    return len(entries)


def write_index(counts, total):
    lines = [
        "# Block Reference",
        "",
        "Every block the City Super Mod registers, grouped by the creative tab it appears in.",
        "",
        '!!! tip "Looking for one particular block?"',
        "",
        "    Use the search box at the top. It matches display names *and* registry ids, so",
        "    `csm:tlitevertwiremount` finds the page it is listed on.",
        "",
        "| Tab | Blocks | What is in it |",
        "|---|---|---|",
    ]
    for tab_id in PAGE_ORDER:
        slug, title, blurb = TABS[tab_id]
        lines.append("| [{0}]({1}.md) | {2} | {3} |".format(title, slug, counts.get(tab_id, 0),
                                                            blurb))
    lines += [
        "| **Total** | **{0}** | |".format(total),
        "",
        "## How to read the table",
        "",
        "**Registry ID** is what the block is called in commands and configs -- `/give`,",
        "`/setblock`, and any mod that addresses blocks by name. It is rarely a tidy version of the",
        "display name, so take it from here rather than guessing at it.",
        "",
        "**Hardness** is how long the block takes to break; **resistance** is how well it stands up",
        "to explosions. **Tool** and **harvest** are what you need to be holding to get the block",
        "back rather than nothing.",
        "",
        "Blank stat columns mean the block does not declare them anywhere this can read, not that",
        "they are zero.",
        "",
    ]
    with io.open(os.path.join(OUT_DIR, "index.md"), "w", encoding="utf-8",
                 newline="\n") as handle:
        handle.write("\n".join(lines))


def main():
    index, tabs, classes = index_tool.build_index()
    names = read_lang()
    stats = read_class_stats()

    # Registry names carry the class they came from; fold that in so a page can resolve stats.
    for registry, info in index.items():
        classes.setdefault(registry, {})
    merged = dict(classes)
    for registry, info in index.items():
        merged[registry] = {"class": info["class"], "extends": info.get("extends")}
    for class_name, info in classes.items():
        merged.setdefault(class_name, info)

    os.makedirs(OUT_DIR, exist_ok=True)
    counts = {}
    total = 0
    for tab_id in PAGE_ORDER:
        entries = sorted((r for r in index if index[r]["tab"] == tab_id),
                         key=lambda r: (names.get(r, r).lower(), r))
        counts[tab_id] = write_page(tab_id, entries, names, merged, stats)
        total += counts[tab_id]

    write_index(counts, total)

    unknown = sorted({index[r]["tab"] for r in index} - set(TABS))
    named = sum(1 for r in index if r in names)
    statted = sum(1 for r in index
                  if resolve_stats(index[r]["class"], classes, stats))

    print("Blocks written      : {0}".format(total))
    print("With a display name : {0}  ({1} missing a lang entry)".format(named, total - named))
    print("With resolved stats : {0}  ({1} left blank)".format(statted, total - statted))
    if unknown:
        print("UNKNOWN TABS (no page): {0}".format(", ".join(unknown)))


if __name__ == "__main__":
    main()
