package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;

/**
 * Finding the span a block hangs from, and how far it has to move to sit on the cable rather than
 * on the block grid.
 *
 * <p>A hanging block can only sit at whole-block heights, so a span of them would step in
 * one-block jumps while the cable behind them ran smooth. The offset closes that: the payload
 * rises by exactly as much as the cable stands above its mount, so the whole assembly — cable,
 * saddle, mast, signal — keeps the same relationship at every point along the span, and the
 * signals trace the curve.
 *
 * <p>This is a lookup, not a decision. The mount has already worked out and cached how far below
 * the cable it sits; all this does is find the mount above a given block and convert. Callers must
 * not call it per frame — see {@code TileEntityTrafficSignalHead.getSpanWireYOffset}, which puts a
 * refresh interval in front of it (the plan's D8).
 *
 * <p>Deliberately knows nothing about the traffic signal or sign packages: the payload calls in,
 * never the other way, so nothing here has to be edited when a new kind of thing becomes hangable.
 */
public final class SpanWireHangOffset {

  /**
   * How far above a payload to look for the mount it hangs from. Three blocks covers one directly
   * under its mount and the taller assemblies that put a backplate or a second head between them.
   *
   * <p>Public because it is also the definition of how tall an assembly can be: a body further
   * from the mount than this does not find it, so it does not take the span's rise, so it is not
   * hanging with the rest. Anything measuring an assembly has to stop where this stops, and
   * reading it is the only way to be sure of that.
   */
  public static final int MAX_SEARCH_UP = 3;

  /** Model units per block, matching the scale the signal head renderer works in. */
  private static final double MODEL_UNITS_PER_BLOCK = 16.0;

  /**
   * How far along its bracket a cluster's mast can be from a head it carries. A cluster is at most
   * four wide and centred on its mast, so never more than two columns.
   */
  private static final int CLUSTER_SEARCH_RADIUS = 2;

  /**
   * Every column a cluster bracket could reach this block from, as x/z offsets.
   *
   * <p>The <b>whole</b> ring, diagonals included. It used to be axis offsets only, on the reasoning
   * that a bracket runs along its span and a span diagonal enough to put a head on a true diagonal
   * from its mast would be crossing block corners anyway. That reasoning was wrong in the one case
   * it mattered: a span running at forty-five degrees steps one across and one along per column,
   * so <em>every</em> head on a diagonal cluster sits on a true diagonal from its mast, and none
   * of them could find it.
   *
   * <p>Generated rather than written out, so the set cannot be short by one entry. Widening it is
   * safe: a cluster is asked whether it actually covers the column before it is accepted, so the
   * search only decides where to look, never what is true.
   */
  private static final int[][] CLUSTER_SEARCH_OFFSETS = buildSearchRing();

  private static int[][] buildSearchRing() {
    final int span = CLUSTER_SEARCH_RADIUS * 2 + 1;
    final int[][] ring = new int[span * span - 1][2];
    int i = 0;
    for (int dx = -CLUSTER_SEARCH_RADIUS; dx <= CLUSTER_SEARCH_RADIUS; dx++) {
      for (int dz = -CLUSTER_SEARCH_RADIUS; dz <= CLUSTER_SEARCH_RADIUS; dz++) {
        if (dx == 0 && dz == 0) {
          continue;
        }
        ring[i][0] = dx;
        ring[i][1] = dz;
        i++;
      }
    }
    return ring;
  }

  private SpanWireHangOffset() {
  }

  /**
   * Whether the block at this position hangs from a span, without working out by how much.
   *
   * <p>For payloads that need to know they are on a span but cannot act on an offset. Signs are
   * the case this was added for: a sign is a plain block model and cannot be shifted a fraction of
   * a block, but it already has a <b>setback</b> mode, and setting that back happens to put it
   * exactly where a hanging head's housing sits.
   *
   * <p>Worth being straight about what that reuse is: setback was built for a sign mounted on a
   * wall or against a pole, not for one hung from a wire. It is borrowed here because the geometry
   * it produces is the geometry a span needs — one shift backwards in the block, into line with
   * the wire and the housings — and borrowing it is far better than adding a second, parallel
   * offset that would then have to be kept in agreement with it forever.
   */
  public static boolean hangsFromSpan(IBlockAccess world, BlockPos pos) {
    return findMount(world, pos) != null;
  }

  /**
   * The offset, in model units, for a payload at the given position, or zero if it does not hang
   * from a span or its mount is set to give instead.
   *
   * <p>Three axes rather than the height alone. A head that only rises still hangs beside the wire
   * whenever its housing is off its block's centre line, which is most of them; the horizontal
   * terms slide it under the clamp so the mast comes straight down.
   *
   * <p>Clamped to the same limit the link tool enforces, so a span left in a state the tool warned
   * about cannot fling a signal an arbitrary distance from the block a player has to click to
   * break it.
   */
  public static Vec3d computeFor(IBlockAccess world, BlockPos pos) {
    final TileEntitySpanWireHanger mount = findMount(world, pos);
    if (mount == null) {
      return Vec3d.ZERO;
    }
    // The mount owns this number -- an extending mast gives nothing because the payload is meant
    // to stay on its own block, a cluster gives nothing because its bracket is rigid and holds
    // everything on it level, and anything else gives its drop up to the style's limit. Asking it
    // rather than repeating the rules here is what keeps this in step with the hardware, which
    // adds the same rise to the payload geometry it is drawn against.
    final Vec3d slide = mount.getPayloadSlide(pos);
    return new Vec3d(slide.x * MODEL_UNITS_PER_BLOCK,
        mount.getPayloadRise() * MODEL_UNITS_PER_BLOCK,
        slide.z * MODEL_UNITS_PER_BLOCK);
  }

  /**
   * The mount a block at this position hangs from, or null.
   *
   * <p>Looks straight up first, then sideways for a cluster bracket. A clustered payload sits
   * <em>beside</em> the mast, so looking straight up finds nothing; the sideways pass checks the
   * columns a bracket could reach from and accepts a cluster only if that cluster agrees it covers
   * this column, so two clusters side by side on one span cannot claim each other's payloads.
   *
   * <p>The sideways pass runs only when the straight-up one failed, which for the overwhelming
   * majority of blocks in a world is a handful of map lookups behind the caller's own refresh
   * interval.
   */
  @Nullable
  static TileEntitySpanWireHanger findMount(IBlockAccess world, BlockPos pos) {
    if (world == null || pos == null) {
      return null;
    }
    for (int up = 1; up <= MAX_SEARCH_UP; up++) {
      final TileEntity above = world.getTileEntity(pos.up(up));
      if (above instanceof TileEntitySpanWireHanger) {
        return (TileEntitySpanWireHanger) above;
      }
    }
    for (int up = 1; up <= MAX_SEARCH_UP; up++) {
      for (int[] offset : CLUSTER_SEARCH_OFFSETS) {
        final TileEntity beside = world.getTileEntity(pos.add(offset[0], up, offset[1]));
        if (beside instanceof TileEntitySpanWireClusterMount
            && ((TileEntitySpanWireClusterMount) beside).covers(pos)) {
          // Everything on one bracket hangs from one mast, so every payload in a cluster takes
          // the cluster's own answer rather than one worked out per payload.
          return (TileEntitySpanWireClusterMount) beside;
        }
      }
    }
    return null;
  }
}
