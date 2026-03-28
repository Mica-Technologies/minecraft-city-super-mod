# Power Grid System

Technical documentation for the power grid subsystem in the City Super Mod.

## Overview

The power grid system provides electrical infrastructure blocks (utility poles, transformers,
wire mounts, insulators) and a Forge Energy (FE) integration layer that bridges the mod's
power grid with Minecraft's energy capability system.

Code is organized in `src/main/java/com/micatechnologies/minecraft/csm/powergrid/`:
- **Root directory**: 44 decorative/structural blocks (poles, mounts, cross-arms)
- **`fe/` subdirectory**: 4 files for Forge Energy integration (2 blocks + 2 tile entities)

## Forge Energy Integration

The FE subsystem provides two functional blocks that bridge creative energy production with
redstone signaling.

### Energy Flow

```
  Redstone ──> [BlockForgeEnergyProducer] ──FE──> [BlockForgeEnergyToRedstone] ──> Redstone
               (infinite FE source)                (FE-to-redstone converter)
               Registry: "rfprod"                  Registry: "rftors"
```

### BlockForgeEnergyProducer / TileEntityForgeEnergyProducer

An infinite energy source that pushes Forge Energy to adjacent blocks.

**Behavior:**
- Only outputs when **powered by redstone** (redstone-gated)
- Pushes `Integer.MAX_VALUE` FE to all 6 adjacent faces every tick
- Sneak-click to adjust tick rate (cycles: 1 -> 10 -> 20 -> ... -> 200 -> 1)
- Displays high voltage warning tooltip

**Tile Entity:**
- Extends `AbstractTickableTileEntity` + implements `IEnergyStorage`
- `receiveEnergy()` -> 0 (source only, never receives)
- `extractEnergy()` -> returns requested amount (infinite supply)
- `getEnergyStored()` -> `Integer.MAX_VALUE`
- Persists `tickRate` to NBT

### BlockForgeEnergyToRedstone / TileEntityForgeEnergyConsumer

Converts incoming Forge Energy to a redstone signal.

**Behavior:**
- Receives FE from adjacent energy sources
- Consumes 6 FE per tick (every 40 ticks)
- Outputs redstone signal strength 15 when energy is available
- `POWERED` block state property controls redstone output

**Tile Entity:**
- Extends `AbstractTickableTileEntity` + implements `IEnergyStorage`
- Max storage: 24 FE
- `receiveEnergy()` -> stores up to capacity
- `extractEnergy()` -> 0 (consumer only)
- Persists `storedEnergy` to NBT

### Energy Networking

The system uses Minecraft Forge's capability system (`CapabilityEnergy.ENERGY`). No custom
networking is needed:

1. Each FE tile entity exposes `IEnergyStorage` capability on all 6 faces
2. The producer queries adjacent blocks for energy capabilities each tick
3. Adjacent blocks that accept energy form an implicit network
4. Any Forge Energy-compatible block from other mods can connect

### Adding a New Energy Block

1. Create block extending `AbstractBlock` implementing `ICsmTileEntityProvider`
2. Create tile entity extending `AbstractTickableTileEntity` implementing `IEnergyStorage`
3. Expose capability via `hasCapability`/`getCapability` overrides:
   ```java
   @Override
   public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
       return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
   }

   @Override
   public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
       if (capability == CapabilityEnergy.ENERGY) {
           return CapabilityEnergy.ENERGY.cast(this);
       }
       return super.getCapability(capability, facing);
   }
   ```
4. Implement `IEnergyStorage` methods for your energy behavior
5. Register in `CsmTabPowerGrid`

## Utility Pole System

The mod provides a modular pole system with stackable segments:

### Pole Segments

| Block | Registry | Purpose |
|---|---|---|
| `BlockFGPoleBottom` | `fgpolebottom` | Base section |
| `BlockFGPoleMiddle` | `fgpolemiddle` | Stackable middle sections |
| `BlockFGPoleTop` | `fgpoletop` | Top section with cross-arm attachment |

All extend `AbstractBlockRotatableNSEWUD` with full 6-direction rotation.

### Cross-Arms and Mounting Hardware

Cross-arms attach to pole tops to hold insulators and wires:

| Series | Blocks | Style |
|---|---|---|
| Old Brooks | `BlockOldBrooksXArm1-7`, `BlockOldESBrooksXArm` | Traditional wooden arm |
| New Brooks | `BlockNewBrooksXArm1-4`, `BlockNewESBrooksXArm1-3` | Updated arm design |
| PCA Series | `BlockPCAB1-3`, `BlockPCAW1-3` | Post-cap arms |
| MLUVMB | `BlockMLUVMB1-5` | Multi-level utility mounts |

### Mounting and Safety Equipment

| Block | Purpose |
|---|---|
| `BlockPoleWireMount` | Wire attachment point |
| `BlockTransformerMount` | Transformer mounting bracket |
| `BlockTEInsulatorCover` / `BlockTEInsulatorCoverDE` | Insulator protective covers |
| `BlockTEPerchGuard` | Perch guard for wildlife safety |
| `BlockPoleVisStrips` | Visibility/safety marking strips |
| `BlockPoleHVSign` / `BlockMPHVSign` / `BlockFGPHVSign` | High voltage warning signs |
| `BlockSCELightMount` / `BlockSCELightMountSmall` | Street light mounting brackets |
| `BlockPullyMount` | Pulley mounting bracket |
| `BlockTSC` | Transformer secondary connection |

### Specialized Blocks

| Block | Material | Notes |
|---|---|---|
| `BlockAFEI` | GLASS | Air Force Equipment Interface, translucent render |
| `BlockAFEIS` | GLASS | Similar specialized equipment |

## Common Properties

All structural/decorative blocks share:
- Base class: `AbstractBlockRotatableNSEWUD`
- Material: ROCK (except AFEI variants which use GLASS)
- Hardness: 1.0-2.0, Resistance: 10.0
- No redstone connectivity
- No tile entities
- Render layer: SOLID (except translucent variants)
- Bounding box: SQUARE_BOUNDING_BOX (most blocks)
