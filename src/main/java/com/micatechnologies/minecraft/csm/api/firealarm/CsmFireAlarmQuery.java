package com.micatechnologies.minecraft.csm.api.firealarm;

import com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmControlPanel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Public query API for the CSM fire alarm system. External mods can use these static methods to
 * check fire alarm state without subscribing to events. This is useful for entities that spawn
 * mid-alarm and need to check current state.
 * <p>
 * All methods are safe to call from the server thread. They read from the
 * {@link FireAlarmPanelRegistry} and world tile entities.
 */
public class CsmFireAlarmQuery {

  /**
   * Maximum hearing range in blocks (voice evac volume 3.0 * 16 = 48 blocks).
   */
  public static final double MAX_HEARING_RANGE = 48.0;

  /**
   * Horn hearing range in blocks (sounder volume 2.0 * 16 = 32 blocks).
   */
  public static final double HORN_HEARING_RANGE = 32.0;

  /**
   * Checks whether any fire alarm sounder is active within hearing range of the given position.
   * This checks connected appliance positions, not just panel positions.
   *
   * @param world the world to check in
   * @param pos   the position to check from (e.g., an entity's block position)
   *
   * @return true if a fire alarm is sounding within range
   */
  public static boolean isFireAlarmActiveNear(World world, BlockPos pos) {
    return isAlarmActiveNear(world, pos,
        FireAlarmPanelRegistry.getActiveFireAlarmPanels(world.provider.getDimension()));
  }

  /**
   * Checks whether any storm alarm sounder is active within hearing range of the given position.
   *
   * @param world the world to check in
   * @param pos   the position to check from
   *
   * @return true if a storm alarm is sounding within range
   */
  public static boolean isStormAlarmActiveNear(World world, BlockPos pos) {
    return isAlarmActiveNear(world, pos,
        FireAlarmPanelRegistry.getActiveStormAlarmPanels(world.provider.getDimension()));
  }

  /**
   * Returns the positions of all active fire alarm control panels in the given world.
   */
  public static Set<BlockPos> getActiveFireAlarmPanels(World world) {
    return FireAlarmPanelRegistry.getActiveFireAlarmPanels(world.provider.getDimension());
  }

  /**
   * Returns the positions of all active storm alarm control panels in the given world.
   */
  public static Set<BlockPos> getActiveStormAlarmPanels(World world) {
    return FireAlarmPanelRegistry.getActiveStormAlarmPanels(world.provider.getDimension());
  }

  /**
   * Returns the connected sounder/appliance positions for a fire alarm panel at the given
   * position. Returns an empty list if the position does not contain a fire alarm panel.
   */
  public static List<BlockPos> getSounderPositions(World world, BlockPos panelPos) {
    if (!world.isBlockLoaded(panelPos)) {
      return Collections.emptyList();
    }
    TileEntity te = world.getTileEntity(panelPos);
    if (te instanceof TileEntityFireAlarmControlPanel) {
      return ((TileEntityFireAlarmControlPanel) te).getConnectedAppliances();
    }
    return Collections.emptyList();
  }

  private static boolean isAlarmActiveNear(World world, BlockPos pos, Set<BlockPos> panelPositions) {
    if (panelPositions.isEmpty()) {
      return false;
    }

    double maxRangeSq = MAX_HEARING_RANGE * MAX_HEARING_RANGE;
    // Quick reject: skip panels whose own position is very far away (panel + max appliance
    // distance + hearing range). Use a generous cutoff to avoid false negatives.
    double panelSkipDistSq = (MAX_HEARING_RANGE + 200.0) * (MAX_HEARING_RANGE + 200.0);

    for (BlockPos panelPos : panelPositions) {
      if (pos.distanceSq(panelPos) > panelSkipDistSq) {
        continue;
      }
      if (!world.isBlockLoaded(panelPos)) {
        continue;
      }

      TileEntity te = world.getTileEntity(panelPos);
      if (!(te instanceof TileEntityFireAlarmControlPanel)) {
        continue;
      }

      List<BlockPos> appliances = ((TileEntityFireAlarmControlPanel) te).getConnectedAppliances();
      for (BlockPos appliance : appliances) {
        if (pos.distanceSq(appliance) <= maxRangeSq) {
          return true;
        }
      }
    }
    return false;
  }
}
