package com.micatechnologies.minecraft.csm.venues;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Fabricator charges for the Sports & Venues tabs, by what a piece is made of: a moulded
 * seat on a cast standard is sheet metal and fittings; a light is an LED and its housing; a glass
 * guard is glass on a metal shoe; a sign is a sign blank; a painted floor marking is dye. An arena
 * holds thousands of seats, so a seat stays cheap. {@code audit_fabricator_costs.py} mirrors these
 * branches.
 *
 * @since 2026.10
 */
public final class VenuesFabricatorRules {

  /** The Seating tab. */
  public static final String TAB_SEATING = "tabvenuesseating";

  private VenuesFabricatorRules() {
  }

  /**
   * Prices a block in a Sports & Venues tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the cost, or null for the generic cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    if (registryName.endsWith("_sign")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    if (registryName.endsWith("_light")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("stadium_glass_guard")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:glass_pane", 2),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.equals("stadium_wheelchair_space")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:dye", 1));
    }
    if (registryName.startsWith("stadium_seat") || registryName.endsWith("_seat")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
        FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
  }
}
