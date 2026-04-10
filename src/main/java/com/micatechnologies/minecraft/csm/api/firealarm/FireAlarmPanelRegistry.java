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

  public static synchronized void registerFireAlarm(int dimensionId, BlockPos panelPos) {
    fireAlarmPanels.computeIfAbsent(dimensionId, k -> new HashSet<>()).add(panelPos);
  }

  public static synchronized void unregisterFireAlarm(int dimensionId, BlockPos panelPos) {
    Set<BlockPos> panels = fireAlarmPanels.get(dimensionId);
    if (panels != null) {
      panels.remove(panelPos);
      if (panels.isEmpty()) {
        fireAlarmPanels.remove(dimensionId);
      }
    }
  }

  public static synchronized void registerStormAlarm(int dimensionId, BlockPos panelPos) {
    stormAlarmPanels.computeIfAbsent(dimensionId, k -> new HashSet<>()).add(panelPos);
  }

  public static synchronized void unregisterStormAlarm(int dimensionId, BlockPos panelPos) {
    Set<BlockPos> panels = stormAlarmPanels.get(dimensionId);
    if (panels != null) {
      panels.remove(panelPos);
      if (panels.isEmpty()) {
        stormAlarmPanels.remove(dimensionId);
      }
    }
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
   * Removes a panel from all registries (fire + storm). Called when a panel TE is invalidated
   * or its chunk is unloaded.
   */
  public static synchronized void unregisterAll(int dimensionId, BlockPos panelPos) {
    unregisterFireAlarm(dimensionId, panelPos);
    unregisterStormAlarm(dimensionId, panelPos);
  }
}
