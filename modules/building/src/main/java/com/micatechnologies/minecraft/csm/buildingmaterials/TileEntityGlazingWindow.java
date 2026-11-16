package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * A glazing window's stored settings ({@link BlockGlazingWindow}). Data only: never ticked, and
 * drawn by the window's baked model, not a renderer.
 *
 * <p>NBT, every key optional (a missing key is its default, so a window set with metadata alone
 * is a plain, closed, square-topped window with no trim):</p>
 * <ul>
 *   <li>{@code pt} (byte) pattern: 0 plain, 1 double-hung one over one, 2 double-hung six over
 *       six, 3 casement pair ({@link WindowLayout#PLAIN} ...). Read from the stack's foot.</li>
 *   <li>{@code op} (byte, 0/1) open: the lower sash slid up, the casements swung out. Read from
 *       the stack's foot.</li>
 *   <li>{@code sl} (byte) sill: 0 none, 1 stone, 2 frame. Read from the stack's foot.</li>
 *   <li>{@code ar} (byte, 0/1) arched top. Read from the stack's top.</li>
 *   <li>{@code hd} (byte) head: 0 none, 1 flat lintel, 2 keystone lintel. Read from the stack's
 *       top.</li>
 * </ul>
 * <p>The facing and depth are metadata, not NBT. A click writes the pattern and the open state
 * to every block of the stack, so the stack keeps them whichever block is broken.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public class TileEntityGlazingWindow extends AbstractTileEntity {

  static final String KEY_PATTERN = "pt";
  static final String KEY_OPEN = "op";
  static final String KEY_ARCH = "ar";
  static final String KEY_SILL = "sl";
  static final String KEY_HEAD = "hd";

  private int pattern = WindowLayout.PLAIN;
  private boolean open;
  private boolean arch;
  private int sill = WindowLayout.SILL_NONE;
  private int head = WindowLayout.HEAD_NONE;

  public int getPattern() {
    return pattern;
  }

  public boolean isOpen() {
    return open;
  }

  public boolean isArch() {
    return arch;
  }

  public int getSill() {
    return sill;
  }

  public int getHead() {
    return head;
  }

  void setPattern(int pattern) {
    this.pattern = Math.floorMod(pattern, WindowLayout.PATTERNS);
  }

  void setOpen(boolean open) {
    this.open = open;
  }

  void setArch(boolean arch) {
    this.arch = arch;
  }

  void setSill(int sill) {
    this.sill = Math.floorMod(sill, WindowLayout.SILLS);
  }

  void setHead(int head) {
    this.head = Math.floorMod(head, WindowLayout.HEADS);
  }

  private static int read(NBTTagCompound c, String key, int bound) {
    if (!c.hasKey(key)) {
      return 0;
    }
    int v = c.getByte(key);
    return v >= 0 && v < bound ? v : 0;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    pattern = read(compound, KEY_PATTERN, WindowLayout.PATTERNS);
    open = read(compound, KEY_OPEN, 2) == 1;
    arch = read(compound, KEY_ARCH, 2) == 1;
    sill = read(compound, KEY_SILL, WindowLayout.SILLS);
    head = read(compound, KEY_HEAD, WindowLayout.HEADS);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setByte(KEY_PATTERN, (byte) pattern);
    compound.setByte(KEY_OPEN, (byte) (open ? 1 : 0));
    compound.setByte(KEY_ARCH, (byte) (arch ? 1 : 0));
    compound.setByte(KEY_SILL, (byte) sill);
    compound.setByte(KEY_HEAD, (byte) head);
    return compound;
  }

  /**
   * Every field the model reads, packed: a sync that changes none of them rebuilds nothing.
   *
   * @return the settings, packed
   */
  @Override
  protected long getBakedModelKey() {
    return settingsKey();
  }

  /** The settings packed into an int, for the model key and its test. */
  long settingsKey() {
    return pattern | (open ? 1 : 0) << 2 | (arch ? 1 : 0) << 3 | sill << 4 | head << 6;
  }
}
