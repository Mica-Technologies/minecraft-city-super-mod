package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

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
