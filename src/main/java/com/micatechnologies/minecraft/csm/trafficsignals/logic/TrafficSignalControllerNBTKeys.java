package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.Arrays;
import java.util.List;

/**
 * Constants class for the
 * {@link com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalController} class
 * which provides the NBT keys for the various NBT methods without cluttering the class and possibly
 * making it more difficult to interpret/understand.
 *
 * <p>The constants whose names match the settings they store (e.g. {@link #MODE},
 * {@link #OPERATING_MODE}) point to short mnemonic NBT key strings. Each constant is paired with a
 * {@code LEGACY_*} counterpart that preserves the historical long key name for backwards
 * compatibility when reading older worlds. Reads should probe the short key first, then fall back
 * to the legacy key and remove it once migrated. Writes should only emit the short key.
 *
 * @author Mica Technologies
 * @version 1.1
 * @since 2023.2.0
 */
public class TrafficSignalControllerNBTKeys {
  ///region: Current Format Keys (v2.0 short)
  //
  // NOTE: do NOT rename these Java identifiers. They are referenced widely across the controller
  // class. Only the literal strings they point to should change if a key is further shortened —
  // and in that case a new LEGACY_* constant must be added covering every prior form.

  /**
   * The key for storing and retrieving the controller's mode from NBT data.
   *
   * @since 2.0
   */
  public static final String MODE = "tcMode";

  /**
   * The key for storing and retrieving the controller's operating mode from NBT data.
   *
   * @since 2.0
   */
  public static final String OPERATING_MODE = "tcOm";

  /**
   * The key for storing and retrieving the traffic signal controller's paused state from NBT data.
   *
   * @since 2.0
   */
  public static final String PAUSED = "tcPs";

  /**
   * The key for storing and retrieving the traffic signal controller's circuits from NBT data.
   *
   * @since 2.0
   */
  public static final String CIRCUITS = "tcCrc";

  /**
   * The key for storing and retrieving the traffic signal controller's overlaps from NBT data.
   *
   * @since 2.0
   */
  public static final String OVERLAPS = "tcOv";

  /**
   * The key for storing and retrieving the traffic signal controller's cached phases from NBT
   * data.
   *
   * @since 2.0
   */
  public static final String CACHED_PHASES = "tcPh";

  /**
   * The key for storing and retrieving the traffic signal controller's last phase change time from
   * NBT data.
   *
   * @since 2.0
   */
  public static final String LAST_PHASE_CHANGE_TIME = "tcPcT";

  /**
   * The key for storing and retrieving the traffic signal controller's last phase applicability
   * change time from NBT data.
   *
   * @since 2.0
   */
  public static final String LAST_PHASE_APPLICABILITY_CHANGE_TIME = "tcPaT";

  /**
   * The key for storing and retrieving the traffic signal controller's last pedestrian phase time
   * from NBT data.
   *
   * @since 2.0
   */
  public static final String LAST_PEDESTRIAN_PHASE_TIME = "tcPdT";

  /**
   * The key for storing and retrieving the traffic signal controller's current phase from NBT
   * data.
   *
   * @since 2.0
   */
  public static final String CURRENT_PHASE = "tcCp";

  /**
   * The key for storing and retrieving the traffic signal controller's current fault message from
   * NBT data.
   *
   * @since 2.0
   */
  public static final String CURRENT_FAULT_MESSAGE = "tcFm";

  /**
   * The key for storing and retrieving the traffic signal controller's nightly fallback to flash
   * mode setting from NBT data.
   *
   * @since 2.0
   */
  public static final String NIGHTLY_FALLBACK_FLASH_MODE = "tcNfm";

  /**
   * The key for storing and retrieving the traffic signal controller's power loss fallback to flash
   * mode setting from NBT data.
   *
   * @since 2.0
   */
  public static final String POWER_LOSS_FALLBACK_FLASH_MODE = "tcPfm";

  /**
   * The key for storing and retrieving the traffic signal controller's overlap pedestrian signals
   * setting from NBT data.
   *
   * @since 2.0
   */
  public static final String OVERLAP_PEDESTRIAN_SIGNALS = "tcOvp";

  /**
   * The key for storing and retrieving the traffic signal controller's yellow time setting from NBT
   * data.
   *
   * @since 2.0
   */
  public static final String YELLOW_TIME = "tcYt";

  /**
   * The key for storing and retrieving the traffic signal controller's flashing don't walk time
   * setting from NBT data.
   *
   * @since 2.0
   */
  public static final String FLASH_DONT_WALK_TIME = "tcFdw";

  /**
   * The key for storing and retrieving the traffic signal controller's all red time setting from
   * NBT data.
   *
   * @since 2.0
   */
  public static final String ALL_RED_TIME = "tcArt";

  /**
   * The key for storing and retrieving the traffic signal controller's minimum requestable service
   * time setting from NBT data.
   *
   * @since 2.0
   */
  public static final String MIN_REQUESTABLE_SERVICE_TIME = "tcMnR";

  /**
   * The key for storing and retrieving the traffic signal controller's maximum requestable service
   * time setting from NBT data.
   *
   * @since 2.0
   */
  public static final String MAX_REQUESTABLE_SERVICE_TIME = "tcMxR";

  /**
   * The key for storing and retrieving the traffic signal controller's minimum green time setting
   * from NBT data.
   *
   * @since 2.0
   */
  public static final String MIN_GREEN_TIME = "tcMnG";

  /**
   * The key for storing and retrieving the traffic signal controller's maximum green time setting
   * from NBT data.
   *
   * @since 2.0
   */
  public static final String MAX_GREEN_TIME = "tcMxG";

  /**
   * The key for storing and retrieving the traffic signal controller's secondary minimum green time
   * setting from NBT data.
   *
   * @since 2.0
   */
  public static final String MIN_GREEN_TIME_SECONDARY = "tcMnGs";

  /**
   * The key for storing and retrieving the traffic signal controller's secondary maximum green time
   * setting from NBT data.
   *
   * @since 2.0
   */
  public static final String MAX_GREEN_TIME_SECONDARY = "tcMxGs";

  /**
   * The key for storing and retrieving the traffic signal controller's dedicated pedestrian signal
   * time setting from NBT data.
   *
   * @since 2.0
   */
  public static final String DEDICATED_PED_SIGNAL_TIME = "tcDps";

  /**
   * The key for storing and retrieving the traffic signal controller's upgraded previous NBT format
   * setting from NBT data.
   *
   * @since 2.0
   */
  public static final String UPGRADED_PREVIOUS_NBT_FORMAT = "tcUp";

  /**
   * The key for storing and retrieving the traffic signal controller's lead pedestrian interval
   * time setting from NBT data.
   *
   * @since 2.0
   */
  public static final String LEAD_PEDESTRIAN_INTERVAL_TIME = "tcLpi";

  /**
   * The key for storing and retrieving the traffic signal controller's all red flash setting from
   * NBT data.
   *
   * @since 2.0
   */
  public static final String ALL_RED_FLASH = "tcArF";

  /**
   * The key for storing and retrieving the traffic signal controller's ramp meter night mode
   * setting from NBT data.
   *
   * @since 2.0
   */
  public static final String RAMP_METER_NIGHT_MODE = "tcRmN";

  ///endregion

  ///region: Legacy v2.0 long-form keys (read fallback only)
  //
  // These were the original v2.0 NBT key names used between 2023.2.0 and the NBT size
  // optimization pass. They are retained here solely so readNBT can fall back to them when
  // loading worlds that were last saved before the short-key migration. writeNBT must not
  // emit them. Once loaded, the controller's next save will write only the short-form key
  // and these tags will be removed from the compound.

  /** @since 1.1 (short-key optimization) */
  public static final String LEGACY_OPERATING_MODE = "tcOperatingMode";
  /** @since 1.1 */
  public static final String LEGACY_PAUSED = "tcPaused";
  /** @since 1.1 */
  public static final String LEGACY_CIRCUITS = "tcCircuits";
  /** @since 1.1 */
  public static final String LEGACY_OVERLAPS = "tcOverlaps";
  /** @since 1.1 */
  public static final String LEGACY_CACHED_PHASES = "tcCachedPhases";
  /** @since 1.1 */
  public static final String LEGACY_LAST_PHASE_CHANGE_TIME = "tcLastPhaseChangeTime";
  /** @since 1.1 */
  public static final String LEGACY_LAST_PHASE_APPLICABILITY_CHANGE_TIME =
      "tcLastPhaseApplicabilityChangeTime";
  /** @since 1.1 */
  public static final String LEGACY_LAST_PEDESTRIAN_PHASE_TIME = "tcLastPedPhaseTime";
  /** @since 1.1 */
  public static final String LEGACY_CURRENT_PHASE = "tcCurrentPhase";
  /** @since 1.1 */
  public static final String LEGACY_CURRENT_FAULT_MESSAGE = "tcCurrentFaultMessage";
  /** @since 1.1 */
  public static final String LEGACY_NIGHTLY_FALLBACK_FLASH_MODE = "tcNightlyFallbackToFlashMode";
  /** @since 1.1 */
  public static final String LEGACY_POWER_LOSS_FALLBACK_FLASH_MODE =
      "tcPowerLossFallbackToFlashMode";
  /** @since 1.1 */
  public static final String LEGACY_OVERLAP_PEDESTRIAN_SIGNALS = "tcOverlapPedestrianSignals";
  /** @since 1.1 */
  public static final String LEGACY_YELLOW_TIME = "tcYellowTime";
  /** @since 1.1 */
  public static final String LEGACY_FLASH_DONT_WALK_TIME = "tcFlashDontWalkTime";
  /** @since 1.1 */
  public static final String LEGACY_ALL_RED_TIME = "tcAllRedTime";
  /** @since 1.1 */
  public static final String LEGACY_MIN_REQUESTABLE_SERVICE_TIME = "tcMinRequestableServiceTime";
  /** @since 1.1 */
  public static final String LEGACY_MAX_REQUESTABLE_SERVICE_TIME = "tcMaxRequestableServiceTime";
  /** @since 1.1 */
  public static final String LEGACY_MIN_GREEN_TIME = "tcMinGreenTime";
  /** @since 1.1 */
  public static final String LEGACY_MAX_GREEN_TIME = "tcMaxGreenTime";
  /** @since 1.1 */
  public static final String LEGACY_MIN_GREEN_TIME_SECONDARY = "tcMinGreenSecondaryTime";
  /** @since 1.1 */
  public static final String LEGACY_MAX_GREEN_TIME_SECONDARY = "tcMaxGreenSecondaryTime";
  /** @since 1.1 */
  public static final String LEGACY_DEDICATED_PED_SIGNAL_TIME = "tcDedicatedPedSignalTime";
  /** @since 1.1 */
  public static final String LEGACY_UPGRADED_PREVIOUS_NBT_FORMAT = "tcUpgradedPreviousNbtFormat";
  /** @since 1.1 */
  public static final String LEGACY_LEAD_PEDESTRIAN_INTERVAL_TIME = "tcLeadPedestrianIntervalTime";
  /** @since 1.1 */
  public static final String LEGACY_ALL_RED_FLASH = "tcAllRedFlash";
  /** @since 1.1 */
  public static final String LEGACY_RAMP_METER_NIGHT_MODE = "tcRampMeterNightMode";

  /**
   * List of every legacy long-form v2.0 key. {@code readNBT} strips any that are still present
   * once it has migrated them to the short form, so subsequent writes produce only short keys.
   *
   * @since 1.1
   */
  public static final List<String> LEGACY_V2_KEY_LIST = Arrays.asList(
      LEGACY_OPERATING_MODE,
      LEGACY_PAUSED,
      LEGACY_CIRCUITS,
      LEGACY_OVERLAPS,
      LEGACY_CACHED_PHASES,
      LEGACY_LAST_PHASE_CHANGE_TIME,
      LEGACY_LAST_PHASE_APPLICABILITY_CHANGE_TIME,
      LEGACY_LAST_PEDESTRIAN_PHASE_TIME,
      LEGACY_CURRENT_PHASE,
      LEGACY_CURRENT_FAULT_MESSAGE,
      LEGACY_NIGHTLY_FALLBACK_FLASH_MODE,
      LEGACY_POWER_LOSS_FALLBACK_FLASH_MODE,
      LEGACY_OVERLAP_PEDESTRIAN_SIGNALS,
      LEGACY_YELLOW_TIME,
      LEGACY_FLASH_DONT_WALK_TIME,
      LEGACY_ALL_RED_TIME,
      LEGACY_MIN_REQUESTABLE_SERVICE_TIME,
      LEGACY_MAX_REQUESTABLE_SERVICE_TIME,
      LEGACY_MIN_GREEN_TIME,
      LEGACY_MAX_GREEN_TIME,
      LEGACY_MIN_GREEN_TIME_SECONDARY,
      LEGACY_MAX_GREEN_TIME_SECONDARY,
      LEGACY_DEDICATED_PED_SIGNAL_TIME,
      LEGACY_UPGRADED_PREVIOUS_NBT_FORMAT,
      LEGACY_LEAD_PEDESTRIAN_INTERVAL_TIME,
      LEGACY_ALL_RED_FLASH,
      LEGACY_RAMP_METER_NIGHT_MODE);

  ///endregion

  ///region: Previous Format Keys (v1.1)

  /**
   * The previous format's key for storing and retrieving the serialized signal circuit list from
   * NBT data.
   *
   * @since 2.0
   */
  public static final String V1_KEY_SERIALIZED_SIGNAL_CIRCUIT_LIST = "SerializedSignalCircuitList";

  /**
   * The previous format's separator between entries in the serialized signal circuit list from NBT
   * data.
   *
   * @since 2.0
   */
  public static final String V1_SERIALIZED_SIGNAL_CIRCUIT_LIST_SEPARATOR = ":";

  /**
   * The previous format's key for storing and retrieving the serialized signal state list from NBT
   * data.
   *
   * @since 2.0
   */
  public static final String V1_KEY_SERIALIZED_SIGNAL_STATE_LIST = "SerializedSignalStateList";

  /**
   * The previous format's separator between entries in the serialized signal state list from NBT
   * data.
   *
   * @since 2.0
   */
  public static final String V1_SERIALIZED_SIGNAL_STATE_LIST_SEPARATOR = ":";

  /**
   * The previous format's key for storing and retrieving the serialized signal flash state list
   * from NBT data.
   *
   * @since 2.0
   */
  public static final String V1_KEY_SERIALIZED_SIGNAL_FLASH_STATE_LIST =
      "SerializedSignalFlashStateList";

  /**
   * The previous format's separator between entries in the serialized signal flash state list from
   * NBT data.
   *
   * @since 2.0
   */
  public static final String V1_SERIALIZED_SIGNAL_FLASH_STATE_LIST_SEPARATOR = ":";

  /**
   * The previous format's key for storing and retrieving the bootsafe boolean flag from NBT data.
   *
   * @since 2.0
   */
  public static final String V1_KEY_BOOT_SAFE = "BootSafe";

  /**
   * The previous format's key for storing and retrieving the bootsafe flash alternating boolean
   * flag from NBT data.
   *
   * @since 2.0
   */
  public static final String V1_KEY_BOOT_SAFE_FLASH = "BootSafeFlash";

  /**
   * The previous format's key for storing and retrieving the last phase change time from NBT data.
   *
   * @since 2.0
   */
  public static final String V1_KEY_LAST_PHASE_CHANGE_TIME = "LastPhaseChangeTime";

  /**
   * The previous format's key for storing and retrieving the current phase time from NBT data.
   *
   * @since 2.0
   */
  public static final String V1_KEY_CURR_PHASE_TIME = "CurrPhaseTime";

  /**
   * The previous format's key for storing and retrieving the current phase from NBT data.
   *
   * @since 2.0
   */
  public static final String V1_KEY_CURRENT_PHASE = "CurrPhase";

  /**
   * The previous format's key for storing and retrieving the current mode from NBT data.
   *
   * @since 2.0
   */
  public static final String V1_KEY_CURRENT_MODE = "CurrentMode";

  /**
   * The previous format's current mode value for flash mode from NBT data.
   *
   * @since 2.0
   */
  public static final int V1_CURRENT_MODE_FLASH = 0;

  /**
   * The previous format's current mode value for standard mode from NBT data.
   *
   * @since 2.0
   */
  public static final int V1_CURRENT_MODE_STANDARD = 1;

  /**
   * The previous format's current mode value for ramp meter mode from NBT data.
   *
   * @since 2.0
   */
  public static final int V1_CURRENT_MODE_METER = 2;

  /**
   * The previous format's current mode value for requestable mode from NBT data.
   *
   * @since 2.0
   */
  public static final int V1_CURRENT_MODE_REQUESTABLE = 3;

  /**
   * The previous format's current mode value for standard flash at night mode from NBT data.
   *
   * @since 2.0
   */
  public static final int V1_CURRENT_MODE_STANDARD_FLASH_NIGHT = 4;

  /**
   * The previous format's current mode value for standard flash at no power mode from NBT data.
   *
   * @since 2.0
   */
  public static final int V1_CURRENT_MODE_STANDARD_FLASH_NO_POWER = 5;

  /**
   * The previous format's current mode value for standard flash at no power and at night mode from
   * NBT data.
   *
   * @since 2.0
   */
  public static final int V1_CURRENT_MODE_STANDARD_FLASH_NO_POWER_NIGHT = 6;

  /**
   * The previous format's NBT data key list.
   *
   * @since 2.0
   */
  public static final List<String> V1_KEY_LIST = Arrays.asList(
      V1_KEY_SERIALIZED_SIGNAL_CIRCUIT_LIST,
      V1_KEY_SERIALIZED_SIGNAL_STATE_LIST,
      V1_KEY_SERIALIZED_SIGNAL_FLASH_STATE_LIST,
      V1_KEY_BOOT_SAFE, V1_KEY_BOOT_SAFE_FLASH,
      V1_KEY_LAST_PHASE_CHANGE_TIME,
      V1_KEY_CURR_PHASE_TIME, V1_KEY_CURRENT_PHASE,
      V1_KEY_CURRENT_MODE);

  ///endregion
}
