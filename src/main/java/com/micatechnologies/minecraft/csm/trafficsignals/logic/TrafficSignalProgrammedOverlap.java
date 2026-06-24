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

  private boolean enabled = false;
  /** Circuit whose signal heads this overlap drives, or -1 if unassigned. */
  private int outputCircuitIndex = -1;
  /** Which of the output circuit's signal lists this overlap drives (typically RIGHT). */
  private TrafficSignalPhaseMovement outputMovement = TrafficSignalPhaseMovement.RIGHT;
  /** Phases whose green/yellow drives this overlap. */
  private int[] includedPhases = new int[0];

  // region: accessors

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

  // endregion

  // region: NBT

  public NBTTagCompound toNBT() {
    NBTTagCompound c = new NBTTagCompound();
    c.setBoolean(K_ENABLED, enabled);
    c.setInteger(K_OUT_CIRCUIT, outputCircuitIndex);
    c.setInteger(K_OUT_MOVEMENT, outputMovement.toNBT());
    c.setIntArray(K_INCLUDED, includedPhases);
    return c;
  }

  public static TrafficSignalProgrammedOverlap fromNBT(NBTTagCompound c) {
    TrafficSignalProgrammedOverlap o = new TrafficSignalProgrammedOverlap();
    o.enabled = c.getBoolean(K_ENABLED);
    o.outputCircuitIndex = c.hasKey(K_OUT_CIRCUIT) ? c.getInteger(K_OUT_CIRCUIT) : -1;
    o.outputMovement = TrafficSignalPhaseMovement.fromNBT(c.getInteger(K_OUT_MOVEMENT));
    o.includedPhases = c.hasKey(K_INCLUDED) ? c.getIntArray(K_INCLUDED) : new int[0];
    return o;
  }

  // endregion
}
