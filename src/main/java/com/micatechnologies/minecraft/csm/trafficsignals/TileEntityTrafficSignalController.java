package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalCircuit;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
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

    private static final String                     SERIALIZED_SIGNAL_FLASH_STATE_LIST_KEY
                                                                                                 = "SerializedSignalFlashStateList";
    private static final String                     SERIALIZED_SIGNAL_FLASH_STATE_LIST_SEPARATOR = ":";
    private final        List< TrafficSignalState > signalFlashStateList                         = new ArrayList<>();

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
                // Remove circuit if now empty
                if ( signalCircuit.getSize() == 0 ) {
                    signalCircuitList.remove( signalCircuit );
                }

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
            else if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.NA_SENSOR ) {
                linkToCircuit.linkSensor( blockPos );
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

    private static final String  KEY_BOOT_SAFE              = "BootSafe";
    private static final String  KEY_BOOT_SAFE_FLASH        = "BootSafeFlash";
    private static final String  KEY_LAST_PHASE_CHANGE_TIME = "LastPhaseChangeTime";
    private static final String  KEY_CURR_PHASE_TIME        = "CurrPhaseTime";
    private static final String  KEY_CURRENT_PHASE          = "CurrPhase";
    private static final String  KEY_CURRENT_MODE           = "CurrentMode";
    private static final int     CURRENT_MODE_FLASH         = 0;
    private static final int     CURRENT_MODE_STANDARD      = 1;
    private static final int     CURRENT_MODE_METER         = 2;
    private static final int     CURRENT_MODE_REQUESTABLE   = 3;
    private              boolean bootSafe;
    private              boolean bootSafeFlash;
    private              int     currentPhase;
    private              int     currentPhaseTime;
    private              long    lastPhaseChangeTime;
    private              int     currentMode;

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        super.readFromNBT( p_readFromNBT_1_ );

        // Load existing serialized signals list (if present)
        if ( p_readFromNBT_1_.hasKey( SERIALIZED_SIGNAL_CIRCUIT_LIST_KEY ) ) {
            String serializedSignalCircuitList = p_readFromNBT_1_.getString( SERIALIZED_SIGNAL_CIRCUIT_LIST_KEY );
            String[] serializedSignalCircuits = serializedSignalCircuitList.split(
                    SERIALIZED_SIGNAL_CIRCUIT_LIST_SEPARATOR );
            for ( String serializedSignalCircuit : serializedSignalCircuits ) {
                TrafficSignalCircuit importedCircuit = new TrafficSignalCircuit( serializedSignalCircuit );
                signalCircuitList.add( importedCircuit );
            }
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

        // Load existing serialized signal flash state list (if present)
        if ( p_readFromNBT_1_.hasKey( SERIALIZED_SIGNAL_FLASH_STATE_LIST_KEY ) ) {
            String serializedSignalFlashStateList = p_readFromNBT_1_.getString(
                    SERIALIZED_SIGNAL_FLASH_STATE_LIST_KEY );
            String[] serializedSignalFlashStates = serializedSignalFlashStateList.split(
                    SERIALIZED_SIGNAL_FLASH_STATE_LIST_SEPARATOR );
            for ( String serializedSignalFlashState : serializedSignalFlashStates ) {
                TrafficSignalState importedState = TrafficSignalState.deserialize( serializedSignalFlashState );
                signalFlashStateList.add( importedState );
            }
        }

        // Load boot safe flag
        if ( p_readFromNBT_1_.hasKey( KEY_BOOT_SAFE ) ) {
            this.bootSafe = p_readFromNBT_1_.getBoolean( KEY_BOOT_SAFE );
        }
        else {
            this.bootSafe = false;
        }

        // Load boot safe flash flag
        if ( p_readFromNBT_1_.hasKey( KEY_BOOT_SAFE_FLASH ) ) {
            this.bootSafeFlash = p_readFromNBT_1_.getBoolean( KEY_BOOT_SAFE_FLASH );
        }
        else {
            this.bootSafeFlash = false;
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

    public int getCircuitPriorityJumpIndex( World world ) {
        // Find circuit with highest priority
        int highestPriorityCircuitIndex = -1;
        int highestPriorityCircuitValue = 0;
        for ( int index = 0; index < signalCircuitList.size(); index++ ) {
            TrafficSignalCircuit signalCircuit = signalCircuitList.get( index );
            int signalCircuitPriority = signalCircuit.getCircuitPriority( world );
            if ( signalCircuitPriority > highestPriorityCircuitValue ) {
                highestPriorityCircuitIndex = index;
                highestPriorityCircuitValue = signalCircuitPriority;
            }
        }
        return highestPriorityCircuitIndex;
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
        // Write boot safe flag
        p_writeToNBT_1_.setBoolean( KEY_BOOT_SAFE, bootSafe );

        // Write boot safe flash flag
        p_writeToNBT_1_.setBoolean( KEY_BOOT_SAFE_FLASH, bootSafeFlash );

        // Write current mode
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

        // Write signal flash state list
        StringBuilder serializedSignalFlashStateListStringBuilder = new StringBuilder();
        Iterator< TrafficSignalState > signalFlashStateIterator = signalFlashStateList.iterator();
        while ( signalFlashStateIterator.hasNext() ) {
            serializedSignalFlashStateListStringBuilder.append( signalFlashStateIterator.next().serialize() );
            if ( signalFlashStateIterator.hasNext() ) {
                serializedSignalFlashStateListStringBuilder.append( SERIALIZED_SIGNAL_FLASH_STATE_LIST_SEPARATOR );
            }
        }
        if ( signalFlashStateList.size() > 0 ) {
            p_writeToNBT_1_.setString( SERIALIZED_SIGNAL_FLASH_STATE_LIST_KEY,
                                       serializedSignalFlashStateListStringBuilder.toString() );
        }

        return super.writeToNBT( p_writeToNBT_1_ );
    }

    public long getLastPhaseChangeTime() {
        return lastPhaseChangeTime;
    }

    public int getCycleTickRate() {
        // Get default tick rate
        int currentTickRate = 20;

        // Set tick rate to 4 if flashing
        if ( currentMode == 0 ) {
            currentTickRate = 4;
        }
        else if ( !bootSafe ) {
            currentTickRate = 12;
        }

        return currentTickRate;
    }

    private boolean lastPowered = false;

    public void runAutomaticSystemVerification( World world ) {
        boolean didChange = verifyAndCleanupSignalTypes( world );
        if ( didChange ) {
            updateSignalStates( world );
        }
    }

    private boolean verifyAndCleanupSignalTypes( World world ) {
        // Boolean to track if changes made
        boolean changed = false;

        // Loop through each signal in each circuit - verify it is in correct list, else move
        for ( TrafficSignalCircuit signalCircuit : signalCircuitList ) {
            // Build list to unlink
            List< BlockPos > signalsToUnlink = new ArrayList<>();

            // Verify hybrid left signals
            for ( BlockPos signalPos : signalCircuit.getHybridLeftSignals() ) {
                IBlockState state = world.getBlockState( signalPos );
                Block blockAtSignalPos = state.getBlock();
                if ( blockAtSignalPos instanceof AbstractBlockControllableSignal ) {
                    AbstractBlockControllableSignal signalAtSignalPos
                            = ( AbstractBlockControllableSignal ) blockAtSignalPos;

                    // Signal is wrong type, unlink and turn off
                    if ( signalAtSignalPos.getSignalSide( world, signalPos ) !=
                            AbstractBlockControllableSignal.SIGNAL_SIDE.HYBRID_LEFT ) {
                        AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                           AbstractBlockControllableSignal.SIGNAL_OFF );
                        signalsToUnlink.add( signalPos );
                    }
                }
                else {
                    // Signal not there anymore, unlink
                    signalsToUnlink.add( signalPos );
                }
            }

            // Verify left signals
            for ( BlockPos signalPos : signalCircuit.getLeftSignals() ) {
                IBlockState state = world.getBlockState( signalPos );
                Block blockAtSignalPos = state.getBlock();
                if ( blockAtSignalPos instanceof AbstractBlockControllableSignal ) {
                    AbstractBlockControllableSignal signalAtSignalPos
                            = ( AbstractBlockControllableSignal ) blockAtSignalPos;

                    // Signal is wrong type, unlink and turn off
                    if ( signalAtSignalPos.getSignalSide( world, signalPos ) !=
                            AbstractBlockControllableSignal.SIGNAL_SIDE.LEFT ) {
                        AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                           AbstractBlockControllableSignal.SIGNAL_OFF );
                        signalsToUnlink.add( signalPos );
                    }
                }
                else {
                    // Signal not there anymore, unlink
                    signalsToUnlink.add( signalPos );
                }
            }

            // Verify ahead signals
            for ( BlockPos signalPos : signalCircuit.getAheadSignals() ) {
                IBlockState state = world.getBlockState( signalPos );
                Block blockAtSignalPos = state.getBlock();
                if ( blockAtSignalPos instanceof AbstractBlockControllableSignal ) {
                    AbstractBlockControllableSignal signalAtSignalPos
                            = ( AbstractBlockControllableSignal ) blockAtSignalPos;

                    // Signal is wrong type, unlink and turn off
                    if ( signalAtSignalPos.getSignalSide( world, signalPos ) !=
                            AbstractBlockControllableSignal.SIGNAL_SIDE.AHEAD ) {
                        AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                           AbstractBlockControllableSignal.SIGNAL_OFF );
                        signalsToUnlink.add( signalPos );
                    }
                }
                else {
                    // Signal not there anymore, unlink
                    signalsToUnlink.add( signalPos );
                }
            }

            // Verify right signals
            for ( BlockPos signalPos : signalCircuit.getRightSignals() ) {
                IBlockState state = world.getBlockState( signalPos );
                Block blockAtSignalPos = state.getBlock();
                if ( blockAtSignalPos instanceof AbstractBlockControllableSignal ) {
                    AbstractBlockControllableSignal signalAtSignalPos
                            = ( AbstractBlockControllableSignal ) blockAtSignalPos;

                    // Signal is wrong type, unlink and turn off
                    if ( signalAtSignalPos.getSignalSide( world, signalPos ) !=
                            AbstractBlockControllableSignal.SIGNAL_SIDE.RIGHT ) {
                        AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                           AbstractBlockControllableSignal.SIGNAL_OFF );
                        signalsToUnlink.add( signalPos );
                    }
                }
                else {
                    // Signal not there anymore, unlink
                    signalsToUnlink.add( signalPos );
                }
            }

            // Verify crosswalk signals
            for ( BlockPos signalPos : signalCircuit.getPedestrianSignals() ) {
                IBlockState state = world.getBlockState( signalPos );
                Block blockAtSignalPos = state.getBlock();
                if ( blockAtSignalPos instanceof AbstractBlockControllableSignal ) {
                    AbstractBlockControllableSignal signalAtSignalPos
                            = ( AbstractBlockControllableSignal ) blockAtSignalPos;

                    // Signal is wrong type, unlink and turn off
                    if ( signalAtSignalPos.getSignalSide( world, signalPos ) !=
                            AbstractBlockControllableSignal.SIGNAL_SIDE.CROSSWALK ) {
                        AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                           AbstractBlockControllableSignal.SIGNAL_OFF );
                        signalsToUnlink.add( signalPos );
                    }
                }
                else {
                    // Signal not there anymore, unlink
                    signalsToUnlink.add( signalPos );
                }
            }

            // Verify protected ahead signals
            for ( BlockPos signalPos : signalCircuit.getProtectedSignals() ) {
                IBlockState state = world.getBlockState( signalPos );
                Block blockAtSignalPos = state.getBlock();
                if ( blockAtSignalPos instanceof AbstractBlockControllableSignal ) {
                    AbstractBlockControllableSignal signalAtSignalPos
                            = ( AbstractBlockControllableSignal ) blockAtSignalPos;

                    // Signal is wrong type, unlink and turn off
                    if ( signalAtSignalPos.getSignalSide( world, signalPos ) !=
                            AbstractBlockControllableSignal.SIGNAL_SIDE.PROTECTED_AHEAD ) {
                        AbstractBlockControllableSignal.changeSignalColor( world, signalPos,
                                                                           AbstractBlockControllableSignal.SIGNAL_OFF );
                        signalsToUnlink.add( signalPos );
                    }
                }
                else {
                    // Signal not there anymore, unlink
                    signalsToUnlink.add( signalPos );
                }
            }

            // Unlink signals that need to be unlinked
            for ( BlockPos signalPos : signalsToUnlink ) {
                if ( !changed ) {
                    changed = true;
                }
                IBlockState state = world.getBlockState( signalPos );
                Block blockAtUnlinkSignalPos = state.getBlock();
                if ( blockAtUnlinkSignalPos instanceof AbstractBlockControllableSignal ) {
                    signalCircuit.unlink( signalPos );
                }
            }
        }
        return changed;
    }

    public void updateSignalStates( World world ) {
        // Verify and cleanup signal types
        verifyAndCleanupSignalTypes( world );

        // Generate flash states (by default, used at power on)
        TrafficSignalState flashState1 = new TrafficSignalState( 1, -1 );
        TrafficSignalState flashState2 = new TrafficSignalState( 1, -1 );

        // Loop through signal circuits
        for ( int index = 0; index < signalCircuitList.size(); index++ ) {
            TrafficSignalCircuit signalCircuit = signalCircuitList.get( index );

            // Add ahead/protected ahead flash to correct state
            if ( index == 0 ) {
                // Primary circuit flashes yellow on state 1
                flashState1.addYellowSignals( signalCircuit.getAheadSignals() );
                flashState1.addRedSignals( signalCircuit.getRightSignals() );
                flashState2.addOffSignals( signalCircuit.getAheadSignals() );
                flashState2.addOffSignals( signalCircuit.getRightSignals() );
            }
            if ( index % 2 == 0 ) {
                // Secondary circuit (even #) flashes red on state 1
                flashState1.addRedSignals( signalCircuit.getAheadSignals() );
                flashState1.addRedSignals( signalCircuit.getRightSignals() );
                flashState2.addOffSignals( signalCircuit.getAheadSignals() );
                flashState2.addOffSignals( signalCircuit.getRightSignals() );
            }
            else {
                // Secondary circuit (odd #) flashes red on state 2
                flashState2.addRedSignals( signalCircuit.getAheadSignals() );
                flashState2.addRedSignals( signalCircuit.getRightSignals() );
                flashState1.addOffSignals( signalCircuit.getAheadSignals() );
                flashState1.addOffSignals( signalCircuit.getRightSignals() );
            }

            // Add turn/protected signals
            if ( index % 2 == 0 ) {
                // Even # flashes red on state 2
                flashState2.addRedSignals( signalCircuit.getHybridLeftSignals() );
                flashState2.addRedSignals( signalCircuit.getLeftSignals() );
                flashState2.addRedSignals( signalCircuit.getProtectedSignals() );
                flashState1.addOffSignals( signalCircuit.getHybridLeftSignals() );
                flashState1.addOffSignals( signalCircuit.getLeftSignals() );
                flashState1.addOffSignals( signalCircuit.getProtectedSignals() );
            }
            else {
                // Odd # flashes red on state 1
                flashState1.addRedSignals( signalCircuit.getHybridLeftSignals() );
                flashState1.addRedSignals( signalCircuit.getLeftSignals() );
                flashState1.addRedSignals( signalCircuit.getProtectedSignals() );
                flashState2.addOffSignals( signalCircuit.getHybridLeftSignals() );
                flashState2.addOffSignals( signalCircuit.getLeftSignals() );
                flashState2.addOffSignals( signalCircuit.getProtectedSignals() );
            }

            // Add pedestrian signals
            flashState1.addOffSignals( signalCircuit.getPedestrianSignals() );
        }
        signalFlashStateList.clear();
        signalFlashStateList.add( flashState1 );
        signalFlashStateList.add( flashState2 );

        // Create temporary list to store states as built
        ArrayList< TrafficSignalState > tempSignalStateList = new ArrayList<>();

        // Handle flash versus normal operation
        if ( currentMode == CURRENT_MODE_FLASH || currentMode == CURRENT_MODE_REQUESTABLE ) {
            // Add completed flash states to new state list
            tempSignalStateList.addAll( signalFlashStateList );
        }
        else if ( currentMode == CURRENT_MODE_STANDARD ) {
            // Create an all red state
            TrafficSignalState allRedSignalState = new TrafficSignalState( 3, -1 );
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

            // Build crosswalk state
            TrafficSignalState allCircuitCrosswalkState = new TrafficSignalState( 12, -1 );
            for ( int pedIndex = 0; pedIndex < signalCircuitList.size(); pedIndex++ ) {

                allCircuitCrosswalkState.addGreenSignals( signalCircuitList.get( pedIndex ).getPedestrianSignals() );

            }
            allCircuitCrosswalkState.combine( allRedSignalState );
            tempSignalStateList.add( allCircuitCrosswalkState );
            tempSignalStateList.add( allRedSignalState );

            // Loop through signal circuits
            for ( int index = 0; index < signalCircuitList.size(); index++ ) {
                TrafficSignalCircuit signalCircuit = signalCircuitList.get( index );

                // If all signals facing same direction, can unify turns and ahead due to lack of conflict
                if ( signalCircuit.areSignalsFacingSameDirection( world ) ) {
                    // Handle if circuit has protected signals
                    if ( signalCircuit.getProtectedSignals().size() > 0 ) {
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
                            TrafficSignalState indexedAllRedState = new TrafficSignalState(
                                    allRedSignalState.getLength(), index );
                            indexedAllRedState.combine( allRedSignalState );
                            tempSignalStateList.add( indexedAllRedState );
                        }

                        // Create state for ahead/protected signals on green
                        TrafficSignalState circuitAheadGreenState = new TrafficSignalState( 15, index );
                        circuitAheadGreenState.addGreenSignals( signalCircuit.getAheadSignals() );
                        circuitAheadGreenState.addGreenSignals( signalCircuit.getProtectedSignals() );
                        for ( int pedIndex = 0; pedIndex < signalCircuitList.size(); pedIndex++ ) {
                            if ( index != pedIndex ) {
                                circuitAheadGreenState.addGreenSignals(
                                        signalCircuitList.get( pedIndex ).getPedestrianSignals() );
                            }
                        }
                        circuitAheadGreenState.combine( allRedSignalState );
                        tempSignalStateList.add( circuitAheadGreenState );

                        // Create right turn states (if signals present)
                        if ( signalCircuit.getRightSignals().size() > 0 ) {
                            // Create state for ahead/protected signals on yellow
                            TrafficSignalState circuitAheadYellowState = new TrafficSignalState( 5, index );
                            circuitAheadYellowState.addGreenSignals( signalCircuit.getAheadSignals() );
                            circuitAheadYellowState.addYellowSignals( signalCircuit.getProtectedSignals() );
                            circuitAheadYellowState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitAheadYellowState );

                            // Add all red phase
                            TrafficSignalState indexedAllRedState = new TrafficSignalState(
                                    allRedSignalState.getLength(), index );
                            indexedAllRedState.addGreenSignals( signalCircuit.getAheadSignals() );
                            indexedAllRedState.combine( allRedSignalState );
                            tempSignalStateList.add( indexedAllRedState );

                            // Create state for right signals on green
                            TrafficSignalState circuitRightGreenState = new TrafficSignalState( 10, index );
                            circuitRightGreenState.addGreenSignals( signalCircuit.getAheadSignals() );
                            circuitRightGreenState.addGreenSignals( signalCircuit.getRightSignals() );
                            circuitRightGreenState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitRightGreenState );

                            // Create state for right signals on yellow
                            TrafficSignalState circuitRightYellowState = new TrafficSignalState( 5, index );
                            circuitRightYellowState.addYellowSignals( signalCircuit.getAheadSignals() );
                            circuitRightYellowState.addYellowSignals( signalCircuit.getRightSignals() );
                            circuitRightYellowState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitRightYellowState );
                        }
                        else {
                            // Create state for ahead/protected signals on yellow
                            TrafficSignalState circuitAheadYellowState = new TrafficSignalState( 5, index );
                            circuitAheadYellowState.addYellowSignals( signalCircuit.getAheadSignals() );
                            circuitAheadYellowState.addYellowSignals( signalCircuit.getProtectedSignals() );
                            circuitAheadYellowState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitAheadYellowState );
                        }

                        // Add all red phase to follow
                        if ( index < signalCircuitList.size() - 1 ) {
                            tempSignalStateList.add( allRedSignalState );
                        }
                    }
                    else {
                        // Create state for signals on green
                        TrafficSignalState circuitGreenState = new TrafficSignalState( 15, index );
                        circuitGreenState.addOffSignals( signalCircuit.getHybridLeftSignals() );
                        circuitGreenState.addGreenSignals( signalCircuit.getLeftSignals() );
                        circuitGreenState.addGreenSignals( signalCircuit.getAheadSignals() );
                        circuitGreenState.addGreenSignals( signalCircuit.getRightSignals() );
                        circuitGreenState.addGreenSignals( signalCircuit.getProtectedSignals() );
                        for ( int pedIndex = 0; pedIndex < signalCircuitList.size(); pedIndex++ ) {
                            if ( index != pedIndex ) {
                                circuitGreenState.addGreenSignals(
                                        signalCircuitList.get( pedIndex ).getPedestrianSignals() );
                            }
                        }
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
                }
                else {
                    // Handle if circuit has protected signals
                    if ( signalCircuit.getProtectedSignals().size() > 0 ) {
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
                            TrafficSignalState indexedAllRedState = new TrafficSignalState(
                                    allRedSignalState.getLength(), index );
                            indexedAllRedState.combine( allRedSignalState );
                            tempSignalStateList.add( indexedAllRedState );
                        }

                        // Create state for ahead/protected signals on green
                        TrafficSignalState circuitAheadGreenState = new TrafficSignalState( 15, index );
                        if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                            circuitAheadGreenState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                        }
                        circuitAheadGreenState.addGreenSignals( signalCircuit.getAheadSignals() );
                        circuitAheadGreenState.addGreenSignals( signalCircuit.getProtectedSignals() );
                        for ( int pedIndex = 0; pedIndex < signalCircuitList.size(); pedIndex++ ) {
                            if ( index != pedIndex ) {
                                circuitAheadGreenState.addGreenSignals(
                                        signalCircuitList.get( pedIndex ).getPedestrianSignals() );
                            }
                        }
                        circuitAheadGreenState.combine( allRedSignalState );
                        tempSignalStateList.add( circuitAheadGreenState );

                        // Create right turn states (if signals present)
                        if ( signalCircuit.getRightSignals().size() > 0 ) {
                            // Create state for protected signals on yellow
                            TrafficSignalState circuitAheadYellowState = new TrafficSignalState( 5, index );
                            if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                                circuitAheadYellowState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                            }
                            circuitAheadYellowState.addGreenSignals( signalCircuit.getAheadSignals() );
                            circuitAheadYellowState.addYellowSignals( signalCircuit.getProtectedSignals() );
                            circuitAheadYellowState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitAheadYellowState );

                            // Add all red phase
                            TrafficSignalState indexedAllRedState = new TrafficSignalState(
                                    allRedSignalState.getLength(), index );
                            if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                                indexedAllRedState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                            }
                            indexedAllRedState.addGreenSignals( signalCircuit.getAheadSignals() );
                            indexedAllRedState.combine( allRedSignalState );
                            tempSignalStateList.add( indexedAllRedState );

                            // Create state for right signals on green
                            TrafficSignalState circuitRightGreenState = new TrafficSignalState( 10, index );
                            if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                                circuitRightGreenState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                            }
                            circuitRightGreenState.addGreenSignals( signalCircuit.getAheadSignals() );
                            circuitRightGreenState.addGreenSignals( signalCircuit.getRightSignals() );
                            circuitRightGreenState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitRightGreenState );

                            // Create state for right signals on yellow
                            TrafficSignalState circuitRightYellowState = new TrafficSignalState( 5, index );
                            if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                                circuitRightYellowState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                            }
                            circuitRightYellowState.addYellowSignals( signalCircuit.getAheadSignals() );
                            circuitRightYellowState.addYellowSignals( signalCircuit.getRightSignals() );
                            circuitRightYellowState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitRightYellowState );
                        }
                        else {
                            // Create state for protected signals on yellow
                            TrafficSignalState circuitAheadYellowState = new TrafficSignalState( 5, index );
                            if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                                circuitAheadYellowState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                            }
                            circuitAheadYellowState.addYellowSignals( signalCircuit.getAheadSignals() );
                            circuitAheadYellowState.addYellowSignals( signalCircuit.getProtectedSignals() );
                            circuitAheadYellowState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitAheadYellowState );
                        }

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
                    // Handle if circuit does not have protected signals
                    else {
                        // Check if left signals all facing same direction
                        boolean areLeftsAllFacingSame = true;
                        EnumFacing encounteredFacingDirection = null;
                        for ( BlockPos signalPos : signalCircuit.getHybridLeftSignals() ) {
                            IBlockState blockState = world.getBlockState( signalPos );
                            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                                EnumFacing currentFacingDirection = blockState.getValue( BlockHorizontal.FACING );
                                if ( encounteredFacingDirection == null ) {
                                    encounteredFacingDirection = currentFacingDirection;
                                }
                                else if ( encounteredFacingDirection != currentFacingDirection ) {
                                    areLeftsAllFacingSame = false;
                                }
                            }
                        }
                        for ( BlockPos signalPos : signalCircuit.getLeftSignals() ) {
                            IBlockState blockState = world.getBlockState( signalPos );
                            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                                EnumFacing currentFacingDirection = blockState.getValue( BlockHorizontal.FACING );
                                if ( encounteredFacingDirection == null ) {
                                    encounteredFacingDirection = currentFacingDirection;
                                }
                                else if ( encounteredFacingDirection != currentFacingDirection ) {
                                    areLeftsAllFacingSame = false;
                                }
                            }
                        }

                        // If lefts are facing same direction, get ahead/right for same direction to overlap
                        List< BlockPos > leftOverlapSignals = new ArrayList<>();
                        if ( areLeftsAllFacingSame ) {
                            for ( BlockPos signalPos : signalCircuit.getAheadSignals() ) {
                                IBlockState blockState = world.getBlockState( signalPos );
                                if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                                    EnumFacing signalFacingDirection = blockState.getValue( BlockHorizontal.FACING );
                                    if ( signalFacingDirection == encounteredFacingDirection ) {
                                        leftOverlapSignals.add( signalPos );
                                    }
                                }
                            }

                            for ( BlockPos signalPos : signalCircuit.getRightSignals() ) {
                                IBlockState blockState = world.getBlockState( signalPos );
                                if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                                    EnumFacing signalFacingDirection = blockState.getValue( BlockHorizontal.FACING );
                                    if ( signalFacingDirection == encounteredFacingDirection ) {
                                        leftOverlapSignals.add( signalPos );
                                    }
                                }
                            }
                        }

                        // Create state for ahead/right signals on green
                        TrafficSignalState circuitGreenState = new TrafficSignalState( 15, index );
                        if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                            circuitGreenState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                        }
                        circuitGreenState.addGreenSignals( signalCircuit.getAheadSignals() );
                        circuitGreenState.addGreenSignals( signalCircuit.getRightSignals() );
                        for ( int pedIndex = 0; pedIndex < signalCircuitList.size(); pedIndex++ ) {
                            if ( index != pedIndex ) {
                                circuitGreenState.addGreenSignals(
                                        signalCircuitList.get( pedIndex ).getPedestrianSignals() );
                            }
                        }
                        circuitGreenState.combine( allRedSignalState );
                        tempSignalStateList.add( circuitGreenState );

                        // Create state for ahead/right signals on yellow
                        TrafficSignalState circuitYellowState = new TrafficSignalState( 5, index );
                        TrafficSignalState circuitYellowStateOverlap = new TrafficSignalState( 5, index );
                        if ( leftOverlapSignals.size() > 0 ) {
                            circuitYellowStateOverlap.addGreenSignals( leftOverlapSignals );
                        }
                        if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                            circuitYellowState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                        }
                        circuitYellowState.addYellowSignals( signalCircuit.getAheadSignals() );
                        circuitYellowState.addYellowSignals( signalCircuit.getRightSignals() );
                        circuitYellowState.combine( allRedSignalState );
                        circuitYellowStateOverlap.combine( circuitYellowState );
                        tempSignalStateList.add( circuitYellowStateOverlap );

                        // Create state for ahead/right signals on red w/ overlap
                        if ( leftOverlapSignals.size() > 0 ) {
                            TrafficSignalState circuitRedOverlapState = new TrafficSignalState( 4, index );
                            TrafficSignalState circuitRedOverlapStateOverlap = new TrafficSignalState( 5, index );
                            if ( leftOverlapSignals.size() > 0 ) {
                                circuitRedOverlapStateOverlap.addGreenSignals( leftOverlapSignals );
                            }
                            if ( signalCircuit.getHybridLeftSignals().size() > 0 ) {
                                circuitRedOverlapState.addGreenSignals( signalCircuit.getHybridLeftSignals() );
                            }
                            circuitRedOverlapState.addRedSignals( signalCircuit.getAheadSignals() );
                            circuitRedOverlapState.addRedSignals( signalCircuit.getRightSignals() );
                            circuitRedOverlapState.combine( allRedSignalState );
                            circuitRedOverlapStateOverlap.combine( circuitRedOverlapState );
                            tempSignalStateList.add( circuitRedOverlapStateOverlap );
                        }

                        // Handle left signals
                        if ( signalCircuit.getHybridLeftSignals().size() > 0 ||
                                signalCircuit.getLeftSignals().size() > 0 ) {

                            // Create state for left turn signals on green
                            TrafficSignalState circuitLeftGreenState = new TrafficSignalState( 10, index );
                            circuitLeftGreenState.addOffSignals( signalCircuit.getHybridLeftSignals() );
                            circuitLeftGreenState.addGreenSignals( signalCircuit.getLeftSignals() );
                            circuitLeftGreenState.addGreenSignals( leftOverlapSignals );
                            circuitLeftGreenState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitLeftGreenState );

                            // Create state for left turn signals on yellow
                            TrafficSignalState circuitLeftYellowState = new TrafficSignalState( 5, index );
                            circuitLeftYellowState.addYellowSignals( signalCircuit.getHybridLeftSignals() );
                            circuitLeftYellowState.addYellowSignals( signalCircuit.getLeftSignals() );
                            circuitLeftYellowState.addYellowSignals( leftOverlapSignals );
                            circuitLeftYellowState.combine( allRedSignalState );
                            tempSignalStateList.add( circuitLeftYellowState );
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
                allRedSignalState.addYellowSignals( signalCircuit.getPedestrianSignals() );
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
                circuitGreenState.addYellowSignals( signalCircuit.getProtectedSignals() );
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

        // Mark applicable circuit as serviced
        try {
            if ( signalState.getActiveCircuit() != -1 ) {
                signalCircuitList.get( signalState.getActiveCircuit() ).markServiced( world );
            }
        }
        catch ( Exception e ) {
            System.err.println( "An error is preventing a traffic signal circuit from being marked as serviced! This " +
                                        "may affect signal prioritization behavior!" );
            e.printStackTrace();
        }
    }

    public String switchMode( World world ) {
        currentMode++;
        if ( currentMode > CURRENT_MODE_REQUESTABLE ) {
            currentMode = CURRENT_MODE_FLASH;
        }
        markDirty();
        bootSafe = false;
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
        else if ( currentMode == CURRENT_MODE_REQUESTABLE ) {
            modeName = "requestable/emergency access";
        }
        return modeName;
    }

    public void cycleSignals( boolean powered, World world ) {
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
            TrafficSignalState currState = signalStateList.get( currentPhase );
            if ( currentPhaseTime++ > currState.getLength() ) {

                currentPhase++;
                currentPhaseTime = 0;
                phaseChanged = true;
                // Check for prioritization
                try {
                    if ( currentMode == CURRENT_MODE_STANDARD && currState.getActiveCircuit() == -1 ) {
                        int topPriorityCircuit = getCircuitPriorityJumpIndex( world );
                        if ( topPriorityCircuit != -1 ) {
                            for ( int stateIndex = 0; stateIndex < signalStateList.size(); stateIndex++ ) {
                                if ( signalStateList.get( stateIndex ).getActiveCircuit() == topPriorityCircuit ) {
                                    currentPhase = stateIndex;
                                    break;
                                }
                            }
                        }
                    }
                    else if ( currentMode == CURRENT_MODE_STANDARD && currState.isAllRed() ) {
                        int topPriorityCircuit = getCircuitPriorityJumpIndex( world );
                        if ( topPriorityCircuit != currState.getActiveCircuit() && topPriorityCircuit != -1 ) {
                            for ( int stateIndex = 0; stateIndex < signalStateList.size(); stateIndex++ ) {
                                if ( signalStateList.get( stateIndex ).getActiveCircuit() == topPriorityCircuit ) {
                                    currentPhase = stateIndex;
                                    break;
                                }
                            }
                        }
                    }
                }
                catch ( Exception e ) {
                    System.err.println( "An error is preventing a traffic signal controller from performing circuit " +
                                                "prioritization! The signal controller will continue in standard mode " +
                                                "without sensors or prioritization." );
                    e.printStackTrace();
                }
            }
            else {
                // Cut green phase short if another circuit becomes top priority, extend if still top priority
                TrafficSignalState signalState = signalStateList.get( currentPhase );
                if ( currentMode == CURRENT_MODE_STANDARD &&
                        signalState.getYellowSignals().size() == 0 &&
                        !signalState.isAllRed() ) {
                    int topPriorityCircuit = getCircuitPriorityJumpIndex( world );
                    if ( topPriorityCircuit != signalState.getActiveCircuit() && topPriorityCircuit != -1 ) {
                        currentPhaseTime = signalState.getLength() + 1;
                    } /*else if ( topPriorityCircuit == signalState.getActiveCircuit() && topPriorityCircuit != -1 ) {
                        currentPhaseTime = 0;
                    }*/
                }
            }
            if ( currentPhase >= signalStateList.size() ) {
                currentPhase = 0;
                currentPhaseTime = 0;
                phaseChanged = true;
            }

            // Get signal state to apply (flash if not booted safely yet)
            TrafficSignalState signalStateToApply = signalStateList.get( currentPhase );
            if ( !bootSafe && currentMode == CURRENT_MODE_FLASH ) {
                bootSafe = true;
            }
            else if ( !bootSafe ) {
                // Get index of next signal state to apply
                int nextPhase = currentPhase + 1;
                if ( nextPhase >= signalStateList.size() ) {
                    nextPhase = 0;
                }

                // Get next signal state to check
                TrafficSignalState nextSignalStateToApply = signalStateList.get( nextPhase );
                List< BlockPos > nextSignalStateToApplyCircuitAheadSignals = signalCircuitList.get( 0 )
                                                                                              .getAheadSignals();
                if ( currentMode == CURRENT_MODE_STANDARD ) {
                    if ( nextSignalStateToApply.getActiveCircuit() == 0 &&
                            nextSignalStateToApplyCircuitAheadSignals.size() > 0 &&
                            nextSignalStateToApply.getGreenSignals()
                                                  .contains( nextSignalStateToApplyCircuitAheadSignals.get( 0 ) ) ) {
                        bootSafe = true;
                    }
                    else if ( nextSignalStateToApply.getActiveCircuit() == 0 &&
                            nextSignalStateToApplyCircuitAheadSignals.size() == 0 ) {
                        bootSafe = true;
                    }
                }
                else {
                    if ( nextSignalStateToApply.isAllRed() ) {
                        bootSafe = true;
                    }
                }

                signalStateToApply = signalFlashStateList.get( bootSafeFlash ? 1 : 0 );
                phaseChanged = true;
                bootSafeFlash = !bootSafeFlash;
            }

            // Update signals if phase changed
            if ( phaseChanged ) {
                updateSignals( signalStateToApply, powered );
                markDirty();
            }
        }
    }
}
