#!/usr/bin/env python3
"""
Generates the truss signal mast arms (GitHub #99): the two-chord signal arms of Massachusetts, New
Hampshire and New Jersey, where a monotube mast arm would be used elsewhere.

WHAT A TRUSS SIGNAL ARM IS
--------------------------
Two pipes leave the pole one above the other and meet at the tip, with a few vertical struts
between them. The whole arm is the truss: there is no upsweep handing off to a horizontal arm, so
one block places the entire arm, pole to tip. Three shapes, from the requester's photos:

  level     the top chord is straight and level, the bottom chord climbs from lower on the pole to
            meet it at the tip (Boston, MassDOT). Every signal head sits at one height.
  sloped    both chords climb away from the pole and converge at the tip (New Jersey).
  diagonal  a level-top arm that leaves the pole at 45 degrees in plan, across the corner of an
            intersection, on the same lattice as the angled thin traffic pole
            (`trafficpolehorizontalangle*`): one cell per diagonal step.

HOW IT IS BUILT
---------------
The mast arm curves' way (MAST_ARM_CURVE_SYSTEM.md): one real block per cell the arm passes
through, all placed or none, the cell index in a tile entity, and the cell list and hitboxes
generated together with the geometry. The block is BlockTrafficPoleMastArmCurve, driven through
ICurvedArmProfile by the enum this script writes (TrussSignalArmProfile).

Every member is a straight pipe, so a member is cut only where it crosses a cell boundary: one
band per member per cell, which is why a 12 block arm is a few hundred triangles.

THE POLE JOINT
--------------
Both chords are narrower than every CSM pole, so each is saddle-cut against the pole's own
cylinder, once per CsmPoleFit (LARGE 6, THIN 4, PEDESTAL 3), and wears a clamp collar round the
pole there. Only the two cells holding a chord's pole end differ between fits.

A diagonal arm's pole is not behind its first cell but diagonally behind it, and its first cell is
diagonal to the pole, where nothing can be clicked. So a diagonal arm is placed against a pole's
face like everything else, and the block then moves itself one step to the right: placed on the
north face it reaches north-east, on the east face south-east, and so on round. The profile says
so by giving cell 0 a non-zero offset and by `getPoleOffset`.

MOUNT HARDWARE
--------------
A signal head, sign or camera against a cell grows a band, a bracket and a face plate toward it,
as on a pole and on the mast arm curves. Which sides a cell can grow hardware on is per cell (a
side that is another cell of the same arm never can), so the `shape` property is packed per cell:
`base[cell] + fit * 2^n + mask`, where the mask holds only that cell's own sides. That keeps the
block state count to what the arm can actually show. A stub hangs off whichever chord is nearest
the face it reaches, so a sign under the arm hangs from the bottom chord.

OUTPUT
------
  models/block/trafficaccessories/shared_models/trusssignalarm_<id>_c<n>.obj        one per cell
  models/block/trafficaccessories/shared_models/trusssignalarm_<id>_c<n>_<fit>.obj  pole-joint cells
  models/block/trafficaccessories/shared_models/trusssignalarm_stub_<n>.obj         mount hardware,
                                                       shared wherever two cells' stubs agree
  models/block/trafficaccessories/shared_models/trusssignalarm_<id>_inv.obj         inventory icon
  models/block/trafficaccessories/shared_models/trusssignalarm.mtl
  blockstates/trusssignalarm<id><finish>.json
  src/main/java/.../trafficaccessories/TrussSignalArmProfile.java                    (generated enum)

Run:  python dev-env-utils/scripts/gen_truss_signal_arms.py [--check] [--fragments]
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
JAVA_REL = "trafficaccessories/TrussSignalArmProfile.java"
MTL_NAME = "trusssignalarm.mtl"
MATERIAL = "body"

# (finish id, texture, label). Galvanized steel is the pole family's "unpainted" texture.
FINISHES = [
    ("galvanized", "csm:blocks/trafficsignals/shared_textures/metal_white", "Galvanized"),
    ("silver", "csm:blocks/trafficsignals/shared_textures/metal_silver", "Silver"),
    ("black", "csm:blocks/trafficsignals/shared_textures/metal_black", "Black"),
]

INSET = 0.01                # keeps the tip cap off the block boundary plane
CHORD_SIDES = 10
STRUT_SIDES = 6

# The pole in the placement frame: centred on the block behind the placed one, axis at x 8, z 24.
POLE_X, POLE_Z = 8.0, 24.0
# CsmPoleFit's order and radii; the ordinal is what the shape slot is computed from.
FITS = [("large", 6.0), ("thin", 4.0), ("pedestal", 3.0)]

# Pipe radii (px). The top chord is the one signal heads clamp to.
R_TOP, R_LOW, R_STRUT = 2.6, 2.1, 1.25
# How far below the top chord the bottom chord leaves the pole (px, centre to centre).
DEPTH_LEVEL = 20.0
DEPTH_SLOPED = 22.0
# Where the bottom chord ends, measured below the top chord's centre: far enough inside the top
# chord that its end cap is buried.
WELD_DROP = 0.4
# The top chord's height in its row. Level arms sit centred, as `trafficpolehorizontal` does, so a
# signal head against one lines up as it does on a monotube arm.
Y_LEVEL = 8.0
# A sloped arm starts a pixel low so that no chord crosses a row and a column boundary at once.
Y_SLOPED = 7.0

COLLAR_STANDOFF = 0.55      # the clamp collar's radius beyond the pole's
COLLAR_MARGIN = 0.9         # the collar reaches this far above and below the chord it holds
COLLAR_SIDES = 14

# Mount directions, in bit order. (name, {along, up, side} of the neighbour, face centre, normal).
# The first four are MastArmCurveProfile's MOUNT_* bits, in its order.
DIRECTIONS = [
    ("down", (0, -1, 0), (8.0, 0.0, 8.0), (0.0, -1.0, 0.0)),
    ("up", (0, 1, 0), (8.0, 16.0, 8.0), (0.0, 1.0, 0.0)),
    ("east", (0, 0, 1), (16.0, 8.0, 8.0), (1.0, 0.0, 0.0)),
    ("west", (0, 0, -1), (0.0, 8.0, 8.0), (-1.0, 0.0, 0.0)),
    ("ahead", (1, 0, 0), (8.0, 8.0, 0.0), (0.0, 0.0, -1.0)),
    ("back", (-1, 0, 0), (8.0, 8.0, 16.0), (0.0, 0.0, 1.0)),
]
BAND_MARGIN = 0.7           # how far the clamp band stands off the pipe it grips
BAND_LEN = 2.6
BRACKET_R = 1.2
PLATE_R = 2.8
PLATE_LEN = 1.0

# (id, kind, size, label). `size` is the run in blocks, or the diagonal steps.
CATALOGUE = [
    ("level6", "level", 6, "Truss Signal Mast Arm, Level Top, 6 Blocks"),
    ("level8", "level", 8, "Truss Signal Mast Arm, Level Top, 8 Blocks"),
    ("level10", "level", 10, "Truss Signal Mast Arm, Level Top, 10 Blocks"),
    ("level12", "level", 12, "Truss Signal Mast Arm, Level Top, 12 Blocks"),
    ("sloped8", "sloped", 8, "Truss Signal Mast Arm, Sloped, 8 Blocks"),
    ("sloped12", "sloped", 12, "Truss Signal Mast Arm, Sloped, 12 Blocks"),
    ("diagonal5", "diagonal", 5, "Truss Signal Mast Arm, Diagonal, 5 Blocks"),
    ("diagonal8", "diagonal", 8, "Truss Signal Mast Arm, Diagonal, 8 Blocks"),
]
# A sloped arm's rise over its run, in blocks: about 7 to 9 degrees, as the New Jersey arms climb.
SLOPED_RISE = {8: 1.0, 12: 2.0}


# --------------------------------------------------------------------------- vectors
def vsub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def vadd(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def vmul(a, k):
    return (a[0] * k, a[1] * k, a[2] * k)


def vdot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def vlen(a):
    return math.sqrt(vdot(a, a))


def vnorm(a):
    n = vlen(a) or 1e-9
    return (a[0] / n, a[1] / n, a[2] / n)


def vcross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def lerp(a, b, u):
    return (a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u, a[2] + (b[2] - a[2]) * u)


# --------------------------------------------------------------------------- mesh
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

    def tri(self, a, b, c, outward):
        """A triangle wound to face `outward`: (position, uv, normal) per corner. Every face here
        has an obvious outward side, and none is worth reasoning about winding by hand."""
        cr = vcross(vsub(b[0], a[0]), vsub(c[0], a[0]))
        if vlen(cr) < 1e-9:
            return
        if vdot(cr, outward) < 0.0:
            b, c = c, b
        self.f.append([(self._add(0, self.v, p), self._add(1, self.vt, t), self._add(2, self.vn, n))
                       for p, t, n in (a, b, c)])

    def empty(self):
        return not self.f

    def key(self):
        return (tuple(tuple(round(c, 5) for c in p) for p in self.v), tuple(map(tuple, self.f)))

    def same_as(self, other):
        return self.key() == other.key()

    def bounds(self):
        return ([min(p[k] for p in self.v) for k in range(3)],
                [max(p[k] for p in self.v) for k in range(3)])

    def moved(self, shift):
        m = Mesh()
        m.v = [vadd(p, shift) for p in self.v]
        m.vt, m.vn = list(self.vt), list(self.vn)
        m.f = [list(t) for t in self.f]
        m._i = [{}, {}, {}]
        return m

    def append(self, other):
        base, tbase, nbase = len(self.v), len(self.vt), len(self.vn)
        self.v += other.v
        self.vt += other.vt
        self.vn += other.vn
        self.f += [[(a + base, b + tbase, c + nbase) for a, b, c in t] for t in other.f]

    def text(self, name, header, scale=1.0, shift=(0.0, 0.0, 0.0)):
        out = io.StringIO()
        out.write("# Generated by dev-env-utils/scripts/gen_truss_signal_arms.py -- do not edit\n")
        out.write("# %s\nmtllib %s\no %s\n" % (header, MTL_NAME, name))
        for p in self.v:
            out.write("v %.6f %.6f %.6f\n" % tuple((p[k] - shift[k]) * scale / 16.0 for k in range(3)))
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
    """Into the sprite's middle half, mirrored: a UV never leaves the sprite (an atlas does not
    tile) and never touches its border, where mip levels bleed."""
    return (0.25 + 0.5 * pingpong(u), 1.0 - (0.25 + 0.5 * pingpong(v)))


def band(mesh, ra, rb, va, vb):
    """The strip between two rings of (position, normal)."""
    n = len(ra)
    for i in range(n):
        j = (i + 1) % n
        ua, ub = i / n, (i + 1) / n
        a, b, c, d = ra[i], ra[j], rb[j], rb[i]
        out = vadd(a[1], b[1])
        mesh.tri((a[0], uv(ua, va), a[1]), (d[0], uv(ua, vb), d[1]), (c[0], uv(ub, vb), c[1]), out)
        mesh.tri((a[0], uv(ua, va), a[1]), (c[0], uv(ub, vb), c[1]), (b[0], uv(ub, va), b[1]), out)


def cap(mesh, rng, outward):
    """A fan closing a ring, facing `outward`."""
    n = len(rng)
    cen = tuple(sum(p[0][k] for p in rng) / n for k in range(3))
    for i in range(n):
        j = (i + 1) % n
        mesh.tri((cen, uv(0.5, 0.5), outward), (rng[i][0], uv(0.2, 0.3), outward),
                 (rng[j][0], uv(0.8, 0.3), outward), outward)


def frame(axis):
    """Two unit vectors square to `axis` and to each other."""
    if abs(axis[1]) < 0.99:
        side = vnorm(vcross(axis, (0.0, 1.0, 0.0)))
    else:
        side = (1.0, 0.0, 0.0)
    return side, vcross(side, axis)


def ring_at(centre, axis, r, sides):
    side, up = frame(axis)
    out = []
    for i in range(sides):
        a = 2.0 * math.pi * i / sides
        d = vadd(vmul(side, math.cos(a)), vmul(up, math.sin(a)))
        out.append((vadd(centre, vmul(d, r)), d))
    return out


def cylinder(mesh, a, b, r, sides, caps=(True, True)):
    axis = vnorm(vsub(b, a))
    ra, rb = ring_at(a, axis, r, sides), ring_at(b, axis, r, sides)
    band(mesh, ra, rb, 0.0, vlen(vsub(b, a)) / 16.0)
    if caps[0]:
        cap(mesh, ra, vmul(axis, -1.0))
    if caps[1]:
        cap(mesh, rb, axis)


# --------------------------------------------------------------------------- members
class Member:
    """A straight pipe from `a` to `b` (px, placement frame). `pole`: `a` is on the pole's axis and
    the pipe is cut where it leaves the pole's skin. `chord`: mount hardware may clamp to it."""

    def __init__(self, a, b, r, sides, pole=False, chord=False):
        self.a, self.b, self.r, self.sides, self.pole, self.chord = a, b, r, sides, pole, chord
        self.axis = vnorm(vsub(b, a))
        self.length = vlen(vsub(b, a))

    def centre(self, u):
        return lerp(self.a, self.b, u)

    def ring(self, u):
        return ring_at(self.centre(u), self.axis, self.r, self.sides)

    def crossings(self):
        """Every u in (0, 1) where the centreline crosses a cell boundary, merged where it crosses
        two at once (a diagonal arm crosses x and z together at every step)."""
        us = []
        for k in range(3):
            lo, hi = sorted((self.a[k], self.b[k]))
            if hi - lo < 1e-9:
                continue
            m = math.ceil(lo / 16.0 - 1e-9)
            while 16.0 * m < hi - 1e-9:
                u = (16.0 * m - self.a[k]) / (self.b[k] - self.a[k])
                if 1e-6 < u < 1.0 - 1e-6:
                    us.append(u)
                m += 1
        us.sort()
        out = []
        for u in us:
            if not out or (u - out[-1]) * self.length > 1e-3:
                out.append(u)
        return out


def pole_cut(member, pole_r):
    """Each rail's point on the pole's skin. A rail runs parallel to the centreline less than a
    pole radius off the axis, so its distance from the axis only grows with u: one crossing."""
    ring, us = [], []
    for i in range(member.sides):
        def outside(u):
            p = member.ring(u)[i][0]
            return (p[0] - POLE_X) ** 2 + (p[2] - POLE_Z) ** 2 - pole_r * pole_r
        lo, hi = 0.0, min(1.0, (pole_r + 6.0) / member.length)
        assert outside(lo) < 0.0 < outside(hi), "a rail does not cross the pole's skin"
        for _ in range(60):
            mid = (lo + hi) / 2.0
            if outside(mid) < 0.0:
                lo = mid
            else:
                hi = mid
        u = (lo + hi) / 2.0
        us.append(u)
        ring.append(member.ring(u)[i])
    return ring, us


def collar(mesh, y, pole_r, chord_r):
    """The clamp collar round the pole where a chord leaves it. Its end discs are inside the pole
    except for the rim that shows."""
    half = chord_r + COLLAR_MARGIN
    r = pole_r + COLLAR_STANDOFF
    cylinder(mesh, (POLE_X, y - half, POLE_Z), (POLE_X, y + half, POLE_Z), r, COLLAR_SIDES)


# --------------------------------------------------------------------------- arm shapes
def strut_positions(count_for, lo, hi):
    """`count_for` struts at cell centres spread over lo..hi (cell indices)."""
    n = count_for
    out = []
    for k in range(1, n + 1):
        c = int(round(lo + (hi - lo) * k / (n + 1.0)))
        if c not in out:
            out.append(c)
    return out


def straight_members(run, y0, rise_px, depth):
    """A level or sloped arm: t is blocks from the pole's face (z 16), the pole's axis at -0.5."""
    def top(t):
        return (8.0, y0 + rise_px * (t + 0.5) / (run + 0.5) if rise_px else y0, 16.0 - 16.0 * t)
    t_tip = run - INSET / 16.0
    t_weld = run - 0.5
    low_a = vadd(top(-0.5), (0.0, -depth, 0.0))
    low_b = vadd(top(t_weld), (0.0, -WELD_DROP, 0.0))
    members = [
        Member(top(-0.5), top(t_tip), R_TOP, CHORD_SIDES, pole=True, chord=True),
        Member(low_a, low_b, R_LOW, CHORD_SIDES, pole=True, chord=True),
    ]
    for c in strut_positions(max(2, int(round(run / 2.6))), 0, run - 1):
        t = c + 0.5
        u = (t + 0.5) / (t_weld + 0.5)
        foot = lerp(low_a, low_b, u)
        head = top(t)
        if head[1] - foot[1] > R_TOP + R_LOW + 2.0:
            members.append(Member(foot, head, R_STRUT, STRUT_SIDES))
    return members


def diagonal_members(steps, depth):
    """A level-top arm at 45 degrees in plan: q is diagonal steps from the pole's axis, cell k
    covering k - 0.5 .. k + 0.5."""
    def top(q):
        return (POLE_X + 16.0 * q, Y_LEVEL, POLE_Z - 16.0 * q)
    q_tip = steps + 0.5 - INSET / 16.0
    q_weld = steps
    low_a = vadd(top(0.0), (0.0, -depth, 0.0))
    low_b = vadd(top(q_weld), (0.0, -WELD_DROP, 0.0))
    members = [
        Member(top(0.0), top(q_tip), R_TOP, CHORD_SIDES, pole=True, chord=True),
        Member(low_a, low_b, R_LOW, CHORD_SIDES, pole=True, chord=True),
    ]
    for c in strut_positions(max(2, int(round(steps * 1.414 / 2.7))), 1, steps):
        u = c / float(q_weld)
        foot = lerp(low_a, low_b, u)
        head = top(float(c))
        if head[1] - foot[1] > R_TOP + R_LOW + 2.0:
            members.append(Member(foot, head, R_STRUT, STRUT_SIDES))
    return members


def members_of(kind, size):
    if kind == "level":
        return straight_members(size, Y_LEVEL, 0.0, DEPTH_LEVEL)
    if kind == "sloped":
        return straight_members(size, Y_SLOPED, 16.0 * SLOPED_RISE[size], DEPTH_SLOPED)
    return diagonal_members(size, DEPTH_LEVEL)


# --------------------------------------------------------------------------- cells
def cell_of(p, kind):
    """Cell (along, up, side) of a point: along counts blocks from the pole's face, side to the
    right of the arm's facing. A point still inside the pole's column belongs to the first cell."""
    along = int(math.floor((16.0 - p[2]) / 16.0 + 1e-9))
    up = int(math.floor(p[1] / 16.0 + 1e-9))
    side = int(math.floor(p[0] / 16.0 + 1e-9))
    if kind == "diagonal":
        if side < 1:
            along, side = 0, 1
    elif along < 0:
        along = 0
    return (along, up, side)


def sweep(members, pole_r, kind):
    """Every member's bands, assigned to cells: {cell: Mesh} in the placement frame, the spans of
    each chord inside each cell ({cell: [(member, u0, u1)]}), and the pole-joint cells."""
    cells, spans, joints = {}, {}, set()

    def mesh_for(c):
        if c not in cells:
            cells[c] = Mesh()
        return cells[c]

    for member in members:
        us = member.crossings()
        rings = []
        if member.pole:
            cut, cut_us = pole_cut(member, pole_r)
            first = max(cut_us)
            us = [u for u in us if (u - first) * member.length > 1e-3]
            rings.append((cut, sum(cut_us) / len(cut_us), first))
        else:
            rings.append((member.ring(0.0), 0.0, 0.0))
        rings += [(member.ring(u), u, u) for u in us + [1.0]]
        first_c = last_c = None
        for k in range(len(rings) - 1):
            ra, ua, ua_far = rings[k]
            rb, ub, _ = rings[k + 1]
            c = cell_of(member.centre((ua_far + ub) / 2.0), kind)
            band(mesh_for(c), ra, rb, ua * member.length / 16.0, ub * member.length / 16.0)
            if member.chord:
                spans.setdefault(c, []).append((member, ua_far, ub))
            if first_c is None:
                first_c = c
            last_c = c
        cap(mesh_for(first_c), rings[0][0], vmul(member.axis, -1.0))
        cap(mesh_for(last_c), rings[-1][0], member.axis)
        if member.pole:
            collar(mesh_for(first_c), member.a[1], pole_r, member.r)
            joints.add(first_c)
    return cells, spans, joints


def origin_of(c):
    """The corner of a cell in the placement frame."""
    return (16.0 * c[2], 16.0 * c[1], -16.0 * c[0])


def local(mesh, c):
    return mesh.moved(vmul(origin_of(c), -1.0))


def box_of(meshes):
    lo = [min(m.bounds()[0][k] for m in meshes) for k in range(3)]
    hi = [max(m.bounds()[1][k] for m in meshes) for k in range(3)]
    box = [min(1.0, max(0.0, v / 16.0)) for v in lo + hi]
    # Never a sliver: a cell a pipe only clips still has to be something a player can hit.
    for k in range(3):
        if box[k + 3] - box[k] < 0.125:
            mid = (box[k] + box[k + 3]) / 2.0
            box[k], box[k + 3] = max(0.0, mid - 0.0625), min(1.0, mid + 0.0625)
    return [round(v, 5) for v in box]


def stub(spans, c, direction):
    """One direction's mount hardware for one cell, in the cell's own frame: a band round the
    nearest chord at its point closest to the face, a bracket, and a plate flat on the face."""
    _name, _offset, face_centre, normal = direction
    origin = origin_of(c)
    best = None
    for member, u0, u1 in spans[c]:
        a, b = vsub(member.centre(u0), origin), vsub(member.centre(u1), origin)
        ab = vsub(b, a)
        k = min(1.0, max(0.0, vdot(vsub(face_centre, a), ab) / vdot(ab, ab)))
        # keep the band clear of the cell's ends, where the next cell's band would meet it
        margin = min(0.45, (BAND_LEN / 2.0 + 0.2) / vlen(ab))
        k = min(1.0 - margin, max(margin, k))
        p = lerp(a, b, k)
        d = vlen(vsub(face_centre, p))
        if best is None or d < best[0] - 1e-9:
            best = (d, p, member)
    dist, anchor, member = best
    mesh = Mesh()
    half = vmul(member.axis, BAND_LEN / 2.0)
    cylinder(mesh, vsub(anchor, half), vadd(anchor, half), member.r + BAND_MARGIN, 12)
    plate_inner = vadd(face_centre, vmul(normal, -PLATE_LEN))
    cylinder(mesh, plate_inner, vadd(face_centre, vmul(normal, -INSET)), PLATE_R, 8)
    if dist > member.r + BAND_MARGIN + PLATE_LEN + 0.3:
        to_face = vnorm(vsub(plate_inner, anchor))
        cylinder(mesh, vadd(anchor, vmul(to_face, member.r - 0.5)), plate_inner, BRACKET_R, 8,
                 caps=(False, False))
    return mesh


def build(pid, kind, size):
    members = members_of(kind, size)
    per_fit = [sweep(members, r, kind) for _, r in FITS]
    cells_l, spans, joints = per_fit[0]
    root = (0, 0, 1) if kind == "diagonal" else (0, 0, 0)
    assert root in cells_l, "%s: the first cell holds no geometry" % pid
    order = [root] + sorted(c for c in cells_l if c != root)
    for cells_f, _s, joints_f in per_fit[1:]:
        assert set(cells_f) == set(cells_l) and joints_f == joints, "%s: a fit changed the cells" % pid
    # Every cell must touch another by a face, or the arm is not one connected run of blocks.
    for c in order:
        if len(order) > 1:
            assert any(tuple(c[k] + d[k] for k in range(3)) in cells_l or kind == "diagonal"
                       for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0))), (pid, c)
    meshes = [local(cells_l[c], c) for c in order]
    fits = {}             # cell index -> [mesh per narrower fit]
    for idx, c in enumerate(order):
        if c in joints:
            fits[idx] = [local(cells_f[c], c) for cells_f, _s, _j in per_fit[1:]]
    boxes = [box_of([meshes[i]] + fits.get(i, [])) for i in range(len(order))]
    # Which sides each cell can grow hardware on: a cell a chord passes through, toward any side
    # that is not another cell of this arm. A cell holding only the bottom chord hangs things
    # below it; one holding the top chord takes them on every free side.
    top = members[0]
    dirs = []
    for c in order:
        has_top = any(m is top for m, _a, _b in spans.get(c, []))
        allowed = 0
        for bit, (name, off, _f, _n) in enumerate(DIRECTIONS):
            if c not in spans:
                continue
            if name in ("ahead", "back") and kind != "diagonal":
                continue
            if not has_top and name != "down":
                continue
            if tuple(c[k] + off[k] for k in range(3)) in cells_l:
                continue
            allowed |= 1 << bit
        dirs.append(allowed)
    stubs = {}            # (cell index, bit) -> Mesh
    for idx, c in enumerate(order):
        for bit, direction in enumerate(DIRECTIONS):
            if dirs[idx] & (1 << bit):
                stubs[(idx, bit)] = stub(spans, c, direction)
    # The pole a joint cell reads, as {along, side} from the cell.
    pole_at = {}
    for idx, c in enumerate(order):
        if c in joints:
            pole_at[idx] = (-1 - c[0], -c[2])
    whole = Mesh()
    for idx, c in enumerate(order):
        whole.append(meshes[idx].moved(origin_of(c)))
    # Each cell's chords as centreline segments in its own frame, for whatever hangs from the arm
    # and has to find the pipe (a street name sign's hangers): {x0, y0, z0, x1, y1, z1, radius}.
    rails = []
    for c in order:
        origin = origin_of(c)
        rails.append([vsub(member.centre(u0), origin) + vsub(member.centre(u1), origin) + (member.r,)
                      for member, u0, u1 in spans.get(c, [])])
    return {"id": pid, "kind": kind, "size": size, "order": order, "meshes": meshes, "fits": fits,
            "boxes": boxes, "dirs": dirs, "stubs": stubs, "pole_at": pole_at, "whole": whole,
            "rails": rails}


def shape_layout(prof):
    """Per cell: (base, bits). A cell's block of shape values is `fits * 2^bits` long."""
    out, base = [], 0
    for idx in range(len(prof["order"])):
        bits = bin(prof["dirs"][idx]).count("1")
        out.append((base, bits))
        base += (len(FITS) if idx in prof["fits"] else 1) << bits
    return out, base


# --------------------------------------------------------------------------- output
def registry(pid, finish):
    return "trusssignalarm%s%s" % (pid, finish)


def cell_model(pid, idx, fit=None):
    return "trusssignalarm_%s_c%d%s.obj" % (pid, idx, "_" + fit if fit else "")


class StubPool:
    """Stub models shared across cells and arms: most of a level arm's are the same mesh."""

    def __init__(self):
        self.names, self.meshes = {}, []

    def name(self, mesh):
        key = mesh.key()
        if key not in self.names:
            self.names[key] = "trusssignalarm_stub_%d.obj" % len(self.meshes)
            self.meshes.append(mesh)
        return self.names[key]


def blockstate(prof, texture, pool):
    pid = prof["id"]
    layout_, _count = shape_layout(prof)
    shape = {}
    for idx, (base, bits) in enumerate(layout_):
        own = [b for b in range(len(DIRECTIONS)) if prof["dirs"][idx] & (1 << b)]
        fit_names = [None] + [f for f, _r in FITS[1:]] if idx in prof["fits"] else [None]
        for f, fit in enumerate(fit_names):
            for mask in range(1 << bits):
                variant = {"model": "%s/%s" % (MODEL_PREFIX, cell_model(pid, idx, fit))}
                sub = {}
                for k, bit in enumerate(own):
                    if mask & (1 << k):
                        sub["mount_" + DIRECTIONS[bit][0]] = {
                            "model": "%s/%s" % (MODEL_PREFIX, pool.name(prof["stubs"][(idx, bit)])),
                            "custom": {"flip-v": True},
                            "textures": {"#" + MATERIAL: texture},
                        }
                if sub:
                    variant["submodel"] = sub
                shape[str(base + (f << bits) + mask)] = variant
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "%s/%s" % (MODEL_PREFIX, cell_model(pid, 0)),
            "custom": {"flip-v": True},
            "textures": {"#" + MATERIAL: texture, "particle": texture},
        },
        "variants": {
            "shape": shape,
            "facing": {"north": {"y": 0}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}},
            "inventory": [{"model": "%s/trusssignalarm_%s_inv.obj" % (MODEL_PREFIX, pid),
                           "transform": "forge:default-block"}],
        },
    }


def inventory_text(prof, label):
    whole = prof["whole"]
    lo, hi = whole.bounds()
    span = max(hi[k] - lo[k] for k in range(3))
    shift = tuple(lo[k] - (span - (hi[k] - lo[k])) / 2.0 for k in range(3))
    return whole.text("trusssignalarm_%s_inv" % prof["id"], "%s, inventory icon" % label,
                      scale=16.0 / span, shift=shift)


def java_enum(profiles):
    body = []
    for i, ((pid, kind, size, label), prof) in enumerate(profiles):
        layout_, count = shape_layout(prof)
        n = len(prof["order"])
        cells = ", ".join("{%d, %d, %d}" % c for c in prof["order"])
        boxes = ",\n          ".join("{%s}" % ", ".join("%.5fD" % v for v in b) for b in prof["boxes"])
        dirs = ", ".join(str(d) for d in prof["dirs"])
        bases = ", ".join(str(b) for b, _bits in layout_)
        poles = ", ".join("{%d, %d}" % prof["pole_at"][k] if k in prof["pole_at"] else "null"
                          for k in range(n))
        rails = ",\n          ".join(
            "{%s}" % ", ".join("{%s}" % ", ".join("%.3fF" % v for v in seg) for seg in cell)
            for cell in prof["rails"])
        body.append(
            "  /** %s: %d cells, %d shapes. */\n"
            "  %s(\"%s\", %d,\n"
            "      new int[][]{%s},\n"
            "      new double[][]{%s},\n"
            "      new int[]{%s},\n"
            "      new int[]{%s},\n"
            "      new int[][]{%s},\n"
            "      new float[][][]{%s})%s"
            % (label, n, count, pid.upper(), label, count, cells, boxes, dirs, bases, poles, rails,
               "," if i < len(profiles) - 1 else ";"))
    return JAVA_TEMPLATE % {"body": "\n".join(body)}


JAVA_TEMPLATE = '''package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmPoleFit;

/**
 * The truss signal mast arms (level top, sloped, and diagonal across a corner), and the exact
 * block cells each one occupies.
 *
 * <p><b>Generated by {@code dev-env-utils/scripts/gen_truss_signal_arms.py}. Do not edit by
 * hand.</b> The cell lists are the lists the generator split the OBJ geometry on, and the boxes
 * are measured off it, as for {@link MastArmCurveProfile}.
 *
 * <p>Each cell is {@code {alongArm, up, side}} in blocks from the placed position, unrotated;
 * {@code side} counts to the right of the arm's facing. A diagonal arm's first cell is not at the
 * placed position but one step to its right, diagonal to the pole.
 *
 * <p>A {@code shape} value is {@code base[cell] + fit * 2^n + mask}: {@code n} is the number of
 * sides that cell can grow mount hardware on, {@code mask} holds those sides only, in
 * {@link ICurvedArmProfile}'s direction order, and {@code fit} is non-zero only for a cell that
 * holds a chord's pole end, on a narrower pole.
 */
public enum TrussSignalArmProfile implements ICurvedArmProfile {

%(body)s

  private final String label;
  private final int shapeCount;
  private final int[][] cells;
  private final double[][] boxes;
  private final int[] mountDirections;
  private final int[] shapeBases;
  private final int[][] poleOffsets;
  private final float[][][] rails;

  TrussSignalArmProfile(String label, int shapeCount, int[][] cells, double[][] boxes,
      int[] mountDirections, int[] shapeBases, int[][] poleOffsets, float[][][] rails) {
    this.label = label;
    this.shapeCount = shapeCount;
    this.cells = cells;
    this.boxes = boxes;
    this.mountDirections = mountDirections;
    this.shapeBases = shapeBases;
    this.poleOffsets = poleOffsets;
    this.rails = rails;
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
    return shapeCount;
  }

  @Override
  public int poleSide(int cell) {
    return poleOffsets[cell] == null ? 0 : 1;
  }

  @Override
  public int[] getPoleOffset(int cell) {
    return poleOffsets[cell];
  }

  @Override
  public int getMountDirections(int cell) {
    return mountDirections[cell];
  }

  @Override
  public float[][] getCellRails(int cell) {
    return rails[cell];
  }

  @Override
  public int shapeIndex(int cell, int mountMask, CsmPoleFit fit) {
    int directions = mountDirections[cell];
    int packed = 0;
    int bits = 0;
    for (int bit = 0; bit < MOUNT_DIRECTION_COUNT; bit++) {
      if ((directions & (1 << bit)) != 0) {
        if ((mountMask & (1 << bit)) != 0) {
          packed |= 1 << bits;
        }
        bits++;
      }
    }
    int fitSlot = poleOffsets[cell] == null ? 0 : fit.ordinal();
    return shapeBases[cell] + (fitSlot << bits) + packed;
  }

  @Override
  public String describe() {
    return label;
  }
}
'''


def mtl_text():
    return ("# Generated by dev-env-utils/scripts/gen_truss_signal_arms.py -- do not edit\n"
            "# One material: every finish's blockstate overrides #body.\n"
            "newmtl %s\nmap_Kd %s\n" % (MATERIAL, FINISHES[0][1]))


def outputs():
    """Every file the script owns: (kind, tree-relative path) -> text."""
    files = {("asset", MODEL_REL + "/" + MTL_NAME): mtl_text()}
    profiles = []
    pool = StubPool()
    for meta in CATALOGUE:
        pid, kind, size, label = meta
        prof = build(pid, kind, size)
        for idx, m in enumerate(prof["meshes"]):
            files[("asset", MODEL_REL + "/" + cell_model(pid, idx))] = m.text(
                "trusssignalarm_%s_c%d" % (pid, idx),
                "%s, cell %d at %s" % (label, idx, list(prof["order"][idx])))
        for idx, fits in prof["fits"].items():
            for (fit, r), m in zip(FITS[1:], fits):
                files[("asset", MODEL_REL + "/" + cell_model(pid, idx, fit))] = m.text(
                    "trusssignalarm_%s_c%d_%s" % (pid, idx, fit),
                    "%s, cell %d on the %s pole (radius %.0f)" % (label, idx, fit, r))
        files[("asset", MODEL_REL + "/trusssignalarm_%s_inv.obj" % pid)] = inventory_text(prof, label)
        for fid, tex, _flabel in FINISHES:
            files[("asset", "blockstates/%s.json" % registry(pid, fid))] = json.dumps(
                blockstate(prof, tex, pool), indent=1) + "\n"
        profiles.append((meta, prof))
    for k, m in enumerate(pool.meshes):
        files[("asset", MODEL_REL + "/trusssignalarm_stub_%d.obj" % k)] = m.text(
            "trusssignalarm_stub_%d" % k, "mount hardware")
    files[("java", JAVA_REL)] = java_enum(profiles)
    return files, profiles


def path_of(kind, rel):
    if kind == "asset":
        return layout.asset_for_write(OWNER, rel)
    return layout.source_for_write(OWNER, rel)


def fragments(profiles):
    for (pid, _k, _s, _label), _prof in profiles:
        for fid, _t, _flabel in FINISHES:
            print('    initTabBlock(new BlockTrafficPoleMastArmCurve("%s", TrussSignalArmProfile.%s));'
                  % (registry(pid, fid), pid.upper()))
    for (pid, _k, _s, label), _prof in profiles:
        for fid, _t, flabel in FINISHES:
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
    stale = [os.path.join(model_dir, f) for f in os.listdir(model_dir) if f.startswith("trusssignalarm")]
    stale += [os.path.join(bs_dir, f) for f in os.listdir(bs_dir) if f.startswith("trusssignalarm")]
    stale = [p for p in stale if os.path.normcase(os.path.abspath(p)) not in owned]
    drift = []
    for (kind, rel), text in files.items():
        path = path_of(kind, rel)
        data = text.encode("utf-8")
        if check:
            if not os.path.exists(path):
                drift.append(rel)
            else:
                with open(path, "rb") as fh:
                    if fh.read().replace(b"\r\n", b"\n") != data:
                        drift.append(rel)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as fh:
            fh.write(data)
    if check:
        drift += [os.path.basename(p) + " (stale)" for p in stale]
        if drift:
            print("gen_truss_signal_arms: %d file(s) drifted:\n  %s" % (len(drift), "\n  ".join(drift)))
            return 1
        print("gen_truss_signal_arms: up to date")
        return 0
    for p in stale:
        os.remove(p)
    for (pid, _k, _s, _label), prof in profiles:
        _layout, count = shape_layout(prof)
        tris = sum(len(m.f) for m in prof["meshes"])
        print("  %-11s %2d cells  %4d tris  %4d shapes  pole joints at %s"
              % (pid, len(prof["order"]), tris, count, sorted(prof["pole_at"])))
    print("wrote %d files" % len(files))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
