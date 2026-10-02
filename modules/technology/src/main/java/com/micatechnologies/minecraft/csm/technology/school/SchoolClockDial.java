package com.micatechnologies.minecraft.csm.technology.school;

import net.minecraft.util.EnumFacing;

/**
 * One dial of a clock whose hands {@link TileEntitySchoolClockRenderer} draws: which way it
 * looks, its middle on its face, its radius, and whether it has a red sweep hand. All in
 * sixteenths, in the model's frame (facing north, the wall at z = 16), so the numbers are the
 * ones {@code gen_technology_school.py} drew the dial at and writes into the tab line.
 *
 * @since 2026.10
 */
public final class SchoolClockDial {

  private final EnumFacing face;
  private final double x;
  private final double y;
  private final double z;
  private final double radius;
  private final boolean sweepHand;

  /**
   * Constructs a dial.
   *
   * @param face      the way the dial looks, in the model's frame (horizontal)
   * @param x         its middle, x
   * @param y         its middle, y
   * @param z         its middle, z (on its face)
   * @param radius    the dial's radius, rim included; the hands are sized from it
   * @param sweepHand whether it has a red sweep hand
   */
  public SchoolClockDial(EnumFacing face, double x, double y, double z, double radius,
      boolean sweepHand) {
    if (face.getAxis() == EnumFacing.Axis.Y) {
      throw new IllegalArgumentException("A dial looks horizontally");
    }
    this.face = face;
    this.x = x;
    this.y = y;
    this.z = z;
    this.radius = radius;
    this.sweepHand = sweepHand;
  }

  public EnumFacing getFace() {
    return face;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getZ() {
    return z;
  }

  public double getRadius() {
    return radius;
  }

  public boolean hasSweepHand() {
    return sweepHand;
  }
}
