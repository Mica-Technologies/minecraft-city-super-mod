# Backplate Tilt/Angle Support Progress

**Created:** 2026-03-29
**Status:** Phase 2 Complete — All backplate types (vertical, horizontal, doghouse, hawk) tested and working.

## Resume Prompt

> Backplate tilt/angle support is fully implemented and tested for all 76 backplate blocks:
> 52 vertical standard (BlockTLBorder*, Phase 1), 4 horizontal (BlockTLHBorder*, Phase 2),
> 10 doghouse (BlockTLDoghouseBorder*, Phase 2), and 10 hawk (BlockTLHawkBorder*, Phase 2).
>
> **Architecture:**
> - `AbstractBlockSignalBackplate` (base): computed TILT PropertyEnum via getActualState,
>   reads TileEntityTrafficSignalHead on same facing axis, mirrorTilt() for SOUTH reversal.
>   Used by vertical standard (52) and horizontal (4) backplates.
> - `AbstractBlockSignalBackplateFitted` (extends above): adds FITTED PropertyBool in meta
>   bit 3 (sneak-to-place toggles fitted/full model). Used by doghouse (10) and hawk (10).
>
> **Blockstate formats (critical lesson — do NOT use combined property name keys):**
> - Simple blocks (vertical, horizontal): Forge separate property blocks — `facing` uses
>   rotation, `tilt` uses model swap. These compose cleanly.
> - Fitted blocks (doghouse, hawk): Forge format with fully enumerated vanilla-style keys
>   (`"facing=X,fitted=Y,tilt=Z"`) — 60 entries per blockstate. Required because two
>   properties (fitted + tilt) both need model swap and Forge separate blocks can't compose
>   two model swaps. The `"fitted,tilt"` combined key approach does NOT work — Forge treats
>   it as a single property name.
>
> **Model structure:**
> - Pre-rotated shared models use per-element JSON rotation at ±22.5°/±45° around pivots
>   LEFT=(8.5,6,28.5) / RIGHT=(8.4,6,29.4).
> - Hawk blocks reference shared models directly in blockstate with texture overrides in
>   defaults (no per-block model files for tilt variants).
> - Doghouse blocks use per-block model files for each fitted×tilt combination.
>
> **Remaining:** 8 pre-existing angled variants (TLVA + TrafficLightLeftAngle) are deprecation
> candidates since native tilt support now covers their use case. No other backplate work needed.

## Architecture (Working)

| Component | Purpose |
|---|---|
| `AbstractBlockSignalBackplate` | Base class: TILT PropertyEnum, getActualState, mirrorTilt |
| `AbstractBlockSignalBackplateFitted` | Extends above: adds FITTED PropertyBool in meta bit 3 |
| Blockstate JSON (simple) | vanilla facing rotation + separate tilt model swap (horizontal) |
| Blockstate JSON (fitted) | fully enumerated vanilla-style keys under forge_marker (doghouse/hawk) |
| Pre-rotated shared models | Per-element `rotation` field matching existing angled models |
| Block model variants | Reference rotated shared models with same textures |

**Key implementation details:**
- TILT property: `PropertyEnum<TrafficSignalBodyTilt>` — NOT in metadata, computed in getActualState
- FITTED property: `PropertyBool` — stored in meta bit 3 (doghouse/hawk only)
- Signal detection: checks `pos.offset(dir)` for all 4 horizontal directions, filtered to same axis
- SOUTH mirror: `mirrorTilt()` swaps left↔right for SOUTH facing to match renderer reversal
- Rotation pivots: LEFT = (8.5, 6, 28.5), RIGHT = (8.4, 6, 29.4) — matching existing angled models
- Angles: LEFT_TILT=-22.5°, RIGHT_TILT=+22.5°, LEFT_ANGLE=-45°, RIGHT_ANGLE=+45°
- Fully enumerated keys: 60 entries per blockstate (6 facings × 2 fitted × 5 tilts) for doghouse/hawk
- Hawk blocks use direct shared model refs in blockstate with texture overrides (no per-block models)

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

## Phase 2: Remaining Backplate Types — COMPLETE ✓

### 2A. Horizontal Signal Borders (4 blocks) — COMPLETE ✓

| Block | Registry | Base Class | Shared Model |
|---|---|---|---|
| BlockTLHBorderBlack | tlhborderblack | AbstractBlockSignalBackplate | borderhorizontal |
| BlockTLHBorderTan | tlhbordertan | AbstractBlockSignalBackplate | borderhorizontal |
| BlockTLHBorderWhite | tlhborderwhite | AbstractBlockSignalBackplate | borderhorizontal |
| BlockTLHBorderYellow | tlhborderyellow | AbstractBlockSignalBackplate | borderhorizontal |

Simple conversion — no FITTED property. Same approach as Phase 1.

### 2B. Doghouse Signal Borders (10 blocks) — COMPLETE ✓

| Block | Registry | Base Class |
|---|---|---|
| BlockTLDoghouseBorderBlackBlack | tldoghouseborderblackblack | AbstractBlockSignalBackplateFitted |
| BlockTLDoghouseBorderBlackBlue | tldoghouseborderblackblue | AbstractBlockSignalBackplateFitted |
| BlockTLDoghouseBorderBlackPink | tldoghouseborderblackpink | AbstractBlockSignalBackplateFitted |
| BlockTLDoghouseBorderBlackWhite | tldoghouseborderblackwhite | AbstractBlockSignalBackplateFitted |
| BlockTLDoghouseBorderBlackYellow | tldoghouseborderblackyellow | AbstractBlockSignalBackplateFitted |
| BlockTLDoghouseBorderBlueBlack | tldoghouseborderblueblack | AbstractBlockSignalBackplateFitted |
| BlockTLDoghouseBorderGrayGray | tldoghousebordergraygray | AbstractBlockSignalBackplateFitted |
| BlockTLDoghouseBorderPinkBlack | tldoghouseborderpinkblack | AbstractBlockSignalBackplateFitted |
| BlockTLDoghouseBorderWhiteBlack | tldoghouseborderwhiteblack | AbstractBlockSignalBackplateFitted |
| BlockTLDoghouseBorderYellowBlack | tldoghouseborderyellowblack | AbstractBlockSignalBackplateFitted |

Has FITTED property. Uses fully enumerated vanilla-style blockstate keys (60 entries per block).
Two shared models: doghouse_full (unfitted) and doghouse_fitted.
Per-block model files for each fitted×tilt combination (80 files total).

### 2C. Hawk Signal Borders (10 blocks) — COMPLETE ✓

| Block | Registry | Base Class |
|---|---|---|
| BlockTLHawkBorderBlackBlack | tlhawkborderblackblack | AbstractBlockSignalBackplateFitted |
| BlockTLHawkBorderBlackBlue | tlhawkborderblackblue | AbstractBlockSignalBackplateFitted |
| BlockTLHawkBorderBlackPink | tlhawkborderblackpink | AbstractBlockSignalBackplateFitted |
| BlockTLHawkBorderBlackWhite | tlhawkborderblackwhite | AbstractBlockSignalBackplateFitted |
| BlockTLHawkBorderBlackYellow | tlhawkborderblackyellow | AbstractBlockSignalBackplateFitted |
| BlockTLHawkBorderBlueBlack | tlhawkborderblueblack | AbstractBlockSignalBackplateFitted |
| BlockTLHawkBorderGrayGray | tlhawkbordergraygray | AbstractBlockSignalBackplateFitted |
| BlockTLHawkBorderPinkBlack | tlhawkborderpinkblack | AbstractBlockSignalBackplateFitted |
| BlockTLHawkBorderWhiteBlack | tlhawkborderwhiteblack | AbstractBlockSignalBackplateFitted |
| BlockTLHawkBorderYellowBlack | tlhawkborderyellowblack | AbstractBlockSignalBackplateFitted |

Has FITTED property. Uses fully enumerated vanilla-style blockstate keys (60 entries per block).
Two shared models: hawk_full and hawk_fitted (in backplates/ subfolder).
References shared models directly in blockstate with texture overrides — no per-block model files for tilt variants.

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

## Phase 2 Files Created

| Type | Count | Pattern |
|---|---|---|
| New Java class | 1 | `AbstractBlockSignalBackplateFitted.java` |
| Rotated shared models | 20 | `*_{tilt}.json` across 3 directories |
| Block model tilt variants | 96 | 16 horizontal + 80 doghouse |
| Updated Java classes | 24 | 4 horizontal + 10 doghouse + 10 hawk |
| Updated blockstates | 24 | 4 horizontal + 10 doghouse + 10 hawk |

## Lessons Learned

1. **Forge blockstate format cannot compose two transforms** — use model swap for tilt, not transforms
2. **Forge format with separate property blocks**: `transform` only works for ONE property's variant;
   the other must use `model` swap or vanilla shorthand rotation
3. **Vanilla shorthand rotation** (`{"y": 180}`) and Forge transform rotation have different behavior —
   don't mix them in the same blockstate
4. **Per-element JSON rotation** supports ±45° in 22.5° increments — sufficient for tilt/angle
5. **SOUTH facing reversal** — the signal renderer reverses left/right for SOUTH, backplate must match
6. **`getActualState` works for model selection** — proven by the sign system (DOWNWARD/SETBACK properties)
7. **Combined property names DON'T work** — Forge format does NOT support comma-separated property
   names (e.g., `"fitted,tilt"`) as a combined key. Forge treats it as a single property name.
   When two properties both need model swap, use fully enumerated vanilla-style keys
   (`"facing=X,fitted=Y,tilt=Z"`) under `forge_marker: 1`.
8. **Hawk blocks use direct shared model refs** — No per-block model files needed when blockstate
   defaults include texture overrides. The `"textures"` in defaults propagate to all variant models.
9. **AbstractBlockSignalBackplateFitted** — When adding a second meta-encoded property to a base class
   that already has getActualState-computed properties, override createBlockState to include all three
   (FACING, TILT, FITTED) and handle meta encoding for FACING + FITTED only.
