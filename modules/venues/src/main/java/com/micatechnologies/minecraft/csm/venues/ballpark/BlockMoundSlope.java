package com.micatechnologies.minecraft.csm.venues.ballpark;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A cell of the pitcher's mound round the rubber ({@code pitchers_mound_slope}): a ground block
 * whose top is the mound's surface over it, a heightfield up to 10 in (4 px) over the block, the
 * packing clay inside the mound's 18 ft circle and the infield grass outside it. It finds the
 * {@link BlockPitchersMound} within three blocks, as a painted field cell finds its anchor, and
 * shows its cell of the mound turned with it.
 *
 * <p>Nothing is stored. Its collision box rises to the cell's highest point, so a player walks
 * up the mound rather than through it.</p>
 *
 * @since 2026.10
 */
public class BlockMoundSlope extends BlockBallparkPainted {

  private final int[] heights;

  /**
   * Constructs the mound's slope.
   *
   * @param registryName its registry name
   * @param family       the mound's cells
   * @param heights      each cell's highest point, in pixels
   */
  public BlockMoundSlope(String registryName, BallparkFamily family, int[] heights) {
    super(registryName, family, Surface.CLAY);
    this.heights = heights;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    int c = find(source, pos)[0];
    int h = c >= 0 && c < heights.length ? heights[c] : 0;
    return h <= 0 ? SQUARE_BOUNDING_BOX : new AxisAlignedBB(0, 0, 0, 1, 1 + h / 16.0, 1);
  }
}
