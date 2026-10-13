package com.micatechnologies.minecraft.csm.trafficsignals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSignal.PedestrianClearance;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import org.junit.jupiter.api.Test;

/** What a European red/green pedestrian head shows through the pedestrian clearance. */
class PedestrianClearanceTest {

  @Test
  void steadyRedLightsTheRedFigureAndFlashesNothing() {
    PedestrianClearance c = PedestrianClearance.STEADY_RED;
    assertTrue(c.isClearance(1));
    assertFalse(c.isClearance(0));
    assertFalse(c.isClearance(2));
    assertTrue(c.lights(TrafficSignalBulbColor.RED));
    assertFalse(c.lights(TrafficSignalBulbColor.GREEN));
    assertFalse(c.flashes(1, TrafficSignalBulbColor.RED));
    assertFalse(c.flashes(1, TrafficSignalBulbColor.GREEN));
  }

  @Test
  void flashingGreenFlashesTheGreenFigureOnlyInTheClearance() {
    PedestrianClearance c = PedestrianClearance.FLASHING_GREEN;
    assertTrue(c.lights(TrafficSignalBulbColor.GREEN));
    assertFalse(c.lights(TrafficSignalBulbColor.RED));
    assertTrue(c.flashes(1, TrafficSignalBulbColor.GREEN));
    assertFalse(c.flashes(2, TrafficSignalBulbColor.GREEN), "walk is steady");
    assertFalse(c.flashes(1, TrafficSignalBulbColor.RED));
  }
}
