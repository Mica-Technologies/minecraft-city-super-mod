# Lighting System

Technical documentation for the lighting subsystem in the City Super Mod.

## Overview

The lighting system provides 119 realistic light fixture blocks (street lights, ceiling lights,
wall-mounted fixtures) with a 4-state on/off system combining redstone control and manual
override. Lights can project illumination downward using invisible "light-up air" blocks.

All lighting code lives in `src/main/java/com/micatechnologies/minecraft/csm/lighting/`.

## Architecture

The system has three categories of blocks:

1. **Interactive Bright Lights** (extend `AbstractBrightLight`) -- the main fixtures with
   redstone + manual on/off control and light emission
2. **Structural Mounting Blocks** (extend `AbstractBlockRotatableNSEW/UD`) -- passive brackets,
   mounts, and junction boxes with no lighting logic
3. **BlockLightupAir** -- invisible light source block automatically placed/removed by lights

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

When a light turns ON, `handleAirLightBlock(true, world, pos)` is called:
1. Scans **downward** from the light's position (up to 16 blocks)
2. Finds the first **air block** (skips the block directly beneath the light)
3. Places a `BlockLightupAir` at that position (invisible, passable, emits light level 15)

When a light turns OFF, the method removes any `BlockLightupAir` blocks in the column below.

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

- Extends `AbstractBlockRotatableNSEWUD`
- Invisible and passable (null collision box, tiny render box)
- Replaceable (can be overwritten by block placement)
- Render layer: TRANSLUCENT
- Emits light level 15

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

Retrofitting this to the other 83 fixtures is a separate job: they share 28 atlas textures with
the lens baked in, so it needs an `<atlas>_off.png` per texture rather than a blockstate change
alone.

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
