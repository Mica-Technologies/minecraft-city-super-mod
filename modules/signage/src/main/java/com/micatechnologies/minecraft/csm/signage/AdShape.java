package com.micatechnologies.minecraft.csm.signage;

import java.util.Collection;
import javax.annotation.Nullable;

/**
 * The shapes an ad is drawn in. A board picks the one nearest its own proportions, so a 2 x 3
 * kiosk and a 40 x 8 billboard each get a layout made for them rather than a crop of one master.
 *
 * <p>The names are the ones {@code gen_ads.py} writes into the ad index and the texture file
 * names; they are never renamed.</p>
 */
public enum AdShape {
  PORTRAIT("portrait", 2, 3),
  SQUARE("square", 1, 1),
  POSTER("poster", 2, 1),
  BULLETIN("bulletin", 7, 2);

  private final String name;
  private final double aspect;

  AdShape(String name, int width, int height) {
    this.name = name;
    this.aspect = (double) width / height;
  }

  /** The name used in the ad index and in texture file names. */
  public String getName() {
    return name;
  }

  /** Width over height. */
  public double getAspect() {
    return aspect;
  }

  /** The shape with the given index name, or {@code null}. */
  @Nullable
  public static AdShape fromName(String name) {
    for (AdShape shape : values()) {
      if (shape.name.equals(name)) {
        return shape;
      }
    }
    return null;
  }

  /**
   * The shape among {@code available} whose proportions are nearest a face {@code width} by
   * {@code height}. Nearness is measured on the logarithm of the aspect, so a board twice as wide
   * as a shape is as far from it as one half as wide.
   *
   * @return the nearest shape, or {@code null} if {@code available} is empty
   */
  @Nullable
  public static AdShape nearest(double width, double height, Collection<AdShape> available) {
    double target = Math.log(width / height);
    AdShape best = null;
    double bestDistance = Double.MAX_VALUE;
    for (AdShape shape : values()) {
      if (!available.contains(shape)) {
        continue;
      }
      double distance = Math.abs(Math.log(shape.aspect) - target);
      if (distance < bestDistance) {
        best = shape;
        bestDistance = distance;
      }
    }
    return best;
  }
}
