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
members are collected, keeping those whose names are *not* SRG names. Some of those are legitimate
-- members Forge adds, or that were never obfuscated, keep their plain names -- so the test is
comparative: the candidate jars may not reference any plain-named member the baseline jars do not.
Use the previous release's jars as the baseline.

    python check_reobf_refs.py --baseline previous/*.jar --candidate build/libs/*-2026.09.11.jar

Exit status 1 if the candidate set has a plain-named reference the baseline lacks. Pass release
jars only: a ``-dev`` jar carries MCP names by design, and is flagged if given.
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
REFERENCE = re.compile(r"// (?:Field|Method|InterfaceMethod) "
                       r"(net/minecraft/[A-Za-z0-9_/$]+)\.([A-Za-z0-9_$<>]+):")
BATCH = 150
SHOWN = 60


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


def plain_refs(javap, jar):
    """The plain-named Minecraft members the CSM classes in ``jar`` reference."""
    with zipfile.ZipFile(jar) as archive:
        classes = [name[:-6].replace("/", ".") for name in archive.namelist()
                   if name.startswith(CSM_PREFIX) and name.endswith(".class")]
    found = set()
    for start in range(0, len(classes), BATCH):
        result = subprocess.run([javap, "-c", "-p", "-classpath", jar]
                                + classes[start:start + BATCH],
                                capture_output=True, text=True, errors="replace")
        # A javap failure yields no references, which would read as a pass. Never let it.
        if result.returncode != 0:
            sys.exit("javap failed on {0}:\n{1}".format(jar, result.stderr[:800]))
        for match in REFERENCE.finditer(result.stdout):
            owner, name = match.group(1), match.group(2)
            # SRG names are what a release jar should carry; constructors and static initialisers
            # are never renamed.
            if not name.startswith(("func_", "field_", "<")):
                found.add((owner, name))
    return found, len(classes)


def collect(javap, jars, label):
    refs = set()
    total = 0
    for jar in jars:
        if jar.endswith("-dev.jar"):
            print("warning: {0} is a dev jar; its names are MCP by design"
                  .format(os.path.basename(jar)))
        found, count = plain_refs(javap, jar)
        refs |= found
        total += count
    print("{0:<9} {1} jar(s), {2} classes, {3} plain-named Minecraft member references"
          .format(label, len(jars), total, len(refs)))
    return refs


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--baseline", nargs="+", required=True, metavar="JAR",
                        help="known-good release jars, normally the previous release")
    parser.add_argument("--candidate", nargs="+", required=True, metavar="JAR",
                        help="the release jars to check")
    args = parser.parse_args(argv)

    javap = find_javap()
    baseline = collect(javap, expand(args.baseline), "baseline")
    candidate = collect(javap, expand(args.candidate), "candidate")

    gaps = sorted(candidate - baseline)
    if gaps:
        print("\n{0} plain-named reference(s) the baseline does not have -- a reobfuscation gap:"
              .format(len(gaps)))
        for owner, name in gaps[:SHOWN]:
            print("  {0}.{1}".format(owner, name))
        if len(gaps) > SHOWN:
            print("  ... and {0} more".format(len(gaps) - SHOWN))
        return 1
    print("\nno plain-named references beyond the baseline's")
    return 0


if __name__ == "__main__":
    sys.exit(main())
