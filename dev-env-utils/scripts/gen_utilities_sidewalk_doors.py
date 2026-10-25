#!/usr/bin/env python3
"""Every asset the Utilities tab's sidewalk doors (#310) ship: steel and aluminium doors set flush
in the pavement over a vault or a utility tunnel, in four styles and six sizes.

    python dev-env-utils/scripts/gen_utilities_sidewalk_doors.py
    python dev-env-utils/scripts/gen_utilities_sidewalk_doors.py --check
    python dev-env-utils/scripts/gen_utilities_sidewalk_doors.py --fragments   # tab lines

A door is BlockSidewalkDoor (package powergrid.sewer): one block a cell, every cell the same
block, its place in the door read from its neighbours as actual state (``left``, ``right``,
``front``, ``back``: the door carries on to the model's west, east, north, south). So each part
here is drawn once, for one cell facing north, and the multipart blockstate picks and turns it:

- **the edge**: the concrete collar running down the cell and the steel angle frame on its top,
  drawn on the model's north side and turned to every side the door stops on (uvlock, so where
  two sides' frames cross at a corner they draw the same texels);
- **the plate**, shut: the leaf's top a fifth of a pixel under the frame, split by the seam where
  two leaves meet (between the two cells of a door two wide, down the middle of the middle cell
  of one three wide; a single leaf has none);
- **the standing leaf**, open: on the model's west edge (and, turned 180, the east edge of a
  double door), as tall as the leaf is wide -- 1 or 1.5 blocks for a double door, 2 or 3 for a
  single one -- with its top face outward, its stiffener rib and hinge knuckles. A JSON element
  reaches one block above its cell at most, so the leaf and its struts are OBJ, cut into one
  block slices so every face's uv stays inside its sprite;
- **the struts**, open: gas struts on the single leaf, hold-open arms on the others, one near
  each end of the door (``strut_a`` where the door stops to the model's north, ``strut_b`` to
  its south; on the turned east leaf the two swap).

Sizes are named width x length: width across the leaves (2 or 3), length along the hinges (1 to
4). The placer stands to the model's north; the door runs to their right and away from them.
"""
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_utilities_meters as um  # noqa: E402
import gen_utilities_water as gw  # noqa: E402
import life_safety_gen_common as lc  # noqa: E402

GEN = "gen_utilities_sidewalk_doors.py"
TAB = "CsmTabUtilities"
FACINGS = um.FACINGS
SIDES = (("front", 0), ("right", 90), ("back", 180), ("left", 270))

C = um.Catalogue(GEN, "utilities/sidewalk", "utilities/sidewalk", assets=um.ASSETS)
box = um.box
Poly = gw.Poly


def T(name):
    return C.T(name)


def W(name):
    """One of the water system's textures, shared rather than drawn again."""
    return "csm:blocks/utilities/water/" + name


PRECAST = "csm:blocks/utilities/sewer/precast"

# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
BLACK_PAINT = (34, 35, 37)
GALV = (150, 156, 158)
LEAF_PAINT = (58, 54, 50)     # the single leaf's dark, speckled paint (image 1 of #310)


def diamond(base, lift, seed):
    """Diamond tread plate: raised diamonds on a two-by-four lattice, lit on their upper edge,
    as the water system's aluminium plate is drawn, in another metal."""
    img = lc.fill(base, 16, 2, seed)
    for y in range(16):
        for x in range(16):
            if (x + 2 * y) % 8 == 0 or (x - 2 * y) % 8 == 4:
                c = tuple(min(255, int(v * lift[0] + lift[1])) for v in base)
                img.putpixel((x, y), c + (255,))
    return img


C.textures["diamond_black"] = lambda: diamond(BLACK_PAINT, (1.0, 26), 3101)
C.textures["diamond_galv"] = lambda: diamond(GALV, (1.12, 0), 3102)
C.textures["paint_black"] = lambda: lc.fill(BLACK_PAINT, 16, 3, 3103)


@C.texture("galv")
def _galv():
    """Hot-dip galvanized steel: grey with the spangle's lighter and darker flakes."""
    img = lc.fill(GALV, 16, 4, 3104)
    rnd = __import__("random").Random(3105)
    for _ in range(14):
        x, y = rnd.randrange(16), rnd.randrange(16)
        k = rnd.choice((0.9, 1.1))
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            img.putpixel(((x + dx) % 16, (y + dy) % 16), lc.shade(GALV, k) + (255,))
    return img


@C.texture("leaf_paint")
def _leaf_paint():
    """The single leaf's paint: dark brown-grey with faint light flecks, a textured coating."""
    img = lc.fill(LEAF_PAINT, 16, 4, 3106)
    rnd = __import__("random").Random(3107)
    for _ in range(10):
        x, y = rnd.randrange(16), rnd.randrange(16)
        img.putpixel((x, y), lc.shade(LEAF_PAINT, rnd.choice((1.15, 1.25))) + (255,))
    return img


# Per style: the plate's top, its underside (seen open), the frame, the struts' kind
STYLES = (
    ("aluminium", "Aluminium", ("Aluminium", "Aluminium", "Aluminio", "Aluminium"),
     {"plate": W("diamond_plate"), "under": W("aluminium"), "frame": W("aluminium")},
     "arm", False),
    ("black", "Black Steel", ("Black Steel", "Stahl schwarz", "Acero Negro", "Svart stål"),
     {"plate": T("diamond_black"), "under": T("paint_black"), "frame": T("paint_black")},
     "arm", False),
    ("galvanized", "Galvanized", ("Galvanized", "Feuerverzinkt", "Galvanizado", "Galvaniserad"),
     {"plate": T("diamond_galv"), "under": T("galv"), "frame": T("galv")},
     "arm", False),
    ("single", "Single Leaf", ("Single Leaf", "Einflügelig", "Una Hoja", "Enkeldörr"),
     {"plate": T("leaf_paint"), "under": T("leaf_paint"), "frame": W("steel")},
     "gas", True),
)
SIZES = ((2, 1), (2, 2), (2, 3), (2, 4), (3, 3), (3, 4))

COLLAR = 1.5      # the concrete collar's wall and the frame on it, as the access hatch's
LEAF_Y = 15.2     # the plate's underside
PLATE_TOP = 15.8  # a fifth of a pixel under the frame's top, so the two never fight
SEAM = 0.2        # half the gap where two leaves meet


def tex(style_tex):
    t = {"concrete": PRECAST, "dark": W("dark"), "steel": W("steel"),
         "particle": style_tex["plate"]}
    t.update(style_tex)
    return t


def model(t, els, display=None):
    m = {"parent": "block/block", "textures": dict(t)}
    if els:
        m["elements"] = els
    if display:
        m["display"] = display
    return m


# ------------------------------------------------------------------------------------------
# The JSON parts: the edge and the shut plate
# ------------------------------------------------------------------------------------------
def edge_els():
    c = COLLAR
    return [box([0, 0, 0], [16, LEAF_Y, c], "concrete", faces=("south", "down", "east", "west")),
            box([0, LEAF_Y, 0], [16, 16, c], "frame", faces=("up", "south", "east", "west"))]


def plate_els(seam):
    """The shut plate of one cell. ``seam``: None, "east", "west" or "mid", where the gap between
    the leaves runs in this cell; under the gap a dark strip stands for the shaft below."""
    spans = {None: [(0, 16)], "east": [(0, 16 - SEAM)], "west": [(SEAM, 16)],
             "mid": [(0, 8 - SEAM), (8 + SEAM, 16)]}[seam]
    els = []
    for x0, x1 in spans:
        faces = ["up", "down"]
        if x0 > 0:
            faces.append("west")
        if x1 < 16:
            faces.append("east")
        els.append(box([x0, LEAF_Y, 0], [x1, PLATE_TOP, 16], "plate", faces=tuple(faces),
                        per={"down": "under", "east": "frame", "west": "frame"}))
    gap = {"east": (16 - SEAM, 16), "west": (0, SEAM), "mid": (8 - SEAM, 8 + SEAM)}.get(seam)
    if gap:
        els.append(box([gap[0], LEAF_Y - 0.6, 0], [gap[1], LEAF_Y - 0.5, 16], "dark",
                       faces=("up",)))
    return els


# ------------------------------------------------------------------------------------------
# OBJ: the standing leaf and its struts, in block units, the model facing north
# ------------------------------------------------------------------------------------------
OBJ_QUADS = {}
MTL = "sidewalk_doors"
MATERIALS = {}


def material(texture):
    """The OBJ material naming a texture, made on first use."""
    for k, v in MATERIALS.items():
        if v == texture:
            return k
    k = "m%d" % len(MATERIALS)
    MATERIALS[k] = texture
    return k


FACE_DEFS = {
    # name: (axis, side, the face's (u axis, u sign) and (v axis, v sign) in the texture)
    "west": (0, 0, (2, 1), (1, -1)), "east": (0, 1, (2, -1), (1, -1)),
    "north": (2, 0, (0, -1), (1, -1)), "south": (2, 1, (0, 1), (1, -1)),
    "down": (1, 0, (0, 1), (2, -1)), "up": (1, 1, (0, 1), (2, 1)),
}


def obj_box(lo, hi, mats, faces=None):
    """A box from ``lo`` to ``hi`` (blocks) as polygons, each face cut at the whole blocks so its
    uv, its place in that block as a JSON face's is, stays inside the sprite. ``mats`` maps a
    face name to its texture, ``"*"`` for the rest."""
    polys = []
    for name, (axis, side, (ua, us), (va, vs)) in FACE_DEFS.items():
        if faces is not None and name not in faces:
            continue
        t = mats.get(name, mats.get("*"))
        if t is None:
            continue
        mat = material(t)
        a, b = [i for i in range(3) if i != axis]
        cuts = []
        for ax in (a, b):
            pts = [lo[ax]]
            k = math.floor(lo[ax]) + 1
            while k < hi[ax] - 1e-9:
                pts.append(k)
                k += 1
            pts.append(hi[ax])
            cuts.append(pts)
        fixed = hi[axis] if side else lo[axis]
        normal = [0, 0, 0]
        normal[axis] = 1 if side else -1
        for i in range(len(cuts[0]) - 1):
            for j in range(len(cuts[1]) - 1):
                p0, p1 = cuts[0][i], cuts[0][i + 1]
                q0, q1 = cuts[1][j], cuts[1][j + 1]
                corners = []
                for pa, qb in ((p0, q0), (p1, q0), (p1, q1), (p0, q1)):
                    v = [0.0, 0.0, 0.0]
                    v[axis], v[a], v[b] = fixed, pa, qb
                    corners.append(tuple(v))
                base = [math.floor(min(x[k] for x in corners) + 1e-9) for k in range(3)]

                def uv(p, base=base):
                    def frac(ax, sign):
                        f = p[ax] - base[ax]
                        return f if sign > 0 else 1 - f
                    return (frac(ua, us), frac(va, vs))
                polys.append(Poly(corners, [uv(p) for p in corners], [tuple(normal)] * 4, mat,
                                  outward=tuple(normal)))
    return polys


def obj_bar(p0, p1, w, h, t):
    """A bar of section w by h (blocks) from p0 to p1, its h side kept horizontal across the
    door (the struts move in the model's x-y plane), stretched over the texture."""
    d = [p1[i] - p0[i] for i in range(3)]
    n = math.sqrt(sum(v * v for v in d))
    d = [v / n for v in d]
    side = (0.0, 0.0, 1.0)
    up = (d[1] * side[2] - d[2] * side[1], d[2] * side[0] - d[0] * side[2],
          d[0] * side[1] - d[1] * side[0])
    mat = material(t)
    corners = []
    for su, sv in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
        corners.append([side[i] * su * h / 2 + up[i] * sv * w / 2 for i in range(3)])
    polys = []
    for k in range(4):
        c0, c1 = corners[k], corners[(k + 1) % 4]
        quad = [tuple(p0[i] + c0[i] for i in range(3)), tuple(p0[i] + c1[i] for i in range(3)),
                tuple(p1[i] + c1[i] for i in range(3)), tuple(p1[i] + c0[i] for i in range(3))]
        mid = [(c0[i] + c1[i]) / 2 for i in range(3)]
        m = math.sqrt(sum(v * v for v in mid)) or 1
        nrm = tuple(v / m for v in mid)
        polys.append(Poly(quad, [(0, 0), (1, 0), (1, 1), (0, 1)], [nrm] * 4, mat, outward=nrm))
    for end, sign in ((p0, -1), (p1, 1)):
        quad = [tuple(end[i] + c[i] for i in range(3)) for c in corners]
        nrm = tuple(v * sign for v in d)
        polys.append(Poly(quad, [(0, 0), (1, 0), (1, 1), (0, 1)], [nrm] * 4, mat, outward=nrm))
    return polys


def write_obj(name, polys):
    o = gw.Obj(MTL, name)
    for p in polys:
        o.add(p)
    C.extra["models/block/utilities/sidewalk/%s.obj" % name] = o.text().replace(
        "gen_utilities_water.py", GEN, 1)
    OBJ_QUADS[name] = o.count
    return "csm:utilities/sidewalk/%s.obj" % name


P = 1 / 16.0
LEAF_X0, LEAF_X1, RIB_X1 = 0.4 * P, 1.4 * P, 2.4 * P   # BlockSidewalkDoor's LEAF_X0, LEAF_X1


def standing_leaf(t, height):
    """The leaf stood on the model's west edge, ``height`` blocks tall: its top face (the plate)
    outward, the underside inward with a stiffener rib up the middle and a channel along its
    free edge, and the hinge knuckles at its foot."""
    top = 1 + height
    polys = obj_box((LEAF_X0, 1, 0), (LEAF_X1, top, 1),
                    {"west": t["plate"], "east": t["under"], "*": t["frame"]},
                    faces=("west", "east", "up", "north", "south"))
    polys += obj_box((LEAF_X1, 1 + 1 * P, 7 * P), (RIB_X1, top - 1 * P, 9 * P),
                     {"*": t["under"]}, faces=("east", "up", "down", "north", "south"))
    polys += obj_box((LEAF_X1, top - 1.5 * P, 0), (RIB_X1, top, 1), {"*": t["under"]},
                     faces=("east", "down", "up"))
    for z0 in (2.5, 12.5):
        polys += obj_box((0, 1 - 0.6 * P, z0 * P), (1.8 * P, 1 + 0.6 * P, (z0 + 1.5) * P),
                         {"*": W("steel")}, faces=("west", "east", "up", "north", "south"))
    return polys


def strut(t, height, kind, z):
    """A strut from the leaf's underside down to the collar, at ``z`` blocks along the door:
    a gas strut (black body, polished rod) or a flat hold-open arm."""
    # on the leaf's face a third of the way up, to a bracket on the collar below the frame
    top = (LEAF_X1, 1 + height * 0.4, z)
    foot = (min(14.0 * P, LEAF_X1 + height * 0.5), 1 - 1.2 * P, z)
    if kind == "gas":
        mid = tuple(top[i] + (foot[i] - top[i]) * 0.45 for i in range(3))
        return (obj_bar(top, mid, 0.35 * P, 0.35 * P, W("aluminium"))
                + obj_bar(mid, foot, 0.8 * P, 0.8 * P, W("dark")))
    return obj_bar(top, foot, 1.0 * P, 0.35 * P, t["frame"])


# ------------------------------------------------------------------------------------------
# The blocks
# ------------------------------------------------------------------------------------------
def rule(name, when, y, uvlock=False):
    r = {"apply": {"model": name if ":" in name else C.M(name)}, "when": when}
    if y % 360:
        r["apply"]["y"] = y % 360
    if uvlock:
        r["apply"]["uvlock"] = True
    return r


def when(facing, **cond):
    w = {"facing": facing}
    for k, v in cond.items():
        w[k] = "true" if v else "false"
    return w


def icon(t, width, length, single):
    """The item: the door shut, scaled to fit a block, on a low concrete collar."""
    s = 14.0 / max(width, length)
    w, ln = width * s, length * s
    x0, z0 = 8 - w / 2, 8 - ln / 2
    x1, z1 = x0 + w, z0 + ln
    c = 1.0
    els = [box([x0, 0, z0], [x1, 4, z1], "concrete", faces=("north", "south", "east", "west",
                                                              "down")),
           box([x0, 4, z0], [x1, 5, z0 + c], "frame"), box([x0, 4, z1 - c], [x1, 5, z1], "frame"),
           box([x0, 4, z0 + c], [x0 + c, 5, z1 - c], "frame"),
           box([x1 - c, 4, z0 + c], [x1, 5, z1 - c], "frame"),
           box([x0 + c, 4, z0 + c], [x1 - c, 4.8, z1 - c], "plate", faces=("up",))]
    if not single:
        els.append(box([7.8, 4.8, z0 + c], [8.2, 4.85, z1 - c], "dark", faces=("up",)))
    return model(t, els, gw.ICON_DISPLAY)


def doors():
    for key, title, names, style_tex, kind, single in STYLES:
        t = tex(style_tex)
        models = {"%s_edge" % key: model(t, edge_els())}
        seams = (None,) if single else (None, "east", "west", "mid")
        for seam in seams:
            models["%s_plate%s" % (key, "_" + seam if seam else "")] = model(t, plate_els(seam))
        for width, length in SIZES:
            height = width if single else width / 2.0
            hk = ("%g" % height).replace(".", "_")
            leaf = "%s_leaf_%s" % (key, hk)
            if leaf + ".obj" not in [k.split("/")[-1] for k in C.extra]:
                write_obj(leaf, standing_leaf(t, height))
                write_obj(leaf + "_strut_a", strut(t, height, kind, 2.0 * P))
                write_obj(leaf + "_strut_b", strut(t, height, kind, 14.0 * P))
            leaf_m = "csm:utilities/sidewalk/%s.obj" % leaf
            rules = []
            for f, fy in FACINGS:
                for side, sy in SIDES:
                    rules.append(rule("%s_edge" % key, when(f, **{side: False}), fy + sy, True))
                if single:
                    rules.append(rule("%s_plate" % key, when(f, open=False), fy))
                elif width == 2:
                    rules.append(rule("%s_plate_east" % key, when(f, open=False, left=False), fy))
                    rules.append(rule("%s_plate_west" % key, when(f, open=False, right=False),
                                      fy))
                else:
                    rules.append(rule("%s_plate_mid" % key,
                                      when(f, open=False, left=True, right=True), fy))
                    rules.append(rule("%s_plate" % key, when(f, open=False, left=False), fy))
                    rules.append(rule("%s_plate" % key, when(f, open=False, right=False), fy))
                # the west leaf, and a double door's east leaf, the same turned 180
                leaves = [(fy, "left", "front", "back")]
                if not single:
                    leaves.append((fy + 180, "right", "back", "front"))
                for y, hinge, near_a, near_b in leaves:
                    rules.append(rule(leaf_m, when(f, open=True, **{hinge: False}), y))
                    rules.append(rule(leaf_m.replace(".obj", "_strut_a.obj"),
                                      when(f, open=True, **{hinge: False, near_a: False}), y))
                    rules.append(rule(leaf_m.replace(".obj", "_strut_b.obj"),
                                      when(f, open=True, **{hinge: False, near_b: False}), y))
            reg = "sidewalk_door_%s_%dx%d" % (key, width, length)
            size = "%d x %d" % (width, length)
            full = ("Sidewalk Door (%s, %s)" % (names[0], size),
                    "Gehwegklappe (%s, %s)" % (names[1], size),
                    "Puerta de Acera (%s, %s)" % (names[2], size),
                    "Trottoarlucka (%s, %s)" % (names[3], size))
            C.add(reg, 'new BlockSidewalkDoor("%s", %d, %d, %s)'
                  % (reg, width, length, "true" if single else "false"),
                  full, models if (width, length) == SIZES[0] else {}, {"multipart": rules},
                  item=icon(t, width, length, single), tab=TAB)


def _mtl():
    text = ["# Generated by dev-env-utils/scripts/%s -- do not edit" % GEN]
    for mat, texture in sorted(MATERIALS.items()):
        text += ["newmtl " + mat, "map_Kd " + texture]
    C.extra["models/block/utilities/sidewalk/%s.mtl" % MTL] = "\n".join(text) + "\n"


doors()
_mtl()


if __name__ == "__main__":
    if "--report" in sys.argv:
        for k in sorted(OBJ_QUADS):
            print("%-36s %4d quads" % (k, OBJ_QUADS[k]))
        sys.exit(0)
    sys.exit(C.main())
