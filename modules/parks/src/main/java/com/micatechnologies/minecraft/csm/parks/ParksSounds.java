package com.micatechnologies.minecraft.csm.parks;

import com.micatechnologies.minecraft.csm.codeutils.CsmSoundRegistry;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;

/**
 * The Parks &amp; Greenery module's sounds: the chainsaw's. Synthesised by
 * {@code dev-env-utils/scripts/gen_parks_tool_sounds.py}.
 *
 * @since 2026.10
 */
public enum ParksSounds implements ICsmSound {

  /** One pull of the starter cord: the rope's rasp and the engine turning over without firing. */
  CHAINSAW_PULL("chainsaw_pull"),
  /** The engine catching on the last pull and settling to idle. */
  CHAINSAW_START("chainsaw_start"),
  /** One second of idle, played back to back while the saw runs. */
  CHAINSAW_IDLE("chainsaw_idle"),
  /** The engine at full throttle through wood. */
  CHAINSAW_CUT("chainsaw_cut");

  private final String soundName;

  ParksSounds(String soundName) {
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
