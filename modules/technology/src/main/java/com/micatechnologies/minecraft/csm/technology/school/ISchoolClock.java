package com.micatechnologies.minecraft.csm.technology.school;

/**
 * A block whose dials {@link TileEntitySchoolClockRenderer} draws hands on. Asked by metadata
 * rather than state so the renderer reads nothing from the world per frame: the tile entity
 * caches its block and metadata.
 *
 * @since 2026.10
 */
public interface ISchoolClock {

  /**
   * The dials of the block with this metadata, in the model's frame, or an empty array for a
   * state with none (the speaker half of a clock/speaker panel).
   *
   * @param meta the block's metadata
   *
   * @return its dials
   */
  SchoolClockDial[] getClockDials(int meta);
}
