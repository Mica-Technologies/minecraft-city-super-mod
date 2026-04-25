package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

public enum GuideSignColor {
  GREEN(0.043f, 0.322f, 0.176f, "Green"),
  BLUE(0.082f, 0.204f, 0.467f, "Blue"),
  BROWN(0.361f, 0.251f, 0.200f, "Brown"),
  YELLOW(0.957f, 0.667f, 0.0f, "Yellow"),
  WHITE(0.922f, 0.922f, 0.902f, "White"),
  BLACK(0.094f, 0.094f, 0.094f, "Black"),
  PURPLE(0.35f, 0.15f, 0.45f, "Purple");

  private final float red;
  private final float green;
  private final float blue;
  private final String friendlyName;

  GuideSignColor(float red, float green, float blue, String friendlyName) {
    this.red = red;
    this.green = green;
    this.blue = blue;
    this.friendlyName = friendlyName;
  }

  public float getRed() {
    return red;
  }

  public float getGreen() {
    return green;
  }

  public float getBlue() {
    return blue;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public int toNBT() {
    return ordinal();
  }

  public GuideSignColor next() {
    GuideSignColor[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static GuideSignColor fromNBT(int value) {
    GuideSignColor[] vals = values();
    if (value < 0 || value >= vals.length) {
      return GREEN;
    }
    return vals[value];
  }
}
