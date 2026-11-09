package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Whole-plan JSON for the ADVANCED (ASC-3) controller, in SPATT's {@code csm-asc3-plan} format
 * (version 2; version 1 is read too): the program a controller's {@code tcAdv} NBT holds, plus
 * the controller-wide red+amber and green flash times, under named fields in the controller's own
 * units (ticks) and enum constants.
 *
 * <p><b>Absent means keep.</b> A plan is applied onto the controller's current program: a field a
 * plan leaves out keeps the controller's value, a pattern slot it does not list is kept, and
 * {@code overlaps}, {@code preempts} and {@code priorities}, when present, replace those lists
 * whole. Phases listed are set; a phase the plan does not list is disabled. So a plan made in
 * SPATT, which carries timing but not wiring (circuits, movements, flash), can be dropped onto a
 * controller that is already wired.</p>
 *
 * <p>{@link #apply} is atomic: it works on a copy of the program and either returns the whole new
 * program or throws {@link PlanFormatException} naming the first problem, so a bad plan changes
 * nothing. Times are clamped to 0..12000 ticks as the panel clamps them.</p>
 *
 * <p>Export writes every field, all four pattern slots and every phase, and fills each phase's
 * informational {@code hasPedestrianSignals} from its circuit, so a controller's plan round-trips
 * through JSON unchanged.</p>
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public final class AdvancedPlanJson {

  /** The format name SPATT writes and reads. */
  public static final String FORMAT = "csm-asc3-plan";

  /** The format version this controller writes. */
  public static final int FORMAT_VERSION = 2;

  /** The longest plan text accepted, in characters; a whole plan is a few kilobytes. */
  public static final int MAX_LENGTH = 256 * 1024;

  /** The largest value any time field takes, in ticks (600 s), as the panel clamps them. */
  static final long MAX_TICKS = 12000L;

  private AdvancedPlanJson() {
  }

  /** A plan that cannot be applied; the message names the field and the problem. */
  public static final class PlanFormatException extends Exception {

    public PlanFormatException(String message) {
      super(message);
    }
  }

  /** What {@link #apply} produced: the new program and the controller-wide settings it set. */
  public static final class Result {

    /** The new program, a copy: the controller's own is untouched until the caller swaps it in. */
    public final TrafficSignalProgrammedPhasePlan plan;

    /** The red+amber time in ticks, or {@code null} when the plan leaves it out. */
    public final Long redAmber;

    /** The green flash time in ticks, or {@code null} when the plan leaves it out. */
    public final Long greenFlash;

    /** The plan's {@code name}, or {@code null}. */
    public final String name;

    Result(TrafficSignalProgrammedPhasePlan plan, Long redAmber, Long greenFlash, String name) {
      this.plan = plan;
      this.redAmber = redAmber;
      this.greenFlash = greenFlash;
      this.name = name;
    }
  }

  // region: export

  /**
   * Writes a controller's whole program as plan JSON.
   *
   * @param plan       the program
   * @param circuits   the controller's circuits, for each phase's {@code hasPedestrianSignals}, or
   *                   {@code null} to leave it out
   * @param redAmber   the controller's red+amber time, in ticks
   * @param greenFlash the controller's green flash time, in ticks
   * @param name       a name for the plan, or {@code null}
   *
   * @return the plan, pretty-printed
   */
  public static String export(TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits, long redAmber, long greenFlash, String name) {
    return new GsonBuilder().setPrettyPrinting().create()
        .toJson(toJson(plan, circuits, redAmber, greenFlash, name));
  }

  /** {@link #export} as a JSON tree. */
  public static JsonObject toJson(TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits, long redAmber, long greenFlash, String name) {
    JsonObject root = new JsonObject();
    root.addProperty("format", FORMAT);
    root.addProperty("formatVersion", FORMAT_VERSION);
    root.addProperty("generator", "CSM");
    if (name != null && !name.isEmpty()) {
      root.addProperty("name", name);
    }
    JsonArray phases = new JsonArray();
    for (TrafficSignalProgrammedPhase p : plan.getPhases()) {
      phases.add(phaseToJson(p, circuits));
    }
    root.add("phases", phases);
    JsonArray rings = new JsonArray();
    rings.add(ints(plan.getRing1Sequence()));
    rings.add(ints(plan.getRing2Sequence()));
    root.add("rings", rings);
    JsonArray patterns = new JsonArray();
    for (int slot = 0; slot < TrafficTimeOfDaySchedule.SLOT_COUNT; slot++) {
      patterns.add(patternToJson(slot, plan.getCoordination(slot)));
    }
    root.add("patterns", patterns);
    JsonObject schedule = new JsonObject();
    schedule.addProperty("enabled", plan.isTimeOfDayPatterns());
    JsonArray hours = new JsonArray();
    for (int slot = 0; slot < TrafficTimeOfDaySchedule.SLOT_COUNT; slot++) {
      hours.add(new JsonPrimitive(plan.getCoordinationSchedule().getStartHour(slot)));
    }
    schedule.add("startHours", hours);
    root.add("schedule", schedule);
    root.addProperty("redAmber", redAmber);
    root.addProperty("greenFlash", greenFlash);
    JsonArray overlaps = new JsonArray();
    for (TrafficSignalProgrammedOverlap ov : plan.getVehicleOverlaps()) {
      overlaps.add(overlapToJson(ov));
    }
    root.add("overlaps", overlaps);
    JsonArray preempts = new JsonArray();
    for (TrafficSignalPreempt pe : plan.getPreempts()) {
      preempts.add(preemptToJson(pe));
    }
    root.add("preempts", preempts);
    JsonArray priorities = new JsonArray();
    for (TrafficSignalPriorityPlan pri : plan.getPriorities()) {
      priorities.add(priorityToJson(pri));
    }
    root.add("priorities", priorities);
    return root;
  }

  private static JsonObject phaseToJson(TrafficSignalProgrammedPhase p,
      TrafficSignalControllerCircuits circuits) {
    JsonObject o = new JsonObject();
    o.addProperty("number", p.getPhaseNumber());
    o.addProperty("enabled", p.isEnabled());
    o.addProperty("barrier", p.getBarrier());
    o.addProperty("minGreen", p.getMinGreen());
    o.addProperty("passage", p.getPassage());
    o.addProperty("maxGreen", p.getMaxGreen());
    o.addProperty("maxGreen2", p.getMax2());
    o.addProperty("yellow", p.getYellow());
    o.addProperty("redClear", p.getRedClear());
    o.addProperty("addedInitial", p.getAddedInitial());
    o.addProperty("maxInitial", p.getMaxInitial());
    o.addProperty("minGap", p.getMinGap());
    o.addProperty("timeBeforeReduce", p.getTimeBeforeReduce());
    o.addProperty("timeToReduce", p.getTimeToReduce());
    o.addProperty("recall", p.getRecallMode().name());
    o.addProperty("pedRecall", p.isPedRecall());
    o.addProperty("restInWalk", p.isRestInWalk());
    o.addProperty("dualEntry", p.isDualEntry());
    o.addProperty("conditionalService", p.isConditionalService());
    o.addProperty("lockCall", p.isLockCall());
    JsonArray ex = new JsonArray();
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      if (p.excludes(n)) {
        ex.add(new JsonPrimitive(n));
      }
    }
    o.add("exclusions", ex);
    o.addProperty("walk", p.getWalk());
    o.addProperty("pedClear", p.getPedClear());
    o.addProperty("movement", p.getMovement().name());
    o.addProperty("circuit", p.getCircuitIndex());
    o.addProperty("flash", p.getFlashOverride().name());
    o.addProperty("delayedGreen", p.getDelayedGreen());
    o.addProperty("bikeMinGreen", p.getBikeMinGreen());
    o.addProperty("permissivePhase", p.getPermissivePhase());
    int ci = p.getCircuitIndex();
    if (circuits != null && ci >= 0 && ci < circuits.getCircuitCount()) {
      o.addProperty("hasPedestrianSignals",
          !circuits.getCircuit(ci).getPedestrianSignals().isEmpty());
    }
    return o;
  }

  private static JsonObject patternToJson(int slot, TrafficSignalCoordinationPlan co) {
    JsonObject o = new JsonObject();
    o.addProperty("slot", slot);
    o.addProperty("mode", co.getMode().name());
    o.addProperty("cycle", co.getCycleLength());
    o.addProperty("offset", co.getOffset());
    o.add("coordinatedPhases", ints(co.getCoordinatedPhases()));
    JsonObject splits = new JsonObject();
    for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
      if (co.getSplit(n) > 0L) {
        splits.addProperty(String.valueOf(n), co.getSplit(n));
      }
    }
    o.add("splits", splits);
    return o;
  }

  private static JsonObject overlapToJson(TrafficSignalProgrammedOverlap ov) {
    JsonObject o = new JsonObject();
    o.addProperty("enabled", ov.isEnabled());
    o.addProperty("outputCircuit", ov.getOutputCircuitIndex());
    o.addProperty("outputMovement", ov.getOutputMovement().name());
    o.add("included", ints(ov.getIncludedPhases()));
    o.add("permissive", ints(ov.getPermissivePhases()));
    o.addProperty("trailGreen", ov.getTrailGreen());
    o.addProperty("leadGreen", ov.getLeadGreen());
    o.addProperty("type", ov.getType().name());
    o.add("modifiers", ints(ov.getModifierPhases()));
    o.addProperty("callPhase", ov.getCallPhase());
    return o;
  }

  private static JsonObject preemptToJson(TrafficSignalPreempt pe) {
    JsonObject o = new JsonObject();
    o.addProperty("enabled", pe.isEnabled());
    o.addProperty("type", pe.getType().name());
    o.addProperty("triggerCircuit", pe.getTriggerCircuitIndex());
    o.addProperty("triggerMovement", pe.getTriggerMovement().name());
    o.addProperty("triggerOnDetectors", pe.isTriggerOnDetectors());
    o.add("trackClear", ints(pe.getTrackClearPhases()));
    o.add("dwell", ints(pe.getDwellPhases()));
    o.add("exit", ints(pe.getExitPhases()));
    o.add("signCircuits", ints(pe.getSignCircuits()));
    o.addProperty("minDwell", pe.getMinDwell());
    o.addProperty("minGreenBefore", pe.getMinGreenBeforePreempt());
    return o;
  }

  private static JsonObject priorityToJson(TrafficSignalPriorityPlan pri) {
    JsonObject o = new JsonObject();
    o.addProperty("enabled", pri.isEnabled());
    o.addProperty("triggerCircuit", pri.getTriggerCircuitIndex());
    o.addProperty("triggerMovement", pri.getTriggerMovement().name());
    o.addProperty("triggerOnDetectors", pri.isTriggerOnDetectors());
    o.addProperty("transitPhase", pri.getTransitPhase());
    o.addProperty("maxExtension", pri.getMaxExtension());
    o.addProperty("maxEarlyReturn", pri.getMaxEarlyReturn());
    o.addProperty("minCycles", pri.getMinCyclesBetweenGrants());
    o.addProperty("queueJump", pri.getQueueJump());
    return o;
  }

  private static JsonArray ints(int[] values) {
    JsonArray a = new JsonArray();
    for (int v : values) {
      a.add(new JsonPrimitive(v));
    }
    return a;
  }

  // endregion

  // region: import

  /**
   * Applies plan JSON onto a copy of {@code current}.
   *
   * @param json    the plan text
   * @param current the controller's program now (not modified)
   *
   * @return the new program and settings
   *
   * @throws PlanFormatException if the text is not a plan this controller can apply; nothing is
   *                             changed
   */
  public static Result apply(String json, TrafficSignalProgrammedPhasePlan current)
      throws PlanFormatException {
    if (json == null || json.trim().isEmpty()) {
      throw new PlanFormatException("the plan is empty");
    }
    if (json.length() > MAX_LENGTH) {
      throw new PlanFormatException("the plan is " + json.length() + " characters, more than the "
          + MAX_LENGTH + " a plan may be");
    }
    JsonElement parsed;
    try {
      parsed = new JsonParser().parse(json);
    } catch (JsonParseException | IllegalStateException e) {
      throw new PlanFormatException("not valid JSON: " + rootMessage(e));
    }
    if (!parsed.isJsonObject()) {
      throw new PlanFormatException("a plan is a JSON object");
    }
    JsonObject root = parsed.getAsJsonObject();
    String format = string(root, "format", "format", null);
    if (!FORMAT.equals(format)) {
      throw new PlanFormatException("format must be \"" + FORMAT + "\", not "
          + (format == null ? "missing" : "\"" + format + "\""));
    }
    long version = integer(root, "formatVersion", "formatVersion", -1L, 1L, 2L);
    if (version < 0) {
      throw new PlanFormatException("formatVersion is missing");
    }

    TrafficSignalProgrammedPhasePlan plan =
        TrafficSignalProgrammedPhasePlan.fromNBT(current.toNBT());

    // Rings first: a phase's ring follows the sequence it is in.
    JsonArray rings = array(root, "rings", "rings");
    if (rings == null) {
      throw new PlanFormatException("rings is missing");
    }
    if (rings.size() != 2) {
      throw new PlanFormatException("rings must hold two rings, not " + rings.size());
    }
    int[] ring1 = phaseList(rings.get(0), "rings[0]");
    int[] ring2 = phaseList(rings.get(1), "rings[1]");
    boolean[] seen = new boolean[TrafficSignalProgrammedPhasePlan.PHASE_COUNT + 1];
    for (int[] ring : new int[][] {ring1, ring2}) {
      for (int n : ring) {
        if (seen[n]) {
          throw new PlanFormatException("rings: phase " + n + " is in a ring twice");
        }
        seen[n] = true;
      }
    }
    plan.setRing1Sequence(ring1);
    plan.setRing2Sequence(ring2);

    JsonArray phases = array(root, "phases", "phases");
    if (phases == null) {
      throw new PlanFormatException("phases is missing");
    }
    if (phases.size() > TrafficSignalProgrammedPhasePlan.PHASE_COUNT) {
      throw new PlanFormatException("phases lists " + phases.size() + " phases; the controller "
          + "has " + TrafficSignalProgrammedPhasePlan.PHASE_COUNT);
    }
    boolean[] listed = new boolean[TrafficSignalProgrammedPhasePlan.PHASE_COUNT + 1];
    for (int i = 0; i < phases.size(); i++) {
      String at = "phases[" + i + "]";
      JsonObject o = object(phases.get(i), at);
      int n = (int) integer(o, "number", at + ".number", -1L, 1L,
          TrafficSignalProgrammedPhasePlan.PHASE_COUNT);
      if (n < 0) {
        throw new PlanFormatException(at + ".number is missing");
      }
      if (listed[n]) {
        throw new PlanFormatException(at + ": phase " + n + " is listed twice");
      }
      listed[n] = true;
      applyPhase(o, plan.getPhase(n), at, ring1, ring2);
    }
    for (TrafficSignalProgrammedPhase p : plan.getPhases()) {
      if (p.getPhaseNumber() >= 1 && p.getPhaseNumber() < listed.length
          && !listed[p.getPhaseNumber()]) {
        p.setEnabled(false);
      }
    }

    JsonArray patterns = array(root, "patterns", "patterns");
    if (patterns != null) {
      if (patterns.size() > TrafficTimeOfDaySchedule.SLOT_COUNT) {
        throw new PlanFormatException("patterns lists " + patterns.size() + " patterns; the "
            + "controller has " + TrafficTimeOfDaySchedule.SLOT_COUNT + " slots");
      }
      boolean[] slotSeen = new boolean[TrafficTimeOfDaySchedule.SLOT_COUNT];
      for (int i = 0; i < patterns.size(); i++) {
        String at = "patterns[" + i + "]";
        JsonObject o = object(patterns.get(i), at);
        int slot = (int) integer(o, "slot", at + ".slot", -1L, 0L,
            TrafficTimeOfDaySchedule.SLOT_COUNT - 1);
        if (slot < 0) {
          throw new PlanFormatException(at + ".slot is missing");
        }
        if (slotSeen[slot]) {
          throw new PlanFormatException(at + ": slot " + slot + " is listed twice");
        }
        slotSeen[slot] = true;
        applyPattern(o, plan.getCoordination(slot), at);
      }
    }

    JsonObject schedule = optObject(root, "schedule", "schedule");
    if (schedule != null) {
      Boolean enabled = bool(schedule, "enabled", "schedule.enabled");
      if (enabled != null) {
        plan.setTimeOfDayPatterns(enabled);
      }
      JsonArray hours = array(schedule, "startHours", "schedule.startHours");
      if (hours != null) {
        if (hours.size() != TrafficTimeOfDaySchedule.SLOT_COUNT) {
          throw new PlanFormatException("schedule.startHours must hold "
              + TrafficTimeOfDaySchedule.SLOT_COUNT + " hours, not " + hours.size());
        }
        for (int slot = 0; slot < hours.size(); slot++) {
          plan.getCoordinationSchedule().setStartHour(slot,
              (int) number(hours.get(slot), "schedule.startHours[" + slot + "]", 0L, 23L));
        }
      }
    }

    JsonArray overlaps = array(root, "overlaps", "overlaps");
    if (overlaps != null) {
      List<TrafficSignalProgrammedOverlap> list = new ArrayList<>();
      for (int i = 0; i < overlaps.size(); i++) {
        list.add(overlapFromJson(object(overlaps.get(i), "overlaps[" + i + "]"),
            "overlaps[" + i + "]"));
      }
      plan.getVehicleOverlaps().clear();
      plan.getVehicleOverlaps().addAll(list);
    }
    JsonArray preempts = array(root, "preempts", "preempts");
    if (preempts != null) {
      List<TrafficSignalPreempt> list = new ArrayList<>();
      for (int i = 0; i < preempts.size(); i++) {
        list.add(preemptFromJson(object(preempts.get(i), "preempts[" + i + "]"),
            "preempts[" + i + "]"));
      }
      plan.getPreempts().clear();
      plan.getPreempts().addAll(list);
    }
    JsonArray priorities = array(root, "priorities", "priorities");
    if (priorities == null && version == 1L && root.has("priority")) {
      // Version 1 held a single priority plan where version 2 has the list.
      priorities = new JsonArray();
      priorities.add(root.get("priority"));
    }
    if (priorities != null) {
      if (priorities.size() > TrafficSignalProgrammedPhasePlan.MAX_PRIORITY_PLANS) {
        throw new PlanFormatException("priorities lists " + priorities.size() + " plans; the "
            + "controller holds " + TrafficSignalProgrammedPhasePlan.MAX_PRIORITY_PLANS);
      }
      List<TrafficSignalPriorityPlan> list = new ArrayList<>();
      for (int i = 0; i < priorities.size(); i++) {
        list.add(priorityFromJson(object(priorities.get(i), "priorities[" + i + "]"),
            "priorities[" + i + "]"));
      }
      // One reference: getPriorities() recreates plan 0 whenever it finds the list empty.
      List<TrafficSignalPriorityPlan> held = plan.getPriorities();
      held.clear();
      held.addAll(list);
      plan.getPriorities(); // never empty: recreates plan 0 when the plan listed none
    }

    long ra = integer(root, "redAmber", "redAmber", -1L, 0L, MAX_TICKS);
    long gf = integer(root, "greenFlash", "greenFlash", -1L, 0L, MAX_TICKS);
    return new Result(plan, ra < 0 ? null : ra, gf < 0 ? null : gf,
        string(root, "name", "name", null));
  }

  private static void applyPhase(JsonObject o, TrafficSignalProgrammedPhase p, String at,
      int[] ring1, int[] ring2) throws PlanFormatException {
    for (int n : ring1) {
      if (n == p.getPhaseNumber()) {
        p.setRing(1);
      }
    }
    for (int n : ring2) {
      if (n == p.getPhaseNumber()) {
        p.setRing(2);
      }
    }
    Boolean b;
    if ((b = bool(o, "enabled", at + ".enabled")) != null) {
      p.setEnabled(b);
    }
    long v;
    if ((v = integer(o, "barrier", at + ".barrier", -1L, 0L,
        TrafficSignalProgrammedPhasePlan.PHASE_COUNT - 1)) >= 0) {
      p.setBarrier((int) v);
    }
    if ((v = ticks(o, "minGreen", at)) >= 0) {
      p.setMinGreen(v);
    }
    if ((v = ticks(o, "passage", at)) >= 0) {
      p.setPassage(v);
    }
    if ((v = ticks(o, "maxGreen", at)) >= 0) {
      p.setMaxGreen(v);
    }
    if ((v = ticks(o, "maxGreen2", at)) >= 0) {
      p.setMax2(v);
    }
    if ((v = ticks(o, "yellow", at)) >= 0) {
      p.setYellow(v);
    }
    if ((v = ticks(o, "redClear", at)) >= 0) {
      p.setRedClear(v);
    }
    if ((v = ticks(o, "addedInitial", at)) >= 0) {
      p.setAddedInitial(v);
    }
    if ((v = ticks(o, "maxInitial", at)) >= 0) {
      p.setMaxInitial(v);
    }
    if ((v = ticks(o, "minGap", at)) >= 0) {
      p.setMinGap(v);
    }
    if ((v = ticks(o, "timeBeforeReduce", at)) >= 0) {
      p.setTimeBeforeReduce(v);
    }
    if ((v = ticks(o, "timeToReduce", at)) >= 0) {
      p.setTimeToReduce(v);
    }
    if ((v = ticks(o, "walk", at)) >= 0) {
      p.setWalk(v);
    }
    if ((v = ticks(o, "pedClear", at)) >= 0) {
      p.setPedClear(v);
    }
    if ((v = ticks(o, "delayedGreen", at)) >= 0) {
      p.setDelayedGreen(v);
    }
    if ((v = ticks(o, "bikeMinGreen", at)) >= 0) {
      p.setBikeMinGreen(v);
    }
    TrafficSignalRecallMode recall =
        enumValue(o, "recall", at + ".recall", TrafficSignalRecallMode.class);
    if (recall != null) {
      p.setRecallMode(recall);
    }
    if ((b = bool(o, "pedRecall", at + ".pedRecall")) != null) {
      p.setPedRecall(b);
    }
    if ((b = bool(o, "restInWalk", at + ".restInWalk")) != null) {
      p.setRestInWalk(b);
    }
    if ((b = bool(o, "dualEntry", at + ".dualEntry")) != null) {
      p.setDualEntry(b);
    }
    if ((b = bool(o, "conditionalService", at + ".conditionalService")) != null) {
      p.setConditionalService(b);
    }
    if ((b = bool(o, "lockCall", at + ".lockCall")) != null) {
      p.setLockCall(b);
    }
    if (o.has("exclusions") && !o.get("exclusions").isJsonNull()) {
      int mask = 0;
      for (int n : phaseList(o.get("exclusions"), at + ".exclusions")) {
        if (n != p.getPhaseNumber()) {
          mask |= 1 << n;
        }
      }
      p.setExclusionMask(mask);
    }
    TrafficSignalPhaseMovement movement =
        enumValue(o, "movement", at + ".movement", TrafficSignalPhaseMovement.class);
    if (movement != null) {
      p.setMovement(movement);
    }
    if ((v = integer(o, "circuit", at + ".circuit", -2L, -1L, Integer.MAX_VALUE)) >= -1) {
      p.setCircuitIndex((int) v);
    }
    TrafficSignalFlashOverride flash =
        enumValue(o, "flash", at + ".flash", TrafficSignalFlashOverride.class);
    if (flash != null) {
      p.setFlashOverride(flash);
    }
    if ((v = integer(o, "permissivePhase", at + ".permissivePhase", -1L, 0L,
        TrafficSignalProgrammedPhasePlan.PHASE_COUNT)) >= 0) {
      p.setPermissivePhase((int) v);
    }
  }

  private static void applyPattern(JsonObject o, TrafficSignalCoordinationPlan co, String at)
      throws PlanFormatException {
    TrafficSignalCoordinationMode mode =
        enumValue(o, "mode", at + ".mode", TrafficSignalCoordinationMode.class);
    if (mode != null) {
      co.setMode(mode);
    }
    long v;
    if ((v = integer(o, "cycle", at + ".cycle", -1L, 1L, MAX_TICKS)) >= 0) {
      co.setCycleLength(v);
    }
    if ((v = ticks(o, "offset", at)) >= 0) {
      co.setOffset(v);
    }
    if (o.has("coordinatedPhases") && !o.get("coordinatedPhases").isJsonNull()) {
      co.setCoordinatedPhases(phaseList(o.get("coordinatedPhases"), at + ".coordinatedPhases"));
    }
    JsonObject splits = optObject(o, "splits", at + ".splits");
    if (splits != null) {
      long[] values = new long[TrafficSignalProgrammedPhasePlan.PHASE_COUNT + 1];
      for (Map.Entry<String, JsonElement> e : splits.entrySet()) {
        int n;
        try {
          n = Integer.parseInt(e.getKey());
        } catch (NumberFormatException ex) {
          n = -1;
        }
        if (n < 1 || n > TrafficSignalProgrammedPhasePlan.PHASE_COUNT) {
          throw new PlanFormatException(at + ".splits: \"" + e.getKey() + "\" is not a phase 1-"
              + TrafficSignalProgrammedPhasePlan.PHASE_COUNT);
        }
        values[n] = number(e.getValue(), at + ".splits." + n, 0L, MAX_TICKS);
      }
      for (int n = 1; n <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; n++) {
        co.setSplit(n, values[n]);
      }
    }
  }

  private static TrafficSignalProgrammedOverlap overlapFromJson(JsonObject o, String at)
      throws PlanFormatException {
    TrafficSignalProgrammedOverlap ov = new TrafficSignalProgrammedOverlap();
    Boolean b = bool(o, "enabled", at + ".enabled");
    if (b != null) {
      ov.setEnabled(b);
    }
    long v;
    if ((v = integer(o, "outputCircuit", at + ".outputCircuit", -2L, -1L, Integer.MAX_VALUE))
        >= -1) {
      ov.setOutputCircuitIndex((int) v);
    }
    TrafficSignalPhaseMovement m =
        enumValue(o, "outputMovement", at + ".outputMovement", TrafficSignalPhaseMovement.class);
    if (m != null) {
      ov.setOutputMovement(m);
    }
    if (o.has("included")) {
      ov.setIncludedPhases(phaseList(o.get("included"), at + ".included"));
    }
    if (o.has("permissive")) {
      ov.setPermissivePhases(phaseList(o.get("permissive"), at + ".permissive"));
    }
    if ((v = ticks(o, "trailGreen", at)) >= 0) {
      ov.setTrailGreen(v);
    }
    if ((v = ticks(o, "leadGreen", at)) >= 0) {
      ov.setLeadGreen(v);
    }
    TrafficSignalOverlapType type =
        enumValue(o, "type", at + ".type", TrafficSignalOverlapType.class);
    if (type != null) {
      ov.setType(type);
    }
    if (o.has("modifiers")) {
      ov.setModifierPhases(phaseList(o.get("modifiers"), at + ".modifiers"));
    }
    if ((v = integer(o, "callPhase", at + ".callPhase", -1L, 0L,
        TrafficSignalProgrammedPhasePlan.PHASE_COUNT)) >= 0) {
      ov.setCallPhase((int) v);
    }
    return ov;
  }

  private static TrafficSignalPreempt preemptFromJson(JsonObject o, String at)
      throws PlanFormatException {
    TrafficSignalPreempt pe = new TrafficSignalPreempt();
    Boolean b = bool(o, "enabled", at + ".enabled");
    if (b != null) {
      pe.setEnabled(b);
    }
    TrafficSignalPreemptType type =
        enumValue(o, "type", at + ".type", TrafficSignalPreemptType.class);
    if (type != null) {
      pe.setType(type);
    }
    long v;
    if ((v = integer(o, "triggerCircuit", at + ".triggerCircuit", -2L, -1L, Integer.MAX_VALUE))
        >= -1) {
      pe.setTriggerCircuitIndex((int) v);
    }
    TrafficSignalPhaseMovement m =
        enumValue(o, "triggerMovement", at + ".triggerMovement", TrafficSignalPhaseMovement.class);
    if (m == TrafficSignalPhaseMovement.PED) {
      // As the panel does: a latched button request never drops, so the dwell could never end.
      m = TrafficSignalPhaseMovement.THROUGH;
    }
    if (m != null) {
      pe.setTriggerMovement(m);
    }
    if ((b = bool(o, "triggerOnDetectors", at + ".triggerOnDetectors")) != null) {
      pe.setTriggerOnDetectors(b);
    }
    if (o.has("trackClear")) {
      pe.setTrackClearPhases(phaseList(o.get("trackClear"), at + ".trackClear"));
    }
    if (o.has("dwell")) {
      pe.setDwellPhases(phaseList(o.get("dwell"), at + ".dwell"));
    }
    if (o.has("exit")) {
      pe.setExitPhases(phaseList(o.get("exit"), at + ".exit"));
    }
    if (o.has("signCircuits")) {
      pe.setSignCircuits(intList(o.get("signCircuits"), at + ".signCircuits", 0L,
          Integer.MAX_VALUE));
    }
    if ((v = ticks(o, "minDwell", at)) >= 0) {
      pe.setMinDwell(v);
    }
    if ((v = ticks(o, "minGreenBefore", at)) >= 0) {
      pe.setMinGreenBeforePreempt(v);
    }
    return pe;
  }

  private static TrafficSignalPriorityPlan priorityFromJson(JsonObject o, String at)
      throws PlanFormatException {
    TrafficSignalPriorityPlan pri = new TrafficSignalPriorityPlan();
    Boolean b = bool(o, "enabled", at + ".enabled");
    if (b != null) {
      pri.setEnabled(b);
    }
    long v;
    if ((v = integer(o, "triggerCircuit", at + ".triggerCircuit", -2L, -1L, Integer.MAX_VALUE))
        >= -1) {
      pri.setTriggerCircuitIndex((int) v);
    }
    TrafficSignalPhaseMovement m =
        enumValue(o, "triggerMovement", at + ".triggerMovement", TrafficSignalPhaseMovement.class);
    if (m != null) {
      pri.setTriggerMovement(m);
    }
    if ((b = bool(o, "triggerOnDetectors", at + ".triggerOnDetectors")) != null) {
      pri.setTriggerOnDetectors(b);
    }
    if ((v = integer(o, "transitPhase", at + ".transitPhase", -1L, 0L,
        TrafficSignalProgrammedPhasePlan.PHASE_COUNT)) >= 0) {
      pri.setTransitPhase((int) v);
    }
    if ((v = ticks(o, "maxExtension", at)) >= 0) {
      pri.setMaxExtension(v);
    }
    if ((v = ticks(o, "maxEarlyReturn", at)) >= 0) {
      pri.setMaxEarlyReturn(v);
    }
    if ((v = integer(o, "minCycles", at + ".minCycles", -1L, 0L, 1000L)) >= 0) {
      pri.setMinCyclesBetweenGrants((int) v);
    }
    if ((v = ticks(o, "queueJump", at)) >= 0) {
      pri.setQueueJump(v);
    }
    return pri;
  }

  // endregion

  // region: typed reads

  private static String rootMessage(Throwable e) {
    Throwable t = e;
    while (t.getCause() != null && t.getCause() != t) {
      t = t.getCause();
    }
    return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
  }

  private static JsonObject object(JsonElement e, String at) throws PlanFormatException {
    if (e == null || !e.isJsonObject()) {
      throw new PlanFormatException(at + " must be an object");
    }
    return e.getAsJsonObject();
  }

  private static JsonObject optObject(JsonObject o, String key, String at)
      throws PlanFormatException {
    if (!o.has(key) || o.get(key).isJsonNull()) {
      return null;
    }
    return object(o.get(key), at);
  }

  private static JsonArray array(JsonObject o, String key, String at) throws PlanFormatException {
    if (!o.has(key) || o.get(key).isJsonNull()) {
      return null;
    }
    if (!o.get(key).isJsonArray()) {
      throw new PlanFormatException(at + " must be a list");
    }
    return o.get(key).getAsJsonArray();
  }

  private static String string(JsonObject o, String key, String at, String absent)
      throws PlanFormatException {
    if (!o.has(key) || o.get(key).isJsonNull()) {
      return absent;
    }
    JsonElement e = o.get(key);
    if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
      throw new PlanFormatException(at + " must be text");
    }
    return e.getAsString();
  }

  private static Boolean bool(JsonObject o, String key, String at) throws PlanFormatException {
    if (!o.has(key) || o.get(key).isJsonNull()) {
      return null;
    }
    JsonElement e = o.get(key);
    if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) {
      throw new PlanFormatException(at + " must be true or false");
    }
    return e.getAsBoolean();
  }

  /** A time field of {@code o}, clamped to 0..12000 ticks; -1 when absent. */
  private static long ticks(JsonObject o, String key, String at) throws PlanFormatException {
    if (!o.has(key) || o.get(key).isJsonNull()) {
      return -1L;
    }
    long v = number(o.get(key), at + "." + key, Long.MIN_VALUE, Long.MAX_VALUE);
    return Math.max(0L, Math.min(MAX_TICKS, v));
  }

  /** A whole-number field of {@code o} within [min, max]; {@code absent} when it is left out. */
  private static long integer(JsonObject o, String key, String at, long absent, long min,
      long max) throws PlanFormatException {
    if (!o.has(key) || o.get(key).isJsonNull()) {
      return absent;
    }
    return number(o.get(key), at, min, max);
  }

  private static long number(JsonElement e, String at, long min, long max)
      throws PlanFormatException {
    if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
      throw new PlanFormatException(at + " must be a number");
    }
    BigDecimal d;
    try {
      d = e.getAsBigDecimal();
    } catch (NumberFormatException ex) {
      throw new PlanFormatException(at + " must be a number");
    }
    long v;
    try {
      v = d.longValueExact();
    } catch (ArithmeticException ex) {
      throw new PlanFormatException(at + " must be a whole number, not " + d.toPlainString());
    }
    if (v < min || v > max) {
      throw new PlanFormatException(at + " is " + v + "; it must be " + min + " to " + max);
    }
    return v;
  }

  private static int[] phaseList(JsonElement e, String at) throws PlanFormatException {
    int[] phases = intList(e, at, 1L, TrafficSignalProgrammedPhasePlan.PHASE_COUNT);
    boolean[] seen = new boolean[TrafficSignalProgrammedPhasePlan.PHASE_COUNT + 1];
    for (int n : phases) {
      if (seen[n]) {
        throw new PlanFormatException(at + ": phase " + n + " is listed twice");
      }
      seen[n] = true;
    }
    return phases;
  }

  private static int[] intList(JsonElement e, String at, long min, long max)
      throws PlanFormatException {
    if (e == null || !e.isJsonArray()) {
      throw new PlanFormatException(at + " must be a list");
    }
    JsonArray a = e.getAsJsonArray();
    int[] out = new int[a.size()];
    for (int i = 0; i < a.size(); i++) {
      out[i] = (int) number(a.get(i), at + "[" + i + "]", min, max);
    }
    return out;
  }

  private static <E extends Enum<E>> E enumValue(JsonObject o, String key, String at,
      Class<E> type) throws PlanFormatException {
    String name = string(o, key, at, null);
    if (name == null) {
      return null;
    }
    for (E constant : type.getEnumConstants()) {
      if (constant.name().equals(name)) {
        return constant;
      }
    }
    StringBuilder allowed = new StringBuilder();
    for (E constant : type.getEnumConstants()) {
      allowed.append(allowed.length() == 0 ? "" : ", ").append(constant.name());
    }
    throw new PlanFormatException(at + " is \"" + name + "\"; it must be one of " + allowed);
  }

  // endregion
}
