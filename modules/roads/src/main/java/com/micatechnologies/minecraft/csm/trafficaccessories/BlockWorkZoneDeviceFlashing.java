package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

/**
 * A work zone device carrying a Type A warning light: the drum with a lamp on its lid.
 *
 * <p>The light runs on its own schedule rather than on redstone — a barrel in a taper flashes
 * because it is a barrel in a taper, not because someone wired it. It reuses the traffic beacon
 * strobe renderer, driven at one short pulse a second — inside the 55 to 75 flashes a minute a
 * Type A light is specified at. The short duty cycle is what makes it read as a warning light
 * rather than as a lamp being switched on and off.</p>
 *
 * <p>Real warning lights on a real taper each run on their own timer and drift out of step within
 * minutes. That is reproduced for free: the per-block random offset
 * {@link TileEntityTrafficBeacon} already carries spans a whole cycle, so no two drums flash
 * together. It costs one {@code long} per tile entity and nothing per frame.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWorkZoneDeviceFlashing extends BlockWorkZoneDeviceRotatable
    implements ITrafficBeaconBlock, ICsmTileEntityProvider {

  /**
   * The lens, in 1/16 block units, matching the lamp the generator builds on the drum's lid.
   *
   * @since 1.0
   */
  private static final float[] LENS_FROM = {8.50f, 15.05f, 7.66f};

  /**
   * The far corner of the lens, in 1/16 block units.
   *
   * @since 1.0
   */
  private static final float[] LENS_TO = {10.60f, 17.15f, 8.34f};

  /**
   * Constructs a {@link BlockWorkZoneDeviceFlashing} instance.
   *
   * @param registryName the registry name of the device
   * @param boundingBox  the bounding box of the device, in block space
   *
   * @since 1.0
   */
  public BlockWorkZoneDeviceFlashing(String registryName, AxisAlignedBB boundingBox) {
    super(registryName, boundingBox);
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityTrafficBeacon();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityTrafficBeacon.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityworkzonewarninglight";
  }

  @Override
  public float[] getBeaconLensFrom() {
    return LENS_FROM;
  }

  @Override
  public float[] getBeaconLensTo() {
    return LENS_TO;
  }

  @Override
  public float getBeaconColorR() {
    return 0.965f;
  }

  @Override
  public float getBeaconColorG() {
    return 0.651f;
  }

  @Override
  public float getBeaconColorB() {
    return 0.094f;
  }

  @Override
  public long getBeaconCycleMillis() {
    return 1000L;
  }
}
