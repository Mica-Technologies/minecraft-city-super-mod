package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.SerializationUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The class representation of a traffic signal controller circuit which stores the following signal types:
 * <ul>
 *     <li>Flashing Left</li>
 *     <li>Flashing Right</li>
 *     <li>Standard Left</li>
 *     <li>Standard Right</li>
 *     <li>Through</li>
 *     <li>Pedestrian</li>
 *     <li>Protected</li>
 *     <li>Sensors</li>
 * </ul> in a format which can be easily serialized and deserialized as Minecraft NBT data.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see #fromNBT(NBTTagCompound)
 * @see #toNBT()
 * @since 2022.1.0
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
    private final ArrayList< BlockPos > flashingLeftSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of flashing right signals in the circuit.
     *
     * @since 1.0
     */
    private final ArrayList< BlockPos > flashingRightSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of standard left signals in the circuit.
     *
     * @since 1.0
     */
    private final ArrayList< BlockPos > leftSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of standard right signals in the circuit.
     *
     * @since 1.0
     */
    private final ArrayList< BlockPos > rightSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of through/solid/ball signals in the circuit.
     *
     * @since 1.0
     */
    private final ArrayList< BlockPos > throughSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of pedestrian signals in the circuit.
     *
     * @since 1.0
     */
    private final ArrayList< BlockPos > pedestrianSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of protected signals in the circuit.
     *
     * @since 1.0
     */
    private final ArrayList< BlockPos > protectedSignals = new ArrayList<>();

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
    public ArrayList< BlockPos > getFlashingLeftSignals() {
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
    public ArrayList< BlockPos > getFlashingRightSignals() {
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
    public ArrayList< BlockPos > getLeftSignals() {
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
    public ArrayList< BlockPos > getRightSignals() {
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
    public ArrayList< BlockPos > getThroughSignals() {
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
    public ArrayList< BlockPos > getPedestrianSignals() {
        return pedestrianSignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of protected signals in the circuit.
     *
     * @return The list of {@link BlockPos}es of protected signals in the circuit.
     *
     * @see #protectedSignals
     * @since 1.0
     */
    public ArrayList< BlockPos > getProtectedSignals() {
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
    public ArrayList< BlockPos > getSensors() {
        return sensors;
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
        return flashingLeftSignals.contains( devicePos ) ||
                flashingRightSignals.contains( devicePos ) ||
                leftSignals.contains( devicePos ) ||
                rightSignals.contains( devicePos ) ||
                throughSignals.contains( devicePos ) ||
                pedestrianSignals.contains( devicePos ) ||
                protectedSignals.contains( devicePos ) ||
                sensors.contains( devicePos );
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
        compound.setIntArray( NBT_KEY_FLASHING_LEFT_SIGNAL_LIST,
                              SerializationUtils.getBlockPosIntArrayFromList( flashingLeftSignals ) );

        // Serialize flashing right signals
        compound.setIntArray( NBT_KEY_FLASHING_RIGHT_SIGNAL_LIST,
                              SerializationUtils.getBlockPosIntArrayFromList( flashingRightSignals ) );

        // Serialize standard left signals
        compound.setIntArray( NBT_KEY_STANDARD_LEFT_SIGNAL_LIST,
                              SerializationUtils.getBlockPosIntArrayFromList( leftSignals ) );

        // Serialize standard right signals
        compound.setIntArray( NBT_KEY_STANDARD_RIGHT_SIGNAL_LIST,
                              SerializationUtils.getBlockPosIntArrayFromList( rightSignals ) );

        // Serialize through/solid/ball signals
        compound.setIntArray( NBT_KEY_THROUGH_SIGNAL_LIST,
                              SerializationUtils.getBlockPosIntArrayFromList( throughSignals ) );

        // Serialize pedestrian signals
        compound.setIntArray( NBT_KEY_PEDESTRIAN_SIGNAL_LIST,
                              SerializationUtils.getBlockPosIntArrayFromList( pedestrianSignals ) );

        // Serialize protected signals
        compound.setIntArray( NBT_KEY_PROTECTED_SIGNAL_LIST,
                              SerializationUtils.getBlockPosIntArrayFromList( protectedSignals ) );

        // Serialize sensors
        compound.setIntArray( NBT_KEY_SENSOR_LIST, SerializationUtils.getBlockPosIntArrayFromList( sensors ) );

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
        circuit.flashingLeftSignals.addAll( SerializationUtils.getBlockPosListFromIntArray(
                nbt.getIntArray( NBT_KEY_FLASHING_LEFT_SIGNAL_LIST ) ) );

        // Deserialize flashing right signals
        circuit.flashingRightSignals.addAll( SerializationUtils.getBlockPosListFromIntArray(
                nbt.getIntArray( NBT_KEY_FLASHING_RIGHT_SIGNAL_LIST ) ) );

        // Deserialize standard left signals
        circuit.leftSignals.addAll( SerializationUtils.getBlockPosListFromIntArray(
                nbt.getIntArray( NBT_KEY_STANDARD_LEFT_SIGNAL_LIST ) ) );

        // Deserialize standard right signals
        circuit.rightSignals.addAll( SerializationUtils.getBlockPosListFromIntArray(
                nbt.getIntArray( NBT_KEY_STANDARD_RIGHT_SIGNAL_LIST ) ) );

        // Deserialize through/solid/ball signals
        circuit.throughSignals.addAll(
                SerializationUtils.getBlockPosListFromIntArray( nbt.getIntArray( NBT_KEY_THROUGH_SIGNAL_LIST ) ) );

        // Deserialize pedestrian signals
        circuit.pedestrianSignals.addAll(
                SerializationUtils.getBlockPosListFromIntArray( nbt.getIntArray( NBT_KEY_PEDESTRIAN_SIGNAL_LIST ) ) );

        // Deserialize protected signals
        circuit.protectedSignals.addAll(
                SerializationUtils.getBlockPosListFromIntArray( nbt.getIntArray( NBT_KEY_PROTECTED_SIGNAL_LIST ) ) );

        // Deserialize sensors
        circuit.sensors.addAll(
                SerializationUtils.getBlockPosListFromIntArray( nbt.getIntArray( NBT_KEY_SENSOR_LIST ) ) );

        // Return the circuit
        return circuit;
    }

    //endregion
}
