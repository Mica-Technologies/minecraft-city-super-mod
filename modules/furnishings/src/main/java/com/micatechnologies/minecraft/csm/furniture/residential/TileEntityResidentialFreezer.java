package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ITickable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * A chest freezer's slots, which freeze water: a water bottle that has sat in a slot for
 * {@link #FREEZE_SECONDS} seconds becomes a block of ice, a water bucket packed ice (a bucket
 * is a whole source of water, a bottle a third of one), and the empty bottle or bucket goes
 * into another slot. While there is no room for it the water waits, unfrozen.
 *
 * <p>Checked once a second on the server. Each slot's time is saved with the block.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class TileEntityResidentialFreezer extends TileEntityResidentialStorage
    implements ITickable {

  /** How long water takes to freeze, in seconds. */
  public static final int FREEZE_SECONDS = 30;
  private static final String KEY_FROZEN = "f";

  private int[] seconds = new int[0];

  /** Constructs an empty one, as the game does before loading a saved one. */
  public TileEntityResidentialFreezer() {
    super(27);
  }

  /**
   * Constructs one with {@code slots} slots.
   *
   * @param slots how many slots
   */
  public TileEntityResidentialFreezer(int slots) {
    super(slots);
  }

  @Override
  public void update() {
    if (world == null || world.isRemote
        || Math.floorMod(world.getTotalWorldTime() + pos.hashCode(), 20L) != 0) {
      return;
    }
    ItemStackHandler items = getItems();
    if (seconds.length != items.getSlots()) {
      int[] resized = new int[items.getSlots()];
      System.arraycopy(seconds, 0, resized, 0, Math.min(seconds.length, resized.length));
      seconds = resized;
    }
    for (int s = 0; s < items.getSlots(); s++) {
      ItemStack stack = items.getStackInSlot(s);
      ItemStack product = frozen(stack);
      if (product.isEmpty()) {
        seconds[s] = 0;
        continue;
      }
      if (++seconds[s] < FREEZE_SECONDS) {
        continue;
      }
      ItemStack empty = new ItemStack(stack.getItem() == Items.WATER_BUCKET ? Items.BUCKET
          : Items.GLASS_BOTTLE);
      if (roomFor(items, s, empty)) {
        items.setStackInSlot(s, product);
        insertElsewhere(items, s, empty);
        seconds[s] = 0;
      } else {
        seconds[s] = FREEZE_SECONDS;
      }
    }
  }

  /** What {@code stack} freezes into, or empty if it does not freeze. */
  private static ItemStack frozen(ItemStack stack) {
    Item item = stack.getItem();
    if (item == Items.WATER_BUCKET) {
      return new ItemStack(Blocks.PACKED_ICE);
    }
    if (item == Items.POTIONITEM && PotionUtils.getPotionFromItem(stack) == PotionTypes.WATER) {
      return new ItemStack(Blocks.ICE);
    }
    return ItemStack.EMPTY;
  }

  /** Whether {@code empty} fits in a slot other than {@code except}. */
  private static boolean roomFor(ItemStackHandler items, int except, ItemStack empty) {
    for (int s = 0; s < items.getSlots(); s++) {
      if (s != except && items.insertItem(s, empty, true).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private static void insertElsewhere(ItemStackHandler items, int except, ItemStack empty) {
    ItemStack left = empty;
    for (int s = 0; s < items.getSlots() && !left.isEmpty(); s++) {
      if (s != except) {
        left = items.insertItem(s, left, false);
      }
    }
    if (!left.isEmpty()) {
      // roomFor said it fitted; this is only reached if the slots changed in between.
      ItemHandlerHelper.insertItemStacked(items, left, false);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    seconds = compound.getIntArray(KEY_FROZEN);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    compound.setIntArray(KEY_FROZEN, seconds);
    return compound;
  }
}
