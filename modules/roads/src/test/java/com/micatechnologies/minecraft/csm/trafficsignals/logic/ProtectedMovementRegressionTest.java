package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.CRC32;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression pins for ADVANCED plans with no {@code PROTECTED} (bus/train/bike lane) phase: the
 * protected-lane movement must leave every existing plan exactly as it was. Each scenario runs the
 * ring-and-barrier engine for several minutes of game time against a fixed, seeded demand script
 * (through, left, right, protected-lane and pedestrian calls on every circuit) and records, every
 * tick, what each ring serves and the colour of every head. The trace's CRC was captured from the
 * engine before the protected-lane movement existed; a different CRC means some head showed a
 * different colour at some tick, or a phase was called or served differently.
 */
class ProtectedMovementRegressionTest {

  private static final long TICKS = 12_000L;

  // Head offsets within a circuit's block of positions.
  private static final int THROUGH = 1;
  private static final int LEFT = 2;
  private static final int RIGHT = 3;
  private static final int PROTECTED = 4;
  private static final int FLASHING_LEFT = 5;
  private static final int PED = 6;

  private static BlockPos head(int circuit, int kind) {
    return new BlockPos(circuit * 10 + kind, 64, 0);
  }

  /** {@code n} circuits each with through, left, right, protected and ped heads; FYA lenses opt. */
  private static TrafficSignalControllerCircuits circuits(int n, boolean fya) {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
    for (int c = 0; c < n; c++) {
      TrafficSignalControllerCircuit circuit = new TrafficSignalControllerCircuit();
      circuit.getThroughSignals().add(head(c, THROUGH));
      circuit.getLeftSignals().add(head(c, LEFT));
      circuit.getRightSignals().add(head(c, RIGHT));
      circuit.getProtectedSignals().add(head(c, PROTECTED));
      if (fya) {
        circuit.getFlashingLeftSignals().add(head(c, FLASHING_LEFT));
      }
      circuit.getPedestrianSignals().add(head(c, PED));
      circuits.addCircuit(circuit);
    }
    return circuits;
  }

  /**
   * The standard 8-phase dual ring: NEMA approaches 1+6 on circuit 0, 5+2 on circuit 1, 3+8 on
   * circuit 2 and 7+4 on circuit 3, the odd phases protected lefts (the default plan's movement).
   * With {@code fya}, each left is a PPLT left permissive behind its opposing through.
   */
  private static TrafficSignalProgrammedPhasePlan eightPhase(boolean fya) {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    int[] circuitOf = {0, 0, 1, 2, 3, 1, 0, 3, 2}; // index = phase number
    for (int n = 1; n <= 8; n++) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      p.setCircuitIndex(circuitOf[n]);
      p.setEnabled(true);
      p.setMinGreen(60L);
      p.setPassage(30L);
      p.setMaxGreen(400L);
      p.setYellow(60L);
      p.setRedClear(20L);
    }
    for (int n : new int[] {2, 4, 6, 8}) {
      plan.getPhase(n).setWalk(80L);
      plan.getPhase(n).setPedClear(100L);
    }
    if (fya) {
      plan.getPhase(1).setPermissivePhase(2);
      plan.getPhase(5).setPermissivePhase(6);
      plan.getPhase(3).setPermissivePhase(4);
      plan.getPhase(7).setPermissivePhase(8);
    }
    return plan;
  }

  /** Canned demand that changes every 150 ticks, from a fixed seed, on every circuit. */
  private static final class ScriptedDemand implements RingBarrierState.DemandSource {

    private final Map<Long, TrafficSignalSensorSummary[]> windows = new HashMap<>();
    private final Map<Long, boolean[]> pedWindows = new HashMap<>();
    private final int circuitCount;
    private final long seed;
    long now;

    ScriptedDemand(int circuitCount, long seed) {
      this.circuitCount = circuitCount;
      this.seed = seed;
    }

    private void fill(long window) {
      if (windows.containsKey(window)) {
        return;
      }
      Random random = new Random(seed * 1_000_003L + window);
      TrafficSignalSensorSummary[] summaries = new TrafficSignalSensorSummary[circuitCount];
      boolean[] peds = new boolean[circuitCount];
      for (int c = 0; c < circuitCount; c++) {
        int through = random.nextInt(3) == 0 ? random.nextInt(3) : 0;
        int left = random.nextInt(4) == 0 ? 1 : 0;
        int right = random.nextInt(5) == 0 ? 1 : 0;
        int prot = random.nextInt(5) == 0 ? 1 : 0;
        summaries[c] = new TrafficSignalSensorSummary(through, 0, 0, 0, 0, left, 0, 0, 0, 0,
            prot, 0, 0, 0, 0, right, 0, 0, 0, 0);
        peds[c] = random.nextInt(7) == 0;
      }
      windows.put(window, summaries);
      pedWindows.put(window, peds);
    }

    @Override
    public TrafficSignalSensorSummary summaryForCircuit(int ci) {
      long window = now / 150L;
      fill(window);
      return ci >= 0 && ci < circuitCount ? windows.get(window)[ci] : null;
    }

    @Override
    public boolean pedestrianRequest(int ci) {
      long window = now / 150L;
      fill(window);
      return ci >= 0 && ci < circuitCount && pedWindows.get(window)[ci];
    }
  }

  /** One character per indication a head can show in a built phase. */
  private static char colour(TrafficSignalPhase phase, BlockPos pos) {
    if (phase.getFlashingGreenSignals().contains(pos)) {
      return 'g';
    }
    if (phase.getGreenSignals().contains(pos)) {
      return 'G';
    }
    if (phase.getYellowSignals().contains(pos)) {
      return 'Y';
    }
    if (phase.getFyaSignals().contains(pos)) {
      return 'F';
    }
    if (phase.getRedAmberSignals().contains(pos)) {
      return 'A';
    }
    if (phase.getRedSignals().contains(pos)) {
      return 'R';
    }
    if (phase.getWalkSignals().contains(pos)) {
      return 'W';
    }
    if (phase.getFlashDontWalkSignals().contains(pos)) {
      return 'f';
    }
    if (phase.getDontWalkSignals().contains(pos)) {
      return 'D';
    }
    if (phase.getOffSignals().contains(pos)) {
      return 'O';
    }
    return '-';
  }

  private static String served(ServedMovement m) {
    return m == null ? "--" : m.phaseNumber + "" + m.vehicle + m.pedestrian;
  }

  /** The run's trace: every tick at which a ring's service or any head's colour changed. */
  private static String trace(TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits, int circuitCount, long seed) {
    RingBarrierState rb = new RingBarrierState();
    ScriptedDemand demand = new ScriptedDemand(circuitCount, seed);
    List<BlockPos> heads = new ArrayList<>();
    for (int c = 0; c < circuits.getCircuitCount(); c++) {
      for (int kind = THROUGH; kind <= PED; kind++) {
        heads.add(head(c, kind));
      }
    }
    StringBuilder out = new StringBuilder();
    String last = null;
    for (long t = 0; t < TICKS; t++) {
      demand.now = t;
      rb.tick(plan, circuits, RingBarrierStateTest.NO_OVERLAPS, t, demand);
      TrafficSignalPhase phase = rb.getLastAppliedPhase();
      StringBuilder line = new StringBuilder();
      line.append(served(rb.getLastServed(1))).append('/').append(served(rb.getLastServed(2)))
          .append(' ');
      for (BlockPos pos : heads) {
        line.append(phase == null ? '?' : colour(phase, pos));
      }
      String now = line.toString();
      if (!now.equals(last)) {
        out.append(t).append(' ').append(now).append('\n');
        last = now;
      }
    }
    return out.toString();
  }

  private static long crc(String trace) {
    CRC32 crc = new CRC32();
    crc.update(trace.getBytes(StandardCharsets.UTF_8));
    return crc.getValue();
  }

  private static void assertTrace(long expectedCrc, String trace) {
    int lines = trace.split("\n").length;
    assertTrue(lines > 200, "the script should exercise many intervals, got " + lines);
    assertEquals(expectedCrc, crc(trace),
        "head colours or service changed against the pinned behaviour (" + lines + " changes)");
  }

  // Captured from the engine before the PROTECTED movement was added.
  private static final long STANDARD_CRC = 2225424002L;
  private static final long FYA_CRC = 3334284004L;
  private static final long OVERLAP_CRC = 1205161887L;

  @Test
  @DisplayName("ordinals 0-4 still decode to the same movements")
  void ordinalsUnchanged() {
    assertEquals(TrafficSignalPhaseMovement.THROUGH, TrafficSignalPhaseMovement.fromNBT(0));
    assertEquals(TrafficSignalPhaseMovement.LEFT, TrafficSignalPhaseMovement.fromNBT(1));
    assertEquals(TrafficSignalPhaseMovement.PROTECTED_LEFT, TrafficSignalPhaseMovement.fromNBT(2));
    assertEquals(TrafficSignalPhaseMovement.RIGHT, TrafficSignalPhaseMovement.fromNBT(3));
    assertEquals(TrafficSignalPhaseMovement.PED, TrafficSignalPhaseMovement.fromNBT(4));
    assertEquals(TrafficSignalPhaseMovement.THROUGH, TrafficSignalPhaseMovement.fromNBT(-1));
    assertEquals(TrafficSignalPhaseMovement.THROUGH, TrafficSignalPhaseMovement.fromNBT(99));
    for (int i = 0; i <= 4; i++) {
      assertEquals(i, TrafficSignalPhaseMovement.fromNBT(i).toNBT());
    }
  }

  @Test
  @DisplayName("standard 8-phase dual ring with protected lefts: pinned head colours")
  void standardEightPhase() {
    assertTrace(STANDARD_CRC, trace(eightPhase(false), circuits(4, false), 4, 7L));
  }

  @Test
  @DisplayName("PPLT/FYA lefts: pinned head colours")
  void ppltFya() {
    String trace = trace(eightPhase(true), circuits(4, true), 4, 11L);
    boolean flashed = false;
    for (String line : trace.split("\n")) {
      flashed |= line.substring(line.lastIndexOf(' ') + 1).indexOf('F') >= 0;
    }
    assertTrue(flashed, "the script should show a flashing yellow arrow somewhere");
    assertTrace(FYA_CRC, trace);
  }

  @Test
  @DisplayName("overlaps with LEFT, THROUGH, RIGHT and PED outputs: pinned head colours")
  void overlaps() {
    TrafficSignalProgrammedPhasePlan plan = eightPhase(false);
    // Circuit 4 carries only overlap outputs, each called from its own zone through callPhase.
    TrafficSignalProgrammedOverlap right = overlap(TrafficSignalPhaseMovement.RIGHT,
        new int[] {2, 3}, 2);
    TrafficSignalProgrammedOverlap left = overlap(TrafficSignalPhaseMovement.LEFT,
        new int[] {4}, 4);
    TrafficSignalProgrammedOverlap through = overlap(TrafficSignalPhaseMovement.THROUGH,
        new int[] {8}, 8);
    TrafficSignalProgrammedOverlap ped = overlap(TrafficSignalPhaseMovement.PED,
        new int[] {6}, 6);
    plan.getVehicleOverlaps().add(right);
    plan.getVehicleOverlaps().add(left);
    plan.getVehicleOverlaps().add(through);
    plan.getVehicleOverlaps().add(ped);
    assertTrace(OVERLAP_CRC, trace(plan, circuits(5, false), 5, 13L));
  }

  private static TrafficSignalProgrammedOverlap overlap(TrafficSignalPhaseMovement movement,
      int[] included, int callPhase) {
    TrafficSignalProgrammedOverlap ov = new TrafficSignalProgrammedOverlap();
    ov.setEnabled(true);
    ov.setOutputCircuitIndex(4);
    ov.setOutputMovement(movement);
    ov.setIncludedPhases(included);
    ov.setCallPhase(callPhase);
    return ov;
  }

  /** A demand of nothing but one protected-lane vehicle on the given circuit. */
  private static RingBarrierStateTest.Demand protectedOnly(int circuitCount, int circuit) {
    RingBarrierStateTest.Demand d = new RingBarrierStateTest.Demand();
    for (int c = 0; c < circuitCount; c++) {
      d.veh(c, 0, 0, 0, c == circuit ? 1 : 0);
    }
    return d;
  }

  @Test
  @DisplayName("without a PROTECTED phase, the protected zone calls the left phase, which lights "
      + "the protected heads")
  void protectedZoneCallsTheLeftWithoutAProtectedPhase() {
    TrafficSignalProgrammedPhasePlan plan = eightPhase(false);
    TrafficSignalControllerCircuits ckts = circuits(4, false);
    RingBarrierState rb = new RingBarrierState();
    RingBarrierStateTest.Demand d = protectedOnly(4, 0); // circuit 0: left phase 1
    boolean leftServed = false;
    for (long t = 0; t < 600L && !leftServed; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, d);
      ServedMovement m = rb.getLastServed(1);
      if (m != null && m.phaseNumber == 1 && m.vehicle == RingBarrierState.VehInterval.GREEN) {
        leftServed = true;
        TrafficSignalPhase phase = rb.getLastAppliedPhase();
        assertTrue(phase.getGreenSignals().contains(head(0, PROTECTED)),
            "the left phase lights its circuit's protected heads");
        assertTrue(phase.getGreenSignals().contains(head(0, LEFT)));
      }
    }
    assertTrue(leftServed, "a protected-lane vehicle calls the left phase on its circuit");
  }
}
