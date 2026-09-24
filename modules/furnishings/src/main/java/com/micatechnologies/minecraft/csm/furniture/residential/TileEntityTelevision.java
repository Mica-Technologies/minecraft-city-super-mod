package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The channel a TV is on. It is kept here rather than in the metadata, which the facing and the
 * block of a two-block TV fill; the TV's {@code getActualState} reads it, and a change syncs to
 * the players watching, whose clients redraw the screen from the new state.
 *
 * @since 2026.9
 */
public class TileEntityTelevision extends AbstractTileEntity {

  private static final String KEY_CHANNEL = "ch";

  private TvChannel channel = TvChannel.OFF;

  /**
   * The channel it shows.
   *
   * @return the channel
   */
  public TvChannel getChannel() {
    return channel;
  }

  /**
   * Switches it to {@code channel} and tells the players in range (server side).
   *
   * @param channel the channel
   */
  public void setChannel(TvChannel channel) {
    this.channel = channel;
    if (world != null && !world.isRemote) {
      markDirty();
      syncServerToClient(world);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    channel = TvChannel.byOrdinal(compound.getByte(KEY_CHANNEL));
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setByte(KEY_CHANNEL, (byte) channel.ordinal());
    return compound;
  }

  /** Only the channel reaches the baked model, so only a new channel rebuilds the section. */
  @Override
  protected long getBakedModelKey() {
    return channel.ordinal();
  }
}
