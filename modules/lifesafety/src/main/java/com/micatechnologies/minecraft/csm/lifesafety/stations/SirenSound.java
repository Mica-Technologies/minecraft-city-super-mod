package com.micatechnologies.minecraft.csm.lifesafety.stations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * An outdoor warning siren as the listener hears it: played at the listener, with no attenuation
 * of its own, at a volume worked out every tick from the distance to the siren and, for a rotating
 * siren, from where its horn is pointing. As the horn sweeps past, the siren swells and fades, the
 * way a real one does across a town.
 *
 * <p>The game's own attenuation reaches only sixteen blocks per unit of volume; a siren is heard
 * across a town, so the distance falloff is done here, out to {@link #RANGE}.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class SirenSound extends MovingSound {

  /** How far a siren is heard, in blocks. */
  public static final double RANGE = 320.0;
  /** The share of full volume heard from behind a rotating horn. */
  private static final float BEHIND = 0.3F;

  private final TileEntityWarningSiren siren;
  private final BlockPos pos;

  SirenSound(TileEntityWarningSiren siren, SoundEvent event, boolean loops) {
    super(event, SoundCategory.BLOCKS);
    this.siren = siren;
    this.pos = siren.getPos();
    this.repeat = loops;
    this.repeatDelay = 0;
    this.attenuationType = AttenuationType.NONE;
    // Never start at zero: the engine drops a sound that starts silent and never updates it.
    this.volume = Math.max(0.02F, loudness(0F));
    follow();
  }

  @Override
  public void update() {
    if (siren.isInvalid() || siren.getSignal() == SirenSignal.NONE) {
      donePlaying = true;
      return;
    }
    follow();
    volume = Math.max(0.001F, loudness(0F));
  }

  /** Stops the sound now. */
  void stop() {
    donePlaying = true;
  }

  private void follow() {
    EntityPlayer player = Minecraft.getMinecraft().player;
    if (player != null) {
      xPosF = (float) player.posX;
      yPosF = (float) player.posY;
      zPosF = (float) player.posZ;
    }
  }

  /** How loud the siren is at the listener now, 0 to 1. */
  private float loudness(float partialTicks) {
    EntityPlayer player = Minecraft.getMinecraft().player;
    if (player == null) {
      return 0F;
    }
    double dx = player.posX - (pos.getX() + 0.5);
    double dz = player.posZ - (pos.getZ() + 0.5);
    double dy = player.posY - (pos.getY() + 0.5);
    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (d >= RANGE) {
      return 0F;
    }
    // Loud and flat up close, then falling off roughly as the inverse of distance.
    float byDistance = (float) Math.min(1.0, 24.0 / Math.max(24.0, d)) * (float) (1 - d / RANGE);
    if (!siren.rotates()) {
      return byDistance;
    }
    // The horn's model points north (-z); the renderer turns it by the angle about +y, which
    // takes (0, -1) to (-sin a, -cos a).
    double a = Math.toRadians(siren.hornAngle(partialTicks));
    double hx = -Math.sin(a);
    double hz = -Math.cos(a);
    double flat = Math.sqrt(dx * dx + dz * dz);
    double facing = flat < 1e-3 ? 1 : (hx * dx + hz * dz) / flat;
    float lobe = (float) Math.pow(Math.max(0, facing), 2);
    return byDistance * (BEHIND + (1 - BEHIND) * lobe);
  }
}
