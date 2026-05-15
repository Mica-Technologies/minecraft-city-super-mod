package com.micatechnologies.minecraft.csm.technology;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side handler for {@link SpeakerAmbientPacket}. Maintains one
 * {@link SpeakerAmbientSound} per active speaker, keyed by the speaker's {@link BlockPos}.
 * Start packets create or replace the active sound for a given speaker; stop packets fade
 * it out via {@link SpeakerAmbientSound#stopPlaying()} and remove the registry entry.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class SpeakerAmbientPacketHandler
    implements IMessageHandler<SpeakerAmbientPacket, IMessage> {

  @SideOnly(Side.CLIENT)
  private static final Map<BlockPos, SpeakerAmbientSound> ACTIVE_SOUNDS = new HashMap<>();

  @Override
  public IMessage onMessage(SpeakerAmbientPacket message, MessageContext ctx) {
    Minecraft.getMinecraft().addScheduledTask(() -> handle(message));
    return null;
  }

  @SideOnly(Side.CLIENT)
  private static void handle(SpeakerAmbientPacket message) {
    BlockPos pos = message.getSpeakerPos();
    if (!message.isStart()) {
      stopAt(pos);
      return;
    }

    String res = message.getSoundResource();
    if (res == null || res.isEmpty()) {
      stopAt(pos);
      return;
    }

    // Replace any existing sound at this speaker so a sound change (cycle while active)
    // takes effect immediately rather than after the current loop ends.
    stopAt(pos);

    SpeakerAmbientSound sound = new SpeakerAmbientSound(
        new ResourceLocation(res), pos, message.getHearingRange());
    ACTIVE_SOUNDS.put(pos.toImmutable(), sound);
    Minecraft.getMinecraft().getSoundHandler().playSound(sound);
  }

  @SideOnly(Side.CLIENT)
  private static void stopAt(BlockPos pos) {
    SpeakerAmbientSound existing = ACTIVE_SOUNDS.remove(pos);
    if (existing != null) {
      existing.stopPlaying();
      Minecraft.getMinecraft().getSoundHandler().stopSound(existing);
    }
  }
}
