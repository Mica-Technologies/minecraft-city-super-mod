package com.micatechnologies.minecraft.csm.transit.wayfinding;

import java.util.function.ToIntFunction;

/**
 * Where everything goes on a face of a large hanging sign ({@link BlockWayfindingPanel}), in
 * blocks in the reader's space (x along the panel to the reader's right from its left edge, y up
 * from its bottom); worked out once per sign and panel size and kept on the tile entity. Kept free
 * of the client, the font reached only through its line height and a width function, so it can be
 * tested; {@link TileEntityWayfindingPanelRenderer} draws from it.
 *
 * <p>The pictogram is a square the panel's height less its margins, at the end away from the
 * arrow; the arrow, {@link #ARROW_SHARE} of that, at the end it points to (the left end for the
 * three leftward arrows, else the right); the text left-aligned between them, its capitals
 * {@link #CAPS_ONE_LINE} of the panel's height on one line or {@link #CAPS_TWO_LINES} each on
 * two, scaled down together to fit the width.</p>
 *
 * <p><b>The text scale.</b> Each line may be drawn smaller ({@code ts} times {@code ts1} or
 * {@code ts2}, {@link TileEntityWayfindingPanel}): the size it wants is scaled, and the lines are
 * then fitted to the width together at the sizes they want, so a scaled line is never made wider
 * than the room. A smaller legend also takes less room from the pictogram and arrow. Each line
 * keeps the middle it has at full size ({@link #textTop} and {@link #lineStep} are the full-size
 * layout's) and stays left-aligned beside the art. A line's own scale is {@link #lineK}, relative
 * to {@link #textScale}, and its top {@link #lineY}; with no scale both are exactly what the
 * layout always gave, 1 and the line's step.</p>
 *
 * @since 2026.10
 */
final class WayfindingPanelLayout {

  /** The frame's width on the panel's outer edges (the generator's PANEL_FRAME), in blocks. */
  static final float FRAME = 0.8f / 16f;
  /**
   * The Highway Gothic capitals' height as a share of the font's line height, measured in game:
   * the line height leaves room for accents and descenders, so the legend is sized by its
   * capitals, which sit about centred in the line.
   */
  static final float CAP_SHARE = 0.45f;
  /** The capitals' height on a one-line sign and on each line of a two-line one, of the panel. */
  static final float CAPS_ONE_LINE = 0.45f;
  static final float CAPS_TWO_LINES = 0.28f;
  /** The arrow's square as a share of the pictogram's: an arrow reads well smaller. */
  static final float ARROW_SHARE = 0.8f;
  /**
   * How small the pictogram and arrow may go, as a share of the panel's height inside its
   * margins, to give a long legend room before the legend itself is shrunk to fit.
   */
  static final float MIN_ART_SHARE = 0.55f;
  /** From one line to the next, in line heights. */
  static final float LINE_STEP = 0.85f;

  float margin;
  float square;
  /** The pictogram's and arrow's middle, up the face. */
  float artMiddle;
  float arrow;
  /** With the arrow at the right end, or none. */
  float pictX;
  float arrowX;
  float textX;
  /** With the arrow at the left end. */
  float pictXArrowLeft;
  float arrowXLeft;
  float textXArrowLeft;
  /** The top of the first line's box, up the face, in blocks. */
  float textTop;
  /** Blocks per font unit, 0 for no legend. */
  float textScale;
  String[] lines;
  /** From one line's top to the next, in font units. */
  int lineStep;
  /** Each line's scale, as a share of {@link #textScale}: 1 for a line at full size. */
  float[] lineK;
  /**
   * Each line's top below {@link #textTop}, in font units at {@link #textScale}: the line's step
   * at full size, a scaled line moved down to keep its middle.
   */
  float[] lineY;
  /** The legend's glyph list key. */
  long textKey;

  private WayfindingPanelLayout() {
  }

  /**
   * A face's layout.
   *
   * @param width       the panel's width, blocks
   * @param height      the panel's height, blocks
   * @param pictogram   whether the sign has a pictogram
   * @param arrow       whether it has an arrow
   * @param line1       the top line, empty for none
   * @param line2       the bottom line, empty for none
   * @param line1Scale  the top line's text scale, 1 for none
   * @param line2Scale  the bottom line's text scale, 1 for none
   * @param fontHeight  the font's line height, font units
   * @param stringWidth a line's width, font units
   * @param textKey     the key the legend's glyph list is kept under
   * @return the layout
   */
  static WayfindingPanelLayout of(int width, int height, boolean pictogram, boolean arrow,
      String line1, String line2, float line1Scale, float line2Scale, int fontHeight,
      ToIntFunction<String> stringWidth, long textKey) {
    WayfindingPanelLayout l = new WayfindingPanelLayout();
    l.textKey = textKey;
    l.margin = FRAME + 0.08f * height;
    float fullSquare = height - 2 * l.margin;
    float gap = l.margin;

    float[] scales;
    if (line1.isEmpty() && line2.isEmpty()) {
      l.lines = new String[0];
      scales = new float[0];
    } else if (line1.isEmpty() || line2.isEmpty()) {
      l.lines = new String[]{line1.isEmpty() ? line2 : line1};
      scales = new float[]{line1.isEmpty() ? line2Scale : line1Scale};
    } else {
      l.lines = new String[]{line1, line2};
      scales = new float[]{line1Scale, line2Scale};
    }
    int n = l.lines.length;
    l.lineStep = Math.round(fontHeight * LINE_STEP);
    l.lineK = new float[n];
    l.lineY = new float[n];
    int widest = 0;
    // the widest line at the size it wants, as a share of full size: widest when unscaled
    float widestScaled = 0;
    for (int i = 0; i < n; i++) {
      l.lineK[i] = 1f;
      l.lineY[i] = i * l.lineStep;
      int w = stringWidth.applyAsInt(l.lines[i]);
      widest = Math.max(widest, w);
      widestScaled = Math.max(widestScaled, w * scales[i]);
    }
    float wantScale = n == 0 ? 0
        : (n == 1 ? CAPS_ONE_LINE : CAPS_TWO_LINES) * height / (CAP_SHARE * fontHeight);

    // A legend too long for the room beside a full-height pictogram and arrow takes room from
    // them first, down to MIN_ART_SHARE, and is shrunk only past that: the words are the sign.
    float perSquare = (pictogram ? 1f : 0f) + (arrow ? ARROW_SHARE : 0f);
    float room = width - 2 * l.margin - (pictogram ? fullSquare + gap : 0)
        - (arrow ? fullSquare * ARROW_SHARE + gap : 0);
    float short_ = widestScaled * wantScale - room;
    l.square = fullSquare;
    if (short_ > 0 && perSquare > 0) {
      l.square = Math.max(fullSquare * MIN_ART_SHARE, fullSquare - short_ / perSquare);
    }
    l.arrow = l.square * ARROW_SHARE;
    l.artMiddle = height / 2f;

    // the arrow at the right end (or none): pictogram, text, arrow
    float xl = l.margin;
    float xr = width - l.margin;
    if (arrow) {
      l.arrowX = xr - l.arrow;
      xr -= l.arrow + gap;
    }
    if (pictogram) {
      l.pictX = xl;
      xl += l.square + gap;
    }
    l.textX = xl;
    float textWidth = xr - xl;
    // the arrow at the left end: arrow, text, pictogram
    xl = l.margin;
    xr = width - l.margin;
    l.arrowXLeft = xl;
    if (arrow) {
      xl += l.arrow + gap;
    }
    if (pictogram) {
      l.pictXArrowLeft = xr - l.square;
    }
    l.textXArrowLeft = xl;

    if (n == 0 || widest == 0 || textWidth <= 0) {
      l.textScale = 0;
    } else {
      float scale = wantScale;
      if (widest * scale > textWidth) {
        scale = textWidth / widest;
      }
      l.textScale = scale;
      float block = ((n - 1) * l.lineStep + fontHeight) * scale;
      l.textTop = height / 2f + block / 2f;
      // the lines at the sizes they want, fitted together; unscaled, exactly the scale above
      float fitted = wantScale;
      if (widestScaled * fitted > textWidth) {
        fitted = textWidth / widestScaled;
      }
      for (int i = 0; i < n; i++) {
        float s = fitted * scales[i];
        l.lineK[i] = s == scale ? 1f : s / scale;
        if (l.lineK[i] != 1f) {
          l.lineY[i] += fontHeight * (1f - l.lineK[i]) / 2f;
          l.textKey = l.textKey * 1000003L ^ Float.floatToIntBits(l.lineK[i]);
          l.textKey = l.textKey * 1000003L ^ Float.floatToIntBits(l.lineY[i]);
        }
      }
    }
    return l;
  }

  /** Whether every line is drawn at {@link #textScale}, one step apart: no text scale shows. */
  boolean isUnscaled() {
    for (float k : lineK) {
      if (k != 1f) {
        return false;
      }
    }
    return true;
  }
}
