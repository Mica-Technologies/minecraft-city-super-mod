package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * What kind of cable a link draws, which decides its shape and its defaults.
 *
 * <p>The names are the strings the {@code style} NBT key takes, so they are saved in worlds and
 * written by the bridge generators: append, never rename.
 */
public enum BridgeCableStyle {

  /** A cable-stayed bridge's stay: taut and straight, pylon to deck. */
  STAY("stay", 4, "white", 0.0, false),

  /**
   * A suspension bridge's main cable: a parabola through its low point, the shape a cable
   * carrying an evenly spread deck takes (a catenary is the shape of a cable carrying only
   * itself, which a main cable is not). 10% of the span is a typical real sag ratio.
   */
  MAIN("main", 8, "galvanized", 10.0, true),

  /** A suspender or arch hanger: straight, normally vertical, thin. */
  HANGER("hanger", 2, "galvanized", 0.0, false),

  /**
   * A rope or a strand of cable net: a true catenary (it carries only itself), solved by the span
   * wire's {@code SpanWireCatenary}.
   */
  ROPE("rope", 2, "galvanized", 4.0, true);

  private final String name;
  private final int defaultThickness;
  private final String defaultColour;
  private final double defaultSag;
  private final boolean defaultSagPercent;

  BridgeCableStyle(String name, int defaultThickness, String defaultColour, double defaultSag,
      boolean defaultSagPercent) {
    this.name = name;
    this.defaultThickness = defaultThickness;
    this.defaultColour = defaultColour;
    this.defaultSag = defaultSag;
    this.defaultSagPercent = defaultSagPercent;
  }

  /** The NBT name. */
  public String getName() {
    return name;
  }

  /** Thickness in pixels when the link gives none. */
  public int getDefaultThickness() {
    return defaultThickness;
  }

  /** Colour name when the link gives none. */
  public String getDefaultColour() {
    return defaultColour;
  }

  /** Sag when the link gives none, in blocks or percent (see {@link #isDefaultSagPercent()}). */
  public double getDefaultSag() {
    return defaultSag;
  }

  /** Whether {@link #getDefaultSag()} is a percentage of the horizontal span. */
  public boolean isDefaultSagPercent() {
    return defaultSagPercent;
  }

  /** Whether this style hangs (and so takes a sag and hangers at all). */
  public boolean hangs() {
    return this == MAIN || this == ROPE;
  }

  /** Parses an NBT name, case-insensitively, or returns null for an unknown one. */
  @Nullable
  public static BridgeCableStyle byName(@Nullable String name) {
    if (name == null) {
      return null;
    }
    final String wanted = name.trim().toLowerCase(Locale.ROOT);
    for (BridgeCableStyle style : values()) {
      if (style.name.equals(wanted)) {
        return style;
      }
    }
    // Spoken aliases a builder might reach for.
    switch (wanted) {
      case "suspender":
        return HANGER;
      case "net":
      case "cable_net":
        return ROPE;
      case "suspension":
        return MAIN;
      default:
        return null;
    }
  }
}
