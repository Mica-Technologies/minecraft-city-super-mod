package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierStateTest.Demand;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Replays a real junction's ADVANCED program against the engine: the UWS E-W 4 x Mission Blvd /
 * Alto Parkway controller as read from the live server on 2026-11-16
 * ({@code src/test/resources/alto/uws_we4_controller_20261116.snbt}, its {@code tcAdv} and
 * {@code tcCrc}).
 *
 * <p>The junction is lead-lag in both barriers: ring 1 [1 EBL, 2 WB, 3 NBL, 4 SB], ring 2
 * [6 EB, 5 WBL, 8 NB, 7 SBL], every phase on MAX recall, coordinated on a 90 s cycle (offset 803,
 * 2 and 6 coordinated). The median bike heads (circuit 8's protected list) are a -GRN/YEL overlap,
 * green with 8 except while the NB left arrow 3 (circuit 6) is green or yellow. The captured
 * program carries two workarounds for bugs in the build that was live; this test puts each back:
 * <ul>
 *   <li>exclusions 1:[5] and 3:[7] (a safety net under the lead-lag order) skipped phase 8, and
 *   once 3, for whole cycles under coordination (filed as issue #361). In this replay the lead-lag
 *   order keeps the excluded windows apart, so the #361 window spacing changes nothing; the skips
 *   came from coordinated phase 6 recycling its walk past its yield point, which the engine no
 *   longer does;</li>
 *   <li>the NB preempt's dwell [3, 8] showed the bike heads green beside the NB left arrow
 *   (issue #363); the live program dwells [8] only.</li>
 * </ul>
 */
class UwsWe4ReplayTest {

  private static final long CYCLE = 1800L;
  private static final long OFFSET = 803L;
  /** Circuit of the NB left arrow (phase 3). */
  private static final int ARROW_CIRCUIT = 6;
  /** Output circuit of the bike overlap. */
  private static final int BIKE_CIRCUIT = 8;
  /** Trigger circuit of the NB preempt ({@code pe[2]}, detector-triggered). */
  private static final int NB_PREEMPT_CIRCUIT = 5;

  private static NBTTagCompound controller() throws Exception {
    File f = new File("modules/roads/src/test/resources/alto/uws_we4_controller_20261116.snbt");
    if (!f.isFile()) {
      f = new File(System.getProperty("user.dir"), f.getPath());
    }
    return JsonToNBT.getTagFromJson(
        new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim());
  }

  /**
   * The junction's plan. {@code exclusions}: put back {@code ex} 1:[5] and 3:[7];
   * {@code originalNbDwell}: put back the NB preempt's dwell [3, 8].
   */
  private static TrafficSignalProgrammedPhasePlan plan(boolean exclusions,
      boolean originalNbDwell) throws Exception {
    NBTTagCompound adv = controller().getCompoundTag("tcAdv");
    if (exclusions) {
      NBTTagList ph = adv.getTagList("ph", 10);
      for (int i = 0; i < ph.tagCount(); i++) {
        NBTTagCompound p = ph.getCompoundTagAt(i);
        if (p.getInteger("n") == 1) {
          p.setTag("ex", new NBTTagIntArray(new int[] {5}));
        } else if (p.getInteger("n") == 3) {
          p.setTag("ex", new NBTTagIntArray(new int[] {7}));
        }
      }
    }
    if (originalNbDwell) {
      adv.getTagList("pe", 10).getCompoundTagAt(2).setTag("dw", new NBTTagIntArray(new int[] {3, 8}));
    }
    return TrafficSignalProgrammedPhasePlan.fromNBT(adv);
  }

  private static TrafficSignalControllerCircuits circuits() throws Exception {
    return TrafficSignalControllerCircuits.fromNBT(controller().getCompoundTag("tcCrc"));
  }

  private static boolean up(ServedMovement m) {
    return m != null && m.phaseNumber != 0 && m.vehicle != VehInterval.RED;
  }

  private static boolean on(Integer indication) {
    return indication != null && !TrafficSignalPhase.isRedIndication(indication);
  }

  /** Every circuit with a standing vehicle call and, if {@code peds}, a ped call. */
  private static Demand allCalled(boolean peds) {
    Demand d = new Demand();
    for (int c = 0; c < 9; c++) {
      d.veh(c, 2, 2, 1);
      if (peds) {
        d.ped(c);
      }
    }
    return d;
  }

  @Test
  @DisplayName("the captured program and circuits parse as the live controller ran them")
  void programParses() throws Exception {
    TrafficSignalProgrammedPhasePlan plan = plan(true, true);
    TrafficSignalControllerCircuits ckts = circuits();
    assertEquals(9, ckts.getCircuitCount());
    assertEquals(4, ckts.getCircuit(BIKE_CIRCUIT).getProtectedSignals().size());
    assertEquals(1, ckts.getCircuit(ARROW_CIRCUIT).getLeftSignals().size());
    assertArrayEquals(new int[] {1, 2, 3, 4}, plan.getRingSequence(1));
    assertArrayEquals(new int[] {6, 5, 8, 7}, plan.getRingSequence(2));
    TrafficSignalCoordinationPlan co = plan.getCoordination();
    assertEquals(TrafficSignalCoordinationMode.COORDINATED, co.getMode());
    assertEquals(CYCLE, co.getCycleLength());
    assertEquals(OFFSET, co.getOffset());
    assertTrue(plan.excludes(1, 5) && plan.excludes(7, 3));
    assertArrayEquals(new int[] {3, 8}, plan.getPreempts().get(2).getDwellPhases());
    assertTrue(plan.getPreempts().get(2).isTriggerOnDetectors());
    assertEquals(ARROW_CIRCUIT, plan.getPhase(3).getCircuitIndex());
    assertNull(plan.validate(ckts));
    // The captured program itself: no exclusions, NB dwell [8].
    TrafficSignalProgrammedPhasePlan live = plan(false, false);
    assertFalse(live.excludes(1, 5));
    assertArrayEquals(new int[] {8}, live.getPreempts().get(2).getDwellPhases());
  }

  // region (a) exclusions under coordination (issue #361)

  @Test
  @DisplayName("with exclusions 1:[5] and 3:[7] under coordination, recall only: every phase once "
      + "a cycle, the excluded pairs never together (issue #361)")
  void exclusionsRecallOnly() throws Exception {
    runCoordinated(true, new Demand(), "ex, recall only");
  }

  @Test
  @DisplayName("with exclusions under coordination, every vehicle and ped called: every phase "
      + "once a cycle, the excluded pairs never together (issue #361)")
  void exclusionsAllCalled() throws Exception {
    runCoordinated(true, allCalled(true), "ex, all called");
  }

  /**
   * A button held on phase 6's crosswalk (circuit 1), the coordinated phase in ring 2: with ped
   * recall its walk runs from green start, and the press asks for a second walk once the first
   * clearance ends at local 300. Only 220 ticks remained before 6's yield point (local 520), so
   * the recycled walk (100 + 200) held 6 green to 600: 5 went green 80 ticks late, cleared 80
   * ticks into barrier B, and the overrun came out of 3 and 8.
   */
  @Test
  @DisplayName("with exclusions under coordination, a button held on the coordinated phase's "
      + "crosswalk: no walk is recycled past its yield point, every phase starts on time")
  void exclusionsCoordinatedPhaseButton() throws Exception {
    runCoordinated(true, new Demand().ped(1), "ex, phase 6 button");
  }

  /**
   * The live run that skipped phase 8 began right after the program was written, which restarts
   * the engine: so start it cold at every 20 ticks of the cycle, recall only and everything called,
   * and look at each of the four full cycles after the start.
   *
   * <p>Barrier B (3, 4, 7, 8, the phases the live run skipped) is served in every one of them.
   * Barrier A's lefts are not always: phases 1 and 5 have splits of 240 = min green 100 + yellow
   * 80 + red 60, so their permissive period is a single tick at the window start, and a ring that
   * reaches them late in the transition cycle passes them by. With the exclusions, a cold start in
   * barrier B (local 920 to 1480, everything called) also costs phase 1 the next cycle: the
   * transition serves 5 late, behind 6, the overrun pushes 4 and 7 past the cycle wrap, and 1's
   * one-tick period has gone. From the second full cycle every phase is served.</p>
   */
  @Test
  @DisplayName("with exclusions under coordination, a cold start anywhere in the cycle: barrier B "
      + "served every cycle, every phase from the second full cycle (issue #361)")
  void exclusionsColdStartAnywhere() throws Exception {
    TrafficSignalProgrammedPhasePlan plan = plan(true, false);
    TrafficSignalControllerCircuits ckts = circuits();
    int fullCycles = 4;
    int[][] missed = new int[fullCycles][9];
    StringBuilder misses = new StringBuilder();
    int runs = 0;
    for (long startLocal = 0; startLocal < CYCLE; startLocal += 20L) {
      for (Demand demand : new Demand[] {new Demand(), allCalled(true)}) {
        runs++;
        long t0 = 10 * CYCLE + OFFSET + startLocal;
        long firstWrap = t0 + (CYCLE - startLocal);
        boolean[][] served = new boolean[fullCycles][9];
        RingBarrierState rb = new RingBarrierState();
        for (long t = t0; t < firstWrap + fullCycles * CYCLE; t++) {
          rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
          ServedMovement m1 = rb.getLastServed(1);
          ServedMovement m2 = rb.getLastServed(2);
          if (up(m1) && up(m2)) {
            assertFalse(plan.excludes(m1.phaseNumber, m2.phaseNumber), "start " + startLocal
                + ": excluded " + m1.phaseNumber + "+" + m2.phaseNumber + " at t=" + t);
          }
          int c = (int) Math.floorDiv(t - firstWrap, CYCLE);
          for (int n = 1; n <= 8 && c >= 0; n++) {
            if ((m1 != null && m1.phaseNumber == n && m1.vehicle == VehInterval.GREEN)
                || (m2 != null && m2.phaseNumber == n && m2.vehicle == VehInterval.GREEN)) {
              served[c][n] = true;
            }
          }
        }
        for (int c = 0; c < fullCycles; c++) {
          for (int n = 1; n <= 8; n++) {
            if (!served[c][n]) {
              missed[c][n]++;
              if (c >= 2 || n == 3 || n == 4 || n == 7 || n == 8) {
                misses.append("\n  cold start at local ").append(startLocal)
                    .append(demand.summaries.isEmpty() ? " (recall only)" : " (all called)")
                    .append(": phase ").append(n).append(" not green in full cycle ").append(c);
              }
            }
          }
        }
      }
    }
    StringBuilder summary = new StringBuilder("cold starts: ").append(runs)
        .append(" runs; runs missing each phase in full cycle 0 / 1 / 2 / 3:");
    for (int n = 1; n <= 8; n++) {
      summary.append("\n  phase ").append(n).append(": ");
      for (int c = 0; c < fullCycles; c++) {
        summary.append(c == 0 ? "" : " / ").append(missed[c][n]);
      }
    }
    System.out.println(summary);
    assertEquals(0, misses.length(), "after a cold start:" + misses + "\n" + summary);
  }

  @Test
  @DisplayName("control: the captured program (no exclusions) serves every phase once a cycle")
  void capturedProgramAllCalled() throws Exception {
    runCoordinated(false, allCalled(true), "no ex, all called");
  }

  /**
   * Each phase's window start (local cycle) from the splits: ring 1 1 [0, 240), 2 [240, 900),
   * 3 [900, 1200), 4 [1200, 1800); ring 2 6 [0, 660), 5 [660, 900), 8 [900, 1500),
   * 7 [1500, 1800). Index = phase number.
   */
  private static final long[] WINDOW_START = {0, 0, 240, 900, 1200, 660, 0, 1500, 900};

  private static void runCoordinated(boolean exclusions, Demand demand, String label)
      throws Exception {
    TrafficSignalProgrammedPhasePlan plan = plan(exclusions, false);
    TrafficSignalControllerCircuits ckts = circuits();
    int cycles = 14;
    int warm = 2;   // cold start and the first transition into step
    RingBarrierState rb = new RingBarrierState();
    int[][] starts = new int[cycles][9];
    long[] firstLocal = new long[9];
    long[] lastStart = new long[9];
    long[] maxGap = new long[9];
    long[] maxLate = new long[9];
    boolean[] was = new boolean[9];
    java.util.Arrays.fill(firstLocal, -1L);
    java.util.Arrays.fill(lastStart, -1L);
    String last = "";
    StringBuilder timeline = new StringBuilder();
    long end = OFFSET + CYCLE * cycles;
    for (long t = 0; t < end; t++) {
      TrafficSignalPhase applied = rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
      ServedMovement m1 = rb.getLastServed(1);
      ServedMovement m2 = rb.getLastServed(2);
      String key = (m1 == null ? "idle" : m1.phaseNumber + ":" + m1.vehicle) + " | "
          + (m2 == null ? "idle" : m2.phaseNumber + ":" + m2.vehicle);
      long local = ((t - OFFSET) % CYCLE + CYCLE) % CYCLE;
      int cycle = (int) Math.floorDiv(t - OFFSET, CYCLE);
      if (!key.equals(last) && cycle >= warm && cycle < warm + 3) {
        timeline.append("\n  t=").append(t).append(" local=").append(local).append("  ")
            .append(key);
      }
      last = key;
      boolean up1 = up(m1);
      boolean up2 = up(m2);
      if (up1 && up2) {
        assertFalse(plan.excludes(m1.phaseNumber, m2.phaseNumber), label + ": excluded "
            + m1.phaseNumber + "+" + m2.phaseNumber + " up together at t=" + t + timeline);
        assertEquals(plan.getPhase(m1.phaseNumber).getBarrier(),
            plan.getPhase(m2.phaseNumber).getBarrier(), label + ": " + m1.phaseNumber + "+"
                + m2.phaseNumber + " up across the barrier at t=" + t + timeline);
      }
      checkHeads(plan, ckts, rb.getLastAppliedPhase(), m1, m2, t, label, timeline);
      for (int n = 1; n <= 8; n++) {
        boolean green = (m1 != null && m1.phaseNumber == n && m1.vehicle == VehInterval.GREEN)
            || (m2 != null && m2.phaseNumber == n && m2.vehicle == VehInterval.GREEN);
        if (green && !was[n] && cycle >= warm) {
          starts[cycle][n]++;
          if (firstLocal[n] < 0) {
            firstLocal[n] = local;
          }
          if (lastStart[n] >= 0) {
            maxGap[n] = Math.max(maxGap[n], t - lastStart[n]);
          }
          lastStart[n] = t;
          long late = ((local - WINDOW_START[n]) % CYCLE + CYCLE) % CYCLE;
          maxLate[n] = Math.max(maxLate[n], late);
          // A phase goes green at its window start, or its delayed green (LPI) after it.
          assertTrue(late <= plan.getPhase(n).getDelayedGreen(), label + ": phase " + n
              + " went green at local " + local + ", " + late + " ticks after its window start, t="
              + t + timeline);
        }
        was[n] = green;
      }
    }
    StringBuilder summary = new StringBuilder(label).append(": ").append(cycles - warm)
        .append(" steady cycles");
    for (int n = 1; n <= 8; n++) {
      int total = 0;
      for (int c = warm; c < cycles; c++) {
        total += starts[c][n];
      }
      summary.append("\n  phase ").append(n).append(": ").append(total).append(" starts, first at local ")
          .append(firstLocal[n]).append(", longest start-to-start ").append(maxGap[n])
          .append(" ticks (").append(maxGap[n] / 20.0).append(" s), latest ").append(maxLate[n])
          .append(" after window start");
    }
    System.out.println(summary);
    for (int c = warm; c < cycles; c++) {
      for (int n = 1; n <= 8; n++) {
        assertEquals(1, starts[c][n], label + ": phase " + n + " started " + starts[c][n]
            + " times in cycle " + c + "\n" + summary + timeline);
      }
    }
    for (int n = 1; n <= 8; n++) {
      assertTrue(maxGap[n] <= CYCLE + 60L, label + ": phase " + n + " waited " + maxGap[n]
          + " ticks between starts\n" + summary + timeline);
    }
  }

  /**
   * Head-level monitor for normal running: a lit head of a phase's circuit needs that phase up in
   * a ring, and the bike overlap is never lit beside a lit NB left arrow.
   */
  private static void checkHeads(TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits ckts, TrafficSignalPhase applied, ServedMovement m1,
      ServedMovement m2, long t, String label, CharSequence timeline) {
    if (applied == null) {
      return;
    }
    Map<BlockPos, Integer> states = applied.resolveVehicleSignalStates();
    for (int n = 1; n <= 8; n++) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      boolean phaseUp = (m1 != null && m1.phaseNumber == n && m1.vehicle != VehInterval.RED)
          || (m2 != null && m2.phaseNumber == n && m2.vehicle != VehInterval.RED);
      if (phaseUp) {
        continue;
      }
      TrafficSignalControllerCircuit c = ckts.getCircuit(p.getCircuitIndex());
      List<BlockPos> heads = new ArrayList<>();
      if (p.getMovement() == TrafficSignalPhaseMovement.THROUGH) {
        heads.addAll(c.getThroughSignals());
      } else {
        heads.addAll(c.getLeftSignals());
      }
      for (BlockPos h : heads) {
        assertFalse(on(states.get(h)), label + ": phase " + n + "'s head " + h + " lit ("
            + states.get(h) + ") while the phase is red, t=" + t + timeline);
      }
    }
    assertFalse(anyOn(states, ckts.getCircuit(BIKE_CIRCUIT).getProtectedSignals())
            && anyOn(states, ckts.getCircuit(ARROW_CIRCUIT).getLeftSignals()),
        label + ": bike overlap lit beside the NB left arrow, t=" + t + timeline);
  }

  private static boolean anyOn(Map<BlockPos, Integer> states, List<BlockPos> heads) {
    for (BlockPos h : heads) {
      if (on(states.get(h))) {
        return true;
      }
    }
    return false;
  }

  // endregion

  // region (b) the NB preempt dwell [3, 8] and the -GRN/YEL bike overlap (issue #363)

  @Test
  @DisplayName("NB preempt dwelling [3, 8], entered at every point of the cycle: the bike heads "
      + "are never lit beside the NB left arrow and never skip their yellow (issue #363)")
  void nbPreemptKeepsBikeOverlapOffTheArrow() throws Exception {
    TrafficSignalProgrammedPhasePlan plan = plan(true, true);
    TrafficSignalControllerCircuits ckts = circuits();
    List<BlockPos> bike = ckts.getCircuit(BIKE_CIRCUIT).getProtectedSignals();
    List<BlockPos> arrow = ckts.getCircuit(ARROW_CIRCUIT).getLeftSignals();
    Demand normal = allCalled(false);
    Demand preempt = allCalled(false);
    preempt.summaries.get(NB_PREEMPT_CIRCUIT).withPreemptDetectorCalls(1);
    int entries = 0;
    int dwellsWithArrow = 0;
    int bikeGreenAfter = 0;
    for (long entry = 0; entry < CYCLE; entry += 25L) {
      RingBarrierState rb = new RingBarrierState();
      long start = OFFSET + 2 * CYCLE + entry;
      long release = start + 400L;
      long end = release + 2 * CYCLE;
      TrafficSignalPhase shown = null;
      Map<BlockPos, Integer> prev = null;
      boolean arrowInDwell = false;
      boolean bikeGreenAfterRelease = false;
      boolean bikeGreenBefore = false;
      boolean[] servedAfter = new boolean[9];
      StringBuilder trace = new StringBuilder();
      String lastKey = "";
      for (long t = 0; t < end; t++) {
        boolean calling = t >= start && t < release;
        TrafficSignalPhase p = rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t,
            calling ? preempt : normal);
        if (p != null) {
          shown = p;
        }
        if (shown == null) {
          continue;
        }
        Map<BlockPos, Integer> states = shown.resolveVehicleSignalStates();
        String key = "bike=" + states.get(bike.get(0)) + " arrow=" + states.get(arrow.get(0));
        if (t >= start - 200 && t < release + 400 && !key.equals(lastKey)) {
          trace.append("\n  t=").append(t).append(" (").append(t - start).append(") ")
              .append(key).append(calling ? " [preempt call]" : "");
        }
        lastKey = key;
        boolean bikeOn = anyOn(states, bike);
        boolean arrowOn = anyOn(states, arrow);
        assertFalse(bikeOn && arrowOn, "entry " + entry + ", t=" + t
            + ": bike overlap lit beside the NB left arrow" + trace);
        if (prev != null) {
          for (BlockPos h : bike) {
            Integer was = prev.get(h);
            Integer now = states.get(h);
            if (was != null && TrafficSignalPhase.isGreenIndication(was)) {
              assertFalse(now != null && TrafficSignalPhase.isRedIndication(now), "entry " + entry
                  + ", t=" + t + ": bike head " + h + " went green to red without a yellow"
                  + trace);
            }
          }
        }
        prev = states;
        boolean bikeGreen = bike.stream().anyMatch(
            h -> states.get(h) != null && TrafficSignalPhase.isGreenIndication(states.get(h)));
        if (t < start && bikeGreen) {
          bikeGreenBefore = true;
        }
        if (calling && t >= start + 200 && arrow.stream().anyMatch(
            h -> states.get(h) != null && TrafficSignalPhase.isGreenIndication(states.get(h)))) {
          arrowInDwell = true;
        }
        if (t >= release && bikeGreen) {
          bikeGreenAfterRelease = true;
        }
        if (t >= release) {
          ServedMovement m1 = rb.getLastServed(1);
          ServedMovement m2 = rb.getLastServed(2);
          if (m1 != null && m1.vehicle == VehInterval.GREEN) {
            servedAfter[m1.phaseNumber] = true;
          }
          if (m2 != null && m2.vehicle == VehInterval.GREEN) {
            servedAfter[m2.phaseNumber] = true;
          }
        }
      }
      entries++;
      assertTrue(bikeGreenBefore, "sanity, entry " + entry + ": bike never green before the call");
      if (arrowInDwell) {
        dwellsWithArrow++;
      }
      if (bikeGreenAfterRelease) {
        bikeGreenAfter++;
      }
      for (int n = 1; n <= 8; n++) {
        assertTrue(servedAfter[n], "entry " + entry + ": phase " + n
            + " not served in the two cycles after the preempt" + trace);
      }
    }
    System.out.println("NB preempt dwell [3, 8]: " + entries + " entries, arrow green in the "
        + "dwell in " + dwellsWithArrow + ", bike green again after release in " + bikeGreenAfter);
    assertEquals(entries, dwellsWithArrow, "the dwell brings up the NB left arrow every time");
    assertEquals(entries, bikeGreenAfter, "the bike heads come back with 8 after the preempt");
  }

  // endregion
}
