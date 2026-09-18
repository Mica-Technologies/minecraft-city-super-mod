package com.micatechnologies.minecraft.csm.buildingmaterials;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * What is packed into a framed wall's stud bays.
 *
 * <p>A block state rather than a second block per wall type. Insulation multiplies the CATALOGUE
 * if it is a block and only the STATES if it is a property, and the states are free where the
 * catalogue is not: every wall block gains an insulated variant without a registry name, a
 * blockstate, a lang line or a tab registration of its own.</p>
 *
 * <p>One kind of insulation, not a choice of several. Mineral wool was drawn as a third value and
 * taken out again: a second colour of the same mat is detail a builder does not reach for, and it
 * cost every stud wall a third creative stack. Its metadata is read back as {@link #BATT}, so a
 * wall placed while it existed keeps its insulation rather than coming back empty.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public enum FramingInsulation implements IStringSerializable {

  /** Open stud bays. */
  NONE("none"),

  /** Fibreglass batt, the pink one. */
  BATT("batt");

  /** The metadata bits mineral wool was stored under, before it was removed. */
  private static final int REMOVED_MINERAL_BITS = 2;

  private final String name;

  FramingInsulation(String name) {
    this.name = name;
  }

  /**
   * The value for the given metadata bits, falling back to {@link #NONE} rather than throwing.
   *
   * <p>Metadata holds two bits for this and only two of the four values are used, so a saved block
   * can legitimately present a value that is out of range — from a hand-written {@code /setblock},
   * from a wall placed while mineral wool still existed, or from a world saved by a future version
   * that added another. Mineral wool becomes {@link #BATT}; anything else falls back, which keeps
   * it from crashing a chunk load.</p>
   *
   * @param bits the metadata bits
   *
   * @return the matching value, {@link #BATT} for mineral wool, or {@link #NONE}
   *
   * @since 1.0
   */
  public static FramingInsulation fromBits(int bits) {
    if (bits == REMOVED_MINERAL_BITS) {
      return BATT;
    }
    FramingInsulation[] values = values();
    return bits >= 0 && bits < values.length ? values[bits] : NONE;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }
}
