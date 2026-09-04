package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.CsmSoundRegistry;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;

/**
 * The sounds shipped by the CSM: Roads &amp; Traffic module: the accessible pedestrian signal
 * voices and tones, and the cuckoo/chirp crosswalk tones.
 *
 * <p>Each constant's name is the sound's {@code sounds.json} key and the path of its registry
 * name, so a sound event stays {@code csm:&lt;key&gt;} and nothing that refers to one by
 * string has to change.</p>
 *
 * @author ah@micatechnologies.com
 * @version 1.0
 * @since 2026.9
 */
public enum RoadsSounds implements ICsmSound {
  CROSSWALK_COOKOO_1("crosswalk_cookoo_1"),
  CROSSWALK_COOKOO_2("crosswalk_cookoo_2"),
  CAMPBELL_PERC_EW("campbell_perc_ew"),
  CAMPBELL_PERC_NS("campbell_perc_ns"),
  CAMPBELL_PHIL_WAIT("campbell_phil_wait"),
  CAMPBELL_PHIL_WAIT_LOOK_BOTH_WAYS("campbell_phil_wait_look_both_ways"),
  CAMPBELL_PHIL_WAIT_TO_CROSS("campbell_phil_wait_to_cross"),
  CAMPBELL_PHIL_WALK_EXCLUSIVE("campbell_phil_walk_exclusive"),
  CAMPBELL_PHIL_WALK_ON("campbell_phil_walk_on"),
  CAMPBELL_PHIL_WALK_ON_TO_CROSS("campbell_phil_walk_on_to_cross"),
  CAMPBELL_PHIL_CROSSING_LIGHTS_ACTIVATED("campbell_phil_crossing_lights_activated"),
  CAMPBELL_PHIL_WARNING_LIGHTS_ACTIVATED("campbell_phil_warning_lights_activated"),
  CAMPBELL_TONE1("campbell_tone1"),
  CAMPBELL_WAIT("campbell_wait"),
  CAMPBELL_WAIT_LOOK_BOTH_WAYS("campbell_wait_look_both_ways"),
  CAMPBELL_WALK_EXCLUSIVE("campbell_walk_exclusive"),
  CAMPBELL_WALK_SIGN_ON("campbell_walk_sign_on"),
  CAMPBELL_WARNING_LIGHTS_ARE_FLASHING("campbell_warning_lights_are_flashing"),
  CAMPBELL_YELLOW_LIGHTS_ARE_FLASHING("campbell_yellow_lights_are_flashing"),
  POLARA_LANG2_WAIT("polara_lang2_wait"),
  POLARA_LANG2_WALK("polara_lang2_walk"),
  POLARA_LANG2_WALK_ALL_CROSSINGS("polara_lang2_walk_all_crossings"),
  POLARA_RAPID_TICK1("polara_rapid_tick1"),
  POLARA_TONE1("polara_tone1"),
  POLARA_WAIT("polara_wait"),
  POLARA_WALK("polara_walk"),
  POLARA_WALK_ALL_CROSSINGS("polara_walk_all_crossings");

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
  RoadsSounds(String soundName) {
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
