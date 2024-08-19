package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

public enum DirectionSixteen implements IStringSerializable {
  S(0, "s", 180.0F),
  W(1, "w", 90.0F),
  N(2, "n", 0.0F),
  E(3, "e", 270.0F),
  SE(4, "se", 225.0F),
  SW(5, "sw", 135.0F),
  NW(6, "nw", 45.0F),
  NE(7, "ne", 315.0F),
  SSW(8, "ssw", 157.5F),
  WSW(9, "wsw", 112.5F),
  NNW(10, "nnw", 22.5F),
  NNE(11, "nne", 337.5F),
  ENE(12, "ene", 292.5F),
  ESE(13, "ese", 247.5F),
  SSE(14, "sse", 202.5F),
  WNW(15, "wnw", 67.5F);

  private final int index;
  private final String name;
  private final float rotation;

  DirectionSixteen(int index, String name, float rotation) {
    this.index = index;
    this.name = name;
    this.rotation = rotation;
  }

  public int getIndex() {
    return index;
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  public float getRotation() {
    return rotation;
  }

  @Override
  public String toString() {
    return name;
  }

  public static DirectionSixteen fromName(String name) {
    for (DirectionSixteen directionSixteen : DirectionSixteen.values()) {
      if (directionSixteen.getName().equals(name)) {
        return directionSixteen;
      }
    }
    return null;
  }

  public static DirectionSixteen fromIndex(int index) {
    for (DirectionSixteen directionSixteen : DirectionSixteen.values()) {
      if (directionSixteen.getIndex() == index) {
        return directionSixteen;
      }
    }
    return null;
  }
}