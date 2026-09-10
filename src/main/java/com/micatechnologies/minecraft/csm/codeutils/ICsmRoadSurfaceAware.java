package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;

/**
 * Marks a block that settles onto the surface below it rather than sitting at the bottom of its
 * own cell, and supplies the offset it settles by.
 *
 * <p>Implemented by the work zone devices, which are placed in the cell above a road and pulled
 * down onto it by however much the road did not fill. See {@link RoadSurfaceHeight} for how the
 * height of that surface is worked out and for the three places the offset has to be applied.</p>
 *
 * <p>The marker is load-bearing as well as descriptive: {@link RoadSurfaceHeight} refuses to
 * settle a block onto another block that carries it, so devices stack on one another as placed
 * instead of compounding offsets.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public interface ICsmRoadSurfaceAware {

  /**
   * Gets the vertical offset this block settles by at the given position.
   *
   * @param world the block access to read from
   * @param pos   the position of this block
   *
   * @return the offset, at most zero
   *
   * @since 1.0
   */
  default double getRoadSurfaceOffset(IBlockAccess world, BlockPos pos) {
    return RoadSurfaceHeight.offsetFor(world, pos);
  }

  /**
   * Gets the offset this block settles by as a vector, for use as a block render offset.
   *
   * @param world the block access to read from
   * @param pos   the position of this block
   *
   * @return the offset vector, with only a vertical component
   *
   * @since 1.0
   */
  default Vec3d getRoadSurfaceOffsetVector(IBlockAccess world, BlockPos pos) {
    double offset = getRoadSurfaceOffset(world, pos);
    return offset == 0.0 ? Vec3d.ZERO : new Vec3d(0.0, offset, 0.0);
  }
}
