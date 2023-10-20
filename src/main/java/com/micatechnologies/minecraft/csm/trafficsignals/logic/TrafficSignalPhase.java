package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.SerializationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The class representation of a traffic signal phase in a format which can be easily serialized and
 * deserialized as Minecraft NBT data.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see #fromNBT(NBTTagCompound)
 * @see #toNBT()
 * @since 2023.2.0
 */
public class TrafficSignalPhase {
  // region: Static Fields

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
   * The key used to store the list of FYA (flashing yellow arrow) signal {@link BlockPos}es in NBT
   * data.
   *
   * @since 1.0
   */
  private static final String NBT_KEY_FYA_SIGNAL_LIST = "fyaSignalList";

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
   * The key used to store the list of walk signal {@link BlockPos}es in NBT data.
   *
   * @since 1.0
   */
  private static final String NBT_KEY_WALK_SIGNAL_LIST = "walkSignalList";

  /**
   * The key used to store the list of flashing don't walk signal {@link BlockPos}es in NBT data.
   *
   * @since 1.0
   */
  private static final String NBT_KEY_FLASH_DONT_WALK_SIGNAL_LIST = "flashDontWalkSignalList";

  /**
   * The key used to store the list of don't walk signal {@link BlockPos}es in NBT data.
   *
   * @since 1.0
   */
  private static final String NBT_KEY_DONT_WALK_SIGNAL_LIST = "dontWalkSignalList";

  /**
   * The key used to store the circuit which is being serviced by this phase in NBT data.
   *
   * @since 1.0
   */
  private static final String NBT_KEY_CIRCUIT = "circuit";

  /**
   * The key used to store the upcoming phase in NBT data.
   *
   * @since 1.0
   */
  private static final String NBT_KEY_UPCOMING_PHASE = "upcomingPhase";

  /**
   * The key used to store the applicability of this phase in NBT data.
   *
   * @since 1.0
   */
  private static final String NBT_KEY_APPLICABILITY = "applicability";

  // endregion

  // region: Instance Fields

  /**
   * The list of {@link BlockPos}es of signals which are in the 'off' state during this phase.
   *
   * @since 1.0
   */
  private final ArrayList<BlockPos> offSignals = new ArrayList<>();

  /**
   * The list of {@link BlockPos}es of signals which are in the 'fya' state during this phase.
   *
   * @since 1.0
   */
  private final ArrayList<BlockPos> fyaSignals = new ArrayList<>();

  /**
   * The list of {@link BlockPos}es of signals which are in the 'green' state during this phase.
   *
   * @since 1.0
   */
  private final ArrayList<BlockPos> greenSignals = new ArrayList<>();

  /**
   * The list of {@link BlockPos}es of signals which are in the 'yellow' state during this phase.
   *
   * @since 1.0
   */
  private final ArrayList<BlockPos> yellowSignals = new ArrayList<>();

  /**
   * The list of {@link BlockPos}es of signals which are in the 'red' state during this phase.
   *
   * @since 1.0
   */
  private final ArrayList<BlockPos> redSignals = new ArrayList<>();

  /**
   * The list of {@link BlockPos}es of signals which are in the 'walk' state during this phase.
   *
   * @since 1.0
   */
  private final ArrayList<BlockPos> walkSignals = new ArrayList<>();

  /**
   * The list of {@link BlockPos}es of signals which are in the 'flashing don't walk' state during
   * this phase.
   *
   * @since 1.0
   */
  private final ArrayList<BlockPos> flashDontWalkSignals = new ArrayList<>();

  /**
   * The list of {@link BlockPos}es of signals which are in the 'don't walk' state during this
   * phase.
   *
   * @since 1.0
   */
  private final ArrayList<BlockPos> dontWalkSignals = new ArrayList<>();

  /**
   * The circuit which is being serviced by this phase, or -1 if this phase is not associated with a
   * circuit.
   *
   * @since 1.0
   */
  private final int circuit;

  /**
   * The upcoming phase for this phase. This is the phase which will be transitioned to after this
   * phase, or subsequent required phases, have been completed.
   *
   * @since 1.0
   */
  private final TrafficSignalPhase upcomingPhase;

  /**
   * The applicability of this phase to the given circuit.
   *
   * @since 1.0
   */
  private final TrafficSignalPhaseApplicability applicability;

  // endregion

  // region: Constructors

  /**
   * Constructor of the class representation of a traffic signal phase in a format which can be
   * easily serialized and deserialized as Minecraft NBT data.
   *
   * @param circuit       The circuit which is being serviced by this phase, or -1 if this phase is
   *                      not associated with a circuit.
   * @param upcomingPhase The upcoming phase for this phase. This is the phase which will be
   *                      transitioned to after this phase, or subsequent required phases, have been
   *                      completed.
   * @param applicability The applicability of this phase to the given circuit.
   *
   * @since 1.0
   */
  public TrafficSignalPhase(int circuit,
      TrafficSignalPhase upcomingPhase,
      TrafficSignalPhaseApplicability applicability) {
    this.circuit = circuit;
    this.upcomingPhase = upcomingPhase;
    this.applicability = applicability;
  }

  /**
   * Constructor of the class representation of a traffic signal phase in a format which can be
   * easily serialized and deserialized as Minecraft NBT data.
   *
   * @param circuit       The circuit which is being serviced by this phase, or -1 if this phase is
   *                      not associated with a circuit.
   * @param applicability The applicability of this phase to the given circuit.
   *
   * @since 1.0
   */
  public TrafficSignalPhase(int circuit, TrafficSignalPhaseApplicability applicability) {
    this.circuit = circuit;
    this.upcomingPhase = null;
    this.applicability = applicability;
  }

  // endregion

  // region: Instance Methods

  /**
   * Gets a {@link TrafficSignalPhase} object from the given {@link NBTTagCompound}.
   *
   * @param nbt             The {@link NBTTagCompound} containing the data for the
   *                        {@link TrafficSignalPhase} object in NBT format.
   * @param isUpcomingPhase Whether or not the {@link TrafficSignalPhase} object is an upcoming
   *                        phase.
   *
   * @return The {@link TrafficSignalPhase} object from the given {@link NBTTagCompound}.
   *
   * @throws IllegalArgumentException If the given {@link NBTTagCompound} is null.
   * @see #toNBT()
   * @since 1.0
   */
  private static TrafficSignalPhase fromNBT(NBTTagCompound nbt, boolean isUpcomingPhase) {
    // Validate the NBT
    if (nbt == null) {
      throw new IllegalArgumentException("The NBT cannot be null.");
    }

    // Get the upcoming phase
    TrafficSignalPhase upcomingPhase = null;
    if (nbt.hasKey(NBT_KEY_UPCOMING_PHASE)) {
      if (isUpcomingPhase) {
        throw new IllegalArgumentException(
            "The NBT cannot contain an upcoming phase with another nested upcoming phase.");
      } else {
        upcomingPhase = fromNBT(nbt.getCompoundTag(NBT_KEY_UPCOMING_PHASE), true);
      }
    }
    // Create the phase
    TrafficSignalPhase phase = new TrafficSignalPhase(nbt.getInteger(NBT_KEY_CIRCUIT),
        upcomingPhase,
        TrafficSignalPhaseApplicability.fromNBT(
            nbt.getInteger(NBT_KEY_APPLICABILITY)));

    // Deserialize off signals
    phase.offSignals.addAll(
        SerializationUtils.getBlockPosListFromBlockPosNBTArray(
            nbt.getTag(NBT_KEY_OFF_SIGNAL_LIST)));

    // Deserialize flashing yellow arrow signals
    phase.fyaSignals.addAll(
        SerializationUtils.getBlockPosListFromBlockPosNBTArray(
            nbt.getTag(NBT_KEY_FYA_SIGNAL_LIST)));

    // Deserialize green signals
    phase.greenSignals.addAll(
        SerializationUtils.getBlockPosListFromBlockPosNBTArray(
            nbt.getTag(NBT_KEY_GREEN_SIGNAL_LIST)));

    // Deserialize yellow signals
    phase.yellowSignals.addAll(
        SerializationUtils.getBlockPosListFromBlockPosNBTArray(
            nbt.getTag(NBT_KEY_YELLOW_SIGNAL_LIST)));

    // Deserialize red signals
    phase.redSignals.addAll(
        SerializationUtils.getBlockPosListFromBlockPosNBTArray(
            nbt.getTag(NBT_KEY_RED_SIGNAL_LIST)));

    // Deserialize walk signals
    phase.walkSignals.addAll(
        SerializationUtils.getBlockPosListFromBlockPosNBTArray(
            nbt.getTag(NBT_KEY_WALK_SIGNAL_LIST)));

    // Deserialize flash don't walk signals
    phase.flashDontWalkSignals.addAll(SerializationUtils.getBlockPosListFromBlockPosNBTArray(
        nbt.getTag(NBT_KEY_FLASH_DONT_WALK_SIGNAL_LIST)));

    // Deserialize don't walk signals
    phase.dontWalkSignals.addAll(
        SerializationUtils.getBlockPosListFromBlockPosNBTArray(
            nbt.getTag(NBT_KEY_DONT_WALK_SIGNAL_LIST)));

    // Return the phase
    return phase;
  }

  /**
   * Gets a {@link TrafficSignalPhase} object from the given {@link NBTTagCompound}.
   *
   * @param nbt The {@link NBTTagCompound} containing the data for the {@link TrafficSignalPhase}
   *            object in NBT format.
   *
   * @return The {@link TrafficSignalPhase} object from the given {@link NBTTagCompound}.
   *
   * @throws IllegalArgumentException If the given {@link NBTTagCompound} is null.
   * @see #toNBT()
   * @since 1.0
   */
  public static TrafficSignalPhase fromNBT(NBTTagCompound nbt) {
    return fromNBT(nbt, false);
  }

  /**
   * Gets the list of {@link BlockPos}es of signals which are in the 'off' state during this phase.
   *
   * @return The list of {@link BlockPos}es of signals which are in the 'off' state during this
   *     phase.
   *
   * @since 1.0
   */
  public ArrayList<BlockPos> getOffSignals() {
    return this.offSignals;
  }

  /**
   * Gets the list of {@link BlockPos}es of signals which are in the 'fya' state during this phase.
   *
   * @return The list of {@link BlockPos}es of signals which are in the 'fya' state during this
   *     phase.
   *
   * @since 1.0
   */
  public ArrayList<BlockPos> getFyaSignals() {
    return this.fyaSignals;
  }

  /**
   * Gets the list of {@link BlockPos}es of signals which are in the 'green' state during this
   * phase.
   *
   * @return The list of {@link BlockPos}es of signals which are in the 'green' state during this
   *     phase.
   *
   * @since 1.0
   */
  public ArrayList<BlockPos> getGreenSignals() {
    return this.greenSignals;
  }

  /**
   * Gets the list of {@link BlockPos}es of signals which are in the 'yellow' state during this
   * phase.
   *
   * @return The list of {@link BlockPos}es of signals which are in the 'yellow' state during this
   *     phase.
   *
   * @since 1.0
   */
  public ArrayList<BlockPos> getYellowSignals() {
    return this.yellowSignals;
  }

  /**
   * Gets the list of {@link BlockPos}es of signals which are in the 'red' state during this phase.
   *
   * @return The list of {@link BlockPos}es of signals which are in the 'red' state during this
   *     phase.
   *
   * @since 1.0
   */
  public ArrayList<BlockPos> getRedSignals() {
    return this.redSignals;
  }

  /**
   * Gets the list of {@link BlockPos}es of signals which are in the 'walk' state during this
   * phase.
   *
   * @return The list of {@link BlockPos}es of signals which are in the 'walk' state during this
   *     phase.
   *
   * @since 1.0
   */
  public ArrayList<BlockPos> getWalkSignals() {
    return this.walkSignals;
  }

  /**
   * Gets the list of {@link BlockPos}es of signals which are in the 'flashing don't walk' state
   * during this phase.
   *
   * @return The list of {@link BlockPos}es of signals which are in the 'flashing don't walk' state
   *     during this phase.
   *
   * @since 1.0
   */
  public ArrayList<BlockPos> getFlashDontWalkSignals() {
    return this.flashDontWalkSignals;
  }

  /**
   * Gets the list of {@link BlockPos}es of signals which are in the 'don't walk' state during this
   * phase.
   *
   * @return The list of {@link BlockPos}es of signals which are in the 'don't walk' state during
   *     this phase.
   *
   * @since 1.0
   */
  public ArrayList<BlockPos> getDontWalkSignals() {
    return this.dontWalkSignals;
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
   * Gets the upcoming phase for this phase. This is the phase which will be transitioned to after
   * this phase, or subsequent required phases, have been completed.
   *
   * @return The upcoming phase for this phase. This is the phase which will be transitioned to
   *     after this phase, or subsequent required phases, have been completed.
   *
   * @since 1.0
   */
  public TrafficSignalPhase getUpcomingPhase() {
    return upcomingPhase;
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
   * Gets the priority indicator for this phase. This is a {@link Tuple} of the circuit which is
   * being serviced by this phase, or -1 if this phase is not associated with a circuit, and the
   * applicability of this phase to the given circuit.
   *
   * @return The priority indicator for this phase. This is a {@link Tuple} of the circuit which is
   *     being serviced by this phase, or -1 if this phase is not associated with a circuit, and the
   *     applicability of this phase to the given circuit.
   *
   * @since 1.0
   */
  public Tuple<Integer, TrafficSignalPhaseApplicability> getPriorityIndicator() {
    return new Tuple<>(circuit, applicability);
  }

  /**
   * Adds the given {@link BlockPos} to the list of signals which are in the 'off' state during this
   * phase.
   *
   * @param pos The {@link BlockPos} to add to the list of signals which are in the 'off' state
   *            during this phase.
   *
   * @return true if the given {@link BlockPos} was added to the list of signals which are in the
   *     'off' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addOffSignal(BlockPos pos) {
    return offSignals.add(pos);
  }

  /**
   * Adds the given {@link BlockPos} to the list of signals which are in the 'fya' state during this
   * phase.
   *
   * @param pos The {@link BlockPos} to add to the list of signals which are in the 'fya' state
   *            during this phase.
   *
   * @return true if the given {@link BlockPos} was added to the list of signals which are in the
   *     'fya' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addFyaSignal(BlockPos pos) {
    return fyaSignals.add(pos);
  }

  /**
   * Adds the given {@link BlockPos} to the list of signals which are in the 'green' state during
   * this phase.
   *
   * @param pos The {@link BlockPos} to add to the list of signals which are in the 'green' state
   *            during this phase.
   *
   * @return true if the given {@link BlockPos} was added to the list of signals which are in the
   *     'green' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addGreenSignal(BlockPos pos) {
    return greenSignals.add(pos);
  }

  /**
   * Adds the given {@link BlockPos} to the list of signals which are in the 'yellow' state during
   * this phase.
   *
   * @param pos The {@link BlockPos} to add to the list of signals which are in the 'yellow' state
   *            during this phase.
   *
   * @return true if the given {@link BlockPos} was added to the list of signals which are in the
   *     'yellow' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addYellowSignal(BlockPos pos) {
    return yellowSignals.add(pos);
  }

  /**
   * Adds the given {@link BlockPos} to the list of signals which are in the 'red' state during this
   * phase.
   *
   * @param pos The {@link BlockPos} to add to the list of signals which are in the 'red' state
   *            during this phase.
   *
   * @return true if the given {@link BlockPos} was added to the list of signals which are in the
   *     'red' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addRedSignal(BlockPos pos) {
    return redSignals.add(pos);
  }

  /**
   * Adds the given {@link BlockPos} to the list of signals which are in the 'walk' state during
   * this phase.
   *
   * @param pos The {@link BlockPos} to add to the list of signals which are in the 'walk' state
   *            during this phase.
   *
   * @return true if the given {@link BlockPos} was added to the list of signals which are in the
   *     'walk' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addWalkSignal(BlockPos pos) {
    return walkSignals.add(pos);
  }

  /**
   * Adds the given {@link BlockPos} to the list of signals which are in the 'flashing don't walk'
   * state during this phase.
   *
   * @param pos The {@link BlockPos} to add to the list of signals which are in the 'flashing don't
   *            walk' state during this phase.
   *
   * @return true if the given {@link BlockPos} was added to the list of signals which are in the
   *     'flashing don't walk' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addFlashDontWalkSignal(BlockPos pos) {
    return flashDontWalkSignals.add(pos);
  }

  /**
   * Adds the given {@link BlockPos} to the list of signals which are in the 'don't walk' state
   * during this phase.
   *
   * @param pos The {@link BlockPos} to add to the list of signals which are in the 'don't walk'
   *            state during this phase.
   *
   * @return true if the given {@link BlockPos} was added to the list of signals which are in the
   *     'don't walk' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addDontWalkSignal(BlockPos pos) {
    return dontWalkSignals.add(pos);
  }

  /**
   * Adds the given list of {@link BlockPos}s to the list of signals which are in the 'off' state
   * during this phase.
   *
   * @param pos The list of {@link BlockPos}s to add to the list of signals which are in the 'off'
   *            state during this phase.
   *
   * @return true if the given list of {@link BlockPos}s was added to the list of signals which are
   *     in the 'off' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addOffSignals(List<BlockPos> pos) {
    return offSignals.addAll(pos);
  }

  /**
   * Adds the given list of {@link BlockPos}s to the list of signals which are in the 'fya' state
   * during this phase.
   *
   * @param pos The list of {@link BlockPos}s to add to the list of signals which are in the 'fya'
   *            state during this phase.
   *
   * @return true if the given list of {@link BlockPos}s was added to the list of signals which are
   *     in the 'fya' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addFyaSignals(List<BlockPos> pos) {
    return fyaSignals.addAll(pos);
  }

  /**
   * Adds the given list of {@link BlockPos}s to the list of signals which are in the 'green' state
   * during this phase.
   *
   * @param pos The list of {@link BlockPos}s to add to the list of signals which are in the 'green'
   *            state during this phase.
   *
   * @return true if the given list of {@link BlockPos}s was added to the list of signals which are
   *     in the 'green' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addGreenSignals(List<BlockPos> pos) {
    return greenSignals.addAll(pos);
  }

  /**
   * Adds the given list of {@link BlockPos}s to the list of signals which are in the 'yellow' state
   * during this phase.
   *
   * @param pos The list of {@link BlockPos}s to add to the list of signals which are in the
   *            'yellow' state during this phase.
   *
   * @return true if the given list of {@link BlockPos}s was added to the list of signals which are
   *     in the 'yellow' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addYellowSignals(List<BlockPos> pos) {
    return yellowSignals.addAll(pos);
  }

  /**
   * Adds the given list of {@link BlockPos}s to the list of signals which are in the 'red' state
   * during this phase.
   *
   * @param pos The list of {@link BlockPos}s to add to the list of signals which are in the 'red'
   *            state during this phase.
   *
   * @return true if the given list of {@link BlockPos}s was added to the list of signals which are
   *     in the 'red' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addRedSignals(List<BlockPos> pos) {
    return redSignals.addAll(pos);
  }

  /**
   * Adds the given list of {@link BlockPos}s to the list of signals which are in the 'walk' state
   * during this phase.
   *
   * @param pos The list of {@link BlockPos}s to add to the list of signals which are in the 'walk'
   *            state during this phase.
   *
   * @return true if the given list of {@link BlockPos}s was added to the list of signals which are
   *     in the 'walk' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addWalkSignals(List<BlockPos> pos) {
    return walkSignals.addAll(pos);
  }

  /**
   * Adds the given list of {@link BlockPos}s to the list of signals which are in the 'flashing
   * don't walk' state during this phase.
   *
   * @param pos The list of {@link BlockPos}s to add to the list of signals which are in the
   *            'flashing don't walk' state during this phase.
   *
   * @return true if the given list of {@link BlockPos}s was added to the list of signals which are
   *     in the 'flashing don't walk' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addFlashDontWalkSignals(List<BlockPos> pos) {
    return flashDontWalkSignals.addAll(pos);
  }

  /**
   * Adds the given list of {@link BlockPos}s to the list of signals which are in the 'don't walk'
   * state during this phase.
   *
   * @param pos The list of {@link BlockPos}s to add to the list of signals which are in the 'don't
   *            walk' state during this phase.
   *
   * @return true if the given list of {@link BlockPos}s was added to the list of signals which are
   *     in the 'don't walk' state during this phase; false otherwise.
   *
   * @since 1.0
   */
  public boolean addDontWalkSignals(List<BlockPos> pos) {
    return dontWalkSignals.addAll(pos);
  }

  // endregion

  // region: Serialization Methods

  /**
   * Moves the given {@link BlockPos} of an overlap signal to the 'green' state. If the given
   * {@link BlockPos} was in any other state, it will be removed from that state and added to the
   * 'green' state.
   *
   * @param pos The {@link BlockPos} of the overlap signal to move to the 'green' state.
   *
   * @return true if the given {@link BlockPos} was moved from another state to the 'green' state;
   *     false otherwise.
   */
  public boolean moveOverlapSignalToGreen(BlockPos pos) {
    boolean moved = false;
    if (!greenSignals.contains(pos)) {
      if (redSignals.remove(pos)) {
        moved = greenSignals.add(pos);
      } else if (yellowSignals.remove(pos)) {
        moved = greenSignals.add(pos);
      } else if (fyaSignals.remove(pos)) {
        moved = greenSignals.add(pos);
      } else if (walkSignals.remove(pos)) {
        moved = greenSignals.add(pos);
      } else if (flashDontWalkSignals.remove(pos)) {
        moved = greenSignals.add(pos);
      } else if (dontWalkSignals.remove(pos)) {
        moved = greenSignals.add(pos);
      } else if (offSignals.remove(pos)) {
        moved = greenSignals.add(pos);
      }
    }

    return moved;
  }

  /**
   * Applies the {@link TrafficSignalPhase} to the given {@link World}.
   *
   * @param world The {@link World} to apply the {@link TrafficSignalPhase} to.
   *
   * @since 1.0
   */
  public void apply(World world) {
    greenSignals.forEach(pos -> AbstractBlockControllableSignal.changeSignalColor(world, pos,
        AbstractBlockControllableSignal.SIGNAL_GREEN));
    yellowSignals.forEach(pos -> AbstractBlockControllableSignal.changeSignalColor(world, pos,
        AbstractBlockControllableSignal.SIGNAL_YELLOW));
    redSignals.forEach(pos -> AbstractBlockControllableSignal.changeSignalColor(world, pos,
        AbstractBlockControllableSignal.SIGNAL_RED));
    offSignals.forEach(pos -> AbstractBlockControllableSignal.changeSignalColor(world, pos,
        AbstractBlockControllableSignal.SIGNAL_OFF));
    fyaSignals.forEach(pos -> AbstractBlockControllableSignal.changeSignalColor(world, pos,
        AbstractBlockControllableSignal.SIGNAL_GREEN));
    walkSignals.forEach(pos -> AbstractBlockControllableSignal.changeSignalColor(world, pos,
        AbstractBlockControllableSignal.SIGNAL_GREEN));
    flashDontWalkSignals.forEach(
        pos -> AbstractBlockControllableSignal.changeSignalColor(world, pos,
            AbstractBlockControllableSignal.SIGNAL_YELLOW));
    dontWalkSignals.forEach(pos -> AbstractBlockControllableSignal.changeSignalColor(world, pos,
        AbstractBlockControllableSignal.SIGNAL_RED));
  }

  /**
   * Gets an {@link NBTTagCompound} containing the data for this phase in NBT format.
   *
   * @return The {@link NBTTagCompound} containing the data for this phase in NBT format.
   *     <p>
   *     The returned {@link NBTTagCompound} can be used to reconstruct the
   *     {@link TrafficSignalPhase} object.
   *     </p>
   *
   * @see #fromNBT(NBTTagCompound)
   * @since 1.0
   */
  public NBTTagCompound toNBT() {
    // Create the compound
    NBTTagCompound compound = new NBTTagCompound();

    // Serialize off signals
    compound.setTag(NBT_KEY_OFF_SIGNAL_LIST,
        SerializationUtils.getBlockPosNBTArrayFromBlockPosList(offSignals));

    // Serialize flashing yellow arrow signals
    compound.setTag(NBT_KEY_FYA_SIGNAL_LIST,
        SerializationUtils.getBlockPosNBTArrayFromBlockPosList(fyaSignals));

    // Serialize green signals
    compound.setTag(NBT_KEY_GREEN_SIGNAL_LIST,
        SerializationUtils.getBlockPosNBTArrayFromBlockPosList(greenSignals));

    // Serialize yellow signals
    compound.setTag(NBT_KEY_YELLOW_SIGNAL_LIST,
        SerializationUtils.getBlockPosNBTArrayFromBlockPosList(yellowSignals));

    // Serialize red signals
    compound.setTag(NBT_KEY_RED_SIGNAL_LIST,
        SerializationUtils.getBlockPosNBTArrayFromBlockPosList(redSignals));

    // Serialize walk signals
    compound.setTag(NBT_KEY_WALK_SIGNAL_LIST,
        SerializationUtils.getBlockPosNBTArrayFromBlockPosList(walkSignals));

    // Serialize flash don't walk signals
    compound.setTag(NBT_KEY_FLASH_DONT_WALK_SIGNAL_LIST,
        SerializationUtils.getBlockPosNBTArrayFromBlockPosList(flashDontWalkSignals));

    // Serialize don't walk signals
    compound.setTag(NBT_KEY_DONT_WALK_SIGNAL_LIST,
        SerializationUtils.getBlockPosNBTArrayFromBlockPosList(dontWalkSignals));

    // Serialize the circuit
    compound.setInteger(NBT_KEY_CIRCUIT, circuit);

    // Serialize the upcoming phase
    if (upcomingPhase != null) {
      compound.setTag(NBT_KEY_UPCOMING_PHASE, upcomingPhase.toNBT());
    }

    // Serialize the applicability
    compound.setInteger(NBT_KEY_APPLICABILITY, applicability.toNBT());

    // Return the compound
    return compound;
  }

  /**
   * Gets the hash code of this {@link TrafficSignalPhase}.
   *
   * @return The hash code of this {@link TrafficSignalPhase}.
   *
   * @since 1.0
   */
  @Override
  public int hashCode() {
    return Objects.hash(offSignals, fyaSignals, yellowSignals, redSignals, walkSignals,
        flashDontWalkSignals,
        dontWalkSignals, circuit, upcomingPhase, applicability);
  }

  /**
   * Checks if the given {@link Object} is equal to this {@link TrafficSignalPhase}.
   *
   * @param o The {@link Object} to check.
   *
   * @return {@code true} if the given {@link Object} is equal to this {@link TrafficSignalPhase};
   *     {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TrafficSignalPhase that = (TrafficSignalPhase) o;
    return circuit == that.circuit &&
        Objects.equals(upcomingPhase, that.upcomingPhase) &&
        Objects.equals(offSignals, that.offSignals) &&
        Objects.equals(fyaSignals, that.fyaSignals) &&
        Objects.equals(greenSignals, that.greenSignals) &&
        Objects.equals(yellowSignals, that.yellowSignals) &&
        Objects.equals(redSignals, that.redSignals) &&
        Objects.equals(walkSignals, that.walkSignals) &&
        Objects.equals(flashDontWalkSignals, that.flashDontWalkSignals) &&
        Objects.equals(dontWalkSignals, that.dontWalkSignals) &&
        applicability == that.applicability;
  }

  // endregion
}
