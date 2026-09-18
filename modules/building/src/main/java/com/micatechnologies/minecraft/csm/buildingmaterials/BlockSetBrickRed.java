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
 * Red brick: the common clay face brick, and the one most people picture when they picture a
 * brick building.
 *
 * <p>A block, stairs, slab and fence set, drawn in running bond at eight courses a block; the top
 * shows the bricks' beds. Its soldier, header and weep-hole courses are {@link BlockBrickTrim}
 * blocks of their own. The textures come from {@code dev-env-utils/scripts/gen_masonry.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSetBrickRed extends AbstractBlockSetBasic {

  /**
   * Constructs a {@link BlockSetBrickRed}.
   *
   * @since 1.0
   */
  public BlockSetBrickRed() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255);
  }

  @Override
  public String getBlockRegistryName() {
    return "brick_red";
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
