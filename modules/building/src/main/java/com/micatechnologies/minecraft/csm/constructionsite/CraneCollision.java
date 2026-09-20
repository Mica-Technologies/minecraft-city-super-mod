package com.micatechnologies.minecraft.csm.constructionsite;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.GetCollisionBoxesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Makes the parts of a tower crane you would stand on solid: the slewing deck, the counter-jib
 * walkway with its winch and ballast, the cab, and the jib out to its tip.
 *
 * <p>The crane head's renderer draws all of that, but a renderer has no collision, and the head
 * block is one cell while the crane is up to eighty blocks across. 1.12 only asks blocks within
 * one cell of an entity for collision, so the head cannot supply it either. This adds it from
 * {@code GetCollisionBoxesEvent} instead, for every crane whose reach the queried box is inside.
 * It is registered on both sides so the server agrees with the client about where a player can
 * stand.</p>
 *
 * <p>The parts follow the slew. At a slew that is a multiple of a right angle each part is one
 * box, exact; at any other slew a part is filled with small squares along it, since a box cannot
 * turn, and only the squares near the queried box are made. The sizes mirror {@link
 * CraneGeometry}: change a part there, change it here.</p>
 *
 * <p>The deck is solid only to something standing on it and not sneaking, like the scaffold's, so
 * a player climbs up through it from the mast and sneaks back down into it. Everything else is
 * solid from every side, which is what lets a player step from the deck up onto a walkway. Parts
 * that would reach into the mast's column are left out, so none of them catches a climber's
 * head.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class CraneCollision {

  /** Loaded crane heads, per world. Each world's set is only touched from that world's thread. */
  private static final Map<World, Set<TileEntityCraneHead>> HEADS =
      Collections.synchronizedMap(new WeakHashMap<>());

  /**
   * Collision queries are numerous (including for item entities), while a crane can reach 84
   * blocks.  A 96-block grid keeps the candidate set local without having to re-index a head when
   * its configured jib length changes.
   */
  private static final int GRID_SIZE = 96;

  /** Largest possible horizontal reach, including the one-block query margin. */
  private static final double MAX_REACH = TileEntityCraneHead.MAX_JIB * 2.0 + 6.0;

  /** Heads by their anchor's grid cell, alongside HEADS which serves the mast-climb lookup. */
  private static final Map<World, Map<Long, Set<TileEntityCraneHead>>> GRID =
      Collections.synchronizedMap(new WeakHashMap<>());

  /** How far above a surface an entity's feet may be and still count as standing on it. */
  private static final double STANDING_TOLERANCE = 1.0E-3;

  /** Spacing of the squares that fill a slewed part, in blocks at scale 1. */
  private static final double STEP = 0.2;

  private CraneCollision() {
  }

  /**
   * Registers the handler. Call from pre-initialization, on both sides.
   *
   * @since 1.0
   */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new CraneCollision());
  }

  static void add(TileEntityCraneHead head) {
    HEADS.computeIfAbsent(head.getWorld(), w -> ConcurrentHashMap.newKeySet()).add(head);
    GRID.computeIfAbsent(head.getWorld(), w -> new ConcurrentHashMap<>())
        .computeIfAbsent(gridKey(head.getPos().getX(), head.getPos().getZ()),
            k -> ConcurrentHashMap.newKeySet())
        .add(head);
  }

  static void remove(TileEntityCraneHead head) {
    Set<TileEntityCraneHead> heads = HEADS.get(head.getWorld());
    if (heads != null) {
      heads.remove(head);
    }
    Map<Long, Set<TileEntityCraneHead>> cells = GRID.get(head.getWorld());
    if (cells != null) {
      long key = gridKey(head.getPos().getX(), head.getPos().getZ());
      Set<TileEntityCraneHead> cell = cells.get(key);
      if (cell != null) {
        cell.remove(head);
        if (cell.isEmpty()) {
          cells.remove(key, cell);
        }
      }
    }
  }

  private static Set<TileEntityCraneHead> heads(World world) {
    Set<TileEntityCraneHead> heads = HEADS.get(world);
    return heads == null ? Collections.emptySet() : heads;
  }

  private static long gridKey(double x, double z) {
    return ((long) MathHelper.floor(x / GRID_SIZE) << 32)
        ^ (MathHelper.floor(z / GRID_SIZE) & 0xFFFFFFFFL);
  }

  /** Height of the top of the slewing deck above the head block's floor. */
  static double deckTop(TileEntityCraneHead head) {
    return 0.45 * head.getScale();
  }

  /**
   * Whether a climber has come out of the top of a mast into the cell beside or at the head, and
   * is not yet above the deck -- the climb handler keeps them climbing until they are, so they can
   * step onto it. Without this a player climbing a 2x2 quarter the head is not on comes out into
   * air, drops back into the mast, and bobs there.
   *
   * @param world the world
   * @param feet  the cell the entity's feet are in
   * @param x     the entity's x
   * @param z     the entity's z
   * @param minY  the bottom of the entity's box
   *
   * @return whether to keep climbing
   *
   * @since 1.0
   */
  public static boolean isClimbingOut(World world, BlockPos feet, double x, double z,
      double minY) {
    if (!(world.getBlockState(feet.down()).getBlock() instanceof BlockCraneMast)) {
      return false;
    }
    for (TileEntityCraneHead head : heads(world)) {
      BlockPos p = head.getPos();
      if (p.getY() != feet.getY() || head.isInvalid()) {
        continue;
      }
      double half = 0.5 * head.getScale();
      double cx = p.getX() + 0.5 + head.getCentreX();
      double cz = p.getZ() + 0.5 + head.getCentreZ();
      if (Math.abs(x - cx) < half && Math.abs(z - cz) < half
          && minY < p.getY() + deckTop(head) + 0.05) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds the boxes of every crane near the queried box.
   *
   * @param event the collision query
   *
   * @since 1.0
   */
  @SubscribeEvent
  public void onGetCollisionBoxes(GetCollisionBoxesEvent event) {
    Entity entity = event.getEntity();
    if (entity == null) {
      return;
    }
    Map<Long, Set<TileEntityCraneHead>> cells = GRID.get(event.getWorld());
    if (cells == null || cells.isEmpty()) {
      return;
    }
    AxisAlignedBB box = event.getAabb();
    int minX = MathHelper.floor((box.minX - MAX_REACH) / GRID_SIZE);
    int maxX = MathHelper.floor((box.maxX + MAX_REACH) / GRID_SIZE);
    int minZ = MathHelper.floor((box.minZ - MAX_REACH) / GRID_SIZE);
    int maxZ = MathHelper.floor((box.maxZ + MAX_REACH) / GRID_SIZE);
    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        Set<TileEntityCraneHead> cell = cells.get(((long) x << 32) ^ (z & 0xFFFFFFFFL));
        if (cell == null) {
          continue;
        }
        for (TileEntityCraneHead head : cell) {
          if (!head.isInvalid()) {
            new Query(head, entity, box, event.getCollisionBoxesList()).run();
          }
        }
      }
    }
  }

  /** One crane against one queried box. */
  private static final class Query {

    private final TileEntityCraneHead head;
    private final Entity entity;
    private final AxisAlignedBB box;
    private final List<AxisAlignedBB> out;
    private final double s;
    private final double cx;
    private final double cy;
    private final double cz;
    private final double cos;
    private final double sin;
    private final boolean square;

    Query(TileEntityCraneHead head, Entity entity, AxisAlignedBB box, List<AxisAlignedBB> out) {
      this.head = head;
      this.entity = entity;
      this.box = box;
      this.out = out;
      this.s = head.getScale();
      BlockPos p = head.getPos();
      this.cx = p.getX() + 0.5 + head.getCentreX();
      this.cy = p.getY();
      this.cz = p.getZ() + 0.5 + head.getCentreZ();
      double a = Math.toRadians(head.getSlew());
      this.cos = Math.cos(a);
      this.sin = Math.sin(a);
      double quarter = head.getSlew() / 90.0;
      this.square = Math.abs(quarter - Math.round(quarter)) < 1.0E-4;
    }

    void run() {
      // Everything the crane stands on is within its reach, and between the bottom of the
      // ballast and the cab roof.
      double r = head.getReach() + 1.0;
      if (box.maxX < cx - r || box.minX > cx + r || box.maxZ < cz - r || box.minZ > cz + r
          || box.maxY < cy - 1.5 * s || box.minY > cy + 1.5 * s) {
        return;
      }
      deck();
      double y0 = 0.55 * s;
      boolean luffing = head.getModel() == CraneModel.LUFFING;
      double l = head.getJibLength();
      double lc = luffing ? Math.max(3.0 * s, l / 4.0) : Math.max(4.0 * s, l / 3.0);
      // Counter-jib walkway, to the rails' centre lines.
      part(-lc, -0.5 * s, -0.4 * s, 0.4 * s, y0, y0 + 0.1 * s);
      // The winch on it.
      double wx = -lc * 0.45;
      part(wx - 0.45 * s, wx + 0.45 * s, -0.3 * s, 0.3 * s, y0 + 0.1 * s, y0 + 0.65 * s);
      // The ballast stack at its end.
      part(-lc + 0.05 * s, -lc + 1.05 * s, -0.5 * s, 0.5 * s, y0 - 1.2 * s, y0 + 0.44 * s);
      // The cab.
      part(0.05 * s, 0.9 * s, 0.5 * s, 1.25 * s, y0 - 0.35 * s, y0 + 0.55 * s);
      // The jib's bottom chords, as a walkway to the tip. A luffing jib is raked too steeply to
      // walk, so it has none.
      if (!luffing) {
        part(0.5 * s, l, -0.4 * s, 0.4 * s, y0 - 0.05 * s, y0 + 0.05 * s);
      }
    }

    /** The slewing deck over the mast: solid from above, and not to a sneaking entity. */
    private void deck() {
      double top = cy + deckTop(head);
      if (entity.isSneaking() || entity.getEntityBoundingBox().minY < top - STANDING_TOLERANCE) {
        return;
      }
      double h = 0.55 * s;
      offer(new AxisAlignedBB(cx - h, cy + 0.25 * s, cz - h, cx + h, top, cz + h), false);
    }

    /**
     * A part, as the box x0..x1, z0..z1 in the crane's frame (the jib along +x), between
     * {@code yb} and {@code yt} above the head block's floor.
     */
    private void part(double x0, double x1, double z0, double z1, double yb, double yt) {
      double y0 = cy + yb;
      double y1 = cy + yt;
      if (box.maxY < y0 || box.minY > y1) {
        return;
      }
      if (square) {
        double ax = wx(x0, z0);
        double az = wz(x0, z0);
        double bx = wx(x1, z1);
        double bz = wz(x1, z1);
        offer(new AxisAlignedBB(Math.min(ax, bx), y0, Math.min(az, bz), Math.max(ax, bx), y1,
            Math.max(az, bz)), true);
        return;
      }
      // Fill the turned part with squares big enough to cover their own cell at any angle, made
      // only where the queried box is: its corners taken into the crane's frame bound them.
      double d = STEP * s;
      double h = d * 0.71;
      double lx0 = Double.MAX_VALUE;
      double lx1 = -Double.MAX_VALUE;
      double lz0 = Double.MAX_VALUE;
      double lz1 = -Double.MAX_VALUE;
      for (double px : new double[]{box.minX - h, box.maxX + h}) {
        for (double pz : new double[]{box.minZ - h, box.maxZ + h}) {
          double ux = (px - cx) * cos + (pz - cz) * sin;
          double uz = -(px - cx) * sin + (pz - cz) * cos;
          lx0 = Math.min(lx0, ux);
          lx1 = Math.max(lx1, ux);
          lz0 = Math.min(lz0, uz);
          lz1 = Math.max(lz1, uz);
        }
      }
      int nx = Math.max(1, (int) Math.ceil((x1 - x0) / d));
      int nz = Math.max(1, (int) Math.ceil((z1 - z0) / d));
      int i0 = Math.max(0, MathHelper.floor((lx0 - x0) / d));
      int i1 = Math.min(nx - 1, MathHelper.ceil((lx1 - x0) / d));
      int k0 = Math.max(0, MathHelper.floor((lz0 - z0) / d));
      int k1 = Math.min(nz - 1, MathHelper.ceil((lz1 - z0) / d));
      for (int i = i0; i <= i1; i++) {
        double lx = Math.min(x0 + (i + 0.5) * d, x1);
        for (int k = k0; k <= k1; k++) {
          double lz = Math.min(z0 + (k + 0.5) * d, z1);
          double px = wx(lx, lz);
          double pz = wz(lx, lz);
          offer(new AxisAlignedBB(px - h, y0, pz - h, px + h, y1, pz + h), true);
        }
      }
    }

    private double wx(double lx, double lz) {
      return cx + lx * cos - lz * sin;
    }

    private double wz(double lx, double lz) {
      return cz + lx * sin + lz * cos;
    }

    /**
     * Adds {@code b} if it meets the queried box; with {@code clear}, not if it reaches into the
     * mast's column, where a climber would hit it.
     */
    private void offer(AxisAlignedBB b, boolean clear) {
      if (!b.intersects(box)) {
        return;
      }
      if (clear) {
        double half = 0.5 * s - 1.0E-3;
        if (b.maxX > cx - half && b.minX < cx + half && b.maxZ > cz - half
            && b.minZ < cz + half) {
          return;
        }
      }
      out.add(b);
    }
  }
}
