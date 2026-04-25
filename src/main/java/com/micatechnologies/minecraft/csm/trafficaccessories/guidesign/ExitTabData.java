package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

public class ExitTabData {

  public static final int POS_LEFT = 0;
  public static final int POS_CENTER = 1;
  public static final int POS_RIGHT = 2;
  private static final String[] POS_NAMES = {"Left", "Center", "Right"};

  private int position = POS_RIGHT;
  private String text = "EXIT";
  private int color = GuideSignColor.GREEN.ordinal();
  private boolean toll = false;

  public ExitTabData() {
  }

  public ExitTabData(int position, String text, int color, boolean toll) {
    this.position = position;
    this.text = text;
    this.color = color;
    this.toll = toll;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = Math.max(0, Math.min(2, position));
  }

  public void cyclePosition() {
    this.position = (position + 1) % 3;
  }

  public String getPositionName() {
    return POS_NAMES[Math.max(0, Math.min(2, position))];
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text != null ? text : "";
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }

  public GuideSignColor getGuideSignColor() {
    return GuideSignColor.fromNBT(color);
  }

  public void cycleColor() {
    this.color = GuideSignColor.fromNBT(color).next().ordinal();
  }

  public boolean isToll() {
    return toll;
  }

  public void setToll(boolean toll) {
    this.toll = toll;
  }

  public ExitTabData copy() {
    return new ExitTabData(position, text, color, toll);
  }
}
