#!/usr/bin/env python3
"""Every asset the bridge cables ship (CSM: Roads & Traffic): the four anchor blocks' models and
blockstates, the Bridge Cable Tool's sprite and item model, and their lang lines.

    python dev-env-utils/scripts/gen_bridge_cables.py
    python dev-env-utils/scripts/gen_bridge_cables.py --check
    python dev-env-utils/scripts/gen_bridge_cables.py --fragments   # the tab lines

The cable itself is not here: `TileEntityBridgeCableAnchorRenderer` draws it (procedural tubes,
no model, no texture but `white1px`). What is here is the hardware a cable ends on, and the
numbers that matter are shared with `BridgeCableAnchorKind.java`: where a cable meets each anchor
(the socket's boss face, the clevis's pin, the saddle's groove, the deck anchor's pedestal top)
and each hitbox. Change one, change the other.

Every model is drawn facing north, as the Java numbers are: a wall-mounted anchor (socket,
clevis) stands out of a wall on its south side toward the north, and the blockstate turns it to
the other five faces (y 90 east, y 180 south, y 270 west, x 270 up, x 90 down). The saddle and
the deck anchor sit on top of something; their groove and fork run north-south and the
blockstate turns them four ways only.

No new block textures: the anchors are drawn in the sign trusses' galvanized plate and chord
textures, which Roads already ships, so the block atlas does not grow.

Round parts are exact octagons, four rectangles with two of them turned 45 degrees (a square plus
the same square turned 45 is a star, not an octagon); see life_safety_gen_common.py.
"""

import argparse
import filecmp
import io
import json
import math
import os
import shutil
import sys
import tempfile

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_scaffold as sc  # noqa: E402 -- its element helper fits every face's UVs
import model_depth  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "modules", "roads", "src", "main", "resources", "assets", "csm")
FOLDER = "trafficaccessories/bridgecable"
MODEL_DIR = os.path.join(ASSETS, "models", "block", FOLDER)
STATE_DIR = os.path.join(ASSETS, "blockstates")
ITEM_TEX_DIR = os.path.join(ASSETS, "textures", "items")
ITEM_MODEL_DIR = os.path.join(ASSETS, "models", "item")
LANG_DIR = os.path.join(ASSETS, "lang")
MODEL_REF = "csm:" + FOLDER + "/%s"

PLATE = "csm:blocks/trafficaccessories/truss/truss_plate"
CHORD = "csm:blocks/trafficaccessories/truss/truss_chord"

SOCKET = "bridge_cable_socket"
CLEVIS = "bridge_cable_clevis"
SADDLE = "bridge_cable_saddle"
DECK = "bridge_cable_deck_anchor"
TOOL = "bridge_cable_tool"
WALL_MOUNTED = (SOCKET, CLEVIS)
BLOCKS = (SOCKET, CLEVIS, SADDLE, DECK)

LANGS = ("en_us", "es_es", "de_de", "sv_se")
NAMES = {
    "tile.%s.name" % SOCKET: ("Bridge Cable Socket", "Anclaje de Tirante de Puente",
                              "Brückenseil-Verankerung", "Brokabelfäste"),
    "tile.%s.name" % CLEVIS: ("Bridge Cable Clevis", "Horquilla de Cable de Puente",
                              "Brückenseil-Gabelkopf", "Brokabelgaffel"),
    "tile.%s.name" % SADDLE: ("Suspension Cable Saddle", "Silla de Cable de Suspensión",
                              "Tragseil-Sattel", "Bärkabelsadel"),
    "tile.%s.name" % DECK: ("Bridge Cable Deck Anchor", "Anclaje de Cable en Tablero",
                            "Brückenseil-Fahrbahnanker", "Brokabelfäste för Brobana"),
    "item.%s.name" % TOOL: ("Bridge Cable Tool", "Herramienta de Cables de Puente",
                            "Brückenseil-Werkzeug", "Brokabelverktyg"),
}

# Where a cable meets each anchor, in pixels, facing north (BridgeCableAnchorKind's numbers).
SOCKET_BOSS_FACE_Z = 11.0      # attach -3/16: 8 + 3
CLEVIS_PIN_Z = 7.0             # attach +1/16: 8 - 1
SADDLE_GROOVE_Y = 13.0         # attach 13/16 (an 8 px main cable's centre)
DECK_PEDESTAL_TOP = 6.0        # attach 6/16

box = sc._box


def octagon(cx, cy, z0, z1, width, tex, axis="z"):
    """An exact octagon prism, flat-to-flat `width`, centred on (cx, cy) across the axis it runs
    along, from z0 to z1 along it. Four rectangles: two square to the axes, two turned 45."""
    half = width / 2.0
    side = width * math.tan(math.radians(22.5)) / 2.0
    out = []
    for angle in (0, 45):
        for a, b in ((half, side), (side, half)):
            if axis == "z":
                el = box(cx - a, cy - b, z0, cx + a, cy + b, z1, tex)
                origin = [cx, cy, (z0 + z1) / 2.0]
            else:  # along y: (cx, cy) is (x, z)
                el = box(cx - a, z0, cy - b, cx + a, z1, cy + b, tex)
                origin = [cx, (z0 + z1) / 2.0, cy]
            if angle:
                el["rotation"] = {"origin": origin, "axis": axis, "angle": 45}
            out.append(el)
    return out


def socket():
    els = [box(3, 3, 14, 13, 13, 16, PLATE)]                       # bearing plate on the pylon
    for x, y in ((3.5, 3.5), (11.5, 3.5), (3.5, 11.5), (11.5, 11.5)):
        els.append(box(x, y, 13.25, x + 1, y + 1, 14, CHORD))       # anchor bolts
    els += octagon(8, 8, SOCKET_BOSS_FACE_Z, 14, 6, CHORD)         # the boss the pipe leaves
    return els


def clevis():
    els = [box(4, 4, 14, 12, 12, 16, PLATE)]                       # base plate
    els.append(box(5, 5, 5, 6.5, 11, 14, PLATE))                    # fork lugs
    els.append(box(9.5, 5, 5, 11, 11, 14, PLATE))
    els += octagon(8, 8, 12, 14, 5, CHORD)                          # the lugs' boss
    # the pin, across the fork at the point the cable meets it, with its heads outside the lugs
    pin = octagon(CLEVIS_PIN_Z, 8, 4.25, 11.75, 2, CHORD, axis="z")
    for el in pin:                                                  # turn z-run into x-run
        f, t = el["from"], el["to"]
        el["from"] = [f[2], f[1], f[0]]
        el["to"] = [t[2], t[1], t[0]]
        if "rotation" in el:
            o = el["rotation"]["origin"]
            el["rotation"] = {"origin": [o[2], o[1], o[0]], "axis": "x", "angle": 45}
        el["faces"] = box(el["from"][0], el["from"][1], el["from"][2],
                          el["to"][0], el["to"][1], el["to"][2], CHORD)["faces"]
    els += pin
    return els


def saddle():
    els = [box(1, 0, 1, 15, 2, 15, PLATE)]                          # bed plate on the tower
    els.append(box(2, 2, 2, 14, 9, 14, PLATE))                      # the casting
    els.append(box(2, 9, 2, 4, 14, 14, PLATE))                      # cheeks either side
    els.append(box(12, 9, 2, 14, 14, 14, PLATE))
    els.append(box(4, 9, 3, 12, 9.5, 13, CHORD))                    # groove liner
    for z in (4, 8, 12):                                            # stiffeners
        els.append(box(1, 2, z - 0.5, 2, 8, z + 0.5, PLATE))
        els.append(box(14, 2, z - 0.5, 15, 8, z + 0.5, PLATE))
    return els


def deck_anchor():
    els = [box(2, 0, 2, 14, 2, 14, PLATE)]                          # base plate on the beam
    for x, z in ((3, 3), (12, 3), (3, 12), (12, 12)):
        els.append(box(x, 2, z, x + 1, 3, z + 1, CHORD))            # anchor bolts
    els += octagon(8, 8, 2, DECK_PEDESTAL_TOP, 6, CHORD, axis="y")  # the pedestal
    els += octagon(8, 8, 2, 3, 9, PLATE, axis="y")                  # its base flange
    return els


MODELS = {SOCKET: socket, CLEVIS: clevis, SADDLE: saddle, DECK: deck_anchor}


def model(name):
    return {"parent": "block/block", "ambientocclusion": False,
            "textures": {"particle": PLATE}, "elements": MODELS[name]()}


def blockstate(name):
    if name in WALL_MOUNTED:
        facing = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
                  "up": {"x": 270}, "down": {"x": 90}}
    else:
        # placed facing one of four ways; up and down are never placed but must still resolve
        facing = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
                  "up": {}, "down": {}}
    return {"forge_marker": 1,
            "defaults": {"model": MODEL_REF % name},
            "variants": {"facing": facing, "inventory": [{}]}}


def tool_sprite():
    """The tool: a white stay cable running down from a pylon socket to a deck anchor, with a
    hand coil of cable, readable at 16 px."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    dark = (60, 64, 70, 255)
    steel = (150, 156, 162, 255)
    white = (236, 236, 232, 255)
    shadow = (170, 172, 168, 255)
    d.rectangle([1, 1, 3, 6], fill=steel)                           # pylon socket
    d.rectangle([1, 1, 1, 6], fill=dark)
    d.line([(3, 3), (13, 12)], fill=white, width=2)                 # the stay
    d.line([(4, 5), (13, 13)], fill=shadow, width=1)
    d.rectangle([11, 12, 15, 14], fill=steel)                       # deck anchor
    d.rectangle([11, 14, 15, 14], fill=dark)
    d.ellipse([1, 9, 7, 15], outline=(192, 54, 44, 255))            # a coil of orange cable
    d.ellipse([2, 10, 6, 14], outline=(150, 40, 32, 255))
    return img


def tool_model():
    return {"parent": "item/generated", "textures": {"layer0": "csm:items/" + TOOL}}


def _dump(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(json.dumps(data, indent=2) + "\n")


def write_lang(lang_dir, source_dir):
    """Writes the lang lines into each language file, keeping its line endings: a key already
    there has its value replaced, a missing key is appended."""
    written = []
    for i, lang in enumerate(LANGS):
        name = lang + ".lang"
        with open(os.path.join(source_dir, name), "rb") as fh:
            raw = fh.read()
        text = raw.decode("utf-8")
        eol = "\r\n" if "\r\n" in text else "\n"
        lines = text.split(eol)
        if lines and lines[-1] == "":
            lines.pop()
        for key, names in NAMES.items():
            entry = "%s=%s" % (key, names[i])
            for j, line in enumerate(lines):
                if line.split("=", 1)[0] == key:
                    lines[j] = entry
                    break
            else:
                lines.append(entry)
        os.makedirs(lang_dir, exist_ok=True)
        with open(os.path.join(lang_dir, name), "wb") as fh:
            fh.write((eol.join(lines) + eol).encode("utf-8"))
        written.append(("lang", name))
    return written


def write_all(model_dir, state_dir, item_tex_dir, item_model_dir, lang_dir):
    written = []
    for name in BLOCKS:
        _dump(os.path.join(model_dir, name + ".json"), model(name))
        written.append(("model", name + ".json"))
        _dump(os.path.join(state_dir, name + ".json"), blockstate(name))
        written.append(("state", name + ".json"))
    os.makedirs(item_tex_dir, exist_ok=True)
    tool_sprite().save(os.path.join(item_tex_dir, TOOL + ".png"))
    written.append(("itemtex", TOOL + ".png"))
    _dump(os.path.join(item_model_dir, TOOL + ".json"), tool_model())
    written.append(("itemmodel", TOOL + ".json"))
    model_depth.separate_dirs([(model_dir, MODEL_DIR), (state_dir, STATE_DIR),
                               (item_tex_dir, ITEM_TEX_DIR), (item_model_dir, ITEM_MODEL_DIR)],
                              written, _dump)
    written += write_lang(lang_dir, LANG_DIR)
    return written


def fragments():
    pkg = "com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable"
    lines = ["## tab (CsmTabTrafficAccessories, after the span wire tool)"]
    for name in BLOCKS:
        lines.append('    initTabBlock(new %s.BlockBridgeCableAnchor("%s"));' % (pkg, name))
    lines.append("    initTabItem(%s.ItemBridgeCableTool.class, fmlPreInitializationEvent);" % pkg)
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    args = ap.parse_args()
    if args.fragments:
        print(fragments())
        return 0
    roots = {"model": MODEL_DIR, "state": STATE_DIR, "itemtex": ITEM_TEX_DIR,
             "itemmodel": ITEM_MODEL_DIR, "lang": LANG_DIR}
    if not args.check:
        n = len(write_all(MODEL_DIR, STATE_DIR, ITEM_TEX_DIR, ITEM_MODEL_DIR, LANG_DIR))
        print("Wrote %d bridge cable files" % n)
        return 0
    tmp = tempfile.mkdtemp(prefix="csm_bridge_cable_")
    try:
        t = {k: os.path.join(tmp, k) for k in roots}
        written = write_all(t["model"], t["state"], t["itemtex"], t["itemmodel"], t["lang"])
        drift = [os.path.relpath(os.path.join(roots[k], f), REPO) for k, f in written
                 if not os.path.exists(os.path.join(roots[k], f))
                 or not filecmp.cmp(os.path.join(roots[k], f), os.path.join(t[k], f),
                                    shallow=False)]
        if drift:
            print("DRIFT: %d file(s) differ from the generator:" % len(drift))
            for p in drift:
                print("  " + p)
            return 1
        print("%d generated bridge cable files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
