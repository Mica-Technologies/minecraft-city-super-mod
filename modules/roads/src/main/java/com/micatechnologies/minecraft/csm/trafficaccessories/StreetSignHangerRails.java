package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Finds the pipes of a curved or truss arm over a hanging street name sign, so its hangers can
 * stop at the pipe instead of at a fixed height.
 *
 * <p>A hanging blade's hangers were built for the straight horizontal arm, whose tube is always in
 * the middle of the block above. A mast arm curve's tube is somewhere else in every cell, and a
 * truss arm's bottom chord climbs along the blade, so each hanger needs its own height. The arms
 * describe their pipes ({@link ICurvedArmProfile#getCellRails}); this turns them into the sign's
 * own pixel space, where the renderer looks one up per hanger.
 *
 * <p>The search is a handful of block lookups, so the renderer keeps the answer and asks again
 * about once a second rather than every frame.
 *
 * @since 2026.11
 */
final class StreetSignHangerRails {

  /**
   * No pipes found: every hanger keeps its fixed reach.
   */
  static final float[] NONE = new float[0];

  /**
   * Floats per segment: {@code x0, y0, z0, x1, y1, z1, radius}.
   */
  static final int STRIDE = 7;

  /**
   * How many blocks above the sign an arm may be and still be what the sign hangs from.
   */
  private static final int MAX_BLOCKS_ABOVE = 3;

  /**
   * How far past a pipe's own radius a hanger may stand, sideways, and still grip it.
   */
  private static final float GRIP_TOLERANCE = 1.0F;

  private StreetSignHangerRails() {
  }

  /**
   * Collects the pipes over a sign, in the sign's pixel space (the space its renderer draws in:
   * +X the reader's right, +Y up from the sign block's floor, the blade's thickness about Z 8).
   *
   * @param world       the world
   * @param pos         the sign's position
   * @param rotationDeg the renderer's rotation about Y for the sign's facing, in degrees
   * @param halfWidthPx half the blade's width, in sign pixels
   *
   * @return the segments, {@link #STRIDE} floats each, or {@link #NONE}
   */
  static float[] find(World world, BlockPos pos, float rotationDeg, float halfWidthPx) {
    float rad = (float) Math.toRadians(rotationDeg);
    float c = MathHelper.cos(rad);
    float s = MathHelper.sin(rad);
    int reach = MathHelper.ceil(halfWidthPx / 16.0F);
    float[] out = NONE;
    int count = 0;
    BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
    int lastX = Integer.MIN_VALUE;
    int lastZ = Integer.MIN_VALUE;
    for (int k = -reach; k <= reach; k++) {
      // One block along the blade at a time. The blade runs along the renderer's X, which the
      // rotation carries to (c, -s) in the world.
      int bx = MathHelper.floor(pos.getX() + 0.5F + k * c);
      int bz = MathHelper.floor(pos.getZ() + 0.5F - k * s);
      if (bx == lastX && bz == lastZ) {
        continue;
      }
      lastX = bx;
      lastZ = bz;
      for (int dy = 1; dy <= MAX_BLOCKS_ABOVE; dy++) {
        cell.setPos(bx, pos.getY() + dy, bz);
        IBlockState state = world.getBlockState(cell);
        Block block = state.getBlock();
        if (!(block instanceof BlockTrafficPoleMastArmCurve)) {
          continue;
        }
        float[][] rails = ((BlockTrafficPoleMastArmCurve) block).getWorldRails(world, cell, state);
        if (rails == null) {
          continue;
        }
        if (out.length < (count + rails.length) * STRIDE) {
          float[] grown = new float[Math.max((count + rails.length) * STRIDE, out.length * 2)];
          System.arraycopy(out, 0, grown, 0, count * STRIDE);
          out = grown;
        }
        float ox = bx - pos.getX() - 0.5F;
        float oz = bz - pos.getZ() - 0.5F;
        float oy = 16.0F * dy;
        for (float[] r : rails) {
          int at = count * STRIDE;
          toSign(r[0], r[2], ox, oz, c, s, out, at);
          out[at + 1] = oy + r[1];
          toSign(r[3], r[5], ox, oz, c, s, out, at + 3);
          out[at + 4] = oy + r[4];
          out[at + 6] = r[6];
          count++;
        }
      }
    }
    if (count * STRIDE == out.length) {
      return out;
    }
    float[] exact = new float[count * STRIDE];
    System.arraycopy(out, 0, exact, 0, exact.length);
    return count == 0 ? NONE : exact;
  }

  /**
   * A point of an arm cell, in sixteenths from that cell's corner, into the sign's pixel space:
   * the inverse of the renderer's rotation about the sign block's centre, then its un-mirror.
   */
  private static void toSign(float cellX, float cellZ, float ox, float oz, float c, float s,
      float[] out, int at) {
    float dx = ox + cellX / 16.0F;
    float dz = oz + cellZ / 16.0F;
    float u = c * dx - s * dz;
    float w = s * dx + c * dz;
    out[at] = 8.0F - 16.0F * u;
    out[at + 2] = 8.0F + 16.0F * w;
  }

  /**
   * The lowest pipe over a hanger.
   *
   * @param rails  the segments {@link #find} returned
   * @param x      the hanger's X, in sign pixels
   * @param z      the hanger's Z, in sign pixels
   * @param floorY the lowest height a pipe's underside may be at to count; anything lower is
   *               beside or below the blade, not over it
   * @param result receives {@code {centre height, radius}}
   *
   * @return whether a pipe was found
   */
  static boolean lowestOver(float[] rails, float x, float z, float floorY, float[] result) {
    boolean found = false;
    for (int at = 0; at < rails.length; at += STRIDE) {
      float ax = rails[at];
      float az = rails[at + 2];
      float bx = rails[at + 3];
      float bz = rails[at + 5];
      float lenSq = (bx - ax) * (bx - ax) + (bz - az) * (bz - az);
      float t = lenSq < 1.0E-4F ? 0.0F
          : MathHelper.clamp(((x - ax) * (bx - ax) + (z - az) * (bz - az)) / lenSq, 0.0F, 1.0F);
      float px = ax + t * (bx - ax);
      float pz = az + t * (bz - az);
      float radius = rails[at + 6];
      float reach = radius + GRIP_TOLERANCE;
      if ((px - x) * (px - x) + (pz - z) * (pz - z) > reach * reach) {
        continue;
      }
      float y = rails[at + 1] + t * (rails[at + 4] - rails[at + 1]);
      if (y - radius < floorY) {
        continue;
      }
      if (!found || y - radius < result[0] - result[1]) {
        result[0] = y;
        result[1] = radius;
        found = true;
      }
    }
    return found;
  }
}
