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
   * Residual decay factor applied when the thermostat stops calling. Instead of snapping
   * to zero, the vent contribution decays by this factor each thermostat tick (every 2s).
   * At 0.97: retains 74% after 20s, 55% after 40s, 30% after 1 min, ~0 after 2 min.
   * Simulates lingering conditioned air in the room after the system shuts off.
   */
  private static final float RESIDUAL_DECAY_FACTOR = 0.97f;

  /**
   * Threshold below which the residual contribution is zeroed out entirely.
   */
  private static final float RESIDUAL_ZERO_THRESHOLD = 0.5f;

  /**
   * Set by the thermostat when the system is calling. Includes ramp-up factor.
   * When the thermostat stops calling (pushes 0), the contribution decays gradually
   * via {@link #RESIDUAL_DECAY_FACTOR} instead of snapping to zero.
   */
  private float currentContribution = 0.0f;

  /**
   * Called by the thermostat to set this vent's current temperature contribution.
   * The value already accounts for ramp-up factor and vent relay strength.
   *
   * <p>When the thermostat stops calling (new contribution near zero), the vent applies
   * residual decay instead of zeroing instantly. This simulates conditioned air lingering
   * in the room and prevents rapid thermostat cycling.</p>
   */
  public void setContribution(float contribution) {
    float oldContribution = this.currentContribution;

    if (Math.abs(contribution) < 0.1f && Math.abs(oldContribution) > RESIDUAL_ZERO_THRESHOLD) {
      // Thermostat stopped calling — apply residual decay instead of zeroing.
      this.currentContribution *= RESIDUAL_DECAY_FACTOR;
      if (Math.abs(this.currentContribution) < RESIDUAL_ZERO_THRESHOLD) {
        this.currentContribution = 0.0f;
      }
    } else {
      this.currentContribution = contribution;
    }

    if (Math.abs(oldContribution - this.currentContribution) > 0.1f) {
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
