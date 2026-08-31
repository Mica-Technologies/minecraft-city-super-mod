package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.Vec3d;

/**
 * Draws the cable running from a hanger mount to the next attachment along its span, plus the
 * mount's own hardware reaching up to the messenger.
 *
 * <p><b>The mast belongs to the mount and rises to the cable, not the other way round.</b> Span
 * wire signals are not hung on a flexible drop — a rigid pipe stands up from the signal's mount
 * and clamps over the messenger at the top, which is why a span of them stays square to the road
 * instead of turning in the wind. The mast is therefore drawn from the mount upward, and its
 * length is whatever it takes to reach the cable.
 *
 * <p>That varying length is the whole reason the mast exists. A mount sits on the block grid and
 * the cable is a smooth curve, so the two almost never meet exactly — on a 20-block span the
 * cable falls a full block below its anchors at midspan, and no arrangement of whole blocks
 * follows that. The mast absorbs the difference, which is exactly what it does on a real span.
 */
public class TileEntitySpanWireHangerRenderer
    extends SpanWireCableRenderer<TileEntitySpanWireHanger> {

  /** A rigid pipe, so noticeably stouter than the messenger it clamps to. */
  private static final double MAST_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 2.0;

  /** The saddle gripping the cable is wider still, and short. */
  private static final double SADDLE_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 2.6;
  private static final double SADDLE_HALF_LENGTH = 0.11;

  /** Galvanized pipe: lighter than the messenger so the two read as separate parts. */
  private static final float MAST_RED = 0.30f;
  private static final float MAST_GREEN = 0.31f;
  private static final float MAST_BLUE = 0.33f;

  /** How far along the span to sample either side of the clamp to get the cable's direction. */
  private static final double TANGENT_EPSILON = 1.0e-3;

  /**
   * The coiled conductor slack at the clamp. Sized to read at the distance a span is actually
   * looked at from — a driver's eye view from thirty or forty blocks back — without becoming a
   * blob up close.
   */
  private static final int COIL_LOOPS = 2;
  private static final int COIL_SEGMENTS = 10;
  private static final double COIL_RADIUS = 0.13;
  private static final double COIL_WIRE_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 0.7;
  private static final double COIL_SPACING = 0.055;

  /**
   * How far along the cable the coils sit from the mast, so they hang beside it rather than
   * through it. Enough to clear the mast and the saddle, both of which are centred on the clamp.
   */
  private static final double COIL_SIDE_OFFSET =
      SADDLE_HALF_LENGTH + COIL_RADIUS + SpanWireCableGeometry.CABLE_RADIUS;

  /** The strap from a mount down onto a payload that carries no mounting hardware of its own. */
  private static final double STRAP_LENGTH = 0.85;
  private static final double STRAP_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 1.6;

  /** The tie from the lower tether of a box span up to the bottom of the signal it steadies. */
  private static final double TIE_HEIGHT = 0.45;
  private static final double TIE_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 1.2;

  /** Black insulation, like the conductor run it is spliced out of. */
  private static final float COIL_RED = 0.09f;
  private static final float COIL_GREEN = 0.09f;
  private static final float COIL_BLUE = 0.10f;

  @Override
  protected void emitAttachmentHardware(TileEntitySpanWireHanger te, BufferBuilder buffer,
      SpanWireCatenary cable, double atT, Vec3d origin, int skyLight, int blockLight) {
    final double drop = te.getCableDrop();
    // Nothing is drawn when the mount is above the cable. That is a placement error, and a mast
    // reaching downward would hide it; leaving the mount visibly unattached is the report.
    if (drop <= 0.0) {
      return;
    }

    final Vec3d cablePoint = cable.pointAt(atT);
    // The foot stands on the payload, not on the middle of the block. For a signal head those
    // are not the same place -- the body is set back in its block and the visors hang off the
    // front, so a drop down the block centre line lands on the top visor.
    final Vec3d mountPoint = te.getHardwareFootPoint();

    // The mast stands upright on the payload and the sideways offset is taken up by a short arm
    // at the top, rather than by leaning the mast across to the cable. The lean is the obvious
    // way to do it and is wrong: the offset is fixed but the drop is not, so a mount sitting
    // close under the cable -- which is most of them near midspan -- would lean about forty
    // degrees. An upright mast with an offset arm is also what the real hardware looks like.
    final Vec3d mastTop = new Vec3d(mountPoint.x, cablePoint.y, mountPoint.z);
    SpanWireCableGeometry.emitStraightTube(buffer, mountPoint, mastTop, origin, MAST_RADIUS,
        MAST_RED, MAST_GREEN, MAST_BLUE, skyLight, blockLight);

    final double armX = cablePoint.x - mastTop.x;
    final double armZ = cablePoint.z - mastTop.z;
    if (armX * armX + armZ * armZ > 1.0e-8) {
      SpanWireCableGeometry.emitStraightTube(buffer, mastTop, cablePoint, origin, MAST_RADIUS,
          MAST_RED, MAST_GREEN, MAST_BLUE, skyLight, blockLight);
    }

    // The saddle at the top, lying along the cable and gripping over it. Built from the cable's
    // own direction so it stays square to the messenger where the messenger is sloping, which
    // near the anchors of a long span it noticeably is.
    final Vec3d ahead = cable.pointAt(Math.min(1.0, atT + TANGENT_EPSILON));
    final Vec3d behind = cable.pointAt(Math.max(0.0, atT - TANGENT_EPSILON));
    Vec3d along = ahead.subtract(behind);
    if (along.x * along.x + along.y * along.y + along.z * along.z < 1.0e-18) {
      along = new Vec3d(1.0, 0.0, 0.0);
    }
    along = along.normalize();

    SpanWireCableGeometry.emitStraightTube(buffer,
        cablePoint.subtract(along.scale(SADDLE_HALF_LENGTH)),
        cablePoint.add(along.scale(SADDLE_HALF_LENGTH)),
        origin, SADDLE_RADIUS, MAST_RED, MAST_GREEN, MAST_BLUE, skyLight, blockLight);

    emitConductorCoils(buffer, te.getCoilStyle(), cablePoint, along, origin, skyLight,
        blockLight);
    emitTetherTie(buffer, te, atT, origin, skyLight, blockLight);
    emitPayloadStrap(buffer, te, mountPoint, origin, skyLight, blockLight);
  }

  /**
   * The strap joining this mount to whatever it carries.
   *
   * <p>Drawn only for payloads that ask for one. A signal head brings its own mount hardware and
   * declines it; a sign or a box has nothing of its own and would otherwise appear to float below
   * the mast with a gap between them.
   */
  private void emitPayloadStrap(BufferBuilder buffer, TileEntitySpanWireHanger te, Vec3d mountPoint,
      Vec3d origin, int skyLight, int blockLight) {
    if (!te.carriesStrapPayload()) {
      return;
    }
    SpanWireCableGeometry.emitStraightTube(buffer, mountPoint,
        mountPoint.subtract(0.0, STRAP_LENGTH, 0.0), origin, STRAP_RADIUS,
        MAST_RED, MAST_GREEN, MAST_BLUE, skyLight, blockLight);
  }

  /**
   * The tie clamping the bottom of this signal to the lower tether of a box span.
   *
   * <p>Without it the tether is a wire that happens to pass near the signals rather than the
   * thing holding them steady, which is its entire reason to exist -- and a viewer reads that
   * immediately even if they could not say why.
   *
   * <p>Drawn upward from the tether by a fixed amount rather than to the signal's actual bottom.
   * Finding that would mean the span wire package reaching into the traffic signal package for
   * per-head geometry, which is the dependency this system has been careful not to create; the
   * tether's clearance is fixed, so a fixed tie lands in the right place for the heads it was
   * sized against.
   */
  private void emitTetherTie(BufferBuilder buffer, TileEntitySpanWireHanger te, double atT,
      Vec3d origin, int skyLight, int blockLight) {
    final SpanWireCatenary tether = te.getTether();
    if (tether == null) {
      return;
    }
    // The tether is already offset across the span, so its own curve is where the tie belongs.
    final Vec3d tetherPoint = tether.pointAt(atT);
    SpanWireCableGeometry.emitStraightTube(buffer, tetherPoint,
        tetherPoint.add(0.0, TIE_HEIGHT, 0.0), origin, TIE_RADIUS,
        MAST_RED, MAST_GREEN, MAST_BLUE, skyLight, blockLight);
  }

  /**
   * The slack conductor coiled up at the clamp.
   *
   * <p>This is the detail that makes a span read as a real one. Every hanger interrupts the
   * lashed conductor run to drop power into the signal, and the surplus is coiled off and left
   * hanging at the clamp so there is something to work with later. In photographs of span wire
   * it is the most conspicuous thing at each hanger — conspicuous enough that it is routinely
   * mistaken for the messenger itself kinking, which it never does (the plan's D2).
   *
   * <p>Two loops rather than one, because a single ring reads as a mistake and a pair reads as
   * coiled slack.
   *
   * <p><b>They sit to one side of the mast, not around it.</b> A coil centred on the clamp
   * intersects the mast and the saddle, which is both wrong and visibly wrong — the slack is
   * coiled off and left hanging beside the hardware, clear of it. Some installations carry a
   * coil on each side; that is a Phase 4B option rather than the default, since one side is the
   * common case.
   */
  private void emitConductorCoils(BufferBuilder buffer, SpanWireCoilStyle style,
      Vec3d cablePoint, Vec3d along, Vec3d origin, int skyLight, int blockLight) {
    if (style == SpanWireCoilStyle.NONE) {
      return;
    }
    // The loops hang in the vertical plane containing the cable, which is how they sit when they
    // are simply left to dangle from the clamp.
    final Vec3d up = new Vec3d(0.0, 1.0, 0.0);

    final int sides = style == SpanWireCoilStyle.BOTH_SIDES ? 2 : 1;
    for (int side = 0; side < sides; side++) {
      final double direction = side == 0 ? 1.0 : -1.0;
      emitCoilBundle(buffer, cablePoint, along, up, direction, origin, skyLight, blockLight);
    }
  }

  /** One bundle of loops, hanging to one side of the mast. */
  private void emitCoilBundle(BufferBuilder buffer, Vec3d cablePoint, Vec3d along, Vec3d up,
      double direction, Vec3d origin, int skyLight, int blockLight) {
    for (int loop = 0; loop < COIL_LOOPS; loop++) {
      final double alongOffset = direction * (COIL_SIDE_OFFSET + loop * COIL_SPACING);
      final Vec3d centre = cablePoint
          .add(along.scale(alongOffset))
          .add(0.0, -(COIL_RADIUS + SpanWireCableGeometry.CABLE_RADIUS), 0.0);

      final List<Vec3d> ring = new ArrayList<>(COIL_SEGMENTS);
      for (int i = 0; i < COIL_SEGMENTS; i++) {
        final double angle = 2.0 * Math.PI * i / COIL_SEGMENTS;
        final double c = Math.cos(angle) * COIL_RADIUS;
        final double s = Math.sin(angle) * COIL_RADIUS;
        ring.add(centre.add(along.scale(c)).add(up.scale(s)));
      }

      SpanWireCableGeometry.emitTubePath(buffer, ring, true, origin, COIL_WIRE_RADIUS,
          COIL_RED, COIL_GREEN, COIL_BLUE, skyLight, blockLight);
    }
  }
}
