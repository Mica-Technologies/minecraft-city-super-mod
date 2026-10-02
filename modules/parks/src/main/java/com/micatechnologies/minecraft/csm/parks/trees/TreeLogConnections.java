package com.micatechnologies.minecraft.csm.parks.trees;

import java.util.Arrays;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * What a tree log sees around it, packed into one {@code long}: the only input its model and its
 * collision boxes need.
 *
 * <p>Bit layout (EnumFacing index order D, U, N, S, W, E for faces):</p>
 * <pre>
 *   0-5    a log on that face
 *   6-17   a log on that edge diagonal ({@link #DIAGONALS} order), counted only when neither of the
 *          two face cells between them is a log: a stepped trunk (- T / T -) is joined through
 *          the shared edge, and a corner already joined through a face is not joined twice
 *   18-23  leaves on that face the log reaches into: always above (the leader into the crown),
 *          otherwise only straight on from a log opposite (a limb into the cluster at its end)
 *   24     solid ground below, and no log: the trunk flares onto it
 *   32-49  each face neighbour's width index, 3 bits a face ({@link TreeLogWidth#getMaskIndex})
 *   50-51  the placed axis (X, Y, Z), for a log with no connections at all
 *   52-59  a log on that corner diagonal ({@link #CORNERS} order), counted only when none of the
 *          six cells between them (three face cells, three edge cells) is a log: a player who
 *          steps a limb on all three axes at once gets one leaning limb, and two logs already
 *          joined through a face or an edge are not joined a second time
 * </pre>
 *
 * <p>Carried to the client model through the block's extended state, never as a listed property:
 * 26 independent booleans would be 67 million block states, all built eagerly by Forge.</p>
 *
 * @since 2026.9
 */
public final class TreeLogConnections {

  /** The twelve edge-diagonal offsets, in mask order. */
  public static final int[][] DIAGONALS = {
      {1, 1, 0}, {1, -1, 0}, {-1, 1, 0}, {-1, -1, 0},
      {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
      {0, 1, 1}, {0, 1, -1}, {0, -1, 1}, {0, -1, -1}};

  /** The eight corner-diagonal offsets, in mask order. */
  public static final int[][] CORNERS = {
      {1, 1, 1}, {1, 1, -1}, {1, -1, 1}, {1, -1, -1},
      {-1, 1, 1}, {-1, 1, -1}, {-1, -1, 1}, {-1, -1, -1}};

  public static final int FACE_LOG_SHIFT = 0;
  public static final int DIAGONAL_SHIFT = 6;
  public static final int FACE_LEAVES_SHIFT = 18;
  public static final int GROUND_BIT = 24;
  public static final int WIDTH_SHIFT = 32;
  public static final int AXIS_SHIFT = 50;
  public static final int CORNER_SHIFT = 52;

  /**
   * The width index ({@link TreeLogWidth#getMaskIndex}) of the log at an offset from the log
   * being masked, or 0 where there is none. Lets the joins be worked out, and tested, without a
   * world.
   */
  @FunctionalInterface
  public interface Widths {

    int at(int dx, int dy, int dz);
  }

  private TreeLogConnections() {
  }

  /**
   * Computes a log's mask from its neighbours.
   *
   * @param world the world (a chunk cache while rendering)
   * @param pos   the log's position
   * @param axis  the log's placed axis
   *
   * @return the mask
   */
  public static long compute(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis) {
    // Each of the 26 cells around is read at most once, and only if a join asks for it.
    int[] seen = new int[27];
    Arrays.fill(seen, -1);
    long mask = ((long) axis.ordinal()) << AXIS_SHIFT;
    mask |= links((dx, dy, dz) -> {
      int k = (dx + 1) * 9 + (dy + 1) * 3 + dz + 1;
      if (seen[k] < 0) {
        seen[k] = logWidthIndex(world.getBlockState(pos.add(dx, dy, dz)));
      }
      return seen[k];
    });
    boolean[] faceLog = new boolean[6];
    for (EnumFacing f : EnumFacing.values()) {
      faceLog[f.getIndex()] = faceLog(mask, f.getIndex());
    }
    for (EnumFacing f : EnumFacing.values()) {
      if (!isLeaves(world.getBlockState(pos.offset(f)).getBlock())) {
        continue;
      }
      if (f == EnumFacing.UP || faceLog[f.getOpposite().getIndex()]) {
        mask |= 1L << (FACE_LEAVES_SHIFT + f.getIndex());
      }
    }
    if (!faceLog[EnumFacing.DOWN.getIndex()]) {
      BlockPos below = pos.down();
      IBlockState s = world.getBlockState(below);
      if (!isLeaves(s.getBlock())
          && s.getBlockFaceShape(world, below, EnumFacing.UP) == BlockFaceShape.SOLID) {
        mask |= 1L << GROUND_BIT;
      }
    }
    return mask;
  }

  /**
   * The two face-adjacent offsets between a cell and its edge-diagonal neighbour.
   *
   * @param d an entry of {@link #DIAGONALS}
   *
   * @return two offsets, each keeping one of {@code d}'s two non-zero components
   */
  public static int[][] betweenCells(int[] d) {
    int[] first = d.clone();
    int[] second = d.clone();
    boolean seen = false;
    for (int k = 0; k < 3; k++) {
      if (d[k] == 0) {
        continue;
      }
      if (!seen) {
        second[k] = 0;
        seen = true;
      } else {
        first[k] = 0;
      }
    }
    return new int[][]{first, second};
  }

  /**
   * The log joins of a mask: the face logs and their widths, the edge diagonals and the corner
   * diagonals, each by its rule (see the class comment). Everything else in a mask (leaves, the
   * ground, the axis) is the caller's.
   *
   * @param widths the logs around, by offset
   *
   * @return the face, width, diagonal and corner bits
   */
  public static long links(Widths widths) {
    long mask = 0L;
    for (EnumFacing f : EnumFacing.values()) {
      int width = widths.at(f.getXOffset(), f.getYOffset(), f.getZOffset());
      if (width > 0) {
        mask |= 1L << (FACE_LOG_SHIFT + f.getIndex());
        mask |= ((long) width) << (WIDTH_SHIFT + 3 * f.getIndex());
      }
    }
    for (int i = 0; i < DIAGONALS.length; i++) {
      int[] d = DIAGONALS[i];
      if (widths.at(d[0], d[1], d[2]) > 0 && noneIsLog(widths, betweenCells(d))) {
        // Joined only through the shared edge: the two face cells between them hold no log.
        mask |= 1L << (DIAGONAL_SHIFT + i);
      }
    }
    for (int i = 0; i < CORNERS.length; i++) {
      int[] c = CORNERS[i];
      if (widths.at(c[0], c[1], c[2]) > 0 && noneIsLog(widths, cornerBetweenCells(c))) {
        // Joined only through the shared corner. A log in any of the six cells between would
        // already join the two through faces and edges, each of which the kit draws.
        mask |= 1L << (CORNER_SHIFT + i);
      }
    }
    return mask;
  }

  private static boolean noneIsLog(Widths widths, int[][] cells) {
    for (int[] c : cells) {
      if (widths.at(c[0], c[1], c[2]) > 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * The six offsets between a cell and its corner-diagonal neighbour: the three face cells (one
   * of {@code c}'s components kept) and the three edge cells (one dropped).
   *
   * @param c an entry of {@link #CORNERS}
   *
   * @return six offsets, the face cells first
   */
  public static int[][] cornerBetweenCells(int[] c) {
    int[][] out = new int[6][];
    for (int k = 0; k < 3; k++) {
      int[] face = new int[3];
      face[k] = c[k];
      out[k] = face;
      int[] edge = c.clone();
      edge[k] = 0;
      out[3 + k] = edge;
    }
    return out;
  }

  /** The width index of a log block (a vanilla log is full width), or 0 for anything else. */
  public static int logWidthIndex(IBlockState state) {
    Block block = state.getBlock();
    if (block instanceof BlockTreeLog) {
      return ((BlockTreeLog) block).getWidth().getMaskIndex();
    }
    if (block instanceof BlockLog) {
      return TreeLogWidth.FULL.getMaskIndex();
    }
    return 0;
  }

  public static boolean isLeaves(Block block) {
    return block instanceof ICsmTreeLeaves || block instanceof BlockLeaves;
  }

  // --- decoding, shared by the geometry and anything testing it ---

  public static boolean faceLog(long mask, int face) {
    return (mask >> (FACE_LOG_SHIFT + face) & 1L) != 0;
  }

  public static boolean diagonal(long mask, int i) {
    return (mask >> (DIAGONAL_SHIFT + i) & 1L) != 0;
  }

  public static boolean corner(long mask, int i) {
    return (mask >> (CORNER_SHIFT + i) & 1L) != 0;
  }

  public static boolean faceLeaves(long mask, int face) {
    return (mask >> (FACE_LEAVES_SHIFT + face) & 1L) != 0;
  }

  public static boolean ground(long mask) {
    return (mask >> GROUND_BIT & 1L) != 0;
  }

  public static int faceWidthIndex(long mask, int face) {
    return (int) (mask >> (WIDTH_SHIFT + 3 * face) & 7L);
  }

  public static EnumFacing.Axis axis(long mask) {
    int i = (int) (mask >> AXIS_SHIFT & 3L);
    return EnumFacing.Axis.values()[Math.min(i, 2)];
  }
}
