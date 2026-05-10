package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.math.BlockPos;

/**
 * Standard traffic light sensor block. Detects vehicles or entities in a configurable zone and
 * sends demand requests to a linked traffic signal controller.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockTrafficLightSensor extends AbstractBlockTrafficSignalSensor {

  public BlockTrafficLightSensor() {
    super(Material.ROCK);
  }

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "trafficlightsensor";
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
        // Width derived from the rotated model extents (X 0.375-0.625). The previous box
        // was computed without applying element rotations, leaving a ~2.7-block-wide
        // selection that grabbed clicks on neighbouring blocks.
        return new AxisAlignedBB(0.375, -1.000, 0.738, 0.625, 1.859, 1.188);
    }
}
