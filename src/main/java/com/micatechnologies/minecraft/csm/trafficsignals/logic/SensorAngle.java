package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;

/**
 * Cosmetic aim angle for a {@link AbstractBlockTrafficSignalSensorAngled} detection-camera sensor.
 * The sensor's logical orientation is its cardinal {@code BlockHorizontal.FACING} (used by the
 * controller); this angle only swings the camera body +/-45&deg; about its mount point so it can aim
 * diagonally while staying physically attached to the pole/mast-arm it is mounted on. Set via the
 * sensor configuration GUI, never from placement.
 *
 * <ul>
 *   <li>{@link #NONE} &mdash; aim straight (the base model).</li>
 *   <li>{@link #LEFT} &mdash; swing 45&deg; left (the {@code *_diagl} model).</li>
 *   <li>{@link #RIGHT} &mdash; swing 45&deg; right (the {@code *_diagr} model).</li>
 * </ul>
 *
 * @author Mica Technologies
 */
public enum SensorAngle implements IStringSerializable {
  NONE("none"),
  LEFT("left"),
  RIGHT("right");

  private final String name;

  SensorAngle(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
