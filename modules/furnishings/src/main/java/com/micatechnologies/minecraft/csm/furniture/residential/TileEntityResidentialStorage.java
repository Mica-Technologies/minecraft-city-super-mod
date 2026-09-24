package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * What a TV stand, sideboard, kitchen cabinet or refrigerator holds: a chest's worth of slots
 * behind its doors (nine for a TV stand, eighteen for a sideboard, 27 for a refrigerator), saved with the block and dropped when it is broken.
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
  /** How many players have it open, server side only; never saved. */
  private transient int users;

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

      @Override
      public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return accepts(stack);
      }

      @Override
      @Nonnull
      public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return accepts(stack) ? super.insertItem(slot, stack, simulate) : stack;
      }
    };
  }

  /**
   * Whether the slots take {@code stack}, from a player or a hopper alike. Anything, unless a
   * subclass holds only one kind of thing (the cookie jar).
   *
   * @param stack the stack
   *
   * @return true if it goes in
   */
  public boolean accepts(@Nonnull ItemStack stack) {
    return true;
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
   * A player opened it: the first to do so plays the block's opening sound.
   */
  public void opened() {
    if (users++ == 0) {
      playSound(true);
    }
  }

  /**
   * A player closed it: the last to do so plays the block's closing sound.
   */
  public void closed() {
    if (users > 0 && --users == 0) {
      playSound(false);
    }
  }

  private void playSound(boolean open) {
    if (world == null || world.isRemote) {
      return;
    }
    Block block = world.getBlockState(pos).getBlock();
    if (!(block instanceof IResidentialStorage)) {
      return;
    }
    IResidentialStorage storage = (IResidentialStorage) block;
    ICsmSound sound = open ? storage.getOpenSound() : storage.getCloseSound();
    SoundEvent event = sound == null ? null : sound.getSoundEvent();
    if (event != null) {
      world.playSound(null, pos, event, SoundCategory.BLOCKS, 0.7F,
          0.95F + world.rand.nextFloat() * 0.1F);
    }
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
