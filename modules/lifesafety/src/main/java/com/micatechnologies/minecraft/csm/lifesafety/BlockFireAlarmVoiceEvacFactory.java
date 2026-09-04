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

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;

  public BlockFireAlarmVoiceEvacFactory(String registryName, AxisAlignedBB boundingBox) {
    this(initRegistryName(registryName), registryName, boundingBox);
  }

  private BlockFireAlarmVoiceEvacFactory(Void ignored, String registryName,
      AxisAlignedBB boundingBox) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
  }

  private static Void initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return null;
  }

  @Override
  public String getBlockRegistryName() {
    if (registryName != null) {
      return registryName;
    }
    return PENDING_REGISTRY_NAME.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }
}
