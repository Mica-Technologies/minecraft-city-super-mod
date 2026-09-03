package com.micatechnologies.minecraft.csm.technology;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side {@link MovingSound} that plays a looping ambient track for a single linked
 * speaker. The sound's logical position follows the local player so that Minecraft's sound
 * engine never discards it for being out of range; volume is recomputed per tick based on
 * the actual distance from the player to the speaker block, giving smooth attenuation as
 * the player moves around — the reliability win the fire alarm pattern was designed for.
 *
 * <p>Each speaker that's ambient-active gets its own instance of this class on each client,
 * keyed by the speaker's {@link BlockPos} via
 * {@link SpeakerAmbientPacketHandler}. Stopping the sound is handled by setting
 * {@code donePlaying = true} from {@link #stopPlaying()} and removing the registry entry.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
@SideOnly(Side.CLIENT)
public class SpeakerAmbientSound extends MovingSound {

  private static final float MAX_VOLUME = 0.85f;
  // Must stay > 0 — a zero-volume MovingSound gets discarded by the sound engine and we
  // never get another update() to ramp it back up.
  private static final float MIN_VOLUME = 0.02f;

  private final BlockPos speakerPos;
  private final float hearingRange;

  public SpeakerAmbientSound(ResourceLocation soundResource, BlockPos speakerPos,
      float hearingRange) {
    super(new SoundEvent(soundResource), SoundCategory.RECORDS);
    this.speakerPos = speakerPos.toImmutable();
    this.hearingRange = hearingRange;
    this.repeat = true;
    this.repeatDelay = 0;
    this.attenuationType = AttenuationType.NONE;

    EntityPlayer player = Minecraft.getMinecraft().player;
    if (player != null) {
      this.xPosF = (float) player.posX;
      this.yPosF = (float) player.posY;
      this.zPosF = (float) player.posZ;
      this.volume = computeVolume(player.posX, player.posY, player.posZ);
    } else {
      this.xPosF = speakerPos.getX() + 0.5f;
      this.yPosF = speakerPos.getY() + 0.5f;
      this.zPosF = speakerPos.getZ() + 0.5f;
      this.volume = MIN_VOLUME;
    }
  }

  /** Quadratic falloff from the speaker block, clamped to {@link #MIN_VOLUME}. */
  private float computeVolume(double px, double py, double pz) {
    double dx = px - (speakerPos.getX() + 0.5);
    double dy = py - (speakerPos.getY() + 0.5);
    double dz = pz - (speakerPos.getZ() + 0.5);
    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (dist >= hearingRange) {
      return MIN_VOLUME;
    }
    float linear = (float) (1.0 - dist / hearingRange);
    float ratio = linear * linear;
    return MIN_VOLUME + (MAX_VOLUME - MIN_VOLUME) * ratio;
  }

  @Override
  public void update() {
    EntityPlayer player = Minecraft.getMinecraft().player;
    if (player == null) {
      donePlaying = true;
      return;
    }
    xPosF = (float) player.posX;
    yPosF = (float) player.posY;
    zPosF = (float) player.posZ;
    volume = computeVolume(player.posX, player.posY, player.posZ);
    pitch = 1.0f;
  }

  /** Called by the packet handler when the speaker stops broadcasting. */
  public void stopPlaying() {
    this.donePlaying = true;
  }

  public BlockPos getSpeakerPos() {
    return speakerPos;
  }
}
