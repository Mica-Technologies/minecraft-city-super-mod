package com.micatechnologies.minecraft.csm.powergrid.services;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A meter bank position: set side by side on a wall, facing the same way, the same block reads as
 * one bank of any length -- the electric meter centre's enclosure and wireway run on unbroken,
 * the gas meters hang off one header. {@link #LEFT} and {@link #RIGHT} (actual state only, never
 * stored; the model's west and east) say whether the bank carries on to that side, and the
 * multipart blockstate draws the bank's end pieces only where it stops: the enclosure's end
 * flanges, the gas header's cap at one end and its supply riser and regulator at the other.
 *
 * <p>Nothing here ticks and nothing is stored beyond the facing: sixteen states, one model
 * location.</p>
 *
 * @since 2026.9
 */
public class BlockUtilityRun extends BlockUtilityFixture {

  /** The bank carries on to the model's west. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** The bank carries on to the model's east. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  /**
   * Constructs a bank position.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockUtilityRun(String registryName, double[] box) {
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
    return state.withProperty(LEFT, continues(world, pos.offset(facing.rotateYCCW()), facing))
        .withProperty(RIGHT, continues(world, pos.offset(facing.rotateY()), facing));
  }

  /** Whether the same block, facing the same way, is at {@code at}. */
  private boolean continues(IBlockAccess world, BlockPos at, EnumFacing facing) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() == this && other.getValue(FACING) == facing;
  }
}
