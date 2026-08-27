#!/usr/bin/env python3
"""
Procedural generator for the CSM decorative lighting family -- pendants and wall sconces.

The lighting tab had no decorative fixtures at all: its only residential ceiling lights were two
0.29-unit-thick wafers, and its only exterior wall fixtures were two grey boxes. This adds ten
fixtures that are mostly surfaces of revolution -- bell shades, globes, domes, cones, lantern
bodies -- which a Forge JSON element model cannot express, because JSON elements are axis-aligned
boxes that rotate only at +/-22.5 or +/-45 degrees about ONE axis. So these are OBJ, following
gen_firealarm_obj.py.

Emits into assets/csm/models/block/lighting/shared_models/ (plus one MTL each), and the shared
swatch textures they wear into assets/csm/textures/blocks/lighting/shared_textures/.

THREE MATERIALS PER MODEL, which is the whole reason this family works as one mesh per fixture:

  * `body`  -- the metal. Each finish variant's blockstate overrides `#body` with
               finish_black / finish_bronze / finish_nickel, so one OBJ serves all three.
  * `shade` -- a constant secondary surface (the white enamel inside a dome shade, candle wax).
               Never changes with finish or state.
  * `lens`  -- the part that lights up. Each blockstate's STATE variants override `#lens` with
               lens_*_on / lens_*_off, so a fixture that is off actually LOOKS off. 83 of the 86
               fixtures already in the tab do not do this; these ten do.

Forge's OBJModel.retexture keys overrides on `#` + the MTL material name, and re-collects textures
after Variant.process, so overrides are stitched into the atlas normally.

Coordinate convention (matches gen_firealarm_obj.py and Forge's block space):
  * 1 unit == 1 block; written 0..1 with the block min corner at the origin, so a blockstate
    x/y rotation -- which pivots about (0.5, 0.5, 0.5) -- lands where it should.
  * Authored here in Minecraft's familiar 0..16 model units and divided at write time.
  * -Z is north. Wall fixtures put their BACK PLATE at z = 16, because
    AbstractBlockRotatableNSEW.getStateForPlacement sets FACING to the placer's horizontal facing
    OPPOSITE -- so the plate ends up against the wall the player was looking at, matching altomvwl.
  * Faces are triangles `f v/vt/vn`, wound so every normal points outward; Forge culls back faces.

Every swatch texture is a VERTICAL GRADIENT and every lathe maps v along the profile, ordered from
the neck down to the mouth. So a shade is automatically dimmer where it meets its fitter and
brightest at its opening, which is what a lit shade looks like, with no per-fixture UV work.

UVs are written for `custom: {"flip-v": true}`: Minecraft UV rows run top-down, OBJ v runs
bottom-up, so vt_v = 1 - V/16.

Run:  python dev-env-utils/scripts/gen_decorative_lighting.py
"""

import json
import math
import os
import random

from PIL import Image

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "csm")
OUT_DIR = os.path.join(ASSETS, "models", "block", "lighting", "shared_models")
TEX_DIR = os.path.join(ASSETS, "textures", "blocks", "lighting", "shared_textures")

BODY, SHADE, LENS = "body", "shade", "lens"

#: Where a wall fixture's geometry may sit instead of exactly on z = 16.
#:
#: Anything drawn on a block boundary is coplanar with the neighbouring block's own face. If both
#: point the same way -- which a wall fixture's backward-facing geometry and the wall's inward face
#: both do -- the depth buffer cannot separate them and the surface shimmers. A tenth of a unit is
#: 1/160 of a block: far too small to see, far too large for the depth buffer to confuse.
WALL_STANDOFF = 15.9

#: The same idea for one part sitting on another's face rather than on a block boundary.
COPLANAR_NUDGE = 0.15

TAU = 2.0 * math.pi

#: Round shapes are lathed at this many segments. 16 reads as a circle at block scale without
#: making the meshes silly; stems and other thin tubes use fewer.
SEGMENTS = 16
STEM_SEGMENTS = 8

#: Per-axis basis. `a` and `b` span the circle, `axis` is what the profile advances along.
AXES = {
    "x": ((0.0, 1.0, 0.0), (0.0, 0.0, 1.0), (1.0, 0.0, 0.0)),
    "y": ((1.0, 0.0, 0.0), (0.0, 0.0, 1.0), (0.0, 1.0, 0.0)),
    "z": ((1.0, 0.0, 0.0), (0.0, 1.0, 0.0), (0.0, 0.0, 1.0)),
}


# --------------------------------------------------------------------------- mesh
class Mesh:
    """Accumulates triangles and writes them out as an OBJ with several materials.

    Same shape as gen_firealarm_obj.py's Mesh -- deliberately, so the two generators stay legible
    side by side -- but multi-material is the normal case here rather than the exception.
    """

    def __init__(self):
        self.positions = []
        self.texcoords = []
        self.normals = []
        self.faces = []

    def _position(self, point):
        self.positions.append(point)
        return len(self.positions)

    def _texcoord(self, uv):
        # Clamp a hair inside the sprite. Minecraft's atlas bleeds neighbouring sprites into the
        # outermost texel row when mipmapping, and these are CUTOUT_MIPPED blocks.
        self.texcoords.append((min(15.99, max(0.01, uv[0])) / 16.0,
                               1.0 - min(15.99, max(0.01, uv[1])) / 16.0))
        return len(self.texcoords)

    def _normal(self, normal):
        self.normals.append(normal)
        return len(self.normals)

    def triangle(self, p1, p2, p3, uv1, uv2, uv3, outward, material=BODY):
        """Emit one triangle, reversing the winding if it faces away from `outward`."""
        normal = _face_normal(p1, p2, p3)
        if _dot(normal, outward) < 0.0:
            p2, p3 = p3, p2
            uv2, uv3 = uv3, uv2
            normal = _face_normal(p1, p2, p3)
        index = self._normal(normal)
        self.faces.append((material,
                           [(self._position(p1), self._texcoord(uv1), index),
                            (self._position(p2), self._texcoord(uv2), index),
                            (self._position(p3), self._texcoord(uv3), index)]))

    def quad(self, p1, p2, p3, p4, uv1, uv2, uv3, uv4, outward, material=BODY):
        self.triangle(p1, p2, p3, uv1, uv2, uv3, outward, material)
        self.triangle(p1, p3, p4, uv1, uv3, uv4, outward, material)

    def bounds(self):
        """(minX, minY, minZ, maxX, maxY, maxZ) in 0..16 units."""
        xs = [p[0] for p in self.positions]
        ys = [p[1] for p in self.positions]
        zs = [p[2] for p in self.positions]
        return (min(xs), min(ys), min(zs), max(xs), max(ys), max(zs))

    def write(self, path, name, textures, header):
        """Write the OBJ and its MTL. `textures` maps material name -> default texture ref."""
        used = [m for m in (BODY, SHADE, LENS) if any(f[0] == m for f in self.faces)]
        mtl_name = os.path.splitext(os.path.basename(path))[0] + ".mtl"
        with open(os.path.join(os.path.dirname(path), mtl_name), "w", newline="\n") as handle:
            handle.write("# Procedurally generated by gen_decorative_lighting.py\n")
            for material in used:
                handle.write("newmtl %s\nmap_Kd %s\n" % (material, textures[material]))
        with open(path, "w", newline="\n") as handle:
            handle.write("# Procedurally generated by "
                         "dev-env-utils/scripts/gen_decorative_lighting.py\n")
            handle.write("# %s\n" % header)
            handle.write("mtllib %s\no %s\n" % (mtl_name, name))
            for x, y, z in self.positions:
                handle.write("v %.6f %.6f %.6f\n" % (x / 16.0, y / 16.0, z / 16.0))
            for u, v in self.texcoords:
                handle.write("vt %.6f %.6f\n" % (u, v))
            for x, y, z in self.normals:
                handle.write("vn %.6f %.6f %.6f\n" % (x, y, z))
            for material in used:
                handle.write("usemtl %s\n" % material)
                for corners in [c for m, c in self.faces if m == material]:
                    handle.write("f " + " ".join("%d/%d/%d" % c for c in corners) + "\n")
        return len(self.faces)


def _dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def _add(*vectors):
    return tuple(sum(v[i] for v in vectors) for i in range(3))


def _scale(vector, k):
    return (vector[0] * k, vector[1] * k, vector[2] * k)


def _face_normal(p1, p2, p3):
    ux, uy, uz = p2[0] - p1[0], p2[1] - p1[1], p2[2] - p1[2]
    vx, vy, vz = p3[0] - p1[0], p3[1] - p1[1], p3[2] - p1[2]
    nx, ny, nz = uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx
    length = math.sqrt(nx * nx + ny * ny + nz * nz)
    if length < 1e-9:
        return (0.0, 0.0, -1.0)
    return (nx / length, ny / length, nz / length)


# --------------------------------------------------------------------------- lathe
def lathe(mesh, profile, material, segments=SEGMENTS, centre=(8.0, 8.0), axis="y",
          angle_span=(0.0, TAU), u_span=(0.5, 15.5), v_span=(0.5, 15.5),
          close_start=False, close_end=False, close_sides=False,
          cap_material=None, side_material=None, flip=False):
    """Surface of revolution.

    `profile` is [(radius, along), ...] in 0..16 units, ordered START -> END. For a shade, order it
    neck -> mouth: v runs with the profile, and the swatch textures are vertical gradients whose
    top is the dim end.

    `centre` is the two coordinates the axis passes through, in the order given by AXES[axis][0:2]
    -- so (x, z) for axis "y", (x, y) for axis "z", (y, z) for axis "x".

    `angle_span` less than a full turn leaves the shape open along two flat cuts; `close_sides`
    fills them, which is how the half-shell sconce gets its flat back against the wall.

    `flip` inverts the outward direction, for a surface seen from inside (a shade's enamel liner).
    """
    e_a, e_b, e_axis = AXES[axis]
    base = _add(_scale(e_a, centre[0]), _scale(e_b, centre[1]))
    a0, a1 = angle_span
    closed = abs((a1 - a0) - TAU) < 1e-9

    def radial(angle):
        return _add(_scale(e_a, math.cos(angle)), _scale(e_b, math.sin(angle)))

    def point(radius, along, angle):
        return _add(base, _scale(e_axis, along), _scale(radial(angle), radius))

    angles = [a0 + (a1 - a0) * i / segments for i in range(segments + 1)]
    rings = [[point(r, along, a) for a in angles] for r, along in profile]

    u0, u1 = u_span
    v0, v1 = v_span
    steps = max(1, len(profile) - 1)

    # Which perpendicular is "outward" depends on the direction the profile was authored in, and it
    # has to be decided for the SURFACE, not per band. Deciding per band is what a rule like
    # "always give the radial component a positive sign" amounts to, and it is wrong for any
    # profile that curves back on itself: on a torus the inner half of the tube genuinely faces
    # TOWARDS the axis, and forcing it outward flips the winding on half of every link.
    #
    # So: take the raw perpendicular (da, -dr), weight each band's radial component by how far out
    # it sits, and if the surface as a whole then faces inward, reverse the lot. A shade authored
    # neck-first gets reversed; a torus does not, because its outer half has the greater radius and
    # therefore the greater say.
    lean = sum((profile[i + 1][1] - profile[i][1]) * (profile[i + 1][0] + profile[i][0])
               for i in range(len(profile) - 1))
    sense = 1.0 if lean >= 0.0 else -1.0
    for r in range(len(rings) - 1):
        lower, upper = rings[r], rings[r + 1]
        v_lo = v0 + (v1 - v0) * r / steps
        v_hi = v0 + (v1 - v0) * (r + 1) / steps
        # The outward reference has to lean the way the profile leans. A purely radial one is
        # perpendicular to the true normal wherever the profile runs flat -- the top of a dome, the
        # lip of a bowl -- so the winding test there decides on a dot product near zero and gets it
        # wrong as often as right. Those faces then cull from outside and the shade has a hole in
        # it when you look down on it.
        #
        # In the (radius, along) plane the normal is perpendicular to the profile tangent (dr, da),
        # which is (da, -dr). Which of the two perpendiculars that is depends on which way the
        # profile was authored, so `sense` below settles it once for the whole surface rather than
        # per band -- see the note where it is computed.
        dr = (profile[r + 1][0] - profile[r][0]) * sense
        da = (profile[r + 1][1] - profile[r][1]) * sense
        radial_weight, axial = da, -dr
        for i in range(segments):
            u_lo = u0 + (u1 - u0) * i / segments
            u_hi = u0 + (u1 - u0) * (i + 1) / segments
            p1, p2, p3, p4 = lower[i], lower[i + 1], upper[i + 1], upper[i]
            mid_angle = (angles[i] + angles[i + 1]) / 2.0
            outward = _normalise(_add(_scale(radial(mid_angle), radial_weight),
                                      _scale(e_axis, axial)))
            if flip:
                outward = _scale(outward, -1.0)
            mesh.quad(p1, p2, p3, p4,
                      (u_lo, v_lo), (u_hi, v_lo), (u_hi, v_hi), (u_lo, v_hi),
                      outward, material)

    caps = cap_material or material
    # A cap faces AWAY from the rest of the profile, which is not always +/- the axis direction:
    # a shade is authored neck-first, so its profile descends and its mouth cap must face down.
    if close_start:
        rising = 1.0 if profile[1][1] >= profile[0][1] else -1.0
        _cap(mesh, rings[0], profile[0][0], profile[0][1], base, e_axis, angles, caps,
             outward=_scale(e_axis, -rising))
    if close_end:
        rising = 1.0 if profile[-1][1] >= profile[-2][1] else -1.0
        _cap(mesh, rings[-1], profile[-1][0], profile[-1][1], base, e_axis, angles, caps,
             outward=_scale(e_axis, rising))
    if close_sides and not closed:
        sides = side_material or material
        for angle, sign in ((a0, 1.0), (a1, -1.0)):
            index = 0 if angle == a0 else segments
            tangent = _add(_scale(e_a, -math.sin(angle)), _scale(e_b, math.cos(angle)))
            _side(mesh, [ring[index] for ring in rings], profile, base, e_axis,
                  _scale(tangent, sign), sides)


def _cap(mesh, ring, radius, along, base, e_axis, angles, material, outward):
    """Close one end of a lathe with a triangle fan, UV'd radially inside the swatch."""
    if radius < 1e-4:
        return
    hub = _add(base, _scale(e_axis, along))
    for i in range(len(ring) - 1):
        mesh.triangle(hub, ring[i], ring[i + 1],
                      (8.0, 8.0),
                      (8.0 + 7.0 * math.cos(angles[i]), 8.0 + 7.0 * math.sin(angles[i])),
                      (8.0 + 7.0 * math.cos(angles[i + 1]), 8.0 + 7.0 * math.sin(angles[i + 1])),
                      outward, material)


def _side(mesh, edge, profile, base, e_axis, outward, material):
    """Fill the flat cut face left by a partial `angle_span`, from the profile back to the axis."""
    hub_start = _add(base, _scale(e_axis, profile[0][1]))
    hub_end = _add(base, _scale(e_axis, profile[-1][1]))
    span = max(1e-6, abs(profile[-1][1] - profile[0][1]))
    for i in range(len(edge) - 1):
        v_lo = 0.5 + 15.0 * abs(profile[i][1] - profile[0][1]) / span
        v_hi = 0.5 + 15.0 * abs(profile[i + 1][1] - profile[0][1]) / span
        mesh.triangle(hub_start, edge[i], edge[i + 1],
                      (1.0, 0.5), (14.0, v_lo), (14.0, v_hi), outward, material)
    mesh.triangle(hub_start, edge[-1], hub_end,
                  (1.0, 0.5), (14.0, 15.5), (1.0, 15.5), outward, material)


# --------------------------------------------------------------------------- primitives
def cylinder(mesh, centre, radius, start, end, material, segments=STEM_SEGMENTS, axis="y",
             close_start=False, close_end=False):
    lathe(mesh, [(radius, start), (radius, end)], material, segments=segments, centre=centre,
          axis=axis, close_start=close_start, close_end=close_end)


def sphere(mesh, centre, radius, material, segments=SEGMENTS, rings=8,
           t_from=-1.0, t_to=1.0, axis="y"):
    """A (possibly truncated) sphere. `t_from`/`t_to` are fractions of the radius along the axis."""
    e_a, e_b, e_axis = AXES[axis]
    along_centre = _dot(centre, e_axis)
    across = (_dot(centre, e_a), _dot(centre, e_b))
    profile = []
    for i in range(rings + 1):
        t = t_from + (t_to - t_from) * i / rings
        r = radius * math.cos(math.asin(max(-1.0, min(1.0, t))))
        profile.append((r, along_centre + radius * t))
    lathe(mesh, profile, material, segments=segments, centre=across, axis=axis)


def disc(mesh, centre, radius, material, axis="y", facing=1.0, segments=SEGMENTS,
         angle_span=(0.0, TAU)):
    """A flat filled circle -- a shade's enamel liner, a half-shell's aperture.

    Note this is NOT a zero-height lathe: a lathe picks its winding from a RADIAL outward, which
    is in the plane of a flat disc and so decides nothing. The fan here takes an explicit `facing`.
    """
    e_a, e_b, e_axis = AXES[axis]
    along = _dot(centre, e_axis)
    base = _add(_scale(e_a, _dot(centre, e_a)), _scale(e_b, _dot(centre, e_b)))
    a0, a1 = angle_span
    angles = [a0 + (a1 - a0) * i / segments for i in range(segments + 1)]
    ring = [_add(base, _scale(e_axis, along),
                 _scale(_add(_scale(e_a, math.cos(a)), _scale(e_b, math.sin(a))), radius))
            for a in angles]
    _cap(mesh, ring, radius, along, base, e_axis, angles, material,
         outward=_scale(e_axis, facing))


def annulus(mesh, centre, inner, outer, material, axis="y", facing=-1.0, segments=SEGMENTS,
            angle_span=(0.0, TAU)):
    """A flat ring -- the trim flange of a recessed downlight.

    Like `disc`, this cannot be a zero-height lathe: a lathe picks its winding from a radial
    outward, which lies in the plane of a flat ring and so decides nothing. `facing` is explicit.
    """
    e_a, e_b, e_axis = AXES[axis]
    along = _dot(centre, e_axis)
    base = _add(_scale(e_a, _dot(centre, e_a)), _scale(e_b, _dot(centre, e_b)))
    outward = _scale(e_axis, facing)
    start, end = angle_span
    for i in range(segments):
        a0 = start + (end - start) * i / segments
        a1 = start + (end - start) * (i + 1) / segments
        def at(radius, angle):
            return _add(base, _scale(e_axis, along),
                        _scale(_add(_scale(e_a, math.cos(angle)), _scale(e_b, math.sin(angle))),
                               radius))
        mesh.quad(at(inner, a0), at(outer, a0), at(outer, a1), at(inner, a1),
                  (6.0, 8.0), (14.0, 8.0), (14.0, 12.0), (6.0, 12.0), outward, material)


def box(mesh, lo, hi, material, uv=(1.0, 1.0, 15.0, 15.0)):
    """Axis-aligned box. Every face takes the same swatch rect; these are plain materials."""
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    u0, v0, u1, v1 = uv
    for points, outward in (
            ([(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)], (0, 0, -1)),
            ([(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)], (0, 0, 1)),
            ([(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)], (-1, 0, 0)),
            ([(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)], (1, 0, 0)),
            ([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], (0, 1, 0)),
            ([(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], (0, -1, 0))):
        mesh.quad(points[0], points[1], points[2], points[3],
                  (u0, v0), (u1, v0), (u1, v1), (u0, v1), outward, material)


def strut(mesh, start, end, thickness, material, caps=True):
    """A thin square bar between two points -- cage wires, lantern posts, gooseneck arms.

    Capped by default. An open-ended tube is fine only where both ends are buried in something
    else; a jointed arm like the gooseneck's bends, so consecutive segments meet at an angle and
    the wedge between them is a hole you can see straight through. Pass `caps=False` only when the
    ends are genuinely covered.
    """
    dx, dy, dz = end[0] - start[0], end[1] - start[1], end[2] - start[2]
    length = math.sqrt(dx * dx + dy * dy + dz * dz)
    if length < 1e-6:
        return
    direction = (dx / length, dy / length, dz / length)
    helper = (0.0, 1.0, 0.0) if abs(direction[1]) < 0.9 else (1.0, 0.0, 0.0)
    u = _cross(direction, helper)
    u = _normalise(u)
    v = _normalise(_cross(direction, u))
    half = thickness / 2.0

    def corner(point, su, sv):
        return _add(point, _scale(u, su * half), _scale(v, sv * half))

    signs = [(-1, -1), (1, -1), (1, 1), (-1, 1)]
    for i in range(4):
        s0, s1 = signs[i], signs[(i + 1) % 4]
        outward = _normalise(_add(_scale(u, (s0[0] + s1[0]) / 2.0),
                                  _scale(v, (s0[1] + s1[1]) / 2.0)))
        mesh.quad(corner(start, *s0), corner(start, *s1), corner(end, *s1), corner(end, *s0),
                  (3.0, 1.0), (7.0, 1.0), (7.0, 15.0), (3.0, 15.0), outward, material)

    if caps:
        for point, facing in ((start, -1.0), (end, 1.0)):
            outward = _scale(direction, facing)
            mesh.quad(corner(point, *signs[0]), corner(point, *signs[1]),
                      corner(point, *signs[2]), corner(point, *signs[3]),
                      (3.0, 3.0), (7.0, 3.0), (7.0, 7.0), (3.0, 7.0), outward, material)


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def _normalise(vector):
    length = math.sqrt(sum(c * c for c in vector)) or 1.0
    return _scale(vector, 1.0 / length)


def torus(mesh, centre, radius, thickness, material, axis="y", ring_segments=10, tube_segments=6):
    """A closed ring -- a chain link.

    A lathe, not a loop of struts. Struts round a ring meet at an angle, so consecutive boxes
    interpenetrate and their outer faces end up coplanar AND overlapping, which is z-fighting all
    the way round every link. A torus is genuinely a surface of revolution: the profile is a circle
    offset from the axis, and it closes on itself with no seams and no caps.
    """
    tube = thickness / 2.0
    along = _dot(centre, AXES[axis][2])
    across = (_dot(centre, AXES[axis][0]), _dot(centre, AXES[axis][1]))
    profile = []
    for i in range(tube_segments + 1):
        angle = TAU * i / tube_segments
        profile.append((radius + tube * math.cos(angle), along + tube * math.sin(angle)))
    lathe(mesh, profile, material, segments=ring_segments, centre=across, axis=axis)


def sweep(mesh, points, thickness, material):
    """A square tube following a polyline, mitred at each joint.

    Independent struts were wrong twice over at a bend: the boxes interpenetrate, so their outer
    faces are coplanar and overlapping (z-fighting), while the wedge on the outside of the bend is
    open (a hole you can see through). One continuous tube whose cross-section rotates with the
    path has neither problem -- consecutive segments share a ring rather than each ending in mid-air.
    """
    if len(points) < 2:
        return
    directions = [_normalise(tuple(b[i] - a[i] for i in range(3)))
                  for a, b in zip(points, points[1:])]
    # The plane of each ring: the segment's own direction at the ends, the bisector at a joint, so
    # the tube meets itself squarely instead of one segment overshooting into the next.
    normals = [directions[0]]
    for before, after in zip(directions, directions[1:]):
        normals.append(_normalise(_add(before, after)))
    normals.append(directions[-1])

    # Parallel transport one reference vector along the path, so the cross-section does not spin.
    helper = (0.0, 1.0, 0.0) if abs(directions[0][1]) < 0.9 else (1.0, 0.0, 0.0)
    reference = _normalise(_cross(directions[0], helper))
    rings, half = [], thickness / 2.0
    for point, plane in zip(points, normals):
        # Re-perpendicularise against the new plane rather than starting over, which is what keeps
        # consecutive rings aligned corner to corner.
        u = _add(reference, _scale(plane, -_dot(reference, plane)))
        u = _normalise(u) if math.sqrt(sum(c * c for c in u)) > 1e-6 else reference
        v = _normalise(_cross(plane, u))
        reference = u
        rings.append([_add(point, _scale(u, su * half), _scale(v, sv * half))
                      for su, sv in ((-1, -1), (1, -1), (1, 1), (-1, 1))])

    for lower, upper in zip(rings, rings[1:]):
        centre_lo = _centroid(lower)
        centre_hi = _centroid(upper)
        for i in range(4):
            j = (i + 1) % 4
            face = (lower[i], lower[j], upper[j], upper[i])
            mid = _centroid(face)
            outward = _normalise(tuple(mid[k] - (centre_lo[k] + centre_hi[k]) / 2.0
                                       for k in range(3)))
            mesh.quad(*face, (3.0, 1.0), (7.0, 1.0), (7.0, 15.0), (3.0, 15.0), outward, material)

    for ring, facing in ((rings[0], -1.0), (rings[-1], 1.0)):
        plane = normals[0] if facing < 0 else normals[-1]
        mesh.quad(ring[0], ring[1], ring[2], ring[3],
                  (3.0, 3.0), (7.0, 3.0), (7.0, 7.0), (3.0, 7.0),
                  _scale(plane, facing), material)


def _centroid(points):
    return tuple(sum(p[i] for p in points) / len(points) for i in range(3))


def bulb_socket(mesh, across, top, bulb_top, stem=0.42, collar=0.95, height=1.1, bite=0.4):
    """The lamp holder a bare bulb screws into.

    Without one the bulb hangs in mid-air inside the shade, which is what these shipped as. It
    runs from the shade's inner apex down to a collar over the bulb's neck, and the collar bites
    `bite` units into the sphere so no gap can open between the two at any angle.
    """
    bottom = bulb_top - bite
    shoulder = bottom + height
    lathe(mesh, [(stem, top), (stem, shoulder), (collar, shoulder - 0.15), (collar, bottom)],
          BODY, segments=STEM_SEGMENTS, close_start=True, close_end=True)


def canopy(mesh, y_top=16.0, radius=3.0, depth=1.0):
    """The ceiling plate every pendant hangs from.

    Closed at BOTH ends. Leaving the top open is tempting -- a ceiling covers it -- but a pendant
    hung on a chain has open air above it, and an open canopy is then a clean line of sight down
    the stem and straight through the shade to the bulb. The top cap lies on y = 16 facing up,
    which is out of the block and therefore culled against a ceiling and harmless against air.
    """
    lathe(mesh, [(radius * 0.94, y_top), (radius, y_top - depth)], BODY,
          close_start=True, close_end=True)


# --------------------------------------------------------------------------- textures
#: Every swatch is a 16x16 vertical gradient with a little brushing noise. The lathe maps v along
#: the profile from neck to mouth, so the TOP row of each image is the dim/neck end.
SWATCHES = {
    # metal finishes -- what `#body` is overridden with, one per variant
    "finish_black": ((30, 30, 32), (58, 58, 62), 5),
    "finish_bronze": ((48, 32, 20), (104, 74, 48), 7),
    "finish_nickel": ((134, 137, 141), (198, 201, 205), 6),
    # White moulded plastic, for the residential flush-mount housings rather than a metal fixture
    "finish_white": ((226, 226, 222), (250, 250, 247), 2),
    # constant secondary surfaces
    "shade_enamel": ((214, 214, 208), (248, 248, 242), 3),
    "shade_wax": ((222, 218, 202), (240, 237, 224), 2),
    # `#lens` -- swapped per STATE by the blockstate
    "lens_opal_on": ((246, 232, 196), (255, 252, 238), 2),
    "lens_opal_off": ((176, 178, 176), (214, 216, 212), 3),
    "lens_bulb_on": ((255, 186, 84), (255, 238, 190), 3),
    "lens_bulb_off": ((122, 120, 112), (170, 168, 158), 4),
}


def write_swatches():
    """Draw the shared material swatches. Deterministic -- reruns must not churn the atlas."""
    written = []
    for name, (top, bottom, jitter) in sorted(SWATCHES.items()):
        rng = random.Random(name)
        image = Image.new("RGBA", (16, 16))
        pixels = image.load()
        for y in range(16):
            t = y / 15.0
            base = tuple(int(round(top[i] + (bottom[i] - top[i]) * t)) for i in range(3))
            for x in range(16):
                # Vertical brushing: one noise value per column per row, fine enough to read as
                # grain rather than dirt at block scale.
                noise = rng.randint(-jitter, jitter)
                pixels[x, y] = tuple(max(0, min(255, c + noise)) for c in base) + (255,)
        image.save(os.path.join(TEX_DIR, name + ".png"))
        written.append(name)
    return written


def refs(body="finish_black", shade="shade_enamel", lens="lens_opal_on"):
    """Default texture refs baked into the MTL. Blockstates override all three."""
    prefix = "csm:blocks/lighting/shared_textures/"
    return {BODY: prefix + body, SHADE: prefix + shade, LENS: prefix + lens}


# --------------------------------------------------------------------------- pendants
def pendant_schoolhouse(mesh):
    """Opal schoolhouse bell on a stem -- the diner/hallway pendant."""
    canopy(mesh, radius=3.0, depth=1.0)
    cylinder(mesh, (8.0, 8.0), 0.5, 11.4, 15.0, BODY)
    lathe(mesh, [(1.5, 11.6), (1.9, 10.5)], BODY, segments=STEM_SEGMENTS, close_start=True)
    # The bell itself is the lens: an opal schoolhouse glows all over, so the whole shade swaps.
    lathe(mesh, [(1.7, 10.5), (3.0, 9.7), (4.1, 8.7), (4.8, 7.5),
                 (5.0, 6.2), (4.7, 5.1), (3.9, 4.3), (3.0, 3.8), (2.5, 3.6)],
          LENS, close_end=True)


def pendant_industrial_dome(mesh):
    """Enamel warehouse/barn dome -- opaque shade, glowing bulb underneath."""
    canopy(mesh, radius=2.5, depth=0.9)
    cylinder(mesh, (8.0, 8.0), 0.45, 11.8, 15.1, BODY)
    lathe(mesh, [(1.3, 11.9), (2.3, 11.2), (3.7, 10.2), (5.1, 8.8),
                 (5.9, 7.3), (6.2, 6.2), (6.3, 5.7), (6.0, 5.3)], BODY, close_start=True)
    # Enamel liner: the same sweep seen from inside, so looking up into the shade is not a hole.
    lathe(mesh, [(1.2, 11.8), (2.2, 11.1), (3.6, 10.1), (5.0, 8.7),
                 (5.8, 7.3), (6.1, 6.2), (6.2, 5.75)], SHADE, flip=True)
    disc(mesh, (8.0, 11.8, 8.0), 1.2, SHADE, facing=-1.0)
    bulb_socket(mesh, (8.0, 8.0), 11.75, 6.95)
    sphere(mesh, (8.0, 5.6, 8.0), 1.35, LENS, rings=6)


def pendant_cone(mesh):
    """Modern minimal metal cone."""
    canopy(mesh, radius=2.2, depth=0.8)
    cylinder(mesh, (8.0, 8.0), 0.4, 10.9, 15.2, BODY)
    lathe(mesh, [(0.95, 11.0), (2.6, 9.3), (4.1, 7.3), (5.2, 5.5), (5.5, 4.6)],
          BODY, close_start=True)
    lathe(mesh, [(0.85, 10.9), (2.5, 9.2), (4.0, 7.25), (5.1, 5.5), (5.4, 4.7)],
          SHADE, flip=True)
    bulb_socket(mesh, (8.0, 8.0), 10.85, 5.75)
    sphere(mesh, (8.0, 4.6, 8.0), 1.15, LENS, rings=6)


def pendant_caged_edison(mesh):
    """Bare bulb inside a wire cage."""
    canopy(mesh, radius=2.2, depth=0.8)
    cylinder(mesh, (8.0, 8.0), 0.35, 11.9, 15.2, BODY)
    lathe(mesh, [(1.15, 12.0), (1.35, 10.7)], BODY, segments=STEM_SEGMENTS, close_start=True)
    # Teardrop bulb: a neck cylinder into a sphere.
    cylinder(mesh, (8.0, 8.0), 0.85, 9.7, 10.7, LENS)
    sphere(mesh, (8.0, 8.0, 8.0), 2.2, LENS, rings=7, t_to=0.78)
    # Cage: two bands and six verticals.
    cylinder(mesh, (8.0, 8.0), 2.95, 10.2, 10.8, BODY, segments=SEGMENTS)
    cylinder(mesh, (8.0, 8.0), 2.45, 5.1, 5.7, BODY, segments=SEGMENTS)
    for i in range(6):
        angle = TAU * i / 6.0
        strut(mesh,
              (8.0 + 2.95 * math.cos(angle), 10.5, 8.0 + 2.95 * math.sin(angle)),
              (8.0 + 2.45 * math.cos(angle), 5.4, 8.0 + 2.45 * math.sin(angle)),
              0.45, BODY)
    lathe(mesh, [(0.9, 5.2), (0.7, 4.5)], BODY, segments=STEM_SEGMENTS, close_end=True)


def pendant_globe(mesh):
    """Opal glass sphere."""
    canopy(mesh, radius=2.4, depth=0.8)
    cylinder(mesh, (8.0, 8.0), 0.4, 12.4, 15.2, BODY)
    lathe(mesh, [(1.0, 12.5), (1.6, 11.7)], BODY, segments=STEM_SEGMENTS, close_start=True)
    # Truncated at the top so the opening meets the fitter instead of floating inside it.
    sphere(mesh, (8.0, 7.9, 8.0), 3.8, LENS, rings=9, t_to=0.90)


# --------------------------------------------------------------------------- sconces
def sconce_halfshell(mesh):
    """Modern ADA half-shell wall wash.

    On a real one the frosted shell IS the fixture -- there is no metal hood with a slot in it --
    so the shell carries the lens material and the metal is only a slim plate and a rim band. The
    cut faces sit flush on z = 16 against the wall, which is why the lathe stops at half a turn.
    """
    box(mesh, (2.2, 3.9, 15.3), (13.8, 5.4, 16.0), BODY)
    # The axis stands at z = WALL_STANDOFF, not at 16. `close_sides` fills the cut with flat faces
    # lying in the axis plane and pointing into the room -- which on z = 16 is exactly coplanar with
    # the neighbouring wall block's own face, pointing the same way. Two surfaces at one depth is
    # z-fighting, and it shimmered right across the back of the shell.
    #
    # The shell is WALLED, not a single skin. A lathe emits one side, and the side it emits faces
    # away from the axis -- so an open bowl modelled as one surface is invisible from above, where
    # you are looking at the back of every face and straight through to the wall. The liner is the
    # same sweep drawn inward, and the flat ring at the mouth joins the two.
    outer = [(1.3, 4.4), (3.3, 5.1), (4.7, 6.2), (5.4, 7.5), (5.6, 8.8)]
    lathe(mesh, outer, LENS, centre=(8.0, WALL_STANDOFF), angle_span=(math.pi, TAU),
          close_start=True, close_sides=True)
    # No `close_sides` on the liner: the outer shell's cut already fills that plane, and a second
    # face on it would be coplanar, same-facing and fighting.
    # No `close_start` on the liner either: `_cap` takes its facing from the profile slope, so both
    # bottoms would face the same way on the same plane and overlap. The interior floor is an
    # explicit disc facing up into the bowl instead.
    lathe(mesh, [(radius - 0.25, along) for radius, along in outer], LENS,
          centre=(8.0, WALL_STANDOFF), angle_span=(math.pi, TAU), flip=True)
    disc(mesh, (8.0, 4.55, WALL_STANDOFF), 1.05, LENS, facing=1.0,
         angle_span=(math.pi, TAU))
    # The rim sits flush with the shell rather than overhanging it. A proud rim looks better and
    # is wrong: the strip of flange past the shell's outer edge has nothing underneath, and since
    # it only faces up you can see through it from below.
    annulus(mesh, (8.0, 8.8, WALL_STANDOFF), 5.35, 5.62, BODY, facing=1.0,
            angle_span=(math.pi, TAU))
    lathe(mesh, [(5.62, 8.8), (5.62, 9.15)], BODY, centre=(8.0, WALL_STANDOFF),
          angle_span=(math.pi, TAU), close_sides=True)


def sconce_colonial(mesh):
    """Candle-style wall sconce."""
    box(mesh, (6.2, 4.6, 15.2), (9.8, 10.6, 16.0), BODY)
    # The boss stands proud of the plate. Its cap used to land on z = 15.2, the plate's own front
    # face, both facing the room -- the shimmer that ran down the middle of the sconce.
    lathe(mesh, [(2.1, 15.2 - COPLANAR_NUDGE), (2.1, 16.0)], BODY, centre=(8.0, 10.6), axis="z",
          close_start=True)
    # Started inside the plate rather than on its face, so the strut's own end cap is buried.
    # Clear of the cup's bottom cap at y = 5.6; sharing that plane put two down-facing
    # surfaces at one depth.
    strut(mesh, (8.0, 6.3, 15.7), (8.0, 6.3, 13.0), 0.8, BODY)
    lathe(mesh, [(1.5, 5.6), (1.05, 6.6)], BODY, centre=(8.0, 13.0), close_start=True)
    cylinder(mesh, (8.0, 13.0), 0.78, 6.6, 10.0, SHADE)
    lathe(mesh, [(0.62, 10.0), (0.55, 10.6), (0.32, 11.3), (0.0, 11.9)], LENS,
          segments=STEM_SEGMENTS, centre=(8.0, 13.0), close_start=True)


def sconce_vanity_bar(mesh):
    """Three-lamp mirror bar."""
    box(mesh, (1.4, 6.5, 15.2), (14.6, 9.5, 16.0), BODY)
    for x in (4.0, 8.0, 12.0):
        cylinder(mesh, (x, 8.0), 1.05, 14.5, 15.2 - COPLANAR_NUDGE, BODY, axis="z",
                 close_end=True)
        lathe(mesh, [(0.95, 14.5), (1.65, 13.7), (1.95, 12.8), (1.85, 12.0),
                     (1.25, 11.4), (0.45, 11.1)],
              LENS, centre=(x, 8.0), axis="z", close_end=True)


def sconce_carriage_lantern(mesh):
    """Exterior porch lantern -- square glass body under a tapered cap."""
    box(mesh, (6.4, 7.6, 15.2), (9.6, 13.2, 16.0), BODY)
    strut(mesh, (8.0, 12.0, 15.7), (8.0, 12.0, 12.2), 0.9, BODY)
    # Cap and finial.
    lathe(mesh, [(3.5, 11.5), (2.3, 12.5), (0.7, 13.3)], BODY, centre=(8.0, 11.5),
          close_start=True, close_end=True)
    sphere(mesh, (8.0, 13.6, 11.5), 0.55, BODY, segments=STEM_SEGMENTS, rings=5)
    # Body: four corner posts, four glass panels, a floor and a drop finial.
    x0, x1 = 5.2, 10.8
    z0, z1 = 8.7, 14.3
    y0, y1 = 5.7, 11.5
    for cx in (x0, x1):
        for cz in (z0, z1):
            strut(mesh, (cx, y0, cz), (cx, y1, cz), 0.62, BODY)
    # The four panes abut rather than overlap. Spanning the full width in both axes made each
    # corner two panes occupying the same space, with coincident faces that fought.
    box(mesh, (x0 + 0.22, y0 + 0.2, z0), (x1 - 0.22, y1 - 0.2, z0 + 0.22), LENS)
    box(mesh, (x0 + 0.22, y0 + 0.2, z1 - 0.22), (x1 - 0.22, y1 - 0.2, z1), LENS)
    box(mesh, (x0, y0 + 0.2, z0), (x0 + 0.22, y1 - 0.2, z1), LENS)
    box(mesh, (x1 - 0.22, y0 + 0.2, z0), (x1, y1 - 0.2, z1), LENS)
    box(mesh, (x0 - 0.3, y0 - 0.7, z0 - 0.3), (x1 + 0.3, y0, z1 + 0.3), BODY)
    lathe(mesh, [(0.85, 5.0), (0.55, 4.3)], BODY, segments=STEM_SEGMENTS,
          centre=(8.0, 11.5), close_end=True)


def sconce_rlm_gooseneck(mesh):
    """Exterior storefront barn light on a gooseneck arm."""
    box(mesh, (6.2, 9.2, 15.2), (9.8, 12.8, 16.0), BODY)
    arm = [(8.0, 11.0, 15.7), (8.0, 11.7, 13.4), (8.0, 11.7, 10.9),
           (8.0, 11.0, 9.2), (8.0, 10.2, 8.4)]
    sweep(mesh, arm, 0.9, BODY)
    lathe(mesh, [(1.1, 10.2), (2.5, 9.5), (4.1, 8.4), (5.5, 7.1),
                 (6.2, 6.0), (6.4, 5.4), (6.1, 5.0)], BODY, centre=(8.0, 8.2),
          close_start=True)
    lathe(mesh, [(1.0, 10.1), (2.4, 9.4), (4.0, 8.35), (5.4, 7.1),
                 (6.1, 6.0), (6.3, 5.45)], SHADE, centre=(8.0, 8.2), flip=True)
    disc(mesh, (8.0, 10.1, 8.2), 1.0, SHADE, facing=-1.0)
    bulb_socket(mesh, (8.0, 8.2), 10.05, 6.7)
    sphere(mesh, (8.0, 5.4, 8.2), 1.3, LENS, rings=6)


# --------------------------------------------------------------------------- repairs
# The two Commercial Electric fixtures are RECESSED downlights, not flush-mount domes. The real
# thing is a thin trim plate sitting almost flush with the ceiling, with the light source set up
# inside the ceiling cavity. A block model cannot occupy the ceiling block, so the recess is faked
# the only way it can be: the trim is one texture pixel deep, and the lens sits HIGHER than the
# trim's bottom face, so you look up into a shallow well.
#
# They originally shipped spanning y 15.95..16.24 -- 83% of their depth in the block ABOVE, and
# 0.05 units (a twentieth of a pixel) inside their own block, which is why they were invisible.
# Their bounding boxes agreed and were equally wrong (y 1.0..1.1, entirely in the neighbour). The
# fix is depth and placement, not bulk: one pixel of trim, everything inside its own block.
TRIM_BOTTOM = 15.0  # 1.0 unit of visible depth -- thin, like the real trim


def ceiling_halo(mesh):
    """Commercial Electric LED Halo Light -- round recessed downlight with a thin trim ring."""
    # Outer trim wall, top left open because the ceiling above covers it.
    lathe(mesh, [(5.6, 16.0), (5.6, 15.15), (5.3, TRIM_BOTTOM)], BODY)
    annulus(mesh, (8.0, TRIM_BOTTOM, 8.0), 3.3, 5.3, BODY, facing=-1.0)
    # Baffle wall climbing back up into the recess; seen from inside, so the normals invert.
    lathe(mesh, [(3.3, TRIM_BOTTOM), (3.15, 15.5), (3.1, 16.0)], BODY, flip=True)
    # The lens is recessed ABOVE the trim's bottom face -- that gap is what reads as "recessed".
    disc(mesh, (8.0, 15.55, 8.0), 3.12, LENS, facing=-1.0)


def ceiling_square(mesh):
    """Commercial Electric LED Square Light -- square recessed panel in a thin frame."""
    outer0, outer1 = 3.4, 12.6
    hole0, hole1 = 5.2, 10.8
    box(mesh, (outer0, TRIM_BOTTOM, outer0), (outer1, 16.0, hole0), BODY)
    box(mesh, (outer0, TRIM_BOTTOM, hole1), (outer1, 16.0, outer1), BODY)
    box(mesh, (outer0, TRIM_BOTTOM, hole0), (hole0, 16.0, hole1), BODY)
    box(mesh, (hole1, TRIM_BOTTOM, hole0), (outer1, 16.0, hole1), BODY)
    box(mesh, (hole0, 15.45, hole0), (hole1, 15.75, hole1), LENS)


# --------------------------------------------------------------------------- chain
def pendant_chain(mesh):
    """Stackable chain/stem extension. Links alternate plane so it reads as chain from any angle."""
    for index, y in enumerate((2.0, 6.0, 10.0, 14.0)):
        # Alternating planes, so a stack reads as chain rather than a column of hoops.
        axis = "z" if index % 2 == 0 else "x"
        torus(mesh, (8.0, y, 8.0), 2.3, 0.55, BODY, axis=axis)


# --------------------------------------------------------------------------- driver
FIXTURES = [
    ("pendant_schoolhouse", pendant_schoolhouse, "Opal schoolhouse bell pendant",
     refs(lens="lens_opal_on")),
    ("pendant_industrial_dome", pendant_industrial_dome, "Enamel warehouse dome pendant",
     refs(lens="lens_bulb_on")),
    ("pendant_cone", pendant_cone, "Modern metal cone pendant", refs(lens="lens_bulb_on")),
    ("pendant_caged_edison", pendant_caged_edison, "Caged Edison bulb pendant",
     refs(lens="lens_bulb_on")),
    ("pendant_globe", pendant_globe, "Opal glass globe pendant", refs(lens="lens_opal_on")),
    ("sconce_halfshell", sconce_halfshell, "Half-shell wall wash sconce",
     refs(lens="lens_opal_on")),
    ("sconce_colonial", sconce_colonial, "Colonial candle wall sconce",
     refs(shade="shade_wax", lens="lens_bulb_on")),
    ("sconce_vanity_bar", sconce_vanity_bar, "Three-lamp vanity bar sconce",
     refs(lens="lens_opal_on")),
    ("sconce_carriage_lantern", sconce_carriage_lantern, "Exterior carriage lantern sconce",
     refs(lens="lens_opal_on")),
    ("sconce_rlm_gooseneck", sconce_rlm_gooseneck, "RLM gooseneck storefront sconce",
     refs(lens="lens_bulb_on")),
    ("pendant_chain", pendant_chain, "Stackable pendant chain", refs()),
    ("ceiling_halo", ceiling_halo, "Commercial Electric LED halo flush mount",
     refs(body="finish_white", lens="lens_opal_on")),
    ("ceiling_square", ceiling_square, "Commercial Electric LED square flush mount",
     refs(body="finish_white", lens="lens_opal_on")),
]


# --------------------------------------------------------------------------- registration
#: One row per fixture: model, registry-id stem, display name, which lens family it uses, and
#: which constant secondary surface. The finish suffix and the three blockstates per row are
#: derived, so the id, the lang key, the blockstate and the tab line cannot drift apart.
TEX = "csm:blocks/lighting/shared_textures/"

FINISHES = [
    ("black", "finish_black", "Black"),
    ("bronze", "finish_bronze", "Bronze"),
    ("nickel", "finish_nickel", "Brushed Nickel"),
]

#: (model, id stem, display name, lens family, shade swatch)
CATALOGUE = [
    ("pendant_schoolhouse", "pendschoolhouse", "Schoolhouse Pendant", "opal", "shade_enamel"),
    ("pendant_industrial_dome", "penddome", "Industrial Dome Pendant", "bulb", "shade_enamel"),
    ("pendant_cone", "pendcone", "Cone Pendant", "bulb", "shade_enamel"),
    ("pendant_caged_edison", "pendcaged", "Caged Edison Pendant", "bulb", "shade_enamel"),
    ("pendant_globe", "pendglobe", "Globe Pendant", "opal", "shade_enamel"),
    ("sconce_halfshell", "sconcehalfshell", "Half-Shell Wall Sconce", "opal", "shade_enamel"),
    ("sconce_colonial", "sconcecolonial", "Colonial Candle Sconce", "bulb", "shade_wax"),
    ("sconce_vanity_bar", "sconcevanity", "Vanity Bar Sconce", "opal", "shade_enamel"),
    ("sconce_carriage_lantern", "sconcelantern", "Carriage Lantern Sconce", "opal",
     "shade_enamel"),
    ("sconce_rlm_gooseneck", "sconcerlm", "Gooseneck Barn Sconce", "bulb", "shade_enamel"),
]

#: The chain is not a light -- no STATE property, no lens, no redstone -- so it is generated from
#: its own row rather than being bent into the fixture template.
CHAIN = ("pendant_chain", "pendchain", "Pendant Chain")

#: STATE 1 and 3 are the two lit states (redstone-on and manual-on); 0 and 2 are the off pair.
LIT_STATES = ("1", "3")

BLOCKSTATE_DIR = os.path.join(ASSETS, "blockstates")

#: These models are authored in 0..1 block space with the block's min corner at the origin, the
#: same as the fire alarm OBJs, so Forge's stock preset frames them correctly and nothing needs
#: hand-tuning. Do NOT hand-write a transform dict here unless you check the units first: Forge
#: blockstate translations are in BLOCKS, not the 1/16 of a vanilla item model's `display`, so a
#: plausible-looking `translation: [0, -1.5, 0]` throws the icon a block and a half off-screen.
INVENTORY_TRANSFORM = "forge:default-block"


#: Existing blocks repaired in place. Same registry names, no finish variants, so these are NOT
#: new tab entries -- only their blockstate and their bounding box change. The script prints the
#: corrected AxisAlignedBB lines for CsmTabLighting.
REPAIRS = [
    ("ceiling_halo", "cehalo", "opal", "finish_white"),
    ("ceiling_square", "cesquare", "opal", "finish_white"),
]


def blockstate(model, finish_swatch, shade_swatch, lens_family, include_shade=True):
    """One fixture's Forge blockstate.

    Every sub-variant carries a real body -- an explicit `y` on the rotations, a texture override
    on the states -- rather than a bare `{}`, because Forge's ForgeBlockStateV1 deserializer types
    a variant by peeking its first sub-entry.
    """
    lens_on = TEX + "lens_%s_on" % lens_family
    lens_off = TEX + "lens_%s_off" % lens_family
    textures = {"#body": TEX + finish_swatch, "#lens": lens_off}
    if include_shade:
        textures["#shade"] = TEX + shade_swatch
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "csm:lighting/shared_models/%s.obj" % model,
            "textures": textures,
            "custom": {"flip-v": True},
        },
        "variants": {
            "facing": {
                "north": {"y": 0},
                "east": {"y": 90},
                "south": {"y": 180},
                "west": {"y": 270},
            },
            "state": {
                str(s): {"textures": {"#lens": lens_on if str(s) in LIT_STATES else lens_off}}
                for s in range(4)
            },
            "inventory": [{"transform": INVENTORY_TRANSFORM, "textures": {"#lens": lens_on}}],
            "normal": [{}],
        },
    }


def chain_blockstate(model, finish_swatch):
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "csm:lighting/shared_models/%s.obj" % model,
            "textures": {"#body": TEX + finish_swatch},
            "custom": {"flip-v": True},
        },
        "variants": {
            "inventory": [{"transform": "forge:default-block"}],
            "normal": [{}],
        },
    }


def write_registration(bounds_by_model):
    """Emit the 33 blockstates, plus lang and tab-registration fragments for merging."""
    os.makedirs(BLOCKSTATE_DIR, exist_ok=True)
    lang, tab, written = [], [], 0

    tab.append("    // Decorative pendants")
    for model, stem, label, lens_family, shade_swatch in CATALOGUE:
        if model == "sconce_halfshell":
            tab.append("    // Decorative wall sconces")
        x0, y0, z0, x1, y1, z1 = bounds_by_model[model]
        for suffix, swatch, finish_label in FINISHES:
            block_id = stem + suffix
            path = os.path.join(BLOCKSTATE_DIR, block_id + ".json")
            with open(path, "w", newline="\n") as handle:
                json.dump(blockstate(model, swatch, shade_swatch, lens_family), handle, indent=2)
                handle.write("\n")
            written += 1
            lang.append("tile.%s.name=%s (%s)" % (block_id, label, finish_label))
            tab.append('    initTabBlock(new BlockBrightLightFactory("%s", '
                       "new AxisAlignedBB(%f, %f, %f, %f, %f, %f), 0));"
                       % (block_id, x0 / 16.0, y0 / 16.0, z0 / 16.0,
                          x1 / 16.0, y1 / 16.0, z1 / 16.0))

    model, stem, label = CHAIN
    tab.append("    // Chain that extends any pendant's drop")
    for suffix, swatch, finish_label in FINISHES:
        block_id = stem + suffix
        path = os.path.join(BLOCKSTATE_DIR, block_id + ".json")
        with open(path, "w", newline="\n") as handle:
            json.dump(chain_blockstate(model, swatch), handle, indent=2)
            handle.write("\n")
        written += 1
        lang.append("tile.%s.name=%s (%s)" % (block_id, label, finish_label))
        tab.append('    initTabBlock(new BlockDecorativeChainFactory("%s"));' % block_id)

    for model, block_id, lens_family, body_swatch in REPAIRS:
        path = os.path.join(BLOCKSTATE_DIR, block_id + ".json")
        with open(path, "w", newline="\n") as handle:
            json.dump(blockstate(model, body_swatch, None, lens_family, include_shade=False),
                      handle, indent=2)
            handle.write("\n")
        written += 1
        x0, y0, z0, x1, y1, z1 = bounds_by_model[model]
        tab.append('    // repaired: was AxisAlignedBB(..., 1.0, ..., 1.1, ...) -- above the block')
        tab.append('    initTabBlock(new BlockBrightLightFactory("%s", '
                   "new AxisAlignedBB(%f, %f, %f, %f, %f, %f), 0));"
                   % (block_id, x0 / 16.0, y0 / 16.0, z0 / 16.0,
                      x1 / 16.0, y1 / 16.0, z1 / 16.0))

    scratch = os.path.join(REPO_ROOT, "dev-env-utils", "scripts")
    with open(os.path.join(scratch, "_decorative_lang.txt"), "w", newline="\n") as handle:
        handle.write("\n".join(lang) + "\n")
    with open(os.path.join(scratch, "_decorative_tab.txt"), "w", newline="\n") as handle:
        handle.write("\n".join(tab) + "\n")
    return written, len(lang)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    os.makedirs(TEX_DIR, exist_ok=True)
    swatches = write_swatches()
    print("swatches: %d" % len(swatches))
    print()
    print("%-26s %6s  %s" % ("model", "tris", "bounds (0..16)"))
    bounds_by_model = {}
    for name, builder, header, textures in FIXTURES:
        mesh = Mesh()
        builder(mesh)
        count = mesh.write(os.path.join(OUT_DIR, name + ".obj"), name, textures, header)
        bounds_by_model[name] = mesh.bounds()
        print("%-26s %6d  %s" % (name, count,
                                 " ".join("%.2f" % v for v in bounds_by_model[name])))
    print()
    blockstates, lang_lines = write_registration(bounds_by_model)
    print("blockstates written: %d" % blockstates)
    print("lang lines staged:   %d  (dev-env-utils/scripts/_decorative_lang.txt)" % lang_lines)
    print("tab lines staged:        (dev-env-utils/scripts/_decorative_tab.txt)")


if __name__ == "__main__":
    main()
