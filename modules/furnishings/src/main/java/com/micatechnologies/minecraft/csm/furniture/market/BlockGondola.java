package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.furniture.residential.BlockBookcase;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Gondola shelving: a block of store shelving, a pegboard back at +Z and two shelves of stock in
 * front of it, drawn by {@code gen_furniture_market.py}. It joins and stacks as the bookcase does
 * -- end uprights only where a run stops ({@link #LEFT}, {@link #RIGHT}), the base deck only at
 * the bottom of a stack and the top cap only at its head ({@link #UP}, {@link #DOWN}) -- but
 * with any gondola, whatever it is stocked with, so tins beside cereal over snacks read as one
 * run of shelving. Two runs set back to back make an island gondola, stocked on both sides.
 *
 * <p>Some stock (bottles, spray triggers) is a cutout, so it draws in the cutout layer. The
 * stock is part of the model: the shelving holds nothing.</p>
 *
 * @since 2026.9
 */
public class BlockGondola extends BlockBookcase {

  /**
   * Constructs a gondola.
   *
   * @param registryName its registry name, ending in what it is stocked with
   * @param box          its box facing north, in sixteenths
   */
  public BlockGondola(String registryName, int[] box) {
    super(registryName, box);
  }

  @Override
  protected boolean continues(IBlockAccess world, BlockPos pos, EnumFacing facing,
      EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    return other.getBlock() instanceof BlockGondola && other.getValue(FACING) == facing;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
