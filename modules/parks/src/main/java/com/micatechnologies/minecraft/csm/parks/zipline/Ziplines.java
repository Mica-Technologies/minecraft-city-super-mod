package com.micatechnologies.minecraft.csm.parks.zipline;

import com.micatechnologies.minecraft.csm.CsmConfig;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * Stringing and taking down zipline lines, for the linker and {@code /csmzipline}: the checks a
 * line must pass, and the links both anchors keep.
 *
 * @version 1.0
 * @since 2026.11
 */
public final class Ziplines {

  /** Step along the line when checking it is clear. */
  private static final double CHECK_STEP = 0.25;
  /** Half a rider's width, for the clearance check. */
  private static final double RIDER_HALF_WIDTH = 0.3;
  /** How far below the stop point a landing floor may be. */
  private static final int LANDING_SEARCH = 4;

  private Ziplines() {
  }

  /**
   * Why a line cannot be strung, or null if it can.
   *
   * @param world   the world
   * @param launch  the launch anchor
   * @param landing the landing anchor
   *
   * @return a translatable reason, or null
   *
   * @since 1.0
   */
  @Nullable
  public static TextComponentTranslation check(World world, BlockPos launch, BlockPos landing) {
    if (!(anchorAt(world, launch) instanceof TileEntityZiplineAnchor)
        || !isAnchor(world, launch, true)) {
      return new TextComponentTranslation("csm.zipline.not_launch", fmt(launch));
    }
    if (!(anchorAt(world, landing) instanceof TileEntityZiplineAnchor)
        || !isAnchor(world, landing, false)) {
      return new TextComponentTranslation("csm.zipline.not_landing", fmt(landing));
    }
    ZiplineLine line = ZiplineLine.of(launch, landing);
    int max = CsmConfig.getZiplineMaxLength();
    if (line.span() > max) {
      return new TextComponentTranslation("csm.zipline.too_long",
          String.format("%.0f", line.span()), max);
    }
    if (line.length() < ZiplineLine.MIN_LENGTH) {
      return new TextComponentTranslation("csm.zipline.too_short");
    }
    BlockPos blocked = firstObstruction(world, line);
    if (blocked != null) {
      return new TextComponentTranslation("csm.zipline.blocked", fmt(blocked));
    }
    if (landingSpot(world, line) == null) {
      return new TextComponentTranslation("csm.zipline.no_landing");
    }
    return null;
  }

  /**
   * Strings a line, after {@link #check}: each anchor keeps the other, and anything either was
   * linked to before is let go.
   *
   * @since 1.0
   */
  public static void link(World world, BlockPos launch, BlockPos landing) {
    TileEntityZiplineAnchor a = (TileEntityZiplineAnchor) anchorAt(world, launch);
    TileEntityZiplineAnchor b = (TileEntityZiplineAnchor) anchorAt(world, landing);
    if (a == null || b == null) {
      return;
    }
    unlink(world, launch);
    unlink(world, landing);
    a.setLink(landing);
    b.setLink(launch);
  }

  /**
   * Takes down the line an anchor is on, at both ends.
   *
   * @return whether there was one
   *
   * @since 1.0
   */
  public static boolean unlink(World world, BlockPos anchor) {
    TileEntity te = anchorAt(world, anchor);
    if (!(te instanceof TileEntityZiplineAnchor)) {
      return false;
    }
    TileEntityZiplineAnchor a = (TileEntityZiplineAnchor) te;
    BlockPos other = a.getLink();
    if (other == null) {
      return false;
    }
    a.setLink(null);
    TileEntity o = world.isBlockLoaded(other) ? world.getTileEntity(other) : null;
    if (o instanceof TileEntityZiplineAnchor
        && anchor.equals(((TileEntityZiplineAnchor) o).getLink())) {
      ((TileEntityZiplineAnchor) o).setLink(null);
    }
    return true;
  }

  /**
   * The first block in the way of a rider, from where they clip on to where they are set down:
   * anything solid in the trolley's path along the cable or in the body hanging below it.
   *
   * @return the block, or null if the line is clear
   *
   * @since 1.0
   */
  @Nullable
  public static BlockPos firstObstruction(World world, ZiplineLine line) {
    BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
    for (double s = line.startDistance(); s <= line.stopDistance(); s += CHECK_STEP) {
      Vec3d c = line.point(s);
      AxisAlignedBB body = new AxisAlignedBB(c.x - RIDER_HALF_WIDTH,
          c.y - ZiplineLine.RIDER_DROP + 0.05, c.z - RIDER_HALF_WIDTH, c.x + RIDER_HALF_WIDTH,
          c.y + 0.25, c.z + RIDER_HALF_WIDTH);
      for (int x = MathHelper.floor(body.minX); x <= MathHelper.floor(body.maxX); x++) {
        for (int y = MathHelper.floor(body.minY); y <= MathHelper.floor(body.maxY); y++) {
          for (int z = MathHelper.floor(body.minZ); z <= MathHelper.floor(body.maxZ); z++) {
            p.setPos(x, y, z);
            if (p.equals(line.launch()) || p.equals(line.landing())) {
              continue;
            }
            if (!world.isBlockLoaded(p)) {
              return p.toImmutable();
            }
            IBlockState state = world.getBlockState(p);
            AxisAlignedBB box = state.getCollisionBoundingBox(world, p);
            if (box != null && box != net.minecraft.block.Block.NULL_AABB
                && box.offset(p).intersects(body)) {
              return p.toImmutable();
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Where a rider is set down: on the first floor at or below the stop point, with room to stand.
   *
   * @return the feet position, or null if there is no floor within reach
   *
   * @since 1.0
   */
  @Nullable
  public static Vec3d landingSpot(World world, ZiplineLine line) {
    Vec3d c = line.point(line.stopDistance());
    double feet = c.y - ZiplineLine.RIDER_DROP;
    BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
    int top = MathHelper.floor(feet + 0.5);
    for (int y = top; y >= top - LANDING_SEARCH; y--) {
      p.setPos(MathHelper.floor(c.x), y - 1, MathHelper.floor(c.z));
      IBlockState below = world.getBlockState(p);
      AxisAlignedBB floor = below.getCollisionBoundingBox(world, p);
      if (floor == null || floor == net.minecraft.block.Block.NULL_AABB) {
        continue;
      }
      double standY = y - 1 + floor.maxY;
      if (standY > feet + 0.6) {
        continue;
      }
      if (passable(world, new BlockPos(p.getX(), y, p.getZ()))
          && passable(world, new BlockPos(p.getX(), y + 1, p.getZ()))) {
        return new Vec3d(c.x, standY, c.z);
      }
    }
    return null;
  }

  private static boolean passable(World world, BlockPos pos) {
    IBlockState s = world.getBlockState(pos);
    AxisAlignedBB box = s.getCollisionBoundingBox(world, pos);
    return box == null || box == net.minecraft.block.Block.NULL_AABB;
  }

  @Nullable
  static TileEntity anchorAt(World world, BlockPos pos) {
    return world.isBlockLoaded(pos) ? world.getTileEntity(pos) : null;
  }

  private static boolean isAnchor(World world, BlockPos pos, boolean launch) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockZiplineAnchor
        && ((BlockZiplineAnchor) s.getBlock()).isLaunch() == launch;
  }

  static String fmt(BlockPos p) {
    return p.getX() + " " + p.getY() + " " + p.getZ();
  }
}
