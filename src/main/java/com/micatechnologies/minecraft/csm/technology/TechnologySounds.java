package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.CsmSoundRegistry;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;

/**
 * The sounds shipped by the CSM: Technology module: the speaker's ambient music and the payment
 * terminal's confirmation tone.
 *
 * <p>Each constant's name is the sound's {@code sounds.json} key and the path of its registry
 * name, so a sound event stays {@code csm:&lt;key&gt;} and nothing that refers to one by
 * string has to change.</p>
 *
 * @author ah@micatechnologies.com
 * @version 1.0
 * @since 2026.9
 */
public enum TechnologySounds implements ICsmSound {
  MII_CHANNEL_REMIX("mii_channel_remix"),
  VERIFONE_MX915("verifone_mx915");

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
  TechnologySounds(String soundName) {
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
