package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Draws the cable running from an anchor to the first attachment along its span, plus the
 * termination hardware the span dead ends on: the thimble the cable is looped around, and the
 * back-guy running down to a ground anchor.
 *
 * <p>The far anchor of a span owns no cable but still draws both of these, which is why
 * {@link SpanWireCableRenderer} draws hardware for an attachment with no segment rather than
 * returning early.
 */
public class TileEntitySpanWireAnchorRenderer
    extends SpanWireCableRenderer<TileEntitySpanWireAnchor> {

  /** The thimble: the teardrop the cable is turned around before it is clamped back on itself. */
  private static final int THIMBLE_SEGMENTS = 10;
  private static final double THIMBLE_RADIUS = 0.15;
  private static final double THIMBLE_WIRE_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 0.95;

  /** A guy strand is about as heavy as the messenger it is resisting. */
  private static final double GUY_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 0.95;

  /**
   * How far a guy is dead ended above the ground anchor block's own origin. The anchor's rod
   * stands half a block proud, and the guy is clamped to the top of it.
   */
  private static final double GUY_ANCHOR_HEIGHT = 0.45;

  private static final float STEEL_RED = 0.24f;
  private static final float STEEL_GREEN = 0.25f;
  private static final float STEEL_BLUE = 0.27f;

  @Override
  protected void emitAttachmentHardware(TileEntitySpanWireAnchor te, BufferBuilder buffer,
      SpanWireCatenary cable, double atT, Vec3d origin, int skyLight, int blockLight) {
    final Vec3d attach = te.getAttachPoint();

    emitThimble(buffer, cable, atT, attach, origin, skyLight, blockLight);
    emitGuy(buffer, te, attach, origin, skyLight, blockLight);
  }

  /**
   * The thimble at the dead end, drawn as a ring standing in the plane the cable leaves in.
   *
   * <p>Oriented from the cable's own direction at this end rather than from the block's facing,
   * so it lies correctly on a span that leaves at an angle -- which, now that spans may run
   * diagonally, is most of them.
   */
  private void emitThimble(BufferBuilder buffer, SpanWireCatenary cable, double atT, Vec3d attach,
      Vec3d origin, int skyLight, int blockLight) {
    // Sample inward along the span; at an end one of the two samples is the end itself.
    final double inward = atT < 0.5 ? Math.min(1.0, atT + 0.01) : Math.max(0.0, atT - 0.01);
    Vec3d along = cable.pointAt(inward).subtract(cable.pointAt(atT));
    if (along.x * along.x + along.y * along.y + along.z * along.z < 1.0e-12) {
      along = new Vec3d(1.0, 0.0, 0.0);
    }
    along = along.normalize();
    final Vec3d up = new Vec3d(0.0, 1.0, 0.0);

    final List<Vec3d> ring = new ArrayList<>(THIMBLE_SEGMENTS);
    for (int i = 0; i < THIMBLE_SEGMENTS; i++) {
      final double angle = 2.0 * Math.PI * i / THIMBLE_SEGMENTS;
      ring.add(attach
          .add(along.scale(Math.cos(angle) * THIMBLE_RADIUS))
          .add(up.scale(Math.sin(angle) * THIMBLE_RADIUS)));
    }
    SpanWireCableGeometry.emitTubePath(buffer, ring, true, origin, THIMBLE_WIRE_RADIUS,
        STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
  }

  /**
   * The back-guy, drawn dead straight.
   *
   * <p>Not a catenary, and that is the point: a guy is pulled up hard enough that its own sag is
   * invisible, and drawing it with any at all would read as a slack rope rather than something
   * holding a pole against the pull of a span.
   */
  private void emitGuy(BufferBuilder buffer, TileEntitySpanWireAnchor te, Vec3d attach,
      Vec3d origin, int skyLight, int blockLight) {
    final BlockPos guyAnchor = te.getGuyAnchor();
    if (guyAnchor == null) {
      return;
    }
    final Vec3d ground = new Vec3d(guyAnchor.getX() + 0.5,
        guyAnchor.getY() + GUY_ANCHOR_HEIGHT, guyAnchor.getZ() + 0.5);
    SpanWireCableGeometry.emitStraightTube(buffer, attach, ground, origin, GUY_RADIUS,
        STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
  }
}
