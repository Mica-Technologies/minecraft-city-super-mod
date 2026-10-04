package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Which cell of a jet bridge turn a block is ({@link BlockJetBridgeTurn}).
 *
 * <p>A turn is up to 25 blocks, each drawing and colliding a different share of it, so every block
 * has to know its index. Block metadata already holds the facing and the hand, leaving one bit:
 * the same reason the mast arm curves keep theirs in {@code TileEntityMastArmCurve}. The index,
 * with the facing and hand, also locates the turn's entry cell, so no cell stores a position that
 * could go stale.</p>
 *
 * @since 2026.10
 */
public class TileEntityJetBridgeTurn extends AbstractTileEntity {

  private static final String CELL_KEY = "cIx";

  private int cell = 0;

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(CELL_KEY)) {
      cell = compound.getInteger(CELL_KEY);
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(CELL_KEY, cell);
    return compound;
  }

  /** This block's cell in its turn's {@link JetBridgeTurnShape}, 0 being end A. */
  public int getCell() {
    return cell;
  }

  /**
   * Sets this block's cell.
   *
   * @param cell the cell in its turn's {@link JetBridgeTurnShape}
   */
  public void setCell(int cell) {
    this.cell = cell;
    markDirty();
  }
}
