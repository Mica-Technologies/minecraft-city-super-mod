package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.PedInterval;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierStateTest.Demand;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The lagging FYA left of two UWS junctions, replayed from their live programs (W2-E4 and RIM-E5,
 * read from the server on 2026-11-17): WB left 5 on circuit 3, permissive phase 6 (EB through),
 * ring 2 [6, 5]. Two display defects were seen live:
 *
 * <ul>
 *   <li>with a leading pedestrian interval (delayed green) on phase 6, the FYA flashed, and then
 *       showed a steady yellow arrow, while EB was still red and the crosswalk the left turns
 *       across was walking: the flash hold ran before phase 6 had ever been green;</li>
 *   <li>when phase 6 went yellow the arrow showed a steady yellow for one engine step, then
 *       flashed again until its protected green: the hold missed the left's call on the tick the
 *       ring committed to it.</li>
 * </ul>
 */
class FyaLeadingPedestrianTest {

  private static final String[] PROGRAMS = {
      "uws_w2e4_controller_20261117.snbt", "uws_rime5_controller_20261117.snbt"};
  /** Circuit of the WB left's FYA head in both programs. */
  private static final int WBL_CIRCUIT = 3;

  private static NBTTagCompound load(String name) throws Exception {
    File f = new File("modules/roads/src/test/resources/alto/" + name);
    if (!f.isFile()) {
      f = new File(System.getProperty("user.dir"), f.getPath());
    }
    return JsonToNBT.getTagFromJson(
        new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim());
  }

  private static String desc(ServedMovement m) {
    return m == null ? "idle" : m.phaseNumber + ":" + m.vehicle + "/" + m.pedestrian;
  }

  private static final class Outcome {
    final List<String> bad = new ArrayList<>();
    boolean flashed;
    boolean flashToProtected;
  }

  /**
   * Runs one program for five cycles. {@code free}: run it FREE instead of coordinated;
   * {@code lpi}: give phase 6 a 60-tick leading pedestrian interval on ped recall.
   */
  private static Outcome run(String name, boolean free, boolean lpi, Demand d) throws Exception {
    NBTTagCompound c = load(name);
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.fromNBT(
        c.getCompoundTag("tcAdv"));
    if (free) {
      plan.getCoordination().setMode(TrafficSignalCoordinationMode.FREE);
    }
    if (lpi) {
      plan.getPhase(6).setDelayedGreen(60L);
      plan.getPhase(6).setPedRecall(true);
    }
    TrafficSignalControllerCircuits ckts = TrafficSignalControllerCircuits.fromNBT(
        c.getCompoundTag("tcCrc"));
    BlockPos fya = ckts.getCircuit(WBL_CIRCUIT).getFlashingLeftSignals().get(0);
    RingBarrierState rb = new RingBarrierState();
    Outcome o = new Outcome();
    Integer prev = null;
    for (long t = 0; t < 9000L; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, d);
      Integer ind = rb.getLastAppliedPhase().resolveVehicleSignalStates().get(fya);
      ServedMovement m1 = rb.getLastServed(1);
      ServedMovement m2 = rb.getLastServed(2);
      if (t < 2000L) { // past the cold start
        prev = ind;
        continue;
      }
      String at = "t=" + t + " " + desc(m1) + " | " + desc(m2);
      boolean sixInDelay = m2 != null && m2.phaseNumber == 6 && m2.vehicle == VehInterval.RED
          && m2.pedestrian == PedInterval.WALK;
      boolean lit = ind != null && !TrafficSignalPhase.isRedIndication(ind);
      if (sixInDelay && lit) {
        o.bad.add(at + ": arrow " + ind + " while EB is held for its walk");
      }
      boolean wasYellow = prev != null && prev == AbstractBlockControllableSignal.SIGNAL_YELLOW;
      if (wasYellow && lit && ind != AbstractBlockControllableSignal.SIGNAL_YELLOW) {
        o.bad.add(at + ": arrow went from steady yellow to " + ind + " without red");
      }
      if (ind != null && ind == TrafficSignalPhase.INDICATION_FYA) {
        o.flashed = true;
      }
      if (prev != null && prev == TrafficSignalPhase.INDICATION_FYA && ind != null
          && TrafficSignalPhase.isGreenIndication(ind)) {
        o.flashToProtected = true;
      }
      prev = ind;
    }
    return o;
  }

  private static Demand[] demands() {
    Demand all = new Demand();
    for (int i = 0; i < 6; i++) {
      all.veh(i, 2, 2, 1).ped(i);
    }
    return new Demand[] {all, new Demand().veh(WBL_CIRCUIT, 0, 2, 0), new Demand()};
  }

  private static void check(boolean free, boolean lpi) throws Exception {
    for (String name : PROGRAMS) {
      for (Demand d : demands()) {
        Outcome o = run(name, free, lpi, d);
        String what = name + (free ? " FREE" : " coordinated") + (lpi ? " with LPI" : "");
        assertTrue(o.bad.isEmpty(), what + ": " + o.bad.size() + " bad ticks, first " + o.bad);
        assertTrue(o.flashed, what + ": the FYA never flashed");
        assertTrue(o.flashToProtected,
            what + ": the FYA never went straight from flashing to its protected green");
      }
    }
  }

  @Test
  @DisplayName("FYA, coordinated: no steady-yellow blip when the opposing through clears")
  void coordinatedNoYellowBlip() throws Exception {
    check(false, false);
  }

  @Test
  @DisplayName("FYA, coordinated, opposing through with a leading ped interval: red until it "
      + "greens, no steady yellow over the walk")
  void coordinatedWithLeadingPedInterval() throws Exception {
    check(false, true);
  }

  @Test
  @DisplayName("FYA, free: red through the opposing through's leading ped interval")
  void freeWithLeadingPedInterval() throws Exception {
    check(true, true);
  }

  @Test
  @DisplayName("FYA, free, no leading ped interval: flashes with the opposing through")
  void freeNoLeadingPedInterval() throws Exception {
    check(true, false);
  }
}
