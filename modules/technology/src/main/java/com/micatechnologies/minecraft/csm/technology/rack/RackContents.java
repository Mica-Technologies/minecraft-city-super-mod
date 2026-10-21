package com.micatechnologies.minecraft.csm.technology.rack;

import java.util.Arrays;

/**
 * What is mounted in one rack: each unit and the U its top is at, in the order they were
 * mounted. Immutable, so a value can key the baked model's quad cache: a room of racks filled
 * alike shares one set of quads.
 *
 * <p>Saved as one byte array of pairs, {@code [unit ordinal, top U, ...]}, read unsigned
 * ({@link #toBytes()}, {@link #fromBytes(byte[])}). Units are numbered from U1 at the bottom, as
 * a real rack's are.</p>
 *
 * @since 2026.10
 */
public final class RackContents {

  /** The most U any rack has: what a saved top U is checked against when read. */
  public static final int MAX_U = 42;

  /** A rack with nothing in it. */
  public static final RackContents EMPTY = new RackContents(new RackUnitType[0], new int[0]);

  private final RackUnitType[] types;
  private final int[] tops;
  private final int hash;

  private RackContents(RackUnitType[] types, int[] tops) {
    this.types = types;
    this.tops = tops;
    this.hash = 31 * Arrays.hashCode(types) + Arrays.hashCode(tops);
  }

  /** How many units are mounted. */
  public int size() {
    return types.length;
  }

  /** Whether nothing is mounted. */
  public boolean isEmpty() {
    return types.length == 0;
  }

  /** The {@code i}th unit mounted. */
  public RackUnitType type(int i) {
    return types[i];
  }

  /** The U the {@code i}th unit's top is at. */
  public int top(int i) {
    return tops[i];
  }

  /** How many U the mounted units take. */
  public int usedU() {
    int used = 0;
    for (RackUnitType type : types) {
      used += type.getHeightU();
    }
    return used;
  }

  /**
   * The unit covering a U.
   *
   * @param u the U, from 1 at the bottom
   * @return its index, or -1 if that U is empty
   */
  public int indexAt(int u) {
    for (int i = 0; i < types.length; i++) {
      if (u <= tops[i] && u > tops[i] - types[i].getHeightU()) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Whether every U from {@code bottom} to {@code top} is empty.
   */
  private boolean free(int bottom, int top) {
    for (int u = bottom; u <= top; u++) {
      if (indexAt(u) >= 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * The highest place a unit of this height fits: the next free span from the top.
   *
   * @param heightU the unit's height
   * @param totalU  the rack's height
   * @return the U the unit's top would be at, or -1 if it does not fit anywhere
   */
  public int findSpanFromTop(int heightU, int totalU) {
    for (int top = totalU; top >= heightU; top--) {
      if (free(top - heightU + 1, top)) {
        return top;
      }
    }
    return -1;
  }

  /**
   * These contents with one more unit, mounted last.
   *
   * @param type the unit
   * @param top  the U its top is at; the span must be free
   * @return the new contents
   */
  public RackContents with(RackUnitType type, int top) {
    if (top - type.getHeightU() + 1 < 1 || top > MAX_U
        || !free(top - type.getHeightU() + 1, top)) {
      throw new IllegalArgumentException("no room for " + type + " at U" + top);
    }
    RackUnitType[] t = Arrays.copyOf(types, types.length + 1);
    int[] u = Arrays.copyOf(tops, tops.length + 1);
    t[types.length] = type;
    u[tops.length] = top;
    return new RackContents(t, u);
  }

  /**
   * These contents without the {@code i}th unit.
   *
   * @param i the index
   * @return the new contents
   */
  public RackContents without(int i) {
    if (types.length == 1) {
      return EMPTY;
    }
    RackUnitType[] t = new RackUnitType[types.length - 1];
    int[] u = new int[tops.length - 1];
    for (int a = 0, b = 0; a < types.length; a++) {
      if (a != i) {
        t[b] = types[a];
        u[b++] = tops[a];
      }
    }
    return new RackContents(t, u);
  }

  /** The saved form: {@code [ordinal, top, ...]}. */
  public byte[] toBytes() {
    byte[] out = new byte[types.length * 2];
    for (int i = 0; i < types.length; i++) {
      out[2 * i] = (byte) types[i].ordinal();
      out[2 * i + 1] = (byte) tops[i];
    }
    return out;
  }

  /**
   * Reads the saved form. A pair naming a unit this version does not know, a top outside
   * 1..{@link #MAX_U}, or a span overlapping a unit read before it is dropped; the rest is kept.
   *
   * @param bytes the saved bytes, possibly null or of odd length
   * @return the contents
   */
  public static RackContents fromBytes(byte[] bytes) {
    RackContents out = EMPTY;
    if (bytes == null) {
      return out;
    }
    for (int i = 0; i + 1 < bytes.length; i += 2) {
      RackUnitType type = RackUnitType.fromOrdinal(bytes[i] & 0xFF);
      int top = bytes[i + 1] & 0xFF;
      if (type == null || top > MAX_U || top - type.getHeightU() + 1 < 1
          || !out.free(top - type.getHeightU() + 1, top)) {
        continue;
      }
      out = out.with(type, top);
    }
    return out;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RackContents)) {
      return false;
    }
    RackContents c = (RackContents) o;
    return hash == c.hash && Arrays.equals(types, c.types) && Arrays.equals(tops, c.tops);
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("RackContents[");
    for (int i = 0; i < types.length; i++) {
      s.append(i == 0 ? "" : ", ").append(types[i]).append("@U").append(tops[i]);
    }
    return s.append(']').toString();
  }
}
