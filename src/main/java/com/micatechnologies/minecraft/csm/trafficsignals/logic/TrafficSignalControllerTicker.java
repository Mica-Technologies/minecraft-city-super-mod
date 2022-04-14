package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.world.World;

import java.util.ArrayList;

/**
 * Utility class for ticking the traffic signal controller tile entity
 * ({@link com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalController}) in supported
 * {@link TrafficSignalControllerMode}s.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2023.2.0
 */
public class TrafficSignalControllerTicker
{

    /**
     * The minimum time (in milliseconds) that a traffic signal controller in requestable mode must be in the default
     * green phase before it can change phases.
     *
     * @since 1.0
     */
    public static final long REQUESTABLE_MODE_DEFAULT_GREEN_MIN_TIME_MS = 10000L;

    /**
     * Handles the tick event for the traffic signal controller and passes the event to the appropriate tick method
     * based on the mode of the traffic signal controller.
     *
     * @param world                                 The world in which the traffic signal controller is located.
     * @param configuredMode                        The configured mode of the traffic signal controller.
     * @param operatingMode                         The operating mode of the traffic signal controller.
     * @param circuits                              The configured/connected circuits of the traffic signal controller.
     * @param cachedPhases                          The programmed phases of the traffic signal controller.
     * @param originalPhase                         The original (current) phase of the traffic signal controller.
     * @param timeSinceLastPhaseApplicabilityChange The time since the traffic signal controller last changed phase
     *                                              applicability.
     * @param timeSinceLastPhaseChange              The time since the traffic signal controller last changed phases.
     * @param alternatingFlash                      The alternating flash state of the traffic signal controller. This
     *                                              boolean value alternates between true and false each tick and is
     *                                              used to control the flashing of traffic signal devices.
     * @param yellowTimeMs                          The yellow time for the traffic signal controller in milliseconds.
     * @param flashDontWalkTimeMs                   The flashing don't walk time for the traffic signal controller in
     *                                              milliseconds.
     * @param allRedTimeMs                          The all red time for the traffic signal controller in milliseconds.
     * @param minRequestableServiceTimeMs           The minimum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     * @param maxRequestableServiceTimeMs           The maximum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase tick( World world,
                                           TrafficSignalControllerMode configuredMode,
                                           TrafficSignalControllerMode operatingMode,
                                           TrafficSignalControllerCircuits circuits,
                                           TrafficSignalPhases cachedPhases,
                                           TrafficSignalPhase originalPhase,
                                           long timeSinceLastPhaseApplicabilityChange,
                                           long timeSinceLastPhaseChange,
                                           boolean alternatingFlash,
                                           long yellowTimeMs,
                                           long flashDontWalkTimeMs,
                                           long allRedTimeMs,
                                           long minRequestableServiceTimeMs,
                                           long maxRequestableServiceTimeMs )
    {
        // Call appropriate tick method based on mode
        switch ( operatingMode ) {
            case FORCED_FAULT:
                return faultModeTick( world, configuredMode, operatingMode, circuits, cachedPhases, originalPhase,
                                      timeSinceLastPhaseApplicabilityChange, timeSinceLastPhaseChange, alternatingFlash,
                                      yellowTimeMs, flashDontWalkTimeMs, allRedTimeMs, minRequestableServiceTimeMs,
                                      maxRequestableServiceTimeMs );
            case MANUAL_OFF:
                return manualOffModeTick( world, configuredMode, operatingMode, circuits, cachedPhases, originalPhase,
                                          timeSinceLastPhaseApplicabilityChange, timeSinceLastPhaseChange,
                                          alternatingFlash, yellowTimeMs, flashDontWalkTimeMs, allRedTimeMs,
                                          minRequestableServiceTimeMs, maxRequestableServiceTimeMs );
            case REQUESTABLE:
                return requestableModeTick( world, configuredMode, operatingMode, circuits, cachedPhases, originalPhase,
                                            timeSinceLastPhaseApplicabilityChange, timeSinceLastPhaseChange,
                                            alternatingFlash, yellowTimeMs, flashDontWalkTimeMs, allRedTimeMs,
                                            minRequestableServiceTimeMs, maxRequestableServiceTimeMs );
            case RAMP_METER_FULL_TIME:
                return rampMeterFullTimeModeTick( world, configuredMode, operatingMode, circuits, cachedPhases,
                                                  originalPhase, timeSinceLastPhaseApplicabilityChange,
                                                  timeSinceLastPhaseChange, alternatingFlash, yellowTimeMs,
                                                  flashDontWalkTimeMs, allRedTimeMs, minRequestableServiceTimeMs,
                                                  maxRequestableServiceTimeMs );
            case RAMP_METER_PART_TIME:
                return rampMeterPartTimeModeTick( world, configuredMode, operatingMode, circuits, cachedPhases,
                                                  originalPhase, timeSinceLastPhaseApplicabilityChange,
                                                  timeSinceLastPhaseChange, alternatingFlash, yellowTimeMs,
                                                  flashDontWalkTimeMs, allRedTimeMs, minRequestableServiceTimeMs,
                                                  maxRequestableServiceTimeMs );
            case NORMAL:
                return normalModeTick( world, configuredMode, operatingMode, circuits, cachedPhases, originalPhase,
                                       timeSinceLastPhaseApplicabilityChange, timeSinceLastPhaseChange,
                                       alternatingFlash, yellowTimeMs, flashDontWalkTimeMs, allRedTimeMs,
                                       minRequestableServiceTimeMs, maxRequestableServiceTimeMs );
            case FLASH:
            default:
                return flashModeTick( world, configuredMode, operatingMode, circuits, cachedPhases, originalPhase,
                                      timeSinceLastPhaseApplicabilityChange, timeSinceLastPhaseChange, alternatingFlash,
                                      yellowTimeMs, flashDontWalkTimeMs, allRedTimeMs, minRequestableServiceTimeMs,
                                      maxRequestableServiceTimeMs );
        }
    }

    /**
     * Handles the tick event for the traffic signal controller in {@link TrafficSignalControllerMode#FLASH} mode.
     *
     * @param world                                 The world in which the traffic signal controller is located.
     * @param configuredMode                        The configured mode of the traffic signal controller.
     * @param operatingMode                         The operating mode of the traffic signal controller.
     * @param circuits                              The configured/connected circuits of the traffic signal controller.
     * @param cachedPhases                          The programmed phases of the traffic signal controller.
     * @param originalPhase                         The original (current) phase of the traffic signal controller.
     * @param timeSinceLastPhaseApplicabilityChange The time since the traffic signal controller last changed phase
     *                                              applicability.
     * @param timeSinceLastPhaseChange              The time since the traffic signal controller last changed phases.
     * @param alternatingFlash                      The alternating flash state of the traffic signal controller. This
     *                                              boolean value alternates between true and false each tick and is
     *                                              used to control the flashing of traffic signal devices.
     * @param yellowTimeMs                          The yellow time for the traffic signal controller in milliseconds.
     * @param flashDontWalkTimeMs                   The flashing don't walk time for the traffic signal controller in
     *                                              milliseconds.
     * @param allRedTimeMs                          The all red time for the traffic signal controller in milliseconds.
     * @param minRequestableServiceTimeMs           The minimum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     * @param maxRequestableServiceTimeMs           The maximum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @implNote This method always returns the {@link TrafficSignalPhase} from {@code cachedPhases} index 0 or
     *         1, depending on the value of {@code alternatingFlash}.
     * @since 1.0
     */
    public static TrafficSignalPhase flashModeTick( World world,
                                                    TrafficSignalControllerMode configuredMode,
                                                    TrafficSignalControllerMode operatingMode,
                                                    TrafficSignalControllerCircuits circuits,
                                                    TrafficSignalPhases cachedPhases,
                                                    TrafficSignalPhase originalPhase,
                                                    long timeSinceLastPhaseApplicabilityChange,
                                                    long timeSinceLastPhaseChange,
                                                    boolean alternatingFlash,
                                                    long yellowTimeMs,
                                                    long flashDontWalkTimeMs,
                                                    long allRedTimeMs,
                                                    long minRequestableServiceTimeMs,
                                                    long maxRequestableServiceTimeMs )
    {
        TrafficSignalPhase flashPhase;

        // If configured mode is ramp meter, return ramp meter specific alternating flash phase from cached phases
        if ( configuredMode == TrafficSignalControllerMode.RAMP_METER_FULL_TIME ||
                configuredMode == TrafficSignalControllerMode.RAMP_METER_PART_TIME ) {
            flashPhase = cachedPhases.getPhase( alternatingFlash ?
                                                TrafficSignalPhases.PHASE_INDEX_RAMP_METER_FLASH_1 :
                                                TrafficSignalPhases.PHASE_INDEX_RAMP_METER_FLASH_2 );
        }
        // Otherwise, return standard alternating flash phase from cached phases
        else {
            flashPhase = cachedPhases.getPhase( alternatingFlash ?
                                                TrafficSignalPhases.PHASE_INDEX_FLASH_1 :
                                                TrafficSignalPhases.PHASE_INDEX_FLASH_2 );
        }

        return flashPhase;
    }

    /**
     * Handles the tick event for the traffic signal controller in {@link TrafficSignalControllerMode#NORMAL} mode.
     * TODO: Implement normal mode
     *
     * @param world                                 The world in which the traffic signal controller is located.
     * @param configuredMode                        The configured mode of the traffic signal controller.
     * @param operatingMode                         The operating mode of the traffic signal controller.
     * @param circuits                              The configured/connected circuits of the traffic signal controller.
     * @param cachedPhases                          The programmed phases of the traffic signal controller.
     * @param originalPhase                         The original (current) phase of the traffic signal controller.
     * @param timeSinceLastPhaseApplicabilityChange The time since the traffic signal controller last changed phase
     *                                              applicability.
     * @param timeSinceLastPhaseChange              The time since the traffic signal controller last changed phases.
     * @param alternatingFlash                      The alternating flash state of the traffic signal controller. This
     *                                              boolean value alternates between true and false each tick and is
     *                                              used to control the flashing of traffic signal devices.
     * @param yellowTimeMs                          The yellow time for the traffic signal controller in milliseconds.
     * @param flashDontWalkTimeMs                   The flashing don't walk time for the traffic signal controller in
     *                                              milliseconds.
     * @param allRedTimeMs                          The all red time for the traffic signal controller in milliseconds.
     * @param minRequestableServiceTimeMs           The minimum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     * @param maxRequestableServiceTimeMs           The maximum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase normalModeTick( World world,
                                                     TrafficSignalControllerMode configuredMode,
                                                     TrafficSignalControllerMode operatingMode,
                                                     TrafficSignalControllerCircuits circuits,
                                                     TrafficSignalPhases cachedPhases,
                                                     TrafficSignalPhase originalPhase,
                                                     long timeSinceLastPhaseApplicabilityChange,
                                                     long timeSinceLastPhaseChange,
                                                     boolean alternatingFlash,
                                                     long yellowTimeMs,
                                                     long flashDontWalkTimeMs,
                                                     long allRedTimeMs,
                                                     long minRequestableServiceTimeMs,
                                                     long maxRequestableServiceTimeMs )
    {
        // Create variable to store next phase (null phase indicates no change)
        TrafficSignalPhase nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_OFF );

        // Return next phase (null phase indicates no change)
        return nextPhase;
    }

    /**
     * Handles the tick event for the traffic signal controller in
     * {@link TrafficSignalControllerMode#RAMP_METER_FULL_TIME} mode.
     *
     * @param world                                 The world in which the traffic signal controller is located.
     * @param configuredMode                        The configured mode of the traffic signal controller.
     * @param operatingMode                         The operating mode of the traffic signal controller.
     * @param circuits                              The configured/connected circuits of the traffic signal controller.
     * @param cachedPhases                          The programmed phases of the traffic signal controller.
     * @param originalPhase                         The original (current) phase of the traffic signal controller.
     * @param timeSinceLastPhaseApplicabilityChange The time since the traffic signal controller last changed phase
     *                                              applicability.
     * @param timeSinceLastPhaseChange              The time since the traffic signal controller last changed phases.
     * @param alternatingFlash                      The alternating flash state of the traffic signal controller. This
     *                                              boolean value alternates between true and false each tick and is
     *                                              used to control the flashing of traffic signal devices.
     * @param yellowTimeMs                          The yellow time for the traffic signal controller in milliseconds.
     * @param flashDontWalkTimeMs                   The flashing don't walk time for the traffic signal controller in
     *                                              milliseconds.
     * @param allRedTimeMs                          The all red time for the traffic signal controller in milliseconds.
     * @param minRequestableServiceTimeMs           The minimum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     * @param maxRequestableServiceTimeMs           The maximum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase rampMeterFullTimeModeTick( World world,
                                                                TrafficSignalControllerMode configuredMode,
                                                                TrafficSignalControllerMode operatingMode,
                                                                TrafficSignalControllerCircuits circuits,
                                                                TrafficSignalPhases cachedPhases,
                                                                TrafficSignalPhase originalPhase,
                                                                long timeSinceLastPhaseApplicabilityChange,
                                                                long timeSinceLastPhaseChange,
                                                                boolean alternatingFlash,
                                                                long yellowTimeMs,
                                                                long flashDontWalkTimeMs,
                                                                long allRedTimeMs,
                                                                long minRequestableServiceTimeMs,
                                                                long maxRequestableServiceTimeMs )
    {
        // Create variable to store next phase (null phase indicates no change)
        TrafficSignalPhase nextPhase = null;

        // If original phase is null, switch to all red phase
        if ( originalPhase == null ) {
            nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_ALL_RED );
        }
        // If currently in green ramp meter phase, switch to all red phase
        else if ( originalPhase.getApplicability() == TrafficSignalPhaseApplicability.RAMP_METER_GREEN ) {
            nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_ALL_RED );
        }
        // Otherwise, switch to green ramp meter phase
        else {
            nextPhase = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, 0, 0,
                                                TrafficSignalPhaseApplicability.RAMP_METER_GREEN );
            for ( TrafficSignalControllerCircuit circuit : circuits.getCircuits() ) {
                // Turn circuit vehicle signals green if there are vehicles waiting
                if ( circuit.getSensorsWaitingCount( world ) > 0 ) {
                    nextPhase.addOffSignals( circuit.getFlashingLeftSignals() );
                    nextPhase.addOffSignals( circuit.getFlashingRightSignals() );
                    nextPhase.addGreenSignals( circuit.getLeftSignals() );
                    nextPhase.addGreenSignals( circuit.getRightSignals() );
                    nextPhase.addGreenSignals( circuit.getThroughSignals() );
                    nextPhase.addOffSignals( circuit.getProtectedSignals() );
                    nextPhase.addRedSignals( circuit.getPedestrianSignals() );
                    nextPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
                    nextPhase.addRedSignals( circuit.getPedestrianAccessorySignals() );
                }
                // Keep circuit signals red if there are no vehicles waiting
                else {
                    nextPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                    nextPhase.addRedSignals( circuit.getFlashingRightSignals() );
                    nextPhase.addRedSignals( circuit.getLeftSignals() );
                    nextPhase.addRedSignals( circuit.getRightSignals() );
                    nextPhase.addRedSignals( circuit.getThroughSignals() );
                    nextPhase.addOffSignals( circuit.getProtectedSignals() );
                    nextPhase.addRedSignals( circuit.getPedestrianSignals() );
                    nextPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
                    nextPhase.addRedSignals( circuit.getPedestrianAccessorySignals() );
                }
            }
        }

        // Return next phase (null phase indicates no change)
        return nextPhase;
    }

    /**
     * Handles the tick event for the traffic signal controller in
     * {@link TrafficSignalControllerMode#RAMP_METER_PART_TIME} mode.
     *
     * @param world                                 The world in which the traffic signal controller is located.
     * @param configuredMode                        The configured mode of the traffic signal controller.
     * @param operatingMode                         The operating mode of the traffic signal controller.
     * @param circuits                              The configured/connected circuits of the traffic signal controller.
     * @param cachedPhases                          The programmed phases of the traffic signal controller.
     * @param originalPhase                         The original (current) phase of the traffic signal controller.
     * @param timeSinceLastPhaseApplicabilityChange The time since the traffic signal controller last changed phase
     *                                              applicability.
     * @param timeSinceLastPhaseChange              The time since the traffic signal controller last changed phases.
     * @param alternatingFlash                      The alternating flash state of the traffic signal controller. This
     *                                              boolean value alternates between true and false each tick and is
     *                                              used to control the flashing of traffic signal devices.
     * @param yellowTimeMs                          The yellow time for the traffic signal controller in milliseconds.
     * @param flashDontWalkTimeMs                   The flashing don't walk time for the traffic signal controller in
     *                                              milliseconds.
     * @param allRedTimeMs                          The all red time for the traffic signal controller in milliseconds.
     * @param minRequestableServiceTimeMs           The minimum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     * @param maxRequestableServiceTimeMs           The maximum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase rampMeterPartTimeModeTick( World world,
                                                                TrafficSignalControllerMode configuredMode,
                                                                TrafficSignalControllerMode operatingMode,
                                                                TrafficSignalControllerCircuits circuits,
                                                                TrafficSignalPhases cachedPhases,
                                                                TrafficSignalPhase originalPhase,
                                                                long timeSinceLastPhaseApplicabilityChange,
                                                                long timeSinceLastPhaseChange,
                                                                boolean alternatingFlash,
                                                                long yellowTimeMs,
                                                                long flashDontWalkTimeMs,
                                                                long allRedTimeMs,
                                                                long minRequestableServiceTimeMs,
                                                                long maxRequestableServiceTimeMs )
    {
        // Create variable to store next phase (null phase indicates no change)
        TrafficSignalPhase nextPhase = null;

        // Check if it is currently nighttime
        boolean isNight = !world.isDaytime();

        // If nighttime, operate using part time mode tick (metering disabled)
        if ( isNight ) {
            // If original phase is null or not set to meter disabled, switch to meter disabled phase
            if ( originalPhase == null ||
                    originalPhase.getApplicability() != TrafficSignalPhaseApplicability.RAMP_METER_DISABLED ) {
                nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_RAMP_METER_DISABLED );
            }
        }
        // If not nighttime, operate using full time mode tick (metering enabled)
        else {
            // If original phase is not null and set to ramp meter disabled phase, set to ramp meter starting phase
            if ( originalPhase != null &&
                    originalPhase.getApplicability() == TrafficSignalPhaseApplicability.RAMP_METER_DISABLED ) {
                nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_RAMP_METER_STARTING );
            }
            // Otherwise pass directly to full time mode tick
            else {
                nextPhase = rampMeterFullTimeModeTick( world, configuredMode, operatingMode, circuits, cachedPhases,
                                                       originalPhase, timeSinceLastPhaseApplicabilityChange,
                                                       timeSinceLastPhaseChange, alternatingFlash, yellowTimeMs,
                                                       flashDontWalkTimeMs, allRedTimeMs, minRequestableServiceTimeMs,
                                                       maxRequestableServiceTimeMs );
            }
        }

        // Return next phase (null phase indicates no change)
        return nextPhase;
    }

    /**
     * Handles the tick event for the traffic signal controller in {@link TrafficSignalControllerMode#REQUESTABLE}
     * mode.
     *
     * @param world                                 The world in which the traffic signal controller is located.
     * @param configuredMode                        The configured mode of the traffic signal controller.
     * @param operatingMode                         The operating mode of the traffic signal controller.
     * @param circuits                              The configured/connected circuits of the traffic signal controller.
     * @param cachedPhases                          The programmed phases of the traffic signal controller.
     * @param originalPhase                         The original (current) phase of the traffic signal controller.
     * @param timeSinceLastPhaseApplicabilityChange The time since the traffic signal controller last changed phase
     *                                              applicability.
     * @param timeSinceLastPhaseChange              The time since the traffic signal controller last changed phases.
     * @param alternatingFlash                      The alternating flash state of the traffic signal controller. This
     *                                              boolean value alternates between true and false each tick and is
     *                                              used to control the flashing of traffic signal devices.
     * @param yellowTimeMs                          The yellow time for the traffic signal controller in milliseconds.
     * @param flashDontWalkTimeMs                   The flashing don't walk time for the traffic signal controller in
     *                                              milliseconds.
     * @param allRedTimeMs                          The all red time for the traffic signal controller in milliseconds.
     * @param minRequestableServiceTimeMs           The minimum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     * @param maxRequestableServiceTimeMs           The maximum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase requestableModeTick( World world,
                                                          TrafficSignalControllerMode configuredMode,
                                                          TrafficSignalControllerMode operatingMode,
                                                          TrafficSignalControllerCircuits circuits,
                                                          TrafficSignalPhases cachedPhases,
                                                          TrafficSignalPhase originalPhase,
                                                          long timeSinceLastPhaseApplicabilityChange,
                                                          long timeSinceLastPhaseChange,
                                                          boolean alternatingFlash,
                                                          long yellowTimeMs,
                                                          long flashDontWalkTimeMs,
                                                          long allRedTimeMs,
                                                          long minRequestableServiceTimeMs,
                                                          long maxRequestableServiceTimeMs )
    {
        // Create variable to store next phase (null phase indicates no change)
        TrafficSignalPhase nextPhase = null;

        // If original phase is null, switch to default green phase
        if ( originalPhase == null ) {
            nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN );
        }
        // If currently in default green phase, time met, and request count is greater than zero, start switch to
        // service phasing
        else if ( originalPhase.getApplicability() == TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN &&
                timeSinceLastPhaseApplicabilityChange >= REQUESTABLE_MODE_DEFAULT_GREEN_MIN_TIME_MS &&
                ( circuits.getCircuits()
                          .stream()
                          .mapToInt( value -> value.getPedestrianAccessoriesRequestCount( world ) )
                          .sum() > 0 ) ) {
            nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW );
        }
        // If currently in default green phase + flashing don't walk and time met, switch to next phase
        else if ( originalPhase.getApplicability() ==
                TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN_FLASH_DW &&
                timeSinceLastPhaseApplicabilityChange >= ( flashDontWalkTimeMs - yellowTimeMs ) ) {
            nextPhase = cachedPhases.getPhase( alternatingFlash ?
                                               TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_1 :
                                               TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_2 );
        }
        // If currently in default green phase + flashing don't walk + flashing yellow hawk check time or flash
        else if ( originalPhase.getApplicability() ==
                TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK ) {
            // If time met, switch to next phase
            if ( timeSinceLastPhaseApplicabilityChange >= yellowTimeMs ) {
                nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_YELLOW );
            }
            // Otherwise, set proper flashing yellow hawk phase
            else {
                nextPhase = cachedPhases.getPhase( alternatingFlash ?
                                                   TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_1 :
                                                   TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_2 );
            }
        }
        // If currently in default yellow phase, and time met, switch to next phase
        else if ( originalPhase.getApplicability() == TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_YELLOW &&
                timeSinceLastPhaseApplicabilityChange >= yellowTimeMs ) {
            nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_RED );
        }
        // If currently in default red phase, and time met, switch to next phase
        else if ( originalPhase.getApplicability() == TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_RED &&
                timeSinceLastPhaseApplicabilityChange >= allRedTimeMs ) {
            nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN );
        }
        // If currently in service green phase, and time met, check sensors and switch to next phase if applicable
        else if ( originalPhase.getApplicability() == TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN &&
                timeSinceLastPhaseApplicabilityChange >= minRequestableServiceTimeMs ) {

            // Switch to next phase if max time met
            if ( timeSinceLastPhaseApplicabilityChange >= maxRequestableServiceTimeMs ) {
                nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW );
            }
            else {
                // Otherwise, check for sensor entity count
                int entityCount = 0;
                ArrayList< TrafficSignalControllerCircuit > circuitsList = circuits.getCircuits();
                for ( int i = 1; i < circuitsList.size(); i++ ) {
                    entityCount += circuitsList.get( i ).getSensorsWaitingCount( world );
                }

                // If request count is zero, switch to next phase
                if ( entityCount == 0 ) {
                    nextPhase = cachedPhases.getPhase(
                            TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW );
                }
            }

            // If switch to next phase, reset request count
            if ( nextPhase != null ) {
                circuits.getCircuits().forEach( value -> value.resetPedestrianAccessoriesRequestCount( world ) );
            }

        }
        // If currently in service green phase + flashing don't walk and time met, switch to next phase
        else if ( originalPhase.getApplicability() ==
                TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN_FLASH_DW &&
                timeSinceLastPhaseApplicabilityChange >= ( flashDontWalkTimeMs - yellowTimeMs ) ) {
            nextPhase = cachedPhases.getPhase( alternatingFlash ?
                                               TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_1 :
                                               TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_2 );
        }
        // If currently in service green phase + flashing don't walk + flashing yellow hawk check time or flash
        else if ( originalPhase.getApplicability() ==
                TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK ) {
            // If time met, switch to next phase
            if ( timeSinceLastPhaseApplicabilityChange >= yellowTimeMs ) {
                nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_YELLOW );
            }
            // Otherwise, set proper flashing yellow hawk phase
            else {
                nextPhase = cachedPhases.getPhase( alternatingFlash ?
                                                   TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_1 :
                                                   TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_2 );
            }
        }
        // If currently in service yellow phase, and time met, switch to next phase
        else if ( originalPhase.getApplicability() == TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_YELLOW &&
                timeSinceLastPhaseApplicabilityChange >= yellowTimeMs ) {
            nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_RED );
        }
        // If currently in service red phase, and time met, switch to back default/start phase
        else if ( originalPhase.getApplicability() == TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_RED &&
                timeSinceLastPhaseApplicabilityChange >= allRedTimeMs ) {
            nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN );
        }

        // Return next phase (null phase indicates no change)
        return nextPhase;
    }

    /**
     * Handles the tick event for the traffic signal controller in {@link TrafficSignalControllerMode#MANUAL_OFF} mode.
     *
     * @param world                                 The world in which the traffic signal controller is located.
     * @param configuredMode                        The configured mode of the traffic signal controller.
     * @param operatingMode                         The operating mode of the traffic signal controller.
     * @param circuits                              The configured/connected circuits of the traffic signal controller.
     * @param cachedPhases                          The programmed phases of the traffic signal controller.
     * @param originalPhase                         The original (current) phase of the traffic signal controller.
     * @param timeSinceLastPhaseApplicabilityChange The time since the traffic signal controller last changed phase
     *                                              applicability.
     * @param timeSinceLastPhaseChange              The time since the traffic signal controller last changed phases.
     * @param alternatingFlash                      The alternating flash state of the traffic signal controller. This
     *                                              boolean value alternates between true and false each tick and is
     *                                              used to control the flashing of traffic signal devices.
     * @param yellowTimeMs                          The yellow time for the traffic signal controller in milliseconds.
     * @param flashDontWalkTimeMs                   The flashing don't walk time for the traffic signal controller in
     *                                              milliseconds.
     * @param allRedTimeMs                          The all red time for the traffic signal controller in milliseconds.
     * @param minRequestableServiceTimeMs           The minimum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     * @param maxRequestableServiceTimeMs           The maximum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @implNote This method always returns 0, which is the index of the off phase.
     * @since 1.0
     */
    public static TrafficSignalPhase manualOffModeTick( World world,
                                                        TrafficSignalControllerMode configuredMode,
                                                        TrafficSignalControllerMode operatingMode,
                                                        TrafficSignalControllerCircuits circuits,
                                                        TrafficSignalPhases cachedPhases,
                                                        TrafficSignalPhase originalPhase,
                                                        long timeSinceLastPhaseApplicabilityChange,
                                                        long timeSinceLastPhaseChange,
                                                        boolean alternatingFlash,
                                                        long yellowTimeMs,
                                                        long flashDontWalkTimeMs,
                                                        long allRedTimeMs,
                                                        long minRequestableServiceTimeMs,
                                                        long maxRequestableServiceTimeMs )
    {
        // Return off phase if original phase is null (not already set), otherwise return null (already set)
        return originalPhase == null ? cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_OFF ) : null;
    }

    /**
     * Handles the tick event for the traffic signal controller in {@link TrafficSignalControllerMode#FORCED_FAULT}
     * mode.
     *
     * @param world                                 The world in which the traffic signal controller is located.
     * @param configuredMode                        The configured mode of the traffic signal controller.
     * @param operatingMode                         The operating mode of the traffic signal controller.
     * @param circuits                              The configured/connected circuits of the traffic signal controller.
     * @param cachedPhases                          The programmed phases of the traffic signal controller.
     * @param originalPhase                         The original (current) phase of the traffic signal controller.
     * @param timeSinceLastPhaseApplicabilityChange The time since the traffic signal controller last changed phase
     *                                              applicability.
     * @param timeSinceLastPhaseChange              The time since the traffic signal controller last changed phases.
     * @param alternatingFlash                      The alternating flash state of the traffic signal controller. This
     *                                              boolean value alternates between true and false each tick and is
     *                                              used to control the flashing of traffic signal devices.
     * @param yellowTimeMs                          The yellow time for the traffic signal controller in milliseconds.
     * @param flashDontWalkTimeMs                   The flashing don't walk time for the traffic signal controller in
     *                                              milliseconds.
     * @param allRedTimeMs                          The all red time for the traffic signal controller in milliseconds.
     * @param minRequestableServiceTimeMs           The minimum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     * @param maxRequestableServiceTimeMs           The maximum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @implNote This method always returns the {@link TrafficSignalPhase} from {@code cachedPhases} index 0 or
     *         1, depending on the value of {@code alternatingFlash}.
     * @since 1.0
     */
    public static TrafficSignalPhase faultModeTick( World world,
                                                    TrafficSignalControllerMode configuredMode,
                                                    TrafficSignalControllerMode operatingMode,
                                                    TrafficSignalControllerCircuits circuits,
                                                    TrafficSignalPhases cachedPhases,
                                                    TrafficSignalPhase originalPhase,
                                                    long timeSinceLastPhaseApplicabilityChange,
                                                    long timeSinceLastPhaseChange,
                                                    boolean alternatingFlash,
                                                    long yellowTimeMs,
                                                    long flashDontWalkTimeMs,
                                                    long allRedTimeMs,
                                                    long minRequestableServiceTimeMs,
                                                    long maxRequestableServiceTimeMs )
    {
        // Return alternating fault phase (0 or 1) from cached phases
        return cachedPhases.getPhase(
                alternatingFlash ? TrafficSignalPhases.PHASE_INDEX_FAULT_1 : TrafficSignalPhases.PHASE_INDEX_FAULT_2 );
    }
}
