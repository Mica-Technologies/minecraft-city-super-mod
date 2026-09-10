# -*- coding: utf-8 -*-
"""Where every part of a guardrail is, in 1/16 block units.

ONE source of truth, imported by both generators. The rails and the end treatments are built by
separate scripts and have to meet exactly: an end shoe whose rail is two units lower than the run
it terminates is the sort of fault that builds green, loads clean, and is only visible from the
right angle in game.

Frame: the run goes along X and spans the cell exactly, x 0..16, the way the barrier walls do.
The rail's face points toward LOW Z, which is the convention every facing device in the work zone
tab already uses -- a vertical panel's post stands behind its panel at high z. So `facing` on a
guardrail means the way the rail LOOKS, and the run is across it.
"""

CELL = 16.0

# --- the W-beam rail ---------------------------------------------------------------------------
# A real W-beam is 310 mm deep with its top 700 mm off the ground, so at a block to the metre the
# rail is about a fifth of a cell tall sitting two thirds of the way up. Those two numbers are what
# every other part is hung off.
RAIL_TOP_Y = 11.20
RAIL_BOTTOM_Y = 6.20
RAIL_HEIGHT = RAIL_TOP_Y - RAIL_BOTTOM_Y

# The rail's own depth, front face toward traffic. Front is low z.
RAIL_FRONT_Z = 0.80
RAIL_BACK_Z = 4.00

# The W profile across the rail's height, as (z offset back from the front face, y). The shape is
# the whole point of the name: a lip at top and bottom, a valley pressed in at mid height, and a
# flat between each. Drawn front to back, bottom to top.
#
# Offsets are FROM RAIL_FRONT_Z, so 0.0 is the face and larger numbers are deeper.
W_PROFILE = [
    (1.60, RAIL_BOTTOM_Y),           # bottom lip, turned back
    (0.30, RAIL_BOTTOM_Y + 0.90),    # out to the lower crest
    (0.00, RAIL_BOTTOM_Y + 1.60),
    (0.30, RAIL_BOTTOM_Y + 2.20),
    (1.45, RAIL_HEIGHT * 0.5 + RAIL_BOTTOM_Y),   # the valley at mid height
    (0.30, RAIL_TOP_Y - 2.20),
    (0.00, RAIL_TOP_Y - 1.60),
    (0.30, RAIL_TOP_Y - 0.90),
    (1.60, RAIL_TOP_Y),              # top lip, turned back
]

# --- the post and its block-out ----------------------------------------------------------------
# From the user's exploded diagram: the rail does not bolt to the post. A spacer -- the block-out --
# holds it off, and on a real installation it is plainly visible from behind. Leaving it out is the
# difference between a guardrail and a plank on a stick.
BLOCKOUT_Z0 = RAIL_BACK_Z
BLOCKOUT_Z1 = 6.40
BLOCKOUT_HALF_X = 1.60
BLOCKOUT_Y0 = RAIL_BOTTOM_Y - 0.20
BLOCKOUT_Y1 = RAIL_TOP_Y + 0.20

POST_Z0 = BLOCKOUT_Z1
POST_Z1 = 9.60
POST_HALF_X = 2.00
POST_TOP_Y = RAIL_TOP_Y + 0.60      # a little proud of the rail, as they stand in reality
POST_BOTTOM_Y = 0.0

# The mirrored half of a double-sided run: block-out and rail again on the far side of the post.
# Centred on the post, so the FRONT rail sits at the same z whichever variant is placed and a
# single-sided run meeting a double-sided one lines up.
POST_CENTRE_Z = (POST_Z0 + POST_Z1) * 0.5
BACK_BLOCKOUT_Z0 = POST_Z1
BACK_BLOCKOUT_Z1 = BACK_BLOCKOUT_Z0 + (BLOCKOUT_Z1 - BLOCKOUT_Z0)
BACK_RAIL_FRONT_Z = BACK_BLOCKOUT_Z1 + (RAIL_BACK_Z - RAIL_FRONT_Z)
BACK_RAIL_BACK_Z = BACK_BLOCKOUT_Z1

# --- slopes ------------------------------------------------------------------------------------
# A run on a grade ramps rather than staircases. `up` means the rail RISES across this cell toward
# its right-hand end, which is the direction WorkZoneJoins already calls right. One cell of rise
# per cell of run is the steepest a block grid can express and is what a 45 degree bank gives.
SLOPE_RISE = CELL
SLOPES = ("flat", "up", "down")

# How much a sloped cell lifts the rail at a given fraction along its run, for `up`. Negative it
# for `down`. The post stands at the middle of the cell, so it meets the rail half a rise up.
def slope_lift(slope, fraction):
    """Rail lift at `fraction` along the cell (0 at the left edge, 1 at the right)."""
    if slope == "up":
        return SLOPE_RISE * fraction
    if slope == "down":
        return -SLOPE_RISE * fraction
    return 0.0


POST_LIFT = 0.5   # the post sits mid-cell, so it meets the rail at half the cell's rise

# --- the diagonal gap ---------------------------------------------------------------------------
# Same arithmetic as the work zone devices: a cell is one long, a diagonal step between cell
# centres is sqrt(2) long, so a forty-five degree run leaves this much at every joint and the
# right-hand end of each connected block fills it.
DIAGONAL_GAP = (2.0 ** 0.5 - 1.0) * CELL

# --- end treatments ------------------------------------------------------------------------------
# An end block sits in the cell PAST the last rail and faces into the run, so its rail must start
# exactly where the run's rail stops. These are the numbers that make that true.
END_RAIL_TOP_Y = RAIL_TOP_Y
END_RAIL_BOTTOM_Y = RAIL_BOTTOM_Y
END_RAIL_FRONT_Z = RAIL_FRONT_Z
END_RAIL_BACK_Z = RAIL_BACK_Z

# The chevron panel on the impact head, in the end block's own frame.
GLOVE_PANEL = (2.20, 13.80, 3.40, 14.60)    # x0, x1, y0, y1
GLOVE_PANEL_Z = 1.20
GLOVE_STRIPE_DEG = 45.0

# --- the thrie-beam rail --------------------------------------------------------------------------
# Three corrugations instead of two, and a great deal deeper: a real thrie beam is 813 mm against
# the W's 312, which is most of a cell. Taken at face value its bottom edge would sit on the
# ground, so it is drawn nine units deep rather than thirteen -- deep enough to read as the bigger
# rail beside a W-beam, shallow enough to still show daylight under it.
#
# The mounting height is the same as the W-beam's, because that is set by what the rail is for
# rather than by how deep it is.
THRIE_RAIL_TOP_Y = 13.00
THRIE_RAIL_BOTTOM_Y = 4.00
THRIE_RAIL_FRONT_Z = RAIL_FRONT_Z
THRIE_RAIL_BACK_Z = RAIL_BACK_Z

# Three crests and two valleys, drawn bottom to top as (z offset back from the face, y). Same
# lip-crest-valley language as the W, one corrugation longer.
_THRIE_SPAN = THRIE_RAIL_TOP_Y - THRIE_RAIL_BOTTOM_Y
THRIE_PROFILE = [
    (1.60, THRIE_RAIL_BOTTOM_Y),
    (0.30, THRIE_RAIL_BOTTOM_Y + 0.80),
    (0.00, THRIE_RAIL_BOTTOM_Y + 1.40),
    (0.30, THRIE_RAIL_BOTTOM_Y + 2.00),
    (1.45, THRIE_RAIL_BOTTOM_Y + _THRIE_SPAN * 0.32),   # first valley
    (0.30, THRIE_RAIL_BOTTOM_Y + _THRIE_SPAN * 0.42),
    (0.00, THRIE_RAIL_BOTTOM_Y + _THRIE_SPAN * 0.50),   # middle crest
    (0.30, THRIE_RAIL_BOTTOM_Y + _THRIE_SPAN * 0.58),
    (1.45, THRIE_RAIL_BOTTOM_Y + _THRIE_SPAN * 0.68),   # second valley
    (0.30, THRIE_RAIL_TOP_Y - 2.00),
    (0.00, THRIE_RAIL_TOP_Y - 1.40),
    (0.30, THRIE_RAIL_TOP_Y - 0.80),
    (1.60, THRIE_RAIL_TOP_Y),
]

# --- the box beam rail ----------------------------------------------------------------------------
# A square tube rather than a pressed section, and the one rail here whose posts carry a visible
# BASE PLATE -- it is bolted down rather than driven, which is plain in the reference and is most
# of what tells the two apart at a glance.
BOX_RAIL_TOP_Y = RAIL_TOP_Y
BOX_RAIL_BOTTOM_Y = BOX_RAIL_TOP_Y - 3.20
BOX_RAIL_FRONT_Z = RAIL_FRONT_Z
BOX_RAIL_BACK_Z = BOX_RAIL_FRONT_Z + 3.20
BOX_BASE_PLATE = (3.40, 0.60)     # half width, thickness

# --- the cable barrier -----------------------------------------------------------------------------
# Not a beam at all: tensioned cables on slim posts. The cables are thin enough that the span wire
# system's lesson applies -- a member under about a unit across reads as a scratch rather than a
# rope, so these are drawn a little fatter than scale.
CABLE_POST_HALF = 0.90
CABLE_POST_TOP_Y = 13.60
CABLE_RADIUS = 0.55
CABLE_HEIGHTS = (8.40, 10.60, 12.60)
CABLE_Z = 2.40                    # the plane the cables run in, in front of the post
CABLE_BACK_Z = 5.60               # the mirrored plane, on a double-sided run

# Each cable is held to the post by a clip. The gap the clip has to bridge is only about a sixth
# of a unit, so a plate filling it would be invisible; what reads at playing distance is the strap
# standing PROUD of the cable on the far side from the post, so that is what these describe.
CABLE_CLIP_X_HALF = 0.55          # narrower than the post, so it reads as separate hardware
CABLE_CLIP_Y_HALF = CABLE_RADIUS + 0.35
CABLE_CLIP_PROUD = 0.30           # how far past the cable's outer face the strap carries

# --- the W-to-thrie transition ---------------------------------------------------------------------
# Real runs do not butt a W-beam against a thrie beam; a transition piece carries one into the
# other. Ours does it across a single cell, W at its LEFT-hand end and thrie at its right, which is
# what lets one run carry straight through the change of rail.
TRANSITION_LEFT_PROFILE = "w_beam"
TRANSITION_RIGHT_PROFILE = "thrie_beam"

# --- registry names -------------------------------------------------------------------------------
RAIL_BLOCKS = (
    "w_beam_guardrail",
    "w_beam_guardrail_wood",
    "w_beam_guardrail_double",
    "w_beam_guardrail_wood_double",
)

THRIE_RAIL_BLOCKS = (
    "thrie_beam_guardrail",
    "thrie_beam_guardrail_wood",
    "thrie_beam_guardrail_double",
    "thrie_beam_guardrail_wood_double",
)

# Box beam and cable barrier get no wooden post: neither is ever built that way. Box beam bolts to
# a plated steel post and cable barrier to a driven or socketed one.
BOX_RAIL_BLOCKS = (
    "box_beam_guardrail",
    "box_beam_guardrail_double",
)

CABLE_BLOCKS = (
    "cable_barrier",
    "cable_barrier_double",
)

END_BLOCKS = (
    "guardrail_end_flared",
    "guardrail_end_boxing_glove",
    "guardrail_end_terminal",
    "guardrail_end_turndown",
)

# The thrie beam's flared shoe and turndown are the RAIL ITSELF folded, so they must match a rail
# two and a half times as deep. The impact head and the energy-absorbing terminal are bolt-on
# units that look much the same whichever rail they cap, and are shared.
THRIE_END_BLOCKS = (
    "guardrail_end_flared_thrie",
    "guardrail_end_turndown_thrie",
)

# Cable barrier ends in an ANCHOR -- a raked block holding the cables' tension -- rather than in
# anything that would deflect a vehicle.
CABLE_END_BLOCKS = (
    "cable_barrier_anchor",
)

# Box beam gets a BULLNOSE, which is none of the four W-beam ends and could not be: there is no
# shoe to fold and no corrugation to press out of a closed tube. The tube is turned through a half
# circle in plan, away from the roadway, and run back parallel to itself.
BOX_END_BLOCKS = (
    "guardrail_end_bullnose_box",
)

TRANSITION_BLOCK = "w_beam_thrie_transition"

# The crash cushion. Laid the way a run is -- a nose and then as many bays as the site wants --
# because a real one is twenty to thirty feet long and a single block would read as a toy beside
# the rail it terminates. The nose is always at the LEFT end: it is the impact face, and nothing
# joins to it.
CUSHION_BLOCKS = (
    "crash_cushion_nose",
    "crash_cushion_bay",
)

#: The section a cushion presents to its own kind. A bay accepts this on its left and any rail at
#: all on its right, which is what lets one cushion terminate all four rail families.
CUSHION_KIND = "crash_cushion"

#: What the nose presents at its impact face. Nothing accepts it, which is the point.
CUSHION_NOSE_KIND = "crash_cushion_nose"

# --- shared paths ---------------------------------------------------------------------------------
MODEL_SUBDIR = "trafficaccessories/shared_models"
TEXTURE_PREFIX = "csm:blocks/trafficaccessories/guardrail"
MATERIAL = "body"

# Galvanised steel, weathered timber, and the chevron panel's two colours.
GALVANISED = (176, 180, 184)
GALVANISED_DARK = (146, 150, 154)
TIMBER = (138, 112, 78)
TIMBER_DARK = (112, 90, 62)
CHEVRON_YELLOW = (238, 194, 20)
CHEVRON_BLACK = (32, 32, 34)
