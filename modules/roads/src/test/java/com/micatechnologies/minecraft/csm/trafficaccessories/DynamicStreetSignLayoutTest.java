package com.micatechnologies.minecraft.csm.trafficaccessories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityDynamicStreetSignRenderer.Layout;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignData;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignLegend;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignMount;
import org.junit.jupiter.api.Test;

/**
 * The two-blade layout: a stacked pair shares one size, the top blade stays where a single
 * blade hangs, and the lower blade sits a fixed gap below it.
 */
class DynamicStreetSignLayoutTest {

  private static final float EPS = 1e-3f;
  private final TileEntityDynamicStreetSignRenderer renderer =
      new TileEntityDynamicStreetSignRenderer();

  private static StreetSignData sign(StreetSignMount mount, boolean framed) {
    StreetSignData data = new StreetSignData();
    data.setMountType(mount);
    data.setExtrudedFrame(framed);
    data.setBorderWidth(2);
    data.setStreetName("OAK");
    data.setSuffix("ST");
    return data;
  }

  /** A legend both wider (a long name) and taller (a city line) than the "OAK ST" blade. */
  private static StreetSignLegend bigLegend() {
    StreetSignLegend legend = new StreetSignLegend();
    legend.setStreetName("MARTIN LUTHER KING JR");
    legend.setSuffix("BLVD");
    legend.setCityText("HISTORIC DISTRICT");
    return legend;
  }

  /** The same sign with the big legend as its only blade, to measure that blade alone. */
  private static StreetSignData bigAlone(StreetSignMount mount, boolean framed) {
    StreetSignData data = sign(mount, framed);
    StreetSignLegend big = bigLegend();
    data.setStreetName(big.getStreetName());
    data.setSuffix(big.getSuffix());
    data.setCityText(big.getCityText());
    return data;
  }

  private static float edge(Layout l) {
    return l.borderInset + l.frameOverhangY;
  }

  @Test
  void singleBladeHasNoLowerLayoutAndKeepsItsPlacement() {
    Layout hanging = renderer.computeLayout(sign(StreetSignMount.HANGING, false));
    assertNull(hanging.lower);
    assertEquals(1, hanging.blades().length);
    assertEquals(16.0f - 5.5f, hanging.signTop, EPS);
    assertEquals(hanging.signBottom - edge(hanging), hanging.assemblyBottom, EPS);

    Layout flat = renderer.computeLayout(sign(StreetSignMount.FLAT, false));
    assertEquals(8.0f, (flat.signTop + flat.signBottom) / 2.0f, EPS);
    assertEquals(8.0f, (flat.assemblyTop + flat.assemblyBottom) / 2.0f, EPS);
  }

  @Test
  void stackedBladesShareTheWiderWidthAndTheTallerHeight() {
    for (StreetSignMount mount : StreetSignMount.values()) {
      Layout small = renderer.computeLayout(sign(mount, false));
      Layout big = renderer.computeLayout(bigAlone(mount, false));
      // Sanity: the font metrics loaded and the big legend really is bigger both ways.
      assertTrue(big.signWidth > small.signWidth + 10, "font metrics missing?");
      assertTrue(big.signHeight > small.signHeight);

      // Either order: the small blade on top or below, both take the big one's size.
      StreetSignData smallOnTop = sign(mount, false);
      smallOnTop.setLowerBlade(bigLegend());
      StreetSignData bigOnTop = bigAlone(mount, false);
      StreetSignLegend smallLower = new StreetSignLegend();
      smallLower.setStreetName("OAK");
      smallLower.setSuffix("ST");
      bigOnTop.setLowerBlade(smallLower);

      for (StreetSignData data : new StreetSignData[]{smallOnTop, bigOnTop}) {
        Layout top = renderer.computeLayout(data);
        Layout lower = top.lower;
        assertEquals(big.signWidth, top.signWidth, EPS, mount.name());
        assertEquals(big.signWidth, lower.signWidth, EPS, mount.name());
        assertEquals(big.signHeight, top.signHeight, EPS, mount.name());
        assertEquals(big.signHeight, lower.signHeight, EPS, mount.name());
        assertEquals(top.signLeft, lower.signLeft, EPS);
        assertEquals(top.signRight, lower.signRight, EPS);
        if (mount.isPostTop()) {
          // The second blade crosses the first rather than stacking under it, so it is
          // drawn by its own pass under a quarter turn and this one must not walk to it.
          assertEquals(1, top.blades().length, mount.name());
          assertTrue(top.crossed, mount.name());
          assertTrue(lower.crossed, mount.name());
        } else {
          assertSame(lower, top.blades()[1]);
        }
        assertNull(lower.lower);
      }
    }
  }

  @Test
  void hangingStackKeepsTheTopBladeWhereASingleBladeHangs() {
    for (boolean framed : new boolean[]{false, true}) {
      for (StreetSignMount mount : new StreetSignMount[]{StreetSignMount.HANGING,
          StreetSignMount.HANGING_BRACKET}) {
        StreetSignData data = bigAlone(mount, framed);
        Layout single = renderer.computeLayout(data);
        data.setLowerBlade(bigLegend());
        Layout top = renderer.computeLayout(data);

        // The hardware grips the top blade, so neither it nor the blade moves.
        assertEquals(single.signTop, top.signTop, EPS);
        assertEquals(single.assemblyTop, top.assemblyTop, EPS);

        Layout lower = top.lower;
        float upperBottomEdge = top.signBottom - edge(top);
        float lowerTopEdge = lower.signTop + edge(lower);
        assertEquals(TileEntityDynamicStreetSignRenderer.BLADE_GAP,
            upperBottomEdge - lowerTopEdge, EPS);
        assertEquals(lower.signBottom - edge(lower), top.assemblyBottom, EPS);
        assertEquals(top.faceZ, lower.faceZ, EPS);
        assertEquals(top.coreBack, lower.coreBack, EPS);
      }
    }
  }

  @Test
  void flatStackCentersAsAWholeOnTheBlock() {
    for (boolean framed : new boolean[]{false, true}) {
      StreetSignData data = sign(StreetSignMount.FLAT, framed);
      data.setLowerBlade(bigLegend());
      Layout top = renderer.computeLayout(data);
      Layout lower = top.lower;

      assertEquals(8.0f, (top.assemblyTop + top.assemblyBottom) / 2.0f, EPS);
      assertEquals(top.signTop + edge(top), top.assemblyTop, EPS);
      assertEquals(TileEntityDynamicStreetSignRenderer.BLADE_GAP,
          (top.signBottom - edge(top)) - (lower.signTop + edge(lower)), EPS);
      // Each blade's legend is centered in its own panel.
      assertEquals((lower.signTop + lower.signBottom) / 2.0f, lower.contentCenterY, EPS);
    }
  }

  @Test
  void previewBoxCoversBothBlades() {
    StreetSignData data = sign(StreetSignMount.HANGING, true);
    data.setLowerBlade(bigLegend());
    Layout top = renderer.computeLayout(data);
    float[] box = renderer.computePreviewBox(data);
    float boxBottom = box[1] - box[3] / 2.0f;
    assertEquals(top.lower.signBottom - edge(top.lower), boxBottom, EPS);
  }
}
