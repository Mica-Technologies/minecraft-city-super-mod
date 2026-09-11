package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * How visible a lit signal section is from where the viewer stands, for the visor types whose
 * whole purpose is to <em>not</em> be seen from the wrong place.
 *
 * <p>Three visors are angle-sensitive. Vertical louvers hide the lens from anyone off to the
 * side of the head's axis. Horizontal louvers hide it from anyone outside a band of elevation --
 * in practice, from the far approach or the adjacent intersection. A programmable visibility
 * visor (the 3M / McCain optically programmed head) is masked at the factory so the lens is only
 * lit from a programmed patch of road. Every function here returns a factor from 0 (unlit-looking)
 * to 1 (fully lit) that the renderer applies to the visor wash and the lens.</p>
 *
 * <p>All of it is client-side and against the local viewer; the server never asks who can see
 * what. All of it is plain arithmetic on doubles so it can be unit tested, and every tunable is a
 * named constant in one place.</p>
 *
 * <h3>Frames</h3>
 *
 * <p>Positions are in blocks, any consistent origin. The head's rotation is the angle the renderer
 * hands to {@code glRotatef(rot, 0, 1, 0)}: facing plus body tilt, so a head turned by its tilt
 * setting carries its louvers with it. In head-local space the model's front is <b>-Z</b>: azimuth
 * is measured from straight ahead, positive to the head's right; elevation is positive above the
 * lens, so a driver on the road below is at a negative elevation.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public final class SignalVisibility {

  // ---- Vertical louvers -----------------------------------------------------------------------

  /** Azimuth within which a vertical-louvered section is fully lit, degrees either side. */
  public static final double VERTICAL_FULL_DEG = 8.0;

  /** Azimuth beyond which a vertical-louvered section reads unlit, degrees either side. */
  public static final double VERTICAL_ZERO_DEG = 20.0;

  // ---- Horizontal louvers ---------------------------------------------------------------------

  /** Padding added to both ends of a programmed elevation band, degrees. */
  public static final double HORIZONTAL_PAD_DEG = 2.0;

  /** How far outside its band a horizontal-louvered section takes to fade to unlit, degrees. */
  public static final double HORIZONTAL_FADE_DEG = 5.0;

  /**
   * Half-width of the pass band the modelled slats give geometrically: one unit of gap over nine
   * units of depth. Used for the band of a head nobody has programmed.
   */
  public static final double DEFAULT_SLAT_HALF_BAND_DEG = Math.toDegrees(Math.atan(1.0 / 9.0));

  /** Steepest extra slat tilt the geometry tolerates before the slats fall out of the shell. */
  public static final double MAX_SLAT_EXTRA_TILT_DEG = 35.0;

  // ---- Programmable visibility ----------------------------------------------------------------

  /**
   * How high a driver's eye sits above the clicked road surface, blocks. The polygon is programmed
   * on the road; the mask is set against eyes, so the projection plane is raised by this much.
   */
  public static final double DRIVER_EYE_HEIGHT = 1.6;

  /**
   * Angle past the polygon's edge, as seen from the lens, over which the lens fades out. Wide
   * enough that there is a real band of road where a programmed lens is only barely visible,
   * rather than a hard step at the kerb.
   */
  public static final double PROGRAMMABLE_FADE_DEG = 9.0;

  /** Strongest residual glow visible from outside the area, right beside the head. */
  public static final double LEAK_MAX = 0.25;

  /** Distance from the lens at which the residual glow has faded to nothing, blocks. */
  public static final double LEAK_RANGE = 8.0;

  private SignalVisibility() {}

  /** Where the viewer is relative to a lens, in the head's own frame. */
  public static final class Direction {

    /** Degrees from straight ahead, positive to the head's right. */
    public final double azimuthDeg;

    /** Degrees above the lens's horizontal plane; negative below. */
    public final double elevationDeg;

    /** Straight-line distance from lens to eye, blocks. */
    public final double distance;

    /** True when the viewer is behind the face plane, where the housing hides the lens anyway. */
    public final boolean behind;

    Direction(double azimuthDeg, double elevationDeg, double distance, boolean behind) {
      this.azimuthDeg = azimuthDeg;
      this.elevationDeg = elevationDeg;
      this.distance = distance;
      this.behind = behind;
    }
  }

  /**
   * Resolves the viewer's direction from a lens in the head's frame.
   *
   * @param lensX           lens position
   * @param lensY           lens position
   * @param lensZ           lens position
   * @param eyeX            viewer eye position, same origin
   * @param eyeY            viewer eye position
   * @param eyeZ            viewer eye position
   * @param headRotationDeg the head's rotation about +Y as the renderer applies it
   *
   * @return the direction; distance is zero and azimuth undefined-but-finite if eye equals lens
   */
  public static Direction directionFromLens(double lensX, double lensY, double lensZ,
      double eyeX, double eyeY, double eyeZ, double headRotationDeg) {
    double dx = eyeX - lensX;
    double dy = eyeY - lensY;
    double dz = eyeZ - lensZ;
    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

    // Undo the renderer's rotation about +Y. glRotatef(θ, 0, 1, 0) takes a model vector to world
    // as x' = x cos θ + z sin θ, z' = -x sin θ + z cos θ; the inverse is the same with -θ.
    double rad = Math.toRadians(headRotationDeg);
    double cos = Math.cos(rad);
    double sin = Math.sin(rad);
    double localX = dx * cos - dz * sin;
    double localZ = dx * sin + dz * cos;

    // The model faces -Z, so "ahead" is -localZ.
    double ahead = -localZ;
    double azimuth = Math.toDegrees(Math.atan2(localX, ahead));
    double horizontal = Math.sqrt(localX * localX + localZ * localZ);
    double elevation = Math.toDegrees(Math.atan2(dy, horizontal));
    return new Direction(azimuth, elevation, distance, ahead < 0.0);
  }

  /**
   * One while {@code value} is at or below {@code fullUntil}, zero at or beyond {@code zeroAt},
   * linear in between.
   *
   * @param value     the measured quantity
   * @param fullUntil the edge of the fully visible region
   * @param zeroAt    where visibility reaches zero; must exceed {@code fullUntil}
   *
   * @return the factor, 0..1
   */
  public static float fade(double value, double fullUntil, double zeroAt) {
    if (value <= fullUntil) {
      return 1.0f;
    }
    if (value >= zeroAt) {
      return 0.0f;
    }
    return (float) (1.0 - (value - fullUntil) / (zeroAt - fullUntil));
  }

  /**
   * Visibility through vertical louvers: full straight ahead, gone off to the side.
   *
   * @param direction the viewer's direction from the lens
   *
   * @return the factor, 0..1
   */
  public static float verticalLouverFactor(Direction direction) {
    if (direction.behind) {
      return 0.0f;
    }
    return fade(Math.abs(direction.azimuthDeg), VERTICAL_FULL_DEG, VERTICAL_ZERO_DEG);
  }

  /**
   * Visibility through horizontal louvers: full inside the elevation band, fading outside it.
   *
   * @param direction the viewer's direction from the lens
   * @param loDeg     the band's lower (steeper, more negative) edge in elevation degrees
   * @param hiDeg     the band's upper (shallower) edge in elevation degrees
   *
   * @return the factor, 0..1
   */
  public static float horizontalLouverFactor(Direction direction, double loDeg, double hiDeg) {
    if (direction.behind) {
      return 0.0f;
    }
    double e = direction.elevationDeg;
    double outside = Math.max(0.0, Math.max(loDeg - e, e - hiDeg));
    return fade(outside, 0.0, HORIZONTAL_FADE_DEG);
  }

  /**
   * The elevation band of a horizontal louver nobody has programmed: whatever its slats pass
   * geometrically.
   *
   * @param slatAngleBelowHorizontalDeg how far below horizontal the slats point, degrees positive
   *
   * @return {lo, hi} in elevation degrees
   */
  public static double[] defaultHorizontalBand(double slatAngleBelowHorizontalDeg) {
    double centre = -slatAngleBelowHorizontalDeg;
    return new double[]{centre - DEFAULT_SLAT_HALF_BAND_DEG, centre + DEFAULT_SLAT_HALF_BAND_DEG};
  }

  /**
   * The elevation band a programmed area asks a horizontal louver for: from the eye of a driver
   * at the area's farthest vertex (shallow) to one at its nearest (steep), padded a little.
   *
   * @param area  the programmed area
   * @param lensX lens position, same coordinates as the area's points
   * @param lensY lens position
   * @param lensZ lens position
   *
   * @return {lo, hi} in elevation degrees
   */
  public static double[] programmedHorizontalBand(SignalVisibilityArea area,
      double lensX, double lensY, double lensZ) {
    double eyeY = area.surfaceY() + DRIVER_EYE_HEIGHT;
    int near = area.nearestVertex(lensX, lensZ);
    int far = area.farthestVertex(lensX, lensZ);
    double eNear = elevationTo(lensX, lensY, lensZ, area.vertexX(near), eyeY, area.vertexZ(near));
    double eFar = elevationTo(lensX, lensY, lensZ, area.vertexX(far), eyeY, area.vertexZ(far));
    return new double[]{
        Math.min(eNear, eFar) - HORIZONTAL_PAD_DEG,
        Math.max(eNear, eFar) + HORIZONTAL_PAD_DEG};
  }

  /**
   * The extra tilt to give a section's slats so they point at the middle of its band, expressed
   * as the adjustment the renderer adds to the tilt already authored into the slat geometry.
   *
   * @param loDeg               band lower edge, elevation degrees
   * @param hiDeg               band upper edge, elevation degrees
   * @param visorTiltDeg        the visor's own downward tilt, degrees
   * @param builtInExtraTiltDeg the extra tilt authored into the slat boxes, degrees
   *
   * @return degrees to add to the authored extra tilt; the total extra stays within 0 and
   *     {@link #MAX_SLAT_EXTRA_TILT_DEG}
   */
  public static double horizontalSlatExtraTiltAdjust(double loDeg, double hiDeg,
      double visorTiltDeg, double builtInExtraTiltDeg) {
    double centreBelowHorizontal = -(loDeg + hiDeg) / 2.0;
    double desiredExtra = centreBelowHorizontal - visorTiltDeg;
    desiredExtra = Math.max(0.0, Math.min(MAX_SLAT_EXTRA_TILT_DEG, desiredExtra));
    return desiredExtra - builtInExtraTiltDeg;
  }

  /**
   * Visibility of a programmable section: the viewer's eye projected through the lens onto the
   * area's eye plane, lit inside the polygon, fading over a few degrees past its edge, and
   * otherwise showing only the close-range leak.
   *
   * @param area  the programmed area, or {@code null} for an unmasked (unprogrammed) head
   * @param lensX lens position, same coordinates as the area's points
   * @param lensY lens position
   * @param lensZ lens position
   * @param eyeX  viewer eye position
   * @param eyeY  viewer eye position
   * @param eyeZ  viewer eye position
   *
   * @return the factor, 0..1
   */
  public static float programmableFactor(SignalVisibilityArea area,
      double lensX, double lensY, double lensZ, double eyeX, double eyeY, double eyeZ) {
    if (area == null) {
      return 1.0f;
    }
    double dx = eyeX - lensX;
    double dy = eyeY - lensY;
    double dz = eyeZ - lensZ;
    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
    float leak = leakFactor(distance);

    // The mask is set for eyes below the lens. From level with it or above, the optical line
    // never meets the road, so the lens is dark apart from the leak.
    if (dy >= -1.0e-6) {
      return leak;
    }
    double planeY = area.surfaceY() + DRIVER_EYE_HEIGHT;
    double t = (planeY - lensY) / dy;
    if (t <= 0.0) {
      // The plane is above the lens (an area programmed uphill of the head); nothing projects.
      return leak;
    }
    double px = lensX + dx * t;
    double pz = lensZ + dz * t;
    if (area.containsXZ(px, pz)) {
      return 1.0f;
    }
    double[] q = area.nearestBoundaryPoint(px, pz);
    double qx = q[0] - lensX;
    double qy = planeY - lensY;
    double qz = q[1] - lensZ;
    double qLength = Math.sqrt(qx * qx + qy * qy + qz * qz);
    if (distance <= 0.0 || qLength <= 0.0) {
      return leak;
    }
    double cos = (dx * qx + dy * qy + dz * qz) / (distance * qLength);
    cos = Math.max(-1.0, Math.min(1.0, cos));
    double angle = Math.toDegrees(Math.acos(cos));
    return Math.max(fade(angle, 0.0, PROGRAMMABLE_FADE_DEG), leak);
  }

  /**
   * The residual glow a masked lens shows to someone standing right under it.
   *
   * @param distance lens to eye, blocks
   *
   * @return 0..{@link #LEAK_MAX}
   */
  public static float leakFactor(double distance) {
    double t = 1.0 - distance / LEAK_RANGE;
    return (float) (LEAK_MAX * Math.max(0.0, Math.min(1.0, t)));
  }

  private static double elevationTo(double fromX, double fromY, double fromZ,
      double toX, double toY, double toZ) {
    double dx = toX - fromX;
    double dz = toZ - fromZ;
    return Math.toDegrees(Math.atan2(toY - fromY, Math.sqrt(dx * dx + dz * dz)));
  }
}
