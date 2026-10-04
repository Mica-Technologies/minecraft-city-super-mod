package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

public enum PostType {
  LEFT("Left"),
  RIGHT("Right"),
  CENTER("Center"),
  OVERHEAD("Overhead"),
  RURAL("Rural"),
  /**
   * Hung on an overhead sign truss behind the sign: hanger brackets on the sign's back, up to the
   * truss's top. Last, since a sign's post type is saved by its ordinal.
   */
  TRUSS("Truss");

  private final String friendlyName;

  PostType(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public PostType next() {
    PostType[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static PostType fromOrdinal(int value) {
    PostType[] vals = values();
    if (value < 0 || value >= vals.length) {
      return OVERHEAD;
    }
    return vals[value];
  }
}
