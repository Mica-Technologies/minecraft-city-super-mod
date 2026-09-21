package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nonnull;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IStringSerializable;

/**
 * How one exit sign is set up: its arrows, letter colour, housing finish, mount, emergency heads
 * and legend. Immutable; the {@code with} methods return a changed copy.
 *
 * <p>A sign keeps this in its {@link TileEntityExitSign} and its item keeps it in the stack's NBT,
 * both under the same short keys, so a sign picked, broken or placed keeps its setup. Each option
 * is saved as its enum's ordinal: <b>never reorder or remove an enum constant</b>, only append,
 * or every placed sign changes. A value a block does not offer is replaced by that block's default
 * when the state is read ({@link ExitSignSpec#clamp}), so a stale or hand-edited value can never
 * reach a model that has no part for it.
 *
 * @since 2026.9
 */
public final class ExitSignConfig {

  /** Which chevrons are lit. The others are moulded into the housing, unlit. */
  public enum Arrow implements IStringSerializable {
    NONE, LEFT, RIGHT, BOTH;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** The colour the legend and arrows light in. */
  public enum Letters implements IStringSerializable {
    RED, GREEN;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** The housing's finish, which the mount matches. */
  public enum Housing implements IStringSerializable {
    WHITE, BLACK, BRUSHED;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /**
   * How the sign is hung: flat against a wall (back mount), from a canopy on the ceiling (top
   * mount), or end-on from a wall to its left or right (end mount), which shows both faces.
   */
  public enum Mount implements IStringSerializable {
    WALL, CEILING, END_LEFT, END_RIGHT;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** The emergency lamp heads on the sign's ends, which light when mains power is lost. */
  public enum Heads implements IStringSerializable {
    NONE, SQUARE, ROUND;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** What the sign says. */
  public enum Legend implements IStringSerializable {
    EXIT, SALIDA;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  // Short NBT keys, shared by the tile entity and the item stack's tag.
  static final String KEY_ARROW = "ar";
  static final String KEY_LETTERS = "lc";
  static final String KEY_HOUSING = "hs";
  static final String KEY_MOUNT = "mt";
  static final String KEY_HEADS = "hd";
  static final String KEY_LEGEND = "lg";

  private final Arrow arrow;
  private final Letters letters;
  private final Housing housing;
  private final Mount mount;
  private final Heads heads;
  private final Legend legend;

  public ExitSignConfig(Arrow arrow, Letters letters, Housing housing, Mount mount, Heads heads,
      Legend legend) {
    this.arrow = Objects.requireNonNull(arrow);
    this.letters = Objects.requireNonNull(letters);
    this.housing = Objects.requireNonNull(housing);
    this.mount = Objects.requireNonNull(mount);
    this.heads = Objects.requireNonNull(heads);
    this.legend = Objects.requireNonNull(legend);
  }

  public Arrow getArrow() {
    return arrow;
  }

  public Letters getLetters() {
    return letters;
  }

  public Housing getHousing() {
    return housing;
  }

  public Mount getMount() {
    return mount;
  }

  public Heads getHeads() {
    return heads;
  }

  public Legend getLegend() {
    return legend;
  }

  public ExitSignConfig withArrow(Arrow value) {
    return new ExitSignConfig(value, letters, housing, mount, heads, legend);
  }

  public ExitSignConfig withLetters(Letters value) {
    return new ExitSignConfig(arrow, value, housing, mount, heads, legend);
  }

  public ExitSignConfig withHousing(Housing value) {
    return new ExitSignConfig(arrow, letters, value, mount, heads, legend);
  }

  public ExitSignConfig withMount(Mount value) {
    return new ExitSignConfig(arrow, letters, housing, value, heads, legend);
  }

  public ExitSignConfig withHeads(Heads value) {
    return new ExitSignConfig(arrow, letters, housing, mount, value, legend);
  }

  public ExitSignConfig withLegend(Legend value) {
    return new ExitSignConfig(arrow, letters, housing, mount, heads, value);
  }

  /**
   * Reads a config from {@code compound}. A key that is missing or out of range keeps the value
   * from {@code fallback}, so an empty compound reads as the fallback itself.
   */
  public static ExitSignConfig read(NBTTagCompound compound, ExitSignConfig fallback) {
    return new ExitSignConfig(
        byOrdinal(Arrow.values(), compound, KEY_ARROW, fallback.arrow),
        byOrdinal(Letters.values(), compound, KEY_LETTERS, fallback.letters),
        byOrdinal(Housing.values(), compound, KEY_HOUSING, fallback.housing),
        byOrdinal(Mount.values(), compound, KEY_MOUNT, fallback.mount),
        byOrdinal(Heads.values(), compound, KEY_HEADS, fallback.heads),
        byOrdinal(Legend.values(), compound, KEY_LEGEND, fallback.legend));
  }

  /** Writes every option into {@code compound} and returns it. */
  public NBTTagCompound write(NBTTagCompound compound) {
    compound.setByte(KEY_ARROW, (byte) arrow.ordinal());
    compound.setByte(KEY_LETTERS, (byte) letters.ordinal());
    compound.setByte(KEY_HOUSING, (byte) housing.ordinal());
    compound.setByte(KEY_MOUNT, (byte) mount.ordinal());
    compound.setByte(KEY_HEADS, (byte) heads.ordinal());
    compound.setByte(KEY_LEGEND, (byte) legend.ordinal());
    return compound;
  }

  /**
   * Every option packed into one number, three bits each. Used as the tile entity's baked model
   * key, so a sync that changes nothing a model reads does not rebuild the chunk section.
   */
  public int pack() {
    return arrow.ordinal() | letters.ordinal() << 3 | housing.ordinal() << 6
        | mount.ordinal() << 9 | heads.ordinal() << 12 | legend.ordinal() << 15;
  }

  private static <E extends Enum<E>> E byOrdinal(E[] values, NBTTagCompound compound, String key,
      E fallback) {
    if (!compound.hasKey(key)) {
      return fallback;
    }
    int ordinal = compound.getByte(key);
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ExitSignConfig)) {
      return false;
    }
    return pack() == ((ExitSignConfig) o).pack();
  }

  @Override
  public int hashCode() {
    return pack();
  }

  @Override
  public String toString() {
    return "ExitSignConfig{" + legend + ' ' + letters + " on " + housing + ", arrow " + arrow
        + ", " + mount + ", heads " + heads + '}';
  }
}
