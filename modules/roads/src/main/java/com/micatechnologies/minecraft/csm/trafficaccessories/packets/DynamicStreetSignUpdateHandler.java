package com.micatechnologies.minecraft.csm.trafficaccessories.packets;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityDynamicStreetSign;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Applies a street blade edit server-side, after checking the sender can actually reach the
 * block and that the document is within a sane size.
 */
public class DynamicStreetSignUpdateHandler implements
    IMessageHandler<DynamicStreetSignUpdatePacket, IMessage> {

  // The blade's document is flat and short -- a few hundred bytes in practice. The cap is
  // generous against that but still bounds the NBT payload against an abusive client.
  private static final int MAX_JSON_LENGTH = 4096;

  @Override
  public IMessage onMessage(DynamicStreetSignUpdatePacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      String json = message.getSignDataJson();
      if (json != null && json.length() > MAX_JSON_LENGTH) {
        return;
      }
      World serverWorld = player.world;
      TileEntity tileEntity = serverWorld.getTileEntity(message.getPos());
      if (tileEntity instanceof TileEntityDynamicStreetSign) {
        ((TileEntityDynamicStreetSign) tileEntity).setSignDataJson(json);
      }
    });
    return null;
  }
}
