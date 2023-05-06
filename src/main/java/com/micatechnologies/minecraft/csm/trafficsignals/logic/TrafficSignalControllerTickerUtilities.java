package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import jdk.nashorn.internal.ir.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
            TrafficSignalPhase tempFlashDontWalkTransitionPhase = new TrafficSignalPhase( currentPhase.getCircuit(),
                                                                                          upcomingPhase,
                                                                                          TrafficSignalPhaseApplicability.FLASH_DONT_WALK_TRANSITIONING );

            // Add the current phase walk signals to the flash don't walk transition phase
            boolean flashDontWalkSignalsAdded = false;
            for ( BlockPos walkSignal : currentPhase.getWalkSignals() ) {
                // Check if walk signal is still in walk state in the upcoming phase (stay in walk state)
                if ( upcomingPhase.getWalkSignals().contains( walkSignal ) ) {
                    // Add walk signal to flash don't walk transition phase (stay in walk state)
                    tempFlashDontWalkTransitionPhase.addWalkSignal( walkSignal );
                }
                // Otherwise, walk signal is not in walk state in the upcoming phase (transition to don't walk state)
                else {
                    // Add don't walk signal to flash don't walk transition phase (transition to don't walk state)
                    tempFlashDontWalkTransitionPhase.addFlashDontWalkSignal( walkSignal );
                    flashDontWalkSignalsAdded = true;
                }
            }

            // Check if flash don't walk signals were added to the flash don't walk transition phase
            if ( flashDontWalkSignalsAdded ) {
                // Build the rest of the phase (no need to do before and waste tick time otherwise)
                // Copy the previous phase signals to the flash don't walk transition phase (except for walk signals)
                tempFlashDontWalkTransitionPhase.addOffSignals( currentPhase.getOffSignals() );
                tempFlashDontWalkTransitionPhase.addFyaSignals( currentPhase.getFyaSignals() );
                tempFlashDontWalkTransitionPhase.addRedSignals( currentPhase.getRedSignals() );
                tempFlashDontWalkTransitionPhase.addYellowSignals( currentPhase.getYellowSignals() );
                tempFlashDontWalkTransitionPhase.addGreenSignals( currentPhase.getGreenSignals() );
                tempFlashDontWalkTransitionPhase.addFlashDontWalkSignals( currentPhase.getFlashDontWalkSignals() );
                tempFlashDontWalkTransitionPhase.addDontWalkSignals( currentPhase.getDontWalkSignals() );

                // Set the flash don't walk transition phase
                flashDontWalkTransitionPhase = tempFlashDontWalkTransitionPhase;
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
        yellowTransitionPhase.addDontWalkSignals( currentPhase.getFlashDontWalkSignals() );
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
                    upcomingPhase.getOffSignals().contains( fyaSignal ) ) {
                // TODO: Take notice of behavior here
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
                    defaultPhase.addFyaSignals( circuit.getFlashingLeftSignals() );
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
                    defaultPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                    defaultPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
                }
                else {
                    addCircuitToPhaseAllRed( circuit, defaultPhase, overlapPedestrianSignals );
                }
            }
        }
        return defaultPhase;
    }

    /**
     * Utility method to add all signals from the specified {@link TrafficSignalControllerCircuit} to their respective
     * red states in the specified {@link TrafficSignalPhase}.
     *
     * @param circuit               The circuit to add to the specified phase.
     * @param destinationPhase      The phase to add the specified circuit to.
     * @param pedestrianSignalsWalk The boolean indicating if the circuit's pedestrian signals should be set to walk or
     *                              don't walk.
     *
     * @since 1.0
     */
    public static void addCircuitToPhaseAllRed( TrafficSignalControllerCircuit circuit,
                                                TrafficSignalPhase destinationPhase,
                                                boolean pedestrianSignalsWalk )
    {
        destinationPhase.addRedSignals( circuit.getFlashingLeftSignals() );
        destinationPhase.addRedSignals( circuit.getFlashingRightSignals() );
        destinationPhase.addRedSignals( circuit.getLeftSignals() );
        destinationPhase.addRedSignals( circuit.getRightSignals() );
        destinationPhase.addRedSignals( circuit.getThroughSignals() );
        destinationPhase.addRedSignals( circuit.getProtectedSignals() );
        destinationPhase.addRedSignals( circuit.getPedestrianBeaconSignals() );
        if ( pedestrianSignalsWalk ) {
            destinationPhase.addWalkSignals( circuit.getPedestrianSignals() );
            destinationPhase.addWalkSignals( circuit.getPedestrianAccessorySignals() );
        }
        else {
            destinationPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
            destinationPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
        }
    }

    /**
     * Gets the priority indicator of the upcoming phase to service. The priority indicator is a tuple containing the
     * circuit number and the applicability of the upcoming phase to service.
     *
     * @param world    The world where the traffic signal controller and devices are  located.
     * @param circuits The configured/connected circuits of the traffic signal controller.
     *
     * @return The priority indicator of the upcoming phase to service.
     *
     * @since 1.0
     */
    public static Tuple< Integer, TrafficSignalPhaseApplicability > getUpcomingPhasePriorityIndicator( World world,
                                                                                                       TrafficSignalControllerCircuits circuits )
    {
        // Create variables to track the highest priority phase
        int highestPriorityCircuitNumber = Integer.MIN_VALUE;
        TrafficSignalPhaseApplicability highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.NONE;
        int highestPriorityWaitingCount = 0;

        // Loop through all circuits
        for ( int i = 1; i <= circuits.getCircuitCount(); i++ ) {
            // Get the circuit
            TrafficSignalControllerCircuit circuit = circuits.getCircuit( i - 1 );

            // Get sensor summary for circuit
            TrafficSignalSensorSummary sensorSummary = circuit.getSensorsWaitingSummary( world );

            // Check circuit pedestrian request count for highest priority
            int pedestrianAccessoriesRequestCount = circuit.getPedestrianAccessoriesRequestCount( world );
            if ( pedestrianAccessoriesRequestCount > highestPriorityWaitingCount ) {
                highestPriorityCircuitNumber = -1;
                highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.PEDESTRIAN;
                highestPriorityWaitingCount = pedestrianAccessoriesRequestCount;
            }

            // Check circuit all left turn lanes detection count for highest priority
            int allLeftTurnLanesDetectionCount = sensorSummary.getLeftTotal();
            if ( allLeftTurnLanesDetectionCount > highestPriorityWaitingCount ) {
                highestPriorityCircuitNumber = i;
                highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_LEFTS;
                highestPriorityWaitingCount = allLeftTurnLanesDetectionCount;
            }

            // Check circuit east facing sensors detection count for highest priority
            int eastFacingDetectionCount = sensorSummary.getNonProtectedTotalEast();
            if ( eastFacingDetectionCount > highestPriorityWaitingCount ) {
                highestPriorityCircuitNumber = i;
                highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_EAST;
                highestPriorityWaitingCount = eastFacingDetectionCount;
            }

            // Check circuit west facing sensors detection count for highest priority
            int westFacingDetectionCount = sensorSummary.getNonProtectedTotalWest();
            if ( westFacingDetectionCount > highestPriorityWaitingCount ) {
                highestPriorityCircuitNumber = i;
                highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_WEST;
                highestPriorityWaitingCount = westFacingDetectionCount;
            }

            // Check circuit north facing sensors detection count for highest priority
            int northFacingDetectionCount = sensorSummary.getNonProtectedTotalNorth();
            if ( northFacingDetectionCount > highestPriorityWaitingCount ) {
                highestPriorityCircuitNumber = i;
                highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_NORTH;
                highestPriorityWaitingCount = northFacingDetectionCount;
            }

            // Check circuit south facing sensors detection count for highest priority
            int southFacingDetectionCount = sensorSummary.getNonProtectedTotalSouth();
            if ( southFacingDetectionCount > highestPriorityWaitingCount ) {
                highestPriorityCircuitNumber = i;
                highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_SOUTH;
                highestPriorityWaitingCount = southFacingDetectionCount;
            }

            // Check circuit through/protecteds detection count for highest priority
            int throughsDetectionCount = sensorSummary.getStandardTotal();
            int throughsProtectedsDetectionCount = throughsDetectionCount + sensorSummary.getProtectedTotal();
            if ( throughsDetectionCount > highestPriorityWaitingCount ) {
                highestPriorityCircuitNumber = i;
                if ( throughsProtectedsDetectionCount > throughsDetectionCount ) {
                    highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS;
                    highestPriorityWaitingCount = throughsProtectedsDetectionCount;
                }
                else {
                    highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS;
                    highestPriorityWaitingCount = throughsDetectionCount;
                }
            }
        }

        // Return the highest priority phase (or null if none)
        if ( highestPriorityCircuitNumber == Integer.MIN_VALUE ) {
            return null;
        }
        else {
            return new Tuple<>( highestPriorityCircuitNumber, highestPriorityPhaseApplicability );
        }
    }

    /**
     * Gets the upcoming {@link TrafficSignalPhase} to service based on the specified priority indicator.
     *
     * @param world                    The world where the traffic signal controller and devices are  located.
     * @param circuits                 The configured/connected circuits of the traffic signal controller.
     * @param priorityIndicator        The priority indicator of the upcoming phase to service.
     * @param overlapPedestrianSignals The overlap pedestrian signals setting of the traffic signal controller. This
     *                                 boolean value is used to determine if the pedestrian signals of all other
     *                                 circuits should be overlapped when servicing a circuit.
     *
     * @return The upcoming {@link TrafficSignalPhase} to service based on the specified priority indicator.
     *
     * @since 1.0
     */
    public static TrafficSignalPhase getUpcomingPhaseForPriorityIndicator( World world,
                                                                           TrafficSignalControllerCircuits circuits,
                                                                           Tuple< Integer, TrafficSignalPhaseApplicability > priorityIndicator,
                                                                           boolean overlapPedestrianSignals )
    {
        // Get the circuit number and phase applicability from the priority indicator
        int circuitNumber = priorityIndicator.getFirst();
        TrafficSignalPhaseApplicability phaseApplicability = priorityIndicator.getSecond();

        // Create the upcoming phase object
        TrafficSignalPhase upcomingPhase = new TrafficSignalPhase( circuitNumber, phaseApplicability );

        // Loop through all circuits
        for ( int i = 1; i <= circuits.getCircuitCount(); i++ ) {
            // Get the circuit
            TrafficSignalControllerCircuit circuit = circuits.getCircuit( i - 1 );

            // Handle dedicated pedestrian phase applicability
            if ( phaseApplicability == TrafficSignalPhaseApplicability.PEDESTRIAN ) {
                boolean pedestrianSignalsWalk = true;
                addCircuitToPhaseAllRed( circuit, upcomingPhase, pedestrianSignalsWalk );
            }
            // Handle all left turn lanes phase applicability
            else if ( phaseApplicability == TrafficSignalPhaseApplicability.ALL_LEFTS ) {
                if ( i == circuitNumber ) {
                    upcomingPhase.addOffSignals( circuit.getFlashingLeftSignals() );
                    upcomingPhase.addRedSignals( circuit.getFlashingRightSignals() );
                    upcomingPhase.addGreenSignals( circuit.getLeftSignals() );
                    upcomingPhase.addRedSignals( circuit.getRightSignals() );
                    upcomingPhase.addRedSignals( circuit.getThroughSignals() );
                    upcomingPhase.addRedSignals( circuit.getProtectedSignals() );
                    upcomingPhase.addRedSignals( circuit.getPedestrianBeaconSignals() );
                    upcomingPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                    upcomingPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
                }
                else {
                    boolean pedestrianSignalsWalk = false;
                    addCircuitToPhaseAllRed( circuit, upcomingPhase, pedestrianSignalsWalk );
                }
            }
            // Handle all east facing lanes phase applicability
            else if ( phaseApplicability == TrafficSignalPhaseApplicability.ALL_EAST ) {
                if ( i == circuitNumber ) {
                    addActiveCircuitToDirectionalGreenPhase( world, circuit, upcomingPhase, EnumFacing.EAST );
                }
                else {
                    boolean pedestrianSignalsWalk = false;
                    addCircuitToPhaseAllRed( circuit, upcomingPhase, pedestrianSignalsWalk );
                }
            }
            // Handle all west facing lanes phase applicability
            else if ( phaseApplicability == TrafficSignalPhaseApplicability.ALL_WEST ) {
                if ( i == circuitNumber ) {
                    addActiveCircuitToDirectionalGreenPhase( world, circuit, upcomingPhase, EnumFacing.WEST );
                }
                else {
                    boolean pedestrianSignalsWalk = false;
                    addCircuitToPhaseAllRed( circuit, upcomingPhase, pedestrianSignalsWalk );
                }
            }
            // Handle all north facing lanes phase applicability
            else if ( phaseApplicability == TrafficSignalPhaseApplicability.ALL_NORTH ) {
                if ( i == circuitNumber ) {
                    addActiveCircuitToDirectionalGreenPhase( world, circuit, upcomingPhase, EnumFacing.NORTH );
                }
                else {
                    boolean pedestrianSignalsWalk = false;
                    addCircuitToPhaseAllRed( circuit, upcomingPhase, pedestrianSignalsWalk );
                }
            }
            // Handle all south facing lanes phase applicability
            else if ( phaseApplicability == TrafficSignalPhaseApplicability.ALL_SOUTH ) {
                if ( i == circuitNumber ) {
                    addActiveCircuitToDirectionalGreenPhase( world, circuit, upcomingPhase, EnumFacing.SOUTH );
                }
                else {
                    boolean pedestrianSignalsWalk = false;
                    addCircuitToPhaseAllRed( circuit, upcomingPhase, pedestrianSignalsWalk );
                }
            }
            // Handle all throughs and rights phase applicability
            else if ( phaseApplicability == TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS ) {
                if ( i == circuitNumber ) {
                    upcomingPhase.addFyaSignals( circuit.getFlashingLeftSignals() );
                    upcomingPhase.addRedSignals( circuit.getLeftSignals() );
                    upcomingPhase.addGreenSignals( circuit.getThroughSignals() );
                    upcomingPhase.addRedSignals( circuit.getProtectedSignals() );
                    upcomingPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
                    upcomingPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                    upcomingPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
                    if ( overlapPedestrianSignals ) {
                        upcomingPhase.addFyaSignals( circuit.getFlashingRightSignals() );
                        upcomingPhase.addRedSignals( circuit.getRightSignals() );
                    }
                    else {
                        upcomingPhase.addOffSignals( circuit.getFlashingRightSignals() );
                        upcomingPhase.addGreenSignals( circuit.getRightSignals() );
                    }
                }
                else {
                    addCircuitToPhaseAllRed( circuit, upcomingPhase, overlapPedestrianSignals );
                }
            }
            // Handle all throughs and protected rights phase applicability
            else if ( phaseApplicability == TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTED_RIGHTS ) {
                if ( i == circuitNumber ) {
                    upcomingPhase.addFyaSignals( circuit.getFlashingLeftSignals() );
                    upcomingPhase.addOffSignals( circuit.getFlashingRightSignals() );
                    upcomingPhase.addRedSignals( circuit.getLeftSignals() );
                    upcomingPhase.addGreenSignals( circuit.getRightSignals() );
                    upcomingPhase.addGreenSignals( circuit.getThroughSignals() );
                    upcomingPhase.addRedSignals( circuit.getProtectedSignals() );
                    upcomingPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
                    upcomingPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                    upcomingPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
                }
                else {
                    boolean pedestrianSignalsWalk = false;
                    addCircuitToPhaseAllRed( circuit, upcomingPhase, pedestrianSignalsWalk );
                }
            }
            // Handle all throughs and protecteds phase applicability
            else if ( phaseApplicability == TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS ) {
                if ( i == circuitNumber ) {
                    upcomingPhase.addFyaSignals( circuit.getFlashingLeftSignals() );
                    upcomingPhase.addFyaSignals( circuit.getFlashingRightSignals() );
                    upcomingPhase.addRedSignals( circuit.getLeftSignals() );
                    upcomingPhase.addRedSignals( circuit.getRightSignals() );
                    upcomingPhase.addGreenSignals( circuit.getThroughSignals() );
                    upcomingPhase.addGreenSignals( circuit.getProtectedSignals() );
                    upcomingPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
                    upcomingPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                    upcomingPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
                }
                else {
                    addCircuitToPhaseAllRed( circuit, upcomingPhase, overlapPedestrianSignals );
                }
            }
            else {
                throw new IllegalStateException( "Encountered an improper phase applicability during standard " +
                                                         "operation: " +
                                                         phaseApplicability );
            }

        }

        // Return the upcoming phase
        return upcomingPhase;
    }

    /**
     * Utility method to filter signals in the specified {@link List<BlockPos>} by their facing direction. This method
     * will return a {@link Tuple} containing two {@link List<BlockPos>}, the first containing the signals that are
     * facing the specified direction, and the second containing the signals that are not facing the specified
     * direction.
     *
     * @param world            The world where the traffic signal controller and devices are located.
     * @param signalBlockPoses The {@link List<BlockPos>} containing the signals to filter.
     * @param enumFacing       The {@link EnumFacing} to filter the signals by.
     *
     * @return A {@link Tuple} containing two {@link List<BlockPos>}, the first containing the signals that are facing
     *         the specified direction, and the second containing the signals that are not facing the specified
     *         direction.
     *
     * @since 1.0
     */
    private static Tuple< List< BlockPos >, List< BlockPos > > filterSignalsByFacingDirection( World world,
                                                                                               List< BlockPos > signalBlockPoses,
                                                                                               EnumFacing enumFacing )
    {
        // Create facing direction stream collector predicate
        Predicate< BlockPos > facingDirectionPredicate = signalPos -> {
            IBlockState blockState = world.getBlockState( signalPos );
            EnumFacing sensorFacingDirection = blockState.getValue( BlockHorizontal.FACING );
            return sensorFacingDirection == enumFacing;
        };

        // Partition the signal list by the facing direction predicate
        Map< Boolean, List< BlockPos > > filteredSignalLists = signalBlockPoses.stream()
                                                                               .collect( ( Collectors.partitioningBy(
                                                                                       facingDirectionPredicate ) ) );

        // Return the filtered signal lists
        return new Tuple<>( filteredSignalLists.get( true ), filteredSignalLists.get( false ) );
    }

    /**
     * Utility method to add all signals from the specified active {@link TrafficSignalControllerCircuit} to their
     * respective states (green or red) in the specified {@link TrafficSignalPhase} based on the specified
     * {@link EnumFacing}.
     *
     * @param world            The world where the traffic signal controller and devices are located.
     * @param circuit          The active circuit to add to the specified phase.
     * @param destinationPhase The phase to add the specified active circuit to.
     * @param enumFacing       The {@link EnumFacing} direction to apply green signals to.
     *
     * @since 1.0
     */
    public static void addActiveCircuitToDirectionalGreenPhase( World world,
                                                                TrafficSignalControllerCircuit circuit,
                                                                TrafficSignalPhase destinationPhase,
                                                                EnumFacing enumFacing )
    {
        // Get directionally filtered signal lists
        Tuple< List< BlockPos >, List< BlockPos > > flashingLeftSignals = filterSignalsByFacingDirection( world,
                                                                                                          circuit.getFlashingLeftSignals(),
                                                                                                          enumFacing );
        Tuple< List< BlockPos >, List< BlockPos > > flashingRightSignals = filterSignalsByFacingDirection( world,
                                                                                                           circuit.getFlashingRightSignals(),
                                                                                                           enumFacing );
        Tuple< List< BlockPos >, List< BlockPos > > leftSignals = filterSignalsByFacingDirection( world,
                                                                                                  circuit.getLeftSignals(),
                                                                                                  enumFacing );
        Tuple< List< BlockPos >, List< BlockPos > > rightSignals = filterSignalsByFacingDirection( world,
                                                                                                   circuit.getRightSignals(),
                                                                                                   enumFacing );
        Tuple< List< BlockPos >, List< BlockPos > > throughSignals = filterSignalsByFacingDirection( world,
                                                                                                     circuit.getThroughSignals(),
                                                                                                     enumFacing );
        Tuple< List< BlockPos >, List< BlockPos > > pedestrianBeaconSignals = filterSignalsByFacingDirection( world,
                                                                                                              circuit.getPedestrianBeaconSignals(),
                                                                                                              enumFacing );

        // Add signals to phase
        destinationPhase.addOffSignals( flashingLeftSignals.getFirst() );
        destinationPhase.addGreenSignals( leftSignals.getFirst() );
        destinationPhase.addFyaSignals( flashingLeftSignals.getSecond() );
        destinationPhase.addRedSignals( leftSignals.getSecond() );
        destinationPhase.addOffSignals( flashingRightSignals.getFirst() );
        destinationPhase.addGreenSignals( rightSignals.getFirst() );
        destinationPhase.addRedSignals( flashingRightSignals.getSecond() );
        destinationPhase.addRedSignals( rightSignals.getSecond() );
        destinationPhase.addGreenSignals( throughSignals.getFirst() );
        destinationPhase.addRedSignals( throughSignals.getSecond() );
        destinationPhase.addOffSignals( pedestrianBeaconSignals.getFirst() );
        destinationPhase.addRedSignals( pedestrianBeaconSignals.getSecond() );
        destinationPhase.addRedSignals( circuit.getProtectedSignals() );
        destinationPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
        destinationPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
    }

}
