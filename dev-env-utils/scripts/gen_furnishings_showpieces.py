#!/usr/bin/env python3
"""The five showpieces of the Furniture and Novelties tabs, as our own models: a 1950s arched
jukebox, a grand piano with its lid on the stick, a longcase (grandfather) clock, an A-frame
porch swing and a decorated Christmas tree.

    python dev-env-utils/scripts/gen_furnishings_showpieces.py           # write the assets
    python dev-env-utils/scripts/gen_furnishings_showpieces.py --check   # exit 1 on drift

Every one of them is round or curved somewhere a JSON element cannot follow -- the jukebox's
arch and its light tubes, the piano's bentside and tail, the tree's tiers -- and most are
bigger than a block, so each is ONE OBJ with ONE material and ONE texture per block. The
texture is a small atlas of swatches drawn here (lacquer, wood grain, brass, the keyboard, the
clock dial...) and every face's UVs point into the swatch it wears, inset half a texel so no
face samples its neighbour.

Conventions (shared with the other OBJ generators here):
  * authored in 1/16 block units, written in block units, with the block's min corner at the
    origin; the model is centred on the block in x and z, so the blockstate's facing rotations
    (which pivot about the block centre) turn it in place;
  * the FRONT faces model north (-z): the NSEWUD placement faces a block toward the player, so
    the player looks at the keyboard, the dial, the record window;
  * OBJ v runs bottom-up and the blockstates set flip-v, so v is written as 1 - row;
  * seen from outside, a north face's texture runs right-to-left in model x (the viewer's right
    is west); ``planar()`` works that out from each face's normal, so swatches are painted as
    they are seen.

Real sizes (1 block = 1 m): the jukebox is 0.9 x 1.5 m, the piano 1.5 m wide and 2 m long with
its keys at 0.72 m and the lid on the long stick, the clock 2.1 m to the finial, the swing frame
2.75 m wide and 2.06 m high with a 1.5 m bench at 0.5 m, and the tree 2 m to its tip with the
star above that. Textures are drawn procedurally from seeded noise; nothing is sampled from any
other model.
"""
import argparse
import io
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

SCRIPT = "gen_furnishings_showpieces.py"
OWNER = "furnishings"
ASSETS = os.path.join(layout.tree_root(OWNER), "src", "main", "resources", "assets", "csm")
MODEL_SUB = "models/block/furniture"
TEX_SUB = "textures/blocks/furniture"
BS_SUB = "blockstates"
MATERIAL = "body"


# ==============================================================================================
# vector helpers
# ==============================================================================================
def add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def mul(a, k):
    return (a[0] * k, a[1] * k, a[2] * k)


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def norm(a):
    ln = math.sqrt(dot(a, a))
    if ln < 1e-12:
        raise ValueError("zero vector")
    return (a[0] / ln, a[1] / ln, a[2] / ln)


def lerp(a, b, t):
    return tuple(x + (y - x) * t for x, y in zip(a, b))


X, Y, Z = (1.0, 0.0, 0.0), (0.0, 1.0, 0.0), (0.0, 0.0, 1.0)


# ==============================================================================================
# texture atlas: one PNG per block, swatches packed on shelves
# ==============================================================================================
class Region:
    def __init__(self, x, y, w, h, size):
        self.x, self.y, self.w, self.h, self.size = x, y, w, h, size

    def uv(self, s, t):
        """(s, t) in 0..1 across the swatch, t down from its top row -> OBJ (u, v)."""
        s = min(max(s, 0.0), 1.0)
        t = min(max(t, 0.0), 1.0)
        u = (self.x + 0.5 + s * (self.w - 1)) / self.size
        v = (self.y + 0.5 + t * (self.h - 1)) / self.size
        return (round(u, 6), round(1.0 - v, 6))


class Atlas:
    PAD = 2

    def __init__(self, size):
        self.size = size
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.cx = self.cy = self.row = 0
        self.regions = {}

    def add(self, name, swatch):
        w, h = swatch.size
        p = self.PAD
        if self.cx + w + 2 * p > self.size:
            self.cx, self.cy, self.row = 0, self.cy + self.row, 0
        if self.cy + h + 2 * p > self.size:
            raise ValueError("atlas full adding %s" % name)
        x, y = self.cx + p, self.cy + p
        # extrude the swatch's edges into the padding so mip levels blend with itself
        big = swatch.resize((w + 2 * p, h + 2 * p), Image.NEAREST)
        self.img.paste(big, (x - p, y - p))
        self.img.paste(swatch, (x, y))
        self.cx += w + 2 * p
        self.row = max(self.row, h + 2 * p)
        self.regions[name] = Region(x, y, w, h, self.size)
        return self.regions[name]

    def __getitem__(self, name):
        return self.regions[name]


# ---- painting ---------------------------------------------------------------------------------
def clamp8(v):
    return max(0, min(255, int(round(v))))


def shade(c, k):
    return tuple(clamp8(x * k) for x in c[:3])


def mix(a, b, t):
    return tuple(clamp8(x + (y - x) * t) for x, y in zip(a[:3], b[:3]))


def flat(w, h, colour, rng, amount=6):
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            d = rng.uniform(-amount, amount)
            px[x, y] = tuple(clamp8(c + d) for c in colour) + (255,)
    return img


def wood(w, h, base, rng, streak=0.22, rings=5.0):
    """Grain running down the swatch (t), which the models lay along each board's length."""
    img = Image.new("RGBA", (w, h))
    px = img.load()
    phase = [rng.uniform(0, 6.28) for _ in range(4)]
    for x in range(w):
        col_k = 1.0 + rng.uniform(-0.05, 0.05)
        for y in range(h):
            wave = math.sin((x + 1.6 * math.sin(y * 0.11 + phase[0])) * rings * 6.28 / w
                            + phase[1])
            k = col_k * (1.0 - streak * 0.5 * (wave + 1.0) * 0.6) + rng.uniform(-0.04, 0.04)
            px[x, y] = shade(base, k) + (255,)
    return img


def metal(w, h, base, rng, highlight=0.35, across=True):
    """Brushed metal with a soft highlight band; ``across`` puts the band across s."""
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            f = (x / max(w - 1, 1)) if across else (y / max(h - 1, 1))
            band = math.exp(-((f - 0.35) / 0.18) ** 2)
            k = 0.78 + highlight * band + rng.uniform(-0.03, 0.03)
            px[x, y] = shade(base, k) + (255,)
    return img


def tube_swatch(w, h, colour, rng, bubbles=False):
    """A lit tube: bright core along its length, darker at the sides (s runs round it)."""
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            f = x / max(w - 1, 1)
            k = 0.72 + 0.45 * math.sin(math.pi * f) ** 2
            c = shade(colour, k)
            px[x, y] = c + (255,)
    if bubbles:
        d = ImageDraw.Draw(img)
        for _ in range(h // 3):
            by = rng.randint(1, h - 2)
            bx = rng.randint(1, w - 2)
            d.point((bx, by), fill=mix(colour, (255, 255, 240), 0.7) + (255,))
    return img


# ==============================================================================================
# mesh
# ==============================================================================================
class Mesh:
    def __init__(self):
        self.v, self.vt, self.vn, self.f = [], [], [], []
        self._vi, self._ti, self._ni = {}, {}, {}

    @staticmethod
    def _add(store, index, tpl):
        k = tuple(round(c, 5) for c in tpl)
        if k not in index:
            store.append(k)
            index[k] = len(store)
        return index[k]

    def tri(self, pts, uvs, nrms, keep=None):
        """Wound to face its normals: Forge culls back faces, so winding is not cosmetic.
        ``keep`` (+1 / -1) instead keeps / reverses the given vertex order outright, for a
        surface whose orientation was settled once for the whole strip."""
        if len(nrms) == 3 and isinstance(nrms[0], tuple):
            want = add(add(nrms[0], nrms[1]), nrms[2])
        else:
            want = nrms
            nrms = [nrms] * 3
        geo = cross(sub(pts[1], pts[0]), sub(pts[2], pts[0]))
        if keep is not None:
            want = mul(geo, keep)
        if dot(geo, geo) < 1e-14:
            return  # degenerate sliver; nothing to draw
        pts, uvs, nrms = list(pts), list(uvs), list(nrms)
        if dot(geo, want) < 0:
            pts.reverse()
            uvs.reverse()
            nrms.reverse()
        self.f.append([(self._add(self.v, self._vi, p),
                        self._add(self.vt, self._ti, t),
                        self._add(self.vn, self._ni, norm(n)))
                       for p, t, n in zip(pts, uvs, nrms)])

    def poly(self, pts, uvs, n):
        """A planar polygon, ear-clipped (it may be concave)."""
        right, up = face_axes(n)
        flat2 = [(dot(p, right), dot(p, up)) for p in pts]
        for a, b, c in earclip(flat2):
            self.tri([pts[a], pts[b], pts[c]], [uvs[a], uvs[b], uvs[c]], n)

    def bounds(self):
        xs = [p[0] for p in self.v]
        ys = [p[1] for p in self.v]
        zs = [p[2] for p in self.v]
        return (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))

    def text(self, name):
        lines = ["# Procedurally generated by dev-env-utils/scripts/%s -- do not hand edit"
                 % SCRIPT, "mtllib %s.mtl" % name, "o %s" % name]
        lines += ["v %.6f %.6f %.6f" % (p[0] / 16.0, p[1] / 16.0, p[2] / 16.0) for p in self.v]
        lines += ["vt %.6f %.6f" % t for t in self.vt]
        lines += ["vn %.5f %.5f %.5f" % n for n in self.vn]
        lines.append("usemtl %s" % MATERIAL)
        lines += ["f " + " ".join("%d/%d/%d" % c for c in tri) for tri in self.f]
        return "\n".join(lines) + "\n"


def face_axes(n):
    """(right, up) as seen by a viewer outside the face; right x up = n."""
    n = norm(n)
    if abs(n[1]) > 0.9:
        up = (0.0, 0.0, -1.0) if n[1] > 0 else (0.0, 0.0, 1.0)
        up = norm(sub(up, mul(n, dot(up, n))))
    else:
        up = norm(sub(Y, mul(n, dot(Y, n))))
    return cross(up, n), up


def earclip(pts):
    """Triangulate a simple polygon given as 2D points; returns index triples."""
    n = len(pts)
    if n < 3:
        return []
    area = sum(pts[i][0] * pts[(i + 1) % n][1] - pts[(i + 1) % n][0] * pts[i][1]
               for i in range(n))
    idx = list(range(n)) if area > 0 else list(range(n))[::-1]

    def cr(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])

    def inside(p, a, b, c):
        return cr(a, b, p) >= -1e-9 and cr(b, c, p) >= -1e-9 and cr(c, a, p) >= -1e-9

    out = []
    guard = 0
    while len(idx) > 3 and guard < 10000:
        guard += 1
        m = len(idx)
        for k in range(m):
            i0, i1, i2 = idx[(k - 1) % m], idx[k], idx[(k + 1) % m]
            a, b, c = pts[i0], pts[i1], pts[i2]
            if cr(a, b, c) <= 1e-9:
                continue
            if any(inside(pts[j], a, b, c) for j in idx if j not in (i0, i1, i2)
                   and pts[j] not in (a, b, c)):
                continue
            out.append((i0, i1, i2))
            idx.pop(k)
            break
        else:
            # only collinear leftovers remain: drop the flattest vertex
            m = len(idx)
            k = min(range(m), key=lambda q: abs(cr(pts[idx[(q - 1) % m]], pts[idx[q]],
                                                   pts[idx[(q + 1) % m]])))
            idx.pop(k)
    if len(idx) == 3:
        out.append(tuple(idx))
    return out


def planar(pts, n, reg, bounds=None):
    """UVs for a planar face, projected along its normal, upright as the viewer sees it."""
    right, up = face_axes(n)
    rs = [dot(p, right) for p in pts]
    us = [dot(p, up) for p in pts]
    r0, r1, u0, u1 = bounds if bounds else (min(rs), max(rs), min(us), max(us))
    dr = (r1 - r0) or 1.0
    du = (u1 - u0) or 1.0
    return [reg.uv((r - r0) / dr, (u1 - u) / du) for r, u in zip(rs, us)]


def face(m, pts, n, reg, bounds=None):
    m.poly(pts, planar(pts, n, reg, bounds), n)


# ---- primitives -------------------------------------------------------------------------------
def obox(m, c, axes, half, reg, regs=None, skip=(), grain=None):
    """Oriented box. ``axes`` three unit vectors, ``half`` the half sizes along them. Faces are
    named by local axis: x-, x+, y-, y+, z-, z+. ``grain``: a local axis index whose direction
    becomes 'up' on the four faces parallel to it, so wood grain runs along the board."""
    regs = regs or {}
    for i, name in enumerate("xyz"):
        for sgn, sname in ((-1, "-"), (1, "+")):
            key = name + sname
            if key in skip:
                continue
            n = mul(axes[i], sgn)
            centre = add(c, mul(n, half[i]))
            j, k = [q for q in range(3) if q != i]
            a, b = mul(axes[j], half[j]), mul(axes[k], half[k])
            pts = [add(add(centre, mul(a, sa)), mul(b, sb))
                   for sa, sb in ((-1, -1), (1, -1), (1, 1), (-1, 1))]
            r = regs.get(key, reg)
            if grain is not None and grain != i:
                g = axes[grain]
                rt = cross(g, n)
                rs = [dot(p, rt) for p in pts]
                us = [dot(p, g) for p in pts]
                bnd = (min(rs), max(rs), min(us), max(us))
                uvs = [r.uv((x - bnd[0]) / ((bnd[1] - bnd[0]) or 1),
                            (bnd[3] - y) / ((bnd[3] - bnd[2]) or 1)) for x, y in zip(rs, us)]
                m.poly(pts, uvs, n)
            else:
                face(m, pts, n, r)


def box(m, lo, hi, reg, regs=None, skip=(), grain=None):
    c = mul(add(lo, hi), 0.5)
    half = mul(sub(hi, lo), 0.5)
    obox(m, c, (X, Y, Z), half, reg, regs, skip, grain)


def beam(m, p0, p1, w, d, reg, hint=Y, regs=None, skip=()):
    """A board of section w x d from p0 to p1, its grain along the length."""
    a1 = norm(sub(p1, p0))
    if abs(dot(a1, hint)) > 0.95:
        hint = X if abs(a1[0]) < 0.9 else Z
    a0 = norm(cross(a1, hint))
    a2 = cross(a0, a1)
    ln = math.sqrt(dot(sub(p1, p0), sub(p1, p0)))
    obox(m, mul(add(p0, p1), 0.5), (a0, a1, a2), (w / 2.0, ln / 2.0, d / 2.0), reg,
         regs, skip, grain=1)


def perp_frame(axis):
    axis = norm(axis)
    helper = X if abs(axis[0]) < 0.9 else Z
    p = norm(cross(helper, axis))
    q = cross(axis, p)
    return p, q


def lathe(m, profile, reg, origin, axis=Y, sides=16, caps=(False, False), cap_reg=None,
          jitter=None, phase=0.0):
    """Surface of revolution. ``profile``: (radius, height) pairs bottom to top along ``axis``.
    t runs from the top of the profile (0) to the bottom (1); s runs once round.
    ``jitter(i, k)`` scales ring k's radius at vertex i (for ragged branch tips)."""
    axis = norm(axis)
    p_ax, q_ax = perp_frame(axis)
    lens = [0.0]
    for (ra, ha), (rb, hb) in zip(profile, profile[1:]):
        lens.append(lens[-1] + math.hypot(rb - ra, hb - ha))
    total = lens[-1] or 1.0
    rings = []
    for k, (r, h) in enumerate(profile):
        pr, ph = profile[max(k - 1, 0)]
        nr_, nh = profile[min(k + 1, len(profile) - 1)]
        dr, dh = nr_ - pr, nh - ph
        ln = math.hypot(dr, dh) or 1.0
        cn, ch = dh / ln, -dr / ln
        ring = []
        for i in range(sides + 1):
            a = phase + 2.0 * math.pi * i / sides
            rr = r * (jitter(i % sides, k) if jitter else 1.0)
            radial = add(mul(p_ax, math.cos(a)), mul(q_ax, math.sin(a)))
            pos = add(add(origin, mul(radial, rr)), mul(axis, h))
            nrm = add(mul(radial, cn), mul(axis, ch))
            ring.append((pos, nrm, reg.uv(i / sides, 1.0 - lens[k] / total)))
        rings.append(ring)
    def plain(r, h, i):
        a = phase + 2.0 * math.pi * i / sides
        radial = add(mul(p_ax, math.cos(a)), mul(q_ax, math.sin(a)))
        return add(add(origin, mul(radial, r)), mul(axis, h))

    for k in range(len(profile) - 1):
        ra, rb = profile[k][0], profile[k + 1][0]
        lo, hi = rings[k], rings[k + 1]
        # settle this strip's winding once, on the un-jittered surface, so a ragged ring
        # cannot flip single triangles inside out
        (r0, h0), (r1, h1) = profile[k], profile[k + 1]
        if r0 > 1e-9:
            t0, t1, t2 = plain(r0, h0, 0), plain(r0, h0, 1), plain(r1, h1, 1 if r1 > 1e-9 else 0)
        else:
            t0, t1, t2 = plain(r0, h0, 0), plain(r1, h1, 1), plain(r1, h1, 0)
        want = add(add(lo[0][1], lo[1][1]), add(hi[0][1], hi[1][1]))
        keep = 1 if dot(cross(sub(t1, t0), sub(t2, t0)), want) > 0 else -1
        for i in range(sides):
            if ra < 1e-9 and rb < 1e-9:
                continue
            if rb < 1e-9:
                m.tri([lo[i][0], lo[i + 1][0], hi[i][0]], [lo[i][2], lo[i + 1][2], hi[i][2]],
                      [lo[i][1], lo[i + 1][1], add(lo[i][1], lo[i + 1][1])], keep)
            elif ra < 1e-9:
                m.tri([lo[i][0], hi[i + 1][0], hi[i][0]], [lo[i][2], hi[i + 1][2], hi[i][2]],
                      [add(hi[i][1], hi[i + 1][1]), hi[i + 1][1], hi[i][1]], keep)
            else:
                m.tri([lo[i][0], lo[i + 1][0], hi[i + 1][0]],
                      [lo[i][2], lo[i + 1][2], hi[i + 1][2]],
                      [lo[i][1], lo[i + 1][1], hi[i + 1][1]], keep)
                m.tri([lo[i][0], hi[i + 1][0], hi[i][0]],
                      [lo[i][2], hi[i + 1][2], hi[i][2]],
                      [lo[i][1], hi[i + 1][1], hi[i][1]], keep)
    creg = cap_reg or reg
    for end, want in ((0, caps[0]), (len(profile) - 1, caps[1])):
        if not want or profile[end][0] < 1e-9:
            continue
        n = mul(axis, -1.0 if end == 0 else 1.0)
        ring = rings[end]
        centre = add(origin, mul(axis, profile[end][1]))
        for i in range(sides):
            a0 = phase + 2.0 * math.pi * i / sides
            a1 = phase + 2.0 * math.pi * (i + 1) / sides
            m.tri([centre, ring[i][0], ring[i + 1][0]],
                  [creg.uv(0.5, 0.5),
                   creg.uv(0.5 + 0.5 * math.cos(a0), 0.5 + 0.5 * math.sin(a0)),
                   creg.uv(0.5 + 0.5 * math.cos(a1), 0.5 + 0.5 * math.sin(a1))], n)


def tube(m, path, r, reg, sides=6, closed=False, caps=True):
    """A round tube swept along a polyline with parallel-transported frames."""
    pts = list(path)
    if closed:
        pts = pts + [pts[0]]
    n = len(pts)
    tangents = []
    for i in range(n):
        if closed and (i == 0 or i == n - 1):
            t = add(norm(sub(pts[1], pts[0])), norm(sub(pts[-1], pts[-2])))
        elif i == 0:
            t = sub(pts[1], pts[0])
        elif i == n - 1:
            t = sub(pts[-1], pts[-2])
        else:
            t = add(norm(sub(pts[i], pts[i - 1])), norm(sub(pts[i + 1], pts[i])))
        tangents.append(norm(t))
    p_ax, _ = perp_frame(tangents[0])
    frames = []
    for i in range(n):
        t = tangents[i]
        p_ax = norm(sub(p_ax, mul(t, dot(p_ax, t))))
        frames.append((p_ax, cross(t, p_ax)))
    lens = [0.0]
    for a, b in zip(pts, pts[1:]):
        lens.append(lens[-1] + math.sqrt(dot(sub(b, a), sub(b, a))))
    total = lens[-1] or 1.0
    rings = []
    for i in range(n):
        pa, qa = frames[i]
        ring = []
        for k in range(sides + 1):
            a = 2.0 * math.pi * k / sides
            radial = add(mul(pa, math.cos(a)), mul(qa, math.sin(a)))
            ring.append((add(pts[i], mul(radial, r)), radial, reg.uv(k / sides, lens[i] / total)))
        rings.append(ring)
    for i in range(n - 1):
        lo, hi = rings[i], rings[i + 1]
        for k in range(sides):
            m.tri([lo[k][0], lo[k + 1][0], hi[k + 1][0]], [lo[k][2], lo[k + 1][2], hi[k + 1][2]],
                  [lo[k][1], lo[k + 1][1], hi[k + 1][1]])
            m.tri([lo[k][0], hi[k + 1][0], hi[k][0]], [lo[k][2], hi[k + 1][2], hi[k][2]],
                  [lo[k][1], hi[k + 1][1], hi[k][1]])
    if caps and not closed:
        for end, sgn in ((0, -1.0), (n - 1, 1.0)):
            nrm = mul(tangents[end], sgn)
            ring = rings[end]
            for k in range(1, sides - 1):
                m.tri([ring[0][0], ring[k][0], ring[k + 1][0]],
                      [reg.uv(0.5, 0.0), reg.uv(0.5, 0.0), reg.uv(0.5, 0.0)], nrm)


def poly_area(poly):
    return 0.5 * sum(poly[i][0] * poly[(i + 1) % len(poly)][1]
                     - poly[(i + 1) % len(poly)][0] * poly[i][1] for i in range(len(poly)))


def offset_poly(poly, d):
    """Move every vertex of a simple polygon inward by d (mitred)."""
    ccw = poly_area(poly) > 0
    n = len(poly)
    out = []
    for i in range(n):
        p0, p1, p2 = poly[i - 1], poly[i], poly[(i + 1) % n]
        e1 = (p1[0] - p0[0], p1[1] - p0[1])
        e2 = (p2[0] - p1[0], p2[1] - p1[1])

        def inward(e):
            ln = math.hypot(*e) or 1.0
            return ((-e[1] / ln, e[0] / ln) if ccw else (e[1] / ln, -e[0] / ln))
        n1, n2 = inward(e1), inward(e2)
        avg = (n1[0] + n2[0], n1[1] + n2[1])
        ln = math.hypot(*avg) or 1.0
        avg = (avg[0] / ln, avg[1] / ln)
        k = d / max(avg[0] * n1[0] + avg[1] * n1[1], 0.3)
        out.append((p1[0] + avg[0] * k, p1[1] + avg[1] * k))
    return out


def extrude(m, poly, origin, e1, e2, d0, d1, reg_side, reg_top=None, reg_bottom=None,
            inward=False, top_bounds=None):
    """Prism of a 2D polygon (coords along e1, e2) between d0 and d1 along n = e1 x e2.
    The d1 cap faces +n, the d0 cap -n; a cap region of None leaves that cap off. ``inward``
    turns the side walls to face into the prism (the inside of a case)."""
    n = norm(cross(e1, e2))
    poly = [p for i, p in enumerate(poly)
            if math.hypot(p[0] - poly[i - 1][0], p[1] - poly[i - 1][1]) > 1e-6]
    ccw = poly_area(poly) > 0
    k = len(poly)

    def at(p, d):
        return add(add(add(origin, mul(e1, p[0])), mul(e2, p[1])), mul(n, d))
    lens = [0.0]
    for i in range(k):
        a, b = poly[i], poly[(i + 1) % k]
        lens.append(lens[-1] + math.hypot(b[0] - a[0], b[1] - a[1]))
    total = lens[-1]
    for i in range(k):
        a, b = poly[i], poly[(i + 1) % k]
        ex, ey = b[0] - a[0], b[1] - a[1]
        if math.hypot(ex, ey) < 1e-9:
            continue
        o2 = (ey, -ex) if ccw else (-ey, ex)
        out = norm(add(mul(e1, o2[0]), mul(e2, o2[1])))
        if inward:
            out = mul(out, -1.0)
        s0, s1 = lens[i] / total, lens[i + 1] / total
        pts = [at(a, d0), at(b, d0), at(b, d1), at(a, d1)]
        uvs = [reg_side.uv(s0, 1), reg_side.uv(s1, 1), reg_side.uv(s1, 0), reg_side.uv(s0, 0)]
        m.tri(pts[:3], uvs[:3], out)
        m.tri([pts[0], pts[2], pts[3]], [uvs[0], uvs[2], uvs[3]], out)
    for d, reg, nn in ((d1, reg_top, n), (d0, reg_bottom, mul(n, -1.0))):
        if reg is None:
            continue
        pts = [at(p, d) for p in poly]
        face(m, pts, nn, reg, top_bounds if nn is n else None)


# ==============================================================================================
# output plumbing
# ==============================================================================================
OUTPUTS = {}


def emit(rel, data):
    if isinstance(data, str):
        data = data.encode("utf-8")
    OUTPUTS[os.path.join(ASSETS, *rel.split("/"))] = data


def png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, format="PNG", optimize=True)
    return buf.getvalue()


def rot_xy(v, rx, ry):
    """Forge applies a transform's [x, y, z] rotation as Rx * Ry * Rz (y first)."""
    x, y, z = v
    a = math.radians(ry)
    x, z = x * math.cos(a) + z * math.sin(a), -x * math.sin(a) + z * math.cos(a)
    b = math.radians(rx)
    y, z = y * math.cos(b) - z * math.sin(b), y * math.sin(b) + z * math.cos(b)
    return (x, y, z)


def inventory_transform(lo, hi):
    """Fit a model bigger than a block into the item slot: scale it down about the block
    centre and translate its own centre back onto the block centre in each view."""
    lo = [c / 16.0 for c in lo]
    hi = [c / 16.0 for c in hi]
    extent = max(h - l for l, h in zip(lo, hi))
    centre = [(l + h) / 2.0 - 0.5 for l, h in zip(lo, hi)]
    views = {
        "gui": ((30, 225, 0), 0.625),
        "ground": ((0, 0, 0), 0.25),
        "fixed": ((0, 0, 0), 0.5),
        "thirdperson_righthand": ((75, 45, 0), 0.375),
        "thirdperson_lefthand": ((75, 225, 0), 0.375),
        "firstperson_righthand": ((0, 45, 0), 0.40),
        "firstperson_lefthand": ((0, 225, 0), 0.40),
    }
    out = {}
    for name, (rot, base) in views.items():
        s = round(base / max(extent, 1.0), 4)
        c = rot_xy(centre, rot[0], rot[1])
        t = [round(-s * v, 4) + 0.0 for v in c]
        if name in ("thirdperson_righthand", "thirdperson_lefthand"):
            t[1] = round(t[1] + 0.16, 4)
        entry = {"rotation": list(rot), "translation": t, "scale": s}
        out[name] = entry
    return out


def blockstate(name, lo, hi):
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "csm:furniture/%s.obj" % name,
            "custom": {"flip-v": True},
        },
        "variants": {
            "facing": {
                "down": {"x": 90},
                "east": {"y": 90},
                "north": {},
                "south": {"y": 180},
                "up": {"x": 270},
                "west": {"y": 270},
            },
            "inventory": [{"transform": inventory_transform(lo, hi)}],
            "normal": [{}],
        },
    }


def finish(registry, mesh, atlas):
    name = registry
    emit("%s/%s.obj" % (MODEL_SUB, name), mesh.text(name))
    emit("%s/%s.mtl" % (MODEL_SUB, name),
         "# Procedurally generated by dev-env-utils/scripts/%s -- do not hand edit\n"
         "newmtl %s\nmap_Kd csm:blocks/furniture/%s\n" % (SCRIPT, MATERIAL, name))
    emit("%s/%s.png" % (TEX_SUB, name), png_bytes(atlas.img))
    lo, hi = mesh.bounds()
    emit("%s/%s.json" % (BS_SUB, registry), json.dumps(blockstate(name, lo, hi), indent=2) + "\n")
    BOUNDS[registry] = (lo, hi)


BOUNDS = {}


# ==============================================================================================
# 1. jukebox -- a 1950s arched cabinet with bubble-tube light bands
# ==============================================================================================
WALNUT = (104, 62, 36)
CHROME = (200, 204, 210)


def arch_path(x0, x1, y_bottom, y_spring, y_top, k, steps=20):
    """The arch outline inset by k, left foot -> over the top -> right foot (x, y)."""
    cx = (x0 + x1) / 2.0
    rx, ry = (x1 - x0) / 2.0 - k, (y_top - y_spring) - k
    pts = [(x0 + k, y_bottom), (x0 + k, y_spring)]
    for i in range(1, steps):
        a = math.pi - math.pi * i / steps
        pts.append((cx + rx * math.cos(a), y_spring + ry * math.sin(a)))
    pts += [(x1 - k, y_spring), (x1 - k, y_bottom)]
    return pts


def build_jukebox():
    rng = random.Random(1015)
    X0, X1, YB, YS, YT = 1.0, 15.0, 1.6, 15.5, 24.0
    ZF, ZB = 4.0, 13.0
    PX = 8  # texels per unit on the front
    fw, fh = int((X1 - X0) * PX), int(round((YT - YB) * PX))

    def fx(h):   # viewer's distance from the cabinet's left edge -> column
        return h * PX

    def fy(y):
        return (YT - y) * PX

    front = Image.new("RGBA", (fw, fh))
    front.paste(wood(fw, fh, WALNUT, rng, streak=0.18, rings=9), (0, 0))
    d = ImageDraw.Draw(front)
    # speaker grille: dark cloth behind a gilt lattice
    gw, gh = int(7.2 * PX), int(6.8 * PX)
    grille = Image.new("RGBA", (gw, gh), (52, 30, 26, 255))
    gld = ImageDraw.Draw(grille)
    for i in range(-8, 10):
        xa = i * 7
        gld.line([(xa, gh), (xa + gh, 0)], fill=(176, 140, 70, 255), width=1)
        gld.line([(xa, 0), (xa + gh, gh)], fill=(176, 140, 70, 255), width=1)
    front.paste(grille, (int(fx(3.4)), int(fy(9.0))))
    # chrome rail over the grille
    d.rectangle([fx(3.4), fy(9.7), fx(10.6), fy(9.0)], fill=CHROME + (255,))
    # title strip rack: two columns of cream cards in a chrome frame
    d.rectangle([fx(3.4), fy(13.6), fx(10.6), fy(9.7)], fill=(150, 154, 160, 255))
    for col in range(2):
        for row in range(5):
            x0 = fx(3.7 + col * 3.5)
            y0 = fy(13.3 - row * 0.72)
            d.rectangle([x0, y0, x0 + 3.1 * PX, y0 + 0.55 * PX], fill=(238, 228, 196, 255))
            d.line([(x0 + 3, y0 + 1), (x0 + 3.1 * PX - 4, y0 + 1)], fill=(170, 40, 34, 255))
            d.line([(x0 + 5, y0 + 3), (x0 + 3.1 * PX - 8, y0 + 3)], fill=(60, 60, 70, 255))
    # record window: dark glass under the arch, a record on the platter, the tone arm
    win = Image.new("L", (fw, fh), 0)
    wd = ImageDraw.Draw(win)
    cx = (X1 - X0) / 2.0
    rx, ry = cx - 3.4, (YT - YS) - 3.4
    wd.rectangle([fx(3.4), fy(YS), fx(10.6), fy(13.8)], fill=255)
    wd.ellipse([fx(cx - rx), fy(YS + ry), fx(cx + rx), fy(YS - ry)], fill=255)
    glass = Image.new("RGBA", (fw, fh), (26, 34, 52, 255))
    gd = ImageDraw.Draw(glass)
    gd.rectangle([fx(3.4), fy(14.6), fx(10.6), fy(13.8)], fill=(120, 120, 128, 255))
    # the record magazine: discs standing on edge under the platter
    for i in range(int(fx(3.9)), int(fx(10.1)), 2):
        gd.line([(i, fy(15.6)), (i, fy(14.6))], fill=(10, 10, 12, 255))
        gd.point((i, fy(15.1)), fill=(170, 40, 36, 255))
    rc = (fx(cx), fy(16.6))
    rr = 2.5 * PX
    gd.ellipse([rc[0] - rr, rc[1] - rr * 0.55, rc[0] + rr, rc[1] + rr * 0.55],
               fill=(14, 14, 16, 255))
    for g in range(3, int(rr), 3):
        gd.ellipse([rc[0] - g, rc[1] - g * 0.55, rc[0] + g, rc[1] + g * 0.55],
                   outline=(40, 40, 46, 255))
    gd.ellipse([rc[0] - 6, rc[1] - 3.3, rc[0] + 6, rc[1] + 3.3], fill=(196, 40, 36, 255))
    gd.line([(fx(10.0), fy(19.5)), (fx(9.2), fy(17.0)), (rc[0] + 5, rc[1] - 1)],
            fill=CHROME + (255,), width=2)
    gd.ellipse([fx(10.0) - 3, fy(19.5) - 3, fx(10.0) + 3, fy(19.5) + 3], fill=CHROME + (255,))
    for i in range(0, 14):   # a glint across the glass
        gd.point((fx(4.2) + i * 2, fy(21.0) + i * 3), fill=(120, 140, 170, 255))
        gd.point((fx(4.2) + i * 2 + 1, fy(21.0) + i * 3), fill=(90, 110, 140, 255))
    front.paste(glass, (0, 0), win)

    at = Atlas(256)
    at.add("front", front)
    at.add("walnut", wood(64, 64, WALNUT, rng, 0.2, 5))
    at.add("back", flat(32, 32, shade(WALNUT, 0.7), rng, 5))
    at.add("chrome", metal(16, 32, CHROME, rng, 0.4))
    at.add("plinth", metal(32, 16, (70, 70, 76), rng, 0.3))
    at.add("red", tube_swatch(8, 64, (236, 52, 36), rng, True))
    at.add("amber", tube_swatch(8, 64, (255, 150, 30), rng, True))
    at.add("yellow", tube_swatch(8, 64, (255, 220, 80), rng, True))
    at.add("green", tube_swatch(8, 64, (80, 210, 110), rng, True))
    btn = Image.new("RGBA", (64, 8), (40, 40, 44, 255))
    bd = ImageDraw.Draw(btn)
    for i in range(10):
        bd.rectangle([2 + i * 6, 2, 5 + i * 6, 5],
                     fill=((230, 226, 214, 255) if i % 2 else (200, 50, 40, 255)))
    at.add("buttons", btn)

    m = Mesh()
    body = [(16.0 - x, y) for x, y in arch_path(X0, X1, YB, YS, YT, 0.0, 24)]
    # front frame: a = 16 - x (the viewer's right), b = y, n = -z
    extrude(m, body, (16.0, 0.0, ZB), (-1.0, 0.0, 0.0), Y, 0.0, ZB - ZF, at["walnut"],
            reg_top=at["front"], reg_bottom=at["back"])
    box(m, (0.6, 0.02, 3.2), (15.4, YB, 13.4), at["plinth"])
    # light bands: chrome outside, then the coloured tubes, chrome again inside
    for i, (k, r, reg) in enumerate(((0.45, 0.45, "chrome"), (1.35, 0.42, "red"),
                                     (2.2, 0.40, "amber"), (3.0, 0.36, "yellow"),
                                     (3.62, 0.24, "chrome"))):
        path = [(x, y, ZF - r + 0.12)
                for x, y in arch_path(X0, X1, YB + 0.25 + 0.03 * i, YS, YT, k, 24)]
        tube(m, path, r, at[reg], sides=8)
    # side pilasters' own green tube down each flank
    for x in (X0 - 0.02, X1 + 0.02):
        path = [(x, YB + 0.4, 6.0), (x, YS, 6.0)]
        tube(m, path, 0.35, at["green"], sides=8)
    # grille bars, window sill, selector buttons
    for h in (4.5, 5.75, 7.0, 8.25, 9.5):
        tube(m, [(16.0 - (X0 + h), 2.5, ZF - 0.25), (16.0 - (X0 + h), 8.8, ZF - 0.25)], 0.2,
             at["chrome"], sides=6)
    tube(m, [(X0 + 3.3, 13.75, ZF - 0.28), (X1 - 3.3, 13.75, ZF - 0.28)], 0.3, at["chrome"])
    box(m, (X0 + 3.6, 9.05, ZF - 0.45), (X1 - 3.6, 9.65, ZF - 0.02), at["chrome"],
        regs={"z-": at["buttons"]})
    return m, at


# ==============================================================================================
# 2. grand piano -- lid up on the long stick
# ==============================================================================================
LACQUER = (20, 20, 23)
BRASS = (200, 160, 70)


def smoothstep(t):
    t = min(max(t, 0.0), 1.0)
    return t * t * (3 - 2 * t)


# case outline in (u, w): u from the spine (the straight bass side), w back from the case front
P_WIDTH, P_LEN, P_TREBLE_STRAIGHT, P_TAIL_START, P_TAIL_U = 24.0, 30.0, 7.0, 23.0, 10.0
SPINE_X, FRONT_Z = 20.0, -5.0


def piano_outline(front_w=0.0, steps=22):
    pts = [(0.0, front_w), (P_WIDTH, front_w)]
    pts.append((P_WIDTH, max(P_TREBLE_STRAIGHT, front_w)))
    for i in range(1, steps + 1):
        w = P_TREBLE_STRAIGHT + (P_TAIL_START - P_TREBLE_STRAIGHT) * i / steps
        pts.append((P_WIDTH - (P_WIDTH - P_TAIL_U) * smoothstep((w - P_TREBLE_STRAIGHT)
                                                              / (P_TAIL_START
                                                                 - P_TREBLE_STRAIGHT)), w))
    tail_r = P_LEN - P_TAIL_START
    for i in range(1, steps - 1):   # the last step is skipped: its inset would fold over
        a = 0.5 * math.pi * i / steps
        pts.append((P_TAIL_U * math.cos(a), P_TAIL_START + tail_r * math.sin(a)))
    pts.append((0.0, P_LEN))
    return pts


def uw_to_xz(p):
    return (SPINE_X - p[0], FRONT_Z + p[1])


def build_piano():
    rng = random.Random(88)
    at = Atlas(256)
    # keyboard, drawn as the player sees it, then turned to the top face's frame
    kw, kh = 208, 28
    keys = Image.new("RGBA", (kw, kh), (242, 238, 226, 255))
    kd = ImageDraw.Draw(keys)
    for i in range(52):
        kd.line([(i * 4 + 3, 0), (i * 4 + 3, kh)], fill=(150, 146, 138, 255))
    names = "ABCDEFG"
    for i in range(51):
        if names[i % 7] in "ACDFG":
            x = i * 4 + 3
            kd.rectangle([x - 1, 0, x + 1, int(kh * 0.62)], fill=(16, 16, 18, 255))
            kd.point((x, 2), fill=(70, 70, 74, 255))
    kd.rectangle([0, 0, kw, 1], fill=(40, 20, 20, 255))  # the felt strip at the back
    at.add("keys_top", keys.rotate(180))
    kf = Image.new("RGBA", (kw, 6), (236, 232, 220, 255))
    kfd = ImageDraw.Draw(kf)
    for i in range(52):
        kfd.line([(i * 4 + 3, 0), (i * 4 + 3, 6)], fill=(150, 146, 138, 255))
    at.add("keys_front", kf)
    at.add("lacquer", metal(32, 32, (34, 34, 38), rng, 0.25))
    at.add("lacquer_flat", flat(16, 16, LACQUER, rng, 3))
    at.add("brass", metal(16, 16, BRASS, rng, 0.35))
    fb = flat(128, 12, LACQUER, rng, 3)
    fd = ImageDraw.Draw(fb)
    fd.rectangle([54, 4, 74, 6], fill=BRASS + (255,))
    fd.line([(48, 5), (52, 5)], fill=BRASS + (255,))
    fd.line([(76, 5), (80, 5)], fill=BRASS + (255,))
    at.add("fallboard", fb)
    desk = flat(96, 36, LACQUER, rng, 3)
    dd = ImageDraw.Draw(desk)
    for sx in (18, 50):
        dd.rectangle([sx, 5, sx + 28, 33], fill=(236, 230, 212, 255))
        for staff in range(3):
            for line in range(5):
                y = 9 + staff * 8 + line
                dd.line([(sx + 2, y), (sx + 26, y)], fill=(120, 116, 110, 255))
            for note in range(5):
                nx = sx + 4 + note * 5
                dd.point((nx, 9 + staff * 8 + rng.randint(0, 4)), fill=(20, 20, 20, 255))
    at.add("desk", desk)

    inner = offset_poly(piano_outline(), 0.7)
    inner_xz = [uw_to_xz(p) for p in inner]
    xs = [p[0] for p in inner_xz]
    zs = [p[1] for p in inner_xz]
    bx0, bx1, bz0, bz1 = min(xs), max(xs), min(zs), max(zs)
    iw, ih = 128, 160
    interior = wood(iw, ih, (212, 170, 112), rng, 0.12, 12)
    idr = ImageDraw.Draw(interior)

    def ip(x, z):
        return ((x - bx0) / (bx1 - bx0) * (iw - 1), (z - bz0) / (bz1 - bz0) * (ih - 1))
    plate = [uw_to_xz(p) for p in offset_poly(piano_outline(), 1.6)]
    plate = [(x, z) for x, z in plate]
    idr.polygon([ip(x, min(z, FRONT_Z + 26.0)) for x, z in plate], fill=(196, 156, 64, 255))
    for (hx, hz, hr) in ((13.0, 6.0, 1.3), (6.0, 7.5, 1.2), (12.0, 14.0, 1.4), (15.0, 20.0, 1.2)):
        a, b = ip(hx - hr, hz - hr), ip(hx + hr, hz + hr)
        idr.ellipse([a, b], fill=(150, 116, 60, 255))
    # strings: front to back, fanning a little, bass (copper) on the spine side
    for i in range(48):
        x = bx0 + 2.0 + (bx1 - bx0 - 3.5) * i / 47.0
        u = SPINE_X - x
        # the back end of the string: just inside the case outline at this u
        w_end = 3.0
        for (pu, pw) in piano_outline():
            if pw > 7 and abs(pu - u) < 1.2:
                w_end = max(w_end, pw)
        w_end = w_end - 2.2
        colour = (186, 120, 70, 255) if x > 14.0 else (208, 208, 212, 255)
        idr.line([ip(x, FRONT_Z + 3.0), ip(x + 0.6, FRONT_Z + w_end)], fill=colour)
    for i in range(40):  # tuning pins
        x = bx0 + 1.5 + (bx1 - bx0 - 3) * i / 39.0
        idr.point(ip(x, FRONT_Z + 1.8), fill=(232, 232, 236, 255))
    idr.rectangle([ip(bx0 + 1.5, FRONT_Z + 4.0), ip(bx1 - 1.5, FRONT_Z + 4.8)],
                  fill=(24, 24, 26, 255))
    at.add("interior", interior)

    m = Mesh()
    outline = piano_outline()
    out_xz = [uw_to_xz(p) for p in outline]
    # vertical frame: a = x, b = -z, n = +y
    poly_out = [(x, -z) for x, z in out_xz]
    poly_in = [(x, -z) for x, z in inner_xz]
    Y_BOT, Y_FLOOR, Y_RIM = 10.0, 11.4, 16.0
    extrude(m, poly_out, (0.0, 0.0, 0.0), X, (0.0, 0.0, -1.0), Y_BOT, Y_RIM, at["lacquer"],
            reg_bottom=at["lacquer_flat"])
    extrude(m, poly_in, (0.0, 0.0, 0.0), X, (0.0, 0.0, -1.0), Y_FLOOR, Y_RIM - 0.01,
            at["lacquer"], reg_top=None, inward=True)
    face(m, [(x, Y_FLOOR, z) for x, z in inner_xz], Y, at["interior"])
    # rim top: a strip between the outline and the inset
    n = len(out_xz)
    for i in range(n):
        j = (i + 1) % n
        pts = [(out_xz[i][0], Y_RIM, out_xz[i][1]), (out_xz[j][0], Y_RIM, out_xz[j][1]),
               (inner_xz[j][0], Y_RIM, inner_xz[j][1]), (inner_xz[i][0], Y_RIM, inner_xz[i][1])]
        uvs = [at["lacquer_flat"].uv(0.1, 0.1), at["lacquer_flat"].uv(0.9, 0.1),
               at["lacquer_flat"].uv(0.9, 0.9), at["lacquer_flat"].uv(0.1, 0.9)]
        m.tri(pts[:3], uvs[:3], Y)
        m.tri([pts[0], pts[2], pts[3]], [uvs[0], uvs[2], uvs[3]], Y)

    # keybed, keys, cheek blocks, fallboard
    KX0, KX1 = -1.8, 17.8
    box(m, (-3.9, 9.2, -8.0), (19.9, 10.8, FRONT_Z - 0.02), at["lacquer"])
    box(m, (KX0, 10.8, -7.6), (KX1, 11.6, FRONT_Z - 0.02), at["keys_front"],
        regs={"y+": at["keys_top"]}, skip=("z+", "y-"))
    for x0, x1 in ((-3.9, KX0), (KX1, 19.9)):
        box(m, (x0, 10.8, -8.0), (x1, 12.6, FRONT_Z - 0.02), at["lacquer"], skip=("z+", "y-"))
    box(m, (KX0, 11.6, FRONT_Z - 0.4), (KX1, 13.4, FRONT_Z - 0.02), at["fallboard"],
        regs={"y+": at["lacquer_flat"]}, skip=("z+", "y-"))
    # music desk, leaning back
    tilt = math.radians(14)
    up = (0.0, math.cos(tilt), math.sin(tilt))
    nz = (0.0, -math.sin(tilt), math.cos(tilt))
    # the desk rail: a flat board over the pin block, the desk standing on it
    box(m, (-3.3, 15.0, FRONT_Z + 0.7), (19.3, 15.5, FRONT_Z + 4.0), at["lacquer"],
        skip=("y-",))
    base = (8.0, 15.5, FRONT_Z + 3.2)
    obox(m, add(base, mul(up, 2.4)), (X, up, nz), (6.2, 2.4, 0.2), at["lacquer"],
         regs={"z-": at["desk"]})
    obox(m, add(base, mul(nz, -0.4)), (X, up, nz), (6.4, 0.2, 0.5), at["lacquer"])

    # lid on the long stick, hinged along the spine
    theta = math.radians(36)
    hinge = (SPINE_X, Y_RIM + 0.05, 0.0)
    e1 = (-math.cos(theta), math.sin(theta), 0.0)
    lid = [(u, FRONT_Z + w) for u, w in piano_outline(front_w=3.5)]
    extrude(m, lid, hinge, e1, Z, 0.0, 0.45, at["lacquer"], reg_top=at["lacquer"],
            reg_bottom=at["lacquer"])
    # the stick: from the rim of the bentside up to a cup under the lid
    w_s = 12.5
    u_edge = P_WIDTH - (P_WIDTH - P_TAIL_U) * smoothstep((w_s - P_TREBLE_STRAIGHT)
                                                         / (P_TAIL_START - P_TREBLE_STRAIGHT))
    foot = (SPINE_X - (u_edge - 0.4), Y_RIM, FRONT_Z + w_s)
    u_top = u_edge - 3.0
    top = add(add(hinge, mul(e1, u_top)), (0.0, 0.0, FRONT_Z + w_s))
    tube(m, [foot, top], 0.3, at["lacquer"], sides=6)
    box(m, (foot[0] - 0.5, Y_RIM, foot[2] - 0.5), (foot[0] + 0.5, Y_RIM + 0.3, foot[2] + 0.5),
        at["brass"], skip=("y-",))

    # three legs with brass casters
    leg = [(1.05, 0.9), (1.05, 1.4), (1.35, 1.8), (1.35, 2.4), (1.0, 2.9), (0.95, 6.5),
           (1.15, 8.6), (1.55, 9.3), (1.6, Y_BOT - 0.02)]
    for u, w in ((1.8, 2.2), (P_WIDTH - 1.8, 2.2), (5.0, 26.0)):
        x, z = uw_to_xz((u, w))
        lathe(m, leg, at["lacquer"], (x, 0.0, z), sides=10, caps=(True, True))
        box(m, (x - 0.35, 0.02, z - 0.7), (x + 0.35, 0.9, z + 0.7), at["brass"], skip=("y+",))
    # pedal lyre: two posts, the pedal box, three brass pedals
    for x in (6.6, 9.4):
        tube(m, [(x, 1.6, -3.6), (x, Y_BOT, -3.6)], 0.28, at["lacquer"], sides=6)
    box(m, (5.4, 0.3, -4.4), (10.6, 1.6, -2.8), at["lacquer"])
    for x in (6.3, 8.0, 9.7):
        box(m, (x - 0.3, 0.75, -5.9), (x + 0.3, 1.05, -4.4), at["brass"], skip=("z+",))
    return m, at


# ==============================================================================================
# 3. grandfather clock
# ==============================================================================================
MAHOGANY = (112, 50, 30)


def build_clock():
    rng = random.Random(1680)
    at = Atlas(256)
    at.add("wood", wood(32, 64, MAHOGANY, rng, 0.24, 4))
    at.add("wood_dark", flat(16, 32, shade(MAHOGANY, 0.45), rng, 4))
    at.add("brass", metal(16, 16, BRASS, rng, 0.4))
    dial = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    dd = ImageDraw.Draw(dial)
    dd.ellipse([0, 0, 63, 63], fill=(236, 228, 204, 255))
    dd.ellipse([3, 3, 60, 60], outline=(40, 34, 30, 255))
    dd.ellipse([9, 9, 54, 54], outline=(40, 34, 30, 255))
    for i in range(60):
        a = math.radians(i * 6)
        r0 = 26.0 if i % 5 else 20.5
        c = (31.5 + 29 * math.sin(a), 31.5 - 29 * math.cos(a))
        s = (31.5 + r0 * math.sin(a), 31.5 - r0 * math.cos(a))
        if i % 5 == 0:
            dd.line([s, (31.5 + 27.5 * math.sin(a), 31.5 - 27.5 * math.cos(a))],
                    fill=(30, 26, 24, 255), width=2)
        else:
            dd.point((31.5 + 27 * math.sin(a), 31.5 - 27 * math.cos(a)), fill=(60, 54, 50, 255))
        del c
    for ang, ln, wd_ in ((300, 13, 2), (60, 19, 1)):   # ten past ten
        a = math.radians(ang)
        dd.line([(31.5, 31.5), (31.5 + ln * math.sin(a), 31.5 - ln * math.cos(a))],
                fill=(20, 18, 18, 255), width=wd_ + 1)
    dd.ellipse([29, 29, 34, 34], fill=(20, 18, 18, 255))
    at.add("dial", dial)
    glass = Image.new("RGBA", (32, 64), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glass)
    for off, a in ((4, 150), (9, 110), (22, 120)):
        gd.line([(off, 63), (off + 18, 30)], fill=(214, 226, 236, a))
    at.add("glass", glass)

    m = Mesh()
    W, D = at["wood"], at["wood_dark"]
    ZBK = 15.8
    # bun feet, plinth and its moulding
    for x in (4.6, 11.4):
        for z in (10.4, 15.0):
            lathe(m, [(0.55, 0.02), (0.75, 0.35), (0.75, 0.7), (0.6, 1.02)], W, (x, 0.0, z),
                  sides=8, caps=(True, False))
    box(m, (4.0, 1.0, 9.8), (12.0, 8.0, ZBK), W, grain=1)
    box(m, (4.6, 2.0, 9.6), (11.4, 7.0, 9.8), W, skip=("z+",), grain=1)
    box(m, (3.6, 8.0, 9.4), (12.4, 8.8, ZBK), W, grain=0)
    # trunk: a frame round the pendulum window
    box(m, (4.8, 8.8, 10.4), (6.2, 22.0, ZBK), W, grain=1)
    box(m, (9.8, 8.8, 10.4), (11.2, 22.0, ZBK), W, grain=1)
    box(m, (6.2, 8.8, 10.4), (9.8, 11.0, ZBK), W, grain=0, skip=("x-", "x+"))
    box(m, (6.2, 20.0, 10.4), (9.8, 22.0, ZBK), W, grain=0, skip=("x-", "x+"))
    box(m, (6.2, 11.0, 14.6), (9.8, 20.0, ZBK), D, regs={"z+": W},
        skip=("x-", "x+", "y-", "y+"))
    # the door's glass: faint streaks, the rest cut away
    face(m, [(6.2, 11.0, 10.55), (9.8, 11.0, 10.55), (9.8, 20.0, 10.55), (6.2, 20.0, 10.55)],
         (0.0, 0.0, -1.0), at["glass"])
    # pendulum and the two weights on their chains
    box(m, (7.85, 13.2, 12.45), (8.15, 19.8, 12.65), at["brass"])
    lathe(m, [(0.0, -0.35), (1.25, -0.22), (1.45, 0.0), (1.25, 0.22), (0.0, 0.35)],
          at["brass"], (8.0, 13.2, 12.55), axis=Z, sides=12)
    for x, y0 in ((6.95, 15.8), (9.05, 17.0)):
        lathe(m, [(0.5, y0), (0.5, y0 + 2.4)], at["brass"], (x, 0.0, 13.6), sides=8,
              caps=(True, True))
        tube(m, [(x, y0 + 2.4, 13.6), (x, 20.0, 13.6)], 0.08, at["brass"], sides=4)
    # waist moulding, the hood
    box(m, (4.2, 22.0, 9.9), (11.8, 22.8, ZBK), W, grain=0)
    box(m, (4.0, 22.8, 10.2), (12.0, 30.4, ZBK), W, grain=1)
    for x in (4.45, 11.55):
        lathe(m, [(0.5, 22.8), (0.5, 23.3), (0.35, 23.5), (0.35, 29.7), (0.5, 29.9),
                  (0.5, 30.4)], at["brass"], (x, 0.0, 9.95), sides=8)
    # the dial, in a brass bezel
    dial_c = (8.0, 26.6, 10.2)
    pts = []
    for i in range(24):
        a = 2 * math.pi * i / 24
        pts.append((8.0 + 3.0 * math.cos(a), 26.6 + 3.0 * math.sin(a), 10.05))
    face(m, pts, (0.0, 0.0, -1.0), at["dial"])
    ring = [(8.0 + 3.15 * math.cos(2 * math.pi * i / 24),
             26.6 + 3.15 * math.sin(2 * math.pi * i / 24), 9.95) for i in range(24)]
    tube(m, ring, 0.22, at["brass"], sides=6, closed=True)
    del dial_c
    # cornice and the arched pediment, with a finial
    box(m, (3.6, 30.4, 9.7), (12.4, 31.2, ZBK), W, grain=0)
    ped = [(16.0 - x, y) for x, y in arch_path(4.2, 11.8, 31.2, 31.2, 33.4, 0.0, 16)]
    extrude(m, ped, (16.0, 0.0, ZBK), (-1.0, 0.0, 0.0), Y, 0.0, ZBK - 10.0, W,
            reg_top=W, reg_bottom=W)
    lathe(m, [(0.3, 33.3), (0.55, 33.6), (0.65, 34.0), (0.5, 34.4), (0.0, 34.7)],
          at["brass"], (8.0, 0.0, 12.9), sides=8)
    return m, at


# ==============================================================================================
# 4. porch swing on an A-frame
# ==============================================================================================
CEDAR = (164, 112, 70)


def build_swing():
    rng = random.Random(1904)
    at = Atlas(128)
    at.add("wood", wood(32, 64, CEDAR, rng, 0.22, 3))
    at.add("wood_seat", wood(32, 64, shade(CEDAR, 1.08), rng, 0.2, 3))
    chain = Image.new("RGBA", (8, 64), (60, 60, 64, 255))
    cd = ImageDraw.Draw(chain)
    for i in range(16):
        cd.rectangle([1, i * 4, 6, i * 4 + 2], fill=((150, 150, 156, 255) if i % 2
                                                     else (110, 110, 116, 255)))
    at.add("chain", chain)
    at.add("steel", metal(16, 16, (120, 122, 126), rng, 0.3))

    m = Mesh()
    W, S = at["wood"], at["wood_seat"]
    # frame: a beam on two A-legs
    box(m, (-14.0, 31.0, 7.0), (30.0, 33.0, 9.0), W, grain=0)
    for side in (-1, 1):
        xg = 8.0 + side * 21.0     # foot
        xt = 8.0 + side * 19.5     # top, under the beam
        for zg in (-3.0, 19.0):
            beam(m, (xg, 0.35, zg), (xt, 31.6, 8.0), 1.8, 1.8, W, hint=X)
        # cross brace, on the outside of the legs
        yb = 12.0
        f = (yb - 0.35) / (31.6 - 0.35)
        xl = xg + (xt - xg) * f + side * 1.3
        zl0, zl1 = -3.0 + 11.0 * f, 19.0 - 11.0 * f
        box(m, (xl - 0.4, yb - 1.0, zl0 - 1.2), (xl + 0.4, yb + 1.0, zl1 + 1.2), W, grain=2)
    # the bench
    X0, X1 = -4.0, 20.0
    for i in range(5):
        z0 = 4.05 + i * 1.65
        box(m, (X0, 7.4, z0), (X1, 8.0, z0 + 1.3), S, grain=0)
    for x in (-3.5, 8.0, 19.5):
        box(m, (x - 0.4, 6.4, 4.0), (x + 0.4, 7.4, 12.4), W, grain=2)
    box(m, (X0, 6.55, 3.4), (X1, 7.35, 4.0), W, grain=0)
    tilt = math.radians(15)
    up = (0.0, math.cos(tilt), math.sin(tilt))
    nz = (0.0, -math.sin(tilt), math.cos(tilt))
    pivot = (8.0, 7.6, 12.3)
    for i in range(4):
        c = add(pivot, mul(up, 2.2 + i * 1.8))
        obox(m, add(c, mul(nz, 0.1)), (X, up, nz), (11.6, 0.6, 0.3), S, grain=0)
    for x in (-3.5, 19.5):
        c = add((x, 7.6, 12.3), mul(up, 4.6))
        obox(m, add(c, mul(nz, 0.7)), (X, up, nz), (0.5, 4.6, 0.4), W, grain=1)
    # arms on their front posts
    for x0, x1 in ((X0, X0 + 1.2), (X1 - 1.2, X1)):
        box(m, (x0, 11.4, 3.4), (x1, 12.0, 12.8), W, grain=2)
        box(m, (x0 + 0.1, 8.0, 3.8), (x1 - 0.1, 11.4, 4.8), W, grain=1, skip=("y+",))
    # chains from eye bolts under the beam to the arm fronts and the back post tops
    for x in (X0 + 0.6, X1 - 0.6):
        hook = (x, 30.4, 8.0)
        box(m, (x - 0.3, 30.4, 7.7), (x + 0.3, 31.0, 8.3), at["steel"], skip=("y+",))
        tube(m, [hook, (x, 12.0, 4.1)], 0.18, at["chain"], sides=4)
        back_top = add((x, 7.6, 12.3), add(mul(up, 9.2), mul(nz, 0.7)))
        tube(m, [hook, back_top], 0.18, at["chain"], sides=4)
    return m, at


# ==============================================================================================
# 5. Christmas tree
# ==============================================================================================
def needles(w, h, rng):
    img = Image.new("RGBA", (w, h), (18, 58, 34, 255))
    d = ImageDraw.Draw(img)
    for _ in range(w * h // 5):
        x, y = rng.randrange(w), rng.randrange(h)
        light = 0.7 + 0.7 * (y / h)    # lighter toward the rim (t = 0.85 is the tips)
        c = shade((34, 92, 52), light * rng.uniform(0.7, 1.15))
        d.line([(x, y), (x + rng.choice((-2, -1, 1, 2)), y + 2)], fill=c + (255,))
    for y in range(int(h * 0.88), h):  # the underside is in shadow
        for x in range(w):
            r, g, b, a = img.getpixel((x, y))
            img.putpixel((x, y), (r // 2, g // 2, b // 2, a))
    return img


def build_tree():
    rng = random.Random(1225)
    at = Atlas(128)
    at.add("needles", needles(64, 64, rng))
    at.add("bark", wood(16, 32, (84, 58, 38), rng, 0.3, 2))
    at.add("stand", metal(16, 16, (170, 30, 30), rng, 0.4))
    skirt = flat(32, 32, (176, 28, 36), rng, 8)
    sd = ImageDraw.Draw(skirt)
    sd.rectangle([0, 26, 31, 31], fill=(240, 238, 232, 255))
    at.add("skirt", skirt)
    at.add("skirt_cap", flat(16, 16, (176, 28, 36), rng, 8))
    at.add("gold", metal(16, 16, (236, 190, 60), rng, 0.45))
    colours = {"red": (200, 24, 30), "blue": (30, 80, 200), "silver": (200, 204, 212),
               "purple": (130, 40, 170), "gold2": (230, 170, 40), "green": (40, 170, 90)}
    for name, c in colours.items():
        sw = Image.new("RGBA", (8, 8))
        for y in range(8):
            for x in range(8):
                k = 0.65 + 0.55 * math.exp(-(((x - 2.5) ** 2 + (y - 2.5) ** 2) / 5.0))
                sw.putpixel((x, y), shade(c, k) + (255,))
        at.add("b_" + name, sw)
    bulbs = {"warm": (255, 226, 150), "bred": (255, 90, 80), "bgreen": (120, 255, 140),
             "bblue": (120, 170, 255)}
    for name, c in bulbs.items():
        at.add(name, flat(4, 4, c, rng, 4))

    m = Mesh()
    C = (8.0, 0.0, 8.0)
    lathe(m, [(8.0, 0.03), (7.7, 0.3), (3.2, 0.5)], at["skirt"], C, sides=20,
          caps=(True, True), cap_reg=at["skirt_cap"])
    lathe(m, [(3.2, 0.5), (3.5, 1.1), (3.1, 2.2), (1.4, 2.4)], at["stand"], C, sides=12,
          caps=(False, True))
    lathe(m, [(0.9, 2.3), (0.8, 28.0)], at["bark"], C, sides=8, caps=(False, True))
    tiers = [(5.0, 10.5, 8.0), (10.0, 9.0, 7.5), (15.0, 7.4, 7.0), (19.5, 5.8, 6.5),
             (23.5, 4.2, 6.0), (27.0, 2.6, 5.0)]
    SIDES = 24
    for ti, (yb, R, h) in enumerate(tiers):
        tr = random.Random(700 + ti)
        tips = [1.0 + (0.16 if i % 2 == 0 else -0.06) + tr.uniform(-0.05, 0.05)
                for i in range(SIDES)]

        def jit(i, k, tips=tips):
            return tips[i] if k == 1 else (1.0 + 0.4 * (tips[i] - 1.0) if k == 2 else 1.0)
        top = ti == len(tiers) - 1
        prof = [(0.9, yb + 1.2), (R, yb - 0.3), (R * 0.55, yb + h * 0.45)]
        prof.append((0.0, yb + h) if top else (R * 0.14, yb + h))
        lathe(m, prof, at["needles"], C, sides=SIDES, jitter=jit, phase=ti * 0.4,
              caps=(False, not top))
    # baubles hanging under the branch tips, and a few nestled in the slopes
    names = list(colours)
    count = 0
    for ti, (yb, R, h) in enumerate(tiers[:-1]):
        n = max(3, int(R * 0.85))
        for k in range(n):
            a = 2 * math.pi * (k + rng.uniform(-0.2, 0.2)) / n + ti * 0.9
            r = R * rng.uniform(0.78, 0.88)
            rb = rng.uniform(0.5, 0.7)
            y = yb - 0.4 - rb
            c = (8.0 + r * math.cos(a), y, 8.0 + r * math.sin(a))
            reg = at["b_" + names[count % len(names)]]
            count += 1
            lathe(m, [(0.0, -rb), (rb * 0.72, -rb * 0.7), (rb, 0.0), (rb * 0.72, rb * 0.7),
                      (0.0, rb)], reg, c, sides=8)
            box(m, (c[0] - 0.15, y + rb - 0.1, c[2] - 0.15), (c[0] + 0.15, y + rb + 0.4,
                                                              c[2] + 0.15), at["gold"])
        for k in range(max(2, n // 2)):
            a = 2 * math.pi * (k + 0.5) / max(2, n // 2) + ti * 1.7
            f = rng.uniform(0.35, 0.7)
            y = yb - 0.3 + f * 0.45 * h
            r = (R - f * 0.45 * R) + 0.2
            rb = rng.uniform(0.45, 0.6)
            c = (8.0 + r * math.cos(a), y + rb * 0.5, 8.0 + r * math.sin(a))
            reg = at["b_" + names[count % len(names)]]
            count += 1
            lathe(m, [(0.0, -rb), (rb * 0.72, -rb * 0.7), (rb, 0.0), (rb * 0.72, rb * 0.7),
                      (0.0, rb)], reg, c, sides=8)
    # bead garland in swags round the tiers
    for ti, (yb, R, h) in enumerate(tiers[:-1]):
        pts = []
        for i in range(48):
            a = 2 * math.pi * i / 48
            sag = abs(math.sin(2.5 * a if ti % 2 else 3 * a))
            f = 0.30 - 0.18 * sag
            y = yb - 0.3 + f * 0.45 * h
            r = (R - f * 0.45 * R) * 1.0 + 0.45
            pts.append((8.0 + r * math.cos(a), y, 8.0 + r * math.sin(a)))
        tube(m, pts, 0.2, at["gold"], sides=4, closed=True)
    # lights: small bulbs on the slopes
    bnames = list(bulbs)
    for i in range(46):
        ti = i % (len(tiers) - 1)
        yb, R, h = tiers[ti]
        f = rng.uniform(0.15, 0.9)
        a = rng.uniform(0, 2 * math.pi)
        y = yb - 0.3 + f * 0.45 * h
        r = (R - f * 0.45 * R) + 0.15
        c = (8.0 + r * math.cos(a), y, 8.0 + r * math.sin(a))
        box(m, (c[0] - 0.2, c[1] - 0.2, c[2] - 0.2), (c[0] + 0.2, c[1] + 0.25, c[2] + 0.2),
            at[bnames[i % len(bnames)]])
    # the star: a double pyramid on a short stem
    lathe(m, [(0.3, 31.0), (0.3, 32.2)], at["gold"], C, sides=6, caps=(False, True))
    sc = (8.0, 33.9, 8.0)
    outline = []
    for i in range(10):
        a = math.pi / 2 + i * math.pi / 5
        r = 2.1 if i % 2 == 0 else 0.85
        outline.append((sc[0] + r * math.cos(a), sc[1] + r * math.sin(a), sc[2]))
    for zsgn in (-1.0, 1.0):
        apex = (sc[0], sc[1], sc[2] + zsgn * 0.8)
        for i in range(10):
            a, b = outline[i], outline[(i + 1) % 10]
            nrm = cross(sub(a, apex), sub(b, apex))
            if nrm[2] * zsgn < 0:
                nrm = mul(nrm, -1.0)
            reg = at["gold"]
            m.tri([apex, a, b], [reg.uv(0.4, 0.4), reg.uv(0.1, 0.9), reg.uv(0.9, 0.9)],
                  norm(nrm))
    return m, at


# ==============================================================================================
BUILDERS = [("csmjukebox", build_jukebox), ("grandpiano", build_piano),
            ("grandfatherclock", build_clock), ("swingchair", build_swing),
            ("cmastree", build_tree)]


def generate():
    for registry, builder in BUILDERS:
        mesh, atlas = builder()
        finish(registry, mesh, atlas)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--check", action="store_true", help="write nothing; exit 1 on drift")
    ap.add_argument("--bounds", action="store_true", help="print each model's bounds")
    args = ap.parse_args()
    generate()
    if args.bounds:
        for reg, (lo, hi) in BOUNDS.items():
            print("%-18s lo %s hi %s  (blocks: %s .. %s)" % (
                reg, tuple(round(c, 2) for c in lo), tuple(round(c, 2) for c in hi),
                tuple(round(c / 16, 4) for c in lo), tuple(round(c / 16, 4) for c in hi)))
    drift = []
    for path, data in sorted(OUTPUTS.items()):
        old = open(path, "rb").read() if os.path.exists(path) else None
        if old != data:
            drift.append(path)
            if not args.check:
                os.makedirs(os.path.dirname(path), exist_ok=True)
                with open(path, "wb") as fh:
                    fh.write(data)
    if args.check:
        for p in drift:
            print("drift:", os.path.relpath(p, layout.REPO_ROOT))
        print("%d files, %d drifted" % (len(OUTPUTS), len(drift)))
        return 1 if drift else 0
    print("%d files, %d written" % (len(OUTPUTS), len(drift)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
