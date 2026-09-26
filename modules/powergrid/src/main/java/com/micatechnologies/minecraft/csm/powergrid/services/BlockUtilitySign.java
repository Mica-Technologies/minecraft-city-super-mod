package com.micatechnologies.minecraft.csm.powergrid.services;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A warning or notice sign hung on what the player was looking at: a wall, or a fence. The gas
 * yard's and the cell site's signs go on their compound's chain-link fence, whose mesh runs
 * through the middle of its block rather than along the face, so against anything that does not
 * offer a solid face ({@link #FENCE}, actual state from the block behind) the plate is drawn
 * reaching back into that block to sit against the mesh, wired to it. Against a wall it sits on
 * the wall's face like a utility label.
 *
 * <p>Nothing is stored beyond the facing: eight states, no tile entity.</p>
 *
 * @since 2026.9
 */
public class BlockUtilitySign extends BlockUtilityFixture {

  /** The block behind is not a solid face (a fence): the plate hangs on its mesh. */
  public static final PropertyBool FENCE = PropertyBool.create("fence");

  /** The box in fence mode, facing north: a sliver at the back of the cell, to click. */
  private static final AxisAlignedBB FENCE_BOX = new AxisAlignedBB(2 / 16.0, 4 / 16.0,
      15 / 16.0, 14 / 16.0, 14 / 16.0, 1);

  /**
   * @param registryName its registry name
   * @param box          its box facing north on a wall, in sixteenths
   */
  public BlockUtilitySign(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(FENCE, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, FENCE);
  }

  /** Whether the block behind a sign at {@code pos} facing {@code facing} is a fence. */
  static boolean onFence(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    BlockPos behind = pos.offset(facing.getOpposite());
    IBlockState state = world.getBlockState(behind);
    if (state.getMaterial() == Material.AIR) {
      return false;
    }
    return state.getBlockFaceShape(world, behind, facing) != BlockFaceShape.SOLID;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return super.getActualState(state, world, pos)
        .withProperty(FENCE, onFence(world, pos, state.getValue(FACING)));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return onFence(source, pos, state.getValue(FACING)) ? FENCE_BOX
        : super.getBlockBoundingBox(state, source, pos);
  }
}
