package com.micatechnologies.minecraft.csm.sounds;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.sys.ModConstants;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

/**
 * Mod sound class. This class contains the list of sound resources and the method for initializing sounds on mod
 * startup.
 *
 * @author ah@micatechnologies.com
 * @version 1.0
 * @since 2020.6
 */
public class Sounds
{

    /**
     * Master list of sounds in the mod. Each entry in this list MUST correspond to a sound file in
     * src/main/resources/assets/csm/sounds, but with the .ogg omitted. For example, if the sound file
     * `example_sound.ogg` was present, the entry in this array MUST be `example_sound`.
     *
     * @since 1.0
     */
    private static final String[] soundResourceList = { "bell",
                                                        "codetector",
                                                        "et70_chime",
                                                        "handdryer",
                                                        "mills_firealarm",
                                                        "mt_code3",
                                                        "nest_test",
                                                        "panasonicfan",
                                                        "2910calcode",
                                                        "4030code44",
                                                        "smokealarm",
                                                        "spectralert",
                                                        "wheelockas",
                                                        "adaptabell",
                                                        "bell2",
                                                        "firebell",
                                                        "kac",
                                                        "svenew",
                                                        "sveold",
                                                        "sven",
                                                        "sveo",
                                                        "wheelock7002t",
                                                        "dingblock",
                                                        "est_genesis",
                                                        "est_integrity",
                                                        "lms_voice_evac",
                                                        "stahorn",
                                                        "7002t_medspeed",
                                                        "7002t_slowspeed",
                                                        "edwards_adaptahorn_code44",
                                                        "gentex_gos_code3",
                                                        "sae_marchtime",
                                                        "simplex_4051_marchtime",
                                                        "broken_7002t",
                                                        "walksig_wait",
                                                        "mclalsve",
                                                        "edwards_io_reset",
                                                        "simplex_panel_reset",
                                                        "pullstation_pull",
                                                        "notifier_voice_evac",
                                                        "simplex_voice_evac_old_alt",
                                                        "PanelComponentAdded",
                                                        "notifier_voice_evac_alt",
                                                        "code3_bell",
                                                        "code44_bell",
                                                        "continuous_bell",
                                                        "marchtime_bell",
                                                        "elevator_dooropen",
                                                        "elevator_fan",
                                                        "mcla_tornado_evac",
                                                        "new_simplex_beep",
                                                        "power_linked",
                                                        "power_unlinked",
                                                        "sp_switch_off",
                                                        "sp_switch_on",
                                                        "standard_switch_off",
                                                        "standard_switch_on",
                                                        "tl_switch_off",
                                                        "tl_switch_on",
                                                        "ups_lowbatt",
                                                        "ups_silence",
                                                        "oldradio",
                                                        "asteroids_cabinet",
                                                        "bz_cabinet",
                                                        "cp_cabinet",
                                                        "galaga_cabinet",
                                                        "miscmd_cabinet",
                                                        "pacman_cabinet",
                                                        "tempest_cabinet",
                                                        "oldrecordplayer2",
                                                        "locker_door_close",
                                                        "locker_door_open",
                                                        "oldrecordplayer",
                                                        "awful_notifier_ve",
                                                        "notifier_voice_evac_alt2",
                                                        "notifier_tornado_voice_evac",
                                                        "fastservice_target_1",
                                                        "fastservice_target_2",
                                                        "fastservice_target_3",
                                                        "fastservice_target_cleared",
                                                        "target_helpbutton_press",
                                                        "target_self_checkout_thanks",
                                                        "sony_dream_machine_1980s",
                                                        "oldradio2",
                                                        "threestooges_ve",
                                                        "marchtime_as",
                                                        "alto_tornado_warning_newpaul",
                                                        "alto_tornado_warning_tom",
                                                        "alto_weather_forecast_newpaul",
                                                        "alto_weather_forecast_tom",
                                                        "male_wait",
                                                        "male_beep",
                                                        "female_automated",
                                                        "male_crosswalk_on",
                                                        "female_beep",
                                                        "female_wait",
                                                        "female_unsafe_cross",
                                                        "crosswalk_cookoo_1",
                                                        "crosswalk_cookoo_2" };

    /**
     * Sound initialization method. This method is called from {@link ElementsCitySuperMod} to load sounds in the mod.
     *
     * @since 1.0
     */
    public static void init() {
        for ( String soundName : soundResourceList ) {
            final ResourceLocation soundResourceLocation = new ResourceLocation( ModConstants.MOD_NAMESPACE,
                                                                                 soundName );
            final SoundEvent soundEvent = new SoundEvent( soundResourceLocation );
            ElementsCitySuperMod.sounds.put( soundResourceLocation, soundEvent );
        }
    }
}
