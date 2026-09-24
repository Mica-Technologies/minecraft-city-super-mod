package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A sofa: one seat a block, placed side by side into a run with its arms only at the run's two
 * ends. A {@link BlockSofaCorner} of the same fabric continues a run too, where its open side
 * meets the sofa, so sofas and a corner make an L. Right-click to sit, one sitter a block;
 * sneak to get up.
 *
 * @since 2026.9
 */
public class BlockSofa extends BlockResidentialRun {

  /** The top of the seat cushion, in sixteenths. */
  private static final double SEAT_TOP = 7.25;
  /** The middle of the seat cushion, forward of the block's middle, in sixteenths. */
  private static final double SEAT_FORWARD = 1.0;

  /**
   * Constructs a sofa.
   *
   * @param registryName its registry name, ending in its fabric
   * @param box          its box facing north, in sixteenths
   */
  public BlockSofa(String registryName, int[] box) {
    super(registryName, box, true, SEAT_TOP, SEAT_FORWARD);
  }

  /**
   * Continues past a sofa of the same block facing the same way, or past a corner of the same
   * fabric whose open side is toward this sofa: a corner facing {@code g} is open on its left
   * ({@code g.rotateYCCW()}) to a sofa facing {@code g}, which finds the corner on its right; and
   * open at its front ({@code g}) to a sofa facing {@code g.rotateYCCW()}, which finds the corner
   * on its left.
   */
  @Override
  protected boolean continues(IBlockAccess world, BlockPos pos, EnumFacing facing,
      EnumFacing side) {
    if (super.continues(world, pos, facing, side)) {
      return true;
    }
    IBlockState other = world.getBlockState(pos.offset(side));
    if (!(other.getBlock() instanceof BlockSofaCorner)
        || !((BlockSofaCorner) other.getBlock()).getFinish().equals(getFinish())) {
      return false;
    }
    EnumFacing g = other.getValue(FACING);
    if (side == facing.rotateY()) {
      return g == facing;
    }
    return g.rotateYCCW() == facing;
  }
}
