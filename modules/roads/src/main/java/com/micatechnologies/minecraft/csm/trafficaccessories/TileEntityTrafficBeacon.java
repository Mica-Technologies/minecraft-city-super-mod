package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Tile entity for traffic beacon blocks: the TESR attachment for the strobe drawn by
 * {@link TileEntityTrafficBeaconRenderer}, and, for the preemption beacon, whether a signal
 * controller it is linked to has it lit. That beacon lights for its controller or for redstone,
 * whichever says so.
 */
public class TileEntityTrafficBeacon extends AbstractTileEntity {

  private static final String K_CONTROLLER_LIT = "cl";

  private final long strobeOffset = ThreadLocalRandom.current().nextLong(1000L);

  /** Whether the controller this beacon is linked to has it lit. */
  private boolean controllerLit = false;

  public boolean isControllerLit() {
    return controllerLit;
  }

  /**
   * Sets whether the linked controller has this beacon lit, and shows the change.
   *
   * @param lit whether the controller has it lit
   */
  public void setControllerLit(boolean lit) {
    if (lit == controllerLit) {
      return;
    }
    controllerLit = lit;
    markDirty();
    if (getWorld() != null && !getWorld().isRemote) {
      BlockPreemptBeacon.showPower(getWorld(), getPos());
    }
  }

  public long getStrobeOffset() {
    return strobeOffset;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    controllerLit = compound.getBoolean(K_CONTROLLER_LIT);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setBoolean(K_CONTROLLER_LIT, controllerLit);
    return compound;
  }

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0,
        pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
  }

  /** A beacon is a warning meant to be seen from a distance; it draws as far as a signal. */
  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(LONG_RANGE_RENDER_DISTANCE_SQUARED);
  }
}
