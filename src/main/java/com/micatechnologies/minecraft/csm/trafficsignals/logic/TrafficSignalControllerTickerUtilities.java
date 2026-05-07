package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.google.common.collect.Lists;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityBlankoutBox;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Utility class for the {@link TrafficSignalControllerTicker} which provides core functionality to
 * the various tick methods without cluttering the class and possibly making it more difficult to
 * interpret/understand.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2023.2.0
 */
public class TrafficSignalControllerTickerUtilities {

  /**
   * Gets the flashing don't walk transition phase for the transition from the specified current
   * phase to the specified upcoming phase.
   *
   * @param currentPhase  The current phase.
   * @param upcomingPhase The upcoming phase.
   *
   * @return The flashing don't walk transition phase for the transition from the specified current
   *     phase to the specified upcoming phase.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase getFlashDontWalkTransitionPhaseForUpcoming(
      TrafficSignalPhase currentPhase,
      TrafficSignalPhase upcomingPhase) {
    // Check for green walk signals in the current phase
    TrafficSignalPhase flashDontWalkTransitionPhase = null;
    if (currentPhase.getWalkSignals().size() > 0) {
      // Create the flash don't walk transition phase
      TrafficSignalPhase tempFlashDontWalkTransitionPhase =
          new TrafficSignalPhase(currentPhase.getCircuit(),
              upcomingPhase,
              TrafficSignalPhaseApplicability.FLASH_DONT_WALK_TRANSITIONING);

      // Add the current phase walk signals to the flash don't walk transition phase
      boolean flashDontWalkSignalsAdded = false;
      for (BlockPos walkSignal : currentPhase.getWalkSignals()) {
        // Check if walk signal is still in walk state in the upcoming phase (stay in walk state)
        if (upcomingPhase.getWalkSignals().contains(walkSignal)) {
          // Add walk signal to flash don't walk transition phase (stay in walk state)
          tempFlashDontWalkTransitionPhase.addWalkSignal(walkSignal);
        }
        // Otherwise, walk signal is not in walk state in the upcoming phase (transition to don't
        // walk state)
        else {
          // Add don't walk signal to flash don't walk transition phase (transition to don't walk
          // state)
          tempFlashDontWalkTransitionPhase.addFlashDontWalkSignal(walkSignal);
          flashDontWalkSignalsAdded = true;
        }
      }

      // Check if flash don't walk signals were added to the flash don't walk transition phase
      if (flashDontWalkSignalsAdded) {
        // Build the rest of the phase (no need to do before and waste tick time otherwise)
        // Copy the previous phase signals to the flash don't walk transition phase (except for
        // walk signals)
        tempFlashDontWalkTransitionPhase.addOffSignals(currentPhase.getOffSignals());
        tempFlashDontWalkTransitionPhase.addFyaSignals(currentPhase.getFyaSignals());
        tempFlashDontWalkTransitionPhase.addRedSignals(currentPhase.getRedSignals());
        tempFlashDontWalkTransitionPhase.addYellowSignals(currentPhase.getYellowSignals());
        tempFlashDontWalkTransitionPhase.addGreenSignals(currentPhase.getGreenSignals());
        tempFlashDontWalkTransitionPhase.addFlashDontWalkSignals(
            currentPhase.getFlashDontWalkSignals());
        tempFlashDontWalkTransitionPhase.addDontWalkSignals(currentPhase.getDontWalkSignals());

        // Set the flash don't walk transition phase
        flashDontWalkTransitionPhase = tempFlashDontWalkTransitionPhase;
      }
    }

    // Return resulting flash don't walk transition phase (null if none)
    return flashDontWalkTransitionPhase;
  }

  /**
   * Gets the yellow transition phase for the transition from the specified current phase to the
   * specified upcoming phase.
   *
   * @param currentPhase  The current phase.
   * @param upcomingPhase The upcoming phase.
   *
   * @return The yellow transition phase for the transition from the specified current phase to the
   *     specified upcoming phase.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase getYellowTransitionPhaseForUpcoming(
      TrafficSignalPhase currentPhase,
      TrafficSignalPhase upcomingPhase) {
    // Create the yellow transition phase
    TrafficSignalPhase yellowTransitionPhase =
        new TrafficSignalPhase(currentPhase.getCircuit(), upcomingPhase,
            TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);

    // Copy the previous phase signals to the yellow transition phase (except for off/green/FYA
    // signals)
    yellowTransitionPhase.addRedSignals(currentPhase.getRedSignals());
    yellowTransitionPhase.addYellowSignals(currentPhase.getYellowSignals());
    yellowTransitionPhase.addWalkSignals(currentPhase.getWalkSignals());
    yellowTransitionPhase.addDontWalkSignals(currentPhase.getFlashDontWalkSignals());
    yellowTransitionPhase.addDontWalkSignals(currentPhase.getDontWalkSignals());

    // Add the current phase green signals to the yellow transition phase
    for (BlockPos greenSignal : currentPhase.getGreenSignals()) {
      // Check if green signal is still in green state in the upcoming phase (stay in green state)
      if (upcomingPhase.getGreenSignals().contains(greenSignal)) {
        yellowTransitionPhase.addGreenSignal(greenSignal);
      }
      // Otherwise, green signal is not in green state in the upcoming phase (transition to
      // yellow state). This applies to all transitions including GREEN→FYA — the yellow
      // clearance interval must always occur per MUTCD requirements.
      else {
        yellowTransitionPhase.addYellowSignal(greenSignal);
      }
    }

    // FYA signals: only stay FYA when the upcoming phase also has them as FYA (no state
    // change). All other transitions (FYA→RED, FYA→OFF, FYA→GREEN) require solid yellow
    // clearance. In the compound hybrid left/right layout, the 3-section block
    // (flashingLeftSignals / flashingRightSignals) uses its YELLOW section for clearance
    // regardless of whether the upcoming state is RED, OFF, or GREEN.
    for (BlockPos fyaSignal : currentPhase.getFyaSignals()) {
      if (upcomingPhase.getFyaSignals().contains(fyaSignal)) {
        yellowTransitionPhase.addFyaSignal(fyaSignal);
      } else {
        yellowTransitionPhase.addYellowSignal(fyaSignal);
      }
    }

    // Off signals going to a non-off state get yellow clearance. In the compound hybrid
    // left/right layout, the 3-section block (flashingLeftSignals / flashingRightSignals)
    // is OFF during protected green (the add-on block shows the green arrow instead).
    // When transitioning away, the 3-section block must display its solid yellow arrow
    // section as clearance. Signals staying off are preserved as off.
    for (BlockPos offSignal : currentPhase.getOffSignals()) {
      if (upcomingPhase.getOffSignals().contains(offSignal)) {
        yellowTransitionPhase.addOffSignal(offSignal);
      } else {
        yellowTransitionPhase.addYellowSignal(offSignal);
      }
    }

    // Return resulting yellow transition phase
    return yellowTransitionPhase;
  }

  /**
   * Gets the red transition phase for the transition from the specified current phase to the
   * specified upcoming phase.
   *
   * @param currentPhase  The current phase.
   * @param upcomingPhase The upcoming phase.
   *
   * @return The red transition phase for the transition from the specified current phase to the
   *     specified upcoming phase.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase getRedTransitionPhaseForUpcoming(TrafficSignalPhase currentPhase,
      TrafficSignalPhase upcomingPhase) {
    // Create the red transition phase
    TrafficSignalPhase redTransitionPhase =
        new TrafficSignalPhase(currentPhase.getCircuit(), upcomingPhase,
            TrafficSignalPhaseApplicability.RED_TRANSITIONING);

    // Copy non-vehicle signals
    redTransitionPhase.addOffSignals(currentPhase.getOffSignals());
    redTransitionPhase.addWalkSignals(currentPhase.getWalkSignals());
    redTransitionPhase.addFlashDontWalkSignals(currentPhase.getFlashDontWalkSignals());
    redTransitionPhase.addDontWalkSignals(currentPhase.getDontWalkSignals());
    redTransitionPhase.addRedSignals(currentPhase.getRedSignals());

    // Yellow signals become red
    redTransitionPhase.addRedSignals(currentPhase.getYellowSignals());

    // FYA signals staying FYA in the upcoming phase are preserved (no red blip for a
    // permissive signal that continues as permissive). All others become red.
    for (BlockPos fyaSignal : currentPhase.getFyaSignals()) {
      if (upcomingPhase.getFyaSignals().contains(fyaSignal)) {
        redTransitionPhase.addFyaSignal(fyaSignal);
      } else {
        redTransitionPhase.addRedSignal(fyaSignal);
      }
    }

    // Green signals that stay green in the upcoming phase remain green (no unnecessary
    // red blip); all others become red
    for (BlockPos greenSignal : currentPhase.getGreenSignals()) {
      if (upcomingPhase.getGreenSignals().contains(greenSignal)) {
        redTransitionPhase.addGreenSignal(greenSignal);
      } else {
        redTransitionPhase.addRedSignal(greenSignal);
      }
    }

    // Return resulting red transition phase
    return redTransitionPhase;
  }

  /**
   * Checks whether LPI (Lead Pedestrian Interval) conditions are met, and if so, returns an LPI
   * phase that wraps the specified upcoming green phase. If LPI conditions are not met, returns
   * the upcoming green phase directly.
   *
   * @param originatingPhase           The phase we are transitioning from (e.g., the ALL_RED or
   *                                   RED_TRANSITIONING phase). Signals that are green in both this
   *                                   phase and the upcoming phase will stay green during LPI.
   * @param upcomingGreenPhase         The upcoming green phase to potentially wrap with LPI.
   * @param overlapPedestrianSignals   Whether overlap pedestrian signals are enabled.
   * @param leadPedestrianIntervalTime The lead pedestrian interval time in ticks (0 = disabled).
   *
   * @return An LPI phase wrapping the upcoming green phase if conditions are met, otherwise the
   *     upcoming green phase itself.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase maybeWrapWithLpi(TrafficSignalPhase originatingPhase,
      TrafficSignalPhase upcomingGreenPhase,
      boolean overlapPedestrianSignals,
      long leadPedestrianIntervalTime) {
    // LPI only applies when overlap pedestrian signals are enabled and LPI time is configured
    if (!overlapPedestrianSignals || leadPedestrianIntervalTime <= 0) {
      return upcomingGreenPhase;
    }

    // LPI only makes sense if the upcoming phase has walk signals
    if (upcomingGreenPhase.getWalkSignals().isEmpty()) {
      return upcomingGreenPhase;
    }

    // Create the LPI phase
    return getLpiPhaseForUpcoming(originatingPhase, upcomingGreenPhase);
  }

  /**
   * Creates a lead pedestrian interval (LPI) phase for the specified upcoming green phase. During
   * the LPI phase, vehicle signals that are newly turning green are held at red while pedestrian
   * walk signals from the upcoming phase are displayed, giving pedestrians a head start before the
   * vehicle phase begins. Signals that are already green in the originating phase and will remain
   * green in the upcoming phase are kept green (not forced to red).
   *
   * @param originatingPhase   The phase we are transitioning from. Signals green in both this
   *                           phase and the upcoming phase will remain green during LPI.
   * @param upcomingGreenPhase The upcoming green phase that will follow the LPI phase.
   *
   * @return The LPI phase with walk signals active and newly-green vehicle signals held at red.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase getLpiPhaseForUpcoming(TrafficSignalPhase originatingPhase,
      TrafficSignalPhase upcomingGreenPhase) {
    TrafficSignalPhase lpiPhase = new TrafficSignalPhase(
        upcomingGreenPhase.getCircuit(), upcomingGreenPhase,
        TrafficSignalPhaseApplicability.LEAD_PEDESTRIAN_INTERVAL);

    // Signals that are green in the originating phase (used to determine which greens to preserve)
    List<BlockPos> originGreenSignals = originatingPhase.getGreenSignals();

    // Pedestrian walk signals get their head start during LPI
    lpiPhase.addWalkSignals(upcomingGreenPhase.getWalkSignals());

    // Don't walk signals remain as don't walk
    lpiPhase.addDontWalkSignals(upcomingGreenPhase.getDontWalkSignals());

    // Flash don't walk signals from the upcoming phase stay as don't walk during LPI
    lpiPhase.addDontWalkSignals(upcomingGreenPhase.getFlashDontWalkSignals());

    // For green signals: keep green if already green in the originating phase, otherwise hold red
    for (BlockPos greenSignal : upcomingGreenPhase.getGreenSignals()) {
      if (originGreenSignals.contains(greenSignal)) {
        lpiPhase.addGreenSignal(greenSignal);
      } else {
        lpiPhase.addRedSignal(greenSignal);
      }
    }

    // All other vehicle signals remain red during LPI
    lpiPhase.addRedSignals(upcomingGreenPhase.getRedSignals());
    lpiPhase.addRedSignals(upcomingGreenPhase.getYellowSignals());
    lpiPhase.addRedSignals(upcomingGreenPhase.getFyaSignals());
    lpiPhase.addRedSignals(upcomingGreenPhase.getOffSignals());

    return lpiPhase;
  }

  /**
   * Checks whether two phases have conflicting vehicle signal assignments. Returns {@code true} if
   * any BlockPos appears in a different vehicle signal list (green, FYA, yellow, red, off) between
   * the two phases — meaning a direct transition from one to the other would change a vehicle
   * signal without clearance.
   *
   * @param a The first phase.
   * @param b The second phase.
   *
   * @return {@code true} if any vehicle signal would change state between the two phases.
   *
   * @since 1.0
   */
  public static boolean hasVehicleSignalConflict(TrafficSignalPhase a, TrafficSignalPhase b) {
    // A signal leaving GREEN needs yellow clearance before any other state
    for (BlockPos pos : a.getGreenSignals()) {
      if (!b.getGreenSignals().contains(pos)) return true;
    }
    // A signal leaving YELLOW must drop to RED before going anywhere else — going to
    // GREEN, FYA, or OFF without an all-red interval skips the standard
    // yellow → red → next-state clearance sequence.
    for (BlockPos pos : a.getYellowSignals()) {
      if (!b.getYellowSignals().contains(pos) && !b.getRedSignals().contains(pos)) {
        return true;
      }
    }
    // A signal leaving FYA to RED needs clearance (permissive ending restrictively).
    // FYA→OFF is safe (compound hybrid going to protected green via add-on block).
    // FYA→GREEN doesn't occur in practice (GREEN on 3-section block IS FYA).
    for (BlockPos pos : a.getFyaSignals()) {
      if (b.getRedSignals().contains(pos)) return true;
    }
    // RED→anything and OFF→anything are safe (signal was already restrictive/dark)
    return false;
  }

  /**
   * Determines whether the right turn signals for the specified active circuit should display a
   * green arrow (suppressing concurrent pedestrian walk) or a flashing yellow arrow (permitting
   * concurrent pedestrian walk). This method is only meaningful when
   * {@code overlapPedestrianSignals} is {@code true}; it always returns {@code false} otherwise.
   *
   * <p>The decision is based on comparing the right turn vehicle count detected by the active
   * circuit's right turn scan zone against the total pedestrian accessory request count across all
   * other circuits. If the right turn count exceeds the combined pedestrian request count, a green
   * right turn arrow is warranted and {@code true} is returned. If the right turn detection zone
   * is not configured (count == 0) or the pedestrian demand is equal to or greater, a flashing
   * yellow arrow is used and {@code false} is returned.</p>
   *
   * <p><b>Asymmetry with {@link #computeGreenLeftTurn}:</b> the left-turn variant refuses
   * protected green on a multi-direction circuit (via {@code !areSignalsFacingSameDirection})
   * because opposing protected lefts intersect each other. Right turns on a multi-direction
   * circuit do <i>not</i> intersect (NB→E and SB→W use disjoint paths), so the same-direction
   * guard is intentionally omitted here. Pedestrian conflicts are still arbitrated by the
   * vehicle-count vs. ped-request comparison, and the protected-vs-right conflict against
   * transit/bike signals is handled by the caller.</p>
   *
   * <p><b>Demand semantic vs. {@link #getEffectiveRightDemand}:</b> this method uses the raw
   * {@code getRightTotal()} count without FYA-clearance adjustment. {@code getEffectiveRightDemand}
   * by contrast subtracts the single-vehicle "clears permissively on FYA" assumption per
   * direction. The split is intentional: phase priority (which decides whether to <i>switch</i>
   * to a turn-specific phase) should not be triggered by a single car that can clear on the
   * concurrent FYA, while the through-phase indication arbitration done here should serve a
   * waiting car a protected arrow whenever it can without giving up nothing — the car is
   * already at the line, so a brief protected green is cheaper than waiting for it to clear
   * permissively.</p>
   *
   * @param circuits                 The configured/connected circuits of the traffic signal
   *                                 controller.
   * @param activeCircuitNumber      The 1-based circuit number currently being served.
   * @param overlapPedestrianSignals Whether concurrent pedestrian signals are enabled.
   * @param world                    The world in which the controller is located.
   *
   * @return {@code true} if the active circuit's right turn signals should show a green arrow and
   *     concurrent pedestrian signals should be suppressed; {@code false} if a flashing yellow
   *     arrow should be used and concurrent pedestrian walk is permitted.
   *
   * @since 1.0
   */
  public static boolean computeGreenRightTurn(TrafficSignalControllerCircuits circuits,
      int activeCircuitNumber,
      boolean overlapPedestrianSignals,
      World world) {
    return !computeGreenRightTurnFacings(circuits, activeCircuitNumber,
        overlapPedestrianSignals, world).isEmpty();
  }

  /**
   * Per-direction variant of {@link #computeGreenRightTurn}: returns the set of facings on
   * which the active circuit's right turn signals should display a solid green arrow. Other
   * facings (or facings with no demand) show FYA permissive.
   *
   * <p>Each direction is arbitrated independently against the total pedestrian request count
   * on other circuits. A direction qualifies when its right-zone count is non-zero AND
   * exceeds the cross-circuit ped request total. This prevents a single car in one direction
   * from carrying empty-lane directions to solid green and unnecessarily suppressing
   * concurrent peds at unrelated crosswalks.</p>
   *
   * <p>Cross-direction ped counts aren't currently tracked, so the per-direction comparison
   * uses the total ped count (a more conservative threshold than aggregating right counts
   * across directions). For single-direction circuits this is identical to the old aggregated
   * comparison.</p>
   *
   * @param circuits                 The configured/connected circuits of the controller.
   * @param activeCircuitNumber      The 1-based circuit number currently being served.
   * @param overlapPedestrianSignals Whether concurrent pedestrian signals are enabled.
   * @param world                    The world in which the controller is located.
   *
   * @return The set of facings where solid green right is warranted; empty when no direction
   *     qualifies, when overlap is disabled, or when the circuit number is out of range.
   *
   * @since 1.0
   */
  public static EnumSet<EnumFacing> computeGreenRightTurnFacings(
      TrafficSignalControllerCircuits circuits,
      int activeCircuitNumber,
      boolean overlapPedestrianSignals,
      World world) {
    EnumSet<EnumFacing> result = EnumSet.noneOf(EnumFacing.class);
    if (!overlapPedestrianSignals || activeCircuitNumber < 1
        || activeCircuitNumber > circuits.getCircuitCount()) {
      return result;
    }
    TrafficSignalSensorSummary summary = circuits.getCircuit(activeCircuitNumber - 1)
        .getSensorsWaitingSummary(world);
    int otherPedCount = sumOtherCircuitPedRequests(circuits, activeCircuitNumber, world);
    addFacingIfQualifies(result, EnumFacing.EAST, summary.getRightEast(), otherPedCount);
    addFacingIfQualifies(result, EnumFacing.WEST, summary.getRightWest(), otherPedCount);
    addFacingIfQualifies(result, EnumFacing.NORTH, summary.getRightNorth(), otherPedCount);
    addFacingIfQualifies(result, EnumFacing.SOUTH, summary.getRightSouth(), otherPedCount);
    return result;
  }

  /**
   * Determines whether the active circuit's left turn signals should display a green arrow
   * (protected left, suppressing concurrent pedestrian walk) or a flashing yellow arrow (permissive
   * left, allowing concurrent pedestrian walk). This method mirrors {@link #computeGreenRightTurn}
   * but uses the left turn scan zone count, and additionally refuses protected green on a
   * multi-direction circuit because opposing protected lefts intersect each other.
   *
   * <p><b>Demand semantic vs. {@link #getEffectiveLeftDemand}:</b> uses the raw
   * {@code getLeftTotal()} count, not the FYA-adjusted directional count. See
   * {@link #computeGreenRightTurn} for the rationale — the through-phase indication should
   * serve a waiting car a protected arrow whenever it can, while phase priority should not
   * promote a single car that can clear permissively to its own ALL_LEFTS phase.</p>
   *
   * @param circuits                 The configured/connected circuits of the traffic signal
   *                                 controller.
   * @param activeCircuitNumber      The 1-based circuit number currently being served.
   * @param overlapPedestrianSignals Whether concurrent pedestrian signals are enabled.
   * @param world                    The world in which the controller is located.
   *
   * @return {@code true} if the active circuit's left turn signals should show a green arrow and
   *     concurrent pedestrian signals should be suppressed; {@code false} if a flashing yellow
   *     arrow should be used and concurrent pedestrian walk is permitted.
   *
   * @since 1.0
   */
  public static boolean computeGreenLeftTurn(TrafficSignalControllerCircuits circuits,
      int activeCircuitNumber,
      boolean overlapPedestrianSignals,
      World world) {
    return !computeGreenLeftTurnFacings(circuits, activeCircuitNumber,
        overlapPedestrianSignals, world).isEmpty();
  }

  /**
   * Per-direction variant of {@link #computeGreenLeftTurn}: returns the set of facings on
   * which the active circuit's left turn signals should display a solid green arrow.
   *
   * <p>Multi-direction circuits return an empty set unconditionally — opposing protected
   * lefts intersect, so left demand on a multi-direction circuit must always be served via
   * FYA permissive. For single-direction circuits each facing is arbitrated independently
   * against cross-circuit ped requests, the same way as right turns.</p>
   *
   * @param circuits                 The configured/connected circuits of the controller.
   * @param activeCircuitNumber      The 1-based circuit number currently being served.
   * @param overlapPedestrianSignals Whether concurrent pedestrian signals are enabled.
   * @param world                    The world in which the controller is located.
   *
   * @return The set of facings where solid green left is warranted; empty when no direction
   *     qualifies, the circuit is multi-direction, overlap is disabled, or the circuit
   *     number is out of range.
   *
   * @since 1.0
   */
  public static EnumSet<EnumFacing> computeGreenLeftTurnFacings(
      TrafficSignalControllerCircuits circuits,
      int activeCircuitNumber,
      boolean overlapPedestrianSignals,
      World world) {
    EnumSet<EnumFacing> result = EnumSet.noneOf(EnumFacing.class);
    if (!overlapPedestrianSignals || activeCircuitNumber < 1
        || activeCircuitNumber > circuits.getCircuitCount()) {
      return result;
    }
    TrafficSignalControllerCircuit circuit = circuits.getCircuit(activeCircuitNumber - 1);
    // Multi-direction guard: opposing protected lefts intersect, so always FYA.
    if (world != null && !circuit.areSignalsFacingSameDirection(world)) {
      return result;
    }
    TrafficSignalSensorSummary summary = circuit.getSensorsWaitingSummary(world);
    int otherPedCount = sumOtherCircuitPedRequests(circuits, activeCircuitNumber, world);
    addFacingIfQualifies(result, EnumFacing.EAST, summary.getLeftEast(), otherPedCount);
    addFacingIfQualifies(result, EnumFacing.WEST, summary.getLeftWest(), otherPedCount);
    addFacingIfQualifies(result, EnumFacing.NORTH, summary.getLeftNorth(), otherPedCount);
    addFacingIfQualifies(result, EnumFacing.SOUTH, summary.getLeftSouth(), otherPedCount);
    return result;
  }

  private static int sumOtherCircuitPedRequests(TrafficSignalControllerCircuits circuits,
      int activeCircuitNumber, World world) {
    int count = 0;
    for (int i = 1; i <= circuits.getCircuitCount(); i++) {
      if (i != activeCircuitNumber) {
        count += circuits.getCircuit(i - 1).getPedestrianAccessoriesRequestCount(world);
      }
    }
    return count;
  }

  private static void addFacingIfQualifies(EnumSet<EnumFacing> result, EnumFacing facing,
      int directionalCount, int otherPedCount) {
    if (directionalCount > 0 && directionalCount > otherPedCount) {
      result.add(facing);
    }
  }

  /**
   * Gets the default phase for the specified circuit number when the traffic signal controller is
   * operating in {@link TrafficSignalControllerMode#NORMAL} mode.
   *
   * @param circuits                 The configured/connected circuits of the traffic signal
   *                                 controller.
   * @param overlaps                 The {@link TrafficSignalControllerOverlaps} to apply to the
   *                                 specified {@link TrafficSignalPhase}.
   * @param circuitNumber            The circuit number to get the default phase for. This is a
   *                                 1-based index.
   * @param overlapPedestrianSignals The overlap pedestrian signals setting of the traffic signal
   *                                 controller. This boolean value is used to determine if the
   *                                 pedestrian signals of all other circuits should be overlapped
   *                                 when servicing a circuit.
   *
   * @return The default phase for the specified circuit number when the traffic signal controller
   *     is operating in {@link TrafficSignalControllerMode#NORMAL} mode.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase getDefaultPhaseForCircuitNumber(
      TrafficSignalControllerCircuits circuits,
      TrafficSignalControllerOverlaps overlaps,
      int circuitNumber,
      boolean overlapPedestrianSignals,World world) {
    // Only create a default phase if there are circuits
    TrafficSignalPhase defaultPhase = new TrafficSignalPhase(
        TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
        TrafficSignalPhaseApplicability.ALL_RED);
    if (circuits.getCircuitCount() > 0) {
      // Check if circuit has protected signals
      boolean hasProtectedSignals =
          !circuits.getCircuit(circuitNumber - 1).getProtectedSignals().isEmpty();

      // Determine which directions warrant a protected green arrow (vs. FYA permissive). Each
      // direction is arbitrated independently against cross-circuit ped requests.
      EnumSet<EnumFacing> greenRightTurnFacings = computeGreenRightTurnFacings(
          circuits, circuitNumber, overlapPedestrianSignals, world);
      EnumSet<EnumFacing> greenLeftTurnFacings = computeGreenLeftTurnFacings(
          circuits, circuitNumber, overlapPedestrianSignals, world);
      // Booleans for circuit-wide decisions (e.g., should peds on OTHER circuits be
      // suppressed because this circuit has any solid green turn).
      boolean greenRightTurn = !greenRightTurnFacings.isEmpty();
      boolean greenLeftTurn = !greenLeftTurnFacings.isEmpty();

      // Get appropriate phase applicability
      TrafficSignalPhaseApplicability phaseApplicability =
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS;
      if (hasProtectedSignals) {
        phaseApplicability = TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS;
      }

      // Create the default phase
      defaultPhase = new TrafficSignalPhase(circuitNumber, null, phaseApplicability);
      for (int i = 1; i <= circuits.getCircuitCount(); i++) {
        TrafficSignalControllerCircuit circuit = circuits.getCircuit(i - 1);
        if (i == circuitNumber) {
          // Check if all facing same direction
          boolean allFacingSameDir = circuit.areSignalsFacingSameDirection(world);

          // Resolve the set of facings where the active circuit's left turn shows solid green.
          // Use the demand-derived facings when overlap arbitration is in play; otherwise the
          // legacy "all-or-nothing" fallback (single-direction circuit without protecteds and
          // without overlap → all left facings present get protected green).
          boolean hasLeftSignals = !circuit.getLeftSignals().isEmpty();
          EnumSet<EnumFacing> activeLeftFacings;
          if (hasLeftSignals && greenLeftTurn) {
            activeLeftFacings = greenLeftTurnFacings;
          } else if (hasLeftSignals && allFacingSameDir
              && !hasProtectedSignals && !overlapPedestrianSignals) {
            activeLeftFacings = collectFacingsFromSignals(world, circuit.getLeftSignals());
            activeLeftFacings.addAll(
                collectFacingsFromSignals(world, circuit.getFlashingLeftSignals()));
          } else {
            activeLeftFacings = EnumSet.noneOf(EnumFacing.class);
          }
          applyLeftTurnStatesByFacing(world, circuit, defaultPhase, activeLeftFacings);

          defaultPhase.addGreenSignals(circuit.getThroughSignals());

          // Resolve the set of facings where the active circuit's right turn shows solid
          // green. Demand-derived in overlap or has-protected modes; "all directions present"
          // in the no-overlap, no-protected case (right always solid green).
          EnumSet<EnumFacing> activeRightFacings;
          if (hasProtectedSignals || overlapPedestrianSignals) {
            activeRightFacings = greenRightTurnFacings;
          } else {
            activeRightFacings = collectFacingsFromSignals(world, circuit.getRightSignals());
            activeRightFacings.addAll(
                collectFacingsFromSignals(world, circuit.getFlashingRightSignals()));
          }
          applyRightTurnAndProtectedStatesByFacing(world, circuit, defaultPhase,
              activeRightFacings);

          defaultPhase.addOffSignals(circuit.getPedestrianBeaconSignals());
          defaultPhase.addOffSignals(circuit.getBeaconSignals());
          defaultPhase.addDontWalkSignals(circuit.getPedestrianSignals());
          defaultPhase.addDontWalkSignals(circuit.getPedestrianAccessorySignals());
          addBlankoutSignalsToPhase(world, circuit, defaultPhase);
        } else {
          addCircuitToPhaseAllRed(circuit, defaultPhase,
              overlapPedestrianSignals && !greenRightTurn && !greenLeftTurn);
        }
      }
    }

    // Add overlaps if necessary
    defaultPhase =
        TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(defaultPhase, overlaps);

    return defaultPhase;
  }

  /**
   * Utility method to add all signals from the specified {@link TrafficSignalControllerCircuit} to
   * their respective red states in the specified {@link TrafficSignalPhase}.
   *
   * @param circuit               The circuit to add to the specified phase.
   * @param destinationPhase      The phase to add the specified circuit to.
   * @param pedestrianSignalsWalk The boolean indicating if the circuit's pedestrian signals should
   *                              be set to walk or don't walk.
   *
   * @since 1.0
   */
  public static void addCircuitToPhaseAllRed(TrafficSignalControllerCircuit circuit,
      TrafficSignalPhase destinationPhase,
      boolean pedestrianSignalsWalk) {
    destinationPhase.addRedSignals(circuit.getFlashingLeftSignals());
    destinationPhase.addRedSignals(circuit.getFlashingRightSignals());
    destinationPhase.addRedSignals(circuit.getLeftSignals());
    destinationPhase.addRedSignals(circuit.getRightSignals());
    destinationPhase.addRedSignals(circuit.getThroughSignals());
    destinationPhase.addRedSignals(circuit.getProtectedSignals());
    destinationPhase.addYellowSignals(circuit.getBeaconSignals());
    destinationPhase.addRedSignals(circuit.getPedestrianBeaconSignals());
    if (pedestrianSignalsWalk) {
      destinationPhase.addWalkSignals(circuit.getPedestrianSignals());
      destinationPhase.addWalkSignals(circuit.getPedestrianAccessorySignals());
    } else {
      destinationPhase.addDontWalkSignals(circuit.getPedestrianSignals());
      destinationPhase.addDontWalkSignals(circuit.getPedestrianAccessorySignals());
    }
    // All vehicle signals on this circuit are red, so the main indication already tells
    // drivers no turns are permitted. A "NO LEFT/RIGHT TURN" sign would just clutter, so
    // the convention is to keep blankouts dark during all-red rather than route them
    // through addBlankoutSignalsToPhase. Phases where some signals are green/FYA on this
    // circuit (the active ALL_LEFTS / ALL_THROUGHS_* / directional branches) do call the
    // helper instead, so the sign lights only when contradicting indication exists.
    destinationPhase.addDontWalkSignals(circuit.getNoTurnBlankoutSignals());
  }

  /**
   * Builds the active-circuit signal assignments for an
   * {@link TrafficSignalPhaseApplicability#ALL_THROUGHS_PROTECTEDS} phase. Each direction's
   * left and right turn arrows are arbitrated independently — only directions present in
   * {@code greenLeftTurnFacings} / {@code greenRightTurnFacings} get protected (solid green)
   * arrows; other directions show FYA permissive.
   *
   * <p>For directions where right is solid green, same-facing transit/bike protected signals
   * go red (conflict). Directions where right is FYA permissive let protected stay green.</p>
   *
   * @param world                  The world (used for facing lookups and by
   *                               {@link #addBlankoutSignalsToPhase}; may be {@code null}
   *                               in tests when both facing sets are empty and no blankouts
   *                               are configured).
   * @param circuit                The active circuit being served.
   * @param upcomingPhase          The phase being built; this method appends to it.
   * @param greenLeftTurnFacings   Facings on which the active circuit's left turn should show
   *                               solid green (protected). Other facings on the same circuit
   *                               show FYA permissive.
   * @param greenRightTurnFacings  Facings on which the active circuit's right turn should show
   *                               solid green. Other facings show FYA permissive.
   *
   * @since 1.0
   */
  static void buildAllThroughsProtectedsActivePhase(World world,
      TrafficSignalControllerCircuit circuit,
      TrafficSignalPhase upcomingPhase,
      EnumSet<EnumFacing> greenLeftTurnFacings,
      EnumSet<EnumFacing> greenRightTurnFacings) {
    applyAllThroughsProtectedsSignalStates(circuit, upcomingPhase,
        partitionSignalsByFacingSet(world, circuit.getFlashingLeftSignals(),
            greenLeftTurnFacings),
        partitionSignalsByFacingSet(world, circuit.getLeftSignals(), greenLeftTurnFacings),
        partitionSignalsByFacingSet(world, circuit.getFlashingRightSignals(),
            greenRightTurnFacings),
        partitionSignalsByFacingSet(world, circuit.getRightSignals(), greenRightTurnFacings),
        partitionSignalsByFacingSet(world, circuit.getProtectedSignals(),
            greenRightTurnFacings));
    addBlankoutSignalsToPhase(world, circuit, upcomingPhase);
  }

  /**
   * Per-direction left turn state assignment: at facings present in {@code protectedFacings}
   * the left arrow shows solid green (FYA companion off); at other facings the FYA flashes
   * yellow and the protected arrow stays red.
   *
   * @since 1.0
   */
  private static void applyLeftTurnStatesByFacing(World world,
      TrafficSignalControllerCircuit circuit, TrafficSignalPhase phase,
      EnumSet<EnumFacing> protectedFacings) {
    Tuple<List<BlockPos>, List<BlockPos>> flashing = partitionSignalsByFacingSet(world,
        circuit.getFlashingLeftSignals(), protectedFacings);
    Tuple<List<BlockPos>, List<BlockPos>> left = partitionSignalsByFacingSet(world,
        circuit.getLeftSignals(), protectedFacings);
    phase.addOffSignals(flashing.getFirst());
    phase.addFyaSignals(flashing.getSecond());
    phase.addGreenSignals(left.getFirst());
    phase.addRedSignals(left.getSecond());
  }

  /**
   * Per-direction right turn + transit/bike protected state assignment. At facings present in
   * {@code solidGreenRightFacings}: right arrow solid green, FYA companion off, same-facing
   * protected (transit/bike) goes red because solid green right conflicts with it. At other
   * facings: right arrow red, FYA permissive flashes yellow, same-facing protected stays
   * green (no conflict with permissive right).
   *
   * @since 1.0
   */
  private static void applyRightTurnAndProtectedStatesByFacing(World world,
      TrafficSignalControllerCircuit circuit, TrafficSignalPhase phase,
      EnumSet<EnumFacing> solidGreenRightFacings) {
    Tuple<List<BlockPos>, List<BlockPos>> flashing = partitionSignalsByFacingSet(world,
        circuit.getFlashingRightSignals(), solidGreenRightFacings);
    Tuple<List<BlockPos>, List<BlockPos>> right = partitionSignalsByFacingSet(world,
        circuit.getRightSignals(), solidGreenRightFacings);
    Tuple<List<BlockPos>, List<BlockPos>> prot = partitionSignalsByFacingSet(world,
        circuit.getProtectedSignals(), solidGreenRightFacings);
    phase.addOffSignals(flashing.getFirst());
    phase.addFyaSignals(flashing.getSecond());
    phase.addGreenSignals(right.getFirst());
    phase.addRedSignals(right.getSecond());
    phase.addRedSignals(prot.getFirst());     // matching: solid right green → protected red
    phase.addGreenSignals(prot.getSecond());  // non-matching: FYA right → protected green
  }

  /**
   * Collects the set of {@link EnumFacing} directions present on the given signal positions.
   * Used to build "all directions present" facings sets for non-overlap branches that
   * unconditionally promote turns at every direction the circuit serves.
   *
   * @since 1.0
   */
  private static EnumSet<EnumFacing> collectFacingsFromSignals(World world,
      List<BlockPos> signals) {
    EnumSet<EnumFacing> facings = EnumSet.noneOf(EnumFacing.class);
    if (world == null || signals == null) {
      return facings;
    }
    for (BlockPos pos : signals) {
      EnumFacing f = signalFacingOrNull(world, pos);
      if (f != null) {
        facings.add(f);
      }
    }
    return facings;
  }

  /**
   * Pure variant of {@link #buildAllThroughsProtectedsActivePhase} that takes pre-partitioned
   * signal tuples instead of doing world-based facing lookups. The {@code first} list of each
   * tuple holds signals at "protected-green" facings; the {@code second} list holds the rest
   * (which will show FYA permissive for left/right and stay green for protected).
   *
   * <p>Extracted so the world-free phase logic can be unit-tested without a Minecraft world.</p>
   *
   * @since 1.0
   */
  static void applyAllThroughsProtectedsSignalStates(TrafficSignalControllerCircuit circuit,
      TrafficSignalPhase upcomingPhase,
      Tuple<List<BlockPos>, List<BlockPos>> flashingLeftPartition,
      Tuple<List<BlockPos>, List<BlockPos>> leftPartition,
      Tuple<List<BlockPos>, List<BlockPos>> flashingRightPartition,
      Tuple<List<BlockPos>, List<BlockPos>> rightPartition,
      Tuple<List<BlockPos>, List<BlockPos>> protectedPartition) {
    // Left: matching facings → protected green; others → FYA permissive
    upcomingPhase.addOffSignals(flashingLeftPartition.getFirst());
    upcomingPhase.addFyaSignals(flashingLeftPartition.getSecond());
    upcomingPhase.addGreenSignals(leftPartition.getFirst());
    upcomingPhase.addRedSignals(leftPartition.getSecond());
    // Right: matching facings → solid green (suppress peds at those crosswalks);
    // others → FYA permissive (peds may walk at those crosswalks)
    upcomingPhase.addOffSignals(flashingRightPartition.getFirst());
    upcomingPhase.addFyaSignals(flashingRightPartition.getSecond());
    upcomingPhase.addGreenSignals(rightPartition.getFirst());
    upcomingPhase.addRedSignals(rightPartition.getSecond());
    // Protected (transit/bike): per-facing — red where same-direction right is solid green
    // (conflict), green where same-direction right is FYA permissive (no conflict)
    upcomingPhase.addRedSignals(protectedPartition.getFirst());
    upcomingPhase.addGreenSignals(protectedPartition.getSecond());
    // Through, peds, beacons — circuit-wide
    upcomingPhase.addGreenSignals(circuit.getThroughSignals());
    upcomingPhase.addOffSignals(circuit.getPedestrianBeaconSignals());
    upcomingPhase.addOffSignals(circuit.getBeaconSignals());
    upcomingPhase.addDontWalkSignals(circuit.getPedestrianSignals());
    upcomingPhase.addDontWalkSignals(circuit.getPedestrianAccessorySignals());
  }

  /**
   * Gets the specified {@link TrafficSignalPhase} with all applicable overlaps applied.
   *
   * @param phase    The {@link TrafficSignalPhase} to apply overlaps to.
   * @param overlaps The {@link TrafficSignalControllerOverlaps} to apply to the specified
   *                 {@link TrafficSignalPhase}.
   *
   * @return The specified {@link TrafficSignalPhase} with all applicable overlaps applied.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase getPhaseWithOverlapsApplied(TrafficSignalPhase phase,
      TrafficSignalControllerOverlaps overlaps) {
    // Check if overlaps are configured
    if (overlaps == null || overlaps.getOverlapCount() == 0) {
      return phase;
    }

    // Loop through each green signal in the phase
    List<BlockPos> greenSignals = Lists.newArrayList(phase.getGreenSignals());
    for (BlockPos greenSignal : greenSignals) {

      // Get the overlap signals for the green signal
      List<BlockPos> overlapSignals = overlaps.getOverlapsForSource(greenSignal);

      // Check if overlap signals were found
      if (overlapSignals != null) {

        // Loop through each overlap signal
        overlapSignals.forEach(phase::moveOverlapSignalToGreen);
      }
    }

    // Return the updated phase
    return phase;
  }

  /**
   * Applies overlaps to a transition phase. For each overlap source that is in a transition state
   * (yellow or red), the corresponding overlap targets are moved to the same state. This ensures
   * overlap signals get proper clearance during phase transitions.
   *
   * @param phase    The transition phase to apply overlaps to.
   * @param overlaps The configured overlaps.
   *
   * @return The phase with overlaps applied.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase getTransitionPhaseWithOverlapsApplied(
      TrafficSignalPhase phase, TrafficSignalControllerOverlaps overlaps) {
    if (overlaps == null || overlaps.getOverlapCount() == 0) {
      return phase;
    }

    // Green overlaps: source is green → targets go green
    List<BlockPos> greenSignals = Lists.newArrayList(phase.getGreenSignals());
    for (BlockPos greenSignal : greenSignals) {
      List<BlockPos> overlapSignals = overlaps.getOverlapsForSource(greenSignal);
      if (overlapSignals != null) {
        overlapSignals.forEach(phase::moveOverlapSignalToGreen);
      }
    }

    // Yellow overlaps: source is yellow → targets go yellow
    List<BlockPos> yellowSignals = Lists.newArrayList(phase.getYellowSignals());
    for (BlockPos yellowSignal : yellowSignals) {
      List<BlockPos> overlapSignals = overlaps.getOverlapsForSource(yellowSignal);
      if (overlapSignals != null) {
        overlapSignals.forEach(phase::moveOverlapSignalToYellow);
      }
    }

    // Red overlaps: source is red → targets go red
    List<BlockPos> redSignals = Lists.newArrayList(phase.getRedSignals());
    for (BlockPos redSignal : redSignals) {
      List<BlockPos> overlapSignals = overlaps.getOverlapsForSource(redSignal);
      if (overlapSignals != null) {
        overlapSignals.forEach(phase::moveOverlapSignalToRed);
      }
    }

    return phase;
  }

  /**
   * Gets the priority indicator of the upcoming phase to service. The priority indicator is a tuple
   * containing the circuit number and the applicability of the upcoming phase to service.
   *
   * @param world                    The world where the traffic signal controller and devices are
   *                                 located.
   * @param circuits                 The configured/connected circuits of the traffic signal
   *                                 controller.
   * @param overlapPedestrianSignals The overlap pedestrian signals setting of the traffic signal
   *                                 controller. This boolean value is used to determine if the
   *                                 pedestrian signals of all other circuits should be overlapped
   *                                 when servicing a circuit.
   *
   * @return The priority indicator of the upcoming phase to service.
   *
   * @since 1.0
   */
  public static Tuple<Integer, TrafficSignalPhaseApplicability> getUpcomingPhasePriorityIndicator(
      World world,
      TrafficSignalControllerCircuits circuits,
      boolean overlapPedestrianSignals) {
    // Create variables to track the highest priority phase
    int highestPriorityCircuitNumber = Integer.MIN_VALUE;
    TrafficSignalPhaseApplicability highestPriorityPhaseApplicability =
        TrafficSignalPhaseApplicability.NONE;
    int highestPriorityWaitingCount = 1;

    // Loop through all circuits to map pedestrian request counts
    int[] pedestrianRequestCount = new int[circuits.getCircuitCount()];
    for (int i = circuits.getCircuitCount(); i > 0; i--) {
      // Get the circuit
      int i0 = i - 1;
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(i0);

      // Get pedestrian request count for circuit
      int pedestrianAccessoriesRequestCount = circuit.getPedestrianAccessoriesRequestCount(world);

      // Store pedestrian request count for circuit
      pedestrianRequestCount[i0] = pedestrianAccessoriesRequestCount;
    }

    // Loop through all circuits
    for (int i = circuits.getCircuitCount(); i > 0; i--) {
      // Get the circuit
      int i0 = i - 1;
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(i0);

      // Get sensor summary for circuit
      TrafficSignalSensorSummary sensorSummary = circuit.getSensorsWaitingSummary(world);

      // Check circuit left turn demand, adjusted for permissive FYA turns.
      // Directions with FYA left signals require 2+ detections to trigger a protected
      // left phase (single detection is assumed to clear on the permissive turn).
      int allLeftTurnLanesDetectionCount = getEffectiveLeftDemand(world, circuit, sensorSummary);
      int allRightTurnLanesDetectionCount = getEffectiveRightDemand(world, circuit, sensorSummary);
      if (allLeftTurnLanesDetectionCount >= highestPriorityWaitingCount) {
        highestPriorityCircuitNumber = i;
        highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_LEFTS;
        highestPriorityWaitingCount = allLeftTurnLanesDetectionCount;
      }

      // Check circuit directional detection counts for highest priority.
      // Uses FYA-adjusted directional demand so that a single left turn detection
      // on a direction with FYA doesn't inflate that direction's count. This prevents
      // a single permissive-clearable left detection from triggering a directional
      // phase (ALL_EAST etc.) that stops opposing traffic unnecessarily.
      boolean leftsWasBest =
          highestPriorityPhaseApplicability == TrafficSignalPhaseApplicability.ALL_LEFTS;

      int eastFacingDetectionCount = getEffectiveDirectionalDemand(world, circuit,
          sensorSummary, EnumFacing.EAST);
      if (leftsWasBest
          ? eastFacingDetectionCount > highestPriorityWaitingCount
          : eastFacingDetectionCount >= highestPriorityWaitingCount) {
        highestPriorityCircuitNumber = i;
        highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_EAST;
        highestPriorityWaitingCount = eastFacingDetectionCount;
        leftsWasBest = false;
      }

      int westFacingDetectionCount = getEffectiveDirectionalDemand(world, circuit,
          sensorSummary, EnumFacing.WEST);
      if (leftsWasBest
          ? westFacingDetectionCount > highestPriorityWaitingCount
          : westFacingDetectionCount >= highestPriorityWaitingCount) {
        highestPriorityCircuitNumber = i;
        highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_WEST;
        highestPriorityWaitingCount = westFacingDetectionCount;
        leftsWasBest = false;
      }

      int northFacingDetectionCount = getEffectiveDirectionalDemand(world, circuit,
          sensorSummary, EnumFacing.NORTH);
      if (leftsWasBest
          ? northFacingDetectionCount > highestPriorityWaitingCount
          : northFacingDetectionCount >= highestPriorityWaitingCount) {
        highestPriorityCircuitNumber = i;
        highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_NORTH;
        highestPriorityWaitingCount = northFacingDetectionCount;
        leftsWasBest = false;
      }

      int southFacingDetectionCount = getEffectiveDirectionalDemand(world, circuit,
          sensorSummary, EnumFacing.SOUTH);
      if (leftsWasBest
          ? southFacingDetectionCount > highestPriorityWaitingCount
          : southFacingDetectionCount >= highestPriorityWaitingCount) {
        highestPriorityCircuitNumber = i;
        highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_SOUTH;
        highestPriorityWaitingCount = southFacingDetectionCount;
        leftsWasBest = false;
      }

      // Check circuit pedestrian request count for highest priority
      int pedestrianAccessoriesRequestCount = pedestrianRequestCount[i0];
      if (pedestrianAccessoriesRequestCount >= highestPriorityWaitingCount) {
        highestPriorityCircuitNumber = -1;
        highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.PEDESTRIAN;
        highestPriorityWaitingCount = pedestrianAccessoriesRequestCount;
      }

      // Calculate the number of requests for services pedestrian overlaps
      int pedestrianOverlapRequestCount = 0;
      if (overlapPedestrianSignals) {
        for (int x = 0; x < pedestrianRequestCount.length; x++) {
          if (x != i0) {
            pedestrianOverlapRequestCount += pedestrianRequestCount[x];
          }
        }
      }

      // Through-type checks: these use >= to match the baseline threshold, BUT they cannot
      // override ALL_LEFTS demand at equal count. Left turn demand should only be overridden
      // by through traffic that genuinely exceeds it, since left turns are a more specific
      // movement that would otherwise never get served against through-heavy traffic.
      // Re-check whether ALL_LEFTS is still the best (directional checks above may have
      // already overridden it with strictly higher demand).
      boolean currentBestIsLefts =
          highestPriorityPhaseApplicability == TrafficSignalPhaseApplicability.ALL_LEFTS;

      // Check circuit through/protecteds detection count for highest priority
      int throughsProtectedsDetectionCount = sensorSummary.getStandardTotal() +
          sensorSummary.getProtectedTotal() +
          allRightTurnLanesDetectionCount +
          pedestrianOverlapRequestCount;
      if (circuit.getProtectedSignals().size() > 0 &&
          (currentBestIsLefts
              ? throughsProtectedsDetectionCount > highestPriorityWaitingCount
              : throughsProtectedsDetectionCount >= highestPriorityWaitingCount)) {
        highestPriorityCircuitNumber = i;
        highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS;
        highestPriorityWaitingCount = throughsProtectedsDetectionCount;
        currentBestIsLefts = false;
      }

      // Check circuit through/rights detection count for highest priority
      int throughsDetectionCount = sensorSummary.getStandardTotal() +
          allRightTurnLanesDetectionCount + pedestrianOverlapRequestCount;
      if (currentBestIsLefts
          ? throughsDetectionCount > highestPriorityWaitingCount
          : throughsDetectionCount >= highestPriorityWaitingCount) {
        highestPriorityCircuitNumber = i;
        highestPriorityPhaseApplicability = TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS;
        highestPriorityWaitingCount = throughsDetectionCount;
        currentBestIsLefts = false;
      }

      // Check circuit through/protected rights detection count for highest priority
      int rawThroughsDetectionCount = sensorSummary.getStandardTotal() +
          allRightTurnLanesDetectionCount;
      if (currentBestIsLefts
          ? rawThroughsDetectionCount > highestPriorityWaitingCount
          : rawThroughsDetectionCount >= highestPriorityWaitingCount) {
        highestPriorityCircuitNumber = i;
        highestPriorityPhaseApplicability =
            TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTED_RIGHTS;
        highestPriorityWaitingCount = rawThroughsDetectionCount;
      }
    }

    // Return the highest priority phase (or null if none)
    if (highestPriorityCircuitNumber == Integer.MIN_VALUE) {
      return null;
    } else {
      return new Tuple<>(highestPriorityCircuitNumber, highestPriorityPhaseApplicability);
    }
  }

  /**
   * Gets the upcoming {@link TrafficSignalPhase} to service based on the specified priority
   * indicator.
   *
   * @param world                    The world where the traffic signal controller and devices are
   *                                 located.
   * @param circuits                 The configured/connected circuits of the traffic signal
   *                                 controller.
   * @param overlaps                 The {@link TrafficSignalControllerOverlaps} to apply to the
   *                                 specified {@link TrafficSignalPhase}.
   * @param priorityIndicator        The priority indicator of the upcoming phase to service.
   * @param overlapPedestrianSignals The overlap pedestrian signals setting of the traffic signal
   *                                 controller. This boolean value is used to determine if the
   *                                 pedestrian signals of all other circuits should be overlapped
   *                                 when servicing a circuit.
   *
   * @return The upcoming {@link TrafficSignalPhase} to service based on the specified priority
   *     indicator.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase getUpcomingPhaseForPriorityIndicator(World world,
      TrafficSignalControllerCircuits circuits,
      TrafficSignalControllerOverlaps overlaps,
      Tuple<Integer, TrafficSignalPhaseApplicability> priorityIndicator,
      boolean overlapPedestrianSignals) {
    // Get the circuit number and phase applicability from the priority indicator
    int circuitNumber = priorityIndicator.getFirst();
    TrafficSignalPhaseApplicability phaseApplicability = priorityIndicator.getSecond();

    // Determine which directions warrant a protected green arrow (vs. FYA permissive). Each
    // direction is arbitrated independently against cross-circuit ped requests.
    EnumSet<EnumFacing> greenRightTurnFacings = computeGreenRightTurnFacings(
        circuits, circuitNumber, overlapPedestrianSignals, world);
    EnumSet<EnumFacing> greenLeftTurnFacings = computeGreenLeftTurnFacings(
        circuits, circuitNumber, overlapPedestrianSignals, world);
    // Booleans for circuit-wide decisions (e.g., should peds on OTHER circuits be suppressed
    // because this circuit has any solid green turn).
    boolean greenRightTurn = !greenRightTurnFacings.isEmpty();
    boolean greenLeftTurn = !greenLeftTurnFacings.isEmpty();

    // Create the upcoming phase object
    TrafficSignalPhase upcomingPhase = new TrafficSignalPhase(circuitNumber, phaseApplicability);

    // Loop through all circuits
    for (int i = 1; i <= circuits.getCircuitCount(); i++) {
      // Get the circuit
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(i - 1);

      // Handle dedicated pedestrian phase applicability
      if (phaseApplicability == TrafficSignalPhaseApplicability.PEDESTRIAN) {
        boolean pedestrianSignalsWalk = true;
        addCircuitToPhaseAllRed(circuit, upcomingPhase, pedestrianSignalsWalk);
      }
      // Handle all left turn lanes phase applicability
      else if (phaseApplicability == TrafficSignalPhaseApplicability.ALL_LEFTS) {
        if (i == circuitNumber) {
          upcomingPhase.addOffSignals(circuit.getFlashingLeftSignals());
          upcomingPhase.addRedSignals(circuit.getFlashingRightSignals());
          upcomingPhase.addGreenSignals(circuit.getLeftSignals());
          upcomingPhase.addRedSignals(circuit.getRightSignals());
          upcomingPhase.addRedSignals(circuit.getThroughSignals());
          upcomingPhase.addRedSignals(circuit.getProtectedSignals());
          upcomingPhase.addYellowSignals(circuit.getBeaconSignals());
          upcomingPhase.addRedSignals(circuit.getPedestrianBeaconSignals());
          upcomingPhase.addDontWalkSignals(circuit.getPedestrianSignals());
          upcomingPhase.addDontWalkSignals(circuit.getPedestrianAccessorySignals());
          // Drive blankout signs from the phase's GREEN/FYA assignments so a "NO RIGHT
          // TURN" sign stays lit when the right turn is red (and a "NO LEFT TURN" sign
          // hides while leftSignals are green).
          addBlankoutSignalsToPhase(world, circuit, upcomingPhase);
        } else {
          boolean pedestrianSignalsWalk = false;
          addCircuitToPhaseAllRed(circuit, upcomingPhase, pedestrianSignalsWalk);
        }
      }
      // Handle all east facing lanes phase applicability
      else if (phaseApplicability == TrafficSignalPhaseApplicability.ALL_EAST) {
        if (i == circuitNumber) {
          addActiveCircuitToDirectionalGreenPhase(world, circuit, upcomingPhase, EnumFacing.EAST);
        } else {
          boolean pedestrianSignalsWalk = false;
          addCircuitToPhaseAllRed(circuit, upcomingPhase, pedestrianSignalsWalk);
        }
      }
      // Handle all west facing lanes phase applicability
      else if (phaseApplicability == TrafficSignalPhaseApplicability.ALL_WEST) {
        if (i == circuitNumber) {
          addActiveCircuitToDirectionalGreenPhase(world, circuit, upcomingPhase, EnumFacing.WEST);
        } else {
          boolean pedestrianSignalsWalk = false;
          addCircuitToPhaseAllRed(circuit, upcomingPhase, pedestrianSignalsWalk);
        }
      }
      // Handle all north facing lanes phase applicability
      else if (phaseApplicability == TrafficSignalPhaseApplicability.ALL_NORTH) {
        if (i == circuitNumber) {
          addActiveCircuitToDirectionalGreenPhase(world, circuit, upcomingPhase, EnumFacing.NORTH);
        } else {
          boolean pedestrianSignalsWalk = false;
          addCircuitToPhaseAllRed(circuit, upcomingPhase, pedestrianSignalsWalk);
        }
      }
      // Handle all south facing lanes phase applicability
      else if (phaseApplicability == TrafficSignalPhaseApplicability.ALL_SOUTH) {
        if (i == circuitNumber) {
          addActiveCircuitToDirectionalGreenPhase(world, circuit, upcomingPhase, EnumFacing.SOUTH);
        } else {
          boolean pedestrianSignalsWalk = false;
          addCircuitToPhaseAllRed(circuit, upcomingPhase, pedestrianSignalsWalk);
        }
      }
      // Handle all throughs and rights phase applicability
      else if (phaseApplicability == TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS) {
        if (i == circuitNumber) {
          upcomingPhase.addGreenSignals(circuit.getThroughSignals());
          upcomingPhase.addOffSignals(circuit.getPedestrianBeaconSignals());
          upcomingPhase.addOffSignals(circuit.getBeaconSignals());
          upcomingPhase.addDontWalkSignals(circuit.getPedestrianSignals());
          upcomingPhase.addDontWalkSignals(circuit.getPedestrianAccessorySignals());
          // Resolve facings sets for this branch's per-direction arbitration.
          // - Right: demand-derived in overlap; "all directions present" without overlap
          //   (right unconditionally solid green when peds aren't a concern).
          // - Left: demand-derived in overlap; in non-overlap, the legacy fallback gave
          //   solid green only on single-direction circuits, which we replicate by using
          //   "all directions present" only when areSignalsFacingSameDirection is true.
          EnumSet<EnumFacing> activeRightFacings;
          EnumSet<EnumFacing> activeLeftFacings;
          if (overlapPedestrianSignals) {
            activeRightFacings = greenRightTurnFacings;
            activeLeftFacings = greenLeftTurnFacings;
          } else {
            activeRightFacings = collectFacingsFromSignals(world, circuit.getRightSignals());
            activeRightFacings.addAll(
                collectFacingsFromSignals(world, circuit.getFlashingRightSignals()));
            if (circuit.areSignalsFacingSameDirection(world)) {
              activeLeftFacings = collectFacingsFromSignals(world, circuit.getLeftSignals());
              activeLeftFacings.addAll(
                  collectFacingsFromSignals(world, circuit.getFlashingLeftSignals()));
            } else {
              activeLeftFacings = EnumSet.noneOf(EnumFacing.class);
            }
          }
          applyRightTurnAndProtectedStatesByFacing(world, circuit, upcomingPhase,
              activeRightFacings);
          applyLeftTurnStatesByFacing(world, circuit, upcomingPhase, activeLeftFacings);
          addBlankoutSignalsToPhase(world, circuit, upcomingPhase);
        } else {
          addCircuitToPhaseAllRed(circuit, upcomingPhase,
              overlapPedestrianSignals && !greenRightTurn && !greenLeftTurn);
        }
      }
      // Handle all throughs and protected rights phase applicability
      else if (phaseApplicability
          == TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTED_RIGHTS) {
        if (i == circuitNumber) {
          // Right + protected: demand-derived per-direction arbitration.
          applyRightTurnAndProtectedStatesByFacing(world, circuit, upcomingPhase,
              greenRightTurnFacings);
          upcomingPhase.addGreenSignals(circuit.getThroughSignals());
          upcomingPhase.addOffSignals(circuit.getPedestrianBeaconSignals());
          upcomingPhase.addOffSignals(circuit.getBeaconSignals());
          upcomingPhase.addDontWalkSignals(circuit.getPedestrianSignals());
          upcomingPhase.addDontWalkSignals(circuit.getPedestrianAccessorySignals());
          // Left: demand-derived in overlap mode; legacy single-direction fallback when no
          // overlap is configured (all directions present get protected green).
          EnumSet<EnumFacing> activeLeftFacings;
          if (greenLeftTurn) {
            activeLeftFacings = greenLeftTurnFacings;
          } else if (circuit.areSignalsFacingSameDirection(world)
              && !overlapPedestrianSignals) {
            activeLeftFacings = collectFacingsFromSignals(world, circuit.getLeftSignals());
            activeLeftFacings.addAll(
                collectFacingsFromSignals(world, circuit.getFlashingLeftSignals()));
          } else {
            activeLeftFacings = EnumSet.noneOf(EnumFacing.class);
          }
          applyLeftTurnStatesByFacing(world, circuit, upcomingPhase, activeLeftFacings);
          addBlankoutSignalsToPhase(world, circuit, upcomingPhase);
        } else {
          addCircuitToPhaseAllRed(circuit, upcomingPhase,
              overlapPedestrianSignals && !greenRightTurn && !greenLeftTurn);
        }
      }
      // Handle all throughs and protecteds phase applicability
      else if (phaseApplicability == TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS) {
        if (i == circuitNumber) {
          buildAllThroughsProtectedsActivePhase(world, circuit, upcomingPhase,
              greenLeftTurnFacings, greenRightTurnFacings);
        } else {
          addCircuitToPhaseAllRed(circuit, upcomingPhase,
              overlapPedestrianSignals && !greenRightTurn && !greenLeftTurn);
        }
      } else {
        throw new IllegalStateException(
            "Encountered an improper phase applicability during standard " +
                "operation: " +
                phaseApplicability);
      }

    }

    // Add overlaps if necessary
    upcomingPhase =
        TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(upcomingPhase, overlaps);

    // Return the upcoming phase
    return upcomingPhase;
  }

  /**
   * Utility method to add all signals from the specified active
   * {@link TrafficSignalControllerCircuit} to their respective states (green or red) in the
   * specified {@link TrafficSignalPhase} based on the specified {@link EnumFacing}.
   *
   * @param world            The world where the traffic signal controller and devices are located.
   * @param circuit          The active circuit to add to the specified phase.
   * @param destinationPhase The phase to add the specified active circuit to.
   * @param enumFacing       The {@link EnumFacing} direction to apply green signals to.
   *
   * @since 1.0
   */
  public static void addActiveCircuitToDirectionalGreenPhase(World world,
      TrafficSignalControllerCircuit circuit,
      TrafficSignalPhase destinationPhase,
      EnumFacing enumFacing) {
    // Get directionally filtered signal lists
    Tuple<List<BlockPos>, List<BlockPos>> flashingLeftSignals =
        filterSignalsByFacingDirection(world,
            circuit.getFlashingLeftSignals(),
            enumFacing);
    Tuple<List<BlockPos>, List<BlockPos>> flashingRightSignals =
        filterSignalsByFacingDirection(world,
            circuit.getFlashingRightSignals(),
            enumFacing);
    Tuple<List<BlockPos>, List<BlockPos>> leftSignals = filterSignalsByFacingDirection(world,
        circuit.getLeftSignals(),
        enumFacing);
    Tuple<List<BlockPos>, List<BlockPos>> rightSignals = filterSignalsByFacingDirection(world,
        circuit.getRightSignals(),
        enumFacing);
    Tuple<List<BlockPos>, List<BlockPos>> throughSignals = filterSignalsByFacingDirection(world,
        circuit.getThroughSignals(),
        enumFacing);
    Tuple<List<BlockPos>, List<BlockPos>> pedestrianBeaconSignals =
        filterSignalsByFacingDirection(world,
            circuit.getPedestrianBeaconSignals(),
            enumFacing);
    Tuple<List<BlockPos>, List<BlockPos>> protectedSignals = filterSignalsByFacingDirection(world,
        circuit.getProtectedSignals(),
        enumFacing);

    applyDirectionalGreenSignalAssignments(circuit, destinationPhase,
        flashingLeftSignals, flashingRightSignals, leftSignals, rightSignals,
        throughSignals, pedestrianBeaconSignals, protectedSignals);
    addBlankoutSignalsToPhase(world, circuit, destinationPhase);
  }

  /**
   * Applies the per-direction signal-state assignments for a directional green phase
   * (ALL_EAST/WEST/NORTH/SOUTH) given pre-filtered signal tuples. Each tuple's
   * {@link Tuple#getFirst()} is the matching-direction subset and {@link Tuple#getSecond()} is
   * the opposite-direction subset.
   *
   * <p>Matching-direction lefts become protected green when an add-on left arrow exists, FYA
   * permissive otherwise. Matching-direction right turns keep their solid green unless a
   * matching-direction protected (transit/bike) signal exists on the same approach, in which
   * case the right turn is held at FYA so the protected can serve concurrently without
   * conflict. Opposite-direction signals all go red.</p>
   *
   * <p>Extracted from {@link #addActiveCircuitToDirectionalGreenPhase} so the world-free phase
   * logic can be unit-tested without a Minecraft world.</p>
   *
   * @since 1.0
   */
  static void applyDirectionalGreenSignalAssignments(
      TrafficSignalControllerCircuit circuit,
      TrafficSignalPhase destinationPhase,
      Tuple<List<BlockPos>, List<BlockPos>> flashingLeftSignals,
      Tuple<List<BlockPos>, List<BlockPos>> flashingRightSignals,
      Tuple<List<BlockPos>, List<BlockPos>> leftSignals,
      Tuple<List<BlockPos>, List<BlockPos>> rightSignals,
      Tuple<List<BlockPos>, List<BlockPos>> throughSignals,
      Tuple<List<BlockPos>, List<BlockPos>> pedestrianBeaconSignals,
      Tuple<List<BlockPos>, List<BlockPos>> protectedSignals) {
    // Matching direction left turn: protected green if green arrow exists, FYA if not
    if (!leftSignals.getFirst().isEmpty()) {
      destinationPhase.addOffSignals(flashingLeftSignals.getFirst());
      destinationPhase.addGreenSignals(leftSignals.getFirst());
    } else {
      destinationPhase.addFyaSignals(flashingLeftSignals.getFirst());
    }
    // Non-matching direction: fully stopped, FYA and left both RED
    destinationPhase.addRedSignals(flashingLeftSignals.getSecond());
    destinationPhase.addRedSignals(leftSignals.getSecond());
    // Matching direction right vs. matching direction protected (transit/bike): solid
    // green right would conflict with a same-direction protected indication, so when a
    // matching protected exists, hold the right turn at FYA (permissive) and serve the
    // protected green concurrently. When no matching protected is configured, the right
    // turn keeps its solid green as before.
    boolean hasMatchingProtected = !protectedSignals.getFirst().isEmpty();
    if (hasMatchingProtected) {
      destinationPhase.addFyaSignals(flashingRightSignals.getFirst());
      destinationPhase.addRedSignals(rightSignals.getFirst());
      destinationPhase.addGreenSignals(protectedSignals.getFirst());
    } else {
      destinationPhase.addOffSignals(flashingRightSignals.getFirst());
      destinationPhase.addGreenSignals(rightSignals.getFirst());
    }
    destinationPhase.addRedSignals(flashingRightSignals.getSecond());
    destinationPhase.addRedSignals(rightSignals.getSecond());
    destinationPhase.addGreenSignals(throughSignals.getFirst());
    destinationPhase.addRedSignals(throughSignals.getSecond());
    destinationPhase.addOffSignals(pedestrianBeaconSignals.getFirst());
    destinationPhase.addRedSignals(pedestrianBeaconSignals.getSecond());
    destinationPhase.addRedSignals(protectedSignals.getSecond());
    destinationPhase.addOffSignals(circuit.getBeaconSignals());
    destinationPhase.addDontWalkSignals(circuit.getPedestrianSignals());
    destinationPhase.addDontWalkSignals(circuit.getPedestrianAccessorySignals());
  }

  /**
   * Utility method to filter signals in the specified {@link List<BlockPos>} by their facing
   * direction. This method will return a {@link Tuple} containing two {@link List<BlockPos>}, the
   * first containing the signals that are facing the specified direction, and the second containing
   * the signals that are not facing the specified direction.
   *
   * @param world            The world where the traffic signal controller and devices are located.
   * @param signalBlockPoses The {@link List<BlockPos>} containing the signals to filter.
   * @param enumFacing       The {@link EnumFacing} to filter the signals by.
   *
   * @return A {@link Tuple} containing two {@link List<BlockPos>}, the first containing the signals
   *     that are facing the specified direction, and the second containing the signals that are not
   *     facing the specified direction.
   *
   * @since 1.0
   */
  public static Tuple<List<BlockPos>, List<BlockPos>> filterSignalsByFacingDirection(World world,
      List<BlockPos> signalBlockPoses,
      EnumFacing enumFacing) {
    // Create facing direction stream collector predicate
    Predicate<BlockPos> facingDirectionPredicate = signalPos -> {
      IBlockState blockState = world.getBlockState(signalPos);
      EnumFacing sensorFacingDirection = blockState.getValue(BlockHorizontal.FACING);
      return sensorFacingDirection == enumFacing;
    };

    // Partition the signal list by the facing direction predicate
    Map<Boolean, List<BlockPos>> filteredSignalLists = signalBlockPoses.stream()
        .collect((Collectors.partitioningBy(
            facingDirectionPredicate)));

    // Return the filtered signal lists
    return new Tuple<>(filteredSignalLists.get(true), filteredSignalLists.get(false));
  }

  /**
   * Partitions a signal list by membership in a set of facings. The first list of the returned
   * tuple contains signals whose {@link BlockHorizontal#FACING} is in {@code matchingFacings};
   * the second list contains the rest. Signals whose facing can't be resolved (null world,
   * unloaded chunk, missing FACING property) fall into the second list.
   *
   * <p>Used by per-direction phase building to split signal lists into "this direction is
   * promoted" vs. "this direction is not promoted" subsets.</p>
   *
   * @param world            The world for facing lookups; if {@code null}, every signal lands in
   *                         the non-matching list.
   * @param signalBlockPoses The signal positions to partition.
   * @param matchingFacings  The set of facings considered "matching."
   *
   * @return A tuple of (matching-facing signals, non-matching-facing signals).
   *
   * @since 1.0
   */
  public static Tuple<List<BlockPos>, List<BlockPos>> partitionSignalsByFacingSet(World world,
      List<BlockPos> signalBlockPoses,
      EnumSet<EnumFacing> matchingFacings) {
    List<BlockPos> matching = new ArrayList<>();
    List<BlockPos> nonMatching = new ArrayList<>();
    if (signalBlockPoses == null || signalBlockPoses.isEmpty()) {
      return new Tuple<>(matching, nonMatching);
    }
    for (BlockPos pos : signalBlockPoses) {
      EnumFacing facing = world == null ? null : signalFacingOrNull(world, pos);
      if (facing != null && matchingFacings != null && matchingFacings.contains(facing)) {
        matching.add(pos);
      } else {
        nonMatching.add(pos);
      }
    }
    return new Tuple<>(matching, nonMatching);
  }

  /**
   * Utility method to filter signals in the specified {@link List<BlockPos>} by whether they should
   * flash. This method will return a {@link Tuple} containing two {@link List<BlockPos>}, the first
   * containing the signals that should flash, and the second containing the signals that should not
   * flash.
   *
   * @param world            The world where the traffic signal controller and devices are located.
   * @param signalBlockPoses The {@link List<BlockPos>} containing the signals to filter.
   *
   * @return A {@link Tuple} containing two {@link List<BlockPos>}, the first containing the signals
   *     that should flash, and the second containing the signals that should not flash.
   *
   * @since 1.0
   */
  public static Tuple<List<BlockPos>, List<BlockPos>> filterSignalsByShouldFlash(World world,
      List<BlockPos> signalBlockPoses) {
    // Create flash enabled stream collector predicate
    Predicate<BlockPos> flashEnabledPredicate = signalPos -> {
      boolean shouldFlash = false;
      if (world != null) {
        IBlockState blockState = world.getBlockState(signalPos);
        if (blockState.getBlock() instanceof AbstractBlockControllableSignal) {
          shouldFlash = ((AbstractBlockControllableSignal) blockState.getBlock()).doesFlash();
        }
      } else {
        shouldFlash = true;
      }
      return shouldFlash;
    };

    // Partition the signal list by the flash enabled predicate
    Map<Boolean, List<BlockPos>> filteredSignalLists = signalBlockPoses.stream()
        .collect((Collectors.partitioningBy(
            flashEnabledPredicate)));

    // Return the filtered signal lists
    return new Tuple<>(filteredSignalLists.get(true), filteredSignalLists.get(false));
  }

  /**
   * Checks whether all circuits in the controller have at least one sensor configured.
   * When true, the controller can rely on sensor data for demand-based decisions like
   * vehicle phase recall (holding green when no conflicting demand exists).
   *
   * @param circuits The configured/connected circuits of the traffic signal controller.
   *
   * @return {@code true} if every circuit has at least one sensor, {@code false} otherwise.
   *
   * @since 1.0
   */
  /**
   * Returns whether the given phase applicability represents a through-type movement
   * (straight, directional, or through+turns). Through-type demand is compatible with
   * the current circuit's green phase and can be served without a phase change — making
   * it safe for phase recall and ped recycle decisions. Non-through demand (left turns,
   * pedestrian phases) requires its own dedicated phase and must not be suppressed.
   *
   * @param applicability The phase applicability to check.
   *
   * @return {@code true} if the applicability is a through-type movement, {@code false}
   *     for left turns, pedestrian phases, or other non-through movements.
   *
   * @since 1.0
   */
  public static boolean isThroughTypeApplicability(TrafficSignalPhaseApplicability applicability) {
    return isOmnidirectionalThroughType(applicability) || isDirectionalPhase(applicability);
  }

  public static boolean isOmnidirectionalThroughType(
      TrafficSignalPhaseApplicability applicability) {
    return applicability == TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS ||
        applicability == TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTED_RIGHTS ||
        applicability == TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS;
  }

  public static boolean isDirectionalPhase(TrafficSignalPhaseApplicability applicability) {
    return applicability == TrafficSignalPhaseApplicability.ALL_EAST ||
        applicability == TrafficSignalPhaseApplicability.ALL_WEST ||
        applicability == TrafficSignalPhaseApplicability.ALL_NORTH ||
        applicability == TrafficSignalPhaseApplicability.ALL_SOUTH;
  }

  /**
   * Returns whether two phase applicabilities are in the same movement category and thus
   * compatible for phase recall. Omnidirectional through-type variants (ALL_THROUGHS_*)
   * are compatible with each other. Directional phases (ALL_EAST etc.) are NOT compatible
   * with omnidirectional phases because they serve fundamentally different signal
   * arrangements. All other types must match exactly (ALL_LEFTS↔ALL_LEFTS, etc.).
   *
   * @param a The first phase applicability.
   * @param b The second phase applicability.
   *
   * @return {@code true} if both applicabilities are in the same movement category.
   *
   * @since 1.0
   */
  public static boolean isSamePhaseCategory(TrafficSignalPhaseApplicability a,
      TrafficSignalPhaseApplicability b) {
    if (isOmnidirectionalThroughType(a) && isOmnidirectionalThroughType(b)) {
      return true;
    }
    return a == b;
  }

  /**
   * Computes the effective left turn demand for a circuit, accounting for permissive FYA
   * (flashing yellow arrow) turns. When a direction has a FYA left signal active, a single
   * vehicle in the left turn detection zone is expected to clear on the permissive turn.
   * Only when 2+ vehicles are detected in a FYA-equipped direction does it count toward
   * protected left turn demand. Directions without FYA count all detections normally.
   *
   * <p>This handles mixed installations where one approach has FYA and another has a
   * standard protected-only left turn signal.</p>
   *
   * <p><b>Demand semantic vs. {@link #computeGreenLeftTurn}:</b> this returns FYA-adjusted
   * directional counts and is used by phase <i>priority</i> selection — i.e., should the
   * controller switch to ALL_LEFTS to serve waiting left turners? A single car on a FYA
   * direction is not enough; it can clear permissively. {@code computeGreenLeftTurn} uses
   * raw counts and decides whether the left turn shows protected green vs. FYA <i>during
   * the through phase that's already running</i>, where serving a single waiting car a
   * protected arrow costs nothing. The two semantics are intentionally different.</p>
   *
   * @param world   The world where the controller is located.
   * @param circuit The circuit to compute effective left demand for.
   * @param summary The sensor summary for the circuit.
   *
   * @return The effective left turn demand count, adjusted for FYA permissive turns.
   *
   * @since 1.0
   */
  public static int getEffectiveLeftDemand(World world, TrafficSignalControllerCircuit circuit,
      TrafficSignalSensorSummary summary) {
    // If no left demand at all, return 0 immediately
    if (summary.getLeftTotal() == 0) {
      return 0;
    }

    // If the circuit has no regular left signals, ALL_LEFTS cannot be served — there is
    // no signal to display a protected green arrow. Left turns are handled permissively
    // via FYA during through phases instead.
    if (circuit.getLeftSignals().isEmpty()) {
      return 0;
    }

    // ALL_LEFTS is only safe when all signals on the circuit face the same direction.
    // On a multi-direction circuit, opposing left turns conflict with each other —
    // left demand must be served permissively via FYA, never as a protected phase.
    if (world != null && !circuit.areSignalsFacingSameDirection(world)) {
      return 0;
    }

    // Determine which directions have flashing left signals (FYA)
    boolean fyaEast = false, fyaWest = false, fyaNorth = false, fyaSouth = false;
    if (world != null) for (BlockPos signalPos : circuit.getFlashingLeftSignals()) {
      IBlockState blockState = world.getBlockState(signalPos);
      if (blockState.getBlock() instanceof AbstractBlockControllableSignal) {
        EnumFacing facing = blockState.getValue(BlockHorizontal.FACING);
        switch (facing) {
          case EAST:  fyaEast = true;  break;
          case WEST:  fyaWest = true;  break;
          case NORTH: fyaNorth = true; break;
          case SOUTH: fyaSouth = true; break;
        }
      }
    }

    // For each direction, apply the FYA threshold: if that direction has FYA,
    // a single detection is assumed to clear on the permissive turn (don't count it).
    // Two or more detections indicate the permissive turn isn't sufficient.
    int effectiveCount = 0;
    effectiveCount += adjustForFya(summary.getLeftEast(), fyaEast);
    effectiveCount += adjustForFya(summary.getLeftWest(), fyaWest);
    effectiveCount += adjustForFya(summary.getLeftNorth(), fyaNorth);
    effectiveCount += adjustForFya(summary.getLeftSouth(), fyaSouth);
    return effectiveCount;
  }

  /**
   * Computes the effective right turn demand for a circuit, mirroring
   * {@link #getEffectiveLeftDemand} but for right-turn detection zones. Per-direction counts
   * are FYA-adjusted: a single vehicle on a FYA-equipped direction is assumed to clear on the
   * permissive turn and contributes 0 to the demand total.
   *
   * <p><b>Demand semantic vs. {@link #computeGreenRightTurn}:</b> see
   * {@link #getEffectiveLeftDemand} — phase priority uses FYA-adjusted demand here, while the
   * through-phase indication arbitration in {@code computeGreenRightTurn} uses raw counts.</p>
   *
   * @param world   The world where the controller is located.
   * @param circuit The circuit to compute effective right demand for.
   * @param summary The sensor summary for the circuit.
   *
   * @return The effective right turn demand count, adjusted for FYA permissive turns.
   *
   * @since 1.0
   */
  public static int getEffectiveRightDemand(World world, TrafficSignalControllerCircuit circuit,
      TrafficSignalSensorSummary summary) {
    if (summary.getRightTotal() == 0) {
      return 0;
    }

    boolean fyaEast = false, fyaWest = false, fyaNorth = false, fyaSouth = false;
    if (world != null) for (BlockPos signalPos : circuit.getFlashingRightSignals()) {
      IBlockState blockState = world.getBlockState(signalPos);
      if (blockState.getBlock() instanceof AbstractBlockControllableSignal) {
        EnumFacing facing = blockState.getValue(BlockHorizontal.FACING);
        switch (facing) {
          case EAST:  fyaEast = true;  break;
          case WEST:  fyaWest = true;  break;
          case NORTH: fyaNorth = true; break;
          case SOUTH: fyaSouth = true; break;
        }
      }
    }

    int effectiveCount = 0;
    effectiveCount += adjustForFya(summary.getRightEast(), fyaEast);
    effectiveCount += adjustForFya(summary.getRightWest(), fyaWest);
    effectiveCount += adjustForFya(summary.getRightNorth(), fyaNorth);
    effectiveCount += adjustForFya(summary.getRightSouth(), fyaSouth);
    return effectiveCount;
  }

  private static int adjustForFya(int leftCount, boolean hasFya) {
    if (hasFya && leftCount <= 1) {
      return 0; // Single detection with FYA available — assume permissive turn
    }
    return leftCount;
  }

  public static int getEffectiveDirectionalDemand(World world,
      TrafficSignalControllerCircuit circuit,
      TrafficSignalSensorSummary summary,
      EnumFacing direction) {
    int standard;
    int left;
    int right;
    switch (direction) {
      case EAST:  standard = summary.getStandardEast();  left = summary.getLeftEast();  right = summary.getRightEast();  break;
      case WEST:  standard = summary.getStandardWest();  left = summary.getLeftWest();  right = summary.getRightWest();  break;
      case NORTH: standard = summary.getStandardNorth(); left = summary.getLeftNorth(); right = summary.getRightNorth(); break;
      case SOUTH: standard = summary.getStandardSouth(); left = summary.getLeftSouth(); right = summary.getRightSouth(); break;
      default: return 0;
    }
    boolean hasLeftFya = false;
    for (BlockPos signalPos : circuit.getFlashingLeftSignals()) {
      IBlockState blockState = world.getBlockState(signalPos);
      if (blockState.getBlock() instanceof AbstractBlockControllableSignal) {
        if (blockState.getValue(BlockHorizontal.FACING) == direction) {
          hasLeftFya = true;
          break;
        }
      }
    }
    boolean hasRightFya = false;
    for (BlockPos signalPos : circuit.getFlashingRightSignals()) {
      IBlockState blockState = world.getBlockState(signalPos);
      if (blockState.getBlock() instanceof AbstractBlockControllableSignal) {
        if (blockState.getValue(BlockHorizontal.FACING) == direction) {
          hasRightFya = true;
          break;
        }
      }
    }
    return standard + adjustForFya(left, hasLeftFya) + adjustForFya(right, hasRightFya);
  }

  /**
   * Sets the on/off state of every "no turn" blankout sign in the circuit based on whether
   * the corresponding turn is permitted at the sign's facing direction in this phase.
   *
   * <p>A turn is permitted at a given direction when any of the circuit's signals on that
   * side at that facing is in either {@code GREEN} (protected) or {@code FYA} (permissive
   * flashing yellow) within this phase. Both indications legally allow a driver to make the
   * turn, so a "NO TURN" blankout sign at the same facing would directly contradict the
   * active signal indication. When neither protected nor permissive is shown for the
   * blankout's facing, the turn is forbidden and the sign is lit.</p>
   *
   * <p>The per-facing match matters in directional phases (ALL_EAST/WEST/NORTH/SOUTH) where
   * only matching-direction signals get green/FYA — a west-facing blankout must remain lit
   * during ALL_EAST even though east-facing left turns are green. In omnidirectional phases
   * (ALL_THROUGHS_*), all directions of a turn type share the same state, so per-facing and
   * facing-agnostic matching produce identical results.</p>
   *
   * <p>Must be called <i>after</i> the phase's vehicle-signal lists are populated for the
   * circuit, since the helper inspects {@link TrafficSignalPhase#getGreenSignals()} and
   * {@link TrafficSignalPhase#getFyaSignals()} to determine turn allowance. When
   * {@code world} is {@code null} (test path with no blankouts), all blankouts are emitted
   * to the walk list (sign visible) since no facing lookup is possible.</p>
   *
   * @param world   The world containing the blankout box tile entities (may be {@code null}
   *                in tests with no blankout signals configured).
   * @param circuit The circuit whose blankout signals to update.
   * @param phase   The phase being built; the helper reads its green/FYA assignments and
   *                writes the blankout signals into the walk/don't-walk lists.
   *
   * @since 1.0
   */
  private static void addBlankoutSignalsToPhase(World world,
      TrafficSignalControllerCircuit circuit,
      TrafficSignalPhase phase) {
    EnumSet<EnumFacing> leftAllowedFacings =
        collectAllowedFacings(world, circuit.getLeftSignals(), phase.getGreenSignals(),
            circuit.getFlashingLeftSignals(), phase.getFyaSignals());
    EnumSet<EnumFacing> rightAllowedFacings =
        collectAllowedFacings(world, circuit.getRightSignals(), phase.getGreenSignals(),
            circuit.getFlashingRightSignals(), phase.getFyaSignals());

    List<BlockPos> onSignals = new ArrayList<>();
    List<BlockPos> offSignals = new ArrayList<>();
    for (BlockPos pos : circuit.getNoTurnBlankoutSignals()) {
      boolean turnOff = false;
      if (world != null) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityBlankoutBox) {
          BlankoutBoxType type = ((TileEntityBlankoutBox) te).getBlankoutType();
          EnumFacing boxFacing = signalFacingOrNull(world, pos);
          if (boxFacing != null) {
            if (type == BlankoutBoxType.NO_RIGHT_TURN
                && rightAllowedFacings.contains(boxFacing)) {
              turnOff = true;
            } else if (type == BlankoutBoxType.NO_LEFT_TURN
                && leftAllowedFacings.contains(boxFacing)) {
              turnOff = true;
            }
          }
        }
      }
      if (turnOff) {
        offSignals.add(pos);
      } else {
        onSignals.add(pos);
      }
    }
    phase.addWalkSignals(onSignals);
    phase.addDontWalkSignals(offSignals);
  }

  /**
   * Builds the set of {@link EnumFacing} directions at which a turn is "allowed" — i.e.,
   * has either a protected-green signal or a permissive-FYA signal active in the phase.
   * Used by {@link #addBlankoutSignalsToPhase} to match each blankout sign to the same-
   * direction turn state.
   *
   * @param world             The world for facing lookups; if {@code null}, returns an
   *                          empty set (callers fall back accordingly).
   * @param protectedSignals  The circuit's protected-green-arrow signals (e.g., leftSignals).
   * @param greenSignals      The phase's green-state signals.
   * @param permissiveSignals The circuit's flashing-arrow signals (e.g., flashingLeftSignals).
   * @param fyaSignals        The phase's FYA-state signals.
   *
   * @return The directions where the turn is allowed.
   *
   * @since 1.0
   */
  private static EnumSet<EnumFacing> collectAllowedFacings(World world,
      List<BlockPos> protectedSignals,
      List<BlockPos> greenSignals,
      List<BlockPos> permissiveSignals,
      List<BlockPos> fyaSignals) {
    EnumSet<EnumFacing> allowed = EnumSet.noneOf(EnumFacing.class);
    if (world == null) {
      return allowed;
    }
    for (BlockPos pos : protectedSignals) {
      if (greenSignals.contains(pos)) {
        EnumFacing facing = signalFacingOrNull(world, pos);
        if (facing != null) allowed.add(facing);
      }
    }
    for (BlockPos pos : permissiveSignals) {
      if (fyaSignals.contains(pos)) {
        EnumFacing facing = signalFacingOrNull(world, pos);
        if (facing != null) allowed.add(facing);
      }
    }
    return allowed;
  }

  /**
   * Reads the {@link BlockHorizontal#FACING} property from the block at {@code pos}, or
   * {@code null} if the block does not carry that property (defensive against malformed
   * states from chunk-edge or unloaded-block conditions).
   */
  private static EnumFacing signalFacingOrNull(World world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    if (state.getProperties().containsKey(BlockHorizontal.FACING)) {
      return state.getValue(BlockHorizontal.FACING);
    }
    return null;
  }

  public static boolean allCircuitsHaveSensors(TrafficSignalControllerCircuits circuits) {
    if (circuits.getCircuitCount() == 0) {
      return false;
    }
    for (int i = 0; i < circuits.getCircuitCount(); i++) {
      if (circuits.getCircuit(i).getSensors().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Validates that every sensor on every circuit faces the same direction as at least one signal
   * head linked to the same circuit. Returns a human-readable fault message describing the first
   * mismatch found, or {@code null} when all sensors align with their circuit's signal facings.
   *
   * <p>The per-direction FYA-vs-protected demand arbitration (see {@link #getEffectiveLeftDemand}
   * and {@link #getEffectiveRightDemand}) correlates each sensor's directional zone count with
   * FYA signals of matching {@code FACING}. A facing mismatch silently breaks that correlation —
   * single-vehicle FYA-clearance suppression no longer applies and every detection counts as
   * protected demand. This validator surfaces the misconfiguration as a controller fault so the
   * setup error gets attention instead of degrading silently.</p>
   *
   * <p>This check is intended for {@link TrafficSignalControllerMode#NORMAL} where the
   * facing-correlation invariant is load-bearing. Other modes (FLASH, MANUAL_OFF, etc.) don't
   * use sensor data the same way and should not be subject to this check.</p>
   *
   * <p>Circuits with no sensors or no signal facings are skipped — there's nothing to validate.
   * Sensors in unloaded chunks or with malformed states (no FACING property) are also skipped
   * defensively, since {@link World#getBlockState} returns air for unloaded positions.</p>
   *
   * @param world    The world containing the controller and devices.
   * @param circuits The configured circuits to validate.
   *
   * @return A fault message describing the first mismatch found, or {@code null} if every sensor
   *     facing matches a signal facing on the same circuit.
   *
   * @since 1.0
   */
  public static String validateSensorFacings(World world,
      TrafficSignalControllerCircuits circuits) {
    if (world == null || circuits == null) {
      return null;
    }
    for (int i = 0; i < circuits.getCircuitCount(); i++) {
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(i);
      if (circuit.getSensors().isEmpty()) {
        continue;
      }

      EnumSet<EnumFacing> signalFacings = EnumSet.noneOf(EnumFacing.class);
      addSignalFacingsTo(world, circuit.getThroughSignals(), signalFacings);
      addSignalFacingsTo(world, circuit.getLeftSignals(), signalFacings);
      addSignalFacingsTo(world, circuit.getRightSignals(), signalFacings);
      addSignalFacingsTo(world, circuit.getFlashingLeftSignals(), signalFacings);
      addSignalFacingsTo(world, circuit.getFlashingRightSignals(), signalFacings);
      addSignalFacingsTo(world, circuit.getProtectedSignals(), signalFacings);
      addSignalFacingsTo(world, circuit.getPedestrianBeaconSignals(), signalFacings);

      if (signalFacings.isEmpty()) {
        continue;
      }

      List<Tuple<BlockPos, EnumFacing>> sensorFacings = new ArrayList<>();
      for (BlockPos sensorPos : circuit.getSensors()) {
        sensorFacings.add(new Tuple<>(sensorPos, signalFacingOrNull(world, sensorPos)));
      }

      String mismatch = findCircuitSensorFacingMismatch(i + 1, signalFacings, sensorFacings);
      if (mismatch != null) {
        return mismatch;
      }
    }
    return null;
  }

  /**
   * Pure helper for {@link #validateSensorFacings}: given a circuit's signal facings and the
   * facing of each sensor on that circuit, returns a fault message for the first sensor whose
   * facing isn't present in the signal set, or {@code null} if all sensors align.
   *
   * <p>Sensors with a {@code null} facing (unloaded chunk, malformed state) are skipped — the
   * validator can't make a determination without the sensor's actual facing. An empty
   * {@code signalFacings} set yields {@code null}; a circuit with no signals has no facing
   * constraint to enforce.</p>
   *
   * @param circuitNumber The 1-based circuit number to include in the fault message.
   * @param signalFacings The set of facings present on signal heads linked to this circuit.
   * @param sensorFacings The (position, facing) pairs for each sensor on this circuit.
   *
   * @return A fault message for the first mismatch, or {@code null} if none.
   *
   * @since 1.0
   */
  static String findCircuitSensorFacingMismatch(int circuitNumber,
      EnumSet<EnumFacing> signalFacings,
      List<Tuple<BlockPos, EnumFacing>> sensorFacings) {
    if (signalFacings == null || signalFacings.isEmpty() || sensorFacings == null) {
      return null;
    }
    for (Tuple<BlockPos, EnumFacing> sensor : sensorFacings) {
      EnumFacing sensorFacing = sensor.getSecond();
      if (sensorFacing == null) {
        continue;
      }
      if (!signalFacings.contains(sensorFacing)) {
        return "Sensor at " + sensor.getFirst() + " (facing " + sensorFacing.getName()
            + ") on circuit " + circuitNumber
            + " has no matching signal facing on the same circuit";
      }
    }
    return null;
  }

  private static void addSignalFacingsTo(World world, List<BlockPos> signals,
      EnumSet<EnumFacing> out) {
    for (BlockPos pos : signals) {
      EnumFacing facing = signalFacingOrNull(world, pos);
      if (facing != null) {
        out.add(facing);
      }
    }
  }

}
