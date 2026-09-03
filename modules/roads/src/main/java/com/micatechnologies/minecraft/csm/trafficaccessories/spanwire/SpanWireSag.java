package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import net.minecraft.util.IStringSerializable;

/**
 * How much a span's messenger is allowed to droop, as a choice a builder can make.
 *
 * <p>The span stores slack -- cable length as a multiple of the straight line between its anchors
 * -- because that is what the catenary solver wants. Nobody thinks in slack. A builder thinks in
 * how far the middle hangs below the ends, so these presets are named and sized by that, and
 * each carries the slack that produces it. For a hanging cable the two are tied by
 * {@code slack = 1 + (8/3) * sag^2} with sag as a fraction of the span length, which is where
 * every number below comes from, and which is why the standard preset lands on the solver's own
 * default: 5% is the figure real signal spans are strung to.
 *
 * <p>Stored on the span rather than the mount, like the signal side and the tether, because the
 * messenger is one wire from anchor to anchor and can only have one droop.
 */
public enum SpanWireSag implements IStringSerializable {

  /** 2.5% -- pulled up hard, nearly straight; a short span between close poles. */
  TAUT("taut", "Taut (2.5%)", 0.025),

  /** 5% -- what real signal span wire is strung to, and the solver's default. */
  STANDARD("standard", "Standard (5%)", 0.05),

  /** 8% -- visibly relaxed; the look of an older installation. */
  RELAXED("relaxed", "Relaxed (8%)", 0.08),

  /** 12% -- a deep swag; needs the mounts well below the anchors to stay under the cable. */
  LOOSE("loose", "Loose (12%)", 0.12);

  private final String name;
  private final String friendlyName;
  private final double sagRatio;

  SpanWireSag(String name, String friendlyName, double sagRatio) {
    this.name = name;
    this.friendlyName = friendlyName;
    this.sagRatio = sagRatio;
  }

  /** Midspan sag as a fraction of the span's horizontal length. */
  public double getSagRatio() {
    return sagRatio;
  }

  /**
   * The slack that produces this sag, in the units {@link SpanWireCatenary#between} takes.
   *
   * <p>The standard preset returns the solver's own default exactly rather than the formula's
   * answer, so a span strung before this option existed reads back as standard instead of as
   * something a hair off it.
   */
  public double getSlack() {
    return this == STANDARD
        ? SpanWireCatenary.DEFAULT_SLACK
        : 1.0 + (8.0 / 3.0) * sagRatio * sagRatio;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  /** The preset whose slack is nearest to the one given. */
  public static SpanWireSag closestTo(double slack) {
    SpanWireSag best = STANDARD;
    double bestDistance = Double.MAX_VALUE;
    for (SpanWireSag candidate : values()) {
      final double distance = Math.abs(candidate.getSlack() - slack);
      if (distance < bestDistance) {
        bestDistance = distance;
        best = candidate;
      }
    }
    return best;
  }

  /** Cycles to the next preset, wrapping. */
  public SpanWireSag getNext() {
    return values()[(ordinal() + 1) % values().length];
  }

  @Override
  public String getName() {
    return name;
  }
}
