package com.micatechnologies.minecraft.csm.venues.exterior;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The blade bracket ({@code venue_blade_bracket}): a steel arm along the bottom of its cell,
 * reaching out from a wall, that a projecting wayfinding blade ({@link BlockVenueBlade}) hangs
 * from on its rods. Put it in the cell over the blade's top; for a blade two blocks out, two
 * brackets in a line make one arm. The cell against the wall draws the wall plate and the knee
 * brace ({@code plate}, actual state: the cell behind is not a bracket of the same arm); the
 * outer end draws an end cap ({@code tip}).
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), the way the arm reaches out from the
 * wall. Placed against a wall by hand it reaches away from it.</p>
 *
 * @since 2026.10
 */
public class BlockBladeBracket extends BlockVenue {

  /** The cell against the wall: plate and knee brace. Actual. */
  public static final PropertyBool PLATE = PropertyBool.create("plate");
  /** The arm's outer end: an end cap. Actual. */
  public static final PropertyBool TIP = PropertyBool.create("tip");

  private static final AxisAlignedBB BOX = new AxisAlignedBB(5 / 16.0, 0, 0, 11 / 16.0,
      4 / 16.0, 1);

  /**
   * Constructs the bracket.
   *
   * @param registryName its registry name
   */
  public BlockBladeBracket(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 1.5F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(PLATE, true).withProperty(TIP, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, PLATE, TIP);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing out = facing.getAxis() == EnumFacing.Axis.Y
        ? placer.getHorizontalFacing().getOpposite() : facing;
    return getDefaultState().withProperty(FACING, out);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    return state.withProperty(PLATE, !same(world, pos.offset(f.getOpposite()), f))
        .withProperty(TIP, !same(world, pos.offset(f), f));
  }

  private boolean same(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() == this && s.getValue(FACING) == facing;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }
}
