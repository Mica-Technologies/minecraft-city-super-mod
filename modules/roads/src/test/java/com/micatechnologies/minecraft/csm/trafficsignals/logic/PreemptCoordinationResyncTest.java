package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Issue #372, replaying the live Madison Ave x 86th program (Alto Midtown, read 2026-11-18):
 * coordinated 2 and 6 on an 1800 cycle, ring 1 [2, 4], ring 2 [6, 5] with 5 a lagging FYA left,
 * every phase on max recall, walks recalled on 2, 4 and 6 (2 and 4 with a leading interval), and
 * an EB/WB preempt dwelling 4.
 *
 * <p>After a preempt that exited while 2 and 6 were due, the two throughs came back with their
 * walks. 6 yielded early for barrier B's window; 2, still timing the minimum green behind its
 * leading interval, could not follow before phase 4's period closed, and ring 2 then sat dark
 * beside 2 for about 80 s while 2 dwelled to its next yield point.</p>
 */
class PreemptCoordinationResyncTest {

  private static final long CYCLE = 1800L;
  /** The preempt's trigger circuit (pe[2], dwelling phase 4). */
  private static final int PREEMPT_CIRCUIT = 2;

  private static NBTTagCompound load(String name) throws Exception {
    File f = new File("modules/roads/src/test/resources/alto/" + name);
    if (!f.isFile()) {
      f = new File(System.getProperty("user.dir"), f.getPath());
    }
    return JsonToNBT.getTagFromJson(
        new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim());
  }

  private static String desc(ServedMovement m) {
    return m == null ? "idle" : m.phaseNumber + ":" + m.vehicle;
  }

  private static boolean coordGreen(ServedMovement m) {
    return m != null && (m.phaseNumber == 2 || m.phaseNumber == 6)
        && m.vehicle == VehInterval.GREEN;
  }

  private static boolean dark(ServedMovement m) {
    return m == null || m.phaseNumber == 0 || m.vehicle == VehInterval.RED;
  }

  @Test
  @DisplayName("coordination: after a preempt exit anywhere in the cycle, neither ring sits dark "
      + "beside the other's coordinated green, and the cycle after runs every phase "
      + "(issue #372)")
  void ringsResyncTogetherAfterPreempt() throws Exception {
    NBTTagCompound c = load("midtown_mad86_controller_20261118.snbt");
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.fromNBT(
        c.getCompoundTag("tcAdv"));
    TrafficSignalControllerCircuits ckts = TrafficSignalControllerCircuits.fromNBT(
        c.getCompoundTag("tcCrc"));
    long offset = plan.getCoordination().getOffset();
    List<String> bad = new ArrayList<>();
    for (long entry = 0L; entry < CYCLE; entry += 50L) {
      Demand normal = new Demand();
      Demand pre = new Demand();
      for (int i = 0; i < ckts.getCircuitCount(); i++) {
        normal.veh(i, 1, 1, 0);
        pre.veh(i, 1, 1, 0);
      }
      pre.summaries.get(PREEMPT_CIRCUIT).withPreemptDetectorCalls(1);
      RingBarrierState rb = new RingBarrierState();
      long start = offset + 3 * CYCLE + entry;
      long release = start + 200L;
      long end = release + 3 * CYCLE;
      long darkSince = -1L;
      long longestDark = 0L;
      boolean served5 = false;
      boolean served4 = false;
      StringBuilder tl = new StringBuilder();
      String last = "";
      for (long t = 0; t < end; t++) {
        boolean calling = t >= start && t < release;
        rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, calling ? pre : normal);
        if (t < release) {
          continue;
        }
        ServedMovement m1 = rb.getLastServed(1);
        ServedMovement m2 = rb.getLastServed(2);
        String key = desc(m1) + " | " + desc(m2);
        if (!key.equals(last) && t < release + 2 * CYCLE) {
          tl.append("\n  t=").append(t - release).append(" local=")
              .append(((t - offset) % CYCLE + CYCLE) % CYCLE).append("  ").append(key);
        }
        last = key;
        boolean lonely = (coordGreen(m1) && dark(m2)) || (coordGreen(m2) && dark(m1));
        if (lonely) {
          if (darkSince < 0L) {
            darkSince = t;
          }
          longestDark = Math.max(longestDark, t - darkSince + 1);
        } else {
          darkSince = -1L;
        }
        if (t >= release + CYCLE) {
          served5 |= m2 != null && m2.phaseNumber == 5 && m2.vehicle == VehInterval.GREEN;
          served4 |= m1 != null && m1.phaseNumber == 4 && m1.vehicle == VehInterval.GREEN;
        }
      }
      // A ring may be dark beside the other's green only for its own clearance and the lagging
      // left's turn (ring 2 clears 6, serves 5, then waits for 2), never for a cycle.
      if (longestDark > 400L) {
        bad.add("entry " + entry + ": a ring dark beside a coordinated green for " + longestDark
            + " ticks" + tl);
      }
      if (!served5 || !served4) {
        bad.add("entry " + entry + ": not back in step after the recovery cycle (5 served "
            + served5 + ", 4 served " + served4 + ")" + tl);
      }
    }
    assertTrue(bad.isEmpty(), bad.size() + " bad entries, first: "
        + (bad.isEmpty() ? "" : bad.get(0)));
  }
}
