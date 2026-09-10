#!/usr/bin/env python3
"""
gen_work_zone_devices.py -- the work zone channelizing devices for the Traffic Accessories tab.

Cones, drums, channelizers, delineators and sand barrels: the temporary hardware that closes a
lane. Every one of them is a body of revolution, which is exactly what a hand-authored Blockbench
model is bad at and a lathe is good at, so the geometry, the swatch textures and the blockstates
are all generated from one catalogue here and cannot drift apart.

WHAT MAKES THESE DIFFERENT FROM THE REST OF THE TAB
---------------------------------------------------
These blocks settle onto the surface under them: placed in the cell above a road that does not
fill its cell, they are drawn and collided with pulled down onto it. That is entirely a Java
concern (``codeutils/RoadSurfaceHeight``), and nothing here needs to know about it -- but it is
why every model is authored standing on y=0 with nothing below it. A model that dipped below its
own cell would double up with the offset.

TEXTURES ARE VERTICAL STRIPS
----------------------------
A cone's white collars and a drum's stripes are horizontal bands at specific heights, so every
device's texture is a vertical strip read straight off the model's height: with ``flip-v`` on, a
vt v of ``y/16`` puts height y at the natural place in the image, foot of the device at the foot
of the texture. Drawing a band at the right height is therefore drawing it at the right row, with
no UV arithmetic in between. Faces that have no height to read a band from -- a lid, a mat, the
flat of a handle -- sample instead from the column of flat swatches down the texture's right-hand
edge.

WHAT MAKES EACH SHAPE READ AS ITSELF
------------------------------------
Measured off reference photographs, and each one is the difference between the thing and a
generic striped object:

  * A CONE is a body of revolution on a SQUARE base. Drawn as a tapered square section it reads
    as a pyramid from every angle. It also has a moulded collar at the tip, used as a handhold.
  * A DRUM tapers hard from foot to lid, stands on a wide thin rubber mat far broader than
    itself, and is RIBBED, with a raised rib at every stripe boundary.
  * A CHANNELIZER is a STEPPED column -- four stacked sections of decreasing diameter, each
    stepping in at a rib -- on a HEXAGONAL rubber mat, with a slotted grip handle on top. It is
    not a smooth tube.

Usage:
    python gen_work_zone_devices.py                  # writes into the repo tree
    python gen_work_zone_devices.py --scratch        # writes into _workzone_out/ instead
    python gen_work_zone_devices.py --only traffic_cone traffic_drum

Requires Pillow.
"""
import argparse
import json
import math
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

TRAFFICACCESSORIES_OWNER = layout.owner_of_folder("trafficaccessories")
MODEL_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER,
                                       "models/block/trafficaccessories/shared_models")
TEXTURE_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER,
                                         "textures/blocks/trafficaccessories/workzone")
BLOCKSTATE_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER, "blockstates")
SCRATCH_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_workzone_out")

# Filled by write_barricade_models; the tab's bounding boxes come from the inventory model, which
# is the widest a barricade ever gets.
BARRICADE_BOUNDS = {}

# The block centre, which every device is built around. Declared up here because the
# shape constants below are written relative to it.
AXIS = 8.0

TEXTURE_PREFIX = "csm:blocks/trafficaccessories/workzone"
MATERIAL = "body"

TEX_SIZE = 32                 # texture edge, px. Power of two, and fine enough for a 4 in band.
# The striped panels get a bigger sprite: a diagonal edge across a rail six times wider than it
# is tall is all staircase at 32 px, and unlike a horizontal band its cost is in the diagonal.
STRIPE_TEX_SIZE = 64
# The signal trailers get a bigger sprite because of the solar array: a cell grid on a 32 px
# sheet is four pixels a cell, which is a blue smear.
TRAILER_TEX_SIZE = 64
# The cell grid the mini solar panel block already ships. Reused rather than drawn
# again so the trailer's array and that block match wherever they stand together.
SOLAR_CELL_TEXTURE = os.path.join(
    layout.REPO_ROOT, "modules", "roads", "src", "main", "resources", "assets",
    "csm", "textures", "blocks", "trafficaccessories", "shared_textures",
    "solar_panel.png")
# The arrow board gets a bigger sprite again: 35 lamps across a 7-column grid leaves about seven
# pixels a cell at 64, and a round lamp does not survive a 2.4 pixel radius.
ARROW_TEX_SIZE = 128
SWATCH_U0 = 0.78125           # 25/32: everything right of this column is flat swatches
SWATCH_BASE_V = 0.125         # v of the base-colour swatch's centre
SWATCH_DARK_V = 0.375         # v of the black swatch's centre
SWATCH_BAND_V = 0.625         # v of the band-colour (reflective white) swatch's centre
SWATCH_ACCENT_V = 0.875       # v of the accent swatch's centre -- the amber lens

# --- palette ---------------------------------------------------------------------------------
ORANGE = (238, 96, 20)        # traffic cone / drum orange, fluorescent
ORANGE_DARK = (196, 72, 12)
WHITE = (233, 233, 228)       # reflective sheeting white, not paper white
WHITE_DIM = (198, 198, 192)
BLACK = (36, 36, 38)          # the rubber base and mat
BLACK_LIGHT = (52, 52, 55)
LIME = (188, 226, 42)         # the high-visibility yellow-green alternative
LIME_DARK = (152, 186, 30)
SAND_YELLOW = (216, 176, 52)  # the sand barrel's weathered yellow
SAND_YELLOW_DARK = (176, 140, 36)
AMBER = (246, 166, 24)        # the warning light's lens
MARKER_YELLOW = (242, 198, 34)    # the temporary marker's moulded yellow, lighter than a cone's
MARKER_YELLOW_DARK = (198, 158, 22)
MARKER_BLUE = (0, 89, 152)        # MUTCD/ADA blue
MARKER_BLUE_DARK = (0, 66, 114)
MARKER_GREEN = (0, 177, 64)       # the FHWA green a bike lane is surfaced in
MARKER_GREEN_DARK = (0, 138, 50)
# A road plate is not bright steel. It goes down weathered and comes up weathered, and what is
# on the road is a dark, almost black surface with the tread worn smooth under traffic and rust
# blooming from the edges and the lifting holes.
SOLAR_BLUE = (30, 44, 92)         # the cell panel on a trailer's deck
GALVANISED = (158, 160, 166)      # a trailer's jack legs and winch hardware
STEEL = (62, 58, 56)              # the road plate's weathered steel
STEEL_DARK = (46, 43, 42)
STEEL_LIGHT = (80, 75, 72)
STEEL_RUST = (104, 60, 36)
STEEL_RUST_LIGHT = (128, 78, 46)
CONCRETE = (168, 168, 162)        # the temporary barrier's precast grey
CONCRETE_DARK = (136, 136, 130)
MARKER_RED = (178, 26, 44)        # MUTCD stop sign red
MARKER_RED_DARK = (140, 18, 32)
MARKER_SILVER = (206, 212, 220)   # the glass-beaded reflective strip along the top
MARKER_SILVER_DARK = (170, 176, 186)
MARKER_GOLD = (250, 216, 96)      # the same strip on an amber marker
MARKER_GOLD_DARK = (214, 178, 62)

# --- the cone, in 1/16 block units --------------------------------------------------------------
# A 28 in cone on a 1 m block: 28 in is 0.711 m, so 11.2 units tall, on a 14 in (5.7 unit) base.
#
# CONE_SCALE then stretches the whole thing about its foot. A cone at true scale reads slightly
# small next to a Minecraft player, who is a good deal shorter than a real person relative to the
# metre block, so the cone is drawn a little over life size to sit right beside one. Everything
# below is written at true scale and multiplied here, so the shape is tuned in one number and the
# collars cannot drift off the body while it changes.
CONE_SCALE = 1.10
CONE_SIDES = 16
_CONE_HEIGHT = 11.20
_CONE_BASE_HALF = 2.95
_CONE_BASE_TOP = 0.62
# (radius, y) bottom up. The gentle concave sweep, rather than a straight taper, is what gives a
# moulded cone its silhouette.
_CONE_PROFILE = [
    (2.45, _CONE_BASE_TOP),
    (2.30, 1.10),
    (2.00, 2.60),
    (1.62, 4.80),
    (1.24, 7.20),
    (0.95, 9.20),
    (0.78, 10.20),
    (0.64, 10.52),   # the neck under the moulded tip collar
    (0.64, 10.66),
    (0.82, 10.76),
    (0.82, 11.02),
    (0.70, 11.12),
    (0.38, _CONE_HEIGHT),
]
# A 6 in reflective band below the top and a 4 in band below that, with an orange gap between.
_CONE_COLLARS = [(6.85, 9.15), (4.30, 6.00)]

CONE_HEIGHT = _CONE_HEIGHT * CONE_SCALE
CONE_BASE_HALF = _CONE_BASE_HALF * CONE_SCALE
CONE_BASE_TOP = _CONE_BASE_TOP * CONE_SCALE
CONE_BASE_PROFILE = [
    (CONE_BASE_HALF, 0.00),
    (CONE_BASE_HALF, 0.42 * CONE_SCALE),
    ((_CONE_BASE_HALF - 0.30) * CONE_SCALE, CONE_BASE_TOP),
]
CONE_PROFILE = [(r * CONE_SCALE, y * CONE_SCALE) for (r, y) in _CONE_PROFILE]
CONE_COLLARS = [(y0 * CONE_SCALE, y1 * CONE_SCALE) for (y0, y1) in _CONE_COLLARS]

# --- the drum -------------------------------------------------------------------------------------
DRUM_SIDES = 16
DRUM_MAT_R = 7.10          # the rubber ballast mat, ~32 in across, far wider than the drum
DRUM_MAT_TOP = 0.32
DRUM_BODY_Y1 = 13.20
DRUM_BOTTOM_R = 4.75       # ~24 in across at the foot
DRUM_TOP_R = 3.45          # ~17 in across at the lid
DRUM_RIB = 0.26            # how far a rib stands proud of the taper
DRUM_RIBS = [1.05, 3.95, 6.55, 8.15, 10.75, 12.70]
DRUM_STRIPES = [(4.05, 6.45), (8.25, 10.65)]
DRUM_LID_R = 3.28
DRUM_LID_Y = 13.95
DRUM_HANDLE = {
    "half_x": 2.55, "half_z": 0.30, "y0": DRUM_LID_Y - 0.20, "y1": 15.35,
    "slot_half_x": 1.50, "slot_y0": 14.45, "slot_y1": 14.95,
}
# The Type A warning light on the lit variant: a white housing carrying an amber lens on each
# face, which is what the real ones show to traffic in both directions.
#
# The lens sits a little over a block above the ground once the drum is under it, which is what
# a real light on a real drum does. ``lens_cy`` is DERIVED from the housing top and the rim
# radius rather than written down, because a hand-picked value that puts the lens bottom below
# the housing top inverts the neck box between them -- which is not a crash, just a hole you
# only see from one side in game.
DRUM_LAMP = {
    "post_x": 1.55, "post_half": 0.50, "post_y1": 14.40,
    "body_half_x": 0.95, "body_half_z": 0.72, "body_y0": 14.30, "body_y1": 14.95,
    "neck_half_x": 0.52, "neck_half_z": 0.40, "neck_rise": 0.30,
    "lens_r": 1.05, "lens_half_z": 0.34, "rim_r": 1.18,
}
DRUM_LAMP["lens_cy"] = (DRUM_LAMP["body_y1"] + DRUM_LAMP["neck_rise"]
                        + DRUM_LAMP["rim_r"] * 0.72)

# --- the channelizer -------------------------------------------------------------------------------
# A 42 in channelizer: a stepped column on a hexagonal mat, with a slotted grip on top.
#
# Two scales, not one, because the two axes want different corrections. The column is drawn
# NARROWER than true scale (a 10 in tube is under a sixth of a block, and at that width the steps
# between sections stop reading at all, so it is only pulled in as far as they survive) and TALLER
# than the block it stands in. It is the tall skinny one on the site: at drum proportions it is
# just a short drum, which is exactly what it looked like before these were split apart. The mat
# keeps its own width, since it is wide on the real thing too and it is what stops the column
# reading as a floating stick.
CHANNELIZER_RADIAL_SCALE = 0.78
CHANNELIZER_HEIGHT_SCALE = 1.19
CHANNELIZER_SIDES = 16
CHANNELIZER_MAT_SIDES = 6
CHANNELIZER_MAT_R = 5.30
CHANNELIZER_MAT_TOP = 0.55
# (radius, y0, y1) for each stacked section, bottom up. Each section steps in at a rib.
_CHANNELIZER_SECTIONS = [
    (2.45, 0.55, 4.10),
    (2.15, 4.45, 7.60),
    (1.88, 7.95, 11.00),
    (1.62, 11.35, 13.60),
]
_CHANNELIZER_BANDS = [(4.45, 5.95), (7.95, 9.45), (11.35, 12.85)]
_CHANNELIZER_NECK_R = 0.88
_CHANNELIZER_NECK_Y = 14.70
_CHANNELIZER_GRIP_TOP = 16.00


def _ch_y(y):
    """Height on the channelizer, stretched about the top of its mat so the column grows upward
    and its foot stays sitting on the mat."""
    return CHANNELIZER_MAT_TOP + (y - CHANNELIZER_MAT_TOP) * CHANNELIZER_HEIGHT_SCALE


CHANNELIZER_SECTIONS = [(r * CHANNELIZER_RADIAL_SCALE, _ch_y(y0), _ch_y(y1))
                        for (r, y0, y1) in _CHANNELIZER_SECTIONS]
CHANNELIZER_STEP = 0.18 * CHANNELIZER_RADIAL_SCALE
CHANNELIZER_BANDS = [(_ch_y(y0), _ch_y(y1)) for (y0, y1) in _CHANNELIZER_BANDS]
CHANNELIZER_NECK_R = _CHANNELIZER_NECK_R * CHANNELIZER_RADIAL_SCALE
CHANNELIZER_NECK_Y = _ch_y(_CHANNELIZER_NECK_Y)
CHANNELIZER_GRIP = {
    "half_x": 1.15 * CHANNELIZER_RADIAL_SCALE, "half_z": 0.26,
    "y0": CHANNELIZER_NECK_Y - 0.15, "y1": _ch_y(_CHANNELIZER_GRIP_TOP),
    "slot_half_x": 0.60 * CHANNELIZER_RADIAL_SCALE,
    "slot_y0": _ch_y(15.05), "slot_y1": _ch_y(15.65),
}

# --- the channelizer-cade ------------------------------------------------------------------------
# A vertical panel in a moulded frame on a rubber mat: a channelizer that is also a small
# barricade. The panel carries MUTCD diagonal stripes, and they SLOPE TOWARD THE SIDE TRAFFIC IS
# MEANT TO PASS -- which is why this comes in a left and a right, and why the two are mirror
# images rather than the same model turned around. Turning one round would point its stripes the
# wrong way, telling drivers to pass on the side the work is on.
CADE_MAT_SIDES = 8
CADE_MAT_R = 4.90
CADE_MAT_TOP = 0.72
CADE_FRAME_HALF_X = 3.30
CADE_FRAME_HALF_Z = 0.42
CADE_FRAME_Y0 = CADE_MAT_TOP
CADE_FRAME_Y1 = 15.60
CADE_FRAME_RAIL = 0.62        # width of the frame border around the panel
CADE_FOOT_Y1 = 2.05           # the moulded foot block the frame stands on
CADE_PANEL = (2.62, 2.35, 14.20)   # half-width, y0, y1 of the striped panel
CADE_PANEL_HALF_Z = 0.22
# The two grip slots through the frame's head, and the bolt hole between them.
CADE_HEAD_SLOT = {"half_x": 0.92, "y0": 14.62, "y1": 15.18, "gap": 0.42}
CADE_PANEL_W = CADE_PANEL[0] * 2.0
CADE_PANEL_H = CADE_PANEL[2] - CADE_PANEL[1]

# --- the barricades ------------------------------------------------------------------------------
# Type I carries one striped rail, Type III three.
#
# BARRICADE_SCALE stretches the assembly's HEIGHT and its members about its feet. A real Type III
# is four to eight feet long and about five feet tall, so even scaled up it is under-sized.
#
# The rails, though, span EXACTLY one cell, and that is what makes barricades connect. Abutting
# runs need their rails to meet at the cell boundary: overhanging rails would overlap a
# neighbour's and z-fight, and the striped panel UVs cannot continue past the edge of their
# sprite to cover an overhang anyway (see D14 for what out-of-range UVs do). Spanning the cell
# exactly also means a run of five barricades is five blocks long, which is what anyone laying
# one out will expect.
#
# The legs are centred ON the cell edges rather than inside them, so where two barricades meet
# the single shared upright sits on the seam instead of beside it.
BARRICADE_SCALE = 1.35
_BARRICADE_RAIL_HALF_Z = 0.26
_BARRICADE_LEG_HALF_X = 0.55
_BARRICADE_LEG_HALF_Z = 0.42
_BARRICADE_FOOT_HALF_Z = 2.60    # the splayed foot each leg stands on
_BARRICADE_FOOT_Y1 = 0.55
# (y0, y1) of each rail, bottom up.
_BARRICADE_TYPE1_RAILS = [(6.60, 9.00)]
_BARRICADE_TYPE1_TOP = 9.60
_BARRICADE_TYPE3_RAILS = [(3.00, 5.40), (6.60, 9.00), (10.20, 12.60)]
_BARRICADE_TYPE3_TOP = 13.20

BARRICADE_RAIL_HALF_X = AXIS       # exactly half a cell: rails meet on the boundary
# How far the rails run PAST the uprights at a free end. Only a free end gets it -- a connected
# one has the neighbour's rail there instead -- which is what lets the same rail both butt
# cleanly against another barricade and finish properly when it is the last in the run.
BARRICADE_RAIL_OVERHANG = 2.26 * BARRICADE_SCALE
BARRICADE_RAIL_HALF_Z = _BARRICADE_RAIL_HALF_Z * BARRICADE_SCALE
BARRICADE_LEG_HALF_X = _BARRICADE_LEG_HALF_X * BARRICADE_SCALE
BARRICADE_LEG_HALF_Z = _BARRICADE_LEG_HALF_Z * BARRICADE_SCALE
BARRICADE_LEG_X = AXIS             # legs centred on the cell edges, so seams share one
BARRICADE_FOOT_HALF_Z = _BARRICADE_FOOT_HALF_Z * BARRICADE_SCALE
BARRICADE_FOOT_Y1 = _BARRICADE_FOOT_Y1 * BARRICADE_SCALE
BARRICADE_TYPE1_RAILS = [(y0 * BARRICADE_SCALE, y1 * BARRICADE_SCALE)
                         for (y0, y1) in _BARRICADE_TYPE1_RAILS]
BARRICADE_TYPE1_TOP = _BARRICADE_TYPE1_TOP * BARRICADE_SCALE
BARRICADE_TYPE3_RAILS = [(y0 * BARRICADE_SCALE, y1 * BARRICADE_SCALE)
                         for (y0, y1) in _BARRICADE_TYPE3_RAILS]
BARRICADE_TYPE3_TOP = _BARRICADE_TYPE3_TOP * BARRICADE_SCALE
# Which rails and what height each barricade type carries, keyed by the name its models take.
BARRICADE_TYPES = {
    "type1": (BARRICADE_TYPE1_RAILS, BARRICADE_TYPE1_TOP),
    "type3": (BARRICADE_TYPE3_RAILS, BARRICADE_TYPE3_TOP),
}
BARRICADE_RAIL_W = BARRICADE_RAIL_HALF_X * 2.0
BARRICADE_RAIL_H = BARRICADE_TYPE1_RAILS[0][1] - BARRICADE_TYPE1_RAILS[0][0]
# The stripe pitch is the rail width over four, which puts exactly TWO full light-dark periods
# across a cell. That is not cosmetic: it means the pattern has the same phase at both edges of
# every rail, so where two barricades meet the striping carries straight on across the seam
# instead of jumping, and a run reads as one long barricade rather than a line of separate
# panels. It is also what lets a free end's overhang continue the pattern by wrapping round to
# the far edge of the same sprite.
BARRICADE_STRIPE = BARRICADE_RAIL_W / 4.0

# --- the Type II folding barricade -----------------------------------------------------------
# The plastic A-frame barricade that folds flat: a vertical panel of two striped rails on two
# uprights, with a pair of legs hinged near the top that swings out behind it to stand it up.
#
# Unlike the trestle barricades this one does NOT join to its neighbours, so it is narrower than
# a cell and its model is one baked shape with no ends to add or take away. A real one is a
# self-contained unit that is carried in, opened and set down; a row of them is a row of separate
# devices each on its own legs, not one continuous rail. Trying to join them would also have to
# decide what happens to the legs at a seam, and the honest answer is that nothing does.
#
# It keeps the trestle barricades' stripe PITCH rather than their proportions, so a work zone
# using both does not show two different stripe widths standing side by side.
FOLD_TOP = 14.85                  # top of the uprights
FOLD_UPRIGHT_X = 5.20             # half the spacing between the two uprights
FOLD_UPRIGHT_HALF_X = 0.70
# The rails run a little PAST the uprights, as they do on a real one, and not flush with
# them: flush would put the rail's end face and the upright's outer face in the same plane,
# which z-fights.
FOLD_RAIL_HALF_X = FOLD_UPRIGHT_X + FOLD_UPRIGHT_HALF_X + 0.35
FOLD_RAIL_HALF_Z = 0.30
FOLD_RAILS = [(5.20, 8.30), (10.25, 13.35)]
# The uprights sit BEHIND the rails, not around them: the striped face is what points at traffic
# and a frame in front of it would interrupt the stripes. They start just inside the rails rather
# than flush against them, so the two never share a plane.
FOLD_FRAME_Z0 = AXIS + FOLD_RAIL_HALF_Z - 0.10
FOLD_FRAME_Z1 = FOLD_FRAME_Z0 + 0.68
FOLD_FRAME_CZ = 0.5 * (FOLD_FRAME_Z0 + FOLD_FRAME_Z1)
FOLD_FOOT_HALF_X = 1.05
FOLD_FOOT_HALF_Z = 1.10
FOLD_FOOT_Y = 0.80
FOLD_HINGE_Y = 13.70              # where the rear legs are pinned to the uprights
FOLD_LEG_SPLAY = 4.60             # how far back the feet stand from the panel
FOLD_LEG_HALF_X = 0.52
FOLD_LEG_HALF_Z = 0.42
FOLD_LEG_Y0 = 0.30                # the leg starts inside its pad, so they share no face
FOLD_PAD_HALF_X = 0.95
FOLD_PAD_HALF_Z = 1.00
FOLD_PAD_Y = 0.75
FOLD_BRACE_Y = 3.20               # the cross brace between the rear legs
FOLD_BRACE_H = 0.55
FOLD_RAIL_W = FOLD_RAIL_HALF_X * 2.0
FOLD_RAIL_H = FOLD_RAILS[0][1] - FOLD_RAILS[0][0]
# Everything above is authored with the panel on the block axis and the legs trailing off behind
# it, which would leave the device sitting in the back half of its cell. It is shifted forward so
# the WHOLE assembly is centred instead. That moves the panel off the axis, so where the panel
# ends up is derived here and handed to the renderer: a warning light clamped to the axis and a
# sign mounted on the axis would both float in front of a barricade that is no longer there.
FOLD_Z_MIN = min(AXIS - FOLD_RAIL_HALF_Z, FOLD_FRAME_CZ - FOLD_FOOT_HALF_Z)
FOLD_Z_MAX = FOLD_FRAME_CZ + FOLD_LEG_SPLAY + FOLD_PAD_HALF_Z
FOLD_SHIFT_Z = AXIS - 0.5 * (FOLD_Z_MIN + FOLD_Z_MAX)
FOLD_RAIL_CZ = AXIS + FOLD_SHIFT_Z

# --- the arrow board -------------------------------------------------------------------------------
# A trailer-mounted arrow board: a black 2:1 panel of amber lamps on a tall orange mast over a
# small two-wheel trailer, measured off reference photographs.
#
# The lamp grid is 7 by 5 -- the 25-lamp board -- rather than the 15-lamp one drawn first. That
# matters for more than lamp count: seven columns is what lets a chevron actually converge to a
# point, and five rows is what lets it have a diagonal rather than a single barb. A 5 by 3 grid
# can only draw a shaft with a bump on it.
ARROW_PANEL = (1.20, 14.80, 10.40, 17.20)   # x0, x1, y0, y1 of the panel face
ARROW_PANEL_Z = (7.30, 8.40)
ARROW_FRAME = 0.42                          # border of panel casing around the lit face
# Twin mast posts, as the references have, rather than one central column.
ARROW_MAST_X = (6.55, 9.45)
ARROW_MAST_HALF = 0.42
ARROW_MAST = (1.80, 11.00)                  # y0, y1
ARROW_MAST_BRACE = (6.40, 9.60)             # y of the two cross braces, below the panel
ARROW_CHASSIS = (2.60, 13.40, 0.85, 1.80, 5.60, 10.40)
ARROW_TONGUE = (13.40, 15.90, 1.05, 1.60, 7.60, 8.40)
ARROW_WHEEL_R = 1.15
ARROW_WHEEL_X = (1.90, 14.10)
ARROW_WHEEL_HALF = 0.45
ARROW_WHEEL_Y = 1.15
ARROW_WHEEL_Z = (5.10, 10.90)
ARROW_JACK_X = (3.40, 12.60)                # the outrigger jacks either side
ARROW_JACK_HALF = 0.28
ARROW_GRID = (7, 5)
# Everything above is authored at a convenient size and then scaled about the block's centre.
#
# The scale is set by the company this device keeps, not by the block grid: the portable message
# sign and the portable speed limit sign next to it in this tab are drawn 66 units wide and over
# 70 tall -- four blocks and more -- by their tile entity renderers. An arrow board built to fit
# inside one cell stands next to them looking like a toy, which is exactly how the first version
# of this came out. An arrow board is smaller than a full message board, so it lands a little
# under them rather than level.
#
# It follows that the model leaves its cell in every direction, which for a static model means
# it is drawn as part of its chunk section and can pop when that section is culled. The mast arm
# curves and the controller cabinets in this tab already accept that; the alternative is a tile
# entity renderer with an expanded render bounding box, as the portable signs use, which is a
# larger change than this batch warrants.
ARROW_SCALE = 4.2
# The two arrow formats a real board can show, plus caution mode. Written pointing RIGHT and
# mirrored for the left variants, so the pair can never drift apart.
#
# A chevron is the arrowhead alone, drawn two lamps thick along each limb. A bar arrow is a shaft
# with a head on it. The barbs sit one column BACK from the tip, not level with it -- barbs in the
# tip's own column light a cross rather than an arrow, which is what the first attempt drew.
_ARROW_CHEVRON_RIGHT = [(6, 2), (5, 1), (4, 0), (5, 3), (4, 4),
                        (5, 2), (4, 1), (3, 0), (4, 3), (3, 4)]
_ARROW_BAR_RIGHT = [(0, 2), (1, 2), (2, 2), (3, 2), (4, 2), (5, 2), (6, 2),
                    (5, 1), (4, 0), (5, 3), (4, 4)]
# Caution mode is the four corner lamps, which is what a real board shows when it is warning
# rather than directing.
_ARROW_CAUTION = [(0, 0), (6, 0), (0, 4), (6, 4)]


def _mirror(lamps):
    """Mirror a pattern left-right across the grid."""
    cols = ARROW_GRID[0]
    return [(cols - 1 - c, r) for (c, r) in lamps]


ARROW_PATTERNS = {
    "chevron_right": _ARROW_CHEVRON_RIGHT,
    "chevron_left": _mirror(_ARROW_CHEVRON_RIGHT),
    "bar_right": _ARROW_BAR_RIGHT,
    "bar_left": _mirror(_ARROW_BAR_RIGHT),
    "caution": _ARROW_CAUTION,
}
ARROW_PANEL_W = (ARROW_PANEL[1] - ARROW_FRAME) - (ARROW_PANEL[0] + ARROW_FRAME)
ARROW_PANEL_H = (ARROW_PANEL[3] - ARROW_FRAME) - (ARROW_PANEL[2] + ARROW_FRAME)
ARROW_LAMP_OFF = (58, 58, 62)
ARROW_PANEL_BLACK = (26, 26, 28)
ARROW_FRAME_ORANGE = (232, 106, 24)   # the trailer and mast, which are orange on every real one

# --- the delineator post -----------------------------------------------------------------------
DELINEATOR_HEIGHT = 15.00
DELINEATOR_SECTION = (1.55, 0.55)   # half-width across the face, half-depth
DELINEATOR_BASE_HALF = 2.30
DELINEATOR_BASE_TOP = 0.55
DELINEATOR_REFLECTOR = (11.90, 13.40)

# --- the zebra delineator ----------------------------------------------------------------------
# The low rubber lane separator laid nose to tail along a bike lane edge: a long flattened dome,
# black, with white reflective bands wrapped across it.
#
# It runs the length of its cell so a line of them is continuous, and tapers to a nose at each
# end, which is what gives a row its scalloped look without needing a gap between blocks.
#
# Its texture is mapped the other way round from every other device here: v runs along the BODY
# rather than up it, and u runs around the arched cross-section. That is what lets band_image --
# written for the collars on a cone -- draw bands ACROSS this one, and it also puts that
# function's one-sided lighting over the crown, where the highlight belongs.
ZEBRA_X = (1.10, 14.90)        # x0, x1 of the body
ZEBRA_HALF_Z = 2.55            # half width at the widest point
ZEBRA_HEIGHT = 3.05            # crown height at the widest point
ZEBRA_NOSE = 0.55              # taper exponent; lower is a longer, finer nose
ZEBRA_TIP = 0.13               # how much section is left at the very tip, as a fraction
ZEBRA_ARCH_SIDES = 10          # segments around the half-ellipse cross-section
ZEBRA_STATIONS = 18            # cross-sections along the length
# Five bands, evenly pitched, stopping short of both noses -- a band wrapped round the tip reads
# as a painted end rather than applied sheeting.
ZEBRA_BAND_W = 1.15
ZEBRA_BANDS = [(c - ZEBRA_BAND_W / 2.0, c + ZEBRA_BAND_W / 2.0)
               for c in (3.40, 5.90, 8.40, 10.90, 13.40)]

# --- the wall devices ----------------------------------------------------------------------------
# Two devices that are walls: the interlocking plastic barrier filled with water on site, and the
# precast concrete barrier that goes in for longer closures. Both run the length of their cell so
# a line of them is a continuous wall.
#
# Both span the cell EXACTLY, and are drawn as a core plus two end caps with the cap left off
# wherever a neighbour stands, the same way the barricades are. Leaving a hair of gap instead and
# always capping was tried first: it avoids the z-fight of two coincident end faces for nothing,
# but a run then reads as a line of separate blocks rather than as a wall, which is the one thing
# a wall has to do.
WALL_X = (0.00, 16.00)

# How far a device laid at forty-five degrees stands off its neighbour. A device spans one cell
# and a diagonal step between cell centres is sqrt(2) cells, so a diagonal run leaves this much
# air at every joint -- about 6.6 units, which is two thirds of a barricade's overhang and
# impossible not to see. Devices that JOIN close it with a filler at their right-hand end.
DIAGONAL_GAP = (2.0 ** 0.5 - 1.0) * 16.0

# (half width, height) up one side of the cross-section, bottom to top.
LCD_PROFILE = [(3.00, 0.00), (3.00, 1.30), (1.70, 4.60), (1.45, 9.60), (1.10, 11.00)]
LCD_TOP_BAND = (9.60, 11.00)   # the white cap the top rail is moulded in
LCD_RIBS = 6                   # moulded vertical ribs along the body

# The New Jersey profile: a wide foot, a steep lower flare that turns a tyre back, and a near
# vertical face above it.
BARRIER_PROFILE = [(3.60, 0.00), (3.60, 1.40), (1.90, 4.20), (1.35, 12.20), (1.20, 13.00)]

# --- the portable signal trailers -----------------------------------------------------------------
# The towed signal that runs a one-lane two-way work zone: a single-axle trailer, a winch mast,
# and on the arm style an arm reaching out over the road.
#
# It carries NO signal heads of its own. The mod already has every signal anyone could want and a
# controller system to drive them, and a signal head placed beside anything draws its own mounting
# hardware, so the trailer's job is to be the thing they mount ON.
#
# THE SCALE IS SET BY THE SIGNALS, NOT BY THE TRAILER. A signal head in this mod is a block, and a
# mast arm intersection is built with its arm around ten blocks over the road; a temporary signal
# sits a little lower but not much, or traffic does not fit under it. So the arm's underside is
# eight blocks up and everything else follows from that -- which makes the mast very tall against
# a compact trailer, exactly as it is on the real thing.
PSIG_ARM_CLEARANCE = 8 * 16.0        # underside of the LEVEL run, in 1/16 units above the base

# Relative to the trailer's own cell and its facing, the cells left free for heads are:
#   cells three to eight out, under the level run -- the overhead head       (arm style only)
#   either cell to the SIDE, five up              -- the near-side head on the mast
# The near head sits well below the overhead one, which is how these are actually rigged: the
# mast head is read from the stop line and the boom head from back down the lane. It goes beside
# the trailer rather than out along the boom because the head faces ACROSS the boom -- the boom
# reaches over the road and the head looks up it -- which puts the mast squarely behind the head,
# and a head with the pole behind it is what Rear Mount is for.
#
# Which cell the overhead head goes in depends on how many sections it has: the body reaches up
# to 1.5 blocks above its own block, so a three-section head hangs SIX up and a single-section
# one seven. Nothing here has to know that -- Overhead Mount measures its own body and reaches
# for the first block boundary above it, which is where the level run's underside is.

# --- the chassis ---------------------------------------------------------------------------
# Sized against the mast and boom above it rather than against a real trailer's own dimensions.
# The first version was measured off the trailer alone and came out a toy: a deck two cells long
# under a boom eight cells long reads as a mast someone left standing on a go-kart. On the
# reference machines the deck is around a third of the overall height, and the wheels and jacks
# are big enough to see from across the road.
# The compact chassis, which the plain mast and pedestrian styles use.
PSIG_BED = (-18.00, 26.00, 10.40, 20.40, 0.00, 16.00)   # x0, x1, y0, y1, z0, z1
PSIG_TONGUE = (26.00, 46.00, 12.60, 17.00, 5.60, 10.40)
PSIG_WHEEL = {"r": 9.20, "half": 3.00, "x": 1.00, "y": 9.20, "z": (-2.60, 18.60)}
PSIG_JACK_X = (-14.40, 22.40)
PSIG_JACK_Z = (-1.20, 17.20)
PSIG_JACK_HALF = 1.10
PSIG_CABINET = (12.00, 24.00, 20.40, 38.00, 2.60, 13.40)  # the controller box beside the mast

# --- the arm style's chassis -----------------------------------------------------------------
# A boom eight cells long is a lever, and what stops the machine going over with it is the base
# under it. On the compact chassis there was barely any: the deck was narrower than the boom was
# long by a factor of eight, and the jacks stood inside the deck's own footprint.
#
# So the arm style gets its own, and every dimension of it is doing a job. The deck is longer and
# a good deal wider. The axle carries twin wheels a side, which is what a trailer this size runs
# on. And the jacks become OUTRIGGERS that reach well outside the body, because a jack directly
# under the deck adds nothing to the tipping base -- the deck edge was already there.
PSIG_ARM_BED = (-30.00, 34.00, 11.20, 22.40, -5.00, 21.00)
PSIG_ARM_TONGUE = (34.00, 56.00, 13.60, 19.00, 5.20, 10.80)
PSIG_ARM_WHEEL = {"r": 10.20, "half": 3.20, "x": 2.00, "y": 10.20,
                  "z": (-8.40, -1.60, 17.60, 24.40)}
PSIG_ARM_JACK_X = (-26.00, 30.00)
PSIG_ARM_JACK_Z = (-11.00, 27.00)     # outside the deck: that is the whole point of an outrigger
PSIG_ARM_JACK_HALF = 1.45
PSIG_ARM_OUTRIGGER_Y = (13.00, 16.20)  # the arms the jacks hang off, reaching out from the bed
PSIG_ARM_SOLAR = (-28.00, 2.00, -3.50, 19.50)   # x0, x1, z0, z1
PSIG_ARM_CABINET = (14.00, 32.00, 22.40, 44.00, 0.40, 15.60)

# --- the solar array -------------------------------------------------------------------------
# The one part of a trailer that is not painted steel, and the only one worth a picture rather
# than a flat colour: a swatch of dark blue at this size reads as a tarpaulin. It carries the
# same cell texture the mini solar panel block uses, so the two match wherever they stand
# together, and it is TILTED — a panel lying flat on the deck is the one thing nobody builds,
# because the whole point of it is to face the sun.
PSIG_SOLAR = (-16.50, 4.50, 0.50, 15.50)   # x0, x1, z0, z1 -- its footprint on the deck
PSIG_SOLAR_Y = 21.00       # underside of its LOW edge
PSIG_SOLAR_RISE = 3.40     # how much higher the far edge stands
PSIG_SOLAR_THICK = 0.90
PSIG_SOLAR_PROP = 0.75     # half-size of the props holding the high edge up
# Where the cell image sits on the trailer's sheet. flip-v is on, so a face asking for v samples
# sprite row size*(1-v) -- the paste has to be at the flipped rows or the panel wears whatever is
# above it. The region's aspect matches the panel's, so the cells come out square.
PSIG_SOLAR_U = (0.03, 0.72)
PSIG_SOLAR_V = (0.55, 0.92)

PSIG_MAST_X = 8.00
PSIG_MAST_Z = 8.00
PSIG_MAST_HALF = 3.00

# --- the boom ------------------------------------------------------------------------------
# NOT a straight bar off the top of the mast. On every real one the boom PIVOTS near the top of
# the mast and RISES to its working height over the first couple of cells, then runs level out
# to the tip -- which is why the mast on these is so much shorter than the boom is high, and why
# a straight bar at full height reads as a gantry rather than as a towed machine.
#
# The level run is also what the block grid wants. It is the part heads hang from, and it has to
# be at ONE height over several cells or there is no row of cells to hang them in.
PSIG_PIVOT_Y = 100.00          # boom centreline where it leaves the mast
PSIG_ELBOW_Z = -32.00          # where the rise ends and the level run begins: two cells out
# Eight whole cells out. These booms are LONG -- on the real thing the boom is nearly as long as
# the whole machine is tall, which is what lets one trailer signal the far lane of a road it is
# parked beside. It ends flush on a cell boundary, so a head hung in any cell under it meets the
# boom rather than poking through it, and the tip cell is the natural place for the far one.
PSIG_ARM_TIP_Z = -128.00
# Half width and depth at each of the three stations. A cantilever this long is not a constant
# section on the real thing and does not look like one here either.
PSIG_BOOM_ROOT = (2.20, 5.20)
PSIG_BOOM_ELBOW = (2.00, 4.20)
PSIG_BOOM_TIP = (1.15, 2.60)

PSIG_PIVOT_PLATE = (5.40, 1.00)   # half height/reach of the pivot gusset, and its thickness
# The king post, and the tie from its top out along the level run. The tie is a ROD, not a wire:
# the first version drew it 1.2 units across, which at this range is a single pixel and reads as
# a cable someone strung between two points rather than as the thing holding the boom up.
PSIG_KING_Z = -38.00
PSIG_KING_RISE = 15.00         # how far the king post stands above the boom's top
PSIG_KING_HALF = 1.25
PSIG_TIE_AT = 0.66             # where the tie meets the level run, along its span
PSIG_TIE_HALF = 1.10
# The ram under the rising section, which on the real machine is what raises the boom. It sits
# close under the pivot rather than reaching down to the middle of the mast -- a strut from half
# way down the mast is a gantry's knee brace, not a boom's ram.
PSIG_RAM_AT = 0.42             # where it meets the rising section, along the rise
PSIG_RAM_DROP = 34.00          # how far below the pivot its foot sits on the mast
PSIG_RAM_HALF = 1.45

# Mast height per style. On the arm style the mast stops just above the pivot -- it does not
# carry the boom's outer end, the tie does. The mast style carries one head against the mast and
# needs no boom; the pedestrian one is lower again, because a walk signal is read from the kerb
# and not from a car.
#
# The mast style's height is set by the head it carries, not by the trailer. Its near-side head
# goes five up, and a three-section body reaches 1.5 blocks above its own block, so the head's
# top -- and the top bracket of its Rear Mount -- is at six and a half blocks. A mast that stops
# at six and a bit ends BELOW the hardware bolted to it, which is what it did at first. It stands
# a clear block above the head now, which is also where the winch and the beacon live on a real
# one.
PSIG_STYLES = {
    "arm": {"mast_top": PSIG_PIVOT_Y + 14.0, "arm": PSIG_ARM_TIP_Z, "heavy": True},
    "mast": {"mast_top": 7 * 16.0 + 8.0, "arm": None, "heavy": False},
    "ped": {"mast_top": 4 * 16.0 + 8.0, "arm": None, "heavy": False},
}

# --- the vertical panel --------------------------------------------------------------------------
# The narrow striped panel on a post: a channelizing device for places too tight for a barricade,
# and the one MUTCD device whose whole job is its own narrowness -- a lane shift beside a bridge
# parapet or a barrier wall where a Type I would not fit.
#
# Its stripes slope toward the side traffic should pass, so it comes in a keep-left and a
# keep-right the way the barricades and the channelizer-cades do.
VPANEL_POST_HALF = 0.52
VPANEL_TOP = 14.20
VPANEL_PANEL = (2.10, 4.30, 13.60)   # half width, y0, y1 of the striped panel
VPANEL_PANEL_HALF_Z = 0.24
VPANEL_FOOT = (1.65, 0.62, 2.30)     # half x, height, half z of the moulded foot
# The post stands BEHIND the panel, not through it: the striped face is what points at traffic
# and a post in front of it interrupts the stripes. It starts just inside the panel's back face
# rather than flush against it, so the two never share a plane.
VPANEL_POST_CZ = AXIS + VPANEL_PANEL_HALF_Z + VPANEL_POST_HALF - 0.10
VPANEL_W = VPANEL_PANEL[0] * 2.0
VPANEL_H = VPANEL_PANEL[2] - VPANEL_PANEL[1]

# --- the steel road plate ------------------------------------------------------------------------
# The plate laid over an open trench so traffic can cross it before the trench is backfilled. It
# is the one device here that is meant to be DRIVEN over rather than steered around, so it is
# nearly the full cell and barely off the road.
# It spans its cell exactly and joins its neighbours on all four sides, so a patch of plates is
# one steel surface rather than a grid of tiles with seams down it.
PLATE_SPAN = 16.00
PLATE_Y = 0.85
PLATE_TEX_SIZE = 64
PLATE_TREAD = 2.10        # pitch of the raised diamond tread, in world units
PLATE_RUST_BLOB = 1.30    # how big a rust stain is, in world units
PLATE_SIDES = (("north", (0, 0, -1)), ("south", (0, 0, 1)),
               ("west", (-1, 0, 0)), ("east", (1, 0, 0)))

# --- the orange safety fence ---------------------------------------------------------------------
# The plastic mesh barrier fence that closes off the work area itself rather than channelizing
# traffic. Its panel is a plane with the mesh cut out of the texture, not modelled: a mesh built
# out of geometry is hundreds of faces for something read at two texels.
#
# The panel spans the whole cell so a run is continuous, and the stakes are inset from the edges
# rather than sitting on them, so where two panels meet the joint shows the pair of stakes a real
# run has instead of one shared post.
FENCE_PANEL = (1.20, 13.50)   # y0, y1 of the mesh
FENCE_HALF_Z = 0.06           # the two faces sit either side of the centre, not on it
FENCE_POST_X = (0.90, 15.10)
FENCE_POST_HALF = 0.42
FENCE_POST_TOP = 14.20
FENCE_TEX_SIZE = 128
FENCE_MESH = 1.55             # pitch of the mesh, in world units
FENCE_STRAND = 0.42           # how much of that pitch is plastic rather than hole

# --- the temporary pavement markers --------------------------------------------------------------
# The small folded plastic tabs taped down a lane line while the permanent markings are missing: a
# flat foot glued to the road, a panel standing up off the back of it, and a beaded reflective
# strip along the panel's top edge under a moulded lip.
#
# Unlike the zebra delineator these do NOT span their cell. Real ones are set out at intervals
# with clear road between them, so one small marker per block already gives the spacing a line of
# them is supposed to have; stretching them to touch would turn a dotted line into a solid one.
#
# They DO take all eight facings, for the same reason the zebra delineator does: a line of them
# marks a lane edge, and a lane edge does not always run with the block grid. A dotted line that
# can only lie north-south or east-west has to staircase across a diagonal instead of following
# it, which is worse on something this small than on something long.
# Everything below is authored at a size that reads clearly on screen and then taken down by
# MARKER_SCALE, because it read as too big beside the devices it shares a road with. The scale is
# on the design dimensions only -- the clearances in the builder that stop two pieces sharing a
# plane are absolute, and shrinking those with the shape would eventually bring them back into
# one plane.
MARKER_SCALE = 0.70
# The markers get a finer texture than the rest of the family. Their reflective strip is a
# little over half a world unit tall, which at the shared 32px size is barely one texel row --
# thin enough that the strip all but disappears and its emissive companion has nothing to
# cover. At 128 it is nearly five rows, which is enough to read as applied sheeting.
MARKER_TEX_SIZE = 128
MARKER_HALF_X = 3.60 * MARKER_SCALE            # half the marker's width
MARKER_PANEL_TOP = 3.15 * MARKER_SCALE         # top of the upright panel
# The panel stands across the back of the foot, and the foot reaches forward from it.
MARKER_PANEL_Z = (AXIS + 0.85 * MARKER_SCALE, AXIS + 1.20 * MARKER_SCALE)
MARKER_FOOT_Z0 = AXIS - 2.40 * MARKER_SCALE
MARKER_FOOT_Y = 0.26 * MARKER_SCALE
MARKER_LIP_H = 0.85 * MARKER_SCALE   # the reflective strip's height, at the top of the panel
MARKER_LIP_Z0 = AXIS + 0.62 * MARKER_SCALE     # the lip stands a little proud of the panel face
MARKER_BAND = (MARKER_PANEL_TOP - MARKER_LIP_H, MARKER_PANEL_TOP)

# --- the sand barrel ---------------------------------------------------------------------------
SAND_BARREL_HEIGHT = 13.80
SAND_BARREL_SIDES = 16
SAND_BARREL_PROFILE = [
    (7.10, 0.00),
    (7.10, 0.60),
    (6.90, 1.00),
    (6.40, 5.50),
    (5.70, 10.60),
    (5.55, 12.20),
    (5.75, 12.60),   # the lid's overhanging rim
    (5.75, 13.15),
    (5.30, 13.45),
    (5.30, SAND_BARREL_HEIGHT),
]
SAND_BARREL_LID_R = 5.30


# --- mesh ---------------------------------------------------------------------------------------
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
        """verts: three (position, uv, normal) triples. Forge culls back faces, so the triangle
        is wound to face its analytic vertex normals rather than to whatever order it was given
        in -- a face can then never come out inside-out."""
        pts = [p for (p, _uv, _n) in verts]
        nrm = [n for (_p, _uv, n) in verts]
        geo = vcross(vsub(pts[1], pts[0]), vsub(pts[2], pts[0]))
        want = vadd(vadd(nrm[0], nrm[1]), nrm[2])
        agree = vdot(geo, want)
        if abs(agree) < 1e-12:
            raise AssertionError("degenerate triangle or normal: %r" % (pts,))
        uvs = [t for (_p, t, _n) in verts]
        if agree < 0:
            pts.reverse()
            nrm.reverse()
            uvs.reverse()
        idx = []
        for p, t, n in zip(pts, uvs, nrm):
            idx.append((self._add(self.v, self._vi, p),
                        self._add(self.vt, self._ti, t),
                        self._add(self.vn, self._ni, n)))
        self.f.append(idx)

    def quad(self, a, b, c, d):
        self.tri([a, b, c])
        self.tri([a, c, d])

    def quad_out(self, pts, normal, uvs):
        geo = vcross(vsub(pts[1], pts[0]), vsub(pts[2], pts[0]))
        if vdot(geo, normal) < 0:
            pts = list(reversed(pts))
            uvs = list(reversed(uvs))
        n = vnorm(normal)
        self.quad(*[(p, t, n) for p, t in zip(pts, uvs)])

    def write(self, path, name, mtl_file):
        lines = ["# Procedurally generated by dev-env-utils/scripts/gen_work_zone_devices.py"
                 " -- do not hand edit",
                 "mtllib %s" % mtl_file,
                 "o %s" % name]
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
    length = math.sqrt(vdot(a, a))
    return (a[0] / length, a[1] / length, a[2] / length) if length > 0 else (0.0, 1.0, 0.0)


# How many 1/16 units of height the band strip spans, for the device currently being generated.
#
# Forge's OBJ loader REFUSES a model with any vt outside 0-1 and substitutes the missing-model
# placeholder, so a device taller than its span silently turns into a purple and black box in
# game. Nothing offline catches it: the audit tool checks geometry and the previewer does its own
# UV maths. Hence a span per device rather than a hard 16, plus the assertion in ``check_uvs``.
V_SPAN = 16.0


def set_v_span(span):
    """Set the height the band strip spans for the device about to be generated. Both the mesh
    UVs and the texture read this, so a band stays at the height it is drawn at."""
    global V_SPAN
    V_SPAN = float(span)


def uv_at(u, y):
    """UV for a point at height ``y`` on the wrapped body.

    ``flip-v`` is on, so the OBJ's v is written as ``y / V_SPAN`` and the sprite row that lands at
    height y is the one drawn there -- the texture reads the same way up as the device stands.
    ``u`` is squeezed into the strip left of the swatch column.
    """
    return (u * SWATCH_U0, y / V_SPAN)


def uv_swatch(v):
    """UV of a flat swatch, for a face that has no height to read a band from."""
    return (SWATCH_U0 + (1.0 - SWATCH_U0) * 0.5, v)


# --- primitives (standing frame, Y up, axis through the block centre) ---------------------------


def ring_angles(sides):
    """Vertex angles offset by half a step, so a flat facet faces each compass direction."""
    step = 2.0 * math.pi / sides
    return [step * (i + 0.5) for i in range(sides)]


def lathe(mesh, profile, sides=16, cx=AXIS, cz=AXIS, swatch_v=None):
    """Surface of revolution about the vertical line (cx, *, cz), bottom up. Normals come from
    the profile's slope, so a rib shades as a rib and a dome as a dome.

    ``swatch_v`` paints the whole surface one flat swatch instead of reading the band strip at
    each height. That is what a rubber mat wants: it has no bands of its own, and on a device
    whose texture is a striped panel rather than a vertical strip, reading the strip would wrap
    the panel's stripes around the mat.
    """
    def surface_uv(u, y):
        return uv_swatch(swatch_v) if swatch_v is not None else uv_at(u, y)
    angles = ring_angles(sides)
    rings = []
    for k, (r, y) in enumerate(profile):
        prev_r, prev_y = profile[max(k - 1, 0)]
        next_r, next_y = profile[min(k + 1, len(profile) - 1)]
        dr, dy = next_r - prev_r, next_y - prev_y
        if abs(dr) < 1e-9 and abs(dy) < 1e-9:
            dr, dy = 0.0, 1.0
        nr, ny = dy, -dr
        length = math.hypot(nr, ny)
        nr, ny = nr / length, ny / length
        rings.append([((cx + r * math.cos(a), y, cz + r * math.sin(a)),
                       (nr * math.cos(a), ny, nr * math.sin(a))) for a in angles])
    for k in range(len(profile) - 1):
        (ra, ya), (rb, yb) = profile[k], profile[k + 1]
        if abs(ya - yb) < 1e-9 and abs(ra - rb) < 1e-9:
            continue
        if abs(ra) < 1e-9 and abs(rb) < 1e-9:
            continue
        lo, hi = rings[k], rings[k + 1]
        for i in range(sides):
            j = (i + 1) % sides
            ua, ub = i / sides, (i + 1) / sides
            if rb < 1e-9:
                mesh.tri([(lo[i][0], surface_uv(ua, ya), lo[i][1]),
                          (lo[j][0], surface_uv(ub, ya), lo[j][1]),
                          ((cx, yb, cz), surface_uv(0.5 * (ua + ub), yb), hi[i][1])])
            elif ra < 1e-9:
                mesh.tri([((cx, ya, cz), surface_uv(0.5 * (ua + ub), ya), lo[i][1]),
                          (hi[j][0], surface_uv(ub, yb), hi[j][1]),
                          (hi[i][0], surface_uv(ua, yb), hi[i][1])])
            else:
                mesh.quad((lo[i][0], surface_uv(ua, ya), lo[i][1]),
                          (lo[j][0], surface_uv(ub, ya), lo[j][1]),
                          (hi[j][0], surface_uv(ub, yb), hi[j][1]),
                          (hi[i][0], surface_uv(ua, yb), hi[i][1]))


def disc(mesh, r, y, normal, swatch_v, sides=16, cx=AXIS, cz=AXIS):
    """A flat cap, wearing one flat swatch rather than a slice of the band strip."""
    angles = ring_angles(sides)
    n = vnorm(normal)
    t = uv_swatch(swatch_v)
    centre = ((cx, y, cz), t, n)
    for i in range(sides):
        j = (i + 1) % sides
        pa = (cx + r * math.cos(angles[i]), y, cz + r * math.sin(angles[i]))
        pb = (cx + r * math.cos(angles[j]), y, cz + r * math.sin(angles[j]))
        tri = [centre, (pa, t, n), (pb, t, n)]
        if vdot(vcross(vsub(pa, centre[0]), vsub(pb, centre[0])), n) < 0:
            tri = [centre, (pb, t, n), (pa, t, n)]
        mesh.tri(tri)


def square_frustum(mesh, profile, cx=AXIS, cz=AXIS):
    """Stack of square sections about (cx, *, cz): ``profile`` is (half-width, y) bottom up."""
    outward = [(0, 0, 1), (1, 0, 0), (0, 0, -1), (-1, 0, 0)]

    def corner(h, y, s, end):
        if s == 0:
            return (cx - h, y, cz + h) if end == 0 else (cx + h, y, cz + h)
        if s == 1:
            return (cx + h, y, cz + h) if end == 0 else (cx + h, y, cz - h)
        if s == 2:
            return (cx + h, y, cz - h) if end == 0 else (cx - h, y, cz - h)
        return (cx - h, y, cz - h) if end == 0 else (cx - h, y, cz + h)

    for k in range(len(profile) - 1):
        (ha, ya), (hb, yb) = profile[k], profile[k + 1]
        if abs(ha - hb) < 1e-9 and abs(ya - yb) < 1e-9:
            continue
        if abs(ya - yb) < 1e-9:
            n = (0, 1, 0) if hb < ha else (0, -1, 0)
            t = uv_swatch(SWATCH_DARK_V)
            for s in range(4):
                pts = [corner(ha, ya, s, 0), corner(ha, ya, s, 1),
                       corner(hb, yb, s, 1), corner(hb, yb, s, 0)]
                mesh.quad_out(pts, n, [t, t, t, t])
            continue
        dh, dy = hb - ha, yb - ya
        for s in range(4):
            ox, _oy, oz = outward[s]
            n = vnorm((ox * dy, -dh, oz * dy))
            pts = [corner(ha, ya, s, 0), corner(ha, ya, s, 1),
                   corner(hb, yb, s, 1), corner(hb, yb, s, 0)]
            uvs = [uv_at(0.0, ya), uv_at(1.0, ya), uv_at(1.0, yb), uv_at(0.0, yb)]
            mesh.quad_out(pts, n, uvs)


def square_cap(mesh, half, y, normal, swatch_v, cx=AXIS, cz=AXIS):
    t = uv_swatch(swatch_v)
    mesh.quad_out([(cx - half, y, cz - half), (cx + half, y, cz - half),
                   (cx + half, y, cz + half), (cx - half, y, cz + half)],
                  normal, [t, t, t, t])


def box(mesh, x0, x1, y0, y1, z0, z1, swatch_v, faces=("x-", "x+", "y-", "y+", "z-", "z+")):
    """Axis-aligned box wearing one flat swatch, for handles, brackets and lamp housings."""
    t = uv_swatch(swatch_v)
    q = [t, t, t, t]
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


def prism(mesh, bottom, top, swatch_v, ends=("bottom", "top")):
    """A six-sided solid between two matching quads, for a member that leans.

    ``box`` only makes axis-aligned solids, and a leg that leans is the one thing here that is
    not. Each face's outward direction is taken from the solid's own centre rather than named, so
    the winding comes out right whichever way the solid leans.

    ``ends`` names which of the two end caps to draw. Drop one where two prisms meet on a shared
    quad -- a bent member built as two prisms that both cap the joint puts two faces in the same
    plane, which z-fights, and capping neither leaves the joint closed by the solids themselves.
    """
    t = uv_swatch(swatch_v)
    q = [t, t, t, t]
    pts_all = list(bottom) + list(top)
    centre = tuple(sum(p[i] for p in pts_all) / 8.0 for i in range(3))

    def face(pts):
        n = vcross(vsub(pts[1], pts[0]), vsub(pts[2], pts[0]))
        mid = tuple(sum(p[i] for p in pts) / len(pts) for i in range(3))
        if vdot(n, vsub(mid, centre)) < 0:
            n = (-n[0], -n[1], -n[2])
        mesh.quad_out(list(pts), n, q)

    if "bottom" in ends:
        face(list(bottom))
    if "top" in ends:
        face(list(top))
    for i in range(4):
        j = (i + 1) % 4
        face([bottom[i], bottom[j], top[j], top[i]])


def slotted_tab(mesh, spec, swatch_v, cx=AXIS, cz=AXIS):
    """The moulded grip handle every drum and channelizer carries: an upright flat tab with a
    slot through it, built as the four bars around the slot so the slot is a real hole rather
    than a painted-on rectangle."""
    x0, x1 = cx - spec["half_x"], cx + spec["half_x"]
    sx0, sx1 = cx - spec["slot_half_x"], cx + spec["slot_half_x"]
    z0, z1 = cz - spec["half_z"], cz + spec["half_z"]
    y0, y1 = spec["y0"], spec["y1"]
    sy0, sy1 = spec["slot_y0"], spec["slot_y1"]
    box(mesh, x0, sx0, y0, y1, z0, z1, swatch_v)          # left upright
    box(mesh, sx1, x1, y0, y1, z0, z1, swatch_v)          # right upright
    box(mesh, sx0, sx1, y0, sy0, z0, z1, swatch_v)        # below the slot
    box(mesh, sx0, sx1, sy1, y1, z0, z1, swatch_v)        # above the slot


def lens_disc(mesh, cx, cy, z, r, normal, swatch_v, sides=12):
    """A round lens face in the XY plane, facing along z."""
    angles = ring_angles(sides)
    n = vnorm(normal)
    t = uv_swatch(swatch_v)
    centre = ((cx, cy, z), t, n)
    for i in range(sides):
        j = (i + 1) % sides
        pa = (cx + r * math.cos(angles[i]), cy + r * math.sin(angles[i]), z)
        pb = (cx + r * math.cos(angles[j]), cy + r * math.sin(angles[j]), z)
        tri = [centre, (pa, t, n), (pb, t, n)]
        if vdot(vcross(vsub(pa, centre[0]), vsub(pb, centre[0])), n) < 0:
            tri = [centre, (pb, t, n), (pa, t, n)]
        mesh.tri(tri)


def lens_rim(mesh, cx, cy, z0, z1, r, swatch_v, sides=12):
    """The cylindrical wall between two lens faces."""
    angles = ring_angles(sides)
    t = uv_swatch(swatch_v)
    for i in range(sides):
        j = (i + 1) % sides
        ax, ay = math.cos(angles[i]), math.sin(angles[i])
        bx, by = math.cos(angles[j]), math.sin(angles[j])
        pa0 = (cx + r * ax, cy + r * ay, z0)
        pb0 = (cx + r * bx, cy + r * by, z0)
        pb1 = (cx + r * bx, cy + r * by, z1)
        pa1 = (cx + r * ax, cy + r * ay, z1)
        na, nb = (ax, ay, 0.0), (bx, by, 0.0)
        mesh.quad((pa0, t, na), (pb0, t, nb), (pb1, t, nb), (pa1, t, na))


# --- parts ---------------------------------------------------------------------------------------
def build_cone(mesh):
    square_frustum(mesh, CONE_BASE_PROFILE)
    square_cap(mesh, CONE_BASE_HALF, 0.0, (0, -1, 0), SWATCH_DARK_V)
    # The base's top face. The cone body stands on it and hides the middle, but the ring of it
    # outside the body's foot is visible, and without this the base is an open box that can be
    # seen into from above.
    square_cap(mesh, CONE_BASE_PROFILE[-1][0], CONE_BASE_TOP, (0, 1, 0), SWATCH_DARK_V)
    lathe(mesh, CONE_PROFILE, sides=CONE_SIDES)
    disc(mesh, CONE_PROFILE[-1][0], CONE_HEIGHT, (0, 1, 0), SWATCH_BASE_V, sides=CONE_SIDES)


def transform_mesh(mesh, xf, nxf):
    """Rewrite a finished mesh through a rigid transform.

    Only proper rotations belong here. Winding was already resolved against each triangle's
    normals when it was added, so a rotation carries both round together and stays consistent; a
    mirror would flip the geometry without flipping the normals and turn every face inside out.
    """
    mesh.v[:] = [xf(p) for p in mesh.v]
    mesh.vn[:] = [nxf(n) for n in mesh.vn]


def scale_mesh(mesh, factor, cx=AXIS, cz=AXIS):
    """Scale a finished mesh about (cx, 0, cz), so it grows upward from the floor and outward
    from the block's axis. Uniform, so normals and UVs are both unaffected."""
    mesh.v[:] = [(cx + (p[0] - cx) * factor, p[1] * factor, cz + (p[2] - cz) * factor)
                 for p in mesh.v]


def rest_on_floor(mesh, cx=AXIS, cz=AXIS):
    """Drop a mesh so its lowest point sits on y=0, and centre it in its cell."""
    minx, miny, minz, maxx, _maxy, maxz = mesh_bounds(mesh)
    dx = cx - 0.5 * (minx + maxx)
    dz = cz - 0.5 * (minz + maxz)
    mesh.v[:] = [(p[0] + dx, p[1] - miny, p[2] + dz) for p in mesh.v]


def build_cone_knocked(mesh):
    """A cone on its side, as one does end up after a truck clips it.

    Built by laying the standing cone down rather than by modelling a second one, so it stays the
    same cone: any change to the profile or the collars shows up in both.
    """
    build_cone(mesh)
    transform_mesh(mesh,
                   lambda p: (p[0], 16.0 - p[2], p[1]),
                   lambda n: (n[0], -n[2], n[1]))
    rest_on_floor(mesh)


def drum_taper(y):
    """Radius of the drum's body at height y, before any rib."""
    span = DRUM_BODY_Y1 - DRUM_MAT_TOP
    t = max(0.0, min(1.0, (y - DRUM_MAT_TOP) / span))
    return DRUM_BOTTOM_R + (DRUM_TOP_R - DRUM_BOTTOM_R) * t


def drum_body_profile():
    """The tapered body with a rib bulge at each stripe boundary."""
    pts = [(drum_taper(DRUM_MAT_TOP), DRUM_MAT_TOP)]
    for yr in DRUM_RIBS:
        pts.append((drum_taper(yr - 0.30), yr - 0.30))
        pts.append((drum_taper(yr) + DRUM_RIB, yr - 0.13))
        pts.append((drum_taper(yr) + DRUM_RIB, yr + 0.13))
        pts.append((drum_taper(yr + 0.30), yr + 0.30))
    pts.append((drum_taper(DRUM_BODY_Y1), DRUM_BODY_Y1))
    pts.append((DRUM_LID_R + 0.20, DRUM_BODY_Y1 + 0.45))   # the shoulder into the lid
    pts.append((DRUM_LID_R, DRUM_LID_Y))
    return sorted(pts, key=lambda p: p[1])


def build_drum_mat(mesh, r, top, sides):
    """The wide thin rubber mat every drum and channelizer stands on.

    Painted entirely from the black swatch. Reading the band strip here would work on a device
    whose texture happens to be black at mat height, and wrap a striped panel around the mat on
    one whose texture is not.
    """
    lathe(mesh, [(r, 0.0), (r, top * 0.55), (r - 0.55, top)], sides=sides,
          swatch_v=SWATCH_DARK_V)
    disc(mesh, r, 0.0, (0, -1, 0), SWATCH_DARK_V, sides=sides)
    disc(mesh, r - 0.55, top, (0, 1, 0), SWATCH_DARK_V, sides=sides)


def build_drum(mesh, lit=False):
    build_drum_mat(mesh, DRUM_MAT_R, DRUM_MAT_TOP, DRUM_SIDES)
    lathe(mesh, drum_body_profile(), sides=DRUM_SIDES)
    disc(mesh, DRUM_LID_R, DRUM_LID_Y, (0, 1, 0), SWATCH_BASE_V, sides=DRUM_SIDES)
    slotted_tab(mesh, DRUM_HANDLE, SWATCH_BASE_V)
    if lit:
        lamp = DRUM_LAMP
        cx = AXIS + lamp["post_x"]
        box(mesh, cx - lamp["post_half"], cx + lamp["post_half"],
            DRUM_LID_Y - 0.15, lamp["post_y1"],
            AXIS - lamp["post_half"], AXIS + lamp["post_half"], SWATCH_BAND_V)
        box(mesh, cx - lamp["body_half_x"], cx + lamp["body_half_x"],
            lamp["body_y0"], lamp["body_y1"],
            AXIS - lamp["body_half_z"], AXIS + lamp["body_half_z"], SWATCH_BAND_V)
        # The neck between the housing and the lens body, drawn before the lens so the lens
        # covers where it enters.
        neck_top = lamp["lens_cy"] - lamp["rim_r"] * 0.55
        assert neck_top > lamp["body_y1"], "lamp neck inverted: lens sits inside its housing"
        # Started inside the housing and with no bottom face: a neck that begins exactly on the
        # housing's top face puts two coplanar faces in the same plane, which z-fights.
        box(mesh, cx - lamp["neck_half_x"], cx + lamp["neck_half_x"],
            lamp["body_y1"] - 0.20, neck_top,
            AXIS - lamp["neck_half_z"], AXIS + lamp["neck_half_z"], SWATCH_BAND_V,
            faces=("x-", "x+", "y+", "z-", "z+"))
        z0, z1 = AXIS - lamp["lens_half_z"], AXIS + lamp["lens_half_z"]
        lens_rim(mesh, cx, lamp["lens_cy"], z0, z1, lamp["rim_r"], SWATCH_BAND_V)
        lens_disc(mesh, cx, lamp["lens_cy"], z1, lamp["lens_r"], (0, 0, 1), SWATCH_ACCENT_V)
        lens_disc(mesh, cx, lamp["lens_cy"], z0, lamp["lens_r"], (0, 0, -1), SWATCH_ACCENT_V)


def channelizer_profile():
    """The stepped column: each section is a straight cylinder, joined to the next by a rib that
    stands proud of both."""
    pts = []
    for k, (r, y0, y1) in enumerate(CHANNELIZER_SECTIONS):
        if k == 0:
            pts.append((r + 0.22, y0))          # the flared skirt at the foot
            pts.append((r, y0 + 0.45))
        else:
            pts.append((r + CHANNELIZER_STEP, y0 - 0.30))
            pts.append((r + CHANNELIZER_STEP, y0 - 0.05))
            pts.append((r, y0 + 0.20))
        pts.append((r, y1))
        if k < len(CHANNELIZER_SECTIONS) - 1:
            nxt = CHANNELIZER_SECTIONS[k + 1][0]
            pts.append((nxt + CHANNELIZER_STEP, y1 + 0.22))
    top_r = CHANNELIZER_SECTIONS[-1][0]
    top_y = CHANNELIZER_SECTIONS[-1][2]
    pts.append((CHANNELIZER_NECK_R + 0.30, top_y + 0.35))
    pts.append((CHANNELIZER_NECK_R, top_y + 0.60))
    pts.append((CHANNELIZER_NECK_R, CHANNELIZER_NECK_Y))
    del top_r
    return sorted(pts, key=lambda p: p[1])


def build_channelizer(mesh):
    build_drum_mat(mesh, CHANNELIZER_MAT_R, CHANNELIZER_MAT_TOP, CHANNELIZER_MAT_SIDES)
    lathe(mesh, channelizer_profile(), sides=CHANNELIZER_SIDES)
    disc(mesh, CHANNELIZER_NECK_R, CHANNELIZER_NECK_Y, (0, 1, 0), SWATCH_BASE_V,
         sides=CHANNELIZER_SIDES)
    slotted_tab(mesh, CHANNELIZER_GRIP, SWATCH_BASE_V)


def build_channelizer_cade(mesh):
    """The frame, its foot, and the striped panel inside it.

    The frame is drawn as the four bars around the panel rather than as a slab with a picture of
    a frame on it, so the panel really is inset and the head's grip slots are real holes.
    """
    build_drum_mat(mesh, CADE_MAT_R, CADE_MAT_TOP, CADE_MAT_SIDES)

    hx, hz = CADE_FRAME_HALF_X, CADE_FRAME_HALF_Z
    x0, x1 = AXIS - hx, AXIS + hx
    z0, z1 = AXIS - hz, AXIS + hz
    rail = CADE_FRAME_RAIL
    p_half, p_y0, p_y1 = CADE_PANEL

    # The moulded foot the frame stands in, wider than the frame itself.
    box(mesh, x0 - 0.45, x1 + 0.45, CADE_MAT_TOP, CADE_FOOT_Y1,
        z0 - 0.40, z1 + 0.40, SWATCH_BAND_V)

    # Frame: two uprights, a head above the panel and a sill below it.
    box(mesh, x0, AXIS - p_half, CADE_FOOT_Y1, CADE_FRAME_Y1, z0, z1, SWATCH_BAND_V)
    box(mesh, AXIS + p_half, x1, CADE_FOOT_Y1, CADE_FRAME_Y1, z0, z1, SWATCH_BAND_V)
    box(mesh, AXIS - p_half, AXIS + p_half, CADE_FOOT_Y1, p_y0, z0, z1, SWATCH_BAND_V)

    # Head, split around the two grip slots.
    slot = CADE_HEAD_SLOT
    inner = slot["gap"]
    sx = slot["half_x"]
    head_y0, head_y1 = p_y1, CADE_FRAME_Y1
    box(mesh, AXIS - p_half, AXIS + p_half, head_y0, slot["y0"], z0, z1, SWATCH_BAND_V)
    box(mesh, AXIS - p_half, AXIS + p_half, slot["y1"], head_y1, z0, z1, SWATCH_BAND_V)
    box(mesh, AXIS - p_half, AXIS - inner - sx, slot["y0"], slot["y1"], z0, z1, SWATCH_BAND_V)
    box(mesh, AXIS - inner, AXIS + inner, slot["y0"], slot["y1"], z0, z1, SWATCH_BAND_V)
    box(mesh, AXIS + inner + sx, AXIS + p_half, slot["y0"], slot["y1"], z0, z1, SWATCH_BAND_V)

    # The panel itself: a thin slab sitting inside the frame, striped on both faces. Its UVs map
    # the whole striped region of the texture onto the panel rectangle, which is what lets one
    # square sprite carry a tall panel's stripes without stretching them off the diagonal.
    pz0, pz1 = AXIS - CADE_PANEL_HALF_Z, AXIS + CADE_PANEL_HALF_Z
    for z, normal in ((pz1, (0, 0, 1)), (pz0, (0, 0, -1))):
        pts = [(AXIS - p_half, p_y0, z), (AXIS + p_half, p_y0, z),
               (AXIS + p_half, p_y1, z), (AXIS - p_half, p_y1, z)]
        uvs = [(0.0, 0.0), (SWATCH_U0, 0.0), (SWATCH_U0, 1.0), (0.0, 1.0)]
        mesh.quad_out(pts, normal, uvs)


def _barricade_upright(mesh, cx, top_y):
    """One leg with the splayed foot it stands on, centred at ``cx``."""
    box(mesh, cx - BARRICADE_LEG_HALF_X - 0.35, cx + BARRICADE_LEG_HALF_X + 0.35,
        0.0, BARRICADE_FOOT_Y1,
        AXIS - BARRICADE_FOOT_HALF_Z, AXIS + BARRICADE_FOOT_HALF_Z, SWATCH_BAND_V)
    box(mesh, cx - BARRICADE_LEG_HALF_X, cx + BARRICADE_LEG_HALF_X,
        BARRICADE_FOOT_Y1, top_y,
        AXIS - BARRICADE_LEG_HALF_Z, AXIS + BARRICADE_LEG_HALF_Z, SWATCH_BAND_V)


def build_barricade_core(mesh, rails, top_y):
    """The rails, and the upright on the LEFT edge of the cell.

    This piece is drawn by every barricade, connected or not, which is what puts exactly one
    upright on each seam of a run: the block to the left of a seam omits its right upright and
    the block to the right always draws its left one. Drawing the upright at both ends and
    hiding both at a seam would leave the joint with no leg at all.

    Each rail's two faces map the whole striped region of the texture onto the rail rectangle,
    so the stripes run across it at the angle the rail's own proportions give them. The legs and
    feet come from the flat white swatch instead, so a barricade shows stripes only where a real
    one carries sheeting.
    """
    _barricade_upright(mesh, AXIS - BARRICADE_LEG_X, top_y)

    hz = BARRICADE_RAIL_HALF_Z
    hx = BARRICADE_RAIL_HALF_X
    for (y0, y1) in rails:
        # The rail's edges, from the swatch; its two broad faces carry the stripes. The end faces
        # are left off: on a connected run they would sit inside the neighbour's rail.
        box(mesh, AXIS - hx, AXIS + hx, y0, y1, AXIS - hz, AXIS + hz, SWATCH_BAND_V,
            faces=("y-", "y+"))
        for z, normal in ((AXIS + hz, (0, 0, 1)), (AXIS - hz, (0, 0, -1))):
            pts = [(AXIS - hx, y0, z), (AXIS + hx, y0, z),
                   (AXIS + hx, y1, z), (AXIS - hx, y1, z)]
            uvs = [(0.0, 0.0), (SWATCH_U0, 0.0), (SWATCH_U0, 1.0), (0.0, 1.0)]
            mesh.quad_out(pts, normal, uvs)


def build_barricade_fill(mesh, rails, top_y):
    """The rails carried on past the cell's right-hand edge to meet a DIAGONAL neighbour.

    No upright: the neighbour draws its own left-hand one, which is what keeps exactly one leg on
    each joint of a run however it is laid.

    The stripe continues across the filler by wrapping to the near edge of the same sprite, the
    same trick the free end's overhang uses -- a fresh mapping would visibly restart the pattern
    at the cell boundary, which is precisely where a run is supposed to look continuous.
    """
    hz = BARRICADE_RAIL_HALF_Z
    inner = AXIS + BARRICADE_RAIL_HALF_X
    outer = inner + DIAGONAL_GAP
    fraction = DIAGONAL_GAP / (BARRICADE_RAIL_HALF_X * 2.0)
    u_inner, u_outer = 0.0, SWATCH_U0 * fraction
    for (y0, y1) in rails:
        box(mesh, inner, outer, y0, y1, AXIS - hz, AXIS + hz, SWATCH_BAND_V,
            faces=("y-", "y+"))
        for z, normal in ((AXIS + hz, (0, 0, 1)), (AXIS - hz, (0, 0, -1))):
            pts = [(inner, y0, z), (outer, y0, z), (outer, y1, z), (inner, y1, z)]
            uvs = [(u_inner, 0.0), (u_outer, 0.0), (u_outer, 1.0), (u_inner, 1.0)]
            mesh.quad_out(pts, normal, uvs)


def build_barricade_end(mesh, rails, top_y, left):
    """An UNCONNECTED end: the rails overhanging past the upright, capped, plus the upright
    itself on the right-hand side.

    A free end keeps the overhang a real barricade has; a connected one must not, because the
    neighbour's rail already occupies that space and two rails in one place z-fight.

    The overhang continues the stripe by wrapping to the far edge of the same sprite, which lands
    seamlessly only because the pitch puts a whole number of periods across the rail (see
    BARRICADE_STRIPE). Give it a fresh 0..1 mapping instead and the pattern visibly restarts at
    the upright.
    """
    sign = -1 if left else 1
    hz = BARRICADE_RAIL_HALF_Z
    inner = AXIS + sign * BARRICADE_RAIL_HALF_X
    outer = inner + sign * BARRICADE_RAIL_OVERHANG
    if not left:
        _barricade_upright(mesh, AXIS + BARRICADE_LEG_X, top_y)

    # The slice of the sprite the overhang wears, taken from the opposite edge so it continues.
    fraction = BARRICADE_RAIL_OVERHANG / (BARRICADE_RAIL_HALF_X * 2.0)
    if left:
        u_outer, u_inner = SWATCH_U0 * (1.0 - fraction), SWATCH_U0
    else:
        u_outer, u_inner = SWATCH_U0 * fraction, 0.0

    t = uv_swatch(SWATCH_BAND_V)
    for (y0, y1) in rails:
        box(mesh, min(inner, outer), max(inner, outer), y0, y1, AXIS - hz, AXIS + hz,
            SWATCH_BAND_V, faces=("y-", "y+"))
        for z, normal in ((AXIS + hz, (0, 0, 1)), (AXIS - hz, (0, 0, -1))):
            pts = [(outer, y0, z), (inner, y0, z), (inner, y1, z), (outer, y1, z)]
            uvs = [(u_outer, 0.0), (u_inner, 0.0), (u_inner, 1.0), (u_outer, 1.0)]
            mesh.quad_out(pts, normal, uvs)
        # Cap the very end, so a free end is not an open box.
        pts = [(outer, y0, AXIS - hz), (outer, y0, AXIS + hz),
               (outer, y1, AXIS + hz), (outer, y1, AXIS - hz)]
        mesh.quad_out(pts, (sign, 0, 0), [t, t, t, t])


def _fold_leg_z(y):
    """Where a rear leg's centre line is at height ``y``, from the hinge down to the floor."""
    return FOLD_FRAME_CZ + FOLD_LEG_SPLAY * (1.0 - y / FOLD_HINGE_Y)


def build_barricade_folding(mesh):
    """The Type II folding barricade: a two-rail panel on a pair of legs that swing out behind.

    The panel is built on the block axis and the whole assembly shifted forward at the end, so
    the device is centred in its cell rather than crowded into the back of it. FOLD_RAIL_CZ is
    where that leaves the panel, and it is what the renderer mounts lights and signs against.
    """
    # The uprights, each on a moulded foot. The upright starts inside the foot and skips its own
    # underside, so the two do not meet in one plane.
    for cx in (AXIS - FOLD_UPRIGHT_X, AXIS + FOLD_UPRIGHT_X):
        box(mesh, cx - FOLD_FOOT_HALF_X, cx + FOLD_FOOT_HALF_X, 0.0, FOLD_FOOT_Y,
            FOLD_FRAME_CZ - FOLD_FOOT_HALF_Z, FOLD_FRAME_CZ + FOLD_FOOT_HALF_Z, SWATCH_BAND_V)
        box(mesh, cx - FOLD_UPRIGHT_HALF_X, cx + FOLD_UPRIGHT_HALF_X, FOLD_FOOT_Y - 0.20,
            FOLD_TOP, FOLD_FRAME_Z0, FOLD_FRAME_Z1, SWATCH_BAND_V,
            faces=("x-", "x+", "y+", "z-", "z+"))

    # The rails. Their two broad faces carry the stripes; the ends and edges come from the flat
    # swatch, so this shows sheeting only where a real one carries sheeting.
    hx = FOLD_RAIL_HALF_X
    hz = FOLD_RAIL_HALF_Z
    for (y0, y1) in FOLD_RAILS:
        box(mesh, AXIS - hx, AXIS + hx, y0, y1, AXIS - hz, AXIS + hz, SWATCH_BAND_V,
            faces=("x-", "x+", "y-", "y+"))
        for z, normal in ((AXIS + hz, (0, 0, 1)), (AXIS - hz, (0, 0, -1))):
            pts = [(AXIS - hx, y0, z), (AXIS + hx, y0, z),
                   (AXIS + hx, y1, z), (AXIS - hx, y1, z)]
            uvs = [(0.0, 0.0), (SWATCH_U0, 0.0), (SWATCH_U0, 1.0), (0.0, 1.0)]
            mesh.quad_out(pts, normal, uvs)

    # The rear legs, and the pad each stands on.
    lx = FOLD_LEG_HALF_X
    lz = FOLD_LEG_HALF_Z
    for cx in (AXIS - FOLD_UPRIGHT_X, AXIS + FOLD_UPRIGHT_X):
        z_top = _fold_leg_z(FOLD_HINGE_Y)
        z_bot = _fold_leg_z(FOLD_LEG_Y0)
        prism(mesh,
              [(cx - lx, FOLD_LEG_Y0, z_bot - lz), (cx + lx, FOLD_LEG_Y0, z_bot - lz),
               (cx + lx, FOLD_LEG_Y0, z_bot + lz), (cx - lx, FOLD_LEG_Y0, z_bot + lz)],
              [(cx - lx, FOLD_HINGE_Y, z_top - lz), (cx + lx, FOLD_HINGE_Y, z_top - lz),
               (cx + lx, FOLD_HINGE_Y, z_top + lz), (cx - lx, FOLD_HINGE_Y, z_top + lz)],
              SWATCH_BAND_V)
        pad_z = _fold_leg_z(0.0)
        box(mesh, cx - FOLD_PAD_HALF_X, cx + FOLD_PAD_HALF_X, 0.0, FOLD_PAD_Y,
            pad_z - FOLD_PAD_HALF_Z, pad_z + FOLD_PAD_HALF_Z, SWATCH_BAND_V)

    # The cross brace, run between the leg CENTRES so its ends finish inside them rather than
    # against them.
    brace_z = _fold_leg_z(FOLD_BRACE_Y)
    box(mesh, AXIS - FOLD_UPRIGHT_X, AXIS + FOLD_UPRIGHT_X,
        FOLD_BRACE_Y, FOLD_BRACE_Y + FOLD_BRACE_H,
        brace_z - 0.30, brace_z + 0.30, SWATCH_BAND_V)

    mesh.v[:] = [(p[0], p[1], p[2] + FOLD_SHIFT_Z) for p in mesh.v]
    _minx, _miny, minz, _maxx, _maxy, maxz = mesh_bounds(mesh)
    assert abs(0.5 * (minz + maxz) - AXIS) < 1e-6, (
        "folding barricade is not centred in its cell: %r" % ((minz, maxz),))


def build_arrow_board(mesh):
    """The arrow board's CHASSIS only, at the size it stands in the world.

    Everything above the chassis -- mast, panel and the lamp grid -- is drawn by
    ``TileEntityArrowBoardRenderer`` instead, because the lamps have to animate and a baked model
    cannot. Splitting it here rather than moving the whole board into the renderer keeps
    something solid in the world, and leaves the static part small enough to sit inside its own
    cell, where it cannot pop when its chunk section is culled. The tall part that would have
    popped is now tile entity geometry, which is culled by its own render bounding box instead.
    """
    _arrow_chassis(mesh)
    scale_mesh(mesh, ARROW_SCALE)


def _arrow_chassis(mesh):
    """The trailer, tongue, wheels and jacks, unscaled.

    Kept apart from ``build_arrow_board`` so the inventory model can start from the same
    chassis at a different size, rather than from a second copy of these numbers.
    """
    cx0, cx1, cy0, cy1, cz0, cz1 = ARROW_CHASSIS
    box(mesh, cx0, cx1, cy0, cy1, cz0, cz1, SWATCH_BAND_V)
    tx0, tx1, ty0, ty1, tz0, tz1 = ARROW_TONGUE
    box(mesh, tx0, tx1, ty0, ty1, tz0, tz1, SWATCH_BAND_V)

    for wx in ARROW_WHEEL_X:
        for wz in ARROW_WHEEL_Z:
            lens_rim(mesh, wx, ARROW_WHEEL_Y, wz - ARROW_WHEEL_HALF, wz + ARROW_WHEEL_HALF,
                     ARROW_WHEEL_R, SWATCH_DARK_V, sides=10)
            lens_disc(mesh, wx, ARROW_WHEEL_Y, wz + ARROW_WHEEL_HALF, ARROW_WHEEL_R,
                      (0, 0, 1), SWATCH_DARK_V, sides=10)
            lens_disc(mesh, wx, ARROW_WHEEL_Y, wz - ARROW_WHEEL_HALF, ARROW_WHEEL_R,
                      (0, 0, -1), SWATCH_DARK_V, sides=10)

    # Outrigger jacks, which is what a deployed board stands on rather than its wheels. The leg
    # starts inside its foot pad and has no bottom face, so the two do not meet in one plane.
    j = ARROW_JACK_HALF
    for jx in ARROW_JACK_X:
        box(mesh, jx - j, jx + j, 0.18, ARROW_CHASSIS[3] - 0.15, AXIS - j, AXIS + j,
            SWATCH_BAND_V, faces=("x-", "x+", "y+", "z-", "z+"))
        box(mesh, jx - j * 2.2, jx + j * 2.2, 0.0, 0.28, AXIS - j * 2.2, AXIS + j * 2.2,
            SWATCH_DARK_V)


def build_arrow_board_inventory(mesh):
    """The WHOLE board -- chassis, mast, braces and a dark panel -- shrunk to fit one cell.

    The placed block is deliberately four blocks wide, which is right in the world and useless as
    an inventory icon: handed to ``forge:default-block`` it is drawn four times the size of its
    slot and spills over the ones around it. So the icon is its own model, sized to the slot.

    It carries the mast and panel the placed block leaves to the renderer, because an icon of the
    trailer alone does not read as an arrow board. The panel is a plain dark face here: an icon
    is one still frame, and a frozen lamp pattern would suggest the board only ever shows that
    one.
    """
    _arrow_chassis(mesh)

    h = ARROW_MAST_HALF
    for mx in ARROW_MAST_X:
        box(mesh, mx - h, mx + h, ARROW_MAST[0], ARROW_MAST[1], AXIS - h, AXIS + h,
            SWATCH_BAND_V)
    for by in ARROW_MAST_BRACE:
        box(mesh, ARROW_MAST_X[0], ARROW_MAST_X[1], by, by + h * 1.4,
            AXIS - h * 0.7, AXIS + h * 0.7, SWATCH_BAND_V)
    px0, px1, py0, py1 = ARROW_PANEL
    box(mesh, px0, px1, py0, py1, ARROW_PANEL_Z[0], ARROW_PANEL_Z[1], SWATCH_DARK_V)

    fit_in_cell(mesh)


def fit_in_cell(mesh, margin=0.5):
    """Shrink a finished mesh until it fits inside its own cell, then stand it on the floor.

    For inventory models of devices that are deliberately bigger than a block: the item transform
    assumes what it is given fits in a unit cube, and silently draws anything larger over its
    neighbours.
    """
    minx, miny, minz, maxx, maxy, maxz = mesh_bounds(mesh)
    span = max(maxx - minx, maxy - miny, maxz - minz)
    scale_mesh(mesh, (16.0 - 2.0 * margin) / span)
    rest_on_floor(mesh)


def build_delineator(mesh):
    square_frustum(mesh, [(DELINEATOR_BASE_HALF, 0.0),
                          (DELINEATOR_BASE_HALF, 0.35),
                          (DELINEATOR_BASE_HALF - 0.5, DELINEATOR_BASE_TOP)])
    square_cap(mesh, DELINEATOR_BASE_HALF, 0.0, (0, -1, 0), SWATCH_DARK_V)
    square_cap(mesh, DELINEATOR_BASE_HALF - 0.5, DELINEATOR_BASE_TOP, (0, 1, 0), SWATCH_DARK_V)
    half_x, half_z = DELINEATOR_SECTION
    faces = [((0, 0, 1), (AXIS - half_x, AXIS + half_z, AXIS + half_x, AXIS + half_z)),
             ((1, 0, 0), (AXIS + half_x, AXIS + half_z, AXIS + half_x, AXIS - half_z)),
             ((0, 0, -1), (AXIS + half_x, AXIS - half_z, AXIS - half_x, AXIS - half_z)),
             ((-1, 0, 0), (AXIS - half_x, AXIS - half_z, AXIS - half_x, AXIS + half_z))]
    y0, y1 = DELINEATOR_BASE_TOP, DELINEATOR_HEIGHT
    for normal, (xa, za, xb, zb) in faces:
        mesh.quad_out([(xa, y0, za), (xb, y0, zb), (xb, y1, zb), (xa, y1, za)], normal,
                      [uv_at(0.0, y0), uv_at(1.0, y0), uv_at(1.0, y1), uv_at(0.0, y1)])
    t = uv_swatch(SWATCH_BAND_V)
    mesh.quad_out([(AXIS - half_x, y1, AXIS - half_z), (AXIS + half_x, y1, AXIS - half_z),
                   (AXIS + half_x, y1, AXIS + half_z), (AXIS - half_x, y1, AXIS + half_z)],
                  (0, 1, 0), [t, t, t, t])


def _zebra_section(f):
    """The cross-section at fraction ``f`` along the body, as (half width, crown height)."""
    scale = max(ZEBRA_TIP, math.sin(math.pi * f) ** ZEBRA_NOSE)
    return ZEBRA_HALF_Z * scale, ZEBRA_HEIGHT * scale


def _zebra_arch(x, half_z, height):
    """One arched cross-section, from the ground on one side over the crown to the other."""
    pts = []
    for j in range(ZEBRA_ARCH_SIDES + 1):
        a = math.pi * j / ZEBRA_ARCH_SIDES
        pts.append((x, height * math.sin(a), AXIS - half_z * math.cos(a)))
    return pts


def build_zebra_delineator(mesh):
    """A long flattened dome that tapers to a nose at each end.

    Built as a run of arched cross-sections rather than a lathe, because the section changes
    along the body: a lathe would give a shape of revolution, and this is a shape that is wide in
    the middle and pointed at both ends.
    """
    x0, x1 = ZEBRA_X
    stations = []
    for i in range(ZEBRA_STATIONS + 1):
        f = i / ZEBRA_STATIONS
        x = x0 + (x1 - x0) * f
        half_z, height = _zebra_section(f)
        stations.append((x, half_z, _zebra_arch(x, half_z, height)))

    # The shell. Each quad's u is where it sits around the arch, its v where it sits along the
    # body, which is what puts the bands across it.
    for i in range(ZEBRA_STATIONS):
        xa, _ha, arch_a = stations[i]
        xb, _hb, arch_b = stations[i + 1]
        for j in range(ZEBRA_ARCH_SIDES):
            t0 = j / ZEBRA_ARCH_SIDES
            t1 = (j + 1) / ZEBRA_ARCH_SIDES
            pts = [arch_a[j], arch_b[j], arch_b[j + 1], arch_a[j + 1]]
            uvs = [uv_at(t0, xa), uv_at(t0, xb), uv_at(t1, xb), uv_at(t1, xa)]
            # Outward is away from the body's own centre line, which is the ground line under
            # the crown. Naming a normal per face would have to know which way each one leans.
            mid = tuple(sum(p[k] for p in pts) / 4.0 for k in range(3))
            axis_point = (mid[0], 0.0, AXIS)
            normal = (mid[0] - axis_point[0], mid[1] - axis_point[1], mid[2] - axis_point[2])
            mesh.quad_out(pts, normal, uvs)

    # The underside, and a fan closing each nose.
    t = uv_swatch(SWATCH_DARK_V)
    for i in range(ZEBRA_STATIONS):
        xa, ha, _a = stations[i]
        xb, hb, _b = stations[i + 1]
        mesh.quad_out([(xa, 0.0, AXIS - ha), (xb, 0.0, AXIS - hb),
                       (xb, 0.0, AXIS + hb), (xa, 0.0, AXIS + ha)],
                      (0, -1, 0), [t, t, t, t])
    for (x, _half_z, arch), sign in ((stations[0], -1), (stations[-1], 1)):
        centre = (x, 0.0, AXIS)
        normal = (sign, 0, 0)
        for j in range(ZEBRA_ARCH_SIDES):
            mesh.tri([(centre, t, normal), (arch[j], t, normal), (arch[j + 1], t, normal)])


def zebra_texture():
    """Black with white bands, and the moulded dimples the rubber body carries.

    The dimples are drawn on the base colour only. They are not decoration: without them the body
    is a flat black shape whose one-sided lighting is the only cue to its curve, and it reads as
    a painted marking rather than something standing off the road.
    """
    img = band_image(BLACK, BLACK_LIGHT, ZEBRA_BANDS, WHITE, WHITE_DIM)
    for r in range(TEX_SIZE):
        for c in range(TEX_SIZE):
            if (c + 0.5) / TEX_SIZE > SWATCH_U0:
                continue
            if (r + c) % 4 or (r - c) % 4:
                continue
            pixel = img.getpixel((c, r))
            if pixel[0] > 120:      # inside a band; leave the sheeting smooth
                continue
            img.putpixel((c, r), tuple(min(255, v + 16) for v in pixel[:3]) + (255,))
    return img


def build_pavement_marker(mesh):
    """A flat foot with a panel standing up off the back of it.

    The panel's two broad faces are mapped by height so the reflective strip lands where the
    texture puts it; everything else -- the foot, the panel's edges -- takes the flat swatch, so
    the marker shows beading only along the strip, as a real one does.
    """
    z0, z1 = MARKER_PANEL_Z
    y0 = MARKER_FOOT_Y - 0.10

    # Foot, panel and lip each step in a little from the one behind, and each finishes INSIDE it
    # rather than flush with it. Three pieces the same width would put three pairs of faces in
    # the same two planes, which z-fights along both ends of the marker.
    foot_hx = MARKER_HALF_X
    panel_hx = MARKER_HALF_X - 0.12
    lip_hx = MARKER_HALF_X - 0.30

    box(mesh, AXIS - foot_hx, AXIS + foot_hx, 0.0, MARKER_FOOT_Y, MARKER_FOOT_Z0, z1 - 0.06,
        SWATCH_BASE_V)

    box(mesh, AXIS - panel_hx, AXIS + panel_hx, y0, MARKER_PANEL_TOP, z0, z1,
        SWATCH_BASE_V, faces=("x-", "x+", "y+"))
    for z, normal in ((z1, (0, 0, 1)), (z0, (0, 0, -1))):
        pts = [(AXIS - panel_hx, y0, z), (AXIS + panel_hx, y0, z),
               (AXIS + panel_hx, MARKER_PANEL_TOP, z), (AXIS - panel_hx, MARKER_PANEL_TOP, z)]
        uvs = [uv_at(0.0, y0), uv_at(1.0, y0),
               uv_at(1.0, MARKER_PANEL_TOP), uv_at(0.0, MARKER_PANEL_TOP)]
        mesh.quad_out(pts, normal, uvs)

    # The moulded lip over the strip: proud of the panel's front face, and kept strictly inside
    # the textured band so its own faces still land on the beading.
    lip_y0 = MARKER_BAND[0] + 0.05
    lip_y1 = MARKER_PANEL_TOP - 0.05
    box(mesh, AXIS - lip_hx, AXIS + lip_hx, lip_y0, lip_y1, MARKER_LIP_Z0, z0 + 0.05,
        SWATCH_BASE_V, faces=("x-", "x+", "y-", "y+"))
    pts = [(AXIS - lip_hx, lip_y0, MARKER_LIP_Z0), (AXIS + lip_hx, lip_y0, MARKER_LIP_Z0),
           (AXIS + lip_hx, lip_y1, MARKER_LIP_Z0), (AXIS - lip_hx, lip_y1, MARKER_LIP_Z0)]
    mesh.quad_out(pts, (0, 0, -1),
                  [uv_at(0.0, lip_y0), uv_at(1.0, lip_y0),
                   uv_at(1.0, lip_y1), uv_at(0.0, lip_y1)])


def marker_texture(body, body_shade, strip, strip_shade):
    """A flat body with the beaded reflective strip across the top of the panel.

    Deliberately not ``band_image``: that shades across u with a sine, which reads as the round
    side of a lathed cone or drum and reads as a vignette on something flat. A marker is a
    moulded plastic tab, so its body is left flat and only the strip carries any texture.
    """
    img = Image.new("RGBA", (MARKER_TEX_SIZE, MARKER_TEX_SIZE), body + (255,))
    band_y0, band_y1 = MARKER_BAND
    for r in range(MARKER_TEX_SIZE):
        y = V_SPAN * (1.0 - (r + 0.5) / MARKER_TEX_SIZE)
        if band_y0 <= y <= band_y1:
            color = strip
            if y - band_y0 < 0.09 or band_y1 - y < 0.09:
                color = strip_shade
        elif 0.0 <= band_y0 - y < 0.08:
            color = body_shade       # a shadow line just under the strip
        else:
            continue
        for c in range(MARKER_TEX_SIZE):
            if (c + 0.5) / MARKER_TEX_SIZE > SWATCH_U0:
                continue
            img.putpixel((c, r), color + (255,))

    # The beading. Sparse bright pixels inside the strip only, which is what separates a
    # retroreflective strip from a painted stripe at this distance.
    r0 = int(MARKER_TEX_SIZE * (1.0 - band_y1 / V_SPAN))
    r1 = int(MARKER_TEX_SIZE * (1.0 - band_y0 / V_SPAN))
    for r in range(max(0, r0 + 1), min(MARKER_TEX_SIZE, r1 - 1)):
        for c in range(MARKER_TEX_SIZE):
            if (c + 0.5) / MARKER_TEX_SIZE > SWATCH_U0 or (r * 5 + c * 3) % 7:
                continue
            img.putpixel((c, r), tuple(min(255, v + 34) for v in strip) + (255,))

    draw_swatches(img, body, strip, AMBER)
    return img


def marker_emissive(body, body_shade, strip, strip_shade):
    """The OptiFine emissive overlay for a marker: the reflective strip alone, on transparency.

    OptiFine draws a texture named ``<name>_e`` over its base at full brightness, which is the
    only way to light PART of a baked model in 1.12. The alternative, making the whole block
    report full brightness, is not confined to the block: a neighbour's renderer asks the block
    across each face for its packed light to shade that face's own vertices, so a fullbright
    marker visibly brightens the road it is standing on.

    Everything but the strip is left transparent, so without OptiFine this file is simply never
    referenced and the marker draws normally.
    """
    base = marker_texture(body, body_shade, strip, strip_shade)
    img = Image.new("RGBA", base.size, (0, 0, 0, 0))
    band_y0, band_y1 = MARKER_BAND
    for r in range(MARKER_TEX_SIZE):
        y = V_SPAN * (1.0 - (r + 0.5) / MARKER_TEX_SIZE)
        if not band_y0 <= y <= band_y1:
            continue
        for c in range(MARKER_TEX_SIZE):
            if (c + 0.5) / MARKER_TEX_SIZE > SWATCH_U0:
                continue
            img.putpixel((c, r), base.getpixel((c, r)))
    return img


def swept_wall(mesh, profile, swatch_v=SWATCH_BAND_V, cz=AXIS, x_range=None, u_range=(0.0, 1.0)):
    """A wall of constant cross-section running the length of the cell.

    ``profile`` is (half width, height) up ONE side, bottom to top; the other side is mirrored.
    The two long faces are mapped by height, so a texture drawn the way ``band_image`` draws one
    puts its bands across the wall at the heights it names. The top and the underside take the
    flat swatch: bare plastic or concrete, not sheeting.

    This is the CORE -- what every segment draws. The caps over its open ends are
    ``swept_wall_end``, and are added only where nothing connects.
    """
    x0, x1 = x_range if x_range is not None else WALL_X
    u0, u1 = u_range
    t = uv_swatch(swatch_v)
    q = [t, t, t, t]

    for sign in (1, -1):
        for (hz0, y0), (hz1, y1) in zip(profile, profile[1:]):
            if abs(hz1 - hz0) < 1e-9 and abs(y1 - y0) < 1e-9:
                continue
            z0 = cz + sign * hz0
            z1 = cz + sign * hz1
            # Outward is perpendicular to the profile segment, in the yz plane. Naming it per
            # segment would have to know which way each one leans.
            normal = (0.0, -(hz1 - hz0), sign * (y1 - y0))
            pts = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z1), (x0, y1, z1)]
            uvs = [uv_at(u0, y0), uv_at(u1, y0), uv_at(u1, y1), uv_at(u0, y1)]
            mesh.quad_out(pts, normal, uvs)

    top_hz, top_y = profile[-1]
    mesh.quad_out([(x0, top_y, cz - top_hz), (x1, top_y, cz - top_hz),
                   (x1, top_y, cz + top_hz), (x0, top_y, cz + top_hz)], (0, 1, 0), q)

    base_hz = profile[0][0]
    mesh.quad_out([(x0, 0.0, cz - base_hz), (x1, 0.0, cz - base_hz),
                   (x1, 0.0, cz + base_hz), (x0, 0.0, cz + base_hz)], (0, -1, 0), q)




def swept_wall_fill(mesh, profile, swatch_v=SWATCH_BAND_V, cz=AXIS):
    """The length of wall that closes the gap to a DIAGONAL neighbour.

    The same swept cross-section as the core, carried on past the cell's right-hand edge. It is
    drawn only where this device joins one a diagonal step away, and only ever on the right, so
    the two segments either side of a joint cannot both fill it and end up inside each other.

    Its own end faces are left off for the same reason the core's are: the neighbour's core
    begins exactly where this finishes, and two caps in one plane z-fight along the whole joint.
    """
    x1 = WALL_X[1]
    span = WALL_X[1] - WALL_X[0]
    swept_wall(mesh, profile, swatch_v, cz,
               x_range=(x1, x1 + DIAGONAL_GAP),
               u_range=(0.0, DIAGONAL_GAP / span))


def swept_wall_end(mesh, profile, left, swatch_v=SWATCH_BAND_V, cz=AXIS):
    """The cap over a wall's open end, drawn only where nothing connects.

    Two segments meeting on the cell boundary would otherwise put their end faces in one plane
    and z-fight along the whole joint. Dropping the cap at a joint leaves the two cores meeting
    edge to edge, which is a seam rather than an overlap and draws cleanly.
    """
    x = WALL_X[0] if left else WALL_X[1]
    normal = (-1, 0, 0) if left else (1, 0, 0)
    t = uv_swatch(swatch_v)
    q = [t, t, t, t]
    for (hz0, y0), (hz1, y1) in zip(profile, profile[1:]):
        if abs(hz1 - hz0) < 1e-9 and abs(y1 - y0) < 1e-9:
            continue
        mesh.quad_out([(x, y0, cz - hz0), (x, y0, cz + hz0),
                       (x, y1, cz + hz1), (x, y1, cz - hz1)], normal, q)


def wall_texture(body, body_shade, band=None, band_color=None, band_shade=None, ribs=0):
    """A wall's long faces: flat body, an optional band across it, and optional moulded ribs.

    Flat rather than ``band_image``'s one-sided shading, which reads as the round side of a cone
    and as a gradient down the length of something this long. The ribs are drawn instead of
    modelled: a rib deep enough to catch the light is a rib deep enough to z-fight against the
    face it stands on, and at this size it would be two texels wide anyway.
    """
    img = Image.new("RGBA", (TEX_SIZE, TEX_SIZE), body + (255,))
    x0, x1 = WALL_X
    for r in range(TEX_SIZE):
        y = V_SPAN * (1.0 - (r + 0.5) / TEX_SIZE)
        row = body
        if band is not None and band[0] <= y <= band[1]:
            row = band_color
            if y - band[0] < 0.18:
                row = band_shade
        for c in range(TEX_SIZE):
            u = (c + 0.5) / TEX_SIZE
            if u > SWATCH_U0:
                continue
            color = row
            if ribs:
                # Where along the wall this column falls, and how close it is to a rib.
                along = (u / SWATCH_U0) * ribs
                if abs(along - round(along)) < 0.055 and round(along) not in (0, ribs):
                    color = band_shade if row is band_color else body_shade
            img.putpixel((c, r), color + (255,))

    draw_swatches(img, body, band_color if band_color is not None else body, AMBER)
    return img


def build_signal_trailer(mesh, style, tip_z=None, rigging=True):
    """A portable signal trailer: chassis, deck, winch mast and, on the arm styles, the boom.

    Everything is flat swatch. There is nothing on a signal trailer that carries sheeting or a
    pattern, and the one part that is not painted steel -- the solar deck -- is a colour rather
    than an image at this size.
    """
    spec = PSIG_STYLES[style]
    heavy = spec["heavy"]

    bed = PSIG_ARM_BED if heavy else PSIG_BED
    tongue = PSIG_ARM_TONGUE if heavy else PSIG_TONGUE
    wheel = PSIG_ARM_WHEEL if heavy else PSIG_WHEEL
    jack_x = PSIG_ARM_JACK_X if heavy else PSIG_JACK_X
    jack_z = PSIG_ARM_JACK_Z if heavy else PSIG_JACK_Z
    jack_half = PSIG_ARM_JACK_HALF if heavy else PSIG_JACK_HALF
    solar = PSIG_ARM_SOLAR if heavy else PSIG_SOLAR
    cabinet = PSIG_ARM_CABINET if heavy else PSIG_CABINET

    bx0, bx1, by0, by1, bz0, bz1 = bed
    box(mesh, bx0, bx1, by0, by1, bz0, bz1, SWATCH_BASE_V)
    tx0, tx1, ty0, ty1, tz0, tz1 = tongue
    box(mesh, tx0, tx1, ty0, ty1, tz0, tz1, SWATCH_BASE_V)

    # One axle on the compact chassis; twin wheels a side on the heavy one, which is what a
    # trailer carrying a boom this long actually runs on.
    for wz in wheel["z"]:
        lens_rim(mesh, wheel["x"], wheel["y"], wz - wheel["half"], wz + wheel["half"],
                 wheel["r"], SWATCH_DARK_V, sides=10)
        lens_disc(mesh, wheel["x"], wheel["y"], wz + wheel["half"], wheel["r"], (0, 0, 1),
                  SWATCH_DARK_V, sides=10)
        lens_disc(mesh, wheel["x"], wheel["y"], wz - wheel["half"], wheel["r"], (0, 0, -1),
                  SWATCH_DARK_V, sides=10)

    # The levelling jacks a deployed trailer stands on. The leg starts inside its pad, so the two
    # never share a plane.
    #
    # On the heavy chassis they stand OUTSIDE the deck and reach it along an outrigger arm. A jack
    # tucked under the deck does nothing for stability -- the deck edge was already the far side
    # of the tipping base -- and a machine holding a boom eight cells out needs the base widened,
    # not restated.
    j = jack_half
    for jx in jack_x:
        for jz in jack_z:
            if heavy:
                oy0, oy1 = PSIG_ARM_OUTRIGGER_Y
                reach_z0, reach_z1 = ((jz, bz0 + 1.0) if jz < bz0 else (bz1 - 1.0, jz))
                box(mesh, jx - j * 0.9, jx + j * 0.9, oy0, oy1,
                    min(reach_z0, reach_z1), max(reach_z0, reach_z1), SWATCH_BASE_V)
            box(mesh, jx - j, jx + j, 0.60, by0 + 1.0, jz - j, jz + j, SWATCH_BAND_V,
                faces=("x-", "x+", "y+", "z-", "z+"))
            box(mesh, jx - j * 2.2, jx + j * 2.2, 0.0, 0.80, jz - j * 2.2, jz + j * 2.2,
                SWATCH_BAND_V)

    solar_array(mesh, by1, solar)

    # Starts INSIDE the bed and skips its own underside, rather than sitting flush on it: flush
    # would put three faces in the plane of the bed's top and z-fight across all of them.
    cx0, cx1, cy0, cy1, cz0, cz1 = cabinet
    box(mesh, cx0, cx1, cy0 - 0.30, cy1, cz0, cz1, SWATCH_BASE_V,
        faces=("x-", "x+", "y+", "z-", "z+"))

    mh = PSIG_MAST_HALF
    mx, mz = PSIG_MAST_X, PSIG_MAST_Z
    top = spec["mast_top"]
    # The mast runs from INSIDE the bed, not from the top of the cabinet. The cabinet stands
    # beside the mast rather than under it, so starting there left the mast hanging in the air
    # over the deck with a visible gap beneath it.
    box(mesh, mx - mh, mx + mh, by1 - 1.20, top, mz - mh, mz + mh, SWATCH_BASE_V,
        faces=("x-", "x+", "y+", "z-", "z+"))

    if spec["arm"] is None:
        return

    tip = spec["arm"] if tip_z is None else tip_z
    ay0 = PSIG_ARM_CLEARANCE
    pivot = PSIG_PIVOT_Y
    elbow = PSIG_ELBOW_Z

    root_h, root_t = PSIG_BOOM_ROOT
    elb_h, elb_t = PSIG_BOOM_ELBOW
    tip_h, tip_t = PSIG_BOOM_TIP

    def station(z, half_x, y0, y1):
        """One cross-section of the boom, as a quad across the boom's own span axis."""
        return [(mx - half_x, y0, z), (mx + half_x, y0, z),
                (mx + half_x, y1, z), (mx - half_x, y1, z)]

    # The three stations. Cut square across Z rather than perpendicular to the member, which on
    # the rising section makes it read very slightly deeper than it is -- a taper the eye takes
    # for the thicker root a real boom has there anyway.
    at_root = station(mz, root_h, pivot - root_t / 2.0, pivot + root_t / 2.0)
    at_elbow = station(elbow, elb_h, ay0, ay0 + elb_t)
    at_tip = station(tip, tip_h, ay0, ay0 + tip_t)

    # The rise, then the level run. Neither caps the elbow: they share that quad exactly, so
    # capping it twice would z-fight and capping it once would be a face inside the solid.
    prism(mesh, at_root, at_elbow, SWATCH_BASE_V, ends=("bottom",))
    prism(mesh, at_elbow, at_tip, SWATCH_BASE_V, ends=("top",))

    # The pivot gusset: the plate the boom swings on, one either side of the mast. Each starts
    # INSIDE the mast rather than flush on its face, so the two never share a plane.
    plate_reach, plate_thick = PSIG_PIVOT_PLATE
    for side in (-1.0, 1.0):
        inner = mx + side * (mh - 0.40)
        outer = mx + side * (mh + plate_thick)
        box(mesh, min(inner, outer), max(inner, outer),
            pivot - plate_reach, pivot + plate_reach,
            mz - plate_reach, mz + plate_reach * 0.65, SWATCH_BASE_V)

    if not rigging:
        return

    # The king post at the bend, and the tie from its top out along the level run. This is how
    # the references are rigged and it is also the honest way to hold a cantilever this long:
    # the mast stops just above the pivot and takes none of the outer end's load.
    king_y = ay0 + elb_t + PSIG_KING_RISE
    kh = PSIG_KING_HALF
    box(mesh, mx - kh, mx + kh, ay0 + elb_t - 1.0, king_y,
        PSIG_KING_Z - kh, PSIG_KING_Z + kh, SWATCH_BASE_V, faces=("x-", "x+", "y+", "z-", "z+"))

    tie_h = PSIG_TIE_HALF
    tie_z = tip * PSIG_TIE_AT
    prism(mesh,
          [(mx - tie_h, king_y - 2.4, PSIG_KING_Z - tie_h),
           (mx + tie_h, king_y - 2.4, PSIG_KING_Z - tie_h),
           (mx + tie_h, king_y - 2.4, PSIG_KING_Z + tie_h),
           (mx - tie_h, king_y - 2.4, PSIG_KING_Z + tie_h)],
          [(mx - tie_h, ay0 + elb_t - 0.8, tie_z - tie_h),
           (mx + tie_h, ay0 + elb_t - 0.8, tie_z - tie_h),
           (mx + tie_h, ay0 + elb_t - 0.8, tie_z + tie_h),
           (mx - tie_h, ay0 + elb_t - 0.8, tie_z + tie_h)],
          SWATCH_BAND_V)

    # The ram under the rise. Both ends finish INSIDE what they meet -- the mast at one end and
    # the boom at the other -- so neither butts against a face.
    ram_h = PSIG_RAM_HALF
    ram_z = mz + (elbow - mz) * PSIG_RAM_AT
    ram_y = pivot - root_t / 2.0 + (ay0 - (pivot - root_t / 2.0)) * PSIG_RAM_AT + 1.0
    prism(mesh,
          [(mx - ram_h, pivot - PSIG_RAM_DROP, mz - ram_h),
           (mx + ram_h, pivot - PSIG_RAM_DROP, mz - ram_h),
           (mx + ram_h, pivot - PSIG_RAM_DROP, mz + ram_h),
           (mx - ram_h, pivot - PSIG_RAM_DROP, mz + ram_h)],
          [(mx - ram_h, ram_y, ram_z - ram_h), (mx + ram_h, ram_y, ram_z - ram_h),
           (mx + ram_h, ram_y, ram_z + ram_h), (mx - ram_h, ram_y, ram_z + ram_h)],
          SWATCH_BAND_V)


PSIG_ICON_TIP_Z = -72.00   # how much of the boom the icon carries


def build_signal_trailer_inventory(mesh, style):
    """The trailer shrunk to fit one cell, for the item icon.

    The placed block is four cells long and eight and a half high, which is right in the world
    and useless as an icon: handed to ``forge:default-block`` it is drawn many times the size of
    its slot and spills over the ones around it.

    The arm style's icon carries a SHORTENED boom and none of the rigging. A slot is sixteen
    pixels; a boom drawn its true eight cells long shrinks the trailer under it to nothing, and a
    tie rod an eighth of a pixel across is not there at all.
    """
    build_signal_trailer(mesh, style,
                         tip_z=PSIG_ICON_TIP_Z if style == "arm" else None,
                         rigging=False)
    fit_in_cell(mesh)


def solar_array(mesh, deck_top, footprint=None):
    """The tilted solar array on a trailer's deck.

    The panel is the one face on any of these devices that carries a picture rather than a flat
    swatch, so it is built by hand instead of with ``box``: its top takes the cell image and the
    other five faces take the dark swatch, which is the frame around a real one.

    It leans, which is why it is a prism and not a box. Everything else about a solar panel is
    negotiable; lying flat is not, because then it is not pointing at anything.
    """
    x0, x1, z0, z1 = footprint if footprint is not None else PSIG_SOLAR
    y0 = deck_top + (PSIG_SOLAR_Y - PSIG_BED[3])
    rise = PSIG_SOLAR_RISE
    t = PSIG_SOLAR_THICK

    # Lower surface, low edge at z0 and high edge at z1; the upper surface is the same lifted.
    low = [(x0, y0, z0), (x1, y0, z0), (x1, y0 + rise, z1), (x0, y0 + rise, z1)]
    top = [(p[0], p[1] + t, p[2]) for p in low]

    u0, u1 = PSIG_SOLAR_U
    v0, v1 = PSIG_SOLAR_V
    cell_uv = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
    # Up and toward the low edge: the cross product of the panel's own two directions, flipped
    # so it points out of the face that is meant to see the sky.
    mesh.quad_out(list(top), (0.0, z1 - z0, -rise), cell_uv)

    dark = uv_swatch(SWATCH_DARK_V)
    flat = [dark, dark, dark, dark]
    mesh.quad_out(list(reversed(low)), (0.0, -(z1 - z0), rise), flat)
    for i in range(4):
        j = (i + 1) % 4
        edge = [low[i], low[j], top[j], top[i]]
        mid = tuple(sum(p[k] for p in edge) / 4.0 for k in range(3))
        centre = (0.5 * (x0 + x1), y0 + rise * 0.5 + t * 0.5, 0.5 * (z0 + z1))
        mesh.quad_out(edge, tuple(mid[k] - centre[k] for k in range(3)), flat)

    # Two props under the high edge. They start inside the deck, finish inside the panel, and
    # stop short of its high edge, so no end of either shares a plane with what it meets.
    p = PSIG_SOLAR_PROP
    for px in (x0 + 3.0, x1 - 3.0):
        box(mesh, px - p, px + p, deck_top - 0.40, y0 + rise + t * 0.5,
            z1 - p * 2.8, z1 - 0.40, SWATCH_BAND_V)


def trailer_texture():
    """A signal trailer's colours: highway orange, black tyres, galvanised jacks, and the solar
    array's cell grid pasted in at the flipped rows the panel's UVs read from."""
    size = TRAILER_TEX_SIZE
    img = Image.new("RGBA", (size, size), ARROW_FRAME_ORANGE + (255,))
    draw_swatches(img, ARROW_FRAME_ORANGE, GALVANISED, SOLAR_BLUE)

    cells = Image.open(SOLAR_CELL_TEXTURE).convert("RGBA")
    u0, u1 = PSIG_SOLAR_U
    v0, v1 = PSIG_SOLAR_V
    px0, px1 = int(round(u0 * size)), int(round(u1 * size))
    py0, py1 = int(round((1.0 - v1) * size)), int(round((1.0 - v0) * size))
    img.paste(cells.resize((px1 - px0, py1 - py0), Image.LANCZOS), (px0, py0))
    return img


def build_vertical_panel(mesh):
    """A striped panel on a post, on a moulded foot.

    The panel's two broad faces carry the stripes and everything else takes the flat swatch, the
    same division the barricade rails use: sheeting where a real one carries sheeting, bare
    plastic everywhere else.
    """
    fx, fy, fz = VPANEL_FOOT
    cz = VPANEL_POST_CZ
    box(mesh, AXIS - fx, AXIS + fx, 0.0, fy, cz - fz, cz + fz, SWATCH_BAND_V)
    box(mesh, AXIS - VPANEL_POST_HALF, AXIS + VPANEL_POST_HALF, fy - 0.14, VPANEL_TOP,
        cz - VPANEL_POST_HALF, cz + VPANEL_POST_HALF, SWATCH_BAND_V,
        faces=("x-", "x+", "y+", "z-", "z+"))

    half, y0, y1 = VPANEL_PANEL
    hz = VPANEL_PANEL_HALF_Z
    box(mesh, AXIS - half, AXIS + half, y0, y1, AXIS - hz, AXIS + hz, SWATCH_BAND_V,
        faces=("x-", "x+", "y-", "y+"))
    for z, normal in ((AXIS + hz, (0, 0, 1)), (AXIS - hz, (0, 0, -1))):
        pts = [(AXIS - half, y0, z), (AXIS + half, y0, z),
               (AXIS + half, y1, z), (AXIS - half, y1, z)]
        uvs = [(0.0, 0.0), (SWATCH_U0, 0.0), (SWATCH_U0, 1.0), (0.0, 1.0)]
        mesh.quad_out(pts, normal, uvs)


def build_road_plate_core(mesh):
    """A steel plate's faces, tread side up, without its rims.

    The tread face is mapped across the plate in BOTH directions rather than by height, which is
    what every other face here does. A plate is the one thing in this family whose pattern runs
    two ways at once.
    """
    n = PLATE_SPAN
    t = uv_swatch(SWATCH_BASE_V)
    mesh.quad_out([(0.0, PLATE_Y, 0.0), (n, PLATE_Y, 0.0), (n, PLATE_Y, n), (0.0, PLATE_Y, n)],
                  (0, 1, 0),
                  [(0.0, 0.0), (SWATCH_U0, 0.0), (SWATCH_U0, 1.0), (0.0, 1.0)])
    mesh.quad_out([(0.0, 0.0, 0.0), (n, 0.0, 0.0), (n, 0.0, n), (0.0, 0.0, n)],
                  (0, -1, 0), [t, t, t, t])


def build_road_plate_edge(mesh, side):
    """One rim of a plate, drawn only where no other plate abuts that side.

    A patch of plates is laid edge to edge and welded or simply butted, so a rim inside the patch
    is not there to be seen -- and two rims in one plane z-fight anyway.
    """
    n = PLATE_SPAN
    t = uv_swatch(SWATCH_BASE_V)
    q = [t, t, t, t]
    if side == "north":
        pts = [(0.0, 0.0, 0.0), (n, 0.0, 0.0), (n, PLATE_Y, 0.0), (0.0, PLATE_Y, 0.0)]
        normal = (0, 0, -1)
    elif side == "south":
        pts = [(0.0, 0.0, n), (n, 0.0, n), (n, PLATE_Y, n), (0.0, PLATE_Y, n)]
        normal = (0, 0, 1)
    elif side == "west":
        pts = [(0.0, 0.0, 0.0), (0.0, 0.0, n), (0.0, PLATE_Y, n), (0.0, PLATE_Y, 0.0)]
        normal = (-1, 0, 0)
    else:
        pts = [(n, 0.0, 0.0), (n, 0.0, n), (n, PLATE_Y, n), (n, PLATE_Y, 0.0)]
        normal = (1, 0, 0)
    mesh.quad_out(pts, normal, q)


def road_plate_texture():
    """Bare steel with the raised diamond tread a road plate carries."""
    img = Image.new("RGBA", (PLATE_TEX_SIZE, PLATE_TEX_SIZE), STEEL + (255,))
    for r in range(PLATE_TEX_SIZE):
        for c in range(PLATE_TEX_SIZE):
            u = (c + 0.5) / PLATE_TEX_SIZE
            if u > SWATCH_U0:
                continue
            # World position across the plate, so the tread keeps its pitch whatever the
            # texture size is.
            wx = (u / SWATCH_U0) * PLATE_SPAN
            wz = ((r + 0.5) / PLATE_TEX_SIZE) * PLATE_SPAN
            a = ((wx + wz) % PLATE_TREAD) / PLATE_TREAD
            b = ((wx - wz) % PLATE_TREAD) / PLATE_TREAD
            colour = STEEL
            # The tread is barely there. It is milled shallow to begin with and polished flat by
            # everything that drives over it, so on a plate that has been down a week it reads as
            # a faint sheen rather than as a pattern.
            if a < 0.22:
                colour = STEEL_LIGHT if a < 0.11 else STEEL_DARK
            elif b < 0.22:
                colour = STEEL_LIGHT if b < 0.11 else STEEL_DARK

            # Rust, strongest at the edges where water sits and the plate is scraped. Hashed
            # rather than random so regenerating gives the same plate back.
            edge = min(wx, PLATE_SPAN - wx, wz, PLATE_SPAN - wz) / (PLATE_SPAN * 0.5)
            # Hashed on a COARSE grid, not per pixel: rust arrives as stains a hand's width
            # across, and a per-pixel hash gives confetti instead. Hashed rather than random so
            # regenerating gives the same plate back.
            cell = (int(wx / PLATE_RUST_BLOB) * 31 + int(wz / PLATE_RUST_BLOB) * 17) % 97
            fine = (int(wx / (PLATE_RUST_BLOB * 0.4)) * 13
                    + int(wz / (PLATE_RUST_BLOB * 0.4)) * 7) % 11
            if cell < 26 - int(edge * 21) and fine > 2:
                colour = STEEL_RUST_LIGHT if fine > 8 else STEEL_RUST
            img.putpixel((c, r), colour + (255,))
    draw_swatches(img, STEEL, STEEL_DARK, STEEL_LIGHT)
    return img


def build_safety_fence(mesh):
    """Two stakes and a mesh panel between them."""
    for cx in FENCE_POST_X:
        box(mesh, cx - FENCE_POST_HALF, cx + FENCE_POST_HALF, 0.0, FENCE_POST_TOP,
            AXIS - FENCE_POST_HALF, AXIS + FENCE_POST_HALF, SWATCH_BAND_V)

    y0, y1 = FENCE_PANEL
    for z, normal in ((AXIS + FENCE_HALF_Z, (0, 0, 1)), (AXIS - FENCE_HALF_Z, (0, 0, -1))):
        pts = [(0.0, y0, z), (16.0, y0, z), (16.0, y1, z), (0.0, y1, z)]
        uvs = [uv_at(0.0, y0), uv_at(1.0, y0), uv_at(1.0, y1), uv_at(0.0, y1)]
        mesh.quad_out(pts, normal, uvs)


def build_safety_fence_fill(mesh):
    """The panel carried on past the cell's right edge to meet a DIAGONAL neighbour.

    Panel only. The stakes stay where they are -- a run gets one pair per cell either way, and a
    stake standing in the gap would be a post in the middle of a span rather than at a joint.
    """
    y0, y1 = FENCE_PANEL
    span = 16.0
    u1 = DIAGONAL_GAP / span
    for z, normal in ((AXIS + FENCE_HALF_Z, (0, 0, 1)), (AXIS - FENCE_HALF_Z, (0, 0, -1))):
        pts = [(span, y0, z), (span + DIAGONAL_GAP, y0, z),
               (span + DIAGONAL_GAP, y1, z), (span, y1, z)]
        uvs = [uv_at(0.0, y0), uv_at(u1, y0), uv_at(u1, y1), uv_at(0.0, y1)]
        mesh.quad_out(pts, normal, uvs)


def safety_fence_texture():
    """Orange mesh: plastic strands on transparency, drawn in world units.

    The holes are real transparency rather than a dark pattern, which is why the block draws on
    the cutout layer. A fence you cannot see through is a wall.
    """
    img = Image.new("RGBA", (FENCE_TEX_SIZE, FENCE_TEX_SIZE), (0, 0, 0, 0))
    for r in range(FENCE_TEX_SIZE):
        wy = V_SPAN * (1.0 - (r + 0.5) / FENCE_TEX_SIZE)
        for c in range(FENCE_TEX_SIZE):
            u = (c + 0.5) / FENCE_TEX_SIZE
            if u > SWATCH_U0:
                continue
            wx = (u / SWATCH_U0) * 16.0
            fx = (wx % FENCE_MESH) / FENCE_MESH
            fy = (wy % FENCE_MESH) / FENCE_MESH
            if fx < FENCE_STRAND:
                img.putpixel((c, r), (ORANGE if fy > FENCE_STRAND else ORANGE_DARK) + (255,))
            elif fy < FENCE_STRAND:
                img.putpixel((c, r), ORANGE_DARK + (255,))
    draw_swatches(img, ORANGE, ORANGE_DARK, WHITE)
    return img


def build_sand_barrel(mesh):
    lathe(mesh, SAND_BARREL_PROFILE, sides=SAND_BARREL_SIDES)
    disc(mesh, SAND_BARREL_LID_R, SAND_BARREL_HEIGHT, (0, 1, 0), SWATCH_BAND_V,
         sides=SAND_BARREL_SIDES)
    disc(mesh, SAND_BARREL_PROFILE[0][0], 0.0, (0, -1, 0), SWATCH_DARK_V,
         sides=SAND_BARREL_SIDES)


# --- textures -------------------------------------------------------------------------------------
def band_image(base, base_shade, bands, band_color, band_shade, base_bottom=None, bottom_to=0.0,
               accent=AMBER):
    """A vertical strip: ``base`` everywhere, ``band_color`` across each (y0, y1) in ``bands``,
    plus the flat swatch column down the right-hand edge.

    Row r of the image is height ``16 * (1 - r / TEX_SIZE)``, which is what ``uv_at`` assumes.
    """
    img = Image.new("RGBA", (TEX_SIZE, TEX_SIZE), base + (255,))
    draw = ImageDraw.Draw(img)

    for r in range(TEX_SIZE):
        y = V_SPAN * (1.0 - (r + 0.5) / TEX_SIZE)
        color = base
        if base_bottom is not None and y <= bottom_to:
            color = base_bottom[0]
        for (y0, y1) in bands:
            if y0 <= y <= y1:
                color = band_color
                # A hair of shade at each edge reads as the lip of applied sheeting.
                if y - y0 < 0.22 or y1 - y < 0.22:
                    color = band_shade
                break
        else:
            if base_bottom is None or y > bottom_to:
                # Keep a darker line just under each band so the sheeting has an edge below it.
                for (y0, _y1) in bands:
                    if 0.0 <= y0 - y < 0.20:
                        color = base_shade
                        break
        for c in range(TEX_SIZE):
            u = (c + 0.5) / TEX_SIZE
            if u > SWATCH_U0:
                continue
            t = u / SWATCH_U0
            # The strip is lit from one side, so a lathe does not read as a flat silhouette.
            lit = 0.84 + 0.24 * math.sin(math.pi * t)
            img.putpixel((c, r), tuple(min(255, int(v * lit)) for v in color) + (255,))

    del draw
    draw_swatches(img, base, band_color, accent)
    return img


def draw_swatches(img, base, band_color, accent):
    """Paint the column of flat swatches down a texture's right-hand edge.

    flip-v again: a face asking for swatch v samples sprite row ``size * (1 - v)``, so the
    rectangles have to be drawn at the flipped rows. Drawing them the natural way up silently
    hands every swatch face its neighbour's colour -- black mats come out white, white lamp
    housings come out amber -- with nothing failing anywhere.
    """
    size = img.size[0]
    draw = ImageDraw.Draw(img)
    swatch_x0 = int(size * SWATCH_U0)
    for (v, color) in ((SWATCH_BASE_V, base), (SWATCH_DARK_V, BLACK),
                       (SWATCH_BAND_V, band_color), (SWATCH_ACCENT_V, accent)):
        r0 = int(round(size * (1.0 - (v + 0.125))))
        r1 = int(round(size * (1.0 - (v - 0.125))))
        draw.rectangle([swatch_x0, r0, size - 1, r1 - 1], fill=color + (255,))


def cone_texture(body, body_shade):
    return band_image(body, body_shade, CONE_COLLARS, WHITE, WHITE_DIM,
                      base_bottom=(BLACK, BLACK_LIGHT), bottom_to=CONE_BASE_TOP)


def drum_texture():
    return band_image(ORANGE, ORANGE_DARK, DRUM_STRIPES, WHITE, WHITE_DIM,
                      base_bottom=(BLACK, BLACK_LIGHT), bottom_to=DRUM_MAT_TOP)


def channelizer_texture(body, body_shade):
    return band_image(body, body_shade, CHANNELIZER_BANDS, WHITE, WHITE_DIM,
                      base_bottom=(BLACK, BLACK_LIGHT), bottom_to=CHANNELIZER_MAT_TOP)


def delineator_texture(body, body_shade):
    return band_image(body, body_shade, [DELINEATOR_REFLECTOR], WHITE, WHITE_DIM,
                      base_bottom=(BLACK, BLACK_LIGHT), bottom_to=DELINEATOR_BASE_TOP)


def diagonal_stripe_image(slope_right, world_w, world_h, stripe_world=3.6, base=WHITE,
                          stripe=ORANGE, accent=AMBER, size=STRIPE_TEX_SIZE):
    """The striped panel texture: 45-degree bands, drawn for one panel's proportions.

    ``slope_right`` picks which way the stripes lean. The stripe direction is the whole message
    of the device -- the bands slope down toward the side traffic should pass -- so the two
    variants are generated as mirror images from this one function rather than by rotating a
    model, which would carry the frame and the mat round with it.

    ``world_w`` and ``world_h`` are the size of the rectangle this texture will be stretched
    across, and the bands are computed in THOSE units rather than in pixels of the sprite. That
    is what keeps a stripe at 45 degrees where it is seen. Banding in pixel space instead makes
    the angle a function of the panel's aspect: on a barricade rail, six times wider than it is
    tall, 45 degrees in the sprite arrives as nine degrees on the rail and the stripes read as
    horizontal lines. The block atlas has no wrap mode, so tiling the texture across the rail is
    not the alternative -- drawing it for the shape is.
    """
    img = Image.new("RGBA", (size, size), base + (255,))
    panel_w = size * SWATCH_U0
    for r in range(size):
        for c in range(size):
            if c >= panel_w:
                continue
            x = (c + 0.5) / panel_w * world_w
            y = (1.0 - (r + 0.5) / size) * world_h
            d = (x + y) if slope_right else (x - y)
            band = math.floor(d / stripe_world) % 2
            color = stripe if band == 0 else base
            img.putpixel((c, r), color + (255,))

    draw_swatches(img, base, WHITE, accent)
    return img


def arrow_board_image():
    """The chassis texture: flat swatches only.

    The panel and its lamps are drawn by the renderer now, so nothing here needs a lamp grid --
    which is the point, since a lamp baked into a texture is a lamp that cannot animate.
    """
    img = Image.new("RGBA", (TEX_SIZE, TEX_SIZE), ARROW_FRAME_ORANGE + (255,))
    draw_swatches(img, ARROW_FRAME_ORANGE, ARROW_FRAME_ORANGE, AMBER)
    return img


def sand_barrel_texture():
    # A sand barrel carries no reflective banding of its own: it is a plain yellow module with a
    # darker lid, and it is the array of them that channelizes rather than any marking on one.
    return band_image(SAND_YELLOW, SAND_YELLOW_DARK, [(12.20, 13.15)], SAND_YELLOW_DARK,
                      SAND_YELLOW_DARK, base_bottom=(BLACK, BLACK_LIGHT), bottom_to=0.60)


# --- catalogue -------------------------------------------------------------------------------------
DEVICES = {
    "traffic_cone": {
        "model": "workzone_cone", "build": build_cone,
        "texture": "workzone_cone_orange",
        "texture_fn": lambda: cone_texture(ORANGE, ORANGE_DARK),
        "display": "Traffic Cone",
    },
    "traffic_cone_lime": {
        "model": "workzone_cone", "build": None,
        "texture": "workzone_cone_lime",
        "texture_fn": lambda: cone_texture(LIME, LIME_DARK),
        "display": "Traffic Cone (Lime)",
    },
    "traffic_cone_knocked": {
        "model": "workzone_cone_knocked", "build": build_cone_knocked,
        "texture": "workzone_cone_orange",
        "texture_fn": lambda: cone_texture(ORANGE, ORANGE_DARK),
        "display": "Traffic Cone (Knocked Over)",
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "traffic_drum": {
        "model": "workzone_drum_lit", "build": lambda m: build_drum(m, lit=True),
        "texture": "workzone_drum",
        "texture_fn": drum_texture,
        "display": "Traffic Drum",
        "diagonal": True, "java": "BlockWorkZoneDeviceFlashing",
    },
    "traffic_drum_unlit": {
        "model": "workzone_drum", "build": lambda m: build_drum(m, lit=False),
        "texture": "workzone_drum",
        "texture_fn": drum_texture,
        "display": "Traffic Drum (No Light)",
    },
    "channelizer_tube": {
        "v_span": 18.0,
        "model": "workzone_channelizer", "build": build_channelizer,
        "texture": "workzone_channelizer_orange",
        "texture_fn": lambda: channelizer_texture(ORANGE, ORANGE_DARK),
        "display": "Channelizer",
    },
    "channelizer_tube_lime": {
        "v_span": 18.0,
        "model": "workzone_channelizer", "build": None,
        "texture": "workzone_channelizer_lime",
        "texture_fn": lambda: channelizer_texture(LIME, LIME_DARK),
        "display": "Channelizer (Lime)",
    },
    "channelizer_cade_left": {
        "model": "workzone_channelizer_cade", "build": build_channelizer_cade,
        "texture": "workzone_cade_left",
        "texture_fn": lambda: diagonal_stripe_image(False, CADE_PANEL_W, CADE_PANEL_H),
        "display": "Channelizer-Cade (Keep Left)",
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "channelizer_cade_right": {
        "model": "workzone_channelizer_cade", "build": None,
        "texture": "workzone_cade_right",
        "texture_fn": lambda: diagonal_stripe_image(True, CADE_PANEL_W, CADE_PANEL_H),
        "display": "Channelizer-Cade (Keep Right)",
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "barricade_type_1_left": {
        "barricade": "type1", "top": "BarricadeGeometry.TYPE1_TOP",
        "model": "workzone_barricade_type1",
        "texture": "workzone_rail_left",
        "texture_fn": lambda: diagonal_stripe_image(False, BARRICADE_RAIL_W, BARRICADE_RAIL_H, BARRICADE_STRIPE),
        "display": "Type I Barricade (Keep Left)",
        "diagonal": True, "java": "BlockWorkZoneBarricade",
    },
    "barricade_type_1_right": {
        "barricade": "type1", "top": "BarricadeGeometry.TYPE1_TOP",
        "model": "workzone_barricade_type1", "build": None,
        "texture": "workzone_rail_right",
        "texture_fn": lambda: diagonal_stripe_image(True, BARRICADE_RAIL_W, BARRICADE_RAIL_H, BARRICADE_STRIPE),
        "display": "Type I Barricade (Keep Right)",
        "diagonal": True, "java": "BlockWorkZoneBarricade",
    },
    "barricade_type_2_left": {
        "model": "workzone_barricade_type2", "build": build_barricade_folding,
        "texture": "workzone_fold_rail_left",
        "texture_fn": lambda: diagonal_stripe_image(False, FOLD_RAIL_W, FOLD_RAIL_H, BARRICADE_STRIPE),
        "display": "Type II Folding Barricade (Keep Left)",
        "diagonal": True, "java": "BlockWorkZoneBarricadeFolding",
        "top": "BarricadeGeometry.FOLDING_TOP",
    },
    "barricade_type_2_right": {
        "model": "workzone_barricade_type2", "build": None,
        "texture": "workzone_fold_rail_right",
        "texture_fn": lambda: diagonal_stripe_image(True, FOLD_RAIL_W, FOLD_RAIL_H, BARRICADE_STRIPE),
        "display": "Type II Folding Barricade (Keep Right)",
        "diagonal": True, "java": "BlockWorkZoneBarricadeFolding",
        "top": "BarricadeGeometry.FOLDING_TOP",
    },
    "barricade_type_3_left": {
        "barricade": "type3", "top": "BarricadeGeometry.TYPE3_TOP",
        "model": "workzone_barricade_type3",
        "texture": "workzone_rail_left",
        "texture_fn": lambda: diagonal_stripe_image(False, BARRICADE_RAIL_W, BARRICADE_RAIL_H, BARRICADE_STRIPE),
        "display": "Type III Barricade (Keep Left)",
        "diagonal": True, "java": "BlockWorkZoneBarricade",
    },
    "barricade_type_3_right": {
        "barricade": "type3", "top": "BarricadeGeometry.TYPE3_TOP",
        "model": "workzone_barricade_type3", "build": None,
        "texture": "workzone_rail_right",
        "texture_fn": lambda: diagonal_stripe_image(True, BARRICADE_RAIL_W, BARRICADE_RAIL_H, BARRICADE_STRIPE),
        "display": "Type III Barricade (Keep Right)",
        "diagonal": True, "java": "BlockWorkZoneBarricade",
    },
    "delineator_post": {
        "model": "workzone_delineator", "build": build_delineator,
        "texture": "workzone_delineator_white",
        "texture_fn": lambda: delineator_texture(WHITE, WHITE_DIM),
        "display": "Delineator Post",
    },
    "delineator_post_yellow": {
        "model": "workzone_delineator", "build": None,
        "texture": "workzone_delineator_yellow",
        "texture_fn": lambda: delineator_texture(SAND_YELLOW, SAND_YELLOW_DARK),
        "display": "Delineator Post (Yellow)",
    },
    "delineator_zebra": {
        "model": "workzone_zebra_delineator", "build": build_zebra_delineator,
        "texture": "workzone_zebra_delineator",
        "texture_fn": zebra_texture,
        "display": "Zebra Delineator",
        # The one device here that gets all eight facings: it is laid ALONG a line rather than
        # standing across one, and a lane edge does not always run with the block grid.
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "pavement_marker_white": {
        "model": "workzone_pavement_marker", "build": build_pavement_marker,
        "texture": "workzone_pavement_marker_white",
        "texture_fn": lambda: marker_texture(WHITE, WHITE_DIM, MARKER_SILVER, MARKER_SILVER_DARK),
        "emissive_fn": lambda: marker_emissive(WHITE, WHITE_DIM, MARKER_SILVER, MARKER_SILVER_DARK),
        "display": "Temporary Pavement Marker (White)",
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "pavement_marker_yellow": {
        "model": "workzone_pavement_marker", "build": None,
        "texture": "workzone_pavement_marker_yellow",
        "texture_fn": lambda: marker_texture(MARKER_YELLOW, MARKER_YELLOW_DARK, MARKER_GOLD, MARKER_GOLD_DARK),
        "emissive_fn": lambda: marker_emissive(MARKER_YELLOW, MARKER_YELLOW_DARK, MARKER_GOLD, MARKER_GOLD_DARK),
        "display": "Temporary Pavement Marker (Yellow)",
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "pavement_marker_blue": {
        "model": "workzone_pavement_marker", "build": None,
        "texture": "workzone_pavement_marker_blue",
        "texture_fn": lambda: marker_texture(MARKER_BLUE, MARKER_BLUE_DARK, MARKER_SILVER, MARKER_SILVER_DARK),
        "emissive_fn": lambda: marker_emissive(MARKER_BLUE, MARKER_BLUE_DARK, MARKER_SILVER, MARKER_SILVER_DARK),
        "display": "Temporary Pavement Marker (Blue)",
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "pavement_marker_green": {
        "model": "workzone_pavement_marker", "build": None,
        "texture": "workzone_pavement_marker_green",
        "texture_fn": lambda: marker_texture(MARKER_GREEN, MARKER_GREEN_DARK, MARKER_SILVER, MARKER_SILVER_DARK),
        "emissive_fn": lambda: marker_emissive(MARKER_GREEN, MARKER_GREEN_DARK, MARKER_SILVER, MARKER_SILVER_DARK),
        "display": "Temporary Pavement Marker (Bike Lane Green)",
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "pavement_marker_orange": {
        "model": "workzone_pavement_marker", "build": None,
        "texture": "workzone_pavement_marker_orange",
        "texture_fn": lambda: marker_texture(ORANGE, ORANGE_DARK, MARKER_SILVER, MARKER_SILVER_DARK),
        "emissive_fn": lambda: marker_emissive(ORANGE, ORANGE_DARK, MARKER_SILVER, MARKER_SILVER_DARK),
        "display": "Temporary Pavement Marker (Construction Orange)",
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "pavement_marker_red": {
        "model": "workzone_pavement_marker", "build": None,
        "texture": "workzone_pavement_marker_red",
        "texture_fn": lambda: marker_texture(MARKER_RED, MARKER_RED_DARK, MARKER_SILVER, MARKER_SILVER_DARK),
        "emissive_fn": lambda: marker_emissive(MARKER_RED, MARKER_RED_DARK, MARKER_SILVER, MARKER_SILVER_DARK),
        "display": "Temporary Pavement Marker (Red)",
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "channelizing_wall_orange": {
        "model": "workzone_channelizing_wall", "build": None,
        "joining": {
            "model": "workzone_channelizing_wall",
            "pieces": {
                "core": lambda m: swept_wall(m, LCD_PROFILE),
                "end_left": lambda m: swept_wall_end(m, LCD_PROFILE, True),
                "end_right": lambda m: swept_wall_end(m, LCD_PROFILE, False),
                "fill_right": lambda m: swept_wall_fill(m, LCD_PROFILE),
            },
            "properties": {"connectleft": "end_left", "connectright": "end_right"},
            "fill_properties": {"diagfill": "fill_right"},
        },
        "texture": "workzone_channelizing_wall_orange",
        "texture_fn": lambda: wall_texture(ORANGE, ORANGE_DARK, LCD_TOP_BAND, WHITE, WHITE_DIM,
                                           LCD_RIBS),
        "display": "Water-Filled Barrier (Orange)",
        "diagonal": True, "java": "BlockWorkZoneWall",
    },
    "channelizing_wall_white": {
        "model": "workzone_channelizing_wall",
        "joining": {
            "model": "workzone_channelizing_wall",
            "pieces": {
                "core": lambda m: swept_wall(m, LCD_PROFILE),
                "end_left": lambda m: swept_wall_end(m, LCD_PROFILE, True),
                "end_right": lambda m: swept_wall_end(m, LCD_PROFILE, False),
                "fill_right": lambda m: swept_wall_fill(m, LCD_PROFILE),
            },
            "properties": {"connectleft": "end_left", "connectright": "end_right"},
            "fill_properties": {"diagfill": "fill_right"},
        },
        "texture": "workzone_channelizing_wall_white",
        "texture_fn": lambda: wall_texture(WHITE, WHITE_DIM, LCD_TOP_BAND, ORANGE, ORANGE_DARK,
                                           LCD_RIBS),
        "display": "Water-Filled Barrier (White)",
        "diagonal": True, "java": "BlockWorkZoneWall",
    },
    "concrete_barrier": {
        "model": "workzone_concrete_barrier",
        "joining": {
            "model": "workzone_concrete_barrier",
            "pieces": {
                "core": lambda m: swept_wall(m, BARRIER_PROFILE, swatch_v=SWATCH_BASE_V),
                "end_left": lambda m: swept_wall_end(m, BARRIER_PROFILE, True,
                                                     swatch_v=SWATCH_BASE_V),
                "end_right": lambda m: swept_wall_end(m, BARRIER_PROFILE, False,
                                                      swatch_v=SWATCH_BASE_V),
                "fill_right": lambda m: swept_wall_fill(m, BARRIER_PROFILE,
                                                        swatch_v=SWATCH_BASE_V),
            },
            "properties": {"connectleft": "end_left", "connectright": "end_right"},
            "fill_properties": {"diagfill": "fill_right"},
        },
        "texture": "workzone_concrete_barrier",
        "texture_fn": lambda: wall_texture(CONCRETE, CONCRETE_DARK),
        "display": "Temporary Concrete Barrier",
        "diagonal": True, "java": "BlockWorkZoneWall",
    },
    "portable_signal_trailer_arm": {
        "model": "workzone_signal_trailer_arm",
        "build": lambda m: build_signal_trailer(m, "arm"),
        "inventory_model": "workzone_signal_trailer_arm_inv",
        "inventory_build": lambda m: build_signal_trailer_inventory(m, "arm"),
        "texture": "workzone_signal_trailer",
        "texture_fn": trailer_texture,
        "display": "Portable Signal Trailer (Mast Arm)",
        # The model reaches eight cells out and eight up; its BOX is the TRAILER only, the way
        # the arrow board's is. A box tall enough to hold the mast would also be a collision box
        # tall enough to wall the road off, and one long enough to hold the boom would have the
        # player selecting the trailer from half way across the road.
        "bbox": (-30.0, 0.0, -5.0, 34.0, 22.4, 21.0),
        "rotatable": True, "java": "BlockWorkZoneDeviceRotatable",
    },
    "portable_signal_trailer": {
        "model": "workzone_signal_trailer_mast",
        "build": lambda m: build_signal_trailer(m, "mast"),
        "inventory_model": "workzone_signal_trailer_mast_inv",
        "inventory_build": lambda m: build_signal_trailer_inventory(m, "mast"),
        "texture": "workzone_signal_trailer",
        "texture_fn": trailer_texture,
        "display": "Portable Signal Trailer",
        "bbox": (-18.0, 0.0, 0.0, 26.0, 21.6, 16.0),
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "portable_ped_signal_trailer": {
        "model": "workzone_signal_trailer_ped",
        "build": lambda m: build_signal_trailer(m, "ped"),
        "inventory_model": "workzone_signal_trailer_ped_inv",
        "inventory_build": lambda m: build_signal_trailer_inventory(m, "ped"),
        "texture": "workzone_signal_trailer",
        "texture_fn": trailer_texture,
        "display": "Portable Pedestrian Signal Trailer",
        "bbox": (-18.0, 0.0, 0.0, 26.0, 21.6, 16.0),
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "vertical_panel_left": {
        "model": "workzone_vertical_panel", "build": build_vertical_panel,
        "texture": "workzone_vpanel_left",
        "texture_fn": lambda: diagonal_stripe_image(False, VPANEL_W, VPANEL_H, BARRICADE_STRIPE),
        "display": "Vertical Panel (Keep Left)",
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "vertical_panel_right": {
        "model": "workzone_vertical_panel", "build": None,
        "texture": "workzone_vpanel_right",
        "texture_fn": lambda: diagonal_stripe_image(True, VPANEL_W, VPANEL_H, BARRICADE_STRIPE),
        "display": "Vertical Panel (Keep Right)",
        "diagonal": True, "java": "BlockWorkZoneDeviceDiagonal",
    },
    "road_plate": {
        "model": "workzone_road_plate",
        "joining": {
            "model": "workzone_road_plate",
            "pieces": dict([("core", build_road_plate_core)] + [
                ("edge_%s" % side, (lambda sd: lambda m: build_road_plate_edge(m, sd))(side))
                for side, _n in PLATE_SIDES]),
            "properties": dict(("connect%s" % side, "edge_%s" % side)
                               for side, _n in PLATE_SIDES),
        },
        "texture": "workzone_road_plate",
        "texture_fn": road_plate_texture,
        "display": "Steel Road Plate",
        "java": "BlockWorkZonePlate",
    },
    "safety_fence": {
        "model": "workzone_safety_fence", "build": build_safety_fence,
        # No end pieces: the panel already spans the cell and the stakes are inset, so a square
        # run needs nothing. Only the diagonal filler, for the gap a forty-five degree run leaves.
        "joining": {
            "model": "workzone_safety_fence",
            "pieces": {
                "core": build_safety_fence,
                "fill_right": build_safety_fence_fill,
            },
            "properties": {},
            "fill_properties": {"diagfill": "fill_right"},
        },
        "texture": "workzone_safety_fence",
        "texture_fn": safety_fence_texture,
        "display": "Safety Fence",
        "diagonal": True, "java": "BlockWorkZoneFence",
    },
    "arrow_board": {
        "model": "workzone_arrow_board", "build": build_arrow_board,
        "inventory_model": "workzone_arrow_board_inv",
        "inventory_build": build_arrow_board_inventory,
        "texture": "workzone_arrow_board",
        "texture_fn": arrow_board_image,
        "display": "Arrow Board",
        "diagonal": True, "java": "BlockWorkZoneArrowBoard",
    },
    "sand_barrel_array": {
        "model": "workzone_sand_barrel", "build": build_sand_barrel,
        "texture": "workzone_sand_barrel",
        "texture_fn": sand_barrel_texture,
        "display": "Sand Barrel",
    },
}


# --- outputs -----------------------------------------------------------------------------------------
def check_uvs(mesh, name):
    """Fail loudly if any UV left the 0-1 range Forge's OBJ loader insists on.

    This is the guard for the failure that has no other symptom: the model loads as the purple
    and black placeholder, the log buries one UVsOutOfBoundsException among the knock-on errors,
    and every offline check still passes.
    """
    bad = [t for t in mesh.vt if not (0.0 <= t[0] <= 1.0 and 0.0 <= t[1] <= 1.0)]
    if bad:
        raise AssertionError(
            "%s: %d UVs outside 0-1 (e.g. %r). Raise that device's v_span above its tallest "
            "band-strip geometry." % (name, len(bad), bad[0]))


def mesh_bounds(mesh):
    """(minX, minY, minZ, maxX, maxY, maxZ) of a mesh, in 1/16 units."""
    xs = [p[0] for p in mesh.v]
    ys = [p[1] for p in mesh.v]
    zs = [p[2] for p in mesh.v]
    return (min(xs), min(ys), min(zs), max(xs), max(ys), max(zs))


# Facing name to model rotation, matching DirectionEight.getRotationDegrees() on the Java
# side. A renderer drawing part of a device has to turn by the same amount as the baked
# model beside it, so the two tables must not drift.
DIAGONAL_FACINGS = (("n", 0), ("nw", 45), ("w", 90), ("sw", 135),
                    ("s", 180), ("se", 225), ("e", 270), ("ne", 315))


def facing_variants(spec):
    """The `facing` variant block for a device, four-way or eight-way.

    Shared by all three blockstate writers. It used to be written out in each of them, and the
    two that draw a device as several pieces were still hardcoded to four facings long after the
    plain one had grown eight -- so a device could be given eight facings in DEVICES, take them
    in its block class, and silently render only four.

    The in-between angles cannot use the variant's own "y" shorthand, which only takes right
    angles. The OBJ loader takes an explicit transform rotation at any angle instead.
    """
    if spec.get("diagonal"):
        return {
            name: ({} if degrees == 0 else
                   {"transform": {"rotation": [{"x": 0}, {"y": degrees}, {"z": 0}]}})
            for name, degrees in DIAGONAL_FACINGS
        }
    if spec.get("rotatable"):
        return {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
    return None


def blockstate_json(spec):
    """The Forge blockstate for one device.

    The MTL already names a texture, so the ``#body`` override here is what lets two colour
    variants share one OBJ: each blockstate repaints the single material. ``particle`` comes
    from the material either way, so it is not set separately.
    """
    model = "csm:trafficaccessories/shared_models/%s.obj" % spec["model"]
    texture = "%s/%s" % (TEXTURE_PREFIX, spec["texture"])
    variants = {}
    facing = facing_variants(spec)
    if facing is not None:
        variants["facing"] = facing
    variants["normal"] = [{}]
    inventory = {"transform": "forge:default-block"}
    if spec.get("inventory_model"):
        # A device drawn bigger than a block needs its own icon model: the item transform assumes
        # what it is handed fits in a unit cube and silently draws anything larger over the slots
        # around it.
        inventory["model"] = ("csm:trafficaccessories/shared_models/%s.obj"
                              % spec["inventory_model"])
    variants["inventory"] = [inventory]
    return {
        "forge_marker": 1,
        "defaults": {
            "model": model,
            "custom": {"flip-v": True},
            "textures": {"#%s" % MATERIAL: texture},
        },
        "variants": variants,
    }


def java_bbox(bounds):
    """A CSM bounding box literal, in block space, from mesh bounds in 1/16 units."""
    return ("new AxisAlignedBB(%.6f, %.6f, %.6f, %.6f, %.6f, %.6f)"
            % tuple(v / 16.0 for v in bounds))


def write_mtl(path, texture_name):
    with open(path, "w", newline="\n") as fh:
        fh.write("# Procedurally generated by dev-env-utils/scripts/gen_work_zone_devices.py"
                 " -- do not hand edit\n")
        fh.write("# One material; every colour variant's blockstate retextures #body.\n")
        fh.write("newmtl %s\n" % MATERIAL)
        fh.write("map_Kd %s/%s\n" % (TEXTURE_PREFIX, texture_name))


def generate(model_dir, texture_dir, blockstate_dir, fragment_dir, only=None):
    for d in (model_dir, texture_dir, blockstate_dir, fragment_dir):
        os.makedirs(d, exist_ok=True)
    written = []
    seen_models = set()
    bounds = {}

    written.extend(write_barricade_models(model_dir))
    for name in BARRICADE_TYPES:
        write_mtl(os.path.join(model_dir, "workzone_barricade_%s.mtl" % name),
                  "workzone_rail_right")

    for registry, spec in DEVICES.items():
        if only and registry not in only:
            continue
        if "joining" in spec:
            stem = spec["joining"]["model"]
            if stem not in seen_models:
                seen_models.add(stem)
                set_v_span(spec.get("v_span", 16.0))
                paths, box_bounds = write_joining_models(model_dir, spec)
                written.extend(paths)
                bounds[stem] = box_bounds
            tex_path = os.path.join(texture_dir, spec["texture"] + ".png")
            if tex_path not in written:
                set_v_span(spec.get("v_span", 16.0))
                spec["texture_fn"]().save(tex_path)
                written.append(tex_path)
            bs_path = os.path.join(blockstate_dir, registry + ".json")
            with open(bs_path, "w", newline="\n") as fh:
                json.dump(joining_blockstate(spec), fh, indent=2)
                fh.write("\n")
            written.append(bs_path)
            continue
        if "barricade" in spec:
            tex_path = os.path.join(texture_dir, spec["texture"] + ".png")
            if tex_path not in written:
                spec["texture_fn"]().save(tex_path)
                written.append(tex_path)
            bs_path = os.path.join(blockstate_dir, registry + ".json")
            with open(bs_path, "w", newline="\n") as fh:
                json.dump(barricade_blockstate(spec), fh, indent=2)
                fh.write("\n")
            written.append(bs_path)
            continue
        inv_model = spec.get("inventory_model")
        if inv_model is not None and inv_model not in seen_models:
            seen_models.add(inv_model)
            set_v_span(spec.get("v_span", 16.0))
            inv_mesh = Mesh()
            spec["inventory_build"](inv_mesh)
            check_uvs(inv_mesh, inv_model)
            inv_path = os.path.join(model_dir, inv_model + ".obj")
            inv_mesh.write(inv_path, inv_model, spec["model"] + ".mtl")
            written.append(inv_path)

        model = spec["model"]
        if spec["build"] is not None and model not in seen_models:
            seen_models.add(model)
            set_v_span(spec.get("v_span", 16.0))
            mesh = Mesh()
            spec["build"](mesh)
            check_uvs(mesh, model)
            bounds[model] = mesh_bounds(mesh)
            obj_path = os.path.join(model_dir, model + ".obj")
            mesh.write(obj_path, model, model + ".mtl")
            written.append(obj_path)
            mtl_path = os.path.join(model_dir, model + ".mtl")
            write_mtl(mtl_path, spec["texture"])
            written.append(mtl_path)

        set_v_span(spec.get("v_span", 16.0))
        tex_path = os.path.join(texture_dir, spec["texture"] + ".png")
        if tex_path not in written:
            spec["texture_fn"]().save(tex_path)
            written.append(tex_path)
        if spec.get("emissive_fn") is not None:
            # OptiFine picks this up by name; nothing references it otherwise.
            emissive_path = os.path.join(texture_dir, spec["texture"] + "_e.png")
            spec["emissive_fn"]().save(emissive_path)
            written.append(emissive_path)

        bs_path = os.path.join(blockstate_dir, registry + ".json")
        with open(bs_path, "w", newline="\n") as fh:
            json.dump(blockstate_json(spec), fh, indent=2)
            fh.write("\n")
        written.append(bs_path)

    # Fragments, for pasting into the lang file and the tab. Written rather than applied so the
    # generator never has to parse and rewrite files it does not own.
    lang_path = os.path.join(fragment_dir, "lang_fragment.txt")
    with open(lang_path, "w", newline="\n") as fh:
        for registry, spec in DEVICES.items():
            if only and registry not in only:
                continue
            fh.write("tile.%s.name=%s\n" % (registry, spec["display"]))
    written.append(lang_path)

    tab_path = os.path.join(fragment_dir, "tab_fragment.java")
    with open(tab_path, "w", newline="\n") as fh:
        fh.write("// Generated by gen_work_zone_devices.py -- bounding boxes are the models'\n")
        fh.write("// own extents, so they follow the geometry rather than being eyeballed.\n")
        for registry, spec in DEVICES.items():
            if only and registry not in only:
                continue
            b = (BARRICADE_BOUNDS.get(spec["barricade"]) if "barricade" in spec
                 else spec.get("bbox") or bounds.get(spec["model"]))
            if b is None:
                continue
            cls = spec.get("java", "BlockWorkZoneDevice")
            if "top" in spec:
                # A barricade also takes its height, so the renderer knows where to clamp a
                # warning light without having to guess the type from the registry name.
                fh.write('initTabBlock(new %s("%s", %s,\n    %s));\n'
                         % (cls, registry, spec["top"], java_bbox(b)))
            else:
                fh.write('initTabBlock(new %s("%s",\n    %s));\n'
                         % (cls, registry, java_bbox(b)))
    written.append(tab_path)

    written.append(write_geometry_constants())
    written.append(write_signal_trailer_geometry())
    written.append(write_barricade_geometry())

    return written


def write_joining_models(model_dir, spec):
    """Emit a joining device as a core, one model per detachable piece, and an inventory model.

    The barricades have their own version of this above, kept separate because their pieces are
    generated per barricade TYPE rather than per device. Everything else that joins comes through
    here.
    """
    joining = spec["joining"]
    stem = joining["model"]
    written = []
    for suffix, builder in joining["pieces"].items():
        mesh = Mesh()
        builder(mesh)
        check_uvs(mesh, "%s_%s" % (stem, suffix))
        path = os.path.join(model_dir, "%s_%s.obj" % (stem, suffix))
        mesh.write(path, "%s_%s" % (stem, suffix), stem + ".mtl")
        written.append(path)

    # An item has no neighbours to ask, so its model is every piece at once -- except the gap
    # filler, which is not part of what a device looks like on its own. Leaving it in would put a
    # stub of wall out of the side of the icon, and would grow the block's bounding box to the
    # sqrt(2) cells the filler reaches, so a single wall would claim the cell beside it.
    fillers = set(joining.get("fill_properties", {}).values())
    inv = Mesh()
    for suffix, builder in joining["pieces"].items():
        if suffix in fillers:
            continue
        builder(inv)
    check_uvs(inv, stem + "_inv")
    inv_path = os.path.join(model_dir, stem + "_inv.obj")
    inv.write(inv_path, stem + "_inv", stem + ".mtl")
    written.append(inv_path)

    mtl_path = os.path.join(model_dir, stem + ".mtl")
    write_mtl(mtl_path, spec["texture"])
    written.append(mtl_path)
    return written, mesh_bounds(inv)


def joining_blockstate(spec):
    """The blockstate for one joining device: each piece appears only where nothing connects."""
    joining = spec["joining"]
    stem = joining["model"]
    texture = "%s/%s" % (TEXTURE_PREFIX, spec["texture"])
    model = "csm:trafficaccessories/shared_models/%s" % stem

    def submodel(suffix):
        return {"submodel": {suffix: {"model": "%s_%s.obj" % (model, suffix),
                                      "custom": {"flip-v": True},
                                      "textures": {"#%s" % MATERIAL: texture}}}}

    variants = {}
    facing = facing_variants(spec)
    if facing is not None:
        variants["facing"] = facing
    # The piece is on the FALSE side: it is drawn where nothing connects.
    for prop, piece in joining["properties"].items():
        variants[prop] = {"false": submodel(piece), "true": {}}
    # The gap filler is the other way round: drawn only where this device DOES join, and only
    # when the joint is diagonal.
    for prop, piece in joining.get("fill_properties", {}).items():
        variants[prop] = {"false": {}, "true": submodel(piece)}
    variants["normal"] = [{}]
    variants["inventory"] = [{"model": "%s_inv.obj" % model,
                              "custom": {"flip-v": True},
                              "textures": {"#%s" % MATERIAL: texture},
                              "transform": "forge:default-block"}]
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "%s_core.obj" % model,
            "custom": {"flip-v": True},
            "textures": {"#%s" % MATERIAL: texture},
        },
        "variants": variants,
    }


def write_barricade_models(model_dir):
    """Emit each barricade type as a core plus two detachable ends.

    A barricade is the one device here whose model depends on its NEIGHBOURS, so it cannot be a
    single baked shape. The core is what every barricade draws; the ends are added only where
    nothing connects. The inventory model is core plus both ends, since an item has no
    neighbours to ask.
    """
    written = []
    for name, (rails, top_y) in BARRICADE_TYPES.items():
        stem = "workzone_barricade_%s" % name
        pieces = {
            "core": lambda m, r=rails, t=top_y: build_barricade_core(m, r, t),
            "end_left": lambda m, r=rails, t=top_y: build_barricade_end(m, r, t, True),
            "end_right": lambda m, r=rails, t=top_y: build_barricade_end(m, r, t, False),
            "fill_right": lambda m, r=rails, t=top_y: build_barricade_fill(m, r, t),
        }
        for suffix, builder in pieces.items():
            mesh = Mesh()
            builder(mesh)
            check_uvs(mesh, "%s_%s" % (stem, suffix))
            path = os.path.join(model_dir, "%s_%s.obj" % (stem, suffix))
            mesh.write(path, "%s_%s" % (stem, suffix), stem + ".mtl")
            written.append(path)

        inv = Mesh()
        build_barricade_core(inv, rails, top_y)
        build_barricade_end(inv, rails, top_y, True)
        build_barricade_end(inv, rails, top_y, False)
        check_uvs(inv, stem + "_inv")
        inv_path = os.path.join(model_dir, stem + "_inv.obj")
        inv.write(inv_path, stem + "_inv", stem + ".mtl")
        written.append(inv_path)
        BARRICADE_BOUNDS[name] = mesh_bounds(inv)
    return written


def barricade_blockstate(spec):
    """The blockstate for one barricade: ends appear only where nothing connects."""
    stem = "workzone_barricade_%s" % spec["barricade"]
    texture = "%s/%s" % (TEXTURE_PREFIX, spec["texture"])
    model = "csm:trafficaccessories/shared_models/%s" % stem

    def submodel(suffix):
        return {"submodel": {suffix: {"model": "%s_%s.obj" % (model, suffix),
                                      "custom": {"flip-v": True},
                                      "textures": {"#%s" % MATERIAL: texture}}}}

    return {
        "forge_marker": 1,
        "defaults": {
            "model": "%s_core.obj" % model,
            "custom": {"flip-v": True},
            "textures": {"#%s" % MATERIAL: texture},
        },
        "variants": {
            "facing": facing_variants(spec),
            # The end is on the FALSE side: it is drawn where nothing connects.
            "connectleft": {"false": submodel("end_left"), "true": {}},
            "connectright": {"false": submodel("end_right"), "true": {}},
            # Drawn only where this barricade DOES join, and only when the joint is diagonal.
            "diagfill": {"false": {}, "true": submodel("fill_right")},
            "normal": [{}],
            "inventory": [{"model": "%s_inv.obj" % model,
                           "custom": {"flip-v": True},
                           "textures": {"#%s" % MATERIAL: texture},
                           "transform": "forge:default-block"}],
        },
    }


def write_barricade_geometry():
    """Emit ``BarricadeGeometry``: the numbers the barricade renderer needs.

    The rails and uprights are a baked model; the warning lights and any mounted sign are drawn
    by a renderer on top of them. Both have to agree about where the top of the barricade is and
    where its uprights stand, or a light ends up floating beside the post it is supposed to be
    clamped to.
    """
    path = layout.source_for_write(TRAFFICACCESSORIES_OWNER,
                                   "trafficaccessories/BarricadeGeometry.java")
    values = [
        ("LEFT_UPRIGHT_X", AXIS - BARRICADE_LEG_X),
        ("RIGHT_UPRIGHT_X", AXIS + BARRICADE_LEG_X),
        ("RAIL_HALF_Z", BARRICADE_RAIL_HALF_Z),
        ("RAIL_CENTRE_Z", AXIS),
        ("TYPE1_TOP", BARRICADE_TYPE1_TOP),
        ("TYPE3_TOP", BARRICADE_TYPE3_TOP),
        ("LEG_HALF_X", BARRICADE_LEG_HALF_X),
        ("FOLDING_LEFT_UPRIGHT_X", AXIS - FOLD_UPRIGHT_X),
        ("FOLDING_RIGHT_UPRIGHT_X", AXIS + FOLD_UPRIGHT_X),
        ("FOLDING_RAIL_HALF_Z", FOLD_RAIL_HALF_Z),
        ("FOLDING_RAIL_CENTRE_Z", FOLD_RAIL_CZ),
        ("FOLDING_TOP", FOLD_TOP),
    ]
    lines = [
        "package com.micatechnologies.minecraft.csm.trafficaccessories;",
        "",
        "/**",
        " * Where the parts of a barricade are, in 1/16 block units.",
        " *",
        " * <p>Generated by {@code dev-env-utils/scripts/gen_work_zone_devices.py} -- do not hand",
        " * edit. The rails and uprights are a baked model and the warning lights and mounted signs",
        " * are drawn over them by {@link TileEntityBarricadeRenderer}; these are the numbers both",
        " * are built from.</p>",
        " *",
        " * @version 1.0",
        " * @since 2026.9",
        " */",
        "public final class BarricadeGeometry {",
        "",
    ]
    for name, value in values:
        lines.append("  /** %s, in 1/16 block units. */" % name.replace("_", " ").lower())
        lines.append("  public static final float %s = %.4ff;" % (name, value))
        lines.append("")
    lines += [
        "  private BarricadeGeometry() {",
        "    throw new AssertionError(\"BarricadeGeometry is constants only\");",
        "  }",
        "}",
    ]
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    return path


def write_geometry_constants():
    """Emit ``ArrowBoardGeometry``: the numbers the renderer draws the board from.

    The chassis is a baked model and everything above it is drawn by a tile entity renderer, so
    the two have to agree about where the chassis ends and the mast begins, and about the scale
    they are both at. Emitting the shared numbers is what stops the drawn board floating above,
    or sinking into, the trailer it is bolted to.

    Everything is written at FINAL scale, in 1/16 block units, ready to use.
    """
    path = layout.source_for_write(TRAFFICACCESSORIES_OWNER,
                                   "trafficaccessories/ArrowBoardGeometry.java")

    def sx(v):
        return AXIS + (v - AXIS) * ARROW_SCALE

    def sy(v):
        return v * ARROW_SCALE

    cols, rows = ARROW_GRID
    lit_x0, lit_x1 = sx(ARROW_PANEL[0] + ARROW_FRAME), sx(ARROW_PANEL[1] - ARROW_FRAME)
    lit_y0, lit_y1 = sy(ARROW_PANEL[2] + ARROW_FRAME), sy(ARROW_PANEL[3] - ARROW_FRAME)
    cell_w = (lit_x1 - lit_x0) / cols
    cell_h = (lit_y1 - lit_y0) / rows
    values = [
        ("MAST_X0", sx(ARROW_MAST_X[0])), ("MAST_X1", sx(ARROW_MAST_X[1])),
        ("MAST_HALF", ARROW_MAST_HALF * ARROW_SCALE),
        ("MAST_Y0", sy(ARROW_CHASSIS[2] + 0.20)),
        # Stops just inside the panel, which then hides it. Taking the brace height here
        # instead ran the posts up the FRONT of the lamp grid.
        ("MAST_Y1", sy(ARROW_PANEL[2] + 0.60)),
        ("BRACE_Y0", sy(ARROW_MAST_BRACE[0])), ("BRACE_Y1", sy(ARROW_MAST_BRACE[1])),
        ("BRACE_HALF_Y", 0.30 * ARROW_SCALE),
        ("PANEL_X0", sx(ARROW_PANEL[0])), ("PANEL_X1", sx(ARROW_PANEL[1])),
        ("PANEL_Y0", sy(ARROW_PANEL[2])), ("PANEL_Y1", sy(ARROW_PANEL[3])),
        ("PANEL_Z0", sx(ARROW_PANEL_Z[0])), ("PANEL_Z1", sx(ARROW_PANEL_Z[1])),
        ("LIT_X0", lit_x0), ("LIT_Y0", lit_y0),
        ("CELL_W", cell_w), ("CELL_H", cell_h),
        ("LAMP_RADIUS", 0.34 * min(cell_w, cell_h)),
    ]
    lines = [
        "package com.micatechnologies.minecraft.csm.trafficaccessories;",
        "",
        "/**",
        " * Where the parts of an arrow board are, in 1/16 block units.",
        " *",
        " * <p>Generated by {@code dev-env-utils/scripts/gen_work_zone_devices.py} -- do not hand",
        " * edit. The chassis is a baked model and everything above it is drawn by",
        " * {@link TileEntityArrowBoardRenderer}; these are the numbers both are built from, so the",
        " * drawn mast meets the modelled trailer instead of floating over it.</p>",
        " *",
        " * @version 1.0",
        " * @since 2026.9",
        " */",
        "public final class ArrowBoardGeometry {",
        "",
        "  /** Lamp columns across the panel. */",
        "  public static final int GRID_COLS = %d;" % cols,
        "",
        "  /** Lamp rows down the panel. */",
        "  public static final int GRID_ROWS = %d;" % rows,
        "",
    ]
    for name, value in values:
        lines.append("  /** %s, in 1/16 block units. */" % name.replace("_", " ").lower())
        lines.append("  public static final float %s = %.4ff;" % (name, value))
        lines.append("")
    lines += [
        "  private ArrowBoardGeometry() {",
        "    throw new AssertionError(\"ArrowBoardGeometry is constants only\");",
        "  }",
        "}",
    ]
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    return path


def write_signal_trailer_geometry():
    """Emit ``SignalTrailerGeometry``: where the arm trailer's boom is, cell by cell.

    A mount kit clamped to the boom has to know where the boom IS, and the boom is not a block
    -- it is geometry belonging to a trailer up to eight cells away. Nothing in the world can be
    probed for it. So the cell layout is emitted from the same constants the boom is swept from,
    and the block that looks for it reads these rather than carrying its own copy of the numbers.
    That is the whole point: the placement code cannot disagree with the geometry it was built
    from, because there is only one set of numbers.

    Everything is in 1/16 block units, in the frame of the CELL the boom passes through: the bar
    is centred on the cell in the axis across the boom, and its underside is the cell's floor.
    """
    path = layout.source_for_write(TRAFFICACCESSORIES_OWNER,
                                   "trafficaccessories/SignalTrailerGeometry.java")

    # Model z of the near face of the trailer's own cell is +16, and one cell is 16 units, so
    # cell n out spans model z from 16-16n to -16n. Cell FIRST is the one the elbow lands on.
    first = int(round((16.0 - PSIG_ELBOW_Z) / 16.0))          # 3
    last = int(round((16.0 - PSIG_ARM_TIP_Z) / 16.0)) - 1      # 8
    height = int(round(PSIG_ARM_CLEARANCE / 16.0))             # 8

    elb_h, elb_t = PSIG_BOOM_ELBOW
    tip_h, tip_t = PSIG_BOOM_TIP

    halves, thicks = [], []
    for n in range(first, last + 1):
        centre_z = 16.0 - 16.0 * n - 8.0
        t = (centre_z - PSIG_ELBOW_Z) / (PSIG_ARM_TIP_Z - PSIG_ELBOW_Z)
        halves.append(elb_h + t * (tip_h - elb_h))
        thicks.append(elb_t + t * (tip_t - elb_t))

    def arr(values):
        return "{" + ", ".join("%.4ff" % v for v in values) + "}"

    lines = [
        "package com.micatechnologies.minecraft.csm.trafficaccessories;",
        "",
        "/**",
        " * Where a portable signal trailer's boom is, cell by cell, in 1/16 block units.",
        " *",
        " * <p>Generated by {@code dev-env-utils/scripts/gen_work_zone_devices.py} -- do not hand",
        " * edit.</p>",
        " *",
        " * <p>The boom is not a block. It is geometry belonging to a trailer up to eight cells",
        " * away, so a mount kit that wants to clamp to it cannot probe the world for it and has to",
        " * be told. These are the same numbers the boom is swept from, which is what stops the",
        " * clamp drifting off the bar the next time the boom is reshaped.</p>",
        " *",
        " * <p>Coordinates are in the frame of the CELL the boom passes through: the bar is centred",
        " * on the cell across the boom's run, and its underside is that cell's floor.</p>",
        " *",
        " * @version 1.0",
        " * @since 2026.9",
        " */",
        "public final class SignalTrailerGeometry {",
        "",
        "  /** Registry name of the trailer style that carries a boom. */",
        "  public static final String ARM_TRAILER = \"portable_signal_trailer_arm\";",
        "",
        "  /** Cells above the trailer's own block that the boom's level run sits at. */",
        "  public static final int BOOM_HEIGHT = %d;" % height,
        "",
        "  /** First cell out from the trailer that the LEVEL run covers; nearer cells are the rise. */",
        "  public static final int FIRST_CELL = %d;" % first,
        "",
        "  /** Last cell out from the trailer that the level run covers. */",
        "  public static final int LAST_CELL = %d;" % last,
        "",
        "  /** Half the bar's width, per cell from {@link #FIRST_CELL}. The boom tapers. */",
        "  public static final float[] HALF_WIDTH = %s;" % arr(halves),
        "",
        "  /** The bar's depth above the cell floor, per cell from {@link #FIRST_CELL}. */",
        "  public static final float[] THICKNESS = %s;" % arr(thicks),
        "",
        "  /** True if {@code cell} is a cell the level run covers. */",
        "  public static boolean isLevelRunCell(int cell) {",
        "    return cell >= FIRST_CELL && cell <= LAST_CELL;",
        "  }",
        "",
        "  /** Half the bar's width at {@code cell}; the nearest covered cell if it is out of range. */",
        "  public static float halfWidthAt(int cell) {",
        "    return HALF_WIDTH[clampIndex(cell)];",
        "  }",
        "",
        "  /** The bar's depth above the cell floor at {@code cell}, clamped the same way. */",
        "  public static float thicknessAt(int cell) {",
        "    return THICKNESS[clampIndex(cell)];",
        "  }",
        "",
        "  private static int clampIndex(int cell) {",
        "    int index = cell - FIRST_CELL;",
        "    if (index < 0) {",
        "      return 0;",
        "    }",
        "    if (index >= HALF_WIDTH.length) {",
        "      return HALF_WIDTH.length - 1;",
        "    }",
        "    return index;",
        "  }",
        "",
        "  private SignalTrailerGeometry() {",
        "    throw new AssertionError(\"SignalTrailerGeometry is constants only\");",
        "  }",
        "}",
    ]
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--scratch", action="store_true",
                        help="write into _workzone_out/ instead of the repo tree")
    parser.add_argument("--only", nargs="*", default=None,
                        help="only generate these registry names")
    args = parser.parse_args()

    if args.scratch:
        model_dir = os.path.join(SCRATCH_DIR, "models")
        texture_dir = os.path.join(SCRATCH_DIR, "textures")
        blockstate_dir = os.path.join(SCRATCH_DIR, "blockstates")
    else:
        model_dir, texture_dir = MODEL_DIR, TEXTURE_DIR
        blockstate_dir = BLOCKSTATE_DIR

    for path in generate(model_dir, texture_dir, blockstate_dir, SCRATCH_DIR, only=args.only):
        print(os.path.relpath(path, layout.REPO_ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
