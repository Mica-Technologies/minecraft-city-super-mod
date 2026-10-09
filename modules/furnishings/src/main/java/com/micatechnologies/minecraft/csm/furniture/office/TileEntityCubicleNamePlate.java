package com.micatechnologies.minecraft.csm.furniture.office;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The words on a cubicle panel's name plate or sign ({@link BlockCubiclePanelNamed}): up to
 * three lines, a name, a title and a department. Only the special renderer reads them; the
 * plate itself is the baked model, so a change never rebuilds the chunk.
 *
 * @since 2026.10
 */
public class TileEntityCubicleNamePlate extends AbstractTileEntity {

  /** The most lines any plate carries. */
  public static final int MAX_LINES = 3;
  /**
   * Characters a line may hold. The longest fit at full width on the sign is about sixteen; up
   * to this many are condensed to fit, as a printed insert would be.
   */
  public static final int MAX_LINE_LENGTH = 24;

  private static final String[] KEYS = {"l1", "l2", "l3"};

  private final String[] lines = {"", "", ""};

  /** Client only: whether the sign was {@link BlockCubiclePanelNamed#WIDE} when last read. */
  public transient boolean wide;
  /** Client only: the world time {@link #wide} was read at, or -1. */
  public transient long wideReadAt = -1;

  /**
   * A line of the plate.
   *
   * @param index the line, from 0
   *
   * @return its text, never null
   */
  public String getLine(int index) {
    return index >= 0 && index < MAX_LINES ? lines[index] : "";
  }

  /**
   * Sets a line, cleaned by {@link #clamp}.
   *
   * @param index the line, from 0
   * @param text  the text
   */
  public void setLine(int index, String text) {
    if (index >= 0 && index < MAX_LINES) {
      lines[index] = clamp(text);
    }
  }

  /**
   * What a line may hold: printable ASCII (the font has nothing else), at most
   * {@link #MAX_LINE_LENGTH} characters, trimmed.
   *
   * @param text the text
   *
   * @return the cleaned text
   */
  public static String clamp(String text) {
    if (text == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(Math.min(text.length(), MAX_LINE_LENGTH));
    for (int i = 0; i < text.length() && out.length() < MAX_LINE_LENGTH; i++) {
      char c = text.charAt(i);
      if (c >= 32 && c < 127) {
        out.append(c);
      }
    }
    return out.toString().trim();
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    // A wide sign reaches a quarter block over each neighbour.
    return new AxisAlignedBB(getPos(), getPos().add(1, 1, 1)).grow(1, 0, 1);
  }

  /** The print is small: past this there is nothing to read, and the plate is the model's. */
  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(32.0 * 32.0);
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    for (int i = 0; i < MAX_LINES; i++) {
      lines[i] = clamp(compound.getString(KEYS[i]));
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    for (int i = 0; i < MAX_LINES; i++) {
      if (!lines[i].isEmpty()) {
        compound.setString(KEYS[i], lines[i]);
      }
    }
    return compound;
  }
}
