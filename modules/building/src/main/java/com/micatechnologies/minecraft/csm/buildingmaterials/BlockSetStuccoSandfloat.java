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
 * Sand float stucco: a coat finished with a float that drags the sand to the surface, the
 * fine even grain most stucco buildings actually have.
 *
 * <p>A block, stairs, slab and fence set in one warm off-white, the same texture on every face;
 * the three stucco sets differ in finish, not colour. The texture comes from
 * {@code dev-env-utils/scripts/gen_stucco.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSetStuccoSandfloat extends AbstractBlockSetBasic {

  /**
   * Constructs a {@link BlockSetStuccoSandfloat}.
   *
   * @since 1.0
   */
  public BlockSetStuccoSandfloat() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255);
  }

  @Override
  public String getBlockRegistryName() {
    return "stucco_sandfloat";
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
