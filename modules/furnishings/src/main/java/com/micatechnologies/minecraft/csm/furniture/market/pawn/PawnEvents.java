package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Pays a player, as they log in, what pawn shops owe them for items pawned in their name while
 * they were away; and resends a player's inventory at the end of a tick when a refused placement
 * left the client's count wrong ({@link PawnBlocks#placed}).
 *
 * @since 2026.10
 */
public class PawnEvents {

  /** Players whose inventory to resend at the end of this server tick. Server thread only. */
  private static final Set<EntityPlayerMP> RESYNC = new LinkedHashSet<>();

  /** Resends {@code player}'s inventory at the end of this server tick. */
  static void resyncInventory(EntityPlayerMP player) {
    RESYNC.add(player);
  }

  @SubscribeEvent
  public void onLogin(PlayerLoggedInEvent event) {
    if (event.player instanceof EntityPlayerMP) {
      PawnEconomy.settle((EntityPlayerMP) event.player);
    }
  }

  @SubscribeEvent
  public void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || RESYNC.isEmpty()) {
      return;
    }
    for (EntityPlayerMP player : RESYNC) {
      if (!player.hasDisconnected()) {
        player.sendContainerToPlayer(player.inventoryContainer);
      }
    }
    RESYNC.clear();
  }
}
