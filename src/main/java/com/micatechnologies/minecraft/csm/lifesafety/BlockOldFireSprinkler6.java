package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Legacy fire sprinkler block (variant 6) that detects fire and lava in the area below,
 * triggering the linked fire alarm control panel when a hazard is found.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public class BlockOldFireSprinkler6 extends AbstractBlockFireSprinkler {

  @Override
  public String getBlockRegistryName() {
    return "oldfiresprinkler6";
  }


    /**
     * Retrieves the bounding box of the block.
     *
     * @param state  the block state
     * @param source the block access
     * @param pos    the block position
     *
     * @return The bounding box of the block.
     *
     * @since 1.0
     */
    @Override
    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
        return new AxisAlignedBB(0.312500, 0.640625, 0.312500, 0.687500, 1.000000, 0.687500);
    }
}
