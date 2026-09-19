package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.buildingmaterials.BlockBuildingDoor.Half;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockBuildingDoor.Hinge;
import com.micatechnologies.minecraft.csm.buildingmaterials.CustomDoorSettings.Movement;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom door's shape and how each movement moves it, in pixels, with the inside to the north and
 * the leaf on the outside face (z 14.25..16) as the fixed doors hang. Shared by the baked model (the
 * closed and open poses), the moving-door renderer (the poses in between) and the block's boxes, so
 * they cannot disagree.
 *
 * <p>The leaf is a frame of stiles and rails in the frame material around a panel, a little thinner
 * and set in, in the upper or lower material. The right-hinged door is the left-hinged one's
 * mirror.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class CustomDoorGeometry {

  private CustomDoorGeometry() {
  }

  /**
   * Which of the door's three materials a box wears.
   *
   * @since 1.0
   */
  public enum Part {
    FRAME, UPPER, LOWER
  }

  /**
   * An axis-aligned box in pixels, and the material it wears.
   *
   * @since 1.0
   */
  public static final class Box {

    public final float x0;
    public final float y0;
    public final float z0;
    public final float x1;
    public final float y1;
    public final float z1;
    public final Part part;

    Box(float x0, float y0, float z0, float x1, float y1, float z1, Part part) {
      this.x0 = Math.min(x0, x1);
      this.y0 = Math.min(y0, y1);
      this.z0 = Math.min(z0, z1);
      this.x1 = Math.max(x0, x1);
      this.y1 = Math.max(y0, y1);
      this.z1 = Math.max(z0, z1);
      this.part = part;
    }

    Box mirror() {
      return new Box(16 - x1, y0, z0, 16 - x0, y1, z1, part);
    }

    Box move(float dx, float dy, float dz) {
      return new Box(x0 + dx, y0 + dy, z0 + dz, x1 + dx, y1 + dy, z1 + dz, part);
    }

    /** A quarter turn about the left hinge's pivot: (x, z) to (z - 14.25, 16 - x). */
    Box swungLeft() {
      return new Box(z0 - Z0, y0, 16 - x1, z1 - Z0, y1, 16 - x0, part);
    }
  }

  /** The leaf's faces, and the pivot a left-hinged leaf swings about. SHARED with gen_doors. */
  static final float Z0 = 14.25F;
  static final float Z1 = 16F;
  public static final float PIVOT_X = 0.875F;
  public static final float PIVOT_Z = 15.125F;

  /**
   * The closed leaf's half, hinged left or right.
   *
   * @param half  which half
   * @param hinge which side the hinge is on
   *
   * @return its boxes
   *
   * @since 1.0
   */
  public static List<Box> closed(Movement movement, Half half, Hinge hinge) {
    List<Box> out = closed(half, hinge);
    if (movement == Movement.SWING) {
      return out;
    }
    List<Box> centred = new ArrayList<>();
    for (Box b : out) {
      centred.add(b.move(0, 0, CENTRE));
    }
    return centred;
  }

  /**
   * How far a door that slides, lifts or splits is set in from the outside face: to the middle of
   * the wall, as a glass pane stands (the user's suggestion, 2026-09-18). It then travels along the
   * middle of the wall and ends up wholly inside the block beside or above it, sharing no face with
   * it. A swinging door stays on the outside face, as a vanilla door hangs, since swung from the
   * middle it would sweep half a block out of its own cell.
   */
  static final float CENTRE = 8F - (Z0 + Z1) / 2;

  public static List<Box> closed(Half half, Hinge hinge) {
    List<Box> out = new ArrayList<>();
    out.add(new Box(0, 0, Z0, 2, 16, Z1, Part.FRAME));
    out.add(new Box(14, 0, Z0, 16, 16, Z1, Part.FRAME));
    if (half == Half.LOWER) {
      out.add(new Box(2, 0, Z0, 14, 2, Z1, Part.FRAME));
      out.add(new Box(2, 2, 14.75F, 14, 16, 15.5F, Part.LOWER));
      // A knob on each face at the latch side.
      out.add(new Box(12, 13, Z0 - 1.25F, 13.5F, 14.5F, Z0, Part.FRAME));
      out.add(new Box(12, 13, Z1, 13.5F, 14.5F, Z1 + 1.25F, Part.FRAME));
    } else {
      out.add(new Box(2, 14, Z0, 14, 16, Z1, Part.FRAME));
      out.add(new Box(2, 0, 14.75F, 14, 14, 15.5F, Part.UPPER));
    }
    if (hinge == Hinge.RIGHT) {
      List<Box> m = new ArrayList<>();
      for (Box b : out) {
        m.add(b.mirror());
      }
      return m;
    }
    return out;
  }

  /**
   * How far a leaf slides to be out of the way: half a pixel short of a whole block. Its edge then
   * shows in the jamb, as a pocket door's pull does, instead of ending exactly in the jamb's plane,
   * where the two would fight over which is drawn.
   */
  static final float TRAVEL = 15.5F;

  /**
   * How far a sliding movement carries a half when fully open, in pixels: {dx, dy, dz}.
   *
   * @param movement the movement
   * @param half     which half
   * @param hinge    which side the hinge is on
   * @param paired   whether the door is one of a pair
   *
   * @return the offset, or null for a swinging door
   *
   * @since 1.0
   */
  public static float[] slide(Movement movement, Half half, Hinge hinge, boolean paired) {
    switch (movement) {
      case SLIDE:
        // Toward the hinge side, into the wall: a pair parts in the middle.
        return new float[]{hinge == Hinge.LEFT ? -TRAVEL : TRAVEL, 0, 0};
      case SLIDE_TOGETHER:
        // Both leaves to the left; the right one of a pair goes twice as far, and further in, to
        // stack beside the other in the one pocket.
        if (hinge == Hinge.RIGHT) {
          return new float[]{paired ? -TRAVEL - 16 : -TRAVEL, 0, -2.5F};
        }
        return new float[]{-TRAVEL, 0, 0};
      case SLIDE_UP:
        return new float[]{0, TRAVEL + 16, 0};
      case SPLIT:
        return new float[]{0, half == Half.UPPER ? TRAVEL : -TRAVEL, 0};
      default:
        return null;
    }
  }

  /**
   * The open leaf's half.
   *
   * @since 1.0
   */
  public static List<Box> open(Movement movement, Half half, Hinge hinge, boolean paired) {
    List<Box> out = new ArrayList<>();
    float[] d = slide(movement, half, hinge, paired);
    if (d != null) {
      for (Box b : closed(movement, half, hinge)) {
        out.add(b.move(d[0], d[1], d[2]));
      }
      return out;
    }
    for (Box b : closed(half, Hinge.LEFT)) {
      Box s = b.swungLeft();
      out.add(hinge == Hinge.RIGHT ? s.mirror() : s);
    }
    return out;
  }
}
