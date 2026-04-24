package com.micatechnologies.minecraft.csm.codeutils.packets;

import com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityVariableSpeedLimit;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TileEntityVariableSpeedLimitUpdateHandler implements
    IMessageHandler<TileEntityVariableSpeedLimitUpdatePacket, IMessage> {

  @Override
  public IMessage onMessage(TileEntityVariableSpeedLimitUpdatePacket message, MessageContext ctx) {
    ctx.getServerHandler().player.server.addScheduledTask(() -> {
      World serverWorld = ctx.getServerHandler().player.world;
      TileEntity tileEntity = serverWorld.getTileEntity(message.getPos());
      if (tileEntity instanceof TileEntityVariableSpeedLimit) {
        ((TileEntityVariableSpeedLimit) tileEntity).setData(
            message.getSpeedValue(), message.getFlasherMode(),
            message.getTrailerColor(), message.getSignAngle());
      }
    });
    return null;
  }
}
