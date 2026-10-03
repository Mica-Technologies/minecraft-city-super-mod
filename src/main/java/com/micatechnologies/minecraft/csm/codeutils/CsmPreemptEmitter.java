package com.micatechnologies.minecraft.csm.codeutils;

/**
 * One preemption emitter that is on: where it is, which way it points, and what kind of call it
 * makes. Reported by an {@link ICsmPreemptSource} and read by Roads' preempt detector.
 * <p>
 * A real emitter is a strobe on the vehicle facing forward, so a detector sees it only while the
 * vehicle is pointed at the detector, never once it has passed through and is driving away. That
 * is what {@link #headingX} and {@link #headingZ} are for.
 *
 * @version 1.0
 * @since 2026.10
 */
public final class CsmPreemptEmitter {

  /**
   * The kind of call an emitter makes. A real system tells them apart by the strobe's rate.
   *
   * @since 1.0
   */
  public enum Kind {
    /** An emergency vehicle: calls an emergency preempt. */
    EMERGENCY,
    /** A transit vehicle: reserved for transit signal priority; no detector acts on it yet. */
    TRANSIT
  }

  /** Where the emitter is, in world coordinates. */
  public final double x;
  /** Where the emitter is, in world coordinates. */
  public final double y;
  /** Where the emitter is, in world coordinates. */
  public final double z;
  /** The horizontal direction the vehicle faces, a unit vector (x part). */
  public final double headingX;
  /** The horizontal direction the vehicle faces, a unit vector (z part). */
  public final double headingZ;
  /** The kind of call. */
  public final Kind kind;

  /**
   * Constructs an emitter. The heading is normalised here; a zero heading is kept as zero and
   * is then treated as facing every way.
   *
   * @param x        where the emitter is
   * @param y        where the emitter is
   * @param z        where the emitter is
   * @param headingX the direction the vehicle faces (x part, any length)
   * @param headingZ the direction the vehicle faces (z part, any length)
   * @param kind     the kind of call
   *
   * @since 1.0
   */
  public CsmPreemptEmitter(double x, double y, double z, double headingX, double headingZ,
      Kind kind) {
    this.x = x;
    this.y = y;
    this.z = z;
    double length = Math.sqrt(headingX * headingX + headingZ * headingZ);
    this.headingX = length < 1.0E-6 ? 0.0 : headingX / length;
    this.headingZ = length < 1.0E-6 ? 0.0 : headingZ / length;
    this.kind = kind;
  }
}
