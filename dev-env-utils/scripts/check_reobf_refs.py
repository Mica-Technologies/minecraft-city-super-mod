#!/usr/bin/env python3
"""Check that release jars name no Minecraft member by its dev (MCP) name.

Why this exists
---------------

A release jar must name every Minecraft field and method by its SRG name (``field_...`` /
``func_...``); the reobfuscator renames them from the MCP names the code is written against.
RetroFuturaGradle's ``reobf<Jar>`` rule resolves an *inherited* member by walking the hierarchy of
the class that owns the reference, and it can only walk classes on that task's reference
classpath. A module block reaches ``net.minecraft.block.Block`` only through Core's
``AbstractBlock``, so until ``modules.gradle`` put Core's classes on every module's reference
classpath, inherited members kept their MCP names in the module jars.

The dev client runs MCP names, so it cannot show this, and neither can a green build. The first
real-launcher test of the module jars crashed with ``NoSuchFieldError: blockState``. This check is
what caught the rest of them.

What it checks
--------------

Every CSM class in each jar is disassembled with ``javap`` and its references to ``net/minecraft``
members are collected, keeping those whose names are *not* SRG names. Many of those are
legitimate: members Forge adds (``BakedQuad.getFormat``, the four-argument
``Block.getExplosionResistance``), enum constants, an enum's ``values`` and ``ordinal``, and
``Object``'s or ``Iterable``'s methods reached through a Minecraft class (``toString``,
``iterator``) are never renamed. So a reference is a gap only if the mappings the reobfuscator
applies (RetroFuturaGradle's ``mcp-srg.srg``) give that MCP name, with that descriptor for a method,
an SRG name. The owner is not compared, since a reference may name a subclass of the class that
declares the member; so a plain-named reference is let through only when *no* class has a renamed
member of that name and descriptor.

The baseline (the previous release's jars) is still compared, and what is new since it is listed,
split into the gaps and the names that are never renamed:

    python check_reobf_refs.py --baseline previous/*.jar --candidate build/libs/*-2026.09.11.jar

Exit status 1 if the candidate set has a plain-named reference, new since the baseline, whose name
the mappings rename. Without the mappings (``--mappings``, by default looked up in the Gradle
cache) every new plain-named reference fails, as before. Pass release jars only: a ``-dev`` jar
carries MCP names by design, and is flagged if given.
"""

import argparse
import glob
import os
import re
import shutil
import subprocess
import sys
import zipfile

CSM_PREFIX = "com/micatechnologies/minecraft/csm/"
REFERENCE = re.compile(r"// (Field|Method|InterfaceMethod) "
                       r"(net/minecraft/[A-Za-z0-9_/$]+)\.([A-Za-z0-9_$<>]+):(\S+)")
BATCH = 150
SHOWN = 60
DEFAULT_MAPPINGS = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "minecraft", "de",
                                "oceanlabs", "mcp", "mcp_stable", "39", "rfg_srgs",
                                "mcp-srg.srg")


def find_javap():
    """``javap`` from ``JAVA_HOME`` if set, otherwise from the ``PATH``."""
    exe = "javap.exe" if os.name == "nt" else "javap"
    home = os.environ.get("JAVA_HOME")
    if home and os.path.isfile(os.path.join(home, "bin", exe)):
        return os.path.join(home, "bin", exe)
    found = shutil.which("javap")
    if found:
        return found
    sys.exit("javap not found: set JAVA_HOME to a JDK, or put javap on the PATH")


def expand(patterns):
    """Expand globs here too, since cmd.exe and PowerShell pass them through literally."""
    jars = []
    for pattern in patterns:
        matches = sorted(glob.glob(pattern)) if any(c in pattern for c in "*?[") else [pattern]
        if not matches:
            sys.exit("no jar matches " + pattern)
        jars.extend(matches)
    for jar in jars:
        if not os.path.isfile(jar):
            sys.exit("not a file: " + jar)
    return jars


def load_renamed(path):
    """The MCP names the reobfuscator renames: fields by name, methods by name and descriptor.

    Read from an MCP-to-SRG table (``MD: owner/name desc owner/srgName srgDesc``,
    ``FD: owner/name owner/srgName``); a member whose SRG name is its MCP name is not renamed.
    Returns the renamed field names, every declared member's ``(owner, name) -> renamed`` (a
    method's key also holds its descriptor), and the renamed methods' ``(name, descriptor)``.
    """
    fields, declared, methods = set(), {}, set()
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            parts = line.split()
            if len(parts) == 3 and parts[0] == "FD:":
                owner, name = parts[1].rsplit("/", 1)
                renamed = parts[2].rsplit("/", 1)[1] != name
                declared[(owner, name)] = renamed
                if renamed:
                    fields.add(name)
            elif len(parts) == 5 and parts[0] == "MD:":
                owner, name = parts[1].rsplit("/", 1)
                renamed = parts[3].rsplit("/", 1)[1] != name
                declared[(owner, name, parts[2])] = renamed
                if renamed:
                    methods.add((name, parts[2]))
    return fields, declared, methods


def plain_refs(javap, jar):
    """The plain-named Minecraft members the CSM classes in ``jar`` reference.

    Returns ``{(owner, name): {(kind, descriptor), ...}}`` and the class count.
    """
    with zipfile.ZipFile(jar) as archive:
        classes = [name[:-6].replace("/", ".") for name in archive.namelist()
                   if name.startswith(CSM_PREFIX) and name.endswith(".class")]
    found = {}
    for start in range(0, len(classes), BATCH):
        result = subprocess.run([javap, "-c", "-p", "-classpath", jar]
                                + classes[start:start + BATCH],
                                capture_output=True, text=True, errors="replace")
        # A javap failure yields no references, which would read as a pass. Never let it.
        if result.returncode != 0:
            sys.exit("javap failed on {0}:\n{1}".format(jar, result.stderr[:800]))
        for match in REFERENCE.finditer(result.stdout):
            kind, owner, name, desc = match.groups()
            # SRG names are what a release jar should carry; constructors and static initialisers
            # are never renamed.
            if not name.startswith(("func_", "field_", "<")):
                found.setdefault((owner, name), set()).add((kind, desc))
    return found, len(classes)


def collect(javap, jars, label):
    refs = {}
    total = 0
    for jar in jars:
        if jar.endswith("-dev.jar"):
            print("warning: {0} is a dev jar; its names are MCP by design"
                  .format(os.path.basename(jar)))
        found, count = plain_refs(javap, jar)
        for key, uses in found.items():
            refs.setdefault(key, set()).update(uses)
        total += count
    print("{0:<9} {1} jar(s), {2} classes, {3} plain-named Minecraft member references"
          .format(label, len(jars), total, len(refs)))
    return refs


def is_renamed(owner, name, uses, renamed):
    """Whether the mappings rename this member, so its plain name in a release jar is a gap.

    A member its owner declares is decided by that entry (an enum constant keeps its name even
    where another class has a renamed field of the same name, and ``NonNullList.get`` even where
    another class has a renamed ``get(int)``); an inherited one by its name (and descriptor).
    """
    fields, declared, methods = renamed
    for kind, desc in uses:
        if kind == "Field" and declared.get((owner, name), name in fields):
            return True
        if kind != "Field" and declared.get((owner, name, desc), (name, desc) in methods):
            return True
    return False


def show(title, keys):
    if not keys:
        return
    print("\n" + title.format(len(keys)))
    for owner, name in keys[:SHOWN]:
        print("  {0}.{1}".format(owner, name))
    if len(keys) > SHOWN:
        print("  ... and {0} more".format(len(keys) - SHOWN))


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--baseline", nargs="+", required=True, metavar="JAR",
                        help="known-good release jars, normally the previous release")
    parser.add_argument("--candidate", nargs="+", required=True, metavar="JAR",
                        help="the release jars to check")
    parser.add_argument("--mappings", default=DEFAULT_MAPPINGS, metavar="SRG",
                        help="the MCP-to-SRG table the reobfuscator applies "
                             "(default: RetroFuturaGradle's mcp-srg.srg for stable_39)")
    args = parser.parse_args(argv)

    renamed = None
    if os.path.isfile(args.mappings):
        renamed = load_renamed(args.mappings)
        print("mappings  {0}: {1} renamed fields, {2} renamed methods"
              .format(args.mappings, len(renamed[0]), len(renamed[2])))
    else:
        print("warning: no mappings at {0}; every new plain-named reference counts as a gap"
              .format(args.mappings))

    javap = find_javap()
    baseline = collect(javap, expand(args.baseline), "baseline")
    candidate = collect(javap, expand(args.candidate), "candidate")

    new = sorted(k for k in candidate if k not in baseline)
    if renamed is None:
        gaps, kept = new, []
    else:
        gaps = [k for k in new if is_renamed(k[0], k[1], candidate[k], renamed)]
        kept = [k for k in new if k not in gaps]
    show("{0} new plain-named reference(s) the mappings never rename (Forge members, enum "
         "constants and methods, Object's methods) -- fine:", kept)
    if gaps:
        show("{0} plain-named reference(s) the mappings rename and the baseline does not have "
             "-- a reobfuscation gap:", gaps)
        return 1
    print("\nno reobfuscation gaps")
    return 0


if __name__ == "__main__":
    sys.exit(main())
