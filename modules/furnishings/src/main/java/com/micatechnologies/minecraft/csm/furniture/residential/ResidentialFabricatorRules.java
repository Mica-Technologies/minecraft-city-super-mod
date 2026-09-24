package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Fabricator charges for the Residential tab: furniture is timber and fittings, as the
 * Furniture tab's is, in proportion to how much of it there is; upholstery adds wool, a
 * bookcase its books, and the cafe table's cast iron base an iron ingot.
 *
 * <p>Priced by registry name, whose piece is its first words and whose finish is its last
 * ({@code sofa_corner_navy}). {@code audit_fabricator_costs.py} mirrors this.</p>
 *
 * @since 2026.9
 */
public final class ResidentialFabricatorRules {

  /** The Residential tab. */
  public static final String TAB_ID = "tabresidential";

  private static final String MC_PLANKS = "minecraft:planks";
  private static final String MC_WOOL = "minecraft:wool";
  private static final String MC_BOOK = "minecraft:book";
  private static final String MC_IRON_INGOT = "minecraft:iron_ingot";

  private ResidentialFabricatorRules() {
  }

  /**
   * Prices a block in the Residential tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the cost, or null for the generic cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    if (registryName.startsWith("sofa_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_WOOL, 3));
    }
    if (registryName.startsWith("armchair_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_WOOL, 2));
    }
    if (registryName.startsWith("bookcase_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.any(MC_BOOK, 2));
    }
    if (registryName.startsWith("sideboard_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 6),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("tv_stand_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 4),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("dining_table_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("cafe_table_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 1),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    // Chairs, stools and the smaller tables.
    return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
        FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
  }
}
