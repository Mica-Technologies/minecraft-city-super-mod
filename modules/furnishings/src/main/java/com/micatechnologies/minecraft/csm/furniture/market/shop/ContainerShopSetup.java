package com.micatechnologies.minecraft.csm.furniture.market.shop;

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
 * A shop's setup screen's slots: the template (the item it sells, slot 0), its 27 slots of stock
 * laid out as a three-row chest's, and the player's inventory below. Stock takes only the
 * template's item; the template slot holds one.
 *
 * <p>On the client the shop's slots are backed by stand-ins (the client is never sent the stock),
 * the stock stand-in refusing what the shop would. Only a shop's managers may keep it open
 * ({@link #canInteractWith}), checked again every tick.</p>
 *
 * @since 2026.10
 */
public class ContainerShopSetup extends Container {

  /** Where the panel to the right of the chest begins. */
  public static final int PANEL_X = 176;
  /** The template slot's position. */
  public static final int TEMPLATE_X = PANEL_X + 8;
  public static final int TEMPLATE_Y = 18;

  private static final int STOCK_END = 1 + TileEntityShop.STOCK_SLOTS;

  private final TileEntityShop shop;
  private final boolean server;

  /**
   * Constructs the container.
   *
   * @param playerInventory the player's inventory
   * @param shop            the shop
   * @param server          whether this is the server's container (the real slots)
   */
  public ContainerShopSetup(InventoryPlayer playerInventory, TileEntityShop shop,
      boolean server) {
    this.shop = shop;
    this.server = server;
    IItemHandler template = server ? shop.getTemplateSlot() : new ItemStackHandler(1);
    IItemHandler stock = server ? shop.getItems() : new ItemStackHandler(TileEntityShop.STOCK_SLOTS) {
      @Override
      public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return shop.matches(stack);
      }
    };
    addSlotToContainer(new SlotItemHandler(template, 0, TEMPLATE_X, TEMPLATE_Y) {
      @Override
      public int getSlotStackLimit() {
        return 1;
      }

      @Override
      public int getItemStackLimit(@Nonnull ItemStack stack) {
        return 1;
      }
    });
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 9; c++) {
        addSlotToContainer(new SlotItemHandler(stock, c + r * 9, 8 + c * 18, 18 + r * 18) {
          @Override
          public boolean isItemValid(@Nonnull ItemStack stack) {
            return shop.matches(stack);
          }
        });
      }
    }
    // Where a three-row chest puts the player's inventory.
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 9; c++) {
        addSlotToContainer(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
      }
    }
    for (int c = 0; c < 9; c++) {
      addSlotToContainer(new Slot(playerInventory, c, 8 + c * 18, 142));
    }
  }

  public TileEntityShop getShop() {
    return shop;
  }

  @Override
  public boolean canInteractWith(@Nonnull EntityPlayer player) {
    if (shop.isInvalid() || player.getDistanceSq(shop.getPos().getX() + 0.5,
        shop.getPos().getY() + 0.5, shop.getPos().getZ() + 0.5) > 64.0) {
      return false;
    }
    return !server || ShopPermissions.mayManage(player, shop);
  }

  /**
   * Shift-click: from the shop into the inventory; from the inventory into the template slot if
   * it is empty, else into stock.
   */
  @Override
  @Nonnull
  public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
    Slot slot = inventorySlots.get(index);
    if (slot == null || !slot.getHasStack()) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = slot.getStack();
    ItemStack copy = stack.copy();
    if (index < STOCK_END) {
      if (!mergeItemStack(stack, STOCK_END, inventorySlots.size(), true)) {
        return ItemStack.EMPTY;
      }
    } else {
      boolean moved = false;
      if (!inventorySlots.get(0).getHasStack()) {
        moved = mergeItemStack(stack, 0, 1, false);
      }
      if (!stack.isEmpty() && shop.matches(stack)) {
        moved |= mergeItemStack(stack, 1, STOCK_END, false);
      }
      if (!moved) {
        return ItemStack.EMPTY;
      }
    }
    if (stack.isEmpty()) {
      slot.putStack(ItemStack.EMPTY);
    } else {
      slot.onSlotChanged();
    }
    return copy;
  }
}
