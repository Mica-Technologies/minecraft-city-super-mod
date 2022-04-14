package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Utility class for building {@link TrafficSignalPhase} objects based on a list of
 * {@link TrafficSignalControllerCircuits} and programming values.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see TrafficSignalPhase
 * @see TrafficSignalControllerCircuits
 * @since 2022.1.0
 */
public class TrafficSignalPhaseBuilder
{
    /**
     * Builds a power off {@link TrafficSignalPhase} object for all signals, based on the given list of
     * {@link TrafficSignalControllerCircuits}.
     *
     * @param trafficSignalControllerCircuits the list of {@link TrafficSignalControllerCircuits} to include in the
     *                                        power off phase
     *
     * @return a power off {@link TrafficSignalPhase} object for all signals, based on the given list of
     *         {@link TrafficSignalControllerCircuits}
     *
     * @since 1.0
     */
    public static TrafficSignalPhase buildPowerOffPhase( TrafficSignalControllerCircuits trafficSignalControllerCircuits )
    {
        // Create a new TrafficSignalPhase object
        TrafficSignalPhase phase = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, 0, 0,
                                                           TrafficSignalPhaseApplicability.NO_POWER );

        // Add all lights in all circuits to off signals in phase
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Add all flashing left lights to off signals in phase
            phase.addOffSignals( circuit.getFlashingLeftSignals() );

            // Add all flashing right lights to off signals in phase
            phase.addOffSignals( circuit.getFlashingRightSignals() );

            // Add all left lights to off signals in phase
            phase.addOffSignals( circuit.getLeftSignals() );

            // Add all right lights to off signals in phase
            phase.addOffSignals( circuit.getRightSignals() );

            // Add all through lights to off signals in phase
            phase.addOffSignals( circuit.getThroughSignals() );

            // Add all pedestrian lights to off signals in phase
            phase.addOffSignals( circuit.getPedestrianSignals() );

            // Add all protected lights to off signals in phase
            phase.addOffSignals( circuit.getProtectedSignals() );
        }

        // Return the phase
        return phase;
    }

}
