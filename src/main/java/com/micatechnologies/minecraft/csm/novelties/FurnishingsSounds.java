package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.codeutils.CsmSoundRegistry;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;

/**
 * The sounds shipped by the CSM: Furniture &amp; Novelties module: the arcade cabinet attract
 * loops, the record players and radios, and the other furnishing sounds.
 *
 * <p>Each constant's name is the sound's {@code sounds.json} key and the path of its registry
 * name, so a sound event stays {@code csm:&lt;key&gt;} and nothing that refers to one by
 * string has to change.</p>
 *
 * @author ah@micatechnologies.com
 * @version 1.0
 * @since 2026.9
 */
public enum FurnishingsSounds implements ICsmSound {
  HANDDRYER("handdryer"),
  OLDRADIO("oldradio"),
  ASTEROIDS_CABINET("asteroids_cabinet"),
  BZ_CABINET("bz_cabinet"),
  CP_CABINET("cp_cabinet"),
  GALAGA_CABINET("galaga_cabinet"),
  MISCMD_CABINET("miscmd_cabinet"),
  PACMAN_CABINET("pacman_cabinet"),
  TEMPEST_CABINET("tempest_cabinet"),
  OLDRECORDPLAYER2("oldrecordplayer2"),
  LOCKER_DOOR_CLOSE("locker_door_close"),
  LOCKER_DOOR_OPEN("locker_door_open"),
  OLDRECORDPLAYER("oldrecordplayer"),
  SONY_DREAM_MACHINE_1980S("sony_dream_machine_1980s"),
  OLDRADIO2("oldradio2");

  /**
   * The name of the sound.
   *
   * @since 1.0
   */
  private final String soundName;

  /**
   * Constructor for a new sound.
   *
   * @param soundName the name of the sound
   *
   * @since 1.0
   */
  FurnishingsSounds(String soundName) {
    this.soundName = soundName;
  }

  @Override
  public String getSoundName() {
    return soundName;
  }

  /**
   * Hands every sound in this enum to Core's sound registrar. This is called from the module's
   * {@code preInit}, which Forge runs before it fires the sound registry event.
   *
   * @since 1.0
   */
  public static void registerSounds() {
    CsmSoundRegistry.register(values());
  }
}
