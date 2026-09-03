package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Coordination settings for {@code ADVANCED} mode: cycle length, offset, the coordinated phase set,
 * and per-phase split times. When {@link TrafficSignalCoordinationMode#COORDINATED} is active, the
 * controller runs on a fixed background cycle derived from world time + offset; non-coordinated
 * phases are bounded by force-off points computed from the splits, while coordinated phases rest in
 * green and yield only within their permissive windows.
 *
 * <p>Splits are stored indexed by phase number (1-8) with index 0 unused, for direct lookup. All
 * times are in game ticks (20 ticks = 1 second).
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public class TrafficSignalCoordinationPlan {

  /** Default cycle length: 90 seconds. */
  public static final long DEFAULT_CYCLE_LENGTH = 1800L;

  private static final String K_MODE = "md";
  private static final String K_CYCLE = "cy";
  private static final String K_OFFSET = "of";
  private static final String K_COORD_PHASES = "cp";
  private static final String K_SPLITS = "sp";

  /** Number of phase slots (index 0 unused; phases 1-8). */
  private static final int SPLIT_SLOTS = 9;

  private TrafficSignalCoordinationMode mode = TrafficSignalCoordinationMode.FREE;
  private long cycleLength = DEFAULT_CYCLE_LENGTH;
  private long offset = 0L;
  /** Phase numbers that are coordinated (rest in green). Default {2, 6}. */
  private int[] coordinatedPhases = new int[] {2, 6};
  /** Per-phase split in ticks, indexed by phase number; 0 = unset/auto. */
  private final long[] splits = new long[SPLIT_SLOTS];

  public TrafficSignalCoordinationMode getMode() {
    return mode;
  }

  public void setMode(TrafficSignalCoordinationMode mode) {
    this.mode = mode;
  }

  public boolean isCoordinated() {
    return mode == TrafficSignalCoordinationMode.COORDINATED;
  }

  public long getCycleLength() {
    return cycleLength;
  }

  public void setCycleLength(long cycleLength) {
    this.cycleLength = Math.max(1L, cycleLength);
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  public int[] getCoordinatedPhases() {
    return coordinatedPhases;
  }

  public void setCoordinatedPhases(int[] coordinatedPhases) {
    this.coordinatedPhases = coordinatedPhases == null ? new int[0] : coordinatedPhases;
  }

  public boolean isCoordinatedPhase(int phaseNumber) {
    for (int p : coordinatedPhases) {
      if (p == phaseNumber) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param phaseNumber the phase number (1-8)
   *
   * @return the configured split for the phase in ticks, or 0 if unset.
   */
  public long getSplit(int phaseNumber) {
    if (phaseNumber < 1 || phaseNumber >= SPLIT_SLOTS) {
      return 0L;
    }
    return splits[phaseNumber];
  }

  public void setSplit(int phaseNumber, long ticks) {
    if (phaseNumber >= 1 && phaseNumber < SPLIT_SLOTS) {
      splits[phaseNumber] = Math.max(0L, ticks);
    }
  }

  // region: NBT

  public NBTTagCompound toNBT() {
    NBTTagCompound c = new NBTTagCompound();
    c.setInteger(K_MODE, mode.toNBT());
    c.setLong(K_CYCLE, cycleLength);
    c.setLong(K_OFFSET, offset);
    c.setIntArray(K_COORD_PHASES, coordinatedPhases);
    // Store splits as a long array via the underlying NBT long-array support.
    NBTTagCompound splitsTag = new NBTTagCompound();
    for (int i = 1; i < SPLIT_SLOTS; i++) {
      if (splits[i] > 0L) {
        splitsTag.setLong(String.valueOf(i), splits[i]);
      }
    }
    c.setTag(K_SPLITS, splitsTag);
    return c;
  }

  public static TrafficSignalCoordinationPlan fromNBT(NBTTagCompound c) {
    TrafficSignalCoordinationPlan p = new TrafficSignalCoordinationPlan();
    p.mode = TrafficSignalCoordinationMode.fromNBT(c.getInteger(K_MODE));
    p.cycleLength = c.hasKey(K_CYCLE) ? c.getLong(K_CYCLE) : DEFAULT_CYCLE_LENGTH;
    p.offset = c.getLong(K_OFFSET);
    if (c.hasKey(K_COORD_PHASES)) {
      p.coordinatedPhases = c.getIntArray(K_COORD_PHASES);
    }
    if (c.hasKey(K_SPLITS)) {
      NBTTagCompound splitsTag = c.getCompoundTag(K_SPLITS);
      for (int i = 1; i < SPLIT_SLOTS; i++) {
        String key = String.valueOf(i);
        if (splitsTag.hasKey(key)) {
          p.splits[i] = splitsTag.getLong(key);
        }
      }
    }
    return p;
  }

  // endregion
}
