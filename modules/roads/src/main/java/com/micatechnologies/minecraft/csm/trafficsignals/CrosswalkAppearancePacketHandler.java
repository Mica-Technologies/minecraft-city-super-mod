package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkVisorType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link CrosswalkAppearancePacket}. Validates the tile entity and applies
 * the pasted appearance bundle via {@link TileEntityCrosswalkSignalNew#applyCopiedAppearance}.
 *
 * <p>Whether the target is a double (12-inch) signal is resolved from the block here rather than
 * trusted from the packet, so a client cannot force the fixed-symbol display format onto a single
 * (16-inch) signal.</p>
 */
public class CrosswalkAppearancePacketHandler implements
    IMessageHandler<CrosswalkAppearancePacket, IMessage> {

  @Override
  public IMessage onMessage(CrosswalkAppearancePacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityCrosswalkSignalNew)) {
        return;
      }

      boolean isDouble = world.getBlockState(message.getPos()).getBlock()
          instanceof BlockControllableCrosswalkSignalDouble;

      ((TileEntityCrosswalkSignalNew) te).applyCopiedAppearance(
          TrafficSignalBodyColor.fromNBT(message.getBodyColor()),
          TrafficSignalBodyColor.fromNBT(message.getVisorColor()),
          CrosswalkVisorType.fromNBT(message.getVisorType()),
          CrosswalkBulbType.fromNBT(message.getBulbType()),
          isDouble);
    });
    return null;
  }
}
