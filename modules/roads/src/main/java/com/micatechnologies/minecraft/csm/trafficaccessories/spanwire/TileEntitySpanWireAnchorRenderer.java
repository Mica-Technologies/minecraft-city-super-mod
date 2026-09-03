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
  private static final double THIMBLE_RADIUS = 0.17;
  private static final double THIMBLE_WIRE_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 1.25;

  /** A guy strand is about as heavy as the messenger it is resisting. */
  private static final double GUY_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 0.95;

  /**
   * How far a guy is dead ended above the ground anchor block's own origin. The anchor's rod
   * stands half a block proud, and the guy is clamped to the top of it.
   */
  private static final double GUY_ANCHOR_HEIGHT = 0.45;

  /** The shackle taking up the offset between the block's own eyebolt and the cable. */
  private static final double LINK_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 2.6;

  /**
   * The longest stub that will be drawn from a dead-ended tether back to its pole.
   *
   * <p>A limit rather than the length. The stub runs to the pole, and how far that is depends on
   * how far the span has been offset sideways off the block centre line; a fixed length is right
   * at one offset and short at every other. This only stops a span in some unforeseen state from
   * drawing a girder across the intersection.
   */
  private static final double TETHER_TERMINATION_MAX_REACH = 1.5;

  private static final float STEEL_RED = 0.24f;
  private static final float STEEL_GREEN = 0.25f;
  private static final float STEEL_BLUE = 0.27f;

  @Override
  protected void emitAttachmentHardware(TileEntitySpanWireAnchor te, BufferBuilder buffer,
      SpanWireCatenary cable, double atT, Vec3d origin, int skyLight, int blockLight) {
    final Vec3d attach = te.getAttachPoint();

    emitOffsetLink(buffer, te, attach, origin, skyLight, blockLight);
    emitThimble(buffer, cable, atT, attach, origin, skyLight, blockLight);
    emitTetherTermination(buffer, te, atT, origin, skyLight, blockLight);
    emitGuy(buffer, te, attach, origin, skyLight, blockLight);
  }

  /**
   * The link from the eyebolt in this block's own model out to where the cable actually is.
   *
   * <p>The block model cannot move: it is a JSON model, the same for every anchor in the world,
   * with its eyebolt on the block's centre line. The span, meanwhile, runs to one side of that
   * line so its wires sit over the signal housings -- so the cable dead-ends beside the eyebolt
   * rather than through it, and the anchor reads as a bracket the wire happens to pass near.
   *
   * <p>A short link closes it, and is what the real hardware would be: an anchor shackle taking up
   * exactly the offset between a fixed plate and where the wire needs to land. Zero length when
   * the span runs down the block centre line, in which case nothing is drawn at all.
   */
  private void emitOffsetLink(BufferBuilder buffer, TileEntitySpanWireAnchor te, Vec3d attach,
      Vec3d origin, int skyLight, int blockLight) {
    final Vec3d eyebolt = SpanWireDefinition.attachPoint(te.getPos());
    final double dx = attach.x - eyebolt.x;
    final double dz = attach.z - eyebolt.z;
    if (dx * dx + dz * dz < 1.0e-8) {
      return;
    }
    SpanWireCableGeometry.emitStraightTube(buffer, eyebolt, attach, origin, LINK_RADIUS,
        STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
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
   * Dead-ends a box span's lower tether on the pole.
   *
   * <p>Without it the tether simply stops in the air a few blocks under the anchor, held by
   * nothing -- the one piece of a box span that was not visibly fixed to anything at either end.
   * A real one gets its own bracket lower down the pole, so it is drawn as a thimble at the end
   * plus a stub running back into the pole.
   *
   * <p>The direction is taken from the span rather than from the block's facing. The anchor sits
   * between the pole and the run, so <em>outward along the span</em> is where the pole is, and
   * that holds for a span leaving at any angle -- including the diagonals, where a block facing
   * could only ever be square to one of the two axes.
   */
  private void emitTetherTermination(BufferBuilder buffer, TileEntitySpanWireAnchor te, double atT,
      Vec3d origin, int skyLight, int blockLight) {
    final SpanWireCatenary tether = te.getTether();
    if (tether == null) {
      return;
    }
    final Vec3d end = tether.pointAt(atT);
    final SpanWireDefinition span = te.getSpan();
    final BlockPos lower = span == null ? null : span.getTetherAnchorFor(te.getPos());

    if (lower != null) {
      // Dead-ended on a real anchor. It is the same block as the one above, so the hardware that
      // meets it is the same too: a shackle from that block's own eyebolt out to where the wire
      // is, then the thimble the wire turns around. The eyebolt sits on the block centre line and
      // the wire does not, for exactly the reason the messenger's does not.
      SpanWireCableGeometry.emitStraightTube(buffer, SpanWireDefinition.attachPoint(lower), end,
          origin, LINK_RADIUS, STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
      emitThimble(buffer, tether, atT, end, origin, skyLight, blockLight);
      return;
    }

    // No anchor placed: the tether hangs at a derived height, so there is nothing to dead-end on
    // and the best that can be drawn is its own bracket, lower down the same pole.
    //
    // Aimed at the pole rather than run back along the wire. Running along the wire was a stub of
    // fixed length pointing wherever the span happened to leave, which stopped in mid-air short of
    // anything -- the wire ended near the anchor bracket rather than on it, and read as a gap. The
    // pole is on this block's own centre line, and going there is what the shackle on the
    // messenger above does, so the two ends now match.
    final Vec3d eyebolt = SpanWireDefinition.attachPoint(te.getPos());
    final Vec3d toPole = new Vec3d(eyebolt.x, end.y, eyebolt.z);
    final double reach = end.distanceTo(toPole);
    if (reach > 1.0e-6) {
      SpanWireCableGeometry.emitStraightTube(buffer, end,
          reach <= TETHER_TERMINATION_MAX_REACH
              ? toPole
              : end.add(toPole.subtract(end).normalize().scale(TETHER_TERMINATION_MAX_REACH)),
          origin, LINK_RADIUS, STEEL_RED, STEEL_GREEN, STEEL_BLUE, skyLight, blockLight);
    }
    emitThimble(buffer, tether, atT, end, origin, skyLight, blockLight);
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
