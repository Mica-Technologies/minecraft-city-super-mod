package com.micatechnologies.minecraft.csm.transit;

import com.micatechnologies.minecraft.csm.codeutils.CsmSoundRegistry;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;

/**
 * The Transit module's sounds, all synthesised by {@code dev-env-utils/scripts/
 * gen_transit_sounds.py}: the help point's connect chime and the ticket validator's accept and
 * refuse tones. Each name is its {@code sounds.json} key and the path of its {@code csm:} event.
 *
 * @since 2026.9
 */
public enum TransitSounds implements ICsmSound {

  /** The help point and emergency point: a rising three-note chime as the call connects. */
  HELP_POINT_CHIME("help_point_chime"),
  /** The ticket validator taking a fare: one bright beep. */
  VALIDATOR_ACCEPT("validator_accept"),
  /** The ticket validator refusing: two low beeps. */
  VALIDATOR_DENY("validator_deny");

  private final String soundName;

  TransitSounds(String soundName) {
    this.soundName = soundName;
  }

  @Override
  public String getSoundName() {
    return soundName;
  }

  /**
   * Hands every sound in this enum to Core's sound registrar. Called from the module's
   * {@code preInit}, which Forge runs before it fires the sound registry event.
   */
  public static void registerSounds() {
    CsmSoundRegistry.register(values());
  }
}
