package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.nbt.NBTTagCompound;

/**
 * A single preemption sequence for {@code ADVANCED} mode (railroad, emergency-vehicle, or transit
 * priority). A preempt is <em>called</em> by detection in a circuit's sensor zone (reusing the same
 * sensors as the phases) and, when active, overrides normal/coordinated operation:
 *
 * <ol>
 *   <li><b>Enter</b> — terminate conflicting movements through yellow + red clearance.</li>
 *   <li><b>Track clear</b> — serve {@link #trackClearPhases} (the path to clear, e.g. off a rail
 *       crossing) for their timing.</li>
 *   <li><b>Dwell</b> — hold {@link #dwellPhases} for at least {@link #minDwell} while the call
 *       remains active (e.g. EV approach held green, conflicting movements red).</li>
 *   <li><b>Exit</b> — return through {@link #exitPhases} to normal/coordinated operation.</li>
 * </ol>
 *
 * <p>The trigger is expressed as a circuit index + movement, reusing the existing sensor zones, so
 * preemption needs no new detector blocks. All times are in game ticks (20 ticks = 1 second).
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public class TrafficSignalPreempt {

  /** Default minimum dwell: 10 seconds. */
  public static final long DEFAULT_MIN_DWELL = 200L;
  /** Default minimum green before a preempt may interrupt: 5 seconds. */
  public static final long DEFAULT_MIN_GREEN_BEFORE = 100L;

  private static final String K_ENABLED = "en";
  private static final String K_TYPE = "ty";
  private static final String K_TRIGGER_CIRCUIT = "tc";
  private static final String K_TRIGGER_MOVEMENT = "tm";
  private static final String K_TRACK_CLEAR = "tk";
  private static final String K_DWELL = "dw";
  private static final String K_EXIT = "ex";
  private static final String K_MIN_DWELL = "md";
  private static final String K_MIN_GREEN_BEFORE = "mb";

  private boolean enabled = false;
  private TrafficSignalPreemptType type = TrafficSignalPreemptType.EMERGENCY;
  /** Circuit index whose sensor zone calls this preempt, or -1 if unassigned. */
  private int triggerCircuitIndex = -1;
  private TrafficSignalPhaseMovement triggerMovement = TrafficSignalPhaseMovement.THROUGH;
  private int[] trackClearPhases = new int[0];
  private int[] dwellPhases = new int[0];
  private int[] exitPhases = new int[0];
  private long minDwell = DEFAULT_MIN_DWELL;
  private long minGreenBeforePreempt = DEFAULT_MIN_GREEN_BEFORE;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /** @return whether this preempt is enabled and has a trigger assigned (i.e. can fire). */
  public boolean isActive() {
    return enabled && triggerCircuitIndex >= 0;
  }

  public TrafficSignalPreemptType getType() {
    return type;
  }

  public void setType(TrafficSignalPreemptType type) {
    this.type = type;
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
    this.triggerMovement = triggerMovement;
  }

  public int[] getTrackClearPhases() {
    return trackClearPhases;
  }

  public void setTrackClearPhases(int[] trackClearPhases) {
    this.trackClearPhases = trackClearPhases == null ? new int[0] : trackClearPhases;
  }

  public int[] getDwellPhases() {
    return dwellPhases;
  }

  public void setDwellPhases(int[] dwellPhases) {
    this.dwellPhases = dwellPhases == null ? new int[0] : dwellPhases;
  }

  public int[] getExitPhases() {
    return exitPhases;
  }

  public void setExitPhases(int[] exitPhases) {
    this.exitPhases = exitPhases == null ? new int[0] : exitPhases;
  }

  public long getMinDwell() {
    return minDwell;
  }

  public void setMinDwell(long minDwell) {
    this.minDwell = minDwell;
  }

  public long getMinGreenBeforePreempt() {
    return minGreenBeforePreempt;
  }

  public void setMinGreenBeforePreempt(long minGreenBeforePreempt) {
    this.minGreenBeforePreempt = minGreenBeforePreempt;
  }

  // region: NBT

  public NBTTagCompound toNBT() {
    NBTTagCompound c = new NBTTagCompound();
    c.setBoolean(K_ENABLED, enabled);
    c.setInteger(K_TYPE, type.toNBT());
    c.setInteger(K_TRIGGER_CIRCUIT, triggerCircuitIndex);
    c.setInteger(K_TRIGGER_MOVEMENT, triggerMovement.toNBT());
    c.setIntArray(K_TRACK_CLEAR, trackClearPhases);
    c.setIntArray(K_DWELL, dwellPhases);
    c.setIntArray(K_EXIT, exitPhases);
    c.setLong(K_MIN_DWELL, minDwell);
    c.setLong(K_MIN_GREEN_BEFORE, minGreenBeforePreempt);
    return c;
  }

  public static TrafficSignalPreempt fromNBT(NBTTagCompound c) {
    TrafficSignalPreempt p = new TrafficSignalPreempt();
    p.enabled = c.getBoolean(K_ENABLED);
    p.type = TrafficSignalPreemptType.fromNBT(c.getInteger(K_TYPE));
    p.triggerCircuitIndex = c.hasKey(K_TRIGGER_CIRCUIT) ? c.getInteger(K_TRIGGER_CIRCUIT) : -1;
    p.triggerMovement = TrafficSignalPhaseMovement.fromNBT(c.getInteger(K_TRIGGER_MOVEMENT));
    p.trackClearPhases = c.getIntArray(K_TRACK_CLEAR);
    p.dwellPhases = c.getIntArray(K_DWELL);
    p.exitPhases = c.getIntArray(K_EXIT);
    p.minDwell = c.hasKey(K_MIN_DWELL) ? c.getLong(K_MIN_DWELL) : DEFAULT_MIN_DWELL;
    p.minGreenBeforePreempt =
        c.hasKey(K_MIN_GREEN_BEFORE) ? c.getLong(K_MIN_GREEN_BEFORE) : DEFAULT_MIN_GREEN_BEFORE;
    return p;
  }

  // endregion
}
