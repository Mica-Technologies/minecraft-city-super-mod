package com.micatechnologies.minecraft.csm.codeutils.packets;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.technology.TileEntityRedstoneTTS;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TileEntityRedstoneTTSUpdateHandler implements
    IMessageHandler<TileEntityRedstoneTTSUpdatePacket, IMessage> {

  @Override
  public IMessage onMessage(TileEntityRedstoneTTSUpdatePacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World serverWorld = player.world;
      TileEntity tileEntity = serverWorld.getTileEntity(message.getPos());
      if (tileEntity instanceof TileEntityRedstoneTTS) {
        TileEntityRedstoneTTS te = (TileEntityRedstoneTTS) tileEntity;
        te.setTtsString(message.getTtsString());
        te.setTtsVoice(message.getTtsVoice());
      }
    });
    return null;
  }
}
