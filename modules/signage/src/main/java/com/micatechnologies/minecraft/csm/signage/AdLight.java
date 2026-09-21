package com.micatechnologies.minecraft.csm.signage;

/**
 * Whether a board's face glows. Lit, the ad is drawn at full brightness, as a backlit poster or
 * a screen is; unlit, it takes the light of the world in front of it, as paper does.
 *
 * <p>The ordinal is stored in NBT and sent in packets: append only.</p>
 */
public enum AdLight {
  /** Printed: lit by the world. */
  UNLIT,
  /** Always lit. */
  LIT,
  /** Lit between dusk and dawn, as a photocell would switch it. */
  NIGHT,
  /** Lit while the controller has a redstone signal. */
  REDSTONE;

  /** The light with the given ordinal, or {@link #UNLIT} for anything out of range. */
  public static AdLight fromOrdinal(int ordinal) {
    AdLight[] values = values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : UNLIT;
  }

  /** The next setting, for a button that cycles. */
  public AdLight next() {
    return values()[(ordinal() + 1) % values().length];
  }

  /**
   * Whether the face is lit now.
   *
   * @param sunBrightness the world's sun brightness, 0.2 at night to 1 at noon
   * @param powered       whether the controller has a redstone signal
   */
  public boolean isLit(float sunBrightness, boolean powered) {
    switch (this) {
      case LIT:
        return true;
      case NIGHT:
        return sunBrightness < 0.6F;
      case REDSTONE:
        return powered;
      default:
        return false;
    }
  }
}
