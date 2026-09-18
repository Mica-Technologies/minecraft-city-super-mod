#!/usr/bin/env python3
"""Every asset the structural framing family ships.

One catalogue drives the textures, the shared geometry, each block's textured models and its
blockstate, so a registry name cannot drift from the thing it draws. Run with no arguments to
write; ``--check`` writes into a temporary tree and fails on any difference, which is the guard to
run before committing anything under the framing asset paths.

    python dev-env-utils/scripts/gen_framing.py
    python dev-env-utils/scripts/gen_framing.py --check
    python dev-env-utils/scripts/gen_framing.py --fragments   # lang and tab lines to paste

GEOMETRY. A wall is drawn post-and-arm, like a fence: an arm reaching from the block centre toward
each side that connects, and a post at the centre only where a wall really has one. Along an arm
reaching NORTH the block divides as

    z:  0 --- 3 == 5 --- 6 | 6 ..... 10 | 10 --- 11 == 13 --- 16
        track   stud  track      hub       track    stud   track

so two opposite arms plus the centre tile the block exactly, overlapping nothing. Nothing shares a
plane with anything facing the same way, which is what keeps a wall seen from above from
z-fighting. Studs run the FULL height of the block so that stacked ones meet end to end, and the
track is separate geometry so it can be left out where one course meets the next.

The shared geometry carries no texture of its own -- it names ``#stud`` and ``#track`` and each
block's own model binds those. That is what will let the wood framing reuse all of it.
"""

import argparse
import filecmp
import io
import json
import os
import random
import shutil
import sys
import tempfile

from PIL import Image, ImageDraw

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODULE = os.path.join(REPO, "modules", "building", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "buildingmaterials", "framing")
MODEL_DIR = os.path.join(MODULE, "models", "block", "buildingmaterials")
SHARED_DIR = os.path.join(MODEL_DIR, "shared_models", "framing")
STATE_DIR = os.path.join(MODULE, "blockstates")

TEX_REF = "csm:blocks/buildingmaterials/framing/%s"
SHARED_REF = "csm:block/buildingmaterials/shared_models/framing/%s"
MODEL_REF = "csm:buildingmaterials/%s"

# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

SIZE = 16

# Galvanised steel: a cool grey with the faint uneven mottle of a spangled zinc coat. Kept narrow
# in range -- real galvanising is subtle, and a wide range reads as damage rather than finish.
STEEL_BASE = (163, 168, 172, 255)
STEEL_LIGHT = (188, 193, 197, 255)
STEEL_DARK = (137, 142, 147, 255)
STEEL_EDGE = (118, 123, 128, 255)

# The knockout: the punched service hole every steel stud web carries, for conduit and pipe.
#
# Sized and placed to land exactly on the four texture pixels the stud model samples. The web is
# 4/16 of a block wide, and the model gives it the default position-derived UV, so it shows
# texture columns 6-9 and nothing else. A knockout drawn wider than that is cropped by the model
# and reads as a slot running off the edge of the stud.
KNOCKOUT_X0, KNOCKOUT_X1 = 7, 8
KNOCKOUT_Y0, KNOCKOUT_Y1 = 5, 10
KNOCKOUT_RIM = (206, 210, 214, 255)


def _mottle(seed):
    """A deterministic spangle. Seeded so two runs produce byte-identical files."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (SIZE, SIZE), STEEL_BASE)
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            roll = rng.random()
            if roll < 0.12:
                px[x, y] = STEEL_LIGHT
            elif roll < 0.24:
                px[x, y] = STEEL_DARK
    return img


def steel_stud():
    """The stud web, seen face on: mottled steel, a punched knockout, a rolled edge each side."""
    img = _mottle(seed=20260917)
    draw = ImageDraw.Draw(img)

    # The rolled edges of the C, which catch the light along the stud's length.
    draw.line([(0, 0), (0, SIZE - 1)], fill=STEEL_EDGE)
    draw.line([(SIZE - 1, 0), (SIZE - 1, SIZE - 1)], fill=STEEL_EDGE)
    draw.line([(1, 0), (1, SIZE - 1)], fill=STEEL_LIGHT)
    draw.line([(SIZE - 2, 0), (SIZE - 2, SIZE - 1)], fill=STEEL_LIGHT)

    # The knockout: a slot punched clean through, with the turned lip the punch leaves round it.
    # The lip is drawn first and the hole cut out of it, so the lip survives as a one-pixel rim on
    # all four sides -- which is what keeps a 2px hole reading as a hole and not as dirt.
    draw.rectangle([KNOCKOUT_X0 - 1, KNOCKOUT_Y0 - 1, KNOCKOUT_X1 + 1, KNOCKOUT_Y1 + 1],
                   fill=KNOCKOUT_RIM)
    draw.rectangle([KNOCKOUT_X0, KNOCKOUT_Y0, KNOCKOUT_X1, KNOCKOUT_Y1], fill=(0, 0, 0, 0))
    return img


def steel_track():
    """The track: the same steel, no knockout. Track is punched only where a service crosses it."""
    img = _mottle(seed=20260918)
    draw = ImageDraw.Draw(img)
    # Track is a plain U, so it shows its two legs rather than a web's rolled edges.
    draw.line([(0, 0), (SIZE - 1, 0)], fill=STEEL_LIGHT)
    draw.line([(0, SIZE - 1), (SIZE - 1, SIZE - 1)], fill=STEEL_EDGE)
    return img


# Insulation, seen edge on through an open bay: a fibrous mat, so the noise runs in horizontal
# streaks rather than as even speckle. Kept dull -- it sits in shadow between the studs, and a
# saturated pink reads as plastic.
BATT = ((196, 146, 158, 255), (176, 124, 138, 255), (208, 162, 172, 255))
MINERAL = ((150, 140, 128, 255), (130, 120, 110, 255), (166, 156, 144, 255))


def _fibrous(seed, palette):
    """A deterministic fibre mat. Streaks are drawn along the run, which is the way the batt is
    seen once it is between two studs."""
    rng = random.Random(seed)
    base, dark, light = palette
    img = Image.new("RGBA", (SIZE, SIZE), base)
    px = img.load()
    for y in range(SIZE):
        x = 0
        while x < SIZE:
            run = rng.randint(1, 4)
            roll = rng.random()
            colour = dark if roll < 0.3 else (light if roll < 0.55 else base)
            for i in range(run):
                if x + i < SIZE:
                    px[x + i, y] = colour
            x += run
    return img


def insulation_batt():
    return _fibrous(20260919, BATT)


def insulation_mineral():
    return _fibrous(20260920, MINERAL)


# Dimensional lumber. Softwood framing is pale and yellowish, with the grain running the length of
# the member -- which is up a stud and along a plate, so the two are drawn on different axes rather
# than being one texture used twice.
WOOD_BASE = (198, 165, 112, 255)
WOOD_GRAIN = (176, 142, 92, 255)
WOOD_LIGHT = (214, 186, 138, 255)
WOOD_KNOT = (146, 112, 68, 255)


def _grain(seed, vertical):
    """Lumber, with the grain running along the member. Deterministic, so two runs match."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (SIZE, SIZE), WOOD_BASE)
    px = img.load()
    for line in range(SIZE):
        roll = rng.random()
        if roll < 0.28:
            colour = WOOD_GRAIN
        elif roll < 0.5:
            colour = WOOD_LIGHT
        else:
            continue
        # A grain line wanders a little rather than running dead straight.
        offset = 0
        for along in range(SIZE):
            if rng.random() < 0.12:
                offset += rng.choice((-1, 1))
            across = (line + offset) % SIZE
            px[(across, along) if vertical else (along, across)] = colour
    # One knot, which is what stops a flat fill reading as cardboard.
    kx, ky = rng.randint(3, 12), rng.randint(3, 12)
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            if abs(dx) + abs(dy) < 2:
                px[(kx + dx) % SIZE, (ky + dy) % SIZE] = WOOD_KNOT
    return img


# A poured topping, floated but not polished: grey with the fine aggregate speckle that separates
# concrete from flat stone at this size.
CONCRETE_BASE = (166, 166, 162, 255)
CONCRETE_DARK = (148, 148, 145, 255)
CONCRETE_LIGHT = (184, 184, 179, 255)


def concrete_topping():
    rng = random.Random(20260923)
    img = Image.new("RGBA", (SIZE, SIZE), CONCRETE_BASE)
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            roll = rng.random()
            if roll < 0.18:
                px[x, y] = CONCRETE_DARK
            elif roll < 0.32:
                px[x, y] = CONCRETE_LIGHT
    return img


def wood_stud():
    """A stud, seen on its face: grain up the member."""
    return _grain(20260921, vertical=True)


def wood_plate():
    """A plate, seen on its face: grain along the member."""
    return _grain(20260922, vertical=False)


TEXTURES = {
    "steel_stud.png": steel_stud,
    "steel_track.png": steel_track,
    "insulation_batt.png": insulation_batt,
    "insulation_mineral.png": insulation_mineral,
    "wood_stud.png": wood_stud,
    "wood_plate.png": wood_plate,
    "concrete_topping.png": concrete_topping,
}

# --------------------------------------------------------------------------------------------
# Shared geometry
# --------------------------------------------------------------------------------------------

WALL_X0, WALL_X1 = 6, 10       # the wall's thickness, centred
ARM_END = 6                    # an arm reaches from the block edge to here
HUB0, HUB1 = 6, 10             # the centre the hub or the post closes
STUD_Z0, STUD_Z1 = 3, 5        # the arm's stud, at a uniform 8/16 pitch along a run
TRACK = 1                      # track thickness


def _faces(tex, web=None, uv=None, drop=()):
    """Face textures. ``web`` marks the two broad faces that carry the knockout, ``uv`` overrides
    the position-derived UV on those faces, and ``drop`` omits faces entirely."""
    broad = web or tex
    out = {}
    for side in ("north", "south", "east", "west", "up", "down"):
        if side in drop:
            continue
        face = {"texture": broad if side in ("north", "south") else tex}
        if uv is not None and side in ("north", "south"):
            face["uv"] = list(uv)
        out[side] = face
    return out


def _box(x0, y0, z0, x1, y1, z1, tex="#track", web=None, uv=None, drop=()):
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": _faces(tex, web, uv, drop)}


def _model(elements):
    return {"textures": {"particle": "#track"}, "elements": elements}


# The narrow partition: a 2.5 in stud rather than a 3.625 in one. Its web is only 2/16 across, so
# the default position-derived UV would show texture columns 7-8 -- which is exactly the knockout
# and none of its rim, leaving a stud that is entirely hole. The web is given the full four-column
# window explicitly instead, squeezed onto the narrower face.
NARROW_X0, NARROW_X1 = 7, 9
NARROW_UV = (WALL_X0, 0, WALL_X1, 16)

# The opening members: a door or window rough opening carries king studs at the block edges and a
# header across the top, with the bay between them left out.
KING_Z = 2
HEADER_Y = 13
SILL_Y0, SILL_Y1 = 6, 8
# The hollow metal frame is a pressed section standing slightly proud of the studs it is set into.
HM_X0, HM_X1 = 5, 11
HM_JAMB_Z = 3
HM_HEAD_Y = 13


# Insulation sits inside the studs rather than flush with them, so the studs still read as the
# nearest thing. Its up and down faces are omitted deliberately: it runs the full height so that a
# stacked wall's insulation is continuous, which means it passes through the track at the top and
# bottom course, and a face there would be coplanar with the track's own and fight it.
INSUL_INSET = 0.5


def insulation_parts(prefix, x0, x1):
    """The bays of one arm, and of the centre, packed out."""
    ix0, ix1 = x0 + INSUL_INSET, x1 - INSUL_INSET
    return {
        prefix + "arm_insulation": _model([
            _box(ix0, 0, 0, ix1, 16, STUD_Z0, tex="#insulation", drop=("up", "down")),
            _box(ix0, 0, STUD_Z1, ix1, 16, ARM_END, tex="#insulation", drop=("up", "down"))]),
        prefix + "hub_insulation": _model([
            _box(ix0, 0, HUB0, ix1, 16, HUB1, tex="#insulation", drop=("up", "down"))]),
    }


# Fire blocking: a short length of the same lumber laid flat between the studs, part way up. Real
# framing carries a row of it to stop a bay acting as a chimney.
BLOCK_Y0, BLOCK_Y1 = 7, 9


def blocking_parts(prefix, x0, x1):
    """The bay of one arm, and of the centre, with a row of blocking through it."""
    return {
        prefix + "arm_blocking": _model([
            _box(x0, BLOCK_Y0, 0, x1, BLOCK_Y1, STUD_Z0),
            _box(x0, BLOCK_Y0, STUD_Z1, x1, BLOCK_Y1, ARM_END)]),
        prefix + "hub_blocking": _model([
            _box(x0, BLOCK_Y0, HUB0, x1, BLOCK_Y1, HUB1)]),
    }


def wall_parts(prefix, x0, x1, uv=None):
    """The six models a framed wall of the given thickness draws."""
    return {
        prefix + "post": _model([_box(x0, 0, HUB0, x1, 16, HUB1)]),
        prefix + "arm_stud": _model([
            _box(x0, 0, STUD_Z0, x1, 16, STUD_Z1, web="#stud", uv=uv)]),
        prefix + "arm_track_bottom": _model([
            _box(x0, 0, 0, x1, TRACK, STUD_Z0),
            _box(x0, 0, STUD_Z1, x1, TRACK, ARM_END)]),
        prefix + "arm_track_top": _model([
            _box(x0, 16 - TRACK, 0, x1, 16, STUD_Z0),
            _box(x0, 16 - TRACK, STUD_Z1, x1, 16, ARM_END)]),
        prefix + "hub_track_bottom": _model([_box(x0, 0, HUB0, x1, TRACK, HUB1)]),
        prefix + "hub_track_top": _model([_box(x0, 16 - TRACK, HUB0, x1, 16, HUB1)]),
    }


def shared_models():
    """The geometry every framing block of this shape draws, textured by its own model."""
    models = {}
    models.update(wall_parts("framing_narrow_", NARROW_X0, NARROW_X1, NARROW_UV))
    models.update(insulation_parts("framing_", WALL_X0, WALL_X1))
    models.update(blocking_parts("framing_", WALL_X0, WALL_X1))
    models.update(insulation_parts("framing_narrow_", NARROW_X0, NARROW_X1))
    # The bare floor runner: track with no stud gap in it, because no stud stands there yet.
    models["framing_runner_arm"] = _model([_box(WALL_X0, 0, 0, WALL_X1, TRACK, ARM_END)])
    models["framing_runner_hub"] = _model([_box(WALL_X0, 0, HUB0, WALL_X1, TRACK, HUB1)])
    # A door rough opening: king stud at the block edge, header over, and no sole plate across
    # the opening -- the floor runs through a doorway.
    models["framing_door_arm"] = _model([
        _box(WALL_X0, 0, 0, WALL_X1, 16, KING_Z, web="#stud"),
        _box(WALL_X0, HEADER_Y, KING_Z, WALL_X1, 16, ARM_END)])
    models["framing_door_hub"] = _model([_box(WALL_X0, HEADER_Y, HUB0, WALL_X1, 16, HUB1)])
    # A window rough opening is the same with a sill, and the sole plate back under the bay.
    models["framing_window_arm"] = _model([
        _box(WALL_X0, 0, 0, WALL_X1, 16, KING_Z, web="#stud"),
        _box(WALL_X0, HEADER_Y, KING_Z, WALL_X1, 16, ARM_END),
        _box(WALL_X0, SILL_Y0, KING_Z, WALL_X1, SILL_Y1, ARM_END),
        _box(WALL_X0, 0, KING_Z, WALL_X1, TRACK, ARM_END)])
    models["framing_window_hub"] = _model([
        _box(WALL_X0, HEADER_Y, HUB0, WALL_X1, 16, HUB1),
        _box(WALL_X0, SILL_Y0, HUB0, WALL_X1, SILL_Y1, HUB1),
        _box(WALL_X0, 0, HUB0, WALL_X1, TRACK, HUB1)])
    # Flat strap X-bracing, one diagonal per arm. Rotated geometry is limited to one axis and to
    # 45 degrees, which is why this is a strap across the arm rather than corner to corner.
    models["framing_brace_arm"] = _model([
        dict(_box(WALL_X0 + 1, 7, 0, WALL_X1 - 1, 9, ARM_END),
             rotation={"origin": [8, 8, 3], "axis": "x", "angle": 45, "rescale": True})])
    # The hollow metal frame: jambs and head, smooth pressed steel with no knockouts.
    models["framing_hmframe_arm"] = _model([
        _box(HM_X0, 0, 0, HM_X1, 16, HM_JAMB_Z),
        _box(HM_X0, HM_HEAD_Y, HM_JAMB_Z, HM_X1, 16, ARM_END)])
    models["framing_hmframe_hub"] = _model([_box(HM_X0, HM_HEAD_Y, HUB0, HM_X1, 16, HUB1)])
    models.update({
        # The post: a built-up corner post, solid, so no knockout. Full height, so a stacked
        # corner's post is one unbroken member.
        "framing_post": _model([
            _box(WALL_X0, 0, HUB0, WALL_X1, 16, HUB1),
        ]),
        # The stud runs the full height of the block.
        "framing_arm_stud": _model([
            _box(WALL_X0, 0, STUD_Z0, WALL_X1, 16, STUD_Z1, web="#stud"),
        ]),
        # Track, split around the stud so nothing overlaps and no top faces fight.
        "framing_arm_track_bottom": _model([
            _box(WALL_X0, 0, 0, WALL_X1, TRACK, STUD_Z0),
            _box(WALL_X0, 0, STUD_Z1, WALL_X1, TRACK, ARM_END),
        ]),
        "framing_arm_track_top": _model([
            _box(WALL_X0, 16 - TRACK, 0, WALL_X1, 16, STUD_Z0),
            _box(WALL_X0, 16 - TRACK, STUD_Z1, WALL_X1, 16, ARM_END),
        ]),
        # The hub closes the centre where no post stands: a straight run, or a lone block.
        "framing_hub_track_bottom": _model([
            _box(WALL_X0, 0, HUB0, WALL_X1, TRACK, HUB1),
        ]),
        "framing_hub_track_top": _model([
            _box(WALL_X0, 16 - TRACK, HUB0, WALL_X1, 16, HUB1),
        ]),
    })
    return models


def inventory_model(stud_tex, track_tex):
    """One complete bay, so the item reads as a piece of wall rather than a lone post. The track
    is split around both studs for the same no-coplanar-faces reason the world models are."""
    return {
        "parent": "block/block",
        "textures": {"stud": stud_tex, "track": track_tex, "particle": track_tex},
        "elements": [
            _box(WALL_X0, 0, 0, WALL_X1, TRACK, STUD_Z0),
            _box(WALL_X0, 0, STUD_Z1, WALL_X1, TRACK, 16 - STUD_Z1),
            _box(WALL_X0, 0, 16 - STUD_Z0, WALL_X1, TRACK, 16),
            _box(WALL_X0, 16 - TRACK, 0, WALL_X1, 16, STUD_Z0),
            _box(WALL_X0, 16 - TRACK, STUD_Z1, WALL_X1, 16, 16 - STUD_Z1),
            _box(WALL_X0, 16 - TRACK, 16 - STUD_Z0, WALL_X1, 16, 16),
            _box(WALL_X0, TRACK, STUD_Z0, WALL_X1, 16 - TRACK, STUD_Z1, web="#stud"),
            _box(WALL_X0, TRACK, 16 - STUD_Z1, WALL_X1, 16 - TRACK, 16 - STUD_Z0, web="#stud"),
        ],
    }


# --------------------------------------------------------------------------------------------
# Horizontal structure: members that SPAN rather than join
# --------------------------------------------------------------------------------------------
#
# A joist is not a wall. A wall is a junction and decides itself from four neighbours; a joist runs
# in one direction and repeats, so all it needs to know is which way it points. Everything here is
# drawn spanning NORTH-SOUTH and the blockstate turns it a quarter for the other axis.
#
# Nothing here is join-aware. A row of joists tiles seamlessly because each block is the full
# length of its own cell, and where a run stops it simply stops -- which is what a joist does where
# it lands on a wall. Bearing seats at the ends were considered and left out; see the plan.


def _span_faces(broad, ends):
    """Face textures for a member spanning north-south: the broad faces are east and west."""
    return {"east": {"texture": broad}, "west": {"texture": broad},
            "up": {"texture": broad}, "down": {"texture": broad},
            "north": {"texture": ends}, "south": {"texture": ends}}


def _span(x0, y0, z0, x1, y1, z1, broad="#body", ends=None, rotation=None):
    box = {"from": [x0, y0, z0], "to": [x1, y1, z1],
           "faces": _span_faces(broad, ends or broad)}
    if rotation is not None:
        box["rotation"] = rotation
    return box


def _diagonals(y_mid, half, thickness=1.0):
    """The V of web bracing between two chords, one diagonal each way.

    Rotation is limited to one axis and to 45 degrees, so the web is drawn as a true 45 degree
    zigzag rather than at whatever angle the chord spacing would imply. Over one block that reads
    correctly and it is what a real open-web joist is close to anyway.
    """
    x0, x1 = 7, 9
    return [
        _span(x0, y_mid - thickness, 1, x1, y_mid + thickness, 9,
              rotation={"origin": [8, y_mid, 5], "axis": "x", "angle": 45, "rescale": True}),
        _span(x0, y_mid - thickness, 7, x1, y_mid + thickness, 15,
              rotation={"origin": [8, y_mid, 11], "axis": "x", "angle": -45, "rescale": True}),
    ]


def span_models():
    """One model per spanning member, used for both axes and for the item."""
    models = {}

    # Dimensional lumber on edge, a 2x10 joist.
    models["span_wood_joist"] = [_span(6, 4, 0, 10, 14, 16)]

    # An engineered I-joist: two flanges and a thin web.
    models["span_wood_i_joist"] = [
        _span(5, 4, 0, 11, 5, 16),
        _span(7, 5, 0, 9, 13, 16),
        _span(5, 13, 0, 11, 14, 16),
    ]

    # Open-web steel joist: two chords and a bracing web between them.
    models["span_bar_joist"] = ([_span(6, 4, 0, 10, 6, 16), _span(6, 12, 0, 10, 14, 16)]
                                + _diagonals(9, 3))

    # A joist girder is the same idea, deeper and heavier, carrying joists rather than deck.
    models["span_joist_girder"] = ([_span(5, 1, 0, 11, 4, 16), _span(5, 12, 0, 11, 15, 16)]
                                   + _diagonals(8, 4, thickness=1.5))

    # Corrugated B-deck. The rib pitch divides the block exactly, so a sheet tiles without the
    # ribs bunching or splitting at the seam.
    deck = [_span(0, 12, 0, 16, 13, 16),
            _span(0, 13, 0, 4, 15, 16),
            _span(8, 13, 0, 12, 15, 16)]
    models["span_metal_roof_deck"] = deck
    # The composite slab: concrete in the flutes and a topping over, drawn so that it never shares
    # a plane with the deck it sits on.
    models["span_metal_roof_deck_concrete"] = deck + [
        _span(4, 13, 0, 8, 15, 16, broad="#topping"),
        _span(12, 13, 0, 16, 15, 16, broad="#topping"),
        _span(0, 15, 0, 16, 16, 16, broad="#topping"),
    ]
    return models


def span_blockstate(name):
    """Two variants: the model as drawn, and the same turned a quarter for the other axis."""
    model = MODEL_REF % name
    return {"variants": {
        "axis=z": {"model": model},
        "axis=x": {"model": model, "y": 90},
        "inventory": {"model": model},
    }}


SPANS = [
    ("wood_joist", "BlockWoodJoist", "span_wood_joist", "wood_plate", None,
     "Wood Joist", "Vigueta de Madera", "Holzbalken", "Tr\u00e4bj\u00e4lke"),
    ("wood_i_joist", "BlockWoodIJoist", "span_wood_i_joist", "wood_plate", None,
     "Wood I-Joist", "Vigueta I de Madera", "Holz-I-Tr\u00e4ger", "I-balk av Tr\u00e4"),
    ("bar_joist", "BlockBarJoist", "span_bar_joist", "steel_track", None,
     "Open-Web Bar Joist", "Vigueta de Celos\u00eda", "Gittertr\u00e4ger",
     "F\u00f6rvandlingsbalk"),
    ("joist_girder", "BlockJoistGirder", "span_joist_girder", "steel_track", None,
     "Joist Girder", "Viga de Celos\u00eda", "Gitterunterzug", "Fackverksbalk"),
    ("metal_roof_deck", "BlockMetalRoofDeck", "span_metal_roof_deck", "steel_track", None,
     "Metal Roof Deck", "Chapa Colaborante", "Trapezblech", "Takpl\u00e5t"),
    ("metal_roof_deck_concrete", "BlockMetalRoofDeckConcrete", "span_metal_roof_deck_concrete",
     "steel_track", "concrete_topping",
     "Metal Deck with Concrete", "Chapa Colaborante con Hormig\u00f3n",
     "Verbunddecke", "Takpl\u00e5t med Betong"),
]


# --------------------------------------------------------------------------------------------
# Blockstates
# --------------------------------------------------------------------------------------------

DIRS = (("north", None), ("east", 90), ("south", 180), ("west", 270))
ALONE = {"north": "false", "east": "false", "south": "false", "west": "false"}

# A post stands where a wall really has one: where two runs meet, and where a run stops.
CORNERS = [{"north": "true", "east": "true"}, {"east": "true", "south": "true"},
           {"south": "true", "west": "true"}, {"west": "true", "north": "true"}]
ENDS = [dict(ALONE, **{d: "true"}) for d, _ in DIRS]

# Everywhere else -- a straight run, or a lone block -- the centre is closed by the hub instead.
# The two sets are disjoint and between them cover all sixteen connection combinations.
NO_POST = [
    {"north": "true", "south": "true", "east": "false", "west": "false"},
    {"east": "true", "west": "true", "north": "false", "south": "false"},
    dict(ALONE),
]


def arm_conditions():
    """Every (condition, rotation) an arm is drawn for: one per connection, plus the lone block's
    two arms, which take their axis from FACING because there is nothing else to take it from."""
    out = [({d: "true"}, y) for d, y in DIRS]
    for axis, rotations in (("east|west", (None, 180)), ("north|south", (90, 270))):
        for y in rotations:
            out.append((dict(ALONE, facing=axis), y))
    return out


def _apply(model, y=None):
    a = {"model": MODEL_REF % model, "uvlock": True}
    if y is not None:
        a["y"] = y
    return a


def _inventory(name, insulated=False):
    """The item models. Each insulation gets a variant of its own so the three stacks in the
    creative tab are told apart by their icons and not only by their names."""
    out = {"inventory": {"model": MODEL_REF % (name + "_inventory")}}
    if insulated:
        for material in INSULATIONS:
            out["inventory_" + material] = {
                "model": MODEL_REF % ("%s_inventory_%s" % (name, material))}
    return out


INSULATIONS = ("batt", "mineral")


def wall_blockstate(name, extra_arms=(), insulated=False, extra_hubs=()):
    """The multipart for a framed wall.

    Conditions are written out per connection rather than folded into an OR, because the format
    cannot AND a sibling key onto an OR: a ``when`` holding "OR" plus anything else is read as an
    AND over properties, one of which is called "OR".
    """
    parts = [{"when": {"OR": CORNERS + ENDS},
              "apply": {"model": MODEL_REF % (name + "_post")}}]
    for suffix in ("arm_stud",) + tuple(extra_arms):
        for when, y in arm_conditions():
            parts.append({"when": when, "apply": _apply("%s_%s" % (name, suffix), y)})
    # Track only at the ends of a STACK: a real stud runs the full height of the wall and meets
    # track at the floor and the ceiling only.
    for edge, suffix in (("down", "track_bottom"), ("up", "track_top")):
        for when, y in arm_conditions():
            parts.append({"when": dict(when, **{edge: "false"}),
                          "apply": _apply("%s_arm_%s" % (name, suffix), y)})
        for when in NO_POST:
            parts.append({"when": dict(when, **{edge: "false"}),
                          "apply": {"model": MODEL_REF % ("%s_hub_%s" % (name, suffix))}})
    # Anything that fills the centre bay follows the hub, never the post: where a post stands the
    # centre is solid and there is no bay to fill.
    for suffix in extra_hubs:
        for when in NO_POST:
            parts.append({"when": when,
                          "apply": {"model": MODEL_REF % ("%s_%s" % (name, suffix))}})
    # Insulation fills the bays, so it follows the arms and the hub but never the post: where a
    # post stands the centre is solid and there is no bay to pack.
    if insulated:
        for material in INSULATIONS:
            for when, y in arm_conditions():
                parts.append({"when": dict(when, insulation=material),
                              "apply": _apply("%s_arm_insulation_%s" % (name, material), y)})
            for when in NO_POST:
                parts.append({"when": dict(when, insulation=material),
                              "apply": {"model": MODEL_REF
                                        % ("%s_hub_insulation_%s" % (name, material))}})
    return {"variants": _inventory(name, insulated), "multipart": parts}


def opening_blockstate(name):
    """A runner, a rough opening or a door frame. These have no post and no course-end track: a
    header, a sill and a floor runner are features of the opening itself, present whatever stands
    above or below, so nothing here is conditioned on the vertical neighbours."""
    parts = [{"apply": {"model": MODEL_REF % (name + "_hub")}}]
    for when, y in arm_conditions():
        parts.append({"when": when, "apply": _apply(name + "_arm", y)})
    return {"variants": _inventory(name), "multipart": parts}


# --------------------------------------------------------------------------------------------
# Inventory models
# --------------------------------------------------------------------------------------------

def _bay_tracks(x0, x1, y0, y1):
    """A track band across a whole bay, split around both studs so no top faces fight."""
    return [_box(x0, y0, 0, x1, y1, STUD_Z0),
            _box(x0, y0, STUD_Z1, x1, y1, 16 - STUD_Z1),
            _box(x0, y0, 16 - STUD_Z0, x1, y1, 16)]


def wall_inventory(x0, x1, uv=None, extra=()):
    """One complete bay, so the item reads as a piece of wall rather than a lone post."""
    elements = _bay_tracks(x0, x1, 0, TRACK) + _bay_tracks(x0, x1, 16 - TRACK, 16)
    elements.append(_box(x0, TRACK, STUD_Z0, x1, 16 - TRACK, STUD_Z1, web="#stud", uv=uv))
    elements.append(_box(x0, TRACK, 16 - STUD_Z1, x1, 16 - TRACK, 16 - STUD_Z0,
                         web="#stud", uv=uv))
    return elements + list(extra)


def insulated_bay(x0, x1):
    """The three bays of a whole block packed out, for an insulated variant's item icon."""
    ix0, ix1 = x0 + INSUL_INSET, x1 - INSUL_INSET
    return [_box(ix0, 0, 0, ix1, 16, STUD_Z0, tex="#insulation", drop=("up", "down")),
            _box(ix0, 0, STUD_Z1, ix1, 16, 16 - STUD_Z1, tex="#insulation",
                 drop=("up", "down")),
            _box(ix0, 0, 16 - STUD_Z0, ix1, 16, 16, tex="#insulation", drop=("up", "down"))]


def runner_inventory():
    return [_box(WALL_X0, 0, 0, WALL_X1, TRACK, 16)]


def opening_inventory(x0, x1, king_z, head_y, sill=None, sole=False):
    """Both jambs and the head, which is what a doorway or a frame looks like on its own."""
    elements = [_box(x0, 0, 0, x1, 16, king_z, web="#stud"),
                _box(x0, 0, 16 - king_z, x1, 16, 16, web="#stud"),
                _box(x0, head_y, king_z, x1, 16, 16 - king_z)]
    if sill is not None:
        elements.append(_box(x0, sill[0], king_z, x1, sill[1], 16 - king_z))
    if sole:
        elements.append(_box(x0, 0, king_z, x1, TRACK, 16 - king_z))
    return elements


def inventory_model(stud_tex, track_tex, elements):
    return {"parent": "block/block",
            "textures": {"stud": stud_tex, "track": track_tex, "particle": track_tex},
            "elements": elements}


# --------------------------------------------------------------------------------------------
# Flavours: which shared models a block wears, and how its blockstate is built
# --------------------------------------------------------------------------------------------

WALL_PARTS = ("post", "arm_stud", "arm_track_bottom", "arm_track_top",
              "hub_track_bottom", "hub_track_top")


def _wall_flavour(prefix, x0, x1, uv=None, braced=False, blocking=False):
    parts = {part: prefix + part for part in WALL_PARTS}
    # Each insulation gets its own textured model, since a multipart apply can pick a model but
    # cannot rebind a texture.
    for material in INSULATIONS:
        for part in ("arm_insulation", "hub_insulation"):
            parts["%s_%s" % (part, material)] = prefix + part
    extra = ()
    inv_extra = ()
    hubs = ()
    if blocking:
        parts["arm_blocking"] = "framing_arm_blocking"
        parts["hub_blocking"] = "framing_hub_blocking"
        extra += ("arm_blocking",)
        hubs = ("hub_blocking",)
        inv_extra += (_box(x0, BLOCK_Y0, 0, x1, BLOCK_Y1, STUD_Z0),
                      _box(x0, BLOCK_Y0, STUD_Z1, x1, BLOCK_Y1, 16 - STUD_Z1),
                      _box(x0, BLOCK_Y0, 16 - STUD_Z0, x1, BLOCK_Y1, 16))
    if braced:
        parts["brace_arm"] = "framing_brace_arm"
        extra += ("brace_arm",)
        inv_extra = (dict(_box(x0 + 1, 7, 0, x1 - 1, 9, 16),
                          rotation={"origin": [8, 8, 8], "axis": "x", "angle": 45,
                                    "rescale": True}),)
    return {
        "parts": parts,
        "insulation_parts": {"%s_%s" % (part, material): material
                             for material in INSULATIONS
                             for part in ("arm_insulation", "hub_insulation")},
        "state": lambda name: wall_blockstate(name, extra, insulated=True, extra_hubs=hubs),
        "inventory": lambda: wall_inventory(x0, x1, uv, inv_extra),
        "insulated_inventory": lambda: wall_inventory(x0, x1, uv,
                                                      tuple(inv_extra) + tuple(
                                                          insulated_bay(x0, x1))),
    }


def _opening_flavour(prefix, inventory):
    return {
        "parts": {"arm": prefix + "arm", "hub": prefix + "hub"},
        "state": opening_blockstate,
        "inventory": inventory,
    }


FLAVOURS = {
    "wall": _wall_flavour("framing_", WALL_X0, WALL_X1),
    "blocking": _wall_flavour("framing_", WALL_X0, WALL_X1, blocking=True),
    "narrow": _wall_flavour("framing_narrow_", NARROW_X0, NARROW_X1, NARROW_UV),
    "braced": _wall_flavour("framing_", WALL_X0, WALL_X1, braced=True),
    "runner": _opening_flavour("framing_runner_", runner_inventory),
    "door": _opening_flavour(
        "framing_door_", lambda: opening_inventory(WALL_X0, WALL_X1, KING_Z, HEADER_Y)),
    "window": _opening_flavour(
        "framing_window_",
        lambda: opening_inventory(WALL_X0, WALL_X1, KING_Z, HEADER_Y,
                                  sill=(SILL_Y0, SILL_Y1), sole=True)),
    "hmframe": _opening_flavour(
        "framing_hmframe_",
        lambda: opening_inventory(HM_X0, HM_X1, HM_JAMB_Z, HM_HEAD_Y)),
}


# --------------------------------------------------------------------------------------------
# Catalogue
# --------------------------------------------------------------------------------------------

# What each insulation is called. Batt is glass wool and mineral is rock wool, which is what the
# German and Swedish names say outright; English and Spanish name the product instead.
INSULATION_LANG = {
    "batt": {"en_us": "Batt Insulation", "es_es": "Aislamiento de Fibra",
             "de_de": "Glaswolle", "sv_se": "Glasull"},
    "mineral": {"en_us": "Mineral Wool", "es_es": "Lana Mineral",
                "de_de": "Steinwolle", "sv_se": "Stenull"},
}


def insulated_name(base, suffix):
    """A variant name, folded into the base name's own bracket if it already has one, so that a
    narrow wall reads "(Narrow, Batt Insulation)" rather than "(Narrow) (Batt Insulation)"."""
    if base.endswith(")"):
        return base[:-1] + ", " + suffix + ")"
    return base + " (" + suffix + ")"


def insulated_entries():
    """(lang key, per-language name) for every insulated variant in the catalogue."""
    out = []
    for entry in CATALOGUE:
        if not FLAVOURS[entry["flavour"]].get("insulation_parts"):
            continue
        for material, names in sorted(INSULATION_LANG.items()):
            key = "tile.%s.%s.name" % (entry["name"], material)
            out.append((key, {lang: insulated_name(entry["lang"][lang], names[lang])
                              for lang in names}))
    return out


def _entry(name, cls, flavour, en, es, de, sv, stud="steel_stud", track="steel_track"):
    return {"name": name, "class": cls, "flavour": flavour,
            "stud": stud, "track": track,
            "lang": {"en_us": en, "es_es": es, "de_de": de, "sv_se": sv}}


def _wood(name, cls, flavour, en, es, de, sv):
    return _entry(name, cls, flavour, en, es, de, sv, stud="wood_stud", track="wood_plate")


CATALOGUE = [
    _entry("steel_stud_wall", "BlockSteelStudWall", "wall",
           "Steel Stud Wall", "Muro de Montantes de Acero",
           "St\u00e4nderwand (Stahl)", "Regelv\u00e4gg av St\u00e5l"),
    _entry("steel_stud_wall_narrow", "BlockSteelStudWallNarrow", "narrow",
           "Steel Stud Wall (Narrow)", "Muro de Montantes de Acero (Estrecho)",
           "St\u00e4nderwand (Stahl, Schmal)", "Regelv\u00e4gg av St\u00e5l (Smal)"),
    _entry("steel_stud_wall_braced", "BlockSteelStudWallBraced", "braced",
           "Steel Stud Wall (Braced)", "Muro de Montantes de Acero (Arriostrado)",
           "St\u00e4nderwand (Stahl, Ausgesteift)", "Regelv\u00e4gg av St\u00e5l (Kryssad)"),
    _entry("steel_stud_wall_door", "BlockSteelStudWallDoor", "door",
           "Steel Stud Wall (Door Opening)", "Muro de Montantes de Acero (Vano de Puerta)",
           "St\u00e4nderwand (Stahl, T\u00fcr\u00f6ffnung)",
           "Regelv\u00e4gg av St\u00e5l (D\u00f6rr\u00f6ppning)"),
    _entry("steel_stud_wall_window", "BlockSteelStudWallWindow", "window",
           "Steel Stud Wall (Window Opening)", "Muro de Montantes de Acero (Vano de Ventana)",
           "St\u00e4nderwand (Stahl, Fenster\u00f6ffnung)",
           "Regelv\u00e4gg av St\u00e5l (F\u00f6nster\u00f6ppning)"),
    _entry("steel_track", "BlockSteelTrack", "runner",
           "Steel Track", "Canal de Acero", "Stahlprofilschiene", "St\u00e5lskena"),
    _entry("hollow_metal_door_frame", "BlockHollowMetalDoorFrame", "hmframe",
           "Hollow Metal Door Frame", "Marco de Puerta de Acero",
           "Stahlzarge", "St\u00e5ld\u00f6rrkarm"),
    _wood("wood_stud_wall", "BlockWoodStudWall", "wall",
          "Wood Stud Wall", "Muro de Montantes de Madera",
          "St\u00e4nderwand (Holz)", "Regelv\u00e4gg av Tr\u00e4"),
    _wood("wood_stud_wall_narrow", "BlockWoodStudWallNarrow", "narrow",
          "Wood Stud Wall (Narrow)", "Muro de Montantes de Madera (Estrecho)",
          "St\u00e4nderwand (Holz, Schmal)", "Regelv\u00e4gg av Tr\u00e4 (Smal)"),
    _wood("wood_stud_wall_braced", "BlockWoodStudWallBraced", "braced",
          "Wood Stud Wall (Braced)", "Muro de Montantes de Madera (Arriostrado)",
          "St\u00e4nderwand (Holz, Ausgesteift)", "Regelv\u00e4gg av Tr\u00e4 (Kryssad)"),
    _wood("wood_stud_wall_blocking", "BlockWoodStudWallBlocking", "blocking",
          "Wood Stud Wall (Fire Blocking)", "Muro de Montantes de Madera (Cortafuego)",
          "St\u00e4nderwand (Holz, Brandsperre)", "Regelv\u00e4gg av Tr\u00e4 (Brandstopp)"),
    _wood("wood_stud_wall_door", "BlockWoodStudWallDoor", "door",
          "Wood Stud Wall (Door Opening)", "Muro de Montantes de Madera (Vano de Puerta)",
          "St\u00e4nderwand (Holz, T\u00fcr\u00f6ffnung)",
          "Regelv\u00e4gg av Tr\u00e4 (D\u00f6rr\u00f6ppning)"),
    _wood("wood_stud_wall_window", "BlockWoodStudWallWindow", "window",
          "Wood Stud Wall (Window Opening)", "Muro de Montantes de Madera (Vano de Ventana)",
          "St\u00e4nderwand (Holz, Fenster\u00f6ffnung)",
          "Regelv\u00e4gg av Tr\u00e4 (F\u00f6nster\u00f6ppning)"),
    _wood("wood_plate", "BlockWoodPlate", "runner",
          "Wood Sole Plate", "Solera de Madera", "Schwelle (Holz)", "Syll av Tr\u00e4"),
]


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------

def _write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


def write_all(tex_dir, shared_dir, model_dir, state_dir):
    """Writes every generated file and returns the paths written, relative to their roots."""
    written = []

    os.makedirs(tex_dir, exist_ok=True)
    for filename, draw in sorted(TEXTURES.items()):
        draw().save(os.path.join(tex_dir, filename))
        written.append(("tex", filename))

    for name, model in sorted(shared_models().items()):
        _write_json(os.path.join(shared_dir, name + ".json"), model)
        written.append(("shared", name + ".json"))

    for entry in CATALOGUE:
        name = entry["name"]
        flavour = FLAVOURS[entry["flavour"]]
        stud_tex = TEX_REF % entry["stud"]
        track_tex = TEX_REF % entry["track"]
        textures = {"stud": stud_tex, "track": track_tex, "particle": track_tex}
        insul = flavour.get("insulation_parts", {})
        for suffix, shared in sorted(flavour["parts"].items()):
            filename = "%s_%s.json" % (name, suffix)
            bindings = textures
            if suffix in insul:
                bindings = dict(textures,
                                insulation=TEX_REF % ("insulation_" + insul[suffix]))
            _write_json(os.path.join(model_dir, filename),
                        {"parent": SHARED_REF % shared, "textures": bindings})
            written.append(("model", filename))
        _write_json(os.path.join(model_dir, name + "_inventory.json"),
                    inventory_model(stud_tex, track_tex, flavour["inventory"]()))
        written.append(("model", name + "_inventory.json"))
        if flavour.get("insulated_inventory"):
            for material in INSULATIONS:
                model = inventory_model(stud_tex, track_tex, flavour["insulated_inventory"]())
                model["textures"]["insulation"] = TEX_REF % ("insulation_" + material)
                filename = "%s_inventory_%s.json" % (name, material)
                _write_json(os.path.join(model_dir, filename), model)
                written.append(("model", filename))
        _write_json(os.path.join(state_dir, name + ".json"), flavour["state"](name))
        written.append(("state", name + ".json"))

    geometry = span_models()
    for name, _cls, shape, body, topping, _en, _es, _de, _sv in SPANS:
        textures = {"body": TEX_REF % body, "particle": TEX_REF % body}
        if topping:
            textures["topping"] = TEX_REF % topping
        _write_json(os.path.join(model_dir, name + ".json"),
                    {"parent": "block/block", "textures": textures,
                     "elements": geometry[shape]})
        written.append(("model", name + ".json"))
        _write_json(os.path.join(state_dir, name + ".json"), span_blockstate(name))
        written.append(("state", name + ".json"))

    return written


def fragments():
    lines = ["# lang lines, one per language file under assets/csm/lang/", ""]
    for lang in ("en_us", "es_es", "de_de", "sv_se"):
        lines.append("## " + lang)
        for entry in CATALOGUE:
            lines.append("tile.%s.name=%s" % (entry["name"], entry["lang"][lang]))
        lines.append("")
    lines.append("## spanning members")
    for lang in ("en_us", "es_es", "de_de", "sv_se"):
        lines.append("### " + lang)
        idx = {"en_us": 5, "es_es": 6, "de_de": 7, "sv_se": 8}[lang]
        for span in SPANS:
            lines.append("tile.%s.name=%s" % (span[0], span[idx]))
        lines.append("")
    lines.append("# tab registration, in CsmTabStructureFraming.initTabElements")
    lines.append("")
    for entry in CATALOGUE:
        lines.append("    initTabBlock(%s.class, fmlPreInitializationEvent); // %s"
                     % (entry["class"], entry["lang"]["en_us"]))
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

    roots = {"tex": TEX_DIR, "shared": SHARED_DIR, "model": MODEL_DIR, "state": STATE_DIR}

    if not args.check:
        written = write_all(TEX_DIR, SHARED_DIR, MODEL_DIR, STATE_DIR)
        print("Wrote %d files for %d block(s)" % (len(written), len(CATALOGUE)))
        return 0

    tmp = tempfile.mkdtemp(prefix="csm_framing_")
    try:
        tmp_roots = {k: os.path.join(tmp, k) for k in roots}
        for path in tmp_roots.values():
            os.makedirs(path, exist_ok=True)
        written = write_all(tmp_roots["tex"], tmp_roots["shared"], tmp_roots["model"],
                            tmp_roots["state"])
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
        print("%d generated framing files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
