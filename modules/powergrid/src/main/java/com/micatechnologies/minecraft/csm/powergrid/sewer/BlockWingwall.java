package com.micatechnologies.minecraft.csm.powergrid.sewer;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * An outfall's wingwall: a wall running downstream from the end of the headwall, its top
 * sloping down to the toe. It is two blocks long and one slope: the block against the headwall
 * (the model's south) reaches back to the headwall's face; a block with a wingwall behind it
 * ({@link #LOWER}, actual state) runs on down to the toe. Eight states.
 *
 * @since 2026.9
 */
public class BlockWingwall extends AbstractPrecastBlock {

  /** A wingwall facing the same way is behind this one: this is the lower, toe block. */
  public static final PropertyBool LOWER = PropertyBool.create("lower");

  private static final double X0 = 5 / 16.0;
  private static final double X1 = 11 / 16.0;

  private final AxisAlignedBB upperBox;
  private final AxisAlignedBB lowerBox;

  /**
   * @param registryName its registry name
   * @param upperTop     the upper block's highest top, in sixteenths
   * @param lowerTop     the lower block's highest top, in sixteenths
   */
  public BlockWingwall(String registryName, double upperTop, double lowerTop) {
    super(registryName, new double[]{5, 0, 0, 11, 16, 16});
    this.upperBox = new AxisAlignedBB(X0, 0, 0, X1, Math.min(1, upperTop / 16.0), 1);
    this.lowerBox = new AxisAlignedBB(X0, 0, 0, X1, lowerTop / 16.0, 1);
    setDefaultState(getDefaultState().withProperty(LOWER, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LOWER);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    IBlockState behind = world.getBlockState(pos.offset(facing.getOpposite()));
    return state.withProperty(LOWER,
        behind.getBlock() == this && behind.getValue(FACING) == facing);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    // asked with the stored state as often as the actual one, so read the neighbour here
    return getActualState(state, source, pos).getValue(LOWER) ? lowerBox : upperBox;
  }
}
