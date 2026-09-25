package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The number an aisle sign shows, 1 to {@link BlockAisleSign#NUMBERS}. It is kept here because
 * the metadata holds only the facing; the sign's {@code getActualState} reads it, and a change
 * syncs to the players in range, whose clients redraw the sign.
 *
 * @since 2026.9
 */
public class TileEntityAisleSign extends AbstractTileEntity {

  private static final String KEY_NUMBER = "n";

  private int number = 1;

  /**
   * The number it shows.
   *
   * @return 1 to {@link BlockAisleSign#NUMBERS}
   */
  public int getNumber() {
    return number;
  }

  /**
   * Sets the number and tells the players in range (server side).
   *
   * @param number 1 to {@link BlockAisleSign#NUMBERS}
   */
  public void setNumber(int number) {
    this.number = clamp(number);
    if (world != null && !world.isRemote) {
      markDirty();
      syncServerToClient(world);
    }
  }

  private static int clamp(int n) {
    return Math.max(1, Math.min(BlockAisleSign.NUMBERS, n));
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    number = compound.hasKey(KEY_NUMBER) ? clamp(compound.getByte(KEY_NUMBER)) : 1;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setByte(KEY_NUMBER, (byte) number);
    return compound;
  }

  /** Only the number reaches the baked model, so only a new number rebuilds the section. */
  @Override
  protected long getBakedModelKey() {
    return number;
  }
}
