# Custom Signal Rendering — Progress & Plan

Custom TESR-based rendering for traffic signal heads, enabling runtime customization of
body color, visor type, bulb style, and tilt without needing separate block variants.

**Created:** 2026-03-28
**Branch:** `dev/signal-custom-rendering-rebase` (rebased from `dev/signal-custom-rendering-work-archived`)
**Status:** In Progress — Rebase complete, display list bug fixed, needs in-game testing

---

## Resume Prompt

> I'm continuing custom signal rendering work. The progress document is at
> `assets/docs/agent_progress/CUSTOM_SIGNAL_RENDERING_PLAN.md`.
>
> **Branch:** `dev/signal-custom-rendering-rebase` — this is a NEW branch created from
> `dev/signal-custom-rendering-work-archived` and rebased onto the latest `main`. The
> original archived branch is preserved for reference. Do NOT push — the user will push
> when ready.
>
> **What's been done:**
> - Rebased 8 custom rendering commits onto latest main (which has all the Forge blockstate
>   and shared_models work from the big cleanup session)
> - 4 rebase conflicts resolved (blockstate format + model locations)
> - Build passes on the rebased branch
> - **The core state-sharing bug has been FIXED.** The bug was: changing one signal head's
>   appearance (body color, visor, etc.) would affect ALL other signal heads. Root cause:
>   `TileEntityTrafficSignalHeadRenderer` had a single `staticDisplayList` field, but
>   Minecraft uses ONE renderer instance for ALL tile entities of the same class. Fix:
>   replaced with `HashMap<BlockPos, Integer> displayListCache` keyed by tile entity position.
>   The fix is committed but NOT yet tested in-game.
>
> **What needs to happen next:**
> 1. **In-game testing** — Place multiple signal heads, configure each differently (body
>    color, visor type, etc.), verify they maintain independent state
> 2. **If the fix works** — Clean up the 8 dev commits into logical commits (the user is OK
>    with rewriting history on this unmerged branch)
> 3. **If additional issues found** — Debug further. Check `TileEntityTrafficSignalHead`
>    NBT read/write, check `getSectionInfos()` for potential reference aliasing
> 4. **Expand** — Once working, apply custom rendering to more signal block types beyond the
>    current 2 (VerticalSolidSignal, VerticalBikeSignal)
>
> **Key source files (all in `src/main/java/com/micatechnologies/minecraft/csm/`):**
>
> Rendering system:
> - `trafficsignals/TileEntityTrafficSignalHead.java` — Tile entity storing per-signal state
>   (section infos array, body tilt, dirty flag). Serializes to NBT. The `getSectionInfos(int
>   currentBulbColor)` method returns the section info array with bulb colors updated based on
>   the current signal color state (0=red, 1=yellow, 2=green, 3=off).
> - `trafficsignals/TileEntityTrafficSignalHeadRenderer.java` — TESR (TileEntitySpecialRenderer).
>   Renders signal heads using OpenGL display lists for static geometry (body, visors) and
>   direct rendering for bulbs (which change with signal color). The display list cache is now
>   per-BlockPos (the bug fix). Reads `.ogldata` vertex data files for 3D geometry.
> - `trafficsignals/logic/AbstractBlockControllableSignalHead.java` — Block base class extending
>   `AbstractBlockControllableSignal`. Adds tile entity support (`hasTileEntity=true`,
>   `createNewTileEntity()`). Implements player interaction for cycling through body colors,
>   visor types, and tilt via sneak-click.
>
> State/data classes:
> - `trafficsignals/logic/TrafficSignalSectionInfo.java` — Per-section state container. Each
>   signal head has an array of these (one per light section). Stores body color, visor type,
>   bulb style, bulb type, bulb color for that section.
> - `trafficsignals/logic/TrafficSignalVertexData.java` — Parses `.ogldata` files into vertex
>   arrays for OpenGL rendering. These files are pre-processed model data in
>   `dev-env-utils/openGlData/`.
> - `trafficsignals/logic/TrafficSignalTextureMap.java` — Maps texture coordinates for signal
>   component textures (lights, body colors).
>
> Enum types (all in `trafficsignals/logic/`):
> - `TrafficSignalBodyColor` — BLACK, GRAY, YELLOW (housing paint color)
> - `TrafficSignalVisorType` — NONE, CAP, CIRCLE, TUNNEL, LOUVERED_H, LOUVERED_V, LOUVERED_BOTH
> - `TrafficSignalBodyTilt` — NONE, LEFT_TILT, RIGHT_TILT, LEFT_ANGLE, RIGHT_ANGLE
> - `TrafficSignalBulbStyle` — INCANDESCENT, LED, WLED (bulb technology)
> - `TrafficSignalBulbType` — SOLID, LEFT, RIGHT, AHEAD, UTURN, BIKE, RAIL (arrow direction)
> - `TrafficSignalBulbColor` — RED, YELLOW, GREEN
>
> Utility classes:
> - `codeutils/RenderHelper.java` — OpenGL rendering helpers
> - `codeutils/RotationUtils.java` — Rotation math for signal orientation
> - `codeutils/DirectionSixteen.java` — 16-direction enum for fine-grained rotation
> - `codeutils/AbstractBlockRotatableHZSixteen.java` — Base class for 16-direction blocks
>
> Blocks currently using custom rendering:
> - `trafficsignals/BlockControllableVerticalSolidSignal.java` — 3-section vertical solid signal
> - `trafficsignals/BlockControllableVerticalBikeSignal.java` — 3-section vertical bike signal
>
> Renderer registration:
> - `CsmClientProxy.java` line ~52: `ClientRegistry.bindTileEntitySpecialRenderer(
>   TileEntityTrafficSignalHead.class, new TileEntityTrafficSignalHeadRenderer())`
>
> Pre-processed model data:
> - `dev-env-utils/openGlData/*.ogldata` — Vertex data for signal bodies and visors
> - `dev-env-utils/src/main/java/.../tools/ModelToOglDataTool.java` — Tool that converts
>   Blockbench JSON models to `.ogldata` format
>
> New textures added by the custom rendering branch:
> - `textures/blocks/trafficsignals/lights/` — LED, WLED, and incandescent bulb textures
>   for various arrow types and colors
> - `textures/blocks/trafficsignals/bodies/` — Body color textures

---

## Rebase Summary (2026-03-28)

Rebased 8 commits from `dev/signal-custom-rendering-work-archived` onto latest `main`
(which includes all the Forge blockstate conversions, shared_models migration, dev-env-utils
improvements, and unused asset cleanup from the massive 2026-03-27 session).

**Commits on the rebased branch (oldest first):**
1. `b9061b96` WIP: Traffic Signal Models and States — The big initial commit: new Java classes
   (TE, TESR, block base, enums, vertex data, texture map), .ogldata files, body/visor models,
   new light textures
2. `40343bfd` Further progress — Iterative development
3. `a5610a4c` Further progress
4. `4873ff6c` Further progress
5. `92e399cf` Further progress
6. `cfd7d4f2` Further progress
7. `7778cd8d` Progress further 2
8. `ac808b3e` Progress further 3
9. `b5858ead` Fix shared display list bug in signal head renderer ← **THE BUG FIX**

**4 conflicts resolved during rebase:**
- `blockstates/controllableverticalsolidsignal.json` — accepted main's Forge format (the custom
  rendering branch had modified it for the old vanilla format)
- 3 backplate model files in `models/custom/trafficsignals/backplates/` — moved to the new
  `models/block/shared_models/trafficsignals/` location per main's restructuring

Build passes after rebase.

---

## Root Cause Analysis: State-Sharing Bug

**Symptom:** Changing body color/visor on one signal head changes ALL other signal heads to match.

**Root cause:** `TileEntityTrafficSignalHeadRenderer` stored a single `int staticDisplayList`
field. Since Minecraft registers ONE renderer instance per tile entity class (via
`ClientRegistry.bindTileEntitySpecialRenderer`), all `TileEntityTrafficSignalHead` tile entities
shared this single renderer instance, and therefore shared the same OpenGL display list.

**The bug in action:**
1. Signal A at pos (10,5,20) renders → `staticDisplayList` compiled with A's body color (BLACK)
   and visor (CIRCLE)
2. Signal B at pos (15,5,20) renders → reuses `staticDisplayList` → appears identical to A
3. Player sneak-clicks Signal A to change body to GRAY → `dirty=true`
4. Signal A re-renders → `staticDisplayList` recompiled with GRAY body
5. Signal B re-renders → `staticDisplayList` now shows GRAY body too (even though B's tile
   entity still stores BLACK)

**The fix (committed as `b5858ead`):**
```java
// BEFORE (broken — shared across all TEs):
private int staticDisplayList = -1;

// AFTER (fixed — per-TE cache):
private final Map<BlockPos, Integer> displayListCache = new HashMap<>();
```

The render method now looks up `displayListCache.get(te.getPos())` to get the display list
for that specific signal head. Each signal head gets its own cached display list that is only
recompiled when THAT head's `dirty` flag is set.

**Potential remaining concern:** The `displayListCache` HashMap grows indefinitely as signals are
placed. It doesn't clean up entries when signals are broken. For a normal gameplay session this
is fine (the map stores just a BlockPos and an int per signal), but for very long sessions with
lots of signal placement/removal, it could accumulate stale entries. A future improvement could
use `invalidate()` on block break or use a WeakHashMap alternative.

---

## Architecture Overview

### How Custom Rendering Works

Traditional Minecraft blocks use JSON blockstate → JSON model → texture pipeline. The custom
rendering system bypasses this for the signal HEAD (the light sections) using a
TileEntitySpecialRenderer (TESR) that draws directly with OpenGL.

**Rendering pipeline:**
1. Block is placed → `AbstractBlockControllableSignalHead.createNewTileEntity()` creates a
   `TileEntityTrafficSignalHead` with default section infos
2. Player can sneak-click to cycle body color, visor type, tilt (modifies TE, sets dirty flag)
3. Signal controller changes color state (0-3) via blockstate `COLOR` property
4. Each render tick, `TileEntityTrafficSignalHeadRenderer.render()`:
   a. Reads facing/color from blockstate, section infos/tilt from TE
   b. Applies GL transformations (translation, rotation, tilt offset)
   c. If dirty: recompiles display list with static geometry (body + visors)
   d. Calls cached display list for static parts
   e. Renders bulbs directly (these change with signal color, not cached)

**Static parts** (cached in display list): Signal body housing, visor geometry, backplate
**Dynamic parts** (rendered every frame): Bulb faces (change color with signal state)

### Data Flow

```
Player Click → TileEntityTrafficSignalHead (modify sectionInfos, set dirty)
                    ↓
Signal Controller → blockstate COLOR property (0=red, 1=yellow, 2=green, 3=off)
                    ↓
TileEntityTrafficSignalHeadRenderer.render()
  ├── Read sectionInfos from TE
  ├── Read COLOR from blockstate
  ├── If dirty: recompile display list (body + visors)
  ├── GL call cached display list
  └── Render bulbs (texture varies by COLOR + bulb style + bulb type)
```

### Components

| Class | Location | Role |
|-------|----------|------|
| `TileEntityTrafficSignalHead` | `trafficsignals/` | Per-signal state: sectionInfos[], bodyTilt, dirty flag. NBT read/write. |
| `TileEntityTrafficSignalHeadRenderer` | `trafficsignals/` | TESR: OpenGL rendering with per-BlockPos display list cache |
| `AbstractBlockControllableSignalHead` | `trafficsignals/logic/` | Block base: TE provider, player interaction, default section infos |
| `TrafficSignalSectionInfo` | `trafficsignals/logic/` | Per-section state: bodyColor, visorType, bulbStyle, bulbType, bulbColor |
| `TrafficSignalVertexData` | `trafficsignals/logic/` | Parses .ogldata files → float[] vertex arrays for GL rendering |
| `TrafficSignalTextureMap` | `trafficsignals/logic/` | Texture coordinate mapping for signal light/body textures |
| `RenderHelper` | `codeutils/` | GL rendering utilities (quad drawing, etc.) |
| `RotationUtils` | `codeutils/` | Rotation math helpers |
| `DirectionSixteen` | `codeutils/` | 16-direction enum for fine-grained rotation |
| `ModelToOglDataTool` | `dev-env-utils/` | Converts Blockbench JSON → .ogldata vertex format |

### Enum Types

| Enum | Values | Stored In | Purpose |
|------|--------|-----------|---------|
| `TrafficSignalBodyColor` | BLACK, GRAY, YELLOW | SectionInfo | Housing paint color |
| `TrafficSignalVisorType` | NONE, CAP, CIRCLE, TUNNEL, LOUVERED_H, LOUVERED_V, LOUVERED_BOTH | SectionInfo | Visor/shroud shape |
| `TrafficSignalBodyTilt` | NONE, LEFT_TILT, RIGHT_TILT, LEFT_ANGLE, RIGHT_ANGLE | TileEntity | Signal head tilt direction |
| `TrafficSignalBulbStyle` | INCANDESCENT, LED, WLED | SectionInfo | Bulb technology (affects texture) |
| `TrafficSignalBulbType` | SOLID, LEFT, RIGHT, AHEAD, UTURN, BIKE, RAIL | SectionInfo | Arrow/symbol direction |
| `TrafficSignalBulbColor` | RED, YELLOW, GREEN | SectionInfo | Which bulb color this section is |

### Pre-Processed Model Data

The `.ogldata` files in `dev-env-utils/openGlData/` contain pre-processed vertex data for
signal body and visor geometry. These are generated from Blockbench JSON models using the
`ModelToOglDataTool`. Files include:
- `1_vertical_mccain.ogldata` — McCain-style vertical signal body (single section)
- `1_vertical_mccain_door.ogldata` — Door/access panel for the body
- `visor_cap.ogldata`, `visor_circle.ogldata`, `visor_tunnel.ogldata`, etc. — Various visor shapes
- `visor_louvered_horizontal.ogldata`, `visor_louvered_vertical.ogldata`, `visor_louvered_both.ogldata`
- `visor_none.ogldata` — Placeholder for no visor

### Blocks Using Custom Rendering (2 of ~140+ signal blocks)

| Block | Registry Name | Sections | Default Config |
|-------|---------------|----------|----------------|
| `BlockControllableVerticalSolidSignal` | `controllableverticalsolidsignal` | 3 (R/Y/G solid) | Black body, circle visors |
| `BlockControllableVerticalBikeSignal` | `controllableverticalbikesignal` | 3 (R/Y/G bike) | Black body, circle visors |

All other signal blocks still use the traditional JSON model approach.

---

## Commit History on Rebased Branch

```
b5858ead Fix shared display list bug in signal head renderer    ← THE FIX
ac808b3e Progress further 3
7778cd8d Progress further 2
cfd7d4f2 Progress further
92e399cf Further progress
4873ff6c Further progress
a5610a4c Further progress
40343bfd Further progress
b9061b96 WIP: Traffic Signal Models and States                  ← Initial custom rendering code
```

These sit on top of main's full commit history including all the Forge blockstate, shared_models,
dev-env-utils work from the 2026-03-27 session.

---

## Checklist

- [x] Create working branch from archived branch
- [x] Rebase onto latest main (79 commits)
- [x] Resolve rebase conflicts (4 total)
- [x] Build verification
- [x] Root cause identification (shared display list)
- [x] Fix display list state-sharing bug (HashMap per BlockPos)
- [ ] In-game testing of the fix (multiple heads, different configs)
- [ ] Investigate any remaining issues (if found during testing)
- [ ] Clean up 8 dev commits into logical structure (user OK with history rewrite on branch)
- [ ] Expand custom rendering to more signal block types
- [ ] Consider display list cleanup on block break (minor memory leak)
- [ ] Eventually: merge to main when stable

---

## Known Considerations

- The custom rendering branch modifies `BlockControllableVerticalSolidSignal.java` which is one
  of the most important signal blocks. Main's Forge blockstate for this block was accepted during
  rebase, so the blockstate format is correct, but the block class now extends
  `AbstractBlockControllableSignalHead` instead of `AbstractBlockControllableSignal`.

- The `.ogldata` files and `ModelToOglDataTool` are development-time tools — the vertex data is
  read at runtime by the renderer. These files are NOT included in the mod JAR (they're in
  dev-env-utils).

- The custom rendering code adds new textures in `textures/blocks/trafficsignals/lights/` for
  different bulb styles (LED, WLED, incandescent) and `textures/blocks/trafficsignals/bodies/`
  for body color textures. These ARE included in the mod JAR.

- Player interaction (sneak-click to cycle options) is handled in
  `AbstractBlockControllableSignalHead.onBlockActivated()`. The order of cycling and which
  options are available per click type may need UX refinement.
