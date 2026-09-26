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
 * A precast piece that joins the same piece beside it into one run: the curb inlet (one long
 * opening under one curb angle) and the emergency spillway's crest. {@link #LEFT} and
 * {@link #RIGHT} (actual state only; the model's west and east) say whether the run carries on
 * to that side, and the multipart blockstate draws the ends -- the inlet's end walls, the
 * spillway's training walls -- only where it stops. Sixteen states, nothing stored but the
 * facing.
 *
 * @since 2026.9
 */
public class BlockPrecastRun extends AbstractPrecastBlock {

  /** The run carries on to the model's west. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** The run carries on to the model's east. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  /**
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockPrecastRun(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(LEFT, false).withProperty(RIGHT, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LEFT, RIGHT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    return state.withProperty(LEFT, joins(world, pos.offset(facing.rotateYCCW()), facing))
        .withProperty(RIGHT, joins(world, pos.offset(facing.rotateY()), facing));
  }

  /**
   * Whether a piece at {@code at} facing {@code facing} carries this one's run on: by default
   * the same block facing the same way.
   */
  protected boolean joins(IBlockAccess world, BlockPos at, EnumFacing facing) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() == this && other.getValue(FACING) == facing;
  }
}
