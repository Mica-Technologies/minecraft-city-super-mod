package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * What a TV stand or sideboard holds: a chest's worth of slots behind its doors (nine for a TV
 * stand, eighteen for a sideboard), saved with the block and dropped when it is broken.
 * Hoppers and pipes reach the same slots through the item handler capability, on any side.
 *
 * <p>The contents never go to clients in the block's sync: the container the player opens
 * sends them to the one player looking.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class TileEntityResidentialStorage extends AbstractTileEntity {

  private static final String KEY_ITEMS = "i";

  private final ItemStackHandler items;

  /** Constructs an empty one, as the game does before loading a saved one. */
  public TileEntityResidentialStorage() {
    this(9);
  }

  /**
   * Constructs one with {@code slots} slots.
   *
   * @param slots how many slots
   */
  public TileEntityResidentialStorage(int slots) {
    items = new ItemStackHandler(slots) {
      @Override
      protected void onContentsChanged(int slot) {
        markDirty();
      }
    };
  }

  /**
   * The slots.
   *
   * @return the item handler
   */
  public ItemStackHandler getItems() {
    return items;
  }

  /**
   * Makes sure there are {@code slots} slots, keeping what they hold. A saved block brings its
   * own count; this covers one saved before a block's size was known.
   *
   * @param slots how many slots the block has
   */
  public void ensureSlots(int slots) {
    if (items.getSlots() >= slots) {
      return;
    }
    NBTTagCompound saved = items.serializeNBT();
    saved.setInteger("Size", slots);
    items.deserializeNBT(saved);
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(KEY_ITEMS)) {
      items.deserializeNBT(compound.getCompoundTag(KEY_ITEMS));
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setTag(KEY_ITEMS, items.serializeNBT());
    return compound;
  }

  /** The block's sync to clients: its position only, never the contents. */
  @Override
  @Nonnull
  public NBTTagCompound getUpdateTag() {
    NBTTagCompound tag = new NBTTagCompound();
    tag.setInteger("x", pos.getX());
    tag.setInteger("y", pos.getY());
    tag.setInteger("z", pos.getZ());
    return tag;
  }

  /**
   * Calls {@code action} with each stack held, for dropping them when the block is broken.
   *
   * @param action what to do with each
   */
  public void forEachItem(java.util.function.Consumer<ItemStack> action) {
    for (int s = 0; s < items.getSlots(); s++) {
      ItemStack st = items.getStackInSlot(s);
      if (!st.isEmpty()) {
        action.accept(st);
      }
    }
  }

  @Override
  public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
    return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
        || super.hasCapability(capability, facing);
  }

  @Override
  @Nullable
  public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
    if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
      return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(items);
    }
    return super.getCapability(capability, facing);
  }
}
