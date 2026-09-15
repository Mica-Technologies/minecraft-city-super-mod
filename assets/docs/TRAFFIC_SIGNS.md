# Traffic Signs System

Technical documentation for the traffic sign subsystem in the City Super Mod.

## Overview

The traffic signs system provides 472 road sign blocks modeled after real US road signs. While
each sign is a simple block with only a registry name override, the system uses a sophisticated
Forge blockstate format with dynamic properties for post extensions and signal arm setback.

All sign code lives in `src/main/java/com/micatechnologies/minecraft/csm/trafficsigns/`.

## Class Hierarchy

```
AbstractBlock
└── AbstractBlockRotatableHZEight  ← 8-direction horizontal rotation
    └── AbstractBlockSign          ← sign-specific properties (DOWNWARD, SETBACK)
        ├── BlockBeachClosedSign
        ├── BlockLHSStopSign
        ├── BlockSpeedLimit25Sign
        ├── ... (472 concrete sign blocks)
        └── BlockTrafficSign           ← one class, registry name per instance
            └── BlockInStreetSign      ← the R1-6 paddle; settles onto the road below
```

Every concrete sign block is minimal -- just a registry name:

```java
public class BlockSpeedLimit25Sign extends AbstractBlockSign {
    @Override
    public String getBlockRegistryName() {
        return "speedlimit25sign";
    }
}
```

All behavior is inherited from `AbstractBlockSign`.

## AbstractBlockSign

**Extends:** `AbstractBlockRotatableHZEight`

### Properties

| Property | Type | Values | Purpose |
|---|---|---|---|
| `FACING` | `PropertyEnum<DirectionEight>` | N, NE, E, SE, S, SW, W, NW | 8-direction rotation (inherited) |
| `DOWNWARD` | `PropertyBool` | true/false | Whether to show extension post below |
| `SETBACK` | `PropertyBool` | true/false | Whether sign is set back from a traffic pole |

### Dynamic State (`getActualState`)

`DOWNWARD` and `SETBACK` are computed dynamically from the world, not stored in meta:

**DOWNWARD** -- Set to `true` when the block below is a `BlockSlab`:
- Adds an extension post model below the sign to visually connect it to the slab surface
- Automatically detected each render frame

**SETBACK** -- Set to `true` when the sign is adjacent to an `AbstractBlockTrafficPole`:
- Checks the block behind the sign (based on facing direction) for a traffic pole
- Also inherits setback from signs directly above or below that are in front of poles
- Changes the model to push the sign back from the pole for visual accuracy

### Meta Encoding

Only FACING (0-7) is stored in block meta. DOWNWARD and SETBACK are computed from world
context via `getActualState()`. This avoids wasting meta bits on information that can be
derived.

### Bounding Box

Small flat box oriented by facing direction:
```java
new AxisAlignedBB(0.3125, 0.0, 0.875, 0.6875, 1.0, 1.0)
```
Automatically rotated for all 8 directions via `RotationUtils`.

### Render Layer

`BlockRenderLayer.CUTOUT_MIPPED` -- required for transparent sign textures.

## Forge Blockstate Format

Traffic signs use the **Forge blockstate format** (`"forge_marker": 1`) instead of vanilla's
flat variant system. This is critical for managing the combinatorial explosion of properties.

### Why Forge Format?

With 8 facing x 2 downward x 2 setback = **32 variant combinations** per sign, vanilla's flat
format would require 32 explicit entries. Forge's format lets you define each property
independently.

### Structure

```json
{
  "forge_marker": 1,
  "defaults": {
    "model": "csm:metal_signpostback_diamond_sign",
    "textures": {
      "all": "csm:blocks/signpostmetal",
      "particle": "csm:blocks/signpostmetal",
      "0": "csm:blocks/signpostmetal",
      "1": "csm:blocks/speedlimit25sign"
    }
  },
  "variants": {
    "facing": {
      "n":  {},
      "ne": { "transform": { "rotation": [{"x":0}, {"y":45},  {"z":0}] } },
      "e":  { "transform": { "rotation": [{"x":0}, {"y":90},  {"z":0}] } },
      "se": { "transform": { "rotation": [{"x":0}, {"y":135}, {"z":0}] } },
      "s":  { "transform": { "rotation": [{"x":0}, {"y":180}, {"z":0}] } },
      "sw": { "transform": { "rotation": [{"x":0}, {"y":225}, {"z":0}] } },
      "w":  { "transform": { "rotation": [{"x":0}, {"y":270}, {"z":0}] } },
      "nw": { "transform": { "rotation": [{"x":0}, {"y":315}, {"z":0}] } }
    },
    "downward": {
      "false": {},
      "true": {
        "submodel": {
          "extension": {
            "model": "csm:metal_signpost",
            "transform": { "translation": [0, -1, 0] }
          }
        }
      }
    },
    "setback": {
      "false": {},
      "true": {
        "model": "csm:metal_signpostback_diamond_sign_setback"
      }
    }
  }
}
```

### Key Forge Features

| Feature | Vanilla | Forge |
|---|---|---|
| **Marker** | None | `"forge_marker": 1` required |
| **Defaults** | N/A | Shared config applied to all variants |
| **Variant Structure** | Flat map of all combinations | Separate maps per property |
| **Transformations** | Only `"y"` rotation (0/90/180/270) | Full `transform` with x/y/z rotation and translation |
| **Submodels** | N/A | Conditional model addition with positioning |
| **Texture Override** | Per-variant only | Global defaults with per-variant overrides |

### Texture Slots

| Slot | Purpose |
|---|---|
| `"all"` | Fallback texture |
| `"particle"` | Break/fall particle texture |
| `"0"` | Metal post texture (shared across all signs) |
| `"1"` | Sign face texture (unique per sign block) |

### Shared Base Models

| Model | Shape | Used By |
|---|---|---|
| `metal_signpostback_diamond_sign` | Diamond frame | Warning signs |
| `metal_signpostback_diamond_sign_setback` | Diamond frame (set back) | Warning signs near poles |
| `metal_signpost_octagon_sign` | Octagonal frame | Stop signs |
| `metal_signpost` | Simple vertical post | Extension posts (DOWNWARD) |
| (and others for rectangular, square, etc.) | | |

## Adding a New Traffic Sign

1. Create a class extending `AbstractBlockSign`:
   ```java
   public class BlockMyCustomSign extends AbstractBlockSign {
       @Override
       public String getBlockRegistryName() {
           return "mycustomsign";
       }
   }
   ```

2. Create the sign face texture at `textures/block/mycustomsign.png`

3. Create blockstate JSON at `blockstates/mycustomsign.json`:
   - Copy an existing sign's blockstate as a template
   - Change `"1"` texture in defaults to `"csm:blocks/mycustomsign"`
   - Choose the appropriate base model (diamond, octagon, rectangle, etc.)

4. No block model JSON needed -- the Forge blockstate format handles everything via the
   shared base models and texture slots.

5. Add lang entry: `tile.mycustomsign.name=My Custom Sign`

6. Register in `CsmTabRoadSigns` via `initTabBlock(BlockMyCustomSign.class, event)`

The 8-direction rotation, slab extension, and pole setback behavior are all inherited
automatically.

## Stacking Behavior

Signs inherit facing from the sign directly below them (from `AbstractBlockRotatableHZEight`,
through `AbstractBlockSign.inheritsFacingFrom`). This means placing a sign on top of another sign
automatically aligns them. Players only need to set the facing on the bottom sign. Only a sign
block passes its facing up: a sign on a guardrail faces the way the player chose.

A sign standing on a guardrail (any `ICsmPostPassesThrough` block) sets `downward`, the same
property the slab extension uses, and draws its post one block further down to the ground. Only
the slab case also extends the sign's bounding box; see `GUARDRAIL_SYSTEM.md`.

## The In-Street Pedestrian Sign

`signstatelawstopforpeds` (and its LED twin) is the R1-6 paddle: a 12 x 36 face on a flexible
base standing in the roadway at a crosswalk, not a plate on a post. It has its own model,
`trafficsigns/in_street_sign` -- an 8 x 24 face at the other signs' scale, painted on both
sides, on a low base -- and its own block class, `BlockInStreetSign`. That subclass of
`BlockTrafficSign` is `ICsmRoadSurfaceAware`: because the paddle stands on the road rather
than on a post, it settles onto a partial-height surface below it (a road that climbs, a slab,
a snow layer) exactly as the work-zone devices do, applying the same offset to its render
offset, its selection box and its collision box (see `WORK_ZONE_ACCESSORIES.md`, "Settling
onto the road below"). The shift and downward variants exist on it, since every traffic sign
carries those properties, but its blockstate maps them all to the same model: there is no
post to set back or extend.

Its face is written at 256 px: a 1:3 face squished into a square texture has only 5 px per
plate unit down its long side at 128, and it blurred a few blocks away.

## LED-Enhanced Flashing Signs

`signpoststopsignflashingled`, `signpoststopsignflashingleddense`, `signwrongwayflashingled`,
`signdonotenterflashingled`, `signpedestrianflashingled`, the two
`signarrowplaquefloyellowdown{left,right}flashingled` plaques, `signschoolcrossingflashingled`,
`signstatelawstopforpedsflashingled` and `signyieldheretopedsflashingled` are the solar LED-enhanced signs
(MUTCD 2A.07): the plain sign with a ring of LEDs on its face along the border, blinking once a
second -- red on the regulatory signs, amber on the pedestrian warning diamond and its W16-7P
arrow plaques, as on the real units. They are ordinary `BlockTrafficSign` blocks with no control
-- the real units run whenever they are installed.

Nothing about them is geometry. The LEDs are composited into the sign's own face texture as a
two-frame strip (dark, lit) and the `.mcmeta` gives the lit frame 2 ticks of a 20-tick cycle:
one 100 ms blink per second. Putting the clock in the texture rather than in a tile entity is
the same choice the RRFB makes, and for the same reason -- Minecraft advances every sprite
animation off the global tick counter, so every LED sign in the world blinks in step, as a bank
of them on one solar controller does. The blockstate is the plain sign's with the texture paths
swapped, so shift, downward and stacking all carry over untouched.

Each strip has an OptiFine emissive companion (`<name>_e.png`, declared by suffix in Core's
`assets/minecraft/optifine/emissive.properties`) animated on the same timing: an empty frame
while dark, the lit LEDs and their bloom alone while lit. Under OptiFine that frame draws at
full brightness, so the blink reads as light at night; without it the base strip's lit frame
still blinks, just shaded like the rest of the sign. OptiFine does not run in the deobfuscated
dev client, so the emissive path can only be checked in a packaged install.

All of it -- textures, `.mcmeta`, companions, blockstates -- comes from
`dev-env-utils/scripts/gen_led_signs.py`, which reads the LED positions off each base texture's
own outline (the octagon's vertices are where its edge meets the texture edge; a rectangle's
panel is its opaque bounds; the diamond's points are where it meets each texture edge, its
edges inset by the diamond's own apothem) rather than hard-coding them, and pre-stretches the WRONG WAY dots
by the wide plate's 22:16 aspect so they come out round in the world. The lang lines and tab
registrations it prints are added by hand next to the plain sign's own.

## Where Sign Faces Come From

Almost every road sign's face texture is generated, not hand-painted. Two generators own them,
and both have a `--check` that fails when a texture no longer matches the script:

- `dev-env-utils/scripts/gen_official_faces.py` -- re-faces signs that already exist. It never
  touches registration, lang or blockstates: it reads the sign's blockstate for the plate model and
  the texture on slot `1` (often not named after the registry), measures the plate's aspect off the
  model's `#1` faces, and writes that one texture.
- `dev-env-utils/scripts/gen_gap_signs.py` -- signs added by the 2026-09 catalogue review; it also
  owns their blockstates, lang lines and tab registration (`--apply`).

### Preference order

1. **The official drawing.** `shs_signs.py` renders a sign straight off the FHWA Standard Highway
   Signs book (2004 chapter PDFs, public domain) or an interim per-sign ZIP, both fetched into the
   gitignored `_shs_cache/`. `recolour` maps the book's print colours onto `MOD_COLOURS`
   (`SHS_PALETTE`); a face stretches to fill its plate. Options on `SHS(...)`: `pick` (a page with
   several signs), `mirror` / `mirror_symbols` / `rotate_symbols` (left-hand and turned arrows),
   `replace=` (re-set a numeral or word: `('50', '35')`, pairs may carry a colour), `blank=True`
   (panel only, every legend dropped).
2. **A book panel with the mod's own legend.** `BOOK_LINES`, `TEXT_PANEL`, `TEXT_DIAMOND` (the W8-1
   diamond, lines fitted to its width band by band), `TWO_PANEL` (the W13-3 RAMP layout),
   `SHS_PANEL_COLOUR` (a book sign on another panel colour when its symbol shares the panel's
   yellow).
3. **A public-domain MUTCD SVG** in `dev-env-utils/scripts/artwork/` (`SVG_SIGN`, `_svg_ink`), for a
   symbol the 2004 book does not draw (W5-2a narrow bridge, California W82 streetcar).
4. **Drawn from a photograph or the original texture**: `PANEL` / `PANEL_LINES` panels and the named
   drawers (`STATE_PROPERTY`, `BUS_LANE`, `FALLOUT_SHELTER`, `LA_NO_STOPPING`, `HV_DANGER`, ...),
   with book symbols lifted off their pages where one fits (`_ink_symbol`, `_book_stop`), or a
   glyph lifted off the sign's own original texture and sharpened (`_original_glyph`).
5. **Left as it was.** Branded or artwork signs (store price tags, agency logos, route shields,
   facility placards) keep their textures; several had only their background shifted onto
   `MOD_COLOURS` by a one-off recolour, so `--check` does not cover them.

### Lettering

Every legend a generator *sets* uses the real FHWA Standard Alphabets, Series B, C, D, E, E(M) and
F, extracted at run time from the book's `Alphabets.pdf` into `_shs_cache/series/`. They are never
committed. `shs_signs.set_legend_line` picks the widest allowed series that fits the line at its cap
height, with the fonts' own letter spacing (it matches the book's glyph placement to within 0.3%) and
the book's measured word space: 0.49 cap height for B/C/D, 0.69 for E and wider. The book chooses the
series per line, not per sign -- long lines drop to B or C -- so drawers pass a tuple of acceptable
series per line. No alphabet has an apostrophe; the series' own comma is drawn raised. Squeezing one
wide font sideways to fit a line is exactly what not to do: it thins the verticals and leaves the
horizontals heavy.

The mod's shipped `highway_gothic_wide.ttf` is Series E width; it remains the fallback for a
character no alphabet has.

### Matching the sign being replaced

`dev-env-utils/scripts/detect_legend_series.py <registry> "LINE|LINE"` reads a sign's first version
from git and measures each legend line's centre, cap height and nearest series. `PANEL(layout=...)`,
`PANEL_LINES` and `BOOK_LINES` take those measurements, so a remake keeps the layout the sign had.
Check its output by eye on small 128 px originals: a blurry thin line measures a series too wide.

### Reviewing a change

`gen_official_faces.py --only a,b --sheet out.png` writes a before/after contact sheet without
touching the tree; every batch of face changes has been reviewed on one before being written.
