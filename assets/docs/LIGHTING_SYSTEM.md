# Lighting System

Technical documentation for the lighting subsystem in the City Super Mod.

## Overview

The lighting system provides 119 realistic light fixture blocks (street lights, ceiling lights,
wall-mounted fixtures) with a 4-state on/off system combining redstone control and manual
override. Lights can project illumination downward using invisible "light-up air" blocks, and the
floodlights along their aim ("Floodlights").

The fixtures live in the Lighting module, `modules/lighting/src/main/java/com/micatechnologies/minecraft/csm/lighting/`.
The light-up air they project is **Core's**: the block, its column projection and the stadium
light footprints are in `src/main/java/com/micatechnologies/minecraft/csm/codeutils/light/`, so
Sports & Venues' stadium light heads can place it too (see "Light-Up Air in Core" below).

## Architecture

The system has three categories of blocks:

1. **Interactive Bright Lights** (extend `AbstractBrightLight`) -- the main fixtures with
   redstone + manual on/off control and light emission
2. **Structural Mounting Blocks** (extend `AbstractBlockRotatableNSEW/UD`) -- passive brackets,
   mounts, and junction boxes with no lighting logic
3. **BlockLightupAir** (Core) -- invisible light source block automatically placed/removed by lights

There are **no tile entities** in the lighting system. All logic is handled through block state
properties and block event methods.

## 4-State Control System

Every `AbstractBrightLight` has a `STATE` property with 4 values:

```java
public static final PropertyInteger STATE = PropertyInteger.create("state", 0, 3);

static final int STATE_RS_OFF  = 0;  // Redstone Off (light off, responds to redstone)
static final int STATE_RS_ON   = 1;  // Redstone On (light on, responds to redstone)
static final int STATE_MAN_OFF = 2;  // Manual Off (overrides redstone, stays off)
static final int STATE_MAN_ON  = 3;  // Manual On (overrides redstone, stays on)
```

### State Transitions

```
                    ┌──── Redstone ON ────┐
                    │                     │
                    v                     │
  ┌──────────┐  power  ┌──────────┐      │
  │ RS_OFF   │ ──────> │ RS_ON    │      │  Automatic Mode
  │ (off)    │ <────── │ (on)     │      │  (responds to redstone)
  └──────────┘  unpower └──────────┘      │
       │                    │             │
       │  click             │  click      │
       v                    v             │
  ┌──────────┐         ┌──────────┐      │
  │ MAN_OFF  │ click   │ MAN_OFF  │      │  Manual Mode
  │ (off)    │ ──────> │          │      │  (ignores redstone)
  └─────���────┘         └──────────┘      │
       │                                  │
       │  click                           │
       v                                  │
  ┌──────────┐                            │
  │ MAN_ON   │ ── click ─────────────────┘
  │ (on)     │     (returns to auto mode based on current redstone)
  └──────────┘
```

**Redstone handling** (`neighborChanged`):
- Only responds when in `STATE_RS_OFF` or `STATE_RS_ON`
- Ignores redstone entirely when in manual modes

**Player interaction** (`onBlockActivated`):
- From `RS_OFF` or `RS_ON` -> `MAN_OFF` (enters manual mode, turns off)
- From `MAN_OFF` -> `MAN_ON` (stays manual, turns on)
- From `MAN_ON` -> Auto mode (returns to `RS_OFF` or `RS_ON` based on current redstone power)

## Light Projection System

Street lights and post lights need to illuminate the ground below them. Since Minecraft
calculates light from block positions, a ceiling-mounted light 10 blocks up wouldn't light the
ground well. The solution is invisible "light-up air" blocks.

### How It Works

When a light turns ON, `handleAirLightBlock(true, world, pos)` is called, which hands its column
to Core's `CsmLightupAir.projectColumn` (the code that used to sit in `AbstractBrightLight`,
moved unchanged):
1. Scans **downward** from the light's position (up to 16 blocks), through the air below it
2. Takes the **lowest** air cell before the first non-air block, the one just above the floor,
   unless that is the cell directly beneath the light
3. Places a `BlockLightupAir` at that position (invisible, passable, emits light level 15)

If the scan meets a `BlockLightupAir` first, the light already has one and nothing is placed: the
lightupair is not air, so without that check the scan would stop on it and stack a second one in
the cell above. Nor does the scan load a chunk; an offset column in an unloaded one is left alone.

When a light turns OFF, the method removes the first `BlockLightupAir` in the column below.

### Lights That Arrive Already Lit

`neighborChanged` and `onBlockActivated` are not the only ways a lit state gets into the world:
`/setblock`, `/fill`, `/clone`, structure blocks and WorldEdit or FAWE pastes all write one
directly, and a light that arrives that way rendered lit but cast no light (issue #264). So a lit
light heals itself, always on the lit side only (an off light never removes a lightupair):

- `onBlockAdded` schedules an update, and `updateTick` places the missing lightupair. The tick
  comes a little later so a paste has written the floor below first. This covers setblock, fill,
  clone, structures and WorldEdit.
- Bright lights tick randomly, and vanilla routes a random tick to `updateTick`. FAWE writes
  chunk sections directly and never calls `onBlockAdded`, so this is what catches its pastes, and
  worlds that already hold dark lit fixtures, about a minute after a player comes near at the
  default `randomTickSpeed`. The heal is a short column scan that stops on the existing lightupair.
  Test it at the default speed: FAWE's tick limiter (`physics-ms` in its `config.yml`) drops most
  random ticks once `randomTickSpeed` is turned far up, so a cranked-up test reads as a heal that
  never runs.
- `/csmlighting relight <x1 y1 z1> <x2 y2 z2>` (op) does a whole box at once, loaded chunks only,
  up to 512 x 64 x 512 blocks.

A redstone light pasted in state 1 (on) with no power stays on: the heal never re-reads redstone,
so a fixture set lit on purpose is not switched off.

### Light X-Offset

Post-mounted lights (like street lights on poles) offset the light projection horizontally:

```java
// In AbstractBrightLight:
public int getBrightLightXOffset() { return 0; }  // Default: straight down

// In BlockPostLight1:
@Override
public int getBrightLightXOffset() { return 1; }  // 1 block to the side
```

A post top whose head is cantilevered off to one side (the GE Evolve EALS on its pole-top
knuckle) wants its light under the head, whichever way it is turned. Such a light returns
`true` from `isBrightLightOffsetFacingRelative()` (`BlockBrightLightFactory.PostTopAimed`): its X
offset then runs forward along `FACING` and its Z offset to the right (`BrightLightOffset`).
Every other light keeps the world-X offset above, so the symmetric post tops (`mclaclassicpostlight`,
`mclaparklight`, `postlight1-3`) still light the cell east of their pole, where their placed
light-up air already is.

### BlockLightupAir

- Core's: `codeutils.light.BlockLightupAir`, registry name `lightupair`, registered from Core's
  hidden tab `CsmTabCoreHidden` (order -16), so it exists whichever modules are installed. Its
  blockstate and its lang line (`tile.lightupair.name`) ship in Core's jar
- Extends `AbstractBlockRotatableNSEWUD`
- Invisible and passable (null collision box, tiny render box)
- Replaceable (can be overwritten by block placement)
- Render layer: CUTOUT
- Emits light level 15

### Light-Up Air in Core

The block moved from Lighting to Core in 2026-10 so the stadium light heads in Sports & Venues,
which depend on Core only, could light a field. Nothing about Lighting changed: same registry
name, same column rule (`CsmLightupAir.projectColumn` is the old `handleAirLightBlock` body),
same heal, same creative order for every visible Lighting block. Existing worlds load unchanged:
a block's id is saved by name, and the registry dump differs only in where the hidden
`lightupair` sits among the hidden blocks (`--unordered-hidden`).

What Core holds, in `codeutils/light/`:

| Class | What it is |
|---|---|
| `BlockLightupAir` | the block, registered from `tabs/CsmTabCoreHidden` |
| `CsmLightupAir` | the column projection, the floodlights' beam projection (`beamPath`, `beamTargets`, `settle`, `beamLights`, `projectBeam`; see "Floodlights"), `firstBelow` (the light-up air a column scan meets before anything else) and `claimedBySource` |
| `ICsmLightupAirSource` | implemented by `AbstractBrightLight`: "do you, lit, rely on this light-up air?" |
| `AbstractLightFootprintTileEntity`, `CsmLightFootprints` | a light that lights a grid of columns (the stadium heads), placed and removed a few a tick; see `VENUES_SYSTEM.md` |

**Sharing.** The light-up air stores no owner. A fixture's scan stops on any light-up air already
in its column, so a street light under a stadium footprint may rely on a cell a head placed. The
footprints therefore never remove a cell that a lit fixture claims (`claimedBySource` asks every
`ICsmLightupAirSource` within 16 cells above and one to either side along x), nor one another
lit head's column meets (it takes the cell over). The other way round is unchanged: a fixture
turning off removes the first light-up air in its column, whoever placed it, and a head missing
that cell fills it again at its next heal.

## AbstractBrightLight Base Class

**Extends:** `AbstractBlockRotatableNSEW`

```java
public AbstractBrightLight() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, false);
    // false = don't set default state (we set it manually with STATE property)
    this.setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(STATE, STATE_RS_OFF));
}
```

### Properties

| Property | Type | Values | Purpose |
|---|---|---|---|
| `FACING` | PropertyDirection | N, S, E, W | Horizontal rotation (inherited) |
| `STATE` | PropertyInteger | 0-3 | On/off state (see above) |

### Meta Encoding

FACING uses 2 bits (0-3), STATE uses 2 bits (0-3). Combined: `facing + state * 4`. Fits in
4-bit meta.

### Light Emission

```java
@Override
public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    int stateValue = state.getValue(STATE);
    if (stateValue == STATE_MAN_ON || stateValue == STATE_RS_ON) {
        return 15;  // Maximum light
    }
    return 0;
}
```

### Common Overrides

All lights share:
- `getBlockConnectsRedstone()` -> `true`
- `getBlockIsOpaqueCube()` -> `false`
- `getBlockIsFullCube()` -> `false`
- `getBlockRenderLayer()` -> `BlockRenderLayer.CUTOUT_MIPPED`

Each concrete light only needs to override:
- `getBlockRegistryName()` -- unique ID
- `getBlockBoundingBox()` -- fixture dimensions

## AbstractBrightLightPoleColored

**Extends:** `AbstractBrightLight`

Adds a `COLOR` property that auto-detects the traffic pole below and inherits its color.
Used for pole-mounted fixtures that should visually match the pole they're attached to.

Provides `TRAFFIC_POLE_COLOR` enum: BLACK, SILVER, TAN, WHITE, UNPAINTED.

## Adding a New Light Fixture

1. Create a class extending `AbstractBrightLight` (or `AbstractBrightLightPoleColored` for
   pole-mounted lights):

```java
public class BlockMyLight extends AbstractBrightLight {
    @Override
    public String getBlockRegistryName() { return "my_light"; }

    @Override
    public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0.25, 0.8, 0.25, 0.75, 1.0, 0.75);
    }

    // Optional: override for pole-mounted lights
    @Override
    public int getBrightLightXOffset() { return 0; }
}
```

2. Create blockstate JSON with `facing` (4 dirs) and `state` (0-3) variants
3. Create model/texture per standard block process
4. Add lang entry: `tile.my_light.name=My Light Fixture`
5. Register in `CsmTabLighting` via `initTabBlock(BlockMyLight.class, event)`

The 4-state control, redstone handling, and light-up air projection are all inherited
automatically.

## Light Fixture Categories

The mod includes fixtures modeled after real manufacturers:

| Prefix | Manufacturer/Style | Example |
|---|---|---|
| `BlockAE*` | Acuity Brands | AE1, AE2 |
| `BlockAlto*` | Alto Lighting | AltoLEDway |
| `BlockCE*` | Cree Enterprises | CESquare, CEHalo |
| `BlockCI*` | Cree Industries | CILEO |
| `BlockCree*` | Cree LEDway | CreeLEDway |
| `BlockGE*` | General Electric | GEEL, GEELSmall |
| `BlockHB*` | High-bay | HB1, HBF1 |
| `BlockDS*` | Street/Daylight | DSH, DSHD |
| `BlockPost*` | Post-mounted | PostLight1-3 |
| `BlockPark*` | Park-style | ParkLight1-2 |
| `BlockClassic*` | Classic-style | ClassicLight1 |
| `BlockSears*` | Sears | SearsLight, SearsLightBig |

## The Decorative Family (Pendants and Sconces)

Eleven decorative fixtures — five ceiling pendants, five wall sconces and a mission flush mount —
plus a stackable chain, each in three metal finishes, for 36 blocks. Unlike the rest of the tab these are **OBJ models,
procedurally generated** by `dev-env-utils/scripts/gen_decorative_lighting.py`; run that script to
regenerate the geometry, the swatch textures and all 36 blockstates at once.

| Model | Registry stem | Notes |
|---|---|---|
| `pendant_schoolhouse` | `pendschoolhouse` | Opal bell; the whole shade is the lens |
| `pendant_industrial_dome` | `penddome` | Enamel-lined dome over a glowing bulb |
| `pendant_cone` | `pendcone` | Modern metal cone |
| `pendant_caged_edison` | `pendcaged` | Teardrop bulb in a six-wire cage |
| `pendant_globe` | `pendglobe` | Opal sphere truncated to meet its fitter |
| `sconce_halfshell` | `sconcehalfshell` | Frosted half-bowl with a metal rim band |
| `sconce_colonial` | `sconcecolonial` | Candle and flame on a shaped backplate |
| `sconce_vanity_bar` | `sconcevanity` | Three globes on a wide bar |
| `sconce_carriage_lantern` | `sconcelantern` | Square glass lantern under a tapered cap |
| `sconce_rlm_gooseneck` | `sconcerlm` | Barn shade on a jointed gooseneck arm |
| `ceiling_mission` | `ceilmission` | Mission / craftsman square flush mount: amber art glass set back in a square frame, a strap down each side and a cross underneath; no `shade` material |
| `pendant_chain` | `pendchain` | Stackable; links overhang so a stack reads continuous |

Finish suffixes are `black`, `bronze`, `nickel` — e.g. `pendglobebronze`.

### Three materials per model

This is what lets one OBJ serve three finishes *and* both lit states, and it is the pattern to
copy for any new decorative fixture:

| MTL material | Overridden by | Purpose |
|---|---|---|
| `body` | the finish variant's blockstate | the metal — `finish_black` / `finish_bronze` / `finish_nickel` |
| `shade` | nothing | constant secondary surface: enamel liner, candle wax |
| `lens` | each STATE variant | the part that lights up |

Forge's `OBJModel.retexture` keys overrides on `#` + the MTL material name, so a blockstate says
`"textures": {"#body": "...", "#shade": "...", "#lens": "..."}`. Overrides are re-collected after
`Variant.process`, so they stitch into the atlas normally.

### These fixtures actually look off when they are off

Every other fixture in the tab renders identically in all four STATE values — the blockstate lists
`"state": {"0": {}, "1": {}, "2": {}, "3": {}}` and one texture covers all of them. The decorative
family instead overrides `#lens` per state: STATE 1 and 3 (redstone-on and manual-on) get
`lens_*_on`, STATE 0 and 2 get `lens_*_off`. Three lens families exist, `lens_opal_*` for glass
shades, `lens_bulb_*` for exposed bulbs and `lens_amber_*` for the mission fixture's art glass.

The older JSON fixtures do this too, by a different route — they wear atlas textures with the
lamp painted in, so they swap the whole atlas per state rather than a `#lens` material. Today that
is the troffers, the strip lights and the two MCLA post-top lanterns; the street lights are OBJ
models since #99 (see [OBJ Roadway Fixtures](#obj-roadway-fixtures)). See
[Lit and Unlit Textures on the Older Fixtures](#lit-and-unlit-textures-on-the-older-fixtures).

### Authoring notes

- **Swatches are vertical gradients, and every lathe maps `v` along the profile from neck to
  mouth.** A shade therefore comes out dimmer at its fitter and brightest at its opening with no
  per-fixture UV work. Order a profile neck-first or the gradient runs backwards.
- **A cap faces away from the rest of the profile, which is not always along the axis.** Shades
  descend, so their mouth caps face *down*. The generator derives this from the profile slope;
  hard-coding `+axis` renders every mouth cap inside-out.
- **A flat ring is not a zero-height lathe.** A lathe chooses its winding from a radial outward
  vector, which lies *in* the plane of a flat disc and so decides nothing. Use `disc()` with an
  explicit facing.
- **Wall fixtures put their back plate at z = 16.** `AbstractBlockRotatableNSEW.getStateForPlacement`
  sets FACING to the placer's horizontal facing *opposite*, so a plate at high z ends up against
  the wall the player was looking at. This matches `altomvwl`.
- **Nothing may sit exactly on a block boundary and face back into the block.** A surface drawn on
  z = 16 is at precisely the depth of the neighbouring block's own inward face, which is also
  drawn, and the depth buffer cannot separate them — it shimmers. Wall fixtures lathe about
  `WALL_STANDOFF` (15.9) rather than 16. The same applies to one part landing on another's face:
  a boss sitting on the plate behind it needs `COPLANAR_NUDGE`. A face on the boundary pointing
  *out* of the block is culled and harmless.
- **A lathe's outward reference must lean the way the profile leans.** A purely radial one is
  perpendicular to the true normal wherever the profile runs flat — the top of a dome, the lip of
  a bowl — so the winding test there turns on a dot product near zero and gets it wrong half the
  time. Those faces then cull from outside and the shade has a hole in it viewed from above.
  Which of the two perpendiculars is outward depends on how the profile was authored, so it is
  settled once per surface, not per band: deciding per band breaks any profile that curves back
  on itself, which is every torus.
- **A jointed arm is one swept tube, not a row of struts.** Independent boxes meeting at an angle
  interpenetrate — coplanar overlapping faces, so z-fighting — while the wedge on the outside of
  the bend is an open hole. `sweep()` mitres the cross-section at each joint instead.
- **Run `python dev-env-utils/scripts/audit_obj_models.py` after regenerating.** It finds all of
  the above mechanically: coplanar overlaps, faces on block boundaries, and inconsistent winding.
  Every one of those rules is there because the audit caught a real instance of it.
- **Inventory transforms use `forge:default-block`.** These models are authored in 0..1 space with
  the block's min corner at the origin, which the preset frames correctly. Do not hand-write a
  transform dict without checking units first — blockstate translations are in *blocks*, not the
  1/16 of a vanilla item model's `display`, so `[0, -1.5, 0]` throws the icon off-screen and the
  slot renders empty with nothing logged.

### The two Commercial Electric flush mounts

`cehalo` and `cesquare` are generated by the same script even though they are not part of the
decorative lineup and have no finish variants. They were repaired rather than added.

They shipped as JSON models spanning **y 15.95 to 16.24**. A block is y 0 to 16, so 0.24 of their
0.29 units of depth — 83% — sat in the block *above*, which for a ceiling light is the ceiling:
buried and invisible. What remained inside their own block was 0.05 units, a twentieth of a
texture pixel. Their bounding boxes agreed and were equally wrong (`y 1.0 → 1.1`, entirely in the
neighbour), so you had to aim at the ceiling block to select the light.

The geometry itself was fine — the halo is a proper 16-element hexadecagon ring. It was purely a
depth and placement fault, and the likely cause is authoring as if the block's top face were the
mounting surface with the body above it, which is one block too high. Nothing errors when this
happens, which is why it survived.

**These are recessed downlights, so the fix was depth and placement, not bulk.** A real one is a
thin trim plate almost flush with the ceiling, with the lamp up inside the ceiling cavity. A block
model cannot occupy the ceiling block, so the recess is faked the only way it can be: the trim is
**one texture pixel deep** (y 15.0 → 16.0) and the lens sits *higher* than the trim's bottom face,
so you look up into a shallow well. Bounding boxes are `y 0.9375 → 1.0`. The two orphaned JSON
models were deleted.

### Known limitations

- The lens texture swaps, but nothing renders **emissive** — `AbstractBrightLight` is
  `CUTOUT_MIPPED` and there is no TESR, so at night a lit fixture's own body is still shaded by
  ambient light. The lens reads brighter, but it does not glow.
- A pendant hung below a `pendchain` still shows its ceiling canopy mid-air. It reads acceptably
  as chain hardware; removing it would need a canopy-less model variant selected from the block
  above.

## Modern Troffers

`dev-env-utils/scripts/gen_troffers.py` writes the lay-in troffers added in 2026-10 and the
rebuilt Lithonia 2ESL2 / 2ESL4. All are `BlockBrightLightFactory` lights sitting at the bottom of
their ceiling cell, so the face lies in the plane of the full-block ceilings beside them; a 2x4
reaches a block north, as the GTL does.

| Registry | What |
|---|---|
| `troffer_slot_2x2`, `_2x4` | a white face with a narrow louvered slot down its length |
| `troffer_basket_2x2`, `_2x4` | angled white reflectors (elements turned 22.5 degrees) either side of a raised lens |
| `troffer_pattern_<bars|dashes|squares|dots>_2x2`, `_2x4` | a white face cut with a pattern of openings over the lens: one cutout picture drawn as tall as the fixture and cut into a square sprite a block |
| `l2esl2`, `l2esl4` | the Lithonia 2ESL, rebuilt: its housing, two lamps and reflectors where the old hand-made model had them, the lamps now the lit lens, its white louvers replaced by silver mirror blades |

No edge-lit flat panel (the GTL is one) and no parabolic of their own (the 2ESL is one).

**The lens is drawn unshaded** (`"shade": false`). A down face's directional shading halves it, so
a lit lens drawn shaded reads grey; the unlit lens (`troffer_lens_off`) is drawn at about half the
white so an off troffer matches the shaded ceiling round it. Lit and unlit are a `#lens` swap per
STATE, as the decorative family does. **States 0 and 2 name the unlit lens explicitly** rather
than inheriting it: `gen_lighting_lit_atlases.py` takes any blockstate whose 0 and 2 are empty for
its own, and these are not its.

## Linear LED Lights

`BlockLinearLight` (`gen_linear_lights.py`): the slim linear fixture of a modern open office, one
block a segment, joined into one fixture along a run. Twenty blocks, `linear_light_<finish>`
(opal lens, a continuous lit strip) and `linear_light_louver_<finish>` (white cross blades in
front of the lens), in the ten-finish palette the hanging acoustic panels share: black, silver,
white; light grey, charcoal; navy, sage, terracotta; oak, walnut. The palette's textures live in
Core (`blocks/finishes/finish_<name>`), since Lighting and Building both draw with them.

**Stored** is only what every bright light stores, facing and STATE (the metadata is full).
**Actual state** does the rest: `front` / `back`, whether a segment on the same axis at the same
height continues the run that way (an end cap where it does not); `hung`, whether the block above
gives no solid underside (flush against the ceiling at y 13..16 when it does, hung at y 10..13
otherwise); `cable`, a hung segment's aircraft cable up out of its block at a run's ends, every
fourth block along it (by world position, so the spacing holds wherever a run starts) and under a
`BlockLinearLightHanger`. The hanger, `linear_light_hanger`, is a block of cable stacked under a
ceiling so a run hangs one or more blocks lower. 128 states a light.

**A run is one fixture.** A click switches every segment, walking the run up to 64 blocks each
way, and redstone powering any segment powers the whole run (each segment's `neighborChanged`
works out the run's power and sets the rest, in the automatic states only).
`AbstractBrightLight.handleAirLightBlock` became protected for this.

The blockstate is vanilla multipart, every part drawn for a run along z and turned by the facing
(the back cap a further half turn). Geometry is a handful of parents (`geo_body_*`, `geo_cap_*`,
the lens models); a finish is a child naming its housing texture. The lens is unshaded, as the
troffers' is, and in the lit louvered lens the blades are too, or they stay a shaded grey in
front of a lit lens.

## Tall Building Beacons (Aviation Obstruction Lights)

Two aviation obstruction beacons for the top of a tall building, a tower or a mast, both
`BlockObstructionBeacon`, with their models, textures and lang from
`dev-env-utils/scripts/gen_obstruction_beacons.py` (`--check`, `--fragments`):

| Block | Registry name | Models |
|---|---|---|
| Tall Building Beacon (Red, Flashing) | `csm:obstruction_beacon_red` | FAA L-864 style |
| Tall Building Beacon (White, Strobe) | `csm:obstruction_beacon_white` | FAA L-865 style |

**The timings are the FAA's.** FAA AC 150/5345-43J, Table 3-5 (Flash Characteristics for
Obstruction Lights):

- L-864 (red): 30 flashes a minute (±3), and, for a light that is not incandescent, a flash of
  100 to 1333 ms. The beacon flashes as the tower crane's L-864 lights do (CONSTRUCTION_SITE.md):
  a 100 ms rise, a 700 ms hold and a 100 ms fade, dark for the rest of the two-second cycle.
- L-865 (white): 40 flashes a minute (one every 1.5 s), a flash of under 100 ms by day and
  twilight. The strobe is 100 ms at full and a 60 ms fade, then dark to the end of the 1.5 s. A
  single 50 ms tick, the first version, was not seen in game at all, so it is as long as the
  standard allows and a little fade. (By night a real L-865 may run a longer, dimmer flash, 100
  to 1000 ms; a dual system hands the night to a red L-864 instead, which is the other block.)

**The flash is a renderer's.** `TileEntityObstructionBeaconRenderer` draws it, and only while
it is on: a fullbright sleeve just outside the lens, a small hot glow facing the camera, and a
wide soft bloom that grows as the surroundings darken (a quarter of it in full daylight). The
glow keeps a minimum size on screen, so a beacon on a roof a hundred blocks off still reads as a
point of light; the tile entity draws to 256 blocks. The timing comes from the wall clock, so
every beacon in view flashes in step. The models carry only the dark lens.

It was first the lens texture's animation (a three-frame strip timed by its `.mcmeta`, with `_e`
companions), which needs no tile entity. It did not work: a baked model is drawn at the light
around it, so the red flash read as a slightly lighter lens by day and dim by night, and the
strobe's one-tick frame was not seen at all. Brightness and glow need a renderer. The tile
entity holds nothing and never ticks, and the renderer returns at once between flashes.

**The flash is drawn from eight shared lists** (`CsmSharedDisplayLists`, `obstruction_beacon`):
each style's sleeve, hot sleeve, bloom and glow, compiled once. The sleeves sit in the block
under the beacon's translation; the bloom and glow are discs of unit radius under the
billboard's turn and a scale. What changes every frame, the flash's brightness, cannot ride on
`glColor`, since the vertex colour array overrides it, so it is the blend's constant alpha
(`glBlendColor`, `GL_CONSTANT_ALPHA`), and the disc vertices carry their colour premultiplied by
their ring's alpha: `colour * ringAlpha * flash` is added to the screen exactly as the live draw's
`SRC_ALPHA, ONE` added `colour * (flash * ringAlpha)` (`ObstructionBeaconListsTest`). Live, a lit
red beacon tessellated 784 vertices a frame for the 900 ms of every 2 s cycle, 17 us
time-averaged (43 us a lit frame); `/csm renderpass skip sharedBakesPerFrame` draws it live
again for an A/B. The blend colour is set back to GL's default `(0, 0, 0, 0)` after the glow.

A lit beacon gives a steady block light of 9 (the airside obstruction lights' level); light cannot
flash without ticking.

**Lit unless powered.** A real obstruction light burns all the time (the red one by night, from
its photocell), so a beacon is placed lit, and redstone power switches it off: a daylight sensor
beside a red beacon keeps it dark by day, as its photocell would. The rule reads the power as it
is, so nothing about the redstone is stored, and there is no click to switch it (a click would be
undone by the next neighbour update). This is simpler than the airside lights' circuits
(TRANSIT_SYSTEM.md), which an airfield switches together; tall buildings do not.

**Mounting.** Placed on top of a block (a roof, a parapet, a pole top) it stands on it
(`mount=floor`); it is an `ICsmPostTopFixture`, so a concrete pole under it shows its tenon.
Placed against the side of a block it stands on a steel shelf bracket off that face
(`mount=north|east|south|west`, the wall at +Z in the model, turned by the blockstate). Ten
states in all (`mount` x `lit`); the metadata holds both.

## OBJ Roadway Fixtures

The street lights are being rebuilt as OBJ models (GitHub #99), one generator for all of them:
`dev-env-utils/scripts/gen_streetlight_fixtures.py`. Today it writes the GE M-400A family, the fixtures converted from JSON, and the new fixtures marked **New** below, drawn from the makers' spec sheets:
the roadway heads, and with them the Alto lot, wall and underpass lights and the GE Powerbracket, which share their materials and conventions:

**His undersides are kept texel for texel (2026-11, the requester's review).** The LED fixtures
converted from his JSON models first wore his LED art as a cut shrunk to a 32 px sprite and
stretched over a panel of another size, and the door art under the rear was dropped. Now each art
face of an old model is described in `UNDERSIDE` as the JSON described it (its rectangle, its `uv`
in texels of his 128 px sheet, its `rotation`), cut from the sheet at its own resolution into the
corner of a square sprite (`under_cut`, `under_sprites`), and hung under the housing as a slab whose
underside is mapped by the game's own rule for a down face (`under_face`). Nothing is resampled or
turned. Where the lofted housing is narrower than his box was, the slab is trimmed to the
housing's outline and the mapping does not move. A `lens` face is drawn unshaded (unlit: halved;
lit: the texels his lit sheet changed left as painted, the housing round them halved); a `door`
face is on a fourth, shaded material, `door`. Sprites are `streetlight_under_<sheet>_on/_off/_door`
and two sheets with the same texels share one, so most Leoteks and the ATB0 share a door. His
sheets are in `dev-env-utils/scripts/artwork/streetlight/sheets/`. The rows below that name
`streetlight_lens_<art>` sprites for an LED head predate this: read `streetlight_under_<sheet>`.
The check is a picture, old model beside new from below; `--fragments <registry>...` prints a
fixture's tab line (its bounding box measured off the model) and lang line.

**An empty receptacle shows.** An HID head sold without a photocell (`np`) carries the collar and
the dark socket (`receptacle`); only the Form 109, the OV-20 and the finned OV-50, made before
twist-lock receptacles, have a bare hood.

| Registry name | Fixture |
|---|---|
| `gem400a` | GE M-400A, drop refractor |
| `gem400acu` | GE M-400A, flat cutoff glass |
| `gem400acunp` | GE M-400A, flat cutoff glass, no photocell |
| `gem400acugreen` | GE M-400A, flat cutoff glass, in City of Anaheim P7-Green: the cobra head for the Anaheim Resort Area pole and arm |
| `gem400r3`, `gem400r3cu`, `gem400r3cunp` | GE M400R3 refractor, cutoff, cutoff without photocell: **converted in place** from JSON |
| `gem240r1`, `gem240r1np` | GE M-250R (CSM's "M240R1"), the long early body, drop refractor, with and without photocell: converted in place |
| `gem240r2`, `gem240r2cu` | GE M-250R2 (CSM's "M240R2"), the short body, drop refractor or flat cutoff glass (his M240 lens art): converted in place |
| `geform109` | GE Form 109, the 1950s-60s mercury fixture: a short high-domed hood over a drop refractor in his olive: converted in place |
| `geform109td` | GE Form 109 teardrop: a lathed pendant, domed hood with finial over a pointed teardrop refractor, on a short neck: converted in place, the one fixture outside the cobra-head envelope (`ENVELOPE_FOR`: its own bounding box, x 3..13, y -7.5..6.25) |
| `whov20` | Westinghouse OV-20, the small residential head (z 7..16) over a drop refractor: converted in place |
| `whov50`, `whov50np` | Westinghouse OV-50, the long 1960s-80s cobra head over a deep drop refractor, with and without photocell: converted in place |
| `whov50finned` | Westinghouse OV-50 with six cooling fins across the hood over the ballast, no photocell (as the JSON had it): converted in place |
| `whov50shaded`, `whov50shadednp` | Westinghouse OV-50 with a round glare shield, a 12 px disc with a skirt round the refractor, with and without photocell: converted in place; the disc is wider than the envelope, so `ENVELOPE_FOR` holds their bounding box (x 2..14) |
| `ae115`, `ae115cu` | American Electric 115, a tall boxy 1970s-90s head on a narrower neck, drop refractor or flat cutoff glass (his framed lens panel, the same one the M240R2 cutoff wears): converted in place |
| `aeatb0`, `aeatb2` | American Electric Autobahn ATB0 / ATB2, low-profile LED roadway heads: a flat underside carrying his LED array art (`streetlight_lens_ael_atb0/2`), a smooth hood thickening toward the slipfitter; the ATB2 wider and flatter, its photocell ahead of the slipfitter rise to stay in its 4 px tall box. Converted in place; their boxes (x 4..12 / 3..13, z 0..16) are their envelopes |
| `ltec`, `ltecdtd` | Leotek E-Cobra, a slim flat-bottomed LED head: a 1 px slab carrying his LED array art (`streetlight_lens_leotek_ec`), the driver housing rising behind it to the slipfitter; the Dusk to Dawn is the short body (from z 5) with the built-in sensor's eye under the rear (`streetlight_lens_leotek_ecdtd`). Converted in place; their 8 px wide boxes (x 4..12, the E-Cobra's from z 0) are their envelopes |
| `ltgc1v1`, `ltgc1v1np`, `ltgc1v2` | Leotek GreenCobra GC1: a finned slab over the LED array and a tall (5 px) driver housing; v1 and v2 differ only in his array art (`streetlight_lens_leotek_gc1v1/gc1v2`), `np` has a blank cap in the receptacle. Converted in place; x 4..12 boxes are their envelopes |
| `ltgcj`, `ltgcjsn` | Leotek GreenCobra GCJ, the 4 px wide junior with fins: the squarer Leotek section (`LEOTEK_EXPONENT`) is what lets its neck swallow the 3 px arm; `sn` carries a smart-city node, a blue puck on the photocell material (`PHOTOCELL_FOR`, `streetlight_smartnode`), in place of the photocell. Converted in place |
| `ltgcl`, `ltgcm`, `ltgcmsn`, `ltgcmv2`, `ltgcmv2np` | Leotek GreenCobra GCL (x 4..12, two arrays side by side) and GCM (x 5..11): one smooth body at two widths, a slab over the array and a 3 px driver housing; GCM v2 wears his two-panel art, `sn` the smart node, `np` the blank cap. Converted in place; the GCL's box is its envelope. The Leotek cuts are turned to lie as his JSON faces drew them (`ART_TURN`) |
| `geel`, `geelpo`, `geelsn`, `gees`, `geespo`, `geesn` | GE Evolve ERL2 (Large, from z 7) and ERL1 (Small, from z 9), from GE's dimensioned sheet (OLP3128): a wide thin deck, flat underneath, and along its rear a raised narrow spine (the tunnel the arm enters) ending forward in the rounded hump that carries the receptacle. Four low ribs cross the ERL1's deck over its one module; a row of tall fin blades, taller down the middle, crosses the front of the ERL2 over its two. The underside is his one face, louvred door and modules (`UNDERSIDE`). `po` is a raised photocell outlet, `sn` a taller dark smart node. They were labelled ESR2 and drawn as a slab with a hump, which is a Leotek's shape, until the requester's review. |
| `creeledway`, `creeledwaysmall` | Cree LEDway: a thin LED panel out front, its underside at y 2 (his array art, `streetlight_lens_cree_ledway/_small`), on a boxy 8 px driver housing carrying the photocell; the Small's panel is half as long (z 5..10). Converted in place; their x 4..12 boxes are their envelopes |
| `creexsp`, `creexspsingle` | Cree XSP: a broad flat LED head from z 2, its top stepping up toward the slipfitter. His one underside face is the whole bottom (two modules side by side, the door behind), stepped down under the neck. The single-module XSP is the same head 16/30 as wide, wearing one of his two modules (`SHEET_DERIVED`: the columns of his sheet that hold it). |
| `creestylusfull`, `creestylushalf` | Cree Stylus: a linear LED fixture hung flush under a ceiling, not from an arm. A rounded bar 0.1 off the top of the block over a diffuser in his troffer white (`streetlight_lens_cree_stylus`, cut from `troffers`, which stays: the troffers use it); the Full runs on to z -16, one fixture two blocks long, as its JSON did. Converted in place; their boxes (y 11..16) are their envelopes |
| `cinavion`, `cinavionalt` | Cooper Industries Navion: an open frame ahead of the driver housing, three rails closed by posts at the nose over a flat panel carrying his LightSquare art (`streetlight_lens_ci_navion`, two squares; `_alt`, three, the panel running on to z -4). Boxy, as the JSON was; the housing a squared loft round the arm's end. Converted in place; their boxes (from z 0 / -4) are their envelopes |
| `smsmx` | Solarmax SMX (the first generation): a broad flat head 10 px wide with its corners cut, 2 px thick at y -1..1, on a neck rising over it to the slipfitter, his LED panel (`streetlight_lens_solarmax_smx`) under the front half, in a new warm grey swatch. Converted in place; its box (x 3..13, y -1..5) is its envelope |
| `altorll`, `altorell`, `altosqll` | Alto lot lights: a squared shoebox in his `metal_silver` (a new `streetlight_body_alto_silver` swatch) over flat glass in his `merc_fixtures` olive, on a neck that swallows the arm; the square one's body starts at z 6. They hang from the Alto lot light mount (`altollm`) or any arm. The "Round" lot light's blockstate had always drawn the rectangle model; it is now round, as its never-used JSON (`alto_round_lot_light.json`, since deleted) drew it: a 16-sided disc 10 px across over a round lens, its bounding box widened to x 3..13, y -0.5..4.75, from z 2 (the user's choice). Converted in place; the rectangle and square keep their boxes (x 5..11, y 0..4, from z 3 / 6), and each box is its envelope |
| `altomvwl`, `altomvwlslim` | Alto mercury vapour wall lights: a rounded back box against the wall, its back at 15.9, and the olive glass standing proud of its face, x 5.5..11.5 (slim 6.5..10.5) as the JSON placed them. Converted in place; their boxes (y 4..12, z 13..16) are their envelopes |
| `altomvul` | Alto mercury vapour underpass light: a ceiling fixture, a round canopy 10 px across 0.1 under the ceiling over a lathed drop bowl in his olive (the JSON's four steps as one bowl), the facets turned half a step so no seam lies on an axis. Converted in place; its box (x and z 3..13, y 9.8..16) is its envelope |
| `gepb` | GE Powerbracket: a wall plate at the back of its block, a cast arm climbing at 22.5 degrees (as the JSON tilted it) to a ballast housing on a round hood 12 px across centred 10 px into the block in front, over a globe of his pale 182 glass with his olive lamp showing through the bottom (`streetlight_lens_powerbracket_on/_off`), the photocell square to the arm. Converted in place; its two-block box (x 2..14, y 0..12, z -16..18) is its envelope, so the housing tops out at 11.95 where the JSON's reached 12.4 |
| `ltkc0`, `ltkc1`, `ltkc2` (each also `hss`) | Leotek KarbonCobra KC0 / KC1 / KC2, from Leotek's sheets (15.9 / 18.0 / 24.4 in long, 7.6 / 9.0 / 10.8 in wide, 4.1 in tall), drawn at about 1.75 in to the pixel (9.5 / 11 / 14 px): a round-nosed paddle pinched at a waist, the hood a low wedge rising to the driver hump, the receptacle on a boss. Its light engine is the GreenCobra's, so the KC0 and KC1 wear his GCJ module (one) and the KC2 his GCM version 2 pair, at the size he drew them, with his door behind (`KARBON`). `hss` adds the house side shield. |
| `aeacmdp` | AEL AutoConnect ACM (24.6 x 10 x 4.1 in) as the Los Angeles Department of Water and Power hangs it in the Owens Valley: a teardrop in plan, the hood swelling over the array and higher over the driver, no receptacle on the hood; the photocell is a short black puck on the door, under the rear (AEL's "bottom" control position). It replaced `aeacm` and `aeacmnp`, which the requester asked to have removed. |
| `gem250a`, `gem250acu`, `gem250acunp` | **New:** the original GE M-250A (about 27.5 x 11.5 in), the M-400A's smaller sibling: drop refractor (his ribbed orange), flat cutoff glass (his M240R2 cutoff panel), and cutoff without a photocell. Its rear is the M-400A's. The `gem240*` blocks (GE's later M-250R / R2) are unchanged |
| `gem400`, `gem400gs`, `gem400cu` | The original GE M-400 (1959 to about 1970), in bare aluminium: the upper shell is a brim that overhangs the door all round and stands out at the nose as a brow, its top nearly straight to a peak over the optics; a one-piece door under it, a deep oval bowl, the receptacle mid-length. Two lofts, brim and door. Mercury olive glass. Each of the three M400 generations comes with a drop refractor, the refractor inside a glare shield (`gs`: a skirt round the bowl, `glare_shield`) and flat cutoff glass (`cu`). |
| `gem400r1`, `gem400r1gs`, `gem400r1cu` | "M400R1": GE made no fixture of that name (the R1 suffix is the M-250's); this is the 1970-85 M-400 that shared the M-400A's shell with one plain door, so it is `build_m400a` without the Powr/Door's second door. Ask the requester for a photo if he means something else. |
| `gem400r2`, `gem400r2gs`, `gem400r2cu` | GE M-400R2 (1986-97): a smooth low-arched shell, longer and narrower than the M-400A (35.5 x 13.75 in), over a one-piece door that is flat underneath with straight sides tapering to the rear; a bail latch at the nose, the receptacle about 40% from the rear. |
| `gem240r1cu`, `gem240r1cunp` | GE M240R1 with flat cutoff glass, with and without a photocell: the R1 body, the M240R2 cutoff's framed glass. |
| `whov25`, `whov25hps`, `whov25early` | **New:** Westinghouse OV-25 "Silverliner", the first cobra head (later sold as Crouse-Hinds): a long cast lid rounded down over a deep round refractor, a narrower ballast wedge under the rear whose underside climbs to the slipfitter; the mid-1960s body in mercury (his olive) and HPS (his orange), and the squarer, flat-lidded 1950s body in mercury. Bare aluminium swatch (`streetlight_body_westinghouse_silver`) |
| `whl250`, `whl250hps` | **New:** Westinghouse L-250 (Crouse-Hinds after 1982): a domed lid over a deep refractor and a squarer, finned ballast housing behind it; mercury and HPS |
| `smsmxll5` | **New:** SolarMax SMX-LL5, the second-generation SolarMax the requester confirmed, a new block beside `smsmx` (unchanged): a thin round-nosed paddle 8 px wide with the LED module under its front half, its top running flat into a deep square driver box, the blue photocell (`PHOTOCELL_FOR`, the smart node's blue) at the box's front edge. Near-white swatch (`streetlight_body_solarmax_white`); its box (x 4..12) is its envelope |
| `geeals`, `geealsarm`, `geealswall`, `geealspt` | **New:** GE Evolve EALS area light (25.2 x 14.7 in, 4.3 in over the fins): a square head with a square LED array and five fins rising toward the rear, on a tapering neck, in GE's C1 slipfitter (any CSM arm), D1 arm mount (a short arm to a pole plate on the block face), V1 wall knuckle (a hub off a wall plate) and S1 pole-top knuckle (a sleeve over a tenon, the head cantilevered north to z -5.3; a `BlockBrightLightFactory.PostTopAimed`: its light falls one block ahead, under the head, whichever way it faces). Black swatch (`streetlight_body_black`) |
| `hpmongoose` | **New:** Holophane Mongoose LED (40.8 x 17.7 x 6.6 in): a long flat head with a crenellated rim of cooling teeth along both top edges and big round optics under it, the driver in a deeper housing over the slipfitter. Longer than a block: the head runs on to z -2 (`ENVELOPE_FOR`). Dark bronze swatch (`streetlight_body_dark_bronze`) |

**Converting a JSON fixture** keeps its registry name, class, bounding box and STATE; only its
blockstate changes hands, from a hand-written one to the generator's, so placed blocks and
`/setblock` scripts keep working. The JSON models it used are deleted once nothing names them.
The requester's lens art is kept: the framed lens from the fixture's old atlas is cut once into
`dev-env-utils/scripts/artwork/streetlight/` and the generator makes a lens sprite of it.

They are ordinary `BlockBrightLightFactory` blocks (4-state STATE, light-up air below); only the
model is new. A JSON cobra head is a stack of boxes; these are lofted bodies, a superellipse
section swept along a profile table from the nose to the slipfitter. They are baked into the chunk
mesh like any JSON model: no tile entity, no renderer.

**The envelope.** A fixture faces north with its slipfitter at z = 16, where every CSM arm ends
(the NOV tapered masts, the pole light mount, the SCE mounts, the mast arm curve tips), and stays
inside the JSON cobra heads' footprint (x 5..11, z 1..16), so a JSON fixture's blockstate can later
point at one of these without moving a bounding box or an arm. The generator asserts both: every
vertex inside the envelope, and the arm's 3 x 3 px end (`ARM_TIP`) inside the rear section, so no
arm corner stands proud of the neck. A converted fixture whose JSON bounding box was bigger
(the Form 109 teardrop) gets that box as its envelope in `ENVELOPE_FOR`. The Cree Stylus and the Alto underpass light hang from the
ceiling, the Alto wall lights and the Powerbracket from a wall, so they have no neck to check; their envelopes are their boxes. Nothing sits on z = 16 facing back into the block; the rear
cap is at 15.9.

**Materials.** One `streetlight.mtl` for every fixture, retextured per blockstate:

| Material | Texture |
|---|---|
| `body` | the housing paint, a 16 px swatch (`streetlight_body_*`) |
| `unshaded_lens` | flat glass: the requester's lens art (`streetlight_lens_<art>_on/_off`) where the JSON fixture had some, else `streetlight_lens_hps_on/_off`; a drop refractor: `streetlight_lens_refractor_on` (his refractor orange, ribbed as his staircase showed it) and `_off` (pale prismatic glass); the mercury Form 109 and the Westinghouse OV-20 / OV-50 wear `streetlight_lens_refractor_merc_on`, his `merc_fixtures` olive (the same olive his `westinghouse_ov50` sheet painted) raised to lamp brightness and ribbed the same way, as does the Alto underpass bowl; the Alto lot and wall lights' flat glass wears the same olive unribbed (`streetlight_lens_merc_glass_on/_off`, made from his colour: `merc_fixtures` is four flat swatches with no framed lens art to cut); an LED head (the Autobahns, the Leoteks, the Evolves, the Crees, the Navions, the SMX) wears his array art the same way as lens art (lit dies white, unlit dies dark on a pale optic); the new fixtures, which had no JSON art to cut, wear LED arrays the generator draws (`GENERATED_LENS`, `led_array_png`: square dies, the ACM and SMX-LL5's amber phosphor, or the area lights' round optics), 32 px each. States 0 and 2 name the unlit lens explicitly, so `gen_lighting_lit_atlases.py` leaves these blockstates alone |
| `photocell` | the twist-lock photocontrol |

**The lens is drawn unshaded.** `CsmObjModelLoader` bakes any OBJ material whose name starts with
`unshaded_` (`CsmObjModelLoader.UNSHADED_PREFIX`) without directional shading, as a JSON element
with `"shade": false` is drawn. Forge's OBJ loader keeps no custom data of its own, so the material
name is the only thing that reaches the bake. Without it a flat lens, which faces straight down, is
drawn at half brightness, and a lit lens seen from the road could be no brighter than a dull tan
whatever its texture. Because nothing shades it, the unlit textures are painted at about half, to
sit beside the shaded housing (the troffers' rule). The prefix is opt-in: the many existing OBJ
materials called plain `lens` (pendants, cameras) are unchanged. With the part bake cache disabled
(`-Dcsm.noPartBakeCache=true`), Forge's own loader bakes CSM's OBJs and the lens is shaded again.

**Adding a fixture:** a profile table and a builder in the generator (`nose_round` rounds a nose in plan, `stations` writes profile rows by fraction of the length), a `CATALOGUE` entry, its lens in `LENS_FOR` (a `GENERATED_LENS` array if it has no art), then
the usual tab line (bounding box from the generated geometry), lang in four locales and
`gen_wiki_reference.py`. Run `audit_obj_models.py` on the new OBJ: winding, coplanar faces,
faces on the block boundary and see-through faces should all be 0.

## Floodlights

Fifteen floods (GitHub #99), each an OBJ written by `dev-env-utils/scripts/gen_floodlights.py`
(`--check`, `--fragments`) from the makers' sheets, at the #99 fixtures' 1.75 in to the pixel:

| Family | Registry names (knuckle, yoke, slipfitter) | Head | Beam reach |
|---|---|---|---|
| Lithonia D-Series DSXF1 LED | `lithdsxf1kn`, `lithdsxf1yk`, `lithdsxf1sf` | 8-7/8 x 8-1/2 x 3-1/8 in; two COB engines in a window over a finned driver housing | 16 |
| Lithonia D-Series DSXF2 LED | `lithdsxf2kn`, `lithdsxf2yk`, `lithdsxf2sf` | 12-7/8 x 7-3/4 x 3-1/8 in, landscape; four engines | 20 |
| Lithonia D-Series DSXF3 LED | `lithdsxf3kn`, `lithdsxf3yk`, `lithdsxf3sf` | 13 x 13-5/8 x 5 in; six engines | 24 |
| Lithonia TFL (metal halide) | `lithtflkn`, `lithtflyk`, `lithtflsf` | 16 x 15-3/4 x 6 in; a door frame round the glass over the lamp and reflector, ribbed ballast housing | 24 |
| GE Evolve EFM LED | `geefmkn`, `geefmyk`, `geefmsf` | ~16.4 x 15.7 x 5.5 in; a grid of LED optics, a finned heat sink behind | 28 |

They are `BlockFloodlight`, an `AbstractBrightLight`: the 4-state STATE, redstone, the click
cycle, the random-tick heal and `/csmlighting relight` are all inherited. What is new is where the
light goes and how a flood is placed on any face.

### Placed on any face: one block per surface

Metadata holds facing (4) x STATE (4), which is full, so a flood's six ways onto a block are
separate blocks, as vanilla's standing and wall signs are. The visible block (in the Lighting tab,
after the area lights) is the one against a wall, a pole's side or an arm's end; its item's
`getStateForPlacement` puts down a companion for the other faces, registered in
`CsmTabLightingHidden`:

| Block | Placed | Aim (`BlockFloodlight.Surface`) |
|---|---|---|
| `<name>` | against the side of a block, the plate on it | out from it, 35 degrees down |
| `<name>_g` | on top of a block; on a knuckle or yoke a ground uplight, on a slipfitter a pole top (`BlockFloodlight.PostTop`, an `ICsmPostTopFixture`) | the way the placer looks; 50 degrees up (uplight) or 35 down (pole top) |
| `<name>_c` | under a block (knuckle and yoke only) | the way the placer looks, 50 degrees down |
| `<name>_x` | against the front of a RealGrid crossarm (knuckle and yoke) | as on a wall |

A companion carries its flood's name (its own lang lines, the same text), drops and is picked as
the visible block, and finds its siblings by name at placement time, so the two tabs' load order
does not matter. 50 blocks in all, 15 visible. Every model faces north with its back toward
z = 16, as every CSM fixture does; the ceiling model is the ground one turned 180 degrees about z,
so it still aims north. The heads are drawn level in their own frame and turned to their aim about
the knuckle's hub or the yoke's trunnion bolts, so the aim the model shows is the aim the light
uses. 288 to 406 triangles each; `audit_obj_models.py` clean.

**Materials.** `floodlight.mtl`: `body` (the street lights' swatches: dark bronze for Lithonia,
black for GE) and `unshaded_lens` (drawn unshaded, as the roadway fixtures' glass is), the lens
`floodlight_lens_<head>_on` in STATE 1 and 3 and `_off` in 0 and 2. Ten 32 px lens sprites (the
D-Series COB windows and the TFL's lamp drawn by the generator; the EFM's optics by
`gen_streetlight_fixtures.led_array_png`).

**"Shaders".** Each lit lens has an OptiFine `_e` emissive companion holding only its LEDs or
lamp (`emissive_of`, every channel at 200 or more), so with OptiFine and a shader pack the lamps
glow and the trim does not; without OptiFine the `_e` sprites are never stitched. No renderer.

### The light: a beam, laid along the floor

Lit, a flood casts a beam from its centre along its aim (Core's `CsmLightupAir.projectBeam`), the
column projection generalised from straight down to any direction:

1. `beamPath` walks every cell the line enters (Amanatides and Woo), passing through air and
   light-up air, until it meets anything else or runs the flood's reach. A beam that runs out of
   reach, leaves the world or reaches an unloaded chunk lights nothing (a beam is never half-lit;
   an uplight with open sky above it lights only its own cell).
2. `beamTargets`: the last cell, the one just in front of what the beam hits, then a cell every
   `BEAM_SPACING` (4) blocks back along the beam, `BEAM_COUNT` (3) in all, none within 2 blocks of
   the flood.
3. `beamLights`: on a beam aimed below level, the row behind the lit spot `settle`s down its
   column onto the floor under the beam (stopping on a light-up air already there). The lit spot
   stays where the beam lands, so a wall it hits is lit where it is hit. An up-aimed beam keeps its
   row on the beam.
4. On, light-up air goes in each of those cells that is still air; off, every light-up air on the
   path is removed, and the first down the column under each row cell.

All on a state change and from the heal only: nothing ticks. The beam is recomputed each time,
so a flood whose beam is blocked later lights where it now lands at its next heal, as a street
light does; a light-up air left behind where the old beam fell (the floor raised under it, say) is
the same orphan a street light leaves, and the same fix applies.

**Why a floor row of three (plan decision D9), measured.** `CsmLightupAirBeamTest` builds a real
vanilla light engine on chunks in memory (`LightTestWorld`: a stone floor, a wall, sky light) and
switches fifty floods on and off together, 8 blocks above the floor on a wall, 6 apart, aimed 35 degrees
down. Server light engine only, best of 5, on the dev machine while other builds ran:

| Projection | Light-up air a flood | On, 50 floods | Off | Floor at light 8+ / 12+, a flood (pools overlap) |
|---|---|---|---|---|
| Street light column (baseline) | 1 | 137.6 ms (2.75 ms a flood) | 320.6 ms | 72.8 / 24.0 |
| Beam, 1 light | 1 | 128.0 ms (2.56) | 277.8 ms | 72.8 / 24.0 |
| Beam, floor row of 2 | 2 | 141.2 ms (2.82) | 361.5 ms | 97.4 / 43.0 |
| **Beam, floor row of 3 (chosen)** | **3** | **155.8 ms (3.12)** | **406.0 ms** | **111.8 / 59.1** |
| Immersive Engineering's 13-ray fan (a light every 3rd cell, reach 32) | 75.2 | 746.5 ms (14.93) | 1,772.5 ms | 209.7 / 150.9 |

Each time includes the flood's own lamp going to light 15. One flood alone, 10 blocks up: a single
light lights 113 floor cells to 8+ (25 to 12+), about a street light's pool; a row of three in the
air on the beam 145 / 29, barely more, because they hang above the floor; the same three settled on
the floor 216 / 65. So the row is laid on the floor: about twice the pool for 22% more time than a
single light, and the IE fan would cost 4.8 times the chosen row and 25 times the light-up air for
under twice its floor. Not measured here: the block-change packets and the client's re-mesh of each
16-block section a light change touches, which grow with the light-up air count (three a flood
here, usually in the same one to four sections).

**In game** (not yet done): fifty `lithdsxf3kn` on a wall, 8-10 up, on one lever, against fifty
street lights on the same lever; watch the server tick (`server_tick_stats` over MCMCP) and the
client's frame time on the toggle, both ways, a few times each.

**Sharing.** A flood's light-up air is ordinary light-up air. The stadium footprints' "is a
fixture still using this cell" check (`claimedBySource`) looks only up the column above a cell, so
it does not see a flood: if a flood's lit spot falls on a cell a stadium head placed, the head
turning off removes it and the flood relights it at its next heal. A flood turning off removes any
light-up air lying in its beam, whoever placed it; that light heals the same way.

### RealGrid crossarms (optional)

RealGrid (mod id `realgrid`) is an optional dependency, reached without linking to it
(`RealGridCompat`): only when `Loader.isModLoaded("realgrid")`, a block is a crossarm segment if its
registry name is `realgrid:crossarm_<material>` (not a brace, spacer or guy mount), and its front is
read from its `facing` property. Placing a knuckle or yoke flood against that front face puts down
its `_x` block: the wall model with a bracket reaching back 10 px to the arm, which RealGrid draws
as a 6 px square at the top back of its block (y 10..16; `BlockCrossarm.ARM_BOUNDS` in its source).
Without RealGrid nothing changes, and a flood placed on top of an arm is a ground flood. **Not
tested in game:** written against RealGrid's source, with no RealGrid jar in the dev run.

## Decorative and Historic Post Tops

The lanterns, globes and multi-lamp heads that sit on the decorative poles (GitHub #99; the poles
are in CONCRETE_POLE_SYSTEM.md, "Decorative and historic poles"). They come from
`dev-env-utils/scripts/gen_decorative_poles.py`, which writes the poles too, and are ordinary
`BlockBrightLightFactory.PostTop` blocks: `ICsmPostTopFixture`, so a decorative or concrete pole
under one shows its capital; 4-state STATE; light-up air one block east (`PostTop`'s X offset 1).
The heads hung forward of their pole (the Genie, the bishop's crook, NYC Type F) are
`PostTopAimed`, so their light falls one block ahead, under the lamp, whichever way they face.

| Registry name | Fixture | Reference |
|---|---|---|
| `lawilshirespecial` | Wilshire Special (1928): a tall four-sided bronze box lantern, one pane a face, a frieze top and bottom, a half-figure over a pendant at each corner, four scrolled brackets under its floor, a cornice with corner urns and a stepped dome under a bud finial | photos of the survivors by MacArthur Park (it was eight-sided, after museum frame 12, until 2026-11) |
| `lametropolitan` | Metropolitan: a narrower octagonal lantern with cornice horns and a spear tip | museum frame 12 (centre) |
| `lawestwoodlantern` | Westwood Village: a hexagonal lantern | museum frame 12 (left) |
| `labroadwayrosehead` | Broadway Rose: two opal globes on swept arms, a rose at each elbow, a spear between | BSL: a two-lamp electrolier garlanded with roses |
| `laum1906twin` | Union Metal UM-1906, the twin-lamp electrolier of Figueroa Street (mid-1920s): two short scrolled bracket arms, an upright opal acorn under a cap on each, a finial between. What the requester called the "Figueroa Special", a name no source uses | waterandpower.org; Street View of North Figueroa (Highland Park); confirmed by the requester |
| `lage104globe` | GE No. 104 globe with brass band | museum frame 09 |
| `lage198globe` | GE No. 198 globe | museum frame 13 |
| `laparagonjr` | Westinghouse Paragon Jr. (1920s) | museum frame 01 |
| `lallewellyn5` | the five-globe Llewellyn electrolier (c. 1900) | museum frame 06 |
| `lagenie` | Genie: a scroll arm and a skirted teardrop | BSL history |
| `lavinedouble` | Vine Double: two Genie arms | BSL history |
| `lahollywoodstar` | Hollywood Special, c. 1960 to 2005: a square black mast with a square-cut elbow and a long grey box head over the street, three prismatic bowl refractors under it and five red stars along each side. `PostTopAimed`; the one head that reaches a full block ahead of its pole (`Fixture.reach`) | the requester's photos of a surviving head and of one in service at Sunset Boulevard |
| `lahollywoodwof` | three tines, a lantern on each. It matches no real light: the 2005 Hollywood Boulevard light is the twin teardrop (`lavinedouble`), and the "pitchfork" it was built from a description of is the Van Nuys Special, which is the luminaire arm `luminairearmvannuystriple*` (Roads). Kept because it shipped | none |
| `lbcalheightslantern` | Long Beach's California Heights lamp (1929): a frosted acorn in a cage of eight slim ribs, a band at its widest point, a low ogee roof and a spike, all aluminium; on `decopolecalheights` | Street View of Olive Avenue; the requester's "blue lights by Bixby Knolls" |
| `reproacorn`, `reprowashingtonglobe`, `reprotwin20`, `reprobishopscrook`, `reprotypef` | the reproduction set: ribbed acorn, Washington Globe, Twin-20, bishop's crook with teardrop, NYC Type F (the 1913 Reverse Scroll Bracket) | maker and city descriptions |

**Scale.** These are drawn at about half the roadway fixtures' 1.4 (a Wilshire lantern is 7.5 ft
to its finial, which at 1.4 would be over three blocks): a lantern stands one and a half to two
blocks over its pole, and a multi-lamp head stays within half a block of its own block each way
(the generator asserts it). Their boxes are clamped to -0.5..1.5.

**Materials.** One `decolight.mtl`: `body` (the paint), `trim` (brass, bronze or gold), `frame`
(a mast or arm that is not the head's colour: a third paint in `FIXTURE_PAINT`, black otherwise),
`unshaded_lens` (glass: `glass_opal_*` for the milk globes, `glass_frosted_*` for the hammered
lantern panels, `glass_prism_*` for the ribbed acorns and teardrops, each `_on` / `_off` per
STATE). Paints and glass are generated 16 px swatches (32 px for the frosted panels and the
Westwood tile) in Core's `textures/blocks/decorativepoles/`.

**Weight.** 440 to 1,260 triangles. The plain globes are 440 to 570; the lanterns and multi-lamp
heads reach 800 to 1,260 (the five-globe Llewellyn is the heaviest) because each lamp is its own
lathe. A street carries one per pole, not one per lane, and none has a renderer.

**Each fixture writes two OBJs**: the model, and `_inv`, the same shrunk to fit one block for the
inventory icon. Every fixture's fitter ends in exactly the ring the next part starts from, so no
annulus is left open to look into; run `audit_obj_models.py` after a change.

## Lit and Unlit Textures on the Older Fixtures

The JSON fixtures that predate the decorative family reach the same result by a different route,
and it is worth knowing which, because the two families are authored in opposite directions. The
retrofit covered 81 fixtures on 27 atlases; the street lights among them became OBJ models in #99,
leaving **27 fixtures on 4 atlases** (`troffers`, `striplights`, `mcla_classic_post_light`,
`mcla_park_light`). The figures below are the original retrofit's.

The decorative family draws both lens textures from scratch and swaps a `#lens` material. The older
fixtures could not: each wears **one atlas across every face**, through three texture keys (`0`,
`all`, `particle`), with the lamp painted into the sheet. So they swap the WHOLE atlas per state
instead — which reaches the lens without anything being re-materialled, and is why **not one model
file changed** in the retrofit.

```jsonc
"defaults": { "textures": { "all": "…/merc_fixtures_off", "particle": "…_off", "0": "…_off" } },
"state": {
  "0": {},                                            // inherits the unlit default
  "1": { "textures": { "all": "…/merc_fixtures", "0": "…/merc_fixtures" } },
  "2": {},
  "3": { "textures": { "all": "…/merc_fixtures", "0": "…/merc_fixtures" } }
}
```

**The artwork that was already there is the LIT state.** That is measured, not assumed: on 24 of
the 27 atlases the lamp is drawn in pure white with no headroom to brighten at all, and only the
olive refractors (`merc_fixtures`, `westinghouse_ov50`) and the amber HPS lamp have any. It is
exactly why an unlit fixture used to look lit. So states 1 and 3 point at the original sheet and a
lit fixture looks as it always has; what the retrofit added is the missing half, a dimmed
`<atlas>_off.png` for states 0 and 2. `particle` stays on the unlit sheet — it is only the
break/step particle colour, and a lit fixture has no business showering bright particles.

Because `defaults` is the unlit sheet, the creative-menu icon and a freshly placed fixture read
unlit, which is what a block placed in `STATE_RS_OFF` should look like.

### Regenerating them

`dev-env-utils/scripts/gen_lighting_lit_atlases.py` owns both halves — the `_off` sheets and the
blockstates (4 and 27 today) — and is idempotent, so it can be re-run after re-tuning anything.

- **`--overlay`** draws each atlas at 8x with every candidate UV rect outlined and labelled by face
  count. This is the classification aid; the models carry explicit per-face UV rects, so the
  candidates are known exactly (140 across the 27 sheets, 2–9 each).
- **`LENS`** is the catalogue: per atlas, the pixel boxes that are the lamp, optionally narrowed to
  an exact colour set, each with a note saying what the region is.
- **`--check`** asserts every generated sheet is pixel-identical to its original **everywhere
  outside the catalogued lamp pixels**, and fails the run. The original artwork is never modified;
  this is what makes that a guarantee rather than a habit.

### Three things that will catch you out

- **Which rect is the lamp cannot be detected.** The obvious heuristic is wrong in both directions:
  on `troffers` the lens is the MOST-used rect (1390 faces — a troffer's whole underside is the
  diffuser), and on `ge_hps_fixtures` (since deleted) it was the rarest (3 of 348). Two atlases
  could not be read off the sheet at all and were settled by rendering the fixture through
  `preview_block_model.py`.
- **Verify from BELOW.** These are downlights; the lens is on the underside. An above-angle render
  shows nothing changing and reads as a false pass. Use `--pitch 30` or greater.
- **An LED array needs a harder dim than a flat lens**, and it is a question of contrast rather
  than amount. The LED street lights' dies were pure white on a panel face already at 220, so the
  shared default (which lands a white diffuser on ~178, matching the decorative family's
  `lens_opal_off`) left the dots the same value as the metal around them; a `dim` of (0.52, 0.45)
  took them to ~133, where they read as dark dots on a light optic. Those fixtures are OBJ since
  #99, so no catalogue entry uses it now; the script's comment by `LENS` says how to bring it back
  for a new sheet that paints a lamp grid on a light frame.
