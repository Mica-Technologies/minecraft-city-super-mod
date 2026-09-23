package com.micatechnologies.minecraft.csm.hvac;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * The primary thermostat: the controller of an HVAC system. It owns the system's heaters and
 * coolers and its zone thermostats, and may have vents of its own.
 *
 * <p>The control itself runs in {@link HvacSystemControl}, once a second, for the whole system at
 * once; this tile entity holds the links, the setpoints and the system's heating/cooling mode, and
 * shows what the control reports. See {@link TileEntityHvacThermostatBase}.</p>
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class TileEntityHvacThermostat extends TileEntityHvacThermostatBase {

  static final String NBT_LINKED_UNITS = "lU";
  static final String LEGACY_NBT_LINKED_UNITS = "linkedUnits";
  static final String NBT_LINKED_ZONES = "lZ";
  static final String LEGACY_NBT_LINKED_ZONES = "linkedZones";
  static final String NBT_SYSTEM_MODE = "sM";

  /** Positions of linked heaters/coolers. */
  private final List<BlockPos> linkedUnits = new ArrayList<>();

  /** Positions of linked zone thermostats. */
  private final List<BlockPos> linkedZones = new ArrayList<>();

  /** What the system is doing: {@link HvacStatus} mode. Saved, so a reload does not flap it. */
  int systemMode = HvacStatus.MODE_IDLE;

  /** World tick the system entered {@link #systemMode}. */
  transient long systemModeSince;

  /** The last non-idle mode, so the system does not reverse without a pause. */
  int lastActiveMode = HvacStatus.MODE_IDLE;

  @Override
  protected int anchorKind() {
    return ThermalAnchor.THERMOSTAT;
  }

  // region Units

  public boolean linkUnit(BlockPos unitPos) {
    if (linkedUnits.contains(unitPos)) {
      return false;
    }
    if (world != null && !(world.getTileEntity(unitPos) instanceof TileEntityHvacHeater)) {
      return false;
    }
    linkedUnits.add(unitPos.toImmutable());
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
    return true;
  }

  public boolean unlinkUnit(BlockPos unitPos) {
    boolean removed = linkedUnits.remove(unitPos);
    if (removed && world != null && !world.isRemote) {
      TileEntity te = world.isBlockLoaded(unitPos) ? world.getTileEntity(unitPos) : null;
      if (te instanceof TileEntityHvacHeater) {
        ((TileEntityHvacHeater) te).applyOutput(0.0f);
      }
      markDirtySync(world, pos, true);
    }
    return removed;
  }

  public List<BlockPos> getLinkedUnits() {
    return linkedUnits;
  }

  public int getLinkedUnitCount() {
    return linkedUnits.size();
  }

  /**
   * Farthest a vent may be linked: 30 blocks, or 100 with a rooftop unit in the system.
   */
  @Override
  public int getMaxVentLinkDistance() {
    int max = 30;
    for (BlockPos unitPos : linkedUnits) {
      if (world != null && world.isBlockLoaded(unitPos)) {
        TileEntity te = world.getTileEntity(unitPos);
        if (te instanceof IHvacUnit) {
          max = Math.max(max, ((IHvacUnit) te).getMaxVentLinkDistance());
        }
      }
    }
    return max;
  }

  // endregion

  // region Zones

  public boolean linkZone(BlockPos zonePos) {
    if (linkedZones.contains(zonePos)) {
      return false;
    }
    if (world != null && !(world.getTileEntity(zonePos) instanceof TileEntityHvacZoneThermostat)) {
      return false;
    }
    linkedZones.add(zonePos.toImmutable());
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
    return true;
  }

  public boolean unlinkZone(BlockPos zonePos) {
    boolean removed = linkedZones.remove(zonePos);
    if (removed && world != null && !world.isRemote) {
      if (world.isBlockLoaded(zonePos)) {
        TileEntity te = world.getTileEntity(zonePos);
        if (te instanceof TileEntityHvacZoneThermostat) {
          ((TileEntityHvacZoneThermostat) te).setLinkedPrimaryPos(null);
        }
      }
      markDirtySync(world, pos, true);
    }
    return removed;
  }

  public List<BlockPos> getLinkedZones() {
    return linkedZones;
  }

  public int getLinkedZoneCount() {
    return linkedZones.size();
  }

  /** Units in the system, as last reported (synced, so it is right on the client too). */
  public int getTotalUnitCount() {
    return world != null && world.isRemote ? totalUnits : linkedUnits.size();
  }

  // endregion

  // region NBT

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    systemMode = compound.getInteger(NBT_SYSTEM_MODE);
    lastActiveMode = systemMode;
    readPosList(compound, NBT_LINKED_UNITS, LEGACY_NBT_LINKED_UNITS, linkedUnits);
    readPosList(compound, NBT_LINKED_ZONES, LEGACY_NBT_LINKED_ZONES, linkedZones);
    compound.removeTag(LEGACY_NBT_LINKED_UNITS);
    compound.removeTag(LEGACY_NBT_LINKED_ZONES);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    compound.setInteger(NBT_SYSTEM_MODE, systemMode);
    compound.setTag(NBT_LINKED_UNITS, writePosList(linkedUnits));
    compound.setTag(NBT_LINKED_ZONES, writePosList(linkedZones));
    return compound;
  }

  // endregion
}
