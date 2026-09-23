package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.ICsmRoadSurfaceAware;
import com.micatechnologies.minecraft.csm.codeutils.RoadSurfaceHeight;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The fire hydrant, which settles onto sloped and partial-height road and sidewalk surfaces like
 * the work zone devices do.
 *
 * <p>It stays on {@link AbstractBlockRotatableNSEWUD} rather than moving to
 * {@code AbstractBlockRoadSurfaceRotatableNSEW}, because hydrants already placed in worlds store
 * their facing as a six-way index: a south-facing one is meta 3, which the horizontal encoding
 * reads as east. Keeping the six-way property keeps every placed hydrant facing the way it did.
 * Only placement is limited to the four horizontal facings, since a hydrant never lies on its side
 * and a six-way block placed while flying would otherwise face up.</p>
 *
 * @version 2.0
 */
public class BlockFireHydrant extends AbstractBlockRotatableNSEWUD
    implements ICsmRoadSurfaceAware {

  public BlockFireHydrant() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "firehydrant";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.181250, 0.000000, 0.237500, 0.818750, 0.862500, 0.856250);
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  /**
   * Faces the hydrant toward the player horizontally, whatever the player's pitch.
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return this.getDefaultState()
        .withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  /**
   * Declares that the hydrant renders at an offset from its own cell, so that
   * {@link #getOffset} is consulted.
   */
  @Override
  @Nonnull
  public Block.EnumOffsetType getOffsetType() {
    return Block.EnumOffsetType.XYZ;
  }

  /**
   * Moves the model down onto the surface below it.
   */
  @Override
  @Nonnull
  public Vec3d getOffset(IBlockState state, IBlockAccess world, BlockPos pos) {
    return getRoadSurfaceOffsetVector(world, pos);
  }

  /**
   * Moves the selection and collision box down with the model.
   */
  @Override
  @Nonnull
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB box = super.getBoundingBox(state, source, pos);
    double offset = getRoadSurfaceOffset(source, pos);
    return offset == 0.0 ? box : box.offset(0.0, offset, 0.0);
  }

  /**
   * Redraws the hydrant when the surface below it changes.
   */
  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    RoadSurfaceHeight.markSurfaceRenderUpdate(world, pos, fromPos);
  }
}
