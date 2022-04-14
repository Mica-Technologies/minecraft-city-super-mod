package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Objects;

/**
 * The class representation of a list of traffic signal controller circuits in a format which can be easily serialized
 * and deserialized as Minecraft NBT data.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see #fromNBT(NBTTagCompound)
 * @see #toNBT()
 * @since 2023.2.0
 */
public class TrafficSignalControllerCircuits
{
    //region: Static/Constant Fields

    //endregion

    //region: Instance Fields

    /**
     * The list of {@link TrafficSignalControllerCircuit}s for the traffic signal controller.
     *
     * @since 1.0
     */
    private final ArrayList< TrafficSignalControllerCircuit > circuits = new ArrayList<>();

    //endregion

    //region: Instance Methods

    /**
     * Gets the list of {@link TrafficSignalControllerCircuit}s for the traffic signal controller.
     *
     * @return The list of {@link TrafficSignalControllerCircuit}s for the traffic signal controller.
     *
     * @since 1.0
     */
    public ArrayList< TrafficSignalControllerCircuit > getCircuits() {
        return circuits;
    }

    /**
     * Gets the number of {@link TrafficSignalControllerCircuit}s configured for the traffic signal controller.
     *
     * @return The number of {@link TrafficSignalControllerCircuit}s configured for the traffic signal controller.
     *
     * @since 1.0
     */
    public int getCircuitCount() {
        return circuits.size();
    }

    /**
     * Gets the {@link TrafficSignalControllerCircuit} at the specified index, or a new
     * {@link TrafficSignalControllerCircuit} if the index is out of bounds.
     *
     * @param index The index of the {@link TrafficSignalControllerCircuit} to get.
     *
     * @return The {@link TrafficSignalControllerCircuit} at the specified index or a new
     *         {@link TrafficSignalControllerCircuit} if the index is out of bounds.
     *
     * @since 1.0
     */
    public TrafficSignalControllerCircuit getCircuit( int index ) {
        // Create a TrafficSignalControllerCircuit variable to store the located or created circuit
        TrafficSignalControllerCircuit circuit;

        // Get the circuit at the specified index, otherwise create and store a new circuit
        if ( index < 0 || index >= circuits.size() ) {
            circuit = new TrafficSignalControllerCircuit();
            circuits.add( circuit );
        }
        else {
            circuit = circuits.get( index );
        }

        // Return the located or created circuit
        return circuit;
    }

    /**
     * Unlinks the device at the specified {@link BlockPos} from the applicable {@link TrafficSignalControllerCircuit},
     * if present.
     *
     * @param pos The {@link BlockPos} of the device to unlink.
     *
     * @return True if the device was unlinked, false otherwise.
     *
     * @since 1.0
     */
    public boolean unlinkDevice( BlockPos pos ) {
        for ( TrafficSignalControllerCircuit circuit : circuits ) {
            if ( circuit.unlinkDevice( pos ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the specified {@link TrafficSignalControllerCircuit} to the list of {@link TrafficSignalControllerCircuit}s
     * for the traffic signal controller.
     *
     * @param circuit The {@link TrafficSignalControllerCircuit} to add to the list of
     *                {@link TrafficSignalControllerCircuit}s for the traffic signal controller.
     *
     * @since 1.0
     */
    public boolean addCircuit( TrafficSignalControllerCircuit circuit ) {
        return circuits.add( circuit );
    }

    /**
     * Removes the specified {@link TrafficSignalControllerCircuit} from the list of
     * {@link TrafficSignalControllerCircuit}s for the traffic signal controller.
     *
     * @param circuit The {@link TrafficSignalControllerCircuit} to remove from the list of
     *                {@link TrafficSignalControllerCircuit}s for the traffic signal controller.
     *
     * @since 1.0
     */
    public boolean removeCircuit( TrafficSignalControllerCircuit circuit ) {
        return circuits.remove( circuit );
    }

    /**
     * Executes the specified {@link java.util.function.Consumer} for each signal device linked to any (all) of the
     * {@link TrafficSignalControllerCircuit}s for the traffic signal controller.
     *
     * @param action The {@link java.util.function.Consumer} to execute for each signal device linked to any (all) of
     *               the {@link TrafficSignalControllerCircuit}s for the traffic signal controller.
     *
     * @since 1.0
     */
    public void forAllSignals( java.util.function.Consumer< ? super BlockPos > action ) {
        for ( TrafficSignalControllerCircuit circuit : circuits ) {
            circuit.forAllSignals( action );
        }
    }

    /**
     * Powers off each signal device linked to any (all) of the {@link TrafficSignalControllerCircuit}s for the traffic
     * signal controller.
     *
     * @param world The {@link World} in which the signal devices are located.
     */
    public void powerOffAllSignals( World world ) {
        for ( TrafficSignalControllerCircuit circuit : circuits ) {
            circuit.powerOffAllSignals( world );
        }
    }

    /**
     * Gets a boolean indicating whether the specified device {@link BlockPos} is linked to any of the
     * {@link TrafficSignalControllerCircuit}s configured for the traffic signal controller.
     *
     * @param devicePos The {@link BlockPos} of the device to check.
     *
     * @return True if the specified device {@link BlockPos} is linked to any of the
     *         {@link TrafficSignalControllerCircuit}s, false otherwise.
     *
     * @since 1.0
     */
    public boolean isDeviceLinked( BlockPos devicePos ) {
        // Check if the device is linked to any of the circuits, if so return true
        for ( TrafficSignalControllerCircuit circuit : circuits ) {
            if ( circuit.isDeviceLinked( devicePos ) ) {
                return true;
            }
        }

        // The device is not linked to any of the circuits, false
        return false;
    }

    //endregion

    //region: Serialization Methods

    /**
     * Gets an {@link NBTTagCompound} containing the data for the list of traffic signal controller circuits in NBT
     * format.
     *
     * @return The {@link NBTTagCompound} containing the data for the list of traffic signal controller circuits in NBT
     *         format.
     *         <p>
     *         The returned {@link NBTTagCompound} can be used to reconstruct the
     *         {@link TrafficSignalControllerCircuits} object.
     *         </p>
     *
     * @since 1.0
     */
    public NBTTagCompound toNBT() {
        // Create the compound
        NBTTagCompound compound = new NBTTagCompound();

        // Add the list of circuits
        for ( int i = 0; i < circuits.size(); i++ ) {
            // Get the circuit
            TrafficSignalControllerCircuit circuit = circuits.get( i );

            // Add the circuit to the compound
            compound.setTag( String.valueOf( i ), circuit.toNBT() );
        }

        // Return the compound
        return compound;
    }

    /**
     * Gets a {@link TrafficSignalControllerCircuits} object containing the data for the list of traffic signal
     * controller circuits in NBT format.
     *
     * @param nbt The {@link NBTTagCompound} containing the data for the list of traffic signal controller circuits in
     *            NBT format.
     *
     * @return The {@link TrafficSignalControllerCircuits} object containing the data for the list of traffic signal
     *         controller circuits in NBT format.
     *
     * @throws IllegalArgumentException If the {@link NBTTagCompound} is null.
     * @since 1.0
     */
    public static TrafficSignalControllerCircuits fromNBT( NBTTagCompound nbt ) {
        // Validate the NBT
        if ( nbt == null ) {
            throw new IllegalArgumentException( "The NBT cannot be null." );
        }

        // Create the circuits object
        TrafficSignalControllerCircuits nbtCircuits = new TrafficSignalControllerCircuits();

        // Get the list of circuits
        for ( int i = 0; i < nbt.getSize(); i++ ) {
            // Get the circuit
            NBTTagCompound circuitNBT = nbt.getCompoundTag( String.valueOf( i ) );

            // Add the circuit to the list
            nbtCircuits.addCircuit( TrafficSignalControllerCircuit.fromNBT( circuitNBT ) );
        }

        // Return the circuits object
        return nbtCircuits;
    }

    /**
     * Checks if the given {@link Object} is equal to this {@link TrafficSignalControllerCircuits} object.
     *
     * @param o The {@link Object} to check.
     *
     * @return {@code true} if the given {@link Object} is equal to this {@link TrafficSignalControllerCircuits} object,
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
        TrafficSignalControllerCircuits that = ( TrafficSignalControllerCircuits ) o;
        return Objects.equals( circuits, that.circuits );
    }

    /**
     * Gets the hash code for this {@link TrafficSignalControllerCircuits} object.
     *
     * @return The hash code for this {@link TrafficSignalControllerCircuits} object.
     *
     * @since 1.0
     */
    @Override
    public int hashCode() {
        return Objects.hash( circuits );
    }

    //endregion
}
