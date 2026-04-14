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
   * Residual decay factor for the direct heater/cooler contribution after the thermostat
   * stops calling. At 0.993 per 1s tick: retains 87% after 20s, 66% after 1 min,
   * 35% after 2.5 min, ~5% (cutoff) after ~7 minutes. Tuned to match the vent relay's
   * ~7-minute full decay timeline despite the heater's faster tick rate (1s vs 2s).
   */
  private static final float RESIDUAL_DECAY_FACTOR = 0.993f;

  /**
   * Set to true when a linked thermostat is calling for this specific unit type to activate.
   * Transient — not persisted to disk. On restart, the thermostat re-evaluates on its next tick.
   */
  private boolean thermostatCalling = false;

  /** Whether this unit is linked to a thermostat (controls standalone vs thermostat mode). */
  private boolean linkedToThermostat = false;

  /**
   * Residual contribution multiplier (0.0 to 1.0). When the thermostat stops calling,
   * this decays gradually instead of snapping to zero, simulating residual heat/cold
   * from the unit. Resets to 1.0 when the thermostat starts calling again.
   */
  private float residualMultiplier = 0.0f;

  /**
   * Sets whether a linked thermostat is currently calling for this specific unit to activate.
   * When calling transitions from true to false, the residual multiplier begins decaying
   * instead of cutting the contribution instantly.
   */
  public void setThermostatCalling(boolean calling) {
    if (calling && !this.thermostatCalling) {
      // Just started calling — reset residual to full
      this.residualMultiplier = 1.0f;
    }
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

  // region IHvacUnit

  @Override
  public float getTemperatureContribution() {
    // When actively calling: full contribution. When residual is decaying: scaled down.
    if (linkedToThermostat && !thermostatCalling && residualMultiplier > 0.0f) {
      return 15.0F * residualMultiplier;
    }
    return 15.0F;
  }

  @Override
  public boolean isHvacActive() {
    if (world == null) {
      return false;
    }
    boolean hasPower = world.isBlockPowered(pos) || storedEnergy > 0;
    if (linkedToThermostat) {
      // Active when calling AND powered, OR when residual is still decaying
      return (hasPower && thermostatCalling) || residualMultiplier > 0.05f;
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

    // Decay residual contribution when thermostat has stopped calling.
    // The heater ticks every 20 game ticks (1 second), matching the vent's 2s decay rate
    // since the vent uses 0.97 per 2s and we use 0.97 per 1s (slightly faster, but the
    // heater's direct contribution is already wall-attenuated and smaller than vent output).
    if (linkedToThermostat && !thermostatCalling && residualMultiplier > 0.0f) {
      residualMultiplier *= RESIDUAL_DECAY_FACTOR;
      if (residualMultiplier < 0.05f) {
        residualMultiplier = 0.0f;
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
    this.thermostatCalling = compound.getBoolean("thermostatCalling");
    this.linkedToThermostat = compound.getBoolean("linkedToThermostat");
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_ENERGY_KEY, storedEnergy);
    compound.setBoolean("thermostatCalling", thermostatCalling);
    compound.setBoolean("linkedToThermostat", linkedToThermostat);
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
