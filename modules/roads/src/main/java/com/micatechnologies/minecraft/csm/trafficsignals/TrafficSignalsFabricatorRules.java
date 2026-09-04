package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensorHZEight;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * The Fabricator cost rule for the Traffic Signals tab, registered with
 * {@link CsmFabricatorCosts} from {@code CsmRoads}' pre-initialization.
 *
 * <p>The rule lives here rather than in Core because it is decided from this subsystem's own
 * class hierarchy. With this module absent, its blocks are absent too, and any block that somehow
 * reached the tab would take Core's generic equipment cost — the same cost this rule ends on.
 *
 * @see CsmFabricatorCosts
 * @since 2026.9
 */
public final class TrafficSignalsFabricatorRules {

  /**
   * The creative tab this rule prices. Must match {@code CsmTabTrafficSignals.getTabId()}.
   *
   * @since 2026.9
   */
  public static final String TAB_ID = "tabtrafficsignals";

  /**
   * Private constructor: this class is a static rule holder and is never instantiated.
   *
   * @since 2026.9
   */
  private TrafficSignalsFabricatorRules() {
    throw new UnsupportedOperationException("TrafficSignalsFabricatorRules is a utility class.");
  }

  /**
   * Prices traffic signal equipment by role: detection, control, or signal head.
   *
   * @param block        the block to price
   * @param registryName the block's registry name; unused, this tab prices by role alone
   *
   * @return the ingredients, or {@code null} for anything else in the tab, which takes Core's
   *     generic equipment cost
   *
   * @since 2026.9
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    if (block instanceof AbstractBlockTrafficSignalSensor
        || block instanceof AbstractBlockTrafficSignalSensorHZEight) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
    }
    if (block instanceof BlockTrafficSignalController) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 2),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (block instanceof AbstractBlockControllableSignal) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    // Pedestrian push buttons and the rest: Core's generic equipment cost, which is what this
    // branch returned directly before the rule moved out of Core.
    return null;
  }
}
