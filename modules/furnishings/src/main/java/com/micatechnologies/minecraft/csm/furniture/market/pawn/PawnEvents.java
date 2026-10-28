package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

/**
 * Pays a player, as they log in, what pawn shops owe them for items pawned in their name while
 * they were away.
 *
 * @since 2026.10
 */
public class PawnEvents {

  @SubscribeEvent
  public void onLogin(PlayerLoggedInEvent event) {
    if (event.player instanceof EntityPlayerMP) {
      PawnEconomy.settle((EntityPlayerMP) event.player);
    }
  }
}
