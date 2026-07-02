package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Shared helpers for client-side render paths. The main purpose is to provide a
 * millisecond-resolution clock that drives all of the mod's signal/strobe flash timing.
 *
 * <p>The clock is sampled from {@link System#currentTimeMillis()} (wall clock) rather than from
 * {@code world.getTotalWorldTime()}. Wall-clock timing keeps flash rate and phase visually
 * constant even when the server/client stutters: a tick lag spike no longer freezes or slews the
 * flash, because the clock is decoupled from tick progression. The one property we give up is
 * cross-player flash sync — two players watching the same signal are no longer guaranteed to see
 * the flash in the same phase — which is intentionally not something this mod cares about.
 *
 * <p>To avoid paying the {@link System#currentTimeMillis()} JNI cost once per visible signal per
 * frame (which adds up in a signal-dense city), the wall clock is sampled exactly once per render
 * frame in {@link FrameClock#onRenderTick(TickEvent.RenderTickEvent)} and cached. Every
 * {@code gameMillis} call in a frame then returns that same cached value — so the cost is one JNI
 * call per frame regardless of how many signals are on screen.
 *
 * <p>Use {@link #gameMillis(World, float)} from inside a TESR's {@code render} method; use
 * {@link #gameMillis(World)} from tick-scoped code or anywhere {@code partialTicks} is not in
 * scope (e.g. helper methods on a tile entity called from the renderer). Both overloads return
 * the same per-frame wall-clock snapshot; the {@code partialTicks} parameter is retained only for
 * call-site compatibility.
 */
public final class CsmRenderUtils {

  /**
   * Wall-clock time (ms) sampled once at the start of the current render frame. Read by every
   * {@code gameMillis} call during that frame. Written only on the client render thread (from
   * {@link FrameClock}); {@code volatile} because tile-entity helper methods that read it may be
   * invoked from render-adjacent paths and we want the freshest published value.
   */
  private static volatile long frameWallMillis = System.currentTimeMillis();

  private CsmRenderUtils() {}

  /**
   * Returns the wall-clock time (in milliseconds) sampled at the start of the current render
   * frame. Suitable for modulo/comparison against existing millisecond constants (e.g.
   * {@code t % 1000L < 500L}).
   *
   * @param world        unused; retained for call-site compatibility
   * @param partialTicks unused; retained for call-site compatibility
   * @return the current frame's wall-clock millisecond snapshot
   */
  public static long gameMillis(World world, float partialTicks) {
    return frameWallMillis;
  }

  /**
   * Returns the wall-clock time (in milliseconds) sampled at the start of the current render
   * frame. Use this overload when {@code partialTicks} is not available at the call site.
   *
   * @param world unused; retained for call-site compatibility
   * @return the current frame's wall-clock millisecond snapshot
   */
  public static long gameMillis(World world) {
    return frameWallMillis;
  }

  /**
   * Forge event handler that refreshes the cached wall clock once per render frame. Registered on
   * the client event bus from {@code CsmClientProxy#preInit}. Sampling at
   * {@link TickEvent.Phase#START} guarantees the value is fresh before any TESR draws this frame.
   */
  public static final class FrameClock {

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
      if (event.phase == TickEvent.Phase.START) {
        frameWallMillis = System.currentTimeMillis();
      }
    }
  }
}
