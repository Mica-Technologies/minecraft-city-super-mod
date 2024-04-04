package com.micatechnologies.minecraft.csm.codeutils.packets;

import com.micatechnologies.minecraft.csm.technology.TileEntityRedstoneTTS;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TileEntityRedstoneTTSUpdateHandler implements
    IMessageHandler<TileEntityRedstoneTTSUpdatePacket, IMessage> {

  @Override
  public IMessage onMessage(TileEntityRedstoneTTSUpdatePacket message, MessageContext ctx) {
    // Ensure execution on the main server thread
    ctx.getServerHandler().player.server.addScheduledTask(() -> {
      World serverWorld = ctx.getServerHandler().player.world;
      TileEntity tileEntity = serverWorld.getTileEntity(message.getPos());
      if (tileEntity instanceof TileEntityRedstoneTTS) {
        ((TileEntityRedstoneTTS) tileEntity).setTtsString(message.getTtsString());
      }
    });
    return null; // No response packet needed
  }
}

