package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Who may manage a pawn shop. Pawn shops are server-run, so every one is managed the same way:
 * through vanilla's {@code canUseCommand(level, node)}, so ops (level 2) pass and a permissions
 * mod can grant or refuse the node on its own.
 *
 * <ul>
 *   <li>{@value #NODE_ADMIN}: place and break a pawn shop, switch a counter's resale on or off,
 *   and take its stock out.</li>
 * </ul>
 *
 * <p>Anyone may pawn. Placing needs the node because a pawn shop pays from nothing: one a player
 * could put down anywhere would be their own money tap, wherever they liked.</p>
 *
 * @since 2026.10
 */
public final class PawnPermissions {

  /** The node to manage pawn shops. */
  public static final String NODE_ADMIN = "csm.pawn.admin";

  private static final int LEVEL = 2;

  private PawnPermissions() {
  }

  public static boolean isAdmin(EntityPlayer player) {
    return player.canUseCommand(LEVEL, NODE_ADMIN);
  }
}
