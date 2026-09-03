package com.micatechnologies.minecraft.csm.trafficaccessories.streetsign;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.CornerStyle;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignArrowType;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignColor;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
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
 */
public class StreetSignData {

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
  public static final int MAX_NAME_LENGTH = 28;
  public static final int MAX_AFFIX_LENGTH = 6;
  public static final int MAX_CITY_LENGTH = 20;
  public static final int MAX_BLOCK_LENGTH = 6;
  public static final int MAX_ROUTE_LENGTH = 4;

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

  // --- Legend ------------------------------------------------------------------------
  /** Cardinal prefix drawn small and raised ahead of the name, e.g. "W". */
  private String prefix = "";
  private String streetName = "MAIN";
  /** Street type drawn small and raised after the name, e.g. "ST", "BLVD". */
  private String suffix = "ST";
  /** Optional small line under the name -- a city, district, or agency. */
  private String cityText = "";
  private float textScale = 1.0f;
  /**
   * Where the prefix and suffix sit against the street name. TOP hangs them from the name's
   * cap line (the raised "W ... RD" look); BOTTOM drops them to its baseline, which is the
   * other common blade style; MIDDLE centers them on the name. Absent in older JSON -> 0
   * (TOP), so a sign written before this existed keeps the look it had.
   */
  private int affixVertical = StreetSignVerticalPos.TOP.ordinal();

  // --- Block number slot -------------------------------------------------------------
  private String blockNumber = "";
  private int blockPosition = StreetSignSlotPosition.NONE.ordinal();
  private int blockVertical = StreetSignVerticalPos.MIDDLE.ordinal();

  // --- Emblem slot (route shield or civic logo) --------------------------------------
  private int emblemKind = StreetSignEmblemKind.NONE.ordinal();
  private int emblemPosition = StreetSignSlotPosition.LEFT.ordinal();
  private int shieldType = GuideSignShieldType.INTERSTATE.ordinal();
  private String shieldRoute = "";
  private int logoType = StreetSignLogoType.SEAL_STAR.ordinal();

  // --- Arrow slot --------------------------------------------------------------------
  private int arrowPosition = StreetSignSlotPosition.NONE.ordinal();
  private int arrowType = GuideSignArrowType.RIGHT.ordinal();

  // --- Size floors -------------------------------------------------------------------
  /** Floor for the blade's width in sign pixels; content stays centered in any surplus. */
  private int minWidth = 16;
  /** Floor for the blade's height in sign pixels; content stays centered in any surplus. */
  private int minHeight = 8;

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

  public void cycleMountType() {
    this.mountType = getMountType().next().ordinal();
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

  // ----------------------------------------------------------------------- legend ----

  public String getPrefix() {
    return prefix == null ? "" : prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = clamp(prefix, MAX_AFFIX_LENGTH);
  }

  public String getStreetName() {
    return streetName == null ? "" : streetName;
  }

  public void setStreetName(String streetName) {
    this.streetName = clamp(streetName, MAX_NAME_LENGTH);
  }

  public String getSuffix() {
    return suffix == null ? "" : suffix;
  }

  public void setSuffix(String suffix) {
    this.suffix = clamp(suffix, MAX_AFFIX_LENGTH);
  }

  public String getCityText() {
    return cityText == null ? "" : cityText;
  }

  public void setCityText(String cityText) {
    this.cityText = clamp(cityText, MAX_CITY_LENGTH);
  }

  public boolean hasCityText() {
    return !getCityText().isEmpty();
  }

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

  // ----------------------------------------------------------------- block number ----

  public String getBlockNumber() {
    return blockNumber == null ? "" : blockNumber;
  }

  public void setBlockNumber(String blockNumber) {
    this.blockNumber = clamp(blockNumber, MAX_BLOCK_LENGTH);
  }

  public StreetSignSlotPosition getBlockPosition() {
    return StreetSignSlotPosition.fromOrdinal(blockPosition);
  }

  public void setBlockPosition(StreetSignSlotPosition position) {
    this.blockPosition = position.ordinal();
  }

  public void cycleBlockPosition() {
    this.blockPosition = getBlockPosition().next().ordinal();
  }

  public StreetSignVerticalPos getBlockVertical() {
    return StreetSignVerticalPos.fromOrdinal(blockVertical);
  }

  public void setBlockVertical(StreetSignVerticalPos vertical) {
    this.blockVertical = vertical.ordinal();
  }

  public void cycleBlockVertical() {
    this.blockVertical = getBlockVertical().next().ordinal();
  }

  /** Whether the block number slot occupies width: switched on AND actually carrying text. */
  public boolean hasBlockNumber() {
    return getBlockPosition().isShown() && !getBlockNumber().isEmpty();
  }

  // ----------------------------------------------------------------------- emblem ----

  public StreetSignEmblemKind getEmblemKind() {
    return StreetSignEmblemKind.fromOrdinal(emblemKind);
  }

  public void setEmblemKind(StreetSignEmblemKind kind) {
    this.emblemKind = kind.ordinal();
  }

  public void cycleEmblemKind() {
    this.emblemKind = getEmblemKind().next().ordinal();
  }

  public StreetSignSlotPosition getEmblemPosition() {
    return StreetSignSlotPosition.fromOrdinal(emblemPosition);
  }

  public void setEmblemPosition(StreetSignSlotPosition position) {
    this.emblemPosition = position.ordinal();
  }

  public void cycleEmblemPosition() {
    this.emblemPosition = getEmblemPosition().next().ordinal();
  }

  public GuideSignShieldType getShieldType() {
    return GuideSignShieldType.fromOrdinal(shieldType);
  }

  public void setShieldType(GuideSignShieldType type) {
    this.shieldType = type.ordinal();
  }

  public String getShieldRoute() {
    return shieldRoute == null ? "" : shieldRoute;
  }

  public void setShieldRoute(String shieldRoute) {
    this.shieldRoute = clamp(shieldRoute, MAX_ROUTE_LENGTH);
  }

  public StreetSignLogoType getLogoType() {
    return StreetSignLogoType.fromOrdinal(logoType);
  }

  public void setLogoType(StreetSignLogoType type) {
    this.logoType = type.ordinal();
  }

  /** Whether the emblem slot occupies width: a kind is chosen AND a side is chosen. */
  public boolean hasEmblem() {
    return getEmblemKind() != StreetSignEmblemKind.NONE && getEmblemPosition().isShown();
  }

  // ------------------------------------------------------------------------ arrow ----

  public StreetSignSlotPosition getArrowPosition() {
    return StreetSignSlotPosition.fromOrdinal(arrowPosition);
  }

  public void setArrowPosition(StreetSignSlotPosition position) {
    this.arrowPosition = position.ordinal();
  }

  public void cycleArrowPosition() {
    this.arrowPosition = getArrowPosition().next().ordinal();
  }

  public GuideSignArrowType getArrowType() {
    return GuideSignArrowType.fromOrdinal(arrowType);
  }

  public void setArrowType(GuideSignArrowType type) {
    this.arrowType = type.ordinal();
  }

  public boolean hasArrow() {
    return getArrowPosition().isShown();
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

  // ----------------------------------------------------------------- serialization ----

  private static String clamp(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    return value.length() > maxLength ? value.substring(0, maxLength) : value;
  }

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
      if (data.prefix == null) {
        data.prefix = "";
      }
      if (data.streetName == null) {
        data.streetName = "";
      }
      if (data.suffix == null) {
        data.suffix = "";
      }
      if (data.cityText == null) {
        data.cityText = "";
      }
      if (data.blockNumber == null) {
        data.blockNumber = "";
      }
      if (data.shieldRoute == null) {
        data.shieldRoute = "";
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
