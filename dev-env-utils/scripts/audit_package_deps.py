#!/usr/bin/env python3
"""Java package dependency matrix for the CSM sources.

Prints which top-level packages under ``com.micatechnologies.minecraft.csm`` reference which,
counting distinct (file, symbol) pairs, then lists the module-to-module and core-to-module
references in full. Written for the modularization work (see the plan in
``assets/docs/agent_progress/``): a module may only reference Core, so every non-zero cell
between two subsystem packages, and every reference from ``codeutils``/root/``tabs`` into a
subsystem, is something the split has to remove or move. Every source tree -- Core plus each
optional module under ``modules/<name>/src/main/java`` -- shares the same package namespace, so
this walks all of them via ``csm_layout``; a package name (``lifesafety``, ``tabs``, ...) may now
physically live in more than one tree, and the matrix still reports on the namespace as a whole.
Run from anywhere; paths are derived from this file's location.
"""
import os, re, sys, collections
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import csm_layout as layout  # noqa: E402
REPO_ROOT = layout.REPO_ROOT
ROOTS = [root for _module, root in layout.java_package_roots()]
BASE = "com.micatechnologies.minecraft.csm"
def pkg_of(path):
    root = next((r for r in ROOTS if path.startswith(r + os.sep)), ROOTS[0])
    rel = os.path.relpath(path, root).replace("\\", "/")
    parts = rel.split("/")
    return parts[0] if len(parts) > 1 else "(root)"
deps = collections.defaultdict(lambda: collections.defaultdict(set))  # from -> to -> set of (file, symbol)
files_by_pkg = collections.Counter()
imp_re = re.compile(r"^import\s+(static\s+)?" + re.escape(BASE) + r"\.([A-Za-z0-9_.]+);", re.M)
fq_re = re.compile(re.escape(BASE) + r"\.([a-z][a-z]+)\.([A-Za-z0-9_.]+)")
for ROOT in ROOTS:
 for dp, dn, fn in os.walk(ROOT):
    for f in fn:
        if not f.endswith(".java"): continue
        p = os.path.join(dp, f)
        src = open(p, encoding="utf-8", errors="replace").read()
        me = pkg_of(p)
        files_by_pkg[me] += 1
        seen = set()
        for m in imp_re.finditer(src):
            sym = m.group(2)
            parts = sym.split(".")
            tgt = parts[0] if (len(parts) > 1 and parts[0][0].islower()) else "(root)"
            seen.add((tgt, sym))
        # fully-qualified inline references (not imports)
        body = imp_re.sub("", src)
        for m in fq_re.finditer(body):
            seen.add((m.group(1), m.group(1)+"."+m.group(2)))
        for tgt, sym in seen:
            if tgt != me:
                deps[me][tgt].add((os.path.relpath(p, ROOT).replace("\\","/"), sym))
pkgs = sorted(files_by_pkg)
print("PACKAGE DEPENDENCY MATRIX (rows depend on columns; count = distinct (file,symbol) refs)")
core = {"codeutils", "tabs", "(root)", "api"}
for a in pkgs:
    row = []
    for b in pkgs:
        n = len(deps[a][b]) if b in deps[a] else 0
        row.append(n)
    print(f"{a:20s}", " ".join(f"{n:4d}" for n in row))
print("cols:", pkgs)
print()
print("=== NON-CORE -> NON-CORE cross-subsystem references (the ones that matter) ===")
for a in pkgs:
    if a in core: continue
    for b in pkgs:
        if b in core or b == a or b not in deps[a]: continue
        refs = deps[a][b]
        syms = collections.Counter(s for _, s in refs)
        print(f"\n{a} -> {b}: {len(refs)} refs in {len(set(f for f,_ in refs))} files")
        for s, n in syms.most_common(25):
            print(f"    {s}  x{n}")
        fl = sorted(set(f for f,_ in refs))
        print("    files:", ", ".join(fl[:15]), ("..." if len(fl) > 15 else ""))
print()
print("=== CORE -> NON-CORE references (core must not depend on modules) ===")
for a in sorted(core):
    for b in pkgs:
        if b in core or b not in deps[a]: continue
        refs = deps[a][b]
        syms = collections.Counter(s for _, s in refs)
        print(f"\n{a} -> {b}: {len(refs)} refs in {len(set(f for f,_ in refs))} files")
        for s, n in syms.most_common(40):
            print(f"    {s}  x{n}")
        fl = sorted(set(f for f,_ in refs))
        print("    files:", ", ".join(fl[:25]), ("..." if len(fl) > 25 else ""))
