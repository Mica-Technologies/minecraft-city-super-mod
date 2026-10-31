package com.micatechnologies.minecraft.csm.venues.pool;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A pace clock's tile entity: no data, there only so its hands can be drawn. The time is the
 * world's.
 *
 * @since 2026.10
 */
public class TileEntityPaceClock extends AbstractTileEntity {

  @Override
  public void readNBT(NBTTagCompound c) {
    // nothing stored
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    return c;
  }

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos);
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(48.0 * 48.0);
  }
}
