package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A pipe a fitting in the cell above or below can join: a sprinkler head hung under a branch, or
 * stood on one, or a sprinkler drop. Lets a module's fitting join another module's pipe without
 * naming it (HVAC's exposed pipe, Life Safety's standpipe).
 *
 * <p>Both answers are taken at the middle of the cell, in sixteenths from the bottom of the
 * pipe's own block; -1 when there is no pipe there (a duct, a pipe set back to a wall).</p>
 *
 * @since 2026.10
 */
public interface ICsmPipe {

  /**
   * Where the pipe's underside is over the middle of the cell: 0 for a pipe that runs down out of
   * its block, 8 minus its radius for one running level through the middle.
   */
  int pipeBottom(IBlockAccess world, BlockPos pos, IBlockState state);

  /**
   * Where the pipe's top is under the middle of the cell: 16 for a pipe that runs up out of its
   * block, 8 plus its radius for one running level.
   */
  int pipeTop(IBlockAccess world, BlockPos pos, IBlockState state);
}
