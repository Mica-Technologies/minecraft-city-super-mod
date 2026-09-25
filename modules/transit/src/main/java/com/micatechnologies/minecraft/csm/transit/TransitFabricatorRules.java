package com.micatechnologies.minecraft.csm.transit;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Fabricator charges for the Transit tab.
 *
 * <p>The fare equipment is a powered cabinet with a reader, a board and its wiring. It costs what
 * it cost in the Technology tab it came from (a control board, sheet metal and a wiring harness),
 * so moving it changed no price. The bus stops are priced by what they are made of: a pole length
 * is a pole section, a flag a sign blank and its fixings, a poster case a sign blank in sheet
 * metal, the arrival display an LED panel with its board, the curb plaque a casting of sheet
 * metal. {@code audit_fabricator_costs.py} mirrors these branches.</p>
 *
 * @since 2026.9
 */
public final class TransitFabricatorRules {

  /** The Transit tab, priced by {@link #price}. */
  public static final String TAB_ID = "tabtransit";

  private TransitFabricatorRules() {
  }

  /**
   * Prices a block in the Transit tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the cost, or null for the generic cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    if (registryName.startsWith("bus_stop_pole_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1));
    }
    if (registryName.startsWith("bus_stop_flag_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("bus_stop_") && registryName.endsWith("_case")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.equals("bus_stop_arrival_display")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.equals("bus_stop_curb_plaque")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
        FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
        FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
  }
}
