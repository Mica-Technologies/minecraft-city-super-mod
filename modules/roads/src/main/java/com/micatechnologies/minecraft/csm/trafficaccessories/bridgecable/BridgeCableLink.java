package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

/**
 * One cable an anchor owns: where it runs to and how it looks. Immutable.
 *
 * <p>The target is held as an offset from the owning anchor, not as a world position, so a bridge
 * copied or pasted somewhere else (a schematic, a structure block) keeps its cables. Builders and
 * generators write absolute targets; they are turned into an offset as they are read. See
 * {@code assets/docs/BRIDGE_CABLE_SYSTEM.md} for the NBT and examples.
 *
 * <p>Fields left unset in NBT stay unset here (zero, null or NaN) and fall back to the style's
 * defaults when drawn, so a default can be improved without rewriting every placed cable.
 */
public final class BridgeCableLink {

  /** Most cables one anchor holds. A pylon socket carries a few stays; sixteen is generous. */
  public static final int MAX_CABLES = 16;

  /** Thickness limits in pixels. The documented choices are 2, 4, 6 and 8. */
  public static final int MIN_THICKNESS = 1;
  public static final int MAX_THICKNESS = 16;

  /** Most hangers one cable drops, so a typo'd spacing of 0.1 cannot fill the world. */
  public static final int MAX_HANGERS = 256;

  /** Hanger thickness when a link gives none, in pixels. */
  public static final int DEFAULT_HANGER_THICKNESS = 2;

  public static final String KEY_CABLES = "cables";
  public static final String KEY_TARGET = "target";
  public static final String KEY_OFFSET = "off";
  public static final String KEY_DX = "dx";
  public static final String KEY_DY = "dy";
  public static final String KEY_DZ = "dz";
  public static final String KEY_STYLE = "style";
  public static final String KEY_THICK = "thick";
  public static final String KEY_COLOUR = "color";
  public static final String KEY_SAG = "sag";
  public static final String KEY_STRIPES = "stripes";
  public static final String KEY_LED = "led";
  public static final String KEY_HANGERS = "hangers";
  public static final String KEY_HANGER_THICK = "hangerThick";
  public static final String KEY_HANGER_COLOUR = "hangerColor";
  public static final String KEY_REMOVE = "remove";

  private final int dx;
  private final int dy;
  private final int dz;
  private final BridgeCableStyle style;
  private final int thickness;
  @Nullable
  private final String colour;
  private final double sag;
  private final boolean sagPercent;
  @Nullable
  private final String stripes;
  @Nullable
  private final String led;
  private final int hangerSpacing;
  private final int hangerThickness;
  @Nullable
  private final String hangerColour;

  public BridgeCableLink(int dx, int dy, int dz, BridgeCableStyle style, int thickness,
      @Nullable String colour, double sag, boolean sagPercent, @Nullable String stripes,
      @Nullable String led, int hangerSpacing, int hangerThickness,
      @Nullable String hangerColour) {
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
    this.style = style == null ? BridgeCableStyle.STAY : style;
    this.thickness = thickness <= 0 ? 0 : clampThickness(thickness);
    this.colour = BridgeCableColour.normalise(colour);
    this.sag = sag;
    this.sagPercent = sagPercent;
    this.stripes = BridgeCableColour.normalise(stripes);
    this.led = BridgeCableColour.normalise(led);
    this.hangerSpacing = Math.max(0, hangerSpacing);
    this.hangerThickness = hangerThickness <= 0 ? 0 : clampThickness(hangerThickness);
    this.hangerColour = BridgeCableColour.normalise(hangerColour);
  }

  /** A plain cable of a style, with every look left to the style's defaults. */
  public static BridgeCableLink of(BlockPos owner, BlockPos target, BridgeCableStyle style) {
    return new BridgeCableLink(target.getX() - owner.getX(), target.getY() - owner.getY(),
        target.getZ() - owner.getZ(), style, 0, null, Double.NaN, false, null, null, 0, 0, null);
  }

  /** The same cable with a hanger spacing. */
  public BridgeCableLink withHangers(int spacing) {
    return new BridgeCableLink(dx, dy, dz, style, thickness, colour, sag, sagPercent, stripes,
        led, spacing, hangerThickness, hangerColour);
  }

  private static int clampThickness(int px) {
    return Math.max(MIN_THICKNESS, Math.min(MAX_THICKNESS, px));
  }

  // --- what is drawn -----------------------------------------------------------------------

  public BlockPos getTarget(BlockPos owner) {
    return owner.add(dx, dy, dz);
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

  public BridgeCableStyle getStyle() {
    return style;
  }

  /** Thickness in pixels, the style's default when unset. */
  public int getThickness() {
    return thickness > 0 ? thickness : style.getDefaultThickness();
  }

  /** Radius in blocks: a thickness of {@code n} pixels is {@code n/16} across. */
  public double getRadius() {
    return getThickness() / 32.0;
  }

  /** Colour name or {@code #rrggbb}, the style's default when unset. */
  public String getColour() {
    return colour != null ? colour : style.getDefaultColour();
  }

  @Nullable
  public String getStripes() {
    return stripes;
  }

  @Nullable
  public String getLed() {
    return led;
  }

  /** Hanger spacing in blocks, or 0 for none. Only a hanging style drops hangers. */
  public int getHangerSpacing() {
    return style.hangs() ? hangerSpacing : 0;
  }

  public double getHangerRadius() {
    return (hangerThickness > 0 ? hangerThickness : DEFAULT_HANGER_THICKNESS) / 32.0;
  }

  public String getHangerColour() {
    return hangerColour != null ? hangerColour : getColour();
  }

  /**
   * How far the cable's middle hangs below the straight line between its ends, in blocks.
   * Straight styles never sag, whatever they were given.
   *
   * @param horizontalLength the plan-view length of the span, which a percentage is taken of
   */
  public double getSagBlocks(double horizontalLength) {
    if (!style.hangs()) {
      return 0.0;
    }
    final boolean given = !Double.isNaN(sag);
    final double value = given ? sag : style.getDefaultSag();
    final boolean percent = given ? sagPercent : style.isDefaultSagPercent();
    final double blocks = percent ? horizontalLength * value / 100.0 : value;
    return Math.max(0.0, blocks);
  }

  // --- NBT -----------------------------------------------------------------------------------

  /**
   * Reads one cable from a compound: an entry of the {@code cables} list, or the anchor's own
   * compound in the single-cable form.
   *
   * @param owner the anchor's position, which an absolute {@code target} is taken from
   *
   * @return the cable, or null when the compound names no target
   */
  @Nullable
  public static BridgeCableLink read(NBTTagCompound tag, BlockPos owner) {
    int[] offset = null;
    final int[] target = readTriple(tag.getTag(KEY_TARGET));
    if (target != null) {
      offset = new int[]{target[0] - owner.getX(), target[1] - owner.getY(),
          target[2] - owner.getZ()};
    }
    if (offset == null) {
      offset = readTriple(tag.getTag(KEY_OFFSET));
    }
    if (offset == null && tag.hasKey(KEY_DX) && tag.hasKey(KEY_DY) && tag.hasKey(KEY_DZ)) {
      final Double x = readNumber(tag.getTag(KEY_DX));
      final Double y = readNumber(tag.getTag(KEY_DY));
      final Double z = readNumber(tag.getTag(KEY_DZ));
      if (x != null && y != null && z != null) {
        offset = new int[]{(int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)};
      }
    }
    if (offset == null || (offset[0] == 0 && offset[1] == 0 && offset[2] == 0)) {
      return null;
    }

    BridgeCableStyle style = BridgeCableStyle.byName(tag.getString(KEY_STYLE));
    if (style == null) {
      style = BridgeCableStyle.STAY;
    }
    final Double thick = readNumber(tag.getTag(KEY_THICK));
    double sag = Double.NaN;
    boolean sagPercent = false;
    final NBTBase sagTag = tag.getTag(KEY_SAG);
    if (sagTag instanceof NBTTagString) {
      String text = ((NBTTagString) sagTag).getString().trim().toLowerCase(Locale.ROOT);
      if (text.endsWith("%")) {
        sagPercent = true;
        text = text.substring(0, text.length() - 1).trim();
      }
      try {
        sag = Double.parseDouble(text);
      } catch (NumberFormatException ignored) {
        sag = Double.NaN;
        sagPercent = false;
      }
    } else {
      final Double value = readNumber(sagTag);
      if (value != null) {
        sag = value;
      }
    }
    final Double hangers = readNumber(tag.getTag(KEY_HANGERS));
    final Double hangerThick = readNumber(tag.getTag(KEY_HANGER_THICK));

    return new BridgeCableLink(offset[0], offset[1], offset[2], style,
        thick == null ? 0 : (int) Math.round(thick),
        stringOrNull(tag, KEY_COLOUR), sag, sagPercent,
        stringOrNull(tag, KEY_STRIPES), stringOrNull(tag, KEY_LED),
        hangers == null ? 0 : (int) Math.round(hangers),
        hangerThick == null ? 0 : (int) Math.round(hangerThick),
        stringOrNull(tag, KEY_HANGER_COLOUR));
  }

  /**
   * Writes the cable as one entry of the {@code cables} list. Only what was set is written, so
   * an unset look keeps following the style's default.
   */
  public NBTTagCompound write() {
    final NBTTagCompound tag = new NBTTagCompound();
    tag.setIntArray(KEY_OFFSET, new int[]{dx, dy, dz});
    tag.setString(KEY_STYLE, style.getName());
    if (thickness > 0) {
      tag.setInteger(KEY_THICK, thickness);
    }
    if (colour != null) {
      tag.setString(KEY_COLOUR, colour);
    }
    if (!Double.isNaN(sag)) {
      if (sagPercent) {
        tag.setString(KEY_SAG, formatNumber(sag) + "%");
      } else {
        tag.setFloat(KEY_SAG, (float) sag);
      }
    }
    if (stripes != null) {
      tag.setString(KEY_STRIPES, stripes);
    }
    if (led != null) {
      tag.setString(KEY_LED, led);
    }
    if (hangerSpacing > 0) {
      tag.setInteger(KEY_HANGERS, hangerSpacing);
    }
    if (hangerThickness > 0) {
      tag.setInteger(KEY_HANGER_THICK, hangerThickness);
    }
    if (hangerColour != null) {
      tag.setString(KEY_HANGER_COLOUR, hangerColour);
    }
    return tag;
  }

  /**
   * Reads every cable an anchor's compound describes.
   *
   * <p>The {@code cables} list comes first. Then the single-cable form, a {@code target} (or
   * {@code dx}, {@code dy}, {@code dz}) at the top of the compound, is put in: it replaces the
   * cable already running to the same block, or is added. That makes one {@code /blockdata} per
   * cable work for a pylon socket carrying several stays, and re-running a generator change
   * nothing. Last, {@code remove:[x,y,z]} drops the cable to that block.
   *
   * <p>The single-cable keys are never written back, so they act once: the next save holds only
   * the list.
   */
  public static List<BridgeCableLink> readAll(NBTTagCompound compound, BlockPos owner) {
    final List<BridgeCableLink> links = new ArrayList<>();
    if (compound.hasKey(KEY_CABLES, Constants.NBT.TAG_LIST)) {
      final NBTTagList list = compound.getTagList(KEY_CABLES, Constants.NBT.TAG_COMPOUND);
      for (int i = 0; i < list.tagCount() && links.size() < MAX_CABLES; i++) {
        final BridgeCableLink link = read(list.getCompoundTagAt(i), owner);
        if (link != null) {
          putInto(links, link);
        }
      }
    }
    if (compound.hasKey(KEY_TARGET) || compound.hasKey(KEY_DX)) {
      final BridgeCableLink single = read(compound, owner);
      if (single != null) {
        putInto(links, single);
      }
    }
    final int[] remove = readTriple(compound.getTag(KEY_REMOVE));
    if (remove != null) {
      final int rx = remove[0] - owner.getX();
      final int ry = remove[1] - owner.getY();
      final int rz = remove[2] - owner.getZ();
      links.removeIf(link -> link.dx == rx && link.dy == ry && link.dz == rz);
    }
    return links;
  }

  /** Writes a list of cables into an anchor's compound, replacing any list there. */
  public static void writeAll(NBTTagCompound compound, List<BridgeCableLink> links) {
    final NBTTagList list = new NBTTagList();
    for (BridgeCableLink link : links) {
      list.appendTag(link.write());
    }
    compound.setTag(KEY_CABLES, list);
  }

  /**
   * Puts a cable into a list: replaces the one to the same target, else adds it while there is
   * room.
   *
   * @return false when the list was full and the cable was not added
   */
  public static boolean putInto(List<BridgeCableLink> links, BridgeCableLink link) {
    for (int i = 0; i < links.size(); i++) {
      if (links.get(i).sameTarget(link)) {
        links.set(i, link);
        return true;
      }
    }
    if (links.size() >= MAX_CABLES) {
      return false;
    }
    links.add(link);
    return true;
  }

  /** Whether two cables run to the same block (from the same anchor). */
  public boolean sameTarget(BridgeCableLink other) {
    return dx == other.dx && dy == other.dy && dz == other.dz;
  }

  /**
   * Reads three whole numbers from an int array, a list of numbers, or a compound with x, y and
   * z, which is every way {@code /blockdata} lets a builder write a position.
   */
  @Nullable
  static int[] readTriple(@Nullable NBTBase tag) {
    if (tag instanceof NBTTagIntArray) {
      final int[] values = ((NBTTagIntArray) tag).getIntArray();
      return values.length == 3 ? values.clone() : null;
    }
    if (tag instanceof NBTTagList) {
      final NBTTagList list = (NBTTagList) tag;
      if (list.tagCount() != 3) {
        return null;
      }
      final int[] values = new int[3];
      for (int i = 0; i < 3; i++) {
        final Double value = readNumber(list.get(i));
        if (value == null) {
          return null;
        }
        values[i] = (int) Math.floor(value);
      }
      return values;
    }
    if (tag instanceof NBTTagCompound) {
      final NBTTagCompound xyz = (NBTTagCompound) tag;
      final Double x = readNumber(xyz.getTag("x"));
      final Double y = readNumber(xyz.getTag("y"));
      final Double z = readNumber(xyz.getTag("z"));
      if (x != null && y != null && z != null) {
        return new int[]{(int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)};
      }
    }
    return null;
  }

  /** A number from any numeric tag, or from a string holding one; null otherwise. */
  @Nullable
  static Double readNumber(@Nullable NBTBase tag) {
    if (tag instanceof NBTPrimitive) {
      return ((NBTPrimitive) tag).getDouble();
    }
    if (tag instanceof NBTTagString) {
      String text = ((NBTTagString) tag).getString().trim().toLowerCase(Locale.ROOT);
      if (text.endsWith("px")) {
        text = text.substring(0, text.length() - 2).trim();
      }
      try {
        return Double.parseDouble(text);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  @Nullable
  private static String stringOrNull(NBTTagCompound tag, String key) {
    return tag.hasKey(key, Constants.NBT.TAG_STRING) ? tag.getString(key) : null;
  }

  private static String formatNumber(double value) {
    if (value == Math.rint(value)) {
      return Long.toString((long) value);
    }
    return Double.toString(value);
  }

  /** An unmodifiable copy, for handing a tile entity's list out. */
  public static List<BridgeCableLink> copyOf(List<BridgeCableLink> links) {
    return Collections.unmodifiableList(new ArrayList<>(links));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BridgeCableLink)) {
      return false;
    }
    final BridgeCableLink that = (BridgeCableLink) o;
    return dx == that.dx && dy == that.dy && dz == that.dz
        && thickness == that.thickness
        && Double.compare(sag, that.sag) == 0
        && sagPercent == that.sagPercent
        && hangerSpacing == that.hangerSpacing
        && hangerThickness == that.hangerThickness
        && style == that.style
        && Objects.equals(colour, that.colour)
        && Objects.equals(stripes, that.stripes)
        && Objects.equals(led, that.led)
        && Objects.equals(hangerColour, that.hangerColour);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dx, dy, dz, style, thickness, colour, sag, sagPercent, stripes, led,
        hangerSpacing, hangerThickness, hangerColour);
  }

  @Override
  public String toString() {
    return style.getName() + " to (" + dx + "," + dy + "," + dz + ") " + getThickness() + "px "
        + getColour();
  }
}
