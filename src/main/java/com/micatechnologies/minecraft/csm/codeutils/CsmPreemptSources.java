package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.World;

/**
 * The registry of {@link ICsmPreemptSource}s, and the one place a preempt detector asks what
 * emitters it can see.
 * <p>
 * A module that knows about vehicles (CSM: Vehicles, for Immersive Vehicles) registers a source
 * from its pre-initialization; Roads' preempt detector calls {@link #emitters(World)} and
 * {@link #sees}. Neither names the other, which is what lets a module reference only Core.
 * <p>
 * The emitters are collected once a world tick, the first time any detector asks, and shared by
 * every detector in the world that tick. With no source registered nothing is ever collected, so
 * a detector costs a list check.
 *
 * @version 1.0
 * @since 2026.10
 */
public final class CsmPreemptSources {

  /**
   * How far either side of straight ahead an emitter's beam reaches, in degrees. A real
   * emitter's beam is wide enough to reach the detector round the last bend before a junction;
   * it still never reaches one behind the vehicle.
   *
   * @since 1.0
   */
  public static final double EMITTER_HALF_ANGLE_DEGREES = 45.0;

  /**
   * The registered sources, in registration order.
   *
   * @since 1.0
   */
  private static final List<ICsmPreemptSource> SOURCES = new ArrayList<>();

  /**
   * Each world's emitters as last collected, keyed weakly so an unloaded world is let go.
   *
   * @since 1.0
   */
  private static final Map<World, Collected> COLLECTED = new WeakHashMap<>();

  /**
   * The sources whose failure has been logged, so each is logged once.
   *
   * @since 1.0
   */
  private static final Set<ICsmPreemptSource> FAILED =
      Collections.newSetFromMap(new IdentityHashMap<>());

  /**
   * A world's emitters and the world tick they were collected on.
   *
   * @since 1.0
   */
  private static final class Collected {

    private final long tick;
    private final List<CsmPreemptEmitter> emitters;

    private Collected(long tick, List<CsmPreemptEmitter> emitters) {
      this.tick = tick;
      this.emitters = emitters;
    }
  }

  /**
   * Private constructor: this class is a static registry and is never instantiated.
   *
   * @since 1.0
   */
  private CsmPreemptSources() {
    throw new UnsupportedOperationException("CsmPreemptSources must not be instantiated");
  }

  /**
   * Registers a source. Call this from a mod's pre-initialization.
   *
   * @param source the source
   *
   * @since 1.0
   */
  public static synchronized void register(ICsmPreemptSource source) {
    if (source != null) {
      SOURCES.add(source);
    }
  }

  /**
   * Whether any source is registered. Without one, no detector can be called except by
   * redstone.
   *
   * @return whether any source is registered
   *
   * @since 1.0
   */
  public static boolean hasSources() {
    return !SOURCES.isEmpty();
  }

  /**
   * The emitters that are on in a world this tick, collected from every source on the first call
   * of the tick. A source that throws is skipped for that tick rather than taking the detector
   * down with it.
   *
   * @param world the world (server side)
   *
   * @return the emitters, never null; do not modify it
   *
   * @since 1.0
   */
  public static synchronized List<CsmPreemptEmitter> emitters(World world) {
    if (SOURCES.isEmpty() || world == null || world.isRemote) {
      return Collections.emptyList();
    }
    long tick = world.getTotalWorldTime();
    Collected collected = COLLECTED.get(world);
    if (collected != null && collected.tick == tick) {
      return collected.emitters;
    }
    List<CsmPreemptEmitter> out = new ArrayList<>();
    for (ICsmPreemptSource source : SOURCES) {
      try {
        source.collectEmitters(world, out);
      } catch (RuntimeException | LinkageError e) {
        // A source that fails (another mod's internals changed under it) calls nothing this tick.
        // Said once per source, or a broken source would fill the log four times a second.
        if (FAILED.add(source)) {
          Csm.getLogger().error("Preempt source {} failed; it will call no preempts until it "
              + "works again", source.getClass().getName(), e);
        }
      }
    }
    COLLECTED.put(world, new Collected(tick, out));
    return out;
  }

  /**
   * Whether a detector sees an emitter: the emitter is within the detector's range and inside
   * its cone, the detector is inside the emitter's beam, and the emitter is not far above or below
   * the detector (a quarter of the range, at least 16 blocks). Pure geometry, kept here so it can
   * be tested without a world.
   *
   * @param emitter           the emitter
   * @param detectorX         the detector's lens, in world coordinates
   * @param detectorY         the detector's lens, in world coordinates
   * @param detectorZ         the detector's lens, in world coordinates
   * @param facingX           the direction the detector looks, a horizontal unit vector (x part)
   * @param facingZ           the direction the detector looks, a horizontal unit vector (z part)
   * @param range             how far the detector sees, in blocks
   * @param halfAngleDegrees  how far either side of its facing the detector sees, in degrees
   *
   * @return whether the detector sees the emitter
   *
   * @since 1.0
   */
  public static boolean sees(CsmPreemptEmitter emitter, double detectorX, double detectorY,
      double detectorZ, double facingX, double facingZ, double range, double halfAngleDegrees) {
    double dx = emitter.x - detectorX;
    double dz = emitter.z - detectorZ;
    double dy = emitter.y - detectorY;
    double horizontal = Math.sqrt(dx * dx + dz * dz);
    if (horizontal < 1.0E-6) {
      return Math.abs(dy) <= 16.0; // right under the detector: no direction to judge
    }
    if (horizontal > range || Math.abs(dy) > Math.max(16.0, range / 4)) {
      return false;
    }
    // The emitter lies inside the detector's cone.
    double cosToEmitter = (dx * facingX + dz * facingZ) / horizontal;
    if (cosToEmitter < Math.cos(Math.toRadians(halfAngleDegrees))) {
      return false;
    }
    // And the vehicle points at the detector: the detector lies inside the emitter's beam.
    if (emitter.headingX == 0.0 && emitter.headingZ == 0.0) {
      return true;
    }
    double cosToDetector = (-dx * emitter.headingX - dz * emitter.headingZ) / horizontal;
    return cosToDetector >= Math.cos(Math.toRadians(EMITTER_HALF_ANGLE_DEGREES));
  }
}
