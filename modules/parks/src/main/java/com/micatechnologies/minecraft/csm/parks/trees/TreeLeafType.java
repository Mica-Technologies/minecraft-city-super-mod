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
  PALM_FEATHER(12, 30, 9, false),
  /**
   * A cabbage palm's (sabal's) crown: a dense round head of big costapalmate fans in three tiers,
   * the lowest drooping, on a boot sized for its stouter trunk.
   */
  PALM_CABBAGE(33, 48, 26, false, 4.4, 3),
  /** A cabbage palm's crown with the dead fronds still hanging under it. */
  PALM_CABBAGE_SKIRT(33, 48, 26, true, 4.4, 3),
  /**
   * A Canary Island date palm's crown: a huge, dense round head of long, stiff feather fronds in
   * three tiers, on the "pineapple", a knob of trimmed leaf bases wider than the trunk.
   */
  PALM_CANARY(72, 72, 19, false, 7.0, 3),
  /**
   * A coconut palm's crown: long feather fronds that droop hard, a dead one or two hanging, and a
   * cluster of coconuts under the crown.
   */
  PALM_COCONUT(24, 60, 13, false, 2.6, 3),
  /**
   * A king palm's crown: a smooth green crownshaft standing on the trunk, and a few long feather
   * fronds arching from its top.
   */
  PALM_KING(14, 60, 12, false, 3.0, 2),
  /**
   * A Joshua tree's rosette: stiff dagger leaves radiating every way from the end of a branch,
   * the old ones turned down over it. Drawn by the palm crown machinery, one on each branch end.
   */
  ROSETTE(36, 17, 7, true, 2.6, 0);

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
  /** Palm crowns only: the boot's radius at its foot, in sixteenths, and the tiers of fronds. */
  final double bootRadius;
  final int tiers;

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
    this.bootRadius = 0;
    this.tiers = 0;
  }

  TreeLeafType(int fronds, double frondLength, double frondWidth, boolean skirt) {
    this(fronds, frondLength, frondWidth, skirt, 2.4, 2);
  }

  TreeLeafType(int fronds, double frondLength, double frondWidth, boolean skirt,
      double bootRadius, int tiers) {
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
    this.bootRadius = bootRadius;
    this.tiers = tiers;
  }

  /** Whether this is a palm crown, drawn by {@link TreePalmGeometry}. */
  public boolean isPalm() {
    return palm;
  }
}
