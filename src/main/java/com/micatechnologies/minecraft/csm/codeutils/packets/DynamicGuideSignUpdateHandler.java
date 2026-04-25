package com.micatechnologies.minecraft.csm.codeutils.packets;

import com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityDynamicGuideSign;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class DynamicGuideSignUpdateHandler implements
    IMessageHandler<DynamicGuideSignUpdatePacket, IMessage> {

  private static final int MAX_JSON_LENGTH = 8192;

  @Override
  public IMessage onMessage(DynamicGuideSignUpdatePacket message, MessageContext ctx) {
    ctx.getServerHandler().player.server.addScheduledTask(() -> {
      String json = message.getSignDataJson();
      if (json != null && json.length() > MAX_JSON_LENGTH) {
        return;
      }
      World serverWorld = ctx.getServerHandler().player.world;
      TileEntity tileEntity = serverWorld.getTileEntity(message.getPos());
      if (tileEntity instanceof TileEntityDynamicGuideSign) {
        ((TileEntityDynamicGuideSign) tileEntity).setSignDataJson(json);
      }
    });
    return null;
  }
}
