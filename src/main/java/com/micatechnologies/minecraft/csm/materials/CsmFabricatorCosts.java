package com.micatechnologies.minecraft.csm.materials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockFence;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockSetBasic;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockSlab;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockStairs;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPoleDiagonal;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.codeutils.ICsmBlock;
import com.micatechnologies.minecraft.csm.lifesafety.AbstractBlockFireAlarmActivator;
import com.micatechnologies.minecraft.csm.lifesafety.AbstractBlockFireAlarmDetector;
import com.micatechnologies.minecraft.csm.lifesafety.AbstractBlockFireAlarmSounder;
import com.micatechnologies.minecraft.csm.lifesafety.AbstractBlockFireAlarmSounderVoiceEvac;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficSignalController;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensorHZEight;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;

/**
 * The part cost of every City Super Mod block, for the {@link BlockCsmFabricator}.
 *
 * <p><b>Why this is computed rather than tabulated.</b> The mod has over 1,500 blocks but only
 * fourteen {@link CsmParts}. Grouping blocks by every attribute a cost table can actually
 * distinguish — creative tab plus base class — yields only 47 groups, so 846 of the 852
 * non-sign blocks would share ingredients with at least one other block. Minecraft resolves
 * identical recipes to whichever loaded first, so a per-block recipe file would have silently
 * left the great majority of the mod uncraftable. The Fabricator sidesteps that by selecting
 * the output explicitly instead of inferring it from ingredients, which means the cost only
 * has to be <i>fair</i>, not <i>unique</i>.</p>
 *
 * <p>Costs are therefore derived at runtime from what a block actually is. Nothing is
 * generated and nothing needs regenerating: a newly added block picks up the cost for its
 * subsystem automatically, and a block moved between tabs re-prices itself.</p>
 *
 * <p>A {@code null} return means "not fabricable". That covers non-CSM blocks and, importantly,
 * every block registered to {@code CsmTabNone}: hidden and retiring blocks are given a
 * {@code null} creative tab by {@code CsmTab}, so they fall out here without needing a
 * separate exclusion list.</p>
 *
 * @author Mica Technologies
 * @since 2026.7
 */
public final class CsmFabricatorCosts {

  /** Creative tab ids, matching the {@code getTabId()} of each {@code CsmTab} implementation. */
  private static final String TAB_BUILDING_MATERIALS = "tabbuildingmaterials";
  private static final String TAB_FURNITURE = "tabfurniture";
  private static final String TAB_GAMING = "tabgaming";
  private static final String TAB_HVAC = "tabhvac";
  private static final String TAB_LIFE_SAFETY = "tablifesafety";
  private static final String TAB_LIGHTING = "tablighting";
  private static final String TAB_MATERIALS = "tabmaterials";
  private static final String TAB_NOVELTIES = "tabnovelties";
  private static final String TAB_POWER_GRID = "tabpowergrid";
  private static final String TAB_ROAD_SIGNS = "tabroadsigns";
  private static final String TAB_TECHNOLOGY = "tabtechnology";
  private static final String TAB_TRAFFIC_ACCESSORIES = "tabtrafficaccessories";
  private static final String TAB_TRAFFIC_SIGNALS = "tabtrafficsignals";

  private CsmFabricatorCosts() {
    throw new UnsupportedOperationException("CsmFabricatorCosts is a utility class.");
  }

  /**
   * Returns the part cost to fabricate one of the given block, or {@code null} if the block
   * cannot be fabricated.
   *
   * @param block the block to price
   *
   * @return an ordered map of part registry name to required count, or {@code null}
   *
   * @since 2026.7
   */
  @Nullable
  public static Map<String, Integer> getCost(Block block) {
    if (!(block instanceof ICsmBlock)) {
      return null;
    }
    CreativeTabs tab = block.getCreativeTab();
    if (tab == null) {
      // Hidden / retiring blocks (CsmTabNone) get a null tab and are deliberately not
      // fabricable — they exist only so old worlds keep loading.
      return null;
    }
    // Resolved via CsmTab rather than CreativeTabs.getTabLabel(), which is client-only and is
    // stripped from the dedicated server by FML.
    String tabId = CsmTab.getTabId(tab);
    if (tabId == null || TAB_MATERIALS.equals(tabId)) {
      // The parts themselves and the Fabricator are crafted at a bench, not fabricated.
      return null;
    }

    switch (tabId) {
      case TAB_ROAD_SIGNS:
        return cost(CsmParts.SIGN_BLANK, 1);

      case TAB_BUILDING_MATERIALS:
        return buildingMaterialCost(block);

      case TAB_HVAC:
        return cost(CsmParts.DUCTING, 1, CsmParts.SHEET_METAL, 1);

      case TAB_LIFE_SAFETY:
        return lifeSafetyCost(block);

      case TAB_LIGHTING:
        return cost(CsmParts.LED_MODULE, 1, CsmParts.SHEET_METAL, 1,
            CsmParts.WIRING_HARNESS, 1);

      case TAB_NOVELTIES:
        return cost(CsmParts.SHEET_METAL, 2, CsmParts.CONTROL_BOARD, 1);

      case TAB_GAMING:
        return cost(CsmParts.SHEET_METAL, 2, CsmParts.CONTROL_BOARD, 1,
            CsmParts.LED_MODULE, 1);

      case TAB_POWER_GRID:
        return cost(CsmParts.POLE_SECTION, 1, CsmParts.WIRING_HARNESS, 1);

      case TAB_TECHNOLOGY:
        return cost(CsmParts.CONTROL_BOARD, 1, CsmParts.SHEET_METAL, 1,
            CsmParts.WIRING_HARNESS, 1);

      case TAB_TRAFFIC_ACCESSORIES:
        if (block instanceof AbstractBlockTrafficPole
            || block instanceof AbstractBlockTrafficPoleDiagonal) {
          return cost(CsmParts.POLE_SECTION, 1, CsmParts.FASTENER_KIT, 1);
        }
        return cost(CsmParts.SHEET_METAL, 1, CsmParts.FASTENER_KIT, 1);

      case TAB_TRAFFIC_SIGNALS:
        return trafficSignalCost(block);

      case TAB_FURNITURE:
        return cost(CsmParts.SHEET_METAL, 1, CsmParts.FASTENER_KIT, 1);

      default:
        // An unrecognised tab means a tab was added without pricing its contents. Fall back to
        // a sane generic cost rather than silently making the whole tab unfabricable.
        return cost(CsmParts.SHEET_METAL, 1, CsmParts.FASTENER_KIT, 1);
    }
  }

  /**
   * Prices building materials. Slab, stairs and fence variants are priced relative to the full
   * block they come from, mirroring vanilla's ratios rather than charging full price for a
   * partial block.
   */
  private static Map<String, Integer> buildingMaterialCost(Block block) {
    if (block instanceof AbstractBlockSlab) {
      return cost(CsmParts.SHEET_METAL, 1);
    }
    if (block instanceof AbstractBlockStairs) {
      return cost(CsmParts.SHEET_METAL, 2);
    }
    if (block instanceof AbstractBlockFence) {
      return cost(CsmParts.SHEET_METAL, 1, CsmParts.FASTENER_KIT, 1);
    }
    if (block instanceof AbstractBlockSetBasic) {
      return cost(CsmParts.SHEET_METAL, 2);
    }
    // The concrete-family blocks (PCC, CTF, CT/DCT variants).
    return cost(CsmParts.CONCRETE_MIX, 2);
  }

  /** Prices fire alarm and life safety equipment by appliance type. */
  private static Map<String, Integer> lifeSafetyCost(Block block) {
    if (block instanceof AbstractBlockFireAlarmSounder
        || block instanceof AbstractBlockFireAlarmSounderVoiceEvac) {
      return cost(CsmParts.SOUNDER_DRIVER, 1, CsmParts.ENCLOSURE_SHELL, 1);
    }
    if (block instanceof AbstractBlockFireAlarmActivator) {
      return cost(CsmParts.CONTROL_BOARD, 1, CsmParts.SHEET_METAL, 1);
    }
    if (block instanceof AbstractBlockFireAlarmDetector) {
      return cost(CsmParts.OPTICAL_SENSOR, 1, CsmParts.CONTROL_BOARD, 1);
    }
    return cost(CsmParts.SHEET_METAL, 1, CsmParts.WIRING_HARNESS, 1);
  }

  /** Prices traffic signal equipment by role: detection, control, or signal head. */
  private static Map<String, Integer> trafficSignalCost(Block block) {
    if (block instanceof AbstractBlockTrafficSignalSensor
        || block instanceof AbstractBlockTrafficSignalSensorHZEight) {
      return cost(CsmParts.OPTICAL_SENSOR, 1, CsmParts.CONTROL_BOARD, 1);
    }
    if (block instanceof BlockTrafficSignalController) {
      return cost(CsmParts.ENCLOSURE_SHELL, 1, CsmParts.CONTROL_BOARD, 2,
          CsmParts.WIRING_HARNESS, 1);
    }
    if (block instanceof AbstractBlockControllableSignal) {
      return cost(CsmParts.LED_MODULE, 1, CsmParts.LENS_ASSEMBLY, 1, CsmParts.SHEET_METAL, 1);
    }
    return cost(CsmParts.SHEET_METAL, 1, CsmParts.WIRING_HARNESS, 1);
  }

  private static Map<String, Integer> cost(String part, int count) {
    Map<String, Integer> map = new LinkedHashMap<>(4);
    map.put(part, count);
    return Collections.unmodifiableMap(map);
  }

  private static Map<String, Integer> cost(String partA, int countA, String partB, int countB) {
    Map<String, Integer> map = new LinkedHashMap<>(4);
    map.put(partA, countA);
    map.put(partB, countB);
    return Collections.unmodifiableMap(map);
  }

  private static Map<String, Integer> cost(String partA, int countA, String partB, int countB,
      String partC, int countC) {
    Map<String, Integer> map = new LinkedHashMap<>(4);
    map.put(partA, countA);
    map.put(partB, countB);
    map.put(partC, countC);
    return Collections.unmodifiableMap(map);
  }
}
