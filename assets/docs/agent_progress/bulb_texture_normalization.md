# Bulb Texture Radius Normalization

**Date:** 2026-04-09
**Status:** Complete

## Problem

The 55 circular bulb textures used in the traffic signal atlas (`lights/atlas.png`) had
inconsistent circle radii and messy edges:

- **Radius variance:** p99 effective radius ranged from 55.6 to 59.7 pixels across textures
- **Stray pixels:** Some textures (WLED, eLED off, bike LED) had stray pixels extending
  well beyond the circle edge (up to 82.7px from center on a 128x128 image)
- **Messy edges:** Incandescent and some other textures had irregular/noisy edge pixels

This caused visible body-color bleed between the visor interior and bulb edges in-game.

## Solution

Created a Python script (`dev-env-utils/scripts/normalize_bulb_radius.py`) that processes
all 55 bulb textures to a consistent target radius with clean edges.

### Script Details

**Algorithm per texture:**
1. Load 128x128 RGBA PNG
2. Measure effective circle radius (p99 of non-transparent pixel distances from center 63.5, 63.5)
3. Scale image content by `target_radius / current_radius` using bicubic interpolation, centered on image center
4. Apply circular alpha mask with 1px anti-aliased feathering:
   - Distance <= (target - 1): full original alpha
   - Distance (target - 1) to (target + 1): feathered alpha (anti-alias)
   - Distance > (target + 1): alpha = 0 (transparent)
5. Save processed PNG

**Parameters:**
- `--target-radius N` (default: 61.0) — target circle radius in pixels
- `--dry-run` — measure and report without modifying files

**Target radius rationale:** GE GTX textures had the largest, cleanest circles at ~60.6px max
radius. Target of 61.0px is slightly larger as requested.

### Usage

```bash
# Dry run (measure only)
python dev-env-utils/scripts/normalize_bulb_radius.py --dry-run

# Process all textures
python dev-env-utils/scripts/normalize_bulb_radius.py

# Custom radius
python dev-env-utils/scripts/normalize_bulb_radius.py --target-radius 62
```

After running, regenerate the atlas using ImageTilerTool (IntelliJ run config "Generate Signal Light Atlas").

## Measured Before/After

| Texture Group | Before (p99 radius) | After (p99 radius) | Scale Factor |
|---------------|---------------------|---------------------|--------------|
| GTX (reference) | 56.4 - 59.7 | ~61.0 | 1.02 - 1.08 |
| iLED | 58.3 - 59.0 | ~61.0 | 1.03 - 1.05 |
| WLED | 58.2 (max 71.6 stray) | ~61.0 | 1.05 |
| Incandescent | 56.1 - 58.8 | ~61.0 | 1.04 - 1.09 |
| eLED | 58.2 - 58.8 (max 70.6 stray) | ~61.0 | 1.04 - 1.05 |
| Bike LED | 58.1 - 58.2 (max 82.7 stray) | ~61.0 | 1.05 |
| Arrow/U-turn | 58.3 - 59.2 | ~61.0 | 1.03 - 1.05 |
| Inca Arrow | 55.6 - 58.5 | ~61.0 | 1.04 - 1.10 |

## Files Modified

- All 55 PNG textures in `src/main/resources/assets/csm/textures/blocks/trafficsignals/lights/`
- `src/main/resources/assets/csm/textures/blocks/trafficsignals/lights/atlas.png` (regenerated)

## Files Created

- `dev-env-utils/scripts/normalize_bulb_radius.py` — the processing script
- `assets/docs/agent_progress/bulb_texture_normalization.md` — this document
