#!/usr/bin/env python3
"""
Generates horizontal backplate model JSON files from existing vertical backplate models.

Applies a 90-degree CCW rotation in the XY plane around center (8, 6) to transform
vertical backplate geometry into horizontal orientation. Validated against the hand-crafted
borderhorizontal.json reference model (all 8 elements match exactly).

Coordinate transform:
    new_from = [14 - old_to_y,   old_from_x - 2, old_from_z]
    new_to   = [14 - old_from_y, old_to_x - 2,   old_to_z]

Face remapping (90 CCW in XY):
    east -> up, up -> west, west -> down, down -> east
    north/south stay but get +90 UV rotation

Usage:
    python generate_horizontal_backplates.py
"""

import json
import os
import copy

# Paths
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
MODELS_DIR = os.path.join(
    PROJECT_ROOT,
    "src", "main", "resources", "assets", "csm", "models", "block",
    "trafficsignals", "shared_models"
)

# Model families to transform (vertical name -> (horizontal name, mirror_y))
# mirror_y: addon models need a Y-axis mirror after rotation so the tape edge
# (outer border) ends up on the correct side relative to the main signal.
# Each family has a base model + 4 tilt variants.
MODEL_FAMILIES = {
    "signal_backplate_vertical_3": ("signal_backplate_horizontal_3", False),
    "signal_backplate_vertical_1": ("signal_backplate_horizontal_1", False),
    "signal_backplate_vertical_addon_1": ("signal_backplate_horizontal_addon_1", True),
    "signal_backplate_vertical_addon_2": ("signal_backplate_horizontal_addon_2", True),
    "signal_backplate_888_vertical_3": ("signal_backplate_888_horizontal_3", False),
    "signal_backplate_8812_vertical_3": ("signal_backplate_8812_horizontal_3", False),
}

TILT_SUFFIXES = ["", "_left_tilt", "_right_tilt", "_left_angle", "_right_angle"]


def transform_element(element, mirror_y=False):
    """Transform a single model element from vertical to horizontal orientation.

    If mirror_y is True, applies an additional Y-axis mirror after the 90 CCW
    rotation. This is needed for addon backplates so the tape edge (outer border)
    ends up on the correct side relative to the main signal.
    """
    elem = copy.deepcopy(element)

    if not mirror_y:
        # Standard transform: 90 CCW around center (8, 6)
        old_from = elem["from"]
        old_to = elem["to"]
        elem["from"] = [
            round(14 - old_to[1], 4),
            round(old_from[0] - 2, 4),
            old_from[2]
        ]
        elem["to"] = [
            round(14 - old_from[1], 4),
            round(old_to[0] - 2, 4),
            old_to[2]
        ]
    else:
        # Combined transform: 90 CCW + Y-mirror around center (8, 6)
        # Equivalent to: new_from = [14 - old_to_y, 14 - old_to_x, z]
        #                new_to   = [14 - old_from_y, 14 - old_from_x, z]
        old_from = elem["from"]
        old_to = elem["to"]
        elem["from"] = [
            round(14 - old_to[1], 4),
            round(14 - old_to[0], 4),
            old_from[2]
        ]
        elem["to"] = [
            round(14 - old_from[1], 4),
            round(14 - old_from[0], 4),
            old_to[2]
        ]

    # Transform element-level rotation if present.
    # For tilt variants, the rotation is a Y-axis tilt (left/right from viewer).
    # The axis, angle, AND origin all stay unchanged — the tilt pivot point is the
    # same in world space regardless of whether the backplate geometry is vertical
    # or horizontal. Validated against hand-crafted borderhorizontal_left_tilt.json
    # which uses identical rotation params to the vertical left_tilt model.
    # (Only the element from/to coordinates change.)

    # Remap faces based on rotation:
    # Standard (90 CCW):        east->up, up->west, west->down, down->east
    # Mirrored (90 CCW + Y-flip): east->down, up->west, west->up, down->east
    # North and south stay but get UV rotation adjustment
    if "faces" in elem:
        old_faces = elem["faces"]
        new_faces = {}

        for face_name, face_data in old_faces.items():
            face_copy = copy.deepcopy(face_data)

            if face_name in ("north", "south"):
                # Z-axis faces: add 90 to UV rotation
                old_rot = face_copy.get("rotation", 0)
                new_rot = (old_rot + 90) % 360
                if new_rot == 0:
                    face_copy.pop("rotation", None)
                else:
                    face_copy["rotation"] = new_rot
                new_faces[face_name] = face_copy
            elif face_name == "east":
                old_rot = face_copy.get("rotation", 0)
                new_rot = (old_rot + 270) % 360
                if new_rot == 0:
                    face_copy.pop("rotation", None)
                else:
                    face_copy["rotation"] = new_rot
                # East -> Up (standard) or East -> Down (mirrored)
                new_faces["down" if mirror_y else "up"] = face_copy
            elif face_name == "up":
                old_rot = face_copy.get("rotation", 0)
                new_rot = (old_rot + 270) % 360
                if new_rot == 0:
                    face_copy.pop("rotation", None)
                else:
                    face_copy["rotation"] = new_rot
                new_faces["west"] = face_copy
            elif face_name == "west":
                old_rot = face_copy.get("rotation", 0)
                new_rot = (old_rot + 270) % 360
                if new_rot == 0:
                    face_copy.pop("rotation", None)
                else:
                    face_copy["rotation"] = new_rot
                # West -> Down (standard) or West -> Up (mirrored)
                new_faces["up" if mirror_y else "down"] = face_copy
            elif face_name == "down":
                old_rot = face_copy.get("rotation", 0)
                new_rot = (old_rot + 270) % 360
                if new_rot == 0:
                    face_copy.pop("rotation", None)
                else:
                    face_copy["rotation"] = new_rot
                new_faces["east"] = face_copy

        # Ensure consistent face ordering: north, east, south, west, up, down
        ordered_faces = {}
        for key in ["north", "east", "south", "west", "up", "down"]:
            if key in new_faces:
                ordered_faces[key] = new_faces[key]
        elem["faces"] = ordered_faces

    return elem


def transform_model(model_data, mirror_y=False):
    """Transform an entire model from vertical to horizontal."""
    result = copy.deepcopy(model_data)

    # Transform all elements
    if "elements" in result:
        result["elements"] = [transform_element(e, mirror_y) for e in result["elements"]]

        if mirror_y:
            # For addon models, re-center the result on the block.
            # The vertical addon model is offset from block center (it sits below/above
            # the main signal). After rotation, this offset transfers to a different axis,
            # pushing the model off-center. Re-center by computing the bounding box center
            # and translating to (8, 6) in XY.
            min_x = min(e["from"][0] for e in result["elements"])
            max_x = max(e["to"][0] for e in result["elements"])
            min_y = min(e["from"][1] for e in result["elements"])
            max_y = max(e["to"][1] for e in result["elements"])
            cx = (min_x + max_x) / 2
            cy = (min_y + max_y) / 2
            dx = round(8 - cx, 4)
            dy = round(6 - cy, 4)

            if dx != 0 or dy != 0:
                for elem in result["elements"]:
                    elem["from"][0] = round(elem["from"][0] + dx, 4)
                    elem["from"][1] = round(elem["from"][1] + dy, 4)
                    elem["to"][0] = round(elem["to"][0] + dx, 4)
                    elem["to"][1] = round(elem["to"][1] + dy, 4)
                    # Also translate the rotation origin if present
                    if "rotation" in elem:
                        elem["rotation"]["origin"][0] = round(
                            elem["rotation"]["origin"][0] + dx, 4)
                        elem["rotation"]["origin"][1] = round(
                            elem["rotation"]["origin"][1] + dy, 4)

    return result


def main():
    generated = 0
    skipped = 0

    for vert_base, (horiz_base, mirror_y) in MODEL_FAMILIES.items():
        for suffix in TILT_SUFFIXES:
            vert_name = vert_base + suffix
            horiz_name = horiz_base + suffix
            vert_path = os.path.join(MODELS_DIR, vert_name + ".json")
            horiz_path = os.path.join(MODELS_DIR, horiz_name + ".json")

            if not os.path.exists(vert_path):
                print(f"  SKIP (source missing): {vert_name}.json")
                skipped += 1
                continue

            with open(vert_path, "r") as f:
                model_data = json.load(f)

            horiz_model = transform_model(model_data, mirror_y)

            with open(horiz_path, "w", newline="\n") as f:
                json.dump(horiz_model, f, indent="\t")
                f.write("\n")

            print(f"  OK: {vert_name}.json -> {horiz_name}.json")
            generated += 1

    print(f"\nDone: {generated} generated, {skipped} skipped")


if __name__ == "__main__":
    main()
