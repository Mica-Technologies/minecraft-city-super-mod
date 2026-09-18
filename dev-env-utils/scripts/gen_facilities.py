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
    "job_trailer_door": ("Job Trailer Door", "Oficina de Obra (Puerta)", "Baucontainer (Tür)",
                         "Bodvagn (Dörr)"),
    "portable_toilet": ("Portable Toilet", "Baño Portátil", "Mobile Toilette", "Bajamaja"),
    "gang_box": ("Gang Box", "Caja de Herramientas de Obra", "Baustellenbox", "Verktygsbox"),
    "concrete_washout": ("Concrete Washout", "Lavadero de Hormigón", "Betonauswaschstation",
                         "Betongtvätt"),
})

# The facing props' boxes, front to the north, in sixteenths -- the Java registration uses these.
BOXES = {
    "portable_toilet": (1, 0, 1, 15, 31, 15),
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
    """A door leaf: flat panel, a horizontal stiffener top and bottom, and two locking bars."""
    img, px, rng = _canvas(seed, rgb, 4)
    for y in range(16):
        for x in range(16):
            px[x, y] = _shift(rgb, 6 + (-14 if y in (0, 15) else 0) + rng.uniform(-3, 3))
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


def trailer_window():
    """A sliding window: a grey frame, dark tinted glass in two panes, a glint across it."""
    img, px, rng = _canvas(20261514, (200, 202, 200), 3)
    for y in range(2, 14):
        for x in range(2, 14):
            if x in (7, 8):
                px[x, y] = _shift((170, 172, 170), rng.uniform(-3, 3))
            else:
                glint = 30 if (x + y) in (9, 10, 19) else 0
                px[x, y] = _shift((52, 70, 88), glint + rng.uniform(-3, 3))
    return img


def trailer_door(upper):
    """A steel entry door: the frame down both sides; the lower half has the lever handle, the
    upper half a small wired-glass light."""
    img, px, rng = _canvas(20261515 + upper, (214, 214, 208), 3)
    for y in range(16):
        for x in (1, 14):
            px[x, y] = _shift((150, 150, 146), rng.uniform(-3, 3))
    if upper:
        for x in range(1, 15):
            px[x, 1] = _shift((150, 150, 146), rng.uniform(-3, 3))
        for y in range(4, 10):
            for x in range(5, 11):
                px[x, y] = _shift((60, 76, 92), rng.uniform(-3, 3))
    else:
        for x in (10, 11, 12):
            px[x, 6] = _shift((70, 70, 72), rng.uniform(-3, 3))
        px[12, 7] = _shift((70, 70, 72), 0)
    return img


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
           "trailer_door_lower": trailer_door(0), "trailer_door_upper": trailer_door(1),
           "trailer_roof": trailer_roof(),
           "trailer_trim": frame((232, 232, 226), 20261518, d=-40),
           "toilet_side": toilet_side(), "toilet_door_lower": toilet_door(0),
           "toilet_door_upper": toilet_door(1), "toilet_roof": toilet_roof(),
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

def portable_toilet():
    els = [retex(box(1, 0, 1, 15, 16, 15, "#side"), {"north": "#doorlow"}),
           retex(box(1, 16, 1, 15, 29, 15, "#side", faces=("north", "south", "east", "west")),
                 {"north": "#doorhigh"}),
           box(0.5, 29, 0.5, 15.5, 31, 15.5, "#roof"),
           box(11, 31, 11, 12.5, 32, 12.5, "#roof")]
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

    tex = {"wall": _t("trailer_wall"), "window": _t("trailer_window"),
           "doorlow": _t("trailer_door_lower"), "doorhigh": _t("trailer_door_upper"),
           "roof": _t("trailer_roof"), "frame": _t("trailer_trim"), "floor": _t("shell_floor")}
    for key in ("wall", "window", "doorlow", "doorhigh"):
        out["job_trailer_" + key] = _model(wall_plane("#" + key), tex, "wall")
    out["job_trailer_roof"] = _model(roof_plane("#roof"), tex, "wall")
    out["job_trailer_floor"] = _model(floor_plane("#floor"), tex, "wall")
    out["job_trailer_post"] = _model(rail_vertical(), tex, "wall")
    out["job_trailer_rail_top"] = _model(rail_top(), tex, "wall")
    out["job_trailer_rail_bottom"] = _model(rail_bottom(), tex, "wall")
    for name, key in (("job_trailer_wall", "wall"), ("job_trailer_window", "window"),
                      ("job_trailer_door", "doorlow")):
        lone = (_around(wall_plane("#" + key)) + roof_plane("#roof") + floor_plane("#floor")
                + _around(rail_vertical()) + _around(rail_top()) + _around(rail_bottom()))
        out[name + "_inventory"] = _model(lone, tex, "wall", parent="block/block")

    out["portable_toilet"] = _model(portable_toilet(), {
        "side": _t("toilet_side"), "doorlow": _t("toilet_door_lower"),
        "doorhigh": _t("toilet_door_upper"), "roof": _t("toilet_roof")}, "side",
        parent="block/block")
    out["portable_toilet"]["display"] = {"gui": {"rotation": [30, 225, 0],
                                                 "translation": [0, -2.5, 0],
                                                 "scale": [0.36, 0.36, 0.36]}}
    out["gang_box"] = _model(gang_box(), {"side": _t("gangbox_side"), "lid": _t("gangbox_lid"),
                                          "dark": _t("site_dark_steel")}, "side",
                             parent="block/block")
    out["concrete_washout"] = _model(concrete_washout(), {
        "pan": _t("site_dark_steel"), "slurry": _t("washout_slurry"),
        "sign": _t("washout_sign")}, "slurry", parent="block/block")
    return out


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

    for name, key in (("job_trailer_wall", "wall"), ("job_trailer_window", "window"),
                      ("job_trailer_door", None)):
        parts = []
        for side, rot in SIDES:
            if key:
                parts.append({"when": {side: "false"},
                              "apply": _apply("job_trailer_" + key, rot)})
            else:
                parts.append({"when": {side: "false", "upper": "false"},
                              "apply": _apply("job_trailer_doorlow", rot)})
                parts.append({"when": {side: "false", "upper": "true"},
                              "apply": _apply("job_trailer_doorhigh", rot)})
        parts.append({"when": {"up": "false"}, "apply": _apply("job_trailer_roof", 0)})
        parts.append({"when": {"down": "false"}, "apply": _apply("job_trailer_floor", 0)})
        parts += _rails("job_trailer")
        out[name] = {"variants": {"inventory": {"model": MODEL_REF % (name + "_inventory")}},
                     "multipart": parts}

    for name in BOXES:
        ref = MODEL_REF % name
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
