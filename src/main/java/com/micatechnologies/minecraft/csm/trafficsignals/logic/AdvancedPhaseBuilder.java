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
    return build(world, plan, circuits, overlaps, ring1, ring2, null);
  }

  /**
   * Build overload that takes precomputed effective intervals for the plan's vehicle overlaps (by
   * overlap index) — used by {@link RingBarrierState} to apply lag (trailing) green, which needs
   * cross-tick state. A {@code null} entry (or a {@code null}/short list) falls back to the
   * stateless {@link #overlapState} decision.
   */
  public static TrafficSignalPhase build(World world,
      TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits,
      TrafficSignalControllerOverlaps overlaps,
      RingBarrierState.ServedMovement ring1,
      RingBarrierState.ServedMovement ring2,
      List<RingBarrierState.VehInterval> overlapIntervals) {
    return build(world, plan, circuits, overlaps, ring1, ring2, overlapIntervals, null);
  }

  /**
   * Build overload that also takes per-phase resolved FYA compound-head states (by left-phase
   * number) from {@link RingBarrierState}, which carry the stateful FLASH&rarr;solid-yellow&rarr;red
   * clearance. A {@code null} map (or a phase missing from it) falls back to the stateless
   * {@link #baseFyaState} decision.
   */
  public static TrafficSignalPhase build(World world,
      TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits,
      TrafficSignalControllerOverlaps overlaps,
      RingBarrierState.ServedMovement ring1,
      RingBarrierState.ServedMovement ring2,
      List<RingBarrierState.VehInterval> overlapIntervals,
      java.util.Map<Integer, FyaLensState> fyaResolved) {
    TrafficSignalPhase phase = redBaseline(circuits);
    applyServed(phase, plan, circuits, ring1);
    applyServed(phase, plan, circuits, ring2);
    applyFyaLenses(phase, plan, circuits, ring1, ring2, fyaResolved);
    applyOverlaps(phase, overlaps);
    applyProgrammedOverlaps(phase, plan, circuits, ring1, ring2, overlapIntervals);
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
      RingBarrierState.ServedMovement ring2,
      List<RingBarrierState.VehInterval> overlapIntervals) {
    List<TrafficSignalProgrammedOverlap> ovs = plan.getVehicleOverlaps();
    for (int i = 0; i < ovs.size(); i++) {
      TrafficSignalProgrammedOverlap ov = ovs.get(i);
      if (!ov.isActive() || ov.getOutputCircuitIndex() >= circuits.getCircuitCount()) {
        continue;
      }
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(ov.getOutputCircuitIndex());
      List<BlockPos> heads = overlapOutputHeads(circuit, ov.getOutputMovement());
      if (heads.isEmpty()) {
        continue;
      }
      if (ov.getOutputMovement() == TrafficSignalPhaseMovement.PED) {
        // Pedestrian overlap: the ped head walks/clears with its included phases' ped intervals.
        phase.removeSignals(heads);
        switch (overlapPedState(ov.getIncludedPhases(), ring1, ring2)) {
          case WALK:
            phase.addWalkSignals(heads);
            break;
          case FDW:
            phase.addFlashDontWalkSignals(heads);
            break;
          case DONT_WALK:
          default:
            phase.addDontWalkSignals(heads);
            break;
        }
        continue;
      }
      RingBarrierState.VehInterval precomputed =
          overlapIntervals != null && i < overlapIntervals.size() ? overlapIntervals.get(i) : null;
      RingBarrierState.VehInterval state =
          precomputed != null ? precomputed : overlapState(ov.getIncludedPhases(), ring1, ring2);
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
   * Pure pedestrian-overlap decision: WALK if any included phase is serving WALK, else FDW (flashing
   * don't walk) if any is in ped clearance, else DON'T WALK.
   */
  static RingBarrierState.PedInterval overlapPedState(int[] includedPhases,
      RingBarrierState.ServedMovement ring1, RingBarrierState.ServedMovement ring2) {
    boolean anyFdw = false;
    for (int p : includedPhases) {
      RingBarrierState.PedInterval pi = servedPed(p, ring1, ring2);
      if (pi == RingBarrierState.PedInterval.WALK) {
        return RingBarrierState.PedInterval.WALK;
      }
      if (pi == RingBarrierState.PedInterval.FDW) {
        anyFdw = true;
      }
    }
    return anyFdw ? RingBarrierState.PedInterval.FDW : RingBarrierState.PedInterval.DONT_WALK;
  }

  /** The pedestrian interval the given phase is served with this tick, or NONE. */
  private static RingBarrierState.PedInterval servedPed(int phaseNumber,
      RingBarrierState.ServedMovement ring1, RingBarrierState.ServedMovement ring2) {
    if (ring1 != null && ring1.phaseNumber == phaseNumber) {
      return ring1.pedestrian;
    }
    if (ring2 != null && ring2.phaseNumber == phaseNumber) {
      return ring2.pedestrian;
    }
    return RingBarrierState.PedInterval.NONE;
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
      case PED: {
        List<BlockPos> peds = new ArrayList<>(circuit.getPedestrianSignals());
        peds.addAll(circuit.getPedestrianAccessorySignals());
        return peds;
      }
      default:
        return new ArrayList<>();
    }
  }

  /** The four compound FYA-head indications (3-section lens + 1-section green-arrow add-on). */
  public enum FyaLensState { PROTECTED_GREEN, SOLID_YELLOW, FLASH, RED }

  /**
   * The stateless (instantaneous) FYA-head indication for a PPLT left, from the left phase's own
   * served interval and its opposing-through ("permissive") interval. FLASH&harr;RED clearance
   * sequencing is layered on statefully by {@link RingBarrierState}; this is the base decision.
   *
   * <ul>
   *   <li>own phase served GREEN &rarr; protected green arrow;</li>
   *   <li>own phase served YELLOW &rarr; solid yellow arrow (protected clearance);</li>
   *   <li>own phase served RED, or not served with permissive closed &rarr; red arrow;</li>
   *   <li>not served, opposing through green or in yellow clearance &rarr; flashing yellow.</li>
   * </ul>
   */
  static FyaLensState baseFyaState(RingBarrierState.VehInterval servedThis,
      RingBarrierState.VehInterval permInterval, boolean hasPermissive) {
    if (servedThis == RingBarrierState.VehInterval.GREEN) {
      return FyaLensState.PROTECTED_GREEN;
    }
    if (servedThis == RingBarrierState.VehInterval.YELLOW) {
      return FyaLensState.SOLID_YELLOW;
    }
    if (servedThis == RingBarrierState.VehInterval.RED) {
      return FyaLensState.RED;
    }
    boolean permissive = hasPermissive && (permInterval == RingBarrierState.VehInterval.GREEN
        || permInterval == RingBarrierState.VehInterval.YELLOW);
    return permissive ? FyaLensState.FLASH : FyaLensState.RED;
  }

  /**
   * Drives every FYA compound head — an active LEFT / PROTECTED_LEFT phase whose circuit has a
   * non-empty {@link TrafficSignalControllerCircuit#getFlashingLeftSignals() flashing-left lens} —
   * to a single valid combined indication, authoritative over {@link #applyServed} for its two
   * lenses. This mirrors the normal-mode {@code applyLeftTurnStatesByFacing} mapping so both modes
   * render an FYA head identically (never red-on-3-section with green-on-add-on):
   *
   * <ul>
   *   <li>PROTECTED_GREEN &rarr; 3-section OFF, green-arrow add-on GREEN;</li>
   *   <li>SOLID_YELLOW &rarr; 3-section YELLOW, add-on RED;</li>
   *   <li>FLASH &rarr; 3-section flashing yellow, add-on RED;</li>
   *   <li>RED &rarr; 3-section RED, add-on RED.</li>
   * </ul>
   *
   * @param resolved per-phase states from {@link RingBarrierState} (carrying the FLASH&rarr;red
   *                 solid-yellow clearance); a phase absent from the map, or a {@code null} map,
   *                 falls back to the stateless {@link #baseFyaState}.
   */
  private static void applyFyaLenses(TrafficSignalPhase phase,
      TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits circuits,
      RingBarrierState.ServedMovement ring1,
      RingBarrierState.ServedMovement ring2,
      java.util.Map<Integer, FyaLensState> resolved) {
    for (TrafficSignalProgrammedPhase p : plan.getPhases()) {
      if (!p.isActive()) {
        continue;
      }
      if (p.getMovement() != TrafficSignalPhaseMovement.LEFT
          && p.getMovement() != TrafficSignalPhaseMovement.PROTECTED_LEFT) {
        continue;
      }
      int ci = p.getCircuitIndex();
      if (ci < 0 || ci >= circuits.getCircuitCount()) {
        continue;
      }
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(ci);
      List<BlockPos> fyaLens = circuit.getFlashingLeftSignals();
      if (fyaLens.isEmpty()) {
        continue; // not an FYA compound head
      }
      FyaLensState state = resolved != null ? resolved.get(p.getPhaseNumber()) : null;
      if (state == null) {
        state = baseFyaState(servedInterval(p.getPhaseNumber(), ring1, ring2),
            servedInterval(p.getPermissivePhase(), ring1, ring2), p.getPermissivePhase() > 0);
      }
      List<BlockPos> arrowLens = circuit.getLeftSignals();
      phase.removeSignals(fyaLens);
      phase.removeSignals(arrowLens);
      switch (state) {
        case PROTECTED_GREEN:
          phase.addOffSignals(fyaLens);
          phase.addGreenSignals(arrowLens);
          break;
        case SOLID_YELLOW:
          phase.addYellowSignals(fyaLens);
          phase.addRedSignals(arrowLens);
          break;
        case FLASH:
          phase.addFyaSignals(fyaLens);
          phase.addRedSignals(arrowLens);
          break;
        case RED:
        default:
          phase.addRedSignals(fyaLens);
          phase.addRedSignals(arrowLens);
          break;
      }
    }
  }

  /**
   * Whether any of {@code phases} is served green or yellow this tick. Used by the
   * {@code -GRN/YEL} overlap type to drop the overlap to red while a modifier movement runs.
   */
  static boolean anyServedActive(int[] phases, RingBarrierState.ServedMovement ring1,
      RingBarrierState.ServedMovement ring2) {
    for (int p : phases) {
      RingBarrierState.VehInterval iv = servedInterval(p, ring1, ring2);
      if (iv == RingBarrierState.VehInterval.GREEN || iv == RingBarrierState.VehInterval.YELLOW) {
        return true;
      }
    }
    return false;
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
