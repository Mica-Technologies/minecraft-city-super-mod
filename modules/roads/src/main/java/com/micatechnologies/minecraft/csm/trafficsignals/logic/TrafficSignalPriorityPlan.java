package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Transit signal priority for {@code ADVANCED} mode: the two moves that nudge a running cycle to
 * favour a bus, without dropping out of coordination.
 *
 * <p>This is not the {@link TrafficSignalPreemptType#PRIORITY} preempt, and the difference is the
 * whole point. A preempt is the heavy hammer — terminate everything conflicting, serve a
 * track-clear, dwell, exit — and a corridor that does that for every bus has no coordination
 * left. Real TSP does neither of those things:</p>
 *
 * <ul>
 *   <li><b>Green extension</b> — a call arriving while the transit phase is already green holds
 *       that green a few seconds longer, so a bus that would just miss it gets through.</li>
 *   <li><b>Early return</b> — a call arriving while the transit phase is red shortens the
 *       conflicting phases toward their minimums, bringing the transit green back sooner.</li>
 * </ul>
 *
 * <p>Both are bounded, and neither can shorten anything below its minimum green or cut a
 * pedestrian clearance: the engine already refuses to terminate a phase until {@code minMet} and
 * {@code pedDone}, and priority only ever moves the <em>maximum</em>. That is what makes this
 * safe to bolt onto the ring engine rather than a change to how it clears.</p>
 *
 * <p>A call comes from the trigger circuit's sensor zone, or from its preempt detectors seeing
 * a transit emitter ({@link #isTriggerOnDetectors()}), which is how a bus asks without a sensor
 * that would also count every car.</p>
 *
 * <p>A <b>queue jump</b> goes with a granted call when the transit phase's circuit has queue jump
 * heads linked: the transit phase starts its green with those heads showing the white transit
 * bar while its general heads are held red for {@link #getQueueJump()}, so the bus leaves ahead
 * of the queue beside it. It runs as a delayed green does, so the green clocks start after it and
 * the phase still gets its whole minimum green.</p>
 *
 * <p>Priority is also rate limited. Without a limit a frequent route holds a corridor open
 * permanently, which is the failure every real deployment guards against.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class TrafficSignalPriorityPlan {

  /** Default extension: 10 seconds, about one bus-length of hesitation at an approach. */
  public static final long DEFAULT_MAX_EXTENSION = 200L;
  /** Default early return: 10 seconds clawed back off the conflicting phases. */
  public static final long DEFAULT_MAX_EARLY_RETURN = 200L;
  /** Default rate limit: at most one grant every other cycle. */
  public static final int DEFAULT_MIN_CYCLES_BETWEEN_GRANTS = 2;
  /** Default queue jump: 4 seconds of bus-only green, about what a bus needs to clear the stop
   * line and pull ahead. */
  public static final long DEFAULT_QUEUE_JUMP = 80L;
  /** The longest queue jump, 10 seconds: past that the queue beside the bus is simply held. */
  public static final long MAX_QUEUE_JUMP = 200L;

  private static final String K_ENABLED = "en";
  private static final String K_CIRCUIT = "tc";
  private static final String K_MOVEMENT = "tm";
  private static final String K_PHASE = "ph";
  private static final String K_EXTENSION = "ex";
  private static final String K_EARLY_RETURN = "er";
  private static final String K_MIN_CYCLES = "mc";
  private static final String K_DETECTORS = "td";
  private static final String K_QUEUE_JUMP = "qj";

  private boolean enabled = false;
  /** Circuit index whose sensor zone calls priority, or -1 if unassigned. */
  private int triggerCircuitIndex = -1;
  private TrafficSignalPhaseMovement triggerMovement = TrafficSignalPhaseMovement.THROUGH;
  /** The phase that serves transit; 0 if unassigned. */
  private int transitPhase = 0;
  private long maxExtension = DEFAULT_MAX_EXTENSION;
  private long maxEarlyReturn = DEFAULT_MAX_EARLY_RETURN;
  private int minCyclesBetweenGrants = DEFAULT_MIN_CYCLES_BETWEEN_GRANTS;
  /** Whether the trigger circuit's preempt detectors call priority instead of its zone. */
  private boolean triggerOnDetectors = false;
  private long queueJump = DEFAULT_QUEUE_JUMP;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getTriggerCircuitIndex() {
    return triggerCircuitIndex;
  }

  public void setTriggerCircuitIndex(int triggerCircuitIndex) {
    this.triggerCircuitIndex = triggerCircuitIndex;
  }

  public TrafficSignalPhaseMovement getTriggerMovement() {
    return triggerMovement;
  }

  public void setTriggerMovement(TrafficSignalPhaseMovement triggerMovement) {
    this.triggerMovement =
        triggerMovement == null ? TrafficSignalPhaseMovement.THROUGH : triggerMovement;
  }

  public boolean isTriggerOnDetectors() {
    return triggerOnDetectors;
  }

  public void setTriggerOnDetectors(boolean triggerOnDetectors) {
    this.triggerOnDetectors = triggerOnDetectors;
  }

  /** @return the queue jump's length in ticks; 0 runs none */
  public long getQueueJump() {
    return queueJump;
  }

  public void setQueueJump(long queueJump) {
    this.queueJump = Math.max(0L, Math.min(MAX_QUEUE_JUMP, queueJump));
  }

  public int getTransitPhase() {
    return transitPhase;
  }

  public void setTransitPhase(int transitPhase) {
    this.transitPhase = clamp(transitPhase, 0, TrafficSignalProgrammedPhasePlan.PHASE_COUNT);
  }

  public long getMaxExtension() {
    return maxExtension;
  }

  public void setMaxExtension(long maxExtension) {
    this.maxExtension = Math.max(0L, maxExtension);
  }

  public long getMaxEarlyReturn() {
    return maxEarlyReturn;
  }

  public void setMaxEarlyReturn(long maxEarlyReturn) {
    this.maxEarlyReturn = Math.max(0L, maxEarlyReturn);
  }

  public int getMinCyclesBetweenGrants() {
    return minCyclesBetweenGrants;
  }

  public void setMinCyclesBetweenGrants(int minCyclesBetweenGrants) {
    this.minCyclesBetweenGrants = Math.max(0, minCyclesBetweenGrants);
  }

  /**
   * Whether priority is configured well enough to do anything.
   *
   * <p>Enabled on its own is not enough: without a phase to favour and a circuit to hear from,
   * a granted priority would have nothing to act on, and the engine should not spend a tick
   * finding that out.</p>
   *
   * @return {@code true} if priority should be evaluated
   */
  public boolean isRunnable() {
    return enabled && transitPhase > 0 && triggerCircuitIndex >= 0;
  }

  /**
   * The maximum green a phase should be given while priority is granted.
   *
   * <p>This is the whole of the behaviour, and it is a function so it can be tested directly.
   * Extension raises the transit phase's ceiling; early return lowers the ceiling of everything
   * that conflicts with it. Neither can go below the phase's own minimum green, which is what
   * keeps priority from ever shortening a phase past what is safe — the engine will not
   * terminate before {@code minMet} regardless, so a lower cap simply has no effect.</p>
   *
   * @param baseMaxGreen  the max green the phase would otherwise get
   * @param minGreen      the phase's effective minimum green
   * @param isTransit     whether this is the phase priority is trying to serve
   * @param conflicts     whether this phase conflicts with the transit phase
   * @param extensionLeft how much extension budget remains
   *
   * @return the max green to use this tick
   */
  public long adjustMaxGreen(long baseMaxGreen, long minGreen, boolean isTransit,
      boolean conflicts, long extensionLeft) {
    if (isTransit) {
      return baseMaxGreen + Math.max(0L, Math.min(extensionLeft, maxExtension));
    }
    if (!conflicts) {
      return baseMaxGreen;
    }
    return Math.max(minGreen, baseMaxGreen - maxEarlyReturn);
  }

  /**
   * Whether a grant is allowed this cycle, given when the last one was.
   *
   * @param currentCycle   the cycle number now running, or negative when not coordinated
   * @param lastGrantCycle the cycle a grant was last made in, or negative if never
   *
   * @return {@code true} if priority may be granted
   */
  public boolean mayGrant(long currentCycle, long lastGrantCycle) {
    if (minCyclesBetweenGrants <= 0 || lastGrantCycle < 0L || currentCycle < 0L) {
      // Free operation has no cycles to count, so the limit cannot apply there — the extension
      // cap is what bounds it instead.
      return true;
    }
    return currentCycle - lastGrantCycle >= minCyclesBetweenGrants;
  }

  private static int clamp(int value, int min, int max) {
    return value < min ? min : (value > max ? max : value);
  }

  public NBTTagCompound toNBT() {
    NBTTagCompound c = new NBTTagCompound();
    c.setBoolean(K_ENABLED, enabled);
    c.setInteger(K_CIRCUIT, triggerCircuitIndex);
    c.setInteger(K_MOVEMENT, triggerMovement.ordinal());
    c.setInteger(K_PHASE, transitPhase);
    c.setLong(K_EXTENSION, maxExtension);
    c.setLong(K_EARLY_RETURN, maxEarlyReturn);
    c.setInteger(K_MIN_CYCLES, minCyclesBetweenGrants);
    c.setBoolean(K_DETECTORS, triggerOnDetectors);
    c.setLong(K_QUEUE_JUMP, queueJump);
    return c;
  }

  public static TrafficSignalPriorityPlan fromNBT(NBTTagCompound c) {
    TrafficSignalPriorityPlan plan = new TrafficSignalPriorityPlan();
    if (c == null) {
      return plan;
    }
    plan.enabled = c.getBoolean(K_ENABLED);
    plan.triggerCircuitIndex = c.hasKey(K_CIRCUIT) ? c.getInteger(K_CIRCUIT) : -1;
    if (c.hasKey(K_MOVEMENT)) {
      TrafficSignalPhaseMovement[] movements = TrafficSignalPhaseMovement.values();
      int ordinal = c.getInteger(K_MOVEMENT);
      plan.triggerMovement = ordinal >= 0 && ordinal < movements.length
          ? movements[ordinal]
          : TrafficSignalPhaseMovement.THROUGH;
    }
    plan.setTransitPhase(c.getInteger(K_PHASE));
    plan.maxExtension = c.hasKey(K_EXTENSION) ? c.getLong(K_EXTENSION) : DEFAULT_MAX_EXTENSION;
    plan.maxEarlyReturn =
        c.hasKey(K_EARLY_RETURN) ? c.getLong(K_EARLY_RETURN) : DEFAULT_MAX_EARLY_RETURN;
    plan.minCyclesBetweenGrants = c.hasKey(K_MIN_CYCLES)
        ? c.getInteger(K_MIN_CYCLES)
        : DEFAULT_MIN_CYCLES_BETWEEN_GRANTS;
    plan.triggerOnDetectors = c.getBoolean(K_DETECTORS);
    plan.setQueueJump(c.hasKey(K_QUEUE_JUMP) ? c.getLong(K_QUEUE_JUMP) : DEFAULT_QUEUE_JUMP);
    return plan;
  }
}
