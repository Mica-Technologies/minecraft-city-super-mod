package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The number a station number plaque shows, 0 to 99. The plaque's model reads it through the
 * block's actual state, so a sync rebuilds the chunk only when the number changes.
 *
 * @since 2026.9
 */
public class TileEntityStationNumber extends AbstractTileEntity {

  private static final String NUMBER_KEY = "n";

  private int number = 1;

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = Math.floorMod(number, 100);
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    number = compound.hasKey(NUMBER_KEY) ? Math.floorMod(compound.getInteger(NUMBER_KEY), 100)
        : 1;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NUMBER_KEY, number);
    return compound;
  }

  @Override
  protected long getBakedModelKey() {
    return number;
  }
}
