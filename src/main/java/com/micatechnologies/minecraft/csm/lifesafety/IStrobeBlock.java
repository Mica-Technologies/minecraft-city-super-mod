package com.micatechnologies.minecraft.csm.lifesafety;

/**
 * Marker interface for fire alarm blocks that have a strobe lens and should render
 * the visual flash effect via {@link TileEntityFireAlarmStrobeRenderer} when in alarm.
 *
 * <p>Implementations can override {@link #getStrobeLensFrom()} and {@link #getStrobeLensTo()}
 * to specify the strobe lens position from their 3D model (Element 2 in the model JSON).
 * Coordinates are in the 0-16 model unit system used by Minecraft block models.
 */
public interface IStrobeBlock {

  /**
   * Returns the "from" corner of the strobe lens element in model coordinates (0-16).
   * Extracted from Element 2 of the device's shared model JSON.
   * Default returns a centered position suitable for most wall-mount devices.
   */
  default float[] getStrobeLensFrom() {
    return new float[]{5.0f, 6.0f, 13.0f};
  }

  /**
   * Returns the "to" corner of the strobe lens element in model coordinates (0-16).
   * Extracted from Element 2 of the device's shared model JSON.
   * Default returns a centered position suitable for most wall-mount devices.
   */
  default float[] getStrobeLensTo() {
    return new float[]{11.0f, 11.0f, 14.0f};
  }

  /**
   * Returns whether this strobe uses the older incandescent style: red color with a
   * slow 50% duty cycle on/off toggle (500ms on, 500ms off) instead of the modern
   * white NFPA 72 xenon/LED flash (75ms burst at 1Hz).
   */
  default boolean isRedSlowToggleStrobe() {
    return false;
  }
}
