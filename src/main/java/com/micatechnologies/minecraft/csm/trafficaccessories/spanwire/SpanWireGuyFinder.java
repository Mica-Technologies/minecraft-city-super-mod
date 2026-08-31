package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Bookkeeping for the optional back-guy between a span anchor and a ground anchor.
 *
 * <p>This class used to <em>find</em> the pairing, by searching the blocks under an anchor and
 * running a guy to whatever it found. That was wrong, and not subtly: real signal spans are hung
 * pole to pole, and a guy is the exception rather than the rule -- so a guy that appeared on its
 * own the moment a ground anchor happened to be nearby put a wire on almost every installation
 * that should not have had one. The pairing is now made explicitly with the span wire tool, and
 * nothing here creates one.
 *
 * <p>What is left is the other half: when a ground anchor is broken, the anchors guyed to it have
 * to stop drawing a wire to a block that is no longer there.
 */
public final class SpanWireGuyFinder {

  /**
   * How far from a span anchor a ground anchor may be to be guyed to it.
   *
   * <p>This is a <b>validation</b> bound, not a search: the tool refuses a pairing outside it.
   * That is what makes the cleanup below provably complete -- an anchor guyed to this position
   * can only be inside this box, so a bounded sweep cannot miss one.
   */
  private static final int MAX_GUY_RADIUS = 4;
  private static final int MAX_GUY_DEPTH = 20;

  private SpanWireGuyFinder() {
  }

  /** Whether a ground anchor at this position is close enough below an anchor to be guyed to it. */
  public static boolean isInGuyRange(BlockPos anchor, BlockPos guyAnchor) {
    final int dy = anchor.getY() - guyAnchor.getY();
    return dy > 0
        && dy <= MAX_GUY_DEPTH
        && Math.abs(anchor.getX() - guyAnchor.getX()) <= MAX_GUY_RADIUS
        && Math.abs(anchor.getZ() - guyAnchor.getZ()) <= MAX_GUY_RADIUS;
  }

  /** A short description of the limit, for the message shown when a pairing is refused. */
  public static String describeRange() {
    return "within " + MAX_GUY_RADIUS + " blocks horizontally and " + MAX_GUY_DEPTH + " below";
  }

  /**
   * Drops the guy from every anchor guyed to this position.
   *
   * <p>Called when a ground anchor is broken. The sweep is the inverse of {@link #isInGuyRange}:
   * an anchor that could be guyed to this position is exactly an anchor above it inside the same
   * box, so one definition of "in range" serves both and they cannot drift apart.
   */
  public static void clearGuysTo(World world, BlockPos guyAnchor) {
    if (world.isRemote) {
      return;
    }
    for (int dy = 1; dy <= MAX_GUY_DEPTH; dy++) {
      for (int dx = -MAX_GUY_RADIUS; dx <= MAX_GUY_RADIUS; dx++) {
        for (int dz = -MAX_GUY_RADIUS; dz <= MAX_GUY_RADIUS; dz++) {
          final BlockPos candidate = guyAnchor.add(dx, dy, dz);
          if (!world.isBlockLoaded(candidate)) {
            continue;
          }
          if (world.getTileEntity(candidate) instanceof TileEntitySpanWireAnchor) {
            ((TileEntitySpanWireAnchor) world.getTileEntity(candidate))
                .clearGuyAnchorIf(guyAnchor);
          }
        }
      }
    }
  }
}
