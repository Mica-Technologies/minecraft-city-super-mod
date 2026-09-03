package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

public enum GuideSignArrowType {
  UP(0, 0, "Up"),
  DOWN(1, 0, "Down"),
  LEFT(2, 0, "Left"),
  RIGHT(3, 0, "Right"),
  UP_LEFT(4, 0, "Up-Left"),
  UP_RIGHT(0, 1, "Up-Right"),
  DOWN_LEFT(1, 1, "Down-Left"),
  DOWN_RIGHT(2, 1, "Down-Right"),
  UP_LEFT_RIGHT(3, 1, "Up-Left-Right"),
  LEFT_RIGHT(4, 1, "Left-Right");

  private final int atlasCol;
  private final int atlasRow;
  private final String friendlyName;

  GuideSignArrowType(int atlasCol, int atlasRow, String friendlyName) {
    this.atlasCol = atlasCol;
    this.atlasRow = atlasRow;
    this.friendlyName = friendlyName;
  }

  public int getAtlasCol() {
    return atlasCol;
  }

  public int getAtlasRow() {
    return atlasRow;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public GuideSignArrowType next() {
    GuideSignArrowType[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public GuideSignArrowType prev() {
    GuideSignArrowType[] vals = values();
    return vals[(ordinal() - 1 + vals.length) % vals.length];
  }

  public static GuideSignArrowType fromOrdinal(int value) {
    GuideSignArrowType[] vals = values();
    if (value < 0 || value >= vals.length) {
      return UP;
    }
    return vals[value];
  }
}
