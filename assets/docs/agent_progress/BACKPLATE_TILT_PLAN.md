# Backplate Tilt/Angle Support Progress

**Created:** 2026-03-29
**Status:** Phase 1 Complete — Vertical standard backplates working. Phase 2 planned.

## Resume Prompt

> Backplate tilt/angle support is implemented and working for all 52 vertical standard
> backplate blocks (BlockTLBorder*). The approach: AbstractBlockSignalBackplate base class
> adds a computed TILT PropertyEnum via getActualState that reads the TileEntityTrafficSignalHead
> on the same facing axis. For SOUTH-facing backplates, left/right tilt is mirrored via
> mirrorTilt() to match the signal renderer's SOUTH reversal. The blockstate JSON uses vanilla
> facing rotation + model swap for tilt variants. Pre-rotated shared models use per-element
> JSON rotation at ±22.5° (tilt) and ±45° (angle) around pivot points matching the existing
> angled backplate models. There are 32 more blocks to convert in Phase 2: 4 horizontal, 10
> doghouse, 10 hawk, 4 TLVA (pre-angled), 4 TrafficLightLeftAngle (pre-angled). The TLVA and
> LeftAngle blocks are candidates for deprecation since native tilt support now covers their
> use case. New shared models may be needed for doghouse and hawk shapes. See the full plan
> at assets/docs/agent_progress/BACKPLATE_TILT_PLAN.md for the complete block inventory and
> Phase 2 attack plan.

## Architecture (Working)

| Component | Purpose |
|---|---|
| `AbstractBlockSignalBackplate` | Base class: TILT PropertyEnum, getActualState, mirrorTilt |
| Blockstate JSON | vanilla facing rotation + model swap for tilt variants |
| Pre-rotated shared models | Per-element `rotation` field matching existing angled models |
| Block model variants | Reference rotated shared models with same textures |

**Key implementation details:**
- TILT property: `PropertyEnum<TrafficSignalBodyTilt>` — NOT in metadata, computed in getActualState
- Signal detection: checks `pos.offset(dir)` for all 4 horizontal directions, filtered to same axis
- SOUTH mirror: `mirrorTilt()` swaps left↔right for SOUTH facing to match renderer reversal
- Rotation pivots: LEFT = (8.5, 6, 28.5), RIGHT = (8.4, 6, 29.4) — matching existing angled models
- Angles: LEFT_TILT=-22.5°, RIGHT_TILT=+22.5°, LEFT_ANGLE=-45°, RIGHT_ANGLE=+45°

## Phase 1: Vertical Standard Backplates — COMPLETE ✓

**52 blocks converted**, all tested and working for all 4 horizontal facings.

### Converted Blocks

| Category | Count | Registry Pattern | Shared Model |
|---|---|---|---|
| Standard 3-section | 10 | `tlborder[color]` | `signal_backplate_vertical_3` |
| 8-inch | 6 | `tlborder[color]8inch` | `signal_backplate_888_vertical_3` |
| 8-8-12 inch | 6 | `tlborder[color]8812inch` | `signal_backplate_8812_vertical_3` |
| Single-section | 10 | `tlbordersingle[color]` | `signal_backplate_vertical_1` |
| 3-section add-on | 10 | `tlborderaddon[color]` | `signal_backplate_vertical_addon_1` |
| 5-section add-on | 10 | `tlborder5addon[color]` | `signal_backplate_vertical_addon_2` |

### Files Created

| Type | Count | Pattern |
|---|---|---|
| Rotated shared models | 24 | `signal_backplate_*_{tilt}.json` |
| Block model variants | 208 | `tlborder*_{tilt}.json` |
| Updated blockstates | 52 | `tlborder*.json` |
| Updated Java classes | 52 | `BlockTLBorder*.java` |

## Phase 2: Remaining Backplate Types — PLANNED

### 2A. Horizontal Signal Borders (4 blocks)

| Block | Registry | Current Base | Shared Model |
|---|---|---|---|
| BlockTLHBorderBlack | tlhborderblack | AbstractBlockRotatableNSEWUD | TBD |
| BlockTLHBorderTan | tlhbordertan | AbstractBlockRotatableNSEWUD | TBD |
| BlockTLHBorderWhite | tlhborderwhite | AbstractBlockRotatableNSEWUD | TBD |
| BlockTLHBorderYellow | tlhborderyellow | AbstractBlockRotatableNSEWUD | TBD |

**Notes:** Horizontal signals may need different tilt behavior since the signal body
is oriented differently. Need to check shared model geometry.

### 2B. Doghouse Signal Borders (10 blocks)

| Block | Registry | Current Base |
|---|---|---|
| BlockTLDoghouseBorderBlackBlack | tldoghouseborderblackblack | AbstractBlockRotatableNSEWUD |
| BlockTLDoghouseBorderBlackBlue | tldoghouseborderblackblue | AbstractBlockRotatableNSEWUD |
| BlockTLDoghouseBorderBlackPink | tldoghouseborderblackpink | AbstractBlockRotatableNSEWUD |
| BlockTLDoghouseBorderBlackWhite | tldoghouseborderblackwhite | AbstractBlockRotatableNSEWUD |
| BlockTLDoghouseBorderBlackYellow | tldoghouseborderblackyellow | AbstractBlockRotatableNSEWUD |
| BlockTLDoghouseBorderBlueBlack | tldoghouseborderblueblack | AbstractBlockRotatableNSEWUD |
| BlockTLDoghouseBorderGrayGray | tldoghouseborderlargegray | AbstractBlockRotatableNSEWUD |
| BlockTLDoghouseBorderPinkBlack | tldoghouseborderpinkblack | AbstractBlockRotatableNSEWUD |
| BlockTLDoghouseBorderWhiteBlack | tldoghouseborderwhiteblack | AbstractBlockRotatableNSEWUD |
| BlockTLDoghouseBorderYellowBlack | tldoghouseborderyellowblack | AbstractBlockRotatableNSEWUD |

**Notes:** Doghouse models have a different shape (wider for the doghouse layout). Need to
identify the shared models and create rotated variants with correct pivot points.

### 2C. Hawk Signal Borders (10 blocks)

| Block | Registry | Current Base |
|---|---|---|
| BlockTLHawkBorderBlackBlack | tlhawkborderblackblack | AbstractBlockRotatableNSEWUD |
| BlockTLHawkBorderBlackBlue | tlhawkborderblackblue | AbstractBlockRotatableNSEWUD |
| BlockTLHawkBorderBlackPink | tlhawkborderblackpink | AbstractBlockRotatableNSEWUD |
| BlockTLHawkBorderBlackWhite | tlhawkborderblackwhite | AbstractBlockRotatableNSEWUD |
| BlockTLHawkBorderBlackYellow | tlhawkborderblackyellow | AbstractBlockRotatableNSEWUD |
| BlockTLHawkBorderBlueBlack | tlhawkborderblueblack | AbstractBlockRotatableNSEWUD |
| BlockTLHawkBorderGrayGray | tlhawkborderlargegray | AbstractBlockRotatableNSEWUD |
| BlockTLHawkBorderPinkBlack | tlhawkborderpinkblack | AbstractBlockRotatableNSEWUD |
| BlockTLHawkBorderWhiteBlack | tlhawkborderwhiteblack | AbstractBlockRotatableNSEWUD |
| BlockTLHawkBorderYellowBlack | tlhawkborderyellowblack | AbstractBlockRotatableNSEWUD |

**Notes:** HAWK signal borders may have unique geometry. Need to check shared models.

### 2D. Pre-Existing Angled Variants (8 blocks) — Deprecation Candidates

| Block | Registry | Angle | Replacement |
|---|---|---|---|
| BlockTLVABorderBlack | tlvaborderblack | Right angle (-45°) | Native tilt on standard backplate |
| BlockTLVABorderBlackWhite | tlvaborderblackwhite | Right angle (-45°) | Native tilt on standard backplate |
| BlockTLVABorderBlackYellow | tlvaborderblackyellow | Right angle (-45°) | Native tilt on standard backplate |
| BlockTLVABorderTan | tlvabordertan | Right angle (-45°) | Native tilt on standard backplate |
| BlockTrafficLightLeftAngleBorderBlack | trafficlightleftangleborderblack | Left angle (+45°) | Native tilt on standard backplate |
| BlockTrafficLightLeftAngleBorderTan | trafficlightleftanglebordertan | Left angle (+45°) | Native tilt on standard backplate |
| BlockTrafficLightLeftAngleBorderWhiteBlack | trafficlightleftangleborderwhiteblack | Left angle (+45°) | Native tilt on standard backplate |
| BlockTrafficLightLeftAngleBorderYellowBlack | trafficlightleftangleborderyellowblack | Left angle (+45°) | Native tilt on standard backplate |

**Notes:** These blocks are now functionally replaced by the native tilt support on
standard vertical backplates. They can be deprecated but should remain for backward
compatibility with existing worlds. No conversion needed.

## Phase 2 Attack Plan

### Step 1: Identify shared models for remaining block types
For each unconverted category (horizontal, doghouse, hawk), find the shared models
they reference and determine if they need new pre-rotated variants.

### Step 2: Create pre-rotated shared models
Generate rotated variants for each new shared model shape using the same per-element
rotation technique (with appropriate pivot points for each shape).

### Step 3: Create block model variants
For each unconverted block, create 4 tilt model variants referencing the rotated shared models.

### Step 4: Convert Java classes
Change `extends AbstractBlockRotatableNSEWUD` to `extends AbstractBlockSignalBackplate`
for all 24 blocks (horizontal + doghouse + hawk). The pre-existing angled variants (8 blocks)
do NOT need conversion.

### Step 5: Update blockstate JSONs
Add tilt model swap variants to each blockstate file.

### Step 6: Test
Test each category with all 4 facings and all tilt values.

## Lessons Learned

1. **Forge blockstate format cannot compose two transforms** — use model swap for tilt, not transforms
2. **Forge format with separate property blocks**: `transform` only works for ONE property's variant;
   the other must use `model` swap or vanilla shorthand rotation
3. **Vanilla shorthand rotation** (`{"y": 180}`) and Forge transform rotation have different behavior —
   don't mix them in the same blockstate
4. **Per-element JSON rotation** supports ±45° in 22.5° increments — sufficient for tilt/angle
5. **SOUTH facing reversal** — the signal renderer reverses left/right for SOUTH, backplate must match
6. **`getActualState` works for model selection** — proven by the sign system (DOWNWARD/SETBACK properties)
