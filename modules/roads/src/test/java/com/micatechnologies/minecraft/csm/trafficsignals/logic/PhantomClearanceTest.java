package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Phantom clearances on the Alto Springs Main St corridor's real plans ({@link
 * AltoCorridorPlans}): a phase that cleared (green, yellow, red) and then went green again in its
 * ring with nothing served in either ring in between. At Main x Church the coordinated throughs 2
 * and 6 reached their yield point with a side-street call standing whose window had not opened
 * yet. The yield is decided on that raw call, but phase next committed only accepted calls, so
 * when the call dropped during the clearance the rings came straight back to 2 and 6.
 */
class PhantomClearanceTest {

  /** One run's phantom clearances, as "phase@tick" strings. */
  static List<String> phantoms(TrafficSignalProgrammedPhasePlan plan, long seed, long from,
      long end) {
    int nc = AltoCorridorPlans.circuitCount(plan);
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(nc);
    Random rnd = new Random(seed);
    boolean[][] z = new boolean[nc][4];
    RingBarrierState rb = null;
    List<String> out = new ArrayList<>();
    int[] green = new int[3];
    int[] cleared = new int[3];       // the phase each ring last cleared, until something greens
    boolean servedSince = false;      // anything new went green in either ring since a clearance
    for (long t = 0; t < end; t += 2) {
      for (int c = 0; c < nc; c++) {
        for (int k = 0; k < 4; k++) {
          if (rnd.nextInt(k == 1 ? 150 : 300) == 0) {
            z[c][k] = !z[c][k];
          }
        }
      }
      if (t == from) {
        rb = new RingBarrierState();
      }
      if (rb == null) {
        continue;
      }
      RingBarrierStateTest.Demand d = new RingBarrierStateTest.Demand();
      for (int c = 0; c < nc; c++) {
        d.veh(c, z[c][0] ? 1 : 0, z[c][1] ? 1 : 0, z[c][2] ? 1 : 0);
        if (z[c][3]) {
          d.ped(c);
        }
      }
      rb.tick(plan, ckts, RingBarrierStateCoordinationTest.NO_OVERLAPS, t, d);
      for (int ring = 1; ring <= 2; ring++) {
        ServedMovement m = rb.getLastServed(ring);
        int g = m != null && m.vehicle == VehInterval.GREEN ? m.phaseNumber : 0;
        if (g == green[ring]) {
          continue;
        }
        if (green[ring] != 0 && g == 0) {
          cleared[ring] = green[ring];
          servedSince = false;
        }
        if (g != 0) {
          if (g == cleared[ring] && !servedSince) {
            out.add(g + "@" + t);
          }
          if (g != cleared[ring]) {
            servedSince = true;
          }
          cleared[ring] = 0;
        }
        green[ring] = g;
      }
    }
    return out;
  }

  @Test
  @DisplayName("Main x Church: the coordinated throughs never clear for a side-street call that "
      + "drops during the clearance and come straight back")
  void churchServesTheCallItYieldedFor() throws Exception {
    TrafficSignalProgrammedPhasePlan church =
        AltoCorridorPlans.plan(AltoCorridorPlans.after("church"));
    // The traced case: 2 and 6 yellow at t=5380, red 5460 and green again at 5500.
    List<String> p = phantoms(church, 4804L, 4804L, 4804L + 2400L * 4);
    assertTrue(p.isEmpty(), "phantom clearances at Church: " + p);
  }

  @Test
  @DisplayName("every Main St plan, many cycles of seeded random demand: no clearance serves "
      + "nothing")
  void corridorSweep() throws Exception {
    Map<String, Integer> counts = new LinkedHashMap<>();
    int total = 0;
    for (String j : AltoCorridorPlans.JUNCTIONS) {
      TrafficSignalProgrammedPhasePlan plan = AltoCorridorPlans.plan(AltoCorridorPlans.after(j));
      int n = 0;
      for (long seed = 1; seed <= 6; seed++) {
        n += phantoms(plan, seed * 7919L, 0L, 2400L * 20).size();
      }
      counts.put(j, n);
      total += n;
    }
    System.out.println("phantom clearances per junction: " + counts);
    assertEquals(0, total, "phantom clearances per junction: " + counts);
  }

  @Test
  @DisplayName("Main x Keough's current plan has both rings meeting at every barrier")
  void keoughBarriersMeet() throws Exception {
    // Ring 2's barrier A is 5 (320) + 6 (960) = 1280, as ring 1's 1 + 2. An earlier advisory
    // that it was 960 came from the phase list before phase 5 was enabled.
    TrafficSignalProgrammedPhasePlan plan =
        AltoCorridorPlans.plan(AltoCorridorPlans.after("keough"));
    RingBarrierState rb = new RingBarrierState();
    rb.tick(plan, RingBarrierStateTest.circuits(AltoCorridorPlans.circuitCount(plan)),
        RingBarrierStateCoordinationTest.NO_OVERLAPS, 0L, new RingBarrierStateTest.Demand());
    assertNull(rb.findBarrierMisalignment(plan));
  }
}
