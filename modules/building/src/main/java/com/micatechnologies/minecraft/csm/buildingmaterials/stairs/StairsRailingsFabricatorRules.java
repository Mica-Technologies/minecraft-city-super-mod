package com.micatechnologies.minecraft.csm.buildingmaterials.stairs;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Stairs &amp; Railings tab costs in the CSM Fabricator, by what each piece is made of:
 * a steel railing iron, a glass one panes and its rail, a timber one planks, a cable rail its
 * posts and a fastener kit for the cables; a steel pan stair sheet metal filled with concrete, a
 * concrete stair concrete, a timber one planks (a carpeted one wool as well), and the open tread
 * stair its treads and a steel stringer. Mirrored in {@code audit_fabricator_costs.py}.
 *
 * @since 2026.10
 */
public final class StairsRailingsFabricatorRules {

  /** The Stairs &amp; Railings tab. */
  public static final String TAB_ID = "tabstairsrailings";

  private static final String IRON = "minecraft:iron_ingot";
  private static final String PLANKS = "minecraft:planks";
  private static final String GLASS_PANE = "minecraft:glass_pane";
  private static final String WOOL = "minecraft:wool";

  private StairsRailingsFabricatorRules() {
  }

  /** The cost of one of this tab's blocks, or null for the generic cost. */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    if (registryName.startsWith("stairs_")) {
      if (registryName.contains("steel_pan")) {
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1));
      }
      if (registryName.contains("concrete")) {
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1));
      }
      if (registryName.contains("open_tread")) {
        return CsmFabricatorCosts.cost(FabricatorIngredient.any(PLANKS, 2),
            FabricatorIngredient.any(IRON, 1));
      }
      if (registryName.contains("carpet")) {
        return CsmFabricatorCosts.cost(FabricatorIngredient.any(PLANKS, 2),
            FabricatorIngredient.any(WOOL, 1));
      }
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(PLANKS, 2));
    }
    if (registryName.startsWith("handrail_wall_")) {
      return registryName.contains("oak")
          ? CsmFabricatorCosts.cost(FabricatorIngredient.any(PLANKS, 1))
          : CsmFabricatorCosts.cost(FabricatorIngredient.any(IRON, 1));
    }
    if (registryName.startsWith("railing_glass_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(GLASS_PANE, 2),
          FabricatorIngredient.any(IRON, 1));
    }
    if (registryName.startsWith("railing_baluster_") || registryName.startsWith("railing_turned_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(PLANKS, 2));
    }
    if (registryName.startsWith("railing_cable_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(IRON, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("railing_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(IRON, 2));
    }
    return null;
  }
}
