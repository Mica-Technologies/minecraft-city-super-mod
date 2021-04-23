package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.trafficsignals.AbstractBlockControllableSignal;
import jdk.nashorn.internal.ir.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrafficSignalCircuit
{
    /**
     * Character used to separate items in lists when performing serialization/deserialization.
     */
    private static final String ITEM_SEPARATOR_CHAR = ";";

    /**
     * Character used to separate lists when performing serialization/deserialization.
     */
    private static final String LIST_SEPARATOR_CHAR = "/";

    /**
     * Index of the hybrid left turn signals list when performing serialization/deserialization
     */
    private static final int HYBRID_LEFT_LIST_INDEX = 0;

    /**
     * Index of the left turn signals list when performing serialization/deserialization
     */
    private static final int LEFT_LIST_INDEX = 1;

    /**
     * Index of the ahead signals list when performing serialization/deserialization
     */
    private static final int AHEAD_LIST_INDEX = 2;

    /**
     * Index of the right turn signals list when performing serialization/deserialization
     */
    private static final int RIGHT_LIST_INDEX = 3;

    /**
     * Index of the pedestrian signals list when performing serialization/deserialization
     */
    private static final int PEDESTRIAN_LIST_INDEX = 4;

    /**
     * Index of the protected signals list when performing serialization/deserialization
     */
    private static final int PROTECTED_LIST_INDEX = 5;

    /**
     * Index of the sensors list when performing serialization/deserialization
     */
    private static final int SENSORS_LIST_INDEX = 6;

    /**
     * List of hybrid left turn signal block positions.
     */
    private final List< BlockPos > hybridLeftSignals = new ArrayList<>();

    /**
     * List of left turn signal block positions.
     */
    private final List< BlockPos > leftSignals = new ArrayList<>();

    /**
     * List of ahead signal block positions.
     */
    private final List< BlockPos > aheadSignals = new ArrayList<>();

    /**
     * List of right turn signal block positions.
     */
    private final List< BlockPos > rightSignals = new ArrayList<>();

    /**
     * List of pedestrian signal block positions.
     */
    private final List< BlockPos > pedestrianSignals = new ArrayList<>();

    /**
     * List of protected (bike, rail) signal block positions.
     */
    private final List< BlockPos > protectedSignals = new ArrayList<>();

    /**
     * List of sensor block positions.
     */
    private final List< BlockPos > sensors = new ArrayList<>();

    /**
     * Constructs a traffic signal circuit with no links.
     */
    public TrafficSignalCircuit() {
        // Do nothing
    }

    /**
     * Constructs a traffic signal circuit and populates its lists using the specified serialized traffic signal circuit
     * string.
     *
     * @param serialized serialized traffic signal circuit
     */
    public TrafficSignalCircuit( String serialized ) {
        // Split serialized string to lists
        String[] serializedLists = serialized.split( LIST_SEPARATOR_CHAR );

        // Process each serialized list
        for ( int serializedListIndex = 0; serializedListIndex < serializedLists.length; serializedListIndex++ ) {
            // Split list into individual items
            String[] serializedListItems = serializedLists[ serializedListIndex ].split( ITEM_SEPARATOR_CHAR );

            // Handle each item in list
            for ( String serializedListItem : serializedListItems ) {
                if ( serializedListItem.length() > 0 ) {
                    // Get block position
                    BlockPos listItemBlockPosition = BlockPos.fromLong( Long.parseLong( serializedListItem ) );

                    // Add to proper list
                    if ( serializedListIndex == HYBRID_LEFT_LIST_INDEX ) {
                        hybridLeftSignals.add( listItemBlockPosition );
                    }
                    else if ( serializedListIndex == LEFT_LIST_INDEX ) {
                        leftSignals.add( listItemBlockPosition );
                    }
                    else if ( serializedListIndex == AHEAD_LIST_INDEX ) {
                        aheadSignals.add( listItemBlockPosition );
                    }
                    else if ( serializedListIndex == RIGHT_LIST_INDEX ) {
                        rightSignals.add( listItemBlockPosition );
                    }
                    else if ( serializedListIndex == PEDESTRIAN_LIST_INDEX ) {
                        pedestrianSignals.add( listItemBlockPosition );
                    }
                    else if ( serializedListIndex == PROTECTED_LIST_INDEX ) {
                        protectedSignals.add( listItemBlockPosition );
                    }
                    else if ( serializedListIndex == SENSORS_LIST_INDEX ) {
                        sensors.add( listItemBlockPosition );
                    }
                }
            }
        }
    }

    /**
     * Returns a serialized string representation of this traffic signal circuit.
     *
     * @return serialized traffic signal circuit string
     */
    public String getSerialized() {
        StringBuilder serializedStringBuilder = new StringBuilder();

        // Add hybrid left signal list
        Iterator< BlockPos > iterator = hybridLeftSignals.iterator();
        while ( iterator.hasNext() ) {
            serializedStringBuilder.append( iterator.next().toLong() );
            if ( iterator.hasNext() ) {
                serializedStringBuilder.append( ITEM_SEPARATOR_CHAR );
            }
        }
        serializedStringBuilder.append( LIST_SEPARATOR_CHAR );

        // Add left signal list
        iterator = leftSignals.iterator();
        while ( iterator.hasNext() ) {
            serializedStringBuilder.append( iterator.next().toLong() );
            if ( iterator.hasNext() ) {
                serializedStringBuilder.append( ITEM_SEPARATOR_CHAR );
            }
        }
        serializedStringBuilder.append( LIST_SEPARATOR_CHAR );

        // Add ahead signal list
        iterator = aheadSignals.iterator();
        while ( iterator.hasNext() ) {
            serializedStringBuilder.append( iterator.next().toLong() );
            if ( iterator.hasNext() ) {
                serializedStringBuilder.append( ITEM_SEPARATOR_CHAR );
            }
        }
        serializedStringBuilder.append( LIST_SEPARATOR_CHAR );

        // Add right signal list
        iterator = rightSignals.iterator();
        while ( iterator.hasNext() ) {
            serializedStringBuilder.append( iterator.next().toLong() );
            if ( iterator.hasNext() ) {
                serializedStringBuilder.append( ITEM_SEPARATOR_CHAR );
            }
        }
        serializedStringBuilder.append( LIST_SEPARATOR_CHAR );

        // Add pedestrian signal list
        iterator = pedestrianSignals.iterator();
        while ( iterator.hasNext() ) {
            serializedStringBuilder.append( iterator.next().toLong() );
            if ( iterator.hasNext() ) {
                serializedStringBuilder.append( ITEM_SEPARATOR_CHAR );
            }
        }
        serializedStringBuilder.append( LIST_SEPARATOR_CHAR );

        // Add protected signal list
        iterator = protectedSignals.iterator();
        while ( iterator.hasNext() ) {
            serializedStringBuilder.append( iterator.next().toLong() );
            if ( iterator.hasNext() ) {
                serializedStringBuilder.append( ITEM_SEPARATOR_CHAR );
            }
        }
        serializedStringBuilder.append( LIST_SEPARATOR_CHAR );

        // Add sensor list
        iterator = sensors.iterator();
        while ( iterator.hasNext() ) {
            serializedStringBuilder.append( iterator.next().toLong() );
            if ( iterator.hasNext() ) {
                serializedStringBuilder.append( ITEM_SEPARATOR_CHAR );
            }
        }

        // Return built serialized string
        return serializedStringBuilder.toString();
    }

    public boolean areSignalsFacingSameDirection( World world ) {
        // Track encountered facing direction
        EnumFacing encounteredFacingDirection = null;

        // Loop through hybrid left signals
        for ( BlockPos signalPos : hybridLeftSignals ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                EnumFacing currentFacingDirection = blockState.getValue( BlockHorizontal.FACING );
                if ( encounteredFacingDirection == null ) {
                    encounteredFacingDirection = currentFacingDirection;
                }
                else if ( encounteredFacingDirection != currentFacingDirection ) {
                    return false;
                }
            }
        }

        // Loop through left signals
        for ( BlockPos signalPos : leftSignals ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                EnumFacing currentFacingDirection = blockState.getValue( BlockHorizontal.FACING );
                if ( encounteredFacingDirection == null ) {
                    encounteredFacingDirection = currentFacingDirection;
                }
                else if ( encounteredFacingDirection != currentFacingDirection ) {
                    return false;
                }
            }
        }

        // Loop through ahead signals
        for ( BlockPos signalPos : aheadSignals ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                EnumFacing currentFacingDirection = blockState.getValue( BlockHorizontal.FACING );
                if ( encounteredFacingDirection == null ) {
                    encounteredFacingDirection = currentFacingDirection;
                }
                else if ( encounteredFacingDirection != currentFacingDirection ) {
                    return false;
                }
            }
        }

        // Loop through right signals
        for ( BlockPos signalPos : rightSignals ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                EnumFacing currentFacingDirection = blockState.getValue( BlockHorizontal.FACING );
                if ( encounteredFacingDirection == null ) {
                    encounteredFacingDirection = currentFacingDirection;
                }
                else if ( encounteredFacingDirection != currentFacingDirection ) {
                    return false;
                }
            }
        }

        // Loop through protected signals
        for ( BlockPos signalPos : protectedSignals ) {
            IBlockState blockState = world.getBlockState( signalPos );
            if ( blockState.getBlock() instanceof AbstractBlockControllableSignal ) {
                EnumFacing currentFacingDirection = blockState.getValue( BlockHorizontal.FACING );
                if ( encounteredFacingDirection == null ) {
                    encounteredFacingDirection = currentFacingDirection;
                }
                else if ( encounteredFacingDirection != currentFacingDirection ) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns a boolean value (true/false) indicating whether or not the specified {@link BlockPos} is linked to this
     * traffic signal circuit.
     *
     * @param blockPos block position to check link
     *
     * @return true if linked to traffic signal circuit
     */
    public boolean isLinked( BlockPos blockPos ) {
        return hybridLeftSignals.contains( blockPos ) ||
                leftSignals.contains( blockPos ) ||
                aheadSignals.contains( blockPos ) ||
                rightSignals.contains( blockPos ) ||
                pedestrianSignals.contains( blockPos ) ||
                protectedSignals.contains( blockPos ) ||
                sensors.contains( blockPos );
    }

    /**
     * Removes any link to the specified {@link BlockPos} within the signal circuit.
     *
     * @param blockPos signal/sensor block position to unlink
     *
     * @return true is signal found and unlinked
     */
    public boolean unlink( BlockPos blockPos ) {
        boolean removed = false;
        if ( hybridLeftSignals.contains( blockPos ) ) {
            hybridLeftSignals.remove( blockPos );
            removed = true;
        }
        else if ( leftSignals.contains( blockPos ) ) {
            leftSignals.remove( blockPos );
            removed = true;
        }
        else if ( aheadSignals.contains( blockPos ) ) {
            aheadSignals.remove( blockPos );
            removed = true;
        }
        else if ( rightSignals.contains( blockPos ) ) {
            rightSignals.remove( blockPos );
            removed = true;
        }
        else if ( pedestrianSignals.contains( blockPos ) ) {
            pedestrianSignals.remove( blockPos );
            removed = true;
        }
        else if ( protectedSignals.contains( blockPos ) ) {
            protectedSignals.remove( blockPos );
            removed = true;
        }
        else if ( sensors.contains( blockPos ) ) {
            sensors.remove( blockPos );
            removed = true;
        }
        return removed;
    }

    /**
     * Links the specified {@link BlockPos} to the traffic signal circuit as a hybrid left turn signal.
     *
     * @param blockPos block position of signal to link
     */
    public void linkHybridLeftSignal( BlockPos blockPos ) {
        hybridLeftSignals.add( blockPos );
    }

    /**
     * Links the specified {@link BlockPos} to the traffic signal circuit as a left turn signal.
     *
     * @param blockPos block position of signal to link
     */
    public void linkLeftSignal( BlockPos blockPos ) {
        leftSignals.add( blockPos );
    }

    /**
     * Links the specified {@link BlockPos} to the traffic signal circuit as an ahead signal.
     *
     * @param blockPos block position of signal to link
     */
    public void linkAheadSignal( BlockPos blockPos ) {
        aheadSignals.add( blockPos );
    }

    /**
     * Links the specified {@link BlockPos} to the traffic signal circuit as a right turn signal.
     *
     * @param blockPos block position of signal to link
     */
    public void linkRightSignal( BlockPos blockPos ) {
        rightSignals.add( blockPos );
    }

    /**
     * Links the specified {@link BlockPos} to the traffic signal circuit as a pedestrian signal.
     *
     * @param blockPos block position of signal to link
     */
    public void linkPedestrianSignal( BlockPos blockPos ) {
        pedestrianSignals.add( blockPos );
    }

    /**
     * Links the specified {@link BlockPos} to the traffic signal circuit as a protected (bike, rail) signal.
     *
     * @param blockPos block position of signal to link
     */
    public void linkProtectedSignal( BlockPos blockPos ) {
        protectedSignals.add( blockPos );
    }

    /**
     * Links the specified {@link BlockPos} to the traffic signal circuit as a sensor.
     *
     * @param blockPos block position of sensor to link
     */
    public void linkSensor( BlockPos blockPos ) {
        sensors.add( blockPos );
    }

    /**
     * Returns the list of hybrid left turn signals in the circuit
     *
     * @return hybrid left turn signal list
     */
    public List< BlockPos > getHybridLeftSignals() {
        return hybridLeftSignals;
    }

    /**
     * Returns the list of  eft turn signals in the circuit
     *
     * @return left turn signal list
     */
    public List< BlockPos > getLeftSignals() {
        return leftSignals;
    }

    /**
     * Returns the list of ahead signals in the circuit
     *
     * @return ahead signal list
     */
    public List< BlockPos > getAheadSignals() {
        return aheadSignals;
    }

    /**
     * Returns the list of right turn signals in the circuit
     *
     * @return right turn signal list
     */
    public List< BlockPos > getRightSignals() {
        return rightSignals;
    }

    /**
     * Returns the list of pedestrian signals in the circuit
     *
     * @return pedestrian signal list
     */
    public List< BlockPos > getPedestrianSignals() {
        return pedestrianSignals;
    }

    /**
     * Returns the list of protected ahead signals in the circuit
     *
     * @return protected ahead signal list
     */
    public List< BlockPos > getProtectedSignals() {
        return protectedSignals;
    }

    /**
     * Returns the list of sensors in the circuit
     *
     * @return sensors list
     */
    public List< BlockPos > getSensors() {
        return sensors;
    }

}
