#!/usr/bin/env python3
"""
Updates backplate blockstate JSON files to add the HORIZONTAL computed property.

Standard backplates: Uses vanilla blockstate format (no forge_marker) with fully
enumerated variant keys. Each entry has a complete model definition.

Fitted backplates (doghouse/hawk): Uses forge_marker format with combined keys,
which is already proven to work for these files.

Usage:
    python update_backplate_blockstates.py
"""

import json
import os
import glob

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
BLOCKSTATES_DIR = os.path.join(
    PROJECT_ROOT,
    "src", "main", "resources", "assets", "csm", "blockstates"
)

FACINGS = {
    "down": {"x": 90},
    "east": {"y": 90},
    "north": {},
    "south": {"y": 180},
    "up": {"x": 270},
    "west": {"y": 270},
}

TILT_STATES = ["none", "left_tilt", "right_tilt", "left_angle", "right_angle"]


def vertical_to_horizontal_model(model_path):
    return model_path.replace("_vertical_", "_horizontal_")


def update_standard_blockstate(filepath):
    """Update a standard (non-fitted) backplate blockstate with forge_marker format.

    Uses standalone property blocks (which reliably work with forge_marker: 1).
    The 'horizontal' property block comes AFTER 'tilt' so its model overrides tilt's
    model when horizontal=true. This means tilt only visually applies in vertical mode.
    """
    with open(filepath, "r") as f:
        data = json.load(f)

    default_model = data.get("defaults", {}).get("model", "")
    textures = data.get("defaults", {}).get("textures", {})

    # If no defaults (vanilla format), extract from variant entries
    if not default_model:
        for key, val in data.get("variants", {}).items():
            if isinstance(val, dict) and "model" in val and "_vertical_" in val["model"]:
                default_model = val["model"]
                if not textures and "textures" in val:
                    textures = val["textures"]
                break

    if "_vertical_" not in default_model:
        return False

    # Extract existing tilt models from whatever format is currently in the file
    old_variants = data.get("variants", {})
    tilt_models = {}
    if "tilt" in old_variants and isinstance(old_variants["tilt"], dict):
        for tilt, tilt_data in old_variants["tilt"].items():
            if isinstance(tilt_data, dict) and "model" in tilt_data:
                tilt_models[tilt] = tilt_data["model"]
    for key, val in old_variants.items():
        if "=" in key and "tilt=" in key and "horizontal=false" in key:
            if isinstance(val, dict) and "model" in val:
                for part in key.split(","):
                    if part.startswith("tilt="):
                        tilt = part.split("=", 1)[1]
                        if tilt not in tilt_models:
                            tilt_models[tilt] = val["model"]

    horiz_default = vertical_to_horizontal_model(default_model)

    # Build forge_marker format with standalone property blocks.
    # Order matters: horizontal AFTER tilt so it overrides the tilt model.
    new_data = {
        "forge_marker": 1,
        "defaults": {
            "model": default_model,
            "textures": textures
        },
        "variants": {
            "facing": dict(FACINGS),
            "tilt": {"none": {}},
            "horizontal": {
                "false": {},
                "true": {"model": horiz_default}
            },
            "inventory": [{}]
        }
    }

    # Add tilt models
    for tilt in TILT_STATES:
        if tilt == "none":
            continue
        if tilt in tilt_models:
            new_data["variants"]["tilt"][tilt] = {"model": tilt_models[tilt]}
        else:
            new_data["variants"]["tilt"][tilt] = {}

    with open(filepath, "w", newline="\n") as f:
        json.dump(new_data, f, indent=2)
        f.write("\n")
    return True


def update_fitted_blockstate(filepath):
    """Update a doghouse/hawk blockstate: add horizontal to all combined keys."""
    with open(filepath, "r") as f:
        data = json.load(f)

    old_variants = data.get("variants", {})
    new_variants = {}

    for key, val in old_variants.items():
        if key == "inventory":
            new_variants[key] = val
            continue

        if "=" not in key:
            continue

        props = {}
        for part in key.split(","):
            if "=" in part:
                pname, pval = part.split("=", 1)
                props[pname] = pval

        if "horizontal" in props:
            sorted_key = ",".join(f"{k}={v}" for k, v in sorted(props.items()))
            new_variants[sorted_key] = val
        else:
            for hz in ["false", "true"]:
                props_copy = dict(props)
                props_copy["horizontal"] = hz
                sorted_key = ",".join(f"{k}={v}" for k, v in sorted(props_copy.items()))
                new_variants[sorted_key] = dict(val)

    if "inventory" not in new_variants and "inventory" in old_variants:
        new_variants["inventory"] = old_variants["inventory"]

    data["variants"] = new_variants

    with open(filepath, "w", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    return True


def main():
    updated = 0

    for fp in sorted(glob.glob(os.path.join(BLOCKSTATES_DIR, "tlborder*.json"))):
        base = os.path.basename(fp)
        if base.startswith("tlhborder"):
            continue
        if update_standard_blockstate(fp):
            print(f"  OK (standard): {base}")
            updated += 1

    for pat in ["tldoghouseborder*.json", "tlhawkborder*.json"]:
        for fp in sorted(glob.glob(os.path.join(BLOCKSTATES_DIR, pat))):
            base = os.path.basename(fp)
            if update_fitted_blockstate(fp):
                print(f"  OK (fitted): {base}")
                updated += 1

    # Horizontal-dedicated: keep forge format, ensure horizontal property
    for fp in sorted(glob.glob(os.path.join(BLOCKSTATES_DIR, "tlhborder*.json"))):
        base = os.path.basename(fp)
        with open(fp) as f:
            data = json.load(f)
        variants = data.get("variants", {})
        if "horizontal" not in variants:
            new_variants = {}
            for k, v in variants.items():
                if k == "inventory":
                    new_variants["horizontal"] = {"false": {}, "true": {}}
                new_variants[k] = v
            if "horizontal" not in new_variants:
                new_variants["horizontal"] = {"false": {}, "true": {}}
            data["variants"] = new_variants
            with open(fp, "w", newline="\n") as f:
                json.dump(data, f, indent=2)
                f.write("\n")
        print(f"  OK (horizontal): {base}")
        updated += 1

    print(f"\nDone: {updated} updated")


if __name__ == "__main__":
    main()
