package com.micatechnologies.minecraft.csm.technology;

/**
 * Operator-controlled override mode for {@link BlockFareGate}. Selected from the
 * Sneak+right-click GUI on the gate. Persisted on {@link TileEntityFareGate} in NBT.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public enum FareGateOpMode {
  /** Standard pay-to-enter + proximity-to-exit behavior. */
  NORMAL("Normal"),
  /** Gate is forced to OPEN_ENTRY indefinitely — no fare media needed to pass. */
  ALWAYS_OPEN("Always Open"),
  /** Gate is forced to CLOSED indefinitely — no ticket, card, or proximity opens it. */
  ALWAYS_CLOSED("Always Closed (Maintenance)");

  public final String label;

  FareGateOpMode(String label) {
    this.label = label;
  }

  /** Lookup by ordinal with bounds-check returning NORMAL for invalid network input. */
  public static FareGateOpMode fromOrdinal(int ordinal) {
    FareGateOpMode[] values = values();
    if (ordinal < 0 || ordinal >= values.length) {
      return NORMAL;
    }
    return values[ordinal];
  }
}
