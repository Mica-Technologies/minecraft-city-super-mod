package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;

/**
 * The class representation of a list of traffic signal controller circuits in a format which can be easily serialized
 * and deserialized as Minecraft NBT data.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see #fromNBT(NBTTagCompound)
 * @see #toNBT()
 * @since 2022.1.0
 */
public class TrafficSignalControllerCircuits
{
    //region: Static/Constant Fields

    /**
     * The prefix appended to NBT keys used to represent an individual {@link TrafficSignalControllerCircuit} as an NBT
     * value.
     *
     * @since 1.0
     */
    public static final String NBT_KEY_CIRCUIT__PREFIX = "circuit_";

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
            compound.setTag( NBT_KEY_CIRCUIT__PREFIX + i, circuit.toNBT() );
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
        TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();

        // Get the list of circuits
        for ( int i = 0; nbt.hasKey( NBT_KEY_CIRCUIT__PREFIX + i ); i++ ) {
            // Get the circuit
            NBTTagCompound circuitNBT = nbt.getCompoundTag( NBT_KEY_CIRCUIT__PREFIX + i );

            // Add the circuit to the list
            circuits.circuits.add( TrafficSignalControllerCircuit.fromNBT( circuitNBT ) );
        }

        // Return the circuits object
        return circuits;
    }

    //endregion
}
