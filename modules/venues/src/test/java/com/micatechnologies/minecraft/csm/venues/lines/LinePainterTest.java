package com.micatechnologies.minecraft.csm.venues.lines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Holds {@link LinePainter} to {@code venues_lines.py}: every cell of every layout is cut into
 * pieces that exactly cover it, and the cells the generators sampled come out with the same area
 * of each material as the generator found (which is how {@code --layout} chose them as painted
 * cells).
 */
class LinePainterTest {

  @Test
  void piecesCoverEveryCell() {
    for (LineArtFamily fam : LineArtFamily.ALL) {
      for (int k = 0; k < fam.layoutCount(); k++) {
        LineArtLayout lay = fam.layout(k);
        int step = lay.extent[2] - lay.extent[0] > 60 ? 3 : 1;
        for (int j = lay.extent[1]; j <= lay.extent[3]; j += step) {
          for (int i = lay.extent[0]; i <= lay.extent[2]; i += step) {
            double sum = 0;
            for (LinePainter.Piece p : LinePainter.paint(lay, i, j)) {
              double a = LinePainter.area2(p.pts) / 2;
              sum += Math.abs(a);
              assertTrue(p.pts.length >= 6, lay.name + " " + i + "," + j);
            }
            assertEquals(1.0, sum, 1e-6, lay.name + " cell " + i + "," + j);
          }
        }
      }
    }
  }

  @Test
  void matchesTheGenerator() {
    check(CourtSamples.FAMILY, CourtSamples.CELLS);
    check(TrackSamples.FAMILY, TrackSamples.CELLS);
    check(PoolSamples.FAMILY, PoolSamples.CELLS);
  }

  private static void check(LineArtFamily fam, double[][] cells) {
    assertTrue(cells.length > 10);
    for (double[] c : cells) {
      LineArtLayout lay = fam.layout((int) c[0]);
      int i = (int) c[1];
      int j = (int) c[2];
      Map<Integer, Double> got = new HashMap<>();
      List<LinePainter.Piece> pieces = LinePainter.paint(lay, i, j);
      for (LinePainter.Piece p : pieces) {
        got.merge(p.mat, Math.abs(LinePainter.area2(p.pts) / 2), Double::sum);
      }
      for (int o = 3; o + 1 < c.length; o += 2) {
        int mat = (int) c[o];
        double want = c[o + 1];
        assertEquals(want, got.getOrDefault(mat, 0.0), 2e-5,
            lay.name + " cell " + i + "," + j + " material " + mat);
      }
    }
  }
}
