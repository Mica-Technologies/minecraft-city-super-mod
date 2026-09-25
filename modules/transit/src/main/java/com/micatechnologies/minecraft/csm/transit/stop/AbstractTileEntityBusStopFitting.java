package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * What a bus stop fitting's renderer needs from the world, looked up once a second rather than
 * every frame: the facing, and how far the stack settles. The settling is read from the block
 * under the stack's bottom, possibly several blocks down, and a chunk loading under the stack
 * fires no neighbour change, so the answer expires rather than waiting to be told. Client-side
 * state only; nothing here is saved.
 *
 * @since 2026.9
 */
public abstract class AbstractTileEntityBusStopFitting extends AbstractTileEntity {

  /** How long a looked-up answer is kept, in ticks. */
  protected static final long VIEW_TICKS = 20;

  private long viewCheckedAt = Long.MIN_VALUE;
  private EnumFacing viewFacing = EnumFacing.NORTH;
  private double viewOffset;

  /**
   * Looks the facing and the settling up again if the last answer is older than
   * {@link #VIEW_TICKS}. Renderers call this once per frame, before reading them.
   */
  public void refreshView() {
    if (world == null) {
      return;
    }
    long now = world.getTotalWorldTime();
    if (viewCheckedAt != Long.MIN_VALUE && now - viewCheckedAt < VIEW_TICKS
        && now >= viewCheckedAt) {
      return;
    }
    viewCheckedAt = now;
    IBlockState state = world.getBlockState(pos);
    if (state.getPropertyKeys().contains(AbstractBlockBusStopStack.FACING)) {
      viewFacing = state.getValue(AbstractBlockBusStopStack.FACING);
    }
    viewOffset = BusStopStack.offsetAt(world, pos);
    refreshMore();
  }

  /** Anything else a subclass looks up on the same schedule. */
  protected void refreshMore() {
  }

  /**
   * The fitting's facing, as last looked up.
   *
   * @return the facing
   */
  public EnumFacing getViewFacing() {
    return viewFacing;
  }

  /**
   * How far the stack settles, in blocks, as last looked up.
   *
   * @return the offset, at most zero
   */
  public double getViewOffset() {
    return viewOffset;
  }

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 2,
        pos.getY() + 2, pos.getZ() + 2);
  }
}
