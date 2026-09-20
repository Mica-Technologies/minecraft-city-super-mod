package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * With no demand the controller rests in green on the coordinated phases, or on its
 * soft-recall phases — a {@code SOFT} recall places a call whenever nothing conflicting is
 * waiting, which is what brings the rings back to the mains once a side street has cleared.
 *
 * <p>This milestone implements free/actuated operation. Coordination (cycle/offset/splits) and
 * preemption are layered on in later milestones.
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public class RingBarrierState {

  private static final Logger LOGGER = LogManager.getLogger(RingBarrierState.class);

  /** Vehicle indication a ring's active phase is currently displaying. */
  public enum VehInterval { GREEN, YELLOW, RED }

  /** Pedestrian indication accompanying a served phase. */
  public enum PedInterval { NONE, WALK, FDW, DONT_WALK }

  /** Stage of an in-progress preemption sequence. */
  private enum PreemptStage { NONE, ENTER, TRACK_CLEAR, TRACK_EXIT, DWELL, EXIT }

  // Fixed preempt interval timing (ticks). Every stage change that takes a movement from green
  // to red runs the same yellow-then-red clearance: entry (whatever was green), track clear to
  // dwell (the track-clear movements that do not continue into the dwell) and exit (the dwell
  // movements). A movement that continues green into the next stage is never cleared.
  private static final long PREEMPT_YELLOW_TICKS = 70L;       // 3.5 s
  private static final long PREEMPT_REDCLEAR_TICKS = 40L;     // 2 s
  private static final long PREEMPT_TRACK_CLEAR_TICKS = 100L; // 5 s
  private static final long PREEMPT_CLEARANCE_TICKS = PREEMPT_YELLOW_TICKS + PREEMPT_REDCLEAR_TICKS;

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
    // Dual entry: this ring is riding as an uncalled companion to the other ring's same-barrier
    // service. Cross-barrier demand (e.g. a coordinated plan's always-called main-street phases)
    // must not end it early — it clears WITH its companion when the barrier's work is done.
    boolean dualEntry = false;
    // Conditional service: a conditional-service phase has already been re-served once on the
    // current barrier (guard against re-serving more than once / looping).
    boolean condServiceUsed = false;
    // Counts this ring's green services (bumped in startGreen); identifies one green uniquely.
    int serviceSeq = 0;
    // The other ring's serviceSeq at the moment this ring last cleared to idle while the other
    // ring was still serving, or -1. Dual entry never re-enters alongside that same green: a
    // companion that has already cleared beside it would otherwise go green -> yellow -> red ->
    // green the instant the call that ended it dropped (NEMA dual entry is an entry rule, not a
    // re-entry rule — the ring waits at the barrier for the next crossing).
    int clearedAlongside = -1;
    // A coordinated phase outside its own window that has decided to yield for a conflicting
    // phase whose window is about to open: that phase's number, or 0. Latched so the yield still
    // happens when min green or a pedestrian clearance holds the green past the opening point.
    int coordYieldFor = 0;
    // Barrier hold: this ring's green is finished with — everything still calling against it is on
    // another barrier — but it is being held because the other ring is not done with this barrier
    // yet, so terminating would only turn the ring dark. The flag is what the other ring reads to
    // see that this one is ready, so two rings that have both gapped out cannot hold each other.
    boolean barrierWaiting = false;
  }

  private int currentBarrier = 0;
  private final RingRuntime ring1 = new RingRuntime();
  private final RingRuntime ring2 = new RingRuntime();
  private boolean initialized = false;
  /** Whether the first tick serves the rest phases; see {@link #beginOnRestPhases()}. */
  private boolean startOnRestPhases = false;
  private TrafficSignalPhase lastApplied = null;

  /**
   * Output-stage clearance holds: heads the enforcer is currently holding in solid yellow, keyed
   * to the tick the hold began. See {@link #enforceOutputClearance}.
   */
  private final Map<BlockPos, Long> outputClearanceHolds = new HashMap<>();

  /** Yellow interval used by the output-stage clearance when the plan has no phases (ticks). */
  static final long DEFAULT_OUTPUT_CLEARANCE_YELLOW = 80L;

  // Preemption runtime.
  private PreemptStage preemptStage = PreemptStage.NONE;
  private int activePreemptIndex = -1;
  private long preemptStageStart = 0L;
  /** Phases green (or in yellow) at preempt entry that the entry clearance takes to red. */
  private final java.util.Set<Integer> preemptClearPhases = new java.util.HashSet<>();
  /** Phases green at preempt entry that the first preempt stage keeps green (never cleared). */
  private final java.util.Set<Integer> preemptContinuePhases = new java.util.HashSet<>();
  /** Phases whose WALK/FDW was showing at preempt entry: their ped heads flash don't-walk through the entry. */
  private final java.util.Set<Integer> preemptPedClearPhases = new java.util.HashSet<>();

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
  /**
   * Locking detector memory (per-phase LOCK): latched vehicle calls, by phase number. A latch is
   * set when a LOCK phase's zone sees a vehicle while the phase is not being served green, and is
   * discharged by the phase's next green — so the call survives the vehicle leaving the zone.
   * Transient like the rest of this state machine (a chunk reload drops pending latched calls).
   */
  private final boolean[] lockedCalls =
      new boolean[TrafficSignalProgrammedPhasePlan.PHASE_COUNT + 1];
  /**
   * Coordination permissive-window acceptance, by phase number: set when a phase's demand
   * registers while its permissive window is open, and held until the phase is served, its demand
   * drops, or it is forced off. Serving an accepted call takes real time (the coordinated phases'
   * rest-in-walk pedestrian clearance alone can outlast a tight window), so acceptance must
   * survive the window closing — re-gating on the open window each tick would erase the call
   * mid-sequence and the side street would never be served (the mains would recycle WALK &rarr;
   * FDW &rarr; WALK every cycle instead).
   *
   * <p>It must not survive a <em>force-off</em>, though: that means the phase's window closed while
   * it was being served, so it has had its turn. Under continuous demand the latch would otherwise
   * never clear (the "demand drops" condition never fires), leaving a standing side-street call
   * through the whole of the mains' green and pulling the controller off its offset for good.</p>
   */
  private final boolean[] windowAccepted =
      new boolean[TrafficSignalProgrammedPhasePlan.PHASE_COUNT + 1];

  // Per-tick coordination state (computed from the plan each tick; all no-ops in FREE mode).
  private static final int PHASE_SLOTS = TrafficSignalProgrammedPhasePlan.PHASE_COUNT + 1;
  private boolean coordinated = false;
  private long localCycle = 0L;
  /** Coordination cycle length in ticks for this tick's plan; 1 when not coordinated. */
  private long cycleTicks = 1L;
  /** This tick's world time less the offset: the unwrapped background cycle position. */
  private long cycleTime = 0L;
  /**
   * The window instance ({@link #windowInstance}) each phase was last served green in, or
   * {@link Long#MIN_VALUE}. A non-coordinated phase is served at most once per window: a recall
   * phase that gapped out early otherwise re-registered its call for the rest of its window,
   * leaving a standing call that kept the rings from returning to the phases ahead of it.
   */
  private final long[] servedWindow = new long[PHASE_SLOTS];

  {
    java.util.Arrays.fill(servedWindow, Long.MIN_VALUE);
  }
  private final long[] windowStart = new long[PHASE_SLOTS];
  private final long[] windowEnd = new long[PHASE_SLOTS];
  private final boolean[] coordPhase = new boolean[PHASE_SLOTS];
  /** Per-tick demand ignoring the permissive-window gate; see the coordinated yield point. */
  private final boolean[] tickRawCalled = new boolean[PHASE_SLOTS];
  /**
   * Soft-recall calls placed this tick, by phase number. A {@code SOFT} phase places a call
   * whenever no <em>unserved</em> phase with demand of its own conflicts with it, so the
   * controller returns to it and rests there once everything else has been served; see
   * {@link #placeSoftRecallCalls}.
   */
  private final boolean[] softCalled = new boolean[PHASE_SLOTS];
  /**
   * NEMA "phase next" commitment, by phase number: the calls that were conflicting with a phase
   * when it began terminating (green &rarr; yellow) are latched here until served, whatever their
   * detection does afterwards. A real controller fixes its next phase at the start of yellow;
   * without the latch a presence call that drops during the clearance (a villager wandering off
   * the side street) changed where the ring went — the mains cleared and simply went green again
   * for nobody, and a dual-entry companion could re-enter beside the same green. See
   * {@link #commitConflictingCalls}.
   */
  private final boolean[] committedCalls = new boolean[PHASE_SLOTS];
  /** Whether the coordination advisories have been emitted by this engine instance. */
  private boolean coordinationAdvisoriesReported = false;

  /**
   * Which time-of-day coordination pattern is actually running, or {@code -1} before the first
   * tick has resolved one.
   *
   * <p>This is the <em>only</em> thing in the engine that knows patterns exist. Everything below
   * it — force-off points, permissive windows, yield — sees a single
   * {@link TrafficSignalCoordinationPlan} and cannot tell whether a table chose it.</p>
   *
   * <p>Transient like the rest of this class: after a reload it is {@code -1} and resolves from
   * the world clock on the next tick, rather than being remembered. A remembered pattern would
   * be the wrong one whenever a world was saved in one slot and loaded in another.</p>
   */
  private int activeCoordinationSlot = -1;

  /**
   * Last tick's position in the background cycle, used only to notice the cycle rolling over.
   * {@code -1} means there is no previous position to compare against.
   */
  private long previousLocalCycle = -1L;

  // Transit signal priority. Transient like the rest: a grant is a live thing, and a controller
  // that came back from a reload still holding one would be favouring a bus that has long gone.
  /** Whether a transit call is being honoured this tick. */
  private boolean priorityGranted = false;
  /** How much of the extension budget is left in the current grant. */
  private long priorityExtensionLeft = 0L;
  /** The cycle a grant was last made in, so the rate limit has something to count from. */
  private long priorityLastGrantCycle = -1L;
  /** Whether a transit call was standing last tick, so one call is one grant. */
  private boolean priorityCallWasActive = false;

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
    computeCoordination(plan, now, world);

    // Transit priority: decide whether a call is being honoured before any phase is timed.
    computePriority(plan, now);

    // Locking detector memory: latch/discharge LOCK phases' vehicle calls before demand is read.
    updateLockedCalls(plan);

    // Soft recall: place the soft calls against this tick's real demand before demand is read.
    placeSoftRecallCalls(plan);

    // Compute which phases are currently calling for service.
    boolean[] called = new boolean[TrafficSignalProgrammedPhasePlan.PHASE_COUNT + 1];
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      called[n] = isCalled(plan, plan.getPhase(n));
    }
    // The same demand WITHOUT coordination's permissive-window gate. The coordinated phase's yield
    // point needs it: it has to start clearing before the waiting phase's window opens, so it
    // cannot wait for that phase's call to be accepted (see the coordinated yield in advanceRing).
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      tickRawCalled[n] = p != null && p.isActive()
          && p.getCircuitIndex() < tickCircuits.getCircuitCount()
          && ((coordinated && coordPhase[n]) || hasDemand(plan, p));
    }

    if (!initialized) {
      currentBarrier = firstBarrier(plan);
      initialized = true;
      if (startOnRestPhases) {
        // Before any ring is filled from the calls: the rest phases come up green, and whoever
        // is calling is served from there, in turn, once their minimum green is up.
        restInGreen(plan, now);
      }
    }
    reportCoordinationAdvisoriesOnce(plan);

    // Preemption overrides normal/coordinated operation while active.
    updatePreempt(plan, now);
    if (preemptStage != PreemptStage.NONE) {
      resetRings(); // park normal operation so it resumes cleanly after the preempt clears
      return changedOrNull(enforceOutputClearance(
          buildPreemptPhase(world, plan, circuits, overlaps, now), plan, now));
    }

    // 1. Advance each ring's active phase through its intervals (green -> yellow -> red clearance).
    advanceRing(ring1, plan, 1, now, called);
    advanceRing(ring2, plan, 2, now, called);
    // Ring 1 is advanced first, so it cannot see a ring 2 that finishes with the barrier on this
    // same tick. A ring 1 green that is only being held for the barrier therefore re-decides once
    // ring 2 has moved: a concurrent pair that both gap out still goes to yellow together, instead
    // of ring 1 trailing ring 2 by a tick. (Ring 2 needs no such pass — it already sees ring 1.)
    if (ring1.barrierWaiting && ring1.interval == VehInterval.GREEN) {
      advanceRing(ring1, plan, 1, now, called);
    }

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
    return changedOrNull(enforceOutputClearance(AdvancedPhaseBuilder.build(
        world, plan, circuits, overlaps, m1, m2, overlapIntervals, fyaHoldFlash), plan, now));
  }

  /**
   * Output-stage clearance enforcer. The ring phases themselves always time green &rarr; yellow
   * &rarr; red, but several layered outputs are computed statelessly from the rings and can drop
   * from GREEN (or FYA) straight to RED: an overlap whose trailing green outlasts its parents'
   * clearance, a {@code -GRN/YEL} modifier phase coming up while the overlap (or its permissive
   * flash) is on, a preempt entry catching an overlap. A real controller times an overlap
   * yellow (and red) clearance in those cases, and an MMU faults the intersection if it sees the
   * skip. This pass gives every such head its yellow: any head that was GREEN or FYA in the last
   * applied phase and would now be RED is held at solid YELLOW for the plan's yellow interval,
   * then released to RED.
   *
   * <p>Holds are keyed by head position and survive across ticks in {@link #outputClearanceHolds};
   * a head that becomes green or FYA again while held is released immediately (nothing to
   * clear). States are compared after {@link TrafficSignalPhase#resolveVehicleSignalStates()} so
   * dual-member bimodal heads are judged on what they display.</p>
   */
  TrafficSignalPhase enforceOutputClearance(TrafficSignalPhase phase,
      TrafficSignalProgrammedPhasePlan plan, long now) {
    return enforceOutputClearance(lastApplied, phase, outputClearanceHolds,
        outputClearanceYellow(plan), now);
  }

  /** Pure form of {@link #enforceOutputClearance(TrafficSignalPhase, TrafficSignalProgrammedPhasePlan, long)}. */
  static TrafficSignalPhase enforceOutputClearance(TrafficSignalPhase previous,
      TrafficSignalPhase phase, Map<BlockPos, Long> holds, long yellowTicks, long now) {
    if (phase == null) {
      return null;
    }
    Map<BlockPos, Integer> before = previous == null
        ? java.util.Collections.<BlockPos, Integer>emptyMap()
        : previous.resolveVehicleSignalStates();
    Map<BlockPos, Integer> after = phase.resolveVehicleSignalStates();

    // Start a hold for every head about to skip its yellow
    for (Map.Entry<BlockPos, Integer> entry : after.entrySet()) {
      if (entry.getValue() != AbstractBlockControllableSignal.SIGNAL_RED) {
        continue;
      }
      Integer was = before.get(entry.getKey());
      boolean permissive = was != null
          && (was == AbstractBlockControllableSignal.SIGNAL_GREEN
              || was == TrafficSignalPhase.INDICATION_FYA);
      if (permissive && !holds.containsKey(entry.getKey())) {
        holds.put(entry.getKey(), now);
      }
    }

    // Apply / expire holds
    Iterator<Map.Entry<BlockPos, Long>> it = holds.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<BlockPos, Long> hold = it.next();
      BlockPos pos = hold.getKey();
      Integer state = after.get(pos);
      boolean stillRed = state != null && state == AbstractBlockControllableSignal.SIGNAL_RED;
      if (!stillRed || now - hold.getValue() >= yellowTicks) {
        it.remove();
        continue;
      }
      phase.moveOverlapSignalToYellow(pos);
    }
    return phase;
  }

  /** The longest yellow interval programmed on any phase of the plan (ticks), or the default. */
  static long outputClearanceYellow(TrafficSignalProgrammedPhasePlan plan) {
    long yellow = 0L;
    if (plan != null) {
      for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
        TrafficSignalProgrammedPhase phase = plan.getPhase(n);
        if (phase != null) {
          yellow = Math.max(yellow, phase.getYellow());
        }
      }
    }
    return yellow > 0L ? yellow : DEFAULT_OUTPUT_CLEARANCE_YELLOW;
  }

  /**
   * The FYA left phases whose permissive flash should be held (not cleared) this tick because their
   * own protected green is coming next: the phase is called, on the current barrier, not yet being
   * served, and is what its ring will serve next on this barrier ({@link #servesNextOnBarrier}).
   * The flash then holds through the opposing through's clearance and goes flash &rarr; protected
   * green directly, with no solid-yellow/red on the FYA head (a flashing yellow is always a safe
   * yield, so holding it is safe). Left phases not in this set clear their flash normally (solid
   * yellow with the opposing through, then red).
   *
   * <p>"Called and on this barrier" alone is not enough: a left whose sequence slot the ring has
   * already passed is <em>not</em> served before the barrier crosses when the other barrier has
   * demand (NEMA: it waits for the next cycle). Holding its flash anyway carried a flashing arrow
   * through the all-red and then, when the cross dropped it out of the hold set, the output
   * clearance painted a solid yellow arrow over the cross street's green.</p>
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
      int ringNum = ringOf(plan, pn);
      if (ringNum == 0) {
        continue;
      }
      RingRuntime ring = ringNum == 1 ? ring1 : ring2;
      if (servesNextOnBarrier(ring, ringNum, p, plan, called)) {
        hold.add(pn);
      }
    }
    return hold;
  }

  /**
   * Whether {@code phase} (called, active, on the current barrier, not being served) is what
   * {@code ring} will serve next on this barrier — mirroring {@link #fillIdleRing}'s selection:
   * the forward sequence first, then conditional service, then the within-barrier wrap (which
   * applies only when every calling phase is on this barrier).
   */
  private boolean servesNextOnBarrier(RingRuntime ring, int ringNum,
      TrafficSignalProgrammedPhase phase, TrafficSignalProgrammedPhasePlan plan,
      boolean[] called) {
    int pn = phase.getPhaseNumber();
    int next = peekNextWithinBarrier(ring, ringNum, plan, called);
    if (next != 0) {
      return next == pn;
    }
    if (!ring.condServiceUsed && phase.isConditionalService()) {
      return true;
    }
    if (nextBarrierWithDemand(plan, called) != currentBarrier) {
      return false;
    }
    for (int n : plan.getRingSequence(ringNum)) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      if (p != null && p.isActive() && p.getBarrier() == currentBarrier
          && n >= 1 && n < called.length && called[n]
          && n != ring1.activePhase && n != ring2.activePhase) {
        return n == pn;
      }
    }
    return false;
  }

  /**
   * Computes each vehicle overlap's effective interval this tick, applying lag (trailing) and lead
   * (advance) green: an overlap is green while its included phases are green, for {@code trailGreen}
   * ticks after they leave green, and for {@code leadGreen} ticks before an included phase greens
   * (during the preceding within-barrier red clearance); otherwise it follows the stateless base
   * decision (yellow during the included phases' clearance, else red).
   *
   * <p>On top of those configured extensions the overlap is held green through a parent phase's
   * clearance whenever the ring is going straight on to another of the overlap's included phases
   * ({@link #holdsBetweenIncluded}) — the whole point of an overlap is that it does not care which
   * of its parents is up.</p>
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
        // Parent to parent: a ring clearing one included phase straight into another needs no
        // clearance on the overlap itself, so hold it green rather than cycling it to red.
        if (eff != VehInterval.GREEN && holdsBetweenIncluded(ov, plan, called)) {
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

  /**
   * Whether the overlap is running from one of its parents straight into another, so that nothing
   * on the overlap's own movement is being taken away and it should stay green through the
   * clearance between them.
   *
   * <p>An overlap is green while any of its {@link TrafficSignalProgrammedOverlap#getIncludedPhases()
   * included} phases is green, so a ring stepping from one included phase to the next — a right
   * turn overlapping both of the side street's phases, say — has the overlap green on either side
   * of the change. Without this hold the stateless base decision yellows and reds it in between
   * purely because the <em>parent</em> is clearing, and the driver on the overlap gets a red
   * between two greens for no reason. A real controller carries the overlap straight through: the
   * clearance belongs to the movement that is ending, and the overlap's movement is not.</p>
   *
   * <p>Deliberately limited to a change <em>within</em> the current barrier ({@link
   * #peekNextWithinBarrier} answers 0 across one). Crossing a barrier starts the conflicting
   * movements in the other ring, and the overlap's included phases say nothing about those, so the
   * overlap clears normally there.</p>
   *
   * <p>A parent that starts with a leading pedestrian interval ({@code DLY GRN}) is not held into
   * either: that delay exists to give the pedestrians crossing the overlap's own path a head
   * start, and the overlap has to be red for it.</p>
   */
  private boolean holdsBetweenIncluded(TrafficSignalProgrammedOverlap ov,
      TrafficSignalProgrammedPhasePlan plan, boolean[] called) {
    return ringHoldsBetweenIncluded(ring1, 1, ov, plan, called)
        || ringHoldsBetweenIncluded(ring2, 2, ov, plan, called);
  }

  /** {@link #holdsBetweenIncluded} for one ring. */
  private boolean ringHoldsBetweenIncluded(RingRuntime ring, int ringNum,
      TrafficSignalProgrammedOverlap ov, TrafficSignalProgrammedPhasePlan plan, boolean[] called) {
    if (ring.activePhase == 0 || ring.interval == VehInterval.GREEN
        || !contains(ov.getIncludedPhases(), ring.activePhase)) {
      return false; // not clearing one of this overlap's parents
    }
    int next = peekNextWithinBarrier(ring, ringNum, plan, called);
    if (next == 0 || !contains(ov.getIncludedPhases(), next)) {
      return false; // across the barrier, or on to something the overlap does not run with
    }
    TrafficSignalProgrammedPhase upcoming = plan.getPhase(next);
    return upcoming != null && upcoming.getDelayedGreen() <= 0L;
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
    ring.dualEntry = false;
    ring.condServiceUsed = false;
    ring.clearedAlongside = -1;
    ring.coordYieldFor = 0;
    ring.barrierWaiting = false;
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
        // A dual-entry companion that picks up demand of its own (a vehicle in its zone, a button
        // press) is from here an ordinary served phase: it extends on its own passage timer and is
        // subject to force-off, the coordinated yield and cross-barrier conflict like any other.
        // Left flagged, it had no split at all — only max-out could end it, so under coordination
        // it held the mains out for a full max green after its companion had already cleared.
        if (ring.dualEntry && (vehicleCount(phase) > 0 || pedRequestPresent(phase))) {
          ring.dualEntry = false;
        }
        // Only demand that actually conflicts with THIS phase can end it. A call on the same
        // barrier in the other ring (e.g. a left turn beside its adjacent through) is compatible
        // and must not clear this green — the other ring handles it while this one keeps running.
        boolean conflict = demandConflictsWith(phase, ringNum, plan, called);
        RingRuntime companion = (ring == ring1) ? ring2 : ring1;
        int companionRingNum = (ring == ring1) ? 2 : 1;
        if (ring.dualEntry) {
          // A dual-entry companion rides with the other ring's same-barrier service. Cross-barrier
          // demand alone must NOT end it early — under coordination the main-street phases are
          // always called, which would otherwise strip the companion right back off the side
          // street. It yields only to within-barrier demand it conflicts with, or when the
          // companion ring finishes its barrier work (so the pair clears together).
          conflict = withinBarrierConflict(phase, ringNum, plan, called)
              || !ringWorkingBarrier(companion, companionRingNum, plan, called);
        }
        int phaseNum = ring.activePhase;
        boolean isCoord = coordinated && coordPhase[phaseNum];

        // Coordinated yield point. A coordinated phase has no force-off, so it can only be taken
        // off green once a conflicting call is ACCEPTED — which cannot happen until that phase's
        // own permissive window opens. With a tight side-street split that is far too late: the
        // mains' pedestrian clearance, yellow and red all land inside the side street's window, the
        // side street is then served entirely outside its window on bare min green, and the whole
        // cycle sits permanently late with nothing to ever take the lateness back.
        //
        // So give the coordinated phase a yield point of its own, set back from its window end by
        // everything its termination actually costs — pedestrian clearance when it is serving one,
        // plus yellow and red — so the next phase's green begins at that phase's window start. It
        // yields only when a conflicting movement is genuinely waiting, judged on raw demand
        // because the waiting phase's window has not opened yet; with nothing calling anywhere the
        // coordinated phase keeps resting in green, which is the whole point of coordination.
        //
        // The same test is what stops the coordinated phase yielding EARLY: until its yield point
        // it holds green (past max green, which coordinated phases are already exempt from) rather
        // than giving the time away and shifting the rest of the cycle. A coordinated phase that
        // finds itself outside its own window dwells on green until the cycle comes back round to
        // that point — add-only offset correction; see coordYieldReached.
        // Budget the pedestrian clearance this phase still OWES, not the whole interval: with a
        // ped recall the walk and its clearance run once from green start and are usually long
        // finished by the yield point, so charging the full interval yields that much too early —
        // the next phase then starts before its own window opens and is force-off'd on bare min
        // green, starving it. A phase resting on WALK is the exception: it recycles the walk, so
        // its clearance has not started counting down and it still owes all of it.
        long pedRemaining = 0L;
        if (ring.pedServing) {
          pedRemaining = ring.resting ? phase.getPedClear()
              : Math.max(0L, ring.pedStart + ring.walkHold + phase.getPedClear() - now);
        }
        long coordClearance = phase.getYellow() + phase.getRedClear() + pedRemaining;
        // A coordinated green OUTSIDE its own window is not always running late: after an early
        // return (a side street that gapped out) it came up early, and the time it is filling may
        // be another conflicting phase's window — typically a lead left at the top of the cycle.
        // Dwelling to its own yield point there held the left's accepted call until the far end of
        // the window, so the left then ran straight after the side street every cycle. So it also
        // yields, early enough for that phase to start on time, when a waiting conflicting phase's
        // window is about to open. Nothing is shortened: the time given up was never its own.
        if (isCoord && !ring.dualEntry && ring.coordYieldFor == 0) {
          ring.coordYieldFor = conflictingWindowOpening(phase, ringNum, plan, coordClearance);
        }
        if (ring.coordYieldFor != 0 && !tickRawCalled[ring.coordYieldFor]) {
          ring.coordYieldFor = 0; // the waiting phase's demand went away before the clearance
        }
        int windowOpeningFor = isCoord && !ring.dualEntry ? ring.coordYieldFor : 0;
        boolean coordYieldDue = isCoord && !ring.dualEntry
            && ((coordYieldReached(phaseNum, coordClearance, now - ring.greenStart)
            && demandConflictsWith(phase, ringNum, plan, tickRawCalled))
            || windowOpeningFor != 0);
        // While a coordinated phase is holding for its yield point, ordinary gap-out/conflict
        // termination must not take it off green — that is what the hold IS.
        boolean coordHold = isCoord && !ring.dualEntry && !coordYieldDue;

        // Rest-in-walk clearance: a phase resting on WALK cannot snap the walk straight to
        // don't-walk when a conflicting call arrives — it must first run pedestrian clearance
        // (FDW). End the rest (the vehicle stays green) and re-arm the ped timer to the start of
        // FDW; the terminate logic below then holds the green until the clearance finishes. A
        // coordinated phase reaching its yield point starts the same clearance, so that the walk
        // it is resting on ends early enough for the next phase to start on time.
        if (ring.resting && ring.pedServing && phase.isRestInWalk()
            && (conflict || coordYieldDue)) {
          ring.resting = false;
          // Start the clearance now — unless the walk being rested on is still inside its own
          // walk interval (a phase re-flagged resting during its first WALK), in which case the
          // walk times out first: a WALK is never cut short of its walk time.
          ring.pedStart = Math.max(ring.pedStart, now - ring.walkHold);
        }
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
        effMaxGreen = applyPriorityToMaxGreen(plan, phaseNum, phase, ringNum, effMaxGreen,
            effMinGreen);
        long effPassage = AdvancedActuationTiming.effectivePassage(phase.getPassage(),
            phase.getMinGap(), phase.getTimeBeforeReduce(), phase.getTimeToReduce(), greenElapsed);
        boolean minMet = greenElapsed >= effMinGreen;
        boolean pedDone = !ring.pedServing
            || (now - ring.pedStart) >= (ring.walkHold + phase.getPedClear());
        // A pedestrian call on the phase already in green — the button pressed after its walk
        // finished, or while it holds/rests with no ped service — is served now if nothing
        // conflicting is waiting (the phase is resting; a real controller recycles the walk).
        // With a conflicting call the phase terminates instead and the latched request recalls
        // it for the next service. Without this the request was never served and, since the
        // requester only resets on WALK/FDW, it stayed lit for good.
        if (pedDone && !conflict && !coordYieldDue && !ring.delayActive
            && !(ring.resting && phase.isRestInWalk()) && pedRequestPresent(phase)) {
          ring.pedServing = true;
          ring.pedStart = now;
          ring.walkHold = phase.getWalk();
          pedDone = false;
        }
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
        // Coordinated force-off: a non-coordinated phase must end at its yield point — its window
        // end less its own clearance — so the next phase's green starts at its window start rather
        // than a clearance later (see pastYieldPoint, which also covers the cycle wrap and a phase
        // served outside its window). Like max-out, force-off never truncates pedestrian clearance
        // (the walk/FDW finishes first), and a resting phase is exempt (with no demand anywhere
        // there is nothing to return the time to). A dual-entry companion is exempt while riding —
        // its own split/force-off apply only when it is served on its own call; the pair ends via
        // the companion's termination.
        boolean forceOff = coordinated && !isCoord && !ring.resting && !ring.dualEntry
            && pastYieldPoint(plan, phaseNum, phase.getYellow() + phase.getRedClear()) && pedDone;

        // Terminate on max-out or force-off, or once min green is met, ped clearance is done, the
        // phase has gapped out, and something conflicting is actually waiting (otherwise rest in
        // green). A dual-entry companion follows the same rule: with no conflicting demand it
        // simply holds green alongside whatever the other ring serves on this barrier.
        boolean terminate = maxOut || forceOff || (coordYieldDue && pedDone)
            || (!coordHold && minMet && pedDone && gapOut && conflict);

        // Hold at the barrier. Everything still calling against this green is on ANOTHER barrier,
        // so terminating releases nothing: the barrier cannot cross while the other ring is still
        // working this one, and the ring would go dark (or clear and immediately re-serve) beside
        // a green it does not conflict with. That is the reported "phase 6 goes fully red between
        // 1+6 and 2+6" — the calls on the side street gapped the unopposed 6 out while ring 1 was
        // still running the lead left and its through. Hold the green and end it the moment the
        // other ring is ready; the max-green timer keeps running underneath (NEMA MAX times
        // against any conflicting call), so nothing is given away by waiting.
        //
        // Coordination's own terminations are exempt: force-off and the coordinated yield are
        // timed against the cycle, both rings reach them together, and delaying either would
        // shift the whole plan. A dual-entry companion neither holds nor is held — it already
        // rides with, and clears with, the ring it companions.
        //
        // Two rings that have both finished must not hold each other, so a held ring flags itself
        // ready (barrierWaiting) and a ring whose companion is flagged terminates at once; the
        // flagged ring then re-decides in the same tick (see the second advanceRing pass in
        // tick()), so a concurrent pair still goes to yellow together and crosses together.
        boolean barrierHold = terminate && minMet && !forceOff && !coordYieldDue
            && !ring.dualEntry && !companion.dualEntry
            && !withinBarrierConflict(phase, ringNum, plan, called)
            && ringWorkingBarrier(companion, companionRingNum, plan, called)
            && !companion.barrierWaiting;
        ring.barrierWaiting = barrierHold;
        if (barrierHold) {
          terminate = false;
        }
        if (terminate && minMet) {
          // Phase next: commit to the calls this termination is for.
          commitConflictingCalls(phase, ringNum, plan, called);
          if (windowOpeningFor != 0 && coordYieldDue) {
            // Its window has not opened yet, so its call is not accepted and the commitment
            // above missed it; without this the idle ring would re-serve the coordinated phase.
            committedCalls[windowOpeningFor] = true;
          }
          if (forceOff) {
            // Forcing off consumes the window acceptance: the phase's window has closed, so it has
            // had its chance this cycle and the remaining time belongs to the coordinated phase.
            // Without this the acceptance latch survives under continuous demand (it is only
            // cleared when the demand itself drops), leaving the side street with a standing call
            // straight through the mains' green — which terminates the coordinated phase after
            // bare min green and pulls the controller permanently off its offset.
            windowAccepted[phaseNum] = false;
          }
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
          RingRuntime other = ring == ring1 ? ring2 : ring1;
          ring.clearedAlongside = other.activePhase != 0 ? other.serviceSeq : -1;
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
   * intersection isn't left with one direction dark. The entered companion is flagged
   * ({@link RingRuntime#dualEntry}) to ride with the other ring's barrier service: cross-barrier
   * demand — including a coordinated plan's always-called main-street phases — doesn't strip it
   * off early; it clears with its companion (see the dual-entry conflict override in
   * {@link #advanceRing}). A candidate that pending <em>within-barrier</em> demand conflicts with
   * is skipped (it would have to clear immediately — flicker), and a companion is never entered
   * off another companion's green (mutual riders would hold each other's green forever).
   */
  private void fillDualEntry(RingRuntime idle, RingRuntime other,
      TrafficSignalProgrammedPhasePlan plan, int idleRingNum, long now, boolean[] called) {
    if (idle.activePhase != 0 || other.activePhase == 0 || other.dualEntry
        || other.serviceSeq == idle.clearedAlongside) {
      return; // (last case: this ring already served and cleared beside that same green)
    }
    TrafficSignalProgrammedPhase otherPhase = plan.getPhase(other.activePhase);
    if (otherPhase == null || otherPhase.getBarrier() != currentBarrier
        || other.interval != VehInterval.GREEN) {
      return;
    }
    int[] seq = plan.getRingSequence(idleRingNum);
    for (int idx = 0; idx < seq.length; idx++) {
      TrafficSignalProgrammedPhase phase = plan.getPhase(seq[idx]);
      if (phase != null && phase.isActive() && phase.getBarrier() == currentBarrier
          && phase.isDualEntry() && !withinBarrierConflict(phase, idleRingNum, plan, called)) {
        startGreen(idle, phase, now);
        idle.dualEntry = true;
        idle.sequencePos = idx;
        return;
      }
    }
  }

  private void startGreen(RingRuntime ring, TrafficSignalProgrammedPhase phase, long now) {
    if (phase.getPhaseNumber() >= 1 && phase.getPhaseNumber() < lockedCalls.length) {
      lockedCalls[phase.getPhaseNumber()] = false; // LOCK: service discharges the latched call
      windowAccepted[phase.getPhaseNumber()] = false; // service consumes the window acceptance
      committedCalls[phase.getPhaseNumber()] = false; // service discharges the commitment
      if (coordinated) {
        servedWindow[phase.getPhaseNumber()] = windowInstance(phase.getPhaseNumber());
      }
    }
    ring.activePhase = phase.getPhaseNumber();
    ring.serviceSeq++;
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
    ring.dualEntry = false; // normal service; fillDualEntry re-flags its own entries afterward
    ring.coordYieldFor = 0;
    ring.barrierWaiting = false;
  }

  /**
   * Whether {@code ring} is still working the current barrier: serving a green phase on it, or —
   * mid-clearance or idle — about to serve another called phase on it, via the forward sequence
   * ({@link #peekNextWithinBarrier}) or the within-barrier wrap (which, mirroring
   * {@link #fillIdleRing}, applies only when every calling phase is on this barrier). A dual-entry
   * companion in the other ring keeps riding while this is true and clears when it turns false.
   */
  private boolean ringWorkingBarrier(RingRuntime ring, int ringNum,
      TrafficSignalProgrammedPhasePlan plan, boolean[] called) {
    if (ring.activePhase != 0) {
      TrafficSignalProgrammedPhase active = plan.getPhase(ring.activePhase);
      if (active == null || active.getBarrier() != currentBarrier) {
        return false;
      }
      if (ring.interval == VehInterval.GREEN) {
        return true;
      }
    }
    if (peekNextWithinBarrier(ring, ringNum, plan, called) != 0) {
      return true;
    }
    if (nextBarrierWithDemand(plan, called) == currentBarrier) {
      int[] seq = plan.getRingSequence(ringNum);
      for (int idx = 0; idx < seq.length; idx++) {
        int phaseNumber = seq[idx];
        TrafficSignalProgrammedPhase phase = plan.getPhase(phaseNumber);
        if (phase != null && phase.isActive() && phase.getBarrier() == currentBarrier
            && phaseNumber >= 1 && phaseNumber < called.length && called[phaseNumber]
            && phaseNumber != ring1.activePhase && phaseNumber != ring2.activePhase) {
          return true;
        }
      }
    }
    return false;
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

  /**
   * Serves the coordinated (or soft-recall, or first active) phase in each ring at green with no
   * max-out. Both rings rest on the <em>same</em> barrier: ring 1's choice fixes the barrier and
   * ring 2 rests on its preferred phase of that barrier (or stays dark if it has none there) —
   * two rest phases on different barriers would be conflicting movements shown green together.
   */
  private void restInGreen(TrafficSignalProgrammedPhasePlan plan, long now) {
    int rest1 = restPhaseForRing(plan, 1, -1);
    int barrier = -1;
    if (rest1 != 0) {
      TrafficSignalProgrammedPhase p1 = plan.getPhase(rest1);
      barrier = p1 == null ? -1 : p1.getBarrier();
    }
    int rest2 = restPhaseForRing(plan, 2, barrier);
    restRing(ring1, plan, 1, rest1, now);
    restRing(ring2, plan, 2, rest2, now);
  }

  private void restRing(RingRuntime ring, TrafficSignalProgrammedPhasePlan plan, int ringNum,
      int restPhase, long now) {
    if (ring.activePhase != 0 || restPhase == 0) {
      return;
    }
    TrafficSignalProgrammedPhase phase = plan.getPhase(restPhase);
    if (phase == null) {
      return;
    }
    currentBarrier = phase.getBarrier();
    startGreen(ring, phase, now);
    ring.resting = true;
    // Rest in Walk: hold the WALK indication on this phase while resting. Otherwise keep the ped
    // service startGreen armed (a ped recall / button request still gets its walk on the rest
    // phase — overwriting it here silently dropped the recall) and don't-walk follows it.
    ring.pedServing = phase.isRestInWalk() || ring.pedServing;
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

  /**
   * The phase ring {@code ringNum} should rest on: a coordinated phase, else a soft-recall phase,
   * else its first active phase — restricted to {@code barrier} unless that is {@code -1}.
   */
  private int restPhaseForRing(TrafficSignalProgrammedPhasePlan plan, int ringNum, int barrier) {
    int[] seq = plan.getRingSequence(ringNum);
    // Prefer a coordinated phase in this ring.
    for (int n : seq) {
      TrafficSignalProgrammedPhase phase = plan.getPhase(n);
      if (phase != null && phase.isActive() && (barrier < 0 || phase.getBarrier() == barrier)
          && plan.getCoordination().isCoordinatedPhase(n)) {
        return n;
      }
    }
    // Then a Soft Recall phase: the configured place to rest when nothing else is calling.
    for (int n : seq) {
      TrafficSignalProgrammedPhase phase = plan.getPhase(n);
      if (phase != null && phase.isActive() && (barrier < 0 || phase.getBarrier() == barrier)
          && phase.getRecallMode() == TrafficSignalRecallMode.SOFT) {
        return n;
      }
    }
    // Otherwise the first active phase in the ring.
    for (int n : seq) {
      TrafficSignalProgrammedPhase phase = plan.getPhase(n);
      if (phase != null && phase.isActive() && (barrier < 0 || phase.getBarrier() == barrier)) {
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

  /**
   * Makes this engine, which must not have ticked yet, come up on its rest phases -- the
   * coordinated phases, else the soft-recall ones, else each ring's first: the main street --
   * rather than on whichever phase of the first barrier happens to have a call. A controller
   * leaving flash starts here (MUTCD 4D.31: steady operation begins with the major street
   * green), where an ordinary cold start would hand a waiting left turn the first green.
   *
   * @see TrafficSignalStartupFlash
   * @since 2026.9
   */
  public void beginOnRestPhases() {
    startOnRestPhases = true;
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
  private void computeCoordination(TrafficSignalProgrammedPhasePlan plan, long now,
      World world) {
    int scheduled = plan.getScheduledCoordinationSlot(world);
    if (activeCoordinationSlot < 0) {
      // Cold start (first tick, or the first after a reload): take whatever the clock says now.
      activeCoordinationSlot = scheduled;
    }
    TrafficSignalCoordinationPlan co = plan.getCoordination(activeCoordinationSlot);
    coordinated = co.isCoordinated();
    for (int i = 0; i < PHASE_SLOTS; i++) {
      windowStart[i] = 0L;
      windowEnd[i] = 0L;
      coordPhase[i] = false;
    }
    if (!coordinated) {
      cycleTicks = 1L;
      activeCoordinationSlot =
          nextCoordinationSlot(activeCoordinationSlot, scheduled, false, 0L, -1L);
      previousLocalCycle = -1L;
      return;
    }
    long cycle = Math.max(1L, co.getCycleLength());
    cycleTicks = cycle;
    localCycle = ((now - co.getOffset()) % cycle + cycle) % cycle;
    cycleTime = now - co.getOffset();

    activeCoordinationSlot = nextCoordinationSlot(activeCoordinationSlot, scheduled, true,
        localCycle, previousLocalCycle);
    previousLocalCycle = localCycle;

    // Windows are laid out barrier by barrier, not ring by ring. The rings cross a barrier
    // together, so a barrier is one slice of the cycle that both rings share: its length is the
    // longer of the two rings' split totals on it, and a ring with no phase on a barrier simply
    // waits at it. Tiling each ring's phases over the whole cycle on its own broke exactly that
    // case — a T intersection whose ring 2 has only phase 6 got a phase-6 window spanning the
    // entire cycle, across the side street's time, whatever split was typed for it.
    BarrierSplits bs = barrierSplits(plan, co, cycle);
    long total = 0L;
    for (int b = 0; b < bs.order.length; b++) {
      total += Math.max(bs.total[0][b], bs.total[1][b]);
    }
    long barrierStart = 0L;
    for (int b = 0; b < bs.order.length && total > 0L; b++) {
      // Barriers are normalised to tile the cycle; the last absorbs rounding.
      long len = b == bs.order.length - 1 ? cycle - barrierStart
          : Math.max(bs.total[0][b], bs.total[1][b]) * cycle / total;
      for (int ring = 1; ring <= 2; ring++) {
        List<Integer> onBarrier = activeOnBarrier(plan, ring, bs.order[b]);
        long ringTotal = bs.total[ring - 1][b];
        long cum = barrierStart;
        for (int i = 0; i < onBarrier.size(); i++) {
          int n = onBarrier.get(i);
          // A ring shorter than the barrier has its phases stretched in proportion to fill it.
          long w = ringTotal > 0L ? splitOrShare(plan, co, ring, n, cycle) * len / ringTotal
              : len / onBarrier.size();
          windowStart[n] = cum;
          cum += w;
          // Last phase on the barrier absorbs rounding so the ring meets the barrier exactly.
          windowEnd[n] = (i == onBarrier.size() - 1) ? barrierStart + len : cum;
        }
      }
      barrierStart += len;
    }
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      coordPhase[n] = co.isCoordinatedPhase(n);
    }
  }

  /**
   * Works out whether a transit call is being honoured this tick.
   *
   * <p>A grant lasts as long as the call does, and spends an extension budget that does not
   * refill until the call drops — so a bus sitting in a detection zone cannot hold a green
   * indefinitely. The rate limit is counted in whole cycles, which is the only unit that means
   * anything to a coordinated corridor.</p>
   */
  private void computePriority(TrafficSignalProgrammedPhasePlan plan, long now) {
    TrafficSignalPriorityPlan priority = plan.getPriority();
    if (!priority.isRunnable()) {
      priorityGranted = false;
      priorityCallWasActive = false;
      return;
    }
    boolean call = zoneCount(priority.getTriggerCircuitIndex(),
        priority.getTriggerMovement()) > 0;
    long cycle = coordinated && cycleTicks > 1L ? now / cycleTicks : -1L;

    if (!call) {
      // The call dropping is what ends a grant and refills the budget: without that a route
      // with continuous demand would be granted priority forever on one call.
      priorityGranted = false;
      priorityCallWasActive = false;
      return;
    }
    if (!priorityCallWasActive) {
      // A fresh call. Grant it only if the rate limit allows, and start a new budget.
      priorityCallWasActive = true;
      if (priority.mayGrant(cycle, priorityLastGrantCycle)) {
        priorityGranted = true;
        priorityExtensionLeft = priority.getMaxExtension();
        priorityLastGrantCycle = cycle;
      } else {
        priorityGranted = false;
      }
    }
    if (priorityGranted && priorityExtensionLeft > 0L) {
      priorityExtensionLeft--;
    }
  }

  /**
   * Applies a granted priority to one phase's maximum green.
   *
   * <p>Only the maximum moves. The engine will not terminate a phase before its minimum green
   * and its pedestrian clearance are done whatever this returns, so priority can lengthen a
   * green or bring one forward but can never cut one short of what is safe.</p>
   */
  private long applyPriorityToMaxGreen(TrafficSignalProgrammedPhasePlan plan, int phaseNum,
      TrafficSignalProgrammedPhase phase, int ringNum, long effMaxGreen, long effMinGreen) {
    if (!priorityGranted) {
      return effMaxGreen;
    }
    TrafficSignalPriorityPlan priority = plan.getPriority();
    int transit = priority.getTransitPhase();
    boolean isTransit = phaseNum == transit;
    boolean conflictsWithTransit = !isTransit && conflicts(phase, ringNum, plan, transit);
    return priority.adjustMaxGreen(effMaxGreen, effMinGreen, isTransit, conflictsWithTransit,
        priorityExtensionLeft);
  }

  /**
   * Decides which coordination pattern runs next.
   *
   * <p>The rule this encodes is the whole of time-of-day patterning as far as the engine is
   * concerned, which is why it is a function rather than a few lines inline: a pattern may only
   * be adopted at a cycle boundary. A pattern with a different cycle length or offset taken up
   * mid-cycle would move every force-off point out from under the phases already timing against
   * them, and the phases would clear late or early for the rest of that cycle.</p>
   *
   * <p>Two cases skip the wait, both because there is nothing to wait for. A cold start has no
   * pattern yet, and a free pattern has no cycle.</p>
   *
   * @param activeSlot         the pattern running now, or negative if none has been chosen yet
   * @param scheduledSlot      the pattern the clock selects
   * @param coordinated        whether the running pattern is coordinated at all
   * @param localCycle         this tick's position in the background cycle
   * @param previousLocalCycle last tick's position, or negative if there was none
   *
   * @return the pattern to run from now on
   */
  static int nextCoordinationSlot(int activeSlot, int scheduledSlot, boolean coordinated,
      long localCycle, long previousLocalCycle) {
    if (activeSlot < 0 || !coordinated) {
      return scheduledSlot;
    }
    boolean rolledOver = previousLocalCycle >= 0L && localCycle < previousLocalCycle;
    return rolledOver ? scheduledSlot : activeSlot;
  }

  /**
   * Reports the coordination advisories — an over-subscribed split ({@link #findSplitShortfall})
   * and barrier split totals that differ between the rings ({@link #findBarrierMisalignment}) —
   * once per engine instance (so once per load, not once per tick).
   */
  private void reportCoordinationAdvisoriesOnce(TrafficSignalProgrammedPhasePlan plan) {
    if (!coordinated || coordinationAdvisoriesReported) {
      return;
    }
    coordinationAdvisoriesReported = true;
    for (String advisory : new String[] {findSplitShortfall(plan), findBarrierMisalignment(plan)}) {
      if (advisory != null) {
        LOGGER.error("Traffic signal controller coordination advisory: " + advisory);
      }
    }
  }

  /** Each ring's split total on each barrier, with the barriers in the order the rings cross them. */
  static final class BarrierSplits {
    /** Barrier numbers in crossing order, starting at the barrier ring 1's sequence opens on. */
    final int[] order;
    /** {@code total[ring - 1][i]}: that ring's split total on {@code order[i]}, in ticks. */
    final long[][] total;

    BarrierSplits(int[] order) {
      this.order = order;
      this.total = new long[2][order.length];
    }
  }

  /**
   * Adds up each ring's splits per barrier for a coordination pattern. The barrier order matches
   * {@link #handleBarrier}'s rotation (ascending, starting at {@link #firstBarrier}), so the
   * windows built from it run in the order the rings actually cross.
   */
  BarrierSplits barrierSplits(TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalCoordinationPlan co, long cycle) {
    int[] ascending = distinctBarriers(plan);
    int first = firstBarrier(plan);
    int start = 0;
    for (int i = 0; i < ascending.length; i++) {
      if (ascending[i] == first) {
        start = i;
        break;
      }
    }
    int[] order = new int[ascending.length];
    for (int i = 0; i < ascending.length; i++) {
      order[i] = ascending[(start + i) % ascending.length];
    }
    BarrierSplits bs = new BarrierSplits(order);
    for (int ring = 1; ring <= 2; ring++) {
      for (int b = 0; b < order.length; b++) {
        for (int n : activeOnBarrier(plan, ring, order[b])) {
          bs.total[ring - 1][b] += splitOrShare(plan, co, ring, n, cycle);
        }
      }
    }
    return bs;
  }

  /** The active phases of a ring's sequence that sit on {@code barrier}, in sequence order. */
  private static List<Integer> activeOnBarrier(TrafficSignalProgrammedPhasePlan plan, int ring,
      int barrier) {
    List<Integer> out = new ArrayList<>();
    for (int n : plan.getRingSequence(ring)) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      if (p != null && p.isActive() && p.getBarrier() == barrier) {
        out.add(n);
      }
    }
    return out;
  }

  /**
   * A phase's split as configured, or — for a split of 0 — an even share of the cycle among the
   * active phases of its ring. Never below one tick, so every active phase keeps a window.
   */
  private static long splitOrShare(TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalCoordinationPlan co, int ring, int phaseNumber, long cycle) {
    long s = co.getSplit(phaseNumber);
    if (s > 0L) {
      return s;
    }
    int active = 0;
    for (int n : plan.getRingSequence(ring)) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      if (p != null && p.isActive()) {
        active++;
      }
    }
    return Math.max(1L, cycle / Math.max(1, active));
  }

  /**
   * Non-fatal check that the two rings' splits meet at every barrier. The rings cross a barrier
   * together, so the engine gives each barrier the <em>longer</em> ring's total and stretches the
   * shorter ring's phases to fill it: the cycle still holds, but the shorter ring's phases run
   * longer than typed, and the whole cycle is rescaled if the barriers then overrun it. A real
   * controller refuses such a plan; this is an advisory for the same reason
   * {@link #findSplitShortfall} is. Barriers that only one ring has phases on are not compared —
   * that ring alone sets the barrier's length and the other waits at it (a T intersection).
   *
   * @return a description of the first mismatched barrier, or {@code null} if every barrier's
   *     totals agree
   */
  String findBarrierMisalignment(TrafficSignalProgrammedPhasePlan plan) {
    TrafficSignalCoordinationPlan co = plan.getCoordination(Math.max(0, activeCoordinationSlot));
    BarrierSplits bs = barrierSplits(plan, co, Math.max(1L, co.getCycleLength()));
    for (int b = 0; b < bs.order.length; b++) {
      long t1 = bs.total[0][b];
      long t2 = bs.total[1][b];
      if (activeOnBarrier(plan, 1, bs.order[b]).isEmpty()
          || activeOnBarrier(plan, 2, bs.order[b]).isEmpty() || t1 == t2) {
        continue;
      }
      char barrier = (char) ('A' + bs.order[b]);
      return "the rings' splits do not meet at barrier " + barrier + ": ring 1's phases there "
          + "total " + t1 + " ticks and ring 2's " + t2 + ". The rings cross a barrier "
          + "together, so the barrier runs the longer " + Math.max(t1, t2) + " and the shorter "
          + "ring's phases are stretched " + Math.abs(t1 - t2) + " ticks past what was typed. "
          + "Give both rings the same split total on each barrier.";
    }
    return null;
  }

  /**
   * Non-fatal check for a coordinated split that is too short to contain what the phase actually
   * needs: its minimum green (or its full walk + pedestrian clearance when it services a walk every
   * cycle) plus the yellow and red clearance that must finish inside the same split.
   *
   * <p>When a split is over-subscribed the engine still does the best it can — it clears as early
   * as the yield point allows — but the next phase necessarily starts late by the shortfall on
   * <em>every</em> cycle, and no amount of offset correction can recover time the plan never
   * allocated. Only phases that service a pedestrian every cycle (ped recall, pedestrian recall
   * mode, or rest-in-walk) are charged for the walk; an on-demand ped is intermittent and would
   * produce false positives.</p>
   *
   * <p>Deliberately an advisory and not part of {@link TrafficSignalProgrammedPhasePlan#validate}:
   * over-subscribed splits are a tuning problem, and faulting the intersection into flash over one
   * would be far more disruptive than the few ticks of lateness it causes.</p>
   *
   * @return a description of the first shortfall found, or {@code null} if every split fits
   */
  String findSplitShortfall(TrafficSignalProgrammedPhasePlan plan) {
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      if (p == null || !p.isActive()) {
        continue;
      }
      long windowLen = windowEnd[n] - windowStart[n];
      if (windowLen <= 0L) {
        continue;
      }
      boolean servesPedEveryCycle = p.isPedRecall() || p.isRestInWalk()
          || p.getRecallMode() == TrafficSignalRecallMode.PEDESTRIAN;
      long green = servesPedEveryCycle
          ? Math.max(p.getMinGreen(), p.getWalk() + p.getPedClear())
          : p.getMinGreen();
      long needed = green + p.getYellow() + p.getRedClear();
      if (needed > windowLen) {
        return "phase " + n + "'s split is " + windowLen + " ticks but the phase wants up to "
            + needed + " (green " + green + " + yellow " + p.getYellow() + " + red clear "
            + p.getRedClear() + "). The split cannot contain the phase, so the next phase starts "
            + "late every cycle and offset correction cannot recover the difference. Lengthen the "
            + "split or the cycle.";
      }
    }
    return null;
  }

  /**
   * Whether the local cycle has reached a phase's <em>yield point</em>: the end of its permissive
   * window, less its own clearance ({@code yellow + redClear}). A split is the time the movement
   * owns end to end — green <em>and</em> its clearance, as on a real controller — so a phase must
   * begin terminating early enough that the next phase's green starts at its own window start.
   * Force-offing at the window end instead let every phase overrun by its clearance, which put the
   * coordinated phase {@code yellow + redClear} ticks late every single cycle with nothing to ever
   * take the lateness back.
   *
   * <p>Measured as a position within the window rather than against {@link #windowEnd} directly:
   * the last window in each ring ends exactly at the cycle wrap, where a plain {@code localCycle >=
   * windowEnd} comparison can never be true. Positions wrap, so a phase being served <em>outside</em>
   * its window (served late on a sticky accepted call) lands past the yield point and is forced off
   * at once — the previous behavior of the {@code !windowOpen} test. The window is taken from
   * {@link #effectiveWindowStart}, so the one way a phase legitimately runs <em>before</em> its own
   * window — the phases ahead of it in its ring were skipped, so the ring reached it early — counts
   * as inside the window rather than wrapping round to "past". The yield point itself is unchanged
   * either way: it is always {@code windowEnd - clearance}.</p>
   *
   * <p>Never returns a yield point below zero, and callers must keep it behind the
   * {@code terminate && minMet} guard so a split shorter than its own clearance still gets its
   * minimum green.</p>
   */
  private boolean pastYieldPoint(TrafficSignalProgrammedPhasePlan plan, int phaseNumber,
      long clearance) {
    long start = effectiveWindowStart(plan, phaseNumber);
    long windowLen = windowEnd[phaseNumber] - start;
    if (windowLen <= 0L) {
      return true;
    }
    return windowPos(start) >= Math.max(0L, windowLen - Math.max(0L, clearance));
  }

  /**
   * The cycle position a phase is actually served from: its own {@link #windowStart}, walked back
   * over each phase immediately ahead of it in its own ring on the same barrier that has NOT been
   * served in that phase's current window instance.
   *
   * <p>Windows are laid out from the splits of every <em>active</em> phase, but a ring does not
   * wait out the split of a phase nothing called — it skips the slot and starts the next phase at
   * the barrier. Without this the phase that took that time was "outside its window" for the whole
   * of it: {@link #pastYieldPoint} wrapped its position round to past the yield point and forced it
   * off the instant it met minimum green, and {@link #acceptanceOpen} would not even register its
   * call, leaving the ring dark until the window caught up. A skipped phase's time belongs to the
   * phase that took it.</p>
   *
   * <p>Walking back stops at the first phase that WAS served in its current window instance
   * (including one being served right now), so this can only ever reach back to the barrier, and a
   * phase whose predecessor really did run keeps its own window start.</p>
   */
  private long effectiveWindowStart(TrafficSignalProgrammedPhasePlan plan, int phaseNumber) {
    TrafficSignalProgrammedPhase phase = plan == null ? null : plan.getPhase(phaseNumber);
    if (!coordinated || phase == null) {
      return windowStart[phaseNumber];
    }
    int ring = ringOf(plan, phaseNumber);
    if (ring == 0) {
      return windowStart[phaseNumber];
    }
    List<Integer> onBarrier = activeOnBarrier(plan, ring, phase.getBarrier());
    long start = windowStart[phaseNumber];
    for (int i = onBarrier.indexOf(phaseNumber) - 1; i >= 0; i--) {
      int earlier = onBarrier.get(i);
      if (servedWindow[earlier] == windowInstance(earlier)) {
        break; // that phase ran (or is running) in this window: its time was not donated
      }
      start = windowStart[earlier];
    }
    return start;
  }

  /**
   * Whether a phase's permissive window is open for <em>accepting</em> a new call. A call is only
   * worth accepting while the phase could still be served this cycle, so acceptance closes at the
   * same yield point that force-off uses — not at the window end.
   *
   * <p>Gating acceptance on the window end instead re-granted acceptance to a phase during its own
   * clearance (its yellow and red-clear still fall inside its window), handing it a standing call
   * into the next cycle: the coordinated phase was then terminated after bare min green and the
   * side street ran a runt green outside its window.</p>
   *
   * <p>Always leaves at least a one-tick acceptance opportunity, so a split configured shorter than
   * its own clearance still gets served rather than starving. Minimum green is guaranteed
   * separately by the {@code terminate && minMet} guard.</p>
   */
  private boolean acceptanceOpen(TrafficSignalProgrammedPhasePlan plan, int phaseNumber,
      long clearance) {
    long start = effectiveWindowStart(plan, phaseNumber);
    long windowLen = windowEnd[phaseNumber] - start;
    if (windowLen <= 0L) {
      return false;
    }
    return windowPos(start) < Math.max(1L, windowLen - Math.max(0L, clearance));
  }

  /**
   * Whether the coordinated phase has reached the yield point at which it must start terminating.
   *
   * <p>Deliberately <em>not</em> {@link #pastYieldPoint}: that treats any position outside the
   * window as "past", which is right for a non-coordinated force-off (a phase outside its window
   * must get off) but exactly wrong for the coordinated phase, which is the one that should be
   * <em>filling</em> time whenever the cycle is out of alignment. A coordinated phase found outside
   * its own window is the signal running off its offset, and the recovery is to dwell — hold green
   * (past max green, which coordinated phases are already exempt from) until the cycle comes back
   * around to its yield point, then resume normal service from there.</p>
   *
   * <p>This is add-only correction: the coordinated phase absorbs the whole error by running long,
   * and no side-street split or pedestrian interval is ever shortened to catch up.</p>
   *
   * <p>The test is whether <em>this green</em> has run through the yield point, not where the local
   * cycle happens to sit right now. An instantaneous "am I past the yield point" comparison cannot
   * tell apart the two cases that matter: a green that started on time and has just crossed its
   * yield point (terminate now — it may have crossed it between two sparse ticks) and a green that
   * only just started well beyond it because the cycle is out of alignment (dwell until the yield
   * point comes round again). Measuring from the start of the green separates them exactly.</p>
   *
   * @param greenElapsed ticks this phase has been green
   */
  private boolean coordYieldReached(int phaseNumber, long clearance, long greenElapsed) {
    long windowLen = windowEnd[phaseNumber] - windowStart[phaseNumber];
    if (windowLen <= 0L) {
      return false;
    }
    long yieldPos = (windowStart[phaseNumber]
        + Math.max(0L, windowLen - Math.max(0L, clearance))) % cycleTicks;
    long startPos = ((localCycle - greenElapsed) % cycleTicks + cycleTicks) % cycleTicks;
    long arcToYield = ((yieldPos - startPos) % cycleTicks + cycleTicks) % cycleTicks;
    return greenElapsed >= arcToYield;
  }

  /**
   * For a coordinated phase currently outside its own window: the waiting conflicting phase whose
   * permissive window opens within {@code clearance} ticks — the point the coordinated phase has
   * to start clearing for that phase to go green at its window start — or {@code 0} if none.
   *
   * <p>Only a window still <em>ahead</em> is looked for; a window already open is left to the
   * offset-recovery dwell. Serving into an open window on a gapped-out recall phase would re-serve
   * it on every gap-out, thrashing the coordinated phase. A yield that could not complete in time
   * (min green or a pedestrian clearance still running) is carried by
   * {@link RingRuntime#coordYieldFor} instead. Inside its own window the coordinated phase never
   * yields early. Demand is judged raw, like the coordinated yield point, because a window that
   * has not opened cannot accept it.</p>
   */
  private int conflictingWindowOpening(TrafficSignalProgrammedPhase coord, int ringNum,
      TrafficSignalProgrammedPhasePlan plan, long clearance) {
    int n = coord.getPhaseNumber();
    long ownLen = windowEnd[n] - windowStart[n];
    if (!coordinated || ownLen <= 0L || windowPos(windowStart[n]) < ownLen) {
      return 0;
    }
    int best = 0;
    long bestArc = Long.MAX_VALUE;
    for (int m = 1; m <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; m++) {
      TrafficSignalProgrammedPhase p = plan.getPhase(m);
      if (m == n || p == null || !p.isActive() || coordPhase[m] || !tickRawCalled[m]
          || m == ring1.activePhase || m == ring2.activePhase
          || windowEnd[m] - windowStart[m] <= 0L || !conflicts(coord, ringNum, plan, m)) {
        continue;
      }
      long arc = ((windowStart[m] - localCycle) % cycleTicks + cycleTicks) % cycleTicks;
      if (arc > 0L && arc <= Math.max(0L, clearance) && arc < bestArc) {
        best = m;
        bestArc = arc;
      }
    }
    return best;
  }

  /**
   * Which occurrence of a phase's permissive window the cycle is in: the number of whole cycles
   * since that window last opened, counted on the unwrapped cycle time. Changes exactly when the
   * window opens, so a window that spans the cycle wrap is still one instance.
   */
  private long windowInstance(int phaseNumber) {
    return Math.floorDiv(cycleTime - windowStart[phaseNumber], Math.max(1L, cycleTicks));
  }

  /**
   * The local cycle's position within a window beginning at {@code start}. Positions wrap, so a
   * local cycle outside the window yields a position at or beyond the window's length.
   */
  private long windowPos(long start) {
    return ((localCycle - start) % cycleTicks + cycleTicks) % cycleTicks;
  }

  // endregion

  // region: Demand

  /**
   * Advances the locking-detector-memory latches (ASC/3 vehicle call memory). For each active
   * LOCK phase: while it is being served green the latch is discharged; otherwise a vehicle in
   * its zone sets the latch, which then persists after the vehicle leaves (pulse-detector
   * behavior — the call is remembered until served). Non-LOCK phases have their latch kept clear
   * so toggling the option off also drops any pending latched call.
   */
  private void updateLockedCalls(TrafficSignalProgrammedPhasePlan plan) {
    for (TrafficSignalProgrammedPhase p : plan.getPhases()) {
      int n = p.getPhaseNumber();
      if (n < 1 || n >= lockedCalls.length) {
        continue;
      }
      if (!p.isActive() || !p.isLockCall()) {
        lockedCalls[n] = false;
        continue;
      }
      boolean servedGreen = (ring1.activePhase == n && ring1.interval == VehInterval.GREEN)
          || (ring2.activePhase == n && ring2.interval == VehInterval.GREEN);
      if (servedGreen) {
        lockedCalls[n] = false;
      } else if (vehicleCount(p) > 0) {
        lockedCalls[n] = true;
      }
    }
  }

  /** Whether a phase is calling for service (vehicle, pedestrian, or recall). */
  private boolean isCalled(TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalProgrammedPhase phase) {
    if (phase == null || !phase.isActive()
        || phase.getCircuitIndex() >= tickCircuits.getCircuitCount()) {
      if (phase != null && phase.getPhaseNumber() >= 1
          && phase.getPhaseNumber() < committedCalls.length) {
        committedCalls[phase.getPhaseNumber()] = false; // cannot be served: drop the commitment
      }
      return false;
    }
    int n = phase.getPhaseNumber();
    // Coordinated phases are served every cycle regardless of their own detection, so the
    // background cycle holds even under continuous side-street demand.
    if (coordinated && coordPhase[n]) {
      return true;
    }
    // A committed call (phase next) is served regardless of what its detection does now, and
    // was accepted when it was committed, so it is not re-gated on the window either.
    if (committedCalls[n]) {
      return true;
    }
    boolean demand = hasDemand(plan, phase);
    if (!coordinated) {
      return demand;
    }
    // In coordinated operation, a non-coordinated phase's call registers only within its
    // permissive window; outside it the time belongs to the coordinated phase, which rests in
    // green. But once accepted inside the window, the call sticks (see windowAccepted) until the
    // phase is served or the demand itself drops — the mains' clearance may legitimately outlast
    // the window, and the accepted phase must still get its (min) green on the far side of it.
    if (!demand) {
      windowAccepted[n] = false;
      return false;
    }
    if (acceptanceOpen(plan, n, phase.getYellow() + phase.getRedClear())
        && servedWindow[n] != windowInstance(n)) {
      windowAccepted[n] = true;
    }
    return windowAccepted[n];
  }

  /**
   * Places this tick's soft-recall calls (ASC/3 {@code SF RCALL}, NTCIP "soft recall"). A
   * {@code SOFT} phase is called whenever there is no serviceable conflicting call: no phase that
   * is <em>not</em> currently being served has demand of its own (a hard recall, a vehicle, a
   * pedestrian, a latched call or overlap detection) that conflicts with it. That is what makes it
   * the place the controller returns to and rests: once the side street's traffic clears, the
   * soft call is the conflicting demand that lets the side street gap out, and the rings cross
   * back to the soft phases and stay there (a soft call cannot conflict with the phase resting on
   * it). Vehicles extending a green that is already being served are extensions, not a waiting
   * call, so a side street under continuous traffic still maxes out against the soft call rather
   * than holding green for good.
   *
   * <p>Unlike {@code MIN}, a soft recall never forces a cycle: while an unserved phase that
   * conflicts with it has real demand, the soft call is withheld, so continuous conflicting demand
   * can starve a soft phase indefinitely. That is the intended difference — use {@code MIN} for a
   * phase that must be served every cycle. Two <em>conflicting</em> soft phases each supply the
   * other's conflicting call, so they alternate like a pair of {@code MIN} recalls; put soft
   * recall on a compatible pair (the mains, 2 and 6).</p>
   *
   * <p>Soft calls are judged against real demand only, never against other soft calls (which
   * would be circular), and are recomputed from scratch every tick. A soft call that terminates
   * a phase is committed like any other call ({@link #commitConflictingCalls}), so a vehicle
   * arriving on the clearing phase during its own clearance cannot withdraw it and send that
   * phase straight back to green.</p>
   */
  private void placeSoftRecallCalls(TrafficSignalProgrammedPhasePlan plan) {
    boolean[] real = new boolean[PHASE_SLOTS];
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      softCalled[n] = false; // cleared first so hasDemand below sees no soft call
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      real[n] = p != null && p.isActive()
          && p.getCircuitIndex() < tickCircuits.getCircuitCount() && hasDemand(plan, p);
    }
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      if (p == null || !p.isActive() || p.getRecallMode() != TrafficSignalRecallMode.SOFT) {
        continue;
      }
      int ring = ringOf(plan, n);
      if (ring == 0) {
        continue; // not in either sequence: could never be served, so never call it
      }
      boolean blocked = false;
      for (int m = 1; m <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; m++) {
        if (m == n || !real[m] || m == ring1.activePhase || m == ring2.activePhase) {
          continue;
        }
        if (conflicts(p, ring, plan, m)) {
          blocked = true;
          break;
        }
      }
      softCalled[n] = !blocked;
    }
  }

  /**
   * NEMA "phase next": latches every called, unserved phase that conflicts with {@code phase} —
   * the demand this termination is for — so it stays called until served even if its detection
   * drops during the clearance. A real controller fixes the next phase at the start of yellow
   * and serves it (for at least its minimum green) regardless. Discharged by {@link #startGreen}.
   */
  private void commitConflictingCalls(TrafficSignalProgrammedPhase phase, int ringNum,
      TrafficSignalProgrammedPhasePlan plan, boolean[] called) {
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      if (!called[n] || n == ring1.activePhase || n == ring2.activePhase
          || n == phase.getPhaseNumber()) {
        continue;
      }
      if (conflicts(phase, ringNum, plan, n)) {
        committedCalls[n] = true;
      }
    }
  }

  /**
   * Whether the phase has demand of its own this tick, ignoring coordination gating: a recall, a
   * latched (LOCK) call, a vehicle in its zone, a pedestrian request, or overlap detection
   * assigned to it ({@link #overlapDemand}).
   */
  private boolean hasDemand(TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalProgrammedPhase phase) {
    TrafficSignalRecallMode recall = phase.getRecallMode();
    if (recall == TrafficSignalRecallMode.MINIMUM || recall == TrafficSignalRecallMode.MAXIMUM
        || recall == TrafficSignalRecallMode.PEDESTRIAN) {
      return true;
    }
    // A soft recall is a conditional call (placed only when nothing conflicting is waiting), so
    // it is decided once per tick in placeSoftRecallCalls rather than unconditionally here.
    if (recall == TrafficSignalRecallMode.SOFT && softCalled[phase.getPhaseNumber()]) {
      return true;
    }
    // Locking detector memory: a latched call counts as demand until the phase is served, even
    // after the vehicle has left the zone. (The coordination gating in isCalled still applies, so
    // a latched call placed outside the phase's permissive window waits for it to open — it
    // persists, it doesn't jump.)
    if (phase.isLockCall() && lockedCalls[phase.getPhaseNumber()]) {
      return true;
    }
    if (vehicleCount(phase) > 0) {
      return true;
    }
    if (pedRequestPresent(phase)) {
      return true;
    }
    return overlapDemand(plan, phase.getPhaseNumber());
  }

  /**
   * Demand from overlap detection (detector-to-phase assignment): an active overlap with this
   * call phase counts as phase demand while vehicles are present in its output circuit's
   * output-movement sensor zone. Overlaps are otherwise pure outputs, so without this a movement
   * served only by an overlap (e.g. a right-turn pocket) could wait forever with nothing calling
   * the phases that light it.
   */
  private boolean overlapDemand(TrafficSignalProgrammedPhasePlan plan, int phaseNumber) {
    for (TrafficSignalProgrammedOverlap ov : plan.getVehicleOverlaps()) {
      if (!ov.isActive() || ov.getCallPhase() != phaseNumber) {
        continue;
      }
      // A pedestrian-output overlap is called by its output circuit's button requests — there is
      // no vehicle sensor zone for PED, so zoneCount would silently report 0 forever.
      boolean present = ov.getOutputMovement() == TrafficSignalPhaseMovement.PED
          ? demand != null && demand.pedestrianRequest(ov.getOutputCircuitIndex())
          : zoneCount(ov.getOutputCircuitIndex(), ov.getOutputMovement()) > 0;
      if (present) {
        return true;
      }
    }
    return false;
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

  /**
   * Whether any called-but-unserved phase on the SAME barrier as {@code phase} conflicts with it
   * (same ring, or an FYA permissive pair). This is the demand that same-barrier operation must
   * yield to; cross-barrier demand is deliberately excluded — a dual-entry companion rides
   * through it and clears with its companion rather than being stripped off early (under
   * coordination the main-street phases are always called, so including cross-barrier demand
   * would disable dual entry on the side street entirely).
   */
  private boolean withinBarrierConflict(TrafficSignalProgrammedPhase phase, int ringNum,
      TrafficSignalProgrammedPhasePlan plan, boolean[] called) {
    if (phase == null) {
      return true;
    }
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      if (!called[n] || n == ring1.activePhase || n == ring2.activePhase
          || n == phase.getPhaseNumber()) {
        continue;
      }
      TrafficSignalProgrammedPhase other = plan.getPhase(n);
      if (other == null || other.getBarrier() != phase.getBarrier()) {
        continue;
      }
      if (conflicts(phase, ringNum, plan, n)) {
        return true;
      }
    }
    return false;
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
        beginPreempt(calledIdx, plan, now);
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
      beginPreempt(calledIdx, plan, now);
      return;
    }

    long elapsed = now - preemptStageStart;
    switch (preemptStage) {
      case ENTER:
        if (elapsed >= PREEMPT_CLEARANCE_TICKS) {
          preemptStage = active.getTrackClearPhases().length > 0
              ? PreemptStage.TRACK_CLEAR : PreemptStage.DWELL;
          preemptStageStart = now;
        }
        break;
      case TRACK_CLEAR:
        if (elapsed >= PREEMPT_TRACK_CLEAR_TICKS) {
          // Track-clear movements that do not continue into the dwell need a clearance first;
          // going green -> green across them displayed conflicting greens together.
          preemptStage = trackClearOnlyPhases(active).isEmpty()
              ? PreemptStage.DWELL : PreemptStage.TRACK_EXIT;
          preemptStageStart = now;
        }
        break;
      case TRACK_EXIT:
        if (elapsed >= PREEMPT_CLEARANCE_TICKS) {
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
        // The dwell movements get their own yellow and red before normal service resumes. An
        // all-red shorter than a yellow left the output clearance still painting the dwell heads
        // yellow after the resumed phase had gone green.
        if (elapsed >= PREEMPT_CLEARANCE_TICKS) {
          preemptStage = PreemptStage.NONE;
          activePreemptIndex = -1;
        }
        break;
    }
  }

  private void beginPreempt(int index, TrafficSignalProgrammedPhasePlan plan, long now) {
    activePreemptIndex = index;
    preemptStage = PreemptStage.ENTER;
    preemptStageStart = now;
    // Capture what is being served so entry can clear it with a proper yellow — except a green
    // that the first preempt stage serves anyway, which simply continues (no pointless
    // green -> yellow -> red -> green on the very movement the preempt wants).
    TrafficSignalPreempt pe = plan.getPreempts().get(index);
    int[] first = pe.getTrackClearPhases().length > 0
        ? pe.getTrackClearPhases() : pe.getDwellPhases();
    preemptClearPhases.clear();
    preemptContinuePhases.clear();
    preemptPedClearPhases.clear();
    for (RingRuntime ring : new RingRuntime[] {ring1, ring2}) {
      if (ring.activePhase == 0 || ring.interval == VehInterval.RED) {
        continue; // already red (or idle): nothing to clear
      }
      ServedMovement shown = describe(ring, plan, now);
      if (shown != null && (shown.pedestrian == PedInterval.WALK
          || shown.pedestrian == PedInterval.FDW)) {
        preemptPedClearPhases.add(ring.activePhase);
      }
      boolean continues = shown != null && shown.vehicle == VehInterval.GREEN
          && contains(first, ring.activePhase);
      (continues ? preemptContinuePhases : preemptClearPhases).add(ring.activePhase);
    }
  }

  private static boolean contains(int[] phases, int phaseNumber) {
    for (int n : phases) {
      if (n == phaseNumber) {
        return true;
      }
    }
    return false;
  }

  /** The track-clear phases that are not also dwell phases (the ones track-clear exit clears). */
  private static List<Integer> trackClearOnlyPhases(TrafficSignalPreempt preempt) {
    List<Integer> out = new ArrayList<>();
    for (int n : preempt.getTrackClearPhases()) {
      if (!contains(preempt.getDwellPhases(), n)) {
        out.add(n);
      }
    }
    return out;
  }

  /** The track-clear phases that continue into the dwell (kept green across the track-clear exit). */
  private static List<Integer> trackClearContinuingPhases(TrafficSignalPreempt preempt) {
    List<Integer> out = new ArrayList<>();
    for (int n : preempt.getTrackClearPhases()) {
      if (contains(preempt.getDwellPhases(), n)) {
        out.add(n);
      }
    }
    return out;
  }

  private boolean isPreemptCalled(TrafficSignalPreempt preempt) {
    return zoneCount(preempt.getTriggerCircuitIndex(), preempt.getTriggerMovement()) > 0;
  }

  private TrafficSignalPhase buildPreemptPhase(World world, TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits, TrafficSignalControllerOverlaps overlaps, long now) {
    TrafficSignalPreempt active = plan.getPreempts().get(activePreemptIndex);
    long elapsed = now - preemptStageStart;
    VehInterval clearing = elapsed < PREEMPT_YELLOW_TICKS ? VehInterval.YELLOW : VehInterval.RED;
    switch (preemptStage) {
      case ENTER: {
        // Whatever was green clears yellow -> red while any movement the first stage serves
        // continues green. Ped heads that were in WALK/FDW flash don't-walk through the entry
        // (a truncated clearance — MUTCD 4D.27 permits shortening it on entry to preemption —
        // rather than snapping straight to don't-walk).
        List<ServedMovement> served = new ArrayList<>();
        for (int n : preemptContinuePhases) {
          served.add(new ServedMovement(n, VehInterval.GREEN,
              preemptPedClearPhases.contains(n) ? PedInterval.FDW : PedInterval.NONE));
        }
        for (int n : preemptClearPhases) {
          served.add(new ServedMovement(n, clearing,
              preemptPedClearPhases.contains(n) ? PedInterval.FDW : PedInterval.NONE));
        }
        return AdvancedPhaseBuilder.buildForMovements(world, plan, circuits, overlaps, served,
            active);
      }
      case TRACK_CLEAR:
        return AdvancedPhaseBuilder.buildForPhases(world, plan, circuits, overlaps,
            toPhaseList(active.getTrackClearPhases()), VehInterval.GREEN, active);
      case TRACK_EXIT: {
        List<ServedMovement> served = new ArrayList<>();
        for (int n : trackClearContinuingPhases(active)) {
          served.add(new ServedMovement(n, VehInterval.GREEN, PedInterval.NONE));
        }
        for (int n : trackClearOnlyPhases(active)) {
          served.add(new ServedMovement(n, clearing, PedInterval.NONE));
        }
        return AdvancedPhaseBuilder.buildForMovements(world, plan, circuits, overlaps, served,
            active);
      }
      case DWELL:
        return AdvancedPhaseBuilder.buildForPhases(world, plan, circuits, overlaps,
            toPhaseList(active.getDwellPhases()), VehInterval.GREEN, active);
      case EXIT:
      default:
        return AdvancedPhaseBuilder.buildForPhases(world, plan, circuits, overlaps,
            toPhaseList(active.getDwellPhases()), clearing, active);
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
