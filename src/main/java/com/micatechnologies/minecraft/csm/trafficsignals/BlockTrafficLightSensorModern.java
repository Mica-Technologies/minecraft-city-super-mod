package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensorAngled;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.math.BlockPos;

/**
 * Modern-style traffic light sensor block (the GridSmart AC3 box camera). A visual variant of the
 * standard traffic sensor with a contemporary housing design that detects vehicles or entities in a
 * configurable zone. Keeps a cardinal facing (so the controller's directional logic applies) plus a
 * configurable aim angle that swings the camera about its pole (see
 * {@link AbstractBlockTrafficSignalSensorAngled}).
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockTrafficLightSensorModern extends AbstractBlockTrafficSignalSensorAngled {

  public BlockTrafficLightSensorModern() {
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
    return "trafficlightsensormodern";
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
        return new AxisAlignedBB(0.34, 0.06, 0.34, 0.66, 2.15, 1.05);
    }
}
