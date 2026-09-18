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
 * Walnut hardwood: strip flooring on its top, the board edges on its sides.
 *
 * <p>A block, stairs, slab and fence set, for building a floor, a stair or a deck of it outright;
 * {@code floor_hardwood_walnut} is the same boards as a one-pixel overlay on an existing floor. The
 * textures come from {@code dev-env-utils/scripts/gen_flooring.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSetHardwoodWalnut extends AbstractBlockSetBasic {

  /**
   * Constructs a {@link BlockSetHardwoodWalnut}.
   *
   * @since 1.0
   */
  public BlockSetHardwoodWalnut() {
    super(Material.WOOD, SoundType.WOOD, "axe", 0, 2F, 10F, 0F, 255);
  }

  @Override
  public String getBlockRegistryName() {
    return "hardwood_walnut";
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
