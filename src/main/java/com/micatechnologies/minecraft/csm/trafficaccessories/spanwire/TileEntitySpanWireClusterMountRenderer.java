package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import java.util.ArrayList;
import java.util.List;
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

  /** The longest a drop will stretch to reach a head that sits lower than the usual stub. */
  private static final double MAX_DROP_LENGTH = 1.5;

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

    final List<TileEntitySpanWireClusterMount.ClusterPayload> payloads = te.getPayloads();
    if (payloads.isEmpty()) {
      // Nothing hanging: draw the bare bracket across the columns it would cover, so an empty
      // cluster still reads as a cluster waiting for heads rather than as a broken mount.
      emitBracket(buffer, te, bracketCentre, along, te.getCoveredColumns(), origin, skyLight,
          blockLight);
      return;
    }

    // Trimmed to what is actually there. A four-wide cluster carrying two heads should not reach
    // two empty columns further along, holding nothing.
    final List<BlockPos> occupied = new ArrayList<>(payloads.size());
    for (TileEntitySpanWireClusterMount.ClusterPayload payload : payloads) {
      occupied.add(payload.getColumn());
    }
    emitBracket(buffer, te, bracketCentre, along, occupied, origin, skyLight, blockLight);

    for (TileEntitySpanWireClusterMount.ClusterPayload payload : payloads) {
      // Each drop leaves the bracket above its own column and lands on its own head. The heads on
      // a cluster need not face the same way -- that is most of the point of one -- and a head's
      // body is set back along the way it faces, so every drop ends somewhere different.
      final Vec3d top = bracketCentre
          .add(along.scale(offsetAlong(te, payload.getColumn(), along)));
      final Vec3d foot = new Vec3d(
          payload.getColumn().getX() + 0.5 + payload.getHardwareOffset().x,
          dropFootY(top.y, payload.getTopY()),
          payload.getColumn().getZ() + 0.5 + payload.getHardwareOffset().z);
      SpanWireCableGeometry.emitStraightTube(buffer, top, foot, origin, DROP_RADIUS,
          STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
    }
  }

  /**
   * How far down a drop reaches: onto the head's roof where it reported one, or a fixed stub.
   *
   * <p>Bounded either way, so a payload answering with something absurd cannot stretch a drop to
   * the ground.
   */
  private static double dropFootY(double bracketY, double payloadTopY) {
    if (Double.isNaN(payloadTopY)) {
      return bracketY - DROP_LENGTH;
    }
    return Math.max(bracketY - MAX_DROP_LENGTH, Math.min(bracketY - DROP_LENGTH, payloadTopY));
  }

  /** The bar itself, spanning the given columns with a little overhang at each end. */
  private void emitBracket(BufferBuilder buffer, TileEntitySpanWireClusterMount te,
      Vec3d bracketCentre, Vec3d along, List<BlockPos> columns, Vec3d origin, int skyLight,
      int blockLight) {
    double lowest = 0.0;
    double highest = 0.0;
    for (BlockPos column : columns) {
      final double offset = offsetAlong(te, column, along);
      lowest = Math.min(lowest, offset);
      highest = Math.max(highest, offset);
    }
    SpanWireCableGeometry.emitStraightTube(buffer,
        bracketCentre.add(along.scale(lowest - BRACKET_OVERHANG)),
        bracketCentre.add(along.scale(highest + BRACKET_OVERHANG)),
        origin, BRACKET_RADIUS, STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
  }

  /** How far along the bracket a covered column sits, in blocks from the mount. */
  private static double offsetAlong(TileEntitySpanWireClusterMount te, BlockPos column,
      Vec3d along) {
    return (column.getX() - te.getPos().getX()) * along.x
        + (column.getZ() - te.getPos().getZ()) * along.z;
  }
}
