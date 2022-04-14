package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.Arrays;
import java.util.List;

/**
 * Constants class for the {@link com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalController}
 * class which provides the NBT keys for the various NBT methods without cluttering the class and possibly making it
 * more difficult to interpret/understand.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2023.2.0
 */
public class TrafficSignalControllerNBTKeys
{
    ///region: Current Format Keys (v2.0)

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
    public static final String OPERATING_MODE = "tcOperatingMode";

    /**
     * The key for storing and retrieving the traffic signal controller's paused state from NBT data.
     *
     * @since 2.0
     */
    public static final String PAUSED = "tcPaused";

    /**
     * The key for storing and retrieving the traffic signal controller's circuits from NBT data.
     *
     * @since 2.0
     */
    public static final String CIRCUITS = "tcCircuits";

    /**
     * The key for storing and retrieving the traffic signal controller's cached phases from NBT data.
     *
     * @since 2.0
     */
    public static final String CACHED_PHASES = "tcCachedPhases";

    /**
     * The key for storing and retrieving the traffic signal controller's last phase change time from NBT data.
     *
     * @since 2.0
     */
    public static final String LAST_PHASE_CHANGE_TIME = "tcLastPhaseChangeTime";

    /**
     * The key for storing and retrieving the traffic signal controller's last phase applicability change time from NBT
     * data.
     *
     * @since 2.0
     */
    public static final String LAST_PHASE_APPLICABILITY_CHANGE_TIME = "tcLastPhaseApplicabilityChangeTime";

    /**
     * The key for storing and retrieving the traffic signal controller's last phase change time from NBT data.
     *
     * @since 2.0
     */
    public static final String LAST_PEDESTRIAN_PHASE_TIME = "tcLastPedPhaseTime";

    /**
     * The key for storing and retrieving the traffic signal controller's current phase from NBT data.
     *
     * @since 2.0
     */
    public static final String CURRENT_PHASE = "tcCurrentPhase";

    /**
     * The key for storing and retrieving the traffic signal controller's current fault message from NBT data.
     *
     * @since 2.0
     */
    public static final String CURRENT_FAULT_MESSAGE = "tcCurrentFaultMessage";

    /**
     * The key for storing and retrieving the traffic signal controller's nightly fallback to flash mode setting from
     * NBT data.
     *
     * @since 2.0
     */
    public static final String NIGHTLY_FALLBACK_FLASH_MODE = "tcNightlyFallbackToFlashMode";

    /**
     * The key for storing and retrieving the traffic signal controller's power loss fallback to flash mode setting from
     * NBT data.
     *
     * @since 2.0
     */
    public static final String POWER_LOSS_FALLBACK_FLASH_MODE = "tcPowerLossFallbackToFlashMode";

    /**
     * The key for storing and retrieving the traffic signal controller's overlap pedestrian signals setting from NBT
     * data.
     *
     * @since 2.0
     */
    public static final String OVERLAP_PEDESTRIAN_SIGNALS = "tcOverlapPedestrianSignals";

    /**
     * The key for storing and retrieving the traffic signal controller's yellow time setting from NBT data.
     *
     * @since 2.0
     */
    public static final String YELLOW_TIME = "tcYellowTime";

    /**
     * The key for storing and retrieving the traffic signal controller's flashing don't walk time setting from NBT
     * data.
     *
     * @since 2.0
     */
    public static final String FLASH_DONT_WALK_TIME = "tcFlashDontWalkTime";

    /**
     * The key for storing and retrieving the traffic signal controller's all red time setting from NBT data.
     *
     * @since 2.0
     */
    public static final String ALL_RED_TIME = "tcAllRedTime";

    /**
     * The key for storing and retrieving the traffic signal controller's minimum requestable service time setting from
     * NBT data.
     *
     * @since 2.0
     */
    public static final String MIN_REQUESTABLE_SERVICE_TIME = "tcMinRequestableServiceTime";

    /**
     * The key for storing and retrieving the traffic signal controller's maximum requestable service time setting from
     * NBT data.
     *
     * @since 2.0
     */
    public static final String MAX_REQUESTABLE_SERVICE_TIME = "tcMaxRequestableServiceTime";

    /**
     * The key for storing and retrieving the traffic signal controller's minimum green time setting from NBT data.
     *
     * @since 2.0
     */
    public static final String MIN_GREEN_TIME = "tcMinGreenTime";

    /**
     * The key for storing and retrieving the traffic signal controller's maximum green time setting from NBT data.
     *
     * @since 2.0
     */
    public static final String MAX_GREEN_TIME = "tcMaxGreenTime";

    /**
     * The key for storing and retrieving the traffic signal controller's secondary minimum green time setting from NBT
     * data.
     *
     * @since 2.0
     */
    public static final String MIN_GREEN_TIME_SECONDARY = "tcMinGreenSecondaryTime";

    /**
     * The key for storing and retrieving the traffic signal controller's secondary maximum green time setting from NBT
     * data.
     *
     * @since 2.0
     */
    public static final String MAX_GREEN_TIME_SECONDARY = "tcMaxGreenSecondaryTime";

    /**
     * The key for storing and retrieving the traffic signal controller's dedicated pedestrian signal time setting from
     * NBT data.
     *
     * @since 2.0
     */
    public static final String DEDICATED_PED_SIGNAL_TIME = "tcDedicatedPedSignalTime";

    ///endregion

    ///region: Previous Format Keys (v1.1)

    /**
     * The previous format's key for storing and retrieving the serialized signal circuit list from NBT data.
     *
     * @since 2.0
     */
    public static final String V1_KEY_SERIALIZED_SIGNAL_CIRCUIT_LIST = "SerializedSignalCircuitList";

    /**
     * The previous format's separator between entries in the serialized signal circuit list from NBT data.
     *
     * @since 2.0
     */
    public static final String V1_SERIALIZED_SIGNAL_CIRCUIT_LIST_SEPARATOR = ":";

    /**
     * The previous format's key for storing and retrieving the serialized signal state list from NBT data.
     *
     * @since 2.0
     */
    public static final String V1_KEY_SERIALIZED_SIGNAL_STATE_LIST = "SerializedSignalStateList";

    /**
     * The previous format's separator between entries in the serialized signal state list from NBT data.
     *
     * @since 2.0
     */
    public static final String V1_SERIALIZED_SIGNAL_STATE_LIST_SEPARATOR = ":";

    /**
     * The previous format's key for storing and retrieving the serialized signal flash state list from NBT data.
     *
     * @since 2.0
     */
    public static final String V1_KEY_SERIALIZED_SIGNAL_FLASH_STATE_LIST = "SerializedSignalFlashStateList";

    /**
     * The previous format's separator between entries in the serialized signal flash state list from NBT data.
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
     * The previous format's key for storing and retrieving the bootsafe flash alternating boolean flag from NBT data.
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
     * The previous format's current mode value for standard flash at no power and at night mode from NBT data.
     *
     * @since 2.0
     */
    public static final int V1_CURRENT_MODE_STANDARD_FLASH_NO_POWER_NIGHT = 6;

    /**
     * The previous format's NBT data key list.
     *
     * @since 2.0
     */
    public static final List< String > V1_KEY_LIST = Arrays.asList( V1_KEY_SERIALIZED_SIGNAL_CIRCUIT_LIST,
                                                                    V1_KEY_SERIALIZED_SIGNAL_STATE_LIST,
                                                                    V1_KEY_SERIALIZED_SIGNAL_FLASH_STATE_LIST,
                                                                    V1_KEY_BOOT_SAFE, V1_KEY_BOOT_SAFE_FLASH,
                                                                    V1_KEY_LAST_PHASE_CHANGE_TIME,
                                                                    V1_KEY_CURR_PHASE_TIME, V1_KEY_CURRENT_PHASE,
                                                                    V1_KEY_CURRENT_MODE );

    ///endregion
}
