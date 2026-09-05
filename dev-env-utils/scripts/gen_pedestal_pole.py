#!/usr/bin/env python3
"""
gen_pedestal_pole.py -- the pedestal traffic pole family (``trafficpolepedestal<colour>``).

A pedestal pole is the slim aluminium post that carries a pedestrian signal, a push button or
a small sign at the kerb: a 4.5 in schedule-40 tube threaded into a cast, tapered pedestal
base with an access door, and a domed cap on top. This script generates every asset the
family needs from one description of that shape, so the geometry, the blockstates and the
Java that decides what each block shows cannot drift apart:

  * ``pedestalpole_shaft.obj``          -- the tube, drawn by every block of the pole
  * ``pedestalpole_cap_n/_s.obj``       -- the domed cap, at the model-north or model-south end
  * ``pedestalpole_base_n/_s.obj``      -- the pedestal base, likewise
  * ``pedestalpole_mount.obj``          -- the band-clamp bracket a flank grows toward a device
  * ``pedestalpole_inv.obj``            -- base + tube + cap in one block, for the inventory
  * ``pedestalpole.mtl``                -- one material; each colour's blockstate retextures it
  * ``blockstates/trafficpolepedestal<colour>.json`` for the five pole colours

WHY THE ENDS ARE SEPARATE MODELS
--------------------------------
The pole is one block type stacked to any height, exactly like the thin and thick poles, and
``BlockTrafficPolePedestal.getActualState`` decides per block what each END of its tube shows:
nothing where another pedestal pole continues it, the pedestal base where a vertical pole
stands on the ground, the cap where the tube ends in the open. That is two enum properties,
``endn`` and ``ends``, named for the ends of the tube IN MODEL SPACE (the tube runs north to
south before the ``facing`` rotation stands it up), which is what lets the blockstate stay a
plain per-property Forge blockstate: each value adds a fixed submodel, and the facing rotation
carries it to the right world end. The alternative -- one ``base`` boolean plus a
facing-dependent transform -- is not expressible in that format without enumerating every
combination by hand.

The geometry is authored once in a STANDING frame (Y up, base on the floor, access door on
the south face) and mapped into model space for each end. Which end faces down depends on the
facing: a pole facing UP (the normal result of placing on the ground) has its model-south end
at the bottom, a pole facing DOWN its model-north end. Both mappings are proper rotations, so
the door ends up on the world south face either way.

Coordinates are authored in 1/16 block units and written in 0..1 block space. Vanilla ``x``/``y``
facing rotations pivot about the block centre, so nothing here is centred on the origin.

Usage:
    python gen_pedestal_pole.py            # writes models, blockstates, lang/tab fragments
    python gen_pedestal_pole.py --check    # regenerate into a temp dir and diff against the tree

Requires nothing beyond the standard library.
"""
import argparse
import filecmp
import json
import math
import os
import shutil
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

TRAFFICACCESSORIES_OWNER = layout.owner_of_folder("trafficaccessories")
MODEL_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER,
                                       "models/block/trafficaccessories/shared_models")
BLOCKSTATE_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER, "blockstates")
SCRATCH_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_pedestal_out")

MODEL_PREFIX = "csm:trafficaccessories/shared_models"
MODEL_STEM = "pedestalpole"
MATERIAL = "body"
MTL_NAME = MODEL_STEM + ".mtl"

# Same five finishes, same textures, as the thin poles and the mast arm curves.
COLORS = [
    ("silver", "csm:blocks/trafficsignals/shared_textures/metal_silver", "Silver"),
    ("black", "csm:blocks/trafficsignals/shared_textures/metal_black", "Black"),
    ("tan", "csm:blocks/trafficaccessories/shared_textures/metal_mattetan", "Tan"),
    ("white", "csm:blocks/trafficsignals/shared_textures/metal_mattewhite", "White"),
    ("unpainted", "csm:blocks/trafficsignals/shared_textures/metal_white", "Unpainted"),
]

# --- the shape, in 1/16 block units ----------------------------------------------------------
SIDES = 16            # hexadecagon, the pole family's silhouette
AXIS = 8.0            # tube axis runs through the block centre
R_SHAFT = 3.0         # 6 across: the thin pole is 8 across, a real 4.5 in tube on a real
                      # 8 in pole is 0.56 -- 0.75 is as slim as it can go and still read as a
                      # pole at Minecraft's texel density
INSET = 0.01          # keeps end faces off the block boundary plane exactly

# Cap: a lip a little wider than the tube, then a dome. (radius, height) pairs, bottom up.
CAP_PROFILE = [(R_SHAFT + 0.02, 14.4), (3.5, 14.7), (3.5, 15.2), (3.15, 15.6),
               (2.3, 15.87), (1.2, 15.98), (0.0, 16.0)]

# Pedestal base: square, tapered with a gentle concave flare. (half-width, height) pairs.
# The step at y=1 is the foot's top ledge.
BASE_PROFILE = [(6.2, INSET), (6.2, 1.0), (6.0, 1.0), (5.3, 3.0), (4.6, 6.5), (3.9, 10.0)]
BASE_TOP = 10.0
# Threaded collar the tube screws into, sitting on the base's shoulder.
COLLAR_PROFILE = [(3.6, BASE_TOP), (3.6, 11.3), (3.3, 11.6), (R_SHAFT + 0.02, 11.6)]
# Access door on the south face: x extent, y extent, how far it stands proud of the face.
DOOR_X = (5.4, 10.6)
DOOR_Y = (2.0, 8.8)
DOOR_PROUD = 0.35
DOOR_BOLT = 0.55      # bolt head size (cube), at the door's four corners
DOOR_LATCH = (0.9, 0.9, 0.3)  # w, h, proud -- the latch boss right of centre

# Band-clamp bracket a flank grows toward whatever is mounted beside the pole. Authored
# pointing model NORTH from the axis, like the thin pole's mount; the blockstate rotates it.
MOUNT_PLATE = (5.8, 10.2)   # square plate extent in x and y
MOUNT_PLATE_Z = (4.9, 6.2)  # plate thickness: outer face 3.1 off the axis (the tube's flat
                            # facet is at 2.94), inner face sunk into the tube so the plate's
                            # corners do not float where the tube curves away
MOUNT_ARM_R = 1.4
MOUNT_ARM_SIDES = 8
MOUNT_BOLT = 0.7            # cube, at the plate's corners, standing off the outer face
MOUNT_BOLT_PROUD = 0.45


# --- mesh --------------------------------------------------------------------------------------
class Mesh:
    """Accumulates positions/uvs/normals and triangles for one OBJ file, applying a rigid
    transform ``xf`` (points) / ``nxf`` (normals) to everything added."""

    def __init__(self, xf=None, nxf=None, share=None):
        if share is None:
            self.v, self.vt, self.vn, self.f = [], [], [], []
            self._vi, self._ti, self._ni = {}, {}, {}
        else:
            self.v, self.vt, self.vn, self.f = share.v, share.vt, share.vn, share.f
            self._vi, self._ti, self._ni = share._vi, share._ti, share._ni
        self.xf = xf or (lambda p: p)
        self.nxf = nxf or (lambda n: n)

    def sub(self, xf, nxf):
        """A view onto the same OBJ that authors through a different frame."""
        return Mesh(xf, nxf, share=self)

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
        """verts: three (position, uv, normal) triples. Forge culls back faces, so winding is
        not cosmetic: the triangle is wound to face its analytic vertex normals, which every
        primitive here sets from the geometry, so a face can never come out inside-out."""
        pts = [self.xf(p) for (p, _uv, _n) in verts]
        nrm = [self.nxf(n) for (_p, _uv, n) in verts]
        geo = vcross(vsub(pts[1], pts[0]), vsub(pts[2], pts[0]))
        want = vadd(vadd(nrm[0], nrm[1]), nrm[2])
        agree = vdot(geo, want)
        if abs(agree) < 1e-12:
            raise AssertionError("degenerate triangle or normal: %r" % (pts,))
        uvs = [uv_ for (_p, uv_, _n) in verts]
        if agree < 0:
            pts.reverse()
            nrm.reverse()
            uvs.reverse()
        idx = []
        for p, uv_, n in zip(pts, uvs, nrm):
            idx.append((self._add(self.v, self._vi, p),
                        self._add(self.vt, self._ti, uv_),
                        self._add(self.vn, self._ni, n)))
        self.f.append(idx)

    def quad(self, a, b, c, d):
        self.tri([a, b, c])
        self.tri([a, c, d])

    def quad_out(self, pts, normal, uvs):
        """A quad from four coplanar points in either order; wound so it faces ``normal``."""
        geo = vcross(vsub(pts[1], pts[0]), vsub(pts[2], pts[0]))
        if vdot(geo, normal) < 0:
            pts = list(reversed(pts))
            uvs = list(reversed(uvs))
        n = vnorm(normal)
        self.quad(*[(p, t, n) for p, t in zip(pts, uvs)])

    def write(self, path, name, header=""):
        lines = ["# Procedurally generated by dev-env-utils/scripts/gen_pedestal_pole.py"
                 " -- do not hand edit"]
        if header:
            lines.append("# " + header)
        lines.append("mtllib %s" % MTL_NAME)
        lines.append("o %s" % name)
        for p in self.v:
            lines.append("v %.6f %.6f %.6f" % (p[0] / 16.0, p[1] / 16.0, p[2] / 16.0))
        for t in self.vt:
            lines.append("vt %.6f %.6f" % t)
        for n in self.vn:
            lines.append("vn %.6f %.6f %.6f" % n)
        lines.append("usemtl %s" % MATERIAL)
        for tri in self.f:
            lines.append("f " + " ".join("%d/%d/%d" % i for i in tri))
        with open(path, "w", newline="\n") as fh:
            fh.write("\n".join(lines) + "\n")


def vsub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def vadd(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def vdot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def vcross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def vnorm(a):
    l = math.sqrt(vdot(a, a))
    return (a[0] / l, a[1] / l, a[2] / l) if l > 0 else (0.0, 1.0, 0.0)


def pingpong(x):
    """Triangle wave into [0,1]: mirrored tiling keeps UVs inside the sprite (the block atlas
    has no wrap mode) at roughly constant texel density along any length."""
    x = x % 2.0
    return x if x <= 1.0 else 2.0 - x


def uv(u_frac, v_frac):
    """Map into the sprite's middle half, away from the border where CUTOUT_MIPPED mip levels
    bleed into neighbouring atlas entries. flip-v is on, so v is written inverted."""
    u = 0.25 + 0.5 * pingpong(u_frac)
    v = 0.25 + 0.5 * pingpong(v_frac)
    return (u, 1.0 - v)


# --- frames --------------------------------------------------------------------------------------
# Standing frame: Y up, tube axis at (AXIS, *, AXIS), door on +Z (south).
# Model frame: tube along Z. The vanilla facing rotation then maps model north (z=0) to the
# facing direction, so a pole facing UP has its model-SOUTH end on the ground and a pole
# facing DOWN its model-NORTH end. Both maps below are proper rotations (det +1) about the
# block centre, so winding and normals survive unchanged in handedness.
def to_model_south(p):
    """Standing frame -> model frame with the floor at model z=16 (facing UP's bottom), and
    therefore the standing TOP at model z=0: this is the map for base_s and for cap_n."""
    x, y, z = p
    return (x, z, 16.0 - y)


def to_model_south_n(n):
    x, y, z = n
    return (x, z, -y)


def to_model_north(p):
    """Standing frame -> model frame with the floor at model z=0 (facing DOWN's bottom), and
    therefore the standing TOP at model z=16: this is the map for base_n and for cap_s."""
    x, y, z = p
    return (x, 16.0 - z, y)


def to_model_north_n(n):
    x, y, z = n
    return (x, -z, y)


# --- primitives (standing frame) ------------------------------------------------------------------
def ring_angles(sides):
    """Vertex angles offset by half a step so a flat facet faces each compass direction; that
    is what lets a flat clamp plate sit on the tube instead of straddling a vertex."""
    step = 2.0 * math.pi / sides
    return [step * (i + 0.5) for i in range(sides)]


def lathe(mesh, profile, sides=SIDES, cx=AXIS, cz=AXIS, bottom_disc=False, top_disc=False,
          v_scale=1.0 / 16.0):
    """Surface of revolution about the vertical line (cx, *, cz). ``profile`` is a list of
    (radius, y) pairs walked from bottom to top; a radius of 0 makes a pole. Normals come from
    the profile's slope, so a dome shades as a dome and a lip shows its underside."""
    angles = ring_angles(sides)
    rings = []
    for k, (r, y) in enumerate(profile):
        # slope normal: tangent along the profile is (dr, dy); outward normal is (dy, -dr)
        prev_r, prev_y = profile[max(k - 1, 0)]
        next_r, next_y = profile[min(k + 1, len(profile) - 1)]
        dr, dy = next_r - prev_r, next_y - prev_y
        if abs(dr) < 1e-9 and abs(dy) < 1e-9:
            dr, dy = 0.0, 1.0
        nr, ny = dy, -dr
        ln = math.hypot(nr, ny)
        nr, ny = nr / ln, ny / ln
        ring = []
        for a in angles:
            ring.append(((cx + r * math.cos(a), y, cz + r * math.sin(a)),
                         (nr * math.cos(a), ny, nr * math.sin(a))))
        rings.append(ring)
    for k in range(len(profile) - 1):
        (ra, ya), (rb, yb) = profile[k], profile[k + 1]
        if abs(ra) < 1e-9 and abs(rb) < 1e-9:
            continue
        lo, hi = rings[k], rings[k + 1]
        va, vb = ya * v_scale, yb * v_scale
        for i in range(sides):
            j = (i + 1) % sides
            ua, ub = i / sides, (i + 1) / sides
            if rb < 1e-9:      # cone to a point at the top
                mesh.tri([(lo[i][0], uv(ua, va), lo[i][1]),
                          (lo[j][0], uv(ub, va), lo[j][1]),
                          ((cx, yb, cz), uv(0.5 * (ua + ub), vb), hi[i][1])])
            elif ra < 1e-9:    # cone from a point at the bottom
                mesh.tri([((cx, ya, cz), uv(0.5 * (ua + ub), va), lo[i][1]),
                          (hi[j][0], uv(ub, vb), hi[j][1]),
                          (hi[i][0], uv(ua, vb), hi[i][1])])
            else:
                mesh.quad((lo[i][0], uv(ua, va), lo[i][1]),
                          (lo[j][0], uv(ub, va), lo[j][1]),
                          (hi[j][0], uv(ub, vb), hi[j][1]),
                          (hi[i][0], uv(ua, vb), hi[i][1]))
    if bottom_disc:
        disc(mesh, profile[0][0], profile[0][1], (0.0, -1.0, 0.0), sides, cx, cz)
    if top_disc:
        disc(mesh, profile[-1][0], profile[-1][1], (0.0, 1.0, 0.0), sides, cx, cz)


def disc(mesh, r, y, normal, sides=SIDES, cx=AXIS, cz=AXIS):
    angles = ring_angles(sides)
    n = vnorm(normal)
    centre = ((cx, y, cz), uv(0.5, 0.5), n)
    for i in range(sides):
        j = (i + 1) % sides
        pa = (cx + r * math.cos(angles[i]), y, cz + r * math.sin(angles[i]))
        pb = (cx + r * math.cos(angles[j]), y, cz + r * math.sin(angles[j]))
        ta = uv(0.5 + 0.5 * math.cos(angles[i]), 0.5 + 0.5 * math.sin(angles[i]))
        tb = uv(0.5 + 0.5 * math.cos(angles[j]), 0.5 + 0.5 * math.sin(angles[j]))
        tri = [centre, (pa, ta, n), (pb, tb, n)]
        geo = vcross(vsub(pa, centre[0]), vsub(pb, centre[0]))
        if vdot(geo, n) < 0:
            tri = [centre, (pb, tb, n), (pa, ta, n)]
        mesh.tri(tri)


def box(mesh, lo, hi, faces=("x-", "x+", "y-", "y+", "z-", "z+")):
    """Axis-aligned box in the current frame, outward faces only as requested."""
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    q = [uv(0, 0), uv(1, 0), uv(1, 1), uv(0, 1)]
    if "x-" in faces:
        mesh.quad_out([(x0, y0, z0), (x0, y1, z0), (x0, y1, z1), (x0, y0, z1)], (-1, 0, 0), q)
    if "x+" in faces:
        mesh.quad_out([(x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (x1, y0, z1)], (1, 0, 0), q)
    if "y-" in faces:
        mesh.quad_out([(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)], (0, -1, 0), q)
    if "y+" in faces:
        mesh.quad_out([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], (0, 1, 0), q)
    if "z-" in faces:
        mesh.quad_out([(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)], (0, 0, -1), q)
    if "z+" in faces:
        mesh.quad_out([(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], (0, 0, 1), q)


def square_frustum(mesh, profile, cx=AXIS, cz=AXIS, top_face=True, bottom_face=True):
    """Stack of square sections about (cx, *, cz): ``profile`` is (half-width, y) pairs,
    bottom up. Consecutive pairs with equal y make a horizontal ledge."""
    # side s: 0 = +Z (south), 1 = +X (east), 2 = -Z (north), 3 = -X (west)
    outward = [(0, 0, 1), (1, 0, 0), (0, 0, -1), (-1, 0, 0)]

    def corner(h, y, s, end):
        # the side's two bottom corners, walked left to right seen from outside
        if s == 0:
            return (cx - h, y, cz + h) if end == 0 else (cx + h, y, cz + h)
        if s == 1:
            return (cx + h, y, cz + h) if end == 0 else (cx + h, y, cz - h)
        if s == 2:
            return (cx + h, y, cz - h) if end == 0 else (cx - h, y, cz - h)
        return (cx - h, y, cz - h) if end == 0 else (cx - h, y, cz + h)

    for k in range(len(profile) - 1):
        (ha, ya), (hb, yb) = profile[k], profile[k + 1]
        if abs(ya - yb) < 1e-9:
            # ledge: a flat ring between the two squares
            n = (0, 1, 0) if hb < ha else (0, -1, 0)
            for s in range(4):
                pts = [corner(ha, ya, s, 0), corner(ha, ya, s, 1),
                       corner(hb, yb, s, 1), corner(hb, yb, s, 0)]
                mesh.quad_out(pts, n, [uv(0, 0), uv(1, 0), uv(1, 0.1), uv(0, 0.1)])
            continue
        dh, dy = hb - ha, yb - ya
        for s in range(4):
            ox, _oy, oz = outward[s]
            # outward normal tilted by the taper: horizontal component dy, vertical -dh
            n = vnorm((ox * dy, -dh, oz * dy))
            pts = [corner(ha, ya, s, 0), corner(ha, ya, s, 1),
                   corner(hb, yb, s, 1), corner(hb, yb, s, 0)]
            va, vb = ya / 16.0, yb / 16.0
            mesh.quad_out(pts, n, [uv(0, va), uv(1, va), uv(1, vb), uv(0, vb)])
    if top_face:
        h, y = profile[-1]
        mesh.quad_out([(cx - h, y, cz - h), (cx + h, y, cz - h),
                       (cx + h, y, cz + h), (cx - h, y, cz + h)], (0, 1, 0),
                      [uv(0, 0), uv(1, 0), uv(1, 1), uv(0, 1)])
    if bottom_face:
        h, y = profile[0]
        mesh.quad_out([(cx - h, y, cz - h), (cx + h, y, cz - h),
                       (cx + h, y, cz + h), (cx - h, y, cz + h)], (0, -1, 0),
                      [uv(0, 0), uv(1, 0), uv(1, 1), uv(0, 1)])


def base_half_at(y):
    """Half-width of the pedestal base's tapered body at height y (piecewise linear)."""
    body = [p for p in BASE_PROFILE if p[1] >= 1.0 - 1e-9]
    for (ha, ya), (hb, yb) in zip(body, body[1:]):
        if ya - 1e-9 <= y <= yb + 1e-9 and yb > ya:
            t = (y - ya) / (yb - ya)
            return ha + (hb - ha) * t
    raise ValueError("y outside the base body: %r" % y)


def emit_door(mesh):
    """Access door on the south face: a plate standing proud of the taper, following it, with
    four corner bolts and a latch boss. Built from the same profile as the face it sits on."""
    x0, x1 = DOOR_X
    y0, y1 = DOOR_Y
    levels = sorted({y0, y1} | {p[1] for p in BASE_PROFILE if y0 < p[1] < y1})
    face = [(y, AXIS + base_half_at(y)) for y in levels]           # (y, z of the face)
    outer = [(y, z + DOOR_PROUD) for (y, z) in face]
    # front plate
    for (ya, za), (yb, zb) in zip(outer, outer[1:]):
        n = vnorm((0, -(zb - za), yb - ya))
        mesh.quad_out([(x0, ya, za), (x1, ya, za), (x1, yb, zb), (x0, yb, zb)], n,
                      [uv(0, ya / 16), uv(0.3, ya / 16), uv(0.3, yb / 16), uv(0, yb / 16)])
    # side walls
    for (ya, za), (yb, zb) in zip(face, face[1:]):
        oa, ob = za + DOOR_PROUD, zb + DOOR_PROUD
        mesh.quad_out([(x0, ya, za), (x0, ya, oa), (x0, yb, ob), (x0, yb, zb)], (-1, 0, 0),
                      [uv(0, 0), uv(0.05, 0), uv(0.05, 0.4), uv(0, 0.4)])
        mesh.quad_out([(x1, ya, za), (x1, ya, oa), (x1, yb, ob), (x1, yb, zb)], (1, 0, 0),
                      [uv(0, 0), uv(0.05, 0), uv(0.05, 0.4), uv(0, 0.4)])
    # top and bottom walls
    yb_, zb_ = face[0]
    yt_, zt_ = face[-1]
    mesh.quad_out([(x0, yb_, zb_), (x1, yb_, zb_), (x1, yb_, zb_ + DOOR_PROUD),
                   (x0, yb_, zb_ + DOOR_PROUD)], (0, -1, 0),
                  [uv(0, 0), uv(0.3, 0), uv(0.3, 0.05), uv(0, 0.05)])
    mesh.quad_out([(x0, yt_, zt_), (x1, yt_, zt_), (x1, yt_, zt_ + DOOR_PROUD),
                   (x0, yt_, zt_ + DOOR_PROUD)], (0, 1, 0),
                  [uv(0, 0), uv(0.3, 0), uv(0.3, 0.05), uv(0, 0.05)])
    # corner bolts and latch: small boxes standing off the plate, sunk slightly into it so
    # they cannot float where the plate slopes
    b = DOOR_BOLT
    for bx in (x0 + 0.5, x1 - 0.5 - b):
        for by in (y0 + 0.5, y1 - 0.5 - b):
            zc = AXIS + base_half_at(by + b / 2) + DOOR_PROUD
            box(mesh, (bx, by, zc - 0.15), (bx + b, by + b, zc + 0.3),
                faces=("x-", "x+", "y-", "y+", "z+"))
    lw, lh, lp = DOOR_LATCH
    ly = (y0 + y1) / 2 - lh / 2
    lx = x1 - 1.6 - lw
    zc = AXIS + base_half_at(ly + lh / 2) + DOOR_PROUD
    box(mesh, (lx, ly, zc - 0.15), (lx + lw, ly + lh, zc + lp),
        faces=("x-", "x+", "y-", "y+", "z+"))


# --- parts -------------------------------------------------------------------------------------------
def build_shaft(mesh):
    """The tube, full block length, with end discs a hair inside the block so nothing sits on
    the boundary plane. Everything that hides an end (cap, base, the next pole) covers them."""
    lathe(mesh, [(R_SHAFT, INSET), (R_SHAFT, 16.0 - INSET)], bottom_disc=True, top_disc=True)


def build_cap(mesh):
    lathe(mesh, CAP_PROFILE)


def build_base(mesh):
    square_frustum(mesh, BASE_PROFILE, top_face=True, bottom_face=True)
    lathe(mesh, COLLAR_PROFILE)          # no bottom disc: it would lie on the base's top face
    emit_door(mesh)


def build_mount(mesh):
    """Model frame, pointing north (-Z) from the axis: clamp plate against the tube, bolts at
    its corners, and the arm out to the face. Symmetric about its own axis on purpose -- the
    blockstate rotates one model into all four flank directions, and the tube runs along X in
    two of those rotations and Y in the other two, so nothing here may prefer either."""
    px0, px1 = MOUNT_PLATE
    pz0, pz1 = MOUNT_PLATE_Z
    box(mesh, (px0, px0, pz0), (px1, px1, pz1))
    b = MOUNT_BOLT
    for bx in (px0 + 0.35, px1 - 0.35 - b):
        for by in (px0 + 0.35, px1 - 0.35 - b):
            box(mesh, (bx, by, pz0 - MOUNT_BOLT_PROUD), (bx + b, by + b, pz0 + 0.1),
                faces=("x-", "x+", "y-", "y+", "z-"))
    # arm: the lathe turns about Y, and to_model_north sends standing y to model z, so a
    # standing tube from y=INSET to the plate becomes the arm from the north face (z=INSET)
    # to the plate, centred on the block. Its far end is buried in the plate, so no disc.
    lathe(mesh.sub(to_model_north, to_model_north_n),
          [(MOUNT_ARM_R, INSET), (MOUNT_ARM_R, pz0 + 0.2)], sides=MOUNT_ARM_SIDES,
          bottom_disc=True)


def build_inventory(mesh):
    """Whole pole in one block, standing: base, tube, cap."""
    square_frustum(mesh, BASE_PROFILE, top_face=True, bottom_face=True)
    lathe(mesh, COLLAR_PROFILE)
    emit_door(mesh)
    lathe(mesh, [(R_SHAFT, 11.0), (R_SHAFT, CAP_PROFILE[0][1] + 0.1)])
    lathe(mesh, CAP_PROFILE)


# --- outputs -------------------------------------------------------------------------------------------
def registry_name(color_id):
    return "trafficpole" + "pedestal" + color_id


def model_ref(part):
    return "%s/%s_%s.obj" % (MODEL_PREFIX, MODEL_STEM, part)


def submodel(part, texture, rotation=None):
    entry = {
        "model": model_ref(part),
        "custom": {"flip-v": True},
        "textures": {"#" + MATERIAL: texture},
    }
    if rotation is not None:
        rx, ry, rz = rotation
        entry["transform"] = {"rotation": [{"x": rx}, {"y": ry}, {"z": rz}]}
    return entry


def blockstate(texture):
    """Per-property Forge blockstate. The four mount transforms are the thin pole's, verbatim:
    Forge submodel rotations are right-handed where the vanilla facing x/y are not, which is
    why ``mounteast`` (a block to the pole's relative west, per BlockUtils) is drawn with
    y=90 and comes out pointing the right way. Do not 'fix' the signs."""
    return {
        "forge_marker": 1,
        "defaults": {
            "model": model_ref("shaft"),
            "custom": {"flip-v": True},
            "textures": {"#" + MATERIAL: texture, "particle": texture},
        },
        "variants": {
            "inventory": [{
                "model": model_ref("inv"),
                "custom": {"flip-v": True},
                "textures": {"#" + MATERIAL: texture},
                "transform": "forge:default-block",
            }],
            "facing": {
                "north": {},
                "east": {"y": 90},
                "south": {"y": 180},
                "west": {"y": 270},
                "up": {"x": 270},
                "down": {"x": 90},
            },
            "mounteast": {"false": {}, "true": {
                "submodel": {"mounte": submodel("mount", texture, (0, 90, 0))}}},
            "mountwest": {"false": {}, "true": {
                "submodel": {"mountw": submodel("mount", texture, (0, 270, 0))}}},
            "mountup": {"false": {}, "true": {
                "submodel": {"mountu": submodel("mount", texture, (270, 0, 0))}}},
            "mountdown": {"false": {}, "true": {
                "submodel": {"mountd": submodel("mount", texture, (90, 0, 0))}}},
            "endn": {
                "none": {},
                "cap": {"submodel": {"cap_n": submodel("cap_n", texture)}},
                "base": {"submodel": {"base_n": submodel("base_n", texture)}},
            },
            "ends": {
                "none": {},
                "cap": {"submodel": {"cap_s": submodel("cap_s", texture)}},
                "base": {"submodel": {"base_s": submodel("base_s", texture)}},
            },
        },
    }


def generate(model_dir, blockstate_dir, scratch_dir):
    os.makedirs(model_dir, exist_ok=True)
    os.makedirs(blockstate_dir, exist_ok=True)
    os.makedirs(scratch_dir, exist_ok=True)

    with open(os.path.join(model_dir, MTL_NAME), "w", newline="\n") as fh:
        fh.write("# Procedurally generated by gen_pedestal_pole.py -- do not hand edit\n"
                 "# One material; every colour's blockstate retextures #%s.\n"
                 "newmtl %s\nmap_Kd %s\n" % (MATERIAL, MATERIAL, COLORS[0][1]))

    # Every part but the mount and the inventory icon is authored standing and mapped into
    # model space; the shaft is symmetric so either map would do. Mind which end of the
    # STANDING piece has to land at the named model end: a base sits at the standing floor,
    # so base_n takes the map that puts the floor at model north; a cap sits at the standing
    # top, so cap_n takes the OTHER map, the one that puts the floor at model south. Getting
    # this backwards renders the cap upside down at the far end of the block -- a ring one
    # block below the top of the pole, and a flat tube end where the dome should be.
    parts = {
        "shaft": (build_shaft, to_model_south, to_model_south_n, "tube, along Z"),
        "cap_n": (build_cap, to_model_south, to_model_south_n, "domed cap at the model-north end"),
        "cap_s": (build_cap, to_model_north, to_model_north_n, "domed cap at the model-south end"),
        "base_n": (build_base, to_model_north, to_model_north_n,
                   "pedestal base at the model-north end (a pole facing DOWN stands on it)"),
        "base_s": (build_base, to_model_south, to_model_south_n,
                   "pedestal base at the model-south end (a pole facing UP stands on it)"),
        "mount": (build_mount, None, None, "band-clamp bracket, pointing model north"),
        "inv": (build_inventory, None, None, "inventory icon: whole pole standing in one block"),
    }
    for part, (builder, xf, nxf, header) in parts.items():
        mesh = Mesh(xf, nxf)
        builder(mesh)
        lo, hi = mesh_bounds(mesh)
        assert all(-0.02 <= c <= 16.02 for c in lo + hi), (part, lo, hi)
        mesh.write(os.path.join(model_dir, "%s_%s.obj" % (MODEL_STEM, part)),
                   "%s_%s" % (MODEL_STEM, part), header)

    lang_lines, tab_lines = [], []
    for color_id, texture, color_label in COLORS:
        name = registry_name(color_id)
        with open(os.path.join(blockstate_dir, name + ".json"), "w", newline="\n") as fh:
            json.dump(blockstate(texture), fh, indent=2)
            fh.write("\n")
        lang_lines.append("tile.%s.name=%s Pedestal Traffic Pole" % (name, color_label))
        tab_lines.append('    initTabBlock(new BlockTrafficPolePedestal("%s", '
                         'TRAFFIC_POLE_COLOR.%s));' % (name, color_id.upper()))
    with open(os.path.join(scratch_dir, "lang_fragment.txt"), "w", newline="\n") as fh:
        fh.write("\n".join(lang_lines) + "\n")
    with open(os.path.join(scratch_dir, "tab_fragment.txt"), "w", newline="\n") as fh:
        fh.write("\n".join(tab_lines) + "\n")
    return sorted(parts), [registry_name(c[0]) for c in COLORS]


def mesh_bounds(mesh):
    xs = [p[0] for p in mesh.v]
    ys = [p[1] for p in mesh.v]
    zs = [p[2] for p in mesh.v]
    return (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true",
                    help="regenerate into a temp dir and fail if the tree differs")
    args = ap.parse_args()

    if not args.check:
        parts, names = generate(MODEL_DIR, BLOCKSTATE_DIR, SCRATCH_DIR)
        print("wrote %d OBJ parts + %s to %s" % (len(parts), MTL_NAME, MODEL_DIR))
        print("wrote %d blockstates to %s" % (len(names), BLOCKSTATE_DIR))
        print("lang/tab fragments in %s" % SCRATCH_DIR)
        return 0

    tmp = tempfile.mkdtemp(prefix="pedestal_")
    try:
        parts, names = generate(os.path.join(tmp, "m"), os.path.join(tmp, "b"),
                                os.path.join(tmp, "s"))
        stale = []
        for part in parts + ["__mtl__"]:
            fn = MTL_NAME if part == "__mtl__" else "%s_%s.obj" % (MODEL_STEM, part)
            if not filecmp.cmp(os.path.join(tmp, "m", fn), os.path.join(MODEL_DIR, fn),
                               shallow=False):
                stale.append(fn)
        for name in names:
            fn = name + ".json"
            if not filecmp.cmp(os.path.join(tmp, "b", fn), os.path.join(BLOCKSTATE_DIR, fn),
                               shallow=False):
                stale.append(fn)
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("pedestal pole assets are up to date")
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
