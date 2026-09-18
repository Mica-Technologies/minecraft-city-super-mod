#!/usr/bin/env python3
"""Every asset the garage doors ship.

    python dev-env-utils/scripts/gen_garage_doors.py
    python dev-env-utils/scripts/gen_garage_doors.py --check
    python dev-env-utils/scripts/gen_garage_doors.py --fragments   # lang lines to paste

Five blocks, one class (BlockGarageDoor): three sectional overhead doors (white raised panel,
windowed, commercial steel), a galvanized roll-up door and a security grille. Each is drawn here
with the INSIDE of the building to the north and the street to the south, and turned by the
blockstate to the way it faces. The door hangs just behind the wall, past the opening's inside
face (z -2.5..-0.5 of 16, outside its own cell), with its tracks on the wall beside the opening --
where a real one is, and where the bend over the top runs through open garage rather than into the
wall over the opening. A closed door has a one-pixel lip past each side and over the top, so there
is no slit to see through where it meets the wall.

A door is built to the size of its opening, and what belongs to the whole door is drawn only on
the block at that edge of it: the side tracks or guides where ccw / cw = false, the roll-up's
hood where up = false, the bottom seal or bar where down = false.

A sectional door's track bends over at the top of the opening and runs back along the ceiling as
far as the door is tall, and an open door's panels lie along it. That reaches up to eight blocks
back from the top course, far past the block-and-a-half a JSON element can reach, so the bend,
the ceiling track and the open panels are OBJ models, one per door height (the block's `depth`,
actual state on the top course). They are plain vertex lists in 0..1 block space whose texture
comes from their MTL, since a vanilla multipart part cannot retexture an OBJ; v runs down the
texture as Minecraft's does (no flip-v).

TileEntityGarageDoorRenderer draws the door while it moves, and it lays each panel on the same
path with the same texture mapping these models use, so the last frame of a move and the model
that replaces it are the same picture. The constants that must agree are marked SHARED below.
"""

import argparse
import math
import os
import sys

from PIL import Image

import gen_cmu
import gen_logistics
import gen_scaffold as sc

REPO = sc.REPO
MODULE = sc.MODULE
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "garage")
MODEL_DIR = os.path.join(MODULE, "models", "block", "garage")
STATE_DIR = sc.STATE_DIR
TEX_REF = "csm:blocks/garage/%s"
MODEL_REF = "csm:garage/%s"

# registry name -> (kind, style, name in each language). Order is creative order.
BLOCKS = {
    "garage_door_sectional_white": ("sectional", "white", (
        "Garage Door (White Raised Panel)", "Puerta de Garaje (Panel Elevado Blanco)",
        "Garagentor (Weiß, Kassetten)", "Garageport (Vit, Speglar)")),
    "garage_door_sectional_windowed": ("sectional", "windowed", (
        "Garage Door (Windowed)", "Puerta de Garaje (con Ventanas)",
        "Garagentor (mit Fenstern)", "Garageport (med Fönster)")),
    "garage_door_sectional_commercial": ("sectional", "commercial", (
        "Sectional Door (Commercial Steel)", "Puerta Seccional (Acero Comercial)",
        "Sektionaltor (Industriestahl)", "Takskjutport (Industristål)")),
    "garage_door_rollup_galvanized": ("rollup", "galvanized", (
        "Roll-Up Door (Galvanized)", "Puerta Enrollable (Galvanizada)", "Rolltor (Verzinkt)",
        "Rulljalusi (Galvaniserad)")),
    "garage_door_grille": ("grille", "grille", (
        "Security Grille", "Reja de Seguridad", "Rollgitter", "Säkerhetsgaller")),
}

# The fittings that go with them: the opener (BlockGarageDoorOpener) and the hanger
# (BlockGarageDoorHanger) that holds it and the back of a sectional door's ceiling tracks up to
# whatever ceiling there is.
FITTINGS = {
    "garage_door_opener": ("Garage Door Opener", "Abridor de Puerta de Garaje",
                           "Garagentorantrieb", "Garageportöppnare"),
    "garage_door_hanger": ("Garage Door Hanger", "Colgador de Puerta de Garaje",
                           "Garagentor-Abhängung", "Garageportsupphängning"),
}

# The wall controls (BlockGarageDoorControl, one class constructed by name), linked to a door by
# sneak-clicking the control and then the door.
CONTROLS = {
    "garage_door_button": ("Garage Door Button", "Pulsador de Puerta de Garaje",
                           "Garagentor-Taster", "Garageportsknapp"),
    "garage_door_station": ("Garage Door Control Station", "Botonera de Puerta de Garaje",
                            "Garagentor-Bedienstation", "Garageportens Manöverpanel"),
    "garage_door_keypad": ("Door Keypad", "Teclado de Puerta", "Tür-Codeschloss",
                           "Dörrkodlås"),
}

# SHARED with BlockGarageDoorOpener.MAX_LENGTH: the longest rail, in blocks of air between the
# opener and the wall over the door.
MAX_RAIL = 10

# SHARED with BlockGarageDoor.MAX_DEPTH and TileEntityGarageDoor.BEND / PLANE.
MAX_DEPTH = 8
BEND = 0.375          # radius of the track's bend, blocks
PLANE_Z = -1.5 / 16   # the panels' mid-plane, z in the north-facing model (inside face at z 0)
HALF = 1.0 / 16       # half a sectional panel's thickness
TRACK_HALF = 1.5 / 16  # half the track channel's depth about the panels' mid-plane
TRACK_W = 1.5 / 16    # the track's width, outboard of the opening on the wall beside it

# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

_shift = gen_logistics._shift
_canvas = gen_logistics._canvas

WHITE = (238, 238, 234)
STEEL = (172, 180, 186)
GLASS_OUT = (58, 70, 82)
GLASS_IN = (104, 118, 130)


def _joints(px, rgb):
    """The two sections' joints: a shadow along each section's bottom, a lit edge along its top."""
    for x in range(16):
        for y in (7, 15):
            px[x, y] = _shift(rgb, -48)
        for y in (0, 8):
            px[x, y] = _shift(rgb, 10)


def _raised(px, rgb, y0):
    """One raised panel in a section whose top row is y0: lit top and left, shaded bottom and
    right."""
    x0, x1, top, bot = 2, 13, y0 + 2, y0 + 5
    for x in range(x0, x1 + 1):
        px[x, top] = _shift(rgb, 14)
        px[x, bot] = _shift(rgb, -30)
    for y in range(top, bot + 1):
        px[x0, y] = _shift(rgb, 10)
        px[x1, y] = _shift(rgb, -24)


def _windows(px, glass, frame_rgb):
    """Two lites in the top section, divided by a mullion."""
    for x in range(2, 14):
        for y in range(2, 6):
            if x == 7 or x == 8:
                px[x, y] = _shift(frame_rgb, -6)
            else:
                px[x, y] = _shift(glass, 12 if (x + y) % 7 == 0 else 0)
    for x in range(2, 14):
        px[x, 1] = _shift(frame_rgb, -24)
        px[x, 6] = _shift(frame_rgb, 12)


def white_face(windowed_top=False):
    img, px, _ = _canvas(20261801 + windowed_top, WHITE, 2)
    _joints(px, WHITE)
    if windowed_top:
        _windows(px, GLASS_OUT, WHITE)
    else:
        _raised(px, WHITE, 0)
    _raised(px, WHITE, 8)
    return img


def white_back(windowed_top=False):
    """The inside of a steel panel: plain, with a stile at each end and the section joints."""
    rgb = _shift(WHITE, -10)
    img, px, _ = _canvas(20261803 + windowed_top, rgb, 2)
    _joints(px, rgb)
    for y in range(16):
        px[0, y] = _shift(rgb, -14)
        px[15, y] = _shift(rgb, -14)
    if windowed_top:
        _windows(px, GLASS_IN, rgb)
    return img


def commercial_face():
    """Commercial steel: flush sections ribbed every two pixels."""
    img, px, rng = _canvas(20261805, STEEL, 2)
    for y in range(16):
        d = 8 if y % 2 == 0 else -8
        for x in range(16):
            px[x, y] = _shift(STEEL, d + rng.uniform(-2, 2))
    _joints(px, STEEL)
    return img


def commercial_back():
    rgb = _shift(STEEL, 10)
    img, px, _ = _canvas(20261806, rgb, 2)
    _joints(px, rgb)
    for y in range(16):
        px[0, y] = _shift(rgb, -14)
        px[15, y] = _shift(rgb, -14)
    return img


def rollup_curtain():
    """Galvanized curtain slats: interlocking, a lit crown and a shaded valley every two pixels,
    with a faint spangle."""
    base = (178, 182, 184)
    img, px, rng = _canvas(20261807, base, 3)
    for y in range(16):
        d = 12 if y % 2 == 0 else -16
        for x in range(16):
            px[x, y] = _shift(base, d + rng.uniform(-4, 4))
    return img


def grille_mesh():
    """A rolling grille's links: horizontal bars every four pixels, joined by short vertical links
    staggered like brickwork, open between."""
    base = (150, 154, 158)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            bar = y % 4 == 0
            link = (x % 8 == (0 if (y // 4) % 2 == 0 else 4))
            if bar:
                px[x, y] = _shift(base, 10)
            elif link:
                px[x, y] = _shift(base, -10)
    return img


def track():
    """Galvanized track and angle: punched with a hole every other pixel along it."""
    base = (138, 142, 146)
    img, px, _ = _canvas(20261809, base, 4)
    for y in range(1, 16, 4):
        for x in range(0, 16, 3):
            px[x, y] = _shift(base, -28)
    return img


def perforated():
    """Perforated steel angle, as hangers are cut from: a round hole every three pixels down the
    middle."""
    base = (160, 164, 168)
    img, px, _ = _canvas(20261813, base, 4)
    for y in range(1, 16, 3):
        for x in range(16):
            if x % 4 in (1, 2):
                px[x, y] = _shift(base, -36)
    return img


def spring():
    """A torsion spring: black oil-tempered wire, a coil every two pixels across the spring's
    length (u runs along it)."""
    base = (30, 30, 32)
    img, px, rng = _canvas(20261814, base, 3)
    for x in range(16):
        d = 30 if x % 2 == 0 else 0
        for y in range(16):
            px[x, y] = _shift(base, d + rng.uniform(-3, 3))
    return img


def shaft():
    return _canvas(20261815, (92, 96, 100), 4)[0]


def plate():
    """A bearing plate or bracket: galvanized, a bolt head in each corner."""
    base = (170, 174, 176)
    img, px, _ = _canvas(20261816, base, 3)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[x, y] = _shift(base, -60)
    return img


def cable():
    return _canvas(20261817, (70, 72, 74), 6)[0]


def chain():
    """A hand chain: links alternately face-on and edge-on down its length."""
    base = (96, 98, 102)
    img, px, _ = _canvas(20261818, base, 4)
    for y in range(16):
        if y % 4 == 3:
            for x in range(16):
                px[x, y] = _shift(base, -50)
        elif y % 4 == 1:
            for x in range(16):
                px[x, y] = _shift(base, 30)
    return img


def motor():
    """The opener's housing: dark grey plastic with a moulded line every four pixels."""
    base = (64, 62, 60)
    img, px, _ = _canvas(20261819, base, 3)
    for x in range(16):
        px[x, 7] = _shift(base, -18)
        px[x, 8] = _shift(base, 14)
    return img


def lens():
    """The opener's light lens: milky white, with a faint prism grid."""
    base = (236, 232, 218)
    img, px, _ = _canvas(20261820, base, 3)
    for y in range(0, 16, 4):
        for x in range(16):
            px[x, y] = _shift(base, -14)
    return img


def rail():
    """The opener's T-rail: bright rolled steel, a seam along the middle."""
    base = (178, 182, 186)
    img, px, _ = _canvas(20261821, base, 3)
    for x in range(16):
        px[x, 8] = _shift(base, -30)
    return img


def hood():
    """The coil hood: galvanized sheet with a seam every four pixels."""
    base = (184, 188, 190)
    img, px, rng = _canvas(20261810, base, 3)
    for y in range(0, 16, 4):
        for x in range(16):
            px[x, y] = _shift(base, -20)
    return img


def seal():
    return _canvas(20261811, (34, 34, 36), 3)[0]


def bar():
    return _canvas(20261812, (150, 152, 156), 3)[0]


def control_plate():
    """The push button's wall plate: white plastic."""
    return _canvas(20261822, (232, 230, 224), 2)[0]


def control_lamp():
    """The push button itself, lit from inside: a warm amber, brightest in the middle."""
    img, px, _ = _canvas(20261823, (255, 196, 96), 3)
    for y in range(16):
        for x in range(16):
            d = -18 * (abs(x - 7.5) + abs(y - 7.5)) / 15
            px[x, y] = _shift((255, 206, 110), d)
    return img


def control_housing():
    """The control station's enclosure: dark grey, powder coated."""
    return _canvas(20261824, (70, 72, 76), 3)[0]


def control_colour(rgb, seed):
    return _canvas(seed, rgb, 4)[0]


def keypad_face():
    """A keypad's face, drawn over the whole texture and mapped whole onto the keypad's front (6 x
    9 px, so it is squeezed across more than down). Drawn at 32 px, because at 16 there is no room
    for the margin that keeps the keys off the bezel on all four sides: a bezel, a lit display
    across the top and twelve backlit keys, three by four, each 6 x 4 so it comes out square."""
    import random as _random
    rng = _random.Random(20261827)
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    for y in range(32):
        for x in range(32):
            if x in (0, 31) or y in (0, 31):
                px[x, y] = _shift((60, 58, 56), rng.uniform(-3, 3))
            else:
                px[x, y] = _shift((30, 30, 32), rng.uniform(-3, 3))
    for y in range(2, 6):
        for x in range(4, 28):
            px[x, y] = _shift((80, 168, 92), -12 if y == 5 else 0)
    for row in range(4):
        for col in range(3):
            for dx in range(6):
                for dy in range(4):
                    x, y = 4 + col * 9 + dx, 8 + row * 6 + dy
                    px[x, y] = _shift((218, 224, 218), -34 if dy == 3 else 0)
    return img


def keypad_body():
    """The keypad's cast housing: dark bronze."""
    return _canvas(20261828, (74, 60, 46), 3)[0]


def textures():
    return {
        "white_face": white_face(), "white_back": white_back(),
        "windowed_face_top": white_face(True), "windowed_back_top": white_back(True),
        "commercial_face": commercial_face(), "commercial_back": commercial_back(),
        "rollup_galvanized": rollup_curtain(), "grille_mesh": grille_mesh(),
        "garage_track": track(), "garage_hood": hood(), "garage_seal": seal(),
        "garage_bar": bar(), "garage_perforated": perforated(), "garage_spring": spring(),
        "garage_shaft": shaft(), "garage_plate": plate(), "garage_cable": cable(),
        "garage_chain": chain(), "garage_motor": motor(), "garage_lens": lens(),
        "garage_rail": rail(),
        "control_plate": control_plate(), "control_lamp": control_lamp(),
        "control_housing": control_housing(),
        "control_open": control_colour((60, 150, 70), 20261825),
        "control_close": control_colour((230, 230, 226), 20261829),
        "control_stop": control_colour((196, 40, 36), 20261826),
        "keypad_face": keypad_face(), "keypad_body": keypad_body(),
    }


def _t(name):
    return TEX_REF % name


# Which texture each sectional style's panels wear: (face, face_top, back, back_top).
SECTIONAL_TEX = {
    "white": ("white_face", "white_face", "white_back", "white_back"),
    "windowed": ("white_face", "windowed_face_top", "white_back", "windowed_back_top"),
    "commercial": ("commercial_face", "commercial_face", "commercial_back", "commercial_back"),
}
CURTAIN_TEX = {"galvanized": "rollup_galvanized", "grille": "grille_mesh"}

# --------------------------------------------------------------------------------------------
# JSON part models -- inside to the north
# --------------------------------------------------------------------------------------------

box = sc._box
retex = gen_logistics.retex
PZ0, PZ1 = (PLANE_Z - HALF) * 16, (PLANE_Z + HALF) * 16   # -2.5 .. -0.5
TZ0, TZ1 = (PLANE_Z - TRACK_HALF) * 16, (PLANE_Z + TRACK_HALF) * 16  # -3 .. 0


def panel():
    el = box(0, 0, PZ0, 16, 16, PZ1, "#back", faces=("north", "south", "up", "down"))
    return [retex(el, {"south": "#face"})]


def panel_lip(x0, y0, x1, y1):
    """A strip of the panel past the opening -- into a track at a side, over the wall at the top
    -- so a closed door covers the gap where it meets the wall."""
    el = box(x0, y0, PZ0, x1, y1, PZ1, "#back", faces=("north", "south", "up", "down", "east",
                                                          "west"))
    return [retex(el, {"south": "#face"})]


def seal_el():
    return [box(0, 0, TZ0 + 0.25, 16, 0.5, TZ1 - 0.25, "#seal")]


def track_el(cw):
    x0, x1 = (16, 16 + TRACK_W * 16) if cw else (-TRACK_W * 16, 0)
    return [box(x0, 0, TZ0, x1, 16, TZ1, "#track")]


def curtain():
    return [box(0, 0, -1, 16, 16, -0.5, "#curtain", faces=("north", "south"))]


def curtain_lip(cw):
    x0, x1 = (16, 17) if cw else (-1, 0)
    return [box(x0, 0, -1, x1, 16, -0.5, "#curtain", faces=("north", "south"))]


def bottom_bar():
    return [box(0, 0, -1.75, 16, 1, -0.25, "#bar")]


def guide(cw):
    x0, x1 = (16, 17.25) if cw else (-1.25, 0)
    return [box(x0, 0, -2, x1, 16, 0, "#track")]


def hood_el():
    """The coil hood, on the wall above the opening's inside face."""
    return [box(0, 16, -8, 16, 26, 0, "#hood", faces=("north", "up", "down", "east", "west"))]


# The sectional door's torsion spring assembly, on the wall above the opening's inside face: a
# shaft across the whole door (on every block of the top course), with a cable drum and a bearing
# plate at each end of it and a spring beside each drum, anchored to a centre bracket. Drawn on
# the top course, above its cell -- the wall over the opening is where a real one is fixed. The
# heights clear the bend of the panels' track (whose top reaches y 23 at the ceiling run).
SHAFT_Y, SHAFT_Z = 26.0, -4.5


def _octagon(x0, x1, yc, zc, r, tex):
    """A short cylinder along x, as a box and the same box turned 45 degrees."""
    a = box(x0, yc - r, zc - r, x1, yc + r, zc + r, tex)
    b = box(x0, yc - r, zc - r, x1, yc + r, zc + r, tex)
    b["rotation"] = {"origin": [(x0 + x1) / 2, yc, zc], "axis": "x", "angle": 45}
    return [a, b]


def torsion_shaft():
    return [box(0, SHAFT_Y - 0.5, SHAFT_Z - 0.5, 16, SHAFT_Y + 0.5, SHAFT_Z + 0.5, "#shaft")]


def torsion_end():
    """The ccw end: the bearing plate on the wall, the cable drum, the spring and its anchor
    bracket."""
    # The bearing plate stands out from the wall beside the opening, over the track.
    els = [box(-2, SHAFT_Y - 4, SHAFT_Z - 1.5, -1.5, SHAFT_Y + 3, 0, "#plate")]
    els += [box(-1.5, SHAFT_Y - 0.5, SHAFT_Z - 0.5, 0, SHAFT_Y + 0.5, SHAFT_Z + 0.5, "#shaft")]
    # The drum over the door's edge, so its cable falls straight down the door's back.
    els += _octagon(-0.75, 0.75, SHAFT_Y, SHAFT_Z, 1.75, "#drum")
    els += [box(2, SHAFT_Y - 1.5, SHAFT_Z - 1.5, 11, SHAFT_Y + 1.5, SHAFT_Z + 1.5, "#spring")]
    els += [box(11, SHAFT_Y - 4, SHAFT_Z - 1.5, 11.5, SHAFT_Y + 3, 0, "#plate")]
    els += _octagon(10.5, 11.5, SHAFT_Y, SHAFT_Z, 2, "#plate")
    return els


def lift_cable(top):
    """The lift cable, from the bottom bracket up the door's back to the drum."""
    z1 = SHAFT_Z + 1.75
    return [box(-0.15, 0, z1 - 0.3, 0.15, SHAFT_Y if top else 16, z1, "#cable")]


def bottom_bracket():
    return [box(0, 0.5, PZ0 - 0.75, 2, 3, PZ0, "#plate")]


def roller_bracket():
    """The hinge bracket carrying a roller into the track, at each section joint."""
    return [box(0, 7, PZ0 - 0.75, 2, 9, PZ0, "#plate"),
            box(0, 15, PZ0 - 0.75, 2, 16, PZ0, "#plate")]


# The roll-up's hood end plates and its hand chain, on the cw side where the chain wheel is.
def hood_end():
    return [box(-1.75, 15, -9, -1.25, 27, 0, "#plate",
                faces=("north", "west", "east", "up", "down"))]


def chain_wheel():
    return _octagon(17.75, 18.75, 21, -4, 3, "#drum")


def hand_chain(top):
    y1 = 21 if top else 16
    return [box(18, 0, -7.25, 18.5, y1, -6.75, "#chain"),
            box(18, 0, -1.25, 18.5, y1, -0.75, "#chain")]


mirror_x = sc._mirror_x


# The hanger, drawn at the NORTH edge of its cell (the blockstate turns it to the others): a
# perforated angle down the edge, bolted to the inner side of a ceiling track that runs just
# outside the edge (a door's tracks are outboard of its opening), a cleat along the ceiling where
# the ceiling is, and a foot at the height of a sectional door's ceiling run.
FOOT_Y = 4.5


def hanger_strap(y0, y1, centre):
    if centre:
        return [box(7.25, y0, 7.25, 8.75, y1, 8, "#strap"), box(7.25, y0, 8, 8, y1, 8.75, "#strap")]
    return [box(6.75, y0, 0, 9.25, y1, 0.5, "#strap"), box(6.75, y0, 0.5, 7.5, y1, 2, "#strap")]


def hanger_cleat(centre):
    if centre:
        return [box(2, 15, 7.25, 14, 16, 8.75, "#strap")]
    return [box(7, 15, 0, 9, 16, 8, "#strap")]


def hanger_foot(centre):
    if centre:
        return [box(6, FOOT_Y, 6, 10, FOOT_Y + 1, 10, "#strap")]
    return [box(6, FOOT_Y, 0, 10, FOOT_Y + 1.5, 0.5, "#strap")]


# The opener, drawn facing north (the door to the north): the motor hung under its straps, the
# rail running to the wall over the door (an OBJ per length, since it can reach ten blocks), the
# header bracket at the wall end.
MOTOR = (3, 5, 3, 13, 11, 15)
RAIL_Y = (11.0, 12.5)


def opener_motor():
    x0, y0, z0, x1, y1, z1 = MOTOR
    return [box(x0, y0, z0, x1, y1, z1, "#motor"),
            box(x0 + 1, y0 - 1, z0 + 1, x1 - 1, y0, z1 - 1, "#lens"),
            box(7.25, RAIL_Y[0], 1, 8.75, RAIL_Y[1], z0, "#rail"),
            box(7.5, y1, 8, 8.5, 16, 9, "#strap")]


def opener_cleat():
    return [box(2, 15, 8, 14, 16, 9, "#strap")]


# The wall controls, drawn with the front to the north and the wall behind them to the south.
def control_button():
    return [box(5, 5, 15, 11, 11, 16, "#plate"),
            box(6.5, 6.5, 14.25, 9.5, 9.5, 15, "#lamp")]


def control_station():
    """The OPEN / CLOSE / STOP station: two buttons over a larger stop button, top to bottom as
    BlockGarageDoorControl reads the click."""
    return [box(5.5, 3, 14.5, 10.5, 13, 16, "#housing"),
            box(7, 10, 14, 9, 12, 14.5, "#open"),
            box(7, 7, 14, 9, 9, 14.5, "#close"),
            box(6.5, 3.75, 13.75, 9.5, 6.25, 14.5, "#stop")]


def control_keypad():
    body = box(5, 3.5, 15, 11, 12.5, 16, "#body")
    # The face texture is mapped whole onto the front rather than by position: a 6 x 9 px window
    # of a 16 px drawing leaves no room for a margin, and cut the edge keys off.
    body["faces"]["north"] = {"texture": "#face", "uv": [0, 0, 16, 16]}
    # A little rain hood over the keys.
    return [body, box(5, 12.5, 14, 11, 13, 16, "#body")]


def _model(elements, textures, parent=None):
    t = dict(textures)
    t["particle"] = next(iter(textures.values()))
    m = {"textures": t, "elements": elements}
    if parent:
        m = {"parent": parent, **m}
    return m


def _inventory(front, back, sides):
    """A closed door block as an item, its street face toward the viewer: the GUI shows a model's
    north face, so this one is the door turned about."""
    els = [retex(box(0, 0, 7, 16, 16, 9, "#back", faces=("north", "south", "up", "down")),
                 {"north": "#face"})]
    els += [box(0, 0, 6.5, 1, 16, 9.5, "#track"), box(15, 0, 6.5, 16, 16, 9.5, "#track")]
    return {"parent": "block/block",
            "textures": {"particle": front, "face": front, "back": back, "track": sides},
            "elements": els}


def models():
    out = {
        "track_ccw": _model(track_el(False), {"track": _t("garage_track")}),
        "track_cw": _model(track_el(True), {"track": _t("garage_track")}),
        "seal": _model(seal_el(), {"seal": _t("garage_seal")}),
        "guide_ccw": _model(guide(False), {"track": _t("garage_track")}),
        "guide_cw": _model(guide(True), {"track": _t("garage_track")}),
        "hood": _model(hood_el(), {"hood": _t("garage_hood")}),
    }
    hw = {"shaft": _t("garage_shaft"), "plate": _t("garage_plate"), "drum": _t("garage_shaft"),
          "spring": _t("garage_spring"), "cable": _t("garage_cable"),
          "chain": _t("garage_chain")}
    parts = {
        "torsion_shaft": torsion_shaft(),
        "torsion_ccw": torsion_end(), "torsion_cw": mirror_x(torsion_end()),
        "cable_ccw": lift_cable(False), "cable_cw": mirror_x(lift_cable(False)),
        "cable_top_ccw": lift_cable(True), "cable_top_cw": mirror_x(lift_cable(True)),
        "bottom_bracket_ccw": bottom_bracket(), "bottom_bracket_cw": mirror_x(bottom_bracket()),
        "rollers_ccw": roller_bracket(), "rollers_cw": mirror_x(roller_bracket()),
        "hood_end_ccw": hood_end(), "hood_end_cw": mirror_x(hood_end()),
        "chain_wheel": chain_wheel(),
        "hand_chain": hand_chain(False), "hand_chain_top": hand_chain(True),
    }
    for part, els in parts.items():
        used = {f["texture"][1:] for e in els for f in e["faces"].values()}
        out[part] = _model(els, {k: v for k, v in hw.items() if k in used})
    strap = {"strap": _t("garage_perforated")}
    for kind in ("edge", "centre"):
        c = kind == "centre"
        out["hanger_%s" % kind] = _model(hanger_strap(FOOT_Y, 16, c), strap)
        out["hanger_%s_low" % kind] = _model(hanger_strap(0, FOOT_Y, c), strap)
        out["hanger_%s_cleat" % kind] = _model(hanger_cleat(c), strap)
        out["hanger_%s_foot" % kind] = _model(hanger_foot(c), strap)
    out["garage_door_hanger_inventory"] = {
        "parent": "block/block", "textures": {"particle": strap["strap"], **strap},
        "elements": hanger_strap(0, 16, True) + hanger_cleat(True) + hanger_foot(True)}
    op = {"motor": _t("garage_motor"), "lens": _t("garage_lens"), "rail": _t("garage_rail"),
          "strap": _t("garage_perforated")}
    out["garage_door_button"] = _model(control_button(), {
        "plate": _t("control_plate"), "lamp": _t("control_lamp")})
    out["garage_door_station"] = _model(control_station(), {
        "housing": _t("control_housing"), "open": _t("control_open"),
        "close": _t("control_close"), "stop": _t("control_stop")})
    out["garage_door_keypad"] = _model(control_keypad(), {
        "body": _t("keypad_body"), "face": _t("keypad_face")})
    out["opener_motor"] = _model(opener_motor(), op)
    out["opener_cleat"] = _model(opener_cleat(), strap)
    out["garage_door_opener_inventory"] = {
        "parent": "block/block", "textures": {"particle": op["motor"], **op},
        "elements": opener_motor()}
    for name, (kind, style, _) in BLOCKS.items():
        if kind == "sectional":
            face, face_top, back, back_top = SECTIONAL_TEX[style]
            out[name + "_panel"] = _model(panel(), {"face": _t(face), "back": _t(back)})
            out[name + "_panel_top"] = _model(panel(), {"face": _t(face_top),
                                                        "back": _t(back_top)})
            plain = {"face": _t(face), "back": _t(back)}
            top = {"face": _t(face_top), "back": _t(back_top)}
            out[name + "_lip_ccw"] = _model(panel_lip(-1, 0, 0, 16), plain)
            out[name + "_lip_cw"] = _model(panel_lip(16, 0, 17, 16), plain)
            out[name + "_lip_ccw_top"] = _model(panel_lip(-1, 0, 0, 17), top)
            out[name + "_lip_cw_top"] = _model(panel_lip(16, 0, 17, 17), top)
            out[name + "_lip_top"] = _model(panel_lip(0, 16, 16, 17), top)
            out[name + "_inventory"] = _inventory(_t(face_top), _t(back_top),
                                                  _t("garage_track"))
        else:
            tex = {"curtain": _t(CURTAIN_TEX[style])}
            out[name + "_curtain"] = _model(curtain(), tex)
            out[name + "_lip_ccw"] = _model(curtain_lip(False), tex)
            out[name + "_lip_cw"] = _model(curtain_lip(True), tex)
            out[name + "_bar"] = _model(bottom_bar(), {"bar": _t("garage_bar")})
            inv = _inventory(_t(CURTAIN_TEX[style]), _t(CURTAIN_TEX[style]), _t("garage_track"))
            out[name + "_inventory"] = inv
    return out

# --------------------------------------------------------------------------------------------
# OBJ models: the sectional door's overhead track and its open panels, one per depth
# --------------------------------------------------------------------------------------------


def track_path(depth):
    """The track's centre line in the (y, z) plane of the top course's cell: up out of the top of
    the cell, round the bend, and back along the ceiling a little past the open panels."""
    pts = []
    steps = 6
    for i in range(steps + 1):
        a = math.pi / 2 * i / steps
        pts.append((1 + BEND * math.sin(a), PLANE_Z - BEND * (1 - math.cos(a))))
    y, z = pts[-1]
    # Half a block past the panels, where a hanger holds its end up.
    pts.append((y, z - depth - 0.5))
    return pts


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


class Obj:
    """Quads in 0..1 block space, written as an OBJ with its materials in a shared MTL."""

    def __init__(self, mtl):
        self.mtl = mtl
        self.v = []
        self.vt = []
        self.faces = []  # (material, [(vi, ti)])

    def quad(self, material, pts, uvs, outward):
        """One quad, wound so its front faces {outward}."""
        a, b, c = pts[0], pts[1], pts[2]
        n = _cross(tuple(b[i] - a[i] for i in range(3)), tuple(c[i] - a[i] for i in range(3)))
        if sum(n[i] * outward[i] for i in range(3)) < 0:
            pts = [pts[0], pts[3], pts[2], pts[1]]
            uvs = [uvs[0], uvs[3], uvs[2], uvs[1]]
        idx = []
        for p, t in zip(pts, uvs):
            assert all(-1e-9 <= c <= 1 + 1e-9 for c in t), t
            self.v.append(p)
            self.vt.append(t)
            idx.append((len(self.v), len(self.vt)))
        self.faces.append((material, idx))

    def text(self, name):
        lines = ["# Generated by dev-env-utils/scripts/gen_garage_doors.py -- do not edit",
                 "mtllib %s.mtl" % self.mtl, "o %s" % name]
        lines += ["v %.5f %.5f %.5f" % p for p in self.v]
        lines += ["vt %.5f %.5f" % t for t in self.vt]
        current = None
        for material, idx in self.faces:
            if material != current:
                lines.append("usemtl " + material)
                current = material
            lines.append("f " + " ".join("%d/%d" % i for i in idx))
        return "\n".join(lines) + "\n"


def track_obj(depth, cw):
    """The bend and the ceiling run of one side's track: a channel swept along the path."""
    o = Obj("garage_track")
    x0, x1 = (1.0, 1 + TRACK_W) if cw else (-TRACK_W, 0.0)
    pts = track_path(depth)
    normals = []
    for i in range(len(pts)):
        a = pts[max(0, i - 1)]
        b = pts[min(len(pts) - 1, i + 1)]
        ty, tz = b[0] - a[0], b[1] - a[1]
        ln = math.hypot(ty, tz)
        # The side the street was on before the bend, SHARED with the renderer: N = (-tz, ty).
        normals.append((-tz / ln, ty / ln))
    uv = [(0, 0), (1 / 16, 0), (1 / 16, 1), (0, 1)]
    for i in range(len(pts) - 1):
        (ya, za), (yb, zb) = pts[i], pts[i + 1]
        na, nb = normals[i], normals[i + 1]
        for sgn in (1, -1):
            pa = (ya + sgn * na[0] * TRACK_HALF, za + sgn * na[1] * TRACK_HALF)
            pb = (yb + sgn * nb[0] * TRACK_HALF, zb + sgn * nb[1] * TRACK_HALF)
            quad = [(x0, pa[0], pa[1]), (x1, pa[0], pa[1]), (x1, pb[0], pb[1]),
                    (x0, pb[0], pb[1])]
            nm = ((na[0] + nb[0]) / 2 * sgn, (na[1] + nb[1]) / 2 * sgn)
            o.quad("track", quad, uv, (0, nm[0], nm[1]))
        for x, out in ((x0, -1), (x1, 1)):
            pa_o = (ya + na[0] * TRACK_HALF, za + na[1] * TRACK_HALF)
            pa_i = (ya - na[0] * TRACK_HALF, za - na[1] * TRACK_HALF)
            pb_o = (yb + nb[0] * TRACK_HALF, zb + nb[1] * TRACK_HALF)
            pb_i = (yb - nb[0] * TRACK_HALF, zb - nb[1] * TRACK_HALF)
            quad = [(x, pa_o[0], pa_o[1]), (x, pa_i[0], pa_i[1]), (x, pb_i[0], pb_i[1]),
                    (x, pb_o[0], pb_o[1])]
            o.quad("track", quad, uv, (out, 0, 0))
    # The end cap at the back.
    (y, z), n = pts[-1], normals[-1]
    cap = [(x0, y + n[0] * TRACK_HALF, z + n[1] * TRACK_HALF),
           (x1, y + n[0] * TRACK_HALF, z + n[1] * TRACK_HALF),
           (x1, y - n[0] * TRACK_HALF, z - n[1] * TRACK_HALF),
           (x0, y - n[0] * TRACK_HALF, z - n[1] * TRACK_HALF)]
    o.quad("track", cap, uv, (0, 0, -1))
    return o


def run_obj(style, depth):
    """An open sectional door's panels, lying along the ceiling behind its top course: the door's
    row j (0 at the bottom) runs from j to j + 1 blocks behind the end of the bend, its street face
    up. Texture: u across the door as on the closed street face (u = x), the back face mirrored
    (u = 1 - x), v = 1 at a row's bottom edge (nearest the opening) and 0 at its top edge -- the
    same mapping the renderer uses on the move."""
    o = Obj("%s_run" % style)
    y_mid = 1 + BEND
    z0 = PLANE_Z - BEND  # where the bend ends
    for j in range(depth):
        top = j == depth - 1
        near, far = z0 - j, z0 - j - 1
        yt, yb = y_mid + HALF, y_mid - HALF
        face = "face_top" if top else "face"
        back = "back_top" if top else "back"
        o.quad(face, [(0, yt, near), (1, yt, near), (1, yt, far), (0, yt, far)],
               [(0, 1), (1, 1), (1, 0), (0, 0)], (0, 1, 0))
        o.quad(back, [(0, yb, near), (1, yb, near), (1, yb, far), (0, yb, far)],
               [(1, 1), (0, 1), (0, 0), (1, 0)], (0, -1, 0))
    # The two ends: the bottom section's bottom edge nearest the opening, the top one at the back.
    edge = [(0, 0.9375), (1, 0.9375), (1, 1), (0, 1)]
    far = z0 - depth
    for z, out in ((z0, 1), (far, -1)):
        o.quad("back" if out > 0 else "back_top",
               [(0, y_mid - HALF, z), (1, y_mid - HALF, z), (1, y_mid + HALF, z),
                (0, y_mid + HALF, z)], edge, (0, 0, out))
    return o


def rail_obj(length):
    """The opener's rail from the motor's front to the wall over the door, {length} blocks of air
    away, and the header bracket at that end."""
    o = Obj("garage_rail")
    x0, x1 = 7.25 / 16, 8.75 / 16
    y0, y1 = RAIL_Y[0] / 16, RAIL_Y[1] / 16
    z_near, z_far = 1 / 16, -float(length)
    uv = [(0, 0), (1, 0), (1, 1), (0, 1)]
    o.quad("rail", [(x0, y1, z_near), (x1, y1, z_near), (x1, y1, z_far), (x0, y1, z_far)], uv,
           (0, 1, 0))
    o.quad("rail", [(x0, y0, z_near), (x1, y0, z_near), (x1, y0, z_far), (x0, y0, z_far)], uv,
           (0, -1, 0))
    o.quad("rail", [(x0, y0, z_near), (x0, y1, z_near), (x0, y1, z_far), (x0, y0, z_far)], uv,
           (-1, 0, 0))
    o.quad("rail", [(x1, y0, z_near), (x1, y1, z_near), (x1, y1, z_far), (x1, y0, z_far)], uv,
           (1, 0, 0))
    # The header bracket: a plate on the wall with a lip under the rail's end.
    bx0, bx1, by0, by1 = 6 / 16, 10 / 16, 9.5 / 16, 14 / 16
    zb = z_far + 1 / 16
    o.quad("plate", [(bx0, by0, zb), (bx1, by0, zb), (bx1, by1, zb), (bx0, by1, zb)], uv,
           (0, 0, 1))
    for x, out in ((bx0, -1), (bx1, 1)):
        o.quad("plate", [(x, by0, z_far), (x, by1, z_far), (x, by1, zb), (x, by0, zb)],
               [(0, 0), (1 / 16, 0), (1 / 16, 1), (0, 1)], (out, 0, 0))
    for y, out in ((by0, -1), (by1, 1)):
        o.quad("plate", [(bx0, y, z_far), (bx1, y, z_far), (bx1, y, zb), (bx0, y, zb)],
               [(0, 0), (1, 0), (1, 1 / 16), (0, 1 / 16)], (0, out, 0))
    return o


def mtl(materials):
    lines = ["# Generated by dev-env-utils/scripts/gen_garage_doors.py -- do not edit"]
    for name, tex in materials:
        lines += ["newmtl " + name, "map_Kd " + _t(tex)]
    return "\n".join(lines) + "\n"


def obj_files():
    """{filename: text} for every OBJ and MTL."""
    out = {"garage_track.mtl": mtl([("track", "garage_track")]),
           "garage_rail.mtl": mtl([("rail", "garage_rail"), ("plate", "garage_plate")])}
    for n in range(0, MAX_RAIL + 1):
        out["opener_rail_%d.obj" % n] = rail_obj(n).text("rail")
    for d in range(1, MAX_DEPTH + 1):
        out["sectional_track_ccw_%d.obj" % d] = track_obj(d, False).text("track")
        out["sectional_track_cw_%d.obj" % d] = track_obj(d, True).text("track")
    for style, (face, face_top, back, back_top) in SECTIONAL_TEX.items():
        out["%s_run.mtl" % style] = mtl([("face", face), ("face_top", face_top),
                                         ("back", back), ("back_top", back_top)])
        for d in range(1, MAX_DEPTH + 1):
            out["%s_run_%d.obj" % (style, d)] = run_obj(style, d).text("run")
    return out

# --------------------------------------------------------------------------------------------
# Blockstates
# --------------------------------------------------------------------------------------------

SIDES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def _parts(conds):
    """One multipart part per facing for each (when, model): the model is turned to the facing."""
    parts = []
    for when, model in conds:
        for side, rot in SIDES:
            w = dict(when)
            w["facing"] = side
            a = {"model": MODEL_REF % model}
            if rot:
                a["y"] = rot
            parts.append({"when": w, "apply": a})
    return parts


def state_for(name):
    kind, style, _ = BLOCKS[name]
    if kind == "sectional":
        conds = [({"ccw": "false"}, "track_ccw"), ({"cw": "false"}, "track_cw")]
        for d in range(1, MAX_DEPTH + 1):
            conds += [({"up": "false", "ccw": "false", "depth": str(d)},
                       "sectional_track_ccw_%d.obj" % d),
                      ({"up": "false", "cw": "false", "depth": str(d)},
                       "sectional_track_cw_%d.obj" % d)]
        conds += [({"up": "false"}, "torsion_shaft"),
                  ({"up": "false", "ccw": "false"}, "torsion_ccw"),
                  ({"up": "false", "cw": "false"}, "torsion_cw")]
        for side in ("ccw", "cw"):
            conds += [({"motion": "closed", side: "false", "up": "true"}, "cable_" + side),
                      ({"motion": "closed", side: "false", "up": "false"}, "cable_top_" + side),
                      ({"motion": "closed", side: "false", "down": "false"},
                       "bottom_bracket_" + side),
                      ({"motion": "closed", side: "false"}, "rollers_" + side)]
        conds += [({"motion": "closed", "up": "true"}, name + "_panel"),
                  ({"motion": "closed", "up": "false"}, name + "_panel_top"),
                  ({"motion": "closed", "down": "false"}, "seal"),
                  ({"motion": "closed", "ccw": "false", "up": "true"}, name + "_lip_ccw"),
                  ({"motion": "closed", "ccw": "false", "up": "false"}, name + "_lip_ccw_top"),
                  ({"motion": "closed", "cw": "false", "up": "true"}, name + "_lip_cw"),
                  ({"motion": "closed", "cw": "false", "up": "false"}, name + "_lip_cw_top"),
                  ({"motion": "closed", "up": "false"}, name + "_lip_top")]
        for d in range(1, MAX_DEPTH + 1):
            conds.append(({"motion": "open", "up": "false", "depth": str(d)},
                          "%s_run_%d.obj" % (style, d)))
    else:
        conds = [({"ccw": "false"}, "guide_ccw"), ({"cw": "false"}, "guide_cw"),
                 ({"up": "false"}, "hood"),
                 ({"up": "false", "ccw": "false"}, "hood_end_ccw"),
                 ({"up": "false", "cw": "false"}, "hood_end_cw"),
                 ({"up": "false", "cw": "false"}, "chain_wheel"),
                 ({"up": "true", "cw": "false"}, "hand_chain"),
                 ({"up": "false", "cw": "false"}, "hand_chain_top"),
                 ({"motion": "closed"}, name + "_curtain"),
                 ({"motion": "closed", "ccw": "false"}, name + "_lip_ccw"),
                 ({"motion": "closed", "cw": "false"}, name + "_lip_cw"),
                 ({"motion": "closed", "down": "false"}, name + "_bar")]
    return {"variants": {"inventory": {"model": MODEL_REF % (name + "_inventory")}},
            "multipart": _parts(conds)}


EDGES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def hanger_state():
    """Five positions: the middle of the cell (over an opener), or against one edge (beside a
    track). The strap runs the full height unless nothing hangs below, where it ends in a foot at
    the height of a ceiling track; a cleat is drawn where it meets the ceiling."""
    parts = []

    def add(when, model, rot=0):
        a = {"model": MODEL_REF % model}
        if rot:
            a["y"] = rot
        parts.append({"when": when, "apply": a})

    add({"position": "center"}, "hanger_centre")
    add({"position": "center", "bottom": "false"}, "hanger_centre_low")
    add({"position": "center", "bottom": "true"}, "hanger_centre_foot")
    add({"position": "center", "top": "true"}, "hanger_centre_cleat")
    for edge, rot in EDGES:
        add({"position": edge}, "hanger_edge", rot)
        add({"position": edge, "bottom": "false"}, "hanger_edge_low", rot)
        add({"position": edge, "bottom": "true"}, "hanger_edge_foot", rot)
        add({"position": edge, "top": "true"}, "hanger_edge_cleat", rot)
    return {"variants": {"inventory": {"model": MODEL_REF % "garage_door_hanger_inventory"}},
            "multipart": parts}


def opener_state():
    conds = [({}, "opener_motor"), ({"top": "true"}, "opener_cleat")]
    conds += [({"length": str(n)}, "opener_rail_%d.obj" % n) for n in range(MAX_RAIL + 1)]
    return {"variants": {"inventory": {"model": MODEL_REF % "garage_door_opener_inventory"}},
            "multipart": _parts(conds)}


def blockstates():
    out = {name: state_for(name) for name in BLOCKS}
    out["garage_door_hanger"] = hanger_state()
    out["garage_door_opener"] = opener_state()
    for name in CONTROLS:
        variants = {"inventory": {"model": MODEL_REF % name}}
        for side, rot in SIDES:
            v = {"model": MODEL_REF % name}
            if rot:
                v["y"] = rot
            variants["facing=" + side] = v
        out[name] = {"variants": variants}
    return out

# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------


def write_all(tex_dir, model_dir, state_dir):
    written = []
    os.makedirs(tex_dir, exist_ok=True)
    os.makedirs(model_dir, exist_ok=True)
    for name, img in sorted(textures().items()):
        img.save(os.path.join(tex_dir, name + ".png"))
        written.append(("tex", name + ".png"))
    for name, body in sorted(models().items()):
        gen_cmu._write_json(os.path.join(model_dir, name + ".json"), body)
        written.append(("model", name + ".json"))
    for name, text in sorted(obj_files().items()):
        with open(os.path.join(model_dir, name), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text)
        written.append(("model", name))
    for name, body in sorted(blockstates().items()):
        gen_cmu._write_json(os.path.join(state_dir, name + ".json"), body)
        written.append(("state", name + ".json"))
    return written


LANGS = gen_cmu.LANGS

# What the controls and the keypad screen say.
MESSAGES = {
    "gui.csm.garage.link_start": (
        "Now sneak-click the door, garage door or opener, with an empty hand",
        "Ahora pulsa agachado la puerta, la puerta de garaje o el abridor, con la mano vacía",
        "Jetzt mit leerer Hand geduckt auf die Tür, das Garagentor oder den Antrieb klicken",
        "Smyg-klicka nu på dörren, garageporten eller öppnaren, med tom hand"),
    "gui.csm.garage.link_done": (
        "Linked", "Vinculado", "Verbunden", "Kopplad"),
    "gui.csm.garage.not_linked": (
        "Not linked: sneak-click this, then a door",
        "Sin vincular: pulsa esto agachado y luego una puerta",
        "Nicht verbunden: dies geduckt anklicken, dann eine Tür",
        "Inte kopplad: smyg-klicka på denna och sedan på en dörr"),
    "gui.csm.door.locked": (
        "Locked: use the keypad", "Cerrada con llave: usa el teclado",
        "Verschlossen: das Codeschloss benutzen", "Låst: använd kodlåset"),
    "gui.csm.garage.not_owner": (
        "Only this keypad's owner can do that", "Solo el dueño de este teclado puede hacerlo",
        "Das kann nur der Besitzer dieses Codeschlosses", "Bara kodlåsets ägare kan göra det"),
    "gui.csm.garage.keypad_wrong": (
        "Wrong code", "Código incorrecto", "Falscher Code", "Fel kod"),
    "gui.csm.garage.keypad_locked": (
        "Too many wrong codes: try again shortly",
        "Demasiados códigos incorrectos: inténtalo en un momento",
        "Zu viele falsche Codes: gleich noch einmal versuchen",
        "För många fel koder: försök igen om en stund"),
    "gui.csm.garage.keypad_no_code": (
        "This keypad has no code set yet", "Este teclado aún no tiene código",
        "Für dieses Codeschloss ist noch kein Code gesetzt", "Kodlåset har ingen kod ännu"),
    "gui.csm.garage.keypad_code_set": (
        "Code set", "Código establecido", "Code gesetzt", "Koden är satt"),
    "gui.csm.garage.keypad": (
        "Keypad", "Teclado", "Codeschloss", "Kodlås"),
    "gui.csm.garage.keypad_clear": ("CLR", "BOR", "LÖS", "RAD"),
    "gui.csm.garage.keypad_enter": ("ENT", "OK", "OK", "OK"),
    "gui.csm.garage.keypad_set": (
        "Set Code", "Fijar Código", "Code Setzen", "Sätt Kod"),
    "gui.csm.garage.keypad_hint_set": (
        "No code yet: type 4 to 6 digits and press Set Code",
        "Sin código: escribe de 4 a 6 dígitos y pulsa Fijar Código",
        "Noch kein Code: 4 bis 6 Ziffern eingeben und Code Setzen drücken",
        "Ingen kod än: skriv 4 till 6 siffror och tryck Sätt Kod"),
    "gui.csm.garage.keypad_hint_owner": (
        "You own this keypad: Set Code changes the code",
        "Este teclado es tuyo: Fijar Código cambia el código",
        "Dieses Codeschloss gehört dir: Code Setzen ändert den Code",
        "Du äger kodlåset: Sätt Kod ändrar koden"),
}


def lang_entries():
    out = [("tile.%s.name" % name, dict(zip(LANGS, names)))
           for name, (_, _, names) in BLOCKS.items()]
    out += [("tile.%s.name" % name, dict(zip(LANGS, names))) for name, names in FITTINGS.items()]
    out += [("tile.%s.name" % name, dict(zip(LANGS, names))) for name, names in CONTROLS.items()]
    out += [(key, dict(zip(LANGS, names))) for key, names in MESSAGES.items()]
    return out


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
        return gen_logistics.check("garage door", write_all, roots)
    written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR)
    print("Wrote %d garage door files" % len(written))
    return 0


if __name__ == "__main__":
    sys.exit(main())
