package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link SignalHeadAppearancePacket}. Validates the tile entity and applies
 * the pasted appearance bundle via {@link TileEntityTrafficSignalHead#applyCopiedAppearance}.
 */
public class SignalHeadAppearancePacketHandler implements
    IMessageHandler<SignalHeadAppearancePacket, IMessage> {

  @Override
  public IMessage onMessage(SignalHeadAppearancePacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityTrafficSignalHead)) {
        return;
      }

      ((TileEntityTrafficSignalHead) te).applyCopiedAppearance(
          TrafficSignalBodyColor.fromNBT(message.getBodyColor()),
          TrafficSignalBodyColor.fromNBT(message.getDoorColor()),
          TrafficSignalBodyColor.fromNBT(message.getVisorColor()),
          TrafficSignalVisorType.fromNBT(message.getVisorType()),
          TrafficSignalBulbStyle.fromNBT(message.getBulbStyle()),
          message.isAgingEnabled(),
          TrafficSignalBodyColor.fromNBT(message.getMountColor()),
          message.isHorizontalFlip());
    });
    return null;
  }
}
