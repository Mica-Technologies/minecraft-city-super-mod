package com.micatechnologies.minecraft.csm.transit.wayfinding;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

/**
 * The large hanging sign's layout. With no text scale it must give exactly the numbers the
 * renderer's own layout gave before the text scale existed: {@link Reference} is that code as it
 * stood, kept here frozen, and every number is compared bit for bit over a spread of sizes,
 * legends, pictograms, arrows and font heights.
 */
class WayfindingPanelLayoutTest {

  static final int[] WIDTHS = {1, 2, 3, 5, 8, 16};
  static final int[] HEIGHTS = {1, 2, 3, 6};
  static final String[][] LEGENDS = {{"GATES", ""}, {"GROUND", "TRANSPORTATION"},
      {"", "ONLY TWO"}, {"", ""}, {"A VERY LONG LEGEND LINE HERE", "X"}, {"W", "MMMMMMMMMMMMM"}};
  static final int[] FONT_HEIGHTS = {9, 32, 64};

  /** A made-up but uneven font: every character its own width. */
  static final ToIntFunction<String> WIDTH = s -> {
    int w = 0;
    for (int i = 0; i < s.length(); i++) {
      w += 3 + s.charAt(i) % 7;
    }
    return w;
  };

  /** The renderer's layout as it stood before the text scale (2026.10), frozen. */
  static final class Reference {

    float margin;
    float square;
    float artMiddle;
    float arrow;
    float pictX;
    float arrowX;
    float textX;
    float pictXArrowLeft;
    float arrowXLeft;
    float textXArrowLeft;
    float textTop;
    float textScale;
    String[] lines;
    int lineStep;

    static Reference of(int width, int height, boolean pictogram, boolean arrow, String line1,
        String line2, int fontHeight, ToIntFunction<String> stringWidth) {
      final float FRAME = 0.8f / 16f;
      final float CAP_SHARE = 0.45f;
      final float CAPS_ONE_LINE = 0.45f;
      final float CAPS_TWO_LINES = 0.28f;
      final float ARROW_SHARE = 0.8f;
      final float MIN_ART_SHARE = 0.55f;
      final float LINE_STEP = 0.85f;
      Reference l = new Reference();
      l.margin = FRAME + 0.08f * height;
      float fullSquare = height - 2 * l.margin;
      float gap = l.margin;
      if (line1.isEmpty() && line2.isEmpty()) {
        l.lines = new String[0];
      } else if (line1.isEmpty() || line2.isEmpty()) {
        l.lines = new String[]{line1.isEmpty() ? line2 : line1};
      } else {
        l.lines = new String[]{line1, line2};
      }
      int n = l.lines.length;
      l.lineStep = Math.round(fontHeight * LINE_STEP);
      int widest = 0;
      for (String line : l.lines) {
        widest = Math.max(widest, stringWidth.applyAsInt(line));
      }
      float wantScale = n == 0 ? 0
          : (n == 1 ? CAPS_ONE_LINE : CAPS_TWO_LINES) * height / (CAP_SHARE * fontHeight);
      float perSquare = (pictogram ? 1f : 0f) + (arrow ? ARROW_SHARE : 0f);
      float room = width - 2 * l.margin - (pictogram ? fullSquare + gap : 0)
          - (arrow ? fullSquare * ARROW_SHARE + gap : 0);
      float short_ = widest * wantScale - room;
      l.square = fullSquare;
      if (short_ > 0 && perSquare > 0) {
        l.square = Math.max(fullSquare * MIN_ART_SHARE, fullSquare - short_ / perSquare);
      }
      l.arrow = l.square * ARROW_SHARE;
      l.artMiddle = height / 2f;
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
      }
      return l;
    }
  }

  private static void assertSame(float expected, float actual, String what) {
    assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(actual),
        () -> what + ": expected " + expected + " but was " + actual);
  }

  @Test
  void unscaledLayoutMatchesTheRendererAsItStood() {
    int cases = 0;
    for (int w : WIDTHS) {
      for (int h : HEIGHTS) {
        for (String[] legend : LEGENDS) {
          for (int fh : FONT_HEIGHTS) {
            for (int art = 0; art < 4; art++) {
              boolean pict = (art & 1) != 0;
              boolean arrow = (art & 2) != 0;
              String what = w + "x" + h + " " + legend[0] + "/" + legend[1] + " fh " + fh
                  + " pict " + pict + " arrow " + arrow;
              Reference r = Reference.of(w, h, pict, arrow, legend[0], legend[1], fh, WIDTH);
              WayfindingPanelLayout l = WayfindingPanelLayout.of(w, h, pict, arrow, legend[0],
                  legend[1], 1f, 1f, fh, WIDTH, 0L);
              assertSame(r.margin, l.margin, what + " margin");
              assertSame(r.square, l.square, what + " square");
              assertSame(r.artMiddle, l.artMiddle, what + " artMiddle");
              assertSame(r.arrow, l.arrow, what + " arrow");
              assertSame(r.pictX, l.pictX, what + " pictX");
              assertSame(r.arrowX, l.arrowX, what + " arrowX");
              assertSame(r.textX, l.textX, what + " textX");
              assertSame(r.pictXArrowLeft, l.pictXArrowLeft, what + " pictXArrowLeft");
              assertSame(r.arrowXLeft, l.arrowXLeft, what + " arrowXLeft");
              assertSame(r.textXArrowLeft, l.textXArrowLeft, what + " textXArrowLeft");
              assertSame(r.textTop, l.textTop, what + " textTop");
              assertSame(r.textScale, l.textScale, what + " textScale");
              assertArrayEquals(r.lines, l.lines, what + " lines");
              assertEquals(r.lineStep, l.lineStep, what + " lineStep");
              assertTrue(l.isUnscaled(), what + " unscaled");
              for (int i = 0; i < l.lines.length; i++) {
                assertSame(i * r.lineStep, l.lineY[i], what + " lineY " + i);
              }
              assertEquals(0L, l.textKey, what + " textKey");
              cases++;
            }
          }
        }
      }
    }
    assertEquals(WIDTHS.length * HEIGHTS.length * LEGENDS.length * FONT_HEIGHTS.length * 4,
        cases);
  }

  /** A line's middle, up the face, in blocks: its box's, from the top of the first line's. */
  private static float middle(WayfindingPanelLayout l, int i, int fontHeight) {
    return l.textTop - (l.lineY[i] + fontHeight * l.lineK[i] / 2f) * l.textScale;
  }

  @Test
  void scaledLinesKeepTheirMiddlesAndLeftEdge() {
    int fh = 9;
    for (float s1 : new float[]{1f, 0.85f, 0.75f, 0.5f}) {
      for (float s2 : new float[]{1f, 0.9f, 0.6f}) {
        WayfindingPanelLayout plain = WayfindingPanelLayout.of(16, 2, true, true, "GATES",
            "B1-B20", 1f, 1f, fh, WIDTH, 0L);
        WayfindingPanelLayout l = WayfindingPanelLayout.of(16, 2, true, true, "GATES",
            "B1-B20", s1, s2, fh, WIDTH, 0L);
        // a wide panel: nothing is fitted, so each line is drawn at exactly its scale
        assertEquals(s1, l.lineK[0] * l.textScale / plain.textScale, 1e-6f);
        assertEquals(s2, l.lineK[1] * l.textScale / plain.textScale, 1e-6f);
        assertEquals(plain.textX, l.textX);
        assertEquals(plain.square, l.square);
        for (int i = 0; i < 2; i++) {
          assertEquals(middle(plain, i, fh), middle(l, i, fh), 1e-5f);
        }
      }
    }
  }

  @Test
  void scaledLinesFitTheWidth() {
    int fh = 9;
    for (int w : WIDTHS) {
      for (String[] legend : LEGENDS) {
        for (float s : new float[]{1f, 0.9f, 0.75f, 0.5f}) {
          WayfindingPanelLayout l = WayfindingPanelLayout.of(w, 1, true, true, legend[0],
              legend[1], s, 1f, fh, WIDTH, 0L);
          if (l.textScale <= 0) {
            continue;
          }
          float room = (l.arrowX - l.margin) - l.textX;
          for (int i = 0; i < l.lines.length; i++) {
            float drawn = WIDTH.applyAsInt(l.lines[i]) * l.textScale * l.lineK[i];
            assertTrue(drawn <= room + 1e-4f, w + " " + legend[0] + "/" + legend[1] + " at "
                + s + ": line " + i + " is " + drawn + " wide in " + room);
          }
        }
      }
    }
  }

  @Test
  void smallerLegendTakesLessFromTheArt() {
    WayfindingPanelLayout full = WayfindingPanelLayout.of(3, 1, true, true,
        "A VERY LONG LEGEND LINE HERE", "", 1f, 1f, 9, WIDTH, 0L);
    WayfindingPanelLayout scaled = WayfindingPanelLayout.of(3, 1, true, true,
        "A VERY LONG LEGEND LINE HERE", "", 0.5f, 1f, 9, WIDTH, 0L);
    assertTrue(scaled.square >= full.square);
  }

  @Test
  void scaledTextHasItsOwnGlyphListKey() {
    WayfindingPanelLayout plain = WayfindingPanelLayout.of(16, 2, true, true, "GATES", "",
        1f, 1f, 9, WIDTH, 42L);
    WayfindingPanelLayout scaled = WayfindingPanelLayout.of(16, 2, true, true, "GATES", "",
        0.8f, 1f, 9, WIDTH, 42L);
    assertEquals(42L, plain.textKey);
    assertTrue(plain.isUnscaled());
    assertFalse(scaled.isUnscaled());
    assertNotEquals(plain.textKey, scaled.textKey);
  }
}
