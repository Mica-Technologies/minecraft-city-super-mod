package com.micatechnologies.minecraft.csm.parks.landscape;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A turned column stacked from several blocks: its base is drawn only on the bottom block and its
 * capital only on the top one, so a three-block gazebo post is one post rather than three stacked
 * ones. Both are actual state, read from the column blocks above and below; the multipart
 * blockstate draws the shaft always and the base and capital where they apply
 * ({@code gen_park_amenities.py}).
 *
 * @since 2026.9
 */
public class BlockParkColumn extends BlockParkProp {

  /** This is the bottom of its column: nothing of the same column below. */
  public static final PropertyBool BASE = PropertyBool.create("base");
  /** This is the top of its column: nothing of the same column above. */
  public static final PropertyBool CAP = PropertyBool.create("cap");

  public BlockParkColumn(String registryName, Kind kind, int height, int inset) {
    super(registryName, kind, height, inset);
    setDefaultState(blockState.getBaseState().withProperty(BASE, true).withProperty(CAP, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, BASE, CAP);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    return state.withProperty(BASE, world.getBlockState(pos.down()).getBlock() != this)
        .withProperty(CAP, world.getBlockState(pos.up()).getBlock() != this);
  }
}
