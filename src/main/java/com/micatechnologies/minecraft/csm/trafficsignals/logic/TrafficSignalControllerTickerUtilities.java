package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import jdk.nashorn.internal.ir.Block;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

/**
 * Utility class for the {@link TrafficSignalControllerTicker} which provides core functionality to the various tick
 * methods without cluttering the class and possibly making it more difficult to interpret/understand.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2023.2.0
 */
public class TrafficSignalControllerTickerUtilities
{

    /**
     * Gets the flashing don't walk transition phase for the transition from the specified current phase to the
     * specified upcoming phase.
     *
     * @param currentPhase  The current phase.
     * @param upcomingPhase The upcoming phase.
     *
     * @return The flashing don't walk transition phase for the transition from the specified current phase to the
     *         specified upcoming phase.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase getFlashDontWalkTransitionPhaseForUpcoming( TrafficSignalPhase currentPhase,
                                                                                 TrafficSignalPhase upcomingPhase )
    {
        // Check for green walk signals in the current phase
        TrafficSignalPhase flashDontWalkTransitionPhase = null;
        if ( currentPhase.getWalkSignals().size() > 0 ) {
            // Create the flash don't walk transition phase
            flashDontWalkTransitionPhase = new TrafficSignalPhase( currentPhase.getCircuit(), upcomingPhase,
                                                                   TrafficSignalPhaseApplicability.FLASH_DONT_WALK_TRANSITIONING );

            // Copy the previous phase signals to the flash don't walk transition phase (except for walk signals)
            flashDontWalkTransitionPhase.addOffSignals( currentPhase.getOffSignals() );
            flashDontWalkTransitionPhase.addFyaSignals( currentPhase.getFyaSignals() );
            flashDontWalkTransitionPhase.addRedSignals( currentPhase.getRedSignals() );
            flashDontWalkTransitionPhase.addYellowSignals( currentPhase.getYellowSignals() );
            flashDontWalkTransitionPhase.addGreenSignals( currentPhase.getGreenSignals() );
            flashDontWalkTransitionPhase.addFlashDontWalkSignals( currentPhase.getFlashDontWalkSignals() );
            flashDontWalkTransitionPhase.addDontWalkSignals( currentPhase.getDontWalkSignals() );

            // Add the current phase walk signals to the flash don't walk transition phase
            for ( BlockPos walkSignal : currentPhase.getWalkSignals() ) {
                // Check if walk signal is still in walk state in the upcoming phase (stay in walk state)
                if ( upcomingPhase.getWalkSignals().contains( walkSignal ) ) {
                    // Add walk signal to flash don't walk transition phase (stay in walk state)
                    flashDontWalkTransitionPhase.addWalkSignal( walkSignal );
                }
                // Otherwise, walk signal is not in walk state in the upcoming phase (transition to don't walk state)
                else {
                    // Add don't walk signal to flash don't walk transition phase (transition to don't walk state)
                    flashDontWalkTransitionPhase.addFlashDontWalkSignal( walkSignal );
                }
            }
        }

        // Return resulting flash don't walk transition phase (null if none)
        return flashDontWalkTransitionPhase;
    }

    /**
     * Gets the yellow transition phase for the transition from the specified current phase to the specified upcoming
     * phase.
     *
     * @param currentPhase  The current phase.
     * @param upcomingPhase The upcoming phase.
     *
     * @return The yellow transition phase for the transition from the specified current phase to the specified upcoming
     *         phase.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase getYellowTransitionPhaseForUpcoming( TrafficSignalPhase currentPhase,
                                                                          TrafficSignalPhase upcomingPhase )
    {
        // Create the yellow transition phase
        TrafficSignalPhase yellowTransitionPhase = new TrafficSignalPhase( currentPhase.getCircuit(), upcomingPhase,
                                                                           TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING );

        // Copy the previous phase signals to the yellow transition phase (except for off/green/FYA signals)
        yellowTransitionPhase.addRedSignals( currentPhase.getRedSignals() );
        yellowTransitionPhase.addYellowSignals( currentPhase.getYellowSignals() );
        yellowTransitionPhase.addWalkSignals( currentPhase.getWalkSignals() );
        yellowTransitionPhase.addFlashDontWalkSignals( currentPhase.getFlashDontWalkSignals() );
        yellowTransitionPhase.addDontWalkSignals( currentPhase.getDontWalkSignals() );

        // Add the current phase green signals to the yellow transition phase
        for ( BlockPos greenSignal : currentPhase.getGreenSignals() ) {
            // Check if green signal is still in green state in the upcoming phase (stay in green state)
            if ( upcomingPhase.getGreenSignals().contains( greenSignal ) ) {
                yellowTransitionPhase.addGreenSignal( greenSignal );
            }
            // Otherwise, green signal is not in green state in the upcoming phase (transition to yellow state)
            else {
                // Add off signal to yellow transition phase (transition to yellow state)
                yellowTransitionPhase.addYellowSignal( greenSignal );
            }
        }

        // Add the current phase FYA signals to the yellow transition phase
        for ( BlockPos fyaSignal : currentPhase.getFyaSignals() ) {
            // Check if FYA signal is still in FYA or green state in the upcoming phase (stay in FYA state)
            if ( upcomingPhase.getFyaSignals().contains( fyaSignal ) ||
                    upcomingPhase.getGreenSignals().contains( fyaSignal ) ) {
                yellowTransitionPhase.addFyaSignal( fyaSignal );
            }
            // Otherwise, FYA signal is not in FYA or green state in the upcoming phase (transition to yellow state)
            else {
                // Add off signal to yellow transition phase (transition to yellow state)
                yellowTransitionPhase.addYellowSignal( fyaSignal );
            }
        }

        // Add the current phase off signals to the yellow transition phase
        for ( BlockPos offSignal : currentPhase.getOffSignals() ) {
            // Check if off signal is still in off state in the upcoming phase (stay in off state)
            if ( upcomingPhase.getOffSignals().contains( offSignal ) ) {
                yellowTransitionPhase.addOffSignal( offSignal );
            }
            // Otherwise, off signal is not in off state in the upcoming phase (transition to yellow state)
            else {
                // Add off signal to yellow transition phase (transition to yellow state)
                yellowTransitionPhase.addYellowSignal( offSignal );
            }
        }

        // Return resulting yellow transition phase
        return yellowTransitionPhase;
    }

    /**
     * Gets the red transition phase for the transition from the specified current phase to the specified upcoming
     * phase.
     *
     * @param currentPhase  The current phase.
     * @param upcomingPhase The upcoming phase.
     *
     * @return The red transition phase for the transition from the specified current phase to the specified upcoming
     *         phase.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase getRedTransitionPhaseForUpcoming( TrafficSignalPhase currentPhase,
                                                                       TrafficSignalPhase upcomingPhase )
    {
        // Create the red transition phase
        TrafficSignalPhase redTransitionPhase = new TrafficSignalPhase( currentPhase.getCircuit(), upcomingPhase,
                                                                        TrafficSignalPhaseApplicability.RED_TRANSITIONING );

        // Copy the previous phase signals to the red transition phase (except for yellow signals)
        redTransitionPhase.addOffSignals( currentPhase.getOffSignals() );
        redTransitionPhase.addFyaSignals( currentPhase.getFyaSignals() );
        redTransitionPhase.addRedSignals( currentPhase.getRedSignals() );
        redTransitionPhase.addGreenSignals( currentPhase.getGreenSignals() );
        redTransitionPhase.addWalkSignals( currentPhase.getWalkSignals() );
        redTransitionPhase.addFlashDontWalkSignals( currentPhase.getFlashDontWalkSignals() );
        redTransitionPhase.addDontWalkSignals( currentPhase.getDontWalkSignals() );

        // Add the current phase yellow signals to the red transition phase
        redTransitionPhase.addRedSignals( currentPhase.getYellowSignals() );

        // Return resulting red transition phase
        return redTransitionPhase;
    }

    /**
     * Gets the priority indicator of the next phase to service. The priority indicator is a tuple containing the
     * circuit number and the applicability of the next phase to service.
     *
     * @return The priority indicator of the next phase to service.
     *
     * @since 1.0
     */
    public static Tuple< Integer, TrafficSignalPhaseApplicability > getNextPhasePriorityIndicator() {
        // TODO: Implement phase priority logic
        return null;
    }

    /**
     * Gets the default phase for the specified circuit number when the traffic signal controller is operating in
     * {@link TrafficSignalControllerMode#NORMAL} mode.
     *
     * @param circuits                 The configured/connected circuits of the traffic signal controller.
     * @param circuitNumber            The circuit number to get the default phase for. This is a 1-based index.
     * @param overlapPedestrianSignals The overlap pedestrian signals setting of the traffic signal controller. This
     *                                 boolean value is used to determine if the pedestrian signals of all other
     *                                 circuits should be overlapped when servicing a circuit.
     *
     * @return The default phase for the specified circuit number when the traffic signal controller is operating in
     *         {@link TrafficSignalControllerMode#NORMAL} mode.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase getDefaultPhaseForCircuitNumber( TrafficSignalControllerCircuits circuits,
                                                                      int circuitNumber,
                                                                      boolean overlapPedestrianSignals )
    {
        // Only create a default phase if there are circuits
        TrafficSignalPhase defaultPhase = null;
        if ( circuits.getCircuitCount() > 0 ) {
            // Check if circuit has protected signals
            boolean hasProtectedSignals = circuits.getCircuit( circuitNumber - 1 ).getProtectedSignals().size() > 0;

            // Get appropriate phase applicability
            TrafficSignalPhaseApplicability phaseApplicability = TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS;
            if ( hasProtectedSignals ) {
                phaseApplicability = TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS;
            }

            // Create the default phase
            defaultPhase = new TrafficSignalPhase( circuitNumber, null, phaseApplicability );
            for ( int i = 1; i <= circuits.getCircuitCount(); i++ ) {
                TrafficSignalControllerCircuit circuit = circuits.getCircuit( i - 1 );
                if ( i == circuitNumber ) {
                    defaultPhase.addOffSignals( circuit.getFlashingLeftSignals() );
                    defaultPhase.addOffSignals( circuit.getFlashingRightSignals() );
                    defaultPhase.addRedSignals( circuit.getLeftSignals() );
                    defaultPhase.addGreenSignals( circuit.getThroughSignals() );
                    if ( hasProtectedSignals ) {
                        defaultPhase.addRedSignals( circuit.getRightSignals() );
                        defaultPhase.addGreenSignals( circuit.getProtectedSignals() );
                    }
                    else {
                        defaultPhase.addGreenSignals( circuit.getRightSignals() );
                        defaultPhase.addRedSignals( circuit.getProtectedSignals() );
                    }
                    defaultPhase.addRedSignals( circuit.getProtectedSignals() );
                    defaultPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
                    defaultPhase.addRedSignals( circuit.getPedestrianSignals() );
                    defaultPhase.addRedSignals( circuit.getPedestrianAccessorySignals() );
                }
                else {
                    defaultPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                    defaultPhase.addRedSignals( circuit.getFlashingRightSignals() );
                    defaultPhase.addRedSignals( circuit.getLeftSignals() );
                    defaultPhase.addRedSignals( circuit.getRightSignals() );
                    defaultPhase.addRedSignals( circuit.getThroughSignals() );
                    defaultPhase.addRedSignals( circuit.getProtectedSignals() );
                    defaultPhase.addRedSignals( circuit.getPedestrianBeaconSignals() );
                    if ( overlapPedestrianSignals ) {
                        defaultPhase.addGreenSignals( circuit.getPedestrianSignals() );
                        defaultPhase.addGreenSignals( circuit.getPedestrianAccessorySignals() );
                    }
                    else {
                        defaultPhase.addRedSignals( circuit.getPedestrianSignals() );
                        defaultPhase.addRedSignals( circuit.getPedestrianAccessorySignals() );
                    }
                }
            }
        }
        return defaultPhase;
    }
}
