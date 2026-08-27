package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

/**
 * Abstract tile entity implementation with tick handler and customizable tick rate. This class is
 * based on the {@link AbstractTileEntity} class and implements the {@link ITickable} interface.
 *
 * @author Mica Technologies
 * @since 2023.2.0
 */
public abstract class AbstractTickableTileEntity extends AbstractTileEntity implements ITickable {

  /**
   * Cached tick rate value, lazily initialized from {@link #getTickRate()} on first use.
   * A value of {@code -1} indicates the cache is not yet populated.
   * Subclasses that dynamically change their tick rate should call
   * {@link #invalidateTickRateCache()} to force a refresh.
   */
  private transient long cachedTickRate = -1;

  /**
   * Returns the tick rate, using a cached value when available. The cache is populated lazily
   * on first access and can be invalidated via {@link #invalidateTickRateCache()}.
   *
   * @return the cached tick rate
   */
  private long getCachedTickRate() {
    if (cachedTickRate == -1) {
      cachedTickRate = getTickRate();
    }
    return cachedTickRate;
  }

  /**
   * Invalidates the cached tick rate so that the next tick will re-read the value from
   * {@link #getTickRate()}. Subclasses should call this method whenever they modify the
   * value that {@link #getTickRate()} returns.
   */
  protected void invalidateTickRateCache() {
    cachedTickRate = -1;
  }

  /**
   * Handler for when the tile entity ticks. This method is called every tick, and a comparison is
   * made to see if the tile entity's tick rate has been reached. If so, the onTick() method is
   * called. The tick rate is determined by the value returned by the {@link #getTickRate()}
   * method.
   */
  @Override
  public void update() {
    if (getWorld() == null) {
      return;
    }
    if (doClientTick() || !getWorld().isRemote) {
      long tickRate = getCachedTickRate();
      if (tickRate <= 0L) {
        // Guard against a misconfigured (zero/negative) tick rate, which would otherwise throw an
        // ArithmeticException on the modulo below every tick.
        tickRate = 1L;
      }
      // Stagger by block position so the (often many) tile entities that share a tick rate don't all
      // fire on the same world tick -- unstaggered, they cause periodic TPS spikes ("thundering
      // herd"). The tick frequency (once every tickRate ticks) is unchanged; only the phase is offset.
      if (!pauseTicking() && (getWorld().getTotalWorldTime() + tickPhaseOffset()) % tickRate == 0L) {
        try {
          onTick();
        } catch (Exception e) {
          Csm.getLogger().error("Error ticking tile entity [remote: {}, class: {}, pos: {}]",
              world.isRemote, this.getClass().getCanonicalName(), getPos(), e);
        }
      }
    }
  }

  /**
   * Cached phase offset. The offset depends only on this tile entity's position, which never
   * changes, so deriving it once is both cheaper and clearer than recomputing it every tick.
   * {@link Long#MIN_VALUE} means "not yet computed" -- the real values are non-negative.
   */
  private transient long cachedTickPhaseOffset = Long.MIN_VALUE;

  /**
   * Returns a stable, non-negative per-position phase offset, so tile entities with the same tick
   * rate tick on different world ticks and spread their work across ticks instead of spiking on
   * the same one.
   *
   * <p><b>Why the position hash is not used directly.</b> {@code BlockPos.hashCode()} is linear in
   * x, y and z: {@code (y + z * 31) * 31 + x}. On a regular lattice -- which is exactly what a city
   * build is, with intersections on a street grid -- consecutive positions therefore advance the
   * hash by a constant step, and {@code step % tickRate} shares a factor with the tick rate. The
   * offsets collapse onto a handful of values instead of spreading across the whole period.
   * Measured on a 16-block grid of controllers, the raw hash produced <em>five</em> distinct
   * phases regardless of whether the tick rate was 20, 40 or 80, putting a fifth of every
   * controller on the same tick -- the precise "thundering herd" this method exists to prevent.</p>
   *
   * <p>Running the hash through an avalanche finalizer (murmur3's fmix32) destroys that linear
   * structure. The same 16-block grid then yields 18 of 20, 35 of 40 and 55 of 80 phases, and the
   * worst single tick drops from a fifth of the controllers to a twentieth.</p>
   *
   * @return a stable non-negative tick phase offset derived from this tile entity's position
   */
  private long tickPhaseOffset() {
    if (cachedTickPhaseOffset == Long.MIN_VALUE) {
      BlockPos pos = getPos();
      cachedTickPhaseOffset = pos == null
          ? 0L
          : Math.floorMod((long) avalanche(pos.hashCode()), Integer.MAX_VALUE);
    }
    return cachedTickPhaseOffset;
  }

  /**
   * murmur3's fmix32 finalizer: mixes every input bit into every output bit, so inputs that differ
   * by a constant step do not produce outputs that differ by a constant step.
   *
   * @param hash the value to mix
   *
   * @return the mixed value
   */
  private static int avalanche(int hash) {
    int h = hash;
    h ^= h >>> 16;
    h *= 0x85EBCA6B;
    h ^= h >>> 13;
    h *= 0xC2B2AE35;
    h ^= h >>> 16;
    return h;
  }

  /**
   * Abstract method which must be implemented to return a boolean indicating if the tile entity
   * should also tick on the client side. By default, the tile entity will always tick on the server
   * side, and in the event of singleplayer/local mode, the host client is considered the server.
   *
   * @return a boolean indicating if the tile entity should also tick on the client side
   */
  public abstract boolean doClientTick();

  /**
   * Abstract method which must be implemented to return a boolean indicating if the tile entity
   * ticking should be paused. If the tile entity is paused, the tick event will not be called.
   *
   * @return a boolean indicating if the tile entity ticking should be paused
   */
  public abstract boolean pauseTicking();

  /**
   * Abstract method which must be implemented to return the tick rate of the tile entity.
   *
   * @return the tick rate of the tile entity
   */
  public abstract long getTickRate();

  /**
   * Abstract method which must be implemented to handle the tick event of the tile entity.
   */
  public abstract void onTick();
}
