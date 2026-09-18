#!/usr/bin/env python3
"""Every asset the frame scaffold ships: textures, part models and the multipart blockstate.

    python dev-env-utils/scripts/gen_scaffold.py
    python dev-env-utils/scripts/gen_scaffold.py --check
    python dev-env-utils/scripts/gen_scaffold.py --fragments   # lang and tab lines to paste

ONE BLOCK THAT DRAWS ITSELF FROM ITS NEIGHBOURS. A real frame scaffold is end frames, cross braces
and planks, but a Minecraft cell holds one block, so each cell is one bay and decides its own
parts: an end frame on its near side always and on its far side only where the run ends, a cross
brace on each open long face, a plank deck on top only when nothing is stacked on it, and screw
jacks under the legs only where it stands on something else. Stacked, a tower reads as frames and
braces with a working deck at the top, which is what a real one looks like. The top deck also
grows a guardrail on every edge that is a drop; which edges those are is decided in Java
(BlockScaffoldFrame.needsRail), and only drawn here.

WHAT IS STORED. Only the facing, which says which way the frames run. Every part is decided in
getActualState from the neighbours, so the look can change completely -- in this file -- without
invalidating a single placed block. That was a requirement: the first scaffold was built to be
looked at, and it had to stay cheap to change its mind about.

CANONICAL ORIENTATION. Every part is drawn for FACING north: the frames stand on the west and east
faces (the run goes east-west, along the wall) and the braces on the north and south faces. The
blockstate turns the lot a quarter for east and west, and names each part's condition in absolute
directions for each of the two turns.
"""

import argparse
import filecmp
import json
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

import gen_cmu
import gen_framing

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODULE = os.path.join(REPO, "modules", "building", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "constructionsite")
MODEL_DIR = os.path.join(MODULE, "models", "block", "constructionsite")
STATE_DIR = os.path.join(MODULE, "blockstates")

TEX_REF = "csm:blocks/constructionsite/%s"
MODEL_REF = "csm:constructionsite/%s"

NAME = "scaffold_frame"
LANG = ("Frame Scaffold", "Andamio de Marco", "Rahmengerüst", "Ramställning")
TAB_LANG = ("CSM: Construction Site", "CSM: Obra en Construcción", "CSM: Baustelle",
            "CSM: Byggarbetsplats")

# --------------------------------------------------------------------------------------------
# Geometry catalogue -- every dimension the look depends on, in 1/16 of a block
# --------------------------------------------------------------------------------------------

TUBE = 1.0            # frame tube section
LEG_Z = (1.0, 14.0)   # the two legs of an end frame, their near z
LEDGER_Y = 14.0       # the frame's top ledger, which the planks bear on
ARCH_Y = 10.5         # the walk-through frame's lower crossbar
BRACE_T = 0.4         # brace thickness (flat bar)
BRACE_W = 0.6         # brace depth
BRACE_Z = (0.2, 0.7)  # the two braces of an X, one in front of the other so they never share
                      # a plane where they cross
DECK_Y = 15.0         # planks from here to the top of the cell
PLANKS = ((2.25, 5.75), (6.25, 9.75), (10.25, 13.75))  # plank spans across the depth
JACK_PLATE = 0.5      # base plate thickness
# The guardrail, drawn into the cell above the deck (y 16..32), on the leg lines.
RAIL_POST_TOP = 32.0  # posts run up from the legs to here
RAIL_TOP_Y = 30.0     # top rail, one tube section from here
RAIL_MID_Y = 23.0     # mid rail
RAIL_INSET = 0.1      # rails are this much thinner than the posts each side, so a rail passing
                      # through a post never shares a face with it
TOEBOARD = (16.0, 18.0, 0.5)  # toeboard bottom, top and thickness, just inside the leg line
JACK_COLLAR = (2.0, 2.75)

# A 45 degree diagonal from corner to corner of a 16 x 16 face is 22.63 long. Rotation cannot
# rescale without also rescaling the section, so the brace is drawn that long and turned without
# rescale -- the rafter's trick in gen_framing.
DIAG_HALF = 11.32

# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

SIZE = 16


def _shift(colour, d):
    return tuple(max(0, min(255, int(round(c + d)))) for c in colour[:3]) + (255,)


def tube_texture():
    """Galvanised tube: the framing family's steel, banded lengthwise so it reads round."""
    rng = random.Random(20261040)
    base = gen_framing.STEEL_BASE
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    band = [8, 4, 0, -6, -10, -4, 2, 6] * 2
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = _shift(base, band[x] + rng.uniform(-3, 3))
    return img


def plank_texture():
    """A scaffold plank: rough-sawn softwood, grain along the plank (along x)."""
    rng = random.Random(20261041)
    base = (176, 142, 96)
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    row = [rng.uniform(-8, 8) for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = _shift(base, row[y] + rng.uniform(-4, 4))
    return img


def jack_texture():
    """Screw jack and base plate: darker, oiled steel."""
    rng = random.Random(20261042)
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = _shift((96, 98, 100), (6 if y % 2 else -6) + rng.uniform(-3, 3))
    return img


TEXTURES = {"scaffold_tube": tube_texture, "scaffold_plank": plank_texture,
            "scaffold_jack": jack_texture}

# --------------------------------------------------------------------------------------------
# Models
# --------------------------------------------------------------------------------------------

FACES = ("north", "south", "east", "west", "up", "down")


def _fit(a, b):
    """One UV span, fitted into the texture without collapsing it.

    A span that lies wholly in a neighbouring cell -- a guardrail, drawn at y 16..32 in the cell
    above the deck -- is shifted by whole blocks, as if it were drawn in that cell, so it gets its
    proper window of the texture. Only a span that genuinely straddles the texture's edge is
    clamped. Clamping a whole-cell-away span instead collapses it onto the sprite's border row, and
    at a distance the mipmap blends that row with the neighbouring sprites on the atlas: the first
    guardrail's posts, rails and toeboards came out as grey and tan speckle.
    """
    lo, hi = min(a, b), max(a, b)
    k = (lo // 16) * 16
    if hi - k <= 16:
        lo, hi = lo - k, hi - k
    else:
        lo, hi = max(0.0, min(16.0, lo)), max(0.0, min(16.0, hi))
    lo, hi = round(lo, 4), round(hi, 4)
    return (lo, hi) if a <= b else (hi, lo)


def _position_uv(face, f, t):
    """The UV Minecraft would derive from the element's position, fitted into the texture.

    Unfitted, a face that reaches past the cell gets UVs outside 0..16, and outside 0..16 the atlas
    does not wrap: it samples whatever sprites sit next to this one. The first scaffold's braces
    did exactly that -- only the stretch of each brace that fell on the tube texture was drawn as
    steel, so the braces looked short and floating, and the rest was painted from other blocks.
    """
    u0, v0, u1, v1 = {"down": (f[0], 16 - t[2], t[0], 16 - f[2]),
                      "up": (f[0], f[2], t[0], t[2]),
                      "north": (16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]),
                      "south": (f[0], 16 - t[1], t[0], 16 - f[1]),
                      "west": (f[2], 16 - t[1], t[2], 16 - f[1]),
                      "east": (16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1])}[face]
    u0, u1 = _fit(u0, u1)
    v0, v1 = _fit(v0, v1)
    return [u0, v0, u1, v1]


# A brace is one flat bar, so every face takes the same thin strip of the tube texture rather than
# a position-derived window: the bar is longer than the cell, and no window of it fits the texture.
BRACE_UV = [2, 0, 3, 16]


def _box(x0, y0, z0, x1, y1, z1, tex, rotation=None, uv=None):
    f, t = (x0, y0, z0), (x1, y1, z1)
    box = {"from": [x0, y0, z0], "to": [x1, y1, z1],
           "faces": {face: {"texture": tex,
                            "uv": list(uv) if uv else _position_uv(face, f, t)}
                     for face in FACES}}
    if rotation is not None:
        box["rotation"] = rotation
    return box


def _reuv(m):
    """Re-derive a mirrored element's UVs from its new position; a brace keeps its strip."""
    f, t = m["from"], m["to"]
    for face, spec in m["faces"].items():
        if spec.get("uv") != BRACE_UV:
            spec["uv"] = _position_uv(face, f, t)
    return m


def _mirror_x(elements):
    """The same part on the far (east) face: x reflected about the middle of the cell."""
    out = []
    for e in elements:
        m = json.loads(json.dumps(e))
        m["from"][0], m["to"][0] = 16 - e["to"][0], 16 - e["from"][0]
        if "rotation" in m:
            m["rotation"]["origin"][0] = 16 - m["rotation"]["origin"][0]
            if m["rotation"]["axis"] != "x":
                m["rotation"]["angle"] = -m["rotation"]["angle"]
        out.append(_reuv(m))
    return out


def _mirror_z(elements):
    """The same part on the far (south) face."""
    out = []
    for e in elements:
        m = json.loads(json.dumps(e))
        m["from"][2], m["to"][2] = 16 - e["to"][2], 16 - e["from"][2]
        if "rotation" in m:
            m["rotation"]["origin"][2] = 16 - m["rotation"]["origin"][2]
            if m["rotation"]["axis"] != "z":
                m["rotation"]["angle"] = -m["rotation"]["angle"]
        out.append(_reuv(m))
    return out


def frame_near():
    """An end frame on the west face: two legs the full height of the cell, so stacked frames meet
    end to end, a top ledger the planks bear on, and the walk-through frame's lower crossbar."""
    t = "#tube"
    z0, z1 = LEG_Z
    return [
        _box(0, 0, z0, TUBE, 16, z0 + TUBE, t),
        _box(0, 0, z1, TUBE, 16, z1 + TUBE, t),
        _box(0, LEDGER_Y, z0 + TUBE, TUBE, LEDGER_Y + TUBE, z1, t),
        _box(0, ARCH_Y, z0 + TUBE, TUBE, ARCH_Y + TUBE, z1, t),
    ]


def brace_north():
    """An X of cross braces on the north face, pinned to the frame legs either side of the bay.
    Each diagonal is drawn one block's diagonal long and turned 45 degrees about the face normal."""
    t = "#tube"
    c = 8.0
    (za0, za1), (zb0, zb1) = (BRACE_Z[0], BRACE_Z[0] + BRACE_T), (BRACE_Z[1], BRACE_Z[1] + BRACE_T)
    return [
        _box(c - DIAG_HALF, c - BRACE_W / 2, za0, c + DIAG_HALF, c + BRACE_W / 2, za1, t,
             rotation={"origin": [c, c, (za0 + za1) / 2], "axis": "z", "angle": 45},
             uv=BRACE_UV),
        _box(c - DIAG_HALF, c - BRACE_W / 2, zb0, c + DIAG_HALF, c + BRACE_W / 2, zb1, t,
             rotation={"origin": [c, c, (zb0 + zb1) / 2], "axis": "z", "angle": -45},
             uv=BRACE_UV),
    ]


def deck():
    """Three planks across the depth, running the length of the bay so a run of decks is one
    walkway. Kept inside the legs so no plank shares its top face with a leg's."""
    return [_box(0, DECK_Y, a, 16, 16, b, "#plank") for a, b in PLANKS]


def jack_near():
    """Screw jacks under the near frame's legs: a base plate and the jack's collar."""
    t = "#jack"
    out = []
    for z0 in LEG_Z:
        out.append(_box(0, 0, z0 - 0.5, 2, JACK_PLATE, z0 + 1.5, t))
        out.append(_box(-0.25, JACK_COLLAR[0], z0 - 0.25, TUBE + 0.25, JACK_COLLAR[1],
                        z0 + TUBE + 0.25, t))
    return out


def _rail_run_x(z0):
    """Top rail, mid rail and toeboard running the length of the bay (along x) on the leg line
    that starts at ``z0``, and the post over the near leg. The far post is its own part, drawn
    only where the run ends."""
    t, p = "#tube", "#plank"
    a, b = z0 + RAIL_INSET, z0 + TUBE - RAIL_INSET
    inner = z0 + TUBE if z0 < 8 else z0 - TOEBOARD[2]
    return [
        _box(0, 16, z0, TUBE, RAIL_POST_TOP, z0 + TUBE, t),
        _box(0, RAIL_TOP_Y + RAIL_INSET, a, 16, RAIL_TOP_Y + TUBE - RAIL_INSET, b, t),
        _box(0, RAIL_MID_Y + RAIL_INSET, a, 16, RAIL_MID_Y + TUBE - RAIL_INSET, b, t),
        _box(0, TOEBOARD[0], inner, 16, TOEBOARD[1], inner + TOEBOARD[2], p),
    ]


def rail_north():
    """The guardrail along the north (long) edge, over the north legs."""
    return _rail_run_x(LEG_Z[0])


def rail_north_post():
    """The post over the far north leg, drawn where the run ends to the east."""
    z0 = LEG_Z[0]
    return [_box(16 - TUBE, 16, z0, 16, RAIL_POST_TOP, z0 + TUBE, "#tube")]


def rail_west():
    """The guardrail across the near end frame: posts over both legs, rails between them."""
    t, p = "#tube", "#plank"
    z0, z1 = LEG_Z
    a, b = RAIL_INSET, TUBE - RAIL_INSET
    return [
        _box(0, 16, z0, TUBE, RAIL_POST_TOP, z0 + TUBE, t),
        _box(0, 16, z1, TUBE, RAIL_POST_TOP, z1 + TUBE, t),
        _box(a, RAIL_TOP_Y + RAIL_INSET, z0 + TUBE, b, RAIL_TOP_Y + TUBE - RAIL_INSET, z1, t),
        _box(a, RAIL_MID_Y + RAIL_INSET, z0 + TUBE, b, RAIL_MID_Y + TUBE - RAIL_INSET, z1, t),
        _box(TUBE, TOEBOARD[0], z0 + TUBE, TUBE + TOEBOARD[2], TOEBOARD[1], z1, p),
    ]


TEXTURE_KEYS = {"tube": TEX_REF % "scaffold_tube", "plank": TEX_REF % "scaffold_plank",
                "jack": TEX_REF % "scaffold_jack", "particle": TEX_REF % "scaffold_tube"}


def _model(elements, parent=None):
    m = {"textures": dict(TEXTURE_KEYS), "elements": elements}
    if parent:
        m = {"parent": parent, **m}
    return m


def part_models():
    near, north, jn = frame_near(), brace_north(), jack_near()
    return {
        NAME + "_frame_near": _model(near),
        NAME + "_frame_far": _model(_mirror_x(near)),
        NAME + "_brace_north": _model(north),
        NAME + "_brace_south": _model(_mirror_z(north)),
        NAME + "_deck": _model(deck()),
        NAME + "_jack_near": _model(jn),
        NAME + "_jack_far": _model(_mirror_x(jn)),
        NAME + "_rail_north": _model(rail_north()),
        NAME + "_rail_south": _model(_mirror_z(rail_north())),
        NAME + "_rail_north_post": _model(rail_north_post()),
        NAME + "_rail_south_post": _model(_mirror_z(rail_north_post())),
        NAME + "_rail_west": _model(rail_west()),
        NAME + "_rail_east": _model(_mirror_x(rail_west())),
        NAME + "_inventory": _model(near + _mirror_x(near) + north + _mirror_z(north) + deck()
                                    + jn + _mirror_x(jn), parent="block/block"),
    }


# --------------------------------------------------------------------------------------------
# Blockstate
# --------------------------------------------------------------------------------------------
#
# The two turns, and for each the absolute side each canonical face lands on. A y rotation of 90
# carries north to east, east to south, south to west and west to north.
TURNS = (("north|south", None,
          {"east": "east", "north": "north", "south": "south", "west": "west"}),
         ("east|west", 90,
          {"east": "south", "north": "east", "south": "west", "west": "north"}))


def _apply(part, y):
    a = {"model": MODEL_REF % (NAME + "_" + part)}
    if y is not None:
        a["y"] = y
    return a


def blockstate():
    parts = []
    for facing, y, side in TURNS:
        f = {"facing": facing}
        parts += [
            {"when": dict(f), "apply": _apply("frame_near", y)},
            {"when": dict(f, **{side["east"]: "false"}), "apply": _apply("frame_far", y)},
            {"when": dict(f, **{side["north"]: "false"}), "apply": _apply("brace_north", y)},
            {"when": dict(f, **{side["south"]: "false"}), "apply": _apply("brace_south", y)},
            {"when": dict(f, up="false"), "apply": _apply("deck", y)},
            {"when": dict(f, down="false"), "apply": _apply("jack_near", y)},
            {"when": dict(f, down="false", **{side["east"]: "false"}),
             "apply": _apply("jack_far", y)},
        ]
        # Guardrails. Which edges get one is decided in Java (the rail_* properties); only the
        # far posts need the run's end as well, since a post stands over every bay line and the
        # next bay draws its own near one.
        for canon in ("north", "south", "west", "east"):
            parts.append({"when": dict(f, **{"rail_" + side[canon]: "true"}),
                          "apply": _apply("rail_" + canon, y)})
        for canon in ("north", "south"):
            parts.append({"when": dict(f, **{"rail_" + side[canon]: "true",
                                              side["east"]: "false"}),
                          "apply": _apply("rail_%s_post" % canon, y)})
    return {"variants": {"inventory": {"model": MODEL_REF % (NAME + "_inventory")}},
            "multipart": parts}


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    for name, fn in sorted(TEXTURES.items()):
        fn().save(os.path.join(tex_dir, name + ".png"))
        written.append(("tex", name + ".png"))
    for name, body in sorted(part_models().items()):
        gen_cmu._write_json(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    gen_cmu._write_json(os.path.join(state_dir, NAME + ".json"), blockstate())
    written.append(("state", NAME + ".json"))
    return written


LANGS = gen_cmu.LANGS


def lang_entries():
    return [("itemGroup.tabconstructionsite", dict(zip(LANGS, TAB_LANG))),
            ("tile.%s.name" % NAME, dict(zip(LANGS, LANG)))]


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in LANGS:
        lines.append("## " + lang)
        for key, names in lang_entries():
            lines.append("%s=%s" % (key, names[lang]))
        lines.append("")
    lines.append("# tab registration, in CsmTabConstructionSite")
    lines.append("    initTabBlock(BlockScaffoldFrame.class, fmlPreInitializationEvent); // %s"
                 % LANG[0])
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

    if not args.check:
        written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR)
        print("Wrote %d scaffold files" % len(written))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_scaffold_")
    try:
        tmp_roots = {k: os.path.join(tmp, k) for k in roots}
        for path in tmp_roots.values():
            os.makedirs(path, exist_ok=True)
        written = write_all(tmp_roots["tex"], tmp_roots["model"], tmp_roots["state"])
        drifted = []
        for kind, filename in written:
            here = os.path.join(roots[kind], filename)
            there = os.path.join(tmp_roots[kind], filename)
            if not os.path.exists(here) or not filecmp.cmp(here, there, shallow=False):
                drifted.append(os.path.relpath(here, REPO))
        if drifted:
            print("DRIFT: %d file(s) differ from the generator:" % len(drifted))
            for path in drifted:
                print("  " + path)
            return 1
        print("%d generated scaffold files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
