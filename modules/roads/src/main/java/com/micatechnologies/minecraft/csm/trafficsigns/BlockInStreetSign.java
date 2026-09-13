package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.ICsmRoadSurfaceAware;
import com.micatechnologies.minecraft.csm.codeutils.RoadSurfaceHeight;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * An in-street pedestrian sign: the 12 x 36 paddle on a flexible base that stands in the roadway
 * at a crosswalk (the R1-6 family). It is a traffic sign in every other respect -- same
 * registration, facing and texture pipeline -- but it stands on the road rather than on a post,
 * so like the work-zone devices it settles onto the surface below it instead of floating a cell
 * above a road that climbs.
 *
 * <p>The settle offset is applied in the three places that must agree (see
 * {@link RoadSurfaceHeight}): the render offset, the selection box and the collision box. The
 * paddle has no tile entity renderer, so there is no fourth.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockInStreetSign extends BlockTrafficSign implements ICsmRoadSurfaceAware {

  /**
   * The paddle's own box: the base's footprint and the face's height, in the model's frame
   * (facing north, the face across the front of the cell).
   *
   * @since 1.0
   */
  private static final AxisAlignedBB PADDLE_BOX =
      new AxisAlignedBB(3 / 16.0, 0.0, -1.5 / 16.0, 13 / 16.0, 26 / 16.0, 2 / 16.0);

  /**
   * Constructs an {@link BlockInStreetSign} instance.
   *
   * @param registryName the block's registry name
   *
   * @since 1.0
   */
  public BlockInStreetSign(String registryName) {
    super(registryName);
  }

  /**
   * The paddle's box is the same whatever the shift: it has no post to set back or pair.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return the paddle's box, unrotated
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return PADDLE_BOX;
  }

  @Override
  @Nonnull
  public Block.EnumOffsetType getOffsetType() {
    return Block.EnumOffsetType.XYZ;
  }

  @Override
  @Nonnull
  public Vec3d getOffset(IBlockState state, IBlockAccess world, BlockPos pos) {
    return getRoadSurfaceOffsetVector(world, pos);
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB box = super.getBoundingBox(state, source, pos);
    double offset = getRoadSurfaceOffset(source, pos);
    return offset == 0.0 ? box : box.offset(0.0, offset, 0.0);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess worldIn,
      BlockPos pos) {
    AxisAlignedBB box = super.getCollisionBoundingBox(state, worldIn, pos);
    if (box == null) {
      return null;
    }
    double offset = getRoadSurfaceOffset(worldIn, pos);
    return offset == 0.0 ? box : box.offset(0.0, offset, 0.0);
  }

  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    RoadSurfaceHeight.markSurfaceRenderUpdate(world, pos, fromPos);
  }
}
