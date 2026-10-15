package com.micatechnologies.minecraft.csm.furniture.market.shop;

import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

/**
 * The counter screen's container: no slots at all. It exists because FML opens a screen from the
 * server only when there is a container to go with it; trades go by {@link ShopActionPacket}.
 *
 * @since 2026.10
 */
public class ContainerShopCounter extends Container {

  private final TileEntityShop shop;

  public ContainerShopCounter(TileEntityShop shop) {
    this.shop = shop;
  }

  public TileEntityShop getShop() {
    return shop;
  }

  @Override
  public boolean canInteractWith(@Nonnull EntityPlayer player) {
    return !shop.isInvalid() && player.getDistanceSq(shop.getPos().getX() + 0.5,
        shop.getPos().getY() + 0.5, shop.getPos().getZ() + 0.5) <= 64.0;
  }

  @Override
  @Nonnull
  public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
    return ItemStack.EMPTY;
  }
}
