package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.Tuple;
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
     * @param overlapPedestrianSignals              The overlap pedestrian signals setting of the traffic signal
     *                                              controller. This boolean value is used to determine if the
     *                                              pedestrian signals of all other circuits should be overlapped when
     *                                              servicing a circuit.
     * @param yellowTimeMs                          The yellow time for the traffic signal controller in milliseconds.
     * @param flashDontWalkTimeMs                   The flashing don't walk time for the traffic signal controller in
     *                                              milliseconds.
     * @param allRedTimeMs                          The all red time for the traffic signal controller in milliseconds.
     * @param minRequestableServiceTimeMs           The minimum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     * @param maxRequestableServiceTimeMs           The maximum service time for the traffic signal controller (in
     *                                              milliseconds) when in requestable mode.
     * @param minGreenTimeMs                        The minimum green time for the traffic signal controller (in
     *                                              milliseconds) when in normal mode.
     * @param maxGreenTimeMs                        The maximum green time for the traffic signal controller (in
     *                                              milliseconds) when in normal mode.
     * @param minGreenTimeSecondaryMs               The secondary minimum green time for the traffic signal controller
     *                                              (in milliseconds) when in normal mode.
     * @param maxGreenTimeSecondaryMs               The secondary maximum green time for the traffic signal controller
     *                                              (in milliseconds) when in normal mode.
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
                                           boolean overlapPedestrianSignals,
                                           long yellowTimeMs,
                                           long flashDontWalkTimeMs,
                                           long allRedTimeMs,
                                           long minRequestableServiceTimeMs,
                                           long maxRequestableServiceTimeMs,
                                           long minGreenTimeMs,
                                           long maxGreenTimeMs,
                                           long minGreenTimeSecondaryMs,
                                           long maxGreenTimeSecondaryMs )
    {
        // Call appropriate tick method based on mode
        switch ( operatingMode ) {
            case FORCED_FAULT:
                return faultModeTick( cachedPhases, alternatingFlash );
            case MANUAL_OFF:
                return manualOffModeTick( cachedPhases, originalPhase );
            case REQUESTABLE:
                return requestableModeTick( world, circuits, cachedPhases, originalPhase,
                                            timeSinceLastPhaseApplicabilityChange, alternatingFlash, yellowTimeMs,
                                            flashDontWalkTimeMs, allRedTimeMs, minRequestableServiceTimeMs,
                                            maxRequestableServiceTimeMs, minGreenTimeMs );
            case RAMP_METER_FULL_TIME:
                return rampMeterFullTimeModeTick( world, circuits, cachedPhases, originalPhase );
            case RAMP_METER_PART_TIME:
                return rampMeterPartTimeModeTick( world, circuits, cachedPhases, originalPhase );
            case NORMAL:
                return normalModeTick( world, configuredMode, operatingMode, circuits, cachedPhases, originalPhase,
                                       timeSinceLastPhaseApplicabilityChange, timeSinceLastPhaseChange,
                                       alternatingFlash, overlapPedestrianSignals, yellowTimeMs, flashDontWalkTimeMs,
                                       allRedTimeMs, minGreenTimeMs, maxGreenTimeMs, minGreenTimeSecondaryMs,
                                       maxGreenTimeSecondaryMs );
            case FLASH:
            default:
                return flashModeTick( configuredMode, cachedPhases, alternatingFlash );
        }
    }

    /**
     * Handles the tick event for the traffic signal controller in {@link TrafficSignalControllerMode#FLASH} mode.
     *
     * @param configuredMode   The configured mode of the traffic signal controller.
     * @param cachedPhases     The programmed phases of the traffic signal controller.
     * @param alternatingFlash The alternating flash state of the traffic signal controller. This boolean value
     *                         alternates between true and false each tick and is used to control the flashing of
     *                         traffic signal devices.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @implNote This method always returns the {@link TrafficSignalPhase} from {@code cachedPhases} index 0 or
     *         1, depending on the value of {@code alternatingFlash}.
     * @since 1.0
     */
    public static TrafficSignalPhase flashModeTick( TrafficSignalControllerMode configuredMode,
                                                    TrafficSignalPhases cachedPhases,
                                                    boolean alternatingFlash )
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
     * @param overlapPedestrianSignals              The overlap pedestrian signals setting of the traffic signal
     *                                              controller. This boolean value is used to determine if the
     *                                              pedestrian signals of all other circuits should be overlapped when
     *                                              servicing a circuit.
     * @param yellowTimeMs                          The yellow time for the traffic signal controller in milliseconds.
     * @param flashDontWalkTimeMs                   The flashing don't walk time for the traffic signal controller in
     *                                              milliseconds.
     * @param allRedTimeMs                          The all red time for the traffic signal controller in milliseconds.
     * @param minGreenTimeMs                        The minimum green time for the traffic signal controller (in
     *                                              milliseconds) when in normal mode.
     * @param maxGreenTimeMs                        The maximum green time for the traffic signal controller (in
     *                                              milliseconds) when in normal mode.
     * @param minGreenTimeSecondaryMs               The secondary minimum green time for the traffic signal controller
     *                                              (in milliseconds) when in normal mode.
     * @param maxGreenTimeSecondaryMs               The secondary maximum green time for the traffic signal controller
     *                                              (in milliseconds) when in normal mode.
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
                                                     boolean overlapPedestrianSignals,
                                                     long yellowTimeMs,
                                                     long flashDontWalkTimeMs,
                                                     long allRedTimeMs,
                                                     long minGreenTimeMs,
                                                     long maxGreenTimeMs,
                                                     long minGreenTimeSecondaryMs,
                                                     long maxGreenTimeSecondaryMs )
    {
        // Create variable to store next phase (null phase indicates no change)
        TrafficSignalPhase nextPhase = null;

        // If original phase is null, switch to all red phase
        if ( originalPhase == null ) {
            nextPhase = cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_ALL_RED );
        }
        // If original phase is all red, change to initial green phase
        else if ( originalPhase.getApplicability() == TrafficSignalPhaseApplicability.ALL_RED &&
                timeSinceLastPhaseChange >= allRedTimeMs ) {
            // Change to initial green phase (on circuit 1)
            nextPhase = TrafficSignalControllerTickerUtilities.getDefaultPhaseForCircuitNumber( circuits, 1,
                                                                                                overlapPedestrianSignals );
        }
        // If original phase is flashing don't walk transitioning to yellow, and flashing don't walk time is up,
        // change to yellow transition phase
        else if ( originalPhase.getApplicability() == TrafficSignalPhaseApplicability.FLASH_DONT_WALK_TRANSITIONING &&
                timeSinceLastPhaseChange >= flashDontWalkTimeMs ) {
            // Change to yellow transition phase
            nextPhase = TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming( originalPhase,
                                                                                                    originalPhase.getUpcomingPhase() );
        }
        // If original phase is yellow transitioning to red, and yellow time is up, change to red transition phase
        else if ( originalPhase.getApplicability() == TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING &&
                timeSinceLastPhaseChange >= yellowTimeMs ) {
            // Change to red transition phase
            nextPhase = TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming( originalPhase,
                                                                                                 originalPhase.getUpcomingPhase() );
        }
        // If original phase is red transitioning to upcoming, and all red time is up, change to green phase
        else if ( originalPhase.getApplicability() == TrafficSignalPhaseApplicability.RED_TRANSITIONING &&
                timeSinceLastPhaseChange >= allRedTimeMs ) {
            // Set to upcoming phase
            nextPhase = originalPhase.getUpcomingPhase();

        }
        // If original phase is green, get corresponding min/max green times and check for phase change
        else {
            // Get corresponding min/max green times
            long phaseMinGreenTimeMs = originalPhase.getCircuit() == 1 ? minGreenTimeMs : minGreenTimeSecondaryMs;
            long phaseMaxGreenTimeMs = originalPhase.getCircuit() == 1 ? maxGreenTimeMs : maxGreenTimeSecondaryMs;

            // If min green time is up, check for phase change
            if ( timeSinceLastPhaseChange >= phaseMinGreenTimeMs ) {
                // Check if max green time is met
                boolean maxGreenTimeMet = ( timeSinceLastPhaseChange >= phaseMaxGreenTimeMs );

                // Get the next phase priority indicator (tells us what should be next based on traffic counts)
                Tuple< Integer, TrafficSignalPhaseApplicability > currentPhasePriorityIndicator
                        = originalPhase.getPriorityIndicator();
                Tuple< Integer, TrafficSignalPhaseApplicability > nextPhasePriorityIndicator
                        = TrafficSignalControllerTickerUtilities.getNextPhasePriorityIndicator();

                // If the current phase priority indicator is different than the next phase priority indicator, then
                // we need to change phases
                if ( nextPhasePriorityIndicator != null &&
                        ( currentPhasePriorityIndicator.getFirst().intValue() !=
                                nextPhasePriorityIndicator.getFirst().intValue() ||
                                currentPhasePriorityIndicator.getSecond() !=
                                        nextPhasePriorityIndicator.getSecond() ) ) {
                    // TODO: Generate upcoming phase based on priority indicator
                }
                // If max green time is met (and priority indicators are the same), then we need to change phases to
                // the next circuit default phase
                else if ( maxGreenTimeMet ) {
                    // Get next circuit number
                    int nextCircuitNumber = 1;
                    if ( originalPhase.getCircuit() >= 0 && originalPhase.getCircuit() < circuits.getCircuitCount() ) {
                        nextCircuitNumber = originalPhase.getCircuit() + 1;
                    }

                    // Get next circuit default phase to set as upcoming phase
                    TrafficSignalPhase upcomingPhase
                            = TrafficSignalControllerTickerUtilities.getDefaultPhaseForCircuitNumber( circuits,
                                                                                                      nextCircuitNumber,
                                                                                                      overlapPedestrianSignals );

                    // Get next phase (flash don't walk transition unless null, otherwise yellow transition)
                    nextPhase = TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
                            originalPhase, upcomingPhase );
                    if ( nextPhase == null ) {
                        nextPhase = TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
                                originalPhase, upcomingPhase );
                    }
                }

            }
        }

        // Return next phase (null phase indicates no change)
        return nextPhase;
    }

    /**
     * Handles the tick event for the traffic signal controller in
     * {@link TrafficSignalControllerMode#RAMP_METER_FULL_TIME} mode.
     *
     * @param world         The world in which the traffic signal controller is located.
     * @param circuits      The configured/connected circuits of the traffic signal controller.
     * @param cachedPhases  The programmed phases of the traffic signal controller.
     * @param originalPhase The original (current) phase of the traffic signal controller.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase rampMeterFullTimeModeTick( World world,
                                                                TrafficSignalControllerCircuits circuits,
                                                                TrafficSignalPhases cachedPhases,
                                                                TrafficSignalPhase originalPhase )
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
            nextPhase = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
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
                    nextPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
                    nextPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                    nextPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
                }
                // Keep circuit signals red if there are no vehicles waiting
                else {
                    nextPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                    nextPhase.addRedSignals( circuit.getFlashingRightSignals() );
                    nextPhase.addRedSignals( circuit.getLeftSignals() );
                    nextPhase.addRedSignals( circuit.getRightSignals() );
                    nextPhase.addRedSignals( circuit.getThroughSignals() );
                    nextPhase.addRedSignals( circuit.getProtectedSignals() );
                    nextPhase.addRedSignals( circuit.getPedestrianBeaconSignals() );
                    nextPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                    nextPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
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
     * @param world         The world in which the traffic signal controller is located.
     * @param circuits      The configured/connected circuits of the traffic signal controller.
     * @param cachedPhases  The programmed phases of the traffic signal controller.
     * @param originalPhase The original (current) phase of the traffic signal controller.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase rampMeterPartTimeModeTick( World world,
                                                                TrafficSignalControllerCircuits circuits,
                                                                TrafficSignalPhases cachedPhases,
                                                                TrafficSignalPhase originalPhase )
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
                nextPhase = rampMeterFullTimeModeTick( world, circuits, cachedPhases, originalPhase );
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
     * @param circuits                              The configured/connected circuits of the traffic signal controller.
     * @param cachedPhases                          The programmed phases of the traffic signal controller.
     * @param originalPhase                         The original (current) phase of the traffic signal controller.
     * @param timeSinceLastPhaseApplicabilityChange The time since the traffic signal controller last changed phase
     *                                              applicability.
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
     * @param minGreenTimeMs                        The minimum green time for the traffic signal controller (in
     *                                              milliseconds) when in normal mode.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase requestableModeTick( World world,
                                                          TrafficSignalControllerCircuits circuits,
                                                          TrafficSignalPhases cachedPhases,
                                                          TrafficSignalPhase originalPhase,
                                                          long timeSinceLastPhaseApplicabilityChange,
                                                          boolean alternatingFlash,
                                                          long yellowTimeMs,
                                                          long flashDontWalkTimeMs,
                                                          long allRedTimeMs,
                                                          long minRequestableServiceTimeMs,
                                                          long maxRequestableServiceTimeMs,
                                                          long minGreenTimeMs )
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
                timeSinceLastPhaseApplicabilityChange >= minGreenTimeMs &&
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
     * @param cachedPhases  The programmed phases of the traffic signal controller.
     * @param originalPhase The original (current) phase of the traffic signal controller.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @implNote This method always returns 0, which is the index of the off phase.
     * @since 1.0
     */
    public static TrafficSignalPhase manualOffModeTick( TrafficSignalPhases cachedPhases,
                                                        TrafficSignalPhase originalPhase )
    {
        // Return off phase if original phase is null (not already set), otherwise return null (already set)
        return originalPhase == null ? cachedPhases.getPhase( TrafficSignalPhases.PHASE_INDEX_OFF ) : null;
    }

    /**
     * Handles the tick event for the traffic signal controller in {@link TrafficSignalControllerMode#FORCED_FAULT}
     * mode.
     *
     * @param cachedPhases     The programmed phases of the traffic signal controller.
     * @param alternatingFlash The alternating flash state of the traffic signal controller. This boolean value
     *                         alternates between true and false each tick and is used to control the flashing of
     *                         traffic signal devices.
     *
     * @return The next phase to use for the traffic signal controller. If null is returned, then the phase is not
     *         changed.
     *
     * @implNote This method always returns the {@link TrafficSignalPhase} from {@code cachedPhases} index 0 or
     *         1, depending on the value of {@code alternatingFlash}.
     * @since 1.0
     */
    public static TrafficSignalPhase faultModeTick( TrafficSignalPhases cachedPhases, boolean alternatingFlash )
    {
        // Return alternating fault phase (0 or 1) from cached phases
        return cachedPhases.getPhase(
                alternatingFlash ? TrafficSignalPhases.PHASE_INDEX_FAULT_1 : TrafficSignalPhases.PHASE_INDEX_FAULT_2 );
    }
}
