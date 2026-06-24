package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.World;

/**
 * Runtime state machine for {@code ADVANCED} (NEMA dual-ring, dual-barrier) operation. One instance
 * lives on the controller tile entity (transient — rebuilt on chunk load) and is advanced once per
 * controller tick via {@link #tick}.
 *
 * <p>Model: two rings each serve their phases in {@link TrafficSignalProgrammedPhasePlan#getRingSequence(int)
 * sequence} order. A phase from each ring may run concurrently as long as both are on the same
 * barrier; the rings advance through the sequence in lockstep across barriers and cross a barrier
 * only together. Each phase is actuated — held to its minimum green, extended by vehicle calls up
 * to its passage (gap) time, and forced off at its maximum green; pedestrian Walk/FDW run
 * concurrently with the served through. With no demand the controller rests in green on the
 * coordinated phases.
 *
 * <p>This milestone implements free/actuated operation. Coordination (cycle/offset/splits) and
 * preemption are layered on in later milestones.
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public class RingBarrierState {

  /** Vehicle indication a ring's active phase is currently displaying. */
  public enum VehInterval { GREEN, YELLOW, RED }

  /** Pedestrian indication accompanying a served phase. */
  public enum PedInterval { NONE, WALK, FDW, DONT_WALK }

  /** Stage of an in-progress preemption sequence. */
  private enum PreemptStage { NONE, ENTER, TRACK_CLEAR, DWELL, EXIT }

  // Fixed preempt interval timing (ticks). Entry clears conflicting movements (yellow then red).
  private static final long PREEMPT_YELLOW_TICKS = 70L;       // 3.5 s
  private static final long PREEMPT_REDCLEAR_TICKS = 40L;     // 2 s
  private static final long PREEMPT_TRACK_CLEAR_TICKS = 100L; // 5 s
  private static final long PREEMPT_EXIT_TICKS = 40L;         // 2 s

  /** Immutable description of what one ring is serving this tick (consumed by the phase builder). */
  public static final class ServedMovement {
    public final int phaseNumber;
    public final VehInterval vehicle;
    public final PedInterval pedestrian;

    public ServedMovement(int phaseNumber, VehInterval vehicle, PedInterval pedestrian) {
      this.phaseNumber = phaseNumber;
      this.vehicle = vehicle;
      this.pedestrian = pedestrian;
    }
  }

  /** Mutable per-ring runtime. */
  private static final class RingRuntime {
    int activePhase = 0;        // NEMA phase number currently served, or 0 = idle
    int sequencePos = -1;       // index into the ring's phase sequence
    VehInterval interval = VehInterval.RED;
    long intervalStart = 0L;    // world tick the current vehicle interval began
    long greenStart = 0L;       // world tick green began (for max-out)
    long lastActuation = 0L;    // world tick a vehicle call last held the phase (for gap-out)
    boolean resting = false;    // serving a coordinated rest phase (no max-out)
    boolean pedServing = false;
    long pedStart = 0L;
    // ASC/3 DLY GRN (delayed green / leading ped interval): while delayActive, the WALK runs but
    // the vehicle is held red and the green clocks are deferred. walkHold is the effective walk
    // length = max(walk, delayedGreen), so the walk extends to the end of the delay when the
    // delay is longer than the configured walk.
    boolean delayActive = false;
    long delayStart = 0L;
    long walkHold = 0L;
    // Volume-density snapshots captured at green start: the queue length (for added initial) and
    // whether a bike call was present (for bike minimum green).
    long queueAtStart = 0L;
    boolean bikeCall = false;
  }

  private int currentBarrier = 0;
  private final RingRuntime ring1 = new RingRuntime();
  private final RingRuntime ring2 = new RingRuntime();
  private boolean initialized = false;
  private TrafficSignalPhase lastApplied = null;

  // Preemption runtime.
  private PreemptStage preemptStage = PreemptStage.NONE;
  private int activePreemptIndex = -1;
  private long preemptStageStart = 0L;
  private final java.util.Set<Integer> preemptClearPhases = new java.util.HashSet<>();

  // Per-tick context / scratch (set at the top of each tick()).
  private World tickWorld;
  private TrafficSignalControllerCircuits tickCircuits;
  /** Source of detector/ped demand for this tick. World-backed in production, canned in tests. */
  private DemandSource demand;
  /** Last movements described this tick (exposed package-private for unit tests). */
  private ServedMovement lastServed1;
  private ServedMovement lastServed2;

  // Per-tick coordination state (computed from the plan each tick; all no-ops in FREE mode).
  private static final int PHASE_SLOTS = TrafficSignalProgrammedPhasePlan.PHASE_COUNT + 1;
  private boolean coordinated = false;
  private long localCycle = 0L;
  private final long[] windowStart = new long[PHASE_SLOTS];
  private final long[] windowEnd = new long[PHASE_SLOTS];
  private final boolean[] coordPhase = new boolean[PHASE_SLOTS];

  /**
   * Source of per-tick detector and pedestrian demand. Production wraps the world; unit tests
   * supply canned data so the ring-and-barrier engine can be exercised without a Minecraft world.
   */
  public interface DemandSource {
    /** Sensor summary for the given circuit index, or {@code null} if unavailable. */
    TrafficSignalSensorSummary summaryForCircuit(int circuitIndex);

    /** Whether a pedestrian (button) request is present on the given circuit. */
    boolean pedestrianRequest(int circuitIndex);
  }

  /** World-backed {@link DemandSource} used in production (caches summaries per tick). */
  private static final class WorldDemandSource implements DemandSource {
    private final World world;
    private final TrafficSignalControllerCircuits circuits;
    private final Map<Integer, TrafficSignalSensorSummary> cache = new HashMap<>();

    WorldDemandSource(World world, TrafficSignalControllerCircuits circuits) {
      this.world = world;
      this.circuits = circuits;
    }

    @Override
    public TrafficSignalSensorSummary summaryForCircuit(int ci) {
      if (world == null || circuits == null || ci < 0 || ci >= circuits.getCircuitCount()) {
        return null;
      }
      return cache.computeIfAbsent(ci,
          idx -> circuits.getCircuit(idx).getSensorsWaitingSummary(world));
    }

    @Override
    public boolean pedestrianRequest(int ci) {
      if (world == null || circuits == null || ci < 0 || ci >= circuits.getCircuitCount()) {
        return false;
      }
      return circuits.getCircuit(ci).getPedestrianAccessoriesRequestCount(world) > 0;
    }
  }

  /** The movement ring {@code n} (1 or 2) was last described as serving this tick, or null. */
  ServedMovement getLastServed(int ringNumber) {
    return ringNumber == 2 ? lastServed2 : lastServed1;
  }

  /**
   * Advances the controller one tick and returns the phase to apply, or {@code null} if the
   * displayed indication is unchanged since the last applied phase.
   */
  public TrafficSignalPhase tick(World world, TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits, TrafficSignalControllerOverlaps overlaps, long now) {
    return tick(plan, circuits, overlaps, now, new WorldDemandSource(world, circuits), world);
  }

  /**
   * World-free tick entry point for unit tests: drives the engine with a canned {@link DemandSource}
   * and no Minecraft world. Inspect the result via {@link #getLastServed(int)}.
   */
  TrafficSignalPhase tick(TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits, TrafficSignalControllerOverlaps overlaps, long now,
      DemandSource demandSource) {
    return tick(plan, circuits, overlaps, now, demandSource, null);
  }

  private TrafficSignalPhase tick(TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits, TrafficSignalControllerOverlaps overlaps, long now,
      DemandSource demandSource, World world) {
    this.tickWorld = world;
    this.tickCircuits = circuits;
    this.demand = demandSource;

    // Compute coordination windows for this tick (no-op in FREE mode).
    computeCoordination(plan, now);

    // Compute which phases are currently calling for service.
    boolean[] called = new boolean[TrafficSignalProgrammedPhasePlan.PHASE_COUNT + 1];
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      called[n] = isCalled(plan.getPhase(n));
    }

    if (!initialized) {
      currentBarrier = firstBarrier(plan);
      initialized = true;
    }

    // Preemption overrides normal/coordinated operation while active.
    updatePreempt(plan, now);
    if (preemptStage != PreemptStage.NONE) {
      resetRings(); // park normal operation so it resumes cleanly after the preempt clears
      return changedOrNull(buildPreemptPhase(world, plan, circuits, overlaps, now));
    }

    // 1. Advance each ring's active phase through its intervals (green -> yellow -> red clearance).
    advanceRing(ring1, plan, now, called);
    advanceRing(ring2, plan, now, called);

    // 2. Fill any idle ring with the next called phase on the current barrier.
    fillIdleRing(ring1, plan, 1, now, called);
    fillIdleRing(ring2, plan, 2, now, called);

    // 3. Barrier handling: if both rings are parked at the barrier, advance or rest.
    handleBarrier(plan, now, called);

    // 4. Build and (only if changed) return the displayed phase.
    ServedMovement m1 = describe(ring1, plan, now);
    ServedMovement m2 = describe(ring2, plan, now);
    this.lastServed1 = m1;
    this.lastServed2 = m2;
    return changedOrNull(AdvancedPhaseBuilder.build(world, plan, circuits, overlaps, m1, m2));
  }

  /** Returns {@code phase} (and records it) only if it differs from the last applied phase. */
  private TrafficSignalPhase changedOrNull(TrafficSignalPhase phase) {
    if (lastApplied == null || !phase.equals(lastApplied)) {
      lastApplied = phase;
      return phase;
    }
    return null;
  }

  private void resetRings() {
    resetRing(ring1);
    resetRing(ring2);
  }

  private void resetRing(RingRuntime ring) {
    ring.activePhase = 0;
    ring.sequencePos = -1;
    ring.interval = VehInterval.RED;
    ring.resting = false;
    ring.pedServing = false;
    ring.delayActive = false;
  }

  // region: Ring stepping

  /** Advances one ring's currently-active phase through green -> yellow -> red clearance. */
  private void advanceRing(RingRuntime ring, TrafficSignalProgrammedPhasePlan plan, long now,
      boolean[] called) {
    if (ring.activePhase == 0) {
      return;
    }
    TrafficSignalProgrammedPhase phase = plan.getPhase(ring.activePhase);
    if (phase == null) {
      ring.activePhase = 0;
      return;
    }

    switch (ring.interval) {
      case GREEN: {
        // A vehicle call holds (extends) the green.
        // Delayed green (DLY GRN): hold the vehicle red while the leading ped walk runs. The
        // green/max/passage clocks do not start until the delay ends, so the phase still gets its
        // full minimum green afterward.
        if (ring.delayActive) {
          if (now - ring.delayStart < phase.getDelayedGreen()) {
            ring.lastActuation = now; // keep the (not-yet-started) green from instantly gapping out
            break;
          }
          ring.delayActive = false;
          ring.greenStart = now;
          ring.intervalStart = now;
          ring.lastActuation = now;
        }
        if (vehicleCount(phase) > 0) {
          ring.lastActuation = now;
        }
        int phaseNum = ring.activePhase;
        boolean isCoord = coordinated && coordPhase[phaseNum];
        long greenElapsed = now - ring.greenStart;
        // Volume-density: minimum green is extended by added initial (queue at start) and bike
        // minimum green; the passage gaps shorter as green runs on; Max 2 replaces Max 1 in
        // coordinated operation when set.
        long addedInit = AdvancedActuationTiming.addedInitial(
            ring.queueAtStart, phase.getAddedInitial(), phase.getMaxInitial());
        long effMinGreen = AdvancedActuationTiming.effectiveMinGreen(
            phase.getMinGreen(), addedInit, phase.getBikeMinGreen(), ring.bikeCall);
        long effMaxGreen = AdvancedActuationTiming.effectiveMaxGreen(
            phase.getMaxGreen(), phase.getMax2(), coordinated);
        long effPassage = AdvancedActuationTiming.effectivePassage(phase.getPassage(),
            phase.getMinGap(), phase.getTimeBeforeReduce(), phase.getTimeToReduce(), greenElapsed);
        boolean minMet = greenElapsed >= effMinGreen;
        // The coordinated phase rests in green (no max-out); it yields only to a called phase.
        boolean maxOut = !ring.resting && !isCoord && greenElapsed >= effMaxGreen;
        boolean gapOut = (now - ring.lastActuation) >= effPassage;
        boolean pedDone = !ring.pedServing
            || (now - ring.pedStart) >= (ring.walkHold + phase.getPedClear());
        boolean conflict = conflictingDemand(called);
        // Coordinated force-off: a non-coordinated phase must end when its split window closes.
        boolean forceOff = coordinated && !isCoord && localCycle >= windowEnd[phaseNum];

        // Terminate on max-out or force-off, or once min green is met, ped clearance is done, the
        // phase has gapped out, and something else is actually waiting (otherwise rest in green).
        boolean terminate = maxOut || forceOff || (minMet && pedDone && gapOut && conflict);
        if (terminate && minMet) {
          ring.interval = VehInterval.YELLOW;
          ring.intervalStart = now;
          ring.pedServing = false;
        }
        break;
      }
      case YELLOW: {
        if (now - ring.intervalStart >= phase.getYellow()) {
          ring.interval = VehInterval.RED;
          ring.intervalStart = now;
        }
        break;
      }
      case RED:
      default: {
        if (now - ring.intervalStart >= phase.getRedClear()) {
          // Phase fully cleared; ring goes idle and will pick its next phase below.
          ring.activePhase = 0;
          ring.resting = false;
        }
        break;
      }
    }
  }

  /** If the ring is idle, start the next called phase in its sequence on the current barrier. */
  private void fillIdleRing(RingRuntime ring, TrafficSignalProgrammedPhasePlan plan, int ringNum,
      long now, boolean[] called) {
    if (ring.activePhase != 0) {
      return;
    }
    int[] seq = plan.getRingSequence(ringNum);
    for (int idx = ring.sequencePos + 1; idx < seq.length; idx++) {
      int phaseNumber = seq[idx];
      TrafficSignalProgrammedPhase phase = plan.getPhase(phaseNumber);
      if (phase == null) {
        ring.sequencePos = idx;
        continue;
      }
      if (phase.getBarrier() != currentBarrier) {
        break; // reached the barrier boundary; wait for both rings to cross
      }
      if (!phase.isActive()) {
        ring.sequencePos = idx; // skip unusable phase
        continue;
      }
      if (phaseNumber >= 1 && phaseNumber < called.length && called[phaseNumber]) {
        startGreen(ring, phase, now);
        ring.sequencePos = idx;
        return;
      }
      ring.sequencePos = idx; // not called now — skip its slot this cycle
    }
    // Nothing more to serve on this barrier for this ring: it is parked at the barrier.
  }

  private void startGreen(RingRuntime ring, TrafficSignalProgrammedPhase phase, long now) {
    ring.activePhase = phase.getPhaseNumber();
    ring.interval = VehInterval.GREEN;
    ring.intervalStart = now;
    ring.greenStart = now;
    ring.lastActuation = now;
    ring.resting = false;
    // Begin a pedestrian service when called or recalled.
    boolean ped = phase.isPedRecall()
        || phase.getRecallMode() == TrafficSignalRecallMode.PEDESTRIAN
        || pedRequestPresent(phase);
    ring.pedServing = ped;
    ring.pedStart = now;
    // ASC/3 DLY GRN: the delay applies only when this phase starts with a ped service. The walk
    // is extended to the end of the delay when the delay exceeds the configured walk.
    ring.delayActive = ped && phase.getDelayedGreen() > 0L;
    ring.delayStart = now;
    ring.walkHold = ped ? Math.max(phase.getWalk(), phase.getDelayedGreen()) : phase.getWalk();
    // Volume-density / bike snapshots at green start.
    ring.queueAtStart = vehicleCount(phase);
    TrafficSignalSensorSummary startSummary = summaryForCircuit(phase.getCircuitIndex());
    ring.bikeCall = startSummary != null && startSummary.getProtectedTotal() > 0;
  }

  // endregion

  // region: Barrier / rest

  private boolean ringParked(RingRuntime ring, TrafficSignalProgrammedPhasePlan plan, int ringNum) {
    if (ring.activePhase != 0) {
      return false;
    }
    int[] seq = plan.getRingSequence(ringNum);
    for (int idx = ring.sequencePos + 1; idx < seq.length; idx++) {
      TrafficSignalProgrammedPhase phase = plan.getPhase(seq[idx]);
      if (phase == null) {
        continue;
      }
      if (phase.getBarrier() == currentBarrier) {
        return false; // still has a slot on this barrier
      }
      break; // hit a phase on the next barrier
    }
    return true;
  }

  /** Crosses the barrier when both rings are parked, or rests in green when there is no demand. */
  private void handleBarrier(TrafficSignalProgrammedPhasePlan plan, long now, boolean[] called) {
    if (!ringParked(ring1, plan, 1) || !ringParked(ring2, plan, 2)) {
      return; // a ring is still working this barrier
    }

    // Both rings parked. If anything is called anywhere, advance to a barrier with demand;
    // otherwise rest in green on the coordinated phases.
    boolean anyCall = false;
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      if (called[n]) {
        anyCall = true;
        break;
      }
    }

    if (!anyCall) {
      restInGreen(plan, now);
      return;
    }

    // Reset both rings to the start of their sequence and select the barrier with demand.
    currentBarrier = barrierWithDemand(plan, called);
    ring1.sequencePos = -1;
    ring2.sequencePos = -1;
    ring1.resting = false;
    ring2.resting = false;
    fillIdleRing(ring1, plan, 1, now, called);
    fillIdleRing(ring2, plan, 2, now, called);
  }

  /** Serves the coordinated (or first active) phase in each ring at green with no max-out. */
  private void restInGreen(TrafficSignalProgrammedPhasePlan plan, long now) {
    restRing(ring1, plan, 1, now);
    restRing(ring2, plan, 2, now);
  }

  private void restRing(RingRuntime ring, TrafficSignalProgrammedPhasePlan plan, int ringNum,
      long now) {
    if (ring.activePhase != 0) {
      return;
    }
    int restPhase = restPhaseForRing(plan, ringNum);
    if (restPhase == 0) {
      return;
    }
    TrafficSignalProgrammedPhase phase = plan.getPhase(restPhase);
    if (phase == null) {
      return;
    }
    currentBarrier = phase.getBarrier();
    startGreen(ring, phase, now);
    ring.resting = true;
    // Rest in Walk: hold the WALK indication on this phase while resting; otherwise don't-walk.
    ring.pedServing = phase.isRestInWalk();
    ring.delayActive = false; // a coordinated rest phase does not run a leading ped interval
    // Align the sequence position with the rest phase so the cycle resumes cleanly on demand.
    int[] seq = plan.getRingSequence(ringNum);
    for (int idx = 0; idx < seq.length; idx++) {
      if (seq[idx] == restPhase) {
        ring.sequencePos = idx;
        break;
      }
    }
  }

  private int restPhaseForRing(TrafficSignalProgrammedPhasePlan plan, int ringNum) {
    int[] seq = plan.getRingSequence(ringNum);
    // Prefer a coordinated phase in this ring.
    for (int n : seq) {
      TrafficSignalProgrammedPhase phase = plan.getPhase(n);
      if (phase != null && phase.isActive() && plan.getCoordination().isCoordinatedPhase(n)) {
        return n;
      }
    }
    // Then a Soft Recall phase: the configured place to rest when nothing else is calling.
    for (int n : seq) {
      TrafficSignalProgrammedPhase phase = plan.getPhase(n);
      if (phase != null && phase.isActive()
          && phase.getRecallMode() == TrafficSignalRecallMode.SOFT) {
        return n;
      }
    }
    // Otherwise the first active phase in the ring.
    for (int n : seq) {
      TrafficSignalProgrammedPhase phase = plan.getPhase(n);
      if (phase != null && phase.isActive()) {
        return n;
      }
    }
    return 0;
  }

  private int barrierWithDemand(TrafficSignalProgrammedPhasePlan plan, boolean[] called) {
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      if (called[n]) {
        TrafficSignalProgrammedPhase phase = plan.getPhase(n);
        if (phase != null) {
          return phase.getBarrier();
        }
      }
    }
    return currentBarrier;
  }

  private int firstBarrier(TrafficSignalProgrammedPhasePlan plan) {
    int[] seq = plan.getRingSequence(1);
    for (int n : seq) {
      TrafficSignalProgrammedPhase phase = plan.getPhase(n);
      if (phase != null && phase.isActive()) {
        return phase.getBarrier();
      }
    }
    return 0;
  }

  // endregion

  // region: Coordination

  /**
   * Computes, for this tick, the local cycle position and each phase's permissive window from the
   * plan's {@link TrafficSignalCoordinationPlan}. In FREE mode this clears the state and returns
   * immediately, so coordination adds nothing to free/actuated operation.
   */
  private void computeCoordination(TrafficSignalProgrammedPhasePlan plan, long now) {
    TrafficSignalCoordinationPlan co = plan.getCoordination();
    coordinated = co.isCoordinated();
    for (int i = 0; i < PHASE_SLOTS; i++) {
      windowStart[i] = 0L;
      windowEnd[i] = 0L;
      coordPhase[i] = false;
    }
    if (!coordinated) {
      return;
    }
    long cycle = Math.max(1L, co.getCycleLength());
    localCycle = ((now - co.getOffset()) % cycle + cycle) % cycle;

    for (int ring = 1; ring <= 2; ring++) {
      int[] seq = plan.getRingSequence(ring);
      // Active phases in this ring, in sequence order.
      List<Integer> active = new ArrayList<>();
      for (int n : seq) {
        TrafficSignalProgrammedPhase p = plan.getPhase(n);
        if (p != null && p.isActive()) {
          active.add(n);
        }
      }
      if (active.isEmpty()) {
        continue;
      }
      // Splits (configured or evenly divided), normalized to tile the cycle exactly.
      long total = 0L;
      long[] split = new long[active.size()];
      for (int i = 0; i < active.size(); i++) {
        long s = co.getSplit(active.get(i));
        if (s <= 0L) {
          s = cycle / active.size();
        }
        split[i] = s;
        total += s;
      }
      long cum = 0L;
      for (int i = 0; i < active.size(); i++) {
        int n = active.get(i);
        long w = total > 0L ? split[i] * cycle / total : cycle / active.size();
        windowStart[n] = cum;
        cum += w;
        // Last phase absorbs rounding so windows tile [0, cycle) exactly.
        windowEnd[n] = (i == active.size() - 1) ? cycle : cum;
      }
    }
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      coordPhase[n] = co.isCoordinatedPhase(n);
    }
  }

  /** Whether a non-coordinated phase's permissive window is currently open. */
  private boolean windowOpen(int phaseNumber) {
    return localCycle >= windowStart[phaseNumber] && localCycle < windowEnd[phaseNumber];
  }

  // endregion

  // region: Demand

  /** Whether a phase is calling for service (vehicle, pedestrian, or recall). */
  private boolean isCalled(TrafficSignalProgrammedPhase phase) {
    if (phase == null || !phase.isActive()
        || phase.getCircuitIndex() >= tickCircuits.getCircuitCount()) {
      return false;
    }
    // In coordinated operation, a non-coordinated phase only calls within its permissive window;
    // outside it the time belongs to the coordinated phase, which rests in green.
    if (coordinated && !coordPhase[phase.getPhaseNumber()] && !windowOpen(phase.getPhaseNumber())) {
      return false;
    }
    // Coordinated phases are served every cycle regardless of their own detection, so the
    // background cycle holds even under continuous side-street demand.
    if (coordinated && coordPhase[phase.getPhaseNumber()]) {
      return true;
    }
    TrafficSignalRecallMode recall = phase.getRecallMode();
    if (recall == TrafficSignalRecallMode.MINIMUM || recall == TrafficSignalRecallMode.MAXIMUM
        || recall == TrafficSignalRecallMode.PEDESTRIAN) {
      return true;
    }
    if (vehicleCount(phase) > 0) {
      return true;
    }
    return pedRequestPresent(phase);
  }

  /** True if any other phase wants service but isn't one of the two currently being served. */
  private boolean conflictingDemand(boolean[] called) {
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      if (called[n] && n != ring1.activePhase && n != ring2.activePhase) {
        return true;
      }
    }
    return false;
  }

  private int vehicleCount(TrafficSignalProgrammedPhase phase) {
    return zoneCount(phase.getCircuitIndex(), phase.getMovement());
  }

  /** Vehicle count in a circuit's sensor zone for the given movement. */
  private int zoneCount(int circuitIndex, TrafficSignalPhaseMovement movement) {
    TrafficSignalSensorSummary summary = summaryForCircuit(circuitIndex);
    if (summary == null) {
      return 0;
    }
    switch (movement) {
      case THROUGH:
        return summary.getStandardTotal();
      case LEFT:
      case PROTECTED_LEFT:
        return summary.getLeftTotal() + summary.getProtectedTotal();
      case RIGHT:
        return summary.getRightTotal();
      case PED:
      default:
        return 0;
    }
  }

  private boolean pedRequestPresent(TrafficSignalProgrammedPhase phase) {
    return demand != null && phase != null && demand.pedestrianRequest(phase.getCircuitIndex());
  }

  private TrafficSignalSensorSummary summaryForCircuit(int ci) {
    return demand == null ? null : demand.summaryForCircuit(ci);
  }

  // endregion

  // region: Preemption

  /**
   * Advances the preemption state machine: detects calls, picks the highest-priority active
   * preempt, and steps enter &rarr; track-clear &rarr; dwell &rarr; exit.
   */
  private void updatePreempt(TrafficSignalProgrammedPhasePlan plan, long now) {
    List<TrafficSignalPreempt> preempts = plan.getPreempts();

    // Highest-priority preempt currently calling for service.
    int calledIdx = -1;
    int calledPriority = Integer.MIN_VALUE;
    for (int i = 0; i < preempts.size(); i++) {
      TrafficSignalPreempt pe = preempts.get(i);
      if (pe.isActive() && isPreemptCalled(pe) && pe.getType().getPriority() > calledPriority) {
        calledIdx = i;
        calledPriority = pe.getType().getPriority();
      }
    }

    if (preemptStage == PreemptStage.NONE) {
      if (calledIdx >= 0) {
        beginPreempt(calledIdx, now);
      }
      return;
    }

    // Guard against the preempt table changing underneath us.
    if (activePreemptIndex < 0 || activePreemptIndex >= preempts.size()) {
      preemptStage = PreemptStage.NONE;
      activePreemptIndex = -1;
      return;
    }
    TrafficSignalPreempt active = preempts.get(activePreemptIndex);

    // A higher-priority call takes over (re-enters clearance).
    if (calledIdx >= 0 && calledPriority > active.getType().getPriority()) {
      beginPreempt(calledIdx, now);
      return;
    }

    long elapsed = now - preemptStageStart;
    switch (preemptStage) {
      case ENTER:
        if (elapsed >= PREEMPT_YELLOW_TICKS + PREEMPT_REDCLEAR_TICKS) {
          preemptStage = active.getTrackClearPhases().length > 0
              ? PreemptStage.TRACK_CLEAR : PreemptStage.DWELL;
          preemptStageStart = now;
        }
        break;
      case TRACK_CLEAR:
        if (elapsed >= PREEMPT_TRACK_CLEAR_TICKS) {
          preemptStage = PreemptStage.DWELL;
          preemptStageStart = now;
        }
        break;
      case DWELL:
        // Hold the dwell phases until the call drops and the minimum dwell has elapsed.
        if (!isPreemptCalled(active) && elapsed >= active.getMinDwell()) {
          preemptStage = PreemptStage.EXIT;
          preemptStageStart = now;
        }
        break;
      case EXIT:
      default:
        if (elapsed >= PREEMPT_EXIT_TICKS) {
          preemptStage = PreemptStage.NONE;
          activePreemptIndex = -1;
        }
        break;
    }
  }

  private void beginPreempt(int index, long now) {
    activePreemptIndex = index;
    preemptStage = PreemptStage.ENTER;
    preemptStageStart = now;
    // Capture whatever is currently green so entry can clear it with a proper yellow.
    preemptClearPhases.clear();
    if (ring1.activePhase != 0) {
      preemptClearPhases.add(ring1.activePhase);
    }
    if (ring2.activePhase != 0) {
      preemptClearPhases.add(ring2.activePhase);
    }
  }

  private boolean isPreemptCalled(TrafficSignalPreempt preempt) {
    return zoneCount(preempt.getTriggerCircuitIndex(), preempt.getTriggerMovement()) > 0;
  }

  private TrafficSignalPhase buildPreemptPhase(World world, TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits, TrafficSignalControllerOverlaps overlaps, long now) {
    TrafficSignalPreempt active = plan.getPreempts().get(activePreemptIndex);
    long elapsed = now - preemptStageStart;
    switch (preemptStage) {
      case ENTER:
        if (elapsed < PREEMPT_YELLOW_TICKS) {
          return AdvancedPhaseBuilder.buildForPhases(world, plan, circuits, overlaps,
              preemptClearPhases, VehInterval.YELLOW);
        }
        return AdvancedPhaseBuilder.buildForPhases(world, plan, circuits, overlaps,
            java.util.Collections.emptyList(), VehInterval.RED);
      case TRACK_CLEAR:
        return AdvancedPhaseBuilder.buildForPhases(world, plan, circuits, overlaps,
            toPhaseList(active.getTrackClearPhases()), VehInterval.GREEN);
      case DWELL:
        return AdvancedPhaseBuilder.buildForPhases(world, plan, circuits, overlaps,
            toPhaseList(active.getDwellPhases()), VehInterval.GREEN);
      case EXIT:
      default:
        return AdvancedPhaseBuilder.buildForPhases(world, plan, circuits, overlaps,
            java.util.Collections.emptyList(), VehInterval.RED);
    }
  }

  private static List<Integer> toPhaseList(int[] phaseNumbers) {
    List<Integer> list = new ArrayList<>(phaseNumbers.length);
    for (int n : phaseNumbers) {
      list.add(n);
    }
    return list;
  }

  // endregion

  // region: Display

  private ServedMovement describe(RingRuntime ring, TrafficSignalProgrammedPhasePlan plan,
      long now) {
    if (ring.activePhase == 0) {
      return null;
    }
    PedInterval ped = PedInterval.NONE;
    if (ring.pedServing && ring.interval == VehInterval.GREEN) {
      TrafficSignalProgrammedPhase phase = plan.getPhase(ring.activePhase);
      if (phase != null) {
        if (ring.resting && phase.isRestInWalk()) {
          // Rest in Walk: the WALK is held for as long as the controller rests here.
          ped = PedInterval.WALK;
        } else {
          long pedElapsed = now - ring.pedStart;
          if (pedElapsed < ring.walkHold) {
            ped = PedInterval.WALK;
          } else if (pedElapsed < ring.walkHold + phase.getPedClear()) {
            ped = PedInterval.FDW;
          } else {
            ped = PedInterval.DONT_WALK;
          }
        }
      }
    }
    // During delayed green the vehicle is held red even though the ring interval is internally
    // GREEN; the leading ped walk (computed above as WALK, since walkHold >= the delay) shows.
    VehInterval veh = ring.delayActive ? VehInterval.RED : ring.interval;
    return new ServedMovement(ring.activePhase, veh, ped);
  }

  // endregion
}
