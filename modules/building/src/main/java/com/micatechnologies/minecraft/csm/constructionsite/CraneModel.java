package com.micatechnologies.minecraft.csm.constructionsite;

/**
 * The tower crane models a crane head can draw.
 *
 * <p>Saved by ordinal in the head's tile entity: append only.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public enum CraneModel {

  /** A long lattice jib straight out from the slewing unit, with no tower head above it. */
  FLAT_TOP,

  /** A jib and counter-jib held by pendant lines from an A-frame tower head. */
  HAMMERHEAD,

  /** A raked jib pinned at the slewing unit and luffed up and down by ropes from an A-frame. */
  LUFFING;

  /**
   * The model for a stored ordinal, falling back to the flat-top rather than throwing.
   *
   * @param ordinal the stored ordinal
   *
   * @return the model
   *
   * @since 1.0
   */
  public static CraneModel fromOrdinal(int ordinal) {
    CraneModel[] values = values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FLAT_TOP;
  }
}
