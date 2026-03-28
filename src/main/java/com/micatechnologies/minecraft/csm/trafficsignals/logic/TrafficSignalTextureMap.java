package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

public class TrafficSignalTextureMap {

  private static final String TEXTURE_ATLAS_NAME =
      "textures/blocks/trafficsignals/lights/atlas.png";
  private static final UVHelper uvHelper = new UVHelper(8, 8);

  private static final Map<TextureKey, TextureInfo> textureInfoCache = new ConcurrentHashMap<>();

  private static class TextureKey {

    private final TrafficSignalBulbStyle style;
    private final TrafficSignalBulbType type;
    private final TrafficSignalBulbColor color;
    private final boolean isLit;
    private final float rotation;

    TextureKey(TrafficSignalBulbStyle style, TrafficSignalBulbType type,
        TrafficSignalBulbColor color, boolean isLit, float rotation) {
      this.style = style;
      this.type = type;
      this.color = color;
      this.isLit = isLit;
      this.rotation = rotation;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TextureKey that = (TextureKey) o;
      return isLit == that.isLit && Float.compare(that.rotation, rotation) == 0
          && style == that.style && type == that.type && color == that.color;
    }

    @Override
    public int hashCode() {
      return Objects.hash(style, type, color, isLit, rotation);
    }
  }

  private static float getRotationForType(TrafficSignalBulbType type) {
    switch (type) {
      case LEFT:
        return 0f;
      case UP_LEFT:
        return 45f;
      case UP:
        return 90f;
      case UP_RIGHT:
        return 135f;
      case RIGHT:
        return 180f;
      case TRANSIT_LEFT:
        return -45f;
      case TRANSIT_RIGHT:
        return 45f;
      case UTURN:
      case TRANSIT:
      case BALL:
        return 0f;
      default:
        return 0f;
    }
  }

  private static TrafficSignalBulbStyle getEffectiveStyle(TrafficSignalBulbStyle style,
      TrafficSignalBulbType type) {
    switch (type) {
      case BIKE:
      case UTURN:
      case TRANSIT:
      case TRANSIT_LEFT:
      case TRANSIT_RIGHT:
        return TrafficSignalBulbStyle.LED; // Force LED
      default:
        return style;
    }
  }

  private static int[] getAtlasRowCol(TrafficSignalBulbStyle style, TrafficSignalBulbType type,
      TrafficSignalBulbColor color, boolean isLit) {
    int idx = getFlatAtlasIndex(style, type, color, isLit);
    int row = idx / 8;
    int col = idx % 8;
    int flippedRow = (uvHelper.texturesHigh - 1) - row;
    return new int[]{flippedRow, col};
  }

  private static int getFlatAtlasIndex(TrafficSignalBulbStyle style, TrafficSignalBulbType type,
      TrafficSignalBulbColor color, boolean isLit) {
    int colorIdx = color.ordinal(); // 0=green, 1=yellow, 2=red
    if (!isLit) {
      switch (type) {
        case BIKE:
          return colorIdx; // Row 0: 0, 1, 2
        case BALL:
          if (style == TrafficSignalBulbStyle.LED_DOTTED) {
            return 48; // Row 6: 48
          }
          if (style == TrafficSignalBulbStyle.LED) {
            return 30 + colorIdx; // Row 3: 30, 31, 32
          }
          if (style == TrafficSignalBulbStyle.INCANDESCENT) {
            return 34 + colorIdx; // Row 4: 34, 35, 36
          }
          break;
        case UTURN:
          return 16 + colorIdx; // Row 2: 16, 17, 18
        case TRANSIT:
        case TRANSIT_LEFT:
        case TRANSIT_RIGHT:
          return 22 + colorIdx; // Row 2: 22, 23,
        case LEFT:
        case UP:
        case UP_LEFT:
        case UP_RIGHT:
        case RIGHT:
          if (style == TrafficSignalBulbStyle.LED_DOTTED) {
            return 6; // Row 0: 6
          }
          if (style == TrafficSignalBulbStyle.LED) {
            return 10 + colorIdx; // Row 1: 10, 11, 12
          }
          if (style == TrafficSignalBulbStyle.INCANDESCENT) {
            return 40 + colorIdx; // Row 5: 40, 41, 42
          }
          break;
      }
    } else {
      switch (type) {
        case BIKE:
          return 3 + colorIdx; // Row 0: 3, 4, 5
        case BALL:
          if (style == TrafficSignalBulbStyle.LED_DOTTED) {
            return 49 + colorIdx; // Row 6: 49, 50, 51
          }
          if (style == TrafficSignalBulbStyle.LED) {
            return 33 + colorIdx; // Row 3: 33, 34, 35
          }
          if (style == TrafficSignalBulbStyle.INCANDESCENT) {
            return 37 + colorIdx; // Row 4: 37, 38, 39
          }
          break;
        case UTURN:
          return 19 + colorIdx; // Row 2: 19, 20, 21
        case TRANSIT:
        case TRANSIT_LEFT:
        case TRANSIT_RIGHT:
          return 25 + colorIdx; // Row 3: 25, 26, 27
        case LEFT:
        case UP:
        case UP_LEFT:
        case UP_RIGHT:
        case RIGHT:
          if (style == TrafficSignalBulbStyle.LED_DOTTED) {
            return 7 + colorIdx; // Row 0-1: 7, 8, 9
          }
          if (style == TrafficSignalBulbStyle.LED) {
            return 13 + colorIdx; // Row 1: 13, 14, 15
          }
          if (style == TrafficSignalBulbStyle.INCANDESCENT) {
            return 43 + colorIdx; // Row 5: 43, 44, 45
          }
          break;
      }
    }
    System.out.println(
        "Fallback to index 63 for type=" + type + ", style=" + style + ", color=" + color + ", lit="
            + isLit);
    return 63; // Fallback
  }

  public static TextureInfo getTextureInfoForBulb(TrafficSignalBulbStyle bulbStyle,
      TrafficSignalBulbType bulbType, TrafficSignalBulbColor bulbColor, boolean isBulbLit) {
    TrafficSignalBulbStyle effectiveStyle = getEffectiveStyle(bulbStyle, bulbType);
    float rotation = getRotationForType(bulbType);

    TextureKey key = new TextureKey(effectiveStyle, bulbType, bulbColor, isBulbLit, rotation);

    TextureInfo cached = textureInfoCache.get(key);
    if (cached != null) {
      return cached;
    }

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

    public TextureInfo(String texture, float rotation, float u1, float v1, float u2, float v2) {
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

    // Invert V so V1 is bottom, V2 is top (Minecraft expects V=0 at bottom)
    public float getV1(int row) {
      return 1.0f - ((float) (row + 1) / texturesHigh);
    }

    public float getU2(int col) {
      return (float) (col + 1) / texturesWide;
    }

    public float getV2(int row) {
      return 1.0f - ((float) row / texturesHigh);
    }
  }
}