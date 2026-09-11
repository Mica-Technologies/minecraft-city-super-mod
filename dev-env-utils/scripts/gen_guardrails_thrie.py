#!/usr/bin/env python3
"""
gen_guardrails_thrie.py -- the THRIE-BEAM guardrail rails, their two end treatments, and the
W-beam-to-thrie transition piece.

Seven blocks, all of them the W-beam machinery in ``gen_guardrails.py`` and ``gen_guardrail_ends.py``
carrying a deeper rail:

    thrie_beam_guardrail*          four rail blocks -- steel or timber post, one rail or two
    guardrail_end_flared_thrie     the flared shoe, folded out of a rail two and a half times as deep
    guardrail_end_turndown_thrie   the rail ramped down to a ground anchor
    w_beam_thrie_transition        one cell that carries a W-beam run into a thrie-beam one

Every dimension is imported from ``guardrail_geometry``; nothing here restates one. The pieces that
are *derived* rather than shared -- the block-out's margin above and below the rail, and how far the
post stands proud of it -- are taken from the W-beam's own numbers so that a thrie post is the same
hardware as a W post, just taller:

    BLOCKOUT_MARGIN = RAIL_BOTTOM_Y - BLOCKOUT_Y0        the block-out overhangs the rail's bottom
    BLOCKOUT_TUCK   = RAIL_TOP_Y    - BLOCKOUT_Y1        and stops this far under its top
    POST_PROUD      = POST_TOP_Y    - RAIL_TOP_Y         the post stands this far above it (none)

The thrie rail's BOTTOM is lower than the W's (7.00 against 11.00), while the two share a top, so a
block-out sized for a W-beam would leave the lower corrugations bolted to nothing. That is what
those derivations are for.

THE PROFILE MORPH, AND WHY BOTH PROFILES ARE RESAMPLED
------------------------------------------------------
The transition's rail is the W profile at x = 0 and the thrie profile at x = 16, interpolated
across the single cell. The two profiles have different numbers of stations -- nine against
thirteen -- so there is no vertex-to-vertex correspondence to blend along, and pairing them by
index would blend the W's mid-height valley into the thrie's *middle crest* and turn the section
inside out part way across.

Both are therefore resampled onto ONE set of stations before anything is blended. The stations are
the sorted union of the two profiles' own vertices, expressed as normalised ARC LENGTH along the
pressed face. That has the property that matters: at x = 0 every W vertex is still exactly on the
curve and at x = 16 every thrie vertex is, so each end of the transition meets its own run exactly,
while in between the corrugations slide and merge instead of popping.

CHIRALITY, AND WHY THE MIRRORED CORE IS NOT A REFLECTED ``up``
---------------------------------------------------------------
The transition is chiral for the same reason the end treatments are: facing means the way the rail
LOOKS, so turning the block round does not swap its two ends, it points the rail away from the
road. A run going thrie-to-W left-to-right needs a mirrored model.

For a FLAT transition the mirrored model is exactly ``mirror_mesh`` of the unmirrored one -- a
reflection about x = 8, the run axis -- and the self-check in ``_verify_mirror_identity`` asserts
that vertex for vertex, so this file cannot drift from ``gen_guardrail_ends.mirror_mesh``.

On a grade the reflection also swaps the slope: an ``up`` cell runs from lift 0 to +16, and its
reflection runs from +16 back to 0, which is exactly a ``down`` cell. The mirrored cores are built
directly anyway, by running the profile blend the other way along the cell's own slope, and the
identity::

    mirrored(s) = reflect(core(opposite s))

is asserted for all three slopes by ``_verify_mirror_identity``, so the two constructions cannot
drift apart. The diagonal filler and the end caps have to be built directly in any case:
``diagfill`` names the world side a filler grows on, so it stays off the RIGHT-hand end in both
hands rather than being carried to the left by the reflection.

THE BLOCKSTATE
--------------
The rails use ``gen_guardrails``' merge trick unchanged -- see the comment on ``blockstate_json``
there. The transition needs one more turn of it, because ``mirrored`` makes the base rail answer to
two properties at once:

  * Forge sorts the property names ALPHABETICALLY and merges earlier into later with ``sync``,
    which only fills what is not already set. So of ``connectleft, connectright, diagfill, facing,
    mirrored, post, slope`` the EARLIER one wins any key the two both touch.
  * ``"model": null`` is a first-class value, not an accident: ``ForgeBlockStateV1.Variant``'s
    deserializer sets ``modelSet`` and leaves ``model`` null with the comment "Allow overriding
    base model to remove it from a state", ``ForgeVariant`` then passes ``builtin/missing`` up, and
    ``ForgeVariant.process`` drops the base and bakes the submodels alone.

So ``mirrored=true`` nulls the base model -- which blocks ``slope``'s own per-slope model as well
as the default -- and the mirrored rail arrives as the ``core_m`` submodel that ``slope`` supplies
and ``mirrored=false`` nulls. The unmirrored hand is then structurally identical to a plain rail.

Usage:
    python gen_guardrails_thrie.py               # writes into the repo tree
    python gen_guardrails_thrie.py --scratch     # writes into _guardrail_out/ instead

Requires Pillow.
"""
import argparse
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout                   # noqa: E402
import guardrail_geometry as geo              # noqa: E402

# The W-beam generators are imported, never copied. Two definitions of the C-section's wall or of
# the swatch a part wears would drift, and a thrie post that is not the same hardware as the W post
# beside it is exactly the fault a shared constant exists to prevent. ``_ccw`` is private there
# only because nothing else had needed it yet.
from gen_guardrails import (                  # noqa: E402
    CX, POST_WALL, RAIL_SWATCH, POST_SWATCH, BLOCKOUT_SWATCH, guardrail_texture, _ccw as ccw,
)
from gen_guardrail_ends import (              # noqa: E402
    POST_BITE, mirror_mesh, rail_stations, sweep_rail as sweep_end_rail, vmul,
    FLARED_LEN, FLARED_BACK, FLARED_DROP, FLARED_NOSE_AT, FLARED_NOSE_FLATTEN, FLARED_NOSE_HSCALE,
    FLARED_POSTS, TURN_LEN, TURN_END_Y, TURN_POW, TURN_POST_AT, TURN_ANCHOR, curve,
    RAIL_THICK as END_RAIL_THICK,
)
from gen_work_zone_devices import (           # noqa: E402
    Mesh, box, check_uvs, facing_variants, java_bbox, mesh_bounds, set_v_span, uv_swatch,
    vcross, vsub, SWATCH_BASE_V, SWATCH_DARK_V,
)

TRAFFICACCESSORIES_OWNER = layout.owner_of_folder("trafficaccessories")
MODEL_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER,
                                       "models/block/" + geo.MODEL_SUBDIR)
TEXTURE_DIR = layout.asset_dir_for_write(
    TRAFFICACCESSORIES_OWNER, "textures/" + geo.TEXTURE_PREFIX.split(":", 1)[1])
BLOCKSTATE_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER, "blockstates")
SCRATCH_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_guardrail_out")

GENERATOR = os.path.basename(__file__)
MODEL_PREFIX = "csm:%s" % geo.MODEL_SUBDIR
RAIL_CLASS = "BlockGuardrail"
END_CLASS = "BlockGuardrailEnd"

TEXTURE_STEEL = "guardrail_thrie_steel"
TEXTURE_TIMBER = "guardrail_thrie_timber"

# --- derived from the W-beam's own hardware -------------------------------------------------------
#: How far the block-out overhangs the rail, top and bottom. Read off the W-beam rather than chosen
#: again, so the spacer is the same part on both rails.
BLOCKOUT_MARGIN = geo.RAIL_BOTTOM_Y - geo.BLOCKOUT_Y0
#: How far under the rail's top the block-out stops; see ``guardrail_geometry.BLOCKOUT_TUCK``.
BLOCKOUT_TUCK = geo.RAIL_TOP_Y - geo.BLOCKOUT_Y1
#: How far the post stands above the rail it carries.
POST_PROUD = geo.POST_TOP_Y - geo.RAIL_TOP_Y

THRIE_HEIGHT = geo.THRIE_RAIL_TOP_Y - geo.THRIE_RAIL_BOTTOM_Y

# The end treatments' sweep uses the sheet thickness derived from the W profile. The thrie profile
# is pressed to the same depth into a rail declared the same depth, so the same sheet serves -- but
# a change to either would silently give the thrie shoe a rail of the wrong thickness, so it is
# checked rather than assumed.
_THRIE_SHEET = ((geo.THRIE_RAIL_BACK_Z - geo.THRIE_RAIL_FRONT_Z)
                - max(o for (o, _y) in geo.THRIE_PROFILE))
assert abs(_THRIE_SHEET - END_RAIL_THICK) < 1e-9, (
    "the thrie rail's sheet thickness (%.4f) no longer matches the W-beam's (%.4f); "
    "gen_guardrail_ends.sweep_rail cannot be shared" % (_THRIE_SHEET, END_RAIL_THICK))

#: Front-face polylines in (z, y), which is the frame everything below sweeps and caps in.
W_FRONT = [(geo.RAIL_FRONT_Z + o, y) for (o, y) in geo.W_PROFILE]
THRIE_FRONT = [(geo.THRIE_RAIL_FRONT_Z + o, y) for (o, y) in geo.THRIE_PROFILE]

#: How much of the cell the transition's blend is sampled over. One ring per unit and a bit: the
#: corrugation count changes across a single block, and too few rings makes that read as a crease.
TRANSITION_RINGS = 13

#: The thrie turndown folds as well as ramps. A nine-unit-deep beam dropped bodily to the ground
#: still stands most of a cell high at its tip, which reads as a rail that was cut off rather than
#: turned down, so the section closes toward the tip the way the flared shoe's nose does.
THRIE_TURN_FOLD_AT = 0.30
THRIE_TURN_HSCALE = 0.55


# --- profile resampling -----------------------------------------------------------------------------
def _arc_params(points):
    """Each vertex's place along the polyline, as a fraction of its total length."""
    run = [0.0]
    for p, q in zip(points, points[1:]):
        run.append(run[-1] + math.hypot(q[0] - p[0], q[1] - p[1]))
    total = run[-1]
    return [d / total for d in run]


def _point_at(points, params, at):
    """The point at normalised arc length ``at`` along a polyline."""
    if at <= params[0]:
        return points[0]
    for i in range(len(params) - 1):
        if at <= params[i + 1]:
            span = params[i + 1] - params[i]
            f = 0.0 if span <= 0.0 else (at - params[i]) / span
            (z0, y0), (z1, y1) = points[i], points[i + 1]
            return (z0 + (z1 - z0) * f, y0 + (y1 - y0) * f)
    return points[-1]


def _blend_stations():
    """The stations both profiles are resampled onto, and each profile resampled onto them.

    The union of the two profiles' own arc-length positions. Every W vertex and every thrie vertex
    is therefore still exactly on its own curve at its own end of the transition -- which is what
    makes the transition meet a W run and a thrie run without a step -- while the stations in
    between let the corrugations slide into each other.

    Stations closer together than ``tol`` are merged: a sliver ring a hundredth of a unit tall
    would be a near-degenerate quad, and the model audit reads slivers as coplanar overlap.
    """
    tol = 2e-3
    w_params = _arc_params(W_FRONT)
    thrie_params = _arc_params(THRIE_FRONT)
    merged = []
    for s in sorted(w_params + thrie_params):
        if not merged or s - merged[-1] > tol:
            merged.append(s)
    merged[0], merged[-1] = 0.0, 1.0
    return (merged,
            [_point_at(W_FRONT, w_params, s) for s in merged],
            [_point_at(THRIE_FRONT, thrie_params, s) for s in merged])


BLEND_STATIONS, W_RESAMPLED, THRIE_RESAMPLED = _blend_stations()


def blended_front(b):
    """The transition's front polyline at blend ``b``: 0 is pure W-beam, 1 pure thrie."""
    b = max(0.0, min(1.0, b))
    return [(wz + (tz - wz) * b, wy + (ty - wy) * b)
            for (wz, wy), (tz, ty) in zip(W_RESAMPLED, THRIE_RESAMPLED)]


def blended_extent(b):
    """(bottom, top) of the blended section -- what the block-out has to span."""
    front = blended_front(b)
    return (front[0][1], front[-1][1])


# --- the rail as a swept solid ---------------------------------------------------------------------
def closed_loop(front, mirror):
    """Close a front polyline against the rail's own back, and wind it counter-clockwise.

    A closed section sweeps into a SOLID, which is what makes a cut end a plain face rather than a
    hole into a shell. ``mirror`` reflects it about the post's centre line for the far rail of a
    double-sided run; the re-wind afterwards is what keeps the outward-normal rule below valid on
    both sides, since reflecting reverses the traversal.
    """
    pts = list(front)
    pts.append((geo.RAIL_BACK_Z, front[-1][1]))
    pts.append((geo.RAIL_BACK_Z, front[0][1]))
    if mirror:
        pts = [(geo.CELL - z, y) for (z, y) in pts]
    return ccw(pts)


def sweep(mesh, x0, x1, front_at, lift_at, mirror=False, rings=2):
    """Sweep the closed section from ``x0`` to ``x1``, with the section free to change as it goes.

    ``gen_guardrails.sweep_rail`` can name each quad's normal analytically because its section
    never changes; here the section is a function of x, so the normal is taken from the quad's own
    two edges. ``cross(along the run, along the section)`` points OUT for a counter-clockwise
    section swept toward +x, which is why ``closed_loop`` re-winds after mirroring.
    """
    t = uv_swatch(RAIL_SWATCH)
    frames = []
    for i in range(rings):
        x = x0 + (x1 - x0) * (i / (rings - 1.0))
        frames.append((x, lift_at(x), closed_loop(front_at(x), mirror)))

    for (xa, la, a), (xb, lb, b) in zip(frames, frames[1:]):
        for i in range(len(a)):
            j = (i + 1) % len(a)
            a0 = (xa, a[i][1] + la, a[i][0])
            a1 = (xa, a[j][1] + la, a[j][0])
            b0 = (xb, b[i][1] + lb, b[i][0])
            b1 = (xb, b[j][1] + lb, b[j][0])
            normal = vcross(vsub(b0, a0), vsub(a1, a0))
            if abs(normal[0]) + abs(normal[1]) + abs(normal[2]) < 1e-9:
                continue
            mesh.quad_out([a0, b0, b1, a1], normal, [t, t, t, t])


def cap(mesh, front, x, lift, outward, mirror=False):
    """The plain cut over one open end of the rail.

    A strip of trapezoids between the pressed face and the rail's back rather than a fan: the
    section is not convex -- that is what a valley at mid height means -- and a fan across a
    concave section puts triangles outside the shape and on top of each other, which is a coplanar
    overlap and an audit failure.
    """
    t = uv_swatch(RAIL_SWATCH)
    normal = (outward, 0.0, 0.0)

    def zz(z):
        return geo.CELL - z if mirror else z

    back = zz(geo.RAIL_BACK_Z)
    for (z0, y0), (z1, y1) in zip(front, front[1:]):
        if abs(y1 - y0) < 1e-9:
            continue
        mesh.quad_out([(x, y0 + lift, zz(z0)), (x, y1 + lift, zz(z1)),
                       (x, y1 + lift, back), (x, y0 + lift, back)],
                      normal, [t, t, t, t])


def rail_sides(double):
    """Which rails a variant carries: the near one, and on a double-sided run the far one too."""
    return (False, True) if double else (False,)


# --- the post and its block-out ---------------------------------------------------------------------
def build_post(mesh, wood, double, slope, bottom_y, top_y):
    """Post plus block-out, sized to whatever rail passes the middle of the cell.

    Exactly ``gen_guardrails.build_post`` with the rail's extent handed in rather than read off the
    W-beam constants, because the thrie rail is deeper at BOTH ends: a block-out sized for a W
    would leave the outer corrugations bolted to thin air, and a post that stopped at the W's top
    would disappear behind the rail it is supposed to stand proud of.

    On a slope it is the W-beam's rule unchanged: a ramp is always drawn in the lower of its two
    cells, so the rail is above the cell at mid span and the post reaches up to it from the floor.
    """
    lift = geo.slope_lift(slope, geo.POST_LIFT)
    foot = min(geo.POST_BOTTOM_Y, lift)
    half = geo.BLOCKOUT_HALF_X
    y0 = bottom_y - BLOCKOUT_MARGIN + lift
    y1 = top_y - BLOCKOUT_TUCK + lift

    # The block-out's back is buried in the post's front, so that face is left off rather than
    # drawn inside it. Its front is only mostly covered by the rail -- the block-out stands a
    # little proud top and bottom -- so that one stays.
    box(mesh, CX - half, CX + half, y0, y1, geo.BLOCKOUT_Z0, geo.BLOCKOUT_Z1, BLOCKOUT_SWATCH,
        faces=("x-", "x+", "y-", "y+", "z-"))
    if double:
        back_faces = ["x-", "x+", "y-", "y+", "z+"]
        if not wood:
            # A C-section is open at the back, so nothing covers the far block-out's inner face.
            back_faces.append("z-")
        box(mesh, CX - half, CX + half, y0, y1,
            geo.BACK_BLOCKOUT_Z0, geo.BACK_BLOCKOUT_Z1, BLOCKOUT_SWATCH, faces=tuple(back_faces))

    top = top_y + POST_PROUD + lift
    if wood:
        # Sawn timber: a plain square section, which is what a wooden post is.
        box(mesh, CX - geo.POST_HALF_X, CX + geo.POST_HALF_X, foot, top,
            geo.POST_Z0, geo.POST_Z1, POST_SWATCH)
        return

    # Rolled steel: a C-section, web toward the rail, flanges reaching back. Three solids that
    # TOUCH rather than overlap, and the flanges leave off the face buried in the web.
    web_z1 = geo.POST_Z0 + POST_WALL
    box(mesh, CX - geo.POST_HALF_X, CX + geo.POST_HALF_X, foot, top,
        geo.POST_Z0, web_z1, POST_SWATCH)
    for sign in (-1.0, 1.0):
        outer = CX + sign * geo.POST_HALF_X
        inner = outer - sign * POST_WALL
        box(mesh, min(outer, inner), max(outer, inner), foot, top,
            web_z1, geo.POST_Z1, POST_SWATCH, faces=("x-", "x+", "y-", "y+", "z+"))


# --- the thrie-beam rail blocks -----------------------------------------------------------------------
def _thrie_front(_x):
    return THRIE_FRONT


def _lift_along(slope):
    return lambda x: geo.slope_lift(slope, x / geo.CELL)


def thrie_core(mesh, double, slope):
    lift_at = _lift_along(slope)
    for mirror in rail_sides(double):
        sweep(mesh, 0.0, geo.CELL, _thrie_front, lift_at, mirror)


def thrie_end(mesh, double, slope, left):
    """The cap over an open end. Only the right-hand cap needs one model per slope: the left-hand
    end is off the floor only on ``down``, whose left end is always joined."""
    lift = geo.slope_lift(slope, 0.0 if left else 1.0)
    x = 0.0 if left else geo.CELL
    for mirror in rail_sides(double):
        cap(mesh, THRIE_FRONT, x, lift, -1.0 if left else 1.0, mirror)


def thrie_fill(mesh, double, slope):
    """The length of rail closing the gap to a DIAGONAL neighbour, off the right-hand end.

    Level, at whatever height the core's right-hand end reached: all the rise happens inside a
    cell, so there is none left to make across the joint.
    """
    lift = geo.slope_lift(slope, 1.0)
    for mirror in rail_sides(double):
        sweep(mesh, geo.CELL, geo.CELL + geo.DIAGONAL_GAP, _thrie_front, lambda _x: lift, mirror)


def thrie_post(mesh, wood, double, slope):
    build_post(mesh, wood, double, slope, geo.THRIE_RAIL_BOTTOM_Y, geo.THRIE_RAIL_TOP_Y)


def thrie_inventory(mesh, wood, double):
    """Everything but the filler: what one segment looks like standing on its own.

    The filler is left out because the bounding box is taken from these bounds, and a filler in the
    bounds gives a lone rail a box reaching into the cell beside it.
    """
    thrie_core(mesh, double, "flat")
    thrie_post(mesh, wood, double, "flat")
    thrie_end(mesh, double, "flat", True)
    thrie_end(mesh, double, "flat", False)


def thrie_pieces(spec):
    """suffix -> builder, for every model one thrie rail block ships."""
    wood, double = spec["wood"], spec["double"]
    pieces = {
        "core": lambda m: thrie_core(m, double, "flat"),
        "end_left": lambda m: thrie_end(m, double, "flat", True),
    }
    for slope in geo.SLOPES:
        if slope != "flat":
            pieces["core_%s" % slope] = (lambda s: lambda m: thrie_core(m, double, s))(slope)
        pieces["post_%s" % slope] = (lambda s: lambda m: thrie_post(m, wood, double, s))(slope)
        pieces["fill_%s" % slope] = (lambda s: lambda m: thrie_fill(m, double, s))(slope)
        pieces["end_right_%s" % slope] = (
            (lambda s: lambda m: thrie_end(m, double, s, False))(slope))
    return pieces


# --- the transition ------------------------------------------------------------------------------------
def _transition_front(hand):
    """Front polyline as a function of x, for one hand.

    ``hand`` false runs W-beam at x = 0 to thrie at x = 16, which is the piece a run meets going
    left to right. ``hand`` true runs the blend the other way -- and *only* the blend: the rail
    keeps the cell's own slope, because ``slope`` is a property of the run, not of the piece's
    chirality.
    """
    if hand:
        return lambda x: blended_front(1.0 - x / geo.CELL)
    return lambda x: blended_front(x / geo.CELL)


def _transition_edge_front(hand, left):
    """The polyline at one end of the cell: W at the W end, thrie at the thrie end."""
    b = 0.0 if left != hand else 1.0
    return blended_front(b)


def transition_core(mesh, slope, hand):
    sweep(mesh, 0.0, geo.CELL, _transition_front(hand), _lift_along(slope),
          rings=TRANSITION_RINGS)


def transition_end(mesh, slope, hand, left):
    lift = geo.slope_lift(slope, 0.0 if left else 1.0)
    x = 0.0 if left else geo.CELL
    cap(mesh, _transition_edge_front(hand, left), x, lift, -1.0 if left else 1.0)


def transition_fill(mesh, slope, hand):
    """The diagonal filler, off the RIGHT-hand end in both hands.

    ``diagfill`` names the world side the filler grows on, so the reflection does not carry it to
    the left -- it stays here and changes SECTION instead: a mirrored transition presents its
    W-beam end to the right, so its filler is a length of W-beam.
    """
    lift = geo.slope_lift(slope, 1.0)
    front = _transition_edge_front(hand, False)
    sweep(mesh, geo.CELL, geo.CELL + geo.DIAGONAL_GAP, lambda _x: front, lambda _x: lift)


def transition_post(mesh, slope):
    """The post, which is the same part in both hands.

    Mid-cell the blend is halfway whichever way it runs, so the rail's extent -- and therefore the
    block-out's -- is identical; and the post is symmetric about the cell's centre line, which is
    the very plane the mirror reflects about.
    """
    bottom, top = blended_extent(0.5)
    build_post(mesh, False, False, slope, bottom, top)


def transition_inventory(mesh):
    transition_core(mesh, "flat", False)
    transition_post(mesh, "flat")
    transition_end(mesh, "flat", False, True)
    transition_end(mesh, "flat", False, False)


def transition_pieces():
    """suffix -> builder for the transition, both hands.

    The mirrored suffix is appended to the piece name it mirrors, so ``core_up_mirrored`` sits
    beside ``core_up``. The post has no mirrored twin on purpose -- see ``transition_post``.
    """
    pieces = {"core": lambda m: transition_core(m, "flat", False),
              "core_mirrored": lambda m: transition_core(m, "flat", True),
              "end_left": lambda m: transition_end(m, "flat", False, True),
              "end_left_mirrored": lambda m: transition_end(m, "flat", True, True)}
    for slope in geo.SLOPES:
        if slope != "flat":
            pieces["core_%s" % slope] = (
                (lambda s: lambda m: transition_core(m, s, False))(slope))
            pieces["core_%s_mirrored" % slope] = (
                (lambda s: lambda m: transition_core(m, s, True))(slope))
        pieces["post_%s" % slope] = (lambda s: lambda m: transition_post(m, s))(slope)
        for hand, tail in ((False, ""), (True, "_mirrored")):
            pieces["fill_%s%s" % (slope, tail)] = (
                (lambda s, h: lambda m: transition_fill(m, s, h))(slope, hand))
            pieces["end_right_%s%s" % (slope, tail)] = (
                (lambda s, h: lambda m: transition_end(m, s, h, False))(slope, hand))
    return pieces


def _verify_mirror_identity():
    """Assert that each mirrored core really is the reflection the end treatments use.

    The mirrored transition is built by running the blend the other way rather than by reflecting a
    finished mesh. A reflection about x = 8 also swaps ``up`` for ``down`` -- one runs 0 to +16,
    the other +16 to 0 -- so the identity is checked slope by slope against the OPPOSITE slope's
    unmirrored core. This is what stops the direct construction from quietly drifting away from
    ``mirror_mesh``, and what would catch the two slopes' lifts no longer being reflections.
    """
    opposite = {"flat": "flat", "up": "down", "down": "up"}

    def key(mesh):
        return sorted(tuple(round(c, 5) for c in p) for p in mesh.v)

    for slope in geo.SLOPES:
        direct = Mesh()
        transition_core(direct, slope, True)
        reflected = Mesh()
        transition_core(reflected, opposite[slope], False)
        mirror_mesh(reflected)
        assert key(direct) == key(reflected), (
            "the mirrored %s transition core is no longer the reflection of the unmirrored %s one "
            "about x = %.1f; one of the two constructions has drifted"
            % (slope, opposite[slope], geo.CELL * 0.5))


# --- the thrie end treatments -----------------------------------------------------------------------
def thrie_section(flatten=0.0, hscale=1.0):
    """The thrie rail's front skin as (offset back from the face, height above the rail's bottom).

    The shape ``gen_guardrail_ends.w_section`` yields, off the thrie profile. ``flatten`` presses
    the corrugation out toward the mean offset and ``hscale`` closes the section down about its own
    middle; together they turn a running rail into the blunt closed nose of an end shoe without a
    second profile having to be written down.
    """
    offs = [o for (o, _y) in geo.THRIE_PROFILE]
    mid_o = 0.5 * (min(offs) + max(offs))
    for (o, y) in geo.THRIE_PROFILE:
        h = y - geo.THRIE_RAIL_BOTTOM_Y
        yield (o + (mid_o - o) * flatten,
               0.5 * THRIE_HEIGHT + (h - 0.5 * THRIE_HEIGHT) * hscale)


def thrie_rail_post(mesh, origin, swatch_v=SWATCH_BASE_V):
    """Block-out and post behind the rail at one point on its path, standing on the floor.

    ``gen_guardrail_ends.rail_post`` sized for the deeper rail. ``origin`` is the point on the
    rail's face, so everything is placed relative to it and follows the rail wherever it has flared
    or dropped to.
    """
    x, y, z = origin
    dz = z - geo.THRIE_RAIL_FRONT_Z
    dy = y - geo.THRIE_RAIL_BOTTOM_Y
    box(mesh, x - geo.BLOCKOUT_HALF_X, x + geo.BLOCKOUT_HALF_X,
        geo.THRIE_RAIL_BOTTOM_Y - BLOCKOUT_MARGIN + dy,
        geo.THRIE_RAIL_TOP_Y - BLOCKOUT_TUCK + dy,
        geo.BLOCKOUT_Z0 + dz, geo.BLOCKOUT_Z1 + dz, swatch_v)
    box(mesh, x - geo.POST_HALF_X, x + geo.POST_HALF_X,
        geo.POST_BOTTOM_Y, geo.THRIE_RAIL_TOP_Y + POST_PROUD + dy,
        geo.POST_Z0 + dz - POST_BITE, geo.POST_Z1 + dz, swatch_v)


def thrie_flared_path(t):
    s = curve(t)
    return (FLARED_LEN * t,
            geo.THRIE_RAIL_BOTTOM_Y - FLARED_DROP * s ** 3,
            geo.THRIE_RAIL_FRONT_Z + FLARED_BACK * s * s)


def thrie_flared_section(t):
    if t <= FLARED_NOSE_AT:
        return thrie_section()
    k = (t - FLARED_NOSE_AT) / (1.0 - FLARED_NOSE_AT)
    k = k * k * (3.0 - 2.0 * k)
    return thrie_section(flatten=FLARED_NOSE_FLATTEN * k,
                         hscale=1.0 + (FLARED_NOSE_HSCALE - 1.0) * k)


def build_flared_thrie(mesh):
    stations = rail_stations(thrie_flared_path, thrie_flared_section, 22)
    sweep_end_rail(mesh, stations)
    for at in FLARED_POSTS:
        thrie_rail_post(mesh, thrie_flared_path(at))


def _turndown_hscale(t):
    """How far the section has closed down at ``t``. See ``THRIE_TURN_FOLD_AT``."""
    if t <= THRIE_TURN_FOLD_AT:
        return 1.0
    k = (t - THRIE_TURN_FOLD_AT) / (1.0 - THRIE_TURN_FOLD_AT)
    k = k * k * (3.0 - 2.0 * k)
    return 1.0 + (THRIE_TURN_HSCALE - 1.0) * k


def thrie_turndown_path(t):
    """The rail's path, offset so the section's BOTTOM lands where the turndown wants it.

    ``rail_stations`` places the section relative to the path point, and a section that has closed
    down about its own middle no longer starts at that point. Without this correction the fold
    would lift the rail off the anchor it is bolted to, which is the one thing a turndown must not
    do.
    """
    s = curve(t)
    bottom = geo.THRIE_RAIL_BOTTOM_Y - (geo.THRIE_RAIL_BOTTOM_Y - TURN_END_Y) * s ** TURN_POW
    closed = 0.5 * THRIE_HEIGHT * (1.0 - _turndown_hscale(t))
    return (TURN_LEN * t, bottom - closed, geo.THRIE_RAIL_FRONT_Z)


def thrie_turndown_section(t):
    return thrie_section(hscale=_turndown_hscale(t))


def build_turndown_thrie(mesh):
    stations = rail_stations(thrie_turndown_path, thrie_turndown_section, 20)
    sweep_end_rail(mesh, stations)
    thrie_rail_post(mesh, thrie_turndown_path(TURN_POST_AT))
    box(mesh, *TURN_ANCHOR, SWATCH_DARK_V)


# --- the catalogues ---------------------------------------------------------------------------------
RAILS = {
    geo.THRIE_RAIL_BLOCKS[0]: {"stem": "guardrail_thrie_beam", "texture": TEXTURE_STEEL,
                               "wood": False, "double": False,
                               "display": "Thrie-Beam Guardrail"},
    geo.THRIE_RAIL_BLOCKS[1]: {"stem": "guardrail_thrie_beam_wood", "texture": TEXTURE_TIMBER,
                               "wood": True, "double": False,
                               "display": "Thrie-Beam Guardrail (Wood Post)"},
    geo.THRIE_RAIL_BLOCKS[2]: {"stem": "guardrail_thrie_beam_double", "texture": TEXTURE_STEEL,
                               "wood": False, "double": True,
                               "display": "Thrie-Beam Guardrail (Double Sided)"},
    geo.THRIE_RAIL_BLOCKS[3]: {"stem": "guardrail_thrie_beam_wood_double",
                               "texture": TEXTURE_TIMBER, "wood": True, "double": True,
                               "display": "Thrie-Beam Guardrail (Wood Post, Double Sided)"},
}

TRANSITION = {
    "stem": "guardrail_w_beam_thrie_transition",
    "texture": TEXTURE_STEEL,
    "display": "W-Beam to Thrie-Beam Transition",
}

ENDS = {
    geo.THRIE_END_BLOCKS[0]: {"model": "guardrail_end_flared_thrie", "build": build_flared_thrie,
                              "texture": TEXTURE_STEEL,
                              "display": "Thrie-Beam Guardrail End (Flared)"},
    geo.THRIE_END_BLOCKS[1]: {"model": "guardrail_end_turndown_thrie",
                              "build": build_turndown_thrie, "texture": TEXTURE_STEEL,
                              "display": "Thrie-Beam Guardrail End (Turndown)"},
}

TEXTURES = {
    TEXTURE_STEEL: lambda: guardrail_texture(geo.GALVANISED_DARK, geo.GALVANISED_DARK),
    TEXTURE_TIMBER: lambda: guardrail_texture(geo.TIMBER, geo.TIMBER_DARK),
}


# --- writers ------------------------------------------------------------------------------------------
def write_model(mesh, path, name, mtl_file):
    """Write one OBJ, with the attribution pointing at THIS generator.

    ``Mesh.write`` stamps the work zone script's name into the header, which is where the class
    lives; a model that says it came from a generator that does not build it sends the next person
    to the wrong file.
    """
    check_uvs(mesh, name)
    mesh.write(path, name, mtl_file)
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    with open(path, "w", newline="\n", encoding="utf-8") as fh:
        fh.write(text.replace("gen_work_zone_devices.py", GENERATOR, 1))


def write_mtl(path, texture_name):
    with open(path, "w", newline="\n") as fh:
        fh.write("# Procedurally generated by dev-env-utils/scripts/%s -- do not hand edit\n"
                 % GENERATOR)
        fh.write("# One material; the steel and timber variants retexture #%s.\n" % geo.MATERIAL)
        fh.write("newmtl %s\n" % geo.MATERIAL)
        fh.write("map_Kd %s/%s\n" % (geo.TEXTURE_PREFIX, texture_name))


def _piece(model, texture, suffix):
    return {"model": "%s_%s.obj" % (model, suffix),
            "custom": {"flip-v": True},
            "textures": {"#%s" % geo.MATERIAL: texture}}


def rail_blockstate(spec):
    """The Forge blockstate for one thrie rail block -- ``gen_guardrails``' arrangement unchanged.

    Forge merges one property's variant at a time, alphabetically, and the first to set a value
    keeps it. ``slope`` sorts LAST of the six, so it carries the post, the right-hand cap and the
    filler each in the shape that slope needs; ``post``, ``connectright`` and ``diagfill`` sort
    before it and declare their submodel key as ``null`` on the side where the piece must NOT be
    drawn, which occupies the key so ``slope``'s entry is never merged in.
    """
    model = "%s/%s" % (MODEL_PREFIX, spec["stem"])
    texture = "%s/%s" % (geo.TEXTURE_PREFIX, spec["texture"])

    def submodel(**pieces):
        return {"submodel": {key: _piece(model, texture, suffix)
                             for key, suffix in pieces.items()}}

    slope_variants = {}
    for slope in geo.SLOPES:
        # `flat` inherits the core model from the defaults; the other two replace it.
        variant = {} if slope == "flat" else {"model": "%s_core_%s.obj" % (model, slope)}
        variant.update(submodel(post="post_%s" % slope, fill="fill_%s" % slope,
                                end_right="end_right_%s" % slope))
        slope_variants[slope] = variant

    variants = {"facing": facing_variants({"diagonal": True})}
    # The end caps are on the FALSE side: a piece is drawn where nothing connects.
    variants["connectleft"] = {"false": submodel(end_left="end_left"), "true": {}}
    variants["connectright"] = {"false": {}, "true": {"submodel": {"end_right": None}}}
    # The post and the diagonal filler are the other way round: drawn where the property is true.
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


def transition_blockstate(spec):
    """The Forge blockstate for the transition: the rails' trick with ``mirrored`` folded in.

    Seven properties, which sort ``connectleft, connectright, diagfill, facing, mirrored, post,
    slope``. ``slope`` is still last and still supplies every actual shape; everything before it
    only ever NULLS a key it owns, and the earliest property to touch a key wins.

    The one new move is the base model. The rail answers to ``slope`` AND ``mirrored``, and only
    one of them can set ``model``. ``mirrored=true`` therefore sets ``"model": null`` -- which
    ``ForgeBlockStateV1``'s deserializer treats as a deliberate removal ("Allow overriding base
    model to remove it from a state"), marking it set so neither ``slope``'s per-slope model nor
    the default can fill it back in -- and the mirrored rail arrives instead as the ``core_m``
    submodel, which ``slope`` supplies and ``mirrored=false`` nulls. The unmirrored hand is left
    structurally identical to a plain rail.

    ``end_left``, ``end_right`` and ``fill`` each get a key per hand for the same reason: they
    answer to three properties at once (a connect flag, ``mirrored``, and ``slope``), and two keys
    is what turns that back into two independent blockers.

    The ``mirrored`` property has to exist on the block for any of this to match a real state --
    ``BlockGuardrailTransition`` carries it, resolved from the run in ``getActualState`` rather
    than stored, since the meta is already full. ``true`` there means the run reads right to left
    through the piece, so the THRIE end is the one on the left; that is the hand this file calls
    mirrored.
    """
    model = "%s/%s" % (MODEL_PREFIX, spec["stem"])
    texture = "%s/%s" % (geo.TEXTURE_PREFIX, spec["texture"])

    def submodel(**pieces):
        return {key: _piece(model, texture, suffix) for key, suffix in pieces.items()}

    slope_variants = {}
    for slope in geo.SLOPES:
        variant = {} if slope == "flat" else {"model": "%s_core_%s.obj" % (model, slope)}
        mirrored_core = "core_mirrored" if slope == "flat" else "core_%s_mirrored" % slope
        variant["submodel"] = submodel(
            core_m=mirrored_core,
            post="post_%s" % slope,
            end_left_u="end_left", end_left_m="end_left_mirrored",
            end_right_u="end_right_%s" % slope,
            end_right_m="end_right_%s_mirrored" % slope,
            fill_u="fill_%s" % slope, fill_m="fill_%s_mirrored" % slope)
        slope_variants[slope] = variant

    variants = {"facing": facing_variants({"diagonal": True})}
    variants["connectleft"] = {"false": {},
                               "true": {"submodel": {"end_left_u": None, "end_left_m": None}}}
    variants["connectright"] = {"false": {},
                                "true": {"submodel": {"end_right_u": None, "end_right_m": None}}}
    variants["diagfill"] = {"false": {"submodel": {"fill_u": None, "fill_m": None}}, "true": {}}
    # The unmirrored rail is the base model, so the mirrored branch has to remove it; the mirrored
    # rail is a submodel, so the unmirrored branch nulls that instead.
    variants["mirrored"] = {
        "false": {"submodel": {"core_m": None, "end_left_m": None,
                               "end_right_m": None, "fill_m": None}},
        "true": {"model": None,
                 "submodel": {"end_left_u": None, "end_right_u": None, "fill_u": None}},
    }
    variants["post"] = {"false": {"submodel": {"post": None}}, "true": {}}
    variants["slope"] = slope_variants
    variants["normal"] = [{}]
    # The icon keeps the unmirrored hand: which way round the piece will land is not known while
    # it is still in a slot.
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


def end_blockstate(spec):
    """The Forge blockstate for one thrie end treatment.

    Deliberately thin, exactly as the W-beam ends are: ``facing`` from the shared eight-way table
    plus ``mirrored``, and nothing else. ``mirrored`` is not something the player sets -- the block
    works out which side of the run it is on and reports it from ``getActualState``.
    """
    model = "csm:%s/%s" % (geo.MODEL_SUBDIR, spec["model"])
    return {
        "forge_marker": 1,
        "defaults": {
            "model": model + ".obj",
            "custom": {"flip-v": True},
            "textures": {"#%s" % geo.MATERIAL: "%s/%s" % (geo.TEXTURE_PREFIX, spec["texture"])},
        },
        "variants": {
            "facing": facing_variants({"diagonal": True}),
            "mirrored": {"false": {}, "true": {"model": model + "_mirrored.obj"}},
            "normal": [{}],
            "inventory": [{"transform": "forge:default-block"}],
        },
    }


def cell_bounds(bounds):
    """Mesh bounds clipped to this block's own cell.

    An end treatment that reaches past its cell must not claim the space in its bounding box: an
    AABB bigger than the block steals the player's clicks from whatever is next door, and nothing
    in the log says so.
    """
    lo = tuple(max(0.0, v) for v in bounds[:3])
    hi = tuple(min(geo.CELL, v) for v in bounds[3:])
    return lo + hi


# --- generation -----------------------------------------------------------------------------------------
def generate(model_dir, texture_dir, blockstate_dir, fragment_dir):
    for d in (model_dir, texture_dir, blockstate_dir, fragment_dir):
        os.makedirs(d, exist_ok=True)
    set_v_span(geo.CELL)
    _verify_mirror_identity()
    written = []
    rail_bounds = {}
    end_bounds = {}

    for texture, build in TEXTURES.items():
        path = os.path.join(texture_dir, texture + ".png")
        build().save(path)
        written.append(path)

    def emit_block(stem, texture, pieces, inventory, registry, blockstate, bounds_into):
        mtl_file = stem + ".mtl"
        for suffix, builder in pieces.items():
            mesh = Mesh()
            builder(mesh)
            name = "%s_%s" % (stem, suffix)
            path = os.path.join(model_dir, name + ".obj")
            write_model(mesh, path, name, mtl_file)
            written.append(path)

        inv = Mesh()
        inventory(inv)
        inv_path = os.path.join(model_dir, stem + "_inv.obj")
        write_model(inv, inv_path, stem + "_inv", mtl_file)
        written.append(inv_path)
        bounds_into[registry] = mesh_bounds(inv)

        mtl_path = os.path.join(model_dir, mtl_file)
        write_mtl(mtl_path, texture)
        written.append(mtl_path)

        bs_path = os.path.join(blockstate_dir, registry + ".json")
        with open(bs_path, "w", newline="\n") as fh:
            json.dump(blockstate, fh, indent=2)
            fh.write("\n")
        written.append(bs_path)

    for registry, spec in RAILS.items():
        emit_block(spec["stem"], spec["texture"], thrie_pieces(spec),
                   (lambda s: lambda m: thrie_inventory(m, s["wood"], s["double"]))(spec),
                   registry, rail_blockstate(spec), rail_bounds)

    emit_block(TRANSITION["stem"], TRANSITION["texture"], transition_pieces(),
               transition_inventory, geo.TRANSITION_BLOCK,
               transition_blockstate(TRANSITION), rail_bounds)

    for registry, spec in ENDS.items():
        mtl_file = spec["model"] + ".mtl"
        mesh = Mesh()
        spec["build"](mesh)
        path = os.path.join(model_dir, spec["model"] + ".obj")
        write_model(mesh, path, spec["model"], mtl_file)
        written.append(path)
        end_bounds[registry] = cell_bounds(mesh_bounds(mesh))

        # The far end of the run. Reflected from the same build, so the two ends of a run can never
        # be two different shapes -- and sharing the MTL means they can never be two textures.
        flipped = Mesh()
        spec["build"](flipped)
        mirror_mesh(flipped)
        name = spec["model"] + "_mirrored"
        mirror_path = os.path.join(model_dir, name + ".obj")
        write_model(flipped, mirror_path, name, mtl_file)
        written.append(mirror_path)

        mtl_path = os.path.join(model_dir, mtl_file)
        write_mtl(mtl_path, spec["texture"])
        written.append(mtl_path)

        bs_path = os.path.join(blockstate_dir, registry + ".json")
        with open(bs_path, "w", newline="\n") as fh:
            json.dump(end_blockstate(spec), fh, indent=2)
            fh.write("\n")
        written.append(bs_path)

    # Fragments, for pasting into the lang file and the tab: this generator never rewrites a file
    # it does not own.
    lang_path = os.path.join(fragment_dir, "thrie_lang.txt")
    with open(lang_path, "w", newline="\n") as fh:
        for registry, spec in RAILS.items():
            fh.write("tile.%s.name=%s\n" % (registry, spec["display"]))
        fh.write("tile.%s.name=%s\n" % (geo.TRANSITION_BLOCK, TRANSITION["display"]))
        for registry, spec in ENDS.items():
            fh.write("tile.%s.name=%s\n" % (registry, spec["display"]))
    written.append(lang_path)

    tab_path = os.path.join(fragment_dir, "thrie_tab.java")
    with open(tab_path, "w", newline="\n") as fh:
        fh.write("// Generated by %s -- the rails' and the transition's bounding boxes are the\n"
                 "// INVENTORY model's own extent, so they follow the geometry and leave the\n"
                 "// diagonal filler out: a lone rail must not claim the cell beside it. The end\n"
                 "// treatments' are their models' extents clipped to the cell.\n"
                 "//\n"
                 "// The BOXES are all this file can supply. The registration in the tab carries a\n"
                 "// further argument these lines do not: the RAIL KIND a run joins on, which no\n"
                 "// generator can know -- and the transition takes a class of its own,\n"
                 "// BlockGuardrailTransition, with a kind at each end, because its blockstate\n"
                 "// carries a `mirrored` property that only that class resolves. Paste the boxes,\n"
                 "// not the whole line, over a registration that already has them.\n"
                 % GENERATOR)
        for registry in RAILS:
            fh.write('initTabBlock(new %s("%s",\n    %s));\n'
                     % (RAIL_CLASS, registry, java_bbox(rail_bounds[registry])))
        fh.write('initTabBlock(new %s("%s",\n    %s));\n'
                 % (RAIL_CLASS, geo.TRANSITION_BLOCK, java_bbox(rail_bounds[geo.TRANSITION_BLOCK])))
        for registry in ENDS:
            fh.write('initTabBlock(new %s("%s",\n    %s));\n'
                     % (END_CLASS, registry, java_bbox(end_bounds[registry])))
    written.append(tab_path)

    return written


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--scratch", action="store_true",
                        help="write models, textures and blockstates into _guardrail_out/ instead")
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
