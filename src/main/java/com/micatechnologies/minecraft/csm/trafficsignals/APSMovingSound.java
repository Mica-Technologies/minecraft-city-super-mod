package com.micatechnologies.minecraft.csm.trafficsignals;

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
 * A client-side MovingSound for APS (accessible pedestrian signal) buttons and crosswalk tweeters.
 * The sound follows the player's position every tick, and the volume is dynamically adjusted based
 * on the player's distance to the sound source position(s). Mirrors the fire alarm voice evac
 * MovingSound architecture but for shorter, intermittent crosswalk sounds.
 */
@SideOnly(Side.CLIENT)
public class APSMovingSound extends MovingSound {

  private static final float MAX_VOLUME = 1.0f;
  private static final float MIN_VOLUME = 0.05f;

  private final List<BlockPos> sourcePositions;
  private final float hearingRange;

  /**
   * Creates a new APS sound that follows the local player with distance-based volume.
   *
   * @param soundResource   the resource location of the sound to play
   * @param sourcePositions the positions of the APS button(s) or tweeter(s) emitting sound
   * @param hearingRange    the maximum distance at which the sound is audible
   */
  public APSMovingSound(ResourceLocation soundResource, List<BlockPos> sourcePositions,
      float hearingRange, boolean repeat) {
    super(SoundEvent.REGISTRY.getObject(soundResource), SoundCategory.NEUTRAL);
    this.sourcePositions = sourcePositions;
    this.hearingRange = hearingRange;
    this.repeat = repeat;
    this.repeatDelay = 0;
    this.attenuationType = AttenuationType.NONE;

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
   * Convenience constructor for a single source position.
   */
  public APSMovingSound(ResourceLocation soundResource, BlockPos sourcePosition,
      float hearingRange, boolean repeat) {
    this(soundResource, Collections.singletonList(sourcePosition), hearingRange, repeat);
  }

  private float calculateVolume(EntityPlayer player) {
    double minDist = Double.MAX_VALUE;
    for (BlockPos sp : sourcePositions) {
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

    xPosF = (float) player.posX;
    yPosF = (float) player.posY;
    zPosF = (float) player.posZ;
    volume = calculateVolume(player);
  }

  public void stopPlaying() {
    donePlaying = true;
  }
}
