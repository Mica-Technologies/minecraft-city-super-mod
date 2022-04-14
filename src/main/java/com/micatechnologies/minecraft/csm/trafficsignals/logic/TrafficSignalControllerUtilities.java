package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.trafficsignals.AbstractBlockControllableSignal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TrafficSignalControllerUtilities
{
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
