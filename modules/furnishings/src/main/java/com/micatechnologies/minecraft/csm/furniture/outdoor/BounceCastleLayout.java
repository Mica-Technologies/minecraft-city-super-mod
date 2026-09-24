package com.micatechnologies.minecraft.csm.furniture.outdoor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * The shape of the bounce castle, three blocks square and two tall, for the blocks that make it
 * up: its root ({@link BlockBounceCastle}) in the middle of the floor and sixteen parts
 * ({@link BlockBounceCastlePart}) round it, eight on the floor and eight above them. The block
 * over the root is left empty, so there is room to bounce.
 *
 * <p>A part's {@code index} is its place round the root in the world, not turned with the castle:
 * 0 to 7 on the floor, 8 to 15 above, in {@link #RING} order. Its walls are worked out in the
 * castle's own frame, facing north with the doorway at the front ({@code gen_furniture_outdoor.py}
 * draws the castle so): the floor's top is {@link #FLOOR_TOP}; the back and side walls, and the
 * front either side of the doorway, stand three sixteenths thick round the edge up to 1.8 m, with
 * a turret at each corner.</p>
 *
 * @since 2026.9
 */
public final class BounceCastleLayout {

  /** The ring of cells round the root, as {dx, dz} in the world. */
  public static final int[][] RING = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1},
      {0, 1}, {1, 1}};
  /** How many parts a castle has. */
  public static final int PARTS = 16;

  /** The top of the inflated floor, in sixteenths. */
  public static final double FLOOR_TOP = 5;
  /** How thick the walls are, in sixteenths. */
  private static final double WALL = 3;
  /** The top of the walls in the upper block, in sixteenths (1.8 m from the ground). */
  private static final double WALL_TOP_UPPER = 13;
  /** The turret at a corner, out from the corner, in sixteenths. */
  private static final double TURRET = 7.5;

  private BounceCastleLayout() {
  }

  /**
   * Where the part with {@code index} is, from the root.
   *
   * @param root  the root
   * @param index the part's index, 0 to 15
   *
   * @return the part's position
   */
  public static BlockPos partPos(BlockPos root, int index) {
    int[] d = RING[index & 7];
    return root.add(d[0], index >= 8 ? 1 : 0, d[1]);
  }

  /**
   * Where the root of the part with {@code index} at {@code pos} is.
   *
   * @param pos   the part
   * @param index its index
   *
   * @return the root's position
   */
  public static BlockPos rootOf(BlockPos pos, int index) {
    int[] d = RING[index & 7];
    return pos.add(-d[0], index >= 8 ? -1 : 0, -d[1]);
  }

  /**
   * The boxes of the part with {@code index} of a castle facing {@code facing}, within its own
   * block, in blocks.
   *
   * @param index  the part's index
   * @param facing the castle's facing (its doorway's side)
   *
   * @return the boxes; none for the block over the doorway, which is open
   */
  public static List<AxisAlignedBB> boxes(int index, EnumFacing facing) {
    int[] d = RING[index & 7];
    // The world offset turned into the castle's own frame, facing north.
    int mx;
    int mz;
    switch (facing) {
      case SOUTH:
        mx = -d[0];
        mz = -d[1];
        break;
      case EAST:
        mx = d[1];
        mz = -d[0];
        break;
      case WEST:
        mx = -d[1];
        mz = d[0];
        break;
      default:
        mx = d[0];
        mz = d[1];
        break;
    }
    boolean upper = index >= 8;
    double y0 = 0;
    double y1 = upper ? WALL_TOP_UPPER : 16;
    List<double[]> local = new ArrayList<>();
    if (!upper) {
      local.add(new double[]{0, 0, 0, 16, FLOOR_TOP, 16});
    }
    if (mz == 1) {
      local.add(new double[]{0, y0, 16 - WALL, 16, y1, 16});
    }
    if (mx == -1) {
      local.add(new double[]{0, y0, 0, WALL, y1, 16});
    }
    if (mx == 1) {
      local.add(new double[]{16 - WALL, y0, 0, 16, y1, 16});
    }
    if (mz == -1 && mx != 0) {
      local.add(new double[]{0, y0, 0, 16, y1, WALL});
    }
    if (mx != 0 && mz != 0) {
      // The turret, whose tower rises to the top of the upper block.
      double x0 = mx < 0 ? 0 : 16 - TURRET;
      double z0 = mz < 0 ? 0 : 16 - TURRET;
      local.add(new double[]{x0, 0, z0, x0 + TURRET, 16, z0 + TURRET});
    }
    List<AxisAlignedBB> out = new ArrayList<>();
    for (double[] b : local) {
      out.add(turn(b, facing));
    }
    return out;
  }

  /** A box in the castle's frame (facing north), in sixteenths, turned to face {@code facing}. */
  private static AxisAlignedBB turn(double[] b, EnumFacing facing) {
    double[] p = point(b[0], b[2], facing);
    double[] q = point(b[3], b[5], facing);
    return new AxisAlignedBB(Math.min(p[0], q[0]) / 16.0, b[1] / 16.0,
        Math.min(p[1], q[1]) / 16.0, Math.max(p[0], q[0]) / 16.0, b[4] / 16.0,
        Math.max(p[1], q[1]) / 16.0);
  }

  /**
   * A point in a block drawn facing north, turned about the block's middle to face
   * {@code facing}, as a blockstate's y rotation turns the model.
   *
   * @param x      x in sixteenths
   * @param z      z in sixteenths
   * @param facing the facing
   *
   * @return {x, z} in sixteenths
   */
  public static double[] point(double x, double z, EnumFacing facing) {
    switch (facing) {
      case EAST:
        return new double[]{16 - z, x};
      case SOUTH:
        return new double[]{16 - x, 16 - z};
      case WEST:
        return new double[]{z, 16 - x};
      default:
        return new double[]{x, z};
    }
  }

  /**
   * The union of the part's boxes, for picking it out with the cursor: the arch over the doorway
   * for the one with none.
   *
   * @param index  the part's index
   * @param facing the castle's facing
   *
   * @return the box, in blocks
   */
  public static AxisAlignedBB outline(int index, EnumFacing facing) {
    List<AxisAlignedBB> boxes = boxes(index, facing);
    if (boxes.isEmpty()) {
      return turn(new double[]{0, 10, 0, 16, 16, WALL}, facing);
    }
    AxisAlignedBB out = boxes.get(0);
    for (AxisAlignedBB b : boxes) {
      out = out.union(b);
    }
    return out;
  }
}
