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
| `SHIFT` | `PropertyEnum<SignShift>` | none, setback, backtoback | Where in its block the plate is drawn |

### Dynamic State (`getActualState`)

`DOWNWARD` and `SHIFT` are computed dynamically from the world, not stored in meta:

**DOWNWARD** -- Set to `true` when the block below is a `BlockSlab`, or a guardrail the post
passes down through:
- Adds an extension post model below the sign to visually connect it to the surface
- Automatically detected each render frame

**SHIFT** -- `setback` when the sign is in front of an `AbstractBlockTrafficPole`'s signal arm or
hung from a span wire, `backtoback` when it pairs with a sign facing the other way, `none`
otherwise. Back-to-back wins over setback. See "The Three Shift Models" below.

### Meta Encoding

Only FACING (0-7) is stored in block meta. DOWNWARD and SHIFT are computed from world
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

## The Three Shift Models

Every sign's blockstate names a model for each value of `shift`, and the three are not three
independent models. They are one model drawn in three places, and how far back each one goes is
fixed by what has to line up with it.

| `shift` | Where the model sits | Why there |
|---|---|---|
| `none` | as authored: plate at the front of the block, post behind it at z 0.5 to 3.5 | the ordinary sign |
| `setback` | the whole model moved **+12.5** in z: plate at 12.5 to 13, post filling 13 to 16 | the plate lands in line with a signal arm's hardware, or with the housings hanging beside it on a span |
| `backtoback` | the whole model moved **+28.5** in z -- the setback plus a block -- **and the post dropped** | the plate lands in the PARTNER's block, on the far side of the partner's post, so one post carries a face each way |

The 28.5 is not a free choice. The partner a block behind is turned 180 degrees, so its own post
occupies z 28.5 to 31.5 in this sign's frame and its plate 31.5 to 32. Putting this sign's plate
at 28.5 to 29 backs it onto the near end of that post: two faces, one post, the way a real
back-to-back assembly is built. Anything nearer leaves daylight between the two signs; anything
further lands inside the partner's own plate and z-fights with it across the whole face. A sign
that is several blocks wide does the same thing at the same depth (see "Signs Bigger Than Their
Block") -- 28.5 is the one convention, whatever the plate is made of.

Two details that look like noise and are not. The back-to-back model paints its face on a
hundredth-of-a-unit sliver at 28.49 rather than on the plate itself, because the plate's own front
face is coplanar with the end cap of the partner's post; and it keeps no post of its own, because
the partner's is already standing there.

A third, which did ship wrong. Every hand-built plate paints its art on that sliver, in all three
shift models, and the plate used to draw its own front face as well: bare metal a hundredth of a
unit -- 0.000625 blocks -- behind the art. A 24-bit depth buffer behind Minecraft's 0.05 near plane
resolves `z * z / 838861` blocks at `z` blocks away, which passes that gap at 23 blocks and is
unreliable from half of it, so two thirds of the mod's signs flickered between their face and bare
metal from a dozen blocks out, and looked perfect from where a model gets checked (issue #212). The
plate's front face cannot simply be deleted: most faces have rounded corners and many do not fill
their plate, so the metal behind the art is part of the sign. Instead the plate keeps its sides and
back and loses its front, and a **backing** element carries that face 0.4 units further back --
0.41 behind the art, good past 100 blocks, where a sign is a few pixels. From the front nothing
changes, and the art does not move, so 28.49 stands. The yield-shaped models have no plate; there
it was the end cap of the post's centre bar, which the next box of the post covers anyway, and it
is simply gone. `SignFaceDepthTest` fails the build on two overlapping faces, looking the same way
in different textures, less than 0.2 units apart; `dev-env-utils/scripts/fix_sign_plate_backing.py
--apply` is the repair for a plate copied from an old revision.

### The invariant, and the check that holds it

**Every `shift` entry must name a model that actually differs from the one it is shifting from,
and a `backtoback` model must put its geometry in the block behind.** Both ways of breaking that
have shipped:

- a `shift` entry that names the default model -- the shift then renders as nothing, and the sign
  simply never goes back to back;
- a `backtoback` model built by keeping the FIRST element of the `none` model and moving that.
  On a sign model element zero is the plate, so this worked; on a mount model it is one bar of a
  bracket, so what appeared in the block behind was a fragment of the hardware with the rest of
  the bracket missing. A wrong model looks worse than no model.

`SignShiftModelTest` (roads module) measures the shipped resources and fails the build on either:
it resolves all three models for every blockstate whose default model lives under
`csm:trafficsigns/`, and requires that `setback` and `backtoback` each differ from `none`, that
`backtoback` is not merely the setback model, that its front-most geometry lies between one block
and 29 units behind the `none` model's, and that the plate it paints is the same size as the
sign's own. It reads OBJ-backed signs too, in blocks rather than model units.

A `backtoback` model with no elements at all is allowed for a piece that has no face of its own:
`signpost` is all post, and in back-to-back the partner's post already stands through this block,
so drawing a second one would only z-fight with it.

### What is exempt, and why

Three signs are on the test's exemption list, because back-to-back has no meaning for them
rather than because it is unfinished:

- **`signstatelawstopforpeds`** and its LED twin -- the R1-6 in-street paddle stands in the
  roadway on a flexible base, painted on both sides. There is no plate to move and no post to
  share.
- **`signdoubleoneway`** / **`signdoubleonewayb`** -- the double one-way blades already read from
  both sides, and they run 26 units along the very axis the shift moves. A copy a block back
  would have to reach z 43.5, past the -16..32 an element may occupy before the whole model
  silently fails to load.

### Which sign of a pair moves

Only one of the two may take `backtoback`: the model puts the plate in the partner's block, so if
both took it they would swap places, each ending up behind the other with its art pointing inward
and its blank back to the world.

Support below normally decides it -- the sign standing on its own post keeps its place, the one
hanging in front of it moves. But a pair hung in the air, on a span wire or on a mast, has support
under neither, and both used to qualify; that is what made back-to-back look broken on the big
panel signs. `AbstractBlockSign.isDesignatedShifter` breaks the tie: the facings of a pair are
always opposite, so exactly one of them is north or west, and that one moves. It has to be a
property of the pair rather than of whichever block the renderer reaches first, because each
block computes its own `getActualState` knowing nothing of what the other decided --
`SignBackToBackRuleTest` pins that down as a pure function.

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

### What the Three Shift Models Are

`AbstractBlockSign.getActualState` works out a `shift` of `none`, `setback` or `backtoback` from
the sign's neighbours, and the blockstate's `shift` variant swaps the model for it. The shift is
*only* that model swap -- the block never moves -- so a plate shape needs all three models
authored, and each has a fixed geometry:

| Shift | Plate | Post | Why |
|---|---|---|---|
| `none` | at the block's front face, z 0..0.5 | z 0.5..3.5, behind the plate | the ordinary roadside sign |
| `setback` | z 12.5..13 | z 13..16, reaching the back of the block | the whole assembly moved 12.5 units to the back of its own block, to clear a signal mast arm or to line up under a span wire |
| `backtoback` | z 28.5..29, i.e. in the block *behind* | none | the partner's plate crosses into the supported sign's block and mounts on the back of *its* post; the two share one post |

Back-to-back only ever shifts the partner that has nothing under it, so the sign that owns the
post keeps `none` and its post stays where it is. That is what makes the fixed 28.5 work: a
`none` post always ends at z 3.5, so a plate whose front face lands at z 28.49 (28.5 - the plate's
own thickness, mirrored into the neighbour's block) always comes to rest flush against the back
of it.

**Every `shift` entry must name a model that actually differs from the default.** An entry that
repeats the default model, or names a copy with the same element boxes, compiles and loads
cleanly and then does nothing: the computed shift never reaches the screen, and two signs placed
back to back simply intersect. This is what the yield-plate family
(`yieldsign`, `signnopassingzone`, `signrailroadcrossbuck`, `signschoolbusstopahead`,
`signschoolcrossing`, `signschoolcrossingflashingled`) did until `yield_sign_setback` and
`yield_sign_back_to_back` were written for it.

On a plate that is painted on both sides -- the yield family paints `#1` on the north face and a
`_back` texture on the south -- the back-to-back model keeps **both** faces. The back is not
merely hidden behind the partner: the yield plate is 23 units across, wider than the block and
wider than a standard 16-unit plate, so on a mixed pair its corners show around whatever is in
front of them, and they have to show the sign's own back rather than its legend reversed.

The one deliberate exception is the in-street paddle (see "The In-Street Pedestrian Sign" below),
which has no post to move and already carries its legend on both faces.

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

It is the one place the "every shift entry must name a model that actually differs" rule above
is deliberately broken, and it is worth saying why rather than leaving it to be found again.
Setback exists to get a plate out from under a signal mast arm or into line with a span wire,
neither of which an object bolted to the pavement ever meets; and back-to-back exists so two
signs can share one post, where this one has no post, already carries its legend on both faces,
and would have its base dragged out of the block a player put it in. So both entries are empty
on purpose.

Its face is written at 256 px: a 1:3 face squished into a square texture has only 5 px per
plate unit down its long side at 128, and it blurred a few blocks away.

## Signs Bigger Than Their Block

`danger_low_clearance_sign` (3 x 2 blocks), `cars_only_banner_sign` (5 x 1) and
`truckers_use_expressways_sign` (5 x 3) are panels several blocks across, made from the user's
own artwork. Each is still one ordinary `BlockTrafficSign` in one block: the plate overhangs the
neighbouring blocks, centred on the block it was placed in both across and up and down, so a
3 x 2 panel reaches one block either side and half a block above and below.

The plate cannot be a JSON element model like the others. An element may reach no further than
-16..32 on any axis, and past that the whole model silently fails to load, so the widest JSON
plate is 48 units. These are Forge OBJ models instead, which have no such limit, generated by
`dev-env-utils/scripts/gen_large_custom_signs.py` from one catalogue line per sign (size in
blocks, texture size). A block model transform `scale` on a small JSON plate would also have
reached, but it scales the setback and back-to-back depths along with the plate, so those models
would have had to be authored at a fraction of their real offsets.

Everything else is the ordinary sign, on the JSON plates' own offsets: the plate is half a unit
thick on the same 1 x 3 post, the setback model moves it back 12.5 units onto a post reaching the
back of the block, and the back-to-back model moves it 28.49 units back, into the partner's
block, with no post of its own -- a hundredth of a unit in front of the back of the partner's
post, where the JSON plates put their own art sliver, so neither face z-fights the other. The
front face wears the art and the back and edges wear `absolutely_nothing_sign`, so transparent
corners in the art are see-through rather than grey. The source art is not in the repository; the
script's `--art` option rebuilds the textures from it, and `--check` covers the models and
blockstates.

What the overhang does not bring with it: the selection and collision boxes are the ordinary
sign's, inside the placed block, so the plate is only clicked and only stops a player there; and
the overhanging part is drawn with the placed block's render section, so it can vanish at the
very edge of the screen when that section is culled.

### A back-to-back pair, and why only one plate may move

Moving a plate nearly two blocks back is only right while exactly ONE sign of a pair does it. The
first pair of these panels hung in mid air showed what happens otherwise: neither had support
below it, so both took the shift, both plates moved 28.49 units back, and they swapped places --
each ended up behind the other, arts pointing at each other, blank backs to the world, a block
and a half apart. The panels made it obvious, but the geometry is every sign family's.

The answer is not a second convention for the big panels. `AbstractBlockSign.getShouldBackToBack`
picks one sign of the pair, so a pair with no support under either still moves one plate and
leaves the other where it is, and the assembly comes out the way a posted pair always did: the
unshifted panel's plate, its post, then the shifted panel's plate bolted to the back of that same
post, each art facing out. Keep the panels on 28.49 for that reason -- an offset of their own
would look tidy on a hanging pair and break every pairing with a sign of another family.

### Why the art is letterboxed, not squished

Every one-block sign squishes its face into a square texture and lets the plate stretch it back
out, and at those aspects it does no harm. On these panels it did: the first version squished a
5:1 banner into a 512 px square, so the legend had 512 texels over 16 units down the plate and
512 over 80 across it, a fivefold difference. OpenGL picks the mip level from the axis with more
texels per screen pixel, so a few blocks away the whole face was drawn from the level the tall
axis wanted, and the long axis -- where the lettering is -- got a fifth of the detail it needed
and turned to mush.

So the art keeps the plate's aspect. `--art` resamples it once to the plate's proportions (the
2.06:1 DANGER art is fitted to its 3 x 2 plate here, not by the UVs), at a whole number of pixels
per block on both axes, and centres it in the square texture; the front face's UVs pick out just
that rectangle, brought in half a texel. Texel density is then the same both ways, and the
texture size is chosen to keep it at or above a one-block sign's 128 px a block: 1024 px for the
5-wide panels (192 px a block), 512 for DANGER (149). The rest of the square is not left
transparent. The art's edge pixels are repeated outward to fill it, alpha included, so the lower
mip levels average the plate's rim with more of itself rather than with transparency or a
neighbouring atlas sprite, while an edge the art itself leaves transparent (the TRUCKERS panel's
rounded corners) stays transparent. Squishing the art to fill the square again would look like
a tidy simplification and would bring the blur straight back.

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

### A face fills its plate

The plate model, not the texture, gives a sign its proportions: every plate's `#1` face maps the
whole square texture, so the drawn sign must run to the texture's edges and the plate must have
the sign's aspect. A narrow sign drawn in the middle of a square texture with transparent margins
looks right from the front only -- the plate's back and edges show the full plate around it, a
wide gray board behind a slim face. Two kept-artwork signs were drawn that way on the 16 x 21 tall
plate and now have plates of their own, the same height as the tall plate and with its post and
its `setback` / `back_to_back` twins:

| Model | Face | Signs |
|---|---|---|
| `metal_sign_tall_narrow` | 12.6 x 21 (0.6) | `ladotsignalsync` (an 18 x 30 in sign) |
| `metal_sign_tall_extra_narrow` | 10.5 x 21 (0.5) | `verizondig` |

Their textures were cropped to the drawn sign and stretched back out to the square (256 px, so the
stretch loses nothing). Before giving a sign a new plate, measure the opaque bounds of its face
texture against the plate's `#1` face; a fill well under the full width or height is this fault.

### A plate needs its own shift twins

The same aspect rule binds the `shift` variants. A sign's blockstate swaps the whole model for
`setback` and `backtoback`, so those models have to carry the *same* plate as the default one --
only moved back in Z, by 12.5 for the setback and to the neighbouring cell at 28.5 for the
back-to-back, which drops the post and paints the face on a 0.01-thick decal in front of a blank
plate. Borrowing another family's twin silently changes the plate size, and because the `#1` face
maps the whole texture either way, the art squashes to whatever plate it lands on: the four
ultra-tall signs pointed at the 16 x 21 tall twins and shrank to half their height the moment
they were set back.

| Plate | Default | Setback | Back to back |
|---|---|---|---|
| 16 x 40 | `metal_sign_ultratall` | `metal_signpostback_ultratall_sign_setback` | `..._back_to_back` |
| 20 x 40 | `metal_sign_ultratall_wide` | `metal_signpostback_ultratall_wide_sign_setback` | `..._back_to_back` |

So a new plate is three models, not one. The check is mechanical: for each blockstate with a
`shift` variant, the `#1` element of the shift model must span the same width and height as the
`#1` element of the default model.

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
