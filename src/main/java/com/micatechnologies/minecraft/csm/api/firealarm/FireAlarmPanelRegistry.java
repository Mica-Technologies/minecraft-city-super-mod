package com.micatechnologies.minecraft.csm.api.firealarm;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.BlockPos;

/**
 * Tracks which fire alarm control panels currently have active fire or storm alarms, keyed by
 * dimension ID. This avoids the need to scan all tile entities when querying alarm state.
 * <p>
 * This registry is maintained by {@code TileEntityFireAlarmControlPanel} and queried by
 * {@link CsmFireAlarmQuery}. External mods should use {@link CsmFireAlarmQuery} rather than
 * accessing this registry directly.
 */
public class FireAlarmPanelRegistry {

  private static final Map<Integer, Set<BlockPos>> fireAlarmPanels = new HashMap<>();
  private static final Map<Integer, Set<BlockPos>> stormAlarmPanels = new HashMap<>();

  /**
   * @return true if the panel was not already listed, i.e. this call changed what a query sees
   */
  public static synchronized boolean registerFireAlarm(int dimensionId, BlockPos panelPos) {
    return fireAlarmPanels.computeIfAbsent(dimensionId, k -> new HashSet<>()).add(panelPos);
  }

  /**
   * @return true if the panel was listed, i.e. this call changed what a query sees
   */
  public static synchronized boolean unregisterFireAlarm(int dimensionId, BlockPos panelPos) {
    return remove(fireAlarmPanels, dimensionId, panelPos);
  }

  /**
   * @return true if the panel was not already listed, i.e. this call changed what a query sees
   */
  public static synchronized boolean registerStormAlarm(int dimensionId, BlockPos panelPos) {
    return stormAlarmPanels.computeIfAbsent(dimensionId, k -> new HashSet<>()).add(panelPos);
  }

  /**
   * @return true if the panel was listed, i.e. this call changed what a query sees
   */
  public static synchronized boolean unregisterStormAlarm(int dimensionId, BlockPos panelPos) {
    return remove(stormAlarmPanels, dimensionId, panelPos);
  }

  private static boolean remove(Map<Integer, Set<BlockPos>> registry, int dimensionId,
      BlockPos panelPos) {
    Set<BlockPos> panels = registry.get(dimensionId);
    if (panels == null) {
      return false;
    }
    boolean removed = panels.remove(panelPos);
    if (panels.isEmpty()) {
      registry.remove(dimensionId);
    }
    return removed;
  }

  /**
   * Returns an unmodifiable snapshot of panels with active fire alarms in the given dimension.
   */
  public static synchronized Set<BlockPos> getActiveFireAlarmPanels(int dimensionId) {
    Set<BlockPos> panels = fireAlarmPanels.get(dimensionId);
    if (panels == null || panels.isEmpty()) {
      return Collections.emptySet();
    }
    return new HashSet<>(panels);
  }

  /**
   * Returns an unmodifiable snapshot of panels with active storm alarms in the given dimension.
   */
  public static synchronized Set<BlockPos> getActiveStormAlarmPanels(int dimensionId) {
    Set<BlockPos> panels = stormAlarmPanels.get(dimensionId);
    if (panels == null || panels.isEmpty()) {
      return Collections.emptySet();
    }
    return new HashSet<>(panels);
  }

  /**
   * Removes a panel from all registries (fire + storm).
   * <p>
   * Prefer the individual {@code unregister} methods where the caller has to post the API events
   * that go with leaving the registry, since those report which of the two actually changed. This
   * one drops that answer, and a caller using it cannot keep {@link FireAlarmEvent} in step with
   * what {@link CsmFireAlarmQuery} reports -- which is precisely how a panel used to leave the
   * registry on chunk unload with no matching {@code Deactivated} behind it.
   */
  public static synchronized void unregisterAll(int dimensionId, BlockPos panelPos) {
    unregisterFireAlarm(dimensionId, panelPos);
    unregisterStormAlarm(dimensionId, panelPos);
  }
}
