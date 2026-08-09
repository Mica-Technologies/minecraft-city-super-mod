package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link ArcadeHighScorePacket}. Verifies the sender is actually standing at
 * the cabinet and that the game named is one the cabinet has installed, then records the score and
 * tells the player if it took the record.
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class ArcadeHighScoreHandler
    implements IMessageHandler<ArcadeHighScorePacket, IMessage> {

  /**
   * Handles an incoming high score submission.
   *
   * @param message the submitted score
   * @param ctx     the message context
   *
   * @return {@code null}; no reply is sent
   *
   * @since 1.0
   */
  @Override
  public IMessage onMessage(ArcadeHighScorePacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      String gameId = message.getGameId();
      if (!ArcadeCatalog.isKnownGame(gameId)) {
        return;
      }
      TileEntity tileEntity = player.world.getTileEntity(message.getPos());
      if (!(tileEntity instanceof TileEntityArcadeCabinet)) {
        return;
      }
      TileEntityArcadeCabinet cabinet = (TileEntityArcadeCabinet) tileEntity;
      if (cabinet.tryPostHighScore(gameId, message.getScore(), player.getName())) {
        player.sendStatusMessage(new TextComponentString(
            ArcadeCatalog.titleOf(gameId) + " - new cabinet record: " + message.getScore()), true);
      }
    });
    return null;
  }
}
