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
 * holds thousands of seats, so a seat stays cheap. On the event floor: ice is ice and a court is
 * planks (a painted cell or a line adds dye), a board is sheet metal, its glass is glass, and the
 * machines (the stanchion, the resurfacer, the ice plant) are priced as the machines they are. {@code audit_fabricator_costs.py} mirrors these
 * branches.
 *
 * @since 2026.10
 */
public final class VenuesFabricatorRules {

  /** The Seating tab. */
  public static final String TAB_SEATING = "tabvenuesseating";
  /** The Arena tab. */
  public static final String TAB_ARENA = "tabvenuesarena";

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
    List<FabricatorIngredient> arena = arena(registryName);
    if (arena != null) {
      return arena;
    }
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

  /** The Arena tab's pieces, or null for a registry name that is not one of them. */
  @Nullable
  private static List<FabricatorIngredient> arena(String registryName) {
    switch (registryName) {
      case "rink_ice":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:ice", 1));
      case "rink_ice_line":
      case "rink_ice_spot":
      case "rink_ice_painted":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:ice", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "rink_rubber_floor":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:carpet", 1));
      case "court_maple":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 1));
      case "court_line":
      case "court_floor_anchor":
      case "court_painted":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "hockey_goal":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_ingot", 2),
            FabricatorIngredient.any("minecraft:string", 2));
      case "basketball_stanchion":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 4),
            FabricatorIngredient.any("minecraft:glass_pane", 2),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "ice_resurfacer":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 6),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 2),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "ice_plant":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 2),
            FabricatorIngredient.part(CsmParts.DUCTING, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "scorers_table":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 2),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "player_bench":
      case "penalty_bench":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.any("minecraft:wool", 1));
      default:
        break;
    }
    if (registryName.startsWith("dasher_glass")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:glass_pane", 2),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.equals("dasher_penalty_door")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.any("minecraft:glass_pane", 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("dasher_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    return null;
  }
}
