package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.codeutils.CsmTts;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client side of {@link BellAnnouncePacket}: speaks the text through {@link CsmTts} (the Text to
 * Speech module's voice when it is installed, the system narrator when not), at most once a
 * second however many arrive.
 *
 * @since 2026.10
 */
public class BellAnnounceHandler implements IMessageHandler<BellAnnouncePacket, IMessage> {

  private static final long MIN_INTERVAL_MS = 1000L;

  /** When the last announcement was spoken; client only, and only read on the client thread. */
  private static long lastSpokenAt;

  @Override
  public IMessage onMessage(BellAnnouncePacket message, MessageContext ctx) {
    Minecraft.getMinecraft().addScheduledTask(() -> speak(message.getText()));
    return null;
  }

  @SideOnly(Side.CLIENT)
  private static void speak(String text) {
    if (text == null || text.isEmpty()) {
      return;
    }
    long now = System.currentTimeMillis();
    if (now - lastSpokenAt < MIN_INTERVAL_MS) {
      return;
    }
    lastSpokenAt = now;
    CsmTts.say(text, CsmTts.getDefaultVoice());
  }
}
