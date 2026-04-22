package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityOverheightDetectionSensor;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalSensor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Utility class for ticking the traffic signal controller tile entity
 * ({@link com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalController}) in
 * supported {@link TrafficSignalControllerMode}s.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2023.2.0
 */
public class TrafficSignalControllerTicker {

  /**
   * Handles the tick event for the traffic signal controller using a
   * {@link ControllerTickContext} to encapsulate all parameters. This delegates to the
   * multi-parameter {@link #tick(World, TrafficSignalControllerMode, TrafficSignalControllerMode,
   * TrafficSignalControllerCircuits, TrafficSignalControllerOverlaps, TrafficSignalPhases,
   * TrafficSignalPhase, long, long, boolean, boolean, long, long, long, long, long, long, long,
   * long, long, long, long, boolean)} method.
   *
   * @param ctx the {@link ControllerTickContext} containing all tick parameters
   *
   * @return The next phase to use for the traffic signal controller. If null is returned, then the
   *     phase is not changed.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase tick(ControllerTickContext ctx) {
    return tick(ctx.getWorld(), ctx.getConfiguredMode(), ctx.getOperatingMode(),
        ctx.getCircuits(), ctx.getOverlaps(), ctx.getCachedPhases(), ctx.getOriginalPhase(),
        ctx.getTimeSinceLastPhaseApplicabilityChange(), ctx.getTimeSinceLastPhaseChange(),
        ctx.isAlternatingFlash(), ctx.isOverlapPedestrianSignals(), ctx.getYellowTime(),
        ctx.getFlashDontWalkTime(), ctx.getAllRedTime(), ctx.getMinRequestableServiceTime(),
        ctx.getMaxRequestableServiceTime(), ctx.getMinGreenTime(), ctx.getMaxGreenTime(),
        ctx.getMinGreenTimeSecondary(), ctx.getMaxGreenTimeSecondary(),
        ctx.getDedicatedPedSignalTime(), ctx.getLeadPedestrianIntervalTime(),
        ctx.isAllRedFlash());
  }

  /**
   * Handles the tick event for the traffic signal controller and passes the event to the
   * appropriate tick method based on the mode of the traffic signal controller.
   *
   * @param world                                 The world in which the traffic signal controller
   *                                              is located.
   * @param configuredMode                        The configured mode of the traffic signal
   *                                              controller.
   * @param operatingMode                         The operating mode of the traffic signal
   *                                              controller.
   * @param circuits                              The configured/connected circuits of the traffic
   *                                              signal controller.
   * @param overlaps                              The configured overlaps of the traffic signal
   *                                              controller.
   * @param cachedPhases                          The programmed phases of the traffic signal
   *                                              controller.
   * @param originalPhase                         The original (current) phase of the traffic signal
   *                                              controller.
   * @param timeSinceLastPhaseApplicabilityChange The time since the traffic signal controller last
   *                                              changed phase applicability.
   * @param timeSinceLastPhaseChange              The time since the traffic signal controller last
   *                                              changed phases.
   * @param alternatingFlash                      The alternating flash state of the traffic signal
   *                                              controller. This boolean value alternates between
   *                                              true and false each tick and is used to control
   *                                              the flashing of traffic signal devices.
   * @param overlapPedestrianSignals              The overlap pedestrian signals setting of the
   *                                              traffic signal controller. This boolean value is
   *                                              used to determine if the pedestrian signals of all
   *                                              other circuits should be overlapped when servicing
   *                                              a circuit.
   * @param yellowTime                            The yellow time for the traffic signal
   *                                              controller.
   * @param flashDontWalkTime                     The flashing don't walk time for the traffic
   *                                              signal controller.
   * @param allRedTime                            The all red time for the traffic signal
   *                                              controller.
   * @param minRequestableServiceTime             The minimum service time for the traffic signal
   *                                              controller when in requestable mode.
   * @param maxRequestableServiceTime             The maximum service time for the traffic signal
   *                                              controller when in requestable mode.
   * @param minGreenTime                          The minimum green time for the traffic signal
   *                                              controller when in normal mode.
   * @param maxGreenTime                          The maximum green time for the traffic signal
   *                                              controller when in normal mode.
   * @param minGreenTimeSecondary                 The secondary minimum green time for the traffic
   *                                              signal controller when in normal mode.
   * @param maxGreenTimeSecondary                 The secondary maximum green time for the traffic
   *                                              signal controller when in normal mode.
   * @param dedicatedPedSignalTime                The dedicated pedestrian signal time for the
   *                                              traffic signal controller when in normal mode.
   * @param leadPedestrianIntervalTime            The lead pedestrian interval time for the traffic
   *                                              signal controller when in normal mode. A value of
   *                                              zero disables the lead pedestrian interval.
   * @param allRedFlash                           Whether to use all-red flashing instead of
   *                                              yellow/red flashing when in flash mode.
   *
   * @return The next phase to use for the traffic signal controller. If null is returned, then the
   *     phase is not changed.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase tick(World world,
      TrafficSignalControllerMode configuredMode,
      TrafficSignalControllerMode operatingMode,
      TrafficSignalControllerCircuits circuits,
      TrafficSignalControllerOverlaps overlaps,
      TrafficSignalPhases cachedPhases,
      TrafficSignalPhase originalPhase,
      long timeSinceLastPhaseApplicabilityChange,
      long timeSinceLastPhaseChange,
      boolean alternatingFlash,
      boolean overlapPedestrianSignals,
      long yellowTime,
      long flashDontWalkTime,
      long allRedTime,
      long minRequestableServiceTime,
      long maxRequestableServiceTime,
      long minGreenTime,
      long maxGreenTime,
      long minGreenTimeSecondary,
      long maxGreenTimeSecondary,
      long dedicatedPedSignalTime,
      long leadPedestrianIntervalTime,
      boolean allRedFlash) {
    // Call appropriate tick method based on mode
    switch (operatingMode) {
      case FORCED_FAULT:
        return faultModeTick(cachedPhases, alternatingFlash);
      case MANUAL_OFF:
        return manualOffModeTick(cachedPhases, originalPhase);
      case REQUESTABLE:
        return requestableModeTick(world, circuits, cachedPhases, originalPhase,
            timeSinceLastPhaseApplicabilityChange, alternatingFlash, yellowTime,
            flashDontWalkTime, allRedTime, minRequestableServiceTime,
            maxRequestableServiceTime, minGreenTime);
      case RAMP_METER_FULL_TIME:
        return rampMeterFullTimeModeTick(world, circuits, cachedPhases, originalPhase);
      case RAMP_METER_PART_TIME:
        return rampMeterPartTimeModeTick(world, circuits, cachedPhases, originalPhase);
      case WRONG_WAY_DETECTION:
        // WWVDS is handled directly by the controller via wrongWayDetectionModeTick()
        // because it requires mutable tracking state not available in this parameter list.
        return null;
      case OVERHEIGHT_DETECTION:
        // Overheight detection is handled directly by the controller via
        // overheightDetectionModeTick() because it requires mutable hold timer state.
        return null;
      case NORMAL:
        return normalModeTick(world, circuits, overlaps, cachedPhases, originalPhase,
            timeSinceLastPhaseChange,
            overlapPedestrianSignals, yellowTime, flashDontWalkTime, allRedTime,
            minGreenTime, maxGreenTime, minGreenTimeSecondary, maxGreenTimeSecondary,
            dedicatedPedSignalTime, leadPedestrianIntervalTime);
      case FLASH:
      default:
        return flashModeTick(configuredMode, cachedPhases, alternatingFlash, allRedFlash);
    }
  }

  /**
   * Handles the tick event for the traffic signal controller in
   * {@link TrafficSignalControllerMode#FORCED_FAULT} mode.
   *
   * @param cachedPhases     The programmed phases of the traffic signal controller.
   * @param alternatingFlash The alternating flash state of the traffic signal controller. This
   *                         boolean value alternates between true and false each tick and is used
   *                         to control the flashing of traffic signal devices.
   *
   * @return The next phase to use for the traffic signal controller. If null is returned, then the
   *     phase is not changed.
   *
   * @implNote This method always returns the {@link TrafficSignalPhase} from
   *     {@code cachedPhases} index 0 or 1, depending on the value of {@code alternatingFlash}.
   * @since 1.0
   */
  public static TrafficSignalPhase faultModeTick(TrafficSignalPhases cachedPhases,
      boolean alternatingFlash) {
    // Return alternating fault phase (0 or 1) from cached phases
    return cachedPhases.getPhase(
        alternatingFlash ? TrafficSignalPhases.PHASE_INDEX_FAULT_1
            : TrafficSignalPhases.PHASE_INDEX_FAULT_2);
  }

  /**
   * Handles the tick event for the traffic signal controller in
   * {@link TrafficSignalControllerMode#MANUAL_OFF} mode.
   *
   * @param cachedPhases  The programmed phases of the traffic signal controller.
   * @param originalPhase The original (current) phase of the traffic signal controller.
   *
   * @return The next phase to use for the traffic signal controller. If null is returned, then the
   *     phase is not changed.
   *
   * @implNote This method always returns 0, which is the index of the off phase.
   * @since 1.0
   */
  public static TrafficSignalPhase manualOffModeTick(TrafficSignalPhases cachedPhases,
      TrafficSignalPhase originalPhase) {
    // Return off phase if original phase is null (not already set), otherwise return null
    // (already set)
    return originalPhase == null ? cachedPhases.getPhase(TrafficSignalPhases.PHASE_INDEX_OFF)
        : null;
  }

  /**
   * Handles the tick event for the traffic signal controller in
   * {@link TrafficSignalControllerMode#REQUESTABLE} mode.
   *
   * @param world                                 The world in which the traffic signal controller
   *                                              is located.
   * @param circuits                              The configured/connected circuits of the traffic
   *                                              signal controller.
   * @param cachedPhases                          The programmed phases of the traffic signal
   *                                              controller.
   * @param originalPhase                         The original (current) phase of the traffic signal
   *                                              controller.
   * @param timeSinceLastPhaseApplicabilityChange The time since the traffic signal controller last
   *                                              changed phase applicability.
   * @param alternatingFlash                      The alternating flash state of the traffic signal
   *                                              controller. This boolean value alternates between
   *                                              true and false each tick and is used to control
   *                                              the flashing of traffic signal devices.
   * @param yellowTime                            The yellow time for the traffic signal
   *                                              controller.
   * @param flashDontWalkTime                     The flashing don't walk time for the traffic
   *                                              signal controller.
   * @param allRedTime                            The all red time for the traffic signal
   *                                              controller.
   * @param minRequestableServiceTime             The minimum service time for the traffic signal
   *                                              controller when in requestable mode.
   * @param maxRequestableServiceTime             The maximum service time for the traffic signal
   *                                              controller when in requestable mode.
   * @param minGreenTime                          The minimum green time for the traffic signal
   *                                              controller when in normal mode.
   *
   * @return The next phase to use for the traffic signal controller. If null is returned, then the
   *     phase is not changed.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase requestableModeTick(World world,
      TrafficSignalControllerCircuits circuits,
      TrafficSignalPhases cachedPhases,
      TrafficSignalPhase originalPhase,
      long timeSinceLastPhaseApplicabilityChange,
      boolean alternatingFlash,
      long yellowTime,
      long flashDontWalkTime,
      long allRedTime,
      long minRequestableServiceTime,
      long maxRequestableServiceTime,
      long minGreenTime) {
    // Create variable to store next phase (null phase indicates no change)
    TrafficSignalPhase nextPhase = null;

    // If original phase is null, switch to default green phase
    if (originalPhase == null) {
      nextPhase = cachedPhases.getPhase(TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN);
    }
    // If currently in default green phase, time met, and request count is greater than zero,
    // start switch to
    // service phasing
    else if (originalPhase.getApplicability()
        == TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN &&
        timeSinceLastPhaseApplicabilityChange >= minGreenTime &&
        (circuits.getCircuits()
            .stream()
            .mapToInt(value -> value.getPedestrianAccessoriesRequestCount(world))
            .sum() > 0)) {

      // Check if flashing don't walk phase is needed
      boolean flashingDontWalkNeeded = true;
      boolean flashingBeaconNeeded = true;
      if (circuits.getCircuitCount() > 0) {
        flashingDontWalkNeeded = circuits.getCircuit(0).getPedestrianSignals().size() > 0;
        flashingBeaconNeeded = circuits.getCircuit(0).getPedestrianBeaconSignals().size() > 0;
      }

      // Switch to flashing don't walk phase if needed
      if (flashingDontWalkNeeded) {
        nextPhase = cachedPhases.getPhase(
            TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW);
      }
      // Skip to flashing yellow pedestrian beacon phase if no pedestrian signals
      else if (flashingBeaconNeeded) {
        nextPhase = cachedPhases.getPhase(alternatingFlash ?
            TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_1 :
            TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_2);
      }
      // Skip to yellow phase if flashing dont walk or flashing beacon not needed
      else {
        nextPhase = cachedPhases.getPhase(
            TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_YELLOW);
      }
    }
    // If currently in default green phase + flashing don't walk and time met, switch to next phase
    else if (originalPhase.getApplicability() ==
        TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN_FLASH_DW &&
        timeSinceLastPhaseApplicabilityChange >= (flashDontWalkTime - yellowTime)) {
      nextPhase = cachedPhases.getPhase(alternatingFlash ?
          TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_1 :
          TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_2);
    }
    // If currently in default green phase + flashing don't walk + flashing yellow hawk check
    // time or flash
    else if (originalPhase.getApplicability() ==
        TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK) {
      // If time met, switch to next phase
      if (timeSinceLastPhaseApplicabilityChange >= yellowTime) {
        nextPhase = cachedPhases.getPhase(
            TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_YELLOW);
      }
      // Otherwise, set proper flashing yellow hawk phase
      else {
        nextPhase = cachedPhases.getPhase(alternatingFlash ?
            TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_1 :
            TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_2);
      }
    }
    // If currently in default yellow phase, and time met, switch to next phase
    else if (originalPhase.getApplicability()
        == TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_YELLOW &&
        timeSinceLastPhaseApplicabilityChange >= yellowTime) {
      nextPhase = cachedPhases.getPhase(TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_RED);
    }
    // If currently in default red phase, and time met, switch to next phase
    else if (
        originalPhase.getApplicability() == TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_RED
            &&
            timeSinceLastPhaseApplicabilityChange >= allRedTime) {
      nextPhase = cachedPhases.getPhase(TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN);
    }
    // If currently in service green phase, and time met, check sensors and switch to next phase
    // if applicable
    else if (originalPhase.getApplicability()
        == TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN &&
        timeSinceLastPhaseApplicabilityChange >= minRequestableServiceTime) {

      // Switch to next phase if max time met
      if (timeSinceLastPhaseApplicabilityChange >= maxRequestableServiceTime) {
        nextPhase = cachedPhases.getPhase(
            TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW);
      } else {
        // Otherwise, check for sensor entity count
        int entityCount = 0;
        ArrayList<TrafficSignalControllerCircuit> circuitsList = circuits.getCircuits();
        for (int i = 1; i < circuitsList.size(); i++) {
          entityCount += circuitsList.get(i).getSensorsWaitingSummary(world).getStandardTotal();
        }

        // If request count is zero, switch to next phase
        if (entityCount == 0) {
          nextPhase = cachedPhases.getPhase(
              TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW);
        }
      }

      // If switch to next phase, reset request count
      if (nextPhase != null) {
        circuits.getCircuits()
            .forEach(value -> value.resetPedestrianAccessoriesRequestCount(world));
      }

    }
    // If currently in service green phase + flashing don't walk and time met, switch to next phase
    else if (originalPhase.getApplicability() ==
        TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN_FLASH_DW &&
        timeSinceLastPhaseApplicabilityChange >= (flashDontWalkTime - yellowTime)) {
      nextPhase = cachedPhases.getPhase(alternatingFlash ?
          TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_1 :
          TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_2);
    }
    // If currently in service green phase + flashing don't walk + flashing yellow hawk check
    // time or flash
    else if (originalPhase.getApplicability() ==
        TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK) {
      // If time met, switch to next phase
      if (timeSinceLastPhaseApplicabilityChange >= yellowTime) {
        nextPhase = cachedPhases.getPhase(
            TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_YELLOW);
      }
      // Otherwise, set proper flashing yellow hawk phase
      else {
        nextPhase = cachedPhases.getPhase(alternatingFlash ?
            TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_1 :
            TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_2);
      }
    }
    // If currently in service yellow phase, and time met, switch to next phase
    else if (originalPhase.getApplicability()
        == TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_YELLOW &&
        timeSinceLastPhaseApplicabilityChange >= yellowTime) {
      nextPhase = cachedPhases.getPhase(TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_SERVICE_RED);
    }
    // If currently in service red phase, and time met, switch to back default/start phase
    else if (
        originalPhase.getApplicability() == TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_RED
            &&
            timeSinceLastPhaseApplicabilityChange >= allRedTime) {
      nextPhase = cachedPhases.getPhase(TrafficSignalPhases.PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN);
    }

    // Return next phase (null phase indicates no change)
    return nextPhase;
  }

  /**
   * Handles the tick event for the traffic signal controller in
   * {@link TrafficSignalControllerMode#RAMP_METER_FULL_TIME} mode.
   *
   * @param world         The world in which the traffic signal controller is located.
   * @param circuits      The configured/connected circuits of the traffic signal controller.
   * @param cachedPhases  The programmed phases of the traffic signal controller.
   * @param originalPhase The original (current) phase of the traffic signal controller.
   *
   * @return The next phase to use for the traffic signal controller. If null is returned, then the
   *     phase is not changed.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase rampMeterFullTimeModeTick(World world,
      TrafficSignalControllerCircuits circuits,
      TrafficSignalPhases cachedPhases,
      TrafficSignalPhase originalPhase) {
    // Create variable to store next phase (null phase indicates no change)
    TrafficSignalPhase nextPhase = null;

    // If original phase is null, switch to all red phase
    if (originalPhase == null) {
      nextPhase = cachedPhases.getPhase(TrafficSignalPhases.PHASE_INDEX_ALL_RED);
    }
    // If currently in green ramp meter phase, switch to all red phase
    else if (originalPhase.getApplicability() == TrafficSignalPhaseApplicability.RAMP_METER_GREEN) {
      nextPhase = cachedPhases.getPhase(TrafficSignalPhases.PHASE_INDEX_ALL_RED);
    }
    // Otherwise, switch to green ramp meter phase
    else {
      nextPhase = new TrafficSignalPhase(TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
          TrafficSignalPhaseApplicability.RAMP_METER_GREEN);
      for (TrafficSignalControllerCircuit circuit : circuits.getCircuits()) {
        // Turn circuit vehicle signals green if there are vehicles waiting
        if (circuit.getSensorsWaitingSummary(world).getStandardTotal() > 0) {
          nextPhase.addOffSignals(circuit.getFlashingLeftSignals());
          nextPhase.addOffSignals(circuit.getFlashingRightSignals());
          nextPhase.addGreenSignals(circuit.getLeftSignals());
          nextPhase.addGreenSignals(circuit.getRightSignals());
          nextPhase.addGreenSignals(circuit.getThroughSignals());
          nextPhase.addOffSignals(circuit.getProtectedSignals());
          nextPhase.addYellowSignals(circuit.getBeaconSignals());
          nextPhase.addOffSignals(circuit.getPedestrianBeaconSignals());
          nextPhase.addDontWalkSignals(circuit.getPedestrianSignals());
          nextPhase.addDontWalkSignals(circuit.getPedestrianAccessorySignals());
        }
        // Keep circuit signals red if there are no vehicles waiting
        else {
          boolean pedestrianSignalsWalk = false;
          TrafficSignalControllerTickerUtilities.addCircuitToPhaseAllRed(circuit, nextPhase,
              pedestrianSignalsWalk);
        }
      }
    }

    // Beacons should flash continuously while the ramp meter is active
    nextPhase = overrideBeaconsToYellow(nextPhase, circuits);

    // Return next phase (null phase indicates no change)
    return nextPhase;
  }

  /**
   * Handles the tick event for the traffic signal controller in
   * {@link TrafficSignalControllerMode#RAMP_METER_PART_TIME} mode.
   *
   * @param world         The world in which the traffic signal controller is located.
   * @param circuits      The configured/connected circuits of the traffic signal controller.
   * @param cachedPhases  The programmed phases of the traffic signal controller.
   * @param originalPhase The original (current) phase of the traffic signal controller.
   *
   * @return The next phase to use for the traffic signal controller. If null is returned, then the
   *     phase is not changed.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase rampMeterPartTimeModeTick(World world,
      TrafficSignalControllerCircuits circuits,
      TrafficSignalPhases cachedPhases,
      TrafficSignalPhase originalPhase) {
    // Create variable to store next phase (null phase indicates no change)
    TrafficSignalPhase nextPhase = null;

    // Check if it is currently nighttime
    boolean isNight = !world.isDaytime();

    // If nighttime, operate using part time mode tick (metering disabled)
    if (isNight) {
      // If original phase is null or not set to meter disabled, switch to meter disabled phase
      if (originalPhase == null ||
          originalPhase.getApplicability() != TrafficSignalPhaseApplicability.RAMP_METER_DISABLED) {
        nextPhase = cachedPhases.getPhase(TrafficSignalPhases.PHASE_INDEX_RAMP_METER_DISABLED);
      }
    }
    // If not nighttime, operate using full time mode tick (metering enabled)
    else {
      // If original phase is not null and set to ramp meter disabled phase, set to ramp meter
      // starting phase
      if (originalPhase != null &&
          originalPhase.getApplicability() == TrafficSignalPhaseApplicability.RAMP_METER_DISABLED) {
        nextPhase = cachedPhases.getPhase(TrafficSignalPhases.PHASE_INDEX_RAMP_METER_STARTING);
      }
      // Otherwise pass directly to full time mode tick
      else {
        nextPhase = rampMeterFullTimeModeTick(world, circuits, cachedPhases, originalPhase);
      }
    }

    // Return next phase (null phase indicates no change)
    return nextPhase;
  }

  /**
   * Handles the tick event for the traffic signal controller in
   * {@link TrafficSignalControllerMode#NORMAL} mode.
   *
   * @param world                    The world in which the traffic signal controller is located.
   * @param circuits                 The configured/connected circuits of the traffic signal
   *                                 controller.
   * @param overlaps                 The configured overlaps of the traffic signal controller.
   * @param cachedPhases             The programmed phases of the traffic signal controller.
   * @param originalPhase            The original (current) phase of the traffic signal controller.
   * @param timeSinceLastPhaseChange The time since the traffic signal controller last changed
   *                                 phases.
   * @param overlapPedestrianSignals The overlap pedestrian signals setting of the traffic signal
   *                                 controller. This boolean value is used to determine if the
   *                                 pedestrian signals of all other circuits should be overlapped
   *                                 when servicing a circuit.
   * @param yellowTime               The yellow time for the traffic signal controller.
   * @param flashDontWalkTime        The flashing don't walk time for the traffic signal
   *                                 controller.
   * @param allRedTime               The all red time for the traffic signal controller.
   * @param minGreenTime             The minimum green time for the traffic signal controller when
   *                                 in normal mode.
   * @param maxGreenTime             The maximum green time for the traffic signal controller when
   *                                 in normal mode.
   * @param minGreenTimeSecondary    The secondary minimum green time for the traffic signal
   *                                 controller when in normal mode.
   * @param maxGreenTimeSecondary    The secondary maximum green time for the traffic signal
   *                                 controller when in normal mode.
   * @param dedicatedPedSignalTime       The dedicated pedestrian signal time for the traffic
   *                                     signal controller when in normal mode.
   * @param leadPedestrianIntervalTime   The lead pedestrian interval time for the traffic signal
   *                                     controller when in normal mode. A value of zero disables
   *                                     the lead pedestrian interval.
   *
   * @return The next phase to use for the traffic signal controller. If null is returned, then the
   *     phase is not changed.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase normalModeTick(World world,
      TrafficSignalControllerCircuits circuits,
      TrafficSignalControllerOverlaps overlaps,
      TrafficSignalPhases cachedPhases,
      TrafficSignalPhase originalPhase,
      long timeSinceLastPhaseChange,
      boolean overlapPedestrianSignals,
      long yellowTime,
      long flashDontWalkTime,
      long allRedTime,
      long minGreenTime,
      long maxGreenTime,
      long minGreenTimeSecondary,
      long maxGreenTimeSecondary,
      long dedicatedPedSignalTime,
      long leadPedestrianIntervalTime) {
    // Create variable to store next phase (null phase indicates no change)
    TrafficSignalPhase nextPhase = null;

    // If original phase is null, switch to all red phase
    if (originalPhase == null) {
      nextPhase = cachedPhases.getPhase(TrafficSignalPhases.PHASE_INDEX_ALL_RED);
    }
    // If original phase is all red, change to initial green phase (or LPI if applicable)
    else if (originalPhase.getApplicability() == TrafficSignalPhaseApplicability.ALL_RED &&
        timeSinceLastPhaseChange >= allRedTime) {
      // Change to initial green phase (on circuit 1)
      TrafficSignalPhase greenPhase =
          TrafficSignalControllerTickerUtilities.getDefaultPhaseForCircuitNumber(circuits,
              overlaps, 1,
              overlapPedestrianSignals, world);

      // Insert LPI phase if applicable (originating from ALL_RED, so no signals are green)
      nextPhase = TrafficSignalControllerTickerUtilities.maybeWrapWithLpi(originalPhase,
          greenPhase, overlapPedestrianSignals, leadPedestrianIntervalTime);
    }
    // If original phase is lead pedestrian interval, and LPI time is up, change to the upcoming
    // green phase
    else if (originalPhase.getApplicability()
        == TrafficSignalPhaseApplicability.LEAD_PEDESTRIAN_INTERVAL &&
        timeSinceLastPhaseChange >= leadPedestrianIntervalTime) {
      nextPhase = originalPhase.getUpcomingPhase();
    }
    // If original phase is flashing don't walk transitioning and FDW time is complete,
    // re-check demand before proceeding. At this point ped clearance has fully completed
    // but vehicle signals are still green — ideal checkpoint for ped recycle. If the
    // demand that triggered the transition is gone, recycle peds back to walk and resume
    // the current circuit's green phase with no vehicle signal disruption.
    else if (originalPhase.getApplicability()
        == TrafficSignalPhaseApplicability.FLASH_DONT_WALK_TRANSITIONING &&
        timeSinceLastPhaseChange >= flashDontWalkTime) {
      // Re-check demand at FDW completion
      boolean allSensored =
          TrafficSignalControllerTickerUtilities.allCircuitsHaveSensors(circuits);
      boolean recycleToGreen = false;

      if (allSensored) {
        int activeCircuit = originalPhase.getCircuit();
        Tuple<Integer, TrafficSignalPhaseApplicability> currentDemand =
            TrafficSignalControllerTickerUtilities.getUpcomingPhasePriorityIndicator(world,
                circuits, overlapPedestrianSignals);

        if (currentDemand == null) {
          // No demand anywhere — recycle peds to walk, stay green
          recycleToGreen = true;
        } else if (activeCircuit > 0 && currentDemand.getFirst() == activeCircuit) {
          // Demand is for the same circuit — only recycle if it's through-type demand.
          // Left turn or pedestrian demand on the same circuit requires its own phase,
          // so we must proceed to yellow transition for those.
          recycleToGreen = TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
              currentDemand.getSecond());
        }
      }

      if (recycleToGreen) {
        // Recycle: return to the active circuit's default phase (restores walk signals).
        // But first verify the rebuilt phase doesn't change any vehicle signals — if the
        // FYA/green-arrow decision has changed (e.g., left-turn vehicles cleared during
        // FDW), we must proceed to yellow transition to provide proper clearance.
        int activeCircuit = originalPhase.getCircuit();
        TrafficSignalPhase rebuiltPhase =
            TrafficSignalControllerTickerUtilities.getDefaultPhaseForCircuitNumber(
                circuits, overlaps, activeCircuit > 0 ? activeCircuit : 1,
                overlapPedestrianSignals, world);
        if (!TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(
            originalPhase, rebuiltPhase)) {
          nextPhase = rebuiltPhase;
        } else {
          // Vehicle signals changed (e.g., green arrow ↔ FYA flip). Transition to the
          // rebuilt phase for the SAME circuit so through signals that are green in both
          // phases stay green — only the signals that actually changed get clearance.
          nextPhase = TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              originalPhase, rebuiltPhase);
          recycleToGreen = false;
        }
      }
      if (!recycleToGreen) {
        // Demand still present on another circuit — proceed to yellow transition.
        // Only build the transition if the conflict path above didn't already set it.
        if (nextPhase == null) {
          nextPhase = TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              originalPhase,
              originalPhase.getUpcomingPhase());
        }
      }
    }
    // If original phase is yellow transitioning to red, and yellow time is up, change to red
    // transition phase
    else if (
        originalPhase.getApplicability() == TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING &&
            timeSinceLastPhaseChange >= yellowTime) {
      // Change to red transition phase
      nextPhase = TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
          originalPhase,
          originalPhase.getUpcomingPhase());
    }
    // If original phase is red transitioning to upcoming, and all red time is up, re-check
    // demand and change to the most appropriate green phase (or LPI if applicable).
    // This is the ped recycle point: if demand that triggered the transition is now gone
    // (e.g., vehicle turned right on red, pedestrian walked away), redirect to the
    // appropriate circuit instead of blindly going to the stored upcoming phase.
    else if (originalPhase.getApplicability() == TrafficSignalPhaseApplicability.RED_TRANSITIONING
        &&
        timeSinceLastPhaseChange >= allRedTime) {
      // Re-check current demand to decide which circuit to serve
      TrafficSignalPhase upcomingGreenPhase = originalPhase.getUpcomingPhase();
      int activeCircuit = originalPhase.getCircuit();
      boolean allSensored =
          TrafficSignalControllerTickerUtilities.allCircuitsHaveSensors(circuits);

      if (allSensored && upcomingGreenPhase != null) {
        Tuple<Integer, TrafficSignalPhaseApplicability> currentDemand =
            TrafficSignalControllerTickerUtilities.getUpcomingPhasePriorityIndicator(world,
                circuits, overlapPedestrianSignals);

        if (currentDemand == null) {
          // No demand anywhere — go back to the original circuit's default phase
          upcomingGreenPhase = TrafficSignalControllerTickerUtilities
              .getDefaultPhaseForCircuitNumber(circuits, overlaps,
                  activeCircuit > 0 ? activeCircuit : 1, overlapPedestrianSignals, world);
        } else if (activeCircuit > 0 && currentDemand.getFirst() == activeCircuit) {
          // Demand is for the original circuit — only redirect to default if it's
          // through-type demand. Left turn demand on the same circuit should proceed
          // to the stored upcoming phase (which is the left turn phase).
          if (TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
              currentDemand.getSecond())) {
            upcomingGreenPhase = TrafficSignalControllerTickerUtilities
                .getDefaultPhaseForCircuitNumber(circuits, overlaps, activeCircuit,
                    overlapPedestrianSignals, world);
          }
        }
        // If demand is still for another circuit, keep the stored upcoming phase
      }

      // Insert LPI phase if applicable (originating phase preserves signals that stay green)
      nextPhase = TrafficSignalControllerTickerUtilities.maybeWrapWithLpi(originalPhase,
          upcomingGreenPhase, overlapPedestrianSignals, leadPedestrianIntervalTime);

    }
    // If original phase is green, get corresponding min/max green times and check for phase change
    else if (originalPhase.getApplicability() != TrafficSignalPhaseApplicability.FLASH_DONT_WALK_TRANSITIONING
        && originalPhase.getApplicability() != TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING
        && originalPhase.getApplicability() != TrafficSignalPhaseApplicability.RED_TRANSITIONING
        && originalPhase.getApplicability() != TrafficSignalPhaseApplicability.LEAD_PEDESTRIAN_INTERVAL
        && originalPhase.getApplicability() != TrafficSignalPhaseApplicability.ALL_RED) {
      // Get corresponding min/max green times
      long phaseMinGreenTimeMs = minGreenTimeSecondary;
      long phaseMaxGreenTimeMs = maxGreenTimeSecondary;

      // Special case time for pedestrian signals
      if (originalPhase.getApplicability() == TrafficSignalPhaseApplicability.PEDESTRIAN) {
        phaseMinGreenTimeMs = dedicatedPedSignalTime;
        phaseMaxGreenTimeMs = dedicatedPedSignalTime;
      }
      // Special case time for primary circuit (1) default/throughs
      else if (originalPhase.getCircuit() == 1 &&
          (originalPhase.getApplicability() == TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS
              ||
              originalPhase.getApplicability() ==
                  TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTED_RIGHTS ||
              originalPhase.getApplicability() ==
                  TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS)) {
        phaseMinGreenTimeMs = minGreenTime;
        phaseMaxGreenTimeMs = maxGreenTime;
      }

      // If min green time is up, check for phase change
      if (timeSinceLastPhaseChange >= phaseMinGreenTimeMs) {
        // Check if max green time is met
        boolean maxGreenTimeMet = (timeSinceLastPhaseChange >= phaseMaxGreenTimeMs);

        // Get the next phase priority indicator (tells us what should be next based on traffic
        // counts)
        Tuple<Integer, TrafficSignalPhaseApplicability> currentPhasePriorityIndicator
            = originalPhase.getPriorityIndicator();
        Tuple<Integer, TrafficSignalPhaseApplicability> upcomingPhasePriorityIndicator
            = TrafficSignalControllerTickerUtilities.getUpcomingPhasePriorityIndicator(world,
            circuits,
            overlapPedestrianSignals);

        // Vehicle phase recall / max recall: when all circuits have sensors and no demand
        // exists on any other circuit, hold the current circuit's green phase indefinitely
        // — even past max green time. This prevents unnecessary red cycling when no one is
        // waiting, matching real-world actuated controller behavior. Pedestrians stay in
        // walk state since no FDW transition is triggered. As soon as conflicting demand
        // appears on another circuit, normal phase change logic resumes immediately.
        // Phase recall only applies when demand is for the same circuit AND the same type
        // of movement (through variants). Left turn demand on the same circuit should still
        // trigger a phase change to serve the left turn.
        boolean allSensored =
            TrafficSignalControllerTickerUtilities.allCircuitsHaveSensors(circuits);
        boolean phaseRecall = false;
        if (allSensored) {
          if (upcomingPhasePriorityIndicator == null) {
            // No demand anywhere — hold green (max recall)
            phaseRecall = true;
          } else if (originalPhase.getCircuit() > 0 &&
              upcomingPhasePriorityIndicator.getFirst() == originalPhase.getCircuit()) {
            // Demand is for the same circuit — recall only if the demand and current phase
            // are in the same movement category (e.g., both through-type, or both left turn).
            // This allows extended recall for any phase type while ensuring mismatched demand
            // (e.g., through demand during a left turn phase) triggers a phase change.
            phaseRecall = TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
                upcomingPhasePriorityIndicator.getSecond(),
                originalPhase.getApplicability());
          }
        }

        // Create variable to store upcoming phase
        TrafficSignalPhase upcomingPhase = null;

        // If phase recall is active, skip the phase change — hold current green
        if (!phaseRecall) {
          // If the current phase priority indicator is different than the upcoming phase priority
          // indicator, then we need to change phases
          if (upcomingPhasePriorityIndicator != null &&
              (currentPhasePriorityIndicator.getFirst().intValue() !=
                  upcomingPhasePriorityIndicator.getFirst().intValue() ||
                  currentPhasePriorityIndicator.getSecond() !=
                      upcomingPhasePriorityIndicator.getSecond())) {
            upcomingPhase =
                TrafficSignalControllerTickerUtilities.getUpcomingPhaseForPriorityIndicator(
                    world,
                    circuits,
                    overlaps,
                    upcomingPhasePriorityIndicator,
                    overlapPedestrianSignals);
          }
          // If max green time is met (and priority indicators are the same), then we need to
          // change phases to the next circuit default phase
          else if (maxGreenTimeMet) {
            // Get next circuit number
            int nextCircuitNumber = 1;
            if (originalPhase.getCircuit() >= 0
                && originalPhase.getCircuit() < circuits.getCircuitCount()) {
              nextCircuitNumber = originalPhase.getCircuit() + 1;
            }

            // Get next circuit default phase to set as upcoming phase
            upcomingPhase = TrafficSignalControllerTickerUtilities.getDefaultPhaseForCircuitNumber(
                circuits,
                overlaps,
                nextCircuitNumber,
                overlapPedestrianSignals, world);
          }
        }

        // Build next phase if an upcoming phase was generated
        if (upcomingPhase != null) {
          // Get next phase (flash don't walk transition unless null, otherwise yellow transition)
          nextPhase =
              TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
                  originalPhase, upcomingPhase);
          if (nextPhase == null) {
            nextPhase = TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
                originalPhase, upcomingPhase);
          }
        }

      }
    }

    // Apply overlaps to transition phases so overlap targets get yellow/red clearance
    // matching their source signals (green phases already have overlaps applied)
    if (nextPhase != null && (
        nextPhase.getApplicability() == TrafficSignalPhaseApplicability.FLASH_DONT_WALK_TRANSITIONING
            || nextPhase.getApplicability() == TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING
            || nextPhase.getApplicability() == TrafficSignalPhaseApplicability.RED_TRANSITIONING)) {
      TrafficSignalControllerTickerUtilities.getTransitionPhaseWithOverlapsApplied(
          nextPhase, overlaps);
    }

    // Beacon advance warning: override per-circuit beacons based on whether their
    // circuit's through signals are green in this phase. Beacons on circuits where
    // through signals are NOT green should flash yellow. This handles transition phases
    // where the generic phase builders may not preserve beacon state correctly.
    if (nextPhase != null) {
      boolean needsCopy = false;
      for (TrafficSignalControllerCircuit circuit : circuits.getCircuits()) {
        if (circuit.getBeaconSignals().isEmpty()) continue;
        boolean throughIsGreen = false;
        for (BlockPos tp : circuit.getThroughSignals()) {
          if (nextPhase.getGreenSignals().contains(tp)) {
            throughIsGreen = true;
            break;
          }
        }
        for (BlockPos bp : circuit.getBeaconSignals()) {
          boolean beaconShouldBeYellow = !throughIsGreen;
          boolean beaconIsYellow = nextPhase.getYellowSignals().contains(bp);
          boolean beaconIsOff = nextPhase.getOffSignals().contains(bp);
          if (beaconShouldBeYellow && !beaconIsYellow) { needsCopy = true; break; }
          if (!beaconShouldBeYellow && !beaconIsOff) { needsCopy = true; break; }
        }
        if (needsCopy) break;
      }
      if (needsCopy) {
        TrafficSignalPhase overridden = new TrafficSignalPhase(
            nextPhase.getCircuit(), nextPhase.getUpcomingPhase(), nextPhase.getApplicability());
        overridden.addOffSignals(nextPhase.getOffSignals());
        overridden.addGreenSignals(nextPhase.getGreenSignals());
        overridden.addYellowSignals(nextPhase.getYellowSignals());
        overridden.addRedSignals(nextPhase.getRedSignals());
        overridden.addFyaSignals(nextPhase.getFyaSignals());
        overridden.addWalkSignals(nextPhase.getWalkSignals());
        overridden.addDontWalkSignals(nextPhase.getDontWalkSignals());
        overridden.addFlashDontWalkSignals(nextPhase.getFlashDontWalkSignals());
        for (TrafficSignalControllerCircuit circuit : circuits.getCircuits()) {
          if (circuit.getBeaconSignals().isEmpty()) continue;
          boolean throughIsGreen = false;
          for (BlockPos tp : circuit.getThroughSignals()) {
            if (nextPhase.getGreenSignals().contains(tp)) {
              throughIsGreen = true;
              break;
            }
          }
          overridden.removeSignals(circuit.getBeaconSignals());
          if (throughIsGreen) {
            overridden.addOffSignals(circuit.getBeaconSignals());
          } else {
            overridden.addYellowSignals(circuit.getBeaconSignals());
          }
        }
        nextPhase = overridden;
      }
    }

    // Return next phase (null phase indicates no change)
    return nextPhase;
  }

  /**
   * Handles the tick event for the traffic signal controller in
   * {@link TrafficSignalControllerMode#FLASH} mode.
   *
   * @param configuredMode   The configured mode of the traffic signal controller.
   * @param cachedPhases     The programmed phases of the traffic signal controller.
   * @param alternatingFlash The alternating flash state of the traffic signal controller. This
   *                         boolean value alternates between true and false each tick and is used
   *                         to control the flashing of traffic signal devices.
   *
   * @return The next phase to use for the traffic signal controller. If null is returned, then the
   *     phase is not changed.
   *
   * @implNote This method always returns the {@link TrafficSignalPhase} from
   *     {@code cachedPhases} index 0 or 1, depending on the value of {@code alternatingFlash}.
   * @since 1.0
   */
  public static TrafficSignalPhase flashModeTick(TrafficSignalControllerMode configuredMode,
      TrafficSignalPhases cachedPhases,
      boolean alternatingFlash,
      boolean allRedFlash) {
    TrafficSignalPhase flashPhase;

    // If configured mode is ramp meter, return ramp meter specific alternating flash phase from
    // cached phases
    if (configuredMode == TrafficSignalControllerMode.RAMP_METER_FULL_TIME ||
        configuredMode == TrafficSignalControllerMode.RAMP_METER_PART_TIME) {
      flashPhase = cachedPhases.getPhase(alternatingFlash ?
          TrafficSignalPhases.PHASE_INDEX_RAMP_METER_FLASH_1 :
          TrafficSignalPhases.PHASE_INDEX_RAMP_METER_FLASH_2);
    }
    // If all red flash is enabled, use fault phases (all-red alternating) instead of standard
    // yellow/red flash
    else if (allRedFlash) {
      flashPhase = cachedPhases.getPhase(alternatingFlash ?
          TrafficSignalPhases.PHASE_INDEX_FAULT_1 :
          TrafficSignalPhases.PHASE_INDEX_FAULT_2);
    }
    // Otherwise, return standard alternating flash phase from cached phases
    else {
      flashPhase = cachedPhases.getPhase(alternatingFlash ?
          TrafficSignalPhases.PHASE_INDEX_FLASH_1 :
          TrafficSignalPhases.PHASE_INDEX_FLASH_2);
    }

    return flashPhase;
  }

  /**
   * The hold time in ticks for wrong way detection beacons after the last approach detection.
   * 30 seconds = 600 ticks.
   *
   * @since 1.0
   */
  public static final long WWVDS_BEACON_HOLD_TIME = 600L;

  /**
   * The minimum cumulative approach distance (in blocks) an entity must travel toward a sensor
   * before it is considered a confirmed wrong-way approach. This prevents brief pass-throughs
   * (e.g. flying past the zone) from triggering the beacons.
   *
   * @since 1.0
   */
  public static final double WWVDS_APPROACH_THRESHOLD = 3.0;

  /**
   * Handles the tick event for the traffic signal controller in
   * {@link TrafficSignalControllerMode#WRONG_WAY_DETECTION} mode. This mode polls linked sensors
   * rapidly and tracks entity movement to detect wrong-way approaches. When an entity in a sensor's
   * main detection zone is moving closer to the sensor block, it is considered a wrong-way
   * approach. Each circuit operates independently — beacons on a circuit are activated only when
   * that circuit's sensors detect an approach. Multiple sensors per circuit are supported (e.g. for
   * curved roads with multiple detection layers), and a detection on any sensor in the circuit
   * triggers that circuit's beacons. Beacons flash yellow for {@link #WWVDS_BEACON_HOLD_TIME}
   * ticks after the last detection before turning off.
   *
   * @param world                    The world in which the traffic signal controller is located.
   * @param circuits                 The configured/connected circuits of the traffic signal
   *                                 controller.
   * @param entityDistances          Mutable map of circuit index to (entity ID to last known
   *                                 distance to nearest sensor). Updated in place each tick.
   * @param entityApproachTotals     Mutable map of circuit index to (entity ID to cumulative
   *                                 approach distance in blocks). An entity must accumulate at
   *                                 least {@link #WWVDS_APPROACH_THRESHOLD} blocks of approach
   *                                 before triggering. Cleared for an entity when it leaves the
   *                                 zone or moves away.
   * @param circuitHoldTimers        Mutable map of circuit index to the world time of the last
   *                                 wrong-way detection. Updated in place when an approach is
   *                                 detected.
   * @param worldTime                The current total world time in ticks.
   *
   * @return The next phase to apply. Always returns a non-null phase.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase wrongWayDetectionModeTick(
      World world,
      TrafficSignalControllerCircuits circuits,
      Map<Integer, Map<Integer, Double>> entityDistances,
      Map<Integer, Map<Integer, Double>> entityApproachTotals,
      Map<Integer, Long> circuitHoldTimers,
      long worldTime) {

    // First pass: scan all circuits, track entity distances, determine which circuits are active
    boolean anyCircuitActive = false;
    boolean[] circuitActive = new boolean[circuits.getCircuitCount()];

    for (int i = 0; i < circuits.getCircuitCount(); i++) {
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(i);
      boolean approachDetected = false;

      // Get the previous distance tracking map and cumulative approach totals for this circuit
      Map<Integer, Double> prevDistances =
          entityDistances.computeIfAbsent(i, k -> new HashMap<>());
      Map<Integer, Double> prevApproachTotals =
          entityApproachTotals.computeIfAbsent(i, k -> new HashMap<>());
      Map<Integer, Double> newDistances = new HashMap<>();
      Map<Integer, Double> newApproachTotals = new HashMap<>();

      // Scan ALL sensors in this circuit (supports multiple sensors for curved roads, etc.)
      for (BlockPos sensorPos : circuit.getSensors()) {
        TileEntity te = world.getTileEntity(sensorPos);
        if (te instanceof TileEntityTrafficSignalSensor) {
          TileEntityTrafficSignalSensor sensor = (TileEntityTrafficSignalSensor) te;
          List<Tuple<Integer, Vec3d>> entities = sensor.scanEntitiesWithPositions();

          Vec3d sensorVec = new Vec3d(
              sensorPos.getX() + 0.5, sensorPos.getY() + 0.5, sensorPos.getZ() + 0.5);

          for (Tuple<Integer, Vec3d> entityData : entities) {
            int entityId = entityData.getFirst();
            Vec3d entityPos = entityData.getSecond();
            double currentDistance = entityPos.distanceTo(sensorVec);

            // For entities seen by multiple sensors, keep the minimum distance
            // (closest sensor is the most relevant reference point)
            Double existingNew = newDistances.get(entityId);
            if (existingNew == null || currentDistance < existingNew) {
              newDistances.put(entityId, currentDistance);
            }

            // Track cumulative approach distance per entity
            Double previousDistance = prevDistances.get(entityId);
            if (previousDistance != null) {
              double delta = previousDistance - currentDistance;
              if (delta > 0) {
                // Entity moved closer — accumulate approach distance
                double prevTotal = prevApproachTotals.getOrDefault(entityId, 0.0);
                double newTotal = prevTotal + delta;
                newApproachTotals.put(entityId, newTotal);

                // Only trigger once cumulative approach exceeds threshold
                if (newTotal >= WWVDS_APPROACH_THRESHOLD) {
                  approachDetected = true;
                }
              }
              // Entity moved away or stayed — reset its cumulative approach
            }
          }
        }
      }

      // Update the distance and approach total maps for next tick
      // (entities that left the zone or moved away are naturally pruned by not being in newMaps)
      entityDistances.put(i, newDistances);
      entityApproachTotals.put(i, newApproachTotals);

      // Update hold timer if approach was detected
      if (approachDetected) {
        circuitHoldTimers.put(i, worldTime);
      }

      // Check if this circuit is still within the hold period
      Long lastDetectionTime = circuitHoldTimers.get(i);
      circuitActive[i] =
          lastDetectionTime != null && (worldTime - lastDetectionTime) < WWVDS_BEACON_HOLD_TIME;
      if (circuitActive[i]) {
        anyCircuitActive = true;
      }
    }

    // Second pass: build the phase based on detection results
    TrafficSignalPhaseApplicability applicability = anyCircuitActive
        ? TrafficSignalPhaseApplicability.WRONG_WAY_ACTIVE
        : TrafficSignalPhaseApplicability.WRONG_WAY_IDLE;

    TrafficSignalPhase nextPhase = new TrafficSignalPhase(
        TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null, applicability);

    for (int i = 0; i < circuits.getCircuitCount(); i++) {
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(i);

      if (circuitActive[i]) {
        // Beacons stay on (yellow) — the beacon renderer handles flash logic internally
        nextPhase.addYellowSignals(circuit.getBeaconSignals());
      } else {
        // Beacons off when no active detection on this circuit
        nextPhase.addOffSignals(circuit.getBeaconSignals());
      }

      // All non-beacon signals are turned off in WWVDS mode
      nextPhase.addOffSignals(circuit.getThroughSignals());
      nextPhase.addOffSignals(circuit.getLeftSignals());
      nextPhase.addOffSignals(circuit.getRightSignals());
      nextPhase.addOffSignals(circuit.getFlashingLeftSignals());
      nextPhase.addOffSignals(circuit.getFlashingRightSignals());
      nextPhase.addOffSignals(circuit.getProtectedSignals());
      nextPhase.addOffSignals(circuit.getPedestrianSignals());
      nextPhase.addOffSignals(circuit.getPedestrianBeaconSignals());
      nextPhase.addOffSignals(circuit.getPedestrianAccessorySignals());
    }

    return nextPhase;
  }

  /**
   * The hold time in ticks for overheight detection beacons after the last detection.
   * 30 seconds = 600 ticks.
   *
   * @since 1.0
   */
  public static final long OVERHEIGHT_BEACON_HOLD_TIME = 600L;

  /**
   * Handles the tick event for the traffic signal controller in
   * {@link TrafficSignalControllerMode#OVERHEIGHT_DETECTION} mode. This mode polls linked
   * overheight detection sensors and activates beacons on circuits where an overheight entity is
   * detected in the sensor pair's detection zone. Each circuit operates independently. Multiple
   * sensor pairs per circuit are supported. Beacons stay on (yellow) for
   * {@link #OVERHEIGHT_BEACON_HOLD_TIME} ticks after the last detection. Entity detection is
   * mod-agnostic — any entity with a bounding box height exceeding
   * {@link TileEntityOverheightDetectionSensor#MIN_ENTITY_HEIGHT} is detected, including modded
   * vehicles from Immersive Vehicles/MTS.
   *
   * @param world             The world in which the traffic signal controller is located.
   * @param circuits          The configured/connected circuits of the traffic signal controller.
   * @param circuitHoldTimers Mutable map of circuit index to the world time of the last overheight
   *                          detection. Updated in place when a detection occurs.
   * @param worldTime         The current total world time in ticks.
   *
   * @return The next phase to apply. Always returns a non-null phase.
   *
   * @since 1.0
   */
  public static TrafficSignalPhase overheightDetectionModeTick(
      World world,
      TrafficSignalControllerCircuits circuits,
      Map<Integer, Long> circuitHoldTimers,
      long worldTime) {

    // First pass: scan all circuits for overheight detections
    boolean anyCircuitActive = false;
    boolean[] circuitActive = new boolean[circuits.getCircuitCount()];

    for (int i = 0; i < circuits.getCircuitCount(); i++) {
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(i);
      boolean detected = false;

      // Scan ALL sensors in this circuit (supports multiple sensor pairs per circuit)
      for (BlockPos sensorPos : circuit.getSensors()) {
        TileEntity te = world.getTileEntity(sensorPos);
        if (te instanceof TileEntityOverheightDetectionSensor) {
          TileEntityOverheightDetectionSensor sensor =
              (TileEntityOverheightDetectionSensor) te;
          if (sensor.isPaired() && sensor.scanForOverheightEntities() > 0) {
            detected = true;
            break;
          }
        }
      }

      // Update hold timer if detection occurred
      if (detected) {
        circuitHoldTimers.put(i, worldTime);
      }

      // Check if this circuit is still within the hold period
      Long lastDetectionTime = circuitHoldTimers.get(i);
      circuitActive[i] =
          lastDetectionTime != null && (worldTime - lastDetectionTime) < OVERHEIGHT_BEACON_HOLD_TIME;
      if (circuitActive[i]) {
        anyCircuitActive = true;
      }
    }

    // Second pass: build the phase based on detection results
    TrafficSignalPhaseApplicability applicability = anyCircuitActive
        ? TrafficSignalPhaseApplicability.OVERHEIGHT_ACTIVE
        : TrafficSignalPhaseApplicability.OVERHEIGHT_IDLE;

    TrafficSignalPhase nextPhase = new TrafficSignalPhase(
        TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null, applicability);

    for (int i = 0; i < circuits.getCircuitCount(); i++) {
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(i);

      if (circuitActive[i]) {
        // Beacons stay on (yellow) — the beacon renderer handles flash logic internally
        nextPhase.addYellowSignals(circuit.getBeaconSignals());
      } else {
        // Beacons off when no active detection on this circuit
        nextPhase.addOffSignals(circuit.getBeaconSignals());
      }

      // All non-beacon signals are turned off in overheight detection mode
      nextPhase.addOffSignals(circuit.getThroughSignals());
      nextPhase.addOffSignals(circuit.getLeftSignals());
      nextPhase.addOffSignals(circuit.getRightSignals());
      nextPhase.addOffSignals(circuit.getFlashingLeftSignals());
      nextPhase.addOffSignals(circuit.getFlashingRightSignals());
      nextPhase.addOffSignals(circuit.getProtectedSignals());
      nextPhase.addOffSignals(circuit.getPedestrianSignals());
      nextPhase.addOffSignals(circuit.getPedestrianBeaconSignals());
      nextPhase.addOffSignals(circuit.getPedestrianAccessorySignals());
    }

    return nextPhase;
  }

  /**
   * Returns a copy of the given phase with all beacon signals overridden to yellow.
   * If all beacons are already yellow, returns the original phase unchanged (avoids
   * unnecessary copy). Creates a fresh phase to avoid mutating cached phases.
   */
  private static TrafficSignalPhase overrideBeaconsToYellow(
      TrafficSignalPhase phase, TrafficSignalControllerCircuits circuits) {
    if (phase == null) return null;
    boolean needsOverride = false;
    for (TrafficSignalControllerCircuit circuit : circuits.getCircuits()) {
      for (BlockPos bp : circuit.getBeaconSignals()) {
        if (!phase.getYellowSignals().contains(bp)) {
          needsOverride = true;
          break;
        }
      }
      if (needsOverride) break;
    }
    if (!needsOverride) return phase;

    TrafficSignalPhase overridden = new TrafficSignalPhase(
        phase.getCircuit(), phase.getUpcomingPhase(), phase.getApplicability());
    overridden.addOffSignals(phase.getOffSignals());
    overridden.addGreenSignals(phase.getGreenSignals());
    overridden.addYellowSignals(phase.getYellowSignals());
    overridden.addRedSignals(phase.getRedSignals());
    overridden.addFyaSignals(phase.getFyaSignals());
    overridden.addWalkSignals(phase.getWalkSignals());
    overridden.addDontWalkSignals(phase.getDontWalkSignals());
    overridden.addFlashDontWalkSignals(phase.getFlashDontWalkSignals());
    for (TrafficSignalControllerCircuit circuit : circuits.getCircuits()) {
      overridden.removeSignals(circuit.getBeaconSignals());
      overridden.addYellowSignals(circuit.getBeaconSignals());
    }
    return overridden;
  }
}
