package com.micatechnologies.minecraft.csm.trafficmgmt.prog;

import com.micatechnologies.minecraft.csm.trafficmgmt.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficmgmt.TrafficMgmtConstants;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Java implementation of a traffic signal phase, formed by lists populated with signals in each possible state (green,
 * yellow, red, off) that can be applied by calling {@link #applyPhaseToSignals(World)}.
 *
 * @author Mica Technologies/ah
 * @since 2020.8
 */
public class SignalPhase

{
    /**
     * List of signals that are in the green state during the phase.
     */
    public final List< BlockPos > greenSignals;

    /**
     * List of signals that are in the yellow state during the phase.
     */
    public final List< BlockPos > yellowSignals;

    /**
     * List of signals that are in the red state during the phase.
     */
    public final List< BlockPos > redSignals;

    /**
     * List of signals that are in the off state during the phase.
     */
    public final List< BlockPos > offSignals;

    /**
     * Constructs a traffic signal phase with lists of signals for each possible state (red, yellow, green, off).
     *
     * @param greenSignals  list of signals in green state
     * @param yellowSignals list of signals in yellow state
     * @param redSignals    list of signals in red state
     * @param offSignals    list of signals in off state
     */
    public SignalPhase( List< BlockPos > greenSignals,
                        List< BlockPos > yellowSignals,
                        List< BlockPos > redSignals,
                        List< BlockPos > offSignals )
    {
        this.greenSignals = greenSignals;
        this.yellowSignals = yellowSignals;
        this.redSignals = redSignals;
        this.offSignals = offSignals;
    }

    /**
     * Applies the correct state to each signal in the phase.
     */
    public void applyPhaseToSignals( World world ) {
        // Apply to off signals
        for ( BlockPos signalPos : offSignals ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                   TrafficMgmtConstants.SIGNAL_STATE_OFF );
            }
        }

        // Apply to red signals
        for ( BlockPos signalPos : redSignals ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                   TrafficMgmtConstants.SIGNAL_STATE_RED );
            }
        }

        // Apply to yellow signals
        for ( BlockPos signalPos : yellowSignals ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                   TrafficMgmtConstants.SIGNAL_STATE_YELLOW );
            }
        }

        // Apply to green signals
        for ( BlockPos signalPos : greenSignals ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                   TrafficMgmtConstants.SIGNAL_STATE_GREEN );
            }
        }
    }
}
