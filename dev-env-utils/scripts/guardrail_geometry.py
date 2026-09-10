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

# --- registry names -------------------------------------------------------------------------------
RAIL_BLOCKS = (
    "w_beam_guardrail",
    "w_beam_guardrail_wood",
    "w_beam_guardrail_double",
    "w_beam_guardrail_wood_double",
)

END_BLOCKS = (
    "guardrail_end_flared",
    "guardrail_end_boxing_glove",
    "guardrail_end_terminal",
    "guardrail_end_turndown",
)

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
