#!/usr/bin/env python3
"""
Procedural generator for the CSM realistic mast arm curve family.

WHY THIS EXISTS
---------------
`trafficpoleverticalcurveconnector` is the only upsweep CSM has, and it is too small. That is
not a stylistic choice -- open the model and you find it pinned at `z: -16 -> 32` and
`y: -12 -> 12`, which are the hard legal limits of a Forge JSON element model. It cannot be made
bigger in that format. It is also not actually a curve: it is 130 stacked 0.75-deep slabs each
stepping 0.25 in y, i.e. a constant-slope ramp.

Real curved mast arms leave the pole low, sweep upward, and flatten to horizontal before they
reach the first signal head. The run is 10-25 ft and the rise 4-10 ft -- 2 to 6 Minecraft blocks
across and 1 to 2 up. The OBJ loader (`OBJLoader.INSTANCE.addDomain("csm")`, CsmClientProxy) has
no size limit, so the geometry is finally expressible.

MULTI-BLOCK, NOT ONE BIG MODEL
------------------------------
A single block carrying the whole sweep would be far less work, and it is the wrong answer:

  * a model is baked into the chunk section VBO of the block that owns it, so a 6-block arm
    hanging off one block vanishes whenever that block's section is frustum-culled while the
    arm's span is still on screen;
  * nothing can mount to empty air, so no signal head, sign or luminaire could ever hang off
    the curved part;
  * the auto-connecting pole system works on block adjacency, so it could never see the curve.

So each curve is a connected staircase of REAL blocks, one per cell the centerline passes
through, and each cell wears its own OBJ slice of the shared sweep. Geometry is continuous
across cells because a band is drawn whole from whichever cell owns it -- an OBJ may leave its
own block, it just must not be the only thing keeping it on screen.

THE CURVE
---------
The centerline is a parabola, which is the right family here for a non-obvious reason: a mast
arm must arrive at the horizontal run with zero slope (or the joint kinks), and must leave the
pole at a finite angle (or it leaves vertically). Writing t for blocks travelled from the pole,

    y(t) = rise * (2t/run - t^2/run^2)

is exactly the quadratic with y(0)=0, y(run)=rise, y'(run)=0. Its entry slope 2*rise/run falls
out of the size rather than being tuned per preset, so every preset is automatically consistent
and the entry angle is a derived, reportable number.

The tube tapers 10 units at the flange to 8 at the tip. 8 is not a free choice: it is the
diameter of `trafficpolehorizontal`, the arm block this thing has to hand off to. The far ring
is sheared flat onto the block boundary plane so the join is flush.

OUTPUT
------
  models/block/trafficaccessories/shared_models/mastarmcurve_<preset>_c<n>.obj   (+ _inv.obj)
  models/block/trafficaccessories/shared_models/mastarmcurve.mtl                 (one, shared)
  blockstates/trafficpolemastarmcurve<preset><color>.json                        (presets x colors)
  ../../src/main/java/.../trafficaccessories/MastArmCurveProfile.java            (generated enum)
  scratch fragments for en_us.lang and CsmTabTrafficAccessories.java

The Java enum is GENERATED, not hand-written, because the block's placement code has to know the
exact same cell list the geometry was split on. Hand-copying that is a drift bug waiting to
happen -- the same reasoning as gen_decorative_lighting.py emitting its own blockstates.

CONVENTIONS (see the csm-obj-authoring notes)
  * 1 unit == 1 block, written 0..1 with the block min corner at the origin, because blockstate
    x/y rotations pivot about (0.5, 0.5, 0.5). Authored below in Minecraft's 0..16 and divided
    at write time.
  * -Z is north. The default (facing=north) model runs the arm toward -Z; the blockstate's
    y: 90/180/270 carry it to east/south/west.
  * `custom: {"flip-v": true}`, so vt_v = 1 - V/16.
  * UVs stay strictly inside the sprite. Block textures live in an atlas, so a UV outside 0..1
    samples a NEIGHBOURING sprite rather than tiling. The metal swatches are near-flat
    (std <= 8/255), so a ping-pong wrap into the sprite's middle half keeps texel scale sane and
    never touches the border, where mipmapping would bleed.

Run:  python dev-env-utils/scripts/gen_mast_arm_curves.py
"""

import glob
import json
import math
import os

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "csm")
MODEL_DIR = os.path.join(ASSETS, "models", "block", "trafficaccessories", "shared_models")
BLOCKSTATE_DIR = os.path.join(ASSETS, "blockstates")
JAVA_DIR = os.path.join(REPO_ROOT, "src", "main", "java", "com", "micatechnologies",
                        "minecraft", "csm", "trafficaccessories")
SCRATCH_DIR = os.path.join(os.path.dirname(__file__), "_mastarm_out")

MODEL_PREFIX = "csm:trafficaccessories/shared_models"
MATERIAL = "body"
MTL_NAME = "mastarmcurve.mtl"

# --------------------------------------------------------------------------------------------
# Catalogue
# --------------------------------------------------------------------------------------------

# (id, run in blocks, rise in blocks, human label). Entry angle is derived, not chosen.
PRESETS = [
    ("4x1", 4, 1, "Local Street"),
    ("5x1", 5, 1, "Collector"),
    ("6x2", 6, 2, "Arterial"),
    ("8x2", 8, 2, "Major Intersection"),
    ("10x2", 10, 2, "Wide Arterial"),
]

# Registry suffix -> (texture, human colour). Matches the existing pole family's mapping; note
# "unpainted" wears metal_white and "white" wears metal_mattewhite, which looks backwards and is
# not -- it is what trafficpoleverticalcurveconnector{white,unpainted} already do, and breaking
# that would make a new curve not match the pole under it.
COLORS = [
    ("silver", "csm:blocks/trafficsignals/shared_textures/metal_silver", "Silver"),
    ("black", "csm:blocks/trafficsignals/shared_textures/metal_black", "Black"),
    ("tan", "csm:blocks/trafficaccessories/shared_textures/metal_mattetan", "Tan"),
    ("white", "csm:blocks/trafficsignals/shared_textures/metal_mattewhite", "White"),
    ("unpainted", "csm:blocks/trafficsignals/shared_textures/metal_white", "Unpainted"),
]

SIDES = 16           # cross-section is a hexadecagon, matching the pole family's silhouette
SAMPLES_PER_BLOCK = 12
R_FLANGE = 5.0       # tube radius in units at the pole (10 across)
R_TIP = 4.0          # tube radius at the tip -- MUST match trafficpolehorizontal's 8 across
FLANGE_MARGIN = 1.2  # how far the connection collar stands proud of the entry aperture
FLANGE_LEN = 2.1
INSET = 0.01         # keeps end caps off the block boundary plane exactly

# Two sweep samples closer than this (in blocks travelled) are the same sample. Comfortably below
# the uniform spacing of run/SAMPLES_PER_BLOCK, and comfortably above the float noise that makes a
# solved boundary crossing miss an exact one.
MERGE_TOL = 1e-4

# How far past its own block the collar reaches toward the pole.
#
# A CSM pole is a cylinder of radius 6 centred in its block, so its surface stops 2 units short
# of the block face -- an arm that ended exactly on the boundary would float a visible 2 units
# off the pole it is supposedly bolted to. 2.5 puts the collar through the pole's skin. Because
# the collar is fractionally wider than the pole (6.2 against 6.0), a thin ring of it stays
# visible around the pole, which is what a real bolted flange plate looks like.
FLANGE_REACH = 2.5


# --------------------------------------------------------------------------------------------
# Curve maths. All in Minecraft units (16 per block), in the origin cell's own frame:
# the tube enters at z = 16 (the face toward the pole) and travels toward -z.
# --------------------------------------------------------------------------------------------

def centre(t, run, rise):
    """Centreline point at t blocks travelled from the pole."""
    y = 8.0 + 16.0 * rise * (2.0 * t / run - (t * t) / (run * run))
    z = 16.0 - 16.0 * t
    return (8.0, y, z)


def tangent(t, run, rise):
    """Unit tangent, pointing away from the pole. Lies in the YZ plane, so there is no torsion
    to track and a swept frame is exact rather than approximated."""
    dy = 16.0 * rise * (2.0 / run - 2.0 * t / (run * run))
    dz = -16.0
    n = math.hypot(dy, dz)
    return (0.0, dy / n, dz / n)


def radius(t, run):
    return R_FLANGE + (R_TIP - R_FLANGE) * (t / run)


def ring(t, run, rise, r=None):
    """One cross-section: SIDES points around the tube, plus their outward normals."""
    px, py, pz = centre(t, run, rise)
    tx, ty, tz = tangent(t, run, rise)
    # In-plane normal: the tangent rotated 90 degrees within YZ. The other basis vector is X,
    # which is constant because the whole curve lives in one vertical plane.
    ny, nz = -tz, ty
    rr = radius(t, run) if r is None else r
    out = []
    for i in range(SIDES):
        a = 2.0 * math.pi * i / SIDES
        ca, sa = math.cos(a), math.sin(a)
        dirx, diry, dirz = ca, sa * ny, sa * nz
        out.append(((px + rr * dirx, py + rr * diry, pz + rr * dirz), (dirx, diry, dirz)))
    return out


def rail_z(t, i, run, rise):
    """z of one angular rail of the tube at parameter t. A rail is the path a single point of
    the cross-section traces along the sweep."""
    px, py, pz = centre(t, run, rise)
    _, ty, tz = tangent(t, run, rise)
    nz = ty  # in-plane normal's z component
    return pz + radius(t, run) * math.sin(2.0 * math.pi * i / SIDES) * nz


def clip_ring(target_z, run, rise):
    """The tube's true intersection with the plane z = target_z, as a ragged ring: every angular
    rail gets its OWN parameter, the one where that rail crosses the plane.

    The obvious alternative -- take one ring and slide it bodily onto the plane along the tangent
    -- is wrong, and wrong in a way that shows. An oblique cut through a tube is an ellipse
    stretched by 1/cos(theta), so the slid ring is much taller than the ring that follows it, and
    the surface between them folds back on itself: near-degenerate, inside-out triangles along
    the top of the arm right where it meets the pole. Clipping rail by rail gives the genuine
    aperture and leaves every quad forward-going.

    Returns (points_with_normals, t_of_each_rail)."""
    pts, ts = [], []
    for i in range(SIDES):
        lo, hi = -1.0, 1.0
        # rail_z decreases with t, so bisect on the sign of (rail_z - target).
        for _ in range(60):
            mid = (lo + hi) / 2.0
            if rail_z(mid, i, run, rise) > target_z:
                lo = mid
            else:
                hi = mid
        t = (lo + hi) / 2.0
        ts.append(t)
        pts.append(ring(t, run, rise)[i])
    return pts, ts


def sample_ts(run, rise):
    """Parameter samples for the sweep.

    Uniform samples alone are not enough: a band that straddles a cell boundary would have to be
    assigned to one cell or the other, which is how a curve ends up with a cell that owns no
    geometry or a diagonal (non-face-connected) step. Injecting the exact boundary crossings as
    samples means every band lies wholly inside one cell, so the cell list is exactly the set of
    cells the tube visits, and it is face-connected by construction.

    Boundary crossings are placed FIRST and a uniform sample within MERGE_TOL of one is dropped
    rather than added. A plain set does not do this job: at run=10 rise=2 the row crossing solves
    to t=5, which is also a column crossing and also a uniform sample, and the quadratic returns
    5.000000000000001 rather than 5. Three "identical" samples then survive as two, separated by
    1e-15, and the band between them is degenerate. Set membership is exact; nearness is what
    actually matters here."""
    forced = []

    def force(t):
        if not (1e-9 < t < run - 1e-9):
            return
        if all(abs(t - u) > MERGE_TOL for u in forced):
            forced.append(t)

    # z boundaries fall at whole blocks travelled.
    for k in range(1, run):
        force(float(k))
    # y boundaries: solve 8 + 16*rise*(2t/run - t^2/run^2) = 16*m for each interior row m.
    for m in range(1, rise + 2):
        # rise*(2t/run - t^2/run^2) = m - 0.5   ->   quadratic in t
        a = -rise / (run * run)
        b = 2.0 * rise / run
        c = 0.5 - m
        disc = b * b - 4 * a * c
        if disc < 0:
            continue
        for sgn in (-1.0, 1.0):
            # ONE sample exactly on the crossing, not a pair straddling it. A pair looks safer
            # and is not: the band between them is 2e-6 long, so its two rings collapse onto each
            # other and every triangle in it is degenerate. Landing a sample on the crossing
            # already guarantees no band spans it, because the bands then abut there.
            force((-b + sgn * math.sqrt(disc)) / (2 * a))

    ts = list(forced)
    n = SAMPLES_PER_BLOCK * run
    for i in range(n + 1):
        t = run * i / n
        if all(abs(t - u) > MERGE_TOL for u in forced):
            ts.append(t)
    return sorted(ts)


def cell_of(p):
    """Which block cell a point belongs to. Cell (ci, cj) covers z in [16-16(ci+1), 16-16ci]
    and y in [16cj, 16(cj+1)]."""
    ci = int(math.floor((16.0 - p[2]) / 16.0 + 1e-9))
    cj = int(math.floor(p[1] / 16.0 + 1e-9))
    return (ci, cj)


# --------------------------------------------------------------------------------------------
# Mesh assembly
# --------------------------------------------------------------------------------------------

class Mesh:
    """Accumulates positions/uvs/normals and triangles for one OBJ file."""

    def __init__(self):
        self.v, self.vt, self.vn, self.f = [], [], [], []
        self._vi, self._ti, self._ni = {}, {}, {}

    @staticmethod
    def _key(tpl):
        return tuple(round(c, 6) for c in tpl)

    def _add(self, store, index, tpl):
        k = self._key(tpl)
        if k not in index:
            store.append(tpl)
            index[k] = len(store)
        return index[k]

    def tri(self, verts):
        """verts: three (position, uv, normal) triples, wound counter-clockwise seen from
        outside. Forge culls back faces, so winding is not cosmetic."""
        idx = []
        for (p, uv, n) in verts:
            idx.append((self._add(self.v, self._vi, p),
                        self._add(self.vt, self._ti, uv),
                        self._add(self.vn, self._ni, n)))
        self.f.append(idx)

    def quad(self, a, b, c, d):
        self.tri([a, b, c])
        self.tri([a, c, d])

    def bounds(self):
        xs = [p[0] for p in self.v]
        ys = [p[1] for p in self.v]
        zs = [p[2] for p in self.v]
        return (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))

    def write(self, path, name, offset=(0.0, 0.0, 0.0), scale=1.0, header=""):
        ox, oy, oz = offset
        lines = ["# Procedurally generated by dev-env-utils/scripts/gen_mast_arm_curves.py"]
        if header:
            lines.append("# " + header)
        lines.append("mtllib %s" % MTL_NAME)
        lines.append("o %s" % name)
        for p in self.v:
            lines.append("v %.6f %.6f %.6f" % (((p[0] - ox) * scale) / 16.0,
                                               ((p[1] - oy) * scale) / 16.0,
                                               ((p[2] - oz) * scale) / 16.0))
        for t in self.vt:
            lines.append("vt %.6f %.6f" % t)
        for n in self.vn:
            lines.append("vn %.6f %.6f %.6f" % n)
        lines.append("usemtl %s" % MATERIAL)
        for tri in self.f:
            lines.append("f " + " ".join("%d/%d/%d" % i for i in tri))
        with open(path, "w", newline="\n") as fh:
            fh.write("\n".join(lines) + "\n")


def pingpong(x):
    """Triangle wave into [0,1]. Mirrored tiling: keeps UVs inside the sprite (mandatory, the
    block atlas has no wrap mode) while holding texel density roughly constant along a tube of
    any length. The mirror seam is invisible on a near-flat metal swatch."""
    x = x % 2.0
    return x if x <= 1.0 else 2.0 - x


def uv(u_frac, v_frac):
    """Map into the sprite's middle half, away from the border where CUTOUT_MIPPED mip levels
    bleed into neighbouring atlas entries. flip-v is on, so v is written inverted."""
    u = 0.25 + 0.5 * pingpong(u_frac)
    v = 0.25 + 0.5 * pingpong(v_frac)
    return (u, 1.0 - v)


def build_sweep(run, rise):
    """Returns (bands, rings). A band is (ring_a, ring_b, t_a, t_b, arclen_a, arclen_b)."""
    near_ring, near_ts = clip_ring(16.0 + FLANGE_REACH, run, rise)
    # The first FULL ring has to sit past every clipped rail, or a quad would run backwards.
    t_first = max(near_ts)
    # The far end needs no clipping: the parabola is horizontal at t = run, so the cross-section
    # there is already vertical. Stop a hundredth of a unit short so the cap is not exactly on
    # the block boundary plane.
    t_last = run - INSET / 16.0

    ts = [t for t in sample_ts(run, rise) if t_first < t < t_last]
    ts = [t_first] + ts + [t_last]
    rings = [ring(t, run, rise) for t in ts]
    rings[0] = near_ring  # ragged clipped aperture, all vertices on z = 16 - INSET
    arc = [0.0]
    for i in range(1, len(ts)):
        arc.append(arc[-1] + math.dist(centre(ts[i - 1], run, rise),
                                       centre(ts[i], run, rise)))

    bands = []
    for i in range(len(ts) - 1):
        if arc[i + 1] - arc[i] < 1e-3:
            raise AssertionError(
                "degenerate band at t=%.9f in run=%d rise=%d: its two rings coincide, so every "
                "triangle in it would be a zero-area sliver" % (ts[i], run, rise))
        bands.append((rings[i], rings[i + 1], ts[i], ts[i + 1], arc[i], arc[i + 1]))
    return bands, rings


def emit_tube(mesh, band):
    ra, rb, ta, tb, aa, ab = band
    va = aa / 16.0
    vb = ab / 16.0
    for i in range(SIDES):
        j = (i + 1) % SIDES
        ua = i / SIDES
        ub = (i + 1) / SIDES
        pa, na = ra[i]
        pb, nb = ra[j]
        pc, nc = rb[j]
        pd, nd = rb[i]
        # Wound so the normal points outward (away from the axis).
        mesh.quad((pa, uv(ua, va), na),
                  (pd, uv(ua, vb), nd),
                  (pc, uv(ub, vb), nc),
                  (pb, uv(ub, va), nb))


def emit_cap(mesh, rng, normal, centre_pt):
    """Triangle fan closing off a ring. Both ends are inset from the block boundary rather than
    sitting on it: coincident opposite-facing quads are safe, but the model audit flags faces on
    a boundary and a hundredth of a unit costs nothing."""
    cu = uv(0.5, 0.5)
    for i in range(SIDES):
        j = (i + 1) % SIDES
        a = rng[i][0]
        b = rng[j][0]
        ua = uv(0.5 + 0.4 * math.cos(2 * math.pi * i / SIDES),
                0.5 + 0.4 * math.sin(2 * math.pi * i / SIDES))
        ub = uv(0.5 + 0.4 * math.cos(2 * math.pi * j / SIDES),
                0.5 + 0.4 * math.sin(2 * math.pi * j / SIDES))
        if normal[2] > 0:
            mesh.tri([(centre_pt, cu, normal), (a, ua, normal), (b, ub, normal)])
        else:
            mesh.tri([(centre_pt, cu, normal), (b, ub, normal), (a, ua, normal)])


def emit_flange(mesh, run, rise):
    """The bolted connection collar where the arm meets the pole.

    It is an ELLIPSE, not a circle, and that is forced rather than decorative. The arm leaves the
    pole at an angle, so the plane where it meets the pole face cuts the tube obliquely -- the
    entry aperture is an ellipse stretched vertically by 1/cos(theta). A round collar sized to
    the tube's diameter would not cover it, and one sized to its height would be absurdly wide.
    Sizing the collar to the actual cut plus a fixed margin covers the seam at every preset and
    stays inside the block.
    """
    _, ty, tz = tangent(0.0, run, rise)
    stretch = 1.0 / abs(tz)  # = 1/cos(theta), the oblique-cut elongation
    rx = R_FLANGE + FLANGE_MARGIN
    ry = R_FLANGE * stretch + FLANGE_MARGIN
    z_front = 16.0 + FLANGE_REACH
    z_back = z_front - FLANGE_LEN
    front, back = [], []
    for i in range(SIDES):
        a = 2.0 * math.pi * i / SIDES
        ca, sa = math.cos(a), math.sin(a)
        # Outward normal of an ellipse is not its radial direction; scale the components the
        # other way round so shading across the collar stays believable.
        n = (ca / rx, sa / ry, 0.0)
        nl = math.hypot(n[0], n[1])
        n = (n[0] / nl, n[1] / nl, 0.0)
        front.append(((8.0 + rx * ca, 8.0 + ry * sa, z_front), n))
        back.append(((8.0 + rx * ca, 8.0 + ry * sa, z_back), n))
    for i in range(SIDES):
        j = (i + 1) % SIDES
        ua, ub = i / SIDES, (i + 1) / SIDES
        mesh.quad((front[i][0], uv(ua, 0.0), front[i][1]),
                  (back[i][0], uv(ua, FLANGE_LEN / 16.0), back[i][1]),
                  (back[j][0], uv(ub, FLANGE_LEN / 16.0), back[j][1]),
                  (front[j][0], uv(ub, 0.0), front[j][1]))
    emit_cap(mesh, front, (0.0, 0.0, 1.0), (8.0, 8.0, z_front))
    emit_cap(mesh, back, (0.0, 0.0, -1.0), (8.0, 8.0, z_back))


# --------------------------------------------------------------------------------------------
# Cell assignment
# --------------------------------------------------------------------------------------------

def assign_cells(bands, run, rise):
    """Split the sweep across block cells, and return an ordered, face-connected cell list with
    the bands each cell owns."""
    order = []
    owned = {}
    for band in bands:
        ra, rb, ta, tb, aa, ab = band
        mid = centre((ta + tb) / 2.0, run, rise)
        c = cell_of(mid)
        # The collar reaches past the pole face, so the first band's midpoint can sit at a
        # negative parameter -- geometrically behind the root, inside the pole. That is not a
        # cell of its own; it belongs to the root. Clamping here rather than fudging the
        # parameter keeps arc length honest, and an earlier version that fudged the parameter
        # instead collided with the first real sample and produced a zero-length band.
        c = (max(0, c[0]), max(0, c[1]))
        if c not in owned:
            owned[c] = []
            order.append(c)
        owned[c].append(band)

    # Repair diagonal steps. They happen when the centreline crosses a row boundary and a column
    # boundary at the same t -- which for a parabola is not exotic: 6x2 does it exactly at t=3.
    # A diagonal step leaves a hole a player can walk through and, worse, breaks the block
    # adjacency the auto-connect system will later need. Insert the cell that advances along the
    # arm first and hand it the destination's first band; the band's geometry is unchanged, only
    # which cell's origin it is drawn from.
    fixed_order, fixed_owned = [order[0]], {order[0]: owned[order[0]]}
    for k in range(1, len(order)):
        prev, cur = fixed_order[-1], order[k]
        if abs(cur[0] - prev[0]) + abs(cur[1] - prev[1]) > 1:
            filler = (cur[0], prev[1])
            if filler in fixed_owned or filler == prev:
                filler = (prev[0], cur[1])
            moved = owned[cur][:1]
            owned[cur] = owned[cur][1:]
            fixed_order.append(filler)
            fixed_owned[filler] = moved
        fixed_order.append(cur)
        fixed_owned[cur] = owned[cur]
    for c in fixed_order:
        if not fixed_owned[c]:
            raise AssertionError("cell %s owns no geometry -- it would place an invisible "
                                 "block" % (c,))
    return fixed_order, fixed_owned


# --------------------------------------------------------------------------------------------
# Emit
# --------------------------------------------------------------------------------------------

def registry_name(preset_id, color_id):
    return "trafficpolemastarmcurve%s%s" % (preset_id, color_id)


def build_preset(preset_id, run, rise):
    bands, rings = build_sweep(run, rise)
    order, owned = assign_cells(bands, run, rise)

    # Whole-curve mesh, used for the inventory icon.
    whole = Mesh()
    # The flange's front disc closes the aperture, so the tube needs no cap of its own here --
    # a second disc on the same plane facing the same way would just z-fight with it.
    emit_flange(whole, run, rise)
    for b in bands:
        emit_tube(whole, b)
    emit_cap(whole, rings[-1], (0.0, 0.0, -1.0),
             (8.0, sum(p[0][1] for p in rings[-1]) / SIDES, rings[-1][0][0][2]))

    cells = []
    for idx, (ci, cj) in enumerate(order):
        m = Mesh()
        if idx == 0:
            emit_flange(m, run, rise)
        for b in owned[(ci, cj)]:
            emit_tube(m, b)
        if (ci, cj) == order[-1]:
            emit_cap(m, rings[-1], (0.0, 0.0, -1.0),
                     (8.0, sum(p[0][1] for p in rings[-1]) / SIDES, rings[-1][0][0][2]))
        # Translate into the cell's own 0..16 frame: cell (ci, cj) covers z from -16ci to
        # 16-16ci and y from 16cj upward.
        (lo, hi) = m.bounds()
        off = (0.0, 16.0 * cj, -16.0 * ci)
        # The cell's own slice, in its own block, clamped to the block. Geometry that overhangs
        # into a neighbour is that neighbour's cell to cover -- and it does, because every cell
        # of a curve is a placed block, so the union of the clamped boxes covers the whole tube.
        box = tuple(min(1.0, max(0.0, (v[k] - off[k]) / 16.0))
                    for v in (lo, hi) for k in range(3))
        cells.append(((ci, cj), m, off, box))
    return cells, whole


def main():
    os.makedirs(MODEL_DIR, exist_ok=True)
    os.makedirs(SCRATCH_DIR, exist_ok=True)

    # Everything this script owns, cleared before it writes. Changing a preset changes how many
    # cells it needs, and a model left behind from a previous cell count is invisible: nothing
    # references it, nothing fails to load, and it ships in the jar anyway. One did survive the
    # first run of this file that way.
    for stale in glob.glob(os.path.join(MODEL_DIR, "mastarmcurve_*.obj")):
        os.remove(stale)
    for stale in glob.glob(os.path.join(BLOCKSTATE_DIR, "trafficpolemastarmcurve*.json")):
        os.remove(stale)

    with open(os.path.join(MODEL_DIR, MTL_NAME), "w", newline="\n") as fh:
        fh.write("# Procedurally generated by gen_mast_arm_curves.py\n"
                 "# One material: every colour variant's blockstate overrides #body.\n"
                 "newmtl %s\nmap_Kd %s\n" % (MATERIAL, COLORS[0][1]))

    lang_lines, tab_lines, summary = [], [], []
    profiles = []

    for preset_id, run, rise, label in PRESETS:
        cells, whole = build_preset(preset_id, run, rise)
        entry_deg = math.degrees(math.atan2(2.0 * rise, float(run)))

        for idx, ((ci, cj), mesh, offset, _box) in enumerate(cells):
            mesh.write(os.path.join(MODEL_DIR, "mastarmcurve_%s_c%d.obj" % (preset_id, idx)),
                       "mastarm_%s_c%d" % (preset_id, idx), offset=offset,
                       header="cell %d of %d at (+%d along arm, +%d up)"
                              % (idx, len(cells), ci, cj))

        (lo, hi) = whole.bounds()
        span = max(hi[0] - lo[0], hi[1] - lo[1], hi[2] - lo[2])
        whole.write(os.path.join(MODEL_DIR, "mastarmcurve_%s_inv.obj" % preset_id),
                    "mastarm_%s_inv" % preset_id,
                    offset=(lo[0] - (span - (hi[0] - lo[0])) / 2.0,
                            lo[1] - (span - (hi[1] - lo[1])) / 2.0,
                            lo[2] - (span - (hi[2] - lo[2])) / 2.0),
                    scale=16.0 / span,
                    header="inventory icon: whole sweep fitted to one block")

        profiles.append((preset_id, run, rise, label, entry_deg,
                         [c[0] for c in cells], [c[3] for c in cells]))

        for color_id, texture, color_label in COLORS:
            name = registry_name(preset_id, color_id)
            write_blockstate(name, preset_id, len(cells), texture)
            lang_lines.append("tile.%s.name=%s Mast Arm Curve %dx%d (%s)"
                              % (name, color_label, run, rise, label))
            tab_lines.append(
                "    initTabBlock(new BlockTrafficPoleMastArmCurve(\"%s\", "
                "MastArmCurveProfile.P%s));" % (name, preset_id.upper()))

        summary.append("  %-4s run %d rise %d  entry %5.1f deg  %d cells  %s"
                       % (preset_id, run, rise, entry_deg, len(cells),
                          " ".join("(%d,%d)" % c[0] for c in cells)))

    write_profile_enum(profiles)

    with open(os.path.join(SCRATCH_DIR, "lang_fragment.txt"), "w", newline="\n") as fh:
        fh.write("\n".join(lang_lines) + "\n")
    with open(os.path.join(SCRATCH_DIR, "tab_fragment.txt"), "w", newline="\n") as fh:
        fh.write("\n".join(tab_lines) + "\n")

    print("Mast arm curve presets:")
    print("\n".join(summary))
    print("\nWrote %d OBJ cells + %d inventory models, %d blockstates."
          % (sum(len(p[5]) for p in profiles), len(PRESETS), len(PRESETS) * len(COLORS)))
    print("Fragments in %s" % SCRATCH_DIR)


def write_blockstate(name, preset_id, cell_count, texture):
    cell_variants = {}
    for i in range(cell_count):
        cell_variants[str(i)] = {
            "model": "%s/mastarmcurve_%s_c%d.obj" % (MODEL_PREFIX, preset_id, i)
        }
    data = {
        "forge_marker": 1,
        "defaults": {
            "model": "%s/mastarmcurve_%s_c0.obj" % (MODEL_PREFIX, preset_id),
            "custom": {"flip-v": True},
            "textures": {"#" + MATERIAL: texture, "particle": texture},
        },
        "variants": {
            "cell": cell_variants,
            "facing": {
                "north": {"y": 0},
                "east": {"y": 90},
                "south": {"y": 180},
                "west": {"y": 270},
            },
            "inventory": [{
                "model": "%s/mastarmcurve_%s_inv.obj" % (MODEL_PREFIX, preset_id),
                "transform": "forge:default-block",
            }],
        },
    }
    with open(os.path.join(BLOCKSTATE_DIR, name + ".json"), "w", newline="\n") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


def write_profile_enum(profiles):
    body = []
    for i, (pid, run, rise, label, entry, cells, boxes) in enumerate(profiles):
        cell_src = ", ".join("{%d, %d}" % c for c in cells)
        box_src = ",\n          ".join("{%s}" % ", ".join("%.5fD" % v for v in b)
                                       for b in boxes)
        term = "," if i < len(profiles) - 1 else ";"
        body.append(
            "  /**\n"
            "   * %s upsweep: %d of run, %d of rise, leaving the pole at %.1f degrees, across\n"
            "   * %d block cells.\n"
            "   */\n"
            "  P%s(%d, %d,\n"
            "      new int[][]{%s},\n"
            "      new double[][]{%s})%s"
            % (label, run, rise, entry, len(cells), pid.upper(), run, rise, cell_src,
               box_src, term))
    src = '''package com.micatechnologies.minecraft.csm.trafficaccessories;

/**
 * The mast arm curve sizes, and the exact block cells each one occupies.
 *
 * <p><b>Generated by {@code dev-env-utils/scripts/gen_mast_arm_curves.py}. Do not edit by
 * hand.</b> The cell lists here are the same lists the generator split the OBJ geometry on. If
 * the two ever disagreed, a curve would place a block with no model in it, or draw a slice of
 * tube in mid-air with no block behind it -- so they are emitted together from one source.
 *
 * <p>Each cell is {@code {alongArm, up}} in blocks from the placed root, before the block's
 * facing rotation is applied. The list is ordered from the pole outward and is face-connected,
 * so consecutive cells always share a face.
 */
public enum MastArmCurveProfile {

%s

  private final int run;
  private final int rise;
  private final int[][] cells;
  private final double[][] boxes;

  MastArmCurveProfile(int run, int rise, int[][] cells, double[][] boxes) {
    this.run = run;
    this.rise = rise;
    this.cells = cells;
    this.boxes = boxes;
  }

  /**
   * The horizontal distance the curve covers, in blocks.
   *
   * @return the run in blocks
   */
  public int getRun() {
    return run;
  }

  /**
   * The vertical distance the curve climbs, in blocks.
   *
   * @return the rise in blocks
   */
  public int getRise() {
    return rise;
  }

  /**
   * The number of block cells this curve occupies, including the root.
   *
   * @return the cell count
   */
  public int getCellCount() {
    return cells.length;
  }

  /**
   * The offset of one cell from the root, as {@code {alongArm, up}} in blocks, unrotated.
   *
   * @param index the cell index, {@code 0} being the root at the pole
   *
   * @return a two-element offset; callers must not mutate it
   */
  public int[] getCellOffset(int index) {
    return cells[index];
  }

  /**
   * The measured extent of one cell's tube slice inside its own block, as
   * {@code {minX, minY, minZ, maxX, maxY, maxZ}} in 0..1, for the unrotated (facing north)
   * orientation. Measured off the emitted mesh rather than guessed, and clamped to the block:
   * geometry that overhangs into the next cell is covered by that cell's own box, because every
   * cell of a curve is a real placed block.
   *
   * @param index the cell index, {@code 0} being the root at the pole
   *
   * @return a six-element box; callers must not mutate it
   */
  public double[] getCellBox(int index) {
    return boxes[index];
  }
}
''' % ("\n".join(body))
    with open(os.path.join(JAVA_DIR, "MastArmCurveProfile.java"), "w", newline="\n") as fh:
        fh.write(src)


if __name__ == "__main__":
    main()
