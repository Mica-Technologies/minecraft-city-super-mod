package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.trafficsignals.AbstractBlockControllableSignal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Utility methods for the traffic signal controller block
 * ({@link com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficSignalController}) to update and apply phases
 * or change circuits, etc.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2022.1.0
 */
public class TrafficSignalControllerUtilities
{
    /**
     * Gets the next {@link TrafficSignalPhase} based on the given {@link TrafficSignalControllerCircuits} and power
     * information (on/off).
     *
     * @param currentPhase The current {@link TrafficSignalPhase} of the traffic signal controller.
     * @param circuits     The {@link TrafficSignalControllerCircuits} for the traffic signal controller.
     * @param isPowered    The power state of the traffic signal controller (true: powered, false: unpowered).
     *
     * @return The next {@link TrafficSignalPhase} based on the given {@link TrafficSignalControllerCircuits} and power
     *         information (on/off).
     *
     * @since 1.0
     */
    public static TrafficSignalPhase getNextPhaseForCircuits( TrafficSignalPhase currentPhase,
                                                              TrafficSignalControllerCircuits circuits,
                                                              boolean isPowered )
    {
        // Create next phase object
        TrafficSignalPhase nextPhase = currentPhase;

        // Check for controller power and build next phase
        if ( isPowered ) {
            nextPhase = TrafficSignalPhaseBuilder.buildPowerOffPhase( circuits );
        }
        else {
            // Switch to power off phase if not already in it
            if ( currentPhase.getApplicability() != TrafficSignalPhaseApplicability.NO_POWER ) {
                nextPhase = TrafficSignalPhaseBuilder.buildPowerOffPhase( circuits );
            }
        }

        // Return next phase
        return nextPhase;
    }

    /**
     * Applies the specified {@link TrafficSignalPhase} to the applicable signals by updating the state (off, green,
     * yellow, red) of the signals in the corresponding lists.
     *
     * @param phase the {@link TrafficSignalPhase} to apply
     * @param world the {@link World} to apply the phase in
     *
     * @since 1.0
     */
    public static void applyPhase( TrafficSignalPhase phase, World world ) {
        // Apply all off signals
        for ( BlockPos signalPos : phase.getOffSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                   AbstractBlockControllableSignal.SIGNAL_OFF );
            }
        }

        // Apply all green signals
        for ( BlockPos signalPos : phase.getGreenSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                   AbstractBlockControllableSignal.SIGNAL_GREEN );
            }
        }

        // Apply all yellow signals
        for ( BlockPos signalPos : phase.getYellowSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                   AbstractBlockControllableSignal.SIGNAL_YELLOW );
            }
        }

        // Apply all red signals
        for ( BlockPos signalPos : phase.getRedSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                   AbstractBlockControllableSignal.SIGNAL_RED );
            }
        }
    }
}
