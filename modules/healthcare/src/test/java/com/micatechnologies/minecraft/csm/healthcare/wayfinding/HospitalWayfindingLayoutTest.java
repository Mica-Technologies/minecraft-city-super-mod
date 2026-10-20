package com.micatechnologies.minecraft.csm.healthcare.wayfinding;

import static org.junit.jupiter.api.Assertions.*;

import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingLayout.Piece;
import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingSign.Arrow;
import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingSign.Pictogram;
import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingSign.Scheme;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The hospital wayfinding panel's layout. With no text scale every piece of every face must be
 * exactly where it was before the text scale existed: the digest below was taken from the layout
 * code as it stood then, over a spread of sizes, legends, pictograms and arrows.
 */
class HospitalWayfindingLayoutTest {

  /** SHA-256 of {@link #dump()} as the layout stood before the text scale. */
  private static final String UNSCALED_DIGEST =
      "6cb2dedc45adf8df815de41dd6378483962b445d7fb9b1a92840d426646e7ff3";

  private static final int[] WIDTHS = {1, 2, 3, 5, 8, 16};
  private static final int[] HEIGHTS = {1, 2, 3, 6};
  private static final String[][] LEGENDS = {{"EXIT", ""}, {"GROUND", "TRANSPORTATION"},
      {"", "ONLY TWO"}, {"", ""}, {"A VERY LONG LEGEND LINE HERE", "X"}};
  private static final Pictogram[] PICTOGRAMS = {Pictogram.INFO, Pictogram.NONE};
  private static final Arrow[] ARROWS = {Arrow.RIGHT, Arrow.LEFT, Arrow.NONE, Arrow.UP_LEFT};

  static HospitalWayfindingFace face(String l1, String l2, Pictogram p, Arrow a, int w, int h) {
    return new HospitalWayfindingFace(l1, l2, p, a, Scheme.NEUTRAL, true, 0, 0, w, h, null);
  }

  /** Every number of every piece, exactly (as float bits). */
  private static String dump() {
    StringBuilder sb = new StringBuilder();
    for (int w : WIDTHS) {
      for (int h : HEIGHTS) {
        for (String[] legend : LEGENDS) {
          for (Pictogram p : PICTOGRAMS) {
            for (Arrow a : ARROWS) {
              sb.append(w).append('x').append(h).append(' ').append(legend[0]).append('/')
                  .append(legend[1]).append(' ').append(p).append(' ').append(a).append('\n');
              List<Piece> pieces = HospitalWayfindingLayout.face(
                  face(legend[0], legend[1], p, a, w, h), a);
              for (Piece q : pieces) {
                for (float v : new float[]{q.x0, q.y0, q.x1, q.y1, q.ua, q.ub, q.uc, q.va, q.vb,
                    q.vc}) {
                  sb.append(Integer.toHexString(Float.floatToIntBits(v))).append(' ');
                }
                sb.append(q.rgb).append(' ').append(q.layer).append('\n');
              }
            }
          }
        }
      }
    }
    return sb.toString();
  }

  private static String sha256(String s) throws Exception {
    byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte b : d) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  @Test
  void unscaledLayoutIsUnchanged() throws Exception {
    assertEquals(UNSCALED_DIGEST, sha256(dump()));
  }

  /** The legend's glyph pieces: every piece after the background, pictogram and arrow. */
  private static List<Piece> glyphs(List<Piece> pieces, boolean pictogram, boolean arrow) {
    return pieces.subList(1 + (pictogram ? 1 : 0) + (arrow ? 1 : 0), pieces.size());
  }

  @Test
  void scaledLineKeepsItsMiddleAndLeftEdge() {
    // a wide panel, so the legend wants its full size and is not fitted to the width
    HospitalWayfindingFace plain = face("EXIT", "", Pictogram.INFO, Arrow.RIGHT, 8, 2);
    HospitalWayfindingFace scaled = new HospitalWayfindingFace("EXIT", "", Pictogram.INFO,
        Arrow.RIGHT, Scheme.NEUTRAL, true, 0.8f, 1f, 0, 0, 8, 2, null);
    List<Piece> a = glyphs(HospitalWayfindingLayout.face(plain, Arrow.RIGHT), true, true);
    List<Piece> b = glyphs(HospitalWayfindingLayout.face(scaled, Arrow.RIGHT), true, true);
    assertEquals(4, a.size());
    assertEquals(a.size(), b.size());
    // the pen starts past the full-size pictogram (margin + square + gap = the height), and one
    // line's capitals are centred on the panel's middle
    float left = 2f;
    float middle = 1f;
    for (int i = 0; i < a.size(); i++) {
      Piece p = a.get(i);
      Piece q = b.get(i);
      assertEquals(left + (p.x0 - left) * 0.8f, q.x0, 1e-5f);
      assertEquals(left + (p.x1 - left) * 0.8f, q.x1, 1e-5f);
      assertEquals(middle + (p.y0 - middle) * 0.8f, q.y0, 1e-5f);
      assertEquals(middle + (p.y1 - middle) * 0.8f, q.y1, 1e-5f);
    }
  }

  @Test
  void scaledLineIsFittedToTheWidth() {
    // a narrow panel: the legend is fitted to the width at full size and at 0.8 alike
    for (float s : new float[]{1f, 0.9f, 0.8f, 0.6f, 0.5f}) {
      HospitalWayfindingFace f = new HospitalWayfindingFace("GROUND", "TRANSPORTATION",
          Pictogram.INFO, Arrow.RIGHT, Scheme.NEUTRAL, true, s, s, 0, 0, 3, 2, null);
      List<Piece> pieces = HospitalWayfindingLayout.face(f, Arrow.RIGHT);
      Piece arrow = pieces.get(2);
      for (Piece g : glyphs(pieces, true, true)) {
        assertTrue(g.x1 <= arrow.x0 + 1e-4f, "scale " + s + ": a glyph reaches the arrow");
      }
    }
  }

  @Test
  void oneLineScaledLeavesTheOtherAlone() {
    HospitalWayfindingFace plain = face("RADIOLOGY", "WEST", Pictogram.NONE, Arrow.NONE, 12, 2);
    HospitalWayfindingFace scaled = new HospitalWayfindingFace("RADIOLOGY", "WEST",
        Pictogram.NONE, Arrow.NONE, Scheme.NEUTRAL, true, 1f, 0.6f, 0, 0, 12, 2, null);
    List<Piece> a = glyphs(HospitalWayfindingLayout.face(plain, Arrow.NONE), false, false);
    List<Piece> b = glyphs(HospitalWayfindingLayout.face(scaled, Arrow.NONE), false, false);
    assertEquals(a.size(), b.size());
    int first = "RADIOLOGY".length();
    for (int i = 0; i < first; i++) {
      assertEquals(a.get(i).x0, b.get(i).x0);
      assertEquals(a.get(i).y0, b.get(i).y0);
      assertEquals(a.get(i).x1, b.get(i).x1);
      assertEquals(a.get(i).y1, b.get(i).y1);
    }
    // the second line: 0.6 the size, about its own capitals' middle and the text's left edge
    float cap = 0.28f * 2;
    float step = cap * 0.85f / 0.45f;
    float top = 1f + (cap + step) / 2f;
    float middle = top - step - cap / 2f;
    float left = 0.8f / 16f + 0.08f * 2;
    for (int i = first; i < a.size(); i++) {
      Piece p = a.get(i);
      Piece q = b.get(i);
      assertEquals(left + (p.x0 - left) * 0.6f, q.x0, 1e-5f);
      assertEquals(left + (p.x1 - left) * 0.6f, q.x1, 1e-5f);
      assertEquals(middle + (p.y0 - middle) * 0.6f, q.y0, 1e-5f);
      assertEquals(middle + (p.y1 - middle) * 0.6f, q.y1, 1e-5f);
    }
  }
}
