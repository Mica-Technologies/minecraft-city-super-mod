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
 * <p>Protected/permissive left turns are supported via the ASC/3 PPLT FYA model: a left phase with a
 * {@code permissivePhase} (opposing-through phase) shows a flashing yellow arrow while that opposing
 * through is green, a solid green/yellow arrow during its own protected phase, and red otherwise.
 * See {@code assets/docs/ADVANCED_MODE_ASC3.md}.
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
    applyFlashingYellowArrows(phase, plan, circuits, ring1, ring2);
    applyOverlaps(phase, overlaps);
    applyProgrammedOverlaps(phase, plan, circuits, ring1, ring2);
    return phase;
  }

  /**
   * Applies the plan's phase-based vehicle overlaps (ASC/3 NORMAL overlap type). Each active overlap
   * drives its output circuit+movement heads green while any included phase is green, yellow while an
   * included phase is in yellow clearance (and none green), and red otherwise. The classic use is a
   * right-turn overlap that runs with both its own through and a non-conflicting opposing left.
   */
  private static void applyProgrammedOverlaps(TrafficSignalPhase phase,
      TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits,
      RingBarrierState.ServedMovement ring1,
      RingBarrierState.ServedMovement ring2) {
    for (TrafficSignalProgrammedOverlap ov : plan.getVehicleOverlaps()) {
      if (!ov.isActive() || ov.getOutputCircuitIndex() >= circuits.getCircuitCount()) {
        continue;
      }
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(ov.getOutputCircuitIndex());
      List<BlockPos> heads = overlapOutputHeads(circuit, ov.getOutputMovement());
      if (heads.isEmpty()) {
        continue;
      }
      RingBarrierState.VehInterval state = overlapState(ov.getIncludedPhases(), ring1, ring2);
      phase.removeSignals(heads);
      switch (state) {
        case GREEN:
          phase.addGreenSignals(heads);
          break;
        case YELLOW:
          phase.addYellowSignals(heads);
          break;
        case RED:
        default:
          phase.addRedSignals(heads);
          break;
      }
    }
  }

  /**
   * Pure overlap-interval decision (world-free, unit-testable): green if any included phase is
   * green, else yellow if any included phase is in yellow clearance, else red.
   */
  static RingBarrierState.VehInterval overlapState(int[] includedPhases,
      RingBarrierState.ServedMovement ring1, RingBarrierState.ServedMovement ring2) {
    boolean anyYellow = false;
    for (int p : includedPhases) {
      RingBarrierState.VehInterval iv = servedInterval(p, ring1, ring2);
      if (iv == RingBarrierState.VehInterval.GREEN) {
        return RingBarrierState.VehInterval.GREEN;
      }
      if (iv == RingBarrierState.VehInterval.YELLOW) {
        anyYellow = true;
      }
    }
    return anyYellow ? RingBarrierState.VehInterval.YELLOW : RingBarrierState.VehInterval.RED;
  }

  /** Resolves an overlap's output signal heads from its circuit and movement. */
  private static List<BlockPos> overlapOutputHeads(TrafficSignalControllerCircuit circuit,
      TrafficSignalPhaseMovement movement) {
    switch (movement) {
      case RIGHT:
        return circuit.getRightSignals();
      case THROUGH:
        return circuit.getThroughSignals();
      case LEFT:
      case PROTECTED_LEFT: {
        List<BlockPos> combined = new ArrayList<>(circuit.getLeftSignals());
        combined.addAll(circuit.getProtectedSignals());
        return combined;
      }
      case PED:
      default:
        return new ArrayList<>();
    }
  }

  /**
   * Applies ASC/3 PPLT FYA permissive-left indications. For each active LEFT/PROTECTED_LEFT phase
   * configured with a {@code permissivePhase} (the opposing-through phase), drives the FYA lens
   * ({@link TrafficSignalControllerCircuit#getFlashingLeftSignals()}):
   * <ul>
   *   <li>the left phase is being served (protected) &rarr; FYA lens off (the solid arrow shows
   *       green/yellow from {@link #applyServed});</li>
   *   <li>else the opposing-through phase is green (permissive) &rarr; FYA lens flashing yellow,
   *       solid green-arrow lens red;</li>
   *   <li>else &rarr; FYA lens red.</li>
   * </ul>
   * This mirrors the normal-mode left-turn convention so both modes render FYA identically.
   */
  private static void applyFlashingYellowArrows(TrafficSignalPhase phase,
      TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits,
      RingBarrierState.ServedMovement ring1,
      RingBarrierState.ServedMovement ring2) {
    for (TrafficSignalProgrammedPhase p : plan.getPhases()) {
      if (!p.isActive() || p.getPermissivePhase() <= 0) {
        continue;
      }
      if (p.getMovement() != TrafficSignalPhaseMovement.LEFT
          && p.getMovement() != TrafficSignalPhaseMovement.PROTECTED_LEFT) {
        continue;
      }
      if (p.getCircuitIndex() < 0 || p.getCircuitIndex() >= circuits.getCircuitCount()) {
        continue;
      }
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(p.getCircuitIndex());
      if (circuit.getFlashingLeftSignals().isEmpty()) {
        continue; // no FYA lens to drive
      }
      RingBarrierState.VehInterval servedThis =
          servedInterval(p.getPhaseNumber(), ring1, ring2);
      boolean permissiveGreen =
          servedInterval(p.getPermissivePhase(), ring1, ring2) == RingBarrierState.VehInterval.GREEN;
      applyFyaLensState(phase, circuit.getFlashingLeftSignals(), circuit.getLeftSignals(),
          servedThis, permissiveGreen);
    }
  }

  /**
   * Pure FYA-lens assignment (world-free, unit-testable). {@code servedThis} is the interval the
   * left phase itself is being served at this tick (or {@code null} if it isn't being served);
   * {@code permissiveGreen} is whether its opposing-through phase is green.
   */
  static void applyFyaLensState(TrafficSignalPhase phase, List<BlockPos> fyaLens,
      List<BlockPos> solidArrowLens, RingBarrierState.VehInterval servedThis,
      boolean permissiveGreen) {
    phase.removeSignals(fyaLens);
    boolean protectedServed = servedThis == RingBarrierState.VehInterval.GREEN
        || servedThis == RingBarrierState.VehInterval.YELLOW;
    if (protectedServed) {
      // Protected: the solid arrow shows green/yellow (set by applyServed); FYA lens stays dark.
      phase.addOffSignals(fyaLens);
    } else if (permissiveGreen) {
      // Permissive: flashing yellow on the FYA lens; the solid green-arrow lens is red.
      phase.addFyaSignals(fyaLens);
      phase.removeSignals(solidArrowLens);
      phase.addRedSignals(solidArrowLens);
    } else {
      phase.addRedSignals(fyaLens);
    }
  }

  /** The interval the given phase is served at this tick by either ring, or {@code null}. */
  private static RingBarrierState.VehInterval servedInterval(int phaseNumber,
      RingBarrierState.ServedMovement ring1, RingBarrierState.ServedMovement ring2) {
    if (ring1 != null && ring1.phaseNumber == phaseNumber) {
      return ring1.vehicle;
    }
    if (ring2 != null && ring2.phaseNumber == phaseNumber) {
      return ring2.vehicle;
    }
    return null;
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
