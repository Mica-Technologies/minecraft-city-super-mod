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
 * to its passage (gap) time, and forced off at its maximum green (which, per NEMA, times from the
 * first <em>conflicting</em> call, so an unopposed green rests rather than cycling); pedestrian
 * Walk/FDW run concurrently with the served through. A green yields only to demand that actually
 * conflicts with it — a call on the same barrier in the other ring (e.g. a left turn beside its
 * adjacent through) terminates only the phases it crosses, leaving the compatible green running.
 * With no demand the controller rests in green on the coordinated phases.
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
    // NEMA MAX: world tick the max-green timer began — set when a conflicting call registers
    // during green, cleared when the conflict drops. -1 = not timing; max green never expires
    // without conflicting demand (the phase rests in green instead of pointlessly cycling).
    long maxStart = -1L;
    // Conditional service: a conditional-service phase has already been re-served once on the
    // current barrier (guard against re-serving more than once / looping).
    boolean condServiceUsed = false;
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
  /** Per-overlap (by plan index) last tick the overlap's included phases were green — for lag green. */
  private final Map<Integer, Long> overlapLastGreen = new HashMap<>();

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

  /** The phase last applied to the world (package-private for unit tests inspecting head states). */
  TrafficSignalPhase getLastAppliedPhase() {
    return lastApplied;
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
    applyOverlapDemand(plan, called);

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
    advanceRing(ring1, plan, 1, now, called);
    advanceRing(ring2, plan, 2, now, called);

    // 2. Fill any idle ring with the next called phase on the current barrier.
    fillIdleRing(ring1, plan, 1, now, called);
    fillIdleRing(ring2, plan, 2, now, called);

    // 2b. Dual entry: a ring with no call companions the other ring's served barrier.
    fillDualEntry(ring1, ring2, plan, 1, now, called);
    fillDualEntry(ring2, ring1, plan, 2, now, called);

    // 3. Barrier handling: if both rings are parked at the barrier, advance or rest.
    handleBarrier(plan, now, called);

    // 4. Build and (only if changed) return the displayed phase.
    ServedMovement m1 = describe(ring1, plan, now);
    ServedMovement m2 = describe(ring2, plan, now);
    this.lastServed1 = m1;
    this.lastServed2 = m2;
    List<VehInterval> overlapIntervals = computeOverlapIntervals(plan, m1, m2, now, called);
    java.util.Set<Integer> fyaHoldFlash = computeFyaHoldFlash(plan, called);
    return changedOrNull(AdvancedPhaseBuilder.build(
        world, plan, circuits, overlaps, m1, m2, overlapIntervals, fyaHoldFlash));
  }

  /**
   * The FYA left phases whose permissive flash should be held (not cleared) this tick because their
   * own protected green is coming: the phase is called and on the current barrier but not yet being
   * served. The ring-barrier engine won't cross off the current barrier until every called phase on
   * it has been served, so a called left on this barrier is guaranteed to get its protected green —
   * the flash holds through the opposing through's clearance and then goes flash &rarr; protected
   * green directly, with no solid-yellow/red on the FYA head (a flashing yellow is always a safe
   * yield, so holding it is safe). Left phases not in this set clear their flash normally (solid
   * yellow with the opposing through, then red — the flash-to-red case for a left with no demand).
   */
  private java.util.Set<Integer> computeFyaHoldFlash(TrafficSignalProgrammedPhasePlan plan,
      boolean[] called) {
    java.util.Set<Integer> hold = new java.util.HashSet<>();
    for (TrafficSignalProgrammedPhase p : plan.getPhases()) {
      if (!p.isActive() || p.getPermissivePhase() <= 0
          || (p.getMovement() != TrafficSignalPhaseMovement.LEFT
          && p.getMovement() != TrafficSignalPhaseMovement.PROTECTED_LEFT)) {
        continue;
      }
      int ci = p.getCircuitIndex();
      int pn = p.getPhaseNumber();
      if (tickCircuits == null || ci < 0 || ci >= tickCircuits.getCircuitCount()
          || tickCircuits.getCircuit(ci).getFlashingLeftSignals().isEmpty()
          || pn >= called.length || !called[pn] || p.getBarrier() != currentBarrier
          || ring1.activePhase == pn || ring2.activePhase == pn) {
        continue;
      }
      hold.add(pn);
    }
    return hold;
  }

  /**
   * Computes each vehicle overlap's effective interval this tick, applying lag (trailing) and lead
   * (advance) green: an overlap is green while its included phases are green, for {@code trailGreen}
   * ticks after they leave green, and for {@code leadGreen} ticks before an included phase greens
   * (during the preceding within-barrier red clearance); otherwise it follows the stateless base
   * decision (yellow during the included phases' clearance, else red).
   */
  private List<VehInterval> computeOverlapIntervals(TrafficSignalProgrammedPhasePlan plan,
      ServedMovement m1, ServedMovement m2, long now, boolean[] called) {
    List<TrafficSignalProgrammedOverlap> ovs = plan.getVehicleOverlaps();
    List<VehInterval> result = new ArrayList<>(ovs.size());
    for (int i = 0; i < ovs.size(); i++) {
      TrafficSignalProgrammedOverlap ov = ovs.get(i);
      VehInterval base = AdvancedPhaseBuilder.overlapState(ov.getIncludedPhases(), m1, m2);
      VehInterval eff;
      if (base == VehInterval.GREEN) {
        overlapLastGreen.put(i, now);
        eff = VehInterval.GREEN;
      } else {
        Long last = overlapLastGreen.get(i);
        boolean trailing = ov.getTrailGreen() > 0L && last != null
            && (now - last) < ov.getTrailGreen();
        eff = trailing ? VehInterval.GREEN : base;
        // Lead (advance) green: green early during the red clearance that precedes an included
        // phase's green (within-barrier).
        if (eff != VehInterval.GREEN && ov.getLeadGreen() > 0L
            && leadingIntoIncluded(ov, plan, now, called)) {
          eff = VehInterval.GREEN;
        }
      }
      // -GRN/YEL: force red while a modifier phase is green or yellow.
      if (ov.getType() == TrafficSignalOverlapType.MINUS_GREEN_YELLOW
          && AdvancedPhaseBuilder.anyServedActive(ov.getModifierPhases(), m1, m2)) {
        eff = VehInterval.RED;
      }
      result.add(eff);
    }
    return result;
  }

  /** Whether any ring is within the overlap's lead window heading into one of its included phases. */
  private boolean leadingIntoIncluded(TrafficSignalProgrammedOverlap ov,
      TrafficSignalProgrammedPhasePlan plan, long now, boolean[] called) {
    for (int p : ov.getIncludedPhases()) {
      if (ringLeadsInto(ring1, 1, p, ov.getLeadGreen(), plan, now, called)
          || ringLeadsInto(ring2, 2, p, ov.getLeadGreen(), plan, now, called)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Whether {@code ring} is in a red clearance whose next within-barrier phase is
   * {@code includedPhase}, and we are within {@code leadGreen} of that clearance ending (i.e. the
   * overlap should be leading green into that phase now).
   */
  private boolean ringLeadsInto(RingRuntime ring, int ringNum, int includedPhase, long leadGreen,
      TrafficSignalProgrammedPhasePlan plan, long now, boolean[] called) {
    if (ring.interval != VehInterval.RED || ring.activePhase == 0) {
      return false; // lead green is shown only during a red clearance preceding the phase
    }
    TrafficSignalProgrammedPhase active = plan.getPhase(ring.activePhase);
    if (active == null) {
      return false;
    }
    long clearEnd = ring.intervalStart + active.getRedClear();
    if (now >= clearEnd || (clearEnd - now) > leadGreen) {
      return false; // clearance already done, or not yet inside the lead window
    }
    return peekNextWithinBarrier(ring, ringNum, plan, called) == includedPhase;
  }

  /**
   * The next phase {@code ring} will serve on the current barrier (mirrors {@link #fillIdleRing}'s
   * selection), or 0 if the next slot is across the barrier (the cross-barrier next phase is not
   * determined here, so lead green doesn't apply across a barrier).
   */
  private int peekNextWithinBarrier(RingRuntime ring, int ringNum,
      TrafficSignalProgrammedPhasePlan plan, boolean[] called) {
    int[] seq = plan.getRingSequence(ringNum);
    for (int idx = ring.sequencePos + 1; idx < seq.length; idx++) {
      int phaseNumber = seq[idx];
      TrafficSignalProgrammedPhase phase = plan.getPhase(phaseNumber);
      if (phase == null) {
        continue;
      }
      if (phase.getBarrier() != currentBarrier) {
        return 0;
      }
      if (!phase.isActive()) {
        continue;
      }
      if (phaseNumber >= 1 && phaseNumber < called.length && called[phaseNumber]) {
        return phaseNumber;
      }
    }
    return 0;
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
    ring.maxStart = -1L;
    ring.condServiceUsed = false;
  }

  // region: Ring stepping

  /** Advances one ring's currently-active phase through green -> yellow -> red clearance. */
  private void advanceRing(RingRuntime ring, TrafficSignalProgrammedPhasePlan plan, int ringNum,
      long now, boolean[] called) {
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
        // Only demand that actually conflicts with THIS phase can end it. A call on the same
        // barrier in the other ring (e.g. a left turn beside its adjacent through) is compatible
        // and must not clear this green — the other ring handles it while this one keeps running.
        boolean conflict = demandConflictsWith(phase, ringNum, plan, called);
        // Rest-in-walk clearance: a phase resting on WALK cannot snap the walk straight to
        // don't-walk when a conflicting call arrives — it must first run pedestrian clearance
        // (FDW). End the rest (the vehicle stays green) and re-arm the ped timer to the start of
        // FDW; the terminate logic below then holds the green until the clearance finishes.
        if (ring.resting && ring.pedServing && conflict) {
          ring.resting = false;
          ring.pedStart = now - ring.walkHold;
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
        boolean pedDone = !ring.pedServing
            || (now - ring.pedStart) >= (ring.walkHold + phase.getPedClear());
        // NEMA MAX: the max-green timer runs only while a conflicting call is present (it starts
        // at the call's registration, not at green start, and resets if the call drops) — an
        // unopposed green rests instead of cycling to yellow for nobody.
        if (conflict) {
          if (ring.maxStart < 0L) {
            ring.maxStart = now;
          }
        } else {
          ring.maxStart = -1L;
        }
        // The coordinated phase rests in green (no max-out); it yields only to a called phase.
        // Max-out must not truncate pedestrian clearance — the walk/FDW interval finishes first.
        boolean maxOut = !ring.resting && !isCoord && ring.maxStart >= 0L
            && (now - ring.maxStart) >= effMaxGreen && pedDone;
        boolean gapOut = (now - ring.lastActuation) >= effPassage;
        // Coordinated force-off: a non-coordinated phase must end when its split window closes.
        boolean forceOff = coordinated && !isCoord && localCycle >= windowEnd[phaseNum];

        // Terminate on max-out or force-off, or once min green is met, ped clearance is done, the
        // phase has gapped out, and something conflicting is actually waiting (otherwise rest in
        // green). A dual-entry companion follows the same rule: with no conflicting demand it
        // simply holds green alongside whatever the other ring serves on this barrier.
        boolean terminate = maxOut || forceOff || (minMet && pedDone && gapOut && conflict);
        if (terminate && minMet) {
          ring.interval = VehInterval.YELLOW;
          ring.intervalStart = now;
          ring.pedServing = false;
        } else if (phase.isRestInWalk() && !conflict) {
          // Rest in Walk: while this phase holds green with nothing else calling, recall the WALK
          // rather than sitting in don't-walk after the first ped clearance — whether the phase was
          // entered as the no-demand rest or served by a call. But never cut off a pedestrian
          // clearance already in progress (e.g. one the clearance block above started for a
          // conflict that then dropped): let the FDW finish, then recycle to WALK. A clearance is
          // in progress once the ped has passed WALK and hasn't reached don't-walk yet.
          boolean clearanceInProgress = ring.pedServing && !pedDone
              && (now - ring.pedStart) >= ring.walkHold;
          if (!clearanceInProgress) {
            ring.resting = true;
            ring.pedServing = true;
          }
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
      ring.sequencePos = idx;
      // Skip unusable phases and phases on another barrier. The shared currentBarrier — not a
      // sequence break — keeps both rings on the same barrier, so a ring advances past phases that
      // aren't on the barrier being served this crossing rather than stopping at the first one.
      if (phase == null || !phase.isActive() || phase.getBarrier() != currentBarrier) {
        continue;
      }
      if (phaseNumber >= 1 && phaseNumber < called.length && called[phaseNumber]) {
        startGreen(ring, phase, now);
        return;
      }
      // Active phase on this barrier but not called right now — skip its slot this cycle.
    }
    // No forward phase is called on this barrier. Conditional service: re-serve (at most once per
    // barrier) an earlier conditional-service phase on this barrier that has reacquired a call —
    // e.g. a lagging left that re-fills before the barrier crosses. sequencePos is left at the
    // barrier end so the ring parks (and crosses) after the re-served phase terminates.
    if (!ring.condServiceUsed) {
      for (int idx = 0; idx < seq.length; idx++) {
        int phaseNumber = seq[idx];
        TrafficSignalProgrammedPhase phase = plan.getPhase(phaseNumber);
        if (phase != null && phase.isActive() && phase.getBarrier() == currentBarrier
            && phase.isConditionalService()
            && phaseNumber >= 1 && phaseNumber < called.length && called[phaseNumber]) {
          startGreen(ring, phase, now);
          ring.condServiceUsed = true;
          return;
        }
      }
    }
    // Within-barrier wrap: when every calling phase is on this barrier (so a crossing would land
    // right back here), re-serve an earlier called phase in this ring directly instead of parking.
    // Parking would force a full both-ring barrier re-cross — needlessly clearing the other ring's
    // compatible green (e.g. the adjacent through would run green -> yellow -> red -> green around
    // a left-turn call that never conflicted with it).
    if (nextBarrierWithDemand(plan, called) == currentBarrier) {
      for (int idx = 0; idx < seq.length; idx++) {
        int phaseNumber = seq[idx];
        TrafficSignalProgrammedPhase phase = plan.getPhase(phaseNumber);
        if (phase != null && phase.isActive() && phase.getBarrier() == currentBarrier
            && phaseNumber >= 1 && phaseNumber < called.length && called[phaseNumber]
            && phaseNumber != ring1.activePhase && phaseNumber != ring2.activePhase) {
          startGreen(ring, phase, now);
          ring.sequencePos = idx;
          return;
        }
      }
    }
    // Nothing more to serve on this barrier for this ring: it is parked at the barrier.
  }

  /**
   * Dual entry: if {@code idle} has no call but the {@code other} ring is serving a green phase on
   * the current barrier, serve {@code idle}'s first active dual-entry phase on that barrier so the
   * intersection isn't left with one direction dark. Once entered, the companion terminates by the
   * same conflict-based rule as any phase — it rests in green through the other ring's
   * within-barrier transitions and yields only to demand that conflicts with it. To avoid entering
   * a green that would have to clear immediately (flicker), no companion is entered while demand is
   * waiting on another barrier, and a candidate that pending demand conflicts with is skipped.
   */
  private void fillDualEntry(RingRuntime idle, RingRuntime other,
      TrafficSignalProgrammedPhasePlan plan, int idleRingNum, long now, boolean[] called) {
    if (idle.activePhase != 0 || other.activePhase == 0) {
      return;
    }
    TrafficSignalProgrammedPhase otherPhase = plan.getPhase(other.activePhase);
    if (otherPhase == null || otherPhase.getBarrier() != currentBarrier
        || other.interval != VehInterval.GREEN) {
      return;
    }
    int target = nextBarrierWithDemand(plan, called);
    if (target >= 0 && target != currentBarrier) {
      return; // the intersection is about to cross; don't enter a green that must clear right away
    }
    int[] seq = plan.getRingSequence(idleRingNum);
    for (int idx = 0; idx < seq.length; idx++) {
      TrafficSignalProgrammedPhase phase = plan.getPhase(seq[idx]);
      if (phase != null && phase.isActive() && phase.getBarrier() == currentBarrier
          && phase.isDualEntry() && !demandConflictsWith(phase, idleRingNum, plan, called)) {
        startGreen(idle, phase, now);
        idle.sequencePos = idx;
        return;
      }
    }
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
    ring.maxStart = -1L; // NEMA MAX: (re)starts timing at the first conflicting call, not at green
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
      if (phase != null && phase.isActive() && phase.getBarrier() == currentBarrier) {
        return false; // still has an active slot to serve on this barrier ahead in the sequence
      }
    }
    return true;
  }

  /** Crosses the barrier when both rings are parked, or rests in green when there is no demand. */
  private void handleBarrier(TrafficSignalProgrammedPhasePlan plan, long now, boolean[] called) {
    if (!ringParked(ring1, plan, 1) || !ringParked(ring2, plan, 2)) {
      return; // a ring is still working this barrier
    }

    // Both rings parked. Rotate to the next barrier (fixed order, skipping empties) that has a
    // call; if nothing is called anywhere, rest in green on the coordinated phases.
    int target = nextBarrierWithDemand(plan, called);
    if (target < 0) {
      restInGreen(plan, now);
      return;
    }

    // Cross to the selected barrier and restart both ring sequences on it.
    currentBarrier = target;
    ring1.sequencePos = -1;
    ring2.sequencePos = -1;
    ring1.resting = false;
    ring2.resting = false;
    ring1.condServiceUsed = false; // new barrier visit: conditional service may re-serve again
    ring2.condServiceUsed = false;
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

  /**
   * Selects the next barrier to serve, rotating through the plan's barriers in fixed ascending
   * order starting just after {@link #currentBarrier} (wrapping around) and returning the first one
   * that has a phase calling for service. Barriers with no demand are skipped so an empty side
   * street never gets a green. Returns {@code -1} when no barrier has demand at all.
   */
  private int nextBarrierWithDemand(TrafficSignalProgrammedPhasePlan plan, boolean[] called) {
    int[] barriers = distinctBarriers(plan);
    if (barriers.length == 0) {
      return -1;
    }
    int cur = -1;
    for (int i = 0; i < barriers.length; i++) {
      if (barriers[i] == currentBarrier) {
        cur = i;
        break;
      }
    }
    for (int step = 1; step <= barriers.length; step++) {
      int barrier = barriers[((cur + step) % barriers.length + barriers.length) % barriers.length];
      if (barrierHasDemand(plan, barrier, called)) {
        return barrier;
      }
    }
    return -1;
  }

  /** The distinct barriers among active phases, in ascending (fixed rotation) order. */
  private int[] distinctBarriers(TrafficSignalProgrammedPhasePlan plan) {
    java.util.TreeSet<Integer> set = new java.util.TreeSet<>();
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      TrafficSignalProgrammedPhase phase = plan.getPhase(n);
      if (phase != null && phase.isActive()) {
        set.add(phase.getBarrier());
      }
    }
    int[] out = new int[set.size()];
    int i = 0;
    for (int b : set) {
      out[i++] = b;
    }
    return out;
  }

  /** Whether any active phase on the given barrier is calling for service. */
  private boolean barrierHasDemand(TrafficSignalProgrammedPhasePlan plan, int barrier,
      boolean[] called) {
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      if (n < called.length && called[n]) {
        TrafficSignalProgrammedPhase phase = plan.getPhase(n);
        if (phase != null && phase.isActive() && phase.getBarrier() == barrier) {
          return true;
        }
      }
    }
    return false;
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

  /**
   * Places calls from overlap detection (detector-to-phase assignment). An active overlap with a
   * configured call phase calls that phase while vehicles are present in its output circuit's
   * output-movement sensor zone. Overlaps are otherwise pure outputs, so without this a movement
   * served only by an overlap (e.g. a right-turn pocket) could wait forever with nothing calling
   * the phases that light it.
   */
  private void applyOverlapDemand(TrafficSignalProgrammedPhasePlan plan, boolean[] called) {
    for (TrafficSignalProgrammedOverlap ov : plan.getVehicleOverlaps()) {
      int cp = ov.getCallPhase();
      if (!ov.isActive() || cp < 1 || cp >= called.length || called[cp]) {
        continue;
      }
      TrafficSignalProgrammedPhase phase = plan.getPhase(cp);
      if (phase == null || !phase.isActive()) {
        continue;
      }
      // Same coordination gating as a direct call: outside its permissive window a
      // non-coordinated phase cannot be called.
      if (coordinated && !coordPhase[cp] && !windowOpen(cp)) {
        continue;
      }
      if (zoneCount(ov.getOutputCircuitIndex(), ov.getOutputMovement()) > 0) {
        called[cp] = true;
      }
    }
  }

  /**
   * True if any called-but-unserved phase conflicts with {@code phase} (which ring
   * {@code ringNum} is serving or considering). Calls on the currently-served phases and on
   * {@code phase} itself never conflict.
   */
  private boolean demandConflictsWith(TrafficSignalProgrammedPhase phase, int ringNum,
      TrafficSignalProgrammedPhasePlan plan, boolean[] called) {
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      if (!called[n] || n == ring1.activePhase || n == ring2.activePhase
          || (phase != null && n == phase.getPhaseNumber())) {
        continue;
      }
      if (conflicts(phase, ringNum, plan, n)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Whether a call on phase {@code n} conflicts with {@code active} (served by ring
   * {@code activeRingNum}). Per the ring-and-barrier model the only compatible combination is a
   * phase on the same barrier in the other ring — with one physical exception: an FYA permissive
   * pair (a left and its opposing through) is a crossing movement pair regardless of how a
   * nonstandard sequence assigns rings, so it always conflicts. Unknown/unsequenced phases are
   * treated as conflicting (safe: matches the pre-compatibility behavior of yielding to them).
   */
  private boolean conflicts(TrafficSignalProgrammedPhase active, int activeRingNum,
      TrafficSignalProgrammedPhasePlan plan, int n) {
    if (active == null) {
      return true;
    }
    TrafficSignalProgrammedPhase other = plan.getPhase(n);
    if (other == null || other.getBarrier() != active.getBarrier()) {
      return true;
    }
    int otherRing = ringOf(plan, n);
    if (otherRing == 0 || otherRing == activeRingNum) {
      return true;
    }
    return other.getPermissivePhase() == active.getPhaseNumber()
        || active.getPermissivePhase() == n;
  }

  /** The ring (1 or 2) whose sequence contains {@code phaseNumber}, or 0 if in neither. */
  private int ringOf(TrafficSignalProgrammedPhasePlan plan, int phaseNumber) {
    for (int ring = 1; ring <= 2; ring++) {
      for (int n : plan.getRingSequence(ring)) {
        if (n == phaseNumber) {
          return ring;
        }
      }
    }
    return 0;
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
