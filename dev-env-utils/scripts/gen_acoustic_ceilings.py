#!/usr/bin/env python3
"""Every asset the hanging acoustic baffles and clouds ship (Building, Interior Finishes tab).

    python dev-env-utils/scripts/gen_acoustic_ceilings.py
    python dev-env-utils/scripts/gen_acoustic_ceilings.py --check
    python dev-env-utils/scripts/gen_acoustic_ceilings.py --fragments   # lang and tab lines

- **Baffles** (BlockAcousticBaffle): a vertical fin 2 px thick and 10 px deep, hung 2 px below
  the ceiling on cables, run along its axis and joined with the baffles beside it into one long
  fin. Its felt edge is drawn only where a run stops.
- **Clouds** (BlockAcousticCloud): a flat panel 1 px thick hung 4 px below the ceiling. Clouds
  side by side read as one panel with no joining state at all: their touching edges lie hidden
  between the panels' faces.

Both are in the linear lights' ten finishes (gen_linear_lights.FINISHES), drawn with the same Core
textures (`blocks/finishes/finish_<name>`), so a fin and the light under it match exactly, and
hang on the same aircraft cable texture. Vanilla multipart blockstates; a finish is a child model
naming its texture.
"""

import argparse
import json
import os
import sys

import gen_linear_lights as lin

REPO = lin.REPO
BUILD = os.path.join(REPO, "modules", "building", "src", "main", "resources", "assets", "csm")
MODEL_DIR = os.path.join(BUILD, "models", "block", "buildingmaterials", "acoustic")
ITEM_DIR = os.path.join(BUILD, "models", "item")
STATE_DIR = os.path.join(BUILD, "blockstates")
MODEL_REF = "csm:buildingmaterials/acoustic/%s"
PARENT_REF = "csm:block/buildingmaterials/acoustic/%s"

KINDS = {
    "baffle": ("acoustic_baffle_%s", ("Acoustic Baffle (%s)", "Bafle Acústico (%s)",
                                      "Akustikbaffel (%s)", "Akustikbaffel (%s)")),
    "cloud": ("acoustic_cloud_%s", ("Acoustic Ceiling Cloud (%s)", "Nube Acústica de Techo (%s)",
                                    "Akustik-Deckensegel (%s)", "Akustikmoln (%s)")),
}

box = lin.box


def baffle_body():
    return [box((7, 4, 0), (9, 14, 16), "felt", ("east", "west", "down", "up"))]


def baffle_end():
    """The felt edge across the fin's north end, where its run stops."""
    return [box((7, 4, 0), (9, 14, 0.1), "felt", ("north",))]


def cloud_panel():
    return [box((0, 11, 0), (16, 12, 16), "felt",
                ("down", "up", "north", "south", "east", "west"))]


def cable(y0):
    return [box((7.9, y0, 7.9), (8.1, 16, 8.1), "cable", ("north", "south", "east", "west"))]


def model(textures, elements, display=None):
    m = {"parent": "block/block", "textures": textures, "elements": elements}
    if display:
        m["display"] = display
    return m


DISPLAY = {
    "gui": {"rotation": [30, 45, 0], "translation": [0, -2, 0], "scale": [0.625] * 3},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.25] * 3},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5] * 3},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 0, 0],
                              "scale": [0.375] * 3},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                              "scale": [0.4] * 3},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                             "scale": [0.4] * 3},
}


def registries():
    out = {}
    for kind, (pattern, names) in KINDS.items():
        for fin, (_, _, fnames) in lin.FINISHES.items():
            out[pattern % fin] = (kind, fin, tuple(n % f for n, f in zip(names, fnames)))
    return out


def blockstate(kind, fin):
    if kind == "cloud":
        return {"multipart": [
            {"apply": {"model": MODEL_REF % ("cloud_%s" % fin)}},
            {"when": {"cable": "true"}, "apply": {"model": MODEL_REF % "cable_cloud"}}]}
    parts = []
    for axis, y in (("z", 0), ("x", 90)):
        def apply(name, turn=0):
            a = {"model": MODEL_REF % name}
            if (y + turn) % 360:
                a["y"] = (y + turn) % 360
            return a
        parts.append({"when": {"axis": axis}, "apply": apply("baffle_%s" % fin)})
        parts.append({"when": {"axis": axis, "front": "false"},
                      "apply": apply("baffle_end_%s" % fin)})
        parts.append({"when": {"axis": axis, "back": "false"},
                      "apply": apply("baffle_end_%s" % fin, 180)})
    parts.append({"when": {"cable": "true"}, "apply": {"model": MODEL_REF % "cable_baffle"}})
    return {"multipart": parts}


def outputs():
    out = {}
    cable_t = {"cable": lin.CABLE_REF, "particle": lin.CABLE_REF}
    out[os.path.join(MODEL_DIR, "cable_baffle.json")] = lin.js(model(cable_t, cable(14)))
    out[os.path.join(MODEL_DIR, "cable_cloud.json")] = lin.js(model(cable_t, cable(12)))
    out[os.path.join(MODEL_DIR, "geo_baffle.json")] = lin.js(model({}, baffle_body()))
    out[os.path.join(MODEL_DIR, "geo_baffle_end.json")] = lin.js(model({}, baffle_end()))
    out[os.path.join(MODEL_DIR, "geo_cloud.json")] = lin.js(model({}, cloud_panel()))
    ends = baffle_end() + [box((7, 4, 15.9), (9, 14, 16), "felt", ("south",))]
    out[os.path.join(MODEL_DIR, "geo_item_baffle.json")] = lin.js(
        model({}, baffle_body() + ends, DISPLAY))
    out[os.path.join(MODEL_DIR, "geo_item_cloud.json")] = lin.js(
        model({}, cloud_panel(), DISPLAY))
    for fin in lin.FINISHES:
        t = {"felt": lin.FINISH_REF % fin, "particle": lin.FINISH_REF % fin}
        for part in ("baffle", "baffle_end", "cloud"):
            out[os.path.join(MODEL_DIR, "%s_%s.json" % (part, fin))] = lin.js(
                {"parent": PARENT_REF % ("geo_" + part), "textures": t})
    for reg, (kind, fin, _) in registries().items():
        out[os.path.join(STATE_DIR, reg + ".json")] = lin.js(blockstate(kind, fin))
        out[os.path.join(ITEM_DIR, reg + ".json")] = lin.js(
            {"parent": PARENT_REF % ("geo_item_" + kind),
             "textures": {"felt": lin.FINISH_REF % fin, "particle": lin.FINISH_REF % fin}})
    return out


def fragments():
    regs = registries()
    for i, lang in enumerate(("en_us", "es_es", "de_de", "sv_se")):
        print("# %s.lang" % lang)
        for reg, (_, _, names) in regs.items():
            print("tile.%s.name=%s" % (reg, names[i]))
    print("# CsmTabInteriorFinishes")
    for reg, (kind, _, names) in regs.items():
        cls = "BlockAcousticBaffle" if kind == "baffle" else "BlockAcousticCloud"
        print('    initTabBlock(new %s("%s")); // %s' % (cls, reg, names[0]))


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    a = ap.parse_args()
    if a.fragments:
        fragments()
        return 0
    out = outputs()
    if a.check:
        bad = []
        for p, data in out.items():
            # git may have checked a text file out with CRLF endings; compare it as LF
            have = open(p, "rb").read() if os.path.exists(p) else None
            if have is not None and p.endswith(".json"):
                have = have.replace(b"\r\n", b"\n")
            if have != data:
                bad.append(os.path.relpath(p, REPO))
        if bad:
            print("gen_acoustic_ceilings.py: %d file(s) differ:\n  %s" % (len(bad),
                                                                         "\n  ".join(bad)))
            return 1
        print("gen_acoustic_ceilings.py: up to date")
        return 0
    for p, data in out.items():
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as f:
            f.write(data)
    print("wrote %d files" % len(out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
