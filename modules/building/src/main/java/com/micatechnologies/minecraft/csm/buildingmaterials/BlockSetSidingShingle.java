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
 * Cedar shingle siding: split cedar shingles laid in courses and left natural, the coastal and
 * Craftsman look.
 *
 * <p>A block, stairs, slab and fence set. Its texture has a direction -- courses level, or boards
 * plumb -- which holds on every face because the set's models only rotate about the vertical. The
 * textures come from {@code dev-env-utils/scripts/gen_siding.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSetSidingShingle extends AbstractBlockSetBasic {

  /**
   * Constructs a {@link BlockSetSidingShingle}.
   *
   * @since 1.0
   */
  public BlockSetSidingShingle() {
    super(Material.WOOD, SoundType.WOOD, "axe", 0, 2F, 5F, 0F, 255);
  }

  @Override
  public String getBlockRegistryName() {
    return "siding_shingle";
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
