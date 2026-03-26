package com.micatechnologies.minecraft.csm.lifesafety;

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

  private final List<BlockPos> speakerPositions;
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
    this.speakerPositions = speakerPositions;
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
   */
  private float calculateVolume(EntityPlayer player) {
    double minDist = Double.MAX_VALUE;
    for (BlockPos sp : speakerPositions) {
      double dist = Math.sqrt(player.getDistanceSq(sp));
      if (dist < minDist) {
        minDist = dist;
      }
    }

    if (minDist > hearingRange) {
      return MIN_VOLUME;
    }
    float ratio = (float) (1.0 - minDist / hearingRange);
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
  }
}
