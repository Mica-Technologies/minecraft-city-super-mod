package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A bookcase: a block of shelving, 0.34 m deep against the wall behind it, with books on its
 * shelves. Placed side by side it joins into one long bookcase ({@link #LEFT}, {@link #RIGHT});
 * stacked it grows taller, with one plinth at the bottom of the stack and one top at its head
 * ({@link #UP}, {@link #DOWN}: the same block, facing the same way, above or below). All four
 * are actual state; the multipart blockstate draws the side panels full height where the stack
 * goes on up.
 *
 * @since 2026.9
 */
public class BlockBookcase extends BlockResidentialRun {

  /** Another of this bookcase stands on top. */
  public static final PropertyBool UP = PropertyBool.create("up");
  /** This one stands on another. */
  public static final PropertyBool DOWN = PropertyBool.create("down");

  /**
   * Constructs a bookcase.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockBookcase(String registryName, int[] box) {
    super(registryName, box, false);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LEFT, RIGHT, UP, DOWN);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing facing = s.getValue(FACING);
    return s.withProperty(UP, continues(world, pos, facing, EnumFacing.UP))
        .withProperty(DOWN, continues(world, pos, facing, EnumFacing.DOWN));
  }
}
