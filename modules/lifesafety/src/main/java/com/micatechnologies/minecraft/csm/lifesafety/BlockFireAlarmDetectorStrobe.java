package com.micatechnologies.minecraft.csm.lifesafety;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A smoke detector with a strobe built in, such as the Gentex 710CS-C. It is linked and reports
 * as any detector does ({@link AbstractBlockFireAlarmDetector}); its strobe flashes whenever the
 * panel it reports to is in alarm, because the panel counts linked initiating devices that are
 * strobes among the strobes it drives.
 *
 * @since 2026.9
 */
public class BlockFireAlarmDetectorStrobe extends AbstractBlockFireAlarmDetector
    implements IStrobeBlock {

  /**
   * The registry name, for the {@code AbstractBlock} constructor, which asks for it before this
   * class's fields are set.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final float[] strobeLensFrom;
  private final float[] strobeLensTo;

  public BlockFireAlarmDetectorStrobe(String registryName, AxisAlignedBB boundingBox,
      float[] strobeLensFrom, float[] strobeLensTo) {
    this(initRegistryName(registryName), registryName, boundingBox, strobeLensFrom,
        strobeLensTo);
  }

  private BlockFireAlarmDetectorStrobe(Void ignored, String registryName,
      AxisAlignedBB boundingBox, float[] strobeLensFrom, float[] strobeLensTo) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.strobeLensFrom = strobeLensFrom;
    this.strobeLensTo = strobeLensTo;
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

  @Override
  public float[] getStrobeLensFrom() {
    return strobeLensFrom;
  }

  @Override
  public float[] getStrobeLensTo() {
    return strobeLensTo;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityFireAlarmSensorStrobe.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityfirealarmsensorstrobe";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityFireAlarmSensorStrobe();
  }
}
