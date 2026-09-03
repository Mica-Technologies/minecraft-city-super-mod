package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.math.BlockPos;

/**
 * Below-ground traffic light sensor block. An in-pavement sensor variant that detects vehicles
 * or entities in a configurable zone from a flush-mounted position.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockTrafficLightSensorBelowGround extends AbstractBlockTrafficSignalSensor {

  public BlockTrafficLightSensorBelowGround() {
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
    return "trafficlightsensorbelowground";
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
        return new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.000000, 1.000000);
    }
}

