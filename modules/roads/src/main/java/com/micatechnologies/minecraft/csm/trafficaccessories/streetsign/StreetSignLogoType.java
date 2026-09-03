package com.micatechnologies.minecraft.csm.trafficaccessories.streetsign;

/**
 * Civic logos a street blade can carry in its emblem slot -- the little city seal or agency
 * mark that sits at the leading end of a real intersection blade.
 *
 * <p>The artwork lives in the shared guide sign atlas
 * ({@code textures/blocks/trafficaccessories/guidesign/sign_atlas.png}), rows 11-12, drawn by
 * the dev-env-utils {@code GuideSignAtlasTool}. Unlike a route shield, each logo cell is
 * self-contained artwork with its own background plate, so it reads on any sign color and the
 * renderer draws no text over it.
 *
 * <p><b>Append-only: ordinals are serialized</b> in the sign's JSON -- never reorder or insert.
 */
public enum StreetSignLogoType {
  SEAL_STAR(0, 11, "Star Seal"),
  SEAL_LAUREL(1, 11, "Laurel Seal"),
  CIVIC_SHIELD(2, 11, "Civic Shield"),
  FLEUR_DE_LIS(3, 11, "Fleur-de-lis"),
  OAK_LEAF(4, 11, "Oak Leaf"),
  PINE_TREE(5, 11, "Pine Tree"),
  SKYLINE(6, 11, "City Skyline"),
  BRIDGE(7, 11, "Bridge"),

  MOUNTAIN(0, 12, "Mountain"),
  RIVER(1, 12, "River"),
  SUNRISE(2, 12, "Sunrise"),
  COMPASS(3, 12, "Compass Rose"),
  CAPITOL(4, 12, "Capitol Dome"),
  TRANSIT(5, 12, "Transit"),
  AIRPORT(6, 12, "Airport"),
  HOSPITAL(7, 12, "Hospital");

  private final int atlasCol;
  private final int atlasRow;
  private final String friendlyName;

  StreetSignLogoType(int atlasCol, int atlasRow, String friendlyName) {
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

  public StreetSignLogoType next() {
    StreetSignLogoType[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public StreetSignLogoType prev() {
    StreetSignLogoType[] vals = values();
    return vals[(ordinal() - 1 + vals.length) % vals.length];
  }

  public static StreetSignLogoType fromOrdinal(int value) {
    StreetSignLogoType[] vals = values();
    if (value < 0 || value >= vals.length) {
      return SEAL_STAR;
    }
    return vals[value];
  }
}
