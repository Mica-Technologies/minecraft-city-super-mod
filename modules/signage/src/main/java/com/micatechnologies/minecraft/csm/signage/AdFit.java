package com.micatechnologies.minecraft.csm.signage;

/**
 * How an ad image is fitted to a board face whose proportions differ from the image's.
 *
 * <p>The ordinal is stored in a board's NBT: append only.</p>
 */
public enum AdFit {
  /** Fill the face and crop what hangs over, keeping the centre. The default. */
  COVER,
  /** Show the whole image, letterboxed in the ad's background colour. */
  CONTAIN,
  /** Fill the face and distort the image to fit. */
  STRETCH;

  /** The fit with the given ordinal, or {@link #COVER} for anything out of range. */
  public static AdFit fromOrdinal(int ordinal) {
    AdFit[] values = values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : COVER;
  }

  /**
   * Where the image goes on the face, as two rectangles: the part of the face the image covers
   * and the part of the image shown there, both in 0..1.
   *
   * @param faceAspect  the face's width over its height
   * @param imageAspect the image's width over its height
   * @return {@code {faceX0, faceY0, faceX1, faceY1, u0, v0, u1, v1}}
   */
  public double[] place(double faceAspect, double imageAspect) {
    double[] out = {0, 0, 1, 1, 0, 0, 1, 1};
    if (this == STRETCH || faceAspect == imageAspect) {
      return out;
    }
    boolean imageWider = imageAspect > faceAspect;
    if (this == COVER) {
      // Crop the image along whichever axis it is too long in.
      if (imageWider) {
        double keep = faceAspect / imageAspect;
        out[4] = (1 - keep) / 2;
        out[6] = 1 - out[4];
      } else {
        double keep = imageAspect / faceAspect;
        out[5] = (1 - keep) / 2;
        out[7] = 1 - out[5];
      }
    } else {
      // Shrink the image on the face along whichever axis it is too short in.
      if (imageWider) {
        double used = faceAspect / imageAspect;
        out[1] = (1 - used) / 2;
        out[3] = 1 - out[1];
      } else {
        double used = imageAspect / faceAspect;
        out[0] = (1 - used) / 2;
        out[2] = 1 - out[0];
      }
    }
    return out;
  }
}
