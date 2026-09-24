package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The record on a stereo's turntable, kept as a jukebox keeps its record: saved with the block,
 * dropped when it is taken off or the stereo is broken. Clients never need it (whether there is
 * one is in the block's state, and the music is started by the server's record event), so it is
 * left out of the sync.
 *
 * @since 2026.9
 */
public class TileEntityStereo extends AbstractTileEntity {

  private static final String KEY_RECORD = "r";

  private ItemStack record = ItemStack.EMPTY;

  /**
   * The record on the turntable.
   *
   * @return the record, or an empty stack
   */
  @Nonnull
  public ItemStack getRecord() {
    return record;
  }

  /**
   * Puts {@code record} on the turntable (or an empty stack to take it off).
   *
   * @param record the record
   */
  public void setRecord(@Nonnull ItemStack record) {
    this.record = record;
    markDirty();
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    record = compound.hasKey(KEY_RECORD) ? new ItemStack(compound.getCompoundTag(KEY_RECORD))
        : ItemStack.EMPTY;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (!record.isEmpty()) {
      compound.setTag(KEY_RECORD, record.writeToNBT(new NBTTagCompound()));
    }
    return compound;
  }

  /** The record stays on the server. */
  @Override
  @Nonnull
  public NBTTagCompound getUpdateTag() {
    NBTTagCompound tag = super.getUpdateTag();
    tag.removeTag(KEY_RECORD);
    return tag;
  }

  /** Nothing the client draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
