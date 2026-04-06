package com.micatechnologies.minecraft.csm.codeutils;

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
 * Factory for creating simple {@link AbstractBlockRotatableNSEWUD} blocks that only differ in
 * registry name, bounding box, constructor parameters, and basic properties. Eliminates the need
 * for a separate class file per block when the only differences are data.
 *
 * @since 2026.4
 */
public class BlockRotatableNSEWUDFactory extends AbstractBlockRotatableNSEWUD {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final boolean opaqueCube;
  private final boolean fullCube;
  private final boolean connectsRedstone;
  private final BlockRenderLayer renderLayer;
  private final boolean passable;
  private final boolean nullCollision;

  /**
   * Full constructor with all configurable properties.
   */
  public BlockRotatableNSEWUDFactory(String registryName, Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance,
      float lightLevel, int lightOpacity, AxisAlignedBB boundingBox,
      boolean opaqueCube, boolean fullCube, boolean connectsRedstone,
      BlockRenderLayer renderLayer, boolean passable, boolean nullCollision) {
    super(initRegistryName(registryName, material), soundType, harvestToolClass, harvestLevel,
        hardness, resistance, lightLevel, lightOpacity);
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.opaqueCube = opaqueCube;
    this.fullCube = fullCube;
    this.connectsRedstone = connectsRedstone;
    this.renderLayer = renderLayer;
    this.passable = passable;
    this.nullCollision = nullCollision;
  }

  /**
   * Convenience constructor for standard non-opaque, non-full-cube blocks with no redstone,
   * CUTOUT_MIPPED render layer, and custom bounding box.
   */
  public BlockRotatableNSEWUDFactory(String registryName, AxisAlignedBB boundingBox) {
    this(registryName, Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0,
        boundingBox, false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false);
  }

  private static Material initRegistryName(String name, Material material) {
    PENDING_REGISTRY_NAME.set(name);
    return material;
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
    return boundingBox;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return opaqueCube;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return fullCube;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return connectsRedstone;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return renderLayer;
  }

  @Override
  public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
    return passable || super.isPassable(worldIn, pos);
  }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn,
      BlockPos pos) {
    return nullCollision ? NULL_AABB : super.getCollisionBoundingBox(blockState, worldIn, pos);
  }
}
