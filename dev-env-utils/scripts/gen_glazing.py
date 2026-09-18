#!/usr/bin/env python3
"""Every asset the glazing ships: glass that joins into one window.

    python dev-env-utils/scripts/gen_glazing.py
    python dev-env-utils/scripts/gen_glazing.py --check
    python dev-env-utils/scripts/gen_glazing.py --fragments   # lang lines to paste

Eight kinds of glass -- clear; grey, bronze and blue tint; one-way; wired; bullet-resistant;
frosted -- each as a full block (BlockGlazing) and a pane (BlockGlazingPane), sixteen blocks.

Glass of one kind joins into one window with no seams, and a thin dark bronze frame is drawn only
around the window's outside:

- a block draws no face against the same glass (its class culls those), and a frame rail only along
  an edge where both faces are on the outside -- the same rule the site containers use;
- a pane's sides are none / edge / glass (see BlockGlazingPane.Side): a frame at an edge, along the
  top where no pane of the same glass is above and along the bottom where none is below, and a
  mullion where the pane turns, branches or ends free.

One-way glass is dark on its outside face and clear on its inside one, and that is all it needs:
Minecraft never draws the back of a face. A pane's arm is drawn once running east and once running
west (not turned 180 degrees, which would move the dark face to the other side of the glass on
half of every pane), and the blockstate turns those two onto the pane's other sides by its facing.

The frame rails stand a quarter pixel proud of the glass face, so an opaque frame and a translucent
face never share a plane.
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
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "glazing")
ITEM_TEX_DIR = os.path.join(MODULE, "textures", "items", "glazing")
MODEL_DIR = os.path.join(MODULE, "models", "block", "glazing")
ITEM_MODEL_DIR = os.path.join(MODULE, "models", "item")
STATE_DIR = sc.STATE_DIR
TEX_REF = "csm:blocks/glazing/%s"
ITEM_TEX_REF = "csm:items/glazing/%s"
MODEL_REF = "csm:glazing/%s"

# kind -> (glass colour RGBA, name in each language). Order is creative order.
KINDS = {
    "clear": ((214, 232, 238, 46), ("Clear Glass", "Vidrio Transparente", "Klarglas",
                                    "Klart Glas")),
    "grey": ((66, 72, 78, 150), ("Grey Tinted Glass", "Vidrio Tintado Gris",
                                 "Grau getöntes Glas", "Grått Tonat Glas")),
    "bronze": ((116, 90, 60, 150), ("Bronze Tinted Glass", "Vidrio Tintado Bronce",
                                    "Bronze getöntes Glas", "Bronsfärgat Tonat Glas")),
    "blue": ((56, 98, 138, 140), ("Blue Tinted Glass", "Vidrio Tintado Azul",
                                  "Blau getöntes Glas", "Blått Tonat Glas")),
    "oneway": ((120, 130, 140, 60), ("One-Way Glass", "Vidrio Espía", "Spionspiegelglas",
                                      "Envägsglas")),
    "wired": ((214, 232, 238, 56), ("Wired Glass", "Vidrio Armado", "Drahtglas", "Trådglas")),
    "bullet": ((200, 230, 208, 64), ("Bullet-Resistant Glass", "Vidrio Antibalas",
                                     "Beschusshemmendes Glas", "Skottsäkert Glas")),
    "frosted": ((236, 240, 242, 205), ("Frosted Glass", "Vidrio Esmerilado", "Milchglas",
                                       "Frostat Glas")),
}
# One-way glass's outside: fully opaque. Any alpha at all lets the inside show through, and the
# point of it is that from outside it does not; the inside face stays clear either way.
ONE_WAY_OUTSIDE = (46, 56, 70, 255)
FRAME = (58, 48, 38)
PANE_NAMES = ("%s Pane", "Panel de %s", "Glasscheibe (%s)", "Glasruta (%s)")

BLOCKS = {}
for _k, (_rgba, _names) in KINDS.items():
    BLOCKS["glass_" + _k] = _names
    BLOCKS["glass_pane_" + _k] = tuple(f % n for f, n in zip(PANE_NAMES, _names))


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

def flat(rgba, seed, noise=0, alpha_noise=0, size=16):
    """Glass: one even tint. Anything drawn in it -- a streak, a glint -- would repeat on every
    block of a big window and read as a pattern, so there is nothing but a little noise."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            d = rng.uniform(-noise, noise) if noise else 0
            a = rgba[3] + (rng.uniform(-alpha_noise, alpha_noise) if alpha_noise else 0)
            px[x, y] = tuple(max(0, min(255, int(round(c + d)))) for c in rgba[:3]) + (
                max(0, min(255, int(round(a)))),)
    return img


def wired(rgba):
    """Wired glass: clear glass with a square steel mesh set in it, 32 px so the wire can be half
    a pixel and the mesh four to a block."""
    img = flat(rgba, 20261601, size=32)
    px = img.load()
    for y in range(32):
        for x in range(32):
            if x % 8 == 0 or y % 8 == 0:
                px[x, y] = (112, 116, 120, 255)
    return img


def frame_texture():
    return gen_logistics._canvas(20261602, FRAME, 4)[0]


def pane_icon(glass):
    """A pane's inventory icon: the glass inside a frame."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    g = glass.load()
    gw = glass.size[0]
    for y in range(16):
        for x in range(16):
            if x in (1, 14) or y in (1, 14):
                px[x, y] = FRAME + (255,)
            elif 1 < x < 14 and 1 < y < 14:
                px[x, y] = g[x * gw // 16, y * gw // 16]
    return img


def textures():
    out = {"glazing_frame": frame_texture(),
           "glass_oneway_outside": flat(ONE_WAY_OUTSIDE, 20261603, noise=2)}
    for i, (k, (rgba, _)) in enumerate(KINDS.items()):
        if k == "wired":
            out["glass_" + k] = wired(rgba)
        elif k == "frosted":
            out["glass_" + k] = flat(rgba, 20261610 + i, noise=5, alpha_noise=12)
        else:
            out["glass_" + k] = flat(rgba, 20261610 + i, noise=2)
    return out


def icons(tex):
    out = {}
    for k in KINDS:
        glass = tex["glass_oneway_outside"] if k == "oneway" else tex["glass_" + k]
        out["glass_pane_" + k] = pane_icon(glass)
    return out


# --------------------------------------------------------------------------------------------
# Geometry
# --------------------------------------------------------------------------------------------

box = sc._box
retex = gen_logistics.retex
PROUD = 0.25          # how far a frame rail stands out of the glass face
RAIL = 1.0            # frame section on a block


def cube(one_way):
    """The block's six faces, each culled against the same glass by the class. One-way: the north
    face is the outside (the blockstate turns it to the facing)."""
    el = box(0, 0, 0, 16, 16, 16, "#glass")
    for face in el["faces"]:
        el["faces"][face]["cullface"] = face
    if one_way:
        el["faces"]["north"]["texture"] = "#outside"
    return [el]


def block_post():
    """The frame on the block's north-west vertical edge."""
    return [box(-PROUD, 0, -PROUD, RAIL, 16, RAIL, "#frame")]


def block_rail_top():
    """The frame along the top of the block's north face."""
    return [box(0, 16 - RAIL, -PROUD, 16, 16 + PROUD, RAIL, "#frame")]


def block_rail_bottom():
    return [box(0, -PROUD, -PROUD, 16, RAIL, RAIL, "#frame")]


PANE = (7.5, 8.5)     # the glass
FRAME_Z = (7.0, 9.0)  # a pane's frame, proud of the glass by half a pixel either side


def pane_arm(east, one_way):
    """The glass from the centre to the east (or west) edge; north is the outside."""
    x0, x1 = (8, 16) if east else (0, 8)
    el = box(x0, 0, PANE[0], x1, 16, PANE[1], "#glass", faces=("north", "south"))
    if one_way:
        el["faces"]["north"]["texture"] = "#outside"
    return [el]


def pane_edge():
    """The frame where the pane meets a wall or different glass at the east edge."""
    return [box(15, 0, FRAME_Z[0], 16, 16, FRAME_Z[1], "#frame")]


def pane_top():
    """The frame along the top of the east arm."""
    return [box(8, 15, FRAME_Z[0], 16, 16, FRAME_Z[1], "#frame")]


def pane_bottom():
    return [box(8, 0, FRAME_Z[0], 16, 1, FRAME_Z[1], "#frame")]


def pane_post():
    """The mullion at the centre."""
    return [box(FRAME_Z[0], 0, FRAME_Z[0], FRAME_Z[1], 16, FRAME_Z[1], "#frame")]


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


def _around(elements):
    """The elements turned onto all four sides, for an inventory model (gen_facilities' turn)."""
    import gen_facilities
    out = []
    for q in range(4):
        out += gen_facilities._turn(elements, q)
    return out


def _textures(kind):
    t = {"glass": TEX_REF % ("glass_" + kind), "frame": TEX_REF % "glazing_frame"}
    if kind == "oneway":
        t["outside"] = TEX_REF % "glass_oneway_outside"
    return t


def models():
    out = {}
    frame = {"frame": TEX_REF % "glazing_frame"}
    out["glazing_post"] = _model(block_post(), frame, "frame")
    out["glazing_rail_top"] = _model(block_rail_top(), frame, "frame")
    out["glazing_rail_bottom"] = _model(block_rail_bottom(), frame, "frame")
    out["glazing_pane_edge"] = _model(pane_edge(), frame, "frame")
    out["glazing_pane_top"] = _model(pane_top(), frame, "frame")
    out["glazing_pane_bottom"] = _model(pane_bottom(), frame, "frame")
    out["glazing_pane_post"] = _model(pane_post(), frame, "frame")
    for k in KINDS:
        tex = _textures(k)
        one_way = k == "oneway"
        out["glass_%s_cube" % k] = _model(cube(one_way), tex, "glass", parent="block/block")
        lone = (cube(one_way) + _around(block_post()) + _around(block_rail_top())
                + _around(block_rail_bottom()))
        out["glass_%s_inventory" % k] = _model(lone, tex, "glass", parent="block/block")
        out["glass_pane_%s_east" % k] = _model(pane_arm(True, one_way), tex, "glass")
        out["glass_pane_%s_west" % k] = _model(pane_arm(False, one_way), tex, "glass")
    return out


def item_models():
    return {"glass_pane_" + k: {"parent": "item/generated",
                                "textures": {"layer0": ITEM_TEX_REF % ("glass_pane_" + k)}}
            for k in KINDS}


def _apply(model, rot=0):
    a = {"model": MODEL_REF % model}
    if rot:
        a["y"] = rot
    return a


SIDES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
CORNERS = ((("north", "west"), 0), (("north", "east"), 90), (("south", "east"), 180),
           (("south", "west"), 270))
# The pane's frame parts are drawn on its east side; the rotation that turns them onto each side.
PANE_SIDES = (("east", 0), ("south", 90), ("west", 180), ("north", 270))
LONE = {"north": "none", "east": "none", "south": "none", "west": "none"}
ARM = "edge|glass"


def block_state(k):
    parts = []
    if k == "oneway":
        for side, rot in SIDES:
            parts.append({"when": {"facing": side}, "apply": _apply("glass_oneway_cube", rot)})
    else:
        parts.append({"apply": _apply("glass_%s_cube" % k)})
    for (a, b), rot in CORNERS:
        parts.append({"when": {a: "false", b: "false"}, "apply": _apply("glazing_post", rot)})
    for side, rot in SIDES:
        parts.append({"when": {side: "false", "up": "false"},
                      "apply": _apply("glazing_rail_top", rot)})
        parts.append({"when": {side: "false", "down": "false"},
                      "apply": _apply("glazing_rail_bottom", rot)})
    return {"variants": {"inventory": {"model": MODEL_REF % ("glass_%s_inventory" % k)}},
            "multipart": parts}


def pane_state(k):
    east = "glass_pane_%s_east" % k
    west = "glass_pane_%s_west" % k
    parts = []
    # The glass. An east or west arm keeps its outside to the north when the pane faces north
    # (or west, at a corner) and is turned half round otherwise; a north or south arm is one of
    # the same two turned a quarter.
    arms = (("east", ("north|west", east, 0), ("south|east", west, 180)),
            ("west", ("north|west", west, 0), ("south|east", east, 180)),
            ("south", ("east|north", east, 90), ("west|south", west, 270)),
            ("north", ("east|north", west, 90), ("west|south", east, 270)))
    for side, *choices in arms:
        for facing, model, rot in choices:
            parts.append({"when": {side: ARM, "facing": facing}, "apply": _apply(model, rot)})
    # A lone pane: drawn along x, framed at both ends.
    for facing, (model_e, rot_e), (model_w, rot_w) in (
            ("north|west", (east, 0), (west, 0)), ("south|east", (west, 180), (east, 180))):
        when = dict(LONE, facing=facing)
        parts.append({"when": when, "apply": _apply(model_e, rot_e)})
        parts.append({"when": dict(when), "apply": _apply(model_w, rot_w)})
    for rot in (0, 180):
        parts.append({"when": dict(LONE), "apply": _apply("glazing_pane_edge", rot)})
        parts.append({"when": dict(LONE, up="false"), "apply": _apply("glazing_pane_top", rot)})
        parts.append({"when": dict(LONE, down="false"),
                      "apply": _apply("glazing_pane_bottom", rot)})
    # The frame.
    for side, rot in PANE_SIDES:
        parts.append({"when": {side: "edge"}, "apply": _apply("glazing_pane_edge", rot)})
        parts.append({"when": {side: ARM, "up": "false"}, "apply": _apply("glazing_pane_top", rot)})
        parts.append({"when": {side: ARM, "down": "false"},
                      "apply": _apply("glazing_pane_bottom", rot)})
    parts.append({"when": {"post": "true"}, "apply": _apply("glazing_pane_post")})
    return {"multipart": parts}


def blockstates():
    out = {}
    for k in KINDS:
        out["glass_" + k] = block_state(k)
        out["glass_pane_" + k] = pane_state(k)
    return out


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def write_all(tex_dir, model_dir, state_dir, item_tex_dir, item_model_dir):
    written = []
    for d in (tex_dir, item_tex_dir):
        os.makedirs(d, exist_ok=True)
    tex = textures()
    for name, img in sorted(tex.items()):
        img.save(os.path.join(tex_dir, name + ".png"))
        written.append(("tex", name + ".png"))
    for name, img in sorted(icons(tex).items()):
        img.save(os.path.join(item_tex_dir, name + ".png"))
        written.append(("itemtex", name + ".png"))
    for name, body in sorted(models().items()):
        gen_cmu._write_json(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    for name, body in sorted(item_models().items()):
        gen_cmu._write_json(os.path.join(item_model_dir, name + ".json"), body)
        written.append(("itemmodel", name + ".json"))
    for name, body in sorted(blockstates().items()):
        gen_cmu._write_json(os.path.join(state_dir, name + ".json"), body)
        written.append(("state", name + ".json"))
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    return [("tile.%s.name" % name, dict(zip(LANGS, names))) for name, names in BLOCKS.items()]


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    return "\n".join(lines)


def main():
    import filecmp
    import shutil
    import tempfile
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
    roots = {"tex": TEX_DIR, "itemtex": ITEM_TEX_DIR, "model": MODEL_DIR,
             "itemmodel": ITEM_MODEL_DIR, "state": STATE_DIR}
    order = ("tex", "model", "state", "itemtex", "itemmodel")
    if not args.check:
        written = write_all(*(roots[k] for k in order))
        print("Wrote %d glazing files" % len(written))
        return 0
    tmp = tempfile.mkdtemp(prefix="csm_glazing_")
    try:
        tmp_roots = {k: os.path.join(tmp, k) for k in roots}
        for path in tmp_roots.values():
            os.makedirs(path, exist_ok=True)
        written = write_all(*(tmp_roots[k] for k in order))
        drifted = [os.path.relpath(os.path.join(roots[kind], f), REPO)
                   for kind, f in written
                   if not os.path.exists(os.path.join(roots[kind], f))
                   or not filecmp.cmp(os.path.join(roots[kind], f),
                                      os.path.join(tmp_roots[kind], f), shallow=False)]
        if drifted:
            print("DRIFT: %d file(s) differ from the generator:" % len(drifted))
            for path in drifted:
                print("  " + path)
            return 1
        print("%d generated glazing files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
