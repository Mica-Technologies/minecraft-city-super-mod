package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

import java.util.Arrays;
import java.util.Objects;
import net.minecraft.nbt.NBTTagCompound;

/**
 * What a wayfinding panel says and how it looks: one immutable value, held by every cell of the
 * panel ({@link AbstractTileEntityWayfindingPanel}) and read by its baked model, so identical
 * signs bake once. {@link #toBuilder()} makes a changed copy.
 *
 * <p><b>The NBT</b>, every key optional. The airport and hospital panels' keys keep their
 * meaning: {@code l1} {@code l2} the lines, {@code p} the pictogram, {@code a} the arrow,
 * {@code s} the scheme, {@code d} double-sided, {@code ts} {@code ts1} {@code ts2} the text
 * scales. Added for #307: {@code l3} {@code l4} and {@code ts3} {@code ts4}; {@code n} the line
 * count (0, the default, shows the lines that are not empty, the old rule; 1 to 4 shows exactly
 * that many, an empty one a gap); {@code al} the alignment ({@code l}, {@code c}, {@code r});
 * {@code il1}..{@code il4} and {@code ir1}..{@code ir4} an icon left and right of each line;
 * {@code is} the icon style ({@code tile} or {@code plain}); {@code m} the
 * {@link WayfindingMount mount}; {@code rd} the drop, sixteenths; {@code sc} the panel's scale;
 * {@code bg} and {@code fg} custom background and legend colours ({@code #RRGGBB}); {@code bd}
 * the border (0 to 2); {@code lt} whether the panel is lit.</p>
 *
 * <p><b>What is written.</b> The old keys always (a line only when it has text), the new ones
 * only when they differ from their default, so a sign that uses none of them writes exactly what
 * it always did. A compound is a sign only if it has {@code p} or any key added for #307
 * ({@link #isSign}): a cell never edited writes no sign at all, and keeps the block's default.
 * Within a sign a missing key is empty, none or off, except the scheme (the block's default),
 * {@code d} (on) and {@code lt} (on).</p>
 *
 * @since 2026.10
 */
public final class WayfindingSign {

  /** The most lines a sign has. */
  public static final int MAX_LINES = 4;
  /** Characters a line may hold. */
  public static final int MAX_LINE_LENGTH = 32;
  /** The smallest a line's text scale ({@code ts} times its own) may be. */
  public static final float MIN_TEXT_SCALE = 0.5f;
  /**
   * The largest: the size the panel's height gives, which a scale only ever makes smaller. To
   * make the whole sign bigger, scale the panel ({@code sc}).
   */
  public static final float MAX_TEXT_SCALE = 1f;
  /** The smallest and largest panel scale ({@code sc}). */
  public static final float MIN_PANEL_SCALE = 0.25f;
  public static final float MAX_PANEL_SCALE = 2f;
  /** The largest drop ({@code rd}), sixteenths. */
  public static final int MAX_DROP = 16;
  /** The thickest border ({@code bd}). */
  public static final int MAX_BORDER = 2;
  /** No custom colour. */
  public static final int NO_COLOUR = -1;

  /** How the lines sit across the panel. */
  public enum Align {
    LEFT("l"),
    CENTRE("c"),
    RIGHT("r");

    private final String id;

    Align(String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }

    public Align next() {
      return values()[(ordinal() + 1) % values().length];
    }

    public static Align byId(String id) {
      for (Align a : values()) {
        if (a.id.equals(id)) {
          return a;
        }
      }
      return LEFT;
    }
  }

  /** How an icon is drawn: on a tile in the pictogram colour, or plain in the legend's. */
  public enum IconStyle {
    TILE("tile"),
    PLAIN("plain");

    private final String id;

    IconStyle(String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }

    public IconStyle next() {
      return values()[(ordinal() + 1) % values().length];
    }

    public static IconStyle byId(String id) {
      return PLAIN.id.equals(id) ? PLAIN : TILE;
    }
  }

  private static final String[] KEY_LINE = {"l1", "l2", "l3", "l4"};
  private static final String[] KEY_LINE_SCALE = {"ts1", "ts2", "ts3", "ts4"};
  private static final String[] KEY_ICON_LEFT = {"il1", "il2", "il3", "il4"};
  private static final String[] KEY_ICON_RIGHT = {"ir1", "ir2", "ir3", "ir4"};
  private static final String KEY_PICTOGRAM = "p";
  private static final String KEY_ARROW = "a";
  private static final String KEY_SCHEME = "s";
  private static final String KEY_DOUBLE = "d";
  private static final String KEY_TEXT_SCALE = "ts";
  private static final String KEY_LINE_COUNT = "n";
  private static final String KEY_ALIGN = "al";
  private static final String KEY_ICON_STYLE = "is";
  private static final String KEY_MOUNT = "m";
  private static final String KEY_DROP = "rd";
  private static final String KEY_PANEL_SCALE = "sc";
  private static final String KEY_BACKGROUND = "bg";
  private static final String KEY_LEGEND = "fg";
  private static final String KEY_BORDER = "bd";
  private static final String KEY_LIT = "lt";
  /** The keys added for #307: any one of them makes a compound a sign, as {@code p} does. */
  private static final String[] NEW_KEYS = {"l3", "l4", "ts3", "ts4", "il1", "il2", "il3",
      "il4", "ir1", "ir2", "ir3", "ir4", KEY_LINE_COUNT, KEY_ALIGN, KEY_ICON_STYLE, KEY_MOUNT,
      KEY_DROP, KEY_PANEL_SCALE, KEY_BACKGROUND, KEY_LEGEND, KEY_BORDER, KEY_LIT};

  private final String[] lines;
  private final float[] lineScales;
  private final float textScale;
  private final WayfindingIcon pictogram;
  private final WayfindingArrow arrow;
  private final WayfindingScheme scheme;
  private final boolean doubleSided;
  private final int lineCount;
  private final Align align;
  private final WayfindingIcon[] iconsLeft;
  private final WayfindingIcon[] iconsRight;
  private final IconStyle iconStyle;
  private final WayfindingMount mount;
  private final int drop;
  private final float panelScale;
  private final int background;
  private final int legend;
  private final int border;
  private final boolean lit;
  private final int hash;

  private WayfindingSign(Builder b) {
    lines = new String[MAX_LINES];
    lineScales = new float[MAX_LINES];
    iconsLeft = new WayfindingIcon[MAX_LINES];
    iconsRight = new WayfindingIcon[MAX_LINES];
    for (int i = 0; i < MAX_LINES; i++) {
      lines[i] = clamp(b.lines[i]);
      lineScales[i] = clampTextScale(b.lineScales[i]);
      iconsLeft[i] = b.iconsLeft[i] == null ? WayfindingIcon.NONE : b.iconsLeft[i];
      iconsRight[i] = b.iconsRight[i] == null ? WayfindingIcon.NONE : b.iconsRight[i];
    }
    textScale = clampTextScale(b.textScale);
    pictogram = b.pictogram == null ? WayfindingIcon.NONE : b.pictogram;
    arrow = b.arrow == null ? WayfindingArrow.NONE : b.arrow;
    scheme = b.scheme == null ? WayfindingScheme.CORPORATE : b.scheme;
    doubleSided = b.doubleSided;
    lineCount = Math.max(0, Math.min(MAX_LINES, b.lineCount));
    align = b.align == null ? Align.LEFT : b.align;
    iconStyle = b.iconStyle == null ? IconStyle.TILE : b.iconStyle;
    mount = b.mount == null ? WayfindingMount.AUTO : b.mount;
    drop = Math.max(0, Math.min(MAX_DROP, b.drop));
    panelScale = clampPanelScale(b.panelScale);
    background = b.background < 0 ? NO_COLOUR : b.background & 0xFFFFFF;
    legend = b.legend < 0 ? NO_COLOUR : b.legend & 0xFFFFFF;
    border = Math.max(0, Math.min(MAX_BORDER, b.border));
    lit = b.lit;
    hash = Objects.hash(Arrays.hashCode(lines), Arrays.hashCode(lineScales), textScale,
        pictogram, arrow, scheme, doubleSided, lineCount, align, Arrays.hashCode(iconsLeft),
        Arrays.hashCode(iconsRight), iconStyle, mount, drop, panelScale, background, legend,
        border, lit);
  }

  /** A builder starting from the plainest sign: no text, no art, the corporate scheme. */
  public static Builder builder() {
    return new Builder();
  }

  /** A builder holding this sign. */
  public Builder toBuilder() {
    Builder b = new Builder();
    for (int i = 0; i < MAX_LINES; i++) {
      b.lines[i] = lines[i];
      b.lineScales[i] = lineScales[i];
      b.iconsLeft[i] = iconsLeft[i];
      b.iconsRight[i] = iconsRight[i];
    }
    b.textScale = textScale;
    b.pictogram = pictogram;
    b.arrow = arrow;
    b.scheme = scheme;
    b.doubleSided = doubleSided;
    b.lineCount = lineCount;
    b.align = align;
    b.iconStyle = iconStyle;
    b.mount = mount;
    b.drop = drop;
    b.panelScale = panelScale;
    b.background = background;
    b.legend = legend;
    b.border = border;
    b.lit = lit;
    return b;
  }

  // --- reading --------------------------------------------------------------------------------

  /** A line's text, 0-based, empty for none. */
  public String getLine(int i) {
    return lines[i];
  }

  /** A line's own text scale ({@code ts1}..{@code ts4}), 1 for none. */
  public float getLineScale(int i) {
    return lineScales[i];
  }

  /** The legend's text scale ({@code ts}), 1 for none. */
  public float getTextScale() {
    return textScale;
  }

  /** The scale a line is drawn at, the legend's times its own, clamped. */
  public float getLineTextScale(int i) {
    return lineTextScale(textScale, lineScales[i]);
  }

  public WayfindingIcon getPictogram() {
    return pictogram;
  }

  public WayfindingArrow getArrow() {
    return arrow;
  }

  public WayfindingScheme getScheme() {
    return scheme;
  }

  /** Whether the back carries the legend too (never on a wall). */
  public boolean isDoubleSided() {
    return doubleSided;
  }

  /** The line count ({@code n}), 0 for the lines that have text. */
  public int getLineCount() {
    return lineCount;
  }

  public Align getAlign() {
    return align;
  }

  public WayfindingIcon getIconLeft(int i) {
    return iconsLeft[i];
  }

  public WayfindingIcon getIconRight(int i) {
    return iconsRight[i];
  }

  public IconStyle getIconStyle() {
    return iconStyle;
  }

  public WayfindingMount getMount() {
    return mount;
  }

  /** The drop ({@code rd}), sixteenths: down from the top when hung, up from the bottom on posts. */
  public int getDrop() {
    return drop;
  }

  /** The panel's size as a share of its cells ({@code sc}). */
  public float getPanelScale() {
    return panelScale;
  }

  /** The custom background colour, or {@link #NO_COLOUR}. */
  public int getCustomBackground() {
    return background;
  }

  /** The custom legend colour, or {@link #NO_COLOUR}. */
  public int getCustomLegend() {
    return legend;
  }

  public int getBorder() {
    return border;
  }

  public boolean isLit() {
    return lit;
  }

  /** The background colour drawn: the custom one, else the scheme's. */
  public int background() {
    return background != NO_COLOUR ? background : scheme.getBackground();
  }

  /** The legend's (and arrows' and border's) colour drawn. */
  public int legend() {
    return legend != NO_COLOUR ? legend : scheme.getLegend();
  }

  /** A pictogram tile's colour drawn: the custom legend colour when there is one. */
  public int pictogramTile() {
    return legend != NO_COLOUR ? legend : scheme.getPictogram();
  }

  /**
   * The lines shown, top first, as indexes into the four: with no line count those that have
   * text, otherwise the first {@link #getLineCount()} whatever they hold.
   */
  public int[] shownLines() {
    if (lineCount > 0) {
      int[] out = new int[lineCount];
      for (int i = 0; i < lineCount; i++) {
        out[i] = i;
      }
      return out;
    }
    int n = 0;
    for (String l : lines) {
      if (!l.isEmpty()) {
        n++;
      }
    }
    int[] out = new int[n];
    int k = 0;
    for (int i = 0; i < MAX_LINES; i++) {
      if (!lines[i].isEmpty()) {
        out[k++] = i;
      }
    }
    return out;
  }

  // --- NBT ------------------------------------------------------------------------------------

  /** Whether a compound holds a sign at all (see the class's notes). */
  public static boolean isSign(NBTTagCompound compound) {
    if (compound.hasKey(KEY_PICTOGRAM)) {
      return true;
    }
    for (String k : NEW_KEYS) {
      if (compound.hasKey(k)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Reads a sign; call only when {@link #isSign} holds.
   *
   * @param compound       the compound
   * @param fallbackScheme the scheme when {@code s} is missing or not known
   * @return the sign, every value cleaned and clamped
   */
  public static WayfindingSign read(NBTTagCompound compound, WayfindingScheme fallbackScheme) {
    Builder b = builder();
    for (int i = 0; i < MAX_LINES; i++) {
      b.lines[i] = compound.getString(KEY_LINE[i]);
      b.lineScales[i] = readFloat(compound, KEY_LINE_SCALE[i], 1f);
      b.iconsLeft[i] = WayfindingIcon.byId(compound.getString(KEY_ICON_LEFT[i]));
      b.iconsRight[i] = WayfindingIcon.byId(compound.getString(KEY_ICON_RIGHT[i]));
    }
    b.textScale = readFloat(compound, KEY_TEXT_SCALE, 1f);
    b.pictogram = WayfindingIcon.byId(compound.getString(KEY_PICTOGRAM));
    b.arrow = WayfindingArrow.byId(compound.getString(KEY_ARROW));
    b.scheme = WayfindingScheme.byId(compound.getString(KEY_SCHEME), fallbackScheme);
    b.doubleSided = !compound.hasKey(KEY_DOUBLE) || compound.getBoolean(KEY_DOUBLE);
    b.lineCount = compound.getInteger(KEY_LINE_COUNT);
    b.align = Align.byId(compound.getString(KEY_ALIGN));
    b.iconStyle = IconStyle.byId(compound.getString(KEY_ICON_STYLE));
    b.mount = WayfindingMount.byId(compound.getString(KEY_MOUNT));
    b.drop = compound.getInteger(KEY_DROP);
    b.panelScale = readFloat(compound, KEY_PANEL_SCALE, 1f);
    b.background = readColour(compound, KEY_BACKGROUND);
    b.legend = readColour(compound, KEY_LEGEND);
    b.border = compound.getInteger(KEY_BORDER);
    b.lit = !compound.hasKey(KEY_LIT) || compound.getBoolean(KEY_LIT);
    return b.build();
  }

  /**
   * Writes this sign (see the class's notes for what is left out).
   *
   * @param compound the compound to write into
   * @return the compound
   */
  public NBTTagCompound write(NBTTagCompound compound) {
    for (int i = 0; i < 2; i++) {
      if (!lines[i].isEmpty()) {
        compound.setString(KEY_LINE[i], lines[i]);
      }
    }
    compound.setString(KEY_PICTOGRAM, pictogram.getId());
    compound.setString(KEY_ARROW, arrow.getId());
    compound.setString(KEY_SCHEME, scheme.getId());
    compound.setBoolean(KEY_DOUBLE, doubleSided);
    writeFloat(compound, KEY_TEXT_SCALE, textScale, 1f);
    writeFloat(compound, KEY_LINE_SCALE[0], lineScales[0], 1f);
    writeFloat(compound, KEY_LINE_SCALE[1], lineScales[1], 1f);
    for (int i = 2; i < MAX_LINES; i++) {
      if (!lines[i].isEmpty()) {
        compound.setString(KEY_LINE[i], lines[i]);
      }
      writeFloat(compound, KEY_LINE_SCALE[i], lineScales[i], 1f);
    }
    for (int i = 0; i < MAX_LINES; i++) {
      if (iconsLeft[i] != WayfindingIcon.NONE) {
        compound.setString(KEY_ICON_LEFT[i], iconsLeft[i].getId());
      }
      if (iconsRight[i] != WayfindingIcon.NONE) {
        compound.setString(KEY_ICON_RIGHT[i], iconsRight[i].getId());
      }
    }
    if (lineCount != 0) {
      compound.setByte(KEY_LINE_COUNT, (byte) lineCount);
    }
    if (align != Align.LEFT) {
      compound.setString(KEY_ALIGN, align.getId());
    }
    if (iconStyle != IconStyle.TILE) {
      compound.setString(KEY_ICON_STYLE, iconStyle.getId());
    }
    if (mount != WayfindingMount.AUTO) {
      compound.setString(KEY_MOUNT, mount.getId());
    }
    if (drop != 0) {
      compound.setByte(KEY_DROP, (byte) drop);
    }
    writeFloat(compound, KEY_PANEL_SCALE, panelScale, 1f);
    if (background != NO_COLOUR) {
      compound.setString(KEY_BACKGROUND, toHex(background));
    }
    if (legend != NO_COLOUR) {
      compound.setString(KEY_LEGEND, toHex(legend));
    }
    if (border != 0) {
      compound.setByte(KEY_BORDER, (byte) border);
    }
    if (!lit) {
      compound.setBoolean(KEY_LIT, false);
    }
    return compound;
  }

  /** A float key, any number type (a {@code /blockdata} may write a double or an int). */
  private static float readFloat(NBTTagCompound compound, String key, float absent) {
    return compound.hasKey(key, 99) ? compound.getFloat(key) : absent;
  }

  private static void writeFloat(NBTTagCompound compound, String key, float value, float absent) {
    if (value != absent) {
      compound.setFloat(key, value);
    }
  }

  /** A colour key: a hex string ({@code #RRGGBB}, {@code RRGGBB} or {@code 0xRRGGBB}) or a number. */
  private static int readColour(NBTTagCompound compound, String key) {
    if (compound.hasKey(key, 99)) {
      int v = compound.getInteger(key);
      return v < 0 ? NO_COLOUR : v & 0xFFFFFF;
    }
    return parseColour(compound.getString(key));
  }

  /**
   * A colour as typed: {@code #RRGGBB}, {@code RRGGBB} or {@code 0xRRGGBB}, any case; the
   * three-digit {@code #RGB} too.
   *
   * @param text the text, may be null
   * @return the colour, or {@link #NO_COLOUR} for anything else (an empty field included)
   */
  public static int parseColour(String text) {
    if (text == null) {
      return NO_COLOUR;
    }
    String t = text.trim();
    if (t.startsWith("#")) {
      t = t.substring(1);
    } else if (t.startsWith("0x") || t.startsWith("0X")) {
      t = t.substring(2);
    }
    if (t.length() == 3) {
      t = new String(new char[]{t.charAt(0), t.charAt(0), t.charAt(1), t.charAt(1), t.charAt(2),
          t.charAt(2)});
    }
    if (t.length() != 6) {
      return NO_COLOUR;
    }
    try {
      return Integer.parseInt(t, 16);
    } catch (NumberFormatException e) {
      return NO_COLOUR;
    }
  }

  /** A colour as {@code #RRGGBB}, upper case. */
  public static String toHex(int rgb) {
    return String.format("#%06X", rgb & 0xFFFFFF);
  }

  // --- the rules every value is cleaned by ----------------------------------------------------

  /**
   * What a line may hold: printable ASCII (the font has nothing else), at most
   * {@link #MAX_LINE_LENGTH} characters, trimmed.
   *
   * @param text the text, may be null
   * @return the cleaned text, never null
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

  /**
   * What a text scale may be: {@link #MIN_TEXT_SCALE} to {@link #MAX_TEXT_SCALE}, 1 (none) for a
   * value that is not a number.
   */
  public static float clampTextScale(float scale) {
    if (Float.isNaN(scale)) {
      return 1f;
    }
    return Math.max(MIN_TEXT_SCALE, Math.min(MAX_TEXT_SCALE, scale));
  }

  /** The scale a line is drawn at: the legend's times the line's own, clamped again. */
  public static float lineTextScale(float textScale, float lineScale) {
    return clampTextScale(textScale * lineScale);
  }

  /** What a panel scale may be: {@link #MIN_PANEL_SCALE} to {@link #MAX_PANEL_SCALE}. */
  public static float clampPanelScale(float scale) {
    if (Float.isNaN(scale)) {
      return 1f;
    }
    return Math.max(MIN_PANEL_SCALE, Math.min(MAX_PANEL_SCALE, scale));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof WayfindingSign)) {
      return false;
    }
    WayfindingSign s = (WayfindingSign) o;
    return hash == s.hash && textScale == s.textScale && pictogram == s.pictogram
        && arrow == s.arrow && scheme == s.scheme && doubleSided == s.doubleSided
        && lineCount == s.lineCount && align == s.align && iconStyle == s.iconStyle
        && mount == s.mount && drop == s.drop && panelScale == s.panelScale
        && background == s.background && legend == s.legend && border == s.border
        && lit == s.lit && Arrays.equals(lines, s.lines)
        && Arrays.equals(lineScales, s.lineScales) && Arrays.equals(iconsLeft, s.iconsLeft)
        && Arrays.equals(iconsRight, s.iconsRight);
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return write(new NBTTagCompound()).toString();
  }

  /** Builds a sign; every value is cleaned and clamped by {@link #build()}. */
  public static final class Builder {

    private final String[] lines = {"", "", "", ""};
    private final float[] lineScales = {1f, 1f, 1f, 1f};
    private final WayfindingIcon[] iconsLeft = new WayfindingIcon[MAX_LINES];
    private final WayfindingIcon[] iconsRight = new WayfindingIcon[MAX_LINES];
    private float textScale = 1f;
    private WayfindingIcon pictogram = WayfindingIcon.NONE;
    private WayfindingArrow arrow = WayfindingArrow.NONE;
    private WayfindingScheme scheme = WayfindingScheme.CORPORATE;
    private boolean doubleSided = true;
    private int lineCount;
    private Align align = Align.LEFT;
    private IconStyle iconStyle = IconStyle.TILE;
    private WayfindingMount mount = WayfindingMount.AUTO;
    private int drop;
    private float panelScale = 1f;
    private int background = NO_COLOUR;
    private int legend = NO_COLOUR;
    private int border;
    private boolean lit = true;

    private Builder() {
    }

    public Builder line(int i, String text) {
      lines[i] = text == null ? "" : text;
      return this;
    }

    public Builder lineScale(int i, float scale) {
      lineScales[i] = scale;
      return this;
    }

    public Builder iconLeft(int i, WayfindingIcon icon) {
      iconsLeft[i] = icon;
      return this;
    }

    public Builder iconRight(int i, WayfindingIcon icon) {
      iconsRight[i] = icon;
      return this;
    }

    public Builder textScale(float scale) {
      textScale = scale;
      return this;
    }

    public Builder pictogram(WayfindingIcon icon) {
      pictogram = icon;
      return this;
    }

    public Builder arrow(WayfindingArrow a) {
      arrow = a;
      return this;
    }

    public Builder scheme(WayfindingScheme s) {
      scheme = s;
      return this;
    }

    public Builder doubleSided(boolean d) {
      doubleSided = d;
      return this;
    }

    public Builder lineCount(int n) {
      lineCount = n;
      return this;
    }

    public Builder align(Align a) {
      align = a;
      return this;
    }

    public Builder iconStyle(IconStyle s) {
      iconStyle = s;
      return this;
    }

    public Builder mount(WayfindingMount m) {
      mount = m;
      return this;
    }

    public Builder drop(int sixteenths) {
      drop = sixteenths;
      return this;
    }

    public Builder panelScale(float scale) {
      panelScale = scale;
      return this;
    }

    public Builder background(int rgb) {
      background = rgb;
      return this;
    }

    public Builder legend(int rgb) {
      legend = rgb;
      return this;
    }

    public Builder border(int b) {
      border = b;
      return this;
    }

    public Builder lit(boolean l) {
      lit = l;
      return this;
    }

    public WayfindingSign build() {
      return new WayfindingSign(this);
    }
  }
}
