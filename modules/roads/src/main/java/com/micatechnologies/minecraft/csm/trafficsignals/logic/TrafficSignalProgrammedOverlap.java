package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.nbt.NBTTagCompound;

/**
 * A phase-based vehicle overlap for {@code ADVANCED} (NEMA) mode, modeled on the ASC/3 MM-2-2
 * <b>NORMAL</b> overlap type: a set of signal heads (identified by an output circuit + movement)
 * that runs <b>green whenever any of its included phases is green</b>, follows them through yellow
 * clearance, and is otherwise red.
 *
 * <p>The classic use is a <b>right-turn overlap</b> — e.g. a right turn that may run not only with
 * its own through but also with the non-conflicting opposing left turn. Include both phases and the
 * right-turn head greens for either.
 *
 * <p>This is distinct from the controller-wide {@link TrafficSignalControllerOverlaps} signal-to-
 * signal "green follows green" map (which is applied in both NORMAL and ADVANCED modes); programmed
 * overlaps are part of the ADVANCED plan and are phase-driven. FYA (protected/permissive left turns)
 * is handled by the per-phase {@code permissivePhase} mechanism, not here — see
 * {@code assets/docs/ADVANCED_MODE_ASC3.md}.
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public class TrafficSignalProgrammedOverlap {

  private static final String K_ENABLED = "en";
  private static final String K_OUT_CIRCUIT = "oc";
  private static final String K_OUT_MOVEMENT = "om";
  private static final String K_INCLUDED = "in";
  private static final String K_TRAIL_GREEN = "tg";
  private static final String K_LEAD_GREEN = "lg";
  private static final String K_TYPE = "ty";
  private static final String K_MODIFIER = "md";
  private static final String K_CALL_PHASE = "cp";
  private static final String K_PERMISSIVE = "pm";

  private boolean enabled = false;
  /** Overlap type (NORMAL or -GRN/YEL). */
  private TrafficSignalOverlapType type = TrafficSignalOverlapType.NORMAL;
  /** Circuit whose signal heads this overlap drives, or -1 if unassigned. */
  private int outputCircuitIndex = -1;
  /** Which of the output circuit's signal lists this overlap drives (typically RIGHT). */
  private TrafficSignalPhaseMovement outputMovement = TrafficSignalPhaseMovement.RIGHT;
  /** Phases whose green/yellow drives this overlap. */
  private int[] includedPhases = new int[0];
  /**
   * ASC/3 lag (trailing) green: the overlap holds green this many ticks after its included phases
   * leave green, to clear turning vehicles. The included phases' yellow/red then provide clearance.
   * 0 = no trailing green.
   */
  private long trailGreen = 0L;
  /**
   * ASC/3 lead (advance) green: the overlap goes green this many ticks <i>before</i> an included
   * phase greens, during the preceding all-red clearance (within-barrier only). As on a real
   * controller, the operator is responsible for keeping the lead within the preceding clearance.
   * 0 = no lead green.
   */
  private long leadGreen = 0L;
  /** Modifier phases for {@link TrafficSignalOverlapType#MINUS_GREEN_YELLOW}: the overlap is red
   * while any of these is green or yellow. */
  private int[] modifierPhases = new int[0];
  /**
   * Detector-to-phase assignment: the phase called while vehicles are detected in the output
   * circuit's output-movement sensor zone. Overlaps are otherwise pure outputs, so a movement
   * served only by an overlap (e.g. a right-turn pocket) generates no demand of its own — this
   * lets it summon one of its included phases. 0 = no call.
   */
  private int callPhase = 0;
  /**
   * FYA permissive phases: while any of these is green (and no included phase is), the overlap
   * shows a flashing yellow arrow instead of red — the right-turn FYA (e.g. flash during the
   * adjacent through whose concurrent WALK conflicts, protected green arrow during the included
   * compatible cross-street left). Empty = no permissive display (classic green/yellow/red
   * overlap).
   */
  private int[] permissivePhases = new int[0];

  // region: accessors

  public TrafficSignalOverlapType getType() {
    return type;
  }

  public void setType(TrafficSignalOverlapType type) {
    this.type = type;
  }

  public int[] getModifierPhases() {
    return modifierPhases;
  }

  public void setModifierPhases(int[] modifierPhases) {
    this.modifierPhases = modifierPhases == null ? new int[0] : modifierPhases;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /** @return whether this overlap is enabled and assigned to an output circuit. */
  public boolean isActive() {
    return enabled && outputCircuitIndex >= 0;
  }

  public int getOutputCircuitIndex() {
    return outputCircuitIndex;
  }

  public void setOutputCircuitIndex(int outputCircuitIndex) {
    this.outputCircuitIndex = outputCircuitIndex;
  }

  public TrafficSignalPhaseMovement getOutputMovement() {
    return outputMovement;
  }

  public void setOutputMovement(TrafficSignalPhaseMovement outputMovement) {
    this.outputMovement = outputMovement;
  }

  public int[] getIncludedPhases() {
    return includedPhases;
  }

  public void setIncludedPhases(int[] includedPhases) {
    this.includedPhases = includedPhases == null ? new int[0] : includedPhases;
  }

  public long getTrailGreen() {
    return trailGreen;
  }

  public void setTrailGreen(long trailGreen) {
    this.trailGreen = trailGreen;
  }

  public long getLeadGreen() {
    return leadGreen;
  }

  public void setLeadGreen(long leadGreen) {
    this.leadGreen = leadGreen;
  }

  public int getCallPhase() {
    return callPhase;
  }

  public void setCallPhase(int callPhase) {
    this.callPhase = Math.max(0, callPhase);
  }

  public int[] getPermissivePhases() {
    return permissivePhases;
  }

  public void setPermissivePhases(int[] permissivePhases) {
    this.permissivePhases = permissivePhases == null ? new int[0] : permissivePhases;
  }

  // endregion

  // region: NBT

  public NBTTagCompound toNBT() {
    NBTTagCompound c = new NBTTagCompound();
    c.setBoolean(K_ENABLED, enabled);
    c.setInteger(K_OUT_CIRCUIT, outputCircuitIndex);
    c.setInteger(K_OUT_MOVEMENT, outputMovement.toNBT());
    c.setIntArray(K_INCLUDED, includedPhases);
    c.setLong(K_TRAIL_GREEN, trailGreen);
    c.setLong(K_LEAD_GREEN, leadGreen);
    c.setInteger(K_TYPE, type.toNBT());
    c.setIntArray(K_MODIFIER, modifierPhases);
    if (callPhase > 0) {
      c.setInteger(K_CALL_PHASE, callPhase);
    }
    c.setIntArray(K_PERMISSIVE, permissivePhases);
    return c;
  }

  public static TrafficSignalProgrammedOverlap fromNBT(NBTTagCompound c) {
    TrafficSignalProgrammedOverlap o = new TrafficSignalProgrammedOverlap();
    o.enabled = c.getBoolean(K_ENABLED);
    o.outputCircuitIndex = c.hasKey(K_OUT_CIRCUIT) ? c.getInteger(K_OUT_CIRCUIT) : -1;
    o.outputMovement = TrafficSignalPhaseMovement.fromNBT(c.getInteger(K_OUT_MOVEMENT));
    o.includedPhases = c.hasKey(K_INCLUDED) ? c.getIntArray(K_INCLUDED) : new int[0];
    o.trailGreen = c.hasKey(K_TRAIL_GREEN) ? c.getLong(K_TRAIL_GREEN) : 0L;
    o.leadGreen = c.hasKey(K_LEAD_GREEN) ? c.getLong(K_LEAD_GREEN) : 0L;
    o.type = TrafficSignalOverlapType.fromNBT(c.getInteger(K_TYPE));
    o.modifierPhases = c.hasKey(K_MODIFIER) ? c.getIntArray(K_MODIFIER) : new int[0];
    o.callPhase = c.hasKey(K_CALL_PHASE) ? c.getInteger(K_CALL_PHASE) : 0;
    o.permissivePhases = c.hasKey(K_PERMISSIVE) ? c.getIntArray(K_PERMISSIVE) : new int[0];
    return o;
  }

  // endregion
}
