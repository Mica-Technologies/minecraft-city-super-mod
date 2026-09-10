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


def build_arrow_board(mesh):
    """The arrow board's CHASSIS only: trailer, tongue, wheels and jacks.

    Everything above the chassis -- mast, panel and the lamp grid -- is drawn by
    ``TileEntityArrowBoardRenderer`` instead, because the lamps have to animate and a baked model
    cannot. Splitting it here rather than moving the whole board into the renderer keeps an
    inventory icon and something solid in the world, and leaves the static part small enough to
    sit inside its own cell, where it cannot pop when its chunk section is culled. The tall part
    that would have popped is now tile entity geometry, which is culled by its own render
    bounding box instead.
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

    scale_mesh(mesh, ARROW_SCALE)


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
        "rotatable": True, "java": "BlockWorkZoneDeviceRotatable",
    },
    "traffic_drum": {
        "model": "workzone_drum_lit", "build": lambda m: build_drum(m, lit=True),
        "texture": "workzone_drum",
        "texture_fn": drum_texture,
        "display": "Traffic Drum",
        "rotatable": True, "java": "BlockWorkZoneDeviceFlashing",
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
        "rotatable": True, "java": "BlockWorkZoneDeviceRotatable",
    },
    "channelizer_cade_right": {
        "model": "workzone_channelizer_cade", "build": None,
        "texture": "workzone_cade_right",
        "texture_fn": lambda: diagonal_stripe_image(True, CADE_PANEL_W, CADE_PANEL_H),
        "display": "Channelizer-Cade (Keep Right)",
        "rotatable": True, "java": "BlockWorkZoneDeviceRotatable",
    },
    "barricade_type_1_left": {
        "barricade": "type1",
        "model": "workzone_barricade_type1",
        "texture": "workzone_rail_left",
        "texture_fn": lambda: diagonal_stripe_image(False, BARRICADE_RAIL_W, BARRICADE_RAIL_H, BARRICADE_STRIPE),
        "display": "Type I Barricade (Keep Left)",
        "rotatable": True, "java": "BlockWorkZoneBarricade",
    },
    "barricade_type_1_right": {
        "barricade": "type1",
        "model": "workzone_barricade_type1", "build": None,
        "texture": "workzone_rail_right",
        "texture_fn": lambda: diagonal_stripe_image(True, BARRICADE_RAIL_W, BARRICADE_RAIL_H, BARRICADE_STRIPE),
        "display": "Type I Barricade (Keep Right)",
        "rotatable": True, "java": "BlockWorkZoneBarricade",
    },
    "barricade_type_3_left": {
        "barricade": "type3",
        "model": "workzone_barricade_type3",
        "texture": "workzone_rail_left",
        "texture_fn": lambda: diagonal_stripe_image(False, BARRICADE_RAIL_W, BARRICADE_RAIL_H, BARRICADE_STRIPE),
        "display": "Type III Barricade (Keep Left)",
        "rotatable": True, "java": "BlockWorkZoneBarricade",
    },
    "barricade_type_3_right": {
        "barricade": "type3",
        "model": "workzone_barricade_type3", "build": None,
        "texture": "workzone_rail_right",
        "texture_fn": lambda: diagonal_stripe_image(True, BARRICADE_RAIL_W, BARRICADE_RAIL_H, BARRICADE_STRIPE),
        "display": "Type III Barricade (Keep Right)",
        "rotatable": True, "java": "BlockWorkZoneBarricade",
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
    "arrow_board": {
        "model": "workzone_arrow_board", "build": build_arrow_board,
        "texture": "workzone_arrow_board",
        "texture_fn": arrow_board_image,
        "display": "Arrow Board",
        "rotatable": True, "java": "BlockWorkZoneArrowBoard",
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


def blockstate_json(spec):
    """The Forge blockstate for one device.

    The MTL already names a texture, so the ``#body`` override here is what lets two colour
    variants share one OBJ: each blockstate repaints the single material. ``particle`` comes
    from the material either way, so it is not set separately.
    """
    model = "csm:trafficaccessories/shared_models/%s.obj" % spec["model"]
    texture = "%s/%s" % (TEXTURE_PREFIX, spec["texture"])
    variants = {}
    if spec.get("rotatable"):
        variants["facing"] = {
            "north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
        }
    variants["normal"] = [{}]
    variants["inventory"] = [{"transform": "forge:default-block"}]
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
                 else bounds.get(spec["model"]))
            if b is None:
                continue
            cls = spec.get("java", "BlockWorkZoneDevice")
            fh.write('initTabBlock(new %s("%s",\n    %s));\n'
                     % (cls, registry, java_bbox(b)))
    written.append(tab_path)

    written.append(write_geometry_constants())

    return written


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
            "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}},
            # The end is on the FALSE side: it is drawn where nothing connects.
            "connectleft": {"false": submodel("end_left"), "true": {}},
            "connectright": {"false": submodel("end_right"), "true": {}},
            "normal": [{}],
            "inventory": [{"model": "%s_inv.obj" % model,
                           "custom": {"flip-v": True},
                           "textures": {"#%s" % MATERIAL: texture},
                           "transform": "forge:default-block"}],
        },
    }


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
