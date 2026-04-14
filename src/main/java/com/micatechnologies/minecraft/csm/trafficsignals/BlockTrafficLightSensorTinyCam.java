package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Tiny camera traffic light sensor block. A miniature camera-style visual variant of the standard
 * traffic sensor that detects vehicles or entities in a configurable zone.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockTrafficLightSensorTinyCam extends AbstractBlockTrafficSignalSensor {

  public BlockTrafficLightSensorTinyCam() {
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
    return "trafficlightsensortinycam";
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
        return new AxisAlignedBB(0.354956, 0.000000, 0.328125, 0.643017, 0.851563, 0.500000);
    }
}
