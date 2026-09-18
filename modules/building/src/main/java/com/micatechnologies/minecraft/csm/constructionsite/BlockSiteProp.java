package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A construction-site prop with no state of its own: a column form, a rebar mat, rebar dowels,
 * a column cage.
 *
 * <p>They differ in nothing but a registry name, a model, a box and what they are made of, so
 * there is one class, constructed with those, rather than a class each. The name is handed across
 * on the thread because {@link AbstractBlock}'s constructor asks for it before this class's
 * fields are assigned -- the same hand-off {@code BlockBrickTrim} uses.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSiteProp extends AbstractBlock {

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;

  /**
   * Constructs a {@link BlockSiteProp}.
   *
   * @param registryName the registry name
   * @param material     the material
   * @param soundType    the sound type
   * @param tool         the harvest tool class
   * @param box          the block's box, in block units
   *
   * @since 1.0
   */
  public BlockSiteProp(String registryName, Material material, SoundType soundType, String tool,
      AxisAlignedBB box) {
    super(pendingMaterial(registryName, material), soundType, tool, 0, 1.5F, 6F, 0F, 0);
    this.registryName = registryName;
    this.box = box;
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName, Material material) {
    PENDING_REGISTRY_NAME.set(registryName);
    return material;
  }

  /**
   * A box from sixteenths, the units the models are drawn in.
   *
   * @return the box in block units
   *
   * @since 1.0
   */
  public static AxisAlignedBB box16(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
