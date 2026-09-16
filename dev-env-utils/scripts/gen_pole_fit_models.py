#!/usr/bin/env python3
"""
Generates the pole-fitted model variants for the side-mounted arms that clamp to a pole, and
wires them into the arms' blockstates.

WHY
---
CSM has three vertical traffic pole styles: the 12-across pole family, the 8-across thin pole
and the 6-across pedestal pole. Every side-mounted arm -- the light mounts, the NOV tapered
masts, the SCE light mounts -- was modelled against the widest one: its
pole-end plate reaches two sixteenths into the block behind it, which is exactly where a
12-across pole's skin is. Against the thin pole that plate stops two sixteenths short of the
skin, against the pedestal pole three, and the arm reads as floating beside the pole (#197).

The arm adapts to the pole rather than the pole growing to meet the arm. Each catalogued model
gets a `_thin` and a `_pedestal` copy in which the pole-end pieces are moved back to the thinner
pole's skin, and the blockstate gains a `polefit` variant block that swaps them in. Which model
is drawn is decided in Java: a block that implements `ICsmPoleFitted` carries `CsmPoleFit.PROPERTY`
in its actual state, resolved from `AbstractBlockTrafficPole.getPoleRadius()` of the block behind
it (see `codeutils/CsmPoleFit.java`).

WHAT MOVES
----------
The models face north with the pole at +Z: the block behind starts at z = 16 and a pole of
radius r has its near skin at z = 24 - r. Two kinds of element take part:

  * a PLATE -- an element lying wholly beyond the block face (from.z >= SHIFT_FROM) -- is moved
    back by the fit's delta, so it lands the same depth inside the thinner pole as it did in
    the large one;
  * the ARM -- an element that crosses the block face (to.z > EXTEND_PAST but starting inside
    the block) -- is stretched by the same delta so it still reaches the plate. A near-flat
    metal swatch does not mind its UVs being stretched by a sixth.

Everything else is copied untouched, so the arm itself is pixel-identical whichever pole it is
on. Elements never leave the JSON model's legal -16..32 range: the deepest plate here ends at
19.5 and the largest delta is 3.

The generated files are committed and `--check` fails if the tree has drifted from the script,
so the catalogue below is the one place that says which arms adapt.

Run:  python dev-env-utils/scripts/gen_pole_fit_models.py [--check]
"""

import copy
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

# (model, relative to models/block/, and the blockstates that draw it). Every block named here
# must be registered as ICsmPoleFitted, or the blockstate's polefit variants are never selected.
CATALOGUE = [
    ("trafficaccessories/shared_models/trafficpoleverticallightmount",
     ["trafficpoleverticallightmount", "trafficpoleverticallightmountblack",
      "trafficpoleverticallightmounttan", "trafficpoleverticallightmountunpainted",
      "trafficpoleverticallightmountwhite"]),
    ("lighting/shared_models/nov_tapered_mast", ["novtm"]),
    ("lighting/shared_models/nov_tapered_mast_2", ["novtm2"]),
    ("lighting/shared_models/nov_tapered_mast_3", ["novtm3"]),
    ("lighting/shared_models/nov_tapered_mast_4", ["novtm4"]),
    ("lighting/shared_models/nov_tapered_mast_5", ["novtm5"]),
    # Not the GE Powerbracket (gepb): its plate is cut for the large pole too, but it is a
    # wall-mount specialty that is rarely put on a pole, and it is a bright light with its own
    # state properties, so it was left alone deliberately.
    ("powergrid/shared_models/scelightmount", ["scelightmount"]),
    ("powergrid/shared_models/scelightmountsmall", ["scelightmountsmall"]),
]

# (blockstate value, model suffix, how much further back the pole's skin is than the large
# pole's). Must agree with CsmPoleFit: LARGE r=6, THIN r=4, PEDESTAL r=3, so the skin at
# z = 24 - r is 2 and 3 further back.
FITS = [
    ("thin", "_thin", 2.0),
    ("pedestal", "_pedestal", 3.0),
]

SHIFT_FROM = 16.9    # an element starting past here is a plate: moved back whole
EXTEND_PAST = 16.0   # an element reaching past here (but starting inside) is the arm: stretched
Z_LIMIT = 32.0       # Forge JSON element models cannot leave -16..32


def fitted(model, delta):
    """A copy of a model with its pole-end pieces moved back by delta."""
    out = copy.deepcopy(model)
    touched = 0
    for element in out.get("elements", []):
        z0, z1 = element["from"][2], element["to"][2]
        if z0 >= SHIFT_FROM:
            element["from"][2] = z0 + delta
            element["to"][2] = z1 + delta
            touched += 1
        elif z1 > EXTEND_PAST:
            element["to"][2] = z1 + delta
            touched += 1
        if element["to"][2] > Z_LIMIT:
            raise AssertionError("element %s..%s leaves the legal model range after +%.1f"
                                 % (element["from"], element["to"], delta))
    if touched == 0:
        raise AssertionError("no element reaches the pole; is this model facing north with "
                             "the pole at +Z?")
    return out


def dump(data):
    return json.dumps(data, indent=2) + "\n"


_NUMBER_ARRAY = re.compile(r"\[\s*((?:-?\d+(?:\.\d+)?(?:e-?\d+)?\s*,\s*)*-?\d+(?:\.\d+)?(?:e-?\d+)?)\s*\]",
                           re.S)


def dump_model(data):
    """A model, with every all-number array (from/to/uv/origin) on one line, so a 44-element
    model is a few hundred lines rather than a few thousand and a diff against the base model
    reads element by element."""
    text = json.dumps(data, indent=2)
    return _NUMBER_ARRAY.sub(lambda m: "[" + ", ".join(v.strip() for v in m.group(1).split(","))
                             + "]", text) + "\n"


def blockstate_with_fits(blockstate, model_rel):
    out = copy.deepcopy(blockstate)
    variants = out.setdefault("variants", {})
    polefit = {"large": {}}
    for value, suffix, _delta in FITS:
        polefit[value] = {"model": "csm:%s%s" % (model_rel, suffix)}
    variants["polefit"] = polefit
    return out


def main(check):
    drift = []
    written = 0
    for model_rel, blockstates in CATALOGUE:
        model_path = layout.resolve_asset("models/block/%s.json" % model_rel)
        if not model_path:
            sys.exit("model not found in any tree: %s" % model_rel)
        with open(model_path, encoding="utf-8") as fh:
            model = json.load(fh)
        for _value, suffix, delta in FITS:
            out_path = model_path[:-5] + suffix + ".json"
            text = dump_model(fitted(model, delta))
            written += write_or_check(out_path, text, check, drift)
        for name in blockstates:
            bs_path = layout.resolve_asset("blockstates/%s.json" % name)
            if not bs_path:
                sys.exit("blockstate not found in any tree: %s" % name)
            with open(bs_path, encoding="utf-8") as fh:
                original = fh.read()
            blockstate = json.loads(original)
            # Some of these files end without a newline; that much reformatting is fine.
            if dump(blockstate) != original.replace("\r\n", "\n").rstrip("\n") + "\n":
                # The file is rewritten wholesale, so its formatting must already be ours, or
                # a one-line change would show up as a whole-file diff.
                sys.exit("%s is not in json.dumps(indent=2) form; reformat it first" % bs_path)
            written += write_or_check(bs_path, dump(blockstate_with_fits(blockstate, model_rel)),
                                      check, drift)
    if check:
        if drift:
            print("pole fit models have drifted from the generator:")
            for p in drift:
                print("  " + p)
            sys.exit(1)
        print("pole fit models are up to date (%d files)" % written)
    else:
        print("wrote %d files" % written)


def write_or_check(path, text, check, drift):
    current = None
    if os.path.exists(path):
        with open(path, encoding="utf-8") as fh:
            current = fh.read().replace("\r\n", "\n")
    if current == text:
        return 1
    if check:
        drift.append(os.path.relpath(path, layout.REPO_ROOT))
        return 1
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(text)
    return 1


if __name__ == "__main__":
    main("--check" in sys.argv[1:])
