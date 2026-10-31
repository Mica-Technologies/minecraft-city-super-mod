package com.micatechnologies.minecraft.csm.venues.arena;

import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The tile entity of a {@link BlockArenaFloorAnchor}: no data, no ticking, no renderer. On the
 * client it asks for the painted cells round its anchor to be drawn again whenever the anchor
 * is loaded, changed or removed, since a block change only redraws its own chunk section and a
 * picture reaches up to nine blocks into the next one.
 *
 * @since 2026.10
 */
public class TileEntityArenaFloorAnchor extends TileEntity {

  /** Further than any picture reaches from its anchor. */
  private static final int REACH = 10;

  @Override
  public void onLoad() {
    super.onLoad();
    redraw();
  }

  @Override
  public void updateContainingBlockInfo() {
    super.updateContainingBlockInfo();
    redraw();
  }

  @Override
  public void invalidate() {
    super.invalidate();
    redraw();
  }

  @Override
  public boolean shouldRefresh(World world, BlockPos pos, @Nonnull IBlockState oldState,
      @Nonnull IBlockState newState) {
    return oldState.getBlock() != newState.getBlock();
  }

  private void redraw() {
    if (world != null && world.isRemote && pos != null) {
      world.markBlockRangeForRenderUpdate(pos.add(-REACH, 0, -REACH), pos.add(REACH, 0, REACH));
    }
  }
}
