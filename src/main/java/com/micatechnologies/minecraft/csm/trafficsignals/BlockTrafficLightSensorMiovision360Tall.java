package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensorHZEight;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Miovision SmartView 360 detection camera -- tall (mast-arm) variant. Same cream bell-dome
 * omnidirectional camera and hook, but with a clamp/banding saddle at the bottom that sits on a
 * horizontal mast arm and a long straight shaft rising upward off the arm; the hook then curls over
 * the top and the dome hangs down from it (matching real mast-arm installs). Functions as a
 * traffic-signal sensor (see {@link AbstractBlockTrafficSignalSensorHZEight}) with finer 8-way
 * rotation.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockTrafficLightSensorMiovision360Tall
    extends AbstractBlockTrafficSignalSensorHZEight {

  public BlockTrafficLightSensorMiovision360Tall() {
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
    return "trafficlightsensormiovision360tall";
  }

  /**
   * Retrieves the bounding box of the block. Spans the top clamp down through the long shaft to the
   * hanging dome (model extents offset from the OBJ's centered space into 0..1 block space).
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.26, -0.45, 0.00, 0.74, 4.65, 1.08);
  }
}
