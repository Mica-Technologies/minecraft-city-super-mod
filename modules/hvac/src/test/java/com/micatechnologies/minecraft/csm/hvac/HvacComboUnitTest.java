package com.micatechnologies.minecraft.csm.hvac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Combination (heat and cool) units: what they run in a system that is heating or cooling, how a
 * system mixing them with single-purpose units adds up its capacity, and how one holds its room
 * standalone in both directions. The standalone law stepped here is the one
 * {@link HvacSystemControl} runs for an unlinked unit.
 */
class HvacComboUnitTest {

  private static final IHvacUnit HEATER = unit(300, true, false);
  private static final IHvacUnit COOLER = unit(300, false, true);
  private static final IHvacUnit COMBO = unit(100, true, true);

  private static IHvacUnit unit(float capacity, boolean heats, boolean cools) {
    return new IHvacUnit() {
      @Override
      public float getHeatCapacity() {
        return capacity;
      }

      @Override
      public boolean isCoolingUnit() {
        return cools && !heats;
      }

      @Override
      public boolean canHeat() {
        return heats;
      }

      @Override
      public boolean canCool() {
        return cools;
      }
    };
  }

  // region The unit classes

  @Test
  void singlePurposeUnitsKeepTheirOneDirection() {
    TileEntityHvacHeater heater = new TileEntityHvacHeater();
    TileEntityHvacCooler cooler = new TileEntityHvacCooler();
    assertTrue(heater.canHeat());
    assertFalse(heater.canCool());
    assertFalse(cooler.canHeat());
    assertTrue(cooler.canCool());
    assertTrue(new TileEntityHvacRtuCooler().canCool());
    assertFalse(new TileEntityHvacRtuCooler().canHeat());
    assertTrue(new TileEntityHvacCrac().canCool());
  }

  @Test
  void aComboUnitHeatsAndCools() {
    TileEntityHvacComboUnit combo = new TileEntityHvacComboUnit();
    assertTrue(combo.canHeat());
    assertTrue(combo.canCool());
    assertFalse(combo.isCoolingUnit(), "not a cooling-only unit");
  }

  // endregion

  // region In a system

  @Test
  void aComboUnitRunsWhenItsSystemHeats() {
    assertTrue(HvacSystemControl.runsIn(COMBO, HvacStatus.MODE_HEATING));
    assertTrue(HvacSystemControl.runsIn(HEATER, HvacStatus.MODE_HEATING));
    assertFalse(HvacSystemControl.runsIn(COOLER, HvacStatus.MODE_HEATING));
  }

  @Test
  void aComboUnitRunsWhenItsSystemCools() {
    assertTrue(HvacSystemControl.runsIn(COMBO, HvacStatus.MODE_COOLING));
    assertTrue(HvacSystemControl.runsIn(COOLER, HvacStatus.MODE_COOLING));
    assertFalse(HvacSystemControl.runsIn(HEATER, HvacStatus.MODE_COOLING));
  }

  @Test
  void nothingRunsWhileItsSystemIdles() {
    for (IHvacUnit u : Arrays.asList(HEATER, COOLER, COMBO)) {
      assertFalse(HvacSystemControl.runsIn(u, HvacStatus.MODE_IDLE));
    }
  }

  @Test
  void aSystemOfComboUnitsAloneCanHeatAndCool() {
    HvacSystemControl.Equipment eq = new HvacSystemControl.Equipment();
    eq.add(COMBO, true);
    eq.add(COMBO, true);
    assertTrue(eq.hasHeater, "heat demand is not dropped");
    assertTrue(eq.hasCooler, "cool demand is not dropped");
    assertEquals(200, eq.heatCap, 1e-3);
    assertEquals(200, eq.coolCap, 1e-3);
    assertEquals(2, eq.powered);
  }

  @Test
  void aSystemMixingComboAndSinglePurposeUnitsPoolsEachWay() {
    HvacSystemControl.Equipment eq = new HvacSystemControl.Equipment();
    eq.add(COOLER, true);
    eq.add(COMBO, true);
    eq.add(HEATER, false); // unpowered: still a heater, but no capacity
    assertTrue(eq.hasHeater);
    assertTrue(eq.hasCooler);
    assertEquals(100, eq.heatCap, 1e-3, "heating: the combo unit only, the heater has no power");
    assertEquals(400, eq.coolCap, 1e-3, "cooling: the cooler and the combo unit");
    assertEquals(2, eq.powered);
  }

  @Test
  void aCoolerWithAComboUnitHeatsOnTheComboUnitAlone() {
    HvacSystemControl.Equipment eq = new HvacSystemControl.Equipment();
    eq.add(COOLER, true);
    eq.add(COMBO, true);
    assertEquals(100, eq.heatCap, 1e-3);
    assertTrue(HvacSystemControl.runsIn(COMBO, HvacStatus.MODE_HEATING));
    assertFalse(HvacSystemControl.runsIn(COOLER, HvacStatus.MODE_HEATING));
  }

  // endregion

  // region Standalone

  private static ThermalSpace room(float outdoor) {
    ThermalModelTest.Grid g = new ThermalModelTest.Grid();
    g.box(0, 0, 0, 7, 5, 7); // inside 6 x 4 x 6, a hotel room
    ThermalScanner.Result r = ThermalScanner.scan(g, 3, 2, 3);
    assertEquals(ThermalScanner.Status.OK, r.status);
    ThermalSpace s = new ThermalSpace(1, r, ThermalScanner.pack(3, 2, 3), 0);
    s.outdoor = outdoor;
    Arrays.fill(s.temperature, outdoor);
    return s;
  }

  /** Steps the room under the standalone law for {@code seconds}; returns the final reading. */
  private static float hold(ThermalSpace s, IHvacUnit unit, int seconds, float[] lastQ) {
    for (int t = 0; t < seconds; t++) {
      s.prepareLoss();
      float q = HvacSystemControl.standaloneDelivery(unit, s, 0);
      lastQ[0] = q;
      lastQ[1] = Math.max(lastQ[1], Math.abs(q));
      s.pendingHeat[0] += q;
      s.integrate(1.0f);
    }
    return s.temperature[0];
  }

  @Test
  void standaloneComboUnitHeatsAColdRoomTo70() {
    ThermalSpace s = room(20);
    float[] q = new float[2];
    float t = hold(s, COMBO, 3600, q);
    assertEquals(HvacSystemControl.STANDALONE_HEAT_TARGET, t, 0.3f);
    assertTrue(q[0] > 0, "heating to hold it");
  }

  @Test
  void standaloneComboUnitCoolsAHotRoomTo74() {
    ThermalSpace s = room(105);
    float[] q = new float[2];
    float t = hold(s, COMBO, 3600, q);
    assertEquals(HvacSystemControl.STANDALONE_COOL_TARGET, t, 0.3f);
    assertTrue(q[0] < 0, "cooling to hold it");
  }

  @Test
  void standaloneComboUnitIdlesBetweenItsTargets() {
    ThermalSpace s = room(72);
    float[] q = new float[2];
    float t = hold(s, COMBO, 600, q);
    assertTrue(t > 70 && t < 74, "drifts with the weather and the ground: " + t);
    assertEquals(0, q[1], 1e-6, "neither heats nor cools between 70 and 74°F");
  }

  @Test
  void singlePurposeUnitsStillOnlyGoOneWay() {
    ThermalSpace hot = room(105);
    float[] q = new float[2];
    hold(hot, HEATER, 60, q);
    assertEquals(0, q[1], 1e-6, "a heater never cools a hot room");
    ThermalSpace cold = room(20);
    hold(cold, COOLER, 60, q);
    assertEquals(0, q[1], 1e-6, "a cooler never heats a cold room");
  }

  @Test
  void lightweightModeRunsAComboUnitInEitherWeather() {
    assertTrue(HvacSystemControl.standaloneWanted(COMBO, 20));
    assertTrue(HvacSystemControl.standaloneWanted(COMBO, 100));
    assertFalse(HvacSystemControl.standaloneWanted(COMBO, 72));
    assertFalse(HvacSystemControl.standaloneWanted(HEATER, 100));
    assertFalse(HvacSystemControl.standaloneWanted(COOLER, 20));
    assertEquals(70, HvacSystemControl.standaloneHold(COMBO, 20), 1e-6);
    assertEquals(74, HvacSystemControl.standaloneHold(COMBO, 100), 1e-6);
    assertEquals(72, HvacSystemControl.standaloneHold(COMBO, 72), 1e-6);
    assertEquals(70, HvacSystemControl.standaloneHold(HEATER, 100), 1e-6);
    assertEquals(74, HvacSystemControl.standaloneHold(COOLER, 20), 1e-6);
  }

  // endregion
}
