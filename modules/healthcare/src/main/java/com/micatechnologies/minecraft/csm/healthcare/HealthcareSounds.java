package com.micatechnologies.minecraft.csm.healthcare;

import com.micatechnologies.minecraft.csm.codeutils.CsmSoundRegistry;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;

/**
 * The sounds shipped by the CSM: Healthcare module, all synthesised by
 * {@code dev-env-utils/scripts/gen_healthcare_sounds.py}: the code blue annunciator's chime, the
 * medical gas master alarm's buzzer and the infant security panel's alarm.
 *
 * <p>Each constant's name is the sound's {@code sounds.json} key and the path of its registry
 * name, so a sound event is {@code csm:&lt;key&gt;}.</p>
 *
 * @since 2026.10
 */
public enum HealthcareSounds implements ICsmSound {
  CODE_BLUE_CHIME("code_blue_chime"),
  MEDICAL_GAS_ALARM("medical_gas_alarm"),
  INFANT_SECURITY_ALARM("infant_security_alarm");

  private final String soundName;

  HealthcareSounds(String soundName) {
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
