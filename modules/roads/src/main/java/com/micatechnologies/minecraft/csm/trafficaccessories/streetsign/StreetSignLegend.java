package com.micatechnologies.minecraft.csm.trafficaccessories.streetsign;

import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignArrowType;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;

/**
 * What is lettered on ONE blade: the cardinal prefix, the street name, the street-type suffix,
 * the city line, and the three side slots (block number, emblem, arrow). Everything that is the
 * same across a whole assembly -- colour, border, corners, mount, frame, lighting, text size,
 * affix alignment, size floors -- lives on {@link StreetSignData} instead.
 *
 * <p>{@link StreetSignData} <b>extends</b> this class, and that is what keeps old saves loading
 * unchanged: Gson writes a superclass's fields at the top level of the document, under the same
 * names they had when they were declared directly on {@code StreetSignData}. The top-level legend
 * is therefore the upper (or only) blade, exactly as it always was, and the optional second blade
 * is a nested instance of this class.
 *
 * <p>Fields absent from older JSON fall to the Java defaults below, which are the upper blade's
 * historical defaults. Never rename a field: its name is the serialized key.
 *
 * @since 2026.9.17
 */
public class StreetSignLegend {

  public static final int MAX_NAME_LENGTH = 28;
  public static final int MAX_AFFIX_LENGTH = 6;
  public static final int MAX_CITY_LENGTH = 20;
  public static final int MAX_BLOCK_LENGTH = 6;
  public static final int MAX_ROUTE_LENGTH = 4;

  // --- Legend ------------------------------------------------------------------------
  /** Cardinal prefix drawn small and raised ahead of the name, e.g. "W". */
  /** How many steps there are round the compass, and how many degrees each one is. */
  public static final int TURNS = 8;
  public static final int DEGREES_PER_TURN = 360 / TURNS;

  /**
   * Which way this blade points, in eighths of a turn from the way the block faces: 0 is
   * along the block's facing, 2 a quarter turn, and the odd values the diagonals.
   *
   * <p>Only a post-top blade uses it. A hanging or flat blade is one panel on one piece of
   * hardware and has nothing to turn about; a post-top pair is two blades bolted round a
   * post, and which way each of them points is the whole point of the mount. Each blade
   * carries its own, so a pair can cross at any of the eight angles rather than only at a
   * right angle -- a fork or a skewed junction is signed the way it really runs.</p>
   *
   * <p>The post does not turn with them: it is the block's own model, and it is what the
   * blades are anchored to.</p>
   *
   * <p>Boxed and left null for a blade pointing straight ahead, so that Gson omits it and
   * a document that never turned a blade -- every hanging blade there has ever been --
   * writes back exactly the keys it was saved with. {@code StreetSignDataTest} holds that
   * invariant, and it is the same trick {@code lowerBlade} uses for the second blade.</p>
   */
  private Integer bladeTurn;

  private String prefix = "";
  private String streetName = "MAIN";
  /** Street type drawn small and raised after the name, e.g. "ST", "BLVD". */
  private String suffix = "ST";
  /** Optional small line under the name -- a city, district, or agency. */
  private String cityText = "";

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

  public StreetSignLegend() {
  }

  // ----------------------------------------------------------------------- legend ----

  /** How many eighths of a turn from the block's facing this blade points, 0 to 7. */
  public int getBladeTurn() {
    return bladeTurn == null ? 0 : ((bladeTurn % TURNS) + TURNS) % TURNS;
  }

  public void setBladeTurn(int turn) {
    int wrapped = ((turn % TURNS) + TURNS) % TURNS;
    this.bladeTurn = wrapped == 0 ? null : wrapped;
  }

  /** Steps the blade round one eighth of a turn, wrapping. */
  public void cycleBladeTurn(int by) {
    setBladeTurn(getBladeTurn() + by);
  }

  /** This blade's angle as the editor shows it. */
  public String getBladeTurnName() {
    return getBladeTurn() == 0 ? "Ahead" : (getBladeTurn() * DEGREES_PER_TURN) + "\u00b0";
  }

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

  // ------------------------------------------------------------------- validation ----

  static String clamp(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    return value.length() > maxLength ? value.substring(0, maxLength) : value;
  }

  /**
   * Repairs what Gson can leave behind, since it writes fields directly and never runs the
   * setters: a null string becomes empty and an over-long one is cut to its cap. The document
   * arrives from a client, so the caps have to hold on the way in, not only when the GUI types.
   */
  void sanitizeLegend() {
    prefix = clamp(prefix, MAX_AFFIX_LENGTH);
    streetName = clamp(streetName, MAX_NAME_LENGTH);
    suffix = clamp(suffix, MAX_AFFIX_LENGTH);
    cityText = clamp(cityText, MAX_CITY_LENGTH);
    blockNumber = clamp(blockNumber, MAX_BLOCK_LENGTH);
    shieldRoute = clamp(shieldRoute, MAX_ROUTE_LENGTH);
  }
}
