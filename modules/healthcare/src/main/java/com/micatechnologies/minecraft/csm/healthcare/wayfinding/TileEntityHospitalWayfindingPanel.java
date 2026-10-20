package com.micatechnologies.minecraft.csm.healthcare.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingSign.Arrow;
import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingSign.Pictogram;
import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingSign.Scheme;
import java.util.Objects;
import net.minecraft.nbt.NBTTagCompound;

/**
 * A cell of a hospital wayfinding panel ({@link BlockHospitalWayfindingPanel}). Data only: never
 * ticked and never drawn by a renderer; the cell's baked model reads it.
 *
 * <p><b>The sign</b>, Transit's large hanging sign's NBT exactly: {@code l1} and {@code l2} the
 * two lines (printable ASCII, 32 characters; an empty line is left out), {@code p} the pictogram,
 * {@code a} the arrow, {@code s} the colours (all by id) and {@code d} whether the back carries
 * the legend (a byte, on unless {@code 0b}), and the optional text scale: {@code ts} (a float,
 * the whole legend) and {@code ts1} and {@code ts2} (each line, times {@code ts}), each
 * {@value HospitalWayfindingSign#MIN_TEXT_SCALE} to {@value HospitalWayfindingSign#MAX_TEXT_SCALE}
 * and a line's product clamped to the same; a key at 1 is no key and is never written. Every
 * cell of a panel holds a copy: a
 * {@code /blockdata} edit of any cell is written to them all, as Transit's is. A compound without
 * {@code p} leaves the sign alone (Transit's rule).</p>
 *
 * <p><b>The place</b> ({@code pc}, {@code pr}, {@code pw}, {@code ph}: this cell's column and row
 * in its panel and the panel's width and height; 0 x 0 for a cell drawn plain), worked out on the
 * server whenever a cell is added or taken away ({@link HospitalWayfindingSign#refresh}) and
 * synced, so each cell's baked model draws its own share of the legend without looking at the
 * rest of the panel. A generator never needs to write it.</p>
 *
 * @since 2026.10
 */
public class TileEntityHospitalWayfindingPanel extends AbstractTileEntity {

  private static final String KEY_LINE1 = "l1";
  private static final String KEY_LINE2 = "l2";
  private static final String KEY_PICTOGRAM = "p";
  private static final String KEY_ARROW = "a";
  private static final String KEY_SCHEME = "s";
  private static final String KEY_DOUBLE = "d";
  private static final String KEY_TEXT_SCALE = "ts";
  private static final String KEY_LINE1_SCALE = "ts1";
  private static final String KEY_LINE2_SCALE = "ts2";
  private static final String KEY_COL = "pc";
  private static final String KEY_ROW = "pr";
  private static final String KEY_WIDTH = "pw";
  private static final String KEY_HEIGHT = "ph";

  private String line1 = HospitalWayfindingSign.Preset.INFORMATION.getLine1();
  private String line2 = "";
  private Pictogram pictogram = Pictogram.INFO;
  private Arrow arrow = Arrow.RIGHT;
  private Scheme scheme = Scheme.NEUTRAL;
  private boolean doubleSided = true;
  private float textScale = 1f;
  private float line1Scale = 1f;
  private float line2Scale = 1f;
  /** Whether this cell's sign was ever written (else it is the default and may adopt one). */
  private boolean ownSign;

  private int col;
  private int row;
  private int width = 1;
  private int height = 1;

  /** Set while an edit spreads to the panel, so the cells it writes do not spread it again. */
  private static boolean spreading;

  public String getLine1() {
    return line1;
  }

  public String getLine2() {
    return line2;
  }

  public Pictogram getPictogram() {
    return pictogram;
  }

  public Arrow getArrow() {
    return arrow;
  }

  public Scheme getScheme() {
    return scheme;
  }

  public boolean isDoubleSided() {
    return doubleSided;
  }

  /** The legend's scale ({@code ts}), 1 for none. */
  public float getTextScale() {
    return textScale;
  }

  /** The top line's own scale ({@code ts1}), 1 for none. */
  public float getLine1Scale() {
    return line1Scale;
  }

  /** The bottom line's own scale ({@code ts2}), 1 for none. */
  public float getLine2Scale() {
    return line2Scale;
  }

  boolean hasOwnSign() {
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

  /**
   * Sets the whole sign, the lines cleaned by {@link HospitalWayfindingSign#clamp} and the scales
   * by {@link HospitalWayfindingSign#clampTextScale}.
   */
  public void setSign(String line1, String line2, Pictogram pictogram, Arrow arrow,
      Scheme scheme, boolean doubleSided, float textScale, float line1Scale, float line2Scale) {
    this.line1 = HospitalWayfindingSign.clamp(line1);
    this.line2 = HospitalWayfindingSign.clamp(line2);
    this.pictogram = pictogram == null ? Pictogram.NONE : pictogram;
    this.arrow = arrow == null ? Arrow.NONE : arrow;
    this.scheme = scheme == null ? Scheme.NEUTRAL : scheme;
    this.doubleSided = doubleSided;
    this.textScale = HospitalWayfindingSign.clampTextScale(textScale);
    this.line1Scale = HospitalWayfindingSign.clampTextScale(line1Scale);
    this.line2Scale = HospitalWayfindingSign.clampTextScale(line2Scale);
    this.ownSign = true;
  }

  /**
   * Takes another cell's sign.
   *
   * @return whether anything changed
   */
  boolean copySign(TileEntityHospitalWayfindingPanel o) {
    boolean same = ownSign && line1.equals(o.line1) && line2.equals(o.line2)
        && pictogram == o.pictogram && arrow == o.arrow && scheme == o.scheme
        && doubleSided == o.doubleSided && textScale == o.textScale
        && line1Scale == o.line1Scale && line2Scale == o.line2Scale;
    if (!same) {
      setSign(o.line1, o.line2, o.pictogram, o.arrow, o.scheme, o.doubleSided, o.textScale,
          o.line1Scale, o.line2Scale);
    }
    return !same;
  }

  /**
   * Sets this cell's place in its panel.
   *
   * @return whether it changed
   */
  boolean setPlace(int col, int row, int width, int height) {
    if (this.col == col && this.row == row && this.width == width && this.height == height) {
      return false;
    }
    this.col = col;
    this.row = row;
    this.width = width;
    this.height = height;
    return true;
  }

  /** The baked model's view of this cell: what it draws, with the rods filled in later. */
  public HospitalWayfindingFace face(float[] rods) {
    return new HospitalWayfindingFace(line1, line2, pictogram, arrow, scheme, doubleSided,
        HospitalWayfindingSign.lineTextScale(textScale, line1Scale),
        HospitalWayfindingSign.lineTextScale(textScale, line2Scale), col, row, width, height,
        rods);
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(KEY_COL)) {
      col = compound.getByte(KEY_COL);
      row = compound.getByte(KEY_ROW);
      width = compound.getByte(KEY_WIDTH);
      height = compound.getByte(KEY_HEIGHT);
    }
    if (!compound.hasKey(KEY_PICTOGRAM)) {
      return;           // a cell never written keeps its sign (Transit's rule)
    }
    setSign(compound.getString(KEY_LINE1), compound.getString(KEY_LINE2),
        Pictogram.byId(compound.getString(KEY_PICTOGRAM)),
        Arrow.byId(compound.getString(KEY_ARROW)), Scheme.byId(compound.getString(KEY_SCHEME)),
        !compound.hasKey(KEY_DOUBLE) || compound.getBoolean(KEY_DOUBLE),
        readScale(compound, KEY_TEXT_SCALE), readScale(compound, KEY_LINE1_SCALE),
        readScale(compound, KEY_LINE2_SCALE));
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
      HospitalWayfindingSign.applyToPanel(world, pos, line1, line2, pictogram, arrow, scheme,
          doubleSided, textScale, line1Scale, line2Scale);
    } finally {
      spreading = false;
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (!line1.isEmpty()) {
      compound.setString(KEY_LINE1, line1);
    }
    if (!line2.isEmpty()) {
      compound.setString(KEY_LINE2, line2);
    }
    if (ownSign) {
      compound.setString(KEY_PICTOGRAM, pictogram.getId());
    }
    compound.setString(KEY_ARROW, arrow.getId());
    compound.setString(KEY_SCHEME, scheme.getId());
    compound.setBoolean(KEY_DOUBLE, doubleSided);
    writeScale(compound, KEY_TEXT_SCALE, textScale);
    writeScale(compound, KEY_LINE1_SCALE, line1Scale);
    writeScale(compound, KEY_LINE2_SCALE, line2Scale);
    compound.setByte(KEY_COL, (byte) col);
    compound.setByte(KEY_ROW, (byte) row);
    compound.setByte(KEY_WIDTH, (byte) width);
    compound.setByte(KEY_HEIGHT, (byte) height);
    return compound;
  }

  /** A scale key, any number type (a {@code /blockdata} may write a double), 1 when absent. */
  private static float readScale(NBTTagCompound compound, String key) {
    return compound.hasKey(key, 99) ? compound.getFloat(key) : 1f;
  }

  /** A scale key, left out at 1 so an unscaled sign writes only what it always did. */
  private static void writeScale(NBTTagCompound compound, String key, float scale) {
    if (scale != 1f) {
      compound.setFloat(key, scale);
    }
  }

  /**
   * Everything the baked model draws from this cell, so a sync that leaves it alone is free; a
   * text scale is part of it (and of {@link HospitalWayfindingFace}, the quad cache's key), so
   * changing it rebakes only this panel's cells. An unscaled sign's key is what it always was.
   */
  @Override
  protected long getBakedModelKey() {
    long h = Objects.hash(line1, line2, pictogram, arrow, scheme, doubleSided);
    if (textScale != 1f || line1Scale != 1f || line2Scale != 1f) {
      h = h * 31 + Objects.hash(textScale, line1Scale, line2Scale);
    }
    return h * 1000003L ^ (col | row << 5 | width << 10 | height << 15);
  }
}
