package com.micatechnologies.minecraft.csm.signage;

/**
 * Where the controller -- the block the player placed -- sits along the bottom row of its board,
 * which is also which way the board grows from it. Seen from the front.
 *
 * <p>The ordinal is stored in NBT and sent in packets: append only.</p>
 */
public enum AdBoardAlign {
  LEFT, CENTRE, RIGHT;

  /** The alignment with the given ordinal, or {@link #CENTRE} for anything out of range. */
  public static AdBoardAlign fromOrdinal(int ordinal) {
    AdBoardAlign[] values = values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CENTRE;
  }

  /** The next alignment, for a button that cycles. */
  public AdBoardAlign next() {
    return values()[(ordinal() + 1) % values().length];
  }

  /**
   * Which column of a board {@code width} wide the controller is in, counted from the left.
   * Centred on an even width, the controller is the left of the two middle columns.
   */
  public int controllerColumn(int width) {
    switch (this) {
      case LEFT:
        return 0;
      case RIGHT:
        return width - 1;
      default:
        return (width - 1) / 2;
    }
  }
}
