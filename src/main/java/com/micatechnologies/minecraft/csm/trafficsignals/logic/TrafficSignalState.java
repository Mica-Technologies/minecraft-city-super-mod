package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Object representing the state of a traffic signal system at any point in time, including signals which are 1. off, 2.
 * green/equivalent, 3. yellow/equivalent, or 4. red/equivalent.
 *
 * @author Mica Technologies/ahawk
 * @since 2021.3
 */
public class TrafficSignalState
{

    ///region: Signal State Lists

    /**
     * List of block positions of signals which are in the off state.
     */
    private final List< BlockPos > offSignals = new ArrayList<>();

    /**
     * List of block positions of signals which are in the green/equivalent state.
     */
    private final List< BlockPos > greenSignals = new ArrayList<>();

    /**
     * List of block positions of signals which are in the yellow/equivalent state.
     */
    private final List< BlockPos > yellowSignals = new ArrayList<>();

    /**
     * List of block positions of signals which are in the red/equivalent state.
     */
    private final List< BlockPos > redSignals = new ArrayList<>();

    ///endregion

    ///region: Signal State List Accessors

    /**
     * Gets the list of block positions of signals which are in the off state.
     *
     * @return list of block positions of signals in the off state
     */
    public List< BlockPos > getOffSignals() {
        return offSignals;
    }

    /**
     * Gets the list of block positions of signals which are in the green/equivalent state.
     *
     * @return list of block positions of signals in the green/equivalent state
     */
    public List< BlockPos > getGreenSignals() {
        return greenSignals;
    }

    /**
     * Gets the list of block positions of signals which are in the yellow/equivalent state.
     *
     * @return list of block positions of signals in the yellow/equivalent state
     */
    public List< BlockPos > getYellowSignals() {
        return yellowSignals;
    }

    /**
     * Gets the list of block positions of signals which are in the red/equivalent state.
     *
     * @return list of block positions of signals in the red/equivalent state
     */
    public List< BlockPos > getRedSignals() {
        return redSignals;
    }

    ///endregion

    ///region: Utility Methods

    /**
     * Combines the specified {@link TrafficSignalState} in to <code>this</code>.
     *
     * @param trafficSignalState {@link TrafficSignalState} to combine
     *
     * @return true if successful
     */
    public boolean combine( TrafficSignalState trafficSignalState ) {
        boolean successful;
        try {
            // Combine off signals list
            offSignals.addAll( trafficSignalState.getOffSignals() );

            // Combine green/equivalent signals list
            greenSignals.addAll( trafficSignalState.getGreenSignals() );

            // Combine yellow/equivalent signals list
            yellowSignals.addAll( trafficSignalState.getYellowSignals() );

            // Combine red/equivalent signals list
            redSignals.addAll( trafficSignalState.getRedSignals() );

            // Set successful flag to true
            successful = true;
        }
        catch ( Exception ignored ) {
            // Set successful flag to false
            successful = false;
        }
        return successful;
    }

    ///endregion

    ///region: Serialization (for NBT)

    /**
     * The character used to separate block positions in NBT serialization strings.
     */
    private static final String NBT_SERIALIZATION_ELEMENT_SEP_CHAR = "/";

    /**
     * The character used to separate signal state lists in NBT serialization strings.
     */
    private static final String NBT_SERIALIZATION_LIST_SEP_CHAR = ";";

    /**
     * The index of the off signals list in NBT serialization strings.
     */
    private static final int NBT_SERIALIZATION_OFF_SIGNAL_LIST_INDEX = 0;

    /**
     * The index of the green signals list in NBT serialization strings.
     */
    private static final int NBT_SERIALIZATION_GREEN_SIGNAL_LIST_INDEX = 1;

    /**
     * The index of the yellow signals list in NBT serialization strings.
     */
    private static final int NBT_SERIALIZATION_YELLOW_SIGNAL_LIST_INDEX = 2;

    /**
     * The index of the red signals list in NBT serialization strings.
     */
    private static final int NBT_SERIALIZATION_RED_SIGNAL_LIST_INDEX = 3;

    /**
     * The number of lists in NBT serialization strings.
     */
    private static final int NBT_SERIALIZATION_LIST_COUNT = 4;

    /**
     * Serializes the {@link TrafficSignalState} object to a string for NBT storage.
     *
     * @return {@link TrafficSignalState} serialized string
     */
    public String serialize() {
        // Serialize off signals list
        StringBuilder offSignalListString = new StringBuilder();
        for ( BlockPos offSignalBlockPos : offSignals ) {
            if ( offSignalListString.length() > 0 ) {
                offSignalListString.append( NBT_SERIALIZATION_ELEMENT_SEP_CHAR );
            }
            offSignalListString.append( offSignalBlockPos.toLong() );
        }

        // Serialize green signals list
        StringBuilder greenSignalListString = new StringBuilder();
        for ( BlockPos greenSignalBlockPos : greenSignals ) {
            if ( greenSignalListString.length() > 0 ) {
                greenSignalListString.append( NBT_SERIALIZATION_ELEMENT_SEP_CHAR );
            }
            greenSignalListString.append( greenSignalBlockPos.toLong() );
        }

        // Serialize yellow signals list
        StringBuilder yellowSignalListString = new StringBuilder();
        for ( BlockPos yellowSignalBlockPos : yellowSignals ) {
            if ( yellowSignalListString.length() > 0 ) {
                yellowSignalListString.append( NBT_SERIALIZATION_ELEMENT_SEP_CHAR );
            }
            yellowSignalListString.append( yellowSignalBlockPos.toLong() );
        }

        // Serialize red signals list
        StringBuilder redSignalListString = new StringBuilder();
        for ( BlockPos redSignalBlockPos : redSignals ) {
            if ( redSignalListString.length() > 0 ) {
                redSignalListString.append( NBT_SERIALIZATION_ELEMENT_SEP_CHAR );
            }
            redSignalListString.append( redSignalBlockPos.toLong() );
        }

        // Combine serialized list strings
        StringBuilder allListString = new StringBuilder();
        for ( int i = 0; i < NBT_SERIALIZATION_LIST_COUNT; i++ ) {
            if ( i == NBT_SERIALIZATION_OFF_SIGNAL_LIST_INDEX ) {
                allListString.append( offSignalListString.toString() );
            }
            else if ( i == NBT_SERIALIZATION_GREEN_SIGNAL_LIST_INDEX ) {
                allListString.append( greenSignalListString.toString() );
            }
            else if ( i == NBT_SERIALIZATION_YELLOW_SIGNAL_LIST_INDEX ) {
                allListString.append( yellowSignalListString.toString() );
            }
            else {
                allListString.append( redSignalListString.toString() );
            }
        }

        return allListString.toString();
    }

    /**
     * Deserializes the specified string in to a {@link TrafficSignalState} object.
     *
     * @param serializedTrafficSignalPhase serialized {@link TrafficSignalState} string
     *
     * @return {@link TrafficSignalState} deserialized from string
     */
    public static TrafficSignalState deserialize( String serializedTrafficSignalPhase ) {
        // Split serialized string by list
        String[] serializedListStrings = serializedTrafficSignalPhase.split( NBT_SERIALIZATION_LIST_SEP_CHAR );

        // Create traffic signal state object
        TrafficSignalState trafficSignalState = new TrafficSignalState();

        // Loop through each list and add all items to corresponding state object list
        for ( int i = 0; i < NBT_SERIALIZATION_LIST_COUNT; i++ ) {
            // Get serialized list items
            String serializedList = serializedListStrings[ i ];
            String[] serializedListItems = serializedList.split( NBT_SERIALIZATION_ELEMENT_SEP_CHAR );

            // Loop through each serialized list item
            for ( String serializedListItem : serializedListItems ) {
                long serializedListItemLong = Long.parseLong( serializedListItem );
                if ( i == NBT_SERIALIZATION_OFF_SIGNAL_LIST_INDEX ) {
                    trafficSignalState.offSignals.add( BlockPos.fromLong( serializedListItemLong ) );
                }
                else if ( i == NBT_SERIALIZATION_GREEN_SIGNAL_LIST_INDEX ) {
                    trafficSignalState.greenSignals.add( BlockPos.fromLong( serializedListItemLong ) );
                }
                else if ( i == NBT_SERIALIZATION_YELLOW_SIGNAL_LIST_INDEX ) {
                    trafficSignalState.yellowSignals.add( BlockPos.fromLong( serializedListItemLong ) );
                }
                else {
                    trafficSignalState.redSignals.add( BlockPos.fromLong( serializedListItemLong ) );
                }
            }
        }
        return trafficSignalState;
    }

    ///endregion
}