#!/usr/bin/env python3
"""
Generates the OBJ street light fixtures (GitHub #99): luminaires built as real curved bodies
rather than stacks of JSON boxes, one catalogue entry per fixture.

WHY
---
The cobra heads in the Lighting tab are MCreator-era JSON: a GE M400R3 is eleven boxes, its
refractor a staircase of five slabs. A cobra head is a lofted shape -- a cast hood that rises
from the slipfitter, swells over the optical chamber and rounds off at the nose -- and JSON
elements cannot draw that. OBJ can, and costs nothing extra at run time: it is baked into the
chunk mesh exactly as a JSON model is, with no tile entity and no renderer.

WHAT IT WRITES (per catalogue entry)
-----------------------------------
  models/block/lighting/shared_models/streetlight_<model>.obj    the fixture, 0..1 block space
  models/block/lighting/shared_models/streetlight.mtl             shared: body, lens, photocell
  blockstates/<registry>.json                                     facing x STATE, the lens per state
  textures/blocks/lighting/shared_textures/streetlight_*.png      16 px swatches, shared by all

Every fixture wears the same three materials, so a new fixture adds geometry and no sprites:
  body       the housing paint, overridden per fixture by the blockstate (`#body`)
  unshaded_lens  the glass, `streetlight_lens_hps_on` in STATE 1 and 3, `_off` in 0 and 2;
                 drawn without directional shading (CsmObjModelLoader.UNSHADED_PREFIX)
  photocell  the twist-lock photocontrol on top

CONVENTIONS (the same as every other cobra head in the tab, so a fixture is a drop-in)
--------------------------------------------------------------------------------------
Authored in 1/16 units, written /16. The fixture faces north: the nose at low z, the slipfitter
at z = 16, where the arm from the block behind arrives (the NOV tapered masts and the pole light
mount end at y 0..3 there). It stays inside the old model's envelope -- x 5..11, z 1..16 -- so
its bounding box is the one the JSON fixture already registers, and swapping a JSON fixture's
blockstate onto one of these models changes nothing a placed block depends on.

Nothing sits on z = 16 facing back into the block: the rear cap is at REAR_Z (15.9), the same
standoff the decorative family uses (LIGHTING_SYSTEM.md).

Run:  python dev-env-utils/scripts/gen_streetlight_fixtures.py [--check]
"""

import io
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

from PIL import Image  # noqa: E402

MODEL_DIR = "models/block/lighting/shared_models"
TEX_DIR = "textures/blocks/lighting/shared_textures"
OWNER = "lighting"
MTL_NAME = "streetlight.mtl"

TEX = "csm:blocks/lighting/shared_textures/"
SWATCHES = {
    # name: (top RGB, bottom RGB) -- a vertical gradient, the decorative family's convention
    "streetlight_body_ge_grey": ((176, 178, 174), (136, 138, 134)),
    "streetlight_photocell": ((70, 72, 76), (38, 40, 44)),
}
LENS_ON = "streetlight_lens_hps_on"
LENS_OFF = "streetlight_lens_hps_off"
# A drop refractor with its lamp off is pale prismatic glass, not the dark smoked look of flat
# glass, so refractor fixtures name this for STATE 0 and 2 instead.
LENS_REFRACTOR_OFF = "streetlight_lens_refractor_off"
# Lit, a refractor wears the requester's refractor orange (ge_hps_fixtures, 255 106 19) in the
# ribs his JSON staircase showed, one band where each of its five steps caught the light.
LENS_REFRACTOR_ON = "streetlight_lens_refractor_on"

# The glass. CsmObjModelLoader draws a material named unshaded_* without directional shading, as
# a JSON element with "shade": false is: the flat lens faces straight down, and shaded it was
# drawn at half brightness, so a lit lens could never read brighter than a dull tan. The unlit
# lens is painted at about half to match the shaded housing round it (the troffers' rule).
LENS_MAT = "unshaded_lens"

ARC = 14          # points on a housing section's top arc
REAR_Z = 15.9     # the rear cap's standoff from the block face


# --------------------------------------------------------------------------- mesh
class Mesh:
    """Triangles with per-face normals, written as `f v/vt/vn`, wound outward."""

    def __init__(self):
        self.v, self.vt, self.vn, self.faces = [], [], [], []

    def _idx(self, store, item):
        store.append(item)
        return len(store)

    def tri(self, mat, ps, uvs):
        n = normal(*ps)
        ni = self._idx(self.vn, n)
        self.faces.append((mat, [(self._idx(self.v, p), self._idx(self.vt, uv), ni)
                                 for p, uv in zip(ps, uvs)]))

    def poly_out(self, mat, ps, uvs, inside):
        """A convex polygon (fan) wound so its normal points away from `inside`."""
        cen = [sum(p[k] for p in ps) / len(ps) for k in range(3)]
        n = normal(ps[0], ps[1], ps[2])
        if abs(sum(n[k] * (cen[k] - inside[k]) for k in range(3))) < 1e-9 and len(ps) > 3:
            n = normal(ps[0], ps[2], ps[3])
        if sum(n[k] * (cen[k] - inside[k]) for k in range(3)) < 0:
            ps, uvs = ps[::-1], uvs[::-1]
        for i in range(1, len(ps) - 1):
            if area(ps[0], ps[i], ps[i + 1]) > 1e-9:
                self.tri(mat, [ps[0], ps[i], ps[i + 1]], [uvs[0], uvs[i], uvs[i + 1]])

    def text(self, header):
        out = io.StringIO()
        out.write("# Generated by dev-env-utils/scripts/gen_streetlight_fixtures.py -- do not edit\n")
        out.write("# %s\nmtllib %s\no fixture\n" % (header, MTL_NAME))
        for x, y, z in self.v:
            out.write("v %.5f %.5f %.5f\n" % (x / 16.0, y / 16.0, z / 16.0))
        for u, w in self.vt:
            out.write("vt %.5f %.5f\n" % (u, w))
        for x, y, z in self.vn:
            out.write("vn %.5f %.5f %.5f\n" % (x, y, z))
        cur = None
        for mat, idx in self.faces:
            if mat != cur:
                out.write("usemtl %s\n" % mat)
                cur = mat
            out.write("f %s\n" % " ".join("%d/%d/%d" % t for t in idx))
        return out.getvalue()


def normal(a, b, c):
    ux, uy, uz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    vx, vy, vz = c[0] - a[0], c[1] - a[1], c[2] - a[2]
    n = (uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx)
    m = math.sqrt(sum(k * k for k in n)) or 1.0
    return tuple(k / m for k in n)


def area(a, b, c):
    ux, uy, uz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    vx, vy, vz = c[0] - a[0], c[1] - a[1], c[2] - a[2]
    n = (uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx)
    return 0.5 * math.sqrt(sum(k * k for k in n))


def swatch_uv(u, v):
    """A swatch UV kept off the sprite's border, so mip levels never sample a neighbour."""
    return (0.1 + 0.8 * min(max(u, 0.0), 1.0), 0.1 + 0.8 * min(max(v, 0.0), 1.0))


# --------------------------------------------------------------------------- shapes
def section(z, hw, yb, yt, ex):
    """A housing cross-section: a superellipse arc over a flat bottom, from (+hw, yb) over the
    top to (-hw, yb). The bottom edge closes it. `ex` > 2 squares the shoulders off."""
    pts = []
    for j in range(ARC + 1):
        a = math.pi * j / ARC
        c, s = math.cos(a), math.sin(a)
        x = math.copysign(abs(c) ** (2.0 / ex), c) * hw
        y = yb + (yt - yb) * abs(s) ** (2.0 / ex)
        pts.append((8.0 + x, y, z))
    return pts


def loft_housing(mesh, profile, ex):
    """Bridge consecutive sections; cap both ends. Returns the rings."""
    rings = [section(z, hw, yb, yt, ex) for (z, hw, yb, yt) in profile]
    n = len(rings[0])
    zs = [p[0] for p in profile]
    zmin, zmax = min(zs), max(zs)
    for i in range(len(rings) - 1):
        a, b = rings[i], rings[i + 1]
        mid_y = (profile[i][2] + profile[i][3] + profile[i + 1][2] + profile[i + 1][3]) / 4
        inside = (8.0, mid_y, (profile[i][0] + profile[i + 1][0]) / 2)
        va = (profile[i][0] - zmin) / (zmax - zmin)
        vb = (profile[i + 1][0] - zmin) / (zmax - zmin)
        for j in range(n):
            j2 = (j + 1) % n  # j = n - 1 closes along the flat bottom
            ua, ub = j / n, (j + 1) / n
            mesh.poly_out("body", [a[j], a[j2], b[j2], b[j]],
                          [swatch_uv(ua, va), swatch_uv(ub, va), swatch_uv(ub, vb),
                           swatch_uv(ua, vb)], inside)
    for ring, nxt in ((rings[0], rings[1]), (rings[-1], rings[-2])):
        cy = sum(p[1] for p in ring) / n
        cz = ring[0][2]
        cen = (8.0, cy, cz)
        behind = (8.0, cy, nxt[0][2])
        for j in range(n):
            j2 = (j + 1) % n
            mesh.poly_out("body", [cen, ring[j], ring[j2]],
                          [swatch_uv(0.5, 0.5), swatch_uv(0.0, 0.0), swatch_uv(1.0, 0.0)], behind)
    return rings


def box(mesh, lo, hi, mat, bottom_mat=None):
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    c = ((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2)
    uv = [swatch_uv(0, 0), swatch_uv(1, 0), swatch_uv(1, 1), swatch_uv(0, 1)]
    faces = {
        "down": [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
        "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        "north": [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)],
        "south": [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)],
        "west": [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)],
        "east": [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)],
    }
    for name, ps in faces.items():
        if name == "down" and bottom_mat:
            # the lens: u across the fixture, v along it, so the lamp's glow spans the glass
            lens_uv = [(0.05 + 0.9 * (x1 - p[0]) / (x1 - x0), 0.05 + 0.9 * (p[2] - z0) / (z1 - z0))
                       for p in ps]
            mesh.poly_out(bottom_mat, ps, lens_uv, c)
        else:
            mesh.poly_out(mat, ps, uv, c)


def cylinder_y(mesh, cx, cz, r, y0, y1, mat, sides=12, cap_top=True):
    ring0 = [(cx + r * math.cos(2 * math.pi * j / sides), y0, cz + r * math.sin(2 * math.pi * j / sides))
             for j in range(sides)]
    ring1 = [(p[0], y1, p[2]) for p in ring0]
    inside = (cx, (y0 + y1) / 2, cz)
    for j in range(sides):
        j2 = (j + 1) % sides
        mesh.poly_out(mat, [ring0[j], ring0[j2], ring1[j2], ring1[j]],
                      [swatch_uv(j / sides, 0), swatch_uv((j + 1) / sides, 0),
                       swatch_uv((j + 1) / sides, 1), swatch_uv(j / sides, 1)], inside)
    if cap_top:
        mesh.poly_out(mat, ring1, [swatch_uv(0.5 + 0.5 * math.cos(2 * math.pi * j / sides),
                                             0.5 + 0.5 * math.sin(2 * math.pi * j / sides))
                                   for j in range(sides)], (cx, y0, cz))


def half_ellipsoid_down(mesh, centre, rx, ry, rz, mat, lat=6, lon=16):
    """A drop refractor: the lower half of an ellipsoid hung under the housing."""
    cx, cy, cz = centre
    rings = []
    for i in range(lat + 1):
        phi = (math.pi / 2) * i / lat           # 0 at the rim, pi/2 at the bottom
        rr, yy = math.cos(phi), math.sin(phi)
        rings.append([(cx + rx * rr * math.cos(2 * math.pi * j / lon), cy - ry * yy,
                       cz + rz * rr * math.sin(2 * math.pi * j / lon)) for j in range(lon)])
    for i in range(lat):
        for j in range(lon):
            j2 = (j + 1) % lon
            ps = [rings[i][j], rings[i][j2], rings[i + 1][j2], rings[i + 1][j]]
            uvs = [(0.05 + 0.9 * (cx + rx - p[0]) / (2 * rx), 0.05 + 0.9 * (p[2] - cz + rz) / (2 * rz))
                   for p in ps]
            mesh.poly_out(mat, ps, uvs, (cx, cy + 0.5, cz))


# --------------------------------------------------------------------------- the catalogue
# GE M-400A (1959-): a cast aluminium hood over a hinged "Powr/Door" holding the ballast and the
# optics, with a twist-lock photocell receptacle on top near the slipfitter. Side profile: the
# hood rises from a narrow neck at the slipfitter, is highest over the optical chamber in the
# front half and rounds down to the nose. Proportions from GE's dimensioned cutoff sheet
# (MDCA/MDCL): 31.81 in long, 15.12 in wide at the head tapering to 9.04 in at the pole end,
# 8.32 in tall with the flat glass. Scaled into the old model's 15 px length that is 7.1 px wide
# (held to 7, the envelope), 4.2 px at the neck and 3.9 px tall.
#
# The neck is wider and deeper than GE's 9 in pole end would scale to, because it has to swallow
# the arm: every arm a cobra head hangs from in CSM ends in a 3 x 3 px square (ARM_TIP), about a
# 6 in pipe at this scale, where the real slipfitter takes a 2 in one. A neck drawn to scale
# left the arm's corners standing proud of the fixture's end.
M400A_PROFILE = [
    # (z, half-width, bottom y, top y)   nose (low z) -> slipfitter (z 16)
    (1.0, 1.2, 0.65, 1.45),
    (1.35, 2.1, 0.32, 2.05),
    (2.1, 2.85, 0.10, 2.70),
    (3.3, 3.30, 0.0, 3.30),
    (5.0, 3.48, 0.0, 3.70),
    (7.0, 3.50, 0.0, 3.90),
    (9.0, 3.40, 0.0, 3.90),
    (11.0, 3.10, -0.05, 3.75),
    (12.8, 2.75, -0.20, 3.60),
    (14.4, 2.50, -0.38, 3.52),
    (REAR_Z, 2.45, -0.45, 3.50),
]
M400A_EXPONENT = 2.6

# Where the arm arrives at z = 16, in the fixture's block: x 6.5..9.5 on every arm, y 0..3 on the
# pole light mount, the SCE mounts and NOV tapered masts 1-3, y -0.25..2.75 on masts 4 and 5.
ARM_TIP = ((6.5, -0.25), (9.5, 3.0))


def check_neck_holds_arm(registry, profile, ex):
    """The rear section must contain the arm's end, corners and all, or the arm shows past it."""
    _, hw, yb, yt = profile[-1]
    (x0, y0), (x1, y1) = ARM_TIP
    for x in (x0, x1):
        for y in (y0, y1):
            inside = y >= yb and (abs(x - 8.0) / hw) ** ex + ((y - yb) / (yt - yb)) ** ex <= 1.0
            assert inside, "%s: the arm's corner (%s, %s) stands outside the neck" % (registry, x, y)


def top_at(profile, z):
    for (za, _, _, ya), (zb, _, _, yb) in zip(profile, profile[1:]):
        if za <= z <= zb:
            return ya + (yb - ya) * (z - za) / (zb - za)
    raise ValueError(z)


def build_m400a(optics, photocell=True):
    m = Mesh()
    check_neck_holds_arm("M-400A", M400A_PROFILE, M400A_EXPONENT)
    loft_housing(m, M400A_PROFILE, M400A_EXPONENT)
    if optics == "cutoff":
        # Flat glass in the door frame: a shallow slab whose underside is the lens.
        box(m, (5.3, -0.35, 3.4), (10.7, 0.02, 10.6), "body", bottom_mat=LENS_MAT)
    else:
        # Drop refractor: prismatic glass bowl under the optical chamber.
        half_ellipsoid_down(m, (8.0, 0.05, 7.0), 2.8, 2.4, 3.6, LENS_MAT)
    # Door latch at the nose.
    box(m, (7.4, -0.25, 1.9), (8.6, 0.12, 2.7), "body")
    if not photocell:
        return m
    # Twist-lock photocell on the hood near the slipfitter.
    pz = 12.6
    base = top_at(M400A_PROFILE, pz) - 0.35
    cylinder_y(m, 8.0, pz, 1.05, base, base + 0.55, "body", sides=12)
    cylinder_y(m, 8.0, pz, 0.95, base + 0.55, base + 2.0, "photocell", sides=12)
    return m


# GE M-400R3 (the late, 1990s M-400 body; Phase 2 of #99 converts the JSON gem400r3* to it). The
# JSON model this replaces is a 6 px wide box rising in steps to the rear, from 3 px over the
# optics to 4.5 px over the ballast, with the photocell at z 12..14.25: the R3's hood is level
# over the glass and tallest over the ballast, where the M-400A's swells over the optics. Held to
# the JSON footprint (x 5..11, y up to 6.5 with the photocell, down to -2.5 under a refractor) so
# the blocks' bounding boxes stay as registered (D2).
M400R3_PROFILE = [
    # (z, half-width, bottom y, top y)   nose (low z) -> slipfitter (z 16)
    (1.0, 1.4, 0.60, 1.60),
    (1.4, 2.2, 0.30, 2.30),
    (2.2, 2.8, 0.08, 2.90),
    (3.5, 3.0, 0.0, 3.30),
    (5.5, 3.0, 0.0, 3.80),
    (8.0, 3.0, 0.0, 4.10),
    (10.5, 2.95, 0.0, 4.40),
    (13.0, 2.8, -0.10, 4.50),
    (14.6, 2.6, -0.30, 4.45),
    (REAR_Z, 2.5, -0.40, 4.40),
]
M400R3_EXPONENT = 2.8


def build_m400r3(optics, photocell=True):
    m = Mesh()
    check_neck_holds_arm("M-400R3", M400R3_PROFILE, M400R3_EXPONENT)
    loft_housing(m, M400R3_PROFILE, M400R3_EXPONENT)
    if optics == "cutoff":
        box(m, (5.6, -0.3, 2.8), (10.4, 0.02, 10.4), "body", bottom_mat=LENS_MAT)
    else:
        # The JSON staircase reached y -2.5 at z 7..9 and ran from z 3 to 9.
        half_ellipsoid_down(m, (8.0, 0.05, 6.2), 2.6, 2.5, 3.2, LENS_MAT)
    box(m, (7.4, -0.25, 1.9), (8.6, 0.12, 2.7), "body")
    if not photocell:
        return m
    pz = 13.1
    base = top_at(M400R3_PROFILE, pz) - 0.35
    cylinder_y(m, 8.0, pz, 1.05, base, base + 0.55, "body", sides=12)
    cylinder_y(m, 8.0, pz, 0.95, base + 0.55, min(base + 2.0, 6.5), "photocell", sides=12)
    return m


# GE M-250 (CSM's "M240" ids are GE's M-250R and M-250R2: the M-400's smaller sibling, for
# residential streets). Two JSON bodies are converted in place (D2):
#   R1, gem240r1/np: the long early body, z 1..16, a 3 px box with a 4 px ballast hump from z 5 and
#       a refractor staircase at z 3..9 down to y -2.
#   R2, gem240r2/cu: the short R2 body, z 5..16, the refractor at z 7..12; the cutoff's bounding box
#       stops at y 0, so its flat glass sits in the housing's underside.
# Both are 6 px wide (x 5..11), the photocell at z 12..14.25 up to y 6.25.
M250R1_PROFILE = [
    # (z, half-width, bottom y, top y)   nose (low z) -> slipfitter (z 16)
    (1.0, 1.3, 0.55, 1.50),
    (1.4, 2.1, 0.25, 2.20),
    (2.2, 2.7, 0.06, 2.75),
    (3.5, 2.95, 0.0, 3.10),
    (6.0, 3.0, 0.0, 3.50),
    (9.0, 2.95, 0.0, 3.80),
    (12.0, 2.8, -0.05, 4.00),
    (14.4, 2.6, -0.25, 4.00),
    (REAR_Z, 2.5, -0.35, 3.95),
]
M250R2_PROFILE = [
    (5.0, 1.3, 0.55, 1.60),
    (5.4, 2.1, 0.25, 2.30),
    (6.2, 2.7, 0.06, 2.90),
    (7.5, 2.95, 0.0, 3.30),
    (9.5, 3.0, 0.0, 3.70),
    (11.5, 2.9, 0.0, 3.95),
    (13.5, 2.75, -0.08, 4.05),
    (14.8, 2.6, -0.20, 4.00),
    (REAR_Z, 2.5, -0.30, 3.95),
]
M250_EXPONENT = 2.8


def build_m250(body, optics, photocell=True):
    m = Mesh()
    profile = M250R1_PROFILE if body == "r1" else M250R2_PROFILE
    check_neck_holds_arm("M-250" + body.upper(), profile, M250_EXPONENT)
    loft_housing(m, profile, M250_EXPONENT)
    nose = profile[0][0]
    if optics == "cutoff":
        # Flat glass flush in the underside: the R2 cutoff's bounding box ends at y 0.
        box(m, (5.7, -0.15, nose + 1.6), (10.3, 0.02, nose + 7.6), "body", bottom_mat=LENS_MAT)
    elif body == "r1":
        # The JSON staircase ran from z 3 to 9 and reached y -2.
        half_ellipsoid_down(m, (8.0, 0.05, 6.0), 2.3, 2.0, 3.0, LENS_MAT)
    else:
        # z 7..12, y -2 at the back of it.
        half_ellipsoid_down(m, (8.0, 0.05, 9.6), 2.2, 2.0, 2.5, LENS_MAT)
    box(m, (7.4, -0.2, nose + 1.0), (8.6, 0.1, nose + 1.8), "body")
    if not photocell:
        return m
    pz = 13.1
    base = top_at(profile, pz) - 0.35
    cylinder_y(m, 8.0, pz, 1.05, base, base + 0.55, "body", sides=12)
    cylinder_y(m, 8.0, pz, 0.95, base + 0.55, min(base + 2.0, 6.25), "photocell", sides=12)
    return m


# registry name -> (display name, builder, body texture, bounding box comment)
CATALOGUE = [
    ("gem400a", "GE M-400A", lambda: build_m400a("refractor"),
     "streetlight_body_ge_grey"),
    ("gem400acu", "GE M-400A (Cutoff Optics)", lambda: build_m400a("cutoff"),
     "streetlight_body_ge_grey"),
    ("gem400acunp", "GE M-400A (Cutoff Optics, No Photocell)",
     lambda: build_m400a("cutoff", photocell=False), "streetlight_body_ge_grey"),
    # Converted in place from the JSON models (D2): same registry names, classes and bounding boxes.
    ("gem400r3", "GE M400R3 (Standard)", lambda: build_m400r3("refractor"),
     "streetlight_body_ge_grey"),
    ("gem400r3cu", "GE M400R3 (Cutoff Optics)", lambda: build_m400r3("cutoff"),
     "streetlight_body_ge_grey"),
    ("gem400r3cunp", "GE M400R3 (Cutoff Optics, No Photocell)",
     lambda: build_m400r3("cutoff", photocell=False), "streetlight_body_ge_grey"),
    ("gem240r1", "GE M240R1 (Standard)", lambda: build_m250("r1", "refractor"), "streetlight_body_ge_grey"),
    ("gem240r1np", "GE M240R1 (Standard, No Photocell)", lambda: build_m250("r1", "refractor", photocell=False),
     "streetlight_body_ge_grey"),
    ("gem240r2", "GE M240R2 (Standard)", lambda: build_m250("r2", "refractor"), "streetlight_body_ge_grey"),
    ("gem240r2cu", "GE M240R2 (Cutoff Optics)", lambda: build_m250("r2", "cutoff"),
     "streetlight_body_ge_grey"),
]


# Lens art cut from the requester's JSON fixture atlases (D5, hybrid): source crops in
# dev-env-utils/scripts/artwork/streetlight/, written as streetlight_lens_<art>_on/_off.
ARTWORK_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "artwork", "streetlight")
LENS_ART = ["ge_m400", "ge_m240"]


def art_lens(art, state):
    return "streetlight_lens_%s_%s" % (art, state)


# Each fixture's (lit, unlit) lens. Drop refractors wear the generated prismatic glass; flat
# glass the requester's lens art where the JSON fixture had some, else the generated HPS glass.
LENS_FOR = {
    "gem400a": (LENS_REFRACTOR_ON, LENS_REFRACTOR_OFF),
    "gem400r3": (LENS_REFRACTOR_ON, LENS_REFRACTOR_OFF),
    "gem400r3cu": (art_lens("ge_m400", "on"), art_lens("ge_m400", "off")),
    "gem400r3cunp": (art_lens("ge_m400", "on"), art_lens("ge_m400", "off")),
    "gem240r1": (LENS_REFRACTOR_ON, LENS_REFRACTOR_OFF),
    "gem240r1np": (LENS_REFRACTOR_ON, LENS_REFRACTOR_OFF),
    "gem240r2": (LENS_REFRACTOR_ON, LENS_REFRACTOR_OFF),
    "gem240r2cu": (art_lens("ge_m240", "on"), art_lens("ge_m240", "off")),
}


# --------------------------------------------------------------------------- textures
def swatch_png(top, bottom):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        t = y / 15.0
        col = tuple(int(round(top[k] + (bottom[k] - top[k]) * t)) for k in range(3)) + (255,)
        for x in range(16):
            img.putpixel((x, y), col)
    return img


def lens_png(lit):
    """Flat HPS glass: lit, a hot amber lamp image fading to orange at the frame; unlit, a dim
    smoky amber with a faint highlight. Opaque, so it bakes in CUTOUT_MIPPED like every fixture."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 7.5, (y - 7.5) / 7.5
            d = min(1.0, math.sqrt(dx * dx * 1.6 + dy * dy * 0.7))
            if lit:
                hot = (255, 236, 190)
                edge = (236, 120, 30)
            else:
                hot = (76, 65, 51)
                edge = (49, 41, 34)
            col = tuple(int(round(hot[k] + (edge[k] - hot[k]) * d)) for k in range(3)) + (255,)
            img.putpixel((x, y), col)
    return img


def refractor_on_png():
    """The requester's refractor orange, banded across the bowl: five light ribs on a body a
    little under his colour, so the bands read and the whole still reads as his orange."""
    orange = (255, 106, 19)
    rib = tuple(int(round(c + (255 - c) * 0.35)) for c in orange)
    body = tuple(int(round(c * 0.86)) for c in orange)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        col = (rib if y in (2, 5, 8, 11, 14) else body) + (255,)
        for x in range(16):
            img.putpixel((x, y), col)
    return img


def refractor_off_png():
    """Unlit prismatic glass: pale warm grey in horizontal prism bands, a little lighter where
    the bowl is deepest. Painted near the shaded housing's level, as it is drawn unshaded."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 7.5, (y - 7.5) / 7.5
            d = min(1.0, math.sqrt(dx * dx + dy * dy))
            band = 0.86 if y % 2 else 1.0
            base = (150, 146, 134)
            k = band * (1.0 - 0.18 * d)
            img.putpixel((x, y), tuple(int(round(c * k)) for c in base) + (255,))
    return img


def art_lens_png(art, state):
    """A lens crop from the requester's atlas, made a sprite: turned so the lamp lies along the
    fixture (the box lens runs v along z), scaled square (a taller-than-wide sprite is read as
    an animation strip) to 32 px. Drawn unshaded now, the unlit art is halved, so it looks as it
    did shaded; the lit art is left as painted, and so reads lit for the first time."""
    src = Image.open(os.path.join(ARTWORK_DIR, "%s_lens_%s.png" % (art, state))).convert("RGBA")
    img = src.transpose(Image.ROTATE_90).resize((32, 32), Image.NEAREST)
    if state == "off":
        img = Image.eval(img, lambda c: c)  # copy
        px = img.load()
        for y in range(32):
            for x in range(32):
                r, g, b, a = px[x, y]
                px[x, y] = (r // 2, g // 2, b // 2, a)
    return img


def all_textures():
    out = {name: swatch_png(*grad) for name, grad in SWATCHES.items()}
    out[LENS_ON] = lens_png(True)
    out[LENS_OFF] = lens_png(False)
    out[LENS_REFRACTOR_OFF] = refractor_off_png()
    out[LENS_REFRACTOR_ON] = refractor_on_png()
    for art in LENS_ART:
        for state in ("on", "off"):
            out[art_lens(art, state)] = art_lens_png(art, state)
    return out


# --------------------------------------------------------------------------- blockstates
def _display(rotation, translation_px, scale):
    # A blockstate transform's translation is in blocks, not the 1/16 a model's `display` uses.
    return {"rotation": rotation, "translation": [round(t / 16.0, 5) for t in translation_px],
            "scale": [scale] * 3}


# The JSON cobra heads' `display` (as ge_m400r3_cutoff.json had it before its conversion), so
# the OBJ fixtures sit in a slot, a hand and on the ground as their neighbours in the tab do:
# side on in the slot, lifted to its middle. forge:default-block draws a fixture this low and long as a sliver in the slot's corner.
INVENTORY_TRANSFORM = {
    "gui": _display([0, 90, -5], [0.5, 4.5, 0], 0.625),
    "ground": _display([0, 0, 0], [0, 6.5, 0], 1.0),
    "fixed": _display([0, 0, 0], [0, 0, 0], 0.5),
    "thirdperson_righthand": _display([0, 90, 0], [0, 4.5, 0], 0.55),
    "thirdperson_lefthand": _display([0, -90, 0], [0, 4.5, 0], 0.55),
    "firstperson_righthand": _display([0, 45, 0], [3, 5, 0], 0.4),
    "firstperson_lefthand": _display([0, 225, 0], [3, 5, 0], 0.4),
}


def blockstate(registry, body_tex, lens_on=LENS_ON, lens_off=LENS_OFF):
    model = "csm:lighting/shared_models/streetlight_%s.obj" % registry
    lit = {"textures": {"#" + LENS_MAT: TEX + lens_on}}
    # States 0 and 2 name the unlit lens rather than inheriting it: gen_lighting_lit_atlases.py
    # claims any blockstate whose 0 and 2 are empty (the troffers' rule, LIGHTING_SYSTEM.md).
    unlit = {"textures": {"#" + LENS_MAT: TEX + lens_off}}
    return {
        "forge_marker": 1,
        "defaults": {
            "model": model,
            "custom": {"flip-v": True},
            "textures": {"#body": TEX + body_tex, "#" + LENS_MAT: TEX + lens_off,
                         "#photocell": TEX + "streetlight_photocell"},
        },
        "variants": {
            "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}},
            "state": {"0": unlit, "1": lit, "2": unlit, "3": lit},
            "inventory": [{"transform": INVENTORY_TRANSFORM}],
        },
    }


def mtl_text():
    lines = ["# Generated by dev-env-utils/scripts/gen_streetlight_fixtures.py -- do not edit",
             "# One set of materials for every OBJ street light; each blockstate retextures them."]
    for mat, tex in (("body", "streetlight_body_ge_grey"), (LENS_MAT, LENS_OFF),
                     ("photocell", "streetlight_photocell")):
        lines += ["newmtl %s" % mat, "map_Kd %s%s" % (TEX, tex), ""]
    return "\n".join(lines)


# --------------------------------------------------------------------------- output
def outputs():
    """Every file the script owns: relative asset path -> bytes."""
    files = {MODEL_DIR + "/" + MTL_NAME: mtl_text().encode()}
    for registry, display, builder, body in CATALOGUE:
        mesh = builder()
        for x, y, z in mesh.v:
            assert 4.5 <= x <= 11.5 and -3.0 <= y <= 7.0 and 0.9 <= z <= REAR_Z + 1e-9, \
                "%s leaves the fixture envelope at %s" % (registry, (x, y, z))
        files[MODEL_DIR + "/streetlight_%s.obj" % registry] = mesh.text(display).encode()
        files["blockstates/%s.json" % registry] = (
            json.dumps(blockstate(registry, body, *LENS_FOR.get(registry, (LENS_ON, LENS_OFF))), indent=2) + "\n").encode()
    for name, img in all_textures().items():
        buf = io.BytesIO()
        img.save(buf, "PNG")
        files[TEX_DIR + "/" + name + ".png"] = buf.getvalue()
    return files


def same(path, data):
    if not os.path.exists(path):
        return False
    if path.endswith(".png"):
        a = Image.open(path).convert("RGBA")
        b = Image.open(io.BytesIO(data)).convert("RGBA")
        return a.size == b.size and a.tobytes() == b.tobytes()
    with open(path, "rb") as f:
        return f.read().replace(b"\r\n", b"\n") == data


def main(argv):
    check = "--check" in argv
    drift = []
    for rel, data in outputs().items():
        path = layout.asset_for_write(layout.owner_of_folder(OWNER), rel)
        if check:
            if not same(path, data):
                drift.append(rel)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(data)
        print("wrote", os.path.relpath(path, layout.REPO_ROOT))
    if check:
        if drift:
            print("gen_streetlight_fixtures: %d file(s) drifted:\n  %s" % (len(drift), "\n  ".join(drift)))
            return 1
        print("gen_streetlight_fixtures: up to date")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
