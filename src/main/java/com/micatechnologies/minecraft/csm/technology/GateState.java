package com.micatechnologies.minecraft.csm.technology;

import net.minecraft.util.IStringSerializable;

/**
 * Three-way state of a {@link BlockFareGate}, used as the value type for the block's
 * {@code state} property. Replaces the earlier OPEN boolean so the open-from-interior
 * "exit in progress" indication can render distinctly from the regular open-from-exterior
 * "entry approved" indication.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public enum GateState implements IStringSerializable {

  /** Default. Barrier is solid; exterior arrow shows red. */
  CLOSED("closed"),
  /** Opened by a player presenting valid fare media on the exterior side; arrow shows green. */
  OPEN_ENTRY("open_entry"),
  /** Opened by proximity detection on the interior side (player exiting); arrow shows red X. */
  OPEN_EXIT("open_exit"),
  /**
   * Operator-locked closed (Always Closed override mode). Barrier blocks just like CLOSED,
   * but renders the red X indicator so anyone approaching sees that the gate is intentionally
   * out of service rather than just waiting for a fare scan.
   */
  CLOSED_LOCKED("closed_locked");

  private final String name;

  GateState(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  public boolean isOpen() {
    return this == OPEN_ENTRY || this == OPEN_EXIT;
  }
}
