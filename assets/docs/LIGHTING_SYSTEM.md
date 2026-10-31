# Lighting System

Technical documentation for the lighting subsystem in the City Super Mod.

## Overview

The lighting system provides 119 realistic light fixture blocks (street lights, ceiling lights,
wall-mounted fixtures) with a 4-state on/off system combining redstone control and manual
override. Lights can project illumination downward using invisible "light-up air" blocks.

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
| `CsmLightupAir` | the column projection, `firstBelow` (the light-up air a column scan meets before anything else) and `claimedBySource` |
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

Ten decorative fixtures — five ceiling pendants and five wall sconces — plus a stackable chain,
each in three metal finishes, for 33 blocks. Unlike the rest of the tab these are **OBJ models,
procedurally generated** by `dev-env-utils/scripts/gen_decorative_lighting.py`; run that script to
regenerate the geometry, the swatch textures and all 33 blockstates at once.

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
`lens_*_on`, STATE 0 and 2 get `lens_*_off`. Two lens families exist, `lens_opal_*` for glass
shades and `lens_bulb_*` for exposed bulbs.

The other 81 fixtures now do this too, by a different route — they share 27 atlas textures with the
lamp painted in, so they swap the whole atlas per state rather than a `#lens` material. See
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

## Lit and Unlit Textures on the Older Fixtures

The 81 fixtures that predate the decorative family reach the same result by a different route, and
it is worth knowing which, because the two families are authored in opposite directions.

The decorative family draws both lens textures from scratch and swaps a `#lens` material. The older
fixtures could not: each wears **one atlas across every face**, through three texture keys (`0`,
`all`, `particle`), with the lamp painted into the sheet. So they swap the WHOLE atlas per state
instead — which reaches the lens without anything being re-materialled, and is why **not one model
file changed** in the retrofit.

```jsonc
"defaults": { "textures": { "all": "…/ge_hps_fixtures_off", "particle": "…_off", "0": "…_off" } },
"state": {
  "0": {},                                            // inherits the unlit default
  "1": { "textures": { "all": "…/ge_hps_fixtures", "0": "…/ge_hps_fixtures" } },
  "2": {},
  "3": { "textures": { "all": "…/ge_hps_fixtures", "0": "…/ge_hps_fixtures" } }
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

`dev-env-utils/scripts/gen_lighting_lit_atlases.py` owns both halves — the 27 `_off` sheets and the
81 blockstates — and is idempotent, so it can be re-run after re-tuning anything.

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
  diffuser), and on `ge_hps_fixtures` it is the rarest (3 of 348). Two atlases could not be read off
  the sheet at all and were settled by rendering the fixture through `preview_block_model.py`.
- **Verify from BELOW.** These are downlights; the lens is on the underside. An above-angle render
  shows nothing changing and reads as a false pass. Use `--pitch 30` or greater.
- **The LED arrays need a harder dim than the flat lenses**, and it is a question of contrast rather
  than amount. Their dies are pure white sitting on a panel face already at 220, so the shared
  default (which lands a white diffuser on ~178, matching the decorative family's `lens_opal_off`)
  left the dots the same value as the metal around them. `LED_DIM` takes them to ~133, where they
  read as dark dots on a light optic — which is what an unlit LED luminaire looks like.
