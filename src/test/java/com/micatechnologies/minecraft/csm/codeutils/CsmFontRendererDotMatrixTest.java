package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * The dot-matrix font's comma must get a tail, so it no longer reads as a full stop, and the
 * change must reach no other glyph: the atlas is shared by every LED board in the mod.
 */
class CsmFontRendererDotMatrixTest {

  private static final int PITCH = 3;
  private static final int CELL_W = 15;
  private static final int CELL_H = 18;
  private static final int COMMA = ',';

  /** A 16 x 16 atlas of random glyph pixels, with the comma's cell a lone 2 x 2 dot. */
  private static BufferedImage atlas() {
    BufferedImage img = new BufferedImage(CELL_W * 16, CELL_H * 16,
        BufferedImage.TYPE_INT_ARGB);
    Random r = new Random(7);
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        if (r.nextInt(3) == 0) {
          img.setRGB(x, y, 0xFFFFFFFF);
        }
      }
    }
    int cx = (COMMA % 16) * CELL_W;
    int cy = (COMMA / 16) * CELL_H;
    for (int y = 0; y < CELL_H; y++) {
      for (int x = 0; x < CELL_W; x++) {
        img.setRGB(cx + x, cy + y, 0);
      }
    }
    // the full stop part: dots (row 3, cols 1-2) and (row 4, cols 1-2)
    for (int y = 3 * PITCH; y < 5 * PITCH; y++) {
      for (int x = PITCH; x < 3 * PITCH; x++) {
        img.setRGB(cx + x, cy + y, 0xFFFFFFFF);
      }
    }
    return img;
  }

  private static boolean inComma(int x, int y) {
    return x / CELL_W == COMMA % 16 && y / CELL_H == COMMA / 16;
  }

  @Test
  void onlyTheCommaCellChanges() {
    BufferedImage plain = atlas();
    BufferedImage tailed = atlas();
    int[] plainSolid = CsmFontRenderer.applyDotMatrix(plain, PITCH, CELL_W, CELL_H, -1);
    int[] tailedSolid = CsmFontRenderer.applyDotMatrix(tailed, PITCH, CELL_W, CELL_H, COMMA);
    int w = plain.getWidth();
    int changed = 0;
    for (int y = 0; y < plain.getHeight(); y++) {
      for (int x = 0; x < w; x++) {
        boolean same = plain.getRGB(x, y) == tailed.getRGB(x, y)
            && plainSolid[y * w + x] == tailedSolid[y * w + x];
        if (!inComma(x, y)) {
          assertTrue(same, "texel " + x + "," + y + " outside the comma moved");
        } else if (!same) {
          changed++;
        }
      }
    }
    assertTrue(changed > 0, "the comma got no tail");
  }

  @Test
  void tailDescendsDownAndLeft() {
    boolean[][] on = new boolean[6][5];
    on[3][1] = true;
    on[3][2] = true;
    on[4][1] = true;
    on[4][2] = true;
    CsmFontRenderer.addCommaTail(on);
    assertTrue(on[5][2], "a dot under the right of the stop");
    // the second tail dot would be row 6, past this cell: left out, not out of bounds
    boolean[][] tall = new boolean[8][5];
    tall[3][1] = true;
    tall[3][2] = true;
    CsmFontRenderer.addCommaTail(tall);
    assertTrue(tall[4][2]);
    assertTrue(tall[5][1]);
    assertFalse(tall[5][2]);
  }

  @Test
  void emptyCellStaysEmpty() {
    boolean[][] on = new boolean[4][4];
    CsmFontRenderer.addCommaTail(on);
    for (boolean[] row : on) {
      assertArrayEquals(new boolean[4], row);
    }
    assertEquals(4, on.length);
  }
}
