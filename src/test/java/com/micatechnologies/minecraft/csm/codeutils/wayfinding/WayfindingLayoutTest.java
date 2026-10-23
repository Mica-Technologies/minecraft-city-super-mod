package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

import static org.junit.jupiter.api.Assertions.*;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingLayout.Piece;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Where a wayfinding panel's face puts things, and where its panel is drawn in its cells.
 */
class WayfindingLayoutTest {

  private static final float EPS = 1e-4f;

  private static List<Piece> art(List<Piece> pieces, int sheet) {
    pieces.removeIf(p -> p.layer != WayfindingLayout.LAYER_ART || p.sheet != sheet);
    return pieces;
  }

  private static float minX(List<Piece> ps) {
    float m = Float.MAX_VALUE;
    for (Piece p : ps) {
      m = Math.min(m, p.x0);
    }
    return m;
  }

  private static float maxX(List<Piece> ps) {
    float m = -Float.MAX_VALUE;
    for (Piece p : ps) {
      m = Math.max(m, p.x1);
    }
    return m;
  }

  @Test
  void everythingStaysInsideTheFrame() {
    WayfindingSign sign = WayfindingSign.builder().line(0, "A VERY LONG LEGEND INDEED")
        .line(1, "AND ANOTHER ONE").pictogram(WayfindingIcon.INFO).arrow(WayfindingArrow.LEFT)
        .iconLeft(0, WayfindingIcon.STAIRS).iconRight(1, WayfindingIcon.ELEVATOR).border(2)
        .build();
    for (Piece p : WayfindingLayout.face(sign, 3, 1, false)) {
      assertTrue(p.x0 >= WayfindingLayout.FRAME - EPS && p.x1 <= 3 - WayfindingLayout.FRAME + EPS,
          "x " + p.x0 + ".." + p.x1);
      assertTrue(p.y0 >= WayfindingLayout.FRAME - EPS && p.y1 <= 1 - WayfindingLayout.FRAME + EPS,
          "y " + p.y0 + ".." + p.y1);
    }
  }

  @Test
  void alignmentMovesTheLine() {
    WayfindingSign.Builder b = WayfindingSign.builder().line(0, "L3");
    List<Piece> left = art(WayfindingLayout.face(b.build(), 4, 1, false),
        WayfindingLayout.SHEET_GLYPHS);
    List<Piece> centre = art(WayfindingLayout.face(
        b.align(WayfindingSign.Align.CENTRE).build(), 4, 1, false), WayfindingLayout.SHEET_GLYPHS);
    List<Piece> right = art(WayfindingLayout.face(
        b.align(WayfindingSign.Align.RIGHT).build(), 4, 1, false), WayfindingLayout.SHEET_GLYPHS);
    assertTrue(minX(left) < minX(centre) && minX(centre) < minX(right));
    // centred: as much room either side
    assertEquals(minX(centre), 4 - maxX(centre), 0.05f);
  }

  @Test
  void aLineIconSitsBesideItsText() {
    WayfindingSign sign = WayfindingSign.builder().line(0, "STAIR A")
        .iconLeft(0, WayfindingIcon.STAIRS).iconStyle(WayfindingSign.IconStyle.PLAIN).build();
    List<Piece> pieces = WayfindingLayout.face(sign, 4, 1, false);
    List<Piece> icons = art(WayfindingLayout.face(sign, 4, 1, false),
        WayfindingLayout.SHEET_ICONS);
    List<Piece> glyphs = art(pieces, WayfindingLayout.SHEET_GLYPHS);
    assertEquals(1, icons.size());
    assertTrue(icons.get(0).x1 < minX(glyphs));
  }

  @Test
  void aTiledIconHasItsTileBehind() {
    WayfindingSign sign = WayfindingSign.builder().pictogram(WayfindingIcon.EXIT).build();
    long tiles = WayfindingLayout.face(sign, 1, 1, false).stream()
        .filter(p -> p.layer == WayfindingLayout.LAYER_TILE).count();
    assertEquals(1, tiles);
    WayfindingSign plain = sign.toBuilder().iconStyle(WayfindingSign.IconStyle.PLAIN).build();
    assertEquals(0, WayfindingLayout.face(plain, 1, 1, false).stream()
        .filter(p -> p.layer == WayfindingLayout.LAYER_TILE).count());
  }

  @Test
  void theBackMirrorsTheArrow() {
    WayfindingSign sign = WayfindingSign.builder().line(0, "GATES")
        .arrow(WayfindingArrow.LEFT).build();
    List<Piece> front = art(WayfindingLayout.face(sign, 4, 1, false),
        WayfindingLayout.SHEET_ICONS);
    List<Piece> back = art(WayfindingLayout.face(sign, 4, 1, true),
        WayfindingLayout.SHEET_ICONS);
    // the left arrow sits at the left end in front, the mirrored one at the right end behind
    assertTrue(front.get(0).x1 < 2);
    assertTrue(back.get(0).x0 > 2);
  }

  @Test
  void moreLinesAreSmaller() {
    WayfindingSign one = WayfindingSign.builder().line(0, "A").build();
    WayfindingSign four = WayfindingSign.builder().line(0, "A").line(1, "B").line(2, "C")
        .line(3, "D").build();
    float h1 = art(WayfindingLayout.face(one, 4, 2, false), WayfindingLayout.SHEET_GLYPHS)
        .get(0).y1;
    List<Piece> g4 = art(WayfindingLayout.face(four, 4, 2, false), WayfindingLayout.SHEET_GLYPHS);
    assertEquals(4, g4.size());
    float span1 = h1 - art(WayfindingLayout.face(one, 4, 2, false),
        WayfindingLayout.SHEET_GLYPHS).get(0).y0;
    float span4 = g4.get(0).y1 - g4.get(0).y0;
    assertTrue(span4 < span1);
  }

  @Test
  void aLineCountKeepsAnEmptyLineAsAGap() {
    WayfindingSign auto = WayfindingSign.builder().line(0, "A").line(2, "C").build();
    assertArrayEquals(new int[]{0, 2}, auto.shownLines());
    WayfindingSign three = auto.toBuilder().lineCount(3).build();
    assertArrayEquals(new int[]{0, 1, 2}, three.shownLines());
  }

  @Test
  void thePanelSitsInItsCellsByMount() {
    WayfindingSign half = WayfindingSign.builder().panelScale(0.5f).drop(4).build();
    WayfindingFace hung = new WayfindingFace(half.toBuilder().mount(WayfindingMount.HUNG).build(),
        0, 0, 2, 1, WayfindingMount.HUNG, null, null);
    assertEquals(1f, hung.panelWidth(), EPS);
    assertEquals(0.5f, hung.panelX0(), EPS);
    // its top a quarter of a block below the cells' top
    assertEquals(1f - 0.25f, hung.panelY0() + hung.panelHeight(), EPS);
    WayfindingFace post = new WayfindingFace(half, 0, 0, 2, 1, WayfindingMount.POST, null, null);
    assertEquals(0.25f, post.panelY0(), EPS);
    assertEquals(1, post.supportXs(true).length);
    WayfindingFace ceiling = new WayfindingFace(half, 0, 0, 2, 1, WayfindingMount.CEILING, null,
        null);
    assertEquals(1f, ceiling.panelY0() + ceiling.panelHeight(), EPS);
    WayfindingFace wall = new WayfindingFace(half.toBuilder().doubleSided(true).build(), 0, 0,
        2, 1, WayfindingMount.WALL, null, null);
    assertFalse(wall.drawsBack());
    assertEquals(16f, wall.slabZ1(), EPS);
    // an unscaled hung panel fills its cells exactly, as the panels always did
    WayfindingFace full = new WayfindingFace(WayfindingSign.builder().build(), 0, 0, 3, 2,
        WayfindingMount.HUNG, null, null);
    assertEquals(0f, full.panelX0(), EPS);
    assertEquals(0f, full.panelY0(), EPS);
    assertEquals(2, full.supportXs(false).length);
  }

  @Test
  void autoIsNeverDrawn() {
    WayfindingFace f = new WayfindingFace(WayfindingSign.builder().build(), 0, 0, 1, 1,
        WayfindingMount.AUTO, null, null);
    assertEquals(WayfindingMount.HUNG, f.getMount());
  }
}
