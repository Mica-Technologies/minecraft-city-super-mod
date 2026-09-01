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

  /**
   * How much larger the coils are on an extending mast.
   *
   * <p>An extending mast is chosen for a long drop, and a long drop means more slack conductor
   * left coiled at the clamp -- so the loop hanging there is bigger. At the flush size it read as
   * a token loop next to all that mast.
   */
  private static final double COIL_MAST_SCALE = 1.625;

  /**
   * How far below the cable the pigtail enters the mast fitting.
   *
   * <p>Into the side of the gooseneck just under the clamp, which is where the conductor actually
   * goes -- not into the clamp itself, which only holds the messenger.
   */
  private static final double PIGTAIL_MAST_DROP = 0.055;

  /** How far the pigtail dips between its two ends. Slack wire, not a taut string. */
  private static final double PIGTAIL_SAG = 0.045;

  /** Enough segments for the dip to read as a curve rather than a bent stick. */
  private static final int PIGTAIL_SEGMENTS = 6;

  /** Where on the coil the lead leaves: the upper inner quarter, as a fraction of its radius. */
  private static final double PIGTAIL_QUARTER = 0.707;
  private static final double COIL_WIRE_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 0.7;
  private static final double COIL_SPACING = 0.055;

  /**
   * The shortest cap on a tether tie, used when the span's own clearance is smaller or unknown.
   *
   * <p>The cap exists so a payload reporting something absurd cannot draw a tie across the sky.
   * It is a floor rather than the whole answer because a legitimate tie can be long: the tether
   * hangs below the <em>deepest</em> thing on the span, so a plain three-section head sharing a
   * span with a doghouse-plus-add-on assembly is tied by a stub two blocks and more in length,
   * and a fixed 1.5 silently drew nothing there.
   */
  private static final double MIN_TIE_HEIGHT_CAP = 1.5;
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

    // The foot stands on the payload, not on the middle of the block. For a signal head those
    // are not the same place -- the body is set back in its block and the visors hang off the
    // front, so a drop down the block centre line lands on the top visor.
    final Vec3d mountPoint = te.getHardwareFootPoint();

    // Grip the cable directly above the mast, not at the mount's own place along the span.
    //
    // The offset between the block and the payload has two parts, and only one of them is real
    // hardware. The part across the cable is genuine: the messenger runs down the centre line and
    // the housing sits to one side of it, so something has to reach across. The part *along* the
    // cable is not -- a head set back along the way it faces puts its housing further down the
    // span, and clamping at the block's own parameter left an arm running away along the wire to
    // meet it, which is what it looked like: a stub of pipe laid on the messenger.
    //
    // Projecting the foot onto the span drops the along-cable part and keeps the across-cable
    // part, so the mast comes straight down from the clamp and the arm only ever bridges the
    // offset that genuinely exists.
    final double gripT = cable.parameterAt(mountPoint.x, mountPoint.z);
    final Vec3d cablePoint = cable.pointAt(gripT);

    // The mast stands upright on the payload and the sideways offset is taken up by a short arm
    // at the top, rather than by leaning the mast across to the cable. The lean is the obvious
    // way to do it and is wrong: the offset is fixed but the drop is not, so a mount sitting
    // close under the cable -- which is most of them near midspan -- would lean about forty
    // degrees. An upright mast with an offset arm is also what the real hardware looks like.
    // The cable's own direction here. Needed before the mast, because a paired hanger is spread
    // along it, and again below to keep the saddle square to a sloping messenger.
    final Vec3d ahead = cable.pointAt(Math.min(1.0, gripT + TANGENT_EPSILON));
    final Vec3d behind = cable.pointAt(Math.max(0.0, gripT - TANGENT_EPSILON));
    Vec3d along = ahead.subtract(behind);
    if (along.x * along.x + along.y * along.y + along.z * along.z < 1.0e-18) {
      along = new Vec3d(1.0, 0.0, 0.0);
    }
    along = along.normalize();

    final Vec3d mastTop = new Vec3d(mountPoint.x, cablePoint.y, mountPoint.z);
    SpanWireCableGeometry.emitStraightTube(buffer, mountPoint, mastTop, origin, MAST_RADIUS,
        MAST_RED, MAST_GREEN, MAST_BLUE, skyLight, blockLight);

    final double armX = cablePoint.x - mastTop.x;
    final double armZ = cablePoint.z - mastTop.z;
    if (armX * armX + armZ * armZ > 1.0e-8) {
      SpanWireCableGeometry.emitStraightTube(buffer, mastTop, cablePoint, origin, MAST_RADIUS,
          MAST_RED, MAST_GREEN, MAST_BLUE, skyLight, blockLight);
    }

    SpanWireCableGeometry.emitStraightTube(buffer,
        cablePoint.subtract(along.scale(SADDLE_HALF_LENGTH)),
        cablePoint.add(along.scale(SADDLE_HALF_LENGTH)),
        origin, SADDLE_RADIUS, MAST_RED, MAST_GREEN, MAST_BLUE, skyLight, blockLight);

    // Only where something is actually being fed. The coil is surplus conductor left from
    // dropping power into the payload, so a sign -- bolted to the wire and wired to nothing --
    // gets none.
    if (te.payloadTakesConductorFeed()) {
      emitConductorCoils(buffer, te.getCoilStyle(), te.getMountStyle(), cablePoint, mastTop,
          along, origin, skyLight, blockLight);
    }
    emitTetherTie(buffer, te, gripT, origin, skyLight, blockLight);
  }

  /**
   * The tie clamping the bottom of this signal to the lower tether of a box span.
   *
   * <p>Without it the tether is a wire that happens to pass near the signals rather than the
   * thing holding them steady, which is its entire reason to exist -- and a viewer reads that
   * immediately even if they could not say why.
   *
   * <p>Drawn to the payload's own underside, which the payload reports -- the span wire package
   * still never reaches into the traffic signal package for per-head geometry, it asks. An earlier
   * version stood a fixed stub up from the tether instead, on the reasoning that the clearance was
   * fixed too. It is not: the clearance is measured from the deepest thing on the span, so a fixed
   * stub was correct at one mount and short or overshooting at every other.
   */
  private void emitTetherTie(BufferBuilder buffer, TileEntitySpanWireHanger te, double atT,
      Vec3d origin, int skyLight, int blockLight) {
    final SpanWireCatenary tether = te.getTether();
    if (tether == null) {
      return;
    }
    // Nothing to tie to. A sign on a box span is not tied to the tether, and drawing a stub
    // upward from the wire into empty air is worse than drawing nothing.
    final double tieY = te.getPayloadTetherTieY();
    if (Double.isNaN(tieY)) {
      return;
    }

    // Taken from the wire directly below the head, rather than at this mount's own parameter
    // along the span. A mount's parameter is its block centre projected onto the chord, which on a
    // diagonal is not where the head actually is -- so evaluating there put the tie's foot off to
    // one side and left it leaning. Asking the wire for its point below the head keeps the tie
    // plumb, and keeps it touching the wire even where the two do not quite agree.
    final Vec3d foot = te.getHardwareFootPoint();
    final Vec3d tetherPoint = tether.pointAt(tether.parameterAt(foot.x, foot.z));

    // Drawn to the payload's actual underside rather than a fixed height. The tether is strung
    // far tighter than the messenger, so heads hanging from the messenger's curve sit at
    // different heights above it along the span: a fixed stub is only ever correct at one point
    // and pokes into the lenses everywhere else. Clamped so a payload reporting something absurd
    // cannot draw a tie across the sky.
    // Capped by how far the tether hangs below the messenger, because no honest tie can be longer
    // than that -- the payload it reaches is hanging from the messenger itself. That bound moves
    // with the span, which is the point: a fixed cap either cuts off the legitimate long ties a
    // mixed span produces, or is loose enough to be no guard at all on a short one.
    final SpanWireDefinition span = te.getSpan();
    final double cap = span == null
        ? MIN_TIE_HEIGHT_CAP
        : Math.max(MIN_TIE_HEIGHT_CAP, span.getTetherClearance());
    final double top = Math.min(tieY, tetherPoint.y + cap);
    if (top - tetherPoint.y < 1.0e-4) {
      return;
    }

    SpanWireCableGeometry.emitStraightTube(buffer, tetherPoint,
        new Vec3d(tetherPoint.x, top, tetherPoint.z), origin, TIE_RADIUS,
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
      SpanWireMountStyle mountStyle, Vec3d cablePoint, Vec3d mastTop, Vec3d along, Vec3d origin,
      int skyLight, int blockLight) {
    final double coilRadius =
        COIL_RADIUS * (mountStyle == SpanWireMountStyle.MAST ? COIL_MAST_SCALE : 1.0);
    // The clearance past the saddle grows with the coil. Without that, a bigger loop simply moves
    // back into the hardware it was offset sideways to avoid in the first place.
    final double sideOffset =
        SADDLE_HALF_LENGTH + coilRadius + SpanWireCableGeometry.CABLE_RADIUS;
    if (style == SpanWireCoilStyle.NONE) {
      return;
    }
    // The loops hang in the vertical plane containing the cable, which is how they sit when they
    // are simply left to dangle from the clamp.
    final Vec3d up = new Vec3d(0.0, 1.0, 0.0);

    final int sides = style == SpanWireCoilStyle.BOTH_SIDES ? 2 : 1;
    for (int side = 0; side < sides; side++) {
      final double direction = side == 0 ? 1.0 : -1.0;
      emitCoilBundle(buffer, cablePoint, mastTop, along, up, direction, coilRadius, sideOffset,
          origin, skyLight, blockLight);
    }
  }

  /**
   * One bundle of loops, hanging to one side of the mast.
   *
   * <p>Takes its size and its clearance rather than reading constants, because both depend on the
   * mount style: an extending mast carries visibly more coiled slack than a flush clamp.
   */
  private void emitCoilBundle(BufferBuilder buffer, Vec3d cablePoint, Vec3d mastTop, Vec3d along,
      Vec3d up, double direction, double coilRadius, double sideOffset, Vec3d origin, int skyLight,
      int blockLight) {
    Vec3d pigtailFrom = null;
    for (int loop = 0; loop < COIL_LOOPS; loop++) {
      final double alongOffset = direction * (sideOffset + loop * COIL_SPACING);
      final Vec3d centre = cablePoint
          .add(along.scale(alongOffset))
          .add(0.0, -(coilRadius + SpanWireCableGeometry.CABLE_RADIUS), 0.0);

      final List<Vec3d> ring = new ArrayList<>(COIL_SEGMENTS);
      for (int i = 0; i < COIL_SEGMENTS; i++) {
        final double angle = 2.0 * Math.PI * i / COIL_SEGMENTS;
        final double c = Math.cos(angle) * coilRadius;
        final double s = Math.sin(angle) * coilRadius;
        ring.add(centre.add(along.scale(c)).add(up.scale(s)));
      }

      SpanWireCableGeometry.emitTubePath(buffer, ring, true, origin, COIL_WIRE_RADIUS,
          COIL_RED, COIL_GREEN, COIL_BLUE, skyLight, blockLight);

      // The loop nearest the mast is the one the lead comes off. Taken from its upper inner
      // quarter, which is the part of a hanging coil that faces the hardware.
      if (loop == 0) {
        pigtailFrom = centre
            .add(along.scale(-direction * coilRadius * PIGTAIL_QUARTER))
            .add(up.scale(coilRadius * PIGTAIL_QUARTER));
      }
    }

    if (pigtailFrom != null) {
      emitPigtail(buffer, mastTop.add(0.0, -PIGTAIL_MAST_DROP, 0.0), pigtailFrom, origin,
          skyLight, blockLight);
    }
  }

  /**
   * The lead running from a coil back into the mast.
   *
   * <p>Coiled slack is surplus conductor, and conductor is surplus because it is on its way
   * somewhere -- into the signal. Without this the coils are rings hanging near the hardware with
   * no reason to be there, which is what they looked like: decoration rather than wiring.
   *
   * <p>Dipped between its ends rather than run straight. It is slack cable a few inches long, and a
   * taut line between two points is the one thing it never looks like.
   */
  private void emitPigtail(BufferBuilder buffer, Vec3d atMast, Vec3d atCoil, Vec3d origin,
      int skyLight, int blockLight) {
    final List<Vec3d> path = new ArrayList<>(PIGTAIL_SEGMENTS + 1);
    for (int i = 0; i <= PIGTAIL_SEGMENTS; i++) {
      final double t = i / (double) PIGTAIL_SEGMENTS;
      // Zero at both ends, deepest in the middle, so it leaves and arrives where it should.
      final double sag = PIGTAIL_SAG * 4.0 * t * (1.0 - t);
      path.add(new Vec3d(
          atMast.x + (atCoil.x - atMast.x) * t,
          atMast.y + (atCoil.y - atMast.y) * t - sag,
          atMast.z + (atCoil.z - atMast.z) * t));
    }
    SpanWireCableGeometry.emitTubePath(buffer, path, false, origin, COIL_WIRE_RADIUS,
        COIL_RED, COIL_GREEN, COIL_BLUE, skyLight, blockLight);
  }
}
