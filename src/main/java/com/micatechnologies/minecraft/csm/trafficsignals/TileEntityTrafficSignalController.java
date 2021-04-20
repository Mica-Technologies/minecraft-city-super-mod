package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalCircuit;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ElementsCitySuperMod.ModElement.Tag
public class TileEntityTrafficSignalController extends TileEntity
{
    ///region: New Logic
    private static final String                       SERIALIZED_SIGNAL_CIRCUIT_LIST_KEY
                                                                                               = "SerializedSignalCircuitList";
    private static final String                       SERIALIZED_SIGNAL_CIRCUIT_LIST_SEPARATOR = ":";
    private final        List< TrafficSignalCircuit > signalCircuitList                        = new ArrayList<>();

    /**
     * Former key for N/S signal list. Used to upgrade previous versions to the new logic.
     */
    private static final String KEY_NS_SIGNALS = "NSSigs";

    /**
     * Former key for E/W signal list. Used to upgrade previous versions to the new logic.
     */
    private static final String KEY_EW_SIGNALS = "EWSigs";

    private void importPreviousListFormat( String listString ) {
        // Create new signal circuit
        TrafficSignalCircuit newCircuit = new TrafficSignalCircuit();

        // Split list by items
        String[] positions = listString.split( "\n" );
        for ( String position : positions ) {
            // Split items in to coordinates
            String[] coordinates = position.split( " " );

            // If coordinates valid, parse and add to new signal circuit
            if ( coordinates.length == 3 ) {
                BlockPos signalPos = new BlockPos( Integer.parseInt( coordinates[ 0 ] ),
                                                   Integer.parseInt( coordinates[ 1 ] ),
                                                   Integer.parseInt( coordinates[ 2 ] ) );

                // Get signal type and add to appropriate list in signal circuit
                IBlockState blockState = world.getBlockState( signalPos );
                if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                    AbstractBlockControllableSignal controllableSignal
                            = ( AbstractBlockControllableSignal ) blockState.getBlock();
                    if ( controllableSignal.getSignalSide( null, null ) ==
                            AbstractBlockControllableSignal.SIGNAL_SIDE.LEFT ) {
                        newCircuit.linkLeftSignal( signalPos );
                    }
                    else if ( controllableSignal.getSignalSide( null, null ) ==
                            AbstractBlockControllableSignal.SIGNAL_SIDE.AHEAD ) {
                        newCircuit.linkAheadSignal( signalPos );
                    }
                    else if ( controllableSignal.getSignalSide( null, null ) ==
                            AbstractBlockControllableSignal.SIGNAL_SIDE.RIGHT ) {
                        newCircuit.linkRightSignal( signalPos );
                    }
                    else if ( controllableSignal.getSignalSide( null, null ) ==
                            AbstractBlockControllableSignal.SIGNAL_SIDE.CROSSWALK ) {
                        newCircuit.linkPedestrianSignal( signalPos );
                    }
                    else if ( controllableSignal.getSignalSide( null, null ) ==
                            AbstractBlockControllableSignal.SIGNAL_SIDE.PROTECTED_AHEAD ) {
                        newCircuit.linkProtectedSignal( signalPos );
                    }
                }
            }
        }

        // Add new signal circuit to list and mark dirty
        signalCircuitList.add( newCircuit );
        markDirty();
    }

    public int getSignalCircuitCount() {
        return signalCircuitList.size();
    }

    public boolean isDeviceLinked( BlockPos blockPos ) {
        for ( TrafficSignalCircuit signalCircuit : signalCircuitList ) {
            if ( signalCircuit.isLinked( blockPos ) ) {
                return true;
            }
        }
        return false;
    }

    public boolean unlinkDevice( BlockPos blockPos ) {
        boolean unlinked = false;
        for ( TrafficSignalCircuit signalCircuit : signalCircuitList ) {
            unlinked = signalCircuit.unlink( blockPos );
            if ( unlinked ) {
                break;
            }
        }
        return unlinked;
    }

    public boolean linkDevice( BlockPos blockPos, AbstractBlockControllableSignal.SIGNAL_SIDE signalSide, int circuit )
    {
        int actualCircuit = circuit - 1;
        boolean result = false;
        if ( !isDeviceLinked( blockPos ) ) {
            // Get circuit to link to, create new one if necessary
            TrafficSignalCircuit linkToCircuit;
            boolean addCircuit = false;
            if ( actualCircuit >= signalCircuitList.size() || circuit < 0 ) {
                linkToCircuit = new TrafficSignalCircuit();
                addCircuit = true;
            }
            else {
                linkToCircuit = signalCircuitList.get( actualCircuit );
            }

            // Link to correct list
            if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.LEFT ) {
                linkToCircuit.linkLeftSignal( blockPos );
            }
            else if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.AHEAD ) {
                linkToCircuit.linkAheadSignal( blockPos );
            }
            else if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.RIGHT ) {
                linkToCircuit.linkRightSignal( blockPos );
            }
            else if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.CROSSWALK ) {
                linkToCircuit.linkPedestrianSignal( blockPos );
            }
            else if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.PROTECTED_AHEAD ) {
                linkToCircuit.linkProtectedSignal( blockPos );
            }

            // Add new circuit if necessary
            if ( addCircuit ) {
                signalCircuitList.add( linkToCircuit );
            }

            result = true;
        }
        return result;
    }
    ///endregion

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

    private static final String KEY_LAST_PHASE_CHANGE_TIME   = "LastPhaseChangeTime";
    private static final String KEY_CURR_PHASE_TIME          = "CurrPhaseTime";
    private static final String KEY_CURRENT_PHASE            = "CurrPhase";
    private static final String KEY_CURRENT_CYCLE_LIST_INDEX = "CurrCycleListIndex";
    private              int    currentPhase;
    private              int    currentPhaseTime;
    private              int    currentCycleListIndex;
    private              long   lastPhaseChangeTime;

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        super.readFromNBT( p_readFromNBT_1_ );

        // Load existing serialized signals list (if present)
        if ( p_readFromNBT_1_.hasKey( SERIALIZED_SIGNAL_CIRCUIT_LIST_KEY ) ) {
            String serializedSignalCircuitList = p_readFromNBT_1_.getString( SERIALIZED_SIGNAL_CIRCUIT_LIST_KEY );
            String[] serializedSignalsLists = serializedSignalCircuitList.split(
                    SERIALIZED_SIGNAL_CIRCUIT_LIST_SEPARATOR );
            for ( String serializedSignalList : serializedSignalsLists ) {
                TrafficSignalCircuit importedCircuit = new TrafficSignalCircuit( serializedSignalList );
                signalCircuitList.add( importedCircuit );
            }
        }
        // Load previous format to new format (if present and not already upgraded)
        else if ( p_readFromNBT_1_.hasKey( KEY_NS_SIGNALS ) && p_readFromNBT_1_.hasKey( KEY_EW_SIGNALS ) ) {
            // Import previous N/S signals list
            String nsSignalsString = p_readFromNBT_1_.getString( KEY_NS_SIGNALS );
            importPreviousListFormat( nsSignalsString );
            p_readFromNBT_1_.removeTag( KEY_NS_SIGNALS );
            markDirty();

            // Import previous N/S signals list
            String ewSignalsString = p_readFromNBT_1_.getString( KEY_EW_SIGNALS );
            importPreviousListFormat( ewSignalsString );
            p_readFromNBT_1_.removeTag( KEY_EW_SIGNALS );
            markDirty();
        }

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

        // Write signal circuit list
        StringBuilder serializedSignalCircuitListStringBuilder = new StringBuilder();
        Iterator< TrafficSignalCircuit > signalCircuitIterator = signalCircuitList.iterator();
        while ( signalCircuitIterator.hasNext() ) {
            serializedSignalCircuitListStringBuilder.append( signalCircuitIterator.next().getSerialized() );
            if ( signalCircuitIterator.hasNext() ) {
                serializedSignalCircuitListStringBuilder.append( SERIALIZED_SIGNAL_CIRCUIT_LIST_SEPARATOR );
            }
        }
        p_writeToNBT_1_.setString( SERIALIZED_SIGNAL_CIRCUIT_LIST_KEY,
                                   serializedSignalCircuitListStringBuilder.toString() );

        return super.writeToNBT( p_writeToNBT_1_ );
    }

    public long getLastPhaseChangeTime() {
        return lastPhaseChangeTime;
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
        // Update N/S left Signals
        TrafficSignalCircuit nsCircuit = signalCircuitList.get( 0 );
        for ( BlockPos signalPos : nsCircuit.getLeftSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       cycle[ currentPhase ][ NS_LEFT_CYCLE_INDEX ] );
                }
            }
        }
        // Update N/S ahead Signals
        for ( BlockPos signalPos : nsCircuit.getAheadSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       cycle[ currentPhase ][ NS_AHEAD_CYCLE_INDEX ] );
                }
            }
        }
        // Update N/S right Signals
        for ( BlockPos signalPos : nsCircuit.getRightSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       cycle[ currentPhase ][ NS_RIGHT_CYCLE_INDEX ] );
                }
            }
        }
        // Update N/S pedestrian Signals
        for ( BlockPos signalPos : nsCircuit.getPedestrianSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       cycle[ currentPhase ][ NS_CROSSWALK_CYCLE_INDEX ] );
                }
            }
        }
        // Update N/S protected Signals
        for ( BlockPos signalPos : nsCircuit.getProtectedSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       cycle[ currentPhase ][ NS_PROTECTED_AHEAD_CYCLE_INDEX ] );
                }
            }
        }

        // Update E/W left Signals
        TrafficSignalCircuit ewCircuit = signalCircuitList.get( 1 );
        for ( BlockPos signalPos : ewCircuit.getLeftSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       cycle[ currentPhase ][ EW_LEFT_CYCLE_INDEX ] );
                }
            }
        }
        // Update E/W ahead Signals
        for ( BlockPos signalPos : ewCircuit.getAheadSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       cycle[ currentPhase ][ EW_AHEAD_CYCLE_INDEX ] );
                }
            }
        }
        // Update E/W right Signals
        for ( BlockPos signalPos : ewCircuit.getRightSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       cycle[ currentPhase ][ EW_RIGHT_CYCLE_INDEX ] );
                }
            }
        }
        // Update E/W pedestrian Signals
        for ( BlockPos signalPos : ewCircuit.getPedestrianSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       cycle[ currentPhase ][ EW_CROSSWALK_CYCLE_INDEX ] );
                }
            }
        }
        // Update E/W protected Signals
        for ( BlockPos signalPos : ewCircuit.getProtectedSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       cycle[ currentPhase ][ EW_PROTECTED_AHEAD_CYCLE_INDEX ] );
                }
            }
        }
    }

    public void cycleSignals( boolean powered ) {
        this.lastPhaseChangeTime = System.currentTimeMillis();

        // Debug information
        FMLCommonHandler.instance()
                        .getMinecraftServerInstance()
                        .sendMessage(
                                new TextComponentString( "CIRCUIT " + "COUNT" + ": " + getSignalCircuitCount() ) );

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
