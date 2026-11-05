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
    # The Form 109's darker cast grey: merc_fixtures paints the housing 98 98 98.
    "streetlight_body_merc_grey": ((112, 112, 110), (84, 84, 82)),
    # American Electric's Autobahn: the light grey of ae_autobahn_atb0/2, 177 on the hood, 153 under.
    "streetlight_body_ael_grey": ((188, 188, 186), (156, 156, 154)),
}
LENS_ON = "streetlight_lens_hps_on"
LENS_OFF = "streetlight_lens_hps_off"
# A drop refractor with its lamp off is pale prismatic glass, not the dark smoked look of flat
# glass, so refractor fixtures name this for STATE 0 and 2 instead.
LENS_REFRACTOR_OFF = "streetlight_lens_refractor_off"
# Lit, a refractor wears the requester's refractor orange (ge_hps_fixtures, 255 106 19) in the
# ribs his JSON staircase showed, one band where each of its five steps caught the light.
LENS_REFRACTOR_ON = "streetlight_lens_refractor_on"
# The mercury fixtures' refractor (merc_fixtures, the GE Form 109): his olive glass, 135 135 86.
# That is a dull mid olive, and unshaded it could not read lit, so the lit sprite takes his hue at
# full lamp brightness (the lit-atlas audit found the olive had headroom) and ribs it the same way.
LENS_REFRACTOR_MERC_ON = "streetlight_lens_refractor_merc_on"
MERC_OLIVE = (135, 135, 86)

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


def lathe(mesh, cx, cz, rings, mat, lon=16):
    """A body of revolution about the vertical axis through (cx, cz): `rings` are (y, radius) from
    top to bottom, each pair bridged by a band. A band whose two rings share a y is a flat annulus
    facing down (the underside of a rim). Lens bands wrap their sprite round the axis, v by height,
    so a ribbed refractor's ribs become rings."""
    ys = [y for y, _ in rings]
    ytop, ybot = max(ys), min(ys)

    def pt(y, r, j):
        a = 2 * math.pi * j / lon
        return (cx + r * math.cos(a), y, cz + r * math.sin(a))

    def uv(y, j):
        t = (ytop - y) / (ytop - ybot)
        if mat == "body":
            return swatch_uv(j / lon, t)
        return (0.05 + 0.9 * j / lon, 0.05 + 0.9 * t)

    for (ya, ra), (yb, rb) in zip(rings, rings[1:]):
        inside = (cx, ya + 1.0, cz) if abs(ya - yb) < 1e-9 else (cx, (ya + yb) / 2, cz)
        for j in range(lon):
            quad = [(pt(ya, ra, j), uv(ya, j)), (pt(ya, ra, j + 1), uv(ya, j + 1)),
                    (pt(yb, rb, j + 1), uv(yb, j + 1)), (pt(yb, rb, j), uv(yb, j))]
            uniq = []
            for p, t in quad:
                if all(max(abs(p[k] - q[k]) for k in range(3)) > 1e-9 for q, _ in uniq):
                    uniq.append((p, t))
            if len(uniq) >= 3:
                mesh.poly_out(mat, [p for p, _ in uniq], [t for _, t in uniq], inside)


# GE Form 109 (1950s-60s): a mercury vapour fixture in two shapes, both converted in place (D2).
# The regular is a short, high-domed hood over a drop refractor: the JSON was a 6 x 3 px box at
# z 7..16 stepped up to y 5 over z 10..15, with a three-step bowl at z 8..15 down to y -2.5. The
# housing rises from the nose to a crown over the lamp, rounder than a cobra head's.
FORM109_PROFILE = [
    # (z, half-width, bottom y, top y)   nose (low z) -> slipfitter (z 16)
    (7.0, 1.4, 0.60, 1.80),
    (7.4, 2.2, 0.25, 2.80),
    (8.2, 2.8, 0.05, 3.70),
    (9.5, 3.0, 0.0, 4.40),
    (11.0, 3.0, 0.0, 4.80),
    (12.5, 2.95, 0.0, 4.95),
    (14.0, 2.8, -0.10, 4.70),
    (15.0, 2.6, -0.25, 4.20),
    (REAR_Z, 2.5, -0.35, 3.80),
]
FORM109_EXPONENT = 2.4

# The teardrop is a pendant: a domed hood with a finial, hung from a short neck, over a teardrop
# glass that comes to a point. The JSON stacked hexadecagons about (8, 9): radius 1 to 4 up the
# hood, a 5 px rim at y -2.25..-1.25 (the bounding box's x 3..13, z 4..14) and the glass narrowing
# to y -7.5, the arm stub at z 11..16. (y, radius) from the top:
FORM109TD_HOOD = [(5.15, 0.0), (5.0, 1.0), (4.4, 2.2), (3.2, 3.2), (1.4, 4.0), (-0.6, 4.6),
                  (-1.4, 5.0), (-2.0, 5.0), (-2.0, 4.6)]
FORM109TD_GLASS = [(-2.0, 4.6), (-3.4, 4.2), (-4.8, 3.3), (-6.0, 2.2), (-6.9, 1.2), (-7.45, 0.0)]
FORM109TD_NECK = [
    (11.5, 1.9, 0.0, 3.0),     # buried in the hood
    (13.6, 2.2, -0.30, 3.30),
    (REAR_Z, 2.5, -0.45, 3.60),
]
FORM109TD_EXPONENT = 2.6


def build_form109(teardrop=False):
    m = Mesh()
    if not teardrop:
        check_neck_holds_arm("Form 109", FORM109_PROFILE, FORM109_EXPONENT)
        loft_housing(m, FORM109_PROFILE, FORM109_EXPONENT)
        half_ellipsoid_down(m, (8.0, 0.05, 11.5), 2.3, 2.5, 3.4, LENS_MAT)
        return m
    check_neck_holds_arm("Form 109 teardrop", FORM109TD_NECK, FORM109TD_EXPONENT)
    lathe(m, 8.0, 9.0, FORM109TD_HOOD, "body")
    lathe(m, 8.0, 9.0, FORM109TD_GLASS, LENS_MAT)
    cylinder_y(m, 8.0, 9.0, 0.5, 4.9, 6.25, "body", sides=10)
    loft_housing(m, FORM109TD_NECK, FORM109TD_EXPONENT)
    return m


def photocell(m, profile, pz, top_cap):
    """The twist-lock photocontrol: a collar sunk into the hood, the cell on it."""
    base = top_at(profile, pz) - 0.35
    cylinder_y(m, 8.0, pz, 1.05, base, base + 0.55, "body", sides=12)
    cylinder_y(m, 8.0, pz, 0.95, base + 0.55, min(base + 2.0, top_cap), "photocell", sides=12)


def tube_y(mesh, cx, cz, r_out, r_in, y_top, y_bot, mat, lon=12):
    """An open-topped thin-walled skirt about the vertical axis: outer wall facing out, inner wall
    facing in, and no top or bottom (its top meets a disc; from below, the gap between the walls
    shows that disc's underside)."""
    def ring(r, y):
        return [(cx + r * math.cos(2 * math.pi * j / lon), y, cz + r * math.sin(2 * math.pi * j / lon))
                for j in range(lon)]
    for r, sign in ((r_out, -1.0), (r_in, 1.0)):
        top, bot = ring(r, y_top), ring(r, y_bot)
        for j in range(lon):
            j2 = (j + 1) % lon
            ps = [top[j], top[j2], bot[j2], bot[j]]
            mx = sum(p[0] for p in ps) / 4 - cx
            mz = sum(p[2] for p in ps) / 4 - cz
            # the point to face away from: toward the axis for the outer wall, away for the inner
            inside = (cx + mx * (1 + sign * 0.5), (y_top + y_bot) / 2, cz + mz * (1 + sign * 0.5))
            mesh.poly_out(mat, ps, [swatch_uv(j / lon, 0), swatch_uv((j + 1) / lon, 0),
                                    swatch_uv((j + 1) / lon, 1), swatch_uv(j / lon, 1)], inside)


# Westinghouse OV-20 and OV-50 (1960s-80s): classic cast cobra heads over a drop refractor, painted
# in the mercury fixtures' grey (98 98 98 on both westinghouse_ov50 and merc_fixtures) with
# AngelWingsPanda's olive refractor glass (135 135 86 on both), so they wear the Form 109's swatch
# and ribbed olive. Converted in place (D2):
#   OV-50, whov50*: z 1..16, a 3 px box with the hood stepped up to 4 px from z 7, a refractor
#       staircase at z 3..10.6 down to y -3, the photocell at z 12..14.25 up to y 6.
#   OV-20, whov20: the small residential head, z 7..16, the hood to y 4, the refractor to y -2.
OV50_PROFILE = [
    # (z, half-width, bottom y, top y)   nose (low z) -> slipfitter (z 16)
    (1.0, 1.3, 0.60, 1.60),
    (1.4, 2.2, 0.25, 2.40),
    (2.2, 2.85, 0.05, 2.95),
    (3.5, 3.0, 0.0, 3.35),
    (5.5, 3.0, 0.0, 3.70),
    (8.0, 3.0, 0.0, 3.95),
    (11.0, 2.9, 0.0, 4.00),
    (13.5, 2.75, -0.10, 3.95),
    (14.8, 2.6, -0.30, 3.85),
    (REAR_Z, 2.5, -0.40, 3.80),
]
OV20_PROFILE = [
    (7.0, 1.3, 0.60, 1.60),
    (7.4, 2.1, 0.25, 2.50),
    (8.2, 2.75, 0.05, 3.20),
    (9.5, 3.0, 0.0, 3.70),
    (11.5, 3.0, 0.0, 3.95),
    (13.5, 2.85, -0.05, 3.95),
    (14.8, 2.65, -0.25, 3.85),
    (REAR_Z, 2.5, -0.35, 3.75),
]
WESTINGHOUSE_EXPONENT = 2.8


def build_ov50(variant="standard", photocell_on=True):
    """variant: standard, finned (cooling fins over the ballast, no photocell, as the JSON had it)
    or shaded (a 12 px round glare shield at the top of the refractor, with a skirt round it)."""
    m = Mesh()
    check_neck_holds_arm("OV-50", OV50_PROFILE, WESTINGHOUSE_EXPONENT)
    loft_housing(m, OV50_PROFILE, WESTINGHOUSE_EXPONENT)
    # The JSON staircase ran from z 3 to 10.6 and reached y -3 at z 5..10.
    half_ellipsoid_down(m, (8.0, 0.05, 6.8), 2.6, 2.95, 3.6, LENS_MAT,
                        lat=5 if variant == "shaded" else 6)
    box(m, (7.4, -0.25, 1.9), (8.6, 0.12, 2.7), "body")
    if variant == "shaded":
        # The JSON's ring of plates about (8, 7): a disc 6 px in radius at y -0.5..0 and a skirt
        # 4..4.5 px out hanging to y -2.2. The disc stands 0.05 off the housing's flat bottom.
        cylinder_y(m, 8.0, 7.0, 6.0, -0.55, -0.05, "body", sides=12)
        under = [(8.0 + 6.0 * math.cos(2 * math.pi * j / 12), -0.55, 7.0 + 6.0 * math.sin(2 * math.pi * j / 12))
                 for j in range(12)]
        m.poly_out("body", under, [swatch_uv(0.5 + 0.5 * math.cos(2 * math.pi * j / 12),
                                             0.5 + 0.5 * math.sin(2 * math.pi * j / 12)) for j in range(12)],
                   (8.0, 0.0, 7.0))
        tube_y(m, 8.0, 7.0, 4.5, 4.0, -0.55, -2.2, "body")
    if variant == "finned":
        for z in (8.6, 9.7, 10.8, 11.9, 13.0, 14.1):
            box(m, (6.3, top_at(OV50_PROFILE, z) - 0.7, z - 0.18), (9.7, 5.3, z + 0.18), "body")
    elif photocell_on:
        photocell(m, OV50_PROFILE, 13.1, 6.0)
    return m


def build_ov20():
    m = Mesh()
    check_neck_holds_arm("OV-20", OV20_PROFILE, WESTINGHOUSE_EXPONENT)
    loft_housing(m, OV20_PROFILE, WESTINGHOUSE_EXPONENT)
    # The JSON staircase: z 8..15, y -2 at z 9..14.
    half_ellipsoid_down(m, (8.0, 0.05, 11.5), 2.3, 2.0, 2.6, LENS_MAT)
    box(m, (7.4, -0.2, 8.0), (8.6, 0.1, 8.8), "body")
    return m


# American Electric 115 (1970s-90s): a tall, boxy cast head on a narrower neck. The JSON: a head
# x 5..11, y -0.5..4 at z 5..12 over a neck x 6..10 to z 16, the refractor deepest at its rear
# (y -3 at z 8..10.5), the photocell at z 13..15.25 up to y 6.25. The cutoff wears the same framed
# lens panel on ge_hps_fixtures that the GE M240R2 cutoff does, so it reuses that cut.
AE115_PROFILE = [
    (5.0, 1.4, 0.40, 1.90),
    (5.35, 2.3, -0.10, 2.90),
    (6.0, 2.85, -0.40, 3.60),
    (7.5, 3.0, -0.50, 3.95),
    (10.0, 3.0, -0.50, 4.05),
    (11.8, 2.85, -0.45, 4.00),
    (13.0, 2.45, -0.32, 3.90),
    (14.6, 2.4, -0.35, 3.90),
    (REAR_Z, 2.4, -0.40, 3.90),
]
AE115_EXPONENT = 2.8


def build_ae115(optics):
    m = Mesh()
    check_neck_holds_arm("AEL 115", AE115_PROFILE, AE115_EXPONENT)
    loft_housing(m, AE115_PROFILE, AE115_EXPONENT)
    if optics == "cutoff":
        box(m, (5.6, -0.6, 6.3), (10.4, -0.45, 11.4), "body", bottom_mat=LENS_MAT)
    else:
        half_ellipsoid_down(m, (8.0, -0.45, 8.4), 2.6, 2.5, 2.4, LENS_MAT)
    box(m, (7.4, -0.7, 5.6), (8.6, -0.35, 6.4), "body")
    photocell(m, AE115_PROFILE, 14.1, 6.25)
    return m


# American Electric Autobahn ATB0 / ATB2: modern low-profile LED roadway heads, a flat underside
# carrying the LED array (AngelWingsPanda's array art, cut from ae_autobahn_atb0/2), a smooth hood
# that thickens toward the slipfitter. The ATB2 is the bigger, flatter one. Their JSON bounding
# boxes are wider and longer than the cobra heads' envelope: x 4..12 and 3..13, z 0..16.
ATB0_PROFILE = [
    (0.3, 2.6, 1.00, 1.90),
    (0.8, 3.5, 0.55, 2.40),
    (1.8, 3.95, 0.30, 2.80),
    (4.0, 4.0, 0.30, 3.10),
    (8.0, 4.0, 0.30, 3.40),
    (11.0, 3.95, 0.30, 3.70),
    (13.0, 3.6, -0.10, 3.90),
    (14.8, 3.1, -0.30, 3.95),
    (REAR_Z, 3.0, -0.35, 3.95),
]
# Only 2 px thick over the optic (bounding box y 0..4), so the slipfitter end rises to take the arm
# and the photocell stands on the flat ahead of that rise, where the box leaves it room.
ATB2_PROFILE = [
    (0.3, 3.6, 0.35, 1.00),
    (0.8, 4.5, 0.25, 1.35),
    (1.8, 4.95, 0.20, 1.60),
    (5.0, 5.0, 0.20, 1.80),
    (9.0, 4.9, 0.20, 2.00),
    (11.5, 4.6, 0.10, 2.40),
    (13.2, 3.9, -0.15, 3.20),
    (14.6, 3.2, -0.30, 3.75),
    (REAR_Z, 3.0, -0.35, 3.85),
]
ATB_EXPONENT = 2.6


def build_atb(size):
    m = Mesh()
    profile = ATB0_PROFILE if size == 0 else ATB2_PROFILE
    check_neck_holds_arm("Autobahn ATB%d" % size, profile, ATB_EXPONENT)
    loft_housing(m, profile, ATB_EXPONENT)
    if size == 0:
        box(m, (4.6, 0.15, 1.6), (11.4, 0.32, 10.8), "body", bottom_mat=LENS_MAT)
        photocell(m, profile, 13.0, 6.0)
    else:
        box(m, (3.7, 0.05, 1.6), (12.3, 0.22, 11.0), "body", bottom_mat=LENS_MAT)
        photocell(m, profile, 11.4, 4.05)
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
    ("geform109", "GE Form 109 (Regular)", lambda: build_form109(), "streetlight_body_merc_grey"),
    ("geform109td", "GE Form 109 (Teardrop)", lambda: build_form109(teardrop=True),
     "streetlight_body_merc_grey"),
    ("whov20", "Westinghouse OV20", build_ov20, "streetlight_body_merc_grey"),
    ("whov50", "Westinghouse OV50 (Standard)", lambda: build_ov50(), "streetlight_body_merc_grey"),
    ("whov50finned", "Westinghouse OV50 (Finned)", lambda: build_ov50("finned"),
     "streetlight_body_merc_grey"),
    ("whov50np", "Westinghouse OV50 (No Photocell)", lambda: build_ov50(photocell_on=False),
     "streetlight_body_merc_grey"),
    ("whov50shaded", "Westinghouse OV50 (Shaded)", lambda: build_ov50("shaded"),
     "streetlight_body_merc_grey"),
    ("whov50shadednp", "Westinghouse OV50 (Shaded, No Photocell)",
     lambda: build_ov50("shaded", photocell_on=False), "streetlight_body_merc_grey"),
    ("ae115", "American Electric 115 (Standard)", lambda: build_ae115("refractor"),
     "streetlight_body_ge_grey"),
    ("ae115cu", "American Electric 115 (Cutoff Optics)", lambda: build_ae115("cutoff"),
     "streetlight_body_ge_grey"),
    ("aeatb0", "American Electric Autobahn ATB0", lambda: build_atb(0), "streetlight_body_ael_grey"),
    ("aeatb2", "American Electric Autobahn ATB2", lambda: build_atb(2), "streetlight_body_ael_grey"),
]

# The cobra heads' envelope (x 5..11, z 1..16 with a little slack) is every fixture's, except a
# converted fixture whose JSON bounding box was bigger: there the bounding box is the envelope.
ENVELOPE = ((4.5, 11.5), (-3.0, 7.0), (0.9, REAR_Z + 1e-9))
ENVELOPE_FOR = {
    "geform109td": ((3.0, 13.0), (-7.5, 6.25), (4.0, REAR_Z + 1e-9)),
    # The OV-50's round glare shield is 12 px across (x 2..14).
    "whov50shaded": ((2.0, 14.0), (-3.0, 6.0), (1.0, REAR_Z + 1e-9)),
    "whov50shadednp": ((2.0, 14.0), (-3.0, 4.0), (1.0, REAR_Z + 1e-9)),
    # The Autobahns' boxes run x 4..12 / 3..13 and z 0..16 from y 0; the neck dips the fraction of a
    # pixel below y 0 that it needs to swallow the arm's end (ARM_TIP), as every cobra head's does.
    "aeatb0": ((4.0, 12.0), (-0.5, 6.0), (0.0, REAR_Z + 1e-9)),
    "aeatb2": ((3.0, 13.0), (-0.5, 4.1), (0.0, REAR_Z + 1e-9)),
}


# Lens art cut from the requester's JSON fixture atlases (D5, hybrid): source crops in
# dev-env-utils/scripts/artwork/streetlight/, written as streetlight_lens_<art>_on/_off.
ARTWORK_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "artwork", "streetlight")
# ael_atb0/2 are the Autobahns' LED arrays (the dies white lit, dimmed dark on a pale optic unlit).
LENS_ART = ["ge_m400", "ge_m240", "ael_atb0", "ael_atb2"]


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
    "geform109": (LENS_REFRACTOR_MERC_ON, LENS_REFRACTOR_OFF),
    "geform109td": (LENS_REFRACTOR_MERC_ON, LENS_REFRACTOR_OFF),
    "whov20": (LENS_REFRACTOR_MERC_ON, LENS_REFRACTOR_OFF),
    "whov50": (LENS_REFRACTOR_MERC_ON, LENS_REFRACTOR_OFF),
    "whov50finned": (LENS_REFRACTOR_MERC_ON, LENS_REFRACTOR_OFF),
    "whov50np": (LENS_REFRACTOR_MERC_ON, LENS_REFRACTOR_OFF),
    "whov50shaded": (LENS_REFRACTOR_MERC_ON, LENS_REFRACTOR_OFF),
    "whov50shadednp": (LENS_REFRACTOR_MERC_ON, LENS_REFRACTOR_OFF),
    "ae115": (LENS_REFRACTOR_ON, LENS_REFRACTOR_OFF),
    "ae115cu": (art_lens("ge_m240", "on"), art_lens("ge_m240", "off")),
    "aeatb0": (art_lens("ael_atb0", "on"), art_lens("ael_atb0", "off")),
    "aeatb2": (art_lens("ael_atb2", "on"), art_lens("ael_atb2", "off")),
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


def refractor_on_png(orange=(255, 106, 19)):
    """The requester's refractor orange, banded across the bowl: five light ribs on a body a
    little under his colour, so the bands read and the whole still reads as his orange."""
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
    # his olive at lamp brightness: the hue kept, the brightest channel raised to 232
    out[LENS_REFRACTOR_MERC_ON] = refractor_on_png(
        tuple(int(round(c * 232.0 / max(MERC_OLIVE))) for c in MERC_OLIVE))
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
        (xa, xb), (ya, yb), (za, zb) = ENVELOPE_FOR.get(registry, ENVELOPE)
        for x, y, z in mesh.v:
            assert xa - 1e-9 <= x <= xb + 1e-9 and ya - 1e-9 <= y <= yb + 1e-9 and za - 1e-9 <= z <= zb, \
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
