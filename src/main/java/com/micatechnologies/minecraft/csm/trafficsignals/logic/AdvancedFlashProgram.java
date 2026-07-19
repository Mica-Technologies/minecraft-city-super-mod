package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Builds the pair of flash phases for a controller whose {@code ADVANCED} (ASC-3) program carries
 * per-phase {@link TrafficSignalFlashOverride}s (the MAP screen's {@code FLSH} column).
 *
 * <p>The legacy flash phases in {@link TrafficSignalPhases} decide yellow-vs-red from a circuit's
 * <em>ordinal</em> position — first circuit yellow, the rest red on alternating half-cycles. That
 * only lines up when circuits were linked in street order, which an ADVANCED intersection generally
 * is not: there the operator programs phases, and one circuit may carry several movements.
 *
 * <p>The programmed flash instead reads the phase map:
 *
 * <ul>
 *   <li><b>Alternating by barrier</b> — a phase lights on the half-cycle its barrier owns: barrier A
 *       ({@code φ1, φ2, φ5, φ6} in the standard layout — the main street and its lefts) on the
 *       first, barrier B ({@code φ3, φ4, φ7, φ8} — the side street) on the second. That reproduces
 *       the familiar main-versus-cross wig-wag, but it follows the phase map rather than the order
 *       the circuits happened to be linked in.</li>
 *   <li><b>Fail-safe</b> — any vehicle head not claimed by an overridden phase flashes red on the
 *       first half-cycle.</li>
 *   <li><b>Pedestrian facilities are dark</b> — ped signals, ped accessories and ped beacons stay
 *       off, matching flash operation at a real intersection.</li>
 *   <li><b>Beacons</b> are set solid yellow in both halves; the TESR flashes them itself (see the
 *       note in {@link TrafficSignalPhases}), so alternating them here would double-flash.</li>
 * </ul>
 *
 * <p>If two enabled phases override the same heads, the higher phase number wins.
 *
 * @author Mica Technologies
 * @see TrafficSignalProgrammedPhasePlan#hasFlashOverrides()
 * @since 2026.7
 */
public class AdvancedFlashProgram {

  private AdvancedFlashProgram() {
  }

  /**
   * Builds the two programmed flash phases.
   *
   * @param world    the world (for reading each head's block type)
   * @param circuits the controller circuits
   * @param plan     the advanced program supplying the flash overrides
   *
   * @return a two-element array: the barrier-A half-cycle followed by the barrier-B half-cycle.
   *
   * @since 2026.7
   */
  public static TrafficSignalPhase[] build(World world, TrafficSignalControllerCircuits circuits,
      TrafficSignalProgrammedPhasePlan plan) {
    // Barrier A's half-cycle carries the fail-safe red for everything the program doesn't claim;
    // barrier B's starts fully dark and is lit only by the phases assigned to it.
    TrafficSignalPhase barrierAhalf = buildBaseline(world, circuits, false);
    TrafficSignalPhase barrierBhalf = buildBaseline(world, circuits, true);

    for (int phaseNumber = 1; phaseNumber <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT;
        phaseNumber++) {
      TrafficSignalProgrammedPhase programmed = plan.getPhase(phaseNumber);
      if (programmed == null || !programmed.isActive()
          || !programmed.getFlashOverride().isOverride()
          || programmed.getCircuitIndex() >= circuits.getCircuitCount()) {
        continue;
      }
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(programmed.getCircuitIndex());
      List<BlockPos> heads = movementHeads(circuit, programmed.getMovement());
      if (heads.isEmpty()) {
        continue;
      }

      // The phase lights on its own barrier's half-cycle and is dark on the other.
      boolean onBarrierA = Math.floorMod(programmed.getBarrier(), 2) == 0;
      TrafficSignalPhase litHalf = onBarrierA ? barrierAhalf : barrierBhalf;
      TrafficSignalPhase darkHalf = onBarrierA ? barrierBhalf : barrierAhalf;

      // Heads whose block type doesn't flash are forced off, exactly as in the legacy flash phases.
      Tuple<List<BlockPos>, List<BlockPos>> filtered
          = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash(world, heads);
      litHalf.removeSignals(heads);
      switch (programmed.getFlashOverride()) {
        case YELLOW:
          litHalf.addYellowSignals(filtered.getFirst());
          break;
        case RED:
          litHalf.addRedSignals(filtered.getFirst());
          break;
        case DARK:
        default:
          litHalf.addOffSignals(filtered.getFirst());
          break;
      }
      litHalf.addOffSignals(filtered.getSecond());

      // Explicitly dark on the other half — a barrier-B phase has to clear the fail-safe red that
      // barrier A's baseline put on its heads.
      darkHalf.removeSignals(heads);
      darkHalf.addOffSignals(heads);
    }

    return new TrafficSignalPhase[] {barrierAhalf, barrierBhalf};
  }

  /**
   * Builds one half-cycle before overrides are applied: pedestrian facilities off, beacons solid
   * yellow, blankout signs off, and every vehicle head either red — the fail-safe default for
   * anything the program doesn't claim, carried on barrier A's half — or off.
   */
  private static TrafficSignalPhase buildBaseline(World world,
      TrafficSignalControllerCircuits circuits, boolean dark) {
    TrafficSignalPhase phase = new TrafficSignalPhase(TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE,
        null, TrafficSignalPhaseApplicability.NONE);
    for (int i = 0; i < circuits.getCircuitCount(); i++) {
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(i);

      List<BlockPos> vehicleHeads = new ArrayList<>();
      vehicleHeads.addAll(circuit.getThroughSignals());
      vehicleHeads.addAll(circuit.getLeftSignals());
      vehicleHeads.addAll(circuit.getRightSignals());
      vehicleHeads.addAll(circuit.getProtectedSignals());
      vehicleHeads.addAll(circuit.getFlashingLeftSignals());
      vehicleHeads.addAll(circuit.getFlashingRightSignals());
      if (dark) {
        phase.addOffSignals(vehicleHeads);
      } else {
        Tuple<List<BlockPos>, List<BlockPos>> filtered
            = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash(world, vehicleHeads);
        phase.addRedSignals(filtered.getFirst());
        phase.addOffSignals(filtered.getSecond());
      }

      // Beacons stay solid yellow in both halves — the renderer does their flashing.
      phase.addYellowSignals(circuit.getBeaconSignals());
      phase.addOffSignals(circuit.getPedestrianSignals());
      phase.addOffSignals(circuit.getPedestrianAccessorySignals());
      phase.addOffSignals(circuit.getPedestrianBeaconSignals());
      phase.addOffSignals(circuit.getNoTurnBlankoutSignals());
    }
    return phase;
  }

  /**
   * Resolves the heads a programmed phase drives, mirroring
   * {@code AdvancedPhaseBuilder.applyServed}: a through phase also carries its concurrent right
   * turn, and a left phase carries both the protected and permissive-left lenses. A pedestrian
   * phase has no vehicle movement of its own, so it drives nothing in flash.
   */
  private static List<BlockPos> movementHeads(TrafficSignalControllerCircuit circuit,
      TrafficSignalPhaseMovement movement) {
    List<BlockPos> heads = new ArrayList<>();
    switch (movement) {
      case THROUGH:
        heads.addAll(circuit.getThroughSignals());
        heads.addAll(circuit.getRightSignals());
        heads.addAll(circuit.getFlashingRightSignals());
        break;
      case LEFT:
      case PROTECTED_LEFT:
        heads.addAll(circuit.getProtectedSignals());
        heads.addAll(circuit.getLeftSignals());
        heads.addAll(circuit.getFlashingLeftSignals());
        break;
      case RIGHT:
        heads.addAll(circuit.getRightSignals());
        heads.addAll(circuit.getFlashingRightSignals());
        break;
      case PED:
      default:
        break;
    }
    return heads;
  }
}
