package com.micatechnologies.minecraft.csm.technology;

/**
 * Three-cell-wide ADA fare gate. Extends one cell in EACH horizontal direction from the
 * placed cell (in default-N orientation; the engine rotates per FACING) — the placed cell
 * is the middle of the gate, with one cell to either side covered by the model + bbox.
 *
 * <p>Same state machine, fare-validation logic, proximity-detection logic, and tile-entity
 * type as {@link BlockFareGate}. Players place this block at the MIDDLE of the desired
 * 3-wide gate footprint.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class BlockFareGateAda3 extends BlockFareGate {

  @Override
  public String getBlockRegistryName() {
    return "fare_gate_ada_3";
  }

  @Override
  protected int getLaneWidthCells() {
    return 3;
  }
}
