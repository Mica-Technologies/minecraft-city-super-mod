package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Tile entity for the HVAC heater block. Consumes Forge Energy and contributes positive temperature
 * when active (redstone-powered or has stored FE). Subclassed by {@link TileEntityHvacCooler} for
 * cooling behavior.
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class TileEntityHvacHeater extends AbstractTickableTileEntity
    implements IHvacUnit, IEnergyStorage {

  private static final String NBT_ENERGY_KEY = "energy";
  private static final int MAX_ENERGY = 1000;
  private static final int MAX_RECEIVE = 100;
  private static final int ENERGY_PER_TICK = 10;

  private int storedEnergy = 0;
  private boolean wasActive = false;

  /**
   * Set to true when a linked thermostat is calling for this specific unit type to activate.
   * Transient — not persisted to disk. On restart, the thermostat re-evaluates on its next tick.
   */
  private boolean thermostatCalling = false;

  /** Whether this unit is linked to a thermostat (controls standalone vs thermostat mode). */
  private boolean linkedToThermostat = false;

  /**
   * Sets whether a linked thermostat is currently calling for this specific unit to activate.
   * Snaps to the commanded state — the unit either contributes or it doesn't, no residual
   * decay. The "room cools down slowly after AC stops" effect is simulated via the
   * thermostat's currentTemperature smoothing (THERMAL_BLEND_FACTOR), not here.
   */
  public void setThermostatCalling(boolean calling) {
    if (this.thermostatCalling != calling) {
      this.thermostatCalling = calling;
      if (world != null && !world.isRemote) {
        markDirtySync(world, pos, true);
      }
    }
  }

  /**
   * Marks this unit as linked to a thermostat. When linked, the unit only activates
   * when the thermostat specifically calls for it (not just when it has power).
   */
  public void setLinkedToThermostat(boolean linked) {
    this.linkedToThermostat = linked;
  }

  /** Exposed for subclasses (e.g. {@link TileEntityHvacCooler}) that override the
   *  contribution method but still need the linked/calling gating. */
  protected boolean isLinkedToThermostat() {
    return linkedToThermostat;
  }

  /** See {@link #isLinkedToThermostat()}. */
  protected boolean isThermostatCalling() {
    return thermostatCalling;
  }

  // region IHvacUnit

  @Override
  public final float getTemperatureContribution() {
    // Linked-but-not-calling units contribute nothing — no residual lingering. The
    // "room holds heat after the heater stops" feel is simulated by the thermostat's
    // currentTemperature smoothing, not by the heater pretending to still be on.
    //
    // Subclasses override {@link #getActiveContribution()} to change the value the unit
    // contributes when it IS running. The linked/calling gating lives here once so every
    // subclass picks it up automatically — previously, RTU subclasses overrode this
    // method and forgot to re-apply the gating, causing them to pump heat/cold even
    // while idle.
    if (linkedToThermostat && !thermostatCalling) {
      return 0.0F;
    }
    return getActiveContribution();
  }

  /**
   * Returns the temperature contribution this unit produces when actively running. The
   * sign indicates whether the unit heats (positive) or cools (negative). Default is the
   * cabinet heater's +15°F. Subclasses override this — never override
   * {@link #getTemperatureContribution()} directly, or the linked/calling gating will
   * be lost.
   */
  protected float getActiveContribution() {
    return 15.0F;
  }

  @Override
  public boolean isHvacActive() {
    if (world == null) {
      return false;
    }
    boolean hasPower = world.isBlockPowered(pos) || storedEnergy > 0;
    if (linkedToThermostat) {
      return hasPower && thermostatCalling;
    }
    // Standalone mode: power alone activates the unit
    return hasPower;
  }

  /**
   * Returns true if this unit has power (redstone or FE), regardless of thermostat state.
   * Used by the thermostat GUI to show power status of linked units.
   */
  public boolean hasPower() {
    if (world == null) {
      return false;
    }
    return world.isBlockPowered(pos) || storedEnergy > 0;
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
    return 20;
  }

  @Override
  public void onTick() {
    if (world == null || world.isRemote) {
      return;
    }

    boolean redstonePowered = world.isBlockPowered(pos);

    // Consume FE when active
    if (redstonePowered || storedEnergy > 0) {
      if (storedEnergy >= ENERGY_PER_TICK) {
        storedEnergy -= ENERGY_PER_TICK;
        markDirty();
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

  // region NBT

  @Override
  public void readNBT(NBTTagCompound compound) {
    this.storedEnergy = compound.getInteger(NBT_ENERGY_KEY);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_ENERGY_KEY, storedEnergy);
    return compound;
  }

  // endregion

  // region IEnergyStorage

  @Override
  public int receiveEnergy(int maxReceive, boolean simulate) {
    int energyReceived = Math.min(MAX_RECEIVE, Math.min(MAX_ENERGY - storedEnergy, maxReceive));
    if (!simulate) {
      storedEnergy += energyReceived;
      markDirty();
    }
    return energyReceived;
  }

  @Override
  public int extractEnergy(int maxExtract, boolean simulate) {
    return 0;
  }

  @Override
  public int getEnergyStored() {
    return storedEnergy;
  }

  @Override
  public int getMaxEnergyStored() {
    return MAX_ENERGY;
  }

  @Override
  public boolean canExtract() {
    return false;
  }

  @Override
  public boolean canReceive() {
    return true;
  }

  // endregion

  // region Capabilities

  @Override
  public boolean hasCapability(Capability<?> capability,
      @Nullable
      EnumFacing facing) {
    return capability == CapabilityEnergy.ENERGY ||
        super.hasCapability(capability, facing);
  }

  @Nullable
  @Override
  public <T> T getCapability(Capability<T> capability,
      @Nullable
      EnumFacing facing) {
    return capability == CapabilityEnergy.ENERGY ?
        CapabilityEnergy.ENERGY.cast(this) :
        super.getCapability(capability, facing);
  }

  // endregion
}
