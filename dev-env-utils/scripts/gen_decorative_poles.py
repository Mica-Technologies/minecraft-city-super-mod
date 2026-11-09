#!/usr/bin/env python3
"""
gen_decorative_poles.py -- decorative and historic street light poles and their post-top
fixtures (GitHub #99): the Los Angeles electroliers, the Anaheim Resort Area pole and the
reproduction cast-iron set.

TWO HALVES, TWO MODULES
-----------------------
* The POLES are Roads blocks (``BlockTrafficPoleDecorative``, Traffic Accessories), one per
  design, stacked to any height. Each block decides its own ends from its neighbours exactly as
  the concrete pole does (CONCRETE_POLE_SYSTEM.md): a cast base on the ground, the design's cap in
  the open, its capital under a post-top fixture, nothing against another block of the pole. The
  shaft is drawn by every block and the ends are submodels named in model space, as the pedestal
  pole's are (PEDESTAL_POLE_SYSTEM.md, "Which end is which").
* The FIXTURES are Lighting blocks (``BlockBrightLightFactory.PostTop`` / ``PostTopAimed``): the
  lanterns, globes and multi-lamp heads that sit on a capital. They carry ``ICsmPostTopFixture``,
  so the pole under them shows its capital. Lit and unlit is a ``#unshaded_lens`` retexture per
  STATE, as for every OBJ street light; the light-up air is the bright light system's own.

Every part is authored standing (Y up) in 1/16 block units and written in 0..1 block space. The
textures are generated swatches and small glass sprites in Core
(``textures/blocks/decorativepoles``), because both modules use them.

SCALE
-----
The roadway fixtures are drawn about 1.4 times true size (an M-400A is 15 px for its 31.8 in).
These fixtures are too large for that: a Wilshire Special lantern is 7.5 ft from its base to its
finial, which at 1.4 would be over three blocks. They are drawn at about 0.6 to 0.8 times that
scale instead, so a lantern stands one and a half to two blocks tall over its pole and the
multi-lamp heads stay within half a block of their own block each side. An OBJ larger than its
block is drawn from its block's section, so it is kept to that half block (the Holophane Mongoose
and the GE Powerbracket already reach past their blocks the same way).

Usage:
    py gen_decorative_poles.py               # write every asset
    py gen_decorative_poles.py --check       # fail if the tree differs from what this writes
    py gen_decorative_poles.py --fragments   # print the tab and lang lines
"""
import argparse
import io
import json
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

ROADS = layout.owner_of_folder("trafficaccessories")
LIGHTING = layout.owner_of_folder("lighting")
CORE = layout.CORE

POLE_MODEL_REL = "models/block/trafficaccessories/shared_models"
POLE_PREFIX = "csm:trafficaccessories/shared_models"
LIGHT_MODEL_REL = "models/block/lighting/shared_models"
LIGHT_PREFIX = "csm:lighting/shared_models"
TEX_REL = "textures/blocks/decorativepoles"
TEX_PREFIX = "csm:blocks/decorativepoles"
POLE_MTL = "decopole.mtl"
LIGHT_MTL = "decolight.mtl"
CONCRETE = "csm:blocks/trafficsignals/shared_textures/concrete_light_pole"

INSET = 0.02          # keeps faces off the block boundary planes
AXIS = 8.0


# =============================================================================== vector helpers
def vsub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def vadd(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def vmul(a, k):
    return (a[0] * k, a[1] * k, a[2] * k)


def vdot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def vcross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def vlen(a):
    return math.sqrt(vdot(a, a))


def vnorm(a):
    n = vlen(a)
    return (a[0] / n, a[1] / n, a[2] / n) if n > 1e-12 else (0.0, 1.0, 0.0)


def pingpong(x):
    x = x % 2.0
    return x if x <= 1.0 else 2.0 - x


def uv(u, v):
    """Into the sprite's middle half, mirrored: block textures live in an atlas with no wrap, and
    the border is where mip levels bleed."""
    return (0.25 + 0.5 * pingpong(u), 1.0 - (0.25 + 0.5 * pingpong(v)))


# =============================================================================== transforms
class Xf:
    """An affine map p -> M p + t with M a rotation times a uniform scale, so normals map by M."""

    def __init__(self, m=None, t=(0.0, 0.0, 0.0)):
        self.m = m or ((1.0, 0.0, 0.0), (0.0, 1.0, 0.0), (0.0, 0.0, 1.0))
        self.t = t

    def apply(self, p):
        m = self.m
        return (m[0][0] * p[0] + m[0][1] * p[1] + m[0][2] * p[2] + self.t[0],
                m[1][0] * p[0] + m[1][1] * p[1] + m[1][2] * p[2] + self.t[1],
                m[2][0] * p[0] + m[2][1] * p[1] + m[2][2] * p[2] + self.t[2])

    def normal(self, n):
        m = self.m
        return vnorm((m[0][0] * n[0] + m[0][1] * n[1] + m[0][2] * n[2],
                      m[1][0] * n[0] + m[1][1] * n[1] + m[1][2] * n[2],
                      m[2][0] * n[0] + m[2][1] * n[1] + m[2][2] * n[2]))

    def then(self, other):
        """other after self."""
        m = tuple(tuple(sum(other.m[i][k] * self.m[k][j] for k in range(3)) for j in range(3))
                  for i in range(3))
        t = other.apply(self.t)
        return Xf(m, t)


def rot_y(deg, cx=AXIS, cz=AXIS):
    """Turn about the vertical line through (cx, cz); +deg turns +X toward -Z (north)."""
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    m = ((c, 0.0, s), (0.0, 1.0, 0.0), (-s, 0.0, c))
    return Xf(m, (cx - c * cx - s * cz, 0.0, cz + s * cx - c * cz))


def move(dx, dy, dz):
    return Xf(None, (dx, dy, dz))


# Standing frame -> model frame, the pedestal pole's two maps (both proper rotations).
TO_SOUTH = Xf(((1.0, 0.0, 0.0), (0.0, 0.0, 1.0), (0.0, -1.0, 0.0)), (0.0, 0.0, 16.0))
TO_NORTH = Xf(((1.0, 0.0, 0.0), (0.0, 0.0, -1.0), (0.0, 1.0, 0.0)), (0.0, 16.0, 0.0))


# =============================================================================== mesh
class Mesh:
    """Triangles in materials. Every triangle is wound to face a reference direction the
    primitive supplies (outward), and carries its own geometric normal, so nothing can come out
    inside-out and every face shades flat, as a baked quad does anyway."""

    def __init__(self):
        self.v, self.vt, self.vn = [], [], []
        self.faces = []                      # (material, [(vi, ti, ni) x 3])
        self._ix = ({}, {}, {})
        self.stack = [Xf()]

    # -- frames
    def push(self, xf):
        self.stack.append(xf.then(self.stack[-1]))

    def pop(self):
        self.stack.pop()

    class _Frame:
        def __init__(self, mesh, xf):
            self.mesh, self.xf = mesh, xf

        def __enter__(self):
            self.mesh.push(self.xf)

        def __exit__(self, *a):
            self.mesh.pop()

    def frame(self, xf):
        return Mesh._Frame(self, xf)

    # -- storage
    def _add(self, k, store, item):
        key = tuple(round(c, 6) for c in item)
        idx = self._ix[k]
        if key not in idx:
            store.append(item)
            idx[key] = len(store)
        return idx[key]

    def tri(self, mat, pts, uvs, ref):
        xf = self.stack[-1]
        p = [xf.apply(q) for q in pts]
        r = xf.normal(ref)
        geo = vcross(vsub(p[1], p[0]), vsub(p[2], p[0]))
        area = vlen(geo)
        if area < 1e-7:
            return
        if vdot(geo, r) < 0:
            p = [p[0], p[2], p[1]]
            uvs = [uvs[0], uvs[2], uvs[1]]
            geo = vmul(geo, -1.0)
        n = vnorm(geo)
        ni = self._add(2, self.vn, n)
        self.faces.append((mat, [(self._add(0, self.v, q), self._add(1, self.vt, t), ni)
                                 for q, t in zip(p, uvs)]))

    def quad(self, mat, pts, uvs, ref):
        self.tri(mat, [pts[0], pts[1], pts[2]], [uvs[0], uvs[1], uvs[2]], ref)
        self.tri(mat, [pts[0], pts[2], pts[3]], [uvs[0], uvs[2], uvs[3]], ref)

    # -- output
    def bounds(self):
        return ([min(p[k] for p in self.v) for k in range(3)],
                [max(p[k] for p in self.v) for k in range(3)])

    def tris(self):
        return len(self.faces)

    def transformed(self, xf):
        out = Mesh()
        for mat, f in self.faces:
            pts = [self.v[i - 1] for i, _t, _n in f]
            uvs = [self.vt[t - 1] for _i, t, _n in f]
            n = self.vn[f[0][2] - 1]
            out.tri(mat, [xf.apply(q) for q in pts], uvs, xf.normal(n))
        return out

    def merge(self, other):
        for mat, f in other.faces:
            pts = [other.v[i - 1] for i, _t, _n in f]
            uvs = [other.vt[t - 1] for _i, t, _n in f]
            self.tri(mat, pts, uvs, other.vn[f[0][2] - 1])

    def text(self, name, mtl, header):
        out = io.StringIO()
        out.write("# Generated by dev-env-utils/scripts/gen_decorative_poles.py -- do not edit\n")
        out.write("# %s\nmtllib %s\no %s\n" % (header, mtl, name))
        for p in self.v:
            out.write("v %.6f %.6f %.6f\n" % (p[0] / 16.0, p[1] / 16.0, p[2] / 16.0))
        for t in self.vt:
            out.write("vt %.6f %.6f\n" % t)
        for n in self.vn:
            out.write("vn %.6f %.6f %.6f\n" % n)
        for mat in sorted({m for m, _f in self.faces}):
            out.write("usemtl %s\n" % mat)
            for m, f in self.faces:
                if m == mat:
                    out.write("f %s\n" % " ".join("%d/%d/%d" % i for i in f))
        return out.getvalue()


# =============================================================================== primitives
def ring_angle(i, sides, phase):
    return 2.0 * math.pi * (i + phase) / sides


def lathe(m, mat, prof, sides=16, cx=AXIS, cz=AXIS, top=False, bottom=False, phase=0.5,
          flutes=0, depth=0.0, flute_y=None, uscale=1.0, vscale=1.0 / 16.0):
    """Surface of revolution about (cx, *, cz): ``prof`` is (radius, y) walked along the outer
    surface from the bottom up, so the outward side is (dy, -dr): a step inward at one height is a
    ledge facing up, a step outward one facing down. ``flutes`` cuts that many rounded grooves
    ``depth`` deep where ``flute_y(y)`` is true (all heights if None); ``sides`` should then be a
    multiple of the flute count."""

    def radius(r, y, a):
        if flutes and r > depth and (flute_y is None or flute_y(y)):
            return r - depth * 0.5 * (1.0 - math.cos(flutes * a))
        return r

    angles = [ring_angle(i, sides, phase) for i in range(sides)]
    for k in range(len(prof) - 1):
        (ra, ya), (rb, yb) = prof[k], prof[k + 1]
        if abs(ra - rb) < 1e-9 and abs(ya - yb) < 1e-9:
            continue
        dr, dy = rb - ra, yb - ya
        for i in range(sides):
            a0, a1 = angles[i], angles[(i + 1) % sides]
            am = 0.5 * (a0 + a1) if i < sides - 1 else a0 + math.pi / sides
            ref = (dy * math.cos(am), -dr, dy * math.sin(am))
            if abs(dy) < 1e-9 and abs(dr) < 1e-9:
                continue
            p00 = (cx + radius(ra, ya, a0) * math.cos(a0), ya, cz + radius(ra, ya, a0) * math.sin(a0))
            p01 = (cx + radius(ra, ya, a1) * math.cos(a1), ya, cz + radius(ra, ya, a1) * math.sin(a1))
            p11 = (cx + radius(rb, yb, a1) * math.cos(a1), yb, cz + radius(rb, yb, a1) * math.sin(a1))
            p10 = (cx + radius(rb, yb, a0) * math.cos(a0), yb, cz + radius(rb, yb, a0) * math.sin(a0))
            u0, u1 = uscale * i / sides, uscale * (i + 1) / sides
            v0, v1 = ya * vscale, yb * vscale
            if rb < 1e-9:
                m.tri(mat, [p00, p01, p10], [uv(u0, v0), uv(u1, v0), uv(0.5 * (u0 + u1), v1)], ref)
            elif ra < 1e-9:
                m.tri(mat, [p00, p11, p10], [uv(0.5 * (u0 + u1), v0), uv(u1, v1), uv(u0, v1)], ref)
            else:
                m.quad(mat, [p00, p01, p11, p10], [uv(u0, v0), uv(u1, v0), uv(u1, v1), uv(u0, v1)],
                       ref)
    if bottom and prof[0][0] > 1e-9:
        disc(m, mat, prof[0][0], prof[0][1], (0.0, -1.0, 0.0), sides, cx, cz, phase)
    if top and prof[-1][0] > 1e-9:
        disc(m, mat, prof[-1][0], prof[-1][1], (0.0, 1.0, 0.0), sides, cx, cz, phase)


def disc(m, mat, r, y, ref, sides=16, cx=AXIS, cz=AXIS, phase=0.5):
    c = (cx, y, cz)
    for i in range(sides):
        a0, a1 = ring_angle(i, sides, phase), ring_angle(i + 1, sides, phase)
        pa = (cx + r * math.cos(a0), y, cz + r * math.sin(a0))
        pb = (cx + r * math.cos(a1), y, cz + r * math.sin(a1))
        m.tri(mat, [c, pa, pb], [uv(0.5, 0.5), uv(0.5 + 0.5 * math.cos(a0), 0.5 + 0.5 * math.sin(a0)),
                                 uv(0.5 + 0.5 * math.cos(a1), 0.5 + 0.5 * math.sin(a1))], ref)


def ellipsoid_profile(cy, rx, ry, steps, y_from=None, y_to=None, r_end=0.0):
    """(radius, y) rows of an ellipse's outline from the bottom pole up, clipped to y_from..y_to
    (a globe that sits in a holder starts at the holder's lip, not at its bottom pole)."""
    rows = []
    for k in range(steps + 1):
        t = math.pi * (1.0 - k / steps)          # pi (bottom) .. 0 (top)
        y = cy + ry * math.cos(t)
        r = rx * math.sin(t)
        rows.append((r, y))
    if y_from is not None:
        y0 = y_from
        r0 = rx * math.sqrt(max(0.0, 1.0 - ((y0 - cy) / ry) ** 2))
        rows = [(r0, y0)] + [p for p in rows if p[1] > y0 + 1e-6]
    if y_to is not None:
        y1 = y_to
        r1 = rx * math.sqrt(max(0.0, 1.0 - ((y1 - cy) / ry) ** 2))
        rows = [p for p in rows if p[1] < y1 - 1e-6] + [(max(r1, r_end), y1)]
    return rows


def box(m, mat, lo, hi):
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    q = [uv(0, 0), uv(1, 0), uv(1, 1), uv(0, 1)]
    m.quad(mat, [(x0, y0, z0), (x0, y1, z0), (x0, y1, z1), (x0, y0, z1)], q, (-1, 0, 0))
    m.quad(mat, [(x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (x1, y0, z1)], q, (1, 0, 0))
    m.quad(mat, [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)], q, (0, -1, 0))
    m.quad(mat, [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], q, (0, 1, 0))
    m.quad(mat, [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)], q, (0, 0, -1))
    m.quad(mat, [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], q, (0, 0, 1))


def square_stack(m, mat, prof, cx=AXIS, cz=AXIS, top=True, bottom=True, vscale=1.0 / 16.0):
    """A stack of square sections (half-width, y), bottom up, the pedestal base's way: the four
    sides of a ring are a lathe with four sides turned so its flats face the compass."""
    lathe(m, mat, [(h * math.sqrt(2.0), y) for h, y in prof], sides=4, cx=cx, cz=cz,
          top=top, bottom=bottom, phase=0.5, uscale=4.0, vscale=vscale)


def frame_along(d):
    """Two unit vectors perpendicular to d."""
    d = vnorm(d)
    up = (0.0, 1.0, 0.0) if abs(d[1]) < 0.9 else (1.0, 0.0, 0.0)
    a = vnorm(vcross(d, up))
    b = vnorm(vcross(a, d))
    return a, b


def tube(m, mat, pts, radii, sides=8, cap0=True, cap1=True, phase=0.0):
    """A tube along a polyline with parallel-transported rings; ``radii`` one per point (or a
    single number)."""
    if not isinstance(radii, (list, tuple)):
        radii = [radii] * len(pts)
    n = len(pts)
    tangents = []
    for k in range(n):
        a = pts[max(k - 1, 0)]
        b = pts[min(k + 1, n - 1)]
        tangents.append(vnorm(vsub(b, a)))
    ea, eb = frame_along(tangents[0])
    rings = []
    length = 0.0
    for k in range(n):
        if k:
            t0, t1 = tangents[k - 1], tangents[k]
            # transport ea from t0 to t1
            ax = vcross(t0, t1)
            s = vlen(ax)
            if s > 1e-9:
                ax = vmul(ax, 1.0 / s)
                c = max(-1.0, min(1.0, vdot(t0, t1)))
                ang = math.atan2(s, c)
                ea = rotate_about(ea, ax, ang)
            ea = vnorm(vsub(ea, vmul(t1, vdot(ea, t1))))
            eb = vnorm(vcross(ea, t1))
            length += vlen(vsub(pts[k], pts[k - 1]))
        ring = []
        for i in range(sides):
            a = 2.0 * math.pi * (i + phase) / sides
            d = vadd(vmul(ea, math.cos(a)), vmul(eb, math.sin(a)))
            ring.append((vadd(pts[k], vmul(d, radii[k])), d))
        rings.append((ring, length / 16.0))
    for k in range(n - 1):
        ra, va = rings[k]
        rb, vb = rings[k + 1]
        for i in range(sides):
            j = (i + 1) % sides
            ref = vadd(vadd(ra[i][1], ra[j][1]), vadd(rb[i][1], rb[j][1]))
            m.quad(mat, [ra[i][0], ra[j][0], rb[j][0], rb[i][0]],
                   [uv(i / sides, va), uv((i + 1) / sides, va), uv((i + 1) / sides, vb),
                    uv(i / sides, vb)], ref)
    if cap0 and radii[0] > 1e-6:
        fan(m, mat, pts[0], [p for p, _d in rings[0][0]], vmul(tangents[0], -1.0))
    if cap1 and radii[-1] > 1e-6:
        fan(m, mat, pts[-1], [p for p, _d in rings[-1][0]], tangents[-1])


def rotate_about(v, axis, ang):
    c, s = math.cos(ang), math.sin(ang)
    return vadd(vadd(vmul(v, c), vmul(vcross(axis, v), s)), vmul(axis, vdot(axis, v) * (1 - c)))


def fan(m, mat, centre, ring, ref):
    for i in range(len(ring)):
        j = (i + 1) % len(ring)
        m.tri(mat, [centre, ring[i], ring[j]], [uv(0.5, 0.5), uv(0.2, 0.3), uv(0.8, 0.3)], ref)


def beam(m, mat, a, b, w):
    """A square bar from a to b, w across."""
    tube(m, mat, [a, b], w / math.sqrt(2.0), sides=4, phase=0.5)


def star(m, mat, centre, r_out, r_in, thick, points=5, facing=(0.0, 0.0, 1.0)):
    """A star prism standing upright, its faces toward +-``facing`` (horizontal)."""
    f = vnorm(facing)
    side = vnorm(vcross((0.0, 1.0, 0.0), f))
    outline = []
    for k in range(points * 2):
        a = math.pi / 2.0 + math.pi * k / points
        r = r_out if k % 2 == 0 else r_in
        outline.append(vadd(centre, vadd(vmul(side, r * math.cos(a)), (0.0, r * math.sin(a), 0.0))))
    front = [vadd(p, vmul(f, thick / 2.0)) for p in outline]
    back = [vadd(p, vmul(f, -thick / 2.0)) for p in outline]
    fan(m, mat, vadd(centre, vmul(f, thick / 2.0)), front, f)
    fan(m, mat, vadd(centre, vmul(f, -thick / 2.0)), back, vmul(f, -1.0))
    for i in range(len(outline)):
        j = (i + 1) % len(outline)
        mid = vmul(vadd(outline[i], outline[j]), 0.5)
        m.quad(mat, [front[i], front[j], back[j], back[i]],
               [uv(0, 0), uv(1, 0), uv(1, 1), uv(0, 1)], vsub(mid, centre))


def blob(m, mat, c, r, sides=4, rings=2):
    """A small low-poly bud (an octahedron at 4 x 2): roses, knobs, bolt heads."""
    prof = [(0.0, c[1] - r)]
    for k in range(1, rings):
        t = math.pi * (1.0 - k / rings)
        prof.append((r * math.sin(t), c[1] + r * math.cos(t)))
    prof.append((0.0, c[1] + r))
    lathe(m, mat, prof, sides=sides, cx=c[0], cz=c[2], phase=0.0)


def arc_pts(c, r, a0, a1, steps, plane="zy"):
    """Points on a circular arc in a vertical plane through c: angle 0 along +z (or +x for
    plane 'xy'), 90 up."""
    out = []
    for k in range(steps + 1):
        a = math.radians(a0 + (a1 - a0) * k / steps)
        h, v = r * math.cos(a), r * math.sin(a)
        if plane == "zy":
            out.append((c[0], c[1] + v, c[2] + h))
        else:
            out.append((c[0] + h, c[1] + v, c[2]))
    return out


def spiral_pts(c, r0, r1, a0, a1, steps, plane="zy"):
    """A scroll: radius running r0..r1 while the angle runs a0..a1 (degrees)."""
    out = []
    for k in range(steps + 1):
        t = k / steps
        a = math.radians(a0 + (a1 - a0) * t)
        r = r0 + (r1 - r0) * t
        h, v = r * math.cos(a), r * math.sin(a)
        if plane == "zy":
            out.append((c[0], c[1] + v, c[2] + h))
        else:
            out.append((c[0] + h, c[1] + v, c[2]))
    return out


def bezier(p0, p1, p2, p3, steps):
    out = []
    for k in range(steps + 1):
        t = k / steps
        a, b, c, d = (1 - t) ** 3, 3 * (1 - t) ** 2 * t, 3 * (1 - t) * t * t, t ** 3
        out.append(tuple(a * p0[i] + b * p1[i] + c * p2[i] + d * p3[i] for i in range(3)))
    return out


# =============================================================================== textures
def _noise_png(size, top, bottom, grain, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        t = y / (size - 1.0)
        for x in range(size):
            j = rnd.uniform(-grain, grain)
            col = tuple(max(0, min(255, int(round(top[k] + (bottom[k] - top[k]) * t + j))))
                        for k in range(3))
            img.putpixel((x, y), col + (255,))
    return img


def _glass_png(size, centre, edge, seed, ribs=0, speckle=0.0):
    """Glass: a soft radial glow from the middle to the frame, optional vertical prism ribs and a
    hammered speckle (the frosted lantern panels of frames 07 and 12)."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            dx, dy = (x + 0.5) / size - 0.5, (y + 0.5) / size - 0.5
            d = min(1.0, math.sqrt(dx * dx + dy * dy) * 1.7)
            k = d * d
            col = [centre[i] + (edge[i] - centre[i]) * k for i in range(3)]
            if ribs:
                s = 0.5 + 0.5 * math.cos(2.0 * math.pi * ribs * (x + 0.5) / size)
                col = [c * (0.9 + 0.1 * s) for c in col]
            if speckle:
                j = rnd.uniform(-speckle, speckle)
                col = [c + j for c in col]
            img.putpixel((x, y), tuple(max(0, min(255, int(round(c)))) for c in col) + (255,))
    return img


def _tile_png():
    """Westwood Village's base: UCLA blue tiles with gold ones in a diagonal band, grouted."""
    img = Image.new("RGBA", (32, 32))
    blue, gold, grout = (38, 84, 150), (214, 168, 52), (196, 190, 176)
    for y in range(32):
        for x in range(32):
            if x % 4 == 0 or y % 4 == 0:
                c = grout
            else:
                tx, ty = x // 4, y // 4
                c = gold if (tx + ty) % 4 == 0 or ty == 3 else blue
                shade = 1.0 - 0.05 * ((x % 4) + (y % 4) - 3)
                c = tuple(int(min(255, v * shade)) for v in c)
            img.putpixel((x, y), c + (255,))
    return img


TEXTURES = {
    # Painted cast iron and steel. Each a gentle vertical gradient with grain, the swatch rule.
    "paint_la_bronze": lambda: _noise_png(16, (92, 82, 58), (64, 56, 40), 5, 11),
    "paint_la_green": lambda: _noise_png(16, (52, 74, 58), (34, 50, 40), 5, 12),
    # Anaheim P7-Green (Pantone 330 C), semi-gloss.
    "paint_anaheim_green": lambda: _noise_png(16, (40, 104, 88), (26, 78, 66), 4, 13),
    "paint_black": lambda: _noise_png(16, (50, 50, 52), (28, 28, 30), 4, 14),
    "paint_hollywood_grey": lambda: _noise_png(16, (170, 172, 172), (128, 130, 132), 4, 15),
    # Long Beach's citywide dark navy, and the cast aluminium of the California Heights cages.
    "paint_navy": lambda: _noise_png(16, (38, 50, 86), (24, 32, 58), 4, 16),
    "paint_aluminium": lambda: _noise_png(16, (196, 198, 200), (148, 150, 154), 5, 17),
    # Trim: Anaheim's Aztec Gold finial, the brass bands, the bronze lantern frames.
    "trim_gold": lambda: _noise_png(16, (226, 186, 96), (166, 124, 46), 6, 21),
    "trim_brass": lambda: _noise_png(16, (196, 160, 84), (132, 100, 46), 6, 22),
    "trim_bronze": lambda: _noise_png(16, (128, 92, 56), (82, 58, 36), 6, 23),
    # The Hollywood Special's stars: red plastic, lit from inside the head.
    "trim_star_red": lambda: _noise_png(16, (226, 78, 70), (196, 52, 50), 5, 24),
    "tile_westwood": _tile_png,
    # Glass, lit and unlit. Opal: the milk-white globes (frames 06, 09). Frosted: the hammered
    # amber-white lantern panels (frames 07, 12, 13). Prism: the ribbed acorn and teardrops.
    "glass_opal_on": lambda: _glass_png(16, (255, 250, 232), (252, 226, 176), 31),
    "glass_opal_off": lambda: _glass_png(16, (150, 148, 140), (118, 116, 110), 32),
    "glass_frosted_on": lambda: _glass_png(32, (255, 236, 190), (238, 184, 104), 33, speckle=14),
    "glass_frosted_off": lambda: _glass_png(32, (140, 132, 112), (104, 98, 84), 34, speckle=10),
    "glass_prism_on": lambda: _glass_png(16, (255, 244, 214), (246, 206, 140), 35, ribs=4),
    "glass_prism_off": lambda: _glass_png(16, (150, 152, 150), (112, 116, 116), 36, ribs=4),
}


def tex(name):
    return "%s/%s" % (TEX_PREFIX, name)


# =============================================================================== poles
# A pole design: its id, label, the radius arms fit to, the shaft's radius, the base's half
# width, the paint, the trim, and the four builders (standing frame, one block).
class PoleDesign:
    def __init__(self, pid, label, fit_radius, shaft_r, base_half, body, trim, color,
                 shaft, base, cap, tenon, tile=None, source=""):
        self.pid, self.label = pid, label
        self.fit_radius, self.shaft_r, self.base_half = fit_radius, shaft_r, base_half
        self.body, self.trim, self.tile, self.color = body, trim, tile, color
        self.shaft, self.base, self.cap, self.tenon = shaft, base, cap, tenon
        self.source = source


def shaft_ends(m, r):
    """End discs a hair inside the block: the walls run the whole block, so stacked shafts meet
    without a seam, and an end that runs into a solid block is closed."""
    disc(m, "body", r, INSET, (0.0, -1.0, 0.0), 12, phase=0.0)
    disc(m, "body", r, 16.0 - INSET, (0.0, 1.0, 0.0), 12, phase=0.0)


def fluted_shaft(r, flutes, depth):
    """Three samples a flute: a crest and the two sides of the groove, which reads as fluting at
    a block's resolution for three quarters of the triangles of four."""
    def build(m):
        lathe(m, "body", [(r, 0.0), (r, 16.0)], sides=flutes * 3, flutes=flutes, depth=depth,
              phase=0.0)
        shaft_ends(m, r - depth * 0.6)
    return build


def plain_shaft(r, sides=16):
    def build(m):
        lathe(m, "body", [(r, 0.0), (r, 16.0)], sides=sides)
        shaft_ends(m, r)
    return build


def capital(r, prof, mat="body"):
    """A capital: the profile from just outside the shaft up to its flat top, closed by a disc
    a hair under the block's top, where the fixture's fitter stands."""
    def build(m):
        lathe(m, mat, [(r - 0.5, prof[0][1]), (r + 0.04, prof[0][1])] + prof, top=True)
    return build


def cap_lathe(r, prof, mat="body", extra=None):
    """The open-air end: the ledge under it reaches in past the flutes, so no groove shows a
    gap into the casting."""
    def build(m):
        lathe(m, mat, [(r - 0.5, prof[0][1]), (r + 0.04, prof[0][1])] + prof)
        if extra:
            extra(m)
    return build


# --- LA cast fluted electrolier (Wilshire Special, Metropolitan, Paragon) --------------------
def la_fluted_base(m):
    square_stack(m, "body", [(7.2, INSET), (7.2, 1.6), (6.6, 1.6), (6.6, 2.4), (6.0, 2.4)])
    # plain mouldings, the fluted drum between them, and ledges that close the grooves' ends
    lathe(m, "body", [(5.6, 2.4), (5.6, 3.2), (5.0, 3.8), (4.9, 4.4), (5.6, 4.9), (5.6, 5.5),
                      (4.9, 5.9), (4.7, 6.2), (4.1, 6.2)])
    lathe(m, "body", [(4.7, 6.2), (4.5, 10.2)], sides=48, flutes=16, depth=0.4, phase=0.0)
    lathe(m, "body", [(4.0, 10.2), (4.5, 10.2), (5.1, 10.6), (5.1, 11.3), (4.6, 11.7),
                      (4.3, 12.4), (4.04, 12.6), (3.5, 12.6)])


LA_FLUTED = PoleDesign(
    "lafluted", "Los Angeles Fluted Electrolier Pole", 4.0, 4.0, 7.2,
    tex("paint_la_bronze"), tex("trim_bronze"), "BLACK",
    fluted_shaft(4.0, 16, 0.45), la_fluted_base,
    cap_lathe(4.0, [(4.0, 11.0), (4.7, 11.6), (4.7, 12.4), (4.1, 12.8), (3.2, 13.6), (2.0, 14.3),
                    (1.1, 14.6), (1.3, 15.0), (0.7, 15.6), (0.0, 15.95)]),
    capital(4.0, [(4.0, 10.8), (4.8, 11.2), (4.8, 11.9), (4.3, 12.3), (4.9, 13.2), (5.8, 14.3),
                  (6.1, 15.0), (6.1, 15.98)]),
    source="the 1920s fluted cast-iron columns under the Wilshire Special and Metropolitan "
           "lanterns (BSL museum frames 01, 12)")


# --- Broadway Rose: fluted, with three garlands of roses climbing the shaft -----------------
ROSE_R = 4.0


def broadway_rose_shaft(m):
    fluted_shaft(ROSE_R, 12, 0.4)(m)
    # Three garlands, a third of a turn a block, so they run on unbroken through a stack: at the
    # top of a block garland k is where garland k+1 starts in the block above.
    for g in range(3):
        a0 = 2.0 * math.pi * g / 3.0
        pts = []
        steps = 6
        for k in range(steps + 1):
            y = 0.25 + 15.5 * k / steps
            a = a0 + (2.0 * math.pi / 3.0) * (y / 16.0)
            pts.append((AXIS + (ROSE_R + 0.15) * math.cos(a), y, AXIS + (ROSE_R + 0.15) * math.sin(a)))
        tube(m, "trim", pts, 0.42, sides=4, cap0=False, cap1=False)
        for k in range(4):
            y = 2.0 + 4.0 * k
            a = a0 + (2.0 * math.pi / 3.0) * (y / 16.0)
            rr = ROSE_R + 0.75
            blob(m, "trim", (AXIS + rr * math.cos(a), y, AXIS + rr * math.sin(a)), 1.15)


def broadway_rose_base(m):
    square_stack(m, "body", [(7.0, INSET), (7.0, 1.8), (6.2, 1.8), (6.2, 2.6), (5.6, 2.6)])
    lathe(m, "body", [(5.4, 2.6), (5.4, 3.4), (4.8, 4.0), (4.6, 5.0), (5.4, 5.6), (5.4, 6.2),
                      (4.9, 6.6), (4.9, 9.4), (5.3, 9.8), (5.3, 10.4), (4.6, 11.0), (4.04, 11.4), (3.5, 11.4)])
    # cast rose wreath round the base drum
    for k in range(8):
        a = 2.0 * math.pi * k / 8.0
        blob(m, "trim", (AXIS + 5.3 * math.cos(a), 8.0, AXIS + 5.3 * math.sin(a)), 1.0)


BROADWAY_ROSE = PoleDesign(
    "labroadwayrose", "Broadway Rose Electrolier Pole", 4.0, 4.0, 7.0,
    tex("paint_la_green"), tex("trim_bronze"), "BLACK",
    broadway_rose_shaft, broadway_rose_base,
    cap_lathe(ROSE_R, [(ROSE_R, 11.6), (4.9, 12.2), (4.9, 12.9), (4.0, 13.4), (2.6, 14.4),
                       (1.0, 15.0), (0.0, 15.95)]),
    capital(ROSE_R, [(ROSE_R, 10.4), (5.0, 10.9), (5.0, 11.6), (4.4, 12.0), (5.2, 13.0),
                     (6.0, 14.2), (6.2, 15.0), (6.2, 15.98)]),
    source="BSL: 'three ornate ribbons of roses climbing up their shafts'; 6th Street between "
           "Olive and Flower")


# --- City Design 815 concrete electrolier (frames 09, 13) -----------------------------------
def concrete_base(m):
    square_stack(m, "body", [(7.0, INSET), (7.0, 3.6), (6.4, 3.6), (6.4, 4.2), (5.8, 4.2)])
    lathe(m, "body", [(5.4, 4.2), (5.4, 5.0), (4.9, 6.6), (4.6, 8.0), (4.54, 8.6), (4.0, 8.6)])


CONCRETE_815 = PoleDesign(
    "laconcrete", "Los Angeles Concrete Electrolier Pole", 4.0, 4.5, 7.0,
    CONCRETE, tex("trim_bronze"), "SILVER",
    plain_shaft(4.5), concrete_base,
    cap_lathe(4.5, [(4.5, 12.0), (5.0, 12.6), (5.0, 13.4), (4.2, 14.2), (2.4, 15.2), (0.0, 15.95)]),
    capital(4.5, [(4.5, 9.0), (4.8, 10.6), (5.4, 12.2), (6.3, 13.8), (6.8, 14.8), (6.8, 15.98)]),
    source="City Design 815 Concrete Electrolier, Venice Blvd., c. 1920s (museum frames 09, 13: "
           "granite-faced concrete, a flared capital under a brass-banded globe holder)")


# --- Westwood Village: a bronze column on a base tiled in UCLA blue and gold ----------------
def westwood_base(m):
    # three closed solids that overlap rather than share faces: a plinth, the tiled die, a
    # cornice; then the round neck into the shaft
    square_stack(m, "body", [(6.8, INSET), (6.8, 1.4), (6.2, 1.4), (6.2, 2.0)])
    square_stack(m, "tile", [(5.9, 1.9), (5.9, 10.0)], top=False, bottom=False, vscale=1.0 / 8.0)
    square_stack(m, "body", [(6.5, 9.9), (6.5, 11.0), (5.0, 11.6)])
    lathe(m, "body", [(4.6, 11.5), (4.6, 12.2), (4.04, 12.8), (3.5, 12.8)])


WESTWOOD = PoleDesign(
    "lawestwood", "Westwood Village Lantern Pole", 4.0, 4.0, 6.8,
    tex("paint_la_bronze"), tex("trim_bronze"), "BLACK",
    plain_shaft(4.0), westwood_base,
    cap_lathe(4.0, [(4.0, 12.0), (4.6, 12.5), (4.6, 13.2), (3.6, 14.0), (1.8, 15.0), (0.0, 15.95)]),
    capital(4.0, [(4.0, 11.0), (4.7, 11.5), (4.7, 12.2), (4.2, 12.6), (5.2, 13.8), (5.6, 15.0),
                  (5.6, 15.98)]),
    tile=tex("tile_westwood"),
    source="BSL: Westwood Village lanterns 'rise from a base tiled in UCLA blue and gold'")


# --- Hollywood: a slim grey steel shaft on a round base cover --------------------------------
def hollywood_base(m):
    lathe(m, "body", [(5.2, INSET), (5.2, 1.0), (4.8, 1.4), (4.0, 6.0), (3.6, 7.0), (3.04, 7.4),
                      (2.5, 7.4)], bottom=True)


HOLLYWOOD = PoleDesign(
    "lahollywood", "Hollywood Boulevard Pole", 3.0, 3.0, 5.2,
    tex("paint_hollywood_grey"), tex("trim_gold"), "SILVER",
    plain_shaft(3.0), hollywood_base,
    cap_lathe(3.0, [(3.0, 13.0), (3.4, 13.4), (3.4, 14.0), (2.2, 14.8), (0.0, 15.95)]),
    capital(3.0, [(3.0, 11.6), (3.6, 12.0), (3.6, 12.8), (3.2, 13.2), (4.2, 14.2), (4.9, 15.0),
                  (4.9, 15.98)]),
    source="the slim steel poles of Hollywood Boulevard")


# --- Reproduction cast-iron post: slim, twelve flutes, a moulded base, a ball finial ---------
def repro_base(m):
    square_stack(m, "body", [(6.0, INSET), (6.0, 1.2), (5.4, 1.2), (5.4, 1.8)])
    lathe(m, "body", [(4.8, 1.8), (4.8, 2.4), (4.2, 3.0), (4.0, 4.0), (4.6, 4.6), (4.6, 5.2),
                      (3.9, 5.8), (3.6, 8.0), (3.9, 8.4), (3.9, 9.0), (3.4, 9.6), (3.2, 10.2),
                      (2.5, 10.2)])


REPRO = PoleDesign(
    "reprofluted", "Reproduction Fluted Cast Iron Post", 3.0, 3.0, 6.0,
    tex("paint_black"), tex("trim_gold"), "BLACK",
    fluted_shaft(3.0, 12, 0.35), repro_base,
    cap_lathe(3.0, [(3.0, 12.4), (3.5, 12.8), (3.5, 13.4), (2.6, 13.8), (1.2, 14.2), (1.5, 14.5),
                    (1.9, 15.0), (1.5, 15.6), (0.0, 15.95)]),
    capital(3.0, [(3.0, 11.4), (3.6, 11.8), (3.6, 12.4), (3.2, 12.8), (4.2, 14.0), (5.0, 15.0),
                  (5.0, 15.98)]),
    source="the Union Metal / Spring City reproductions of the cast-iron post")


# --- Anaheim Resort Area, City of Anaheim Street Light Design #749 --------------------------
# A 12-flute galvanized steel pole, 7.5 in at the base, in Anaheim P7-Green; a two-piece cast
# base with a door, 43 in tall; a cast ring with two banner arm mounts under the 8 ft arm; an
# Aztec Gold finial (drawing L 139 rev 5, 2019).
def anaheim_base(m):
    square_stack(m, "body", [(5.0, INSET), (5.0, 1.4), (4.5, 1.4), (4.5, 2.0)])
    lathe(m, "body", [(4.4, 1.9), (4.4, 2.6), (4.0, 3.2), (4.0, 3.6), (3.4, 3.6)])
    lathe(m, "body", [(4.0, 3.6), (3.8, 11.0)], sides=36, flutes=12, depth=0.35, phase=0.0)
    lathe(m, "body", [(3.3, 11.0), (3.8, 11.0), (4.3, 11.4), (4.3, 12.0), (3.7, 12.6),
                      (3.3, 13.6), (3.04, 14.2), (2.5, 14.2)])
    box(m, "body", (6.7, 5.0, 11.6), (9.3, 9.4, 12.05))       # the access door, street side


def anaheim_ring(m):
    """The cast ring's two banner arm mounts, and the Aztec Gold finial over the collar."""
    for sign in (1, -1):
        x0, x1 = AXIS + sign * 3.9, AXIS + sign * 6.6
        tube(m, "body", [(x0, 4.0, AXIS), (x1, 4.0, AXIS)], 0.4, sides=6, cap0=False)
        blob(m, "trim", (x1 + sign * 0.5, 4.0, AXIS), 0.7, sides=6, rings=3)
    lathe(m, "trim", [(1.2, 13.0), (1.7, 13.6), (1.0, 14.4), (1.9, 15.4), (2.1, 16.4),
                      (1.5, 17.4), (0.7, 18.6), (0.9, 19.3), (0.0, 21.0)], sides=12, bottom=True)


ANAHEIM = PoleDesign(
    "anaheim", "Anaheim Resort Area Pole", 3.0, 3.0, 5.0,
    tex("paint_anaheim_green"), tex("trim_gold"), "BLACK",
    fluted_shaft(3.0, 12, 0.35), anaheim_base,
    cap_lathe(3.0, [(3.0, 2.8), (3.9, 3.2), (4.2, 3.8), (4.2, 4.2), (3.9, 4.8), (3.04, 5.2),
                    (3.04, 11.6), (3.6, 12.0), (3.6, 12.8), (2.2, 13.3), (1.2, 13.3)],
              extra=anaheim_ring),
    capital(3.0, [(3.0, 11.0), (3.6, 11.5), (3.6, 12.2), (3.2, 12.6), (4.2, 14.0), (4.8, 15.2),
                  (4.8, 15.98)]),
    source="City of Anaheim Street Light Special Design #749 (L 139 rev 5)")


# --- California Heights (Long Beach, 1929): a slim navy steel post, an aluminium capital ------
def cal_heights_base(m):
    lathe(m, "body", [(4.6, INSET), (4.6, 0.9), (4.0, 1.3), (3.4, 4.0), (3.0, 6.4), (3.2, 6.8),
                      (3.2, 7.3), (2.64, 7.7), (2.2, 7.7)], bottom=True)


CAL_HEIGHTS = PoleDesign(
    "calheights", "California Heights Lamp Post", 3.0, 2.6, 4.6,
    tex("paint_navy"), tex("paint_aluminium"), "BLACK",
    plain_shaft(2.6), cal_heights_base,
    cap_lathe(2.6, [(2.6, 13.2), (3.0, 13.6), (3.0, 14.2), (1.8, 14.9), (0.0, 15.95)]),
    capital(2.6, [(2.6, 11.4), (3.0, 11.8), (3.0, 12.4), (2.8, 12.8), (3.6, 14.2), (4.4, 15.2),
                  (4.4, 15.98)], mat="trim"),
    source="the California Heights lampposts, Long Beach: navy post, silver leaf capital")


POLE_DESIGNS = [LA_FLUTED, BROADWAY_ROSE, CONCRETE_815, WESTWOOD, HOLLYWOOD, REPRO, ANAHEIM,
                CAL_HEIGHTS]


# --- the mount clamp every pole grows toward a device on its flank -----------------------------
def build_mount(m, r):
    """Model frame, pointing north from the axis, symmetric about its own axis (the pedestal
    pole's rule: one model is rotated into all four flanks)."""
    face = 8.0 - r - 0.15
    box(m, "body", (5.6, 5.6, face - 1.2), (10.4, 10.4, face + 1.4))
    tube(m, "body", [(8.0, 8.0, INSET), (8.0, 8.0, face - 1.0)], 1.4, sides=8, cap1=False)


def model_ref(prefix, name):
    return "%s/%s.obj" % (prefix, name)


def pole_parts(d):
    """{part: (mesh, header)} for one pole design, in model space."""
    parts = {}

    def standing(builder, xf, header):
        m = Mesh()
        with m.frame(xf):
            builder(m)
        return m, header

    parts["shaft"] = standing(d.shaft, TO_SOUTH, "shaft, along Z")
    for end, xf_top, xf_bottom in (("n", TO_SOUTH, TO_NORTH), ("s", TO_NORTH, TO_SOUTH)):
        parts["cap_" + end] = standing(d.cap, xf_top, "cap at the model-%s end" % end)
        parts["tenon_" + end] = standing(d.tenon, xf_top, "capital at the model-%s end" % end)
        parts["base_" + end] = standing(d.base, xf_bottom, "base at the model-%s end" % end)
    mount = Mesh()
    build_mount(mount, d.shaft_r)
    parts["mount"] = (mount, "flank clamp, pointing model north")
    inv = Mesh()
    d.base(inv)
    d.shaft(inv)
    d.cap(inv)
    parts["inv"] = (inv, "inventory icon: base, shaft and cap in one block")
    return parts


def pole_registry(d):
    return "decopole" + d.pid


def pole_blockstate(d):
    name = lambda part: model_ref(POLE_PREFIX, "decopole_%s_%s" % (d.pid, part))
    textures = {"#body": d.body, "#trim": d.trim, "#tile": d.tile or d.body, "particle": d.body}

    def sub(part, rotation=None):
        e = {"model": name(part), "custom": {"flip-v": True}, "textures": textures}
        if rotation is not None:
            e["transform"] = {"rotation": [{"x": rotation[0]}, {"y": rotation[1]}, {"z": rotation[2]}]}
        return e

    def ends(e):
        return {"none": {}, "cap": {"submodel": {"cap_" + e: sub("cap_" + e)}},
                "tenon": {"submodel": {"tenon_" + e: sub("tenon_" + e)}},
                "base": {"submodel": {"base_" + e: sub("base_" + e)}}}

    return {
        "forge_marker": 1,
        "defaults": {"model": name("shaft"), "custom": {"flip-v": True}, "textures": textures},
        "variants": {
            "inventory": [{"model": name("inv"), "custom": {"flip-v": True}, "textures": textures,
                           "transform": "forge:default-block"}],
            "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
                       "up": {"x": 270}, "down": {"x": 90}},
            "mounteast": {"false": {}, "true": {"submodel": {"mounte": sub("mount", (0, 90, 0))}}},
            "mountwest": {"false": {}, "true": {"submodel": {"mountw": sub("mount", (0, 270, 0))}}},
            "mountup": {"false": {}, "true": {"submodel": {"mountu": sub("mount", (270, 0, 0))}}},
            "mountdown": {"false": {}, "true": {"submodel": {"mountd": sub("mount", (90, 0, 0))}}},
            "endn": ends("n"),
            "ends": ends("s"),
        },
    }


# =============================================================================== fixtures
class Fixture:
    """A post-top fixture: its id, label, builder (standing frame; its fitter starts at y 0 on
    the capital), the glass it wears, whether its light is aimed ahead (a head hung forward of
    its pole), and the reference it follows. A third paint, where given in FIXTURE_PAINT, is the
    ``frame`` material: the mast or arm a head hangs from, when it is not the head's colour."""

    def __init__(self, fid, label, build, glass, aimed=False, source="", reach=-8.5):
        self.fid, self.label, self.build, self.glass = fid, label, build, glass
        self.aimed, self.source = aimed, source
        # How far ahead of its block (model north, -Z) the head may reach. Half a block unless
        # the head cannot be drawn that short.
        self.reach = reach


def fitter(m, r=4.6, top=2.4, neck=None, mat="body"):
    """The slip fitter that sits on a pole's capital (whose top is 6.1 across at most). It ends
    in exactly the ring the part above starts from (``neck``), so no annulus is left open
    between them to look down into."""
    neck = r - 1.8 if neck is None else neck
    lathe(m, mat, [(r, INSET), (r, 1.0), (r - 0.5, 1.4), (min(r - 1.4, neck + 0.6), 2.0),
                   (neck, top)], sides=12, bottom=True)


# --- the lanterns -----------------------------------------------------------------------------
def octagon_lantern(m, sides, y0, y_glass0, y_glass1, r_bot, r_mid, r_top, roof, body="body",
                    post="trim", horns=False, spire=None, figures=False):
    """A cast lantern: a bowl tapering down to the stem from y0, glass panels from y_glass0 to
    y_glass1 widening r_mid -> r_top, corner posts, a cornice band, a roof (glass or metal) up to
    a crown, and an optional spire, corner horns or corner figures."""
    lathe(m, body, [(r_bot, y0), (r_mid + 0.5, y_glass0 - 0.6), (r_mid + 0.5, y_glass0)],
          sides=sides, bottom=True, phase=0.0)
    # glass, a hair inside the posts
    lathe(m, "unshaded_lens", [(r_mid, y_glass0), (r_top, y_glass1)], sides=sides, phase=0.0)
    lathe(m, body, [(r_mid + 0.5, y_glass0), (r_mid, y_glass0)], sides=sides, phase=0.0)
    for i in range(sides):
        a = 2.0 * math.pi * i / sides
        p0 = (AXIS + (r_mid + 0.15) * math.cos(a), y_glass0, AXIS + (r_mid + 0.15) * math.sin(a))
        p1 = (AXIS + (r_top + 0.15) * math.cos(a), y_glass1, AXIS + (r_top + 0.15) * math.sin(a))
        beam(m, post, p0, p1, 0.55)
    yc = y_glass1
    rc = r_top + 0.7
    lathe(m, body, [(r_top, yc), (rc, yc), (rc, yc + 1.0), (r_top + 0.2, yc + 1.0)],
          sides=sides, phase=0.0)
    roof_mat, roof_top_r, roof_top_y = roof
    lathe(m, roof_mat, [(r_top + 0.2, yc + 1.0), (roof_top_r, roof_top_y)], sides=sides, phase=0.0)
    for i in range(sides):
        a = 2.0 * math.pi * i / sides
        p0 = (AXIS + (r_top + 0.35) * math.cos(a), yc + 1.0, AXIS + (r_top + 0.35) * math.sin(a))
        p1 = (AXIS + (roof_top_r + 0.15) * math.cos(a), roof_top_y,
              AXIS + (roof_top_r + 0.15) * math.sin(a))
        beam(m, post, p0, p1, 0.5)
    if horns:
        for i in range(0, sides, 2 if sides > 6 else 1):
            a = 2.0 * math.pi * i / sides
            base = (AXIS + rc * math.cos(a), yc + 0.5, AXIS + rc * math.sin(a))
            out = (math.cos(a), 0.0, math.sin(a))
            pts = bezier(base, vadd(base, vadd(vmul(out, 1.2), (0, 0.6, 0))),
                         vadd(base, vadd(vmul(out, 1.4), (0, 2.0, 0))),
                         vadd(base, vadd(vmul(out, 0.7), (0, 3.0, 0))), 4)
            tube(m, post, pts, [0.4, 0.35, 0.3, 0.22, 0.12], sides=4)
    if figures:
        for i in range(sides // 8, sides, sides // 4):
            a = 2.0 * math.pi * (i + 0.0) / sides
            fx, fz = AXIS + (rc - 0.95) * math.cos(a), AXIS + (rc - 0.95) * math.sin(a)
            # a standing figure: plinth, gown, shoulders, head
            lathe(m, post, [(0.9, yc + 1.0), (0.9, yc + 1.5), (0.75, yc + 1.5), (0.55, yc + 3.2),
                            (0.75, yc + 3.6), (0.4, yc + 3.9)], sides=5, cx=fx, cz=fz, top=True)
            blob(m, post, (fx, yc + 4.4, fz), 0.5, sides=5, rings=2)
    # crown and spire
    ct = roof_top_y
    if spire:
        lathe(m, body, [(roof_top_r + 0.3, ct), (roof_top_r + 0.3, ct + 0.7), (roof_top_r - 0.2, ct + 1.1)]
              + spire, sides=8, phase=0.0, bottom=True)
    else:
        lathe(m, body, [(roof_top_r + 0.3, ct), (roof_top_r + 0.3, ct + 0.6), (0.0, ct + 1.4)],
              sides=8, phase=0.0, bottom=True)


def build_wilshire(m):
    """The Wilshire Special (1928), from photos of the survivors by MacArthur Park: a tall
    four-sided bronze box, about twice as tall as wide, one pane of obscure glass a face; a
    scrollwork frieze top and bottom; a half-figure at each corner over a tapering pendant; four
    scrolled brackets under its floor; an overhanging cornice with an urn at each corner, and a
    stepped square dome under a bud finial."""
    half = 3.3
    y0, y1 = 6.4, 17.6
    fitter(m, 4.6, 2.6, 2.6)
    lathe(m, "body", [(2.6, 2.6), (2.3, 3.6), (2.9, 4.0), (2.9, 4.5), (2.2, 5.0), (2.2, 5.5)],
          sides=12)
    square_stack(m, "body", [(2.2, 5.5), (half + 0.4, 5.9), (half + 0.4, y0), (half, y0)],
                 bottom=True, top=False)
    for sx in (1.0, -1.0):
        for sz in (1.0, -1.0):
            # a bracket from the stem out under the floor's corner, curling at its end
            p = lambda r, y, sx=sx, sz=sz: (AXIS + sx * r, y, AXIS + sz * r)
            tube(m, "trim", [p(1.6, 3.4), p(2.4, 3.6), (p(3.3, 4.4)), p(3.7, 5.4), p(3.4, 5.9)],
                 0.38, sides=4)
    # the glass, a hair inside its frame, and the four corner posts
    lathe(m, "unshaded_lens", [(half * math.sqrt(2.0), y0), (half * math.sqrt(2.0), y1)], sides=4,
          phase=0.5, uscale=4.0)
    for sx in (1.0, -1.0):
        for sz in (1.0, -1.0):
            cx, cz = AXIS + sx * (half + 0.05), AXIS + sz * (half + 0.05)
            beam(m, "body", (cx, y0, cz), (cx, y1, cz), 0.7)
            # the corner figure: a pendant tapering down, a gowned half-figure, a head
            fx, fz = AXIS + sx * (half + 0.75), AXIS + sz * (half + 0.75)
            lathe(m, "trim", [(0.0, 9.0), (0.45, 10.4), (0.7, 11.6), (0.55, 12.8), (0.8, 13.6),
                              (0.35, 14.2)], sides=4, cx=fx, cz=fz, top=True)
            blob(m, "trim", (fx, 14.75, fz), 0.5, sides=4, rings=2)
    # the friezes: a band standing proud of the glass at the foot and the head of the panes
    # (each stands on the floor or under the cornice, so only its open edge gets a ledge)
    square_stack(m, "trim", [(half + 0.2, y0), (half + 0.2, y0 + 1.1), (half - 0.05, y0 + 1.1)],
                 top=False, bottom=False)
    square_stack(m, "trim", [(half - 0.05, y1 - 1.1), (half + 0.2, y1 - 1.1), (half + 0.2, y1)],
                 top=False, bottom=False)
    # the cornice, its four urns, the stepped dome and the bud
    square_stack(m, "body", [(half, y1), (half + 1.1, y1 + 0.4), (half + 1.1, y1 + 1.0),
                             (half + 0.3, y1 + 1.0)], top=False, bottom=False)
    for sx in (1.0, -1.0):
        for sz in (1.0, -1.0):
            lathe(m, "trim", [(0.35, y1 + 1.0), (0.7, y1 + 1.5), (0.55, y1 + 2.1), (0.2, y1 + 2.4),
                              (0.0, y1 + 3.0)], sides=5, cx=AXIS + sx * (half + 0.5),
                  cz=AXIS + sz * (half + 0.5))
    square_stack(m, "body", [(half + 0.3, y1 + 1.0), (half - 0.5, y1 + 1.9), (half - 0.5, y1 + 2.3),
                             (half - 1.3, y1 + 3.2), (half - 1.3, y1 + 3.6), (1.0, y1 + 4.4)],
                 top=False, bottom=False)
    lathe(m, "trim", [(1.4, y1 + 4.4), (0.8, y1 + 4.9), (1.2, y1 + 5.6), (0.9, y1 + 6.4),
                      (0.3, y1 + 7.0), (0.0, y1 + 8.0)], sides=8, bottom=True)


def build_um1906(m):
    """Union Metal UM-1906, the twin-lamp electrolier of Figueroa Street (mid-1920s; at Adams, at
    Washington, and on North Figueroa in Highland Park): two short scrolled bracket arms across the
    post, an upright opal acorn under a small cap on each, and a finial between them."""
    fitter(m, 4.6, 2.4, 2.4)
    lathe(m, "body", [(2.4, 2.4), (2.2, 6.4), (2.8, 6.8), (2.8, 7.6), (1.5, 8.2), (1.1, 11.0),
                      (1.7, 11.5), (1.0, 12.4), (0.5, 13.6), (0.0, 16.0)], sides=10)
    for sign in (1, -1):
        cz = 8.0 - sign * 7.4
        arm = bezier((8.0, 7.0, 8.0), (8.0, 7.9, 8.0 - sign * 3.0), (8.0, 7.6, 8.0 - sign * 6.4),
                     (8.0, 6.7, cz), 6)
        tube(m, "body", arm, 0.6, sides=5, cap0=False)
        # the scroll under the arm, from the post out and curling up into it
        tube(m, "body", [(8.0, 3.6, 8.0 - sign * 2.2), (8.0, 3.4, 8.0 - sign * 4.0),
                         (8.0, 4.2, 8.0 - sign * 5.6), (8.0, 5.6, 8.0 - sign * 6.0),
                         (8.0, 6.6, 8.0 - sign * 5.0)], 0.38, sides=4)
        lathe(m, "body", [(0.6, 6.4), (1.7, 7.0), (2.0, 7.9), (1.6, 8.1)], sides=10, cz=cz,
              bottom=True)
        lathe(m, "unshaded_lens", [(1.6, 8.1), (2.4, 9.4), (3.1, 11.4), (3.3, 13.2), (3.0, 14.4)],
              sides=10, cz=cz)
        lathe(m, "body", [(3.0, 14.4), (3.3, 14.5), (3.2, 15.0), (2.0, 15.7), (0.7, 16.1),
                          (0.8, 16.6), (0.0, 17.6)], sides=10, cz=cz)


def build_cal_heights(m):
    """The California Heights lamp (Long Beach, 1929; the blue posts south of Bixby Road): an
    upright frosted acorn in a cage of slim ribs, a pierced band round its widest point, a low
    ogee roof and a spike. Only the post is blue: cage, band, roof and collar are aluminium."""
    fitter(m, 4.2, 2.2, 1.9)
    lathe(m, "body", [(1.9, 2.2), (2.3, 2.8), (2.3, 3.3), (1.9, 3.5)], sides=12)
    globe = [(1.9, 3.5), (2.5, 4.8), (3.3, 7.0), (3.9, 9.4), (4.0, 10.8)]
    lathe(m, "unshaded_lens", globe, sides=12)
    ribs = 8
    for i in range(ribs):
        a = 2.0 * math.pi * i / ribs
        pts = [(AXIS + (r + 0.22) * math.cos(a), y, AXIS + (r + 0.22) * math.sin(a))
               for r, y in globe]
        tube(m, "body", pts, 0.2, sides=3, cap0=False, cap1=False)
    lathe(m, "trim", [(4.0, 10.8), (4.4, 10.8), (4.4, 12.0), (4.0, 12.0)], sides=12)
    lathe(m, "body", [(4.0, 12.0), (4.6, 12.1), (4.5, 12.6), (3.4, 13.2), (2.0, 13.6), (1.2, 14.4),
                      (0.5, 14.8), (0.6, 15.3), (0.25, 15.6), (0.0, 17.4)], sides=12)


def build_metropolitan(m):
    fitter(m, 4.6, 2.6, 2.6)
    lathe(m, "body", [(2.6, 2.6), (2.4, 4.0), (2.9, 4.4), (2.9, 4.9)], sides=12)
    octagon_lantern(m, 8, 4.9, 9.0, 17.6, 2.6, 3.6, 5.0, ("unshaded_lens", 1.8, 21.2), horns=True,
                    spire=[(1.0, 22.6), (1.3, 23.2), (0.6, 23.8), (0.4, 25.4), (0.0, 29.0)])


def build_westwood(m):
    fitter(m, 4.6, 2.6, 2.7)
    lathe(m, "body", [(2.7, 2.6), (2.5, 4.2), (3.0, 4.6), (3.0, 5.1)], sides=12)
    octagon_lantern(m, 6, 5.1, 7.6, 18.0, 2.8, 4.4, 5.6, ("unshaded_lens", 1.6, 21.6),
                    spire=[(0.9, 23.0), (1.2, 23.5), (0.0, 24.4)])


# --- globes --------------------------------------------------------------------------------
def build_ge104(m):
    """GE #104 globe with brass band (frame 09): a tall opal ovoid in a cast holder, a pierced
    brass band round its shoulder."""
    fitter(m, 4.6, 2.2, 3.4)
    lathe(m, "body", [(3.4, 2.2), (4.4, 2.8), (4.4, 4.4), (4.0, 4.6)], sides=12)
    lathe(m, "body", [(4.0, 4.6), (2.19, 4.6)], sides=12)
    lathe(m, "unshaded_lens", ellipsoid_profile(13.5, 4.8, 10.0, 10, y_from=4.6), sides=12)
    lathe(m, "trim", [(4.2, 17.6), (4.5, 17.8), (4.4, 19.4), (4.0, 19.6), (3.6, 19.6)], sides=12)
    for i in range(4):
        a = 2.0 * math.pi * (i + 0.5) / 4
        blob(m, "trim", (AXIS + 4.4 * math.cos(a), 4.0, AXIS + 4.4 * math.sin(a)), 0.45)


def build_ge198(m):
    """GE #198 globe (frame 13): a broad frosted globe, flat-shouldered, in a bronze holder."""
    fitter(m, 4.8, 2.0, 3.2)
    lathe(m, "trim", [(3.2, 2.0), (5.4, 2.6), (5.6, 3.2), (5.6, 5.0), (5.2, 5.2)], sides=12)
    lathe(m, "trim", [(5.2, 5.2), (4.6, 5.2)], sides=12)
    lathe(m, "unshaded_lens", [(4.6, 5.2), (5.4, 6.6), (6.4, 9.0), (6.8, 11.0), (6.6, 13.0),
                               (5.8, 15.0), (4.4, 16.6), (2.4, 17.6), (0.0, 18.0)], sides=12)
    for i in range(4):
        a = 2.0 * math.pi * (i + 0.5) / 4
        blob(m, "trim", (AXIS + 5.7 * math.cos(a), 4.6, AXIS + 5.7 * math.sin(a)), 0.45)


def build_paragon(m):
    """Westinghouse Paragon Jr. (frame 01): a frosted urn on a short cast base, a small cap."""
    fitter(m, 4.6, 1.8, 3.0)
    lathe(m, "body", [(3.0, 1.8), (2.8, 2.6), (5.2, 3.0), (5.4, 3.6), (5.4, 4.2), (4.6, 4.4)],
          sides=12)
    lathe(m, "body", [(4.6, 4.4), (4.4, 4.4)], sides=12)
    lathe(m, "unshaded_lens", [(4.4, 4.4), (5.8, 7.0), (6.6, 10.0), (6.6, 12.6), (5.8, 15.2),
                               (4.6, 17.0), (3.2, 18.0)], sides=12)
    lathe(m, "body", [(3.2, 18.0), (3.6, 18.2), (3.6, 18.8), (2.2, 19.4), (1.0, 19.8), (1.2, 20.6),
                      (0.0, 21.4)], sides=12)


def build_llewellyn(m):
    """Five-globe Llewellyn electrolier (frame 06): a centre globe raised on its stem and four
    smaller globes on cast arms, each arm over a scrolled bracket."""
    fitter(m, 4.6, 2.4, 2.4)
    lathe(m, "body", [(2.4, 2.4), (2.2, 6.0), (3.0, 6.4), (3.0, 7.2), (1.6, 7.8), (1.4, 10.0),
                      (2.4, 10.4), (2.6, 11.2), (2.2, 11.4), (1.85, 11.4)], sides=12)
    lathe(m, "unshaded_lens", ellipsoid_profile(15.6, 4.6, 4.6, 8, y_from=11.4), sides=12)
    reach = 8.6
    for i in range(4):
        a = math.pi / 4.0 + math.pi / 2.0 * i
        ca, sa = math.cos(a), math.sin(a)
        p = lambda r, y: (AXIS + r * ca, y, AXIS + r * sa)
        tube(m, "body", [p(1.8, 6.6), p(4.0, 6.9), p(6.4, 6.6), p(reach - 0.4, 6.2)], 0.6, sides=5)
        tube(m, "body", spiral_bracket(p, 2.0, 4.0, 6.4), 0.35, sides=3)
        cx, cz = AXIS + reach * ca, AXIS + reach * sa
        lathe(m, "body", [(0.6, 5.4), (1.4, 6.0), (1.6, 7.2), (1.2, 7.4)], sides=6, cx=cx, cz=cz,
              bottom=True)
        lathe(m, "unshaded_lens", ellipsoid_profile(10.6, 3.4, 3.4, 5, y_from=7.4), sides=8,
              cx=cx, cz=cz)


def spiral_bracket(p, r_in, r_out, y_top):
    """A scroll under an arm: from the stem down and out, curling up into the arm."""
    pts = []
    for k in range(7):
        t = k / 6.0
        r = r_in + (r_out - r_in) * t
        y = y_top - 2.2 * math.sin(math.pi * t) - 0.2
        pts.append(p(r, y))
    return pts


# --- multi-lamp and hung heads ------------------------------------------------------------
def teardrop(m, cx, cz, y_top, scale=1.0, skirt=True):
    """A pendant teardrop hung at (cx, cz) from y_top: a hood (or a bell skirt round it, the
    Genie's), a prismatic teardrop, a finial drop."""
    s = scale
    if skirt:
        yg = y_top - 1.8 * s
        # the bell's outside, rim up to the hanger, and its inside, from the glass down to the rim
        lathe(m, "body", [(4.6 * s, y_top - 2.9 * s), (4.4 * s, y_top - 2.4 * s),
                          (1.2 * s, y_top - 0.5 * s), (0.6 * s, y_top)],
              sides=10, cx=cx, cz=cz, top=True)
        lathe(m, "body", [(2.2 * s, yg), (4.0 * s, y_top - 2.9 * s), (4.6 * s, y_top - 2.9 * s)],
              sides=10, cx=cx, cz=cz)
    else:
        yg = y_top - 1.6 * s
        lathe(m, "body", [(2.2 * s, yg), (2.6 * s, yg), (2.6 * s, y_top - 1.0 * s), (0.6 * s, y_top)],
              sides=10, cx=cx, cz=cz, top=True)
    prof = [(2.2 * s, yg), (3.0 * s, yg - 1.4 * s), (3.1 * s, yg - 3.2 * s), (2.4 * s, yg - 5.0 * s),
            (1.2 * s, yg - 6.4 * s), (0.4 * s, yg - 7.0 * s)]
    lathe(m, "unshaded_lens", list(reversed([(r, y) for r, y in prof])), sides=10, cx=cx, cz=cz,
          top=False)
    disc(m, "unshaded_lens", 2.2 * s, yg, (0.0, 1.0, 0.0), 10, cx, cz)
    lathe(m, "body", [(0.0, yg - 8.0 * s), (0.4 * s, yg - 7.4 * s), (0.45 * s, yg - 6.9 * s)],
          sides=6, cx=cx, cz=cz, top=True)


def scroll_arm(m, sign, reach, y_root, y_peak, mat="body"):
    """A Genie scroll arm in the z = 8 +- reach plane: up from the stem, over, ending at a hanger
    at z = 8 - sign*reach, with a scroll in the elbow."""
    z0 = AXIS
    zt = AXIS - sign * reach
    pts = bezier((8.0, y_root, z0), (8.0, y_peak + 1.0, z0 - sign * 0.5),
                 (8.0, y_peak + 1.4, zt + sign * 2.0), (8.0, y_peak - 0.6, zt), 8)
    tube(m, mat, pts, 0.7, sides=5)
    sc = (8.0, y_root + 2.4, AXIS - sign * reach * 0.45)
    sp = spiral_pts(sc, 2.4, 0.9, 90.0 if sign > 0 else 90.0, 90.0 + sign * 540.0, 10)
    # flip the scroll's sense so it curls under the arm toward the pole
    sp = [(p[0], p[1], 2 * sc[2] - p[2]) for p in sp]
    tube(m, mat, sp, 0.35, sides=4)
    return zt, y_peak - 0.6


def build_genie(m):
    fitter(m, 4.6, 2.4, 2.2)
    lathe(m, "body", [(2.2, 2.4), (2.0, 9.0), (2.6, 9.4), (2.6, 10.0), (1.6, 10.6)], sides=12)
    zt, yt = scroll_arm(m, 1, 9.0, 9.6, 14.0)
    tube(m, "body", [(8.0, yt, zt), (8.0, yt - 1.2, zt)], 0.3, sides=4)
    teardrop(m, 8.0, zt, yt - 1.2, 0.9)
    lathe(m, "body", [(1.6, 10.6), (1.0, 13.4), (1.4, 13.8), (0.0, 15.6)], sides=8)


def build_vine_double(m):
    fitter(m, 4.6, 2.4, 2.2)
    lathe(m, "body", [(2.2, 2.4), (2.0, 9.0), (2.6, 9.4), (2.6, 10.0), (1.6, 10.6)], sides=12)
    for sign in (1, -1):
        zt, yt = scroll_arm(m, sign, 9.0, 9.6, 14.0)
        tube(m, "body", [(8.0, yt, zt), (8.0, yt - 1.2, zt)], 0.3, sides=4)
        teardrop(m, 8.0, zt, yt - 1.2, 0.9)
    lathe(m, "body", [(1.6, 10.6), (1.0, 14.0), (1.5, 14.4), (0.5, 16.0), (0.0, 18.0)], sides=8)


def drum(m, cx, cz, y0, r, h, cap=True):
    """A cylindrical lamp: a cast cup, a glass drum, a flat cap."""
    lathe(m, "body", [(0.8, y0), (r + 0.3, y0 + 0.8), (r + 0.3, y0 + 1.4), (r, y0 + 1.4)],
          sides=10, cx=cx, cz=cz, bottom=True)
    lathe(m, "unshaded_lens", [(r, y0 + 1.4), (r, y0 + 1.4 + h)], sides=10, cx=cx, cz=cz)
    if cap:
        lathe(m, "body", [(r, y0 + 1.4 + h), (r + 0.5, y0 + 1.6 + h), (r + 0.5, y0 + 2.2 + h),
                          (r * 0.5, y0 + 2.8 + h), (0.0, y0 + 3.0 + h)], sides=10, cx=cx, cz=cz)


def flat_star(m, mat, centre, r_out, r_in, facing, points=5):
    """A star decal standing a hair off a wall, facing ``facing`` (horizontal): its front only."""
    f = vnorm(facing)
    side = vnorm(vcross((0.0, 1.0, 0.0), f))
    outline = []
    for k in range(points * 2):
        a = math.pi / 2.0 + math.pi * k / points
        r = r_out if k % 2 == 0 else r_in
        outline.append(vadd(centre, vadd(vmul(side, r * math.cos(a)), (0.0, r * math.sin(a), 0.0))))
    fan(m, mat, centre, outline, f)


# The five-star head reaches a full block ahead of its pole: three lamps in a row do not fit in
# the half block the other heads keep to.
HOLLYWOOD_STAR_REACH = -15.8


def build_hollywood_star(m):
    """1960s Hollywood Special, from the requester's photos of a surviving head and of one in
    service at Sunset Boulevard: a square black mast with a square-cut elbow, and a long grey box
    head reaching over the street from it, three prismatic bowl refractors in a row under the box
    and five red stars along each of its sides."""
    fitter(m, 4.6, 2.4, 2.3, mat="frame")
    box(m, "frame", (6.4, 2.3, 6.4), (9.6, 14.7, 9.6))            # the mast
    box(m, "frame", (6.55, 11.6, 3.2), (9.45, 14.55, 8.0))        # the arm, into the head's end
    z0, z1 = HOLLYWOOD_STAR_REACH, 3.6
    box(m, "body", (5.0, 11.2, z0), (11.0, 14.9, z1))             # the head
    box(m, "body", (4.7, 10.9, z0 - 0.0 + 0.3), (11.3, 11.5, z1 - 0.3))   # the door frame's lip
    pitch = (z1 - z0 - 1.0) / 3.0
    for k in range(3):
        cz = z0 + 0.5 + pitch * (k + 0.5)
        lathe(m, "unshaded_lens", ellipsoid_profile(10.9, 2.6, 2.4, 5, y_to=10.9), sides=10, cz=cz)
    pitch = (z1 - z0) / 5.0
    for k in range(5):
        cz = z0 + pitch * (k + 0.5)
        for sign in (1.0, -1.0):
            flat_star(m, "trim", (8.0 + sign * 3.12, 13.2, cz), 1.35, 0.55, (sign, 0.0, 0.0))


def build_hollywood_wof(m):
    """2005 Hollywood Walk of Fame 'pitchfork': the stem parts into three tines, each topped by a
    lantern."""
    fitter(m, 4.6, 2.4, 2.2)
    lathe(m, "body", [(2.2, 2.4), (2.0, 7.0), (2.6, 7.4), (2.6, 8.0), (1.4, 8.4)], sides=12)
    tube(m, "body", [(8.0, 8.0, 8.0), (8.0, 15.0, 8.0)], 1.0, sides=8, cap0=False, cap1=False)
    for sign in (1, -1):
        pts = bezier((8.0, 8.6, 8.0), (8.0, 8.8, 8.0 - sign * 5.0), (8.0, 10.0, 8.0 - sign * 7.4),
                     (8.0, 15.0, 8.0 - sign * 7.4), 8)
        tube(m, "body", pts, 0.8, sides=6, cap0=False, cap1=False)
        lantern_cyl(m, 8.0 - sign * 7.4, 15.0)
    lantern_cyl(m, 8.0, 15.0)


def lantern_cyl(m, cz, y0):
    lathe(m, "body", [(1.0, y0), (2.4, y0 + 0.8), (2.4, y0 + 1.4), (2.0, y0 + 1.4)], sides=8,
          cz=cz, bottom=True, phase=0.0)
    lathe(m, "unshaded_lens", [(2.0, y0 + 1.4), (2.2, y0 + 6.4)], sides=8, cz=cz, phase=0.0)
    for i in range(0, 8, 2):
        a = 2.0 * math.pi * i / 8
        beam(m, "body", (AXIS + 2.15 * math.cos(a), y0 + 1.4, cz + 2.15 * math.sin(a)),
             (AXIS + 2.35 * math.cos(a), y0 + 6.4, cz + 2.35 * math.sin(a)), 0.35)
    lathe(m, "body", [(2.2, y0 + 6.4), (2.8, y0 + 6.6), (2.8, y0 + 7.2), (1.0, y0 + 8.4),
                      (0.3, y0 + 9.2), (0.0, y0 + 10.2)], sides=8, cz=cz, phase=0.0)


def build_broadway_rose_head(m):
    """Broadway Rose's two-lamp head: two cast arms swept out and up from the capital, each under
    an opal globe, a spear finial between them, a rose at each elbow."""
    fitter(m, 4.6, 2.4, 2.4)
    lathe(m, "body", [(2.4, 2.4), (2.2, 6.2), (2.8, 6.6), (2.8, 7.2), (1.4, 7.8), (1.0, 12.0),
                      (1.6, 12.4), (0.8, 13.4), (0.0, 17.0)], sides=8)
    for sign in (1, -1):
        pts = bezier((8.0, 6.6, 8.0), (8.0, 6.0, 8.0 - sign * 4.0), (8.0, 6.0, 8.0 - sign * 7.6),
                     (8.0, 9.0, 8.0 - sign * 7.6), 8)
        tube(m, "body", pts, 0.65, sides=6, cap0=False)
        blob(m, "trim", (8.0, 6.6, 8.0 - sign * 4.4), 0.9)
        cz = 8.0 - sign * 7.6
        lathe(m, "body", [(0.6, 8.6), (1.8, 9.2), (2.0, 10.2), (1.7, 10.4)], sides=10, cz=cz,
              bottom=True)
        lathe(m, "unshaded_lens", ellipsoid_profile(14.0, 3.6, 4.2, 8, y_from=10.4), sides=12,
              cz=cz)


# --- reproductions -------------------------------------------------------------------------
def build_acorn(m):
    """A reproduction acorn: a ribbed prismatic acorn on a cast fitter, a crown and finial."""
    fitter(m, 4.6, 2.4, 3.0)
    lathe(m, "body", [(3.0, 2.4), (4.6, 3.6), (4.8, 4.4), (4.2, 4.6)], sides=12)
    lathe(m, "body", [(4.2, 4.6), (4.0, 4.6)], sides=12)
    lathe(m, "unshaded_lens", [(4.0, 4.6), (5.2, 7.0), (5.6, 10.0), (5.4, 12.6), (4.6, 15.0)],
          sides=12)
    lathe(m, "body", [(4.6, 15.0), (5.0, 15.2), (5.0, 16.0), (3.6, 17.2), (1.6, 18.0), (1.0, 18.6),
                      (1.4, 19.2), (0.6, 20.0), (0.0, 21.6)], sides=12)


def build_washington(m):
    """Washington Globe: an opal globe narrowing to a small cast top, on a leafy fitter."""
    fitter(m, 4.6, 2.2, 3.0)
    lathe(m, "body", [(3.0, 2.2), (3.6, 3.0), (4.4, 3.4), (4.4, 4.2), (3.8, 4.4)], sides=12)
    for i in range(8):
        a = 2.0 * math.pi * (i + 0.5) / 8
        blob(m, "body", (AXIS + 3.6 * math.cos(a), 3.8, AXIS + 3.6 * math.sin(a)), 0.8)
    lathe(m, "body", [(3.8, 4.4), (3.6, 4.4)], sides=12)
    lathe(m, "unshaded_lens", [(3.6, 4.4), (5.2, 6.0), (6.2, 8.6), (6.4, 11.0), (5.8, 13.6),
                               (4.2, 15.8), (2.6, 16.8)], sides=12)
    lathe(m, "body", [(2.6, 16.8), (2.9, 17.0), (2.9, 17.6), (1.6, 18.4), (0.9, 18.8),
                      (1.1, 19.4), (0.0, 20.2)], sides=12)


def wash_globe_small(m, cz, y0):
    lathe(m, "body", [(0.8, y0), (2.4, y0 + 0.8), (2.6, y0 + 1.6), (2.2, y0 + 1.8)], sides=12,
          cz=cz, bottom=True)
    lathe(m, "unshaded_lens", [(2.2, y0 + 1.8), (3.4, y0 + 3.2), (4.0, y0 + 5.4), (3.6, y0 + 7.4),
                               (2.4, y0 + 8.8), (1.4, y0 + 9.4)], sides=12, cz=cz)
    lathe(m, "body", [(1.4, y0 + 9.4), (1.6, y0 + 9.6), (1.0, y0 + 10.2), (0.0, y0 + 11.0)],
          sides=8, cz=cz)


def build_twin20(m):
    """Twin-20: a cast T-bar across the street with a Washington globe at each end, a finial
    between them."""
    fitter(m, 4.6, 2.4, 2.4)
    lathe(m, "body", [(2.4, 2.4), (2.2, 6.0), (2.8, 6.4), (2.8, 7.4), (1.2, 8.0), (0.9, 10.6),
                      (1.4, 11.0), (0.0, 13.0)], sides=8)
    tube(m, "body", [(8.0, 7.0, 0.4), (8.0, 7.0, 15.6)], 0.75, sides=8)
    for sign in (1, -1):
        cz = 8.0 - sign * 7.6
        tube(m, "body", [(8.0, 6.4, 8.0 - sign * 2.4), (8.0, 4.6, 8.0 - sign * 4.6),
                         (8.0, 5.6, 8.0 - sign * 6.6), (8.0, 6.6, cz)], 0.4, sides=4)
        wash_globe_small(m, cz, 7.2)


def build_bishops_crook(m):
    """Bishop's crook: the post's tip rises and bends over in a crook, a scroll inside its curve,
    a teardrop hung from its end."""
    fitter(m, 4.0, 2.4, 0.9)
    rc = 5.5
    straight = [(8.0, 2.0, 8.0), (8.0, 18.0, 8.0)]
    crook = arc_pts((8.0, 18.0, 8.0 - rc), rc, 0.0, 180.0, 10)
    pts = straight + crook[1:]
    tube(m, "body", pts, 0.9, sides=8, cap0=False)
    sc = spiral_pts((8.0, 17.0, 8.0 - rc), 2.6, 0.9, 0.0, -400.0, 12)
    tube(m, "body", sc, 0.35, sides=4)
    zt = 8.0 - 2.0 * rc
    teardrop(m, 8.0, zt, 18.0, 0.85, skirt=False)


def build_type_f(m):
    """NYC Type F (the 1913 Reverse Scroll Bracket): a scroll arm whose curl turns back under
    itself toward the post, carrying a pendant from its tip."""
    fitter(m, 4.6, 2.4, 2.4)
    lathe(m, "body", [(2.4, 2.4), (2.2, 11.0), (2.8, 11.4), (2.8, 12.2), (1.6, 12.6),
                      (1.2, 14.0), (0.0, 15.0)], sides=12)
    arm = bezier((8.0, 12.0, 8.0), (8.0, 13.6, 4.0), (8.0, 13.6, -1.4), (8.0, 12.6, -4.0), 10)
    tube(m, "body", arm, 0.75, sides=6)
    sc = spiral_pts((8.0, 9.4, 1.6), 3.0, 1.1, 90.0, 400.0, 14)
    tube(m, "body", [(8.0, 11.2, 7.0)] + sc, 0.45, sides=4)
    teardrop(m, 8.0, -4.0, 12.4, 0.85)


def along_street(builder):
    """A symmetric head is drawn with its arms along z, then turned so they run along x: across
    the block's facing, parallel to the kerb of a street it faces."""
    def build(m):
        with m.frame(rot_y(90.0)):
            builder(m)
    return build


# The order is the creative order.
FIXTURES = [
    Fixture("lawilshirespecial", "Wilshire Special Lantern", build_wilshire, "frosted",
            source="BSL: solid bronze, 1928, four-sided, a figure at each corner; from photos of "
                   "the survivors on Wilshire Boulevard by MacArthur Park"),
    Fixture("lametropolitan", "Metropolitan Lantern", build_metropolitan, "frosted",
            source="the spear-tipped octagonal lantern in frame 12 (centre), horns at the cornice"),
    Fixture("lawestwoodlantern", "Westwood Village Lantern", build_westwood, "frosted",
            source="the hexagonal lantern in frame 12 (left)"),
    Fixture("labroadwayrosehead", "Broadway Rose Twin Globes", along_street(build_broadway_rose_head), "opal",
            source="BSL: a two-lamp electrolier garlanded with roses"),
    Fixture("laum1906twin", "UM-1906 Twin Electrolier (Figueroa Street)", along_street(build_um1906),
            "opal", source="Union Metal UM-1906, as on Figueroa Street (the requester's 'Figueroa "
                           "Special'; waterandpower.org, Street View of Highland Park)"),
    Fixture("lage104globe", "GE No. 104 Globe with Brass Band", build_ge104, "opal",
            source="museum frame 09"),
    Fixture("lage198globe", "GE No. 198 Globe", build_ge198, "frosted", source="museum frame 13"),
    Fixture("laparagonjr", "Westinghouse Paragon Jr.", build_paragon, "frosted",
            source="museum frame 01"),
    Fixture("lallewellyn5", "Five-Globe Llewellyn Electrolier", build_llewellyn, "opal",
            source="museum frame 06 (Downtown Los Angeles, c. 1900)"),
    Fixture("lagenie", "Genie Scroll Arm Teardrop", build_genie, "prism", aimed=True,
            source="BSL: post-war scroll ('Genie') arm with a skirted teardrop"),
    Fixture("lavinedouble", "Vine Double Teardrops", along_street(build_vine_double), "prism",
            source="BSL: the two-arm Genie, as on Vine Street and Hollywood Boulevard"),
    Fixture("lahollywoodstar", "Hollywood Special Five-Star Head", build_hollywood_star, "prism",
            aimed=True, reach=HOLLYWOOD_STAR_REACH - 0.5,
            source="BSL: c. 1960 'Five-Star' three-lamp head, from photos of a surviving head"),
    Fixture("lahollywoodwof", "Hollywood Walk of Fame Three-Lamp Head", along_street(build_hollywood_wof),
            "frosted", source="the 2005 Walk of Fame 'pitchfork' (a reconstruction; see the plan)"),
    Fixture("lbcalheightslantern", "California Heights Caged Acorn Lantern", build_cal_heights,
            "opal", source="Long Beach's California Heights lamps, 1929 (Olive Avenue)"),
    Fixture("reproacorn", "Reproduction Acorn", build_acorn, "prism",
            source="the cast-post acorn of Union Metal / Spring City reproductions"),
    Fixture("reprowashingtonglobe", "Washington Globe", build_washington, "opal",
            source="Washington, DC's Washington Globe"),
    Fixture("reprotwin20", "Twin-20 Washington Globes", along_street(build_twin20), "opal",
            source="Washington, DC's Twin-20"),
    Fixture("reprobishopscrook", "Bishop's Crook with Teardrop", build_bishops_crook, "prism",
            aimed=True, source="New York's Bishop's Crook"),
    Fixture("reprotypef", "NYC Type F Reverse Scroll", build_type_f, "prism", aimed=True,
            source="NYC DOT Type F, the 1913 Reverse Scroll Bracket"),
]

FIXTURE_PAINT = {
    "lawilshirespecial": ("trim_bronze", "trim_brass"),
    "lametropolitan": ("paint_la_bronze", "trim_bronze"),
    "lawestwoodlantern": ("paint_la_bronze", "trim_bronze"),
    "labroadwayrosehead": ("paint_la_green", "trim_bronze"),
    "laum1906twin": ("paint_la_bronze", "trim_bronze"),
    "lage104globe": ("paint_black", "trim_brass"),
    "lage198globe": ("paint_la_bronze", "trim_bronze"),
    "laparagonjr": ("paint_la_green", "trim_bronze"),
    "lallewellyn5": ("paint_black", "trim_bronze"),
    "lagenie": ("paint_la_green", "trim_bronze"),
    "lavinedouble": ("paint_la_green", "trim_bronze"),
    "lahollywoodstar": ("paint_hollywood_grey", "trim_star_red", "paint_black"),
    "lahollywoodwof": ("paint_hollywood_grey", "trim_gold"),
    "lbcalheightslantern": ("paint_aluminium", "paint_aluminium"),
    "reproacorn": ("paint_black", "trim_gold"),
    "reprowashingtonglobe": ("paint_black", "trim_gold"),
    "reprotwin20": ("paint_black", "trim_gold"),
    "reprobishopscrook": ("paint_black", "trim_gold"),
    "reprotypef": ("paint_black", "trim_gold"),
}


def fixture_mesh(f):
    m = Mesh()
    f.build(m)
    return m


def inventory_mesh(m):
    """The whole fixture shrunk to fit its block, for the inventory icon."""
    lo, hi = m.bounds()
    span = max(16.0, max(hi[k] - lo[k] for k in range(3)))
    if span <= 16.0 and min(lo) >= 0.0 and max(hi) <= 16.0:
        return m
    k = 15.6 / span
    cx, cz = (lo[0] + hi[0]) / 2.0, (lo[2] + hi[2]) / 2.0
    xf = Xf(((k, 0.0, 0.0), (0.0, k, 0.0), (0.0, 0.0, k)),
            (8.0 - k * cx, 0.2 - k * lo[1], 8.0 - k * cz))
    return m.transformed(xf)


def fixture_blockstate(f):
    model = model_ref(LIGHT_PREFIX, "decolight_" + f.fid)
    paint = FIXTURE_PAINT[f.fid]
    body, trim = paint[0], paint[1]
    off, on = tex("glass_%s_off" % f.glass), tex("glass_%s_on" % f.glass)
    textures = {"#body": tex(body), "#trim": tex(trim), "#unshaded_lens": off,
                "particle": tex(body)}
    if len(paint) > 2:
        textures["#frame"] = tex(paint[2])
    return {
        "forge_marker": 1,
        "defaults": {"model": model, "custom": {"flip-v": True}, "textures": textures},
        "variants": {
            "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}},
            "state": {str(s): {"textures": {"#unshaded_lens": on if s in (1, 3) else off}}
                      for s in range(4)},
            "inventory": [{"model": model_ref(LIGHT_PREFIX, "decolight_%s_inv" % f.fid),
                           "custom": {"flip-v": True}, "textures": textures,
                           "transform": "forge:default-block"}],
        },
    }


def fixture_box(m):
    lo, hi = m.bounds()
    return [round(v / 16.0, 6) for v in lo + hi]


# =============================================================================== outputs
def mtl_text(materials, default):
    out = "# Generated by dev-env-utils/scripts/gen_decorative_poles.py -- do not edit\n"
    out += "# Every blockstate retextures these.\n"
    for name in materials:
        out += "newmtl %s\nmap_Kd %s\n" % (name, default)
    return out


def png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def outputs():
    """{(owner, tree-relative path): bytes}, plus the stats per design."""
    files = {}
    stats = []
    files[(ROADS, POLE_MODEL_REL + "/" + POLE_MTL)] = mtl_text(
        ["body", "tile", "trim"], tex("paint_black")).encode()
    files[(LIGHTING, LIGHT_MODEL_REL + "/" + LIGHT_MTL)] = mtl_text(
        ["body", "trim", "frame", "unshaded_lens"], tex("paint_black")).encode()
    for d in POLE_DESIGNS:
        parts = pole_parts(d)
        for part, (mesh, header) in parts.items():
            lo, hi = mesh.bounds()
            if part not in ("inv", "cap_n", "cap_s"):
                assert all(-0.05 <= c <= 16.05 for c in lo + hi), \
                    (d.pid, part, lo, hi)
            name = "decopole_%s_%s" % (d.pid, part)
            files[(ROADS, POLE_MODEL_REL + "/" + name + ".obj")] = mesh.text(
                name, POLE_MTL, "%s: %s" % (d.label, header)).encode()
        files[(ROADS, "blockstates/%s.json" % pole_registry(d))] = (
            json.dumps(pole_blockstate(d), indent=2) + "\n").encode()
        stats.append(("pole", pole_registry(d), d.label,
                      {p: parts[p][0].tris() for p in ("shaft", "base_s", "cap_n", "tenon_n")}))
    for f in FIXTURES:
        mesh = fixture_mesh(f)
        lo, hi = mesh.bounds()
        assert lo[1] >= 0.0, (f.fid, lo)
        assert -8.5 <= lo[0] and hi[0] <= 24.5 and f.reach <= lo[2] and hi[2] <= 24.5, (f.fid, lo, hi)
        name = "decolight_" + f.fid
        files[(LIGHTING, LIGHT_MODEL_REL + "/" + name + ".obj")] = mesh.text(
            name, LIGHT_MTL, "%s (after %s)" % (f.label, f.source)).encode()
        files[(LIGHTING, LIGHT_MODEL_REL + "/" + name + "_inv.obj")] = inventory_mesh(mesh).text(
            name + "_inv", LIGHT_MTL, "%s, inventory icon" % f.label).encode()
        files[(LIGHTING, "blockstates/%s.json" % f.fid)] = (
            json.dumps(fixture_blockstate(f), indent=2) + "\n").encode()
        stats.append(("fixture", f.fid, f.label, {"tris": mesh.tris(), "box": fixture_box(mesh)}))
    for name, make in TEXTURES.items():
        files[(CORE, TEX_REL + "/" + name + ".png")] = png_bytes(make())
    return files, stats


def path_of(owner, rel):
    return layout.asset_for_write(owner, rel)


def same(path, data):
    if not os.path.exists(path):
        return False
    old = open(path, "rb").read()
    if path.endswith(".png"):
        a = Image.open(path).convert("RGBA")
        b = Image.open(io.BytesIO(data)).convert("RGBA")
        return a.size == b.size and a.tobytes() == b.tobytes()
    return old.replace(b"\r\n", b"\n") == data


def fragments(stats):
    print("--- Roads tab (CsmTabTrafficAccessories) ---")
    for d in POLE_DESIGNS:
        print('    initTabBlock(new BlockTrafficPoleDecorative("%s", %.1f, %.1f, %.1f, '
              'TRAFFIC_POLE_COLOR.%s));' % (pole_registry(d), d.fit_radius, d.shaft_r,
                                            d.base_half, d.color))
    print("--- Lighting tab (CsmTabLighting) ---")
    for kind, rid, _label, st in stats:
        if kind != "fixture":
            continue
        f = next(x for x in FIXTURES if x.fid == rid)
        b = st["box"]
        cls = "PostTopAimed" if f.aimed else "PostTop"
        print('    initTabBlock(new BlockBrightLightFactory.%s("%s", new AxisAlignedBB(%.6f, %.6f, '
              '%.6f, %.6f, %.6f, %.6f), 1));' % ((cls, rid) + tuple(max(-0.5, min(1.5, v)) for v in b)))
    print("--- en_us ---")
    for d in POLE_DESIGNS:
        print("tile.%s.name=%s" % (pole_registry(d), d.label))
    for f in FIXTURES:
        print("tile.%s.name=%s" % (f.fid, f.label))


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    ap.add_argument("--stats", action="store_true")
    args = ap.parse_args(argv)
    files, stats = outputs()
    if args.fragments:
        fragments(stats)
        return 0
    if args.stats:
        for row in stats:
            print(row)
        return 0
    drift = []
    for (owner, rel), data in files.items():
        path = path_of(owner, rel)
        if args.check:
            if not same(path, data):
                drift.append(rel)
            continue
        if same(path, data):
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as fh:
            fh.write(data)
    # stale files this script used to own
    owned = {os.path.normcase(os.path.abspath(path_of(o, r))) for o, r in files}
    stale = []
    for owner, rel_dir, prefix in ((ROADS, POLE_MODEL_REL, "decopole_"),
                                   (LIGHTING, LIGHT_MODEL_REL, "decolight_"),
                                   (CORE, TEX_REL, "")):
        d = os.path.dirname(path_of(owner, rel_dir + "/x"))
        if os.path.isdir(d):
            for fn in os.listdir(d):
                p = os.path.normcase(os.path.abspath(os.path.join(d, fn)))
                if fn.startswith(prefix) and p not in owned and (prefix or fn.endswith(".png")):
                    stale.append(p)
    if args.check:
        drift += [os.path.basename(p) + " (stale)" for p in stale]
        if drift:
            print("gen_decorative_poles: %d file(s) out of date:\n  %s" % (len(drift), "\n  ".join(drift)))
            return 1
        print("gen_decorative_poles: up to date")
        return 0
    for p in stale:
        os.remove(p)
    for kind, rid, label, st in stats:
        print("  %-8s %-24s %s" % (kind, rid, st))
    print("wrote %d files" % len(files))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
