package com.micatechnologies.minecraft.csm.codeutils.packets;

import com.micatechnologies.minecraft.csm.codeutils.CsmTts;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TileEntityRedstoneTTSInvokeHandler implements
    IMessageHandler<TileEntityRedstoneTTSInvokePacket, IMessage> {

  @Override
  public IMessage onMessage(TileEntityRedstoneTTSInvokePacket message, MessageContext ctx) {
    Minecraft.getMinecraft().addScheduledTask(() -> {
      CsmTts.say(message.getTtsString(), message.getTtsVoice());
    });
    return null;
  }
}
