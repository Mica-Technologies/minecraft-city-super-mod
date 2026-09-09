package com.micatechnologies.minecraft.csm.trafficaccessories;

/**
 * The two background colours the MUTCD permits on a warning sign face.
 *
 * <p>Standard sign yellow is the long-standing one. Fluorescent yellow-green was added for the
 * pedestrian, bicycle and school warning series, and the MUTCD lets an agency post either — which
 * is why this is a choice on the sign rather than a fixed colour. The rule is only that a
 * jurisdiction should be consistent about it, and that is a decision for whoever is building the
 * city, not for the block.</p>
 *
 * <p>This is deliberately not {@code TrafficSignalBodyColor}. That palette is the colour a signal
 * <em>housing</em> is painted, and it has sixteen entries because a housing can be almost
 * anything; a regulatory or warning sign face has exactly these two legal choices.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public enum MutcdSignFaceColor {

  /** Standard MUTCD sign yellow. */
  YELLOW("Yellow", 1.0f, 0.80f, 0.0f),

  /**
   * Fluorescent yellow-green. The same value the school zone assembly paints its SCHOOL plaque,
   * so a radar sign and a school zone sign on the same pole match rather than nearly matching.
   */
  FLUORESCENT_YELLOW_GREEN("Fluorescent Yellow-Green", 0.72f, 0.93f, 0.20f);

  private final String friendlyName;
  private final float red;
  private final float green;
  private final float blue;

  MutcdSignFaceColor(String friendlyName, float red, float green, float blue) {
    this.friendlyName = friendlyName;
    this.red = red;
    this.green = green;
    this.blue = blue;
  }

  /**
   * Reads a colour back from its stored ordinal, falling back to the first rather than throwing.
   *
   * @param ordinal the stored ordinal
   *
   * @return the colour
   */
  public static MutcdSignFaceColor fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return YELLOW;
    }
    return values()[ordinal];
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public float getRed() {
    return red;
  }

  public float getGreen() {
    return green;
  }

  public float getBlue() {
    return blue;
  }
}
