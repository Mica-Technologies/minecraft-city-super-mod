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
   * Residual decay factor applied when the thermostat stops calling (commands ≈ 0). At 0.93
   * per 2s tick the contribution decays:
   * <ul>
   *   <li>50% in ~20s</li>
   *   <li>95% in ~80s</li>
   *   <li>effectively zero by ~2 minutes</li>
   * </ul>
   * This simulates room-level thermal mass at a physically reasonable timescale: a player
   * walking into the room shortly after the system shuts off will still feel some lingering
   * warmth/coolness, then the room equilibrates back to baseline within a couple minutes.
   *
   * <p>Earlier versions used 0.99 (13-minute decay), which caused thermostat oscillation
   * because the vent kept "blowing" long after the thermostat said stop. The faster factor
   * here, combined with proportional-control output scaling at the thermostat, eliminates
   * that runaway without making rooms feel artificially cold.</p>
   */
  private static final float RESIDUAL_DECAY_FACTOR = 0.93f;

  /**
   * Threshold below which residual contribution snaps to exactly 0. Avoids leaving
   * sub-degree noise floating in the calculation indefinitely.
   */
  private static final float RESIDUAL_ZERO_THRESHOLD = 0.5f;

  /**
   * Set by the thermostat when the system is calling. Includes ramp-up factor and any
   * proportional-control modulation. When the thermostat stops calling (pushes 0), this
   * decays toward zero gradually rather than snapping — the lingering warmth/coolness
   * gives joining players a believable "the room was just heated" experience.
   */
  private float currentContribution = 0.0f;

  /**
   * Called by the thermostat to set this vent's current temperature contribution. The
   * commanded value already accounts for ramp, proportional control, vent strength, and
   * density bonus. When the command is ~zero (system idle) and the previous value was
   * meaningful, apply residual decay instead of snapping to zero. Otherwise overwrite
   * verbatim.
   */
  public void setContribution(float contribution) {
    float oldContribution = this.currentContribution;

    if (Math.abs(contribution) < 0.1f && Math.abs(oldContribution) > RESIDUAL_ZERO_THRESHOLD) {
      // Thermostat stopped calling — apply residual decay so the room cools gradually.
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

  /**
   * Skip ticking entirely when this vent has no thermostat link. Cosmetic-only placements
   * across the world (decorative ceilings, pure-aesthetic builds) then cost nothing per
   * game tick beyond the bare {@link #update()} entry. Once a thermostat links to the vent
   * via {@link #setLinkedThermostat}, ticking resumes so {@link #onTick()} can detect a
   * thermostat that gets destroyed out from under it.
   */
  @Override
  public boolean pauseTicking() {
    return linkedThermostatPos == null;
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
