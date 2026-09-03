package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Minimal tile entity for traffic beacon blocks. Stores no data — exists solely to enable
 * TESR attachment for the visual strobe effect rendered by {@link TileEntityTrafficBeaconRenderer}.
 */
public class TileEntityTrafficBeacon extends AbstractTileEntity {

  private final long strobeOffset = ThreadLocalRandom.current().nextLong(1000L);

  public long getStrobeOffset() {
    return strobeOffset;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    return compound;
  }

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0,
        pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
  }
}
