package com.micatechnologies.minecraft.csm.powergrid.fe;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Tile entity for blocks that consume Forge Energy. Accepts energy from adjacent producers via
 * the Forge Energy capability, tracks stored energy, and outputs a redstone signal when the
 * energy threshold is met.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class TileEntityForgeEnergyConsumer extends AbstractTickableTileEntity implements
    IEnergyStorage {

  private static final int DEFAULT_ENERGY_CONSUME = 6;
  private static final int DEFAULT_TICK_RATE = 40;
  private static final int DEFAULT_STORED_ENERGY_MAX = 24;
  private static final String NBT_ENERGY_KEY = "energy";
  private static final String NBT_ENERGY_CONSUME_KEY = "energyConsume";
  private static final String NBT_TICK_RATE_KEY = "tickRate";
  private static final String NBT_STORED_ENERGY_MAX_KEY = "storedEnergyMax";
  private int energyConsume = DEFAULT_ENERGY_CONSUME;
  private int configuredTickRate = DEFAULT_TICK_RATE;
  private int storedEnergyMax = DEFAULT_STORED_ENERGY_MAX;
  private int storedEnergy;

  /**
   * Abstract method which must be implemented to process the reading of the tile entity's NBT data
   * from the supplied NBT tag compound.
   *
   * @param compound the NBT tag compound to read the tile entity's NBT data from
   */
  @Override
  public void readNBT(NBTTagCompound compound) {
    this.storedEnergy = compound.getInteger(NBT_ENERGY_KEY);
    if (compound.hasKey(NBT_ENERGY_CONSUME_KEY)) {
      this.energyConsume = compound.getInteger(NBT_ENERGY_CONSUME_KEY);
    }
    if (compound.hasKey(NBT_TICK_RATE_KEY)) {
      this.configuredTickRate = compound.getInteger(NBT_TICK_RATE_KEY);
    }
    if (compound.hasKey(NBT_STORED_ENERGY_MAX_KEY)) {
      this.storedEnergyMax = compound.getInteger(NBT_STORED_ENERGY_MAX_KEY);
    }
  }

  /**
   * Abstract method which must be implemented to return the NBT tag compound with the tile entity's
   * NBT data.
   *
   * @param compound the NBT tag compound to write the tile entity's NBT data to
   *
   * @return the NBT tag compound with the tile entity's NBT data
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_ENERGY_KEY, storedEnergy);
    compound.setInteger(NBT_ENERGY_CONSUME_KEY, energyConsume);
    compound.setInteger(NBT_TICK_RATE_KEY, configuredTickRate);
    compound.setInteger(NBT_STORED_ENERGY_MAX_KEY, storedEnergyMax);
    return compound;
  }

  @Override
  public boolean hasCapability(Capability<?> p_hasCapability_1_,
      @Nullable
      EnumFacing p_hasCapability_2_) {
    return p_hasCapability_1_ == CapabilityEnergy.ENERGY ||
        super.hasCapability(p_hasCapability_1_, p_hasCapability_2_);
  }

  @Nullable
  @Override
  public <T> T getCapability(Capability<T> p_getCapability_1_,
      @Nullable
      EnumFacing p_getCapability_2_) {
    return p_getCapability_1_ == CapabilityEnergy.ENERGY ?
        CapabilityEnergy.ENERGY.cast(this) :
        super.getCapability(p_getCapability_1_, p_getCapability_2_);
  }

  @Override
  public int receiveEnergy(int i, boolean b) {
    // Receive energy from the grid (up to max capacity)
    int energyReceived = Math.min(storedEnergyMax - storedEnergy, i);
    storedEnergy += energyReceived;
    return energyReceived;
  }

  @Override
  public int extractEnergy(int i, boolean b) {
    return 0;
  }

  @Override
  public int getEnergyStored() {
    return storedEnergy;
  }

  @Override
  public int getMaxEnergyStored() {
    return storedEnergyMax;
  }

  @Override
  public boolean canExtract() {
    return false;
  }

  @Override
  public boolean canReceive() {
    return true;
  }

  /**
   * Abstract method which must be implemented to return a boolean indicating if the tile entity
   * should also tick on the client side. By default, the tile entity will always tick on the server
   * side, and in the event of singleplayer/local mode, the host client is considered the server.
   *
   * @return a boolean indicating if the tile entity should also tick on the client side
   */
  @Override
  public boolean doClientTick() {
    return false;
  }

  /**
   * Abstract method which must be implemented to return a boolean indicating if the tile entity
   * ticking should be paused. If the tile entity is paused, the tick event will not be called.
   *
   * @return a boolean indicating if the tile entity ticking should be paused
   */
  @Override
  public boolean pauseTicking() {
    return false;
  }

  /**
   * Abstract method which must be implemented to return the tick rate of the tile entity.
   *
   * @return the tick rate of the tile entity
   */
  @Override
  public long getTickRate() {
    return configuredTickRate;
  }

  /**
   * Abstract method which must be implemented to handle the tick event of the tile entity.
   */
  @Override
  public void onTick() {
    try {
      // Get block and world information
      BlockPos blockPos = getPos();
      IBlockState blockState = world.getBlockState(blockPos);

      // Consume power from Forge Energy (true if power, false if none)
      boolean isGridPowered = consumeEnergy(energyConsume);

      // Check if block is already outputting power
      PropertyBool blockPoweredProperty = BlockForgeEnergyToRedstone.POWERED;
      boolean isBlockPowered = blockState.getValue(blockPoweredProperty);

      // Update block power state if grid power does not match
      if (isBlockPowered != isGridPowered) {
        world.setBlockState(blockPos, blockState.withProperty(blockPoweredProperty, isGridPowered),
            3);
      }
    } catch (NullPointerException | ClassCastException e) {
      Csm.getLogger().error("Error ticking Forge energy consumer at {}", getPos(), e);
    }
  }

  public boolean consumeEnergy(int i) {
    boolean consumed = false;
    if (storedEnergy >= i) {
      storedEnergy -= i;
      consumed = true;
      markDirty();
    }
    return consumed;
  }
}
