package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.SerializationUtils;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalSensor;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalTickableRequester;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal.SIGNAL_SIDE;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The class representation of a traffic signal controller circuit which stores the following signal types:
 * <ul>
 *     <li>Flashing Left</li>
 *     <li>Flashing Right</li>
 *     <li>Standard Left</li>
 *     <li>Standard Right</li>
 *     <li>Through</li>
 *     <li>Pedestrian</li>
 *     <li>Pedestrian Beacon</li>
 *     <li>Pedestrian Button</li>
 *     <li>Protected</li>
 *     <li>Sensors</li>
 * </ul> in a format which can be easily serialized and deserialized as Minecraft NBT data.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see #fromNBT(NBTTagCompound)
 * @see #toNBT()
 * @since 2023.2.0
 */
public class TrafficSignalControllerCircuit
{
    //region: Static/Constant Fields

    /**
     * The key used to store the list of flashing left signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_FLASHING_LEFT_SIGNAL_LIST = "flashingLeftSignalList";

    /**
     * The key used to store the list of flashing right signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_FLASHING_RIGHT_SIGNAL_LIST = "flashingRightSignalList";

    /**
     * The key used to store the list of standard left signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_STANDARD_LEFT_SIGNAL_LIST = "standardLeftSignalList";

    /**
     * The key used to store the list of standard right signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_STANDARD_RIGHT_SIGNAL_LIST = "standardRightSignalList";

    /**
     * The key used to store the list of through signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_THROUGH_SIGNAL_LIST = "throughSignalList";

    /**
     * The key used to store the list of pedestrian signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_PEDESTRIAN_SIGNAL_LIST = "pedestrianSignalList";

    /**
     * The key used to store the list of pedestrian beacon signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_PEDESTRIAN_BEACON_SIGNAL_LIST = "pedestrianBeaconSignalList";

    /**
     * The key used to store the list of pedestrian accessory signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_PEDESTRIAN_ACCESSORY_SIGNAL_LIST = "pedestrianAccessorySignalList";

    /**
     * The key used to store the list of protected signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_PROTECTED_SIGNAL_LIST = "protectedSignalList";

    /**
     * The key used to store the list of sensor {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_SENSOR_LIST = "sensorList";

    //endregion

    //region: Instance Fields

    /**
     * The list of {@link BlockPos}es of flashing left signals in the circuit.
     *
     * @since 1.0
     */
    private final List< BlockPos > flashingLeftSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of flashing right signals in the circuit.
     *
     * @since 1.0
     */
    private final List< BlockPos > flashingRightSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of standard left signals in the circuit.
     *
     * @since 1.0
     */
    private final List< BlockPos > leftSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of standard right signals in the circuit.
     *
     * @since 1.0
     */
    private final List< BlockPos > rightSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of through/solid/ball signals in the circuit.
     *
     * @since 1.0
     */
    private final List< BlockPos > throughSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of pedestrian signals in the circuit.
     *
     * @since 1.0
     */
    private final List< BlockPos > pedestrianSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of pedestrian beacon signals in the circuit.
     *
     * @since 1.0
     */
    private final List< BlockPos > pedestrianBeaconSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of pedestrian accessory signals in the circuit.
     *
     * @since 1.0
     */
    private final List< BlockPos > pedestrianAccessorySignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of protected signals in the circuit.
     *
     * @since 1.0
     */
    private final List< BlockPos > protectedSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of sensors in the circuit.
     *
     * @since 1.0
     */
    private final ArrayList< BlockPos > sensors = new ArrayList<>();

    //endregion

    //region: Instance Methods

    /**
     * Gets the list of {@link BlockPos}es of flashing left signals in the circuit.
     *
     * @return The list of {@link BlockPos}es of flashing left signals in the circuit.
     *
     * @see #flashingLeftSignals
     * @since 1.0
     */
    public List< BlockPos > getFlashingLeftSignals() {
        return flashingLeftSignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of flashing right signals in the circuit.
     *
     * @return The list of {@link BlockPos}es of flashing right signals in the circuit.
     *
     * @see #flashingRightSignals
     * @since 1.0
     */
    public List< BlockPos > getFlashingRightSignals() {
        return flashingRightSignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of standard left signals in the circuit.
     *
     * @return The list of {@link BlockPos}es of standard left signals in the circuit.
     *
     * @see #leftSignals
     * @since 1.0
     */
    public List< BlockPos > getLeftSignals() {
        return leftSignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of standard right signals in the circuit.
     *
     * @return The list of {@link BlockPos}es of standard right signals in the circuit.
     *
     * @see #rightSignals
     * @since 1.0
     */
    public List< BlockPos > getRightSignals() {
        return rightSignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of through/solid/ball signals in the circuit.
     *
     * @return The list of {@link BlockPos}es of through/solid/ball signals in the circuit.
     *
     * @see #throughSignals
     * @since 1.0
     */
    public List< BlockPos > getThroughSignals() {
        return throughSignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of pedestrian signals in the circuit.
     *
     * @return The list of {@link BlockPos}es of pedestrian signals in the circuit.
     *
     * @see #pedestrianSignals
     * @since 1.0
     */
    public List< BlockPos > getPedestrianSignals() {
        return pedestrianSignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of pedestrian beacon signals in the circuit.
     *
     * @return The list of {@link BlockPos}es of pedestrian beacon signals in the circuit.
     *
     * @see #pedestrianBeaconSignals
     * @since 1.0
     */
    public List< BlockPos > getPedestrianBeaconSignals() {
        return pedestrianBeaconSignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of pedestrian accessory signals in the circuit.
     *
     * @return The list of {@link BlockPos}es of pedestrian accessory signals in the circuit.
     *
     * @see #pedestrianAccessorySignals
     * @since 1.0
     */
    public List< BlockPos > getPedestrianAccessorySignals() {
        return pedestrianAccessorySignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of protected signals in the circuit.
     *
     * @return The list of {@link BlockPos}es of protected signals in the circuit.
     *
     * @see #protectedSignals
     * @since 1.0
     */
    public List< BlockPos > getProtectedSignals() {
        return protectedSignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of sensors in the circuit.
     *
     * @return The list of {@link BlockPos}es of sensors in the circuit.
     *
     * @see #sensors
     * @since 1.0
     */
    public List< BlockPos > getSensors() {
        return sensors;
    }

    /**
     * Gets the count of waiting entities at all sensors in the circuit.
     *
     * @return The count of waiting entities at all sensors in the circuit.
     *
     * @since 1.0
     */
    public TrafficSignalSensorSummary getSensorsWaitingSummary( World world ) {
        // Create variables to track counts
        int standardAll = 0, leftAll = 0, protectedAll = 0;
        int standardEast = 0, leftEast = 0, protectedEast = 0;
        int standardWest = 0, leftWest = 0, protectedWest = 0;
        int standardNorth = 0, leftNorth = 0, protectedNorth = 0;
        int standardSouth = 0, leftSouth = 0, protectedSouth = 0;

        // Loop through each sensor and count appropriately
        for ( BlockPos sensorPos : sensors ) {
            TileEntity tileEntity = world.getTileEntity( sensorPos );
            if ( tileEntity instanceof TileEntityTrafficSignalSensor ) {
                TileEntityTrafficSignalSensor tileEntityTrafficSignalSensor
                        = ( TileEntityTrafficSignalSensor ) tileEntity;
                int standardCount = tileEntityTrafficSignalSensor.scanEntities();
                int leftCount = tileEntityTrafficSignalSensor.scanLeftEntities();
                int protectedCount = tileEntityTrafficSignalSensor.scanProtectedEntities();
                standardAll += standardCount;
                leftAll += leftCount;
                protectedAll += protectedCount;

                // Get the direction of the sensor
                IBlockState blockState = world.getBlockState( sensorPos );
                EnumFacing sensorFacingDirection = blockState.getValue( BlockHorizontal.FACING );
                if ( sensorFacingDirection == EnumFacing.EAST ) {
                    standardEast += standardCount;
                    leftEast += leftCount;
                    protectedEast += protectedCount;
                }
                else if ( sensorFacingDirection == EnumFacing.WEST ) {
                    standardWest += standardCount;
                    leftWest += leftCount;
                    protectedWest += protectedCount;
                }
                else if ( sensorFacingDirection == EnumFacing.NORTH ) {
                    standardNorth += standardCount;
                    leftNorth += leftCount;
                    protectedNorth += protectedCount;
                }
                else if ( sensorFacingDirection == EnumFacing.SOUTH ) {
                    standardSouth += standardCount;
                    leftSouth += leftCount;
                    protectedSouth += protectedCount;
                }
            }
        }

        // Return new summary object
        return new TrafficSignalSensorSummary( standardAll, standardEast, standardWest, standardNorth, standardSouth,
                                               leftAll, leftEast, leftWest, leftNorth, leftSouth, protectedAll,
                                               protectedEast, protectedWest, protectedNorth, protectedSouth );
    }

    /**
     * Gets the total request count from all pedestrian accessories in the circuit.
     *
     * @return The total request count from all pedestrian accessories in the circuit.
     *
     * @since 1.0
     */
    public int getPedestrianAccessoriesRequestCount( World world ) {
        int count = 0;
        for ( BlockPos accessoryPos : pedestrianAccessorySignals ) {
            TileEntity tileEntity = world.getTileEntity( accessoryPos );
            if ( tileEntity instanceof TileEntityTrafficSignalTickableRequester ) {
                TileEntityTrafficSignalTickableRequester tileEntityTrafficSignalTickableRequester
                        = ( TileEntityTrafficSignalTickableRequester ) tileEntity;
                count += tileEntityTrafficSignalTickableRequester.getRequestCount();
            }
        }
        return count;
    }

    /**
     * Resets the request count for all pedestrian accessories in the circuit.
     *
     * @since 1.0
     */
    public void resetPedestrianAccessoriesRequestCount( World world ) {
        for ( BlockPos accessoryPos : pedestrianAccessorySignals ) {
            TileEntity tileEntity = world.getTileEntity( accessoryPos );
            if ( tileEntity instanceof TileEntityTrafficSignalTickableRequester ) {
                TileEntityTrafficSignalTickableRequester tileEntityTrafficSignalTickableRequester
                        = ( TileEntityTrafficSignalTickableRequester ) tileEntity;
                tileEntityTrafficSignalTickableRequester.resetRequestCount();
            }
        }
    }

    /**
     * Executes the specified {@link java.util.function.Consumer} for each signal device linked to this circuit.
     *
     * @param action The {@link java.util.function.Consumer} to execute for each signal device linked to this circuit.
     *
     * @since 1.0
     */
    public void forAllSignals( java.util.function.Consumer< ? super BlockPos > action ) {
        throughSignals.forEach( action );
        leftSignals.forEach( action );
        rightSignals.forEach( action );
        flashingLeftSignals.forEach( action );
        flashingRightSignals.forEach( action );
        pedestrianSignals.forEach( action );
        pedestrianBeaconSignals.forEach( action );
        pedestrianAccessorySignals.forEach( action );
        protectedSignals.forEach( action );
    }

    /**
     * Powers off all signal devices linked to this circuit in the specified {@link World}.
     *
     * @param world The {@link World} in which the signal devices are located.
     */
    public void powerOffAllSignals( World world ) {
        forAllSignals( signal -> AbstractBlockControllableSignal.changeSignalColor( world, signal,
                                                                                    AbstractBlockControllableSignal.SIGNAL_OFF ) );
    }

    /**
     * Gets a boolean indicating whether the specified device {@link BlockPos} is linked to this circuit.
     *
     * @param devicePos The {@link BlockPos} of the device to check.
     *
     * @return True if the specified device {@link BlockPos} is linked to this circuit, false otherwise.
     *
     * @since 1.0
     */
    public boolean isDeviceLinked( BlockPos devicePos ) {
        return throughSignals.contains( devicePos ) ||
                leftSignals.contains( devicePos ) ||
                rightSignals.contains( devicePos ) ||
                flashingLeftSignals.contains( devicePos ) ||
                flashingRightSignals.contains( devicePos ) ||
                pedestrianSignals.contains( devicePos ) ||
                pedestrianBeaconSignals.contains( devicePos ) ||
                pedestrianAccessorySignals.contains( devicePos ) ||
                protectedSignals.contains( devicePos ) ||
                sensors.contains( devicePos );
    }

    /**
     * Links the specified signal {@link BlockPos} to this circuit as a flashing left signal
     *
     * @param signalPos The {@link BlockPos} of the signal to link.
     *
     * @since 1.0
     */
    public boolean linkFlashingLeftSignal( BlockPos signalPos ) {
        return flashingLeftSignals.add( signalPos );
    }

    /**
     * Links each signal {@link BlockPos} in the specified list to this circuit as a flashing left signal.
     *
     * @param signalPoses The list of {@link BlockPos}es of signals to link.
     *
     * @since 1.0
     */
    public boolean linkFlashingLeftSignals( List< BlockPos > signalPoses ) {
        return flashingLeftSignals.addAll( signalPoses );
    }

    /**
     * Links the specified signal {@link BlockPos} to this circuit as a flashing right signal
     *
     * @param signalPos The {@link BlockPos} of the signal to link.
     *
     * @since 1.0
     */
    public boolean linkFlashingRightSignal( BlockPos signalPos ) {
        return flashingRightSignals.add( signalPos );
    }

    /**
     * Links each signal {@link BlockPos} in the specified list to this circuit as a flashing right signal.
     *
     * @param signalPoses The list of {@link BlockPos}es of signals to link.
     *
     * @since 1.0
     */
    public boolean linkFlashingRightSignals( List< BlockPos > signalPoses ) {
        return flashingRightSignals.addAll( signalPoses );
    }

    /**
     * Links the specified signal {@link BlockPos} to this circuit as a standard left signal
     *
     * @param signalPos The {@link BlockPos} of the signal to link.
     *
     * @since 1.0
     */
    public boolean linkLeftSignal( BlockPos signalPos ) {
        return leftSignals.add( signalPos );
    }

    /**
     * Links each signal {@link BlockPos} in the specified list to this circuit as a standard left signal.
     *
     * @param signalPoses The list of {@link BlockPos}es of signals to link.
     *
     * @since 1.0
     */
    public boolean linkLeftSignals( List< BlockPos > signalPoses ) {
        return leftSignals.addAll( signalPoses );
    }

    /**
     * Links the specified signal {@link BlockPos} to this circuit as a standard right signal
     *
     * @param signalPos The {@link BlockPos} of the signal to link.
     *
     * @since 1.0
     */
    public boolean linkRightSignal( BlockPos signalPos ) {
        return rightSignals.add( signalPos );
    }

    /**
     * Links each signal {@link BlockPos} in the specified list to this circuit as a standard right signal.
     *
     * @param signalPoses The list of {@link BlockPos}es of signals to link.
     *
     * @since 1.0
     */
    public boolean linkRightSignals( List< BlockPos > signalPoses ) {
        return rightSignals.addAll( signalPoses );
    }

    /**
     * Links the specified signal {@link BlockPos} to this circuit as a through/solid/ball signal
     *
     * @param signalPos The {@link BlockPos} of the signal to link.
     *
     * @since 1.0
     */
    public boolean linkThroughSignal( BlockPos signalPos ) {
        return throughSignals.add( signalPos );
    }

    /**
     * Links each signal {@link BlockPos} in the specified list to this circuit as a through/solid/ball signal.
     *
     * @param signalPoses The list of {@link BlockPos}es of signals to link.
     *
     * @since 1.0
     */
    public boolean linkThroughSignals( List< BlockPos > signalPoses ) {
        return throughSignals.addAll( signalPoses );
    }

    /**
     * Links the specified signal {@link BlockPos} to this circuit as a pedestrian signal
     *
     * @param signalPos The {@link BlockPos} of the signal to link.
     *
     * @since 1.0
     */
    public boolean linkPedestrianSignal( BlockPos signalPos ) {
        return pedestrianSignals.add( signalPos );
    }

    /**
     * Links each signal {@link BlockPos} in the specified list to this circuit as a pedestrian signal.
     *
     * @param signalPoses The list of {@link BlockPos}es of signals to link.
     *
     * @since 1.0
     */
    public boolean linkPedestrianSignals( List< BlockPos > signalPoses ) {
        return pedestrianSignals.addAll( signalPoses );
    }

    /**
     * Links the specified signal {@link BlockPos} to this circuit as a pedestrian beacon signal
     *
     * @param signalPos The {@link BlockPos} of the signal to link.
     *
     * @since 1.0
     */
    public boolean linkPedestrianBeaconSignal( BlockPos signalPos ) {
        return pedestrianBeaconSignals.add( signalPos );
    }

    /**
     * Links each signal {@link BlockPos} in the specified list to this circuit as a pedestrian beacon signal.
     *
     * @param signalPoses The list of {@link BlockPos}es of signals to link.
     *
     * @since 1.0
     */
    public boolean linkPedestrianBeaconSignals( List< BlockPos > signalPoses ) {
        return pedestrianBeaconSignals.addAll( signalPoses );
    }

    /**
     * Links the specified signal {@link BlockPos} to this circuit as a pedestrian accessory signal.
     *
     * @param buttonPos The {@link BlockPos} of the signal to link.
     *
     * @since 1.0
     */
    public boolean linkPedestrianAccessorySignal( BlockPos buttonPos ) {
        return pedestrianAccessorySignals.add( buttonPos );
    }

    /**
     * Links each signal {@link BlockPos} in the specified list to this circuit as a pedestrian accessory signal.
     *
     * @param buttonPoses The list of {@link BlockPos}es of signal to link.
     *
     * @since 1.0
     */
    public boolean linkPedestrianAccessorySignals( List< BlockPos > buttonPoses ) {
        return pedestrianAccessorySignals.addAll( buttonPoses );
    }

    /**
     * Links the specified signal {@link BlockPos} to this circuit as a protected signal
     *
     * @param signalPos The {@link BlockPos} of the signal to link.
     *
     * @since 1.0
     */
    public boolean linkProtectedSignal( BlockPos signalPos ) {
        return protectedSignals.add( signalPos );
    }

    /**
     * Links each signal {@link BlockPos} in the specified list to this circuit as a protected signal.
     *
     * @param signalPoses The list of {@link BlockPos}es of signals to link.
     *
     * @since 1.0
     */
    public boolean linkProtectedSignals( List< BlockPos > signalPoses ) {
        return protectedSignals.addAll( signalPoses );
    }

    /**
     * Links the specified sensor {@link BlockPos} to this circuit
     *
     * @param sensorPos The {@link BlockPos} of the sensor to link.
     *
     * @since 1.0
     */
    public boolean linkSensor( BlockPos sensorPos ) {
        return sensors.add( sensorPos );
    }

    /**
     * Links each sensor {@link BlockPos} in the specified list to this circuit.
     *
     * @param sensorPoses The list of {@link BlockPos}es of sensors to link.
     *
     * @since 1.0
     */
    public boolean linkSensors( List< BlockPos > sensorPoses ) {
        return sensors.addAll( sensorPoses );
    }

    /**
     * Tries to link the devices in the specified device {@link BlockPos} list to this circuit (for migration from
     * previous NBT data format). If the device is not an {@link AbstractBlockControllableSignal} or
     * {@link AbstractBlockTrafficSignalSensor} then it is ignored. If specified boolean {@code isSensorsList} is true,
     * then the devices will be linked as sensors, otherwise they will be linked as signals. Signal lists can be any
     * type of signal, but sensor lists must be sensors.
     *
     * @param linkWorld     The {@link World} to link the devices in.
     * @param devicePosList The list of {@link BlockPos}es of devices to link.
     * @param isSensorsList Whether the list is a list of sensors. If true, the devices will be linked as sensors,
     *                      otherwise they will be linked as signals.
     *
     * @since 1.0
     */
    public void tryLinkDevicesMigration( World linkWorld, List< BlockPos > devicePosList, boolean isSensorsList ) {
        devicePosList.forEach( signalPos -> {
            try {
                if ( signalPos != null ) {
                    if ( isSensorsList ) {
                        linkSensor( signalPos );
                    }
                    else {
                        AbstractBlockControllableSignal controllableSignal
                                = AbstractBlockControllableSignal.getSignalBlockInstanceOrNull( linkWorld, signalPos );
                        if ( controllableSignal != null ) {
                            SIGNAL_SIDE signalSide = controllableSignal.getSignalSide( linkWorld, signalPos );
                            boolean linked = linkDevice( signalPos, signalSide );
                            if ( linked ) {
                                System.out.println(
                                        "Linked device at " + signalPos + " to circuit on side " + signalSide.name() );
                            }
                            else {
                                System.err.println( "Failed to link device (tryLinkDevices) at " +
                                                            signalPos +
                                                            " to " +
                                                            "circuit!" );
                            }
                        }
                        else {
                            System.err.println( "Failed to link device (tryLinkDevices) at " +
                                                        signalPos +
                                                        " to " +
                                                        "circuit because it was null!" );
                        }
                    }
                }
            }
            catch ( Exception e ) {
                System.err.println( "Error linking device (tryLinkDevices) at " + signalPos + " to circuit!" );
                e.printStackTrace();
            }
        } );
    }

    /**
     * Links the specified device {@link BlockPos} to this circuit with the specified {@link SIGNAL_SIDE}
     *
     * @param devicePos The {@link BlockPos} of the device to link.
     * @param side      The {@link SIGNAL_SIDE} of the device to link.
     *
     * @since 1.0
     */
    public boolean linkDevice( BlockPos devicePos, SIGNAL_SIDE side ) {
        if ( side == SIGNAL_SIDE.LEFT ) {
            return linkLeftSignal( devicePos );
        }
        else if ( side == SIGNAL_SIDE.RIGHT ) {
            return linkRightSignal( devicePos );
        }
        else if ( side == SIGNAL_SIDE.THROUGH ) {
            return linkThroughSignal( devicePos );
        }
        else if ( side == SIGNAL_SIDE.PEDESTRIAN ) {
            return linkPedestrianSignal( devicePos );
        }
        else if ( side == SIGNAL_SIDE.PEDESTRIAN_BEACON ) {
            return linkPedestrianBeaconSignal( devicePos );
        }
        else if ( side == SIGNAL_SIDE.PEDESTRIAN_ACCESSORY ) {
            return linkPedestrianAccessorySignal( devicePos );
        }
        else if ( side == SIGNAL_SIDE.PROTECTED ) {
            return linkProtectedSignal( devicePos );
        }
        else if ( side == SIGNAL_SIDE.FLASHING_LEFT ) {
            return linkFlashingLeftSignal( devicePos );
        }
        else if ( side == SIGNAL_SIDE.FLASHING_RIGHT ) {
            return linkFlashingRightSignal( devicePos );
        }
        else if ( side == SIGNAL_SIDE.NA_SENSOR ) {
            return linkSensor( devicePos );
        }
        return false;
    }

    /**
     * Unlinks the specified device {@link BlockPos} from this circuit, if it is linked.
     *
     * @param blockPos The {@link BlockPos} of the device to unlink.
     *
     * @return True if the device was unlinked, false otherwise.
     *
     * @since 1.0
     */
    public boolean unlinkDevice( BlockPos blockPos ) {
        boolean removed = false;
        if ( blockPos != null ) {
            removed = throughSignals.remove( blockPos );
            removed |= leftSignals.remove( blockPos );
            removed |= rightSignals.remove( blockPos );
            removed |= flashingLeftSignals.remove( blockPos );
            removed |= flashingRightSignals.remove( blockPos );
            removed |= pedestrianSignals.remove( blockPos );
            removed |= pedestrianBeaconSignals.remove( blockPos );
            removed |= pedestrianAccessorySignals.remove( blockPos );
            removed |= protectedSignals.remove( blockPos );
            removed |= sensors.remove( blockPos );
        }
        return removed;
    }

    /**
     * Gets the size of this circuit in terms of the number of connected devices.
     *
     * @return The size of this circuit.
     *
     * @since 1.0
     */
    public int getSize() {
        return flashingLeftSignals.size() +
                flashingRightSignals.size() +
                leftSignals.size() +
                rightSignals.size() +
                throughSignals.size() +
                pedestrianSignals.size() +
                pedestrianBeaconSignals.size() +
                pedestrianAccessorySignals.size() +
                protectedSignals.size() +
                sensors.size();
    }

    /**
     * Gets a boolean indicating whether all the signals in this circuit are facing the same direction.
     *
     * @param world The {@link World} to check the signals in.
     *
     * @return {@code true} if all the signals in this circuit are facing the same direction, {@code false} otherwise.
     *
     * @since 1.0
     */
    public boolean areSignalsFacingSameDirection( World world ) {
        // Track encountered facing direction
        EnumFacing encounteredFacingDirection = null;

        // Loop through all flashing left signals
        for ( BlockPos signalPos : flashingLeftSignals ) {
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

        // Loop through all flashing right signals
        for ( BlockPos signalPos : flashingRightSignals ) {
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

        // Loop through all left signals
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

        // Loop through all right signals
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

        // Loop through all through signals
        for ( BlockPos signalPos : throughSignals ) {
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

        // Loop through all protected signals
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

        // Loop through all pedestrian beacon signals
        for ( BlockPos signalPos : pedestrianBeaconSignals ) {
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

    //endregion

    //region: Serialization Methods

    /**
     * Gets an {@link NBTTagCompound} containing the data for this circuit in NBT format.
     *
     * @return The {@link NBTTagCompound} containing the data for this circuit in NBT format.
     *         <p>
     *         The returned {@link NBTTagCompound} can be used to reconstruct the {@link TrafficSignalControllerCircuit}
     *         object.
     *         </p>
     *
     * @see #fromNBT(NBTTagCompound)
     * @since 1.0
     */
    public NBTTagCompound toNBT() {
        // Create the compound
        NBTTagCompound compound = new NBTTagCompound();

        // Serialize flashing left signals
        compound.setTag( NBT_KEY_FLASHING_LEFT_SIGNAL_LIST,
                         SerializationUtils.getBlockPosNBTArrayFromBlockPosList( flashingLeftSignals ) );

        // Serialize flashing right signals
        compound.setTag( NBT_KEY_FLASHING_RIGHT_SIGNAL_LIST,
                         SerializationUtils.getBlockPosNBTArrayFromBlockPosList( flashingRightSignals ) );

        // Serialize standard left signals
        compound.setTag( NBT_KEY_STANDARD_LEFT_SIGNAL_LIST,
                         SerializationUtils.getBlockPosNBTArrayFromBlockPosList( leftSignals ) );

        // Serialize standard right signals
        compound.setTag( NBT_KEY_STANDARD_RIGHT_SIGNAL_LIST,
                         SerializationUtils.getBlockPosNBTArrayFromBlockPosList( rightSignals ) );

        // Serialize through/solid/ball signals
        compound.setTag( NBT_KEY_THROUGH_SIGNAL_LIST,
                         SerializationUtils.getBlockPosNBTArrayFromBlockPosList( throughSignals ) );

        // Serialize pedestrian signals
        compound.setTag( NBT_KEY_PEDESTRIAN_SIGNAL_LIST,
                         SerializationUtils.getBlockPosNBTArrayFromBlockPosList( pedestrianSignals ) );

        // Serialize pedestrian beacon signals
        compound.setTag( NBT_KEY_PEDESTRIAN_BEACON_SIGNAL_LIST,
                         SerializationUtils.getBlockPosNBTArrayFromBlockPosList( pedestrianBeaconSignals ) );

        // Serialize pedestrian accessory signals
        compound.setTag( NBT_KEY_PEDESTRIAN_ACCESSORY_SIGNAL_LIST,
                         SerializationUtils.getBlockPosNBTArrayFromBlockPosList( pedestrianAccessorySignals ) );

        // Serialize protected signals
        compound.setTag( NBT_KEY_PROTECTED_SIGNAL_LIST,
                         SerializationUtils.getBlockPosNBTArrayFromBlockPosList( protectedSignals ) );

        // Serialize sensors
        compound.setTag( NBT_KEY_SENSOR_LIST, SerializationUtils.getBlockPosNBTArrayFromBlockPosList( sensors ) );

        // Return the compound
        return compound;
    }

    /**
     * Gets a {@link TrafficSignalControllerCircuit} object from the given {@link NBTTagCompound}.
     *
     * @param nbt The {@link NBTTagCompound} containing the data for the {@link TrafficSignalControllerCircuit} object
     *            in NBT format.
     *
     * @return The {@link TrafficSignalControllerCircuit} object from the given {@link NBTTagCompound}.
     *
     * @throws IllegalArgumentException If the given {@link NBTTagCompound} is null.
     * @see #toNBT()
     * @since 1.0
     */
    public static TrafficSignalControllerCircuit fromNBT( NBTTagCompound nbt ) {
        // Validate the NBT
        if ( nbt == null ) {
            throw new IllegalArgumentException( "The NBT cannot be null." );
        }

        // Create the circuit
        TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();

        // Deserialize flashing left signals
        circuit.flashingLeftSignals.addAll( SerializationUtils.getBlockPosListFromBlockPosNBTArray(
                nbt.getTag( NBT_KEY_FLASHING_LEFT_SIGNAL_LIST ) ) );

        // Deserialize flashing right signals
        circuit.flashingRightSignals.addAll( SerializationUtils.getBlockPosListFromBlockPosNBTArray(
                nbt.getTag( NBT_KEY_FLASHING_RIGHT_SIGNAL_LIST ) ) );

        // Deserialize standard left signals
        circuit.leftSignals.addAll( SerializationUtils.getBlockPosListFromBlockPosNBTArray(
                nbt.getTag( NBT_KEY_STANDARD_LEFT_SIGNAL_LIST ) ) );

        // Deserialize standard right signals
        circuit.rightSignals.addAll( SerializationUtils.getBlockPosListFromBlockPosNBTArray(
                nbt.getTag( NBT_KEY_STANDARD_RIGHT_SIGNAL_LIST ) ) );

        // Deserialize through/solid/ball signals
        circuit.throughSignals.addAll(
                SerializationUtils.getBlockPosListFromBlockPosNBTArray( nbt.getTag( NBT_KEY_THROUGH_SIGNAL_LIST ) ) );

        // Deserialize pedestrian signals
        circuit.pedestrianSignals.addAll( SerializationUtils.getBlockPosListFromBlockPosNBTArray(
                nbt.getTag( NBT_KEY_PEDESTRIAN_SIGNAL_LIST ) ) );

        // Deserialize pedestrian beacon signals
        circuit.pedestrianBeaconSignals.addAll( SerializationUtils.getBlockPosListFromBlockPosNBTArray(
                nbt.getTag( NBT_KEY_PEDESTRIAN_BEACON_SIGNAL_LIST ) ) );

        // Deserialize pedestrian accessory signals
        circuit.pedestrianAccessorySignals.addAll( SerializationUtils.getBlockPosListFromBlockPosNBTArray(
                nbt.getTag( NBT_KEY_PEDESTRIAN_ACCESSORY_SIGNAL_LIST ) ) );

        // Deserialize protected signals
        circuit.protectedSignals.addAll(
                SerializationUtils.getBlockPosListFromBlockPosNBTArray( nbt.getTag( NBT_KEY_PROTECTED_SIGNAL_LIST ) ) );

        // Deserialize sensors
        circuit.sensors.addAll(
                SerializationUtils.getBlockPosListFromBlockPosNBTArray( nbt.getTag( NBT_KEY_SENSOR_LIST ) ) );

        // Return the circuit
        return circuit;
    }

    /**
     * Checks if the given {@link Object} is equal to this {@link TrafficSignalControllerCircuit} object.
     *
     * @param o The {@link Object} to check.
     *
     * @return {@code true} if the given {@link Object} is equal to this {@link TrafficSignalControllerCircuit} object,
     *         {@code false} otherwise.
     *
     * @since 1.0
     */
    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }
        TrafficSignalControllerCircuit that = ( TrafficSignalControllerCircuit ) o;
        return Objects.equals( flashingLeftSignals, that.flashingLeftSignals ) &&
                Objects.equals( flashingRightSignals, that.flashingRightSignals ) &&
                Objects.equals( leftSignals, that.leftSignals ) &&
                Objects.equals( rightSignals, that.rightSignals ) &&
                Objects.equals( throughSignals, that.throughSignals ) &&
                Objects.equals( pedestrianSignals, that.pedestrianSignals ) &&
                Objects.equals( pedestrianBeaconSignals, that.pedestrianBeaconSignals ) &&
                Objects.equals( pedestrianAccessorySignals, that.pedestrianAccessorySignals ) &&
                Objects.equals( protectedSignals, that.protectedSignals ) &&
                Objects.equals( sensors, that.sensors );
    }

    /**
     * Gets the hash code of this {@link TrafficSignalControllerCircuit} object.
     *
     * @return The hash code of this {@link TrafficSignalControllerCircuit} object.
     *
     * @since 1.0
     */
    @Override
    public int hashCode() {
        return Objects.hash( flashingLeftSignals, flashingRightSignals, leftSignals, rightSignals, throughSignals,
                             pedestrianSignals, pedestrianBeaconSignals, pedestrianAccessorySignals, protectedSignals,
                             sensors );
    }

    //endregion
}
