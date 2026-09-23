package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * The cabinet heater, and the base of every heating and cooling unit.
 *
 * <p>A unit runs on redstone or Forge Energy. Linked to a thermostat, it runs as hard as its
 * system asks; unlinked and powered, it holds the room it stands in at a comfortable temperature
 * by itself, like a space heater. Energy is drawn in proportion to how hard it runs, so an idle
 * unit on a battery keeps its charge.</p>
 *
 * <p>It is an anchor of the thermal simulation: an unlinked unit needs to know its room, and a
 * linked one with no vents in its system delivers into it.</p>
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class TileEntityHvacHeater extends AbstractTileEntity implements IHvacUnit, IEnergyStorage {

  private static final String NBT_ENERGY_KEY = "energy";
  private static final String NBT_OUTPUT_KEY = "out";
  private static final int MAX_ENERGY = 1000;
  private static final int MAX_RECEIVE = 100;

  /** Energy per game tick at full output. */
  private static final int ENERGY_PER_TICK = 10;

  /** Heat per second at full output: enough for a 14 x 14 room against a -50°F winter. */
  public static final float CABINET_CAPACITY = 300.0f;

  private int storedEnergy = 0;

  /** Output as a fraction of capacity, 0..1, as last run. Synced for display. */
  private float output;

  /** Simulation step in which a system last claimed this unit; otherwise it runs standalone. */
  transient long claimStep = -1;

  // region IHvacUnit

  @Override
  public float getHeatCapacity() {
    return CABINET_CAPACITY;
  }

  @Override
  public boolean isCoolingUnit() {
    return false;
  }

  /** Output as a fraction of capacity, 0..1. */
  public float getOutput() {
    return output;
  }

  /** Whether the unit is running at all. */
  public boolean isRunning() {
    return output > 0.0f;
  }

  /** Whether the unit has power: redstone, or stored energy. */
  public boolean hasPower() {
    return world != null && (world.isBlockPowered(pos) || storedEnergy > 0);
  }

  /**
   * Sets the output for the coming second and draws the energy for it. Redstone power is free;
   * otherwise energy is drawn in proportion to output.
   */
  void applyOutput(float fraction) {
    float f = Math.max(0.0f, Math.min(1.0f, fraction));
    if (f > 0.0f && world != null && !world.isBlockPowered(pos) && storedEnergy > 0) {
      int use = (int) Math.ceil(ENERGY_PER_TICK * HvacThermalWorld.STEP_TICKS * f);
      storedEnergy = Math.max(0, storedEnergy - use);
      markDirty();
    }
    boolean changed = Math.round(f * 20) != Math.round(output * 20);
    output = f;
    if (changed && world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  // endregion

  // region Simulation anchor

  @Override
  public void onLoad() {
    super.onLoad();
    HvacThermalWorld w = HvacThermal.get(world);
    if (w != null) {
      w.registerAnchor(pos, ThermalAnchor.UNIT, Float.NaN);
    }
  }

  @Override
  public void onChunkUnload() {
    unregister();
    super.onChunkUnload();
  }

  @Override
  public void invalidate() {
    unregister();
    super.invalidate();
  }

  private void unregister() {
    HvacThermalWorld w = HvacThermal.peek(world);
    if (w != null) {
      w.unregisterAnchor(pos);
    }
  }

  // endregion

  // region NBT

  @Override
  public void readNBT(NBTTagCompound compound) {
    this.storedEnergy = compound.getInteger(NBT_ENERGY_KEY);
    this.output = compound.getFloat(NBT_OUTPUT_KEY);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_ENERGY_KEY, storedEnergy);
    compound.setFloat(NBT_OUTPUT_KEY, output);
    return compound;
  }

  /** No baked model reads this tile entity. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
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
  public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
    return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
  }

  @Nullable
  @Override
  public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
    return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(this)
        : super.getCapability(capability, facing);
  }

  // endregion
}
