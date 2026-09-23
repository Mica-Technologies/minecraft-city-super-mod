package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * Fabricator costs for the Streetscape tab.
 *
 * <p>Covers are cast iron, so they cost iron rather than the sheet metal and fasteners a bracket
 * does; the ones set in a concrete box add concrete, and the storm drain marker is a printed
 * plaque. Anything this does not know (the hydrant, the delineators) returns {@code null} and
 * takes the generic cost, which is what those blocks cost before they moved to this tab.</p>
 *
 * @version 1.0
 */
public final class StreetscapeFabricatorRules {

  /**
   * The Streetscape tab's id, as {@code CsmTabStreetscape.getTabId()} returns it.
   */
  public static final String TAB_ID = "tabstreetscape";

  private static final String MC_IRON_INGOT = "minecraft:iron_ingot";

  private StreetscapeFabricatorRules() {
  }

  /**
   * Prices a block in the Streetscape tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the ingredients, or {@code null} for the generic cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    if (!(block instanceof BlockStreetCover)) {
      return null;
    }
    if (registryName.startsWith("storm_drain_marker")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    if (registryName.startsWith("vault_lid") || registryName.startsWith("valve_box")
        || registryName.startsWith("sewer_cleanout")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1));
    }
    return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 2));
  }
}
