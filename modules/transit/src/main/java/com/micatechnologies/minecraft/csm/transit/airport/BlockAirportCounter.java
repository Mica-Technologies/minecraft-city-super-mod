package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A piece of an airport counter that joins into a run with the pieces of its family beside it,
 * facing the same way: the check-in desks and the bag drop scales between them are one family
 * ({@code "checkin"}), the gate desks another ({@code "gate"}). {@link #LEFT} and {@link #RIGHT}
 * (actual state; the model's west and east, as {@code BlockPlatformRun} names them) say whether
 * the run carries on to that side, and the blockstate draws a desk's end panel only where the run
 * stops, so a row of desks and scales reads as one counter.
 *
 * @since 2026.9
 */
public class BlockAirportCounter extends BlockPlatformFixture {

  /** The run carries on to the model's west. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** The run carries on to the model's east. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  private final String family;

  /**
   * Constructs a counter piece.
   *
   * @param registryName its registry name
   * @param family       the pieces it joins with
   * @param box          its box facing north, in sixteenths
   */
  public BlockAirportCounter(String registryName, String family, double[] box) {
    super(registryName, box);
    this.family = family;
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

  /** Whether a counter of the same family, facing the same way, is at {@code at}. */
  private boolean continues(IBlockAccess world, BlockPos at, EnumFacing facing) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() instanceof BlockAirportCounter
        && ((BlockAirportCounter) other.getBlock()).family.equals(family)
        && other.getValue(FACING) == facing;
  }
}
