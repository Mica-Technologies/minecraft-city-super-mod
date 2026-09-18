package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Applies a {@link CraneHeadConfigPacket} on the server.
 *
 * <p>The conventions every CSM packet follows (see "Network safety" in
 * {@code assets/docs/PERFORMANCE_AND_SECURITY.md}): the work is scheduled onto the server thread;
 * its first check is that the player can reach the block they clicked; the head is found from
 * that block the same way the GUI found it; a non-finite float is refused outright rather than
 * clamped, since a NaN clamps to NaN; and the ordinals and every range are clamped again by the
 * tile entity, whatever the client sent. A configuration GUI checks reach only, as the in-world
 * gate for opening it is reach.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class CraneHeadConfigPacketHandler implements
    IMessageHandler<CraneHeadConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(CraneHeadConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      if (!Float.isFinite(message.getSlew()) || !Float.isFinite(message.getTrolley())
          || !Float.isFinite(message.getLuff())) {
        return;
      }
      TileEntityCraneHead head = CraneLocator.findHead(player.world, message.getPos());
      if (head == null) {
        return;
      }
      head.setConfiguration(CraneModel.fromOrdinal(message.getModel()),
          CraneLivery.fromOrdinal(message.getLivery()), message.getJibLength(),
          message.getSlew(), message.getTrolley(), message.getHookDrop(), message.getLuff());
    });
    return null;
  }
}
