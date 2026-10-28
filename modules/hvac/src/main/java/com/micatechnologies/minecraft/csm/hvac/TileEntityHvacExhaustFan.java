package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * A rooftop exhaust fan (#314): the lab exhaust fan's master, the upblast and the mushroom fan.
 *
 * <p>Powered as a heater is (redstone, or Forge Energy drawn while it runs) and linked to a
 * primary thermostat, it exhausts its capacity through the exhaust grilles of that system, which
 * {@link HvacSystemControl} hands out once a second. It is not an anchor of the simulation: it
 * needs no room of its own, so an unlinked fan, or one a world has thousands of, costs nothing.
 * It does not tick.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class TileEntityHvacExhaustFan extends AbstractTileEntity implements IEnergyStorage {

  private static final String NBT_ENERGY_KEY = "energy";
  private static final int MAX_ENERGY = 1000;
  private static final int MAX_RECEIVE = 100;
  /** Energy per game tick while running. */
  private static final int ENERGY_PER_TICK = 5;

  private int storedEnergy = 0;

  /** Air exhausted in the last step, cubic feet a minute; for {@code /csmhvac}. */
  transient float exhaustCfm;

  /** The fan's capacity, from its block. */
  public int getCapacityCfm() {
    Block b = getBlockType();
    return b instanceof BlockHvacExhaustFan ? ((BlockHvacExhaustFan) b).getCapacityCfm() : 0;
  }

  /** Whether the fan has power: redstone, or stored energy. */
  public boolean hasPower() {
    return world != null && (world.isBlockPowered(pos) || storedEnergy > 0);
  }

  /** Takes this step's airflow, drawing the energy for it when not on redstone. */
  void applyExhaust(float cfm) {
    exhaustCfm = cfm;
    if (cfm > 0 && world != null && !world.isBlockPowered(pos) && storedEnergy > 0) {
      storedEnergy = Math.max(0, storedEnergy - ENERGY_PER_TICK * HvacThermalWorld.STEP_TICKS);
      markDirty();
    }
  }

  // region NBT

  @Override
  public void readNBT(NBTTagCompound compound) {
    storedEnergy = compound.getInteger(NBT_ENERGY_KEY);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_ENERGY_KEY, storedEnergy);
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
    int received = Math.min(MAX_RECEIVE, Math.min(MAX_ENERGY - storedEnergy, maxReceive));
    if (!simulate) {
      storedEnergy += received;
      markDirty();
    }
    return received;
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
}
