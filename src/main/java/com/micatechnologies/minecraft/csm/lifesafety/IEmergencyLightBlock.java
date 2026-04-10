package com.micatechnologies.minecraft.csm.lifesafety;

/**
 * Interface for emergency light blocks that have light bulb elements which should render
 * a glow/halo effect via {@link TileEntityEmergencyLightRenderer} when active (no power).
 *
 * <p>Emergency lights have two directional bulbs that face forward. Each bulb's position
 * is specified in the 0-16 model coordinate system matching the Blockbench model.
 */
public interface IEmergencyLightBlock {

  /**
   * Returns the "from" corner of the left bulb element in model coordinates (0-16).
   */
  default float[] getLeftBulbFrom() {
    return new float[]{1.0f, 4.0f, 13.0f};
  }

  /**
   * Returns the "to" corner of the left bulb element in model coordinates (0-16).
   */
  default float[] getLeftBulbTo() {
    return new float[]{5.0f, 8.0f, 15.0f};
  }

  /**
   * Returns the "from" corner of the right bulb element in model coordinates (0-16).
   */
  default float[] getRightBulbFrom() {
    return new float[]{11.0f, 4.0f, 13.0f};
  }

  /**
   * Returns the "to" corner of the right bulb element in model coordinates (0-16).
   */
  default float[] getRightBulbTo() {
    return new float[]{15.0f, 8.0f, 15.0f};
  }
}
