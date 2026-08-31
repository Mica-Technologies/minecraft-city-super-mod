package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
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
   */
  private static final int MAX_SEARCH_UP = 3;

  /** Model units per block, matching the scale the signal head renderer works in. */
  private static final double MODEL_UNITS_PER_BLOCK = 16.0;

  /**
   * Columns a cluster bracket could reach this block from, as x/z offsets. A cluster is at most
   * four wide, so its mast is never more than two columns away along the bracket. Axis offsets
   * only: a bracket runs along its span, and a span diagonal enough to put a head on a true
   * diagonal from its mast would have the bracket crossing block corners anyway.
   */
  private static final int[][] CLUSTER_SEARCH_OFFSETS = {
      {1, 0}, {-1, 0}, {0, 1}, {0, -1},
      {2, 0}, {-2, 0}, {0, 2}, {0, -2},
  };

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
   * The Y offset, in model units, for a payload at the given position, or zero if it does not hang
   * from a span or its mount is set to give instead.
   *
   * <p>Clamped to the same limit the link tool enforces, so a span left in a state the tool warned
   * about cannot fling a signal an arbitrary distance from the block a player has to click to
   * break it.
   */
  public static float computeFor(IBlockAccess world, BlockPos pos) {
    final TileEntitySpanWireHanger mount = findMount(world, pos);
    if (mount == null) {
      return 0.0f;
    }
    // An extending mast is the choice that the *mast* gives rather than the payload, so the
    // payload stays exactly where its block puts it. That is the whole point of the setting:
    // heads of different sizes on one span line up with each other instead of with the wire.
    if (mount.getMountStyle() == SpanWireMountStyle.MAST) {
      return 0.0f;
    }
    // A cluster bracket is rigid and hangs at a fixed height under its mast, so everything on it
    // sits level with everything else on it -- which is exactly what a real cluster looks like,
    // and is why the heads on one do not each trace the cable's curve. Letting them rise drove
    // their tops up through the bracket that is supposed to be carrying them.
    if (mount instanceof TileEntitySpanWireClusterMount) {
      return 0.0f;
    }
    final double drop = mount.getCableDrop();
    // A mount above its own cable is a placement error the tool reports; the payload stays put
    // rather than being dragged somewhere that hides it.
    if (drop <= 0.0) {
      return 0.0f;
    }
    final double clamped = Math.min(drop, mount.getMountStyle().getMaximumDrop());
    return (float) (clamped * MODEL_UNITS_PER_BLOCK);
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
  private static TileEntitySpanWireHanger findMount(IBlockAccess world, BlockPos pos) {
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
