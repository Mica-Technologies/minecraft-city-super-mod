#!/usr/bin/env python3
"""
gen_venues_skate.py -- the Sports & Venues Skate Park tab.

    py -3 dev-env-utils/scripts/gen_venues_skate.py                    # write
    py -3 dev-env-utils/scripts/gen_venues_skate.py --check            # fail on drift
    py -3 dev-env-utils/scripts/gen_venues_skate.py --fragments        # the tab's lines
    py -3 dev-env-utils/scripts/gen_venues_skate.py --layout park --commands X Y Z [--rot R]

Skate park basics in smooth concrete, one block a metre: quarter pipes (a 1 m transition in one
block, a 2 m one in three), a bowl corner that joins two 1 m quarter pipes round an inside
corner, a kicker and a bank in two halves and a box top (a funbox is a bank, a box and a bank),
a grind rail and a ledge that join into runs. The curves are true, OBJ surfaces from the
transition's circle (the bowl corner a surface of revolution about its inside corner); the
collision is a staircase of boxes under them, steps of at most 4 px but the last one at the
top of a transition, so a player walks up what a skater would ride. Steel coping on every lip.
No brand.
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_venues_common as vc  # noqa: E402
import venues_units as vu  # noqa: E402
import gen_venues_courts as gc  # noqa: E402

GEN = "gen_venues_skate.py"
TAB = "Sports & Venues - Skate Park"
C = vc.catalogue(GEN, "skate")
T = C.T
M = C.M
U = vu.Units(C, GEN, "skate")
box = vc.box
FACINGS = vc.FACINGS
ys = vu.ys
CONCRETE = (186, 184, 178)
STEEL = (200, 204, 210)


def rgba(c, a=255):
    return tuple(c[:3]) + (a,)


@C.texture("concrete")
def _concrete():
    """Smooth trowelled concrete: a fine mottle, no grain."""
    import random
    from PIL import Image
    rng = random.Random(20261501)
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    for y in range(32):
        for x in range(32):
            k = 1.0 + rng.uniform(-0.025, 0.025) + 0.02 * math.sin((x + 2 * y) / 5.0)
            px[x, y] = rgba(vc.shade(CONCRETE, k))
    return img


C.textures["coping"] = lambda: vc.brushed(STEEL, 20261502)
C.textures["rail"] = lambda: vc.brushed((60, 64, 70), 20261503)
C.textures["black"] = lambda: vc.swatch((30, 30, 32), 20261504)

MATS = {"concrete": T("concrete"), "coping": T("coping")}

# ------------------------------------------------------------------------------------------
# Surfaces swept along x: a profile h(z) (pixels, model frame: the riding surface faces north,
# rising towards +z), cut to one block
# ------------------------------------------------------------------------------------------
N = 16


def poly(pts, mat, outward, uvs):
    return vc.Poly(pts, mat, outward, uvs)


def swept(h, z0, y0, front=True, back=True, top=True, sides=True):
    """The solid under h inside the block whose model-frame corner is (z0, y0): its riding
    surface, flat top, sides, back and front where they are outside."""
    def t(z):
        return max(0.0, min(16.0, h(z0 + z) - y0))
    zs = [16.0 * k / N for k in range(N + 1)]
    # where the solid starts within the block
    start = 0.0
    if t(0) <= 1e-9:
        lo, hi = 0.0, 16.0
        if t(16) <= 1e-9:
            return []
        for _ in range(40):
            mid = (lo + hi) / 2
            if t(mid) > 1e-9:
                hi = mid
            else:
                lo = mid
        start = hi
    pts = [start] + [z for z in zs if z > start + 1e-6]
    out = []
    for za, zb in zip(pts, pts[1:]):
        ta, tb = t(za), t(zb)
        if ta >= 16 - 1e-9 and tb >= 16 - 1e-9:
            if top:
                out.append(poly([(0, 16, zb), (16, 16, zb), (16, 16, za), (0, 16, za)], "concrete",
                                (0, 1, 0), [(0, zb / 16), (1, zb / 16), (1, za / 16), (0, za / 16)]))
        elif tb > 1e-9 or ta > 1e-9:
            dz, dy = zb - za, tb - ta
            n = (0, dz, -dy)
            out.append(poly([(0, tb, zb), (16, tb, zb), (16, ta, za), (0, ta, za)], "concrete", n,
                            [(0, zb / 16), (1, zb / 16), (1, za / 16), (0, za / 16)]))
        if sides:
            for x, nx in ((0.0, -1), (16.0, 1)):
                q = [(x, 0, za), (x, 0, zb), (x, tb, zb), (x, ta, za)]
                out.append(poly(q, "concrete", (nx, 0, 0),
                                [(p[2] / 16, 1 - p[1] / 16) for p in q]))
    if back and t(16) > 1e-9:
        q = [(16, 0, 16), (0, 0, 16), (0, t(16), 16), (16, t(16), 16)]
        out.append(poly(q, "concrete", (0, 0, 1), [(1 - p[0] / 16, 1 - p[1] / 16) for p in q]))
    if front and start < 1e-9 and t(0) > 1e-9:
        q = [(0, 0, 0), (16, 0, 0), (16, t(0), 0), (0, t(0), 0)]
        out.append(poly(q, "concrete", (0, 0, -1), [(p[0] / 16, 1 - p[1] / 16) for p in q]))
    return out


def circle(r):
    """A transition of radius r: flat at z = 0, vertical at z = r."""
    return lambda z: r - math.sqrt(max(0.0, r * r - min(z, r) ** 2)) if z < r else 1e6


def coping_x(y, z, r=1.0):
    return vu.tube((0, y, z), (16, y, z), r, "coping", caps=False, sides=8)


def steps(h, z0, y0, step=2):
    """Collision: a box a 2 px column under h, in the block whose corner is (z0, y0)."""
    out = []
    for k in range(0, 16, step):
        zm = k + step / 2.0
        top = max(0.0, min(16.0, h(z0 + zm) - y0))
        hi = int(round(top))
        if hi > 0:
            out.append((0, 0, k, 16, hi, k + step))
    return out


def flat(bs):
    return "new int[]{%s}" % ", ".join(", ".join("%d" % v for v in b) for b in bs)


# Quarter pipes: piece 0 the 1 m one; 1-3 the 2 m one's front, back and upper back blocks
QP_S = circle(16.0)
QP_L = circle(32.0)
QP_PIECES = [
    ("qp_small", swept(QP_S, 0, 0) + coping_x(16.0, 15.0), steps(QP_S, 0, 0)),
    ("qp_large_front", swept(QP_L, 0, 0, back=False), steps(QP_L, 0, 0)),
    ("qp_large_back", swept(QP_L, 16, 0, front=False, top=False), steps(QP_L, 16, 0)),
    ("qp_large_top", swept(QP_L, 16, 16, front=False) + coping_x(16.0, 15.0),
     steps(QP_L, 16, 16)),
]
for name, polys, _b in QP_PIECES:
    U.write_obj(name, "skate", polys)
U.mtl("skate", MATS)


def bowl_corner(r=16.0):
    """The inside corner of a bowl: the 1 m transition turned a quarter about the block's
    north-west corner, flat at that corner and vertical along the quarter circle of radius r;
    the deck beyond it."""
    out = []
    nr, na = 8, 8

    def hh(rho):
        return r - math.sqrt(max(0.0, r * r - rho * rho))

    rings = [r * math.sin(math.pi / 2 * k / nr) for k in range(nr + 1)]
    angs = [math.pi / 2 * k / na for k in range(na + 1)]
    for a, b in zip(rings, rings[1:]):
        for ta, tb in zip(angs, angs[1:]):
            pts = [(b * math.cos(ta), hh(b), b * math.sin(ta)), (b * math.cos(tb), hh(b), b * math.sin(tb)),
                   (a * math.cos(tb), hh(a), a * math.sin(tb)), (a * math.cos(ta), hh(a), a * math.sin(ta))]
            if a < 1e-9:
                pts = pts[:3]
            mid = ((a + b) / 2, (ta + tb) / 2)
            rho = mid[0]
            slope = rho / math.sqrt(max(1e-6, r * r - rho * rho))
            n = (-slope * math.cos(mid[1]), 1.0, -slope * math.sin(mid[1]))
            uvs = [(p[0] / 16, p[2] / 16) for p in pts]
            out.append(vc.Poly(pts, "concrete", n, uvs))
    for ta, tb in zip(angs, angs[1:]):
        def edge(th):
            c, s = math.cos(th), math.sin(th)
            k = 16.0 / max(c, s)
            return (k * c, 16.0, k * s)
        a0 = (r * math.cos(ta), 16.0, r * math.sin(ta))
        a1 = (r * math.cos(tb), 16.0, r * math.sin(tb))
        e0, e1 = edge(ta), edge(tb)
        pts = [a0, a1, e1, e0]
        if ta < math.pi / 4 < tb - 1e-9:
            pts = [a0, a1, e1, (16.0, 16.0, 16.0), e0]
        out.append(vc.Poly(pts, "concrete", (0, 1, 0), [(p[0] / 16, p[2] / 16) for p in pts]))
    for x, nx in ((16.0, 1),):
        q = [(x, 0, 0), (x, 0, 16), (x, 16, 16), (x, 16, 0)]
        out.append(vc.Poly(q, "concrete", (nx, 0, 0), [(p[2] / 16, 1 - p[1] / 16) for p in q]))
    q = [(16, 0, 16), (0, 0, 16), (0, 16, 16), (16, 16, 16)]
    out.append(vc.Poly(q, "concrete", (0, 0, 1), [(1 - p[0] / 16, 1 - p[1] / 16) for p in q]))
    zs = [16.0 * k / N for k in range(N + 1)]
    for za, zb in zip(zs, zs[1:]):
        for axis in ("x", "z"):
            ta, tb = hh(min(za, r)) if za < r else 16, hh(min(zb, r)) if zb < r else 16
            if axis == "x":
                q = [(0, 0, za), (0, 0, zb), (0, tb, zb), (0, ta, za)]
                out.append(vc.Poly(q, "concrete", (-1, 0, 0), [(p[2] / 16, 1 - p[1] / 16) for p in q]))
            else:
                q = [(za, 0, 0), (zb, 0, 0), (zb, tb, 0), (za, ta, 0)]
                out.append(vc.Poly(q, "concrete", (0, 0, -1), [(p[0] / 16, 1 - p[1] / 16) for p in q]))
    # coping round the lip, as eight straight lengths of the arc
    for ta, tb in zip(angs, angs[1:]):
        rr = r - 1.0
        out += vu.tube((rr * math.cos(ta), 16.0, rr * math.sin(ta)),
                       (rr * math.cos(tb), 16.0, rr * math.sin(tb)), 1.0, "coping", caps=False,
                       sides=8)
    return out


def bowl_steps(r=16.0, cell=4):
    out = []
    for i in range(0, 16, cell):
        for k in range(0, 16, cell):
            rho = math.hypot(i + cell / 2.0, k + cell / 2.0)
            top = 16.0 if rho >= r else r - math.sqrt(r * r - rho * rho)
            hi = int(round(top))
            if hi > 0:
                out.append((i, 0, k, i + cell, hi, k + cell))
    return out


U.write_obj("bowl_corner", "skate", bowl_corner())

# Kicker, bank halves and the box top
RAMPS = [("kicker", lambda z: 6.0 * z / 16.0, 0, 0),
         ("bank_low", lambda z: 8.0 * z / 16.0, 0, 0),
         ("bank_high", lambda z: 8.0 + 8.0 * z / 16.0, 0, 0),
         ("box_top", lambda z: 8.0, 0, 0)]
RAMP_PIECES = []
for name, h, z0, y0 in RAMPS:
    polys = swept(h, z0, y0, front=True)
    if name in ("kicker", "bank_high"):
        polys += coping_x(h(16.0) - 0.6, 15.4, 0.6) if name == "bank_high" else []
    if name == "box_top":
        polys += vu.box(0, 16, 7.4, 8.2, -0.2, 0.6, "coping", faces="nud")
        polys += vu.box(0, 16, 7.4, 8.2, 15.4, 16.2, "coping", faces="sud")
    U.write_obj(name, "skate", polys)
    RAMP_PIECES.append((name, steps(h, z0, y0) if name != "box_top" else [(0, 0, 0, 16, 8, 16)]))


def obj_variants(names):
    return {"piece": {str(k): {"model": M(n) + ".obj"} for k, n in enumerate(names)}}


C.add("skate_quarter_pipe",
      "new BlockSkateElement(\"skate_quarter_pipe\", new int[][]{%s})" % ", ".join(
          flat(b) for _n, _p, b in QP_PIECES),
      ("Quarter Pipe", "Quarterpipe", "Cuarto de Tubo", "Quarterpipe"), {},
      U.obj_state("qp_small", 0.55, ty=0.0, extra=obj_variants([n for n, _p, _b in QP_PIECES])),
      tab=TAB)
C.add("skate_bowl_corner",
      "new BlockSkateElement(\"skate_bowl_corner\", new int[][]{%s})" % flat(bowl_steps()),
      ("Bowl Corner", "Bowl-Ecke", "Esquina de Bowl", "Bowlhörn"), {},
      U.obj_state("bowl_corner", 0.55), tab=TAB)
C.add("skate_ramp",
      "new BlockSkateElement(\"skate_ramp\", new int[][]{%s})" % ", ".join(
          flat(b) for _n, b in RAMP_PIECES),
      ("Kicker, Bank and Box", "Kicker, Bank und Box", "Kicker, Rampa y Cajón",
       "Kicker, Bank och Box"), {},
      U.obj_state("kicker", 0.55, extra=obj_variants([n for n, _b in RAMP_PIECES])), tab=TAB)

C.add("skate_concrete",
      'new BlockSportSurface("skate_concrete", BlockArenaFloorBase.Surface.CONCRETE, false)',
      ("Skate Park Concrete", "Skatepark-Beton", "Hormigón de Skatepark", "Skateparksbetong"),
      {"skate_concrete": gc.cube(T("concrete"), T("concrete"))},
      {"forge_marker": 1, "defaults": {"model": M("skate_concrete")},
       "variants": {"inventory": [{}], "normal": [{}]}}, tab=TAB)

# ------------------------------------------------------------------------------------------
# Grind rail and ledge (BlockArenaRun)
# ------------------------------------------------------------------------------------------
RUN_TEX = {"rail": T("rail"), "concrete": T("concrete"), "coping": T("coping"),
           "particle": T("rail")}


def rail_parts():
    els = vc.lsc.pipe_x(5.0, 8.0, 0.8, 0, 16, "rail", ends=False)
    els.append(box([7.4, 0, 7.4], [8.6, 4.4, 8.6], "rail"))
    els.append(box([6, 0, 6], [10, 0.4, 10], "rail"))
    return els


def rail_end():
    p = box([0.6, 0, 7.4], [1.8, 5.8, 8.6], "rail")
    return [p, box([0, 0, 6.6], [2.4, 0.4, 9.4], "rail")]


def ledge_parts():
    return [box([0, 0, 0.8], [16, 6.2, 9], "concrete"),
            box([0, 6.2, 0.8], [16, 7, 9], "concrete", faces=("up", "south")),
            box([0, 6.2, 0], [16, 7, 0.8], "coping"), box([0, 0, 0], [16, 6.2, 0.8], "concrete",
                                                          faces=("north", "down"))]


def ledge_end():
    return [box([0, 6.2, 0], [0.4, 7, 9], "coping", faces=("west", "up"))]


C.add("skate_grind_rail", 'new BlockArenaRun("skate_grind_rail", BlockArenaRun.Kind.GRIND_RAIL)',
      ("Grind Rail", "Grindrail", "Barandilla de Grind", "Grindräcke"),
      gc.run_models("grind_rail", rail_parts(), rail_end(), RUN_TEX), None,
      item={"parent": "csm:block/venues/skate/grind_rail_item",
            "display": vu.icon_display(0.625)}, tab=TAB)
C.add("skate_ledge", 'new BlockArenaRun("skate_ledge", BlockArenaRun.Kind.SKATE_LEDGE)',
      ("Skate Ledge", "Skate-Ledge", "Bordillo de Skate", "Skateledge"),
      gc.run_models("ledge", ledge_parts(), ledge_end(), RUN_TEX), None,
      item={"parent": "csm:block/venues/skate/ledge_item",
            "display": vu.icon_display(0.625)}, tab=TAB)


def run_state(prefix):
    rules = []
    for facing, y in FACINGS:
        def ap(name):
            return dict({"model": M(name)}, **ys(y))
        rules.append({"when": {"facing": facing}, "apply": ap(prefix)})
        rules.append({"when": {"facing": facing, "end_l": "true"}, "apply": ap(prefix + "_end_l")})
        rules.append({"when": {"facing": facing, "end_r": "true"}, "apply": ap(prefix + "_end_r")})
    return {"multipart": rules}


for _b in C.blocks:
    if _b["registry"] == "skate_grind_rail":
        _b["blockstate"] = run_state("grind_rail")
    if _b["registry"] == "skate_ledge":
        _b["blockstate"] = run_state("ledge")

L = C.add_lang
L("itemGroup.tabvenuesskate", ("CSM: Sports & Venues - Skate Park",
                               "CSM: Sport & Veranstaltungsstätten - Skatepark",
                               "CSM: Deportes y Recintos - Skatepark",
                               "CSM: Sport & Arenor - Skatepark"))
for _k, _names in {"skate_quarter_pipe": (("1 m quarter pipe", "1-m-Quarterpipe",
                                            "Cuarto de tubo de 1 m", "1 m quarterpipe"),
                                           ("2 m quarter pipe: front block", "2-m-Quarterpipe: vorderer Block",
                                            "Cuarto de tubo de 2 m: bloque delantero",
                                            "2 m quarterpipe: främre block"),
                                           ("2 m quarter pipe: back block", "2-m-Quarterpipe: hinterer Block",
                                            "Cuarto de tubo de 2 m: bloque trasero",
                                            "2 m quarterpipe: bakre block"),
                                           ("2 m quarter pipe: top block", "2-m-Quarterpipe: oberer Block",
                                            "Cuarto de tubo de 2 m: bloque superior",
                                            "2 m quarterpipe: övre block")),
                   "skate_ramp": (("Kicker", "Kicker", "Kicker", "Kicker"),
                                  ("Bank: lower half", "Bank: untere Hälfte", "Rampa: mitad inferior",
                                   "Bank: nedre halvan"),
                                  ("Bank: upper half", "Bank: obere Hälfte", "Rampa: mitad superior",
                                   "Bank: övre halvan"),
                                  ("Box top", "Box-Oberseite", "Parte superior del cajón",
                                   "Boxens ovansida"))}.items():
    for _i, _n in enumerate(_names):
        L("csm.venues.skate.%s.%d" % (_k, _i), _n)


# ------------------------------------------------------------------------------------------
# --layout park: a small street park
# ------------------------------------------------------------------------------------------
FACE_IDX = {"south": 0, "west": 1, "north": 2, "east": 3}


def park():
    """(i, dy, j, block, meta): a 16 x 12 slab, a 1 m half pipe across its west end (two quarter
    pipes 4 blocks apart, 4 wide), a 2 m quarter pipe on the north side, a mini bowl corner,
    a funbox, a kicker, a ledge and a rail."""
    out = []
    for i in range(16):
        for j in range(12):
            out.append((i, 0, j, "skate_concrete", 0))
    # half pipe: QPs facing each other along z, j 0 and j 5, x 0..3
    for i in range(4):
        out.append((i, 1, 0, "skate_quarter_pipe", FACE_IDX["south"]))
        out.append((i, 1, 5, "skate_quarter_pipe", FACE_IDX["north"]))
        out.append((i, 1, -1, "skate_concrete", 0))
        out.append((i, 1, 6, "skate_concrete", 0))
    # a 2 m quarter pipe facing south along the north edge, x 6..9: front j 1, back j 0, top j 0
    for i in range(6, 10):
        out.append((i, 1, 1, "skate_quarter_pipe", FACE_IDX["south"] | 1 << 2))
        out.append((i, 1, 0, "skate_quarter_pipe", FACE_IDX["south"] | 2 << 2))
        out.append((i, 2, 0, "skate_quarter_pipe", FACE_IDX["south"] | 3 << 2))
        out.append((i, 2, -1, "skate_concrete", 0))
        out.append((i, 1, -1, "skate_concrete", 0))
    # a bowl's inside corner in the south-east: QPs facing north along j 11 and facing west
    # along i 15, the corner at (15, 11)
    for i in range(11, 15):
        out.append((i, 1, 11, "skate_quarter_pipe", FACE_IDX["north"]))
    for j in range(7, 11):
        out.append((15, 1, j, "skate_quarter_pipe", FACE_IDX["west"]))
    out.append((15, 1, 11, "skate_bowl_corner", FACE_IDX["north"]))
    # funbox: bank up (rising east), box, bank down; a kicker; a ledge; a rail
    for j in (7, 8):
        out.append((5, 1, j, "skate_ramp", FACE_IDX["west"] | 1 << 2))
        out.append((6, 1, j, "skate_ramp", FACE_IDX["west"] | 3 << 2))
        out.append((7, 1, j, "skate_ramp", FACE_IDX["west"] | 3 << 2))
        out.append((8, 1, j, "skate_ramp", FACE_IDX["east"] | 1 << 2))
    out.append((12, 1, 4, "skate_ramp", FACE_IDX["west"]))
    for i in range(10, 15):
        out.append((i, 1, 2, "skate_ledge", FACE_IDX["south"]))
        out.append((i, 1, 6, "skate_grind_rail", FACE_IDX["south"]))
    return out


def turn_meta(block, meta, rot):
    if block == "skate_concrete":
        return meta
    return (meta & ~3) | (((meta & 3) + rot) % 4)


def commands(name, x0, y0, z0, rot=0):
    out = []
    for (i, dy, j, b, m) in park():
        wx, wz = gc.to_world(i, j, rot)
        out.append("setblock %d %d %d csm:%s %d" % (x0 + wx, y0 + dy, z0 + wz, b,
                                                    turn_meta(b, m, rot)))
    return out


def main():
    if "--layout" in sys.argv:
        name = sys.argv[sys.argv.index("--layout") + 1]
        rot = int(sys.argv[sys.argv.index("--rot") + 1]) if "--rot" in sys.argv else 0
        if "--commands" in sys.argv:
            i = sys.argv.index("--commands")
            x, y, z = (int(v) for v in sys.argv[i + 1:i + 4])
            print("\n".join(commands(name, x, y, z, rot)))
        else:
            print(json.dumps({"layout": name, "objects": [list(o) for o in park()]}))
        return 0
    rc = C.main()
    if rc == 0 and "--fragments" not in sys.argv and "--check" not in sys.argv:
        vc.check_json([os.path.join(C.assets, rel) for rel in
                       [os.path.join(dp, fn) for dp, _, fs in os.walk(C.assets) for fn in fs]])
    return rc


if __name__ == "__main__":
    sys.exit(main())
