package com.micatechnologies.minecraft.csm.transit.platform;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Platform furniture that joins into a run: set side by side, facing the same way, the same
 * block reads as one piece of any length. {@link #LEFT} and {@link #RIGHT} (actual state, seen
 * from in front of it: the model's west and east) say whether the run carries on to that side,
 * and the blockstate draws a piece's ends only where it stops -- the perch's end caps, the
 * bench's end armrests -- and the joint where it does not.
 *
 * @since 2026.9
 */
public class BlockPlatformRun extends BlockPlatformFixture {

  /** The run carries on to the model's west. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** The run carries on to the model's east. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  /**
   * Constructs a run piece.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockPlatformRun(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(LEFT, false).withProperty(RIGHT, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LEFT, RIGHT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    return state.withProperty(LEFT, continues(world, pos.offset(facing.rotateYCCW()), facing))
        .withProperty(RIGHT, continues(world, pos.offset(facing.rotateY()), facing));
  }

  /** Whether the same block, facing the same way, is at {@code at}. */
  private boolean continues(IBlockAccess world, BlockPos at, EnumFacing facing) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() == this && other.getValue(FACING) == facing;
  }
}
