package com.micatechnologies.minecraft.csm.transit.platform;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * What a stepped station sign shows: the platform number of a number sign or a column's number
 * band, or which of the invented names a station name sign carries. It is kept here because the
 * metadata holds only the facing; the block's {@code getActualState} reads it (through
 * {@link PlatformSigns#valueAt}), and a change syncs to the players in range, whose clients
 * redraw the sign.
 *
 * @since 2026.9
 */
public class TileEntityPlatformSign extends AbstractTileEntity {

  /** The most any stepped sign counts to. */
  public static final int MAX = 99;

  private static final String KEY_VALUE = "n";

  private int value = 1;

  /**
   * What it shows.
   *
   * @return 1 to {@link #MAX}
   */
  public int getValue() {
    return value;
  }

  /**
   * Sets what it shows and tells the players in range (server side).
   *
   * @param value 1 to {@link #MAX}
   */
  public void setValue(int value) {
    this.value = clamp(value);
    if (world != null && !world.isRemote) {
      markDirty();
      syncServerToClient(world);
    }
  }

  private static int clamp(int n) {
    return Math.max(1, Math.min(MAX, n));
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    value = compound.hasKey(KEY_VALUE) ? clamp(compound.getByte(KEY_VALUE)) : 1;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setByte(KEY_VALUE, (byte) value);
    return compound;
  }

  /** Only the value reaches the baked model, so only a new value rebuilds the section. */
  @Override
  protected long getBakedModelKey() {
    return value;
  }
}
