# Lighting System

Technical documentation for the lighting subsystem in the City Super Mod.

## Overview

The lighting system provides 70+ realistic light fixture blocks (street lights, ceiling lights,
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
