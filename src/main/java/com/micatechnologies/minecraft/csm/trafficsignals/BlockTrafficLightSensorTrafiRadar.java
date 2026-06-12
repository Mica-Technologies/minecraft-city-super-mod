package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * FLIR TrafiRadar radar+camera traffic detection sensor. A rounded black rectangular housing (taller
 * than wide) with a flat radar panel up front and a small camera/lens with a brim below it, on the
 * same small discrete side-mount (flat bracket + arm + knuckle) as the FLIR TrafiCam.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockTrafficLightSensorTrafiRadar extends AbstractBlockTrafficSignalSensor {

  public BlockTrafficLightSensorTrafiRadar() {
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
    return "trafficlightsensortrafiradar";
  }

  /**
   * Retrieves the bounding box of the block.
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.36, -0.50, 0.26, 0.64, 0.58, 0.68);
  }
}
