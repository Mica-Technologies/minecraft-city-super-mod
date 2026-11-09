#!/usr/bin/env python3
"""
Generates the OBJ floodlights (GitHub #99): Lithonia D-Series DSXF1 / DSXF2 / DSXF3 and TFL, and
GE Evolve EFM, each on a knuckle, a yoke (trunnion) and a slipfitter, as baked models.

WHY
---
The requester asked for the floods that have public spec sheets, mountable on walls, poles and
RealGrid crossarms. A flood is a flat head turned on a pivot, so its model is the head drawn level
in its own frame and then turned to its aim about the pivot: the same head serves every mount.

WHAT IT WRITES
--------------
  models/block/lighting/shared_models/floodlight_<block>.obj   one per block, 0..1 block space
  models/block/lighting/shared_models/floodlight.mtl            shared: body, unshaded_lens
  blockstates/<block>.json                                      facing x STATE, the lens per state
  textures/blocks/lighting/shared_textures/floodlight_lens_*.png  32 px lens sprites, and an `_e`
                                                                companion of each lit one (OptiFine
                                                                emissive: the LEDs or lamp only)

The housings wear the street lights' 16 px swatches (`streetlight_body_*`, gen_streetlight_fixtures
owns them), so a flood adds geometry and two small lens sprites, nothing else.

THE BLOCKS (BlockFloodlight, LIGHTING_SYSTEM.md "Floodlights")
--------------------------------------------------------------
Metadata holds facing (4) x STATE (4), so each mounting surface is its own block: <name> on a
wall, a pole's side or an arm's end; <name>_g on top of a block (a ground uplight; a slipfitter's is
a pole top); <name>_c hung under a block; <name>_x on a RealGrid crossarm's front. Every model is
drawn facing north, its back (a wall plate, an arm's slipfitter) toward z = 16, as every CSM
fixture is, and aimed by the pitch its BlockFloodlight.Surface gives the light: 35 degrees down
on a wall, a pole top and a crossarm, 50 up from the ground, 50 down from a ceiling.

SCALE AND SOURCES
-----------------
1.75 in to the pixel, as the #99 fixtures are (gen_streetlight_fixtures.py). Dimensions from the
makers' sheets: DSXF3 13 W x 13-5/8 H x 5 D in, 17-1/2 overall with its knuckle (Lithonia DSXF3-LED,
rev. 02/08/17); DSXF2 12-7/8 x 7-3/4 x 3-1/8 (DSXF2-LED); DSXF1 8-7/8 x ~8-1/2 x 3-1/8, 12 overall
(Lithonia's Size 1 brochure: two COB engines in a window over a finned driver housing); TFL
16 x 15-3/4 x 6 in; GE Evolve EFM ~16.4 x 15.7 x 5.5 in (the plan's research table).

Run:  python dev-env-utils/scripts/gen_floodlights.py [--check] [--fragments]
"""

import io
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402
import gen_streetlight_fixtures as sl  # noqa: E402

from PIL import Image  # noqa: E402

MODEL_DIR = sl.MODEL_DIR
TEX_DIR = sl.TEX_DIR
OWNER = sl.OWNER
MTL_NAME = "floodlight.mtl"
TEX = sl.TEX
LENS_MAT = sl.LENS_MAT
IN_PER_PX = 1.75
PLATE_Z = 15.4      # a wall plate's front; its back stands off the face at sl.REAR_Z (15.9)
FLOOR_Y = 0.1       # a ground base's underside, off the block's floor


def px(inches):
    return inches / IN_PER_PX


# --------------------------------------------------------------------------- meshes
class Mesh(sl.Mesh):
    """The street lights' mesh, written as a floodlight."""

    def text(self, header):
        out = sl.Mesh.text(self, header)
        return out.replace("gen_streetlight_fixtures.py", "gen_floodlights.py").replace(
            "mtllib " + sl.MTL_NAME, "mtllib " + MTL_NAME)


class Part:
    """Polygons in a part's own frame, emitted into a mesh through a transform. The shape
    helpers of gen_streetlight_fixtures only call poly_out, so they draw into a Part as they
    would into a Mesh."""

    def __init__(self):
        self.polys = []

    def poly_out(self, mat, ps, uvs, inside):
        self.polys.append((mat, [tuple(p) for p in ps], list(uvs), tuple(inside)))

    def points(self):
        return [p for _, ps, _, _ in self.polys for p in ps]

    def emit(self, mesh, f):
        for mat, ps, uvs, inside in self.polys:
            mesh.poly_out(mat, [f(p) for p in ps], uvs, f(inside))


def rot_x(deg):
    """A turn about x; the head is level at 0 and a positive pitch aims it down. The face's
    normal (0, 0, -1) goes to (0, -sin, -cos)."""
    a = math.radians(-deg)
    c, s = math.cos(a), math.sin(a)
    return lambda p: (p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c)


def compose(*fs):
    def f(p):
        for g in reversed(fs):
            p = g(p)
        return p
    return f


def move(dx, dy, dz):
    return lambda p: (p[0] + dx, p[1] + dy, p[2] + dz)


def lens_box(m, lo, hi):
    """A panel whose front (low z) face is the lens, u across x, v down y; the rest body."""
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    c = ((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2)
    uv = [sl.swatch_uv(0, 0), sl.swatch_uv(1, 0), sl.swatch_uv(1, 1), sl.swatch_uv(0, 1)]
    front = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)]
    m.poly_out(LENS_MAT, front, [(0.03 + 0.94 * (x1 - p[0]) / (x1 - x0),
                                  0.03 + 0.94 * (y1 - p[1]) / (y1 - y0)) for p in front], c)
    for ps in ([(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
               [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
               [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)],
               [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)]):
        m.poly_out("body", ps, uv, c)


def open_box(m, lo, hi, skip):
    """A box without the faces named in `skip` (they are buried in what it stands on)."""
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    c = ((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2)
    uv = [sl.swatch_uv(0, 0), sl.swatch_uv(1, 0), sl.swatch_uv(1, 1), sl.swatch_uv(0, 1)]
    faces = {
        "down": [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
        "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        "north": [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)],
        "south": [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)],
        "west": [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)],
        "east": [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)],
    }
    for name, ps in faces.items():
        if name not in skip:
            m.poly_out("body", ps, uv, c)


# --------------------------------------------------------------------------- the heads
# Every head is drawn level in its own frame: centred on the origin, W across x, H up y, its lens
# facing -z at z = -D/2, its back at +D/2.
class Head:
    def __init__(self, name, w, h, d, builder):
        self.name, self.w, self.h, self.d = name, w, h, d
        self.builder = builder

    def part(self):
        p = Part()
        self.builder(self, p)
        return p


def dsxf_head(window, fins):
    """Lithonia D-Series: a die-cast slab, square-cornered, its back swelling to a rounded
    hump; on the face the optics window across the top (`window`, the fraction of the face it
    takes) and the driver's vertical fins under it, the knuckle threaded into the bottom."""
    def build(hd, m):
        w, h, d = hd.w / 2, hd.h / 2, hd.d
        z0 = -d / 2
        sl.loft_closed(m, [(z0, 0.0, 0.0, w, h), (z0 + 0.3 * d, 0.0, 0.0, w, h),
                           (z0 + 0.7 * d, 0.0, -0.05 * h, 0.95 * w, 0.9 * h),
                           (z0 + d, 0.0, -0.1 * h, 0.72 * w, 0.62 * h)], 7.0, n=20)
        top = h - 0.35
        bottom = h - window * 2 * h
        lens_box(m, (-w + 0.4, bottom, z0 - 0.12), (w - 0.4, top, z0 + 0.05))
        # the driver's fins: a row of ribs proud of the face under the window
        span = 2 * w - 1.0
        pitch = span / fins
        for k in range(fins):
            x = -w + 0.5 + pitch * (k + 0.5)
            open_box(m, (x - 0.14, -h + 0.45, z0 - 0.28), (x + 0.14, bottom - 0.35, z0 + 0.05),
                     skip=("south",))
    return build


def tfl_head(hd, m):
    """Lithonia TFL: a square HID flood, a door frame round a big tempered glass lens over the
    lamp and reflector, the ballast housing behind tapering to a rounded back, ribbed on top."""
    w, h, d = hd.w / 2, hd.h / 2, hd.d
    z0 = -d / 2
    sl.loft_closed(m, [(z0, 0.0, 0.0, w, h), (z0 + 0.25 * d, 0.0, 0.0, w, h),
                       (z0 + 0.6 * d, 0.0, 0.0, 0.88 * w, 0.88 * h),
                       (z0 + d, 0.0, -0.05 * h, 0.6 * w, 0.6 * h)], 6.0, n=20)
    lens_box(m, (-w + 0.75, -h + 0.75, z0 - 0.08), (w - 0.75, h - 0.75, z0 + 0.05))
    # the door frame's clamp lugs, top and bottom
    for y in (h - 0.35, -h + 0.35):
        open_box(m, (-0.7, y - 0.3, z0 - 0.25), (0.7, y + 0.3, z0 + 0.05), skip=("south",))
    # ribs over the ballast housing
    for k in range(4):
        x = -0.6 * w + k * 0.4 * w
        open_box(m, (x - 0.15, 0.6 * h, z0 + 0.3 * d), (x + 0.15, h + 0.2, z0 + 0.55 * d),
                 skip=("down",))


def efm_head(hd, m):
    """GE Evolve EFM: a square frame round a grid of LED optics, a deep heat sink behind it
    of vertical fins."""
    w, h, d = hd.w / 2, hd.h / 2, hd.d
    z0 = -d / 2
    sl.loft_closed(m, [(z0, 0.0, 0.0, w, h), (z0 + 0.35 * d, 0.0, 0.0, w, h),
                       (z0 + 0.5 * d, 0.0, 0.0, 0.92 * w, 0.92 * h)], 8.0, n=20)
    lens_box(m, (-w + 0.6, -h + 0.6, z0 - 0.1), (w - 0.6, h - 0.6, z0 + 0.05))
    # the heat sink: fins standing off the back plate
    fins = 7
    for k in range(fins):
        x = -0.8 * w + k * (1.6 * w / (fins - 1))
        open_box(m, (x - 0.16, -0.85 * h, z0 + 0.45 * d), (x + 0.16, 0.85 * h, z0 + d),
                 skip=("north",))


HEADS = {
    # DSXF1: 8-7/8 x ~8-1/2 x 3-1/8 in, two COB engines
    "dsxf1": Head("dsxf1", px(8.875), px(8.5), px(3.125), dsxf_head(0.42, 6)),
    # DSXF2: 12-7/8 x 7-3/4 x 3-1/8 in, landscape, four engines
    "dsxf2": Head("dsxf2", px(12.875), px(7.75), px(3.125), dsxf_head(0.5, 9)),
    # DSXF3: 13 x 13-5/8 x 5 in, six engines
    "dsxf3": Head("dsxf3", px(13.0), px(13.625), px(5.0), dsxf_head(0.45, 9)),
    # TFL: 16 x 15-3/4 x 6 in
    "tfl": Head("tfl", px(16.0), px(15.75), px(6.0), tfl_head),
    # EFM: ~16.4 x 15.7 x 5.5 in
    "efm": Head("efm", px(16.4), px(15.7), px(5.5), efm_head),
}


# --------------------------------------------------------------------------- the mounts
def hub(m, centre, r=0.8, half=0.75):
    """A knuckle's hub, its bolt across x."""
    for sx in (1.0, -1.0):
        sl.cylinder_axis(m, centre, (sx, 0.0, 0.0), r, 0.0, half, "body", sides=10)


def disc_down(m, cx, cz, r, y, sides):
    """The underside of a cylinder_y (which caps only its top), so a base or a sleeve is closed
    from below too."""
    ring = [(cx + r * math.cos(2 * math.pi * j / sides), y, cz + r * math.sin(2 * math.pi * j / sides))
            for j in range(sides)]
    m.poly_out("body", ring, [sl.swatch_uv(0.5 + 0.5 * math.cos(2 * math.pi * j / sides),
                                           0.5 + 0.5 * math.sin(2 * math.pi * j / sides))
                              for j in range(sides)], (cx, y + 1.0, cz))


def unit(v):
    n = math.sqrt(sum(k * k for k in v)) or 1.0
    return tuple(k / n for k in v)


def knuckle_head(mesh, head, pitch, knuckle):
    """The head on a knuckle at `knuckle`: a short neck from the hub up into the bottom of the
    head, near its back, the head turned to `pitch` about the hub."""
    r = rot_x(pitch)
    attach = (0.0, -head.h / 2, 0.15 * head.d)
    neck = 1.1
    low = r((0.0, -head.h / 2 - neck, 0.15 * head.d))
    t = tuple(knuckle[k] - low[k] for k in range(3))
    head.part().emit(mesh, compose(move(*t), r))
    top = tuple(r(attach)[k] + t[k] for k in range(3))
    axis = unit(tuple(top[k] - knuckle[k] for k in range(3)))
    length = math.sqrt(sum((top[k] - knuckle[k]) ** 2 for k in range(3)))
    sl.cylinder_axis(mesh, knuckle, axis, 0.5, 0.0, length + 0.25, "body", sides=10)
    hub(mesh, knuckle)


def yoke_extent(head, pitch):
    r = rot_x(pitch)
    pts = [r(p) for p in head.part().points()]
    return [min(p[k] for p in pts) for k in range(3)], [max(p[k] for p in pts) for k in range(3)]


def yoke_head(mesh, head, pitch, axis):
    """The head on its trunnion bolts at `axis`, turned to `pitch`, and the bolt knobs."""
    r = rot_x(pitch)
    head.part().emit(mesh, compose(move(*axis), r))
    arm_x = head.w / 2 + 0.45
    for sx in (1.0, -1.0):
        sl.cylinder_axis(mesh, (8.0 + sx * (arm_x + 0.2), axis[1], axis[2]), (sx, 0.0, 0.0), 0.55,
                         0.0, 0.35, "body", sides=10)
    return arm_x


def build(head, mount, surface):
    """One block's model: `head` on `mount` (kn, yk, sf) for `surface` (wall, ground, crossarm)."""
    m = Mesh()
    if surface in ("wall", "crossarm"):
        pitch = 35.0
        if mount == "kn":
            py = 4.6
            open_box(m, (6.6, py - 1.4, PLATE_Z), (9.4, py + 1.4, sl.REAR_Z), skip=())
            sl.cylinder_axis(m, (8.0, py, PLATE_Z), (0.0, 0.0, -1.0), 0.55, 0.0, 2.0, "body",
                             sides=10)
            knuckle_head(m, head, pitch, (8.0, py, PLATE_Z - 2.4))
        elif mount == "yk":
            lo, hi = yoke_extent(head, pitch)
            za = PLATE_Z - 0.4 - hi[2]
            py = max(0.9 - lo[1], 8.0 - (hi[1] + lo[1]) / 2)
            arm_x = yoke_head(m, head, pitch, (8.0, py, za))
            open_box(m, (8.0 - arm_x - 0.4, py - 0.7, PLATE_Z), (8.0 + arm_x + 0.4, py + 0.7, sl.REAR_Z),
                     skip=())
            for sx in (1.0, -1.0):
                x = 8.0 + sx * arm_x
                open_box(m, (x - 0.2, py - 0.6, za - 0.6), (x + 0.2, py + 0.6, PLATE_Z), skip=("south",))
        else:
            # on an arm's end: a squared slipfitter swallowing the arm (sl.ARM_TIP), a knuckle on
            # its front and the head over it
            open_box(m, (6.2, -0.5, 12.6), (9.8, 3.3, sl.REAR_Z), skip=())
            sl.cylinder_axis(m, (8.0, 1.4, 12.6), (0.0, 0.0, -1.0), 0.55, 0.0, 0.8, "body", sides=10)
            knuckle_head(m, head, pitch, (8.0, 1.4, 11.4))
        if surface == "crossarm":
            crossarm_bracket(m, py if mount != "sf" else 1.4)
        return m
    # ground: on top of a block; the slipfitter's is a pole-top tenon
    if mount == "sf":
        sl.cylinder_y(m, 8.0, 8.0, 1.25, FLOOR_Y, 3.0, "body", sides=12)
        disc_down(m, 8.0, 8.0, 1.25, FLOOR_Y, 12)
        sl.cylinder_y(m, 8.0, 8.0, 0.6, 3.0, 3.6, "body", sides=10)
        knuckle_head(m, head, 35.0, (8.0, 4.0, 8.0))
        return m
    pitch = -50.0
    if mount == "kn":
        sl.cylinder_y(m, 8.0, 8.0, 1.6, FLOOR_Y, 0.55, "body", sides=12)
        disc_down(m, 8.0, 8.0, 1.6, FLOOR_Y, 12)
        sl.cylinder_y(m, 8.0, 8.0, 0.55, 0.55, 1.6, "body", sides=10)
        knuckle_head(m, head, pitch, (8.0, 2.1, 8.0))
    else:
        lo, hi = yoke_extent(head, pitch)
        ya = 1.0 - lo[1]
        arm_x = yoke_head(m, head, pitch, (8.0, ya, 8.0))
        open_box(m, (8.0 - arm_x - 0.2, FLOOR_Y, 7.3), (8.0 + arm_x + 0.2, 0.7, 8.7), skip=())
        for sx in (1.0, -1.0):
            x = 8.0 + sx * arm_x
            open_box(m, (x - 0.2, 0.7, 7.4), (x + 0.2, ya + 0.6, 8.6), skip=("down",))
    centre_z(m)
    return m


def crossarm_bracket(m, py):
    """The bracket to a RealGrid crossarm: an arm flush against a pole at the top back of its
    block, 6 px square (y 10..16, z 10..16 of the cell behind this one), so its front face is
    10 px behind this cell's back. A bar from the plate back under the arm's face and a strap up
    it, bolted on."""
    open_box(m, (7.3, py - 0.5, sl.REAR_Z), (8.7, py + 0.5, 25.4), skip=("south",))
    open_box(m, (7.0, py - 0.5, 25.4), (9.0, 15.5, 25.9), skip=())


def centre_z(m):
    """Slide a ground model along z so it sits in the middle of its block (a ground flood stands
    where its base is, and a head tilted back would otherwise run out of the back of the cell)."""
    zs = [v[2] for v in m.v]
    shift = 8.0 - (min(zs) + max(zs)) / 2
    m.v = [(x, y, z + shift) for x, y, z in m.v]


def ceiling_of(ground):
    """A ceiling flood is the ground one turned over: 180 degrees about z keeps it aimed north,
    now 50 degrees down."""
    m = Mesh()
    m.v = [(16.0 - x, 16.0 - y, z) for x, y, z in ground.v]
    m.vt, m.faces = ground.vt, ground.faces
    m.vn = [(-x, -y, z) for x, y, z in ground.vn]
    return m


# --------------------------------------------------------------------------- the catalogue
FAMILIES = [
    # head, registry prefix, display name, body swatch, beam range (blocks)
    ("dsxf1", "lithdsxf1", "Lithonia DSXF1 LED Floodlight", "streetlight_body_dark_bronze", 16),
    ("dsxf2", "lithdsxf2", "Lithonia DSXF2 LED Floodlight", "streetlight_body_dark_bronze", 20),
    ("dsxf3", "lithdsxf3", "Lithonia DSXF3 LED Floodlight", "streetlight_body_dark_bronze", 24),
    ("tfl", "lithtfl", "Lithonia TFL Metal Halide Floodlight", "streetlight_body_dark_bronze", 24),
    ("efm", "geefm", "GE Evolve EFM LED Floodlight", "streetlight_body_black", 28),
]
MOUNTS = [("kn", "Knuckle"), ("yk", "Yoke"), ("sf", "Slipfitter")]
# the surfaces each mount has, with the Java surface and the block suffix
SURFACES = {
    "kn": [("wall", "WALL", ""), ("ground", "GROUND", "_g"), ("ceiling", "CEILING", "_c"),
           ("crossarm", "CROSSARM", "_x")],
    "yk": [("wall", "WALL", ""), ("ground", "GROUND", "_g"), ("ceiling", "CEILING", "_c"),
           ("crossarm", "CROSSARM", "_x")],
    "sf": [("wall", "WALL", ""), ("ground", "POLE_TOP", "_g")],
}
TRANSLATIONS = {
    # en: (de, es, sv)
    "Lithonia DSXF1 LED Floodlight": ("Lithonia DSXF1 LED-Flutlicht", "Reflector LED Lithonia DSXF1",
                                      "Lithonia DSXF1 LED-strålkastare"),
    "Lithonia DSXF2 LED Floodlight": ("Lithonia DSXF2 LED-Flutlicht", "Reflector LED Lithonia DSXF2",
                                      "Lithonia DSXF2 LED-strålkastare"),
    "Lithonia DSXF3 LED Floodlight": ("Lithonia DSXF3 LED-Flutlicht", "Reflector LED Lithonia DSXF3",
                                      "Lithonia DSXF3 LED-strålkastare"),
    "Lithonia TFL Metal Halide Floodlight": ("Lithonia TFL Halogen-Metalldampf-Flutlicht",
                                             "Reflector de Halogenuro Metálico Lithonia TFL",
                                             "Lithonia TFL metallhalogenstrålkastare"),
    "GE Evolve EFM LED Floodlight": ("GE Evolve EFM LED-Flutlicht", "Reflector LED GE Evolve EFM",
                                     "GE Evolve EFM LED-strålkastare"),
    "Knuckle": ("Gelenkhalterung", "Rótula", "Ledfäste"),
    "Yoke": ("Bügel", "Horquilla", "Bygel"),
    "Slipfitter": ("Aufsteckmuffe", "Acoplador Deslizante", "Rörfäste"),
}


def blocks():
    """Every block: (registry, main registry, display, head, mount, surface, java surface,
    body, range, mesh)."""
    out = []
    for head, prefix, display, body, rng in FAMILIES:
        for mount, mount_name in MOUNTS:
            main = prefix + mount
            meshes = {}
            for surface, java, suffix in SURFACES[mount]:
                if surface == "ceiling":
                    mesh = ceiling_of(meshes["ground"])
                else:
                    mesh = build(HEADS[head], mount, surface)
                meshes[surface] = mesh
                out.append((main + suffix, main, "%s (%s)" % (display, mount_name), head, mount,
                            surface, java, body, rng, mesh))
    return out


def bounds(mesh):
    """The model's box in blocks, clamped to its own cell (the crossarm bracket runs on into the
    arm's cell; the selection box stays in this one)."""
    lo = [max(0.0, min(v[k] for v in mesh.v) / 16.0) for k in range(3)]
    hi = [min(1.0, max(v[k] for v in mesh.v) / 16.0) for k in range(3)]
    return [round(math.floor(c * 64) / 64, 6) for c in lo] + [round(math.ceil(c * 64) / 64, 6) for c in hi]


def check_envelope(registry, surface, mesh):
    for x, y, z in mesh.v:
        zmax = 26.0 if surface == "crossarm" else sl.REAR_Z
        assert -1.0 <= x <= 17.0 and -1.0 <= y <= 17.0 and -2.0 <= z <= zmax + 1e-9, \
            "%s leaves its block at %s" % (registry, (x, y, z))


# --------------------------------------------------------------------------- textures
COB = {"dsxf1": (2, 1), "dsxf2": (4, 1), "dsxf3": (3, 2)}


def lens_name(head, state):
    return "floodlight_lens_%s_%s" % (head, state)


def cob_png(head, lit):
    """A D-Series optics window: dark trim round a reflector field, each COB engine a round
    phosphor die in a square optic. Lit the dies are near white and the field glows; unlit the
    dies show their yellow phosphor, the whole at about half (unshaded)."""
    cols, rows = COB[head]
    img = Image.new("RGBA", (32, 32), (40, 38, 36, 255))
    p = img.load()
    field = (186, 186, 182) if lit else (64, 64, 66)
    for y in range(1, 31):
        for x in range(1, 31):
            p[x, y] = field + (255,)
    cw, ch = 30 // cols, 30 // rows
    for i in range(cols):
        for j in range(rows):
            cx, cy = 1 + i * cw + cw / 2.0, 1 + j * ch + ch / 2.0
            r_opt = min(cw, ch) * 0.42
            r_die = min(cw, ch) * 0.2
            for y in range(32):
                for x in range(32):
                    dx, dy = x + 0.5 - cx, y + 0.5 - cy
                    d = math.sqrt(dx * dx + dy * dy)
                    if d <= r_die:
                        p[x, y] = ((255, 252, 236) if lit else (160, 128, 40)) + (255,)
                    elif d <= r_opt:
                        k = 1.0 - 0.4 * (d - r_die) / (r_opt - r_die)
                        base = (236, 236, 228) if lit else (92, 92, 94)
                        p[x, y] = tuple(int(c * k) for c in base) + (255,)
    return img


def hid_png(lit):
    """The TFL's glass over its reflector: a hot horizontal arc tube in the middle, the reflector
    in concentric facets round it. Unlit, grey glass over a dark reflector."""
    img = Image.new("RGBA", (32, 32))
    p = img.load()
    for y in range(32):
        for x in range(32):
            dx, dy = (x + 0.5 - 16) / 16, (y + 0.5 - 16) / 16
            d = math.sqrt(dx * dx * 0.7 + dy * dy * 1.6)
            facet = 0.9 if int(d * 7) % 2 else 1.0
            if lit:
                arc = abs(dy) < 0.09 and abs(dx) < 0.45
                if arc:
                    col = (255, 255, 246)
                else:
                    k = max(0.0, 1.0 - d)
                    col = tuple(int(min(255, (150 + 105 * k) * facet * c)) for c in (1.0, 0.98, 0.9))
            else:
                k = 0.55 + 0.2 * (1.0 - min(1.0, d))
                col = tuple(int(c * k * facet) for c in (120, 120, 118))
            p[x, y] = col + (255,)
    return img


def lens_png(head, lit):
    if head in COB:
        return cob_png(head, lit)
    if head == "tfl":
        return hid_png(lit)
    return sl.led_array_png(4, 4, "lens", "on" if lit else "off")


def emissive_of(img, threshold=200):
    """OptiFine's `_e` companion: only the pixels lit like a lamp (all channels bright), the
    rest transparent, so the glow stays on the LEDs and the lamp and not on the trim."""
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    src, dst = img.load(), out.load()
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            r, g, b, a = src[x, y]
            if min(r, g, b) >= threshold:
                dst[x, y] = (r, g, b, a)
    return out


def all_textures():
    out = {}
    for head in [f[0] for f in FAMILIES]:
        on = lens_png(head, True)
        out[lens_name(head, "on")] = on
        out[lens_name(head, "on") + "_e"] = emissive_of(on)
        out[lens_name(head, "off")] = lens_png(head, False)
    return out


# --------------------------------------------------------------------------- blockstates
def blockstate(registry, head, body):
    model = "csm:lighting/shared_models/floodlight_%s.obj" % registry
    lit = {"textures": {"#" + LENS_MAT: TEX + lens_name(head, "on")}}
    unlit = {"textures": {"#" + LENS_MAT: TEX + lens_name(head, "off")}}
    return {
        "forge_marker": 1,
        "defaults": {
            "model": model,
            "custom": {"flip-v": True},
            "textures": {"#body": TEX + body, "#" + LENS_MAT: TEX + lens_name(head, "off")},
        },
        "variants": {
            "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}},
            "state": {"0": unlit, "1": lit, "2": unlit, "3": lit},
            "inventory": [{"transform": "forge:default-block"}],
        },
    }


def mtl_text():
    lines = ["# Generated by dev-env-utils/scripts/gen_floodlights.py -- do not edit",
             "# The floodlights' materials; each blockstate retextures them."]
    for mat, tex in (("body", "streetlight_body_dark_bronze"), (LENS_MAT, lens_name("dsxf3", "off"))):
        lines += ["newmtl %s" % mat, "map_Kd %s%s" % (TEX, tex), ""]
    return "\n".join(lines)


# --------------------------------------------------------------------------- output
def outputs():
    files = {MODEL_DIR + "/" + MTL_NAME: mtl_text().encode()}
    for registry, main, display, head, mount, surface, java, body, rng, mesh in blocks():
        check_envelope(registry, surface, mesh)
        files[MODEL_DIR + "/floodlight_%s.obj" % registry] = mesh.text(
            "%s, %s" % (display, surface)).encode()
        files["blockstates/%s.json" % registry] = (
            json.dumps(blockstate(registry, head, body), indent=2) + "\n").encode()
    for name, img in all_textures().items():
        buf = io.BytesIO()
        img.save(buf, "PNG")
        files[TEX_DIR + "/" + name + ".png"] = buf.getvalue()
    return files


def fragments():
    """The tab lines (visible blocks for CsmTabLighting, companions for CsmTabLightingHidden)
    and the lang lines."""
    vis, hidden, lang = [], [], {}
    for registry, main, display, head, mount, surface, java, body, rng, mesh in blocks():
        b = bounds(mesh)
        box = "new AxisAlignedBB(%s)" % ", ".join("%.6f" % c for c in b)
        if java == "POLE_TOP":
            line = "    initTabBlock(new BlockFloodlight.PostTop(\"%s\", %s, %d));" % (registry, box, rng)
        else:
            line = "    initTabBlock(new BlockFloodlight(\"%s\", BlockFloodlight.Surface.%s, %s, %d));" % (
                registry, java, box, rng)
        (vis if registry == main else hidden).append(line)
        # a companion carries its flood's name (a probe or a tooltip shows it)
        fam, mount_name = display.rsplit(" (", 1)
        mount_name = mount_name[:-1]
        de, es, sv = TRANSLATIONS[fam]
        mde, mes, msv = TRANSLATIONS[mount_name]
        lang[registry] = (display, "%s (%s)" % (de, mde), "%s (%s)" % (es, mes),
                          "%s (%s)" % (sv, msv))
    return vis, hidden, lang


def main(argv):
    if "--fragments" in argv:
        vis, hidden, lang = fragments()
        print("// CsmTabLighting\n" + "\n".join(vis))
        print("// CsmTabLightingHidden\n" + "\n".join(hidden))
        for reg, names in lang.items():
            print("tile.%s.name=%s" % (reg, " | ".join(names)))
        return 0
    check = "--check" in argv
    drift = []
    for rel, data in outputs().items():
        path = layout.asset_for_write(layout.owner_of_folder(OWNER), rel)
        if check:
            if not sl.same(path, data):
                drift.append(rel)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(data)
        print("wrote", os.path.relpath(path, layout.REPO_ROOT))
    if check:
        if drift:
            print("gen_floodlights: %d file(s) drifted:\n  %s" % (len(drift), "\n  ".join(drift)))
            return 1
        print("gen_floodlights: up to date")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
