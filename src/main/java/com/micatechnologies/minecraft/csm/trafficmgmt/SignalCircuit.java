package com.micatechnologies.minecraft.csm.trafficmgmt;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Signal circuit implementation for Mica Minecraft CSM traffic signal controller systems.
 *
 * @author Mica Technologies/ah
 * @since 2020.8
 */
public class SignalCircuit
{
    /**
     * Delimiter used to separate lists in serialized signal circuit strings.
     */
    private static final String DELIMITER_SERIAL_LIST = "%";

    /**
     * Delimiter used to separate signal items in lists in serialized signal circuit strings.
     */
    private static final String DELIMITER_SERIAL_LIST_ITEMS = "&";

    /**
     * List of linked left signals. If a signal does not match the list type, or is no longer present, it will be
     * removed.
     */
    private final ArrayList< BlockPos > leftSignals = new ArrayList<>();

    /**
     * List of linked ahead/solid/ball signals. If a signal does not match the list type, or is no longer present, it
     * will be removed.
     */
    private final ArrayList< BlockPos > aheadSignals = new ArrayList<>();

    /**
     * List of linked right signals. If a signal does not match the list type, or is no longer present, it will be
     * removed.
     */
    private final ArrayList< BlockPos > rightSignals = new ArrayList<>();

    /**
     * List of linked protected ahead signals (bike, light rail). If a signal does not match the list type, or is no
     * longer present, it will be removed.
     */
    private final ArrayList< BlockPos > protectedAheadSignals = new ArrayList<>();

    /**
     * List of linked pedestrian signals. If a signal does not match the list type, or is no longer present, it will be
     * removed.
     */
    private final ArrayList< BlockPos > pedestrianSignals = new ArrayList<>();

    /**
     * List of linked traffic sensors. If a signal does not match the list type, or is no longer present, it will be
     * removed.
     */
    private final ArrayList< BlockPos > trafficSensors = new ArrayList<>();

    /**
     * Initialize a new signal circuit with no linked signals, sensors or accessories.
     */
    public SignalCircuit() {
        super();
    }

    /**
     * Initialize a new signal circuit with the linked signals specified by the supplied serialized signal circuit
     * string.
     *
     * @param serialized serialized signal circuit string
     */
    public SignalCircuit( String serialized ) {
        super();

        // Split serialized string to lists
        String[] serializedLists = serialized.split( DELIMITER_SERIAL_LIST );

        // Parse left signals list
        String[] leftSignalsList = serializedLists[ 0 ].split( DELIMITER_SERIAL_LIST_ITEMS );
        for ( String leftSignalPosStr : leftSignalsList ) {
            long leftSignalPos = Long.parseLong( leftSignalPosStr );
            leftSignals.add( BlockPos.fromLong( leftSignalPos ) );
        }

        // Parse ahead signals list
        String[] aheadSignalsList = serializedLists[ 1 ].split( DELIMITER_SERIAL_LIST_ITEMS );
        for ( String aheadSignalPosStr : aheadSignalsList ) {
            long aheadSignalPos = Long.parseLong( aheadSignalPosStr );
            aheadSignals.add( BlockPos.fromLong( aheadSignalPos ) );
        }

        // Parse right signals list
        String[] rightSignalsList = serializedLists[ 2 ].split( DELIMITER_SERIAL_LIST_ITEMS );
        for ( String rightSignalPosStr : rightSignalsList ) {
            long rightSignalPos = Long.parseLong( rightSignalPosStr );
            rightSignals.add( BlockPos.fromLong( rightSignalPos ) );
        }

        // Parse protected ahead signals list
        String[] protectedAheadSignalsList = serializedLists[ 3 ].split( DELIMITER_SERIAL_LIST_ITEMS );
        for ( String protectedAheadSignalPosStr : protectedAheadSignalsList ) {
            long protectedAheadSignalPos = Long.parseLong( protectedAheadSignalPosStr );
            protectedAheadSignals.add( BlockPos.fromLong( protectedAheadSignalPos ) );
        }

        // Parse pedestrian signals list
        String[] pedestrianSignalsList = serializedLists[ 4 ].split( DELIMITER_SERIAL_LIST_ITEMS );
        for ( String pedestrianSignalPosStr : pedestrianSignalsList ) {
            long pedestrianSignalPos = Long.parseLong( pedestrianSignalPosStr );
            pedestrianSignals.add( BlockPos.fromLong( pedestrianSignalPos ) );
        }

        // Parse traffic sensors list
        String[] trafficSensorsList = serializedLists[ 5 ].split( DELIMITER_SERIAL_LIST_ITEMS );
        for ( String trafficSensorPosStr : trafficSensorsList ) {
            long trafficSensorPos = Long.parseLong( trafficSensorPosStr );
            trafficSensors.add( BlockPos.fromLong( trafficSensorPos ) );
        }
    }

    /**
     * Converts the current signal circuit to a serialize signal circuit string that can be stored in Minecraft NBT
     * data.
     *
     * @return serialized signal circuit string
     */
    public String getSerializedString() {
        StringBuilder serializedStringBuilder = new StringBuilder();

        // Add left signals list
        Iterator< BlockPos > leftSignalIterator = leftSignals.iterator();
        while ( leftSignalIterator.hasNext() ) {
            BlockPos leftSignalPos = leftSignalIterator.next();
            serializedStringBuilder.append( leftSignalPos.toLong() );
            if ( leftSignalIterator.hasNext() ) {
                serializedStringBuilder.append( DELIMITER_SERIAL_LIST_ITEMS );
            }
        }
        serializedStringBuilder.append( DELIMITER_SERIAL_LIST );

        // Add ahead signals list
        Iterator< BlockPos > aheadSignalIterator = aheadSignals.iterator();
        while ( aheadSignalIterator.hasNext() ) {
            BlockPos aheadSignalPos = aheadSignalIterator.next();
            serializedStringBuilder.append( aheadSignalPos.toLong() );
            if ( aheadSignalIterator.hasNext() ) {
                serializedStringBuilder.append( DELIMITER_SERIAL_LIST_ITEMS );
            }
        }
        serializedStringBuilder.append( DELIMITER_SERIAL_LIST );

        // Add right signals list
        Iterator< BlockPos > rightSignalIterator = rightSignals.iterator();
        while ( rightSignalIterator.hasNext() ) {
            BlockPos rightSignalPos = rightSignalIterator.next();
            serializedStringBuilder.append( rightSignalPos.toLong() );
            if ( rightSignalIterator.hasNext() ) {
                serializedStringBuilder.append( DELIMITER_SERIAL_LIST_ITEMS );
            }
        }
        serializedStringBuilder.append( DELIMITER_SERIAL_LIST );

        // Add protected ahead signals list
        Iterator< BlockPos > protectedAheadSignalIterator = protectedAheadSignals.iterator();
        while ( protectedAheadSignalIterator.hasNext() ) {
            BlockPos protectedAheadSignalPos = protectedAheadSignalIterator.next();
            serializedStringBuilder.append( protectedAheadSignalPos.toLong() );
            if ( protectedAheadSignalIterator.hasNext() ) {
                serializedStringBuilder.append( DELIMITER_SERIAL_LIST_ITEMS );
            }
        }
        serializedStringBuilder.append( DELIMITER_SERIAL_LIST );

        // Add pedestrian signals list
        Iterator< BlockPos > pedestrianSignalIterator = pedestrianSignals.iterator();
        while ( pedestrianSignalIterator.hasNext() ) {
            BlockPos pedestrianSignalPos = pedestrianSignalIterator.next();
            serializedStringBuilder.append( pedestrianSignalPos.toLong() );
            if ( pedestrianSignalIterator.hasNext() ) {
                serializedStringBuilder.append( DELIMITER_SERIAL_LIST_ITEMS );
            }
        }
        serializedStringBuilder.append( DELIMITER_SERIAL_LIST );

        // Add traffic sensors list
        Iterator< BlockPos > trafficSensorIterator = trafficSensors.iterator();
        while ( trafficSensorIterator.hasNext() ) {
            BlockPos trafficSensorPos = trafficSensorIterator.next();
            serializedStringBuilder.append( trafficSensorPos.toLong() );
            if ( trafficSensorIterator.hasNext() ) {
                serializedStringBuilder.append( DELIMITER_SERIAL_LIST_ITEMS );
            }
        }

        return serializedStringBuilder.toString();
    }
}
