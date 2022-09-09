package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.SerializationUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

/**
 * The class representation of a traffic signal phase in a format which can be easily serialized and deserialized as
 * Minecraft NBT data.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see #fromNBT(NBTTagCompound)
 * @see #toNBT()
 * @since 2022.1.0
 */
public class TrafficSignalPhase
{
    //region: Static Fields

    /**
     * The circuit value which is used to indicate that the phase is not applicable to the circuit.
     *
     * @since 1.0
     */
    public static final int CIRCUIT_NOT_APPLICABLE = -1;

    /**
     * The key used to store the list of off signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_OFF_SIGNAL_LIST = "offSignalList";

    /**
     * The key used to store the list of green signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_GREEN_SIGNAL_LIST = "greenSignalList";

    /**
     * The key used to store the list of yellow signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_YELLOW_SIGNAL_LIST = "yellowSignalList";

    /**
     * The key used to store the list of red signal {@link BlockPos}es in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_RED_SIGNAL_LIST = "redSignalList";

    /**
     * The key used to store the circuit which is being serviced by this phase in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_CIRCUIT = "circuit";

    /**
     * The key used to store the maximum green time in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_MAX_GREEN_TIME = "maxGreenTime";

    /**
     * The key used to store the minimum green time in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_MIN_GREEN_TIME = "minGreenTime";

    /**
     * The key used to store the applicability of this phase in NBT data.
     *
     * @since 1.0
     */
    private static final String NBT_KEY_APPLICABILITY = "applicability";

    //endregion

    //region: Instance Fields

    /**
     * The list of {@link BlockPos}es of signals which are in the 'off' state during this phase.
     *
     * @since 1.0
     */
    private final ArrayList< BlockPos > offSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of signals which are in the 'green' state during this phase.
     *
     * @since 1.0
     */
    private final ArrayList< BlockPos > greenSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of signals which are in the 'yellow' state during this phase.
     *
     * @since 1.0
     */
    private final ArrayList< BlockPos > yellowSignals = new ArrayList<>();

    /**
     * The list of {@link BlockPos}es of signals which are in the 'red' state during this phase.
     *
     * @since 1.0
     */
    private final ArrayList< BlockPos > redSignals = new ArrayList<>();

    /**
     * The circuit which is being serviced by this phase, or -1 if this phase is not associated with a circuit.
     *
     * @since 1.0
     */
    private final int circuit;

    /**
     * The maximum green time for this phase.
     *
     * @since 1.0
     */
    private final int maxGreenTime;

    /**
     * The minimum green time for this phase.
     *
     * @since 1.0
     */
    private final int minGreenTime;

    /**
     * The applicability of this phase to the given circuit.
     *
     * @since 1.0
     */
    private final TrafficSignalPhaseApplicability applicability;

    //endregion

    //region: Constructors

    /**
     * Constructor of the class representation of a traffic signal phase in a format which can be easily serialized and
     * deserialized as Minecraft NBT data.
     *
     * @param circuit       The circuit which is being serviced by this phase, or -1 if this phase is not associated
     *                      with a
     * @param maxGreenTime  The maximum green time for this phase.
     * @param minGreenTime  The minimum green time for this phase.
     * @param applicability The applicability of this phase to the given circuit.
     *
     * @since 1.0
     */
    public TrafficSignalPhase( int circuit,
                               int maxGreenTime,
                               int minGreenTime,
                               TrafficSignalPhaseApplicability applicability )
    {
        this.circuit = circuit;
        this.maxGreenTime = maxGreenTime;
        this.minGreenTime = minGreenTime;
        this.applicability = applicability;
    }

    //endregion

    //region: Instance Methods

    /**
     * Gets the list of {@link BlockPos}es of signals which are in the 'off' state during this phase.
     *
     * @return The list of {@link BlockPos}es of signals which are in the 'off' state during this phase.
     *
     * @since 1.0
     */
    public ArrayList< BlockPos > getOffSignals()
    {
        return this.offSignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of signals which are in the 'green' state during this phase.
     *
     * @return The list of {@link BlockPos}es of signals which are in the 'green' state during this phase.
     *
     * @since 1.0
     */
    public ArrayList< BlockPos > getGreenSignals()
    {
        return this.greenSignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of signals which are in the 'yellow' state during this phase.
     *
     * @return The list of {@link BlockPos}es of signals which are in the 'yellow' state during this phase.
     *
     * @since 1.0
     */
    public ArrayList< BlockPos > getYellowSignals()
    {
        return this.yellowSignals;
    }

    /**
     * Gets the list of {@link BlockPos}es of signals which are in the 'red' state during this phase.
     *
     * @return The list of {@link BlockPos}es of signals which are in the 'red' state during this phase.
     *
     * @since 1.0
     */
    public ArrayList< BlockPos > getRedSignals()
    {
        return this.redSignals;
    }

    /**
     * Gets the circuit which is being serviced by this phase.
     *
     * @return The circuit which is being serviced by this phase.
     *
     * @since 1.0
     */
    public int getCircuit() {
        return circuit;
    }

    /**
     * Gets the maximum green time for this phase.
     *
     * @return The maximum green time for this phase.
     *
     * @since 1.0
     */
    public int getMaxGreenTime() {
        return maxGreenTime;
    }

    /**
     * Gets the minimum green time for this phase.
     *
     * @return The minimum green time for this phase.
     *
     * @since 1.0
     */
    public int getMinGreenTime() {
        return minGreenTime;
    }

    /**
     * Gets the applicability of this phase to the given circuit.
     *
     * @return The applicability of this phase to the given circuit.
     *
     * @since 1.0
     */
    public TrafficSignalPhaseApplicability getApplicability() {
        return applicability;
    }

    /**
     * Adds the given {@link BlockPos} to the list of signals which are in the 'off' state during this phase.
     *
     * @param pos The {@link BlockPos} to add to the list of signals which are in the 'off' state during this phase.
     *
     * @return true if the given {@link BlockPos} was added to the list of signals which are in the 'off' state during
     *         this phase; false otherwise.
     *
     * @since 1.0
     */
    public boolean addOffSignal( BlockPos pos )
    {
        return offSignals.add( pos );
    }

    /**
     * Adds the given {@link BlockPos} to the list of signals which are in the 'green' state during this phase.
     *
     * @param pos The {@link BlockPos} to add to the list of signals which are in the 'green' state during this phase.
     *
     * @return true if the given {@link BlockPos} was added to the list of signals which are in the 'green' state during
     *         this phase; false otherwise.
     *
     * @since 1.0
     */
    public boolean addGreenSignal( BlockPos pos )
    {
        return greenSignals.add( pos );
    }

    /**
     * Adds the given {@link BlockPos} to the list of signals which are in the 'yellow' state during this phase.
     *
     * @param pos The {@link BlockPos} to add to the list of signals which are in the 'yellow' state during this phase.
     *
     * @return true if the given {@link BlockPos} was added to the list of signals which are in the 'yellow' state
     *         during this phase; false otherwise.
     *
     * @since 1.0
     */
    public boolean addYellowSignal( BlockPos pos )
    {
        return yellowSignals.add( pos );
    }

    /**
     * Adds the given {@link BlockPos} to the list of signals which are in the 'red' state during this phase.
     *
     * @param pos The {@link BlockPos} to add to the list of signals which are in the 'red' state during this phase.
     *
     * @return true if the given {@link BlockPos} was added to the list of signals which are in the 'red' state during
     *         this phase; false otherwise.
     *
     * @since 1.0
     */
    public boolean addRedSignal( BlockPos pos )
    {
        return redSignals.add( pos );
    }

    /**
     * Adds the given list of {@link BlockPos}s to the list of signals which are in the 'off' state during this phase.
     *
     * @param pos The list of {@link BlockPos}s to add to the list of signals which are in the 'off' state during this
     *            phase.
     *
     * @return true if the given list of {@link BlockPos}s was added to the list of signals which are in the 'off' state
     *         during this phase; false otherwise.
     *
     * @since 1.0
     */
    public boolean addOffSignals( ArrayList< BlockPos > pos )
    {
        return offSignals.addAll( pos );
    }

    /**
     * Adds the given list of {@link BlockPos}s to the list of signals which are in the 'green' state during this
     * phase.
     *
     * @param pos The list of {@link BlockPos}s to add to the list of signals which are in the 'green' state during this
     *            phase.
     *
     * @return true if the given list of {@link BlockPos}s was added to the list of signals which are in the 'green'
     *         state during this phase; false otherwise.
     *
     * @since 1.0
     */
    public boolean addGreenSignals( ArrayList< BlockPos > pos )
    {
        return greenSignals.addAll( pos );
    }

    /**
     * Adds the given list of {@link BlockPos}s to the list of signals which are in the 'yellow' state during this
     * phase.
     *
     * @param pos The list of {@link BlockPos}s to add to the list of signals which are in the 'yellow' state during
     *            this phase.
     *
     * @return true if the given list of {@link BlockPos}s was added to the list of signals which are in the 'yellow'
     *         state during this phase; false otherwise.
     *
     * @since 1.0
     */
    public boolean addYellowSignals( ArrayList< BlockPos > pos )
    {
        return yellowSignals.addAll( pos );
    }

    /**
     * Adds the given list of {@link BlockPos}s to the list of signals which are in the 'red' state during this phase.
     *
     * @param pos The list of {@link BlockPos}s to add to the list of signals which are in the 'red' state during this
     *            phase.
     *
     * @return true if the given list of {@link BlockPos}s was added to the list of signals which are in the 'red' state
     *         during this phase; false otherwise.
     *
     * @since 1.0
     */
    public boolean addRedSignals( ArrayList< BlockPos > pos )
    {
        return redSignals.addAll( pos );
    }

    //endregion

    //region: Serialization Methods

    /**
     * Gets an {@link NBTTagCompound} containing the data for this phase in NBT format.
     *
     * @return The {@link NBTTagCompound} containing the data for this phase in NBT format.
     *         <p>
     *         The returned {@link NBTTagCompound} can be used to reconstruct the {@link TrafficSignalPhase} object.
     *         </p>
     *
     * @see #fromNBT(NBTTagCompound)
     * @since 1.0
     */
    public NBTTagCompound toNBT() {
        // Create the compound
        NBTTagCompound compound = new NBTTagCompound();

        // Serialize off signals
        compound.setIntArray( NBT_KEY_OFF_SIGNAL_LIST, SerializationUtils.getBlockPosIntArrayFromList( offSignals ) );

        // Serialize green signals
        compound.setIntArray( NBT_KEY_GREEN_SIGNAL_LIST,
                              SerializationUtils.getBlockPosIntArrayFromList( greenSignals ) );

        // Serialize yellow signals
        compound.setIntArray( NBT_KEY_YELLOW_SIGNAL_LIST,
                              SerializationUtils.getBlockPosIntArrayFromList( yellowSignals ) );

        // Serialize red signals
        compound.setIntArray( NBT_KEY_RED_SIGNAL_LIST, SerializationUtils.getBlockPosIntArrayFromList( redSignals ) );

        // Serialize the circuit
        compound.setInteger( NBT_KEY_CIRCUIT, circuit );

        // Serialize the max green time
        compound.setInteger( NBT_KEY_MAX_GREEN_TIME, maxGreenTime );

        // Serialize the min green time
        compound.setInteger( NBT_KEY_MIN_GREEN_TIME, minGreenTime );

        // Serialize the applicability
        compound.setInteger( NBT_KEY_APPLICABILITY, applicability.getId() );

        // Return the compound
        return compound;
    }

    /**
     * Gets a {@link TrafficSignalPhase} object from the given {@link NBTTagCompound}.
     *
     * @param nbt The {@link NBTTagCompound} containing the data for the {@link TrafficSignalPhase} object in NBT
     *            format.
     *
     * @return The {@link TrafficSignalPhase} object from the given {@link NBTTagCompound}.
     *
     * @throws IllegalArgumentException If the given {@link NBTTagCompound} is null.
     * @see #toNBT()
     * @since 1.0
     */
    public static TrafficSignalPhase fromNBT( NBTTagCompound nbt ) {
        // Validate the NBT
        if ( nbt == null ) {
            throw new IllegalArgumentException( "The NBT cannot be null." );
        }

        // Create the phase
        TrafficSignalPhase phase = new TrafficSignalPhase( nbt.getInteger( NBT_KEY_CIRCUIT ),
                                                           nbt.getInteger( NBT_KEY_MAX_GREEN_TIME ),
                                                           nbt.getInteger( NBT_KEY_MIN_GREEN_TIME ),
                                                           TrafficSignalPhaseApplicability.getDirection(
                                                                   nbt.getInteger( NBT_KEY_APPLICABILITY ) ) );

        // Deserialize off signals
        phase.offSignals.addAll(
                SerializationUtils.getBlockPosListFromIntArray( nbt.getIntArray( NBT_KEY_OFF_SIGNAL_LIST ) ) );

        // Deserialize green signals
        phase.greenSignals.addAll(
                SerializationUtils.getBlockPosListFromIntArray( nbt.getIntArray( NBT_KEY_GREEN_SIGNAL_LIST ) ) );

        // Deserialize yellow signals
        phase.yellowSignals.addAll(
                SerializationUtils.getBlockPosListFromIntArray( nbt.getIntArray( NBT_KEY_YELLOW_SIGNAL_LIST ) ) );

        // Deserialize red signals
        phase.redSignals.addAll(
                SerializationUtils.getBlockPosListFromIntArray( nbt.getIntArray( NBT_KEY_RED_SIGNAL_LIST ) ) );

        // Return the phase
        return phase;
    }

    //endregion
}
