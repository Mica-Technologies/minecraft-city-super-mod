#!/usr/bin/env python3
"""
Generates the multi-block luminaire arms (GitHub #99): the Caltrans davit arms that carry a street
light out from its pole, single and double, and the Hapco-style truss arms used across New
England, single and double.

WHY A SEPARATE GENERATOR
------------------------
These are built the way the mast arm curves are (MAST_ARM_CURVE_SYSTEM.md): one real block per
cell the arm passes through, all placed or none, the cell index in a tile entity, the cell list
and hitboxes generated together with the geometry. They reuse that block class
(BlockTrafficPoleMastArmCurve) through its ICurvedArmProfile, so placement, demolition and the
pole fit are the curves' own. What differs is everything the mast arm generator hard-codes: a slim
tapered tube rather than the 9.2-across mast arm, a tip that lands where every street light's
neck expects an arm (ARM_TIP in gen_streetlight_fixtures.py: x 6.5..9.5, y -0.25..3 at the far
face of the last cell), a rise that is not a whole number of blocks, several members (the truss),
and a mirrored second arm (the double arms).

SCALE
-----
The street lights are drawn about 1.2-1.4 times true size against a 1 m block (an M-400A is 15 px
for its 31.8 in), so the arms are too, at 1.4: a Caltrans 6 / 8 / 10 / 12 / 15 ft arm runs 2 / 3 /
4 / 5 / 6 blocks from the pole's face to its tip. Caltrans gives the arm tip's height above the
pole top for each length (Standard Plans ES-6A: 1'6", 2'0", 2'9", 3'9", 4'3"); that height is taken
as the arm's rise from where it leaves the pole, at the same 1.4. Every arm then leaves its pole at
about 30 degrees, the parabola levelling to the horizontal at the tip (the mast arm curves' rule).

The truss arms follow Hapco's RTA truss bracket: a curved, tapered upper member, a straight lower
member leaving the pole lower down and meeting the upper about two thirds of the way out, and one
vertical strut. 8 / 10 / 12 / 15 ft.

THE POLE JOINT
--------------
A luminaire arm's tube (4.2 across at the root, a 5.4 boot) is narrower than every CSM pole, so the
mast arm curves' saddle cut works against all three widths: each member that starts at the pole is
cut against the pole's own cylinder, once per CsmPoleFit (LARGE 6, THIN 4, PEDESTAL 3). Only the
cells holding a pole joint differ between fits; the generator finds them by comparing the meshes,
and the profile packs the fit into those cells' shape slots. There is no mount hardware on these
arms, so a shape is just a slot -- no mount mask.

OUTPUT
------
  models/block/trafficaccessories/shared_models/luminairearm_<id>_c<n>.obj        one per cell
  models/block/trafficaccessories/shared_models/luminairearm_<id>_c<n>_<fit>.obj  pole-joint cells
  models/block/trafficaccessories/shared_models/luminairearm_<id>_inv.obj         inventory icon
  models/block/trafficaccessories/shared_models/luminairearm.mtl
  blockstates/luminairearm<id><finish>.json
  src/main/java/.../trafficaccessories/LuminaireArmProfile.java                    (generated enum)

Run:  python dev-env-utils/scripts/gen_luminaire_arms.py [--check] [--fragments]
"""

import io
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

OWNER = layout.owner_of_folder("trafficaccessories")
MODEL_REL = "models/block/trafficaccessories/shared_models"
MODEL_PREFIX = "csm:trafficaccessories/shared_models"
JAVA_REL = "trafficaccessories/LuminaireArmProfile.java"
MTL_NAME = "luminairearm.mtl"
MATERIAL = "body"

# (finish id, texture, label). Galvanized steel is the pole family's "unpainted" texture.
FINISHES = [
    ("galvanized", "csm:blocks/trafficsignals/shared_textures/metal_white", "Galvanized"),
    ("silver", "csm:blocks/trafficsignals/shared_textures/metal_silver", "Silver"),
]

# Finishes a profile is offered in, where it is not FINISHES: the Anaheim arm only ever comes in
# the city's P7-Green (the decorative poles' swatch, in Core).
ANAHEIM_GREEN = ("green", "csm:blocks/decorativepoles/paint_anaheim_green", "Anaheim Green")
BLACK = ("black", "csm:blocks/trafficsignals/shared_textures/metal_black", "Black")
FINISHES_FOR = {
    "anaheim8ft": [ANAHEIM_GREEN],
    "laupsweep8ft": FINISHES + [BLACK],
    "laupsweep10ft": FINISHES + [BLACK],
    "laupsweep12ft": FINISHES + [BLACK],
}


def finishes_of(pid):
    return FINISHES_FOR.get(pid, FINISHES)


SCALE = 1.4                 # the arms are drawn 1.4 times true size, as the fixtures are
PX_PER_M = 16.0
FT = 0.3048
SIDES = 12
SAMPLES_PER_BLOCK = 14
INSET = 0.01                # keeps the tip cap off the block boundary plane

# Where a street light's neck expects the arm: the centre of ARM_TIP (gen_streetlight_fixtures).
TIP_Y = 1.375

# The pole in the root cell's frame: centred on the block behind (z 16..32), axis at x 8, z 24.
POLE_X, POLE_Z = 8.0, 24.0
# CsmPoleFit's order and radii; the ordinal is what the shape slot is computed from.
FITS = [("large", 6.0), ("thin", 4.0), ("pedestal", 3.0)]

# Tube radii (px). The tip is 2.8 across, inside the 3 x 3.25 ARM_TIP every fixture neck holds.
R_ROOT, R_TIP, R_BOOT = 2.1, 1.4, 2.7
TRUSS_UPPER = (1.8, 1.4)
TRUSS_LOWER = 1.25
TRUSS_STRUT = 0.75


def ft_px(feet):
    return feet * FT * SCALE * PX_PER_M


# (id, kind, run in blocks, rise in px, length in ft, label)
CALTRANS = [
    ("caltrans6ft", 2, ft_px(1.5), 6),
    ("caltrans8ft", 3, ft_px(2.0), 8),
    # Caltrans' 2'9" puts this tip one row up from a root too low in its block; 3'0" keeps the
    # root on the pole's side at y 13 (a 3 in rounding of the drawing's height, recorded here).
    ("caltrans10ft", 4, ft_px(3.0), 10),
    ("caltrans12ft", 5, ft_px(3.75), 12),
    ("caltrans15ft", 6, ft_px(4.25), 15),
]
# Hapco RTA truss: the upper member's rise 3'3"; the two members leave the pole 2'0" apart.
TRUSS = [
    ("truss8ft", 3, 8),
    ("truss10ft", 4, 10),
    ("truss12ft", 5, 12),
    ("truss15ft", 6, 15),
]
TRUSS_RISE = ft_px(3.25)
TRUSS_DEPTH = ft_px(2.0)


def start_y(rise):
    """The root height that lands the tip on TIP_Y in some row: the lowest row that keeps the root
    at least 3.5 px into its block."""
    for row in range(0, 4):
        y0 = 16.0 * row + TIP_Y - rise
        if y0 >= 3.5:
            return y0
    raise AssertionError(rise)


# --------------------------------------------------------------------------- members
class Member:
    """A tube along a centreline in the x = 8 plane. `point(u)` -> (y, z); `radius(u)`. `pole`:
    the start is cut against the pole's cylinder. Both ends are capped."""

    def __init__(self, point, radius, u0, u1, pole=False, per_block=SAMPLES_PER_BLOCK):
        self.point, self.radius, self.u0, self.u1, self.pole = point, radius, u0, u1, pole
        self.per_block = per_block

    def arc(self, u):
        """Length along the centreline from u0, in blocks: the tube's v coordinate. Measured on a
        fixed table, so a band's UVs do not depend on where the pole cut starts the sweep."""
        if not hasattr(self, "_arc"):
            n = 600
            us = [self.u0 - 0.6 + (self.u1 - self.u0 + 0.6) * k / n for k in range(n + 1)]
            acc = [0.0]
            for a, b in zip(us, us[1:]):
                acc.append(acc[-1] + math.dist(self.centre(a), self.centre(b)) / 16.0)
            self._arc = (us, acc)
        us, acc = self._arc
        if u <= us[0]:
            return acc[0]
        for k in range(1, len(us)):
            if u <= us[k]:
                f = (u - us[k - 1]) / (us[k] - us[k - 1])
                return acc[k - 1] + f * (acc[k] - acc[k - 1])
        return acc[-1]

    def centre(self, u):
        y, z = self.point(u)
        return (8.0, y, z)

    def tangent(self, u):
        h = 1e-5
        a, b = self.point(u - h), self.point(u + h)
        dy, dz = b[0] - a[0], b[1] - a[1]
        n = math.hypot(dy, dz)
        return (0.0, dy / n, dz / n)

    def ring(self, u):
        c = self.centre(u)
        t = self.tangent(u)
        ny, nz = -t[2], t[1]
        r = self.radius(u)
        out = []
        for i in range(SIDES):
            a = 2.0 * math.pi * i / SIDES
            d = (math.cos(a), math.sin(a) * ny, math.sin(a) * nz)
            out.append(((c[0] + r * d[0], c[1] + r * d[1], c[2] + r * d[2]), d))
        return out


def parabola(y0, rise, run):
    """The mast arm curves' parabola: level at the tip. u is blocks from the pole's face."""
    def point(t):
        return (y0 + rise * (2.0 * t / run - t * t / (run * run)), 16.0 - 16.0 * t)
    return point


def boot_radius(member_point, r_root, r_tip, run, boot=True):
    def radius(t):
        base = r_root + (r_tip - r_root) * max(0.0, t) / run
        if not boot:
            return base
        d = POLE_Z - member_point(t)[1]       # distance from the pole's axis, px
        k = min(1.0, max(0.0, (d - 6.0) / 5.0))
        return base + (R_BOOT - r_root) * (1.0 - k) ** 1.5
    return radius


def davit_members(run, rise):
    y0 = start_y(rise)
    pt = parabola(y0, rise, run)
    return [Member(pt, boot_radius(pt, R_ROOT, R_TIP, run), -0.6, run - INSET / 16.0, pole=True)]


def truss_members(run):
    y0 = start_y(TRUSS_RISE)
    up = parabola(y0, TRUSS_RISE, run)
    yl = y0 - TRUSS_DEPTH
    tm = 2.0 * run / 3.0
    ym = up(tm)[0]

    def low(t):
        return (yl + (ym - yl) * t / tm, 16.0 - 16.0 * t)
    ts = run / 3.0
    y_s0, y_s1 = low(ts)[0], up(ts)[0]

    def strut(u):
        return (y_s0 + (y_s1 - y_s0) * u, 16.0 - 16.0 * ts)
    return [
        Member(up, boot_radius(up, TRUSS_UPPER[0], TRUSS_UPPER[1], run), -0.6, run - INSET / 16.0,
               pole=True),
        Member(low, lambda t: TRUSS_LOWER, -0.6, tm, pole=True),
        Member(strut, lambda u: TRUSS_STRUT, 0.0, 1.0, per_block=8),
    ]


# --------------------------------------------------------------------------- sweep
def cell_of(p):
    """Cell (along, up) of a point in the root frame: along counts blocks from the pole."""
    return (int(math.floor((16.0 - p[2]) / 16.0 + 1e-9)), int(math.floor(p[1] / 16.0 + 1e-9)))


def pole_cut(member, pole_r):
    """Each rail's parameter where it meets the pole's skin, walking back from outside (the first
    crossing, as the mast arm curves do -- the far side of the pole is a crossing too)."""
    ring, us = [], []
    for i in range(SIDES):
        def depth(u):
            p = member.ring(u)[i][0]
            return pole_r * pole_r - ((p[0] - POLE_X) ** 2 + (p[2] - POLE_Z) ** 2)
        u_out = member.u0 + 0.9
        assert depth(u_out) < 0, "rail starts inside the pole"
        u, u_in = u_out, None
        while u > member.u0 - 0.5:
            u -= 0.002
            if depth(u) > 0:
                u_in = u
                break
            u_out = u
        assert u_in is not None, "rail %d never reaches the pole" % i
        for _ in range(50):
            mid = (u_in + u_out) / 2.0
            if depth(mid) > 0:
                u_in = mid
            else:
                u_out = mid
        u = (u_in + u_out) / 2.0
        us.append(u)
        ring.append(member.ring(u)[i])
    return ring, us


def samples(member):
    """Uniform samples plus every cell-boundary crossing of the centreline, so no band straddles
    two cells."""
    length = member.u1 - member.u0
    n = max(4, int(math.ceil(member.per_block * length)))
    us = [member.u0 + length * k / n for k in range(n + 1)]
    out = [us[0]]
    for a, b in zip(us, us[1:]):
        lo = a
        while cell_of(member.centre(lo)) != cell_of(member.centre(b)):
            ca = cell_of(member.centre(lo))
            x0, x1 = lo, b
            for _ in range(60):
                mid = (x0 + x1) / 2.0
                if cell_of(member.centre(mid)) == ca:
                    x0 = mid
                else:
                    x1 = mid
            if x1 - out[-1] > 1e-4:
                out.append(x1)
            lo = x1 + 1e-7
            if lo >= b:
                break
        if b - out[-1] > 1e-4:
            out.append(b)
    return out


class Mesh:
    def __init__(self):
        self.v, self.vt, self.vn, self.f = [], [], [], []
        self._i = [{}, {}, {}]

    def _add(self, k, store, item):
        key = tuple(round(c, 6) for c in item)
        if key not in self._i[k]:
            store.append(item)
            self._i[k][key] = len(store)
        return self._i[k][key]

    def tri(self, verts):
        self.f.append([(self._add(0, self.v, p), self._add(1, self.vt, t), self._add(2, self.vn, n))
                       for p, t, n in verts])

    def same_as(self, other):
        return ([tuple(round(c, 6) for c in p) for p in self.v]
                == [tuple(round(c, 6) for c in p) for p in other.v] and self.f == other.f)

    def bounds(self):
        return ([min(p[k] for p in self.v) for k in range(3)],
                [max(p[k] for p in self.v) for k in range(3)])

    def transformed(self, fn, nfn):
        m = Mesh()
        m.v = [fn(p) for p in self.v]
        m.vt = list(self.vt)
        m.vn = [nfn(n) for n in self.vn]
        m.f = [list(t) for t in self.f]
        return m

    def text(self, name, header):
        out = io.StringIO()
        out.write("# Generated by dev-env-utils/scripts/gen_luminaire_arms.py -- do not edit\n")
        out.write("# %s\nmtllib %s\no %s\n" % (header, MTL_NAME, name))
        for p in self.v:
            out.write("v %.6f %.6f %.6f\n" % tuple(c / 16.0 for c in p))
        for t in self.vt:
            out.write("vt %.6f %.6f\n" % t)
        for n in self.vn:
            out.write("vn %.6f %.6f %.6f\n" % n)
        out.write("usemtl %s\n" % MATERIAL)
        for t in self.f:
            out.write("f %s\n" % " ".join("%d/%d/%d" % i for i in t))
        return out.getvalue()


def pingpong(x):
    x = x % 2.0
    return x if x <= 1.0 else 2.0 - x


def uv(u, v):
    return (0.25 + 0.5 * pingpong(u), 1.0 - (0.25 + 0.5 * pingpong(v)))


def tube_band(mesh, ra, rb, va, vb):
    for i in range(SIDES):
        j = (i + 1) % SIDES
        ua, ub = i / SIDES, (i + 1) / SIDES
        a, b, c, d = ra[i], ra[j], rb[j], rb[i]
        mesh.tri([(a[0], uv(ua, va), a[1]), (d[0], uv(ua, vb), d[1]), (c[0], uv(ub, vb), c[1])])
        mesh.tri([(a[0], uv(ua, va), a[1]), (c[0], uv(ub, vb), c[1]), (b[0], uv(ub, va), b[1])])


def cap(mesh, rng, outward):
    """A fan closing a ring, wound to face `outward` (a unit vector)."""
    n = len(rng)
    cen = tuple(sum(p[0][k] for p in rng) / n for k in range(3))
    for i in range(n):
        j = (i + 1) % n
        a, b = rng[i][0], rng[j][0]
        e1 = [a[k] - cen[k] for k in range(3)]
        e2 = [b[k] - cen[k] for k in range(3)]
        cr = (e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0])
        if math.sqrt(sum(c * c for c in cr)) < 1e-9:
            continue
        if sum(cr[k] * outward[k] for k in range(3)) < 0:
            a, b = b, a
        mesh.tri([(cen, uv(0.5, 0.5), outward), (a, uv(0.2, 0.3), outward), (b, uv(0.8, 0.3), outward)])


def sweep(members, pole_r):
    """Every member's bands, assigned to cells: {cell: Mesh} in the root frame, and the cell order
    (along the arm first, then up)."""
    cells = {}

    def mesh_for(c):
        if c not in cells:
            cells[c] = Mesh()
        return cells[c]

    for member in members:
        us = samples(member)
        rings = []
        if member.pole:
            cut, cut_us = pole_cut(member, pole_r)
            first = max(cut_us)
            us = [u for u in us if u > first + 1e-4]
            rings.append((cut, min(cut_us), first))
        rings += [(member.ring(u), u, u) for u in us]
        prev_c = None
        for k in range(len(rings) - 1):
            ra, ua0, ua1 = rings[k]
            rb, ub, _ = rings[k + 1]
            mid = member.centre((ua1 + ub) / 2.0)
            c = cell_of(mid)
            c = (max(c[0], 0), c[1])
            tube_band(mesh_for(c), ra, rb, member.arc(ua1), member.arc(ub))
            if k == 0:
                start_c = c
            prev_c = c
        tan0 = member.tangent(member.u0 if not member.pole else rings[0][1])
        cap(mesh_for(start_c), rings[0][0], tuple(-t for t in tan0))
        cap(mesh_for(prev_c), rings[-1][0], member.tangent(rings[-1][1]))
    # The root -- the block placed against the pole, where the upper member leaves it -- is cell
    # 0 at (0, 0): placement lays every other cell out from it by offset.
    assert (0, 0) in cells, "the root cell holds no geometry"
    order = [(0, 0)] + sorted((c for c in cells if c != (0, 0)), key=lambda c: (c[0], c[1]))
    return cells, order


def local(mesh, c):
    """A cell's mesh moved into its own block: cell (along, up) covers z 16-16(along+1)..16-16along
    and y 16up..16up+16."""
    ci, cj = c
    return mesh.transformed(lambda p: (p[0], p[1] - 16.0 * cj, p[2] + 16.0 * ci), lambda n: n)


def mirrored(mesh):
    """The cell turned half round about its block's vertical axis: the second arm of a double is
    the first one turned about the pole, which turns each of its cells about its own centre."""
    return mesh.transformed(lambda p: (16.0 - p[0], p[1], 16.0 - p[2]), lambda n: (-n[0], n[1], -n[2]))


def box_of(meshes):
    lo = [min(m.bounds()[0][k] for m in meshes) for k in range(3)]
    hi = [max(m.bounds()[1][k] for m in meshes) for k in range(3)]
    return [round(min(1.0, max(0.0, v / 16.0)), 5) for v in lo + hi]


# --------------------------------------------------------------------------- profiles
def build(pid, members, double):
    """Returns the profile: cell list, per-cell meshes (LARGE), fit variants, boxes, root sides."""
    per_fit = [sweep(members, r) for _, r in FITS]
    cells_l, order = per_fit[0]
    for cells_f, order_f in per_fit[1:]:
        assert order_f == order, "%s: a fit changed the cell list" % pid
    meshes = [local(cells_l[c], c) for c in order]
    variants = []        # (cell index, [mesh per non-large fit])
    for idx, c in enumerate(order):
        fits = [local(cells_f[c], c) for cells_f, _ in per_fit[1:]]
        if any(not f.same_as(meshes[idx]) for f in fits):
            variants.append((idx, fits))
    sides = {idx: 1 for idx, _ in variants}
    offsets = [list(c) for c in order]
    boxes = [box_of([meshes[i]] + dict(variants).get(i, [])) for i in range(len(order))]
    if double:
        n = len(order)
        offsets += [[-2 - c[0], c[1]] for c in order]
        meshes = meshes + [mirrored(m) for m in meshes]
        variants = variants + [(n + i, [mirrored(f) for f in fits]) for i, fits in variants]
        for i, _ in list(variants):
            sides[i] = 1 if i < n else -1
        boxes += [[round(1.0 - b[3], 5), b[1], round(1.0 - b[5], 5), round(1.0 - b[0], 5), b[4],
                   round(1.0 - b[2], 5)] for b in boxes]
    whole = Mesh()
    for idx, m in enumerate(meshes):
        ci, cj = offsets[idx]
        part = m.transformed(lambda p, ci=ci, cj=cj: (p[0], p[1] + 16.0 * cj, p[2] - 16.0 * ci),
                             lambda q: q)
        base = len(whole.v)
        tbase, nbase = len(whole.vt), len(whole.vn)
        whole.v += part.v
        whole.vt += part.vt
        whole.vn += part.vn
        whole.f += [[(a + base, b + tbase, c + nbase) for a, b, c in t] for t in part.f]
    return {"id": pid, "offsets": offsets, "meshes": meshes, "variants": variants,
            "sides": sides, "boxes": boxes, "whole": whole}


# City of Anaheim Special Design #749 (Anaheim Resort Area): an 8 ft arm whose tip stands 3'3" over
# the pole top (26'4" against the 23'1" pole), with cast scroll work under its root. The arm is a
# slimmer tube than the Caltrans davit; the scroll is a brace from the pole, lower down, rising into
# the arm, with a curl hung inside the angle between them.
ANAHEIM_RUN, ANAHEIM_RISE = 3, ft_px(3.25)
ANAHEIM_R = (1.6, 1.2)


def anaheim_members():
    y0 = start_y(ANAHEIM_RISE)
    up = parabola(y0, ANAHEIM_RISE, ANAHEIM_RUN)
    t_end = 0.85
    y_end = up(t_end)[0] - 1.0
    yl = y0 - 9.0

    def brace(t):
        k = min(1.0, max(0.0, t / t_end))
        k = k * k * (3.0 - 2.0 * k)
        return (yl + (y_end - yl) * k, 16.0 - 16.0 * t)

    cy, cz = yl + 4.2, 16.0 - 16.0 * 0.42

    def curl(u):
        a = math.radians(-70.0 - 430.0 * u)
        r = 2.6 - 1.7 * u
        return (cy + r * math.sin(a), cz + r * math.cos(a))

    return [
        Member(up, boot_radius(up, ANAHEIM_R[0], ANAHEIM_R[1], ANAHEIM_RUN), -0.6,
               ANAHEIM_RUN - INSET / 16.0, pole=True),
        Member(brace, lambda t: 0.6, -0.6, t_end + 0.05, pole=True),
        Member(curl, lambda u: 0.45, 0.0, 1.0, per_block=20),
    ]


# Los Angeles' 1950s-60s upsweep davits (the BSL museum's black pole in frame 02, a rusted one in
# the requester's photos): the arm leaves the pole steeply, well below its top, and sweeps up and
# out to a long, nearly level reach; a thin tie rod from lower on the pole holds it about halfway
# out, bowed (it climbs the pole before it reaches out). Drawn at the Caltrans arms' scale, the
# rise a judgement from the photos.
LA_UPSWEEP = [
    ("laupsweep8ft", 3, ft_px(3.5), 8),
    ("laupsweep10ft", 4, ft_px(4.0), 10),
    ("laupsweep12ft", 5, ft_px(4.25), 12),     # 4'3" keeps the root in the placed block
]
LA_ROD_DROP = 20.0          # px below the arm's root where the tie rod leaves the pole
LA_ROD_R = 0.45


def la_upsweep_members(run, rise):
    y0 = start_y(rise)
    up = parabola(y0, rise, run)
    tm = 0.45 * run
    ym = up(tm)[0] - 0.8
    yl = y0 - LA_ROD_DROP

    def rod(t):
        k = max(0.0, t) / tm
        return (yl + (ym - yl) * k ** 0.6, 16.0 - 16.0 * t)
    return [
        Member(up, boot_radius(up, R_ROOT, R_TIP, run), -0.6, run - INSET / 16.0, pole=True),
        Member(rod, lambda t: LA_ROD_R, -0.6, tm, pole=True),
    ]


# Los Angeles' Van Nuys Special (BSL CD-955, early 1970s, Van Nuys Boulevard from Burbank Boulevard
# to Sherman Way; CD-853 on Westwood Boulevard is its twin): one pole parting at a single point
# into three slender davits that sweep up and out over the road in one plane, nested: the inner
# arm the steepest, ending highest and nearest, the outer reaching farthest and ending lowest, a
# cobra head on each. Each tip lands on TIP_Y of its own row, one row and one block apart, so three
# street lights fit, each against its own tip.
VANNUYS_Y0 = 6.0
VANNUYS_ARMS = [(3, 3), (4, 2), (5, 1)]      # (run in blocks, the row its tip lands in)
VANNUYS_R = (1.7, 1.3)


def van_nuys_members():
    out = []
    for run, row in VANNUYS_ARMS:
        rise = 16.0 * row + TIP_Y - VANNUYS_Y0
        pt = parabola(VANNUYS_Y0, rise, run)
        # Three arms in one block's width: fewer rings each than a lone davit gets.
        out.append(Member(pt, boot_radius(pt, VANNUYS_R[0], VANNUYS_R[1], run), -0.6,
                          run - INSET / 16.0, pole=True, per_block=8))
    return out


def catalogue():
    out = []
    for pid, run, rise, ft in CALTRANS:
        for double in (False, True):
            label = "Caltrans %sLuminaire Arm %d ft" % ("Double " if double else "", ft)
            out.append((pid + ("double" if double else ""), lambda run=run, rise=rise: davit_members(run, rise),
                        double, label, ft, run, rise))
    for pid, run, ft in TRUSS:
        for double in (False, True):
            label = "%sTruss Luminaire Arm %d ft" % ("Double " if double else "", ft)
            out.append((pid + ("double" if double else ""), lambda run=run: truss_members(run), double,
                        label, ft, run, TRUSS_RISE))
    for pid, run, rise, ft in LA_UPSWEEP:
        out.append((pid, lambda run=run, rise=rise: la_upsweep_members(run, rise), False,
                    "Los Angeles Upsweep Luminaire Arm %d ft" % ft, ft, run, rise))
    out.append(("anaheim8ft", anaheim_members, False, "Anaheim Resort Area Luminaire Arm 8 ft", 8,
                ANAHEIM_RUN, ANAHEIM_RISE))
    out.append(("vannuystriple", van_nuys_members, False, "Van Nuys Special Triple Luminaire Arm", 15,
                VANNUYS_ARMS[-1][0], 16.0 * VANNUYS_ARMS[0][1] + TIP_Y - VANNUYS_Y0))
    return out


def registry(pid, finish):
    return "luminairearm%s%s" % (pid, finish)


def model_name(pid, idx, fit=None):
    return "luminairearm_%s_c%d%s.obj" % (pid, idx, "_" + fit if fit else "")


def blockstate(prof, texture):
    pid = prof["id"]
    n = len(prof["offsets"])
    shape = {}
    for i in range(n):
        shape[str(i)] = {"model": "%s/%s" % (MODEL_PREFIX, model_name(pid, i))}
    for k, (idx, _fits) in enumerate(prof["variants"]):
        for f, (fit, _r) in enumerate(FITS[1:]):
            shape[str(n + 2 * k + f)] = {"model": "%s/%s" % (MODEL_PREFIX, model_name(pid, idx, fit))}
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "%s/%s" % (MODEL_PREFIX, model_name(pid, 0)),
            "custom": {"flip-v": True},
            "textures": {"#" + MATERIAL: texture, "particle": texture},
        },
        "variants": {
            "shape": shape,
            "facing": {"north": {"y": 0}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}},
            "inventory": [{"model": "%s/luminairearm_%s_inv.obj" % (MODEL_PREFIX, pid),
                           "transform": "forge:default-block"}],
        },
    }


def inventory_mesh(whole):
    lo, hi = whole.bounds()
    span = max(hi[k] - lo[k] for k in range(3))
    off = [lo[k] - (span - (hi[k] - lo[k])) / 2.0 for k in range(3)]
    return whole.transformed(lambda p: tuple((p[k] - off[k]) * 16.0 / span for k in range(3)), lambda q: q)


def java_enum(profiles):
    body = []
    for i, (meta, prof) in enumerate(profiles):
        pid, _b, double, label, ft, run, rise = meta
        cells = ", ".join("{%d, %d}" % tuple(c) for c in prof["offsets"])
        boxes = ",\n          ".join("{%s}" % ", ".join("%.5fD" % v for v in b) for b in prof["boxes"])
        fit_cells = ", ".join(str(idx) for idx, _ in prof["variants"])
        sides = ", ".join(str(prof["sides"][idx]) for idx, _ in prof["variants"])
        body.append(
            "  /** %s: %d of run, %d cells. */\n"
            "  %s(\"%s\",\n"
            "      new int[][]{%s},\n"
            "      new double[][]{%s},\n"
            "      new int[]{%s}, new int[]{%s})%s"
            % (label, run, len(prof["offsets"]), pid.upper(), label, cells, boxes, fit_cells, sides,
               "," if i < len(profiles) - 1 else ";"))
    return JAVA_TEMPLATE % {"body": "\n".join(body)}


JAVA_TEMPLATE = '''package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmPoleFit;

/**
 * The luminaire arm sizes (Caltrans davit arms and truss arms, single and double), and the exact
 * block cells each one occupies.
 *
 * <p><b>Generated by {@code dev-env-utils/scripts/gen_luminaire_arms.py}. Do not edit by
 * hand.</b> The cell lists are the lists the generator split the OBJ geometry on, and the boxes
 * are measured off it, as for {@link MastArmCurveProfile}.
 *
 * <p>Each cell is {@code {alongArm, up}} in blocks from the placed root, unrotated; a double arm's
 * second arm sits at negative {@code alongArm}, past the pole. The arms carry no mount hardware,
 * so a {@code shape} value is a slot alone: the cell index, or, for a cell holding a pole joint on
 * a narrower pole, a slot past the last cell.
 */
public enum LuminaireArmProfile implements ICurvedArmProfile {

%(body)s

  private final String label;
  private final int[][] cells;
  private final double[][] boxes;
  private final int[] fitCells;
  private final int[] poleSides;

  LuminaireArmProfile(String label, int[][] cells, double[][] boxes, int[] fitCells,
      int[] poleSides) {
    this.label = label;
    this.cells = cells;
    this.boxes = boxes;
    this.fitCells = fitCells;
    this.poleSides = poleSides;
  }

  @Override
  public int getCellCount() {
    return cells.length;
  }

  @Override
  public int[] getCellOffset(int index) {
    return cells[index];
  }

  @Override
  public double[] getCellBox(int index) {
    return boxes[index];
  }

  @Override
  public int getShapeCount() {
    return cells.length + 2 * fitCells.length;
  }

  @Override
  public int poleSide(int cell) {
    for (int k = 0; k < fitCells.length; k++) {
      if (fitCells[k] == cell) {
        return poleSides[k];
      }
    }
    return 0;
  }

  @Override
  public int shapeIndex(int cell, int mountMask, CsmPoleFit fit) {
    if (fit != CsmPoleFit.LARGE) {
      for (int k = 0; k < fitCells.length; k++) {
        if (fitCells[k] == cell) {
          return cells.length + 2 * k + fit.ordinal() - 1;
        }
      }
    }
    return cell;
  }

  @Override
  public String describe() {
    return label;
  }
}
'''


def mtl_text():
    return ("# Generated by dev-env-utils/scripts/gen_luminaire_arms.py -- do not edit\n"
            "# One material: every finish's blockstate overrides #body.\n"
            "newmtl %s\nmap_Kd %s\n" % (MATERIAL, FINISHES[0][1]))


def outputs():
    """Every file the script owns: (tree-relative kind, relative path) -> text."""
    files = {("asset", MODEL_REL + "/" + MTL_NAME): mtl_text()}
    profiles = []
    for meta in catalogue():
        pid, builder, double, label, ft, run, rise = meta
        prof = build(pid, builder(), double)
        for i, m in enumerate(prof["meshes"]):
            files[("asset", MODEL_REL + "/" + model_name(pid, i))] = m.text(
                "luminairearm_%s_c%d" % (pid, i), "%s, cell %d at %s" % (label, i, prof["offsets"][i]))
        for idx, fits in prof["variants"]:
            for (fit, r), m in zip(FITS[1:], fits):
                files[("asset", MODEL_REL + "/" + model_name(pid, idx, fit))] = m.text(
                    "luminairearm_%s_c%d_%s" % (pid, idx, fit),
                    "%s, cell %d on the %s pole (radius %.0f)" % (label, idx, fit, r))
        files[("asset", MODEL_REL + "/luminairearm_%s_inv.obj" % pid)] = inventory_mesh(
            prof["whole"]).text("luminairearm_%s_inv" % pid, "%s, inventory icon" % label)
        for fid, tex, _flabel in finishes_of(pid):
            files[("asset", "blockstates/%s.json" % registry(pid, fid))] = json.dumps(
                blockstate(prof, tex), indent=2) + "\n"
        profiles.append((meta, prof))
    files[("java", JAVA_REL)] = java_enum(profiles)
    return files, profiles


def path_of(kind, rel):
    if kind == "asset":
        return layout.asset_for_write(OWNER, rel)
    return layout.source_for_write(OWNER, rel)


def fragments(profiles):
    for meta, prof in profiles:
        pid, _b, _d, label, *_ = meta
        for fid, _t, flabel in finishes_of(pid):
            print('    initTabBlock(new BlockTrafficPoleMastArmCurve("%s", LuminaireArmProfile.%s));'
                  % (registry(pid, fid), pid.upper()))
    for meta, prof in profiles:
        pid, _b, _d, label, *_ = meta
        for fid, _t, flabel in finishes_of(pid):
            print("tile.%s.name=%s (%s)" % (registry(pid, fid), label, flabel))


def main(argv):
    files, profiles = outputs()
    if "--fragments" in argv:
        fragments(profiles)
        return 0
    check = "--check" in argv
    model_dir = os.path.dirname(path_of("asset", MODEL_REL + "/" + MTL_NAME))
    bs_dir = os.path.dirname(path_of("asset", "blockstates/x.json"))
    owned = {os.path.normcase(os.path.abspath(path_of(k, r))) for k, r in files}
    stale = [os.path.join(model_dir, f) for f in os.listdir(model_dir) if f.startswith("luminairearm_")]
    stale += [os.path.join(bs_dir, f) for f in os.listdir(bs_dir) if f.startswith("luminairearm")]
    stale = [p for p in stale if os.path.normcase(os.path.abspath(p)) not in owned]
    drift = []
    for (kind, rel), text in files.items():
        path = path_of(kind, rel)
        data = text.encode("utf-8")
        if check:
            if not os.path.exists(path) or open(path, "rb").read().replace(b"\r\n", b"\n") != data:
                drift.append(rel)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as fh:
            fh.write(data)
    if check:
        drift += [os.path.basename(p) + " (stale)" for p in stale]
        if drift:
            print("gen_luminaire_arms: %d file(s) drifted:\n  %s" % (len(drift), "\n  ".join(drift)))
            return 1
        print("gen_luminaire_arms: up to date")
        return 0
    for p in stale:
        os.remove(p)
    for meta, prof in profiles:
        tris = sum(len(m.f) for m in prof["meshes"])
        print("  %-22s %2d cells  %4d tris  pole-joint cells %s"
              % (meta[0], len(prof["offsets"]), tris, [i for i, _ in prof["variants"]]))
    print("wrote %d files" % len(files))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
