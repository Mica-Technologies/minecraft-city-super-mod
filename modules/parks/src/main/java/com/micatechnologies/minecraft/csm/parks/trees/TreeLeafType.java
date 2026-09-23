package com.micatechnologies.minecraft.csm.parks.trees;

/**
 * How a leaves block arranges its leaf cards ({@link TreeLeavesGeometry}). The species decides the
 * texture; the type decides the shape.
 *
 * @since 2026.9
 */
public enum TreeLeafType {
  /** Oaks, elm, plane, ginkgo: a full crown of broad cards, a fringe past every open face. */
  BROADLEAF(6, 3, 8, 12, 0.7, false),
  /** Honey locust, jacaranda: fewer, more open cards; a crown the light gets through. */
  AIRY(4, 2, 8, 12, 0.8, false),
  /** Italian cypress, arborvitae: upright narrow cards; a one-wide column is the whole tree. */
  NEEDLE(10, 4, 7, 14, 0.15, true),
  /** Willow, pepper tree tips: broad cards, and a curtain hanging past the bottom face. */
  WEEPING(5, 3, 8, 12, 0.5, false);

  /** Cards inside the cell. */
  final int interior;
  /** Fringe cards past each open face. */
  final int fringe;
  /** Card width range, in sixteenths. */
  final double minSize;
  final double maxSize;
  /** Largest tilt from upright, in radians. */
  final double tilt;
  /** Whether cards are tall and narrow rather than square. */
  final boolean upright;

  TreeLeafType(int interior, int fringe, double minSize, double maxSize, double tilt,
      boolean upright) {
    this.interior = interior;
    this.fringe = fringe;
    this.minSize = minSize;
    this.maxSize = maxSize;
    this.tilt = tilt;
    this.upright = upright;
  }
}
