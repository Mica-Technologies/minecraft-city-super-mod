package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Remembers whether redstone powered a block when it last looked, for a block whose metadata has
 * no room left for it (the fireplace: facing, block and fire; the ceiling fan: facing, light and
 * fan), so that only a change of power switches it and a click still can between changes. It
 * never ticks and never syncs anything a client draws.
 *
 * @since 2026.9
 */
public class TileEntityPowerMemory extends AbstractTileEntity {

  private static final String KEY_POWERED = "p";

  private boolean powered;

  /**
   * Whether redstone powered the block when it was last looked at.
   *
   * @return whether it was powered
   */
  public boolean wasPowered() {
    return powered;
  }

  /**
   * Remembers whether redstone powers the block.
   *
   * @param powered whether it is powered
   */
  public void setPowered(boolean powered) {
    this.powered = powered;
    markDirty();
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    powered = compound.getBoolean(KEY_POWERED);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setBoolean(KEY_POWERED, powered);
    return compound;
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
