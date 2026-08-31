package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Draws a cluster mount: everything a single mount draws, plus the horizontal bracket its heads
 * hang along.
 *
 * <p>The bracket runs at the bottom of the mast, along the cable, with a short drop at each column
 * it reaches. Those drops are what visually attach the heads to the bracket rather than leaving
 * them floating beneath it.
 */
public class TileEntitySpanWireClusterMountRenderer
    extends SpanWireCableRenderer<TileEntitySpanWireClusterMount> {

  private static final double BRACKET_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 1.8;
  private static final double DROP_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 1.4;

  /**
   * How far below the mount's attach point the bracket runs.
   *
   * <p>Has to clear the tops of the heads it carries, which is tighter than it looks: a head one
   * block down reaches 0.5 above its own block, so with the attach point 0.75 up there is only
   * a quarter of a block to work in. At 0.12 the bracket's underside clears a head top by about
   * a pixel. Heads on a cluster take no span rise ({@code SpanWireHangOffset}), without which
   * this clearance does not exist at all.
   */
  private static final double BRACKET_BELOW = 0.12;

  /** How far the drops reach below the bracket, ending just inside the head tops they bolt to. */
  private static final double DROP_LENGTH = 0.18;

  /** How far past the outermost column the bracket runs, so it reads as carrying it. */
  private static final double BRACKET_OVERHANG = 0.4;

  private static final float STEEL_RED = 0.30f;
  private static final float STEEL_GREEN = 0.31f;
  private static final float STEEL_BLUE = 0.33f;

  /**
   * Reused rather than reimplemented, so a cluster gets exactly the same mast, saddle and coils
   * as any other mount, and any later change to those reaches both.
   */
  private final TileEntitySpanWireHangerRenderer singleMount =
      new TileEntitySpanWireHangerRenderer();

  @Override
  protected void emitAttachmentHardware(TileEntitySpanWireClusterMount te, BufferBuilder buffer,
      SpanWireCatenary cable, double atT, Vec3d origin, int skyLight, int blockLight) {
    singleMount.emitAttachmentHardware(te, buffer, cable, atT, origin, skyLight, blockLight);

    if (te.getCableDrop() <= 0.0) {
      return;
    }

    final Vec3d attach = te.getAttachPoint();
    final Vec3d bracketCentre = attach.subtract(0.0, BRACKET_BELOW, 0.0);
    final Vec3d along = te.getBracketDirection();

    double lowest = 0.0;
    double highest = 0.0;
    for (BlockPos column : te.getCoveredColumns()) {
      final double offset = offsetAlong(te, column, along);
      lowest = Math.min(lowest, offset);
      highest = Math.max(highest, offset);
    }

    SpanWireCableGeometry.emitStraightTube(buffer,
        bracketCentre.add(along.scale(lowest - BRACKET_OVERHANG)),
        bracketCentre.add(along.scale(highest + BRACKET_OVERHANG)),
        origin, BRACKET_RADIUS, STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);

    for (BlockPos column : te.getCoveredColumns()) {
      final Vec3d top = bracketCentre.add(along.scale(offsetAlong(te, column, along)));
      SpanWireCableGeometry.emitStraightTube(buffer, top,
          top.subtract(0.0, DROP_LENGTH, 0.0), origin, DROP_RADIUS,
          STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
    }
  }

  /** How far along the bracket a covered column sits, in blocks from the mount. */
  private static double offsetAlong(TileEntitySpanWireClusterMount te, BlockPos column,
      Vec3d along) {
    return (column.getX() - te.getPos().getX()) * along.x
        + (column.getZ() - te.getPos().getZ()) * along.z;
  }
}
