package com.micatechnologies.minecraft.csm.furniture.residential;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The corner base cabinet, which turns a countertop run through 90 degrees as the sofa corner
 * turns a sofa: its backs are behind it and to its right, and it is open on its left and at its
 * front. A base run to its left faces the way the corner does; a run in front of it faces the
 * corner's left, its back running on from the corner's right-hand back. A corner meeting another
 * corner's open side joins it too, so two corners make a U.
 *
 * <p>{@link #LEFT} and {@link #FRONT} say whether such a run is there (actual state). With both,
 * the corner is a blind corner, as a real one is: countertop over an L of carcass behind the two
 * runs' fronts. With one, it is the end of that run, doors facing along it and an end panel
 * closing it. Alone it is a plain two-door base. It holds eighteen slots.</p>
 *
 * @since 2026.9
 */
public class BlockKitchenCorner extends BlockKitchenCabinet {

  /** A base run joins on the corner's left. */
  public static final PropertyBool LEFT = BlockResidentialRun.LEFT;
  /** A base run joins at the corner's front. */
  public static final PropertyBool FRONT = PropertyBool.create("front");

  private static final int[] BOX = {0, 0, 0, 16, 15, 16};

  /**
   * Constructs a corner cabinet.
   *
   * @param registryName its registry name, ending in its finish
   */
  public BlockKitchenCorner(String registryName) {
    super(registryName, BOX, KitchenLine.BASE, 18, KitchenFront.DOORS);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LEFT, FRONT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    EnumFacing g = state.getValue(FACING);
    return state.withProperty(LEFT, joins(world, pos, g.rotateYCCW(), g))
        .withProperty(FRONT, joins(world, pos, g, g.rotateYCCW()));
  }

  /**
   * Takes a run facing its own way on its left, and a run facing its left at its front.
   */
  @Override
  public boolean acceptsRun(IBlockState state, EnumFacing side, EnumFacing runFacing) {
    EnumFacing g = state.getValue(FACING);
    return (side == g.rotateYCCW() && runFacing == g)
        || (side == g && runFacing == g.rotateYCCW());
  }
}
