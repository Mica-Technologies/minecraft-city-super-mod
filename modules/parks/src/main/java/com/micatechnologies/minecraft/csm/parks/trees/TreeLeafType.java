package com.micatechnologies.minecraft.csm.parks.trees;

/**
 * How a leaves block arranges its leaf cards ({@link TreeLeavesGeometry}) or, for a palm crown,
 * its fronds ({@link TreePalmGeometry}). The species decides the texture; the type decides the
 * shape.
 *
 * @since 2026.9
 */
public enum TreeLeafType {
  /** Oaks, elm, plane, ginkgo: a full crown of broad leaves, a tuft past every open face. */
  BROADLEAF(1, 8, 12, 0.7, false),
  /** Honey locust, jacaranda, gum: an open sprite; a crown the light gets through. */
  AIRY(1, 8, 12, 0.8, false),
  /** Italian cypress, arborvitae: upright narrow cards; a one-wide column is the whole tree. */
  NEEDLE(2, 7, 14, 0.15, true),
  /** Willow, pepper tree: a curtain of long strands hung from the crown's underside. */
  WEEPING(1, 8, 12, 0.5, false),
  /** Pleached lindens, topiary: clipped flat, a leafy face flush with each open side. */
  CLIPPED(3, 8, 12, 0.4, false),
  /** A fan palm's crown: round fan fronds on short stalks, a tight ball. */
  PALM_FAN(18, 26, 16, false),
  /** A fan palm's crown with the skirt of dead fronds that hangs down its trunk. */
  PALM_FAN_SKIRT(18, 26, 16, true),
  /** A queen or coconut palm's crown: long feather fronds that arch and droop. */
  PALM_FEATHER(12, 30, 9, false);

  /** Cards inside the cell, at most. */
  final int interior;
  /** Card width range, in sixteenths. */
  final double minSize;
  final double maxSize;
  /** Largest tilt from upright, in radians. */
  final double tilt;
  /** Whether cards are tall and narrow rather than square. */
  final boolean upright;

  /** Palm crowns only: fronds, frond length and width in sixteenths, and whether it has a skirt. */
  final boolean palm;
  final int fronds;
  final double frondLength;
  final double frondWidth;
  final boolean skirt;

  TreeLeafType(int interior, double minSize, double maxSize, double tilt, boolean upright) {
    this.interior = interior;
    this.minSize = minSize;
    this.maxSize = maxSize;
    this.tilt = tilt;
    this.upright = upright;
    this.palm = false;
    this.fronds = 0;
    this.frondLength = 0;
    this.frondWidth = 0;
    this.skirt = false;
  }

  TreeLeafType(int fronds, double frondLength, double frondWidth, boolean skirt) {
    this.interior = 0;
    this.minSize = 0;
    this.maxSize = 0;
    this.tilt = 0;
    this.upright = false;
    this.palm = true;
    this.fronds = fronds;
    this.frondLength = frondLength;
    this.frondWidth = frondWidth;
    this.skirt = skirt;
  }

  /** Whether this is a palm crown, drawn by {@link TreePalmGeometry}. */
  public boolean isPalm() {
    return palm;
  }
}
