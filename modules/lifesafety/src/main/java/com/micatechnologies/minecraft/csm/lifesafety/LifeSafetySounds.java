package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.CsmSoundRegistry;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;

/**
 * The sounds shipped by the CSM: Life Safety module: every fire alarm appliance tone, voice
 * evacuation message and panel sound.
 *
 * <p>Each constant's name is the sound's {@code sounds.json} key and the path of its registry
 * name, so a sound event stays {@code csm:&lt;key&gt;} and nothing that refers to one by
 * string has to change.</p>
 *
 * @author ah@micatechnologies.com
 * @version 1.0
 * @since 2026.9
 */
public enum LifeSafetySounds implements ICsmSound {
  BELL("bell"),
  CODETECTOR("codetector"),
  ET70_CHIME("et70_chime"),
  MILLS_FIREALARM("mills_firealarm"),
  MT_CODE3("mt_code3"),
  NEST_TEST("nest_test"),
  _2910CALCODE("2910calcode"),
  _4030CODE44("4030code44"),
  SMOKEALARM("smokealarm"),
  SPECTRALERT("spectralert"),
  SPECTRALERT_CLASSIC("spectralert_classic"),
  SPECTRALERT_LF("spectralert_lf"),
  WHEELOCKAS("wheelockas"),
  ADAPTABELL("adaptabell"),
  BELL2("bell2"),
  FIREBELL("firebell"),
  KAC_CONTINUOUS("kac_continuous"),
  KAC_CODE3("kac_code3"),
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
  GENTEX_GOS_CODE3_CHIME("gentex_gos_code3_chime"),
  GENTEX_GOS_WHOOP("gentex_gos_whoop"),
  GENTEX_GOS_CONTINUOUS_CHIME("gentex_gos_continuous_chime"),
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
  AWFUL_NOTIFIER_VE("awful_notifier_ve"),
  NOTIFIER_VOICE_EVAC_ALT2("notifier_voice_evac_alt2"),
  NOTIFIER_UCLA_VOICE_EVAC("notifier_ucla_voice_evac"),
  NOTIFIER_TORNADO_VOICE_EVAC("notifier_tornado_voice_evac"),
  THREESTOOGES_VE("threestooges_ve"),
  MARCHTIME_AS("marchtime_as"),
  FIRECOM8500("firecom8500"),
  BSP_THE_LOFTS_VOIC_EVAC("bsp_the_lofts_voic_evac"),
  EST_PREINT_VOICE_EVAC("est_preint_voice_evac"),
  FCI_VOIC_EVAC_FEMALE("fci_voic_evac_female"),
  FCI_VOIC_EVAC_MALE("fci_voic_evac_male"),
  SIMPLEX_VOICE_EVAC_OLD_ALT2("simplex_voice_evac_old_alt2"),
  BROKEN_GENTEX_GOS("broken_gentex_gos");

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
  LifeSafetySounds(String soundName) {
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
