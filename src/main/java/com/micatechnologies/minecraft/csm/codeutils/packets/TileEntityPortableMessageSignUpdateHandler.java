package com.micatechnologies.minecraft.csm.codeutils.packets;

import com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityPortableMessageSign;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TileEntityPortableMessageSignUpdateHandler implements
    IMessageHandler<TileEntityPortableMessageSignUpdatePacket, IMessage> {

  @Override
  public IMessage onMessage(TileEntityPortableMessageSignUpdatePacket message, MessageContext ctx) {
    ctx.getServerHandler().player.server.addScheduledTask(() -> {
      World serverWorld = ctx.getServerHandler().player.world;
      TileEntity tileEntity = serverWorld.getTileEntity(message.getPos());
      if (tileEntity instanceof TileEntityPortableMessageSign) {
        ((TileEntityPortableMessageSign) tileEntity).setData(
            message.getPages(), message.getFlasherMode(), message.getCycleSpeed(),
            message.getTrailerColor(), message.getSignAngle());
      }
    });
    return null;
  }
}
