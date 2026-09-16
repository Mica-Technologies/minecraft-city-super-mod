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

THE POLE END
------------
The root cell is generated once per pole width the arm can land on (POLE_FITS). Against the
12-across pole family the tube is saddle-cut into the pole and swells into a welded boot. The
thin pole (8 across) and the pedestal pole (6 across) are narrower than the tube itself, so no
cut into them can hide the arm's end; there the root ends the tube flat inside a bolted bracket
plate with a saddle block and two band straps round the pole. Only the root's model differs, so
the fit is one more thing folded into the `shape` property; the block reads the pole's radius
off `AbstractBlockTrafficPole.getPoleRadius` and picks the fit through `PoleFit.forPoleRadius`.

OUTPUT
------
  models/block/trafficaccessories/shared_models/mastarmcurve_<preset>_c<n>.obj   (+ _inv.obj)
  models/block/trafficaccessories/shared_models/mastarmcurve_<preset>_c0_thin.obj, _c0_ped.obj
                                                                  (the root on a narrow pole)
  models/block/trafficaccessories/shared_models/mastarmcurve.mtl                 (one, shared)
  blockstates/trafficpolemastarmcurve<preset><color>.json                        (presets x colors)
  src/main/java/.../trafficaccessories/MastArmCurveProfile.java, in whichever tree already has
    it (the roads module today; resolved via csm_layout.source_for_write)      (generated enum)
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
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

REPO_ROOT = layout.REPO_ROOT
TRAFFICACCESSORIES_OWNER = layout.owner_of_folder("trafficaccessories")
MODEL_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER,
                                       "models/block/trafficaccessories/shared_models")
BLOCKSTATE_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER, "blockstates")
# The generated Java enum: an existing copy always wins (it is only ever regenerated in place).
JAVA_PATH = layout.source_for_write(TRAFFICACCESSORIES_OWNER,
                                    "trafficaccessories/MastArmCurveProfile.java")
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
R_ROOT = 4.6         # tube radius in units where the boot ends (9.2 across)
R_TIP = 4.0          # tube radius at the tip -- MUST match trafficpolehorizontal's 8 across
INSET = 0.01         # keeps end caps off the block boundary plane exactly

# The boot: the arm swells over its last stretch before the pole, the way a welded mast arm boot
# does. R_BOOT is capped by two neighbours it has to disappear behind, and 5.0 is the largest
# value that clears both:
#   * the pole itself is 12 across, so anything wider than 12 cannot hide behind it at any angle;
#   * `trafficpoleverticalconnectorangled*`, the existing block for this joint, has a boss
#     10 across (x 3..13) and 11 tall, and the boot should tuck inside that too.
# The collar this replaces was 12.4 across against a 12-wide pole -- it could not hide, which is
# why it showed as flat slivers either side of the pole.
R_BOOT = 5.0
BOOT_LEN = 0.36      # blocks of run over which the boot swells

# The pole the arm lands on, in the root cell's own frame. A CSM pole is a cylinder of radius 6
# centred in its block; the block north of the root starts at z = 16, so its axis is at z = 24.
POLE_RADIUS = 6.0
POLE_AXIS_X = 8.0
POLE_AXIS_Z = 24.0

# Narrower poles get a different joint. The tube is 9.2 across at the root, so against the thin
# pole (8 across) or the pedestal pole (6 across) the saddle cut has nothing to land on: the rails
# at the flanks would never enter the pole's cylinder, and a boot wider than the pole cannot hide
# behind it. A real arm on a slim pole is not welded on either; it is bolted to a bracket plate
# that is strapped round the pole. So the root cell of a curve on a narrow pole ends the tube flat
# inside a plate, a saddle block behind the plate hugs the pole, and two band straps wrap it.
#
# (registry/model suffix, pole radius, label). The first entry is the pole family's 12-across
# tube, which keeps the saddle-cut boot. The Java enum's PoleFit is emitted from this list in this
# order, and the radius thresholds it picks a fit by are the midpoints between these radii.
POLE_FITS = [
    ("", POLE_RADIUS, "large"),
    ("_thin", 4.0, "thin"),        # trafficpolehorizontal* (model `small`, x 4..12)
    ("_ped", 3.0, "pedestal"),     # trafficpolepedestal* (R_SHAFT in gen_pedestal_pole.py)
]
PLATE_T = 1.2          # bracket plate thickness
PLATE_GAP = 0.3        # the plate stands this far off the pole's front skin
PLATE_MARGIN = 0.9     # the plate extends this far past the tube's cut end all round
TUBE_INTO_PLATE = 0.4  # the tube's flat end sits this far inside the plate's front face
SADDLE_INSET = 1.0     # the saddle block stops this far short of the plate's top and bottom
SADDLE_BITE = 0.3      # how far the saddle block's concave back reaches inside the pole skin
STRAP_STANDOFF = 0.45  # band strap radius beyond the pole's
STRAP_H = 1.4
STRAP_INSET = 0.9      # straps sit this far in from the plate's top and bottom edges


def is_large(fit):
    return fit[0] == ""


def bracket_geometry(pole_r):
    """The z planes of the narrow-pole bracket in the root cell's frame, for a pole of the given
    radius: its front skin, the plate's back and front, and where the tube is cut."""
    z_front = POLE_AXIS_Z - pole_r
    z_plate_back = z_front - PLATE_GAP
    z_plate_front = z_plate_back - PLATE_T
    return {"z_front": z_front, "z_plate_back": z_plate_back,
            "z_plate_front": z_plate_front, "z_cut": z_plate_front + TUBE_INTO_PLATE}

# Two sweep samples closer than this (in blocks travelled) are the same sample. Comfortably below
# the uniform spacing of run/SAMPLES_PER_BLOCK, and comfortably above the float noise that makes a
# solved boundary crossing miss an exact one.
MERGE_TOL = 1e-4



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


def radius(t, run, boot=True):
    """Tube radius at t, including the boot.

    Two effects superposed: the arm's own taper from root to tip, plus a swelling over the last
    BOOT_LEN of run before the pole. Making the boot part of the tube's radius rather than a
    separate collar part is what keeps it from poking out of anything -- there is only ever one
    surface here, so there is nothing to bleed through.

    boot=False leaves the swelling out. The narrow-pole bracket ends the tube flat inside a
    bolted plate, and a welded boot on a bolted bracket would be two joints on one arm."""
    base = R_ROOT + (R_TIP - R_ROOT) * (t / run)
    if not boot:
        return base
    u = min(1.0, max(0.0, t / BOOT_LEN))
    return base + (R_BOOT - R_ROOT) * (1.0 - u) ** 1.6


def ring(t, run, rise, r=None, boot=True):
    """One cross-section: SIDES points around the tube, plus their outward normals."""
    px, py, pz = centre(t, run, rise)
    tx, ty, tz = tangent(t, run, rise)
    # In-plane normal: the tangent rotated 90 degrees within YZ. The other basis vector is X,
    # which is constant because the whole curve lives in one vertical plane.
    ny, nz = -tz, ty
    rr = radius(t, run, boot) if r is None else r
    out = []
    for i in range(SIDES):
        a = 2.0 * math.pi * i / SIDES
        ca, sa = math.cos(a), math.sin(a)
        dirx, diry, dirz = ca, sa * ny, sa * nz
        out.append(((px + rr * dirx, py + rr * diry, pz + rr * dirz), (dirx, diry, dirz)))
    return out


def rail_point(t, i, run, rise):
    """Where one angular rail of the tube is at parameter t. A rail is the path a single point of
    the cross-section traces along the sweep."""
    return ring(t, run, rise)[i][0]


def pole_depth(t, i, run, rise):
    """How far inside the pole's cylinder a rail is at t. Negative outside, zero on the skin."""
    p = rail_point(t, i, run, rise)
    dx = p[0] - POLE_AXIS_X
    dz = p[2] - POLE_AXIS_Z
    return POLE_RADIUS * POLE_RADIUS - (dx * dx + dz * dz)


def clip_ring(run, rise):
    """The arm's true intersection with the POLE, as a ragged ring: every angular rail gets its
    own parameter, the one where that rail meets the pole's skin.

    Cutting against the pole's cylinder rather than against a flat plane is the whole difference
    between a joint and a mess. A pole is round, so a flat cut lands at the right depth only
    along the arm's centreline: at the arm's flanks the pole's surface has receded 2.7 units
    further back, and the flat cut face hangs in the open there. That, plus a collar wider than
    the pole it was meant to hide behind, is what made the old joint look unfinished. A saddle
    cut lands every rail exactly on the pole's skin, so the arm meets it everywhere at once --
    which is also what a welded mast arm boot actually looks like.

    Rails can only miss the cylinder if the tube were wider than the pole; R_BOOT (5.0) against
    POLE_RADIUS (6.0) guarantees they do not.

    Returns (points_with_normals, t_of_each_rail)."""
    pts, ts = [], []
    for i in range(SIDES):
        # Find the FIRST crossing walking backwards, not any root. Depth is negative on both
        # sides of the pole -- keep going and a rail comes out the far side -- so a plain
        # bisection over a wide bracket is as likely to land on the exit as on the entry, and
        # the exit is 40-odd units away through the middle of the pole.
        t_out = 0.5                      # z = 8 here, 16 from the pole's axis: certainly outside
        step = 0.002
        t_in = None
        t = t_out
        while t > -1.5:
            t -= step
            if pole_depth(t, i, run, rise) > 0.0:
                t_in = t
                break
            t_out = t
        if t_in is None:
            raise AssertionError("rail %d of run=%d rise=%d never reaches the pole" % (i, run, rise))
        for _ in range(50):
            mid = (t_out + t_in) / 2.0
            if pole_depth(mid, i, run, rise) > 0.0:
                t_in = mid
            else:
                t_out = mid
        t = (t_out + t_in) / 2.0
        ts.append(t)
        pts.append(ring(t, run, rise)[i])
    return pts, ts


def clip_plane_ring(run, rise, z_cut):
    """The arm's flat end for the narrow-pole bracket: every rail clipped where it crosses the
    plane z = z_cut, so the ring is planar and the plate in front of it hides it.

    This is the "clipped rail by rail against a flat plane" cut the doc records as wrong -- and
    it was wrong THERE, against a bare round pole, because nothing covered the flanks. Behind a
    bracket plate wider than the tube it is the right cut: the plate is flat, so the end must be
    too. The rails are walked backwards from a parameter known to be outside, the way clip_ring
    does, rather than solved directly: the rail's z is the centreline's z plus a term that turns
    with the tangent, and it is simpler to scan than to prove monotone.

    Returns (points_with_normals, t_of_each_rail)."""
    pts, ts = [], []
    for i in range(SIDES):
        t_out = 0.5
        step = 0.002
        t_in = None
        t = t_out
        while t > -1.5:
            t -= step
            if ring(t, run, rise, boot=False)[i][0][2] >= z_cut:
                t_in = t
                break
            t_out = t
        if t_in is None:
            raise AssertionError("rail %d of run=%d rise=%d never reaches z=%.2f"
                                 % (i, run, rise, z_cut))
        for _ in range(50):
            mid = (t_out + t_in) / 2.0
            if ring(mid, run, rise, boot=False)[i][0][2] >= z_cut:
                t_in = mid
            else:
                t_out = mid
        t = (t_out + t_in) / 2.0
        ts.append(t)
        p, n = ring(t, run, rise, boot=False)[i]
        pts.append(((p[0], p[1], z_cut), n))
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

    def same_as(self, other):
        """Whether two meshes are the same geometry, to the precision the OBJ is written at."""
        return ([self._key(p) for p in self.v] == [other._key(p) for p in other.v]
                and self.f == other.f)

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


def build_sweep(run, rise, fit=POLE_FITS[0]):
    """Returns (bands, rings). A band is (ring_a, ring_b, t_a, t_b, arclen_a, arclen_b).

    `fit` is a POLE_FITS entry. The large pole gets the saddle cut and the boot; a narrow pole
    gets the boot-free tube, cut flat where it enters the bracket plate. From the boot's end
    outward the sweep is identical whichever fit is asked for, and the boot lies inside the root
    cell, which is what lets a narrow fit replace the root cell's model and nothing else."""
    boot = is_large(fit)
    if boot:
        near_ring, near_ts = clip_ring(run, rise)
    else:
        near_ring, near_ts = clip_plane_ring(run, rise, bracket_geometry(fit[1])["z_cut"])
    # The first FULL ring has to sit past every clipped rail, or a quad would run backwards.
    t_first = max(near_ts)
    # The far end needs no clipping: the parabola is horizontal at t = run, so the cross-section
    # there is already vertical. Stop a hundredth of a unit short so the cap is not exactly on
    # the block boundary plane.
    t_last = run - INSET / 16.0

    ts = [t for t in sample_ts(run, rise) if t_first < t < t_last]
    ts = [t_first] + ts + [t_last]
    rings = [ring(t, run, rise, boot=boot) for t in ts]
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


def emit_saddle_cap(mesh, rng):
    """Closes the saddle-cut end of the arm.

    Normally invisible -- it sits inside the pole. It exists for the case where someone places a
    curve without a pole behind it, where an open end would be see-through. The ring is not
    planar, so this fans from its centroid rather than from a plane centre."""
    n = len(rng)
    centroid = (sum(p[0][0] for p in rng) / n,
                sum(p[0][1] for p in rng) / n,
                sum(p[0][2] for p in rng) / n)
    emit_cap(mesh, rng, (0.0, 0.0, 1.0), centroid)


# --------------------------------------------------------------------------------------------
# Mount stubs
#
# When something mountable sits next to a curve cell, that cell grows the hardware a pole grows:
# a band clamped round the tube, a short bracket, and a plate on the shared face. On a pole this
# is one model reused everywhere, because a pole is a straight tube centred in its block. A curve
# cell's tube is neither centred nor level and its pose differs in every cell, so the hardware has
# to be generated per cell and per direction -- 4 files per cell.
#
# Directions are MODEL space, i.e. as the curve is drawn facing north with the arm running toward
# -Z. The blockstate's y rotation carries them round with the block. The order of this list is the
# bit order of the mount mask; MOUNT_BIT_ORDER in the generated enum repeats it for the Java side.
# --------------------------------------------------------------------------------------------

# (name, centre of the face it reaches, that face's outward normal)
STUB_DIRECTIONS = [
    ("down", (8.0, 0.0, 8.0), (0.0, -1.0, 0.0)),
    ("up", (8.0, 16.0, 8.0), (0.0, 1.0, 0.0)),
    ("east", (16.0, 8.0, 8.0), (1.0, 0.0, 0.0)),
    ("west", (0.0, 8.0, 8.0), (-1.0, 0.0, 0.0)),
]

BAND_MARGIN = 0.9    # how far the clamp band stands off the tube it grips
BAND_LEN = 3.0
BRACKET_R = 1.6
PLATE_R = 3.2
PLATE_LEN = 1.2


def vsub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def vadd(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def vmul(a, k):
    return (a[0] * k, a[1] * k, a[2] * k)


def vlen(a):
    return math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])


def vnorm(a):
    n = vlen(a) or 1e-9
    return (a[0] / n, a[1] / n, a[2] / n)


def vcross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def emit_cylinder(mesh, a, b, r, sides=12):
    """A capped cylinder from a to b. Used for all three pieces of a stub.

    The cross-section basis is deliberately LEFT handed (q = p x axis, not axis x p) to match the
    basis the tube sweep uses, so the same vertex order winds outward here as it does there rather
    than inside out."""
    axis = vnorm(vsub(b, a))
    seed = (0.0, 0.0, 1.0) if abs(axis[2]) < 0.9 else (1.0, 0.0, 0.0)
    p = vnorm(vcross(axis, seed))
    q = vcross(p, axis)
    ra, rb, normals = [], [], []
    for i in range(sides):
        ang = 2.0 * math.pi * i / sides
        d = vadd(vmul(p, math.cos(ang)), vmul(q, math.sin(ang)))
        normals.append(d)
        ra.append(vadd(a, vmul(d, r)))
        rb.append(vadd(b, vmul(d, r)))
    for i in range(sides):
        j = (i + 1) % sides
        ua, ub = i / sides, (i + 1) / sides
        mesh.quad((ra[i], uv(ua, 0.0), normals[i]),
                  (rb[i], uv(ua, r / 16.0), normals[i]),
                  (rb[j], uv(ub, r / 16.0), normals[j]),
                  (ra[j], uv(ub, 0.0), normals[j]))
    # Caps. Going round the ring in increasing angle turns toward -axis in this basis (it is
    # left handed by construction, see above), so the cap that faces along +axis is the one that
    # needs its winding reversed -- the opposite of what reads naturally.
    cu = uv(0.5, 0.5)
    for (centre_pt, rng, n, reverse) in ((a, ra, vmul(axis, -1.0), False),
                                         (b, rb, axis, True)):
        for i in range(sides):
            j = (i + 1) % sides
            tri = [(centre_pt, cu, n), (rng[i], uv(0.3, 0.3), n), (rng[j], uv(0.7, 0.7), n)]
            mesh.tri([tri[0], tri[2], tri[1]] if reverse else tri)


def emit_stub(mesh, t_lo, t_hi, run, rise, offset, face_centre, face_normal, boot=True):
    """One direction's mount hardware for one cell, in that cell's local frame.

    The band is placed at the point of the tube CLOSEST TO THE FACE CENTRE rather than at the
    cell's middle. Those are not the same point: in a cell the tube only clips a corner of -- and
    several of every preset's cells are like that -- the middle of the cell has no tube in it, and
    hardware placed there would float."""
    best = None
    for i in range(129):
        t = t_lo + (t_hi - t_lo) * i / 128.0
        pt = vsub(centre(t, run, rise), offset)
        d = vlen(vsub(face_centre, pt))
        if best is None or d < best[0]:
            best = (d, t, pt)
    dist, t_star, anchor = best
    r = radius(t_star, run, boot)
    to_face = vnorm(vsub(face_centre, anchor))

    # Clamp band, straddling the tube along its own axis.
    tan = tangent(t_star, run, rise)
    speed = vlen(vsub(centre(t_star + 1e-4, run, rise), centre(t_star - 1e-4, run, rise))) / 2e-4
    half = vmul(tan, BAND_LEN / 2.0) if speed else (0.0, 0.0, 0.0)
    emit_cylinder(mesh, vsub(anchor, half), vadd(anchor, half), r + BAND_MARGIN, sides=16)

    # Face plate, lying FLAT on the face it reaches -- its axis is the face's own normal, not the
    # bracket's. The bracket leaves the tube at whatever angle the tube happens to sit at, so a
    # plate square to the bracket ends up tilted against the block it is supposed to be bolted
    # to. Only the bracket should be allowed to slant; the plate is hardware against a flat
    # surface. Held a hair off the boundary so it is never coplanar with the neighbour's own face.
    plate_inner = vadd(face_centre, vmul(face_normal, -PLATE_LEN))
    emit_cylinder(mesh, plate_inner, vadd(face_centre, vmul(face_normal, -INSET)), PLATE_R,
                  sides=8)

    # Bracket, only when there is actually a gap to span. On a cell whose tube runs close to the
    # face -- an "up" stub under a tube that already fills the top of its cell -- there is not.
    if dist > r + BAND_MARGIN + 1.0:
        emit_cylinder(mesh, vadd(anchor, vmul(to_face, r - 0.6)), plate_inner, BRACKET_R)


# --------------------------------------------------------------------------------------------
# The narrow-pole bracket
#
# The joint a curve makes with a pole thinner than its own tube: a flat plate the tube ends
# inside, a saddle block behind the plate that hugs the pole, and two band straps round the pole.
# All of it is drawn by the ROOT CELL and sits in the pole's block (z > 16). An OBJ may leave its
# block; the root cell is right there behind it, so this is never the only thing keeping the
# bracket on screen.
# --------------------------------------------------------------------------------------------

def tri_n(mesh, a, b, c, n, uva=(0.3, 0.3), uvb=(0.7, 0.3), uvc=(0.5, 0.7)):
    """A triangle wound so that its face agrees with the normal it is given, whichever order
    the corners came in. Every face of the bracket has an obvious outward normal, and none of
    them is worth reasoning about winding by hand."""
    cr = vcross(vsub(b, a), vsub(c, a))
    if cr[0] * n[0] + cr[1] * n[1] + cr[2] * n[2] < 0.0:
        b, c = c, b
        uvb, uvc = uvc, uvb
    mesh.tri([(a, uv(*uva), n), (b, uv(*uvb), n), (c, uv(*uvc), n)])


def quad_n(mesh, a, b, c, d, n):
    """A planar quad with its corners in order round the edge, wound to the given normal."""
    tri_n(mesh, a, b, c, n, (0.3, 0.3), (0.7, 0.3), (0.7, 0.7))
    tri_n(mesh, a, c, d, n, (0.3, 0.3), (0.7, 0.7), (0.3, 0.7))


def emit_box(mesh, lo, hi):
    (x0, y0, z0), (x1, y1, z1) = lo, hi
    quad_n(mesh, (x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (0.0, 0.0, -1.0))
    quad_n(mesh, (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1), (0.0, 0.0, 1.0))
    quad_n(mesh, (x0, y0, z0), (x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (-1.0, 0.0, 0.0))
    quad_n(mesh, (x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (x1, y0, z1), (1.0, 0.0, 0.0))
    quad_n(mesh, (x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1), (0.0, -1.0, 0.0))
    quad_n(mesh, (x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1), (0.0, 1.0, 0.0))


def emit_saddle_block(mesh, pole_r, y0, y1, z_front, segments=8):
    """The block between the bracket plate and the pole: flat against the plate, concave
    against the pole.

    Its back is an arc of radius pole_r - SADDLE_BITE, so it sits just inside the pole's skin
    all the way across. A flat back would touch a round pole along one line and leave a
    widening gap toward each corner -- the same gap that made the old flat-cut arm look
    unfinished. Triangulated as strips at fixed x rather than as a fan: the arc indents the
    outline, so a fan from any one point would cross it."""
    half = 0.85 * pole_r
    r_arc = pole_r - SADDLE_BITE
    xs = [POLE_AXIS_X - half + 2.0 * half * k / segments for k in range(segments + 1)]
    back = [POLE_AXIS_Z - math.sqrt(max(0.0, r_arc * r_arc - (x - POLE_AXIS_X) ** 2))
            for x in xs]
    for k in range(segments):
        xa, xb = xs[k], xs[k + 1]
        za, zb = back[k], back[k + 1]
        quad_n(mesh, (xa, y1, z_front), (xb, y1, z_front), (xb, y1, zb), (xa, y1, za),
               (0.0, 1.0, 0.0))
        quad_n(mesh, (xa, y0, z_front), (xb, y0, z_front), (xb, y0, zb), (xa, y0, za),
               (0.0, -1.0, 0.0))
        # The concave back: outward from the block is toward the pole's axis.
        n = vnorm((POLE_AXIS_X - (xa + xb) / 2.0, 0.0, POLE_AXIS_Z - (za + zb) / 2.0))
        quad_n(mesh, (xa, y0, za), (xb, y0, zb), (xb, y1, zb), (xa, y1, za), n)
    quad_n(mesh, (xs[0], y0, z_front), (xs[-1], y0, z_front), (xs[-1], y1, z_front),
           (xs[0], y1, z_front), (0.0, 0.0, -1.0))
    quad_n(mesh, (xs[0], y0, z_front), (xs[0], y0, back[0]), (xs[0], y1, back[0]),
           (xs[0], y1, z_front), (-1.0, 0.0, 0.0))
    quad_n(mesh, (xs[-1], y0, z_front), (xs[-1], y0, back[-1]), (xs[-1], y1, back[-1]),
           (xs[-1], y1, z_front), (1.0, 0.0, 0.0))


def emit_bracket(mesh, cut_ring, pole_r):
    """Plate, saddle block and straps for one preset on one narrow pole.

    The plate is sized off the tube's own cut ring -- PLATE_MARGIN past it on every side -- so
    it is measured per preset rather than guessed: a steeper preset's oblique cut is a taller
    ellipse, and it also sits lower, because the parabola keeps dropping behind the pole face.

    Returns (plate_y0, plate_y1, plate_half_width) for the run summary."""
    g = bracket_geometry(pole_r)
    xs = [p[0][0] for p in cut_ring]
    ys = [p[0][1] for p in cut_ring]
    half_w = round((max(xs) - min(xs)) / 2.0 + PLATE_MARGIN, 1)
    y0 = round(min(ys) - PLATE_MARGIN, 1)
    y1 = round(max(ys) + PLATE_MARGIN, 1)
    emit_box(mesh, (POLE_AXIS_X - half_w, y0, g["z_plate_front"]),
             (POLE_AXIS_X + half_w, y1, g["z_plate_back"]))
    # The saddle block starts a fifth of a unit inside the plate so the two share no face.
    emit_saddle_block(mesh, pole_r, y0 + SADDLE_INSET, y1 - SADDLE_INSET,
                      g["z_plate_back"] - 0.2)
    for yc in (y0 + STRAP_INSET + STRAP_H / 2.0, y1 - STRAP_INSET - STRAP_H / 2.0):
        emit_cylinder(mesh, (POLE_AXIS_X, yc - STRAP_H / 2.0, POLE_AXIS_Z),
                      (POLE_AXIS_X, yc + STRAP_H / 2.0, POLE_AXIS_Z),
                      pole_r + STRAP_STANDOFF, sides=16)
    return (y0, y1, half_w)


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
    emit_saddle_cap(whole, rings[0])
    for b in bands:
        emit_tube(whole, b)
    emit_cap(whole, rings[-1], (0.0, 0.0, -1.0),
             (8.0, sum(p[0][1] for p in rings[-1]) / SIDES, rings[-1][0][0][2]))

    cells = []
    for idx, (ci, cj) in enumerate(order):
        m = Mesh()
        if idx == 0:
            emit_saddle_cap(m, rings[0])
        for b in owned[(ci, cj)]:
            emit_tube(m, b)
        if (ci, cj) == order[-1]:
            emit_cap(m, rings[-1], (0.0, 0.0, -1.0),
                     (8.0, sum(p[0][1] for p in rings[-1]) / SIDES, rings[-1][0][0][2]))
        # Translate into the cell's own 0..16 frame: cell (ci, cj) covers z from -16ci to
        # 16-16ci and y from 16cj upward.
        (lo, hi) = m.bounds()
        off = (0.0, 16.0 * cj, -16.0 * ci)
        # The span of the sweep this cell owns, which is what the stubs anchor against.
        t_lo = min(b[2] for b in owned[(ci, cj)])
        t_hi = max(b[3] for b in owned[(ci, cj)])
        stubs = []
        for (dir_name, face_centre, face_normal) in STUB_DIRECTIONS:
            sm = Mesh()
            emit_stub(sm, t_lo, t_hi, run, rise, off, face_centre, face_normal)
            stubs.append((dir_name, sm))
        # The cell's own slice, in its own block, clamped to the block. Geometry that overhangs
        # into a neighbour is that neighbour's cell to cover -- and it does, because every cell
        # of a curve is a placed block, so the union of the clamped boxes covers the whole tube.
        box = tuple(min(1.0, max(0.0, (v[k] - off[k]) / 16.0))
                    for v in (lo, hi) for k in range(3))
        cells.append(((ci, cj), m, off, box, stubs))

    # The root cell again, once per narrow pole: the same sweep from the boot's end outward,
    # the boot-free tube cut flat behind the bracket plate, and the bracket itself.
    root_variants = []
    for fit in POLE_FITS[1:]:
        fbands, frings = build_sweep(run, rise, fit)
        forder, fowned = assign_cells(fbands, run, rise)
        if forder != order:
            raise AssertionError("the %s fit of %s visits different cells: %s vs %s"
                                 % (fit[2], preset_id, forder, order))
        for c in order[1:]:
            if ([(b[2], b[3]) for b in fowned[c]] != [(b[2], b[3]) for b in owned[c]]):
                raise AssertionError("the %s fit of %s changed cell %s, which only the root "
                                     "may differ in" % (fit[2], preset_id, c))
        m = Mesh()
        emit_cap(m, frings[0], (0.0, 0.0, 1.0),
                 (POLE_AXIS_X, sum(p[0][1] for p in frings[0]) / SIDES, frings[0][0][0][2]))
        for b in fowned[order[0]]:
            emit_tube(m, b)
        plate = emit_bracket(m, frings[0], fit[1])
        # The stubs anchor on the LARGE root's parameter span, not this fit's. The tube is the
        # same from the boot's end outward and no stub anchors inside the boot, so searching the
        # same span lands on the same point and the meshes come out identical -- which is what
        # lets the blockstate reuse the large root's stub files. Searching this fit's own span
        # would move the search grid by a hair and produce four near-identical files per fit.
        t_lo = min(b[2] for b in owned[order[0]])
        t_hi = max(b[3] for b in owned[order[0]])
        stubs = []
        for (dir_name, face_centre, face_normal) in STUB_DIRECTIONS:
            sm = Mesh()
            emit_stub(sm, t_lo, t_hi, run, rise, (0.0, 0.0, 0.0), face_centre, face_normal,
                      boot=False)
            stubs.append((dir_name, sm))
        root_variants.append((fit, m, stubs, plate))
    return cells, whole, root_variants


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
        cells, whole, root_variants = build_preset(preset_id, run, rise)
        entry_deg = math.degrees(math.atan2(2.0 * rise, float(run)))

        for idx, ((ci, cj), mesh, offset, _box, stubs) in enumerate(cells):
            mesh.write(os.path.join(MODEL_DIR, "mastarmcurve_%s_c%d.obj" % (preset_id, idx)),
                       "mastarm_%s_c%d" % (preset_id, idx), offset=offset,
                       header="cell %d of %d at (+%d along arm, +%d up)"
                              % (idx, len(cells), ci, cj))
            # Stubs are already built in the cell's own frame, so they are written unoffset.
            for (dir_name, stub_mesh) in stubs:
                stub_mesh.write(
                    os.path.join(MODEL_DIR,
                                 "mastarmcurve_%s_c%d_%s.obj" % (preset_id, idx, dir_name)),
                    "mastarm_%s_c%d_%s" % (preset_id, idx, dir_name),
                    header="mount hardware on cell %d, model-space %s" % (idx, dir_name))

        # The narrow-pole roots. Their stubs are the large root's own files whenever the two
        # meshes agree -- they do unless a stub anchors inside the boot, which none does today --
        # so the stub set is shared rather than tripled.
        variant_models = []
        for (fit, mesh, stubs, (py0, py1, phw)) in root_variants:
            model_file = "mastarmcurve_%s_c0%s.obj" % (preset_id, fit[0])
            mesh.write(os.path.join(MODEL_DIR, model_file),
                       "mastarm_%s_c0%s" % (preset_id, fit[0]),
                       header="root cell on the %s pole (radius %.0f): bracket plate %.1f wide,"
                              " y %.1f..%.1f" % (fit[2], fit[1], 2.0 * phw, py0, py1))
            stub_files = {}
            for (dir_name, stub_mesh), (_d, large_stub) in zip(stubs, cells[0][4]):
                if stub_mesh.same_as(large_stub):
                    stub_files[dir_name] = "mastarmcurve_%s_c0_%s.obj" % (preset_id, dir_name)
                    continue
                stub_files[dir_name] = "mastarmcurve_%s_c0%s_%s.obj" % (preset_id, fit[0],
                                                                       dir_name)
                stub_mesh.write(os.path.join(MODEL_DIR, stub_files[dir_name]),
                                "mastarm_%s_c0%s_%s" % (preset_id, fit[0], dir_name),
                                header="mount hardware on the %s-pole root, model-space %s"
                                       % (fit[2], dir_name))
            variant_models.append((fit, model_file, stub_files))
            summary.append("       %-8s pole: plate %.1f wide, y %.1f..%.1f"
                           % (fit[2], 2.0 * phw, py0, py1))

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
            write_blockstate(name, preset_id, len(cells), texture, variant_models)
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
    print("\nWrote %d OBJ cells + %d narrow-pole roots + %d inventory models, %d blockstates."
          % (sum(len(p[5]) for p in profiles), len(PRESETS) * (len(POLE_FITS) - 1),
             len(PRESETS), len(PRESETS) * len(COLORS)))
    print("Fragments in %s" % SCRATCH_DIR)


def shape_variant(model_file, stub_files, mask, texture):
    """One `shape` variant: a cell's tube model plus the stub submodels its mask asks for."""
    variant = {"model": "%s/%s" % (MODEL_PREFIX, model_file)}
    submodels = {}
    for bit, (dir_name, _face, _normal) in enumerate(STUB_DIRECTIONS):
        if mask & (1 << bit):
            submodels["mount_" + dir_name] = {
                "model": "%s/%s" % (MODEL_PREFIX, stub_files[dir_name]),
                "custom": {"flip-v": True},
                "textures": {"#" + MATERIAL: texture},
            }
    if submodels:
        variant["submodel"] = submodels
    return variant


def write_blockstate(name, preset_id, cell_count, texture, variant_models):
    """One blockstate per preset per colour.

    Model and mount hardware are selected by a SINGLE `shape` property holding
    `slot * 16 + mountMask`, not by a `cell` property plus four mount properties. That is not a
    style choice, it is the only encoding that fits: a stub model depends on BOTH which cell it is
    and which way it points, so a mount property would have to carry the cell index, and four
    13-value mount properties alongside cell and facing is 1,370,928 block states -- all of which
    Minecraft materialises eagerly. Folding the same information into one property is 768 states,
    exactly what four plain booleans would have cost, while still letting each stub match the tube
    it clamps to.

    The slot is the cell index, except that the root cell on a narrow pole takes a slot past the
    last cell: `cell_count + (fit - 1)`. Which pole is behind the root is one more thing only the
    root's model depends on, and it goes in the same property for the same reason the mount mask
    does. `MastArmCurveProfile.shapeIndex` is the other half of this packing.
    """
    shape_variants = {}
    for cell in range(cell_count):
        model_file = "mastarmcurve_%s_c%d.obj" % (preset_id, cell)
        stub_files = {d: "mastarmcurve_%s_c%d_%s.obj" % (preset_id, cell, d)
                      for (d, _face, _normal) in STUB_DIRECTIONS}
        for mask in range(1 << len(STUB_DIRECTIONS)):
            shape_variants[str(cell * 16 + mask)] = shape_variant(model_file, stub_files, mask,
                                                                  texture)
    for k, (_fit, model_file, stub_files) in enumerate(variant_models):
        for mask in range(1 << len(STUB_DIRECTIONS)):
            shape_variants[str((cell_count + k) * 16 + mask)] = shape_variant(
                model_file, stub_files, mask, texture)

    data = {
        "forge_marker": 1,
        "defaults": {
            "model": "%s/mastarmcurve_%s_c0.obj" % (MODEL_PREFIX, preset_id),
            "custom": {"flip-v": True},
            "textures": {"#" + MATERIAL: texture, "particle": texture},
        },
        "variants": {
            "shape": shape_variants,
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

%(body)s

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
   * Bits of a mount mask. Model space, as the curve is drawn facing north with the arm running
   * toward -Z; the blockstate's y rotation carries them round with the block, so model EAST is
   * {@code facing.rotateY()} in the world and model WEST is {@code facing.rotateYCCW()}.
   */
  public static final int MOUNT_DOWN = 1;

  /**
   * @see #MOUNT_DOWN
   */
  public static final int MOUNT_UP = 2;

  /**
   * @see #MOUNT_DOWN
   */
  public static final int MOUNT_EAST = 4;

  /**
   * @see #MOUNT_DOWN
   */
  public static final int MOUNT_WEST = 8;

  /**
   * How many shape values one cell accounts for -- one per mount mask.
   */
  public static final int SHAPE_STRIDE = 16;

  /**
   * How the root cell meets the pole behind it. The joint is generated per pole width, because
   * the arm's tube is 9.2 units across at the root and only the 12-across pole family is wide
   * enough to be saddle-cut against; a thinner pole gets a bolted bracket instead.
   */
  public enum PoleFit {
%(fit_constants)s

    /**
     * The fit for a pole of the given tube radius, in sixteenths of a block. The thresholds are
     * the midpoints between the radii the joints were generated for, so a pole of some other
     * width gets the nearest joint rather than none.
     *
     * @param radius the pole's tube radius, as {@code AbstractBlockTrafficPole#getPoleRadius}
     *               reports it
     *
     * @return the fit to draw
     */
    public static PoleFit forPoleRadius(double radius) {
%(fit_thresholds)s
    }
  }

  /**
   * How many extra root-cell slots the narrow-pole fits take, past the last cell.
   */
  public static final int ROOT_VARIANTS = %(root_variants)d;

  /**
   * Packs a cell index and a mount mask into the single {@code shape} property value the
   * blockstate keys its model and its stub submodels off, for the root on the large pole.
   *
   * <p>One property rather than five is forced, not preferred. A stub model depends on both the
   * cell and the direction, so a mount property would have to carry the cell index; four such
   * properties beside cell and facing come to 1,370,928 block states, and Minecraft builds every
   * one of them eagerly. This packing is 768 -- the same as four plain booleans would have cost.
   *
   * @param cell      the cell index
   * @param mountMask any of {@link #MOUNT_DOWN}, {@link #MOUNT_UP}, {@link #MOUNT_EAST},
   *                  {@link #MOUNT_WEST}, or-ed together
   *
   * @return the packed shape value
   */
  public static int shapeIndex(int cell, int mountMask) {
    return cell * SHAPE_STRIDE + mountMask;
  }

  /**
   * The packed shape value for a cell, taking the pole behind the root into account. Only the
   * root's model depends on the pole, so a narrow-pole fit moves the root to a slot past the
   * last cell and every other cell packs as {@link #shapeIndex(int, int)} does.
   *
   * @param cell      the cell index
   * @param mountMask the mount mask, as for {@link #shapeIndex(int, int)}
   * @param fit       how the root meets its pole; ignored for any cell but the root
   *
   * @return the packed shape value
   */
  public int shapeIndex(int cell, int mountMask, PoleFit fit) {
    int slot = cell;
    if (cell == 0 && fit != PoleFit.LARGE) {
      slot = cells.length + fit.ordinal() - 1;
    }
    return slot * SHAPE_STRIDE + mountMask;
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

  /**
   * The number of distinct {@code shape} values this profile can take: one per cell per mount
   * mask, plus the root's narrow-pole slots.
   *
   * @return the shape count
   */
  public int getShapeCount() {
    return (cells.length + ROOT_VARIANTS) * SHAPE_STRIDE;
  }
}
''' % {"body": "\n".join(body),
       "fit_constants": pole_fit_constants(),
       "fit_thresholds": pole_fit_thresholds(),
       "root_variants": len(POLE_FITS) - 1}
    with open(JAVA_PATH, "w", newline="\n") as fh:
        fh.write(src)


def pole_fit_constants():
    docs = {
        "large": "The 12-across pole family: the arm is saddle-cut against the pole's cylinder "
                 "and swells into a welded boot.",
        "thin": "The 8-across thin pole: the tube ends flat inside a bracket plate strapped "
                "round the pole.",
        "pedestal": "The 6-across pedestal pole: the same bracket, strapped round the thinner "
                    "tube.",
    }
    out = []
    for i, (_suffix, r, label) in enumerate(POLE_FITS):
        out.append("    /** %s Generated for a tube radius of %.0f. */\n    %s%s"
                   % (docs[label], r, label.upper(), "," if i < len(POLE_FITS) - 1 else ";"))
    return "\n".join(out)


def pole_fit_thresholds():
    out = []
    for i in range(len(POLE_FITS) - 1):
        threshold = (POLE_FITS[i][1] + POLE_FITS[i + 1][1]) / 2.0
        out.append("      if (radius >= %.1fD) {\n        return %s;\n      }"
                   % (threshold, POLE_FITS[i][2].upper()))
    out.append("      return %s;" % POLE_FITS[-1][2].upper())
    return "\n".join(out)


if __name__ == "__main__":
    main()
