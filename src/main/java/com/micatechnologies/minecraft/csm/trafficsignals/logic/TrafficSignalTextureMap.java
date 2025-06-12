package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

public class TrafficSignalTextureMap {

  private static final String TEXTURE_ATLAS_NAME = "textures/blocks/trafficsignals/lights/atlas.png";
  private static final UVHelper uvHelper = new UVHelper(8, 8);

  // Cache for TextureInfo
  private static final Map<TextureKey, TextureInfo> textureInfoCache = new ConcurrentHashMap<>();

  // Composite key for cache
  private static class TextureKey {
    private final TrafficSignalBulbStyle style;
    private final TrafficSignalBulbType type;
    private final TrafficSignalBulbColor color;
    private final boolean isLit;
    private final float rotation;

    TextureKey(TrafficSignalBulbStyle style, TrafficSignalBulbType type, TrafficSignalBulbColor color, boolean isLit, float rotation) {
      this.style = style;
      this.type = type;
      this.color = color;
      this.isLit = isLit;
      this.rotation = rotation;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TextureKey that = (TextureKey) o;
      return isLit == that.isLit &&
             Float.compare(that.rotation, rotation) == 0 &&
             style == that.style &&
             type == that.type &&
             color == that.color;
    }

    @Override
    public int hashCode() {
      return Objects.hash(style, type, color, isLit, rotation);
    }
  }

  // Rotation mapping for arrow types (all use left-facing base)
  private static float getRotationForType(TrafficSignalBulbType type) {
    switch (type) {
      case LEFT: return 0f;
      case UP_LEFT: return 315f;
      case UP: return 270f;
      case UP_RIGHT: return 225f;
      case RIGHT: return 180f;
      case TRANSIT_LEFT: return 45f;
      case TRANSIT_RIGHT: return -45f;
      default: return 0f;
    }
  }

  // Fallback logic for style/type combos
  private static TrafficSignalBulbStyle getEffectiveStyle(TrafficSignalBulbStyle style, TrafficSignalBulbType type) {
    switch (type) {
      case BIKE:
      case TRANSIT:
      case TRANSIT_LEFT:
      case TRANSIT_RIGHT:
        return TrafficSignalBulbStyle.LED;
      default:
        return style;
    }
  }

  // Atlas position mapping helper: returns int[]{row, col}
  private static int[] getAtlasRowCol(TrafficSignalBulbStyle style, TrafficSignalBulbType type, TrafficSignalBulbColor color, boolean isLit) {
    int idx = getFlatAtlasIndex(style, type, color, isLit);
    int row = idx / 8;
    int col = idx % 8;
    return new int[]{row, col};
  }

  // Flat index mapping (same as before)
  private static int getFlatAtlasIndex(TrafficSignalBulbStyle style, TrafficSignalBulbType type, TrafficSignalBulbColor color, boolean isLit) {
    int colorIdx = color.ordinal();
    switch (type) {
      case BIKE:
        return colorIdx * 2 + (isLit ? 1 : 0);
      case BALL:
        if (style == TrafficSignalBulbStyle.LED_DOTTED) {
          if (!isLit) return 6;
          return 7 + colorIdx;
        } else if (style == TrafficSignalBulbStyle.LED) {
          if (isLit) return 31 + colorIdx;
          if (color == TrafficSignalBulbColor.RED) return 34;
          if (color == TrafficSignalBulbColor.GREEN) return 35;
          return 36;
        } else if (style == TrafficSignalBulbStyle.INCANDESCENT) {
          if (isLit) return 37 + colorIdx;
          return 40 + colorIdx;
        }
        break;
      case UTURN:
        if (isLit) return 19 + colorIdx;
        return 22 + colorIdx;
      case TRANSIT:
      case TRANSIT_LEFT:
      case TRANSIT_RIGHT:
        if (isLit) return 25 + colorIdx * 2;
        return 26 + colorIdx * 2;
      case LEFT:
      case UP_LEFT:
      case UP:
      case UP_RIGHT:
      case RIGHT:
        if (style == TrafficSignalBulbStyle.LED_DOTTED) {
          if (!isLit) return 10;
          return 11 + colorIdx;
        } else if (style == TrafficSignalBulbStyle.LED) {
          if (isLit) return 14 + colorIdx;
          return 17 + colorIdx;
        } else if (style == TrafficSignalBulbStyle.INCANDESCENT) {
          if (isLit) return 43 + colorIdx;
          return 46 + colorIdx;
        }
        break;
    }
    // Default/fallback
    return 0;
  }

  public static TextureInfo getTextureInfoForBulb(TrafficSignalBulbStyle bulbStyle,
      TrafficSignalBulbType bulbType, TrafficSignalBulbColor bulbColor, boolean isBulbLit) {

    TrafficSignalBulbStyle effectiveStyle = getEffectiveStyle(bulbStyle, bulbType);
    float rotation = getRotationForType(bulbType);

    // UTURN and TRANSIT do not rotate
    if (bulbType == TrafficSignalBulbType.UTURN || bulbType == TrafficSignalBulbType.TRANSIT) {
      rotation = 0f;
    }

    // Compose cache key
    TextureKey key = new TextureKey(effectiveStyle, bulbType, bulbColor, isBulbLit, rotation);

    // Check cache
    TextureInfo cached = textureInfoCache.get(key);
    if (cached != null) return cached;

    int[] rowCol = getAtlasRowCol(effectiveStyle, bulbType, bulbColor, isBulbLit);
    int row = rowCol[0];
    int col = rowCol[1];

    float u1 = uvHelper.getU1(col);
    float v1 = uvHelper.getV1(row);
    float u2 = uvHelper.getU2(col);
    float v2 = uvHelper.getV2(row);

    TextureInfo info = new TextureInfo(TEXTURE_ATLAS_NAME, rotation, u1, v1, u2, v2);
    textureInfoCache.put(key, info);
    return info;
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
    // col: 0-based column, row: 0-based row
    public float getU1(int col) {
      return (float) col / texturesWide;
    }
    public float getV1(int row) {
      return (float) row / texturesHigh;
    }
    public float getU2(int col) {
      return (float) (col + 1) / texturesWide;
    }
    public float getV2(int row) {
      return (float) (row + 1) / texturesHigh;
    }
  }
}