package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Minimal tile entity for emergency light blocks. Stores no data and does not tick.
 * Its sole purpose is to enable TESR attachment for the visual glow effect rendered
 * by {@link TileEntityEmergencyLightRenderer}.
 */
public class TileEntityEmergencyLight extends AbstractTileEntity {

  @Override
  public void readNBT(NBTTagCompound compound) {
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    return compound;
  }

  /**
   * Returns a render bounding box large enough to cover the forward light cone projection
   * (~1.6 blocks reach) so vanilla frustum culling can skip the TESR when the fixture is
   * offscreen. Without this override the TE inherits {@code INFINITE_EXTENT_AABB} and the
   * cone geometry is rebuilt every frame even when no pixel of it would be visible.
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 2.0, pos.getY() - 2.0, pos.getZ() - 2.0,
        pos.getX() + 3.0, pos.getY() + 3.0, pos.getZ() + 3.0);
  }
}
