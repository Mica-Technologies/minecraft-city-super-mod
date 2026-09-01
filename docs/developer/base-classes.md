# Block & Item Base Classes

Every block and item in CSM extends one of a small set of base classes. Picking the right one is
most of the work — the base handles registration, rotation, meta encoding and bounding boxes so you
do not have to.

## The design

**Blocks register themselves.** You never call a registration method: the constructor does it, and
creates the `ItemBlock` for the inventory too.

```java
// What AbstractBlock's constructor does for you
setTranslationKey(getBlockRegistryName());
setRegistryName(CsmConstants.MOD_NAMESPACE, getBlockRegistryName());
CsmRegistry.registerBlock(this);
CsmRegistry.registerItem(new ItemBlock(this));
```

So creating a block is: extend the right base, implement a few abstract methods, add it to a tab.

Three other patterns run through all of it:

- **Rotation is abstracted.** `RotationUtils` does the bounding box maths for every directional
  block, so a subclass never rotates its own box.
- **NBT is delegated.** Tile entity subclasses implement only `readNBT` / `writeNBT`; the parent
  handles Minecraft's save, load and sync plumbing.
- **Template methods.** The abstract methods on `ICsmBlock` / `ICsmItem` are what keep behaviour
  consistent across 1,639 blocks.

## Choosing a base class

| Class | Use when |
|---|---|
| `AbstractBlock` | The block does not rotate |
| `AbstractBlockRotatableNSEW` | Horizontal rotation — north, south, east, west |
| `AbstractBlockRotatableNSEWUD` | Full six-way, including up and down |
| `AbstractBlockRotatableHZEight` | Eight-way horizontal, for signs |
| `AbstractPoweredBlockRotatableNSEWUD` | Six-way **and** redstone-powered |
| `AbstractBlockSetBasic` | Generates a fence, stairs and slab set from one definition |
| `AbstractBlockFence` / `Stairs` / `Slab` | An individual fence, stair or slab |
| `AbstractBlockTrafficPole` | A traffic pole with directional mounting support |
| `AbstractBlockTrafficPoleDiagonal` | The diagonal pole variant |

Items extend `AbstractItem` or `AbstractItemSpade`. Tile entities extend `AbstractTileEntity` or
`AbstractTickableTileEntity`.

## The constructor

```java
// Simple — STONE sound, no tool, zero hardness, resistance and light
AbstractBlock(Material material)

// Full control
AbstractBlock(Material material, SoundType soundType, String harvestToolClass,
    int harvestLevel, float hardness, float resistance, float lightLevel, int lightOpacity)
```

| Parameter | Purpose | Example |
|---|---|---|
| `material` | Physics and map colour | `Material.ROCK` |
| `soundType` | Step, break and place sounds | `SoundType.METAL` |
| `harvestToolClass` | Required tool | `"pickaxe"` |
| `harvestLevel` | Minimum tier — 0 wood, 1 stone, 2 iron, 3 diamond | `1` |
| `hardness` | Break time | `2.0F` |
| `resistance` | Explosion resistance | `10.0F` |
| `lightLevel` | Emission, 0.0–1.0 | `0.0F` |
| `lightOpacity` | Light blocking, 0–15 | `0` |

!!! note "This is where the block reference gets its stats"

    `gen_wiki_reference.py` reads this call to fill the hardness, resistance, tool and harvest
    columns, walking up the `extends` chain when a class does not make the call itself. A block
    whose stats arrive as constructor parameters rather than literals shows blank columns.

## What every block must implement

| Method | Purpose |
|---|---|
| `getBlockRegistryName()` | The unique id, in `snake_case` |
| `getBlockBoundingBox(state, source, pos)` | Collision and selection box; `null` means full cube |
| `getBlockIsOpaqueCube(state)` | Does it fully obscure what is behind it? |
| `getBlockIsFullCube(state)` | Is it a full 1×1×1 cube? |
| `getBlockConnectsRedstone(state, access, pos, facing)` | Can redstone wire connect? |
| `getBlockRenderLayer()` | `SOLID`, `CUTOUT`, `CUTOUT_MIPPED` or `TRANSLUCENT` |

## Tile entities

Any block can carry one by implementing `ICsmTileEntityProvider`, which wants a class and a name:

```java
@Override
public Class<? extends TileEntity> getTileEntityClass() {
  return TileEntityMyThing.class;
}

@Override
public String getTileEntityName() {
  return "tileentitymything";
}
```

## Traps worth knowing

!!! danger "Do not rotate a bounding box yourself"

    `AbstractBlockRotatableNSEW` **already rotates** the box returned by `getBlockBoundingBox`. A
    per-facing switch inside that method rotates it a second time and lands it on the wrong face.
    Return the box for the block's default facing and let the base class do the rest.

!!! warning "NSEWUD faces UP when placed while flying"

    A six-way block placed by a player looking downward gets `UP`, which reads in game as "rotation
    is broken". It is not — it is the placement rule working as written.

## Retiring a block

`ICsmRetiringBlock` replaces a block with a different one on load, carrying its tile entity data
across. That is how blocks get consolidated without breaking existing worlds — the old id stays
registered so a saved chunk still loads, and it becomes the new block in place.

Retired blocks live in the `CsmTabNone` tab and appear on the
[Unlisted reference page](../reference/unlisted.md).
