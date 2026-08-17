package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.SerializationUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  // Short-form NBT keys used by this phase's toNBT / fromNBT. LEGACY_* constants preserve the
  // historical long key names for back-compat reads only.

  /** @since 1.0 */
  private static final String NBT_KEY_OFF_SIGNAL_LIST = "oS";
  private static final String LEGACY_NBT_KEY_OFF_SIGNAL_LIST = "offSignalList";

  /** @since 1.0 */
  private static final String NBT_KEY_FYA_SIGNAL_LIST = "fyS";
  private static final String LEGACY_NBT_KEY_FYA_SIGNAL_LIST = "fyaSignalList";

  /** @since 1.0 */
  private static final String NBT_KEY_GREEN_SIGNAL_LIST = "gS";
  private static final String LEGACY_NBT_KEY_GREEN_SIGNAL_LIST = "greenSignalList";

  /** @since 1.0 */
  private static final String NBT_KEY_YELLOW_SIGNAL_LIST = "yS";
  private static final String LEGACY_NBT_KEY_YELLOW_SIGNAL_LIST = "yellowSignalList";

  /** @since 1.0 */
  private static final String NBT_KEY_RED_SIGNAL_LIST = "rS";
  private static final String LEGACY_NBT_KEY_RED_SIGNAL_LIST = "redSignalList";

  /** @since 1.0 */
  private static final String NBT_KEY_WALK_SIGNAL_LIST = "wS";
  private static final String LEGACY_NBT_KEY_WALK_SIGNAL_LIST = "walkSignalList";

  /** @since 1.0 */
  private static final String NBT_KEY_FLASH_DONT_WALK_SIGNAL_LIST = "fdw";
  private static final String LEGACY_NBT_KEY_FLASH_DONT_WALK_SIGNAL_LIST = "flashDontWalkSignalList";

  /** @since 1.0 */
  private static final String NBT_KEY_DONT_WALK_SIGNAL_LIST = "dw";
  private static final String LEGACY_NBT_KEY_DONT_WALK_SIGNAL_LIST = "dontWalkSignalList";

  /** @since 1.0 */
  private static final String NBT_KEY_CIRCUIT = "c";
  private static final String LEGACY_NBT_KEY_CIRCUIT = "circuit";

  /** @since 1.0 */
  private static final String NBT_KEY_UPCOMING_PHASE = "uP";
  private static final String LEGACY_NBT_KEY_UPCOMING_PHASE = "upcomingPhase";

  /** @since 1.0 */
  private static final String NBT_KEY_APPLICABILITY = "ap";
  private static final String LEGACY_NBT_KEY_APPLICABILITY = "applicability";

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
    String upcomingKey = null;
    if (nbt.hasKey(NBT_KEY_UPCOMING_PHASE)) {
      upcomingKey = NBT_KEY_UPCOMING_PHASE;
    } else if (nbt.hasKey(LEGACY_NBT_KEY_UPCOMING_PHASE)) {
      upcomingKey = LEGACY_NBT_KEY_UPCOMING_PHASE;
    }
    if (upcomingKey != null) {
      if (isUpcomingPhase) {
        throw new IllegalArgumentException(
            "The NBT cannot contain an upcoming phase with another nested upcoming phase.");
      } else {
        upcomingPhase = fromNBT(nbt.getCompoundTag(upcomingKey), true);
      }
    }
    // Create the phase
    TrafficSignalPhase phase = new TrafficSignalPhase(readInt(nbt, NBT_KEY_CIRCUIT,
        LEGACY_NBT_KEY_CIRCUIT), upcomingPhase,
        TrafficSignalPhaseApplicability.fromNBT(
            readInt(nbt, NBT_KEY_APPLICABILITY, LEGACY_NBT_KEY_APPLICABILITY)));

    // Deserialize each signal list (short-key preferred, legacy fallback)
    phase.offSignals.addAll(readPosList(nbt, NBT_KEY_OFF_SIGNAL_LIST,
        LEGACY_NBT_KEY_OFF_SIGNAL_LIST));
    phase.fyaSignals.addAll(readPosList(nbt, NBT_KEY_FYA_SIGNAL_LIST,
        LEGACY_NBT_KEY_FYA_SIGNAL_LIST));
    phase.greenSignals.addAll(readPosList(nbt, NBT_KEY_GREEN_SIGNAL_LIST,
        LEGACY_NBT_KEY_GREEN_SIGNAL_LIST));
    phase.yellowSignals.addAll(readPosList(nbt, NBT_KEY_YELLOW_SIGNAL_LIST,
        LEGACY_NBT_KEY_YELLOW_SIGNAL_LIST));
    phase.redSignals.addAll(readPosList(nbt, NBT_KEY_RED_SIGNAL_LIST,
        LEGACY_NBT_KEY_RED_SIGNAL_LIST));
    phase.walkSignals.addAll(readPosList(nbt, NBT_KEY_WALK_SIGNAL_LIST,
        LEGACY_NBT_KEY_WALK_SIGNAL_LIST));
    phase.flashDontWalkSignals.addAll(readPosList(nbt, NBT_KEY_FLASH_DONT_WALK_SIGNAL_LIST,
        LEGACY_NBT_KEY_FLASH_DONT_WALK_SIGNAL_LIST));
    phase.dontWalkSignals.addAll(readPosList(nbt, NBT_KEY_DONT_WALK_SIGNAL_LIST,
        LEGACY_NBT_KEY_DONT_WALK_SIGNAL_LIST));

    // Strip legacy long-form keys so subsequent writes emit only short-form output
    nbt.removeTag(LEGACY_NBT_KEY_OFF_SIGNAL_LIST);
    nbt.removeTag(LEGACY_NBT_KEY_FYA_SIGNAL_LIST);
    nbt.removeTag(LEGACY_NBT_KEY_GREEN_SIGNAL_LIST);
    nbt.removeTag(LEGACY_NBT_KEY_YELLOW_SIGNAL_LIST);
    nbt.removeTag(LEGACY_NBT_KEY_RED_SIGNAL_LIST);
    nbt.removeTag(LEGACY_NBT_KEY_WALK_SIGNAL_LIST);
    nbt.removeTag(LEGACY_NBT_KEY_FLASH_DONT_WALK_SIGNAL_LIST);
    nbt.removeTag(LEGACY_NBT_KEY_DONT_WALK_SIGNAL_LIST);
    nbt.removeTag(LEGACY_NBT_KEY_CIRCUIT);
    nbt.removeTag(LEGACY_NBT_KEY_UPCOMING_PHASE);
    nbt.removeTag(LEGACY_NBT_KEY_APPLICABILITY);

    // Return the phase
    return phase;
  }

  private static int readInt(NBTTagCompound nbt, String key, String legacyKey) {
    if (nbt.hasKey(key)) return nbt.getInteger(key);
    if (nbt.hasKey(legacyKey)) return nbt.getInteger(legacyKey);
    return 0;
  }

  private static List<BlockPos> readPosList(NBTTagCompound nbt, String shortKey,
      String legacyKey) {
    if (nbt.hasKey(shortKey)) {
      return SerializationUtils.getBlockPosListFromBlockPosNBTArray(nbt.getTag(shortKey));
    }
    if (nbt.hasKey(legacyKey)) {
      return SerializationUtils.getBlockPosListFromBlockPosNBTArray(nbt.getTag(legacyKey));
    }
    return new ArrayList<>();
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
   * Removes the given list of {@link BlockPos}s from ALL signal lists in this phase.
   * Used to re-assign signals to a different state after initial phase construction.
   */
  public void removeSignals(List<BlockPos> positions) {
    offSignals.removeAll(positions);
    greenSignals.removeAll(positions);
    yellowSignals.removeAll(positions);
    redSignals.removeAll(positions);
    fyaSignals.removeAll(positions);
    walkSignals.removeAll(positions);
    dontWalkSignals.removeAll(positions);
    flashDontWalkSignals.removeAll(positions);
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
    return moveOverlapSignalTo(pos, greenSignals);
  }

  /**
   * Moves the given {@link BlockPos} of an overlap signal to the 'yellow' state.
   */
  public boolean moveOverlapSignalToYellow(BlockPos pos) {
    return moveOverlapSignalTo(pos, yellowSignals);
  }

  /**
   * Moves the given {@link BlockPos} of an overlap signal to the 'red' state.
   */
  public boolean moveOverlapSignalToRed(BlockPos pos) {
    return moveOverlapSignalTo(pos, redSignals);
  }

  /**
   * Removes {@code pos} from every vehicle-signal state list other than {@code destination}
   * and adds it to {@code destination}. A position may legitimately be in more than one list
   * (bimodal FYA heads are linked as both a flashing and a plain turn signal), so every list
   * is checked rather than stopping at the first hit.
   *
   * @return true if the position was found in at least one other list and moved; false if it
   *     was not in any list or was already only in {@code destination}.
   */
  private boolean moveOverlapSignalTo(BlockPos pos, List<BlockPos> destination) {
    boolean removed = false;
    for (List<BlockPos> list : new List[] {greenSignals, yellowSignals, redSignals, offSignals,
        fyaSignals}) {
      if (list != destination) {
        removed |= list.remove(pos);
      }
    }
    if (removed && !destination.contains(pos)) {
      destination.add(pos);
    }
    return removed;
  }

  /**
   * Applies the {@link TrafficSignalPhase} to the given {@link World}. Sets all signals to their
   * designated colors, skipping any that are missing. Returns the position of the first missing
   * signal encountered, or null if all signals were set successfully.
   *
   * @param world The {@link World} to apply the {@link TrafficSignalPhase} to.
   *
   * @return The {@link BlockPos} of the first missing signal, or null if all succeeded.
   *
   * @since 1.0
   */
  public BlockPos apply(World world) {
    BlockPos firstMissing = null;
    // Vehicle signals: a position may be in more than one list (bimodal FYA heads are linked
    // as both a flashing and a plain turn signal, so a phase hands them e.g. OFF + GREEN).
    // Resolve each position to a single indication first, then write once per position.
    Map<BlockPos, Integer> resolved = resolveVehicleSignalStates();
    for (Map.Entry<BlockPos, Integer> entry : resolved.entrySet()) {
      BlockPos pos = entry.getKey();
      if (!world.isBlockLoaded(pos)) {
        continue;
      }
      int indication = entry.getValue();
      int color = indication == INDICATION_FYA ? AbstractBlockControllableSignal.SIGNAL_GREEN
          : indication;
      if (!AbstractBlockControllableSignal.changeSignalColor(world, pos, color,
          indication == INDICATION_FYA) && firstMissing == null) {
        firstMissing = pos;
      }
    }
    firstMissing = applyColorToSignals(world, walkSignals, AbstractBlockControllableSignal.SIGNAL_GREEN, firstMissing);
    firstMissing = applyColorToSignals(world, flashDontWalkSignals, AbstractBlockControllableSignal.SIGNAL_YELLOW, firstMissing);
    firstMissing = applyColorToSignals(world, dontWalkSignals, AbstractBlockControllableSignal.SIGNAL_RED, firstMissing);
    return firstMissing;
  }

  /** Pseudo-indication for the permissive flashing yellow arrow (distinct from solid green). */
  static final int INDICATION_FYA = 4;

  /**
   * Priority of each vehicle indication when a position appears in more than one state list.
   * Higher wins. GREEN > FYA > YELLOW > RED > OFF: for a bimodal FYA head the flashing-list
   * assignment and the plain-list assignment combine as protected = OFF + GREEN -> GREEN,
   * permissive = FYA + RED -> FYA, clearance = YELLOW + RED -> YELLOW, stopped = RED + RED ->
   * RED. Positions that appear in exactly one list (every legacy head) are unaffected.
   */
  private static int indicationPriority(int indication) {
    switch (indication) {
      case AbstractBlockControllableSignal.SIGNAL_GREEN:
        return 4;
      case INDICATION_FYA:
        return 3;
      case AbstractBlockControllableSignal.SIGNAL_YELLOW:
        return 2;
      case AbstractBlockControllableSignal.SIGNAL_RED:
        return 1;
      default:
        return 0;
    }
  }

  /**
   * Resolves the vehicle-signal state lists (green, yellow, red, off, FYA) to one indication per
   * position using {@link #indicationPriority(int)}. Insertion order follows the lists in
   * priority order so the write order is deterministic.
   *
   * @return map of position to indication ({@code SIGNAL_*} color, or {@link #INDICATION_FYA})
   *
   * @since 2026.8
   */
  Map<BlockPos, Integer> resolveVehicleSignalStates() {
    Map<BlockPos, Integer> resolved = new LinkedHashMap<>();
    mergeIndication(resolved, greenSignals, AbstractBlockControllableSignal.SIGNAL_GREEN);
    mergeIndication(resolved, fyaSignals, INDICATION_FYA);
    mergeIndication(resolved, yellowSignals, AbstractBlockControllableSignal.SIGNAL_YELLOW);
    mergeIndication(resolved, redSignals, AbstractBlockControllableSignal.SIGNAL_RED);
    mergeIndication(resolved, offSignals, AbstractBlockControllableSignal.SIGNAL_OFF);
    return resolved;
  }

  private static void mergeIndication(Map<BlockPos, Integer> resolved, List<BlockPos> signals,
      int indication) {
    for (BlockPos pos : signals) {
      Integer existing = resolved.get(pos);
      if (existing == null || indicationPriority(indication) > indicationPriority(existing)) {
        resolved.put(pos, indication);
      }
    }
  }

  /**
   * Helper method that applies a signal color to all positions in the given list, skipping signals
   * in unloaded chunks. Returns the first missing signal position encountered, or the previously
   * recorded first missing position if one was already found.
   *
   * @param world        The {@link World} to apply the signal color in.
   * @param signals      The list of {@link BlockPos} positions to apply the color to.
   * @param color        The signal color to apply.
   * @param firstMissing The previously recorded first missing signal position, or null.
   *
   * @return The first missing signal position, or null if all signals were set successfully.
   *
   * @since 1.0
   */
  private static BlockPos applyColorToSignals(World world, List<BlockPos> signals, int color,
      BlockPos firstMissing) {
    for (BlockPos pos : signals) {
      if (!world.isBlockLoaded(pos)) {
        continue;
      }
      if (!AbstractBlockControllableSignal.changeSignalColor(world, pos, color)
          && firstMissing == null) {
        firstMissing = pos;
      }
    }
    return firstMissing;
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
    return Objects.hash(offSignals, greenSignals, fyaSignals, yellowSignals, redSignals,
        walkSignals, flashDontWalkSignals,
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
