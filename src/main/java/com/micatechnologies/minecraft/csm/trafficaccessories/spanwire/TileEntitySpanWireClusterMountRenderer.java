package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

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

  /** How far below the attach point the bracket runs. Shared with the mast that stands on it. */
  private static final double BRACKET_BELOW = TileEntitySpanWireClusterMount.BRACKET_DROP;

  /**
   * The thin bar tying the bottoms of a cluster's heads together.
   *
   * <p>Real clusters have one, and it is what stops two heads bolted to a common bracket swinging
   * independently. Thinner than the top bracket, which carries the weight; this one only holds
   * them in step.
   */
  private static final double TIE_BAR_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 0.9;

  /** How far the drops reach below the bracket, ending just inside the head tops they bolt to. */
  private static final double DROP_LENGTH = 0.18;

  /** The longest a drop will stretch to reach a head that sits lower than the usual stub. */
  private static final double MAX_DROP_LENGTH = 1.5;

  /**
   * How far past the outermost drop the bracket runs.
   *
   * <p>A stub, not a cantilever. It was 0.4 -- most of a half block past the last head at each end
   * -- which read as a bar sized for heads that were not there. Real hardware ends just past the
   * bolt: enough to see the bar is carrying the drop rather than stopping short of it, and no
   * more.
   */
  private static final double BRACKET_OVERHANG = 0.09;

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
    // Grips above the middle of the bracket rather than above the mount block, because the single
    // mount clamps above whatever getHardwareFootPoint answers and a cluster's is the bracket's
    // midpoint. The clamp, saddle and coils follow the mast, so the assembly is one straight drop.
    singleMount.emitAttachmentHardware(te, buffer, cable, atT, origin, skyLight, blockLight);

    if (te.getCableDrop() <= 0.0) {
      return;
    }

    final Vec3d attach = te.getAttachPoint();
    final Vec3d bracketCentre = attach.subtract(0.0, BRACKET_BELOW, 0.0);
    final Vec3d along = te.getBracketDirection();

    // Occupied where anything hangs, covered where nothing does. Asked of the tile entity rather
    // than worked out here, because the mast stands on the middle of this same answer and the two
    // must not disagree.
    emitBracket(buffer, te, bracketCentre, along, te.getBracketColumns(), origin, skyLight,
        blockLight);

    final List<TileEntitySpanWireClusterMount.ClusterPayload> payloads = te.getPayloads();
    if (payloads.isEmpty()) {
      return;
    }

    for (TileEntitySpanWireClusterMount.ClusterPayload payload : payloads) {
      // Each drop leaves the bracket above its own column and lands on its own head. The heads on
      // a cluster need not face the same way -- that is most of the point of one -- and a head's
      // body is set back along the way it faces, so every drop ends somewhere different.
      final Vec3d top = bracketCentre
          .add(along.scale(te.offsetAlongBracket(payload.getColumn(), along)));
      final Vec3d foot = new Vec3d(
          payload.getColumn().getX() + 0.5 + payload.getHardwareOffset().x,
          dropFootY(top.y, payload.getTopY()),
          payload.getColumn().getZ() + 0.5 + payload.getHardwareOffset().z);
      SpanWireCableGeometry.emitStraightTube(buffer, top, foot, origin, DROP_RADIUS,
          STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
    }

    emitTieBar(buffer, te, along, payloads, origin, skyLight, blockLight);
  }

  /**
   * The thin bar across the bottoms of the heads, joining them to each other.
   *
   * <p>Runs between the outermost heads rather than past them: the top bracket overhangs because
   * it has to look like it is carrying them, but this one is a tie between housings and stops at
   * the ones it ties.
   *
   * <p>Drawn at the same height a box span's tether would meet these heads, which is what the
   * payload already answers for the bottom of its housing. Heads on a cluster hang level with each
   * other, so a level bar meets all of them; where they somehow do not, the mean keeps it from
   * favouring one end. A head that reports no such height is left out, and if none report one
   * there is no bar -- a guessed height would put a metal bar through the lenses.
   *
   * <p>Nothing is drawn for a single head. There is nothing to tie it to, and a stub sticking out
   * of one housing is worse than no bar at all.
   */
  private void emitTieBar(BufferBuilder buffer, TileEntitySpanWireClusterMount te, Vec3d along,
      List<TileEntitySpanWireClusterMount.ClusterPayload> payloads, Vec3d origin, int skyLight,
      int blockLight) {
    TileEntitySpanWireClusterMount.ClusterPayload first = null;
    TileEntitySpanWireClusterMount.ClusterPayload last = null;
    double lowestOffset = Double.POSITIVE_INFINITY;
    double highestOffset = Double.NEGATIVE_INFINITY;
    double tieSum = 0.0;
    int tieCount = 0;

    for (TileEntitySpanWireClusterMount.ClusterPayload payload : payloads) {
      if (Double.isNaN(payload.getTieY())) {
        continue;
      }
      final double offset = te.offsetAlongBracket(payload.getColumn(), along);
      if (offset < lowestOffset) {
        lowestOffset = offset;
        first = payload;
      }
      if (offset > highestOffset) {
        highestOffset = offset;
        last = payload;
      }
      tieSum += payload.getTieY();
      tieCount++;
    }

    if (tieCount < 2 || first == last) {
      return;
    }

    final double tieY = tieSum / tieCount;
    SpanWireCableGeometry.emitStraightTube(buffer,
        tieBarEnd(first, tieY), tieBarEnd(last, tieY),
        origin, TIE_BAR_RADIUS, STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
  }

  /** Where the tie bar meets one head: the same point across the housing its drop lands on. */
  private static Vec3d tieBarEnd(TileEntitySpanWireClusterMount.ClusterPayload payload,
      double tieY) {
    return new Vec3d(
        payload.getColumn().getX() + 0.5 + payload.getHardwareOffset().x,
        tieY,
        payload.getColumn().getZ() + 0.5 + payload.getHardwareOffset().z);
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
      final double offset = te.offsetAlongBracket(column, along);
      lowest = Math.min(lowest, offset);
      highest = Math.max(highest, offset);
    }
    SpanWireCableGeometry.emitStraightTube(buffer,
        bracketCentre.add(along.scale(lowest - BRACKET_OVERHANG)),
        bracketCentre.add(along.scale(highest + BRACKET_OVERHANG)),
        origin, BRACKET_RADIUS, STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
  }

}
