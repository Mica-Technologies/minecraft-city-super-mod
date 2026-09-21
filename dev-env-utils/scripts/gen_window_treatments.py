#!/usr/bin/env python3
"""Every asset the window blinds, shades and curtains ship.

    python dev-env-utils/scripts/gen_window_treatments.py
    python dev-env-utils/scripts/gen_window_treatments.py --check
    python dev-env-utils/scripts/gen_window_treatments.py --fragments   # lang lines to paste

Nine blocks, one class (BlockWindowTreatment): a white venetian blind; white, grey and blackout
roller shades; a white vertical blind; beige, grey and navy curtains and a white sheer. Each hangs in
the cell on the room side of a window, drawn here with the window to the NORTH and turned by the
blockstate to the way it faces.

Blinds of one kind hung the same way join into one blind, and every part that belongs to the whole
blind rather than to a block is drawn only where it belongs:

- the headrail / cassette / rod along the top course only (up = false);
- a bottom rail along the bottom course only (down = false);
- a raised venetian blind's slat stack, and a half-lowered roller shade's bar, on the top or bottom
  course of the whole blind;
- a drawn-aside vertical blind's vanes, and an open curtain's bunched fabric, at the ends of the
  whole run (left = false, right = false: seen from inside, looking at the window).

States (the block's `state`): venetian 0 open, 1 tilted, 2 closed, 3 raised; roller 0 down, 1 half,
2 up; vertical 0 open, 1 closed, 2 drawn aside; curtain 0 closed, 1 open.

Nothing drawn in a texture here repeats as a pattern across a big blind: fabric and slat textures
are even, with the lines that make them read as slats or pleats running the whole way across.
"""

import argparse
import os
import random
import sys

from PIL import Image

import gen_cmu
import gen_logistics
import gen_scaffold as sc

REPO = sc.REPO
MODULE = sc.MODULE
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "interior")
MODEL_DIR = os.path.join(MODULE, "models", "block", "interior")
STATE_DIR = sc.STATE_DIR
TEX_REF = "csm:blocks/interior/%s"
MODEL_REF = "csm:interior/%s"

# registry name -> (kind, texture set, name in each language). Order is creative order.
BLOCKS = {
    "blind_venetian_white": ("venetian", "white", ("Venetian Blind (White)",
                                                   "Persiana Veneciana (Blanca)",
                                                   "Jalousie (Weiß)", "Persienn (Vit)")),
    "shade_roller_white": ("roller", "white", ("Roller Shade (White)", "Estor Enrollable (Blanco)",
                                               "Rollo (Weiß)", "Rullgardin (Vit)")),
    "shade_roller_grey": ("roller", "grey", ("Roller Shade (Grey)", "Estor Enrollable (Gris)",
                                             "Rollo (Grau)", "Rullgardin (Grå)")),
    "shade_roller_blackout": ("roller", "blackout", ("Roller Shade (Blackout)",
                                                     "Estor Enrollable (Opaco)",
                                                     "Rollo (Verdunkelung)",
                                                     "Rullgardin (Mörkläggning)")),
    "blind_vertical_white": ("vertical", "white", ("Vertical Blind (White)",
                                                   "Persiana Vertical (Blanca)",
                                                   "Lamellenvorhang (Weiß)",
                                                   "Lamellgardin (Vit)")),
    "curtain_beige": ("curtain", "beige", ("Curtain (Beige)", "Cortina (Beige)",
                                           "Vorhang (Beige)", "Gardin (Beige)")),
    "curtain_grey": ("curtain", "grey", ("Curtain (Grey)", "Cortina (Gris)", "Vorhang (Grau)",
                                         "Gardin (Grå)")),
    "curtain_navy": ("curtain", "navy", ("Curtain (Navy)", "Cortina (Azul Marino)",
                                         "Vorhang (Marineblau)", "Gardin (Marinblå)")),
    "curtain_sheer": ("curtain", "sheer", ("Sheer Curtain", "Cortina Visillo",
                                           "Gardine (Transparent)", "Skir Gardin")),
}

FABRIC = {"white": (234, 232, 224), "grey": (132, 134, 136), "blackout": (46, 46, 50),
          "beige": (206, 186, 150), "navy": (40, 52, 90)}
SHEER_ALPHA = 110


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

_shift = gen_logistics._shift
_canvas = gen_logistics._canvas


def slat():
    """A venetian slat: white aluminium, lit along its top edge."""
    img, px, rng = _canvas(20261701, (236, 236, 232), 3)
    for x in range(16):
        px[x, 0] = _shift((250, 250, 248), rng.uniform(-2, 2))
    return img


def slats_closed():
    """A closed venetian blind face on: slats edge to edge, a line at every slat's lip."""
    img, px, rng = _canvas(20261702, (232, 232, 228), 3)
    for y in range(16):
        d = -22 if y % 2 == 1 else 0
        for x in range(16):
            px[x, y] = _shift((232, 232, 228), d + rng.uniform(-2, 2))
    return img


def rail(rgb):
    """A headrail or bottom rail: painted metal, darker than the blind."""
    return _canvas(20261703, _shift(rgb, -26), 3)[0]


def fabric(name, seed):
    """A roller shade's fabric: an even weave, a faint horizontal thread every other row."""
    rgb = FABRIC[name]
    img, px, rng = _canvas(seed, rgb, 3)
    for y in range(0, 16, 2):
        for x in range(16):
            px[x, y] = _shift(rgb, -6 + rng.uniform(-2, 2))
    return img


def vane():
    """A vertical blind's vane: textured white fabric, woven up and down."""
    img, px, rng = _canvas(20261710, (228, 226, 218), 3)
    for x in range(0, 16, 2):
        for y in range(16):
            px[x, y] = _shift((228, 226, 218), -8 + rng.uniform(-2, 2))
    return img


def curtain(name, seed):
    """Curtain fabric: pleat shading down it, light and dark every two pixels. The sheer one is
    translucent."""
    rgb = FABRIC.get(name, (244, 244, 240))
    img, px, rng = _canvas(seed, rgb, 3)
    for x in range(16):
        d = {0: 8, 1: 0, 2: -10, 3: 0}[x % 4]
        for y in range(16):
            px[x, y] = _shift(rgb, d + rng.uniform(-2, 2))
            if name == "sheer":
                px[x, y] = px[x, y][:3] + (SHEER_ALPHA,)
    return img


def rod():
    """A curtain rod: dark bronze, to match the glazing's frame."""
    return _canvas(20261720, (58, 48, 38), 4)[0]


def textures():
    out = {"blind_slat": slat(), "blind_slats_closed": slats_closed(),
           "blind_rail_white": rail((236, 236, 232)), "blind_vane": vane(),
           "curtain_rod": rod()}
    for i, name in enumerate(("white", "grey", "blackout")):
        out["shade_fabric_" + name] = fabric(name, 20261704 + i)
        out["shade_rail_" + name] = rail(FABRIC[name] if name != "blackout" else (90, 90, 96))
    for i, name in enumerate(("beige", "grey", "navy", "sheer")):
        out["curtain_" + name] = curtain(name, 20261712 + i)
    return out


# --------------------------------------------------------------------------------------------
# Geometry -- window to the north
# --------------------------------------------------------------------------------------------

box = sc._box
retex = gen_logistics.retex
FACE = ("north", "south")


# Venetian.
V_TOP = 14.6


def v_headrail():
    return [box(0, V_TOP, 0.3, 16, 16, 2.3, "#rail")]


def v_bottom():
    return [box(0, 0, 0.5, 16, 0.6, 2.1, "#rail")]


# Eleven slats to a block, so the pitch divides the block exactly and the gap across a seam is the
# same as every other gap. At the 1.5 px it was, the last slat in a block sat at 14.4 and the next
# block's first at 16.9: a 2.5 px gap at every block, which a tall blind shows as a band of wider
# gaps at each seam (issue #219).
V_SLATS_PER_BLOCK = 11
V_SLAT_PITCH = 16.0 / V_SLATS_PER_BLOCK


def v_slat_ys(top):
    ys = []
    k = 0
    y = 0.9
    while y + 0.2 <= (V_TOP - 0.1 if top else 16):
        ys.append(round(y, 2))
        k += 1
        y = 0.9 + k * V_SLAT_PITCH
    return ys


def v_open(top):
    return [box(0, y, 0.6, 16, y + 0.2, 2.0, "#slat") for y in v_slat_ys(top)]


def v_tilted(top):
    els = []
    for y in v_slat_ys(top):
        el = box(0, y, 0.6, 16, y + 0.2, 2.0, "#slat")
        el["rotation"] = {"origin": [8, y + 0.1, 1.3], "axis": "x", "angle": 45}
        els.append(el)
    return els


def v_closed(top):
    return [box(0, 0, 1.2, 16, V_TOP if top else 16, 1.4, "#closed", faces=FACE)]


def v_raised():
    """The slats stacked up under the headrail."""
    return [box(0, 11.8, 0.6, 16, V_TOP, 2.0, "#closed")]


# Roller.
R_TOP = 14.0


def r_cassette():
    return [box(0, R_TOP, 0.3, 16, 16, 2.5, "#rail")]


def r_fabric(y0, y1):
    return [box(0, y0, 1.2, 16, y1, 1.4, "#fabric", faces=FACE)]


def r_bar(y):
    return [box(0, y, 0.9, 16, y + 0.8, 1.7, "#rail")]


# Vertical.
VT_TOP = 15.0


def vt_headrail():
    return [box(0, VT_TOP, 0.3, 16, 16, 2.5, "#rail")]


def vt_open(top):
    h = VT_TOP if top else 16
    return [box(x, 0, 0.5, x + 0.25, h, 2.5, "#vane") for x in range(1, 16, 2)]


def vt_closed(top):
    return [box(0, 0, 1.4, 16, VT_TOP if top else 16, 1.6, "#vane", faces=FACE)]


def vt_drawn(top):
    """The vanes stacked at the left end of the whole blind, seen from inside looking at the window
    (the west, x = 0, with the window to the north)."""
    h = VT_TOP if top else 16
    return [box(0, 0, 0.5, 3.5, h, 2.5, "#vane")]


# Curtain.
C_TOP = 14.8


def c_rod():
    return [box(0, C_TOP, 1.8, 16, C_TOP + 0.6, 2.4, "#rod")]


def c_rod_end(left):
    """What holds the rod up at one end of the run (issue #222): a bracket screwed to the wall with
    its arm under the rod, and a finial capping the rod's end.

    The finial reaches a little past the cell, as a real one stands past the window, so the rod's
    end face lies inside it instead of in the same plane as the finial's. Drawn for the left end
    (x = 0, seen from inside with the window to the north); the right end is its mirror.
    """
    parts = [
        # Wall plate, standing off the window's face by a hair.
        (1.2, C_TOP - 1.0, 0.1, 2.0, C_TOP + 0.2, 0.4),
        # Arm, from the plate out under the rod. Its top stops a hair under the rod line, where the
        # curtain's pleats also end, so the two tops are not one plane.
        (1.35, C_TOP - 0.5, 0.4, 1.85, C_TOP - 0.02, 2.2),
        # Finial, enclosing the rod's end.
        (-0.6, C_TOP - 0.3, 1.5, 0.4, C_TOP + 0.9, 2.7),
    ]
    els = []
    for i, (x0, y0, z0, x1, y1, z1) in enumerate(parts):
        if not left:
            x0, x1 = 16 - x1, 16 - x0
        # The finial pokes past the cell, where UVs taken from its position would run off the
        # rod's sprite into its neighbour's, so it samples a fixed patch instead.
        uv = (7, 7, 8, 8) if i == 2 else None
        els.append(box(x0, y0, z0, x1, y1, z1, "#rod", uv=uv))
    return els


def c_closed(top):
    """Pleats: panels two pixels wide, alternately forward and back."""
    h = C_TOP if top else 16
    els = []
    for i, x in enumerate(range(0, 16, 2)):
        off = 0.5 if i % 2 else 0.0
        els.append(box(x, 0, 0.8 + off, x + 2, h, 1.6 + off, "#fabric"))
    return els


def c_bunched(top, left):
    """An open curtain bunched at one end of the run: deep pleats in a narrow band."""
    h = C_TOP if top else 16
    x0 = 0.0 if left else 11.5
    els = []
    for i in range(3):
        x = x0 + i * 1.5
        off = 0.6 if i % 2 else 0.0
        els.append(box(x, 0, 0.5 + off, x + 1.5, h, 2.2 + off, "#fabric"))
    return els


# --------------------------------------------------------------------------------------------
# Models and blockstates
# --------------------------------------------------------------------------------------------

def _model(elements, textures, particle, parent=None):
    t = dict(textures)
    t["particle"] = textures[particle]
    m = {"textures": t, "elements": elements}
    if parent:
        m = {"parent": parent, **m}
    return m


def _t(name):
    return TEX_REF % name


def _textures(kind, tex):
    if kind == "venetian":
        return {"slat": _t("blind_slat"), "closed": _t("blind_slats_closed"),
                "rail": _t("blind_rail_white")}
    if kind == "roller":
        return {"fabric": _t("shade_fabric_" + tex), "rail": _t("shade_rail_" + tex)}
    if kind == "vertical":
        return {"vane": _t("blind_vane"), "rail": _t("blind_rail_white")}
    return {"fabric": _t("curtain_" + tex), "rod": _t("curtain_rod")}


def _part_models(kind, textures, p):
    """Every part model a kind uses, named {p}_<part>."""
    particle = next(iter(textures))
    m = {}

    def add(name, els):
        m[p + "_" + name] = _model(els, textures, particle)

    if kind == "venetian":
        add("headrail", v_headrail())
        add("bottom", v_bottom())
        for top in (False, True):
            s = "_top" if top else ""
            add("open" + s, v_open(top))
            add("tilted" + s, v_tilted(top))
            add("closed" + s, v_closed(top))
        add("raised", v_raised())
        lone = v_headrail() + v_closed(True) + v_bottom()
    elif kind == "roller":
        add("cassette", r_cassette())
        add("fabric", r_fabric(0, 16))
        add("fabric_top", r_fabric(0, R_TOP))
        add("fabric_half", r_fabric(8, R_TOP))
        add("bar_bottom", r_bar(0))
        add("bar_high", r_bar(15.2))
        add("bar_mid", r_bar(7.2))
        lone = r_cassette() + r_fabric(0, R_TOP) + r_bar(0)
    elif kind == "vertical":
        add("headrail", vt_headrail())
        for top in (False, True):
            s = "_top" if top else ""
            add("open" + s, vt_open(top))
            add("closed" + s, vt_closed(top))
            add("drawn" + s, vt_drawn(top))
        lone = vt_headrail() + vt_closed(True)
    else:
        add("rod", c_rod())
        add("rod_end_left", c_rod_end(True))
        add("rod_end_right", c_rod_end(False))
        for top in (False, True):
            s = "_top" if top else ""
            add("closed" + s, c_closed(top))
            add("left" + s, c_bunched(top, True))
            add("right" + s, c_bunched(top, False))
        lone = c_rod() + c_rod_end(True) + c_rod_end(False) + c_closed(True)
    m[p + "_inventory"] = _model(lone, textures, particle, parent="block/block")
    return m


def models():
    out = {}
    for name, (kind, tex, _) in BLOCKS.items():
        out.update(_part_models(kind, _textures(kind, tex), name))
    return out


SIDES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def _parts(p, conds):
    """One multipart part per facing for each (when, model): the model is turned to the facing."""
    parts = []
    for when, model in conds:
        for side, rot in SIDES:
            w = dict(when)
            w["facing"] = side
            a = {"model": MODEL_REF % (p + "_" + model)}
            if rot:
                a["y"] = rot
            parts.append({"when": w, "apply": a})
    return parts


def _course(state, model):
    """The part for {state}, the top course's version where the top course differs."""
    return [({"state": state, "up": "true"}, model), ({"state": state, "up": "false"}, model + "_top")]


def state_for(name):
    kind = BLOCKS[name][0]
    p = name
    if kind == "venetian":
        conds = [({"up": "false"}, "headrail"),
                 ({"down": "false", "state": "0|1|2"}, "bottom"),
                 ({"state": "3", "up": "false"}, "raised")]
        conds += _course("0", "open") + _course("1", "tilted") + _course("2", "closed")
    elif kind == "roller":
        conds = [({"up": "false"}, "cassette"),
                 # Down: fabric the whole way, the bar at the bottom.
                 ({"state": "0", "up": "true"}, "fabric"),
                 ({"state": "0", "up": "false"}, "fabric_top"),
                 ({"state": "0", "down": "false"}, "bar_bottom"),
                 # Half: every course but the bottom one covered, the bar at the bottom course's
                 # top; a blind one course high comes halfway down its only course.
                 ({"state": "1", "down": "true", "up": "true"}, "fabric"),
                 ({"state": "1", "down": "true", "up": "false"}, "fabric_top"),
                 ({"state": "1", "down": "false", "up": "true"}, "bar_high"),
                 ({"state": "1", "down": "false", "up": "false"}, "fabric_half"),
                 ({"state": "1", "down": "false", "up": "false"}, "bar_mid")]
    elif kind == "vertical":
        conds = [({"up": "false"}, "headrail")]
        conds += _course("0", "open") + _course("1", "closed")
        conds += [({"state": "2", "left": "false", "up": "true"}, "drawn"),
                  ({"state": "2", "left": "false", "up": "false"}, "drawn_top")]
    else:
        conds = [({"up": "false"}, "rod"),
                 ({"up": "false", "left": "false"}, "rod_end_left"),
                 ({"up": "false", "right": "false"}, "rod_end_right")]
        conds += _course("0", "closed")
        conds += [({"state": "1", "left": "false", "up": "true"}, "left"),
                  ({"state": "1", "left": "false", "up": "false"}, "left_top"),
                  ({"state": "1", "right": "false", "up": "true"}, "right"),
                  ({"state": "1", "right": "false", "up": "false"}, "right_top")]
    return {"variants": {"inventory": {"model": MODEL_REF % (p + "_inventory")}},
            "multipart": _parts(p, conds)}


def blockstates():
    return {name: state_for(name) for name in BLOCKS}


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    for name, img in sorted(textures().items()):
        img.save(os.path.join(tex_dir, name + ".png"))
        written.append(("tex", name + ".png"))
    for name, body in sorted(models().items()):
        gen_cmu._write_json(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    for name, body in sorted(blockstates().items()):
        gen_cmu._write_json(os.path.join(state_dir, name + ".json"), body)
        written.append(("state", name + ".json"))
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    return [("tile.%s.name" % name, dict(zip(LANGS, names)))
            for name, (_, _, names) in BLOCKS.items()]


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    ap.add_argument("--fragments", action="store_true",
                    help="print the lang lines and exit")
    args = ap.parse_args()
    if args.fragments:
        print(fragments())
        return 0
    roots = {"tex": TEX_DIR, "model": MODEL_DIR, "state": STATE_DIR}
    if args.check:
        return gen_logistics.check("window treatment", write_all, roots)
    written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR)
    print("Wrote %d window treatment files" % len(written))
    return 0


if __name__ == "__main__":
    sys.exit(main())
