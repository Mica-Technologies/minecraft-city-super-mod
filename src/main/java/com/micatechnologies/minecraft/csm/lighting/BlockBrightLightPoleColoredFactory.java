package com.micatechnologies.minecraft.csm.lighting;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Factory for creating {@link AbstractBrightLightPoleColored} blocks that only differ in registry
 * name, bounding box, and X offset. Eliminates the need for a separate class file per post light.
 *
 * @since 2026.4
 */
public class BlockBrightLightPoleColoredFactory extends AbstractBrightLightPoleColored {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final int brightLightXOffset;

  public BlockBrightLightPoleColoredFactory(String registryName, AxisAlignedBB boundingBox,
      int brightLightXOffset) {
    this(initRegistryName(registryName), registryName, boundingBox, brightLightXOffset);
  }

  private BlockBrightLightPoleColoredFactory(Void ignored, String registryName,
      AxisAlignedBB boundingBox, int brightLightXOffset) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.brightLightXOffset = brightLightXOffset;
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

  @Override
  public int getBrightLightXOffset() {
    return brightLightXOffset;
  }
}
