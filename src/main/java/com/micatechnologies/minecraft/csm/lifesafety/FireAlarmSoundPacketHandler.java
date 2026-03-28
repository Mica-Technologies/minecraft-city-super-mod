package com.micatechnologies.minecraft.csm.lifesafety;

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
 * Client-side handler for fire alarm sound packets. Manages multiple concurrent
 * {@link FireAlarmVoiceEvacSound} instances keyed by channel name, allowing voice evac, storm,
 * and multiple horn sound types to play simultaneously with independent distance-based volume.
 */
public class FireAlarmSoundPacketHandler implements
    IMessageHandler<FireAlarmSoundPacket, IMessage> {

  private static final Map<String, FireAlarmVoiceEvacSound> activeSounds = new HashMap<>();

  @Override
  public IMessage onMessage(FireAlarmSoundPacket message, MessageContext ctx) {
    Minecraft.getMinecraft().addScheduledTask(() -> handlePacket(message));
    return null;
  }

  @SideOnly(Side.CLIENT)
  private static void handlePacket(FireAlarmSoundPacket message) {
    String channel = message.getChannel();

    if (!message.isStart()) {
      if (channel == null || channel.isEmpty()) {
        // Empty channel = stop all sounds
        stopAllSounds();
      } else {
        // Stop specific channel
        stopChannel(channel);
      }
      return;
    }

    // Start: stop existing sound on this channel, then create a new one
    if (message.getSpeakerPositions().isEmpty()) {
      return;
    }

    stopChannel(channel);

    FireAlarmVoiceEvacSound sound = new FireAlarmVoiceEvacSound(
        new ResourceLocation(message.getSoundResource()),
        message.getSpeakerPositions(),
        message.getHearingRange());
    activeSounds.put(channel, sound);
    Minecraft.getMinecraft().getSoundHandler().playSound(sound);
  }

  @SideOnly(Side.CLIENT)
  private static void stopChannel(String channel) {
    FireAlarmVoiceEvacSound sound = activeSounds.remove(channel);
    if (sound != null) {
      sound.stopPlaying();
      Minecraft.getMinecraft().getSoundHandler().stopSound(sound);
    }
  }

  /**
   * Stops all currently playing fire alarm sounds across all channels.
   */
  @SideOnly(Side.CLIENT)
  public static void stopAllSounds() {
    for (FireAlarmVoiceEvacSound sound : activeSounds.values()) {
      sound.stopPlaying();
      Minecraft.getMinecraft().getSoundHandler().stopSound(sound);
    }
    activeSounds.clear();
  }
}
