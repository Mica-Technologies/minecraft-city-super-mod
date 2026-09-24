package com.micatechnologies.minecraft.csm.furniture.residential;

import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * A TV stand's or sideboard's slots over the player's inventory, laid out as a vanilla chest's
 * are (rows of nine at the chest's positions), so {@link GuiResidentialStorage} can draw it on
 * the chest texture.
 *
 * <p>On the client the storage's slots are backed by an empty stand-in of the right size: the
 * client's tile entity is never sent the contents, and the container sync fills the slots.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class ContainerResidentialStorage extends Container {

  private final TileEntityResidentialStorage storage;
  private final int rows;
  private final int boxSlots;

  /**
   * Constructs the container.
   *
   * @param playerInventory the player's inventory
   * @param storage         the storage's tile entity
   * @param slots           how many slots it has, a multiple of nine
   * @param server          whether this is the server's container (the real slots)
   */
  public ContainerResidentialStorage(InventoryPlayer playerInventory,
      TileEntityResidentialStorage storage, int slots, boolean server) {
    this.storage = storage;
    this.rows = Math.max(1, slots / 9);
    this.boxSlots = rows * 9;
    IItemHandler handler = server ? storage.getItems() : new ItemStackHandler(boxSlots);
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < 9; c++) {
        addSlotToContainer(new SlotItemHandler(handler, c + r * 9, 8 + c * 18, 18 + r * 18));
      }
    }
    // Where a vanilla chest of this many rows puts the player's inventory.
    int shift = (rows - 4) * 18;
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 9; c++) {
        addSlotToContainer(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18,
            103 + r * 18 + shift));
      }
    }
    for (int c = 0; c < 9; c++) {
      addSlotToContainer(new Slot(playerInventory, c, 8 + c * 18, 161 + shift));
    }
  }

  /**
   * How many rows of nine the storage shows.
   *
   * @return the rows
   */
  public int getRows() {
    return rows;
  }

  @Override
  public boolean canInteractWith(@Nonnull EntityPlayer player) {
    return !storage.isInvalid() && player.getDistanceSq(storage.getPos().getX() + 0.5,
        storage.getPos().getY() + 0.5, storage.getPos().getZ() + 0.5) <= 64.0;
  }

  /** Shift-click: from the storage into the inventory, or from the inventory into the storage. */
  @Override
  @Nonnull
  public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
    Slot slot = inventorySlots.get(index);
    if (slot == null || !slot.getHasStack()) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = slot.getStack();
    ItemStack copy = stack.copy();
    if (index < boxSlots) {
      if (!mergeItemStack(stack, boxSlots, inventorySlots.size(), true)) {
        return ItemStack.EMPTY;
      }
    } else if (!mergeItemStack(stack, 0, boxSlots, false)) {
      return ItemStack.EMPTY;
    }
    if (stack.isEmpty()) {
      slot.putStack(ItemStack.EMPTY);
    } else {
      slot.onSlotChanged();
    }
    return copy;
  }
}
