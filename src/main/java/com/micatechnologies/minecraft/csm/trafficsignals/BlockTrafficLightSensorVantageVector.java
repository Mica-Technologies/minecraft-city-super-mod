package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Iteris Vantage Vector traffic detection sensor. A smooth rounded white "loaf" housing that sits on
 * top of a mast arm with a slight down-and-forward tilt, an overhanging front hood over a recessed
 * lens, a small mounting stub protruding downward to cosmetically meet the mast arm, and a cable
 * drip-loop dropping into the arm. Because the downward stub handles the mounting visually, this
 * block is in {@code AbstractBlockTrafficPole.IGNORE_BLOCK} so poles do not auto-sprout a connector.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockTrafficLightSensorVantageVector extends AbstractBlockTrafficSignalSensor {

  public BlockTrafficLightSensorVantageVector() {
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
    return "trafficlightsensorvantagevector";
  }

  /**
   * Retrieves the bounding box of the block.
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.20, -0.50, 0.20, 0.80, 0.52, 0.72);
  }
}
