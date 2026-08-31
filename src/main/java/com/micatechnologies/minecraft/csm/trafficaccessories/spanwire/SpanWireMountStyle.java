package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import net.minecraft.util.IStringSerializable;

/**
 * How a wire mount meets the cable, and therefore what happens to the signal it carries.
 *
 * <p>These two are not a preference. A mount sits on the block grid and the cable is a smooth
 * curve, so something has to absorb the difference between them, and there are exactly two things
 * that can: the signal, or the mast. Which one should give depends on the build, which is why
 * this is set per mount rather than once for the mod.
 *
 * <ul>
 *   <li>{@link #FLUSH} — the signal rises to hang just under the cable, and the mast is short.
 *   Signals on a span trace the cable's curve. This is what every mount did before the option
 *   existed, and it is right when the heads on a span are all the same size.</li>
 *   <li>{@link #MAST} — the signal stays where its block puts it and the mast stretches, up to
 *   three blocks, to reach the cable. Right when the heads are <em>not</em> all the same size: a
 *   five-section head beside a three-section one should line up with its neighbour, not with the
 *   wire. Also what a tall or unusually slack span needs.</li>
 * </ul>
 */
public enum SpanWireMountStyle implements IStringSerializable {

  /** Signal rises to the cable; short mast. */
  FLUSH("flush", "Flush to Wire", 1.5),

  /** Signal stays put; the mast stretches to reach the cable. */
  MAST("mast", "Extending Mast", 3.0);

  private final String name;
  private final String friendlyName;
  private final double maximumDrop;

  SpanWireMountStyle(String name, String friendlyName, double maximumDrop) {
    this.name = name;
    this.friendlyName = friendlyName;
    this.maximumDrop = maximumDrop;
  }

  /**
   * The longest run of hardware, in blocks, this style will build between the mount and the
   * cable.
   *
   * <p>The two differ because they fail differently. A flush mount that is far from the cable has
   * dragged its signal a long way from the block a player has to click to break it, so it is
   * held tight. An extending mast is <em>meant</em> to be long — that is the whole point of
   * choosing it — so it is allowed three blocks, which is what the user asked for and about as
   * far as real hardware reaches.
   */
  public double getMaximumDrop() {
    return maximumDrop;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public int toNBT() {
    return ordinal();
  }

  public static SpanWireMountStyle fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return FLUSH;
    }
    return values()[ordinal];
  }

  /** Cycles to the next style, wrapping. */
  public SpanWireMountStyle getNext() {
    return values()[(ordinal() + 1) % values().length];
  }

  @Override
  public String getName() {
    return name;
  }
}
