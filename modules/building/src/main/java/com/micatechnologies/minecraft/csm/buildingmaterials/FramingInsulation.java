package com.micatechnologies.minecraft.csm.buildingmaterials;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * What is packed into a framed wall's stud bays.
 *
 * <p>A block state rather than three blocks per wall type. Insulation multiplies the CATALOGUE if
 * it is a block and only the STATES if it is a property, and the states are free where the
 * catalogue is not: every wall block gains an insulated variant without a registry name, a
 * blockstate, a lang line or a tab registration of its own.</p>
 *
 * <p>The three values exist from the first wall block onward even though only {@link #NONE} is
 * drawn today. Block states multiply as a product, so a property added to a shipped block
 * multiplies against everything it already has; adding this one later would have been a change to
 * every wall in the catalogue. {@link #BATT} and {@link #MINERAL} render as {@link #NONE} until
 * their models exist.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public enum FramingInsulation implements IStringSerializable {

  /** Open stud bays. */
  NONE("none"),

  /** Fibreglass batt, the pink one. */
  BATT("batt"),

  /** Mineral wool, denser and darker than batt. */
  MINERAL("mineral");

  private final String name;

  FramingInsulation(String name) {
    this.name = name;
  }

  /**
   * The value for the given metadata bits, falling back to {@link #NONE} rather than throwing.
   *
   * <p>Metadata holds two bits for this and only three of the four are used, so a saved block can
   * legitimately present a value that is out of range — from a hand-written {@code /setblock}, or
   * from a world saved by a future version that added a fourth. Falling back keeps that from
   * crashing a chunk load.</p>
   *
   * @param bits the metadata bits
   *
   * @return the matching value, or {@link #NONE}
   *
   * @since 1.0
   */
  public static FramingInsulation fromBits(int bits) {
    FramingInsulation[] values = values();
    return bits >= 0 && bits < values.length ? values[bits] : NONE;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }
}
