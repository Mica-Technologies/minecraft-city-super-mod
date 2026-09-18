package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockSetBasic;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Polished concrete: a ground and sealed slab, the finished floor of a shop or a loft.
 *
 * <p>A block, stairs, slab and fence set with the same texture on every face, for building a floor
 * of it outright; {@code floor_polished_concrete} is the same finish as a one-pixel overlay on an
 * existing floor. The texture comes from {@code dev-env-utils/scripts/gen_flooring.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSetPolishedConcrete extends AbstractBlockSetBasic {

  /**
   * Constructs a {@link BlockSetPolishedConcrete}.
   *
   * @since 1.0
   */
  public BlockSetPolishedConcrete() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255);
  }

  @Override
  public String getBlockRegistryName() {
    return "polished_concrete";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
