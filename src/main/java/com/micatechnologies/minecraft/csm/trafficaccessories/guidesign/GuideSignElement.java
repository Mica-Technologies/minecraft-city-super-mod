package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

public class GuideSignElement {

  public static final int TYPE_TEXT = 0;
  public static final int TYPE_SHIELD = 1;
  public static final int TYPE_ARROW = 2;
  public static final int TYPE_DIVIDER = 3;
  public static final int TYPE_SPACING = 4;
  public static final int TYPE_COUNT = 5;
  private static final String[] TYPE_NAMES = {"Text", "Shield", "Arrow", "Divider", "Spacing"};

  private int type = TYPE_TEXT;

  private String text = "";
  private float textScale = 1.0f;

  private int shieldType = 0;
  private String routeNumber = "";
  private int bannerType = 0;

  private int arrowType = 0;

  private int spacingWidth = 4;

  public GuideSignElement() {
  }

  public static GuideSignElement createText(String text, float scale) {
    GuideSignElement e = new GuideSignElement();
    e.type = TYPE_TEXT;
    e.text = text;
    e.textScale = scale;
    return e;
  }

  public static GuideSignElement createShield(GuideSignShieldType shield, String routeNumber,
      GuideSignBannerType banner) {
    GuideSignElement e = new GuideSignElement();
    e.type = TYPE_SHIELD;
    e.shieldType = shield.ordinal();
    e.routeNumber = routeNumber;
    e.bannerType = banner.ordinal();
    return e;
  }

  public static GuideSignElement createArrow(GuideSignArrowType arrow) {
    GuideSignElement e = new GuideSignElement();
    e.type = TYPE_ARROW;
    e.arrowType = arrow.ordinal();
    return e;
  }

  public static GuideSignElement createDivider() {
    GuideSignElement e = new GuideSignElement();
    e.type = TYPE_DIVIDER;
    return e;
  }

  public static GuideSignElement createSpacing(int width) {
    GuideSignElement e = new GuideSignElement();
    e.type = TYPE_SPACING;
    e.spacingWidth = width;
    return e;
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = Math.max(0, Math.min(TYPE_COUNT - 1, type));
  }

  public static String getTypeName(int type) {
    if (type < 0 || type >= TYPE_NAMES.length) return "Unknown";
    return TYPE_NAMES[type];
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text != null ? text : "";
  }

  public float getTextScale() {
    return textScale;
  }

  public void setTextScale(float textScale) {
    this.textScale = Math.max(0.5f, Math.min(2.0f, textScale));
  }

  public int getShieldType() {
    return shieldType;
  }

  public void setShieldType(int shieldType) {
    this.shieldType = shieldType;
  }

  public GuideSignShieldType getGuideSignShieldType() {
    return GuideSignShieldType.fromOrdinal(shieldType);
  }

  public String getRouteNumber() {
    return routeNumber;
  }

  public void setRouteNumber(String routeNumber) {
    this.routeNumber = routeNumber != null ? routeNumber : "";
  }

  public int getBannerType() {
    return bannerType;
  }

  public void setBannerType(int bannerType) {
    this.bannerType = bannerType;
  }

  public GuideSignBannerType getGuideSignBannerType() {
    return GuideSignBannerType.fromOrdinal(bannerType);
  }

  public int getArrowType() {
    return arrowType;
  }

  public void setArrowType(int arrowType) {
    this.arrowType = arrowType;
  }

  public GuideSignArrowType getGuideSignArrowType() {
    return GuideSignArrowType.fromOrdinal(arrowType);
  }

  public int getSpacingWidth() {
    return spacingWidth;
  }

  public void setSpacingWidth(int spacingWidth) {
    this.spacingWidth = Math.max(1, Math.min(32, spacingWidth));
  }

  public String getSummary() {
    switch (type) {
      case TYPE_TEXT:
        return "\"" + (text.length() > 12 ? text.substring(0, 12) + ".." : text) + "\"";
      case TYPE_SHIELD:
        return getGuideSignShieldType().getFriendlyName() + " " + routeNumber;
      case TYPE_ARROW:
        return getGuideSignArrowType().getFriendlyName() + " Arrow";
      case TYPE_DIVIDER:
        return "---";
      case TYPE_SPACING:
        return "Space(" + spacingWidth + ")";
      default:
        return "?";
    }
  }

  public GuideSignElement copy() {
    GuideSignElement e = new GuideSignElement();
    e.type = type;
    e.text = text;
    e.textScale = textScale;
    e.shieldType = shieldType;
    e.routeNumber = routeNumber;
    e.bannerType = bannerType;
    e.arrowType = arrowType;
    e.spacingWidth = spacingWidth;
    return e;
  }
}
