package com.micatechnologies.minecraft.csm.transit.platform;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
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
 * Station wall tile: a full block faced in glazed subway tile, plain white or with a frieze band
 * in one of the agencies' colours on its sides (the band course is drawn at the same height on
 * every block, so a row of band blocks is one unbroken frieze). One class, constructed by
 * registry name.
 *
 * @since 2026.9
 */
public class BlockStationTile extends AbstractBlock {

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a tile block.
   *
   * @param registryName its registry name
   */
  public BlockStationTile(String registryName) {
    super(stash(registryName), SoundType.STONE, "pickaxe", 0, 1.5F, 6.0F, 0F, 255);
    this.registryName = registryName;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.ROCK;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
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
