package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Factory for creating {@link AbstractBlockFireAlarmSounderVoiceEvac} blocks (no strobe) that only
 * differ in registry name and bounding box. Voice evac speakers return null for sound resource.
 *
 * @since 2026.4
 */
public class BlockFireAlarmVoiceEvacFactory extends AbstractBlockFireAlarmSounderVoiceEvac {

  private final String registryName;
  private final AxisAlignedBB boundingBox;

  public BlockFireAlarmVoiceEvacFactory(String registryName, AxisAlignedBB boundingBox) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }
}
