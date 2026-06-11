package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensorHZEight;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Miovision SmartView 360 detection camera -- short (vertical-shaft) variant. A cream bell-dome
 * omnidirectional camera hanging from a curved "shepherd's-hook" gooseneck arm with a back mount
 * bracket for side-mounting on a vertical pole shaft. Functions as a traffic-signal sensor (see
 * {@link AbstractBlockTrafficSignalSensorHZEight}) with finer 8-way rotation.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockTrafficLightSensorMiovision360 extends AbstractBlockTrafficSignalSensorHZEight {

  public BlockTrafficLightSensorMiovision360() {
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
    return "trafficlightsensormiovision360";
  }

  /**
   * Retrieves the bounding box of the block. Covers the dome, gooseneck arm and back bracket
   * (model extents offset from the OBJ's centered space into 0..1 block space).
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.26, -0.25, -0.06, 0.74, 1.40, 1.08);
  }
}
