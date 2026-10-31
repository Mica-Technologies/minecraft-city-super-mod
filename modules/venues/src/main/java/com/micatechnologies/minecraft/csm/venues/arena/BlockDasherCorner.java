package com.micatechnologies.minecraft.csm.venues.arena;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A 45 degree corner of dasher board ({@code dasher_board_corner}) or of its glass
 * ({@code dasher_glass_corner}): a board across its cell from one corner to the opposite one,
 * mitred into the straights diagonally beside it, for a chamfered rink corner or a run of
 * corners along a 45 degree line. OBJ, drawn as at a rink's north-west corner.
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3). The board faces between that facing and
 * the facing a quarter turn clockwise from it: east faces south-east (a rink's north-west
 * corner), south faces south-west (north-east corner), west faces north-west (south-east corner)
 * and north faces north-east (south-west corner). The straights it joins stand in the cells
 * beside its own, one cell away from the ice: a north-west corner at cell (x, z) joins the west
 * run at (x - 1, z + 1) and the north run at (x + 1, z - 1).</p>
 *
 * @since 2026.10
 */
public class BlockDasherCorner extends BlockDasher {

  private static final AxisAlignedBB CELL = new AxisAlignedBB(0, 0, 0, 1, 1, 1);

  /**
   * Constructs a corner.
   *
   * @param registryName its registry name
   * @param glass        glass rather than a board
   */
  public BlockDasherCorner(String registryName, boolean glass) {
    super(registryName, glass);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.EAST));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state;
  }

  /** The board faces away from the placer, between two cardinal facings. */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    float yaw = MathHelper.wrapDegrees(placer.rotationYaw + 180.0F);
    int quarter = MathHelper.floor((yaw < 0 ? yaw + 360.0F : yaw) / 90.0F) & 3;
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(quarter));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return CELL;
  }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    double[] b = isGlass() ? DasherShapes.CORNER_GLASS : DasherShapes.CORNER_BOARD;
    EnumFacing turn = state.getValue(FACING).rotateYCCW();
    addBoxes(pos, entityBox, boxes, b, turn, collisionHeight());
  }

  /** Adds px boxes (x0, z0, x1, z1 quads, drawn frame) turned as a model turned to {@code turn}. */
  static void addBoxes(BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
      double[] b, EnumFacing turn, double height) {
    for (int o = 0; o + 3 < b.length; o += 4) {
      addCollisionBoxToList(pos, entityBox, boxes, RotationUtils.rotateBoundingBoxByFacing(
          px(b[o], b[o + 1], b[o + 2], b[o + 3], 0, height), turn));
    }
  }
}
