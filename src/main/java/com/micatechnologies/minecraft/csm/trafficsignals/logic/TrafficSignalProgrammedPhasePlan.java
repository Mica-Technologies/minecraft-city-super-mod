package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The full {@code ADVANCED}-mode program: the 8 NEMA {@link TrafficSignalProgrammedPhase}s, the
 * per-ring phase sequence (so lead/lag lefts are configurable), the {@link
 * TrafficSignalCoordinationPlan}, and the {@link TrafficSignalPreempt} table.
 *
 * <p>The standard dual-ring structure is the textbook 8-phase layout:
 *
 * <pre>
 *         BARRIER A          BARRIER B
 *  Ring 1 | p1   p2     |   p3   p4
 *  Ring 2 | p5   p6     |   p7   p8
 * </pre>
 *
 * Even phases (2,4,6,8) default to {@code THROUGH}; odd phases (1,3,5,7) default to
 * {@code PROTECTED_LEFT}. Phases are unassigned (circuit -1, disabled) until the operator maps them
 * to circuits — either by hand or via the "Load Standard 8-Phase" auto-template (added in the
 * actuated-core milestone).
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public class TrafficSignalProgrammedPhasePlan {

  /** Number of NEMA phases supported (standard dual-ring). */
  public static final int PHASE_COUNT = 8;

  /** Standard ring 1 phase numbers. */
  public static final int[] DEFAULT_RING_1 = {1, 2, 3, 4};
  /** Standard ring 2 phase numbers. */
  public static final int[] DEFAULT_RING_2 = {5, 6, 7, 8};

  private static final String K_PHASES = "ph";
  private static final String K_RING_1 = "r1";
  private static final String K_RING_2 = "r2";
  private static final String K_COORDINATION = "co";
  private static final String K_PREEMPTS = "pe";

  private final List<TrafficSignalProgrammedPhase> phases;
  private int[] ring1Sequence;
  private int[] ring2Sequence;
  private TrafficSignalCoordinationPlan coordination;
  private final List<TrafficSignalPreempt> preempts;

  private TrafficSignalProgrammedPhasePlan(List<TrafficSignalProgrammedPhase> phases,
      int[] ring1Sequence, int[] ring2Sequence, TrafficSignalCoordinationPlan coordination,
      List<TrafficSignalPreempt> preempts) {
    this.phases = phases;
    this.ring1Sequence = ring1Sequence;
    this.ring2Sequence = ring2Sequence;
    this.coordination = coordination;
    this.preempts = preempts;
  }

  /**
   * Builds a fresh plan with the standard 8-phase dual-ring structure, every phase unassigned and
   * disabled. The operator (or the auto-template) fills in circuit assignments afterward.
   *
   * @return a new default plan.
   */
  public static TrafficSignalProgrammedPhasePlan createDefault() {
    List<TrafficSignalProgrammedPhase> phases = new ArrayList<>(PHASE_COUNT);
    for (int n = 1; n <= PHASE_COUNT; n++) {
      int ring = n <= 4 ? 1 : 2;
      // Barrier A holds {1,2,5,6}; barrier B holds {3,4,7,8}.
      int withinRing = ((n - 1) % 4); // 0..3
      int barrier = withinRing < 2 ? 0 : 1;
      TrafficSignalProgrammedPhase phase = new TrafficSignalProgrammedPhase(n, ring, barrier);
      // Even = through, odd = protected left (standard NEMA convention).
      phase.setMovement(n % 2 == 0
          ? TrafficSignalPhaseMovement.THROUGH
          : TrafficSignalPhaseMovement.PROTECTED_LEFT);
      phases.add(phase);
    }
    return new TrafficSignalProgrammedPhasePlan(phases, DEFAULT_RING_1.clone(),
        DEFAULT_RING_2.clone(), new TrafficSignalCoordinationPlan(), new ArrayList<>());
  }

  // region: Accessors

  public List<TrafficSignalProgrammedPhase> getPhases() {
    return phases;
  }

  /**
   * @param phaseNumber the 1-based NEMA phase number
   *
   * @return the phase with that number, or {@code null} if not present.
   */
  public TrafficSignalProgrammedPhase getPhase(int phaseNumber) {
    for (TrafficSignalProgrammedPhase p : phases) {
      if (p.getPhaseNumber() == phaseNumber) {
        return p;
      }
    }
    return null;
  }

  public int[] getRing1Sequence() {
    return ring1Sequence;
  }

  public void setRing1Sequence(int[] ring1Sequence) {
    this.ring1Sequence = ring1Sequence == null ? new int[0] : ring1Sequence;
  }

  public int[] getRing2Sequence() {
    return ring2Sequence;
  }

  public void setRing2Sequence(int[] ring2Sequence) {
    this.ring2Sequence = ring2Sequence == null ? new int[0] : ring2Sequence;
  }

  /**
   * @param ring the ring number (1 or 2)
   *
   * @return that ring's phase sequence (defensive: ring 2 for 2, otherwise ring 1).
   */
  public int[] getRingSequence(int ring) {
    return ring == 2 ? ring2Sequence : ring1Sequence;
  }

  public TrafficSignalCoordinationPlan getCoordination() {
    return coordination;
  }

  public List<TrafficSignalPreempt> getPreempts() {
    return preempts;
  }

  /**
   * @return {@code true} if at least one phase is enabled and assigned to a circuit (i.e. the plan
   *     can actually run).
   */
  public boolean isConfigured() {
    for (TrafficSignalProgrammedPhase p : phases) {
      if (p.isActive()) {
        return true;
      }
    }
    return false;
  }

  // endregion

  // region: Auto-template + validation

  /**
   * Auto-assigns the standard 8-phase plan onto the controller's circuits by reading each circuit's
   * through-signal facing. North/South approaches become the major street (through phases 2 &amp; 6
   * with lefts 1 &amp; 5); East/West become the minor street (through phases 4 &amp; 8 with lefts 3
   * &amp; 7). A left phase is enabled only when its circuit actually has protected/left signals.
   *
   * <p>Assumes one circuit per approach (the recommended ADVANCED-mode setup). Circuits beyond the
   * first two of each axis are left unassigned.
   *
   * @param circuits the controller circuits
   * @param world    the world (for reading signal facing)
   */
  public void loadStandardEightPhase(TrafficSignalControllerCircuits circuits, World world) {
    List<Integer> major = new ArrayList<>(); // N/S approaches
    List<Integer> minor = new ArrayList<>(); // E/W approaches
    for (int i = 0; i < circuits.getCircuitCount(); i++) {
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(i);
      if (circuit.getThroughSignals().isEmpty()) {
        continue;
      }
      EnumFacing facing = readFacing(world, circuit.getThroughSignals().get(0));
      if (facing == null) {
        continue;
      }
      if (facing.getAxis() == EnumFacing.Axis.Z) {
        major.add(i);
      } else if (facing.getAxis() == EnumFacing.Axis.X) {
        minor.add(i);
      }
    }

    // Clear all assignments first so re-running the template is idempotent.
    for (TrafficSignalProgrammedPhase p : phases) {
      p.setCircuitIndex(-1);
      p.setEnabled(false);
    }

    // Major street: through 2/6, lefts 1/5; longer max greens.
    if (major.size() > 0) {
      assignApproach(circuits, major.get(0), 2, 1, 1200L);
    }
    if (major.size() > 1) {
      assignApproach(circuits, major.get(1), 6, 5, 1200L);
    }
    // Minor street: through 4/8, lefts 3/7; shorter max greens.
    if (minor.size() > 0) {
      assignApproach(circuits, minor.get(0), 4, 3, 600L);
    }
    if (minor.size() > 1) {
      assignApproach(circuits, minor.get(1), 8, 7, 600L);
    }
  }

  private void assignApproach(TrafficSignalControllerCircuits circuits, int circuitIndex,
      int throughPhaseNumber, int leftPhaseNumber, long throughMaxGreen) {
    TrafficSignalControllerCircuit circuit = circuits.getCircuit(circuitIndex);
    TrafficSignalProgrammedPhase through = getPhase(throughPhaseNumber);
    if (through != null) {
      through.setCircuitIndex(circuitIndex);
      through.setMovement(TrafficSignalPhaseMovement.THROUGH);
      through.setMaxGreen(throughMaxGreen);
      through.setEnabled(true);
    }
    boolean hasLeft = !circuit.getProtectedSignals().isEmpty()
        || !circuit.getLeftSignals().isEmpty();
    TrafficSignalProgrammedPhase left = getPhase(leftPhaseNumber);
    if (left != null && hasLeft) {
      left.setCircuitIndex(circuitIndex);
      left.setMovement(TrafficSignalPhaseMovement.PROTECTED_LEFT);
      left.setMaxGreen(300L);
      left.setEnabled(true);
    }
  }

  private static EnumFacing readFacing(World world, BlockPos pos) {
    if (world == null || !world.isBlockLoaded(pos)) {
      return null;
    }
    IBlockState state = world.getBlockState(pos);
    if (state.getProperties().containsKey(AbstractBlockRotatableNSEW.FACING)) {
      return state.getValue(AbstractBlockRotatableNSEW.FACING);
    }
    return null;
  }

  /**
   * Validates the plan against the controller's circuits. Returns a human-readable error message
   * describing the first problem found, or {@code null} if the plan is usable.
   *
   * @param circuits the controller circuits
   *
   * @return the first validation error, or {@code null}
   */
  public String validate(TrafficSignalControllerCircuits circuits) {
    if (!isConfigured()) {
      return "Advanced mode: no phases are enabled/assigned. Map phases to circuits.";
    }
    for (TrafficSignalProgrammedPhase p : phases) {
      if (!p.isEnabled()) {
        continue;
      }
      int ci = p.getCircuitIndex();
      if (ci < 0 || ci >= circuits.getCircuitCount()) {
        return "Phase " + p.getPhaseNumber() + " is enabled but not assigned to a valid circuit.";
      }
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(ci);
      boolean hasSignals;
      switch (p.getMovement()) {
        case THROUGH:
          hasSignals = !circuit.getThroughSignals().isEmpty();
          break;
        case LEFT:
        case PROTECTED_LEFT:
          hasSignals = !circuit.getProtectedSignals().isEmpty()
              || !circuit.getLeftSignals().isEmpty();
          break;
        case RIGHT:
          hasSignals = !circuit.getRightSignals().isEmpty();
          break;
        case PED:
          hasSignals = !circuit.getPedestrianSignals().isEmpty();
          break;
        default:
          hasSignals = false;
          break;
      }
      if (!hasSignals) {
        return "Phase " + p.getPhaseNumber() + " (" + p.getMovement().getName()
            + ") has no matching signals on circuit " + (ci + 1) + ".";
      }
    }
    return null;
  }

  // endregion

  // region: NBT

  public NBTTagCompound toNBT() {
    NBTTagCompound c = new NBTTagCompound();
    NBTTagList phaseList = new NBTTagList();
    for (TrafficSignalProgrammedPhase p : phases) {
      phaseList.appendTag(p.toNBT());
    }
    c.setTag(K_PHASES, phaseList);
    c.setIntArray(K_RING_1, ring1Sequence);
    c.setIntArray(K_RING_2, ring2Sequence);
    c.setTag(K_COORDINATION, coordination.toNBT());
    NBTTagList preemptList = new NBTTagList();
    for (TrafficSignalPreempt p : preempts) {
      preemptList.appendTag(p.toNBT());
    }
    c.setTag(K_PREEMPTS, preemptList);
    return c;
  }

  public static TrafficSignalProgrammedPhasePlan fromNBT(NBTTagCompound c) {
    List<TrafficSignalProgrammedPhase> phases = new ArrayList<>();
    if (c.hasKey(K_PHASES)) {
      NBTTagList phaseList = c.getTagList(K_PHASES, 10); // 10 = TAG_COMPOUND
      for (int i = 0; i < phaseList.tagCount(); i++) {
        phases.add(TrafficSignalProgrammedPhase.fromNBT(phaseList.getCompoundTagAt(i)));
      }
    }
    // Safety: if a saved plan is missing phases, fall back to the default structure.
    if (phases.isEmpty()) {
      return createDefault();
    }
    int[] ring1 = c.hasKey(K_RING_1) ? c.getIntArray(K_RING_1) : DEFAULT_RING_1.clone();
    int[] ring2 = c.hasKey(K_RING_2) ? c.getIntArray(K_RING_2) : DEFAULT_RING_2.clone();
    TrafficSignalCoordinationPlan coordination = c.hasKey(K_COORDINATION)
        ? TrafficSignalCoordinationPlan.fromNBT(c.getCompoundTag(K_COORDINATION))
        : new TrafficSignalCoordinationPlan();
    List<TrafficSignalPreempt> preempts = new ArrayList<>();
    if (c.hasKey(K_PREEMPTS)) {
      NBTTagList preemptList = c.getTagList(K_PREEMPTS, 10);
      for (int i = 0; i < preemptList.tagCount(); i++) {
        preempts.add(TrafficSignalPreempt.fromNBT(preemptList.getCompoundTagAt(i)));
      }
    }
    return new TrafficSignalProgrammedPhasePlan(phases, ring1, ring2, coordination, preempts);
  }

  // endregion
}
