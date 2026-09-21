package com.micatechnologies.minecraft.csm.signage;

/**
 * What the back of a double-sided board shows. Only a {@link AdBoardKind#isCabinet() cabinet}
 * board has a back to show anything on.
 *
 * <p>The ordinal is stored in NBT and sent in packets: append only.</p>
 */
public enum AdBack {
  /** Plain steel. */
  NONE,
  /** The same ad as the front. */
  SAME,
  /** The ad the front will show next, so the two sides of a rotating board differ. */
  NEXT;

  /** The setting with the given ordinal, or {@link #NONE} for anything out of range. */
  public static AdBack fromOrdinal(int ordinal) {
    AdBack[] values = values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
  }

  /** The next setting, for a button that cycles. */
  public AdBack next() {
    return values()[(ordinal() + 1) % values().length];
  }
}
