package com.micatechnologies.minecraft.csm.buildingmaterials;

import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * The Door Workshop's slots and the player's inventory, kept in step between server and client.
 * Material slots take only blocks a door can be made of, the edit slot only custom doors, and
 * nothing can be put into the output.
 *
 * @version 1.0
 * @since 2026.9
 */
public class ContainerDoorWorkshop extends Container {

  /** Where the workshop's slots sit on the screen, in its coordinates. SHARED with the GUI. */
  static final int MATERIAL_X = 8;
  static final int[] MATERIAL_Y = {18, 40, 62};
  static final int RIGHT_X = 222;
  static final int OUTPUT_Y = 18;
  static final int EDIT_Y = 72;
  static final int INVENTORY_X = 43;
  static final int INVENTORY_Y = 154;

  private final TileEntityDoorWorkshop workshop;

  /**
   * Constructs a {@link ContainerDoorWorkshop}.
   *
   * @param playerInventory the player's inventory
   * @param workshop        the workshop
   *
   * @since 1.0
   */
  public ContainerDoorWorkshop(InventoryPlayer playerInventory, TileEntityDoorWorkshop workshop) {
    this.workshop = workshop;
    ItemStackHandler inv = workshop.getInventory();
    for (int i = 0; i < 3; i++) {
      addSlotToContainer(new SlotItemHandler(inv, i, MATERIAL_X, MATERIAL_Y[i]) {
        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
          return TileEntityDoorWorkshop.isMaterial(stack);
        }
      });
    }
    addSlotToContainer(new SlotItemHandler(inv, TileEntityDoorWorkshop.EDIT, RIGHT_X, EDIT_Y) {
      @Override
      public boolean isItemValid(@Nonnull ItemStack stack) {
        return stack.getItem() instanceof ItemCustomDoor;
      }
    });
    addSlotToContainer(new SlotItemHandler(inv, TileEntityDoorWorkshop.OUTPUT, RIGHT_X,
        OUTPUT_Y) {
      @Override
      public boolean isItemValid(@Nonnull ItemStack stack) {
        return false;
      }
    });
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, INVENTORY_X + col * 18,
            INVENTORY_Y + row * 18));
      }
    }
    for (int col = 0; col < 9; col++) {
      addSlotToContainer(new Slot(playerInventory, col, INVENTORY_X + col * 18,
          INVENTORY_Y + 58));
    }
  }

  public TileEntityDoorWorkshop getWorkshop() {
    return workshop;
  }

  @Override
  public boolean canInteractWith(@Nonnull EntityPlayer playerIn) {
    return !workshop.isInvalid() && playerIn.getDistanceSq(workshop.getPos().getX() + 0.5,
        workshop.getPos().getY() + 0.5, workshop.getPos().getZ() + 0.5) <= 64.0;
  }

  /**
   * Shift-click: from the workshop into the player's inventory; from the inventory into the first
   * workshop slot that takes it (a door to the edit slot, a block to the first material slot with
   * room).
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) {
    Slot slot = inventorySlots.get(index);
    if (slot == null || !slot.getHasStack()) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = slot.getStack();
    ItemStack copy = stack.copy();
    int workshopSlots = 5;
    if (index < workshopSlots) {
      if (!mergeItemStack(stack, workshopSlots, inventorySlots.size(), true)) {
        return ItemStack.EMPTY;
      }
    } else if (stack.getItem() instanceof ItemCustomDoor) {
      if (!mergeItemStack(stack, 3, 4, false)) {
        return ItemStack.EMPTY;
      }
    } else if (TileEntityDoorWorkshop.isMaterial(stack)) {
      if (!mergeItemStack(stack, 0, 3, false)) {
        return ItemStack.EMPTY;
      }
    } else {
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
