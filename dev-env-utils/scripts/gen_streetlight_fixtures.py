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
  textures/blocks/lighting/shared_textures/streetlight_*.png      16 px swatches, shared by all,
                                                                  and 32 px lens sprites

Every fixture wears the same three materials, so a new fixture adds geometry and at most a small
lens sprite (cut from the requester's art, or an LED array drawn by `led_array_png`):
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
    # Leotek's die-cast grey: every ltec/ltgc sheet paints the hood 186 and the underside 153.
    "streetlight_body_leotek_grey": ((196, 196, 196), (156, 156, 156)),
    # The smart-city node the GreenCobra "SN" heads carry in the photocell's receptacle: the blue
    # (0, 92, 154) of ltgcj_smartnode / ltgcm_smartnode, worn on the photocell material.
    "streetlight_smartnode": ((10, 104, 170), (0, 80, 138)),
    # The GE Evolve's grey: ge_evolve_large/small paint the hood 186 and its flanks 177.
    "streetlight_body_ge_evolve_grey": ((196, 196, 196), (170, 170, 170)),
    # Cree's LEDway and XSP and Cooper's Navion share one paint on their sheets: 186 on the housing,
    # 132 on the panel's edges.
    "streetlight_body_led_grey": ((194, 194, 194), (142, 142, 142)),
    # The Cree Stylus's housing, the troffers sheet's 184 183 185.
    "streetlight_body_stylus_grey": ((194, 193, 195), (172, 171, 173)),
    # Solarmax's warm grey: solarmax_smx paints the head 188 183 172 and its edges 153 152 145.
    "streetlight_body_solarmax": ((196, 191, 180), (150, 149, 142)),
    # The Alto lot lights' housing: metal_silver, a brushed grey from 94 to 116 (106 on average).
    "streetlight_body_alto_silver": ((120, 120, 120), (96, 96, 96)),
    # Leotek's KarbonCobra grey, RAL 7040 (window grey) on its sheets.
    "streetlight_body_leotek_karbon": ((178, 183, 189), (140, 145, 151)),
    # The Westinghouse OV-25 "Silverliner": bare cast aluminium.
    "streetlight_body_westinghouse_silver": ((186, 188, 188), (140, 142, 142)),
    # The SolarMax SMX-LL5's near-white housing (his reference photo).
    "streetlight_body_solarmax_white": ((234, 234, 230), (196, 196, 192)),
    # Black and dark bronze, the area lights' standard finishes (GE Evolve, Holophane).
    "streetlight_body_black": ((72, 72, 74), (44, 44, 46)),
    "streetlight_body_dark_bronze": ((94, 80, 66), (62, 52, 43)),
    # City of Anaheim P7-Green (Pantone 330 C), the decorative poles' paint_anaheim_green: the
    # cobra head on an Anaheim Resort Area pole is painted with its pole.
    "streetlight_body_anaheim_green": ((40, 104, 88), (26, 78, 66)),
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
# The same olive as flat glass, for the Alto lot and wall lights: merc_fixtures is four flat
# quadrants (housing 98, photocell black, the olive glass, a pale 182 globe), no framed lens art,
# so the sprite is made from his colour rather than cut. Lit at lamp brightness, unlit at half.
LENS_MERC_GLASS_ON = "streetlight_lens_merc_glass_on"
LENS_MERC_GLASS_OFF = "streetlight_lens_merc_glass_off"
# The GE Powerbracket's globe: his pale 182 glass round the sides (the JSON's ring of plates) and
# his olive lamp seen through the bottom of it, one sprite down the lathe.
LENS_POWERBRACKET_ON = "streetlight_lens_powerbracket_on"
LENS_POWERBRACKET_OFF = "streetlight_lens_powerbracket_off"
MERC_GLOBE = (182, 182, 182)

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


def lathe(mesh, cx, cz, rings, mat, lon=16, phase=0.0):
    """A body of revolution about the vertical axis through (cx, cz): `rings` are (y, radius) from
    top to bottom, each pair bridged by a band. A band whose two rings share a y is a flat annulus
    facing down (the underside of a rim). Lens bands wrap their sprite round the axis, v by height,
    so a ribbed refractor's ribs become rings. `phase` turns the facets by that fraction of one."""
    ys = [y for y, _ in rings]
    ytop, ybot = max(ys), min(ys)

    def pt(y, r, j):
        a = 2 * math.pi * (j + phase) / lon
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


# Leotek's LED cobra heads: the E-Cobra and the GreenCobra family. All are flat-bottomed die-cast
# heads: a thin slab carrying the LED array underneath, a driver housing that rises behind it to
# the slipfitter, the twist-lock receptacle on top of that. The JSON drew each as a 1 px panel, a
# 1-2 px hump and a 3-5 px rear box; the lofted sections follow those steps. Their squarer section
# (LEOTEK_EXPONENT) is both the look and what lets the 4 px GCJ's neck swallow the 3 px arm.
# GC1 and GCJ wear cooling fins on top of the slab (the ribbed band of their sheets); the GCL, GCM
# and E-Cobra are smooth.
#   E-Cobra, ltec: z 0..16, x 4..12; the slab to z 11, the rear 3 px. Dusk to Dawn, ltecdtd: the
#       short body from z 5, with the built-in sensor's eye under the rear (the JSON kept the
#       twist-lock too).
#   GC1, ltgc1v1/np, ltgc1v2: z 1..16, x 4..12, a 5 px driver housing from z 10.
#   GCJ, ltgcj/sn: the junior, z 8..16 and only x 6..10.
#   GCL, ltgcl (x 4..12) and GCM, ltgcm/sn, ltgcmv2/np (x 5..11): z 4..16, the rear 3 px.
# v1 and v2 differ only in their LED art. "np" carries a blank cap in the receptacle, "sn" a
# smart-city node in place of the photocell.
LEOTEK_EXPONENT = 3.2
ECOBRA_REAR = [
    (11.2, 3.95, 0.05, 2.90),
    (12.6, 3.85, -0.20, 3.40),
    (14.6, 3.8, -0.30, 3.45),
    (REAR_Z, 3.8, -0.30, 3.45),
]
ECOBRA_PROFILE = [
    # (z, half-width, bottom y, top y)   nose (low z) -> slipfitter (z 16)
    (0.2, 3.2, 0.50, 0.95),
    (0.6, 3.85, 0.28, 1.20),
    (1.4, 4.0, 0.20, 1.30),
    (6.0, 4.0, 0.20, 1.40),
    (9.8, 4.0, 0.20, 1.60),
] + ECOBRA_REAR
ECOBRA_DTD_PROFILE = [
    (5.2, 3.2, 0.50, 0.95),
    (5.6, 3.85, 0.28, 1.20),
    (6.4, 4.0, 0.20, 1.30),
    (9.8, 4.0, 0.20, 1.55),
] + ECOBRA_REAR
GC1_PROFILE = [
    (1.2, 3.0, 0.50, 0.95),
    (1.6, 3.8, 0.28, 1.20),
    (2.4, 4.0, 0.20, 1.30),
    (5.6, 4.0, 0.20, 1.45),
    (6.8, 4.0, 0.20, 2.60),
    (9.4, 4.0, 0.15, 3.00),
    (10.4, 4.0, 0.05, 4.70),
    (12.2, 3.95, -0.10, 5.00),
    (14.6, 3.85, -0.30, 4.95),
    (REAR_Z, 3.8, -0.30, 4.90),
]
GCJ_PROFILE = [
    (8.15, 1.4, 0.45, 0.95),
    (8.5, 1.9, 0.25, 1.15),
    (9.1, 2.0, 0.20, 1.25),
    (10.6, 2.0, 0.20, 1.35),
    (11.4, 2.0, 0.20, 2.05),
    (12.6, 2.0, 0.12, 2.25),
    (13.3, 2.0, -0.05, 3.55),
    (14.6, 2.0, -0.30, 3.75),
    (REAR_Z, 2.0, -0.30, 3.75),
]


def gc_medium_profile(hw):
    """The GCL (hw 4) and GCM (hw 3): one body at two widths."""
    return [
        (4.15, hw - 1.0, 0.45, 0.95),
        (4.5, hw - 0.15, 0.25, 1.15),
        (5.2, hw, 0.20, 1.25),
        (6.6, hw, 0.20, 1.40),
        (7.6, hw, 0.20, 2.05),
        (10.2, hw, 0.15, 2.25),
        (11.4, hw, 0.0, 3.35),
        (12.8, hw - 0.05, -0.20, 3.60),
        (14.6, hw - 0.1, -0.30, 3.60),
        (REAR_Z, hw - 0.1, -0.30, 3.60),
    ]


# model -> (profile, lens box (x0, x1, z0, z1), fins (x offsets, z0, z1, top) or None, receptacle z,
#           top of the photocell)
LEOTEK = {
    "ec": (ECOBRA_PROFILE, (4.7, 11.3, 1.4, 10.4), None, 13.2, 5.0),
    "ecdtd": (ECOBRA_DTD_PROFILE, (4.7, 11.3, 6.2, 10.4), None, 13.2, 5.0),
    "gc1": (GC1_PROFILE, (4.7, 11.3, 2.4, 9.8), ((-3.0, -1.5, 0.0, 1.5, 3.0), 2.6, 6.6, 2.25), 11.5, 7.0),
    "gcj": (GCJ_PROFILE, (6.45, 9.55, 9.1, 12.9), ((-1.1, 0.0, 1.1), 9.3, 11.8, 1.85), 14.5, 5.0),
    "gcl": (gc_medium_profile(4.0), (4.7, 11.3, 5.2, 10.8), None, 12.4, 5.0),
    "gcm": (gc_medium_profile(3.0), (5.6, 10.4, 5.2, 10.8), None, 12.4, 5.0),
}


def build_leotek(model, top="photocell"):
    """top: photocell (the twist-lock cell), cap (a blank cap in the receptacle), node (a smart
    node: a squat puck and its dome, on the photocell material, which its blockstate paints blue)."""
    profile, (lx0, lx1, lz0, lz1), fins, pz, cell_top = LEOTEK[model]
    m = Mesh()
    check_neck_holds_arm("Leotek " + model.upper(), profile, LEOTEK_EXPONENT)
    loft_housing(m, profile, LEOTEK_EXPONENT)
    # The LED array: a slab just under the flat bottom, its top buried in the housing.
    box(m, (lx0, 0.0, lz0), (lx1, 0.3, lz1), "body", bottom_mat=LENS_MAT)
    if fins:
        offsets, fz0, fz1, ftop = fins
        for dx in offsets:
            box(m, (8.0 + dx - 0.18, 0.7, fz0), (8.0 + dx + 0.18, ftop, fz1), "body")
    if model == "ecdtd":
        # the built-in dusk-to-dawn sensor's eye, under the driver housing
        box(m, (7.0, -0.5, 13.0), (9.0, -0.05, 15.0), "body", bottom_mat="photocell")
    base = top_at(profile, pz) - 0.35
    if top == "photocell":
        photocell(m, profile, pz, cell_top)
    elif top == "cap":
        cylinder_y(m, 8.0, pz, 1.05, base, base + 0.55, "body", sides=12)
    elif top == "node":
        cylinder_y(m, 8.0, pz, 1.15, base, base + 0.6, "photocell", sides=12)
        cylinder_y(m, 8.0, pz, 0.7, base + 0.6, base + 0.85, "photocell", sides=12)
    return m


def receptacle_top(m, profile, pz, top, cell_top):
    """What stands in the twist-lock receptacle: the photocell, a blank cap (a receptacle with
    nothing in it), or a smart node (a squat puck and its dome, on the photocell material)."""
    base = top_at(profile, pz) - 0.35
    if top == "photocell":
        photocell(m, profile, pz, cell_top)
    elif top == "cap":
        cylinder_y(m, 8.0, pz, 1.05, base, base + 0.55, "body", sides=12)
    elif top == "node":
        cylinder_y(m, 8.0, pz, 1.15, base, base + 0.6, "photocell", sides=12)
        cylinder_y(m, 8.0, pz, 0.7, base + 0.6, base + 0.85, "photocell", sides=12)


# GE Evolve ERS2 (CSM's "geel"/"gees", Large and Small): a slim LED roadway head, a 1 px plate
# carrying the LED module under a finned top, the driver housing rising behind it to the
# slipfitter. The JSON: a plate x 5..11, y 0..1 from z 7 (Large) or 9 (Small) to 16, a narrower
# nose 1 px ahead of it, a 1 px hump x 6..10 over z 11..16 and the receptacle at z 13..15. Its
# boxes are only 4 px tall (5 with a smart node, 6 with the outlet), so the rear rises just enough
# to swallow the arm's 3 px end and the receptacle stands on the plate ahead of that rise, as the
# ATB2's does. The vents his sheet paints on top of the plate are cooling fins along it.
#   po: a raised photocell outlet with the photocell on it; sn: a taller, dark smart node cell.
def evolve_profile(nose):
    return [
        # (z, half-width, bottom y, top y)   nose (low z) -> slipfitter (z 16)
        (nose + 0.1, 1.6, 0.30, 0.85),
        (nose + 0.5, 2.5, 0.18, 1.00),
        (nose + 1.2, 3.0, 0.15, 1.10),
        (10.6, 3.0, 0.15, 1.25),
        (12.4, 2.95, 0.10, 1.75),
        (13.6, 2.75, -0.15, 3.00),
        (14.6, 2.6, -0.30, 3.55),
        (REAR_Z, 2.55, -0.35, 3.60),
    ]


EVOLVE_EXPONENT = 3.0
# size -> (nose z, lens box (x0, x1, z0, z1), fin z0, z1)
EVOLVE = {
    "large": (6.0, (5.5, 10.5, 7.4, 12.3), 7.8, 10.6),
    "small": (8.0, (5.5, 10.5, 9.4, 12.2), 9.5, 10.7),
}


def build_evolve(size, top="photocell"):
    nose, (lx0, lx1, lz0, lz1), fz0, fz1 = EVOLVE[size]
    profile = evolve_profile(nose)
    m = Mesh()
    check_neck_holds_arm("GE Evolve " + size, profile, EVOLVE_EXPONENT)
    loft_housing(m, profile, EVOLVE_EXPONENT)
    box(m, (lx0, 0.0, lz0), (lx1, 0.3, lz1), "body", bottom_mat=LENS_MAT)
    for dx in (-2.0, -1.0, 0.0, 1.0, 2.0):
        box(m, (8.0 + dx - 0.15, 0.9, fz0), (8.0 + dx + 0.15, 1.55, fz1), "body")
    pz = 12.0
    base = top_at(profile, pz) - 0.35
    if top == "outlet":
        # As the JSON drew it: a 2 px raised photocell outlet (y 2..4), the photocell on it (4..6).
        cylinder_y(m, 8.0, pz, 1.05, base, 4.0, "body", sides=12)
        cylinder_y(m, 8.0, pz, 0.95, 4.0, 6.0, "photocell", sides=12)
    elif top == "node":
        # As the JSON drew it: the smart node is a taller cell (to y 5) in the photocell's dark.
        cylinder_y(m, 8.0, pz, 1.05, base, base + 0.55, "body", sides=12)
        cylinder_y(m, 8.0, pz, 0.95, base + 0.55, 5.0, "photocell", sides=12)
    else:
        receptacle_top(m, profile, pz, top, 4.0)
    return m


# Cree LEDway: a thin LED panel out front, x 4..12 and 1 px thick with its underside at y 2, on a
# boxy 8 px driver housing at z 10..16 that carries the photocell at z 13..15. The Small's panel is
# half as long (z 5..10). The JSON's housing is y 1..4 over a 4 px neck at y 0..1; the loft's
# flat bottom runs at the neck's depth so the arm's end is inside it.
LEDWAY_REAR = [
    (9.6, 3.6, 1.10, 3.40),
    (10.1, 3.95, 0.60, 4.10),
    (11.0, 4.0, 0.10, 4.60),
    (12.5, 4.0, -0.20, 4.85),
    (14.6, 3.95, -0.35, 4.90),
    (REAR_Z, 3.9, -0.35, 4.90),
]
LEDWAY_EXPONENT = 3.6


def ledway_panel(nose):
    return [
        (nose + 0.1, 3.4, 2.25, 2.85),
        (nose + 0.5, 3.95, 2.05, 3.0),
        (nose + 1.0, 4.0, 2.0, 3.05),
        (10.6, 4.0, 2.0, 3.10),
    ]


def build_ledway(small=False):
    nose = 5.0 if small else 0.0
    m = Mesh()
    check_neck_holds_arm("Cree LEDway", LEDWAY_REAR, LEDWAY_EXPONENT)
    loft_housing(m, LEDWAY_REAR, LEDWAY_EXPONENT)
    loft_housing(m, ledway_panel(nose), 4.0)
    box(m, (4.6, 1.85, nose + 0.7), (11.4, 2.15, 9.7), "body", bottom_mat=LENS_MAT)
    photocell(m, LEDWAY_REAR, 13.8, 7.0 if small else 8.0)
    return m


# Cree XSP: a broad flat LED head, x 4..12 from z 2, its top stepping up toward the slipfitter
# (the JSON's 1 px plate under 6 and 4 px wide tiers), the photocell at z 11..13. His two LED
# modules lie side by side across the front half of the underside.
XSP_PROFILE = [
    (2.1, 3.0, 0.25, 0.85),
    (2.6, 3.7, 0.12, 1.00),
    (3.3, 4.0, 0.10, 1.15),
    (5.5, 4.0, 0.10, 1.80),
    (8.0, 3.95, 0.10, 2.45),
    (11.0, 3.9, 0.05, 2.85),
    (13.5, 3.85, -0.10, 3.15),
    (14.8, 3.8, -0.30, 3.45),
    (REAR_Z, 3.8, -0.35, 3.50),
]
XSP_EXPONENT = 3.4


def build_xsp():
    m = Mesh()
    check_neck_holds_arm("Cree XSP", XSP_PROFILE, XSP_EXPONENT)
    loft_housing(m, XSP_PROFILE, XSP_EXPONENT)
    box(m, (4.7, 0.0, 4.0), (11.3, 0.3, 9.6), "body", bottom_mat=LENS_MAT)
    photocell(m, XSP_PROFILE, 12.0, 5.0)
    return m


def mirror_y(mesh, c):
    """The mesh turned upside down about y = c: every y becomes 2c - y, the windings reversed so
    the faces still point out."""
    out = Mesh()
    out.v = [(x, 2 * c - y, z) for x, y, z in mesh.v]
    out.vt = list(mesh.vt)
    out.vn = [(x, -y, z) for x, y, z in mesh.vn]
    out.faces = [(mat, idx[::-1]) for mat, idx in mesh.faces]
    return out


# Cree Stylus: a linear LED fixture hung flush under a ceiling, not from an arm. The JSON: a
# 6 x 4 px bar at the top of the block (y 12..16) the length of the block (the Full runs on into
# the block to the north, z -16..16, as one 2-block fixture) over a 4 px diffuser at y 11..12,
# both on the troffers sheet. Here the bar is a rounded extrusion, its flat top 0.1 off the
# ceiling, the diffuser along its underside wearing his troffer white.
def build_stylus(full):
    z0 = -REAR_Z if full else 16.0 - REAR_Z
    m = Mesh()
    # Lofted with its flat side down, then turned over: the flat side becomes the top.
    loft_housing(m, [(z0, 3.0, 0.1, 4.0), (REAR_Z, 3.0, 0.1, 4.0)], 3.2)
    m = mirror_y(m, 8.0)
    box(m, (6.0, 11.1, z0 + 0.6), (10.0, 12.3, REAR_Z - 0.6), "body", bottom_mat=LENS_MAT)
    return m


# Cooper Industries Navion: an LED roadway head whose LightSquare modules sit in an open frame
# ahead of the driver housing: the JSON's flat 1 px panel x 5..11 from z 0 (z -4 for the three
# square "alt") to 10, three rails along it (x 5..6, 7..9, 10..11, y 1..3) closed at the nose by
# three posts, on a 6 x 4 px housing at z 10..16 with the photocell at z 13..15. Boxy, as the
# JSON and the real fixture are; the housing a squared loft round the arm's end.
NAVION_REAR = [
    (9.6, 2.8, 0.40, 3.30),
    (10.1, 3.0, 0.05, 3.85),
    (11.0, 3.0, -0.10, 4.0),
    (14.6, 3.0, -0.35, 4.0),
    (REAR_Z, 3.0, -0.35, 4.0),
]
NAVION_EXPONENT = 4.0


def build_navion(squares):
    nose = 0.1 if squares == 2 else -3.9
    m = Mesh()
    check_neck_holds_arm("Navion", NAVION_REAR, NAVION_EXPONENT)
    loft_housing(m, NAVION_REAR, NAVION_EXPONENT)
    # The panel: its underside is the LightSquares, its top buried under the rails.
    box(m, (5.2, 0.0, nose + 0.05), (10.8, 0.9, 10.2), "body", bottom_mat=LENS_MAT)
    for x0, x1 in ((5.1, 6.0), (7.0, 9.0), (10.0, 10.9)):
        box(m, (x0, 0.8, nose + 0.9), (x1, 3.0, 10.7), "body")      # the rail, into the housing
        box(m, (x0 - 0.05, 0.7, nose), (x1 + 0.05, 3.05, nose + 1.0), "body")  # the post at the nose
    photocell(m, NAVION_REAR, 14.0, 6.0)
    return m


# Solarmax SMX (the first generation): a broad flat head, 2 px thick at y -1..1, ten wide with its
# corners cut (the JSON's stepped plates: x 4..12 at the ends, 3..13 through the middle), from
# z 1 to 14, on a 4 px neck at z 10..16 that rises over the head to the slipfitter, the photocell
# on the neck at z 11..13. His LED panel (two modules) is under the front half, z 3..8, across the
# full 8 px.
SMX_HEAD = [
    (1.1, 3.6, -0.85, 0.85),
    (1.6, 4.5, -1.0, 1.0),
    (2.6, 5.0, -1.0, 1.0),
    (12.4, 5.0, -1.0, 1.0),
    (13.4, 4.5, -1.0, 1.0),
    (13.9, 3.6, -0.85, 0.85),
]
SMX_NECK = [
    (9.0, 2.0, 0.0, 2.0),      # buried in the head
    (11.0, 2.4, -0.25, 3.40),
    (14.6, 2.4, -0.35, 3.60),
    (REAR_Z, 2.4, -0.35, 3.60),
]
SMX_EXPONENT = 3.0


def build_smx():
    m = Mesh()
    check_neck_holds_arm("Solarmax SMX", SMX_NECK, SMX_EXPONENT)
    loft_housing(m, SMX_HEAD, 4.0)
    loft_housing(m, SMX_NECK, SMX_EXPONENT)
    box(m, (4.0, -1.15, 3.0), (12.0, -0.8, 8.0), "body", bottom_mat=LENS_MAT)
    photocell(m, SMX_NECK, 12.0, 5.0)
    return m


def rounded_ring(z, cx, cy, hw, hh, ex, n):
    """A closed superellipse section in the x-y plane at depth z, centred on (cx, cy)."""
    pts = []
    for j in range(n):
        a = 2 * math.pi * j / n
        c, s = math.cos(a), math.sin(a)
        pts.append((cx + math.copysign(abs(c) ** (2.0 / ex), c) * hw,
                    cy + math.copysign(abs(s) ** (2.0 / ex), s) * hh, z))
    return pts


def loft_closed(mesh, sections, ex, mat="body", front_mat=None, n=16):
    """A rounded box along z: closed superellipse sections (z, cx, cy, half-width, half-height),
    bridged and capped at both ends. A section's centre may move, so a run of them can climb
    (a sloping arm). `front_mat` paints the low-z cap, mapped flat across it, x and y."""
    rings = [rounded_ring(z, cx, cy, hw, hh, ex, n) for (z, cx, cy, hw, hh) in sections]
    zs = [s[0] for s in sections]
    zmin, zmax = min(zs), max(zs)
    for i in range(len(rings) - 1):
        a, b = rings[i], rings[i + 1]
        sa, sb = sections[i], sections[i + 1]
        inside = ((sa[1] + sb[1]) / 2, (sa[2] + sb[2]) / 2, (sa[0] + sb[0]) / 2)
        va, vb = (sa[0] - zmin) / (zmax - zmin), (sb[0] - zmin) / (zmax - zmin)
        for j in range(n):
            j2 = (j + 1) % n
            if mat == "body":
                uvs = [swatch_uv(j / n, va), swatch_uv((j + 1) / n, va), swatch_uv((j + 1) / n, vb),
                       swatch_uv(j / n, vb)]
            else:
                uvs = [(0.05 + 0.9 * j / n, 0.05 + 0.9 * va), (0.05 + 0.9 * (j + 1) / n, 0.05 + 0.9 * va),
                       (0.05 + 0.9 * (j + 1) / n, 0.05 + 0.9 * vb), (0.05 + 0.9 * j / n, 0.05 + 0.9 * vb)]
            mesh.poly_out(mat, [a[j], a[j2], b[j2], b[j]], uvs, inside)
    for k, (ring, other) in enumerate(((rings[0], sections[1]), (rings[-1], sections[-2]))):
        sec = sections[0] if k == 0 else sections[-1]
        cen = (sec[1], sec[2], sec[0])
        behind = (other[1], other[2], other[0])
        cap_mat = front_mat if (k == 0 and front_mat) else mat
        for j in range(len(ring)):
            j2 = (j + 1) % len(ring)
            ps = [cen, ring[j], ring[j2]]
            if cap_mat == "body":
                uvs = [swatch_uv(0.5, 0.5), swatch_uv(0.0, 0.0), swatch_uv(1.0, 0.0)]
            else:
                # the glass seen from the front: u east to west across it, v top to bottom
                uvs = [(0.05 + 0.9 * (sec[1] + sec[3] - p[0]) / (2 * sec[3]),
                        0.05 + 0.9 * (sec[2] + sec[4] - p[1]) / (2 * sec[4])) for p in ps]
            mesh.poly_out(cap_mat, ps, uvs, behind)
    return rings


def cylinder_axis(mesh, base, axis, r, h0, h1, mat, sides=12):
    """A capped cylinder along any unit `axis` from `base`, between heights h0 and h1 along it.
    Only the far end is capped: the near end is always sunk into whatever it stands on."""
    ax = axis
    # two unit vectors perpendicular to the axis
    ref = (1.0, 0.0, 0.0) if abs(ax[0]) < 0.9 else (0.0, 1.0, 0.0)
    u = normal((0, 0, 0), ax, ref)
    v = normal((0, 0, 0), u, ax)

    def ring(h):
        return [tuple(base[k] + ax[k] * h + r * (math.cos(2 * math.pi * j / sides) * u[k]
                                                  + math.sin(2 * math.pi * j / sides) * v[k])
                      for k in range(3)) for j in range(sides)]
    r0, r1 = ring(h0), ring(h1)
    mid = tuple(base[k] + ax[k] * (h0 + h1) / 2 for k in range(3))
    for j in range(sides):
        j2 = (j + 1) % sides
        mesh.poly_out(mat, [r0[j], r0[j2], r1[j2], r1[j]],
                      [swatch_uv(j / sides, 0), swatch_uv((j + 1) / sides, 0),
                       swatch_uv((j + 1) / sides, 1), swatch_uv(j / sides, 1)], mid)
    mesh.poly_out(mat, r1, [swatch_uv(0.5 + 0.5 * math.cos(2 * math.pi * j / sides),
                                      0.5 + 0.5 * math.sin(2 * math.pi * j / sides))
                            for j in range(sides)], mid)


# Alto lot lights (altorll, altorell, altosqll): shoebox area lights that hang off the Alto lot
# light mount (altollm, a hub with four 4 x 4 px arms at y 0..4) or any arm. The JSON: a 6 x 3 px
# body (x 5..11, y 0.8..3.8) from z 3 (the square one from z 6) to 12, a 3 x 3 px neck on to the
# slipfitter, and the olive glass under the body, x 6..10 from z 4 (7) to 11, all in his
# metal_silver with the glass from merc_fixtures. The "Round" lot light's blockstate draws the same
# rectangular model as the Rectangle one, so it is converted as it draws (the unused round JSON
# model is a separate question). The body is a squared shoebox; the neck dips the fraction of a
# pixel under it that it needs to swallow the arm's end (ARM_TIP), as every cobra head's does.
ALTO_LOT_EXPONENT = 5.0
ALTO_LOT_NECK = [
    (11.8, 2.0, 0.0, 3.2),     # buried in the body
    (12.6, 2.2, -0.3, 3.5),
    (REAR_Z, 2.2, -0.3, 3.5),
]
ALTO_NECK_EXPONENT = 3.2


# The Alto Round Lot Light (altorll). Its blockstate used to point at the rectangle model, so in
# game it was a copy of altorell; the round JSON (alto_round_lot_light.json) was never wired up.
# The user chose it round at that JSON's size: a 16-sided disc 10 px across (x 3..13, centred on
# z 7), y -0.25..4.75, a 4 px-radius lens disc under it, y -0.5..-0.25; the block's bounding box
# was widened to match. Its neck is the rectangle's, onto the same altollm mount, started deeper
# (z 10.5) so its front corners stay inside the disc, whose edge is at z 11.6 two px off centre.
ALTO_ROUND_NECK = [
    (10.5, 2.0, 0.0, 3.2),     # buried in the disc
    (12.6, 2.2, -0.3, 3.5),
    (REAR_Z, 2.2, -0.3, 3.5),
]


def build_alto_round_lot():
    m = Mesh()
    check_neck_holds_arm("Alto round lot light", ALTO_ROUND_NECK, ALTO_NECK_EXPONENT)
    cx, cz, sides = 8.0, 7.0, 16
    # the disc: walls and top from cylinder_y, then its underside as a ring round the lens
    cylinder_y(m, cx, cz, 5.0, -0.25, 4.75, "body", sides=sides)
    outer = [(cx + 5.0 * math.cos(2 * math.pi * j / sides), -0.25,
              cz + 5.0 * math.sin(2 * math.pi * j / sides)) for j in range(sides)]
    inner = [(cx + 4.0 * math.cos(2 * math.pi * j / sides), -0.25,
              cz + 4.0 * math.sin(2 * math.pi * j / sides)) for j in range(sides)]
    above = (cx, 1.0, cz)
    for j in range(sides):
        j2 = (j + 1) % sides
        quad = [outer[j], outer[j2], inner[j2], inner[j]]
        m.poly_out("body", quad, [swatch_uv(0, 0), swatch_uv(1, 0), swatch_uv(1, 1),
                                  swatch_uv(0, 1)], above)
    # the lens: a shallow disc whose underside is the glass
    cylinder_y(m, cx, cz, 4.0, -0.5, -0.25, "body", sides=sides, cap_top=False)
    bottom = [(cx + 4.0 * math.cos(2 * math.pi * j / sides), -0.5,
               cz + 4.0 * math.sin(2 * math.pi * j / sides)) for j in range(sides)]
    lens_uv = [(0.05 + 0.9 * (cx + 4.0 - x) / 8.0, 0.05 + 0.9 * (z - cz + 4.0) / 8.0)
               for x, _, z in bottom]
    m.poly_out(LENS_MAT, bottom, lens_uv, above)
    loft_housing(m, ALTO_ROUND_NECK, ALTO_NECK_EXPONENT)
    return m


def alto_lot_body(z0, z1):
    return [
        (z0 + 0.05, 2.5, 0.0, 3.2),
        (z0 + 0.35, 2.92, -0.25, 3.6),
        (z0 + 0.8, 3.0, -0.3, 3.7),
        (z1 - 0.8, 3.0, -0.3, 3.7),
        (z1 - 0.35, 2.92, -0.25, 3.6),
        (z1 - 0.05, 2.5, 0.0, 3.2),
    ]


def build_alto_lot(shape):
    """shape: rect (z 3..12, glass z 4..11) or square (z 6..12, glass z 7..11)."""
    z0, gz0 = (3.0, 4.0) if shape == "rect" else (6.0, 7.0)
    m = Mesh()
    check_neck_holds_arm("Alto lot light", ALTO_LOT_NECK, ALTO_NECK_EXPONENT)
    loft_housing(m, alto_lot_body(z0, 12.4), ALTO_LOT_EXPONENT)
    loft_housing(m, ALTO_LOT_NECK, ALTO_NECK_EXPONENT)
    # the olive glass under the body, its top buried 0.05 in the body's flat bottom
    box(m, (6.0, -0.5, gz0), (10.0, -0.25, 11.0), "body", bottom_mat=LENS_MAT)
    return m


# Alto mercury vapour wall lights (altomvwl, and the narrower altomvwlslim): a wall pack, a back
# box against the wall with the olive glass standing proud of its face. The JSON: the box
# x 5.5..11.5 (slim 6.5..10.5), y 3.8..11.8, z 14..16, the glass x 6..11 (7..10), y 5.2..11.45,
# z 13..14, olive on every face. The back plate is at z = 16 (LIGHTING_SYSTEM.md), drawn at
# REAR_Z so nothing lies on the wall's own face; the boxes' bottoms rise to the bounding box's y 4.
def build_alto_mv_wall(slim):
    hw, gw = (2.0, 1.5) if slim else (3.0, 2.5)
    cx, cy, hh = 8.5, 7.9, 3.9
    gy, gh = 8.325, 3.125
    m = Mesh()
    loft_closed(m, [(14.0, cx, cy, hw - 0.3, hh - 0.3), (14.35, cx, cy, hw, hh), (REAR_Z, cx, cy, hw, hh)],
                5.0, n=24)
    loft_closed(m, [(13.0, cx, gy, gw - 0.2, gh - 0.2), (13.2, cx, gy, gw, gh), (14.1, cx, gy, gw, gh)],
                5.0, mat=LENS_MAT, front_mat=LENS_MAT, n=24)
    return m


# Alto mercury vapour underpass light (altomvul): a ceiling fixture, a shallow round canopy 10 px
# across against the ceiling over an olive glass bowl. The JSON: a hexadecagon 5 px in radius at
# y 15..16, then the bowl in four steps, radius 4 at y 13..15, 3 at 11..13, 2 at 10.2..11 and 1 at
# 9.8..10.3, all about the block's centre. Here the steps are one lathed bowl, a drop refractor in
# his olive; the canopy stops 0.1 short of the ceiling, as the Cree Stylus does.
ALTO_UNDERPASS_BOWL = [(15.1, 4.05), (13.6, 3.9), (12.6, 3.4), (11.6, 2.7), (10.8, 1.9), (10.2, 1.0),
                       (9.85, 0.0)]


def build_alto_mv_underpass():
    m = Mesh()
    # The canopy: its rim, its underside in under the bowl's rim (which is sunk into it, so there
    # is no seam to see through), and its top 0.1 under the ceiling. Every facet is turned half a
    # step, so no seam lies on the block's axes or diagonals.
    lathe(m, 8.0, 8.0, [(15.9, 5.0), (14.9, 5.0), (14.9, 3.8)], "body", phase=0.5)
    angles = [2 * math.pi * (j + 0.5) / 16 for j in range(16)]
    top = [(8.0 + 5.0 * math.cos(a), 15.9, 8.0 + 5.0 * math.sin(a)) for a in angles]
    m.poly_out("body", top, [swatch_uv(0.5 + 0.5 * math.cos(a), 0.5 + 0.5 * math.sin(a)) for a in angles],
               (8.0, 15.0, 8.0))
    lathe(m, 8.0, 8.0, ALTO_UNDERPASS_BOWL, LENS_MAT, phase=0.5)
    return m


# GE Powerbracket: a mercury bracket light, the ballast in a cast arm that climbs from a wall
# plate to a round hood over a drop globe. Two blocks long: the plate is at the back of its own
# block and the hood centred 10 px into the block in front (its bounding box: z -16..18). The
# JSON: a plate x 5..11, y 0..5, z 13..16; the arm a 6 x 3 px bar tilted 22.5 degrees, from inside
# the plate up to a 6 x 3 px housing at y 9.4..12.4, z -11..-4; under that a hood 12 px across
# (y 7.75..10.25) about (8, -10) over a ring of his pale 182 glass, radius 3 down to y 2.75, with
# the olive lamp inside; a photocell on the arm, leaning back with it. Held to the bounding box's
# y 12, so the housing tops out at 11.95 rather than 12.4.
PB_SLOPE = math.tan(math.radians(22.5))
PB_HOOD = [(10.4, 0.0), (10.0, 3.2), (9.2, 5.4), (7.8, 5.95), (7.8, 3.0)]
PB_GLOBE = [(7.8, 3.0), (5.6, 3.0), (3.83, 2.65), (3.2, 2.1), (2.8, 1.15), (2.7, 0.0)]


def pb_arm_y(z):
    """The arm's centreline: 22.5 degrees, as the JSON tilted it."""
    return 2.7 + (14.9 - z) * PB_SLOPE


def build_powerbracket():
    m = Mesh()
    # the wall plate
    loft_closed(m, [(13.0, 8.0, 2.5, 2.7, 2.3), (13.3, 8.0, 2.5, 3.0, 2.5), (REAR_Z, 8.0, 2.5, 3.0, 2.5)], 4.0)
    # the arm, sheared up the slope (vertical sections, so its thickness is 3 px square to it)
    hh = 1.45
    loft_closed(m, [(14.2, 8.0, pb_arm_y(14.2), 2.6, hh), (-3.8, 8.0, pb_arm_y(-3.8), 2.6, hh)], 3.0)
    # the ballast housing over the hood
    loft_closed(m, [(-11.2, 8.0, 10.75, 2.6, 1.0), (-10.8, 8.0, 10.7, 3.0, 1.25),
                    (-3.0, 8.0, 10.7, 3.0, 1.25)], 5.0)
    lathe(m, 8.0, -10.0, PB_HOOD, "body")
    lathe(m, 8.0, -10.0, PB_GLOBE, LENS_MAT)
    # the photocell, square to the arm's top: it leans back toward the plate, as the JSON's did
    pz = 6.0
    a = math.radians(22.5)
    axis = (0.0, math.cos(a), math.sin(a))
    base = (8.0, pb_arm_y(pz) + hh, pz)
    cylinder_axis(m, base, axis, 1.05, -0.5, 0.45, "body")
    cylinder_axis(m, base, axis, 0.95, 0.4, 2.4, "photocell")
    return m


# =========================================================================== new fixtures (#99)
# Fixtures CSM never had as JSON, drawn from the makers' published drawings. They keep the cobra
# heads' conventions (north-facing, slipfitter at z 16, the neck swallowing ARM_TIP) and their
# scale: about 2 in to the pixel, the M-400A's 31.8 in being its 15 px. An LED head is only ~4 in
# tall, 2 px, so its rear is drawn taller than scale where it has to swallow the arm's 3 px end,
# as the GreenCobra GCJ's is.
def nose_round(z0, radius, hw, ends, steps=(0.06, 0.25, 0.5, 0.78)):
    """Sections rounding a nose in plan: the half-width follows a quarter ellipse from the tip
    (z0) to `hw` at z0 + radius. `ends` = ((yb, yt) at the tip, (yb, yt) at full width)."""
    (yb0, yt0), (yb1, yt1) = ends
    out = []
    for t in steps:
        out.append((z0 + t * radius, hw * math.sqrt(1.0 - (1.0 - t) ** 2),
                    yb0 + (yb1 - yb0) * t, yt0 + (yt1 - yt0) * t))
    return out


def stations(z0, length, rows):
    """(fraction of the length from the nose, half-width, bottom y, top y) -> profile rows; the
    rear fraction 1.0 lands on REAR_Z."""
    return [(min(z0 + f * length, REAR_Z), hw, yb, yt) for f, hw, yb, yt in rows]


def pedestal_photocell(m, profile, pz, deck, cell_h=1.7):
    """A photocell on a raised receptacle boss: the LED heads' receptacle stands on a boss at the
    front of the driver hump, so the cell stands clear of the low hood in front of it."""
    base = top_at(profile, pz) - 0.35
    deck = max(deck, base + 0.5)
    cylinder_y(m, 8.0, pz, 1.1, base, deck, "body", sides=12)
    cylinder_y(m, 8.0, pz, 0.95, deck - 0.05, deck + cell_h, "photocell", sides=12)


# Leotek KarbonCobra (2023-), the GreenCobra's successor: a die-cast paddle, round-nosed and
# widest over the LED array in front, pinched at a waist behind it and flaring a little at the
# slipfitter; the hood a low wedge rising to a hump over the driver, the 7-pin receptacle on a boss
# at the front of the hump, a tool-free door under the rear. Leotek's sheets (KC0 / KC1 / KC2):
# 15.9 x 7.6, 18.0 x 9.0 and 24.4 x 10.8 in, all 4.1 in tall; the array 16, 36 and 72 LEDs.
# model -> (length px, head half-width, receptacle at (fraction from the nose), the array:
#           (half-width as a fraction of the head's, from, to as fractions), LED grid (across, along))
KARBON = {
    "kc0": (9.5, 2.3, 0.50, (0.64, 0.10, 0.42), (4, 4)),
    "kc1": (11.0, 2.6, 0.52, (0.68, 0.09, 0.45), (6, 6)),
    "kc2": (14.0, 3.1, 0.58, (0.72, 0.08, 0.48), (6, 12)),
}
KARBON_EXPONENT = 3.0
KARBON_NECK = 2.1  # the rear half-width: the waist, held as narrow as the arm's end allows


def karbon_profile(length, hw):
    z0 = 16.0 - length + 0.05
    rn = 0.2 * length
    return nose_round(z0, rn, hw, ((0.15, 0.55), (0.15, 1.0))) + stations(z0, length, [
        (0.2, hw, 0.15, 1.0),
        (0.36, hw, 0.15, 1.3),
        (0.50, (hw + KARBON_NECK) / 2 + 0.15, 0.15, 1.7),
        (0.64, KARBON_NECK + 0.1, 0.15, 2.4),
        (0.76, KARBON_NECK, 0.05, 3.2),
        (0.86, KARBON_NECK, -0.2, 3.62),
        (1.0, KARBON_NECK + 0.08, -0.30, 3.7),
    ])


def build_karbon(model, shield=False):
    """shield: Leotek's snap-on house side shield, a plate hung along the array's pole side."""
    length, hw, rf, (aw, af0, af1), _ = KARBON[model]
    profile = karbon_profile(length, hw)
    z0 = 16.0 - length
    m = Mesh()
    check_neck_holds_arm("KarbonCobra " + model.upper(), profile, KARBON_EXPONENT)
    loft_housing(m, profile, KARBON_EXPONENT)
    ax = hw * aw
    az0, az1 = z0 + af0 * length, z0 + af1 * length
    box(m, (8.0 - ax, 0.0, az0), (8.0 + ax, 0.25, az1), "body", bottom_mat=LENS_MAT)
    # the door release under the rear, just behind the array
    box(m, (7.3, 0.0, az1 + 0.5), (8.7, 0.2, az1 + 0.9), "body")
    if shield:
        box(m, (8.0 - ax - 0.1, -0.8, az1 + 0.02), (8.0 + ax + 0.1, 0.2, az1 + 0.18), "body")
    pedestal_photocell(m, profile, z0 + rf * length, 2.9, 1.6)
    return m


# AEL AutoConnect ACM (2019-), the medium of the ACC / ACM / ACL LED cobra heads: a teardrop in
# plan, round and widest over the LED array, tapering to the slipfitter, the hood swelling in two
# lobes (over the array, and higher over the driver, where the receptacle sits). AEL's figures:
# 24.6 x 10 x 4.1 in; the flat underside carries the array in front and the door behind it.
ACM_LENGTH, ACM_HW, ACM_EXPONENT = 12.0, 2.6, 2.8


def acm_profile():
    z0, length, hw = 4.05, 11.85, ACM_HW
    return nose_round(z0, 0.22 * length, hw, ((0.2, 0.6), (0.2, 1.6))) + stations(z0, length, [
        (0.22, hw, 0.2, 1.75),
        (0.36, hw, 0.2, 2.3),
        (0.50, 2.5, 0.2, 2.4),
        (0.66, 2.32, 0.2, 3.05),
        (0.82, 2.18, -0.05, 3.75),
        (0.92, 2.15, -0.25, 3.72),
        (1.0, 2.15, -0.30, 3.70),
    ])


def build_acm(top="photocell"):
    profile = acm_profile()
    m = Mesh()
    check_neck_holds_arm("AutoConnect ACM", profile, ACM_EXPONENT)
    loft_housing(m, profile, ACM_EXPONENT)
    box(m, (8.0 - 1.85, 0.0, 4.9), (8.0 + 1.85, 0.25, 9.5), "body", bottom_mat=LENS_MAT)
    receptacle_top(m, profile, 13.75, top, 6.0)
    return m


# GE M-250A (1960s-90s; the M-250A2 the later run): the M-400A's smaller sibling, the same
# Powr/Door cobra head at about 27.5 x 11.5 in to the M-400A's 31.8 x 15.1. CSM's gem240* ids are
# GE's later M-250R / R2 bodies; this is the original, drop refractor or flat cutoff glass. Its
# rear is the M-400A's, the size every cobra neck has to be to swallow the arm.
M250A_PROFILE = [
    # (z, half-width, bottom y, top y)   nose (low z) -> slipfitter (z 16)
    (3.0, 1.1, 0.65, 1.40),
    (3.3, 1.9, 0.32, 1.95),
    (4.0, 2.55, 0.10, 2.55),
    (5.1, 2.95, 0.0, 3.05),
    (6.6, 3.05, 0.0, 3.35),
    (8.3, 3.0, 0.0, 3.50),
    (10.0, 2.9, 0.0, 3.55),
    (11.8, 2.7, -0.05, 3.55),
    (13.4, 2.55, -0.22, 3.52),
    (14.7, 2.47, -0.38, 3.50),
    (REAR_Z, 2.45, -0.45, 3.50),
]


def build_m250a(optics, with_photocell=True):
    m = Mesh()
    check_neck_holds_arm("M-250A", M250A_PROFILE, M400A_EXPONENT)
    loft_housing(m, M250A_PROFILE, M400A_EXPONENT)
    if optics == "cutoff":
        box(m, (5.6, -0.3, 4.6), (10.4, 0.02, 10.2), "body", bottom_mat=LENS_MAT)
    else:
        half_ellipsoid_down(m, (8.0, 0.05, 7.4), 2.45, 2.1, 3.0, LENS_MAT)
    box(m, (7.4, -0.2, 3.8), (8.6, 0.2, 4.5), "body")
    if with_photocell:
        photocell(m, M250A_PROFILE, 12.6, 6.0)
    return m


# Westinghouse OV-25 "Silverliner" (1950s-70s), the first cobra head: a long cast lid over the
# whole fixture, rounded down over a deep round refractor in the front half, and under the rear
# half a narrower wedge (the ballast housing) whose underside climbs from the refractor's rim to
# the slipfitter. The early (1950s) one is squarer, its lid flat on top; the mid-1960s restyle
# has "softer lines". Crouse-Hinds sold the line after Cooper bought it in 1982.
def ov25_lid(early):
    if early:
        rows = [(1.0, 1.9, 0.75, 1.85), (1.35, 2.7, 0.62, 2.35), (2.0, 3.1, 0.6, 2.55),
                (3.0, 3.2, 0.6, 2.6), (9.0, 3.1, 0.6, 2.6)]
    else:
        rows = [(1.0, 1.4, 0.75, 1.55), (1.4, 2.3, 0.62, 2.15), (2.2, 2.95, 0.6, 2.6),
                (3.6, 3.2, 0.6, 2.85), (5.5, 3.25, 0.6, 2.95), (7.5, 3.2, 0.6, 2.95),
                (9.5, 3.0, 0.6, 2.95)]
    return rows + [(11.5, 2.8, 0.6, 3.0), (13.2, 2.6, 0.5, 3.15), (14.6, 2.48, -0.25, 3.45),
                   (REAR_Z, 2.45, -0.45, 3.5)]


OV25_WEDGE = [
    (8.5, 2.35, -1.25, 1.0),
    (10.5, 2.4, -1.05, 1.0),
    (12.5, 2.3, -0.75, 1.0),
    (14.6, 2.2, -0.35, 1.0),
]


def build_ov25(early=False):
    lid = ov25_lid(early)
    ex = 4.0 if early else 2.6
    m = Mesh()
    check_neck_holds_arm("OV-25", lid, ex)
    loft_housing(m, lid, ex)
    loft_housing(m, OV25_WEDGE, 3.2)
    half_ellipsoid_down(m, (8.0, 0.62, 5.0), 2.85, 2.5, 3.7, LENS_MAT)
    photocell(m, lid, 11.6, 6.0)
    return m


# Westinghouse L-250 (1970s-80s; Crouse-Hinds after 1982): short, a rounded lid domed over a deep
# refractor in front, the ballast in a squarer housing behind it tapering to the slipfitter.
L250_LID = [
    (4.0, 1.4, 0.75, 1.60),
    (4.4, 2.3, 0.60, 2.40),
    (5.2, 2.9, 0.55, 2.90),
    (6.6, 3.1, 0.55, 3.15),
    (8.2, 3.1, 0.55, 3.20),
    (9.6, 2.95, 0.55, 3.10),
    (10.6, 2.6, 0.60, 2.80),
]
L250_REAR = [
    (9.4, 2.5, -0.20, 2.60),
    (10.2, 2.7, -0.45, 3.10),
    (12.5, 2.65, -0.50, 3.30),
    (14.6, 2.5, -0.45, 3.40),
    (REAR_Z, 2.45, -0.45, 3.40),
]


def build_l250():
    m = Mesh()
    check_neck_holds_arm("L-250", L250_REAR, 3.6)
    loft_housing(m, L250_LID, 2.2)
    loft_housing(m, L250_REAR, 3.6)
    half_ellipsoid_down(m, (8.0, 0.6, 7.3), 2.7, 2.4, 3.0, LENS_MAT)
    for z in (11.2, 11.9, 12.6):
        box(m, (6.3, top_at(L250_REAR, z) - 0.4, z - 0.15), (9.7, top_at(L250_REAR, z) + 0.3, z + 0.15), "body")
    photocell(m, L250_REAR, 13.9, 6.0)
    return m


# SolarMax SMX-LL5 (2nd generation SolarMax, confirmed by AngelWingsPanda): a thin round-nosed
# paddle carrying the LED module under its front half, its top running flat into a deep, square
# driver box behind, the photocell receptacle at the front edge of the box. SolarMax's sheet:
# 19.4 x 12.4 x 4.8 in; drawn at the first-generation smsmx's breadth (8 px), its look refined.
LL5_PADDLE = nose_round(2.55, 2.6, 4.0, ((1.8, 2.55), (1.8, 3.05))) + [
    (5.15, 4.0, 1.8, 3.05),
    (11.0, 4.0, 1.8, 3.05),
]
LL5_BOX = [
    (8.4, 2.6, 0.0, 2.9),
    (8.7, 3.0, -0.3, 3.15),
    (15.0, 3.0, -0.35, 3.15),
    (REAR_Z, 2.9, -0.35, 3.10),
]


def build_smx_ll5():
    m = Mesh()
    check_neck_holds_arm("SMX-LL5", LL5_BOX, 5.0)
    loft_housing(m, LL5_PADDLE, 3.0)
    loft_housing(m, LL5_BOX, 5.0)
    # the LED module: a raised block under the paddle, its underside the array
    box(m, (5.4, 1.25, 3.7), (10.6, 1.85, 8.5), "body", bottom_mat=LENS_MAT)
    photocell(m, LL5_BOX, 9.9, 5.3)
    return m


# GE Evolve EALS area light (2015-): a square die-cast head carrying a square LED array, five
# cooling fins across its top rising toward the rear, on a tapering neck with the receptacle.
# GE's sheet: 25.2 in long, 14.7 wide at the head (16.0 over the fins), 1.9 in thick at the
# nose, 4.3 over the fins. Four mounts: C1 an integral slipfitter (z 16, any CSM arm), D1 a short
# arm to a pole plate, V1 a knuckle on a wall plate, S1 a knuckle slipfitter on a pole-top tenon.
EALS_HEAD, EALS_NECK = 7.5, 5.4   # px


def eals_body(m, z0, dy, neck_rear):
    """The head from z0 and the neck behind it, raised by dy; `neck_rear` = (hw, yb, yt) at the
    rear. Returns the neck profile (for the photocell)."""
    zh = z0 + EALS_HEAD
    head = nose_round(z0, 0.9, 3.8, ((0.1, 0.8), (0.1, 1.05))) + [(z0 + 0.9, 3.8, 0.1, 1.05),
                                                                  (zh, 3.8, 0.1, 1.1)]
    hw, yb, yt = neck_rear
    zr = zh + EALS_NECK - 0.1 if z0 + EALS_HEAD + EALS_NECK < REAR_Z else REAR_Z
    neck = [(zh - 0.3, 3.5, 0.1, 1.5), (zh + 0.4, 3.2, 0.08, 1.9),
            (zh + 0.55 * (zr - zh), (3.2 + hw) / 2, (0.08 + yb) / 2, (1.9 + yt) / 2 + 0.2),
            (zr - 0.6, hw + 0.05, yb, yt), (zr, hw, yb, yt)]
    head = [(z, w, b + dy, t + dy) for z, w, b, t in head]
    neck = [(z, w, b + dy, t + dy) for z, w, b, t in neck]
    loft_housing(m, head, 4.0)
    loft_housing(m, neck, 3.2)
    for k, fz in enumerate((1.0, 2.25, 3.5, 4.75, 6.0)):
        top = 1.45 + 0.1 * k + dy
        box(m, (4.75, 0.8 + dy, z0 + fz), (11.25, top, z0 + fz + 0.5), "body")
    box(m, (4.6, dy, z0 + 0.65), (11.4, 0.15 + dy, zh - 0.5), "body", bottom_mat=LENS_MAT)
    return neck


def build_eals(mount):
    m = Mesh()
    if mount == "slip":
        neck = eals_body(m, 16.0 - EALS_HEAD - EALS_NECK, 0.0, (2.1, -0.3, 3.7))
        check_neck_holds_arm("EALS", neck, 3.2)
        photocell(m, neck, 12.6, 6.0)
    elif mount == "arm":
        # D1: a short flat arm under the neck's end to a pole plate on the block face
        neck = eals_body(m, 1.6, 0.0, (1.7, 0.05, 2.2))
        box(m, (7.25, 0.35, neck[-1][0] - 0.5), (8.75, 1.45, 15.5), "body")
        box(m, (6.9, -0.9, 15.45), (9.1, 2.6, REAR_Z), "body")
        photocell(m, neck, 12.2, 6.0)
    elif mount == "wall":
        # V1: a knuckle hub on a bracket off a square wall plate
        neck = eals_body(m, 0.8, 0.0, (1.7, 0.05, 2.2))
        zr = neck[-1][0]
        box(m, (7.4, 0.55, zr - 0.5), (8.6, 1.75, 13.9), "body")
        for sx in (1.0, -1.0):
            cylinder_axis(m, (8.0, 1.15, 14.0), (sx, 0.0, 0.0), 0.8, 0.0, 1.05, "body")
        box(m, (7.4, 0.6, 14.3), (8.6, 1.7, 15.45), "body")
        box(m, (6.6, -0.3, 15.4), (9.4, 2.6, REAR_Z), "body")
        photocell(m, neck, 11.4, 6.0)
    else:
        # S1 on a pole-top tenon: a sleeve on the block's centre, a knuckle on it and the fixture
        # cantilevered north from the knuckle, level (the knuckle aims 0-45; CSM draws it at 0)
        cylinder_y(m, 8.0, 8.0, 1.05, 0.0, 2.5, "body", sides=12)
        for sx in (1.0, -1.0):
            cylinder_axis(m, (8.0, 2.95, 8.0), (sx, 0.0, 0.0), 0.75, 0.0, 0.95, "body")
        z0 = 7.6 - EALS_HEAD - EALS_NECK
        neck = eals_body(m, z0, 2.9, (1.6, 0.05, 1.6))
        box(m, (7.4, 2.55, neck[-1][0] - 0.4), (8.6, 3.35, 7.9), "body")
        photocell(m, neck, neck[-1][0] - 2.0, 7.0)
    return m


# Holophane Mongoose LED (2014-): a long, flat roadway and area head, its big individual LED
# optics in a grid under it, a crenellated rim of cooling teeth along both top edges, the driver
# in a deeper housing over the slipfitter. Holophane's figures: 40.8 x 17.7 x 6.6 in, dark bronze,
# for 30-50 ft mounting. At CSM's scale it is longer than a block: the head runs on to z -2.
MONGOOSE_HEAD = nose_round(-2.0, 1.2, 4.0, ((0.45, 1.3), (0.4, 1.6))) + [(-0.8, 4.0, 0.4, 1.6),
                                                                       (10.0, 4.0, 0.4, 1.6)]
MONGOOSE_REAR = [
    (8.6, 3.4, 0.2, 2.0),
    (9.6, 3.6, -0.1, 3.0),
    (12.5, 3.3, -0.35, 3.6),
    (14.8, 2.7, -0.35, 3.7),
    (REAR_Z, 2.6, -0.35, 3.7),
]


def build_mongoose():
    m = Mesh()
    check_neck_holds_arm("Mongoose", MONGOOSE_REAR, 3.2)
    loft_housing(m, MONGOOSE_HEAD, 4.5)
    loft_housing(m, MONGOOSE_REAR, 3.2)
    z = -0.6
    while z < 8.0:
        for x0 in (4.25, 11.05):
            box(m, (x0, 1.15, z), (x0 + 0.7, 2.35, z + 1.0), "body")
        z += 1.7
    box(m, (4.55, 0.25, -1.1), (11.45, 0.45, 8.4), "body", bottom_mat=LENS_MAT)
    photocell(m, MONGOOSE_REAR, 12.4, 6.5)
    return m


# registry name -> (display name, builder, body texture, bounding box comment)
CATALOGUE = [
    ("gem400a", "GE M-400A", lambda: build_m400a("refractor"),
     "streetlight_body_ge_grey"),
    ("gem400acu", "GE M-400A (Cutoff Optics)", lambda: build_m400a("cutoff"),
     "streetlight_body_ge_grey"),
    ("gem400acunp", "GE M-400A (Cutoff Optics, No Photocell)",
     lambda: build_m400a("cutoff", photocell=False), "streetlight_body_ge_grey"),
    ("gem400acugreen", "GE M-400A (Cutoff Optics, Anaheim Green)", lambda: build_m400a("cutoff"),
     "streetlight_body_anaheim_green"),
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
    ("ltec", "Leotek E-Cobra", lambda: build_leotek("ec"), "streetlight_body_leotek_grey"),
    ("ltecdtd", "Leotek E-Cobra (Dusk to Dawn)", lambda: build_leotek("ecdtd"), "streetlight_body_leotek_grey"),
    ("ltgc1v1", "Leotek GreenCobra GC1 (Version 1)", lambda: build_leotek("gc1"),
     "streetlight_body_leotek_grey"),
    ("ltgc1v1np", "Leotek GreenCobra GC1 (Version 1, No Photocell)", lambda: build_leotek("gc1", "cap"),
     "streetlight_body_leotek_grey"),
    ("ltgc1v2", "Leotek GreenCobra GC1 (Version 2)", lambda: build_leotek("gc1"),
     "streetlight_body_leotek_grey"),
    ("ltgcj", "Leotek GreenCobra GCJ", lambda: build_leotek("gcj"), "streetlight_body_leotek_grey"),
    ("ltgcjsn", "Leotek GreenCobra GCJ (Smart Node)", lambda: build_leotek("gcj", "node"),
     "streetlight_body_leotek_grey"),
    ("ltgcl", "Leotek GreenCobra GCL", lambda: build_leotek("gcl"), "streetlight_body_leotek_grey"),
    ("ltgcm", "Leotek GreenCobra GCM", lambda: build_leotek("gcm"), "streetlight_body_leotek_grey"),
    ("ltgcmsn", "Leotek GreenCobra GCM (Smart Node)", lambda: build_leotek("gcm", "node"),
     "streetlight_body_leotek_grey"),
    ("ltgcmv2", "Leotek GreenCobra GCM (Version 2)", lambda: build_leotek("gcm"),
     "streetlight_body_leotek_grey"),
    ("ltgcmv2np", "Leotek GreenCobra GCM (Version 2, No Photocell)", lambda: build_leotek("gcm", "cap"),
     "streetlight_body_leotek_grey"),
    ("geel", "GE Evolve ESR2 (Large)", lambda: build_evolve("large"), "streetlight_body_ge_evolve_grey"),
    ("geelpo", "GE Evolve ESR2 (Large, Photocell Outlet)", lambda: build_evolve("large", "outlet"),
     "streetlight_body_ge_evolve_grey"),
    ("geelsn", "GE Evolve ESR2 (Large, Smart Node)", lambda: build_evolve("large", "node"),
     "streetlight_body_ge_evolve_grey"),
    ("gees", "GE Evolve ESR2 (Small)", lambda: build_evolve("small"), "streetlight_body_ge_evolve_grey"),
    ("geespo", "GE Evolve ESR2 (Small, Photocell Outlet)", lambda: build_evolve("small", "outlet"),
     "streetlight_body_ge_evolve_grey"),
    ("geesn", "GE Evolve ESR2 (Small, Smart Node)", lambda: build_evolve("small", "node"),
     "streetlight_body_ge_evolve_grey"),
    ("creeledway", "Cree LEDway", lambda: build_ledway(), "streetlight_body_led_grey"),
    ("creeledwaysmall", "Cree LEDway (Small)", lambda: build_ledway(small=True), "streetlight_body_led_grey"),
    ("creexsp", "Cree XSP", build_xsp, "streetlight_body_led_grey"),
    ("creestylusfull", "Cree Stylus (Full)", lambda: build_stylus(True), "streetlight_body_stylus_grey"),
    ("creestylushalf", "Cree Stylus (Half)", lambda: build_stylus(False), "streetlight_body_stylus_grey"),
    ("cinavion", "Cooper Industries Navion", lambda: build_navion(2), "streetlight_body_led_grey"),
    ("cinavionalt", "Cooper Industries Navion (Alternate)", lambda: build_navion(3), "streetlight_body_led_grey"),
    ("smsmx", "Solarmax SMX", build_smx, "streetlight_body_solarmax"),
    ("altorll", "Alto Round Lot Light", build_alto_round_lot, "streetlight_body_alto_silver"),
    ("altorell", "Alto Rectangle Lot Light", lambda: build_alto_lot("rect"), "streetlight_body_alto_silver"),
    ("altosqll", "Alto Square Lot Light", lambda: build_alto_lot("square"), "streetlight_body_alto_silver"),
    ("altomvul", "Alto Mercury Vapor Underpass Light", build_alto_mv_underpass, "streetlight_body_merc_grey"),
    ("altomvwl", "Alto Mercury Vapor Wall Light", lambda: build_alto_mv_wall(False), "streetlight_body_merc_grey"),
    ("altomvwlslim", "Alto Slim Mercury Vapor Wall Light", lambda: build_alto_mv_wall(True),
     "streetlight_body_merc_grey"),
    ("gepb", "GE Powerbracket", build_powerbracket, "streetlight_body_merc_grey"),
    # New fixtures (#99), from the makers' drawings.
    ("ltkc0", "Leotek KarbonCobra KC0", lambda: build_karbon("kc0"), "streetlight_body_leotek_karbon"),
    ("ltkc0hss", "Leotek KarbonCobra KC0 (House Side Shield)", lambda: build_karbon("kc0", shield=True),
     "streetlight_body_leotek_karbon"),
    ("ltkc1", "Leotek KarbonCobra KC1", lambda: build_karbon("kc1"), "streetlight_body_leotek_karbon"),
    ("ltkc1hss", "Leotek KarbonCobra KC1 (House Side Shield)", lambda: build_karbon("kc1", shield=True),
     "streetlight_body_leotek_karbon"),
    ("ltkc2", "Leotek KarbonCobra KC2", lambda: build_karbon("kc2"), "streetlight_body_leotek_karbon"),
    ("ltkc2hss", "Leotek KarbonCobra KC2 (House Side Shield)", lambda: build_karbon("kc2", shield=True),
     "streetlight_body_leotek_karbon"),
    ("aeacm", "American Electric AutoConnect ACM", lambda: build_acm(), "streetlight_body_ael_grey"),
    ("aeacmnp", "American Electric AutoConnect ACM (No Photocell)", lambda: build_acm("cap"),
     "streetlight_body_ael_grey"),
    ("gem250a", "GE M-250A (Standard)", lambda: build_m250a("refractor"), "streetlight_body_ge_grey"),
    ("gem250acu", "GE M-250A (Cutoff Optics)", lambda: build_m250a("cutoff"), "streetlight_body_ge_grey"),
    ("gem250acunp", "GE M-250A (Cutoff Optics, No Photocell)", lambda: build_m250a("cutoff", with_photocell=False),
     "streetlight_body_ge_grey"),
    ("whov25", "Westinghouse OV-25 Silverliner (Mercury Vapor)", lambda: build_ov25(),
     "streetlight_body_westinghouse_silver"),
    ("whov25hps", "Westinghouse OV-25 Silverliner (HPS)", lambda: build_ov25(),
     "streetlight_body_westinghouse_silver"),
    ("whov25early", "Westinghouse OV-25 Silverliner (1950s, Mercury Vapor)", lambda: build_ov25(early=True),
     "streetlight_body_westinghouse_silver"),
    ("whl250", "Westinghouse L-250 (Mercury Vapor)", build_l250, "streetlight_body_ge_grey"),
    ("whl250hps", "Westinghouse L-250 (HPS)", build_l250, "streetlight_body_ge_grey"),
    ("smsmxll5", "SolarMax SMX-LL5", build_smx_ll5, "streetlight_body_solarmax_white"),
    ("geeals", "GE Evolve EALS Area Light (Slipfitter)", lambda: build_eals("slip"), "streetlight_body_black"),
    ("geealsarm", "GE Evolve EALS Area Light (Arm Mount)", lambda: build_eals("arm"), "streetlight_body_black"),
    ("geealswall", "GE Evolve EALS Area Light (Wall Knuckle)", lambda: build_eals("wall"),
     "streetlight_body_black"),
    ("geealspt", "GE Evolve EALS Area Light (Pole Top Knuckle)", lambda: build_eals("post"),
     "streetlight_body_black"),
    ("hpmongoose", "Holophane Mongoose LED", build_mongoose, "streetlight_body_dark_bronze"),
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
    # The 8 px wide Leoteks (x 4..12); the E-Cobra's box also starts at z 0. The neck dips below
    # y 0 for the arm, as the Autobahns' does.
    "ltec": ((4.0, 12.0), (-0.5, 5.0), (0.0, REAR_Z + 1e-9)),
    "ltecdtd": ((4.0, 12.0), (-0.5, 5.0), (5.0, REAR_Z + 1e-9)),
    "ltgc1v1": ((4.0, 12.0), (-0.5, 7.0), (1.0, REAR_Z + 1e-9)),
    "ltgc1v1np": ((4.0, 12.0), (-0.5, 5.45), (1.0, REAR_Z + 1e-9)),
    "ltgc1v2": ((4.0, 12.0), (-0.5, 7.0), (1.0, REAR_Z + 1e-9)),
    "ltgcl": ((4.0, 12.0), (-0.5, 5.0), (4.0, REAR_Z + 1e-9)),
    # The 8 px wide Cree heads (x 4..12); the LEDway's box starts at z 0, the XSP's photocell box
    # tops out at y 5.
    "creeledway": ((4.0, 12.0), (-0.5, 8.0), (0.0, REAR_Z + 1e-9)),
    "creeledwaysmall": ((4.0, 12.0), (-0.5, 7.0), (5.0, REAR_Z + 1e-9)),
    "creexsp": ((4.0, 12.0), (-0.5, 5.0), (2.0, REAR_Z + 1e-9)),
    # The Stylus hangs from the ceiling at the top of the block; the Full runs on to z -16.
    "creestylusfull": ((5.0, 11.0), (11.0, 16.0), (-16.0, 16.0)),
    "creestylushalf": ((5.0, 11.0), (11.0, 16.0), (0.0, 16.0)),
    # The Navions start at z 0 (z -4 with three squares).
    "cinavion": ((4.9, 11.1), (-0.5, 6.0), (0.0, REAR_Z + 1e-9)),
    "cinavionalt": ((4.9, 11.1), (-0.5, 6.0), (-4.0, REAR_Z + 1e-9)),
    # The SMX's head is 10 px wide (x 3..13) and hangs to y -1; his panel a fraction under it.
    "smsmx": ((3.0, 13.0), (-1.2, 5.0), (1.0, REAR_Z + 1e-9)),
    # The Alto lot lights' boxes (x 5..11, y 0..4, from z 3 / 6); the neck and the glass dip under
    # y 0 as the cobra heads' necks do.
    "altorll": ((3.0, 13.0), (-0.5, 4.75), (2.0, REAR_Z + 1e-9)),
    "altorell": ((5.0, 11.0), (-0.5, 4.0), (3.0, REAR_Z + 1e-9)),
    "altosqll": ((5.0, 11.0), (-0.5, 4.0), (6.0, REAR_Z + 1e-9)),
    # The underpass light hangs from the ceiling: x and z 3..13, y 9.8..16.
    "altomvul": ((3.0, 13.0), (9.8, 16.0), (3.0, 13.0)),
    # The wall lights stand off the wall at z 16: their boxes run z 13..16, y 4..12.
    "altomvwl": ((5.5, 12.0), (4.0, 12.0), (13.0, REAR_Z + 1e-9)),
    "altomvwlslim": ((6.5, 10.5), (4.0, 12.0), (13.0, REAR_Z + 1e-9)),
    # The Powerbracket is two blocks long: its box runs z -16..18, x 2..14, y 0..12.
    "gepb": ((2.0, 14.0), (0.0, 12.0), (-16.0, REAR_Z + 1e-9)),
    # The SMX-LL5's paddle is 8 px wide (x 4..12), as the first-generation SMX's head is broad.
    "smsmxll5": ((4.0, 12.0), (-0.5, 5.5), (2.0, REAR_Z + 1e-9)),
    # The EALS head is 7.6 px wide (x 4.2..11.8); the arm and wall mounts move it forward, the
    # pole top one sits on its knuckle over the block's centre and runs on north to z -5.3.
    "geeals": ((4.0, 12.0), (-0.5, 6.0), (3.0, REAR_Z + 1e-9)),
    "geealsarm": ((4.0, 12.0), (-1.0, 6.0), (1.5, REAR_Z + 1e-9)),
    "geealswall": ((4.0, 12.0), (-0.5, 6.0), (0.5, REAR_Z + 1e-9)),
    "geealspt": ((4.0, 12.0), (0.0, 7.0), (-5.5, 9.1)),
    # The Mongoose is longer than a block: its head runs on to z -2.
    "hpmongoose": ((4.0, 12.0), (-0.5, 6.5), (-2.0, REAR_Z + 1e-9)),
}


# Lens art cut from the requester's JSON fixture atlases (D5, hybrid): source crops in
# dev-env-utils/scripts/artwork/streetlight/, written as streetlight_lens_<art>_on/_off.
ARTWORK_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "artwork", "streetlight")
# ael_atb0/2 are the Autobahns' LED arrays (the dies white lit, dimmed dark on a pale optic unlit).
# leotek_* are the E-Cobra and GreenCobra LED arrays, cut from the ltec / ltgc* sheets. The GCJ and
# GCJ SN sheets carry pixel-identical arrays, as do the GCM and GCM SN, so each pair shares a cut.
LENS_ART = ["ge_m400", "ge_m240", "ael_atb0", "ael_atb2",
            "leotek_ec", "leotek_ecdtd", "leotek_gc1v1", "leotek_gc1v2", "leotek_gcj", "leotek_gcl",
            "leotek_gcm", "leotek_gcmv2",
            "ge_evolve_large", "ge_evolve_small", "cree_ledway", "cree_ledway_small", "cree_xsp",
            "cree_stylus", "ci_navion", "ci_navion_alt", "solarmax_smx"]
# The cuts are stored as they lie on the atlas. Where the JSON face mapped them onto the fixture
# differently from art_lens_png's convention (the source's u from the slipfitter to the nose, its v
# from east to west), they are turned first, so the array lands as the JSON drew it: a down face
# with "rotation": 270 runs u nose -> slipfitter and v west -> east (half a turn), the GCL's also
# mirrors u (a flip top to bottom), the GCJ's "rotation": 90 already matches.
ART_TURN = {
    "leotek_ec": Image.ROTATE_180, "leotek_ecdtd": Image.ROTATE_180,
    "leotek_gc1v1": Image.ROTATE_180, "leotek_gc1v2": Image.ROTATE_180,
    "leotek_gcl": Image.FLIP_TOP_BOTTOM,
    "leotek_gcm": Image.ROTATE_180, "leotek_gcmv2": Image.ROTATE_180,
    # the XSP's two modules lie side by side across the head, where the sheet has them along u
    "cree_xsp": Image.ROTATE_90,
}


def art_lens(art, state):
    return "streetlight_lens_%s_%s" % (art, state)


# The new fixtures' LED arrays, drawn rather than cut (their JSON never existed): art name ->
# (LEDs across, LEDs along, style). "die" is a chip-on-board square die on a pale optic (the
# requester's array style), "amber" the same with the SMX-LL5's yellow phosphor showing unlit,
# "lens" a grid of round optics, one per LED (the area lights).
GENERATED_LENS = {
    "leotek_kc0": (4, 4, "die"),
    "leotek_kc1": (6, 6, "die"),
    "leotek_kc2": (6, 12, "die"),
    "ael_acm": (5, 9, "amber"),
    "solarmax_smx_ll5": (4, 9, "amber"),
    "ge_eals": (6, 6, "lens"),
    "holophane_mongoose": (3, 6, "lens"),
}


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
    "ltec": (art_lens("leotek_ec", "on"), art_lens("leotek_ec", "off")),
    "ltecdtd": (art_lens("leotek_ecdtd", "on"), art_lens("leotek_ecdtd", "off")),
    "ltgc1v1": (art_lens("leotek_gc1v1", "on"), art_lens("leotek_gc1v1", "off")),
    "ltgc1v1np": (art_lens("leotek_gc1v1", "on"), art_lens("leotek_gc1v1", "off")),
    "ltgc1v2": (art_lens("leotek_gc1v2", "on"), art_lens("leotek_gc1v2", "off")),
    "ltgcj": (art_lens("leotek_gcj", "on"), art_lens("leotek_gcj", "off")),
    "ltgcjsn": (art_lens("leotek_gcj", "on"), art_lens("leotek_gcj", "off")),
    "ltgcl": (art_lens("leotek_gcl", "on"), art_lens("leotek_gcl", "off")),
    "ltgcm": (art_lens("leotek_gcm", "on"), art_lens("leotek_gcm", "off")),
    "ltgcmsn": (art_lens("leotek_gcm", "on"), art_lens("leotek_gcm", "off")),
    "ltgcmv2": (art_lens("leotek_gcmv2", "on"), art_lens("leotek_gcmv2", "off")),
    "ltgcmv2np": (art_lens("leotek_gcmv2", "on"), art_lens("leotek_gcmv2", "off")),
    "geel": (art_lens("ge_evolve_large", "on"), art_lens("ge_evolve_large", "off")),
    "geelpo": (art_lens("ge_evolve_large", "on"), art_lens("ge_evolve_large", "off")),
    "geelsn": (art_lens("ge_evolve_large", "on"), art_lens("ge_evolve_large", "off")),
    "gees": (art_lens("ge_evolve_small", "on"), art_lens("ge_evolve_small", "off")),
    "geespo": (art_lens("ge_evolve_small", "on"), art_lens("ge_evolve_small", "off")),
    "geesn": (art_lens("ge_evolve_small", "on"), art_lens("ge_evolve_small", "off")),
    "creeledway": (art_lens("cree_ledway", "on"), art_lens("cree_ledway", "off")),
    "creeledwaysmall": (art_lens("cree_ledway_small", "on"), art_lens("cree_ledway_small", "off")),
    "creexsp": (art_lens("cree_xsp", "on"), art_lens("cree_xsp", "off")),
    "creestylusfull": (art_lens("cree_stylus", "on"), art_lens("cree_stylus", "off")),
    "creestylushalf": (art_lens("cree_stylus", "on"), art_lens("cree_stylus", "off")),
    "cinavion": (art_lens("ci_navion", "on"), art_lens("ci_navion", "off")),
    "cinavionalt": (art_lens("ci_navion_alt", "on"), art_lens("ci_navion_alt", "off")),
    "smsmx": (art_lens("solarmax_smx", "on"), art_lens("solarmax_smx", "off")),
    "altorll": (LENS_MERC_GLASS_ON, LENS_MERC_GLASS_OFF),
    "altorell": (LENS_MERC_GLASS_ON, LENS_MERC_GLASS_OFF),
    "altosqll": (LENS_MERC_GLASS_ON, LENS_MERC_GLASS_OFF),
    "altomvul": (LENS_REFRACTOR_MERC_ON, LENS_REFRACTOR_OFF),
    "altomvwl": (LENS_MERC_GLASS_ON, LENS_MERC_GLASS_OFF),
    "altomvwlslim": (LENS_MERC_GLASS_ON, LENS_MERC_GLASS_OFF),
    "gepb": (LENS_POWERBRACKET_ON, LENS_POWERBRACKET_OFF),
    "ltkc0": (art_lens("leotek_kc0", "on"), art_lens("leotek_kc0", "off")),
    "ltkc0hss": (art_lens("leotek_kc0", "on"), art_lens("leotek_kc0", "off")),
    "ltkc1": (art_lens("leotek_kc1", "on"), art_lens("leotek_kc1", "off")),
    "ltkc1hss": (art_lens("leotek_kc1", "on"), art_lens("leotek_kc1", "off")),
    "ltkc2": (art_lens("leotek_kc2", "on"), art_lens("leotek_kc2", "off")),
    "ltkc2hss": (art_lens("leotek_kc2", "on"), art_lens("leotek_kc2", "off")),
    "aeacm": (art_lens("ael_acm", "on"), art_lens("ael_acm", "off")),
    "aeacmnp": (art_lens("ael_acm", "on"), art_lens("ael_acm", "off")),
    "gem250a": (LENS_REFRACTOR_ON, LENS_REFRACTOR_OFF),
    # the M-250 family's cutoff glass: the framed panel the M240R2 cutoff wears
    "gem250acu": (art_lens("ge_m240", "on"), art_lens("ge_m240", "off")),
    "gem250acunp": (art_lens("ge_m240", "on"), art_lens("ge_m240", "off")),
    # mercury: his olive, as the OV-20 / OV-50 wear it; HPS: his refractor orange
    "whov25": (LENS_REFRACTOR_MERC_ON, LENS_REFRACTOR_OFF),
    "whov25hps": (LENS_REFRACTOR_ON, LENS_REFRACTOR_OFF),
    "whov25early": (LENS_REFRACTOR_MERC_ON, LENS_REFRACTOR_OFF),
    "whl250": (LENS_REFRACTOR_MERC_ON, LENS_REFRACTOR_OFF),
    "whl250hps": (LENS_REFRACTOR_ON, LENS_REFRACTOR_OFF),
    "smsmxll5": (art_lens("solarmax_smx_ll5", "on"), art_lens("solarmax_smx_ll5", "off")),
    "geeals": (art_lens("ge_eals", "on"), art_lens("ge_eals", "off")),
    "geealsarm": (art_lens("ge_eals", "on"), art_lens("ge_eals", "off")),
    "geealswall": (art_lens("ge_eals", "on"), art_lens("ge_eals", "off")),
    "geealspt": (art_lens("ge_eals", "on"), art_lens("ge_eals", "off")),
    "hpmongoose": (art_lens("holophane_mongoose", "on"), art_lens("holophane_mongoose", "off")),
}

# The photocell material's texture, where a fixture carries something else in the receptacle.
PHOTOCELL_FOR = {
    "ltgcjsn": "streetlight_smartnode",
    "ltgcmsn": "streetlight_smartnode",
    # the SMX-LL5 in his photo carries a blue photocell, the smart node's blue
    "smsmxll5": "streetlight_smartnode",
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


def scaled(colour, top):
    """`colour` with its hue kept and its brightest channel raised (or lowered) to `top`."""
    return tuple(int(round(c * float(top) / max(colour))) for c in colour)


def merc_glass_png(lit):
    """His olive as flat glass: lit, at lamp brightness, hotter at the middle; unlit, at half."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 7.5, (y - 7.5) / 7.5
            d = min(1.0, math.sqrt(dx * dx + dy * dy))
            if lit:
                hot, edge = scaled(MERC_OLIVE, 250), scaled(MERC_OLIVE, 218)
            else:
                hot, edge = scaled(MERC_OLIVE, 72), scaled(MERC_OLIVE, 62)
            img.putpixel((x, y), tuple(int(round(hot[k] + (edge[k] - hot[k]) * d)) for k in range(3)) + (255,))
    return img


def powerbracket_globe_png(lit):
    """Down the lathe, top to bottom: his pale globe glass in prism bands (rows 0..11), then his
    olive lamp showing through the bottom of the globe (rows 12..15)."""
    if lit:
        glass, glass_rib = scaled(MERC_GLOBE, 212), scaled(MERC_GLOBE, 236)
        lamp, lamp_rib = scaled(MERC_OLIVE, 232), scaled(MERC_OLIVE, 250)
    else:
        glass, glass_rib = scaled(MERC_GLOBE, 118), scaled(MERC_GLOBE, 132)
        lamp, lamp_rib = scaled(MERC_OLIVE, 68), scaled(MERC_OLIVE, 76)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        if y < 12:
            col = glass_rib if y in (2, 5, 8, 11) else glass
        else:
            col = lamp_rib if y == 14 else lamp
        for x in range(16):
            img.putpixel((x, y), col + (255,))
    return img


def art_lens_png(art, state):
    """A lens crop from the requester's atlas, made a sprite: turned so the lamp lies along the
    fixture (the box lens runs v along z), scaled square (a taller-than-wide sprite is read as
    an animation strip) to 32 px. Drawn unshaded now, the unlit art is halved, so it looks as it
    did shaded; the lit art is left as painted, and so reads lit for the first time."""
    src = Image.open(os.path.join(ARTWORK_DIR, "%s_lens_%s.png" % (art, state))).convert("RGBA")
    if art in ART_TURN:
        src = src.transpose(ART_TURN[art])
    img = src.transpose(Image.ROTATE_90).resize((32, 32), Image.NEAREST)
    if state == "off":
        img = Image.eval(img, lambda c: c)  # copy
        px = img.load()
        for y in range(32):
            for x in range(32):
                r, g, b, a = px[x, y]
                px[x, y] = (r // 2, g // 2, b // 2, a)
    return img


def led_array_png(across, along, style, state):
    """A drawn LED array, 32 px, u across the fixture and v along it (as the box lens maps it): a
    frame round a pale optic and an even grid of LEDs (each cell a whole number of pixels, so the
    grid does not wander). Lit, the LEDs are near white on the optic; unlit the whole is painted
    at about half (the art lenses' rule) with the LEDs dark, or the amber phosphor where the style
    shows it. A "lens" LED is a rounded optic, a dark rim round its bright centre."""
    lit = state == "on"
    frame = (140, 140, 142)
    optic = (196, 196, 194)
    if lit:
        led = (255, 250, 232) if style == "amber" else (255, 255, 252)
    else:
        frame = tuple(c // 2 for c in frame)
        optic = tuple(c // 2 for c in optic)
        led = (140, 100, 36) if style == "amber" else (36, 36, 38)
    rim = tuple(int(c * 0.62) for c in optic)
    img = Image.new("RGBA", (32, 32), frame + (255,))
    px = img.load()
    for y in range(1, 31):
        for x in range(1, 31):
            px[x, y] = optic + (255,)
    cw, ch = 30 // across, 30 // along
    ox, oy = (32 - cw * across) // 2, (32 - ch * along) // 2
    for i in range(across):
        for j in range(along):
            if style == "lens":
                n = max(3, min(cw, ch) - 1)
                x0, y0 = ox + i * cw + (cw - n) // 2, oy + j * ch + (ch - n) // 2
                for y in range(n):
                    for x in range(n):
                        edge = x in (0, n - 1) or y in (0, n - 1)
                        if edge and (x in (0, n - 1) and y in (0, n - 1)) and n > 3:
                            continue  # round the optic's corners
                        px[x0 + x, y0 + y] = (rim if edge else led) + (255,)
            else:
                n = max(1, min(cw, ch) // 2)
                x0, y0 = ox + i * cw + (cw - n) // 2, oy + j * ch + (ch - n) // 2
                for y in range(n):
                    for x in range(n):
                        px[x0 + x, y0 + y] = led + (255,)
    return img


def all_textures():
    out = {name: swatch_png(*grad) for name, grad in SWATCHES.items()}
    for art, (across, along, style) in GENERATED_LENS.items():
        for state in ("on", "off"):
            out[art_lens(art, state)] = led_array_png(across, along, style, state)
    out[LENS_ON] = lens_png(True)
    out[LENS_OFF] = lens_png(False)
    out[LENS_REFRACTOR_OFF] = refractor_off_png()
    out[LENS_REFRACTOR_ON] = refractor_on_png()
    # his olive at lamp brightness: the hue kept, the brightest channel raised to 232
    out[LENS_REFRACTOR_MERC_ON] = refractor_on_png(
        tuple(int(round(c * 232.0 / max(MERC_OLIVE))) for c in MERC_OLIVE))
    out[LENS_MERC_GLASS_ON] = merc_glass_png(True)
    out[LENS_MERC_GLASS_OFF] = merc_glass_png(False)
    out[LENS_POWERBRACKET_ON] = powerbracket_globe_png(True)
    out[LENS_POWERBRACKET_OFF] = powerbracket_globe_png(False)
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

# A converted fixture whose JSON model carried a different `display` keeps it.
TRANSFORM_FOR = {
    # As alto_mv_underpass_light.json had it (deleted): the fixture is at the top of its block, so it was lowered.
    "altomvul": dict(INVENTORY_TRANSFORM,
                     gui=_display([0, 90, -5], [0.25, -3.5, 0], 0.625),
                     ground=_display([0, 0, 0], [0, -4.25, 0], 1.0),
                     thirdperson_righthand=_display([0, 90, 0], [0, -2.5, 0], 0.55),
                     thirdperson_lefthand=_display([0, -90, 0], [0, -2.75, 0], 0.55)),
    # As ge_powerbracket.json had it (deleted): two blocks long, so smaller in the slot and moved along.
    "gepb": dict(INVENTORY_TRANSFORM, gui=_display([0, 90, -5], [4, 1, 0], 0.5)),
}


def blockstate(registry, body_tex, lens_on=LENS_ON, lens_off=LENS_OFF, photocell_tex="streetlight_photocell"):
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
                         "#photocell": TEX + photocell_tex},
        },
        "variants": {
            "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}},
            "state": {"0": unlit, "1": lit, "2": unlit, "3": lit},
            "inventory": [{"transform": TRANSFORM_FOR.get(registry, INVENTORY_TRANSFORM)}],
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
            json.dumps(blockstate(registry, body, *LENS_FOR.get(registry, (LENS_ON, LENS_OFF)),
                                  photocell_tex=PHOTOCELL_FOR.get(registry, "streetlight_photocell")),
                       indent=2) + "\n").encode()
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
