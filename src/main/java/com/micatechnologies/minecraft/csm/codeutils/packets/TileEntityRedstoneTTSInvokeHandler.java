package com.micatechnologies.minecraft.csm.codeutils.packets;

import com.micatechnologies.minecraft.csm.codeutils.CsmNarrator;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TileEntityRedstoneTTSInvokeHandler implements
    IMessageHandler<TileEntityRedstoneTTSInvokePacket, IMessage> {

  @Override
  public IMessage onMessage(TileEntityRedstoneTTSInvokePacket message, MessageContext ctx) {
    Minecraft.getMinecraft().addScheduledTask(() -> {
      CsmNarrator.say(message.getTtsString());
    });
    return null;
  }
}

