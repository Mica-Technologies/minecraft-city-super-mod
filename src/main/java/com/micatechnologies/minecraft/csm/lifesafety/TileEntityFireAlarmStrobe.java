package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

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
}
