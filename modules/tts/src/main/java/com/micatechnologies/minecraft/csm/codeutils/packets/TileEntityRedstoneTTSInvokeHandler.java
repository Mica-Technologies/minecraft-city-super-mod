package com.micatechnologies.minecraft.csm.codeutils.packets;

import com.micatechnologies.minecraft.csm.codeutils.CsmTts;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TileEntityRedstoneTTSInvokeHandler implements
    IMessageHandler<TileEntityRedstoneTTSInvokePacket, IMessage> {

  /** Minimum interval between accepted TTS invocations; guards the OS speech synthesizer
   *  against a misbehaving/malicious server spamming utterances. Wall-clock is correct here —
   *  this is not render-path code. */
  private static final long MIN_INVOKE_INTERVAL_MS = 1000;

  private static long lastInvokeMs = 0;

  @Override
  public IMessage onMessage(TileEntityRedstoneTTSInvokePacket message, MessageContext ctx) {
    Minecraft.getMinecraft().addScheduledTask(() -> {
      long now = System.currentTimeMillis();
      if (now - lastInvokeMs < MIN_INVOKE_INTERVAL_MS) {
        return;
      }
      lastInvokeMs = now;
      CsmTts.say(message.getTtsString(), message.getTtsVoice());
    });
    return null;
  }
}
