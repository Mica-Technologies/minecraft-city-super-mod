package com.micatechnologies.minecraft.csm.lifesafety.stations;

import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The client side of a warning siren: starts its {@link SirenSound} when it begins sounding within
 * range of the listener, and stops it when the signal ends or the siren goes away. Only called
 * from {@link TileEntityWarningSiren} when its world is remote, so a server never loads it.
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class SirenClient {

  private SirenClient() {
  }

  static void tick(TileEntityWarningSiren siren) {
    SirenSignal signal = siren.getSignal();
    SirenSound playing = siren.clientSound instanceof SirenSound
        ? (SirenSound) siren.clientSound : null;
    if (signal == SirenSignal.NONE) {
      if (playing != null) {
        playing.stop();
        siren.clientSound = null;
      }
      return;
    }
    Minecraft mc = Minecraft.getMinecraft();
    if (playing != null && (playing.isDonePlaying()
        || !mc.getSoundHandler().isSoundPlaying(playing))) {
      // A one-shot signal that has played through stays finished; a looping one that the
      // engine dropped (out of range, or the sound system restarted) is started again below.
      if (!signal.loops) {
        return;
      }
      siren.clientSound = null;
      playing = null;
    }
    if (playing == null && mc.player != null
        && mc.player.getDistanceSq(siren.getPos()) < SirenSound.RANGE * SirenSound.RANGE) {
      SoundEvent event = signal.sound.getSoundEvent();
      if (event != null) {
        SirenSound sound = new SirenSound(siren, event, signal.loops);
        siren.clientSound = sound;
        mc.getSoundHandler().playSound(sound);
      }
    }
  }

  static void stop(TileEntityWarningSiren siren) {
    if (siren.clientSound instanceof SirenSound) {
      ((SirenSound) siren.clientSound).stop();
      siren.clientSound = null;
    }
  }
}
