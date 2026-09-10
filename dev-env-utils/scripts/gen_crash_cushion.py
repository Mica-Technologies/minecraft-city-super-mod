#!/usr/bin/env python3
"""
gen_crash_cushion.py -- the telescoping steel crash cushion: a NOSE block and a BAY block.

A crash cushion is the impact attenuator at the end of a barrier or in a gore point. This is the
telescoping-bay type: a rigid frame carrying overlapping steel side panels that collapse into each
other on impact, with a tapered nose at the front and a backup that bolts to whatever the cushion
protects.

    crash_cushion_nose    the front cell: the side panels taper in to a striped nose plate
    crash_cushion_bay     one full-width body cell; lay as many as the site wants

WHY TWO BLOCKS AND NOT ONE
--------------------------
A real cushion is twenty to thirty feet long. One block would be a metre, which reads as a toy
beside the guardrail it terminates, and it could not be lengthened for a wide gore. So the cushion
is laid the way a guardrail run is: a nose, then bays, joining themselves and then joining the run.

WHAT IT JOINS
-------------
It joins on the RAIL, through the same ``ICsmGuardrailRail`` contract every guardrail block uses --
see ``GuardrailJoins``. The arrangement is deliberately one-way:

    nose --> bay --> bay --> any rail

A bay presents ``crash_cushion`` at both its ends, accepts only ``crash_cushion`` on its LEFT, and
accepts any rail at all on its RIGHT. That is what lets one cushion terminate a W-beam, a thrie
beam, a box beam or a cable run without four different backups, and what stops a run reading into
the nose from the wrong side -- the nose is the impact face, and nothing may join to it.

WHERE THE NUMBERS COME FROM
---------------------------
``guardrail_geometry`` again, and for one reason that matters: ``CUSHION_Z0`` is the guardrail's
own ``RAIL_FRONT_Z``. The cushion's traffic-side panel and the rail face it terminates are the same
plane, so the two read as one continuous line rather than a cushion parked beside a rail.

THE FRAME
---------
Same frame as every other guardrail block: the run goes along X and spans the cell, the traffic
side is LOW Z, and a cell's right-hand neighbour is at +x. The nose therefore sits at the LEFT end
of a cushion and the impact face points toward -x.

Usage:
    python gen_crash_cushion.py                 # writes into the repo tree
    python gen_crash_cushion.py --scratch       # writes into _guardrail_out/ instead

Requires Pillow.
"""
import argparse
import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout              # noqa: E402
import guardrail_geometry as geo         # noqa: E402
from gen_work_zone_devices import (      # noqa: E402
    Mesh, box, prism, check_uvs, diagonal_stripe_image, draw_swatches, facing_variants,
    java_bbox, mesh_bounds, set_v_span, uv_swatch,
    SWATCH_BAND_V, SWATCH_BASE_V, SWATCH_DARK_V, SWATCH_U0, STRIPE_TEX_SIZE, TEX_SIZE,
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
JAVA_CLASS = "BlockCrashCushion"

CELL = geo.CELL

# --- the cushion's own shape ------------------------------------------------------------------------
#: Traffic-side panel face. The guardrail's own rail face, so a cushion and the run it terminates
#: are one line rather than two parallel ones.
CUSHION_Z0 = geo.RAIL_FRONT_Z
CUSHION_WIDTH = 7.20
CUSHION_Z1 = CUSHION_Z0 + CUSHION_WIDTH
PANEL_THICK = 0.60
#: Bottom of the side panels. Below this is the frame the bays slide on, which is what makes a
#: telescoping cushion read as machinery rather than as a box.
PANEL_SILL_Y = 1.60
PANEL_TOP_Y = 13.20

#: The batten at each cell's left-hand end: the lap where one bay's panel slides over the next.
#: Standing proud is the whole point -- flush, it would be an invisible line on a flat wall.
#:
#: ``LAP_BITE`` and ``LAP_INSET`` are the two numbers that keep it out of trouble. The batten's
#: far face is sunk INTO the panel rather than stopped on its far surface, and its top and bottom
#: are held clear of the panel's, because a batten sized exactly to the panel would share a plane
#: with it on three sides -- the coplanar overlap every generator in this batch has to dodge.
#: ``LAP_X0`` keeps it off the cell boundary for the same kind of reason.
LAP_X0 = 0.15
LAP_X1 = 1.25
LAP_PROUD = 0.35
LAP_BITE = 0.20
LAP_INSET = 0.25

#: The capping rail along the top of each side panel. ``CAP_BITE`` sinks its underside into the
#: panel instead of landing it on the panel's top face.
CAP_Y = 0.70
CAP_PROUD = 0.25
CAP_BITE = 0.30

#: The frame the bays slide along, on the ground down the cushion's centre line.
FRAME_Z_HALF = 1.80
FRAME_TOP_Y = 1.30
FRAME_CZ = (CUSHION_Z0 + CUSHION_Z1) * 0.5

#: The nose. Its panels taper from the full width at the back of its cell to this at the front.
#:
#: The plate is held back off the cell face rather than drawn on it, so that the frame's own cap
#: -- which does sit on the cell face, where nothing is ever seen against it -- and the plate do
#: not share a plane. The panels then start a little INSIDE the plate for the same reason.
NOSE_HALF = 1.50
NOSE_Z0 = FRAME_CZ - NOSE_HALF
NOSE_Z1 = FRAME_CZ + NOSE_HALF
NOSE_PLATE_X0 = 0.30
NOSE_PLATE_X1 = 1.00
NOSE_PLATE_BITE = 0.25
#: The plate runs from inside the frame to above the panels, so no face of it lands on a face of
#: either.
NOSE_PLATE_Y0 = 1.00
NOSE_PLATE_Y1 = PANEL_TOP_Y + 0.30

#: The backup plate closing an unjoined end, held clear of the panels top and bottom and sunk into
#: them at each side -- three planes it would otherwise share with them.
BACKUP_THICK = 0.60
BACKUP_INSET_Y = 0.25
BACKUP_BITE_Z = 0.20
#: How far back off the cell face the plate stands. The nose's panels are a solid with a real face
#: at the back of the cell, and a plate sunk into them and landing on that face would share it.
BACKUP_SETBACK = 0.15

TEXTURE_STEEL = "crash_cushion_steel"
TEXTURE_CHEVRON = "crash_cushion_chevron"

#: The nose plate's own size in world units. Every striped face samples the sheet in THESE units,
#: so a stripe stays at the angle it was drawn at wherever it lands -- the same rule
#: ``gen_guardrail_ends.py`` states for the impact head, and for the same reason: banding in sprite
#: space makes the angle a function of the face's aspect.
CHEVRON_W = NOSE_Z1 - NOSE_Z0
CHEVRON_H = NOSE_PLATE_Y1 - NOSE_PLATE_Y0
CHEVRON_STRIPE = 2.60

PANEL_SWATCH = SWATCH_BASE_V
LAP_SWATCH = SWATCH_BAND_V
FRAME_SWATCH = SWATCH_DARK_V


# --- primitives ---------------------------------------------------------------------------------
def sweep_box(mesh, z0, z1, y0, y1, x0, x1, lift, grade, swatch_v, caps=(False, False)):
    """A rectangular section swept along X, rising by ``grade`` per unit of run.

    The cushion is all constant cross-sections carried along the run -- side panels, capping rails,
    the frame -- so one sweep serves the lot, and every one of them follows a slope by the same
    rule the rails do rather than by a per-part special case.

    ``caps`` names which of the two open ends to close. A cell in the middle of a cushion closes
    neither: its neighbours do that, and two cells that both capped the joint would put two faces
    in one plane.
    """
    t = uv_swatch(swatch_v)
    q = [t, t, t, t]
    lift_b = lift + grade * (x1 - x0)
    section = [(z0, y0), (z1, y0), (z1, y1), (z0, y1)]
    for (a, b) in zip(section, section[1:] + section[:1]):
        dz, dy = b[0] - a[0], b[1] - a[1]
        normal = (grade * dz, -dz, dy)
        mesh.quad_out([(x0, a[1] + lift, a[0]), (x1, a[1] + lift_b, a[0]),
                       (x1, b[1] + lift_b, b[0]), (x0, b[1] + lift, b[0])], normal, q)
    for (want, x, l, sign) in ((caps[0], x0, lift, -1.0), (caps[1], x1, lift_b, 1.0)):
        if want:
            box(mesh, x, x, y0 + l, y1 + l, z0, z1, swatch_v,
                faces=("x-",) if sign < 0.0 else ("x+",))


def sheared_box(mesh, x0, x1, y0, y1, z0, z1, grade, swatch_v,
                faces=("x-", "x+", "y-", "y+", "z-", "z+")):
    """``box``, but leaning by ``grade`` along X, and able to leave a named face off.

    Needed for exactly one part -- the nose plate, whose front face is drawn separately because it
    is the only striped face on the model. Drawing it here as well would put two quads in one
    plane, and leaving the plate axis-aligned on a sloped nose would step it away from the panels
    it is bolted between.
    """
    t = uv_swatch(swatch_v)
    q = [t, t, t, t]
    a, b = grade * x0, grade * x1

    def corners(y_at_x0, y_at_x1):
        return ((x0, y_at_x0 + a, z0), (x1, y_at_x1 + b, z0),
                (x1, y_at_x1 + b, z1), (x0, y_at_x0 + a, z1))

    (lo00, lo10, lo11, lo01) = corners(y0, y0)
    (hi00, hi10, hi11, hi01) = corners(y1, y1)
    if "x-" in faces:
        mesh.quad_out([lo00, hi00, hi01, lo01], (-1, 0, 0), q)
    if "x+" in faces:
        mesh.quad_out([lo10, hi10, hi11, lo11], (1, 0, 0), q)
    # A face leaning by ``grade`` along X has its outward normal leaning the OTHER way: the
    # surface is spanned by (1, grade, 0) and (0, 0, 1), whose cross product is (grade, -1, 0).
    # The same expression ``sweep_box`` uses, and getting the sign backwards makes the normal
    # exactly perpendicular to the triangle, which the mesh rejects as degenerate rather than
    # silently drawing inside out.
    if "y-" in faces:
        mesh.quad_out([lo00, lo10, lo11, lo01], (grade, -1, 0), q)
    if "y+" in faces:
        mesh.quad_out([hi00, hi10, hi11, hi01], (-grade, 1, 0), q)
    if "z-" in faces:
        mesh.quad_out([lo00, lo10, hi10, hi00], (0, 0, -1), q)
    if "z+" in faces:
        mesh.quad_out([lo01, lo11, hi11, hi01], (0, 0, 1), q)


def panel_planes():
    """(low z, high z) of each side panel's own slab, traffic side first."""
    return ((CUSHION_Z0, CUSHION_Z0 + PANEL_THICK),
            (CUSHION_Z1 - PANEL_THICK, CUSHION_Z1))


def grade_of(slope):
    return geo.slope_lift(slope, 1.0) / CELL


# --- the bay ------------------------------------------------------------------------------------
def build_bay_shell(mesh, slope, x0=0.0, x1=CELL, lift=0.0):
    """The side panels and their capping rails: everything a cushion shows above the ground.

    Split out from the frame because the diagonal filler wants this and not that, the same way a
    rail's filler carries the rail and not the post. A ground frame bridged across a joint would
    be floating anyway on a sloped run, where the filler sits a whole cell higher than the frame
    it is supposed to continue.
    """
    grade = grade_of(slope)
    for (z0, z1) in panel_planes():
        sweep_box(mesh, z0, z1, PANEL_SILL_Y, PANEL_TOP_Y, x0, x1, lift, grade, PANEL_SWATCH)
        # The capping rail, standing proud on the outside so the panel reads as sheet under a rail
        # rather than as a slab with a thicker top.
        traffic_side = z0 == CUSHION_Z0
        cap_z0 = z0 - CAP_PROUD if traffic_side else z0 + CAP_BITE
        cap_z1 = z1 - CAP_BITE if traffic_side else z1 + CAP_PROUD
        sweep_box(mesh, cap_z0, cap_z1, PANEL_TOP_Y - CAP_BITE, PANEL_TOP_Y + CAP_Y,
                  x0, x1, lift, grade, LAP_SWATCH)


def build_frame(mesh, slope, caps=(False, False)):
    """The frame the bays slide along, on the ground down the cushion's centre line."""
    sweep_box(mesh, FRAME_CZ - FRAME_Z_HALF, FRAME_CZ + FRAME_Z_HALF, 0.0, FRAME_TOP_Y,
              0.0, CELL, 0.0, grade_of(slope), FRAME_SWATCH, caps=caps)


def build_bay_core(mesh, slope):
    build_bay_shell(mesh, slope)
    build_frame(mesh, slope)


def build_bay_lap(mesh, slope):
    """The lapped joint at this cell's left-hand end.

    Drawn as part of the CORE rather than as its own connect-driven piece: the lap is where this
    bay's panel slides over the next one's, so it belongs to the bay whether or not anything is
    joined to it -- a cushion whose laps appeared and vanished with its neighbours would be a
    different machine every time one was added.
    """
    grade = grade_of(slope)
    for (z0, z1) in panel_planes():
        traffic_side = z0 == CUSHION_Z0
        lap_z0 = z0 - LAP_PROUD if traffic_side else z0 + LAP_BITE
        lap_z1 = z1 - LAP_BITE if traffic_side else z1 + LAP_PROUD
        sweep_box(mesh, lap_z0, lap_z1, PANEL_SILL_Y + LAP_INSET, PANEL_TOP_Y - LAP_INSET,
                  LAP_X0, LAP_X1, grade * LAP_X0, grade, LAP_SWATCH, caps=(True, True))


def build_bay(mesh, slope):
    build_bay_core(mesh, slope)
    build_bay_lap(mesh, slope)


def build_bay_end(mesh, slope, left):
    """The plate closing an open end of the cushion.

    On the right-hand end this is the BACKUP: the wall the cushion pushes against, which in the
    field is bolted to the rail or the pier behind it. On the left it is the same plate closing a
    cushion that has been laid without a nose.
    """
    lift = 0.0 if left else geo.slope_lift(slope, 1.0)
    x = BACKUP_SETBACK if left else CELL - BACKUP_SETBACK
    x0, x1 = (x, x + BACKUP_THICK) if left else (x - BACKUP_THICK, x)
    (near_z0, near_z1), (far_z0, far_z1) = panel_planes()
    box(mesh, x0, x1, PANEL_SILL_Y + BACKUP_INSET_Y + lift, PANEL_TOP_Y - BACKUP_INSET_Y + lift,
        near_z1 - BACKUP_BITE_Z, far_z0 + BACKUP_BITE_Z, PANEL_SWATCH)
    # The frame's own cap at this end. It is here rather than in the core for the same reason the
    # rails put theirs here: a cap drawn at every cell boundary whether or not anything is joined
    # puts two plates in one plane at every joint, which z-fights between two blocks.
    cap_x = 0.0 if left else CELL
    box(mesh, cap_x, cap_x, lift, FRAME_TOP_Y + lift,
        FRAME_CZ - FRAME_Z_HALF, FRAME_CZ + FRAME_Z_HALF, FRAME_SWATCH,
        faces=("x-",) if left else ("x+",))


def build_bay_fill(mesh, slope):
    """The cushion's own continuation into the diagonal gap -- level, for the reason every rail
    generator in this batch gives: all of a slope's rise happens inside its own cell."""
    build_bay_shell(mesh, "flat", x0=CELL, x1=CELL + geo.DIAGONAL_GAP,
                    lift=geo.slope_lift(slope, 1.0))


def build_bay_inventory(mesh):
    build_bay(mesh, "flat")
    build_bay_end(mesh, "flat", False)


# --- the nose -----------------------------------------------------------------------------------
def taper_panel(mesh, nose_z0, nose_z1, back_z0, back_z1, grade):
    """One tapering side panel, as a solid between its plan outline at sill and cap height.

    ``prism`` rather than ``sweep_box``: this is the one part of the cushion whose section is not
    constant along the run, and a leaning slab is exactly what prism exists for.

    The taper is defined from the cell face to the back of the cell, but DRAWN from just inside
    the nose plate -- so the panel's own front face ends up buried in the plate rather than lying
    on its back face. The plan positions at that station are interpolated rather than restated,
    which is what keeps the taper one straight line all the way to the bay behind it.
    """
    start = NOSE_PLATE_X1 - NOSE_PLATE_BITE
    t = start / CELL

    def at(front, back):
        return front + (back - front) * t

    plan = [(start, at(nose_z0, back_z0)), (CELL, back_z0),
            (CELL, back_z1), (start, at(nose_z1, back_z1))]
    bottom = [(x, PANEL_SILL_Y + grade * x, z) for (x, z) in plan]
    top = [(x, PANEL_TOP_Y + grade * x, z) for (x, z) in plan]
    prism(mesh, bottom, top, PANEL_SWATCH)


def panel_uv(x_local, y_local):
    """UV into the chevron sheet, in the sheet's own world units. Same mapping
    ``gen_guardrail_ends.py`` uses for the impact head, for the same reason."""
    return ((x_local / CHEVRON_W) * SWATCH_U0, y_local / CHEVRON_H)


def striped_quad(mesh, pts, normal, extents):
    mesh.quad_out(list(pts), normal, [panel_uv(a, b) for (a, b) in extents])


def build_nose(mesh, slope):
    """The nose cell, sheared to whatever the cushion behind it is climbing.

    A cushion really does sit on a flat pad, so a sloped nose is not a thing anybody builds. It is
    drawn anyway because the joining machinery gives every cell a slope, taken from its right-hand
    neighbour -- and a nose that ignored it would leave a visible step exactly where it joins the
    first bay, which is the one part of a cushion a player is standing next to.
    """
    grade = grade_of(slope)
    (front_z0, front_z1), (back_z0, back_z1) = panel_planes()
    taper_panel(mesh, NOSE_Z0, NOSE_Z0 + PANEL_THICK, front_z0, front_z1, grade)
    taper_panel(mesh, NOSE_Z1 - PANEL_THICK, NOSE_Z1, back_z0, back_z1, grade)

    # The frame runs the length of the nose cell too, so the nose is carried on the same rail the
    # bays slide along rather than floating at the front of one. Capped at the cell face, which is
    # the one end of a cushion that is never joined to anything.
    build_frame(mesh, slope, caps=(True, False))

    # The nose plate: the impact face, and the one thing on a cushion anybody actually recognises.
    # Striped on the face traffic meets, plain steel on the five it does not -- so the x- face is
    # left off here and drawn as the striped quad below, rather than drawn twice in one plane.
    sheared_box(mesh, NOSE_PLATE_X0, NOSE_PLATE_X1, NOSE_PLATE_Y0, NOSE_PLATE_Y1,
                NOSE_Z0, NOSE_Z1, grade, PANEL_SWATCH,
                faces=("x+", "y-", "y+", "z-", "z+"))

    face_lift = grade * NOSE_PLATE_X0
    striped_quad(mesh,
                 [(NOSE_PLATE_X0, NOSE_PLATE_Y0 + face_lift, NOSE_Z0),
                  (NOSE_PLATE_X0, NOSE_PLATE_Y1 + face_lift, NOSE_Z0),
                  (NOSE_PLATE_X0, NOSE_PLATE_Y1 + face_lift, NOSE_Z1),
                  (NOSE_PLATE_X0, NOSE_PLATE_Y0 + face_lift, NOSE_Z1)],
                 (-1.0, 0.0, 0.0),
                 [(0.0, 0.0), (0.0, CHEVRON_H), (CHEVRON_W, CHEVRON_H), (CHEVRON_W, 0.0)])


def build_nose_end(mesh, slope, left):
    if left:
        # Nothing joins the impact face, so there is no left-hand cap to draw. An empty piece
        # keeps the blockstate's shape the same as every other cushion block's.
        return
    build_bay_end(mesh, slope, False)


def build_nose_fill(mesh, slope):
    build_bay_fill(mesh, slope)


def build_nose_inventory(mesh):
    build_nose(mesh, "flat")
    build_bay_end(mesh, "flat", False)


# --- textures -----------------------------------------------------------------------------------
def steel_texture():
    img = Image.new("RGBA", (TEX_SIZE, TEX_SIZE), geo.GALVANISED + (255,))
    draw_swatches(img, geo.GALVANISED, geo.GALVANISED_DARK, geo.CHEVRON_YELLOW)
    return img


def chevron_texture():
    """The nose plate's sheet, carrying the SAME swatch column as the plain one so the nose
    block's unstriped faces come out the same galvanised as a bay's."""
    img = diagonal_stripe_image(True, CHEVRON_W, CHEVRON_H, stripe_world=CHEVRON_STRIPE,
                                base=geo.CHEVRON_BLACK, stripe=geo.CHEVRON_YELLOW,
                                size=STRIPE_TEX_SIZE)
    draw_swatches(img, geo.GALVANISED, geo.GALVANISED_DARK, geo.CHEVRON_YELLOW)
    return img


# --- catalogue ----------------------------------------------------------------------------------
BLOCKS = {
    geo.CUSHION_BLOCKS[0]: {
        "stem": "crash_cushion_nose",
        "texture": TEXTURE_CHEVRON,
        "display": "Crash Cushion (Nose)",
        "core": build_nose,
        "end": build_nose_end,
        "fill": build_nose_fill,
        "inventory": build_nose_inventory,
    },
    geo.CUSHION_BLOCKS[1]: {
        "stem": "crash_cushion_bay",
        "texture": TEXTURE_STEEL,
        "display": "Crash Cushion (Bay)",
        "core": build_bay,
        "end": build_bay_end,
        "fill": build_bay_fill,
        "inventory": build_bay_inventory,
    },
}


def model_pieces(spec):
    """The named pieces one cushion block's blockstate composes.

    Identical shape to the rails' -- ``core``, ``end_left``, ``end_right_<slope>``,
    ``fill_<slope>`` -- minus ``post``, because a crash cushion has no post to leave off. That is
    also why these blocks are their own Java class rather than a ``BlockGuardrail`` subclass: the
    post is in meta on that one, and a state bit nothing draws is a state bit somebody will
    eventually toggle by accident.
    """
    pieces = {
        "core": lambda m: spec["core"](m, "flat"),
        "end_left": lambda m: spec["end"](m, "flat", True),
    }
    for slope in geo.SLOPES:
        if slope != "flat":
            pieces["core_%s" % slope] = (lambda s: lambda m: spec["core"](m, s))(slope)
        pieces["fill_%s" % slope] = (lambda s: lambda m: spec["fill"](m, s))(slope)
        pieces["end_right_%s" % slope] = (lambda s: lambda m: spec["end"](m, s, False))(slope)
    return pieces


def blockstate_json(spec):
    """The Forge blockstate for one cushion block.

    Identical in shape to ``gen_guardrails_box_cable.py``'s ``rail_blockstate_json`` -- see
    ``gen_guardrails.py``'s own docstring for the merge trick, which is not obvious and is the
    reason one submodel can depend on two properties -- minus the ``post`` variant, because a
    crash cushion has no post.

    A submodel's value has to be a full model OBJECT, not the bare path a reader expects. Forge
    parses that slot as a model definition, and a string there fails the whole blockstate with
    "Not a JSON Object" and paints every one of the block's states purple.
    """
    stem = spec["stem"]
    model = "%s/%s" % (MODEL_PREFIX, stem)
    texture = "%s/%s" % (geo.TEXTURE_PREFIX, spec["texture"])

    def piece(suffix):
        return {"model": "%s_%s.obj" % (model, suffix),
                "custom": {"flip-v": True},
                "textures": {"#%s" % geo.MATERIAL: texture}}

    def submodel(**parts):
        return {"submodel": {key: piece(suffix) for key, suffix in parts.items()}}

    slope_variants = {}
    for slope in geo.SLOPES:
        # The sloped core REPLACES the flat one by naming itself as the model, rather than being
        # laid over it.
        variant = {} if slope == "flat" else {"model": "%s_core_%s.obj" % (model, slope)}
        variant.update(submodel(fill="fill_%s" % slope, end_right="end_right_%s" % slope))
        slope_variants[slope] = variant

    variants = {
        "facing": facing_variants({"diagonal": True}),
        "connectleft": {"false": submodel(end_left="end_left"), "true": {}},
        "connectright": {"false": {}, "true": {"submodel": {"end_right": None}}},
        "diagfill": {"false": {"submodel": {"fill": None}}, "true": {}},
        "slope": slope_variants,
        "normal": [{}],
        "inventory": [{"model": "%s_inv.obj" % model,
                       "custom": {"flip-v": True},
                       "textures": {"#%s" % geo.MATERIAL: texture},
                       "transform": "forge:default-block"}],
    }
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "%s_core.obj" % model,
            "custom": {"flip-v": True},
            "textures": {"#%s" % geo.MATERIAL: texture},
        },
        "variants": variants,
    }


# --- output -------------------------------------------------------------------------------------
def write_model(mesh, path, name, mtl_file):
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
        fh.write("# One material; the blockstate retextures #%s.\n" % geo.MATERIAL)
        fh.write("newmtl %s\n" % geo.MATERIAL)
        fh.write("map_Kd %s/%s\n" % (geo.TEXTURE_PREFIX, texture_name))


def generate(model_dir, texture_dir, blockstate_dir, fragment_dir):
    for d in (model_dir, texture_dir, blockstate_dir, fragment_dir):
        os.makedirs(d, exist_ok=True)
    set_v_span(CELL)
    written = []
    bounds = {}

    for texture, build in ((TEXTURE_STEEL, steel_texture), (TEXTURE_CHEVRON, chevron_texture)):
        path = os.path.join(texture_dir, texture + ".png")
        build().save(path)
        written.append(path)

    for registry, spec in BLOCKS.items():
        stem = spec["stem"]
        mtl_file = stem + ".mtl"
        for suffix, builder in model_pieces(spec).items():
            mesh = Mesh()
            builder(mesh)
            name = "%s_%s" % (stem, suffix)
            path = os.path.join(model_dir, name + ".obj")
            write_model(mesh, path, name, mtl_file)
            written.append(path)

        inv = Mesh()
        spec["inventory"](inv)
        inv_path = os.path.join(model_dir, stem + "_inv.obj")
        write_model(inv, inv_path, stem + "_inv", mtl_file)
        written.append(inv_path)
        bounds[registry] = mesh_bounds(inv)

        mtl_path = os.path.join(model_dir, mtl_file)
        write_mtl(mtl_path, spec["texture"])
        written.append(mtl_path)

        bs_path = os.path.join(blockstate_dir, registry + ".json")
        with open(bs_path, "w", newline="\n") as fh:
            json.dump(blockstate_json(spec), fh, indent=2)
            fh.write("\n")
        written.append(bs_path)

    lang_path = os.path.join(fragment_dir, "crash_cushion_lang.txt")
    with open(lang_path, "w", newline="\n") as fh:
        for registry, spec in BLOCKS.items():
            fh.write("tile.%s.name=%s\n" % (registry, spec["display"]))
    written.append(lang_path)

    tab_path = os.path.join(fragment_dir, "crash_cushion_tab.java")
    with open(tab_path, "w", newline="\n") as fh:
        fh.write("// Generated by %s -- bounding boxes are the INVENTORY models' own extent\n"
                 "// (the diagonal filler is left out of them on purpose).\n" % GENERATOR)
        for registry, spec in BLOCKS.items():
            fh.write('initTabBlock(new %s("%s",\n    %s));\n'
                     % (JAVA_CLASS, registry, java_bbox(bounds[registry])))
    written.append(tab_path)

    return written


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--scratch", action="store_true",
                        help="write into _guardrail_out/ instead of the repo tree")
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
