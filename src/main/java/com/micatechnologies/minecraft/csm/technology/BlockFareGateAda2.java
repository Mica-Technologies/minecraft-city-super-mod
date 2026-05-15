package com.micatechnologies.minecraft.csm.technology;

/**
 * Two-cell-wide ADA fare gate. Extends one cell in the +X direction from the placed cell
 * (in default-N orientation; the engine rotates per FACING). Same state machine, fare-
 * validation logic, proximity-detection logic, and tile-entity type as {@link BlockFareGate}
 * — only the {@link #getLaneWidthCells()} return value and the registry name differ.
 *
 * <p>Players place this block at the LEFT side of the desired 2-wide gate footprint; the
 * model and bounding box extend into the cell immediately to the gate's right. The cell
 * the gate extends into doesn't need to be empty at placement time — the gate's collision
 * just adds to whatever is there.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class BlockFareGateAda2 extends BlockFareGate {

  @Override
  public String getBlockRegistryName() {
    return "fare_gate_ada_2";
  }

  @Override
  protected int getLaneWidthCells() {
    return 2;
  }
}
