package com.micatechnologies.minecraft.csm.transit.platform;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * The platform clock's tile entity. It stores nothing; it exists so that
 * {@link TileEntityPlatformClockRenderer} has something to draw the hands from.
 *
 * @since 2026.9
 */
public class TileEntityPlatformClock extends AbstractTileEntity {

  /** The cell, or a block round it for a large clock whose dial reaches past it. */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockPos p = getPos();
    AxisAlignedBB cell = new AxisAlignedBB(p.getX(), p.getY(), p.getZ(), p.getX() + 1,
        p.getY() + 1, p.getZ() + 1);
    return large() ? cell.grow(1.0) : cell;
  }

  /** Hands a finger long are not read from further away than this; a large clock's, further. */
  @Override
  public double getMaxRenderDistanceSquared() {
    double d = large() ? 96.0 : 48.0;
    return CsmPerformance.capRenderDistanceSq(d * d);
  }

  private boolean large() {
    return getBlockType() instanceof BlockPlatformClock
        && ((BlockPlatformClock) getBlockType()).getScale() > 1.0;
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
