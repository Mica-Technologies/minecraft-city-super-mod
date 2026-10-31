package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Everything one anchor draws, worked out against the world: each cable's two ends, its sampled
 * centreline, its guide pipes, its hangers down to whatever they land on, its LED strip. A list
 * of tubes the renderer turns into a display list, plus the box they fill and a key that changes
 * whenever any of it would.
 *
 * <p>Built from a {@link WorldView} rather than a world so the tests can stand a cable over a
 * made-up deck. Nothing here touches the GL.
 */
public final class BridgeCableLayout {

  /** Deepest a hanger looks for something to land on, in blocks. */
  static final int HANGER_SEARCH_DEPTH = 256;

  /** A hanger shorter than this is left out: the cable is resting on what is under it. */
  static final double MIN_HANGER_LENGTH = 0.25;

  /** LED strip radius, in blocks: under a pixel across, a strip of light rather than a tube. */
  static final double LED_RADIUS = 0.75 / 32.0;

  /** How much darker a guide pipe and a hanger clamp are than the cable they fit. */
  static final float FITTING_SHADE = 0.82f;

  /** What the layout asks of the world. */
  public interface WorldView {

    /** The anchor at a position, or null when the block there is not one. */
    @Nullable
    AnchorEnd anchorAt(BlockPos pos);

    /**
     * The top of what at a position would stop a hanger, in world y, or NaN when a hanger passes
     * through it (air, water, plants).
     */
    double solidTop(BlockPos pos);
  }

  /** An anchor block: which kind, facing which way. */
  public static final class AnchorEnd {

    final BridgeCableAnchorKind kind;
    final EnumFacing facing;

    public AnchorEnd(BridgeCableAnchorKind kind, EnumFacing facing) {
      this.kind = kind;
      this.facing = facing;
    }

    Vec3d attachPoint(BlockPos pos) {
      return kind.attachPoint(pos, facing);
    }
  }

  /** One tube to draw: a centreline, a radius and a colour. */
  public static final class Tube {

    public final List<Vec3d> points;
    public final double radius;
    public final int sides;
    public final int rgb;
    /** The sheath stripe's colour, or -1 for none. */
    public final int stripeRgb;
    /** Drawn at full brightness whatever the light: the LED strip. */
    public final boolean glow;

    Tube(List<Vec3d> points, double radius, int rgb, int stripeRgb, boolean glow) {
      this.points = points;
      this.radius = radius;
      this.sides = sidesFor(radius);
      this.rgb = rgb;
      this.stripeRgb = stripeRgb;
      this.glow = glow;
    }
  }

  /** What one hanger ended up as, for reporting and the tests. */
  public static final class Hanger {

    public final Vec3d top;
    public final Vec3d bottom;
    public final boolean onDeckAnchor;

    Hanger(Vec3d top, Vec3d bottom, boolean onDeckAnchor) {
      this.top = top;
      this.bottom = bottom;
      this.onDeckAnchor = onDeckAnchor;
    }
  }

  private final List<Tube> tubes;
  private final List<Hanger> hangers;
  private final AxisAlignedBB bounds;
  private final long key;

  private BridgeCableLayout(List<Tube> tubes, List<Hanger> hangers, AxisAlignedBB bounds,
      long key) {
    this.tubes = Collections.unmodifiableList(tubes);
    this.hangers = Collections.unmodifiableList(hangers);
    this.bounds = bounds;
    this.key = key;
  }

  public List<Tube> getTubes() {
    return tubes;
  }

  public List<Hanger> getHangers() {
    return hangers;
  }

  /** The box every tube lies inside, the owner's own block included. */
  public AxisAlignedBB getBounds() {
    return bounds;
  }

  /**
   * Changes whenever anything drawn would: the links, either end of any cable, any hanger's
   * landing. The renderer keys its display list on it.
   */
  public long getKey() {
    return key;
  }

  /** Total quads the layout draws, for the cost figures. */
  public int countQuads() {
    int quads = 0;
    for (Tube tube : tubes) {
      quads += (tube.points.size() - 1) * tube.sides;
    }
    return quads;
  }

  /** Prism sides for a radius: a 2 px cable needs six, an 8 px main cable twelve. */
  static int sidesFor(double radius) {
    if (radius <= 2.0 / 32.0 + 1.0e-9) {
      return 6;
    }
    if (radius <= 4.0 / 32.0 + 1.0e-9) {
      return 8;
    }
    return 12;
  }

  /**
   * Works out what an anchor draws.
   *
   * @param owner the anchor's position
   * @param links the cables it owns
   * @param view  the world, as far as cables care about it
   */
  public static BridgeCableLayout build(BlockPos owner, List<BridgeCableLink> links,
      WorldView view) {
    final List<Tube> tubes = new ArrayList<>();
    final List<Hanger> hangers = new ArrayList<>();
    long key = 17L;

    final AnchorEnd own = view.anchorAt(owner);
    final Vec3d start = own != null ? own.attachPoint(owner) : centre(owner);
    key = mix(key, start);

    for (BridgeCableLink link : links) {
      key = key * 31L + link.hashCode();
      final BlockPos targetPos = link.getTarget(owner);
      final AnchorEnd far = view.anchorAt(targetPos);
      final Vec3d end = far != null ? far.attachPoint(targetPos) : looseEnd(targetPos, view);
      key = mix(key, end);
      if (start.squareDistanceTo(end) < 1.0e-6) {
        continue;
      }

      final double plan = BridgeCableShape.horizontalLength(start, end);
      final double sag = link.getSagBlocks(plan);
      final BridgeCableShape.Curve curve =
          BridgeCableShape.curveFor(link.getStyle(), start, end, sag);
      final String stripes = link.getStripes();
      final List<Vec3d> points = BridgeCableShape.sample(curve,
          BridgeCableShape.segmentsFor(link.getStyle(), start, end, stripes != null));
      final double radius = link.getRadius();
      final int rgb = BridgeCableColour.rgb(link.getColour());
      tubes.add(new Tube(points, radius, rgb,
          stripes == null ? -1 : BridgeCableColour.rgb(stripes), false));

      // Guide pipes where the cable meets a socket or a deck anchor, at the angle it meets it.
      final double length = start.distanceTo(end);
      if (own != null) {
        addSleeve(tubes, start, points.get(1), own.kind.getSleeveLength(), length, radius, rgb);
      }
      if (far != null) {
        addSleeve(tubes, end, points.get(points.size() - 2), far.kind.getSleeveLength(), length,
            radius, rgb);
      }

      if (link.getLed() != null) {
        tubes.add(new Tube(offsetAbove(points, radius + LED_RADIUS), LED_RADIUS,
            BridgeCableColour.rgb(link.getLed()), -1, true));
      }

      final int spacing = link.getHangerSpacing();
      if (spacing > 0) {
        final int hangerRgb = BridgeCableColour.rgb(link.getHangerColour());
        final double hangerRadius = link.getHangerRadius();
        for (double t : BridgeCableShape.hangerStations(start, end, spacing)) {
          final Vec3d top = curve.pointAt(t);
          final Hanger hanger = dropHanger(top, radius, view);
          if (hanger == null) {
            key = key * 31L + 1L;
            continue;
          }
          key = mix(key, hanger.bottom);
          hangers.add(hanger);
          tubes.add(new Tube(twoPoints(top, hanger.bottom), hangerRadius, hangerRgb, -1, false));
          // The cable band the hanger hangs from, square to the cable where it clamps.
          final Vec3d along = curve.pointAt(Math.min(1.0, t + 1.0e-3))
              .subtract(curve.pointAt(Math.max(0.0, t - 1.0e-3))).normalize();
          final double half = Math.max(0.125, radius * 1.5);
          tubes.add(new Tube(twoPoints(top.subtract(along.scale(half)), top.add(along.scale(half))),
              radius * 1.3 + 1.0 / 64.0, shade(rgb, FITTING_SHADE), -1, false));
          if (hanger.onDeckAnchor) {
            addSleeve(tubes, hanger.bottom, top, BridgeCableAnchorKind.DECK.getSleeveLength() * 0.5,
                top.distanceTo(hanger.bottom), hangerRadius, hangerRgb);
          }
        }
      }
    }

    return new BridgeCableLayout(tubes, hangers, boundsOf(owner, tubes), key);
  }

  /**
   * Finds what a hanger dropped from a point on the cable lands on: the first deck anchor, or the
   * first block that stops it, straight below. Null when there is nothing within reach, or when the
   * cable runs through a solid block there.
   */
  @Nullable
  static Hanger dropHanger(Vec3d top, double cableRadius, WorldView view) {
    final int x = (int) Math.floor(top.x);
    final int z = (int) Math.floor(top.z);
    final int startY = (int) Math.floor(top.y - cableRadius - 1.0e-3);
    final int lowest = Math.max(0, startY - HANGER_SEARCH_DEPTH);
    for (int y = startY; y >= lowest; y--) {
      final BlockPos pos = new BlockPos(x, y, z);
      final AnchorEnd anchor = view.anchorAt(pos);
      Vec3d bottom = null;
      boolean onDeck = false;
      if (anchor != null && anchor.kind == BridgeCableAnchorKind.DECK) {
        bottom = anchor.attachPoint(pos);
        onDeck = true;
      } else {
        final double solid = view.solidTop(pos);
        if (!Double.isNaN(solid)) {
          bottom = new Vec3d(top.x, solid, top.z);
        }
      }
      if (bottom != null) {
        if (top.y - bottom.y < MIN_HANGER_LENGTH) {
          return null;
        }
        return new Hanger(top, bottom, onDeck);
      }
    }
    return null;
  }

  /**
   * Where a cable ends on a block that is not an anchor: on top of it, or in the middle of it
   * when it is air (a target that has not been built yet, or a cable meant to end in mid-air).
   */
  static Vec3d looseEnd(BlockPos pos, WorldView view) {
    final double solid = view.solidTop(pos);
    if (Double.isNaN(solid)) {
      return centre(pos);
    }
    return new Vec3d(pos.getX() + 0.5, solid, pos.getZ() + 0.5);
  }

  private static void addSleeve(List<Tube> tubes, Vec3d at, Vec3d toward, double sleeveLength,
      double cableLength, double radius, int rgb) {
    if (sleeveLength <= 0.0) {
      return;
    }
    final Vec3d direction = toward.subtract(at);
    if (lengthSq(direction) < 1.0e-12) {
      return;
    }
    final double length = Math.min(sleeveLength, cableLength * 0.25);
    final Vec3d far = at.add(direction.normalize().scale(length));
    tubes.add(new Tube(twoPoints(at, far), radius + Math.max(radius * 0.6, 1.0 / 32.0),
        shade(rgb, FITTING_SHADE), -1, false));
  }

  /**
   * The centreline moved to run along the top of the cable, square to it where it slopes, for the
   * LED strip. A vertical cable has no top; its strip runs up the east side instead.
   */
  static List<Vec3d> offsetAbove(List<Vec3d> points, double distance) {
    final List<Vec3d> out = new ArrayList<>(points.size());
    final Vec3d up = new Vec3d(0.0, 1.0, 0.0);
    for (int i = 0; i < points.size(); i++) {
      final Vec3d previous = points.get(Math.max(0, i - 1));
      final Vec3d next = points.get(Math.min(points.size() - 1, i + 1));
      Vec3d tangent = next.subtract(previous);
      tangent = lengthSq(tangent) < 1.0e-12 ? up : tangent.normalize();
      Vec3d normal = up.subtract(tangent.scale(up.dotProduct(tangent)));
      if (lengthSq(normal) < 1.0e-6) {
        normal = new Vec3d(1.0, 0.0, 0.0);
      }
      out.add(points.get(i).add(normal.normalize().scale(distance)));
    }
    return out;
  }

  private static List<Vec3d> twoPoints(Vec3d a, Vec3d b) {
    final List<Vec3d> points = new ArrayList<>(2);
    points.add(a);
    points.add(b);
    return points;
  }

  private static AxisAlignedBB boundsOf(BlockPos owner, List<Tube> tubes) {
    double minX = owner.getX();
    double minY = owner.getY();
    double minZ = owner.getZ();
    double maxX = owner.getX() + 1.0;
    double maxY = owner.getY() + 1.0;
    double maxZ = owner.getZ() + 1.0;
    for (Tube tube : tubes) {
      final double r = tube.radius;
      for (Vec3d p : tube.points) {
        minX = Math.min(minX, p.x - r);
        minY = Math.min(minY, p.y - r);
        minZ = Math.min(minZ, p.z - r);
        maxX = Math.max(maxX, p.x + r);
        maxY = Math.max(maxY, p.y + r);
        maxZ = Math.max(maxZ, p.z + r);
      }
    }
    return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ).grow(0.5);
  }

  static int shade(int rgb, float factor) {
    final int r = Math.round(((rgb >> 16) & 0xFF) * factor);
    final int g = Math.round(((rgb >> 8) & 0xFF) * factor);
    final int b = Math.round((rgb & 0xFF) * factor);
    return (r << 16) | (g << 8) | b;
  }

  /**
   * Squared length, written out: Vec3d's own accessor is named differently between mappings
   * (the span wire geometry avoids it for the same reason).
   */
  private static double lengthSq(Vec3d v) {
    return v.x * v.x + v.y * v.y + v.z * v.z;
  }

  private static Vec3d centre(BlockPos pos) {
    return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
  }

  private static long mix(long key, Vec3d point) {
    key = key * 31L + Double.doubleToLongBits(point.x);
    key = key * 31L + Double.doubleToLongBits(point.y);
    return key * 31L + Double.doubleToLongBits(point.z);
  }
}
