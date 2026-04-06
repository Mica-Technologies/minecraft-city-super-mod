package com.micatechnologies.minecraft.csm.lifesafety;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side handler for fire alarm sound packets. Manages multiple concurrent
 * {@link FireAlarmVoiceEvacSound} instances keyed by channel name, allowing voice evac, storm,
 * and multiple horn sound types to play simultaneously with independent distance-based volume.
 * Also maintains the {@link ActiveStrobeRegistry} for visual strobe rendering.
 */
public class FireAlarmSoundPacketHandler implements
    IMessageHandler<FireAlarmSoundPacket, IMessage> {

  private static final Map<String, FireAlarmVoiceEvacSound> activeSounds = new HashMap<>();
  private static final Map<String, Set<BlockPos>> channelPositions = new HashMap<>();

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

    // Ignore malformed start packets with null/empty channel
    if (channel == null || channel.isEmpty()) {
      return;
    }

    // Start: stop existing sound on this channel, then create a new one
    if (message.getSpeakerPositions().isEmpty()) {
      return;
    }

    stopChannel(channel);

    // Track positions for this channel and register with strobe registry
    Set<BlockPos> positions = new HashSet<>(message.getSpeakerPositions());
    channelPositions.put(channel, positions);
    ActiveStrobeRegistry.addPositions(positions);

    // Strobe-only channels have an empty sound resource — register positions but skip sound
    String soundResource = message.getSoundResource();
    if (soundResource == null || soundResource.isEmpty()) {
      return;
    }

    FireAlarmVoiceEvacSound sound = new FireAlarmVoiceEvacSound(
        new ResourceLocation(soundResource),
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
    Set<BlockPos> positions = channelPositions.remove(channel);
    if (positions != null) {
      ActiveStrobeRegistry.removePositions(positions);
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
    channelPositions.clear();
    ActiveStrobeRegistry.clearAll();
  }
}
