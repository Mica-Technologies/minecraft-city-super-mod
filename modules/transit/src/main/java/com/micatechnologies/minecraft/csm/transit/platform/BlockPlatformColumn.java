package com.micatechnologies.minecraft.csm.transit.platform;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A platform column, one block of shaft at a time, tiled or steel. Stacked, columns read as one:
 * {@link #UP} and {@link #DOWN} (actual state) say whether another column stands above and
 * below, and the blockstate draws the plinth only at the foot and the capital only at the head.
 * Any column counts, so a number band column set into a tiled one joins it. The top of a column
 * carries whatever is set on it (a canopy, a ceiling), so its top face is solid.
 *
 * @since 2026.9
 */
public class BlockPlatformColumn extends BlockPlatformFixture {

  /** Another column stands above. */
  public static final PropertyBool UP = PropertyBool.create("up");
  /** Another column stands below. */
  public static final PropertyBool DOWN = PropertyBool.create("down");

  /**
   * Constructs a column.
   *
   * @param registryName its registry name
   * @param box          its box, in sixteenths
   */
  public BlockPlatformColumn(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(UP, false).withProperty(DOWN, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, UP, DOWN);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(UP, isColumn(world, pos.up()))
        .withProperty(DOWN, isColumn(world, pos.down()));
  }

  private static boolean isColumn(IBlockAccess world, BlockPos at) {
    return world.getBlockState(at).getBlock() instanceof BlockPlatformColumn;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face.getAxis() == EnumFacing.Axis.Y ? BlockFaceShape.CENTER_BIG
        : BlockFaceShape.UNDEFINED;
  }
}
