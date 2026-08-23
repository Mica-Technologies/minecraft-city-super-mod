package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Factory for creating fire alarm beacon blocks: strobe-only notification appliances with a
 * coloured lens and no horn or speaker.
 *
 * <p>Beacons join an alarm system exactly the way a horn strobe's strobe half does -- the
 * control panel activates them through {@link ActiveStrobeRegistry} and they flash on the
 * standard NFPA 72 cadence -- but they never produce sound, so
 * {@link #getSoundResourceName(IBlockState)} always returns {@code null}. The lens colour is
 * supplied per block and drives both the model texture and the rendered flash.</p>
 *
 * @since 2026.8.17
 */
public class BlockFireAlarmBeaconFactory extends AbstractBlockFireAlarmSounder
    implements ICsmTileEntityProvider, IStrobeBlock {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final float[] strobeLensFrom;
  private final float[] strobeLensTo;
  private final float[] strobeColor;

  public BlockFireAlarmBeaconFactory(String registryName, AxisAlignedBB boundingBox,
      float[] strobeLensFrom, float[] strobeLensTo, float[] strobeColor) {
    this(initRegistryName(registryName), registryName, boundingBox, strobeLensFrom, strobeLensTo,
        strobeColor);
  }

  private BlockFireAlarmBeaconFactory(Void ignored, String registryName,
      AxisAlignedBB boundingBox, float[] strobeLensFrom, float[] strobeLensTo,
      float[] strobeColor) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.strobeLensFrom = strobeLensFrom;
    this.strobeLensTo = strobeLensTo;
    this.strobeColor = strobeColor;
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
    return null;
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
  public float[] getStrobeColor() {
    return strobeColor;
  }

  @Override
  public StrobeLensShape getStrobeLensShape() {
    return StrobeLensShape.ROUND;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityFireAlarmStrobe.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityfirealarmstrobe";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityFireAlarmStrobe();
  }
}
