package com.micatechnologies.minecraft.csm.trafficaccessories.streetsign;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.CornerStyle;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignColor;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.SignLightMode;

/**
 * The whole configuration of one dynamic street sign, serialized to JSON with Gson and stored
 * in a single tile entity NBT string.
 *
 * <p>Unlike {@code GuideSignData} this is a <b>flat, fixed-slot</b> document, not a tree: a
 * street blade always has the same anatomy (an optional cardinal prefix, the street name, an
 * optional suffix, an optional city line, and up to three optional side slots), so there is
 * nothing for a polymorphic element list to buy. Everything here is plain data plus clamping
 * validation, with no Minecraft or rendering dependency, so it is usable on both sides.
 *
 * <p>Enum-valued fields are stored as ordinals and read back through each enum's clamping
 * {@code fromOrdinal}, so a hand-edited or truncated document can never produce an invalid
 * value. Fields absent from older JSON fall to their Java defaults, which are chosen so an
 * older sign keeps rendering exactly as it did.
 *
 * <p><b>The legend lives in the superclass.</b> What is lettered on a blade is a
 * {@link StreetSignLegend}, and this class extends it: the inherited fields are the upper (or
 * only) blade, serialized at the top level under the names they always had. An optional second
 * blade hangs below it as a nested {@link StreetSignLegend} in {@code lowerBlade}; everything else
 * here -- colour, border, corners, mount, frame, lighting, text size, affix alignment and the size
 * floors -- is shared by both blades, so a stacked pair cannot drift apart in style.
 */
public class StreetSignData extends StreetSignLegend {

  private static final int VERSION = 1;
  private static final Gson GSON = new GsonBuilder().create();

  /** 16 px = 1 block, so a blade may be forced out to 20 blocks wide and 4 blocks tall. */
  public static final int MAX_MIN_WIDTH = 320;
  public static final int MAX_MIN_HEIGHT = 64;
  public static final int MIN_MIN_WIDTH = 16;
  public static final int MIN_MIN_HEIGHT = 8;
  public static final int MAX_BORDER_WIDTH = 4;
  public static final float MIN_TEXT_SCALE = 0.5f;
  public static final float MAX_TEXT_SCALE = 3.0f;

  private int version = VERSION;

  // --- Panel style -------------------------------------------------------------------
  private int signColor = GuideSignColor.GREEN.ordinal();
  private int borderWidth = 1;
  private int cornerStyle = CornerStyle.ROUND.ordinal();
  private int mountType = StreetSignMount.HANGING.ordinal();
  /**
   * Draw the dark extruded aluminum frame around the panel edge -- the top and bottom rails
   * and end castings of an internally-illuminated blade. Absent in older JSON, so a sign
   * written before this existed keeps its plain painted edge.
   */
  private boolean extrudedFrame = false;
  /**
   * Render the legend on the blade's reverse as well. Only meaningful for a mount that leaves
   * the reverse exposed -- see {@link StreetSignMount#canBeDoubleSided()}.
   */
  private boolean doubleSided = true;

  // --- Lighting ----------------------------------------------------------------------
  /**
   * Internal illumination. There is no fixture hardware to choose: the face itself lights,
   * which is the only kind of lighting a street blade gets.
   */
  private boolean internalLight = false;
  private int lightMode = SignLightMode.NIGHT.ordinal();

  // --- Shared legend style -----------------------------------------------------------
  private float textScale = 1.0f;
  /**
   * Where the prefix and suffix sit against the street name. TOP hangs them from the name's
   * cap line (the raised "W ... RD" look); BOTTOM drops them to its baseline, which is the
   * other common blade style; MIDDLE centers them on the name. Absent in older JSON -> 0
   * (TOP), so a sign written before this existed keeps the look it had.
   */
  private int affixVertical = StreetSignVerticalPos.TOP.ordinal();

  // --- Size floors -------------------------------------------------------------------
  /** Floor for the blade's width in sign pixels; content stays centered in any surplus. */
  private int minWidth = 16;
  /** Floor for the blade's height in sign pixels; content stays centered in any surplus. */
  private int minHeight = 8;

  // --- Second blade ------------------------------------------------------------------
  /**
   * The legend of a second blade hung directly below the first, sharing its width, height and
   * style -- the two-name assembly used where a road changes name at the junction. Null means
   * a single blade, and Gson omits a null field, so a sign without one saves exactly the
   * document it always did and an older sign loads with none.
   */
  private StreetSignLegend lowerBlade = null;

  public StreetSignData() {
  }

  // ------------------------------------------------------------------ panel style ----

  public GuideSignColor getSignColor() {
    return GuideSignColor.fromNBT(signColor);
  }

  public void setSignColor(GuideSignColor color) {
    this.signColor = color.ordinal();
  }

  public void cycleSignColor() {
    this.signColor = getSignColor().next().ordinal();
  }

  public int getBorderWidth() {
    return borderWidth;
  }

  public void setBorderWidth(int borderWidth) {
    this.borderWidth = Math.max(0, Math.min(MAX_BORDER_WIDTH, borderWidth));
  }

  public CornerStyle getCornerStyle() {
    return CornerStyle.fromOrdinal(cornerStyle);
  }

  public void setCornerStyle(CornerStyle style) {
    this.cornerStyle = style.ordinal();
  }

  public void cycleCornerStyle() {
    this.cornerStyle = getCornerStyle().next().ordinal();
  }

  public StreetSignMount getMountType() {
    return StreetSignMount.fromOrdinal(mountType);
  }

  public void setMountType(StreetSignMount mount) {
    this.mountType = mount.ordinal();
  }

  /**
   * Steps to the next mount the editor offers, which is every mount except the post-top
   * brackets. Those are set by the street name blade blocks, which stand on a sign post;
   * this sign hangs from an arm or bolts flat and has nothing for that hardware to grip,
   * so offering it here would draw a bracket attached to nothing.
   */
  public void cycleMountType() {
    StreetSignMount next = getMountType().next();
    while (next.isPostTop()) {
      next = next.next();
    }
    this.mountType = next.ordinal();
  }

  public boolean hasExtrudedFrame() {
    return extrudedFrame;
  }

  public void setExtrudedFrame(boolean extrudedFrame) {
    this.extrudedFrame = extrudedFrame;
  }

  public void toggleExtrudedFrame() {
    this.extrudedFrame = !this.extrudedFrame;
  }

  /** Whether a back face should actually be drawn: asked for AND possible for this mount. */
  public boolean isDoubleSided() {
    return doubleSided && getMountType().canBeDoubleSided();
  }

  /** The raw toggle, so the GUI can show the player's choice even on a flat mount. */
  public boolean isDoubleSidedRequested() {
    return doubleSided;
  }

  public void setDoubleSided(boolean doubleSided) {
    this.doubleSided = doubleSided;
  }

  public void toggleDoubleSided() {
    this.doubleSided = !this.doubleSided;
  }

  // --------------------------------------------------------------------- lighting ----

  public boolean hasInternalLight() {
    return internalLight;
  }

  public void setInternalLight(boolean internalLight) {
    this.internalLight = internalLight;
  }

  public void toggleInternalLight() {
    this.internalLight = !this.internalLight;
    // Fitting lights to a sign should visibly do something, and switching them off should
    // park the mode somewhere sane to come back to -- same convention as the guide sign.
    if (!internalLight) {
      this.lightMode = SignLightMode.OFF.ordinal();
    } else if (getLightMode() == SignLightMode.OFF) {
      this.lightMode = SignLightMode.NIGHT.ordinal();
    }
  }

  public SignLightMode getLightMode() {
    return SignLightMode.fromOrdinal(lightMode);
  }

  public void setLightMode(SignLightMode mode) {
    this.lightMode = mode.ordinal();
  }

  public void cycleLightMode() {
    this.lightMode = getLightMode().next().ordinal();
  }

  // ---------------------------------------------------------- shared legend style ----

  public StreetSignVerticalPos getAffixVertical() {
    return StreetSignVerticalPos.fromOrdinal(affixVertical);
  }

  public void setAffixVertical(StreetSignVerticalPos vertical) {
    this.affixVertical = vertical.ordinal();
  }

  public void cycleAffixVertical() {
    this.affixVertical = getAffixVertical().next().ordinal();
  }

  public float getTextScale() {
    return Math.max(MIN_TEXT_SCALE, Math.min(MAX_TEXT_SCALE, textScale));
  }

  public void setTextScale(float textScale) {
    this.textScale = Math.max(MIN_TEXT_SCALE, Math.min(MAX_TEXT_SCALE, textScale));
  }

  // ------------------------------------------------------------------- size floors ----

  public int getMinWidth() {
    return Math.max(MIN_MIN_WIDTH, Math.min(MAX_MIN_WIDTH, minWidth));
  }

  public void setMinWidth(int minWidth) {
    this.minWidth = Math.max(MIN_MIN_WIDTH, Math.min(MAX_MIN_WIDTH, minWidth));
  }

  public int getMinHeight() {
    return Math.max(MIN_MIN_HEIGHT, Math.min(MAX_MIN_HEIGHT, minHeight));
  }

  public void setMinHeight(int minHeight) {
    this.minHeight = Math.max(MIN_MIN_HEIGHT, Math.min(MAX_MIN_HEIGHT, minHeight));
  }

  // ------------------------------------------------------------------ second blade ----

  /** Whether a second blade hangs below the first. */
  public boolean hasLowerBlade() {
    return lowerBlade != null;
  }

  /**
   * The second blade's legend.
   *
   * @return the lower blade, or null when this is a single blade
   */
  public StreetSignLegend getLowerBlade() {
    return lowerBlade;
  }

  /**
   * Adds, replaces or (with null) removes the second blade.
   *
   * @param legend the lower blade's legend, or null for a single blade
   */
  public void setLowerBlade(StreetSignLegend legend) {
    this.lowerBlade = legend;
  }

  /**
   * A fresh legend for a newly added second blade: a different street name than the first
   * blade's default, so switching the blade on visibly adds a second, distinct sign rather than
   * what looks like a duplicate.
   *
   * @return a new legend reading "ELM ST"
   */
  public static StreetSignLegend newLowerBlade() {
    StreetSignLegend legend = new StreetSignLegend();
    legend.setStreetName("ELM");
    legend.setSuffix("ST");
    return legend;
  }

  // ----------------------------------------------------------------- serialization ----

  public String toJson() {
    this.version = VERSION;
    return GSON.toJson(this);
  }

  /**
   * Parses a stored document. Defensive by design: a null, empty, malformed, or truncated
   * document yields a fresh default sign rather than throwing, because this runs inside the
   * renderer and on the network path where an exception would be a crash.
   */
  public static StreetSignData fromJson(String json) {
    if (json == null || json.isEmpty()) {
      return new StreetSignData();
    }
    try {
      StreetSignData data = GSON.fromJson(json, StreetSignData.class);
      if (data == null) {
        return new StreetSignData();
      }
      data.sanitizeLegend();
      if (data.lowerBlade != null) {
        data.lowerBlade.sanitizeLegend();
      }
      return data;
    } catch (Exception e) {
      return new StreetSignData();
    }
  }

  /** A deep copy, via a JSON round trip -- the document is small and this cannot alias. */
  public StreetSignData copy() {
    return fromJson(toJson());
  }
}
