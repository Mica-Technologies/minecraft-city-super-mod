package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
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
 * A horizontal structural member: a joist, a girder or a deck. Something that SPANS.
 *
 * <h3>Why this is not {@link BlockFramingWall}</h3>
 *
 * <p>A wall is a junction. It may carry on in four directions, turn a corner, branch or cross, so
 * it reads six neighbours and draws itself from what it finds. A joist does none of that: it runs
 * one way and repeats. All it needs to know is which way it points, which is one property with two
 * values rather than the wall's 768 states.</p>
 *
 * <p>Nothing here is join-aware, deliberately. A row of joists tiles seamlessly because each block
 * is the full length of its own cell, and where a run stops it simply stops — which is what a
 * joist does where it lands on a wall. A bearing seat drawn only at the ends of a run would be the
 * equivalent of the wall's post; it was left out because unlike the wall's post, nothing looks
 * wrong without it.</p>
 *
 * <h3>Collision</h3>
 *
 * <p>Each member's bounding box is its own real extent, not a full cube, so a floor of joists can
 * be walked between and stood on before it is decked. The box is given as the member is drawn —
 * spanning north-south — and turned here for the other axis, so a subclass never has to think
 * about rotation.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public abstract class BlockFramingSpan extends AbstractBlock {

  /**
   * The axis the member spans along.
   *
   * <p>Only the two horizontal axes. A vertical joist is not a thing, and leaving {@code Y} out
   * keeps the state count at two.</p>
   *
   * @since 1.0
   */
  public static final PropertyEnum<EnumFacing.Axis> AXIS =
      PropertyEnum.create("axis", EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);

  /**
   * Constructs a {@link BlockFramingSpan}.
   *
   * @param material         the material of the block
   * @param soundType        the sound type of the block
   * @param harvestToolClass the harvest tool class of the block
   * @param harvestLevel     the harvest level of the block
   * @param hardness         the block's hardness
   * @param resistance       the block's resistance to explosions
   *
   * @since 1.0
   */
  protected BlockFramingSpan(Material material, SoundType soundType, String harvestToolClass,
      int harvestLevel, float hardness, float resistance) {
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance, 0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Z));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, AXIS);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState()
        .withProperty(AXIS, (meta & 1) == 1 ? EnumFacing.Axis.X : EnumFacing.Axis.Z);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS) == EnumFacing.Axis.X ? 1 : 0;
  }

  /**
   * Spans across the way the player is looking, which is how a joist is placed in practice: you
   * stand in the room it crosses.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing.Axis looking = placer.getHorizontalFacing().getAxis();
    return getDefaultState().withProperty(AXIS,
        looking == EnumFacing.Axis.X ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
  }

  /**
   * The member's extent as it is DRAWN, spanning north-south. Turned for the other axis by
   * {@link #getBlockBoundingBox}, so this is written once and never thinks about rotation.
   *
   * @return the bounding box for a member spanning north-south
   *
   * @since 1.0
   */
  protected abstract AxisAlignedBB getSpanBoundingBox();

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB box = getSpanBoundingBox();
    if (state.getValue(AXIS) != EnumFacing.Axis.X) {
      return box;
    }
    // A quarter turn about Y swaps X and Z. Written out rather than run through the rotation
    // helper because the member is symmetrical about both, so the swap is the whole rotation.
    return new AxisAlignedBB(box.minZ, box.minY, box.minX, box.maxZ, box.maxY, box.maxX);
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
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess world, BlockPos pos,
      @Nullable EnumFacing side) {
    return false;
  }

  /**
   * Cutout, so an open-web joist is seen through between its chords.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
