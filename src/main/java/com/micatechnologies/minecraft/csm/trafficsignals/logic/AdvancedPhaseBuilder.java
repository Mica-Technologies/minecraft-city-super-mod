package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Translates the live ring-and-barrier state of {@link RingBarrierState} into a concrete
 * {@link TrafficSignalPhase} that can be applied to the world.
 *
 * <p>The build starts from an all-red / don't-walk baseline across every circuit, then lights the
 * movements being served by each ring at their current interval colour, applies pedestrian Walk /
 * FDW, and finally applies the controller's overlap map (an overlap target follows its green
 * source). This reuses the existing {@link TrafficSignalPhase} signal-state lists and
 * {@link TrafficSignalPhase#apply(World)} machinery — no new signal-setting code.
 *
 * <p>NOTE (v1 limitation): left movements are shown as protected-or-red. Permissive flashing-yellow
 * (FYA) arbitration in ADVANCED mode is a planned refinement; for now a left phase that isn't being
 * served stays red rather than flashing yellow.
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public final class AdvancedPhaseBuilder {

  private AdvancedPhaseBuilder() {
  }

  /**
   * Builds the phase for the current ring state.
   *
   * @param world    the world
   * @param plan     the programmed phase plan
   * @param circuits the controller circuits
   * @param overlaps the controller overlap map
   * @param ring1    what ring 1 is serving (or {@code null} if ring 1 is dark/all-red)
   * @param ring2    what ring 2 is serving (or {@code null})
   *
   * @return a fully-populated {@link TrafficSignalPhase}
   */
  public static TrafficSignalPhase build(World world,
      TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits,
      TrafficSignalControllerOverlaps overlaps,
      RingBarrierState.ServedMovement ring1,
      RingBarrierState.ServedMovement ring2) {
    TrafficSignalPhase phase = redBaseline(circuits);
    applyServed(phase, plan, circuits, ring1);
    applyServed(phase, plan, circuits, ring2);
    applyOverlaps(phase, overlaps);
    return phase;
  }

  /**
   * Builds a phase that lights a given set of NEMA phases at one interval, with everything else red
   * — used by preemption's track-clear / dwell stages. An empty {@code phaseNumbers} yields an
   * all-red phase (used for preempt enter/exit clearance).
   *
   * @param phaseNumbers the phases to serve
   * @param interval     the vehicle interval to display them at
   */
  public static TrafficSignalPhase buildForPhases(World world,
      TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits,
      TrafficSignalControllerOverlaps overlaps,
      Collection<Integer> phaseNumbers,
      RingBarrierState.VehInterval interval) {
    TrafficSignalPhase phase = redBaseline(circuits);
    for (int n : phaseNumbers) {
      applyServed(phase, plan, circuits,
          new RingBarrierState.ServedMovement(n, interval, RingBarrierState.PedInterval.NONE));
    }
    if (interval == RingBarrierState.VehInterval.GREEN) {
      applyOverlaps(phase, overlaps);
    }
    return phase;
  }

  /** Creates the all-red / don't-walk baseline across every circuit. */
  private static TrafficSignalPhase redBaseline(TrafficSignalControllerCircuits circuits) {
    TrafficSignalPhase phase = new TrafficSignalPhase(
        TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, TrafficSignalPhaseApplicability.NONE);
    for (int i = 0; i < circuits.getCircuitCount(); i++) {
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(i);
      phase.addRedSignals(circuit.getThroughSignals());
      phase.addRedSignals(circuit.getLeftSignals());
      phase.addRedSignals(circuit.getRightSignals());
      phase.addRedSignals(circuit.getProtectedSignals());
      phase.addRedSignals(circuit.getFlashingLeftSignals());
      phase.addRedSignals(circuit.getFlashingRightSignals());
      phase.addDontWalkSignals(circuit.getPedestrianSignals());
      phase.addDontWalkSignals(circuit.getPedestrianAccessorySignals());
      phase.addDontWalkSignals(circuit.getPedestrianBeaconSignals());
      phase.addOffSignals(circuit.getBeaconSignals());
      phase.addOffSignals(circuit.getNoTurnBlankoutSignals());
    }
    return phase;
  }

  /** Applies the overlap map: any overlap target of a now-green signal follows it to green. */
  private static void applyOverlaps(TrafficSignalPhase phase,
      TrafficSignalControllerOverlaps overlaps) {
    List<BlockPos> greenSnapshot = new ArrayList<>(phase.getGreenSignals());
    for (BlockPos source : greenSnapshot) {
      List<BlockPos> targets = overlaps.getOverlapsForSource(source);
      if (targets != null) {
        for (BlockPos target : targets) {
          phase.moveOverlapSignalToGreen(target);
        }
      }
    }
  }

  /**
   * Reassigns the signals of the movement served by {@code served} from the red baseline to their
   * current interval colour, plus pedestrian Walk/FDW where applicable.
   */
  private static void applyServed(TrafficSignalPhase phase,
      TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits,
      RingBarrierState.ServedMovement served) {
    if (served == null) {
      return;
    }
    TrafficSignalProgrammedPhase programmed = plan.getPhase(served.phaseNumber);
    if (programmed == null || programmed.getCircuitIndex() < 0
        || programmed.getCircuitIndex() >= circuits.getCircuitCount()) {
      return;
    }
    TrafficSignalControllerCircuit circuit = circuits.getCircuit(programmed.getCircuitIndex());

    // Vehicle signals for the movement.
    List<BlockPos> vehicleSignals = new ArrayList<>();
    switch (programmed.getMovement()) {
      case THROUGH:
        vehicleSignals.addAll(circuit.getThroughSignals());
        // A through phase also serves the concurrent right turn.
        vehicleSignals.addAll(circuit.getRightSignals());
        break;
      case LEFT:
      case PROTECTED_LEFT:
        vehicleSignals.addAll(circuit.getProtectedSignals());
        vehicleSignals.addAll(circuit.getLeftSignals());
        break;
      case RIGHT:
        vehicleSignals.addAll(circuit.getRightSignals());
        break;
      case PED:
        // Pedestrian-only phase: no vehicle movement of its own.
        break;
      default:
        break;
    }

    if (!vehicleSignals.isEmpty()) {
      phase.removeSignals(vehicleSignals);
      switch (served.vehicle) {
        case GREEN:
          phase.addGreenSignals(vehicleSignals);
          break;
        case YELLOW:
          phase.addYellowSignals(vehicleSignals);
          break;
        case RED:
        default:
          phase.addRedSignals(vehicleSignals);
          break;
      }
    }

    // Pedestrian interval (for through / ped movements).
    if (served.pedestrian != RingBarrierState.PedInterval.NONE) {
      List<BlockPos> peds = new ArrayList<>(circuit.getPedestrianSignals());
      peds.addAll(circuit.getPedestrianAccessorySignals());
      phase.removeSignals(peds);
      switch (served.pedestrian) {
        case WALK:
          phase.addWalkSignals(peds);
          break;
        case FDW:
          phase.addFlashDontWalkSignals(peds);
          break;
        case DONT_WALK:
        default:
          phase.addDontWalkSignals(peds);
          break;
      }
    }
  }
}
