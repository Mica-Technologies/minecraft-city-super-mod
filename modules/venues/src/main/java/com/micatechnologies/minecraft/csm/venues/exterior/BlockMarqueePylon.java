package com.micatechnologies.minecraft.csm.venues.exterior;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The marquee board's pylon ({@code marquee_pylon}): a clad base two or three blocks tall that a
 * double-sided marquee board stands on, out by the street. Stack it and set cells side by side to
 * make the base as wide as wanted: each cell is one 10 px deep slab of dark bronze cladding,
 * centred in its cell under the double-sided board's cabinet, with a concrete plinth where no
 * pylon is under it and a cap where none is over it ({@code down}, {@code up}, actual state).
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), the way the board above it faces; the
 * slab runs across it.</p>
 *
 * @since 2026.10
 */
public class BlockMarqueePylon extends BlockVenue {

  /** No pylon above: the cap. Actual. */
  public static final PropertyBool UP = PropertyBool.create("up");
  /** No pylon below: the plinth. Actual. */
  public static final PropertyBool DOWN = PropertyBool.create("down");

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0, 0, 3 / 16.0, 1, 1, 13 / 16.0);

  /**
   * Constructs the pylon.
   *
   * @param registryName its registry name
   */
  public BlockMarqueePylon(String registryName) {
    super(registryName, Material.ROCK, SoundType.STONE, 2.5F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(UP, true).withProperty(DOWN, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, UP, DOWN);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(UP, !(world.getBlockState(pos.up()).getBlock() == this))
        .withProperty(DOWN, !(world.getBlockState(pos.down()).getBlock() == this));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
