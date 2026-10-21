package com.micatechnologies.minecraft.csm.technology.rack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The boxes a rack's units are drawn as, in model pixels, before the rack is turned to its
 * facing.
 *
 * <p>The same rules as {@code unit_boxes} in {@code gen_technology_rack.py}, which writes the
 * pre-filled racks' models, so a filled rack and a pre-filled one look the same. A face is left
 * out where nothing can see it: the back of a unit inside a cabinet, a top or a bottom against
 * the next unit when that one is at least as deep, and the face a box would have at the cell
 * line (the other cell draws the rest of the unit).</p>
 *
 * @since 2026.10
 */
public final class RackUnitBoxes {

  /** Face bits, one a side of the box. */
  public static final int NORTH = 1;
  public static final int SOUTH = 2;
  public static final int EAST = 4;
  public static final int WEST = 8;
  public static final int UP = 16;
  public static final int DOWN = 32;

  private RackUnitBoxes() {
  }

  /** One unit's box in one cell. */
  public static final class Box {

    public final RackUnitType type;
    public final float x0;
    public final float y0;
    public final float z0;
    public final float x1;
    public final float y1;
    public final float z1;
    /** Which faces to draw: the bits above. */
    public final int faces;
    /** The face's uv on the unit sheet, in sprite units (0..16): u0, v0, u1, v1. */
    public final float[] frontUv;
    /** The sides' uv, inside the unit's colour swatch. */
    public final float[] sideUv;

    Box(RackUnitType type, float x0, float y0, float z0, float x1, float y1, float z1, int faces,
        float[] frontUv, float[] sideUv) {
      this.type = type;
      this.x0 = x0;
      this.y0 = y0;
      this.z0 = z0;
      this.x1 = x1;
      this.y1 = y1;
      this.z1 = z1;
      this.faces = faces;
      this.frontUv = frontUv;
      this.sideUv = sideUv;
    }

    public boolean has(int face) {
      return (faces & face) != 0;
    }
  }

  /**
   * The units' boxes in one cell of a rack, moved down to the cell's own 0..16. Units the frame
   * cannot hold (above its top U) are left out.
   *
   * @param frame    the rack
   * @param contents what is mounted
   * @param cell     0 for the lower cell, 1 for the upper
   * @return the boxes
   */
  public static List<Box> boxes(RackFrame frame, RackContents contents, int cell) {
    if (contents.isEmpty()) {
      return Collections.emptyList();
    }
    int n = contents.size();
    float[] y0 = new float[n];
    float[] y1 = new float[n];
    float[] z0 = new float[n];
    float[] z1 = new float[n];
    for (int i = 0; i < n; i++) {
      RackUnitType t = contents.type(i);
      int top = contents.top(i);
      y0[i] = frame.getFirstY() + (top - t.getHeightU()) * RackFrame.PITCH;
      y1[i] = frame.getFirstY() + top * RackFrame.PITCH;
      z0[i] = frame.frontZ(t.getDepth());
      z1[i] = z0[i] + t.getDepth();
    }
    float lo = cell * 16.0f;
    float hi = lo + 16.0f;
    float k = 16.0f / RackUnitType.SHEET_TEXELS;
    List<Box> out = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      RackUnitType t = contents.type(i);
      int top = contents.top(i);
      if (top > frame.getTotalU() || y1[i] <= lo || y0[i] >= hi) {
        continue;
      }
      boolean above = false;
      boolean below = false;
      for (int j = 0; j < n; j++) {
        if (j == i || z0[j] > z0[i] || z1[j] < z1[i]) {
          continue;
        }
        int topJ = contents.top(j);
        if (topJ - contents.type(j).getHeightU() == top) {
          above = true;
        }
        if (topJ == top - t.getHeightU()) {
          below = true;
        }
      }
      float ya = Math.max(y0[i], lo);
      float yb = Math.min(y1[i], hi);
      int faces = NORTH | EAST | WEST;
      if (frame.isOpen()) {
        faces |= SOUTH;
      }
      if (!above && y1[i] <= hi) {
        faces |= UP;
      }
      if (!below && y0[i] >= lo) {
        faces |= DOWN;
      }
      float v0 = t.getBandRow() * k;
      float v1 = (t.getBandRow() + t.getHeightU() * RackUnitType.TEXELS_PER_U) * k;
      float vt = v0 + (y1[i] - yb) / (y1[i] - y0[i]) * (v1 - v0);
      float vb = v0 + (y1[i] - ya) / (y1[i] - y0[i]) * (v1 - v0);
      float su = t.getSwatch() * 32 * k;
      float sv = RackUnitType.SWATCH_ROW * k;
      out.add(new Box(t, RackFrame.UNIT_X0, ya - lo, z0[i], RackFrame.UNIT_X1, yb - lo, z1[i],
          faces, new float[]{0, vt, 16, vb},
          new float[]{su + 0.5f, sv + 0.25f, su + 3.5f, sv + 0.75f}));
    }
    return out;
  }
}
