package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
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

public class BlockTrafficAccessoryNSEWUD extends AbstractBlockRotatableNSEWUD {

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final BlockRenderLayer renderLayer;
  private final boolean fullCube;

  public BlockTrafficAccessoryNSEWUD(String registryName, AxisAlignedBB boundingBox,
      BlockRenderLayer renderLayer, float hardness, boolean fullCube) {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, hardness, 10F, 0F, 0);
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.renderLayer = renderLayer;
    this.fullCube = fullCube;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return fullCube;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return renderLayer;
  }
}
