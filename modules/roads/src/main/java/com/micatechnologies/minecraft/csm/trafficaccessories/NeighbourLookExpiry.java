package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.util.EnumFacing;

/**
 * When a tile entity's cached look at the blocks around it is out of date: never looked, looked
 * for another facing, dropped by {@link #invalidate} (a sync announcing a neighbour change), or
 * older than {@link #ticks} (the backstop for what no sync covers: a neighbour in a chunk that
 * had not loaded yet, or a block beyond the cell whose change is announced). The render rules
 * forbid reading the world every frame; this is the bookkeeping for not doing so.
 *
 * @since 2026.10
 */
public final class NeighbourLookExpiry {

  private final long ticks;
  private long lookedAt = Long.MIN_VALUE;
  private EnumFacing lookedFacing;

  /**
   * Creates an expiry.
   *
   * @param ticks how long a look is trusted, in ticks
   */
  public NeighbourLookExpiry(long ticks) {
    this.ticks = ticks;
  }

  /**
   * Whether the cached look must be taken again.
   *
   * @param now    the world's total time
   * @param facing the facing the look is for
   *
   * @return {@code true} to look again
   */
  public boolean isStale(long now, EnumFacing facing) {
    return lookedAt == Long.MIN_VALUE || facing != lookedFacing || now < lookedAt
        || now - lookedAt >= ticks;
  }

  /**
   * Records that the look was just taken.
   *
   * @param now    the world's total time
   * @param facing the facing it was taken for
   */
  public void looked(long now, EnumFacing facing) {
    lookedAt = now;
    lookedFacing = facing;
  }

  /** Makes the next {@link #isStale} answer {@code true}. */
  public void invalidate() {
    lookedAt = Long.MIN_VALUE;
  }
}
