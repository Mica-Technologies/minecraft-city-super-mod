package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalCircuit;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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

    private static final String                     SERIALIZED_SIGNAL_STATE_LIST_KEY       = "SerializedSignalStateList";
    private static final String                     SERIALIZED_SIGNAL_STATE_LIST_SEPARATOR = ":";
    private final        List< TrafficSignalState > signalStateList                        = new ArrayList<>();

    /**
     * Former key for N/S signal list. Used to upgrade previous versions to the new logic.
     */
    private static final String KEY_NS_SIGNALS = "NSSigs";

    /**
     * Former key for E/W signal list. Used to upgrade previous versions to the new logic.
     */
    private static final String KEY_EW_SIGNALS = "EWSigs";

    public void importPreviousConfig( World currWorld ) {
        // Load previous format to new format (if present and not already upgraded)
        if ( nsSignalsString != null && ewSignalsString != null ) {
            importPreviousListFormat( currWorld, nsSignalsString );
            importPreviousListFormat( currWorld, ewSignalsString );
            markDirty();
        }

        // Trigger state regeneration
        updateSignalStates( world );
    }

    private void importPreviousListFormat( World currWorld, String listString ) {
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
                IBlockState blockState = currWorld.getBlockState( signalPos );
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

    public boolean unlinkDevice( BlockPos blockPos, World world ) {
        boolean unlinked = false;
        for ( TrafficSignalCircuit signalCircuit : signalCircuitList ) {
            unlinked = signalCircuit.unlink( blockPos );
            if ( unlinked ) {
                updateSignalStates( world );
                break;
            }
        }
        return unlinked;
    }

    public boolean linkDevice( World world,
                               BlockPos blockPos,
                               AbstractBlockControllableSignal.SIGNAL_SIDE signalSide,
                               int circuit )
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
            else if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.HYBRID_LEFT ) {
                linkToCircuit.linkHybridLeftSignal( blockPos );
            }

            // Add new circuit if necessary
            if ( addCircuit ) {
                signalCircuitList.add( linkToCircuit );
            }

            // Trigger state regeneration
            updateSignalStates( world );

            result = true;
        }
        return result;
    }
    ///endregion

    private static final String KEY_LAST_PHASE_CHANGE_TIME = "LastPhaseChangeTime";
    private static final String KEY_CURR_PHASE_TIME        = "CurrPhaseTime";
    private static final String KEY_CURRENT_PHASE          = "CurrPhase";
    private static final String KEY_CURRENT_MODE           = "CurrentMode";
    private static final int    CURRENT_MODE_FLASH         = 0;
    private static final int    CURRENT_MODE_STANDARD      = 1;
    private static final int    CURRENT_MODE_METER         = 2;
    private              int    currentPhase;
    private              int    currentPhaseTime;
    private              long   lastPhaseChangeTime;
    private              int    currentMode;
    private              String ewSignalsString            = null;
    private              String nsSignalsString            = null;

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        super.readFromNBT( p_readFromNBT_1_ );

        // Load previous format to new format (if present)
        if ( p_readFromNBT_1_.hasKey( KEY_NS_SIGNALS ) && p_readFromNBT_1_.hasKey( KEY_EW_SIGNALS ) ) {
            // Import previous signals lists
            nsSignalsString = p_readFromNBT_1_.getString( KEY_NS_SIGNALS );
            ewSignalsString = p_readFromNBT_1_.getString( KEY_EW_SIGNALS );
        }

        // Load existing serialized signals list (if present)
        if ( p_readFromNBT_1_.hasKey( SERIALIZED_SIGNAL_CIRCUIT_LIST_KEY ) ) {
            String serializedSignalCircuitList = p_readFromNBT_1_.getString( SERIALIZED_SIGNAL_CIRCUIT_LIST_KEY );
            String[] serializedSignalCircuits = serializedSignalCircuitList.split(
                    SERIALIZED_SIGNAL_CIRCUIT_LIST_SEPARATOR );
            for ( String serializedSignalCircuit : serializedSignalCircuits ) {
                TrafficSignalCircuit importedCircuit = new TrafficSignalCircuit( serializedSignalCircuit );
                signalCircuitList.add( importedCircuit );
            }

            // Remove old config
            p_readFromNBT_1_.removeTag( KEY_NS_SIGNALS );
            p_readFromNBT_1_.removeTag( KEY_EW_SIGNALS );
        }

        // Load existing serialized signal state list (if present)
        if ( p_readFromNBT_1_.hasKey( SERIALIZED_SIGNAL_STATE_LIST_KEY ) ) {
            String serializedSignalStateList = p_readFromNBT_1_.getString( SERIALIZED_SIGNAL_STATE_LIST_KEY );
            String[] serializedSignalStates = serializedSignalStateList.split( SERIALIZED_SIGNAL_STATE_LIST_SEPARATOR );
            for ( String serializedSignalState : serializedSignalStates ) {
                TrafficSignalState importedState = TrafficSignalState.deserialize( serializedSignalState );
                signalStateList.add( importedState );
            }
        }

        // Load current mode
        if ( p_readFromNBT_1_.hasKey( KEY_CURRENT_MODE ) ) {
            this.currentMode = p_readFromNBT_1_.getInteger( KEY_CURRENT_MODE );
        }
        else {
            this.currentMode = 0;
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
        // Write current flash mode enabled
        p_writeToNBT_1_.setInteger( KEY_CURRENT_MODE, currentMode );

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
        if ( signalCircuitList.size() > 0 ) {
            p_writeToNBT_1_.setString( SERIALIZED_SIGNAL_CIRCUIT_LIST_KEY,
                                       serializedSignalCircuitListStringBuilder.toString() );
        }

        // Write signal state list
        StringBuilder serializedSignalStateListStringBuilder = new StringBuilder();
        Iterator< TrafficSignalState > signalStateIterator = signalStateList.iterator();
        while ( signalStateIterator.hasNext() ) {
            serializedSignalStateListStringBuilder.append( signalStateIterator.next().serialize() );
            if ( signalStateIterator.hasNext() ) {
                serializedSignalStateListStringBuilder.append( SERIALIZED_SIGNAL_STATE_LIST_SEPARATOR );
            }
        }
        if ( signalStateList.size() > 0 ) {
            p_writeToNBT_1_.setString( SERIALIZED_SIGNAL_STATE_LIST_KEY,
                                       serializedSignalStateListStringBuilder.toString() );
        }

        return super.writeToNBT( p_writeToNBT_1_ );
    }

    public long getLastPhaseChangeTime() {
        return lastPhaseChangeTime;
    }

    public int getCycleTickRate() {
        return currentMode == 0 ? 4 : 20;
    }

    private boolean lastPowered = false;

    public void updateSignalStates( World world ) {
        // Create temporary list to store states as built
        ArrayList< TrafficSignalState > tempSignalStateList = new ArrayList<>();

        // Handle flash versus normal operation
        if ( currentMode == CURRENT_MODE_FLASH ) {
            // Create two flash states
            TrafficSignalState flashState1 = new TrafficSignalState( 1, -1 );
            TrafficSignalState flashState2 = new TrafficSignalState( 1, -1 );

            // Loop through signal circuits
            for ( int index = 0; index < signalCircuitList.size(); index++ ) {
                TrafficSignalCircuit signalCircuit = signalCircuitList.get( index );

                // Add ahead/protected ahead flash to correct state
                if ( index == 0 ) {
                    // Primary circuit flashes yellow on state 1
                    flashState1.addYellowSignals( signalCircuit.getAheadSignals() );
                    flashState1.addYellowSignals( signalCircuit.getProtectedSignals() );
                    flashState2.addOffSignals( signalCircuit.getAheadSignals() );
                    flashState2.addOffSignals( signalCircuit.getProtectedSignals() );
                }
                if ( index % 2 == 0 ) {
                    // Secondary circuit (even #) flashes red on state 1
                    flashState1.addRedSignals( signalCircuit.getAheadSignals() );
                    flashState1.addRedSignals( signalCircuit.getProtectedSignals() );
                    flashState2.addOffSignals( signalCircuit.getAheadSignals() );
                    flashState2.addOffSignals( signalCircuit.getProtectedSignals() );
                }
                else {
                    // Secondary circuit (odd #) flashes red on state 2
                    flashState2.addRedSignals( signalCircuit.getAheadSignals() );
                    flashState2.addRedSignals( signalCircuit.getProtectedSignals() );
                    flashState1.addOffSignals( signalCircuit.getAheadSignals() );
                    flashState1.addOffSignals( signalCircuit.getProtectedSignals() );
                }

                // Add turn signals
                if ( index % 2 == 0 ) {
                    // Even # flashes red on state 2
                    flashState2.addRedSignals( signalCircuit.getHybridLeftSignals() );
                    flashState2.addRedSignals( signalCircuit.getLeftSignals() );
                    flashState2.addRedSignals( signalCircuit.getRightSignals() );
                    flashState1.addOffSignals( signalCircuit.getHybridLeftSignals() );
                    flashState1.addOffSignals( signalCircuit.getLeftSignals() );
                    flashState1.addOffSignals( signalCircuit.getRightSignals() );
                }
                else {
                    // Odd # flashes red on state 1
                    flashState1.addRedSignals( signalCircuit.getHybridLeftSignals() );
                    flashState1.addRedSignals( signalCircuit.getLeftSignals() );
                    flashState1.addRedSignals( signalCircuit.getRightSignals() );
                    flashState2.addOffSignals( signalCircuit.getHybridLeftSignals() );
                    flashState2.addOffSignals( signalCircuit.getLeftSignals() );
                    flashState2.addOffSignals( signalCircuit.getRightSignals() );
                }

                // Add pedestrian signals
                flashState1.addOffSignals( signalCircuit.getPedestrianSignals() );
            }

            // Add completed flash states to new state list
            tempSignalStateList.add( flashState1 );
            tempSignalStateList.add( flashState2 );
        }
        else if ( currentMode == CURRENT_MODE_STANDARD ) {
            // Create an all red state
            TrafficSignalState allRedSignalState = new TrafficSignalState( 4, -1 );
            for ( TrafficSignalCircuit signalCircuit : signalCircuitList ) {
                allRedSignalState.addRedSignals( signalCircuit.getHybridLeftSignals() );
                allRedSignalState.addRedSignals( signalCircuit.getLeftSignals() );
                allRedSignalState.addRedSignals( signalCircuit.getAheadSignals() );
                allRedSignalState.addRedSignals( signalCircuit.getRightSignals() );
                allRedSignalState.addRedSignals( signalCircuit.getProtectedSignals() );
                allRedSignalState.addRedSignals( signalCircuit.getPedestrianSignals() );
            }

            // Add all red state as first
            tempSignalStateList.add( allRedSignalState );

            // Loop through signal circuits
            for ( int index = 0; index < signalCircuitList.size(); index++ ) {
                TrafficSignalCircuit signalCircuit = signalCircuitList.get( index );

                // If all signals facing same direction, can unify turns and ahead due to lack of conflict
                if ( signalCircuit.areSignalsFacingSameDirection( world ) ) {
                    // Create state for signals on green
                    TrafficSignalState circuitGreenState = new TrafficSignalState( 15, index );
                    circuitGreenState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                    circuitGreenState.addGreenSignals( signalCircuit.getLeftSignals() );
                    circuitGreenState.addGreenSignals( signalCircuit.getAheadSignals() );
                    circuitGreenState.addGreenSignals( signalCircuit.getRightSignals() );
                    circuitGreenState.addGreenSignals( signalCircuit.getProtectedSignals() );
                    circuitGreenState.combine( allRedSignalState );
                    tempSignalStateList.add( circuitGreenState );

                    // Create state for signals on yellow
                    TrafficSignalState circuitYellowState = new TrafficSignalState( 5, index );
                    circuitYellowState.addYellowSignals( signalCircuit.getHybridLeftSignals() );
                    circuitYellowState.addYellowSignals( signalCircuit.getLeftSignals() );
                    circuitYellowState.addYellowSignals( signalCircuit.getAheadSignals() );
                    circuitYellowState.addYellowSignals( signalCircuit.getRightSignals() );
                    circuitYellowState.addYellowSignals( signalCircuit.getProtectedSignals() );
                    circuitYellowState.combine( allRedSignalState );
                    tempSignalStateList.add( circuitYellowState );

                    // Add all red phase to follow
                    if ( index < signalCircuitList.size() - 1 ) {
                        tempSignalStateList.add( allRedSignalState );
                    }
                }
                else {
                    if ( signalCircuit.getHybridLeftSignals().size() > 0 ||
                            signalCircuit.getLeftSignals().size() > 0 ) {
                        // Create state for left turn signals on green
                        TrafficSignalState circuitLeftGreenState = new TrafficSignalState( 10, index );
                        circuitLeftGreenState.addOffSignals( signalCircuit.getHybridLeftSignals() );
                        circuitLeftGreenState.addGreenSignals( signalCircuit.getLeftSignals() );
                        circuitLeftGreenState.combine( allRedSignalState );
                        tempSignalStateList.add( circuitLeftGreenState );

                        // Create state for left turn signals on yellow
                        TrafficSignalState circuitLeftYellowState = new TrafficSignalState( 5, index );
                        circuitLeftYellowState.addYellowSignals( signalCircuit.getHybridLeftSignals() );
                        circuitLeftYellowState.addYellowSignals( signalCircuit.getLeftSignals() );
                        circuitLeftYellowState.combine( allRedSignalState );
                        tempSignalStateList.add( circuitLeftYellowState );

                        // Add all red phase
                        tempSignalStateList.add( allRedSignalState );
                    }

                    // Handle if circuit has protected signals
                    if ( signalCircuit.getProtectedSignals().size() > 0 ) {
                        // Create state for ahead/protected signals on green
                        TrafficSignalState circuitAheadGreenState = new TrafficSignalState( 15, index );
                        if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                            circuitAheadGreenState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                        }
                        circuitAheadGreenState.addGreenSignals( signalCircuit.getAheadSignals() );
                        circuitAheadGreenState.addGreenSignals( signalCircuit.getProtectedSignals() );
                        circuitAheadGreenState.combine( allRedSignalState );
                        tempSignalStateList.add( circuitAheadGreenState );

                        // Create state for ahead/protected signals on yellow
                        TrafficSignalState circuitAheadYellowState = new TrafficSignalState( 5, index );
                        if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                            circuitAheadYellowState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                        }
                        circuitAheadYellowState.addYellowSignals( signalCircuit.getAheadSignals() );
                        circuitAheadYellowState.addYellowSignals( signalCircuit.getProtectedSignals() );
                        circuitAheadYellowState.combine( allRedSignalState );
                        tempSignalStateList.add( circuitAheadYellowState );

                        // Add hybrid left yellow cycle (if present)
                        if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                            // Add all red phase
                            TrafficSignalState circuitSwitchRedState = new TrafficSignalState( 2, index );
                            circuitSwitchRedState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                            circuitSwitchRedState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitSwitchRedState );

                            TrafficSignalState circuitHybridLeftYellowState = new TrafficSignalState( 5, index );
                            circuitHybridLeftYellowState.addYellowSignals( signalCircuit.getHybridLeftSignals() );
                            circuitHybridLeftYellowState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitHybridLeftYellowState );
                        }

                        // Create right turn states (if signals present)
                        if ( signalCircuit.getRightSignals().size() > 0 ) {
                            // Add all red phase
                            tempSignalStateList.add( allRedSignalState );

                            // Create state for right signals on green
                            TrafficSignalState circuitRightGreenState = new TrafficSignalState( 10, index );
                            circuitRightGreenState.addGreenSignals( signalCircuit.getRightSignals() );
                            circuitRightGreenState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitRightGreenState );

                            // Create state for right signals on yellow
                            TrafficSignalState circuitRightYellowState = new TrafficSignalState( 5, index );
                            circuitRightYellowState.addYellowSignals( signalCircuit.getRightSignals() );
                            circuitRightYellowState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitRightYellowState );
                        }

                        // Add all red phase to follow
                        if ( index < signalCircuitList.size() - 1 ) {
                            tempSignalStateList.add( allRedSignalState );
                        }
                    }
                    // Handle if circuit does not have protected signals
                    else {
                        // Create state for ahead/right signals on green
                        TrafficSignalState circuitGreenState = new TrafficSignalState( 15, index );
                        if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                            circuitGreenState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                        }
                        circuitGreenState.addGreenSignals( signalCircuit.getAheadSignals() );
                        circuitGreenState.addGreenSignals( signalCircuit.getRightSignals() );
                        circuitGreenState.combine( allRedSignalState );
                        tempSignalStateList.add( circuitGreenState );

                        // Create state for ahead/right signals on yellow
                        TrafficSignalState circuitYellowState = new TrafficSignalState( 5, index );
                        if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                            circuitYellowState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                        }
                        circuitYellowState.addYellowSignals( signalCircuit.getAheadSignals() );
                        circuitYellowState.addYellowSignals( signalCircuit.getRightSignals() );
                        circuitYellowState.combine( allRedSignalState );
                        tempSignalStateList.add( circuitYellowState );

                        // Add hybrid left yellow cycle (if present)
                        if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                            // Add all red phase
                            TrafficSignalState circuitSwitchRedState = new TrafficSignalState( 2, index );
                            circuitSwitchRedState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                            circuitSwitchRedState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitSwitchRedState );

                            TrafficSignalState circuitHybridLeftYellowState = new TrafficSignalState( 5, index );
                            circuitHybridLeftYellowState.addYellowSignals( signalCircuit.getHybridLeftSignals() );
                            circuitHybridLeftYellowState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitHybridLeftYellowState );
                        }

                        // Add all red phase to follow
                        if ( index < signalCircuitList.size() - 1 ) {
                            tempSignalStateList.add( allRedSignalState );
                        }
                    }
                }
            }
        }
        else if ( currentMode == CURRENT_MODE_METER ) {
            // Create an all red state (pedestrian yellow - meter mode)
            TrafficSignalState allRedSignalState = new TrafficSignalState( 4, -1 );
            for ( TrafficSignalCircuit signalCircuit : signalCircuitList ) {
                allRedSignalState.addRedSignals( signalCircuit.getHybridLeftSignals() );
                allRedSignalState.addRedSignals( signalCircuit.getLeftSignals() );
                allRedSignalState.addRedSignals( signalCircuit.getAheadSignals() );
                allRedSignalState.addRedSignals( signalCircuit.getRightSignals() );
                allRedSignalState.addRedSignals( signalCircuit.getProtectedSignals() );
                allRedSignalState.addGreenSignals( signalCircuit.getPedestrianSignals() );
            }

            // Add all red state as first
            tempSignalStateList.add( allRedSignalState );

            // Loop through signal circuits
            for ( int index = 0; index < signalCircuitList.size(); index++ ) {
                TrafficSignalCircuit signalCircuit = signalCircuitList.get( index );

                // Create green state for circuit
                TrafficSignalState circuitGreenState = new TrafficSignalState( 3, index );
                circuitGreenState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                circuitGreenState.addGreenSignals( signalCircuit.getLeftSignals() );
                circuitGreenState.addGreenSignals( signalCircuit.getAheadSignals() );
                circuitGreenState.addGreenSignals( signalCircuit.getProtectedSignals() );
                circuitGreenState.addGreenSignals( signalCircuit.getRightSignals() );
                circuitGreenState.combine( allRedSignalState );
                tempSignalStateList.add( circuitGreenState );

                // Add all red phase to follow
                if ( index < signalCircuitList.size() - 1 ) {
                    tempSignalStateList.add( allRedSignalState );
                }
            }
        }

        // Store updated state list
        signalStateList.clear();
        signalStateList.addAll( tempSignalStateList );
        markDirty();
    }

    public void updateSignals( TrafficSignalState signalState, boolean powered ) {
        // Apply red signals
        for ( BlockPos signalPos : signalState.getRedSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       AbstractBlockControllableSignal.SIGNAL_RED );
                }
                else {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       AbstractBlockControllableSignal.SIGNAL_OFF );
                }
            }
        }

        // Apply yellow signals
        for ( BlockPos signalPos : signalState.getYellowSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       AbstractBlockControllableSignal.SIGNAL_YELLOW );
                }
                else {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       AbstractBlockControllableSignal.SIGNAL_OFF );
                }
            }
        }

        // Apply red signals
        for ( BlockPos signalPos : signalState.getGreenSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( powered ) {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       AbstractBlockControllableSignal.SIGNAL_GREEN );
                }
                else {
                    AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                       AbstractBlockControllableSignal.SIGNAL_OFF );
                }
            }
        }

        // Apply off signals
        for ( BlockPos signalPos : signalState.getOffSignals() ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                   AbstractBlockControllableSignal.SIGNAL_OFF );
            }
        }
    }

    public String switchMode( World world ) {
        currentMode++;
        if ( currentMode > CURRENT_MODE_METER ) {
            currentMode = CURRENT_MODE_FLASH;
        }
        markDirty();
        updateSignalStates( world );

        String modeName = "[unknown, error]";
        if ( currentMode == CURRENT_MODE_FLASH ) {
            modeName = "flash";
        }
        else if ( currentMode == CURRENT_MODE_STANDARD ) {
            modeName = "standard";
        }
        else if ( currentMode == CURRENT_MODE_METER ) {
            modeName = "ramp meter";
        }
        return modeName;
    }

    public void cycleSignals( boolean powered ) {
        if ( signalStateList.size() > 0 ) {
            this.lastPhaseChangeTime = System.currentTimeMillis();

            // Reset current phase if out of bounds
            boolean phaseChanged = false;
            if ( currentPhase >= signalStateList.size() ) {
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
            if ( currentPhaseTime++ > signalStateList.get( currentPhase ).getLength() ) {
                currentPhase++;
                currentPhaseTime = 0;
                phaseChanged = true;
            }
            if ( currentPhase >= signalStateList.size() ) {
                currentPhase = 0;
                currentPhaseTime = 0;
                phaseChanged = true;
            }

            // Update signals if phase changed
            if ( phaseChanged ) {
                updateSignals( signalStateList.get( currentPhase ), powered );
            }
        }
    }
}
