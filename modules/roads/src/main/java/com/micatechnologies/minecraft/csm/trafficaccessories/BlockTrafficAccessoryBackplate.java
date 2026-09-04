package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.ICsmRetiringBlock;
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

public class BlockTrafficAccessoryBackplate extends AbstractBlockSignalBackplate
    implements ICsmRetiringBlock {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  /**
   * Optional registry id this block retires into when randomTick'd. Non-null means the block
   * is deprecated and will auto-replace itself with the named block (see
   * {@link ICsmRetiringBlock}). Null means this is a live block and retirement is skipped.
   */
  @Nullable
  private final String replacementBlockId;

  public BlockTrafficAccessoryBackplate(String registryName) {
    this(registryName, null);
  }

  public BlockTrafficAccessoryBackplate(String registryName,
      @Nullable String replacementBlockId) {
    super(initRegistryName(registryName), SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
    this.registryName = registryName;
    this.replacementBlockId = replacementBlockId;
  }

  @Nullable
  @Override
  public String getReplacementBlockId() {
    return replacementBlockId;
  }

  private static Material initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.ROCK;
  }

  @Override
  public String getBlockRegistryName() {
    if (registryName != null) {
      return registryName;
    }
    return PENDING_REGISTRY_NAME.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0D, 0D, 0.8D, 1D, 1D, 1D);
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
