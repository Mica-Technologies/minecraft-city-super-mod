package com.micatechnologies.minecraft.csm.parks.trees;

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
 * </pre>
 *
 * <p>Carried to the client model through the block's extended state, never as a listed property:
 * 18 independent booleans would be 262,144 block states, all built eagerly by Forge.</p>
 *
 * @since 2026.9
 */
public final class TreeLogConnections {

  /** The twelve edge-diagonal offsets, in mask order. */
  public static final int[][] DIAGONALS = {
      {1, 1, 0}, {1, -1, 0}, {-1, 1, 0}, {-1, -1, 0},
      {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
      {0, 1, 1}, {0, 1, -1}, {0, -1, 1}, {0, -1, -1}};

  public static final int FACE_LOG_SHIFT = 0;
  public static final int DIAGONAL_SHIFT = 6;
  public static final int FACE_LEAVES_SHIFT = 18;
  public static final int GROUND_BIT = 24;
  public static final int WIDTH_SHIFT = 32;
  public static final int AXIS_SHIFT = 50;

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
    long mask = ((long) axis.ordinal()) << AXIS_SHIFT;
    boolean[] faceLog = new boolean[6];
    for (EnumFacing f : EnumFacing.values()) {
      IBlockState s = world.getBlockState(pos.offset(f));
      int width = logWidthIndex(s);
      if (width > 0) {
        faceLog[f.getIndex()] = true;
        mask |= 1L << (FACE_LOG_SHIFT + f.getIndex());
        mask |= ((long) width) << (WIDTH_SHIFT + 3 * f.getIndex());
      }
    }
    for (int i = 0; i < DIAGONALS.length; i++) {
      int[] d = DIAGONALS[i];
      if (logWidthIndex(world.getBlockState(pos.add(d[0], d[1], d[2]))) == 0) {
        continue;
      }
      // Joined only through the shared edge: the two face cells between them hold no log. They
      // are the diagonal with one or the other of its two non-zero components dropped.
      int[][] between = betweenCells(d);
      BlockPos a = pos.add(between[0][0], between[0][1], between[0][2]);
      BlockPos b = pos.add(between[1][0], between[1][1], between[1][2]);
      if (logWidthIndex(world.getBlockState(a)) == 0 && logWidthIndex(world.getBlockState(b)) == 0) {
        mask |= 1L << (DIAGONAL_SHIFT + i);
      }
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
