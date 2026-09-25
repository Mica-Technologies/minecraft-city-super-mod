package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import com.micatechnologies.minecraft.csm.codeutils.SignShift;
import com.micatechnologies.minecraft.csm.trafficsigns.AbstractBlockSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * What a bus stop sign's renderer needs from the world, looked up once a second rather than every
 * frame: the sign's facing and the {@link SignShift} the road sign system has put it in. The
 * shift is worked out from the neighbours (a signal arm behind, a span wire above, a partner
 * facing the other way), which is several block lookups, and it changes only when a neighbour
 * does, so the answer expires rather than being asked for on every frame. Client-side state
 * only; nothing here is saved.
 *
 * @since 2026.9
 */
public abstract class AbstractTileEntityBusStopSign extends AbstractTileEntity {

  /** How long a looked-up answer is kept, in ticks. */
  protected static final long VIEW_TICKS = 20;

  private long viewCheckedAt = Long.MIN_VALUE;
  private DirectionEight viewFacing = DirectionEight.N;
  private SignShift viewShift = SignShift.NONE;

  /**
   * Looks the facing and the shift up again if the last answer is older than
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
    if (state.getBlock() instanceof AbstractBlockSign) {
      IBlockState actual = state.getActualState(world, pos);
      viewFacing = actual.getValue(AbstractBlockSign.FACING);
      viewShift = actual.getValue(AbstractBlockSign.SHIFT);
    }
    refreshMore();
  }

  /** Anything else a subclass looks up on the same schedule. */
  protected void refreshMore() {
  }

  /**
   * The sign's facing, as last looked up.
   *
   * @return the facing
   */
  public DirectionEight getViewFacing() {
    return viewFacing;
  }

  /**
   * The sign's shift, as last looked up.
   *
   * @return the shift
   */
  public SignShift getViewShift() {
    return viewShift;
  }

  /**
   * Reaches the block behind (a back-to-back plate is drawn a block and more behind its own) and
   * the flag's top, which stands above its block.
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos.getX() - 2, pos.getY() - 1, pos.getZ() - 2, pos.getX() + 3,
        pos.getY() + 3, pos.getZ() + 3);
  }
}
