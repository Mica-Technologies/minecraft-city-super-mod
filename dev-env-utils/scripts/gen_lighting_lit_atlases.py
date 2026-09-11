#!/usr/bin/env python3
"""
gen_lighting_lit_atlases.py -- make a switched-off lighting fixture actually LOOK off.

Every ``AbstractBrightLight`` has a four-value ``state`` (0 redstone-off, 1 redstone-on, 2 manual-
off, 3 manual-on) that really does drive ``getLightValue``. But 81 of the older fixtures declare all
four states as empty overrides and wear ONE atlas for the whole block, so an unlit cobra head is
pixel-identical to a lit one. You can see the light it casts on the ground; the fixture itself never
changes.

The decorative pendant/sconce family already does this properly, by swapping a ``#lens`` texture per
state (see ``assets/docs/LIGHTING_SYSTEM.md`` and ``cehalo.json``). This script retrofits the rest.

THE ONE RULE THIS SCRIPT EXISTS TO ENFORCE
------------------------------------------
**The atlases as they stand today are never modified.** Somebody spent real time drawing them, so
this writes ``<atlas>_off.png`` ALONGSIDE each original, and ``--check`` asserts that every
generated sheet is pixel-identical to its original everywhere outside the catalogued lens pixels.
That is a hard failure, not a warning: it is the difference between a promise and a habit.

WHICH STATE THE EXISTING ART IS
-------------------------------
The LIT one, and that is measured rather than assumed: on 24 of the 27 atlases the lamp is already
drawn in pure white, with no headroom to brighten at all. Only merc_fixtures and westinghouse_ov50
(olive refractor) and ge_hps_fixtures (amber) have any. The art was drawn lit, which is exactly why
an unlit fixture looks lit today.

So states 1 and 3 keep pointing at the original and **a lit fixture goes on looking precisely as it
looks now**. What this generates is the missing half: a dimmed sheet for states 0 and 2.

A wrong lens pick is cheap either way. The worst a mistake can do is dim something that should have
stayed bright, and the fix is one line in ``LENS`` plus a rerun.

WHY NO MODEL CHANGES
--------------------
All 81 are JSON element models wearing a single texture through three keys (``0``, ``all``,
``particle``). Because every face reads the same atlas, swapping that whole atlas per state already
reaches the lens -- nothing has to be re-materialled to separate a ``#lens`` out first.

WHY THE LENS IS CHOSEN BY EYE AND NOT DETECTED
----------------------------------------------
Each model carries explicit per-face UV rects, so the candidate regions are known exactly: 140
distinct rects across the 27 atlases, between 2 and 9 each. What is NOT knowable from the geometry
is which of them is the lamp, and the obvious heuristic is wrong in both directions:

    troffers          lens is the MOST used rect (1390 faces) -- a troffer's whole underside is
                      the diffuser panel
    ge_hps_fixtures   lens is the RAREST (3 faces of 348) -- the little painted amber lamp

Nothing separates those but looking, which is what ``--overlay`` is for: it draws each atlas at 8x
with every candidate rect outlined and labelled, turning 27 judgements into 27 glances.

HOW THE UNLIT REGION IS DIMMED
------------------------------
In HSV, with the **hue left alone** -- value down, saturation down. An unlit lamp is a duller,
greyer version of itself, not a different colour. The default lands a white diffuser on about
(178, 178, 179), within a couple of levels of the ``lens_opal_off`` the decorative family already
uses, so the two families read as the same mod.

Usage:
    python gen_lighting_lit_atlases.py --overlay     # contact sheets for classifying the lenses
    python gen_lighting_lit_atlases.py --report      # the rect table as text
    python gen_lighting_lit_atlases.py               # write the _off atlases (needs LENS filled in)
    python gen_lighting_lit_atlases.py --check       # verify the _off sheets against the originals

Requires Pillow.
"""
import argparse
import collections
import colorsys
import io
import json
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout                       # noqa: E402

GENERATOR = os.path.basename(__file__)

LIGHTING_OWNER = layout.owner_of_folder("lighting")
BLOCKSTATE_DIR = layout.asset_dir_for_write(LIGHTING_OWNER, "blockstates")
SCRATCH_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_lighting_out")

#: Where a `csm:` reference may resolve. A lighting asset lives in the lighting tree unless it is
#: shared, in which case it stays in Core -- see MODULE_SYSTEM.md.
ASSET_ROOTS = [
    os.path.join(layout.REPO_ROOT, "modules", "lighting", "src", "main", "resources", "assets",
                 "csm"),
    os.path.join(layout.REPO_ROOT, "src", "main", "resources", "assets", "csm"),
]

#: Suffix for the generated unlit sheet. The LIT one keeps its name: it is the file that already
#: exists, every fixture already points at it, and it is the state it was drawn in.
OFF_SUFFIX = "_off"


# ===================================================================================================
# THE LENS CATALOGUE
# ===================================================================================================
# One entry per atlas, naming the UV rects that are the LAMP. Rects are in the models' own 0-16 UV
# space, exactly as they appear in the model JSON, so an entry can be pasted straight from what
# --report prints and checked against the model by eye.
#
# Filled in by looking at `--overlay`. An atlas with no entry is refused by the generate path, so a
# half-finished catalogue cannot silently ship fixtures that still do not change.
#
# `boxes` are PIXEL rects on the 128x128 sheet, not model UV rects. The UV rects the models carry
# are what `--overlay` draws and they are how each box was found, but several of them cover a whole
# painted fixture face -- housing and lamp together -- so the catalogue has to be able to say
# "this part of that rect".
#
# `colors` narrows a box to an exact set of RGBs. The LED arrays all need it: their panel rect is
# a grey frame with the dies sitting in it, and brightening the frame would wash the fixture out.
# Every one of those panels draws its dies in pure white, which is what makes this a list of one.
WHITE_DIE = [(255, 255, 255)]

#: The LED arrays want a harder dim than a flat diffuser, and it is a question of CONTRAST rather
#: than of amount: a die taken from 255 to the default's 178 sits against a panel face that is
#: already 220, so the dots stay about the value of the metal around them and the fixture barely
#: changes. At ~133 the dies read clearly darker than their panel, which is what an unlit LED
#: luminaire looks like -- a grid of dark dots on a light grey optic.
LED_DIM = (0.52, 0.45)


LENS = {
    # --- flat sheets, where the lit surface is a whole region ---------------------------------
    "troffers": {
        "boxes": [(0, 0, 64, 64)], "colors": None,
        "note": "the white diffuser -- a troffer's entire visible underside is the lit panel",
    },
    "striplights": {
        "boxes": [(0, 0, 64, 64)], "colors": None,
        "note": "the white diffuser; the blue quadrant beside it is the painted housing",
    },
    "mcla_classic_post_light": {
        "boxes": [(0, 0, 64, 64)], "colors": None,
        "note": "the white lantern glass; the dark quadrant is the metalwork",
    },
    "mcla_park_light": {
        "boxes": [(0, 0, 64, 64)], "colors": None,
        "note": "the white lantern glass; the green quadrant is the painted body",
    },
    "merc_fixtures": {
        "boxes": [(0, 64, 64, 128)], "colors": None,
        "note": "the olive refractor bowl. Confirmed by rendering alto_mv_underpass_light: dark "
                "canopy on top, olive glass bowl beneath. Not guessable from the sheet alone.",
    },
    "westinghouse_ov50": {
        "boxes": [(69, 24, 81, 36)], "colors": None,
        "note": "the olive refractor swatch -- the same olive merc_fixtures uses for its bowl, "
                "which is the cross-check that settled both",
    },
    "ge_hps_fixtures": {
        "boxes": [(8, 7, 33, 29), (69, 24, 81, 36), (5, 81, 19, 103)], "colors": None,
        "note": "the amber HPS lamp: two painted lamp windows and the flat swatch the other "
                "fixtures wear. Boxes are the bounding boxes of the warm pixels, so the little "
                "reflector frame inside them lifts too -- which is what a lit lamp does to it.",
    },

    # --- LED arrays: the panel, restricted to the dies ------------------------------------------
    "ge_evolve_large": {"boxes": [(0, 0, 41, 30)], "colors": WHITE_DIE, "dim": LED_DIM,
                        "note": "the LED module's dies"},
    "ge_evolve_small": {"boxes": [(0, 0, 31, 30)], "colors": WHITE_DIE, "dim": LED_DIM,
                        "note": "the LED module's dies"},
    "ltgc1_v1": {"boxes": [(0, 0, 64, 64)], "colors": WHITE_DIE, "dim": LED_DIM,
                 "note": "the 8x8 LED array; the ribbed band below it is the heatsink"},
    "ltgc1_v2": {"boxes": [(0, 0, 64, 64)], "colors": WHITE_DIE, "dim": LED_DIM,
                 "note": "the LED array; the ribbed band below it is the heatsink"},
    "ltgcm_v2": {"boxes": [(0, 0, 64, 63)],
                 "colors": [(255, 255, 255), (253, 252, 253), (250, 250, 250)],
                 "dim": LED_DIM,
                     "note": "the two LED panels. This sheet shades its dies in three near-whites "
                         "rather than one, so all three are listed.",
    },
    "ltgcj": {"boxes": [(0, 0, 38, 47)], "colors": WHITE_DIE, "dim": LED_DIM, "note": "the LED array"},
    "ltgcj_smartnode": {"boxes": [(0, 0, 38, 47)], "colors": WHITE_DIE, "dim": LED_DIM,
                        "note": "the LED array; the blue swatch is the smart node, not a lamp"},
    "ltgcl": {"boxes": [(0, 0, 64, 64)], "colors": WHITE_DIE, "dim": LED_DIM, "note": "the two LED arrays"},
    "ltgcm": {"boxes": [(0, 0, 64, 64)], "colors": WHITE_DIE, "dim": LED_DIM, "note": "the LED array"},
    "ltgcm_smartnode": {"boxes": [(0, 0, 64, 64)], "colors": WHITE_DIE, "dim": LED_DIM,
                        "note": "the LED array; the blue swatch is the smart node, not a lamp"},
    "ae_autobahn_atb0": {"boxes": [(0, 0, 56, 30)], "colors": WHITE_DIE, "dim": LED_DIM,
                         "note": "the five LED modules"},
    "ae_autobahn_atb2": {"boxes": [(0, 0, 56, 53)], "colors": WHITE_DIE, "dim": LED_DIM,
                         "note": "the four LED modules"},
    "ci_navion": {"boxes": [(0, 0, 41, 31)], "colors": WHITE_DIE, "dim": LED_DIM, "note": "the LED array"},
    "ci_navion_alt": {"boxes": [(0, 0, 60, 31)], "colors": WHITE_DIE, "dim": LED_DIM, "note": "the LED array"},
    "cree_ledway": {"boxes": [(0, 0, 45, 31)], "colors": WHITE_DIE, "dim": LED_DIM, "note": "the LED array"},
    "cree_ledway_small": {"boxes": [(0, 0, 24, 31)], "colors": WHITE_DIE, "dim": LED_DIM, "note": "the LED array"},
    "cree_xsp": {"boxes": [(0, 0, 30, 31)], "colors": WHITE_DIE, "dim": LED_DIM, "note": "the LED array"},
    "ltec": {"boxes": [(0, 0, 56, 30)], "colors": WHITE_DIE, "dim": LED_DIM, "note": "the LED array"},
    "ltecdtd": {"boxes": [(0, 0, 26, 30)], "colors": WHITE_DIE, "dim": LED_DIM, "note": "the LED array"},
    "solarmax_smx": {"boxes": [(0, 0, 44, 64)],
                     "colors": [(255, 255, 255), (254, 253, 247)],
                     "dim": LED_DIM,
                     "note": "the LED dies and the lit band across the middle of the panel"},
}

#: How far to pull an unlit lens down, as (value multiplier, saturation multiplier). Applied in HSV
#: so the hue survives untouched. These put a white diffuser on about (178, 178, 179), a couple of
#: levels from the decorative family's own lens_opal_off (176, 178, 176).
DIM_DEFAULT = (0.70, 0.45)

#: Per-atlas overrides, for anything the shared default flatters badly. An unlit high-pressure
#: sodium lamp is a grey ceramic tube, not a pale peach one, so the amber wants taking down further
#: than a white diffuser does. A catalogue entry may also carry its own ``dim`` key, which wins.
DIM_OVERRIDES = {
    "ge_hps_fixtures": (0.55, 0.30),
}


# --- reading the tree ------------------------------------------------------------------------------
def resolve(kind, ref, ext):
    """Resolve a `csm:` asset reference to a path, looking in the lighting tree then Core."""
    rel = ref.split(":", 1)[1].replace("/", os.sep)
    for root in ASSET_ROOTS:
        path = os.path.join(root, kind, rel + ext)
        if os.path.exists(path):
            return path
    return None


def is_target(blockstate):
    """True if this fixture is one of the ones being retrofitted -- before OR after the retrofit.

    Two shapes count. A FLAT fixture wears one look for all four states, which is the thing being
    fixed. A fixture this script has already done puts the lit sheet back on states 1 and 3 and
    leaves 0 and 2 empty, and has to keep matching so the script can be run again -- a generator
    that refuses to regenerate its own output is a generator nobody can re-tune.

    Anything else is the decorative family, which already does this properly and is not ours.
    """
    state = blockstate.get("variants", {}).get("state")
    if not isinstance(state, dict) or len(state) != 4:
        return False
    if all(v == {} for v in state.values()):
        return True
    return (state.get("0") == {} and state.get("2") == {}
            and "textures" in state.get("1", {}) and "textures" in state.get("3", {}))


def base_texture(ref):
    """The LIT sheet's reference, whichever of the pair a blockstate happens to name right now."""
    return ref[:-len(OFF_SUFFIX)] if ref.endswith(OFF_SUFFIX) else ref


def scan():
    """Every flat fixture, grouped by the atlas it wears.

    Returns ``{texture_ref: {"fixtures": [...], "rects": Counter{(uv, face): n}}}``.
    """
    atlases = collections.defaultdict(
        lambda: {"fixtures": [], "rects": collections.Counter(), "keys": set()})
    for name in sorted(os.listdir(BLOCKSTATE_DIR)):
        if not name.endswith(".json"):
            continue
        path = os.path.join(BLOCKSTATE_DIR, name)
        with io.open(path, encoding="utf-8") as fh:
            data = json.load(fh)
        if not is_target(data):
            continue
        defaults = data.get("defaults", {})
        textures = defaults.get("textures", {})
        model_ref = defaults.get("model", "")
        model_path = resolve("models/block", model_ref, ".json")
        if model_path is None:
            raise SystemExit("%s: cannot resolve model %s" % (name, model_ref))
        with io.open(model_path, encoding="utf-8") as fh:
            model = json.load(fh)

        refs = {base_texture(value) for value in textures.values()}
        if len(refs) != 1:
            raise SystemExit("%s: expected one texture, found %d" % (name, len(refs)))
        ref = refs.pop()
        entry = atlases[ref]
        entry["fixtures"].append(name[:-5])
        entry["keys"].update(textures.keys())
        for element in model.get("elements", []):
            for face, data_face in element.get("faces", {}).items():
                uv = data_face.get("uv")
                if uv:
                    entry["rects"][(tuple(round(v, 3) for v in uv), face)] += 1
    return atlases


def rect_summary(entry):
    """The atlas's distinct UV rects, most-used first, as (uv, faces, [directions])."""
    merged = collections.Counter()
    faces = collections.defaultdict(set)
    for (uv, face), count in entry["rects"].items():
        merged[uv] += count
        faces[uv].add(face)
    return [(uv, count, sorted(faces[uv])) for uv, count in merged.most_common()]


def to_pixels(uv, size):
    """A model UV rect (0-16, y from the top) as a pixel box, normalised so x0<x1 and y0<y1.

    Some faces carry a FLIPPED rect -- ``(8.625, 4.5, 10.125, 3)`` appears in ge_hps_fixtures --
    which is how a model mirrors a face. As a region of the sheet it means the same pixels, so the
    normalisation here is not a fudge: it is the only reading that makes sense.
    """
    scale = size / 16.0
    x0, y0, x1, y1 = [v * scale for v in uv]
    return (int(round(min(x0, x1))), int(round(min(y0, y1))),
            int(round(max(x0, x1))), int(round(max(y0, y1))))


# --- overlay ---------------------------------------------------------------------------------------
def overlay_sheet(ref, entry, scale=8):
    """One atlas drawn large with every candidate rect outlined and labelled by face count."""
    path = resolve("textures", ref, ".png")
    image = Image.open(path).convert("RGBA")
    size = image.size[0]
    # Transparent pixels composite onto magenta, so a rect over empty sheet is obvious rather than
    # reading as black geometry.
    ground = Image.new("RGBA", image.size, (255, 0, 255, 255))
    ground.alpha_composite(image)
    big = ground.convert("RGB").resize((size * scale, size * scale), Image.NEAREST)

    draw = ImageDraw.Draw(big)
    summary = rect_summary(entry)
    busiest = summary[0][1] if summary else 1
    for index, (uv, count, faces) in enumerate(summary):
        x0, y0, x1, y1 = [v * scale for v in to_pixels(uv, size)]
        colour = ((255, 70, 70) if index == 0
                  else (90, 255, 140) if count <= max(2, busiest // 20)
                  else (110, 180, 255))
        draw.rectangle([x0, y0, x1 - 1, y1 - 1], outline=colour, width=3)
        draw.text((x0 + 5, y0 + 3), "%d %s" % (count, ",".join(faces)[:28]), fill=colour)
    return big


def write_overlays(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    atlases = scan()
    written = []
    for ref, entry in sorted(atlases.items(), key=lambda kv: -len(kv[1]["fixtures"])):
        name = ref.split("/")[-1]
        path = os.path.join(out_dir, name + ".png")
        overlay_sheet(ref, entry).save(path)
        written.append(path)
    return written, atlases


def report(atlases):
    lines = []
    for ref, entry in sorted(atlases.items(), key=lambda kv: -len(kv[1]["fixtures"])):
        name = ref.split("/")[-1]
        lines.append("%s  (%d fixtures, keys %s)%s"
                     % (name, len(entry["fixtures"]), ",".join(sorted(entry["keys"])),
                        "" if name in LENS else "   [NO LENS ENTRY]"))
        for uv, count, faces in rect_summary(entry):
            lines.append("    %-32s faces=%-5d %s" % (str(uv), count, ",".join(faces)))
        if name in LENS:
            spec = LENS[name]
            lines.append("    LENS %s%s -- %s"
                         % (spec["boxes"],
                            "" if not spec.get("colors") else " colors=%s" % (spec["colors"],),
                            spec["note"]))
        lines.append("")
    return "\n".join(lines)


# --- generating the lit sheets ---------------------------------------------------------------------
def dim(pixel, value_mul, sat_mul):
    """Pull a pixel down in brightness and saturation, leaving its hue exactly where it was."""
    r, g, b, a = pixel
    if a == 0:
        return pixel
    h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
    v = min(1.0, v * value_mul)
    s = min(1.0, s * sat_mul)
    r, g, b = colorsys.hsv_to_rgb(h, s, v)
    return (int(round(r * 255)), int(round(g * 255)), int(round(b * 255)), a)


def lens_pixels(spec, image):
    """Every pixel the catalogue says is lamp: inside a box, and of a listed colour if any."""
    colors = set(spec["colors"]) if spec.get("colors") else None
    width, height = image.size
    found = set()
    for (x0, y0, x1, y1) in spec["boxes"]:
        for y in range(max(0, y0), min(height, y1)):
            for x in range(max(0, x0), min(width, x1)):
                pixel = image.getpixel((x, y))
                if pixel[3] == 0:
                    continue
                if colors is not None and pixel[:3] not in colors:
                    continue
                found.add((x, y))
    return found


def unlit_atlas(ref, spec):
    """The original with only the catalogued lamp pixels pulled down."""
    image = Image.open(resolve("textures", ref, ".png")).convert("RGBA")
    out = image.copy()
    value_mul, sat_mul = spec.get("dim") or DIM_OVERRIDES.get(ref.split("/")[-1],
                                                            DIM_DEFAULT)
    for (x, y) in lens_pixels(spec, image):
        out.putpixel((x, y), dim(image.getpixel((x, y)), value_mul, sat_mul))
    return out


def outside_lens_identical(ref, spec, other_path):
    """True if the lit sheet matches its original everywhere outside the catalogued lamp pixels.

    The whole promise of this retrofit in one assertion: the artwork as it stands IS the off state,
    and the only pixels allowed to differ are the ones the catalogue calls the lamp.
    """
    original = Image.open(resolve("textures", ref, ".png")).convert("RGBA")
    other = Image.open(other_path).convert("RGBA")
    if original.size != other.size:
        return False
    allowed = lens_pixels(spec, original)
    width, height = original.size
    for y in range(height):
        for x in range(width):
            if (x, y) in allowed:
                continue
            if original.getpixel((x, y)) != other.getpixel((x, y)):
                return False
    return True


def retrofit_blockstate(data, ref):
    """Point the fixture's four states at the right sheet. Returns True if anything changed.

    Shaped after the decorative family's ``cehalo.json``, which is the house style for this.

    The DEFAULT is the unlit sheet, which is what states 0 and 2 then inherit by staying empty --
    and it is also what the creative-menu icon and a freshly placed fixture read, both of which are
    right: a block placed in ``STATE_RS_OFF`` is off.

    ``particle`` is deliberately left on the unlit sheet. It is only the break/step particle colour,
    and a lit fixture has no business showering bright particles when it is broken.
    """
    lit = ref
    unlit = ref + OFF_SUFFIX
    defaults = data["defaults"]
    before = json.dumps(data, sort_keys=True)

    defaults["textures"] = {key: unlit for key in defaults["textures"]}
    # Only the keys the MODEL reads go back to the lit sheet; particle stays unlit.
    relit = {"textures": {key: lit for key in defaults["textures"] if key != "particle"}}

    state = data["variants"]["state"]
    state["0"] = {}
    state["1"] = json.loads(json.dumps(relit))
    state["2"] = {}
    state["3"] = json.loads(json.dumps(relit))
    return json.dumps(data, sort_keys=True) != before


def write_blockstates():
    """Rewrite every targeted fixture's blockstate. Returns the paths that actually changed."""
    changed = []
    for name in sorted(os.listdir(BLOCKSTATE_DIR)):
        if not name.endswith(".json"):
            continue
        path = os.path.join(BLOCKSTATE_DIR, name)
        with io.open(path, encoding="utf-8") as fh:
            data = json.load(fh)
        if not is_target(data):
            continue
        refs = {base_texture(v) for v in data.get("defaults", {}).get("textures", {}).values()}
        if len(refs) != 1:
            raise SystemExit("%s: expected one texture, found %d" % (name, len(refs)))
        if retrofit_blockstate(data, refs.pop()):
            with io.open(path, "w", encoding="utf-8", newline="\n") as fh:
                json.dump(data, fh, indent=2)
                fh.write("\n")
            changed.append(path)
    return changed


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--overlay", action="store_true",
                        help="write one classification contact sheet per atlas and stop")
    parser.add_argument("--report", action="store_true",
                        help="print the atlas/rect table and stop")
    parser.add_argument("--check", action="store_true",
                        help="verify each _off sheet against its original and stop")
    args = parser.parse_args()

    if args.overlay:
        out = os.path.join(SCRATCH_DIR, "overlay")
        written, atlases = write_overlays(out)
        print(report(atlases))
        print("%d overlays in %s" % (len(written), os.path.relpath(out, layout.REPO_ROOT)))
        return 0

    atlases = scan()
    if args.report:
        print(report(atlases))
        return 0

    missing = [ref.split("/")[-1] for ref in atlases if ref.split("/")[-1] not in LENS]
    if missing:
        raise SystemExit("LENS has no entry for: %s\nRun --overlay and classify them first."
                         % ", ".join(sorted(missing)))

    written = []
    for ref, entry in sorted(atlases.items()):
        name = ref.split("/")[-1]
        spec = LENS[name]
        target = resolve("textures", ref, ".png").replace(".png", OFF_SUFFIX + ".png")
        if args.check:
            if not os.path.exists(target):
                raise SystemExit("%s: %s has not been generated" % (name, OFF_SUFFIX))
            if not outside_lens_identical(ref, spec, target):
                raise SystemExit("%s: the unlit sheet differs from its original OUTSIDE the "
                                 "lens pixels. The artwork must not change." % name)
            print("%-34s ok" % name)
            continue
        unlit_atlas(ref, spec).save(target)
        written.append(target)

    if args.check:
        return 0

    for path in written:
        print(os.path.relpath(path, layout.REPO_ROOT))
    for path in write_blockstates():
        print(os.path.relpath(path, layout.REPO_ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
