#!/usr/bin/env python3
"""Every asset the everyday novelties in Furniture & Novelties ship: the barber pole, birdhouse,
doghouse, hand water pump, coffee cup, picnic basket, plate of cookies, xylophone, office water
cooler and garden flamingo. The seasonal ones (wreath, pumpkins, nutcracker, scarecrow, snowman)
are gen_novelties_seasonal.py, which borrows the helpers here.

    python dev-env-utils/scripts/gen_novelties.py            # write
    python dev-env-utils/scripts/gen_novelties.py --check    # exit 1 if the tree has drifted
    python dev-env-utils/scripts/gen_novelties.py --boxes    # print each block's bounding box

Each block keeps its registry name, class, tab, lang and behaviour; only its blockstate is
rewritten here (same facing/inventory/normal variants, so placed blocks keep their facing) and
pointed at a model made here, with textures drawn here. Nothing is copied from the models these
replaced.

Every model is drawn at real scale (1 block = 1 m, 1 px = 6.25 cm), facing north: the front of
a freestanding object at z = 0, and what a wall-hung one hangs on at z = 16. The rotatable base
class turns the model and its bounding box from there.

Square things are JSON elements. Round things (the barber pole's glass, the cup and plate, the
water bottle) are OBJ lathes, since an element cannot be round.

JSON boxes take UVs from their own position, so a texture tiles on without stretching; a span
that runs past 0..16 is shifted a whole block back into the sprite rather than clamped. OBJ
parts are mapped the same way (boxes) or around their axis (lathes).

The inventory transform of each block is worked out from the model's bounds, so a coffee cup
fills its slot as well as a two-block water cooler does.
"""
import argparse
import json
import math
import os
import random
import shutil
import sys
import tempfile
from collections import OrderedDict

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402
import gen_trees  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "modules", "furnishings", "src", "main", "resources", "assets", "csm")

shade = lc.shade
clamp = lc.clamp
rect = lc.rect
disc = lc.disc


# ==========================================================================================
# Textures
# ==========================================================================================
def paint(colour, size=16, grain=4, seed=1):
    return lc.fill(colour, size=size, grain=grain, seed=seed)


def put(img, x, y, colour):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), clamp(colour[:3]) + (colour[3] if len(colour) == 4 else 255,))


def wood(base, size=16, seed=1, boards=4, vertical=False, grain=5):
    """Boards with dark joints and a streaky grain along them."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    pitch = size // boards
    tone = [rng.uniform(0.9, 1.08) for _ in range(boards)]
    streak = [[rng.uniform(-1, 1) for _ in range(size)] for _ in range(size)]
    for y in range(size):
        for x in range(size):
            a, b = (x, y) if vertical else (y, x)   # a across the boards, b along
            k = tone[(a // pitch) % boards]
            s = streak[a][(b // 3) % size] * 0.05
            c = shade(base, k + s + rng.uniform(-grain, grain) / 255.0)
            if a % pitch == pitch - 1:
                c = shade(base, 0.62)
            px[x, y] = c + (255,)
    return img


def shingles(base, size=16, seed=1, row=4, width=4):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        r = y // row
        off = (width // 2) * (r % 2)
        for x in range(size):
            col = (x + off) // width
            k = 0.88 + 0.2 * random.Random(seed * 1000 + r * 37 + col).random()
            c = shade(base, k + rng.uniform(-0.03, 0.03))
            if y % row == row - 1:
                c = shade(base, 0.55)
            elif (x + off) % width == 0:
                c = shade(base, 0.7)
            px[x, y] = c + (255,)
    return img


def stone_blocks(base, size=32, seed=1, course=8, unit=12):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        r = y // course
        off = (unit // 2) * (r % 2)
        for x in range(size):
            col = (x + off) // unit
            k = 0.86 + 0.22 * random.Random(seed * 7919 + r * 131 + col).random()
            c = shade(base, k + rng.uniform(-0.06, 0.06))
            if y % course == 0 or (x + off) % unit == 0:
                c = shade(base, 0.6)
            px[x, y] = c + (255,)
    return img


def weave(a, b, size=32, strand=2):
    """Basket weave: horizontal strands passing over and under vertical stakes."""
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            row = y // strand
            col = x // (strand * 2)
            over = (row + col) % 2 == 0
            c = a if over else b
            k = 1.0
            if y % strand == 0:
                k = 1.12
            if y % strand == strand - 1:
                k = 0.8
            if not over and x % (strand * 2) == 0:
                k *= 0.8
            px[x, y] = shade(c, k) + (255,)
    return img


def checks(a, b, size=16, cell=2):
    """Gingham: two colours and their blend where the bands cross."""
    mix = tuple((u + v) // 2 for u, v in zip(a, b))
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            h = (y // cell) % 2 == 0
            v = (x // cell) % 2 == 0
            px[x, y] = (a if h and v else mix if h or v else b) + (255,)
    return img


def metal(base, size=16, seed=1, streak=True):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        k = 1.0 + (0.12 if y % 5 == 1 else 0) - (0.08 if y % 7 == 3 else 0) if streak else 1.0
        for x in range(size):
            px[x, y] = shade(base, k + rng.uniform(-0.03, 0.03)) + (255,)
    return img


# ==========================================================================================
# JSON elements
# ==========================================================================================
def _wrap(a, b):
    """Shifts the span a..b a whole number of blocks so it lies inside 0..16 (never clamps it:
    a clamped span squashes the texture into one texel line)."""
    if b - a > 16:
        return 0.0, 16.0        # longer than the sprite: the one case it has to stretch
    k = math.floor(a / 16.0)
    a, b = a - 16 * k, b - 16 * k
    if b > 16.0001:
        a, b = a - (b - 16), 16.0
    return a, b


def uv_rect(frm, to, side):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    u = {
        "north": (16 - x1, 16 - x0, 16 - y1, 16 - y0),
        "south": (x0, x1, 16 - y1, 16 - y0),
        "east": (16 - z1, 16 - z0, 16 - y1, 16 - y0),
        "west": (z0, z1, 16 - y1, 16 - y0),
        "up": (x0, x1, z0, z1),
        "down": (x0, x1, 16 - z1, 16 - z0),
    }[side]
    ua, ub = _wrap(u[0], u[1])
    va, vb = _wrap(u[2], u[3])
    if ub - ua < 0.01:
        ub = ua + 0.01
    if vb - va < 0.01:
        vb = va + 0.01
    return [round(ua, 3), round(va, 3), round(ub, 3), round(vb, 3)]


ALL = ("north", "south", "east", "west", "up", "down")


def fbox(frm, to, tex, faces=ALL, per=None, rot=None):
    """An element with UVs fitted to its own size. rot = (axis, angle, origin)."""
    el = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to],
          "faces": OrderedDict()}
    for f in faces:
        el["faces"][f] = {"texture": "#" + (per or {}).get(f, tex), "uv": uv_rect(frm, to, f)}
    if rot:
        axis, angle, origin = rot
        el["rotation"] = {"origin": [round(v, 3) for v in origin], "axis": axis, "angle": angle}
    return el


def region(frm, to, side, size):
    """The texel rectangle (x0, y0, x1, y1) a face of fbox(frm, to) samples on a size-px
    texture, so a detail can be drawn exactly where it will be seen."""
    u0, v0, u1, v1 = uv_rect(frm, to, side)
    k = size / 16.0
    return (int(round(u0 * k)), int(round(v0 * k)), int(round(u1 * k)), int(round(v1 * k)))


def json_model(textures, elements, particle):
    tex = OrderedDict([("particle", particle)])
    tex.update(textures)
    return {"parent": "block/block", "textures": tex, "elements": elements}


def _rot(axis, deg):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    if axis == "x":
        return ((1, 0, 0), (0, c, -s), (0, s, c))
    if axis == "y":
        return ((c, 0, s), (0, 1, 0), (-s, 0, c))
    return ((c, -s, 0), (s, c, 0), (0, 0, 1))


def _mul(m, v):
    return tuple(sum(m[i][j] * v[j] for j in range(3)) for i in range(3))


def json_bounds(elements):
    lo, hi = [1e9] * 3, [-1e9] * 3
    for el in elements:
        f, t = el["from"], el["to"]
        for cx in (f[0], t[0]):
            for cy in (f[1], t[1]):
                for cz in (f[2], t[2]):
                    p = (cx, cy, cz)
                    r = el.get("rotation")
                    if r:
                        o = r["origin"]
                        q = _mul(_rot(r["axis"], r["angle"]), [p[i] - o[i] for i in range(3)])
                        p = tuple(q[i] + o[i] for i in range(3))
                    for i in range(3):
                        lo[i] = min(lo[i], p[i])
                        hi[i] = max(hi[i], p[i])
    return lo, hi


# ==========================================================================================
# OBJ meshes
# ==========================================================================================
def vsub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def vadd(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def vdot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def vcross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def vnorm(a):
    n = math.sqrt(vdot(a, a))
    return (a[0] / n, a[1] / n, a[2] / n) if n > 1e-12 else (0.0, 1.0, 0.0)


class Frame(object):
    """A rigid placement: p -> origin + R p (R a rotation, so normals just turn with it)."""

    def __init__(self, R=((1, 0, 0), (0, 1, 0), (0, 0, 1)), origin=(0, 0, 0)):
        self.R = R
        self.o = origin

    def p(self, v):
        return vadd(self.o, _mul(self.R, v))

    def n(self, v):
        return _mul(self.R, v)

    def then(self, inner):
        """This frame applied after `inner` (inner is the child's placement inside this one)."""
        R = tuple(tuple(sum(self.R[i][k] * inner.R[k][j] for k in range(3)) for j in range(3))
                  for i in range(3))
        return Frame(R, self.p(inner.o))


def at(x, y, z):
    return Frame(origin=(x, y, z))


def turned(axis, deg, origin=(0, 0, 0)):
    return Frame(_rot(axis, deg), origin)


IDENT = Frame()


class Obj(object):
    """Triangles grouped by material. Each triangle is wound to face its vertex normals (Forge
    culls back faces, so winding is not cosmetic)."""

    def __init__(self):
        self.groups = OrderedDict()

    def tri(self, mat, verts, fr=IDENT):
        pts = [fr.p(p) for p, _t, _n in verts]
        nrm = [vnorm(fr.n(n)) for _p, _t, n in verts]
        uvs = [t for _p, t, _n in verts]
        geo = vcross(vsub(pts[1], pts[0]), vsub(pts[2], pts[0]))
        if vdot(geo, geo) < 1e-14:
            return
        want = vadd(vadd(nrm[0], nrm[1]), nrm[2])
        if vdot(geo, want) < 0:
            pts.reverse()
            nrm.reverse()
            uvs.reverse()
        self.groups.setdefault(mat, []).append(list(zip(pts, uvs, nrm)))

    def quad(self, mat, a, b, c, d, fr=IDENT):
        self.tri(mat, [a, b, c], fr)
        self.tri(mat, [a, c, d], fr)

    def points(self):
        for tris in self.groups.values():
            for t in tris:
                for p, _uv, _n in t:
                    yield p

    def bounds(self):
        pts = list(self.points())
        return ([min(p[i] for p in pts) for i in range(3)],
                [max(p[i] for p in pts) for i in range(3)])

    def text(self, name, mtl, script):
        vs, vts, vns = OrderedDict(), OrderedDict(), OrderedDict()

        def idx(store, key):
            if key not in store:
                store[key] = len(store) + 1
            return store[key]
        faces = []
        for mat, tris in self.groups.items():
            faces.append("usemtl " + mat)
            for t in tris:
                refs = []
                for p, uv, n in t:
                    refs.append("%d/%d/%d" % (
                        idx(vs, tuple("%.5f" % (c / 16.0) for c in p)),
                        idx(vts, tuple("%.5f" % c for c in uv)),
                        idx(vns, tuple("%.5f" % (c + 0.0) for c in n))))
                faces.append("f " + " ".join(refs))
        lines = ["# Procedurally generated by dev-env-utils/scripts/%s -- do not hand edit"
                 % script, "mtllib " + mtl, "o " + name]
        lines += ["v %s %s %s" % v for v in vs]
        lines += ["vt %s %s" % t for t in vts]
        lines += ["vn %s %s %s" % n for n in vns]
        return "\n".join(lines + faces) + "\n"


def tuv(u, v):
    """A texture coordinate from image fractions (v = 0 at the top of the image). flip-v is on
    in every blockstate here, so OBJ v is written inverted. Kept a hair inside the sprite."""
    u = 0.004 + 0.992 * min(1.0, max(0.0, u))
    v = 0.004 + 0.992 * min(1.0, max(0.0, v))
    return (round(u, 5), round(1.0 - v, 5))


def _span(a, b):
    a, b = _wrap(a, b)
    return a / 16.0, b / 16.0


def obox(obj, mat, lo, hi, fr=IDENT, faces=("x-", "x+", "y-", "y+", "z-", "z+")):
    """An axis-aligned box in frame fr, with UVs fitted to its size (as a JSON element's)."""
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    for f in faces:
        if f[0] == "x":
            x = x0 if f == "x-" else x1
            n = (-1 if f == "x-" else 1, 0, 0)
            ua, ub = _span(z0, z1)
            va, vb = _span(16 - y1, 16 - y0)
            quad = [((x, y0, z0), (ua, vb)), ((x, y0, z1), (ub, vb)),
                    ((x, y1, z1), (ub, va)), ((x, y1, z0), (ua, va))]
        elif f[0] == "y":
            y = y0 if f == "y-" else y1
            n = (0, -1 if f == "y-" else 1, 0)
            ua, ub = _span(x0, x1)
            va, vb = _span(z0, z1)
            quad = [((x0, y, z0), (ua, va)), ((x1, y, z0), (ub, va)),
                    ((x1, y, z1), (ub, vb)), ((x0, y, z1), (ua, vb))]
        else:
            z = z0 if f == "z-" else z1
            n = (0, 0, -1 if f == "z-" else 1)
            ua, ub = _span(16 - x1, 16 - x0)
            va, vb = _span(16 - y1, 16 - y0)
            quad = [((x1, y0, z), (ua, vb)), ((x0, y0, z), (ub, vb)),
                    ((x0, y1, z), (ub, va)), ((x1, y1, z), (ua, va))]
        obj.quad(mat, *[(p, tuv(*t), n) for p, t in quad], fr=fr)


def lathe(obj, mat, profile, fr=IDENT, sides=16, uvf=None, radial=None, normals=None):
    """A surface of revolution about the local y axis. profile: (r, y) pairs; the normal is
    the profile's direction turned right, so walk an outside surface upward (or a top face
    inward, or an inside wall downward). uvf(ufrac, k, r, y, angle) -> (u, v) image fractions;
    the default wraps u once around and runs v down the profile. radial(angle) scales r (a
    pumpkin's ribs)."""
    n = len(profile)
    if uvf is None:
        uvf = lambda uf, k, r, y, a: (uf, 1.0 - k / float(n - 1))  # noqa: E731
    rings = []
    for k, (r, y) in enumerate(profile):
        pr, py = profile[max(k - 1, 0)]
        nr_, ny_ = profile[min(k + 1, n - 1)]
        dr, dy = nr_ - pr, ny_ - py
        ln = math.hypot(dr, dy) or 1.0
        cr, cy = dy / ln, -dr / ln
        ring = []
        for i in range(sides + 1):
            a = 2 * math.pi * i / sides
            m = radial(a) if radial else 1.0
            p = (r * m * math.cos(a), y, r * m * math.sin(a))
            nv = normals(a, k) if normals else (cr * math.cos(a), cy, cr * math.sin(a))
            ring.append((p, tuv(*uvf(i / float(sides), k, r, y, a)), nv))
        rings.append(ring)
    for k in range(n - 1):
        (ra, _ya), (rb, _yb) = profile[k], profile[k + 1]
        if ra < 1e-9 and rb < 1e-9:
            continue
        for i in range(sides):
            a, b = rings[k][i], rings[k][i + 1]
            c, d = rings[k + 1][i + 1], rings[k + 1][i]
            if ra < 1e-9:
                obj.tri(mat, [a, c, d], fr)
            elif rb < 1e-9:
                obj.tri(mat, [a, b, c], fr)
            else:
                obj.quad(mat, a, b, c, d, fr)


def planar_uv(scale):
    """A lathe uvf mapping a flat cap straight down onto the texture (a coffee's surface, a
    cookie's face): u, v from x, z, `scale` px across the whole image."""
    def f(_uf, _k, r, _y, a):
        return (0.5 + r * math.cos(a) / scale, 0.5 + r * math.sin(a) / scale)
    return f


def sphere_profile(r, steps=10, y0=0.0, cut_below=None):
    """(r, y) from the bottom pole to the top, centre at y0; cut_below flattens the bottom."""
    out = []
    for k in range(steps + 1):
        t = math.pi * (1 - k / float(steps))       # pi .. 0
        rr, yy = r * math.sin(t), y0 + r * math.cos(t)
        if cut_below is not None and yy < cut_below:
            continue
        out.append((max(0.0, rr), yy))
    if cut_below is not None:
        rc = math.sqrt(max(0.0, r * r - (cut_below - y0) ** 2))
        out = [(0.0, cut_below), (rc, cut_below)] + out
    out[-1] = (0.0, out[-1][1])
    return out


def torus(obj, mat, R, r, fr=IDENT, seg=24, sides=8, a0=0.0, a1=360.0, uvf=None, bump=None):
    """A ring in the local x-y plane about the origin (its axis along z). bump(i, j) -> extra
    tube radius, for a bushy wreath."""
    rings = []
    for i in range(seg + 1):
        a = math.radians(a0 + (a1 - a0) * i / float(seg))
        ring = []
        for j in range(sides + 1):
            b = 2 * math.pi * j / sides
            rr = r + (bump(i % seg if a1 - a0 >= 360 else i, j % sides) if bump else 0.0)
            nrm = (math.cos(a) * math.cos(b), math.sin(a) * math.cos(b), math.sin(b))
            p = ((R + rr * math.cos(b)) * math.cos(a), (R + rr * math.cos(b)) * math.sin(a),
                 rr * math.sin(b))
            t = uvf(i / float(seg), j / float(sides)) if uvf else (
                pingpong(i * 6.0 / seg), pingpong(j * 2.0 / sides))
            ring.append((p, tuv(*t), nrm))
        rings.append(ring)
    for i in range(seg):
        for j in range(sides):
            obj.quad(mat, rings[i][j], rings[i + 1][j], rings[i + 1][j + 1], rings[i][j + 1], fr)


def pingpong(x):
    """A triangle wave into 0..1: a texture mirrored rather than wrapped, so a strip of quads
    round a ring never jumps back across the whole sprite."""
    x = x % 2.0
    return x if x <= 1.0 else 2.0 - x


def rbox(obj, mat, lo, hi, axis, deg, pivot, faces=("x-", "x+", "y-", "y+", "z-", "z+")):
    """obox turned deg about axis through pivot (lo, hi given unturned, in block space)."""
    fr = turned(axis, deg, pivot)
    obox(obj, mat, vsub(lo, pivot), vsub(hi, pivot), fr, faces)


def mtl_text(materials, script):
    lines = ["# Procedurally generated by dev-env-utils/scripts/%s -- do not hand edit" % script]
    for name, tex in materials.items():
        lines += ["newmtl " + name, "map_Kd " + tex]
    return "\n".join(lines) + "\n"


# ==========================================================================================
# Blockstates
# ==========================================================================================
FACING = OrderedDict([("down", {"x": 90}), ("east", {"y": 90}), ("north", {}),
                      ("south", {"y": 180}), ("up", {"x": 270}), ("west", {"y": 270})])

# forge:default-block's perspectives: rotation, translation (blocks), scale.
PERSPECTIVES = [
    ("gui", (30, 225, 0), (0, 0, 0), 0.625),
    ("ground", (0, 0, 0), (0, 0.1875, 0), 0.25),
    ("fixed", (0, 0, 0), (0, 0, 0), 0.5),
    ("thirdperson_righthand", (75, 45, 0), (0, 0.15625, 0), 0.375),
    ("thirdperson_lefthand", (75, 45, 0), (0, 0.15625, 0), 0.375),
    ("firstperson_righthand", (0, 45, 0), (0, 0, 0), 0.4),
    ("firstperson_lefthand", (0, 225, 0), (0, 0, 0), 0.4),
]


def inventory_transform(lo, hi, zoom=1.0):
    """Every perspective scaled so the model's largest extent fills what a full block would,
    and shifted so the model's centre sits where a block's does. Forge turns a blockstate
    transform about the block centre as R = Rx Ry Rz, translation in blocks."""
    ext = max(hi[i] - lo[i] for i in range(3)) / 16.0
    k = min(4.0, max(0.3, 0.95 / max(ext, 0.05))) * zoom
    c = [((lo[i] + hi[i]) / 32.0) - 0.5 for i in range(3)]
    out = OrderedDict()
    for name, rot, tr, s in PERSPECTIVES:
        s2 = s * k
        R = _rot("x", rot[0])
        R = tuple(tuple(sum(R[i][m] * _rot("y", rot[1])[m][j] for m in range(3))
                        for j in range(3)) for i in range(3))
        off = _mul(R, [v * s2 for v in c])
        out[name] = OrderedDict([
            ("rotation", list(rot)),
            ("translation", [round(tr[i] - off[i], 4) for i in range(3)]),
            ("scale", [round(s2, 4)] * 3)])
    return out


def blockstate(model, lo, hi, textures=None, obj=False, zoom=1.0):
    d = OrderedDict([("model", model)])
    if obj:
        d["custom"] = {"flip-v": True}
    if textures:
        d["textures"] = textures
    inv = OrderedDict()
    if obj:
        inv["custom"] = {"flip-v": True}
    inv["transform"] = inventory_transform(lo, hi, zoom)
    return OrderedDict([("forge_marker", 1), ("defaults", d), ("variants", OrderedDict([
        ("facing", FACING), ("inventory", [inv]), ("normal", [{}])]))])


# ==========================================================================================
# Catalogue
# ==========================================================================================
class Catalogue(object):
    def __init__(self, script, sub):
        self.script = script
        self.sub = sub          # e.g. "novelties/props": texture and model folder
        self.textures = OrderedDict()
        self.files = OrderedDict()      # rel path -> text
        self.blocks = []                # (registry, java path, lo, hi)

    def T(self, name):
        return "csm:blocks/%s/%s" % (self.sub, name)

    def tex(self, name, img, mcmeta=None):
        self.textures[name] = img
        if mcmeta:
            self.files["textures/blocks/%s/%s.png.mcmeta" % (self.sub, name)] = (
                json.dumps(mcmeta, indent=2) + "\n")
        return name

    def add_json(self, registry, java, name, textures, elements, particle, zoom=1.0):
        for el in elements:
            for v in el["from"] + el["to"]:
                assert -16 <= v <= 32, (registry, el)
        m = json_model(OrderedDict((k, self.T(v)) for k, v in textures.items()), elements,
                       self.T(particle))
        self.files["models/block/%s/%s.json" % (self.sub, name)] = json.dumps(m, indent=2) + "\n"
        lo, hi = json_bounds(elements)
        bs = blockstate("csm:%s/%s" % (self.sub, name), lo, hi, zoom=zoom)
        self.files["blockstates/%s.json" % registry] = json.dumps(bs, indent=2) + "\n"
        self.blocks.append((registry, java, lo, hi))

    def add_obj(self, registry, java, name, obj, materials, particle, zoom=1.0):
        mats = OrderedDict((k, self.T(v)) for k, v in materials.items())
        self.files["models/block/%s/%s.obj" % (self.sub, name)] = obj.text(
            name, name + ".mtl", self.script)
        self.files["models/block/%s/%s.mtl" % (self.sub, name)] = mtl_text(mats, self.script)
        lo, hi = obj.bounds()
        tex = OrderedDict([("particle", self.T(particle))])
        tex.update(("#" + k, v) for k, v in mats.items())
        bs = blockstate("csm:%s/%s.obj" % (self.sub, name), lo, hi, textures=tex, obj=True,
                        zoom=zoom)
        self.files["blockstates/%s.json" % registry] = json.dumps(bs, indent=2) + "\n"
        self.blocks.append((registry, java, lo, hi))

    def generate(self, assets):
        written = []
        for name, img in self.textures.items():
            rel = "textures/blocks/%s/%s.png" % (self.sub, name)
            path = os.path.join(assets, rel)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            img.save(path)
            written.append(rel)
        for rel, text in self.files.items():
            path = os.path.join(assets, rel)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", newline="\n", encoding="utf-8") as fh:
                fh.write(text)
            written.append(rel)
        return written

    def box_lines(self):
        out = []
        for registry, java, lo, hi in self.blocks:
            b = [lo[0] / 16, max(0.0, lo[1]) / 16, lo[2] / 16, hi[0] / 16, hi[1] / 16, hi[2] / 16]
            out.append("%-16s %s  new AxisAlignedBB(%s)" % (
                registry, java, ", ".join("%.6f" % v for v in b)))
        return out

    def main(self):
        ap = argparse.ArgumentParser(description=self.script)
        ap.add_argument("--check", action="store_true")
        ap.add_argument("--boxes", action="store_true")
        args = ap.parse_args()
        if args.boxes:
            print("\n".join(self.box_lines()))
            return 0
        if not args.check:
            written = self.generate(ASSETS)
            print("wrote %d files under %s" % (len(written), ASSETS))
            return 0
        tmp = tempfile.mkdtemp(prefix="novelties_")
        try:
            written = self.generate(tmp)
            stale = [rel for rel in written
                     if not gen_trees.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
            if stale:
                print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
                return 1
            print("%s: up to date (%d files)" % (self.script, len(written)))
            return 0
        finally:
            shutil.rmtree(tmp, ignore_errors=True)


JAVA = "modules/furnishings/src/main/java/com/micatechnologies/minecraft/csm/"
C = Catalogue("gen_novelties.py", "novelties/props")


# ==========================================================================================
# Barber pole: wall-mounted, the glass turning (an animated texture) between chrome caps
# ==========================================================================================
def barber_stripes():
    """16 frames of a helix of red and blue on white, each frame the helix a texel further
    round, so it climbs the glass like a turning pole. A fixed highlight is the glass."""
    frames = 16
    img = Image.new("RGBA", (16, 16 * frames))
    red, blue, white = (196, 28, 36), (30, 58, 160), (244, 244, 240)
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                d = (x + y + f) % 16
                c = red if d < 4 else blue if 8 <= d < 12 else white
                if x in (10, 11):
                    c = shade(c, 1.25) if c != white else (255, 255, 255)
                if x in (3, 4):
                    c = shade(c, 0.8)
                img.putpixel((x, y + 16 * f), clamp(c) + (255,))
    return img


def barberpole():
    t_str = C.tex("barber_stripes", barber_stripes(), {"animation": {"frametime": 3}})
    t_chrome = C.tex("chrome", metal((196, 200, 206), seed=3))
    t_plate = C.tex("chrome_plate", metal((168, 172, 178), seed=4, streak=False))
    o = Obj()
    ax = at(8, 0, 11.6)
    # Bottom cap: a pointed chrome cup, wider than the glass.
    lathe(o, "chrome", [(0.0, 1.0), (0.6, 1.2), (1.6, 1.8), (2.6, 2.6), (2.6, 3.4), (2.2, 3.4)],
          ax, sides=20)
    # The glass with the helix behind it: u once round, v the height.
    lathe(o, "stripes", [(2.2, 3.4), (2.2, 12.6)], ax, sides=20,
          uvf=lambda uf, k, r, y, a: (uf, (12.6 - y) / 9.2))
    # Top cap: a dome with a ball finial.
    lathe(o, "chrome", [(2.2, 12.6), (2.6, 12.6), (2.6, 13.4), (2.0, 14.2), (1.2, 14.8),
                        (0.5, 15.0), (0.75, 15.25), (0.8, 15.5), (0.6, 15.8), (0.0, 15.95)],
          ax, sides=20)
    # Back plate on the wall and the two arms that hold the caps off it.
    obox(o, "plate", (6.2, 1.4, 15.4), (9.8, 14.8, 16.0))
    for y0 in (2.2, 12.8):
        obox(o, "chrome", (7.4, y0, 13.6), (8.6, y0 + 0.8, 15.4), faces=("x-", "x+", "y-", "y+"))
    C.add_obj("barberpole", JAVA + "furniture/BlockBarberpole.java", "barberpole", o,
              OrderedDict([("stripes", t_str), ("chrome", t_chrome), ("plate", t_plate)]),
              t_chrome)


# ==========================================================================================
# Birdhouse: a cedar box with a gable roof on a garden post
# ==========================================================================================
def gable_roof(ridge, half, overhang, z0, z1, thick, tex, ridge_tex=None):
    """Two roof planes at 45 degrees meeting at (8, ridge), each reaching `half` out and
    `overhang` past that, and a ridge cap."""
    L = half * math.sqrt(2) + overhang
    els = [
        fbox((8 - L, ridge, z0), (8, ridge + thick, z1), tex, rot=("z", 45, (8, ridge, 8))),
        fbox((8, ridge, z0), (8 + L, ridge + thick, z1), tex, rot=("z", -45, (8, ridge, 8))),
    ]
    c = thick * 1.2
    els.append(fbox((8 - c / 2, ridge + thick * 0.2, z0 - 0.1), (8 + c / 2, ridge + thick * 0.2 + c,
                                                                 z1 + 0.1),
                    ridge_tex or tex, rot=("z", 45, (8, ridge + thick * 0.2 + c / 2, 8))))
    return els


def birdhouse():
    cedar = (170, 112, 66)
    C.tex("cedar", wood(cedar, seed=11, boards=4, vertical=True))
    front = wood(cedar, size=64, seed=11, boards=4, vertical=True)
    body = ((5, 17.5, 5), (11, 22.5, 11))
    x0, y0, x1, y1 = region(body[0], body[1], "north", 64)
    # The entrance hole, dark inside, with a lighter ring of end grain round it.
    cx, cy = (16 - 8) * 4, (16 - 21) % 16 * 4
    disc(front, cx, cy, 4.4, shade(cedar, 1.25))
    disc(front, cx, cy, 3.4, (30, 20, 14))
    C.tex("birdhouse_front", front)
    C.tex("roof_green", shingles((58, 104, 70), seed=12, row=3, width=3))
    C.tex("post_grey", wood((128, 122, 112), seed=13, boards=2, vertical=True))
    C.tex("dowel", paint((196, 160, 110), seed=14))
    els = [
        fbox((7.25, 0, 7.25), (8.75, 17, 8.75), "post"),
        fbox((4.5, 17, 4.5), (11.5, 17.5, 11.5), "wood"),
        fbox(body[0], body[1], "wood", per={"north": "front"}),
        # Gable ends: a square turned 45 whose lower half hides inside the body.
        fbox((8 - 2.1213, 22.5 - 2.1213, 5.05), (8 + 2.1213, 22.5 + 2.1213, 10.95), "wood",
             rot=("z", 45, (8, 22.5, 8))),
        fbox((7.8, 19.6, 3.8), (8.2, 20.0, 5.0), "dowel"),
    ]
    els += gable_roof(25.5, 3.0, 1.0, 4.2, 11.8, 0.5, "roof")
    C.add_json("birdhouse", JAVA + "furniture/BlockBirdhouse.java", "birdhouse",
               OrderedDict([("wood", "cedar"), ("front", "birdhouse_front"),
                            ("roof", "roof_green"), ("post", "post_grey"), ("dowel", "dowel")]),
               els, "cedar")


# ==========================================================================================
# Doghouse: lap-sided walls, a door, a shingled gable roof
# ==========================================================================================
def doghouse():
    pine = (206, 170, 118)
    C.tex("siding_pine", wood(pine, seed=21, boards=4))
    C.tex("roof_red", shingles((150, 40, 34), seed=22))
    C.tex("trim_white", paint((236, 234, 226), seed=23, grain=3))
    C.tex("inside_dark", paint((70, 52, 36), seed=24, grain=6))
    plate = paint((236, 234, 226), size=64, seed=25, grain=3)
    pf, pt = (6.4, 8.0, 0.7), (9.6, 9.3, 1.0)
    x0, y0, x1, y1 = region(pf, pt, "north", 64)
    lc.frame(plate, x0, y0, x1, y1, (120, 80, 50))
    lc.draw_text_centred(plate, "REX", (x0 + x1) / 2.0, (y0 + y1) // 2 - 2, (120, 36, 30))
    C.tex("doghouse_plate", plate)
    W = 10.0            # wall top
    els = [
        # Floor (seen through the door) and the two skids it sits on.
        fbox((2.2, 0.0, 1.5), (3.2, 0.5, 14.5), "trim"),
        fbox((12.8, 0.0, 1.5), (13.8, 0.5, 14.5), "trim"),
        fbox((2.8, 0.5, 1.8), (13.2, 0.8, 14.2), "inside", faces=("up",)),
        # Front wall round the door (door 5.5..10.5 wide, 0.5..7.5 high), back and sides.
        fbox((2.0, 0.5, 1.0), (5.5, W, 1.8), "wall"),
        fbox((10.5, 0.5, 1.0), (14.0, W, 1.8), "wall"),
        fbox((5.5, 7.5, 1.0), (10.5, W, 1.8), "wall"),
        fbox((5.5, 6.8, 1.0), (6.2, 7.5, 1.8), "wall"),
        fbox((9.8, 6.8, 1.0), (10.5, 7.5, 1.8), "wall"),
        fbox((2.0, 0.5, 14.2), (14.0, W, 15.0), "wall"),
        fbox((2.0, 0.5, 1.8), (2.8, W, 14.2), "wall", per={"east": "inside"}),
        fbox((13.2, 0.5, 1.8), (14.0, W, 14.2), "wall", per={"west": "inside"}),
        # Back of the inside, so the door does not show the back wall's siding.
        fbox((2.8, 0.8, 14.15), (13.2, W, 14.2), "inside", faces=("north",)),
        # Door trim and corner boards.
        fbox((5.1, 0.5, 0.8), (5.5, 7.9, 1.0), "trim"),
        fbox((10.5, 0.5, 0.8), (10.9, 7.9, 1.0), "trim"),
        fbox((5.1, 7.5, 0.8), (10.9, 7.9, 1.0), "trim"),
        fbox(pf, pt, "trim", per={"north": "plate"}),
    ]
    for x in (1.8, 13.4):
        for z in (0.8, 14.4):
            els.append(fbox((x, 0.5, z), (x + 0.8, W, z + 0.8), "trim"))
    # Gable ends stepped a pixel at a time; the roof's overhang hides the steps.
    for k in range(6):
        h = 6.0 - k - 0.35
        for z0, z1 in ((1.0, 1.8), (14.2, 15.0)):
            els.append(fbox((8 - h, W + k, z0), (8 + h, W + k + 1, z1), "wall",
                            faces=("north", "south", "up", "down")))
    els += gable_roof(16.0, 6.0, 1.4, 0.2, 15.8, 1.0, "roof", "trim")
    C.add_json("doghouse", JAVA + "furniture/BlockDoghouse.java", "doghouse",
               OrderedDict([("wall", "siding_pine"), ("roof", "roof_red"), ("trim", "trim_white"),
                            ("inside", "inside_dark"), ("plate", "doghouse_plate")]),
               els, "siding_pine")


# ==========================================================================================
# Water pump: a cast-iron pitcher pump on a stone plinth
# ==========================================================================================
def waterpump():
    stone = (140, 138, 132)
    C.tex("stone_blocks", stone_blocks(stone, seed=31))
    top = stone_blocks(stone, seed=32, course=10, unit=14)
    # Worn wet patch and a drain under the spout (plinth top, texel = x, z at 2 px/px).
    for y in range(4, 14):
        for x in range(10, 22):
            if ((x - 16) / 6.5) ** 2 + ((y - 9) / 5.0) ** 2 <= 1:
                r, g, b, a = top.getpixel((x, y))
                top.putpixel((x, y), shade((r, g, b), 0.72) + (255,))
    rect(top, 13, 7, 19, 11, (40, 40, 40))
    for x in range(13, 19, 2):
        rect(top, x, 7, x + 1, 11, (90, 90, 88))
    C.tex("stone_top", top)
    C.tex("cast_iron", metal((58, 66, 62), seed=33))
    post = lc.post
    cz = 9.5
    els = [
        fbox((3.0, 0.0, 3.0), (13.0, 2.5, 14.0), "stone", per={"up": "top"}),
    ]
    els += post(8, cz, 2.3, 2.5, 3.3, "iron")          # flange
    els += post(8, cz, 1.7, 3.3, 12.6, "iron", bottom=False)
    els += post(8, cz, 2.0, 12.6, 13.6, "iron")        # cap
    els += post(8, cz, 0.7, 13.6, 14.2, "iron", bottom=False)
    els += [
        # Spout: out north, then turned down.
        fbox((7.2, 9.8, 4.3), (8.8, 11.3, 8.2), "iron"),
        fbox((7.3, 8.8, 4.1), (8.7, 11.3, 5.5), "iron"),
        # Pivot ears on top at the back, and the handle rising behind at 45 degrees.
        fbox((6.9, 13.4, 10.6), (7.4, 15.4, 11.8), "iron"),
        fbox((8.6, 13.4, 10.6), (9.1, 15.4, 11.8), "iron"),
        fbox((7.4, 14.2, 11.2), (8.6, 15.0, 18.4), "iron", rot=("x", -45, (8, 14.6, 11.2))),
        fbox((7.2, 14.0, 17.0), (8.8, 15.2, 18.4), "iron", rot=("x", -45, (8, 14.6, 11.2))),
        # Pump rod coming out of the cap into the handle.
        fbox((7.7, 14.2, 9.1), (8.3, 15.2, 9.9), "iron"),
    ]
    C.add_json("waterpump", JAVA + "furniture/BlockWaterPump.java", "waterpump",
               OrderedDict([("stone", "stone_blocks"), ("top", "stone_top"),
                            ("iron", "cast_iron")]),
               els, "cast_iron")


# ==========================================================================================
# Coffee cup: a mug on a saucer
# ==========================================================================================
def coffeecup():
    C.tex("china", paint((240, 238, 232), seed=41, grain=3))
    coffee = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 8.0
            c = (92, 52, 26) if d > 0.85 else (70, 38, 18) if d > 0.55 else (58, 30, 14)
            coffee.putpixel((x, y), c + (255,))
    rect(coffee, 6, 6, 8, 7, (150, 100, 60))
    C.tex("coffee", coffee)
    o = Obj()
    c = at(8, 0, 8)
    lathe(o, "china", [(0.0, 0.0), (1.1, 0.0), (1.3, 0.12), (1.9, 0.26), (1.95, 0.33),
                       (1.75, 0.31), (1.15, 0.2), (0.0, 0.2)], c, sides=20)
    lathe(o, "china", [(0.72, 0.2), (0.8, 0.3), (0.84, 1.1), (0.86, 1.72), (0.76, 1.74),
                       (0.74, 1.5)], c, sides=20)
    lathe(o, "coffee", [(0.74, 1.5), (0.0, 1.5)], c, sides=20, uvf=planar_uv(1.5))
    # Handle: half a ring on the east side.
    torus(o, "china", 0.42, 0.11, at(8.84, 0.98, 8), seg=8, sides=6, a0=-90, a1=90)
    C.add_obj("coffeecup", JAVA + "novelties/BlockCoffeeCup.java", "coffeecup", o,
              OrderedDict([("china", "china"), ("coffee", "coffee")]), "china")


# ==========================================================================================
# Picnic basket: wicker, two lids on a centre bar, a gingham cloth hanging out
# ==========================================================================================
def picnicbasket():
    C.tex("wicker", weave((196, 150, 90), (160, 116, 64)))
    C.tex("wicker_band", weave((150, 106, 58), (126, 88, 46), strand=1))
    C.tex("gingham", checks((190, 30, 36), (246, 244, 238)))
    C.tex("handle_wood", wood((150, 100, 56), seed=51, boards=2))
    els = [
        fbox((4.0, 0.0, 5.0), (12.0, 3.6, 11.0), "wicker"),
        fbox((3.8, 3.6, 4.8), (12.2, 4.2, 11.2), "band"),
        # Lids either side of the centre bar; the east one sits a little proud on the cloth.
        fbox((3.9, 4.2, 4.9), (7.6, 4.7, 11.1), "band"),
        fbox((8.4, 4.4, 4.9), (12.1, 4.9, 11.1), "band"),
        fbox((7.6, 4.2, 4.9), (8.4, 5.0, 11.1), "wood"),
        # The cloth: under the east lid, over the front rim and down the front.
        fbox((8.6, 4.2, 4.6), (11.8, 4.4, 9.0), "cloth"),
        fbox((9.0, 1.9, 4.6), (11.6, 4.4, 4.8), "cloth"),
        fbox((9.3, 1.5, 4.62), (10.4, 1.9, 4.78), "cloth"),
        # Handle: two uprights from the bar and a grip across.
        fbox((7.7, 5.0, 5.5), (8.3, 7.6, 6.1), "wood"),
        fbox((7.7, 5.0, 9.9), (8.3, 7.6, 10.5), "wood"),
        fbox((7.65, 7.6, 5.5), (8.35, 8.2, 10.5), "wood"),
    ]
    C.add_json("picnicbasket", JAVA + "novelties/BlockPicnicBasket.java", "picnicbasket",
               OrderedDict([("wicker", "wicker"), ("band", "wicker_band"), ("cloth", "gingham"),
                            ("wood", "handle_wood")]),
               els, "wicker")


# ==========================================================================================
# Cookies: a plate of chocolate chip cookies
# ==========================================================================================
def cookie_texture():
    rng = random.Random(61)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 8.0
            base = (198, 140, 72) if d < 0.7 else (176, 116, 54)
            img.putpixel((x, y), shade(base, rng.uniform(0.92, 1.06)) + (255,))
    for _ in range(9):
        x, y = rng.randint(3, 12), rng.randint(3, 12)
        rect(img, x, y, x + 2, y + 1, (62, 36, 22))
        put(img, x, y + 1, (74, 44, 26))
    return img


def cookies():
    C.tex("plate", paint((236, 236, 232), seed=62, grain=3))
    C.tex("cookie", cookie_texture())
    o = Obj()
    lathe(o, "plate", [(0.0, 0.0), (1.8, 0.0), (2.0, 0.1), (2.9, 0.3), (3.0, 0.38),
                       (2.8, 0.36), (1.9, 0.2), (0.0, 0.2)], at(8, 0, 8), sides=24)
    prof = [(0.0, 0.0), (0.56, 0.0), (0.6, 0.08), (0.56, 0.17), (0.0, 0.17)]

    def cookie(fr):
        lathe(o, "cookie", prof, fr, sides=12, uvf=planar_uv(1.25))
    # Six round the plate, leaning on the rim, and three piled in the middle.
    for i in range(6):
        a = math.radians(30 + 60 * i)
        x, z = 8 + 1.55 * math.cos(a), 8 + 1.55 * math.sin(a)
        tilt = turned("x", 10, (0, 0, 0))
        spin = turned("y", -math.degrees(a) + 90, (x, 0.21 + 0.02 * (i % 2), z))
        cookie(spin.then(tilt))
    cookie(at(7.7, 0.21, 7.8))
    cookie(turned("y", 40, (8.3, 0.4, 8.2)).then(turned("z", 8)))
    cookie(turned("y", 110, (7.9, 0.58, 7.9)).then(turned("x", -6)))
    C.add_obj("cookies", JAVA + "furniture/BlockCookies.java", "cookies", o,
              OrderedDict([("plate", "plate"), ("cookie", "cookie")]), "cookie")


# ==========================================================================================
# Xylophone: six rainbow bars on two rails, two mallets
# ==========================================================================================
BAR_COLOURS = [(208, 40, 40), (232, 124, 30), (236, 204, 44), (60, 160, 70), (40, 100, 200),
               (130, 60, 170)]


def xylophone():
    for i, c in enumerate(BAR_COLOURS):
        C.tex("bar_%d" % i, paint(c, seed=70 + i, grain=5))
    C.tex("rail_wood", wood((170, 120, 70), seed=77, boards=2))
    C.tex("stud", metal((200, 200, 204), seed=78))
    C.tex("mallet_head", paint((220, 60, 50), seed=79))
    els = [
        fbox((0.4, 0.0, 5.0), (15.6, 1.6, 6.2), "rail"),
        fbox((0.4, 0.0, 9.8), (15.6, 1.6, 11.0), "rail"),
    ]
    tex = OrderedDict([("rail", "rail_wood"), ("stud", "stud"), ("head", "mallet_head")])
    # Bar k sits over the sixth of the block the class reads as note k (hitX for a
    # north/south facing), longest and lowest at x = 0.
    for k in range(6):
        cx = 16 * (k + 0.5) / 6
        L = 10.0 - 0.8 * k
        tex["b%d" % k] = "bar_%d" % k
        els.append(fbox((cx - 1.05, 1.6, 8 - L / 2), (cx + 1.05, 2.2, 8 + L / 2), "b%d" % k))
        for z in (5.35, 10.25):
            els.append(fbox((cx - 0.2, 2.2, z), (cx + 0.2, 2.35, z + 0.4), "stud",
                            faces=("north", "south", "east", "west", "up")))
    # Mallets lying in front.
    for x, ang in ((4.0, 22.5), (9.5, -22.5)):
        r = ("y", ang, (x + 3, 0, 2.2))
        els.append(fbox((x, 0.0, 2.0), (x + 5.6, 0.35, 2.35), "rail", rot=r))
        els.append(fbox((x + 5.4, 0.0, 1.7), (x + 6.4, 0.95, 2.65), "head", rot=r))
    C.add_json("xylophone", JAVA + "novelties/BlockXylophone.java", "xylophone", tex, els,
               "rail_wood")


# ==========================================================================================
# Water dispenser: an office water cooler with a 5-gallon bottle on top
# ==========================================================================================
def waterdispenser():
    body = paint((232, 232, 226), seed=81, grain=3)
    C.tex("cooler_body", body)
    panel = paint((70, 74, 80), seed=82, grain=3)
    C.tex("cooler_panel", panel)
    grille = paint((150, 154, 158), seed=83, grain=2)
    for y in range(0, 16, 2):
        rect(grille, 0, y, 16, y + 1, (96, 100, 104))
    C.tex("drip_grille", grille)
    C.tex("tap_hot", paint((200, 40, 40), seed=84))
    C.tex("tap_cold", paint((40, 90, 200), seed=85))
    bottle = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            air = y < 5          # the top of the upturned bottle is air
            c = (170, 212, 236) if air else (80, 150, 214)
            if x in (5, 6):
                c = shade(c, 1.22)
            if x in (12, 13):
                c = shade(c, 0.85)
            if y == 5:
                c = (200, 230, 246)
            bottle.putpixel((x, y), clamp(c) + (255,))
    C.tex("bottle", bottle)
    o = Obj()
    x0, x1, z0, z1 = 5.4, 10.6, 5.3, 10.7
    for x in (x0, x1 - 0.5):
        for z in (z0, z1 - 0.5):
            obox(o, "panel", (x, 0.0, z), (x + 0.5, 0.3, z + 0.5), faces=("x-", "x+", "z-", "z+"))
    obox(o, "body", (x0, 0.3, z0), (x1, 8.5, z1))
    obox(o, "body", (x0, 12.0, z0), (x1, 15.0, z1))
    obox(o, "body", (x0, 8.5, z0), (x0 + 0.6, 12.0, z1), faces=("x-", "x+", "z-", "z+"))
    obox(o, "body", (x1 - 0.6, 8.5, z0), (x1, 12.0, z1), faces=("x-", "x+", "z-", "z+"))
    obox(o, "panel", (x0 + 0.6, 8.5, z0 + 0.6), (x1 - 0.6, 12.0, z1), faces=("z-",))
    obox(o, "body", (x0 + 0.6, 8.5, z0 + 0.6), (x1 - 0.6, 12.0, z1), faces=("z+",))
    obox(o, "grille", (6.3, 8.5, z0), (9.7, 8.9, 6.2), faces=("x-", "x+", "y+", "z-"))
    for mat, x in (("hot", 6.8), ("cold", 8.6)):
        obox(o, mat, (x, 10.6, z0 - 0.1), (x + 0.6, 11.5, z0 + 0.6))
        obox(o, mat, (x + 0.15, 10.0, z0 + 0.05), (x + 0.45, 10.6, z0 + 0.35))
    # The bottle, upside down, its neck in the cooler's top.
    lathe(o, "bottle", [(0.9, 14.8), (0.9, 15.8), (1.5, 16.3), (2.2, 17.2), (2.2, 19.0),
                        (2.35, 19.2), (2.2, 19.4), (2.2, 21.6), (2.35, 21.8), (2.2, 22.0),
                        (2.2, 22.4), (1.9, 23.0), (1.0, 23.35), (0.0, 23.4)], at(8, 0, 8),
          sides=20, uvf=lambda uf, k, r, y, a: (uf, (23.4 - y) / 8.6))
    C.add_obj("waterdispenser", JAVA + "novelties/BlockWaterDispenser.java", "waterdispenser",
              o, OrderedDict([("body", "cooler_body"), ("panel", "cooler_panel"),
                              ("grille", "drip_grille"), ("hot", "tap_hot"),
                              ("cold", "tap_cold"), ("bottle", "bottle")]), "cooler_body")


# ==========================================================================================
# Garden flamingo: a pink plastic lawn flamingo on two wire legs
# ==========================================================================================
def gardenflamingo():
    pink = (238, 122, 160)
    C.tex("flamingo_pink", paint(pink, seed=91, grain=6))
    wing = paint(shade(pink, 0.88), seed=92, grain=5)
    for y in range(0, 16, 3):
        rect(wing, 0, y, 16, y + 1, shade(pink, 0.74))
    C.tex("flamingo_wing", wing)
    C.tex("beak_black", paint((30, 30, 32), seed=93))
    C.tex("beak_pale", paint((236, 214, 190), seed=94))
    C.tex("wire", metal((150, 152, 150), seed=95))
    r = lambda ang, o: ("x", ang, o)  # noqa: E731
    els = [
        fbox((6.7, 8.2, 6.5), (9.3, 10.8, 11.5), "pink"),
        fbox((7.0, 10.8, 7.0), (9.0, 11.3, 11.0), "pink"),
        fbox((7.0, 7.7, 7.2), (9.0, 8.2, 10.8), "pink"),
        fbox((7.0, 8.6, 5.8), (9.0, 10.6, 6.5), "pink"),
        fbox((6.55, 8.8, 7.6), (9.45, 10.5, 11.2), "wing"),
        fbox((7.2, 10.0, 11.3), (8.8, 10.8, 13.2), "wing", rot=r(-22.5, (8, 10.4, 11.3))),
        # Neck: forward and up from the chest, then back up to the head.
        fbox((7.5, 10.2, 5.8), (8.5, 13.0, 6.8), "pink", rot=r(-22.5, (8, 10.2, 6.3))),
        fbox((7.55, 12.5, 4.8), (8.45, 14.9, 5.7), "pink", rot=r(22.5, (8, 12.5, 5.25))),
        fbox((7.35, 14.2, 5.0), (8.65, 15.4, 6.8), "pink"),
        fbox((7.6, 14.0, 3.6), (8.4, 14.8, 5.2), "pale", rot=r(-22.5, (8, 14.8, 5.2))),
        fbox((7.62, 14.02, 3.3), (8.38, 14.78, 4.2), "black", rot=r(-22.5, (8, 14.8, 5.2))),
        fbox((7.3, 14.8, 5.4), (8.7, 15.1, 5.75), "black"),
        fbox((7.4, 0.0, 8.8), (7.65, 8.2, 9.05), "wire"),
        fbox((8.35, 0.0, 8.8), (8.6, 8.2, 9.05), "wire"),
    ]
    C.add_json("gardenflamingo", JAVA + "furniture/BlockGardenFlamingo.java", "gardenflamingo",
               OrderedDict([("pink", "flamingo_pink"), ("wing", "flamingo_wing"),
                            ("black", "beak_black"), ("pale", "beak_pale"), ("wire", "wire")]),
               els, "flamingo_pink")


if __name__ == "__main__":
    for _build in (barberpole, birdhouse, doghouse, waterpump, coffeecup, picnicbasket, cookies,
                   xylophone, waterdispenser, gardenflamingo):
        _build()
    sys.exit(C.main())
