#!/usr/bin/env python3
"""
gen_guardrails_box_cable.py -- the BOX BEAM guardrail RAIL blocks, the CABLE BARRIER RAIL blocks,
and the cable barrier's own END TREATMENT (an anchor, not a deflecting shoe) for the Traffic
Accessories tab.

Three block families, all built here because they share one texture folder and the box beam and
the cable barrier are both thin enough on distinguishing geometry that splitting them into their
own files would mean splitting the shared sweep/cap primitives too:

    box_beam_guardrail, box_beam_guardrail_double       a square steel tube on a BOLTED post --
                                                        the base plate is what tells it apart from
                                                        a driven post at a glance
    cable_barrier, cable_barrier_double                 three tensioned cables on slim posts, not
                                                        a beam at all
    cable_barrier_anchor                                the raked block the cables terminate into
                                                        and pull against

WHERE THE NUMBERS COME FROM
----------------------------
Every dimension lives in ``guardrail_geometry`` and is imported, never restated: ``BOX_*`` and
``CABLE_*`` for what is unique to these two families, plus the shared ``POST_*``, ``BLOCKOUT_*``,
``SLOPE_*``/``slope_lift``/``POST_LIFT``, ``DIAGONAL_GAP`` and the palette that every guardrail
already uses. The one exception is the cable post's own DEPTH (front-to-back thickness) and the
anchor's own shape, which are not shared with anything else and so are declared locally.

FOLLOWING gen_guardrails.py
----------------------------
The box beam rail is built exactly the way ``gen_guardrails.py`` builds the W-beam: a closed
cross-section swept along X for the length of the cell, a post-plus-block-out drawn only where
``post`` is true, end caps only where nothing connects, and a diagonal filler off the right-hand
end. The box beam's section is a plain rectangle rather than a W, and its post carries a BASE
PLATE the W-beam's does not, but the sweep/cap/fill/blockstate machinery underneath is identical --
generalised here into ``sweep_section``/``cap_section`` so the cable barrier's own (much smaller)
circular sections can reuse it rather than duplicating the sweep math a second time.

THE CABLE BARRIER IS NOT A RAIL
--------------------------------
Three cables at fixed heights, not one solid cross-section, so ``build_cable_core`` sweeps one
small polygon per cable rather than one big one per rail. Everything else -- the slope variants,
the diagonal filler, the ``connectleft``/``connectright`` caps, the ``post`` toggle -- goes through
the same shaped pieces the box beam uses, because ``BlockGuardrail`` is one Java class shared by
every rail-shaped guardrail in this mod: whichever block it is, its blockstate must offer the same
property set, or the class's ``IProperty`` list and the JSON disagree.

THE CABLE ANCHOR IS AN END TREATMENT, NOT A RAIL
--------------------------------------------------
Built the way ``gen_guardrail_ends.py`` builds a W-beam end: one fixed shape, ``facing`` from the
shared eight-way table, and a ``mirrored`` branch for the far end of a run, produced by reflecting
the same mesh about x = 8 exactly as ``mirror_mesh`` there does (copied rather than imported,
since editing that file is off limits for this batch -- see the docstring on the copy below for
why the reflection has to renegotiate winding as well as position).

THE ONE BLOCKSTATE TRICK, AGAIN
---------------------------------
Both rail families reuse ``gen_guardrails.py``'s merge trick verbatim: ``connectright``,
``diagfill`` and ``post`` all sort alphabetically before ``slope``, so ``slope`` carries the post,
the right-hand cap and the filler in the shape each slope needs, and the three booleans null out
the key they own on the branch where their own piece must not appear. See
``rail_blockstate_json`` below, or the fuller explanation in ``gen_guardrails.py`` itself.

Usage:
    python gen_guardrails_box_cable.py                 # writes into the repo tree
    python gen_guardrails_box_cable.py --scratch        # writes into _guardrail_out/ instead

Requires Pillow.
"""
import argparse
import json
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout              # noqa: E402
import guardrail_geometry as geo         # noqa: E402
from gen_work_zone_devices import (      # noqa: E402
    Mesh, box, check_uvs, draw_swatches, facing_variants, fit_in_cell, java_bbox, mesh_bounds,
    set_v_span, uv_swatch, CONCRETE,
    SWATCH_ACCENT_V, SWATCH_BAND_V, SWATCH_BASE_V, SWATCH_DARK_V, TEX_SIZE,
)

TRAFFICACCESSORIES_OWNER = layout.owner_of_folder("trafficaccessories")
MODEL_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER,
                                       "models/block/" + geo.MODEL_SUBDIR)
TEXTURE_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER,
                                         "textures/blocks/trafficaccessories/guardrail")
BLOCKSTATE_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER, "blockstates")
SCRATCH_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_guardrail_out")

GENERATOR = os.path.basename(__file__)
MODEL_PREFIX = "csm:%s" % geo.MODEL_SUBDIR
RAIL_JAVA_CLASS = "BlockGuardrail"
END_JAVA_CLASS = "BlockGuardrailEnd"

# Where the post stands: the middle of the cell, across the run -- same convention as
# gen_guardrails.py, for both rail families.
CX = geo.CELL * 0.5

# Wall thickness of the box beam's C-section post -- the same steel section the W-beam post uses.
POST_WALL = 0.60

# Which flat swatch each box beam part wears.
BOX_RAIL_SWATCH = SWATCH_BASE_V
BOX_POST_SWATCH = SWATCH_BAND_V
BOX_BLOCKOUT_SWATCH = SWATCH_ACCENT_V
# DARK_V is always painted plain black by draw_swatches, which is exactly what a bolted-down base
# plate wants beside a lighter galvanised post: no separate colour to pick.
BOX_PLATE_SWATCH = SWATCH_DARK_V

# Which swatch the cable barrier's two parts wear.
CABLE_POST_SWATCH = SWATCH_BASE_V
CABLE_SWATCH = SWATCH_BAND_V
CABLE_SIDES = 8          # a low-poly strand reads fine at this scale; a run of it is long

# The cable post's own depth (front-to-back thickness). Not shared with anything else, so it is
# declared here rather than in guardrail_geometry -- a simple square, per the brief. Centred
# halfway between the two cable planes, so the post stands squarely between them on the double
# variant and squarely behind the single plane otherwise.
CABLE_POST_Z_HALF = geo.CABLE_POST_HALF
CABLE_POST_CZ = (geo.CABLE_Z + geo.CABLE_BACK_Z) * 0.5


# --- shared closed-section sweep/cap (rectangle for the box beam, small polygons for the cables) -
def _ccw(section):
    """Wind a closed (z, y) section counter-clockwise, so one outward-normal rule serves it all.
    Same test gen_guardrails.py uses for the W profile; copied rather than imported because it is
    six lines and importing it would mean importing the W-beam machinery around it."""
    area = 0.0
    for (z0, y0), (z1, y1) in zip(section, section[1:] + section[:1]):
        area += z0 * y1 - z1 * y0
    return section if area > 0.0 else list(reversed(section))


def polygon_section(cz, cy, r, sides):
    """A small regular polygon in the (z, y) plane, centred on one cable's own position."""
    pts = []
    for i in range(sides):
        a = 2.0 * math.pi * (i + 0.5) / sides
        pts.append((cz + r * math.sin(a), cy + r * math.cos(a)))
    return _ccw(pts)


def sweep_section(mesh, section, swatch_v, x0, x1, lift, grade):
    """Sweep a closed (z, y) section from ``x0`` to ``x1``, lifting by ``grade`` per unit of run.

    Generalised straight out of ``gen_guardrails.py``'s ``sweep_rail``: nothing here depends on the
    section being the W profile, only on it being closed, so the box beam's rectangle and each
    cable's small polygon both sweep through the one function.
    """
    t = uv_swatch(swatch_v)
    lift_b = lift + grade * (x1 - x0)
    for (z0, y0), (z1, y1) in zip(section, section[1:] + section[:1]):
        dz, dy = z1 - z0, y1 - y0
        if abs(dz) < 1e-9 and abs(dy) < 1e-9:
            continue
        normal = (grade * dz, -dz, dy)
        mesh.quad_out([(x0, y0 + lift, z0), (x1, y0 + lift_b, z0),
                       (x1, y1 + lift_b, z1), (x0, y1 + lift, z1)],
                      normal, [t, t, t, t])


def cap_section(mesh, section, swatch_v, x, lift, outward):
    """The plain cut over one open end of a swept section, as a fan from its own centroid.

    A fan is unsafe across the W profile's concave valley (gen_guardrails.py's own ``rail_cap``
    says so), but every section built here -- the box beam's rectangle, a cable's polygon -- is
    convex, and a fan across a convex polygon cannot put a triangle outside the shape or on top of
    another one, so it is the simpler of the two approaches and safe to share between both.
    """
    t = uv_swatch(swatch_v)
    normal = (outward, 0.0, 0.0)
    cz = sum(p[0] for p in section) / len(section)
    cy = sum(p[1] for p in section) / len(section)
    centre = ((x, cy + lift, cz), t, normal)
    for (z0, y0), (z1, y1) in zip(section, section[1:] + section[:1]):
        mesh.tri([centre, ((x, y0 + lift, z0), t, normal), ((x, y1 + lift, z1), t, normal)])


def rail_sides(double):
    """Which rails/cable planes a variant carries: the near one, and on a double-sided run the
    far one too. Same shape gen_guardrails.py's ``rail_sides`` returns, generalised to hand back
    a MIRROR flag for the box beam (which mirrors its rectangle) or a Z PLANE for the cables
    (which do not mirror a shape, only relocate it) via the two callers below."""
    return (False, True) if double else (False,)


# ===================================================================================================
# A. THE BOX BEAM RAIL
# ===================================================================================================
def box_rail_section(mirror=False):
    """The rail's closed cross-section in (z, y): a plain rectangle, front face toward traffic.

    ``mirror`` reflects it about z = CELL / 2 -- the post's own centre line, since
    ``POST_CENTRE_Z`` in guardrail_geometry is exactly ``CELL / 2`` -- for the far rail of a
    double-sided run, the same trick ``gen_guardrails.py``'s ``rail_section`` uses.
    """
    z0, z1 = geo.BOX_RAIL_FRONT_Z, geo.BOX_RAIL_BACK_Z
    y0, y1 = geo.BOX_RAIL_BOTTOM_Y, geo.BOX_RAIL_TOP_Y
    pts = [(z0, y0), (z1, y0), (z1, y1), (z0, y1)]
    if mirror:
        pts = [(geo.CELL - z, y) for (z, y) in pts]
    return _ccw(pts)


def box_rail_cap_z(mirror):
    """(low z, high z) of the box rail's rectangle at either end, mirrored or not."""
    z0, z1 = geo.BOX_RAIL_FRONT_Z, geo.BOX_RAIL_BACK_Z
    if not mirror:
        return z0, z1
    return geo.CELL - z1, geo.CELL - z0


def build_box_post(mesh, double, slope):
    """Post, block-out and BASE PLATE, at the height the rail passes the middle of the cell.

    Post and block-out are the shared steel C-section ``gen_guardrails.py`` builds for the
    non-wood W-beam variant -- box beam is never built on a wooden post. What is new is the base
    plate: a slab wider than the post standing under its foot, which is what says "bolted down"
    rather than "driven" in the reference photograph. It sits UNDER the post rather than around
    it, so the post's own foot is raised onto the plate's top face instead of burying itself in
    the plate's volume -- coincident faces there are opposite-facing (the plate's top points up,
    the post's underside points down) and so excused by the audit as a back-to-back seam, but
    raising the post keeps them from sharing a plane by more than that one incidental face.
    """
    lift = geo.slope_lift(slope, geo.POST_LIFT)
    foot = min(geo.POST_BOTTOM_Y, lift)
    half = geo.BLOCKOUT_HALF_X
    y0, y1 = geo.BLOCKOUT_Y0 + lift, geo.BLOCKOUT_Y1 + lift

    box(mesh, CX - half, CX + half, y0, y1, geo.BLOCKOUT_Z0, geo.BLOCKOUT_Z1, BOX_BLOCKOUT_SWATCH,
        faces=("x-", "x+", "y-", "y+", "z-"))
    if double:
        box(mesh, CX - half, CX + half, y0, y1, geo.BACK_BLOCKOUT_Z0, geo.BACK_BLOCKOUT_Z1,
            BOX_BLOCKOUT_SWATCH, faces=("x-", "x+", "y-", "y+", "z+", "z-"))

    plate_half, plate_thick = geo.BOX_BASE_PLATE
    post_cz = (geo.POST_Z0 + geo.POST_Z1) * 0.5
    plate_y0, plate_y1 = foot, foot + plate_thick
    box(mesh, CX - plate_half, CX + plate_half, plate_y0, plate_y1,
        post_cz - plate_half, post_cz + plate_half, BOX_PLATE_SWATCH)

    post_foot = plate_y1
    top = geo.POST_TOP_Y + lift
    web_z1 = geo.POST_Z0 + POST_WALL
    box(mesh, CX - geo.POST_HALF_X, CX + geo.POST_HALF_X, post_foot, top,
        geo.POST_Z0, web_z1, BOX_POST_SWATCH)
    for sign in (-1.0, 1.0):
        outer = CX + sign * geo.POST_HALF_X
        inner = outer - sign * POST_WALL
        box(mesh, min(outer, inner), max(outer, inner), post_foot, top,
            web_z1, geo.POST_Z1, BOX_POST_SWATCH, faces=("x-", "x+", "y-", "y+", "z+"))


def build_box_core(mesh, double, slope):
    grade = geo.slope_lift(slope, 1.0) / geo.CELL
    for mirror in rail_sides(double):
        sweep_section(mesh, box_rail_section(mirror), BOX_RAIL_SWATCH, 0.0, geo.CELL, 0.0, grade)


def build_box_end(mesh, double, slope, left):
    """The cap over an open end. Like the W-beam, only the right-hand cap needs one per slope --
    the left-hand end is at the foot of the rise on every slope."""
    lift = 0.0 if left else geo.slope_lift(slope, 1.0)
    x = 0.0 if left else geo.CELL
    for mirror in rail_sides(double):
        cap_section(mesh, box_rail_section(mirror), BOX_RAIL_SWATCH, x, lift,
                   -1.0 if left else 1.0)


def build_box_fill(mesh, double, slope):
    lift = geo.slope_lift(slope, 1.0)
    for mirror in rail_sides(double):
        sweep_section(mesh, box_rail_section(mirror), BOX_RAIL_SWATCH,
                      geo.CELL, geo.CELL + geo.DIAGONAL_GAP, lift, 0.0)


def build_box_inventory(mesh, double):
    build_box_core(mesh, double, "flat")
    build_box_post(mesh, double, "flat")
    build_box_end(mesh, double, "flat", True)
    build_box_end(mesh, double, "flat", False)


def box_model_pieces(spec):
    double = spec["double"]
    pieces = {
        "core": lambda m: build_box_core(m, double, "flat"),
        "end_left": lambda m: build_box_end(m, double, "flat", True),
    }
    for slope in geo.SLOPES:
        if slope != "flat":
            pieces["core_%s" % slope] = (lambda s: lambda m: build_box_core(m, double, s))(slope)
        pieces["post_%s" % slope] = (lambda s: lambda m: build_box_post(m, double, s))(slope)
        pieces["fill_%s" % slope] = (lambda s: lambda m: build_box_fill(m, double, s))(slope)
        pieces["end_right_%s" % slope] = (
            (lambda s: lambda m: build_box_end(m, double, s, False))(slope))
    return pieces


# ===================================================================================================
# B. THE CABLE BARRIER
# ===================================================================================================
def cable_planes(double):
    """Which z the cables run in: the near plane, and on a double-sided run the far one too."""
    return (geo.CABLE_Z, geo.CABLE_BACK_Z) if double else (geo.CABLE_Z,)


def build_cable_post(mesh, double, slope):
    """A simple square post -- cable barrier gets no C-section, per the brief. Centred between
    the two cable planes so it stands squarely behind a single-sided run's one plane and squarely
    between a double-sided run's two, and carrying the clip that holds each cable it passes."""
    lift = geo.slope_lift(slope, geo.POST_LIFT)
    foot = min(geo.POST_BOTTOM_Y, lift)
    top = geo.CABLE_POST_TOP_Y + lift
    half = geo.CABLE_POST_HALF
    box(mesh, CX - half, CX + half, foot, top,
        CABLE_POST_CZ - CABLE_POST_Z_HALF, CABLE_POST_CZ + CABLE_POST_Z_HALF, CABLE_POST_SWATCH)
    build_cable_clips(mesh, double, lift)


def build_cable_clips(mesh, double, lift):
    """The strap holding each cable to the post it passes.

    One box per cable per plane, reaching from the post's own face out past the cable's far side.
    It runs THROUGH the cable rather than wrapping it: at this scale a hollow U would be three
    slivers where a solid strap is one clearly bolted-on band, and the cable is opaque either way.

    It is drawn with the post's lift, not the cable's grade, because the whole thing is bolted to
    the post at one x -- the point where the cable crosses it -- so there is nothing here to
    ramp."""
    for z in cable_planes(double):
        outward = -1.0 if z < CABLE_POST_CZ else 1.0
        face = CABLE_POST_CZ + outward * CABLE_POST_Z_HALF
        tip = z + outward * (geo.CABLE_RADIUS + geo.CABLE_CLIP_PROUD)
        z0, z1 = (tip, face) if outward < 0.0 else (face, tip)
        for cy in geo.CABLE_HEIGHTS:
            y = cy + lift
            box(mesh, CX - geo.CABLE_CLIP_X_HALF, CX + geo.CABLE_CLIP_X_HALF,
                y - geo.CABLE_CLIP_Y_HALF, y + geo.CABLE_CLIP_Y_HALF,
                z0, z1, CABLE_POST_SWATCH)


def build_cable_core(mesh, double, slope):
    grade = geo.slope_lift(slope, 1.0) / geo.CELL
    for z in cable_planes(double):
        for cy in geo.CABLE_HEIGHTS:
            section = polygon_section(z, cy, geo.CABLE_RADIUS, CABLE_SIDES)
            sweep_section(mesh, section, CABLE_SWATCH, 0.0, geo.CELL, 0.0, grade)


def build_cable_end(mesh, double, slope, left):
    lift = 0.0 if left else geo.slope_lift(slope, 1.0)
    x = 0.0 if left else geo.CELL
    outward = -1.0 if left else 1.0
    for z in cable_planes(double):
        for cy in geo.CABLE_HEIGHTS:
            section = polygon_section(z, cy, geo.CABLE_RADIUS, CABLE_SIDES)
            cap_section(mesh, section, CABLE_SWATCH, x, lift, outward)


def build_cable_fill(mesh, double, slope):
    """The cables' own continuation into the diagonal gap -- level, the same reason
    gen_guardrails.py's ``build_fill`` gives: all of a slope's rise happens inside its own cell,
    none of it is left for the joint."""
    lift = geo.slope_lift(slope, 1.0)
    for z in cable_planes(double):
        for cy in geo.CABLE_HEIGHTS:
            section = polygon_section(z, cy, geo.CABLE_RADIUS, CABLE_SIDES)
            sweep_section(mesh, section, CABLE_SWATCH,
                         geo.CELL, geo.CELL + geo.DIAGONAL_GAP, lift, 0.0)


def build_cable_inventory(mesh, double):
    build_cable_core(mesh, double, "flat")
    build_cable_post(mesh, double, "flat")
    build_cable_end(mesh, double, "flat", True)
    build_cable_end(mesh, double, "flat", False)


def cable_model_pieces(spec):
    double = spec["double"]
    pieces = {
        "core": lambda m: build_cable_core(m, double, "flat"),
        "end_left": lambda m: build_cable_end(m, double, "flat", True),
    }
    for slope in geo.SLOPES:
        if slope != "flat":
            pieces["core_%s" % slope] = (lambda s: lambda m: build_cable_core(m, double, s))(slope)
        pieces["post_%s" % slope] = (
            (lambda s: lambda m: build_cable_post(m, double, s))(slope))
        pieces["fill_%s" % slope] = (lambda s: lambda m: build_cable_fill(m, double, s))(slope)
        pieces["end_right_%s" % slope] = (
            (lambda s: lambda m: build_cable_end(m, double, s, False))(slope))
    return pieces


# --- the rail catalogue, shared blockstate/model-piece logic for both families -------------------
def _rail(registry, stem, texture, double, display):
    return (registry, {"stem": stem, "texture": texture, "double": double, "display": display})


BOX_BLOCKS = dict([
    _rail(geo.BOX_RAIL_BLOCKS[0], "guardrail_box_beam", "guardrail_box_beam_steel",
         False, "Box Beam Guardrail"),
    _rail(geo.BOX_RAIL_BLOCKS[1], "guardrail_box_beam_double", "guardrail_box_beam_steel",
         True, "Box Beam Guardrail (Double Sided)"),
])

CABLE_RAIL_BLOCKS = dict([
    _rail(geo.CABLE_BLOCKS[0], "guardrail_cable_barrier", "guardrail_cable_barrier_steel",
         False, "Cable Barrier"),
    _rail(geo.CABLE_BLOCKS[1], "guardrail_cable_barrier_double", "guardrail_cable_barrier_steel",
         True, "Cable Barrier (Double Sided)"),
])


def rail_blockstate_json(spec):
    """The Forge blockstate for one box beam or cable barrier block.

    Identical merge trick to ``gen_guardrails.py``'s ``blockstate_json`` -- see that function's own
    docstring for the mechanism -- because both rail families are drawn by the same
    ``BlockGuardrail`` Java class the W-beam and thrie-beam rails use, and that class's property
    set does not change per registry name.
    """
    stem = spec["stem"]
    model = "%s/%s" % (MODEL_PREFIX, stem)
    texture = "%s/%s" % (geo.TEXTURE_PREFIX, spec["texture"])

    def piece(suffix):
        return {"model": "%s_%s.obj" % (model, suffix),
               "custom": {"flip-v": True},
               "textures": {"#%s" % geo.MATERIAL: texture}}

    def submodel(**pieces):
        return {"submodel": {key: piece(suffix) for key, suffix in pieces.items()}}

    slope_variants = {}
    for slope in geo.SLOPES:
        variant = {} if slope == "flat" else {"model": "%s_core_%s.obj" % (model, slope)}
        variant.update(submodel(post="post_%s" % slope, fill="fill_%s" % slope,
                                end_right="end_right_%s" % slope))
        slope_variants[slope] = variant

    variants = {"facing": facing_variants({"diagonal": True})}
    variants["connectleft"] = {"false": submodel(end_left="end_left"), "true": {}}
    variants["connectright"] = {"false": {}, "true": {"submodel": {"end_right": None}}}
    variants["post"] = {"false": {"submodel": {"post": None}}, "true": {}}
    variants["diagfill"] = {"false": {"submodel": {"fill": None}}, "true": {}}
    variants["slope"] = slope_variants
    variants["normal"] = [{}]
    variants["inventory"] = [{"model": "%s_inv.obj" % model,
                              "custom": {"flip-v": True},
                              "textures": {"#%s" % geo.MATERIAL: texture},
                              "transform": "forge:default-block"}]
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "%s_core.obj" % model,
            "custom": {"flip-v": True},
            "textures": {"#%s" % geo.MATERIAL: texture},
        },
        "variants": variants,
    }


# ===================================================================================================
# C. THE CABLE END ANCHOR
# ===================================================================================================
# A raked concrete deadman the cables terminate into: tall where the cables meet it, low at the
# back. Centred on the cable's own z plane rather than the block's axis, since that is where the
# thing it is anchoring actually runs.
ANCHOR_X = (0.00, 8.60)                              # within its own cell, like the other ends
ANCHOR_Z_HALF = 2.20
ANCHOR_Z = (geo.CABLE_Z - ANCHOR_Z_HALF, geo.CABLE_Z + ANCHOR_Z_HALF)
ANCHOR_FRONT_Y = geo.CABLE_POST_TOP_Y                # as tall as the run's own post
ANCHOR_BACK_Y = 4.20                                 # raked low at the back

# The steel anchor plate bolted to the concrete's front face, spanning the cluster of cables.
ANCHOR_PLATE_X_HALF = 0.35
ANCHOR_PLATE_Y = (geo.CABLE_HEIGHTS[0] - 1.60, geo.CABLE_HEIGHTS[-1] + 1.60)
ANCHOR_PLATE_Z_HALF = ANCHOR_Z_HALF - 0.30

# Each cable's own stub, reaching a little OUT of the block (into the cell the run occupies) and a
# little INTO it (buried in the concrete) -- the same "legitimately reaches beyond its own cell"
# allowance gen_guardrail_ends.py gives the energy-absorbing terminal, and clipped back to the cell
# for the bounding box the same way.
ANCHOR_STUB_X = (-1.60, 2.20)

ANCHOR_TEXTURE = "guardrail_cable_anchor"


def mirror_mesh(mesh):
    """Reflect a finished mesh about x = 8 -- the run axis -- for the run's other end.

    Copied verbatim from ``gen_guardrail_ends.py`` (editing that file is off limits for this
    batch). A REFLECTION, not a rotation about Y: reflecting in x reverses the run direction while
    leaving the anchor's own z position where it was, which is what the far end of a run needs
    since both ends carry the same facing.

    The catch is that a reflection reverses handedness. ``Mesh`` resolved every triangle's winding
    against its own normals when it was added, so reflecting the positions alone leaves the whole
    model inside-out, culling from the side you are looking at -- which the audit reports as
    ``winding``. Negating the normals' x and reversing every face's vertex order carries the two
    round together.
    """
    half = geo.CELL * 0.5
    mesh.v[:] = [(2.0 * half - x, y, z) for (x, y, z) in mesh.v]
    mesh.vn[:] = [(-nx, ny, nz) for (nx, ny, nz) in mesh.vn]
    mesh.f[:] = [list(reversed(tri)) for tri in mesh.f]


def build_anchor(mesh):
    x0, x1 = ANCHOR_X
    z0, z1 = ANCHOR_Z
    t_body = uv_swatch(SWATCH_BASE_V)

    # The raked concrete deadman: a six-sided solid, tall at the front where the cables meet it,
    # low at the back. Built as explicit quads (rather than through gen_work_zone_devices' own
    # ``prism``) so each of the six faces stays a named, single-purpose piece -- the front wall is
    # exactly the plane the cable stubs are built to poke through without sharing.
    def quad(pts, normal):
        mesh.quad_out(list(pts), normal, [t_body, t_body, t_body, t_body])

    # Front wall (x = x0): tall, the face the cables terminate against.
    quad([(x0, 0.0, z0), (x0, 0.0, z1), (x0, ANCHOR_FRONT_Y, z1), (x0, ANCHOR_FRONT_Y, z0)],
        (-1.0, 0.0, 0.0))
    # Back wall (x = x1): low, the raked back of the block.
    quad([(x1, 0.0, z1), (x1, 0.0, z0), (x1, ANCHOR_BACK_Y, z0), (x1, ANCHOR_BACK_Y, z1)],
        (1.0, 0.0, 0.0))
    # The two sides, each a flat quad at z0/z1 sloping from the tall front down to the low back.
    quad([(x0, 0.0, z0), (x1, 0.0, z0), (x1, ANCHOR_BACK_Y, z0), (x0, ANCHOR_FRONT_Y, z0)],
        (0.0, 0.0, -1.0))
    quad([(x1, 0.0, z1), (x0, 0.0, z1), (x0, ANCHOR_FRONT_Y, z1), (x1, ANCHOR_BACK_Y, z1)],
        (0.0, 0.0, 1.0))
    # The raked top -- the only face of the six that actually needs its own normal computed rather
    # than named, since it leans; the other five are all axis-aligned in the frame this block is
    # authored in.
    top_pts = [(x0, ANCHOR_FRONT_Y, z0), (x1, ANCHOR_BACK_Y, z0),
              (x1, ANCHOR_BACK_Y, z1), (x0, ANCHOR_FRONT_Y, z1)]
    top_normal = (ANCHOR_FRONT_Y - ANCHOR_BACK_Y, x1 - x0, 0.0)
    quad(top_pts, top_normal)
    # The footing, on the ground.
    quad([(x0, 0.0, z1), (x1, 0.0, z1), (x1, 0.0, z0), (x0, 0.0, z0)], (0.0, -1.0, 0.0))

    # The steel anchor plate, standing proud of the front wall rather than flush with it -- flush
    # would put the plate's own back face exactly on the wall's plane, the coplanar-overlap fault
    # every guardrail generator in this batch has to dodge once.
    box(mesh, x0 - ANCHOR_PLATE_X_HALF, x0 + ANCHOR_PLATE_X_HALF,
       ANCHOR_PLATE_Y[0], ANCHOR_PLATE_Y[1],
       geo.CABLE_Z - ANCHOR_PLATE_Z_HALF, geo.CABLE_Z + ANCHOR_PLATE_Z_HALF, SWATCH_BAND_V)

    # Each cable's own stub: capped where it pokes clear of the block, open where it is buried --
    # an uncapped end inside a solid is never seen, the same reasoning ``gen_guardrail_ends.py``
    # gives for leaving the far cap off a rail post's block-out.
    stub_x0, stub_x1 = ANCHOR_STUB_X
    for cy in geo.CABLE_HEIGHTS:
        section = polygon_section(geo.CABLE_Z, cy, geo.CABLE_RADIUS, CABLE_SIDES)
        sweep_section(mesh, section, SWATCH_ACCENT_V, stub_x0, stub_x1, 0.0, 0.0)
        cap_section(mesh, section, SWATCH_ACCENT_V, stub_x0, 0.0, -1.0)


def build_anchor_inventory(mesh):
    build_anchor(mesh)
    fit_in_cell(mesh)


def anchor_cell_bounds(bounds):
    """Mesh bounds clipped to this block's own cell -- the cable stubs reach past x = 0 on
    purpose, and an AABB that followed them there would steal a click from the block behind this
    one, the same reasoning ``gen_guardrail_ends.py``'s ``cell_bounds`` gives for the terminal."""
    lo = tuple(max(0.0, v) for v in bounds[:3])
    hi = tuple(min(geo.CELL, v) for v in bounds[3:])
    return lo + hi


def anchor_texture():
    img = Image.new("RGBA", (TEX_SIZE, TEX_SIZE), CONCRETE + (255,))
    draw_swatches(img, CONCRETE, geo.GALVANISED, geo.GALVANISED_DARK)
    return img


def anchor_blockstate_json():
    variants = {
        "facing": facing_variants({"diagonal": True}),
        "mirrored": {
            "false": {},
            "true": {"model": "%s/guardrail_cable_anchor_mirrored.obj" % MODEL_PREFIX},
        },
        "normal": [{}],
        "inventory": [{"model": "%s/guardrail_cable_anchor_inv.obj" % MODEL_PREFIX,
                       "custom": {"flip-v": True},
                       "textures": {"#%s" % geo.MATERIAL: "%s/%s"
                                    % (geo.TEXTURE_PREFIX, ANCHOR_TEXTURE)},
                       "transform": "forge:default-block"}],
    }
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "%s/guardrail_cable_anchor.obj" % MODEL_PREFIX,
            "custom": {"flip-v": True},
            "textures": {"#%s" % geo.MATERIAL: "%s/%s" % (geo.TEXTURE_PREFIX, ANCHOR_TEXTURE)},
        },
        "variants": variants,
    }


# ===================================================================================================
# D. THE BOX BEAM BULLNOSE END
# ===================================================================================================
# The tube leaves the last rail dead straight, turns through a half circle in plan away from the
# roadway, and runs back parallel to itself. Everything here is in PLAN -- the tube keeps the run's
# own height the whole way round, which is what makes a bullnose different from every W-beam end
# in this batch: those all get their shape by dropping or flaring the rail, and this one gets its
# shape without leaving the horizontal at all.
BULLNOSE_LEAD_X = 3.60                               # dead straight before the turn starts
BULLNOSE_RADIUS = 4.10                               # centre-line radius of the half turn
BULLNOSE_RETURN_X = 1.40                             # where the returning leg is cut off
BULLNOSE_SAMPLES = 15                                # stations round the half circle
BULLNOSE_CZ = (geo.BOX_RAIL_FRONT_Z + geo.BOX_RAIL_BACK_Z) * 0.5
BULLNOSE_HALF = (geo.BOX_RAIL_BACK_Z - geo.BOX_RAIL_FRONT_Z) * 0.5
BULLNOSE_RETURN_CZ = BULLNOSE_CZ + 2.0 * BULLNOSE_RADIUS
BULLNOSE_APEX_X = BULLNOSE_LEAD_X + BULLNOSE_RADIUS

# One post under each leg and one under the nose. Bolted down like every other box beam post,
# but on a NARROWER plate: the run's own plate is nearly seven units across, which is fine when the
# next post is a whole cell away and laps straight over its neighbour when three posts share one
# cell. Two slabs on the same ground plane are a coplanar overlap, so the plate is sized to the
# spacing rather than the spacing to the plate.
BULLNOSE_PLATE = (2.20, geo.BOX_BASE_PLATE[1])       # half width, thickness
BULLNOSE_POSTS = (
    (2.40, BULLNOSE_CZ),
    (2.40, BULLNOSE_RETURN_CZ),
    (BULLNOSE_APEX_X, BULLNOSE_CZ + BULLNOSE_RADIUS),
)

# The object marker on the nose, standing on top of the tube and facing the traffic the bullnose
# is turned away from. Its foot is sunk INTO the tube rather than resting on it: a plate whose
# underside sat exactly on the tube's top would share a plane with it, which is the one fault
# every generator in this batch has to dodge.
BULLNOSE_MARKER_X = (BULLNOSE_APEX_X - 0.30, BULLNOSE_APEX_X + 0.30)
# Capped rather than simply hung off the rail: the rails were raised by MOUNT_LIFT and a marker
# that followed them all the way up would stand out of the top of its own cell.
BULLNOSE_MARKER_Y = (geo.BOX_RAIL_TOP_Y - 0.80, min(geo.BOX_RAIL_TOP_Y + 4.00, 15.60))
BULLNOSE_MARKER_Z_HALF = 1.60

BULLNOSE_TEXTURE = "guardrail_box_end"
BULLNOSE_MODEL = "guardrail_end_bullnose_box"


def bullnose_path():
    """The tube's centre line in plan, as (x, z) stations: lead-in, half turn, return leg.

    The turn is centred one radius to the FAR side of the run from the traffic face, so the tube
    leaves the lead-in already travelling straight -- a turn centred anywhere else would put a
    kink at the joint, and the joint is the one place on an end treatment that has to line up with
    something else.
    """
    cx, cz = BULLNOSE_LEAD_X, BULLNOSE_CZ + BULLNOSE_RADIUS
    pts = [(0.0, BULLNOSE_CZ)]
    for i in range(BULLNOSE_SAMPLES):
        a = -0.5 * math.pi + math.pi * i / float(BULLNOSE_SAMPLES - 1)
        pts.append((cx + BULLNOSE_RADIUS * math.cos(a), cz + BULLNOSE_RADIUS * math.sin(a)))
    pts.append((BULLNOSE_RETURN_X, BULLNOSE_RETURN_CZ))
    return pts


def bullnose_section():
    """The tube's cross-section as (offset ACROSS the path, y).

    Across the path rather than across z, which is the whole difference between this and
    ``box_rail_section``: a section written in z can only be swept down a straight run, and this
    one has to stay square to a curve.
    """
    y0, y1 = geo.BOX_RAIL_BOTTOM_Y, geo.BOX_RAIL_TOP_Y
    return [(-BULLNOSE_HALF, y0), (BULLNOSE_HALF, y0),
            (BULLNOSE_HALF, y1), (-BULLNOSE_HALF, y1)]


def _path_frames(path):
    """Each station's point and unit normal across the path, from its own tangent."""
    frames = []
    for i, (px, pz) in enumerate(path):
        ax, az = path[max(i - 1, 0)]
        bx, bz = path[min(i + 1, len(path) - 1)]
        tx, tz = bx - ax, bz - az
        length = math.hypot(tx, tz) or 1.0
        tx, tz = tx / length, tz / length
        frames.append(((px, pz), (-tz, tx), (tx, tz)))
    return frames


def sweep_along_path(mesh, path, section, swatch_v):
    """Sweep a closed (offset, y) section along a plan path, capping both open ends.

    The same job ``sweep_section`` does for a straight run, with the section's offset axis turned
    to follow the path instead of pinned to z. Outward normals come out of the section the same
    way -- an edge running (d_offset, dy) has its outward side at (dy, -d_offset) in the section's
    own frame -- so a counter-clockwise section gives outward normals here exactly as it does
    there, and no piece of this needs its own winding rule.
    """
    t = uv_swatch(swatch_v)
    frames = _path_frames(path)
    rings = [[(px + ox * o, y, pz + oz * o) for (o, y) in section]
             for ((px, pz), (ox, oz), _tan) in frames]

    n_pts = len(section)
    for k in range(len(rings) - 1):
        a, b = rings[k], rings[k + 1]
        (ox, oz) = frames[k][1]
        for i in range(n_pts):
            j = (i + 1) % n_pts
            d_off = section[j][0] - section[i][0]
            dy = section[j][1] - section[i][1]
            normal = (ox * dy, -d_off, oz * dy)
            mesh.quad_out([a[i], b[i], b[j], a[j]], normal, [t, t, t, t])

    for (ring, (_pt, _n, (tx, tz)), sign) in ((rings[0], frames[0], -1.0),
                                              (rings[-1], frames[-1], 1.0)):
        normal = (sign * tx, 0.0, sign * tz)
        cx = sum(p[0] for p in ring) / n_pts
        cy = sum(p[1] for p in ring) / n_pts
        cz = sum(p[2] for p in ring) / n_pts
        centre = ((cx, cy, cz), t, normal)
        for i in range(n_pts):
            j = (i + 1) % n_pts
            first, second = (i, j) if sign > 0.0 else (j, i)
            mesh.tri([centre, (ring[first], t, normal), (ring[second], t, normal)])


def bullnose_post(mesh, x, z):
    """A bolted-down post under the tube at one point on the path.

    The base plate and the raised foot are the box beam run's own arrangement, taken from
    ``build_box_post`` rather than restated: box beam is bolted down, not driven, and a bullnose
    that stood on a driven post would say the opposite of what the run beside it says.
    """
    plate_half, plate_thick = BULLNOSE_PLATE
    box(mesh, x - plate_half, x + plate_half, 0.0, plate_thick,
        z - plate_half, z + plate_half, BOX_PLATE_SWATCH)
    half = geo.POST_HALF_X
    box(mesh, x - half, x + half, plate_thick, geo.BOX_RAIL_BOTTOM_Y + 0.40,
        z - half, z + half, BOX_POST_SWATCH)


def build_bullnose(mesh):
    sweep_along_path(mesh, bullnose_path(), bullnose_section(), BOX_RAIL_SWATCH)
    for (x, z) in BULLNOSE_POSTS:
        bullnose_post(mesh, x, z)
    cz = BULLNOSE_CZ + BULLNOSE_RADIUS
    box(mesh, BULLNOSE_MARKER_X[0], BULLNOSE_MARKER_X[1],
        BULLNOSE_MARKER_Y[0], BULLNOSE_MARKER_Y[1],
        cz - BULLNOSE_MARKER_Z_HALF, cz + BULLNOSE_MARKER_Z_HALF, SWATCH_ACCENT_V)


def build_bullnose_inventory(mesh):
    build_bullnose(mesh)
    fit_in_cell(mesh)


def bullnose_texture():
    """The run's own galvanised steel, plus a yellow accent for the object marker."""
    img = Image.new("RGBA", (TEX_SIZE, TEX_SIZE), geo.GALVANISED + (255,))
    draw_swatches(img, geo.GALVANISED, geo.GALVANISED_DARK, geo.CHEVRON_YELLOW)
    return img


def bullnose_blockstate_json():
    variants = {
        "facing": facing_variants({"diagonal": True}),
        "mirrored": {
            "false": {},
            "true": {"model": "%s/%s_mirrored.obj" % (MODEL_PREFIX, BULLNOSE_MODEL)},
        },
        "normal": [{}],
        "inventory": [{"model": "%s/%s_inv.obj" % (MODEL_PREFIX, BULLNOSE_MODEL),
                       "custom": {"flip-v": True},
                       "textures": {"#%s" % geo.MATERIAL: "%s/%s"
                                    % (geo.TEXTURE_PREFIX, BULLNOSE_TEXTURE)},
                       "transform": "forge:default-block"}],
    }
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "%s/%s.obj" % (MODEL_PREFIX, BULLNOSE_MODEL),
            "custom": {"flip-v": True},
            "textures": {"#%s" % geo.MATERIAL: "%s/%s" % (geo.TEXTURE_PREFIX, BULLNOSE_TEXTURE)},
        },
        "variants": variants,
    }


# --- output -----------------------------------------------------------------------------------------
def write_model(mesh, path, name, mtl_file):
    """Write one OBJ, stamped with THIS generator's own name -- ``Mesh.write`` stamps the work
    zone script's name into the header, which is where the shared primitives live, and a model
    that says it came from a generator that does not build it sends the next person to the wrong
    file."""
    check_uvs(mesh, name)
    mesh.write(path, name, mtl_file)
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    text = text.replace("gen_work_zone_devices.py", GENERATOR, 1)
    with open(path, "w", newline="\n", encoding="utf-8") as fh:
        fh.write(text)


def write_mtl(path, texture_name):
    with open(path, "w", newline="\n") as fh:
        fh.write("# Procedurally generated by dev-env-utils/scripts/%s -- do not hand edit\n"
                 % GENERATOR)
        fh.write("# One material; every colour variant's blockstate retextures #%s.\n"
                 % geo.MATERIAL)
        fh.write("newmtl %s\n" % geo.MATERIAL)
        fh.write("map_Kd %s/%s\n" % (geo.TEXTURE_PREFIX, texture_name))


def box_beam_texture():
    """One steel sheet for both box beam variants -- no wood, box beam is never built that way."""
    img = Image.new("RGBA", (TEX_SIZE, TEX_SIZE), geo.GALVANISED + (255,))
    draw_swatches(img, geo.GALVANISED, geo.GALVANISED_DARK, geo.GALVANISED_DARK)
    return img


def cable_barrier_texture():
    img = Image.new("RGBA", (TEX_SIZE, TEX_SIZE), geo.GALVANISED + (255,))
    draw_swatches(img, geo.GALVANISED, geo.GALVANISED_DARK, geo.GALVANISED_DARK)
    return img


TEXTURES = {
    "guardrail_box_beam_steel": box_beam_texture,
    "guardrail_cable_barrier_steel": cable_barrier_texture,
    ANCHOR_TEXTURE: anchor_texture,
    BULLNOSE_TEXTURE: bullnose_texture,
}


def _write_rail_family(blocks, model_pieces_fn, build_inventory_fn, model_dir, texture_dir,
                       blockstate_dir, written, bounds):
    for registry, spec in blocks.items():
        stem = spec["stem"]
        mtl_file = stem + ".mtl"
        for suffix, builder in model_pieces_fn(spec).items():
            mesh = Mesh()
            builder(mesh)
            name = "%s_%s" % (stem, suffix)
            path = os.path.join(model_dir, name + ".obj")
            write_model(mesh, path, name, mtl_file)
            written.append(path)

        inv = Mesh()
        build_inventory_fn(inv, spec["double"])
        inv_path = os.path.join(model_dir, stem + "_inv.obj")
        write_model(inv, inv_path, stem + "_inv", mtl_file)
        written.append(inv_path)
        bounds[registry] = mesh_bounds(inv)

        mtl_path = os.path.join(model_dir, mtl_file)
        write_mtl(mtl_path, spec["texture"])
        written.append(mtl_path)

        bs_path = os.path.join(blockstate_dir, registry + ".json")
        with open(bs_path, "w", newline="\n") as fh:
            json.dump(rail_blockstate_json(spec), fh, indent=2)
            fh.write("\n")
        written.append(bs_path)


def _write_end_treatment(registry, model, texture, build, build_inventory, blockstate_fn,
                         clip_bounds, model_dir, blockstate_dir, written, bounds):
    """Emit one end treatment: its model, its mirrored sibling, its inventory model, its material
    and its blockstate.

    The mirrored sibling is built from the SAME builder and reflected, never built a second time,
    so the two ends of a run cannot end up two different shapes -- and sharing one MTL means they
    cannot end up two different textures either.
    """
    mtl_file = model + ".mtl"

    mesh = Mesh()
    build(mesh)
    bounds[registry] = clip_bounds(mesh_bounds(mesh))
    obj_path = os.path.join(model_dir, model + ".obj")
    write_model(mesh, obj_path, model, mtl_file)
    written.append(obj_path)

    mirrored = Mesh()
    build(mirrored)
    mirror_mesh(mirrored)
    mirror_path = os.path.join(model_dir, model + "_mirrored.obj")
    write_model(mirrored, mirror_path, model + "_mirrored", mtl_file)
    written.append(mirror_path)

    inv_mesh = Mesh()
    build_inventory(inv_mesh)
    inv_path = os.path.join(model_dir, model + "_inv.obj")
    write_model(inv_mesh, inv_path, model + "_inv", mtl_file)
    written.append(inv_path)

    mtl_path = os.path.join(model_dir, mtl_file)
    write_mtl(mtl_path, texture)
    written.append(mtl_path)

    bs_path = os.path.join(blockstate_dir, registry + ".json")
    with open(bs_path, "w", newline="\n") as fh:
        json.dump(blockstate_fn(), fh, indent=2)
        fh.write("\n")
    written.append(bs_path)


def generate(model_dir, texture_dir, blockstate_dir, fragment_dir):
    for d in (model_dir, texture_dir, blockstate_dir, fragment_dir):
        os.makedirs(d, exist_ok=True)
    set_v_span(geo.CELL)
    written = []
    bounds = {}

    for texture, build in TEXTURES.items():
        path = os.path.join(texture_dir, texture + ".png")
        build().save(path)
        written.append(path)

    _write_rail_family(BOX_BLOCKS, box_model_pieces, build_box_inventory,
                       model_dir, texture_dir, blockstate_dir, written, bounds)
    _write_rail_family(CABLE_RAIL_BLOCKS, cable_model_pieces, build_cable_inventory,
                       model_dir, texture_dir, blockstate_dir, written, bounds)

    # C. The cable end anchor and D. the box beam bullnose: one fixed shape apiece, each with a
    # mirrored sibling, exactly like a W-beam end.
    anchor_registry = geo.CABLE_END_BLOCKS[0]
    _write_end_treatment(anchor_registry, "guardrail_cable_anchor", ANCHOR_TEXTURE,
                         build_anchor, build_anchor_inventory, anchor_blockstate_json,
                         anchor_cell_bounds,
                         model_dir, blockstate_dir, written, bounds)

    bullnose_registry = geo.BOX_END_BLOCKS[0]
    _write_end_treatment(bullnose_registry, BULLNOSE_MODEL, BULLNOSE_TEXTURE,
                         build_bullnose, build_bullnose_inventory, bullnose_blockstate_json,
                         anchor_cell_bounds,
                         model_dir, blockstate_dir, written, bounds)

    # Fragments, for pasting into the lang file and the tab: this generator never rewrites a file
    # it does not own.
    all_display = {}
    all_display.update({k: v["display"] for k, v in BOX_BLOCKS.items()})
    all_display.update({k: v["display"] for k, v in CABLE_RAIL_BLOCKS.items()})
    all_display[anchor_registry] = "Cable Barrier Anchor"
    all_display[bullnose_registry] = "Box Beam Guardrail End (Bullnose)"

    lang_path = os.path.join(fragment_dir, "box_cable_lang.txt")
    with open(lang_path, "w", newline="\n") as fh:
        for registry, display in all_display.items():
            fh.write("tile.%s.name=%s\n" % (registry, display))
    written.append(lang_path)

    tab_path = os.path.join(fragment_dir, "box_cable_tab.java")
    with open(tab_path, "w", newline="\n") as fh:
        fh.write("// Generated by %s -- the rail bounding boxes are the INVENTORY models' own\n"
                 "// extent (the diagonal filler is left out of them on purpose), and the\n"
                 "// anchor's is its own extent clipped to the cell, since its cable stubs\n"
                 "// legitimately reach past x = 0.\n" % GENERATOR)
        for registry in list(BOX_BLOCKS) + list(CABLE_RAIL_BLOCKS):
            fh.write('initTabBlock(new %s("%s",\n    %s));\n'
                     % (RAIL_JAVA_CLASS, registry, java_bbox(bounds[registry])))
        for registry in (anchor_registry, bullnose_registry):
            fh.write('initTabBlock(new %s("%s",\n    %s));\n'
                     % (END_JAVA_CLASS, registry, java_bbox(bounds[registry])))
    written.append(tab_path)

    return written


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--scratch", action="store_true",
                        help="write models, textures and blockstates into _guardrail_out/ too")
    args = parser.parse_args()

    if args.scratch:
        model_dir = os.path.join(SCRATCH_DIR, "models")
        texture_dir = os.path.join(SCRATCH_DIR, "textures")
        blockstate_dir = os.path.join(SCRATCH_DIR, "blockstates")
    else:
        model_dir, texture_dir, blockstate_dir = MODEL_DIR, TEXTURE_DIR, BLOCKSTATE_DIR

    for path in generate(model_dir, texture_dir, blockstate_dir, SCRATCH_DIR):
        print(os.path.relpath(path, layout.REPO_ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
