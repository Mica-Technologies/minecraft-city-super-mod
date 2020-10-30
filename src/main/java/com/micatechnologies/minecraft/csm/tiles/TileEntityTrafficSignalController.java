package com.micatechnologies.minecraft.csm.tiles;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.block.AbstractBlockControllableSignal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;

@ElementsCitySuperMod.ModElement.Tag
public class TileEntityTrafficSignalController extends TileEntity
{
    private static final int      NS_PROTECTED_AHEAD_CYCLE_INDEX = 0;
    private static final int      NS_LEFT_CYCLE_INDEX            = 1;
    private static final int      NS_AHEAD_CYCLE_INDEX           = 2;
    private static final int      NS_RIGHT_CYCLE_INDEX           = 3;
    private static final int      NS_CROSSWALK_CYCLE_INDEX       = 4;
    private static final int      EW_PROTECTED_AHEAD_CYCLE_INDEX = 5;
    private static final int      EW_LEFT_CYCLE_INDEX            = 6;
    private static final int      EW_AHEAD_CYCLE_INDEX           = 7;
    private static final int      EW_RIGHT_CYCLE_INDEX           = 8;
    private static final int      EW_CROSSWALK_CYCLE_INDEX       = 9;
    private static final int      CYCLE_LENGTH_INDEX             = 10;
    public static final  String[] CYCLE_NAMES                    = { "Flash",
                                                                     "Standard w/ Protected Left",
                                                                     "Standard w/ Protected Left " +
                                                                             "and Right (Train/Bike Lane Compatible)",
                                                                     "Standard with No Protected Turns",
                                                                     "One Car Per Green" };
    private static final int[][]  CYCLE_LIST_0                   = { { 3, 0, 3, 0, 3, 0, 3, 0, 3, 3, 1 },
                                                                     { 1, 3, 1, 3, 3, 3, 0, 3, 0, 3, 1 } };

    private static final int[][] CYCLE_LIST_1 = { { 3, 0, 0, 0, 0, 3, 0, 0, 2, 0, 5 },
                                                  { 3, 2, 0, 0, 0, 3, 0, 0, 2, 0, 10 },
                                                  { 3, 1, 0, 0, 0, 3, 0, 0, 1, 0, 5 },
                                                  { 3, 0, 0, 0, 0, 3, 0, 0, 0, 1, 5 },
                                                  { 3, 0, 2, 2, 0, 3, 0, 0, 0, 1, 14 },
                                                  { 3, 0, 1, 2, 0, 3, 0, 0, 0, 0, 5 },
                                                  { 3, 0, 0, 2, 0, 3, 0, 0, 0, 0, 5 },
                                                  { 3, 0, 0, 2, 0, 3, 2, 0, 0, 0, 10 },
                                                  { 3, 0, 0, 1, 0, 3, 1, 0, 0, 0, 5 },
                                                  { 3, 0, 0, 0, 1, 3, 0, 0, 0, 0, 5 },
                                                  { 3, 0, 0, 0, 1, 3, 0, 2, 2, 0, 14 },
                                                  { 3, 0, 0, 0, 0, 3, 0, 1, 2, 0, 5 } };

    private static final int[][] CYCLE_LIST_2 = { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 },
                                                  { 0, 2, 0, 0, 0, 0, 0, 0, 2, 0, 10 },
                                                  { 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 5 },
                                                  { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 },
                                                  { 2, 0, 2, 0, 0, 0, 0, 0, 0, 1, 14 },
                                                  { 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 5 },
                                                  { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 },
                                                  { 0, 0, 0, 2, 0, 0, 2, 0, 0, 0, 10 },
                                                  { 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 5 },
                                                  { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 },
                                                  { 0, 0, 0, 0, 1, 2, 0, 2, 0, 0, 14 },
                                                  { 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 5 } };

    private static final int[][] CYCLE_LIST_3         = { { 3, 0, 0, 0, 0, 3, 0, 0, 0, 1, 5 },
                                                          { 3, 2, 2, 2, 0, 3, 0, 0, 0, 1, 14 },
                                                          { 3, 1, 1, 1, 0, 3, 0, 0, 0, 0, 5 },
                                                          { 3, 0, 0, 0, 1, 3, 0, 0, 0, 0, 5 },
                                                          { 3, 0, 0, 0, 1, 3, 2, 2, 2, 0, 14 },
                                                          { 3, 0, 0, 0, 0, 3, 1, 1, 1, 0, 5 } };
    private static final int[][] CYCLE_LIST_4         = { { 2, 2, 2, 2, 2, 0, 0, 0, 0, 2, 3 },
                                                          { 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, 3 },
                                                          { 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 3 },
                                                          { 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, 3 } };
    private static final int     CYCLE_LIST_INDEX_MIN = 0;
    private static final int     CYCLE_LIST_INDEX_MAX = 4;

    private static final String                KEY_LAST_PHASE_CHANGE_TIME   = "LastPhaseChangeTime";
    private static final String                KEY_CURR_PHASE_TIME          = "CurrPhaseTime";
    private static final String                KEY_CURRENT_PHASE            = "CurrPhase";
    private static final String                KEY_NS_SIGNALS               = "NSSigs";
    private static final String                KEY_EW_SIGNALS               = "EWSigs";
    private static final String                KEY_CURRENT_CYCLE_LIST_INDEX = "CurrCycleListIndex";
    private              int                   currentPhase;
    private              int                   currentPhaseTime;
    private              int                   currentCycleListIndex;
    private              long                  lastPhaseChangeTime;
    private              ArrayList< BlockPos > northSouthSignals            = new ArrayList<>();
    private              ArrayList< BlockPos > eastWestSignals              = new ArrayList<>();

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        super.readFromNBT( p_readFromNBT_1_ );

        // Load current cycle list index
        if ( p_readFromNBT_1_.hasKey( KEY_CURRENT_CYCLE_LIST_INDEX ) ) {
            this.currentCycleListIndex = p_readFromNBT_1_.getInteger( KEY_CURRENT_CYCLE_LIST_INDEX );
        }
        else {
            this.currentCycleListIndex = 0;
        }

        // Load current phase
        if ( p_readFromNBT_1_.hasKey( KEY_CURRENT_PHASE ) ) {
            this.currentPhase = p_readFromNBT_1_.getInteger( KEY_CURRENT_PHASE );
        }
        else {
            this.currentPhase = 0;
        }

        // Load current phase time
        if ( p_readFromNBT_1_.hasKey( KEY_CURR_PHASE_TIME ) ) {
            this.currentPhaseTime = p_readFromNBT_1_.getInteger( KEY_CURR_PHASE_TIME );
        }
        else {
            this.currentPhaseTime = 0;
        }

        // Load last phase change time
        if ( p_readFromNBT_1_.hasKey( KEY_LAST_PHASE_CHANGE_TIME ) ) {
            this.lastPhaseChangeTime = p_readFromNBT_1_.getLong( KEY_LAST_PHASE_CHANGE_TIME );
        }
        else {
            this.lastPhaseChangeTime = System.currentTimeMillis();
        }

        // Load primary signals
        northSouthSignals.clear();
        if ( p_readFromNBT_1_.hasKey( KEY_NS_SIGNALS ) ) {
            // Split into each block position
            String[] positions = p_readFromNBT_1_.getString( KEY_NS_SIGNALS ).split( "\n" );
            for ( String position : positions ) {
                String[] coordinates = position.split( " " );
                if ( coordinates.length == 3 ) {
                    northSouthSignals.add(
                            new BlockPos( Integer.parseInt( coordinates[ 0 ] ), Integer.parseInt( coordinates[ 1 ] ),
                                          Integer.parseInt( coordinates[ 2 ] ) ) );
                }
            }
        }

        // Load secondary signals
        eastWestSignals.clear();
        if ( p_readFromNBT_1_.hasKey( KEY_EW_SIGNALS ) ) {
            // Split into each block position
            String[] positions = p_readFromNBT_1_.getString( KEY_EW_SIGNALS ).split( "\n" );
            for ( String position : positions ) {
                String[] coordinates = position.split( " " );
                if ( coordinates.length == 3 ) {
                    eastWestSignals.add(
                            new BlockPos( Integer.parseInt( coordinates[ 0 ] ), Integer.parseInt( coordinates[ 1 ] ),
                                          Integer.parseInt( coordinates[ 2 ] ) ) );
                }
            }
        }
    }

    @Override
    public boolean shouldRefresh( World p_shouldRefresh_1_,
                                  BlockPos p_shouldRefresh_2_,
                                  IBlockState p_shouldRefresh_3_,
                                  IBlockState p_shouldRefresh_4_ )
    {
        return false;
    }

    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound p_writeToNBT_1_ ) {
        // Write current cycle list index
        p_writeToNBT_1_.setInteger( KEY_CURRENT_CYCLE_LIST_INDEX, currentCycleListIndex );

        // Write current phase
        p_writeToNBT_1_.setInteger( KEY_CURRENT_PHASE, currentPhase );

        // Write current phase time
        p_writeToNBT_1_.setInteger( KEY_CURR_PHASE_TIME, currentPhaseTime );

        // Write last phase change time
        p_writeToNBT_1_.setLong( KEY_LAST_PHASE_CHANGE_TIME, lastPhaseChangeTime );

        // Write primary signals
        StringBuilder northSouthSignalsString = new StringBuilder();
        for ( BlockPos bp : northSouthSignals ) {
            northSouthSignalsString.append( bp.getX() )
                                   .append( " " )
                                   .append( bp.getY() )
                                   .append( " " )
                                   .append( bp.getZ() )
                                   .append( "\n" );
        }
        p_writeToNBT_1_.setString( KEY_NS_SIGNALS, northSouthSignalsString.toString() );

        // Write secondary signals
        StringBuilder eastWestSignalsString = new StringBuilder();
        for ( BlockPos bp : eastWestSignals ) {
            eastWestSignalsString.append( bp.getX() )
                                 .append( " " )
                                 .append( bp.getY() )
                                 .append( " " )
                                 .append( bp.getZ() )
                                 .append( "\n" );
        }
        p_writeToNBT_1_.setString( KEY_EW_SIGNALS, eastWestSignalsString.toString() );

        return super.writeToNBT( p_writeToNBT_1_ );
    }

    public long getLastPhaseChangeTime() {
        return lastPhaseChangeTime;
    }

    public boolean addNSSignal( BlockPos blockPos ) {
        if ( !northSouthSignals.contains( blockPos ) && !eastWestSignals.contains( blockPos ) ) {
            northSouthSignals.add( blockPos );
            markDirty();
            return true;
        }
        return false;
    }

    public boolean addEWSignal( BlockPos blockPos ) {
        if ( !northSouthSignals.contains( blockPos ) && !eastWestSignals.contains( blockPos ) ) {
            eastWestSignals.add( blockPos );
            markDirty();
            return true;
        }
        return false;
    }

    public int getCycleTickRate() {
        return currentCycleListIndex == 0 ? 4 : 20;
    }

    public String incrementCycleIndex() {
        currentCycleListIndex++;
        if ( currentCycleListIndex > CYCLE_LIST_INDEX_MAX ) {
            currentCycleListIndex = CYCLE_LIST_INDEX_MIN;
        }
        markDirty();
        return CYCLE_NAMES[ currentCycleListIndex ];
    }

    private boolean lastPowered = false;

    public void updateSignals( int[][] cycle, boolean powered ) {
        // Update N/S Signals
        for ( BlockPos nsBlockPos : northSouthSignals ) {
            IBlockState blockState = world.getBlockState( nsBlockPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                AbstractBlockControllableSignal controllableSignal
                        = ( AbstractBlockControllableSignal ) blockState.getBlock();

                if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.LEFT ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, nsBlockPos,
                                                                       cycle[ currentPhase ][ NS_LEFT_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.AHEAD ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, nsBlockPos,
                                                                       cycle[ currentPhase ][ NS_AHEAD_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.RIGHT ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, nsBlockPos,
                                                                       cycle[ currentPhase ][ NS_RIGHT_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.CROSSWALK ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, nsBlockPos,
                                                                       cycle[ currentPhase ][ NS_CROSSWALK_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() ==
                                AbstractBlockControllableSignal.SIGNAL_SIDE.PROTECTED_AHEAD ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, nsBlockPos,
                                                                       cycle[ currentPhase ][ NS_PROTECTED_AHEAD_CYCLE_INDEX ] );
                }
                else {
                    AbstractBlockControllableSignal.changeSignalColor( world, nsBlockPos, 3 );
                }
            }

        }

        // Update E/W Signals
        for ( BlockPos ewBlockPos : eastWestSignals ) {
            IBlockState blockState = world.getBlockState( ewBlockPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                AbstractBlockControllableSignal controllableSignal
                        = ( AbstractBlockControllableSignal ) blockState.getBlock();

                if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.LEFT ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, ewBlockPos,
                                                                       cycle[ currentPhase ][ EW_LEFT_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.AHEAD ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, ewBlockPos,
                                                                       cycle[ currentPhase ][ EW_AHEAD_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.RIGHT ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, ewBlockPos,
                                                                       cycle[ currentPhase ][ EW_RIGHT_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.CROSSWALK ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, ewBlockPos,
                                                                       cycle[ currentPhase ][ EW_CROSSWALK_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() ==
                                AbstractBlockControllableSignal.SIGNAL_SIDE.PROTECTED_AHEAD ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, ewBlockPos,
                                                                       cycle[ currentPhase ][ EW_PROTECTED_AHEAD_CYCLE_INDEX ] );
                }
                else {
                    AbstractBlockControllableSignal.changeSignalColor( world, ewBlockPos, 3 );
                }
            }

        }
    }

    public void cycleSignals( boolean powered ) {
        this.lastPhaseChangeTime = System.currentTimeMillis();

        // Get cycle
        int[][] currCycleArr;
        if ( currentCycleListIndex == 1 ) {
            currCycleArr = CYCLE_LIST_1;
        }
        else if ( currentCycleListIndex == 2 ) {
            currCycleArr = CYCLE_LIST_2;
        }
        else if ( currentCycleListIndex == 3 ) {
            currCycleArr = CYCLE_LIST_3;
        }
        else if ( currentCycleListIndex == 4 ) {
            currCycleArr = CYCLE_LIST_4;
        }
        else {
            currCycleArr = CYCLE_LIST_0;
        }

        // Reset current phase if out of bounds
        boolean phaseChanged = false;
        if ( currentPhase >= currCycleArr.length ) {
            currentPhase = 0;
            currentPhaseTime = 0;
            phaseChanged = true;
        }

        // Check for power change
        if ( lastPowered != powered ) {
            phaseChanged = true;
            lastPowered = powered;
        }

        // Get phase
        if ( currentPhaseTime++ > currCycleArr[ currentPhase ][ CYCLE_LENGTH_INDEX ] ) {
            currentPhase++;
            currentPhaseTime = 0;
            phaseChanged = true;
        }
        if ( currentPhase >= currCycleArr.length ) {
            currentPhase = 0;
            currentPhaseTime = 0;
            phaseChanged = true;
        }

        // Update signals if phase changed
        if ( phaseChanged ) {
            updateSignals( currCycleArr, powered );
        }
    }
}
