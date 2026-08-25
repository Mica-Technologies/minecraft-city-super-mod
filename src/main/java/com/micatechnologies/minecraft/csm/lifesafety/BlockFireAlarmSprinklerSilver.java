package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Silver fire sprinkler head block that detects fire and lava in the area below,
 * triggering the linked fire alarm control panel when a hazard is found.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public class BlockFireAlarmSprinklerSilver extends AbstractBlockFireSprinkler {

  @Override
  public String getBlockRegistryName() {
    return "firealarmsprinklersilver";
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
        return new AxisAlignedBB(0.500000, 0.437500, 0.890625, 0.593750, 0.562500, 1.000000);
    }
}
