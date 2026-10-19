package com.micatechnologies.minecraft.csm.healthcare.clinical;

/**
 * A head hung from the ceiling on a drop column ({@link CeilingHung}).
 *
 * @since 2026.10
 */
public interface ICeilingHung {

  /**
   * How high the head hangs when placed by hand on a ceiling: the cells between the floor's top
   * and the head (0 = in the cell standing on the floor).
   *
   * @return the lift in cells
   */
  int getLift();
}
