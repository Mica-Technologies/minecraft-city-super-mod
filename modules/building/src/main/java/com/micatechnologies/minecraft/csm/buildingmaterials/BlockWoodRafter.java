package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
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
 * A wood rafter, running corner to corner of its block at 45 degrees.
 *
 * <p>Stack these diagonally, the way stairs are stacked, and the slope carries on unbroken as far
 * as it is built — a true 12:12 roof. That is what this exists for: a pitched roof TRUSS cannot be
 * a single block, because element rotation allows one axis and only 22.5 or 45 degrees, and a real
 * truss spans six to twelve blocks with its chord rising the whole way. A rafter at 45 degrees
 * fits its own block exactly and needs no multi-block machinery at all.</p>
 *
 * <p>Four facings rather than the two axes the other spanning members carry: a roof has a slope
 * each side of its ridge, and a quarter turn cannot mirror a slope.</p>
 *
 * @version 1.0
 * @see BlockFramingSpan
 * @since 2026.9
 */
public class BlockWoodRafter extends AbstractBlock {

  /**
   * Which way the rafter climbs.
   *
   * @since 1.0
   */
  public static final PropertyDirection FACING = BlockHorizontal.FACING;

  /**
   * Constructs a {@link BlockWoodRafter}.
   *
   * @since 1.0
   */
  public BlockWoodRafter() {
    super(Material.WOOD, SoundType.WOOD, "axe", 0, 1.0F, 5F, 0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
  }

  @Override
  public String getBlockRegistryName() {
    return "wood_rafter";
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
   * A full cube.
   *
   * <p>The rafter's real extent is a diagonal, which an axis-aligned box cannot describe. Every
   * other member in this family gets its own true extent, and this one cannot, so it takes the
   * honest approximation — which also keeps a roof something that can be stood on rather than
   * fallen through.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
