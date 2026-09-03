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

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final String soundResource;
  private final AxisAlignedBB boundingBox;

  public BlockFireAlarmSounderFactory(String registryName, String soundResource,
      AxisAlignedBB boundingBox) {
    this(initRegistryName(registryName), registryName, soundResource, boundingBox);
  }

  private BlockFireAlarmSounderFactory(Void ignored, String registryName, String soundResource,
      AxisAlignedBB boundingBox) {
    this.registryName = registryName;
    this.soundResource = soundResource;
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
  public String getSoundResourceName(IBlockState blockState) {
    return soundResource;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }
}
