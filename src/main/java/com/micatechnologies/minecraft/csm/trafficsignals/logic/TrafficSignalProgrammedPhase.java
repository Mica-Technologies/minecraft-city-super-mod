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

  // endregion

  // region: Fields

  private int phaseNumber;
  private int ring;
  private int barrier;
  /** Index into the controller's circuit list, or -1 if unassigned. */
  private int circuitIndex = -1;
  private TrafficSignalPhaseMovement movement = TrafficSignalPhaseMovement.THROUGH;
  private boolean enabled = false;
  private long minGreen = DEFAULT_MIN_GREEN;
  private long passage = DEFAULT_PASSAGE;
  private long maxGreen = DEFAULT_MAX_GREEN;
  private long yellow = DEFAULT_YELLOW;
  private long redClear = DEFAULT_RED_CLEAR;
  private long walk = DEFAULT_WALK;
  private long pedClear = DEFAULT_PED_CLEAR;
  private TrafficSignalRecallMode recallMode = TrafficSignalRecallMode.NONE;
  private boolean pedRecall = false;

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

  // endregion

  // region: NBT

  public NBTTagCompound toNBT() {
    NBTTagCompound c = new NBTTagCompound();
    c.setInteger(K_NUMBER, phaseNumber);
    c.setInteger(K_RING, ring);
    c.setInteger(K_BARRIER, barrier);
    c.setInteger(K_CIRCUIT, circuitIndex);
    c.setInteger(K_MOVEMENT, movement.toNBT());
    c.setBoolean(K_ENABLED, enabled);
    c.setLong(K_MIN_GREEN, minGreen);
    c.setLong(K_PASSAGE, passage);
    c.setLong(K_MAX_GREEN, maxGreen);
    c.setLong(K_YELLOW, yellow);
    c.setLong(K_RED_CLEAR, redClear);
    c.setLong(K_WALK, walk);
    c.setLong(K_PED_CLEAR, pedClear);
    c.setInteger(K_RECALL, recallMode.toNBT());
    c.setBoolean(K_PED_RECALL, pedRecall);
    return c;
  }

  public static TrafficSignalProgrammedPhase fromNBT(NBTTagCompound c) {
    TrafficSignalProgrammedPhase p = new TrafficSignalProgrammedPhase(
        c.getInteger(K_NUMBER), c.getInteger(K_RING), c.getInteger(K_BARRIER));
    p.circuitIndex = c.hasKey(K_CIRCUIT) ? c.getInteger(K_CIRCUIT) : -1;
    p.movement = TrafficSignalPhaseMovement.fromNBT(c.getInteger(K_MOVEMENT));
    p.enabled = c.getBoolean(K_ENABLED);
    p.minGreen = c.hasKey(K_MIN_GREEN) ? c.getLong(K_MIN_GREEN) : DEFAULT_MIN_GREEN;
    p.passage = c.hasKey(K_PASSAGE) ? c.getLong(K_PASSAGE) : DEFAULT_PASSAGE;
    p.maxGreen = c.hasKey(K_MAX_GREEN) ? c.getLong(K_MAX_GREEN) : DEFAULT_MAX_GREEN;
    p.yellow = c.hasKey(K_YELLOW) ? c.getLong(K_YELLOW) : DEFAULT_YELLOW;
    p.redClear = c.hasKey(K_RED_CLEAR) ? c.getLong(K_RED_CLEAR) : DEFAULT_RED_CLEAR;
    p.walk = c.hasKey(K_WALK) ? c.getLong(K_WALK) : DEFAULT_WALK;
    p.pedClear = c.hasKey(K_PED_CLEAR) ? c.getLong(K_PED_CLEAR) : DEFAULT_PED_CLEAR;
    p.recallMode = TrafficSignalRecallMode.fromNBT(c.getInteger(K_RECALL));
    p.pedRecall = c.getBoolean(K_PED_RECALL);
    return p;
  }

  // endregion
}
