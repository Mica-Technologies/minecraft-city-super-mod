package com.micatechnologies.minecraft.csm.trafficsigns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.codeutils.SignShift;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Holds the mile marker's legend to what its plate can carry: numbers clamped to the plate's
 * digits, the legend laid out on the plate and in front of its art, and a crafted value never
 * producing a piece outside the sign.
 */
class MileMarkerLayoutTest {

  @Test
  void numbersAreClampedToThePlate() {
    assertEquals("9", MileMarkerLegend.of(MileMarkerLayout.D10_1, 42, 0, null, "", 0).getMile());
    assertEquals("10", MileMarkerLegend.of(MileMarkerLayout.D10_2, 3, 0, null, "", 0).getMile());
    assertEquals("999",
        MileMarkerLegend.of(MileMarkerLayout.D10_3, 123456, 0, null, "", 0).getMile());
    assertEquals("7", MileMarkerLegend.of(MileMarkerLayout.D10_4, 7, 0, null, "", 0).getMile());
    assertEquals(".9",
        MileMarkerLegend.of(MileMarkerLayout.D10_1A, 5, 42, null, "", 0).getTenth());
    assertEquals("", MileMarkerLegend.of(MileMarkerLayout.D10_1, 5, 4, null, "", 0).getTenth());
    MileMarkerLegend enhanced =
        MileMarkerLegend.of(MileMarkerLayout.D10_5, 216, 2, null, "1a2b34", 9);
    assertEquals("123", enhanced.getRoute());
    assertEquals(3, enhanced.getDirection());
    assertEquals(GuideSignShieldType.US_ROUTE, enhanced.getShield());
  }

  @Test
  void everyPieceIsOnTheSignAndInFrontOfItsArt() {
    for (MileMarkerLayout layout : MileMarkerLayout.values()) {
      for (SignShift shift : SignShift.values()) {
        MileMarkerLegend legend = MileMarkerLegend.of(layout, 888, 8,
            GuideSignShieldType.INTERSTATE, "888", 1);
        List<MileMarkerFaces.Piece> pieces = MileMarkerFaces.pieces(layout, legend, shift);
        int expected = (layout.isStacked() ? layout.getDigits() : 3)
            + (layout.hasTenth() ? 2 : 0) + (layout.isEnhanced() ? 1 + 1 + 3 : 0);
        assertEquals(expected, pieces.size(), layout + " " + shift);
        for (MileMarkerFaces.Piece p : pieces) {
          assertTrue(p.x0 >= 0 && p.x1 <= 16 && p.x0 < p.x1, layout + " x " + p.x0 + ".." + p.x1);
          assertTrue(p.y0 >= -16 && p.y1 <= 32 && p.y0 < p.y1, layout + " y " + p.y0);
          assertTrue(p.z <= MileMarkerFaces.artZ(shift) - MileMarkerFaces.PROUD + 1e-4,
              layout + " z " + p.z);
        }
      }
    }
  }
}
