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
   * The outline of a strobe lens, which the renderer uses to shape the flash it draws over it.
   *
   * <p>Xenon appliances have a rectangular lens and are the default. The System Sensor L-Series
   * LED devices and the coloured-lens beacons have a circular one, and a square flash sitting on
   * a round lens is obvious the moment it fires.</p>
   */
  enum StrobeLensShape {
    /** A rectangular lens, as on every xenon horn strobe and speaker strobe. */
    RECTANGULAR,
    /** A circular lens, as on the L-Series LED appliances and the beacons. */
    ROUND
  }

  /**
   * Returns the outline of this device's strobe lens. The bounds from
   * {@link #getStrobeLensFrom()} and {@link #getStrobeLensTo()} describe the lens either way; the
   * shape says whether the flash fills those bounds as a rectangle or as the ellipse inscribed in
   * them.
   */
  default StrobeLensShape getStrobeLensShape() {
    return StrobeLensShape.RECTANGULAR;
  }

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

  /**
   * Returns the colour of the rendered flash as {red, green, blue}, each 0-1.
   *
   * <p>Defaults to the deep red of an incandescent strobe when
   * {@link #isRedSlowToggleStrobe()} is set and plain white otherwise, which is what
   * every horn strobe and speaker strobe wants. Coloured-lens devices such as beacons
   * override this so the flash matches the lens.</p>
   */
  default float[] getStrobeColor() {
    return isRedSlowToggleStrobe() ? new float[]{1.0f, 0.15f, 0.1f}
        : new float[]{1.0f, 1.0f, 1.0f};
  }
}
