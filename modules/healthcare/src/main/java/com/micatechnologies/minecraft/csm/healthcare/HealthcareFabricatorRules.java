package com.micatechnologies.minecraft.csm.healthcare;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Fabricator charges for the two Healthcare tabs, by what a piece is made of: a frame,
 * a motor and its controls for a bed; electronics where a piece has a screen or calls someone;
 * sheet metal and wiring for a headwall, several of each for a CT or MRI scanner; cloth for a
 * curtain; a sign blank for a sign; sheet metal and fittings for the rest. {@code audit_fabricator_costs.py} mirrors these branches.
 *
 * @since 2026.10
 */
public final class HealthcareFabricatorRules {

  /** The Patient Care tab. */
  public static final String TAB_PATIENT_CARE = "tabhealthcarepatientcare";
  /** The Clinical tab. */
  public static final String TAB_CLINICAL = "tabhealthcareclinical";

  private static final String WOOL = "minecraft:wool";

  private HealthcareFabricatorRules() {
  }

  /**
   * Prices a block in either Healthcare tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the cost, or null for the generic cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    if (registryName.startsWith("hospital_bed_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1),
          FabricatorIngredient.any(WOOL, 2));
    }
    if ("ct_scanner".equals(registryName) || "mri_scanner".equals(registryName)) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 4),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 2),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 2));
    }
    if (registryName.startsWith("nurse_call_") || registryName.startsWith("patient_monitor_")
        || registryName.startsWith("iv_pole_pump") || registryName.startsWith("patient_room_tv")
        || registryName.startsWith("vitals_") || registryName.endsWith("_analyser")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("patient_headwall")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("cubicle_curtain_track")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("cubicle_curtain")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(WOOL, 1));
    }
    if (registryName.endsWith("_sign")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
        FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
  }
}
