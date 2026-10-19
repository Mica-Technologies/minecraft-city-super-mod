package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The background colours a dynamic guide sign, its exit tabs and a dynamic street sign blade can
 * be painted.
 *
 * <p><b>Append only.</b> The ordinal is what is stored ({@code signColor} in a guide sign's or
 * street sign's JSON, {@code color} in an exit tab), so a new colour goes at the end and an
 * existing one is never moved, renamed into another's place or removed.
 *
 * <p>The RGB values are the sign's look under ordinary light rather than a print spec: the
 * MUTCD/FHWA colour, pulled slightly toward the muted tone retroreflective sheeting has in the
 * game's lighting, in the same spirit as the original seven.
 */
public enum GuideSignColor {
  GREEN(0.043f, 0.322f, 0.176f, "Green"),
  BLUE(0.082f, 0.204f, 0.467f, "Blue"),
  BROWN(0.361f, 0.251f, 0.200f, "Brown"),
  YELLOW(0.957f, 0.667f, 0.0f, "Yellow"),
  WHITE(0.922f, 0.922f, 0.902f, "White"),
  BLACK(0.094f, 0.094f, 0.094f, "Black"),
  PURPLE(0.35f, 0.15f, 0.45f, "Purple"),
  // ---- appended 2026-10 (issue #291); ordinals 7..10 ----
  /** MUTCD red (regulatory: STOP, DO NOT ENTER); white legend. */
  RED(0.702f, 0.071f, 0.098f, "Red"),
  /** MUTCD orange (temporary traffic control); black legend. */
  ORANGE(0.937f, 0.420f, 0.020f, "Orange"),
  /** MUTCD fluorescent yellow-green (pedestrian, bicycle, school); black legend. */
  FLUORESCENT_YELLOW_GREEN(0.741f, 0.882f, 0.035f, "Fluorescent Yellow-Green", "Fl. Yel-Grn"),
  /** MUTCD coral (incident management); black legend, as the MUTCD specifies. */
  CORAL(0.949f, 0.522f, 0.475f, "Coral");

  private static final Logger LOGGER = LogManager.getLogger(GuideSignColor.class);

  /** Set once an out-of-range index has been reported, so a sign read every frame logs once. */
  private static final AtomicBoolean OUT_OF_RANGE_LOGGED = new AtomicBoolean(false);

  /** What an out-of-range or unknown index reads as. */
  public static final GuideSignColor DEFAULT = GREEN;

  private final float red;
  private final float green;
  private final float blue;
  private final String friendlyName;
  private final String shortName;

  GuideSignColor(float red, float green, float blue, String friendlyName) {
    this(red, green, blue, friendlyName, friendlyName);
  }

  GuideSignColor(float red, float green, float blue, String friendlyName, String shortName) {
    this.red = red;
    this.green = green;
    this.blue = blue;
    this.friendlyName = friendlyName;
    this.shortName = shortName;
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

  public String getFriendlyName() {
    return friendlyName;
  }

  /** A name short enough for a narrow GUI button; the friendly name unless that is long. */
  public String getShortName() {
    return shortName;
  }

  /**
   * Whether this background is light enough that legend (text, borders,
   * dividers) drawn over it must be dark to stay readable. Follows the MUTCD's legend colours:
   * black on yellow, white, orange, fluorescent yellow-green and coral; white on the rest.
   */
  public boolean isLight() {
    return this == YELLOW || this == WHITE || this == ORANGE || this == FLUORESCENT_YELLOW_GREEN
        || this == CORAL;
  }

  public int toNBT() {
    return ordinal();
  }

  public GuideSignColor next() {
    GuideSignColor[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  /** Whether {@code value} is the ordinal of a colour. */
  public static boolean isValid(int value) {
    return value >= 0 && value < values().length;
  }

  /**
   * The colour stored as {@code value}, or {@link #DEFAULT} when no colour has that index (a
   * hand-edited document, or one written by a newer version). The first out-of-range index
   * seen is logged; later ones are not, since this runs in the renderer.
   */
  public static GuideSignColor fromNBT(int value) {
    if (!isValid(value)) {
      reportOutOfRange(value);
      return DEFAULT;
    }
    return values()[value];
  }

  /**
   * {@code value} when it is a colour's ordinal, otherwise {@link #DEFAULT}'s, logged as
   * {@link #fromNBT(int)} logs. For normalising a document as it is read, so the stored value
   * itself is repaired and a tool that wrote it sees the change.
   */
  public static int sanitize(int value) {
    return fromNBT(value).ordinal();
  }

  private static void reportOutOfRange(int value) {
    if (OUT_OF_RANGE_LOGGED.compareAndSet(false, true)) {
      LOGGER.warn("Sign colour index {} is out of range (0..{}); using {}. Further out-of-range "
          + "colour indices are not logged.", value, values().length - 1, DEFAULT);
    }
  }
}
