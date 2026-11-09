package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Whole-plan JSON (issue #337): a controller's program round-trips NBT &rarr; JSON &rarr; NBT
 * byte for byte, every SPATT fixture ({@code src/test/resources/spatt}, copied from
 * Mica-Technologies/SPATT {@code test/fixtures/csm}, MIT, same owner) imports, and a plan that
 * does not parse changes nothing.
 */
class AdvancedPlanJsonTest {

  /** A plan with every part the format carries set away from its default. */
  static TrafficSignalProgrammedPhasePlan everything() {
    TrafficSignalProgrammedPhasePlan plan =
        RingBarrierStateCoordinationTest.mainStreetPlan(1500L,
            RingBarrierStateCoordinationTest.HAMMOND_SPLITS);
    TrafficSignalProgrammedPhase p4 = plan.getPhase(4);
    p4.setLockCall(true);
    p4.setDualEntry(true);
    p4.setConditionalService(true);
    p4.setPedRecall(true);
    p4.setRestInWalk(true);
    p4.setDelayedGreen(60L);
    p4.setBikeMinGreen(120L);
    p4.setMax2(500L);
    p4.setAddedInitial(20L);
    p4.setMaxInitial(200L);
    p4.setMinGap(30L);
    p4.setTimeBeforeReduce(100L);
    p4.setTimeToReduce(200L);
    p4.setFlashOverride(TrafficSignalFlashOverride.RED);
    p4.setExclusionMask((1 << 7) | (1 << 8));
    plan.getPhase(3).setMovement(TrafficSignalPhaseMovement.PROTECTED);
    plan.getPhase(2).setRecallMode(TrafficSignalRecallMode.SOFT);
    plan.setTimeOfDayPatterns(true);
    plan.getCoordinationSchedule().setStartHour(1, 10);
    TrafficSignalCoordinationPlan night = plan.getCoordination(3);
    night.setMode(TrafficSignalCoordinationMode.COORDINATED);
    night.setCycleLength(1200L);
    night.setOffset(77L);
    night.setCoordinatedPhases(new int[] {6});
    night.setSplit(6, 700L);
    TrafficSignalProgrammedOverlap ov = new TrafficSignalProgrammedOverlap();
    ov.setEnabled(true);
    ov.setOutputCircuitIndex(1);
    ov.setOutputMovement(TrafficSignalPhaseMovement.RIGHT);
    ov.setIncludedPhases(new int[] {3, 4});
    ov.setPermissivePhases(new int[] {6});
    ov.setTrailGreen(40L);
    ov.setLeadGreen(20L);
    ov.setType(TrafficSignalOverlapType.MINUS_GREEN_YELLOW);
    ov.setModifierPhases(new int[] {1});
    ov.setCallPhase(4);
    plan.getVehicleOverlaps().add(ov);
    plan.getVehicleOverlaps().add(new TrafficSignalProgrammedOverlap());
    TrafficSignalPreempt pe = new TrafficSignalPreempt();
    pe.setEnabled(true);
    pe.setType(TrafficSignalPreemptType.EMERGENCY);
    pe.setTriggerCircuitIndex(2);
    pe.setTriggerOnDetectors(true);
    pe.setTrackClearPhases(new int[] {2});
    pe.setDwellPhases(new int[] {6, 1});
    pe.setExitPhases(new int[] {2, 6});
    pe.setSignCircuits(new int[] {0, 2});
    pe.setMinDwell(300L);
    pe.setMinGreenBeforePreempt(60L);
    plan.getPreempts().add(pe);
    TrafficSignalPriorityPlan bus = plan.getPriority(1);
    bus.setEnabled(true);
    bus.setTriggerCircuitIndex(1);
    bus.setTriggerOnDetectors(true);
    bus.setTransitPhase(6);
    bus.setMaxExtension(150L);
    bus.setMaxEarlyReturn(90L);
    bus.setMinCyclesBetweenGrants(3);
    bus.setQueueJump(60L);
    return plan;
  }

  /** Every plan the engine tests build, by name, as round-trip fixtures. */
  private static Map<String, TrafficSignalProgrammedPhasePlan> fixtures() {
    Map<String, TrafficSignalProgrammedPhasePlan> plans = new LinkedHashMap<>();
    plans.put("default", TrafficSignalProgrammedPhasePlan.createDefault());
    plans.put("everything", everything());
    plans.put("hammond", RingBarrierStateCoordinationTest.mainStreetPlan(1500L,
        RingBarrierStateCoordinationTest.HAMMOND_SPLITS));
    plans.put("keough", RingBarrierStateCoordinationTest.keoughPlan());
    plans.put("z01", CoordinatedDualEntryFreezeReproTest.z01());
    plans.put("t03", CoordinatedDualEntryFreezeReproTest.t03());
    plans.put("overlapHold", RingBarrierStateTest.overlapHoldPlan(3, 4));
    plans.put("arterial", FyaBarrierClearanceTest.arterialPlan(true));
    return plans;
  }

  private static byte[] bytes(NBTTagCompound tag) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CompressedStreamTools.write(tag, new DataOutputStream(out));
    return out.toByteArray();
  }

  @Test
  @DisplayName("NBT -> JSON -> NBT is byte-equal for every fixture plan, read onto a fresh "
      + "controller")
  void roundTripIsExact() throws Exception {
    for (Map.Entry<String, TrafficSignalProgrammedPhasePlan> e : fixtures().entrySet()) {
      TrafficSignalProgrammedPhasePlan plan = e.getValue();
      String json = AdvancedPlanJson.export(plan, null, 20L, 60L, e.getKey());
      AdvancedPlanJson.Result result =
          AdvancedPlanJson.apply(json, TrafficSignalProgrammedPhasePlan.createDefault());
      assertEquals(plan.toNBT(), result.plan.toNBT(), e.getKey() + ": the plan changed");
      assertArrayEquals(bytes(plan.toNBT()), bytes(result.plan.toNBT()),
          e.getKey() + ": the NBT is not byte-equal");
      assertEquals(Long.valueOf(20L), result.redAmber, e.getKey());
      assertEquals(Long.valueOf(60L), result.greenFlash, e.getKey());
      assertEquals(e.getKey(), result.name);
      // And exporting the result gives the same text again.
      assertEquals(json, AdvancedPlanJson.export(result.plan, null, 20L, 60L, e.getKey()),
          e.getKey());
    }
  }

  private static List<File> spattFixtures() {
    File dir = new File("modules/roads/src/test/resources/spatt");
    if (!dir.isDirectory()) {
      dir = new File(System.getProperty("user.dir"), "modules/roads/src/test/resources/spatt");
    }
    List<File> files = new ArrayList<>();
    File[] listed = dir.listFiles((d, n) -> n.endsWith(".csm.json"));
    if (listed != null) {
      java.util.Collections.addAll(files, listed);
    }
    return files;
  }

  @Test
  @DisplayName("every SPATT fixture imports onto a default controller without error")
  void spattFixturesImport() throws Exception {
    List<File> files = spattFixtures();
    assertEquals(5, files.size(), "expected the five SPATT fixtures, found " + files);
    for (File f : files) {
      String json = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
      TrafficSignalProgrammedPhasePlan current = TrafficSignalProgrammedPhasePlan.createDefault();
      AdvancedPlanJson.Result result = AdvancedPlanJson.apply(json, current);
      assertTrue(result.plan.isConfigured() || result.plan.getPhases().stream()
          .anyMatch(TrafficSignalProgrammedPhase::isEnabled), f.getName());
      assertEquals(Long.valueOf(0L), result.redAmber, f.getName());
      // Applying again onto the result changes nothing: the import is a pure function of the
      // plan and what it lands on.
      assertEquals(result.plan.toNBT(), AdvancedPlanJson.apply(json, result.plan).plan.toNBT(),
          f.getName());
    }
  }

  @Test
  @DisplayName("SPATT lead-lag fixture: rings, timing and the pattern land where the plan says")
  void leadLagFixtureValues() throws Exception {
    File f = new File(spattFixtures().get(0).getParentFile(), "lead-lag-8-phase.csm.json");
    String json = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    TrafficSignalProgrammedPhasePlan current = TrafficSignalProgrammedPhasePlan.createDefault();
    current.getPhase(2).setCircuitIndex(3); // wiring the plan leaves out is kept
    AdvancedPlanJson.Result result = AdvancedPlanJson.apply(json, current);
    TrafficSignalProgrammedPhasePlan plan = result.plan;
    assertArrayEquals(new int[] {2, 1, 3, 4}, plan.getRing1Sequence());
    assertArrayEquals(new int[] {5, 6, 7, 8}, plan.getRing2Sequence());
    assertEquals(200L, plan.getPhase(2).getMinGreen());
    assertEquals(360L, plan.getPhase(2).getPedClear());
    assertEquals(3, plan.getPhase(2).getCircuitIndex(), "an absent circuit keeps its value");
    assertEquals(1, plan.getPhase(3).getBarrier());
    TrafficSignalCoordinationPlan co = plan.getCoordination(0);
    assertEquals(TrafficSignalCoordinationMode.COORDINATED, co.getMode());
    assertEquals(1800L, co.getCycleLength());
    assertEquals(560L, co.getSplit(8));
    assertTrue(co.isCoordinatedPhase(2) && co.isCoordinatedPhase(6));
    assertTrue(plan.isTimeOfDayPatterns());
    assertEquals(15, plan.getCoordinationSchedule().getStartHour(0));
    assertFalse(current.getCoordination(0).isCoordinated(), "the current plan is untouched");
  }

  @Test
  @DisplayName("phases the plan lists are set; phases it does not list are disabled")
  void unlistedPhasesAreDisabled() throws Exception {
    TrafficSignalProgrammedPhasePlan current = everything();
    String json = "{\"format\":\"csm-asc3-plan\",\"formatVersion\":2,\"rings\":[[2],[6]],"
        + "\"phases\":[{\"number\":2,\"enabled\":true,\"minGreen\":15000},"
        + "{\"number\":6,\"enabled\":true}]}";
    AdvancedPlanJson.Result result = AdvancedPlanJson.apply(json, current);
    assertTrue(result.plan.getPhase(2).isEnabled());
    assertEquals(12000L, result.plan.getPhase(2).getMinGreen(), "times clamp to 600 s");
    assertFalse(result.plan.getPhase(1).isEnabled());
    assertFalse(result.plan.getPhase(4).isEnabled());
    assertEquals(current.getPhase(4).getDelayedGreen(),
        result.plan.getPhase(4).getDelayedGreen(), "an unlisted phase keeps its timing");
    assertEquals(1, result.plan.getVehicleOverlaps().get(0).getOutputCircuitIndex(),
        "overlaps the plan leaves out are kept");
    assertNull(result.redAmber);
  }

  @Test
  @DisplayName("a plan that does not parse, or names an unknown value, changes nothing")
  void badPlansChangeNothing() {
    TrafficSignalProgrammedPhasePlan current = everything();
    NBTTagCompound before = current.toNBT();
    String[] bad = {
        "",
        "not json {",
        "[1,2]",
        "{\"format\":\"something-else\",\"formatVersion\":2}",
        "{\"format\":\"csm-asc3-plan\",\"formatVersion\":3,\"rings\":[[1],[5]],\"phases\":[]}",
        "{\"format\":\"csm-asc3-plan\",\"formatVersion\":2,\"rings\":[[1,1],[5]],\"phases\":[]}",
        "{\"format\":\"csm-asc3-plan\",\"formatVersion\":2,\"rings\":[[1],[5]],"
            + "\"phases\":[{\"number\":1,\"recall\":\"SOMETIMES\"}]}",
        "{\"format\":\"csm-asc3-plan\",\"formatVersion\":2,\"rings\":[[1],[5]],"
            + "\"phases\":[{\"number\":9}]}",
        "{\"format\":\"csm-asc3-plan\",\"formatVersion\":2,\"rings\":[[1],[5]],"
            + "\"phases\":[{\"number\":1,\"minGreen\":1.5}]}",
        "{\"format\":\"csm-asc3-plan\",\"formatVersion\":2,\"rings\":[[1],[5]],\"phases\":[],"
            + "\"patterns\":[{\"slot\":0,\"splits\":{\"9\":100}}]}",
    };
    for (String json : bad) {
      AdvancedPlanJson.PlanFormatException e = assertThrows(
          AdvancedPlanJson.PlanFormatException.class, () -> AdvancedPlanJson.apply(json, current),
          "should be refused: " + json);
      assertFalse(e.getMessage() == null || e.getMessage().isEmpty(), json);
    }
    assertEquals(before, current.toNBT(), "a refused plan must not touch the program");
  }

  @Test
  @DisplayName("a version 1 plan's single priority becomes the first priority plan")
  void versionOnePriority() throws Exception {
    String json = "{\"format\":\"csm-asc3-plan\",\"formatVersion\":1,\"rings\":[[2],[6]],"
        + "\"phases\":[{\"number\":2,\"enabled\":true}],"
        + "\"priority\":{\"enabled\":true,\"triggerCircuit\":0,\"transitPhase\":2}}";
    AdvancedPlanJson.Result result =
        AdvancedPlanJson.apply(json, TrafficSignalProgrammedPhasePlan.createDefault());
    assertTrue(result.plan.getPriority(0).isEnabled());
    assertEquals(2, result.plan.getPriority(0).getTransitPhase());
  }
}
