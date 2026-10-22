package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A block-high length of aircraft cable, stacked under a ceiling so a {@link BlockLinearLight} run
 * hangs one or more blocks lower: the segment under it hangs and runs its cable up into it. A thin
 * cable down the middle of the block, no collision, nothing stored.
 *
 * @since 2026.10
 */
public class BlockLinearLightHanger extends AbstractBlock {

  private static final AxisAlignedBB BOX = new AxisAlignedBB(7 / 16.0, 0, 7 / 16.0, 9 / 16.0, 1,
      9 / 16.0);

  public BlockLinearLightHanger() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 0, 1F, 5F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "linear_light_hanger";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Nullable
  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
    return true;
  }

  @Nonnull
  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
