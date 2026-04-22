package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.world.World;

/**
 * Shared helpers for client-side render paths. The main purpose is to provide a pause-aware
 * millisecond-resolution game clock that avoids the per-frame JNI cost of
 * {@link System#currentTimeMillis()}.
 *
 * <p>Use {@link #gameMillis(World, float)} from inside a TESR's {@code render} method where
 * {@code partialTicks} is available; use {@link #gameMillis(World)} from tick-scoped code or
 * anywhere {@code partialTicks} is not in scope (e.g. helper methods on a tile entity called
 * from the renderer).
 */
public final class CsmRenderUtils {

  /**
   * Ticks per second in Minecraft. Used to convert between game ticks and a millisecond-scale
   * clock so that existing millisecond-based timing comparisons (e.g.
   * {@code t % 1000L < 500L}) continue to work after swapping
   * {@link System#currentTimeMillis()} for a pause-aware game clock.
   */
  private static final long MS_PER_TICK = 50L;

  private CsmRenderUtils() {}

  /**
   * Returns a pause-aware game clock in milliseconds, with sub-tick precision supplied by
   * {@code partialTicks}. Each game tick corresponds to 50 ms (matching the 20 TPS tick
   * rate), and {@code partialTicks} refines the value to 0..50 ms within the current tick.
   *
   * <p>Behavior notes:
   * <ul>
   *   <li>Paused game freezes this clock (unlike {@link System#currentTimeMillis()}).</li>
   *   <li>Resets when the world resets, not when wall clock wraps.</li>
   *   <li>Far cheaper than {@link System#currentTimeMillis()} in a render loop — the JNI
   *       call is eliminated entirely.</li>
   * </ul>
   *
   * @param world        the world the renderer is rendering in; must be non-null
   * @param partialTicks the {@code partialTicks} value passed to
   *                     {@code TileEntitySpecialRenderer#render}
   * @return a pseudo-millisecond game clock suitable for modulo/comparison against
   *     existing millisecond constants
   */
  public static long gameMillis(World world, float partialTicks) {
    return (world.getTotalWorldTime() * MS_PER_TICK) + (long) (partialTicks * MS_PER_TICK);
  }

  /**
   * Returns a pause-aware game clock in milliseconds, quantized to tick boundaries (50 ms
   * increments). Use this overload when {@code partialTicks} is not available at the call
   * site — e.g. helper methods on a tile entity or block class that are invoked from the
   * renderer without the partial-tick value threaded through.
   *
   * @param world the world to source tick time from; must be non-null
   * @return a pseudo-millisecond game clock
   */
  public static long gameMillis(World world) {
    return world.getTotalWorldTime() * MS_PER_TICK;
  }
}
