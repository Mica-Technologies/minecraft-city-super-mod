package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The department a hanging department sign names. It is kept here because the metadata holds
 * only the facing; the sign's {@code getActualState} reads it, and a change syncs to the players
 * in range, whose clients redraw the sign.
 *
 * @since 2026.9
 */
public class TileEntityDepartmentSign extends AbstractTileEntity {

  private static final String KEY_DEPARTMENT = "d";

  private StoreDepartment department = StoreDepartment.PRODUCE;

  /**
   * The department it names.
   *
   * @return the department
   */
  public StoreDepartment getDepartment() {
    return department;
  }

  /**
   * Sets the department and tells the players in range (server side).
   *
   * @param department the department
   */
  public void setDepartment(StoreDepartment department) {
    this.department = department;
    if (world != null && !world.isRemote) {
      markDirty();
      syncServerToClient(world);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    department = StoreDepartment.byOrdinal(
        compound.hasKey(KEY_DEPARTMENT) ? compound.getByte(KEY_DEPARTMENT) : 0);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setByte(KEY_DEPARTMENT, (byte) department.ordinal());
    return compound;
  }

  /** Only the department reaches the baked model, so only a new one rebuilds the section. */
  @Override
  protected long getBakedModelKey() {
    return department.ordinal();
  }
}
