package com.micatechnologies.minecraft.csm.furniture.market.shop;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Who may do what to a shop. Each check is vanilla's {@code canUseCommand(level, node)}, so ops
 * (level 2) pass and a permissions mod can grant or refuse each node on its own.
 *
 * <ul>
 *   <li>{@value #NODE_ADMIN}: open and break anyone's shop.</li>
 *   <li>{@value #NODE_SERVER}: make a shop a server shop (no owner, payments destroyed,
 *   buy-backs paid from nothing), and set up or break one.</li>
 *   <li>{@value #NODE_INFINITE}: give a shop unlimited stock.</li>
 * </ul>
 *
 * @since 2026.10
 */
public final class ShopPermissions {

  /** The node to manage any player's shop. */
  public static final String NODE_ADMIN = "csm.shop.admin";
  /** The node to make and manage server shops. */
  public static final String NODE_SERVER = "csm.shop.server";
  /** The node to give a shop unlimited stock. */
  public static final String NODE_INFINITE = "csm.shop.infinite";

  private static final int LEVEL = 2;

  private ShopPermissions() {
  }

  public static boolean isAdmin(EntityPlayer player) {
    return player.canUseCommand(LEVEL, NODE_ADMIN);
  }

  public static boolean canServer(EntityPlayer player) {
    return player.canUseCommand(LEVEL, NODE_SERVER);
  }

  public static boolean canInfinite(EntityPlayer player) {
    return player.canUseCommand(LEVEL, NODE_INFINITE);
  }

  /**
   * Whether the player may set the shop up, collect from it and break it: a server shop's
   * managers, or a player shop's owner or an admin.
   */
  public static boolean mayManage(EntityPlayer player, TileEntityShop shop) {
    if (shop.isServerShop()) {
      return canServer(player);
    }
    return shop.isOwner(player) || isAdmin(player);
  }
}
