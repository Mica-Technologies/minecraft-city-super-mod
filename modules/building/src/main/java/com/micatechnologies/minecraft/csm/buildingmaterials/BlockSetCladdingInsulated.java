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
 * Insulated panel cladding: white insulated metal panels laid horizontally, the skin of
 * distribution centres and cold stores.
 *
 * <p>A block, stairs, slab and fence set. The profile is drawn into the texture rather than
 * modelled, since a wall is seen face on, where shading alone reads as the profile; its pitch
 * divides the block, so a run has no split rib at a seam. The textures come from
 * {@code dev-env-utils/scripts/gen_cladding.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSetCladdingInsulated extends AbstractBlockSetBasic {

  /**
   * Constructs a {@link BlockSetCladdingInsulated}.
   *
   * @since 1.0
   */
  public BlockSetCladdingInsulated() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 3F, 10F, 0F, 255);
  }

  @Override
  public String getBlockRegistryName() {
    return "cladding_insulated";
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
