package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side handler for fire alarm voice evac sound packets. Manages a single
 * {@link FireAlarmVoiceEvacSound} instance that follows the player with distance-based volume.
 * When a start packet arrives, any existing sound is stopped and a new one begins. When a stop
 * packet arrives, the current sound is stopped.
 */
public class FireAlarmSoundPacketHandler implements
    IMessageHandler<FireAlarmSoundPacket, IMessage> {

  private static FireAlarmVoiceEvacSound currentSound = null;

  @Override
  public IMessage onMessage(FireAlarmSoundPacket message, MessageContext ctx) {
    Minecraft.getMinecraft().addScheduledTask(() -> handlePacket(message));
    return null;
  }

  @SideOnly(Side.CLIENT)
  private static void handlePacket(FireAlarmSoundPacket message) {
    // Stop any currently playing voice evac sound
    if (currentSound != null) {
      currentSound.stopPlaying();
      Minecraft.getMinecraft().getSoundHandler().stopSound(currentSound);
      currentSound = null;
    }

    if (message.isStart() && !message.getSpeakerPositions().isEmpty()) {
      currentSound = new FireAlarmVoiceEvacSound(
          new ResourceLocation(message.getSoundResource()),
          message.getSpeakerPositions(),
          message.getHearingRange());
      Minecraft.getMinecraft().getSoundHandler().playSound(currentSound);
    }
  }

  /**
   * Stops the currently playing voice evac sound, if any. Can be called from other client-side
   * code if needed.
   */
  @SideOnly(Side.CLIENT)
  public static void stopCurrentSound() {
    if (currentSound != null) {
      currentSound.stopPlaying();
      Minecraft.getMinecraft().getSoundHandler().stopSound(currentSound);
      currentSound = null;
    }
  }
}
