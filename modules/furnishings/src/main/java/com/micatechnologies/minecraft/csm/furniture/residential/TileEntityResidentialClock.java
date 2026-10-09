package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * The tile entity of a clock whose time is drawn by {@code TileEntityResidentialClockRenderer}:
 * the bedside digital clock and the wall clock. It stores nothing; it exists so the renderer has
 * something to draw from, and it holds the renderer's per-clock answers so nothing is worked out
 * again every frame: the time string (rebuilt when the minute changes) and, for a clock standing
 * on furniture, how far down it stands (read from the world at most once a second, since
 * placing a table under it fires nothing here).
 *
 * @since 2026.9
 */
public class TileEntityResidentialClock extends AbstractTileEntity {

  /** Client only: the minute of the day the text was made for, or -1. */
  public transient long textMinute = -1;
  /** Client only: the time as shown. */
  public transient String text = "";
  /** Client only: the text's width in font units. */
  public transient int textWidth;
  /** Client only: how far down the clock stands, in sixteenths. */
  public transient double drop;
  /** Client only: the world time the drop was read at, or -1. */
  public transient long dropReadAt = -1;

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockPos p = getPos();
    return new AxisAlignedBB(p.getX(), p.getY() - 1, p.getZ(), p.getX() + 1, p.getY() + 1,
        p.getZ() + 1);
  }

  /** A clock's face is small: past this there is nothing to read. */
  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(32.0 * 32.0);
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
