package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
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
 * A construction-site block that lies along one horizontal axis: a wall form, a rebar bundle.
 *
 * <p>Only the axis is stored, since these look the same end for end. The model and the box are
 * drawn running along x ({@link #getRunBox()}) and turned a quarter for z, the model by the
 * blockstate and the box here.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public abstract class AbstractBlockSiteAxial extends AbstractBlock {

  /**
   * The axis the block runs along. Stored.
   *
   * @since 1.0
   */
  public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis",
      EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);

  /**
   * Constructs an {@link AbstractBlockSiteAxial}.
   *
   * @param material  the material
   * @param soundType the sound type
   * @param tool      the harvest tool class
   *
   * @since 1.0
   */
  protected AbstractBlockSiteAxial(Material material, SoundType soundType, String tool) {
    super(material, soundType, tool, 0, 1.5F, 6F, 0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X));
  }

  /**
   * The block's box, running along x.
   *
   * @return the box
   *
   * @since 1.0
   */
  protected abstract AxisAlignedBB getRunBox();

  /**
   * Whether the block runs along the placer's line of sight (a bundle laid down in front of
   * you) or across it (a wall form set out in front of you).
   *
   * @return {@code true} to run along the line of sight
   *
   * @since 1.0
   */
  protected abstract boolean runsAlongLook();

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, AXIS);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(AXIS,
        (meta & 1) != 0 ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS) == EnumFacing.Axis.Z ? 1 : 0;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing.Axis look = placer.getHorizontalFacing().getAxis();
    EnumFacing.Axis run = runsAlongLook() ? look
        : (look == EnumFacing.Axis.X ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
    return getDefaultState().withProperty(AXIS, run);
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB box = getRunBox();
    if (state.getValue(AXIS) == EnumFacing.Axis.X) {
      return box;
    }
    return new AxisAlignedBB(box.minZ, box.minY, box.minX, box.maxZ, box.maxY, box.maxX);
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
