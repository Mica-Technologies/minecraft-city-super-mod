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
 * Fiber cement lap siding: pressed cement and cellulose boards with an embossed cedar grain,
 * painted, the everyday siding of new houses. Harvested with a pickaxe, being cement.
 *
 * <p>A block, stairs, slab and fence set. Its texture has a direction -- courses level, or boards
 * plumb -- which holds on every face because the set's models only rotate about the vertical. The
 * textures come from {@code dev-env-utils/scripts/gen_siding.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSetSidingLap extends AbstractBlockSetBasic {

  /**
   * Constructs a {@link BlockSetSidingLap}.
   *
   * @since 1.0
   */
  public BlockSetSidingLap() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255);
  }

  @Override
  public String getBlockRegistryName() {
    return "siding_lap";
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
