package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Configuration for a single NEMA phase in {@code ADVANCED} (ring-and-barrier) mode.
 *
 * <p>A programmed phase is a thin overlay on the existing circuit/sensor model: it references a
 * circuit ({@link #circuitIndex}) and a {@link TrafficSignalPhaseMovement}, which together select
 * the signal heads the phase drives and the sensor-summary zone that calls it. All vehicle and
 * pedestrian timing live here, per-phase, exactly as on a real controller.
 *
 * <p>Times are stored in game ticks (20 ticks = 1 second), matching every other timing value in
 * the controller.
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public class TrafficSignalProgrammedPhase {

  // region: Default timing (ticks; 20 ticks = 1 second)

  /** Default minimum green: 5 seconds. */
  public static final long DEFAULT_MIN_GREEN = 100L;
  /** Default passage / vehicle-extension (gap) time: 2 seconds. */
  public static final long DEFAULT_PASSAGE = 40L;
  /** Default maximum green: 30 seconds. */
  public static final long DEFAULT_MAX_GREEN = 600L;
  /** Default yellow change interval: 3.5 seconds. */
  public static final long DEFAULT_YELLOW = 70L;
  /** Default red clearance interval: 2 seconds. */
  public static final long DEFAULT_RED_CLEAR = 40L;
  /** Default walk interval: 7 seconds. */
  public static final long DEFAULT_WALK = 140L;
  /** Default pedestrian clearance (flashing don't walk): 10 seconds. */
  public static final long DEFAULT_PED_CLEAR = 200L;
  /** Default delayed green (ASC/3 DLY GRN / leading ped interval): 0 = off. */
  public static final long DEFAULT_DLY_GREEN = 0L;
  /** Default secondary max green (ASC/3 MAX 2): 0 = disabled (use Max 1). */
  public static final long DEFAULT_MAX2 = 0L;
  /** Default bike minimum green (ASC/3 BK MGRN): 0 = off. */
  public static final long DEFAULT_BIKE_MIN_GREEN = 0L;
  /** Default added-initial per waiting vehicle (volume-density): 0 = off. */
  public static final long DEFAULT_ADDED_INITIAL = 0L;
  /** Default max initial (volume-density cap on added initial): 0 = no cap contribution. */
  public static final long DEFAULT_MAX_INITIAL = 0L;
  /** Default minimum gap (volume-density gap reduction floor): 0 = no reduction. */
  public static final long DEFAULT_MIN_GAP = 0L;
  /** Default time-before-reduction (volume-density): 0 = reduce from green start. */
  public static final long DEFAULT_TIME_B4_REDUCE = 0L;
  /** Default time-to-reduce (volume-density): 0 = gap reduction disabled. */
  public static final long DEFAULT_TIME_TO_REDUCE = 0L;

  // endregion

  // region: NBT keys (namespaced inside this phase's own compound)

  private static final String K_NUMBER = "n";
  private static final String K_RING = "rg";
  private static final String K_BARRIER = "ba";
  private static final String K_CIRCUIT = "ci";
  private static final String K_MOVEMENT = "mv";
  private static final String K_ENABLED = "en";
  private static final String K_MIN_GREEN = "mg";
  private static final String K_PASSAGE = "pa";
  private static final String K_MAX_GREEN = "xg";
  private static final String K_YELLOW = "ye";
  private static final String K_RED_CLEAR = "rc";
  private static final String K_WALK = "wk";
  private static final String K_PED_CLEAR = "pc";
  private static final String K_RECALL = "rm";
  private static final String K_PED_RECALL = "pr";
  private static final String K_DLY_GREEN = "dg";
  private static final String K_PERM_PHASE = "pp";
  private static final String K_REST_IN_WALK = "rw";
  private static final String K_DUAL_ENTRY = "de";
  private static final String K_COND_SERVICE = "cs";
  private static final String K_LOCK_CALL = "lc";
  private static final String K_MAX2 = "x2";
  private static final String K_BIKE_MIN_GREEN = "bk";
  private static final String K_ADDED_INITIAL = "ai";
  private static final String K_MAX_INITIAL = "xi";
  private static final String K_MIN_GAP = "mn";
  private static final String K_TIME_B4_REDUCE = "t4";
  private static final String K_TIME_TO_REDUCE = "tr";
  private static final String K_FLASH_OVERRIDE = "fo";

  // endregion

  // region: Fields

  private int phaseNumber;
  private int ring;
  private int barrier;
  /** Index into the controller's circuit list, or -1 if unassigned. */
  private int circuitIndex = -1;
  private TrafficSignalPhaseMovement movement = TrafficSignalPhaseMovement.THROUGH;
  /**
   * What this phase's movement does in flash mode. {@link TrafficSignalFlashOverride#AUTO} (the
   * default) leaves the controller on its legacy circuit-ordinal flash rule.
   */
  private TrafficSignalFlashOverride flashOverride = TrafficSignalFlashOverride.AUTO;
  private boolean enabled = false;
  private long minGreen = DEFAULT_MIN_GREEN;
  private long passage = DEFAULT_PASSAGE;
  private long maxGreen = DEFAULT_MAX_GREEN;
  private long yellow = DEFAULT_YELLOW;
  private long redClear = DEFAULT_RED_CLEAR;
  private long walk = DEFAULT_WALK;
  private long pedClear = DEFAULT_PED_CLEAR;
  /** ASC/3 DLY GRN: vehicle green delayed from start of walk (leading ped interval). 0 = off. */
  private long delayedGreen = DEFAULT_DLY_GREEN;
  /** ASC/3 MAX 2: secondary maximum green, used in coordinated operation when set. */
  private long max2 = DEFAULT_MAX2;
  /** ASC/3 BK MGRN: minimum green guaranteed when a bike call is present at green start. */
  private long bikeMinGreen = DEFAULT_BIKE_MIN_GREEN;
  /** Volume-density: extra guaranteed initial green per waiting vehicle at green start. */
  private long addedInitial = DEFAULT_ADDED_INITIAL;
  /** Volume-density: cap on the computed added-initial green. */
  private long maxInitial = DEFAULT_MAX_INITIAL;
  /** Volume-density: gap-reduction floor (the passage shrinks toward this). */
  private long minGap = DEFAULT_MIN_GAP;
  /** Volume-density: green time before gap reduction begins. */
  private long timeBeforeReduce = DEFAULT_TIME_B4_REDUCE;
  /** Volume-density: duration over which the passage reduces from its value to the min gap. */
  private long timeToReduce = DEFAULT_TIME_TO_REDUCE;
  private TrafficSignalRecallMode recallMode = TrafficSignalRecallMode.NONE;
  private boolean pedRecall = false;
  /** ASC/3 REST IN WALK: hold the WALK indication while resting on this phase (vs. don't-walk). */
  private boolean restInWalk = false;
  /** ASC/3 DUAL ENTRY: serve this phase to companion the other ring when it has no call. */
  private boolean dualEntry = false;
  /** ASC/3 conditional service: may be re-served once within the same barrier on a fresh call. */
  private boolean conditionalService = false;
  /**
   * ASC/3 vehicle call memory (locking detector memory): a vehicle call registered while this
   * phase is not being served green is latched until the phase's next green, even if the vehicle
   * leaves the detection zone (pulse-detector behavior). Default {@code false} = non-locking
   * (presence): the call exists only while the detector sees the vehicle — the right choice for
   * stop-bar presence zones, where a car that clears on a permissive FYA flash should drop its
   * call rather than leave a phantom protected arrow behind.
   */
  private boolean lockCall = false;
  /**
   * ASC/3 PPLT FYA permissive phase: the opposing-through phase number whose green makes this
   * (left) phase show a flashing yellow arrow. 0 = protected-only (no FYA).
   */
  private int permissivePhase = 0;

  // endregion

  public TrafficSignalProgrammedPhase(int phaseNumber, int ring, int barrier) {
    this.phaseNumber = phaseNumber;
    this.ring = ring;
    this.barrier = barrier;
  }

  // region: Getters / setters

  public int getPhaseNumber() {
    return phaseNumber;
  }

  public int getRing() {
    return ring;
  }

  public void setRing(int ring) {
    this.ring = ring;
  }

  public int getBarrier() {
    return barrier;
  }

  public void setBarrier(int barrier) {
    this.barrier = barrier;
  }

  public int getCircuitIndex() {
    return circuitIndex;
  }

  public void setCircuitIndex(int circuitIndex) {
    this.circuitIndex = circuitIndex;
  }

  public TrafficSignalPhaseMovement getMovement() {
    return movement;
  }

  public void setMovement(TrafficSignalPhaseMovement movement) {
    this.movement = movement;
  }

  public TrafficSignalFlashOverride getFlashOverride() {
    return flashOverride;
  }

  public void setFlashOverride(TrafficSignalFlashOverride flashOverride) {
    this.flashOverride = flashOverride;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /** @return whether this phase has an assigned circuit and is enabled (i.e. can be served). */
  public boolean isActive() {
    return enabled && circuitIndex >= 0;
  }

  public long getMinGreen() {
    return minGreen;
  }

  public void setMinGreen(long minGreen) {
    this.minGreen = minGreen;
  }

  public long getPassage() {
    return passage;
  }

  public void setPassage(long passage) {
    this.passage = passage;
  }

  public long getMaxGreen() {
    return maxGreen;
  }

  public void setMaxGreen(long maxGreen) {
    this.maxGreen = maxGreen;
  }

  public long getYellow() {
    return yellow;
  }

  public void setYellow(long yellow) {
    this.yellow = yellow;
  }

  public long getRedClear() {
    return redClear;
  }

  public void setRedClear(long redClear) {
    this.redClear = redClear;
  }

  public long getWalk() {
    return walk;
  }

  public void setWalk(long walk) {
    this.walk = walk;
  }

  public long getPedClear() {
    return pedClear;
  }

  public void setPedClear(long pedClear) {
    this.pedClear = pedClear;
  }

  public long getDelayedGreen() {
    return delayedGreen;
  }

  public void setDelayedGreen(long delayedGreen) {
    this.delayedGreen = delayedGreen;
  }

  public int getPermissivePhase() {
    return permissivePhase;
  }

  public void setPermissivePhase(int permissivePhase) {
    this.permissivePhase = permissivePhase;
  }

  public long getMax2() {
    return max2;
  }

  public void setMax2(long max2) {
    this.max2 = max2;
  }

  public long getBikeMinGreen() {
    return bikeMinGreen;
  }

  public void setBikeMinGreen(long bikeMinGreen) {
    this.bikeMinGreen = bikeMinGreen;
  }

  public long getAddedInitial() {
    return addedInitial;
  }

  public void setAddedInitial(long addedInitial) {
    this.addedInitial = addedInitial;
  }

  public long getMaxInitial() {
    return maxInitial;
  }

  public void setMaxInitial(long maxInitial) {
    this.maxInitial = maxInitial;
  }

  public long getMinGap() {
    return minGap;
  }

  public void setMinGap(long minGap) {
    this.minGap = minGap;
  }

  public long getTimeBeforeReduce() {
    return timeBeforeReduce;
  }

  public void setTimeBeforeReduce(long timeBeforeReduce) {
    this.timeBeforeReduce = timeBeforeReduce;
  }

  public long getTimeToReduce() {
    return timeToReduce;
  }

  public void setTimeToReduce(long timeToReduce) {
    this.timeToReduce = timeToReduce;
  }

  public TrafficSignalRecallMode getRecallMode() {
    return recallMode;
  }

  public void setRecallMode(TrafficSignalRecallMode recallMode) {
    this.recallMode = recallMode;
  }

  public boolean isPedRecall() {
    return pedRecall;
  }

  public void setPedRecall(boolean pedRecall) {
    this.pedRecall = pedRecall;
  }

  public boolean isRestInWalk() {
    return restInWalk;
  }

  public void setRestInWalk(boolean restInWalk) {
    this.restInWalk = restInWalk;
  }

  public boolean isDualEntry() {
    return dualEntry;
  }

  public void setDualEntry(boolean dualEntry) {
    this.dualEntry = dualEntry;
  }

  public boolean isConditionalService() {
    return conditionalService;
  }

  public void setConditionalService(boolean conditionalService) {
    this.conditionalService = conditionalService;
  }

  public boolean isLockCall() {
    return lockCall;
  }

  public void setLockCall(boolean lockCall) {
    this.lockCall = lockCall;
  }

  // endregion

  // region: NBT

  public NBTTagCompound toNBT() {
    NBTTagCompound c = new NBTTagCompound();
    c.setInteger(K_NUMBER, phaseNumber);
    c.setInteger(K_RING, ring);
    c.setInteger(K_BARRIER, barrier);
    c.setInteger(K_CIRCUIT, circuitIndex);
    c.setInteger(K_MOVEMENT, movement.toNBT());
    c.setInteger(K_FLASH_OVERRIDE, flashOverride.toNBT());
    c.setBoolean(K_ENABLED, enabled);
    c.setLong(K_MIN_GREEN, minGreen);
    c.setLong(K_PASSAGE, passage);
    c.setLong(K_MAX_GREEN, maxGreen);
    c.setLong(K_YELLOW, yellow);
    c.setLong(K_RED_CLEAR, redClear);
    c.setLong(K_WALK, walk);
    c.setLong(K_PED_CLEAR, pedClear);
    c.setLong(K_DLY_GREEN, delayedGreen);
    c.setInteger(K_RECALL, recallMode.toNBT());
    c.setBoolean(K_PED_RECALL, pedRecall);
    c.setBoolean(K_REST_IN_WALK, restInWalk);
    c.setBoolean(K_DUAL_ENTRY, dualEntry);
    c.setBoolean(K_COND_SERVICE, conditionalService);
    c.setBoolean(K_LOCK_CALL, lockCall);
    c.setInteger(K_PERM_PHASE, permissivePhase);
    c.setLong(K_MAX2, max2);
    c.setLong(K_BIKE_MIN_GREEN, bikeMinGreen);
    c.setLong(K_ADDED_INITIAL, addedInitial);
    c.setLong(K_MAX_INITIAL, maxInitial);
    c.setLong(K_MIN_GAP, minGap);
    c.setLong(K_TIME_B4_REDUCE, timeBeforeReduce);
    c.setLong(K_TIME_TO_REDUCE, timeToReduce);
    return c;
  }

  public static TrafficSignalProgrammedPhase fromNBT(NBTTagCompound c) {
    TrafficSignalProgrammedPhase p = new TrafficSignalProgrammedPhase(
        c.getInteger(K_NUMBER), c.getInteger(K_RING), c.getInteger(K_BARRIER));
    p.circuitIndex = c.hasKey(K_CIRCUIT) ? c.getInteger(K_CIRCUIT) : -1;
    p.movement = TrafficSignalPhaseMovement.fromNBT(c.getInteger(K_MOVEMENT));
    p.flashOverride = TrafficSignalFlashOverride.fromNBT(c.getInteger(K_FLASH_OVERRIDE));
    p.enabled = c.getBoolean(K_ENABLED);
    p.minGreen = c.hasKey(K_MIN_GREEN) ? c.getLong(K_MIN_GREEN) : DEFAULT_MIN_GREEN;
    p.passage = c.hasKey(K_PASSAGE) ? c.getLong(K_PASSAGE) : DEFAULT_PASSAGE;
    p.maxGreen = c.hasKey(K_MAX_GREEN) ? c.getLong(K_MAX_GREEN) : DEFAULT_MAX_GREEN;
    p.yellow = c.hasKey(K_YELLOW) ? c.getLong(K_YELLOW) : DEFAULT_YELLOW;
    p.redClear = c.hasKey(K_RED_CLEAR) ? c.getLong(K_RED_CLEAR) : DEFAULT_RED_CLEAR;
    p.walk = c.hasKey(K_WALK) ? c.getLong(K_WALK) : DEFAULT_WALK;
    p.pedClear = c.hasKey(K_PED_CLEAR) ? c.getLong(K_PED_CLEAR) : DEFAULT_PED_CLEAR;
    p.delayedGreen = c.hasKey(K_DLY_GREEN) ? c.getLong(K_DLY_GREEN) : DEFAULT_DLY_GREEN;
    p.recallMode = TrafficSignalRecallMode.fromNBT(c.getInteger(K_RECALL));
    p.pedRecall = c.getBoolean(K_PED_RECALL);
    p.restInWalk = c.getBoolean(K_REST_IN_WALK);
    p.dualEntry = c.getBoolean(K_DUAL_ENTRY);
    p.conditionalService = c.getBoolean(K_COND_SERVICE);
    p.lockCall = c.getBoolean(K_LOCK_CALL);
    p.permissivePhase = c.hasKey(K_PERM_PHASE) ? c.getInteger(K_PERM_PHASE) : 0;
    p.max2 = c.hasKey(K_MAX2) ? c.getLong(K_MAX2) : DEFAULT_MAX2;
    p.bikeMinGreen = c.hasKey(K_BIKE_MIN_GREEN) ? c.getLong(K_BIKE_MIN_GREEN) : DEFAULT_BIKE_MIN_GREEN;
    p.addedInitial = c.hasKey(K_ADDED_INITIAL) ? c.getLong(K_ADDED_INITIAL) : DEFAULT_ADDED_INITIAL;
    p.maxInitial = c.hasKey(K_MAX_INITIAL) ? c.getLong(K_MAX_INITIAL) : DEFAULT_MAX_INITIAL;
    p.minGap = c.hasKey(K_MIN_GAP) ? c.getLong(K_MIN_GAP) : DEFAULT_MIN_GAP;
    p.timeBeforeReduce =
        c.hasKey(K_TIME_B4_REDUCE) ? c.getLong(K_TIME_B4_REDUCE) : DEFAULT_TIME_B4_REDUCE;
    p.timeToReduce = c.hasKey(K_TIME_TO_REDUCE) ? c.getLong(K_TIME_TO_REDUCE) : DEFAULT_TIME_TO_REDUCE;
    return p;
  }

  // endregion
}
