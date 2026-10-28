package com.micatechnologies.minecraft.csm.hvac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The exhaust book-keeping of #314: grilles share their fans, a room drawing out more than it is
 * supplied reads negative, and the make-up air of the shortfall costs heat in the region it is
 * drawn from, and only there, and only while it lasts.
 */
class HvacAirBalanceTest {

  @Test
  void grillesShareTheFansUpToTheirSize() {
    assertEquals(500f, HvacAirBalance.grilleCfm(10000f, 4), 1e-3);
    assertEquals(300f, HvacAirBalance.grilleCfm(1200f, 4), 1e-3);
    assertEquals(0f, HvacAirBalance.grilleCfm(0f, 4), 1e-3);
    assertEquals(0f, HvacAirBalance.grilleCfm(10000f, 0), 1e-3);
  }

  @Test
  void pressureFollowsTheBalanceWithANeutralBand() {
    assertEquals(HvacAirBalance.NEGATIVE, HvacAirBalance.pressure(300f, 500f));
    assertEquals(HvacAirBalance.POSITIVE, HvacAirBalance.pressure(600f, 500f));
    assertEquals(HvacAirBalance.NEUTRAL, HvacAirBalance.pressure(500f, 510f));
    // the band grows with the flows: 5% of 10,000
    assertEquals(HvacAirBalance.NEUTRAL, HvacAirBalance.pressure(10000f, 10400f));
    assertEquals(HvacAirBalance.NEGATIVE, HvacAirBalance.pressure(10000f, 10600f));
    assertEquals(HvacAirBalance.NEUTRAL, HvacAirBalance.pressure(0f, 0f));
  }

  @Test
  void onlyAShortfallDrawsInOutdoorAir() {
    assertEquals(0f, HvacAirBalance.infiltrationUA(600f, 500f), 1e-6);
    float ua = HvacAirBalance.infiltrationUA(300f, 800f);
    assertEquals(500f / HvacAirBalance.CFM_PER_BLOCK_PER_SECOND, ua, 1e-6);
    assertTrue(HvacAirBalance.describe(300f, 800f).contains("negative pressure (-500 CFM)"));
    assertTrue(HvacAirBalance.describe(600f, 500f).contains("positive pressure (+100 CFM)"));
  }

  private static ThermalSpace room(int outdoor) {
    ThermalModelTest.Grid g = new ThermalModelTest.Grid();
    g.box(0, 0, 0, 11, 5, 11); // 10 x 4 x 10, one region
    ThermalScanner.Result r = ThermalScanner.scan(g, 5, 2, 5);
    ThermalSpace s = new ThermalSpace(1, r, ThermalScanner.pack(5, 2, 5), 0);
    s.outdoor = outdoor;
    java.util.Arrays.fill(s.temperature, 70);
    return s;
  }

  @Test
  void aNegativeRoomLosesMoreHeatUntilTheExhaustStops() {
    ThermalSpace s = room(-50);
    s.prepareLoss();
    float sealed = s.lossAt(0, 70);
    float envelope = s.envelopeUA();

    s.addSupply(HvacAirBalance.VENT_CFM);
    s.addExhaust(0, 800f);
    s.finishAirflow();
    assertEquals(300f, s.lastSupplyCfm, 1e-3);
    assertEquals(800f, s.lastExhaustCfm, 1e-3);
    float extra = 500f / HvacAirBalance.CFM_PER_BLOCK_PER_SECOND;
    assertEquals(envelope + extra, s.envelopeUA(), 1e-4);
    s.prepareLoss();
    assertEquals(sealed + extra * 120f, s.lossAt(0, 70), 1e-2);

    // a balanced step: the make-up air is gone again, and so are its arrays
    s.addSupply(800f);
    s.addExhaust(0, 800f);
    s.finishAirflow();
    assertNull(s.infiltrationUA);
    s.finishAirflow(); // and a step with no air at all
    assertNull(s.exhaustRegionCfm);
    assertEquals(0f, s.lastExhaustCfm, 1e-6);
    assertEquals(envelope, s.envelopeUA(), 1e-6);
  }

  @Test
  void aFrozenRoomKeepsItsAirflow() {
    ThermalSpace s = room(-50);
    s.addExhaust(0, 500f);
    s.finishAirflow();
    s.frozen = true;
    s.finishAirflow();
    assertEquals(500f, s.lastExhaustCfm, 1e-3);
    assertTrue(s.infiltrationUA != null && s.infiltrationUA[0] > 0);
  }

  @Test
  void aRoomOnlySuppliedHasNoArraysAtAll() {
    ThermalSpace s = room(-50);
    s.addSupply(600f);
    s.finishAirflow();
    assertNull(s.exhaustRegionCfm);
    assertNull(s.infiltrationUA);
    assertEquals(HvacAirBalance.POSITIVE,
        HvacAirBalance.pressure(s.lastSupplyCfm, s.lastExhaustCfm));
  }
}
