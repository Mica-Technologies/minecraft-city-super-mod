package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Tile entity for the HVAC vent relay block. Links to a thermostat controller (not directly
 * to heaters/coolers). The thermostat manages the full system and tells vents what to contribute.
 * Vent relays get their temperature contribution from the thermostat's system state.
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class TileEntityHvacVentRelay extends AbstractTickableTileEntity implements IHvacUnit {

  private static final String NBT_HAS_LINK = "hasLink";
  private static final String NBT_LINK_X = "linkX";
  private static final String NBT_LINK_Y = "linkY";
  private static final String NBT_LINK_Z = "linkZ";

  /** Position of the linked thermostat, or null if not linked. */
  private BlockPos linkedThermostatPos = null;

  /** Cached active state for detecting transitions. */
  private boolean wasActive = false;

  /**
   * Set by the thermostat when the system is calling. Includes ramp-up factor.
   * Transient — not persisted. Thermostat re-pushes on its next tick after restart.
   */
  private float currentContribution = 0.0f;

  /**
   * Called by the thermostat to set this vent's current temperature contribution.
   * The value already accounts for ramp-up factor and vent relay strength.
   * Syncs to client so the HUD temperature calculation sees the contribution.
   */
  public void setContribution(float contribution) {
    if (Math.abs(this.currentContribution - contribution) > 0.1f) {
      this.currentContribution = contribution;
      if (world != null && !world.isRemote) {
        markDirtySync(world, pos, true);
      }
    }
  }

  // region IHvacUnit

  @Override
  public float getTemperatureContribution() {
    return currentContribution;
  }

  @Override
  public boolean isHvacActive() {
    return Math.abs(currentContribution) > 0.1f;
  }

  // endregion

  // region AbstractTickableTileEntity

  @Override
  public boolean doClientTick() {
    return false;
  }

  @Override
  public boolean pauseTicking() {
    return false;
  }

  @Override
  public long getTickRate() {
    return 40;
  }

  @Override
  public void onTick() {
    if (world == null || world.isRemote) {
      return;
    }

    // Validate thermostat link (can be linked to primary or zone thermostat)
    if (linkedThermostatPos != null && world.isBlockLoaded(linkedThermostatPos)) {
      TileEntity te = world.getTileEntity(linkedThermostatPos);
      if (!(te instanceof TileEntityHvacThermostat)
          && !(te instanceof TileEntityHvacZoneThermostat)) {
        clearLink();
      }
    }

    // Notify temperature manager if activation state changed
    boolean active = isHvacActive();
    if (active != wasActive) {
      wasActive = active;
      HvacTemperatureManager.invalidateChunk(world, pos.getX() >> 4, pos.getZ() >> 4);
    }
  }

  // endregion

  // region Link Management

  /**
   * Links this vent relay to a thermostat. Validates distance using the thermostat's
   * max vent link distance (derived from the strongest unit connected to it).
   */
  public boolean setLinkedThermostat(BlockPos thermostatPos, int maxDistance) {
    if (thermostatPos != null) {
      if (pos.getDistance(thermostatPos.getX(), thermostatPos.getY(),
          thermostatPos.getZ()) > maxDistance) {
        return false;
      }
    }
    this.linkedThermostatPos = thermostatPos;
    this.currentContribution = 0.0f;
    if (world != null) {
      markDirtySync(world, pos);
    }
    return true;
  }

  public void clearLink() {
    this.linkedThermostatPos = null;
    this.currentContribution = 0.0f;
    if (world != null) {
      markDirtySync(world, pos);
    }
  }

  public BlockPos getLinkedThermostatPos() {
    return linkedThermostatPos;
  }

  // endregion

  // region NBT

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.getBoolean(NBT_HAS_LINK)) {
      linkedThermostatPos = new BlockPos(
          compound.getInteger(NBT_LINK_X),
          compound.getInteger(NBT_LINK_Y),
          compound.getInteger(NBT_LINK_Z));
    } else {
      linkedThermostatPos = null;
    }
    this.currentContribution = compound.getFloat("contribution");
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (linkedThermostatPos != null) {
      compound.setBoolean(NBT_HAS_LINK, true);
      compound.setInteger(NBT_LINK_X, linkedThermostatPos.getX());
      compound.setInteger(NBT_LINK_Y, linkedThermostatPos.getY());
      compound.setInteger(NBT_LINK_Z, linkedThermostatPos.getZ());
    } else {
      compound.setBoolean(NBT_HAS_LINK, false);
    }
    compound.setFloat("contribution", currentContribution);
    return compound;
  }

  // endregion
}
