package com.micatechnologies.minecraft.csm;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;

/**
 * Mod sound class. This class contains the list of sound resources and the method for initializing
 * sounds on mod startup.
 *
 * @author ah@micatechnologies.com
 * @version 1.0
 * @since 2020.6
 */
public class CsmSounds {

  /**
   * Sound registration method. This method is called from {@link Csm} to register sounds in the
   * mod.
   *
   * @since 1.0
   */
  public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
    for (SOUND sound : SOUND.values()) {
      sound.registerSound(event);
    }
  }

  /**
   * Master enum list of sounds in the mod. Each entry in this enum MUST correspond to a sound file
   * in src/main/resources/assets/csm/sounds, but with the .ogg omitted. For example, if the sound
   * file `example_sound.ogg` was present, the entry in this array MUST be `example_sound`.
   *
   * @version 2.0
   * @since 1.0
   */
  public enum SOUND {
    // Available sounds
    BELL("bell"),
    CODETECTOR("codetector"),
    ET70_CHIME("et70_chime"),
    HANDDRYER("handdryer"),
    MILLS_FIREALARM("mills_firealarm"),
    MT_CODE3("mt_code3"),
    NEST_TEST("nest_test"),
    PANASONICFAN("panasonicfan"),
    _2910CALCODE("2910calcode"),
    _4030CODE44("4030code44"),
    SMOKEALARM("smokealarm"),
    SPECTRALERT("spectralert"),
    WHEELOCKAS("wheelockas"),
    ADAPTABELL("adaptabell"),
    BELL2("bell2"),
    FIREBELL("firebell"),
    KAC("kac"),
    SVENEW("svenew"),
    SVEOLD("sveold"),
    SVEN("sven"),
    SVEO("sveo"),
    WHEELOCK7002T("wheelock7002t"),
    DINGBLOCK("dingblock"),
    EST_GENESIS("est_genesis"),
    EST_INTEGRITY("est_integrity"),
    LMS_VOICE_EVAC("lms_voice_evac"),
    STAHORN("stahorn"),
    _7002T_MEDSPEED("7002t_medspeed"),
    _7002T_SLOWSPEED("7002t_slowspeed"),
    EDWARDS_ADAPTAHORN_CODE44("edwards_adaptahorn_code44"),
    GENTEX_GOS_CODE3("gentex_gos_code3"),
    SAE_MARCHTIME("sae_marchtime"),
    SIMPLEX_4051_MARCHTIME("simplex_4051_marchtime"),
    BROKEN_7002T("broken_7002t"),
    MCLALSVE("mclalsve"),
    EDWARDS_IO_RESET("edwards_io_reset"),
    SIMPLEX_PANEL_RESET("simplex_panel_reset"),
    PULLSTATION_PULL("pullstation_pull"),
    NOTIFIER_VOICE_EVAC("notifier_voice_evac"),
    SIMPLEX_VOICE_EVAC_OLD_ALT("simplex_voice_evac_old_alt"),
    PANEL_COMPONENT_ADDED("PanelComponentAdded"),
    NOTIFIER_VOICE_EVAC_ALT("notifier_voice_evac_alt"),
    CODE3_BELL("code3_bell"),
    CODE44_BELL("code44_bell"),
    CONTINUOUS_BELL("continuous_bell"),
    MARCHTIME_BELL("marchtime_bell"),
    MCLA_TORNADO_EVAC("mcla_tornado_evac"),
    NEW_SIMPLEX_BEEP("new_simplex_beep"),
    POWER_LINKED("power_linked"),
    POWER_UNLINKED("power_unlinked"),
    SP_SWITCH_OFF("sp_switch_off"),
    SP_SWITCH_ON("sp_switch_on"),
    STANDARD_SWITCH_OFF("standard_switch_off"),
    STANDARD_SWITCH_ON("standard_switch_on"),
    TL_SWITCH_OFF("tl_switch_off"),
    TL_SWITCH_ON("tl_switch_on"),
    UPS_LOWBATT("ups_lowbatt"),
    UPS_SILENCE("ups_silence"),
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
    AWFUL_NOTIFIER_VE("awful_notifier_ve"),
    NOTIFIER_VOICE_EVAC_ALT2("notifier_voice_evac_alt2"),
    NOTIFIER_TORNADO_VOICE_EVAC("notifier_tornado_voice_evac"),
    SONY_DREAM_MACHINE_1980S("sony_dream_machine_1980s"),
    OLDRADIO2("oldradio2"),
    THREESTOOGES_VE("threestooges_ve"),
    MARCHTIME_AS("marchtime_as"),
    CROSSWALK_COOKOO_1("crosswalk_cookoo_1"),
    CROSSWALK_COOKOO_2("crosswalk_cookoo_2"),
    FIRECOM8500("firecom8500"),
    CAMPBELL_PERC_EW("campbell_perc_ew"),
    CAMPBELL_PERC_NS("campbell_perc_ns"),
    CAMPBELL_PHIL_WAIT("campbell_phil_wait"),
    CAMPBELL_PHIL_WAIT_LOOK_BOTH_WAYS("campbell_phil_wait_look_both_ways"),
    CAMPBELL_PHIL_WAIT_TO_CROSS("campbell_phil_wait_to_cross"),
    CAMPBELL_PHIL_WALK_EXCLUSIVE("campbell_phil_walk_exclusive"),
    CAMPBELL_PHIL_WALK_ON("campbell_phil_walk_on"),
    CAMPBELL_PHIL_WALK_ON_TO_CROSS("campbell_phil_walk_on_to_cross"),
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
     * @since 2.0
     */
    final String soundName;

    /**
     * Constructor for a new sound.
     *
     * @param soundName the name of the sound.
     *
     * @since 2.0
     */
    SOUND(String soundName) {
      this.soundName = soundName;
    }

    /**
     * Gets the sound event for this sound.
     *
     * @return the sound event.
     *
     * @since 2.0
     */
    public SoundEvent getSoundEvent() {
      return SoundEvent.REGISTRY.getObject(getSoundLocation());
    }

    /**
     * Gets the resource location of the sound.
     *
     * @return the resource location of the sound.\
     *
     * @since 2.0
     */
    public ResourceLocation getSoundLocation() {
      return new ResourceLocation(CsmConstants.MOD_NAMESPACE, soundName);
    }

    /**
     * Registers the sound event for this sound with the sound event registry.
     * <p>This method should not be used or accessed directly as it is only for initialization.</p>
     */
    private void registerSound(RegistryEvent.Register<SoundEvent> event) {
      final ResourceLocation soundResourceLocation = getSoundLocation();
      final SoundEvent soundEvent = new SoundEvent(soundResourceLocation).setRegistryName(
          soundResourceLocation);
      event.getRegistry().register(soundEvent);
    }
  }
}
