package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Factory for creating simple {@link AbstractBlockFireAlarmSounder} blocks (no strobe, no SOUND
 * property) that only differ in registry name, sound resource, and bounding box.
 *
 * @since 2026.4
 */
public class BlockFireAlarmSounderFactory extends AbstractBlockFireAlarmSounder {

  private final String registryName;
  private final String soundResource;
  private final AxisAlignedBB boundingBox;

  public BlockFireAlarmSounderFactory(String registryName, String soundResource,
      AxisAlignedBB boundingBox) {
    this.registryName = registryName;
    this.soundResource = soundResource;
    this.boundingBox = boundingBox;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName;
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return soundResource;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }
}
