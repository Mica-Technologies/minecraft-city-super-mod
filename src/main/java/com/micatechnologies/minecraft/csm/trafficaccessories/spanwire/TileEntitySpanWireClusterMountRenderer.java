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
      new TileEntitySpanWireHangerRenderer() {
        @Override
        protected boolean drawsTetherTie() {
          // A cluster has one mast and several heads. Borrowing the mount's tie gave one tie at
          // the bracket's midpoint -- standing in the gap between two housings, holding neither,
          // while both heads floated over the tether unattached. Ties are drawn per head below.
          return false;
        }
      };

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

    final List<TileEntitySpanWireClusterMount.ClusterPayload> payloads = te.getPayloads();
    if (payloads.isEmpty()) {
      // Nothing hanging: the bare bar across the columns it would cover, so an empty cluster still
      // reads as one waiting for heads rather than as a broken mount.
      emitEmptyBracket(buffer, te, bracketCentre, along, origin, skyLight, blockLight);
      return;
    }

    // The bar spans the points the heads actually hang from, which is where the drops leave it.
    double lowest = Double.POSITIVE_INFINITY;
    double highest = Double.NEGATIVE_INFINITY;
    for (TileEntitySpanWireClusterMount.ClusterPayload payload : payloads) {
      final double distance = te.alongFor(payload);
      lowest = Math.min(lowest, distance);
      highest = Math.max(highest, distance);
    }
    SpanWireCableGeometry.emitStraightTube(buffer,
        bracketCentre.add(along.scale(lowest - BRACKET_OVERHANG)),
        bracketCentre.add(along.scale(highest + BRACKET_OVERHANG)),
        origin, BRACKET_RADIUS, STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);

    for (TileEntitySpanWireClusterMount.ClusterPayload payload : payloads) {
      // Straight down. The head has been slid onto the bracket line by SpanWireHangOffset, using
      // this same projection, so the point the drop leaves the bar is directly above the housing it
      // lands on. It used to leave the bar above the head's block and then lean out to the housing,
      // which on a head facing along the bar meant a drop that passed over the housing and bent
      // back to it.
      final Vec3d hanging = te.bracketPointFor(payload);
      final Vec3d top = new Vec3d(hanging.x, bracketCentre.y, hanging.z);
      final Vec3d foot = new Vec3d(top.x, dropFootY(top.y, payload.getTopY()), top.z);
      SpanWireCableGeometry.emitStraightTube(buffer, top, foot, origin, DROP_RADIUS,
          STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
    }

    emitTieBar(buffer, te, along, payloads, origin, skyLight, blockLight);
    emitTetherTies(buffer, te, payloads, origin, skyLight, blockLight);
  }

  /**
   * One tie down to a box span's tether for every head on the bracket.
   *
   * <p>Reached from the point each head hangs from rather than from the mount, which is the same
   * projection the drops and the tie bar use -- so a tie leaves the tether directly under the
   * housing it holds, on a diagonal span as much as a square one.
   *
   * <p>No rise is added to the reported height. A cluster's bracket is rigid and gives its heads
   * no rise, so what the payload reports is already where its underside is.
   */
  private void emitTetherTies(BufferBuilder buffer, TileEntitySpanWireClusterMount te,
      List<TileEntitySpanWireClusterMount.ClusterPayload> payloads, Vec3d origin, int skyLight,
      int blockLight) {
    final SpanWireCatenary tether = te.getTether();
    if (tether == null) {
      return;
    }
    for (TileEntitySpanWireClusterMount.ClusterPayload payload : payloads) {
      // A payload that takes no tie -- a sign -- reports NaN, and gets no stub into thin air.
      if (Double.isNaN(payload.getTieY())) {
        continue;
      }
      TileEntitySpanWireHangerRenderer.emitTetherTieAt(buffer, tether, te.getSpan(),
          te.bracketPointFor(payload), payload.getTieY(), origin, skyLight, blockLight);
    }
  }

  /**
   * The thin bar across the bottoms of the heads, joining them to each other.
   *
   * <p>Runs between the outermost heads rather than past them: the top bracket overhangs because
   * it has to look like it is carrying them, but this one is a tie between housings and stops at
   * the ones it ties.
   *
   * <p>Drawn at the same height a box span's tether would meet these heads, which is what the
   * payload already answers for the bottom of its assembly. Heads on a cluster hang level with
   * each other, so for the ordinary cluster every answer is the same one.
   *
   * <p>Where they differ, the <b>deepest</b> wins rather than the mean. Heads on one bracket are
   * level but assemblies on it need not be: hang a three-section head beside one carrying a
   * single-section add-on and the two reach a block apart. A mean sits between them -- floating
   * under the short one and buried in the tall one -- whereas the deepest passes under both, which
   * is the one answer that can never put a metal bar through a lens. A head that reports no height
   * is left out, and if none report one there is no bar.
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
    double tieY = Double.POSITIVE_INFINITY;
    int tieCount = 0;

    for (TileEntitySpanWireClusterMount.ClusterPayload payload : payloads) {
      if (Double.isNaN(payload.getTieY())) {
        continue;
      }
      final double offset = te.alongFor(payload);
      if (offset < lowestOffset) {
        lowestOffset = offset;
        first = payload;
      }
      if (offset > highestOffset) {
        highestOffset = offset;
        last = payload;
      }
      tieY = Math.min(tieY, payload.getTieY());
      tieCount++;
    }

    if (tieCount < 2 || first == last) {
      return;
    }
    SpanWireCableGeometry.emitStraightTube(buffer,
        tieBarEnd(te, first, tieY), tieBarEnd(te, last, tieY),
        origin, TIE_BAR_RADIUS, STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
  }

  /** Where the tie bar meets one head: directly under the point its drop leaves the bracket. */
  private static Vec3d tieBarEnd(TileEntitySpanWireClusterMount te,
      TileEntitySpanWireClusterMount.ClusterPayload payload, double tieY) {
    final Vec3d hanging = te.bracketPointFor(payload);
    return new Vec3d(hanging.x, tieY, hanging.z);
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

  /** The bar for a cluster with nothing on it yet, spanning the columns it would cover. */
  private void emitEmptyBracket(BufferBuilder buffer, TileEntitySpanWireClusterMount te,
      Vec3d bracketCentre, Vec3d along, Vec3d origin, int skyLight, int blockLight) {
    double lowest = 0.0;
    double highest = 0.0;
    for (BlockPos column : te.getBracketColumns()) {
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
