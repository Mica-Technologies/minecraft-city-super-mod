package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.math.BlockPos;

/**
 * Bell-style traffic light sensor block. A visual variant of the standard traffic sensor with a
 * bell-shaped housing that detects vehicles or entities in a configurable zone.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockTrafficLightSensorBell extends AbstractBlockTrafficSignalSensor {

  public BlockTrafficLightSensorBell() {
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
    return "trafficlightsensorbell";
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
        return new AxisAlignedBB(0.250000, -1.000000, -0.546875, 0.750000, 1.937500, 1.218750);
    }
}
