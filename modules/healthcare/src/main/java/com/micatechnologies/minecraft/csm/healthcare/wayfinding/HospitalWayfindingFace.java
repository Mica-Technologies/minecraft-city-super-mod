package com.micatechnologies.minecraft.csm.healthcare.wayfinding;

import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingSign.Arrow;
import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingSign.Pictogram;
import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingSign.Scheme;
import java.util.Arrays;
import java.util.Objects;

/**
 * Everything one cell of a hospital wayfinding panel draws beyond its slab: the panel's sign,
 * the cell's place in the panel and the rods it hangs. Immutable, and a value: the baked model
 * keeps the quads it baked for one under it, so a ward of identical signs bakes each cell's share
 * once.
 *
 * @since 2026.10
 */
public final class HospitalWayfindingFace {

  final String line1;
  final String line2;
  final Pictogram pictogram;
  final Arrow arrow;
  final Scheme scheme;
  final boolean doubleSided;
  final int col;
  final int row;
  final int width;
  final int height;
  /** Rods in this cell's column: x from the panel's left edge and drop, in pairs (blocks). */
  final float[] rods;
  private final int hash;

  HospitalWayfindingFace(String line1, String line2, Pictogram pictogram, Arrow arrow,
      Scheme scheme, boolean doubleSided, int col, int row, int width, int height,
      float[] rods) {
    this.line1 = line1;
    this.line2 = line2;
    this.pictogram = pictogram;
    this.arrow = arrow;
    this.scheme = scheme;
    this.doubleSided = doubleSided;
    this.col = col;
    this.row = row;
    this.width = width;
    this.height = height;
    this.rods = rods == null ? new float[0] : rods;
    this.hash = Objects.hash(line1, line2, pictogram, arrow, scheme, doubleSided, col, row, width,
        height) * 31 + Arrays.hashCode(this.rods);
  }

  /** Whether the cell draws any of the sign (a cell outside its panel's rectangle does not). */
  boolean drawsSign() {
    return width > 0 && height > 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof HospitalWayfindingFace)) {
      return false;
    }
    HospitalWayfindingFace f = (HospitalWayfindingFace) o;
    return hash == f.hash && doubleSided == f.doubleSided && col == f.col && row == f.row
        && width == f.width && height == f.height && pictogram == f.pictogram
        && arrow == f.arrow && scheme == f.scheme && line1.equals(f.line1)
        && line2.equals(f.line2) && Arrays.equals(rods, f.rods);
  }

  @Override
  public int hashCode() {
    return hash;
  }
}
