package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Minimal tile entity for fire alarm strobe blocks. Stores no data and does not tick.
 * Its sole purpose is to enable TESR attachment for the visual strobe flash effect
 * rendered by {@link TileEntityFireAlarmStrobeRenderer}.
 */
public class TileEntityFireAlarmStrobe extends AbstractTileEntity {

  @Override
  public void readNBT(NBTTagCompound compound) {
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    return compound;
  }

  /**
   * Returns a render bounding box sized for the flash quad plus a 1-block margin on every
   * side so the lens-face quad is never clipped at the edge of the frustum. Overriding this
   * switches vanilla from {@code INFINITE_EXTENT_AABB} (which disables frustum culling) to
   * real frustum culling — strobes outside the view skip the TESR render entirely.
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0,
        pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
  }
}
