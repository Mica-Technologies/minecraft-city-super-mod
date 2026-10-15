package com.micatechnologies.minecraft.csm.furniture.market.shop;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A Market &amp; Store block that is a shop ({@link TileEntityShop}): it says where its item is
 * drawn and which of its blocks holds the shop.
 *
 * @since 2026.10
 */
public interface IShopBlock {

  /** The shop's setup screen (owner, managers), with a server-side container. */
  int GUI_SETUP = 47;
  /** The shop's counter screen (customers), client only. */
  int GUI_COUNTER = 48;

  /** Where its item is drawn and its price label hangs. */
  ShopDisplay getShopDisplay();

  /** The block holding the shop for the block at {@code pos} (a tall piece's lower half). */
  default BlockPos getShopPos(IBlockAccess world, BlockPos pos) {
    return pos;
  }
}
