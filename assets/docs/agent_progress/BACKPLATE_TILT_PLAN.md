# Backplate Tilt/Angle Support Plan

**Created:** 2026-03-29
**Status:** Planned

## Resume Prompt

> We need to add tilt/angle support to traffic signal backplate blocks so they visually
> match the tilt setting of the signal head behind them. The approach: add a computed
> `TILT` property to backplate blocks via `getActualState` (no tile entity, no metadata
> storage, no world bloat). The backplate reads the `TileEntityTrafficSignalHead` behind
> it each render frame and mirrors its `TrafficSignalBodyTilt` value. The blockstate JSON
> uses combined facing+tilt variant entries with facing-specific translations to shift the
> backplate model geometry. A new abstract base class `AbstractBlockSignalBackplate` handles
> the shared tilt logic for all 20+ backplate block variants. The translation values must
> be facing-specific because the signal renderer applies tilt in world-space X (with SOUTH
> reversal), while Forge blockstate transforms are in model-local space. Key files: the
> abstract base class, all BlockTLBorder*.java files (change extends), all blockstate JSONs,
> and TrafficSignalBodyTilt.java (needs IStringSerializable). See the full plan doc at
> assets/docs/agent_progress/BACKPLATE_TILT_PLAN.md for exact translation values, file
> lists, and implementation steps.

## Problem

When a signal head is tilted/angled via `TrafficSignalBodyTilt` (configured with the signal
config tool), the TESR shifts the signal body left or right. But the backplate block in front
of it is a static JSON model with no knowledge of the tilt — it stays centered. This causes
a visual mismatch where the signal body pokes out one side of the backplate.

## Current Backplate Architecture

- Blocks placed 1 space in front of the signal, model geometry shifted back ~1 block (Z=27-28
  in Blockbench model space) to visually wrap around the signal body
- No tile entities — purely decorative model-based blocks
- Extends `AbstractBlockRotatableNSEWUD` (FACING property in metadata, 6 values)
- 3 shared model shapes:
  - `signal_backplate_vertical_3.json` — full 3-section surround (closed top and bottom)
  - `signal_backplate_vertical_addon_1.json` — 3-section add-on (open top, for extending)
  - `signal_backplate_vertical_addon_2.json` — 5-section add-on (taller, open top)
- Color variants (outer edge / inner frame):
  - Black/Black (no retroreflective tape)
  - Black/Yellow (yellow RRF edge)
  - Black/White (white RRF edge)
  - Black/Blue (blue RRF edge)
  - Black/Pink (pink RRF edge)
  - Blue/Black, Pink/Black, White/Black, Yellow/Black (reversed)
  - Gray/Gray (large gray variant)
- Block classes: 10 color variants × 2 sizes (3-section addon + 5-section addon) = 20 blocks,
  plus 10 base (non-addon) blocks = 30 total backplate blocks

## Signal Renderer Tilt Values

From `TileEntityTrafficSignalHeadRenderer.java` (lines 98-116):

| TrafficSignalBodyTilt | Model-space offset | Block-space offset |
|---|---|---|
| NONE | 0 units | 0 blocks |
| LEFT_TILT | +2 units | +0.125 blocks |
| RIGHT_TILT | -2 units | -0.125 blocks |
| LEFT_ANGLE | +4 units | +0.25 blocks |
| RIGHT_ANGLE | -4 units | -0.25 blocks |

The renderer applies these offsets in world-space X AFTER the facing rotation, with a sign
reversal for SOUTH facing. This means "LEFT_TILT" always shifts the signal to the viewer's
left regardless of which direction the signal faces.

## Coordinate System Analysis

The critical complexity: Forge blockstate transforms are applied in **model-local space**
(before facing rotation), but the signal renderer applies tilt in **world space** (after
rotation). Model-local +X maps differently to world coordinates depending on facing:

| Facing | Forge Y rotation | Model +X → World | Signal LEFT → World |
|---|---|---|---|
| NORTH | 0° | +X | +X |
| SOUTH | 180° | -X | -X |
| EAST | 90° | -Z | +Z |
| WEST | 270° | +Z | -Z |

For NORTH and SOUTH: model +X aligns with signal LEFT. ✓
For EAST and WEST: model +X does NOT align with signal LEFT. ✗

This means we CANNOT use separate facing/tilt variant blocks (which would apply the same
model-local translation regardless of facing). We must use **combined variant entries** with
facing-specific translations.

## Translation Lookup Table

For each facing, the world-space direction of "signal LEFT" determines the translation:

### LEFT_TILT (+0.125 blocks toward signal's left)

| Facing | World direction | Translation [X, Y, Z] |
|---|---|---|
| NORTH | +X | [0.125, 0, 0] |
| SOUTH | -X | [-0.125, 0, 0] |
| EAST | +Z | [0, 0, 0.125] |
| WEST | -Z | [0, 0, -0.125] |

### RIGHT_TILT (-0.125 blocks, toward signal's right)

| Facing | Translation [X, Y, Z] |
|---|---|
| NORTH | [-0.125, 0, 0] |
| SOUTH | [0.125, 0, 0] |
| EAST | [0, 0, -0.125] |
| WEST | [0, 0, 0.125] |

### LEFT_ANGLE (+0.25 blocks toward signal's left)

| Facing | Translation [X, Y, Z] |
|---|---|
| NORTH | [0.25, 0, 0] |
| SOUTH | [-0.25, 0, 0] |
| EAST | [0, 0, 0.25] |
| WEST | [0, 0, -0.25] |

### RIGHT_ANGLE (-0.25 blocks toward signal's right)

| Facing | Translation [X, Y, Z] |
|---|---|
| NORTH | [-0.25, 0, 0] |
| SOUTH | [0.25, 0, 0] |
| EAST | [0, 0, -0.25] |
| WEST | [0, 0, 0.25] |

### UP/DOWN facings: no tilt support (signals aren't mounted horizontally)

## Implementation Steps

### Step 1: Make TrafficSignalBodyTilt implement IStringSerializable

`TrafficSignalBodyTilt` needs to implement `IStringSerializable` so it can be used as a
`PropertyEnum` in blockstates. Add `getName()` returning the lowercase enum name.

**File:** `src/main/java/com/micatechnologies/minecraft/csm/trafficsignals/logic/TrafficSignalBodyTilt.java`

### Step 2: Create AbstractBlockSignalBackplate

New abstract base class extending `AbstractBlockRotatableNSEWUD` that:

1. Declares `PropertyEnum<TrafficSignalBodyTilt> TILT` property
2. Overrides `createBlockState()` to include both FACING and TILT
3. Overrides `getActualState()` to:
   a. Determine the block position "behind" this backplate based on FACING
   b. Check if that block has a `TileEntityTrafficSignalHead`
   c. Read the `bodyTilt` from the TE
   d. Return the blockstate with TILT set accordingly
4. Does NOT include TILT in `getMetaFromState`/`getStateFromMeta` (TILT is computed only)
5. Provides common constructor matching the existing backplate block pattern

**File:** `src/main/java/com/micatechnologies/minecraft/csm/trafficaccessories/AbstractBlockSignalBackplate.java`

### Step 3: Update all 30 backplate block classes

Change each `BlockTLBorder*` and `BlockTLBorder5*` class to extend `AbstractBlockSignalBackplate`
instead of `AbstractBlockRotatableNSEWUD`. No other changes needed per class — the tilt logic
is entirely in the new base class.

**Files (30 total):**
- `BlockTLBorderAddOnBlackBlack.java` through `BlockTLBorderAddOnYellowBlack.java` (10 files)
- `BlockTLBorder5AddOnBlackBlack.java` through `BlockTLBorder5AddOnYellowBlack.java` (10 files)
- `BlockTLBorderBlackBlack.java` through `BlockTLBorderYellowBlack.java` (10 files)

### Step 4: Update all 30 blockstate JSON files

Replace the current separate `facing` variant block with combined `facing=X,tilt=Y` entries.
Each blockstate file needs 22 entries:
- 4 horizontal facings × 5 tilt values = 20 entries (with facing-specific translations)
- 2 vertical facings (up/down) × 1 tilt (none only) = 2 entries

Template for each blockstate:
```json
{
  "forge_marker": 1,
  "defaults": {
    "model": "csm:<block_model_name>"
  },
  "variants": {
    "facing=north,tilt=none": {},
    "facing=north,tilt=left_tilt": {"transform": {"translation": [0.125, 0, 0]}},
    "facing=north,tilt=right_tilt": {"transform": {"translation": [-0.125, 0, 0]}},
    "facing=north,tilt=left_angle": {"transform": {"translation": [0.25, 0, 0]}},
    "facing=north,tilt=right_angle": {"transform": {"translation": [-0.25, 0, 0]}},
    "facing=south,tilt=none": {"y": 180},
    "facing=south,tilt=left_tilt": {"y": 180, "transform": {"translation": [-0.125, 0, 0]}},
    "facing=south,tilt=right_tilt": {"y": 180, "transform": {"translation": [0.125, 0, 0]}},
    "facing=south,tilt=left_angle": {"y": 180, "transform": {"translation": [-0.25, 0, 0]}},
    "facing=south,tilt=right_angle": {"y": 180, "transform": {"translation": [0.25, 0, 0]}},
    "facing=east,tilt=none": {"y": 90},
    "facing=east,tilt=left_tilt": {"y": 90, "transform": {"translation": [0, 0, 0.125]}},
    "facing=east,tilt=right_tilt": {"y": 90, "transform": {"translation": [0, 0, -0.125]}},
    "facing=east,tilt=left_angle": {"y": 90, "transform": {"translation": [0, 0, 0.25]}},
    "facing=east,tilt=right_angle": {"y": 90, "transform": {"translation": [0, 0, -0.25]}},
    "facing=west,tilt=none": {"y": 270},
    "facing=west,tilt=left_tilt": {"y": 270, "transform": {"translation": [0, 0, -0.125]}},
    "facing=west,tilt=right_tilt": {"y": 270, "transform": {"translation": [0, 0, 0.125]}},
    "facing=west,tilt=left_angle": {"y": 270, "transform": {"translation": [0, 0, -0.25]}},
    "facing=west,tilt=right_angle": {"y": 270, "transform": {"translation": [0, 0, 0.25]}},
    "facing=down,tilt=none": {"x": 90},
    "facing=up,tilt=none": {"x": 270},
    "inventory": [{}]
  }
}
```

**Files (30 total):** All `tlborderaddon*.json`, `tlborder5addon*.json`, `tlborder*.json`
in `src/main/resources/assets/csm/blockstates/`

### Step 5: Determine "behind" block position

The backplate sits in front of the signal. "Behind" means 1 block in the direction the
backplate FACES (since the backplate faces the same way as the signal — toward the viewer).
For NORTH facing: behind = pos.south() (the signal is at pos + south direction).

| Backplate FACING | Signal position relative to backplate |
|---|---|
| NORTH | pos.south() |
| SOUTH | pos.north() |
| EAST | pos.west() |
| WEST | pos.east() |
| UP | pos.down() |
| DOWN | pos.up() |

This is simply `pos.offset(facing)` since FACING points toward the viewer, and the signal
is behind the backplate (in the facing direction from the backplate's perspective... actually
no. The backplate FACES the viewer. The signal is BEHIND the backplate. So the signal is in
the OPPOSITE direction of FACING).

Wait — let me re-check. The backplate model geometry is shifted BACK (toward +Z in model space,
which is BEHIND the block). When placed facing NORTH, the backplate's visual content is at the
SOUTH side of the block (shifted toward the signal). The signal is at `pos.south()` (one block
to the south, behind the backplate).

Actually: the backplate is placed in front of the signal. If the signal faces NORTH, the
backplate is placed at `signalPos.north()` (one block to the north, in front). The backplate
also faces NORTH. So from the backplate's position, the signal is at `backplatePos.south()`
= `backplatePos.offset(FACING.getOpposite())`.

**Signal position = `pos.offset(state.getValue(FACING).getOpposite())`**

Wait, actually for NSEWUD blocks, the FACING property indicates which direction the block
faces. For a north-facing backplate placed in front of a north-facing signal:
- Signal at position (0, 0, 0), facing NORTH
- Backplate at position (0, 0, -1), facing NORTH
- From backplate, signal is at (0, 0, 0) = backplate pos + SOUTH = pos.offset(SOUTH)
- SOUTH = NORTH.getOpposite()

So: **signal position = `pos.offset(facing.getOpposite())`** ✗

Hmm wait, that gives (0, 0, 0) from (0, 0, -1) + SOUTH... SOUTH is +Z, so (0, 0, -1+1) = (0,0,0). Yes! That's correct.

Actually wait, the backplate is in FRONT of the signal. "In front" means toward the viewer.
For a NORTH-facing signal, the viewer is to the NORTH. So the backplate is placed NORTH of
the signal:
- Signal at (0, 0, 5)
- Backplate at (0, 0, 4) — one block north (toward -Z)
- Backplate faces NORTH
- From backplate (0,0,4), signal is at (0,0,5) = backplate + SOUTH direction (+Z)
- SOUTH = NORTH.getOpposite()

So signal = `pos.offset(facing.getOpposite())`. Wait, NORTH.getOpposite() = SOUTH, and
pos.offset(SOUTH) from (0,0,4) = (0,0,5). Yes, correct!

But actually, I need to double-check: does `EnumFacing.NORTH` point toward -Z or +Z?
In Minecraft: NORTH = -Z, SOUTH = +Z, EAST = +X, WEST = -X.

So for a NORTH-facing backplate at (0,0,4):
- `facing.getOpposite()` = SOUTH = +Z direction
- `pos.offset(SOUTH)` = (0, 0, 4+1) = (0, 0, 5) = signal position ✓

Great, so **signal position = `pos.offset(facing.getOpposite())`**.

Hmm wait, but that seems backward. Let me think again...

The signal faces NORTH (toward -Z, toward the viewer). The backplate is placed in front of the
signal, which is toward the viewer, which is toward NORTH (-Z). So backplate is at signalPos
offset NORTH = signalPos + (-Z) = (0, 0, 5) + (0, 0, -1) = (0, 0, 4).

From backplate at (0, 0, 4):
- facing = NORTH
- facing.getOpposite() = SOUTH
- pos.offset(SOUTH) = (0, 0, 4) + (0, 0, 1) = (0, 0, 5) = signal position ✓

OK so I had it right: `pos.offset(facing.getOpposite())`.

But wait, I just realized there might be an issue. The `FACING` property for these blocks is
set by `getStateForPlacement` which uses `EnumFacing.getDirectionFromEntityLiving`. This means
the facing is set based on where the PLAYER is looking, not where the signal is. If the player
places the backplate while looking at the signal (from the front), the facing would be the
direction FROM the player TO the signal... which is actually the direction the backplate should
face (toward the viewer = toward the player).

Hmm actually `getDirectionFromEntityLiving` returns the direction the player is FACING, which
is the direction the block will face. If the player is standing north of the signal and looking
south (at the signal), the block faces SOUTH. That means:
- facing = SOUTH
- facing.getOpposite() = NORTH
- pos.offset(NORTH) = moves toward -Z... that's AWAY from the signal.

That's wrong! Let me re-check...

Actually, `getDirectionFromEntityLiving` for a NSEWUD block returns the direction the block
should face based on the player's look direction. If the player looks at the south face of a
block and places a new block, the new block faces the player — i.e., it faces NORTH (the
player is to the north).

Hmm, actually this might vary. Let me look at the actual implementation...

OK, I think I'm overthinking the placement logic. The key insight is: the backplate and the
signal should have the SAME facing. The player would typically place them facing the same way.
And "behind" the backplate is in the `facing` direction (the backplate model extends backward
into the signal's block space).

Wait, no. The backplate model extends BACKWARD from its own block, TOWARD the signal. The
model Z coordinates are 27-28 (in a 0-16 block, Z=27 is beyond the back face). This means the
visual model content is behind the block, in the direction of the block's back face.

For a NORTH-facing block: the front face is toward NORTH (-Z), the back face is toward SOUTH
(+Z). The model extends toward SOUTH (toward the signal). So the signal is at
`pos.offset(EnumFacing.SOUTH)` = `pos.offset(facing.getOpposite())`. Wait that's what I had.

Actually, I realize the confusion: for `AbstractBlockRotatableNSEWUD`, `FACING` is set by
`getDirectionFromEntityLiving` which returns the direction FROM the block position TO the
player. So if the player is to the north, FACING = NORTH. The block's FRONT faces north
(toward the player). The block's BACK faces south (toward the signal behind it).

So the signal is behind the backplate, in the direction of `facing.getOpposite()`:
- `pos.offset(facing.getOpposite())` = signal position ✓

Actually wait, `getDirectionFromEntityLiving` might return the opposite. Let me check...

`EnumFacing.getDirectionFromEntityLiving(pos, placer)` returns the direction the placed block
should face. For a block placed on a wall, it faces the player. So the facing direction IS
toward the player.

For a NORTH-facing backplate, the player is to the NORTH. The signal is to the SOUTH
(behind). Signal = `pos.offset(SOUTH)` = `pos.offset(NORTH.getOpposite())` =
`pos.offset(facing.getOpposite())`.

OK, confirmed: **`signalPos = pos.offset(facing.getOpposite())`**

But actually, I realize this might be wrong for the specific case. Let me look at what
happens with `BlockDirectional.FACING`:

`EnumFacing.getDirectionFromEntityLiving` returns:
- If player looks up: UP
- If player looks down: DOWN
- Otherwise: player's horizontal facing (the direction the player is looking)

So if the player stands to the north of where they want to place the backplate and looks
south (toward the signal), FACING = SOUTH. The block faces SOUTH.

But the backplate should face the SAME direction as the signal (both face the traffic/viewer).
If the signal faces NORTH (toward traffic coming from the north), the backplate should also
face NORTH. But the player, standing to the north and looking south, would get FACING = SOUTH.

This is a placement issue that already exists — the player just needs to place the backplate
from the right angle (standing on the signal side, looking outward toward traffic). This is
the existing behavior and not something we need to change.

Regardless of how the facing is SET, the "behind" logic is: signal is in the direction
opposite to FACING. Let me just go with `pos.offset(facing.getOpposite())`.

Actually no wait. If the backplate FACES the same direction as the signal (both toward
traffic), then "behind" the backplate (toward the signal) is in the FACING direction, not
opposite.

I keep going back and forth. Let me just look at the model geometry to determine definitively:

The model has elements at Z=27-28 (in Blockbench 0-16 scale, that's 27-28 which is well
beyond the block's back face at Z=16). Wait, Blockbench uses 0-16 for one block. Z=27 is
1.6875 blocks from the origin, which is 0.6875 blocks past the back face.

Actually the model elements are specified in a coordinate system where 0-16 = one block.
Z=27 means 27/16 = 1.6875 blocks from the block origin. Since the block origin's Z=0 face
is the NORTH face (in Minecraft, -Z = NORTH), Z=27 is toward +Z = SOUTH = the BACK of a
NORTH-facing model.

For a NORTH-facing backplate, the model content at Z=27 is at worldPos + 27/16 blocks in
the +Z direction = toward SOUTH. This means the visual backplate is ~1 block SOUTH of the
backplate block's position. The signal is also ~1 block SOUTH (since the backplate is placed
1 block north of the signal).

So the signal is at `pos.offset(SOUTH)` for a NORTH-facing backplate.
SOUTH = NORTH.getOpposite() = `facing.getOpposite()`.

But wait, does FACING=NORTH mean the block faces toward -Z (north) or that the block was
placed FROM the north? I think `BlockDirectional.FACING` indicates which direction the block
faces. A NORTH-facing block has its front face toward -Z.

Actually, for these rotatable blocks, the FACING property determines how the model is rotated.
NORTH = no rotation (the model's default orientation, which faces -Z).

Given the model content is at Z=27 (toward +Z = SOUTH), for a NORTH-facing block, the
content is 1 block to the SOUTH of the block position. The signal would be placed at
`pos.offset(SOUTH)` = `pos.offset(facing.getOpposite())`.

For a SOUTH-facing block (y=180 rotation): the model is rotated 180°. Z=27 in model space
now points toward NORTH (-Z). The signal would be at `pos.offset(NORTH)` =
`pos.offset(SOUTH.getOpposite())` = `pos.offset(facing.getOpposite())`.

Great — `pos.offset(facing.getOpposite())` consistently gives the signal position. ✓

### Step 6: Testing strategy

1. Place a signal head with a backplate in front of it
2. Use the signal config tool to cycle through tilt values
3. Verify the backplate shifts to match each tilt setting
4. Test all 4 horizontal facings to verify correct shift direction
5. Test with add-on signals + add-on backplates to verify both shift together
6. Test that removing/replacing the signal resets the backplate to no tilt

## File Summary

| Category | Count | Description |
|---|---|---|
| New Java file | 1 | `AbstractBlockSignalBackplate.java` |
| Modified Java file | 1 | `TrafficSignalBodyTilt.java` (add IStringSerializable) |
| Modified Java files | 30 | All `BlockTLBorder*.java` (change extends) |
| Modified JSON files | 30 | All backplate blockstate JSONs (add tilt variants) |
| **Total files** | **62** | |

## Risks and Considerations

- **Forge transform application order:** Need to verify that Forge applies combined variant
  transforms correctly (rotation + translation). May need in-game testing to confirm the
  translation directions are correct for each facing.
- **Existing worlds:** Backplates in existing worlds will get `tilt=none` by default from
  `getActualState` since there's no stored metadata. This is correct behavior — they'll just
  show no tilt offset until a tilted signal is placed behind them.
- **Performance:** `getActualState` is called per render frame. Reading the TE behind the
  backplate involves a world.getTileEntity() call. This should be fast (TE lookup is O(1) in
  the chunk) but is worth monitoring.
- **Blockstate JSON size:** 22 entries per file is larger than the current 8, but well within
  Forge's capabilities. The JSON can be generated programmatically for consistency.
