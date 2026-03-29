package com.micatechnologies.minecraft.csm.trafficsignals;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side handler for APS sound packets. Manages concurrent {@link APSMovingSound} instances
 * keyed by channel name, allowing multiple APS buttons and tweeters to play simultaneously with
 * independent distance-based volume.
 */
public class APSSoundPacketHandler implements IMessageHandler<APSSoundPacket, IMessage> {

  private static final Map<String, APSMovingSound> activeSounds = new HashMap<>();

  @Override
  public IMessage onMessage(APSSoundPacket message, MessageContext ctx) {
    Minecraft.getMinecraft().addScheduledTask(() -> handlePacket(message));
    return null;
  }

  @SideOnly(Side.CLIENT)
  private static void handlePacket(APSSoundPacket message) {
    String channel = message.getChannel();

    if (!message.isStart()) {
      if (channel == null || channel.isEmpty()) {
        stopAllSounds();
      } else {
        stopChannel(channel);
      }
      return;
    }

    if (message.getSourcePositions().isEmpty()) {
      return;
    }

    stopChannel(channel);

    APSMovingSound sound = new APSMovingSound(
        new ResourceLocation(message.getSoundResource()),
        message.getSourcePositions(),
        message.getHearingRange(),
        message.isRepeatSound());
    activeSounds.put(channel, sound);
    Minecraft.getMinecraft().getSoundHandler().playSound(sound);
  }

  @SideOnly(Side.CLIENT)
  private static void stopChannel(String channel) {
    APSMovingSound sound = activeSounds.remove(channel);
    if (sound != null) {
      sound.stopPlaying();
      Minecraft.getMinecraft().getSoundHandler().stopSound(sound);
    }
  }

  /**
   * Stops all currently playing APS sounds across all channels.
   */
  @SideOnly(Side.CLIENT)
  public static void stopAllSounds() {
    for (APSMovingSound sound : activeSounds.values()) {
      sound.stopPlaying();
      Minecraft.getMinecraft().getSoundHandler().stopSound(sound);
    }
    activeSounds.clear();
  }
}
