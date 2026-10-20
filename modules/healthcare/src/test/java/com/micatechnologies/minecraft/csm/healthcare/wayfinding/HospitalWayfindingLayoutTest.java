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
}
