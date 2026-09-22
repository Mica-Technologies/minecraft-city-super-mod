#!/usr/bin/env python3
"""Every asset the construction site's facilities ship.

    python dev-env-utils/scripts/gen_facilities.py
    python dev-env-utils/scripts/gen_facilities.py --check
    python dev-env-utils/scripts/gen_facilities.py --fragments   # lang lines to paste

Two kinds of block:

- Build-to-size shells (BlockSiteShell): shipping containers and roll-off dumpsters in four
  colours each, and the job trailer's wall, window and door blocks. A shell block draws a wall
  only on a side whose neighbour is not part of the same object, and a frame rail only along an
  edge where both sides are outside, so any box of them reads as one container, dumpster or
  trailer. A container has doors on the side it faces; a dumpster is a container with the top left
  open, its walls drawn inside and out; a trailer's windows and door are blocks of their own,
  placed where you want them.
- Props with a front (BlockSiteFacingProp): the portable toilet, gang box and concrete washout.

Walls are zero-thickness planes set half a pixel in from the cell face, so the frame rails stand
proud of them; every face takes gen_scaffold's fitted, positional UVs, so a wall's texture runs
on unbroken across the blocks it is built from.
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
TEX_DIR = sc.TEX_DIR
MODEL_DIR = sc.MODEL_DIR
STATE_DIR = sc.STATE_DIR
TEX_REF = sc.TEX_REF
MODEL_REF = sc.MODEL_REF

# Container and dumpster colours: name -> (base colour, name in each language).
COLOURS = {
    "blue": ((38, 88, 150), ("Blue", "Azul", "Blau", "Blå")),
    "maroon": ((122, 40, 38), ("Maroon", "Granate", "Weinrot", "Vinröd")),
    "green": ((44, 104, 64), ("Green", "Verde", "Grün", "Grön")),
    "grey": ((118, 122, 126), ("Grey", "Gris", "Grau", "Grå")),
}
CONTAINER = ("Shipping Container", "Contenedor Marítimo", "Schiffscontainer", "Fraktcontainer")
DUMPSTER = ("Roll-Off Dumpster", "Contenedor de Escombros", "Abrollcontainer",
            "Lastväxlarflak")

BLOCKS = {}
for _c, (_rgb, _names) in COLOURS.items():
    BLOCKS["container_" + _c] = tuple("%s (%s)" % (a, b) for a, b in zip(CONTAINER, _names))
for _c, (_rgb, _names) in COLOURS.items():
    BLOCKS["dumpster_" + _c] = tuple("%s (%s)" % (a, b) for a, b in zip(DUMPSTER, _names))
BLOCKS.update({
    "job_trailer_wall": ("Job Trailer Wall", "Oficina de Obra (Pared)", "Baucontainer (Wand)",
                         "Bodvagn (Vägg)"),
    "job_trailer_window": ("Job Trailer Window", "Oficina de Obra (Ventana)",
                           "Baucontainer (Fenster)", "Bodvagn (Fönster)"),
    "portable_toilet": ("Portable Toilet", "Baño Portátil", "Mobile Toilette", "Bajamaja"),
    "gang_box": ("Gang Box", "Caja de Herramientas de Obra", "Baustellenbox", "Verktygsbox"),
    "concrete_washout": ("Concrete Washout", "Lavadero de Hormigón", "Betonauswaschstation",
                         "Betongtvätt"),
})

# The facing props' boxes, front to the north, in sixteenths -- the Java registration uses these.
BOXES = {
    "portable_toilet": (0.25, 0, 0.25, 15.75, 16, 15.75),   # its lower half; see BlockPortableToilet
    "gang_box": (0, 0, 2.5, 16, 10.5, 13.5),
    "concrete_washout": (0, 0, 0, 16, 5, 16),
}


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

_shift = gen_logistics._shift
_canvas = gen_logistics._canvas


def container_wall(rgb, seed):
    """Corrugated container steel: trapezoidal ribs every four pixels."""
    img, px, rng = _canvas(seed, rgb, 4)
    rib = {0: -26, 1: -8, 2: 14, 3: -8}
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(rgb, rib[x % 4] + rng.uniform(-4, 4))
    # No rust flecks: on a wall built of many blocks every fleck repeats, and reads as polka dots.
    return img


def container_door(rgb, seed):
    """A door leaf: flat panel and two locking bars. Nothing runs across it: a container is built
    two to four blocks high, and a line across every block of a door reads as seams; the frame
    rails close its top and bottom."""
    img, px, rng = _canvas(seed, rgb, 4)
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(rgb, 6 + rng.uniform(-3, 3))
    for x in (3, 12):
        for y in range(16):
            px[x, y] = _shift((176, 180, 182), rng.uniform(-8, 8))
            px[x + 1, y] = _shift((110, 114, 116), rng.uniform(-6, 6))
    return img


def container_roof(rgb, seed):
    """The roof: shallow dents across it, sun-faded."""
    img, px, rng = _canvas(seed, rgb, 4)
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(rgb, 16 + (-10 if y % 8 in (0, 1) else 0) + rng.uniform(-4, 4))
    return img


def frame(rgb, seed, d=-28):
    """Frame rails and corner posts: the base colour, darker."""
    return _canvas(seed, _shift(rgb, d), 4)[0]


def dumpster_wall(rgb, seed):
    """A roll-off's side: flat plate with a vertical stiffener every eight pixels."""
    img, px, rng = _canvas(seed, rgb, 5)
    for y in range(16):
        for x in range(16):
            d = {0: -24, 1: 16, 7: -12}.get(x % 8, 0)
            px[x, y] = _shift(rgb, d + rng.uniform(-4, 4))
    return img


def dumpster_inside(rgb, seed):
    """Inside a roll-off: the same paint, scuffed through to bare steel."""
    img, px, rng = _canvas(seed, _shift(rgb, -14), 6)
    for _ in range(14):
        px[rng.randrange(16), rng.randrange(16)] = _shift((96, 92, 88), rng.uniform(-10, 10))
    return img


def dumpster_floor():
    img, px, rng = _canvas(20261511, (86, 70, 58), 10)
    return img


def shell_floor():
    """The underside of a container or trailer: dark, greasy steel."""
    return _canvas(20261512, (46, 44, 42), 5)[0]


def trailer_wall():
    """Job trailer siding: white ribbed steel, ribs every four pixels."""
    img, px, rng = _canvas(20261513, (228, 228, 222), 3)
    for y in range(16):
        for x in range(16):
            if y % 4 == 0:
                px[x, y] = _shift((196, 196, 190), rng.uniform(-3, 3))
    return img


TRAILER_GLASS = (58, 80, 100)
TRAILER_GLASS_ALPHA = 120


def trailer_window(top=True, bottom=True):
    """A sliding window: a grey frame, two panes of tinted glass you can see through, a glint
    across them. A window stacked on a window leaves out the frame between them ({top} and
    {bottom} say which of its own frame rows it keeps), so stacked window blocks are one tall
    window. The same drawing serves inside and out."""
    img, px, rng = _canvas(20261514, (200, 202, 200), 3)
    for y in range(2 if top else 0, 14 if bottom else 16):
        for x in range(2, 14):
            if x in (7, 8):
                px[x, y] = _shift((170, 172, 170), rng.uniform(-3, 3))
            elif (x + y) in (9, 10, 19):
                px[x, y] = _shift(TRAILER_GLASS, 70)[:3] + (170,)
            else:
                px[x, y] = _shift(TRAILER_GLASS, rng.uniform(-3, 3))[:3] + (TRAILER_GLASS_ALPHA,)
    return img


def trailer_panel():
    """Inside a trailer's walls: vinyl-faced panel, warm white, a batten at each panel joint (one a
    block, so it lands on the seam between blocks and never across a wall)."""
    img, px, rng = _canvas(20261516, (226, 220, 204), 2)
    for y in range(16):
        px[0, y] = _shift((196, 190, 174), rng.uniform(-2, 2))
    return img


def trailer_floor_in():
    """The floor inside: vinyl composition tile, eight pixels a tile, joints barely darker."""
    img, px, rng = _canvas(20261519, (170, 164, 150), 3)
    for y in range(16):
        for x in range(16):
            if x % 8 == 0 or y % 8 == 0:
                px[x, y] = _shift((162, 156, 143), rng.uniform(-2, 2))
    return img


def trailer_ceiling():
    """The ceiling inside: plain white panel, faintly mottled."""
    return _canvas(20261520, (232, 232, 226), 3)[0]


def trailer_roof():
    return _canvas(20261517, (190, 190, 186), 5)[0]


def toilet_side():
    """A portable toilet's moulded plastic wall: blue, with vertical ridges."""
    img, px, rng = _canvas(20261521, (40, 86, 168), 4)
    for y in range(16):
        for x in range(16):
            if x % 4 == 0:
                px[x, y] = _shift((40, 86, 168), -18 + rng.uniform(-3, 3))
    return img


def toilet_door(upper):
    """The door: lower half with the frame and a pull; upper half with the vent louvres and the
    vacancy indicator."""
    img, px, rng = _canvas(20261522 + upper, (46, 94, 176), 3)
    for y in range(16):
        for x in (1, 14):
            px[x, y] = _shift((30, 64, 130), rng.uniform(-3, 3))
    if upper:
        for x in range(1, 15):
            px[x, 3] = _shift((30, 64, 130), rng.uniform(-3, 3))
        for y in (5, 7):
            for x in range(4, 12):
                px[x, y] = _shift((24, 44, 90), rng.uniform(-3, 3))
        px[10, 11] = (60, 170, 70, 255)
        px[11, 11] = (60, 170, 70, 255)
    else:
        for y in (4, 5, 6):
            px[12, y] = _shift((220, 222, 224), rng.uniform(-4, 4))
    return img


def toilet_inside():
    """Inside a portable toilet: the same moulded plastic, plain and a shade lighter."""
    return _canvas(20261529, (70, 116, 190), 4)[0]


def toilet_door_inside():
    """The back of the door: plain plastic with the slide latch."""
    img, px, rng = _canvas(20261531, (70, 116, 190), 4)
    for x in (2, 3, 4):
        px[x, 8] = _shift((200, 202, 204), rng.uniform(-6, 6))
    return img


def toilet_roof():
    """The translucent white roof that lets the light in."""
    return _canvas(20261524, (232, 234, 230), 4)[0]


def gangbox_side():
    """A gang box: safety-yellow steel with a darker ribbed band along the bottom."""
    img, px, rng = _canvas(20261525, (228, 186, 36), 4)
    for y in range(12, 16):
        for x in range(16):
            px[x, y] = _shift((228, 186, 36), (-30 if x % 2 else -18) + rng.uniform(-3, 3))
    return img


def gangbox_lid():
    """The lid: the same yellow, with a raised diamond-plate pattern."""
    img, px, rng = _canvas(20261526, (232, 192, 44), 4)
    for y in range(16):
        for x in range(16):
            if (x + 2 * y) % 6 == 0:
                px[x, y] = _shift((232, 192, 44), 22 + rng.uniform(-3, 3))
    return img


def dark_steel():
    return _canvas(20261527, (52, 54, 56), 5)[0]


def washout_slurry():
    """Settled washout: grey concrete slurry, darker where it is still wet."""
    img, px, rng = _canvas(20261528, (150, 150, 144), 8)
    for _ in range(20):
        px[rng.randrange(16), rng.randrange(16)] = _shift((112, 112, 108), rng.uniform(-6, 6))
    return img


def washout_sign():
    """The CONCRETE WASHOUT sign, drawn as its layout: white board, black border, the legend as
    three bold bars (a legend is not legible at this size, its shape is)."""
    img = Image.new("RGBA", (16, 16), (244, 244, 240, 255))
    px = img.load()
    for i in range(16):
        for y in (0, 15):
            px[i, y] = (30, 30, 30, 255)
        for x in (0, 15):
            px[x, i] = (30, 30, 30, 255)
    for y, (x0, x1) in ((4, (3, 13)), (7, (2, 14)), (11, (4, 12))):
        for x in range(x0, x1):
            px[x, y] = (30, 30, 30, 255)
            px[x, y + 1] = (30, 30, 30, 255)
    return img


def textures():
    out = {"shell_floor": shell_floor(), "dumpster_floor": dumpster_floor(),
           "trailer_wall": trailer_wall(), "trailer_window": trailer_window(),
           "trailer_window_bottom": trailer_window(top=False),
           "trailer_window_top": trailer_window(bottom=False),
           "trailer_window_middle": trailer_window(top=False, bottom=False),
           "trailer_panel": trailer_panel(), "trailer_floor_in": trailer_floor_in(),
           "trailer_ceiling": trailer_ceiling(), "trailer_roof": trailer_roof(),
           "trailer_trim": frame((232, 232, 226), 20261518, d=-40),
           "toilet_side": toilet_side(), "toilet_door_lower": toilet_door(0),
           "toilet_door_upper": toilet_door(1), "toilet_roof": toilet_roof(),
           "toilet_inside": toilet_inside(), "toilet_door_inside": toilet_door_inside(),
           "gangbox_side": gangbox_side(), "gangbox_lid": gangbox_lid(),
           "site_dark_steel": dark_steel(), "washout_slurry": washout_slurry(),
           "washout_sign": washout_sign()}
    for i, (c, (rgb, _)) in enumerate(COLOURS.items()):
        seed = 20261530 + 10 * i
        out["container_%s_wall" % c] = container_wall(rgb, seed)
        out["container_%s_door" % c] = container_door(rgb, seed + 1)
        out["container_%s_roof" % c] = container_roof(rgb, seed + 2)
        out["container_%s_frame" % c] = frame(rgb, seed + 3)
        out["dumpster_%s_wall" % c] = dumpster_wall(rgb, seed + 4)
        out["dumpster_%s_inside" % c] = dumpster_inside(rgb, seed + 5)
        out["dumpster_%s_frame" % c] = frame(rgb, seed + 6)
    return out


# --------------------------------------------------------------------------------------------
# Geometry
# --------------------------------------------------------------------------------------------

box = sc._box
retex = gen_logistics.retex
INSET = 0.5          # a wall plane's distance in from the cell face
RAIL = 1.5           # frame rail and corner post section


def wall_plane(tex):
    """A shell wall on the north face."""
    return [box(0, 0, INSET, 16, 16, INSET, tex, faces=("north",))]


def roof_plane(tex):
    return [box(0, 16 - INSET, 0, 16, 16 - INSET, 16, tex, faces=("up",))]


def floor_plane(tex):
    return [box(0, INSET, 0, 16, INSET, 16, tex, faces=("down",))]


def rail_vertical():
    """The corner post on the north-west edge."""
    return [box(0, 0, 0, RAIL, 16, RAIL, "#frame")]


def rail_top():
    """The top rail along the north edge."""
    return [box(0, 16 - RAIL, 0, 16, 16, RAIL, "#frame")]


def rail_bottom():
    """The bottom rail along the north edge."""
    return [box(0, 0, 0, 16, RAIL, RAIL, "#frame")]


DUMPSTER_WALL = 1.5


def dumpster_wall_el():
    """A dumpster wall on the north side, painted outside, scuffed inside."""
    return [retex(box(0, 0, 0, 16, 16, DUMPSTER_WALL, "#wall", faces=("north", "south")),
                  {"south": "#inside"})]


def dumpster_lip():
    """The heavy lip along the top of an open wall."""
    return [box(0, 15, -0.5, 16, 16.5, DUMPSTER_WALL + 0.5, "#frame")]


def dumpster_floor_box():
    return [retex(box(0, 0, 0, 16, 1, 16, "#floor", faces=("up", "down")), {"down": "#under"})]


# Facing props, front to the north.

T_WALL = 0.5          # the toilet's moulded wall
T_IN = 0.25           # how far its walls sit in from the cell face, so a row of them do not touch
T_TOP = 29.0          # wall height, before the blockstate stretches it


def portable_toilet():
    """A portable toilet you can sit in, front (the door) to the north: walls drawn inside and out
    since a player sits in there, the door's back, the toilet box along the back wall with its
    black seat, a paper roll, and the translucent roof. Drawn 2 blocks tall; the blockstate
    stretches it to a real unit's 2.3."""
    a, b = T_IN, 16 - T_IN
    w = T_WALL
    wall_faces = ("north", "south", "east", "west")
    els = [box(a, 0, a, b, 0.5, b, "#inside", faces=("up", "down")),
           # Back and side walls: painted outside, plain inside.
           retex(box(a, 0.5, b - w, b, T_TOP, b, "#side", faces=wall_faces),
                 {"north": "#inside"}),
           retex(box(a, 0.5, a + w, a + w, T_TOP, b - w, "#side", faces=wall_faces),
                 {"east": "#inside"}),
           retex(box(b - w, 0.5, a + w, b, T_TOP, b - w, "#side", faces=wall_faces),
                 {"west": "#inside"}),
           # The front, which is the door: its face outside, its back inside.
           retex(box(a, 0.5, a, b, 16, a + w, "#side", faces=wall_faces),
                 {"north": "#doorlow", "south": "#doorinside"}),
           retex(box(a, 16, a, b, T_TOP, a + w, "#side", faces=wall_faces),
                 {"north": "#doorhigh", "south": "#doorinside"}),
           box(0, T_TOP, 0, 16, 31, 16, "#roof"),
           box(11, 31, 11, 12.5, 32, 12.5, "#roof"),
           # The toilet: a box along the back wall, its seat, and the paper.
           box(2, 0.5, 9, 14, 7, b - w, "#inside"),
           box(5, 7, 10, 11, 7.5, 14, "#seat"),
           box(b - w - 1.5, 9, 6, b - w, 10.5, 7.5, "#roof")]
    return els


def gang_box():
    return [box(1, 0, 3, 15, 9, 13, "#side"),
            box(0.5, 9, 2.5, 15.5, 10.5, 13.5, "#lid"),
            box(7, 5.5, 2.2, 9, 8.5, 3, "#dark"),
            box(0, 6, 6, 1, 7, 10, "#dark"),
            box(15, 6, 6, 16, 7, 10, "#dark")]


def concrete_washout():
    """A lined steel pan of settled slurry, with its sign at the front."""
    els = [box(0, 0, 0, 16, 1, 16, "#pan"),
           box(0, 1, 0, 16, 5, 1, "#pan"), box(0, 1, 15, 16, 5, 16, "#pan"),
           box(0, 1, 1, 1, 5, 15, "#pan"), box(15, 1, 1, 16, 5, 15, "#pan"),
           box(1, 1, 1, 15, 3, 15, "#slurry", faces=("up",)),
           box(7.5, 5, 0.3, 8.5, 9, 0.8, "#pan"),
           retex(box(4, 9, 0.2, 12, 14, 0.7, "#pan"), {"north": "#sign"})]
    return els


# --------------------------------------------------------------------------------------------
# Models and blockstates
# --------------------------------------------------------------------------------------------

def _t(name):
    return TEX_REF % name


def _model(elements, textures, particle, parent=None):
    t = dict(textures)
    t["particle"] = textures[particle]
    m = {"textures": t, "elements": elements}
    if parent:
        m = {"parent": parent, **m}
    return m


def _turn(elements, quarter):
    """Elements turned a number of quarter turns clockwise seen from above (as a blockstate's y
    rotation would), for the inventory models, which cannot use a blockstate's rotations."""
    turn_face = {"north": "east", "east": "south", "south": "west", "west": "north",
                 "up": "up", "down": "down"}
    out = []
    for el in elements:
        f, t = el["from"], el["to"]
        x0, z0, x1, z1 = f[0], f[2], t[0], t[2]
        faces = dict(el["faces"])
        for _ in range(quarter):
            x0, z0, x1, z1 = 16 - z1, x0, 16 - z0, x1
            faces = {turn_face[k]: v for k, v in faces.items()}
        textures = {k: v["texture"] for k, v in faces.items()}
        first = next(iter(textures.values()))
        new = box(x0, f[1], z0, x1, t[1], z1, first, faces=tuple(faces.keys()))
        out.append(retex(new, textures))
    return out


def _around(elements):
    out = []
    for q in range(4):
        out += _turn(elements, q)
    return out


SIDES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
CORNERS = ((("north", "west"), 0), (("north", "east"), 90), (("south", "east"), 180),
           (("south", "west"), 270))


def _apply(model, rot):
    a = {"model": MODEL_REF % model}
    if rot:
        a["y"] = rot
    return a


def _rails(prefix, top=True):
    parts = []
    for (a, b), rot in CORNERS:
        parts.append({"when": {a: "false", b: "false"}, "apply": _apply(prefix + "_post", rot)})
    for side, rot in SIDES:
        if top:
            parts.append({"when": {side: "false", "up": "false"},
                          "apply": _apply(prefix + "_rail_top", rot)})
        parts.append({"when": {side: "false", "down": "false"},
                      "apply": _apply(prefix + "_rail_bottom", rot)})
    return parts


# --------------------------------------------------------------------------------------------
# The job trailer
# --------------------------------------------------------------------------------------------
#
# A trailer is built as a building is: a floor, walls and a roof of trailer blocks, with air inside
# to walk about and furnish. Each side of a block is "joined" (more trailer), "out" or "in", which
# BlockJobTrailer works out: a side is in when the space it faces has trailer both above and below
# it (for the top and bottom, the one of those that is across the space). Out is siding, roof and
# underside; in is panelling, floor and ceiling. The door is a real door from the doors family,
# set in an opening in the wall, and the window's glass can be seen through.
#
# Every part is written where it goes rather than turned by the blockstate, so which of its faces
# is which side is never in doubt.

T_HORIZONTAL = ("north", "east", "south", "west")
T_QUARTER = {"north": 0, "east": 1, "south": 2, "west": 3}
T_OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east",
              "up": "down", "down": "up"}
# side -> (axis, whether it is the positive end of it)
T_AXIS = {"west": (0, False), "east": (0, True), "down": (1, False), "up": (1, True),
          "north": (2, False), "south": (2, True)}
T_AXIS_ENDS = {0: ("west", "east"), 1: ("down", "up"), 2: ("north", "south")}
EXPOSED = "in|out"
# Every edge of the cell, as the two sides that meet there: the four corner posts, then the rails
# along the top and the bottom.
T_POSTS = (("north", "west"), ("north", "east"), ("south", "east"), ("south", "west"))
T_EDGES = T_POSTS + tuple((h, v) for v in ("up", "down") for h in T_HORIZONTAL)


def _span(side, depth):
    """The span along a side's axis that lies within {depth} of that side's face."""
    return (16 - depth, 16) if T_AXIS[side][1] else (0, depth)


def _cuboid(ranges, tex, faces):
    (x0, x1), (y0, y1), (z0, z1) = ranges
    return box(x0, y0, z0, x1, y1, z1, tex, faces=tuple(faces))


def t_plane(side, tex):
    """A trailer face on {side}: a plane half a pixel in from the cell face, as a container's."""
    if side == "up":
        return roof_plane(tex)
    if side == "down":
        return floor_plane(tex)
    return _turn(wall_plane(tex), T_QUARTER[side])


def t_rail(a, b, extend=None):
    """The frame rail along the edge where sides {a} and {b} meet: a corner post where both are
    walls, a top or bottom rail where one is the roof or the underside.

    It is drawn on every edge where both sides are open and one of them is out. Where the other
    is in -- the sides of a door opening, whose jambs face a space with the header over it and the
    floor under it -- the rail is the opening's casing. Its face toward the opening lies in the
    plane of the door leaf's edge, but the two face opposite ways, so each is culled from the
    side the other is seen from and they never fight. (Leaving that face out, as the first build
    did, opened a channel down which the siding showed edge-on, its rib rows dark nubs up the
    jamb.)

    {extend} ("up" or "down") draws instead the post's continuation a rail's depth into the block
    above or below. At the top of a door opening the casing post ends at its block and the rail
    over the opening starts in the next, and the corner between them, in a block that has neither,
    was left open: the notch at the top of the doorway in issue #228."""
    ranges = [(0, 16), (0, 16), (0, 16)]
    for side in (a, b):
        ranges[T_AXIS[side][0]] = _span(side, RAIL)
    if extend == "up":
        ranges[1] = (16, 16 + RAIL)
    elif extend == "down":
        ranges[1] = (-RAIL, 0)
    return [_cuboid(ranges, "#frame", sc.FACES)]


def t_bead(a, b, ends):
    """The bead in a concave corner: the edge where sides {a} and {b} both join more trailer.

    The faces either side of a concave corner are drawn by the two blocks beside this one, each
    half a pixel in from its cell face, so they stop half a pixel short of meeting and the corner
    was a slit you could see the sky through (issue #228). This block cannot tell whether the
    corner is concave -- that is the block diagonally across, which is not in its state -- so it
    draws the bead on every edge where both sides join: inside a solid run of trailer it is walled
    in and never seen. It is the half pixel square the two faces left out, standing half a pixel
    proud of them, so both faces run into it.

    Along the edge it reaches the cell face where more trailer carries on ({ends}: whether the
    lower and upper ends join), so a bead runs unbroken from block to block, and stops half a
    pixel short where the block is open, behind the face drawn there, which it would otherwise
    poke through."""
    ranges = [None, None, None]
    for side in (a, b):
        ranges[T_AXIS[side][0]] = _span(side, INSET)
    c = ranges.index(None)
    ranges[c] = (0 if ends[0] else INSET, 16 if ends[1] else 16 - INSET)
    return [_cuboid(ranges, "#frame", (a, b))]


def t_reveal(side, through=None):
    """A window's reveal on a side where the wall carries on: the frame's face inside the window,
    from the glass on one face of the wall to the glass on the other, so a window seen at an angle
    shows the depth of the wall rather than the hollow inside the wall beside it.

    A reveal on a wall side spans the wall's depth, which is its other horizontal axis. The head
    and sill reveals ({side} up or down) span it along {through}, the axis through the wall
    (0 for x, 2 for z)."""
    ranges = [(0, 16), (0, 16), (0, 16)]
    axis, positive = T_AXIS[side]
    ranges[axis] = (16, 16) if positive else (0, 0)
    depth = through if through is not None else 2 - axis
    ranges[depth] = (INSET, 16 - INSET)
    return [_cuboid(ranges, "#frame", (T_OPPOSITE[side],))]


# The window's four drawings, by (upper, topped): whether a window is below it and above it.
T_WINDOWS = (("false", "false", "window"), ("false", "true", "windowbottom"),
             ("true", "true", "windowmiddle"), ("true", "false", "windowtop"))


def trailer_models():
    tex = {"wall": _t("trailer_wall"), "panel": _t("trailer_panel"),
           "window": _t("trailer_window"), "windowbottom": _t("trailer_window_bottom"),
           "windowtop": _t("trailer_window_top"), "windowmiddle": _t("trailer_window_middle"),
           "roof": _t("trailer_roof"), "floorin": _t("trailer_floor_in"),
           "floor": _t("shell_floor"), "ceiling": _t("trailer_ceiling"),
           "frame": _t("trailer_trim")}
    out = {}
    p = "job_trailer_"
    for side in T_HORIZONTAL:
        for key in ("wall", "panel") + tuple(w for _, _, w in T_WINDOWS):
            out[p + key + "_" + side] = _model(t_plane(side, "#" + key), tex, "wall")
        out[p + "reveal_" + side] = _model(t_reveal(side), tex, "wall")
    for side, key in (("up", "roof"), ("up", "floorin"), ("down", "floor"),
                      ("down", "ceiling")):
        out[p + key] = _model(t_plane(side, "#" + key), tex, "wall")
    for side in ("up", "down"):
        for through, name in ((2, "z"), (0, "x")):
            out[p + "reveal_%s_%s" % (side, name)] = _model(t_reveal(side, through), tex, "wall")
    for a, b in T_EDGES:
        post = b not in ("up", "down")
        name = p + ("post_%s_%s" if post else "rail_%s_%s") % (a, b)
        out[name] = _model(t_rail(a, b), tex, "wall")
        if post:
            for extend in ("up", "down"):
                out[name + "_" + extend] = _model(t_rail(a, b, extend), tex, "wall")
        for ends in ((False, False), (False, True), (True, False), (True, True)):
            out[p + "bead_%s_%s_%s" % (a, b, _ends_name(ends))] = _model(
                t_bead(a, b, ends), tex, "wall")
    for name, key in (("job_trailer_wall", "wall"), ("job_trailer_window", "window")):
        lone = [el for side in T_HORIZONTAL for el in t_plane(side, "#" + key)]
        lone += t_plane("up", "#roof") + t_plane("down", "#floor")
        for a, b in T_EDGES:
            lone += t_rail(a, b)
        out[name + "_inventory"] = _model(lone, tex, "wall", parent="block/block")
    return out


def _ends_name(ends):
    return "".join("j" if e else "o" for e in ends)


def _apply_t(model):
    return {"model": MODEL_REF % ("job_trailer_" + model)}


def trailer_state(key):
    """The multipart blockstate of the wall ({key} "wall") or the window ("window")."""
    parts = []
    for side in T_HORIZONTAL:
        if key == "window":
            for upper, topped, drawing in T_WINDOWS:
                parts.append({"when": {side: EXPOSED, "upper": upper, "topped": topped},
                              "apply": _apply_t(drawing + "_" + side)})
            parts.append({"when": {side: "joined"}, "apply": _apply_t("reveal_" + side)})
        else:
            parts.append({"when": {side: "out"}, "apply": _apply_t("wall_" + side)})
            parts.append({"when": {side: "in"}, "apply": _apply_t("panel_" + side)})
    parts.append({"when": {"up": "out"}, "apply": _apply_t("roof")})
    parts.append({"when": {"up": "in"}, "apply": _apply_t("floorin")})
    parts.append({"when": {"down": "out"}, "apply": _apply_t("floor")})
    parts.append({"when": {"down": "in"}, "apply": _apply_t("ceiling")})
    if key == "window":
        # The head and sill of the whole window: left out between stacked windows.
        for side, stacked in (("up", "topped"), ("down", "upper")):
            for through, name, faces in ((2, "z", ("north", "south")),
                                         (0, "x", ("east", "west"))):
                parts.append({"when": {"OR": [{side: "joined", stacked: "false", f: EXPOSED}
                                              for f in faces]},
                              "apply": _apply_t("reveal_%s_%s" % (side, name))})
    for a, b in T_EDGES:
        post = b not in ("up", "down")
        base = ("post_%s_%s" if post else "rail_%s_%s") % (a, b)
        # Both open and one of them out: out and out, or out and in either way round.
        rail = [{a: "out", b: EXPOSED}, {a: "in", b: "out"}]
        parts.append({"when": {"OR": rail}, "apply": _apply_t(base)})
        if post:
            for extend in ("up", "down"):
                when = [dict(w, **{extend: "joined"}) for w in rail]
                parts.append({"when": {"OR": when}, "apply": _apply_t(base + "_" + extend)})
        c = [i for i in range(3) if i not in (T_AXIS[a][0], T_AXIS[b][0])][0]
        lo, hi = T_AXIS_ENDS[c]
        for ends in ((False, False), (False, True), (True, False), (True, True)):
            when = {a: "joined", b: "joined", lo: "joined" if ends[0] else EXPOSED,
                    hi: "joined" if ends[1] else EXPOSED}
            parts.append({"when": when,
                          "apply": _apply_t("bead_%s_%s_%s" % (a, b, _ends_name(ends)))})
    name = "job_trailer_" + key
    return {"variants": {"inventory": {"model": MODEL_REF % (name + "_inventory")}},
            "multipart": parts}


def models():
    out = {}
    for c in COLOURS:
        p = "container_" + c
        tex = {"wall": _t(p + "_wall"), "door": _t(p + "_door"), "roof": _t(p + "_roof"),
               "frame": _t(p + "_frame"), "floor": _t("shell_floor")}
        out[p + "_wall"] = _model(wall_plane("#wall"), tex, "wall")
        out[p + "_door"] = _model(wall_plane("#door"), tex, "wall")
        out[p + "_roof"] = _model(roof_plane("#roof"), tex, "wall")
        out[p + "_floor"] = _model(floor_plane("#floor"), tex, "wall")
        out[p + "_post"] = _model(rail_vertical(), tex, "wall")
        out[p + "_rail_top"] = _model(rail_top(), tex, "wall")
        out[p + "_rail_bottom"] = _model(rail_bottom(), tex, "wall")
        lone = (_turn(wall_plane("#door"), 0) + _around(wall_plane("#wall"))[1:]
                + roof_plane("#roof") + floor_plane("#floor") + _around(rail_vertical())
                + _around(rail_top()) + _around(rail_bottom()))
        out[p + "_inventory"] = _model(lone, tex, "wall", parent="block/block")

        d = "dumpster_" + c
        tex = {"wall": _t(d + "_wall"), "inside": _t(d + "_inside"), "frame": _t(d + "_frame"),
               "floor": _t("dumpster_floor"), "under": _t("shell_floor")}
        out[d + "_wall"] = _model(dumpster_wall_el(), tex, "wall")
        out[d + "_lip"] = _model(dumpster_lip(), tex, "wall")
        out[d + "_floor"] = _model(dumpster_floor_box(), tex, "wall")
        out[d + "_post"] = _model(rail_vertical(), tex, "wall")
        out[d + "_rail_bottom"] = _model(rail_bottom(), tex, "wall")
        lone = (_around(dumpster_wall_el()) + _around(dumpster_lip()) + dumpster_floor_box()
                + _around(rail_vertical()) + _around(rail_bottom()))
        out[d + "_inventory"] = _model(lone, tex, "wall", parent="block/block")

    out.update(trailer_models())

    out["portable_toilet"] = _model(portable_toilet(), {
        "side": _t("toilet_side"), "doorlow": _t("toilet_door_lower"),
        "doorhigh": _t("toilet_door_upper"), "roof": _t("toilet_roof"),
        "inside": _t("toilet_inside"), "doorinside": _t("toilet_door_inside"),
        "seat": _t("site_dark_steel")}, "side", parent="block/block")
    out["portable_toilet"]["display"] = {"gui": {"rotation": [30, 225, 0],
                                                 "translation": [0, -2.5, 0],
                                                 "scale": [0.36, 0.36, 0.36]}}
    out["portable_toilet_upper"] = {"textures": {"particle": _t("toilet_side")}, "elements": []}
    out["gang_box"] = _model(gang_box(), {"side": _t("gangbox_side"), "lid": _t("gangbox_lid"),
                                          "dark": _t("site_dark_steel")}, "side",
                             parent="block/block")
    out["concrete_washout"] = _model(concrete_washout(), {
        "pan": _t("site_dark_steel"), "slurry": _t("washout_slurry"),
        "sign": _t("washout_sign")}, "slurry", parent="block/block")
    return out


TOILET_STRETCH = 1.15    # 2 blocks as drawn, 2.3 as stood: a real unit's height


def toilet_state(ref):
    """The toilet's blockstate, in Forge's format so it can stretch the model upward: a block model
    cannot be drawn taller than two blocks. The stretch is about the block's centre, so it is
    lifted back onto the ground by what the stretch pushed below it (block units, as a Forge
    transform is)."""
    lift = round(0.5 * (TOILET_STRETCH - 1), 4)
    return {"forge_marker": 1,
            "defaults": {"model": ref,
                         "transform": {"scale": [1, TOILET_STRETCH, 1],
                                       "translation": [0, lift, 0]}},
            "variants": {"facing": {side: ({"y": rot} if rot else {}) for side, rot in SIDES},
                         # The upper half is only there to be clicked: it draws nothing.
                         "upper": {"false": {},
                                   "true": {"model": MODEL_REF % "portable_toilet_upper"}},
                         "inventory": [{}]}}


def blockstates():
    out = {}
    others = {s: "|".join(o for o, _ in SIDES if o != s) for s, _ in SIDES}
    for c in COLOURS:
        p = "container_" + c
        parts = []
        for side, rot in SIDES:
            parts.append({"when": {"facing": side, side: "false"}, "apply": _apply(p + "_door", rot)})
            parts.append({"when": {"facing": others[side], side: "false"},
                          "apply": _apply(p + "_wall", rot)})
        parts.append({"when": {"up": "false"}, "apply": _apply(p + "_roof", 0)})
        parts.append({"when": {"down": "false"}, "apply": _apply(p + "_floor", 0)})
        parts += _rails(p)
        out[p] = {"variants": {"inventory": {"model": MODEL_REF % (p + "_inventory")}},
                  "multipart": parts}

        d = "dumpster_" + c
        parts = []
        for side, rot in SIDES:
            parts.append({"when": {side: "false"}, "apply": _apply(d + "_wall", rot)})
            parts.append({"when": {side: "false", "up": "false"}, "apply": _apply(d + "_lip", rot)})
        parts.append({"when": {"down": "false"}, "apply": _apply(d + "_floor", 0)})
        parts += _rails(d, top=False)
        out[d] = {"variants": {"inventory": {"model": MODEL_REF % (d + "_inventory")}},
                  "multipart": parts}

    for key in ("wall", "window"):
        out["job_trailer_" + key] = trailer_state(key)

    for name in BOXES:
        ref = MODEL_REF % name
        if name == "portable_toilet":
            out[name] = toilet_state(ref)
            continue
        variants = {"facing=%s" % side: ({"model": ref, "y": rot} if rot else {"model": ref})
                    for side, rot in SIDES}
        variants["inventory"] = {"model": ref}
        out[name] = {"variants": variants}
    return out


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
        return gen_logistics.check("facilities", write_all, roots)
    written = write_all(TEX_DIR, MODEL_DIR, STATE_DIR)
    print("Wrote %d facilities files" % len(written))
    return 0


if __name__ == "__main__":
    sys.exit(main())
