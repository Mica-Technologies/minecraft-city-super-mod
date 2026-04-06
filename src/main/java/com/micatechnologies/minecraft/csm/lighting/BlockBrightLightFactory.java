package com.micatechnologies.minecraft.csm.lighting;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Factory for creating {@link AbstractBrightLight} blocks that only differ in registry name,
 * bounding box, and X offset. Eliminates the need for a separate class file per light fixture.
 *
 * @since 2026.4
 */
public class BlockBrightLightFactory extends AbstractBrightLight {

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final int brightLightXOffset;

  public BlockBrightLightFactory(String registryName, AxisAlignedBB boundingBox,
      int brightLightXOffset) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.brightLightXOffset = brightLightXOffset;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName;
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
