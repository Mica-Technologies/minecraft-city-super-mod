package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

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
}
