package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * A cell of a wayfinding panel ({@link AbstractBlockWayfindingPanel}). Data only: never ticked
 * and never drawn by a renderer; the cell's baked model reads it.
 *
 * <p><b>The sign</b> ({@link WayfindingSign}, whose notes list the NBT). Every cell of a panel
 * holds a copy: an edit writes them all (from the editor, a click or a {@code /blockdata} on any
 * cell), and a cell placed against a panel takes its neighbour's. A cell never edited writes no
 * sign and shows its block's {@link #defaultSign() default}.</p>
 *
 * <p><b>The place</b> ({@code pc}, {@code pr}, {@code pw}, {@code ph}: this cell's column and row
 * in its panel and the panel's width and height; 0 x 0 for a cell drawn plain), worked out on the
 * server whenever a cell is added or taken away ({@link WayfindingPanels#refresh}) and synced, so
 * each cell's baked model draws its own share without looking at the rest of the panel. A
 * generator never needs to write it. A cell saved without one (the airport panel kept none) has
 * it worked out at the end of the tick it is loaded in.</p>
 *
 * @since 2026.10
 */
public abstract class AbstractTileEntityWayfindingPanel extends AbstractTileEntity {

  private static final String KEY_COL = "pc";
  private static final String KEY_ROW = "pr";
  private static final String KEY_WIDTH = "pw";
  private static final String KEY_HEIGHT = "ph";

  private WayfindingSign sign;
  /** Whether this cell's sign was ever written (else it is the default and may adopt one). */
  private boolean ownSign;

  private int col;
  private int row;
  private int width = 1;
  private int height = 1;
  private boolean placeKnown;

  /** Set while an edit spreads to the panel, so the cells it writes do not spread it again. */
  private static boolean spreading;

  /** The sign a cell of this block shows before anyone writes one. */
  public abstract WayfindingSign defaultSign();

  public WayfindingSign getSign() {
    if (sign == null) {
      sign = defaultSign();
    }
    return sign;
  }

  /** Whether this cell's sign was written (not the block's default). */
  public boolean hasOwnSign() {
    return ownSign;
  }

  public int getCol() {
    return col;
  }

  public int getRow() {
    return row;
  }

  public int getPanelWidth() {
    return width;
  }

  public int getPanelHeight() {
    return height;
  }

  /** Whether the cell's place was ever worked out (a cell saved before it was kept has none). */
  public boolean isPlaceKnown() {
    return placeKnown;
  }

  /**
   * Sets the whole sign.
   *
   * @return whether anything changed
   */
  public boolean setSign(WayfindingSign sign) {
    boolean changed = !ownSign || !getSign().equals(sign);
    boolean relight = getSign().isLit() != sign.isLit();
    this.sign = sign;
    this.ownSign = true;
    // only for a live cell: one read as its chunk loads is lit with the chunk
    if (relight && world != null && world.isBlockLoaded(pos)
        && world.getTileEntity(pos) == this) {
      world.checkLight(pos);
    }
    return changed;
  }

  /**
   * Sets this cell's place in its panel.
   *
   * @return whether it changed
   */
  boolean setPlace(int col, int row, int width, int height) {
    boolean was = placeKnown;
    placeKnown = true;
    if (was && this.col == col && this.row == row && this.width == width
        && this.height == height) {
      return false;
    }
    this.col = col;
    this.row = row;
    this.width = width;
    this.height = height;
    return true;
  }

  @Override
  public void onLoad() {
    super.onLoad();
    if (!placeKnown && world != null && !world.isRemote) {
      WayfindingPanels.queueRefresh(world, pos);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(KEY_COL)) {
      col = compound.getByte(KEY_COL);
      row = compound.getByte(KEY_ROW);
      width = compound.getByte(KEY_WIDTH);
      height = compound.getByte(KEY_HEIGHT);
      placeKnown = true;
    }
    if (!WayfindingSign.isSign(compound)) {
      return;           // a cell never written keeps its block's default sign
    }
    setSign(WayfindingSign.read(compound, defaultSign().getScheme()));
    spreadEdit();
  }

  /**
   * A {@code /blockdata} (or {@code /setblock} with NBT) edit of any cell is the whole panel's.
   * Only for a live edit on the server: a cell read as its chunk loads is not yet in the world.
   */
  private void spreadEdit() {
    if (spreading || world == null || world.isRemote || !world.isBlockLoaded(pos)
        || world.getTileEntity(pos) != this) {
      return;
    }
    spreading = true;
    try {
      WayfindingPanels.applyToPanel(world, pos, getSign());
    } finally {
      spreading = false;
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (ownSign) {
      getSign().write(compound);
    }
    if (placeKnown) {
      compound.setByte(KEY_COL, (byte) col);
      compound.setByte(KEY_ROW, (byte) row);
      compound.setByte(KEY_WIDTH, (byte) width);
      compound.setByte(KEY_HEIGHT, (byte) height);
    }
    return compound;
  }

  /**
   * Everything the baked model draws from this cell, so a sync that leaves it alone is free.
   */
  @Override
  protected long getBakedModelKey() {
    return (long) getSign().hashCode() * 1000003L
        ^ (col | row << 5 | width << 10 | height << 15);
  }
}
