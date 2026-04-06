package com.micatechnologies.minecraft.csm.lifesafety;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * A client-side MovingSound that plays a voice evac or storm alarm sound attached to the player.
 * The sound follows the player's position every tick, and the volume is dynamically adjusted based
 * on the player's distance to the nearest linked speaker/strobe position. When the player moves
 * beyond hearing range of all speakers, the volume drops to zero and the sound stops.
 */
@SideOnly(Side.CLIENT)
public class FireAlarmVoiceEvacSound extends MovingSound {

  private static final float MAX_VOLUME = 1.0f;
  private static final float MIN_VOLUME = 0.05f;

  private List<BlockPos> speakerPositions;
  private final float hearingRange;

  /**
   * Creates a new voice evac sound that follows the local player.
   *
   * @param soundResource   the resource location of the sound to play (e.g. "csm:svenew")
   * @param speakerPositions the positions of all linked voice evac speakers
   * @param hearingRange     the maximum distance from a speaker at which the sound is audible
   */
  public FireAlarmVoiceEvacSound(ResourceLocation soundResource, List<BlockPos> speakerPositions,
      float hearingRange) {
    super(SoundEvent.REGISTRY.getObject(soundResource), SoundCategory.AMBIENT);
    this.speakerPositions = new ArrayList<>(speakerPositions);
    this.hearingRange = hearingRange;
    this.repeat = true;
    this.repeatDelay = 0;
    this.attenuationType = AttenuationType.NONE;

    // Initialize position to player and calculate initial volume.
    // IMPORTANT: volume must not be 0 at play time - MC's sound engine will skip/discard
    // sounds with zero volume and never call update().
    EntityPlayer player = Minecraft.getMinecraft().player;
    if (player != null) {
      this.xPosF = (float) player.posX;
      this.yPosF = (float) player.posY;
      this.zPosF = (float) player.posZ;
      this.volume = calculateVolume(player);
    } else {
      this.volume = MIN_VOLUME;
    }
  }

  /**
   * Calculates the volume based on the player's distance to the nearest speaker.
   * Uses squared distances for comparison, only computing sqrt for the final nearest speaker.
   */
  private float calculateVolume(EntityPlayer player) {
    double minDistSq = Double.MAX_VALUE;
    for (BlockPos sp : speakerPositions) {
      double distSq = player.getDistanceSq(sp);
      if (distSq < minDistSq) {
        minDistSq = distSq;
      }
    }

    double hearingRangeSq = (double) hearingRange * hearingRange;
    if (minDistSq > hearingRangeSq) {
      return MIN_VOLUME;
    }
    // Only compute sqrt for the final closest distance (needed for linear falloff calculation)
    double minDist = Math.sqrt(minDistSq);
    // Quadratic falloff for realistic distance attenuation (inverse-square-like)
    float linear = (float) (1.0 - minDist / hearingRange);
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

    // Update position to follow the player
    xPosF = (float) player.posX;
    yPosF = (float) player.posY;
    zPosF = (float) player.posZ;

    // Update volume based on distance to nearest speaker.
    // Use MIN_VOLUME instead of 0 when out of range to prevent MC from discarding the sound.
    volume = calculateVolume(player);
  }

  /**
   * Stops this sound from playing.
   */
  public void stopPlaying() {
    donePlaying = true;
    speakerPositions = Collections.emptyList();
  }
}
