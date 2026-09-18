package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A construction-site prop with a front, which faces the player who places it: the portable
 * toilet, the gang box, the concrete washout.
 *
 * <p>One class constructed by registry name, like {@link BlockSiteProp}; only the facing is
 * stored. The model and the box are drawn with the front to the north and turned by the facing,
 * the model by the blockstate and the box here. The models come from
 * {@code dev-env-utils/scripts/gen_facilities.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSiteFacingProp extends AbstractBlock {

  /**
   * The way the front faces. Stored.
   *
   * @since 1.0
   */
  public static final PropertyDirection FACING = BlockHorizontal.FACING;

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB northBox;

  /**
   * Constructs a {@link BlockSiteFacingProp}.
   *
   * @param registryName the registry name
   * @param material     the material
   * @param soundType    the sound type
   * @param tool         the harvest tool class
   * @param northBox     the box with the front to the north, in block units
   *
   * @since 1.0
   */
  public BlockSiteFacingProp(String registryName, Material material, SoundType soundType,
      String tool, AxisAlignedBB northBox) {
    super(pendingMaterial(registryName, material), soundType, tool, 0, 1.5F, 6F, 0F, 0);
    this.registryName = registryName;
    this.northBox = northBox;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName, Material material) {
    PENDING_REGISTRY_NAME.set(registryName);
    return material;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  /**
   * {@code box}, drawn with the front to the north, turned to face {@code facing}.
   *
   * @param box    the box
   * @param facing the facing
   *
   * @return the turned box
   *
   * @since 1.0
   */
  static AxisAlignedBB turn(AxisAlignedBB box, EnumFacing facing) {
    switch (facing) {
      case SOUTH:
        return new AxisAlignedBB(1 - box.maxX, box.minY, 1 - box.maxZ, 1 - box.minX, box.maxY,
            1 - box.minZ);
      case EAST:
        return new AxisAlignedBB(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY,
            box.maxX);
      case WEST:
        return new AxisAlignedBB(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY,
            1 - box.minX);
      default:
        return box;
    }
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return turn(northBox, state.getValue(FACING));
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
