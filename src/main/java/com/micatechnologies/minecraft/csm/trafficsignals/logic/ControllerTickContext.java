package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.world.World;

/**
 * Data class encapsulating all parameters needed by
 * {@link TrafficSignalControllerTicker#tick(ControllerTickContext)} to process a single tick of the
 * traffic signal controller. This replaces the previous 26-parameter method signature with a single
 * structured context object.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2026.4.0
 */
public class ControllerTickContext {

  private final World world;
  private final TrafficSignalControllerMode configuredMode;
  private final TrafficSignalControllerMode operatingMode;
  private final TrafficSignalControllerCircuits circuits;
  private final TrafficSignalControllerOverlaps overlaps;
  private final TrafficSignalPhases cachedPhases;
  private final TrafficSignalPhase originalPhase;
  private final long timeSinceLastPhaseApplicabilityChange;
  private final long timeSinceLastPhaseChange;
  private final boolean alternatingFlash;
  private final boolean overlapPedestrianSignals;
  private final long yellowTime;
  private final long flashDontWalkTime;
  private final long allRedTime;
  private final long minRequestableServiceTime;
  private final long maxRequestableServiceTime;
  private final long minGreenTime;
  private final long maxGreenTime;
  private final long minGreenTimeSecondary;
  private final long maxGreenTimeSecondary;
  private final long dedicatedPedSignalTime;
  private final long leadPedestrianIntervalTime;
  private final boolean allRedFlash;

  /**
   * Constructs a new {@link ControllerTickContext} with all required parameters.
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
   *                                              controller.
   * @param overlapPedestrianSignals              Whether pedestrian signals of all other circuits
   *                                              should be overlapped when servicing a circuit.
   * @param yellowTime                            The yellow time for the traffic signal controller.
   * @param flashDontWalkTime                     The flashing don't walk time for the traffic
   *                                              signal controller.
   * @param allRedTime                            The all red time for the traffic signal
   *                                              controller.
   * @param minRequestableServiceTime             The minimum service time when in requestable mode.
   * @param maxRequestableServiceTime             The maximum service time when in requestable mode.
   * @param minGreenTime                          The minimum green time when in normal mode.
   * @param maxGreenTime                          The maximum green time when in normal mode.
   * @param minGreenTimeSecondary                 The secondary minimum green time when in normal
   *                                              mode.
   * @param maxGreenTimeSecondary                 The secondary maximum green time when in normal
   *                                              mode.
   * @param dedicatedPedSignalTime                The dedicated pedestrian signal time when in
   *                                              normal mode.
   * @param leadPedestrianIntervalTime            The lead pedestrian interval time when in normal
   *                                              mode. A value of zero disables the lead pedestrian
   *                                              interval.
   * @param allRedFlash                           Whether to use all-red flashing instead of
   *                                              yellow/red flashing when in flash mode.
   */
  public ControllerTickContext(World world,
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
    this.world = world;
    this.configuredMode = configuredMode;
    this.operatingMode = operatingMode;
    this.circuits = circuits;
    this.overlaps = overlaps;
    this.cachedPhases = cachedPhases;
    this.originalPhase = originalPhase;
    this.timeSinceLastPhaseApplicabilityChange = timeSinceLastPhaseApplicabilityChange;
    this.timeSinceLastPhaseChange = timeSinceLastPhaseChange;
    this.alternatingFlash = alternatingFlash;
    this.overlapPedestrianSignals = overlapPedestrianSignals;
    this.yellowTime = yellowTime;
    this.flashDontWalkTime = flashDontWalkTime;
    this.allRedTime = allRedTime;
    this.minRequestableServiceTime = minRequestableServiceTime;
    this.maxRequestableServiceTime = maxRequestableServiceTime;
    this.minGreenTime = minGreenTime;
    this.maxGreenTime = maxGreenTime;
    this.minGreenTimeSecondary = minGreenTimeSecondary;
    this.maxGreenTimeSecondary = maxGreenTimeSecondary;
    this.dedicatedPedSignalTime = dedicatedPedSignalTime;
    this.leadPedestrianIntervalTime = leadPedestrianIntervalTime;
    this.allRedFlash = allRedFlash;
  }

  public World getWorld() {
    return world;
  }

  public TrafficSignalControllerMode getConfiguredMode() {
    return configuredMode;
  }

  public TrafficSignalControllerMode getOperatingMode() {
    return operatingMode;
  }

  public TrafficSignalControllerCircuits getCircuits() {
    return circuits;
  }

  public TrafficSignalControllerOverlaps getOverlaps() {
    return overlaps;
  }

  public TrafficSignalPhases getCachedPhases() {
    return cachedPhases;
  }

  public TrafficSignalPhase getOriginalPhase() {
    return originalPhase;
  }

  public long getTimeSinceLastPhaseApplicabilityChange() {
    return timeSinceLastPhaseApplicabilityChange;
  }

  public long getTimeSinceLastPhaseChange() {
    return timeSinceLastPhaseChange;
  }

  public boolean isAlternatingFlash() {
    return alternatingFlash;
  }

  public boolean isOverlapPedestrianSignals() {
    return overlapPedestrianSignals;
  }

  public long getYellowTime() {
    return yellowTime;
  }

  public long getFlashDontWalkTime() {
    return flashDontWalkTime;
  }

  public long getAllRedTime() {
    return allRedTime;
  }

  public long getMinRequestableServiceTime() {
    return minRequestableServiceTime;
  }

  public long getMaxRequestableServiceTime() {
    return maxRequestableServiceTime;
  }

  public long getMinGreenTime() {
    return minGreenTime;
  }

  public long getMaxGreenTime() {
    return maxGreenTime;
  }

  public long getMinGreenTimeSecondary() {
    return minGreenTimeSecondary;
  }

  public long getMaxGreenTimeSecondary() {
    return maxGreenTimeSecondary;
  }

  public long getDedicatedPedSignalTime() {
    return dedicatedPedSignalTime;
  }

  public long getLeadPedestrianIntervalTime() {
    return leadPedestrianIntervalTime;
  }

  public boolean isAllRedFlash() {
    return allRedFlash;
  }
}
