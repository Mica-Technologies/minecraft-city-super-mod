package com.micatechnologies.minecraft.csm.lifesafety;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Works out where a strobe's light lands on the world around it, so
 * {@link TileEntityFireAlarmStrobeRenderer} can paint a pool of light on each surface the beam
 * reaches instead of ending the effect in mid-air.
 *
 * <p>A fan of rays is cast out of the lens along the direction the device points. Every ray that
 * hits something becomes a {@link Splash}: a position on that surface, the face it landed on, and
 * a weight combining how far the light travelled and how squarely it struck. The renderer turns
 * each one into a soft additive pool.</p>
 *
 * <p>The result is cached per position and recomputed only every {@link #CACHE_TTL_TICKS} ticks,
 * staggered by position so a corridor of devices does not all re-cast on the same frame. Casting
 * nine short rays is cheap, but doing it for every strobe on every frame of a 75 ms flash is not,
 * and the geometry a wall-mounted appliance points at rarely changes.</p>
 */
@SideOnly(Side.CLIENT)
public final class StrobeSurfaceProjection {

  /**
   * How far the beam is followed, in blocks. Also what the strobe tile entities widen their render
   * bounding box by -- a splash outside those bounds is culled the moment the device itself leaves
   * the screen, which reads as the light switching off as you turn your head.
   *
   * <p>Keep this a compile-time constant. The tile entities that read it are side-neutral while
   * this class is client-only, and it is only safe for them to name it because javac inlines the
   * value rather than emitting a reference to a class a dedicated server will have stripped.</p>
   */
  public static final float MAX_DISTANCE = 6.0f;

  /** Rays per cast: one down the axis, the rest around it at {@link #FAN_ANGLE_DEGREES}. */
  private static final int RING_RAYS = 8;
  private static final float FAN_ANGLE_DEGREES = 30.0f;

  private static final int CACHE_TTL_TICKS = 40;
  private static final int CACHE_SOFT_LIMIT = 256;
  private static final int CACHE_STALE_TICKS = 600;

  /**
   * Two splashes closer together than this on the same face are treated as one. Without it the
   * rays that converge on a nearby wall stack their pools on the same spot and blow out to a
   * white disc, which is the opposite of the soft wash this is for.
   */
  private static final double MERGE_DISTANCE = 0.45;

  /** How much of a splash's weight comes from striking the surface square-on rather than obliquely. */
  private static final float INCIDENCE_FLOOR = 0.40f;

  /**
   * Distance at which a pool is at half strength, in blocks.
   *
   * <p>The falloff has to be a curve rather than a straight line to zero at {@link #MAX_DISTANCE}.
   * A linear ramp spends almost all of its range near nothing -- at five blocks it leaves about a
   * sixtieth of the light, which on a dark wall is not merely subtle, it is not there. This is the
   * inverse-square shape real light has, softened enough that the far wall of an ordinary room
   * still reads as lit.</p>
   */
  private static final float HALF_STRENGTH_DISTANCE = 3.0f;

  private static final float POOL_RADIUS_BASE = 0.30f;
  private static final float POOL_RADIUS_PER_BLOCK = 0.30f;

  /** A pool of light on one surface, positioned relative to the centre of the strobe's own block. */
  public static final class Splash {

    /** Pool centre, offset from the strobe block's centre, already lifted off the surface. */
    public final double offsetX;
    public final double offsetY;
    public final double offsetZ;
    /** The face the light landed on, which fixes the plane the pool is drawn in. */
    public final EnumFacing face;
    /** Pool radius in blocks. */
    public final float radius;
    /** Distance falloff times incidence, 0-1. The renderer scales alpha by this. */
    public final float weight;
    /** The air block in front of the surface, sampled to decide how dark it is there. */
    public final BlockPos lightPos;

    private Splash(double offsetX, double offsetY, double offsetZ, EnumFacing face, float radius,
        float weight, BlockPos lightPos) {
      this.offsetX = offsetX;
      this.offsetY = offsetY;
      this.offsetZ = offsetZ;
      this.face = face;
      this.radius = radius;
      this.weight = weight;
      this.lightPos = lightPos;
    }
  }

  private static final class Entry {

    private final Splash[] splashes;
    private final long computedAtTick;

    private Entry(Splash[] splashes, long computedAtTick) {
      this.splashes = splashes;
      this.computedAtTick = computedAtTick;
    }
  }

  private static final Splash[] NONE = new Splash[0];
  private static final Map<BlockPos, Entry> CACHE = new HashMap<>();

  private StrobeSurfaceProjection() {}

  /** Drops every cached projection. Called when the strobe registry is cleared on disconnect. */
  public static void clear() {
    CACHE.clear();
  }

  /**
   * The surfaces this strobe's beam reaches, recomputing only when the cached answer has aged out.
   *
   * @param world   the client world
   * @param pos     the strobe block
   * @param facing  the direction the device points
   * @param lens    the lens centre in the device's local frame, as {x, y, z} in block units
   *                measured from the block centre
   */
  public static Splash[] get(World world, BlockPos pos, EnumFacing facing, float[] lens) {
    long tick = world.getTotalWorldTime();
    Entry entry = CACHE.get(pos);
    if (entry != null && tick - entry.computedAtTick < CACHE_TTL_TICKS) {
      return entry.splashes;
    }
    if (CACHE.size() > CACHE_SOFT_LIMIT) {
      prune(tick);
    }
    Splash[] splashes = cast(world, pos, facing, lens);
    if (entry == null) {
      // Backdate the first stamp by a per-position amount so a corridor of devices falls due on
      // different ticks. Only the first one: backdating every time would leave the positions that
      // draw a near-full offset re-casting on every single frame.
      CACHE.put(pos, new Entry(splashes, tick - Math.floorMod(pos.hashCode(), CACHE_TTL_TICKS)));
    } else {
      CACHE.put(pos, new Entry(splashes, tick));
    }
    return splashes;
  }

  private static void prune(long tick) {
    Iterator<Map.Entry<BlockPos, Entry>> iterator = CACHE.entrySet().iterator();
    while (iterator.hasNext()) {
      if (tick - iterator.next().getValue().computedAtTick > CACHE_STALE_TICKS) {
        iterator.remove();
      }
    }
  }

  private static Splash[] cast(World world, BlockPos pos, EnumFacing facing, float[] lens) {
    Vec3d right = axis(facing, 0);
    Vec3d up = axis(facing, 1);
    Vec3d forward = new Vec3d(facing.getDirectionVec());

    // The lens sits in the device's local frame, where +Z runs back into the wall -- so its local
    // z contributes along -forward. Getting this the wrong way round starts every ray inside the
    // block the device is mounted on.
    Vec3d centre = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    Vec3d origin = centre
        .add(right.scale(lens[0]))
        .add(up.scale(lens[1]))
        .add(forward.scale(-lens[2]))
        .add(forward.scale(0.02));

    double tan = Math.tan(Math.toRadians(FAN_ANGLE_DEGREES));
    List<Splash> found = new ArrayList<>(RING_RAYS + 1);
    for (int i = 0; i <= RING_RAYS; i++) {
      Vec3d direction;
      if (i == 0) {
        direction = forward;
      } else {
        double phi = 2.0 * Math.PI * (i - 1) / RING_RAYS;
        direction = forward
            .add(right.scale(Math.cos(phi) * tan))
            .add(up.scale(Math.sin(phi) * tan))
            .normalize();
      }
      trace(world, pos, origin, direction, centre, found);
    }
    return found.isEmpty() ? NONE : found.toArray(new Splash[0]);
  }

  private static void trace(World world, BlockPos self, Vec3d origin, Vec3d direction,
      Vec3d blockCentre, List<Splash> found) {
    Vec3d start = origin;
    RayTraceResult hit = null;
    // The ray leaves from the lens, which is inside the device's own block. Most of these
    // appliances have a collision box that a trace starting inside it reports a hit on, and on the
    // ones whose box reaches further forward than their lens it would do exactly that -- painting
    // a pool of light onto the strobe itself and stopping the ray before it reached the room.
    for (int attempt = 0; attempt < 2; attempt++) {
      Vec3d end = origin.add(direction.scale(MAX_DISTANCE));
      hit = world.rayTraceBlocks(start, end, false, true, false);
      if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.sideHit == null) {
        return;
      }
      if (!hit.getBlockPos().equals(self)) {
        break;
      }
      start = hit.hitVec.add(direction.scale(0.02));
      hit = null;
    }
    if (hit == null) {
      return;
    }

    Vec3d point = hit.hitVec;
    double distance = origin.distanceTo(point);
    if (distance > MAX_DISTANCE) {
      return;
    }

    EnumFacing face = hit.sideHit;
    Vec3d normal = new Vec3d(face.getDirectionVec());
    // A ray travels into the face it hits, so the dot product is negative; how negative is how
    // square-on the light struck, and an oblique hit spreads the same light over more surface.
    float incidence = (float) Math.max(0.0, -direction.dotProduct(normal));
    double ratio = distance / HALF_STRENGTH_DISTANCE;
    float falloff = (float) (1.0 / (1.0 + ratio * ratio));
    float weight = falloff * (INCIDENCE_FLOOR + (1.0f - INCIDENCE_FLOOR) * incidence);
    if (weight <= 0.01f) {
      return;
    }

    for (Splash existing : found) {
      if (existing.face != face) {
        continue;
      }
      double dx = existing.offsetX - (point.x - blockCentre.x);
      double dy = existing.offsetY - (point.y - blockCentre.y);
      double dz = existing.offsetZ - (point.z - blockCentre.z);
      if (dx * dx + dy * dy + dz * dz < MERGE_DISTANCE * MERGE_DISTANCE) {
        return;
      }
    }

    // Sample the light in the air in front of the surface, not inside the block: a solid block's
    // own light value is always zero, which would report every surface as pitch dark.
    BlockPos lightPos = hit.getBlockPos().offset(face);
    // Lift the pool off the surface so it wins the depth test against the block it sits on.
    Vec3d lifted = point.add(normal.scale(0.012));
    found.add(new Splash(
        lifted.x - blockCentre.x, lifted.y - blockCentre.y, lifted.z - blockCentre.z,
        face, POOL_RADIUS_BASE + (float) distance * POOL_RADIUS_PER_BLOCK, weight, lightPos));
  }

  /**
   * The device's local right (index 0) and up (index 1) axes in world space.
   *
   * <p>These mirror {@code TileEntityFireAlarmStrobeRenderer.applyFacingRotation} exactly. They
   * have to: the renderer places the lens with those rotations and this places the beam that
   * leaves it, so if the two disagree a wide lens throws its light out sideways.</p>
   */
  private static Vec3d axis(EnumFacing facing, int index) {
    switch (facing) {
      case NORTH:
        return index == 0 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
      case SOUTH:
        return index == 0 ? new Vec3d(-1, 0, 0) : new Vec3d(0, 1, 0);
      case EAST:
        return index == 0 ? new Vec3d(0, 0, 1) : new Vec3d(0, 1, 0);
      case WEST:
        return index == 0 ? new Vec3d(0, 0, -1) : new Vec3d(0, 1, 0);
      case UP:
        return index == 0 ? new Vec3d(1, 0, 0) : new Vec3d(0, 0, 1);
      case DOWN:
      default:
        return index == 0 ? new Vec3d(1, 0, 0) : new Vec3d(0, 0, -1);
    }
  }
}
