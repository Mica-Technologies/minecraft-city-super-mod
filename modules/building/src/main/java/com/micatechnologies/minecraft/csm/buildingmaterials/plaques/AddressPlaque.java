package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;

/**
 * What an address plaque says and how it is set: one immutable value, held by the plaque's tile
 * entity and baked into its model. Every key is optional.
 *
 * <table>
 *   <caption>NBT</caption>
 *   <tr><th>Key</th><th>Type</th><th>Meaning</th></tr>
 *   <tr><td>{@code num}</td><td>string</td><td>the house number, up to 8 characters (default
 *   {@code 123})</td></tr>
 *   <tr><td>{@code st}</td><td>string</td><td>the street line, up to 32 characters; empty for
 *   none. Not drawn in a vertical stack</td></tr>
 *   <tr><td>{@code font}</td><td>string</td><td>{@code classic}, {@code didone}, {@code modern},
 *   {@code narrow}; absent for the style's</td></tr>
 *   <tr><td>{@code size}</td><td>float</td><td>text size, 0.5 to 1 of the largest that fits
 *   (default 1)</td></tr>
 *   <tr><td>{@code plate}</td><td>string</td><td>{@code s}, {@code m} (default), {@code l}</td></tr>
 *   <tr><td>{@code lay}</td><td>string</td><td>{@code h} (default) or {@code v}, a vertical
 *   stack for a narrow pier</td></tr>
 *   <tr><td>{@code ink}</td><td>string</td><td>the numerals' colour: {@code default} (the
 *   style's), {@code black}, {@code white}, {@code gold}, {@code brass}, {@code bronze},
 *   {@code blue}, {@code steel}, {@code red}, {@code green}, {@code slate}</td></tr>
 *   <tr><td>{@code dx}, {@code dy}</td><td>byte</td><td>moves the plaque across and up its
 *   block, px, -8 to 8 (it never leaves the block)</td></tr>
 *   <tr><td>{@code dz}</td><td>byte</td><td>the transom decal only: how far behind the block's
 *   back face the decal lies, px, -24 to 16 (7 reaches a pane in the middle of the block behind;
 *   -20 or so reaches glass in front of a decal placed inside)</td></tr>
 *   <tr><td>{@code lit}</td><td>byte</td><td>the lamp box only: lit (default 1)</td></tr>
 * </table>
 *
 * <p>Characters outside the plaque font ({@link PlaqueSheet#CHARS}: digits, letters, space and
 * {@code . , - ' / & #}) are taken out, and every number is clamped, so a {@code /blockdata}
 * and the editor are read the same way. Enums are saved by id; an unknown id reads as the
 * default. Keys holding their default are not written, except {@code num}.</p>
 *
 * @since 2026.11
 */
public final class AddressPlaque {

  public static final int MAX_NUMBER = 8;
  public static final int MAX_STREET = 32;
  public static final float MIN_SIZE = 0.5f;
  public static final int MAX_OFFSET = 8;
  public static final int MIN_DEPTH = -24;
  public static final int MAX_DEPTH = 16;
  public static final String DEFAULT_NUMBER = "123";

  static final String KEY_NUMBER = "num";
  static final String KEY_STREET = "st";
  static final String KEY_FONT = "font";
  static final String KEY_SIZE = "size";
  static final String KEY_PLATE = "plate";
  static final String KEY_LAYOUT = "lay";
  static final String KEY_INK = "ink";
  static final String KEY_DX = "dx";
  static final String KEY_DY = "dy";
  static final String KEY_DZ = "dz";
  static final String KEY_LIT = "lit";

  public static final AddressPlaque DEFAULT = new Builder().build();

  private final String number;
  private final String street;
  @Nullable
  private final PlaqueFont font;
  private final float size;
  private final PlaqueSize plate;
  private final boolean vertical;
  private final PlaqueInk ink;
  private final int dx;
  private final int dy;
  private final int dz;
  private final boolean lit;

  private AddressPlaque(Builder b) {
    this.number = clean(b.number, MAX_NUMBER);
    this.street = clean(b.street, MAX_STREET);
    this.font = b.font;
    this.size = Float.isNaN(b.size) ? 1f : Math.max(MIN_SIZE, Math.min(1f, b.size));
    this.plate = b.plate == null ? PlaqueSize.MEDIUM : b.plate;
    this.vertical = b.vertical;
    this.ink = b.ink == null ? PlaqueInk.DEFAULT : b.ink;
    this.dx = clamp(b.dx, -MAX_OFFSET, MAX_OFFSET);
    this.dy = clamp(b.dy, -MAX_OFFSET, MAX_OFFSET);
    this.dz = clamp(b.dz, MIN_DEPTH, MAX_DEPTH);
    this.lit = b.lit;
  }

  private static int clamp(int v, int lo, int hi) {
    return Math.max(lo, Math.min(hi, v));
  }

  /** Only the characters the plaque font has, trimmed, at most {@code max} long. */
  static String clean(@Nullable String text, int max) {
    if (text == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(Math.min(text.length(), max + 8));
    for (int i = 0; i < text.length() && out.length() < max * 4; i++) {
      char c = text.charAt(i);
      if (PlaqueSheet.CHARS.indexOf(c) >= 0) {
        out.append(c);
      }
    }
    String s = out.toString().trim();
    return s.length() > max ? s.substring(0, max).trim() : s;
  }

  public String getNumber() {
    return number;
  }

  public String getStreet() {
    return street;
  }

  /** The font chosen, or null for the style's. */
  @Nullable
  public PlaqueFont getFont() {
    return font;
  }

  public PlaqueFont fontFor(PlaqueStyle style) {
    return font != null ? font : style.getDefaultFont();
  }

  public float getSize() {
    return size;
  }

  public PlaqueSize getPlate() {
    return plate;
  }

  public boolean isVertical() {
    return vertical;
  }

  public PlaqueInk getInk() {
    return ink;
  }

  /** The numerals' colour on this style. */
  public int inkFor(PlaqueStyle style) {
    return (ink == PlaqueInk.DEFAULT ? style.getDefaultInk() : ink).getRgb();
  }

  public int getDx() {
    return dx;
  }

  public int getDy() {
    return dy;
  }

  public int getDz() {
    return dz;
  }

  public boolean isLit() {
    return lit;
  }

  public Builder toBuilder() {
    Builder b = new Builder();
    b.number = number;
    b.street = street;
    b.font = font;
    b.size = size;
    b.plate = plate;
    b.vertical = vertical;
    b.ink = ink;
    b.dx = dx;
    b.dy = dy;
    b.dz = dz;
    b.lit = lit;
    return b;
  }

  // --- NBT -----------------------------------------------------------------------------------

  /** Writes the plaque's keys into {@code compound}, defaults left out except the number. */
  public NBTTagCompound write(NBTTagCompound compound) {
    compound.setString(KEY_NUMBER, number);
    if (!street.isEmpty()) {
      compound.setString(KEY_STREET, street);
    }
    if (font != null) {
      compound.setString(KEY_FONT, font.getId());
    }
    if (size < 1f) {
      compound.setFloat(KEY_SIZE, size);
    }
    if (plate != PlaqueSize.MEDIUM) {
      compound.setString(KEY_PLATE, plate.getId());
    }
    if (vertical) {
      compound.setString(KEY_LAYOUT, "v");
    }
    if (ink != PlaqueInk.DEFAULT) {
      compound.setString(KEY_INK, ink.getId());
    }
    if (dx != 0) {
      compound.setByte(KEY_DX, (byte) dx);
    }
    if (dy != 0) {
      compound.setByte(KEY_DY, (byte) dy);
    }
    if (dz != 0) {
      compound.setByte(KEY_DZ, (byte) dz);
    }
    if (!lit) {
      compound.setByte(KEY_LIT, (byte) 0);
    }
    return compound;
  }

  /** Whether a compound carries a plaque (any of its keys). */
  public static boolean isPlaque(NBTTagCompound compound) {
    for (String key : new String[]{KEY_NUMBER, KEY_STREET, KEY_FONT, KEY_SIZE, KEY_PLATE,
        KEY_LAYOUT, KEY_INK, KEY_DX, KEY_DY, KEY_DZ, KEY_LIT}) {
      if (compound.hasKey(key)) {
        return true;
      }
    }
    return false;
  }

  /** Reads a plaque, every key cleaned and clamped; a missing key is its default. */
  public static AddressPlaque read(NBTTagCompound c) {
    Builder b = new Builder();
    if (c.hasKey(KEY_NUMBER)) {
      // a number written as a number (/blockdata {num:12}) reads as its digits
      b.number = c.hasKey(KEY_NUMBER, 8) ? c.getString(KEY_NUMBER)
          : String.valueOf(c.getInteger(KEY_NUMBER));
    }
    b.street = c.getString(KEY_STREET);
    b.font = c.hasKey(KEY_FONT) ? PlaqueFont.byId(c.getString(KEY_FONT)) : null;
    b.size = c.hasKey(KEY_SIZE) ? c.getFloat(KEY_SIZE) : 1f;
    b.plate = PlaqueSize.byId(c.getString(KEY_PLATE));
    b.vertical = "v".equals(c.getString(KEY_LAYOUT));
    b.ink = PlaqueInk.byId(c.getString(KEY_INK));
    b.dx = c.getInteger(KEY_DX);
    b.dy = c.getInteger(KEY_DY);
    b.dz = c.getInteger(KEY_DZ);
    b.lit = !c.hasKey(KEY_LIT) || c.getInteger(KEY_LIT) != 0;
    return b.build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AddressPlaque)) {
      return false;
    }
    AddressPlaque p = (AddressPlaque) o;
    return Float.compare(p.size, size) == 0 && vertical == p.vertical && dx == p.dx
        && dy == p.dy && dz == p.dz && lit == p.lit && number.equals(p.number)
        && street.equals(p.street) && font == p.font && plate == p.plate && ink == p.ink;
  }

  @Override
  public int hashCode() {
    return Objects.hash(number, street, font, size, plate, vertical, ink, dx, dy, dz, lit);
  }

  @Override
  public String toString() {
    return number + (street.isEmpty() ? "" : " / " + street);
  }

  /** Builds a plaque; anything not set is its default. */
  public static final class Builder {

    private String number = DEFAULT_NUMBER;
    private String street = "";
    private PlaqueFont font;
    private float size = 1f;
    private PlaqueSize plate = PlaqueSize.MEDIUM;
    private boolean vertical;
    private PlaqueInk ink = PlaqueInk.DEFAULT;
    private int dx;
    private int dy;
    private int dz;
    private boolean lit = true;

    public Builder number(String v) {
      number = v;
      return this;
    }

    public Builder street(String v) {
      street = v;
      return this;
    }

    public Builder font(@Nullable PlaqueFont v) {
      font = v;
      return this;
    }

    public Builder size(float v) {
      size = v;
      return this;
    }

    public Builder plate(PlaqueSize v) {
      plate = v;
      return this;
    }

    public Builder vertical(boolean v) {
      vertical = v;
      return this;
    }

    public Builder ink(PlaqueInk v) {
      ink = v;
      return this;
    }

    public Builder dx(int v) {
      dx = v;
      return this;
    }

    public Builder dy(int v) {
      dy = v;
      return this;
    }

    public Builder dz(int v) {
      dz = v;
      return this;
    }

    public Builder lit(boolean v) {
      lit = v;
      return this;
    }

    public AddressPlaque build() {
      return new AddressPlaque(this);
    }
  }
}
