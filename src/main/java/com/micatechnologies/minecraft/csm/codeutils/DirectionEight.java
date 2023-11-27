package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.IStringSerializable;

public enum DirectionEight implements IStringSerializable {
  S(0, "s"),
  W(1, "w"),
  N(2, "n"),
  E(3, "e"),
  SE(4, "se"),
  SW(5, "sw"),
  NW(6, "nw"),
  NE(7, "ne");

  private final int index;
  private final String name;

  DirectionEight(int index, String name) {
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


