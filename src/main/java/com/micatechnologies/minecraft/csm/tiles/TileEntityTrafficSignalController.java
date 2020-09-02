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
    private static final int     NS_LEFT_CYCLE_INDEX      = 0;
    private static final int     NS_AHEAD_CYCLE_INDEX     = 1;
    private static final int     NS_RIGHT_CYCLE_INDEX     = 2;
    private static final int     NS_CROSSWALK_CYCLE_INDEX = 3;
    private static final int     EW_LEFT_CYCLE_INDEX      = 4;
    private static final int     EW_AHEAD_CYCLE_INDEX     = 5;
    private static final int     EW_RIGHT_CYCLE_INDEX     = 6;
    private static final int     EW_CROSSWALK_CYCLE_INDEX = 7;
    private static final int     CYCLE_LENGTH_INDEX       = 8;
    private static final int[][] SIGNAL_CYCLES_LIST       = { { 0, 0, 0, 0, 0, 0, 2, 0, 3 },
                                                              { 2, 0, 0, 0, 0, 0, 2, 0, 16 },
                                                              { 1, 0, 0, 0, 0, 0, 1, 0, 5 },
                                                              { 0, 0, 0, 0, 0, 0, 0, 1, 3 },
                                                              { 0, 2, 2, 0, 0, 0, 0, 1, 24 },
                                                              { 0, 1, 2, 0, 0, 0, 0, 0, 5 },
                                                              { 0, 0, 2, 0, 0, 0, 0, 0, 3 },
                                                              { 0, 0, 2, 0, 2, 0, 0, 0, 16 },
                                                              { 0, 0, 1, 0, 1, 0, 0, 0, 5 },
                                                              { 0, 0, 0, 1, 0, 0, 0, 0, 3 },
                                                              { 0, 0, 0, 1, 0, 2, 2, 0, 24 },
                                                              { 0, 0, 0, 0, 0, 1, 2, 0, 5 } };

    private static final String                KEY_CURRENT_CYCLE_TIME = "CurrCycleTime";
    private static final String                KEY_CURRENT_CYCLE      = "CurrCycle";
    private static final String                KEY_NS_SIGNALS         = "NSSigs";
    private static final String                KEY_EW_SIGNALS         = "EWSigs";
    private              int                   currentCycle;
    private              int                   currentCycleTime;
    private              ArrayList< BlockPos > northSouthSignals      = new ArrayList<>();
    private              ArrayList< BlockPos > eastWestSignals        = new ArrayList<>();

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        super.readFromNBT( p_readFromNBT_1_ );

        // Load current cycle
        if ( p_readFromNBT_1_.hasKey( KEY_CURRENT_CYCLE ) ) {
            this.currentCycle = p_readFromNBT_1_.getInteger( KEY_CURRENT_CYCLE );
        }
        else {
            this.currentCycle = 0;
        }

        // Load current cycle time
        if ( p_readFromNBT_1_.hasKey( KEY_CURRENT_CYCLE_TIME ) ) {
            this.currentCycleTime = p_readFromNBT_1_.getInteger( KEY_CURRENT_CYCLE_TIME );
        }
        else {
            this.currentCycleTime = 0;
        }

        // Load north/south signals
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

        // Load east/west signals
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
        // Write current cycle
        p_writeToNBT_1_.setInteger( KEY_CURRENT_CYCLE, currentCycle );

        // Write current cycle time
        p_writeToNBT_1_.setInteger( KEY_CURRENT_CYCLE_TIME, currentCycleTime );

        // Write north/south signals
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

        // Write east/west signals
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

    public void cycleSignals( World world, boolean powered ) {
        // Reset current cycle if out of bounds
        if ( currentCycle >= SIGNAL_CYCLES_LIST.length ) {
            currentCycle = 0;
            currentCycleTime = 0;
        }

        // Get cycle
        if ( currentCycleTime++ > SIGNAL_CYCLES_LIST[ currentCycle ][ CYCLE_LENGTH_INDEX ] ) {
            currentCycle++;
            currentCycleTime = 0;
        }
        if ( currentCycle >= SIGNAL_CYCLES_LIST.length ) {
            currentCycle = 0;
            currentCycleTime = 0;
        }

        // Update N/S Signals
        for ( BlockPos nsBlockPos : northSouthSignals ) {
            IBlockState blockState = world.getBlockState( nsBlockPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                AbstractBlockControllableSignal controllableSignal
                        = ( AbstractBlockControllableSignal ) blockState.getBlock();

                if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.LEFT ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, nsBlockPos,
                                                                       SIGNAL_CYCLES_LIST[ currentCycle ][ NS_LEFT_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.AHEAD ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, nsBlockPos,
                                                                       SIGNAL_CYCLES_LIST[ currentCycle ][ NS_AHEAD_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.RIGHT ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, nsBlockPos,
                                                                       SIGNAL_CYCLES_LIST[ currentCycle ][ NS_RIGHT_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.CROSSWALK ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, nsBlockPos,
                                                                       SIGNAL_CYCLES_LIST[ currentCycle ][ NS_CROSSWALK_CYCLE_INDEX ] );
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
                                                                       SIGNAL_CYCLES_LIST[ currentCycle ][ EW_LEFT_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.AHEAD ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, ewBlockPos,
                                                                       SIGNAL_CYCLES_LIST[ currentCycle ][ EW_AHEAD_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.RIGHT ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, ewBlockPos,
                                                                       SIGNAL_CYCLES_LIST[ currentCycle ][ EW_RIGHT_CYCLE_INDEX ] );
                }
                else if ( powered &&
                        controllableSignal.getSignalSide() == AbstractBlockControllableSignal.SIGNAL_SIDE.CROSSWALK ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, ewBlockPos,
                                                                       SIGNAL_CYCLES_LIST[ currentCycle ][ EW_CROSSWALK_CYCLE_INDEX ] );
                }
                else {
                    AbstractBlockControllableSignal.changeSignalColor( world, ewBlockPos, 3 );
                }
            }

        }
    }
}
