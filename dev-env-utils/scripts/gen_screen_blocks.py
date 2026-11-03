#!/usr/bin/env python3
"""Every asset the screen blocks ship: breeze blocks and slatted timber screens.

    python dev-env-utils/scripts/gen_screen_blocks.py
    python dev-env-utils/scripts/gen_screen_blocks.py --check       # fails on drift
    python dev-env-utils/scripts/gen_screen_blocks.py --fragments   # lang and tab lines

BREEZE BLOCKS (BlockBreezeBlock, one class constructed by registry name) are the pierced concrete
screen units of mid-century and tropical buildings, laid up into a wall that light, air and a
view pass through: four patterns (circle, quatrefoil, diamond, squares) in three colours (white,
grey, sand). A real unit is about a foot square, so a block face shows four of them, two by two,
on a 32 px texture, each with a one-pixel rim, so two units side by side meet in a two-pixel joint.

A unit's pattern goes right through it, which a cube of cutout faces cannot show: through a hole
at an angle it would show nothing but the far side's holes. So the model is the cube's six faces
plus thin slices of the pattern inside it, every 2 px through its depth, drawn both ways in a
darker "web" texture, the inside of the holes in shade. Through a hole at an angle the slices
stack into the hole's walls. It is 6 + 14 quads a block; the block culls every face against
another breeze block (BlockBreezeBlock.shouldSideBeRendered), so a wall draws its two pierced
faces, its outside edges and its slices. The pattern runs one way through the block: the blockstate
stores the axis it is seen through, and every pattern is mirror-symmetric, so the far face lines
up with the near one whichever way it is drawn.

SLAT SCREENS (BlockSlatScreen, by name) are vertical timber slats, two pixels wide with two-pixel
gaps, standing along one edge of the cell (the wall to the NORTH as drawn, turned by the
blockstate), in teak, oak and walnut. The slats run on through a stack; a rail caps the top and
the bottom of a run (up / down = false), so the screen has one head and one sill however tall it
is. The slats are inset half a pixel from the rails' faces so no two faces share a plane.

Light comes from the upper left, as everywhere in the tab.
"""

import argparse
import math
import os
import random
import sys

from PIL import Image

import gen_cmu
import gen_logistics
import gen_scaffold as sc
import model_depth  # noqa: E402

REPO = sc.REPO
MODULE = sc.MODULE
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "buildingmaterials", "screens")
MODEL_DIR = os.path.join(MODULE, "models", "block", "buildingmaterials", "screens")
STATE_DIR = sc.STATE_DIR
TEX_REF = "csm:blocks/buildingmaterials/screens/%s"
MODEL_REF = "csm:buildingmaterials/screens/%s"

LANGS = gen_cmu.LANGS

# pattern -> name in each language. Order is creative order.
PATTERNS = {
    "circle": ("Circle", "Círculo", "Kreis", "Cirkel"),
    "quatrefoil": ("Quatrefoil", "Cuadrifolio", "Vierpass", "Fyrpass"),
    "diamond": ("Diamond", "Rombo", "Raute", "Romb"),
    "square": ("Squares", "Cuadrados", "Quadrate", "Kvadrater"),
}

# colour -> (body rgb, seed, name in each language)
COLOURS = {
    "white": ((232, 230, 222), 20261110, ("White", "Blanco", "Weiß", "Vit")),
    "grey": ((150, 150, 146), 20261111, ("Grey", "Gris", "Grau", "Grå")),
    "sand": ((212, 184, 140), 20261112, ("Sand", "Arena", "Sand", "Sand")),
}

BREEZE_NAME = ("Breeze Block", "Celosía de Hormigón", "Betonformstein", "Dekorblock")

# timber -> (body rgb, seed, name in each language)
TIMBERS = {
    "teak": ((150, 94, 54), 20261120, ("Teak", "Teca", "Teak", "Teak")),
    "oak": ((184, 140, 90), 20261121, ("Oak", "Roble", "Eiche", "Ek")),
    "walnut": ((108, 74, 50), 20261122, ("Walnut", "Nogal", "Nussbaum", "Valnöt")),
}

SLAT_NAME = ("Slatted Screen", "Celosía de Listones", "Lattenblende", "Ribbskärm")

SIZE = 32   # a breeze block face: two units by two
UNIT = 16   # one unit

_shift = gen_logistics._shift


def breeze_names():
    return ["breeze_block_%s_%s" % (p, c) for p in PATTERNS for c in COLOURS]


def slat_names():
    return ["slat_screen_%s" % t for t in TIMBERS]

# --------------------------------------------------------------------------------------------
# Breeze block textures
# --------------------------------------------------------------------------------------------


def _hole(pattern, lx, ly):
    """Whether pixel (lx, ly) of a 16 px unit is open. The unit has a one-pixel rim all round;
    inside it, centred coordinates run -6.5 .. 6.5."""
    if lx in (0, UNIT - 1) or ly in (0, UNIT - 1):
        return False
    cx, cy = lx - 7.5, ly - 7.5
    if pattern == "circle":
        return math.hypot(cx, cy) <= 6.1
    if pattern == "quatrefoil":
        return min(math.hypot(cx - px, cy - py)
                   for px, py in ((3.0, 0.0), (-3.0, 0.0), (0.0, 3.0), (0.0, -3.0))) <= 3.3
    if pattern == "diamond":
        # The centre diamond, and the four corner triangles that make diamonds with the next
        # units' across the joints
        d = abs(cx) + abs(cy)
        return d <= 4.5 or d >= 8.5
    if pattern == "square":
        # Four square holes, a two-pixel cross between them
        return abs(cx) >= 1.0 and abs(cy) >= 1.0
    raise ValueError(pattern)


def _mask(pattern):
    return [[_hole(pattern, x % UNIT, y % UNIT) for x in range(SIZE)] for y in range(SIZE)]


def face_texture(pattern, colour):
    """The pierced face: concrete with a fine grain, the joint between units a shade darker, each
    hole's edge lit or shaded by which side of it the light falls on."""
    rgb, seed, _ = COLOURS[colour]
    rng = random.Random(seed * 7 + sorted(PATTERNS).index(pattern))
    mask = _mask(pattern)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            if mask[y][x]:
                continue
            d = rng.uniform(-4, 4)
            if x % UNIT == 0 or y % UNIT == 0:
                d -= 12   # the mortar joint between units
            # A hole below or to the right: this is its top or left edge, which the light
            # catches; a hole above or to the left: its bottom or right edge, in shadow
            if mask[(y + 1) % SIZE][x] or mask[y][(x + 1) % SIZE]:
                d += 10
            if mask[(y - 1) % SIZE][x] or mask[y][(x - 1) % SIZE]:
                d -= 14
            px[x, y] = _shift(rgb, d)
    return img


def web_texture(pattern, colour):
    """The same pattern drawn inside the block: the walls of the holes, in shade."""
    rgb, seed, _ = COLOURS[colour]
    rng = random.Random(seed * 11 + sorted(PATTERNS).index(pattern))
    mask = _mask(pattern)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            if not mask[y][x]:
                px[x, y] = _shift(rgb, -40 + rng.uniform(-3, 3))
    return img


def edge_texture(colour):
    """A unit's solid edges, seen on the end, top and bottom of a wall: plain concrete with the
    joints between the units, two courses to a block."""
    rgb, seed, _ = COLOURS[colour]
    rng = random.Random(seed * 13)
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            d = rng.uniform(-4, 4)
            if x % UNIT == 0 or y % UNIT == 0:
                d -= 12
            px[x, y] = _shift(rgb, d)
    return img

# --------------------------------------------------------------------------------------------
# Slat screen textures
# --------------------------------------------------------------------------------------------


def timber_texture(timber):
    """Oiled timber, the grain streaking up the slat, each column its own shade."""
    rgb, seed, _ = TIMBERS[timber]
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for x in range(16):
        tone = rng.uniform(-10, 10)
        for y in range(16):
            px[x, y] = _shift(rgb, tone + rng.uniform(-4, 4))
    return img


def textures():
    out = {}
    for pattern in PATTERNS:
        for colour in COLOURS:
            name = "breeze_block_%s_%s" % (pattern, colour)
            out[name] = face_texture(pattern, colour)
            out[name + "_web"] = web_texture(pattern, colour)
    for colour in COLOURS:
        out["breeze_block_edge_%s" % colour] = edge_texture(colour)
    for timber in TIMBERS:
        out["slat_screen_%s" % timber] = timber_texture(timber)
    return out

# --------------------------------------------------------------------------------------------
# Models
# --------------------------------------------------------------------------------------------

box = sc._box

# The planes inside a breeze block the pattern is drawn on, in px from its north face
SLICES = tuple(range(2, 16, 2))


def _cull(el, *faces):
    for f in faces:
        el["faces"][f]["cullface"] = f
    return el


def breeze_model(name, colour):
    """The pattern seen to the north and south, the solid edges east, west, up and down, and the
    slices inside. No smooth lighting: the slices are inside the cell, where vanilla's AO would
    shade them from the blocks around it."""
    shell = _cull(box(0, 0, 0, 16, 16, 16, "#edge"), *sc.FACES)
    shell["faces"]["north"]["texture"] = "#face"
    shell["faces"]["south"]["texture"] = "#face"
    elements = [shell] + [box(0, 0, z, 16, 16, z, "#web", faces=("north", "south"))
                          for z in SLICES]
    return {"parent": "block/block", "ambientocclusion": False,
            "textures": {"face": TEX_REF % name, "web": TEX_REF % (name + "_web"),
                         "edge": TEX_REF % ("breeze_block_edge_" + colour),
                         "particle": TEX_REF % ("breeze_block_edge_" + colour)},
            "elements": elements}


# The slats, as drawn against the north edge: x of each, 2 px wide, 2 px apart
SLATS = (1, 5, 9, 13)
SLAT_Z = (0.5, 3.5)
RAIL_Z = (0.0, 4.0)
RAIL_H = 1.0


def _slats():
    # No ends: a rail covers them at the top and the bottom of a run, and in a run the slat
    # carries on into the next block
    return [box(x, 0, SLAT_Z[0], x + 2, 16, SLAT_Z[1], "#wood",
                faces=("north", "south", "east", "west")) for x in SLATS]


def _rail(top):
    y0, y1 = (16 - RAIL_H, 16) if top else (0, RAIL_H)
    el = box(0, y0, RAIL_Z[0], 16, y1, RAIL_Z[1], "#wood")
    return [_cull(el, "north", "up" if top else "down")]


def _slat_model(name, elements):
    return {"parent": "block/block", "textures": {"wood": TEX_REF % name,
                                                  "particle": TEX_REF % name},
            "elements": elements}


def models():
    out = {}
    for pattern in PATTERNS:
        for colour in COLOURS:
            name = "breeze_block_%s_%s" % (pattern, colour)
            out[name] = breeze_model(name, colour)
    for name in slat_names():
        out[name] = _slat_model(name, _slats() + _rail(True) + _rail(False))
        out[name + "_slats"] = _slat_model(name, _slats())
        out[name + "_rail_top"] = _slat_model(name, _rail(True))
        out[name + "_rail_bottom"] = _slat_model(name, _rail(False))
    return out

# --------------------------------------------------------------------------------------------
# Blockstates
# --------------------------------------------------------------------------------------------

SIDES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def breeze_state(name):
    return {"variants": {"axis=z": {"model": MODEL_REF % name},
                         "axis=x": {"model": MODEL_REF % name, "y": 90},
                         "inventory": {"model": MODEL_REF % name}}}


def slat_state(name):
    parts = []
    for when, suffix in (({}, "_slats"), ({"up": "false"}, "_rail_top"),
                         ({"down": "false"}, "_rail_bottom")):
        for side, rot in SIDES:
            w = dict(when)
            w["facing"] = side
            a = {"model": MODEL_REF % (name + suffix)}
            if rot:
                a["y"] = rot
            parts.append({"when": w, "apply": a})
    return {"variants": {"inventory": {"model": MODEL_REF % name}}, "multipart": parts}


def blockstates():
    out = {name: breeze_state(name) for name in breeze_names()}
    out.update({name: slat_state(name) for name in slat_names()})
    return out

# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------


def write_all(tex_dir, model_dir, state_dir):
    written = []
    for d in (tex_dir, model_dir, state_dir):
        os.makedirs(d, exist_ok=True)
    for name, img in sorted(textures().items()):
        img.save(os.path.join(tex_dir, name + ".png"))
        written.append(("tex", name + ".png"))
    for name, body in sorted(models().items()):
        gen_cmu._write_json(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    for name, body in sorted(blockstates().items()):
        gen_cmu._write_json(os.path.join(state_dir, name + ".json"), body)
        written.append(("state", name + ".json"))
    model_depth.separate_dirs([(tex_dir, TEX_DIR), (model_dir, MODEL_DIR), (state_dir, STATE_DIR)],
                              written, gen_cmu._write_json)
    return written


def lang_entries():
    out = []
    for pattern, pnames in PATTERNS.items():
        for colour, (_, _, cnames) in COLOURS.items():
            out.append(("tile.breeze_block_%s_%s.name" % (pattern, colour),
                        {lang: "%s (%s, %s)" % (BREEZE_NAME[i], pnames[i], cnames[i])
                         for i, lang in enumerate(LANGS)}))
    for timber, (_, _, tnames) in TIMBERS.items():
        out.append(("tile.slat_screen_%s.name" % timber,
                    {lang: "%s (%s)" % (SLAT_NAME[i], tnames[i]) for i, lang in enumerate(LANGS)}))
    return out


def tab_lines():
    names = dict((k[len("tile."):-len(".name")], v["en_us"]) for k, v in lang_entries())
    lines = ['    initTabBlock(new BlockBreezeBlock("%s")); // %s' % (n, names[n])
             for n in breeze_names()]
    lines += ['    initTabBlock(new BlockSlatScreen("%s")); // %s' % (n, names[n])
              for n in slat_names()]
    return lines


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    lines.append("# tab registration, in CsmTabBuildingMaterials, after the stone veneer sets")
    lines += tab_lines()
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    ap.add_argument("--fragments", action="store_true",
                    help="print the lang and tab-registration lines and exit")
    args = ap.parse_args()
    if args.fragments:
        print(fragments())
        return 0
    roots = {"tex": TEX_DIR, "model": MODEL_DIR, "state": STATE_DIR}
    if args.check:
        return gen_logistics.check("screen block", write_all, roots)
    written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR)
    print("Wrote %d screen block files" % len(written))
    return 0


if __name__ == "__main__":
    sys.exit(main())
