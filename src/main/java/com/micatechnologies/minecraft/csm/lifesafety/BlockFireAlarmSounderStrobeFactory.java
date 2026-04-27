package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Factory for creating {@link AbstractBlockFireAlarmSounder} blocks with strobe (no SOUND property)
 * that only differ in registry name, sound resource, bounding box, and strobe lens coordinates.
 *
 * @since 2026.4
 */
public class BlockFireAlarmSounderStrobeFactory extends AbstractBlockFireAlarmSounder
    implements ICsmTileEntityProvider, IStrobeBlock {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final String soundResource;
  private final AxisAlignedBB boundingBox;
  private final float[] strobeLensFrom;
  private final float[] strobeLensTo;
  private final boolean redSlowToggle;

  public BlockFireAlarmSounderStrobeFactory(String registryName, String soundResource,
      AxisAlignedBB boundingBox, float[] strobeLensFrom, float[] strobeLensTo) {
    this(registryName, soundResource, boundingBox, strobeLensFrom, strobeLensTo, false);
  }

  public BlockFireAlarmSounderStrobeFactory(String registryName, String soundResource,
      AxisAlignedBB boundingBox, float[] strobeLensFrom, float[] strobeLensTo,
      boolean redSlowToggle) {
    this(initRegistryName(registryName), registryName, soundResource, boundingBox,
        strobeLensFrom, strobeLensTo, redSlowToggle);
  }

  private BlockFireAlarmSounderStrobeFactory(Void ignored, String registryName,
      String soundResource, AxisAlignedBB boundingBox, float[] strobeLensFrom,
      float[] strobeLensTo, boolean redSlowToggle) {
    this.registryName = registryName;
    this.soundResource = soundResource;
    this.boundingBox = boundingBox;
    this.strobeLensFrom = strobeLensFrom;
    this.strobeLensTo = strobeLensTo;
    this.redSlowToggle = redSlowToggle;
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

  @Override
  public float[] getStrobeLensFrom() {
    return strobeLensFrom;
  }

  @Override
  public float[] getStrobeLensTo() {
    return strobeLensTo;
  }

  @Override
  public boolean isRedSlowToggleStrobe() {
    return redSlowToggle;
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
