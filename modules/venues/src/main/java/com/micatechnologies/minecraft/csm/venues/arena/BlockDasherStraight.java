package com.micatechnologies.minecraft.csm.venues.arena;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import javax.annotation.Nonnull;
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
 * A straight length of dasher board ({@code dasher_board}: white, a yellow kick plate, a grey cap
 * rail, 4 px thick on the ice edge of its cell) or of the glass stacked on one
 * ({@code dasher_glass}). Lengths side by side are one run; where a run stops (nothing of its
 * layer beside it, or an open door) the length draws an end post: an aluminium end cap on a
 * board, a stanchion on the glass. Nothing about the ends is stored.
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), the way the board faces: towards the
 * ice. A run beside a 45 degree corner or a curve joins it diagonally, the corner being one cell
 * towards the ice from the cell beside.</p>
 *
 * <p>{@code end_l} and {@code end_r} (actual) are the ends at the left and right of someone
 * standing behind the board looking at the ice: {@code facing} turned anticlockwise and
 * clockwise.</p>
 *
 * @since 2026.10
 */
public class BlockDasherStraight extends BlockDasher {

  /** The run stops on the left. Actual. */
  public static final PropertyBool END_L = PropertyBool.create("end_l");
  /** The run stops on the right. Actual. */
  public static final PropertyBool END_R = PropertyBool.create("end_r");

  private static final AxisAlignedBB SELECT_BOARD = px(0, -0.5, 16, 4.5, 0, 1.03);
  private static final AxisAlignedBB SELECT_GLASS = px(0, 1.25, 16, 3.0, 0, 1.0);

  /**
   * Constructs a straight.
   *
   * @param registryName its registry name
   * @param glass        glass rather than a board
   */
  public BlockDasherStraight(String registryName, boolean glass) {
    super(registryName, glass);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(END_L, true).withProperty(END_R, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, END_L, END_R);
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
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    return state.withProperty(END_L, !joined(world, pos, f, f.rotateYCCW()))
        .withProperty(END_R, !joined(world, pos, f, f.rotateY()));
  }

  /** Whether the run carries on to one side: beside it, or diagonally towards the ice. */
  private boolean joined(IBlockAccess world, BlockPos pos, EnumFacing facing, EnumFacing side) {
    BlockPos beside = pos.offset(side);
    if (continues(world, beside, isGlass())) {
      return true;
    }
    IBlockState d = world.getBlockState(beside.offset(facing));
    return (d.getBlock() instanceof BlockDasherCorner || d.getBlock() instanceof BlockDasherCurve)
        && ((BlockDasher) d.getBlock()).isGlass() == isGlass();
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return isGlass() ? SELECT_GLASS : SELECT_BOARD;
  }

  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
      IBlockAccess source, BlockPos pos) {
    AxisAlignedBB b = isGlass() ? px(0, 1.25, 16, 3.0, 0, 1.0) : px(0, 0, 16, 4.25, 0,
        BOARD_HEIGHT);
    return RotationUtils.rotateBoundingBoxByFacing(b, state.getValue(FACING));
  }
}
