package com.micatechnologies.minecraft.csm.lifesafety;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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

  // Glitch effect constants: ~6% chance per tick of starting a dropout (~1 glitch/s on average)
  private static final float GLITCH_CHANCE_PER_TICK = 0.06f;
  // Pitch-artifact glitches (brief pitch shift) occur 1 in 3 glitch events
  private static final float PITCH_ARTIFACT_CHANCE = 0.33f;

  private List<BlockPos> speakerPositions;
  private final float hearingRange;
  private final boolean glitchy;
  private final Random glitchRandom = new Random();
  private int glitchTicksRemaining = 0;
  private boolean glitchIsPitchArtifact = false;
  // Volume multiplier for the current glitch: full dropout (~0) or partial dip (0.2-0.45)
  private float glitchVolumeMultiplier = 0.0f;

  /**
   * Creates a new voice evac sound that follows the local player.
   *
   * @param soundResource    the resource location of the sound to play (e.g. "csm:svenew")
   * @param speakerPositions the positions of all linked voice evac speakers
   * @param hearingRange     the maximum distance from a speaker at which the sound is audible
   * @param glitchy          if true, the sound will play with occasional stutters and dropouts
   */
  public FireAlarmVoiceEvacSound(ResourceLocation soundResource, List<BlockPos> speakerPositions,
      float hearingRange, boolean glitchy) {
    super(SoundEvent.REGISTRY.getObject(soundResource), SoundCategory.AMBIENT);
    this.speakerPositions = new ArrayList<>(speakerPositions);
    this.hearingRange = hearingRange;
    this.glitchy = glitchy;
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
    return calculateVolume(player.posX, player.posY, player.posZ, speakerPositions, hearingRange);
  }

  /**
   * Calculates the volume based on the listener's distance to the nearest speaker. Extracted as a
   * package-private static method to enable unit testing without requiring a running Minecraft
   * client.
   *
   * @param playerX          the listener's X position
   * @param playerY          the listener's Y position
   * @param playerZ          the listener's Z position
   * @param speakerPositions the positions of all linked speakers
   * @param hearingRange     the maximum distance from a speaker at which the sound is audible
   *
   * @return the calculated volume between {@link #MIN_VOLUME} and {@link #MAX_VOLUME}
   */
  static float calculateVolume(double playerX, double playerY, double playerZ,
      List<BlockPos> speakerPositions, float hearingRange) {
    double minDistSq = Double.MAX_VALUE;
    for (BlockPos sp : speakerPositions) {
      double dx = playerX - (sp.getX() + 0.5);
      double dy = playerY - (sp.getY() + 0.5);
      double dz = playerZ - (sp.getZ() + 0.5);
      double distSq = dx * dx + dy * dy + dz * dz;
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

    float baseVolume = calculateVolume(player);

    if (glitchy) {
      if (glitchTicksRemaining > 0) {
        // Mid-glitch: apply volume multiplier and optionally shift pitch
        glitchTicksRemaining--;
        // Clamp so MC's sound engine never discards the instance (volume must stay > 0)
        volume = Math.max(MIN_VOLUME, baseVolume * glitchVolumeMultiplier);
        pitch = glitchIsPitchArtifact ? (0.80f + glitchRandom.nextFloat() * 0.25f) : 1.0f;
      } else {
        // Normal playback; occasionally trigger a glitch event
        volume = baseVolume;
        pitch = 1.0f;
        if (glitchRandom.nextFloat() < GLITCH_CHANCE_PER_TICK) {
          // Dropout lasts 2-7 ticks (0.1-0.35 s)
          glitchTicksRemaining = 2 + glitchRandom.nextInt(6);
          glitchIsPitchArtifact = glitchRandom.nextFloat() < PITCH_ARTIFACT_CHANCE;
          // 50% full dropout, 50% partial dip (20-45% of base volume) — sounds broken but audible
          glitchVolumeMultiplier = glitchRandom.nextFloat() < 0.5f
              ? 0.0f
              : (0.20f + glitchRandom.nextFloat() * 0.25f);
        }
      }
    } else {
      // Update volume based on distance to nearest speaker.
      // Use MIN_VOLUME instead of 0 when out of range to prevent MC from discarding the sound.
      volume = baseVolume;
      pitch = 1.0f;
    }
  }

  /**
   * Stops this sound from playing.
   */
  public void stopPlaying() {
    donePlaying = true;
    speakerPositions = Collections.emptyList();
  }
}
