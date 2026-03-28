# Block and Item Base Classes

Comprehensive reference for the abstract class hierarchy in
`src/main/java/com/micatechnologies/minecraft/csm/codeutils/`.

## Design Philosophy

All blocks and items in this mod auto-register themselves during construction. You never
manually call registration methods -- the constructors handle it via `CsmRegistry`. This means
creating a block is as simple as extending the right base class, implementing a few abstract
methods, and adding it to a tab.

Key patterns:
- **Auto-registration**: Constructors call `CsmRegistry.registerBlock(this)` and create an
  `ItemBlock` automatically.
- **Template methods**: Abstract methods in `ICsmBlock`/`ICsmItem` ensure consistent behavior.
- **Rotation abstraction**: `RotationUtils` handles bounding box rotation math for all
  directional blocks.
- **NBT delegation**: Tile entity subclasses only implement `readNBT`/`writeNBT`; the parent
  handles Minecraft's save/load/sync plumbing.

## Block Class Hierarchy

```
Block (Minecraft)
├── AbstractBlock ← root of all mod blocks
│   ├── AbstractBlockRotatableNSEW ← 4-direction horizontal rotation
│   ├── AbstractBlockRotatableNSEWUD ← 6-direction full rotation
│   │   ├── AbstractBlockTrafficPole ← pole with mount detection
│   │   │   └── AbstractBlockTrafficPoleDiagonal ← 8-dir mount detection
│   │   └── (many subsystem blocks)
│   ├── AbstractBlockRotatableHZEight ← 8-direction horizontal rotation
│   ├── AbstractPoweredBlockRotatableNSEWUD ← 6-dir + redstone POWERED
│   └── AbstractBlockSetBasic ← auto-creates fence+slab+stairs set
│
├── BlockFence (Minecraft)
│   └── AbstractBlockFence
│
├── BlockStairs (Minecraft)
│   └── AbstractBlockStairs
│
└── BlockSlab (Minecraft)
    └── AbstractBlockSlab ← auto-creates double slab variant
```

## AbstractBlock (Root)

**Extends:** `Block` | **Implements:** `IHasModel`, `ICsmBlock`

The foundation for every block in the mod.

### Constructors

```java
// Simple (defaults to STONE sound, no tool, 0 hardness/resistance/light)
AbstractBlock(Material material)

// Full control
AbstractBlock(Material material, SoundType soundType, String harvestToolClass,
    int harvestLevel, float hardness, float resistance, float lightLevel, int lightOpacity)
```

| Parameter | Type | Purpose | Example |
|---|---|---|---|
| `material` | Material | Physics/map color | `Material.ROCK`, `Material.IRON` |
| `soundType` | SoundType | Step/break/place sounds | `SoundType.STONE`, `SoundType.METAL` |
| `harvestToolClass` | String | Required tool type | `"pickaxe"`, `"axe"`, `"shovel"` |
| `harvestLevel` | int | Minimum tool tier (0=wood, 1=stone, 2=iron, 3=diamond) | `1` |
| `hardness` | float | Break time | `2.0F` |
| `resistance` | float | Explosion resistance | `10.0F` |
| `lightLevel` | float | Light emission (0.0-1.0) | `0.0F` |
| `lightOpacity` | int | Light blocking (0-15) | `0` |

### What the Constructor Does

```java
// Automatically called in constructor:
setTranslationKey(getBlockRegistryName());
setRegistryName(CsmConstants.MOD_NAMESPACE, getBlockRegistryName());
CsmRegistry.registerBlock(this);
CsmRegistry.registerItem(new ItemBlock(this));  // Creates inventory item
```

### Abstract Methods (from ICsmBlock)

Every block subclass must implement these:

| Method | Returns | Purpose |
|---|---|---|
| `getBlockRegistryName()` | `String` | Unique ID in `snake_case` (e.g., `"my_block"`) |
| `getBlockBoundingBox(state, source, pos)` | `AxisAlignedBB` | Collision/selection box (null = full cube) |
| `getBlockIsOpaqueCube(state)` | `boolean` | Does the block fully obscure what's behind it? |
| `getBlockIsFullCube(state)` | `boolean` | Is it a full 1x1x1 cube? |
| `getBlockConnectsRedstone(state, access, pos, facing)` | `boolean` | Can redstone wire connect? |
| `getBlockRenderLayer()` | `BlockRenderLayer` | Render pass (`SOLID`, `CUTOUT`, `CUTOUT_MIPPED`, `TRANSLUCENT`) |

### Tile Entity Support

Any block can support a tile entity by implementing `ICsmTileEntityProvider`:

```java
public class MyBlock extends AbstractBlock implements ICsmTileEntityProvider {
    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
        return MyTileEntity.class;
    }

    @Override
    public String getTileEntityName() {
        return "mytileentity";  // Must be unique
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new MyTileEntity();
    }
}
```

Registration is automatic -- `Csm.java` scans all registered blocks for `ICsmTileEntityProvider`
and registers their tile entity classes.

### Block Retirement (ICsmRetiringBlock)

Blocks scheduled for replacement implement `ICsmRetiringBlock`:

```java
public interface ICsmRetiringBlock {
    String getReplacementBlockId();
}
```

On random ticks, the old block is automatically replaced with the new one, preserving facing.

## AbstractBlockRotatableNSEW (4-Direction)

**Extends:** `AbstractBlock`

Adds horizontal rotation (North, South, East, West).

### Property

```java
public static final PropertyDirection FACING = BlockHorizontal.FACING;
// Values: NORTH, SOUTH, EAST, WEST
```

### Meta Encoding

4 bits used entirely for facing (values 0-3).

### Placement

Block faces opposite to the player's horizontal look direction (player places block facing
toward them).

### Bounding Box Rotation

Override `getBlockBoundingBox()` to return the bounding box as if facing **NORTH**. The base
class automatically rotates it to match the actual facing via `RotationUtils`.

```java
@Override
public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    // Define as if facing NORTH (Z+ side is the back/wall)
    return new AxisAlignedBB(0.1, 0.0, 0.8, 0.9, 1.0, 1.0);
}
// Auto-rotated for EAST, SOUTH, WEST
```

## AbstractBlockRotatableNSEWUD (6-Direction)

**Extends:** `AbstractBlock`

Adds full rotation including up and down. This is the most commonly used base class for
wall-mounted devices (fire alarms, signs, sensors, etc.).

### Property

```java
public static final PropertyDirection FACING = BlockDirectional.FACING;
// Values: NORTH, SOUTH, EAST, WEST, UP, DOWN
```

### Meta Encoding

Values 0-5 for 6 directions. This leaves `floor(15/6) = 2` values for an additional property
(max range 0-1 for a second property like SOUND).

### Placement

Uses `EnumFacing.getDirectionFromEntityLiving()` -- the block faces the surface it's placed on
(wall-mounting behavior).

## AbstractBlockRotatableHZEight (8-Direction)

**Extends:** `AbstractBlock`

Eight-direction horizontal rotation for signs and poles.

### Property

```java
public static final PropertyEnum<DirectionEight> FACING =
    PropertyEnum.create("facing", DirectionEight.class);
// Values: N, S, E, W, NE, NW, SE, SW (indices 0-7)
```

### Meta Encoding

Values 0-7 for 8 directions. Uses full meta range -- no room for additional properties.

### Special: Stacking Behavior

When placed on top of another block of the same type, inherits the lower block's facing
direction. Otherwise, calculates 8-direction from player yaw in 22.5-degree increments.

## AbstractPoweredBlockRotatableNSEWUD (6-Dir + Redstone)

**Extends:** `AbstractBlock`

6-direction rotation plus a `POWERED` boolean property.

### Properties

```java
public static final PropertyDirection FACING = BlockDirectional.FACING;
public static final PropertyBool POWERED = PropertyBool.create("powered");
```

### Meta Encoding

Bits 0-2 = FACING (0-5), Bit 3 = POWERED flag. Fits in 4 bits.

### Behavior

Automatically updates `POWERED` when adjacent redstone power changes via `neighborChanged()`.

## AbstractBlockSetBasic (Block Set Generator)

**Extends:** `AbstractBlock`

Creates a complete set of 4 blocks from a single class:
- `my_block` (the base block)
- `my_block_fence`
- `my_block_stairs`
- `my_block_slab` (+ `my_block_slab_double`)

### Usage

```java
public class BlockBrickRed extends AbstractBlockSetBasic {
    public BlockBrickRed() {
        super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
    }

    @Override
    public String getBlockRegistryName() { return "brick_red"; }
    // Creates: brick_red, brick_red_fence, brick_red_stairs, brick_red_slab
}
```

All variants auto-register and share the parent's creative tab.

## AbstractBlockFence / Stairs / Slab

These extend Minecraft's vanilla `BlockFence`, `BlockStairs`, and `BlockSlab` directly (not
`AbstractBlock`) but implement the same `IHasModel` and `ICsmBlock` interfaces.

### AbstractBlockSlab Special Behavior

The constructor automatically creates and registers a double-slab variant:
- Single: `getBlockRegistryName()`
- Double: `getBlockRegistryName() + "_double"`

Uses a `VARIANT` property (single value: DEFAULT) and `HALF` property (TOP/BOTTOM).

## AbstractBlockTrafficPole

**Extends:** `AbstractBlockRotatableNSEWUD`

Specialized for traffic signal poles with dynamic mount detection.

### Properties

```java
public static final PropertyBool MOUNT_EAST = PropertyBool.create("mounteast");
public static final PropertyBool MOUNT_WEST = PropertyBool.create("mountwest");
public static final PropertyBool MOUNT_UP = PropertyBool.create("mountup");
public static final PropertyBool MOUNT_DOWN = PropertyBool.create("mountdown");
```

### Mount Detection

`getActualState()` dynamically checks adjacent blocks to set MOUNT properties. Uses
`BlockUtils.getRelativeFacing()` to rotate the check directions based on the pole's facing.
Certain block types can be excluded via `getIgnoreBlock()`.

### Color Variants

Abstract method `getTrafficPoleColor()` returns a `TRAFFIC_POLE_COLOR` enum value (BLACK,
SILVER, TAN, WHITE, UNPAINTED).

`AbstractBlockTrafficPoleDiagonal` extends this with additional `MOUNT_NORTH` and
`MOUNT_SOUTH` properties for diagonal mount detection.

## Item Classes

### AbstractItem

**Extends:** `Item` | **Implements:** `IHasModel`, `ICsmItem`

```java
AbstractItem()                        // 0 damage, 64 stack size
AbstractItem(int maxDamage, int maxStackSize)
```

Must implement: `String getItemRegistryName()`

Auto-registers via `CsmRegistry.registerItem(this)`.

### AbstractItemSpade

**Extends:** `ItemSpade` | **Implements:** `IHasModel`, `ICsmItem`

```java
AbstractItemSpade(ToolMaterial material)
AbstractItemSpade(int maxDamage, int maxStackSize, ToolMaterial material)
```

## Tile Entity Classes

### AbstractTileEntity

**Extends:** `TileEntity`

Base for all non-ticking tile entities. Handles NBT save/load/sync plumbing.

**Must implement:**
```java
void readNBT(NBTTagCompound compound)           // Load your data
NBTTagCompound writeNBT(NBTTagCompound compound) // Save your data, return compound
```

**Provided sync helpers:**

| Method | Saves to Disk | Syncs to Client | Re-renders |
|---|---|---|---|
| `markDirty()` | Yes | No | No |
| `markDirtySync(world, pos)` | Yes | No | No |
| `markDirtySync(world, pos, true)` | Yes | Yes | No |
| `markDirtySync(world, pos, state)` | Yes | No | Yes |
| `markDirtySync(world, pos, state, true)` | Yes | Yes | Yes |
| `syncServerToClient(world)` | No | Yes | No |

### AbstractTickableTileEntity

**Extends:** `AbstractTileEntity` | **Implements:** `ITickable`

Adds configurable tick behavior.

**Must implement (in addition to readNBT/writeNBT):**

| Method | Returns | Purpose |
|---|---|---|
| `doClientTick()` | `boolean` | Should this also tick on the client? (Server always ticks) |
| `pauseTicking()` | `boolean` | Temporarily stop ticking? |
| `getTickRate()` | `long` | Ticks between `onTick()` calls |
| `onTick()` | `void` | Your tick logic |

**Tick condition:**
```
Ticks when: (server OR doClientTick) AND !pauseTicking AND (worldTime % tickRate == 0)
```

## Registration and Tabs

### CsmRegistry

Central registry -- you rarely interact with it directly since constructors handle registration.

```java
CsmRegistry.getBlock("my_block");      // Lookup by registry name
CsmRegistry.getBlocks();               // All registered blocks
CsmRegistry.getItems();                // All registered items
```

### CsmTab (Creative Tabs)

Tabs are defined in `src/main/java/.../csm/tabs/` and loaded via annotation:

```java
@CsmTab.Load(order = 5)
public class CsmTabLifeSafety extends CsmTab {
    @Override public String getTabId() { return "tablifesafety"; }
    @Override public Block getTabIcon() { return CsmRegistry.getBlock("firealarmcontrolpanel"); }
    @Override public boolean getTabSearchable() { return true; }
    @Override public boolean getTabHidden() { return false; }

    @Override
    public void initTabElements(FMLPreInitializationEvent event) {
        initTabBlock(BlockFireAlarmControlPanel.class, event);
        initTabItem(ItemSignalLinker.class, event);
        // ...
    }
}
```

The `order` annotation value determines tab display order in the creative menu.

`initTabBlock(Class, event)` instantiates the block (triggering auto-registration) and assigns
it to this tab. `initTabItem(Class, event)` does the same for items.

## Utility Classes

### RotationUtils

Rotates bounding boxes to match facing direction:

```java
// For NSEW and NSEWUD blocks:
AxisAlignedBB rotated = RotationUtils.rotateBoundingBoxByFacing(northFacingBox, facing);

// For HZEight blocks:
AxisAlignedBB rotated = RotationUtils.rotateBoundingBoxByFacing(northFacingBox, directionEight);
```

Define your bounding box as if the block faces **NORTH**. The utility handles all rotation math.

### BlockUtils

Checks for adjacent blocks in specific directions:

```java
// Check if there's a specific block type to the east
boolean hasMount = BlockUtils.getIsBlockToSide(world, pos.east(), MyBlock.class);

// Relative facing (for rotated adjacency checks)
EnumFacing relative = BlockUtils.getRelativeFacing(blockFacing, EnumFacing.EAST);
```

## Quick Reference: Choosing a Base Class

| I need... | Use this base class |
|---|---|
| A simple full cube | `AbstractBlock` |
| A block that faces N/S/E/W | `AbstractBlockRotatableNSEW` |
| A wall-mounted block (any face) | `AbstractBlockRotatableNSEWUD` |
| A sign/pole with 8 rotations | `AbstractBlockRotatableHZEight` |
| A redstone-reactive rotatable block | `AbstractPoweredBlockRotatableNSEWUD` |
| A full block set (fence+stairs+slab) | `AbstractBlockSetBasic` |
| Just a fence | `AbstractBlockFence` |
| Just stairs | `AbstractBlockStairs` |
| Just a slab | `AbstractBlockSlab` |
| A traffic signal pole | `AbstractBlockTrafficPole` |
| A diagonal traffic pole | `AbstractBlockTrafficPoleDiagonal` |
