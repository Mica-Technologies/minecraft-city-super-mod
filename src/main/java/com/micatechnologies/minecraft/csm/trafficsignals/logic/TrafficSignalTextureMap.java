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
    // Atlas tile order (from ImageTilerTool INPUT_IMAGE_NAMES):
    //  0-2:  biled off (G/Y/R)        3-5:  biled on (G/Y/R)         [BIKE]
    //  6:    led dotted arrow off      7-9:  led dotted arrow (G/Y/R) [LED_DOTTED arrow]
    // 10-12: arrow off (G/Y/R)       13-15: arrow on (G/Y/R)         [LED arrow]
    // 16-18: uturn off (G/Y/R)       19-21: uturn on (G/Y/R)         [UTURN]
    // 22-24: wled off (G/Y/R)        25-27: wled on (G/Y/R)          [TRANSIT]
    // 28-30: iled off (G/Y/R)        31-33: iled on (G/Y/R)          [LED ball]
    // 34-36: inca off (G/Y/R)        37-39: inca on (G/Y/R)          [INCANDESCENT ball]
    // 40-42: inca arrow off (G/Y/R)  43-45: inca arrow on (G/Y/R)    [INCANDESCENT arrow]
    // 46:    eled off                 47-49: eled on (G/Y/R)          [LED_DOTTED ball]
    // 50:    gtx off                  51-53: gtx on (G/Y/R)           [unused/future]
    // 54:    wled_red_x                                               [unused/future]
    if (!isLit) {
      switch (type) {
        case BIKE:
          return colorIdx; // 0, 1, 2: biled off (G/Y/R)
        case BALL:
          if (style == TrafficSignalBulbStyle.LED_DOTTED) {
            return 46; // eled_off
          }
          if (style == TrafficSignalBulbStyle.LED) {
            return 28 + colorIdx; // 28-30: iled off (G/Y/R)
          }
          if (style == TrafficSignalBulbStyle.INCANDESCENT) {
            return 34 + colorIdx; // 34-36: inca off (G/Y/R)
          }
          break;
        case UTURN:
          return 16 + colorIdx; // 16-18: uturn off (G/Y/R)
        case TRANSIT:
        case TRANSIT_LEFT:
        case TRANSIT_RIGHT:
          return 22 + colorIdx; // 22-24: wled off (G/Y/R)
        case LEFT:
        case UP:
        case UP_LEFT:
        case UP_RIGHT:
        case RIGHT:
          if (style == TrafficSignalBulbStyle.LED_DOTTED) {
            return 6; // led arrow off
          }
          if (style == TrafficSignalBulbStyle.LED) {
            return 10 + colorIdx; // 10-12: arrow off (G/Y/R)
          }
          if (style == TrafficSignalBulbStyle.INCANDESCENT) {
            return 40 + colorIdx; // 40-42: inca arrow off (G/Y/R)
          }
          break;
      }
    } else {
      switch (type) {
        case BIKE:
          return 3 + colorIdx; // 3-5: biled on (G/Y/R)
        case BALL:
          if (style == TrafficSignalBulbStyle.LED_DOTTED) {
            return 47 + colorIdx; // 47-49: eled on (G/Y/R)
          }
          if (style == TrafficSignalBulbStyle.LED) {
            return 31 + colorIdx; // 31-33: iled on (G/Y/R)
          }
          if (style == TrafficSignalBulbStyle.INCANDESCENT) {
            return 37 + colorIdx; // 37-39: inca on (G/Y/R)
          }
          break;
        case UTURN:
          return 19 + colorIdx; // 19-21: uturn on (G/Y/R)
        case TRANSIT:
        case TRANSIT_LEFT:
        case TRANSIT_RIGHT:
          return 25 + colorIdx; // 25-27: wled on (G/Y/R)
        case LEFT:
        case UP:
        case UP_LEFT:
        case UP_RIGHT:
        case RIGHT:
          if (style == TrafficSignalBulbStyle.LED_DOTTED) {
            return 7 + colorIdx; // 7-9: led dotted arrow (G/Y/R)
          }
          if (style == TrafficSignalBulbStyle.LED) {
            return 13 + colorIdx; // 13-15: arrow on (G/Y/R)
          }
          if (style == TrafficSignalBulbStyle.INCANDESCENT) {
            return 43 + colorIdx; // 43-45: inca arrow on (G/Y/R)
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
    // AHEAD type: UP arrow for green aspect, BALL for red/yellow
    TrafficSignalBulbType effectiveBulbType = bulbType;
    if (bulbType == TrafficSignalBulbType.AHEAD) {
      effectiveBulbType = (bulbColor == TrafficSignalBulbColor.GREEN)
          ? TrafficSignalBulbType.UP
          : TrafficSignalBulbType.BALL;
    }
    TrafficSignalBulbStyle effectiveStyle = getEffectiveStyle(bulbStyle, effectiveBulbType);
    float rotation = getRotationForType(effectiveBulbType);

    TextureKey key = new TextureKey(effectiveStyle, effectiveBulbType, bulbColor, isBulbLit, rotation);

    TextureInfo cached = textureInfoCache.get(key);
    if (cached != null) {
      return cached;
    }

    int[] rowCol = getAtlasRowCol(effectiveStyle, effectiveBulbType, bulbColor, isBulbLit);
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