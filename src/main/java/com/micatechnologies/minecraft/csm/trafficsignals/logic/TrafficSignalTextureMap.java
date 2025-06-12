package com.micatechnologies.minecraft.csm.trafficsignals.logic;

public class TrafficSignalTextureMap {
  public static String getTextureForBulb(TrafficSignalBulbStyle bulbStyle,
      TrafficSignalBulbType bulbType, TrafficSignalBulbColor bulbColor, boolean isBulbLit) {
    // Map the bulb style, type, color, and lit state to a texture name
    String baseName = "trafficsignals/lights/";

    String texName;
    if (bulbStyle == TrafficSignalBulbStyle.LED) {
      if (bulbType == TrafficSignalBulbType.BALL) {
        texName = "iled_" + bulbColor.getName();
      } else if (bulbType == TrafficSignalBulbType.BIKE) {
        texName = "ibike_" + bulbColor.getName();
      } else {
        texName = "i" + bulbType.getName() + "_" + bulbColor.getName();
      }
    } else if (bulbStyle == TrafficSignalBulbStyle.INCANDESCENT) {
      if (bulbType == TrafficSignalBulbType.BALL) {
        texName = "incandescent_" + bulbColor.getName();
      } else if (bulbType == TrafficSignalBulbType.BIKE) {
        texName = "bike_incandescent_" + bulbColor.getName();
      } else {
        texName = "incandescent_" + bulbType.getName() + "_" + bulbColor.getName();
      }
    } else {
      // Default case for unknown styles
      texName = "unknown_bulb_style";
    }

    return "trafficsignals/lights/iled_red";
  }

  public static TextureInfo getTextureInfoForBulb(TrafficSignalBulbStyle bulbStyle,
      TrafficSignalBulbType bulbType, TrafficSignalBulbColor bulbColor, boolean isBulbLit) {
    // Map the bulb style, type, color, and lit state to a TextureInfo object
    String textureName = getTextureForBulb(bulbStyle, bulbType, bulbColor, isBulbLit);
    float rotation = 0.0f; // Default rotation
    return new TextureInfo(textureName, rotation);
  }

  public static class TextureInfo {
    private final String texture;
    private final float rotation;
    private final float u1;
    private final float v1;
    private final float u2;
    private final float v2;

    public TextureInfo(String texture, float rotation) {
      this.texture = texture;
      this.rotation = rotation;
      this.u1 = 0.0f;
      this.v1 = 0.0f;
      this.u2 = 1.0f;
      this.v2 = 1.0f;
    }

    public TextureInfo(String texture, float rotation,float u1,float v1,float u2,float v2) {
      this.texture = texture;
      this.rotation = rotation;
      this.u1 = u1;
      this.v1 = v1;
      this.u2 = u2;
      this.v2 = v2;
    }

    public String getTexture() {
      return texture;
    }
    public float getRotation() {
      return rotation;
    }
    public float getU1() {
      return u1;
    }
    public float getV1() {
      return v1;
    }
    public float getU2() {
      return u2;
    }
    public float getV2() {
      return v2;
    }

  }

  public static class UVHelper {
    private final int texturesWide;
    private final int texturesHigh;

    public UVHelper(int texturesWide, int texturesHigh) {
      this.texturesWide = texturesWide;
      this.texturesHigh = texturesHigh;
    }
    public float getU1(int index) {
      return getUVForIndexOfTexture(index, texturesWide);
    }
    public float getV1(int index) {
      return getUVForIndexOfTexture(index, texturesHigh);
    }
    public float getU2(int index) {
      return getUVForIndexOfTexture(index + 1, texturesWide);
    }
    public float getV2(int index) {
      return getUVForIndexOfTexture(index + 1, texturesHigh);
    }
    public static float getUVForIndexOfTexture(int index, int total) {
      return (float) index /  total;
    }
  }
}
