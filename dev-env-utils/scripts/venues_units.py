"""
venues_units.py -- what the courts and track generators share for their fixtures: OBJ writing
into a catalogue, round members, net panels, the multi-block unit's footprint and tab line, and
item displays. (The football generator's helpers, made catalogue-agnostic.)

Not run on its own.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_venues_common as vc  # noqa: E402

FACINGS = vc.FACINGS


def ys(y):
    return {"y": y} if y else {}


def icon_display(scale, dy=0.0, rot_y=225):
    return {"gui": {"rotation": [30, rot_y, 0], "translation": [0, dy, 0],
                    "scale": [scale, scale, scale]},
            "ground": {"translation": [0, 3, 0], "scale": [scale * 0.5] * 3},
            "fixed": {"translation": [0, dy, 0], "scale": [scale, scale, scale]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                      "scale": [scale * 0.6] * 3},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [scale * 0.7] * 3}}


def obj_inventory(scale, ty=0.0, tx=0.0, tz=0.0):
    """A Forge blockstate inventory variant's transform for an OBJ drawn at full size: scaled
    into the slot, the translation in blocks."""
    def t(rot, s, tr):
        return {"rotation": [{"x": rot[0]}, {"y": rot[1]}, {"z": rot[2]}],
                "translation": tr, "scale": [s, s, s]}
    return {"gui": t((30, 225, 0), scale, [tx, ty, tz]),
            "ground": t((0, 0, 0), scale * 0.5, [0, 0.2, 0]),
            "fixed": t((0, 180, 0), scale, [tx, ty, tz]),
            "thirdperson_righthand": t((75, 45, 0), scale * 0.6, [0, 0.15, 0]),
            "thirdperson_lefthand": t((75, 45, 0), scale * 0.6, [0, 0.15, 0]),
            "firstperson_righthand": t((0, 45, 0), scale * 0.7, [0, 0.1, 0]),
            "firstperson_lefthand": t((0, 225, 0), scale * 0.7, [0, 0.1, 0])}


class Units(object):
    """OBJ and unit helpers writing into one catalogue's model folder."""

    def __init__(self, cat, gen, sub):
        self.C, self.gen, self.sub = cat, gen, sub

    def write_obj(self, name, mtl, polys):
        o = vc.Obj(self.gen, mtl, name)
        for p in polys:
            o.add(p)
        self.C.extra["models/block/venues/%s/%s.obj" % (self.sub, name)] = o.text()

    def mtl(self, name, mats):
        self.C.extra["models/block/venues/%s/%s.mtl" % (self.sub, name)] = vc.mtl_text(self.gen,
                                                                                       mats)

    def obj_state(self, obj, inventory_scale, ty=0.0, ambient=True, extra=None, tx=0.0, tz=0.0):
        d = {"model": self.C.M(obj) + ".obj"}
        if not ambient:
            d["custom"] = {"ambient": False}
        variants = {"facing": {f: ys(y) for f, y in FACINGS},
                    "inventory": [{"transform": obj_inventory(inventory_scale, ty, tx, tz)}]}
        if extra:
            variants.update(extra)
        return {"forge_marker": 1, "defaults": d, "variants": variants}


def perp(d):
    """Two unit vectors square to d and to each other."""
    a = (0.0, 1.0, 0.0) if abs(d[1]) < 0.9 else (1.0, 0.0, 0.0)
    u = vc._norm(vc._cross(d, a))
    v = vc._norm(vc._cross(d, u))
    return u, v


def tube(p0, p1, r, mat, caps=True, sides=8):
    """A round member from p0 to p1 (pixels): a prism of ``sides`` sides."""
    d = vc._norm(vc._sub(p1, p0))
    u, v = perp(d)
    ring = []
    for i in range(sides):
        a = math.radians(180.0 / sides + 360.0 / sides * i)
        ring.append((u[0] * math.cos(a) + v[0] * math.sin(a),
                     u[1] * math.cos(a) + v[1] * math.sin(a),
                     u[2] * math.cos(a) + v[2] * math.sin(a)))
    out = []
    for i in range(sides):
        a, b = ring[i], ring[(i + 1) % sides]
        n = ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2)
        pts = [tuple(p0[k] + a[k] * r for k in range(3)), tuple(p0[k] + b[k] * r for k in range(3)),
               tuple(p1[k] + b[k] * r for k in range(3)), tuple(p1[k] + a[k] * r for k in range(3))]
        out.append(vc.Poly(pts, mat, n))
    if caps:
        out.append(vc.Poly([tuple(p0[k] + c[k] * r for k in range(3)) for c in ring], mat,
                           tuple(-x for x in d)))
        out.append(vc.Poly([tuple(p1[k] + c[k] * r for k in range(3)) for c in ring], mat, d))
    return out


def box(x0, x1, y0, y1, z0, z1, mat, faces="nsewud"):
    return vc.obox(x0, x1, y0, y1, z0, z1, mat, faces)


def panel(a, b, c, d, outward, mat, both=True, tile=16.0):
    """A flat panel a-b-c-d cut into pieces about ``tile`` pixels each, so its texture repeats
    (an OBJ's uv may not leave 0..1); both faces unless told."""
    def lerp(p, q, t):
        return tuple(p[k] + (q[k] - p[k]) * t for k in range(3))
    nu = max(1, int(math.ceil(math.dist(a, b) / tile - 1e-6)))
    nv = max(1, int(math.ceil(math.dist(a, d) / tile - 1e-6)))
    out = []
    for i in range(nu):
        for j in range(nv):
            def at(s, t):
                return lerp(lerp(a, b, s), lerp(d, c, s), t)
            s0, s1, t0, t1 = i / float(nu), (i + 1) / float(nu), j / float(nv), (j + 1) / float(nv)
            pts = [at(s0, t0), at(s1, t0), at(s1, t1), at(s0, t1)]
            fu = math.dist(a, b) / nu / tile
            fv = math.dist(a, d) / nv / tile
            uvs = [(0, 0), (fu, 0), (fu, fv), (0, fv)]
            out.append(vc.Poly(pts, mat, outward, uvs))
            if both:
                out.append(vc.Poly(list(reversed(pts)), mat, tuple(-x for x in outward),
                                   list(reversed(uvs))))
    return out


def unit_cells(boxes_px):
    """The arena generator's rule: every cell a box reaches 6 px into, but the master."""
    def axis(lo, hi, tol=6.0):
        out = []
        for c in range(int(math.floor(lo / 16.0)), int(math.ceil(hi / 16.0))):
            ov = min(hi, 16.0 * (c + 1)) - max(lo, 16.0 * c)
            if ov >= min(tol, hi - lo - 1e-6):
                out.append(c)
        return out
    cells = set()
    for b in boxes_px:
        for cx in axis(b[0], b[3]):
            for cy in axis(b[1], b[4]):
                for cz in axis(b[2], b[5]):
                    cells.add((cx, cy, cz))
    cells.discard((0, 0, 0))
    return sorted(cells, key=lambda c: (c[1], c[2], c[0]))


def check_connected(reg, cells):
    todo, seen = [(0, 0, 0)], {(0, 0, 0)}
    cs = set(cells)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in cs and n not in seen:
                seen.add(n)
                todo.append(n)
    lost = cs - seen
    if lost:
        raise SystemExit("%s: unit cells not connected to the master: %s" % (reg, sorted(lost)))


def unit_java(cls, reg, boxes_px, extra=""):
    cells = unit_cells(boxes_px)
    check_connected(reg, cells)
    flat_c = ", ".join("%d, %d, %d" % c for c in cells)
    flat_b = ", ".join(", ".join("%d" % int(round(v)) for v in b) for b in boxes_px)
    return 'new %s("%s"%s, new int[]{%s}, new int[]{%s})' % (cls, reg, extra, flat_c, flat_b)
