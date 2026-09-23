package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Detectors that differ only in registry name and box: the photoelectric smoke detector, the duct
 * detector, the beam detector's heads. Each scans for fire as every detector does
 * ({@link AbstractBlockFireAlarmDetector}) and only reports; putting a fire out is the
 * sprinklers' job, as it is for the heat detector.
 *
 * @since 2026.9
 */
public class BlockFireAlarmDetectorFactory extends AbstractBlockFireAlarmDetector {

  /**
   * The registry name, for the {@code AbstractBlock} constructor, which asks for it before this
   * class's fields are set.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;

  public BlockFireAlarmDetectorFactory(String registryName, AxisAlignedBB boundingBox) {
    this(initRegistryName(registryName), registryName, boundingBox);
  }

  private BlockFireAlarmDetectorFactory(Void ignored, String registryName,
      AxisAlignedBB boundingBox) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    PENDING_REGISTRY_NAME.remove();
  }

  private static Void initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return null;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  public void onFire(World world, BlockPos blockPos, IBlockState blockState, BlockPos firePos) {
    // Reports only.
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }
}
