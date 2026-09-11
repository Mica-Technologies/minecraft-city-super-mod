#!/usr/bin/env python3
"""
gen_guardrail_ends.py -- the four W-beam guardrail END TREATMENT blocks.

An end treatment is the block in the cell PAST the last rail of a run. It is not a rail variant:
each style is completely different geometry, two of them reach outside a single cell, and none of
them has a connect, slope or post property -- an end treatment is one fixed shape, eight-way
rotatable and nothing else.

    guardrail_end_flared          the classic flared shoe, curving back out of the roadway and
                                  rounding over into a blunt closed nose
    guardrail_end_boxing_glove    the impact head: a squared-off box carrying a chevron panel
    guardrail_end_terminal        the long energy-absorbing terminal, rail running out onto a
                                  sloped steel ramp with a small striped head at the tip
    guardrail_end_turndown        the rail ramped down to a ground anchor

WHERE THE NUMBERS COME FROM
---------------------------
Every dimension that the end treatments share with the rails lives in ``guardrail_geometry`` and
is imported, never restated. ``END_RAIL_TOP_Y`` / ``END_RAIL_BOTTOM_Y`` / ``END_RAIL_FRONT_Z`` /
``END_RAIL_BACK_Z`` and ``W_PROFILE`` are what make an end shoe meet the run it terminates. An end
whose rail is two units off the run builds green, loads clean, and is only wrong from one angle in
game, so there is deliberately no second copy of those numbers here.

The rail's sheet thickness is DERIVED rather than chosen: the W profile's deepest point is 1.60
back from the face, and the rail's declared back is ``END_RAIL_BACK_Z``, so the sheet is exactly
thick enough for the swept solid to fill the depth the geometry file declares.

THE FRAME
---------
The same frame the rails use, because that is the only way the two meet: the run goes along X and
spans the cell exactly, and the rail's face points toward LOW Z, which is the traffic side. So an
end treatment joins the run at x = 0 and develops toward +x -- the run's RIGHT-hand end, the
direction ``WorkZoneJoins`` already calls right -- and its chevron panel faces low z, which is
what ``GLOVE_PANEL_Z`` describes.

CHIRALITY -- READ THIS BEFORE PLACING ONE AT BOTH ENDS OF A RUN
---------------------------------------------------------------
Both ends of a run carry the SAME facing, because facing is the way the rail LOOKS and the rail
looks the same way along the whole run. An end treatment is chiral: rotating this model 180
degrees turns its rail round to z 12.0-15.2, which is not where the run's rail is. So these four
blocks fit the RIGHT-hand end of a run. The left-hand end needs a mirrored sibling block (or a
boolean property that picks a mirrored model) -- that is a Java and blockstate decision, not one
this script can take on its own, and it is flagged rather than guessed at.

TEXTURES
--------
Two sprites for four blocks. Both carry the same flat swatch column down the right-hand edge, so
any face on any of the four can ask for galvanised, dark, shaded galvanised or chevron yellow by
sampling a swatch, and the two ends that need stripes read them out of the panel area on the left.
``flip-v`` is on: ``draw_swatches`` from the work zone generator draws the swatch column at the
flipped rows, which is why it is called rather than reimplemented.

Usage:
    python gen_guardrail_ends.py                 # writes into the repo tree
    python gen_guardrail_ends.py --scratch       # writes into _guardrail_out/ instead
    python gen_guardrail_ends.py --only guardrail_end_turndown

Requires Pillow.
"""
import argparse
import json
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout                       # noqa: E402
import guardrail_geometry as geom                 # noqa: E402

# The shared primitives. Imported rather than copied: two copies of ``prism`` would drift, and the
# swatch/flip-v contract lives in exactly one place because getting it wrong is silent.
from gen_work_zone_devices import (                # noqa: E402
    Mesh, box, prism, uv_swatch, draw_swatches, check_uvs, mesh_bounds, fit_in_cell,
    facing_variants, diagonal_stripe_image, java_bbox, vadd, vcross, vnorm,
    SWATCH_U0, SWATCH_BASE_V, SWATCH_DARK_V, SWATCH_BAND_V, TEX_SIZE, STRIPE_TEX_SIZE,
)

TRAFFICACCESSORIES_OWNER = layout.owner_of_folder("trafficaccessories")
MODEL_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER,
                                       "models/block/" + geom.MODEL_SUBDIR)
TEXTURE_DIR = layout.asset_dir_for_write(
    TRAFFICACCESSORIES_OWNER, "textures/" + geom.TEXTURE_PREFIX.split(":", 1)[1])
BLOCKSTATE_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER, "blockstates")
SCRATCH_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_guardrail_out")

GENERATOR = os.path.basename(__file__)

# --- derived from the shared geometry ------------------------------------------------------------
#: How deep the W profile is pressed, front face to deepest lip.
W_DEPTH = max(o for (o, _y) in geom.W_PROFILE)
#: Sheet thickness. Chosen for us: front face + profile depth + sheet = the declared rail back.
RAIL_THICK = (geom.END_RAIL_BACK_Z - geom.END_RAIL_FRONT_Z) - W_DEPTH
#: Rail height, from the shared constants.
RAIL_H = geom.END_RAIL_TOP_Y - geom.END_RAIL_BOTTOM_Y

CELL = geom.CELL

# --- shapes ---------------------------------------------------------------------------------------
# Flared shoe. It flares back this far and drops this far across the cell; the nose closes over the
# last stretch. Kept short of x = 16 so the whole shoe lives inside its own cell.
FLARED_LEN = 13.10
FLARED_BACK = 7.00
FLARED_DROP = 1.90
FLARED_NOSE_AT = 0.80          # where along the sweep the nose starts rounding over
FLARED_NOSE_FLATTEN = 0.66     # how far the corrugation presses out at the tip
FLARED_NOSE_HSCALE = 0.46      # how far the section closes down at the tip
FLARED_POSTS = (0.24, 0.60)    # fractions along the sweep carrying a post

# Impact head. The panel is the head's own front face, so the head starts at GLOVE_PANEL_Z.
GLOVE_X0, GLOVE_X1, GLOVE_Y0, GLOVE_Y1 = geom.GLOVE_PANEL
GLOVE_DEPTH = 7.20
GLOVE_Z0 = geom.GLOVE_PANEL_Z
GLOVE_Z1 = GLOVE_Z0 + GLOVE_DEPTH
GLOVE_STUB = GLOVE_X0          # the rail stops at the head's face; see GLOVE_PANEL's comment
GLOVE_LEG_HALF = 0.95
GLOVE_LEGS = (4.60, 11.40)
GLOVE_LEG_Z = (2.60, 6.90)

# Energy-absorbing terminal. This one legitimately reaches beyond its own cell along the run, the
# way the arrow board and the signal trailers do; its bounding box is clamped back to the cell so
# it cannot steal a click from the block next door.
TERM_LEN = 30.00
TERM_BACK = 2.80
# Where the rail's bottom edge comes to rest at the ramp. Fixed, with the drop derived from it, so
# raising the run (#192) steepens the run-out instead of lifting it off the ramp it lands on.
TERM_END_BOTTOM_Y = 2.80
TERM_DROP = geom.END_RAIL_BOTTOM_Y - TERM_END_BOTTOM_Y
TERM_DROP_POW = 2.20
TERM_POSTS = (0.10, 0.30)
TERM_RAMP_X = (12.00, 32.00)
TERM_RAMP_Y = (5.20, 0.90)     # top surface, at each end of the ramp
TERM_RAMP_THICK = 0.80
TERM_RAMP_Z = (0.40, 7.60)
TERM_HEAD_X = (29.00, 33.60)
TERM_HEAD_Y = (0.30, 7.40)
TERM_HEAD_Z = (1.80, 8.20)

# Turndown. The rail ramps to the ground over the cell and is bolted to an anchor there.
TURN_LEN = 12.00
TURN_END_Y = 0.35
TURN_POW = 2.20
TURN_POST_AT = 0.20
TURN_ANCHOR = (10.60, 15.40, 0.00, 1.25, 0.30, 4.70)

# Posts. Same three parts as a run's post -- rail, spacer BLOCK-OUT, then the post itself -- taken
# from the shared constants and offset by however far the rail has flared at that station. Leaving
# the block-out out is the difference between a guardrail and a plank on a stick, and it is just as
# plainly visible from behind on an end treatment as it is on a run.
POST_PROUD = geom.POST_TOP_Y - geom.RAIL_TOP_Y
#: The post starts this far inside the block-out, so the two overlap instead of meeting on a shared
#: plane that neither of them will ever show.
POST_BITE = 0.40

TEXTURE_STEEL = "guardrail_end_steel"
TEXTURE_CHEVRON = "guardrail_end_chevron"

#: The chevron sheet is drawn for the impact head's panel, and every other striped face on the
#: four models samples it in the SAME world units, so a stripe stays at GLOVE_STRIPE_DEG wherever
#: it lands. Banding in sprite space instead makes the angle a function of the face's aspect.
CHEVRON_W = GLOVE_X1 - GLOVE_X0
CHEVRON_H = GLOVE_Y1 - GLOVE_Y0
CHEVRON_STRIPE = 3.20


# --- small vector helper ---------------------------------------------------------------------------
def vmul(a, s):
    """Scale a vector. The work zone module has add/sub/cross/norm but no scale."""
    return (a[0] * s, a[1] * s, a[2] * s)


def mirror_mesh(mesh):
    """Reflect a finished mesh about x = 8 -- the run axis -- for the run's other end.

    A REFLECTION, not a rotation about Y. Reflecting in x reverses the run direction while leaving
    the rail's face where it was, at z 0.80-4.00, which is exactly what the far end of a run needs:
    both ends of a run carry the same facing, because facing is the way the rail LOOKS. Turning the
    model 180 degrees instead would flip z as well and put the rail on the far side of the cell,
    nowhere near the run it is supposed to terminate.

    The catch is that a reflection reverses handedness. ``Mesh`` resolved every triangle's winding
    against its own normals when it was added, so reflecting the positions alone leaves the whole
    model inside-out, culling from the side you are looking at -- which the audit reports as
    ``winding``. Negating the normals' x and reversing every face's vertex order carries the two
    round together.

    ``transform_mesh`` in the work zone generator deliberately refuses this job, and says so.
    """
    half = CELL * 0.5
    mesh.v[:] = [(2.0 * half - x, y, z) for (x, y, z) in mesh.v]
    mesh.vn[:] = [(-nx, ny, nz) for (nx, ny, nz) in mesh.vn]
    mesh.f[:] = [list(reversed(tri)) for tri in mesh.f]


#: How much of a sweep leaves the run dead straight before it starts to curve.
#:
#: Not cosmetic. The station frames come from the path's own tangent, so a path that starts
#: curving at t = 0 gives the FIRST section a tilted frame, and the rail's joint face then leans
#: out of the cell and out of line with the run it is supposed to continue. A straight lead-in is
#: also what the real hardware does: the rail leaves the last post square before it flares.
LEAD = 0.12


def curve(t):
    """The curve parameter: zero across the straight lead-in, 0-1 across the rest."""
    return 0.0 if t <= LEAD else (t - LEAD) / (1.0 - LEAD)


# --- the rail as a swept solid ---------------------------------------------------------------------
def w_section(flatten=0.0, hscale=1.0):
    """The rail's front skin as (offset back from the face, height above the rail's bottom).

    ``flatten`` presses the corrugation out toward the mean offset, and ``hscale`` closes the
    section down about its own middle. Together they are what turns the open W of a running rail
    into the blunt closed nose of an end shoe, without a second profile having to be written down
    anywhere.
    """
    offs = [o for (o, _y) in geom.W_PROFILE]
    mid_o = 0.5 * (min(offs) + max(offs))
    for (o, y) in geom.W_PROFILE:
        h = y - geom.END_RAIL_BOTTOM_Y
        yield (o + (mid_o - o) * flatten,
               0.5 * RAIL_H + (h - 0.5 * RAIL_H) * hscale)


def section_normals(section):
    """Outward normal at each section vertex, in the section's own (offset, height) frame.

    The W profile's height is strictly increasing, so the skin is a graph and its outward side is
    unambiguously the low-offset one -- which is the front, the traffic side.
    """
    seg = []
    for i in range(len(section) - 1):
        do = section[i + 1][0] - section[i][0]
        dh = section[i + 1][1] - section[i][1]
        length = math.hypot(do, dh)
        seg.append((-dh / length, do / length))
    out = []
    for i in range(len(section)):
        a = seg[max(i - 1, 0)]
        b = seg[min(i, len(seg) - 1)]
        n = (a[0] + b[0], a[1] + b[1])
        length = math.hypot(*n) or 1.0
        out.append((n[0] / length, n[1] / length))
    return out


def rail_stations(path, section_of, samples):
    """Sample a rail sweep into frames.

    ``path(t)`` gives the point on the rail's FACE at the bottom of the section. ``u`` is the
    horizontal direction the profile presses back into, ``w`` the section's up -- taken from the
    path's own tangent, so a rail that descends tilts its section with it instead of shearing.
    """
    ts = [i / float(samples - 1) for i in range(samples)]
    pts = [path(t) for t in ts]
    stations = []
    for i, t in enumerate(ts):
        nxt = pts[min(i + 1, len(pts) - 1)]
        prv = pts[max(i - 1, 0)]
        travel = vnorm((nxt[0] - prv[0], nxt[1] - prv[1], nxt[2] - prv[2]))
        flat = vnorm((travel[0], 0.0, travel[2]))
        u = (-flat[2], 0.0, flat[0])
        w = vnorm(vcross(u, travel))
        stations.append({"origin": pts[i], "u": u, "w": w, "travel": travel,
                         "section": list(section_of(t))})
    return stations


def sweep_rail(mesh, stations, swatch_v=SWATCH_BASE_V, caps=(True, True)):
    """Sweep the rail's closed cross-section along a run of frames.

    The section is a band: a front skin and the same skin offset back by the sheet thickness. That
    is why the end caps can be built as a strip of quads between the two skins rather than by
    triangulating a very non-convex W -- a fan across that shape puts triangles outside the
    polygon, which is a coplanar overlap and an audit failure.
    """
    t = uv_swatch(swatch_v)
    rings = []
    for st in stations:
        origin, u, w, section = st["origin"], st["u"], st["w"], st["section"]
        front = [vadd(origin, vadd(vmul(u, o), vmul(w, h))) for (o, h) in section]
        back = [vadd(origin, vadd(vmul(u, o + RAIL_THICK), vmul(w, h))) for (o, h) in section]
        norms = section_normals(section)
        fn = [vnorm(vadd(vmul(u, no), vmul(w, nh))) for (no, nh) in norms]
        bn = [vmul(n, -1.0) for n in fn]
        rings.append({"front": front, "back": back, "fn": fn, "bn": bn,
                      "w": w, "travel": st["travel"]})

    n_pts = len(rings[0]["front"])
    for k in range(len(rings) - 1):
        a, b = rings[k], rings[k + 1]
        for i in range(n_pts - 1):
            mesh.quad((a["front"][i], t, a["fn"][i]), (a["front"][i + 1], t, a["fn"][i + 1]),
                      (b["front"][i + 1], t, b["fn"][i + 1]), (b["front"][i], t, b["fn"][i]))
            mesh.quad((a["back"][i], t, a["bn"][i]), (b["back"][i], t, b["bn"][i]),
                      (b["back"][i + 1], t, b["bn"][i + 1]), (a["back"][i + 1], t, a["bn"][i + 1]))
        for (index, sign) in ((0, -1.0), (n_pts - 1, 1.0)):
            na, nb = vmul(a["w"], sign), vmul(b["w"], sign)
            mesh.quad((a["front"][index], t, na), (a["back"][index], t, na),
                      (b["back"][index], t, nb), (b["front"][index], t, nb))

    for (ring, want, sign) in ((rings[0], caps[0], -1.0), (rings[-1], caps[1], 1.0)):
        if not want:
            continue
        n = vmul(ring["travel"], sign)
        for i in range(n_pts - 1):
            mesh.quad((ring["front"][i], t, n), (ring["back"][i], t, n),
                      (ring["back"][i + 1], t, n), (ring["front"][i + 1], t, n))


def rail_post(mesh, origin, swatch_v=SWATCH_BASE_V):
    """Block-out and post behind the rail at one point on its path, standing on the floor.

    ``origin`` is the point on the rail's face, so everything here is placed relative to it and
    follows the rail wherever it has flared or dropped to. The z values come from the shared
    constants; the only local number is how far the post bites into the block-out.

    Nothing here can come out coplanar with the rail: no two consecutive ``W_PROFILE`` points share
    an offset, so the swept skins have no face of constant z anywhere for an axis-aligned box to
    fight with.
    """
    x, y, z = origin
    dz = z - geom.RAIL_FRONT_Z
    dy = y - geom.RAIL_BOTTOM_Y
    box(mesh, x - geom.BLOCKOUT_HALF_X, x + geom.BLOCKOUT_HALF_X,
        geom.BLOCKOUT_Y0 + dy, geom.BLOCKOUT_Y1 + dy,
        geom.BLOCKOUT_Z0 + dz, geom.BLOCKOUT_Z1 + dz, swatch_v)
    box(mesh, x - geom.POST_HALF_X, x + geom.POST_HALF_X,
        geom.POST_BOTTOM_Y, geom.RAIL_TOP_Y + POST_PROUD + dy,
        geom.POST_Z0 + dz - POST_BITE, geom.POST_Z1 + dz, swatch_v)


def panel_uv(x_local, y_local):
    """UV into the chevron sheet, in the sheet's own world units.

    ``diagonal_stripe_image`` draws column c at world x = c / (size * SWATCH_U0) * world_w and row
    r at world y = (1 - r / size) * world_h; with flip-v a v of ``y / world_h`` samples that row.
    So a face wearing the sheet asks for its own world position, and the stripes stay at the angle
    they were drawn at whatever the face's aspect.
    """
    return ((x_local / CHEVRON_W) * SWATCH_U0, y_local / CHEVRON_H)


def striped_quad(mesh, pts, normal, extents):
    """A flat rectangle wearing the chevron sheet.

    ``extents`` gives each corner's place ON THE SHEET, in the sheet's world units and in the same
    order as ``pts`` -- so a face that is not the panel's own size still gets the stripe pitch the
    sheet was drawn at rather than a stretched one.
    """
    uvs = [panel_uv(a, b) for (a, b) in extents]
    mesh.quad_out(list(pts), normal, uvs)


# --- the four models --------------------------------------------------------------------------------
def flared_path(t):
    s = curve(t)
    return (FLARED_LEN * t,
            geom.END_RAIL_BOTTOM_Y - FLARED_DROP * s ** 3,
            geom.END_RAIL_FRONT_Z + FLARED_BACK * s * s)


def flared_section(t):
    if t <= FLARED_NOSE_AT:
        return w_section()
    k = (t - FLARED_NOSE_AT) / (1.0 - FLARED_NOSE_AT)
    k = k * k * (3.0 - 2.0 * k)
    return w_section(flatten=FLARED_NOSE_FLATTEN * k,
                     hscale=1.0 + (FLARED_NOSE_HSCALE - 1.0) * k)


def build_flared(mesh):
    stations = rail_stations(flared_path, flared_section, 22)
    sweep_rail(mesh, stations)
    for at in FLARED_POSTS:
        rail_post(mesh, flared_path(at))


def build_boxing_glove(mesh):
    def path(t):
        return (GLOVE_STUB * t, geom.END_RAIL_BOTTOM_Y, geom.END_RAIL_FRONT_Z)

    sweep_rail(mesh, rail_stations(path, lambda _t: w_section(), 3))

    # The head. Its front face IS the chevron panel, and its nose carries the same stripes at the
    # same world pitch, so the two read as one sheet wrapped round the corner.
    box(mesh, GLOVE_X0, GLOVE_X1, GLOVE_Y0, GLOVE_Y1, GLOVE_Z0, GLOVE_Z1, SWATCH_BASE_V,
        faces=("x-", "y-", "y+", "z+"))
    striped_quad(mesh,
                 [(GLOVE_X0, GLOVE_Y0, GLOVE_Z0), (GLOVE_X1, GLOVE_Y0, GLOVE_Z0),
                  (GLOVE_X1, GLOVE_Y1, GLOVE_Z0), (GLOVE_X0, GLOVE_Y1, GLOVE_Z0)],
                 (0, 0, -1),
                 [(0.0, 0.0), (CHEVRON_W, 0.0), (CHEVRON_W, CHEVRON_H), (0.0, CHEVRON_H)])
    striped_quad(mesh,
                 [(GLOVE_X1, GLOVE_Y0, GLOVE_Z0), (GLOVE_X1, GLOVE_Y0, GLOVE_Z1),
                  (GLOVE_X1, GLOVE_Y1, GLOVE_Z1), (GLOVE_X1, GLOVE_Y1, GLOVE_Z0)],
                 (1, 0, 0),
                 [(0.0, 0.0), (GLOVE_DEPTH, 0.0), (GLOVE_DEPTH, CHEVRON_H), (0.0, CHEVRON_H)])

    # Skids. Their tops sit inside the head so nothing shares a plane with its underside.
    for cx in GLOVE_LEGS:
        box(mesh, cx - GLOVE_LEG_HALF, cx + GLOVE_LEG_HALF, 0.0, GLOVE_Y0 + 1.10,
            GLOVE_LEG_Z[0], GLOVE_LEG_Z[1], SWATCH_DARK_V)


def terminal_path(t):
    s = curve(t)
    return (TERM_LEN * t,
            geom.END_RAIL_BOTTOM_Y - TERM_DROP * s ** TERM_DROP_POW,
            geom.END_RAIL_FRONT_Z + TERM_BACK * s * s)


def build_terminal(mesh):
    stations = rail_stations(terminal_path, lambda _t: w_section(), 20)
    sweep_rail(mesh, stations)
    for at in TERM_POSTS:
        rail_post(mesh, terminal_path(at))

    # The ramp the rail runs out onto: one leaning slab, which is exactly what ``prism`` is for.
    (rx0, rx1), (ry0, ry1), (rz0, rz1) = TERM_RAMP_X, TERM_RAMP_Y, TERM_RAMP_Z
    top = [(rx0, ry0, rz0), (rx1, ry1, rz0), (rx1, ry1, rz1), (rx0, ry0, rz1)]
    bottom = [(x, y - TERM_RAMP_THICK, z) for (x, y, z) in top]
    prism(mesh, bottom, top, SWATCH_BAND_V)

    # The head at the tip. Small, and striped on the two faces traffic can see it from.
    (hx0, hx1), (hy0, hy1), (hz0, hz1) = TERM_HEAD_X, TERM_HEAD_Y, TERM_HEAD_Z
    box(mesh, hx0, hx1, hy0, hy1, hz0, hz1, SWATCH_BASE_V, faces=("x-", "y-", "y+", "z+"))
    striped_quad(mesh,
                 [(hx0, hy0, hz0), (hx1, hy0, hz0), (hx1, hy1, hz0), (hx0, hy1, hz0)],
                 (0, 0, -1),
                 [(0.0, 0.0), (hx1 - hx0, 0.0), (hx1 - hx0, hy1 - hy0), (0.0, hy1 - hy0)])
    striped_quad(mesh,
                 [(hx1, hy0, hz0), (hx1, hy0, hz1), (hx1, hy1, hz1), (hx1, hy1, hz0)],
                 (1, 0, 0),
                 [(0.0, 0.0), (hz1 - hz0, 0.0), (hz1 - hz0, hy1 - hy0), (0.0, hy1 - hy0)])


def build_terminal_inventory(mesh):
    build_terminal(mesh)
    fit_in_cell(mesh)


def turndown_path(t):
    s = curve(t)
    return (TURN_LEN * t,
            geom.END_RAIL_BOTTOM_Y - (geom.END_RAIL_BOTTOM_Y - TURN_END_Y) * s ** TURN_POW,
            geom.END_RAIL_FRONT_Z)


def build_turndown(mesh):
    stations = rail_stations(turndown_path, lambda _t: w_section(), 18)
    sweep_rail(mesh, stations)
    rail_post(mesh, turndown_path(TURN_POST_AT))
    box(mesh, *TURN_ANCHOR, SWATCH_DARK_V)


# --- textures ---------------------------------------------------------------------------------------
def steel_texture():
    """Flat galvanised, with the swatch column every face of every model samples."""
    img = Image.new("RGBA", (TEX_SIZE, TEX_SIZE), geom.GALVANISED + (255,))
    draw_swatches(img, geom.GALVANISED, geom.GALVANISED_DARK, geom.CHEVRON_YELLOW)
    return img


def chevron_texture():
    """The impact head's sheet: 45-degree chevron stripes, plus the SAME swatch column.

    ``diagonal_stripe_image`` paints its own swatches for the work zone's palette on the way out;
    they are painted over here so a galvanised face on this sheet is the same galvanised as on the
    plain one, and every model can name one swatch v and get the colour it expects.
    """
    img = diagonal_stripe_image(True, CHEVRON_W, CHEVRON_H, stripe_world=CHEVRON_STRIPE,
                                base=geom.CHEVRON_BLACK, stripe=geom.CHEVRON_YELLOW,
                                size=STRIPE_TEX_SIZE)
    draw_swatches(img, geom.GALVANISED, geom.GALVANISED_DARK, geom.CHEVRON_YELLOW)
    return img


# --- catalogue ----------------------------------------------------------------------------------------
ENDS = {
    "guardrail_end_flared": {
        "model": "guardrail_end_flared", "build": build_flared,
        "texture": TEXTURE_STEEL, "texture_fn": steel_texture,
        "display": "W-Beam Guardrail End (Flared)",
    },
    "guardrail_end_boxing_glove": {
        "model": "guardrail_end_boxing_glove", "build": build_boxing_glove,
        "texture": TEXTURE_CHEVRON, "texture_fn": chevron_texture,
        "display": "W-Beam Guardrail End (Impact Head)",
    },
    "guardrail_end_terminal": {
        "model": "guardrail_end_terminal", "build": build_terminal,
        "inventory_model": "guardrail_end_terminal_inv",
        "inventory_build": build_terminal_inventory,
        "texture": TEXTURE_CHEVRON, "texture_fn": chevron_texture,
        "display": "W-Beam Guardrail End (Terminal)",
    },
    "guardrail_end_turndown": {
        "model": "guardrail_end_turndown", "build": build_turndown,
        "texture": TEXTURE_STEEL, "texture_fn": steel_texture,
        "display": "W-Beam Guardrail End (Turndown)",
    },
}

for _spec in ENDS.values():
    # Eight-way, from the shared rotation table. A device drawn at a facing the renderer turns by a
    # different amount than the baked model is the bug the shared table exists to prevent.
    _spec["diagonal"] = True


# --- writers -------------------------------------------------------------------------------------------
def _restamp(path):
    """Point a file the shared writers produced at the generator that actually produced it."""
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    with open(path, "w", newline="\n") as fh:
        fh.write(text.replace("gen_work_zone_devices.py", GENERATOR))


def write_obj(mesh, path, name, mtl_file):
    mesh.write(path, name, mtl_file)
    _restamp(path)


def write_mtl(path, texture_name):
    with open(path, "w", newline="\n") as fh:
        fh.write("# Procedurally generated by dev-env-utils/scripts/%s -- do not hand edit\n"
                 % GENERATOR)
        fh.write("# One material; the blockstate retextures #%s.\n" % geom.MATERIAL)
        fh.write("newmtl %s\n" % geom.MATERIAL)
        fh.write("map_Kd %s/%s\n" % (geom.TEXTURE_PREFIX, texture_name))


def mirrored_name(spec):
    return spec["model"] + "_mirrored"


def blockstate_json(spec):
    """The Forge blockstate for one end treatment.

    Property-map dialect, and deliberately thin: ``facing`` from the shared eight-way table, plus
    ``mirrored``, and nothing else. An end treatment has no connect, slope or post state -- it is
    one fixed shape, which is the whole reason it is its own block rather than a rail variant.

    ``mirrored`` is not something the player sets. The block works out which side of the run it is
    on and reports it from ``getActualState``, and the true branch swaps in the reflected model.
    The inventory keeps the unmirrored icon: which end of a run the item will land on is not known
    while it is still in a slot.
    """
    variants = {
        "facing": facing_variants(spec),
        "mirrored": {
            "false": {},
            "true": {"model": "csm:%s/%s.obj" % (geom.MODEL_SUBDIR, mirrored_name(spec))},
        },
        "normal": [{}],
    }
    inventory = {"transform": "forge:default-block"}
    if spec.get("inventory_model"):
        inventory["model"] = "csm:%s/%s.obj" % (geom.MODEL_SUBDIR, spec["inventory_model"])
    variants["inventory"] = [inventory]
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "csm:%s/%s.obj" % (geom.MODEL_SUBDIR, spec["model"]),
            "custom": {"flip-v": True},
            "textures": {"#%s" % geom.MATERIAL: "%s/%s" % (geom.TEXTURE_PREFIX, spec["texture"])},
        },
        "variants": variants,
    }


def cell_bounds(bounds):
    """Mesh bounds clipped to this block's own cell.

    The terminal reaches two cells along the run on purpose. Its bounding box must not: an AABB
    bigger than the block steals the player's clicks from whatever is next door, and there is
    nothing in the log to say so.
    """
    lo = tuple(max(0.0, v) for v in bounds[:3])
    hi = tuple(min(CELL, v) for v in bounds[3:])
    return lo + hi


def generate(model_dir, texture_dir, blockstate_dir, fragment_dir, only=None):
    for d in (model_dir, texture_dir, blockstate_dir, fragment_dir):
        os.makedirs(d, exist_ok=True)
    written = []
    bounds = {}

    for registry, spec in ENDS.items():
        if only and registry not in only:
            continue

        mesh = Mesh()
        spec["build"](mesh)
        check_uvs(mesh, spec["model"])
        raw = mesh_bounds(mesh)
        bounds[registry] = cell_bounds(raw)
        obj_path = os.path.join(model_dir, spec["model"] + ".obj")
        write_obj(mesh, obj_path, spec["model"], spec["model"] + ".mtl")
        written.append(obj_path)
        mtl_path = os.path.join(model_dir, spec["model"] + ".mtl")
        write_mtl(mtl_path, spec["texture"])
        written.append(mtl_path)

        # The far end of the run. Reflected from the same build, so the two ends of a run can never
        # be two different shapes -- and sharing the MTL means they can never be two textures.
        flipped = Mesh()
        spec["build"](flipped)
        mirror_mesh(flipped)
        check_uvs(flipped, mirrored_name(spec))
        mirror_path = os.path.join(model_dir, mirrored_name(spec) + ".obj")
        write_obj(flipped, mirror_path, mirrored_name(spec), spec["model"] + ".mtl")
        written.append(mirror_path)

        inv_model = spec.get("inventory_model")
        if inv_model:
            inv_mesh = Mesh()
            spec["inventory_build"](inv_mesh)
            check_uvs(inv_mesh, inv_model)
            inv_path = os.path.join(model_dir, inv_model + ".obj")
            write_obj(inv_mesh, inv_path, inv_model, spec["model"] + ".mtl")
            written.append(inv_path)

        tex_path = os.path.join(texture_dir, spec["texture"] + ".png")
        if tex_path not in written:
            spec["texture_fn"]().save(tex_path)
            written.append(tex_path)

        bs_path = os.path.join(blockstate_dir, registry + ".json")
        with open(bs_path, "w", newline="\n") as fh:
            json.dump(blockstate_json(spec), fh, indent=2)
            fh.write("\n")
        written.append(bs_path)

    lang_path = os.path.join(fragment_dir, "ends_lang.txt")
    with open(lang_path, "w", newline="\n") as fh:
        for registry, spec in ENDS.items():
            if only and registry not in only:
                continue
            fh.write("tile.%s.name=%s\n" % (registry, spec["display"]))
    written.append(lang_path)

    tab_path = os.path.join(fragment_dir, "ends_tab.java")
    with open(tab_path, "w", newline="\n") as fh:
        fh.write("// Generated by %s -- bounding boxes are the models' own extents clipped to\n"
                 "// the cell, so the terminal's overhang cannot steal a click next door.\n"
                 % GENERATOR)
        for registry, spec in ENDS.items():
            if registry not in bounds:
                continue
            fh.write('initTabBlock(new BlockGuardrailEnd("%s",\n    %s));\n'
                     % (registry, java_bbox(bounds[registry])))
    written.append(tab_path)

    return written


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--scratch", action="store_true",
                        help="write into _guardrail_out/ instead of the repo tree")
    parser.add_argument("--only", nargs="*", default=None,
                        help="only generate these registry names")
    args = parser.parse_args()

    if args.scratch:
        model_dir = os.path.join(SCRATCH_DIR, "models")
        texture_dir = os.path.join(SCRATCH_DIR, "textures")
        blockstate_dir = os.path.join(SCRATCH_DIR, "blockstates")
    else:
        model_dir, texture_dir, blockstate_dir = MODEL_DIR, TEXTURE_DIR, BLOCKSTATE_DIR

    for path in generate(model_dir, texture_dir, blockstate_dir, SCRATCH_DIR, only=args.only):
        print(os.path.relpath(path, layout.REPO_ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
