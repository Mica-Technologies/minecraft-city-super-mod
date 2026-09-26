package com.micatechnologies.minecraft.csm.powergrid.sewer;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A piece of a stormwater outfall's headwall: the plain wall, the wall with the pipe's mouth,
 * and the wall with the pipe and its flap gate. Any of them beside or above another, facing the
 * same way, is one wall: its end faces are drawn only where it stops ({@link #LEFT},
 * {@link #RIGHT}) and its coping only where nothing of the wall is above ({@link #UP}), all
 * actual state. Thirty-two states.
 *
 * @since 2026.9
 */
public class BlockHeadwall extends BlockPrecastRun {

  /** Another piece of the headwall is on top of this one. */
  public static final PropertyBool UP = PropertyBool.create("up");

  /**
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockHeadwall(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(UP, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LEFT, RIGHT, UP);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return super.getActualState(state, world, pos)
        .withProperty(UP, joins(world, pos.up(), state.getValue(FACING)));
  }

  /** Every headwall piece joins every other, facing the same way. */
  @Override
  protected boolean joins(IBlockAccess world, BlockPos at, EnumFacing facing) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() instanceof BlockHeadwall && other.getValue(FACING) == facing;
  }
}
