package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Stores which cell of a mast arm curve a block is.
 *
 * <p>A curve is a staircase of up to ten blocks, and each one draws a different slice of the
 * sweep, so every block has to know its index. That will not fit in block metadata: four bits
 * have to cover the horizontal facing already, leaving four states where up to ten are needed.
 * This is the same reason {@code TileEntityFireAlarmSoundIndex} exists.
 *
 * <p>The index is also what makes the curve breakable as a unit. Knowing the index and the
 * facing is enough to locate the root -- see {@link BlockTrafficPoleMastArmCurve} -- so no
 * cell has to store a position that could go stale if the structure were ever moved.
 */
public class TileEntityMastArmCurve extends AbstractTileEntity {

  private static final String CELL_INDEX_KEY = "cIx";

  private int cellIndex = 0;

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(CELL_INDEX_KEY)) {
      cellIndex = compound.getInteger(CELL_INDEX_KEY);
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(CELL_INDEX_KEY, cellIndex);
    return compound;
  }

  /**
   * Gets this block's index within its curve, {@code 0} being the cell at the pole.
   *
   * @return the cell index
   */
  public int getCellIndex() {
    return cellIndex;
  }

  /**
   * Sets this block's index within its curve.
   *
   * @param cellIndex the cell index, {@code 0} being the cell at the pole
   */
  public void setCellIndex(int cellIndex) {
    this.cellIndex = cellIndex;
    markDirty();
  }
}
