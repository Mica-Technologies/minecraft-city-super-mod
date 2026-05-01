package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.IStringSerializable;

public enum SignShift implements IStringSerializable {
  NONE(0, "none"),
  SETBACK(1, "setback"),
  BACKTOBACK(2, "backtoback");

  private final int index;
  private final String name;

  SignShift(int index, String name) {
    this.index = index;
    this.name = name;
  }

  public int getIndex() {
    return index;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
