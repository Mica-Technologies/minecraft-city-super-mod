package com.micatechnologies.minecraft.csm.powergrid;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Fabricator charges for the Utilities tab, by registry name.
 *
 * <p>Meters and panels are an enclosure shell with what is inside it: a control board for a
 * meter that counts electronically, glass for the analog meter's cover, a wiring harness for
 * anything that carries a building's supply. Gas and water fittings are iron pipe and a fastener
 * kit, a label a sign blank. The water system is steel by the plate: a tank (one item for the
 * whole bowl, or one layer of a ground tank) is sheet metal and fasteners by its size, a tower
 * leg or riser a pole section, the bracing iron. Anything this does not know takes the generic
 * cost.</p>
 *
 * <p>Mirrored in {@code dev-env-utils/scripts/audit_fabricator_costs.py}.</p>
 *
 * @since 2026.9
 */
public final class UtilitiesFabricatorRules {

  /** The Utilities tab, priced by {@link #price}. */
  public static final String TAB_ID = "tabutilities";

  private static final String MC_IRON_INGOT = "minecraft:iron_ingot";
  private static final String MC_GLASS_PANE = "minecraft:glass_pane";

  private static FabricatorIngredient sheet(int n) {
    return FabricatorIngredient.part(CsmParts.SHEET_METAL, n);
  }

  private static FabricatorIngredient iron(int n) {
    return FabricatorIngredient.any(MC_IRON_INGOT, n);
  }

  private static FabricatorIngredient part(String id, int n) {
    return FabricatorIngredient.part(id, n);
  }

  private UtilitiesFabricatorRules() {
  }

  /**
   * Prices a block in the Utilities tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the cost, or null for the generic cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    if (registryName.startsWith("utility_label_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    switch (registryName) {
      case "electric_meter_digital":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "electric_meter_analog":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
            FabricatorIngredient.any(MC_GLASS_PANE, 1));
      case "electric_meter_bank":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "electric_meter_socket":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "service_disconnect":
      case "main_panel":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "electric_switchboard":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 2));
      case "gas_meter":
      case "gas_meter_bank":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.any(MC_IRON_INGOT, 2));
      case "water_meter_setter":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 2),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      default:
        return water(registryName);
    }
  }

  /** The water system: the tower and the tanks. */
  @Nullable
  private static List<FabricatorIngredient> water(String registryName) {
    switch (registryName) {
      case "water_tower_leg":
      case "water_tower_riser":
        return CsmFabricatorCosts.cost(part(CsmParts.POLE_SECTION, 1));
      case "water_tower_brace":
      case "water_tower_strut":
      case "caged_ladder":
        return CsmFabricatorCosts.cost(iron(1));
      case "water_tower_pedestal":
        return CsmFabricatorCosts.cost(sheet(2));
      case "water_tower_bowl_small":
      case "water_tower_spheroid_small":
        return CsmFabricatorCosts.cost(sheet(8), part(CsmParts.FASTENER_KIT, 2));
      case "water_tower_bowl_medium":
      case "water_tower_spheroid_medium":
        return CsmFabricatorCosts.cost(sheet(16), part(CsmParts.FASTENER_KIT, 4));
      case "ground_tank_small":
        return CsmFabricatorCosts.cost(sheet(6), part(CsmParts.CONCRETE_MIX, 1));
      case "ground_tank_large":
        return CsmFabricatorCosts.cost(sheet(12), part(CsmParts.CONCRETE_MIX, 2));
      default:
        return null;
    }
  }
}
